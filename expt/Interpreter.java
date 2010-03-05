import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import se.krka.kahlua.luaj.compiler.LuaCompiler;
import se.krka.kahlua.stdlib.BaseLib;
import se.krka.kahlua.vm.LuaClosure;
import se.krka.kahlua.vm.LuaState;

public class Interpreter {
	public static void main(String[] args) throws Exception {
		BufferedReader in = new BufferedReader(new InputStreamReader(System.in));

		LuaState state = new LuaState();
		LuaCompiler.register(state);
		while (true) {
			System.out.print(">> ");
			System.out.flush();
			LuaClosure closure = getClosure(state, in.readLine(), in);
			if (closure != null) {
				Object[] result = state.pcall(closure);
				if (result[0] == Boolean.TRUE) {
					printResults(result, state);
				} else {
					System.out.println(result[1].toString());
					System.out.println(result[2].toString());
				}
			}
		}
	}

	private static void printResults(Object[] result, LuaState state) {
		for (int i = 1; i < result.length; i++) {
			if (i > 1) {
				System.out.print("\t");
			}
			System.out.print(BaseLib.tostring(result[i], state));
		}
		System.out.println();
	}

	private static LuaClosure getClosure(LuaState state, String line,
			BufferedReader in) throws IOException {
		if (line == null) {
			System.out.println();
			System.exit(0);
			return null;
		}
		if (line.length() == 0) {
			return null;
		}
		if (line.charAt(0) == '=') {
			line = "return " + line.substring(1);
		}
		try {
			return LuaCompiler
					.loadstring(line, "stdin", state.getEnvironment());
		} catch (IOException e) {
			System.out.println(e.getMessage());
			return null;
		} catch (RuntimeException e) {
			if (e.getMessage().contains("<eof>")) {
				System.out.print(">> ");
				System.out.flush();
				return getClosure(state, line + "\n" + in.readLine(), in);
			}
			System.out.println(e.getMessage());
			System.out.flush();
			return null;
		}
	}
}
