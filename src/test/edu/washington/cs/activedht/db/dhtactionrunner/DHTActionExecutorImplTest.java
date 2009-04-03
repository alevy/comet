package edu.washington.cs.activedht.db.dhtactionrunner;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;

import org.gudy.azureus2.core3.util.HashWrapper;

import com.aelitis.azureus.core.dht.DHTOperationListener;
import com.aelitis.azureus.core.dht.DHTStorageBlock;
import com.aelitis.azureus.core.dht.control.DHTControl;
import com.aelitis.azureus.core.dht.control.DHTControlActivity;
import com.aelitis.azureus.core.dht.control.DHTControlListener;
import com.aelitis.azureus.core.dht.control.DHTControlStats;
import com.aelitis.azureus.core.dht.db.DHTDB;
import com.aelitis.azureus.core.dht.db.DHTDBLookupResult;
import com.aelitis.azureus.core.dht.db.DHTDBStats;
import com.aelitis.azureus.core.dht.db.DHTDBValue;
import com.aelitis.azureus.core.dht.router.DHTRouter;
import com.aelitis.azureus.core.dht.transport.DHTTransport;
import com.aelitis.azureus.core.dht.transport.DHTTransportContact;
import com.aelitis.azureus.core.dht.transport.DHTTransportValue;

import edu.washington.cs.activedht.code.insecure.dhtaction.DHTActionList;
import edu.washington.cs.activedht.code.insecure.dhtaction.GetDHTAction;
import edu.washington.cs.activedht.db.ActiveDHTDB;
import edu.washington.cs.activedht.db.dhtactionexecutor.DHTActionExecutor;
import edu.washington.cs.activedht.db.dhtactionexecutor.DHTActionExecutorImpl;
import edu.washington.cs.activedht.db.dhtactionexecutor.exedhtaction.ExecutableDHTActionFactoryImpl;
import junit.framework.TestCase;

public class DHTActionExecutorImplTest extends TestCase {
	private DHTActionExecutor executor;
	private TestDHTControl control;
	
	public DHTActionExecutorImplTest() {
		control = new TestDHTControl();
		ActiveDHTDB db = new TestActiveDHTDB();
		db.setControl(control);
		this.executor = new DHTActionExecutorImpl(db,
				new ExecutableDHTActionFactoryImpl());
	}
	
	public void testExecuteOnePreaction() {
		DHTActionList preactions =
			new DHTActionList(3);
		GetDHTAction action = new GetDHTAction(0);
		try {
			preactions.addAction(action);
		} catch (Exception e1) {
			e1.printStackTrace();
		}
		
		try { 
			executor.executeActions(preactions, 
				                    new HashWrapper("key".getBytes()),
				                    50,
				                    true);
		} catch (Exception e) {
			e.printStackTrace();
			fail("Failed to execute.");
		}
		
		assertTrue(action.actionWasExecuted());
		assertEquals(1, control.num_gets);
	}
	
	public void testExecuteTwoPreactions() {
		DHTActionList preactions =
			new DHTActionList(3);
		GetDHTAction action1 = new GetDHTAction(0);
		GetDHTAction action2 = new GetDHTAction(0);
		try {
			preactions.addAction(action1);
			preactions.addAction(action2);
		} catch (Exception e1) {
			e1.printStackTrace();
		}
		
		try { 
			executor.executeActions(preactions, 
				                    new HashWrapper("key".getBytes()),
				                    50,
				                    true);
		} catch (Exception e) {
			e.printStackTrace();
			fail("Failed to execute.");
		}
		
		assertTrue(action1.actionWasExecuted());
		assertTrue(action2.actionWasExecuted());
		assertEquals(2, control.num_gets);
	}
	
	public void testTryTwoPreactionsTimeRunsOutBeforeSecond() {
		control.setOperationDuration(100);
		
		DHTActionList preactions =
			new DHTActionList(3);
		GetDHTAction action1 = new GetDHTAction(0);
		GetDHTAction action2 = new GetDHTAction(0);
		try {
			preactions.addAction(action1);
			preactions.addAction(action2);
		} catch (Exception e1) {
			e1.printStackTrace();
		}
		
		try { 
			executor.executeActions(preactions, 
				                    new HashWrapper("key".getBytes()),
				                    50,
				                    true);
			fail("Executed both preactions w/o running out of time");
		} catch (Exception e) { }  // expected
		
		assertTrue(action1.actionWasExecuted());
		assertFalse(action2.actionWasExecuted());
		assertEquals(1, control.num_gets);
	}
}

class TestDHTControl implements DHTControl {
	protected long operation_duration = 0;
	protected boolean execution_timed_out = false;
	
	protected int num_gets = 0;
	protected int num_puts = 0;
	
	public TestDHTControl() { }
	
	protected void setOperationDuration(long operation_duration) {
		this.operation_duration = operation_duration;
	}
	
	@Override
	public void addListener(DHTControlListener l) { }

	@Override
	public int computeAndCompareDistances(byte[] n1, byte[] n2, byte[] pivot) {
		return 0;
	}

