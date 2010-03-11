/*
 * Created on May 29, 2006 2:13:41 PM
 * Copyright (C) 2006 Aelitis, All Rights Reserved.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 *
 * AELITIS, SAS au capital de 46,603.30 euros
 * 8 Allee Lenotre, La Grille Royale, 78600 Le Mesnil le Roi, France.
 */
package com.aelitis.azureus.ui.swt;

import java.io.File;
import java.util.Iterator;
import java.util.List;

import org.eclipse.swt.widgets.Display;

import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.core3.global.GlobalManager;
import org.gudy.azureus2.core3.global.GlobalManagerDownloadRemovalVetoException;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.util.*;
import org.gudy.azureus2.plugins.PluginEvent;
import org.gudy.azureus2.plugins.utils.DelayedTask;
import org.gudy.azureus2.pluginsimpl.local.utils.UtilitiesImpl;
import org.gudy.azureus2.ui.common.util.UserAlerts;
import org.gudy.azureus2.ui.swt.*;
import org.gudy.azureus2.ui.swt.auth.AuthenticatorWindow;
import org.gudy.azureus2.ui.swt.auth.CertificateTrustWindow;
import org.gudy.azureus2.ui.swt.auth.CryptoWindow;
import org.gudy.azureus2.ui.swt.mainwindow.*;
import org.gudy.azureus2.ui.swt.networks.SWTNetworkSelection;
import org.gudy.azureus2.ui.swt.pluginsinstaller.InstallPluginWizard;
import org.gudy.azureus2.ui.swt.progress.ProgressWindow;
import org.gudy.azureus2.ui.swt.update.UpdateMonitor;
import org.gudy.azureus2.ui.swt.updater2.PreUpdateChecker;
import org.gudy.azureus2.ui.swt.updater2.SWTUpdateChecker;

import com.aelitis.azureus.core.*;
import com.aelitis.azureus.core.cnetwork.ContentNetwork;
import com.aelitis.azureus.core.messenger.ClientMessageContext;
import com.aelitis.azureus.core.messenger.PlatformMessenger;
import com.aelitis.azureus.core.messenger.config.PlatformConfigMessenger;
import com.aelitis.azureus.core.torrent.PlatformTorrentUtils;
import com.aelitis.azureus.core.util.CopyOnWriteList;
import com.aelitis.azureus.launcher.Launcher;
import com.aelitis.azureus.ui.IUIIntializer;
import com.aelitis.azureus.ui.InitializerListener;
import com.aelitis.azureus.ui.UIFunctionsManager;
import com.aelitis.azureus.ui.swt.browser.listener.*;
import com.aelitis.azureus.ui.swt.browser.msg.MessageDispatcherSWT;
import com.aelitis.azureus.ui.swt.content.RelatedContentUI;
import com.aelitis.azureus.ui.swt.devices.DeviceManagerUI;
import com.aelitis.azureus.ui.swt.shells.main.MainWindow;
import com.aelitis.azureus.ui.swt.subscriptions.SubscriptionManagerUI;
import com.aelitis.azureus.ui.swt.utils.UIMagnetHandler;
import com.aelitis.azureus.util.InitialisationFunctions;

/**
 * @author TuxPaper
 * @created May 29, 2006
 *
 */
