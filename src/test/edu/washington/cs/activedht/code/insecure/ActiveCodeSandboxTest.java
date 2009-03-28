package edu.washington.cs.activedht.code.insecure;

import java.util.concurrent.Callable;

import edu.washington.cs.activedht.code.insecure.exceptions.ActiveCodeExecutionInterruptedException;
import edu.washington.cs.activedht.code.insecure.exceptions.InitializationException;
import edu.washington.cs.activedht.code.insecure.sandbox.ActiveCodeSandboxImpl;

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
	private ActiveCodeSandboxImpl<Integer> sandbox;
	
	@Override
	public void setUp() {
		sandbox = new ActiveCodeSandboxImpl<Integer>(1000);
		try {
			sandbox.init();
		} catch (InitializationException e) {
			e.printStackTrace();
			fail("Failed to init sandbox.");
		}
	}
	
	@Override
	public void tearDown() {
		// TODO(roxana): Stop the threads... 
	}
	
	public void testSandboxWithNiceComputation() {
		Integer result = null;
		try {
			result = sandbox.executeWithinSandbox(new TestClosure(10));
		} catch (ActiveCodeExecutionInterruptedException e) {
			e.printStackTrace();
			fail("Execution timed out when it shouldn't have.");
		}
		assertNotNull(result);
		assertEquals(1, result.intValue());
		assertEquals(0, sandbox.getNumPendingTasks());
	}
	
	public void testSandboxTimesOutForTooLongComputation() {
		Integer result = null;
		try {
			result = sandbox.executeWithinSandbox(new TestClosure(10000));
			fail("Execution succeeded when it shouldn't have.");
		} catch (ActiveCodeExecutionInterruptedException e) { }  // expected.
        // assertEquals(0, sandbox.getNumPendingTasks());  // TODO(roxana)
	}
}
