/**
 * 
 */
package edu.washington.cs.activedht.expt;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Semaphore;

import org.keplerproject.luajava.LuaState;
import org.keplerproject.luajava.LuaStateFactory;

import com.aelitis.azureus.core.dht.DHTOperationAdapter;
import com.aelitis.azureus.core.dht.transport.DHTTransportContact;
import com.aelitis.azureus.core.dht.transport.DHTTransportValue;

import edu.washington.cs.activedht.lua.Serializer;

/**
 * @author levya
 * 
 */
public class Microbenchmark {

	private final ActivePeer peer;
	private static final LuaState luaState = LuaStateFactory.newLuaState();

	public Microbenchmark(ActivePeer peer) {
		this.peer = peer;
	}

	public void put(String key, String lua) throws InterruptedException {
		luaState.LdoString(lua);
		byte[] value = new Serializer(luaState).serialize(luaState
				.getLuaObject("activeobject"));

		final Semaphore sema = new Semaphore(1);
		sema.acquire();
		final List<DHTTransportContact> contacts = new LinkedList<DHTTransportContact>();
		peer.put(key.getBytes(), value, new DHTOperationAdapter() {
			public void wrote(DHTTransportContact contact,
					DHTTransportValue value) {
				contacts.add(contact);
			}

			@Override
			public void complete(boolean t) {
				sema.release();
			}
		});
		sema.acquire();
	}

	public void get(String key, final Semaphore sema)
			throws InterruptedException {
		sema.acquire();
		peer.get(key.getBytes(), new DHTOperationAdapter() {
			public void complete(boolean t) {
				sema.release();
			}
		});
	}

	public static void main(String[] args) throws Exception {
		int numObjects = 100;

		ActivePeer bootstrap = new ActivePeer(48386,
				"marykate.cs.washington.edu:48386", false);
		bootstrap.init();
		Thread.sleep(5000);
		ActivePeer peer = new ActivePeer(1234,
				"marykate.cs.washington.edu:48386", false);
		peer.init();
		Thread.sleep(5000);
		Microbenchmark microbenchmark = new Microbenchmark(peer);
		for (int i = 0; i < numObjects; ++i) {
			String key = "" + i;
			microbenchmark.put(key,
					"activeobject = { onGet = function(self) return \"" + key
							+ "\" end }");
		}

		Semaphore sema = new Semaphore(numObjects);
		long start = System.currentTimeMillis();
		for (int j = 0; j < numObjects; ++j) {
			String key = "" + j;
			microbenchmark.get(key, sema);
		}
		sema.acquire(numObjects);
		long elapsed = System.currentTimeMillis() - start;
		System.out.println(1.0 * elapsed / numObjects);

		peer.stop();
		bootstrap.stop();
	}

}
