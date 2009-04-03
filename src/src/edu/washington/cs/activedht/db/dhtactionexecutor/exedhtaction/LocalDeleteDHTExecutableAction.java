package edu.washington.cs.activedht.db.dhtactionexecutor.exedhtaction;

import org.gudy.azureus2.core3.util.HashWrapper;

import com.aelitis.azureus.core.dht.DHTOperationListener;

import edu.washington.cs.activedht.code.insecure.dhtaction.LocalDeleteDHTAction;
import edu.washington.cs.activedht.db.ActiveDHTDB;

public class LocalDeleteDHTExecutableAction
extends ExecutableDHTAction<LocalDeleteDHTAction> {	
	public LocalDeleteDHTExecutableAction(LocalDeleteDHTAction action,
			                              ActiveDHTDB db,
			                              HashWrapper key) {
		super(action, db, key);
	}

	@Override
	protected void executeUsingListener(ActiveDHTOperationListener listener) {
		getDB().superRemove(getControl().getTransport().getLocalContact(),
				            getKey());
		listener.complete(false);
	}

	@Override
	protected DHTOperationListener getListener() { return null; }
}
