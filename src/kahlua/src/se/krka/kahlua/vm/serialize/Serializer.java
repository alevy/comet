package se.krka.kahlua.vm.serialize;

import java.io.DataOutputStream;
import java.io.IOException;

import se.krka.kahlua.vm.LuaClosure;
import se.krka.kahlua.vm.LuaPrototype;
import se.krka.kahlua.vm.LuaTable;

public class Serializer {

	private final DataOutputStream stream;

	public Serializer(DataOutputStream stream) {
		this.stream = stream;
	}

	public void serializeTable(LuaTable table) throws IOException {
		stream.writeByte(Type.TABLE);
		stream.writeInt(table.len());
		Object key = table.next(null);
		while (key != null) {
			serialize(key);
			Object value = table.rawget(key);
			serialize(value);
			key = table.next(key);
		}
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

	public void serializePrototype(LuaPrototype value) throws IOException {
		stream.writeByte(Type.PROTOTYPE);
		value.dump(stream);
	}

	public void serialize(Object value) {
		try {
			if (String.class.isInstance(value)) {
				serializeString((String) value);
			} else if (Double.class.isInstance(value)) {
				serializeDouble((Double) value);
			} else if (Boolean.class.isInstance(value)) {
				serializeBoolean((Boolean) value);
			} else if (LuaTable.class.isInstance(value)) {
				serializeTable((LuaTable) value);
			} else if (LuaClosure.class.isInstance(value)) {
				serializePrototype(((LuaClosure) value).prototype);
			} else if (LuaPrototype.class.isInstance(value)) {
				serializePrototype((LuaPrototype) value);
			} else {
				throw new IllegalArgumentException(value.getClass()
						+ " is not serializeable");
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
