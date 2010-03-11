/**
 * Created on Feb 24, 2009
 *
 * Copyright 2008 Vuze, Inc.  All rights reserved.
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
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307  USA 
 */

package com.aelitis.azureus.ui.swt.devices;

import java.io.File;
import java.net.URL;
import java.util.*;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.*;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;

import org.gudy.azureus2.core3.category.Category;
import org.gudy.azureus2.core3.category.CategoryManager;
import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.plugins.ui.UIManager;
import org.gudy.azureus2.plugins.ui.tables.TableColumn;
import org.gudy.azureus2.plugins.ui.tables.TableColumnCreationListener;
import org.gudy.azureus2.plugins.ui.tables.TableManager;
import org.gudy.azureus2.pluginsimpl.local.PluginInitializer;
import org.gudy.azureus2.ui.swt.*;
import org.gudy.azureus2.ui.swt.URLTransfer;
import org.gudy.azureus2.ui.swt.mainwindow.ClipboardCopy;
import org.gudy.azureus2.ui.swt.shells.MessageBoxShell;
import org.gudy.azureus2.ui.swt.views.table.TableViewSWTMenuFillListener;
import org.gudy.azureus2.ui.swt.views.table.impl.TableViewSWTImpl;
import org.gudy.azureus2.ui.swt.views.utils.ManagerUtils;

import com.aelitis.azureus.core.AzureusCore;
import com.aelitis.azureus.core.AzureusCoreFactory;
import com.aelitis.azureus.core.AzureusCoreRunningListener;
import com.aelitis.azureus.core.devices.*;
import com.aelitis.azureus.ui.UIFunctions;
import com.aelitis.azureus.ui.UIFunctionsManager;
import com.aelitis.azureus.ui.UserPrompterResultListener;
import com.aelitis.azureus.ui.common.table.*;
import com.aelitis.azureus.ui.common.updater.UIUpdatable;
import com.aelitis.azureus.ui.selectedcontent.SelectedContentManager;
import com.aelitis.azureus.ui.swt.columns.torrent.ColumnThumbnail;
import com.aelitis.azureus.ui.swt.devices.columns.*;
import com.aelitis.azureus.ui.swt.skin.SWTSkinButtonUtility;
import com.aelitis.azureus.ui.swt.skin.SWTSkinObject;
import com.aelitis.azureus.ui.swt.skin.SWTSkinObjectText;
import com.aelitis.azureus.ui.swt.skin.SWTSkinButtonUtility.ButtonListenerAdapter;
import com.aelitis.azureus.ui.swt.views.skin.InfoBarUtil;
import com.aelitis.azureus.ui.swt.views.skin.SkinView;
import com.aelitis.azureus.ui.swt.views.skin.SkinViewManager;
import com.aelitis.azureus.ui.swt.views.skin.sidebar.SideBar;
import com.aelitis.azureus.ui.swt.views.skin.sidebar.SideBarEntrySWT;

/**
 * @author TuxPaper
 * @created Feb 24, 2009
 *
 */
