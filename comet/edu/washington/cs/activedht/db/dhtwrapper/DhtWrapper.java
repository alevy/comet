package edu.washington.cs.activedht.db.dhtwrapper;

import java.util.Collection;
import java.util.HashSet;
import java.util.Queue;
import java.util.Set;

import org.gudy.azureus2.core3.util.HashWrapper;

import com.aelitis.azureus.core.dht.control.DHTControl;
import com.aelitis.azureus.core.dht.transport.DHTTransportContact;

import edu.washington.cs.activedht.db.ActiveDHTDBValue;

public class DhtWrapper {

	private final DHTControl control;
	private final HashWrapper key;
	private final ActiveDHTDBValue value;
	private final Set<DHTTransportContact> neighbors;
	private final Queue<Runnable> postActions;

	public DhtWrapper(DHTControl control, HashWrapper key,
			ActiveDHTDBValue value, Set<DHTTransportContact> neighbors,
			Queue<Runnable> postActions) {
		this.control = control;
		this.key = key;
		this.value = value;
		this.neighbors = neighbors;
		this.postActions = postActions;
	}

	public Collection<String> getNeighbors() {
		synchronized (neighbors) {
			Collection<String> result = new HashSet<String>();
			for (DHTTransportContact neighbor : neighbors) {
				result.add(neighbor.getExternalAddress().toString());
			}
			return result;
		}
	}

	public String getIP() {
		return control.getTransport().getLocalContact().getExternalAddress()
				.toString();
	}

	public long currentTimeInMillis() {
		return System.currentTimeMillis();
	}

	public long lifeInMillis() {
		return currentTimeInMillis() - value.getCreationTime();
	}

	public void get(int maxValues) {
		synchronized (postActions) {
			postActions.offer(new GetAction(maxValues, control, neighbors, key
					.getBytes()));
		}
	}

	public void put(int maxValues) {
		put(maxValues, value);
	}

	public void put(final int maxValues, final Object putValue) {
		synchronized (postActions) {
			postActions.add(new PutAction(putValue, control, neighbors, key
					.getBytes(), value));
		}
	}

}