package edu.washington.cs.activedht.db.dhtactionexecutor;

import java.util.Iterator;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import org.gudy.azureus2.core3.util.HashWrapper;

import com.aelitis.azureus.core.dht.DHTOperationListener;
import com.aelitis.azureus.core.dht.transport.DHTTransportContact;
import com.aelitis.azureus.core.dht.transport.DHTTransportValue;

import edu.washington.cs.activedht.code.insecure.dhtaction.DHTAction;
import edu.washington.cs.activedht.code.insecure.dhtaction.DHTActionList;
import edu.washington.cs.activedht.code.insecure.exceptions.ActiveCodeExecutionInterruptedException;
import edu.washington.cs.activedht.code.insecure.exceptions.InitializationException;
import edu.washington.cs.activedht.db.ActiveDHTDB;
import edu.washington.cs.activedht.db.dhtactionexecutor.exedhtaction.ActiveDHTOperationListener;
import edu.washington.cs.activedht.db.dhtactionexecutor.exedhtaction.ExecutableDHTAction;
import edu.washington.cs.activedht.db.dhtactionexecutor.exedhtaction.ExecutableDHTActionFactory;
import edu.washington.cs.activedht.db.dhtactionexecutor.exedhtaction.NoSuchDHTActionException;
import edu.washington.cs.activedht.util.Constants;

public class DHTActionExecutorImpl implements DHTActionExecutor, Constants {
	private boolean is_initialized = false;
	public ActiveDHTDB db_pointer;
	private ExecutableDHTActionFactory exe_action_factory;
	
	public DHTActionExecutorImpl(ActiveDHTDB db_pointer,
			ExecutableDHTActionFactory exe_action_factory) {
		this.db_pointer = db_pointer;
		this.exe_action_factory = exe_action_factory;
	}
	
	// DHTActionExecutor interface:
	
	public String getThisHostAddr() {
		try {
			return db_pointer.getControl().getTransport()
				.getLocalContact().getAddress().toString();
		} catch (NullPointerException e) {
			return null;
		}
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public void executeActions(DHTActionList actions, HashWrapper key,
			                   long max_running_time,
			                   boolean should_wait_for_results)
	throws ActiveCodeExecutionInterruptedException, AbortDHTActionException {
		long deadline = System.currentTimeMillis() + max_running_time;
		
		// Execute actions in bulk, while the time permits.
		
		Iterator<DHTAction> actions_it = actions.iterator();
		Semaphore action_bulk_slots_semaphore =
			new Semaphore(NUM_SIMULTANEOUS_DHT_ACTIONS_PER_OBJECT);
		long time_left_to_run_actions = max_running_time;
		int num_actions_issued = 0;
		while (actions_it.hasNext() && time_left_to_run_actions > 0) {
			// Wait until I have one slot free in the actions semaphore.
			time_left_to_run_actions = grabSlots(action_bulk_slots_semaphore,
					                             1,
					                             deadline);
			// Run the next DHT action:
			try {
				startExecutingDHTAction(actions_it.next(), key,
						                action_bulk_slots_semaphore,
						                time_left_to_run_actions);
				++num_actions_issued;
			} catch (NoSuchDHTActionException e) { }
			
			time_left_to_run_actions = deadline - System.currentTimeMillis();
		}
		
		if (time_left_to_run_actions < 0) {
			throw new ActiveCodeExecutionInterruptedException(
				"Interrupted DHT Actions.");
		}

        // Wait for the results.
		time_left_to_run_actions = grabSlots(action_bulk_slots_semaphore,
				Math.min(num_actions_issued,
						 NUM_SIMULTANEOUS_DHT_ACTIONS_PER_OBJECT),
                deadline);
	}
	
	// Helper functions:
	
	private long grabSlots(Semaphore sem, int num_slots, long deadline)
	throws ActiveCodeExecutionInterruptedException {
		boolean grabbed_slots = false;
		long time_left = deadline - System.currentTimeMillis();
		while (!grabbed_slots && time_left > 0) {
			try {
				grabbed_slots = sem.tryAcquire(num_slots, time_left,
						                       TimeUnit.MILLISECONDS);
			} catch (InterruptedException e) { }
			time_left = deadline - System.currentTimeMillis();
		}
		if (! grabbed_slots) {
			throw new ActiveCodeExecutionInterruptedException(
					"Interrupted DHT Actions.");
		}
		return time_left;
	}
	
	private void startExecutingDHTAction(final DHTAction action,
			                             final HashWrapper key, 
			                             final Semaphore sem,
			                             final long timeout)
	throws AbortDHTActionException, NoSuchDHTActionException {
		if (action == null) return;
		
		// Wrap the action into an executable action.
		ExecutableDHTAction executable_action = 
			exe_action_factory.createAction(action, key, db_pointer, timeout);
		if (executable_action == null) return;  // unmatched executable action

		// Create a DHT listener for this action.
		ActiveDHTOperationListener listener =
			new ActiveDHTOperationListener() {
				private DHTOperationListener delegate;
				
				@Override
				public void setActionSpecificListener(
						DHTOperationListener delegate) {
					this.delegate = delegate;
				}

				@Override
				public void complete(boolean timeout) {
					if (delegate != null) delegate.complete(timeout);
					if (!timeout) action.markAsExecuted();
					sem.release();
				}

				@Override
				public void diversified(String desc) { 
					if (delegate != null) delegate.diversified(desc);
				}

				@Override
				public void found(DHTTransportContact contact) {
					if (delegate != null) delegate.found(contact);
				}

				@Override
				public void read(DHTTransportContact contact,
						         DHTTransportValue value) {
					if (delegate != null) delegate.read(contact, value);
				}
				
				@Override
				public void searching(DHTTransportContact contact, int level,
			 			              int active_searches) {
					if (delegate != null) {
						delegate.searching(contact, level, active_searches);
					}
				}

				@Override
				public void wrote(DHTTransportContact contact,
						          DHTTransportValue value) {
					if (delegate != null) delegate.wrote(contact, value);
				}
			};
		
		// Trigger the execution of the current action.
		executable_action.startExecuting(listener);
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
