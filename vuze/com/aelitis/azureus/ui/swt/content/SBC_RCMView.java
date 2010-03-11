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

package com.aelitis.azureus.ui.swt.content;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

import org.eclipse.swt.SWT;

import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;

import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.util.Constants;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.core3.util.UrlUtils;

import org.gudy.azureus2.core3.util.TorrentUtils;
import org.gudy.azureus2.plugins.ui.UIManager;
import org.gudy.azureus2.plugins.ui.tables.TableColumn;
import org.gudy.azureus2.plugins.ui.tables.TableColumnCreationListener;
import org.gudy.azureus2.plugins.ui.tables.TableManager;
import org.gudy.azureus2.pluginsimpl.local.PluginInitializer;
import org.gudy.azureus2.ui.swt.*;

import org.gudy.azureus2.ui.swt.views.table.*;
import org.gudy.azureus2.ui.swt.views.table.impl.TableViewSWTImpl;

import com.aelitis.azureus.core.AzureusCore;
import com.aelitis.azureus.core.AzureusCoreFactory;
import com.aelitis.azureus.core.AzureusCoreRunningListener;
import com.aelitis.azureus.core.content.RelatedContent;
import com.aelitis.azureus.core.content.RelatedContentManager;
import com.aelitis.azureus.core.content.RelatedContentManagerListener;
import com.aelitis.azureus.ui.UIFunctions;
import com.aelitis.azureus.ui.UIFunctionsManager;
import com.aelitis.azureus.ui.common.table.*;
import com.aelitis.azureus.ui.common.updater.UIUpdatable;
import com.aelitis.azureus.ui.selectedcontent.DownloadUrlInfo;
import com.aelitis.azureus.ui.selectedcontent.ISelectedContent;
import com.aelitis.azureus.ui.selectedcontent.SelectedContentManager;
import com.aelitis.azureus.ui.swt.content.columns.*;
import com.aelitis.azureus.ui.swt.skin.SWTSkinObject;
import com.aelitis.azureus.ui.swt.skin.SWTSkinObjectContainer;
import com.aelitis.azureus.ui.swt.toolbar.ToolBarEnablerSelectedContent;
import com.aelitis.azureus.ui.swt.views.skin.SkinView;
import com.aelitis.azureus.ui.swt.views.skin.SkinViewManager;
import com.aelitis.azureus.ui.swt.views.skin.sidebar.SideBar;
import com.aelitis.azureus.ui.swt.views.skin.sidebar.SideBarEntrySWT;


