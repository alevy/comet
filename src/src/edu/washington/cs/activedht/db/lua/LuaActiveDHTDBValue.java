package edu.washington.cs.activedht.db.lua;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.gudy.azureus2.core3.util.HashWrapper;
import org.keplerproject.luajava.LuaException;
import org.keplerproject.luajava.LuaObject;
import org.keplerproject.luajava.LuaState;
import org.keplerproject.luajava.LuaStateFactory;

import com.aelitis.azureus.core.dht.control.DHTControl;
import com.aelitis.azureus.core.dht.transport.BasicDHTTransportValue;
import com.aelitis.azureus.core.dht.transport.DHTTransportContact;
import com.aelitis.azureus.core.dht.transport.DHTTransportValue;

import edu.washington.cs.activedht.db.ActiveDHTDBValue;
import edu.washington.cs.activedht.db.dhtwrapper.DhtWrapper;
import edu.washington.cs.activedht.lua.Serializer;

/**
 * @author levya
 * 
 */
public class LuaActiveDHTDBValue implements ActiveDHTDBValue {

	private static final Logger logger = Logger
			.getLogger(LuaActiveDHTDBValue.class.getName());
	static {
		logger.setLevel(Level.OFF);
	}

	private final Set<String> neighbors = new HashSet<String>();
	private final Queue<Runnable> postActions = new LinkedList<Runnable>();

	private LuaState luaState;
	private Serializer serializer;

	private long creationTime;
	private final int version;
	private DHTTransportContact originator;
	private DHTTransportContact sender;
	private final boolean local;
	private int flags;
	private long storeTime;
	private DhtWrapper dhtWrapper;

	private byte[] value;
	private LuaObject luaObject;
	private boolean luaObjectChanged = false;

	public LuaActiveDHTDBValue(DHTTransportContact sender,
			DHTTransportValue other, boolean local) {
		this(other.getCreationTime(), other.getValue(), other.getVersion(),
				other.getOriginator(), sender, local, other.getFlags());
	}

	public LuaActiveDHTDBValue(long creationTime, byte[] value, int version,
			DHTTransportContact originator, DHTTransportContact sender,
			boolean local, int flags) {
		this.value = value;
		this.creationTime = creationTime;
		this.version = version;
		this.originator = originator;
		this.sender = sender;
		this.local = local;
		this.flags = flags;
	}

	/**
	 * Executes the specified callback on the lua-activeobject.
	 * 
	 * @param callback
	 *            specifies which callback to execute
	 * @return the value from the callback or null in case of an error
	 */
	public synchronized ActiveDHTDBValue executeCallback(String callback,
			final DhtWrapper dhtWrapper, Object... args) {
		ActiveDHTDBValue result = this;
		LuaState state = getLuaState();
		synchronized (state) {
			logger.info("Entering executeCallback");
			if (luaObject == null) {
				luaObject = getSerializer().deserialize(value);
			}

			state.pushJavaObject(dhtWrapper);
			state.setGlobal("dht");
			Object[] functionArgs = new Object[args.length + 1];
			functionArgs[0] = luaObject;
			for (int i = 1; i < functionArgs.length; ++i) {
				functionArgs[i] = args[i - 1];
			}
			try {
				if (luaObject.isTable()) {
					LuaObject callbackFunction = luaObject.getField(callback);
					if (callbackFunction.isFunction()) {
						LuaObject returnValue = (LuaObject) callbackFunction
								.call(functionArgs);
						luaObjectChanged = true;
						if (returnValue.isNil()) {
							result = null;
						} else {
							result = new LuaActiveDHTDBValue(getCreationTime(),
									getSerializer().serialize(returnValue),
									getVersion(), getOriginator(), getSender(),
									isLocal(), getFlags());
						}
					}
				}
			} catch (LuaException e) {
				result = null;
			}
		}

		synchronized (postActions) {
			while (!postActions.isEmpty()) {
				postActions.poll().run();
			}
		}
		logger.info("Exiting executeCallback");
		return result;
	}

	public long getCreationTime() {
		return creationTime;
	}

	public int getVersion() {
		return version;
	}

	public DHTTransportContact getOriginator() {
		return originator;
	}

	public DHTTransportContact getSender() {
		return sender;
	}

	public boolean isLocal() {
		return local;
	}

	public int getFlags() {
		return flags;
	}

	public DHTTransportValue getValueForRelay(DHTTransportContact new_originator) {
		return new BasicDHTTransportValue(creationTime, getValue(),
				getString(), version, originator, local, flags);
	}

	public synchronized String getString() {
		logger.info("getString");
		return "LuaActiveObject";
	}

	public synchronized byte[] serialize(Object object) {
		LuaObject luaObject = null;
		if (LuaObject.class.isInstance(object)) {
			luaObject = LuaObject.class.cast(object);
		} else {
			LuaState state = getLuaState();
			try {
				state.pushObjectValue(object);
			} catch (LuaException e) {
				e.printStackTrace();
				return null;
			}
			luaObject = state.getLuaObject(-1);
			state.pop(1);
		}
		return getSerializer().serialize(luaObject);
	}

	public synchronized Object deserialize(byte[] value) {
		return getSerializer().deserialize(value);
	}

	public synchronized byte[] getValue() {
		logger.info("getValue");
		if (luaObjectChanged) {
			synchronized (getLuaState()) {
				value = getSerializer().serialize(luaObject);
			}
			luaObjectChanged = false;
		}
		return value;
	}

	@Override
	public String toString() {
		return getString();
	}

	public long getStoreTime() {
		return storeTime;
	}

	public DHTTransportValue getValueForDeletion(int version) {
		return new BasicDHTTransportValue(System.currentTimeMillis(),
				new byte[] {}, "", version, originator, local, flags);
	}

	public void setCreationTime() {
		this.creationTime = System.currentTimeMillis();
	}

	public void setStoreTime(long storeTime) {
		this.storeTime = storeTime;
	}

	public void reset() {
		storeTime = System.currentTimeMillis();
		if (creationTime > storeTime) {
			creationTime = storeTime;
		}
	}

	public void setOriginator(DHTTransportContact contact) {
		this.originator = contact;
	}

	public void setSender(DHTTransportContact contact) {
		this.sender = contact;
	}

	public DhtWrapper getDhtWrapper(DHTControl control, HashWrapper key) {
		if (dhtWrapper == null) {
			dhtWrapper = new DhtWrapper(control, key, this, neighbors,
					postActions);
		}
		return dhtWrapper;
	}

	public LuaObject getLuaObject() {
		return luaObject;
	}

	private synchronized LuaState getLuaState() {
		if (luaState == null) {
			luaState = LuaStateFactory.newLuaState();
		}
		return luaState;
	}

	private synchronized Serializer getSerializer() {
		if (serializer == null) {
			serializer = new Serializer(getLuaState());
		}
		return serializer;
	}
}
