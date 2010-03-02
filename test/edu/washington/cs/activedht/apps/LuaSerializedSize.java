package edu.washington.cs.activedht.apps;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


import se.krka.kahlua.luaj.compiler.LuaCompiler;
import se.krka.kahlua.vm.LuaClosure;
import se.krka.kahlua.vm.LuaState;
import se.krka.kahlua.vm.LuaTable;
import se.krka.kahlua.vm.LuaTableImpl;
import se.krka.kahlua.vm.serialize.Serializer;


public class LuaSerializedSize {

	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {
		File luaFile = new File("apps/replication.lua");
		byte[] lua = new byte[(int)luaFile.length()];
		new FileInputStream(luaFile).read(lua);
		LuaState state = new LuaState();
		List<Object> excludes = new ArrayList<Object>();
		Object key = state.getEnvironment().next(null);
		while (key != null) {
			excludes.add(key);
			key = state.getEnvironment().next(key);
		}

		LuaClosure closure = LuaCompiler.loadstring(new String(lua), "stdin", state.getEnvironment());
		
		state.call(closure, new Object[]{});
		
		LuaTable table = new LuaTableImpl();
		key = state.getEnvironment().next(null);
		while (key != null) {
			if (!excludes.contains(key)) {
				table.rawset(key, state.getEnvironment().rawget(key));
			}
			key = state.getEnvironment().next(key);
		}
		
		byte[] arr = Serializer.serialize(table);
		System.out.println(arr.length);
		System.out.println(Arrays.toString(arr));
	}

}
