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

package com.aelitis.azureus.ui.swt.views.skin.sidebar;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.*;

import com.aelitis.azureus.ui.common.updater.UIUpdatable;
import com.aelitis.azureus.ui.common.viewtitleinfo.ViewTitleInfo;
import com.aelitis.azureus.ui.swt.uiupdater.UIUpdaterSWT;

import org.gudy.azureus2.plugins.ui.sidebar.SideBarVitalityImage;

/**
 * @author TuxPaper
 * @created Aug 13, 2008
 *
 */
public class SideBarToolTips
	implements Listener, UIUpdatable
{
	Shell toolTipShell = null;

	Shell mainShell = null;

	Label toolTipLabel = null;

	private final Tree tree;

	private final SideBar sidebar;

	private SideBarEntrySWT sidebarEntry;

	private Point lastMouseHoverPos;

	/**
	 * Initialize
	 */
	public SideBarToolTips(SideBar sidebar, Tree tree) {
		this.sidebar = sidebar;
		this.tree = tree;
		mainShell = tree.getShell();

		tree.addListener(SWT.Dispose, this);
		tree.addListener(SWT.KeyDown, this);
		tree.addListener(SWT.MouseMove, this);
		tree.addListener(SWT.MouseHover, this);
		mainShell.addListener(SWT.Deactivate, this);
		tree.addListener(SWT.Deactivate, this);
	}

	public void handleEvent(Event event) {
		switch (event.type) {
			case SWT.MouseHover: {
				handleHover(new Point(event.x, event.y));
				break;
			}

			case SWT.Dispose:
				if (mainShell != null && !mainShell.isDisposed()) {
					mainShell.removeListener(SWT.Deactivate, this);
				}
				// fall through

			default:
				if (toolTipShell != null) {
					toolTipShell.dispose();
					toolTipShell = null;
					toolTipLabel = null;
				}
				break;
		} // switch
	} // handlEvent()

	/**
	 * 
	 *
	 * @since 3.1.1.1
	 */
	private void handleHover(Point mousePos) {
		if (toolTipShell != null && !toolTipShell.isDisposed())
			toolTipShell.dispose();

		if (tree.getItemCount() == 0) {
			return;
		}
		int indent = tree.getItem(0).getBounds().x;
		TreeItem treeItem = tree.getItem(new Point(indent, mousePos.y));
		if (treeItem == null) {
			return;
		}
		String id = (String) treeItem.getData("Plugin.viewID");
		sidebarEntry = SideBar.getEntry(id);

		String sToolTip = getToolTip(mousePos);
		if (sToolTip == null) {
			return;
		}

		lastMouseHoverPos = mousePos;

		Display d = tree.getDisplay();
		if (d == null)
			return;

		// We don't get mouse down notifications on trim or borders..
		toolTipShell = new Shell(tree.getShell(), SWT.ON_TOP);
		toolTipShell.addListener(SWT.Dispose, new Listener() {

			public void handleEvent(Event event) {
				UIUpdaterSWT.getInstance().removeUpdater(SideBarToolTips.this);
			}
		});
		FillLayout f = new FillLayout();
		try {
			f.marginWidth = 3;
			f.marginHeight = 1;
		} catch (NoSuchFieldError e) {
			/* Ignore for Pre 3.0 SWT.. */
		}
		toolTipShell.setLayout(f);
		toolTipShell.setBackground(d.getSystemColor(SWT.COLOR_INFO_BACKGROUND));

		toolTipLabel = new Label(toolTipShell, SWT.WRAP);
		toolTipLabel.setForeground(d.getSystemColor(SWT.COLOR_INFO_FOREGROUND));
		toolTipLabel.setBackground(d.getSystemColor(SWT.COLOR_INFO_BACKGROUND));
		toolTipLabel.setText(sToolTip.replaceAll("&", "&&"));
		// compute size on label instead of shell because label
		// calculates wrap, while shell doesn't
		Point size = toolTipLabel.computeSize(SWT.DEFAULT, SWT.DEFAULT);
		if (size.x > 600) {
			size = toolTipLabel.computeSize(600, SWT.DEFAULT, true);
		}
		size.x += toolTipShell.getBorderWidth() * 2 + 2;
		size.y += toolTipShell.getBorderWidth() * 2;
		try {
			size.x += toolTipShell.getBorderWidth() * 2 + (f.marginWidth * 2);
			size.y += toolTipShell.getBorderWidth() * 2 + (f.marginHeight * 2);
		} catch (NoSuchFieldError e) {
			/* Ignore for Pre 3.0 SWT.. */
		}
		Point pt = tree.toDisplay(mousePos.x, mousePos.y);
		Rectangle displayRect;
		try {
			displayRect = tree.getMonitor().getClientArea();
		} catch (NoSuchMethodError e) {
			displayRect = tree.getDisplay().getClientArea();
		}
		if (pt.x + size.x > displayRect.x + displayRect.width) {
			pt.x = displayRect.x + displayRect.width - size.x;
		}

		if (pt.y + size.y > displayRect.y + displayRect.height) {
			pt.y -= size.y + 2;
		} else {
			pt.y += 21;
		}

		if (pt.y < displayRect.y)
			pt.y = displayRect.y;

		toolTipShell.setBounds(pt.x, pt.y, size.x, size.y);
		toolTipShell.setVisible(true);
		UIUpdaterSWT.getInstance().addUpdater(this);

	}

	/**
	 * @return
	 *
	 * @since 3.1.1.1
	 */
	private String getToolTip(Point mousePos) {
		SideBarVitalityImage[] vitalityImages = sidebarEntry.getVitalityImages();
		for (int i = 0; i < vitalityImages.length; i++) {
			SideBarVitalityImageSWT vitalityImage = (SideBarVitalityImageSWT) vitalityImages[i];
			String indicatorToolTip = vitalityImage.getToolTip();
			if (indicatorToolTip == null || !vitalityImage.isVisible()) {
				continue;
			}
			Rectangle hitArea = vitalityImage.getHitArea();
			if (hitArea == null) {
				continue;
			}
			if (hitArea.contains(mousePos)) {
				return indicatorToolTip;
			}
		}

		if (sidebarEntry.titleInfo != null) {
			return (String) sidebarEntry.titleInfo.getTitleInfoProperty(ViewTitleInfo.TITLE_INDICATOR_TEXT_TOOLTIP);
		}

		return null;
	}

	// @see com.aelitis.azureus.ui.common.updater.UIUpdatable#getUpdateUIName()
	public String getUpdateUIName() {
		return "SideBarToolTips";
	}

	// @see com.aelitis.azureus.ui.common.updater.UIUpdatable#updateUI()
	public void updateUI() {
		if (toolTipLabel == null || toolTipLabel.isDisposed()) {
			return;
		}
		if (sidebarEntry == null || sidebarEntry.titleInfo == null) {
			return;
		}
		String sToolTip = getToolTip(lastMouseHoverPos);
		if (sToolTip == null) {
			return;
		}

		toolTipLabel.setText(sToolTip.replaceAll("&", "&&"));
	}
}
