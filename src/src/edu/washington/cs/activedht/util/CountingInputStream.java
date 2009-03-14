package edu.washington.cs.activedht.util;

import java.io.ByteArrayInputStream;
import java.io.IOException;

public class CountingInputStream extends ByteArrayInputStream {
	private int count;
	
	/**
	 * Constructs a new CountingInputStream.
	 * @param in InputStream to delegate to
	 */
	public CountingInputStream(byte[] bytes) { super(bytes); }

	/**
	 * Increases the count by super.read(b)'s return count
	 *
	 * @see java.io.InputStream#read(byte[])
	 */
	public int read(byte[] b) throws IOException {
		int found = super.read(b);
		this.count += found;
		return found;
	}
	
	/**
	 * Increases the count by super.read(b, off, len)'s return count
	 *
	 * @see java.io.InputStream#read(byte[], int, int)
	 */
	public int read(byte[] b, int off, int len) {
		int found = super.read(b, off, len);
		this.count += found;
		return found;
	}
	
	/**
	 * Increases the count by 1.
	 *
	 * @see java.io.InputStream#read()
	 */
	public int read() {
		this.count++;
		return super.read();
	}
	
	/**
	 * The number of bytes that have passed through this stream.
	 *
	 * @return the number of bytes accumulated
	 */
	public int getCount() { return this.count; } 
}
