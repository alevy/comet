package data;

import edu.washington.cs.activedht.code.ActiveCode;

public class TestDataActiveCode implements ActiveCode {
	private static final long serialVersionUID = 13579424689L;

	private int value = -1;
	
	/** De-serialization constructor. */
	public TestDataActiveCode() { }
	
	public TestDataActiveCode(int value) { this.value = value; }
	
	@Override
	public Object onTest() {
		return this.value;
	}
}
