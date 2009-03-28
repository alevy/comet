package edu.washington.cs.activedht.db.dhtactionexecutor.exedhtaction;

import com.aelitis.azureus.core.dht.DHTOperationListener;
import com.aelitis.azureus.core.dht.control.DHTControl;

import edu.washington.cs.activedht.code.insecure.dhtaction.GetIPAction;

public class GetIPExecutableAction extends ExecutableDHTAction<GetIPAction> {
	public GetIPExecutableAction(GetIPAction action, DHTControl control) {
		super(action, control);
	}
	
	@Override
	protected void executeUsingListener(ActiveDHTOperationListener listener) {
		getAction().setIP(getControl().getTransport().getLocalContact()
				.getExternalAddress().getAddress().getHostAddress());
		listener.complete(false);
	}

	@Override
	protected DHTOperationListener getListener() { return null; }
}
