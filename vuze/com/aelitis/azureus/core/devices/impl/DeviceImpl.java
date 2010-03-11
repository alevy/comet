/*
 * Created on Jan 28, 2009
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


package com.aelitis.azureus.core.devices.impl;

import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.lang.reflect.Constructor;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.*;

import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.util.AERunnable;
import org.gudy.azureus2.core3.util.BEncoder;
import org.gudy.azureus2.core3.util.ByteFormatter;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.core3.util.DelayedEvent;
import org.gudy.azureus2.core3.util.FileUtil;
import org.gudy.azureus2.core3.util.IndentWriter;
import org.gudy.azureus2.core3.util.LightHashMap;
import org.gudy.azureus2.core3.util.SystemProperties;
import org.gudy.azureus2.core3.util.SystemTime;
import org.gudy.azureus2.plugins.disk.DiskManagerFileInfo;

import com.aelitis.azureus.core.devices.Device;
import com.aelitis.azureus.core.devices.DeviceListener;
import com.aelitis.azureus.core.devices.DeviceMediaRenderer;
import com.aelitis.azureus.core.devices.TranscodeException;
import com.aelitis.azureus.core.devices.TranscodeFile;
import com.aelitis.azureus.core.devices.TranscodeProfile;
import com.aelitis.azureus.core.devices.TranscodeProvider;
import com.aelitis.azureus.core.devices.TranscodeTarget;
import com.aelitis.azureus.core.devices.TranscodeTargetListener;
import com.aelitis.azureus.core.util.CopyOnWriteList;
import com.aelitis.azureus.util.ImportExportUtils;

public abstract class 
DeviceImpl
	implements Device
{
	private static final String MY_PACKAGE = "com.aelitis.azureus.core.devices.impl";
	
	protected static DeviceImpl
	importFromBEncodedMapStatic(
		DeviceManagerImpl	manager,
		Map					map )
	
		throws IOException
	{
		String	impl = ImportExportUtils.importString( map, "_impl" );
		
		if ( impl.startsWith( "." )){
			
			impl = MY_PACKAGE + impl;
		}
		
		try{
			Class<DeviceImpl> cla = (Class<DeviceImpl>) Class.forName( impl );
			
			Constructor<DeviceImpl> cons = cla.getDeclaredConstructor( DeviceManagerImpl.class, Map.class );
			
			cons.setAccessible( true );
			
			return( cons.newInstance( manager, map ));
			
		}catch( Throwable e ){

			Debug.out( "Can't construct device for " + impl, e );
			
			throw( new IOException( "Construction failed: " + Debug.getNestedExceptionMessage(e)));
		}
	}
	
	private static final String PP_REND_WORK_DIR		= "tt_work_dir"; 
	private static final String PP_REND_DEF_TRANS_PROF	= "tt_def_trans_prof";
	private static final String PP_REND_TRANS_REQ		= "tt_req";
	private static final String PP_REND_TRANS_CACHE		= "tt_always_cache";
	private static final String PP_REND_RSS_PUB			= "tt_rss_pub";
	
	protected static final String PP_REND_SHOW_CAT		= "tt_show_cat";
	
	protected static final String	PP_IP_ADDRESS 		= "rend_ip";	
	protected static final String	TP_IP_ADDRESS 		= "DeviceUPnPImpl:ip";	// transient
	protected static final String	PP_FILTER_FILES 	= "rend_filter";
	
	protected static final String	PP_COPY_OUTSTANDING = "copy_outstanding";
	protected static final String	PP_AUTO_START		= "auto_start";
	
	
	protected static final String	PP_COPY_TO_FOLDER	= "copy_to_folder";
	protected static final String	PP_AUTO_COPY		= "auto_copy";
	
	protected static final String	PP_LIVENESS_DETECTABLE	= "live_det";

	protected static final String	PP_TIVO_MACHINE		= "tivo_machine";

	protected static final String	PP_OD_ENABLED			= "od_enabled";
	protected static final String	PP_OD_SHOWN_FTUX		= "od_shown_ftux";
	protected static final String	PP_OD_MANUFACTURER		= "od_manufacturer";
	protected static final String	PP_OD_STATE_CACHE		= "od_state_cache";
	protected static final String	PP_OD_XFER_CACHE		= "od_xfer_cache";
	protected static final String	PP_OD_UPNP_DISC_CACHE	= "od_upnp_cache";

	
	protected static final boolean	PR_AUTO_START_DEFAULT	= true;
	protected static final boolean	PP_AUTO_COPY_DEFAULT	= false;
	
	private static final String	GENERIC = "generic";
	
	
	private DeviceManagerImpl	manager;
	private int					type;
	private String				uid;
	private String				secondary_uid;
	private String 				classification;
	private String				name;
	private boolean				manual;
	
	private boolean			hidden;
	private long			last_seen;
	
	private int				busy_count;
	private boolean			online;
	
	private boolean			transcoding;
	
	private Map<String,Object>	persistent_properties 	= new LightHashMap<String, Object>(1);

	private Map<Object,Object>	transient_properties 	= new LightHashMap<Object, Object>(1);
	
	private long						device_files_last_mod;
	private boolean						device_files_dirty;
	private Map<String,Map<String,?>>	device_files;
	
	private WeakReference<Map<String,Map<String,?>>> device_files_ref;
	
	private CopyOnWriteList<TranscodeTargetListener>	listeners = new CopyOnWriteList<TranscodeTargetListener>();
	
	private Map<Object,String>	errors 	= new HashMap<Object, String>();
	private Map<Object,String>	infos	= new HashMap<Object, String>();
	
	private CopyOnWriteList<DeviceListener>		device_listeners;
	
	protected
	DeviceImpl(
		DeviceManagerImpl	_manager,
		int					_type,
		String				_uid,
		String				_classification,
		boolean				_manual )
	{
		this( _manager, _type, _uid, _classification, _manual, _classification );
	}
	
	protected
	DeviceImpl(
		DeviceManagerImpl	_manager,
		int					_type,
		String				_uid,
		String				_classification,
		boolean				_manual,
		String				_name )
	{
		manager			= _manager;
		type			= _type;
		uid				= _uid;
		classification	= _classification;
		name			= _name;
		manual			= _manual;
	}
	
	protected
	DeviceImpl(
		DeviceManagerImpl	_manager,
		Map					map )
	
		throws IOException
	{
		manager	= _manager;
		
		type			= (int)ImportExportUtils.importLong( map, "_type" );
		uid				= ImportExportUtils.importString( map, "_uid" );
		classification	= ImportExportUtils.importString( map, "_name" );
		name			= ImportExportUtils.importString( map, "_lname" );
		
		if ( name == null ){
			
			name = classification;
		}
		
		secondary_uid		= ImportExportUtils.importString( map, "_suid" );

		last_seen	= ImportExportUtils.importLong( map, "_ls" );
		hidden		= ImportExportUtils.importBoolean( map, "_hide" );	
		manual		= ImportExportUtils.importBoolean( map, "_man" );

		if ( map.containsKey( "_pprops" )){
			
			persistent_properties = (Map<String,Object>)map.get( "_pprops" );
		}
	}
	
	protected void
	exportToBEncodedMap(
		Map					map )
	
		throws IOException
	{
		String	cla = this.getClass().getName();
		
		if ( cla.startsWith( MY_PACKAGE )){
			
			cla = cla.substring( MY_PACKAGE.length());
		}
		
		ImportExportUtils.exportString( map, "_impl", cla );
		ImportExportUtils.exportLong( map, "_type", new Long( type ));
		ImportExportUtils.exportString( map, "_uid", uid );
		ImportExportUtils.exportString( map, "_name", classification );
		ImportExportUtils.exportString( map, "_lname", name );
		
		if ( secondary_uid != null ){
			
			ImportExportUtils.exportString( map, "_suid", secondary_uid );
		}
		
		ImportExportUtils.exportLong( map, "_ls", new Long( last_seen ));
		ImportExportUtils.exportBoolean( map, "_hide", hidden );
		ImportExportUtils.exportBoolean( map, "_man", manual );
		
		map.put( "_pprops", persistent_properties );
	}
	
	protected boolean
	updateFrom(
		DeviceImpl		other,
		boolean			is_alive )
	{
		if ( type != other.type ){
			
			Debug.out( "Inconsistent update operation (type)" );
			
			return( false );			
		}
		
		String	o_uid 	= other.uid;
		String	o_suid	= other.secondary_uid;
		
		if ( !uid.equals( o_uid )){
			
			boolean borked = false;
			
			if ( secondary_uid == null && o_suid == null ){
			
				borked = true;
			
			}else if ( 	secondary_uid == null && uid.equals( o_suid ) ||
						o_suid == null && o_uid.equals( secondary_uid ) ||
						o_suid.equals( secondary_uid )){
				
			}else{
				
				borked = true;
			}
			
			if ( borked ){
				
				Debug.out( "Inconsistent update operation (uids)" );
				
				return( false );

			}
		}
		
		if ( !classification.equals( other.classification )){
			
			classification	= other.classification;
			
			setDirty();
		}
		
		/* don't overwite the name as user may have altered it!
		if ( !name.equals( other.name )){
			
			name	= other.name;
			
			setDirty();
		}
		*/
		
		if ( manual != other.manual ){
			
			manual	= other.manual;
			
			setDirty();
		}
		
		if ( is_alive ){
		
			alive();
		}
		
		return( true );
	}
	
	protected void
	initialise()
	{
		updateStatus( 0 );
	}
	
	protected void
	destroy()
	{	
	}
	
	public int
	getType()
	{
		return( type );
	}
	
	public String
	getID()
	{
		return( uid );
	}
	
	protected void
	setSecondaryID(
		String		str )
	{
		secondary_uid = str;
	}
	
	protected String
	getSecondaryID()
	{
		return( secondary_uid );
	}
	
	public Device
	getDevice()
	{
		return( this );
	}

	public String
	getName()
	{
		return( name );
	}
	
	public void 
	setName(
		String _name ) 
	{
		if ( !name.equals( _name )){
			
			name = _name;
			
			setDirty();
		}
	}
	
	public String
	getClassification()
	{
		return( classification );
	}
	
	public String
	getShortDescription()
	{
		if ( getRendererSpecies() == DeviceMediaRenderer.RS_ITUNES ){
			
			return( "iPhone, iPod, Apple TV" );
		}
		
		return( null );
	}
	
	public int
	getRendererSpecies()
	{
			// note, overridden in itunes
		
		if ( classification.equalsIgnoreCase( "PS3" )){
			
			return( DeviceMediaRenderer.RS_PS3 );
			
		}else if ( classification.equalsIgnoreCase( "XBox 360" )){
			
			return( DeviceMediaRenderer.RS_XBOX );
		
		}else if ( classification.equalsIgnoreCase( "Wii" )){

			return( DeviceMediaRenderer.RS_WII );
			
		}else if ( classification.equalsIgnoreCase( "Browser" )){

			return( DeviceMediaRenderer.RS_BROWSER );
			
		}else{
			
			return( DeviceMediaRenderer.RS_OTHER );
		}	
	}
	
	protected String
	getDeviceClassification()
	{
			// note, overridden in itunes
		
			// bit of a mess here. First release used name as classification and mapped to 
			// species + device-classification here.
			// second release moved to separate name and classification and used the correct
			// device-classification as the classification
			// so we deal with both for the moment...
			// 'generic' means one we don't explicitly support, which are rendereres discovered
			// by UPnP
		
		switch( getRendererSpecies()){
		
			case DeviceMediaRenderer.RS_PS3:{
				
				return( "sony.PS3" );
			}
			case DeviceMediaRenderer.RS_XBOX:{
				
				return( "microsoft.XBox" );
			}
			case DeviceMediaRenderer.RS_WII:{
				
				return( "nintendo.Wii" );
			}
			case DeviceMediaRenderer.RS_BROWSER:{
				
				return( "browser.generic" );
			}
			case DeviceMediaRenderer.RS_OTHER:{
				
				if ( isManual()){
					
					return( classification );
				}
				
				if ( 	classification.equals( "sony.PSP" ) ||
						classification.startsWith( "tivo." )){
					
					return( classification );	
				}
	
				return( GENERIC );
			}
			default:{
				Debug.out( "Unknown classification" );
				
				return( GENERIC );
			}
		}
	}
	
	public boolean
	isGeneric()
	{
		return( getDeviceClassification() == GENERIC );
	}
	
	public boolean
	isManual()
	{
		return( manual );
	}
	
	public boolean
	isHidden()
	{
		return( hidden );
	}
	
	public void
	setHidden(
		boolean		h )
	{
		if ( h != hidden ){
			
			hidden	= h;
			
			setDirty();
		}
	}
	
	protected void
	alive()
	{
		last_seen	= SystemTime.getCurrentTime();
			
		if ( !online ){
		
			online	= true;
			
			setDirty( false );
		}
	}
	
	public boolean
	isLivenessDetectable()
	{
		return( !manual );
	}
	
	public boolean
	isAlive()
	{
		return( online );
	}
	
	protected void
	dead()
	{		
		if ( online ){
		
			online	= false;
			
			setDirty( false );
		}
	}
	
	protected void
	setDirty()
	{
		setDirty( true );
	}
	
	protected void
	setDirty(
		boolean		save_changes )
	{
		manager.configDirty( this, save_changes );
	}
	
	protected void
	updateStatus(
		int		tick_count )
	{	
	}
	
	public void 
	requestAttention() 
	{
		manager.requestAttention( this );
	}
	
	public TranscodeFileImpl[]
	getFiles()
	{
		try{
			synchronized( this ){
				
				if ( device_files == null ){
					
					loadDeviceFile();
				}
				
				List<TranscodeFile> result = new ArrayList<TranscodeFile>();
								
				Iterator<Map.Entry<String,Map<String,?>>> it = device_files.entrySet().iterator();
					
				while( it.hasNext()){
					
					Map.Entry<String,Map<String,?>> entry = it.next();
					
					try{
						TranscodeFileImpl tf = new TranscodeFileImpl( this, entry.getKey(), device_files );
						
						result.add( tf );
						
					}catch( Throwable e ){
						
						it.remove();
						
						log( "Failed to deserialise transcode file", e );
					}
				}
				
				return( result.toArray( new TranscodeFileImpl[ result.size() ]));
			}
		}catch( Throwable e ){
			
			Debug.out( e );
			
			return( new TranscodeFileImpl[0] );
		}
	}
	
	public TranscodeFileImpl
	allocateFile(
		TranscodeProfile		profile,
		boolean					no_xcode,
		DiskManagerFileInfo		file,
		boolean					for_job )
	
		throws TranscodeException
	{
		TranscodeFileImpl	result = null;
		
		try{
			synchronized( this ){
				
				if ( device_files == null ){
					
					loadDeviceFile();
				}
	
				String	key = ByteFormatter.encodeString( file.getDownloadHash() ) + ":" + file.getIndex() + ":" + profile.getUID();
								
				if ( device_files.containsKey( key )){
				
					try{					
						result = new TranscodeFileImpl( this, key, device_files );
						
					}catch( Throwable e ){
						
						device_files.remove( key );
						
						log( "Failed to deserialise transcode file", e );
					}
				}
				
				if ( result == null ){
							
					String ext = profile.getFileExtension();
					
					String	target_file = file.getFile().getName();
					
					if ( ext != null && !no_xcode ){
						
						int	pos = target_file.lastIndexOf( '.' );
						
						if ( pos != -1 ){
							
							target_file = target_file.substring( 0, pos ); 
						}
						
						target_file += ext;
					}
						
					target_file = allocateUniqueFileName( target_file );
					
					File output_file = getWorkingDirectory( true );
	
					output_file = new File( output_file.getAbsoluteFile(), target_file );
	
					result = new TranscodeFileImpl( this, key, profile.getName(), device_files, output_file, for_job );
							
					result.setSourceFile( file );
					
					saveDeviceFile();
					
				}else{
					
					result.setSourceFile( file );
					
					result.setProfileName( profile.getName());
				}
			}
		}catch( Throwable e ){
			
			throw( new TranscodeException( "File allocation failed", e ));
		}
		
		for ( TranscodeTargetListener l: listeners ){
			
			try{
				l.fileAdded( result );
				
			}catch( Throwable e ){
				
				Debug.out( e );
			}
		}
		
		return( result );
	}
	
	protected String
	allocateUniqueFileName(
		String		str )
	{
		Set<String> name_set = new HashSet<String>();
		
		for (Map<String,?> entry: device_files.values()){
			
			try{
				name_set.add( new File( ImportExportUtils.importString( entry, TranscodeFileImpl.KEY_FILE )).getName());
				
			}catch( Throwable e ){
				
				Debug.out( e );
			}
		}

		for (int i=0;i<1024;i++){
			
			String	test_name = i==0?str:( i + "_" + str);
			
			if ( !name_set.contains( test_name )){
			
				str = test_name;
				
				break;
			}				
		}
		
		return( str );
	}
	
	protected void
	revertFileName(
		TranscodeFileImpl	tf )
	
		throws TranscodeException
	{
		File cache_file = tf.getCacheFile();
		
		if ( cache_file.exists()){
			
			Debug.out( "Cache file already allocated, can't rename" );
			
			return;
		}
		
		File source_file = tf.getSourceFile().getFile();
		
		String	original_name = source_file.getName();
		
		int pos = original_name.indexOf('.');
		
		if ( pos == -1 ){
			
			return;
		}
		
		String	cf_name = cache_file.getName();
		
		if ( cf_name.endsWith( original_name.substring(pos))){
			
			return;
		}
		
		try{
			synchronized( this ){
				
				if ( device_files == null ){
					
					loadDeviceFile();
				}
				
				String reverted_name = allocateUniqueFileName( original_name );
				
				tf.setCacheFile( new File( cache_file.getParentFile(), reverted_name ));
			}
		}catch( Throwable e ){
			
			throw( new TranscodeException( "File name revertion failed", e ));
		}
	}
	
	public TranscodeFileImpl
	lookupFile(
		TranscodeProfile		profile,
		DiskManagerFileInfo		file )
	{
		try{
			synchronized( this ){
				
				if ( device_files == null ){
					
					loadDeviceFile();
				}
	
				String	key = ByteFormatter.encodeString( file.getDownloadHash() ) + ":" + file.getIndex() + ":" + profile.getUID();
								
				if ( device_files.containsKey( key )){
				
					try{					
						return( new TranscodeFileImpl( this, key, device_files ));
						
					}catch( Throwable e ){
						
						device_files.remove( key );
						
						log( "Failed to deserialise transcode file", e );
					}
				}
			}
		}catch( Throwable e ){
		}
		
		return( null );
	}
	
	protected TranscodeFileImpl
	getTranscodeFile(
		String		key )
	{
		try{
			synchronized( this ){
				
				if ( device_files == null ){
					
					loadDeviceFile();
				}
									
				if ( device_files.containsKey( key )){
				
					try{					

						return( new TranscodeFileImpl( this, key, device_files ));
						
				}	catch( Throwable e ){
						
						device_files.remove( key );
						
						log( "Failed to deserialise transcode file", e );
					}
				}
			}
		}catch( Throwable e ){
		}
		
		return( null );
	}
	
	public File
	getWorkingDirectory()
	{
		return( getWorkingDirectory( false ));
	}
	
	public File
	getWorkingDirectory(
		boolean	persist )
	{				
		String result = getPersistentStringProperty( PP_REND_WORK_DIR );
		
		if ( result.length() == 0 ){
			
			File f = manager.getDefaultWorkingDirectory( persist );
			
			if ( persist ){
			
				f.mkdirs();
			}
			
			String	name = FileUtil.convertOSSpecificChars( getName(), true );
			
			for (int i=0;i<1024;i++){
				
				String test_name = name + (i==0?"":("_"+i));
				
				File test_file = new File( f, test_name );
				
				if ( !test_file.exists()){
			
					f = test_file;
					
					break;
				}
			}
			
			result = f.getAbsolutePath();
			
			if ( persist ){
			
				setPersistentStringProperty( PP_REND_WORK_DIR, result );
			}
		}
		
		File f_result = new File( result );
		
		if ( !f_result.exists()){
			
			if ( persist ){
			
				f_result.mkdirs();
			}
		}
		
		return( f_result );
	}
	
	public void
	setWorkingDirectory(
		File		directory )
	{
		setPersistentStringProperty( PP_REND_WORK_DIR, directory.getAbsolutePath());
	}

	protected void
	resetWorkingDirectory()
	{
		setPersistentStringProperty( PP_REND_WORK_DIR, "" );
	}
	
	public TranscodeProfile[]
	getTranscodeProfiles()
	{		
		List<TranscodeProfile>	profiles = new ArrayList<TranscodeProfile>();
		
		DeviceManagerImpl dm = getManager();
		
		TranscodeManagerImpl tm = dm.getTranscodeManager();
				
		TranscodeProvider[] providers = tm.getProviders();
				
		String classification = getDeviceClassification();
		
		for ( TranscodeProvider provider: providers ){
			
			TranscodeProfile[] ps = provider.getProfiles();
			
			for ( TranscodeProfile p : ps ){
				
				String c = p.getDeviceClassification();
				
				if ( c == null ){
					
					log( "Device classification missing for " + p.getName());
					
				}else{
					if ( c.toLowerCase().startsWith( classification.toLowerCase())){
						
						profiles.add( p );
					}
				}
			}
		}
		
		return( profiles.toArray( new TranscodeProfile[profiles.size()] ));
	}
		
	public TranscodeProfile
	getDefaultTranscodeProfile()
	{
		String uid = getPersistentStringProperty( PP_REND_DEF_TRANS_PROF );
		
		DeviceManagerImpl dm = getManager();
		
		TranscodeManagerImpl tm = dm.getTranscodeManager();

		TranscodeProfile profile = tm.getProfileFromUID( uid );
		
		if ( profile != null ){
			
			return( profile );
		}
	
		return( null );
	}
	
	public void
	setDefaultTranscodeProfile(
		TranscodeProfile		profile )
	{
		if ( profile == null ){
		
			removePersistentProperty( PP_REND_DEF_TRANS_PROF );
			
		}else{
			
			setPersistentStringProperty( PP_REND_DEF_TRANS_PROF, profile.getUID());
		}
	}
	
	protected void
	setTranscoding(
		boolean		_transcoding )
	{
		transcoding = _transcoding;
		
		manager.deviceChanged( this, false );
	}
	
	public boolean
	isTranscoding()
	{
		return( transcoding );
	}
	
	public int
	getTranscodeRequirement()
	{
		return( getPersistentIntProperty( PP_REND_TRANS_REQ, TranscodeTarget.TRANSCODE_WHEN_REQUIRED ));
	}
	
	public void
	setTranscodeRequirement(
		int		req )
	{
		setPersistentIntProperty( PP_REND_TRANS_REQ, req );
	}
	
	public boolean
	isAudioCompatible(
		TranscodeFile		file )
	{
		return( false );
	}
	
	public boolean
	getAlwaysCacheFiles()
	{
		return( getPersistentBooleanProperty( PP_REND_TRANS_CACHE, false ));
	}
	
	public void
	setAlwaysCacheFiles(
		boolean		always_cache )
	{
		setPersistentBooleanProperty( PP_REND_TRANS_CACHE, always_cache );
	}
	
	public boolean
	isRSSPublishEnabled()
	{
		return( getPersistentBooleanProperty( PP_REND_RSS_PUB, true ));
	}
	
	public void
	setRSSPublishEnabled(
		boolean		enabled )
	{
		setPersistentBooleanProperty( PP_REND_RSS_PUB, enabled );
	}
	
	public String[][] 
	getDisplayProperties() 
	{
		List<String[]> dp = new ArrayList<String[]>();
		
	    getDisplayProperties( dp );
	    	    
	    String[][] res = new String[2][dp.size()];
	   
	    int	pos = 0;
	    
	    for ( String[] entry: dp ){
	    
	    	res[0][pos] = entry[0];
	    	res[1][pos] = entry[1];
	    	
	    	pos++;
	    }
	    
	    return( res );
	}
	
	protected void
	getDisplayProperties(
		List<String[]>	dp )
	{
		if ( !name.equals( classification )){
			
			addDP( dp, "TableColumn.header.name", name );
		}
		
		addDP( dp, "TableColumn.header.class", classification.toLowerCase());
		
		addDP( dp, "!UID!", getID());
		
		if ( !manual ){
		
			addDP( dp, "azbuddy.ui.table.online",  online );
		
			addDP( dp, "device.lastseen", last_seen==0?"":new SimpleDateFormat().format(new Date( last_seen )));
		}
	}
	
	protected void
	getTTDisplayProperties(
		List<String[]>	dp )
	{
		addDP( dp, "devices.xcode.working_dir", getWorkingDirectory( false ).getAbsolutePath());
	
		addDP( dp, "devices.xcode.prof_def", getDefaultTranscodeProfile());
		
		addDP( dp, "devices.xcode.profs", getTranscodeProfiles());
		
		int	tran_req = getTranscodeRequirement();
		
		String	tran_req_str;
		
		if ( tran_req == TranscodeTarget.TRANSCODE_ALWAYS ){
			
			 tran_req_str = "device.xcode.always";
			 
		}else if ( tran_req == TranscodeTarget.TRANSCODE_NEVER ){
			
			 tran_req_str = "device.xcode.never";
		}else{
			
			 tran_req_str = "device.xcode.whenreq";
		}
		
		addDP( dp, "device.xcode", MessageText.getString( tran_req_str ));
	}
	
	protected void
	addDP(
		List<String[]>	dp,
		String			name,
		String			value )
	{
		dp.add( new String[]{ name, value });
	}
	
	protected void
	addDP(
		List<String[]>	dp,
		String			name,
		File			value )
	{
		dp.add( new String[]{ name, value==null?"":value.getAbsolutePath() });
	}
	protected void
	addDP(
		List<String[]>	dp,
		String			name,
		String[]		values )
	{
		String value = "";
		
		for ( String v: values ){
			
			value += (value.length()==0?"":",") + v;
		}
		
		dp.add( new String[]{ name, value });
	}
	
	protected void
	addDP(
		List<String[]>	dp,
		String			name,
		boolean			value )
	{
		dp.add( new String[]{ name, MessageText.getString( value?"GeneralView.yes":"GeneralView.no" ) });
	}
	
	
	protected void
	addDP(
		List<String[]>		dp,
		String				name,
		TranscodeProfile	value )
	{
		addDP( dp, name, value==null?"":value.getName());
	}
	
	protected void
	addDP(
		List<String[]>		dp,
		String				name,
		TranscodeProfile[]	values )
	{
		String[]	names = new String[values.length];
		
		for (int i=0;i<values.length;i++){
			
			names[i] = values[i].getName();
		}
		
		addDP( dp, name, names);
	}
	
	public boolean
	canRemove()
	{
		return( true );
	}
	
	public boolean
	isBusy()
	{
		if ( isTranscoding()){
			
			return( true );
		}
		
		synchronized( this ){
			
			return( busy_count > 0 );
		}
	}
	
	protected void
	setBusy(
		boolean	busy )
	{
		boolean	changed = false;
		
		synchronized( this ){	
			
			if ( busy ){
		
				changed = busy_count++ == 0; 
				
			}else{
				
				changed = busy_count-- == 1; 
			}
		}
		
		if ( changed ){
			
			manager.deviceChanged( this, false );
		}
	}
	
	public void
	remove()
	{
		manager.removeDevice( this );
	}
	
	public String
	getPersistentStringProperty(
		String		prop )
	{
		return( getPersistentStringProperty( prop, "" ));
	}
	
	public String
	getPersistentStringProperty(
		String		prop,
		String		def )
	{
		synchronized( persistent_properties ){
			
			try{
				byte[]	value = (byte[])persistent_properties.get( prop );
		
				if ( value == null ){
					
					return( def );
				}
				
				return( new String( value, "UTF-8" ));
				
			}catch( Throwable e ){
				
				Debug.printStackTrace(e);
				
				return( def );
			}
		}
	}
	
	public void
	setPersistentStringProperty(
		String		prop,
		String		value )
	{
		boolean	dirty = false;
		
		synchronized( persistent_properties ){
			
			String existing = getPersistentStringProperty( prop );
			
			if ( !existing.equals( value )){
				
				try{
					if ( value == null ){
						
						persistent_properties.remove( prop );
						
					}else{
					
						persistent_properties.put( prop, value.getBytes( "UTF-8" ));
					}
					
					dirty = true;
					
				}catch( Throwable e ){
					
					Debug.printStackTrace(e);
				}
			}
		}
		
		if ( dirty ){
			
			setDirty();
		}
	}
	
	public <T> Map<String,T>
	getPersistentMapProperty(
		String					prop,
		Map<String,T>			def )
	{
		synchronized( persistent_properties ){
			
			try{
				Map<String,T>	value = (Map<String,T>)persistent_properties.get( prop );
		
				if ( value == null ){
					
					return( def );
				}
				
				return( value );
				
			}catch( Throwable e ){
				
				Debug.printStackTrace(e);
				
				return( def );
			}
		}
	}
	
	public <T>void
	setPersistentMapProperty(
		String					prop,
		Map<String,T>			value )
	{
		boolean	dirty = false;
		
		synchronized( persistent_properties ){
			
			Map<String,T> existing = getPersistentMapProperty( prop, null );
			
			if ( !BEncoder.mapsAreIdentical( value, existing )){
				
				try{
					if ( value == null ){
						
						persistent_properties.remove( prop );
						
					}else{
					
						persistent_properties.put( prop, value );
					}
					
					dirty = true;
					
				}catch( Throwable e ){
					
					Debug.printStackTrace(e);
				}
			}
		}
		
		if ( dirty ){
			
			setDirty();
		}
	}
	
	public void
	removePersistentProperty(
		String		prop )
	{
		boolean	dirty = false;
		
		synchronized( persistent_properties ){
			
			String existing = getPersistentStringProperty( prop );
			
			if ( existing != null ){
				
				try{
					persistent_properties.remove( prop );
					
					dirty = true;
					
				}catch( Throwable e ){
					
					Debug.printStackTrace(e);
				}
			}
		}
		
		if ( dirty ){
			
			setDirty();
		}
	}
	public String
	getError()
	{
		synchronized( errors ){

			if ( errors.size() == 0 ){
				
				return( null );
			}
			
			String 	res = "";
			
			for ( String s: errors.values()){
				
				res += (res.length()==0?"":"; ") + s;
			}
			
			return( res );
		}
	}
	
	protected void
	setError(
		Object	key,
		String	error )
	{
		boolean	changed = false;
		
		if ( error == null || error.length() == 0 ){
			
			synchronized( errors ){
			
				changed = errors.remove( key ) != null;
			}
		}else{
			
			String	existing;
			
			synchronized( errors ){
				
				existing = errors.put( key, error );
			}
			
			changed = existing == null || !existing.equals( error );
		}
		
		if ( changed ){
			
			manager.deviceChanged( this, false );
		}
	}
	
	public String
	getInfo()
	{
		synchronized( infos ){

			if ( infos.size() == 0 ){
				
				return( null );
			}
			
			String 	res = "";
			
			for ( String s: infos.values()){
				
				res += (res.length()==0?"":"; ") + s;
			}
			
			return( res );
		}
	}
	
	protected void
	setInfo(
		Object	key,
		String	info )
	{
		boolean	changed = false;
		
		if ( info == null || info.length() == 0 ){
			
			synchronized( infos ){
			
				changed = infos.remove( key ) != null;
			}
		}else{
			
			String	existing;
			
			synchronized( infos ){
				
				existing = infos.put( key, info );
			}
			
			changed = existing == null || !existing.equals( info );
		}
		
		if ( changed ){
			
			manager.deviceChanged( this, false );
		}
	}
	
	public boolean
	getPersistentBooleanProperty(
		String		prop,
		boolean		def )
	{
		return( getPersistentStringProperty( prop, def?"true":"false" ).equals( "true" ));
	}
	
	public void
	setPersistentBooleanProperty(
		String		prop,
		boolean		value )
	{
		setPersistentStringProperty(prop, value?"true":"false" );
	}
	
	public int
	getPersistentIntProperty(
		String		prop,
		int			def )
	{
		return( Integer.parseInt( getPersistentStringProperty( prop, String.valueOf(def) )));
	}
	
	public void
	setPersistentIntProperty(
		String		prop,
		int			value )
	{
		setPersistentStringProperty(prop, String.valueOf( value ));
	}
	
	public String[]
	getPersistentStringListProperty(
		String		prop )
	{
		synchronized( persistent_properties ){
			
			try{
				List<byte[]>	values = (List<byte[]>)persistent_properties.get( prop );
		
				if ( values == null ){
					
					return( new String[0] );
				}
				
				String[]	res = new String[values.size()];
				
				int	pos = 0;
				
				for (byte[] value: values ){
				
					res[pos++] = new String( value, "UTF-8" );
				}
				
				return( res );
				
			}catch( Throwable e ){
				
				Debug.printStackTrace(e);
				
				return( new String[0] );
			}
		}
	}
	
	public void
	setPersistentStringListProperty(
		String			prop,
		String[]		values )
	{
		boolean dirty = false;

		synchronized( persistent_properties ){
			
			try{
				List<byte[]> values_list = new ArrayList<byte[]>();
				
				for (String value: values ){
					
					values_list.add( value.getBytes( "UTF-8" ));
				}
				
				persistent_properties.put( prop, values_list );
				
				dirty = true;
				
			}catch( Throwable e ){
				
				Debug.printStackTrace(e);
			}
		}
		
		if ( dirty ){
			
			setDirty();
		}
	}
	
	public void
	setTransientProperty(
		Object		key,
		Object		value )
	{
		synchronized( transient_properties ){
			
			if ( value == null ){
				
				transient_properties.remove( key );
				
			}else{
			
				transient_properties.put( key, value );
			}
		}
	}
	
	public Object
	getTransientProperty(
		Object		key )
	{
		synchronized( transient_properties ){
			
			return( transient_properties.get( key ));
		}
	}
	
	public void
	setTransientProperty(
		Object		key1,
		Object		key2,
		Object		value )
	{
		synchronized( transient_properties ){
			
			Map<Object,Object> l1 = (Map<Object,Object>)transient_properties.get( key1 );
			
			if ( l1 == null ){
				
				if ( value == null ){
					
					return;
				}
				
				l1 = new HashMap<Object, Object>();
				
				transient_properties.put( key1, l1 );
			}
			
			if ( value == null ){
				
				l1.remove( key2 );
				
				if ( l1.size() == 0 ){
					
					transient_properties.remove( key1 );	
				}
			}else{
				
				l1.put( key2, value );
			}
		}
	}
	
	public Object
	getTransientProperty(
		Object		key1,
		Object		key2 )
	{
		synchronized( transient_properties ){
			
			Map<Object,Object> l1 = (Map<Object,Object>)transient_properties.get( key1 );
			
			if ( l1 == null ){
				
				return( null );
			}

			return( l1.get( key2 ));
		}
	}
	
	protected void
	close()
	{
		synchronized( this ){

			if ( device_files_dirty ){
				
				saveDeviceFile();
			}
		}
	}
	
	protected void
	loadDeviceFile()
	
		throws IOException
	{
		device_files_last_mod = SystemTime.getMonotonousTime();
		
		if ( device_files_ref != null ){
		
			device_files = device_files_ref.get();
		}
		
		if ( device_files == null ){
			
			Map	map = FileUtil.readResilientFile( getDeviceFile());
	
			device_files = (Map<String,Map<String,?>>)map.get( "files" );
			
			if ( device_files == null ){
				
				device_files = new HashMap<String, Map<String,?>>();
			}
		
			device_files_ref = new WeakReference<Map<String,Map<String,?>>>( device_files );
			
			log( "Loaded device file for " + getName() + ": files=" + device_files.size());
		}
		
		final int GC_TIME = 15000;
		
		new DelayedEvent( 
			"Device:gc", 
			GC_TIME,
			new AERunnable()
			{
				public void
				runSupport()
				{
					synchronized( DeviceImpl.this ){
						
						if ( SystemTime.getMonotonousTime() - device_files_last_mod >= GC_TIME ){
								
							if ( device_files_dirty ){
								
								saveDeviceFile();
							}
							
							device_files = null;
							
						}else{
							
							new DelayedEvent( "Device:gc2", GC_TIME, this );
						}
					}
				}
			});
	}
	
	protected URL
	getStreamURL(
		TranscodeFileImpl		file,
		String					host )
	{
		return( manager.getStreamURL( file, host ));
	}
	
	protected String
	getMimeType(
		TranscodeFileImpl		file )
	{
		return( manager.getMimeType( file ));
	}
	
	protected void
	deleteFile(
		TranscodeFileImpl	file,
		boolean				delete_contents,
		boolean				remove )
	
		throws TranscodeException 
	{	
		if ( file.isDeleted()){
			
			return;
		}
		
		if ( delete_contents ){
			
			File f = file.getCacheFile();
				
			int	 time = 0;
			
			while( f.exists() && !f.delete()){
						
				if ( time > 3000 ){
				
					log( "Failed to remove file '" + f.getAbsolutePath() + "'" );
					
					break;
					
				}else{
					
					try{
						Thread.sleep(500);
						
					}catch( Throwable e ){
						
					}
					
					time += 500;
				}
			}
		}
		
		if ( remove ){
			
			try{
				synchronized( this ){
					
					if ( device_files == null ){
						
						loadDeviceFile();
						
					}else{
						
						device_files_last_mod = SystemTime.getMonotonousTime();
					}
					
					device_files.remove( file.getKey());
					
					device_files_dirty	= true;
				}
				
				for ( TranscodeTargetListener l: listeners ){
					
					try{
						l.fileRemoved( file );
						
					}catch( Throwable e ){
						
						Debug.out( e );
					}
				}
			}catch( Throwable e ){
				
				throw( new TranscodeException( "Delete failed", e ));
			}
		}
	}
	
	protected void
	fileDirty(
		TranscodeFileImpl	file,
		int					type,
		Object				data )
	{
		try{
			synchronized( this ){
				
				if ( device_files == null ){
					
					loadDeviceFile();
					
				}else{
					
					device_files_last_mod = SystemTime.getMonotonousTime();
				}
			}
			
			device_files_dirty	= true;
			
		}catch( Throwable e ){
			
			Debug.out( "Failed to load device file", e );
		}
		
		for ( TranscodeTargetListener l: listeners ){
			
			try{
				l.fileChanged( file, type, data );
				
			}catch( Throwable e ){
				
				Debug.out( e );
			}
		}
	}
	
	protected void
	saveDeviceFile()
	{
		device_files_dirty = false;
		
		try{
			loadDeviceFile();
			
			if ( device_files == null || device_files.size()==0 ){
				
				FileUtil.deleteResilientFile( getDeviceFile());
				
			}else{
				Map map = new HashMap();
				
				map.put( "files", device_files );
				
				FileUtil.writeResilientFile( getDeviceFile(), map );
			}
		}catch( Throwable e ){
			
			Debug.out( "Failed to save device file", e );
		}
	}
	
	protected File
	getDeviceFile()
	
		throws IOException
	{
 		File dir = getDevicesDir();
 		
 		return( new File( dir, FileUtil.convertOSSpecificChars(getID(),false) + ".dat" ));
	}
	
	protected File
	getDevicesDir()
	
		throws IOException
	{
		File dir = new File(SystemProperties.getUserPath());

		dir = new File( dir, "devices" );
 		
 		if ( !dir.exists()){
 			
 			if ( !dir.mkdirs()){
 				
 				throw( new IOException( "Failed to create '" + dir + "'" ));
 			}
 		}	
 		
 		return( dir );
	}
	
	protected DeviceManagerImpl
	getManager()
	{
		return( manager );
	}

	public void
	addListener(
		TranscodeTargetListener		listener )
	{
		if (!listeners.contains(listener)) {
			listeners.add( listener );
		}
	}
	
	public void
	removeListener(
		TranscodeTargetListener		listener )
	{
		listeners.remove( listener );
	}
	
	protected void
	fireChanged()
	{
		List<DeviceListener> l;
		
		synchronized( this ){
			
			if ( device_listeners != null ){
				
				l = device_listeners.getList();
				
			}else{
				
				return;
			}
		}
		
		for ( DeviceListener listener: l ){
			
			try{
				listener.deviceChanged( this );
				
			}catch( Throwable e ){
				
				Debug.out( e );
			}
		}
	}
	
	public void
	addListener(
		DeviceListener		listener )
	{
		synchronized( this ){
			
			if ( device_listeners == null ){
				
				device_listeners = new CopyOnWriteList<DeviceListener>();
			}
			
			device_listeners.add( listener );
		}
	}
	
	public void
	removeListener(
		DeviceListener		listener )
	{
		synchronized( this ){
			
			if ( device_listeners != null ){
							
				device_listeners.remove( listener );
				
				if ( device_listeners.size() == 0 ){
					
					device_listeners = null;
				}		
			}
		}
	}
	
	protected void
	log(
		String		str )
	{
		manager.log( str );
	}
	
	protected void
	log(
		String		str,
		Throwable	e )
	{
		manager.log( str, e );
	}
	
	public String
	getString()
	{
		return( "type=" + type + ",uid=" + uid + ",class=" + classification );
	}
	
	public void
	generate(
		IndentWriter		writer )
	{
		writer.println( getName() + "/" + getID() + "/" + type );
		
		try{
			writer.indent();
			
			writer.println( 
				"hidden=" + hidden + 
				", last_seen=" + new SimpleDateFormat().format(new Date(last_seen)) +
				", online=" + online +
				", transcoding=" + transcoding );
			
			writer.println( "p_props=" + persistent_properties );
			writer.println( "t_props=" + transient_properties );
			
			writer.println( "errors=" + errors );
			writer.println( "infos=" + infos );
		}finally{
			
			writer.exdent();
		}
	}
	
	public void
	generateTT(
		IndentWriter		writer )
	{
		TranscodeFileImpl[] files = getFiles();
		
		int	complete	 = 0;
		int	copied		 = 0;
		int	deleted	 	= 0;
		int	template	 = 0;
		
		for ( TranscodeFileImpl f: files ){
			
			if ( f.isComplete()){
				
				complete++;
			}
			
			if ( f.isCopiedToDevice()){
				
				copied++;
			}
			
			if ( f.isDeleted()){
				
				deleted++;
			}
			
			if ( f.isTemplate()){
				
				template++;
			}
		}
		
		writer.println( "files=" + files.length + ", comp=" + complete + ", copied=" + copied + ", deleted=" + deleted + ", template=" + template );
	}
	
	protected class
	browseLocationImpl
		implements browseLocation
	{
		private String		name;
		private URL			url;
		
		protected
		browseLocationImpl(
			String		_name,
			URL			_url )
		{
			name		= _name;
			url			= _url;
		}
		
		public String
		getName()
		{
			return( name );
		}
		
		public URL
		getURL()
		{
			return( url );
		}
	}
}
