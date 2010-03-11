/*
 * Created on Jul 14, 2009
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


package com.aelitis.azureus.ui.swt.content;



import java.util.ArrayList;
import java.util.List;


import org.eclipse.swt.widgets.TreeItem;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.util.AERunnable;
import org.gudy.azureus2.core3.util.AsyncDispatcher;
import org.gudy.azureus2.core3.util.ByteArrayHashMap;
import org.gudy.azureus2.core3.util.ByteFormatter;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.plugins.PluginInterface;
import org.gudy.azureus2.plugins.download.Download;
import org.gudy.azureus2.plugins.torrent.Torrent;
import org.gudy.azureus2.plugins.ui.UIInstance;
import org.gudy.azureus2.plugins.ui.UIManager;
import org.gudy.azureus2.plugins.ui.UIManagerListener;
import org.gudy.azureus2.plugins.ui.config.BooleanParameter;
import org.gudy.azureus2.plugins.ui.config.ConfigSection;
import org.gudy.azureus2.plugins.ui.config.IntParameter;
import org.gudy.azureus2.plugins.ui.config.Parameter;
import org.gudy.azureus2.plugins.ui.config.ParameterListener;
import org.gudy.azureus2.plugins.ui.menus.MenuItem;
import org.gudy.azureus2.plugins.ui.menus.MenuItemListener;
import org.gudy.azureus2.plugins.ui.menus.MenuManager;
import org.gudy.azureus2.plugins.ui.model.BasicPluginConfigModel;
import org.gudy.azureus2.plugins.ui.sidebar.SideBarCloseListener;
import org.gudy.azureus2.plugins.ui.sidebar.SideBarEntry;
import org.gudy.azureus2.plugins.ui.sidebar.SideBarVitalityImage;
import org.gudy.azureus2.plugins.ui.tables.TableContextMenuItem;
import org.gudy.azureus2.plugins.ui.tables.TableManager;
import org.gudy.azureus2.plugins.ui.tables.TableRow;
import org.gudy.azureus2.pluginsimpl.local.PluginInitializer;
import org.gudy.azureus2.ui.swt.Utils;
import org.gudy.azureus2.ui.swt.plugins.UISWTInstance;

import com.aelitis.azureus.core.AzureusCore;
import com.aelitis.azureus.core.AzureusCoreFactory;
import com.aelitis.azureus.core.AzureusCoreRunningListener;
import com.aelitis.azureus.core.content.ContentException;
import com.aelitis.azureus.core.content.RelatedContent;
import com.aelitis.azureus.core.content.RelatedContentLookupListener;
import com.aelitis.azureus.core.content.RelatedContentManager;
import com.aelitis.azureus.core.content.RelatedContentManagerListener;
import com.aelitis.azureus.core.util.CopyOnWriteList;

import com.aelitis.azureus.ui.UIFunctions;
import com.aelitis.azureus.ui.UIFunctionsManager;
import com.aelitis.azureus.ui.common.viewtitleinfo.ViewTitleInfo;
import com.aelitis.azureus.ui.common.viewtitleinfo.ViewTitleInfoManager;
import com.aelitis.azureus.ui.swt.views.skin.SkinView;
import com.aelitis.azureus.ui.swt.views.skin.SkinViewManager;
import com.aelitis.azureus.ui.swt.views.skin.SkinViewManager.SkinViewManagerListener;
import com.aelitis.azureus.ui.swt.views.skin.sidebar.SideBar;
import com.aelitis.azureus.ui.swt.views.skin.sidebar.SideBarEntrySWT;

public class 
RelatedContentUI 
{		
	private static RelatedContentUI	singleton;
	
	public static synchronized RelatedContentUI
	getSingleton()
	{
		if ( singleton == null ){
			
			singleton = new RelatedContentUI();
		}
		
		return( singleton );
	}
	
	private PluginInterface		plugin_interface;
	private UIManager			ui_manager;
	
	private RelatedContentManager	manager;
	
	private boolean			ui_setup;
	private SideBar			side_bar;
	private boolean			root_menus_added;
	private MainViewInfo 	main_view_info;
	
	
	private ByteArrayHashMap<RCMItem>	rcm_item_map = new ByteArrayHashMap<RCMItem>();
	
	private AsyncDispatcher	async_dispatcher = new AsyncDispatcher();
	
	public 
	RelatedContentUI()
	{
		plugin_interface = PluginInitializer.getDefaultInterface();
		
		ui_manager = plugin_interface.getUIManager();

		ui_manager.addUIListener(
				new UIManagerListener()
				{
					public void
					UIAttached(
							UIInstance		instance )
					{
						if ( instance instanceof UISWTInstance ){

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
	
	private void 
	uiAttachedAndCoreRunning(
		AzureusCore core ) 
	{
		Utils.execSWTThread(
			new AERunnable() 
			{
				public void 
				runSupport() 
				{
					SideBar sideBar = (SideBar) SkinViewManager.getByClass(SideBar.class);
					
					if ( sideBar != null ){
						
						setupUI(sideBar);
						
					} else {
						
						SkinViewManager.addListener(
							new SkinViewManagerListener() 
							{
								public void 
								skinViewAdded(
									SkinView skinview ) 
								{
									if (skinview instanceof SideBar) {
									
										setupUI((SideBar) skinview);
										
										SkinViewManager.RemoveListener(this);
									}
								}
							});
					}
				}
			});
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

		try{	
			manager 	= RelatedContentManager.getSingleton();

			if ( RelatedContentManager.DISABLE_ALL_UI ){
				
				return;
			}
			
			BasicPluginConfigModel config_model = 
				ui_manager.createBasicPluginConfigModel(
					ConfigSection.SECTION_ROOT, "Associations");
			
			final BooleanParameter enabled = 
				config_model.addBooleanParameter2( 
					"rcm.config.enabled", "rcm.config.enabled",
					manager.isUIEnabled());
			
			enabled.addListener(
					new ParameterListener()
					{
						public void 
						parameterChanged(
							Parameter param) 
						{
							manager.setUIEnabled( enabled.getValue());
							
							buildSideBar();
						}
					});
			
			final IntParameter max_results = 
				config_model.addIntParameter2( 
					"rcm.config.max_results", "rcm.config.max_results",
					manager.getMaxResults());
			
			max_results.addListener(
					new ParameterListener()
					{
						public void 
						parameterChanged(
							Parameter param) 
						{
							manager.setMaxResults( max_results.getValue());
						}
					});
			
			final IntParameter max_level = 
				config_model.addIntParameter2( 
					"rcm.config.max_level", "rcm.config.max_level",
					manager.getMaxSearchLevel());
			
			max_level.addListener(
					new ParameterListener()
					{
						public void 
						parameterChanged(
							Parameter param) 
						{
							manager.setMaxSearchLevel( max_level.getValue());
						}
					});
			
			enabled.addEnabledOnSelection( max_results );
			enabled.addEnabledOnSelection( max_level );
			
			main_view_info = new MainViewInfo();

			hookMenus();
						
			buildSideBar();
			
			manager.addListener(
				new RelatedContentManagerListener()
				{
					private int last_unread;
					
					public void
					contentFound(
						RelatedContent[]	content )
					{
						check();
					}

					public void
					contentChanged(
						RelatedContent[]	content )
					{
						contentChanged();
					}
					
					public void 
					contentChanged() 
					{
						check();
						
						List<RCMItem>	items;
						
						synchronized( RelatedContentUI.this ){
							
							items = new ArrayList<RCMItem>( rcm_item_map.values());
						}
						
						for ( RCMItem item: items ){
							
							item.updateNumUnread();
						}
					}
					
					public void 
					contentRemoved(
						RelatedContent[] content ) 
					{
						check();
						
						List<RCMItem>	items;
						
						synchronized( RelatedContentUI.this ){
							
							items = new ArrayList<RCMItem>( rcm_item_map.values());
						}
						
						for ( RCMItem item: items ){
							
							item.contentRemoved( content );
						}
					}
					
					public void
					contentReset()
					{
						check();
					}
					
					protected void
					check()
					{
						int	unread = manager.getNumUnread();
						
						synchronized( this ){
							
							if ( unread == last_unread ){
								
								return;
							}
							
							last_unread = unread;
						}
						
						ViewTitleInfoManager.refreshTitleInfo( main_view_info );
					}
				});
			
		}catch( Throwable e ){
			
			Debug.out( e );
		}
	}
	
	protected void
	hookMenus()
	{
		TableManager	table_manager = plugin_interface.getUIManager().getTableManager();

		String[]	table_ids = {
				TableManager.TABLE_MYTORRENTS_INCOMPLETE,
				TableManager.TABLE_MYTORRENTS_COMPLETE,
				TableManager.TABLE_MYTORRENTS_ALL_BIG,
				TableManager.TABLE_MYTORRENTS_COMPLETE_BIG,
				TableManager.TABLE_MYTORRENTS_INCOMPLETE_BIG,
		};
		
		for ( String table_id: table_ids ){
			
			TableContextMenuItem menu_item = table_manager.addContextMenuItem( table_id, "rcm.contextmenu.lookupassoc");
		
			menu_item.setStyle( TableContextMenuItem.STYLE_PUSH );

			MenuItemListener listener = 
				new MenuItemListener()
				{
					public void 
					selected(
						MenuItem 	menu, 
						Object 		target) 
					{
						TableRow[]	rows = (TableRow[])target;
						
						if ( rows.length > 0 ){
							
							Download download = (Download)rows[0].getDataSource();
							
							explicitSearch( download );
						}
					}
				};
				
				menu_item.addMultiListener( listener );
		}
	}
	
	protected void
	explicitSearch(
		Download		download )
	{
		addSearch( download );
	}
	
	protected void
	buildSideBar()
	{		
		final String parent_id = "sidebar." + SideBar.SIDEBAR_SECTION_RELATED_CONTENT;

		final SideBarEntrySWT main_sb_entry = SideBar.getEntry( SideBar.SIDEBAR_SECTION_RELATED_CONTENT );

		if ( main_sb_entry != null ){
				
			SideBarEntrySWT subs_entry = SideBar.getEntry( SideBar.SIDEBAR_SECTION_SUBSCRIPTIONS );

			int index = side_bar.getIndexOfEntryRelativeToParent( subs_entry );
			
			if ( index >= 0 ){
				
				index++;
			}
			
			if ( main_sb_entry.getTreeItem() == null ){
				
				if ( manager.isUIEnabled()){
										
					side_bar.createEntryFromSkinRef(
							null,
							SideBar.SIDEBAR_SECTION_RELATED_CONTENT, "rcmview",
							main_view_info.getTitle(),
							main_view_info, null, false, index  );
					
					main_sb_entry.setImageLeftID( "image.sidebar.rcm" );
					
					main_sb_entry.setDatasource(
						new RelatedContentEnumerator()
						{
							private RelatedContentManagerListener base_listener;
							
							private RelatedContentEnumeratorListener current_listener;
							
							public void
							enumerate(
								RelatedContentEnumeratorListener	listener )
							{
								current_listener = listener;
								
								if ( base_listener == null ){
									
									base_listener = 
										new RelatedContentManagerListener()
										{
											public void
											contentFound(
												RelatedContent[]	content )
											{
												current_listener.contentFound( content );
											}
											
											public void
											contentChanged(
												RelatedContent[]	content )
											{
											}
											
											public void 
											contentRemoved(
												RelatedContent[] 	content ) 
											{
											}
											
											public void 
											contentChanged() 
											{
											}
											
											public void
											contentReset()
											{
											}
										};
										
									manager.addListener( base_listener );
								}
								
								RelatedContent[] current_content = manager.getRelatedContent();
								
								listener.contentFound( current_content );
							}
						});
				}else{
					
					return;
				}
			}else if ( !manager.isUIEnabled()){
				
				main_sb_entry.getTreeItem().dispose();
				
				return;
			}
			
			if ( !root_menus_added ){
				
				root_menus_added = true;
				
				MenuManager menu_manager = ui_manager.getMenuManager();
	
				MenuItem menu_item = menu_manager.addMenuItem( parent_id, "v3.activity.button.readall" );
				
				menu_item.addListener( 
						new MenuItemListener() 
						{
							public void 
							selected(
								MenuItem menu, Object target ) 
							{
						      	manager.setAllRead();
							}
						});
				
				menu_item = menu_manager.addMenuItem( parent_id, "Subscription.menu.deleteall");
				
				menu_item.addListener(
						new MenuItemListener() 
						{
							public void 
							selected(
								MenuItem menu, Object target ) 
							{
						      	manager.deleteAll();
							}
						});
				
				menu_item = menu_manager.addMenuItem( parent_id, "Subscription.menu.reset" );
				
				menu_item.addListener( 
						new MenuItemListener() 
						{
							public void 
							selected(
								MenuItem menu, Object target ) 
							{
								for ( RCMItem item: rcm_item_map.values()){
									
									item.getTreeItem().dispose();
								}
								
						      	manager.reset();
							}
						});
				
				
				menu_item = menu_manager.addMenuItem( parent_id, "sep" );

				menu_item.setStyle( MenuItem.STYLE_SEPARATOR );
				
				menu_item = menu_manager.addMenuItem( parent_id, "ConfigView.title.short" );
				
				menu_item.addListener( 
						new MenuItemListener() 
						{
							public void 
							selected(
								MenuItem menu, Object target ) 
							{
						      	 UIFunctions uif = UIFunctionsManager.getUIFunctions();
						      	 
						      	 if ( uif != null ){
						      		 
						      		 uif.openView( UIFunctions.VIEW_CONFIG, "Associations" );
						      	 }
							}
						});
			}
		}
	}
	
	protected void
	addSearch(
		final Download		download )
	{
		Torrent	torrent = download.getTorrent();
		
		if ( torrent == null ){
			
			return;
		}
		
		final byte[] hash = torrent.getHash();
		
		addSearch( hash, download.getName());
	}
	
	protected void
	addSearch(
		final byte[]		hash,
		final String		name )
	{
		synchronized( this ){
			
			final RCMItem existing_si = rcm_item_map.get( hash );
			
			if (  existing_si == null ){
	
				final RCMItem new_si = new RCMItem( hash );
				
				rcm_item_map.put( hash, new_si );
				
				Utils.execSWTThread(
					new Runnable()
					{
						public void
						run()
						{
							synchronized( RelatedContentUI.this ){

								if ( new_si.isDestroyed()){
									
									return;
								}
								
								RCMView view = new RCMView( SideBar.SIDEBAR_SECTION_RELATED_CONTENT, name );
								
								new_si.setView( view );
								
								String key = "RCM_" + ByteFormatter.encodeString( hash );
								
								SideBarEntrySWT	entry = side_bar.createEntryFromSkinRef(
										SideBar.SIDEBAR_SECTION_RELATED_CONTENT,
										key, "rcmview",
										view.getTitle(),
										view, null, true, -1 );
								
								new_si.setTreeItem( entry.getTreeItem(), entry );
								
								/*
								TreeItem  tree_item = 
									side_bar.createTreeItemFromIView(
										SideBar.SIDEBAR_SECTION_RELATED_CONTENT, 
										view,
										key, 
										null, 
										true, 
										true,
										false );
										
									SideBarEntrySWT	entry = SideBar.getEntry( key );
																
									new_si.setTreeItem( tree_item, entry );
								*/
																
									
								/*
								PluginInterface pi = PluginInitializer.getDefaultInterface();
								UIManager uim = pi.getUIManager();
								MenuManager menuManager = uim.getMenuManager();
								
								MenuItem menuItem;
								*/
								
								new_si.activate();
							}
						}
					});
			}else{
				
				Utils.execSWTThread(
						new Runnable()
						{
							public void
							run()
							{
								ViewTitleInfoManager.refreshTitleInfo( existing_si.getView());
								
								SideBarEntrySWT mainSBEntry = SideBar.getEntry(SideBar.SIDEBAR_SECTION_RELATED_CONTENT );
								
								if ( mainSBEntry != null ){
									
									ViewTitleInfoManager.refreshTitleInfo( mainSBEntry.getTitleInfo());
								}
								
								existing_si.activate();
							}
						});
			}
		}
	}
	
	protected class
	MainViewInfo
		implements 	ViewTitleInfo
	{
		protected
		MainViewInfo()
		{
		}
		
		public Object 
		getTitleInfoProperty(
			int propertyID ) 
		{		
			if ( propertyID == TITLE_TEXT ){
				
				return( getTitle());
				
			}else if ( propertyID == TITLE_INDICATOR_TEXT ){
				
				int	 unread = manager.getNumUnread();
				
				if ( unread > 0 ){
				
					return( String.valueOf( unread ));
				}
				
			}else if ( propertyID == TITLE_INDICATOR_COLOR ){
	
			}
			
			return null;
		}
		
		public String
		getTitle()
		{
			return( MessageText.getString("rcm.view.title"));
		}
	}
	
	protected class
	RCMView
		implements 	ViewTitleInfo
	{
		private String			parent_key;
		private String			name;
		
		private int				num_unread;
		
		protected
		RCMView(
			String			_parent_key,
			String			_name )
		{
			parent_key	= _parent_key;
			name		= _name;
		}
		
		public Object 
		getTitleInfoProperty(
			int propertyID ) 
		{		
			if ( propertyID == TITLE_TEXT ){
				
				return( getTitle());
				
			}else if ( propertyID == TITLE_INDICATOR_TEXT ){
				
				if ( num_unread > 0 ){
				
					return( String.valueOf( num_unread ));
				}
				
			}else if ( propertyID == TITLE_INDICATOR_COLOR ){
	
			}
			
			return null;
		}
		
		public String
		getTitle()
		{
			return( name );
		}
		
		protected void
		setNumUnread(
			int	n )
		{
			num_unread = n;
						
			ViewTitleInfoManager.refreshTitleInfo( this );
		}
	}
	
	private static final String SPINNER_IMAGE_ID 	= "image.sidebar.vitality.dl";

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
	
	public class
	RCMItem
		implements RelatedContentEnumerator, SideBarCloseListener
	{	
		private byte[]				hash;
		
		private RCMView				view;
		private SideBarEntrySWT		sb_entry;
		private TreeItem			tree_item;
		private boolean				destroyed;
		
		private SideBarVitalityImage	spinner;
		
		private List<RelatedContent>	content_list = new ArrayList<RelatedContent>();
		
		private int	num_unread;
		
		private CopyOnWriteList<RelatedContentEnumeratorListener>	listeners = new CopyOnWriteList<RelatedContentEnumeratorListener>();
		
		private boolean	lookup_complete;
		
		protected
		RCMItem(
			byte[]		_hash )
		{
			hash		= _hash;
		}
		
		protected void
		setTreeItem(
			TreeItem		_tree_item,
			SideBarEntrySWT	_sb_entry )
		{
			tree_item	= _tree_item;
			sb_entry	= _sb_entry;
			
			sb_entry.setDatasource( this );
			
			sb_entry.addListener( this );
			
			spinner = sb_entry.addVitalityImage( SPINNER_IMAGE_ID );
			
			try{
				showIcon( spinner, null );
				
				manager.lookupContent(
					hash,
					new RelatedContentLookupListener()
					{
						public void
						lookupStart()
						{
						}
						
						public void
						contentFound(
							RelatedContent[]	content )
						{
							synchronized( RCMItem.this ){
							
								if ( !destroyed ){
								
									for ( RelatedContent c: content ){
									
										if ( !content_list.contains( c )){
										
											content_list.add( c );
										}
									}
								}
							}
							
							updateNumUnread();
							
							for ( RelatedContentEnumeratorListener listener: listeners ){
								
								try{
									listener.contentFound( content );
									
								}catch( Throwable e ){
									
									Debug.out( e );
								}
							}
						}
						
						public void
						lookupComplete()
						{	
							synchronized( RCMItem.this ){
								
								lookup_complete = true;
							}
							
							hideIcon( spinner );
						}
						
						public void
						lookupFailed(
							ContentException e )
						{	
							lookupComplete();
						}
					});
			}catch( Throwable e ){
				
				lookup_complete = true;
				
				Debug.out( e );
				
				hideIcon( spinner );
			}
		}
		
		protected void 
		contentRemoved(
			RelatedContent[] content ) 
		{
			boolean deleted = false;
			
			synchronized( RCMItem.this ){
									
				for ( RelatedContent c: content ){
						
					if ( content_list.remove( c )){
														
						deleted = true;
					}
				}
			}
			
			if ( deleted ){
			
				updateNumUnread();
			}
		}
		
		protected void
		updateNumUnread()
		{
			synchronized( RCMItem.this ){
				
				int	num = 0;
				
				for ( RelatedContent c: content_list ){
					
					if ( c.isUnread()){
						
						num++;
					}
				}
				
				if ( num != num_unread ){
					
					num_unread = num;
					
					final int f_num = num;
										
					async_dispatcher.dispatch(
						new AERunnable()
						{
							public void
							runSupport()
							{
								if ( async_dispatcher.getQueueSize() > 0 ){
									
									return;
								}
								
								view.setNumUnread( f_num );
							}
						});
				}
			}
		}
		
		public void
		enumerate(
			final RelatedContentEnumeratorListener	listener )
		{
			RelatedContent[]	already_found;
			 
			synchronized( this ){
				
				if ( !lookup_complete ){
					
					listeners.add( listener );
				}
				
				already_found = content_list.toArray( new RelatedContent[ content_list.size()]);
			}
			
			if ( already_found.length > 0 ){
				
				listener.contentFound( already_found );
			}
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
			RCMView		_view )
		{
			view	= _view;
		}
		
		protected RCMView
		getView()
		{
			return( view );
		}
		
		protected boolean
		isDestroyed()
		{
			return( destroyed );
		}
		
		public void 
		sidebarClosed(
			SideBarEntry entry )
		{
			destroy();
		}
		
		protected void
		destroy()
		{
			synchronized( this ){
			
				content_list.clear();
				
				destroyed = true;
			}
			
			synchronized( RelatedContentUI.this ){
					
				rcm_item_map.remove( hash );
			}
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
