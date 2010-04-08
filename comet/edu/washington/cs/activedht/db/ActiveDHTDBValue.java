package edu.washington.cs.activedht.db;

import org.gudy.azureus2.core3.util.HashWrapper;

import com.aelitis.azureus.core.dht.control.DHTControl;
import com.aelitis.azureus.core.dht.transport.DHTTransportContact;

import edu.washington.cs.activedht.transport.BasicDHTTransportValue;

/**
 * @author levya
 * 
 */
public abstract class ActiveDHTDBValue extends BasicDHTTransportValue {

	private int intervalUnit = 1;

	public ActiveDHTDBValue(long creationTime, byte[] value, String string,
			int version, DHTTransportContact originator, boolean local,
			int flags) {
		super(creationTime, value, string, version, originator, local, flags);
	}

	public abstract ActiveDHTDBValue executeCallback(String string,
			Object... args);

	public abstract void registerGlobalState(DHTControl control, HashWrapper key);

	public abstract byte[] serialize(Object object);

	public abstract Object deserialize(byte[] value);

	public abstract Object wrap(DHTTransportContact reader);

	public void setIntervalUnit(int intervalUnit) {
		if (intervalUnit >= 0) {
			this.intervalUnit = intervalUnit;
		}
	}

	public int getIntervalUnit() {
		return intervalUnit;
	}

}
