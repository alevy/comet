package edu.washington.cs.activedht.expt;

import java.io.File;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.apache.log4j.Logger;
import org.gudy.azureus2.plugins.PluginInterface;

import com.aelitis.azureus.core.AzureusCore;
import com.aelitis.azureus.core.AzureusCoreFactory;
import com.aelitis.azureus.core.dht.DHT;
import com.aelitis.azureus.core.dht.DHTFactory;
import com.aelitis.azureus.core.dht.DHTLogger;
import com.aelitis.azureus.core.dht.DHTOperationAdapter;
import com.aelitis.azureus.core.dht.DHTStorageAdapter;
import com.aelitis.azureus.core.dht.control.DHTControl;
import com.aelitis.azureus.core.dht.impl.DHTLog;
import com.aelitis.azureus.core.dht.nat.DHTNATPuncherAdapter;
import com.aelitis.azureus.core.dht.transport.DHTTransportContact;
import com.aelitis.azureus.core.dht.transport.DHTTransportException;
import com.aelitis.azureus.core.dht.transport.udp.DHTTransportUDP;
import com.aelitis.azureus.core.dht.transport.udp.impl.DHTTransportUDPImpl;
import com.aelitis.azureus.plugins.dht.impl.DHTPluginStorageManager;

import edu.washington.cs.activedht.db.ActiveDHTInitializer;

/**
 * Vuze-based implementation of the VanishBackendInterface.
 * 
 * @author roxana
 */

class DHTParams {
	final int kDhtIndexLen;
	final int kNumTries;
	final long kRetrySleep_ms;

	final int kNumTriesIntegrate;
	final long kIntegrateRetryInterval_ms;

	final int kDdhtOperationBulkSize;
	final long kTimeoutBetweenOperationBulks;

	public DHTParams(int dhtIndexLen, int numTries, int retrySleep_ms,
			int dhtOperationBulkSize, long timeoutBetweenOperationBulks,
			int numTriesIntegrate, long integrateRetryInterval_ms) {
		this.kDhtIndexLen = dhtIndexLen;
		this.kNumTries = numTries;
		this.kRetrySleep_ms = retrySleep_ms;
		this.kDdhtOperationBulkSize = dhtOperationBulkSize;
		this.kTimeoutBetweenOperationBulks = timeoutBetweenOperationBulks;
		this.kNumTriesIntegrate = numTriesIntegrate;
		this.kIntegrateRetryInterval_ms = integrateRetryInterval_ms;
	}
}

public class ActivePeer implements DHTNATPuncherAdapter {
	private static final AzureusCore azureusCore = AzureusCoreFactory.create();
	// DHT-related params (these are fixed forever, as we give them to DHT):
	private final boolean kDhtLoggingOn;
	private final int kDhtLookupConcurrency;

	private final int kDhtUDPOperationTimeoutMin_ms;
	private final int kDhtUDPOperationTimeoutMax_ms;
	private final long kDhtUDPTimeoutIntervalRecalibrationPeriod;

	private final String kDhtBootstrapAddress;
	private final int kDhtBootstrapPort;

	private final int kDhtNetwork;
	private final byte kDhtProtocolVersion;

	private int kDhtPort;
	private final int kDhtNumReplicas;
	private final String kDhtDirectory;

	private final DHTLogger kDhtLogger;
	private final DHTStorageAdapter kStorageManager;

	private final Properties kDhtProperties;

	// Vanish backend params, which are reloadable from the Configuration.
	private DHTParams params;

	// State:

	private final Logger LOG;

	private final Timer timeout_recalibration_process;

	/** The DHT object. */
	private DHT dht = null; // null until init is called
	private ConfigurableTimeoutDHTTransport transport = null;
	/** Protects the DHT and transport. */
	ReentrantReadWriteLock dht_lock = new ReentrantReadWriteLock();

	private int current_udp_timeout_range_min;
	private int current_udp_timeout_range_max;
	private int current_udp_timeout;

