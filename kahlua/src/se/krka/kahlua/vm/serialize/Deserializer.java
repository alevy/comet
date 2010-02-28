package se.krka.kahlua.vm.serialize;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;

import se.krka.kahlua.vm.LuaClosure;
import se.krka.kahlua.vm.LuaPrototype;
import se.krka.kahlua.vm.LuaTable;
import se.krka.kahlua.vm.LuaTableImpl;

public class Deserializer {

	private final DataInputStream stream;
	private final LuaTable env;

	public Deserializer(DataInputStream stream, LuaTable env) {
		this.stream = stream;
		this.env = env;
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
		for (int i = 0; i < length; ++i) {
			Object key = deserialize();
			Object value = deserialize();
			result.rawset(key, value);
		}
		return result;
	}

	public LuaClosure readPrototype() throws IOException {
		return LuaPrototype.loadByteCode(stream, env);
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
			case Type.PROTOTYPE:
				return readPrototype();
			case Type.NULL:
				return null;
			default: throw new IllegalStateException("Stream does not represent a Lua Object: " + type);
			}
		} catch (IOException e) {
			throw new IllegalStateException("Stream does not represent a Lua Object", e);
		}
	}

	public static Object deserializeBytes(byte[] value, LuaTable env) {
		return new Deserializer(new DataInputStream(new ByteArrayInputStream(value)), env).deserialize();
	}
}
