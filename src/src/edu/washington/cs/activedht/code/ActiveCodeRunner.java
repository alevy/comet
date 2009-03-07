package edu.washington.cs.activedht.code;

import java.net.MalformedURLException;

import edu.washington.cs.activedht.code.insecure.ActiveCodeSandbox;
import edu.washington.cs.activedht.code.insecure.DHTEventHandlerCallback;
import edu.washington.cs.activedht.code.insecure.DHTEventHandlerClosure;
import edu.washington.cs.activedht.code.insecure.dhtaction.DHTAction;
import edu.washington.cs.activedht.code.insecure.dhtaction.DHTActionList;
import edu.washington.cs.activedht.code.insecure.dhtaction.DHTActionMap;
import edu.washington.cs.activedht.code.insecure.dhtaction.DHTPostaction;
import edu.washington.cs.activedht.code.insecure.dhtaction.DHTPreaction;
import edu.washington.cs.activedht.code.insecure.io.InputStreamSecureClassLoader;
import edu.washington.cs.activedht.util.Constants;

/**
 * Secure wrapper of active code. It is used to delegate events securely
 * to active code.
 *  
 * All methods in this class are executed outside the sandbox.
 * 
 * Therefore, no manipulation of ActiveCode objects (instantiation,
 * deserialization, or method calling) can be done anywhere except for the
 * executeActiveCodeSecurely(DHTEventHandlerClosure, InputStream,
 * DHTActionList<DHTPreaction>, DHTActionList<DHTPostaction>) function.
 * 
 * TODO(roxana): How to prevent sending the preactions list during replication
 * or get?
 *   
 * @author roxana
 */
public class ActiveCodeRunner implements Constants {
	/** Pointer to the secure sandbox in which to run all objects. */
	private final ActiveCodeSandbox<byte[]> active_code_sandbox;
	
	/** ClassLoader used to load the active_code class and object. */
	private final InputStreamSecureClassLoader class_loader;

	public ActiveCodeRunner(ActiveCodeSandbox<byte[]> active_code_sandbox,
			String value_owner_ip, int value_owner_port)
	throws MalformedURLException {
		class_loader = InputStreamSecureClassLoader.newInstance(value_owner_ip,
				value_owner_port);
		this.active_code_sandbox = active_code_sandbox;
	}
	
	// Event methods:
	
	public byte[] onInitialPut(DHTActionMap<DHTPreaction> all_preactions,
			byte[] value_bytes, final String caller_ip) {
		if (value_bytes == null) return null;
		all_preactions.resetAll();
		DHTEventHandlerCallback callback =
			new DHTEventHandlerCallback.InitialPutCb(class_loader, caller_ip,
					all_preactions);
		return doOnEvent(null, value_bytes, callback);
	}

	public byte[] onPut(DHTActionList<DHTPreaction> preactions,
			byte[] value_bytes, final String caller_ip,
			final byte[] new_value_bytes) {
		if (value_bytes == null) return null;
		DHTEventHandlerCallback callback =
			new DHTEventHandlerCallback.PutCb(class_loader, caller_ip,
				new_value_bytes);
		return doOnEvent(preactions, value_bytes, callback);
	}
	
	public byte[] onGet(DHTActionList<DHTPreaction> preactions,
			byte[] value_bytes, final String caller_ip) {
		if (value_bytes == null) return null;
		DHTEventHandlerCallback callback = 
			new DHTEventHandlerCallback.GetCb(class_loader, caller_ip);
		return doOnEvent(preactions, value_bytes, callback);
	}

	public byte[] onDelete(DHTActionList<DHTPreaction> preactions,
			byte[] value_bytes, final String caller_ip) {
		if (value_bytes == null) return null;
		DHTEventHandlerCallback callback =
			new DHTEventHandlerCallback.DeleteCb(class_loader, caller_ip);
		return doOnEvent(preactions, value_bytes, callback);
	}

	public byte[] onTimer(DHTActionList<DHTPreaction> preactions,
			byte[] value_bytes) {
		if (value_bytes == null) return null;
		DHTEventHandlerCallback callback =
			new DHTEventHandlerCallback.TimerCb(class_loader);
		return doOnEvent(preactions, value_bytes, callback);
	}
	
	// Helper functions:
	
	private byte[] doOnEvent(DHTActionList<DHTPreaction> event_preactions,
			byte[] value_bytes, DHTEventHandlerCallback event_callback) {
		if (value_bytes == null) return null;

		// 1. Run the preactions.
		if (event_preactions != null) executeActions(event_preactions);

		// 2. Run the object's code.
		DHTActionList<DHTPostaction> event_postactions =
			    new DHTActionList<DHTPostaction>(
			    		MAX_NUM_DHT_ACTIONS_PER_EVENT);
		byte[] current_value_bytes =
			executeActiveCodeSecurely(event_callback, value_bytes,
					event_preactions, event_postactions);

		// 3. Clear the preactions for the next execution.
		if (event_preactions != null) resetPreactions(event_preactions);
		
		// 4. Run the postactions.
		if (event_postactions != null) executeActions(event_postactions);

		return current_value_bytes;
	}
	
	/**
	 * TODO(roxana): Maybe add a time limit on action list execution.
	 * @param actions
	 */
	private void executeActions(DHTActionList actions) {
		long start = System.currentTimeMillis();
		for (int i = 0;
		     ((i < actions.size()) && (System.currentTimeMillis() - start <
		    		 MAX_TIME_RUN_DHT_ACTIONS_PER_EVENT));
		     ++i) {
			DHTAction action = actions.getAction(i);
			if (action == null) continue;
			executeAction(action);
		}
	}
	
	private void executeAction(DHTAction action) {
		if (action == null) return;
		if (action instanceof DHTPreaction) {
			executePreaction((DHTPreaction)action);
		} else {
			executePostaction((DHTPostaction)action);
		}
		action.markAsExecuted();
	}
	
	private void executePreaction(DHTPreaction action) {
		// assert(action != null);
		// TODO(roxana): Execute it based on its type.
	}
	
	private void executePostaction(DHTPostaction action) {
        // assert(action != null);
        // TODO(roxana): Execute it based on its type.
	}
	
	private void resetPreactions(DHTActionList<DHTPreaction> actions) {
		for (int i = 0; i < actions.size(); ++i) {
			DHTPreaction action = actions.getAction(i);
			if (action != null) action.resetExecution();
		}
	}
	
	private byte[] executeActiveCodeSecurely(
			DHTEventHandlerCallback event_callback, 
			byte[] value_bytes,
			DHTActionList<DHTPreaction> executed_preactions,
			DHTActionList<DHTPostaction> postactions) {
		// Wrap the callback into a closure.
		DHTEventHandlerClosure closure = new DHTEventHandlerClosure(
				event_callback, value_bytes, executed_preactions, postactions);

		// Execute the closure within the sandbox.
		byte[] active_code = null;
		try {
			active_code = active_code_sandbox.executeWithinSandbox(closure);
		} catch (Exception e) { e.printStackTrace(); }  // error, but so what..

		return active_code;
	}
}

