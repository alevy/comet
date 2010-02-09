package edu.washington.cs.vanish.internal.backend.activedhtimpl;

import edu.washington.cs.activedht.db.ActiveDHTInitializer;
import edu.washington.cs.activedht.expt.ActivePeer;
import edu.washington.cs.vanish.internal.backend.VanishBackendException;
import edu.washington.cs.vanish.internal.backend.vuzeimpl.DHTVanishBackend;
import edu.washington.cs.vanish.internal.backend.vuzeimpl.DHTVanishParams;

public class ActiveDHTVanishBackend extends DHTVanishBackend {
	public ActiveDHTVanishBackend(DHTVanishParams params) {
		super(params);
	}
	
	@Override
	public void init() throws VanishBackendException {
		// First, initialize the active-dht.
		ActiveDHTInitializer.prepareRuntimeForActiveCode(ActivePeer.NA_VALUE_FACTORY_INTERFACE);
		
		// Then, initialize the vanish backend.
		super.init();
	}
}
