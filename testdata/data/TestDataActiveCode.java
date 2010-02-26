package data;

import edu.washington.cs.activedht.code.insecure.DHTEvent;
import edu.washington.cs.activedht.code.insecure.candefine.ActiveCode;
import edu.washington.cs.activedht.code.insecure.dhtaction.DHTActionList;
import edu.washington.cs.activedht.code.insecure.dhtaction.DHTActionMap;

public class TestDataActiveCode implements ActiveCode {
	private static final long serialVersionUID = 13579424689L;

	private int value = -1;
	
	/** De-serialization constructor. */
	public TestDataActiveCode() { }
	
	public TestDataActiveCode(int value) { this.value = value; }
	
	public void onDelete(
			String caller_ip,
			DHTActionList preactions,
			DHTActionList postactions) { }

	public void onGet(
			String caller_ip,
			DHTActionList preactions,
			DHTActionList postactions) { }

	public void onValueAdded(
			String this_node_ip,
			String caller_ip,
			DHTActionMap all_preactions,
			DHTActionList postactions) { }

	public void onValueChanged(
			String caller_ip,
			byte[] new_value,
			DHTActionList preactions,
			DHTActionList postactions) { }
	
	public void onValueChanged(
			String caller_ip,
			ActiveCode new_active_value,
			DHTActionList preactions,
			DHTActionList postactions) { }

	public void onTimer(
			DHTActionList preactions,
			DHTActionList postactions) { }
	
	public boolean onTest(int input) { return this.value == input; }
}
