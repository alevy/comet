package edu.washington.cs.activedht.db.dhtactionexecutor.exedhtaction;

import com.aelitis.azureus.core.dht.DHTOperationAdapter;
import com.aelitis.azureus.core.dht.DHTOperationListener;
import com.aelitis.azureus.core.dht.control.DHTControl;
import com.aelitis.azureus.core.dht.transport.DHTTransportContact;
import com.aelitis.azureus.core.dht.transport.DHTTransportValue;

import edu.washington.cs.activedht.code.insecure.dhtaction.GetDHTAction;

public class GetDHTExecutableAction
extends ExecutableDHTAction<GetDHTAction> {
	private long timeout;
	
	public GetDHTExecutableAction(GetDHTAction action, DHTControl control,
			                      long timeout) {
		super(action, control);
		this.timeout = timeout;
	}

	@Override
	protected void executeUsingListener(ActiveDHTOperationListener listener) {
		GetDHTAction action = getAction();
		getControl().get(action.getKey(), "ActiveDHT action", (byte)0,
				         action.getNumResponsesToWaitForSubjectToTimeout(),
				         this.timeout,
				         false,  // exhaustive -- TODO(roxana).
				         false,
				         listener);
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
