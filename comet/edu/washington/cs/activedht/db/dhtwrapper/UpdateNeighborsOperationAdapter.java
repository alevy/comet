/**
 * 
 */
package edu.washington.cs.activedht.db.dhtwrapper;

import java.util.SortedSet;
import java.util.TreeSet;

import com.aelitis.azureus.core.dht.DHTOperationAdapter;
import com.aelitis.azureus.core.dht.transport.DHTTransportContact;

import edu.washington.cs.activedht.db.kahlua.dhtwrapper.NodeWrapper;

public class UpdateNeighborsOperationAdapter extends DHTOperationAdapter {
	private final SortedSet<NodeWrapper> tmpNeighbors = new TreeSet<NodeWrapper>();
	public final SortedSet<NodeWrapper> neighbors;

	private final UpdateNeighborsCallback updateCallback;

	public UpdateNeighborsOperationAdapter(SortedSet<NodeWrapper> neighbors,
			UpdateNeighborsCallback updateCallback) {
		this.neighbors = neighbors;
		this.updateCallback = updateCallback;
	}

	@Override
	public void found(DHTTransportContact contact, boolean isClosest) {
		synchronized (tmpNeighbors) {
			tmpNeighbors.add(new NodeWrapper(contact));
		}
	}
	
	@Override
	public void complete(boolean timeout) {
		synchronized (neighbors) {
			neighbors.clear();
			neighbors.addAll(tmpNeighbors);
		}
		if (updateCallback != null) {
			updateCallback.call(tmpNeighbors);
		}
	}

}