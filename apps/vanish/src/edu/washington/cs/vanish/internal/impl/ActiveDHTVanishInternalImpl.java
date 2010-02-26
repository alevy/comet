package edu.washington.cs.vanish.internal.impl;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;

import edu.washington.cs.activedht.code.insecure.candefine.ActiveCode;
import edu.washington.cs.activedht.code.insecure.candefine.VanishActiveCode;
import edu.washington.cs.activedht.code.insecure.io.ClassObjectOutputStream;
import edu.washington.cs.vanish.internal.VanishException;
import edu.washington.cs.vanish.internal.backend.VanishBackendInterface;
import edu.washington.cs.vanish.internal.crypto.SecretSharing;
import edu.washington.cs.vanish.internal.crypto.SymmetricEncDec;
import edu.washington.cs.vanish.internal.metadata.ActiveVDOParams;
import edu.washington.cs.vanish.internal.metadata.VDOParams;

/**
 * 
 * @author roxana
 *
 */
public class ActiveDHTVanishInternalImpl
extends MetadataEncapsulatedIndirectKeyVanishImpl {
	public ActiveDHTVanishInternalImpl(VanishBackendInterface vanish_backend,
			                           SymmetricEncDec encryption_module,
			                           SecretSharing secret_sharing_module)
	throws VanishException {
		super(vanish_backend, encryption_module, secret_sharing_module);
	}
	
	@Override
	public byte[][] getShares(byte[][] locations, VDOParams params)
	throws VanishException {
		byte[][] shares = super.getShares(locations, params);
		if (shares == null) return null;
		if (params instanceof ActiveVDOParams) {
			shares = unwrapSharesFromActiveObjects(shares,
					                               (ActiveVDOParams)params);
		}
		return shares;
	}
	
	@Override
	public void pushShares(byte[][] shares, byte[][] locations,
			               VDOParams params)
	throws VanishException {
		if (params instanceof ActiveVDOParams) {
			shares = wrapSharesIntoActiveObjects(shares,
					                             (ActiveVDOParams)params);
		}
		super.pushShares(shares, locations, params);
	}
	
	// Helper functions:
	
	private byte[][] unwrapSharesFromActiveObjects(byte[][] active_values,
			@SuppressWarnings("unused") ActiveVDOParams params)
	throws ActiveDHTVanishException {
		byte[][] shares = new byte[active_values.length][];
		int i = 0;
		for (byte[] active_value: active_values) {
			VanishActiveCode active_code = unwrap(active_value);
			shares[i++] = (active_code == null
					       ? active_value : active_code.getValue());
		}
		return shares;
	}
	
	private byte[][] wrapSharesIntoActiveObjects(byte[][] shares,
             ActiveVDOParams params) {
		byte[][] active_values = shares;
		int i = 0;
		for (byte[] share: shares) active_values[i++] = wrap(share, params);
		return active_values;
	}
	
	private VanishActiveCode unwrap(byte[] active_object) {
		// TODO(roxana): NOT SECURE NOW!!!! Shouldn't use the OIS, but rather
		// the default loader of only classes that are locally defined.
		ObjectInputStream ois = null;
		try {
			ois = new ObjectInputStream(
					new ByteArrayInputStream(active_object));
			VanishActiveCode ac = (VanishActiveCode)ois.readObject();
			return ac;
		} catch (Exception e) {
		} finally {
			if (ois != null) {
				try { ois.close(); }
				catch (IOException e) { }
			}
		}
		return null;
	}
	
	private byte[] wrap(byte[] share, ActiveVDOParams params) {
		ActiveCode ac = new VanishActiveCode(share,
				params.destroyAfterFirstRead(),
				params.getTimeoutDate(),
				params.getReleaseDate());
		
		// Serialize.
		// TODO(roxana): Make this a utility function.
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
}

