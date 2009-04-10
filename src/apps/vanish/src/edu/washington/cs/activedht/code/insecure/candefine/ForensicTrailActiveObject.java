package edu.washington.cs.activedht.code.insecure.candefine;

import java.util.HashSet;
import java.util.Set;

import edu.washington.cs.activedht.code.insecure.ActiveObjectAdapter;
import edu.washington.cs.activedht.code.insecure.dhtaction.DHTActionList;
import edu.washington.cs.activedht.code.insecure.dhtaction.DHTActionMap;

public class ForensicTrailActiveObject extends ActiveObjectAdapter {
	private static final long serialVersionUID = -2075342456379803964L;
	
	private Set<String> replica_ips;
	private Set<String> accessor_ips;
	
	public ForensicTrailActiveObject(byte[] actual_value) {
		super(actual_value);
		this.replica_ips = new HashSet<String>();
		this.accessor_ips = new HashSet<String>();
	}
	
	// Accessors:
	
	public Set<String> getReplicaIPs() { return replica_ips; }
	
	public Set<String> getAccessorIPs() { return accessor_ips; }
	
	// ActiveCode interface:

	@Override
	public void onGet(String caller_ip,
			          DHTActionList executed_preactions,
			          DHTActionList postactions) {
		accessor_ips.add(caller_ip);
	}

	@Override
	public void onValueAdded(String this_node_ip, String caller_ip,
			                 DHTActionMap preactions_map,
			                 DHTActionList postactions) {
		accessor_ips.add(this_node_ip);
		replica_ips.add(this_node_ip);
	}
}
