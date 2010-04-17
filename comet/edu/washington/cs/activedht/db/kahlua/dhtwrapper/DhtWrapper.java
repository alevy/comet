package edu.washington.cs.activedht.db.kahlua.dhtwrapper;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Queue;
import java.util.SortedSet;
import java.util.TreeSet;

import org.gudy.azureus2.core3.util.HashWrapper;
import org.gudy.azureus2.core3.util.SHA1Simple;

import se.krka.kahlua.stdlib.BaseLib;
import se.krka.kahlua.vm.JavaFunction;
import se.krka.kahlua.vm.LuaCallFrame;
import se.krka.kahlua.vm.LuaClosure;
import se.krka.kahlua.vm.LuaMapTable;
import se.krka.kahlua.vm.LuaReadOnlyTable;
import se.krka.kahlua.vm.LuaState;
import se.krka.kahlua.vm.LuaTable;
import se.krka.kahlua.vm.serialize.Serializer;

import com.aelitis.azureus.core.dht.control.DHTControl;

import edu.washington.cs.activedht.db.dhtwrapper.GetAction;
import edu.washington.cs.activedht.db.dhtwrapper.GetCallback;
import edu.washington.cs.activedht.db.dhtwrapper.GetOperationAdapter;
import edu.washington.cs.activedht.db.dhtwrapper.LookupAction;
import edu.washington.cs.activedht.db.dhtwrapper.PutAction;
import edu.washington.cs.activedht.db.dhtwrapper.UpdateNeighborsCallback;
import edu.washington.cs.activedht.db.dhtwrapper.UpdateNeighborsOperationAdapter;
import edu.washington.cs.activedht.db.kahlua.KahluaActiveDHTDBValue;

public class DhtWrapper implements JavaFunction {

	public static enum Function {
		SYS_TIME("sysTime"), KEY("key"), GET("get"), PUT("put"), DELETE(
				"delete"), LOOKUP("lookup"), SET_TIMER("setTimerInterval");
		String name;

		Function(String name) {
			this.name = name;

		}
	}

	private final Queue<Runnable> postActions;

	private final Function function;
	private final Map<HashWrapper, SortedSet<NodeWrapper>> neighbors;
	private final DHTControl control;

	private final LuaState state;

	private final HashWrapper key;

	private final KahluaActiveDHTDBValue value;

	protected DhtWrapper(Function function, LuaState state, HashWrapper key,
			Map<HashWrapper, SortedSet<NodeWrapper>> neighbors,
			DHTControl control) {
		this(function, state, key, neighbors, control,
				new LinkedList<Runnable>(), null);
	}

	public DhtWrapper(Function function, LuaState state, HashWrapper key,
			Map<HashWrapper, SortedSet<NodeWrapper>> neighbors,
			DHTControl control, Queue<Runnable> postActions, KahluaActiveDHTDBValue value) {
		this.function = function;
		this.state = state;
		this.key = key;
		this.neighbors = neighbors;
		this.value = value;
		this.neighbors.put(key, new TreeSet<NodeWrapper>());
		this.control = control;
		this.postActions = postActions;
	}

	public int call(LuaCallFrame callFrame, int nArguments) {
		switch (function) {
		case SYS_TIME:
			return getSystemTime(callFrame, nArguments);
		case KEY:
			return getKey(callFrame, nArguments);
		case GET:
			return get(callFrame, nArguments);
		case PUT:
			return put(callFrame, nArguments);
		case DELETE:
			return delete(callFrame, nArguments);
		case LOOKUP:
			return lookup(callFrame, nArguments);
		case SET_TIMER:
			return setTimer(callFrame, nArguments);
		default:
			return 0;
		}
	}

	private int setTimer(LuaCallFrame callFrame, int nArguments) {
		BaseLib.luaAssert(nArguments > 0, "Excpected one argument");
		int units = ((Double)callFrame.get(0)).intValue();
		BaseLib.luaAssert(units >= 0, "Interval cannot be negative.");
		
		value.setIntervalUnit(units);
		return 0;
	}
	
	private int lookup(LuaCallFrame callFrame, int nArguments) {
		HashWrapper key = this.key;
		if (nArguments > 0) {
			key = (HashWrapper) callFrame.get(0);
		}
		UpdateNeighborsCallback callback = null;
		if (nArguments > 1) {
			final LuaClosure closure = (LuaClosure) callFrame.get(1);
			callback = new LuaUpdateNeighborsCallback(closure, value);
		}
		postActions.offer(new LookupAction(key, control,
				new UpdateNeighborsOperationAdapter(neighbors.get(key),
						callback)));
		return 0;
	}

