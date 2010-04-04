package edu.washington.cs.activedht.expt.remote;

import java.net.InetAddress;
import java.net.InetSocketAddress;

import org.gudy.azureus2.plugins.PluginInterface;

import com.aelitis.azureus.core.AzureusCoreFactory;
import com.aelitis.azureus.core.dht.DHT;
import com.aelitis.azureus.core.dht.DHTLogger;
import com.aelitis.azureus.core.dht.transport.udp.DHTTransportUDP;
import com.aelitis.azureus.core.dht.transport.udp.impl.DHTTransportUDPContactImpl;
import com.aelitis.azureus.core.dht.transport.udp.impl.DHTTransportUDPImpl;

public abstract class RemoteNodeAction {

	protected DHTTransportUDPContactImpl contact;
	protected DHTTransportUDPImpl transport;

	public RemoteNodeAction(String args[]) throws Exception {
		String node = "nethack.cs.washington.edu:1234";
		if (args.length > 0) {
			node = args[0];
		}

		String ip = node.split(":")[0];
		int port = Integer.parseInt(node.split(":")[1]);

		String myAddress = InetAddress.getLocalHost().getHostAddress();

		AzureusCoreFactory.create();

		InetSocketAddress remoteAddress = new InetSocketAddress(ip, port);

		int instanceId = InetAddress.getLocalHost().hashCode();
		
		InetSocketAddress localAddress = new InetSocketAddress(myAddress, 1234);
		DHTTransportUDPContactImpl localContact = new DHTTransportUDPContactImpl(false, transport, localAddress,
				localAddress, DHTTransportUDP.PROTOCOL_VERSION_RESTRICT_ID_PORTS, instanceId,
				0L);
		
		transport = new DHTTransportUDPImpl(
				DHTTransportUDP.PROTOCOL_VERSION_RESTRICT_ID_PORTS,
				DHT.NW_MAIN, false, myAddress, null, 1232, 1, 1, 10000, 0, 0,
				false, false, new DHTLogger() {

					@Override
					public PluginInterface getPluginInterface() {
						return null;
					}

					@Override
					public boolean isEnabled(int logType) {
						return false;
					}

					@Override
					public void log(String str) {
						// System.out.println(str);
					}

					@Override
					public void log(Throwable e) {
						// e.printStackTrace();
					}

					@Override
					public void log(int logType, String str) {
						// log(str);
					}

				}, localContact);
		localContact.setTransport(transport);

		contact = new DHTTransportUDPContactImpl(false, transport, remoteAddress,
				remoteAddress, DHTTransportUDP.PROTOCOL_VERSION_RESTRICT_ID_PORTS, 0,
				0L);
	}

}