/**
 * 
 */
package edu.washington.cs.activedht.db.dhtwrapper;

import org.gudy.azureus2.core3.util.HashWrapper;

import com.aelitis.azureus.core.dht.control.DHTControl;

import edu.washington.cs.activedht.transport.BasicDHTTransportValue;

public class PutAction implements Runnable {
	public final byte[] value;
	public final DHTControl control;
	public final HashWrapper key;
	public final UpdateNeighborsOperationAdapter operationAdapter;

	public PutAction(HashWrapper key, byte[] value, DHTControl control,
			UpdateNeighborsOperationAdapter operationAdapter) {
		this.control = control;
		this.key = key;
		this.value = value;
		this.operationAdapter = operationAdapter;
	}

	public void run() {
		control.putEncodedKey(key.getBytes(), "ActiveDHT Put",
				new BasicDHTTransportValue(System.currentTimeMillis(), value,
						"", 1, control.getTransport().getLocalContact(), false,
						0), 60000, true, operationAdapter);
	}
}