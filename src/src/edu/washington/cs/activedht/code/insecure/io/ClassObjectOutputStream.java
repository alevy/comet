package edu.washington.cs.activedht.code.insecure.io;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;

import edu.washington.cs.activedht.code.insecure.candefine.ActiveCode;
import edu.washington.cs.activedht.util.Constants;
/**
 * Serializes classes alongside with the object instance.
 * NOTE: Only serializes classes for ActiveCode objects. In rest, it behaves
 * as a regular ObjectOutputStream.
 * @author roxana
 */
public class ClassObjectOutputStream extends ObjectOutputStream implements
		Constants {
	public ClassObjectOutputStream(OutputStream output) throws IOException,
			SecurityException {
		super(output);
	}

	@Override
	protected void annotateClass(Class cls) throws IOException {
		super.annotateClass(cls);
		if (!shouldSerializeClass(cls)) {
			writeInt(0);  // just declare that we have no enclosed class in.
			return; // do nothing else.
		}
		
		// Must write the class' bytecode, as well.
		
		// Get the bytecode.
		InputStream bytecode_is = classToByteCode(cls);

		// Write the bytecode and its size to the stream.
		byte[] bytecode = new byte[MAX_ADMISSIBLE_CLASS_SIZE];
		int bytecode_size = bytecode_is.read(bytecode);
		writeInt(bytecode_size);
		write(bytecode, 0, bytecode_size);
	}

	private InputStream classToByteCode(Class cls) {
		return cls.getResourceAsStream("/" + cls.getName().replace(".", "/")
				+ ".class");
	}

	@SuppressWarnings("unchecked")
	protected boolean shouldSerializeClass(Class cls) {
		try {
			cls.asSubclass(ActiveCode.class);
			return true;  // it's active code, so serialize.
		} catch (ClassCastException e) {
			return false;  // it's not active code, so don't.
		}
	}
	
	protected static boolean shouldReadClassDefinitionFromStream(
			int bytecode_size) throws IOException {
		if (bytecode_size == 0) return false;
		if (bytecode_size < 0) throw new IOException("Invalid bytecode size");
		return true;
	}
}
