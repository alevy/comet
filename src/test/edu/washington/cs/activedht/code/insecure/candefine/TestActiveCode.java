package edu.washington.cs.activedht.code.insecure.candefine;

import edu.washington.cs.activedht.code.insecure.DHTEvent;
import edu.washington.cs.activedht.code.insecure.candefine.ActiveCode;
import edu.washington.cs.activedht.code.insecure.dhtaction.DHTActionList;
import edu.washington.cs.activedht.code.insecure.dhtaction.DHTActionMap;
import edu.washington.cs.activedht.code.insecure.dhtaction.DHTPostaction;
import edu.washington.cs.activedht.code.insecure.dhtaction.DHTPreaction;
import edu.washington.cs.activedht.code.insecure.dhtaction.TestPostaction;
import edu.washington.cs.activedht.code.insecure.dhtaction.TestPreaction;

public class TestActiveCode implements ActiveCode {
	private static final long serialVersionUID = 13579424689L;
	
	private int value = 0;
	private DHTEvent event_with_preaction;
	
	private TestPreaction preaction;
	private TestPostaction postaction;
	
	/** De-serialization constructor. */
	public TestActiveCode(DHTEvent event_with_preaction) {
		this.event_with_preaction = event_with_preaction;
		preaction  = new TestPreaction();
		postaction = new TestPostaction();
	}

	// ActiveCode interface:

	public void onInitialPut(String caller_ip,
			DHTActionMap<DHTPreaction> preactions_map) {
		System.out.println("OnInitialPut()");
		try {
			preactions_map.addPreactionToEvent(event_with_preaction,
				preaction);
		} catch (Exception e) { e.printStackTrace(); }
	}
	
	public void onPut(String caller_ip, byte[] plain_new_value,
			DHTActionList<DHTPreaction> executed_preactions,
			DHTActionList<DHTPostaction> postactions) {
		onAnyEvent(executed_preactions, postactions);
	}
	
	public void onPut(String caller_ip, ActiveCode new_active_value,
			DHTActionList<DHTPreaction> executed_preactions,
			DHTActionList<DHTPostaction> postactions) {
		onAnyEvent(executed_preactions, postactions);
	}
	
	public void onGet(String caller_ip,
			DHTActionList<DHTPreaction> executed_preactions,
			DHTActionList<DHTPostaction> postactions) {
		onAnyEvent(executed_preactions, postactions);
	}
	
	public void onDelete(String caller_ip,
			DHTActionList<DHTPreaction> executed_preactions,
			DHTActionList<DHTPostaction> postactions) {
		onAnyEvent(executed_preactions, postactions);
	}

	public void onTimer(DHTActionList<DHTPreaction> executed_preactions,
			DHTActionList<DHTPostaction> postactions) {
		onAnyEvent(executed_preactions, postactions);
	}
	
	public boolean onTest(int value) { return this.value == value; }
	
	// Helper functions:
	
	private void onAnyEvent(DHTActionList<DHTPreaction> executed_preactions,
			                DHTActionList<DHTPostaction> postactions) {
		if (executed_preactions.size() > 0 &&
			executed_preactions.getAction(0).actionWasExecuted()) {
			++value;
			try { postactions.addAction(postaction); }
			catch (Exception e) { e.printStackTrace(); }
		}  // else, don't do anything.
	}
	
	// Accessors:
	
	public TestPreaction getPreaction() { return preaction; }
	
	public TestPostaction getPostaction() { return postaction; }
	
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
