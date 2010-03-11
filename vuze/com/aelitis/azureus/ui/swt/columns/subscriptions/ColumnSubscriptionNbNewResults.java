/**
 * Copyright (C) 2008 Vuze Inc., All Rights Reserved.
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
 */

package com.aelitis.azureus.ui.swt.columns.subscriptions;

import org.gudy.azureus2.plugins.ui.tables.TableCell;
import org.gudy.azureus2.plugins.ui.tables.TableCellRefreshListener;
import org.gudy.azureus2.ui.swt.views.table.utils.CoreTableColumn;

import com.aelitis.azureus.core.subs.Subscription;

/**
 * @author Olivier Chalouhi
 * @created Oct 7, 2008
 *
 */
public class ColumnSubscriptionNbNewResults
	extends CoreTableColumn
	implements TableCellRefreshListener
{
	public static String COLUMN_ID = "nb-new-results";

	/** Default Constructor */
	public ColumnSubscriptionNbNewResults(String sTableID) {
		super(COLUMN_ID, POSITION_LAST, 100, sTableID);
		setMinWidth(100);
		setMaxWidth(100);
	}

	public void refresh(TableCell cell) {
		int nbResults = 0;
		Subscription sub = (Subscription) cell.getDataSource();
		if (sub != null) {
			nbResults = sub.getHistory().getNumUnread();
		}

		if (!cell.setSortValue(nbResults) && cell.isValid()) {
			return;
		}

		if (!cell.isShown()) {
			return;
		}
		
		cell.setText("" + nbResults);
		return;
		
	}
}
