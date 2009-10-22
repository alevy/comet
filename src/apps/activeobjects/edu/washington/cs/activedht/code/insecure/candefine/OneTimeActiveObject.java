package edu.washington.cs.activedht.code.insecure.candefine;

import edu.washington.cs.activedht.code.insecure.ActiveObjectAdapter;
import edu.washington.cs.activedht.code.insecure.dhtaction.AbortOperationAction;
import edu.washington.cs.activedht.code.insecure.dhtaction.DHTActionList;
import edu.washington.cs.activedht.code.insecure.dhtaction.GetDHTAction;
import edu.washington.cs.activedht.code.insecure.dhtaction.LocalDeleteDHTAction;

/** 
 * Self-destructs after read once.
 * 
 * @author roxana
 */
public class OneTimeActiveObject extends ActiveObjectAdapter {
	private static final long serialVersionUID = 2944320933345264199L;
	
	private long grace_period;
	private long self_destruction_initiation_time = 0;  // not initiated yet.

	public OneTimeActiveObject(byte[] value, long grace_period) {
		super(value);
		this.grace_period = grace_period;
	}
	
	public OneTimeActiveObject(byte[] value) { this(value, 30 * 60000L); }

	// ActiveCode interface:
	
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
			//try { postactions.addAction(new GetDHTAction(20)); }
			//catch (Exception e) { }
			
			// If it's time to self-destruct, do it.
			if (timeToSelfDestruct()) selfDestruct(postactions);
		}  // else, nothing to do.
	}
	
	// Helper functions:
	
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
		return self_destruction_initiation_time + grace_period <
		       System.currentTimeMillis();
	}
}