public class SBC_DevicesView
	extends SkinView
	implements TranscodeQueueListener, IconBarEnabler, UIUpdatable,
	TranscodeTargetListener
{
	public static final String TABLE_DEVICES = "Devices";

	public static final String TABLE_TRANSCODE_QUEUE = "TranscodeQueue";

	public static final String TABLE_DEVICE_LIBRARY = "DeviceLibrary";

	private static boolean columnsAdded = false;

	private DeviceManager device_manager;

	private TranscodeManager transcode_manager;

	private TranscodeQueue transcode_queue;

	private TableViewSWTImpl 	tvDevices;
	private DragSource 			dragSource;
	private DropTarget 			dropTarget;
	private int					drag_drop_line_start = -1;
	private TableRowCore[]		drag_drop_rows;

	
	private TableViewSWTImpl<TranscodeFile> tvFiles;

	private SideBarEntrySWT sidebarEntry;

	private Composite tableJobsParent;

	private Device device;

	private TranscodeTarget transTarget;

	// @see com.aelitis.azureus.ui.swt.views.skin.SkinView#skinObjectInitialShow(com.aelitis.azureus.ui.swt.skin.SWTSkinObject, java.lang.Object)
	public Object skinObjectInitialShow(SWTSkinObject skinObject, Object params) {
		AzureusCoreFactory.addCoreRunningListener(new AzureusCoreRunningListener() {
			public void azureusCoreRunning(AzureusCore core) {
				initColumns(core);
			}
		});

		device_manager = DeviceManagerFactory.getSingleton();

		transcode_manager = device_manager.getTranscodeManager();

		transcode_queue = transcode_manager.getQueue();

		SideBar sidebar = (SideBar) SkinViewManager.getByClass(SideBar.class);
		if (sidebar != null) {
			sidebarEntry = sidebar.getCurrentEntry();
			sidebarEntry.setIconBarEnabler(this);
			device = (Device) sidebarEntry.getDatasource();
		}

		if (device instanceof TranscodeTarget) {
			transTarget = (TranscodeTarget) device;
		}

		if (device == null) {
			new InfoBarUtil(skinObject, "devicesview.infobar", false,
					"DeviceView.infobar", "v3.deviceview.infobar") {
				public boolean allowShow() {
					return true;
				}
			};
		} else if (device instanceof DeviceMediaRenderer) {
			DeviceMediaRenderer renderer = (DeviceMediaRenderer) device;
			int species = renderer.getRendererSpecies();
			String speciesID = null;
			switch (species) {
				case DeviceMediaRenderer.RS_ITUNES:
					speciesID = "itunes";
					break;
				case DeviceMediaRenderer.RS_PS3:
					speciesID = "ps3";
					break;
				case DeviceMediaRenderer.RS_XBOX:
					speciesID = "xbox";
					break;
				case DeviceMediaRenderer.RS_OTHER:{
					String classification = renderer.getClassification();
					
					if ( classification.equals( "sony.PSP")){
						speciesID = "psp";
					}else if ( classification.startsWith( "tivo.")){
						speciesID = "tivo";
					}
				}
				default:
					break;
			}

			if (speciesID != null) {
				final String fSpeciesID = speciesID;
				new InfoBarUtil(skinObject, "devicesview.infobar", false,
						"DeviceView.infobar." + speciesID, "v3.deviceview.infobar") {
					public boolean allowShow() {
						return true;
					}

					// @see com.aelitis.azureus.ui.swt.views.skin.InfoBarUtil#created(com.aelitis.azureus.ui.swt.skin.SWTSkinObject)
					protected void created(SWTSkinObject parent) {
						SWTSkinObjectText soLine1 = (SWTSkinObjectText) skin.getSkinObject(
								"line1", parent);
						soLine1.setTextID("v3.deviceview.infobar.line1.generic",
								new String[] {
									device.getName()
								});
						SWTSkinObjectText soLine2 = (SWTSkinObjectText) skin.getSkinObject(
								"line2", parent);
						soLine2.setTextID("v3.deviceview.infobar.line2." + fSpeciesID);
					}
				};
			}
		}

		SWTSkinObject soAdvInfo = getSkinObject("advinfo");
		if (soAdvInfo != null) {
			initAdvInfo(soAdvInfo);
		}

		if (device != null) {
			SWTSkinObject soTitle = getSkinObject("title");
			if (soTitle instanceof SWTSkinObjectText) {
				((SWTSkinObjectText) soTitle).setTextID("device.view.heading",
						new String[] {
							device.getName()
						});
			}
		}

		return null;
	}

	/**
	 * 
	 *
	 * @since 4.1.0.5
	 */
	private void initColumns(AzureusCore core) {
		if (columnsAdded) {
			return;
		}
		columnsAdded = true;
		UIManager uiManager = PluginInitializer.getDefaultInterface().getUIManager();
		TableManager tableManager = uiManager.getTableManager();
		tableManager.registerColumn(TranscodeFile.class, ColumnTJ_Rank.COLUMN_ID,
				new TableColumnCreationListener() {
					public void tableColumnCreated(TableColumn column) {
						new ColumnTJ_Rank(column);
						if (!column.getTableID().equals(TABLE_TRANSCODE_QUEUE)) {
							column.setVisible(false);
						}
					}
				});
		tableManager.registerColumn(TranscodeFile.class, ColumnThumbnail.COLUMN_ID,
				new TableColumnCreationListener() {
					public void tableColumnCreated(TableColumn column) {
						new ColumnThumbnail(column);
						column.setWidth(70);
					}
				});
		tableManager.registerColumn(TranscodeFile.class, ColumnTJ_Name.COLUMN_ID,
				new TableColumnCreationListener() {
					public void tableColumnCreated(TableColumn column) {
						new ColumnTJ_Name(column);
						if (column.getTableID().equals(TABLE_TRANSCODE_QUEUE)) {
							column.setWidth(200);
						} else if (!column.getTableID().endsWith(":type=1")) {
							column.setWidth(140);
						}
					}
				});
		tableManager.registerColumn(TranscodeFile.class,
				ColumnTJ_Duration.COLUMN_ID, new TableColumnCreationListener() {
					public void tableColumnCreated(TableColumn column) {
						new ColumnTJ_Duration(column);
					}
				});
		tableManager.registerColumn(TranscodeFile.class, ColumnTJ_Device.COLUMN_ID,
				new TableColumnCreationListener() {
					public void tableColumnCreated(TableColumn column) {
						new ColumnTJ_Device(column);
						column.setVisible(false);
					}
				});
		tableManager.registerColumn(TranscodeFile.class,
				ColumnTJ_Profile.COLUMN_ID, new TableColumnCreationListener() {
					public void tableColumnCreated(TableColumn column) {
						new ColumnTJ_Profile(column);
						if (column.getTableID().equals(TABLE_TRANSCODE_QUEUE)) {
							column.setWidth(70);
						}
					}
				});

		tableManager.registerColumn(TranscodeFile.class,
				ColumnTJ_Resolution.COLUMN_ID, new TableColumnCreationListener() {
					public void tableColumnCreated(TableColumn column) {
						new ColumnTJ_Resolution(column);
						column.setVisible(false);
						if (column.getTableID().equals(TABLE_TRANSCODE_QUEUE)) {
							column.setWidth(95);
						}
					}
				});

		tableManager.registerColumn(TranscodeFile.class, ColumnTJ_Status.COLUMN_ID,
				new TableColumnCreationListener() {
					public void tableColumnCreated(TableColumn column) {
						new ColumnTJ_Status(column);
					}
				});
		tableManager.registerColumn(TranscodeFile.class,
				ColumnTJ_Completion.COLUMN_ID, new TableColumnCreationListener() {
					public void tableColumnCreated(TableColumn column) {
						new ColumnTJ_Completion(column);
						column.setWidth(145);
					}
				});
		tableManager.registerColumn(TranscodeFile.class,
				ColumnTJ_CopiedToDevice.COLUMN_ID, new TableColumnCreationListener() {
					public void tableColumnCreated(TableColumn column) {
						new ColumnTJ_CopiedToDevice(column);

						if (column.getTableID().endsWith(":type=1")
								|| column.getTableID().equals(TABLE_TRANSCODE_QUEUE)) {

							column.setVisible(false);
						}
					}
				});
		
		tableManager.registerColumn(TranscodeFile.class,
				ColumnTJ_Category.COLUMN_ID, new TableColumnCreationListener() {
					public void tableColumnCreated(TableColumn column) {
						new ColumnTJ_Category(column);
					}
				});
	}

	// @see com.aelitis.azureus.ui.swt.views.skin.SkinView#skinObjectShown(com.aelitis.azureus.ui.swt.skin.SWTSkinObject, java.lang.Object)
	public Object skinObjectShown(SWTSkinObject skinObject, Object params) {
		super.skinObjectShown(skinObject, params);

		transcode_queue.addListener(this);

		if (transTarget != null) {
			transTarget.addListener(this);
		}

		SWTSkinObject soDeviceList = getSkinObject("device-list");
		if (soDeviceList != null) {
			initDeviceListTable((Composite) soDeviceList.getControl());
		}

		SWTSkinObject soTranscodeQueue = getSkinObject("transcode-queue");
		if (soTranscodeQueue != null) {
			initTranscodeQueueTable((Composite) soTranscodeQueue.getControl());
		}

		if (device instanceof TranscodeTarget){
		
			createDragDrop( tvFiles!=null?tvFiles:tvDevices);
		}
		
		setAdditionalInfoTitle(false);

		// This is bad.  Example: 
		// 1) Do a search
		// 2) Sidebar entry opens under Devices
		// 3) Close search sidebar
		// 4) Device entry gets auto-selected
		// 5) User gets ftux
		// 6) User says no, anger increases
		// 7) Go to 1
		//DevicesFTUX.ensureInstalled();

		return null;
	}

	/**
	 * @param soAdvInfo
	 *
	 * @since 4.1.0.5
	 */
	private void initAdvInfo(SWTSkinObject soAdvInfo) {
		SWTSkinButtonUtility btnAdvInfo = new SWTSkinButtonUtility(soAdvInfo);
		btnAdvInfo.addSelectionListener(new ButtonListenerAdapter() {
			public void pressed(SWTSkinButtonUtility buttonUtility,
					SWTSkinObject skinObject, int stateMask) {
				SWTSkinObject soArea = getSkinObject("advinfo-area");
				if (soArea != null) {
					boolean newVisibility = !soArea.isVisible();
					setAdditionalInfoTitle(newVisibility);
				}
			}
		});
		setAdditionalInfoTitle(false);
	}

	/**
	 * @param newVisibility
	 *
	 * @since 4.1.0.5
	 */
	protected void setAdditionalInfoTitle(boolean newVisibility) {
		SWTSkinObject soArea = getSkinObject("advinfo-area");
		if (soArea != null) {
			soArea.setVisible(newVisibility);
		}
		SWTSkinObject soText = getSkinObject("advinfo-title");
		if (soText instanceof SWTSkinObjectText) {
			String s = (newVisibility ? "[-]" : "[+]");
			if (device != null) {
				s += "Additional Device Info and Settings";
			} else {
				s += "General Options";
			}
			((SWTSkinObjectText) soText).setText(s);
		}
	}

	// @see com.aelitis.azureus.ui.swt.views.skin.SkinView#skinObjectHidden(com.aelitis.azureus.ui.swt.skin.SWTSkinObject, java.lang.Object)
	public Object skinObjectHidden(SWTSkinObject skinObject, Object params) {
		transcode_queue.removeListener(this);

		if (transTarget != null) {
			transTarget.removeListener(this);
		}

		synchronized (this) {
			if (tvFiles != null) {
				tvFiles.delete();
				tvFiles = null;
			}
		}
		Utils.disposeSWTObjects(new Object[] {
			tableJobsParent,
			dropTarget,
			dragSource,
		});
		if (tvDevices != null) {
			tvDevices.delete();
			tvDevices = null;
		}

		return super.skinObjectHidden(skinObject, params);
	}

	/**
	 * @param control
	 *
	 * @since 4.1.0.5
	 */
	private void initTranscodeQueueTable(Composite control) {
		String tableID;

		if (device == null) {

			tableID = TABLE_TRANSCODE_QUEUE;

		} else {

			tableID = TABLE_DEVICE_LIBRARY;

			if (device instanceof DeviceMediaRenderer) {

				DeviceMediaRenderer dmr = (DeviceMediaRenderer)device;
				
				if (!(dmr.canCopyToDevice()||dmr.canCopyToFolder())) {

					tableID += ":type=1";
				}
			}
		}

		tvFiles = new TableViewSWTImpl<TranscodeFile>(TranscodeFile.class, tableID,
				tableID, new TableColumnCore[0], device == null
						? ColumnTJ_Rank.COLUMN_ID : ColumnTJ_Status.COLUMN_ID, SWT.MULTI
						| SWT.FULL_SELECTION | SWT.VIRTUAL);
		tvFiles.setRowDefaultHeight(50);
		tvFiles.setHeaderVisible(true);
		tvFiles.setParentDataSource(device);

		tableJobsParent = new Composite(control, SWT.NONE);
		tableJobsParent.setLayoutData(Utils.getFilledFormData());
		GridLayout layout = new GridLayout();
		layout.marginHeight = layout.marginWidth = layout.verticalSpacing = layout.horizontalSpacing = 0;
		tableJobsParent.setLayout(layout);

		tvFiles.addSelectionListener(new TableSelectionListener() {

			public void selected(TableRowCore[] row) {
				SelectedContentManager.clearCurrentlySelectedContent();

				UIFunctions uiFunctions = UIFunctionsManager.getUIFunctions();
				if (uiFunctions != null) {
					uiFunctions.refreshIconBar();
				}
				
			}

			public void mouseExit(TableRowCore row) {
			}

			public void mouseEnter(TableRowCore row) {
			}

			public void focusChanged(TableRowCore focus) {
				SelectedContentManager.clearCurrentlySelectedContent();
				
				UIFunctions uiFunctions = UIFunctionsManager.getUIFunctions();
				if (uiFunctions != null) {
					uiFunctions.refreshIconBar();
				}
			}

			public void deselected(TableRowCore[] rows) {
				SelectedContentManager.clearCurrentlySelectedContent();
				
				UIFunctions uiFunctions = UIFunctionsManager.getUIFunctions();
				if (uiFunctions != null) {
					uiFunctions.refreshIconBar();
				}
			}

			public void defaultSelected(TableRowCore[] rows, int stateMask) {
				SelectedContentManager.clearCurrentlySelectedContent();
				
			}
		}, false);

		tvFiles.addLifeCycleListener(new TableLifeCycleListener() {
			public void tableViewInitialized() {
				if (transTarget == null) {
					// just add all jobs' files
					TranscodeJob[] jobs = transcode_queue.getJobs();
					for (TranscodeJob job : jobs) {
						TranscodeFile file = job.getTranscodeFile();
						if (file != null) {
							tvFiles.addDataSource(file);
						}
					}
				} else {
					tvFiles.addDataSources(transTarget.getFiles());
				}
			}

			public void tableViewDestroyed() {
			}
		});

		tvFiles.addMenuFillListener(new TableViewSWTMenuFillListener() {
			public void fillMenu(String sColumnName, Menu menu) {
				SBC_DevicesView.this.fillMenu(menu);
			}

			public void addThisColumnSubMenu(String columnName, Menu menuThisColumn) {
			}
		});

		tvFiles.addKeyListener(
			new KeyListener()
			{
				public void 
				keyPressed(
					KeyEvent e )
				{
					if ( e.stateMask == 0 && e.keyCode == SWT.DEL ){
						
						TranscodeFile[] selected;
						
						synchronized (this) {
							
							if ( tvFiles == null ){
								
								selected = new TranscodeFile[0];
								
							}else{
							
								List<TranscodeFile> selectedDataSources = tvFiles.getSelectedDataSources();
								selected = selectedDataSources.toArray(new TranscodeFile[0]);
							}
						}
						
						deleteFiles(selected, 0);
						
						e.doit = false;
					}
				}
				
				public void 
				keyReleased(
					KeyEvent arg0 ) 
				{
				}
			});
		
		tvFiles.initialize(tableJobsParent);

		control.layout(true);
	}

	/**
	 * @param menu
	 *
	 * @since 4.0.0.5
	 */
	protected void fillMenu(Menu menu) {

		Object[] _files = tvFiles.getSelectedDataSources().toArray();

		final TranscodeFile[] files = new TranscodeFile[_files.length];

		System.arraycopy(_files, 0, files, 0, files.length);

		// open file

		final MenuItem open_item = new MenuItem(menu, SWT.PUSH);

		Messages.setLanguageText(open_item, "MyTorrentsView.menu.open");

		Utils.setMenuItemImage(open_item, "run");

		File target_file = null;
		File source_file = null;

		try {
			if (files.length == 1) {

				target_file = files[0].getTargetFile().getFile();

				if (!target_file.exists()) {

					target_file = null;
				}
			}
		} catch (Throwable e) {

			Debug.out(e);
		}

		try {
			if (files.length == 1) {

				source_file = files[0].getSourceFile().getFile();

				if (!source_file.exists()) {

					source_file = null;
				}
			}
		} catch (Throwable e) {

			Debug.out(e);
		}

		final File f_target_file = target_file;
		final File f_source_file = source_file;

		open_item.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent ev) {

				Utils.launch(f_target_file.getAbsolutePath());
			};
		});

		open_item.setEnabled(target_file != null);

		// show in explorer

		final boolean use_open_containing_folder = COConfigurationManager.getBooleanParameter("MyTorrentsView.menu.show_parent_folder_enabled");

		final MenuItem show_item = new MenuItem(menu, SWT.PUSH);

		Messages.setLanguageText(show_item, "MyTorrentsView.menu."
				+ (use_open_containing_folder ? "open_parent_folder" : "explore"));

		show_item.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {

				ManagerUtils.open(
						f_target_file != null ? f_target_file : f_source_file,
						use_open_containing_folder);
			};
		});

		show_item.setEnabled((source_file != null && !files[0].isComplete())
				|| (target_file != null && files[0].isComplete()));

		
			// category
		
	    Menu menu_category = new Menu(menu.getShell(), SWT.DROP_DOWN);
	    final MenuItem item_category = new MenuItem(menu, SWT.CASCADE);
	    Messages.setLanguageText(item_category, "MyTorrentsView.menu.setCategory");
	    item_category.setMenu(menu_category);

	    addCategorySubMenu( menu_category, files );

		
		new MenuItem(menu, SWT.SEPARATOR);

		// pause

		final MenuItem pause_item = new MenuItem(menu, SWT.PUSH);

		pause_item.setText(MessageText.getString("v3.MainWindow.button.pause"));

		pause_item.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {

				for (int i = 0; i < files.length; i++) {
					TranscodeJob job = files[i].getJob();

					if (job != null) {
						job.pause();
					}
				}
			};
		});

		// resume

		final MenuItem resume_item = new MenuItem(menu, SWT.PUSH);

		resume_item.setText(MessageText.getString("v3.MainWindow.button.resume"));

		resume_item.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				for (int i = 0; i < files.length; i++) {
					TranscodeJob job = files[i].getJob();

					if (job != null) {
						job.resume();
					}
				}
			};
		});

		// separator

		new MenuItem(menu, SWT.SEPARATOR);

		if (device instanceof DeviceMediaRenderer) {

			DeviceMediaRenderer dmr = (DeviceMediaRenderer) device;

			if (dmr.canCopyToDevice() || dmr.canCopyToFolder()) {

				// retry

				final MenuItem retry_item = new MenuItem(menu, SWT.PUSH);

				retry_item.setText(MessageText.getString("device.retry.copy"));

				retry_item.addSelectionListener(new SelectionAdapter() {
					public void widgetSelected(SelectionEvent e) {
						for (int i = 0; i < files.length; i++) {
							TranscodeFile file = files[i];

							if ( file.getCopyToDeviceFails() > 0 || file.isCopiedToDevice() ){

								file.retryCopyToDevice();
							}
						}
					};
				});

				retry_item.setEnabled(false);

				for (TranscodeFile file : files) {

					if ( file.getCopyToDeviceFails() > 0 || file.isCopiedToDevice()) {

						retry_item.setEnabled(true);
					}
				}

				// separator

				new MenuItem(menu, SWT.SEPARATOR);
			}
		}

		// copy stream uri

		final MenuItem sc_item = new MenuItem(menu, SWT.PUSH);

		sc_item.setText(MessageText.getString("devices.copy_url"));

		if (files.length == 1) {

			final URL url = files[0].getStreamURL();

			if (url != null) {

				sc_item.addSelectionListener(new SelectionAdapter() {
					public void widgetSelected(SelectionEvent e) {
						ClipboardCopy.copyToClipBoard(url.toExternalForm());
					};
				});

			} else {

				sc_item.setEnabled(false);
			}
		} else {

			sc_item.setEnabled(false);
		}

		// remove

		final MenuItem remove_item = new MenuItem(menu, SWT.PUSH);

		remove_item.setText(MessageText.getString("azbuddy.ui.menu.remove"));

		Utils.setMenuItemImage(remove_item, "delete");

		remove_item.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				deleteFiles(files, 0);
			};
		});

		// separator

		new MenuItem(menu, SWT.SEPARATOR);

		// Logic to disable items 

		boolean has_selection = files.length > 0;

		remove_item.setEnabled(has_selection);

		boolean can_pause = has_selection;
		boolean can_resume = has_selection;

		int job_count = 0;

		for (int i = 0; i < files.length; i++) {
			TranscodeJob job = files[i].getJob();
			if (job == null) {
				continue;
			}

			job_count++;

			int state = job.getState();

			if (state != TranscodeJob.ST_RUNNING || !job.canPause()) {

				can_pause = false;
			}

			if (state != TranscodeJob.ST_PAUSED) {

				can_resume = false;
			}
		}

		pause_item.setEnabled(can_pause && job_count > 0);
		resume_item.setEnabled(can_resume && job_count > 0);
	}
	
	private void 
	addCategorySubMenu(
		Menu						menu_category,
		final TranscodeFile[]		files )
	{
		MenuItem[] items = menu_category.getItems();
		int i;
		for (i = 0; i < items.length; i++) {
			items[i].dispose();
		}

		Category[] categories = CategoryManager.getCategories();
		Arrays.sort(categories);

		if (categories.length > 0) {
			Category catUncat = CategoryManager.getCategory(Category.TYPE_UNCATEGORIZED);
			if (catUncat != null) {
				final MenuItem itemCategory = new MenuItem(menu_category, SWT.PUSH);
				Messages.setLanguageText(itemCategory, catUncat.getName());
				itemCategory.setData("Category", catUncat);
				itemCategory.addListener(SWT.Selection, new Listener() {
					public void handleEvent(Event event) {
						MenuItem item = (MenuItem)event.widget;
						assignSelectedToCategory((Category)item.getData("Category"),files);
					}
				});

				new MenuItem(menu_category, SWT.SEPARATOR);
			}

			for (i = 0; i < categories.length; i++) {
				if (categories[i].getType() == Category.TYPE_USER) {
					final MenuItem itemCategory = new MenuItem(menu_category, SWT.PUSH);
					itemCategory.setText(categories[i].getName());
					itemCategory.setData("Category", categories[i]);

					itemCategory.addListener(SWT.Selection, new Listener() {
						public void handleEvent(Event event) {
							MenuItem item = (MenuItem)event.widget;
							assignSelectedToCategory((Category)item.getData("Category"),files);
						}
					});
				}
			}

			new MenuItem(menu_category, SWT.SEPARATOR);
		}

		final MenuItem itemAddCategory = new MenuItem(menu_category, SWT.PUSH);
		Messages.setLanguageText(itemAddCategory,
		"MyTorrentsView.menu.setCategory.add");

		itemAddCategory.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event event) {
				addCategory(files);
			}
		});

	}

	private void 
	addCategory(
		TranscodeFile[]		files )	
	{
		CategoryAdderWindow adderWindow = new CategoryAdderWindow(Display.getDefault());
		Category newCategory = adderWindow.getNewCategory();
		if (newCategory != null)
			assignSelectedToCategory(newCategory,files);
	}

	private void 
	assignSelectedToCategory(
		Category 			category,
		TranscodeFile[]		files )
	{
		String[]	cats;
		
		if ( category.getType() == Category.TYPE_UNCATEGORIZED ){
		
			cats = new String[0];
			
		}else{
			
			cats = new String[]{ category.getName()};
		}
		
		for ( TranscodeFile file: files ){
			
			file.setCategories( cats );
		}
	}


	
	
	/**
	 * 
	 *
	 * @param parent 
	 * @since 4.1.0.5
	 */
	private void initDeviceListTable(Composite control) {
		tvDevices = new TableViewSWTImpl(TranscodeProvider.class, TABLE_DEVICES,
				TABLE_DEVICES, new TableColumnCore[0], ColumnTJ_Rank.COLUMN_ID);
		tvDevices.setRowDefaultHeight(50);
		tvDevices.setHeaderVisible(true);

		Composite parent = new Composite(control, SWT.NONE);
		parent.setLayoutData(Utils.getFilledFormData());
		GridLayout layout = new GridLayout();
		layout.marginHeight = layout.marginWidth = layout.verticalSpacing = layout.horizontalSpacing = 0;
		parent.setLayout(layout);
		 
		tvDevices.initialize(parent);
	}

	// @see com.aelitis.azureus.core.devices.TranscodeQueueListener#jobAdded(com.aelitis.azureus.core.devices.TranscodeJob)
	public void jobAdded(TranscodeJob job) {
		synchronized (this) {
			if (tvFiles == null) {
				return;
			}

			if (transTarget == null) {
				TranscodeFile file = job.getTranscodeFile();
				if (file != null) {
					tvFiles.addDataSource(file);
				}
			}
		}
	}

	// @see com.aelitis.azureus.core.devices.TranscodeQueueListener#jobChanged(com.aelitis.azureus.core.devices.TranscodeJob)
	public void jobChanged(TranscodeJob job) {
		synchronized (this) {
			if (tvFiles == null) {
				return;
			}
			TableRowCore row = tvFiles.getRow(job.getTranscodeFile());
			if (row != null) {
				row.invalidate();
				if (row.isVisible()) {
					UIFunctions uiFunctions = UIFunctionsManager.getUIFunctions();
					if (uiFunctions != null) {
						uiFunctions.refreshIconBar();
					}
				}
			}
		}
	}

	// @see com.aelitis.azureus.core.devices.TranscodeQueueListener#jobRemoved(com.aelitis.azureus.core.devices.TranscodeJob)
	public void jobRemoved(TranscodeJob job) {
		synchronized (this) {
			if (tvFiles == null) {
				return;
			}
			if (transTarget == null) {
				TranscodeFile file = job.getTranscodeFile();
				if (file != null) {
					tvFiles.removeDataSource(file);
				}
			} else {
				TableRowCore row = tvFiles.getRow(job.getTranscodeFile());
				if (row != null) {
					row.invalidate();
					if (row.isVisible()) {
						UIFunctions uiFunctions = UIFunctionsManager.getUIFunctions();
						if (uiFunctions != null) {
							uiFunctions.refreshIconBar();
						}
					}
				}
			}
		}
	}

	// @see org.gudy.azureus2.ui.swt.IconBarEnabler#isEnabled(java.lang.String)
	public boolean isEnabled(String itemKey) {
		Object[] selectedDS;
		int size;
		synchronized (this) {
			if (tvFiles == null) {
				return false;
			}
			selectedDS = tvFiles.getSelectedDataSources().toArray();
			size = tvFiles.size(false);
		}
		if (selectedDS.length == 0) {

			return (false);
		}

		if (itemKey.equals("remove")) {

			return (true);
		}

		boolean can_stop = true;
		boolean can_queue = true;
		boolean can_move_up = true;
		boolean can_move_down = true;
		boolean hasJob = false;

		for (Object ds : selectedDS) {
			TranscodeJob job = ((TranscodeFile) ds).getJob();

			if (job == null) {
				continue;
			}

			hasJob = true;

			int index = job.getIndex();

			if (index == 1) {

				can_move_up = false;

			}

			if (index == size) {

				can_move_down = false;
			}

			int state = job.getState();

			if (state != TranscodeJob.ST_PAUSED && state != TranscodeJob.ST_RUNNING
					&& state != TranscodeJob.ST_FAILED && state != TranscodeJob.ST_QUEUED) {

				can_stop = false;
			}

			if (state != TranscodeJob.ST_PAUSED && state != TranscodeJob.ST_STOPPED
					&& state != TranscodeJob.ST_FAILED) {

				can_queue = false;
			}
		}

		if (!hasJob) {
			can_stop = can_queue = can_move_down = can_move_up = false;
		}

		if (itemKey.equals("stop")) {

			return (can_stop);
		}

		if (itemKey.equals("start")) {

			return (can_queue);
		}

		if (itemKey.equals("up")) {

			return (can_move_up);
		}

		if (itemKey.equals("down")) {

			return (can_move_down);
		}

		return (false);
	}

	// @see org.gudy.azureus2.ui.swt.IconBarEnabler#isSelected(java.lang.String)
	public boolean isSelected(String itemKey) {
		return false;
	}

	// @see org.gudy.azureus2.ui.swt.IconBarEnabler#itemActivated(java.lang.String)
	public void itemActivated(final String itemKey) {
		// assumed to be on SWT thread, so it's safe to use tvFiles without a sync
		if (tvFiles == null) {
			return;
		}

		TranscodeFile[] selectedDS = tvFiles.getSelectedDataSources().toArray(new TranscodeFile[0]);
		if (selectedDS.length == 0) {
			return;
		}

		if (itemKey.equals("remove")) {
			deleteFiles(selectedDS, 0);
			return;
		}

		java.util.List<TranscodeJob> jobs = new ArrayList<TranscodeJob>(
				selectedDS.length);

		for (int i = 0; i < selectedDS.length; i++) {
			TranscodeFile file = (TranscodeFile) selectedDS[i];
			TranscodeJob job = file.getJob();
			if (job != null) {
				jobs.add(job);
			}
		}
		if (jobs.size() == 0) {
			return;
		}

		if (itemKey.equals("up") || itemKey.equals("down")) {

			Collections.sort(jobs, new Comparator<TranscodeJob>() {
				public int compare(TranscodeJob j1, TranscodeJob j2) {

					return ((itemKey.equals("up") ? 1 : -1) * (j1.getIndex() - j2.getIndex()));
				}
			});
		}

		boolean forceSort = false;
		for (TranscodeJob job : jobs) {

			if (itemKey.equals("stop")) {

				job.stop();

			} else if (itemKey.equals("start")) {

				job.queue();

			} else if (itemKey.equals("up")) {

				job.moveUp();

				TableColumnCore sortColumn = tvFiles.getSortColumn();
				forceSort = sortColumn != null
						&& sortColumn.getName().equals(ColumnTJ_Rank.COLUMN_ID);

			} else if (itemKey.equals("down")) {

				job.moveDown();

				TableColumnCore sortColumn = tvFiles.getSortColumn();
				forceSort = sortColumn != null
						&& sortColumn.getName().equals(ColumnTJ_Rank.COLUMN_ID);
			}
		}
		tvFiles.refreshTable(forceSort);

	}

	// @see com.aelitis.azureus.ui.common.updater.UIUpdatable#getUpdateUIName()
	public String getUpdateUIName() {
		return "DevicesView";
	}

	// @see com.aelitis.azureus.ui.common.updater.UIUpdatable#updateUI()
	public void updateUI() {
		if (tvFiles != null) {
			tvFiles.refreshTable(false);
		}
	}

	// @see com.aelitis.azureus.core.devices.TranscodeTargetListener#fileAdded(com.aelitis.azureus.core.devices.TranscodeFile)
	public void fileAdded(TranscodeFile file) {
		synchronized (this) {
			if (tvFiles != null) {
				tvFiles.addDataSource(file);
			}
		}
	}

	// @see com.aelitis.azureus.core.devices.TranscodeTargetListener#fileChanged(com.aelitis.azureus.core.devices.TranscodeFile, int, java.lang.Object)
	public void fileChanged(TranscodeFile file, int type, Object data) {
		synchronized (this) {
			if (tvFiles == null) {
				return;
			}
			TableRowCore row = tvFiles.getRow(file);
			if (row != null) {
				row.invalidate();
				if (row.isVisible()) {
					UIFunctions uiFunctions = UIFunctionsManager.getUIFunctions();
					if (uiFunctions != null) {
						uiFunctions.refreshIconBar();
					}
				}
			}
		}
	}

	// @see com.aelitis.azureus.core.devices.TranscodeTargetListener#fileRemoved(com.aelitis.azureus.core.devices.TranscodeFile)
	public void fileRemoved(TranscodeFile file) {
		synchronized (this) {
			if (tvFiles != null) {
				tvFiles.removeDataSource(file);
			}
		}
	}

	protected void deleteFiles(final TranscodeFile[] toRemove, final int startIndex) {
		if (toRemove[startIndex] == null) {
			int nextIndex = startIndex + 1;
			if (nextIndex < toRemove.length) {
				deleteFiles(toRemove, nextIndex);
			}
			return;
		}

		final TranscodeFile file = toRemove[startIndex];
		try {

			File cache_file = file.getCacheFileIfExists();

			if (cache_file != null && cache_file.exists() && file.isComplete()) {

				String path = cache_file.toString();

				String title = MessageText.getString("xcode.deletedata.title");

				String copy_text = "";

				Device device = file.getDevice();

				if (device instanceof DeviceMediaRenderer) {

					DeviceMediaRenderer dmr = (DeviceMediaRenderer)device;
					
					File copy_to = dmr.getCopyToFolder();
					
					if ( dmr.canCopyToDevice() || ( dmr.canCopyToFolder() && copy_to != null && copy_to.exists())){

						copy_text = MessageText.getString("xcode.deletedata.message.2",
								new String[] {
									device.getName()
								});
					}
				}

				String text = MessageText.getString("xcode.deletedata.message",
						new String[] {
							file.getName(),
							file.getProfileName(),
							copy_text
						});

				MessageBoxShell mb = new MessageBoxShell(title, text);
				mb.setRemember("xcode.deletedata.noconfirm.key", false,
						MessageText.getString("deletedata.noprompt"));

				if (startIndex == toRemove.length - 1) {
  				mb.setButtons(0, new String[] {
  					MessageText.getString("Button.yes"),
  					MessageText.getString("Button.no"),
  				}, new Integer[] { 0, 1 });
  				mb.setRememberOnlyIfButton(0);
				} else {
  				mb.setButtons(1, new String[] {
  					MessageText.getString("Button.removeAll"),
  					MessageText.getString("Button.yes"),
  					MessageText.getString("Button.no"),
  				}, new Integer[] { 2, 0, 1 });
  				mb.setRememberOnlyIfButton(1);
				}

				DownloadManager dm = null;

				if (dm != null) {

					mb.setRelatedObject(dm);
				}

				mb.setLeftImage(SWT.ICON_WARNING);

				mb.open(new UserPrompterResultListener() {
					public void prompterClosed(int result) {
						if (result == -1) {
							return;
						} else if (result == 0) {
							deleteNoCheck(file);
						} else if (result == 2) {
							for (int i = startIndex; i < toRemove.length; i++) {
								if (toRemove[i] != null) {
									deleteNoCheck(toRemove[i]);
								}
							}
							return;
						}

						int nextIndex = startIndex + 1;
						if (nextIndex < toRemove.length) {
							deleteFiles(toRemove, nextIndex);
						}
					}
				});

			} else {

				deleteNoCheck(file);
			}
		} catch (Throwable e) {

			Debug.out(e);
		}
	}
	
	private void deleteNoCheck(TranscodeFile file) {
		TranscodeJob job = file.getJob();

		if (job != null) {

			job.remove();
		}

		try {
			file.delete(file.getCacheFileIfExists() != null);
		} catch (TranscodeException e) {
			Debug.out(e);
		}
	}
	
	private void 
	createDragDrop(
		final TableViewSWTImpl<?>		table )
	{
		try {

			Transfer[] types = new Transfer[] { TextTransfer.getInstance() };

			if (dragSource != null && !dragSource.isDisposed()) {
				dragSource.dispose();
			}

			if (dropTarget != null && !dropTarget.isDisposed()) {
				dropTarget.dispose();
			}

			dragSource = table.createDragSource(DND.DROP_MOVE | DND.DROP_COPY);
			if (dragSource != null) {
				dragSource.setTransfer(types);
				dragSource.addDragListener(new DragSourceAdapter() {
					private String eventData;

					public void dragStart(DragSourceEvent event) {
						TableRowCore[] rows = table.getSelectedRows();
						if (rows.length != 0) {
							event.doit = true;
							// System.out.println("DragStart");
							drag_drop_line_start = rows[0].getIndex();
							drag_drop_rows = rows;
						} else {
							event.doit = false;
							drag_drop_line_start = -1;
							drag_drop_rows = null;
						}

						// Build eventData here because on OSX, selection gets cleared
						// by the time dragSetData occurs
						
						java.util.List selectedFiles = table.getSelectedDataSources();
						
						eventData="TranscodeFile\n";

						for ( Object o: selectedFiles ){
														
							TranscodeFile file = (TranscodeFile)o;
							
							if ( file.isComplete()){
								
								try{
									eventData += file.getTargetFile().getFile().getAbsolutePath() + "\n";
									
								}catch( Throwable e ){
									
								}
							}
						}
					}

					public void dragSetData(DragSourceEvent event) {
						// System.out.println("DragSetData");
						event.data = eventData;
					}
				});
			}

			dropTarget = table.createDropTarget(DND.DROP_DEFAULT | DND.DROP_MOVE
					| DND.DROP_COPY | DND.DROP_LINK | DND.DROP_TARGET_MOVE);
			if (dropTarget != null) {
				dropTarget.setTransfer(new Transfer[] { HTMLTransfer.getInstance(),
						URLTransfer.getInstance(), FileTransfer.getInstance(),
						TextTransfer.getInstance() });

				dropTarget.addDropListener(new DropTargetAdapter() {
					public void dropAccept(DropTargetEvent event) {
						event.currentDataType = URLTransfer.pickBestType(event.dataTypes,
								event.currentDataType);
					}

					public void dragEnter(DropTargetEvent event) {
						// no event.data on dragOver, use drag_drop_line_start to determine
						// if ours
						if (drag_drop_line_start < 0) {
							if (event.detail != DND.DROP_COPY) {
								if ((event.operations & DND.DROP_LINK) > 0)
									event.detail = DND.DROP_LINK;
								else if ((event.operations & DND.DROP_COPY) > 0)
									event.detail = DND.DROP_COPY;
							}
						} else if (TextTransfer.getInstance().isSupportedType(
								event.currentDataType)) {
							event.detail = event.item == null ? DND.DROP_NONE : DND.DROP_MOVE;
							event.feedback = DND.FEEDBACK_SCROLL | DND.FEEDBACK_INSERT_BEFORE;
						}
					}

					public void dragOver(DropTargetEvent event) {
						if (drag_drop_line_start >= 0) {
							event.detail = event.item == null ? DND.DROP_NONE : DND.DROP_MOVE;
							event.feedback = DND.FEEDBACK_SCROLL | DND.FEEDBACK_INSERT_BEFORE;
						}
					}

					public void drop(DropTargetEvent event) {
						try{
							if ( 	event.data instanceof String &&
									((String) event.data).startsWith("TranscodeFile\n")) {
								
									// todo: support drag and drop reordering of xcode queue?
								
								return;
							}
	
							event.detail = DND.DROP_NONE;
							
							DeviceManagerUI.handleDrop((TranscodeTarget)device, event.data );

						}finally{
							
							drag_drop_line_start = -1;
							drag_drop_rows = null;
						}
					}
				});
			}

		} catch (Throwable t) {
			Debug.out( "failed to init drag-n-drop", t);
		}
	}
}
