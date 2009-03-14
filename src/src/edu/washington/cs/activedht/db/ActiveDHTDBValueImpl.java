package edu.washington.cs.activedht.db;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Arrays;

import com.aelitis.azureus.core.dht.db.impl.DHTDBValueImpl;
import com.aelitis.azureus.core.dht.transport.DHTTransportContact;
import com.aelitis.azureus.core.dht.transport.DHTTransportValue;

import edu.washington.cs.activedht.code.insecure.dhtaction.DHTActionMap;
import edu.washington.cs.activedht.code.insecure.dhtaction.DHTPreaction;
import edu.washington.cs.activedht.code.insecure.exceptions.NotAnActiveObjectException;
import edu.washington.cs.activedht.util.Constants;
import edu.washington.cs.activedht.util.CountingInputStream;

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
public class ActiveDHTDBValueImpl extends DHTDBValueImpl {
	private DHTActionMap<DHTPreaction> preactions_map;
	
	protected ActiveDHTDBValueImpl(DHTTransportContact _sender,
			DHTTransportValue _other, boolean _local) {
		super(_sender, _other, _local);
		setIsPacked();
	}
	
	protected ActiveDHTDBValueImpl(long _creation_time, byte[] _value,
			int _version,
			DHTTransportContact _originator, DHTTransportContact _sender,
			boolean _local, int _flags) {
		super(_creation_time, _value, _version, _originator, _sender,
				_local, _flags);
		setIsPacked();
	}
	
	/**
	 * Unpacks the preactions map member from the blob of bytes in the value
	 * and leaves only the ActiveCode object bytes in the value.
	 * @param  is_local_value
	 * @throws NotAnActiveObjectException
	 * @throws InvalidActiveObjectException
	 * @throws IOException
	 */
	@SuppressWarnings("unchecked")
	protected void unpack(boolean is_local_value)
	throws NotAnActiveObjectException, InvalidActiveObjectException,
 	       IOException {
		if (isUnpacked()) return;  // already unpacked.

		if (! is_local_value) {
			// Value was gotten from remotely, so its current contents
			// should only contain 
			preactions_map = new DHTActionMap<DHTPreaction>(
					Constants.MAX_NUM_DHT_ACTIONS_PER_EVENT);
			return;
		}
		
		byte[] value_bytes = super.getValue();
		if (value_bytes == null) {
			throw new NotAnActiveObjectException("Null value");
		}
		
		CountingInputStream cis = new CountingInputStream(value_bytes);
		ObjectInputStream ois = new ObjectInputStream(cis);
		
		// Initialize the preactions map, which must be saved in the value.
		try {
			preactions_map = (DHTActionMap<DHTPreaction>)ois.readObject();
		} catch (ClassNotFoundException e) {
			throw new InvalidActiveObjectException("Invalid preactions");
		}
		
		int object_bytes_offset = cis.getCount();
		
		// Initialize the object bytes from the remaining bytes.
		if (object_bytes_offset > 0) { 
			try {
				super.setValue(Arrays.copyOfRange(value_bytes,
						object_bytes_offset,
						value_bytes.length));
			} catch (IllegalArgumentException e) {
				throw new InvalidActiveObjectException("Invalid object");
			}
		}
		
		cis.close();
		ois.close();
	}
	
	/**
	 * Turns this value into a regular value by packing its preactions
	 * map into its value.
	 * 
	 * @throws IOException
	 */
	protected void pack() throws IOException {
		if (isPacked()) return;  // already packed.

		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		ObjectOutputStream oos = new ObjectOutputStream(baos);
		oos.writeObject(preactions_map);
		if (super.getValue() != null) oos.write(super.getValue());
		
		super.setValue(baos.toByteArray());  // call super, otherwise loop.
		
		baos.close();
		oos.close();
		
		setIsPacked();
	}
	
	/**
	 * Get the bytes consisting only of the ActiveCode object (if any), and
	 * not the preactions.
	 * 
	 * TODO(roxana): Maybe in the end we'll trust the Map and List objects
	 * and not need this at all.
	 *  
	 * Packaging state of this value is preserved, unless IOException
	 * during packaging/unpackaging.
	 * @return
	 */
	protected byte[] getValueForSendingItRemotely() {
		// Unpack, and then return that value. Then, re-pack, if it was packed.
		boolean was_packed_at_beginning = true;
		if (isPacked()) {
			was_packed_at_beginning = false;
			try { this.unpack(true); }
			catch (Exception e) { return super.getValue(); }
		}

		byte[] unpacked_value = super.getValue();
		
		if (was_packed_at_beginning) {  // Restore the package.
			try { this.pack(); }
			catch (IOException e) { }
		}
		
		return unpacked_value;
	}
	
	// Accessors:

	protected DHTActionMap<DHTPreaction> getPreactions()
	throws IllegalPackingStateException {
		if (isPacked()) {
			throw new IllegalPackingStateException(
					"Value is packed; expected unpacked.");
		}
		return preactions_map;
	}
	
	/**
	 * Preserves the packaging state.
	 */
	protected boolean isActiveValue(boolean is_local_value)
	throws InvalidActiveObjectException, IOException {
		// Try to unpack; if we get an exception, we know it's not active.
		if (isPacked()) {
			try { this.unpack(is_local_value); }
			catch (NotAnActiveObjectException e) { return false; }
			pack();  // package it back.
		}  // else, we know for sure that it's an active value (only those
		   // can be unpacked successfully).
		return true;
	}
	
	private boolean isUnpacked() { return preactions_map != null; }
	
	private boolean isPacked() { return ! isUnpacked(); }
	
	private void setIsPacked() { this.preactions_map = null; }
	
	@SuppressWarnings("serial")
	protected static class IllegalPackingStateException extends Exception {
		public IllegalPackingStateException(String msg) { super(msg); }
	} 
}
