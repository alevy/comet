package edu.washington.cs.activedht.db;

import com.aelitis.azureus.core.dht.db.DHTDBFactory;

import edu.washington.cs.activedht.DHTConstants;
import edu.washington.cs.activedht.util.Constants;

/**
 * Must call the initialization function before
 * 
 * @author roxana
 * 
 */
public class ActiveDHTInitializer implements Constants {
	private static boolean isInitialized = false;

	public static void prepareRuntimeForActiveCode(DHTDBValueFactory valueFactory) {
		if (isInitialized)
			return;

		// Initialize Vuze to run active code:
		initVuzeForActiveCode(valueFactory);

		// Add the rest here:

		// The following line should be the last one.
		isInitialized = true;
	}

	private static void initVuzeForActiveCode(DHTDBValueFactory valueFactory) {
		// Initialize the DHTDB factory:
		DHTDBFactory.factory = DHTDBFactory.ActiveDBFactory;

		// Initialize the DHTDBValue factory:
		DHTDBValueFactory.set(valueFactory);

		configureAllConstants();

		// Add all the rest initializations for ActiveCode-capable Vuze here.

		// ...
	}

	private static void configureAllConstants() {
		DHTConstants.MAX_VALUE_SIZE = 10 * KB;
	}
}
