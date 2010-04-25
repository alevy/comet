package edu.washington.cs.activedht.apps;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Arrays;

import se.krka.kahlua.luaj.compiler.LuaCompiler;
import se.krka.kahlua.vm.LuaClosure;
import se.krka.kahlua.vm.LuaState;
import se.krka.kahlua.vm.LuaTable;
import se.krka.kahlua.vm.serialize.Deserializer;
import se.krka.kahlua.vm.serialize.Serializer;

public class LuaSerializedSize {

	public static enum OPCode {
		MOVE, LOADK, LOADBOOL, LOADNIL, GETUPVAL,
		GETGLOBAL, GETTABLE, SETGLOBAL, SETUPVAL, SETTABLE,
		NEWTABLE, SELF, ADD, SUB, MUL, DIV, MOD, POW,
		UNM, NOT, LEN, CONCAT, JMP, EQ, LT, LE,
		TEST, TESTSET, CALL, TAILCALL, RETURN, FORLOOP,
		FORPREP, TFORLOOP, SETLIST, CLOSE, CLOSURE, VARARG;
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {
		InputStream luaFile = new FileInputStream(new File("apps/lifetime_data.lua"));
		LuaState state = new LuaState();

		LuaClosure closure = LuaCompiler.loadis(luaFile, "stdin", state
				.getEnvironment());
		state.call(closure);

		LuaTable obj = (LuaTable)state.getEnvironment().rawget(
				"object");
		LuaClosure get = (LuaClosure)obj.rawget("onGet");
		printByteCode(get);
		//System.out.println(state.call(get));
		byte[] arr = Serializer.serialize(obj, state.getEnvironment());
		System.out.println(arr.length);
		System.out.println(Arrays.toString(arr));
		System.out.println(Deserializer.deserializeBytes(arr, state.getEnvironment()));
	}

	private static void printByteCode(LuaClosure closure) {
		int[] code = closure.prototype.code;
		OPCode[] values = OPCode.values();
		for (int op : code) {
			int opcode = op & 63;
			System.out.println(values[opcode] + ":" + ((op >>> 14) - 131071));
		}

	}
}
