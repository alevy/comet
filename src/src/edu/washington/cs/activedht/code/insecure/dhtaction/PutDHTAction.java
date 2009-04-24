package edu.washington.cs.activedht.code.insecure.dhtaction;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;


public final class PutDHTAction extends DHTAction {
	private static final long serialVersionUID = -1324892916593067072L;
	
	// Inputs:
	private byte[] value;
	
	// Outputs:
	private Set<String> replicas;
	
	public PutDHTAction(byte[] value) {
		this.value = value;
		replicas = new HashSet<String>();
	}
	
	// Input accessors:
	
	public final byte[] getValue() { return value; }
	
	// Output accessors:
	
	public final synchronized void addResponse(String contact_ip) {
		this.replicas.add(contact_ip);
	}
	
	public final Set<String> getResponses() { return replicas; }
	
	@Override
	public final boolean equals(Object o) {
		if (this == o) return true;
		if (o == null) return false;
		if (! (o instanceof PutDHTAction)) return false;
		return value == null || Arrays.equals(value, ((PutDHTAction)o).value);
	}

	@Override
	public final int hashCode() {  
		return value == null ? 0 : Arrays.hashCode(value);
	}
}
