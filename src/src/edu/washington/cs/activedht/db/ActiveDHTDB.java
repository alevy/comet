package edu.washington.cs.activedht.db;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.gudy.azureus2.core3.util.HashWrapper;

import com.aelitis.azureus.core.dht.DHTLogger;
import com.aelitis.azureus.core.dht.DHTStorageAdapter;
import com.aelitis.azureus.core.dht.DHTStorageBlock;
import com.aelitis.azureus.core.dht.db.DHTDBLookupResult;
import com.aelitis.azureus.core.dht.db.DHTDBValue;
import com.aelitis.azureus.core.dht.db.impl.DHTDBImpl;
import com.aelitis.azureus.core.dht.db.impl.DHTDBValueImpl;
import com.aelitis.azureus.core.dht.transport.DHTTransportContact;
import com.aelitis.azureus.core.dht.transport.DHTTransportValue;

import edu.washington.cs.activedht.code.ActiveCodeRunner;
import edu.washington.cs.activedht.code.insecure.candefine.ActiveCode;

public class ActiveDHTDB extends DHTDBImpl {
	public ActiveDHTDB(DHTStorageAdapter _adapter,
			           int _original_republish_interval, 
			           int _cache_republish_interval,
			           DHTLogger _logger) {
		super(_adapter, _original_republish_interval,
			  _cache_republish_interval, _logger);
	}
	
	// Override all external requests:
/*
	@Override
	public DHTDBLookupResult get(DHTTransportContact reader,
			HashWrapper key, int max_values, byte flags,
			boolean external_request) {
		DHTDBLookupResult result = super.get(reader, key, max_values, flags,
				external_request);
		if (result == null) return result;  // nothing to do.
		DHTDBValue[] values = result.getValues();
		if (values == null || values.length == 0) return result;
		
		for (DHTDBValue value: values) {
			if (value == null) continue;
			boolean was_modified = doGet(value);
			if (was_modified) storeValueBack(value);  // store back.
		}

		return result;
	}
*/	
	/*@Override
	protected DHTDBValueImpl newDHTDBValue(long _creation_time, byte[] _value,
			int _version,
			DHTTransportContact _originator, DHTTransportContact _sender,
			boolean _local, int _flags ) {
		return new ActiveCodeValue(_creation_time, _value, _version,
				_originator, _sender, _local, _flags);
	}
	
	@Override
	protected DHTDBValueImpl newDHTDBValue(
			DHTTransportContact _sender, DHTTransportValue _other,
			boolean _local) {
		return new ActiveCodeValue(_sender, _other, _local);
	}*/
	
	
	// DHT event handers:
/*
	private boolean doGet(DHTDBValue value, DHTOperationHandler h)
	throws IOException {
		// assert(value != null);
		ActiveCodeWrapper ac = ActiveCodeWrapperFactory.create(value);
		if (ac == null) return false;  // passive value. nothing to do.
		
		// Perform preactions.
		
		// Perform the event.
		boolean modified = ac.onGet();
		
		// Perform postactions.
		
		if (isActiveObject(o)) {
			byte[] new_contents = doGet(o);
		}

		boolean was_modified = false;
		if (new_contents != null) {
			value.setValue(new_contents);
			was_modified = true;
		}
	
		// Not modified.
		return was_modified;
	}
*/	
	/**
	 * Returns either the input byte[] or an ActiveCode subtype. 
	 * @param current_contents
	 * @return
	 */
/*	private Object deserializeValue(DHTDBValue value)
	throws IOException {
		byte[] contents = value.getValue();
		if (current_contents == null) return false;
		InputStream is = new ByteArrayInputStream(current_contents)
	}
*/	
	private byte[] doGet(final byte[] current_contents) {
		boolean was_modified = false;
	
		// ActiveCode remote_code = 
	
		if (!was_modified) return null;
		//else return
		return null;
	}

	@Override
	public DHTStorageBlock keyBlockRequest(DHTTransportContact direct_sender,
			byte[] request, byte[] signature) {
		return super.keyBlockRequest(direct_sender, request, signature);
	}

	@Override
	public DHTDBValue remove(DHTTransportContact sender, HashWrapper key) {
		return super.remove(sender, key);
	}

	@Override
	public byte store(DHTTransportContact sender, HashWrapper key,
			          DHTTransportValue[] values) {
		return super.store(sender, key, values);
	}
	
	
	private void storeValueBack(DHTDBValue value) {
		// TODO(roxana): implement.
	}
}

interface DHTOperationHandler {
	public boolean doOp(ActiveCode code);
}

class DHTGetHandler implements DHTOperationHandler {
	@Override
	public boolean doOp(ActiveCode code) {
		//return code.onGet();
		return false;
	}
}
