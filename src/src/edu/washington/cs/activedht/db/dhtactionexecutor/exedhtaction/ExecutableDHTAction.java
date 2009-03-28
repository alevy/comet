package edu.washington.cs.activedht.db.dhtactionexecutor.exedhtaction;

import com.aelitis.azureus.core.dht.DHTOperationListener;
import com.aelitis.azureus.core.dht.control.DHTControl;

import edu.washington.cs.activedht.code.insecure.dhtaction.DHTAction;

public abstract class ExecutableDHTAction<T extends DHTAction> {
	private T action;
	private DHTControl control;
	
	public ExecutableDHTAction(T action, DHTControl control) {
		this.action = action;
		this.control = control;
	}
	
	public final void execute(ActiveDHTOperationListener listener) {
		DHTOperationListener specific_listener = getListener();
		
		listener.setActionSpecificListener(specific_listener);
		
		executeUsingListener(listener);
	}
	
	protected abstract void executeUsingListener(ActiveDHTOperationListener listener);
	
	protected abstract DHTOperationListener getListener();
	
	// Accessors (available only to subclasses):
	
	protected T getAction() { return action; }
	
	protected DHTControl getControl() { return control; }
}
