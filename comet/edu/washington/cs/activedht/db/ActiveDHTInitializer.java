package edu.washington.cs.activedht.db;

import com.aelitis.azureus.core.dht.DHTLogger;
import com.aelitis.azureus.core.dht.DHTStorageAdapter;
import com.aelitis.azureus.core.dht.control.impl.DHTControlImpl;
import com.aelitis.azureus.core.dht.db.DHTDB;
import com.aelitis.azureus.core.dht.db.DHTDBFactory;
import com.aelitis.azureus.core.dht.db.impl.DHTDBImpl;
import com.aelitis.azureus.core.dht.transport.udp.impl.DHTTransportUDPImpl;
import com.aelitis.azureus.core.dht.transport.udp.impl.DHTUDPPacketHelper;

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
/*		DHTDBFactory.init(new DHTDBFactory() {
			public DHTDB create(DHTStorageAdapter adapter,
					int original_republish_interval,
					int cache_republish_interval, DHTLogger logger) {
				return new ActiveDB(adapter);
			}
		});*/

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
