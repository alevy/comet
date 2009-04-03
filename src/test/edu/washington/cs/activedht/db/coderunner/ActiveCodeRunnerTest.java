package edu.washington.cs.activedht.db.coderunner;

import java.util.List;

import org.gudy.azureus2.core3.util.HashWrapper;

import com.aelitis.azureus.core.dht.DHTOperationListener;
import com.aelitis.azureus.core.dht.db.DHTDBValue;
import com.aelitis.azureus.core.dht.transport.DHTTransportContact;
import com.aelitis.azureus.core.dht.transport.DHTTransportValue;

import edu.washington.cs.activedht.code.insecure.DHTEvent;
import edu.washington.cs.activedht.code.insecure.DHTEventHandlerCallbackTest;
import edu.washington.cs.activedht.code.insecure.candefine.ActiveCode;
import edu.washington.cs.activedht.code.insecure.candefine.TestActiveCode;
import edu.washington.cs.activedht.code.insecure.dhtaction.DHTAction;
import edu.washington.cs.activedht.code.insecure.dhtaction.DHTActionMap;
import edu.washington.cs.activedht.code.insecure.dhtaction.TestPostaction;
import edu.washington.cs.activedht.code.insecure.dhtaction.TestPreaction;
import edu.washington.cs.activedht.code.insecure.exceptions.InitializationException;
import edu.washington.cs.activedht.code.insecure.sandbox.ActiveCodeSandbox;
import edu.washington.cs.activedht.code.insecure.sandbox.ActiveCodeSandboxImpl;
import edu.washington.cs.activedht.db.ActiveDHTDB;
import edu.washington.cs.activedht.db.ActiveDHTDBValueImpl;
import edu.washington.cs.activedht.db.ActiveDHTInitializer;
import edu.washington.cs.activedht.db.StoreListener;
import edu.washington.cs.activedht.db.TestDHTClasses;
import edu.washington.cs.activedht.db.coderunner.ActiveCodeRunner;
import edu.washington.cs.activedht.db.coderunner.IllegalPackingStateException;
import edu.washington.cs.activedht.db.dhtactionexecutor.DHTActionExecutorImpl;
import edu.washington.cs.activedht.db.dhtactionexecutor.exedhtaction.ActiveDHTOperationListener;
import edu.washington.cs.activedht.db.dhtactionexecutor.exedhtaction.ExecutableDHTAction;
import edu.washington.cs.activedht.db.dhtactionexecutor.exedhtaction.ExecutableDHTActionFactory;
import edu.washington.cs.activedht.util.Pair;
import junit.framework.TestCase;

public class ActiveCodeRunnerTest extends TestCase implements TestDHTClasses {
	private ActiveCodeRunner runner;
	
	private ActiveCode active_object;
	private byte[] active_object_bytes;
	private DHTTransportContact sender;
	
	// TestCase functions:
	
	@Override
	protected void setUp() {
		ActiveDHTInitializer.prepareRuntimeForActiveCode();
		
		ActiveCodeRunner.ActiveCodeRunnerParam params =
			new ActiveCodeRunner.ActiveCodeRunnerParam();
		ActiveCodeSandbox<byte[]> sandbox =
			new ActiveCodeSandboxImpl<byte[]>(
					params.active_code_execution_timeout);
			
		try { sandbox.init(); }
		catch (InitializationException e) {
			e.printStackTrace();
			fail("Failed to initialize sandbox.");
		}
		
		runner = new ActiveCodeRunner(sandbox, new TestDHTActionExecutorImpl(),
				                      params);
		
		active_object = new TestActiveCode(DHTEvent.GET,
				                           new TestPreaction(1),
				                           new TestPostaction(1));
		active_object_bytes = 
			DHTEventHandlerCallbackTest.serializeActiveObject(active_object);
		
		sender = new TestDHTTransportContact(1);
	}
	
	@Override
	protected void tearDown() { clearCounters(); }
	
	private void clearCounters() {
		TestPreaction.resetAllExecutions();
		TestPostaction.resetAllExecutions();
	}
	
	/**
	 * Assumes that value has already been unpacked.
	 * @param value
	 */
	private void checkPreactionsForUnpackedValue(ActiveDHTDBValueImpl value) {
		DHTActionMap all_preactions = null;
		try { all_preactions = value.getPreactions(); }
		catch (IllegalPackingStateException e1) {  // should never happen.
			e1.printStackTrace();
			fail("Couldn't get preactions");
		}
		assertNotNull(all_preactions);
		assertNotNull(all_preactions.getActionsForEvent(DHTEvent.GET));
		assertEquals(1,
				     all_preactions.getActionsForEvent(DHTEvent.GET).size());
		assertFalse(all_preactions.getActionsForEvent(DHTEvent.GET)
				.getAction(0).actionWasExecuted());
	}
	
