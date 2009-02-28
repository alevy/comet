package edu.washington.cs.activedht.code;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
/**
 * Doesn't support nulls.
 * @author roxana
 *
 * @param <T>
 */
public abstract class ClassSerializable<T extends Serializable>
implements Writable {
	T object;
	
	public ClassSerializable(T object) {
		this.object = object;
	}
	
	// Writable interface:

	public void readFields(ObjectInputStream in) throws IOException {
		
		
	}

	public void write(ObjectOutputStream out) throws IOException {
		if (object == null) {
			throw new IOException("ClassSerializable does not serialize " +
					              "nulls.");
		}
		
	}
}
