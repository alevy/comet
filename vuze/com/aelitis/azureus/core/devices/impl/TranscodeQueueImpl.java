/*
 * Created on Feb 6, 2009
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

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.*;

import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.config.ParameterListener;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.util.AERunnable;
import org.gudy.azureus2.core3.util.AESemaphore;
import org.gudy.azureus2.core3.util.AEThread2;
import org.gudy.azureus2.core3.util.Constants;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.core3.util.DelayedEvent;
import org.gudy.azureus2.core3.util.FileUtil;
import org.gudy.azureus2.core3.util.IndentWriter;
import org.gudy.azureus2.plugins.PluginInterface;
import org.gudy.azureus2.plugins.disk.DiskManagerFileInfo;
import org.gudy.azureus2.plugins.ipc.IPCInterface;
import org.gudy.azureus2.plugins.utils.DelayedTask;
import org.gudy.azureus2.pluginsimpl.local.PluginInitializer;
import org.gudy.azureus2.pluginsimpl.local.utils.UtilitiesImpl;

import com.aelitis.azureus.core.devices.*;
import com.aelitis.azureus.core.messenger.config.PlatformDevicesMessenger;
import com.aelitis.azureus.core.util.CopyOnWriteList;
import com.aelitis.azureus.core.util.average.Average;
import com.aelitis.azureus.core.util.average.AverageFactory;

public class 
TranscodeQueueImpl 	
	implements TranscodeQueue
{
	private static final String	CONFIG_FILE 			= "xcodejobs.config";

	private static final Object KEY_XCODE_ERROR	= new Object();
	
	private TranscodeManagerImpl		manager;
	
	private List<TranscodeJobImpl>		queue		= new ArrayList<TranscodeJobImpl>();
	private AESemaphore 				queue_sem 	= new AESemaphore( "XcodeQ" );
	private AEThread2					queue_thread;
	
	private volatile TranscodeJobImpl	current_job;
	
	private CopyOnWriteList<TranscodeQueueListener>	listeners = new CopyOnWriteList<TranscodeQueueListener>();
	
	private volatile boolean 	paused;
	private volatile int		max_bytes_per_sec;
	
	private volatile boolean	config_dirty;
	
	protected
	TranscodeQueueImpl(
		TranscodeManagerImpl	_manager )
	{
		manager = _manager;
	}
	
	protected void
	initialise()
	{	
		loadConfig();
		
		COConfigurationManager.addAndFireParameterListeners(
			new String[]{
				"xcode.queue.paused",
				"xcode.queue.maxbps",
			},
			new ParameterListener()
			{
				public void 
				parameterChanged(
					String		name )
				{
					paused				= COConfigurationManager.getBooleanParameter( "xcode.queue.paused", false );
					max_bytes_per_sec	= COConfigurationManager.getIntParameter( "xcode.queue.maxbps", 0 );
				}
			});
		
		DelayedTask delayed_task = 
			UtilitiesImpl.addDelayedTask(
				"TranscodeQueue:schedule", 
				new Runnable()
				{
					public void
					run()
					{
						schedule();
					}
				});
		
		delayed_task.queue();
	}
	
	protected boolean
	process(
		final TranscodeJobImpl		job )
	{				
		TranscodePipe pipe = null;
		
		current_job = job;
		
		DeviceImpl	device = job.getDevice();
		
		device.setTranscoding( true );
		
		try{
			job.starts();
			
			TranscodeProvider provider = job.getProfile().getProvider();

			
			final Throwable[] error = { null };

				
			TranscodeProfile profile = job.getProfile();
				
			final AESemaphore analysis_sem = new AESemaphore( "analysis:proc" );
			

			TranscodeProviderAdapter analysis_adapter = 
				new TranscodeProviderAdapter()
				{
					public void
					updateProgress(
						int		percent,
						int		eta_secs,
						int		width,
						int		height )
					{
					}
					
					public void
					streamStats(
						long					connect_rate,
						long					write_speed )
					{
					}
					
					public void
					failed(
						TranscodeException		e )
					{
						error[0] = e;
						
						analysis_sem.release();
					}
					
					public void 
					complete() 
					{
						analysis_sem.release();
					}
				};
				
			final TranscodeProviderAnalysis provider_analysis =	provider.analyse( analysis_adapter, job.getFile(), profile );
			
			TranscodeQueueListener analysis_q_listener = 
				new TranscodeQueueListener()
				{
					public void
					jobAdded(
						TranscodeJob		job )
					{					
					}
					
					public void
					jobChanged(
						TranscodeJob		changed_job )
					{
						if ( changed_job == job ){
							
							int	state = job.getState();
							
							if ( 	state == TranscodeJob.ST_CANCELLED ||
									state == TranscodeJob.ST_STOPPED ){
							
								provider_analysis.cancel();
							}
						}
					}
					
					public void
					jobRemoved(
						TranscodeJob		removed_job )
					{	
						if ( removed_job == job ){
							
							provider_analysis.cancel();
						}
					}
				};
				
			try{
				addListener( analysis_q_listener );
			
				analysis_sem.reserve();
				
			}finally{
				
				removeListener( analysis_q_listener );
			}
			
			if ( error[0] != null ){
				
				throw( error[0] );
			}
			
			boolean xcode_required 	= provider_analysis.getBooleanProperty( TranscodeProviderAnalysis.PT_TRANSCODE_REQUIRED );
			
			final TranscodeFileImpl		transcode_file = job.getTranscodeFile();

			transcode_file.update( provider_analysis );
			
			int	tt_req;
			
			if ( job.isStream()){
				
					// already advertised as a transcoded asset so no option not to
					// transcode (as name/format would change if decided not to transcode and then
					// this would confuse the clients)
				
				tt_req = TranscodeTarget.TRANSCODE_ALWAYS;
				
			}else{
				
				tt_req = job.getTranscodeRequirement();
				
					// audio hack for PSP audio
				
				if ( device instanceof TranscodeTarget ){
					
					if ( provider_analysis.getLongProperty( TranscodeProviderAnalysis.PT_VIDEO_HEIGHT ) == 0 ){
						
						if (((TranscodeTarget)device).isAudioCompatible( transcode_file )){
					
							tt_req = TranscodeTarget.TRANSCODE_NEVER;
						}
					}
				}
			}
						
			if ( tt_req == TranscodeTarget.TRANSCODE_NEVER ){
				
				xcode_required = false;
				
			}else if ( tt_req == TranscodeTarget.TRANSCODE_ALWAYS ){
				
				xcode_required 	= true;
				
				provider_analysis.setBooleanProperty( TranscodeProviderAnalysis.PT_FORCE_TRANSCODE, true );
			}
							
			if ( xcode_required ){
				
				final AESemaphore xcode_sem = new AESemaphore( "xcode:proc" );
				
				final TranscodeProviderJob[] provider_job = { null }; 
	
				TranscodeProviderAdapter xcode_adapter = 
					new TranscodeProviderAdapter()
					{
						private boolean	resolution_updated;
					
						private final int ETA_AVERAGE_SIZE	= 10;
						
						private int		last_eta;
						private int		eta_samples;
						private Average eta_average 		= AverageFactory.MovingAverage(ETA_AVERAGE_SIZE);
						private int		last_percent;
						
						public void
						updateProgress(
							int			percent,
							int			eta_secs,
							int			new_width,
							int			new_height )
						{
							last_eta 		= eta_secs;
							last_percent	= percent;
							
							TranscodeProviderJob	prov_job = provider_job[0];
							
							if ( prov_job == null ){
								
								return;
							}
							
							int	job_state = job.getState();
							
							if ( 	job_state == TranscodeJob.ST_CANCELLED || 
									job_state == TranscodeJob.ST_REMOVED ){
																
								prov_job.cancel();
								
							}else if ( paused || job_state == TranscodeJob.ST_PAUSED ){
									
								prov_job.pause();
								
							}else{
								
								if ( job_state == TranscodeJob.ST_RUNNING ){
									
									prov_job.resume();
								}
								
								job.updateProgress( percent, eta_secs );
								
								prov_job.setMaxBytesPerSecond( max_bytes_per_sec );
								
								if ( !resolution_updated ){
									
									if ( new_width > 0 && new_height > 0 ){
									
										transcode_file.setResolution( new_width, new_height );
										
										resolution_updated = true;
									}	
								}
							}
						}
						
						public void
						streamStats(
							long					connect_rate,
							long					write_speed )
						{
								// problem on OSX with some files thrashing the indirect piped input
								// and eventually failing - try and spot this behaviour and revert
								// to direct input if needed
							
							if ( Constants.isOSX && job.canUseDirectInput() && job.getAutoRetryCount() == 0 ){
								
								if ( connect_rate > 5 && last_percent < 100 ){
									
									long eta = (long)eta_average.update(last_eta );

									eta_samples++;
									
									if ( eta_samples >= ETA_AVERAGE_SIZE ){
										
										long	total_time = (eta*100 )/(100-last_percent);
										
										long 	total_write = total_time*write_speed;
										
										DiskManagerFileInfo file = job.getFile();
										
										long	length = file.getLength();

										if ( length > 0 ){

											double over_write = ((double)total_write)/length;
										
											if ( over_write > 5.0 ){
													
												failed( new TranscodeException( "Overwrite limit exceeded, abandoning transcode" ));
													
												provider_job[0].cancel();		
											}
										}	
									}
								}else{
									
									eta_samples = 0;
								}
							}
						}
						
						public void
						failed(
							TranscodeException		e )
						{
							if ( error[0] == null ){
							
								error[0] = e;
							}
							
							xcode_sem.release();
						}
						
						public void 
						complete() 
						{
							xcode_sem.release();
						}
					};
				
				boolean	direct_input = job.useDirectInput();
					
				if ( job.isStream()){
					
					/*
					provider_job[0] = 
						provider.transcode(
							adapter,
							job.getFile(),
							profile,
							new File( "C:\\temp\\arse").toURI().toURL());
					*/
					
					pipe = new TranscodePipeStreamSource2(
								new TranscodePipeStreamSource2.streamListener()
								{
									public void 
									gotStream(
										InputStream is ) 
									{
										job.setStream( is );
									}
								});
					
					provider_job[0] = 
						provider.transcode(
							xcode_adapter,
							provider_analysis,
							direct_input,
							job.getFile(),
							profile,
							new URL( "tcp://127.0.0.1:" + pipe.getPort()));
	
				}else{
										
					File output_file = transcode_file.getCacheFile();
					
					provider_job[0] = 
						provider.transcode(
							xcode_adapter,
							provider_analysis,
							direct_input,
							job.getFile(),
							profile,
							output_file.toURI().toURL());
				}
				
				provider_job[0].setMaxBytesPerSecond( max_bytes_per_sec );
				
				TranscodeQueueListener listener = 
					new TranscodeQueueListener()
					{
						public void
						jobAdded(
							TranscodeJob		job )
						{					
						}
						
						public void
						jobChanged(
							TranscodeJob		changed_job )
						{
							if ( changed_job == job ){
								
								int	state = job.getState();
								
								if ( state == TranscodeJob.ST_PAUSED ){
									
									provider_job[0].pause();
									
								}else if ( state == TranscodeJob.ST_RUNNING ){
										
									provider_job[0].resume();
									
								}else if ( 	state == TranscodeJob.ST_CANCELLED ||
											state == TranscodeJob.ST_STOPPED ){
								
									provider_job[0].cancel();
								}
							}
						}
						
						public void
						jobRemoved(
							TranscodeJob		removed_job )
						{	
							if ( removed_job == job ){
								
								provider_job[0].cancel();
							}
						}
					};
					
				try{
					addListener( listener );
				
					xcode_sem.reserve();
					
				}finally{
					
					removeListener( listener );
				}
				
				if ( error[0] != null ){
					
					throw( error[0] );
				}
			}else{
				
					// no transcode required...
				
				DiskManagerFileInfo source = job.getFile();
				
				transcode_file.setTranscodeRequired( false );
				
				if ( job.isStream()){
					
					PluginInterface av_pi = PluginInitializer.getDefaultInterface().getPluginManager().getPluginInterfaceByID( "azupnpav" );
					
					if ( av_pi == null ){
					
						throw( new TranscodeException( "Media Server plugin not found" ));
					}
					
					IPCInterface av_ipc = av_pi.getIPC();
					
					String url_str = (String)av_ipc.invoke( "getContentURL", new Object[]{ source });
					
					
					if ( url_str == null || url_str.length() == 0 ){
						
							// see if we can use the file directly
						
						File source_file = source.getFile();
						
						if ( source_file.exists()){
							
							job.setStream( new BufferedInputStream( new FileInputStream( source_file )));
							
						}else{
							
							throw( new TranscodeException( "No UPnPAV URL and file doesn't exist" ));
						}
					}else{
						
						URL source_url = new URL( url_str );
					
						job.setStream( source_url.openConnection().getInputStream());
					}
				}else{
					
					if ( device.getAlwaysCacheFiles()){
						
						PluginInterface av_pi = PluginInitializer.getDefaultInterface().getPluginManager().getPluginInterfaceByID( "azupnpav" );
						
						if ( av_pi == null ){
						
							throw( new TranscodeException( "Media Server plugin not found" ));
						}
						
						IPCInterface av_ipc = av_pi.getIPC();
						
						String url_str = (String)av_ipc.invoke( "getContentURL", new Object[]{ source });
						
						InputStream	is;
						
						long		length;
						
						if ( url_str == null || url_str.length() == 0 ){
							
								// see if we can use the file directly
							
							File source_file = source.getFile();
							
							if ( source_file.exists()){
								
								is = new BufferedInputStream( new FileInputStream( source_file ));
								
								length = source_file.length();
								
							}else{
								
								throw( new TranscodeException( "No UPnPAV URL and file doesn't exist" ));
							}
						}else{
							
							URL source_url = new URL( url_str );
						
							URLConnection connection = source_url.openConnection();
							
							is = source_url.openConnection().getInputStream();
							
							String s = connection.getHeaderField( "content-length" );
							
							if ( s != null ){
								
								length = Long.parseLong( s );
								
							}else{
								
								length = -1;
							}
						}
						
						OutputStream	os = null;
						
						final boolean[]	cancel_copy = { false };
						
						TranscodeQueueListener copy_listener = 
							new TranscodeQueueListener()
							{
								public void
								jobAdded(
									TranscodeJob		job )
								{					
								}
								
								public void
								jobChanged(
									TranscodeJob		changed_job )
								{
									if ( changed_job == job ){
										
										int	state = job.getState();
										
										if ( state == TranscodeJob.ST_PAUSED ){
																						
										}else if ( state == TranscodeJob.ST_RUNNING ){
																							
										}else if ( 	state == TranscodeJob.ST_CANCELLED ||
													state == TranscodeJob.ST_STOPPED ){
										
											cancel_copy[0] = true;
										}
									}
								}
								
								public void
								jobRemoved(
									TranscodeJob		removed_job )
								{	
									if ( removed_job == job ){
										
										cancel_copy[0] = true;
									}
								}
							};
							
						try{
							addListener( copy_listener );

							os = new FileOutputStream( transcode_file.getCacheFile());
							
							long	total_copied = 0;
							
							byte[] buffer = new byte[128*1024];
							
							while( true ){
								
								if ( cancel_copy[0] ){
									
									throw( new TranscodeException( "Copy cancelled" ));
								}
								
								int	len = is.read( buffer );
								
								if ( len <= 0 ){
									
									break;
								}
								
								os.write( buffer, 0, len );
								
								total_copied += len;
								
								if ( length > 0 ){
								
									job.updateProgress((int)( total_copied*100/length ), -1 );
								}
								
								total_copied += len;
							}
						}finally{
							
							try{
								is.close();
								
							}catch( Throwable e ){
								
								Debug.out( e );
							}
							
							try{
								if ( os != null ){
								
									os.close();
								}
							}catch( Throwable e ){
								
								Debug.out( e );
							}
							
							removeListener( copy_listener );
						}
					}
				}
			}
			
			job.complete();
			
			return( true );
			
		}catch( Throwable e ){
			
			job.failed( e );
			
			if ( !job.isStream() && job.getAutoRetryCount() == 0 && job.canUseDirectInput() && !job.useDirectInput()){
				
				log( "Auto-retrying transcode with direct input" );
				
				job.setUseDirectInput();
				
				job.setAutoRetry( true );
				
				queue_sem.release();
			}
			
			return( false );
			
		}finally{
			
			if ( pipe != null ){
				
				pipe.destroy();
			}
			
			device.setTranscoding( false );

			current_job = null;
		}
	}
	
	protected void
	schedule()
	{
		synchronized( this ){

			if ( queue.size() > 0 && queue_thread == null ){
				
				queue_thread = new
					AEThread2( "XcodeQ", true )
					{
						public void 
						run() 
						{
							try{
								while( true ){
									
									checkJobStatus();
									
									boolean got = queue_sem.reserve( 30*1000 );
																			
									TranscodeJobImpl	job = null;
									
									synchronized( TranscodeQueueImpl.this ){
										
										if ( !got ){
											
											if ( queue.size() == 0 ){
												
												queue_thread = null;
												
												return;
											}
										}
										
										for ( TranscodeJobImpl j: queue ){
											
											int state = j.getState();
											
												// pick up any existing paused ones (remember, paused=running but with transcode paused
												
											if ( state == TranscodeJob.ST_FAILED && j.isAutoRetry()){
												
												j.setAutoRetry( false );
												
												j.reset();
												
												job	= j;
												
												break;
												
											}else if ( state == TranscodeJob.ST_PAUSED ){
	
													// remember paused here is paused after an azureus
													// restart as 'process' blocks on pause
												
												job = j;
												
											}else if ( state == TranscodeJob.ST_QUEUED ){
												
												if ( job == null && j.getDownloadETA() == 0 ){
												
													job = j;
												}
											}
										}
									}
									
									checkJobStatus();

									if ( job != null ){
									
										if ( process( job )){
										
											remove( job );
										}																			
									}
								}
							}finally{
								
								checkJobStatus();
							}
						}
					};
					
				queue_thread.start();
			}
		}
	}
	
	protected void
	updateStatus(
		int	tick_count )
	{
		if ( queue.size() > 0 ){
			
			TranscodeJobImpl[] jobs = getJobs();
			
			for ( TranscodeJobImpl job: jobs ){
				
				job.updateStatus();
			}
		}	
	}
	
	protected void
	checkJobStatus()
	{
		Set<DeviceImpl> devices = new HashSet<DeviceImpl>( Arrays.asList(manager.getManager().getDevices()));
		
		synchronized( this ){
			
			for ( TranscodeJobImpl j: queue ){

				if ( j.getState() == TranscodeJob.ST_FAILED ){
					
					DeviceImpl	device = j.getDevice();
					
					device.setError( KEY_XCODE_ERROR, MessageText.getString( "device.error.xcodefail" ));
					
					devices.remove( device );
				}
			}
		}
		
		for ( DeviceImpl device: devices ){
			
			device.setError( KEY_XCODE_ERROR, null );
		}
	}
	
	public TranscodeJobImpl
	add(
		TranscodeTarget			target,
		TranscodeProfile		profile,
		DiskManagerFileInfo		file )
	
		throws TranscodeException
	{
		return( add( target, profile, file, false ));
	}
	
	public TranscodeJobImpl
	add(
		TranscodeTarget			target,
		TranscodeProfile		profile,
		DiskManagerFileInfo		file,
		int						transcode_requirement )
	
		throws TranscodeException
	{
		return( add( target, profile, file, false, transcode_requirement ));
	}
	
	public TranscodeJobImpl
	add(
		TranscodeTarget			target,
		TranscodeProfile		profile,
		DiskManagerFileInfo		file,
		boolean					stream )
	
		throws TranscodeException
	{
		return( add( target, profile, file, stream, -1 ));
	}
	
	public TranscodeJobImpl
	add(
		TranscodeTarget			target,
		TranscodeProfile		profile,
		DiskManagerFileInfo		file,
		boolean					stream,
		int						transcode_requirement )
	
		throws TranscodeException
	{
		TranscodeFileImpl existing_tf = ((DeviceImpl)target.getDevice()).lookupFile( profile, file );
		
		if ( existing_tf != null ){
			
			List<TranscodeJobImpl>	to_remove = new ArrayList<TranscodeJobImpl>();
			
			synchronized( this ){
	
				for ( TranscodeJobImpl job: queue ){
					
					if ( job.getTarget() == target && job.getTranscodeFile().equals( existing_tf )){
						
						to_remove.add( job );
					}
				}
			}
			
			for ( TranscodeJobImpl job: to_remove ){
	
				job.remove();
			}
			
			if ( !stream ){
			
				existing_tf.delete( true );
			}
		}
		
		TranscodeJobImpl job = new TranscodeJobImpl( this, target, profile, file, transcode_requirement, stream );
		
		try{
			synchronized( this ){
				
				queue.add( job );
				
				queue_sem.release();
				
				saveConfig();
			}
			
			// I'd rather do qos from a listener trigger, but for now this ensures
			// I get the event even if listeners haven't had a chance to be added
			try {
				// force state log to queued just in case it got started (or errored)
				// between job creation and here
				PlatformDevicesMessenger.qosTranscode(job, TranscodeJob.ST_QUEUED);
			} catch (Throwable t) {
				Debug.out(t);
			}
			
			for ( TranscodeQueueListener listener: listeners ){
				
				try{
					listener.jobAdded( job );
					
				}catch( Throwable e ){
					
					Debug.printStackTrace( e );
				}
			}
		}finally{
		
			schedule();
		}
		
		return( job );
	}
	
	protected void
	remove(
		TranscodeJobImpl		job )
	{
		synchronized( this ){
			
			if ( !queue.remove( job )){
				
				return;
			}
		}
		
		saveConfig();

		job.destroy();
		
		for ( TranscodeQueueListener listener: listeners ){
			
			try{
				listener.jobRemoved( job );
				
			}catch( Throwable e ){
				
				Debug.printStackTrace( e );
			}
		}
		
		checkJobStatus();
		
		schedule();
	}
	
	protected void
	jobChanged(
		TranscodeJob			job,
		boolean					schedule,
		boolean					persistable )
	{

		for ( TranscodeQueueListener listener: listeners ){
			
			try{
				listener.jobChanged( job );
				
			}catch( Throwable e ){
				
				Debug.printStackTrace( e );
			}
		}
		
		if ( persistable ){
		
			configDirty();
		}
		
		if ( schedule ){
			
			queue_sem.release();
			
			schedule();
		}
	}
	
	protected int
	getIndex(
		TranscodeJobImpl		job )
	{
		synchronized( this ){
		
			return( queue.indexOf(job)+1);
		}
	}
	
	public TranscodeJobImpl[]
	getJobs()
	{
		synchronized( this ){

			return( queue.toArray( new TranscodeJobImpl[queue.size()]));
		}
	}
	
	public int
	getJobCount()
	{
		synchronized( this ){

			return( queue.size());
		}	
	}
	
	public TranscodeJob
	getCurrentJob()
	{
		return( current_job );
	}
	
	public boolean
	isTranscoding()
	{
		return( current_job != null );
	}
	
	protected TranscodeJobImpl
	getJob(
		TranscodeFile		for_file )
	{
		synchronized( this ){

			for ( TranscodeJobImpl job: queue ){
				
				if ( job.getTranscodeFile().equals( for_file )){
					
					return( job );
				}
			}
		}
		
		return( null );
	}
	
	public void 
	moveUp(
		TranscodeJobImpl	job )
	{
		TranscodeJob[] updated;
			
		synchronized( this ){
		
			int index = queue.indexOf( job );
			
			if ( index <= 0 || queue.size() == 1 ){
				
				return;
			}
			
			queue.remove( job );
			
			queue.add( index-1, job );
			
			updated = getJobs();
		}
		
		for ( TranscodeJob j: updated ){
			
			jobChanged( j, false, true );
		}
	}
	
	public void 
	moveDown(
		TranscodeJobImpl	job )
	{
		TranscodeJob[] updated;

		synchronized( this ){
		
			int index = queue.indexOf( job );
			
			if ( index < 0 || index == queue.size() - 1 ){
				
				return;
			}
			
			queue.remove( job );
			
			queue.add( index+1, job );
			
			updated = getJobs();
		}
		
		for ( TranscodeJob j: updated ){
			
			jobChanged( j, false, true );
		}
	}
	
	public void
	pause()
	{
		if ( !paused ){
			
			if ( paused ){
				
				COConfigurationManager.setParameter( "xcode.paused", true );
			}
		}
	}
	
	public boolean
	isPaused()
	{
		return( paused );
	}
	
	public void
	resume()
	{
		if ( paused ){
			
			COConfigurationManager.setParameter( "xcode.queue.paused", false );
		}
	}
	
	public long
	getMaxBytesPerSecond()
	{
		return( max_bytes_per_sec );
	}
	
	public void
	setMaxBytesPerSecond(
		long		max )
	{
		COConfigurationManager.setParameter( "xcode.queue.maxbps", max );
	}
	
	protected TranscodeTarget
	lookupTarget(
		String		target_id )
	
		throws TranscodeException
	{
		return( manager.lookupTarget( target_id ));
	}
	
	protected TranscodeProfile
	lookupProfile(
		String		profile_id )
	
		throws TranscodeException
	{
		TranscodeProfile profile = manager.getProfileFromUID( profile_id );
		
		if ( profile == null ){
			
			throw( new TranscodeException( "Transcode profile with id '" + profile_id + "' not found" ));
		}
		
		return( profile );
	}
	
	protected DiskManagerFileInfo
	lookupFile(
		byte[]		hash,
		int			index )
	
		throws TranscodeException
	{
		return( manager.lookupFile( hash, index ));
	}
	
	protected void
	configDirty()
	{
		synchronized( this ){
			
			if ( config_dirty ){
				
				return;
			}
			
			config_dirty = true;
		
			new DelayedEvent( 
				"TranscodeQueue:save", 5000,
				new AERunnable()
				{
					public void 
					runSupport() 
					{
						synchronized( TranscodeQueueImpl.this ){
							
							if ( !config_dirty ){

								return;
							}
							
							saveConfig();
						}	
					}
				});
		}
	}
	
	protected void
	loadConfig()
	{
		if ( !FileUtil.resilientConfigFileExists( CONFIG_FILE )){
			
			return;
		}
		
		log( "Loading configuration" );
			
		try{
			synchronized( this ){
				
				Map map = FileUtil.readResilientConfigFile( CONFIG_FILE );
				
				List<Map<String,Object>>	l_jobs = (List<Map<String,Object>>)map.get( "jobs" );
				
				if ( l_jobs != null ){
					
					for ( Map<String,Object> m: l_jobs ){
						
						try{
							TranscodeJobImpl job = new TranscodeJobImpl( this, m );
						
							queue.add(job );
							
							queue_sem.release();
							
						}catch( Throwable e ){
							
							log( "Failed to restore job: " + m, e );
						}
					}
				}
			}
		}catch( Throwable e ){
			
			log( "Configuration load failed", e );
		}
	}
	
	protected void
	saveConfig()
	{
		synchronized( this ){

			config_dirty = false;
			
			if ( queue.size() == 0 ){

				FileUtil.deleteResilientConfigFile( CONFIG_FILE );

			}else{
				
				Map<String,Object>	map = new HashMap<String,Object>();
				
				List<Map<String,Object>>	l_jobs = new ArrayList<Map<String,Object>>();
				
				map.put( "jobs", l_jobs );
				
				for ( TranscodeJobImpl job: queue ){
				
					if ( job.isStream()){
						
						continue;
					}
					
					try{
					
						l_jobs.add( job.toMap());
						
					}catch( Throwable e ){
						
						log( "Failed to save job", e );
					}
				}
				
				FileUtil.writeResilientConfigFile( CONFIG_FILE, map );
			}
		}
	}
	
	protected void
	close()
	{
		if ( config_dirty ){
		
			saveConfig();
		}
	}
	
	public void
	addListener(
		TranscodeQueueListener		listener )
	{
		if (!listeners.contains(listener)) {
			listeners.add( listener );
		}
	}
	
	public void
	removeListener(
		TranscodeQueueListener		listener )
	{
		listeners.remove( listener );	
	}
	
	protected void
	log( 
		String	str )
	{
		manager.log( "Queue: " + str );
	}
	
	protected void
	log( 
		String		str,
		Throwable	e )
	{
		manager.log( "Queue: " + str, e );
	}
	
	public void
	generate(
		IndentWriter		writer )
	{
		writer.println( "Transcode Queue: paused=" + paused + ",max_bps=" + max_bytes_per_sec );
		
		try{
			writer.indent();
	
			TranscodeJobImpl[] jobs = getJobs();
			
			for ( TranscodeJobImpl job: jobs ){
				
				job.generate( writer );
			}
		}finally{
			
			writer.exdent();
		}
	}
}
