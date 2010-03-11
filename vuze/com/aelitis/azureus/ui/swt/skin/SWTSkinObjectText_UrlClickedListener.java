/**
 * Created on Jan 15, 2010
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
 
package com.aelitis.azureus.ui.swt.skin;

import org.gudy.azureus2.ui.swt.shells.GCStringPrinter.URLInfo;

/**
 * @author TuxPaper
 * @created Jan 15, 2010
 *
 */
public interface SWTSkinObjectText_UrlClickedListener
{
	/**
	 * 
	 * @param urlInfo
	 * @return true = url processed; false = do default
	 */
	public boolean urlClicked(URLInfo urlInfo);
}
