package edu.washington.cs.activedht.db.dhtactionexecutor.exedhtaction;

import org.gudy.azureus2.core3.util.HashWrapper;

import com.aelitis.azureus.core.dht.DHTOperationAdapter;
import com.aelitis.azureus.core.dht.DHTOperationListener;
import com.aelitis.azureus.core.dht.db.impl.DHTDBValueImpl;
import com.aelitis.azureus.core.dht.transport.DHTTransportContact;
import com.aelitis.azureus.core.dht.transport.DHTTransportValue;

import edu.washington.cs.activedht.code.insecure.dhtaction.PutDHTAction;
import edu.washington.cs.activedht.db.ActiveDHTDB;

public class ReplicateValueDHTExecutableAction
extends ExecutableDHTAction<PutDHTAction> {
	private DHTDBValueImpl value;
	
	public ReplicateValueDHTExecutableAction(PutDHTAction action, ActiveDHTDB db,
			                                 HashWrapper key,
			                                 DHTDBValueImpl value) {
		super(action, db, key);
		this.value = value;
	}

	@Override
	protected void executeUsingListener(ActiveDHTOperationListener listener) {
		getDB().registerForRepublishing(getKey(), value);
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
