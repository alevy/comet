package edu.washington.cs.activedht.expt;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
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
public class LuaNodeWorkload extends NodeWorklaodMicrobenchmark {


	public LuaNodeWorkload(ActivePeer peer, int numObjects,
			int observations, int startupTime, int gap, PrintStream out,
			InputStream in) throws Exception {
		super(peer, numObjects, observations, startupTime, gap, out, in);
	}

	@Override
	public byte[] generateValue(double size) {
		int num = (int)size;
		String lua = "activeobject = {onGet = function() local p = 0; for i = 1," + num + " do p = i end; return p end}";
		LuaState state = new LuaState();
		LuaClosure closure;
		try {
			closure = LuaCompiler.loadstring(lua, "stdin", state
					.getEnvironment());
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		state.call(closure, new Object[] {});
		Object obj = state.getEnvironment().rawget("activeobject");
		return Serializer.serialize(obj, state.getEnvironment());
	}
	
	public static void main(String[] args) throws Exception {
		int observations = 100;
		int startupTime = 5000;
		int numObjects = 1;
		int gap = 1000;
		int localPort = 1234;
		String localHostname = "localhost";
		String bootstrapLoc = "localhost:4321";
		DHTDBValueFactory valueFactory = ActivePeer.NA_VALUE_FACTORY_INTERFACE;
		PrintStream out = System.out;
		InputStream in = System.in;

		for (int i = 0; i < args.length; ++i) {
			if (args[i].equals("-i")) {
				observations = Integer.parseInt(args[++i]);
			} else if (args[i].equals("-o")) {
				out = new PrintStream(new FileOutputStream(args[++i], true));
			} else if (args[i].equals("-n")) {
				numObjects = Integer.parseInt(args[++i]);
			} else if (args[i].equals("-v")) {
				valueFactory = ActivePeer.ValueFactory.valueOf(args[++i]).fi;
			} else if (args[i].equals("-w")) {
				startupTime = Integer.parseInt(args[++i]);
			} else if (args[i].equals("-g")) {
				gap = Integer.parseInt(args[++i]);
			} else if (args[i].equals("-p")) {
				localPort = Integer.parseInt(args[++i]);
			} else if (args[i].equals("-h")) {
				localHostname = args[++i];
			} else if (args[i].equals("-b")){ // bootstrap host:port
				bootstrapLoc = args[++i];
			} else if (args[i].equals("-f")) {
				in = new FileInputStream(args[++i]);
			}
		}
		ActivePeer peer = new ActivePeer(localPort, bootstrapLoc, Level.OFF,
				valueFactory, 200);
		LuaNodeWorkload microbenchmark = new LuaNodeWorkload(peer, numObjects,
				observations, startupTime, gap, out, in);
		peer.init(localHostname);

		microbenchmark.run();

		peer.stop();
		out.close();
	}

}
