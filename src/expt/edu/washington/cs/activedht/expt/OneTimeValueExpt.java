package edu.washington.cs.activedht.expt;

import com.aelitis.azureus.core.AzureusCoreFactory;
import com.aelitis.azureus.core.dht.*;
import com.aelitis.azureus.core.dht.control.DHTControlContact;
import com.aelitis.azureus.core.dht.nat.DHTNATPuncherAdapter;
import com.aelitis.azureus.core.dht.nat.impl.DHTNATPuncherImpl;
import com.aelitis.azureus.core.dht.transport.*;
import com.aelitis.azureus.core.dht.transport.loopback.DHTTransportLoopbackImpl;
import com.aelitis.azureus.core.dht.transport.udp.DHTTransportUDP;
import com.aelitis.azureus.core.dht.transport.udp.impl.DHTTransportUDPImpl;
import com.aelitis.azureus.plugins.dht.impl.DHTPluginStorageManager;

import edu.washington.cs.activedht.code.insecure.ActiveObjectAdapter;
import edu.washington.cs.activedht.code.insecure.candefine.OneTimeActiveObject;
import edu.washington.cs.activedht.db.ActiveDHTInitializer;

import java.io.*;
import java.math.BigInteger;
import java.net.InetSocketAddress;
import java.security.KeyFactory;
import java.security.Signature;
import java.security.interfaces.RSAPrivateKey;
import java.security.spec.RSAPrivateKeySpec;
import java.util.*;

import org.gudy.azureus2.core3.util.AEThread2;
import org.gudy.azureus2.core3.util.SHA1Simple;
import org.gudy.azureus2.core3.util.Timer;
import org.gudy.azureus2.core3.util.TimerEvent;
import org.gudy.azureus2.core3.util.TimerEventPerformer;
import org.gudy.azureus2.plugins.PluginInterface;
import org.gudy.azureus2.plugins.logging.LoggerChannel;
import org.gudy.azureus2.plugins.logging.LoggerChannelListener;

/**
 * @author levya
 * 
 */

public class OneTimeValueExpt implements DHTNATPuncherAdapter {
	static boolean AELITIS_TEST = false;
	static InetSocketAddress AELITIS_ADDRESS = new InetSocketAddress(
			"213.186.46.164", 6881);

	static int DEFAULT_NETWORK = DHT.NW_CVS;

	static {

		DHTTransportUDPImpl.TEST_EXTERNAL_IP = false;
	}

	int num_dhts = 3;
	int num_stores = 2;
	static int MAX_VALUES = 10000;

	boolean udp_protocol = true;
	int udp_timeout = 1000;

	static int K = 20;
	static int B = 5;
	static int ID_BYTES = 20;

	int fail_percentage = 00;

	static Properties dht_props = new Properties();

	static {
		dht_props.put(DHT.PR_CONTACTS_PER_NODE, new Integer(K));
		dht_props.put(DHT.PR_NODE_SPLIT_FACTOR, new Integer(B));
		dht_props.put(DHT.PR_CACHE_REPUBLISH_INTERVAL, new Integer(30000));
		dht_props.put(DHT.PR_ORIGINAL_REPUBLISH_INTERVAL, new Integer(60000));
	}

	static byte[] th_key = new byte[] { 1, 1, 1, 1 };

	static Map check = new HashMap();

	static DHTLogger logger;

	static {

		final LoggerChannel c_logger = AzureusCoreFactory.create()
				.getPluginManager().getDefaultPluginInterface().getLogger()
				.getNullChannel("test");

		c_logger.addListener(new LoggerChannelListener() {
			public void messageLogged(int type, String content) {
				System.out.println(content);
			}

			public void messageLogged(String str, Throwable error) {
				System.out.println(str);

				error.printStackTrace();
			}
		});

		logger = new DHTLogger() {
			public void log(String str) {
				c_logger.log(str);
			}

			public void log(Throwable e) {
				c_logger.log(e);
			}

			public void log(int log_type, String str) {
				if (isEnabled(log_type)) {

					c_logger.log(str);
				}
			}

			public boolean isEnabled(int log_type) {
				return (true);
			}

			public PluginInterface getPluginInterface() {
				return (c_logger.getLogger().getPluginInterface());
			}
		};
	}

	public static DHTLogger getLogger() {
		return (logger);
	}

	public static void main(String[] args) throws Exception {
		ActiveDHTInitializer.prepareRuntimeForActiveCode();
		new OneTimeValueExpt(10, new PrintStream(new File("/tmp/test.txt")), 5000);
	}

	Map port_map = new HashMap();

	protected OneTimeValueExpt() throws Exception {
		this(3, new PrintStream(new File("/Users/levya/test.txt")), 5000);
	}

