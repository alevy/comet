/**
 * Copyright (C) 2006 Aelitis, All Rights Reserved.
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

package org.gudy.azureus2.ui.swt;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.core3.global.GlobalManager;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.security.SESecurityManager;
import org.gudy.azureus2.core3.tracker.client.TRTrackerScraperResponse;
import org.gudy.azureus2.core3.util.*;
import org.gudy.azureus2.ui.swt.shells.MessageBoxShell;

import com.aelitis.azureus.core.util.CopyOnWriteList;
import com.aelitis.azureus.ui.swt.UIFunctionsManagerSWT;

/**
 * @author TuxPaper
 * @created Nov 6, 2006
 *
 */
public class UIExitUtilsSWT
{
	private static boolean skipCloseCheck = false;
	
	private static CopyOnWriteList<canCloseListener>	listeners	= new CopyOnWriteList<canCloseListener>();
	
	public static void
	addListener(
		canCloseListener	l )
	{
		listeners.add( l );
	}
	
	public static void setSkipCloseCheck(boolean b) {
		skipCloseCheck = b;
	}
	
	/**
	 * @return
	 */
	public static boolean canClose(GlobalManager globalManager,
			boolean bForRestart) {
		if (skipCloseCheck) {
			return true;
		}
		
		Shell mainShell = UIFunctionsManagerSWT.getUIFunctionsSWT().getMainShell();
		if (mainShell != null
				&& (!mainShell.isVisible() || mainShell.getMinimized())
				&& COConfigurationManager.getBooleanParameter("Password enabled")) {

			if (!PasswordWindow.showPasswordWindow(Display.getCurrent())) {
				return false;
			}
		}
		
		
		if (COConfigurationManager.getBooleanParameter("confirmationOnExit")) {
			if (!getExitConfirmation(bForRestart)) {
				return false;
			}
		}

		for ( canCloseListener listener: listeners ){
			
			if ( !listener.canClose()){
				
				return( false );
			}
		}
		
		if (globalManager != null) {
			ArrayList listUnfinished = new ArrayList();
			Object[] dms = globalManager.getDownloadManagers().toArray();
			for (int i = 0; i < dms.length; i++) {
				DownloadManager dm = (DownloadManager) dms[i];
				if (dm.getState() == DownloadManager.STATE_SEEDING
						&& dm.getDownloadState().isOurContent()
						&& dm.getStats().getAvailability() < 2) {
					TRTrackerScraperResponse scrape = dm.getTrackerScrapeResponse();
					int numSeeds = scrape.getSeeds();
					long seedingStartedOn = dm.getStats().getTimeStartedSeeding();
					if ((numSeeds > 0) && (seedingStartedOn > 0)
							&& (scrape.getScrapeStartTime() > seedingStartedOn))
						numSeeds--;

					if (numSeeds == 0) {
						listUnfinished.add(dm);
					}
				}
			}

			if (listUnfinished.size() > 0) {
				boolean allowQuit;
				final List flistUnfinished = listUnfinished;
				if (listUnfinished.size() == 1) {
					allowQuit = Utils.execSWTThreadWithBool("quitSeeding",
							new AERunnableBoolean() {
								public boolean runSupport() {
									String title = MessageText.getString("Content.alert.notuploaded.title");
									String text = MessageText.getString(
											"Content.alert.notuploaded.text",
											new String[] {
												((DownloadManager) flistUnfinished.get(0)).getDisplayName(),
												MessageText.getString("Content.alert.notuploaded.quit")
											});

									MessageBoxShell mb = new MessageBoxShell(
											title,
											text,
											new String[] {
												MessageText.getString("UpdateWindow.quit"),
												MessageText.getString("Content.alert.notuploaded.button.abort")
											}, 1);
									mb.setRelatedObject(((DownloadManager) flistUnfinished.get(0)));

									mb.open(null);
									return mb.waitUntilClosed() == 0;
								}
							}, 0);
				} else {
					allowQuit = Utils.execSWTThreadWithBool("quitSeeding",
							new AERunnableBoolean() {
								public boolean runSupport() {
									String sList = "";
									for (int i = 0; i < flistUnfinished.size() && i < 5; i++) {
										DownloadManager dm = ((DownloadManager) flistUnfinished.get(i));
										if (sList != "") {
											sList += "\n";
										}
										sList += dm.getDisplayName();
									}

									String title = MessageText.getString("Content.alert.notuploaded.multi.title");
									String text = MessageText.getString(
											"Content.alert.notuploaded.multi.text",
											new String[] {
												"" + flistUnfinished.size(),
												MessageText.getString("Content.alert.notuploaded.quit"),
												sList
											});

									MessageBoxShell mb = new MessageBoxShell(
											title,
											text,
											new String[] {
												MessageText.getString("UpdateWindow.quit"),
												MessageText.getString("Content.alert.notuploaded.button.abort")
											}, 1);

									mb.open(null);
									return mb.waitUntilClosed() == 0;
								}
							}, 0);
				}
				return allowQuit;
			}
		}

		return true;
	}

	/**
	 * @return true, if the user choosed OK in the exit dialog
	 *
	 * @author Rene Leonhardt
	 */
	private static boolean getExitConfirmation(boolean for_restart) {
		MessageBoxShell mb = new MessageBoxShell(SWT.ICON_WARNING | SWT.YES
				| SWT.NO, for_restart ? "MainWindow.dialog.restartconfirmation"
				: "MainWindow.dialog.exitconfirmation", (String[]) null);
		mb.open(null);

		return mb.waitUntilClosed() == SWT.YES;
	}

	public static void uiShutdown() {
		// problem with closing down web start as AWT threads don't close properly
		if (SystemProperties.isJavaWebStartInstance()) {

			Thread close = new AEThread("JWS Force Terminate") {
				public void runSupport() {
					try {
						Thread.sleep(2500);

					} catch (Throwable e) {

						Debug.printStackTrace(e);
					}

					SESecurityManager.exitVM(1);
				}
			};

			close.setDaemon(true);

			close.start();
		}
	}
	
	public interface
	canCloseListener
	{
		public boolean
		canClose();
	}
}
