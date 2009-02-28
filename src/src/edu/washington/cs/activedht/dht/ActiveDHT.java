package edu.washington.cs.activedht.dht;

import com.aelitis.azureus.core.dht.DHT;
import com.aelitis.azureus.core.dht.DHTOperationListener;

public interface ActiveDHT extends DHT {
	public void
	put(
		byte[]					key,
		String					description,
		byte[]					value,
		ActiveCode				code,
		byte					flags,
		DHTOperationListener	listener );
	
	public void
	put(
		byte[]					key,
		String					description,
		byte[]					value,
		ActiveCode				code,
		byte					flags,
		boolean					high_priority,
		DHTOperationListener	listener );
}