	protected OneTimeValueExpt(int _num_dhts, final PrintStream out, long gap) {
		this.num_dhts = _num_dhts;

		try {

			DHT[] dhts = new DHT[num_dhts * 2 + 30];
			DHTTransport[] transports = new DHTTransport[num_dhts * 2 + 30];

			for (int i = 0; i < num_dhts; i++) {

				createDHT(dhts, transports, DEFAULT_NETWORK, i);
			}

			for (int i = 0; i < num_dhts - 1; i++) {

				if (AELITIS_TEST) {

					((DHTTransportUDP) transports[i]).importContact(
							AELITIS_ADDRESS,
							DHTTransportUDP.PROTOCOL_VERSION_MAIN);

				} else {
					ByteArrayOutputStream baos = new ByteArrayOutputStream();

					DataOutputStream daos = new DataOutputStream(baos);

					transports[i].getLocalContact().exportContact(daos);

					daos.close();

					transports[i + 1].importContact(new DataInputStream(
							new ByteArrayInputStream(baos.toByteArray())));
				}

				dhts[i].integrate(true);

				if (i > 0 && i % 10 == 0) {

					System.out.println("Integrated " + i + " DHTs");
				}
			}

			if (AELITIS_TEST) {

				((DHTTransportUDP) transports[num_dhts - 1]).importContact(
						AELITIS_ADDRESS, DHTTransportUDP.PROTOCOL_VERSION_MAIN);

			} else {

				ByteArrayOutputStream baos = new ByteArrayOutputStream();

				DataOutputStream daos = new DataOutputStream(baos);

				transports[0].getLocalContact().exportContact(daos);

				daos.close();

				transports[num_dhts - 1].importContact(new DataInputStream(
						new ByteArrayInputStream(baos.toByteArray())));
			}

			dhts[num_dhts - 1].integrate(true);

			DHTTransportLoopbackImpl.setFailPercentage(fail_percentage);

			// dht1.print();

			// DHTTransportLoopbackImpl.setLatency( 500);

			/*
			 * System.out.println( "before put:" +
			 * transports[99].getStats().getString()); dhts[99].put(
			 * "fred".getBytes(), new byte[2]); System.out.println( "after put:"
			 * + transports[99].getStats().getString());
			 * 
			 * System.out.println( "get:" + dhts[0].get( "fred".getBytes()));
			 * System.out.println( "get:" + dhts[77].get( "fred".getBytes()));
			 */

			Map store_index = new HashMap();

			for (int i = 0; i < num_stores; i++) {

				int dht_index = (int) (Math.random() * num_dhts);

				DHT dht = dhts[dht_index];

				dht.put(("" + i).getBytes(), "", new byte[4], (byte) 0,
						new DHTOperationAdapter());

				store_index.put("" + i, dht);

				if (i != 0 && i % 100 == 0) {

					System.out.println("Stored " + i + " values");
				}
			}

			Timer timer = new Timer("");

			timer.addPeriodicEvent(10000, new TimerEventPerformer() {
				public void perform(TimerEvent event) {
					if (!udp_protocol) {

						DHTTransportStats stats = DHTTransportLoopbackImpl
								.getOverallStats();

						System.out.println("Overall stats: "
								+ stats.getString());
					}
				}
			});

			LineNumberReader reader = new LineNumberReader(
					new InputStreamReader(System.in));
			
			for (int i = 0; i < 100; ++i) {

				int dht_index = (int) (Math.random() * num_dhts);

				DHT dht = dhts[dht_index];
				byte[] key = ("hello" + i).getBytes();
				dht
						.put(key, "",
								getOneTimeValueBytes("world"), (byte) (Math
										.random() * 255),
								new DHTOperationAdapter());
			}

			waitFor(10000);
			
			for (int i = 0; i < 100; ++i) {

				int dht_index = (int) (Math.random() * num_dhts);

				DHT dht = dhts[dht_index];
				byte[] key = ("hello" + i).getBytes();
				dht
						.put(key, "",
								getOneTimeValueBytes("world"), (byte) (Math
										.random() * 255),
								new DHTOperationAdapter());

				dht.get(key, "", (byte) 0, 32, 0, false, false,
						new DHTOperationAdapter() {
							boolean found = false;

							public void read(DHTTransportContact contact,
									DHTTransportValue value) {
								found = true;
							}

							public void complete(boolean timeout) {
								out.println(found);
							}
						});
				
				waitFor(gap);

				dht.get(key, "", (byte) 0, 32, 0, false, false,
						new DHTOperationAdapter() {
							boolean found = false;

							public void read(DHTTransportContact contact,
									DHTTransportValue value) {
								found = true;
							}

							public void complete(boolean timeout) {
								out.println(found);
							}
						});
			}
		} catch (Throwable e) {

			e.printStackTrace();
		}
	}

	private void waitFor(long timeout) {
		for (long start = System.currentTimeMillis(); System
				.currentTimeMillis() < start + timeout;) {

		}
	}

	private byte[] getOneTimeValueBytes(String string) {
		return new String(((ActiveObjectAdapter) new OneTimeActiveObject(string
				.getBytes())).getValue()).getBytes();
	}

