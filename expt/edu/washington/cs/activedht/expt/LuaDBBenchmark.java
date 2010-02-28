package edu.washington.cs.activedht.expt;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintStream;

import se.krka.kahlua.luaj.compiler.LuaCompiler;
import se.krka.kahlua.vm.LuaClosure;
import se.krka.kahlua.vm.LuaState;
import se.krka.kahlua.vm.serialize.Serializer;

import com.aelitis.azureus.core.dht.db.DHTDB;
import com.aelitis.azureus.core.dht.db.impl.DHTDBValueFactory.FactoryInterface;
import com.aelitis.azureus.plugins.dht.impl.DHTPluginStorageManager;

import edu.washington.cs.activedht.db.ActiveDB;
import edu.washington.cs.activedht.db.ActiveDHTInitializer;

/**
 * @author levya
 * 
 */
public class LuaDBBenchmark extends DBBenchmark {

	public LuaDBBenchmark(DHTDB db, String lua, int numCurRequests,
			int observations, int startupTime, int gap, PrintStream out)
			throws Exception {
		super(db, lua, numCurRequests, observations, startupTime, gap, out);
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
		int gap = 10000;
		FactoryInterface valueFactory = ActivePeer.KAHLUA_VALUE_FACTORY_INTERFACE;
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
			}
		}
		ActiveDHTInitializer.prepareRuntimeForActiveCode(valueFactory);
		DHTDB db = new ActiveDB(new DHTPluginStorageManager(0, null, new File("/tmp/activedht")));
		LuaDBBenchmark microbenchmark = new LuaDBBenchmark(db,
				"activeobject = {onGet = function(self) return \"hello\" end}",
				numObjects, observations, startupTime, gap, out);

		microbenchmark.run();

		out.close();
	}

}
