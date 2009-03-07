package data;

import edu.washington.cs.activedht.code.ActiveCode;
import edu.washington.cs.activedht.code.insecure.nodefine.DHTActionList;
import edu.washington.cs.activedht.code.insecure.nodefine.DHTActionMap;
import edu.washington.cs.activedht.code.insecure.nodefine.DHTEvent;
import edu.washington.cs.activedht.code.insecure.nodefine.DHTPostaction;
import edu.washington.cs.activedht.code.insecure.nodefine.DHTPreaction;
import edu.washington.cs.activedht.code.insecure.nodefine.GetDHTPreaction;
import edu.washington.cs.activedht.code.insecure.nodefine.InvalidActionException;
import edu.washington.cs.activedht.code.insecure.nodefine.LimitExceededException;
import edu.washington.cs.activedht.code.insecure.nodefine.PutDHTPostaction;

public class TestDataActiveCode implements ActiveCode {
	private static final long serialVersionUID = 13579424689L;

	private int value = -1;
	
	/** De-serialization constructor. */
	public TestDataActiveCode() { }
	
	public TestDataActiveCode(int value) { this.value = value; }
	
	public void onDelete(String caller_ip,
			DHTActionList<DHTPreaction> preactions,
			DHTActionList<DHTPostaction> postactions) { }

	public void onGet(String caller_ip,
			DHTActionList<DHTPreaction> preactions,
			DHTActionList<DHTPostaction> postactions) { }

	public void onInitialPut(String caller_ip,
			DHTActionMap<DHTPreaction> all_preactions) { }

	public void onPut(String caller_ip, byte[] new_value,
			DHTActionList<DHTPreaction> preactions,
			DHTActionList<DHTPostaction> postactions) { }
	
	public void onPut(String caller_ip, ActiveCode new_active_value,
			DHTActionList<DHTPreaction> preactions,
			DHTActionList<DHTPostaction> postactions) { }

	public void onTimer(DHTActionList<DHTPreaction> preactions,
			DHTActionList<DHTPostaction> postactions) { }
	
	public boolean onTest(int input) { return this.value == input; }
}
