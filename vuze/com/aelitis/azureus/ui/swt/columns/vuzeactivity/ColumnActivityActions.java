/**
 * Created on Sep 25, 2008
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

package com.aelitis.azureus.ui.swt.columns.vuzeactivity;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.*;
import org.eclipse.swt.widgets.Display;

import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.plugins.ui.tables.*;
import org.gudy.azureus2.ui.swt.Utils;
import org.gudy.azureus2.ui.swt.shells.GCStringPrinter;
import org.gudy.azureus2.ui.swt.shells.GCStringPrinter.URLInfo;
import org.gudy.azureus2.ui.swt.views.table.TableCellSWT;
import org.gudy.azureus2.ui.swt.views.table.TableCellSWTPaintListener;
import org.gudy.azureus2.ui.swt.views.table.utils.CoreTableColumn;

import com.aelitis.azureus.activities.VuzeActivitiesEntry;
import com.aelitis.azureus.ui.swt.UIFunctionsManagerSWT;
import com.aelitis.azureus.ui.swt.UIFunctionsSWT;
import com.aelitis.azureus.ui.swt.skin.SWTSkinFactory;
import com.aelitis.azureus.ui.swt.skin.SWTSkinProperties;
import com.aelitis.azureus.ui.swt.views.skin.TorrentListViewsUtils;
import com.aelitis.azureus.util.*;

/**
 * @author TuxPaper
 * @created Sep 25, 2008
 *
 */
