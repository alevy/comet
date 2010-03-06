package se.krka.kahlua.vm.serialize;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import se.krka.kahlua.vm.LuaClosure;
import se.krka.kahlua.vm.LuaPrototype;
import se.krka.kahlua.vm.LuaTable;
import se.krka.kahlua.vm.LuaTableImpl;

public class Deserializer {

	private final DataInputStream stream;
	private final List<Object> deserialized = new ArrayList<Object>();
	private LuaTable env; 

	public Deserializer(DataInputStream stream, LuaTable globalEnv) {
		this.stream = stream;
		this.env = globalEnv;
		this.deserialized.add(globalEnv);
	}

	public String readString() throws IOException {
		StringBuilder sb = new StringBuilder();
		int length = stream.readInt();
		for (int i = 0; i < length; ++i) {
			sb.append(stream.readChar());
		}
		return sb.toString();
	}

	public LuaTable readTable() throws IOException {
		int length = stream.readInt();
		LuaTable result = new LuaTableImpl(length);
		deserialized.add(result);
		for (int i = 0; i < length; ++i) {
			Object key = deserialize();
			Object value = deserialize();
			result.rawset(key, value);
		}
		result.setMetatable((LuaTable)deserialize());
		return result;
	}

	public LuaClosure readClosure() throws IOException {
		LuaClosure result = LuaPrototype.loadByteCode(stream, env);
		deserialized.add(result);
		return result;
	}
	
	public Object readReference() throws IOException {
		int index = stream.readInt();
		return deserialized.get(index);
	}

	public Object deserialize() {
		byte type;
		try {
			type = stream.readByte();
			switch (type) {
			case Type.BOOLEAN:
				return stream.readBoolean();
			case Type.DOUBLE:
				return stream.readDouble();
			case Type.STRING:
				return readString();
			case Type.TABLE:
				return readTable();
			case Type.CLOSURE:
				return readClosure();
			case Type.NULL:
				return null;
			case Type.REFERENCE:
				return readReference();
			default: throw new IllegalStateException("Stream does not represent a Lua Object: " + type);
			}
		} catch (IOException e) {
			throw new IllegalStateException("Stream does not represent a Lua Object", e);
		}
	}

	public static Object deserializeBytes(byte[] value, LuaTable env) {
		if (value.length == 0) {
			return null;
		}
		return new Deserializer(new DataInputStream(new ByteArrayInputStream(value)), env).deserialize();
	}
}
