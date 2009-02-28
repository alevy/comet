package edu.washington.cs.activedht.db;

import com.aelitis.azureus.core.dht.db.impl.DHTDBValueImpl;
import com.aelitis.azureus.core.dht.transport.DHTTransportContact;
import com.aelitis.azureus.core.dht.transport.DHTTransportValue;

public class ActiveDHTDBValue extends DHTDBValueImpl {

	protected ActiveDHTDBValue(DHTTransportContact _sender,
			DHTTransportValue _other, boolean _local) {
		super(_sender, _other, _local);
	}

}
