package edu.washington.cs.activedht.operations;

import com.aelitis.azureus.core.dht.control.DHTControl;

/**
 * Both input and output types *must* be in standard Java:
 * java.lang or java.util.
 * 
 * @author roxana
 *
 * @param <In>  Input type.
 * @param <Out> Output type.
 */
public abstract class DHTOperation<In, Out> implements Runnable {
	private DHTControl dht;
	private In param;
	private Out result;
	
	public DHTOperation(DHTControl dht, In param) {
		this.dht = dht;
		this.param = param;
	}
	
	public void run() {
		this.result = execute(dht, param);
	}
	
	public Out getResult() {
		return result;
	}
	
	public abstract Out execute(DHTControl dht, In param);
}
