package edu.washington.cs.activedht.db.dhtwrapper;

import java.util.List;

import com.aelitis.azureus.core.dht.transport.DHTTransportContact;
import com.aelitis.azureus.core.dht.transport.DHTTransportValue;

import edu.washington.cs.activedht.db.kahlua.dhtwrapper.NodeWrapper;

/**
 * @author alevy
 *
 */
public class PutOperationAdapter extends UpdateNeighborsOperationAdapter {

	public PutOperationAdapter(List<NodeWrapper> neighbors,
			UpdateNeighborsCallback updateCallback) {
		super(neighbors, updateCallback);
	}
	
	@Override
	public void wrote(DHTTransportContact contact, DHTTransportValue value) {
		addNeighbor(contact);
	}

}
