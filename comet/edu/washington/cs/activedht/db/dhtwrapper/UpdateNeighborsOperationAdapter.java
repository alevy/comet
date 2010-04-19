/**
 * 
 */
package edu.washington.cs.activedht.db.dhtwrapper;

import java.util.ArrayList;
import java.util.List;

import com.aelitis.azureus.core.dht.DHTOperationAdapter;
import com.aelitis.azureus.core.dht.transport.DHTTransportContact;

import edu.washington.cs.activedht.db.kahlua.dhtwrapper.NodeWrapper;

public class UpdateNeighborsOperationAdapter extends DHTOperationAdapter {
	protected final List<NodeWrapper> tmpNeighbors = new ArrayList<NodeWrapper>();
	public final List<NodeWrapper> neighbors;

	private final UpdateNeighborsCallback updateCallback;

	public UpdateNeighborsOperationAdapter(List<NodeWrapper> neighbors,
			UpdateNeighborsCallback updateCallback) {
		this.neighbors = neighbors;
		this.updateCallback = updateCallback;
	}

	@Override
	public void found(DHTTransportContact contact, boolean isClosest) {
		addNeighbor(contact);
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

	protected void addNeighbor(DHTTransportContact contact) {
		synchronized (tmpNeighbors) {
			if (!tmpNeighbors.contains(contact)) {
				tmpNeighbors.add(new NodeWrapper(contact));
			}
		}
	}

}