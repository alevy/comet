package edu.washington.cs.activedht.db;

import com.aelitis.azureus.core.dht.db.DHTDBValue;
import com.aelitis.azureus.core.dht.transport.DHTTransportContact;
import com.aelitis.azureus.core.dht.transport.DHTTransportValue;

public abstract class DHTDBValueFactory {

	private static DHTDBValueFactory factory;

	public abstract DHTDBValue create(long _creation_time, byte[] _value,
			int _version, DHTTransportContact _originator,
			DHTTransportContact _sender, boolean _local, int _flags);
	
	public abstract DHTDBValue create(DHTTransportContact sender,
			DHTTransportValue other, boolean local);
	
	public static DHTDBValueFactory get() {
		return factory;
	}
	
	public static void set(DHTDBValueFactory fact) {
		factory = fact;
	}
	
}
