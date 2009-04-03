package edu.washington.cs.activedht.code.insecure.dhtaction;

public final class LocalDeleteDHTAction extends DHTAction {
	private static final long serialVersionUID = 2300651554836770846L;
	
	public LocalDeleteDHTAction() { }
		
	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null) return false;
		return (o instanceof LocalDeleteDHTAction);
	}

	@Override
	public int hashCode() { return (int)serialVersionUID; }
}
