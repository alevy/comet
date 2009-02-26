/*
 * File    : TableColumnManager.java
*
 * Copyright (C) 2004, 2005, 2006 Aelitis SAS, All rights Reserved
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details ( see the LICENSE file ).
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 *
 * AELITIS, SAS au capital de 46,603.30 euros,
 * 8 Allee Lenotre, La Grille Royale, 78600 Le Mesnil le Roi, France.
 */
 
package org.gudy.azureus2.ui.swt.views.table.utils;

import java.util.*;

import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.util.*;
import org.gudy.azureus2.ui.swt.views.table.TableColumnCoreCreationListener;

import com.aelitis.azureus.ui.common.table.TableColumnCore;
import com.aelitis.azureus.ui.common.table.impl.TableColumnImpl;

import org.gudy.azureus2.plugins.download.Download;
import org.gudy.azureus2.plugins.download.DownloadTypeComplete;
import org.gudy.azureus2.plugins.download.DownloadTypeIncomplete;
import org.gudy.azureus2.plugins.ui.tables.*;


/** Holds a list of column definitions (TableColumnCore) for 
 * all the tables in Azureus.
 *
 * Column definitions are added via 
 * PluginInterface.addColumn(TableColumn)
 * See Use javadoc section for more uses.
 *
 * @author Oliver (Original Code)
 * @author TuxPaper (Modifications to make generic & comments)
 */
public class TableColumnManager {

  private static final String CONFIG_FILE = "tables.config";
	private static TableColumnManager instance;
  private static AEMonitor 			class_mon 	= new AEMonitor( "TableColumnManager" );

  /* Holds all the TableColumnCore objects.
   * key   = TABLE_* type (see TableColumnCore)
   * value = Map:
   *           key = column name
   *           value = TableColumnCore object
   */
  private Map<String,Map>	items;
  private AEMonitor 		items_mon 	= new AEMonitor( "TableColumnManager:items" );
  
  /**
   * Holds the order in which the columns are auto-hidden
   * 
   * key   = TABLE_* type
   * value = List of TableColumn, indexed in the order they should be removed
   */ 
  private Map autoHideOrder = new LightHashMap();
	private Map mapTablesConfig; // key = table; value = map of columns
	private static Comparator orderComparator;
	
	private Map<String, TableColumnCreationListener> mapColumnIDsToListener = new LightHashMap();
	private Map<Class, List> mapDataSourceTypeToColumnIDs = new LightHashMap();

	static {
		orderComparator = new Comparator() {
			public int compare(Object arg0, Object arg1) {				
				if ((arg1 instanceof TableColumn) && (arg0 instanceof TableColumn)) {
					int iPositionA = ((TableColumn) arg0).getPosition();
					if (iPositionA < 0)
						iPositionA = 0xFFFF + iPositionA;
					int iPositionB = ((TableColumn) arg1).getPosition();
					if (iPositionB < 0)
						iPositionB = 0xFFFF + iPositionB;

					return iPositionA - iPositionB;
				}
				return 0;
			}
		};
	}

  
  private TableColumnManager() {
   items = new HashMap<String,Map>();

   mapTablesConfig = FileUtil.readResilientConfigFile(CONFIG_FILE);
  }
  
  /** Retrieve the static TableColumnManager instance
   * @return the static TableColumnManager instance
   */
  public static TableColumnManager getInstance() {
  	try{
  		class_mon.enter();
  	
  		if(instance == null)
  			instance = new TableColumnManager();
  		return instance;
  	}finally{
  		
  		class_mon.exit();
  	}
  }
  
