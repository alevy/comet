package edu.washington.cs.activedht.operations;

import com.aelitis.azureus.core.dht.control.DHTControl;

public class GetIPDHTOperation implements DHTOperation<Void, String> {
	@Override
	public String execute(DHTControl dht, Void param) {
		return dht.getTransport().getLocalContact().getExternalAddress()
			.getHostName();
	}
}
