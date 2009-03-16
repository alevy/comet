package edu.washington.cs.activedht.code.insecure.candefine;

import edu.washington.cs.activedht.code.insecure.dhtaction.DHTPreaction;

@SuppressWarnings("serial")
public class MyBogusDHTPreaction extends DHTPreaction {
	private int x;
	
	public MyBogusDHTPreaction(int x) { this.x = x; }
	
	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null) return false;
		if (o.getClass().getName().equals(this.getClass().getName())) {
			return false;
		}
		return this.x == ((MyBogusDHTPreaction)o).x;
	}

	@Override
	public int hashCode() { return x; }
}
	
