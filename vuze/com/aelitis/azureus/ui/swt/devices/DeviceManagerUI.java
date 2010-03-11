/*
 * Created on Jan 27, 2009
 * Created by Paul Gardner
 * 
 * Copyright 2009 Vuze, Inc.  All rights reserved.
 * 
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; version 2 of the License only.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307, USA.
 */


package com.aelitis.azureus.ui.swt.devices;



import java.io.File;
import java.net.InetAddress;
import java.util.*;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.*;

import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.util.*;
import org.gudy.azureus2.plugins.PluginInterface;
import org.gudy.azureus2.plugins.disk.DiskManagerFileInfo;
import org.gudy.azureus2.plugins.download.Download;
import org.gudy.azureus2.plugins.installer.PluginInstaller;
import org.gudy.azureus2.plugins.installer.StandardPlugin;
import org.gudy.azureus2.plugins.ui.*;
import org.gudy.azureus2.plugins.ui.config.*;
import org.gudy.azureus2.plugins.ui.menus.*;
import org.gudy.azureus2.plugins.ui.menus.MenuItem;
import org.gudy.azureus2.plugins.ui.model.BasicPluginConfigModel;
import org.gudy.azureus2.plugins.ui.sidebar.*;
import org.gudy.azureus2.plugins.ui.tables.TableContextMenuItem;
import org.gudy.azureus2.plugins.ui.tables.TableManager;
import org.gudy.azureus2.plugins.ui.tables.TableRow;
import org.gudy.azureus2.pluginsimpl.local.PluginInitializer;
import org.gudy.azureus2.ui.swt.*;
import org.gudy.azureus2.ui.swt.plugins.UISWTInputReceiver;
import org.gudy.azureus2.ui.swt.plugins.UISWTInstance;
import org.gudy.azureus2.ui.swt.shells.CoreWaiterSWT;
import org.gudy.azureus2.ui.swt.shells.MessageBoxShell;
import org.gudy.azureus2.ui.swt.views.AbstractIView;
import org.gudy.azureus2.ui.swt.views.utils.ManagerUtils;

import com.aelitis.azureus.core.AzureusCore;
import com.aelitis.azureus.core.AzureusCoreFactory;
import com.aelitis.azureus.core.AzureusCoreRunningListener;
import com.aelitis.azureus.core.devices.*;
import com.aelitis.azureus.core.devices.DeviceManager.DeviceManufacturer;
import com.aelitis.azureus.core.devices.DeviceManager.UnassociatedDevice;
import com.aelitis.azureus.core.download.DiskManagerFileInfoFile;
import com.aelitis.azureus.core.messenger.config.PlatformDevicesMessenger;
import com.aelitis.azureus.ui.UIFunctions;
import com.aelitis.azureus.ui.UIFunctionsManager;
import com.aelitis.azureus.ui.UserPrompterResultListener;
import com.aelitis.azureus.ui.common.viewtitleinfo.ViewTitleInfo;
import com.aelitis.azureus.ui.common.viewtitleinfo.ViewTitleInfoManager;
import com.aelitis.azureus.ui.selectedcontent.ISelectedContent;
import com.aelitis.azureus.ui.selectedcontent.SelectedContentManager;
import com.aelitis.azureus.ui.swt.devices.add.DeviceTemplateChooser;
import com.aelitis.azureus.ui.swt.devices.add.ManufacturerChooser;
import com.aelitis.azureus.ui.swt.devices.add.DeviceTemplateChooser.DeviceTemplateClosedListener;
import com.aelitis.azureus.ui.swt.devices.add.ManufacturerChooser.ClosedListener;
import com.aelitis.azureus.ui.swt.toolbar.ToolBarEnabler;
import com.aelitis.azureus.ui.swt.toolbar.ToolBarEnablerSelectedContent;
import com.aelitis.azureus.ui.swt.views.skin.SkinView;
import com.aelitis.azureus.ui.swt.views.skin.SkinViewManager;
import com.aelitis.azureus.ui.swt.views.skin.SkinViewManager.SkinViewManagerListener;
import com.aelitis.azureus.ui.swt.views.skin.sidebar.SideBar;
import com.aelitis.azureus.ui.swt.views.skin.sidebar.SideBarEntrySWT;
import com.aelitis.azureus.ui.swt.views.skin.sidebar.SideBarListener;

