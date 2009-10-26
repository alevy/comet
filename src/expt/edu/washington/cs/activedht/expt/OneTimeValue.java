package edu.washington.cs.activedht.expt;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;
import java.util.concurrent.Semaphore;

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

	public int[] run(int gap, int iterations) throws InterruptedException {

		final int data[] = new int[iterations];

		String baseKey = "" + System.currentTimeMillis();
		Thread.sleep(60000);

		final Semaphore semaphore = new Semaphore(iterations);
		semaphore.drainPermits();
		for (int i = 0; i < iterations; ++i) {

			byte[] key = (baseKey + i).getBytes();
			peer.put(key, getOneTimeValueBytes("world"),
					new DHTOperationAdapter() {
						@Override
						public void complete(boolean timeout) {
							semaphore.release();
							System.out.println("Released");
						}
					});
			Thread.sleep(0);
		}
		System.out.println("Waiting for puts to finish...");
		semaphore.acquire(iterations);
		semaphore.release(iterations * 2);
		System.out.println(semaphore.availablePermits());
		
		
		for (int i = 0; i < iterations; ++i) {
			final int _i = i;
			byte[] key = (baseKey + i).getBytes();
			semaphore.acquire();
			peer.get(key, new DHTOperationAdapter() {
				boolean found = false;
				public void read(DHTTransportContact contact,
						DHTTransportValue value) {
					found = true;
					out.println(contact.getAddress().getHostName());
				}
				public void complete(boolean timeout) {
					semaphore.release();
					if (found) data[_i]++;
				}
			});

			Thread.sleep(gap);
			semaphore.acquire();
			peer.get(key, new DHTOperationAdapter() {
				boolean found = false;
				public void read(DHTTransportContact contact,
						DHTTransportValue value) {
					found = true;
					out.println(contact.getAddress().getHostName());
				}
				public void complete(boolean timeout) {
					semaphore.release();
					if (found) data[_i]++;
				}
			});
		}
		semaphore.acquire(iterations * 2);
		return data;
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
		ActivePeer peer = new ActivePeer(Integer.parseInt(args[0]), args[1], true);
		peer.init();
		int data[] = new OneTimeValue(peer, new PrintStream(new File(
				"/tmp/activedht/onetime.out"))).run(10000, 50);
		PrintStream out = new PrintStream(new File(
				"/tmp/activedht/onetime.results"));
		for (int d : data) {
			out.println(d);
		}
		peer.stop();
		System.out.println("DONE");
	}

}
