package edu.washington.cs.activedht.db;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.aelitis.azureus.core.dht.transport.DHTTransportValue;


public class StoreListener implements Iterable<StoreOutcome> {
	private List<StoreOutcome> outcomes;
	
	public StoreListener() {
		outcomes = new ArrayList<StoreOutcome>();
	}
	
	public void addOutcome(DHTTransportValue added_value,
			               DHTTransportValue overwritten_value) {
		outcomes.add(new StoreOutcome(added_value, overwritten_value));
	}
	
	@Override
	public Iterator<StoreOutcome> iterator() {
		return outcomes.iterator();
	}
}