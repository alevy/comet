package edu.washington.cs.activedht.db.dhtactionexecutor.exedhtaction;

import com.aelitis.azureus.core.dht.control.DHTControl;

import edu.washington.cs.activedht.code.insecure.dhtaction.AbortOperationAction;
import edu.washington.cs.activedht.code.insecure.dhtaction.DHTAction;
import edu.washington.cs.activedht.code.insecure.dhtaction.GetDHTAction;
import edu.washington.cs.activedht.code.insecure.dhtaction.GetIPAction;
import edu.washington.cs.activedht.code.insecure.dhtaction.PutDHTAction;
import edu.washington.cs.activedht.db.dhtactionexecutor.AbortDHTActionException;

public class ExecutableDHTActionFactoryImpl
implements ExecutableDHTActionFactory {
	@Override
	@SuppressWarnings("unchecked")
	public ExecutableDHTAction createAction(DHTAction action,
			                                DHTControl control,
			                                long running_timeout)
	throws AbortDHTActionException, NoSuchDHTActionException {
		if (action instanceof GetIPAction) {
			return new GetIPExecutableAction((GetIPAction)action,
					                         control);
		} else if (action instanceof GetDHTAction) {
			return new GetDHTExecutableAction((GetDHTAction)action,
					                          control,
					                          running_timeout);
		} else if (action instanceof PutDHTAction) {
			return new PutDHTExecutableAction((PutDHTAction)action,
					                          control,
					                          running_timeout);
		} else if (action instanceof AbortOperationAction) {
			throw new AbortDHTActionException();
		}
		
		throw new NoSuchDHTActionException("Unexecutable action.");
	}
}
