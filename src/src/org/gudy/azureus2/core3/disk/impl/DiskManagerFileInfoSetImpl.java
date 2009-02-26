/**
 * Copyright (C) 2008 Aelitis, All Rights Reserved.
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
 * AELITIS, SAS au capital de 63.529,40 euros
 * 8 Allee Lenotre, La Grille Royale, 78600 Le Mesnil le Roi, France.
 *
 */
package org.gudy.azureus2.core3.disk.impl;

import java.util.Arrays;

import org.gudy.azureus2.core3.disk.DiskManagerFileInfo;
import org.gudy.azureus2.core3.disk.DiskManagerFileInfoSet;
import org.gudy.azureus2.core3.disk.impl.DiskManagerImpl.FileSkeleton;
import org.gudy.azureus2.core3.download.DownloadManagerState;
import org.gudy.azureus2.core3.util.Debug;

import com.aelitis.azureus.core.diskmanager.cache.CacheFile;

/**
 * @author Aaron Grunthal
 * @create 10.05.2008
 */
public class DiskManagerFileInfoSetImpl implements DiskManagerFileInfoSet {

	final DiskManagerFileInfoImpl[] files;
	final DiskManagerHelper diskManager;
	
	public DiskManagerFileInfoSetImpl(DiskManagerFileInfoImpl[] files, DiskManagerHelper dm) {
		this.files = files;
		this.diskManager = dm;
	}
	
	public DiskManagerFileInfo[] getFiles() {
		return files;
	}

	public int nbFiles() {
		return files.length;
	}

	public void setPriority(boolean[] toChange, boolean setPriority) {
		if(toChange.length != files.length)
			throw new IllegalArgumentException("array length mismatches the number of files");

		DownloadManagerState dmState = diskManager.getDownloadState();
		
		try	{
			dmState.suppressStateSave(true);

		
			for(int i=0;i<files.length;i++)
				if(toChange[i])
					files[i].setPriority(setPriority);
		} finally {
			dmState.suppressStateSave(false);
		}
	}

	public void setSkipped(boolean[] toChange, boolean setSkipped) {
		if(toChange.length != files.length)
			throw new IllegalArgumentException("array length mismatches the number of files");

		DownloadManagerState dmState = diskManager.getDownloadState();
		
		try	{
			dmState.suppressStateSave(true);
			
			if (!setSkipped && !Arrays.equals(toChange, setStorageTypes(toChange, FileSkeleton.ST_LINEAR)))
				return;
			
			for (int i = 0; i < files.length; i++)
				if (toChange[i])
				{
					files[i].skipped = setSkipped;
					diskManager.skippedFileSetChanged(files[i]);
				}

			if(!setSkipped)
				DiskManagerUtil.doFileExistenceChecks(this, toChange, diskManager.getDownloadState().getDownloadManager(), true);
			
		} finally {
			dmState.suppressStateSave(false);
		}

	}

	public boolean[] setStorageTypes(boolean[] toChange, int newStroageType) {
		if(toChange.length != files.length)
			throw new IllegalArgumentException("array length mismatches the number of files");
		if(files.length == 0)
			return new boolean[0];
		
		String[] types = diskManager.getStorageTypes();
		
		boolean[] modified = new boolean[files.length];
		DownloadManagerState	dm_state = diskManager.getDownloadState();

		if (newStroageType == DiskManagerFileInfo.ST_COMPACT)
		{
			Debug.out("Download must be stopped for linear -> compact conversion");
			return modified;
		}
		
		try	{
			dm_state.suppressStateSave(true);

			for (int i = 0; i < files.length; i++)
			{
				if(!toChange[i])
					continue;
				
				int old_type = types[i].equals("L") ? DiskManagerFileInfo.ST_LINEAR : DiskManagerFileInfo.ST_COMPACT;
				if (newStroageType == old_type)
				{
					modified[i] = true;
					continue;
				}
			
				DiskManagerFileInfoImpl file = files[i];
				
				try	{
					file.getCacheFile().setStorageType(newStroageType == DiskManagerFileInfo.ST_LINEAR ? CacheFile.CT_LINEAR : CacheFile.CT_COMPACT);
					modified[i] = true;
				} catch (Throwable e) {
					Debug.printStackTrace(e);
					diskManager.setFailed(file, "Failed to change storage type for '" + file.getFile(true) + "': " + Debug.getNestedExceptionMessage(e));
					break;
				} finally {
					types[i] = file.getCacheFile().getStorageType() == CacheFile.CT_LINEAR ? "L" : "C";
				}
			}
			
			dm_state.setListAttribute(DownloadManagerState.AT_FILE_STORE_TYPES, types);
			
			DiskManagerUtil.doFileExistenceChecks(this, toChange, dm_state.getDownloadManager(), true);
			
		} finally {
			dm_state.suppressStateSave(false);
			dm_state.save();
		}
		
		return modified;
	}
}
