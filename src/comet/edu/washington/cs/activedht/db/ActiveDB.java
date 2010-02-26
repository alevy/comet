/**
 * 
 */
package edu.washington.cs.activedht.db;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.gudy.azureus2.core3.util.HashWrapper;
import org.gudy.azureus2.core3.util.SystemTime;

import com.aelitis.azureus.core.dht.DHT;
import com.aelitis.azureus.core.dht.DHTStorageAdapter;
import com.aelitis.azureus.core.dht.DHTStorageBlock;
import com.aelitis.azureus.core.dht.control.DHTControl;
import com.aelitis.azureus.core.dht.db.DHTDB;
import com.aelitis.azureus.core.dht.db.DHTDBLookupResult;
import com.aelitis.azureus.core.dht.db.DHTDBStats;
import com.aelitis.azureus.core.dht.db.impl.DHTDBValueFactory;
import com.aelitis.azureus.core.dht.transport.DHTTransportContact;
import com.aelitis.azureus.core.dht.transport.DHTTransportValue;

import edu.washington.cs.activedht.db.coderunner.ActiveCodeRunner;

/**
 * @author levya
 * 
 */
public class ActiveDB implements DHTDB {

	private Map<HashWrapper, ActiveDHTDBValue> store = Collections
			.synchronizedMap(new HashMap<HashWrapper, ActiveDHTDBValue>());
	private final ActiveCodeRunner codeRunner;
	private DHTControl control;
	private final DHTStorageAdapter adapter;

	public ActiveDB(DHTStorageAdapter adapter) {
		this.adapter = adapter;
		this.codeRunner = new ActiveCodeRunner(this);
	}

	public DHTTransportValue get(HashWrapper key) {
		DHTTransportContact localContact = null;
		if (control != null) {
			localContact = control.getTransport().getLocalContact();
		}
		ActiveDHTDBValue result = get(key, localContact);
		return result.getValueForRelay(result.getOriginator());
	}
	
	private ActiveDHTDBValue get(HashWrapper key, DHTTransportContact reader) {
		ActiveDHTDBValue value = store.get(key);
		if (value != null) {
			value = codeRunner.onGet(reader, key, value);
		}
		return value;
	}

	public DHTDBLookupResult get(DHTTransportContact reader, HashWrapper key,
			int max_values, byte flags, boolean external_request) {
		return new ActiveDHTDBLookupResult(get(key, reader));
	}

	public DHTControl getControl() {
		return control;
	}

	public Iterator<HashWrapper> getKeys() {
		return store.keySet().iterator();
	}

	public boolean isEmpty() {
		return store.isEmpty();
	}

	public DHTDBStats getStats() {
		return null;
	}

	public int[] getValueDetails() {
		return new int[6];
	}

	public ActiveDHTDBValue remove(DHTTransportContact sender, HashWrapper key) {
		// TODO Auto-generated method stub
		return null;
	}

	public void setControl(DHTControl control) {
		this.control = control;
	}

	public DHTTransportValue store(HashWrapper key, byte[] value, byte flags) {
		DHTTransportContact localContact = control.getTransport()
				.getLocalContact();
		ActiveDHTDBValue activeValue = (ActiveDHTDBValue) DHTDBValueFactory
				.create(SystemTime.getCurrentTime(), value, adapter
						.getNextValueVersions(1), localContact, localContact,
						true, flags);
		store(localContact, key, new DHTTransportValue[] { activeValue });
		return activeValue.getValueForRelay(localContact);
	}

	public byte store(DHTTransportContact sender, HashWrapper key,
			DHTTransportValue[] values) {
		if (values.length == 0) {
			return DHT.DT_NONE;
		}
		if (values.length > 1) {
			throw new IllegalArgumentException(values.length + "");
		}

		DHTTransportValue value = values[0];
		synchronized (store) {
			ActiveDHTDBValue oldValue = store.get(key);
			if (oldValue != null) {
				ActiveDHTDBValue result = codeRunner.onUpdate(sender, key,
						oldValue, value);
				if (result != null) {
					store.put(key, result);
					adapter.valueUpdated(adapter.keyCreated(key, value
							.isLocal()), oldValue, result);
				}
			} else {
				ActiveDHTDBValue activeValue = (ActiveDHTDBValue) (ActiveDHTDBValue.class
						.isInstance(value) ? value : DHTDBValueFactory.create(
						value.getOriginator(), value, value.isLocal()));
				if (codeRunner.onStore(sender, key, activeValue) != null) {
					store.put(key, activeValue);
					adapter.valueAdded(adapter.keyCreated(key, activeValue
							.isLocal()), activeValue);
				}
			}
		}
		return DHT.DT_NONE;
	}

	public DHTStorageBlock[] getDirectKeyBlocks() {
		return new DHTStorageBlock[0];
	}

	public DHTStorageBlock getKeyBlockDetails(byte[] key) {
		// TODO Auto-generated method stub
		return null;
	}

	public boolean isKeyBlocked(byte[] key) {
		// TODO Auto-generated method stub
		return false;
	}

	public DHTStorageBlock keyBlockRequest(DHTTransportContact direct_sender,
			byte[] request, byte[] signature) {
		// TODO Auto-generated method stub
		return null;
	}

	public void print(boolean full) {
		// TODO Auto-generated method stub

	}

}
