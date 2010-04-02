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

	private final byte[] payload;
	
	public StoreLua(String[] args) throws Exception {
		super(args);
		if (args.length > 1) {
			LuaState state = new LuaState();
			File source = new File(args[1]);
			LuaClosure require = LuaCompiler.loadis(new FileInputStream(source),
					"source", state.getEnvironment());
			state.call(require);
			payload = Serializer.serialize(state.getEnvironment().rawget("object"), state.getEnvironment());
		} else {
			throw new IllegalArgumentException("Not enough arguments");
		}
	}

	protected void run() throws Exception {
		final Semaphore sema = new Semaphore(0);
		contact.sendStore(new DHTTransportReplyHandlerAdapter() {
			
			@Override
			public void storeReply(DHTTransportContact contact,
					byte[] diversifications) {
				sema.release();
				System.out.println("Stored");
			}
			
			@Override
			public void failed(DHTTransportContact contact, Throwable error) {
				error.printStackTrace();
				sema.release();				
			}
		}, new byte[][] {contact.getID()}, new DHTTransportValue[][] { new DHTTransportValue[] { new BasicDHTTransportValue(
				System.currentTimeMillis(),
				payload, "", 1, transport.getLocalContact(), false,
				0) } }, true);
		sema.acquire();
	}

	public static void main(String[] args) throws Exception {
		new StoreLua(args).run();
	}

}
