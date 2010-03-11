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

package com.aelitis.azureus.ui.swt.skin;

import org.gudy.azureus2.core3.util.Debug;

/**
 * Converts {@link SWTSkinObjectListener} events to method calls 
 * 
 * @author TuxPaper
 * @created Sep 30, 2006
 *
 */
public class SWTSkinObjectAdapter
	implements SWTSkinObjectListener
{
	public Object skinObjectShown(SWTSkinObject skinObject, Object params) {
		return null;
	}

	public Object skinObjectHidden(SWTSkinObject skinObject, Object params) {
		return null;
	}

	public Object skinObjectSelected(SWTSkinObject skinObject, Object params) {
		return null;
	}

	public Object skinObjectDestroyed(SWTSkinObject skinObject, Object params) {
		return null;
	}
	
	public Object skinObjectCreated(SWTSkinObject skinObject, Object params) {
		return null;
	}

	public Object updateLanguage(SWTSkinObject skinObject, Object params) {
		return null;
	}

	/* (non-Javadoc)
	 * @see com.aelitis.azureus.ui.swt.skin.SWTSkinObjectListener#eventOccured(com.aelitis.azureus.ui.swt.skin.SWTSkinObject, int, java.lang.Object)
	 */
	public Object eventOccured(SWTSkinObject skinObject, int eventType,
			Object params) {
		try {
			switch (eventType) {
				case EVENT_SHOW:
					return skinObjectShown(skinObject, params);

				case EVENT_HIDE:
					return skinObjectHidden(skinObject, params);

				case EVENT_SELECT:
					return skinObjectSelected(skinObject, params);
					
				case EVENT_DESTROY:
					return skinObjectDestroyed(skinObject, params);

				case EVENT_CREATED:
					return skinObjectCreated(skinObject, params);
					
				case EVENT_LANGUAGE_CHANGE:
					return updateLanguage(skinObject, params);

				default:
					return null;
			}

		} catch (Exception e) {
			Debug.out("Skin Event " + NAMES[eventType] + " caused an error", e);
		}
		return null;
	}

}
