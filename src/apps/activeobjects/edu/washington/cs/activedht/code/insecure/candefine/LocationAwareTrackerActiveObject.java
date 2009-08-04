package edu.washington.cs.activedht.code.insecure.candefine;

import edu.washington.cs.activedht.code.insecure.ActiveObjectAdapter;
import edu.washington.cs.activedht.code.insecure.dhtaction.DHTActionList;
import edu.washington.cs.activedht.code.insecure.dhtaction.ReturnAction;
import edu.washington.cs.activedht.code.insecure.exceptions.InvalidActionException;
import edu.washington.cs.activedht.code.insecure.exceptions.LimitExceededException;

public class LocationAwareTrackerActiveObject extends ActiveObjectAdapter {
	
	private final String[] ips;

	public LocationAwareTrackerActiveObject() {
		this.ips = new String[] {"192.168.1.1", "543.123.54.6", "127.0.0.1"};
	}
	
	@Override
	public void onGet(String caller_ip, DHTActionList executed_preactions,
			DHTActionList postactions) {
		try {
			String result = ips[0];
			int prev =  result.compareTo(caller_ip);
			for (String ip : ips) {
				int next = ip.compareTo(caller_ip);
				if (next < prev) {
					prev = next;
					result = ip;
				}
			}
			postactions.addAction(new ReturnAction(ips[0].getBytes()));
		} catch (InvalidActionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (LimitExceededException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
}
