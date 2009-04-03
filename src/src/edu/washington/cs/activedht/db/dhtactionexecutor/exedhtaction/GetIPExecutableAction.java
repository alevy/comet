package edu.washington.cs.activedht.db.dhtactionexecutor.exedhtaction;

import com.aelitis.azureus.core.dht.DHTOperationListener;

import edu.washington.cs.activedht.code.insecure.dhtaction.GetIPAction;
import edu.washington.cs.activedht.db.ActiveDHTDB;

public class GetIPExecutableAction extends ExecutableDHTAction<GetIPAction> {
	public GetIPExecutableAction(GetIPAction action, ActiveDHTDB db) {
		super(action, db, null);
	}
	
	@Override
	protected void executeUsingListener(ActiveDHTOperationListener listener) {
		getAction().setIP(getControl().getTransport().getLocalContact()
				.getExternalAddress().toString());
		listener.complete(false);
	}

	@Override
	protected DHTOperationListener getListener() { return null; }
}
