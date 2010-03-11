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

import org.gudy.azureus2.core3.util.Debug;

import com.aelitis.azureus.ui.UIFunctionsManager;
import com.aelitis.azureus.ui.common.updater.UIUpdatable;
import com.aelitis.azureus.ui.swt.skin.*;

/**
 * Converts {@link SWTSkinObjectListener} events to method calls, and
 * ensures we only "show" (initialize) once.
 * <p>
 * Available SkinViews are managed by {@link SkinViewManager} 
 * 
 * @author TuxPaper
 * @created Sep 30, 2006
 *
 */
public abstract class SkinView
	extends SWTSkinObjectAdapter
{
	private boolean shownOnce;
	
	private boolean visible;

	protected SWTSkinObject soMain;

	protected SWTSkin skin;

	/**
	 * 
	 */
	public SkinView() {
		shownOnce = false;
		
		if (this instanceof UIUpdatable) {
			UIUpdatable updateable = (UIUpdatable) this;
			try {
				UIFunctionsManager.getUIFunctions().getUIUpdater().addUpdater(
						updateable);
			} catch (Exception e) {
				Debug.out(e);
			}
		}
	}

	/**
	 * @return the visible
	 */
	public boolean isVisible() {
		return visible;
	}

	public Object skinObjectShown(SWTSkinObject skinObject, Object params) {
		setMainSkinObject(skinObject);
		
		visible = true;

		if (shownOnce) {
			return null;
		}

		shownOnce = true;
		try {
			return skinObjectInitialShow(skinObject, params);
		} catch (Exception e) {
			Debug.out(e);
		}
		return null;
	}
	
	// @see com.aelitis.azureus.ui.swt.skin.SWTSkinObjectAdapter#skinObjectHidden(com.aelitis.azureus.ui.swt.skin.SWTSkinObject, java.lang.Object)
	public Object skinObjectHidden(SWTSkinObject skinObject, Object params) {
		visible = false;
		return super.skinObjectHidden(skinObject, params);
	}
	
	// @see com.aelitis.azureus.ui.swt.skin.SWTSkinObjectAdapter#skinObjectDestroyed(com.aelitis.azureus.ui.swt.skin.SWTSkinObject, java.lang.Object)
	public Object skinObjectDestroyed(SWTSkinObject skinObject, Object params) {
		SkinViewManager.remove(this);
		if (this instanceof UIUpdatable) {
			UIUpdatable updateable = (UIUpdatable) this;
			try {
				UIFunctionsManager.getUIFunctions().getUIUpdater().removeUpdater(
						updateable);
			} catch (Exception e) {
				Debug.out(e);
			}
		}
		return null;
	}

	/**
	 * @param skinObject
	 * @param params
	 * @return
	 */
	public abstract Object skinObjectInitialShow(SWTSkinObject skinObject, Object params);

	public SWTSkinObject getMainSkinObject() {
		return soMain;
	}

	final public void setMainSkinObject(SWTSkinObject main) {
		if (soMain != null) {
			return;
		}
		soMain = main;
		if (soMain != null) {
			skin = soMain.getSkin();
		}
		SkinViewManager.add(this);
	}

	final public SWTSkin getSkin() {
		return skin;
	}

	final public SWTSkinObject getSkinObject(String viewID) {
		return skin.getSkinObject(viewID, soMain);
	}
}
