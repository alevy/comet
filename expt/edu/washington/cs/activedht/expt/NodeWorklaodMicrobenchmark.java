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
public abstract class NodeWorklaodMicrobenchmark {

	private final Object getsMon = new Object();

	private final ActivePeer peer;
	private final int observations;
	private final int startupTime;
	private final int gap;
	private final PrintStream out;

	private int gets = 0;
	private int timedout = 0;
	private boolean keepRunning = false;

	private final Scanner in;

	private final int numObjects;

	public NodeWorklaodMicrobenchmark(ActivePeer peer, int numObjects, int observations,
			int startupTime, int gap, PrintStream out, InputStream in)
			throws Exception {
		this.peer = peer;
		this.observations = observations;
		this.startupTime = startupTime;
		this.gap = gap;
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
					peer.getLocal(key.getBytes());
					synchronized (getsMon) {
						++gets;
					}
				}
			}
		}.start();
	}

	public void run() throws InterruptedException {
		keepRunning = true;
		int i;
		for (i = 0; i < numObjects && in.hasNextDouble(); ++i) {
			String key = "hello" + i;
			put(key);
			System.out.println(i + " - " + numObjects);
		}
		for (int j = 0; j < i; ++j) {
			String key = "hello" + j;
			get(key);
		}
		try {
			Thread.sleep(startupTime);
			synchronized (getsMon) {
				gets = 0;
			}
			for (int k = 0; k < observations; ++k) {
				Thread.sleep(gap);
				int tmpGets;
				synchronized (getsMon) {
					tmpGets = gets;
					gets = 0;
				}
				out.println(i + "," + 1000.0 * tmpGets / gap + "," + timedout);
			}
			keepRunning = false;
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

}
