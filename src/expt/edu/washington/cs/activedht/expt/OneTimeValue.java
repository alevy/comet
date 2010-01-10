package edu.washington.cs.activedht.expt;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;
import java.util.concurrent.Semaphore;

import org.gudy.azureus2.core3.util.Timer;
import org.gudy.azureus2.core3.util.TimerEvent;
import org.gudy.azureus2.core3.util.TimerEventPerformer;

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
		final Semaphore semas[] = new Semaphore[iterations];
		for (int i = 0; i < iterations; ++i) {
			final int _i = i;
			final byte[] key = (baseKey + i).getBytes();
			semas[i] = new Semaphore(1);
			semas[i].acquire();

			peer.put(key, "0".getBytes(), new DHTOperationAdapter() {
				@Override
				public void wrote(DHTTransportContact contact,
						DHTTransportValue value) {
					System.out.println(contact + " - " + value.getString());
				}
				
				@Override
				public void found(DHTTransportContact contact) {
					System.out.println(contact.getString());
				}
				
				@Override
				public void complete(boolean timeout) {
					semas[_i].release();
				}
			});

			semas[_i].acquire();
			long eventTime = System.currentTimeMillis() + 30000;//00;
			Timer t = new Timer(new String(key));
		/*	t.addEvent(eventTime, new TimerEventPerformer() {
				public void perform(TimerEvent event) {
					peer.put(key, "1".getBytes(), new DHTOperationAdapter() {
						@Override
						public void complete(boolean timeout) {
							System.out.println(new String(key));
						}
					});
				}
			});*/
			t.addEvent(eventTime + gap, new TimerEventPerformer() {
				public void perform(TimerEvent event) {
					peer.get(key, new DHTOperationAdapter() {
						boolean found = false;

						public void read(DHTTransportContact contact,
								DHTTransportValue value) {
							found = true;
							System.out.println(value.getString());
							out.println(contact.getAddress().getHostName());
						}

						public void complete(boolean timeout) {
							System.out.println(found);
							if (found)
								data[_i]++;
							semas[_i].release();
						}
					});
				}
			});
		}

		for (Semaphore s : semas) {
			s.acquire();
		}
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
		ActivePeer peer = new ActivePeer(Integer.parseInt(args[0]), "213.186.46.164:6881",
				false);
		peer.init();
		Thread.sleep(120000);
		for (int i = 0; i < 20; ++i) {
			int data[] = new OneTimeValue(peer, new PrintStream(new File(
					"/tmp/activedht/onetime.out.320" + i * 1000 + ".100")))
					.run(i * 1000, 10);
			PrintStream out = new PrintStream(new File(
					"/tmp/activedht/onetime.results.320" + i * 1000 + ".100"));
			for (int d : data) {
				out.println(d);
			}
			System.out.println("DONE");
		}
		peer.stop();
		System.exit(0);
	}

}
