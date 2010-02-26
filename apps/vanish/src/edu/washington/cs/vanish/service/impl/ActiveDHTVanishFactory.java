package edu.washington.cs.vanish.service.impl;

import edu.washington.cs.vanish.internal.MetadataEncapsulatedVanishInternalInterface;
import edu.washington.cs.vanish.internal.VanishException;
import edu.washington.cs.vanish.internal.backend.VanishBackendInterface;
import edu.washington.cs.vanish.internal.backend.activedhtimpl.ActiveDHTVanishBackend;
import edu.washington.cs.vanish.internal.crypto.SecretSharing;
import edu.washington.cs.vanish.internal.crypto.SymmetricEncDec;
import edu.washington.cs.vanish.internal.impl.ActiveDHTVanishInternalImpl;

/**
 * 
 * @author roxana
 *
 */
public class ActiveDHTVanishFactory extends DefaultVanishFactory {
	/**
	 * For a VanishFactory class to be passable to a VanishServer (-e option),
	 * it must define this exact constructor.
	 * @param params
	 */
	public ActiveDHTVanishFactory(VanishServerParams params) { super(params); }
	
	// Modify the VanishInternal implementation and the backend.
	
	@Override
	protected MetadataEncapsulatedVanishInternalInterface
	instantiateVanishInternal(VanishBackendInterface vanish_backend,
			                  SymmetricEncDec encryption_module,
			                  SecretSharing sss_module)
	throws VanishException {
		return new ActiveDHTVanishInternalImpl(
				vanish_backend, encryption_module, sss_module);
	}
	
	@Override
	protected VanishBackendInterface instantiateBackend()
	throws VanishException {
		return new ActiveDHTVanishBackend(getParams());
	}
}
