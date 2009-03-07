package edu.washington.cs.activedht.code.insecure.io;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;

import edu.washington.cs.activedht.code.insecure.candefine.ActiveCode;
import edu.washington.cs.activedht.code.insecure.io.InputStreamSecureClassLoader;

import junit.framework.TestCase;

public class InputStreamSecureClassLoaderTest extends TestCase {
	@Override
	protected void setUp() { }
	
	@Override
	protected void tearDown() { }
	
	public void testLoadsClassFromBuffer() {
		String classname = "data.TestDataActiveCode";

		// Open the class file (should not be in the path).
		InputStream bytecode_file_is = null;
		try {
			bytecode_file_is = new FileInputStream(
				"build/testdata/data/TestDataActiveCode.class");
		} catch (IOException e) {
			e.printStackTrace();
			fail("Couldn't open data file");
		}
		
		// Create a secure class loader.
		InputStreamSecureClassLoader class_loader = null;
		try {
			class_loader = InputStreamSecureClassLoader.newInstance(
					"blah.com", 1023);
		} catch (MalformedURLException e) {
			e.printStackTrace();
			fail("Failed to instantiate class loader.");
		}
		
		// Initialize the class loader with the class file.
		try { class_loader.init(bytecode_file_is); }
		catch (IOException e1) {		
			e1.printStackTrace();
			fail("Failed to initialize class loader.");
		}
		
		// Load the class from the class file.
		Class<? extends ActiveCode> cls = null;
		try { cls = class_loader.loadClass(classname)
			.asSubclass(ActiveCode.class); }
		catch (ClassNotFoundException e) {
			e.printStackTrace();
			fail("Failed to load the class.");
		}
		
		// Should have loaded precisely the given class. 
		assertNotNull(cls);
		assertEquals(classname, cls.getName());
		
		// Instantiate an object and then do something with it.
		ActiveCode o = null;
		try { o = cls.newInstance(); }
		catch (Exception e) {
			e.printStackTrace();
			fail("Could not instantiate object.");
		}
		
		// Do something with the object.
		assertTrue(o.onTest(-1));
	}
}
