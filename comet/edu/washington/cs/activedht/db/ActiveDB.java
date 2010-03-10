/**
 * 
 */
package edu.washington.cs.activedht.db;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.gudy.azureus2.core3.util.HashWrapper;
import org.gudy.azureus2.core3.util.SimpleTimer;
import org.gudy.azureus2.core3.util.SystemTime;
import org.gudy.azureus2.core3.util.TimerEvent;
import org.gudy.azureus2.core3.util.TimerEventPerformer;

import com.aelitis.azureus.core.dht.DHT;
import com.aelitis.azureus.core.dht.DHTStorageAdapter;
import com.aelitis.azureus.core.dht.DHTStorageBlock;
import com.aelitis.azureus.core.dht.control.DHTControl;
import com.aelitis.azureus.core.dht.db.DHTDB;
import com.aelitis.azureus.core.dht.db.DHTDBLookupResult;
import com.aelitis.azureus.core.dht.db.DHTDBStats;
import com.aelitis.azureus.core.dht.transport.DHTTransportContact;
import com.aelitis.azureus.core.dht.transport.DHTTransportQueryStoreReply;
import com.aelitis.azureus.core.dht.transport.DHTTransportValue;

import edu.washington.cs.activedht.db.coderunner.ActiveCodeRunner;
import edu.washington.cs.activedht.transport.BasicDHTTransportValue;

/**
 * @author levya
 * 
 */
public class ActiveDB implements DHTDB {

	private DHTControl control;

	private final Map<HashWrapper, ActiveDHTDBValue> store = Collections
			.synchronizedMap(new HashMap<HashWrapper, ActiveDHTDBValue>());
	
	private final ActiveCodeRunner codeRunner;
	private final DHTStorageAdapter adapter;
	private final TimerEventPerformer performer;

	public ActiveDB(DHTStorageAdapter adapter) {
		this.adapter = adapter;
		this.codeRunner = new ActiveCodeRunner();
		this.performer = new TimerEventPerformer() {
			@Override
			public void perform(TimerEvent event) {
				for (Map.Entry<HashWrapper, ActiveDHTDBValue> entry : store
						.entrySet()) {
					codeRunner.onTimer(entry.getKey(), entry.getValue());
				}
			}
		};
	}

	public void init() {
		SimpleTimer.addPeriodicEvent("ActiveDB Timer", 60000,
				//DHTControl.CACHE_REPUBLISH_INTERVAL_DEFAULT,
				performer);
	}

	public DHTTransportValue get(HashWrapper key, HashWrapper readerId,
			byte[] payload) {
		DHTTransportContact localContact = null;
		if (control != null) {
			localContact = control.getTransport().getLocalContact();
		}
		ActiveDHTDBValue result = get(localContact, key, readerId, payload);
		if (result == null) {
			return null;
		}
		return result.getValueForRelay(result.getOriginator());
	}

	private ActiveDHTDBValue get(DHTTransportContact reader, HashWrapper key,
			HashWrapper readerId, byte[] payload) {
		ActiveDHTDBValue value = store.get(key);
		if (value != null) {
			value = codeRunner.onGet(reader, key, readerId, payload, value);
		}
		return value;
	}

	public DHTDBLookupResult get(DHTTransportContact reader, HashWrapper key,
			HashWrapper readerId, byte[] payload, int max_values, byte flags,
			boolean external_request) {
		return new ActiveDHTDBLookupResult(get(reader, key, readerId, payload));
	}

	public DHTControl getControl() {
		return control;
	}

	public Iterator<HashWrapper> getKeys() {
		return store.keySet().iterator();
	}

	public boolean hasKey(HashWrapper key) {
		return store.containsKey(key);
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
		ActiveDHTDBValue removedValue = store.get(key);
		if (removedValue != null) {
			ActiveDHTDBValue result = codeRunner.onRemove(sender, key,
					removedValue);
			if (result == null) {
				store.remove(key);
			} else {
				result.registerGlobalState(control, key);
				store.put(key, result);
			}
		}
		return null;
	}

	public void setControl(DHTControl control) {
		this.control = control;
	}

	public DHTTransportValue store(HashWrapper key, byte[] value, byte flags,
			byte lifeHours, byte replicationControl) {
		DHTTransportContact localContact = control.getTransport()
				.getLocalContact();
		DHTTransportValue activeValue = new BasicDHTTransportValue(SystemTime
				.getCurrentTime(), value, "", adapter.getNextValueVersions(1),
				localContact, true, flags);
		store(localContact, key, new DHTTransportValue[] { activeValue });
		return activeValue;
	}

	public byte store(DHTTransportContact sender, HashWrapper key,
			DHTTransportValue[] values) {
		if (values.length == 0) {
			return DHT.DT_NONE;
		}
		if (values.length > 1) {
			// throw new IllegalArgumentException(values.length + "");
		}

		DHTTransportValue value = values[0];
		synchronized (store) {
			ActiveDHTDBValue oldValue = store.get(key);
			if (oldValue != null) {
				ActiveDHTDBValue result = codeRunner.onUpdate(sender, key,
						oldValue, value);
				if (result != null) {
					result.registerGlobalState(control, key);
					store.put(key, result);
					adapter.valueUpdated(adapter.keyCreated(key, value
							.isLocal()), oldValue, result);
				}
			} else {
				ActiveDHTDBValue activeValue = (ActiveDHTDBValue) DHTDBValueFactory
						.get().create(value.getOriginator(), value,
								value.isLocal());
				activeValue.registerGlobalState(control, key);
				ActiveDHTDBValue result = codeRunner.onStore(sender, key, activeValue);
				if (result != null) {
					result.registerGlobalState(control, key);
					store.put(key, result);
					adapter.valueAdded(adapter.keyCreated(key, result
							.isLocal()), result);
				}
			}
		}
		return DHT.DT_NONE;
	}

	public DHTStorageBlock[] getDirectKeyBlocks() {
		return new DHTStorageBlock[0];
	}

	public DHTStorageBlock getKeyBlockDetails(byte[] key) {
		return null;
	}

	public boolean isKeyBlocked(byte[] key) {
		return false;
	}

	public DHTStorageBlock keyBlockRequest(DHTTransportContact direct_sender,
			byte[] request, byte[] signature) {
		return null;
	}

	public void print(boolean full) {

	}

	public DHTTransportQueryStoreReply queryStore(
			DHTTransportContact originatingContact, final int headerLen,
			List<Object[]> keys) {
		return new DHTTransportQueryStoreReply() {

			public int getHeaderSize() {
				return headerLen;
			}

			public List<byte[]> getEntries() {
				return new ArrayList<byte[]>();
			}
		};
	}

}
