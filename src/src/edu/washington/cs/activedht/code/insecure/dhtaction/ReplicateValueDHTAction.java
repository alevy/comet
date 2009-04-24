package edu.washington.cs.activedht.code.insecure.dhtaction;

public class ReplicateValueDHTAction extends DHTAction {
	private static final long serialVersionUID = -7836369571370233716L;

	public ReplicateValueDHTAction() { }

	@Override
	public final boolean equals(Object o) {
		if (this == o) return true;
		if (o == null) return false;
		if (! (o instanceof ReplicateValueDHTAction)) return false;
		return true;
	}

	@Override
	public final int hashCode() { return (int)serialVersionUID; }
}
