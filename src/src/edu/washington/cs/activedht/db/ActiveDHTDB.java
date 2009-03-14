package edu.washington.cs.activedht.db;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import org.gudy.azureus2.core3.util.HashWrapper;

import com.aelitis.azureus.core.dht.DHTLogger;
import com.aelitis.azureus.core.dht.DHTStorageAdapter;
import com.aelitis.azureus.core.dht.DHTStorageBlock;
import com.aelitis.azureus.core.dht.db.DHTDBLookupResult;
import com.aelitis.azureus.core.dht.db.DHTDBValue;
import com.aelitis.azureus.core.dht.db.impl.DHTDBImpl;
import com.aelitis.azureus.core.dht.transport.DHTTransportContact;
import com.aelitis.azureus.core.dht.transport.DHTTransportValue;

import edu.washington.cs.activedht.code.insecure.ActiveCodeSandbox;
import edu.washington.cs.activedht.code.insecure.DHTEvent;
import edu.washington.cs.activedht.code.insecure.DHTEventHandlerCallback;
import edu.washington.cs.activedht.code.insecure.DHTEventHandlerClosure;
import edu.washington.cs.activedht.code.insecure.dhtaction.DHTAction;
import edu.washington.cs.activedht.code.insecure.dhtaction.DHTActionList;
import edu.washington.cs.activedht.code.insecure.dhtaction.DHTActionMap;
import edu.washington.cs.activedht.code.insecure.dhtaction.DHTPostaction;
import edu.washington.cs.activedht.code.insecure.dhtaction.DHTPreaction;
import edu.washington.cs.activedht.code.insecure.exceptions.NotAnActiveObjectException;
import edu.washington.cs.activedht.db.ActiveDHTDBValueImpl.IllegalPackingStateException;
import edu.washington.cs.activedht.util.Constants;

public class ActiveDHTDB extends DHTDBImpl implements Constants {	
	static {
		Initializer.prepareRuntimeForActiveCode();
	}
	
	/** Pointer to the secure sandbox in which to run all objects. */
	private final ActiveCodeSandbox<byte[]> active_code_sandbox;

	public ActiveDHTDB(DHTStorageAdapter _adapter,
			           int _original_republish_interval, 
			           int _cache_republish_interval,
			           DHTLogger _logger) {
		super(_adapter, _original_republish_interval,
			  _cache_republish_interval, _logger);
		
		active_code_sandbox = new ActiveCodeSandbox<byte[]>(
				ACTIVE_CODE_EXECUTION_TIMEOUT);
	}
	
	// Override all DHTDB methods:

	/**
	 * Looks up a key in the DB and returns a set of values associated with
	 * that key. In Vuze, each value is associated with a different originator.
	 * Each value in the result set is then announced that it is being read.
	 * Upon this, it can decide whether to request exclusion from the returned
	 * set or not. This is typically done by preparing and returning a
	 * AbortDHTPostaction, though other solutions may be possible, depending
	 * on the available range of DHTActions.
	 */
	@Override
	public DHTDBLookupResult get(final DHTTransportContact reader,
			final HashWrapper key, int max_values, byte flags,
			boolean external_request) {
		DHTDBLookupResult result = super.get(reader, key, max_values, flags,
				external_request);
		if (result == null || ! external_request) return result;  // nothing.

		// Announce all of the values that they're being read.
		final DHTDBValue[] all_values = result.getValues();
		final DHTDBValue[] values_allowing_access = getHelper(reader, key,
				all_values);
		
		// If not all values have decided to remain, create a result with
		// only those that have not denied access.
		if (all_values != values_allowing_access) {  // Pointer comparison.
			final byte div_type = result.getDiversificationType();
			result = new DHTDBLookupResult() {
				public DHTDBValue[] getValues() {
					return values_allowing_access;
				}
				public byte getDiversificationType() { return div_type; }
			};
		}
		
		return result;
	}
	
