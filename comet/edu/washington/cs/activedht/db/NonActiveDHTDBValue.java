/**
 * 
 */
package edu.washington.cs.activedht.db;

import org.gudy.azureus2.core3.util.HashWrapper;

import com.aelitis.azureus.core.dht.control.DHTControl;
import com.aelitis.azureus.core.dht.transport.BasicDHTTransportValue;
import com.aelitis.azureus.core.dht.transport.DHTTransportContact;
import com.aelitis.azureus.core.dht.transport.DHTTransportValue;

/**
 * @author levya
 * 
 */
public class NonActiveDHTDBValue extends BasicDHTTransportValue implements
		ActiveDHTDBValue {

	private DHTTransportContact sender;

	public NonActiveDHTDBValue(long creationTime, byte[] value, int version,
			DHTTransportContact originator, DHTTransportContact sender,
			boolean local, int flags) {
		super(creationTime, value, "Nonactive", version, originator, local,
				flags);
		this.sender = sender;
	}

	public Object deserialize(byte[] value) {
		return null;
	}

	public ActiveDHTDBValue executeCallback(String string, Object... args) {
		return new NonActiveDHTDBValue(getCreationTime(), getValue(),
				getVersion(), getOriginator(), sender, isLocal(), getFlags());
	}

	public Object wrap(DHTTransportContact contact) {
		return contact;
	}
	
	public void registerGlobalState(DHTControl control, HashWrapper key) {
		return;
	}

	public byte[] serialize(Object object) {
		return null;
	}

	public DHTTransportContact getSender() {
		return sender;
	}

	public long getStoreTime() {
		return 0;
	}

	public DHTTransportValue getValueForDeletion(int nextValueVersion) {
		return this;
	}

	public DHTTransportValue getValueForRelay(DHTTransportContact newOriginator) {
		return this;
	}

	public void reset() {

	}

	public void setCreationTime() {

	}

	public void setOriginator(DHTTransportContact contact) {

	}

	public void setSender(DHTTransportContact contact) {
		this.sender = contact;

	}

	public void setStoreTime(long l) {

	}

}
