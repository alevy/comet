package edu.washington.cs.activedht.db.dhtactionexecutor;

import org.gudy.azureus2.core3.util.HashWrapper;

import edu.washington.cs.activedht.code.insecure.Initializable;
import edu.washington.cs.activedht.code.insecure.dhtaction.DHTActionList;
import edu.washington.cs.activedht.code.insecure.exceptions.ActiveCodeExecutionInterruptedException;

public interface DHTActionExecutor extends Initializable {
	/**
	 * Executes the given list of actions and waits for their results.
	 * 
	 * @param actions
	 * @param execution_timeout
	 * @param wait_for_the_results
	 * @throws ActiveCodeExecutionInterruptedException if the running is
	 * interrupted due to timeout.
	 * @throws AbortDHTActionException if one of the preactions requested
	 * abort.
	 */
	@SuppressWarnings("unchecked")
	public void executeActions(DHTActionList actions, HashWrapper key,
			                   long execution_timeout,
			                   boolean wait_for_responses)
	throws ActiveCodeExecutionInterruptedException, AbortDHTActionException;
	
	public String getThisHostAddr();
}
