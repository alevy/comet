package edu.washington.cs.activedht.expt.remote;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.concurrent.Semaphore;

import com.aelitis.azureus.core.dht.transport.DHTTransportContact;
import com.aelitis.azureus.core.dht.transport.DHTTransportReplyHandlerAdapter;
import com.aelitis.azureus.core.dht.transport.DHTTransportValue;

public abstract class RemoteGet extends RemoteNodeAction {

	PrintStream out;

	public RemoteGet(String[] args) throws Exception {
		super(args);
		if (args.length > 1) {
			File file = new File(args[1]);
			out = new PrintStream(new FileOutputStream(file, false));
		}
	}

	public abstract void handleValue(byte[] value);

	public abstract byte[] getKey();

	public abstract byte[] getReaderID();

	public abstract byte[] getPayload();

	protected void run() throws Exception {
		final Semaphore sema = new Semaphore(0);
		contact.sendFindValue(new DHTTransportReplyHandlerAdapter() {

			@Override
			public void findValueReply(DHTTransportContact contact,
					DHTTransportValue[] values, byte diversificationType,
					boolean moreToCome) {
				if (values.length > 0) {
					handleValue(values[0].getValue());
				}
				sema.release();
			}

			@Override
			public void failed(DHTTransportContact contact, Throwable error) {
				error.printStackTrace();
				sema.release();
			}
		}, getKey(), getReaderID(), getPayload(), 1, (byte) 0);
		sema.acquire();
		out.flush();
		out.close();
	}

}