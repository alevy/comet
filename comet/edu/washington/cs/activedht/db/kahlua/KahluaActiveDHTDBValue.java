/**
 * 
 */
package edu.washington.cs.activedht.db.kahlua;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import org.gudy.azureus2.core3.util.HashWrapper;

import se.krka.kahlua.stdlib.BaseLib;
import se.krka.kahlua.stdlib.MathLib;
import se.krka.kahlua.stdlib.StringLib;
import se.krka.kahlua.stdlib.TableLib;
import se.krka.kahlua.vm.ComposedLuaTable;
import se.krka.kahlua.vm.LuaClosure;
import se.krka.kahlua.vm.LuaMapTable;
import se.krka.kahlua.vm.LuaReadOnlyTable;
import se.krka.kahlua.vm.LuaState;
import se.krka.kahlua.vm.LuaTable;
import se.krka.kahlua.vm.serialize.Deserializer;
import se.krka.kahlua.vm.serialize.Serializer;

import com.aelitis.azureus.core.dht.control.DHTControl;
import com.aelitis.azureus.core.dht.db.DHTDBValue;
import com.aelitis.azureus.core.dht.transport.DHTTransportContact;
import com.aelitis.azureus.core.dht.transport.DHTTransportValue;

import edu.washington.cs.activedht.db.ActiveDHTDBValue;
import edu.washington.cs.activedht.db.kahlua.dhtwrapper.DhtWrapper;
import edu.washington.cs.activedht.db.kahlua.dhtwrapper.NodeWrapper;
import edu.washington.cs.activedht.transport.BasicDHTTransportValue;

/**
 * @author levya
 * 
 */
public class KahluaActiveDHTDBValue extends ActiveDHTDBValue {

	public static LuaReadOnlyTable env;
	static {
		LuaMapTable t = new LuaMapTable();
		BaseLib.register(t);
		StringLib.register(t);
		MathLib.register(t);
		TableLib.register(t);
		env = new LuaReadOnlyTable(t);
	}

	private Object luaObject;
	private final Queue<Runnable> postActions = new LinkedList<Runnable>();

	private DHTControl control;
	private HashWrapper key;

	public KahluaActiveDHTDBValue(DHTTransportContact sender,
			DHTTransportValue other, boolean local) {
		this(other.getCreationTime(), other.getValue(), other.getVersion(),
				other.getOriginator(), local, other.getFlags());
	}

	public KahluaActiveDHTDBValue(long creationTime, byte[] value, int version,
			DHTTransportContact originator, boolean local, int flags) {
		super(creationTime, value, "KahluaActiveValue", version, originator,
				local, flags);
	}

	public ActiveDHTDBValue executeCallback(String callback, Object... args) {
		ActiveDHTDBValue result = this;
		if (luaObject == null) {
			luaObject = deserialize(super.getValue());
		}
		if (LuaTable.class.isInstance(luaObject)) {
			LuaTable luaTable = (LuaTable) luaObject;
			Object function = luaTable.rawget(callback);
			if (function == null && luaTable.getMetatable() != null) {
				function = luaTable.getMetatable().rawget(callback);
			}
			if (LuaClosure.class.isInstance(function)) {
				Object returnedValue = call((LuaClosure) function, args);
				if (returnedValue == null) {
					result = null;
				} else if (returnedValue != luaObject) {
					result = new KahluaActiveDHTDBValue(getCreationTime(),
							serialize(returnedValue), getVersion(),
							getOriginator(), isLocal(), getFlags());
				}
			}
		}

		if (!postActions.isEmpty()) {
			final Queue<Runnable> actions = new LinkedList<Runnable>(
					postActions);
			postActions.clear();
			new Thread() {
				@Override
				public void run() {
					yield();
					for (Runnable task : actions) {
						task.run();
					}
				}
			}.start();
		}

		return result;
	}

	public synchronized Object call(LuaClosure function, Object[] args) {
		LuaMapTable dhtMap = new LuaReadOnlyTable();
		LuaState state = new LuaState(new ComposedLuaTable(dhtMap, env));
		DhtWrapper.register(dhtMap, state, key,
				new HashMap<HashWrapper, List<NodeWrapper>>(), control,
				postActions, this);
		Object[] functionArgs = new Object[args.length + 1];
		functionArgs[0] = luaObject;
		for (int i = 1; i < functionArgs.length; ++i) {
			functionArgs[i] = args[i - 1];
		}
		Object returnedValue = null;
		try {
			returnedValue = state.call(function, functionArgs);
		} catch (Exception e) {
			System.err.println(state.currentThread.stackTrace);
			e.printStackTrace();
		}
		return returnedValue;
	}

	public synchronized Object deserialize(byte[] value) {
		return Deserializer.deserializeBytes(value, env);
	}

	public byte[] serialize(Object object) {
		return Serializer.serialize(object, env);
	}

	public void registerGlobalState(DHTControl control, HashWrapper key) {
		this.control = control;
		this.key = key;
	}

	public Object wrap(DHTTransportContact contact) {
		return new NodeWrapper(contact);
	}

	public DHTDBValue getValueForRelay(DHTTransportContact newOriginator) {
		return new BasicDHTTransportValue(getCreationTime(), getValue(),
				getString(), getVersion(), newOriginator, isLocal(), getFlags());
	}

	public String getString() {
		return "Kahlua ActiveValue";
	}

	public synchronized byte[] getValue() {
		if (luaObject != null) {
			return serialize(luaObject);
		}

		return super.getValue();
	}

}