public class 
DeviceManagerUI 
{
	// Not supported for Unix and OSX PPC
	public static boolean DISABLED;
	
	private static final int MIN_FILE_SIZE_FOR_XCODE	= 128*1024;
	private static final int MAX_FILES_FOR_MULTI_XCODE	= 64;
	
	private static final Object	DEVICE_IVIEW_KEY = new Object();
	
	private static final String CONFIG_VIEW_TYPE				= "device.sidebar.ui.viewtype";
	static final String CONFIG_VIEW_HIDE_REND_GENERIC	= "device.sidebar.ui.rend.hidegeneric";
	
	private static final String SPINNER_IMAGE_ID 	= "image.sidebar.vitality.dl";
	private static final String INFO_IMAGE_ID		= "image.sidebar.vitality.info";
	private static final String ALERT_IMAGE_ID		= "image.sidebar.vitality.alert";

	private static final boolean	SHOW_RENDERER_VITALITY 	= false;
	private static final boolean	SHOW_OD_VITALITY 		= true;
	
	private static final String[] to_copy_indicator_colors = { "#000000", "#000000", "#168866", "#1c5620" };
	
	private DeviceManager			device_manager;
	private DeviceManagerListener	device_manager_listener;
	
	private final PluginInterface	plugin_interface;
	private final UIManager			ui_manager;
	
	private UISWTInstance			swt_ui;
	
	private boolean		ui_setup;
	
	private SideBar		side_bar;
	private boolean		sidebar_built;
	
	private static final int SBV_SIMPLE		= 0;
	private static final int SBV_FULL		= 0x7FFFFFFF;
	
	private int			side_bar_view_type		= COConfigurationManager.getIntParameter( CONFIG_VIEW_TYPE, SBV_SIMPLE );
	private boolean		side_bar_hide_rend_gen	= COConfigurationManager.getBooleanParameter( CONFIG_VIEW_HIDE_REND_GENERIC, true );
	
	private int			next_sidebar_id;
		
	private List<categoryView>	categories = new ArrayList<categoryView>();
	
	
	private MenuItemListener properties_listener;
	private MenuItemListener hide_listener;
	private MenuItemListener rename_listener;
	
	private MenuItemFillListener	will_remove_listener;
	private MenuItemListener 		remove_listener;
	
	private MenuItemFillListener	show_fill_listener;
	private MenuItemListener 		show_listener;

	
	private MenuItemFillListener will_browse_listener;
	
	private boolean	offline_menus_setup;
	
	
	static {
		try {
  		if (Constants.isOSX) {
  			String arch = System.getProperty("os.arch", "");
  			DISABLED = arch.equalsIgnoreCase("powerpc") || arch.equalsIgnoreCase("ppc");
  		} else {
  			DISABLED = Constants.isUnix;
  		}
		} catch (Throwable t) {
			// Benefit of the doubt?
			DISABLED = false;
		}
	}
	
	public
	DeviceManagerUI(
		AzureusCore			core )
	{
		plugin_interface = PluginInitializer.getDefaultInterface();
		
		ui_manager = plugin_interface.getUIManager();

		if (DISABLED) {
			return;
		}

		ui_manager.addUIListener(
				new UIManagerListener()
				{
					public void
					UIAttached(
						UIInstance		instance )
					{
						if ( instance instanceof UISWTInstance ){
							
							swt_ui	= (UISWTInstance)instance;
							
							AzureusCoreFactory.addCoreRunningListener(
								new AzureusCoreRunningListener() 
								{
									public void 
									azureusCoreRunning(
										AzureusCore core )
									{
										uiAttachedAndCoreRunning(core);

									}
								});

						}
					}
					
					public void
					UIDetached(
						UIInstance		instance )
					{
					}
				});
	}

	private void uiAttachedAndCoreRunning(AzureusCore core) {
		Utils.execSWTThread(new AERunnable() {
			public void runSupport() {
				SideBar sideBar = (SideBar) SkinViewManager.getByClass(SideBar.class);
				
				if (sideBar != null) {
					
					setupUI(sideBar);
				} else {
					
					SkinViewManager.addListener(new SkinViewManagerListener() {
						public void skinViewAdded(SkinView skinview) {
							
							if (skinview instanceof SideBar) {
								
								setupUI((SideBar) skinview);
								SkinViewManager.RemoveListener(this);
							}
						}
					});
				}
			}
		});
		
		UIExitUtilsSWT.addListener(
			new UIExitUtilsSWT.canCloseListener()
			{
				public boolean 
				canClose() 
				{
					try {
  					final TranscodeJob job = device_manager.getTranscodeManager().getQueue().getCurrentJob();
  					
  					if ( job == null || job.getState() != TranscodeJob.ST_RUNNING ){
  						
  						return( true );
  					}

						String title = MessageText.getString("device.quit.transcoding.title");
						String text = MessageText.getString(
								"device.quit.transcoding.text",
								new String[] {
									job.getName(),
									job.getTarget().getDevice().getName(),
									String.valueOf( job.getPercentComplete())
								});

						MessageBoxShell mb = new MessageBoxShell(
								title,
								text,
								new String[] {
									MessageText.getString("UpdateWindow.quit"),
									MessageText.getString("Content.alert.notuploaded.button.abort")
								}, 1);
						mb.open(null);
						mb.waitUntilClosed();
						return mb.getResult() == 0;

					} catch (Exception e) {
						Debug.out(e);
						return true;
					}
				}
			});
	}


	protected DeviceManager
	getDeviceManager()
	{
		return( device_manager );
	}
	
	protected PluginInterface
	getPluginInterface()
	{
		return( plugin_interface );
	}
	
	protected void
	setupUI(
		SideBar			_side_bar )	
	{
		synchronized( this ){
			
			if ( ui_setup ){
				
				return;
			}
			
			ui_setup = true;
		}
		
		side_bar		= _side_bar;

		device_manager 	= DeviceManagerFactory.getSingleton();
		
		device_manager.addListener(new DeviceManagerListener() {
		
			public void deviceRemoved(Device device) {
			}
		
			public void deviceManagerLoaded() {
				device_manager.removeListener(this);
				Utils.execSWTThread(new AERunnable() {
					public void runSupport() {
						setupUIwithDeviceManager();
					}
				});
			}
		
			public void deviceChanged(Device device) {
			}
		
			public void deviceAttentionRequest(Device device) {
			}
		
			public void deviceAdded(Device device) {
			}
		});
	}

	
	private void
	setupUIwithDeviceManager()
	{
		device_manager_listener = 
			new DeviceManagerListener()
			{
				public void 
				deviceAdded(
					Device device ) 
				{
					addOrChangeDevice( device );
				}
				
				public void
				deviceChanged(
					Device		device )
				{
					addOrChangeDevice( device );
				}
				
				public void
				deviceAttentionRequest(
					Device		device )
				{
					showDevice( device );
				}
				
				public void
				deviceRemoved(
					Device		device )
				{
					removeDevice( device );
				}
				
				public void 
				deviceManagerLoaded() {
				}
			};
		
			TranscodeManager transMan = device_manager.getTranscodeManager();
			TranscodeQueue transQ = transMan.getQueue();
			transQ.addListener(
			new TranscodeQueueListener()
			{
				int	last_job_count = 0;
				
				public void
				jobAdded(
					TranscodeJob		job )
				{
					check();
				}
				
				public void
				jobChanged(
					TranscodeJob		job )
				{
					check();
				}
				
				public void
				jobRemoved(
					TranscodeJob		job )
				{
					check();
				}
				
				protected void
				check()
				{
					int job_count = device_manager.getTranscodeManager().getQueue().getJobCount();
					
					if ( job_count != last_job_count ){
						
						if ( job_count == 0 || last_job_count == 0 ){
													
							SideBarEntrySWT main_sb_entry = SideBar.getEntry( SideBar.SIDEBAR_SECTION_DEVICES );
	
							if ( main_sb_entry != null ){
						
								ViewTitleInfoManager.refreshTitleInfo( main_sb_entry.getTitleInfo());
							}
						}
						
						last_job_count = job_count;
					}
				}
			});
		
		setupListeners();
		
		buildSideBar( false );
		
		BasicPluginConfigModel configModel = ui_manager.createBasicPluginConfigModel(
				ConfigSection.SECTION_ROOT, "Devices");

			// auto search
		
		final BooleanParameter as = 
			configModel.addBooleanParameter2( 
				"device.search.auto", "device.search.auto",
				device_manager.getAutoSearch());
		
		as.addListener(
			new ParameterListener()
			{
				public void 
				parameterChanged(
					Parameter param) 
				{
					device_manager.setAutoSearch( as.getValue());
					
					if ( device_manager.getAutoSearch()){
						
						search();
					}
				}
			});
		
		final BooleanParameter qosParam = configModel.addBooleanParameter2(
				PlatformDevicesMessenger.CFG_SEND_QOS, "devices.turnon.qos", false);
		
			// send qos
		
		qosParam.setValue(COConfigurationManager.getBooleanParameter( PlatformDevicesMessenger.CFG_SEND_QOS, false));
		
		qosParam.addListener(
			new ParameterListener() {
				public void parameterChanged(Parameter param) {
					COConfigurationManager.setParameter(
							PlatformDevicesMessenger.CFG_SEND_QOS, qosParam.getValue());
				}
			});

			// config - simple view
		
		final BooleanParameter config_simple_view = 
			configModel.addBooleanParameter2( 
				CONFIG_VIEW_TYPE, "devices.sidebar.simple",
				side_bar_view_type == SBV_SIMPLE );
		
		config_simple_view.addListener(
			new ParameterListener()
			{
				public void 
				parameterChanged(
					Parameter param) 
				{
					COConfigurationManager.setParameter( CONFIG_VIEW_TYPE, config_simple_view.getValue()?SBV_SIMPLE:SBV_FULL );
				}
			});	
		
		COConfigurationManager.addParameterListener(
			CONFIG_VIEW_TYPE,
			new org.gudy.azureus2.core3.config.ParameterListener()
			{
				public void 
				parameterChanged(String 
					parameterName ) 
				{
					config_simple_view.setValue( COConfigurationManager.getIntParameter( CONFIG_VIEW_TYPE, SBV_SIMPLE ) == SBV_SIMPLE );
				}
			});
			
			// generic devices
		
		configModel.addBooleanParameter2( 
				"!" + CONFIG_VIEW_HIDE_REND_GENERIC + "!", "devices.sidebar.hide.rend.generic",
				side_bar_hide_rend_gen );
		
		// transcoding
		
			// default dir
		
		String def = device_manager.getDefaultWorkingDirectory().getAbsolutePath();
		
		final DirectoryParameter def_work_dir = configModel.addDirectoryParameter2( "device.config.xcode.workdir", "device.config.xcode.workdir", def );
		
		def_work_dir.setValue( def );
		
		def_work_dir.addListener(
			new ParameterListener()
			{
				public void 
				parameterChanged(
					Parameter param ) 
				{
					device_manager.setDefaultWorkingDirectory(new File( def_work_dir.getValue()));
				}
			});
		
			// max xcode
		
		final IntParameter max_xcode = 
			configModel.addIntParameter2( 
				"device.config.xcode.maxbps", "device.config.xcode.maxbps",
				(int)(device_manager.getTranscodeManager().getQueue().getMaxBytesPerSecond()/1024), 
				0, Integer.MAX_VALUE );
		
		max_xcode.addListener(
			new ParameterListener()
			{
				public void 
				parameterChanged(
					Parameter param) 
				{
					device_manager.getTranscodeManager().getQueue().setMaxBytesPerSecond( max_xcode.getValue()*1024 );
				}
			});

			// itunes
		
		final ActionParameter btnITunes = configModel.addActionParameter2("devices.button.installitunes", "UpdateWindow.columns.install");
		btnITunes.setEnabled(false);
		AzureusCoreFactory.addCoreRunningListener(new AzureusCoreRunningListener() {
			public void azureusCoreRunning(AzureusCore core) {
				boolean hasItunes = core.getPluginManager().getPluginInterfaceByID(
						"azitunes") != null;
				btnITunes.setEnabled(!hasItunes);
			}
		});

		btnITunes.addListener(new ParameterListener() {
			public void parameterChanged(Parameter param) {
				CoreWaiterSWT.waitForCoreRunning(new AzureusCoreRunningListener() {
					public void azureusCoreRunning(AzureusCore core) {
						try {
							PluginInstaller installer = core.getPluginManager().getPluginInstaller();
							StandardPlugin itunes_plugin = installer.getStandardPlugin("azitunes");
							
							itunes_plugin.install(false);
							
						} catch (Throwable e) {
							
							Debug.printStackTrace(e);
						}
					}
				});
			}
		});
		
		configModel.createGroup(
			"device.xcode.group",
			new Parameter[]
			{
					def_work_dir, max_xcode, btnITunes
			});
		
			// rss
		
		final BooleanParameter rss_enable = 
			configModel.addBooleanParameter2( 
				"device.rss.enable", "device.rss.enable",
				device_manager.isRSSPublishEnabled());
		
		rss_enable.addListener(
			new ParameterListener()
			{
				public void 
				parameterChanged(
					Parameter param) 
				{
					device_manager.setRSSPublishEnabled( rss_enable.getValue());
				}
			});
				
		HyperlinkParameter rss_view = 
			configModel.addHyperlinkParameter2(
				"device.rss.view", device_manager.getRSSLink());
		
		rss_enable.addEnabledOnSelection( rss_view );
		
		configModel.createGroup(
			"device.rss.group",
			new Parameter[]
			{
					rss_enable, rss_view,
			});

			// offline downloaders
		
				// enable
		
		final DeviceOfflineDownloaderManager dodm = device_manager.getOfflineDownlaoderManager();
		
		final BooleanParameter od_enable = 
			configModel.addBooleanParameter2( 
				"device.od.enable", "device.od.enable",
				dodm.isOfflineDownloadingEnabled());
		
		od_enable.addListener(
			new ParameterListener()
			{
				public void 
				parameterChanged(
					Parameter param) 
				{
					dodm.setOfflineDownloadingEnabled( od_enable.getValue());
					
					rebuildSideBar();
				}
			});
		
				// auto manage
		
		final BooleanParameter od_auto_enable = 
			configModel.addBooleanParameter2( 
				"device.odauto.enable", "device.odauto.enable",
				dodm.getOfflineDownloadingIsAuto());
		
		od_auto_enable.addListener(
			new ParameterListener()
			{
				public void 
				parameterChanged(
					Parameter param) 
				{
					dodm.setOfflineDownloadingIsAuto( od_auto_enable.getValue());
				}
			});
		
				// private torrents
		
		final BooleanParameter od_pt_enable = 
			configModel.addBooleanParameter2( 
				"device.odpt.enable", "device.odpt.enable",
				dodm.getOfflineDownloadingIncludePrivate());
		
		od_pt_enable.addListener(
			new ParameterListener()
			{
				public void 
				parameterChanged(
					Parameter param) 
				{
					dodm.setOfflineDownloadingIncludePrivate( od_pt_enable.getValue());
				}
			});
		
		od_auto_enable.addEnabledOnSelection( od_pt_enable );
		
		configModel.createGroup(
			"device.od.group",
			new Parameter[]
			{
				od_enable, od_auto_enable, od_pt_enable,
			});
		
		final BooleanParameter tivo_enable = 
			configModel.addBooleanParameter2( 
				"device.tivo.enable", "device.tivo.enable", false );
		
		tivo_enable.setValue(device_manager.isTiVoEnabled());
		
		tivo_enable.addListener(
			new ParameterListener()
			{
				public void 
				parameterChanged(
					Parameter param) 
				{
					device_manager.setTiVoEnabled( tivo_enable.getValue());
					
					rebuildSideBar();
				}
			});
		
		addAllDevices();
	
		setupTranscodeMenus();
	}
	
	protected String
	getRSSLink(
		int		port )
	{
		return( "http://127.0.0.1:" + port + "/" );
	}
	
	protected void
	setupListeners()
	{
		properties_listener = 
			new MenuItemListener() 
			{
				public void 
				selected(
					MenuItem menu, 
					Object target) 
				{
					if (target instanceof SideBarEntry) {
						SideBarEntry info = (SideBarEntry) target;
						Device device = (Device)info.getDatasource();
					
						showProperties( device );
					}
				}
			};
		
		hide_listener = 
			new MenuItemListener() 
			{
				public void 
				selected(
					MenuItem menu, 
					Object target) 
				{
					if (target instanceof SideBarEntry){
						
						SideBarEntry info = (SideBarEntry) target;
						
						Device device = (Device)info.getDatasource();
					
						device.setHidden( true );
					}
				}
			};
			
		rename_listener = 
				new MenuItemListener() 
				{
					public void 
					selected(
						MenuItem menu, 
						Object target) 
					{
						if (target instanceof SideBarEntry){
							
							SideBarEntry info = (SideBarEntry) target;
							
							final Device device = (Device)info.getDatasource();
							
							UISWTInputReceiver entry = (UISWTInputReceiver)swt_ui.getInputReceiver();
							
							entry.setPreenteredText(device.getName(), false );
							
							entry.maintainWhitespace(false);
							
							entry.allowEmptyInput( false );
							
							entry.setLocalisedTitle(MessageText.getString("label.rename",
									new String[] {
								device.getName()
							}));
	
							entry.prompt(new UIInputReceiverListener() {
								public void UIInputReceiverClosed(UIInputReceiver entry) {
									if (!entry.hasSubmittedInput()) {
										return;
									}
									String input = entry.getSubmittedInput().trim();
									
									if ( input.length() > 0 ){
										
										device.setName( input );
									}
								}
							});		
						}
					}
				};

		will_remove_listener = 
				new MenuItemFillListener() 
				{
					public void 
					menuWillBeShown(
						MenuItem 	menu, 
						Object 		targets) 
					{
						Object[]	rows;
						
						if ( targets instanceof Object[] ){
							
							rows = (Object[])targets;
							
						}else{
							
							rows = new Object[]{ targets };
						}
						
						if ( rows.length > 0 && rows[0] instanceof SideBarEntry ){
													
							SideBarEntry info = (SideBarEntry)rows[0];
						
							Device device = (Device)info.getDatasource();
							
							menu.setEnabled( device.canRemove());
							
						}else{
							
							menu.setEnabled( false );
						}
					}
				};
				
		remove_listener = 
			new MenuItemListener() 
			{
				public void 
				selected(
					MenuItem menu, 
					Object target) 
				{
					if (target instanceof SideBarEntry){
						
						SideBarEntry info = (SideBarEntry) target;
						
						Device device = (Device)info.getDatasource();
					
						device.remove();
					}
				}
			};
			
		will_browse_listener = 
				new MenuItemFillListener() 
				{
					public void 
					menuWillBeShown(
						MenuItem 	menu, 
						Object 		targets) 
					{
						menu.removeAllChildItems();
				
						boolean	enabled = false;
						
						Object[]	rows;
						
						if ( targets instanceof Object[] ){
							
							rows = (Object[])targets;
							
						}else{
							
							rows = new Object[]{ targets };
						}
						
						if ( rows.length > 0 && rows[0] instanceof SideBarEntry ){
													
							SideBarEntry info = (SideBarEntry)rows[0];
						
							Device device = (Device)info.getDatasource();
					
							Device.browseLocation[] locs = device.getBrowseLocations();
							
							enabled = locs.length > 0;
							
							MenuManager menuManager = ui_manager.getMenuManager();

							for ( final Device.browseLocation loc: locs ){
							
								MenuItem loc_menu = menuManager.addMenuItem( menu, loc.getName());
								
								loc_menu.addListener(
									new MenuItemListener()
									{
										public void 
										selected(
											MenuItem 	menu,
											Object 		target ) 
										{
											Utils.launch( loc.getURL().toExternalForm());
										}
									});
							}
						}
						
						menu.setEnabled( enabled );
					}
				};
			
		show_listener = 
			new MenuItemListener() 
			{
				public void 
				selected(
					MenuItem menu, 
					Object target) 
				{
					if ( target instanceof SideBarEntry ){
						
						SideBarEntry info = (SideBarEntry)target;
												
						Object ds = info.getDatasource();
						
						if ( ds instanceof Device ){
							
								// shouldn't get here really as its hidden :)
							
							Device device = (Device)ds;
					
							device.setHidden( true );
							
						}else{
							
							int	category_type = ds==null?Device.DT_UNKNOWN:(Integer)ds;
							
							Device[] devices = device_manager.getDevices();
							
							for ( Device device: devices ){
								
								if ( 	category_type == Device.DT_UNKNOWN ||
										device.getType() == category_type && device.isHidden()){
									
									device.setHidden( false );
								}
							}
						}
					}
				}
			};
			
		show_fill_listener = 
				new MenuItemFillListener() 
				{
					public void 
					menuWillBeShown(
						MenuItem 	menu, 
						Object 		targets) 
					{
						boolean	enabled = false;
						
						Object[]	rows;
						
						if ( targets instanceof Object[] ){
							
							rows = (Object[])targets;
							
						}else{
							
							rows = new Object[]{ targets };
						}
						
						for ( Object row: rows ){
							
							if ( row instanceof SideBarEntry ){
								
								SideBarEntry info = (SideBarEntry)row;
														
								Object ds = info.getDatasource();
								
								if ( ds instanceof Device ){
																	
								}else{
									
									int	category_type = ds==null?Device.DT_UNKNOWN:(Integer)ds;
										
									Device[] devices = device_manager.getDevices();
									
									for ( Device device: devices ){
										
										if ( 	category_type == Device.DT_UNKNOWN ||
												device.getType() == category_type && device.isHidden()){
											
											if ( device.isHidden()){
												
												enabled = true;
											}
										}
									}
								}
							}
						}
						
						menu.setEnabled( enabled );
					}
			
				};
		side_bar.addListener(
			new SideBarListener()
			{
				public void 
				sidebarItemSelected(
					SideBarEntrySWT new_entry,
					SideBarEntrySWT old_entry	)
				{
					Object data_source = new_entry.getDatasource();
					
					if ( data_source instanceof Device ){
						
						final Device	device = (Device)data_source;
						
						ISelectedContent[] sels = {
							new ToolBarEnablerSelectedContent( 
								new ToolBarEnabler()
								{
									public boolean 
									isEnabled(
										String itemKey )
									{
										return( "remove".equals(itemKey));
									}
									  
									public boolean 
									isSelected(
										String itemKey )
									{
										return( false );
									}
									  
									public void 
									itemActivated(
										String itemKey )
									{
										MessageBoxShell mb = 
											new MessageBoxShell(
												MessageText.getString("message.confirm.delete.title"),
												MessageText.getString("message.confirm.delete.text",
														new String[] {
															device.getName()
														}), 
												new String[] {
													MessageText.getString("Button.yes"),
													MessageText.getString("Button.no")
												},
												1 );
										
										mb.open(new UserPrompterResultListener() {
											public void prompterClosed(int result) {
												if (result == 0) {
													device.remove();
												}
											}
										});
									}
								})};
										
						SelectedContentManager.changeCurrentlySelectedContent("IconBarEnabler", sels );
					}
				}
			});
		
		COConfigurationManager.addAndFireParameterListeners( 
			new String[]{
				CONFIG_VIEW_TYPE,
				CONFIG_VIEW_HIDE_REND_GENERIC,
			},
			new org.gudy.azureus2.core3.config.ParameterListener()
			{
				public void 
				parameterChanged(
					String parameterName )
				{
					side_bar_view_type = COConfigurationManager.getIntParameter( CONFIG_VIEW_TYPE, SBV_SIMPLE );
					
					side_bar_hide_rend_gen = COConfigurationManager.getBooleanParameter( CONFIG_VIEW_HIDE_REND_GENERIC, true );

					rebuildSideBar();
				}
			});
	}
	
	protected static void
	hideIcon(
		SideBarVitalityImage	x )
	{
		if ( x == null ){
			return;
		}
		
		x.setVisible( false );
		x.setToolTip( "" );
	}
	
	protected static void
	showIcon(
		SideBarVitalityImage	x ,
		String					t )
	{
		if ( x == null ){
			return;
		}
		
		x.setToolTip( t );
		x.setVisible( true );
	}
	
	protected void
	rebuildSideBar()
	{
		if ( sidebar_built ){
			
			Utils.execSWTThread(
					new Runnable()
					{
						public void
						run()
						{
							removeAllDevices();
							
							buildSideBar( true );
							
							addAllDevices();
						}
					});
		}
	}
	
	protected void
	buildSideBar(
		boolean			rebuild )	
	{		
		final SideBarEntrySWT main_sb_entry = SideBar.getEntry( SideBar.SIDEBAR_SECTION_DEVICES );

		if ( main_sb_entry != null ){
				
			MenuManager menu_manager = ui_manager.getMenuManager();

			if ( !rebuild ){

				addDefaultDropListener( main_sb_entry );
				
				side_bar.createEntryFromSkinRef(null,
						SideBar.SIDEBAR_SECTION_DEVICES, "devicesview",
						MessageText.getString("devices.view.title"),
						null, null, false, -1);


				/* and away you go!
				SideBarVitalityImage addDevice = main_sb_entry.addVitalityImage("image.sidebar.subs.add");
				
				addDevice.setToolTip("Add Device");
				
				addDevice.addListener(
					new SideBarVitalityImageListener() 
					{
						public void 
						sbVitalityImage_clicked(
							int x, int y) 
						{
							addNewDevice();
							//new DevicesWizard( DeviceManagerUI.this );
						}
					});
				*/
				
				if (device_manager.getTranscodeManager().getProviders().length == 0) {
					SideBarVitalityImage turnon = main_sb_entry.addVitalityImage("image.sidebar.turnon");
					turnon.addListener(new SideBarVitalityImageListener() {
						public void sbVitalityImage_clicked(int x, int y) {
							DevicesFTUX.ensureInstalled();
						}
					});
				}
				SideBarVitalityImage beta = main_sb_entry.addVitalityImage("image.sidebar.beta");
				beta.setAlignment(SWT.LEFT);
				
				main_sb_entry.setImageLeftID( "image.sidebar.devices" );
					
				
				main_sb_entry.setTitleInfo(
					new ViewTitleInfo() 
					{
						private int last_indicator = 0;
						
						SideBarVitalityImage spinner = main_sb_entry.addVitalityImage( SPINNER_IMAGE_ID );
						SideBarVitalityImage warning = main_sb_entry.addVitalityImage( ALERT_IMAGE_ID );
						SideBarVitalityImage info	 = main_sb_entry.addVitalityImage( INFO_IMAGE_ID );

						{
							hideIcon( spinner );
							hideIcon( warning );
							hideIcon( info );
						}
						
						public Object 
						getTitleInfoProperty(
							int propertyID ) 
						{
							boolean expanded = main_sb_entry.getTreeItem().getExpanded();
														
							if ( propertyID == TITLE_TEXT ){
								
								return MessageText.getString( "devices.view.title" );
								
							}else if ( propertyID == TITLE_INDICATOR_TEXT ){
																
								spinner.setVisible( !expanded && device_manager.isBusy());
								
								if ( !expanded ){
																	
									Device[] devices = device_manager.getDevices();
									
									last_indicator = 0;
									
									String all_errors = "";
									String all_infos = "";
									
									for ( Device device: devices ){
										
										String error = device.getError();
										
										if ( error != null ){
											
											all_errors += (all_errors.length()==0?"":"; ") + error;
										}
										
										String info = device.getInfo();
										
										if ( info != null ){
											
											all_infos += (all_infos.length()==0?"":"; ") + info;
										}
										
										if ( device instanceof DeviceMediaRenderer ){
									
											if ( SHOW_RENDERER_VITALITY ){
												
												DeviceMediaRenderer	renderer = (DeviceMediaRenderer)device;
												
												last_indicator += renderer.getCopyToDevicePending() + renderer.getCopyToFolderPending();
											}
										}else if ( device instanceof DeviceOfflineDownloader ){
											
											if ( SHOW_OD_VITALITY ){
												
												DeviceOfflineDownloader	dod = (DeviceOfflineDownloader)device;
												
												last_indicator += dod.getTransferingCount();
											}
										}
									}
									
									if ( all_errors.length() > 0 ){
										 
										hideIcon( info );
										
										showIcon( warning, all_errors );
										
									}else{
										
										hideIcon( warning );
										
										if ( all_infos.length() > 0 ){
										
											showIcon( info, all_infos );
																						
										}else{
										
											hideIcon( info );
										}
									}
									
									if ( last_indicator > 0 ){
																						
										return( String.valueOf( last_indicator ));
									}
								}else{
									
									hideIcon( warning );
									hideIcon( info );
								
								}
							}else if ( propertyID == TITLE_INDICATOR_COLOR ){
									
								/*
								if ( last_indicator > 0 ){
									
									if ( SHOW_VITALITY ){
										
										return( to_copy_indicator_colors );
									}
								}
								*/
							}

							return null;
						}
					});

					// devices
				
				String parentID = "sidebar." + SideBar.SIDEBAR_SECTION_DEVICES;
							
				MenuItem de_menu_item = menu_manager.addMenuItem( parentID, "device.search" );
			
				de_menu_item.addListener( 
						new MenuItemListener() 
						{
							public void 
							selected(
								MenuItem menu, Object target ) 
							{
								search();
							}
						});
				
					// show hidden
				
				de_menu_item = menu_manager.addMenuItem( parentID, "device.show" );

				de_menu_item.addListener( show_listener );
				de_menu_item.addFillListener( show_fill_listener );
				

					// simple
				
				de_menu_item = menu_manager.addMenuItem( parentID, "devices.sidebar.simple" );
				
				de_menu_item.setStyle( MenuItem.STYLE_CHECK );
								
				de_menu_item.addFillListener(
					new MenuItemFillListener()
					{
						public void 
						menuWillBeShown(
							MenuItem menu, 
							Object data) 
						{
							menu.setData( COConfigurationManager.getIntParameter( CONFIG_VIEW_TYPE, SBV_SIMPLE ) == SBV_SIMPLE );
						}
					});
				
				de_menu_item.addListener( 
						new MenuItemListener() 
						{
							public void 
							selected(
								MenuItem menu, Object target ) 
							{
								COConfigurationManager.setParameter( CONFIG_VIEW_TYPE, ((Boolean)menu.getData())?SBV_SIMPLE:SBV_FULL );
							}
						});
				
				de_menu_item = menu_manager.addMenuItem( parentID, "sep" );

				de_menu_item.setStyle( MenuItem.STYLE_SEPARATOR );
				
					// options 
				
				de_menu_item = menu_manager.addMenuItem( parentID, "ConfigView.title.short" );
				
				de_menu_item.addListener( 
						new MenuItemListener() 
						{
							public void 
							selected(
								MenuItem menu, Object target ) 
							{
						      	 UIFunctions uif = UIFunctionsManager.getUIFunctions();
						      	 
						      	 if ( uif != null ){
						      		 
						      		 uif.openView( UIFunctions.VIEW_CONFIG, "Devices" );
						      	 }
							}
						});

				if (Constants.isCVSVersion()) {
					de_menu_item = menu_manager.addMenuItem(parentID,
							"!(CVS Only)Show FTUX!");
					
					de_menu_item.addListener(new MenuItemListener(){
						public void selected(MenuItem menu, Object target) {
							DevicesFTUX.showForDebug();
						}
					});

				}
			}
			
			if ( rebuild ){
				
				for ( categoryView category: categories ){
					
					category.destroy();
				}
			}
			
			categories.clear();
			
			if ( side_bar_view_type == SBV_FULL ){
				
					// renderers
				
				categoryView renderers_category 		= addDeviceCategory( Device.DT_MEDIA_RENDERER, "device.renderer.view.title", "image.sidebar.device.renderer" );
				
				categories.add( renderers_category );
				
				MenuItem re_menu_item = menu_manager.addMenuItem( "sidebar." + renderers_category.getKey(), "device.show" );
	
				re_menu_item.addListener( show_listener );
				re_menu_item.addFillListener( show_fill_listener );
				
					// media servers
				
				categoryView media_servers_category	= addDeviceCategory( Device.DT_CONTENT_DIRECTORY, "device.mediaserver.view.title", "image.sidebar.device.mediaserver" );
					
				categories.add( media_servers_category );
				
				MenuItem ms_menu_item = menu_manager.addMenuItem( "sidebar." + media_servers_category.getKey(), "device.show" );
	
				ms_menu_item.addListener( show_listener );
				ms_menu_item.addFillListener( show_fill_listener );
				
				ms_menu_item = menu_manager.addMenuItem( "sidebar." + media_servers_category.getKey(), "device.mediaserver.configure");
				
				ms_menu_item.addListener( 
						new MenuItemListener() 
						{
							public void 
							selected(
								MenuItem menu, Object target ) 
							{
						      	 UIFunctions uif = UIFunctionsManager.getUIFunctions();
						      	 
						      	 if ( uif != null ){
						      		 
						      		 uif.openView( UIFunctions.VIEW_CONFIG, "upnpmediaserver.name" );
						      	 }
							}
						});
	
					// routers
				
				categoryView routers_category			= addDeviceCategory( Device.DT_INTERNET_GATEWAY, "device.router.view.title", "image.sidebar.device.router" );
				
				categories.add( routers_category );
				
				MenuItem rt_menu_item = menu_manager.addMenuItem( "sidebar." + routers_category.getKey(), "device.show" );
	
				rt_menu_item.addListener( show_listener );
				rt_menu_item.addFillListener( show_fill_listener );
				
				rt_menu_item = menu_manager.addMenuItem( "sidebar." + routers_category.getKey(), "device.router.configure" );
				
				rt_menu_item.addListener( 
						new MenuItemListener() 
						{
							public void 
							selected(
								MenuItem menu, Object target ) 
							{
						      	 UIFunctions uif = UIFunctionsManager.getUIFunctions();
						      	 
						      	 if ( uif != null ){
						      		 
						      		 uif.openView( UIFunctions.VIEW_CONFIG, "UPnP" );
						      	 }
							}
						});
				
					// offline downloaders
				
				if ( device_manager.getOfflineDownlaoderManager().isOfflineDownloadingEnabled()){
					
					categoryView od_category	= addDeviceCategory( Device.DT_OFFLINE_DOWNLOADER, "device.offlinedownloader.view.title", "image.sidebar.device.offlinedownloader" );
					
					categories.add( od_category );
				}
				
					// internet
				
				categoryView internet_category	= addDeviceCategory( Device.DT_INTERNET, "MainWindow.about.section.internet", "image.sidebar.device.internet" );
				
				categories.add( internet_category );
			}
		}
		
		sidebar_built = true;
	}
	
	
	/**
	 * 
	 *
	 * @since 4.1.0.5
	 */
	protected void addNewDevice() {
		ManufacturerChooser mfChooser = new ManufacturerChooser();
		mfChooser.open(new ClosedListener() {
			public void MfChooserClosed(DeviceManufacturer mf) {
				if (mf == null) {
					return;
				}
				DeviceTemplateChooser deviceTemplateChooser = new DeviceTemplateChooser(mf);
				
				deviceTemplateChooser.open(new DeviceTemplateClosedListener() {
					public void deviceTemplateChooserClosed(DeviceTemplate deviceTemplate) {
						if (deviceTemplate == null) {
							return;
						}

						Device device;
						try {
							device = deviceTemplate.createInstance(deviceTemplate.getName() + " test!" );
							device.requestAttention();
						} catch (DeviceManagerException e) {
							Debug.out(e);
						}

						/*  Don't really need to choose a profile now..
						TranscodeProfile[] profiles = null;// deviceTemplate.getTranscodeProfiles();
						new TranscodeChooser(profiles) {
							public void closed() {
								Utils.openMessageBox(null, 0, "CHOSE", "You chose "
										+ (selectedProfile == null ? "NULL"
												: selectedProfile.getName()));
								
							}
						};
						*/
					}
				});
			}
		});
	}

	private void 
	setupTranscodeMenus()
	{					
			// top level menus
				
		final String[] tables = {
				TableManager.TABLE_MYTORRENTS_INCOMPLETE,
				TableManager.TABLE_MYTORRENTS_INCOMPLETE_BIG,
				TableManager.TABLE_MYTORRENTS_COMPLETE,
				TableManager.TABLE_MYTORRENTS_COMPLETE_BIG,
				TableManager.TABLE_TORRENT_FILES,
				TableManager.TABLE_MYTORRENTS_UNOPENED,
				TableManager.TABLE_MYTORRENTS_UNOPENED_BIG,
				TableManager.TABLE_MYTORRENTS_ALL_BIG,
			};
		
		TableManager table_manager = plugin_interface.getUIManager().getTableManager();
		
		MenuItemFillListener	menu_fill_listener = 
			new MenuItemFillListener()
			{
				public void
				menuWillBeShown(
					MenuItem	menu,
					Object		_target )
				{
					final TableRow[]	target;
					
					if ( _target instanceof TableRow ){
						
						target = new TableRow[]{ (TableRow)_target };
						
					}else{
						
						target = (TableRow[])_target;
					}
					
					boolean	enabled = target.length > 0;
					
					for ( TableRow row: target ){
						
						Object obj = row.getDataSource();
					
						if ( obj instanceof Download ){
						
							Download download = (Download)obj;

							if ( download.getState() == Download.ST_ERROR ){
								
								enabled = false;
							}
						}else{
							
							DiskManagerFileInfo file = (DiskManagerFileInfo)obj;
							
							try{
								if ( file.getDownload().getState() == Download.ST_ERROR ){
								
									enabled = false;
								}
							}catch( Throwable e ){
								
								enabled = false;
							}
						}
					}
					
					menu.setEnabled( enabled );
					
					menu.removeAllChildItems();
					
					if ( enabled ){
						
						Device[] devices = device_manager.getDevices();
						
						int	devices_added = 0;
						
						for ( Device device: devices ){
							
							if ( device instanceof TranscodeTarget ){
								
								devices_added++;
								
								final TranscodeTarget renderer = (TranscodeTarget)device;
								
								TranscodeProfile[] profiles = renderer.getTranscodeProfiles();
								

								TableContextMenuItem device_item =
									plugin_interface.getUIManager().getTableManager().addContextMenuItem(
										(TableContextMenuItem)menu,
										"!" + device.getName() + (profiles.length==0?" (No Profiles)":"") + "!");
								
								device_item.setStyle( MenuItem.STYLE_MENU );
								
								if ( profiles.length == 0 ){
									
									device_item.setEnabled( false );
									
								}else{
									
									for ( final TranscodeProfile profile: profiles ){
										
										TableContextMenuItem profile_item =
											plugin_interface.getUIManager().getTableManager().addContextMenuItem(
												device_item,
												"!" + profile.getName() + "!");

										profile_item.addMultiListener(
											new MenuItemListener()
											{
												public void 
												selected(
													MenuItem 	menu,
													Object 		x ) 
												{													
													for ( TableRow row: target ){
														
														Object obj = row.getDataSource();
													
														try{
															if ( obj instanceof Download ){
															
																Download download = (Download)obj;
	
																addDownload( renderer, profile, -1, download );
																	
															}else{
																
																DiskManagerFileInfo file = (DiskManagerFileInfo)obj;
																
																addFile( renderer, profile, -1, file );
															}
														}catch( Throwable e ){
															
															Debug.out( e );
														}
													}
												}
											});
									}
								}
							}
						}
						
						if ( devices_added == 0 ){
						
							TableContextMenuItem device_item =
								plugin_interface.getUIManager().getTableManager().addContextMenuItem(
									(TableContextMenuItem)menu,
									"!(No Devices)!");
							
							device_item.setEnabled( false );

						}
					}
				}
			};
		
		// TUX TODO: make a table_manager.addContentMenuItem(Class forDataSourceType, String resourceKey)
		//           instead of forcing a loop like this
		for( String table: tables ){
				
			TableContextMenuItem menu = table_manager.addContextMenuItem(table, "devices.contextmenu.xcode" );
			
			menu.setStyle(TableContextMenuItem.STYLE_MENU);
		
			menu.addFillListener( menu_fill_listener );				
		}
	}
	
	private void 
	setupOfflineDownloadingMenus()
	{					
		final String[] tables = {
				TableManager.TABLE_MYTORRENTS_INCOMPLETE,
				TableManager.TABLE_MYTORRENTS_INCOMPLETE_BIG,
				TableManager.TABLE_MYTORRENTS_ALL_BIG,
			};
		
		TableManager table_manager = plugin_interface.getUIManager().getTableManager();
		
		final DeviceOfflineDownloaderManager dodm = device_manager.getOfflineDownlaoderManager();
		
		MenuItemFillListener	menu_fill_listener = 
			new MenuItemFillListener()
			{
				public void
				menuWillBeShown(
					MenuItem	menu,
					Object		_target )
				{
					menu.removeAllChildItems();

					if ( dodm.getOfflineDownloadingIsAuto()){
						
						menu.setEnabled( true );
						
						TableContextMenuItem auto_item =
							plugin_interface.getUIManager().getTableManager().addContextMenuItem(
								(TableContextMenuItem)menu,
								"devices.contextmenu.od.auto");
						
						auto_item.setEnabled( false );

						return;
					}
			
					final TableRow[]	target;
					
					if ( _target instanceof TableRow ){
						
						target = new TableRow[]{ (TableRow)_target };
						
					}else{
						
						target = (TableRow[])_target;
					}
										
					boolean	all_non_manual	= true;
					boolean all_manual		= true;
					
					final List<Download> downloads = new ArrayList<Download>();
					
					for ( TableRow row: target ){
						
						Object obj = row.getDataSource();
					
						if ( obj instanceof Download ){
						
							Download download = (Download)obj;

							downloads.add( download );
							
							if ( dodm.isManualDownload( download )){
								
								all_non_manual = false;
								
							}else{
								
								all_manual = false;
							}
						}
					}
					
					boolean	enabled = downloads.size() > 0;

					menu.setEnabled( enabled );
										
					if ( enabled ){
						
						TableContextMenuItem manual_item =
							plugin_interface.getUIManager().getTableManager().addContextMenuItem(
								(TableContextMenuItem)menu,
								"devices.contextmenu.od.enable" + (all_manual?"d":""));
						
						final boolean f_all_manual = all_manual;
						
						manual_item.setData( new Boolean( f_all_manual ));
						
						manual_item.setStyle( MenuItem.STYLE_CHECK );
						
						manual_item.addListener(
							new MenuItemListener()
							{
								public void
								selected(
									MenuItem			menu,
									Object 				target )
								{
									Download[] d = downloads.toArray( new Download[ downloads.size()]);
									
									if ( f_all_manual ){
										
										dodm.removeManualDownloads( d );
										
									}else{
										
										dodm.addManualDownloads( d );
									}
								}
							});
					}
				}
			};
		
		// TUX TODO: make a table_manager.addContentMenuItem(Class forDataSourceType, String resourceKey)
		//           instead of forcing a loop like this
			
		for( String table: tables ){
				
			TableContextMenuItem menu = table_manager.addContextMenuItem(table, "devices.contextmenu.od" );
			
			menu.setStyle(TableContextMenuItem.STYLE_MENU);
		
			menu.addFillListener( menu_fill_listener );				
		}
	}
	
	protected void
	search()
	{
      	device_manager.search(
      			10*1000,
      			new DeviceSearchListener()
      			{
      				public void 
      				deviceFound(
      					Device device ) 
      				{
      				}
      				
      				public void 
      				complete() 
      				{
      				}
      			});
	}
	
	protected void
	addOrChangeDevice(
		final Device		device )
	{
		int	type = device.getType();
		
		if ( !device_manager.getOfflineDownlaoderManager().isOfflineDownloadingEnabled() && type == Device.DT_OFFLINE_DOWNLOADER ){
			
			return;
		}
		
		String parent_key = null;
		
		if ( side_bar_view_type == SBV_FULL ){
			
			for ( categoryView view: categories ){
				
				if ( view.getDeviceType() == type ){
					
					parent_key = view.getKey();
					
					break;
				}
			}
		}else{
			
			if ( type != Device.DT_MEDIA_RENDERER && type != Device.DT_OFFLINE_DOWNLOADER ){
				
				return;
			}
			
			parent_key = SideBar.SIDEBAR_SECTION_DEVICES;
		}
		
		if ( parent_key == null ){
			
			Debug.out( "Unknown device type: " + device.getString());
			
			return;
		}
			
		boolean	hide_device = device.isHidden();
		
		if ( type == Device.DT_MEDIA_RENDERER && side_bar_hide_rend_gen ){
			
			DeviceMediaRenderer rend = (DeviceMediaRenderer)device;
			
			if ( rend.isGeneric()){
				
				hide_device = true;
			}
		}
		
		if ( hide_device ){
			
			removeDevice( device );
			
			return;
		}
				
		final String parent = parent_key;
		
		synchronized( this ){

			final deviceItem existing_di = (deviceItem)device.getTransientProperty( DEVICE_IVIEW_KEY );
			
			if (  existing_di == null ){
	
				if ( type == Device.DT_OFFLINE_DOWNLOADER ){

					if ( !offline_menus_setup ){
					
						offline_menus_setup = true;
					
						setupOfflineDownloadingMenus();
					}
					
					DeviceOfflineDownloader	dod = (DeviceOfflineDownloader)device;
					
					if ( !dod.hasShownFTUX()){
						
						try{
							new DevicesODFTUX( dod );
														
						}catch( Throwable e ){
							
							Debug.out( "Failed to show offline downloader FTUX", e );
						}
					}
				}
				
				if ( !device.isHidden()){
					
					final deviceItem new_di = new deviceItem();
					
					device.setTransientProperty( DEVICE_IVIEW_KEY, new_di );
					
					Utils.execSWTThread(
						new Runnable()
						{
							public void
							run()
							{
								synchronized( DeviceManagerUI.this ){
	
									if ( new_di.isDestroyed()){
										
										return;
									}
									
									deviceView view = new deviceView( parent, device );
									
									new_di.setView( view );
										
									String key = parent + "/" + device.getID() + ":" + nextSidebarID();

									final SideBarEntrySWT	entry;
									
									int	device_type = device.getType();
									
									if ( device_type == Device.DT_MEDIA_RENDERER ){

										entry = 
											side_bar.createEntryFromSkinRef(
												parent,
												key, "devicerendererview",
												device.getName(),
												view, null, false, -1);
										
										String id = null;
										
										int	species = ((DeviceMediaRenderer)device).getRendererSpecies();
										
										if ( species != DeviceMediaRenderer.RS_OTHER ){
										
											id = "image.sidebar.device." + species + ".small";
											
										}else{
											
											String	classification = device.getClassification();
											
											if ( classification.equals( "sony.PSP")){
												
												id = "psp";
												
											}else if ( classification.startsWith( "tivo.")){
												
												id = "tivo";
												
											}else{
												
												id = String.valueOf( species );
											}
											
											id = "image.sidebar.device." + id + ".small";
										}
										
										if ( id != null ){
										
											entry.setImageLeftID(id);
										}
										
									}else if ( device_type == Device.DT_OFFLINE_DOWNLOADER ){
										
										entry = 
											side_bar.createEntryFromSkinRef(
												parent,
												key, "devicesodview",
												device.getName(),
												view, null, false, -1);
										
											
										DeviceOfflineDownloader dod = (DeviceOfflineDownloader)device;
										
										String	id;
										
										String manufacturer = dod.getManufacturer();
										
										if ( manufacturer.toLowerCase().contains( "vuze" )){
											
											id = "vuze";

										}else if ( manufacturer.toLowerCase().contains( "belkin" )){
											
											id = "bel";
											
										}else{
											
											id = "other";
										}
										
										entry.setImageLeftID( "image.sidebar.device.od." + id + ".small" );

									}else{
										
										side_bar.createTreeItemFromIView(
												parent, 
												view,
												key, 
												device, 
												false, 
												false,
												false );

										entry = SideBar.getEntry( key );
									}
									
									entry.setDatasource( device );
									
									entry.setLogID(parent + "-" + device.getName());

									new_di.setTreeItem( entry.getTreeItem(), entry );
									
									setStatus( device, new_di );
																			
									if ( device instanceof TranscodeTarget ){
										
										entry.addListener(
											new SideBarDropListener()
											{
												public boolean 
												sideBarEntryDrop(
													SideBarEntry 		entry, 
													Object 				payload  )
												{
													return handleDrop((TranscodeTarget)device, payload );
												}
											});
									}
									
									final MenuManager menu_manager = ui_manager.getMenuManager();

									boolean	need_sep = false;
									
									if ( device instanceof TranscodeTarget ){
										
										need_sep = true;
										
	  									MenuItem explore_menu_item = menu_manager.addMenuItem("sidebar." + key, "v3.menu.device.exploreTranscodes");
	  									
	  									explore_menu_item.addListener(new MenuItemListener() {
	  										public void selected(MenuItem menu, Object target) {
	  							 				ManagerUtils.open( ((TranscodeTarget) device).getWorkingDirectory());
	  										}
	  									});
									}
									
									if ( device instanceof DeviceMediaRenderer ){
										
										need_sep = true;
											// filter view
										
										final DeviceMediaRenderer renderer = (DeviceMediaRenderer) device;
										
										if ( renderer.canFilterFilesView()){
											MenuItem filterfiles_menu_item = menu_manager.addMenuItem("sidebar." + key, "devices.xcode.only.show");
											filterfiles_menu_item.setStyle(MenuItem.STYLE_CHECK);
	
											filterfiles_menu_item.addFillListener(new MenuItemFillListener() {
												public void menuWillBeShown(MenuItem menu, Object data) {
													menu.setData(new Boolean(renderer.getFilterFilesView()));
												}
											});
											filterfiles_menu_item.addListener(new MenuItemListener() {
												public void selected(MenuItem menu, Object target) {
									 				renderer.setFilterFilesView( (Boolean) menu.getData());
												}
											});
										}
										
											// show cats
										
										if ( renderer.canShowCategories()){
											MenuItem showcat_menu_item = menu_manager.addMenuItem("sidebar." + key, "devices.xcode.show.cat");
											showcat_menu_item.setStyle(MenuItem.STYLE_CHECK);
	
											showcat_menu_item.addFillListener(new MenuItemFillListener() {
												public void menuWillBeShown(MenuItem menu, Object data) {
													menu.setData(new Boolean(renderer.getShowCategories()));
												}
											});
											showcat_menu_item.addListener(new MenuItemListener() {
												public void selected(MenuItem menu, Object target) {
									 				renderer.setShowCategories( (Boolean) menu.getData());
												}
											});
										}

											// cache files
										
										MenuItem alwayscache_menu_item = menu_manager.addMenuItem("sidebar." + key, "devices.always.cache");
										alwayscache_menu_item.setStyle(MenuItem.STYLE_CHECK);
	
										alwayscache_menu_item.addFillListener(new MenuItemFillListener() {
												public void menuWillBeShown(MenuItem menu, Object data) {
													menu.setData(new Boolean(renderer.getAlwaysCacheFiles()));
												}
											});
										alwayscache_menu_item.addListener(new MenuItemListener() {
												public void selected(MenuItem menu, Object target) {
									 				renderer.setAlwaysCacheFiles( (Boolean) menu.getData());
												}
											});
					
									}
									
									if ( need_sep ){
									
										menu_manager.addMenuItem("sidebar." + key, "1" ).setStyle( MenuItem.STYLE_SEPARATOR );
									}
									
									need_sep = false;
									
									if ( device instanceof DeviceMediaRenderer ){
	
										final DeviceMediaRenderer renderer = (DeviceMediaRenderer) device;

										if ( renderer.canCopyToFolder()){
											
											need_sep = true;
											
											MenuItem autocopy_menu_item = menu_manager.addMenuItem("sidebar." + key, "devices.xcode.autoCopy");
											autocopy_menu_item.setStyle(MenuItem.STYLE_CHECK);
											
											autocopy_menu_item.addFillListener(new MenuItemFillListener() {
												public void menuWillBeShown(MenuItem menu, Object data) {
													menu.setData(new Boolean(renderer.getAutoCopyToFolder()));
												}
											});
											autocopy_menu_item.addListener(new MenuItemListener() {
												public void selected(MenuItem menu, Object target) {
									 				renderer.setAutoCopyToFolder((Boolean) menu.getData());
												}
											});
													
											final MenuItem mancopy_menu_item = menu_manager.addMenuItem("sidebar." + key, "devices.xcode.mancopy");
											mancopy_menu_item.setStyle(MenuItem.STYLE_PUSH);
																			
											mancopy_menu_item.addListener(new MenuItemListener() {
												public void 
												selected(
													MenuItem menu, Object target) 
												{
													try{
														renderer.manualCopy();
														
													}catch( Throwable e ){
														
														Debug.out( e );
													}
												}
											});
											
											mancopy_menu_item.addFillListener(
												new MenuItemFillListener()
												{
													public void 
													menuWillBeShown(
														MenuItem menu, Object data )
													{
														boolean	enabled = false;
													
														if ( !renderer.getAutoCopyToFolder()){
															
															File target = renderer.getCopyToFolder();
															
															if ( target != null && target.exists()){
																
																enabled = renderer.getCopyToFolderPending() > 0;
															}
														}
														mancopy_menu_item.setEnabled( enabled );
													}
												});
											
											MenuItem setcopyto_menu_item = menu_manager.addMenuItem("sidebar." + key, "devices.xcode.setcopyto");
											setcopyto_menu_item.setStyle(MenuItem.STYLE_PUSH);
										
											
											setcopyto_menu_item.addListener(new MenuItemListener() {
												public void 
												selected(
													MenuItem menu, Object target) 
												{
													Shell shell = entry.getTreeItem().getDisplay().getActiveShell();
													
													DirectoryDialog dd = new DirectoryDialog( shell );
													
													File existing = renderer.getCopyToFolder();
													
													if ( existing != null ){
														
														dd.setFilterPath( existing.getAbsolutePath());
													}
													
													dd.setText( MessageText.getString( "devices.xcode.setcopyto.title" ));
													
													String	path = dd.open();
													
													if ( path != null ){
														
														renderer.setCopyToFolder( new File( path ));
													}
												}
											});


										}
										
										if ( renderer.canAutoStartDevice()){
											
											need_sep = true;
											
											MenuItem autostart_menu_item = menu_manager.addMenuItem("sidebar." + key, "devices.xcode.autoStart");
											autostart_menu_item.setStyle(MenuItem.STYLE_CHECK);
	
											autostart_menu_item.addFillListener(new MenuItemFillListener() {
												public void menuWillBeShown(MenuItem menu, Object data) {
													menu.setData(new Boolean(renderer.getAutoStartDevice()));
												}
											});
											autostart_menu_item.addListener(new MenuItemListener() {
												public void selected(MenuItem menu, Object target) {
									 				renderer.setAutoStartDevice((Boolean) menu.getData());
												}
											});
										}
										
										if ( renderer.canAssociate()){
											
											need_sep = true;
											
											final MenuItem menu_associate = menu_manager.addMenuItem(
													"sidebar." + key, "devices.associate");
											
											menu_associate.setStyle(MenuItem.STYLE_MENU);

											menu_associate.addFillListener(
												new MenuItemFillListener()
												{
													public void 
													menuWillBeShown(
														MenuItem menu, Object data )
													{
														menu_associate.removeAllChildItems();
														
														if ( renderer.isAlive()){
															
															InetAddress a = renderer.getAddress();
															
															String address = a==null?"":a.getHostAddress();
															
															MenuItem menu_none = menu_manager.addMenuItem(
																	menu_associate,
																	"!" + MessageText.getString( "devices.associate.already" ) + ": " + address + "!" );

															menu_none.setEnabled( false );
															
															menu_associate.setEnabled( true );
															
														}else{
															
															UnassociatedDevice[] unassoc = device_manager.getUnassociatedDevices();
															
															if ( unassoc.length == 0 ){

																menu_associate.setEnabled( false );
																
															}else{
																
																menu_associate.setEnabled( true );
																
																for ( final UnassociatedDevice un: unassoc ){
																	
																	MenuItem menu_un = menu_manager.addMenuItem(
																			menu_associate,
																			"!" + un.getAddress().getHostAddress() + ": " + un.getDescription() + "!");
																	
																	menu_un.addListener(
																		new MenuItemListener() 
																		{
																			public void 
																			selected(
																				MenuItem 	menu, 
																				Object 		target)
																			{
																				renderer.associate( un );
																			}
																		});
																}
															}
														}
													}
												});

										}
										
										TranscodeProfile[] transcodeProfiles = renderer.getTranscodeProfiles();
										
										if (transcodeProfiles.length > 0) {
											
											need_sep = true;
											
											MenuItem menu_default_profile = menu_manager.addMenuItem(
													"sidebar." + key, "v3.menu.device.defaultprofile");
											menu_default_profile.setStyle(MenuItem.STYLE_MENU);

											MenuItem menu_profile_none = menu_manager.addMenuItem(
												menu_default_profile, "option.askeverytime");
											menu_profile_none.setStyle(MenuItem.STYLE_RADIO);
											menu_profile_none.setData(Boolean.FALSE);
											menu_profile_none.addListener(new MenuItemListener() {
												public void selected(MenuItem menu, Object target) {
													renderer.setDefaultTranscodeProfile(null);
												}
											});
											menu_profile_none.addFillListener(new MenuItemFillListener() {
												public void menuWillBeShown(MenuItem menu, Object data) {
													TranscodeProfile profile = null;
													try {
														profile = renderer.getDefaultTranscodeProfile();
													} catch (TranscodeException e) {
													}
													menu.setData((profile == null) ? Boolean.TRUE
															: Boolean.FALSE);
												}
											});

											for (final TranscodeProfile profile : transcodeProfiles) {
												MenuItem menuItem = menu_manager.addMenuItem(
														menu_default_profile, "!" + profile.getName() + "!");
												menuItem.setStyle(MenuItem.STYLE_RADIO);
												menuItem.setData(Boolean.FALSE);
												menuItem.addListener(new MenuItemListener() {
													public void selected(MenuItem menu, Object target) {
														renderer.setDefaultTranscodeProfile(profile);
													}
												});
												menuItem.addFillListener(new MenuItemFillListener() {
													public void menuWillBeShown(MenuItem menu, Object data) {
														TranscodeProfile dprofile = null;
														try {
															dprofile = renderer.getDefaultTranscodeProfile();
														} catch (TranscodeException e) {
														}
														menu.setData((profile.equals(dprofile))
																? Boolean.TRUE : Boolean.FALSE);
													}
												});
											}
										}
										
										// publish to RSS feed
										
										if ( true ){
											
											need_sep = true;
											
											MenuItem rss_menu_item = menu_manager.addMenuItem("sidebar." + key, "devices.xcode.rsspub");
											rss_menu_item.setStyle(MenuItem.STYLE_CHECK);

											rss_menu_item.addFillListener(new MenuItemFillListener() {
												public void menuWillBeShown(MenuItem menu, Object data) {
													menu.setData(new Boolean(device_manager.isRSSPublishEnabled() && renderer.isRSSPublishEnabled()));
												}
											});
											rss_menu_item.addListener(new MenuItemListener() {
												public void selected(MenuItem menu, Object target) {
									 				renderer.setRSSPublishEnabled((Boolean) menu.getData());
												}
											});
											
											rss_menu_item.setEnabled( device_manager.isRSSPublishEnabled());
										}
									}

									if ( device instanceof DeviceOfflineDownloader ){

										final DeviceOfflineDownloader	dod = (DeviceOfflineDownloader)device;
										
										need_sep = true;
										
										MenuItem configure_menu_item = menu_manager.addMenuItem("sidebar." + key, "device.configure");
											

										configure_menu_item.addFillListener(new MenuItemFillListener() {
											public void menuWillBeShown(MenuItem menu, Object data) {
												menu.setEnabled( dod.isAlive());
											}
										});

										configure_menu_item.addListener(
											new MenuItemListener()
											{
												public void 
												selected(
													MenuItem 	menu,
													Object 		target ) 
												{
													try{
														new DevicesODFTUX( dod );
														
													}catch( Throwable e ){
														
														Debug.out( e );
													}
												}
											});
										
										MenuItem enabled_menu_item = menu_manager.addMenuItem("sidebar." + key, "devices.contextmenu.od.enable" );
										
										enabled_menu_item.setStyle(MenuItem.STYLE_CHECK);

										enabled_menu_item.addFillListener(new MenuItemFillListener() {
											public void menuWillBeShown(MenuItem menu, Object data) {
												menu.setData(new Boolean( dod.isEnabled()));
											}
										});
										
										enabled_menu_item.addListener(new MenuItemListener() {
											public void selected(MenuItem menu, Object target) {
								 				dod.setEnabled((Boolean) menu.getData());
											}
										});
									}
									
									if ( device.isBrowsable()){
										
										need_sep = true;
										
										MenuItem browse_menu_item = menu_manager.addMenuItem("sidebar." + key, "device.browse");
										
										browse_menu_item.setStyle( MenuItem.STYLE_MENU );
										
										browse_menu_item.addFillListener( will_browse_listener );
									}
																	
									
									if ( need_sep ){
									
										menu_manager.addMenuItem("sidebar." + key, "s2" ).setStyle( MenuItem.STYLE_SEPARATOR );
									}
									
									MenuItem rename_menu_item = menu_manager.addMenuItem("sidebar." + key, "MyTorrentsView.menu.rename" );
																	
									rename_menu_item.addListener( rename_listener );									
									
									MenuItem hide_menu_item = menu_manager.addMenuItem("sidebar." + key, "device.hide");
									
									hide_menu_item.addListener( hide_listener );
	
									MenuItem remove_menu_item = menu_manager.addMenuItem("sidebar." + key, "MySharesView.menu.remove");
									
									remove_menu_item.addFillListener( will_remove_listener );
 									
									remove_menu_item.addListener( remove_listener );

										// sep
									
									menu_manager.addMenuItem("sidebar." + key, "s3" ).setStyle( MenuItem.STYLE_SEPARATOR );
									
										// props
									
									MenuItem menu_item = menu_manager.addMenuItem("sidebar." + key,"Subscription.menu.properties");
									
									menu_item.addListener( properties_listener );
								}
							}
						});
				}
			}else{
				
				Utils.execSWTThread(
						new Runnable()
						{
							public void
							run()
							{
								ViewTitleInfoManager.refreshTitleInfo( existing_di.getView());
								
								setStatus( device, existing_di );
							}
						});
			}
		}
	}
	
	protected void
	showDevice(
		Device		device )
	{
		synchronized( this ){
			
			final deviceItem existing_di = (deviceItem)device.getTransientProperty( DEVICE_IVIEW_KEY );

			if ( existing_di != null ){
				
				Utils.execSWTThread(
						new Runnable()
						{
							public void
							run()
							{
								synchronized( DeviceManagerUI.this ){

									TreeItem ti = existing_di.getTreeItem();
									
									if ( ti != null ){
										
										TreeItem x = ti;
										
										while( x != null ){
											
											x.setExpanded( true );
											
											x = x.getParentItem();
										}
																				
										ti.getParent().setSelection( ti );
									}
								}
							}
						});
			}
		}
	}
	
	public static boolean
	handleDrop(
		TranscodeTarget			target,
		final Object			payload )
	{
		if (!(payload instanceof String[]) && !(payload instanceof String)) {
			return false;
		}
		TranscodeChooser deviceChooser = new TranscodeChooser(target) {
			
			public void 
			closed() 
			{
				if ( selectedTranscodeTarget != null && selectedProfile != null ){
					
					handleDrop(
						selectedTranscodeTarget, 
						selectedProfile, 
						payload,
						getTranscodeRequirement());
				}
			}
		};
		
		deviceChooser.show();
		return true;
	}

	protected static void
	addDownload(
		TranscodeTarget		target,
		TranscodeProfile 	profile,
		int					transcode_requirement,
		byte[]				hash )
	{
		try{
		
			addDownload( target, profile, transcode_requirement, AzureusCoreFactory.getSingleton().getPluginManager().getDefaultPluginInterface().getShortCuts().getDownload(hash));
			
		}catch( Throwable e ){
			
			Debug.out( e );
		}
	}
	
	protected static void
	addDownload(
		TranscodeTarget		target,
		TranscodeProfile 	profile,
		int					transcode_requirement,
		Download			download )
	{
			// we could use the primary file
			// int index = DownloadManagerEnhancer.getSingleton().getEnhancedDownload(hash).getPrimaryFile().getIndex();
			// DiskManagerFileInfo dm_file = plugin_interface.getShortCuts().getDownload(hash).getDiskManagerFileInfo()[index];
	
			// but lets just grab all files

		DiskManagerFileInfo[] dm_files = download.getDiskManagerFileInfo();
		
		int	num_added = 0;
		
		for ( DiskManagerFileInfo dm_file: dm_files ){
			
				// limit number of files we can add to avoid crazyness
			
			if ( num_added > MAX_FILES_FOR_MULTI_XCODE ){
				
				break;
			}
			
				// could be smarter here and check extension or whatever
			
			if ( dm_files.length == 1 || dm_file.getLength() >= MIN_FILE_SIZE_FOR_XCODE ){
				
				addFile( target, profile, transcode_requirement, dm_file );
				
				num_added++;
			}
		}
	}
	
	protected static void
	addFile(
		TranscodeTarget			target,
		TranscodeProfile 		profile,
		int						transcode_requirement,
		DiskManagerFileInfo		file )
	{
		try{
			DeviceManagerFactory.getSingleton().getTranscodeManager().getQueue().add(
				target,
				profile,
				file,
				transcode_requirement);
			
		}catch( Throwable e ){
			
			Debug.out( e );
		}
	}
	
	protected static void
	addDirectory(
		TranscodeTarget			target,
		TranscodeProfile 		profile,
		int						transcode_requirement,
		File					file )
	{
		if ( !file.isDirectory()){
			
			return;
		}
		
		File[]	files = file.listFiles();
		
		int	num_added = 0;
		
		for ( File f: files ){
		
			if ( num_added > MAX_FILES_FOR_MULTI_XCODE ){
				
				break;
			}
			
			if ( f.isDirectory()){
				
				continue;
			}
			
			if ( f.length() > MIN_FILE_SIZE_FOR_XCODE ){
				
				addFile( target, profile, transcode_requirement, f );
				
				num_added++;
			}
		}
	}
	
	protected static void
	addFile(
		TranscodeTarget			target,
		TranscodeProfile 		profile,
		int						transcode_requirement,
		File					file )
	{
		if ( file.exists() && file.isFile()){

			try{
				DeviceManagerFactory.getSingleton().getTranscodeManager().getQueue().add(
					target,
					profile,
					new DiskManagerFileInfoFile( file ),
					transcode_requirement );
				
			}catch( Throwable e ){
				
				Debug.out( e );
			}
		}else{
			
			Debug.out( "Drop to " + target.getDevice().getName() + " for " + file + " failed, file doesn't exist" );
		}
	}
	
	protected static void
	handleDrop(
		TranscodeTarget		target,
		TranscodeProfile 	profile,
		Object				payload,
		int					transcode_requirement )
	{
		if ( payload instanceof String[]){
			
			String[]	files = (String[])payload;
			
			for ( String file: files ){
			
				File f = new File( file );

				if ( f.isFile()){
				
					addFile( target, profile, transcode_requirement, f );
					
				}else{
					
					addDirectory( target, profile, transcode_requirement, f );
				}
			}
		}else if ( payload instanceof String ){
			
			String stuff = (String)payload;
			
			if ( stuff.startsWith( "DownloadManager\n" ) ||stuff.startsWith( "DiskManagerFileInfo\n" )){
				
				String[]	bits = stuff.split( "\n" );
				
				for (int i=1;i<bits.length;i++){
					
					String	hash_str = bits[i];
					
					int	pos = hash_str.indexOf(';');
					
					try{

						if ( pos == -1 ){
							
							byte[]	 hash = Base32.decode( bits[i] );
			
							addDownload( target, profile, transcode_requirement, hash );
										
						}else{
							
							String[] files = hash_str.split(";");
							
							byte[]	 hash = Base32.decode( files[0].trim());
							
							DiskManagerFileInfo[] dm_files = AzureusCoreFactory.getSingleton().getPluginManager().getDefaultPluginInterface().getShortCuts().getDownload(hash).getDiskManagerFileInfo();
							
							for (int j=1;j<files.length;j++){
								
								DiskManagerFileInfo dm_file = dm_files[Integer.parseInt(files[j].trim())];
								
								addFile( target, profile, transcode_requirement, dm_file );
							}
						}
					}catch( Throwable e ){
						
						Debug.out( "Failed to get download for hash " + bits[1] );
					}
				}
			}else if ( stuff.startsWith( "TranscodeFile\n" )){
				
				String[]	bits = stuff.split( "\n" );
				
				for (int i=1;i<bits.length;i++){
					
					File f = new File( bits[i] );

					if ( f.isFile()){
					
						addFile( target, profile, transcode_requirement, f );
					}
				}
			}
		}
	}
	
	protected void
	setStatus(
		Device			device,
		deviceItem		sbi )
	{
		sbi.setStatus( device );
	}
	
	protected void
	addAllDevices()
	{
		device_manager.addListener( device_manager_listener );
			
		Utils.execSWTThread(
				new Runnable()
				{
					public void
					run()
					{
						Device[] devices = device_manager.getDevices();
						
						Arrays.sort(
							devices,
							new Comparator<Device>()
							{
								public int 
								compare(
									Device o1, 
									Device o2) 
								{
									return( o1.getName().compareToIgnoreCase( o2.getName()));
								}
							});
						
						for ( Device device: devices ){
							
							addOrChangeDevice( device );
						}
					}
				});
	}
	
	protected void
	removeAllDevices()
	{
		device_manager.removeListener( device_manager_listener );

		Utils.execSWTThread(
				new Runnable()
				{
					public void
					run()
					{
						Device[] devices = device_manager.getDevices();
						
						for ( Device device: devices ){
							
							removeDevice( device );
						}
					}
				});
	}
	
	protected void
	removeDevice(
		final Device		device )
	{
		synchronized( this ){
			
			final deviceItem existing_di = (deviceItem)device.getTransientProperty( DEVICE_IVIEW_KEY );
			
			if ( existing_di != null ){
				
				device.setTransientProperty( DEVICE_IVIEW_KEY, null );
				
				existing_di.destroy();
			}
		}
	}
	
	protected categoryView
	addDeviceCategory(
		int			device_type,
		String		category_title,
		String		category_image_id )
	{
		String key = "Device_" + category_title + ":" + nextSidebarID();
		
		categoryView view;
		
		if ( device_type == Device.DT_INTERNET ){
			
			view = new DeviceInternetView( this, category_title );
					
		}else{
			
			view = new categoryViewGeneric( this, device_type, category_title );
		}
		
		TreeItem item = 
			side_bar.createTreeItemFromIView(
				SideBar.SIDEBAR_SECTION_DEVICES, 
				view,
				key, 
				new Integer( device_type ), 
				false, 
				false,
				true );

		SideBarEntrySWT	entry = SideBar.getEntry( key );

		addDefaultDropListener( entry );
		
		entry.setImageLeftID( category_image_id );
				
		view.setDetails( entry, item, key );
		
		return( view );
	}
	
	protected void
	addDefaultDropListener(
		SideBarEntrySWT		entry )
	{
		entry.addListener(
				new SideBarDropListener()
				{
					public boolean 
					sideBarEntryDrop(
						SideBarEntry 		entry, 
						Object 				payload  )
					{
						return handleDrop(null, payload);
					}
				});
	}
	
	protected void
	showProperties(
		Device		device )
	{
		String[][] props = device.getDisplayProperties();
		
		new PropertiesWindow( device.getName(), props[0], props[1] );
	}
	
	protected int
	nextSidebarID()
	{
		synchronized( this ){
			
			return( next_sidebar_id++ );
		}
	}
	
	protected abstract static class
	categoryView
		extends 	AbstractIView
		implements 	ViewTitleInfo
	{
		private DeviceManagerUI	ui;
		private int				device_type;
		private String			title;
			
		private TreeItem		tree_item;
		private String			key;
			
		private SideBarVitalityImage spinner;
		private SideBarVitalityImage warning;
		private SideBarVitalityImage info;
		
		private int				last_indicator;
		
		protected
		categoryView(
			DeviceManagerUI		_ui,
			int					_device_type,
			String				_title )
		{
			ui				= _ui;
			device_type		= _device_type;
			title			= _title;
		}
		
		protected void
		setDetails(
			SideBarEntrySWT	_entry,
			TreeItem		_ti,
			String			_key )
		{
			tree_item 	= _ti;
			key			= _key;
			
			spinner = _entry.addVitalityImage( SPINNER_IMAGE_ID );

			hideIcon( spinner );
			
			warning = _entry.addVitalityImage( ALERT_IMAGE_ID );

			hideIcon( warning );
			
			info = _entry.addVitalityImage( INFO_IMAGE_ID );

			hideIcon( info );
		}
		
		
		protected int
		getDeviceType()
		{
			return( device_type );
		}
		
		protected String
		getKey()
		{
			return( key );
		}
		
		protected String
		getTitle()
		{
			return( MessageText.getString( title ));
		}
		
		public Object 
		getTitleInfoProperty(
			int propertyID ) 
		{
			boolean expanded = tree_item != null && tree_item.getExpanded();
			
			if ( propertyID == TITLE_TEXT ){
				
				return( getTitle());
				
			}else if ( propertyID == TITLE_INDICATOR_TEXT ){
			
				if ( device_type == Device.DT_MEDIA_RENDERER || device_type == Device.DT_OFFLINE_DOWNLOADER ){ 
				
					if ( spinner != null ){
					
						spinner.setVisible( !expanded && ui.getDeviceManager().isBusy());
					}
					
					if ( !expanded ){
										
						Device[] devices = ui.getDeviceManager().getDevices();
						
						last_indicator = 0;
						
						String all_errors 	= "";
						String all_infos	= "";
						
						for ( Device device: devices ){
							
							String error = device.getError();
							
							if ( error != null ){
								
								all_errors += (all_errors.length()==0?"":"; ") + error;
							}
							
							String info = device.getInfo();
							
							if ( info != null ){
								
								all_infos += (all_infos.length()==0?"":"; ") + info;
							}
							
							if ( device instanceof DeviceMediaRenderer ){
						
								if ( SHOW_RENDERER_VITALITY ){
									
									DeviceMediaRenderer	renderer = (DeviceMediaRenderer)device;
									
									last_indicator += renderer.getCopyToDevicePending() + renderer.getCopyToFolderPending();
								}
							}else if ( device instanceof DeviceOfflineDownloader ){
								
								if ( SHOW_OD_VITALITY ){
									
									DeviceOfflineDownloader dod = (DeviceOfflineDownloader)device;
									
									last_indicator += dod.getTransferingCount();
								}
							}
						}
						
						if ( all_errors.length() > 0 ){
							 
							showIcon( warning, all_errors );
													
						}else{
							
							hideIcon( warning );
							
							if ( all_infos.length() > 0 ){
							
								showIcon( info, all_infos );
																			
							}else{
							
								hideIcon( info );
							}
						}
						
						if ( last_indicator > 0 ){
														
							return( String.valueOf( last_indicator ));
						}
					}else{
						
						hideIcon( warning );
						hideIcon( info );
					}
				}
			}else if ( propertyID == TITLE_INDICATOR_COLOR ){
				
				/*
				if ( last_indicator > 0 ){
				
					if ( SHOW_VITALITY ){
					
						return( to_copy_indicator_colors );
					}
				}
				*/
			}
			
			return null;
		}
		
		protected void
		destroy()
		{
			if ( Utils.isThisThreadSWT()){
				
				tree_item.dispose();
				
				delete();
				
			}else{
				
				Utils.execSWTThread(
						new Runnable()
						{
							public void
							run()
							{
								tree_item.dispose();
								
								delete();
							}
						});
			}
		}
	}
	
	protected static class
	categoryViewGeneric
		extends 	categoryView
	{
		private Composite		composite;
		
		protected
		categoryViewGeneric(
			DeviceManagerUI		_ui,
			int					_device_type,
			String				_title )
		{
			super( _ui, _device_type, _title );
		}
		
		public void 
		initialize(
			Composite parent_composite )
		{  
			composite = new Composite( parent_composite, SWT.NULL );
			
			FormLayout layout = new FormLayout();
			
			layout.marginTop	= 4;
			layout.marginLeft	= 4;
			layout.marginRight	= 4;
			layout.marginBottom	= 4;
			
			composite.setLayout( layout );

			FormData data = new FormData();
			data.left = new FormAttachment(0,0);
			data.right = new FormAttachment(100,0);
			data.top = new FormAttachment(composite,0);
			data.bottom = new FormAttachment(100,0);


			Label label = new Label( composite, SWT.NULL );
			
			label.setText( "Nothing to show for " + getTitle());
			
			label.setLayoutData( data );
		}
		
		public Composite 
		getComposite()
		{
			return( composite );
		}
		
		public void
		delete()
		{
			super.delete();
		}
	}
	
	protected static class
	deviceView
		extends 	AbstractIView
		implements 	ViewTitleInfo, TranscodeTargetListener
	{
		private String			parent_key;
		private Device			device;
		
		private Composite		parent_composite;
		private Composite		composite;
		
		private int last_indicator;

		protected
		deviceView(
			String			_parent_key,
			Device			_device )
		{
			parent_key	= _parent_key;
			device		= _device;
			
			if ( device instanceof DeviceMediaRenderer ){
				
				DeviceMediaRenderer	renderer = (DeviceMediaRenderer)device;

				renderer.addListener( this );
			}
		}
			
		public void 
		initialize(
			Composite _parent_composite )
		{  
			parent_composite	= _parent_composite;

			composite = new Composite( parent_composite, SWT.NULL );
			
			FormLayout layout = new FormLayout();
			
			layout.marginTop	= 4;
			layout.marginLeft	= 4;
			layout.marginRight	= 4;
			layout.marginBottom	= 4;
			
			composite.setLayout( layout );
			
			FormData data = new FormData();
			
			data.left 	= new FormAttachment(0,0);
			data.right 	= new FormAttachment(100,0);
			data.top 	= new FormAttachment(composite,0);
			data.bottom = new FormAttachment(100,0);


			Label label = new Label( composite, SWT.NULL );
			
			label.setText( "Nothing to show for " + device.getName());
			
			label.setLayoutData( data );
		}
		
		public Composite 
		getComposite()
		{
			return( composite );
		}
		
		public Object 
		getTitleInfoProperty(
			int propertyID ) 
		{		
			if ( propertyID == TITLE_TEXT ){
				
				return( getTitle());
				
			}else if ( propertyID == TITLE_INDICATOR_TEXT ){
				
				if ( device instanceof DeviceMediaRenderer ){
					
					if ( SHOW_RENDERER_VITALITY ){
					
						DeviceMediaRenderer	renderer = (DeviceMediaRenderer)device;
					
					
						last_indicator = renderer.getCopyToDevicePending() + renderer.getCopyToFolderPending();
					}
				}else if ( device instanceof DeviceOfflineDownloader ){
					
					if ( SHOW_OD_VITALITY ){
					
						DeviceOfflineDownloader	dod = (DeviceOfflineDownloader)device;
					
						last_indicator = dod.getTransferingCount();
					}
				}
				
				if ( last_indicator > 0 ){
										
					return( String.valueOf( last_indicator ));
				}
			}else if ( propertyID == TITLE_INDICATOR_COLOR ){
				/*	
				if ( last_indicator > 0 ){
						
					if ( SHOW_VITALITY ){
					
						return( to_copy_indicator_colors );
					}
				}
				*/
			}else if ( propertyID == TITLE_ACTIVE_STATE ){

				if ( device.isLivenessDetectable()){
				
					return( new Long( device.isAlive()?1:2 ));
				}
			}
			
			return null;
		}
		
		public String
		getTitle()
		{
			return( device.getName());
		}
		
		public void
		fileAdded(
			TranscodeFile		file )
		{	
		}
		
		public void
		fileChanged(
			TranscodeFile		file,
			int					type,
			Object				data )
		{
			if ( 	type == TranscodeTargetListener.CT_PROPERTY &&
					data == TranscodeFile.PT_COMPLETE ){
				
				refreshTitles();
			}
		}
		
		protected void
		refreshTitles()
		{
			ViewTitleInfoManager.refreshTitleInfo( this );

			String	key = parent_key;
			
			while( key != null ){
			
				SideBarEntrySWT parent = SideBar.getEntry( key );
			
				if ( parent != null ){
				
					ViewTitleInfoManager.refreshTitleInfo(parent.getTitleInfo());
					
					key = parent.getParentID();
				}
			}
		}
		
		public void
		fileRemoved(
			TranscodeFile		file )
		{	
		}
		
		public void
		delete()
		{
			super.delete();
			
			if ( device instanceof DeviceMediaRenderer ){
				
				DeviceMediaRenderer	renderer = (DeviceMediaRenderer)device;

				renderer.removeListener( this );
			}
		}
	}
	
	public class
	deviceItem
	{		
		private deviceView			view;
		private SideBarEntrySWT		sb_entry;
		private TreeItem			tree_item;
		private boolean				destroyed;
		
		private SideBarVitalityImage	warning;
		private SideBarVitalityImage	spinner;
		private SideBarVitalityImage	info;
		
		protected
		deviceItem()
		{
		}
		
		protected void
		setTreeItem(
			TreeItem		_tree_item,
			SideBarEntrySWT	_sb_entry )
		{
			tree_item	= _tree_item;
			sb_entry	= _sb_entry;
			
			warning = sb_entry.addVitalityImage( ALERT_IMAGE_ID );
			
			hideIcon( warning );
			
			spinner = sb_entry.addVitalityImage( SPINNER_IMAGE_ID );
			
			hideIcon( spinner );
			
			info = sb_entry.addVitalityImage( INFO_IMAGE_ID );
			
			hideIcon( info );
		}
		
		protected TreeItem
		getTreeItem()
		{
			return( tree_item );
		}
		
		protected SideBarEntrySWT
		getSideBarEntry()
		{
			return( sb_entry );
		}
		
		protected void
		setView(
			deviceView		_view )
		{
			view	= _view;
		}
		
		protected deviceView
		getView()
		{
			return( view );
		}
		
		protected void
		setStatus(
			Device	device )
		{
				// possible during initialisation, status will be shown again on complete
			
			if ( warning != null && info != null ){
							
				String error = device.getError();
				
				if ( error != null ){
				 
					hideIcon( info );
					
					warning.setToolTip( error );
					
					warning.setImageID( ALERT_IMAGE_ID );
					
					warning.setVisible( true );
					
				}else{
					
					hideIcon( warning );
					
					String info_str = device.getInfo();
					
					if ( info_str != null ){
						
						showIcon( info, info_str );
						
					}else{
						
						hideIcon( info );
					}
				}
			}
			
			if ( spinner != null ){
						
				spinner.setVisible( device.isBusy());
			}
			
			if ( view != null ){
				
				view.refreshTitles();
			}
		}
		
		protected boolean
		isDestroyed()
		{
			return( destroyed );
		}
		
		protected void
		destroy()
		{
			destroyed = true;
			
			Utils.execSWTThread(
					new Runnable()
					{
						public void
						run()
						{
							synchronized( DeviceManagerUI.this ){
								
								if ( tree_item != null && !tree_item.isDisposed()){
									
									tree_item.dispose();
								}
							}
							
							view.delete();
						}
					});
		}
		
		public void 
		activate() 
		{
			SideBar sideBar = (SideBar)SkinViewManager.getByClass(SideBar.class);
			
			if ( sideBar != null && sb_entry != null ){
				
				sideBar.showEntryByID(sb_entry.getId());
			}
		}
	}
}
