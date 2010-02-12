package edu.washington.cs.activedht.db.lua;

import java.util.ArrayList;
import java.util.List;

import junit.framework.TestCase;

import org.keplerproject.luajava.LuaObject;
import org.keplerproject.luajava.LuaState;
import org.keplerproject.luajava.LuaStateFactory;

import com.aelitis.azureus.core.dht.DHT;

import edu.washington.cs.activedht.lua.Serializer;

public class LuaActiveDHTDBValueTest extends TestCase {

	public void testExecuteAction() throws Exception {
		LuaState state = LuaStateFactory.newLuaState();
		state
				.LdoString("activeobject = { onGet = function(self) return \"hello\" end }");
		LuaObject luaObject = state.getLuaObject("activeobject");
		Serializer serializer = new Serializer(state);
		while (true) {
			byte[] bytes = serializer.serialize(luaObject);
		}
	}

	public void testStressTest() throws Exception {
		LuaState state = LuaStateFactory.newLuaState();
		state
				.LdoString("activeobject = { onGet = function(self) return \"hello\" end }");
		byte[] bytes = new Serializer(state).serialize(state
				.getLuaObject("activeobject"));
		LuaActiveDHTDBValue value = new LuaActiveDHTDBValue(0, bytes, 1, null,
				null, false, (byte) DHT.FLAG_STATS);
		List<ExecThread> threads = new ArrayList<ExecThread>();
		for (int i = 0; i < 100; ++i) {
			threads.add(new ExecThread(value, "onGet"));
		}
		for (ExecThread thread : threads) {
			thread.start();
		}
		for (ExecThread thread : threads) {
			thread.join();
		}
	}

	private static class ExecThread extends Thread {
		private final LuaActiveDHTDBValue value;
		private final String callback;

		public ExecThread(LuaActiveDHTDBValue value, String callback) {
			this.value = value;
			this.callback = callback;
		}

		public void run() {
			LuaActiveDHTDBValue result = (LuaActiveDHTDBValue) value
					.executeCallback(callback, value.getDhtWrapper(null, null));
			assertEquals("hello", result.getLuaObject().getString());
		}
	}

}
