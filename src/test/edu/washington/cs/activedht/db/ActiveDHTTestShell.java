package edu.washington.cs.activedht.db;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;

import com.aelitis.azureus.core.dht.DHTLogger;
import com.aelitis.azureus.core.dht.DHTStorageAdapter;
import com.aelitis.azureus.core.dht.impl.Test;
import com.aelitis.azureus.core.dht.transport.DHTTransportValue;

import edu.washington.cs.activedht.code.insecure.candefine.AccessCountingActiveCode;
import edu.washington.cs.activedht.code.insecure.candefine.ActiveCode;
import edu.washington.cs.activedht.code.insecure.io.ClassObjectOutputStream;

public class ActiveDHTTestShell extends Test {
	@Override
	protected DHTStorageAdapter createStorageAdapter(int network,
			                                         DHTLogger logger,
		                                             File file) {
		return new ActiveDHTStorageAdapter(super.createStorageAdapter(network,
				                                                      logger,
				                                                      file));
	}
	
	@Override
	protected String getTmpDirectory() { return "/tmp/dht/"; }
	
	@Override
	protected byte[] getBytes(String value) {
		if (! value.startsWith("active")) {
			return super.getBytes(value);
		}
			
		// Value should be active.
		
		value = value.substring("active".length());

		// Create a test active value.
		ActiveCode ac = new AccessCountingActiveCode(value);
		
		// Serialize it.
		byte[] value_bytes = null;
		try {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			ClassObjectOutputStream oos = new ClassObjectOutputStream(baos);
			oos.writeObject(ac);
			value_bytes = baos.toByteArray();
			oos.close();
		} catch (Exception e) {
			e.printStackTrace();
		}

		return value_bytes;
	}
	
	@Override
	protected String getString(DHTTransportValue value) {		
		ActiveDHTDBValueImpl active_val =
			new ActiveDHTDBValueImpl(null, value, false);
		try { active_val.unpack(null); }
		catch (Exception e) { return super.getString(value); }  // regular val.
		
		byte[] active_object = active_val.getValue();
		ObjectInputStream ois = null;
		try {
			ois = new ObjectInputStream(
					new ByteArrayInputStream(active_object));
			ActiveCode ac = (ActiveCode)ois.readObject();
			if (ac != null) return ac.toString();
		} catch (Exception e) {
		} finally {
			if (ois != null) {
				try { ois.close(); }
				catch (IOException e) { }
			}
		}
		
		return new String(active_object);  // regular value.
	}
	
	public static void main(String args[]) {
		// First, initialize the active engine of the DHT.
		ActiveDHTInitializer.prepareRuntimeForActiveCode();
		
		// Create a test shell using Azureus' one.
		new ActiveDHTTestShell();
	}
}