public class ColumnActivityActions
	extends CoreTableColumn
	implements TableCellSWTPaintListener, TableCellRefreshListener,
	TableCellMouseMoveListener, TableCellAddedListener
{
	public static final String COLUMN_ID = "activityActions";

	private Color colorLinkNormal;

	private Color colorLinkHover;

	private static Font font = null;

	/**
	 * @param name
	 * @param tableID
	 */
	public ColumnActivityActions(String tableID) {
		super(COLUMN_ID, tableID);
		initializeAsGraphic(150);

		SWTSkinProperties skinProperties = SWTSkinFactory.getInstance().getSkinProperties();
		colorLinkNormal = skinProperties.getColor("color.links.normal");
		colorLinkHover = skinProperties.getColor("color.links.hover");
	}

	// @see org.gudy.azureus2.ui.swt.views.table.TableCellSWTPaintListener#cellPaint(org.eclipse.swt.graphics.GC, org.gudy.azureus2.ui.swt.views.table.TableCellSWT)
	public void cellPaint(GC gc, TableCellSWT cell) {
		VuzeActivitiesEntry entry = (VuzeActivitiesEntry) cell.getDataSource();
		if (entry == null) {
			return;
		}

		String text = cell.getText();

		if (text != null && text.length() > 0) {
			if (font == null) {
				FontData[] fontData = gc.getFont().getFontData();
				fontData[0].setStyle(SWT.BOLD);
				font = new Font(gc.getDevice(), fontData);
			}
			gc.setFont(font);

			Rectangle bounds = getDrawBounds(cell);

			GCStringPrinter sp = new GCStringPrinter(gc, text, bounds, true, true,
					SWT.WRAP | SWT.CENTER);

			sp.calculateMetrics();

			if (sp.hasHitUrl()) {
				URLInfo[] hitUrlInfo = sp.getHitUrlInfo();
				for (int i = 0; i < hitUrlInfo.length; i++) {
					URLInfo info = hitUrlInfo[i];
						// handle fake row when showing in column editor
					
					info.urlUnderline = cell.getTableRow() == null || cell.getTableRow().isSelected();
					if (info.urlUnderline) {
						info.urlColor = null;
					} else {
						info.urlColor = colorLinkNormal;
					}
				}
				int[] mouseOfs = cell.getMouseOffset();
				if (mouseOfs != null) {
					Rectangle realBounds = cell.getBounds();
					URLInfo hitUrl = sp.getHitUrl(mouseOfs[0] + realBounds.x, mouseOfs[1]
							+ realBounds.y);
					if (hitUrl != null) {
						hitUrl.urlColor = colorLinkHover;
					}
				}
			}

			sp.printString();
		}
	}
	
	// @see org.gudy.azureus2.plugins.ui.tables.TableCellAddedListener#cellAdded(org.gudy.azureus2.plugins.ui.tables.TableCell)
	public void cellAdded(TableCell cell) {
		cell.setMarginHeight(0);
	}

	// @see org.gudy.azureus2.plugins.ui.tables.TableCellRefreshListener#refresh(org.gudy.azureus2.plugins.ui.tables.TableCell)
	public void refresh(TableCell cell) {
		VuzeActivitiesEntry entry = (VuzeActivitiesEntry) cell.getDataSource();
		
		if(entry == null) return;
		
		if (!cell.setSortValue(entry.getTypeID()) && cell.isValid()) {
			return;
		}

		DownloadManager dm = entry.getDownloadManger();
		boolean canPlay = PlayUtils.canPlayDS(entry);
		boolean canDL = dm == null && entry.getDownloadManger() == null
				&& (entry.getTorrent() != null || entry.getAssetHash() != null);
		boolean canRun = !canPlay && dm != null;
		if (canRun && dm != null && !dm.getAssumedComplete()) {
			canRun = false;
		}

		StringBuffer sb = new StringBuffer();
		if (canDL) {
			if (sb.length() > 0) {
				sb.append(" | ");
			}
			sb.append("<A HREF=\"download\">Download</A>");
		}

		if (canPlay) {
			if (sb.length() > 0) {
				sb.append(" | ");
			}
			sb.append("<A HREF=\"play\">Play</A>");
		}

		if (canRun) {
			if (sb.length() > 0) {
				sb.append(", ");
			}
			sb.append("<A HREF=\"launch\">Launch</A>");
		}

		cell.setText(sb.toString());
	}

	// @see org.gudy.azureus2.plugins.ui.tables.TableCellMouseListener#cellMouseTrigger(org.gudy.azureus2.plugins.ui.tables.TableCellMouseEvent)
	public void cellMouseTrigger(TableCellMouseEvent event) {
		VuzeActivitiesEntry entry = (VuzeActivitiesEntry) event.cell.getDataSource();

		String tooltip = null;
		boolean invalidateAndRefresh = false;

		Rectangle bounds = ((TableCellSWT) event.cell).getBounds();
		String text = event.cell.getText();

		GCStringPrinter sp = null;
		GC gc = new GC(Display.getDefault());
		try {
			if (font != null) {
				gc.setFont(font);
			}
			Rectangle drawBounds = getDrawBounds((TableCellSWT) event.cell);
			sp = new GCStringPrinter(gc, text, drawBounds, true, true, SWT.WRAP
					| SWT.CENTER);
			sp.calculateMetrics();
		} catch (Exception e) {
			Debug.out(e);
		} finally {
			gc.dispose();
		}

		if (sp != null) {
			URLInfo hitUrl = sp.getHitUrl(event.x + bounds.x, event.y + bounds.y);
			int newCursor;
			if (hitUrl != null) {
				if (event.eventType == TableCellMouseEvent.EVENT_MOUSEUP) {
					if (hitUrl.url.equals("download")) {
						String referal = null;
						Object ds = event.cell.getDataSource();
						if (ds instanceof VuzeActivitiesEntry) {
							if (((VuzeActivitiesEntry) ds).isDRM()) {
								if (DataSourceUtils.isPlatformContent(ds)) {
									TorrentListViewsUtils.viewDetailsFromDS(
											event.cell.getDataSource(), "activity-dl");
								}
								return;
							}

							referal = DLReferals.DL_REFERAL_DASHACTIVITY + "-"
									+ ((VuzeActivitiesEntry) ds).getTypeID();
						}
						TorrentListViewsUtils.downloadDataSource(ds, false, referal);
						
					} else if (hitUrl.url.equals("play")) {
						String referal = null;
						Object ds = event.cell.getDataSource();
						if (ds instanceof VuzeActivitiesEntry) {
							if (((VuzeActivitiesEntry) ds).isDRM()
									&& ((VuzeActivitiesEntry) ds).getDownloadManger() == null) {
								if (DataSourceUtils.isPlatformContent(ds)) {
									TorrentListViewsUtils.viewDetailsFromDS(
											event.cell.getDataSource(), "thumb");
								}
								return;
							}
							referal = DLReferals.DL_REFERAL_PLAYDASHACTIVITY + "-"
									+ ((VuzeActivitiesEntry) ds).getTypeID();
						}
						TorrentListViewsUtils.playOrStreamDataSource(ds, null, referal);
						
					} else if (hitUrl.url.equals("launch")) {
						// run via play or stream so we get the security warning
						Object ds = event.cell.getDataSource();
						TorrentListViewsUtils.playOrStreamDataSource(ds, null,
								DLReferals.DL_REFERAL_LAUNCH);
						
					} else if (!UrlFilter.getInstance().urlCanRPC(hitUrl.url)) {
						Utils.launch(hitUrl.url);
					} else {
						UIFunctionsSWT uif = UIFunctionsManagerSWT.getUIFunctionsSWT();
						if (uif != null) {
							String target = hitUrl.target;
							if (target == null) {
								target = ContentNetworkUtils.getTarget(entry.getContentNetwork());
							}
							uif.viewURL(hitUrl.url, target, "column.activity.action");
							return;
						}
					}
				}
				Object ds = event.cell.getDataSource();

				newCursor = SWT.CURSOR_HAND;
				if (UrlFilter.getInstance().urlCanRPC(hitUrl.url)) {
					tooltip = hitUrl.title;
				} else {
					tooltip = hitUrl.url;
				}
			} else {
				newCursor = SWT.CURSOR_ARROW;
			}

			int oldCursor = ((TableCellSWT) event.cell).getCursorID();
			if (oldCursor != newCursor) {
				invalidateAndRefresh = true;
				((TableCellSWT) event.cell).setCursorID(newCursor);
			}
		}

		Object o = event.cell.getToolTip();
		if ((o == null) || (o instanceof String)) {
			String oldTooltip = (String) o;
			if (!StringCompareUtils.equals(oldTooltip, tooltip)) {
				invalidateAndRefresh = true;
				event.cell.setToolTip(tooltip);
			}
		}

		if (invalidateAndRefresh) {
			event.cell.invalidate();
			((TableCellSWT)event.cell).redraw();
		}
	}

	boolean bMouseDowned = false;

	private Rectangle getDrawBounds(TableCellSWT cell) {
		Rectangle bounds = cell.getBounds();
		bounds.height -= 12;
		bounds.y += 6;
		bounds.x += 4;
		bounds.width -= 4;

		return bounds;
	}
}
