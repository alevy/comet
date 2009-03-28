package edu.washington.cs.activedht.code.insecure.dhtaction;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;


public final class PutDHTAction extends DHTAction {
	private static final long serialVersionUID = -1324892916593067072L;
	
	// Inputs:
	private byte[] key;
	private byte[] value;
	
	// Outputs:
	private Set<String> replicas;
	
	public PutDHTAction(byte[] key, byte[] value) {
		this.key = key;
		this.value = value;
		
		replicas = new HashSet<String>();
	}
	
	// Input accessors:
	
	public byte[] getKey() { return key; }
	
	public byte[] getValue() { return value; }
	
	// Output accessors:
	
	public synchronized void addResponse(String contact_ip) {
		this.replicas.add(contact_ip);
	}
	
	public Set<String> getResponses() { return replicas; }
	
	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null) return false;
		if (! (o instanceof PutDHTAction)) return false;
		return key == null || Arrays.equals(key, ((PutDHTAction)o).key);
	}

	@Override
	public int hashCode() { 
		return key == null ? 0 : Arrays.hashCode(key);
	}
}