public class 
SBC_RCMView
	extends SkinView
	implements UIUpdatable, IconBarEnabler, TableViewFilterCheck<RelatedContent>
{
	public static final String TABLE_RCM = "RCM";

	private static boolean columnsAdded = false;

	private static RelatedContentManager	manager;
	
	static{
		try{
			manager = RelatedContentManager.getSingleton();
			
		}catch( Throwable e ){
			
			Debug.out(e);
		}
	}
	
	private TableViewSWT<RelatedContent> tv_related_content;

	private SideBarEntrySWT 	sidebar_entry;
	private Composite			table_parent;
	private boolean				space_reserved;
	
	
	private Text txtFilter;

	public Object 
	skinObjectInitialShow(
		SWTSkinObject skinObject, Object params ) 
	{
		AzureusCoreFactory.addCoreRunningListener(
			new AzureusCoreRunningListener() 
			{
				public void 
				azureusCoreRunning(
					AzureusCore core )
				{
					initColumns( core );
				}
			});


		SideBar sidebar = (SideBar)SkinViewManager.getByClass(SideBar.class);
		
		if ( sidebar != null ){
			
			sidebar_entry = sidebar.getCurrentEntry();
			
			sidebar_entry.setIconBarEnabler(this);

			if ( !sidebar_entry.getId().equals( SideBar.SIDEBAR_SECTION_RELATED_CONTENT )){
		
				manager.reserveTemporarySpace();
				
				space_reserved = true;
			}
		}

		SWTSkinObject soFilterArea = getSkinObject("filterarea");
		if (soFilterArea != null) {
			Composite parent = (Composite) soFilterArea.getControl();
			FormData fd;
			CLabel lblFilter = new CLabel(parent, SWT.CENTER);
			Messages.setLanguageText(lblFilter, "MyTorrentsView.filter");
			fd = Utils.getFilledFormData();
			fd.right = null;
			lblFilter.setLayoutData(fd);

			txtFilter = new Text(parent, SWT.BORDER);
			fd = new FormData();
			fd.left = new FormAttachment(lblFilter, 10);
			fd.top = new FormAttachment(lblFilter, 0, SWT.CENTER);
			fd.right = new FormAttachment(100, -10);
			txtFilter.setLayoutData(fd);
			
			parent.layout(true);
		}

		return null;
	}


	private void 
	initColumns(
		AzureusCore core ) 
	{
		synchronized( SBC_RCMView.class ){
			
			if ( columnsAdded ){
			
				return;
			}
		
			columnsAdded = true;
		}
		
		UIManager uiManager = PluginInitializer.getDefaultInterface().getUIManager();
		
		TableManager tableManager = uiManager.getTableManager();
		
		tableManager.registerColumn(
				RelatedContent.class, 
				ColumnRC_New.COLUMN_ID,
					new TableColumnCreationListener() {
						public void tableColumnCreated(TableColumn column) {
							new ColumnRC_New(column);
						}
					});

		tableManager.registerColumn(
				RelatedContent.class, 
				ColumnRC_Rank.COLUMN_ID,
					new TableColumnCreationListener() {
						public void tableColumnCreated(TableColumn column) {
							new ColumnRC_Rank(column);
						}
					});
		
		tableManager.registerColumn(
				RelatedContent.class, 
				ColumnRC_Level.COLUMN_ID,
					new TableColumnCreationListener() {
						public void tableColumnCreated(TableColumn column) {
							new ColumnRC_Level(column);
						}
					});
			
		tableManager.registerColumn(
				RelatedContent.class, 
				ColumnRC_Title.COLUMN_ID,
					new TableColumnCreationListener() {
						public void tableColumnCreated(TableColumn column) {
							new ColumnRC_Title(column);
						}
					});

		tableManager.registerColumn(
				RelatedContent.class, 
				ColumnRC_Actions.COLUMN_ID,
					new TableColumnCreationListener() {
						public void tableColumnCreated(TableColumn column) {
							new ColumnRC_Actions(column);
						}
					});
			
		tableManager.registerColumn(
				RelatedContent.class, 
				ColumnRC_Hash.COLUMN_ID,
					new TableColumnCreationListener() {
						public void tableColumnCreated(TableColumn column) {
							new ColumnRC_Hash(column);
						}
					});
			
		tableManager.registerColumn(
				RelatedContent.class, 
				ColumnRC_Tracker.COLUMN_ID,
					new TableColumnCreationListener() {
						public void tableColumnCreated(TableColumn column) {
							new ColumnRC_Tracker(column);
						}
					});
			
		tableManager.registerColumn(
				RelatedContent.class, 
				ColumnRC_Size.COLUMN_ID,
					new TableColumnCreationListener() {
						public void tableColumnCreated(TableColumn column) {
							new ColumnRC_Size(column);
						}
					});
		
		tableManager.registerColumn(
				RelatedContent.class, 
				ColumnRC_Created.COLUMN_ID,
					new TableColumnCreationListener() {
						public void tableColumnCreated(TableColumn column) {
							new ColumnRC_Created(column);
						}
					});
		tableManager.registerColumn(
				RelatedContent.class, 
				ColumnRC_Seeds.COLUMN_ID,
					new TableColumnCreationListener() {
						public void tableColumnCreated(TableColumn column) {
							new ColumnRC_Seeds(column);
						}
					});

		tableManager.registerColumn(
				RelatedContent.class, 
				ColumnRC_Peers.COLUMN_ID,
					new TableColumnCreationListener() {
						public void tableColumnCreated(TableColumn column) {
							new ColumnRC_Peers(column);
						}
					});

		
		
		tableManager.registerColumn(
				RelatedContent.class, 
				ColumnRC_LastSeen.COLUMN_ID,
					new TableColumnCreationListener() {
						public void tableColumnCreated(TableColumn column) {
							new ColumnRC_LastSeen(column);
						}
					});
	}

	public Object 
	skinObjectShown(
		SWTSkinObject 	skinObject, 
		Object 			params ) 
	{
		super.skinObjectShown(skinObject, params);

		SWTSkinObject so_list = getSkinObject("rcm-list");
		
		if ( so_list != null ){
			
			initTable((Composite) so_list.getControl());
		}
		
		return null;
	}

	public Object 
	skinObjectHidden(
		SWTSkinObject 	skinObject, 
		Object 			params ) 
	{
		synchronized( this ){
			
			if ( tv_related_content != null ){
				
				tv_related_content.delete();
				
				tv_related_content = null;
			}
		}
		
		Utils.disposeSWTObjects(new Object[] {
			table_parent,
		});

		return( super.skinObjectHidden(skinObject, params));
	}

	public Object
	skinObjectDestroyed(
		SWTSkinObject 	skinObject, 
		Object 			params ) 
	{
		synchronized( this ){
			
			if ( tv_related_content != null ){
				
				tv_related_content.delete();
				
				tv_related_content = null;
			}
		}
		
		Utils.disposeSWTObjects(new Object[] {
			table_parent,
		});

		if ( space_reserved ){
		
			manager.releaseTemporarySpace();
		}
		
		return( super.skinObjectDestroyed(skinObject, params));
	}
	
	private void 
	initTable(
		Composite control ) 
	{
		tv_related_content = new TableViewSWTImpl<RelatedContent>(
				RelatedContent.class, 
				TABLE_RCM,
				TABLE_RCM, 
				new TableColumnCore[0], 
				ColumnRC_New.COLUMN_ID, 
				SWT.MULTI | SWT.FULL_SELECTION | SWT.VIRTUAL );
		if (txtFilter != null) {
			tv_related_content.enableFilterCheck(txtFilter, this);
		}
		tv_related_content.setRowDefaultHeight(16);
		SWTSkinObject soSizeSlider = getSkinObject("table-size-slider");
		if (soSizeSlider instanceof SWTSkinObjectContainer) {
			SWTSkinObjectContainer so = (SWTSkinObjectContainer) soSizeSlider;
			if (!tv_related_content.enableSizeSlider(so.getComposite(), 16, 100)) {
				so.setVisible(false);
			}
		}
		
		table_parent = new Composite(control, SWT.NONE);
		table_parent.setLayoutData(Utils.getFilledFormData());
		GridLayout layout = new GridLayout();
		layout.marginHeight = layout.marginWidth = layout.verticalSpacing = layout.horizontalSpacing = 0;
		table_parent.setLayout(layout);

		tv_related_content.addSelectionListener(new TableSelectionListener() {

			public void 
			selected(
				TableRowCore[] rows) 
			{				
				ArrayList<ISelectedContent>	valid = new ArrayList<ISelectedContent>();
				
				for (int i=0;i<rows.length;i++){
					
					final RelatedContent rc = (RelatedContent)rows[i].getDataSource();
					
					if ( rc.getHash() != null ){

						valid.add(
							new ToolBarEnablerSelectedContent( SBC_RCMView.this )
							{
								public DownloadUrlInfo 
								getDownloadInfo() 
								{
									return(	new DownloadUrlInfo( UrlUtils.getMagnetURI( rc.getHash())));
								}
							});
					}
				}
				
				ISelectedContent[] sels = valid.toArray( new ISelectedContent[valid.size()] );
				
				SelectedContentManager.changeCurrentlySelectedContent( "IconBarEnabler", sels, null );
				
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
				UIFunctions uiFunctions = UIFunctionsManager.getUIFunctions();
				if (uiFunctions != null) {
					uiFunctions.refreshIconBar();
				}
			}

			public void deselected(TableRowCore[] rows) {
				UIFunctions uiFunctions = UIFunctionsManager.getUIFunctions();
				if (uiFunctions != null) {
					uiFunctions.refreshIconBar();
				}
			}

			public void defaultSelected(TableRowCore[] rows, int stateMask) {
			}
		}, false);

		tv_related_content.addLifeCycleListener(
			new TableLifeCycleListener() 
			{
				private Set<RelatedContent>	content_set = new HashSet<RelatedContent>();
				
				private int liveness_marker;
				
				private RelatedContentManagerListener current_rcm_listener;
				
				
				public void 
				tableViewInitialized() 
				{
					final int current_liveness_marker = ++liveness_marker;
					
					current_rcm_listener = 
						new RelatedContentManagerListener()
						{
							public void
							contentFound(
								RelatedContent[]	content )
							{							
							}

							public void
							contentChanged(
								RelatedContent[]	content )
							{
								final java.util.List<RelatedContent> hits = new ArrayList<RelatedContent>( content.length );

								synchronized( content_set ){
									
									if ( liveness_marker != current_liveness_marker ){
										
										return;
									}							
								
									for ( RelatedContent c: content ){

										if ( content_set.contains( c )){
									
											hits.add( c );
										}
									}
								}
								
								if ( hits.size() > 0 ){
									
									for (RelatedContent rc : hits) {
										TableRowCore row = tv_related_content.getRow(rc);
										if (row != null) {
											row.refresh(true);
										}
									}
								}
							}
							
							public void 
							contentRemoved(
								final RelatedContent[] content )
							{
								final java.util.List<RelatedContent> hits = new ArrayList<RelatedContent>( content.length );
								
								synchronized( content_set ){
									
									if ( liveness_marker != current_liveness_marker ){
										
										return;
									}
								
									for ( RelatedContent c: content ){
									
										if ( content_set.remove( c )){
									
											hits.add( c );
										}
									}
								}
								
								if ( hits.size() > 0 ){
									
									Utils.execSWTThread(
											new Runnable()
											{
												public void
												run()
												{
													if ( tv_related_content != null ){
														
														tv_related_content.removeDataSources( hits.toArray( new RelatedContent[ hits.size()] ));
													}
												}
											});
								}
							}
							
							public void
							contentChanged()
							{
								if ( tv_related_content != null ){
									
									tv_related_content.refreshTable( false );
								}
							}
							
							public void
							contentReset()
							{
								if ( tv_related_content != null ){
									
									tv_related_content.removeAllTableRows();
								}
							}
						};
						
					manager.addListener( current_rcm_listener );
				
					Object data_source = sidebar_entry.getDatasource();
					
					if ( data_source instanceof RelatedContentEnumerator ){
						
						final TableViewSWT<RelatedContent> f_table = tv_related_content;
						
						((RelatedContentEnumerator)data_source).enumerate(
							new RelatedContentEnumerator.RelatedContentEnumeratorListener()
							{
								public void
								contentFound(
									RelatedContent[]	content )
								{
									ArrayList<RelatedContent> new_content = null;
									
									synchronized( content_set ){
										
										if ( liveness_marker != current_liveness_marker ){
											
											return;
										}
										
										for ( RelatedContent c: content ){
											
											if ( content_set.contains( c )){
												
												if ( new_content == null ){
													
													new_content = new ArrayList<RelatedContent>( content.length );
													
													for ( RelatedContent c2: content ){
														
														if ( c == c2 ){
															
															break;
														}
														
														new_content.add( c2 );
													}
												}									
											}else{
												
												if ( new_content != null ){
													
													new_content.add( c );
												}
											}
										}
					
										if ( new_content != null ){
											
											content = new_content.toArray( new RelatedContent[ new_content.size()]);
										}
										
										content_set.addAll( Arrays.asList( content ));
									}
									
									if ( content.length > 0 ){
										
										final RelatedContent[] f_content = content; 
										
										Utils.execSWTThread(
											new Runnable()
											{
												public void
												run()
												{
													if ( tv_related_content == f_table ){
													
														synchronized( content_set ){
															
															if ( liveness_marker != current_liveness_marker ){
																
																return;
															}
														}
														
														f_table.addDataSources( f_content );
													}
												}
											});
									}
								}
							});
					}
				}

				public void 
				tableViewDestroyed() 
				{
					manager.removeListener( current_rcm_listener );
					
					synchronized( content_set ){
						
						liveness_marker++;
					
						content_set.clear();
					}
				}
			});

		tv_related_content.addMenuFillListener(
			new TableViewSWTMenuFillListener() 
			{
				public void 
				fillMenu(String sColumnName, Menu menu)
				{
					Object[] _related_content = tv_related_content.getSelectedDataSources().toArray();

					final RelatedContent[] related_content = new RelatedContent[_related_content.length];

					System.arraycopy(_related_content, 0, related_content, 0, related_content.length);

					final MenuItem assoc_item = new MenuItem(menu, SWT.PUSH);

					assoc_item.setText(MessageText.getString("rcm.contextmenu.lookupassoc"));

					final ArrayList<RelatedContent> assoc_ok = new ArrayList<RelatedContent>();
					
					for ( RelatedContent c: related_content ){
						
						if ( c.getHash() != null ){
							
							assoc_ok.add( c );
						}
					}
					
					assoc_item.addSelectionListener(new SelectionAdapter() {
						public void widgetSelected(SelectionEvent e ){
							
							int	 i = 0;
							
							RelatedContentUI ui = RelatedContentUI.getSingleton();
							
							for ( RelatedContent c: assoc_ok ){
							
								ui.addSearch( c.getHash(), c.getTitle());
								
								i++;
								
								if ( i > 8 ){
									
									break;
								}
							}
						};
					});
					
					if ( assoc_ok.size() == 0 ){
						
						assoc_item.setEnabled( false );
					}

					MenuItem item = new MenuItem(menu, SWT.PUSH);
					item.setText("gis");
					item.addSelectionListener(new SelectionAdapter() {
						public void widgetSelected(SelectionEvent e) {
							String s = related_content[0].getTitle();
							s = s.replaceAll("[-_]", " ");
							Utils.launch("http://images.google.com/images?q=" + UrlUtils.encode(s));
						};
					});

					item = new MenuItem(menu, SWT.PUSH);
					item.setText("g");
					item.addSelectionListener(new SelectionAdapter() {
						public void widgetSelected(SelectionEvent e) {
							String s = related_content[0].getTitle();
							s = s.replaceAll("[-_]", " ");
							Utils.launch("http://google.com/search?q=" + UrlUtils.encode(s));
						};
					});

					
					new MenuItem(menu, SWT.SEPARATOR );

					final MenuItem remove_item = new MenuItem(menu, SWT.PUSH);

					remove_item.setText(MessageText.getString("azbuddy.ui.menu.remove"));

					Utils.setMenuItemImage( remove_item, "delete" );

					remove_item.addSelectionListener(new SelectionAdapter() {
						public void widgetSelected(SelectionEvent e) {
							manager.delete( related_content );
						};
					});
				}

				public void 
				addThisColumnSubMenu(
					String columnName, Menu menuThisColumn) 
				{
				}
			});

		tv_related_content.addKeyListener(
				new KeyListener()
				{
					public void 
					keyPressed(
						KeyEvent e )
					{
						if ( e.stateMask == 0 && e.keyCode == SWT.DEL ){
							
							Object[] selected;
							
							synchronized (this) {
								
								if ( tv_related_content == null ){
									
									selected = new Object[0];
									
								}else{
								
									selected = tv_related_content.getSelectedDataSources().toArray();
								}
							}
							
							RelatedContent[] content = new RelatedContent[ selected.length ];
							
							for ( int i=0;i<content.length;i++){
								
								content[i] = (RelatedContent)selected[i];
							}
							
							manager.delete( content );
							
							e.doit = false;
						}
					}
					
					public void 
					keyReleased(
						KeyEvent arg0 ) 
					{
					}
				});
		tv_related_content.initialize( table_parent );

		control.layout(true);
	}
	
	public boolean 
	isEnabled(
		String key )
	{
		if ( key.equals( "remove" )){
	
			return( tv_related_content.getSelectedDataSources().size() > 0 );
		}
		
		return( false );
	}
	
	public boolean 
	isSelected(
		String key )
	{
		return( false );
	}
	
	public void 
	itemActivated(
		String key )
	{
		Object[] _related_content = tv_related_content.getSelectedDataSources().toArray();

		if ( _related_content.length > 0 ){
			
			RelatedContent[] related_content = new RelatedContent[_related_content.length];
	
			System.arraycopy( _related_content, 0, related_content, 0, related_content.length );
			
			manager.delete( related_content );
		}
	}
	
	
	public String 
	getUpdateUIName() 
	{
		return( "RCMView" );
	}

	public void 
	updateUI() 
	{
		if ( tv_related_content != null ){
			
			tv_related_content.refreshTable( false );
		}
	}


	// @see org.gudy.azureus2.ui.swt.views.table.TableViewFilterCheck#filterCheck(java.lang.Object, java.lang.String, boolean)
	public boolean filterCheck(RelatedContent ds, String filter, boolean regex) {
		if ( filter == null || filter.length() == 0 ){
			
			return( true );
		}

		try {
			String name = ds.getTitle();
			String s = regex ? filter : "\\Q" + filter.replaceAll("[|;]", "\\\\E|\\\\Q") + "\\E";
  		Pattern pattern = Pattern.compile(s, Pattern.CASE_INSENSITIVE);
  
  		return pattern.matcher(name).find();
		} catch (Exception e) {
			return true;
		}
	}
	
	// @see org.gudy.azureus2.ui.swt.views.table.TableViewFilterCheck#filterSet(java.lang.String)
	public void filterSet(String filter) {
	}
}
