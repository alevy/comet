package edu.washington.cs.activedht.expt;

import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.concurrent.Semaphore;

import org.keplerproject.luajava.LuaState;
import org.keplerproject.luajava.LuaStateFactory;

import edu.washington.cs.activedht.lua.Serializer;

/**
 * @author levya
 * 
 */
public class LuaMicrobenchmark extends Microbenchmark {

	public LuaMicrobenchmark(ActivePeer peer, String lua, int numCurRequests)
			throws Exception {
		super(peer, lua, numCurRequests);
	}

	public byte[] generateValue(String lua) throws Exception {
		LuaState luaState = LuaStateFactory.newLuaState();
		luaState.LdoString(lua);
		return new Serializer(luaState).serialize(luaState
				.getLuaObject("activeobject"));
	}

	public static void main(String[] args) throws Exception {
		int numObjects = 100;
		PrintStream out = System.out;
		
		if (args.length > 0) {
			numObjects = Integer.parseInt(args[0]);
		}
		if (args.length > 1) {
			out = new PrintStream(new FileOutputStream(args[1], true));
		}

		ActivePeer bootstrap = new ActivePeer(48386, "localhost:48386", false);
		ActivePeer peer = new ActivePeer(1234, "localhost:48386", false);
		LuaMicrobenchmark microbenchmark = new LuaMicrobenchmark(peer,
				"activeobject = {onGet = function(self) return \"hello\" end}", numObjects);
		
		bootstrap.init("localhost");
		Thread.sleep(5000);
		peer.init("localhost");
		Thread.sleep(5000);

		Semaphore sema = new Semaphore(numObjects);
		microbenchmark.run(sema, 100, out);
		sema.acquire(numObjects);

		peer.stop();
		bootstrap.stop();
		out.close();
	}

}
