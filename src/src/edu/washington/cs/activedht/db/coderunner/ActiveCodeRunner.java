package edu.washington.cs.activedht.db.coderunner;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.gudy.azureus2.core3.util.HashWrapper;

import com.aelitis.azureus.core.dht.db.DHTDB;
import com.aelitis.azureus.core.dht.db.impl.DHTDBValueFactory;
import com.aelitis.azureus.core.dht.transport.DHTTransportContact;
import com.aelitis.azureus.core.dht.transport.DHTTransportValue;

import edu.washington.cs.activedht.db.ActiveDHTDBValue;
import edu.washington.cs.activedht.db.StoreListener;
import edu.washington.cs.activedht.db.StoreOutcome;
import edu.washington.cs.activedht.util.Pair;

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
		ActiveDHTDBValue result = value.executeCallback("onGet", value
				.getDhtWrapper(db.getControl(), key));
		return result;
	}

	public ActiveDHTDBValue onRemove(DHTTransportContact sender,
			HashWrapper key, ActiveDHTDBValue removedValue) {
		return removedValue.executeCallback("onRemove", removedValue
				.getDhtWrapper(db.getControl(), key));
	}

	public Pair<List<DHTTransportValue>, List<DHTTransportValue>> onStore(
			DHTTransportContact sender, HashWrapper key,
			StoreListener store_listener) {
		List<DHTTransportValue> valuesToAddBack = new ArrayList<DHTTransportValue>();
		List<DHTTransportValue> valuesToRemove = new ArrayList<DHTTransportValue>();

		for (StoreOutcome outcome : store_listener) {
			DHTTransportValue overwrittenValue = outcome.getOverwrittenValue();
			DHTTransportValue addedValue = outcome.getAddedValue();
			if (overwrittenValue != null
					&& ActiveDHTDBValue.class.isInstance(overwrittenValue)) {
				DHTTransportValue value = onUpdate(sender, key,
						(ActiveDHTDBValue) overwrittenValue, addedValue);
				if (value != null) {
					valuesToAddBack.add(value);
				}
			} else {
				ActiveDHTDBValue value;
				if (ActiveDHTDBValue.class.isInstance(addedValue)) {
					value = (ActiveDHTDBValue) addedValue;
				} else {
					value = (ActiveDHTDBValue) DHTDBValueFactory.create(
							addedValue.getOriginator(), addedValue, addedValue
									.isLocal());
				}
				ActiveDHTDBValue result = onStore(sender, key, value);
				if (result == null) {
					valuesToRemove.add(addedValue);
				}
			}
		}
		return new Pair<List<DHTTransportValue>, List<DHTTransportValue>>(
				valuesToAddBack, valuesToRemove);
	}

	public ActiveDHTDBValue onStore(DHTTransportContact sender,
			HashWrapper key, ActiveDHTDBValue activeValue) {
		return activeValue.executeCallback("onStore", activeValue
				.getDhtWrapper(db.getControl(), key));
	}

	public ActiveDHTDBValue onUpdate(DHTTransportContact sender,
			HashWrapper key, ActiveDHTDBValue activeValue,
			DHTTransportValue updateValue) {
		return activeValue.executeCallback("onUpdate", activeValue
				.getDhtWrapper(db.getControl(), key), activeValue
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
		value.executeCallback("onTimes", value.getDhtWrapper(db
				.getControl(), key));
	}

}
