package data;

import edu.washington.cs.activedht.code.insecure.DHTEvent;
import edu.washington.cs.activedht.code.insecure.candefine.ActiveCode;
import edu.washington.cs.activedht.code.insecure.dhtaction.DHTActionList;
import edu.washington.cs.activedht.code.insecure.dhtaction.DHTActionMap;
import edu.washington.cs.activedht.code.insecure.dhtaction.DHTPostaction;
import edu.washington.cs.activedht.code.insecure.dhtaction.DHTPreaction;

public class TestDataActiveCode implements ActiveCode {
	private static final long serialVersionUID = 13579424689L;

	private int value = -1;
	
	/** De-serialization constructor. */
	public TestDataActiveCode() { }
	
	public TestDataActiveCode(int value) { this.value = value; }
	
	public void onDelete(
			String caller_ip,
			DHTActionList<DHTPreaction> preactions,
			DHTActionList<DHTPostaction> postactions) { }

	public void onGet(
			String caller_ip,
			DHTActionList<DHTPreaction> preactions,
			DHTActionList<DHTPostaction> postactions) { }

	public void onValueAdded(
			String caller_ip,
			DHTActionMap<DHTPreaction> all_preactions,
			DHTActionList<DHTPostaction> postactions) { }

	public void onValueChanged(
			String caller_ip,
			byte[] new_value,
			DHTActionList<DHTPreaction> preactions,
			DHTActionList<DHTPostaction> postactions) { }
	
	public void onValueChanged(
			String caller_ip,
			ActiveCode new_active_value,
			DHTActionList<DHTPreaction> preactions,
			DHTActionList<DHTPostaction> postactions) { }

	public void onTimer(
			DHTActionList<DHTPreaction> preactions,
			DHTActionList<DHTPostaction> postactions) { }
	
	public boolean onTest(int input) { return this.value == input; }
}
