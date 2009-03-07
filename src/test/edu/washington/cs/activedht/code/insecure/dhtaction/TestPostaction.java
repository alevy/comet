package edu.washington.cs.activedht.code.insecure.dhtaction;

import edu.washington.cs.activedht.code.insecure.dhtaction.DHTPostaction;

public class TestPostaction extends DHTPostaction {
	private static final long serialVersionUID = 4192376659437104309L;
	
	private static int num_times_executed_ever = 0;

	public TestPostaction() { }
	
	@Override
	public void markAsExecuted() {
		++num_times_executed_ever;
		super.markAsExecuted();
	}
	
	public static int getNumTimesExecuted() { return num_times_executed_ever; }
}
