package edu.washington.cs.activedht.code;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

public interface ActiveCode extends Serializable {	
	public Map<String, List<String>> getPreactions();

	public Map<String, List<String>> getPostactions();
	
	/**
	 * 
	 * @param input
	 * @return
	 */
	public boolean onGet(List<Object> inputs);
	
	/**
	 * 
	 * @param input
	 * @return
	 */
	public boolean onPut(List<Object> inputs);
	
	/**
	 * 
	 * @param input
	 * @return
	 */
	public boolean onDelete(List<Object> inputs);
	
	/**
	 * 
	 * @param input
	 * @return
	 */
	public boolean onTimer(List<Object> inputs);
	
	/**
	 * Function with testing purposes only.
	 * @param input the input
	 * @return the output
	 */
	public boolean onTest(List<Object> inputs);
}
