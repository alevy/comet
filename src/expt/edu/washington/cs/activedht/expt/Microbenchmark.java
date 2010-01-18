/**
 * 
 */
package edu.washington.cs.activedht.expt;

import java.io.File;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.concurrent.Semaphore;

import org.gudy.azureus2.core3.util.AEMonitor;
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
	private int gets = 0;
	private AEMonitor getsMon = new AEMonitor("Gets");

	private byte[] value;

	public Microbenchmark(ActivePeer peer, String lua) {
		this.peer = peer;
		luaState.LdoString(lua);
		value = new Serializer(luaState).serialize(luaState
				.getLuaObject("activeobject"));
	}

	public void put(String key) throws InterruptedException {
		final Semaphore sema = new Semaphore(1);
		sema.acquire();
		peer.put(key.getBytes(), value, new DHTOperationAdapter() {
			public void wrote(DHTTransportContact contact,
					DHTTransportValue value) {
				System.out.println(contact.getString());
			}
			public void complete(boolean t) {
				sema.release();
			}
		});
		sema.acquire();
	}

	public void get(final String key, final Semaphore sema) {
		try {
			sema.acquire();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		peer.get(key.getBytes(), new DHTOperationAdapter() {
			boolean read = false;

			public void read(DHTTransportContact contact,
					DHTTransportValue value) {
				read = true;
			}

			public void complete(boolean t) {
				if (read) {
					getsMon.enter();
					++gets;
					getsMon.exit();
				}
				sema.release();
				get(key, sema);
			}
		});
	}

	public static void main(String[] args) throws Exception {
		final int numCurRequests = 10;
		// File file = new File("/Users/levya/activedhtdata/bench_load_" +
		// numCurRequests + ".csv");
		// file.createNewFile();
		// final PrintStream out = System.out;//new PrintStream(file);

		ActivePeer bootstrap = new ActivePeer(48386,
				"marykate.cs.washington.edu:48386", false);
		bootstrap.init();
		Thread.sleep(5000);
		ActivePeer peer = new ActivePeer(1234,
				"marykate.cs.washington.edu:48386", false);
		peer.init();
		Thread.sleep(5000);
		final Microbenchmark microbenchmark = new Microbenchmark(peer,
				"activeobject = {onGet = \"hello\"}");
		for (int i = 0; i < numCurRequests; ++i) {
			String key = "hello" + i;
			microbenchmark.put(key);
		}
		final Semaphore sema = new Semaphore(numCurRequests);
		for (int j = 0; j < numCurRequests; ++j) {
			String key = "hello" + j;
			microbenchmark.get(key, sema);
		}

		new Thread(new Runnable() {
			public void run() {
				try {
					// Thread.sleep(5000);
					System.out.println("Benchmarking...");
					microbenchmark.getsMon.enter();
					microbenchmark.gets = 0;
					microbenchmark.getsMon.exit();
					for (int i = 0; i < 30; ++i) {
						Thread.sleep(1000);
						microbenchmark.getsMon.enter();
						System.out.println(numCurRequests + ","
								+ (numCurRequests - sema.availablePermits())
								+ "," + microbenchmark.gets);
						microbenchmark.gets = 0;
						microbenchmark.getsMon.exit();
					}
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}).start();
		sema.acquire(numCurRequests);

		peer.stop();
		bootstrap.stop();
		// out.close();
	}

}
