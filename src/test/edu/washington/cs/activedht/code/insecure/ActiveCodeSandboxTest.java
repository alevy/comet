package edu.washington.cs.activedht.code.insecure;

import java.util.concurrent.Callable;

import junit.framework.TestCase;

class TestClosure implements Callable<Integer> {
	private static int value = 0;
	private long sleep_time_ms;

	public TestClosure(long sleep_time_ms) {
		this.sleep_time_ms = sleep_time_ms;
	}
	
	public Integer call() throws Exception {
		Thread.sleep(sleep_time_ms);  // sleep for some ms and then return.  
		return ++value;
	}
}

public class ActiveCodeSandboxTest extends TestCase {
	private ActiveCodeSandbox<Integer> sandbox;
	
	@Override
	public void setUp() {
		sandbox = new ActiveCodeSandbox<Integer>(1000);
	}
	
	@Override
	public void tearDown() {
		// TODO(roxana): Stop the threads... 
	}
	
	public void testSandboxWithNiceComputation() {
		Integer result = sandbox.executeWithinSandbox(new TestClosure(10));
		assertNotNull(result);
		assertEquals(1, result.intValue());
		assertEquals(0, sandbox.getNumPendingTasks());
	}
	
	public void testSandboxTimesOutForTooLongComputation() {
		Integer result = sandbox.executeWithinSandbox(new TestClosure(10000));
		// Must have timed out.
		assertNull(result);
        // assertEquals(0, sandbox.getNumPendingTasks());
	}
}
