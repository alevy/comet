package edu.washington.cs.activedht.db;

import java.io.DataInputStream;
import java.io.IOException;

import org.gudy.azureus2.core3.util.HashWrapper;

import com.aelitis.azureus.core.dht.DHTStorageAdapter;
import com.aelitis.azureus.core.dht.DHTStorageBlock;
import com.aelitis.azureus.core.dht.DHTStorageKey;
import com.aelitis.azureus.core.dht.DHTStorageKeyStats;
import com.aelitis.azureus.core.dht.transport.DHTTransportContact;
import com.aelitis.azureus.core.dht.transport.DHTTransportValue;

public class ActiveDHTStorageAdapter implements DHTStorageAdapter {
	DHTStorageAdapter adapter; 
	public ActiveDHTStorageAdapter(DHTStorageAdapter underlying_adapter) {
		this.adapter = underlying_adapter;
	}

	public byte[][] createNewDiversification(DHTTransportContact cause,
			byte[] key, boolean put_operation, byte diversification_type,
			boolean exhaustive_get, int max_depth) {
		return adapter.createNewDiversification(cause, key, put_operation,
				diversification_type, exhaustive_get, max_depth);
	}

	public DHTStorageKeyStats deserialiseStats(DataInputStream is)
	throws IOException { return adapter.deserialiseStats(is); }

	public DHTStorageBlock[] getDirectKeyBlocks() {
		return adapter.getDirectKeyBlocks();
	}

	public byte[][] getExistingDiversification(byte[] key,
			boolean put_operation, boolean exhaustive_get, int max_depth) {
		return adapter.getExistingDiversification(key, put_operation,
				exhaustive_get, max_depth);
	}

	public DHTStorageBlock getKeyBlockDetails(byte[] key) {
		return adapter.getKeyBlockDetails(key);
	}

	public int getKeyCount() { return adapter.getKeyCount(); }

	public byte[] getKeyForKeyBlock(byte[] request) {
		return adapter.getKeyForKeyBlock(request);
	}

	public int getNetwork() { return adapter.getNetwork(); }

	public int getNextValueVersions(int num) {
		return adapter.getNextValueVersions(num);
	}

	public int getRemoteFreqDivCount() {
		return adapter.getRemoteFreqDivCount();
	}

	public int getRemoteSizeDivCount() {
		return adapter.getRemoteSizeDivCount(); 
	}

	public byte[] getStorageForKey(String key) {
		return adapter.getStorageForKey(key);
	}

	public boolean isDiversified(byte[] key) {
		return adapter.isDiversified(key);
	}

	public DHTStorageBlock keyBlockRequest(DHTTransportContact direct_sender,
			byte[] request, byte[] signature) {
		return adapter.keyBlockRequest(direct_sender, request, signature);
	}

	public DHTStorageKey keyCreated(HashWrapper key, boolean local) {
		return adapter.keyCreated(key, local);
	}

	public void keyDeleted(DHTStorageKey adapter_key) {
		adapter.keyDeleted(adapter_key);
	}

	public void keyRead(DHTStorageKey adapter_key,
			            DHTTransportContact contact) {
		adapter.keyRead(adapter_key, contact);
	}

	public void setStorageForKey(String key, byte[] data) {
		adapter.setStorageForKey(key, data);
	}

	public void valueAdded(DHTStorageKey key, DHTTransportValue value) {
		adapter.valueAdded(key, value);		
	}

	public void valueDeleted(DHTStorageKey key, DHTTransportValue value) {
		adapter.valueDeleted(key, value);
	}

	public void valueUpdated(DHTStorageKey key,
			DHTTransportValue old_value, DHTTransportValue new_value) {
		adapter.valueUpdated(key, old_value, new_value);
	}
}
