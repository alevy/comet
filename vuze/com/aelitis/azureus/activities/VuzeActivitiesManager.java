/**
 * Created on Jan 28, 2008 
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

package com.aelitis.azureus.activities;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;

import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.util.*;

import com.aelitis.azureus.core.AzureusCore;
import com.aelitis.azureus.core.AzureusCoreLifecycleAdapter;
import com.aelitis.azureus.core.cnetwork.*;
import com.aelitis.azureus.core.messenger.config.PlatformVuzeActivitiesMessenger;
import com.aelitis.azureus.util.ConstantsVuze;
import com.aelitis.azureus.util.MapUtils;

/**
 * Manage Vuze News Entries.  Loads, Saves, and expires them
 * 
 * @author TuxPaper
 * @created Jan 28, 2008
 *
 */
public class VuzeActivitiesManager
{
	public static final long MAX_LIFE_MS = 1000L * 60 * 60 * 24 * 30;

	private static final long DEFAULT_PLATFORM_REFRESH = 60 * 60 * 1000L * 24;

	private static final String SAVE_FILENAME = "VuzeActivities.config";

	private static ArrayList<VuzeActivitiesListener> listeners = new ArrayList<VuzeActivitiesListener>();

	private static ArrayList<VuzeActivitiesEntry> allEntries = new ArrayList<VuzeActivitiesEntry>();

	private static AEMonitor allEntries_mon = new AEMonitor("VuzeActivityMan");

	private static List<VuzeActivitiesEntry> removedEntries = new ArrayList<VuzeActivitiesEntry>();

	private static PlatformVuzeActivitiesMessenger.GetEntriesReplyListener replyListener;

	private static AEDiagnosticsLogger diag_logger;

	/** Key: NetworkID, Value: last time we pulled news **/ 
	private static Map<String, Long> lastNewsAt = new HashMap<String, Long>();

	private static boolean skipAutoSave = true;

	private static AEMonitor config_mon = new AEMonitor("ConfigMon");

	private static boolean saveEventsOnClose = false;

	static {
		if (System.getProperty("debug.vuzenews", "0").equals("1")) {
			diag_logger = AEDiagnostics.getLogger("v3.vuzenews");
			diag_logger.log("\n\nVuze News Logging Starts");
		} else {
			diag_logger = null;
		}
	}

	public static void initialize(final AzureusCore core) {
		new AEThread2("lazy init", true) {
			public void run() {
				_initialize(core);
			}
		}.start();
	}

	private static void _initialize(AzureusCore core) {
		if (diag_logger != null) {
			diag_logger.log("Initialize Called");
		}
		
		core.addLifecycleListener(new AzureusCoreLifecycleAdapter() {
			public void stopping(AzureusCore core) {
				if (saveEventsOnClose) {
					saveEventsNow();
				}
			}
		});

		loadEvents();

		ContentNetworkManager cnm = ContentNetworkManagerFactory.getSingleton();
		if (cnm != null) {
			ContentNetwork[] contentNetworks = cnm.getContentNetworks();
			cnm.addListener(new ContentNetworkListener() {

				public void networkRemoved(ContentNetwork network) {
				}

				public void networkChanged(ContentNetwork network) {
				}

				public void networkAdded(ContentNetwork cn) {
					setupContentNetwork(cn);
				}

				public void networkAddFailed(long network_id, Throwable error) {
				}
			});
			
			for (ContentNetwork cn : contentNetworks) {
				setupContentNetwork(cn);
			}
		}
		
		replyListener = new PlatformVuzeActivitiesMessenger.GetEntriesReplyListener() {
			public void gotVuzeNewsEntries(VuzeActivitiesEntry[] entries,
					long refreshInMS) {
				if (diag_logger != null) {
					diag_logger.log("Received Reply from platform with " + entries.length
							+ " entries.  Refresh in " + refreshInMS);
				}

				addEntries(entries);

				if (refreshInMS <= 0) {
					refreshInMS = DEFAULT_PLATFORM_REFRESH;
				}

				SimpleTimer.addEvent("GetVuzeNews",
						SystemTime.getOffsetTime(refreshInMS), new TimerEventPerformer() {
							public void perform(TimerEvent event) {
								pullActivitiesNow(5000);
							}
						});
			}
		};

		pullActivitiesNow(5000);
	}

