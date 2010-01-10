package edu.washington.cs.activedht.db.coderunner;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.gudy.azureus2.core3.util.HashWrapper;

import com.aelitis.azureus.core.dht.transport.DHTTransportContact;
import com.aelitis.azureus.core.dht.transport.DHTTransportValue;

import edu.washington.cs.activedht.db.ActiveDHTDBValue;
import edu.washington.cs.activedht.db.StoreListener;
import edu.washington.cs.activedht.db.StoreOutcome;
import edu.washington.cs.activedht.util.Pair;

public class ActiveCodeRunner {

	public ActiveDHTDBValue[] onGet(DHTTransportContact reader, HashWrapper key,
			ActiveDHTDBValue[] values) {
		ActiveDHTDBValue[] intermediate = new ActiveDHTDBValue[values.length];
		int resultTotal = 0;
		for (int i = 0; i < values.length; ++i) {
			intermediate[i] = onGet(reader, key, values[i]);
			if (intermediate[i] != null) {
				++resultTotal;
			}
		}

		ActiveDHTDBValue[] resultValues = new ActiveDHTDBValue[resultTotal];
		for (int i = 0, j = 0; i < values.length; ++i) {
			if (intermediate[i] != null) {
				resultValues[j] = intermediate[i];
				++j;
			}
		}
		return resultValues;
	}
	
	public ActiveDHTDBValue onGet(DHTTransportContact reader, HashWrapper key,
			ActiveDHTDBValue value) {
		return value.executeCallback("onGet");
	}

	public ActiveDHTDBValue onRemove(DHTTransportContact sender, HashWrapper key,
			ActiveDHTDBValue removedValue) {
		return removedValue.executeCallback("onRemove");
	}
	
	public Pair<List<DHTTransportValue>, List<DHTTransportValue>> onStore(
			DHTTransportContact sender, HashWrapper key,
			StoreListener store_listener) {
		for (StoreOutcome outcome : store_listener) {
			onStore(sender, key, outcome);
		}
		return null;
	}

	public DHTTransportValue onStore(DHTTransportContact sender,
			HashWrapper key, StoreOutcome store_outcome) {
		// TODO Auto-generated method stub
		return null;
	}

	public void onTimer(Map<HashWrapper, List<ActiveDHTDBValue>> to_activate) {
		for (Map.Entry<HashWrapper, List<ActiveDHTDBValue>> pair : to_activate
				.entrySet()) {
			HashWrapper key = pair.getKey();
			for (ActiveDHTDBValue value : pair.getValue()) {
				onTimer(key, value);
			}
		}
	}
	
	public void onTimer(HashWrapper key, ActiveDHTDBValue value) {
		// TODO Auto-generated method stub
		
	}

}
