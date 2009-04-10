package edu.washington.cs.activedht.code.insecure.candefine;

import edu.washington.cs.activedht.code.insecure.ActiveObjectAdapter;
import edu.washington.cs.activedht.code.insecure.dhtaction.AbortOperationAction;
import edu.washington.cs.activedht.code.insecure.dhtaction.DHTActionList;

/**
 * Challenges:
 *   - Replication expands the number of nodes who can see the value before
 *     the release date.
 *
 * @author roxana
 */
public class TimeReleaseActiveObject extends ActiveObjectAdapter {
	private static final long serialVersionUID = -1481297571876799807L;

	private long release_date;

	public TimeReleaseActiveObject(byte[] value, long release_date) {
		super(value);
		this.release_date = release_date;
	}
	
	// ActiveCode interface:
	
	@Override
	public void onGet(String caller_ip, DHTActionList executed_preactions,
			          DHTActionList postactions) {
		if (System.currentTimeMillis() < release_date) {
			// Refuse to release before then.
			try { postactions.addAction(new AbortOperationAction()); }
			catch (Exception e) { }
		}
	}
}
