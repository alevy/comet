/**
 * 
 */
package edu.washington.cs.activedht.db.dhtwrapper;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

import org.gudy.azureus2.core3.util.HashWrapper;

import com.aelitis.azureus.core.dht.control.DHTControl;
import com.aelitis.azureus.core.dht.transport.DHTTransportContact;
import com.aelitis.azureus.core.dht.transport.DHTTransportValue;

import edu.washington.cs.activedht.db.kahlua.dhtwrapper.NodeWrapper;
import edu.washington.cs.activedht.transport.BasicDHTTransportValue;

public class PutAction implements Runnable {
	public final byte[] value;
	public final DHTControl control;
	public final HashWrapper key;
	public final List<NodeWrapper> nodes;
	public final int numNodes;
	public final UpdateNeighborsOperationAdapter operationAdapter;

	public PutAction(HashWrapper key, byte[] value, int numNodes,
			DHTControl control, UpdateNeighborsOperationAdapter operationAdapter) {
		this(key, value, numNodes, null, control, operationAdapter);
	}

	public PutAction(HashWrapper key, byte[] value, List<NodeWrapper> nodes,
			DHTControl control, UpdateNeighborsOperationAdapter operationAdapter) {
		this(key, value, 0, nodes, control, operationAdapter);
	}

	private PutAction(HashWrapper key, byte[] value, int numNodes,
			List<NodeWrapper> nodes, DHTControl control,
			UpdateNeighborsOperationAdapter operationAdapter) {
		this.numNodes = numNodes;
		this.nodes = nodes;
		this.control = control;
		this.key = key;
		this.value = value;
		this.operationAdapter = operationAdapter;
	}

	public void run() {
		final DHTTransportValue[][] valueSets = new DHTTransportValue[][] { new DHTTransportValue[] { new BasicDHTTransportValue(
				System.currentTimeMillis(), value, "", 1, control
						.getTransport().getLocalContact(), false, 0) } };

		final byte[][] keys = new byte[][] { key.getBytes() };

		if (nodes != null) {
			control.putDirectEncodedKeys(keys, "ActiveDHT Put", valueSets,
					nodes, operationAdapter);
		} else {
			control.lookupEncoded(key.getBytes(), "ActiveDHT Lookup", 0, false,
					new UpdateNeighborsOperationAdapter(
							new TreeSet<NodeWrapper>(),
							new UpdateNeighborsCallback() {

								@Override
								public void call(SortedSet<NodeWrapper> tmpNeighbors) {
									List<DHTTransportContact> neighbors = new ArrayList<DHTTransportContact>();
									Iterator<NodeWrapper> itr = tmpNeighbors
											.iterator();
									for (int i = 0; i < numNodes
											&& itr.hasNext(); ++i) {
										neighbors.add(itr.next().contact);
									}
									control.putDirectEncodedKeys(keys,
											"ActiveDHT Put", valueSets,
											neighbors, operationAdapter);

								}
							}));
		}
	}
}