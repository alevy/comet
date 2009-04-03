package edu.washington.cs.activedht.code.insecure.dhtaction;

import java.util.HashSet;
import java.util.Set;

public final class GetDHTAction extends DHTAction {
	private static final long serialVersionUID = 6320233174050803439L;

	// Inputs:
	private int num_responses_to_wait_for_subject_to_timeout;
	
	// Outputs:
	private Set<String> replicas;
	
	public GetDHTAction(int num_responses_to_wait_for_subject_to_timeout) {
		replicas = new HashSet<String>();
	}
	
	// Input accessors:
	
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
		return (o instanceof GetDHTAction);
	}

	@Override
	public int hashCode() { return (int)serialVersionUID; }
}
