package edu.washington.cs.activedht.code.insecure;

import edu.washington.cs.activedht.code.insecure.candefine.ActiveCode;
import edu.washington.cs.activedht.code.insecure.dhtaction.DHTActionList;
import edu.washington.cs.activedht.code.insecure.dhtaction.DHTActionMap;

public class ActiveObjectAdapter implements ActiveCode {
	private static final long serialVersionUID = -3212277878700655619L;
	
	private byte[] value;
	
	public ActiveObjectAdapter(byte[] value) { this.value = value; }
	
	public ActiveObjectAdapter() { }
	
    // Accessors:
	
	public byte[] getValue() { return value; }
	
	public void setValue(byte[] value) { this.value = value; }
	
	// ActiveCode interface:
	
	public void onGet(String caller_ip, DHTActionList executed_preactions,
	                  DHTActionList postactions) { }
	
	public void onValueAdded(String this_node_ip, String caller_ip,
                             DHTActionMap preactions_map,
                             DHTActionList postactions) { }

	public void onValueChanged(String caller_ip, byte[] plain_new_value,
                               DHTActionList executed_preactions,
                               DHTActionList postactions) { }

	public void onValueChanged(String caller_ip, ActiveCode new_active_value,
                               DHTActionList executed_preactions,
                               DHTActionList postactions) { }
	
	public void onDelete(String caller_ip, DHTActionList executed_preactions,
			             DHTActionList postactions) { }

	public void onTimer(DHTActionList executed_preactions,
			            DHTActionList postactions) { }

	public boolean onTest(int value) { return false; }
}
