package edu.washington.cs.activedht.db.dhtactionexecutor.exedhtaction;

import org.gudy.azureus2.core3.util.HashWrapper;

import com.aelitis.azureus.core.dht.DHTOperationListener;
import com.aelitis.azureus.core.dht.control.DHTControl;

import edu.washington.cs.activedht.code.insecure.dhtaction.DHTAction;
import edu.washington.cs.activedht.db.ActiveDHTDB;

public abstract class ExecutableDHTAction<T extends DHTAction> {
	private T action;
	private ActiveDHTDB db;
	private HashWrapper key;
	
	private byte[] key_bytes;
	
	public ExecutableDHTAction(T action, ActiveDHTDB db, HashWrapper key) {
		this.action = action;
		this.db     = db;
		this.key    = key;
		key_bytes   = key.getBytes();
	}
	
	public final void startExecuting(ActiveDHTOperationListener listener) {
		DHTOperationListener specific_listener = getListener();
		
		listener.setActionSpecificListener(specific_listener);
		
		executeUsingListener(listener);
	}
	
	protected abstract void executeUsingListener(
			ActiveDHTOperationListener listener);
	
	protected abstract DHTOperationListener getListener();
	
	// Accessors (available only to subclasses):
	
	protected T getAction() { return action; }
	
	protected DHTControl getControl() {
		return db == null ? null : db.getControl();
	}
	
	protected ActiveDHTDB getDB() { return db; }
	
	protected HashWrapper getKey() { return key; }
	
	protected byte[] getKeyBytes() { return key_bytes; }
}
