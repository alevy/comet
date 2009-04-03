package edu.washington.cs.activedht.code.insecure.candefine;

import edu.washington.cs.activedht.code.insecure.dhtaction.AbortOperationAction;
import edu.washington.cs.activedht.code.insecure.dhtaction.DHTActionList;
import edu.washington.cs.activedht.code.insecure.dhtaction.DHTActionMap;
/**
 * TODO(roxana): Can't implement right now, as I don't have side-channels
 * for the user to communicate passwds. 
 * 
 * @author roxana
 *
 */
public class SensitiveValueActiveObject implements ActiveCode {
	private static final long serialVersionUID = 1L;

	public static final int READ_LEVEL   = 0;
	public static final int WRITE_LEVEL  = 1;
	public static final int DELETE_LEVEL = 2;
	
	private byte[] actual_value;
	private String passwd;
	private int event_level_requires_passwd;
	
	public SensitiveValueActiveObject(byte[] actual_value,
			                          int event_level_requires_passwd,
			                          String passwd) {
		this.actual_value = actual_value;
		this.passwd = passwd;
		this.event_level_requires_passwd = event_level_requires_passwd;
	}

	public void onDelete(String caller_ip, DHTActionList executed_preactions,
			             DHTActionList postactions) {
		
	}

	public void onGet(String caller_ip, DHTActionList executed_preactions,
			          DHTActionList postactions) {
		
	}

	public boolean onTest(int value) { return false; }

	public void onTimer(DHTActionList executed_preactions,
			            DHTActionList postactions) { }

	public void onValueAdded(String this_node_ip, String caller_ip,
			                 DHTActionMap preactions_map,
			                 DHTActionList postactions) {
		
	}

	public void onValueChanged(String caller_ip, byte[] plain_new_value,
			                   DHTActionList executed_preactions,
			                   DHTActionList postactions) {
		addAbortPostaction(postactions);
	}

	public void onValueChanged(String caller_ip, ActiveCode new_active_value,
			                   DHTActionList executed_preactions,
			                   DHTActionList postactions) {
		if (! (new_active_value instanceof SensitiveValueActiveObject)) {
			addAbortPostaction(postactions);
		}
		if (!passwd.equals(
				((SensitiveValueActiveObject)new_active_value).passwd)) {
			return;
		}
	}
	
	private void addAbortPostaction(DHTActionList postactions) {
		try { postactions.addAction(new AbortOperationAction()); }
		catch (Exception e) { }
	}
}
