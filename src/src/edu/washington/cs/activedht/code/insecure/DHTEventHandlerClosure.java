package edu.washington.cs.activedht.code.insecure;

import java.util.concurrent.Callable;

import edu.washington.cs.activedht.code.insecure.dhtaction.DHTActionList;


/**
 * Wrapper around the DHTEventHandlerCallback. It transforms a
 * DHTEventHandlerCallback into a Callable closure, which gets passed to the
 * ActiveCodeSandbox for running.
 * 
 * @author roxana
 */
public class DHTEventHandlerClosure implements Callable<byte[]> {
	DHTEventHandlerCallback callback;
	byte[] value_bytes;
	DHTActionList executed_preactions;
	DHTActionList postactions;
	
	public DHTEventHandlerClosure(DHTEventHandlerCallback callback,
			byte[] value_bytes,
			DHTActionList executed_preactions,
			DHTActionList postactions) {
		this.callback = callback;
		this.value_bytes = value_bytes;
		this.executed_preactions = executed_preactions;
		this.postactions = postactions;
	}

	public final byte[] call() throws Exception {
		return callback.executeEventOnActiveObject(value_bytes,
				executed_preactions, postactions);
	}
}
