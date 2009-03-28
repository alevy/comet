package edu.washington.cs.activedht.db.dhtactionexecutor.exedhtaction;

import com.aelitis.azureus.core.dht.DHTOperationListener;

public interface ActiveDHTOperationListener extends DHTOperationListener {
	public void setActionSpecificListener(DHTOperationListener listener);
}
