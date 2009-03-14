package edu.washington.cs.activedht.code;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Arrays;

import com.aelitis.azureus.core.dht.db.DHTDBValue;

import edu.washington.cs.activedht.code.insecure.dhtaction.DHTActionMap;
import edu.washington.cs.activedht.code.insecure.dhtaction.DHTPreaction;
import edu.washington.cs.activedht.code.insecure.exceptions.NotAnActiveObjectException;
import edu.washington.cs.activedht.db.InvalidActiveObjectException;
import edu.washington.cs.activedht.util.Constants;
import edu.washington.cs.activedht.util.CountingInputStream;

public class ActiveCodeWrapper {
	private DHTDBValue value;

	DHTActionMap<DHTPreaction> preactions_map;
	byte[] object_bytes;
	
	/**
	 * 
	 * @param value
	 * @param is_local_value  true iff the value was gotten from the local
	 *                        node or storage. If the parameter is true,
	 *                        then the serialized object inside is expected
	 *                        to have an instance of the 
	 * @throws NotAnActiveObjectException
	 */
	@SuppressWarnings("unchecked")
	public ActiveCodeWrapper(DHTDBValue value, boolean is_local_value)
	throws NotAnActiveObjectException, InvalidActiveObjectException,
	       IOException {
		this.value = value;
		
		// Attempt to decode the preactions map and the object bytes, as well.
		unpack(is_local_value);
	}
	
	@SuppressWarnings("unchecked")
	public void unpack(boolean is_local_value) 
	throws NotAnActiveObjectException, InvalidActiveObjectException,
    	   IOException {
		byte[] value_bytes = value.getValue();
		if (value_bytes == null) {
			throw new NotAnActiveObjectException("Null value");
		}
		
		CountingInputStream cis = new CountingInputStream(value_bytes);
		ObjectInputStream ois = new ObjectInputStream(cis);
		
		// 1. Initialize the preactions map.
		if (is_local_value) {  // Preactions map must be saved in the value. 
			try {
				preactions_map = (DHTActionMap<DHTPreaction>)ois.readObject();
			} catch (ClassNotFoundException e) {
				throw new InvalidActiveObjectException("Invalid preactions");
			}
		} else {  // Initialize preactions map to a new one.
			preactions_map = new DHTActionMap<DHTPreaction>(
					Constants.MAX_NUM_DHT_ACTIONS_PER_EVENT);
		}
		
		int object_bytes_offset = cis.getCount();
		
		// 2. Initialize the object bytes.
		// assert(is_local_value && object_bytes_offset > 0);
		if (object_bytes_offset > 0) {
			try {
				this.object_bytes = Arrays.copyOfRange(value_bytes,
						object_bytes_offset,
						value_bytes.length);
			} catch (IllegalArgumentException e) {
				throw new InvalidActiveObjectException("Invalid object");
			}
		} else {
			this.object_bytes = value_bytes;
		}
		
		cis.close();
		ois.close();
	}
	
	private boolean isUnpacked() {
		return this.object_bytes != null && this.preactions_map != null;
	}
	
	public void pack() throws IOException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		ObjectOutputStream oos = new ObjectOutputStream(baos);
		if (preactions_map != null) oos.writeObject(preactions_map);
		if (object_bytes != null) oos.write(object_bytes);
		
		super.setValue(baos.toByteArray());
		
		baos.close();
		oos.close();
	}
}

