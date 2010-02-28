/**
 * 
 */
package edu.washington.cs.activedht.db.dhtwrapper;

import java.util.HashSet;
import java.util.Set;

import org.gudy.azureus2.core3.util.HashWrapper;

import com.aelitis.azureus.core.dht.DHTOperationAdapter;
import com.aelitis.azureus.core.dht.transport.DHTTransportContact;

import edu.washington.cs.activedht.db.kahlua.dhtwrapper.NodeWrapper;

public class UpdateNeighborsOperationAdapter extends
		DHTOperationAdapter {
	private final Set<NodeWrapper> tmpNeighbors = new HashSet<NodeWrapper>();
	public final Set<NodeWrapper> neighbors;

	public UpdateNeighborsOperationAdapter(Set<NodeWrapper> neighbors) {
		this.neighbors = neighbors;
	}
	
	public void found(DHTTransportContact contact) {
		synchronized (tmpNeighbors) {
			tmpNeighbors.add(new NodeWrapper(contact, new HashWrapper(
					contact.getID())));
		}
	}


	public void complete(boolean timeout) {
		synchronized (neighbors) {
			neighbors.clear();
			neighbors.addAll(tmpNeighbors);
		}
	}

}