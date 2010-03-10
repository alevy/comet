/**
 * 
 */
package edu.washington.cs.activedht.db.kahlua.dhtwrapper;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import junit.framework.TestCase;

import org.gudy.azureus2.core3.util.HashWrapper;

import se.krka.kahlua.vm.LuaCallFrame;
import se.krka.kahlua.vm.LuaClosure;
import se.krka.kahlua.vm.LuaPrototype;
import se.krka.kahlua.vm.LuaState;
import se.krka.kahlua.vm.LuaTable;
import edu.washington.cs.activedht.db.dhtwrapper.GetAction;
import edu.washington.cs.activedht.db.dhtwrapper.LookupAction;
import edu.washington.cs.activedht.db.dhtwrapper.PutAction;
import edu.washington.cs.activedht.db.kahlua.dhtwrapper.DhtWrapper.Function;

/**
 * @author levya
 * 
 */
public class DhtWrapperTest extends TestCase {

	private final LuaState state = new LuaState();
	private final HashWrapper key = new HashWrapper(new byte[] { 1, 2, 34 });
	private Map<HashWrapper, Set<NodeWrapper>> neighbors;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		neighbors = new HashMap<HashWrapper, Set<NodeWrapper>>();
		neighbors.put(key, new HashSet<NodeWrapper>());
	}

	public void testSysTime() {
		DhtWrapper sysTime = new DhtWrapper(Function.SYS_TIME, null, null,
				null, null);
		LuaCallFrame callFrame = new LuaCallFrame(state.currentThread);

		assertEquals(1, sysTime.call(callFrame, 0));
		double currentTime = (Double) callFrame.get(0);
		assertEquals(System.currentTimeMillis(), currentTime, 100);
	}

	public void testGetNoArguments() throws Exception {
		DhtWrapper get = new DhtWrapper(Function.GET, state, key, neighbors,
				null);
		LuaCallFrame callFrame = new LuaCallFrame(state.currentThread);

		assertEquals(0, get.call(callFrame, 0));
		assertEquals(1, get.getPostActions().size());
		GetAction action = (GetAction) get.getPostActions().peek();
		assertEquals(key, action.key);
		assertEquals(0, action.maxValues);
		assertNull(action.getOperationAdapter.callback);
		assertSame(action.getOperationAdapter.neighbors, neighbors.get(key));
	}

	public void testGetKeyArgument() throws Exception {
		DhtWrapper get = new DhtWrapper(Function.GET, state, null, neighbors,
				null);
		LuaCallFrame callFrame = new LuaCallFrame(state.currentThread);

		callFrame.push(key);

		assertEquals(0, get.call(callFrame, 1));
		assertEquals(1, get.getPostActions().size());
		GetAction action = (GetAction) get.getPostActions().peek();
		assertEquals(key, action.key);
		assertEquals(0, action.maxValues);
		assertNull(action.getOperationAdapter.callback);
		assertSame(action.getOperationAdapter.neighbors, neighbors.get(key));
	}

	public void testGetMaxValuesArgument() throws Exception {
		DhtWrapper get = new DhtWrapper(Function.GET, state, null, neighbors,
				null);
		LuaCallFrame callFrame = new LuaCallFrame(state.currentThread);

		callFrame.push(key);
		callFrame.push(12);

		assertEquals(0, get.call(callFrame, 2));
		assertEquals(1, get.getPostActions().size());
		GetAction action = (GetAction) get.getPostActions().peek();
		assertEquals(key, action.key);
		assertEquals(12, action.maxValues);
		assertNull(action.getOperationAdapter.callback);
		assertSame(action.getOperationAdapter.neighbors, neighbors.get(key));
	}

	public void testGetCallbackArgument() throws Exception {
		DhtWrapper get = new DhtWrapper(Function.GET, state, null, neighbors,
				null);
		LuaCallFrame callFrame = new LuaCallFrame(state.currentThread);

		LuaClosure closure = new LuaClosure(new LuaPrototype(), null);
		callFrame.push(key);
		callFrame.push(12);
		callFrame.push(closure);

		assertEquals(0, get.call(callFrame, 3));
		assertEquals(1, get.getPostActions().size());
		GetAction action = (GetAction) get.getPostActions().peek();
		assertEquals(key, action.key);
		assertEquals(12, action.maxValues);
		assertSame(closure,
				((LuaGetCallback) action.getOperationAdapter.callback).closure);
		assertSame(action.getOperationAdapter.neighbors, neighbors.get(key));
	}

	public void testDeleteNoKey() throws Exception {
		DhtWrapper delete = new DhtWrapper(Function.DELETE, state, key,
				neighbors, null);
		LuaCallFrame callFrame = new LuaCallFrame(state.currentThread);

		assertEquals(0, delete.call(callFrame, 0));
		assertEquals(1, delete.getPostActions().size());
		DeleteAction action = (DeleteAction) delete.getPostActions().peek();
		assertEquals(key, action.key);
		assertNotNull(action.operationAdapter);
		assertSame(action.operationAdapter.neighbors, neighbors.get(key));
	}

	public void testDeleteWithKey() throws Exception {
		DhtWrapper delete = new DhtWrapper(Function.DELETE, state, null,
				neighbors, null);
		LuaCallFrame callFrame = new LuaCallFrame(state.currentThread);

		callFrame.push(key);

		assertEquals(0, delete.call(callFrame, 1));
		assertEquals(1, delete.getPostActions().size());
		DeleteAction action = (DeleteAction) delete.getPostActions().peek();
		assertEquals(key, action.key);
		assertNotNull(action.operationAdapter);
		assertSame(action.operationAdapter.neighbors, neighbors.get(key));
	}

	public void testPutNoArguments() throws Exception {
		DhtWrapper put = new DhtWrapper(Function.PUT, state, null, neighbors,
				null);
		LuaCallFrame callFrame = new LuaCallFrame(state.currentThread);
		try {
			assertEquals(0, put.call(callFrame, 0));
			fail("Should have thrown a runtime exception for not enough arguments");
		} catch (RuntimeException e) {
			assertEquals("Not enough arguments", e.getMessage());
		}
	}

	public void testPutNoKey() throws Exception {
		DhtWrapper put = new DhtWrapper(Function.PUT, state, key, neighbors,
				null);
		LuaCallFrame callFrame = new LuaCallFrame(state.currentThread);

		callFrame.push("hello world".getBytes());

		assertEquals(0, put.call(callFrame, 1));
		assertEquals(1, put.getPostActions().size());
		PutAction action = (PutAction) put.getPostActions().peek();
		assertEquals(key, action.key);
		assertNotNull(action.operationAdapter);
		assertSame(action.operationAdapter.neighbors, neighbors.get(key));
	}

	public void testPutWithKey() throws Exception {
		DhtWrapper put = new DhtWrapper(Function.PUT, state, null, neighbors,
				null);
		LuaCallFrame callFrame = new LuaCallFrame(state.currentThread);

		callFrame.push(key);
		callFrame.push("hello world".getBytes());

		assertEquals(0, put.call(callFrame, 2));
		assertEquals(1, put.getPostActions().size());
		PutAction action = (PutAction) put.getPostActions().peek();
		assertEquals(key, action.key);
		assertNotNull(action.operationAdapter);
		assertSame(action.operationAdapter.neighbors, neighbors.get(key));
	}

	public void testLookupNoKey() throws Exception {
		DhtWrapper lookup = new DhtWrapper(Function.LOOKUP, state, key,
				neighbors, null);
		LuaCallFrame callFrame = new LuaCallFrame(state.currentThread);

		assertEquals(0, lookup.call(callFrame, 0));
		assertEquals(1, lookup.getPostActions().size());
		LookupAction action = (LookupAction) lookup.getPostActions().peek();
		assertEquals(key, action.key);
		assertNotNull(action.operationAdapter);
		assertSame(action.operationAdapter.neighbors, neighbors.get(key));
	}

	public void testLookupWithKey() throws Exception {
		DhtWrapper lookup = new DhtWrapper(Function.LOOKUP, state, null,
				neighbors, null);
		LuaCallFrame callFrame = new LuaCallFrame(state.currentThread);

		HashWrapper key = new HashWrapper(new byte[] { 1, 2, 34 });
		callFrame.push(key);

		assertEquals(0, lookup.call(callFrame, 1));
		assertEquals(1, lookup.getPostActions().size());
		LookupAction action = (LookupAction) lookup.getPostActions().peek();
		assertEquals(key, action.key);
		assertNotNull(action.operationAdapter);
		assertSame(action.operationAdapter.neighbors, neighbors.get(key));
	}

	public void testKey() throws Exception {
		DhtWrapper getKey = new DhtWrapper(Function.KEY, state, key, null, null);
		LuaCallFrame callFrame = new LuaCallFrame(state.currentThread);
		assertEquals(1, getKey.call(callFrame, 0));
		assertEquals(key, callFrame.get(0));
	}

	public void testRegister() {
		DhtWrapper.register(state, null, null, null, null);
		LuaTable dht = (LuaTable) state.getEnvironment().rawget("dht");
		assertNotNull(dht);
		for (Function function : Function.values()) {
			DhtWrapper wrapper = (DhtWrapper) dht.rawget(function.name);
			assertNotNull(wrapper);
			assertEquals(function, wrapper.getFunction());
		}
	}

}
