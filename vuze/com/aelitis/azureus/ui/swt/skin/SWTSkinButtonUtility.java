package com.aelitis.azureus.ui.swt.skin;

import java.util.ArrayList;
import java.util.Iterator;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;

import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.util.*;
import org.gudy.azureus2.ui.swt.Utils;

/**
 * Simple encapsulation of SWTSkinObjectContainer that provides typical button 
 * funtionality
 *
 */
public class SWTSkinButtonUtility
{
	ArrayList listeners = new ArrayList();

	private final SWTSkinObject skinObject;

	private final String imageViewID;

	public static class ButtonListenerAdapter
	{
		public void pressed(SWTSkinButtonUtility buttonUtility,
				SWTSkinObject skinObject, int stateMask) {
		}

		/**
		 * 
		 * @param buttonUtility
		 *
		 * @deprecated
		 */
		public void pressed(SWTSkinButtonUtility buttonUtility) {
		}
		
		public boolean held(SWTSkinButtonUtility buttonUtility) {
			return false;
		}

		public void disabledStateChanged(SWTSkinButtonUtility buttonUtility,
				boolean disabled) {
		}
	}

	public SWTSkinButtonUtility(SWTSkinObject skinObject) {
		this(skinObject, null);
	}

	public SWTSkinButtonUtility(SWTSkinObject skinObject, String imageViewID) {
		this.skinObject = skinObject;
		this.imageViewID = imageViewID;
		
		if (skinObject instanceof SWTSkinObjectButton) {
			return;
		}
		
		Listener l = new Listener() {
			boolean bDownPressed;
			private TimerEvent timerEvent;

			public void handleEvent(Event event) {
				if (event.type == SWT.MouseDown) {
					if (timerEvent == null) {
						timerEvent = SimpleTimer.addEvent("MouseHold",
								SystemTime.getOffsetTime(1000), new TimerEventPerformer() {
									public void perform(TimerEvent event) {
										timerEvent = null;

										if (!bDownPressed) {
											return;
										}
										bDownPressed = false;

										boolean stillPressed = true;
										for (Iterator iter = listeners.iterator(); iter.hasNext();) {
											ButtonListenerAdapter l = (ButtonListenerAdapter) iter.next();
											stillPressed &= !l.held(SWTSkinButtonUtility.this);
										}
										bDownPressed = stillPressed;
									}
								});
					}
					bDownPressed = true;
					return;
				} else {
					if (timerEvent != null) {
						timerEvent.cancel();
						timerEvent = null;
					}
					if (!bDownPressed) {
						return;
					}
				}

				bDownPressed = false;

				if (isDisabled()) {
					return;
				}

				for (Iterator iter = listeners.iterator(); iter.hasNext();) {
					ButtonListenerAdapter l = (ButtonListenerAdapter) iter.next();
					l.pressed(SWTSkinButtonUtility.this);
					l.pressed(SWTSkinButtonUtility.this,
							SWTSkinButtonUtility.this.skinObject, event.stateMask);
				}
			}
		};
		if (skinObject instanceof SWTSkinObjectContainer) {
			Utils.addListenerAndChildren((Composite) skinObject.getControl(),
					SWT.MouseUp, l);
			Utils.addListenerAndChildren((Composite) skinObject.getControl(),
					SWT.MouseDown, l);
		} else {
			skinObject.getControl().addListener(SWT.MouseUp, l);
			skinObject.getControl().addListener(SWT.MouseDown, l);
		}
	}

	public boolean isDisabled() {
		return skinObject.getSuffix().indexOf("-disabled") >= 0;
	}

	private boolean inSetDisabled = false;
	public void setDisabled(boolean disabled) {
		if (inSetDisabled) {
			return;
		}
		inSetDisabled = true;
		try {
  		if (disabled == isDisabled()) {
  			return;
  		}
  		String suffix = disabled ? "-disabled" : "";
  		skinObject.switchSuffix(suffix, 1, false);
  
  		for (Iterator iter = listeners.iterator(); iter.hasNext();) {
  			ButtonListenerAdapter l = (ButtonListenerAdapter) iter.next();
  			l.disabledStateChanged(SWTSkinButtonUtility.this, disabled);
  		}
		} finally {
			inSetDisabled = false;
		}
	}

	public void addSelectionListener(ButtonListenerAdapter listener) {
		if (skinObject instanceof SWTSkinObjectButton) {
			((SWTSkinObjectButton)skinObject).addSelectionListener(listener);
			return;
		}

		if (listeners.contains(listener)) {
			return;
		}
		listeners.add(listener);
	}

	public SWTSkinObject getSkinObject() {
		return skinObject;
	}

	public void setTextID(final String id) {
		if (skinObject instanceof SWTSkinObjectButton) {
			((SWTSkinObjectButton)skinObject).setText(MessageText.getString(id));
			return;
		}
		Utils.execSWTThreadLater(0, new AERunnable() {
			public void runSupport() {
				if (skinObject instanceof SWTSkinObjectText) {
					SWTSkinObjectText skinTextObject = (SWTSkinObjectText) skinObject;
					skinTextObject.setTextID(id);
				} else if (skinObject instanceof SWTSkinObjectContainer) {
					SWTSkinObject[] children = ((SWTSkinObjectContainer) skinObject).getChildren();
					if (children.length > 0 && children[0] instanceof SWTSkinObjectText) {
						SWTSkinObjectText skinTextObject = (SWTSkinObjectText) children[0];
						skinTextObject.setTextID(id);
					}
				}
				Utils.relayout(skinObject.getControl());
			}
		});
	}

	public void setImage(final String id) {
		if (skinObject instanceof SWTSkinObjectButton) {
			// TODO implement
			return;
		}
		Utils.execSWTThread(new AERunnable() {
			public void runSupport() {
				if (imageViewID != null) {
					SWTSkinObject skinImageObject = skinObject.getSkin().getSkinObject(imageViewID, skinObject);
					if (skinImageObject instanceof SWTSkinObjectImage) {
						((SWTSkinObjectImage) skinImageObject).setImageByID(id, null);
						return;
					}
				}
				if (skinObject instanceof SWTSkinObjectImage) {
					SWTSkinObjectImage skinImageObject = (SWTSkinObjectImage) skinObject;
					skinImageObject.setImageByID(id, null);
				} else if (skinObject instanceof SWTSkinObjectContainer) {
					SWTSkinObject[] children = ((SWTSkinObjectContainer) skinObject).getChildren();
					if (children.length > 0 && children[0] instanceof SWTSkinObjectImage) {
						SWTSkinObjectImage skinImageObject = (SWTSkinObjectImage) children[0];
						skinImageObject.setImageByID(id, null);
					}
				}
			}
		});
	}

	public void setTooltipID(final String id) {
		if (skinObject instanceof SWTSkinObjectButton) {
			// TODO implement
			return;
		}
		if (skinObject instanceof SWTSkinObjectImage) {
			SWTSkinObjectImage skinImageObject = (SWTSkinObjectImage) skinObject;
			skinImageObject.setTooltipID(id);
		} else if (skinObject instanceof SWTSkinObjectContainer) {
			SWTSkinObject[] children = ((SWTSkinObjectContainer) skinObject).getChildren();
			if (children.length > 0 && children[0] instanceof SWTSkinObjectImage) {
				SWTSkinObjectImage skinImageObject = (SWTSkinObjectImage) children[0];
				skinImageObject.setTooltipID(id);
			}
		}
	}
}
