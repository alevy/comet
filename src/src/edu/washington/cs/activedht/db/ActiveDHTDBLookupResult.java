/**
 * 
 */
package edu.washington.cs.activedht.db;

import com.aelitis.azureus.core.dht.DHT;
import com.aelitis.azureus.core.dht.db.DHTDBLookupResult;
import com.aelitis.azureus.core.dht.db.DHTDBValue;

class ActiveDHTDBLookupResult implements DHTDBLookupResult {

	private final ActiveDHTDBValue[] values;

	public ActiveDHTDBLookupResult(ActiveDHTDBValue value) {
		if (value != null) {
			this.values = new ActiveDHTDBValue[] { value };
		} else {
			this.values = new ActiveDHTDBValue[] {};
		}
	}

	public byte getDiversificationType() {
		return DHT.DT_NONE;
	}

	public DHTDBValue[] getValues() {
		return values;
	}

}