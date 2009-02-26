/**
 * Created on Aug 13, 2008
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
 
package org.gudy.azureus2.plugins.ui.sidebar;

/**
 * @author TuxPaper
 * @created Aug 13, 2008
 *
 */
public interface SideBarEntry
{

	public String getParentID();

	public Object getDatasource();

	public boolean isCloseable();

	public Class getIViewClass();

	public Class[] getIViewClassArgs();

	public Object[] getIViewClassVals();

	public String getId();

	public SideBarVitalityImage addVitalityImage(String imageID);

	/**
	 * @param l
	 *
	 * @since 4.1.0.3
	 */
	void addListener(SideBarCloseListener l);

	/**
	 * @param l
	 *
	 * @since 4.1.0.3
	 */
	void removeListener(SideBarCloseListener l);

	/**
	 * @param l
	 *
	 * @since 4.1.0.3
	 */
	void addListener(SideBarOpenListener l);

	/**
	 * @param l
	 *
	 * @since 4.1.0.3
	 */
	void removeListener(SideBarOpenListener l);
}
