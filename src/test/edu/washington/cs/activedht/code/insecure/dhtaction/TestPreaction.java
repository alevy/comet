package edu.washington.cs.activedht.code.insecure.dhtaction;

import java.util.Vector;

import edu.washington.cs.activedht.code.insecure.dhtaction.DHTPreaction;

public class TestPreaction extends DHTPreaction {
	private static final long serialVersionUID = 5959951266185638033L;
	
	private static Vector<Integer> executed_test_preactions = 
		new Vector<Integer>();
	private int x;
	
	public TestPreaction(int x) {
		this.x = x;
	}
	
	@Override
	public void markAsExecuted() {
		executed_test_preactions.add(x);
		super.markAsExecuted();
	}
	
	public static Vector<Integer> getAllExecutions() {
		return executed_test_preactions;
	}
	
	public static void resetAllExecutions() {
		executed_test_preactions.clear();
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null) return false;
		if (o.getClass().getName().equals(this.getClass().getName())) {
			return false;
		}
		return this.x == ((TestPreaction)o).x;
	}

	@Override
	public int hashCode() { return x; }
}
