package edu.washington.cs.activedht.code.insecure.candefine;

import edu.washington.cs.activedht.code.insecure.ActiveObjectAdapter;
import edu.washington.cs.activedht.code.insecure.DHTEvent;
import edu.washington.cs.activedht.code.insecure.dhtaction.DHTActionList;
import edu.washington.cs.activedht.code.insecure.dhtaction.DHTActionMap;
import edu.washington.cs.activedht.code.insecure.dhtaction.GetDHTAction;
import edu.washington.cs.activedht.code.insecure.dhtaction.PutDHTAction;
import edu.washington.cs.activedht.code.insecure.dhtaction.ReplicateValueDHTAction;

public class SelfRegulatingReplicationObject extends ActiveObjectAdapter {
	private static final long serialVersionUID = 7658526247629146684L;
	
	private int replication_threshold;
	private int replication_factor;
	private long replication_interval;
	
	private long next_replication_date;
	
	public SelfRegulatingReplicationObject(byte[] value, int replication_threshold,
			                        int replication_factor,
			                        long replication_interval) {
		super(value);
		this.replication_threshold = replication_threshold;
		this.replication_factor    = replication_factor;
		this.replication_interval  = replication_interval;
	}
	
	// ActiveCode interface:

	@Override
	public void onValueAdded(String this_node_ip, String caller_ip,
			                 DHTActionMap preactions_map,
			                 DHTActionList postactions) {
		try {
			preactions_map.addPreactionToEvent(DHTEvent.TIMER,
					new GetDHTAction(replication_factor));
		} catch (Exception e) { }
		
		markAsReplicated();  // at the beginning, assume already replicated.
	}
	
	@Override
	public void onTimer(DHTActionList executed_preactions,
			            DHTActionList postactions) {
		if (shouldReplicate(executed_preactions)) {
			// Replicate this value now.
			try { postactions.addAction(new ReplicateValueDHTAction()); }
			catch (Exception e) { }
			
			markAsReplicated();  // no way of knowing if we actually will
			                     // be replicated, but assume we will.
		}  // else, nothing to do.
	}
	
	// Helper functions:
	
	private boolean shouldReplicate(DHTActionList executed_preactions) {
		if (System.currentTimeMillis() < next_replication_date) return false;
		
		// Check how many replicas I was able to access.
		PutDHTAction action = (PutDHTAction)executed_preactions.getAction(0);
		if (! action.actionWasExecuted()) return true;  // they didn't execute
				// my preaction; replicate to be on the safe side.

		return action.getResponses().size() <= replication_threshold;
	}
	
	private void markAsReplicated() {
		next_replication_date = System.currentTimeMillis() +
		                        replication_interval;
	}
}
