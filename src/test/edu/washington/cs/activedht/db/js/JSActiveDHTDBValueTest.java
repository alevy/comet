package edu.washington.cs.activedht.db.js;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;

import junit.framework.TestCase;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.ScriptableObject;
import org.mozilla.javascript.serialize.ScriptableInputStream;
import org.mozilla.javascript.serialize.ScriptableOutputStream;

import edu.washington.cs.activedht.db.ActiveDHTDBValue;
import edu.washington.cs.activedht.db.dhtwrapper.DhtWrapper;

/**
 * @author levya
 *
 */
public class JSActiveDHTDBValueTest extends TestCase {

	public void testDhtGetIp() throws Exception {
		Context context = Context.enter();
		context.setOptimizationLevel(-1);
		ScriptableObject scope = context.initStandardObjects();
		Object obj = context.evaluateString(scope, "activeobject = {onGet: function(self) { return dht.getIP() } }", "", 1, null);
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		ScriptableOutputStream sos = new ScriptableOutputStream(out, scope);
		sos.writeObject(obj);
		
		JSActiveDHTDBValue activeValue = new JSActiveDHTDBValue(out.toByteArray(), 0, 1, null, null, false, 0);
		activeValue.setDhtWrapper(new TestDhtWrapper("192.168.1.1"));
		ActiveDHTDBValue result = activeValue.executeCallback("onGet", activeValue.getDhtWrapper(null, null), "hello");
		Object finalObject = new ScriptableInputStream(new ByteArrayInputStream(result.getValue()), scope).readObject();
		assertEquals("192.168.1.1", finalObject);
	}
	
	public void testExceptionInJSReturnsNull() throws Exception {
		Context context = Context.enter();
		context.setOptimizationLevel(-1);
		ScriptableObject scope = context.initStandardObjects();
		Object obj = context.evaluateString(scope, "activeobject = {onGet: function(self) { return self.notafunction() } }", "", 1, null);
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		ScriptableOutputStream sos = new ScriptableOutputStream(out, scope);
		sos.writeObject(obj);
		
		JSActiveDHTDBValue activeValue = new JSActiveDHTDBValue(out.toByteArray(), 0, 1, null, null, false, 0);
		ActiveDHTDBValue result = activeValue.executeCallback("onGet", null, "hello");
		assertNull(result);
	}
	
	private static class TestDhtWrapper extends DhtWrapper {
		private final String ip;

		public TestDhtWrapper(String ip) {
			super(null, null, null, new HashSet<String>(), null);
			this.ip = ip;
		}
		
		public Collection<String> getNeighbors() {
			return Collections.unmodifiableCollection(super.getNeighbors());
		}
		
		public String getIP() {
			return ip;
		}
	}
	
}
