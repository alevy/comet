package edu.washington.cs.activedht.db.dhtactionexecutor.exedhtaction;

import org.gudy.azureus2.core3.util.HashWrapper;

import com.aelitis.azureus.core.dht.DHTOperationAdapter;
import com.aelitis.azureus.core.dht.DHTOperationListener;
import com.aelitis.azureus.core.dht.transport.DHTTransportContact;
import com.aelitis.azureus.core.dht.transport.DHTTransportValue;

import edu.washington.cs.activedht.code.insecure.dhtaction.GetDHTAction;
import edu.washington.cs.activedht.db.ActiveDHTDB;

public class GetDHTExecutableAction
extends ExecutableDHTAction<GetDHTAction> {
	private long timeout;
	
	public GetDHTExecutableAction(GetDHTAction action, ActiveDHTDB db,
			                      HashWrapper key,
			                      long timeout) {
		super(action, db, key);
		this.timeout = timeout;
	}

	@Override
	protected void executeUsingListener(ActiveDHTOperationListener listener) {
		GetDHTAction action = getAction();
		getControl().getEncodedKey(getKeyBytes(), "ActiveDHT action", (byte)0,
				action.getNumResponsesToWaitForSubjectToTimeout(),
				this.timeout,
				true,  // exhaustive?? -- TODO(roxana).
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
						.getAddress().toString());
			}
		};
	}
}