	public ActivePeer(int port, String bootstrap) throws Exception {
		this(port, bootstrap, false);
	}

	public ActivePeer(int port, String bootstrap, boolean logging)
			throws Exception {
		ActiveDHTInitializer.prepareRuntimeForActiveCode();
		this.LOG = Logger.getLogger(this.getClass());

		// Load the parameters from the configuration:

		kDhtLoggingOn = logging;
		kDhtLookupConcurrency = 20;
		kDhtPort = port;
		kDhtNumReplicas = 20;

		kDhtUDPOperationTimeoutMin_ms = 200;
		kDhtUDPOperationTimeoutMax_ms = 2000;
		kDhtUDPTimeoutIntervalRecalibrationPeriod = 1;

		String full_addr = bootstrap;
		kDhtBootstrapAddress = getHostname(full_addr);
		kDhtBootstrapPort = getPort(full_addr);

		kDhtNetwork = DHT.NW_CVS;
		kDhtProtocolVersion = (kDhtNetwork == DHT.NW_MAIN ? DHTTransportUDP.PROTOCOL_VERSION_MAIN
				: DHTTransportUDP.PROTOCOL_VERSION_CVS);

		kDhtDirectory = "/tmp/activedht";

		kDhtLogger = getLogger();
		kStorageManager = createStorageAdapter(kDhtNetwork, kDhtLogger,
				new File(kDhtDirectory));

		kDhtProperties = constructDHTProperties(kDhtLoggingOn);

		params = this.getRenewedParamsFromConfig();
		setParams(params);

		timeout_recalibration_process = new Timer("timeoutrecalib", true);

		// Initialize non-final state:
		initState();
	}
	
	public DHTTransportContact getLocalContact() {
		return dht.getControl().getTransport().getLocalContact();
	}

	private void initState() {
		resetTimeoutInterval();

		if (current_udp_timeout_range_max < current_udp_timeout_range_min) {
			throw new IllegalStateException("Invalid UDP timeout range");
		}

		current_udp_timeout = (current_udp_timeout_range_min + current_udp_timeout_range_max) / 2;
	}

	/**
	 * @param full_address
	 *            machine:port format.
	 * @return
	 * @throws VanishBackendException
	 */
	private String getHostname(String full_address) {
		return full_address.substring(0, full_address.indexOf(':'));
	}

	private int getPort(String full_address) {
		return Integer.parseInt(full_address.substring(full_address
				.indexOf(':') + 1));
	}

	// Initializable interface:

	/**
	 * Thread-safe.
	 * @param default_ip_address 
	 */
	// @Override
	public void init(String default_ip_address) throws RuntimeException {
		// final LogStat log_stat = LOG.logStat("DHTVanishBackend.init");

		dht_lock.writeLock().lock();
		if (isInitialized())
			throw new RuntimeException("Already init");
		try {
			startDHTNode(current_udp_timeout, default_ip_address);
		} finally {
			dht_lock.writeLock().unlock();
		}

		if (kDhtUDPTimeoutIntervalRecalibrationPeriod > 0) {
			timeout_recalibration_process.schedule(
					new TimeoutRecalibrationTask(),
					kDhtUDPTimeoutIntervalRecalibrationPeriod,
					kDhtUDPTimeoutIntervalRecalibrationPeriod);
		}
		// log_stat.end();
	}

	/**
	 * Not thread-safe!
	 */
	// @Override
	public boolean isInitialized() {
		boolean ret = false;
		ret = (dht != null);
		return ret;
	}

	// @Override
	public void stop() throws RuntimeException {
		if (dht != null)
			dht.destroy();
		azureusCore.stop();
	}

	// Helper functions:

