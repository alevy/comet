/*
 * File    : IconBar.java
 * Created : 7 d�c. 2003
 * By      : Olivier
 *
 * Azureus - a Java Bittorrent client
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
 */
package org.gudy.azureus2.ui.swt;

import java.util.*;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.*;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.*;

import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.util.Constants;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.ui.swt.components.BufferedToolItem;

import com.aelitis.azureus.ui.swt.imageloader.ImageLoader;

/**
 * @author Olivier
 *
 */
public class IconBar {
	private final boolean OVERRIDE_SHOW_UISWITCHER = System.getProperty(
			"ui.toolbar.uiswitcher", "0").equals("1");
  
  CoolBar coolBar;
  Composite parent;    
  Map itemKeyToControl;
  
  IconBarEnabler currentEnabler;
	private Composite cIconBar;
	
	private static List listeners = new ArrayList(0);

	private Listener listenerToolItem;
  
  public IconBar(Composite parent) {
    this.parent = parent;
    
    listenerToolItem = new Listener() {
			public void handleEvent(Event e) {
				if (e.type == SWT.Selection) {
					if (currentEnabler != null) {
						currentEnabler.itemActivated((String) e.widget.getData("key"));
					}
				} else if (e.type == SWT.Dispose) {
					ImageLoader.getInstance().releaseImage(
							(String) e.widget.getData("ImageID"));
				}
			}
		};
    
    cIconBar = new Composite(parent, SWT.NONE);
    
    GridLayout layout = new GridLayout(2, false);
    layout.marginHeight = 0;
    layout.marginWidth = 0;
    layout.horizontalSpacing = 0;
    layout.verticalSpacing = 0;
    cIconBar.setLayout(layout);
    
    this.itemKeyToControl = new HashMap();    
    	// 3.1 onwards the default is gradient-fill - the disabled icons' transparency no workies
    	// so disabled buttons look bad on the gradient-filled background
    this.coolBar = new CoolBar(cIconBar,Constants.isWindows?SWT.FLAT:SWT.NULL);
    initBar();       
    this.coolBar.setLocked(true);
    
    coolBar.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

    // We could setup a listener on the parameter and dynamically remove/add
    // ui switcher button, but it's not worth the effort
		boolean enableUISwitcher = OVERRIDE_SHOW_UISWITCHER
				|| COConfigurationManager.getBooleanParameter("ui.toolbar.uiswitcher")
				|| COConfigurationManager.getBooleanParameter("ui.asked", false);
    
		if (enableUISwitcher) {
			ToolBar tbSwitch = new ToolBar(cIconBar, SWT.FLAT);
			GridData gridData = new GridData(GridData.HORIZONTAL_ALIGN_END);
			tbSwitch.setLayoutData(gridData);
			ToolItem tiSwitch = new ToolItem(tbSwitch, SWT.PUSH);
			tiSwitch.setImage(ImageLoader.getInstance().getImage("cb_switch"));
			Messages.setLanguageText(tiSwitch, "iconBar.switch.tooltip", true);
			tiSwitch.addSelectionListener(new SelectionListener() {
				public void widgetSelected(SelectionEvent e) {
					UISwitcherUtil.openSwitcherWindow();
				}

				public void widgetDefaultSelected(SelectionEvent e) {
				}
			});
			tiSwitch.addDisposeListener(new DisposeListener() {
				public void widgetDisposed(DisposeEvent e) {
					ImageLoader.getInstance().releaseImage("cb_switch");
				}
			});
		}
	}

	public void setEnabled(String itemKey,boolean enabled) {
    BufferedToolItem BufferedToolItem = (BufferedToolItem) itemKeyToControl.get(itemKey);
    if(BufferedToolItem != null)
      BufferedToolItem.setEnabled(enabled);
  }
  
  public void setSelection(String itemKey,boolean selection) {
    BufferedToolItem BufferedToolItem = (BufferedToolItem) itemKeyToControl.get(itemKey);
    if(BufferedToolItem != null)
      BufferedToolItem.setSelection(selection);
  }
  
  public void setCurrentEnabler(IconBarEnabler enabler) {
    this.currentEnabler = enabler;
    refreshEnableItems();
  }

    // dead
  public IconBarEnabler getCurrentEnabler() {
  	return this.currentEnabler;
  }
  
  private void refreshEnableItems() {
    Iterator iter = itemKeyToControl.keySet().iterator();
    while(iter.hasNext()) {
      String key = (String) iter.next();
      BufferedToolItem BufferedToolItem = (BufferedToolItem) itemKeyToControl.get(key);
      if(BufferedToolItem == null )
        continue;
      if(currentEnabler != null) {
        BufferedToolItem.setEnabled(currentEnabler.isEnabled(key));
        BufferedToolItem.setSelection(currentEnabler.isSelected(key));
      }
      else {        
        BufferedToolItem.setEnabled(false);
        BufferedToolItem.setSelection(false);
      }
    }
  }
  
