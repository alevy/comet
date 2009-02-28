package edu.washington.cs.activedht.code;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.OutputStream;

public class ClassObjectOutputStream extends ObjectOutputStream {

	public ClassObjectOutputStream()
	throws IOException, SecurityException { super(); }

	public ClassObjectOutputStream(OutputStream output)
	throws IOException, SecurityException { super(output); }
	
	@Override
	protected void annotateClass(Class cls) throws IOException {
		super.annotateClass(cls);
		byte[] class_bytes = getByteCode(cls);
		super.write(class_bytes);
	}
	
	private byte[] getByteCode(Class cls) {
		return null;
	}
	
	
}
