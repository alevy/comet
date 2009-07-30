package edu.washington.cs.activedht.code.insecure.dhtaction;

/**
 * Action to return a value other than the Active Object from a get.
 * 
 * @author levya
 *
 */
public class ReturnAction extends DHTAction {

	private final byte[] value;

	public ReturnAction(byte[] value) {
		this.value = value;
	}
	
	@Override
	public boolean equals(Object o) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public int hashCode() {
		// TODO Auto-generated method stub
		return 0;
	}

	public byte[] getValue() {
		return value;
	}

}