	/**
	 * Assumes the value has been previously unpacked.
	 * @param value
	 * @param num_execution_times
	 */
	private void checkActiveObjectStateForUnpackedValue(
			ActiveDHTDBValueImpl value,
			int num_execution_times) {
		TestActiveCode current_object =
			(TestActiveCode)DHTEventHandlerCallbackTest
			.instantiateActiveObject(value.getValue(),
					sender.getAddress().getHostName(),
					sender.getAddress().getPort());
		assertTrue(current_object.onTest(num_execution_times));
	}
	
	// Test cases:

	public void testOnInitialStoreNoOverwriteNoCancellation() {		
		ActiveDHTDBValueImpl added_value = new ActiveDHTDBValueImpl(
				sender,
				new TestDHTTransportValue(sender, active_object_bytes, false),
				false);
		
		StoreListener store_listener = new StoreListener();
		
		store_listener.addOutcome(null, added_value);  // no overwriting
		
		Pair<List<DHTTransportValue>, List<DHTTransportValue>> result =
			runner.onStore(sender, new HashWrapper("k1".getBytes()),
					       store_listener);
		
		// Check the result; no requests should exist.
		assertNotNull(result);
		assertNotNull(result.getFirst());
		assertNotNull(result.getSecond());
		assertEquals(0, result.getFirst().size());
		assertEquals(0, result.getSecond().size());
		
		try { added_value.unpack(null); }
		catch (Exception e) {
			e.printStackTrace();
			fail("Couldn't unpack the value");
		}
		
		// Check preactions.
		checkPreactionsForUnpackedValue(added_value);
		assertEquals(0, TestPreaction.getAllExecutions().size());
		
		// Check postactions.
		assertEquals(1, TestPostaction.getAllExecutions().size());
		assertEquals(1,
				     TestPostaction.getAllExecutions().get(0).intValue());

		// Check the active value state.
		checkActiveObjectStateForUnpackedValue(added_value, 1);
		
		try {
			added_value.pack();
		} catch (Exception e) {
			e.printStackTrace();
			fail("Couldn't pack the value");
		}
	}
	
	public void testOnOverwritingStoreNoCancellation() {
		ActiveDHTDBValueImpl old_value = new ActiveDHTDBValueImpl(
				sender,
				new TestDHTTransportValue(sender, active_object_bytes, false),
				false);		
		// Store the overwritten value.
		StoreListener store_listener = new StoreListener();
		store_listener.addOutcome(null, old_value);  // no overwriting.
		Pair<List<DHTTransportValue>, List<DHTTransportValue>> result =
			runner.onStore(sender, new HashWrapper("k1".getBytes()),
					       store_listener);
		
		clearCounters();
		
		// Create another value.
		ActiveCode new_object = new TestActiveCode(DHTEvent.GET,
				new TestPreaction(2), new TestPostaction(2));
		byte[] new_object_bytes = DHTEventHandlerCallbackTest
				.serializeActiveObject(new_object);
		ActiveDHTDBValueImpl new_value = new ActiveDHTDBValueImpl(
				sender,
				new TestDHTTransportValue(sender, new_object_bytes, false),
				false);
		
		// Add the new value, overwriting the old one.
		store_listener = new StoreListener();
		store_listener.addOutcome(old_value, new_value);  // overwriting.
		
		result = runner.onStore(sender, new HashWrapper("k1".getBytes()),
					            store_listener);
		
		// Check the result; no requests should exist.
		assertNotNull(result);
		assertNotNull(result.getFirst());
		assertNotNull(result.getSecond());
		assertEquals(0, result.getFirst().size());
		assertEquals(0, result.getSecond().size());
		
		try { old_value.unpack(null); }
		catch (Exception e) {
			e.printStackTrace();
			fail("Couldn't unpack old value.");
		}
		try { new_value.unpack(null); }
		catch (Exception e) {
			e.printStackTrace();
			fail("Couldn't unpack new value.");
		}
		
		// Check preactions for the added and overwritten values.
		checkPreactionsForUnpackedValue(old_value);
		checkPreactionsForUnpackedValue(new_value);		
		
		// Check preactions execution: no preactions for put.
		assertEquals(0, TestPreaction.getAllExecutions().size());
		
		// Check postactions execution: postactions for the overwritten value
		// were executed first, and then postactions for the added value.
		assertEquals(2, TestPostaction.getAllExecutions().size());
		assertEquals(1,
				     TestPostaction.getAllExecutions().get(0).intValue());
		assertEquals(2,
			         TestPostaction.getAllExecutions().get(1).intValue());
		
		// Check the states of the active objects.
		checkActiveObjectStateForUnpackedValue(new_value, 1);
		checkActiveObjectStateForUnpackedValue(old_value, 2);  // once from
				// the initial store.
		
		try { new_value.pack(); }
		catch (Exception e) {
			e.printStackTrace();
			fail("Couldn't pack the new value");
		}

		try { old_value.pack(); }
		catch (Exception e) {
			e.printStackTrace();
			fail("Couldn't pack the old value");
		}
	}
	
