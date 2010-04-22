package edu.washington.cs.activedht.expt;

import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.logging.Level;

import edu.washington.cs.activedht.db.DHTDBValueFactory;

import se.krka.kahlua.luaj.compiler.LuaCompiler;
import se.krka.kahlua.vm.LuaClosure;
import se.krka.kahlua.vm.LuaState;
import se.krka.kahlua.vm.serialize.Serializer;

/**
 * @author levya
 * 
 */
public class LuaNodeLatencyMicrobenchmark extends NodeLatencyMicrobenchmark {

	public LuaNodeLatencyMicrobenchmark(ActivePeer peer, String lua,
			int numCurRequests, int experimentTime, PrintStream out)
			throws Exception {
		super(peer, lua, numCurRequests, experimentTime, out);
	}

	public byte[] generateValue(String lua) throws Exception {
		LuaState state = new LuaState();
		LuaClosure closure = LuaCompiler.loadstring(lua, "stdin", state
				.getEnvironment());
		state.call(closure, new Object[] {});
		Object obj = state.getEnvironment().rawget("activeobject");
		return Serializer.serialize(obj, state.getEnvironment());
	}

	public static void main(String[] args) throws Exception {
		int numObjects = 100;
		int experimentTime = 50000;
		int localPort = 2134;
		String localHostname = "localhost";
		String bootstrapLoc = "localhost:4321";
		DHTDBValueFactory valueFactory = ActivePeer.KAHLUA_VALUE_FACTORY_INTERFACE;
		PrintStream out = System.out;

		for (int i = 0; i < args.length; ++i) {
			if (args[i].equals("-n")) {
				numObjects = Integer.parseInt(args[++i]);
			} else if (args[i].equals("-o")) {
				out = new PrintStream(new FileOutputStream(args[++i], true));
			} else if (args[i].equals("-v")) {
				valueFactory = ActivePeer.ValueFactory.valueOf(args[++i]).fi;
			} else if (args[i].equals("-w")) {
				experimentTime = Integer.parseInt(args[++i]);
			} else if (args[i].equals("-p")) {
				localPort = Integer.parseInt(args[++i]);
			} else if (args[i].equals("-h")) {
				localHostname = args[++i];
			} else if (args[i].equals("-b")) { // bootstrap host:port
				bootstrapLoc = args[++i];
			}
		}
		ActivePeer boot = new ActivePeer(4321, bootstrapLoc, Level.OFF,
				valueFactory, 200);
		ActivePeer peer = new ActivePeer(localPort, bootstrapLoc, Level.OFF,
				valueFactory, 200);
		LuaNodeLatencyMicrobenchmark microbenchmark = new LuaNodeLatencyMicrobenchmark(
				peer,
				"activeobject = {onGet = function(self) return \"hello\" end}",
				numObjects, experimentTime, out);
		boot.init(localHostname);
		peer.init(localHostname);
		Thread.sleep(5000);

		microbenchmark.run();

		peer.stop();
		boot.stop();
		out.close();
	}

}