  /** Adds a column definition to the list
   * @param item The column definition object
   */
  public void addColumns(TableColumnCore[] itemsToAdd) {
		try {
			items_mon.enter();
			for (int i = 0; i < itemsToAdd.length; i++) {
				TableColumnCore item = itemsToAdd[i];
				if ( item.isRemoved()){
					continue;
				}
				String name = item.getName();
				String sTableID = item.getTableID();
				Map mTypes = (Map) items.get(sTableID);
				if (mTypes == null) {
					// LinkedHashMap to preserve order
					mTypes = new LinkedHashMap();
					items.put(sTableID, mTypes);
				}
				if (!mTypes.containsKey(name)) {
					mTypes.put(name, item);
					Map mapColumnConfig = getTableConfigMap(sTableID);
					mapTablesConfig.put("Table." + sTableID, mapColumnConfig);
					((TableColumnCore) item).loadSettings(mapColumnConfig);
				}
				if (!item.getColumnAdded()) {
					item.setColumnAdded(true);
				}
			}
		} catch (Exception e) {
			System.out.println("Error while adding Table Column Extension");
			Debug.printStackTrace(e);
		} finally {
			items_mon.exit();
		}
	}

  /** Adds a column definition to the list
   * @param item The column definition object
   */
  public void removeColumns(TableColumnCore[] itemsToRemove) {
		try {
			items_mon.enter();
			for (int i = 0; i < itemsToRemove.length; i++) {
				TableColumnCore item = itemsToRemove[i];
				String name = item.getName();
				String sTableID = item.getTableID();
				Map mTypes = (Map) items.get(sTableID);
				if (mTypes != null) {
					if ( mTypes.remove(name) != null ){
					}
				}
			}
		} catch (Exception e) {
			System.out.println("Error while adding Table Column Extension");
			Debug.printStackTrace(e);
		} finally {
			items_mon.exit();
		}
	}
  
  /** Retrieves TableColumnCore objects of a particular type.
   * @param sTableID TABLE_* constant.  See {@link TableColumn} for list 
   * of constants
   * @param forDataSourceType 
   *
   * @return Map of column definition objects matching the supplied criteria.
   *         key = name
   *         value = TableColumnCore object
   */
  public Map<String, TableColumnCore> getTableColumnsAsMap(
			Class forDataSourceType, String sTableID) {
    //System.out.println("getTableColumnsAsMap(" + sTableID + ")");
    try{
    	items_mon.enter();
      Map<String, TableColumnCore> mReturn = new LinkedHashMap();
    	Map<String, TableColumnCore> mTypes = getAllTableColumnCore(
					forDataSourceType, sTableID);
      if (mTypes != null) {
        mReturn.putAll(mTypes);
      }
      //System.out.println("getTableColumnsAsMap(" + sTableID + ") returnsize: " + mReturn.size());
      return mReturn;
    }finally{
    	
    	items_mon.exit();
    }
  }
  
  public int getTableColumnCount(String sTableID) {
    Map mTypes = (Map)items.get(sTableID);
    if (mTypes == null) {
    	return 0;
    }
    return mTypes.size();
  }

  
  public TableColumnCore[] getAllTableColumnCoreAsArray(
			Class forDataSourceType, String tableID)
	{
  	Map mTypes = getAllTableColumnCore(forDataSourceType, tableID);
		return (TableColumnCore[]) mTypes.values().toArray(
				new TableColumnCore[mTypes.values().size()]);
	}

