package edu.washington.cs.activedht.db.coderunner;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.gudy.azureus2.core3.util.HashWrapper;

import com.aelitis.azureus.core.dht.db.impl.DHTDBImpl;
import com.aelitis.azureus.core.dht.db.impl.DHTDBValueFactory;
import com.aelitis.azureus.core.dht.transport.DHTTransportContact;
import com.aelitis.azureus.core.dht.transport.DHTTransportValue;

import edu.washington.cs.activedht.db.ActiveDHTDBValue;
import edu.washington.cs.activedht.db.StoreListener;
import edu.washington.cs.activedht.db.StoreOutcome;
import edu.washington.cs.activedht.util.Pair;

public class ActiveCodeRunner {

	private final DHTDBImpl control;

	public ActiveCodeRunner(DHTDBImpl control) {
		this.control = control;
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
		ActiveDHTDBValue result = value.executeCallback("onGet", value.getDhtWrapper(control
				.getControl(), key));
		return result;
	}

	public ActiveDHTDBValue onRemove(DHTTransportContact sender,
			HashWrapper key, ActiveDHTDBValue removedValue) {
		return removedValue.executeCallback("onRemove", removedValue
				.getDhtWrapper(control.getControl(), key));
	}

	public Pair<List<DHTTransportValue>, List<DHTTransportValue>> onStore(
			DHTTransportContact sender, HashWrapper key,
			StoreListener store_listener) {
		List<DHTTransportValue> valuesToAddBack = new ArrayList<DHTTransportValue>();
		List<DHTTransportValue> valuesToRemove = new ArrayList<DHTTransportValue>();

		for (StoreOutcome outcome : store_listener) {
			DHTTransportValue overwrittenValue = outcome.getOverwrittenValue();
			DHTTransportValue addedValue = outcome.getAddedValue();
			if (overwrittenValue != null) {
				DHTTransportValue value = onUpdate(sender, key,
						overwrittenValue, addedValue);
				if (value != null) {
					valuesToAddBack.add(value);
				}
			} else {
				DHTTransportValue value = onStore(sender, key, addedValue);
				if (value == null) {
					valuesToRemove.add(addedValue);
				}
			}
		}
		return new Pair<List<DHTTransportValue>, List<DHTTransportValue>>(
				valuesToAddBack, valuesToRemove);
	}

	public DHTTransportValue onStore(DHTTransportContact sender,
			HashWrapper key, DHTTransportValue value) {
		if (ActiveDHTDBValue.class.isInstance(value)) {
			ActiveDHTDBValue activeValue = ActiveDHTDBValue.class.cast(value);
			return activeValue.executeCallback("onStore", activeValue
					.getDhtWrapper(control.getControl(), key));
		} else {
			return DHTDBValueFactory.create(value.getOriginator(), value, value
					.isLocal());
		}
	}

	public DHTTransportValue onUpdate(DHTTransportContact sender,
			HashWrapper key, DHTTransportValue value,
			DHTTransportValue updateValue) {
		if (ActiveDHTDBValue.class.isInstance(value)) {
			ActiveDHTDBValue activeValue = ActiveDHTDBValue.class.cast(value);
			return activeValue.executeCallback("onUpdate", activeValue
					.getDhtWrapper(control.getControl(), key), activeValue
					.deserialize(updateValue.getValue()));
		} else {
			return DHTDBValueFactory.create(value.getOriginator(), value, value
					.isLocal());
		}
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
		value.executeCallback("onTimes", value.getDhtWrapper(control
				.getControl(), key));
	}

}
