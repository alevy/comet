package edu.washington.cs.activedht.code.insecure.dhtaction;

import java.util.ArrayList;
import java.util.List;

import edu.washington.cs.activedht.code.insecure.exceptions.InvalidActionException;
import edu.washington.cs.activedht.code.insecure.exceptions.LimitExceededException;



public final class DHTActionList<T extends DHTAction> {
	private static final long serialVersionUID = 6598853463473219627L;
	
	private int count_limit;
	private List<T> list;
	
	public DHTActionList(int count_limit) {
		list = new ArrayList<T>();
		this.count_limit = count_limit;
	}
	
	public final void addAction(T action)
	throws InvalidActionException, LimitExceededException {
		// Validate the input first.
		DHTAction.validateActionType(action);

		// Verify that the addition would not take us over the limit. 
		if (list.size() >= count_limit) {
			throw new LimitExceededException("");
		}
		
		// Everything was OK, so just add it.
		list.add(action);
	}
	
	public final T getAction(int index) { return list.get(index); }
	
	public final int getLimit() { return count_limit; }
	
	public final int size() { return list.size(); }
}
