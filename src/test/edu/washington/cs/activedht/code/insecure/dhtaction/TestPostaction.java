package edu.washington.cs.activedht.code.insecure.dhtaction;

import java.util.Vector;

public class TestPostaction extends DHTAction {
	private static final long serialVersionUID = 4192376659437104309L;

	private static Vector<Integer> executed_test_preactions = 
		new Vector<Integer>();
	
	private int x;
	
	public TestPostaction(int x) { this.x = x; }
	
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
		return this.x == ((TestPostaction)o).x;
	}

	@Override
	public int hashCode() { return x; }
}
