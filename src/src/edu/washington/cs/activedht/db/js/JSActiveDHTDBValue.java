package edu.washington.cs.activedht.db.js;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.gudy.azureus2.core3.util.HashWrapper;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.mozilla.javascript.serialize.ScriptableInputStream;
import org.mozilla.javascript.serialize.ScriptableOutputStream;

import com.aelitis.azureus.core.dht.control.DHTControl;
import com.aelitis.azureus.core.dht.transport.BasicDHTTransportValue;
import com.aelitis.azureus.core.dht.transport.DHTTransportContact;
import com.aelitis.azureus.core.dht.transport.DHTTransportValue;

import edu.washington.cs.activedht.db.ActiveDHTDBValue;
import edu.washington.cs.activedht.db.DhtWrapper;

/**
 * @author alevy
 * 
 */
public class JSActiveDHTDBValue implements ActiveDHTDBValue {

	private final Context context = Context.enter();
	private final ScriptableObject scope = context.initStandardObjects();
	private final Object jsObj;
	private final int flags;
	private final int version;
	private final boolean local;

	private long creationTime;
	private DHTTransportContact sender;
	private long storeTime;
	private DHTTransportContact originator;

	private JSActiveDHTDBValue(Object jsObj, long creationTime, int version,
			DHTTransportContact originator, DHTTransportContact sender,
			boolean local, int flags) {
		context.setOptimizationLevel(-1);
		this.creationTime = creationTime;
		this.version = version;
		this.originator = originator;
		this.sender = sender;
		this.local = local;
		this.flags = flags;
		this.jsObj = jsObj;
	}

	public JSActiveDHTDBValue(byte[] value, long creationTime, int version,
			DHTTransportContact originator, DHTTransportContact sender,
			boolean local, int flags) {
		context.setOptimizationLevel(-1);
		this.creationTime = creationTime;
		this.version = version;
		this.originator = originator;
		this.sender = sender;
		this.local = local;
		this.flags = flags;
		ScriptableInputStream sis;
		try {
			sis = new ScriptableInputStream(new ByteArrayInputStream(value),
					scope);
			jsObj = sis.readObject();
		} catch (Exception e) {
			throw new IllegalArgumentException(e);
		}
	}

	public ActiveDHTDBValue executeCallback(String callback,
			DhtWrapper dhtWrapper, Object... args) {
		ActiveDHTDBValue result = this;
		if (Scriptable.class.isInstance(jsObj)) {
			Object func = ((Scriptable) jsObj).get(callback, scope);
			if (Function.class.isInstance(func)) {
				Object returnValue = ((Function) func).call(context, scope,
						scope, args);
				result = new JSActiveDHTDBValue(returnValue, creationTime,
						version, originator, sender, local, flags);
			}
		}
		return result;
	}

	public Object deserialize(byte[] value) {
		try {
			return new ScriptableInputStream(new ByteArrayInputStream(value),
					scope).readObject();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	public DhtWrapper getDhtWrapper(DHTControl control, HashWrapper key) {
		return null;
	}

	public byte[] serialize(Object object) {
		ByteArrayOutputStream os = new ByteArrayOutputStream();
		try {
			new ScriptableOutputStream(os, scope).writeObject(object);
			return os.toByteArray();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return new byte[] {};
	}

	public long getCreationTime() {
		return this.creationTime;
	}

	public DHTTransportContact getSender() {
		return this.sender;
	}

	@Override
	public long getStoreTime() {
		return this.storeTime;
	}

	public DHTTransportValue getValueForDeletion(int version) {
		return new BasicDHTTransportValue(System.currentTimeMillis(),
				new byte[] {}, "", version, originator, local, flags);
	}

	public DHTTransportValue getValueForRelay(DHTTransportContact new_originator) {
		return new BasicDHTTransportValue(creationTime, getValue(),
				getString(), version, originator, local, flags);
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

	public void setOriginator(DHTTransportContact contact) {
		this.originator = contact;
	}

	public void setSender(DHTTransportContact contact) {
		this.sender = contact;
	}

	public void setStoreTime(long l) {
		this.storeTime = l;
	}

	public int getFlags() {
		return this.flags;
	}

	public DHTTransportContact getOriginator() {
		return this.originator;
	}

	public String getString() {
		return "JSActiveObject";
	}

	public byte[] getValue() {
		return serialize(jsObj);
	}

	public int getVersion() {
		return this.version;
	}

	public boolean isLocal() {
		return this.local;
	}
}
