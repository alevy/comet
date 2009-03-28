package edu.washington.cs.activedht.code.insecure.candefine;

import java.io.Serializable;

import edu.washington.cs.activedht.code.insecure.dhtaction.DHTActionList;
import edu.washington.cs.activedht.code.insecure.dhtaction.DHTActionMap;


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
			DHTActionMap preactions_map,
			DHTActionList postactions);
	
	public void onValueChanged(
			final String caller_ip, byte[] plain_new_value,
			DHTActionList executed_preactions,
			DHTActionList postactions);
	
	public void onValueChanged(
			final String caller_ip,
			ActiveCode new_active_value,
			DHTActionList executed_preactions,
			DHTActionList postactions);
	
	public void onGet(
			final String caller_ip,
			DHTActionList executed_preactions,
			DHTActionList postactions);
	
	public void onDelete(
			final String caller_ip,
			DHTActionList executed_preactions,
			DHTActionList postactions);
	
	public void onTimer(
			DHTActionList executed_preactions,
			DHTActionList postactions);
	
	/**
	 * Test function. Only used for testing.
	 * @param value
	 * @return
	 */
	public boolean onTest(int value);
}
