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

package com.aelitis.azureus.ui.swt.views.skin;

import java.util.*;

import org.gudy.azureus2.core3.util.AEMonitor2;
import org.gudy.azureus2.core3.util.Debug;

import com.aelitis.azureus.ui.swt.skin.SWTSkinObject;
import com.aelitis.azureus.ui.swt.skin.SWTSkinObjectListener;

/**
 * Manages a list of SkinViews currently in use by the app
 * 
 * @author TuxPaper
 * @created Oct 6, 2006
 *
 */
public class SkinViewManager
{

	private static Map<Class<?>, List<SkinView>> mapSkinViews = new HashMap<Class<?>, List<SkinView>>();
	
	private static AEMonitor2 mon_skinViews = new AEMonitor2("skinViews");

	/**
	 * Map SkinObjectID to skin view
	 */
	private static Map<String, SkinView> skinIDs = new HashMap<String, SkinView>();
	
	private static List listeners = new ArrayList();
	
	/**
	 * @param key
	 * @param skinView
	 */
	public static void add(final SkinView skinView) {
		mon_skinViews.enter();
		try {
  		List<SkinView> list = mapSkinViews.get(skinView);
  		if (list == null) {
  			list = new ArrayList<SkinView>(1);
  			mapSkinViews.put(skinView.getClass(), list);
  		}
  		list.add(skinView);
		} finally {
			mon_skinViews.exit();
		}
		
		SWTSkinObject mainSkinObject = skinView.getMainSkinObject();
		if (mainSkinObject != null) {
			skinIDs.put(mainSkinObject.getSkinObjectID(), skinView);
		}

		triggerViewAddedListeners(skinView);
	}

	public static void remove(SkinView skinView) {
		if (skinView == null) {
			return;
		}

		mon_skinViews.enter();
		try {
			List<SkinView> list = mapSkinViews.get(skinView.getClass());
			if (list != null) {
  			list.remove(skinView);
  			if (list.isEmpty()) {
  				mapSkinViews.remove(skinView.getClass());
  			}
			}
		} finally {
			mon_skinViews.exit();
		}
		
		SWTSkinObject mainSkinObject = skinView.getMainSkinObject();
		if (mainSkinObject != null) {
			skinIDs.remove(mainSkinObject.getSkinObjectID());
		}
	}

	/**
	 * Gets the first SkinView created of the specified class
	 * 
	 * @param cla
	 * @return
	 */
	public static SkinView getByClass(Class<?> cla) {
		List<SkinView> list = mapSkinViews.get(cla);
		if (list == null) {
			return null;
		}
		
		Object[] skinViews = list.toArray();
		for (int i = 0; i < skinViews.length; i++) {
			SkinView sv = (SkinView) skinViews[i];

			SWTSkinObject so = sv.getMainSkinObject();
  		if (so != null) {
    		if (!so.isDisposed()) {
    			return sv;
    		}
  			remove(sv);
  		}
		}

		return null;
	}
	
	/**
	 * Return all added SkinViews of a certain class
	 * 
	 * @param cla
	 * @return
	 */
	public static SkinView[] getMultiByClass(Class<?> cla) {
		List<SkinView> list = mapSkinViews.get(cla);
		if (list == null) {
			return new SkinView[0];
		}
		return list.toArray(new SkinView[0]);
	}

	/**
	 * Get the SkinView related to a SkinObjectID 
	 * 
	 * @param id
	 * @return
	 */
	public static SkinView getBySkinObjectID(String id) {
		SkinView sv = skinIDs.get(id);
		if (sv != null) {
  		SWTSkinObject so = sv.getMainSkinObject();
  		if (so != null && so.isDisposed()) {
  			remove(sv);
  			return null;
  		}
		}
		return sv;
	}
	
	/**
	 * Listen in on SkinView adds
	 * 
	 * @param l
	 */
	public static void addListener(SkinViewManagerListener l) {
		synchronized (SkinViewManager.class) {
			if (!listeners.contains(l)) {
				listeners.add(l);
			}
		}
	}
	
	public static void RemoveListener(SkinViewManagerListener l) {
		synchronized (SkinViewManager.class) {
			listeners.remove(l);
		}
	}
	
	public static void triggerViewAddedListeners(SkinView skinView) {
		Object[] array = listeners.toArray();
		for (int i = 0; i < array.length; i++) {
			SkinViewManagerListener l = (SkinViewManagerListener) array[i];
			try {
				l.skinViewAdded(skinView);
			} catch (Exception e) {
				Debug.out(e);
			}
		}
	}
	
	public static interface SkinViewManagerListener {
		public void skinViewAdded(SkinView skinview);
	}
}
