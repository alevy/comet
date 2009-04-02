package edu.washington.cs.activedht.db;

import com.aelitis.azureus.core.dht.transport.DHTTransportValue;

import edu.washington.cs.activedht.util.Pair;

public class StoreOutcome {
	private Pair<DHTTransportValue, DHTTransportValue> p;
	
	public StoreOutcome(DHTTransportValue overwritten_value,
			            DHTTransportValue added_value) {
		p = new Pair<DHTTransportValue, DHTTransportValue>(added_value,
				overwritten_value);
	}
		
	public DHTTransportValue getAddedValue() { return p.getFirst(); }
	
	public DHTTransportValue getOverwrittenValue() {
		return p.getSecond();
	}
}
