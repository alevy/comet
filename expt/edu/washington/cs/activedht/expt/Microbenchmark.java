package edu.washington.cs.activedht.expt;

import java.io.PrintStream;
import java.util.concurrent.Semaphore;

import com.aelitis.azureus.core.dht.DHTOperationAdapter;
import com.aelitis.azureus.core.dht.transport.DHTTransportContact;
import com.aelitis.azureus.core.dht.transport.DHTTransportValue;

/**
 * @author levya
 * 
 */
public abstract class Microbenchmark {

	private final Object getsMon = new Object();

	private final ActivePeer peer;
	private final byte[] value;
	private final int numCurRequests;
	private final int observations;
	private final int startupTime;
	private final int gap;
	private final PrintStream out;

	private int gets = 0;
	private int timedout = 0;
	private boolean keepRunning = false;

	public Microbenchmark(ActivePeer peer, String lua, int numCurRequests,
			int observations, int startupTime, int gap, PrintStream out)
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

	public void put(String key) {
		final Semaphore sema = new Semaphore(1);
		sema.acquireUninterruptibly();
		peer.put(key.getBytes(), value, new DHTOperationAdapter() {
			public void complete(boolean t) {
				sema.release();
			}
		});
		sema.acquireUninterruptibly();
	}

	public void get(final String key) {
		final Semaphore sema = new Semaphore(0);
		final DHTOperationAdapter getAdapter = new DHTOperationAdapter() {
			boolean read = false;
			
			public void read(DHTTransportContact contact,
					DHTTransportValue value) {
				read = true;
			}
			
			public void complete(boolean t) {
				if (read) {
					synchronized (getsMon) {
						++gets;
					}
				} else {
					++timedout;
				}
				sema.release();
			}
		};
		
		new Thread() {
			public void run() {
				while (keepRunning) {
					peer.get(key.getBytes(), 0, getAdapter);
					sema.acquireUninterruptibly();
				}
			}
		}.start();
	}

	public void run() throws InterruptedException {
		keepRunning = true;
		Thread.sleep(5000);
		for (int i = 0; i < numCurRequests; ++i) {
			String key = "hello" + i;
			put(key);
		}
		for (int j = 0; j < numCurRequests; ++j) {
			String key = "hello" + j;
			get(key);
		}
		try {
			Thread.sleep(startupTime);
			synchronized (getsMon) {
				gets = 0;
			}
			for (int i = 0; i < observations; ++i) {
				Thread.sleep(gap);
				int tmpGets;
				synchronized (getsMon) {
					tmpGets = gets;
					gets = 0;
				}
				out.println(numCurRequests + "," + tmpGets + "," + timedout);
			}
			keepRunning = false;
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

}
