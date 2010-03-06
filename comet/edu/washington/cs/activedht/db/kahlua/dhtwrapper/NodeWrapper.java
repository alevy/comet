package edu.washington.cs.activedht.db.kahlua.dhtwrapper;

import org.gudy.azureus2.core3.util.HashWrapper;

import se.krka.kahlua.vm.JavaFunction;
import se.krka.kahlua.vm.LuaCallFrame;
import se.krka.kahlua.vm.LuaTable;
import se.krka.kahlua.vm.LuaTableImpl;

import com.aelitis.azureus.core.dht.transport.DHTTransportContact;

/**
 * @author levya
 *
 */
public class NodeWrapper extends LuaTableWrapper {
	
	private class NodeWrapperFunction implements JavaFunction {
		static final int GET_IP = 1;
		static final int GET_PORT = 2;
		static final int GET_NODE_ID = 3;
		private final int type;
		
		public NodeWrapperFunction(int type) {
			this.type = type;
		}
		
		public int call(LuaCallFrame callFrame, int nArguments) {
			switch(type) {
			case GET_IP:
				callFrame.push(getIP());
				break;
			case GET_PORT:
				callFrame.push(getPort());
				break;
			case GET_NODE_ID:
				callFrame.push(getNodeID());
				break;
			default: return 0;
			}
			return 1;
		}
	}

	protected final DHTTransportContact contact;
	protected final LuaTable table = new LuaTableImpl();

	public NodeWrapper(DHTTransportContact contact) {
		super(new LuaTableImpl());
		this.contact = contact;
		super.rawset("getIP", new NodeWrapperFunction(NodeWrapperFunction.GET_IP));
		super.rawset("getID", new NodeWrapperFunction(NodeWrapperFunction.GET_NODE_ID));
		super.rawset("getPort", new NodeWrapperFunction(NodeWrapperFunction.GET_PORT));
	}
	
	
	public String getIP() {
		return contact.getExternalAddress().getAddress().getHostAddress();
	}
	
	public Double getPort() {
		return (double)contact.getExternalAddress().getPort();
	}

	public HashWrapper getNodeID() {
		return new HashWrapper(contact.getID());
	}

}
