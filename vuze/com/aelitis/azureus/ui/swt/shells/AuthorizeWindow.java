/**
 * Created on Dec 2, 2008
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

package com.aelitis.azureus.ui.swt.shells;

import org.gudy.azureus2.ui.swt.Utils;

import com.aelitis.azureus.core.cnetwork.ContentNetwork;
import com.aelitis.azureus.core.messenger.ClientMessageContext;
import com.aelitis.azureus.core.messenger.browser.BrowserMessage;
import com.aelitis.azureus.core.messenger.browser.listeners.AbstractBrowserMessageListener;
import com.aelitis.azureus.util.ContentNetworkUtils;

/**
 * @author TuxPaper
 * @created Dec 2, 2008
 *
 */
public class AuthorizeWindow
{
	public static boolean open = false;

	public static boolean openAuthorizeWindow(final ContentNetwork cn) {
		if (open) {
			return false;
		}
		open = true;
		try {
  		String authURL = ContentNetworkUtils.getUrl(cn,
  				ContentNetwork.SERVICE_AUTHORIZE, new Object[] {
  					cn.getPersistentProperty(ContentNetwork.PP_SOURCE_REF)
  				});
  		BrowserWindow browserWindow = new BrowserWindow(Utils.findAnyShell(),
  				authURL, 560, 390, false, true);
  
  		final Boolean[] b = new Boolean[1];
  		b[0] = Boolean.FALSE;
  
  		ClientMessageContext context = browserWindow.getContext();
  		context.addMessageListener(new AbstractBrowserMessageListener(
  				"contentnetwork") {
  			public void handleMessage(BrowserMessage message) {
  				String opid = message.getOperationId();
  
  				if ("authorize".equals(opid)) {
  					cn.setPersistentProperty(ContentNetwork.PP_AUTH_PAGE_SHOWN,
  							Boolean.TRUE);
  					b[0] = Boolean.TRUE;
  				}
  			}
  		});
  
  		browserWindow.waitUntilClosed();
  
  		return b[0].booleanValue();
		} finally {
			open = false;
		}
	}
}
