package edu.washington.cs.activedht.code.insecure.candefine;

import java.util.ArrayList;
import java.util.List;

import edu.washington.cs.activedht.code.insecure.DHTEvent;
import edu.washington.cs.activedht.code.insecure.dhtaction.DHTActionList;
import edu.washington.cs.activedht.code.insecure.dhtaction.DHTActionMap;
import edu.washington.cs.activedht.code.insecure.dhtaction.GetIPAction;

public class AccessCountingActiveCode implements ActiveCode {
	private static final long serialVersionUID = 7396244516476894743L;
	
	private String id;
	private List<String> accessor_ips;
	
	public AccessCountingActiveCode(String value) {
		this.id = value;
		this.accessor_ips = new ArrayList<String>();
	}

	public void onDelete(String caller_ip, DHTActionList executed_preactions,
			             DHTActionList postactions) {
	}

	public void onGet(String caller_ip, DHTActionList executed_preactions,
			          DHTActionList postactions) {
		if (executed_preactions.size() > 0) {
			GetIPAction action = (GetIPAction)executed_preactions.getAction(0);
			if (action.actionWasExecuted()) {
				System.out.println("ActiveObject owned by: " + action.getIP());
			}
		}
		accessor_ips.add(caller_ip);
	}

	public boolean onTest(int value) { return accessor_ips.size() == value; }

	public void onTimer(DHTActionList executed_preactions,
			            DHTActionList postactions) {
	}

	public void onValueAdded(String this_node_ip, String caller_ip,
			                 DHTActionMap preactions_map,
			                 DHTActionList postactions) {
		try { 
			preactions_map.addPreactionToEvent(DHTEvent.GET,
					                           new GetIPAction());
		} catch (Exception e) { }
	}

	public void onValueChanged(String caller_ip, byte[] plain_new_value,
			                   DHTActionList executed_preactions,
			                   DHTActionList postactions) {
	}

	public void onValueChanged(String caller_ip, ActiveCode new_active_value,
			                   DHTActionList executed_preactions,
			                   DHTActionList postactions) {
	}
	
	public String toString() {
		return "Value " + id + " (seen by: " + accessor_ips + ")";
	}
}