  private BufferedToolItem createBufferedToolItem(ToolBar toolBar,int style,String key,final String imageName,String toolTipKey) {    
    final BufferedToolItem bufferedToolItem = new BufferedToolItem(toolBar,style);
    bufferedToolItem.setData("key",key);
    bufferedToolItem.setData("ImageID",key);
    Messages.setLanguageText(bufferedToolItem.getWidget(),toolTipKey,true);
    bufferedToolItem.setImage(ImageLoader.getInstance().getImage(imageName));
   
    bufferedToolItem.addListener(SWT.Selection, listenerToolItem);
    bufferedToolItem.addListener(SWT.Dispose, listenerToolItem);

    itemKeyToControl.put(key,bufferedToolItem);
    return bufferedToolItem;
  }  
  
  public void addItemKeyToControl(String key, BufferedToolItem item) {
  	itemKeyToControl.put(key, item);
  }
  
  private void initBar() {
    //The File Menu
    CoolItem coolItem = new CoolItem(coolBar,SWT.NULL);

    ToolBar toolBar = new ToolBar(coolBar,SWT.FLAT);
    createBufferedToolItem(toolBar,SWT.PUSH,"open","cb_open_no_default","iconBar.open.tooltip");
    // XXX TuxPaper: Remove images (open, open_url, open_folder) from CVS and ImageRepository
    createBufferedToolItem(toolBar,SWT.PUSH,"new","cb_new","iconBar.new.tooltip");
    toolBar.pack(); 
    Point p = toolBar.getSize();
    coolItem.setControl(toolBar);
    coolItem.setSize(coolItem.computeSize (p.x,p.y));
    coolItem.setMinimumSize(p.x,p.y);
    
    
    coolItem = new CoolItem(coolBar,SWT.NULL);
    toolBar = new ToolBar(coolBar,SWT.FLAT);    
    createBufferedToolItem(toolBar,SWT.PUSH,"top","cb_top","iconBar.top.tooltip");
    createBufferedToolItem(toolBar,SWT.PUSH,"up","cb_up","iconBar.up.tooltip");
    createBufferedToolItem(toolBar,SWT.PUSH,"down","cb_down","iconBar.down.tooltip");
    createBufferedToolItem(toolBar,SWT.PUSH,"bottom","cb_bottom","iconBar.bottom.tooltip");
    new BufferedToolItem(toolBar,SWT.SEPARATOR);
    createBufferedToolItem(toolBar,SWT.PUSH,"run","cb_run","iconBar.run.tooltip");
    new BufferedToolItem(toolBar,SWT.SEPARATOR);
    createBufferedToolItem(toolBar,SWT.PUSH,"start","cb_start","iconBar.start.tooltip");
    createBufferedToolItem(toolBar,SWT.PUSH,"stop","cb_stop","iconBar.stop.tooltip");
    createBufferedToolItem(toolBar,SWT.PUSH,"remove","cb_remove","iconBar.remove.tooltip");
    new BufferedToolItem(toolBar,SWT.SEPARATOR);
    createBufferedToolItem(toolBar,SWT.PUSH,"editcolumns","cb_editcolumns","iconBar.editcolumns.tooltip");

    for (Iterator iter = listeners.iterator(); iter.hasNext();) {
    	try {
    		IconBarListener l = (IconBarListener) iter.next();
    		l.iconBarInitialized(coolBar, this);
    	} catch (Exception e) {
    		Debug.out(e);
    	}
		}
    
    toolBar.pack();
    p = toolBar.getSize();
    coolItem.setControl(toolBar);
    if (Constants.isOSX) {
    	p.x += 12;
    }
    coolItem.setSize(p.x,p.y);
  	coolItem.setMinimumSize(p.x,p.y);
  }
  
  public void setLayoutData(Object layoutData) {
  	cIconBar.setLayoutData(layoutData);
  }
  
  public static void main(String args[]) {
    Display display = new Display();
    Shell shell = new Shell(display);
    FormLayout layout = new FormLayout();
    layout.marginHeight = 0;
    layout.marginWidth = 0;
    shell.setLayout(layout);
    IconBar ibar = new IconBar(shell);
    FormData formData = new FormData();
    formData.left = new FormAttachment(0,0);
    formData.right = new FormAttachment(100,0);
    ibar.setLayoutData(formData);
    shell.open();
    while (!shell.isDisposed()) {
      if (!display.readAndDispatch ()) display.sleep ();
    }
    display.dispose ();        
  }
  
	public Composite getComposite() {
		return cIconBar;
	}

	/**
	 * 
	 */
	public void delete() {
		Utils.disposeComposite(cIconBar);
		itemKeyToControl.clear();
		currentEnabler = null;
	}

	public static void addListener(IconBarListener l) {
		if (!listeners.contains(l)) {
			listeners.add(l);
		}
	}
	
	public static interface IconBarListener {
		public void iconBarInitialized(CoolBar coolBar, IconBar ib);
	}
}