	private DHTDBValue[] getHelper(final DHTTransportContact reader,
			                       final HashWrapper key,
			                       DHTDBValue[] values) {
		if (values == null || values.length == 0) return values;  // nothing.
		Set<DHTDBValue> excluded_values = new HashSet<DHTDBValue>();
		for (DHTDBValue value: values) {
			if (value == null) continue;
			try {
				doOnEvent(new DHTEventHandlerCallback.GetCb(
								reader.getAddress().getHostName()),
						  reader,
						  key,
						  value);
			} catch (NotAnActiveObjectException e) {    // nothing to do.
			} catch (InvalidActiveObjectException e) {  // nothing to do.
			} catch (AbortDHTActionException e) {  // value wants exclusion.
				// Exclude value from the result.
				excluded_values.add(value);
			}
		}
		
		// Remove those that have requested to be excluded.
		if (! excluded_values.isEmpty()) {
			int num_remaining_values = values.length - excluded_values.size();
			final DHTDBValue[] remaining_values = 
					new DHTDBValue[num_remaining_values];
			int i = 0;
			for (DHTDBValue v: values) {
				if (! excluded_values.contains(v)) remaining_values[i++] = v;
			}
			
			values = remaining_values;
		}
		
		return values;
	}

	@Override
	public DHTStorageBlock keyBlockRequest(DHTTransportContact direct_sender,
			byte[] request, byte[] signature) {
		// TODO(roxana): Need event handling for this?
		return super.keyBlockRequest(direct_sender, request, signature);
	}

	/**
	 * Removes a value from the database and then announces it of the fact that
	 * it's being evicted. The value has the chance of re-inserting itself
	 * into the DB, if it so chooses.
	 * 
	 * The re-insert, however, changes the value somewhat: the value looks
	 * like a brand new value (e.g., its timestamp is reset).
	 */
	@Override
	public DHTDBValue remove(DHTTransportContact sender, HashWrapper key) {
		// Remove the object.
		DHTDBValue removed_value = super.remove(sender, key);
		if (removed_value == null) return removed_value;  // nothing to do.
		
		// Announce the object of its eviction and re-add if it so requests.
		try {
			doOnEvent(new DHTEventHandlerCallback.DeleteCb(
							sender.getAddress().getHostName()),
					  sender,
					  key,
					  removed_value);
		} catch (NotAnActiveObjectException e) {    // nothing to do.
		} catch (InvalidActiveObjectException e) {  // nothing to do.
		} catch (AbortDHTActionException e) {  // value wants back in the DB.
			super.store(removed_value.getOriginator(), key,
					    new DHTDBValue[] { removed_value });
		}

		return removed_value;
	}

	@Override
	public byte store(DHTTransportContact sender, HashWrapper key,
			          DHTTransportValue[] values) {
		// Store the values.
		final byte diversification_type = super.store(sender, key, values);
		
		// Announce each value that it is being stored.
		storeHelper(sender, key, values);
		
		return diversification_type;
	}
	
	private DHTDBValue[] storeHelper(final DHTTransportContact sender,
            final HashWrapper key,
            DHTTransportValue[] values) {
		if (values == null || values.length == 0) return null;  // nothing.
		Set<DHTDBValue> excluded_values = new HashSet<DHTDBValue>();
		for (DHTDBValue value: values) {
			if (value == null) continue;
			try {
				doOnEvent(new DHTEventHandlerCallback.PutCb(
								sender.getAddress().getHostName(),
								values[0].getValue()),
						  sender,
						  key,
						  value);	
			} catch (NotAnActiveObjectException e) {    // nothing to do.
			} catch (InvalidActiveObjectException e) {  // nothing to do.
			} catch (AbortDHTActionException e) {  // value wants exclusion.
				// Exclude value from the result.
				excluded_values.add(value);
			}
		}

		return excluded_values;
	}
	