	/**
	 * @param cn
	 *
	 * @since 4.0.0.5
	 */
	private static void setupContentNetwork(final ContentNetwork cn) {
		cn.addPersistentPropertyChangeListener(new ContentNetworkPropertyChangeListener() {
			// @see com.aelitis.azureus.core.cnetwork.ContentNetworkPropertyChangeListener#propertyChanged(java.lang.String)
			public void propertyChanged(String name) {
				if (!ContentNetwork.PP_ACTIVE.equals(name)) {
					return;
				}
				Object oIsActive = cn.getPersistentProperty(ContentNetwork.PP_ACTIVE);
				boolean isActive = (oIsActive instanceof Boolean)
						? ((Boolean) oIsActive).booleanValue() : false;
				if (isActive) {
					pullActivitiesNow(2000);
				}
			}
		});
		
		final String id_str = cn.getServiceURL( ContentNetwork.SERVICE_IDENTIFY );
		
		if ( id_str != null && id_str.length() > 0 ){
			
			try{
				SimpleTimer.addPeriodicEvent(
					"act:id",
					23*60*60*1000,
					new TimerEventPerformer()
					{
						public void 
						perform(
							TimerEvent event ) 
						{
							identify( cn, id_str );
						}
					});
				
				identify( cn, id_str );
				
			}catch( Throwable e ){
				
				Debug.out( e );
			}
		}
	}
	
	private static void
	identify(
		ContentNetwork		cn,
		String				str )
	{
		try{
			URL	url = new URL( str );
			
			HttpURLConnection con = (HttpURLConnection)url.openConnection();
			
			UrlUtils.setBrowserHeaders( con, null );
			
			String	key = "cn." + cn.getID() + ".identify.cookie";
			
			String cookie = COConfigurationManager.getStringParameter( key, null );
			
			if ( cookie != null ){
				
				con.setRequestProperty( "Cookie", cookie + ";" );
			}
			
			con.setRequestProperty( "Connection", "close" );
			
			con.getResponseCode();
			
			cookie = con.getHeaderField( "Set-Cookie" );
			
			if ( cookie != null ){
				
				String[] bits = cookie.split( ";" );
				
				if ( bits.length > 0 && bits[0].length() > 0 ){
					
					COConfigurationManager.setParameter( key, bits[0] );
				}
			}
		}catch( Throwable e ){
			
		}
	}

	/**
	 * Pull entries from webapp
	 * 
	 * @param agoMS Pull all events within this timespan (ms)
	 * @param delay max time to wait before running request
	 *
	 * @since 3.0.4.3
	 */
	public static void pullActivitiesNow(long delay) {
		/*
		ContentNetworkManager cnm = ContentNetworkManagerFactory.getSingleton();
		if (cnm == null) {
			return;
		}
		
		ContentNetwork[] contentNetworks = cnm.getContentNetworks();
		for (ContentNetwork cn : contentNetworks) {
		*/
		{
			// short circuit.. only get vuzenews from default network
			ContentNetwork cn = ConstantsVuze.getDefaultContentNetwork();
			if (cn == null) {
				return; //continue;
			}
			
			Object oIsActive = cn.getPersistentProperty(ContentNetwork.PP_ACTIVE);
			boolean isActive = (oIsActive instanceof Boolean)
					? ((Boolean) oIsActive).booleanValue() : false;
			if (!isActive) {
				return; //continue;
			}
			
			String id = "" + cn.getID();
			Long oLastPullTime = lastNewsAt.get(id);
			long lastPullTime = oLastPullTime != null ? oLastPullTime.longValue() : 0;
			long now = SystemTime.getCurrentTime();
			long diff = now - lastPullTime;
			if (diff < 5000) {
				return;
			}
			if (diff > MAX_LIFE_MS) {
				diff = MAX_LIFE_MS;
			}
			PlatformVuzeActivitiesMessenger.getEntries(cn.getID(), diff, delay,
					replyListener);
			lastNewsAt.put(id, new Long(now));
		}
	}
	
	public static void clearLastPullTimes() {
		lastNewsAt = new HashMap<String, Long>();
	}

	/**
	 * Clear the removed entries list so that an entry that was once deleted will
	 * will be able to be added again
	 * 
	 *
	 * @since 3.0.4.3
	 */
	public static void resetRemovedEntries() {
		removedEntries.clear();
		saveEvents();
	}

	/**
	 * 
	 *
	 * @since 3.1.1.1
	 */
	private static void saveEvents() {
		saveEventsOnClose  = true;
	}

