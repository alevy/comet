import java.util.concurrent.Semaphore;

import org.gudy.azureus2.core3.util.SHA1Simple;

import se.krka.kahlua.vm.serialize.Serializer;

import com.aelitis.azureus.core.dht.DHTOperationAdapter;
import com.aelitis.azureus.core.dht.transport.DHTTransportContact;
import com.aelitis.azureus.core.dht.transport.DHTTransportReplyHandlerAdapter;
import com.aelitis.azureus.core.dht.transport.DHTTransportValue;

import edu.washington.cs.activedht.expt.ActivePeer;
import edu.washington.cs.activedht.transport.BasicDHTTransportValue;

/**
 * 
 */

/**
 * @author levya
 * 
 */
public class PutMany {

	static DHTTransportContact contact;

	public static byte[] encodeKey(byte[] key) {
		byte[] temp = new SHA1Simple().calculateHash(key);

		int keylen = 20;
		byte[] result = new byte[keylen];

		System.arraycopy(temp, 0, result, 0, keylen);

		return (result);
	}

	public static void main(String[] args) throws Exception {
		int numObjects = 10;
		int localPort = 1234;
		String localHostname = "localhost";
		String bootstrapLoc = "localhost:4321";

		for (int i = 0; i < args.length; ++i) {
			if (args[i].equals("-n")) {
				numObjects = Integer.parseInt(args[++i]);
			} else if (args[i].equals("-p")) {
				localPort = Integer.parseInt(args[++i]);
			} else if (args[i].equals("-h")) {
				localHostname = args[++i];
			} else if (args[i].equals("-b")) { // bootstrap host:port
				bootstrapLoc = args[++i];
			}
		}

		ActivePeer peer = new ActivePeer(localPort, bootstrapLoc, false,
				ActivePeer.NA_VALUE_FACTORY_INTERFACE, 20);
		peer.init(localHostname);
		final Semaphore sema = new Semaphore(0);
		peer.dht.getControl().lookup("hello".getBytes(), "", 0,
				new DHTOperationAdapter() {
					public void found(DHTTransportContact contact,
							boolean isClosest) {
						if (isClosest) {
							PutMany.contact = contact;
							sema.release();
						}
					}
				});
		sema.acquireUninterruptibly();
		byte[] value = Serializer.serialize("hello world", null);
		DHTTransportValue[][] values = { new DHTTransportValue[] { new BasicDHTTransportValue(
				System.currentTimeMillis(), value, "", 1, peer.dht.getControl()
						.getTransport().getLocalContact(), false, (byte) 0) } };

		for (int i = 1; i <= numObjects; ++i) {
			contact.sendStore(new DHTTransportReplyHandlerAdapter() {

				@Override
				public void failed(DHTTransportContact contact, Throwable error) {
					error.printStackTrace(System.err);
					sema.release();
				}

				@Override
				public void storeReply(DHTTransportContact contact,
						byte[] diversifications) {
					sema.release();
				}
			}, new byte[][] { encodeKey(("" + i).getBytes()) }, values, true);
			sema.acquireUninterruptibly(1);
			if (i % 100 == 0) {
				System.err.println("Finished putting " + i + " of "
						+ numObjects);
			}
		}
		System.err.println("Done.");
		peer.stop();

	}

}
