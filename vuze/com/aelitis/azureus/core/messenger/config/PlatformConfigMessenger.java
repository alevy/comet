/**
 * Copyright (C) 2006 Aelitis, All Rights Reserved.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
 *
 * AELITIS, SAS au capital de 63.529,40 euros
 * 8 Allee Lenotre, La Grille Royale, 78600 Le Mesnil le Roi, France.
 *
 */

package com.aelitis.azureus.core.messenger.config;

import java.util.*;

import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.core3.util.SystemTime;
import org.gudy.azureus2.platform.PlatformManager;
import org.gudy.azureus2.platform.PlatformManagerFactory;

import com.aelitis.azureus.core.cnetwork.ContentNetwork;
import com.aelitis.azureus.core.cnetwork.ContentNetworkManagerFactory;
import com.aelitis.azureus.core.messenger.PlatformMessage;
import com.aelitis.azureus.core.messenger.PlatformMessenger;
import com.aelitis.azureus.core.messenger.PlatformMessengerListener;
import com.aelitis.azureus.core.torrent.PlatformTorrentUtils;
import com.aelitis.azureus.util.*;

import org.gudy.azureus2.plugins.platform.PlatformManagerException;

/**
 * @author TuxPaper
 * @created Sep 26, 2006
 *
 */
public class PlatformConfigMessenger
{
	public static final String LISTENER_ID = "config";

	private static boolean allowSendDeviceList = false;

	private static int iRPCVersion = 0;

	private static String playAfterURL = null;

	private static boolean sendStats = true;

	private static boolean doUrlQOS = false;
	
	private static boolean platformLoginComplete = false;

	protected static List platformLoginCompleteListeners = Collections.EMPTY_LIST;
	
	public static void login(long contentNetworkID, long maxDelayMS) {
		PlatformManager pm = PlatformManagerFactory.getPlatformManager();
		String azComputerID = "";
		try {
			azComputerID = pm.getAzComputerID();
		} catch (PlatformManagerException e) {
		}
		
		String sourceRef = null;
		if (contentNetworkID != ConstantsVuze.DEFAULT_CONTENT_NETWORK_ID) {
  		ContentNetwork cn = ContentNetworkManagerFactory.getSingleton().getContentNetwork(contentNetworkID);
  		sourceRef = (String) cn.getPersistentProperty(ContentNetwork.PP_SOURCE_REF);
		}
		if (sourceRef == null) {
			sourceRef = "unknown";
		}

		Object[] params = new Object[] {
			"version",
			org.gudy.azureus2.core3.util.Constants.AZUREUS_VERSION,
			"locale",
			Locale.getDefault().toString(),
			"azCID",
			azComputerID,
			"vid",
			COConfigurationManager.getStringParameter("ID"),
			"source-ref",
			sourceRef
		};
		PlatformMessage message = new PlatformMessage("AZMSG", LISTENER_ID,
				"login", params, maxDelayMS);
		message.setContentNetworkID(contentNetworkID);

		PlatformMessengerListener listener = new PlatformMessengerListener() {

			public void replyReceived(PlatformMessage message, String replyType,
					Map reply) {
				if (reply == null) {
					return;
				}
				
				boolean allowMulti = MapUtils.getMapBoolean(reply, "allow-multi-rpc",
						PlatformMessenger.getAllowMulti());
				PlatformMessenger.setAllowMulti(allowMulti);

				try {
					List listURLs = (List) MapUtils.getMapObject(reply, "url-whitelist",
							null, List.class);
					if (listURLs != null) {
						for (int i = 0; i < listURLs.size(); i++) {
							String string = (String) listURLs.get(i);
							UrlFilter.getInstance().addUrlWhitelist(string);
						}
					}
				} catch (Exception e) {
					Debug.out(e);
				}

				try {
					List listURLs = (List) MapUtils.getMapObject(reply, "url-blacklist",
							null, List.class);
					if (listURLs != null) {
						for (int i = 0; i < listURLs.size(); i++) {
							String string = (String) listURLs.get(i);
							UrlFilter.getInstance().addUrlBlacklist(string);
						}
					}
				} catch (Exception e) {
					Debug.out(e);
				}
				

				try {
					List listDomains = (List) MapUtils.getMapObject(reply, "tracker-domains",
							null, List.class);
					if (listDomains != null) {
						for (int i = 0; i < listDomains.size(); i++) {
							String s = (String) listDomains.get(i);
							PlatformTorrentUtils.addPlatformHost(s);
							PlatformMessenger.debug("v3.login: got tracker domain of " + s);
						}
					}
				} catch (Exception e) {
					Debug.out(e);
				}
				
				if (message.getContentNetworkID() != ConstantsVuze.getDefaultContentNetwork().getID()) {
					return;
				}
				
				try {
					sendStats = MapUtils.getMapBoolean(reply, "send-stats", false);
					doUrlQOS = MapUtils.getMapBoolean(reply, "do-url-qos", false);
					allowSendDeviceList = MapUtils.getMapBoolean(reply, "send-device-list", false);
				} catch (Exception e) {
				}
				
				
				try {
  				iRPCVersion = MapUtils.getMapInt(reply, "rpc-version", 0);
  				playAfterURL = (String) MapUtils.getMapString(reply,
  						"play-after-url", null);
				} catch (Exception e) {
					Debug.out(e);
				}
				
				platformLoginComplete = true;
				Object[] listeners = platformLoginCompleteListeners.toArray();
				platformLoginCompleteListeners = Collections.EMPTY_LIST;
				for (int i = 0; i < listeners.length; i++) {
					try {
						PlatformLoginCompleteListener l = (PlatformLoginCompleteListener) listeners[i];
						l.platformLoginComplete();
					} catch (Exception e) {
						Debug.out(e);
					}
				}
			}

			public void messageSent(PlatformMessage message) {
			}

		};

		PlatformMessenger.pushMessageNow(message, listener);
	}