	protected byte[] getBytes(String val) {
		return val.getBytes();
	}

	protected String getString(DHTTransportValue value) {
		return (new String(value.getValue()) + "; flags="
				+ Integer.parseInt(String.valueOf(value.getFlags()), 16)
				+ ", orig=" + value.getOriginator().getAddress());
	}

	protected String getTmpDirectory() {
		return "/tmp/dht";
	}

	protected void createDHT(DHT[] dhts, DHTTransport[] transports,
			int network, int i)

	throws DHTTransportException {
		DHTTransport transport;

		if (udp_protocol) {

			Integer next_port = (Integer) port_map.get(new Integer(network));

			if (next_port == null) {

				next_port = new Integer(0);

			} else {

				next_port = new Integer(next_port.intValue() + 1);
			}

			port_map.put(new Integer(network), next_port);

			byte protocol = network == 0 ? DHTTransportUDP.PROTOCOL_VERSION_MAIN
					: DHTTransportUDP.PROTOCOL_VERSION_CVS;

			// byte protocol =
			// i%2==0?DHTTransportUDP.PROTOCOL_VERSION_MAIN:DHTTransportUDP.PROTOCOL_VERSION_CVS;

			transport = DHTTransportFactory.createUDP(protocol, network, false,
					null, null, 6890 + next_port.intValue(), 5, 3, udp_timeout,
					50, 25, false, false, logger);

		} else {

			transport = DHTTransportFactory.createLoopback(ID_BYTES);
		}

		transport.registerTransferHandler(th_key,
				new DHTTransportTransferHandler() {
					public String getName() {
						return ("test");
					}

					public byte[] handleRead(DHTTransportContact originator,
							byte[] key) {
						byte[] data = new byte[1000];

						System.out.println("handle read -> length = "
								+ data.length);

						return (data);
					}

					public byte[] handleWrite(DHTTransportContact originator,
							byte[] key, byte[] value) {
						byte[] reply = null;

						if (value.length == 1000) {

							reply = new byte[4];
						}

						System.out.println("handle write -> length = "
								+ value.length + ", reply = " + reply);

						return (reply);
					}
				});

		/*
		 * HashWrapper id = new HashWrapper(
		 * transport.getLocalContact().getID());
		 * 
		 * if ( check.get(id) != null ){
		 * 
		 * System.out.println( "Duplicate ID - aborting" );
		 * 
		 * return; }
		 * 
		 * check.put(id,"");
		 */

		DHTStorageAdapter storage_adapter = createStorageAdapter(network,
				logger, new File(getTmpDirectory() + i));

		DHT dht = DHTFactory.create(transport, dht_props, storage_adapter,
				this, logger);

		dhts[i] = dht;

		transports[i] = transport;
	}

	protected DHTStorageAdapter createStorageAdapter(int network,
			DHTLogger logger, File file) {
		return new DHTPluginStorageManager(network, logger, file);
	}

	/*
	 * public DHTStorageKey keyCreated( HashWrapper key, boolean local ) {
	 * System.out.println( "key created" );
	 * 
	 * return( new DHTStorageKey() { public byte getDiversificationType() {
	 * return( DHT.DT_NONE ); } public void serialiseStats( DataOutputStream os
	 * )
	 * 
	 * throws IOException { os.writeInt( 45 ); } }); }
	 * 
	 * public void keyDeleted( DHTStorageKey key ) { System.out.println(
	 * "key deleted" ); }
	 * 
	 * public void keyRead( DHTStorageKey adapter_key, DHTTransportContact
	 * contact ) { System.out.println( "value read" ); }
	 * 
	 * public void valueAdded( DHTStorageKey key, DHTTransportValue value ) {
	 * System.out.println( "value added" ); }
	 * 
	 * public void valueUpdated( DHTStorageKey key, DHTTransportValue old_value,
	 * DHTTransportValue new_value) { System.out.println( "value updated" ); }
	 * 
	 * public void valueDeleted( DHTStorageKey key, DHTTransportValue value ) {
	 * System.out.println( "value deleted" ); }
	 * 
	 * public byte[][] getExistingDiversification( byte[] key, boolean
	 * put_operation ) { System.out.println(
	 * "getExistingDiversification: put = " + put_operation );
	 * 
	 * return( new byte[][]{ key }); }
	 * 
	 * public byte[][] createNewDiversification( byte[] key, boolean
	 * put_operation, int diversification_type ) { System.out.println(
	 * "createNewDiversification: put = " + put_operation + ", type = " +
	 * diversification_type );
	 * 
	 * return( new byte[0][] ); }
	 */

	public Map getClientData(InetSocketAddress originator, Map originator_data) {
		System.out.println("getClientData - " + originator_data + "/"
				+ originator);

		Map res = new HashMap();

		res.put("udp_data_port", new Long(1234));
		res.put("tcp_data_port", new Long(5678));

		return (res);
	}

	protected static void usage() {
		System.out.println("syntax: [p g] <key>[=<value>]");
	}
}
