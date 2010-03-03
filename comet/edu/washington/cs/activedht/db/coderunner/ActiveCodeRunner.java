package edu.washington.cs.activedht.db.coderunner;

import org.gudy.azureus2.core3.util.HashWrapper;

import com.aelitis.azureus.core.dht.transport.DHTTransportContact;
import com.aelitis.azureus.core.dht.transport.DHTTransportValue;

import edu.washington.cs.activedht.db.ActiveDHTDBValue;

public class ActiveCodeRunner {

	public ActiveDHTDBValue onGet(DHTTransportContact reader, HashWrapper key, HashWrapper readerId, byte[] payload,
			ActiveDHTDBValue value) {
		ActiveDHTDBValue result = value.executeCallback("onGet", value
				.wrap(reader), readerId, value.deserialize(payload));
		return result;
	}

	public ActiveDHTDBValue onRemove(DHTTransportContact sender,
			HashWrapper key, ActiveDHTDBValue removedValue) {
		return removedValue.executeCallback("onRemove", removedValue
				.wrap(sender));
	}

	public ActiveDHTDBValue onStore(DHTTransportContact sender,
			HashWrapper key, ActiveDHTDBValue activeValue) {
		return activeValue.executeCallback("onStore", activeValue.wrap(sender));
	}

	public ActiveDHTDBValue onUpdate(DHTTransportContact sender,
			HashWrapper key, ActiveDHTDBValue activeValue,
			DHTTransportValue updateValue) {
		return activeValue.executeCallback("onUpdate", activeValue
				.deserialize(updateValue.getValue()), activeValue.wrap(sender));
	}

	public void onTimer(HashWrapper key, ActiveDHTDBValue value) {
		value.executeCallback("onTimer");
	}

}
