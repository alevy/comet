package edu.washington.cs.activedht.db;

import com.aelitis.azureus.core.dht.DHTConstants;
import com.aelitis.azureus.core.dht.DHTLogger;
import com.aelitis.azureus.core.dht.DHTStorageAdapter;
import com.aelitis.azureus.core.dht.control.impl.DHTControlImpl;
import com.aelitis.azureus.core.dht.db.DHTDB;
import com.aelitis.azureus.core.dht.db.DHTDBFactory;
import com.aelitis.azureus.core.dht.db.DHTDBValue;
import com.aelitis.azureus.core.dht.db.impl.DHTDBImpl;
import com.aelitis.azureus.core.dht.db.impl.DHTDBValueFactory;
import com.aelitis.azureus.core.dht.db.impl.DHTDBValueFactory.FactoryInterface;
import com.aelitis.azureus.core.dht.transport.DHTTransportContact;
import com.aelitis.azureus.core.dht.transport.DHTTransportValue;
import com.aelitis.azureus.core.dht.transport.udp.impl.DHTTransportUDPImpl;
import com.aelitis.azureus.core.dht.transport.udp.impl.DHTUDPPacketHelper;

import edu.washington.cs.activedht.db.coderunner.ActiveCodeRunner;
import edu.washington.cs.activedht.db.js.JSActiveDHTDBValue;
import edu.washington.cs.activedht.db.lua.LuaActiveDHTDBValue;
import edu.washington.cs.activedht.util.Constants;

/**
 * Must call the initialization function before
 * 
 * @author roxana
 * 
 */
public class ActiveDHTInitializer implements Constants {
	private static boolean isInitialized = false;

	public static void prepareRuntimeForActiveCode(FactoryInterface valueFactory) {
		if (isInitialized)
			return;

		// Initialize Vuze to run active code:
		initVuzeForActiveCode(valueFactory);

		// Add the rest here:

		// The following line should be the last one.
		isInitialized = true;
	}

	private static void initVuzeForActiveCode(FactoryInterface valueFactory) {
		// Initialize the DHTDB factory:
		DHTDBFactory.init(new DHTDBFactory.FactoryInterface() {
			public DHTDB create(DHTStorageAdapter adapter,
					int original_republish_interval,
					int cache_republish_interval, DHTLogger logger) {
				/*ActiveDHTDBImpl dht_db = new ActiveDHTDBImpl(
						new ActiveDHTStorageAdapter(adapter),
						original_republish_interval, cache_republish_interval,
						logger);
				dht_db.init(); // initialize the DB.
				return dht_db;*/
				return new ActiveDB(adapter, original_republish_interval, cache_republish_interval, logger);
			}
		});

		// Initialize the DHTDBValue factory:
		DHTDBValueFactory.init(valueFactory);

		configureAllConstants();

		// Add all the rest initializations for ActiveCode-capable Vuze here.

		// ...
	}

	private static void configureAllConstants() {
		DHTConstants.MAX_VALUE_SIZE = 10 * KB;
		DHTUDPPacketHelper.PACKET_MAX_BYTES = 15 * KB; // slightly larger than
														// DHT.MAX_VALUE_SIZE
		DHTTransportUDPImpl.MAX_TRANSFER_QUEUE_BYTES = 80 * MB;
		DHTDBImpl.MAX_TOTAL_SIZE = 40 * MB;
		DHTControlImpl.SHOULD_TRANSFER_VALUES_ON_JOIN = false;
	}
}
