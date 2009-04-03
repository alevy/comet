package edu.washington.cs.activedht.db.coderunner;

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

import edu.washington.cs.activedht.code.insecure.DHTEventHandlerCallback;
import edu.washington.cs.activedht.code.insecure.DHTEventHandlerClosure;
import edu.washington.cs.activedht.code.insecure.dhtaction.DHTAction;
import edu.washington.cs.activedht.code.insecure.dhtaction.DHTActionList;
import edu.washington.cs.activedht.code.insecure.dhtaction.DHTActionMap;
import edu.washington.cs.activedht.code.insecure.exceptions.ActiveCodeExecutionInterruptedException;
import edu.washington.cs.activedht.code.insecure.exceptions.NotAnActiveObjectException;
import edu.washington.cs.activedht.code.insecure.io.InputStreamSecureClassLoader;
import edu.washington.cs.activedht.code.insecure.sandbox.ActiveCodeSandbox;
import edu.washington.cs.activedht.db.ActiveDHTDBValueImpl;
import edu.washington.cs.activedht.db.StoreListener;
import edu.washington.cs.activedht.db.StoreOutcome;
import edu.washington.cs.activedht.db.dhtactionexecutor.AbortDHTActionException;
import edu.washington.cs.activedht.db.dhtactionexecutor.DHTActionExecutor;
import edu.washington.cs.activedht.util.Constants;
import edu.washington.cs.activedht.util.Pair;

/**
 * TODO(roxana): Isolate in interface.
 * TODO(roxana): Make singleton.
 * @author roxana
 *
 */
public class ActiveCodeRunner {
	// TODO(roxana): Change this with config!
	private final ActiveCodeRunnerParam params;
	
	/** Pointer to the control engine of the DB. */
	private DHTActionExecutor dht_action_executor;
	
	/** Pointer to the sandbox where all active code will run. */
	private final ActiveCodeSandbox<byte[]> active_code_sandbox;
	
	/**
	 * @param active_code_sandbox
	 * @param params
	 */
	public ActiveCodeRunner(ActiveCodeSandbox<byte[]> active_code_sandbox,
			                DHTActionExecutor dht_action_executor,
			                @Deprecated ActiveCodeRunnerParam params) {
		assert(active_code_sandbox.isInitialized());
		
		this.active_code_sandbox = active_code_sandbox;
		this.dht_action_executor = dht_action_executor;
		
		this.params = params;  // TODO(roxana): Replace w/ config.
	}
	
	public DHTDBValue[] onGet(final DHTTransportContact reader,
			                  final HashWrapper key,
			                  DHTDBValue[] values) {
		if (values == null || values.length == 0) return values;  // nothing.
		Set<DHTDBValue> excluded_values = new HashSet<DHTDBValue>();
		for (DHTDBValue value: values) {
			if (value == null) continue;
			try {
				doOnEvent(new DHTEventHandlerCallback.GetCb(
								reader.getAddress().toString()),
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
	
	public DHTDBValue onRemove(final DHTTransportContact sender,
                               final HashWrapper key,
                               DHTDBValue removed_value) {
		try {
			doOnEvent(new DHTEventHandlerCallback.DeleteCb(
							sender.getAddress().toString()),
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
	
	public Pair<List<DHTTransportValue>, List<DHTTransportValue>>
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
									sender.getAddress().toString(),
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
								this.dht_action_executor.getThisHostAddr(),
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
	
	public void onTimer() { 
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
		catch (IOException e) { return; }  // nothing to do.

		DHTActionList event_postactions = null;
		try {
			// Run the preactions.
			DHTActionMap all_preactions = null;
			try { all_preactions = value.getPreactions(); }
			catch(IllegalPackingStateException e) {
				e.printStackTrace();
				assert(false);  // this is a true bug.
			}
			DHTActionList event_preactions = null;
			if (all_preactions != null) {
				event_preactions = all_preactions.getActionsForEvent(
						event_callback.getEvent());
				if (event_preactions != null) {
					try {
						dht_action_executor.executeActions(event_preactions,
								key,
								params.max_time_run_dht_actions_per_event,
								true);
					} catch (ActiveCodeExecutionInterruptedException e) {
						// Nothing to do??
					}
				}
			}
		
			// Run the object's code in the sandbox.
			event_postactions = new DHTActionList(
					params.max_num_dht_actions_per_event);
			byte[] current_value_bytes = executeActiveCodeSecurely(
					event_callback,
					value.getValue(),
					event_preactions,
					event_postactions);
			value.setValue(current_value_bytes);
		
			// Clear the preactions for the next execution.
			if (event_preactions != null) resetPreactions(event_preactions);
		} catch(AbortDHTActionException e) {
			throw e;
		} finally {
			try { value.pack(); }
			catch (IOException e) { return; }
		}

		// Finally, run the postactions.
		if (event_postactions != null) {
			try {
				dht_action_executor.executeActions(event_postactions, key,
						params.max_time_run_dht_actions_per_event,
						false);
			} catch (ActiveCodeExecutionInterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	private void resetPreactions(DHTActionList actions) {
		for (int i = 0; i < actions.size(); ++i) {
			DHTAction action = actions.getAction(i);
			if (action != null) action.resetExecution();
		}
	}

	private byte[] executeActiveCodeSecurely(
			DHTEventHandlerCallback event_callback, 
			byte[] value_bytes,
			DHTActionList executed_preactions,
			DHTActionList postactions) {
		// Wrap the callback into a closure.
		DHTEventHandlerClosure closure = new DHTEventHandlerClosure(
				event_callback,
				value_bytes,
				executed_preactions,
				postactions);
		
		// Execute the closure within the sandbox.
		byte[] active_code = null;
		try {
			active_code = active_code_sandbox.executeWithinSandbox(closure);
		} catch (Exception e) { e.printStackTrace(); }  // error, but so what..

		return active_code;
	}
	
	// TODO(roxana): Change this with config.
	public static class ActiveCodeRunnerParam {
		public long active_code_execution_timeout =
			Constants.ACTIVE_CODE_CHECK_EXECUTION_INTERVAL_NANOS;
		public int max_num_dht_actions_per_event =
			Constants.MAX_NUM_DHT_ACTIONS_PER_EVENT;
		public long max_time_run_dht_actions_per_event =
			Constants.MAX_TIME_RUN_DHT_ACTIONS_PER_EVENT;
	}
}
