package edu.washington.cs.activedht.lua;

import java.util.Arrays;

import org.keplerproject.luajava.CPtr;
import org.keplerproject.luajava.LuaObject;
import org.keplerproject.luajava.LuaState;

/**
 * @author levya
 *
 */
public class Serializer {

	private static final int MAX_SIZE = 2048;
	private final LuaState luaState;
	
	public Serializer(LuaState luaState) {
		this.luaState = luaState;
	}

	private synchronized native int _serialize(CPtr L, byte[] buffer);
	private synchronized native int _deserialize(CPtr L, byte[] buffer);
	
	public byte[] serialize(LuaObject lObj) {
		LuaState luaState = lObj.getLuaState();
		byte[] buffer = new byte[MAX_SIZE];
		luaState.newTable();
		lObj.push();
		int size = _serialize(luaState.getCPtr(), buffer);
		luaState.pop(luaState.getTop());
		byte[] result = new byte[size];
		for (int i = 0; i < size; ++i) {
			result[i] = buffer[i];
		}
		return result;
	}

	public LuaObject deserialize(byte[] buffer) {
		_deserialize(luaState.getCPtr(), buffer);
		LuaObject result = luaState.getLuaObject(-1);
		luaState.pop(luaState.getTop());
		return result;
	}
	
}
