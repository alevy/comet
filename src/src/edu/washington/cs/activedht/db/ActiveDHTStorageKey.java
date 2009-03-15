package edu.washington.cs.activedht.db;

import java.io.DataOutputStream;
import java.io.IOException;

import org.gudy.azureus2.core3.util.HashWrapper;

import com.aelitis.azureus.core.dht.DHTStorageKey;

public class ActiveDHTStorageKey implements DHTStorageKey {
	private DHTStorageKey key_adapter;
	private HashWrapper hash;
	
	public ActiveDHTStorageKey(DHTStorageKey key_adapter, HashWrapper hash) {
		this.key_adapter = key_adapter;
		this.hash = hash;
	}
	
	@Override
	public byte getDiversificationType() {
		return key_adapter.getDiversificationType();
	}

	@Override
	public void serialiseStats(DataOutputStream os) throws IOException {
		key_adapter.serialiseStats(os);
	}
	
	protected HashWrapper getHash() { return hash; }
}