  /*
  private Map getAllTableColumnCore(
			Class forDataSourceType, String tableID)
	{
		String[] dstColumnIDs = new String[0];
		if (forDataSourceType != null) {
  		List listDST = (List) mapDataSourceTypeToColumnIDs.get(forDataSourceType);
  		if (listDST != null) {
  			dstColumnIDs = (String[]) listDST.toArray(new String[0]);
  		}
  		if (forDataSourceType.equals(DownloadTypeComplete.class)
  				|| forDataSourceType.equals(DownloadTypeIncomplete.class)) {
  			listDST = (List) mapDataSourceTypeToColumnIDs.get(Download.class);
  			if (listDST != null && listDST.size() > 0) {
  				String[] ids1 = (String[]) listDST.toArray(new String[0]);
  				String[] ids2 = dstColumnIDs;
  				dstColumnIDs = new String[ids2.length + ids1.length];
  				System.arraycopy(ids2, 0, dstColumnIDs, 0, ids2.length);
  				System.arraycopy(ids1, 0, dstColumnIDs, ids2.length, ids1.length);
  			}
  		} else if (forDataSourceType.equals(Download.class)) {
  			listDST = (List) mapDataSourceTypeToColumnIDs.get(DownloadTypeComplete.class);
  			if (listDST != null && listDST.size() > 0) {
  				String[] ids = (String[]) listDST.toArray(new String[listDST.size()]);
  				dstColumnIDs = appendLists(ids, dstColumnIDs);
  			}
  			listDST = (List) mapDataSourceTypeToColumnIDs.get(DownloadTypeIncomplete.class);
  			if (listDST != null && listDST.size() > 0) {
  				String[] ids = (String[]) listDST.toArray(new String[listDST.size()]);
  				dstColumnIDs = appendLists(ids, dstColumnIDs);
  			}
  		}
		}

		try {
			items_mon.enter();

			Map mTypes = (Map) items.get(tableID);
			if (mTypes == null) {
        mTypes = new LinkedHashMap();
				items.put(tableID, mTypes);
			}

			for (int i = 0; i < dstColumnIDs.length; i++) {
				String columnID = dstColumnIDs[i];
				if (!mTypes.containsKey(columnID)) {
					try {
						TableColumnCreationListener l = mapColumnIDsToListener.get(forDataSourceType
								+ "." + columnID);
						TableColumnCore tc = null;
						if (l instanceof TableColumnCoreCreationListener) {
							tc = ((TableColumnCoreCreationListener) l).createTableColumnCore(
									tableID, columnID);
						}
						if (tc == null) {
							tc = new TableColumnImpl(tableID, columnID);
						}

						l.tableColumnCreated(tc);

						addColumns(new TableColumnCore[] { tc });

					} catch (Exception e) {
						Debug.out(e);
					}
				}
			}

			return mTypes;
		} finally {
			items_mon.exit();
		}
	}
  */

  /**
   * Will create columns for tableID if needed
   */
  private Map<String, TableColumnCore> getAllTableColumnCore(
			Class forDataSourceType, String tableID) {
		Map mTypes = null;
		try {
			items_mon.enter();

			mTypes = (Map) items.get(tableID);
			if (mTypes == null) {
				mTypes = new LinkedHashMap();
				items.put(tableID, mTypes);
			}

			if (forDataSourceType != null) {
				List listDST = (List) mapDataSourceTypeToColumnIDs.get(forDataSourceType);
				if (listDST != null) {
					doAddCreate(mTypes, forDataSourceType, tableID, listDST);
				}
				if (forDataSourceType.equals(DownloadTypeComplete.class)
						|| forDataSourceType.equals(DownloadTypeIncomplete.class)) {
					listDST = (List) mapDataSourceTypeToColumnIDs.get(Download.class);
					if (listDST != null && listDST.size() > 0) {
						doAddCreate(mTypes, Download.class, tableID, listDST);
					}
				} else if (Download.class.equals(forDataSourceType)) {
					listDST = (List) mapDataSourceTypeToColumnIDs.get(DownloadTypeComplete.class);
					if (listDST != null && listDST.size() > 0) {
						doAddCreate(mTypes, DownloadTypeComplete.class, tableID, listDST);
					}
					listDST = (List) mapDataSourceTypeToColumnIDs.get(DownloadTypeIncomplete.class);
					if (listDST != null && listDST.size() > 0) {
						doAddCreate(mTypes, DownloadTypeIncomplete.class, tableID, listDST);
					}
				}
			}
		} finally {
			items_mon.exit();
		}

		return mTypes;
	}

  /** 
   * Helper for getAllTableColumnCore
	 * @param types
	 * @param listDST
	 *
	 * @since 4.0.0.5
	 */
	private void doAddCreate(Map mTypes, Class forDataSourceType, String tableID, List listDST) {
		for (Iterator iter = listDST.iterator(); iter.hasNext();) {
			String columnID = (String) iter.next();
			if (!mTypes.containsKey(columnID)) {
				try {
					TableColumnCreationListener l = mapColumnIDsToListener.get(forDataSourceType
							+ "." + columnID);
					TableColumnCore tc = null;
					if (l instanceof TableColumnCoreCreationListener) {
						tc = ((TableColumnCoreCreationListener) l).createTableColumnCore(
								forDataSourceType, tableID, columnID);
					}
					if (tc == null) {
						tc = new TableColumnImpl(forDataSourceType, tableID, columnID);
					}

					l.tableColumnCreated(tc);

					addColumns(new TableColumnCore[] {
						tc
					});

				} catch (Exception e) {
					Debug.out(e);
				}
			}
		}
	}