	/**
	 * 
	 *
	 * @since 3.0.4.3
	 */
	private static void loadEvents() {
		skipAutoSave = true;

		try {
			Map<?,?> map = FileUtil.readResilientConfigFile(SAVE_FILENAME);

			// Clear all entries if we aren't on v2
			if (map != null && map.size() > 0
					&& MapUtils.getMapLong(map, "version", 0) < 2) {
				clearLastPullTimes();
				skipAutoSave = false;
				saveEventsNow();
				return;
			}
			
			long cutoffTime = getCutoffTime();

			try {
				lastNewsAt = MapUtils.getMapMap(map, "LastChecks", new HashMap());
			} catch (Exception e) {
				Debug.out(e);
			}

			// "LastCheck" backward compat
			if (lastNewsAt.size() == 0) {
  			long lastVuzeNewsAt = MapUtils.getMapLong(map, "LastCheck", 0);
  			if (lastVuzeNewsAt > 0) {
    			if (lastVuzeNewsAt < cutoffTime) {
    				lastVuzeNewsAt = cutoffTime;
    			}
  				lastNewsAt.put("" + ContentNetwork.CONTENT_NETWORK_VUZE, new Long(
  						lastVuzeNewsAt));
  			}
			}
			
			Object value;

			List newRemovedEntries = (List) MapUtils.getMapObject(map,
					"removed-entries", null, List.class);
			if (newRemovedEntries != null) {
				for (Iterator iter = newRemovedEntries.iterator(); iter.hasNext();) {
					value = iter.next();
					if (!(value instanceof Map)) {
						continue;
					}
					VuzeActivitiesEntry entry = createEntryFromMap((Map) value, true);

					if (entry != null && entry.getTimestamp() > cutoffTime) {
						removedEntries.add(entry);
					}
				}
			}

			value = map.get("entries");
			if (!(value instanceof List)) {
				return;
			}

			List entries = (List) value;
			List entriesToAdd = new ArrayList(entries.size());
			for (Iterator iter = entries.iterator(); iter.hasNext();) {
				value = iter.next();
				if (!(value instanceof Map)) {
					continue;
				}

				VuzeActivitiesEntry entry = createEntryFromMap((Map) value, true);

				if (entry != null) {
					if (entry.getTimestamp() > cutoffTime) {
						entriesToAdd.add(entry);
					}
				}
			}

			int num = entriesToAdd.size();
			if (num > 0) {
				addEntries((VuzeActivitiesEntry[]) entriesToAdd.toArray(new VuzeActivitiesEntry[num]));
			}
		} finally {
			skipAutoSave = false;
		}
	}

	private static void saveEventsNow() {
		if (skipAutoSave) {
			return;
		}

		try {
			config_mon.enter();

			Map mapSave = new HashMap();
			mapSave.put("LastChecks", lastNewsAt);
			mapSave.put("version", new Long(2));

			List entriesList = new ArrayList();

			VuzeActivitiesEntry[] allEntriesArray = getAllEntries();
			for (int i = 0; i < allEntriesArray.length; i++) {
				VuzeActivitiesEntry entry = allEntriesArray[i];
				if (entry == null) {
					continue;
				}

				boolean isHeader = VuzeActivitiesConstants.TYPEID_HEADER.equals(entry.getTypeID());
				if (!isHeader) {
					entriesList.add(entry.toMap());
				}
			}
			mapSave.put("entries", entriesList);

			List removedEntriesList = new ArrayList();
			for (Iterator<VuzeActivitiesEntry> iter = removedEntries.iterator(); iter.hasNext();) {
				VuzeActivitiesEntry entry = iter.next();
				removedEntriesList.add(entry.toDeletedMap());
			}
			mapSave.put("removed-entries", removedEntriesList);

			FileUtil.writeResilientConfigFile(SAVE_FILENAME, mapSave);

		} catch (Throwable t) {
			Debug.out(t);
		} finally {
			config_mon.exit();
		}
	}

	public static long getCutoffTime() {
		return SystemTime.getOffsetTime(-MAX_LIFE_MS);
	}

	public static void addListener(VuzeActivitiesListener l) {
		listeners.add(l);
	}

	public static void removeListener(VuzeActivitiesListener l) {
		listeners.remove(l);
	}

