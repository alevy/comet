/*
 * File    : Main.java
 * Created : 04-Feb-2004
 * By      : parg
 * 
 * Azureus - a Java Bittorrent client
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details ( see the LICENSE file ).
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package org.gudy.azureus2.ui.jws;

/**
 * @author parg
 *
 */

import java.net.URL;
import java.util.Enumeration;
import java.util.Properties;

import org.gudy.azureus2.core3.util.AEMonitor;
import org.gudy.azureus2.core3.util.AESemaphore;
import org.gudy.azureus2.core3.util.AEThread;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.plugins.Plugin;
import org.gudy.azureus2.plugins.PluginEvent;
import org.gudy.azureus2.plugins.PluginEventListener;
import org.gudy.azureus2.plugins.PluginInterface;
import org.gudy.azureus2.plugins.PluginListener;
import org.gudy.azureus2.plugins.PluginManager;
import org.gudy.azureus2.plugins.logging.LoggerChannel;
import org.gudy.azureus2.plugins.logging.LoggerChannelListener;

import com.aelitis.azureus.launcher.Launcher;

public class 
Main 
	implements Plugin, PluginListener, PluginEventListener
{
	protected static Main				singleton;
	
	protected static AESemaphore		init_sem = new AESemaphore("UIJWS");
	private static AEMonitor			class_mon	= new AEMonitor( "UIJWS" );

	public static Main
	getSingleton(
		final String[]	args )
	{
		try{
			class_mon.enter();
		
			if ( singleton == null ){
				
				new AEThread( "plugin initialiser ")
				{
					public void
					runSupport()
					{
						PluginManager.registerPlugin( Main.class );
						
						Properties props = new Properties();
						
						props.put( PluginManager.PR_MULTI_INSTANCE, "false" );
											
						PluginManager.startAzureus( PluginManager.UI_SWT, props );
					}
				}.start();
				
				init_sem.reserve();
			}
			
			return( singleton );
			
		}finally{
			
			class_mon.exit();
		}
	}	
	
	protected PluginInterface		plugin_interface;
	protected LoggerChannel 		log;
	protected AESemaphore			ready_sem	= new AESemaphore("UIJWSReady");

	public void 
	initialize(
		PluginInterface _pi )
	{	
		plugin_interface = _pi;

		singleton	= this;
		
		log = plugin_interface.getLogger().getChannel("JWS Launcher");
		
		log.addListener(
			new LoggerChannelListener()
			{
				public void
				messageLogged(
					int		type,
					String	content )
				{
					System.out.println( content );
				}
				
				public void
				messageLogged(
					String		str,
					Throwable	error )
				{
					System.out.println( str );
					
					Debug.printStackTrace( error );
				}
			});
		
		log.log(LoggerChannel.LT_INFORMATION, "Plugin Initialised");
		
		plugin_interface.addListener( this );
		
		plugin_interface.addEventListener( this );
		
		init_sem.release();
	}
	
	public void
	initializationComplete()
	{
	}
	
	public void
	handleEvent(
		PluginEvent	ev )
	{
		System.out.println( "PluginEvent:" + ev.getType());
		
		if ( ev.getType() == PluginEvent.PEV_CONFIGURATION_WIZARD_COMPLETES ){
			
			ready_sem.release();
		}
	}
	
	protected void
	process()
	{
		ready_sem.reserve();
		
		log.log(LoggerChannel.LT_INFORMATION, "processing jws request" );
		
		Properties props = System.getProperties();
		
		Enumeration enumx = props.keys();
		
		while( enumx.hasMoreElements()){
		
			String	key = (String)enumx.nextElement();
			
			log.log(LoggerChannel.LT_INFORMATION, "\t" + key + " = '" + props.get(key) + "'");
		}
		
		String	torrent_url = (String)props.get( "azureus.javaws.torrent_url");
		
		log.log(LoggerChannel.LT_INFORMATION, "Torrent URL = " + torrent_url );
		
		if ( torrent_url != null ){
			
			try{
				plugin_interface.getDownloadManager().addDownload(new URL(torrent_url));
				
			}catch( Throwable e ){
							
				log.log( e );
			}
		}
		
	}
	
	public void
	closedownInitiated()
	{
	}
	
	public void
	closedownComplete()
	{
	}
	
	public static void
	main(
		String[]		args )
	{
		if(Launcher.checkAndLaunch(Main.class, args))
			return;
		
		getSingleton( args ).process();
	}
}
