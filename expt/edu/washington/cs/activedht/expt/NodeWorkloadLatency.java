package edu.washington.cs.activedht.expt;

import java.io.InputStream;
import java.io.PrintStream;
import java.util.Scanner;
import java.util.concurrent.Semaphore;

import com.aelitis.azureus.core.dht.DHTOperationAdapter;

/**
 * @author levya
 * 
 */
public abstract class NodeWorkloadLatency {

	private static final int TRIALS = 1000;

	private final ActivePeer peer;
	private final int experimentTime;
	private final PrintStream out;

	private boolean keepRunning = false;

	private final Scanner in;

	private int i;
	
	private final int numObjects;

	public NodeWorkloadLatency(ActivePeer peer, int numObjects,
			int experimentTime, PrintStream out, InputStream in)
			throws Exception {
		this.peer = peer;
		this.experimentTime = experimentTime;
		this.numObjects = numObjects;
		this.out = out;
		this.in = new Scanner(in);
	}

	public abstract byte[] generateValue(double size);

	public void put(String key) {
		final Semaphore sema = new Semaphore(1);
		sema.acquireUninterruptibly();
		peer.put(key.getBytes(), generateValue(in.nextDouble()),
				new DHTOperationAdapter() {
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
					out.println(i + "," + 1.0 * elapsedTime / TRIALS);
				}
			}
		}.start();
	}

	public void run() throws InterruptedException {
		keepRunning = true;
		for (i = 0; i < numObjects && in.hasNextDouble(); ++i) {
			String key = "hello" + i;
			put(key);
		}
		for (int j = 0; j < i; ++j) {
			String key = "hello" + j;
			get(key);
		}
		
		Thread.sleep(5000);
		try {
			Thread.sleep(experimentTime);
			keepRunning = false;
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

}
