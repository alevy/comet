package se.krka.kahlua.vm.serialize;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import se.krka.kahlua.vm.LuaClosure;
import se.krka.kahlua.vm.LuaPrototype;
import se.krka.kahlua.vm.LuaTable;

public class Serializer {

	private final DataOutputStream stream;
	private final List<Object> serialized = new ArrayList<Object>(100);

	public Serializer(DataOutputStream stream, LuaTable env) {
		this.stream = stream;
		this.serialized.add(env);
	}

	public void serializeTable(LuaTable table) throws IOException {
		serialized.add(table);
		stream.writeByte(Type.TABLE);
		stream.writeInt(table.len());
		Object key = table.next(null);
		while (key != null) {
			serializeObject(key);
			Object value = table.rawget(key);
			serializeObject(value);
			key = table.next(key);
		}
		serializeObject(table.getMetatable());
	}

	public void serializeString(String value) throws IOException {
		stream.writeByte(Type.STRING);
		stream.writeInt(value.length());
		stream.writeChars(value);
	}

	public void serializeDouble(Double value) throws IOException {
		stream.writeByte(Type.DOUBLE);
		stream.writeDouble(value);
	}

	public void serializeBoolean(Boolean value) throws IOException {
		stream.writeByte(Type.BOOLEAN);
		stream.writeBoolean(value);
	}

	public void serializeClosure(LuaClosure value) throws IOException {
		serialized.add(value);
		stream.writeByte(Type.CLOSURE);
		value.prototype.dump(stream);
	}

	public void serializeNull() throws IOException {
		stream.writeByte(Type.NULL);
	}
	
	public void serializeReference(int index) throws IOException {
		stream.writeByte(Type.REFERENCE);
		stream.writeInt(index);
	}

	public void serializeObject(Object value) {
		try {
			int index = alreadySerialized(value);
			if (index >= 0) {
				serializeReference(index);
			} else if (String.class.isInstance(value)) {
				serializeString((String) value);
			} else if (Double.class.isInstance(value)) {
				serializeDouble((Double) value);
			} else if (Boolean.class.isInstance(value)) {
				serializeBoolean((Boolean) value);
			} else if (LuaTable.class.isInstance(value)) {
				serializeTable((LuaTable) value);
			} else if (LuaClosure.class.isInstance(value)) {
				serializeClosure((LuaClosure)value);
			} else if (LuaPrototype.class.isInstance(value)) {
				// This shouldn't happen
				// TODO(levya): Remove when sure
				throw new IllegalArgumentException();
			} else {
				serializeNull();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Checks if the value has already been serialized
	 * 
	 * @param value
	 * @return the index of the value in the list {@code serialized} or -1 if it
	 *         hasn't been serialized yet.
	 */
	private int alreadySerialized(Object value) {
		int i = 0;
		for (Object obj : serialized) {
			if (obj == value) {
				return i;
			}
			++i;
		}
		return -1;
	}

	public static byte[] serialize(Object value, LuaTable env) {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		new Serializer(new DataOutputStream(out), env).serializeObject(value);
		return out.toByteArray();
	}

}