	public void testOnGetNoCancellation() {
		ActiveDHTDBValueImpl value = new ActiveDHTDBValueImpl(
				sender,
				new TestDHTTransportValue(sender, active_object_bytes, false),
				false);		
		// Store the overwritten value.
		StoreListener store_listener = new StoreListener();
		store_listener.addOutcome(null, value);  // no overwritten value.
		runner.onStore(sender, new HashWrapper("k1".getBytes()),
				       store_listener);
		
		clearCounters();
		
		// Do a Get:
		DHTDBValue[] values = new ActiveDHTDBValueImpl[] { value };
		
		DHTDBValue[] values_allowing_access = runner.onGet(
				sender,
				new HashWrapper("k1".getBytes()),
				values);
		
		// Check the result; no requests should exist.
		assertNull(values_allowing_access);
		
		try { value.unpack(null); }
		catch (Exception e) {
			e.printStackTrace();
			fail("Couldn't unpack old value.");
		}
		
		// Check preactions.
		checkPreactionsForUnpackedValue(value);
		
		// Check preactions execution: no preactions for put, but 1 for get.
		assertEquals(1, TestPreaction.getAllExecutions().size());
		assertEquals(1, TestPreaction.getAllExecutions().get(0).intValue());
		
		// Check postactions execution: no postactions for put, but 1 for get.
		assertEquals(1, TestPostaction.getAllExecutions().size());
		assertEquals(1,
				     TestPostaction.getAllExecutions().get(0).intValue());
		
		// Check the states of the active objects.
		checkActiveObjectStateForUnpackedValue(value, 2);
		
		try { value.pack(); }
		catch (Exception e) {
			e.printStackTrace();
			fail("Couldn't pack the new value");
		}
	}
	
	public void testOnRemoveNoCancellation() {
		ActiveDHTDBValueImpl value = new ActiveDHTDBValueImpl(
				sender,
				new TestDHTTransportValue(sender, active_object_bytes, false),
				false);		
		// Store the overwritten value.
		StoreListener store_listener = new StoreListener();
		store_listener.addOutcome(null, value);  // no overwritten value
		runner.onStore(sender, new HashWrapper("k1".getBytes()),
				       store_listener);
		
		clearCounters();
		
		// Do a Delete:		
		DHTDBValue value_to_be_placed_back = runner.onRemove(
				sender,
				new HashWrapper("k1".getBytes()),
				value);
		
		// Check the result; no requests should exist.
		assertNull(value_to_be_placed_back);
		
		try { value.unpack(null); }
		catch (Exception e) {
			e.printStackTrace();
			fail("Couldn't unpack old value.");
		}
		
		// Check preactions.
		checkPreactionsForUnpackedValue(value);
		
		// Check preactions execution: no preactions.
		assertEquals(0, TestPreaction.getAllExecutions().size());
		
		// Check postactions execution: one preaction for delete.
		assertEquals(1, TestPostaction.getAllExecutions().size());
		assertEquals(1,
				     TestPostaction.getAllExecutions().get(0).intValue());
		
		// Check the states of the active objects.
		checkActiveObjectStateForUnpackedValue(value, 2);
		
		try { value.pack(); }
		catch (Exception e) {
			e.printStackTrace();
			fail("Couldn't pack the new value");
		}
	}
}

class TestDHTActionExecutorImpl extends DHTActionExecutorImpl {
	public TestDHTActionExecutorImpl() {
		super(null, new TestExecutableDHTActionFactory());
	}
}
class TestExecutableDHTActionFactory implements ExecutableDHTActionFactory {

	@SuppressWarnings("unchecked")
	@Override
	public ExecutableDHTAction createAction(DHTAction action,
                                            HashWrapper key,
                                            ActiveDHTDB db,
                                            long running_timeout) {
		if (action instanceof TestPreaction) {
			return new TestExecutableAction<TestPreaction>(
					(TestPreaction)action);
		} else if (action instanceof TestPostaction) {
			return new TestExecutableAction<TestPostaction>(
					(TestPostaction)action);
		}
		return null;
	}
}

class TestExecutableAction<T extends DHTAction>
extends ExecutableDHTAction<T> {
	public TestExecutableAction(T action) { super(action, null, null); }

	@Override
	protected void executeUsingListener(ActiveDHTOperationListener listener) {
		listener.complete(false);
	}

	@Override
	protected DHTOperationListener getListener() { return null; }
}
