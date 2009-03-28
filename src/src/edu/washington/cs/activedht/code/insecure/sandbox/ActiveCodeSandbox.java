package edu.washington.cs.activedht.code.insecure.sandbox;

import java.util.concurrent.Callable;

import edu.washington.cs.activedht.code.insecure.Initializable;
import edu.washington.cs.activedht.code.insecure.exceptions.ActiveCodeExecutionInterruptedException;

/**
 * The sandbox for running active code securely.
 * 
 * Two dimensions of sandboxing:
 * 1. Security:
 *    - The sandboxed code can only access a tiny set of resources.
 *
 * 2. Resouce isolation:
 *    - The sandbox is limited in terms of memory consumption and runtime.
 *
 * @author roxana
 */
public interface ActiveCodeSandbox<RETURN_TYPE> extends Initializable {	
	/**
	 * Executes a task within the sandbox, waits for it to finish, and returns
	 * the result.
	 * 
	 * This function is called from outside the sandbox.
	 * @param task
	 * @return
	 * @throws InterruptedException  if the task was interrupted due to any
	 * reasons, like 
	 */
	public RETURN_TYPE executeWithinSandbox(Callable<RETURN_TYPE> task)
	throws ActiveCodeExecutionInterruptedException;
}