	private void doOnEvent(DHTEventHandlerCallback event_callback,
					       DHTTransportContact caller,
			               HashWrapper key,
			               DHTDBValue db_value)
	throws NotAnActiveObjectException, InvalidActiveObjectException,
	       AbortDHTActionException {
		if (db_value == null || 
			(! (db_value instanceof ActiveDHTDBValueImpl)) ||
			db_value.getValue() == null) {
			throw new NotAnActiveObjectException("Null or wrong-type value.");
		}
		
		ActiveDHTDBValueImpl value = (ActiveDHTDBValueImpl)db_value;
		
		// Unpack the value.
		try { value.unpack(event_callback.isValueLocal()); }
		catch (IOException e) {
			e.printStackTrace();
			return;
		}
		
		// Run the preactions.
		DHTActionMap<DHTPreaction> all_preactions = null;
		try { all_preactions = value.getPreactions(); }
		catch (IllegalPackingStateException e) {
			e.printStackTrace();
			assert(false);  // this is a true bug.
		}

		DHTActionList<DHTPreaction> event_preactions = null;
		if (all_preactions != null) {
			event_preactions = all_preactions.getActionsForEvent(
					event_callback.getEvent());
			if (event_preactions != null) executeActions(event_preactions);
		}

		// Run the object's code in the sandbox.
		DHTActionList<DHTPostaction> event_postactions =
			    new DHTActionList<DHTPostaction>(
			    		MAX_NUM_DHT_ACTIONS_PER_EVENT);
		byte[] current_value_bytes = executeActiveCodeSecurely(event_callback,
				value.getValue(),
				event_preactions,
				event_postactions);
		
		value.setValue(current_value_bytes);

		// Clear the preactions for the next execution.
		if (event_preactions != null) resetPreactions(event_preactions);
		
        // Pack the object back. TODO(roxana): Do we need this really?
		try { value.pack(); }
		catch (IOException e) { e.printStackTrace(); }
		
		// Finally, run the postactions.
		if (event_postactions != null) executeActions(event_postactions);
	}
	
	/**
	 * TODO(roxana): Maybe add a time limit on action list execution.
	 * @param actions
	 */
	private void executeActions(DHTActionList actions)
	throws AbortDHTActionException {
		long start = System.currentTimeMillis();
		for (int i = 0;
		     ((i < actions.size()) && (System.currentTimeMillis() - start <
		    		 MAX_TIME_RUN_DHT_ACTIONS_PER_EVENT));
		     ++i) {
			DHTAction action = actions.getAction(i);
			if (action == null) continue;
			executeAction(action);
		}
	}
	
	private void executeAction(DHTAction action) throws AbortDHTActionException {
		if (action == null) return;
		if (action instanceof DHTPreaction) {
			executePreaction((DHTPreaction)action);
		} else {
			executePostaction((DHTPostaction)action);
		}
		action.markAsExecuted();
	}
	
	private void executePreaction(DHTPreaction action)
	throws AbortDHTActionException {
		// assert(action != null);
		// TODO(roxana): Execute it based on its type.
	}
	
	private void executePostaction(DHTPostaction action)
	throws AbortDHTActionException {
        // assert(action != null);
        // TODO(roxana): Execute it based on its type.
	}
	
	private void resetPreactions(DHTActionList<DHTPreaction> actions) {
		for (int i = 0; i < actions.size(); ++i) {
			DHTPreaction action = actions.getAction(i);
			if (action != null) action.resetExecution();
		}
	}
	
	private byte[] executeActiveCodeSecurely(
			DHTEventHandlerCallback event_callback, 
			byte[] value_bytes,
			DHTActionList<DHTPreaction> executed_preactions,
			DHTActionList<DHTPostaction> postactions) {
		// Wrap the callback into a closure.
		DHTEventHandlerClosure closure = new DHTEventHandlerClosure(
				event_callback, value_bytes, executed_preactions, postactions);

		// Execute the closure within the sandbox.
		byte[] active_code = null;
		try {
			active_code = active_code_sandbox.executeWithinSandbox(closure);
		} catch (Exception e) { e.printStackTrace(); }  // error, but so what..

		return active_code;
	}
}

