package edu.washington.cs.activedht.code;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.util.HashSet;
import java.util.Set;

public class ClassObjectOutputStream extends ObjectOutputStream {
	public static final Set<Class> serialized_classes = new HashSet<Class>();
	
	static {
		serialized_classes.add(ActiveCode.class);
	}

	public ClassObjectOutputStream()
	throws IOException, SecurityException { super(); }

	public ClassObjectOutputStream(OutputStream output)
	throws IOException, SecurityException { super(output); }
	
	@Override
	protected void annotateClass(Class cls) throws IOException {
		super.annotateClass(cls);
		if (! shouldSerializeClass(cls)) return;  // do nothing else.
		
		// Get the class' bytecode.
		InputStream bytecode_is = classToByteCode(cls);
		
		// Write the bytecode and its size to the stream.
		int bytecode_size = bytecode_is.available();
		super.writeInt(bytecode_size);
		byte[] bytecode = new byte[bytecode_size];
		bytecode_is.read(bytecode);
		
		super.write(bytecode);
	}
	
	private InputStream classToByteCode(Class cls) {
		return ClassLoader.getSystemResourceAsStream(cls.getCanonicalName());
	}
	
	private boolean shouldSerializeClass(Class cls) {
		for (Class c: cls.getInterfaces()) {
			if (serialized_classes.contains(c)) return true;
		}
		if (serialized_classes.contains(cls.getSuperclass())) return true;
		return false;
	}
}
