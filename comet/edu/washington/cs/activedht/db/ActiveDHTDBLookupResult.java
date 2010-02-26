/**
 * 
 */
package edu.washington.cs.activedht.db;

import com.aelitis.azureus.core.dht.DHT;
import com.aelitis.azureus.core.dht.db.DHTDBLookupResult;
import com.aelitis.azureus.core.dht.db.DHTDBValue;

class ActiveDHTDBLookupResult implements DHTDBLookupResult {

	private final DHTDBValue[] values;

	public ActiveDHTDBLookupResult(DHTDBValue value) {
		if (value != null) {
			this.values = new DHTDBValue[] { value };
		} else {
			this.values = new DHTDBValue[] {};
		}
	}

	public byte getDiversificationType() {
		return DHT.DT_NONE;
	}

	public DHTDBValue[] getValues() {
		return values;
	}

}