package edu.washington.cs.activedht.db.kahlua.dhtwrapper;

import org.gudy.azureus2.core3.util.HashWrapper;

import com.aelitis.azureus.core.dht.control.DHTControl;

import edu.washington.cs.activedht.db.dhtwrapper.UpdateNeighborsOperationAdapter;

public class DeleteAction implements Runnable {

	public final HashWrapper key;
	public final DHTControl control;
	public final UpdateNeighborsOperationAdapter operationAdapter;

	public DeleteAction(HashWrapper key, DHTControl control,
			UpdateNeighborsOperationAdapter operationAdapter) {
		this.key = key;
		this.control = control;
		this.operationAdapter = operationAdapter;
	}

	public void run() {
		control.remove(key.getBytes(), "ActiveDHT Remove", operationAdapter);
	}

}
