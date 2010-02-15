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

	private final AEMonitor getsMon = new AEMonitor("Gets");

	private final ActivePeer peer;
	private final byte[] value;
	private final int numCurRequests;
	private final int observations;
	private final int startupTime;
	private final int gap;
	private final PrintStream out;

	private int gets = 0;
	private boolean keepRunning = false;

	public Microbenchmark(ActivePeer peer, String lua, int numCurRequests, int observations, int startupTime, int gap, PrintStream out)
			throws Exception {
		this.peer = peer;
		this.numCurRequests = numCurRequests;
		this.observations = observations;
		this.startupTime = startupTime;
		this.gap = gap;
		this.out = out;
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
		peer.get(key.getBytes(), 0, new DHTOperationAdapter() {
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
				if (keepRunning) {
					get(key, sema);
				} else {
					sema.release();
				}
			}
		});
	}

	public void run() throws InterruptedException {
		Semaphore sema = new Semaphore(numCurRequests);
		keepRunning = true;
		for (int i = 0; i < numCurRequests; ++i) {
			String key = "hello" + i;
			put(key);
		}
		for (int j = 0; j < numCurRequests; ++j) {
			sema.acquire();
			String key = "hello" + j;
			get(key, sema);
		}
		new Thread(new Runnable() {
			public void run() {
				try {
					Thread.sleep(startupTime);
					getsMon.enter();
					gets = 0;
					getsMon.exit();
					for (int i = 0; i < observations; ++i) {
						Thread.sleep(gap);
						getsMon.enter();
						int tmpGets = gets;
						gets = 0;
						getsMon.exit();
						out.println(numCurRequests + "," + tmpGets);
					}
					keepRunning = false;
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}).start();
		sema.acquire(numCurRequests);
	}

}
