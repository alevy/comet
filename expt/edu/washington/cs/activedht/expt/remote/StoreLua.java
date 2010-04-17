/**
 * 
 */
package edu.washington.cs.activedht.expt.remote;

import java.io.File;
import java.io.FileInputStream;
import java.util.concurrent.Semaphore;

import se.krka.kahlua.luaj.compiler.LuaCompiler;
import se.krka.kahlua.vm.LuaClosure;
import se.krka.kahlua.vm.LuaState;
import se.krka.kahlua.vm.serialize.Serializer;

import com.aelitis.azureus.core.dht.transport.DHTTransportContact;
import com.aelitis.azureus.core.dht.transport.DHTTransportReplyHandlerAdapter;
import com.aelitis.azureus.core.dht.transport.DHTTransportValue;

import edu.washington.cs.activedht.transport.BasicDHTTransportValue;

/**
 * @author levya
 *
 */
public class StoreLua extends RemoteNodeAction {

	private byte[] payload;
	private byte[] key;
	
	public StoreLua(String[] args) throws Exception {
		super(args);
		key = contact.getID();
		if (args.length > 1) {
			LuaState state = new LuaState();
			File source = new File(args[1]);
			LuaClosure require = LuaCompiler.loadis(new FileInputStream(source),
					"source", state.getEnvironment());
			state.call(require);
			payload = Serializer.serialize(state.getEnvironment().rawget("object"), state.getEnvironment());
		}
	}
	
	public void setKey(byte[] key) {
		this.key = key;
	}
	
	public void setPayload(byte[] payload) {
		this.payload = payload;
	}

	public boolean run() throws Exception {
		final Semaphore sema = new Semaphore(0);
		final Bool b = new Bool();
		contact.sendStore(new DHTTransportReplyHandlerAdapter() {
			
			@Override
			public void storeReply(DHTTransportContact contact,
					byte[] diversifications) {
				b.b = true;
				sema.release();
				System.err.println("Stored");
			}
			
			@Override
			public void failed(DHTTransportContact contact, Throwable error) {
				error.printStackTrace();
				sema.release();				
			}
		}, new byte[][] {key}, new DHTTransportValue[][] { new DHTTransportValue[] { new BasicDHTTransportValue(
				System.currentTimeMillis(),
				payload, "", 1, transport.getLocalContact(), false,
				0) } }, true);
		sema.acquire();
		return b.b;
	}

	public static void main(String[] args) throws Exception {
		new StoreLua(args).run();
	}
	
	private static class Bool {
		private boolean b = false;
	}

}
