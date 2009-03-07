package edu.washington.cs.activedht.db;

import com.aelitis.azureus.core.dht.db.impl.DHTDBValueImpl;
import com.aelitis.azureus.core.dht.transport.DHTTransportContact;
import com.aelitis.azureus.core.dht.transport.DHTTransportValue;

import edu.washington.cs.activedht.code.insecure.candefine.ActiveCode;

public class ActiveCodeValue extends DHTDBValueImpl {
	
	protected ActiveCodeValue(DHTTransportContact _sender,
							  DHTTransportValue _other,
							  boolean _local) {
		super(_sender, _other, _local);
	}

	protected ActiveCodeValue(long _creation_time,
							  byte[] _value,
							  int _version,
		                      DHTTransportContact _originator,
		                      DHTTransportContact _sender,
		                      boolean _local,
		                      int _flags ) {
		super(_creation_time, _value, _version, _originator, _sender,
			  _local, _flags);
	}
	
	@Override
	protected DHTDBValueImpl newDHTDBValueImpl(
			DHTTransportContact	_sender,
			DHTTransportValue	_other,
			boolean				_local ) {
		return new ActiveCodeValue( _sender, _other, _local );
	}
	
	public ActiveCode getActiveValueObject() {
		byte[] value = getValue();
		return null;
	}
}
