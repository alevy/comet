package edu.washington.cs.activedht.db.lua;

import java.util.Collections;
import java.util.Queue;
import java.util.Set;

import org.gudy.azureus2.core3.util.HashWrapper;

import com.aelitis.azureus.core.dht.control.DHTControl;

import edu.washington.cs.activedht.db.DhtWrapper;

public class LuaDhtWrapper implements DhtWrapper {

	private final DHTControl control;
	private final HashWrapper key;
	private final LuaActiveDHTDBValue value;
	private final Set<String> neighbors;
	private final Queue<Runnable> postActions;

	public LuaDhtWrapper(DHTControl control, HashWrapper key,
			LuaActiveDHTDBValue value, Set<String> neighbors,
			Queue<Runnable> postActions) {
		this.control = control;
		this.key = key;
		this.value = value;
		this.neighbors = neighbors;
		this.postActions = postActions;
	}

	public Set<String> getNeighbors() {
		return Collections.unmodifiableSet(this.neighbors);
	}

	public String getIP() {
		return control.getTransport().getLocalContact().getExternalAddress()
				.toString();
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