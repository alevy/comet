/*
 * File    : ConfigPanel*.java
 * Created : 11 mar. 2004
 * By      : TuxPaper
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

package org.gudy.azureus2.ui.swt.views.configsections;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.plugins.ui.config.ConfigSection;
import org.gudy.azureus2.ui.swt.Messages;
import org.gudy.azureus2.ui.swt.UISwitcherUtil;
import org.gudy.azureus2.ui.swt.config.BooleanParameter;
import org.gudy.azureus2.ui.swt.config.ChangeSelectionActionPerformer;
import org.gudy.azureus2.ui.swt.plugins.UISWTConfigSection;
import org.gudy.azureus2.ui.swt.shells.MessageBoxShell;

import com.aelitis.azureus.ui.UIFunctions;
import com.aelitis.azureus.ui.UIFunctionsManager;

public class ConfigSectionInterfaceStart implements UISWTConfigSection {
  public String configSectionGetParentSection() {
    return ConfigSection.SECTION_INTERFACE;
  }

	public String configSectionGetName() {
		return "start";
	}

  public void configSectionSave() {
  }

  public void configSectionDelete() {
  }
  
	public int maxUserMode() {
		return 0;
	}


  public Composite configSectionCreate(final Composite parent) {
    // "Start" Sub-Section
    // -------------------
    GridLayout layout;
    Composite cStart = new Composite(parent, SWT.NULL);

    cStart.setLayoutData(new GridData(GridData.FILL_BOTH));
    layout = new GridLayout();
    layout.numColumns = 1;
    cStart.setLayout(layout);

    new BooleanParameter(cStart, "Show Splash", "ConfigView.label.showsplash");
    new BooleanParameter(cStart, "update.start", "ConfigView.label.checkonstart");
    new BooleanParameter(cStart, "update.periodic", "ConfigView.label.periodiccheck");
    BooleanParameter autoDownload = new BooleanParameter(cStart, "update.autodownload", "ConfigView.section.update.autodownload");
    BooleanParameter openDialog = new BooleanParameter(cStart, "update.opendialog", "ConfigView.label.opendialog");
    
		autoDownload.setAdditionalActionPerformer(new ChangeSelectionActionPerformer(
				new Control[] { openDialog.getControl() }, true ));
    
    new Label(cStart,SWT.NULL);
    new BooleanParameter(cStart, "Open MyTorrents", "ConfigView.label.openmytorrents");
    new BooleanParameter(cStart, "Open Console", "ConfigView.label.openconsole");
    new BooleanParameter(cStart, "Open Stats On Start", "ConfigView.label.openstatsonstart");
    new BooleanParameter(cStart, "Open Config", "ConfigView.label.openconfig");
    new BooleanParameter(cStart, "Open Transfer Bar On Start", "ConfigView.label.open_transfer_bar_on_start");
    new BooleanParameter(cStart, "Start Minimized", "ConfigView.label.startminimized");
    
    if (COConfigurationManager.getStringParameter("ui").equals("az3")) {
			new BooleanParameter(cStart, "v3.Start Advanced",
					"ConfigView.interface.start.library");
		}
  
	// UI switcher window.
    Composite cUISwitcher = new Composite(cStart, SWT.NONE);
    layout = new GridLayout(2, false);
    layout.marginHeight = 0;
    layout.marginWidth = 0;
		cUISwitcher.setLayout(layout);

		final Label ui_switcher_label = new Label(cUISwitcher, SWT.NULL);
		Messages.setLanguageText(ui_switcher_label, "ConfigView.label.ui_switcher");

		final Button ui_switcher_button = new Button(cUISwitcher, SWT.PUSH);
		Messages.setLanguageText(ui_switcher_button,
				"ConfigView.label.ui_switcher_button");

		ui_switcher_button.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event event) {
				String uiOld = COConfigurationManager.getStringParameter("ui");
				String uiNew = UISwitcherUtil.openSwitcherWindow(true);
				if (!uiOld.equals(uiNew)) {
  				int result = MessageBoxShell.open(parent.getShell(),
  						MessageText.getString("dialog.uiswitcher.restart.title"),
  						MessageText.getString("dialog.uiswitcher.restart.text"),
  						new String[] {
  							MessageText.getString("UpdateWindow.restart"),
  							MessageText.getString("UpdateWindow.restartLater"),
  						}, 0);
  				if (result == 0) {
  					UIFunctions uif = UIFunctionsManager.getUIFunctions();
  					if (uif != null) {
  						uif.dispose(true, false);
  					}
  				}
				}
			}
		});
    
    return cStart;
  }
}
