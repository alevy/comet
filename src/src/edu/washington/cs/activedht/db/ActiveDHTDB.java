package edu.washington.cs.activedht.db;

import org.gudy.azureus2.core3.util.HashWrapper;

import com.aelitis.azureus.core.dht.db.DHTDB;
import com.aelitis.azureus.core.dht.db.DHTDBLookupResult;
import com.aelitis.azureus.core.dht.db.DHTDBValue;
import com.aelitis.azureus.core.dht.transport.DHTTransportContact;
import com.aelitis.azureus.core.dht.transport.DHTTransportValue;

public interface ActiveDHTDB extends DHTDB {
	public void registerForRepublishing(HashWrapper key, DHTDBValue value);
	
	public DHTDBValue superStore(HashWrapper key, byte[] value, byte flags);

	public byte superStore(DHTTransportContact sender, HashWrapper key,
			               DHTTransportValue[] values);

	public DHTDBValue superGet(HashWrapper key);
	
	public DHTDBLookupResult superGet(DHTTransportContact reader,
		                              HashWrapper key,
		                              int max_values,
		                              byte flags,
		                              boolean external_request);
	
	public DHTDBValue superRemove(DHTTransportContact sender,
			                      HashWrapper key);
}

