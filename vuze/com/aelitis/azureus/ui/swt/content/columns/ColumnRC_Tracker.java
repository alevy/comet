/**
 * Created on Feb 26, 2009
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

package com.aelitis.azureus.ui.swt.content.columns;

import com.aelitis.azureus.core.content.RelatedContent;
import com.aelitis.azureus.ui.common.table.TableColumnCore;

import org.eclipse.swt.SWT;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.plugins.ui.tables.*;
import org.gudy.azureus2.ui.swt.Utils;
import org.gudy.azureus2.ui.swt.views.table.TableCellSWT;

/**
 * @author TuxPaper
 * @created Feb 26, 2009
 *
 */
public class ColumnRC_Tracker
	implements TableCellRefreshListener, TableCellAddedListener // , TableCellMouseListener
{
	public static final String COLUMN_ID = "rc_tracker";


	public ColumnRC_Tracker(TableColumn column) {
		column.initialize(TableColumn.ALIGN_LEAD, TableColumn.POSITION_INVISIBLE, 215);
		column.addListeners(this);
		column.setRefreshInterval(TableColumn.INTERVAL_GRAPHIC);
		column.setType(TableColumn.TYPE_TEXT_ONLY);
		
		if ( column instanceof TableColumnCore ){
			((TableColumnCore)column).setUseCoreDataSource( true );
		}
	}

	public void refresh(TableCell cell) {
		RelatedContent rc = (RelatedContent) cell.getDataSource();
		if (rc == null) {
			return;
		}

		String tracker = rc.getTracker();
		
		if ( !validTracker( tracker )){
			
			return;
		}

		cell.setText(tracker);
	}
	
	public void cellAdded(TableCell cell) {
		
		RelatedContent rc = (RelatedContent) cell.getDataSource();
		
		if ( cell instanceof TableCellSWT && rc != null && validTracker(  rc.getTracker())){
		
			((TableCellSWT)cell).setCursorID( SWT.CURSOR_HAND );
			
			cell.setToolTip( MessageText.getString( "rcm.rc_tracker.tt" ));
		}
	}
	
	public void cellMouseTrigger(final TableCellMouseEvent event) {
		if (event.eventType == TableRowMouseEvent.EVENT_MOUSEDOWN
				&& event.button == 1) {
			RelatedContent rc = (RelatedContent) event.cell.getDataSource();
			
			String tracker = rc.getTracker();
			
			if ( validTracker( tracker )){
				
				rc.setUnread( false );
				
				Utils.launch( "http://" + tracker + "/" );
			}
		}
	}
	
	private boolean
	validTracker(
		String	tracker )
	{
		if ( tracker == null || tracker.length() == 0 ){
			
			return( false );
		}
		
		int pos = tracker.lastIndexOf( '.' );
		
		if ( pos == -1 ){
			
			return( false );
		}
		
		for ( int i=pos+1;i<tracker.length();i++){
			
			if ( !Character.isDigit( tracker.charAt( i ))){
				
				return( true );
			}
		}
		
		return( false );
	}
}
