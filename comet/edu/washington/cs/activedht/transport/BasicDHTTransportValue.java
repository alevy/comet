package edu.washington.cs.activedht.transport;

import com.aelitis.azureus.core.dht.DHT;
import com.aelitis.azureus.core.dht.db.DHTDBValue;
import com.aelitis.azureus.core.dht.transport.DHTTransportContact;

/**
 * 
 * @author levya
 *
 */
public class BasicDHTTransportValue implements DHTDBValue {

	private final long creationTime;
	private final byte[] value;
	private final int version;
	private final DHTTransportContact originator;
	private final boolean local;
	private final String string;
	
	private int flags;

	public BasicDHTTransportValue(long creationTime, byte[] value,
			String string, int version, DHTTransportContact originator,
			boolean local, int flags) {
		this.creationTime = creationTime;
		this.value = value;
		this.string = string;
		this.version = version;
		this.originator = originator;
		this.local = local;
		this.flags = flags;
	}
	
	public long getCreationTime() {
		return creationTime;
	}

	public int getFlags() {
		return flags;
	}

	public DHTTransportContact getOriginator() {
		return originator;
	}

	public String getString() {
		return string;
	}

	public byte[] getValue() {
		return value;
	}

	public int getVersion() {
		return version;
	}

	public boolean isLocal() {
		return local;
	}

	public int
	getLifeTimeHours() {
		return 8;
	}
	
	public byte
	getReplicationControl() {
		return DHT.REP_FACT_DEFAULT;
	}
	
	public byte
	getReplicationFactor() {
		return DHT.REP_FACT_NONE;
	}

	public byte
	getReplicationFrequencyHours() {
		return DHT.REP_FACT_NONE;
	}

	public DHTDBValue getValueForRelay(DHTTransportContact arg0) {
		return this;
	}

	public void setFlags(byte flags) {
		this.flags = flags;
	}

}