package edu.washington.cs.activedht.db.dhtactionexecutor;

import edu.washington.cs.activedht.code.insecure.Initializable;
import edu.washington.cs.activedht.code.insecure.dhtaction.DHTActionList;
import edu.washington.cs.activedht.code.insecure.exceptions.ActiveCodeExecutionInterruptedException;

public interface DHTActionExecutor extends Initializable {
	/**
	 * Executes the given list of actions and waits for their results.
	 * 
	 * @param actions
	 * @param execution_timeout
	 * @throws ActiveCodeExecutionInterruptedException if the running is
	 * interrupted due to timeout.
	 * @throws AbortDHTActionException if one of the preactions requested
	 * abort.
	 */
	@SuppressWarnings("unchecked")
	public void executeActions(DHTActionList actions, long execution_timeout)
	throws ActiveCodeExecutionInterruptedException, AbortDHTActionException;
}