		public String[]
  	getTableIDs()
  	{
		try {
			items_mon.enter();

			Set<String> ids = items.keySet();
			
			return( ids.toArray( new String[ids.size()]));
			
		} finally {
			items_mon.exit();
		}
	}
  	
  public String[] appendLists(String[] list1, String[] list2) {
  	int size = list1.length + list2.length;
  	String[] list = new String[size];
  	System.arraycopy(list1, 0, list, 0, list1.length);
  	System.arraycopy(list2, 0, list, list1.length, list2.length);
  	return list;
  }
  
  public TableColumnCore getTableColumnCore(String sTableID,
                                            String sColumnName) {
    Map mTypes = (Map)items.get(sTableID);
    if (mTypes == null)
      return null;
    return (TableColumnCore)mTypes.get(sColumnName);
  }
  
  public void ensureIntegrety(String sTableID) {
    Map mTypes = (Map)items.get(sTableID);
    if (mTypes == null)
      return;

    TableColumnCore[] tableColumns = 
      (TableColumnCore[])mTypes.values().toArray(new TableColumnCore[mTypes.values().size()]);

    Arrays.sort(tableColumns, getTableColumnOrderComparator());

    int iPos = 0;
    for (int i = 0; i < tableColumns.length; i++) {
      int iCurPos = tableColumns[i].getPosition();
      if (iCurPos == TableColumnCore.POSITION_INVISIBLE) {
      	tableColumns[i].setVisible(false);
      } else {
        tableColumns[i].setPositionNoShift(iPos++);
      }
    }
  }
  
  public String getDefaultSortColumnName(String tableID) {
  	Map mapTableConfig = getTableConfigMap(tableID);
  	Object object = mapTableConfig.get("SortColumn");
  	if( object instanceof byte[])
  		object =  new String((byte[])object);
  	
  	if (object instanceof String) {
			return (String) object;
		}

		String s = COConfigurationManager.getStringParameter(tableID + ".sortColumn");
		if (s != null) {
			COConfigurationManager.removeParameter(tableID + ".sortColumn");
			COConfigurationManager.removeParameter(tableID + ".sortAsc");
		}
		return s;
  }
  
  public void setDefaultSortColumnName(String tableID, String columnName) {
  	Map mapTableConfig = getTableConfigMap(tableID);
  	mapTableConfig.put("SortColumn", columnName);
    FileUtil.writeResilientConfigFile(CONFIG_FILE, mapTablesConfig);
  }

  /** Saves all the user configurable Table Column settings at once, complete
   * with a COConfigurationManager.save().
   *
   * @param sTableID Table to save settings for
   */
  public void saveTableColumns(Class forDataSourceType, String sTableID) {
  	try {
  		Map mapTableConfig = getTableConfigMap(sTableID);
      TableColumnCore[] tcs = getAllTableColumnCoreAsArray(forDataSourceType,
					sTableID);
      for (int i = 0; i < tcs.length; i++) {
        if (tcs[i] != null)
          tcs[i].saveSettings(mapTableConfig);
      }
      FileUtil.writeResilientConfigFile(CONFIG_FILE, mapTablesConfig);
  	} catch (Exception e) {
  		Debug.out(e);
  	}
  }
  
  public boolean loadTableColumnSettings(Class forDataSourceType, String sTableID) {
  	try {
  		Map mapTableConfig = getTableConfigMap(sTableID);
  		if (mapTableConfig.isEmpty()) {
  			return false;
  		}
      TableColumnCore[] tcs = getAllTableColumnCoreAsArray(forDataSourceType,
					sTableID);
      for (int i = 0; i < tcs.length; i++) {
        if (tcs[i] != null)
          tcs[i].loadSettings(mapTableConfig);
      }
  	} catch (Exception e) {
  		Debug.out(e);
  	}
  	return true;
  }
  
