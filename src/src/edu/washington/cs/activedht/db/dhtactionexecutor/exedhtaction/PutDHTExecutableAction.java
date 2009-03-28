package edu.washington.cs.activedht.db.dhtactionexecutor.exedhtaction;

import com.aelitis.azureus.core.dht.DHTOperationAdapter;
import com.aelitis.azureus.core.dht.DHTOperationListener;
import com.aelitis.azureus.core.dht.control.DHTControl;
import com.aelitis.azureus.core.dht.transport.DHTTransportContact;
import com.aelitis.azureus.core.dht.transport.DHTTransportValue;

import edu.washington.cs.activedht.code.insecure.dhtaction.PutDHTAction;

public class PutDHTExecutableAction
extends ExecutableDHTAction<PutDHTAction> {
	private long timeout;
	
	public PutDHTExecutableAction(PutDHTAction action,
			                      DHTControl control,
			                      long timeout) {
		super(action, control);
		this.timeout = timeout;
	}

	@Override
	protected void executeUsingListener(ActiveDHTOperationListener listener) {
		PutDHTAction action = getAction();
		getControl().put(action.getKey(), "ActiveDHT action",
				         action.getValue(),
				         (byte)0,
					     false,
					     null);
		// getControl().putEncodedKey(key, description, value, timeout,
		//	  	                      false);
	}

	@Override
	protected DHTOperationListener getListener() {
		return new DHTOperationAdapter() {
			@Override
			public void read(DHTTransportContact contact,
					         DHTTransportValue value) {
				getAction().addResponse(contact.getExternalAddress()
						.getAddress().getHostAddress());
			}
		};
	}
}
