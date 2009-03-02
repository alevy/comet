package edu.washington.cs.activedht.code;

import edu.washington.cs.activedht.code.ActiveCode;

public class TestActiveCode implements ActiveCode {
	private static final long serialVersionUID = 13579424689L;
	private int value;
	
	/** De-serialization constructor. */
	public TestActiveCode() { }
	
	public TestActiveCode(int value) { this.value = value; }
	
	@Override
	public Object onTest() { return this.value; }
	
	@Override
	public boolean equals(Object other) {
		if (other == null ||
			!other.getClass().getName().equals(this.getClass().getName())) {
			return false;
		}
		
		TestActiveCode o = (TestActiveCode)other;
		return this.value == o.value; 
	}
}