	public static void sendUsageStats(Map stats, long timestamp, String version,
			PlatformMessengerListener l) {
		if (!sendStats) {
			return;
		}
		try {
			PlatformMessage message = new PlatformMessage("AZMSG", LISTENER_ID,
					"send-usage-stats2", new Object[] {
						"stats",
						stats,
						"version",
						version,
						"timestamp",
						new Long(timestamp),
						"ago-ms",
						new Long(SystemTime.getCurrentTime() - timestamp),
					}, 5000);

			PlatformMessenger.queueMessage(message, l);
		} catch (Exception e) {
			Debug.out(e);
		}
	}

	public static interface GetBrowseSectionsReplyListener
	{
		public void messageSent();

		public void replyReceived(Map[] browseSections);
	}

	
	/**
	 * @return the iRPCVersion
	 */
	public static int getRPCVersion() {
		return iRPCVersion;
	}

	public static String getPlayAfterURL() {
		return playAfterURL;
	}

	public static boolean allowSendStats() {
		return sendStats;
	}

	/**
	 * @return
	 *
	 * @since 4.0.0.1
	 */
	public static boolean doUrlQOS() {
		return doUrlQOS;
	}
	
	public static void addPlatformLoginCompleteListener(
			PlatformLoginCompleteListener l) {
		try {
			if (l == null) {
				return;
			}
			if (platformLoginComplete) {
				l.platformLoginComplete();
				return;
			}
			if (platformLoginCompleteListeners == Collections.EMPTY_LIST) {
				platformLoginCompleteListeners = new ArrayList(1);
			}
			platformLoginCompleteListeners.add(l);
		} catch (Exception e) {
			Debug.out(e);
		}
	}
	
	public static interface PlatformLoginCompleteListener {
		public void platformLoginComplete();
	}


	public static boolean allowSendDeviceList() {
		return allowSendDeviceList;
	}
}
