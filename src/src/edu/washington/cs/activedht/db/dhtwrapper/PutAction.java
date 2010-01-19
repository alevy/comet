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
import com.aelitis.azureus.core.dht.transport.DHTTransportValue;

import edu.washington.cs.activedht.db.ActiveDHTDBValue;
import edu.washington.cs.activedht.db.lua.LuaActiveDHTDBValue;

public class PutAction implements Runnable {
	private final Object putValue;
	private final DHTControl control;
	private final Set<String> neighbors;
	private final byte[] key;
	private final ActiveDHTDBValue value;

	public PutAction(Object putValue, DHTControl control,
			Set<String> neighbors, byte[] key, ActiveDHTDBValue value) {
		this.putValue = putValue;
		this.control = control;
		this.neighbors = neighbors;
		this.key = key;
		this.value = value;
	}

	public void run() {
		byte[] result = null;
		if (LuaActiveDHTDBValue.class.isInstance(putValue)) {
			result = LuaActiveDHTDBValue.class.cast(putValue).getValue();
		} else {
			result = value.serialize(putValue);
		}
		control.put(key, "ActiveDHT Put",
				result,
				(byte)0, true, new DHTOperationAdapter() {
					Set<String> tmpNeighbors = Collections
							.synchronizedSet(new HashSet<String>());

					public void wrote(DHTTransportContact contact,
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