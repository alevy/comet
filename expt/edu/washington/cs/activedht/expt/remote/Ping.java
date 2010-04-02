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

	public Ping(String[] args) throws Exception {
		super(args);
	}

	protected void run() throws Exception {
		final Semaphore sema = new Semaphore(0);
		contact.sendPing(new DHTTransportReplyHandlerAdapter() {
			
			@Override
			public void pingReply(DHTTransportContact contact) {
				System.out.println(contact.getString());
				sema.release();
			}
			
			@Override
			public void pingReply(DHTTransportContact contact, int elapsed) {
				System.out.println(elapsed);
				sema.release();
			}
			
			public void failed(DHTTransportContact contact, Throwable error) {
				error.printStackTrace();
				sema.release();
			}
		});
		sema.acquire();
	}
	
	public static void main(String[] args) throws Exception {
		new Ping(args).run();
	}

}
