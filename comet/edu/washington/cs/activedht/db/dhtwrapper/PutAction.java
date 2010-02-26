/**
 * 
 */
package edu.washington.cs.activedht.db.dhtwrapper;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import com.aelitis.azureus.core.dht.DHTOperationAdapter;
import com.aelitis.azureus.core.dht.control.DHTControl;
import com.aelitis.azureus.core.dht.transport.DHTTransportContact;
import com.aelitis.azureus.core.dht.transport.DHTTransportReplyHandlerAdapter;
import com.aelitis.azureus.core.dht.transport.DHTTransportValue;

import edu.washington.cs.activedht.db.ActiveDHTDBValue;
import edu.washington.cs.activedht.db.kahlua.KahluaActiveDHTDBValue;

public class PutAction implements Runnable {
	private final Object putValue;
	private final DHTControl control;
	private final Set<DHTTransportContact> neighbors;
	private final byte[] key;
	private final ActiveDHTDBValue value;

	public PutAction(Object putValue, DHTControl control,
			Set<DHTTransportContact> neighbors, byte[] key,
			ActiveDHTDBValue value) {
		this.putValue = putValue;
		this.control = control;
		this.neighbors = neighbors;
		this.key = key;
		this.value = value;
	}

	public void run() {
		DHTTransportValue result = null;
		if (DHTTransportValue.class.isInstance(putValue)) {
			result = DHTTransportValue.class.cast(putValue);
		} else {
			result = new KahluaActiveDHTDBValue(System.currentTimeMillis(),
					value.serialize(putValue), 1, control.getTransport()
							.getLocalContact(), control.getTransport()
							.getLocalContact(), false, 0);
		}

		for (DHTTransportContact contact : neighbors) {
			contact.sendStore(
					new DHTTransportReplyHandlerAdapter() {

						public void failed(DHTTransportContact contact,
								Throwable error) {
							// TODO Auto-generated method stub

						}
					},
					new byte[][] { key },
					new DHTTransportValue[][] { new DHTTransportValue[] { result } },
					false);
		}

		control.put(key, "ActiveDHT Put", result, (byte) 0, true,
				new DHTOperationAdapter() {
					Set<DHTTransportContact> tmpNeighbors = Collections
							.synchronizedSet(new HashSet<DHTTransportContact>());

					public void wrote(DHTTransportContact contact,
							DHTTransportValue value) {
						synchronized (tmpNeighbors) {
							tmpNeighbors.add(contact);
						}
					}

					public void complete(boolean timeout) {
						synchronized (neighbors) {
							neighbors.clear();
							neighbors.addAll(tmpNeighbors);
						}
					}
				});
	}
}