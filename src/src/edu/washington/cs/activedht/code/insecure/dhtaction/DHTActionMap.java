package edu.washington.cs.activedht.code.insecure.dhtaction;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import edu.washington.cs.activedht.code.insecure.DHTEvent;
import edu.washington.cs.activedht.code.insecure.exceptions.InvalidActionException;
import edu.washington.cs.activedht.code.insecure.exceptions.LimitExceededException;


public final class DHTActionMap<T extends DHTAction> implements Serializable {
	private static final long serialVersionUID = -5779743462727549362L;	
	private Map<DHTEvent, DHTActionList<T>> event_to_actions_map;
	private int max_action_list_size;
		
	public DHTActionMap(int max_action_list_size) {
		this.event_to_actions_map = new HashMap<DHTEvent, DHTActionList<T>>();
		this.max_action_list_size = max_action_list_size;
	}

	// Functions used by the client:
	
	public final void addPreactionToEvent(DHTEvent event, T action)
	throws InvalidActionException, LimitExceededException {
		DHTAction.validateActionType(action);
		DHTActionList<T> existing_actions = event_to_actions_map.get(event);
		if (existing_actions == null) {
			existing_actions = new DHTActionList<T>(max_action_list_size);
			event_to_actions_map.put(event, existing_actions);
		}
		existing_actions.addAction(action);
	}
	
	public final void setEvent(DHTEvent event, DHTActionList<T> action_list) {
		this.event_to_actions_map.put(event, action_list);
	}
	
	// Function used by the DHT (and possibly the client):

	public final DHTActionList<T> getActionsForEvent(DHTEvent event) {
		return event_to_actions_map.get(event);
	}
	
	public final void resetAll() { event_to_actions_map.clear(); }
}
