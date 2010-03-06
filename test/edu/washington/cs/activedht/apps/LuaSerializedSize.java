package edu.washington.cs.activedht.apps;

import java.io.InputStream;
import java.util.Arrays;

import se.krka.kahlua.luaj.compiler.LuaCompiler;
import se.krka.kahlua.vm.LuaClosure;
import se.krka.kahlua.vm.LuaState;
import se.krka.kahlua.vm.serialize.Serializer;


public class LuaSerializedSize {

	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {
		InputStream luaFile = LuaSerializedSize.class.getResourceAsStream("/delayed_get.lua");
		LuaState state = new LuaState();

		LuaClosure closure = LuaCompiler.loadis(luaFile, "stdin", state.getEnvironment());
		
		state.call(closure);		
		
		byte[] arr = Serializer.serialize(state.getEnvironment().rawget("object"), state.getEnvironment());
		System.out.println(arr.length);
		System.out.println(Arrays.toString(arr));
	}

}
