package edu.washington.cs.activedht.code.insecure.candefine;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import edu.washington.cs.activedht.code.insecure.dhtaction.DHTActionList;
import edu.washington.cs.activedht.code.insecure.dhtaction.DHTActionMap;

public class ForensicTrailActiveObject implements ActiveCode {
	private static final long serialVersionUID = 1L;

	private byte[] actual_value;
	
	private Set<String> replica_ips;
	private Set<String> accessor_ips;
	
	public ForensicTrailActiveObject(byte[] actual_value) {
		this.actual_value = actual_value;
		this.replica_ips = Collections.synchronizedSet(new HashSet<String>());
		this.accessor_ips = Collections.synchronizedSet(new HashSet<String>());
	}
	
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

	@Override
	public void onValueChanged(String caller_ip,
			                   byte[] plain_new_value,
			                   DHTActionList executed_preactions,
			                   DHTActionList postactions) { }

	@Override
	public void onValueChanged(String caller_ip,
			                   ActiveCode new_active_value,
			                   DHTActionList executed_preactions,
			                   DHTActionList postactions) { }
	
	@Override
	public void onTimer(DHTActionList executed_preactions,
			            DHTActionList postactions) { }
	
	@Override
	public void onDelete(String caller_ip,
			             DHTActionList executed_preactions,
			             DHTActionList postactions) { }
	
	@Override
	public boolean onTest(int value) { return false; }
	
	@Override
	public String toString() {
		return new String(actual_value) + " (replicated by: " +
			              replica_ips + "; read by: " + accessor_ips + ")";
	}
}