	/**
	 * 
	 * @param entries
	 * @return list of entries actually added (no dups)
	 *
	 * @since 3.0.4.3
	 */
	public static VuzeActivitiesEntry[] addEntries(VuzeActivitiesEntry[] entries) {
		long cutoffTime = getCutoffTime();

		ArrayList newEntries = new ArrayList(entries.length);
		ArrayList existingEntries = new ArrayList(0);

		try {
			allEntries_mon.enter();

			for (int i = 0; i < entries.length; i++) {
				VuzeActivitiesEntry entry = entries[i];
				boolean isHeader = VuzeActivitiesConstants.TYPEID_HEADER.equals(entry.getTypeID());
				if ((entry.getTimestamp() >= cutoffTime || isHeader)
						&& !removedEntries.contains(entry)) {
					if (allEntries.contains(entry)) {
						existingEntries.add(entry);
					} else {
						newEntries.add(entry);
						allEntries.add(entry);
					}
				}
			}
		} finally {
			allEntries_mon.exit();
		}

		VuzeActivitiesEntry[] newEntriesArray = (VuzeActivitiesEntry[]) newEntries.toArray(new VuzeActivitiesEntry[newEntries.size()]);

		if (newEntriesArray.length > 0) {
			saveEventsNow();

			Object[] listenersArray = listeners.toArray();
			for (int i = 0; i < listenersArray.length; i++) {
				VuzeActivitiesListener l = (VuzeActivitiesListener) listenersArray[i];
				l.vuzeNewsEntriesAdded(newEntriesArray);
			}
		}

		if (existingEntries.size() > 0) {
			if (newEntriesArray.length == 0) {
				saveEvents();
			}

  		for (Iterator<VuzeActivitiesEntry> iter = existingEntries.iterator(); iter.hasNext();) {
  			VuzeActivitiesEntry entry = iter.next();
  			triggerEntryChanged(entry);
  		}
		}

		return newEntriesArray;
	}

	public static void removeEntries(VuzeActivitiesEntry[] entries) {
		removeEntries(entries, false);
	}
	
	public static void removeEntries(VuzeActivitiesEntry[] entries, boolean allowReAdd) {
		long cutoffTime = getCutoffTime();

		try {
			allEntries_mon.enter();

			for (int i = 0; i < entries.length; i++) {
				VuzeActivitiesEntry entry = entries[i];
				if (entry == null) {
					continue;
				}
				allEntries.remove(entry);
				boolean isHeader = VuzeActivitiesConstants.TYPEID_HEADER.equals(entry.getTypeID());
				if (!allowReAdd && entry.getTimestamp() > cutoffTime && !isHeader) {
					removedEntries.add(entry);
				}
			}
		} finally {
			allEntries_mon.exit();
		}

		Object[] listenersArray = listeners.toArray();
		for (int i = 0; i < listenersArray.length; i++) {
			VuzeActivitiesListener l = (VuzeActivitiesListener) listenersArray[i];
			l.vuzeNewsEntriesRemoved(entries);
		}
		saveEventsNow();
	}

	public static VuzeActivitiesEntry getEntryByID(String id) {
		try {
			allEntries_mon.enter();

			for (Iterator<VuzeActivitiesEntry> iter = allEntries.iterator(); iter.hasNext();) {
				VuzeActivitiesEntry entry = iter.next();
				if (entry == null) {
					continue;
				}
				String entryID = entry.getID();
				if (entryID != null && entryID.equals(id)) {
					return entry;
				}
			}
		} finally {
			allEntries_mon.exit();
		}

		return null;
	}

	public static VuzeActivitiesEntry[] getAllEntries() {
		return allEntries.toArray(new VuzeActivitiesEntry[allEntries.size()]);
	}
	
	public static int getNumEntries() {
		return allEntries.size();
	}

	public static void log(String s) {
		if (diag_logger != null) {
			diag_logger.log(s);
		}
	}

	/**
	 * @param vuzeActivitiesEntry
	 *
	 * @since 3.0.4.3
	 */
	public static void triggerEntryChanged(VuzeActivitiesEntry entry) {
		Object[] listenersArray = listeners.toArray();
		for (int i = 0; i < listenersArray.length; i++) {
			VuzeActivitiesListener l = (VuzeActivitiesListener) listenersArray[i];
			l.vuzeNewsEntryChanged(entry);
		}
		saveEvents();
	}

	public static VuzeActivitiesEntry createEntryFromMap(Map map,
			boolean internalMap) {
		return createEntryFromMap(ContentNetwork.CONTENT_NETWORK_VUZE, map,
				internalMap);
	}

	/**
	 * @param map
	 * @return
	 *
	 * @since 3.0.5.3
	 */
	public static VuzeActivitiesEntry createEntryFromMap(
			long defaultContentNetworkID, Map map, boolean internalMap) {
		VuzeActivitiesEntry entry;
		String typeID = MapUtils.getMapString(map, "typeID", MapUtils.getMapString(
				map, "type-id", null));
		entry = new VuzeActivitiesEntry();
		entry.setContentNetworkID(defaultContentNetworkID);
		if (internalMap) {
			entry.loadFromInternalMap(map);
		} else {
			entry.loadFromExternalMap(map);
		}
		return entry;
	}
}
