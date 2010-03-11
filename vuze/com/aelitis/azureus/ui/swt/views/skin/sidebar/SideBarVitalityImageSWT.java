/**
 * Created on Sep 15, 2008
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;

import org.gudy.azureus2.core3.util.*;
import org.gudy.azureus2.ui.swt.Utils;

import com.aelitis.azureus.ui.swt.imageloader.ImageLoader;

import org.gudy.azureus2.plugins.ui.sidebar.*;

/**
 * @author TuxPaper
 * @created Sep 15, 2008
 *
 */
public class SideBarVitalityImageSWT
	implements SideBarVitalityImage
{
	private String imageID;

	private final SideBarEntrySWT sideBarEntry;

	private List listeners = Collections.EMPTY_LIST;

	private String tooltip;

	private Rectangle hitArea;

	private boolean visible = true;

	private int currentAnimationIndex;

	private String suffix = "";

	private TimerEventPerformer performer;

	private TimerEventPeriodic timerEvent;

	private Image[] images;
	
	private int delayTime = -1;

	private String fullImageID;
	
	private int alignment = SWT.RIGHT;

	/**
	 * @param imageID
	 */
	public 
	SideBarVitalityImageSWT(
		SideBarEntrySWT entry, 
		String 			imageID )
	{
		performer = 
			new TimerEventPerformer() 
			{		
				private boolean	exec_pending 	= false;
				private Object	lock			= this;
				
				public void 
				perform(
					TimerEvent event ) 
				{
					synchronized( lock ){
						
						if ( exec_pending ){
					
							return;
						}
						
						exec_pending = true;
					}
					
					Utils.execSWTThread(
						new AERunnable() 
						{
							public void 
							runSupport() 
							{
								synchronized( lock ){
									
									exec_pending = false;
								}
								
								if (images == null || images.length == 0 || !visible
										|| hitArea == null) {
									return;
								}
								currentAnimationIndex++;
								if (currentAnimationIndex >= images.length) {
									currentAnimationIndex = 0;
								}
								TreeItem treeItem = sideBarEntry.getTreeItem();
								if (treeItem == null || treeItem.isDisposed()) {
									return;
								}
								Tree parent = treeItem.getParent();
								parent.redraw(hitArea.x, hitArea.y, hitArea.width, hitArea.height, true);
								parent.update();
							}
						});
				}
			};

		this.sideBarEntry = entry;
		
		entry.addListener(new SideBarCloseListener() {
		
			public void sidebarClosed(SideBarEntry entry) {
				ImageLoader imageLoader = ImageLoader.getInstance();
				if (fullImageID != null) {
					imageLoader.releaseImage(fullImageID);
				}
			}
		});
		
		setImageID(imageID);
	}

	// @see org.gudy.azureus2.plugins.ui.sidebar.SideBarVitalityImage#getImageID()
	public String getImageID() {
		return imageID;
	}

	/**
	 * @return the sideBarEntry
	 */
	public SideBarEntry getSideBarEntry() {
		return sideBarEntry;
	}

	// @see org.gudy.azureus2.plugins.ui.sidebar.SideBarVitalityImage#addListener(org.gudy.azureus2.plugins.ui.sidebar.SideBarVitalityImageListener)
	public void addListener(SideBarVitalityImageListener l) {
		if (listeners == Collections.EMPTY_LIST) {
			listeners = new ArrayList(1);
		}
		listeners.add(l);
	}
	
	public void triggerClickedListeners(int x, int y) {
		Object[] list = listeners.toArray();
		for (int i = 0; i < list.length; i++) {
			SideBarVitalityImageListener l = (SideBarVitalityImageListener) list[i];
			try {
				l.sbVitalityImage_clicked(x, y);
			} catch (Exception e) {
				Debug.out(e);
			}
		}
	}

	// @see org.gudy.azureus2.plugins.ui.sidebar.SideBarVitalityImage#setTooltip(java.lang.String)
	public void setToolTip(String tooltip) {
		this.tooltip = tooltip;
	}

	public String getToolTip() {
		return tooltip;
	}

	/**
	 * @param bounds
	 *
	 * @since 3.1.1.1
	 */
	public void setHitArea(Rectangle hitArea) {
		this.hitArea = hitArea;
	}

	public Rectangle getHitArea() {
		return hitArea;
	}

	// @see org.gudy.azureus2.plugins.ui.sidebar.SideBarVitalityImage#getVisible()
	public boolean isVisible() {
		return visible;
	}

	// @see org.gudy.azureus2.plugins.ui.sidebar.SideBarVitalityImage#setVisible(boolean)
	public void setVisible(boolean visible) {
		if (this.visible == visible) {
			return;
		}
		this.visible = visible;

		if (visible) {
			createTimerEvent();
		} else if (timerEvent != null) {
			timerEvent.cancel();
		}

		Utils.execSWTThread(new AERunnable() {
			public void runSupport() {
				if (sideBarEntry != null) {
					sideBarEntry.redraw();
				}
			}
		});
	}

	/**
	 * 
	 *
	 * @since 3.1.1.1
	 */
	private void createTimerEvent() {
		if (timerEvent != null) {
			timerEvent.cancel();
		}
		if (images.length > 1) {
			ImageLoader imageLoader = ImageLoader.getInstance();
			int delay = delayTime == -1 ? imageLoader.getAnimationDelay(imageID)
					: delayTime;
			
			timerEvent = SimpleTimer.addPeriodicEvent("Animate " + imageID + suffix,
					delay, performer);
		}
	}

	/**
	 * @param images 
	 * @return the currentAnimationIndex
	 */
	public int getCurrentAnimationIndex(Image[] images) {
		if (currentAnimationIndex >= images.length) {
			currentAnimationIndex = 0;
		} else if (currentAnimationIndex < 0) {
			currentAnimationIndex = 0;
		}
		return currentAnimationIndex;
	}

	public void switchSuffix(String suffix) {
		if (suffix == null) {
			suffix = "";
		}
		if (suffix.equals(this.suffix)) {
			return;
		}
		this.suffix = suffix;
		setImageID(imageID);
	}

	public void setImageID(String id) {
		ImageLoader imageLoader = ImageLoader.getInstance();
		String newFullImageID = id + suffix;
		if (newFullImageID.equals(fullImageID)) {
			return;
		}
		if (fullImageID != null) {
			imageLoader.releaseImage(fullImageID);
		}
		this.imageID = id;
		images = imageLoader.getImages(newFullImageID);
		if (images == null || images.length == 0) {
			imageLoader.releaseImage(newFullImageID);
			newFullImageID = id;
			images = imageLoader.getImages(id);
		}
		fullImageID = newFullImageID;
		currentAnimationIndex = 0;
		createTimerEvent();
	}

	/**
	 * @return
	 *
	 * @since 3.1.1.1
	 */
	public Image getImage() {
		if (images == null || images.length == 0
				|| currentAnimationIndex >= images.length) {
			return null;
		}
		return images[currentAnimationIndex];
	}

	/**
	 * @param delayTime the delayTime to set
	 */
	public void setDelayTime(int delayTime) {
		if (this.delayTime == delayTime) {
			return;
		}
		this.delayTime = delayTime;
		createTimerEvent();
	}

	/**
	 * @return the delayTime
	 */
	public int getDelayTime() {
		return delayTime;
	}

	public int getAlignment() {
		return alignment;
	}

	public void setAlignment(int alignment) {
		this.alignment = alignment;
	}
}
