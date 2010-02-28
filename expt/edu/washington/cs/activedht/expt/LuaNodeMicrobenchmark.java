package edu.washington.cs.activedht.expt;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.io.PrintStream;

import se.krka.kahlua.luaj.compiler.LuaCompiler;
import se.krka.kahlua.vm.LuaClosure;
import se.krka.kahlua.vm.LuaState;
import se.krka.kahlua.vm.serialize.Serializer;

import com.aelitis.azureus.core.dht.db.impl.DHTDBValueFactory.FactoryInterface;

/**
 * @author levya
 * 
 */
public class LuaNodeMicrobenchmark extends NodeMicrobenchmark {

	public LuaNodeMicrobenchmark(ActivePeer peer, String lua, int numCurRequests,
			int observations, int startupTime, int gap, PrintStream out)
			throws Exception {
		super(peer, lua, numCurRequests, observations, startupTime, gap, out);
	}

	public byte[] generateValue(String lua) throws Exception {
		LuaState state = new LuaState();
		LuaClosure closure = LuaCompiler
				.loadstring(
						lua,
						"stdin", state.getEnvironment());
		state.call(closure, new Object[] {});
		Object obj = state.getEnvironment().rawget("activeobject");
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		new Serializer(new DataOutputStream(bos)).serializeObject(obj);
		return bos.toByteArray();
	}

	public static void main(String[] args) throws Exception {
		int numObjects = 10;
		int observations = 100;
		int startupTime = 5000;
		int gap = 1000;
		int localPort = 1234;
		String localHostname = "localhost";
		String bootstrapLoc = "localhost:4321";
		FactoryInterface valueFactory = ActivePeer.NA_VALUE_FACTORY_INTERFACE;
		PrintStream out = System.out;

		for (int i = 0; i < args.length; ++i) {
			if (args[i].equals("-n")) {
				numObjects = Integer.parseInt(args[++i]);
			} else if (args[i].equals("-i")) {
				observations = Integer.parseInt(args[++i]);
			} else if (args[i].equals("-o")) {
				out = new PrintStream(new FileOutputStream(args[++i], true));
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
			}
		}
		ActivePeer peer = new ActivePeer(localPort, bootstrapLoc, false,
				valueFactory, 200);
		LuaNodeMicrobenchmark microbenchmark = new LuaNodeMicrobenchmark(peer,
				"activeobject = {onGet = function(self) return \"hello\" end}",
				numObjects, observations, startupTime, gap, out);
		peer.init(localHostname);
		Thread.sleep(5000);

		microbenchmark.run();

		peer.stop();
		out.close();
	}

}
