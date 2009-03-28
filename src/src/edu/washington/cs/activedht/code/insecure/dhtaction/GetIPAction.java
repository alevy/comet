package edu.washington.cs.activedht.code.insecure.dhtaction;

public final class GetIPAction extends DHTAction {
	private static final long serialVersionUID = -9203749180276395051L;
	
	// Inputs:
	// No inputs.
	
	// Results:
	private String ip;
	
	public GetIPAction() { }
	
	// Accessors:
	
	public void setIP(String ip) { this.ip = ip; }
	
	public String getIP() { return ip; }
	
	// Object functions:
	
	@Override
	public boolean equals(Object o) { return (this == o); }

	@Override
	public int hashCode() { return 0; }
}
