package edu.washington.cs.activedht.code.insecure.candefine;

import edu.washington.cs.activedht.code.insecure.dhtaction.AbortOperationAction;
import edu.washington.cs.activedht.code.insecure.dhtaction.DHTActionList;
import edu.washington.cs.activedht.code.insecure.dhtaction.DHTActionMap;
import edu.washington.cs.activedht.code.insecure.dhtaction.GetDHTAction;
import edu.washington.cs.activedht.code.insecure.dhtaction.LocalDeleteDHTAction;

/** 
 * Self-destructs after read once.
 * 
 * @author roxana
 */
public class OneTimeActiveObject implements ActiveCode {
	private static final long serialVersionUID = 2944320933345264199L;

	private long grace_period;
	private long self_destruction_initiation_time = 0;  // not initiated yet.

	public OneTimeActiveObject(long grace_time) { 
		this.grace_period = grace_time;
	}
	
	public OneTimeActiveObject() { this(30 * 60000L); }

	@Override
	public void onGet(String caller_ip, DHTActionList executed_preactions,
			          DHTActionList postactions) {
		if (! selfDestructionInitiated()) {
			initiateSelfDestruction();
			if (timeToSelfDestruct()) selfDestruct(postactions);
		} else {
			try { postactions.addAction(new AbortOperationAction()); }
			catch (Exception e) { }
		}
	}

	@Override
	public void onTimer(DHTActionList executed_preactions,
			            DHTActionList postactions) {
		if (selfDestructionInitiated()) {
			// With one of the last breaths, send out the self-destruct
			// announcement to the other replicas.
			try { postactions.addAction(new GetDHTAction(0)); }
			catch (Exception e) { }
			
			// If it's time to self-destruct, do it.
			if (timeToSelfDestruct()) selfDestruct(postactions);
		}  // else, nothing to do.
	}
	
	private void selfDestruct(DHTActionList postactions) {
		try { postactions.addAction(new LocalDeleteDHTAction()); }
		catch (Exception e) { }
	}
	
	private void initiateSelfDestruction() {
		self_destruction_initiation_time = System.currentTimeMillis();
	}
	
	private boolean selfDestructionInitiated() { 
		return self_destruction_initiation_time > 0;
	}
	
	private boolean timeToSelfDestruct() {
		return self_destruction_initiation_time + grace_period >
		       System.currentTimeMillis();
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
