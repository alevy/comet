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
	private final HashWrapper readerId;

	public GetAction(HashWrapper key, HashWrapper readerId, int maxValues, DHTControl control,
			GetOperationAdapter getOperationAdapter) {
		this.readerId = readerId;
		this.control = control;
		this.key = key;
		this.maxValues = maxValues;
		this.getOperationAdapter = getOperationAdapter;
	}

	public void run() {
		control.getEncodedKey(key.getBytes(), readerId.getBytes(), new byte[] {}, "LuaActiveDHT Get", (byte) 0,
				maxValues, 60000, true, false, getOperationAdapter);
	}
}