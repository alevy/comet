package edu.washington.cs.activedht.util;

import java.io.IOException;

import junit.framework.TestCase;

public class CountingInputStreamTest extends TestCase {
	public void testC() {
		byte[] bytes = "123456789".getBytes();
		CountingInputStream stream = new CountingInputStream(bytes);
		
		byte[] buff = new byte[2];
		
		// Read 2.
		int read = 0;
		try { read = stream.read(buff); }
		catch (IOException e) {
			e.printStackTrace();
			fail("Failed to read.");
		}
		assertEquals(2, read);
		assertEquals(2, stream.getCount());
		
		// Read another 2.
		try { read = stream.read(buff); }
		catch (IOException e) {
			e.printStackTrace();
			fail("Failed to read.");
		}
		assertEquals(2, read);
		assertEquals(4, stream.getCount());
		
		// Read the rest.
		buff = new byte[6];
		try { read = stream.read(buff); }
		catch (IOException e) {
			e.printStackTrace();
			fail("Failed to read.");
		}
		assertEquals(5, read);
		assertEquals(9, stream.getCount());
	}
}
