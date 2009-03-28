package edu.washington.cs.activedht.code.insecure.dhtaction;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public final class GetDHTAction extends DHTAction {
	private static final long serialVersionUID = 6320233174050803439L;

	// Inputs:
	private int num_responses_to_wait_for_subject_to_timeout;
	private byte[] key;
	
	// Outputs:
	private Set<String> replicas;
	
	public GetDHTAction(byte[] key,
			               int num_responses_to_wait_for_subject_to_timeout) {
		this.key = key;
		replicas = new HashSet<String>();
	}
	
	// Input accessors:
	
	public byte[] getKey() { return key; }
	
	public int getNumResponsesToWaitForSubjectToTimeout() {
		return num_responses_to_wait_for_subject_to_timeout;
	}
	
	// Output accessors:
	
	public synchronized void addResponse(String contact_ip) {
		this.replicas.add(contact_ip);
	}
	
	public Set<String> getResponses() { return replicas; }
	
	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null) return false;
		if (! (o instanceof GetDHTAction)) return false;
		return key == null || Arrays.equals(key, ((GetDHTAction)o).key);
	}

	@Override
	public int hashCode() { 
		return key == null ? 0 : Arrays.hashCode(key);
	}
}
