package edu.washington.cs.activedht.db;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.gudy.azureus2.core3.util.HashWrapper;

import com.aelitis.azureus.core.dht.db.DHTDBValue;
import com.aelitis.azureus.core.dht.transport.DHTTransportContact;
import com.aelitis.azureus.core.dht.transport.DHTTransportValue;

import edu.washington.cs.activedht.code.insecure.ActiveCodeSandbox;
import edu.washington.cs.activedht.code.insecure.DHTEventHandlerCallback;
import edu.washington.cs.activedht.code.insecure.DHTEventHandlerClosure;
import edu.washington.cs.activedht.code.insecure.dhtaction.DHTAction;
import edu.washington.cs.activedht.code.insecure.dhtaction.DHTActionList;
import edu.washington.cs.activedht.code.insecure.dhtaction.DHTActionMap;
import edu.washington.cs.activedht.code.insecure.dhtaction.DHTPostaction;
import edu.washington.cs.activedht.code.insecure.dhtaction.DHTPreaction;
import edu.washington.cs.activedht.code.insecure.exceptions.NotAnActiveObjectException;
import edu.washington.cs.activedht.code.insecure.io.InputStreamSecureClassLoader;
import edu.washington.cs.activedht.db.ActiveDHTDBValueImpl.IllegalPackingStateException;
import edu.washington.cs.activedht.db.ActiveDHTStorageAdapter.StoreListener;
import edu.washington.cs.activedht.db.ActiveDHTStorageAdapter.StoreOutcome;
import edu.washington.cs.activedht.util.Constants;
import edu.washington.cs.activedht.util.Pair;

public class ActiveCodeRunner {
	// TODO(roxana): Change this with config.
	private final ActiveCodeRunnerParams params;
	
	/** Pointer back to the DB. */
	private ActiveDHTDB db;
	
	/** Pointer to the sandbox where all active code will run. */
	private final ActiveCodeSandbox<byte[]> active_code_sandbox;
	
	protected ActiveCodeRunner(ActiveDHTDB db, ActiveCodeRunnerParams params) {
		active_code_sandbox = new ActiveCodeSandbox<byte[]>(
				params.active_code_execution_timeout);
		this.db = db;
		this.params = params;
	}
	
	protected DHTDBValue[] onGet(final DHTTransportContact reader,
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
		
		DHTDBValue[] remaining_values = null;
		// Remove those that have requested to be excluded.
		if (! excluded_values.isEmpty()) {
			int num_remaining_values = values.length - excluded_values.size();
			remaining_values = new DHTDBValue[num_remaining_values];
			int i = 0;
			for (DHTDBValue v: values) {
				if (! excluded_values.contains(v)) remaining_values[i++] = v;
			}
		}
		
		return remaining_values;
	}
	
	
	protected DHTDBValue onRemove(final DHTTransportContact sender,
                                  final HashWrapper key,
                                  DHTDBValue removed_value) {
		try {
			doOnEvent(new DHTEventHandlerCallback.DeleteCb(
					sender.getAddress().getHostName()),
					sender,
					key,
					removed_value);
		} catch (NotAnActiveObjectException e) {    // nothing to do.
		} catch (InvalidActiveObjectException e) {  // nothing to do.
		} catch (AbortDHTActionException e) {  // value wants back in the DB.
			return removed_value;
		}

		return null;
	}
	
