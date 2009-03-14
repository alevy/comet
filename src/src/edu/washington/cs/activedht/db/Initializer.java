package edu.washington.cs.activedht.db;

import com.aelitis.azureus.core.dht.db.impl.DHTDBValueFactory;
import com.aelitis.azureus.core.dht.db.impl.DHTDBValueImpl;
import com.aelitis.azureus.core.dht.transport.DHTTransportContact;
import com.aelitis.azureus.core.dht.transport.DHTTransportValue;
/**
 * Must call the initialization function before
 * @author roxana
 *
 */
public class Initializer {
	
	public static void prepareRuntimeForActiveCode() {
		// Initialize Vuze to run active code:
		initVuzeForActiveCode();
		
		// Initialize the manager:
		
		// TODO(roxana): add the rest here.
	}
	
	private static void initVuzeForActiveCode() {
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
