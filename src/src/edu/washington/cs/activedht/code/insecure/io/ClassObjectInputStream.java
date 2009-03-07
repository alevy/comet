package edu.washington.cs.activedht.code.insecure.io;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectStreamClass;


public class ClassObjectInputStream extends ObjectInputStream {
	InputStreamSecureClassLoader class_loader;
	
	public ClassObjectInputStream(InputStream is,
			                      InputStreamSecureClassLoader loader)
	throws IOException, SecurityException {
		super(is);
		this.class_loader = loader;
	}
	
	@SuppressWarnings("unchecked")
	@Override
	protected Class resolveClass(ObjectStreamClass desc)
	throws IOException, ClassNotFoundException {
		Class cls = null;
		try { cls = super.resolveClass(desc); }  
		catch(ClassNotFoundException e) { }
		
		// Read the class size.
		int byte_code_size = this.readInt();
		boolean should_read_class_from_stream =
			ClassObjectOutputStream.shouldReadClassDefinitionFromStream(
				byte_code_size);
		if (! should_read_class_from_stream) {
			if (cls == null) throw new ClassNotFoundException();
			return cls;
		}
		
		// Class definition is specified in the stream.

		// Read the bytecodes.
		byte[] bytecode = new byte[byte_code_size];
		readFully(bytecode, 0, byte_code_size);
		
		// Load the class securely.
		return secureByteCodeToClass(bytecode, desc.getName());
	}

	private Class secureByteCodeToClass(byte[] bytecode, String classname)
	throws IOException, ClassNotFoundException {
		ByteArrayInputStream bytecode_is = new ByteArrayInputStream(bytecode);
		
		// Initialize the class loader to read from stream containing bytecode.
		class_loader.init(bytecode_is);
		
		// Load the class.
		return class_loader.loadClass(classname);
	}
}
