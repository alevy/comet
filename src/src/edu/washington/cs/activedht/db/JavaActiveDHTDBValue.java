package edu.washington.cs.activedht.db;

import com.aelitis.azureus.core.dht.db.impl.DHTDBValueImpl;
import com.aelitis.azureus.core.dht.impl.DHTLog;
import com.aelitis.azureus.core.dht.transport.DHTTransportContact;
import com.aelitis.azureus.core.dht.transport.DHTTransportValue;

import edu.washington.cs.activedht.code.insecure.dhtaction.DHTActionMap;

/**
 * The implementation of a value that has the potential of being either
 * active or non-active.
 * 
 * Example USAGE:
 *   ActiveDHTDBValueImpl active_value = ...;  // normally gotten from factory
 *   active_value.unpack();
 *   
 *   DHTActionMap<DHTPreaction> preactions = active_value.getPreactionsMap();
 *   byte[] active_code_bytes = active_value.getValue();
 *   
 *   // ... Do stuff here with preactions and active_code_bytes. 
 *   
 *   active_value.pack();
 *   
 * VERY IMPORTANT: Not doing unpack() before a call to getValue() results in
 * the returned bytes containing *both* the serialization of the preactions
 * map and the bytes of the actual ActiveCode object.
 * 
 * @author roxana
 *
 */
@Deprecated
public class JavaActiveDHTDBValue extends DHTDBValueImpl implements ActiveDHTDBValue {
	private DHTActionMap preactions_map;
	
	public JavaActiveDHTDBValue(DHTTransportContact _sender,
			DHTTransportValue _other, boolean _local) {
		super(_sender, _other, _local);
	}
	
	public JavaActiveDHTDBValue(long _creation_time, byte[] _value,
			int _version,
			DHTTransportContact _originator, DHTTransportContact _sender,
			boolean _local, int _flags) {
		super(_creation_time, _value, _version, _originator, _sender,
				_local, _flags);
	}
	
	// Accessors:
	
	public synchronized DHTActionMap getPreactions(
			DHTActionMap imposed_preaction_map) {
		if (imposed_preaction_map != null) {
			this.preactions_map = imposed_preaction_map;
		}
		return preactions_map;
	}
	
	@Override
	public String getString() {
		return DHTLog.getString( getValue() );
	}
	
	public String toString() {
		return getString();
	}

	public ActiveDHTDBValue executeCallback(String string) {
		throw new RuntimeException("Unimplmente method");
	}
}
