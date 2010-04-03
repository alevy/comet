package edu.washington.cs.activedht.expt.remote;

import java.util.concurrent.Semaphore;

import com.aelitis.azureus.core.dht.transport.DHTTransportContact;
import com.aelitis.azureus.core.dht.transport.DHTTransportFullStats;
import com.aelitis.azureus.core.dht.transport.DHTTransportReplyHandlerAdapter;

/**
 * 
 * @author levya
 *
 */
public class Stats extends RemoteNodeAction {
	
	public Stats(String[] args) throws Exception {
		super(args);
	}

	protected void run() throws Exception {
		final Semaphore sema = new Semaphore(0);
		contact.sendStats(new DHTTransportReplyHandlerAdapter() {
			@Override
			public void statsReply(DHTTransportContact contact,
					DHTTransportFullStats stats) {
				System.out.println(stats.getString());
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
		new Stats(args).run();
	}
}
