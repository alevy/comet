/*
 * Created on Aug 4, 2006 9:18:52 AM
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

import org.eclipse.swt.graphics.Color;

/**
 * @author TuxPaper
 * @created Aug 4, 2006
 *
 */
public interface SWTSkinObjectText
	extends SWTSkinObject
{
	public void setText(String text);

	public void setTextID(String id);
	
	public void setTextID(String id, String[] params);

	/**
	 * @return
	 *
	 * @since 3.1.1.1
	 */
	int getStyle();

	/**
	 * @param style
	 *
	 * @since 3.1.1.1
	 */
	void setStyle(int style);

	/**
	 * @return
	 *
	 * @since 4.1.0.5
	 */
	public String getText();

	/**
	 * @param l
	 *
	 * @since 4.2.0.7
	 */
	void addUrlClickedListener(SWTSkinObjectText_UrlClickedListener l);

	/**
	 * @param l
	 *
	 * @since 4.2.0.7
	 */
	void removeUrlClickedListener(SWTSkinObjectText_UrlClickedListener l);

	public void setTextColor(Color color);
}