public class Initializer
	implements IUIIntializer
{
	// Whether to initialize the UI before the core has been started
	private static boolean STARTUP_UIFIRST = System.getProperty("ui.startfirst", "1").equals("1");

	// Used in debug to find out how long initialization took
	public static final long startTime = System.currentTimeMillis();

	private static StartServer startServer;

	private final AzureusCore core;

	private final String[] args;

	private CopyOnWriteList listeners = new CopyOnWriteList();

	private AEMonitor listeners_mon = new AEMonitor("Initializer:l");

	private int curPercent = 0;

	private AESemaphore init_task = new AESemaphore("delayed init");

	private MainWindow mainWindow;
	
	private static Initializer lastInitializer;

	public static void main(final String args[]) {
		if (Launcher.checkAndLaunch(Initializer.class, args))
			return;

		if (System.getProperty("ui.temp") == null) {
			System.setProperty("ui.temp", "az3");
		}

		org.gudy.azureus2.ui.swt.Main.main(args);
	}

	/**
	 * Main Initializer.  Usually called by reflection
	 * @param core
	 * @param args
	 */
	public Initializer(AzureusCore core, boolean createSWTThreadAndRun,
			String[] args) {
		this.core = core;
		this.args = args;
		lastInitializer = this;

		if (createSWTThreadAndRun) {
			try {
				SWTThread.createInstance(this);
			} catch (SWTThreadAlreadyInstanciatedException e) {
				Debug.printStackTrace(e);
			}
		} else {

			initializePlatformClientMessageContext();
			new AEThread2("cleanupOldStuff", true) {
				public void run() {
					cleanupOldStuff();
				}
			}.start();

			PlatformConfigMessenger.login(ContentNetwork.CONTENT_NETWORK_VUZE, 0);
			// typically the caller will call run() now 
		}
	}
	
	private void cleanupOldStuff() {
		File v3Shares = new File(SystemProperties.getUserPath(), "v3shares");
		if (v3Shares.isDirectory()) {
			FileUtil.recursiveDeleteNoCheck(v3Shares);
		}
		File dirFriends = new File(SystemProperties.getUserPath(), "friends");
		if (dirFriends.isDirectory()) {
			FileUtil.recursiveDeleteNoCheck(dirFriends);
		}
		File dirMedia = new File(SystemProperties.getUserPath(), "media");
		if (dirMedia.isDirectory()) {
			FileUtil.recursiveDeleteNoCheck(dirMedia);
		}
		deleteConfig("v3.Friends.dat");
		deleteConfig("unsentdata.config");
		AzureusCoreFactory.addCoreRunningListener(new AzureusCoreRunningListener() {
			public void azureusCoreRunning(final AzureusCore core) {
				new AEThread2("cleanupOldStuff", true) {
					public void run() {
						GlobalManager gm = core.getGlobalManager();
						List dms = gm.getDownloadManagers();
						for (Object o : dms) {
							DownloadManager dm = (DownloadManager) o;
							if (dm != null) {
								String val = PlatformTorrentUtils.getContentMapString(
										dm.getTorrent(), "Ad ID");
								if (val != null) {
									try {
										gm.removeDownloadManager(dm, true, true);
									} catch (Exception e) {
									}
								}
							}
						}
					}
				}.start();
			}
		});
	}

	private void deleteConfig(String name) {
		try {
  		File file = new File(SystemProperties.getUserPath(), name);
  		if (file.exists()) {
  			file.delete();
  		}
		} catch (Exception e) {
		}
		try {
  		File file = new File(SystemProperties.getUserPath(), name + ".bak");
  		if (file.exists()) {
  			file.delete();
  		}
		} catch (Exception e) {
		}
	}

	public void runInSWTThread() {
		COConfigurationManager.setBooleanDefault("ui.startfirst", true);
		STARTUP_UIFIRST = STARTUP_UIFIRST
				&& COConfigurationManager.getBooleanParameter("ui.startfirst", true);
		
		if (!STARTUP_UIFIRST) {
			return;
		}

		// Ensure colors initialized
		Colors.getInstance();

		UIConfigDefaultsSWT.initialize();

		UIConfigDefaultsSWTv3.initialize(core);

		mainWindow = new MainWindow(Display.getDefault(), this);
	}

	public void run() {
		
		DelayedTask delayed_task = UtilitiesImpl.addDelayedTask( "SWT Initialisation", new Runnable()
				{
					public void
					run()
					{
						init_task.reserve();
					}
				});

		delayed_task.queueFirst();
		
		// initialise the SWT locale util
		long startTime = SystemTime.getCurrentTime();

		new LocaleUtilSWT(core);
		
		final Display display = SWTThread.getInstance().getDisplay();

		new UIMagnetHandler(core);
		
		if (!STARTUP_UIFIRST) {
			UIConfigDefaultsSWT.initialize();
			UIConfigDefaultsSWTv3.initialize(core);
		} else {
			COConfigurationManager.setBooleanDefault("Show Splash", false);
		}

		if (COConfigurationManager.getBooleanParameter("Show Splash")) {
			display.asyncExec(new AERunnable() {
				public void runSupport() {
					new SplashWindow(display, Initializer.this);
				}
			});
		}

		System.out.println("Locale Initializing took "
				+ (SystemTime.getCurrentTime() - startTime) + "ms");
		startTime = SystemTime.getCurrentTime();

		core.addListener(new AzureusCoreListener() {
			int fakePercent = Math.min(70, 100 - curPercent);

			long startTime = SystemTime.getCurrentTime();
			long lastTaskTimeSecs = startTime / 500;

			String sLastTask;

			public void reportCurrentTask(AzureusCoreOperation op, String currentTask) {
				if (op.getOperationType() != AzureusCoreOperation.OP_INITIALISATION) {
					return;
				}

				Initializer.this.reportCurrentTask(currentTask);

				long now = SystemTime.getCurrentTime();
				if (fakePercent > 0 && lastTaskTimeSecs != now / 200) {
					lastTaskTimeSecs = SystemTime.getCurrentTime() / 200;
					fakePercent--;
					Initializer.this.reportPercent(curPercent + 1);
				}

				if (sLastTask != null && !sLastTask.startsWith("Loading Torrent")) {
						
					long diff = now - startTime;
					if (diff > 10 && diff < 1000 * 60 * 5) {
						System.out.println(TimeFormatter.milliStamp() + "   Core: " + diff + "ms for activity between '" + sLastTask + "' and '" + currentTask + "'");
					}
					startTime = SystemTime.getCurrentTime();
				}
				sLastTask = currentTask;
				//System.out.println(currentTask);
			}

			public void reportPercent(AzureusCoreOperation op, int percent) {
				/*
				if (op.getOperationType() != AzureusCoreOperation.OP_INITIALISATION) {
					return;
				}
				if (percent == 100) {
					long now = SystemTime.getCurrentTime();
					long diff = now - startTime;
					if (diff > 10 && diff < 1000 * 60 * 5) {
						System.out.println("   Core: " + diff + "ms for " + sLastTask);
					}
				}
				*/
				// TODO Auto-generated method stub
			}

		});

		core.addLifecycleListener(new AzureusCoreLifecycleAdapter() {
			private GlobalManager gm;

			public void componentCreated(AzureusCore core,
					AzureusCoreComponent component) {
				Initializer.this.reportPercent(curPercent + 1);
				if (component instanceof GlobalManager) {
					reportCurrentTaskByKey("splash.initializePlugins");

					gm = (GlobalManager) component;

					InitialisationFunctions.earlyInitialisation(core);
				}
			}

			// @see com.aelitis.azureus.core.AzureusCoreLifecycleAdapter#started(com.aelitis.azureus.core.AzureusCore)
			public void started(AzureusCore core) {
				boolean	main_window_will_report_complete = false;
				
				try {
	
					InitialisationFunctions.lateInitialisation(core);
					if (gm == null) {
						return;
					}
	
					// Ensure colors initialized
					Colors.getInstance();
	
					Initializer.this.reportPercent(curPercent + 1);
					new UserAlerts(gm);
	
					reportCurrentTaskByKey("splash.initializeGui");
	
					Initializer.this.reportPercent(curPercent + 1);
					Cursors.init();
	
					Initializer.this.reportPercent(curPercent + 1);
					
					main_window_will_report_complete = true;
					
					if (STARTUP_UIFIRST) {
						mainWindow.init(core);
					} else {
						new MainWindow(core, Display.getDefault(), Initializer.this);
					}
					
					reportCurrentTaskByKey("splash.openViews");
	
					SWTUpdateChecker.initialize();
	
					PreUpdateChecker.initialize(core,
							COConfigurationManager.getStringParameter("ui"));
	
					UpdateMonitor.getSingleton(core); // setup the update monitor
	
					//Tell listeners that all is initialized :
					Alerts.initComplete();
	
					//Finally, open torrents if any.
					for (int i = 0; i < args.length; i++) {
	
						try {
							TorrentOpener.openTorrent(args[i]);
	
						} catch (Throwable e) {
	
							Debug.printStackTrace(e);
						}
					}
				}
				finally{
					
					if ( !main_window_will_report_complete ){
						init_task.release();
					}
				}

			}

			public void stopping(AzureusCore core) {
				Alerts.stopInitiated();
			}

			public void stopped(AzureusCore core) {
			}

			public boolean syncInvokeRequired() {
				return (true);
			}

			public boolean
			requiresPluginInitCompleteBeforeStartedEvent()
			{
				return( false );
			}
			
			public boolean stopRequested(AzureusCore _core)
					throws AzureusCoreException {
				return org.gudy.azureus2.ui.swt.mainwindow.Initializer.handleStopRestart(false);
			}

			public boolean restartRequested(final AzureusCore core) {
				return org.gudy.azureus2.ui.swt.mainwindow.Initializer.handleStopRestart(true);
			}

		});

		reportCurrentTaskByKey("splash.initializeCore");

		try{
			new SubscriptionManagerUI();
			
		}catch( Throwable e ){
			
			Debug.printStackTrace(e);
		}
		
		try{
			RelatedContentUI.getSingleton();
			
		}catch( Throwable e ){
			
			Debug.printStackTrace(e);
		}
		
		try{
			new DeviceManagerUI( core );
				
		}catch( Throwable e ){
				
			Debug.printStackTrace(e);
		}
		
		core.start();

		reportPercent(50);

		System.out.println("Core Initializing took "
				+ (SystemTime.getCurrentTime() - startTime) + "ms");
		startTime = SystemTime.getCurrentTime();

		reportCurrentTaskByKey("splash.initializeUIElements");

		// Ensure colors initialized
		Colors.getInstance();

		reportPercent(curPercent + 1);
		Alerts.init();

		reportPercent(curPercent + 1);
		ProgressWindow.register(core);

		reportPercent(curPercent + 1);
		new SWTNetworkSelection();

		reportPercent(curPercent + 1);
		new AuthenticatorWindow();
		new CryptoWindow();
		
		reportPercent(curPercent + 1);
		new CertificateTrustWindow();

		InstallPluginWizard.register(core, display);
	}

	public void stopIt(boolean isForRestart, boolean isCloseAreadyInProgress)
			throws AzureusCoreException {
		if (core != null && !isCloseAreadyInProgress) {

			if (isForRestart) {

				core.checkRestartSupported();
			}
		}

		try {

			//			Cursors.dispose();

			try {
				UIFunctionsManager.getUIFunctions().getUIUpdater().stopIt();
			} catch (Exception e) {
				Debug.out(e);
			}

			Utils.execSWTThread(new AERunnable() {
				public void runSupport() {
					SWTThread.getInstance().terminate();
				}
			});

		} finally {

			try{
				if ( core != null && !isCloseAreadyInProgress) {
	
					try {
						if (isForRestart) {
	
							core.restart();
	
						} else {
	
							long lStopStarted = System.currentTimeMillis();
							System.out.println("core.stop");
							core.stop();
							System.out.println("core.stop done in "
									+ (System.currentTimeMillis() - lStopStarted));
						}
					} catch (Throwable e) {
	
						// don't let any failure here cause the stop operation to fail
	
						Debug.out(e);
					}
				}
			}finally{
				
					// do this after closing core to minimise window when the we aren't 
					// listening and therefore another Azureus start can potentially get
					// in and screw things up
				
				if (startServer != null) {
					startServer.stopIt();
				}
			}
		}
	}

	// @see com.aelitis.azureus.ui.IUIIntializer#addListener(org.gudy.azureus2.ui.swt.mainwindow.InitializerListener)
	public void addListener(InitializerListener listener) {
		try {
			listeners_mon.enter();

			listeners.add(listener);
		} finally {

			listeners_mon.exit();
		}
	}

	// @see com.aelitis.azureus.ui.IUIIntializer#removeListener(org.gudy.azureus2.ui.swt.mainwindow.InitializerListener)
	public void removeListener(InitializerListener listener) {
		try {
			listeners_mon.enter();

			listeners.remove(listener);
		} finally {

			listeners_mon.exit();
		}
	}

	public void reportCurrentTask(String currentTaskString) {
		try {
			listeners_mon.enter();

			Iterator iter = listeners.iterator();
			while (iter.hasNext()) {
				InitializerListener listener = (InitializerListener) iter.next();
				try {
					listener.reportCurrentTask(currentTaskString);
				} catch (Exception e) {
					// ignore
				}
			}
		} finally {

			listeners_mon.exit();
		}
	}

	private void reportCurrentTaskByKey(String key) {
		reportCurrentTask(MessageText.getString(key));
	}
	
	public void increaseProgress() {
		if (curPercent < 100) {
			reportPercent(curPercent + 1);
		}
	}
	
	// @see com.aelitis.azureus.ui.IUIIntializer#abortProgress()
	public void abortProgress() {
		reportPercent(101);
	}

	public void reportPercent(int percent) {
		if (curPercent > percent) {
			return;
		}

		curPercent = percent;
		try {
			listeners_mon.enter();

			Iterator iter = listeners.iterator();
			while (iter.hasNext()) {
				InitializerListener listener = (InitializerListener) iter.next();
				try {
					listener.reportPercent(percent);
				} catch (Exception e) {
					// ignore
				}
			}

			if (percent > 100) {
				listeners.clear();
			}
		} finally {

			listeners_mon.exit();
		}
	}
	
	public void
	initializationComplete()
	{
		core.getPluginManager().firePluginEvent( PluginEvent.PEV_INITIALISATION_UI_COMPLETES );

		  new DelayedEvent( 
				  "SWTInitComplete:delay",
				  2500,
				  new AERunnable()
				  {
					  public void
					  runSupport()
					  {
					  	//System.out.println("Release Init. Task");
						  init_task.release();
					  }
				  });
	}

	/**
	 * 
	 *
	 * @since 3.0.5.3
	 */
	private void initializePlatformClientMessageContext() {
		ClientMessageContext clientMsgContext = PlatformMessenger.getClientMessageContext();
		if (clientMsgContext != null) {
			clientMsgContext.setMessageDispatcher(new MessageDispatcherSWT(clientMsgContext));
			clientMsgContext.addMessageListener(new TorrentListener());
			clientMsgContext.addMessageListener(new VuzeListener());
			clientMsgContext.addMessageListener(new DisplayListener(null));
			clientMsgContext.addMessageListener(new ConfigListener(null));
		}
	}

	public static Initializer getLastInitializer() {
		return lastInitializer;
	}
}