	protected Pair<List<DHTTransportValue>, List<DHTTransportValue>>
	onStore(final DHTTransportContact sender,
			final HashWrapper key,
			StoreListener store_listener) {
		List<DHTTransportValue> values_to_add_back =
			new ArrayList<DHTTransportValue>();
		List<DHTTransportValue> values_to_remove =
			new ArrayList<DHTTransportValue>();
		
		for (StoreOutcome store_outcome: store_listener) {
			DHTTransportValue added_value = store_outcome.getAddedValue();
			DHTTransportValue overwritten_value =
				store_outcome.getOverwrittenValue();
			
			// Overwritten value takes precedence over added value, so
			// announce it first.
			if (overwritten_value != null) {
				try {
					doOnEvent(new DHTEventHandlerCallback.ValueChangedCb(
									sender.getAddress().getHostName(),
									added_value.getValue()),
							  sender,
							  key,
							  (DHTDBValue)overwritten_value);
				} catch (NotAnActiveObjectException e) {    // nothing to do.
				} catch (InvalidActiveObjectException e) {  // nothing to do.
				} catch (AbortDHTActionException e) {  // value wants back in.
					values_to_add_back.add(overwritten_value);
					continue;  // ignore what the added value has to say.
				}
			}
			
			// If there was no overwritten value or it didn't mind being
			// overwritten, then initialize the new value.
			try {
				doOnEvent(new DHTEventHandlerCallback.ValueAddedCb(
								sender.getAddress().getHostName(),
								Constants.MAX_NUM_DHT_ACTIONS_PER_EVENT),
						  sender,
						  key,
						  (DHTDBValue)added_value);
			} catch (NotAnActiveObjectException e) {    // nothing to do.
			} catch (InvalidActiveObjectException e) {  // nothing to do.
			} catch (AbortDHTActionException e) {  // value wants back in.
				if (overwritten_value != null) {  // replace it with former.
					values_to_add_back.add(overwritten_value);
				} else {  // remove it from the DB.
					// TODO(roxana): Does the above have the correct semantic?
					values_to_remove.add(added_value);
				}
			}
		}
		
		return new Pair<List<DHTTransportValue>, List<DHTTransportValue>>(
				values_to_add_back,
				values_to_remove);
	} 
	
	protected void onTimer() { 
		// TODO(roxana): Implement.
	}
	
	// General-event handler:
	
	protected void doOnEvent(DHTEventHandlerCallback event_callback,
	                         DHTTransportContact caller,
                             HashWrapper key,
                             DHTDBValue db_value)
	throws NotAnActiveObjectException, InvalidActiveObjectException,
		   AbortDHTActionException {
		// Preliminary checks.
		if (db_value == null || 
				(! (db_value instanceof ActiveDHTDBValueImpl)) ||
				db_value.getValue() == null) {
			throw new NotAnActiveObjectException("Null or wrong-type value.");
		}
		
		// Initialize the event callback:
		try {
			event_callback.init(InputStreamSecureClassLoader.newInstance(
					caller.getAddress().getHostName(),
					caller.getAddress().getPort()));
		} catch (MalformedURLException e) {
			e.printStackTrace();
			return;
		}

		ActiveDHTDBValueImpl value = (ActiveDHTDBValueImpl)db_value;

		// Unpack the value.
		try { value.unpack(event_callback.getImposedPreactionsMap()); }
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
					params.max_num_dht_actions_per_event);
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
	@SuppressWarnings("unchecked")
	private void executeActions(DHTActionList actions)
	throws AbortDHTActionException {
		long start = System.currentTimeMillis();
		for (int i = 0;
		     ((i < actions.size()) && (System.currentTimeMillis() - start <
	          params.max_time_run_dht_actions_per_event));
		     ++i) {
			DHTAction action = actions.getAction(i);
			if (action == null) continue;
			executeAction(action);
		}
	}

	private void executeAction(DHTAction action)
	throws AbortDHTActionException {
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
	
	protected static class ActiveCodeRunnerParams {
		public long active_code_execution_timeout =
			Constants.ACTIVE_CODE_CHECK_EXECUTION_INTERVAL_NANOS;
		public int max_num_dht_actions_per_event =
			Constants.MAX_NUM_DHT_ACTIONS_PER_EVENT;
		public long max_time_run_dht_actions_per_event =
			Constants.MAX_TIME_RUN_DHT_ACTIONS_PER_EVENT;
	} 
}
