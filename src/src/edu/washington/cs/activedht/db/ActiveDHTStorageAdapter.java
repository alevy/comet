package edu.washington.cs.activedht.db;

import java.io.DataInputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.gudy.azureus2.core3.util.HashWrapper;

import com.aelitis.azureus.core.dht.DHTStorageAdapter;
import com.aelitis.azureus.core.dht.DHTStorageBlock;
import com.aelitis.azureus.core.dht.DHTStorageKey;
import com.aelitis.azureus.core.dht.DHTStorageKeyStats;
import com.aelitis.azureus.core.dht.transport.DHTTransportContact;
import com.aelitis.azureus.core.dht.transport.DHTTransportValue;


public class ActiveDHTStorageAdapter implements DHTStorageAdapter {	
	private DHTStorageAdapter adapter;
	
	private Map<HashWrapper, StoreListener> store_handlers_map;
	
	public ActiveDHTStorageAdapter(DHTStorageAdapter underlying_adapter) {
		this.adapter = underlying_adapter;
		store_handlers_map = Collections.synchronizedMap(
				new HashMap<HashWrapper, StoreListener>());
	}
	
	/**
	 * Registers a StoreListener for a particular key. If another listener
	 * already exists for the key, then the function will wait until that
	 * listener is unregistered. 
	 * 
	 * @param key
	 * @param listener
	 */
	protected void registerStoreListener(HashWrapper key,
			                             StoreListener listener) {
		synchronized(store_handlers_map) {
			while (store_handlers_map.containsKey(key)) {
				try { store_handlers_map.wait(); }
				catch (InterruptedException e) { }
			}

		    // assert(! store_handlers_map.containsKey(key));
			store_handlers_map.put(key, listener);
		}
	}
	
	/**
	 * Registers a StoreListener for a particular key. If another listener
	 * already exists for the key, then the function will wait until that
	 * listener is unregistered or until the timeout is reached.
	 * 
	 * @param key
	 * @param listener
	 * @param wait_timeout  timeout to wait in ms. If the timeout <= 0, then
	 * the function behaves equivalently to the registerStoreListener(
	 * DHTStorageKey, StoreListener) function.
	 * 
	 * @return true if the listener was added, false otherwise.
	 */
	protected boolean registerStoreListener(HashWrapper key,
                                            StoreListener listener,
                                            long wait_timeout) {
		if (wait_timeout <= 0) {
			registerStoreListener(key, listener);
			return true;
		}
		
		long start_time = System.currentTimeMillis();

		synchronized(store_handlers_map) {
			while (store_handlers_map.containsKey(key)) {
				try {
					store_handlers_map.wait(wait_timeout);
				} catch (InterruptedException e) {
					if (System.currentTimeMillis() - start_time >=
						wait_timeout) {
						return false;
					}
				}
			}
			
			// assert(! store_handlers_map.containsKey(key));
			store_handlers_map.put(key, listener);
			return true;
		}
	}
	
	protected void unregisterStoreListener(HashWrapper key) {
		synchronized(store_handlers_map) {
			store_handlers_map.remove(key);
			store_handlers_map.notify();  // notify a register-waiter.
		}
	}
	
	// Modified DB event handlers:

	@Override
	public void valueAdded(DHTStorageKey key, DHTTransportValue value) {
		adapter.valueAdded(key, value);
		addOutcome(key, null, value);
	}

	@Override
	public void valueUpdated(DHTStorageKey key,
			DHTTransportValue old_value, DHTTransportValue new_value) {
		adapter.valueUpdated(key, old_value, new_value);
		addOutcome(key, old_value, new_value);
	}
	
	private void addOutcome(DHTStorageKey key, DHTTransportValue old_value,
			                DHTTransportValue new_value) {
		StoreListener listener = store_handlers_map.get(key.getKey());  // sync
		if (listener == null) return;  // no listener registered.
		listener.addOutcome(old_value, new_value);
	}
	
	@Override
	public DHTStorageKey keyCreated(HashWrapper key, boolean local) {
		return adapter.keyCreated(key, local);
	} 

	// Unmodified DB event handlers:
	
	@Override
	public void keyRead(DHTStorageKey adapter_key,
            DHTTransportContact contact) {
		adapter.keyRead(adapter_key, contact);
	}

	@Override
	public void valueDeleted(DHTStorageKey key, DHTTransportValue value) {
		adapter.valueDeleted(key, value);
	}
	
	@Override
	public void keyDeleted(DHTStorageKey adapter_key) {
		adapter.keyDeleted(adapter_key);
	}
	
	@Override
	public void setStorageForKey(String key, byte[] data) {
		adapter.setStorageForKey(key, data);
	}
	
	@Override
	public byte[][] createNewDiversification(DHTTransportContact cause,
			byte[] key, boolean put_operation, byte diversification_type,
			boolean exhaustive_get, int max_depth) {
		return adapter.createNewDiversification(cause, key, put_operation,
				diversification_type, exhaustive_get, max_depth);
	}

	@Override
	public DHTStorageKeyStats deserialiseStats(DataInputStream is)
	throws IOException { return adapter.deserialiseStats(is); }

	@Override
	public DHTStorageBlock[] getDirectKeyBlocks() {
		return adapter.getDirectKeyBlocks();
	}

	@Override
	public byte[][] getExistingDiversification(byte[] key,
			boolean put_operation, boolean exhaustive_get, int max_depth) {
		return adapter.getExistingDiversification(key, put_operation,
				exhaustive_get, max_depth);
	}

	@Override
	public DHTStorageBlock getKeyBlockDetails(byte[] key) {
		return adapter.getKeyBlockDetails(key);
	}

	@Override
	public int getKeyCount() { return adapter.getKeyCount(); }

	@Override
	public byte[] getKeyForKeyBlock(byte[] request) {
		return adapter.getKeyForKeyBlock(request);
	}

	@Override
	public int getNetwork() { return adapter.getNetwork(); }

	@Override
	public int getNextValueVersions(int num) {
		return adapter.getNextValueVersions(num);
	}

	@Override
	public int getRemoteFreqDivCount() {
		return adapter.getRemoteFreqDivCount();
	}

	@Override
	public int getRemoteSizeDivCount() {
		return adapter.getRemoteSizeDivCount(); 
	}

	@Override
	public byte[] getStorageForKey(String key) {
		return adapter.getStorageForKey(key);
	}

	@Override
	public boolean isDiversified(byte[] key) {
		return adapter.isDiversified(key);
	}

	@Override
	public DHTStorageBlock keyBlockRequest(DHTTransportContact direct_sender,
			byte[] request, byte[] signature) {
		return adapter.keyBlockRequest(direct_sender, request, signature);
	}
}
