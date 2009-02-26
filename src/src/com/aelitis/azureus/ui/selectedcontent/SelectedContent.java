/**
 * Created on May 6, 2008
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

package com.aelitis.azureus.ui.selectedcontent;

import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.core3.global.GlobalManager;
import org.gudy.azureus2.core3.torrent.TOTorrent;
import org.gudy.azureus2.core3.util.Base32;
import org.gudy.azureus2.core3.util.HashWrapper;


import com.aelitis.azureus.core.AzureusCoreFactory;

/**
 * Represents a piece of content (torrent) that is selected
 * 
 * @author TuxPaper
 * @created May 6, 2008
 *
 */
public class SelectedContent implements ISelectedContent
{
	private String hash;

	private DownloadManager dm;

	private String displayName;
	
	private DownloadUrlInfo downloadInfo;

	/**
	 * @param dm2
	 * @throws Exception 
	 */
	public SelectedContent(DownloadManager dm){
		setDM(dm);
		TOTorrent t = dm.getTorrent();
		if ( t != null ){
			try{
				setHash(t.getHashWrapper().toBase32String());
				
			}catch( Throwable e ){
			}
		}
		setDisplayName(dm.getDisplayName());
	}

	/**
	 * 
	 */
	public SelectedContent(String hash, String displayName) {
		this.hash = hash;
		this.displayName = displayName;
	}

	public SelectedContent() {
	}

	// @see com.aelitis.azureus.ui.selectedcontent.ISelectedContent#getHash()
	public String getHash() {
		return hash;
	}

	// @see com.aelitis.azureus.ui.selectedcontent.ISelectedContent#setHash(java.lang.String)
	public void setHash(String hash) {
		this.hash = hash;
	}

	// @see com.aelitis.azureus.ui.selectedcontent.ISelectedContent#getDM()
	public DownloadManager getDM() {
		if (dm == null && hash != null) {
			GlobalManager gm = AzureusCoreFactory.getSingleton().getGlobalManager();
			return gm.getDownloadManager(new HashWrapper(Base32.decode(hash)));
		}
		return dm;
	}

	// @see com.aelitis.azureus.ui.selectedcontent.ISelectedContent#setDM(org.gudy.azureus2.core3.download.DownloadManager)
	public void setDM(DownloadManager dm) {
		this.dm = dm;
		if (this.dm != null) {
			try {
				hash = this.dm.getTorrent().getHashWrapper().toBase32String();
			} catch (Exception e) {
				hash = null;
			}
		}
	}

	// @see com.aelitis.azureus.ui.selectedcontent.ISelectedContent#getDisplayName()
	public String getDisplayName() {
		return displayName;
	}

	// @see com.aelitis.azureus.ui.selectedcontent.ISelectedContent#setDisplayName(java.lang.String)
	public void setDisplayName(String displayName) {
		this.displayName = displayName;
	}
	
	// @see com.aelitis.azureus.ui.selectedcontent.ISelectedContent#getDownloadInfo()
	public DownloadUrlInfo getDownloadInfo() {
		return downloadInfo;
	}
	
	// @see com.aelitis.azureus.ui.selectedcontent.ISelectedContent#setDownloadInfo(com.aelitis.azureus.ui.selectedcontent.SelectedContentDownloadInfo)
	public void setDownloadInfo(DownloadUrlInfo info) {
		this.downloadInfo = info;
	}
}
