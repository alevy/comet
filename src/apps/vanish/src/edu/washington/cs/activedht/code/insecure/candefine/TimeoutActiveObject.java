package edu.washington.cs.activedht.code.insecure.candefine;

import edu.washington.cs.activedht.code.insecure.dhtaction.AbortOperationAction;
import edu.washington.cs.activedht.code.insecure.dhtaction.DHTActionList;
import edu.washington.cs.activedht.code.insecure.dhtaction.DHTActionMap;
import edu.washington.cs.activedht.code.insecure.dhtaction.LocalDeleteDHTAction;

/**
 * ActiveObject that times out at around a given time. 
 * @author roxana
 */
public class TimeoutActiveObject implements ActiveCode {
	private static final long serialVersionUID = 2944320933345264199L;

	private long self_destruction_time;

	public TimeoutActiveObject(long self_destruction_time) { 
		this.self_destruction_time = self_destruction_time;
	}
	
	@Override
	public void onGet(String caller_ip, DHTActionList executed_preactions,
			          DHTActionList postactions) {
		if (timeToSelfDestruct()) {
            // Initiate the self-destruction.
			selfDestruct(postactions);
			// Prevent this object from being read.
			try { postactions.addAction(new AbortOperationAction()); }
			catch (Exception e) { }
		}  // else, nothing to do.
	}
	
	@Override
	public void onTimer(DHTActionList executed_preactions,
			            DHTActionList postactions) {
		if (timeToSelfDestruct()) selfDestruct(postactions);
	}
	
	private boolean timeToSelfDestruct() {
		return self_destruction_time > System.currentTimeMillis();
	}
	
	private void selfDestruct(DHTActionList postactions) {
		try { postactions.addAction(new LocalDeleteDHTAction()); }
		catch (Exception e) { }
	}
	
	@Override
	public void onDelete(String caller_ip, DHTActionList executed_preactions,
			             DHTActionList postactions) { }
	
	@Override
	public void onValueAdded(String this_node_ip, String caller_ip,
			                 DHTActionMap preactions_map,
			                 DHTActionList postactions) { }

	@Override
	public void onValueChanged(String caller_ip, byte[] plain_new_value,
			                   DHTActionList executed_preactions,
			                   DHTActionList postactions) { }

	@Override
	public void onValueChanged(String caller_ip, ActiveCode new_active_value,
			                   DHTActionList executed_preactions,
			                   DHTActionList postactions) { }
	
	@Override
	public boolean onTest(int value) { return false; }
}
