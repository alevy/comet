package edu.washington.cs.activedht.db.java;

import org.gudy.azureus2.core3.util.HashWrapper;

import com.aelitis.azureus.core.dht.control.DHTControl;
import com.aelitis.azureus.core.dht.db.impl.DHTDBValueImpl;
import com.aelitis.azureus.core.dht.impl.DHTLog;
import com.aelitis.azureus.core.dht.transport.DHTTransportContact;
import com.aelitis.azureus.core.dht.transport.DHTTransportValue;

import edu.washington.cs.activedht.code.insecure.dhtaction.DHTActionMap;
import edu.washington.cs.activedht.db.ActiveDHTDBValue;
import edu.washington.cs.activedht.db.dhtwrapper.DhtWrapper;

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

	public ActiveDHTDBValue executeCallback(String string, DhtWrapper dhtWrapper) {
		// TODO Auto-generated method stub
		return null;
	}

	public DhtWrapper getDhtWrapper(DHTControl control, HashWrapper key) {
		// TODO Auto-generated method stub
		return null;
	}

	public ActiveDHTDBValue executeCallback(String string,
			DhtWrapper dhtWrapper, Object... args) {
		throw new UnsupportedOperationException();
	}

	public Object deserialize(byte[] value) {
		// TODO Auto-generated method stub
		return null;
	}

	public byte[] serialize(Object object) {
		// TODO Auto-generated method stub
		return null;
	}
}
