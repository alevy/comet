/**
 * 
 */
package edu.washington.cs.activedht.db.dhtwrapper;

import org.gudy.azureus2.core3.util.HashWrapper;

import com.aelitis.azureus.core.dht.control.DHTControl;

public class LookupAction implements Runnable {
	public final DHTControl control;
	public final HashWrapper key;
	public final UpdateNeighborsOperationAdapter operationAdapter;

	public LookupAction(HashWrapper key, DHTControl control,
			UpdateNeighborsOperationAdapter operationAdapter) {
		this.control = control;
		this.key = key;
		this.operationAdapter = operationAdapter;
	}

	public void run() {
		control.lookupEncodedKey(key.getBytes(), 60000, operationAdapter);
	}
}