package edu.washington.cs.activedht.code;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.Arrays;

import edu.washington.cs.activedht.code.insecure.ActiveCodeSandbox;
import edu.washington.cs.activedht.code.insecure.DHTEvent;
import edu.washington.cs.activedht.code.insecure.candefine.ActiveCode;
import edu.washington.cs.activedht.code.insecure.candefine.TestActiveCode;
import edu.washington.cs.activedht.code.insecure.dhtaction.DHTActionMap;
import edu.washington.cs.activedht.code.insecure.dhtaction.DHTPreaction;
import edu.washington.cs.activedht.code.insecure.dhtaction.TestPostaction;
import edu.washington.cs.activedht.code.insecure.dhtaction.TestPreaction;
import edu.washington.cs.activedht.code.insecure.io.ClassObjectInputStream;
import edu.washington.cs.activedht.code.insecure.io.ClassObjectOutputStream;
import edu.washington.cs.activedht.code.insecure.io.InputStreamSecureClassLoader;
import edu.washington.cs.activedht.util.Constants;
import junit.framework.TestCase;

public class ActiveCodeRunnerTest extends TestCase {
	private ActiveCodeRunner runner;
	private byte[] value_bytes;
	
	// TestCase functions:
	
	@Override
	protected void setUp() {
		try {
			runner = new ActiveCodeRunner(
					new ActiveCodeSandbox<byte[]>(10000), "host.com", 1024);
		} catch (MalformedURLException e) {
			e.printStackTrace();
			fail("Could not create wrapper.");
		}
		ActiveCode object = new TestActiveCode(DHTEvent.GET);
		value_bytes = serializeActiveObject(object);
	}
	
	@Override
	protected void tearDown() { }

	// Helpers:
	
	public static byte[] serializeActiveObject(ActiveCode active_code) {
		byte[] serialized_object = null;
		try {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			ClassObjectOutputStream oos = new ClassObjectOutputStream(baos);
			oos.writeObject(active_code);
			serialized_object = baos.toByteArray();
			oos.close();
			baos.close();
		} catch (IOException e) {
			e.printStackTrace();
			fail("Could not serialize ActiveCode object.");
		}
		return serialized_object;
	}
	
	public static ActiveCode instantiateActiveObject(byte[] value_bytes) {
		ByteArrayInputStream bais = new ByteArrayInputStream(value_bytes);
		ActiveCode deserialized_object = null;
		try {
			ClassObjectInputStream cois = new ClassObjectInputStream(bais,
				InputStreamSecureClassLoader.newInstance("host.com", 1024));
			deserialized_object = (ActiveCode)cois.readObject();
			cois.close();
		} catch (Exception e) {
			e.printStackTrace();
			fail("Could not de-serialize ActiveCode object.");
		}
		return deserialized_object;
	}

	// Test cases:

	public void testOnInitialPutOnGet() {
		// Do a first PUT:
		// TestActiveCode.OnFirstPut() does not trigger either preactions or
		// postactions.
		DHTActionMap<DHTPreaction> all_preactions =
			new DHTActionMap<DHTPreaction>(
					Constants.MAX_NUM_DHT_ACTIONS_PER_EVENT);
		byte[] current_value_bytes =
			runner.onInitialPut(all_preactions, value_bytes, "caller.com");

		// Check the preactions, the object, and the postactions.
		assertNotNull(all_preactions.getActionsForEvent(DHTEvent.GET));
		assertEquals(1,
				all_preactions.getActionsForEvent(DHTEvent.GET).size());
		assertFalse(all_preactions.getActionsForEvent(DHTEvent.GET)
				.getAction(0).actionWasExecuted());
		assertEquals(0, TestPreaction.getNumTimesExecuted());
		assertNotNull(current_value_bytes);
		assertTrue(Arrays.equals(value_bytes, current_value_bytes));
		TestActiveCode current_object =
				(TestActiveCode)instantiateActiveObject(current_value_bytes);
		assertTrue(current_object.onTest(0));  // called zero times (initial
				// put doesn't count for TestActiveCode).
		assertEquals(0, TestPostaction.getNumTimesExecuted());  // initial put
		        // in TestActiveCode doesn't return any postaction.

		// Do a GET:
		// TestActiveCode.OnGet() trigger both preactions and postactions.
		current_value_bytes = runner.onGet(
				all_preactions.getActionsForEvent(DHTEvent.GET),
				current_value_bytes, "caller.com");

		// Check the preactions, the object, and the postactions:
		assertEquals(1, TestPreaction.getNumTimesExecuted());  // was reset
		assertNotNull(current_value_bytes);
		assertFalse(Arrays.equals(value_bytes, current_value_bytes));
		current_object =
			(TestActiveCode)instantiateActiveObject(current_value_bytes);
		assertTrue(current_object.onTest(1));  // called once.
		assertEquals(1, TestPreaction.getNumTimesExecuted());
		
		// Do another GET atop the result:
		// TestActiveCode.OnGet() trigger both preactions and postactions.
		current_value_bytes = runner.onGet(
				all_preactions.getActionsForEvent(DHTEvent.GET),
				current_value_bytes, "caller.com");

		// Check the preactions, the object, and the postactions:
		assertEquals(2, TestPreaction.getNumTimesExecuted());  // was reset
		assertNotNull(current_value_bytes);
		assertFalse(Arrays.equals(value_bytes, current_value_bytes));
		current_object =
			(TestActiveCode)instantiateActiveObject(current_value_bytes);
		assertTrue(current_object.onTest(2));  // called once.
		assertEquals(2, TestPreaction.getNumTimesExecuted());
	}
}
