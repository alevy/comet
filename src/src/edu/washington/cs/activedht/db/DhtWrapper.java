package edu.washington.cs.activedht.db;

import java.util.Set;

public interface DhtWrapper {

	public abstract Set<String> getNeighbors();

	public abstract String getIP();

	public abstract void get(final int maxValues);

	public abstract void put(int maxValues);

	public abstract void put(final int maxValues, final Object putValue);

}