package edu.washington.cs.activedht.expt;

import java.io.FileOutputStream;
import java.io.PrintStream;
import java.net.InetSocketAddress;
import java.util.Random;

import se.krka.kahlua.vm.LuaMapTable;
import se.krka.kahlua.vm.LuaTable;
import se.krka.kahlua.vm.serialize.Deserializer;
import se.krka.kahlua.vm.serialize.Serializer;

import com.aelitis.azureus.core.dht.netcoords.DHTNetworkPosition;
import com.aelitis.azureus.core.dht.transport.DHTTransportContact;
import com.aelitis.azureus.core.dht.transport.DHTTransportException;
import com.aelitis.azureus.core.dht.transport.DHTTransportReplyHandlerAdapter;
import com.aelitis.azureus.core.dht.transport.DHTTransportValue;
import com.aelitis.azureus.core.dht.transport.udp.DHTTransportUDP;
import com.aelitis.azureus.core.dht.transport.udp.impl.DHTTransportUDPContactImpl;
import com.aelitis.azureus.core.dht.transport.udp.impl.DHTTransportUDPImpl;

/**
 * @author levya
 * 
 */
public class TrackerClient {

	private ActivePeer peer;
	private InetSocketAddress remoteAddress;
	private PrintStream out;
	private int period;

	public TrackerClient(String[] args) throws Exception {
		peer = ActivePeer.create(args);
		remoteAddress = new InetSocketAddress("regina.cs.washington.edu", 5431);
		out = System.out;
		period = 10000;

		for (int i = 0; i < args.length; ++i) {
			if (args[i].equals("-o")) {
				out = new PrintStream(new FileOutputStream(args[++i], false));
			} else if (args[i].equals("-g")) {
				period = Integer.parseInt(args[++i]);
			} else if (args[i].equals("--tracker")) {
				String[] n = args[++i].split(":");
				remoteAddress = new InetSocketAddress(n[0], Integer.parseInt(n[1]));
			}
		}
	}

	public void run() throws DHTTransportException, InterruptedException {
		peer.init(null);
		DHTTransportUDPImpl transport = (DHTTransportUDPImpl) peer.dht
				.getTransport();
		DHTTransportContact contact = new DHTTransportUDPContactImpl(false,
				transport, remoteAddress, remoteAddress,
				DHTTransportUDP.PROTOCOL_VERSION_RESTRICT_ID_PORTS, 0, 0L);
		DHTTransportContact localContact = peer.dht.getTransport()
				.getLocalContact();

		LuaTable payload = new LuaMapTable();
		payload.rawset("event", "completed");
		payload.rawset("peer", localContact.getAddress().getHostName());
		out.println(System.currentTimeMillis());
		Random rand = new Random();
		while (true) {
			Thread.sleep(period / 2 + rand.nextInt(period));
			LuaMapTable vivaldi = LuaMapTable.fromDoubleArray(localContact
					.getNetworkPosition(
							DHTNetworkPosition.POSITION_TYPE_VIVALDI_V1)
					.getLocation());
			payload.rawset("vivaldi", vivaldi);
			contact.sendFindValue(new DHTTransportReplyHandlerAdapter() {

				@Override
				public void findValueReply(DHTTransportContact contact,
						DHTTransportValue[] values, byte diversificationType,
						boolean moreToCome) {
					for (DHTTransportValue value : values) {
						LuaMapTable table = (LuaMapTable)Deserializer.deserializeBytes(value
								.getValue(), null);
						String str = table.toString();
						out.println(System.currentTimeMillis() + "," + str.substring(1, str.length() - 1));
					}
				}

				@Override
				public void failed(DHTTransportContact contact, Throwable error) {
					error.printStackTrace();
				}
			}, contact.getID(), new byte[] {}, Serializer.serialize(payload,
					null), 1, (byte) 0);
		}
	}

	public static void main(String[] args) throws Exception {
		TrackerClient client = new TrackerClient(args);
		client.run();
	}

}
