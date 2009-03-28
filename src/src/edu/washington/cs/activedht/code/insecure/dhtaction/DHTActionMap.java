package edu.washington.cs.activedht.code.insecure.dhtaction;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import edu.washington.cs.activedht.code.insecure.DHTEvent;
import edu.washington.cs.activedht.code.insecure.exceptions.InvalidActionException;
import edu.washington.cs.activedht.code.insecure.exceptions.LimitExceededException;


public final class DHTActionMap implements Serializable {
	private static final long serialVersionUID = -5779743462727549362L;	
	private Map<DHTEvent, DHTActionList> event_to_actions_map;
	private int max_action_list_size;
		
	public DHTActionMap(int max_action_list_size) {
		this.event_to_actions_map = new HashMap<DHTEvent, DHTActionList>();
		this.max_action_list_size = max_action_list_size;
	}

	// Functions used by the client:
	
	public final void addPreactionToEvent(DHTEvent event, DHTAction action)
	throws InvalidActionException, LimitExceededException {
		DHTAction.validateActionType(action);
		DHTActionList existing_actions = event_to_actions_map.get(event);
		if (existing_actions == null) {
			existing_actions = new DHTActionList(max_action_list_size);
			event_to_actions_map.put(event, existing_actions);
		}
		existing_actions.addAction(action);
	}
	
	public final void setEvent(DHTEvent event, DHTActionList action_list) {
		this.event_to_actions_map.put(event, action_list);
	}
	
	// Function used by the DHT (and possibly the client):

	public final DHTActionList getActionsForEvent(DHTEvent event) {
		return event_to_actions_map.get(event);
	}
	
	public final void resetAll() { event_to_actions_map.clear(); }
	
	@SuppressWarnings("unchecked")
	@Override
	public final boolean equals(Object other) {
		if (other == this) return true;
		if (other == null) return false;
		if (! other.getClass().getName().equals(this.getClass().getName())) {
			return false;
		}
		DHTActionMap o = (DHTActionMap)other;
		if (this.event_to_actions_map == o.event_to_actions_map) return true;
		if (this.event_to_actions_map == null) return false;
		return this.event_to_actions_map.equals(o.event_to_actions_map);		
	}
	
	@Override
	public final int hashCode() {
		return ((event_to_actions_map == null)
			    ? 0 : event_to_actions_map.hashCode());
	}
	
	@SuppressWarnings("unchecked")
	public static final DHTActionMap ZERO_SIZE_ACTION_MAP
			= new DHTActionMap(0);
}
