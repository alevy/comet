package edu.washington.cs.activedht.code.insecure.dhtaction;

import edu.washington.cs.activedht.code.insecure.dhtaction.DHTPreaction;

public class TestPreaction extends DHTPreaction {
	private static final long serialVersionUID = 5959951266185638033L;
	
	private static int num_times_executed_ever = 0;

	public TestPreaction() { }
	
	@Override
	public void markAsExecuted() {
		++num_times_executed_ever;
		super.markAsExecuted();
	}
	
	public static int getNumTimesExecuted() { return num_times_executed_ever; }
}
