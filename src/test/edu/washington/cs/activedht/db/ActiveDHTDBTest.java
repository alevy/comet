package edu.washington.cs.activedht.db;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;

import com.aelitis.azureus.core.dht.netcoords.DHTNetworkPosition;
import com.aelitis.azureus.core.dht.transport.DHTTransport;
import com.aelitis.azureus.core.dht.transport.DHTTransportContact;
import com.aelitis.azureus.core.dht.transport.DHTTransportException;
import com.aelitis.azureus.core.dht.transport.DHTTransportFullStats;
import com.aelitis.azureus.core.dht.transport.DHTTransportReplyHandler;
import com.aelitis.azureus.core.dht.transport.DHTTransportValue;
import com.aelitis.azureus.plugins.dht.impl.DHTPluginStorageManager;

import junit.framework.TestCase;

import edu.washington.cs.activedht.code.insecure.DHTEvent;
import edu.washington.cs.activedht.code.insecure.DHTEventHandlerCallbackTest;
import edu.washington.cs.activedht.code.insecure.candefine.ActiveCode;
import edu.washington.cs.activedht.code.insecure.candefine.TestActiveCode;

class TestDHTTransportContact implements DHTTransportContact {
	String id;
	
	public TestDHTTransportContact(int id) {
		this.id = "" + id;
	}
	
	@Override
	public void exportContact(DataOutputStream os) throws IOException,
			DHTTransportException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public InetSocketAddress getAddress() {
		return new InetSocketAddress("host."+ id +".com", 1024);
	}

	@Override
	public long getClockSkew() { return 0; }

	@Override
	public InetSocketAddress getExternalAddress() { return getAddress(); }

	@Override
	public byte[] getID() { return id.getBytes(); }

	@Override
	public int getInstanceID() { return id.hashCode(); }

	@Override
	public int getMaxFailForLiveCount() { return 0; }

	@Override
	public int getMaxFailForUnknownCount() { return 0; }

	@Override
	public String getName() { return id; }

	@Override
	public DHTNetworkPosition getNetworkPosition(byte position_type) {
		return null;
	}

	@Override
	public DHTNetworkPosition[] getNetworkPositions() { return null; }

	@Override
	public byte getProtocolVersion() { return 0; }

	@Override
	public int getRandomID() { return id.hashCode(); }

	@Override
	public DHTTransportFullStats getStats() { return null; }

	@Override
	public String getString() { return id; }

	@Override
	public DHTTransport getTransport() { return null; }

	@Override
	public boolean isAlive(long timeout) { return true; }

	@Override
	public boolean isValid() { return true; }

	@Override
	public void remove() { }

	@Override
	public void sendFindNode(DHTTransportReplyHandler handler, byte[] id) { }

	@Override
	public void sendFindValue(DHTTransportReplyHandler handler, byte[] key,
			int max_values, byte flags) { }

	@Override
	public void sendImmediatePing(DHTTransportReplyHandler handler,
			                      long timeout) { }

	@Override
	public void sendKeyBlock(DHTTransportReplyHandler handler,
			byte[] key_block_request, byte[] key_block_signature) { }

	@Override
	public void sendPing(DHTTransportReplyHandler handler) { }

	@Override
	public void sendStats(DHTTransportReplyHandler handler) { }

	@Override
	public void sendStore(DHTTransportReplyHandler handler, byte[][] keys,
			DHTTransportValue[][] value_sets, boolean immediate) { }

	@Override
	public void setRandomID(int id) { }
}

class TestDHTTransportValue implements DHTTransportValue {
	private DHTTransportContact originator;
	private byte[] value;
	private boolean is_local;
	
	public TestDHTTransportValue(DHTTransportContact originator,
			 byte[] value, boolean is_local) {
		this.originator = originator;
		this.value = value;
		this.is_local = is_local;
	}
	
	@Override
	public long getCreationTime() { return 0; }

	@Override
	public int getFlags() { return 0; }

	@Override
	public DHTTransportContact getOriginator() { return originator; }

	@Override
	public String getString() { return "" + value; }

	@Override
	public byte[] getValue() { return value; }

	@Override
	public int getVersion() { return 0; }

	@Override
	public boolean isLocal() { return is_local; }
	
}

public class ActiveDHTDBTest extends TestCase {
	private ActiveDHTDB db;
	private byte[] value_bytes;
	
	// TestCase functions:
	
	@Override
	protected void setUp() {
		db = new ActiveDHTDB(new DHTPluginStorageManager(0, null, null),
				             0, 0, null, false);

		ActiveCode object = new TestActiveCode(DHTEvent.GET);
		value_bytes = DHTEventHandlerCallbackTest.serializeActiveObject(object);
	}
	
	@Override
	protected void tearDown() { }

	// Test cases:

	public void testOnInitialPutOnGet() {

	}
}
