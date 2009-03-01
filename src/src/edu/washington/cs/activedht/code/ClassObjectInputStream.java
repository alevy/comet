package edu.washington.cs.activedht.code;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectStreamClass;

public class ClassObjectInputStream extends ObjectInputStream {
	InputStreamSecureClassLoader class_loader;
	
	protected ClassObjectInputStream(InputStreamSecureClassLoader loader)
	throws IOException, SecurityException {
		super();
		this.class_loader = loader;
	}
	
	@Override
	protected Class resolveClass(ObjectStreamClass desc)
	throws IOException, ClassNotFoundException {
		try { return super.resolveClass(desc); }  // TODO(roxana): What's the
		// effect of this? Security threat due to the use of a non-secure
		// loader??                                    
		catch(ClassNotFoundException e) { }
		
		// Class wasn't found locally, so try and execute it.
		
		if (! shouldDeserializeClass(desc)) throw new ClassNotFoundException();

		// Read the bytecode bytes.
		int byte_code_size = this.readInt();
		if (byte_code_size <= 0 || byte_code_size > this.available()) {
			throw new IOException("Invalid input stream");
		}
		byte[] bytecode = new byte[byte_code_size];
		if (this.read(bytecode, 0, byte_code_size) < byte_code_size) {
			throw new IOException("Invalid input stream");  // never happens?
		}
		
		// Load the class securely.
		return secureByteCodeToClass(bytecode, desc.getName());
	}
	
	private boolean shouldDeserializeClass(ObjectStreamClass desc) {
		return true;
	}

	private Class secureByteCodeToClass(byte[] bytecode, String classname)
	throws IOException, ClassNotFoundException {
		ByteArrayInputStream bytecode_is = new ByteArrayInputStream(bytecode);
		
		// Initialize the class loader to read from stream containing bytecode.
		class_loader.init(bytecode_is);
		
		// Load the class.
		Class cls = class_loader.loadClass(classname);
		
		return cls;
	}
}
