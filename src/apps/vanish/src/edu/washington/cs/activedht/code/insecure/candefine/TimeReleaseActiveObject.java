package edu.washington.cs.activedht.code.insecure.candefine;

import edu.washington.cs.activedht.code.insecure.dhtaction.AbortOperationAction;
import edu.washington.cs.activedht.code.insecure.dhtaction.DHTActionList;
import edu.washington.cs.activedht.code.insecure.dhtaction.DHTActionMap;

/**
 * Challenges:
 *   - Replication expands the number of nodes who can see the value before
 *     the release date.
 *
 * @author roxana
 */
public class TimeReleaseActiveObject implements ActiveCode {
	private static final long serialVersionUID = -1481297571876799807L;
	
	private long release_date;

	public TimeReleaseActiveObject(long release_date) {
		this.release_date = release_date;
	}
	
	@Override
	public void onDelete(String caller_ip, DHTActionList executed_preactions,
			             DHTActionList postactions) { }

	@Override
	public void onGet(String caller_ip, DHTActionList executed_preactions,
			          DHTActionList postactions) {
		if (System.currentTimeMillis() < release_date) {
			// Refuse to release before then.
			try { postactions.addAction(new AbortOperationAction()); }
			catch (Exception e) { }
		}
	}

	@Override
	public void onTimer(DHTActionList executed_preactions,
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
