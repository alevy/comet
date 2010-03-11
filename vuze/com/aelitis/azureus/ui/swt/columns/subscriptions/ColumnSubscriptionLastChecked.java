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
import org.gudy.azureus2.ui.swt.views.table.utils.TableColumnCreator;
import org.gudy.azureus2.ui.swt.views.tableitems.ColumnDateSizer;

import com.aelitis.azureus.core.subs.Subscription;

/**
 * @author Olivier Chalouhi
 * @created Oct 7, 2008
 *
 */
public class 
ColumnSubscriptionLastChecked
	extends ColumnDateSizer
{	
	public static String COLUMN_ID = "last-checked";
	
	public 
	ColumnSubscriptionLastChecked(
		String sTableID ) 
	{
		super(Subscription.class, COLUMN_ID, TableColumnCreator.DATE_COLUMN_WIDTH,
				sTableID);
		
		setMinWidth(100);
		
		setMultiline( false );
		
		setShowTime( true );
	}

	public void 
	refresh(
		TableCell 	cell, 
		long 		timestamp ) 
	{
		timestamp = 0;
				
		Subscription sub = (Subscription) cell.getDataSource();
		
		if ( sub != null ){

			timestamp = sub.getHistory().getLastScanTime();
		}

		if (!cell.setSortValue(timestamp) && cell.isValid()) {
			return;
		}

		if (!cell.isShown()) {
			return;
		}
		
		if ( timestamp <= 0 ){
		
			cell.setText( "--" );
			
		}else{
		
			super.refresh( cell, timestamp );
		}
	}
}
