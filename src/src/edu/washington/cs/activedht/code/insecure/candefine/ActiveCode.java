package edu.washington.cs.activedht.code.insecure.candefine;

import java.io.Serializable;

import edu.washington.cs.activedht.code.insecure.dhtaction.DHTActionList;
import edu.washington.cs.activedht.code.insecure.dhtaction.DHTActionMap;
import edu.washington.cs.activedht.code.insecure.dhtaction.DHTPostaction;
import edu.washington.cs.activedht.code.insecure.dhtaction.DHTPreaction;


/**
 * Interface implemented by all active values in an ActiveDHT.
 * 
 * All functions in this interface must be called from the confines of
 * the INSECURE sandbox.
 * 
 * @author roxana
 */
public interface ActiveCode extends Serializable {
	public void onValueAdded(
			final String caller_ip,
			DHTActionMap<DHTPreaction> preactions_map,
			DHTActionList<DHTPostaction> postactions);
	
	public void onValueChanged(
			final String caller_ip, byte[] plain_new_value,
			DHTActionList<DHTPreaction> executed_preactions,
			DHTActionList<DHTPostaction> postactions);
	
	public void onValueChanged(
			final String caller_ip,
			ActiveCode new_active_value,
			DHTActionList<DHTPreaction> executed_preactions,
			DHTActionList<DHTPostaction> postactions);
	
	public void onGet(
			final String caller_ip,
			DHTActionList<DHTPreaction> executed_preactions,
			DHTActionList<DHTPostaction> postactions);
	
	public void onDelete(
			final String caller_ip,
			DHTActionList<DHTPreaction> executed_preactions,
			DHTActionList<DHTPostaction> postactions);
	
	public void onTimer(
			DHTActionList<DHTPreaction> executed_preactions,
			DHTActionList<DHTPostaction> postactions);
	
	/**
	 * Test function. Only used for testing.
	 * @param value
	 * @return
	 */
	public boolean onTest(int value);
}
