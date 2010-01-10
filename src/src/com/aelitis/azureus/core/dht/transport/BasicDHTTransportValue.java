package com.aelitis.azureus.core.dht.transport;

/**
 * 
 * @author levya
 *
 */
public class BasicDHTTransportValue implements DHTTransportValue {

	private final long creationTime;
	private final byte[] value;
	private final int version;
	private final DHTTransportContact originator;
	private final boolean local;
	private final int flags;
	private final String string;

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

}