	@Override
	public void exportState(DataOutputStream os, int max) throws IOException {
	}

	@Override
	public void get(byte[] key, String description, byte flags, int max_values,
			long timeout,
			boolean exhaustive,
			boolean high_priority,
			DHTOperationListener listener) {
		++num_gets;
		exec(listener);
	}
	
	private void exec(DHTOperationListener listener) {
		try { Thread.sleep(operation_duration); }
		catch (InterruptedException e) {}
		
		listener.complete(execution_timed_out);
	}

	@Override
	public DHTControlActivity[] getActivities() { return null; }

	@SuppressWarnings("unchecked")
	@Override
	public List getClosestContactsList(byte[] id, int num_to_return,
			boolean live_only) {
		
		return null;
	}

	@SuppressWarnings("unchecked")
	@Override
	public List getClosestKContactsList(byte[] id, boolean live_only) {
		
		return null;
	}

	@SuppressWarnings("unchecked")
	@Override
	public List getContacts() {
		
		return null;
	}

	@Override
	public DHTDB getDataBase() {
		
		return null;
	}

	@Override
	public DHTTransportValue getLocalValue(byte[] key) {
		
		return null;
	}

	@Override
	public DHTRouter getRouter() {
		
		return null;
	}

	@Override
	public DHTControlStats getStats() {
		
		return null;
	}

	@Override
	public DHTTransport getTransport() {
		
		return null;
	}

	@Override
	public void importState(DataInputStream is) throws IOException {
		
		
	}

	@Override
	public boolean isDiversified(byte[] key) {
		
		return false;
	}

	@Override
	public boolean lookup(byte[] id, long timeout,
			              DHTOperationListener listener) {
		
		return false;
	}

	@Override
	public void pingAll() {
		
		
	}

	@Override
	public void print(boolean full) {
		
		
	}

	@Override
	public void put(byte[] key, String description, byte[] value, byte flags,
			boolean high_priority, DHTOperationListener listener) {
		++num_puts;
		exec(listener);
	}

	@SuppressWarnings("unchecked")
	@Override
	public void putDirectEncodedKeys(byte[][] keys, String description,
			DHTTransportValue[][] value_sets, List contacts) {
	}

	@Override
	public void putEncodedKey(byte[] key, String description,
			DHTTransportValue value, long timeout, boolean original_mappings) {
		
		
	}

	@Override
	public byte[] remove(byte[] key, String description,
			DHTOperationListener listener) {
		
		return null;
	}

	@Override
	public byte[] remove(DHTTransportContact[] contacts, byte[] key,
			String description, DHTOperationListener listener) {
		
		return null;
	}

	@Override
	public void removeListener(DHTControlListener l) {
		
		
	}

	@Override
	public void seed(boolean full_wait) { }

	@SuppressWarnings("unchecked")
	@Override
	public List sortContactsByDistance(List contacts) { return null; }

	@Override
	public boolean verifyContact(DHTTransportContact c, boolean direct) {
		
		return false;
	}
}

class TestDB implements DHTDB {
	private DHTControl control;
	
	public DHTDBValue get(HashWrapper key) { return null; }

	public DHTDBLookupResult get(DHTTransportContact reader,
			                     HashWrapper key,
			                     int max_values,
			                     byte flags,
			                     boolean external_request) {
		return null;
	}

	public DHTControl getControl() { return control; }

	public DHTStorageBlock[] getDirectKeyBlocks() { return null; }

	public DHTStorageBlock getKeyBlockDetails(byte[] key) { return null; }

	public Iterator getKeys() { return null; }

	public DHTDBStats getStats() { return null; }

	public boolean isEmpty() { return false; }

	public boolean isKeyBlocked(byte[] key) { return false; }

	public DHTStorageBlock keyBlockRequest(
			DHTTransportContact direct_sender,
			byte[] request,
			byte[] signature) {
		return null;
	}

	public void print(boolean full) { }

	public DHTDBValue remove(DHTTransportContact sender, HashWrapper key) {
		return null;
	}

	public void setControl(DHTControl control) { this.control = control; }

	public DHTDBValue store(HashWrapper key, byte[] value, byte flags) {
		return null;
	}

	public byte store(DHTTransportContact sender, HashWrapper key,
			          DHTTransportValue[] values) {
		return 0;
	}
	
}

class TestActiveDHTDB extends TestDB implements ActiveDHTDB {

	public DHTDBValue superGet(HashWrapper key) { return null; }

	public DHTDBLookupResult superGet(DHTTransportContact reader,
			                          HashWrapper key,
			                          int max_values,
			                          byte flags,
			                          boolean external_request) {
		return null;
	}

	public DHTDBValue superRemove(DHTTransportContact sender,
			                      HashWrapper key) {
		return null;
	}

	public DHTDBValue superStore(HashWrapper key, byte[] value, byte flags) {
		return null;
	}

	public byte superStore(DHTTransportContact sender, HashWrapper key,
			               DHTTransportValue[] values) {
		return 0;
	}
}