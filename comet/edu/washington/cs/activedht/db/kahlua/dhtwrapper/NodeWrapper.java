package edu.washington.cs.activedht.db.kahlua.dhtwrapper;

import org.gudy.azureus2.core3.util.HashWrapper;

import com.aelitis.azureus.core.dht.transport.DHTTransportContact;

import se.krka.kahlua.vm.JavaFunction;
import se.krka.kahlua.vm.LuaCallFrame;

/**
 * @author levya
 *
 */
public class NodeWrapper implements JavaFunction {

	protected final DHTTransportContact contact;

	public NodeWrapper(DHTTransportContact contact) {
		this.contact = contact;
	}
	
	public int call(LuaCallFrame callFrame, int nArguments) {
		
		return 0;
	}
	
	public String getIP() {
		return contact.getExternalAddress().getAddress().getHostAddress();
	}
	
	public int getPort() {
		return contact.getExternalAddress().getPort();
	}

	public HashWrapper getNodeID() {
		return new HashWrapper(contact.getID());
	}

}
