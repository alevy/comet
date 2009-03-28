package edu.washington.cs.activedht.db.dhtactionexecutor;

import java.util.Iterator;

import com.aelitis.azureus.core.dht.DHTOperationListener;
import com.aelitis.azureus.core.dht.control.DHTControl;
import com.aelitis.azureus.core.dht.transport.DHTTransportContact;
import com.aelitis.azureus.core.dht.transport.DHTTransportValue;

import edu.washington.cs.activedht.code.insecure.dhtaction.DHTAction;
import edu.washington.cs.activedht.code.insecure.dhtaction.DHTActionList;
import edu.washington.cs.activedht.code.insecure.exceptions.ActiveCodeExecutionInterruptedException;
import edu.washington.cs.activedht.code.insecure.exceptions.InitializationException;
import edu.washington.cs.activedht.db.dhtactionexecutor.exedhtaction.ActiveDHTOperationListener;
import edu.washington.cs.activedht.db.dhtactionexecutor.exedhtaction.ExecutableDHTAction;
import edu.washington.cs.activedht.db.dhtactionexecutor.exedhtaction.ExecutableDHTActionFactory;
/**
 * TODO(roxana): There might be a vulnerability here. We're executing actions
 * one after the other within different threads. Hence, we're occupying
 * 
 * @author roxana
 *
 */
public class DHTActionExecutorImpl implements DHTActionExecutor {
	private boolean is_initialized = false;
	private DHTControl control;
	private ExecutableDHTActionFactory exe_action_factory;
	
	public DHTActionExecutorImpl(DHTControl control,
			ExecutableDHTActionFactory exe_action_factory) {
		this.control = control;
		this.exe_action_factory = exe_action_factory;
	}
	
	// DHTActionExecutor interface:
	
	@SuppressWarnings("unchecked")
	@Override
	public void executeActions(final DHTActionList actions,
			                   final long running_timeout)
	throws ActiveCodeExecutionInterruptedException, AbortDHTActionException {
		executeActions(actions.iterator(),
				       System.currentTimeMillis() + running_timeout);
	}
	
	@SuppressWarnings("unchecked")
	private void executeActions(final Iterator<DHTAction> actions,
			                    final long deadline) {
		long time_left_to_run_actions = deadline - System.currentTimeMillis();
		if (time_left_to_run_actions <= 0) return;  // we're past the timeout.		
		if (! actions.hasNext()) return;  // nothing else left to do.
		
		// Execute the next action.
		
		final DHTAction action = actions.next();
		
		// Wrap the action into an executable action.
		final ExecutableDHTAction executable_action = 
			exe_action_factory.createAction(action, control, 
					                        time_left_to_run_actions);
		if (executable_action == null) {
			// Unmatched executable version of the action.
			// Go on to the next action.
			executeActions(actions, deadline);
			return;
		}

		// Create a DHT listener for this action.
		ActiveDHTOperationListener listener =
			new ActiveDHTOperationListener() {
				private DHTOperationListener listener;
			
				// ActiveDHTOperationListener interface:
			
				@Override
				public void setActionSpecificListener(
						DHTOperationListener listener) {
					this.listener = listener;
				}

				// DHTOperationListener interface:
			
				@Override
				public void complete(boolean timeout) {
					if (listener != null) this.listener.complete(timeout);
				
					if (! timeout) action.markAsExecuted();
				
					// Continue running the next actions in list recursively.
					executeActions(actions, deadline);
				}

				@Override
				public void diversified(String desc) {
					if (listener != null) listener.diversified(desc);
				}

				@Override
				public void found(DHTTransportContact contact) {
					if (listener != null) listener.found(contact);
				}

				@Override
				public void read(DHTTransportContact contact,
								 DHTTransportValue value) {
					if (listener != null) listener.read(contact, value);
				}

				@Override
				public void searching(DHTTransportContact contact, int level,
					                  int active_searches) {
					if (listener != null) {
						listener.searching(contact, level, active_searches);
					}
				}

				@Override
				public void wrote(DHTTransportContact contact,
				   	              DHTTransportValue value) {
					if (listener != null) listener.wrote(contact, value);
				}
			};
		
		// Execute the current action.
		executable_action.execute(listener);
	}

	// Initializable interface:
	
	@Override
	public void init() throws InitializationException {
		is_initialized = true;
	}

	@Override
	public boolean isInitialized() {
		return is_initialized;
	}

	@Override
	public void stop() { }
}
