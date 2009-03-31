package edu.washington.cs.activedht.db;

import com.aelitis.azureus.core.dht.DHTLogger;
import com.aelitis.azureus.core.dht.DHTStorageAdapter;
import com.aelitis.azureus.core.dht.db.DHTDB;
import com.aelitis.azureus.core.dht.db.DHTDBFactory;
import com.aelitis.azureus.core.dht.db.impl.DHTDBValueFactory;
import com.aelitis.azureus.core.dht.db.impl.DHTDBValueImpl;
import com.aelitis.azureus.core.dht.transport.DHTTransportContact;
import com.aelitis.azureus.core.dht.transport.DHTTransportValue;
/**
 * Must call the initialization function before
 * @author roxana
 *
 */
public class ActiveDHTInitializer {
	private static boolean isInitialized = false;
	
	public static void prepareRuntimeForActiveCode() {
		if (isInitialized) return;
		
		// Initialize Vuze to run active code:
		initVuzeForActiveCode();
		
		// Add the rest here:
		
		// The following line should be the last one.
		isInitialized = true;
	}
	
	private static void initVuzeForActiveCode() {
		// Initialize the DHTDB factory:
		DHTDBFactory.init(new DHTDBFactory.FactoryInterface() {
			@Override
			public DHTDB create(DHTStorageAdapter adapter,
					            int original_republish_interval,
					            int cache_republish_interval,
					            DHTLogger logger) {
				return new ActiveDHTDB(adapter, original_republish_interval,
						               cache_republish_interval,
						               logger);
			}
		});
		
		// Initialize the DHTDBValue factory:
		DHTDBValueFactory.init(new DHTDBValueFactory.FactoryInterface() {
			public DHTDBValueImpl create(long _creation_time, byte[] _value,
					int _version, DHTTransportContact _originator,
					DHTTransportContact _sender, boolean _local, int _flags) {
				return new ActiveDHTDBValueImpl(_creation_time, _value,
						_version, _originator, _sender, _local, _flags);
			}

			public DHTDBValueImpl create(DHTTransportContact _sender,
					DHTTransportValue _other, boolean _local) {
				return new ActiveDHTDBValueImpl(_sender, _other, _local);
			}
		});
		
		// Add all the rest initializations for ActiveCode-capable Vuze here.
	}
}
