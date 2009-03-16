package edu.washington.cs.activedht.code.insecure.io;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import edu.washington.cs.activedht.code.insecure.DHTEvent;
import edu.washington.cs.activedht.code.insecure.candefine.ActiveCode;
import edu.washington.cs.activedht.code.insecure.candefine.TestActiveCode;
import edu.washington.cs.activedht.code.insecure.io.ClassObjectInputStream;
import edu.washington.cs.activedht.code.insecure.io.ClassObjectOutputStream;
import edu.washington.cs.activedht.code.insecure.io.InputStreamSecureClassLoader;

import junit.framework.TestCase;

public class ClassObjectInputOutputStreamTest extends TestCase {
	@Override
	protected void setUp() { }
	
	@Override
	protected void tearDown() { }
	
	public void testCorrectInOutMustSerializeClass() {
		ActiveCode my_object = new TestActiveCode(DHTEvent.GET, 0);

		// Serialize the object.
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		ClassObjectOutputStream coos = null;
		try { coos = new ClassObjectOutputStream(baos); }
		catch (Exception e) {
			e.printStackTrace();
			fail("Could not create ClassObjectOutputStream.");
		}
		try { coos.writeObject(my_object); }
		catch (IOException e) {
			e.printStackTrace();
			fail("Could not write active code object.");
		}
		
		byte[] serialized_object_bytes = baos.toByteArray();
		
		// De-serialize the object using the class loader.
		Object deserialized_object = null;
		ClassObjectInputStream cois = null;
		try {
			cois = new ClassObjectInputStream(
				new ByteArrayInputStream(serialized_object_bytes),
				InputStreamSecureClassLoader.newInstance("host.com", 1024));
		} catch (Exception e) {
			e.printStackTrace();
			fail("Could not create ClassObjectInputStream.");
		}
		
		try { deserialized_object = cois.readObject(); }
		catch (Exception e) {
			e.printStackTrace();
			fail("Could not read object.");
		}

		try {
			baos.close();
			cois.close();
		} catch (IOException e) {
			e.printStackTrace();
			fail("Couldn't close streams.");
		}
		
		// Test that they are equal.
		assertEquals(my_object, deserialized_object);
	}
}
