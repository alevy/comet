package edu.washington.cs.activedht.code.insecure.candefine;

import edu.washington.cs.activedht.code.insecure.ActiveObjectAdapter;
import edu.washington.cs.activedht.code.insecure.dhtaction.AbortOperationAction;
import edu.washington.cs.activedht.code.insecure.dhtaction.DHTActionList;
import edu.washington.cs.activedht.code.insecure.dhtaction.LocalDeleteDHTAction;

/**
 * ActiveObject that times out at around a given time.
 * 
 * @author roxana
 */
public class TimeoutActiveObject extends ActiveObjectAdapter {
	private static final long serialVersionUID = 2944320933345264199L;

	private long self_destruction_time;

	public TimeoutActiveObject(byte[] value, long self_destruction_date) {
		super(value);
		this.self_destruction_time = self_destruction_date;
	}
	
	// ActiveCode interface:
	
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
	
	// Helper functions:
	
	private boolean timeToSelfDestruct() {
		return self_destruction_time > System.currentTimeMillis();
	}
	
	private void selfDestruct(DHTActionList postactions) {
		try { postactions.addAction(new LocalDeleteDHTAction()); }
		catch (Exception e) { }
	}
}
