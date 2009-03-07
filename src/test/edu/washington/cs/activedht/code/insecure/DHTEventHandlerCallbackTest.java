package edu.washington.cs.activedht.code.insecure;

import java.net.MalformedURLException;

import edu.washington.cs.activedht.code.ActiveCodeRunnerTest;
import edu.washington.cs.activedht.code.insecure.candefine.ActiveCode;
import edu.washington.cs.activedht.code.insecure.dhtaction.DHTActionList;
import edu.washington.cs.activedht.code.insecure.dhtaction.DHTActionMap;
import edu.washington.cs.activedht.code.insecure.dhtaction.DHTPostaction;
import edu.washington.cs.activedht.code.insecure.dhtaction.DHTPreaction;
import edu.washington.cs.activedht.code.insecure.io.InputStreamSecureClassLoader;
import junit.framework.TestCase;

class TestDHTHandlerClosure extends DHTEventHandlerCallback {
	private boolean was_executed = false;

	public TestDHTHandlerClosure() throws MalformedURLException {
		super(InputStreamSecureClassLoader.newInstance("host.com", 1024));
	}

	@Override
	protected void executeEventOnActiveObject(ActiveCode active_code,
			DHTActionList<DHTPreaction> executed_preactions,
			DHTActionList<DHTPostaction> postactions) {
		if (!(active_code instanceof TestActiveCode)) return;
		this.was_executed = true;
		active_code.onGet("", executed_preactions, postactions);
	}
	
	public boolean was_executed() { return was_executed; }
}

@SuppressWarnings("serial")
class TestActiveCode implements ActiveCode {
	private int value;

	public void onDelete(String caller_ip,
			DHTActionList<DHTPreaction> executed_preactions,
			DHTActionList<DHTPostaction> postactions) {
		onAnyEvent();
	}

	public void onGet(String caller_ip,
			DHTActionList<DHTPreaction> executed_preactions,
			DHTActionList<DHTPostaction> postactions) { onAnyEvent(); }

	public void onInitialPut(String caller_ip,
			DHTActionMap<DHTPreaction> preactions_map) { onAnyEvent(); }

	public void onPut(String caller_ip, byte[] plain_new_value,
			DHTActionList<DHTPreaction> executed_preactions,
			DHTActionList<DHTPostaction> postactions) { onAnyEvent(); }

	public void onPut(String caller_ip, ActiveCode new_active_value,
			DHTActionList<DHTPreaction> executed_preactions,
			DHTActionList<DHTPostaction> postactions) { onAnyEvent(); }
	
	public void onTimer(DHTActionList<DHTPreaction> executed_preactions,
			DHTActionList<DHTPostaction> postactions) { onAnyEvent(); }
	
	public boolean onTest(int value) { return this.value == value; }
	
	private void onAnyEvent() { ++value; }
	
}

public class DHTEventHandlerCallbackTest extends TestCase {
	TestDHTHandlerClosure handler;
	byte[] active_object_bytes;
	
	@Override
	protected void setUp() {
		ActiveCode active_object = new TestActiveCode();
		active_object_bytes =
				ActiveCodeRunnerTest.serializeActiveObject(active_object);
		
		try { handler = new TestDHTHandlerClosure(); }
		catch (MalformedURLException e) {
			e.printStackTrace();
			fail("Could not create handler.");
		}
	}
	
	@Override
	protected void tearDown() { }
	
	public void testHandler() {
		byte[] current_object_bytes = null;
		try {
			current_object_bytes = handler.executeEventOnActiveObject(
					active_object_bytes, null,
					new DHTActionList<DHTPostaction>(2));
		} catch (Exception e) {
			e.printStackTrace();
			fail("Failed to execute handler.");
		}
		
		assertNotNull(current_object_bytes);
		TestActiveCode current_active_object =
			(TestActiveCode)ActiveCodeRunnerTest.instantiateActiveObject(
					current_object_bytes);
		assertNotNull(current_active_object);
		assertTrue(current_active_object.onTest(1));
	}
}
