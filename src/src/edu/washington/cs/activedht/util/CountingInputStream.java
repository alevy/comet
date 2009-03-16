package edu.washington.cs.activedht.util;

import java.io.ByteArrayInputStream;

/**
 * @author roxana
 */
public class CountingInputStream extends ByteArrayInputStream {
	/**
	 * Constructs a new CountingInputStream.
	 * @param in InputStream to delegate to
	 */
	public CountingInputStream(byte[] bytes) { super(bytes); }
	
	/**
	 * The number of bytes that have passed through this stream.
	 *
	 * @return the number of bytes accumulated
	 */
	public int getCount() { return this.pos; } 
}
