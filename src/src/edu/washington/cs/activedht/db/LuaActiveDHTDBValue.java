package edu.washington.cs.activedht.db;

import org.keplerproject.luajava.LuaException;
import org.keplerproject.luajava.LuaObject;
import org.keplerproject.luajava.LuaStateFactory;

import com.aelitis.azureus.core.dht.transport.BasicDHTTransportValue;
import com.aelitis.azureus.core.dht.transport.DHTTransportContact;
import com.aelitis.azureus.core.dht.transport.DHTTransportValue;

import edu.washington.cs.activedht.lua.Serializer;

/**
 * @author levya
 * 
 */
public class LuaActiveDHTDBValue implements ActiveDHTDBValue {

	private final LuaObject luaObject;
	private long creationTime;
	private final int version;
	private DHTTransportContact originator;
	private DHTTransportContact sender;
	private final boolean local;
	private int flags;
	private long storeTime;

	protected LuaActiveDHTDBValue(DHTTransportContact sender,
			DHTTransportValue other, boolean local) {
		this(new Serializer(LuaStateFactory.newLuaState()).deserialize(other
				.getValue()), other.getCreationTime(), other.getVersion(),
				other.getOriginator(), sender, local, other.getFlags());
	}

	public LuaActiveDHTDBValue(LuaObject luaObject, long creationTime,
			int version, DHTTransportContact originator,
			DHTTransportContact sender, boolean local, int flags) {
		this.luaObject = luaObject;
		this.creationTime = creationTime;
		this.version = version;
		this.originator = originator;
		this.sender = sender;
		this.local = local;
		this.flags = flags;
	}

	public LuaActiveDHTDBValue(long creationTime, byte[] value, int version,
			DHTTransportContact originator, DHTTransportContact sender,
			boolean local, int flags) {
		this(new Serializer(LuaStateFactory.newLuaState()).deserialize(value),
				creationTime, version, originator, sender, local, flags);
	}

	/**
	 * Executes the specified callback on the lua-activeobject.
	 * 
	 * @param callback
	 *            specifies which callback to execute
	 * @return the value from the callback or null in case of an error
	 */
	public ActiveDHTDBValue executeCallback(String callback) {
		ActiveDHTDBValue value = this;
		try {
			if (luaObject.isTable()) {
				LuaObject callbackFunction = luaObject.getField(callback);
				if (callbackFunction.isFunction()) {
					LuaObject result = (LuaObject) callbackFunction
							.call(new Object[] {luaObject});
					if (result.isNil()) {
						return null;
					} else {
						return new LuaActiveDHTDBValue(result,
								getCreationTime(), getVersion(),
								getOriginator(), getSender(), isLocal(),
								getFlags());
					}
				}
			}
		} catch (LuaException e) {
			return null;
		}
		return value;
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

	public String getString() {
		return "LuaActiveObject: " + luaObject.toString();
	}

	public byte[] getValue() {
		Serializer serializer = new Serializer(luaObject.getLuaState());
		return serializer.serialize(luaObject);
	}

	@Override
	public String toString() {
		return getString();
	}

	public long getStoreTime() {
		return storeTime;
	}

	public DHTTransportValue getValueForDeletion(int version) {
		return new BasicDHTTransportValue(System.currentTimeMillis(), new byte[] {}, "", version, originator, local, flags);
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
}
