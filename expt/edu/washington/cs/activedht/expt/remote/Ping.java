/**
 * 
 */
package edu.washington.cs.activedht.expt.remote;

import java.util.concurrent.Semaphore;

import com.aelitis.azureus.core.dht.transport.DHTTransportContact;
import com.aelitis.azureus.core.dht.transport.DHTTransportReplyHandlerAdapter;

/**
 * @author levya
 * 
 */
public class Ping extends RemoteNodeAction {

	private static class Counter {
		private int count = 0;
	}

	private static int maxelapsed;

	public Ping(String[] args) throws Exception {
		super(args);
	}

	protected void run(final Semaphore sema, final Counter counter)
			throws Exception {
		contact.sendPing(new DHTTransportReplyHandlerAdapter() {

			@Override
			public void pingReply(DHTTransportContact contact) {
				System.out.println(contact.getString());
				sema.release();
			}

			@Override
			public void pingReply(DHTTransportContact contact, int elapsed) {
				synchronized (counter) {
					maxelapsed = Math.max(elapsed, elapsed);
					++counter.count;
				}
				sema.release();
			}

			public void failed(DHTTransportContact contact, Throwable error) {
				error.printStackTrace();
				sema.release();
			}
		});
	}

	public static void main(String[] args) throws Exception {
		Semaphore sema = new Semaphore(0);
		Counter counter = new Counter();
		for (String arg : args) {
			try {
			new Ping(new String[] { arg }).run(sema, counter);
			} catch (Exception e ){
				sema.release();
				e.printStackTrace();
			}
		}
		sema.acquire(args.length);
		System.out.println("FINISHED");
		System.out.println(counter.count);
		System.out.println(maxelapsed);
	}

}
