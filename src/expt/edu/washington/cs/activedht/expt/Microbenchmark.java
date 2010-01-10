/**
 * 
 */
package edu.washington.cs.activedht.expt;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Semaphore;

import org.keplerproject.luajava.LuaState;
import org.keplerproject.luajava.LuaStateFactory;

import com.aelitis.azureus.core.dht.DHTOperationAdapter;
import com.aelitis.azureus.core.dht.transport.DHTTransportContact;
import com.aelitis.azureus.core.dht.transport.DHTTransportValue;

import edu.washington.cs.activedht.lua.Serializer;

/**
 * @author levya
 *
 */
public class Microbenchmark {

	private final ActivePeer peer;
	private static final LuaState luaState = LuaStateFactory.newLuaState(); 

	public Microbenchmark(ActivePeer peer) {
		this.peer = peer;
	}
	
	public void put() throws InterruptedException {
		luaState.LdoString("activeobject = { onGet = function(self) return self.message end, message = \"world\" }");
		byte[] value = new Serializer(luaState).serialize(luaState.getLuaObject("activeobject"));
		
		final Semaphore sema = new Semaphore(1);
		sema.acquire();
		final List<DHTTransportContact> contacts = new LinkedList<DHTTransportContact>();
		peer.put("hello".getBytes(), value, new DHTOperationAdapter() {
			public void wrote(DHTTransportContact contact,
					DHTTransportValue value) {
				contacts.add(contact);
			}
			
			@Override
			public void complete(boolean t) {
				System.out.println(t);
				sema.release();
			}
		});
		sema.acquire();
		System.out.println(contacts);
	}
	
	public void get() throws InterruptedException {
		final Semaphore sema = new Semaphore(1);
		sema.acquire();
		final List<DHTTransportContact> contacts = new LinkedList<DHTTransportContact>();
		peer.get("hello".getBytes(), new DHTOperationAdapter() {
			public void complete(boolean t) {
				System.out.println(t);
				sema.release();
			}
			
			public void read(DHTTransportContact contact,
					DHTTransportValue value) {
				contacts.add(contact);
				System.out.println(new Serializer(luaState).deserialize(value.getValue()));
			}
		});
		sema.acquire();
		System.out.println(contacts);
	}
	
	public static void main(String[] args) throws Exception {
		ActivePeer peer = new ActivePeer(1234, "marykate.cs.washington.edu:48386");
		peer.init();
		Microbenchmark microbenchmark = new Microbenchmark(peer);
		microbenchmark.put();
		
		Thread.sleep(10000);
		
		microbenchmark.get();
	}
	
}
