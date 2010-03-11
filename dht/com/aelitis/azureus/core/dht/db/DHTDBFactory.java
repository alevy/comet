/**
 * 
 */
package com.aelitis.azureus.core.dht.db;

import com.aelitis.azureus.core.dht.DHTLogger;
import com.aelitis.azureus.core.dht.DHTStorageAdapter;
import com.aelitis.azureus.core.dht.db.impl.DHTDBImpl;

import edu.washington.cs.activedht.db.ActiveDB;

/**
 * @author levya
 *
 */
public abstract class DHTDBFactory {
	

	public static final DHTDBFactory DBFactory = new DHTDBFactory() {
		public DHTDB createDB(DHTStorageAdapter adapter,
				int original_republish_interval,
				int cache_republish_interval, byte protocol_version, DHTLogger logger) {
			return new DHTDBImpl( 
			adapter,
			original_republish_interval, 
			cache_republish_interval, 
			protocol_version,
			logger );
		}
	};
	
	public static final DHTDBFactory ActiveDBFactory = new DHTDBFactory() {
		public DHTDB createDB(DHTStorageAdapter adapter,
				int original_republish_interval,
				int cache_republish_interval, byte protocol_version, DHTLogger logger) {
			ActiveDB activeDB = new ActiveDB(adapter);
			activeDB.init();
			return activeDB;
		}
	};

	public static DHTDBFactory factory = DBFactory;
	
	public abstract DHTDB createDB(DHTStorageAdapter adapter,
			int original_republish_interval,
			int cache_republish_interval, byte protocol_version, DHTLogger logger);
	
	public static DHTDB create(DHTStorageAdapter adapter,
			int original_republish_interval,
			int cache_republish_interval, byte protocol_version, DHTLogger logger) {
		return factory.createDB(adapter, original_republish_interval, cache_republish_interval, protocol_version, logger);
	}

}
