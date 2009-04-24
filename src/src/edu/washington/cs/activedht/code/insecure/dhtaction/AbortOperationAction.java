package edu.washington.cs.activedht.code.insecure.dhtaction;

public final class AbortOperationAction extends DHTAction {
	private static final long serialVersionUID = 9209076603520432225L;
	
	@Override
	public final boolean equals(Object o) {
		if (o == null) return false;
		if (o == this) return false;
		return o instanceof AbortOperationAction;
 	}

	@Override
	public final int hashCode() { return (int)serialVersionUID; }
}
