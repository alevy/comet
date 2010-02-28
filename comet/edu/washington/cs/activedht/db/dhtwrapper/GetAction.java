/**
 * 
 */
package edu.washington.cs.activedht.db.dhtwrapper;

import org.gudy.azureus2.core3.util.HashWrapper;

import com.aelitis.azureus.core.dht.control.DHTControl;

public class GetAction implements Runnable {

	public final DHTControl control;
	public final HashWrapper key;
	public final int maxValues;
	public final GetOperationAdapter getOperationAdapter;

	public GetAction(HashWrapper key, int maxValues, DHTControl control,
			GetOperationAdapter getOperationAdapter) {
		this.control = control;
		this.key = key;
		this.maxValues = maxValues;
		this.getOperationAdapter = getOperationAdapter;
	}

	public void run() {
		control.getEncodedKey(key.getBytes(), "LuaActiveDHT Get", (byte) 0,
				maxValues, 60000, true, false, getOperationAdapter);
	}
}