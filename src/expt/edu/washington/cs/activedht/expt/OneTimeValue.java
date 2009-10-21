package edu.washington.cs.activedht.expt;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;
import java.util.concurrent.Semaphore;

import com.aelitis.azureus.core.dht.DHT;
import com.aelitis.azureus.core.dht.DHTOperationAdapter;
import com.aelitis.azureus.core.dht.transport.DHTTransportContact;
import com.aelitis.azureus.core.dht.transport.DHTTransportValue;

import edu.washington.cs.activedht.code.insecure.candefine.OneTimeActiveObject;
import edu.washington.cs.activedht.code.insecure.io.ClassObjectOutputStream;

public class OneTimeValue {

	private final ActivePeer peer;
	private final PrintStream out;

	public OneTimeValue(ActivePeer peer, PrintStream out) {
		this.peer = peer;
		this.out = out;
	}

	public void run(int gap, int iterations) throws InterruptedException{
		Thread.sleep(10000);
		
		final Semaphore semaphore = new Semaphore(iterations);
		semaphore.drainPermits();
		for (int i = 0; i < iterations; ++i) {

			byte[] key = ("hello" + i).getBytes();
			peer.put(key, getOneTimeValueBytes("world"),
					new DHTOperationAdapter() {
				@Override
				public void complete(boolean timeout) {
					semaphore.release();
					System.out.println("Released");
				}
			});
			Thread.sleep(500);
		}
		System.out.println("Waiting for puts to finish...");
		semaphore.acquire();

		for (int i = 0; i < iterations; ++i) {
			final int _i = i;
			byte[] key = ("hello" + i).getBytes();

			peer.get(key, new DHTOperationAdapter() {
				boolean found = false;

				public void read(DHTTransportContact contact,
						DHTTransportValue value) {
					found = true;
				}

				public void complete(boolean timeout) {
					out.println("Before-" + _i + ":" + found);
				}
			});

			Thread.sleep(gap);

			peer.get(key, new DHTOperationAdapter() {
						boolean found = false;

						public void read(DHTTransportContact contact,
								DHTTransportValue value) {
							found = true;
						}

						public void complete(boolean timeout) {
							out.println("After-" + _i + ": " + found);
						}
					});
		}
	}

	private byte[] getOneTimeValueBytes(String string) {
		byte[] value_bytes = null;
		try {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			ClassObjectOutputStream oos = new ClassObjectOutputStream(baos);
			oos.writeObject(new OneTimeActiveObject(string.getBytes()));
			value_bytes = baos.toByteArray();
			oos.close();
		} catch (Exception e) {
			e.printStackTrace();
		}

		return value_bytes;
	}

	public static void main(String[] args) throws Exception {
		ActivePeer peer = new ActivePeer(Integer.parseInt(args[0]), args[1]);
		peer.init();
		new OneTimeValue(peer, new PrintStream(new File("/tmp/activedht/onetime.out"))).run(1000, 40);
	}

}
