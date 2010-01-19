/**
 * 
 */
package edu.washington.cs.activedht.db.dhtwrapper;

import java.util.HashSet;
import java.util.Set;

import com.aelitis.azureus.core.dht.DHTOperationAdapter;
import com.aelitis.azureus.core.dht.control.DHTControl;
import com.aelitis.azureus.core.dht.transport.DHTTransportContact;
import com.aelitis.azureus.core.dht.transport.DHTTransportValue;

public class GetAction implements Runnable {
	private final int maxValues;
	private final DHTControl control;
	private final Set<String> neighbors;
	private final byte[] key;

	public GetAction(int maxValues, DHTControl control,
			Set<String> neighbors, byte[] key) {
		this.maxValues = maxValues;
		this.control = control;
		this.neighbors = neighbors;
		this.key = key;
	}

	public void run() {
		control.getEncodedKey(key, "LuaActiveDHT Get", (byte)0, maxValues, 60000,
				true, false, new DHTOperationAdapter() {
					Set<String> tmpNeighbors = new HashSet<String>();

					public void read(DHTTransportContact contact,
							DHTTransportValue value) {
						synchronized (tmpNeighbors) {
							tmpNeighbors.add(contact.getExternalAddress()
									.getAddress().getHostAddress());
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