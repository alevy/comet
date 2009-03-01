package edu.washington.cs.activedht.code;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;

import junit.framework.TestCase;

public class InputStreamSecureClassLoaderTest extends TestCase {
	@Override
	protected void setUp() { }
	
	@Override
	protected void tearDown() { }
	
	public void testLoadsClassFromBuffer() {
		String classname = "myclassloader.InputStreamSecureClassLoader";
		// Get the bytes of a class.
		InputStream is = ClassLoader.getSystemResourceAsStream(
			classname);
		
		InputStreamSecureClassLoader class_loader = null;
		try {
			class_loader = InputStreamSecureClassLoader.newInstance(
					"blah.com", 1023);
		} catch (MalformedURLException e) {
			e.printStackTrace();
			fail("Failed to instantiate class loader.");
		}
		
		// Initialize the class loader.
		try { class_loader.init(is); }
		catch (IOException e1) {		
			e1.printStackTrace();
			fail("Failed to initialize class loader.");
		}
		
		Class cls = null;
		try { cls = class_loader.loadClass(classname); }
		catch (ClassNotFoundException e) {
			e.printStackTrace();
			fail("Failed to load the class.");
		}
		
		assertEquals(InputStreamSecureClassLoader.class, cls);
	}
}
