package edu.washington.cs.activedht.db.kahlua.dhtwrapper;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.util.ArrayList;
import java.util.List;

import junit.framework.TestCase;
import se.krka.kahlua.luaj.compiler.LuaCompiler;
import se.krka.kahlua.vm.JavaFunction;
import se.krka.kahlua.vm.LuaCallFrame;
import se.krka.kahlua.vm.LuaClosure;
import se.krka.kahlua.vm.LuaState;
import se.krka.kahlua.vm.serialize.Serializer;

import com.aelitis.azureus.core.dht.transport.DHTTransportValue;

import edu.washington.cs.activedht.transport.BasicDHTTransportValue;

/**
 * 
 * @author levya
 * 
 */
public class LuaGetCallbackTest extends TestCase {

	private static final LuaState state = new LuaState();
	private static final String[] vals = new String[] { "1234.5",
			"\"hello world\"", "null" };
	private static byte[][] objs = new byte[vals.length][];
	static {
		try {
			for (int i = 0; i < vals.length; ++i) {
				state.call(LuaCompiler.loadstring("x = " + vals[i], "num",
						state.getEnvironment()), new Object[] {});
				ByteArrayOutputStream out = new ByteArrayOutputStream();
				new Serializer(new DataOutputStream(out), state.getEnvironment()).serializeObject(state.getEnvironment().rawget("x"));
				objs[i] = out.toByteArray();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void testCall() throws Exception {
		List<DHTTransportValue> values = new ArrayList<DHTTransportValue>();
		for (byte[] obj : objs) {
			values.add(new BasicDHTTransportValue(0, obj, "", 1,
					null, false, 0));
		}
		state.getEnvironment().rawset("assertEquals", new AssertEquals());
		state.call(LuaCompiler.loadstring("callback = function(tbl) assertEquals(1234.5, tbl[1]); assertEquals(\"hello world\", tbl[2]) end", "num",
				state.getEnvironment()), new Object[] {});
		LuaClosure closure = (LuaClosure)state.getEnvironment().rawget("callback");
		LuaGetCallback callback = new LuaGetCallback(closure, state);
		callback.call(values);
	}
	
	private static class AssertEquals implements JavaFunction {

		public int call(LuaCallFrame callFrame, int nArguments) {
			assertEquals(callFrame.get(0), callFrame.get(1));
			return 0;
		}
		
	}
}
