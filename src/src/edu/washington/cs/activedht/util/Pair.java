package edu.washington.cs.activedht.util;

/**
 * A C-style structure combining two values.
 * 
 * @author roxana
 *
 * @param <V1>
 * @param <V2>
 */
public class Pair<V1, V2> {
	private V1 first;
	private V2 second;
	
	public Pair() { this(null, null); }
	
	public Pair(V1 first, V2 second) { setBoth(first, second); }
	
	public void setBoth(V1 first, V2 second) {
		setFirst(first);
		setSecond(second);
	}
	
	public void setFirst(V1 first) { this.first = first; }
	
	public void setSecond(V2 second) { this.second = second; }
	
	public V1 getFirst() { return first; }
	
	public V2 getSecond() { return second; }
}