	private Properties constructDHTProperties(boolean loggingOn) {
		DHTTransportUDPImpl.TEST_EXTERNAL_IP = false; // TODO(roxana): true?

		Properties dht_props = new Properties();
		dht_props.put(DHT.PR_CONTACTS_PER_NODE, kDhtNumReplicas);
		dht_props.put(DHT.PR_LOOKUP_CONCURRENCY, kDhtLookupConcurrency);
		dht_props.put(DHT.PR_SEARCH_CONCURRENCY, kDhtLookupConcurrency);
		dht_props.put(DHT.PR_CACHE_REPUBLISH_INTERVAL, new Integer(
				DHTControl.CACHE_REPUBLISH_INTERVAL_DEFAULT));

		DHTLog.logging_on = loggingOn;

		return dht_props;
	}

	private class ConfigurableTimeoutDHTTransport extends DHTTransportUDPImpl {
		public ConfigurableTimeoutDHTTransport(byte protocol_version,
				int network, boolean v6, String ip, String default_ip,
				int port, int max_fails_for_live, int max_fails_for_unknown,
				long timeout, int send_delay, int receive_delay,
				boolean bootstrap_node, boolean reachable, DHTLogger logger)
				throws DHTTransportException {
			super(protocol_version, network, v6, ip, default_ip, port,
					max_fails_for_live, max_fails_for_unknown, timeout,
					send_delay, receive_delay, bootstrap_node, reachable,
					logger);
		}

	}

	/**
	 * Must be called while holding the writelock on dht_lock.
	 * @param defaultIpAddress 
	 * 
	 * @param bootstrapAddress
	 * @throws VanishBackendException
	 */
	protected void startDHTNode(int udp_timeout, String defaultIpAddress) throws RuntimeException {
		try {
			transport = new ConfigurableTimeoutDHTTransport(
					kDhtProtocolVersion, kDhtNetwork, false, defaultIpAddress, defaultIpAddress,
					kDhtPort, 3, 1, udp_timeout, 0, 0, false, false,
					kDhtLogger);
			dht = DHTFactory.create(transport, kDhtProperties, kStorageManager,
					this, kDhtLogger);
			Throwable exception = null;
			for (int i = 0; i < params.kNumTriesIntegrate; ++i) {
				try {
					((DHTTransportUDP) transport).importContact(
							new InetSocketAddress(kDhtBootstrapAddress,
									kDhtBootstrapPort), kDhtProtocolVersion);
					dht.integrate(true);
					exception = null;
					break; // integration completed successfully.
				} catch (Throwable e) {
					exception = e;
					Thread.sleep(params.kIntegrateRetryInterval_ms);
				}
			}
			if (exception != null)
				throw exception;
		} catch (Throwable e) {
			throw new RuntimeException(e);
		}
	}

	protected DHTStorageAdapter createStorageAdapter(int network,
			DHTLogger logger, File storage_dir) {
		return new DHTPluginStorageManager(network, logger, storage_dir);
	}

	protected DHTLogger getLogger() {
		final PluginInterface plugin_interface = azureusCore.getPluginManager()
				.getDefaultPluginInterface();

		DHTLogger ret_logger = new DHTLogger() {
			public void log(String str) {
				if (DHTLog.logging_on)
					System.err.println(str);// LOG.debug(str);
			}

			public void log(Throwable e) {
				if (DHTLog.logging_on)
					System.err.println(e.getMessage());// LOG.error("", e);
			}

			public void log(int log_type, String str) {
				if (isEnabled(log_type))
					System.err.println(str);// LOG.debug(str);
			}

			public boolean isEnabled(int log_type) {
				return DHTLog.logging_on;
			}

			public PluginInterface getPluginInterface() {
				return plugin_interface;
			}
		};

		return ret_logger;
	}

	protected int getNewTimeout(boolean success) {
		if (success) { // Decrease
			current_udp_timeout_range_max = current_udp_timeout;
		} else { // Increase
			current_udp_timeout_range_min = current_udp_timeout;
			if (current_udp_timeout_range_max - current_udp_timeout_range_min <= 1) {
				current_udp_timeout_range_max = kDhtUDPOperationTimeoutMax_ms;
			}
		}
		int new_udp_timeout = (current_udp_timeout_range_min + current_udp_timeout_range_max) / 2;
		return new_udp_timeout;
	}