  public Map getTableConfigMap(String sTableID) {
  	Map mapTableConfig = (Map) mapTablesConfig.get("Table." + sTableID);
  	if (mapTableConfig == null) {
  		mapTableConfig = new HashMap();
  		mapTablesConfig.put("Table." + sTableID, mapTableConfig);
  	}
  	return mapTableConfig;
  }
  
  public void setAutoHideOrder(String sTableID, String[] autoHideOrderColumnIDs) {
  	ArrayList autoHideOrderList = new ArrayList(autoHideOrderColumnIDs.length);
  	for (int i = 0; i < autoHideOrderColumnIDs.length; i++) {
			String sColumnID = autoHideOrderColumnIDs[i];
			TableColumnCore column = getTableColumnCore(sTableID, sColumnID);
			if (column != null) {
				autoHideOrderList.add(column);
			}
		}
  	
  	autoHideOrder.put(sTableID, autoHideOrderList);
  }
  
  public List getAutoHideOrder(String sTableID) {
		List list = (List) autoHideOrder.get(sTableID);
		if (list == null) {
			return Collections.EMPTY_LIST;
		}
		return list;
	}

	/**
	 * @param writer
	 */
	public void generateDiagnostics(IndentWriter writer) {
    try{
     	items_mon.enter();
     	
     	writer.println("TableColumns");

     	for (Iterator iter = items.keySet().iterator(); iter.hasNext();) {
     		String sTableID = (String)iter.next();
        Map mTypes = (Map)items.get(sTableID);

        writer.indent();
     		writer.println(sTableID + ": " + mTypes.size() + " columns:");
     		
     		writer.indent();
       	for (Iterator iter2 = mTypes.values().iterator(); iter2.hasNext();) {
       		TableColumnCore tc = (TableColumnCore)iter2.next();
       		tc.generateDiagnostics(writer);
       	}
        writer.exdent();

        writer.exdent();
			}
    } catch (Exception e) {
    	e.printStackTrace();
    } finally {
    	items_mon.exit();
    }
	}
	
	public static Comparator getTableColumnOrderComparator()
	{
		return orderComparator;
	}

	/**
	 * @param forDataSourceType
	 * @param columnID
	 * @param listener
	 *
	 * @since 3.1.1.1
	 */
	public void registerColumn(Class forDataSourceType, String columnID,
			TableColumnCreationListener listener) {
  	mapColumnIDsToListener.put(forDataSourceType + "." + columnID, listener);
		List list = (List) mapDataSourceTypeToColumnIDs.get(forDataSourceType);
		if (list == null) {
			list = new ArrayList(1);
			mapDataSourceTypeToColumnIDs.put(forDataSourceType, list);
		}
		list.add(columnID);
	}
	
	public void unregisterColumn(Class forDataSourceType, String columnID,
			TableColumnCreationListener listener) {
		mapColumnIDsToListener.remove(forDataSourceType + "." + columnID);
		List list = (List) mapDataSourceTypeToColumnIDs.get(forDataSourceType);
		if (list != null) {
			list.remove(columnID);
		}
	}

	public TableColumnInfo getColumnInfo(Class forDataSourceType, String forTableID,
			String columnID) {

		TableColumnCore column = getTableColumnCore(forTableID, columnID);
		
		TableColumnInfoImpl columnInfo = new TableColumnInfoImpl(column);
		List<TableColumnExtraInfoListener> listeners = column.getColumnExtraInfoListeners();
		for (TableColumnExtraInfoListener l : listeners) {
			l.fillTableColumnInfo(columnInfo);
		}
		if (columnInfo.getCategories() == null && !(column instanceof CoreTableColumn)) {
			columnInfo.addCategories(new String[] { "plugin" });
		}

		return columnInfo;
	}
}
