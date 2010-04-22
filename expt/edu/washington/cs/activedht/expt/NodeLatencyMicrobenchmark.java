package edu.washington.cs.activedht.expt;

import java.io.PrintStream;
import java.util.concurrent.Semaphore;

import com.aelitis.azureus.core.dht.DHTOperationAdapter;

/**
 * @author levya
 * 
 */
public abstract class NodeLatencyMicrobenchmark {

	private final ActivePeer peer;
	private final byte[] value;
	private final int numCurRequests;
	private final int experimentTime;
	private final PrintStream out;

	private boolean keepRunning = false;
	private static final int TRIALS = 1000;;

	public NodeLatencyMicrobenchmark(ActivePeer peer, String lua,
			int numCurRequests, int startupTime, PrintStream out)
			throws Exception {
		this.peer = peer;
		this.numCurRequests = numCurRequests;
		this.experimentTime = startupTime;
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
		new Thread() {
			public void run() {
				while (keepRunning) {
					long startTime = System.currentTimeMillis();
					for (int i = 0; i < TRIALS; ++i) {
						peer.getLocal(key.getBytes());
					}
					long elapsedTime = System.currentTimeMillis() - startTime;
					out.println(numCurRequests + "," + 1.0 * elapsedTime / TRIALS);
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
			Thread.sleep(experimentTime);
			keepRunning = false;
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

}
