package edu.washington.cs.activedht.expt;

import java.io.File;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.gudy.azureus2.core3.util.Timer;
import org.gudy.azureus2.core3.util.TimerEvent;
import org.gudy.azureus2.core3.util.TimerEventPerformer;
import org.gudy.azureus2.plugins.PluginInterface;
import org.gudy.azureus2.plugins.logging.LoggerChannel;
import org.gudy.azureus2.plugins.logging.LoggerChannelListener;

import com.aelitis.azureus.core.AzureusCoreFactory;
import com.aelitis.azureus.core.dht.DHT;
import com.aelitis.azureus.core.dht.DHTFactory;
import com.aelitis.azureus.core.dht.DHTLogger;
import com.aelitis.azureus.core.dht.DHTOperationAdapter;
import com.aelitis.azureus.core.dht.DHTStorageAdapter;
import com.aelitis.azureus.core.dht.nat.DHTNATPuncherAdapter;
import com.aelitis.azureus.core.dht.transport.DHTTransport;
import com.aelitis.azureus.core.dht.transport.DHTTransportContact;
import com.aelitis.azureus.core.dht.transport.DHTTransportException;
import com.aelitis.azureus.core.dht.transport.DHTTransportFactory;
import com.aelitis.azureus.core.dht.transport.DHTTransportStats;
import com.aelitis.azureus.core.dht.transport.DHTTransportValue;
import com.aelitis.azureus.core.dht.transport.loopback.DHTTransportLoopbackImpl;
import com.aelitis.azureus.core.dht.transport.udp.DHTTransportUDP;
import com.aelitis.azureus.plugins.dht.impl.DHTPluginStorageManager;

//import edu.washington.cs.activedht.db.ActiveDHTInitializer;

public class ActiveDHTPeerRunner {

	private static final int K = 20;
	private static final int B = 5;
	private static final int DHT_NETWORK = DHT.NW_CVS;
	private static final int UDP_TIMEOUT = 100;
	private static final int DEFAULT_PORT = 6850;

	private static final DHTLogger LOGGER;
	private DHT dht;
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

		LOGGER = new DHTLogger() {
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

	public ActiveDHTPeerRunner(int port, String bootstrapAddress,
			int bootstrapPort, Properties properties,
			DHTStorageAdapter storageAdapter, DHTNATPuncherAdapter natAdapter,
			String dhtDirectory) throws Exception {
		DHTTransport transport = DHTTransportFactory.createUDP(
				DHTTransportUDP.PROTOCOL_VERSION_CVS, DHT_NETWORK, false, bootstrapAddress,
				bootstrapAddress, port, 3, 1, UDP_TIMEOUT, 25, 25, false, false, LOGGER);
		dht = DHTFactory.create(transport, properties,
				new DHTPluginStorageManager(DHT_NETWORK, LOGGER, new File(
						dhtDirectory)), natAdapter, LOGGER);
		//AzureusCoreFactory.getSingleton().start();

		Exception thrown = null;
		for (int i = 0; i < 10; ++i) {
			try {
				((DHTTransportUDP) transport).importContact(
						new InetSocketAddress(bootstrapAddress, bootstrapPort),
						DHTTransportUDP.PROTOCOL_VERSION_MAIN);
				dht.integrate(true);
				break;
			} catch (Exception e) {
				thrown = e;
				System.out.println("Failed attempt" + i);
				Thread.sleep(500);
			}
		}

		if (thrown != null) {
			throw thrown;
		}

		Timer timer = new Timer("");

		timer.addPeriodicEvent(10000, new TimerEventPerformer() {
			public void perform(TimerEvent event) {
				if (!false) {

					dht.get("hello".getBytes(), "", (byte) 0, 32, 0, false, false,
							new DHTOperationAdapter() {
								
								public void read(DHTTransportContact contact,
										DHTTransportValue value) {
									System.out.println(value.getString());
								}

								public void complete(boolean timeout) {
									System.err.println("Complete");
								}
							});
					dht.put("hello".getBytes(), "", "world".getBytes(), (byte) (Math
							.random() * 255), new DHTOperationAdapter());
				}
			}
		});
	}

	public DHT getDHT() {
		return dht;
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {
		//ActiveDHTInitializer.prepareRuntimeForActiveCode();
		
		Properties properties = new Properties();
		properties.put(DHT.PR_CONTACTS_PER_NODE, K);
		properties.put(DHT.PR_NODE_SPLIT_FACTOR, B);
		properties.put(DHT.PR_CACHE_REPUBLISH_INTERVAL, 30000);
		properties.put(DHT.PR_ORIGINAL_REPUBLISH_INTERVAL, 60000);

		DHTStorageAdapter storageAdapter = new DHTPluginStorageManager(
				DHT_NETWORK, LOGGER, new File("/tmp/activedht"));
		DHTNATPuncherAdapter natAdapter = new DHTNATPuncherAdapter() {
			
			public Map getClientData(InetSocketAddress originator,
					Map originator_data) {
				System.out.println("getClientData - " + originator_data + "/"
						+ originator);

				Map res = new HashMap();

				res.put("udp_data_port", new Long(1234));
				res.put("tcp_data_port", new Long(5678));

				return (res);
			}

		};

		ActiveDHTPeerRunner peer = new ActiveDHTPeerRunner(DEFAULT_PORT,
				"marykate.cs.washington.edu", DEFAULT_PORT, properties,
				storageAdapter, natAdapter, "/tmp/activedht");
	}

}
