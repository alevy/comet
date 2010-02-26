/**
 * 
 */
package edu.washington.cs.activedht.db.kahlua;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.util.Arrays;

import org.gudy.azureus2.core3.util.HashWrapper;

import se.krka.kahlua.luaj.compiler.LuaCompiler;
import se.krka.kahlua.vm.LuaClosure;
import se.krka.kahlua.vm.LuaState;
import se.krka.kahlua.vm.LuaTable;
import se.krka.kahlua.vm.serialize.Deserializer;
import se.krka.kahlua.vm.serialize.Serializer;

import com.aelitis.azureus.core.dht.control.DHTControl;
import com.aelitis.azureus.core.dht.transport.BasicDHTTransportValue;
import com.aelitis.azureus.core.dht.transport.DHTTransportContact;
import com.aelitis.azureus.core.dht.transport.DHTTransportValue;

import edu.washington.cs.activedht.db.ActiveDHTDBValue;
import edu.washington.cs.activedht.db.dhtwrapper.DhtWrapper;

/**
 * @author levya
 * 
 */
public class KahluaActiveDHTDBValue implements ActiveDHTDBValue {

	private final int version;
	private final boolean local;
	private final int flags;

	private byte[] value;
	private long creationTime;
	private long storeTime;
	private DHTTransportContact sender;
	private DHTTransportContact originator;

	private LuaState luaState;
	private Object luaObject;

	public KahluaActiveDHTDBValue(DHTTransportContact sender,
			DHTTransportValue other, boolean local) {
		this(other.getCreationTime(), other.getValue(), other.getVersion(),
				other.getOriginator(), sender, local, other.getFlags());
	}

	public KahluaActiveDHTDBValue(long creationTime, byte[] value, int version,
			DHTTransportContact originator, DHTTransportContact sender,
			boolean local, int flags) {
		this.creationTime = creationTime;
		this.value = value;
		this.version = version;
		this.originator = originator;
		this.sender = sender;
		this.local = local;
		this.flags = flags;

	}

	public ActiveDHTDBValue executeCallback(String callback, Object... args) {
		ActiveDHTDBValue result = this;
		LuaState state = getLuaState();
		synchronized (state) {
			if (luaObject == null) {
				luaObject = new Deserializer(new DataInputStream(
						new ByteArrayInputStream(value)), state
						.getEnvironment()).deserialize();
			}
			Object[] functionArgs = new Object[args.length + 1];
			functionArgs[0] = luaObject;
			for (int i = 1; i < functionArgs.length; ++i) {
				functionArgs[i] = args[i - 1];
			}

			if (LuaTable.class.isInstance(luaObject)) {
				Object function = ((LuaTable) luaObject).rawget(callback);
				if (LuaClosure.class.isInstance(function)) {
					Object returnedValue = state.call(function, args);
					if (returnedValue == null) {
						result = null;
					} else {
						result = new KahluaActiveDHTDBValue(getCreationTime(),
								serialize(returnedValue), getVersion(),
								getOriginator(), getSender(), isLocal(),
								getFlags());
					}
				}
			}

		}
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
		synchronized (state) {
			return new Deserializer(new DataInputStream(
					new ByteArrayInputStream(value)), state.getEnvironment());
		}
	}

	public byte[] serialize(Object object) {
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		new Serializer(new DataOutputStream(bos))
				.serialize(object);
		return bos.toByteArray();
	}
	
	public DhtWrapper getDhtWrapper(DHTControl control, HashWrapper key) {
		// TODO Auto-generated method stub
		return null;
	}


	public long getCreationTime() {
		return creationTime;
	}

	public DHTTransportContact getSender() {
		return sender;
	}

	public long getStoreTime() {
		return storeTime;
	}

	public DHTTransportValue getValueForDeletion(int nextValueVersion) {
		return new BasicDHTTransportValue(getCreationTime(), getValue(),
				getString(), nextValueVersion, getOriginator(), isLocal(),
				getFlags());
	}

	public DHTTransportValue getValueForRelay(DHTTransportContact newOriginator) {
		return new BasicDHTTransportValue(creationTime, getValue(),
				getString(), getVersion(), newOriginator, isLocal(), getFlags());
	}

	public void reset() {
		storeTime = System.currentTimeMillis();
		if (creationTime > storeTime) {
			creationTime = storeTime;
		}
	}

	public void setCreationTime() {
		this.creationTime = System.currentTimeMillis();
	}

	public void setOriginator(DHTTransportContact originator) {
		this.originator = originator;
	}

	public void setSender(DHTTransportContact sender) {
		this.sender = sender;
	}

	public void setStoreTime(long storeTime) {
		this.storeTime = storeTime;
	}

	public int getFlags() {
		return flags;
	}

	public DHTTransportContact getOriginator() {
		return originator;
	}

	public String getString() {
		return "Kahlua ActiveValue";
	}

	public byte[] getValue() {
		return value;
	}

	public int getVersion() {
		return version;
	}

	public boolean isLocal() {
		return local;
	}

	public static void main(String[] args) throws Exception {
		LuaState state = new LuaState();
		LuaClosure closure = LuaCompiler
				.loadstring(
						"ac = { onGet = \"hello world\", onRemove = function() return 1234 end }",
						"stdin", state.getEnvironment());
		state.call(closure, new Object[] {});
		LuaTable env = (LuaTable) state.getEnvironment().rawget("ac");
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		new Serializer(new DataOutputStream(bos)).serializeTable(env);
		byte[] byteArray = bos.toByteArray();
		System.out.println(Arrays.toString(byteArray));
		System.out.println(new Deserializer(new DataInputStream(
				new ByteArrayInputStream(byteArray)), state.getEnvironment())
				.deserialize());
	}

}
