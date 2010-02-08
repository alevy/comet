package edu.washington.cs.activedht.expt;

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
		ActivePeer bootstrap = new ActivePeer(48386,
				"localhost:48386", false);
		bootstrap.init("localhost");
		Thread.sleep(5000);
		ActivePeer peer = new ActivePeer(1234,
				"localhost:48386", false);
		peer.init("localhost");
		Thread.sleep(5000);

		int numCurRequests = 10;
		LuaMicrobenchmark microbenchmark = new LuaMicrobenchmark(peer,
				"activeobject = {onGet = function(self) return \"hello\" end}", numCurRequests);

		Semaphore sema = new Semaphore(numCurRequests);
		microbenchmark.run(sema, 100, System.out);
		sema.acquire(numCurRequests);

		peer.stop();
		bootstrap.stop();
	}

}
