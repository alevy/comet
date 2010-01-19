package edu.washington.cs.activedht.db.js;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;

import org.gudy.azureus2.core3.util.HashWrapper;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.NativeJavaObject;
import org.mozilla.javascript.RhinoException;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.mozilla.javascript.serialize.ScriptableInputStream;
import org.mozilla.javascript.serialize.ScriptableOutputStream;

import com.aelitis.azureus.core.dht.control.DHTControl;
import com.aelitis.azureus.core.dht.db.DHTDBValue;
import com.aelitis.azureus.core.dht.transport.BasicDHTTransportValue;
import com.aelitis.azureus.core.dht.transport.DHTTransportContact;
import com.aelitis.azureus.core.dht.transport.DHTTransportValue;

import edu.washington.cs.activedht.db.ActiveDHTDBValue;
import edu.washington.cs.activedht.db.dhtwrapper.DhtWrapper;

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

	private final Set<String> neighbors = new HashSet<String>();
	private final Queue<Runnable> postActions = new LinkedList<Runnable>();

	private long creationTime;
	private DHTTransportContact sender;
	private long storeTime;
	private DHTTransportContact originator;
	private DhtWrapper dhtWrapper;

	private JSActiveDHTDBValue(Object jsObj, long creationTime, int version,
			DHTTransportContact originator, DHTTransportContact sender,
			boolean local, int flags) {
		initContext();
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
		initContext();
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

	private void initContext() {
		context.setOptimizationLevel(-1);
		if (!context.hasClassShutter()) {
			context.setClassShutter(new ActiveDhtClassShutter());
		}
	}

	public ActiveDHTDBValue executeCallback(String callback,
			DhtWrapper dhtWrapper, Object... args) {
		ActiveDHTDBValue result = this;
		try {
			if (Scriptable.class.isInstance(jsObj)) {
				Object func = ((Scriptable) jsObj).get(callback, scope);
				if (Function.class.isInstance(func)) {
					scope.put("dht", scope, dhtWrapper);
					Object[] funcArgs = new Object[args.length + 1];
					funcArgs[0] = func;
					for (int i = 1; i < funcArgs.length; ++i) {
						funcArgs[i] = args[i - 1];
					}
					Object returnValue = ((Function) func).call(context, scope,
							scope, funcArgs);
					result = new JSActiveDHTDBValue(returnValue, creationTime,
							version, originator, sender, local, flags);
				}
			}
		} catch (RhinoException e) {
			e.printStackTrace();
			result = null;
		}
		
		synchronized (postActions) {
			while (!postActions.isEmpty()) {
				postActions.poll().run();
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

	// For testing
	protected void setDhtWrapper(DhtWrapper dhtWrapper) {
		this.dhtWrapper = dhtWrapper;
	}

	public DhtWrapper getDhtWrapper(DHTControl control, HashWrapper key) {
		if (dhtWrapper == null) {
			dhtWrapper = new DhtWrapper(control, key, this, neighbors,
					postActions);
		}
		return dhtWrapper;
	}

	public byte[] serialize(Object object) {
		if (NativeJavaObject.class.isInstance(object)) {
			object = ((NativeJavaObject) object).unwrap();
		}
		ByteArrayOutputStream os = new ByteArrayOutputStream();
		try {
			new ScriptableOutputStream(os, scope).writeObject(object);
			return os.toByteArray();
		} catch (IOException e) {
			e.printStackTrace();
			return DHTDBValue.ZERO_LENGTH_BYTE_ARRAY;
		}
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
				DHTDBValue.ZERO_LENGTH_BYTE_ARRAY, "", version, originator,
				local, flags);
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
