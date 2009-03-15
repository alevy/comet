package edu.washington.cs.activedht.db;

import java.util.Iterator;

import org.gudy.azureus2.core3.util.HashWrapper;

import com.aelitis.azureus.core.dht.db.impl.DHTDBImpl;
import com.aelitis.azureus.core.dht.db.impl.DHTDBMapping;
import com.aelitis.azureus.core.dht.db.impl.DHTDBValueImpl;
import com.aelitis.azureus.core.dht.transport.DHTTransportContact;

public class ActiveDHTDBMapping extends DHTDBMapping {
	protected ActiveDHTDBMapping(DHTDBImpl _db, HashWrapper _key,
			                     boolean _local) {
		super (_db, _key, _local);
	}
	
	@Override
	protected DHTDBValueImpl get(DHTTransportContact originator) {
		return super.get(originator);
	}
	
	@SuppressWarnings("unchecked")
	protected Iterator getValues() { return super.getValues(); }
}
