package edu.washington.cs.activedht.expt;

import java.io.PrintStream;
import java.util.concurrent.Semaphore;

import org.gudy.azureus2.core3.util.AEMonitor;

import com.aelitis.azureus.core.dht.DHTOperationAdapter;
import com.aelitis.azureus.core.dht.transport.DHTTransportContact;
import com.aelitis.azureus.core.dht.transport.DHTTransportValue;

/**
 * @author levya
 * 
 */
public abstract class Microbenchmark {

	private final ActivePeer peer;
	private int gets = 0;
	private AEMonitor getsMon = new AEMonitor("Gets");
	private boolean keepRunning = false;

	private byte[] value;
	private int numCurRequests;

	public Microbenchmark(ActivePeer peer, String lua, int numCurRequests)
			throws Exception {
		this.peer = peer;
		this.numCurRequests = numCurRequests;
		value = generateValue(lua);
	}

	public abstract byte[] generateValue(String lua) throws Exception;

	public void put(String key) throws InterruptedException {
		final Semaphore sema = new Semaphore(1);
		sema.acquire();
		peer.put(key.getBytes(), value, new DHTOperationAdapter() {
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
				if (keepRunning) {
					get(key, sema);
				}
			}
		});
	}

	public void run(final Semaphore sema, final int observations,
			final PrintStream out) throws InterruptedException {
		keepRunning = true;
		for (int i = 0; i < numCurRequests; ++i) {
			String key = "hello" + i;
			put(key);
		}
		for (int j = 0; j < numCurRequests; ++j) {
			String key = "hello" + j;
			get(key, sema);
		}
		new Thread(new Runnable() {
			public void run() {
				try {
					Thread.sleep(600000);
					getsMon.enter();
					gets = 0;
					getsMon.exit();
					for (int i = 0; i < observations; ++i) {
						Thread.sleep(300000);
						getsMon.enter();
						out.println(numCurRequests + "," + gets);
						gets = 0;
						getsMon.exit();
					}
					keepRunning = false;
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}).start();
	}

}
