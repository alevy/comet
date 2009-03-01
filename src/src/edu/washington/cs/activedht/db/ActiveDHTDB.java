package edu.washington.cs.activedht.db;

import java.io.ByteArrayInputStream;
import java.net.URLClassLoader;
import java.nio.ByteBuffer;

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

import edu.washington.cs.activedht.code.ActiveCode;

public class ActiveDHTDB extends DHTDBImpl {
	public ActiveDHTDB(DHTStorageAdapter _adapter,
			           int _original_republish_interval, 
			           int _cache_republish_interval,
			           DHTLogger _logger) {
		super(_adapter, _original_republish_interval,
			  _cache_republish_interval, _logger);
		
	}
	
	// Override all external requests:

	@Override
	public DHTDBLookupResult get(DHTTransportContact reader,
			HashWrapper key, int max_values, byte flags,
			boolean external_request) {
		DHTDBLookupResult result = super.get(reader, key, max_values, flags,
				external_request);
		if (result == null) return result;  // nothing to do.
		DHTDBValue[] values = result.getValues();
		if (values == null || values.length == 0) return result;
		
		int i = -1;
		for (DHTDBValue value: values) {
			++i;
			if (value == null) continue;
			boolean was_modified = doGet(value);
			if (was_modified) store(value);
		}
		return result;
	}
	
	private void store(DHTDBValue value) {
		// TODO(roxana): implement.
	}
	
	protected DHTDBValueImpl
	newDHTDBValue(long _creation_time,
			      byte[] _value,
			      int _version,
			      DHTTransportContact _originator,
			      DHTTransportContact _sender,
			      boolean _local,
			      int _flags ) {
		return new ActiveCodeValue(_creation_time, _value, _version,
				_originator, _sender, _local, _flags);
	}
	
	protected DHTDBValueImpl
	newDHTDBValue(DHTTransportContact _sender,
			  DHTTransportValue _other,
			  boolean _local) {
		return new ActiveCodeValue(_sender, _other, _local);
	}
	
	private boolean doGet(DHTDBValue value) {
		// assert(value != null);
		byte[] current_contents = value.getValue();
		if (current_contents == null) return false;
		
		byte[] new_contents = doGet(current_contents);

		boolean was_modified = false;
		if (new_contents != null) {
			value.setValue(new_contents);
			was_modified = true;
		}
		
		// Not modified.
		return was_modified;
	}
	
	private byte[] doGet(final byte[] current_contents) {
		boolean was_modified = false;
	
		ActiveCode remote_code = 
	
		if (!was_modified) return null;
		else return 
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
	
	
}