	private int delete(LuaCallFrame callFrame, int nArguments) {
		HashWrapper key = this.key;
		if (nArguments > 0) {
			key = (HashWrapper) callFrame.get(0);
		}
		UpdateNeighborsCallback callback = null;
		if (nArguments > 1) {
			final LuaClosure closure = (LuaClosure) callFrame.get(1);
			callback = new LuaUpdateNeighborsCallback(closure, value);
		}
		postActions.offer(new DeleteAction(key, control,
				new UpdateNeighborsOperationAdapter(neighbors.get(key),
						callback)));
		return 0;
	}

	private int put(LuaCallFrame callFrame, int nArguments) {
		HashWrapper key = this.key;
		BaseLib.luaAssert(nArguments > 1, "Not enough arguments");
		key = (HashWrapper) callFrame.get(0);
		byte[] value = Serializer.serialize(callFrame.get(1), state
				.getEnvironment());

		Object obj = 20.0;
		if (nArguments > 2) {
			obj = callFrame.get(2);
		}
		
		UpdateNeighborsCallback callback = null;
		if (nArguments > 3) {
			final LuaClosure closure = (LuaClosure) callFrame.get(3);
			callback = new LuaUpdateNeighborsCallback(closure, this.value);
		}

		SortedSet<NodeWrapper> nbrs = neighbors.get(key);
		if (LuaTable.class.isInstance(obj)) {
			List<NodeWrapper> nodes = new ArrayList<NodeWrapper>();
			LuaTable table = (LuaTable) obj;
			for (int next = 1; next <= table.len(); ++next) {
				Object elm = table.rawget(next);
				BaseLib.luaAssert(NodeWrapper.class.isInstance(elm),
						"Object not a NodeWrapper");
				nodes.add((NodeWrapper) elm);
			}
			postActions.offer(new PutAction(key, value, nodes, control,
					new UpdateNeighborsOperationAdapter(nbrs, callback)));
		} else {
			BaseLib.luaAssert(Double.class.isInstance(obj),
					"Expecting number of replicas");
			int numNodes = ((Double) obj).intValue();
			postActions.offer(new PutAction(key, value, numNodes, control,
					new UpdateNeighborsOperationAdapter(nbrs, callback)));
		}
		return 0;
	}

	private int get(LuaCallFrame callFrame, int nArguments) {
		HashWrapper key = this.key;
		if (nArguments > 0) {
			key = (HashWrapper) callFrame.get(0);
		}
		int maxValues = 0;
		if (nArguments > 1) {
			maxValues = (Integer) callFrame.get(1);
		}
		GetCallback callback = null;
		if (nArguments > 2) {
			final LuaClosure closure = (LuaClosure) callFrame.get(2);
			callback = new LuaGetCallback(closure, state);
		}
		postActions.offer(new GetAction(key, this.key, maxValues, control,
				new GetOperationAdapter(neighbors.get(key), callback)));
		return 0;
	}

	private int getKey(LuaCallFrame callFrame, int nArguments) {
		HashWrapper key = this.key;
		if (nArguments > 0) {
			byte[] bytes = key.getBytes();
			int n = (int)((double)((Double) callFrame.get(0)));
			BaseLib.luaAssert(n <= 5, "Child to compute is too far away: " + n);
			for (int i = 0; i < n; ++i) {
				bytes = new SHA1Simple().calculateHash(bytes, 0, bytes.length);
			}
			key = new HashWrapper(bytes);
		}
		callFrame.push(key);
		return 1;
	}

	private int getSystemTime(LuaCallFrame callFrame, int nArguments) {
		callFrame.push((double) System.currentTimeMillis());
		return 1;
	}

	public Function getFunction() {
		return function;
	}

	public Queue<Runnable> getPostActions() {
		return postActions;
	}

	public static LuaReadOnlyTable register(LuaMapTable outerTable,
			LuaState state, HashWrapper key,
			Map<HashWrapper, SortedSet<NodeWrapper>> neighbors,
			DHTControl control, Queue<Runnable> postActions, KahluaActiveDHTDBValue value) {
		NodeWrapper node = null;
		if (control != null) {
			node = new NodeWrapper(control.getTransport().getLocalContact());
		}

		LuaReadOnlyTable dht = new LuaReadOnlyTable();
		outerTable.table.put("dht", dht);

		NavigableMap<Object, Object> dhtMap = dht.table;
		dhtMap.put("localNode", node);

		for (Function function : Function.values()) {
			dhtMap.put(function.name, new DhtWrapper(function, state, key,
					neighbors, control, postActions, value));
		}
		return dht;
	}

}