package edu.washington.cs.activedht.code.insecure.dhtaction;




public final class GetIPDHTPreaction extends DHTPreaction {
	private static final long serialVersionUID = -9203749180276395051L;
	
	/**
	 * Ensure that it cannot be instantiated. Only use the default PREACTION.
	 */
	private GetIPDHTPreaction() { }
	
	@Override
	public boolean equals(Object o) {
		return (this == o);
	}

	@Override
	public int hashCode() { return 0; }
	
	public static final GetIPDHTPreaction PREACTION = new GetIPDHTPreaction();
}
