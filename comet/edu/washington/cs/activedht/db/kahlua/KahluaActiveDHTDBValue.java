/**
 * 
 */
package edu.washington.cs.activedht.db.kahlua;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Queue;
import java.util.SortedSet;

import org.gudy.azureus2.core3.util.HashWrapper;

import se.krka.kahlua.vm.LuaClosure;
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

	private byte[] value;

	private Object lock;
	private LuaState luaState;
	private Object luaObject;
	private final Queue<Runnable> postActions = new LinkedList<Runnable>();

	private LuaTable dhtWrapper;


	public KahluaActiveDHTDBValue(DHTTransportContact sender,
			DHTTransportValue other, boolean local) {
		this(other.getCreationTime(), other.getValue(), other.getVersion(),
				other.getOriginator(), local, other.getFlags());
	}

	public KahluaActiveDHTDBValue(long creationTime, byte[] value, int version,
			DHTTransportContact originator, boolean local, int flags) {
		super(creationTime, value, "KahluaActiveValue", version, originator, local, flags);
		this.value = value;
	}

	public ActiveDHTDBValue executeCallback(String callback, Object... args) {
		ActiveDHTDBValue result = this;
		LuaState state = getLuaState();
		synchronized (lock) {
			if (luaObject == null) {
				luaObject = Deserializer.deserializeBytes(value, state
						.getEnvironment());
			}
			Object[] functionArgs = new Object[args.length + 1];
			functionArgs[0] = luaObject;
			for (int i = 1; i < functionArgs.length; ++i) {
				functionArgs[i] = args[i - 1];
			}

			if (LuaTable.class.isInstance(luaObject)) {
				LuaTable luaTable = (LuaTable) luaObject;
				Object function = luaTable.rawget(callback);
				if (function == null && luaTable.getMetatable() != null) {
					function = luaTable.getMetatable().rawget(callback);
				}
				if (LuaClosure.class.isInstance(function)) {
					Object returnedValue = null;
					try {
						returnedValue = state.call(function, functionArgs);
					} catch (Exception e) {
						e.printStackTrace();
					}
					if (returnedValue == null) {
						result = null;
					} else {
						result = new KahluaActiveDHTDBValue(getCreationTime(),
								serialize(returnedValue), getVersion(),
								getOriginator(), isLocal(),
								getFlags());
					}
				}
			}

		}
		
		for (Runnable task : postActions) {
			task.run();
		}
		postActions.clear();
		
		return result;
	}

	private LuaState getLuaState() {
		if (luaState == null) {
			luaState = new LuaState();
		}
		return luaState;
	}

	public Object deserialize(byte[] value) {
		LuaState state = getLuaState();
		synchronized (lock) {
			return Deserializer.deserializeBytes(value, state.getEnvironment());
		}
	}

	public byte[] serialize(Object object) {
		return Serializer.serialize(object, getLuaState().getEnvironment());
	}

	public void registerGlobalState(DHTControl control, HashWrapper key) {
		dhtWrapper = DhtWrapper.register(getLuaState(), key,
				new HashMap<HashWrapper, SortedSet<NodeWrapper>>(), control, postActions);
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

	public byte[] getValue() {
		return value;
	}

}
