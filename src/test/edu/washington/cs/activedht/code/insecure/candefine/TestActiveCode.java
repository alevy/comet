package edu.washington.cs.activedht.code.insecure.candefine;

import edu.washington.cs.activedht.code.insecure.DHTEvent;
import edu.washington.cs.activedht.code.insecure.candefine.ActiveCode;
import edu.washington.cs.activedht.code.insecure.dhtaction.DHTAction;
import edu.washington.cs.activedht.code.insecure.dhtaction.DHTActionList;
import edu.washington.cs.activedht.code.insecure.dhtaction.DHTActionMap;

public class TestActiveCode implements ActiveCode {
	private static final long serialVersionUID = 13579424689L;
	
	private int value = 0;
	private DHTEvent event_with_preaction;
	
	private DHTAction preaction;
	private DHTAction postaction;
	
	public TestActiveCode(DHTEvent event_with_preaction,
			              DHTAction preaction,
			              DHTAction postaction) {
		this.event_with_preaction = event_with_preaction;
		this.preaction = preaction;
		this.postaction = postaction;
	}

	// ActiveCode interface:

	@Override
	public void onValueAdded(String this_node_ip, String caller_ip,
			DHTActionMap preactions_map,
			DHTActionList postactions) {
		onAnyEvent(null, postactions);
		try {
			preactions_map.addPreactionToEvent(event_with_preaction,
				preaction);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	@Override
	public void onValueChanged(String caller_ip, byte[] plain_new_value,
			DHTActionList executed_preactions,
			DHTActionList postactions) {
		onAnyEvent(executed_preactions, postactions);
	}
	
	@Override
	public void onValueChanged(String caller_ip, ActiveCode new_active_value,
			DHTActionList executed_preactions,
			DHTActionList postactions) {
		onAnyEvent(executed_preactions, postactions);
	}
	
	@Override
	public void onGet(String caller_ip,
			DHTActionList executed_preactions,
			DHTActionList postactions) {
		onAnyEvent(executed_preactions, postactions);
	}
	
	@Override
	public void onDelete(String caller_ip,
			DHTActionList executed_preactions,
			DHTActionList postactions) {
		onAnyEvent(executed_preactions, postactions);
	}

	@Override
	public void onTimer(DHTActionList executed_preactions,
			DHTActionList postactions) {
		onAnyEvent(executed_preactions, postactions);
	}
	
	public boolean onTest(int value) { return this.value == value; }
	
	// Helper functions:
	
	protected void onAnyEvent(DHTActionList executed_preactions,
			                  DHTActionList postactions) {
		if (executed_preactions == null ||
			executed_preactions.size() == 0 ||
			executed_preactions.getAction(0).actionWasExecuted()) {
			++value;
			try { postactions.addAction(postaction); }
			catch (Exception e) { e.printStackTrace(); }
		}  // else, don't do anything.
	}
	
	// Accessors:
	
	public DHTAction getPreaction() { return preaction; }
	
	public DHTAction getPostaction() { return postaction; }
	
	// Object functions:
	
	@Override
	public boolean equals(Object other) {
		if (other == null) return false;
		if (other == this) return true;
		if (! other.getClass().getName().equals(this.getClass().getName())) {
			return false;
		}
		TestActiveCode o = (TestActiveCode)other;
		return this.value == o.value;
	}
	
	@Override
	public int hashCode() { return value; }
}
