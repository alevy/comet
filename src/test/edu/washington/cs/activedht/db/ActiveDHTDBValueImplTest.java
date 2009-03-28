package edu.washington.cs.activedht.db;

import java.io.IOException;
import java.util.Arrays;

import com.aelitis.azureus.core.dht.transport.DHTTransportContact;

import edu.washington.cs.activedht.code.insecure.DHTEvent;
import edu.washington.cs.activedht.code.insecure.DHTEventHandlerCallbackTest;
import edu.washington.cs.activedht.code.insecure.candefine.ActiveCode;
import edu.washington.cs.activedht.code.insecure.candefine.TestActiveCode;
import edu.washington.cs.activedht.code.insecure.dhtaction.DHTActionMap;
import edu.washington.cs.activedht.code.insecure.dhtaction.TestPostaction;
import edu.washington.cs.activedht.code.insecure.dhtaction.TestPreaction;
import edu.washington.cs.activedht.db.coderunner.IllegalPackingStateException;

import junit.framework.TestCase;

public class ActiveDHTDBValueImplTest extends TestCase {
	ActiveCode active_object;
	byte[] active_object_bytes;
	
	DHTTransportContact node;
	
	ActiveDHTDBValueImpl value;
	
	@Override
	protected void setUp() {
		active_object = new TestActiveCode(DHTEvent.GET,
				new TestPreaction(0), new TestPostaction(0));
		active_object_bytes = DHTEventHandlerCallbackTest
				.serializeActiveObject(active_object);

		node = new TestDHTClasses.TestDHTTransportContact(1);
		
		value = new ActiveDHTDBValueImpl(
					node,
					new TestDHTClasses.TestDHTTransportValue(
							node,
							active_object_bytes,
							false),
					false);
		
	}
	
	@Override
	protected void tearDown() { }
	
	public void testInitialUnpackAndPack() {
		DHTActionMap preactions = new DHTActionMap(2);
		try { value.unpack(preactions); }
		catch (Exception e) {
			e.printStackTrace();
			fail("Failed to unpack");
		}
		
		try { assertEquals(preactions, value.getPreactions()); }
		catch (IllegalPackingStateException e1) {
			e1.printStackTrace();
			fail("Failed to getPreactions() from value");
		}

		TestActiveCode code = (TestActiveCode)DHTEventHandlerCallbackTest
			.instantiateActiveObject(value.getValue(),
					node.getAddress().getHostName(),
					node.getAddress().getPort());
		
		assertNotNull(code);
		assertTrue(code.onTest(0));

		try { value.pack(); }
		catch (IOException e) {
			e.printStackTrace();
			fail("Failed to pack");
		}
		
		try {
			value.getPreactions();
			fail("Packed value has non-null preactions.");
		} catch (IllegalPackingStateException e) { }  // expected
		
		assertNotNull(value.getValue());
		assertFalse(Arrays.equals(active_object_bytes, value.getValue()));
	}
	
	public void testUnpackAfterPack() {
		// Pack and unpack it once.
		DHTActionMap preactions = new DHTActionMap(2);
		try {
			value.unpack(preactions);
			value.pack(); 
		} catch (Exception e) {
			e.printStackTrace();
			fail("Failed to pack/unpack.");
		}
		
		// Unpack it the second time.
		try { value.unpack(null); }
		catch (Exception e) {
			e.printStackTrace();
			fail("Failed to unpack.");
		}

		try { assertEquals(preactions, value.getPreactions()); }
		catch (IllegalPackingStateException e1) {
			e1.printStackTrace();
			fail("Failed to getPreactions() from value.");
		}

		// The active object should be identical.
		assertTrue(Arrays.equals(this.active_object_bytes, value.getValue()));
		TestActiveCode code = (TestActiveCode)DHTEventHandlerCallbackTest
			.instantiateActiveObject(value.getValue(),
				node.getAddress().getHostName(),
				node.getAddress().getPort());
		
		assertNotNull(code);
		assert(code.onTest(0));
	}
}
