package edu.washington.cs.activedht.db;

import java.io.File;

import com.aelitis.azureus.core.dht.DHTLogger;
import com.aelitis.azureus.core.dht.DHTStorageAdapter;
import com.aelitis.azureus.core.dht.impl.Test;

import edu.washington.cs.activedht.db.ActiveDHTStorageAdapter;

public class ActiveDHTTestShell extends Test {
	static {
		ActiveDHTInitializer.prepareRuntimeForActiveCode();
	}
	
	@Override
	protected DHTStorageAdapter createStorageAdapter(int network,
			                                         DHTLogger logger,
		                                             File file) {
		return new ActiveDHTStorageAdapter(super.createStorageAdapter(network,
				                                                      logger,
				                                                      file));
	}
	
	public static void main(String args[]) {
		new ActiveDHTTestShell();
	}
}
