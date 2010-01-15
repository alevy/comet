package edu.washington.cs.activedht.db.coderunner;

import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.gudy.azureus2.core3.util.HashWrapper;

import com.aelitis.azureus.core.dht.db.DHTDBValue;
import com.aelitis.azureus.core.dht.db.impl.DHTDBValueImpl;
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
import edu.washington.cs.activedht.db.StoreListener;
import edu.washington.cs.activedht.db.StoreOutcome;
import edu.washington.cs.activedht.db.dhtactionexecutor.AbortDHTActionException;
import edu.washington.cs.activedht.db.dhtactionexecutor.DHTActionExecutor;
import edu.washington.cs.activedht.db.java.JavaActiveDHTDBValue;
import edu.washington.cs.activedht.util.Constants;
import edu.washington.cs.activedht.util.Pair;

/**
 * This class is {@link Deprecated}. Use {@link LuaActiveCodeRunner} instead.
 * 
 * TODO(roxana): Isolate in interface.
 * TODO(roxana): Make singleton.
 * @author roxana
 *
 */
@Deprecated
public class JavaActiveCodeRunner {// implements ActiveCodeRunner {
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
	public JavaActiveCodeRunner(ActiveCodeSandbox<byte[]> active_code_sandbox,
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
								getExternalAddress(reader)),
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
	
	private static String getExternalAddress(DHTTransportContact sender) {
		return sender.getExternalAddress().toString();
	}
	
	public DHTDBValue onRemove(final DHTTransportContact sender,
                               final HashWrapper key,
                               DHTDBValue removed_value) {
		try {
			doOnEvent(new DHTEventHandlerCallback.DeleteCb(
							getExternalAddress(sender)),
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
			// Overwritten value takes precedence over added value, so announce
			// it first.
			if (overwritten_value != null) {
				try {
					doOnEvent(new DHTEventHandlerCallback.ValueChangedCb(
									getExternalAddress(sender),
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
								getExternalAddress(sender),
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
	
	public void onTimer(
			Map<HashWrapper, List<DHTDBValueImpl>> key_to_value_map) { 
		for (Map.Entry<HashWrapper, List<DHTDBValueImpl>> e:
			 key_to_value_map.entrySet()) {
			HashWrapper key = e.getKey();
			for (DHTDBValueImpl v: e.getValue()) {
				if (! (v instanceof JavaActiveDHTDBValue)) continue;  // ignore
				JavaActiveDHTDBValue value = (JavaActiveDHTDBValue)v;
				try {
					doOnEvent(new DHTEventHandlerCallback.TimerCb(), null, key,
							  value);
				} catch (NotAnActiveObjectException e1) {  // nothing to do.
				} catch (InvalidActiveObjectException e1) {  // nothing to do.
				} catch (AbortDHTActionException e1) { }  // nothing to do.
			}
		}
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
			(! (db_value instanceof JavaActiveDHTDBValue)) ||
			db_value.getValue() == null) {
			throw new NotAnActiveObjectException("Null or wrong-type value.");
		}
		
		JavaActiveDHTDBValue value = (JavaActiveDHTDBValue)db_value;
		
		// Initialize the event callback:
		try {
			InetSocketAddress originator =
				db_value.getOriginator().getExternalAddress();
			event_callback.init(InputStreamSecureClassLoader.newInstance(
					originator.getHostName(),
					originator.getPort()));
		} catch (MalformedURLException e) {
			e.printStackTrace();
			return;
		}

		DHTActionList event_postactions = null;
		DHTActionMap all_preactions = value.getPreactions(
				event_callback.getImposedPreactionsMap());
		DHTActionList event_preactions = null;
		if (all_preactions != null) {
			event_preactions = all_preactions.getActionsForEvent(
					event_callback.getEvent());
			if (event_preactions != null) {
				try {
					dht_action_executor.executeActions(event_preactions,
							key,
							value,
							params.max_time_run_dht_actions_per_event,
							true);
				} catch (ActiveCodeExecutionInterruptedException e) {
					throw new AbortDHTActionException(e.getMessage());
				}
			}
		}

		// Run the object's code in the sandbox.
		event_postactions = new DHTActionList(
				params.max_num_dht_actions_per_event);
		byte[] old_value = value.getValue();
		byte[] current_value_bytes = executeActiveCodeSecurely(
				event_callback,
				value.getValue(),
				event_preactions,
				event_postactions);
		if (current_value_bytes != null && current_value_bytes != old_value) {
			value.setValue(current_value_bytes);
		}
	
		// Clear the preactions for the next execution.
		if (event_preactions != null) resetPreactions(event_preactions);

		// Finally, run the postactions.
		if (event_postactions != null) {
			try {
				dht_action_executor.executeActions(event_postactions, key, value,
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
		byte[] active_code = value_bytes;
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
