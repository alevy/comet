package edu.washington.cs.activedht.db.dhtactionexecutor.exedhtaction;

import com.aelitis.azureus.core.dht.DHTOperationListener;

public interface ActiveDHTOperationListener extends DHTOperationListener {
	public void setActionSpecificListener(DHTOperationListener delegate);
	
	/*private DHTOperationListener delegate;
	private Semaphore sem;
	private boolean has_timed_out = false;
	
	public ActiveDHTOperationListener(Semaphore sem) {
		this.sem = sem;
	}
	
	// Accessors:
	
	protected void setActionSpecificListener(DHTOperationListener delegate) {
		this.delegate = delegate;
	}

	public boolean hasTimedOut() { return has_timed_out; }

	// DHTOperationListener interface:
	
	@Override
	public final void complete(boolean timeout) {
		if (delegate != null) delegate.complete(timeout);
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
	public void read(DHTTransportContact contact, DHTTransportValue value) {
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
	public void wrote(DHTTransportContact contact, DHTTransportValue value) {
		if (delegate != null) delegate.wrote(contact, value);
	}*/
}
