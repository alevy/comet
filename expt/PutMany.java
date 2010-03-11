import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.concurrent.Semaphore;

import com.aelitis.azureus.core.dht.DHTOperationAdapter;

import edu.washington.cs.activedht.expt.ActivePeer;

/**
 * 
 */

/**
 * @author levya
 *
 */
public class PutMany {

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
			} else if (args[i].equals("-b")){ // bootstrap host:port
				bootstrapLoc = args[++i];
			}
		}
		
		ActivePeer peer = new ActivePeer(localPort, bootstrapLoc);
		peer.init(localHostname);
		byte[] value = "hello world".getBytes();
		final Semaphore sema = new Semaphore(0);
		for (int i = 0; i < numObjects; ++i) {
			peer.dht.put(("" + i).getBytes(), "", value, (byte)0, new DHTOperationAdapter() {
				public void complete(boolean timeout) {
					sema.release();
				}
			});
		}
		sema.acquireUninterruptibly(numObjects);
		peer.stop();

	}

}