	/**
	 * CAlled only while holding writerlock on dht_lock.
	 */
	private void resetTimeoutInterval() {
		current_udp_timeout_range_max = kDhtUDPOperationTimeoutMax_ms;
		current_udp_timeout_range_min = kDhtUDPOperationTimeoutMin_ms;
	}

	private class TimeoutRecalibrationTask extends TimerTask {
		@Override
		public void run() {
			LOG.info("DHTVanishBackend#TimeoutRecalibrationTask reenabling "
					+ "recalibration");
			dht_lock.writeLock().lock();
			resetTimeoutInterval();
			dht_lock.writeLock().unlock();
		}
	}

	// @Override
	public byte[] generateLocation(Random prg) {
		byte[] index = new byte[params.kDhtIndexLen];
		prg.nextBytes(index);
		return index;
	}

	// @Override
	public byte[] getID() {
		byte[] res = null;
		dht_lock.readLock().lock();
		res = dht.getControl().getRouter().getLocalContact().getID();
		dht_lock.readLock().unlock();
		return res;
	}

	// Protected to enable testing.
	protected void put(byte[] key, byte[] value, DHTOperationAdapter adapter) {
		dht.put(key, "", value, (byte) 0, adapter);
	}

	// Protected to enable testing.
	protected void get(byte[] key, DHTOperationAdapter adapter) {
		dht.get(key, "", (byte) 0, 32, 60000, true, false, adapter);
	}

	// DHTNATPuncherInterface:

	// @Override
	@SuppressWarnings("unchecked")
	public Map getClientData(InetSocketAddress originator, Map originator_data) {
		Map res = new HashMap();
		res.put("udp_data_port", this.dht.getTransport().getPort());
		res.put("tcp_data_port", this.dht.getTransport().getPort());
		return res;
	}

	// RefreshableConfig interface:

	// @Override
	public DHTParams getRenewedParamsFromConfig() {
		final int kDhtIndexLen = 20;
		final int kNumTries = 1;
		final int kRetrySleep_ms = 0;
		/*
		 * final int kTimeoutWaitForShare_ms = config.getLong(
		 * Defaults.CONF_VANISH_BACKEND_VUZEIMPL_SHARETIMEOUT_MS, 10 *
		 * kDhtUDPOperationTimeoutMax_ms);
		 */

		final int kOperationBulkSize = 30;

		final long kTimeoutBetweenOperationBulks = 500L;

		final int kNumTriesIntegrate = 10;

		final long kIntegrateInterval_ms = 10000L;

		DHTParams params = new DHTParams(kDhtIndexLen, kNumTries,
				kRetrySleep_ms, kOperationBulkSize,
				kTimeoutBetweenOperationBulks, kNumTriesIntegrate,
				kIntegrateInterval_ms);

		// validate(params);
		return params;
	}

	// @Override
	public void setParams(DHTParams params) {
		this.params = params;
	}

	public static void main(String[] args) throws Exception {
		boolean booting = false;
		boolean churn = false;
		int port = 0;
		String bootstrap = InetAddress.getLocalHost().getHostName();

		int i = 0;
		if (args.length > i && "-b".equals(args[i])) {
			booting = true;
			++i;
		}
		if (args.length > i) {
			bootstrap = args[i];
		}
		++i;
		if (args.length > i) {
			port = Integer.parseInt(args[i]);
		}
		++i;
		if (args.length > i && "-c".equals(args[i])) {
			churn = true;
		}
		if (booting) {
			ActivePeer peer = new ActivePeer(port, bootstrap);
			peer.init(null);
			while (true) {
				Thread.sleep(10000);
			}
		} else {
			port = new ServerSocket(port).getLocalPort();
			ActivePeer peer = new ActivePeer(port, bootstrap, false);
			System.out.println(port);
			peer.init(null);
			long timeout = 10000;
			while (true) {
				Thread.sleep(timeout);
				if (churn && Math.random() < 0.002) {
					break;
				}
			}
			peer.stop();
		}
		System.exit(0);
	}
}
