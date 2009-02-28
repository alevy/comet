package edu.washington.cs.activedht.code;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

/**
 * TODO(roxana): Replace this with apache's Writable.
 * @author roxana
 */
public interface Writable {
	public void readFields(DataInput in) throws IOException; 
	public void write(DataOutput out) throws IOException; 
}
