package edu.washington.cs.activedht.db.dhtactionexecutor.exedhtaction;

import org.gudy.azureus2.core3.util.HashWrapper;

import edu.washington.cs.activedht.code.insecure.dhtaction.DHTAction;
import edu.washington.cs.activedht.db.ActiveDHTDB;
import edu.washington.cs.activedht.db.dhtactionexecutor.AbortDHTActionException;

/**
 * Add here whenever you add a new DHTAction type.
 * 
 * TODO(roxana): We could also make this be automatic, using some naming
 * convention.
 * 
 * @author roxana
 */
public interface ExecutableDHTActionFactory {
	@SuppressWarnings("unchecked")
	public ExecutableDHTAction createAction(DHTAction action,
			                                HashWrapper key,
                                            ActiveDHTDB control,
                                            long running_timeout)
	throws AbortDHTActionException, NoSuchDHTActionException;
}
