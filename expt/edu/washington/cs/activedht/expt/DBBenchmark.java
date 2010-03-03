package edu.washington.cs.activedht.expt;

import java.io.PrintStream;

import org.gudy.azureus2.core3.util.HashWrapper;

import com.aelitis.azureus.core.dht.db.DHTDB;
import com.aelitis.azureus.core.dht.transport.BasicDHTTransportValue;
import com.aelitis.azureus.core.dht.transport.DHTTransportValue;

/**
 * @author levya
 * 
 */
public abstract class DBBenchmark {

	private static final byte[] EMPTY_BYTE_ARRAY = new byte[] {};

	private final Object getsMon = new Object();
	
	private final DHTDB db;
	private final byte[] value;
	private final int numCurRequests;
	private final int observations;
	private final int startupTime;
	private final int gap;
	private final PrintStream out;

	private long gets = 0;
	private boolean keepRunning = false;

	public DBBenchmark(DHTDB db, String lua, int numCurRequests,
			int observations, int startupTime, int gap, PrintStream out)
			throws Exception {
		this.db = db;
		this.numCurRequests = numCurRequests;
		this.observations = observations;
		this.startupTime = startupTime;
		this.gap = gap;
		this.out = out;
		value = generateValue(lua);
	}

	public abstract byte[] generateValue(String lua) throws Exception;

	public void put(String key) {
		db.store(null, new HashWrapper(key.getBytes()),
				new DHTTransportValue[] { new BasicDHTTransportValue(System
						.currentTimeMillis(), value, "", 1, null, false, 0) });
	}

	public void get(final String key) {
		final HashWrapper hashKey = new HashWrapper(key.getBytes());
		final HashWrapper readerId = new HashWrapper("19876543".getBytes());
		new Thread() {
			public void run() {
				while (keepRunning) {
					db.get(hashKey, readerId, EMPTY_BYTE_ARRAY);
					synchronized (getsMon) {
						++gets;
					}
				}
			}
		}.start();
	}

	public void run() throws InterruptedException {
		keepRunning = true;
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
				long tmpGets;
				synchronized (getsMon) {
					tmpGets = gets;
					gets = 0;
				}
				out.println(numCurRequests + "," + tmpGets);
			}
			keepRunning = false;
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

}
