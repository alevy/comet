package edu.washington.cs.activedht.db.coderunner;

import java.util.List;
import java.util.Map;

import org.gudy.azureus2.core3.util.HashWrapper;

import com.aelitis.azureus.core.dht.db.DHTDB;
import com.aelitis.azureus.core.dht.transport.DHTTransportContact;
import com.aelitis.azureus.core.dht.transport.DHTTransportValue;

import edu.washington.cs.activedht.db.ActiveDHTDBValue;

public class ActiveCodeRunner {

	private final DHTDB db;

	public ActiveCodeRunner(DHTDB db) {
		this.db = db;
	}

	public ActiveDHTDBValue[] onGet(DHTTransportContact reader,
			HashWrapper key, ActiveDHTDBValue[] values) {
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
		value.getDhtWrapper(db.getControl(), key);
		ActiveDHTDBValue result = value.executeCallback("onGet");
		return result;
	}

	public ActiveDHTDBValue onRemove(DHTTransportContact sender,
			HashWrapper key, ActiveDHTDBValue removedValue) {
		removedValue.getDhtWrapper(db.getControl(), key);
		return removedValue.executeCallback("onRemove");
	}

	public ActiveDHTDBValue onStore(DHTTransportContact sender,
			HashWrapper key, ActiveDHTDBValue activeValue) {
		activeValue.getDhtWrapper(db.getControl(), key);
		return activeValue.executeCallback("onStore");
	}

	public ActiveDHTDBValue onUpdate(DHTTransportContact sender,
			HashWrapper key, ActiveDHTDBValue activeValue,
			DHTTransportValue updateValue) {
		activeValue.getDhtWrapper(db.getControl(), key);
		return activeValue.executeCallback("onUpdate", activeValue
				.deserialize(updateValue.getValue()));
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
		value.getDhtWrapper(db.getControl(), key);
		value.executeCallback("onTimes", value.getDhtWrapper(db
				.getControl(), key));
	}

}
