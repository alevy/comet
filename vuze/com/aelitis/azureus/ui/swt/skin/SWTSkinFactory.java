/*
 * Created on Jun 1, 2006 2:06:48 PM
 * Copyright (C) 2006 Aelitis, All Rights Reserved.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 *
 * AELITIS, SAS au capital de 46,603.30 euros
 * 8 Allee Lenotre, La Grille Royale, 78600 Le Mesnil le Roi, France.
 */
package com.aelitis.azureus.ui.swt.skin;

/**
 * @author TuxPaper
 * @created Jun 1, 2006
 *
 */
public class SWTSkinFactory
{
	private static SWTSkin instance = null;

	public static SWTSkin getInstance() {
		if (instance == null) {
			instance = new SWTSkin();
		}
		return instance;
	}

	public static void setInstance(SWTSkin skin) {
		instance = skin;
	}

	public static SWTSkin getNonPersistentInstance(ClassLoader classLoader, String skinPath,
			String mainSkinFile) {
		return new SWTSkin(classLoader, skinPath, mainSkinFile);
	}
}
