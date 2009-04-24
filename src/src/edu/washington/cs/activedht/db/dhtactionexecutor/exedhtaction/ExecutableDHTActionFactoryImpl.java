package edu.washington.cs.activedht.db.dhtactionexecutor.exedhtaction;

import org.gudy.azureus2.core3.util.HashWrapper;

import edu.washington.cs.activedht.code.insecure.dhtaction.AbortOperationAction;
import edu.washington.cs.activedht.code.insecure.dhtaction.DHTAction;
import edu.washington.cs.activedht.code.insecure.dhtaction.GetDHTAction;
import edu.washington.cs.activedht.code.insecure.dhtaction.GetIPAction;
import edu.washington.cs.activedht.code.insecure.dhtaction.LocalDeleteDHTAction;
import edu.washington.cs.activedht.code.insecure.dhtaction.PutDHTAction;
import edu.washington.cs.activedht.db.ActiveDHTDB;
import edu.washington.cs.activedht.db.ActiveDHTDBValueImpl;
import edu.washington.cs.activedht.db.dhtactionexecutor.AbortDHTActionException;

public class ExecutableDHTActionFactoryImpl
implements ExecutableDHTActionFactory {
	@Override
	@SuppressWarnings("unchecked")
	public ExecutableDHTAction createAction(DHTAction action,
			                                HashWrapper key,
			                                ActiveDHTDBValueImpl value,
			                                ActiveDHTDB db,
			                                long running_timeout)
	throws AbortDHTActionException, NoSuchDHTActionException {
		
		if (action instanceof GetIPAction) {
			return new GetIPExecutableAction((GetIPAction)action, db);
		} else if (action instanceof GetDHTAction) {
			return new GetDHTExecutableAction((GetDHTAction)action, db, key,
					                          running_timeout);
		} else if (action instanceof PutDHTAction) {
			return new PutDHTExecutableAction((PutDHTAction)action, db, key,
					                          running_timeout);
		} else if (action instanceof LocalDeleteDHTAction) {
			return new LocalDeleteDHTExecutableAction(
					(LocalDeleteDHTAction)action, db, key);
	    } else if (action instanceof AbortOperationAction) {
			throw new AbortDHTActionException();
		}
		
		throw new NoSuchDHTActionException("Unexecutable action.");
	}
}
