/*
 * Created on Aug 4, 2006 9:03:19 AM
 * Copyright (C) 2006 Aelitis, All Rights Reserved.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 *
 * AELITIS, SAS au capital de 46,603.30 euros
 * 8 Allee Lenotre, La Grille Royale, 78600 Le Mesnil le Roi, France.
 */
package com.aelitis.azureus.ui.swt.skin;

import java.text.NumberFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Locale;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.*;
import org.eclipse.swt.graphics.*;
import org.eclipse.swt.widgets.*;

import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.util.AERunnable;
import org.gudy.azureus2.core3.util.Constants;
import org.gudy.azureus2.core3.util.UrlUtils;
import org.gudy.azureus2.ui.swt.Messages;
import org.gudy.azureus2.ui.swt.Utils;
import org.gudy.azureus2.ui.swt.mainwindow.ClipboardCopy;
import org.gudy.azureus2.ui.swt.shells.GCStringPrinter;
import org.gudy.azureus2.ui.swt.shells.MessageSlideShell;
import org.gudy.azureus2.ui.swt.shells.GCStringPrinter.URLInfo;

import com.aelitis.azureus.core.cnetwork.ContentNetwork;
import com.aelitis.azureus.core.cnetwork.ContentNetworkManagerFactory;
import com.aelitis.azureus.ui.swt.utils.ColorCache;

/**
 * Text Skin Object.  This one paints text on parent.
 * 
 * @author TuxPaper
 * @created Aug 4, 2006
 *
 */
public class SWTSkinObjectText2
	extends SWTSkinObjectBasic
	implements SWTSkinObjectText, PaintListener
{
	String sText;

	String sDisplayText;

	String sKey;

	boolean bIsTextDefault = false;

	private int style;

	private Canvas canvas;

	private boolean isUnderline;

	private int antialiasMode = SWT.DEFAULT;

	private boolean isAllcaps;

	private boolean hasShadow;

	private int hpadding;

	private int vpadding;

	private boolean relayoutOnTextChange = true;

	private boolean isItalic = false;

	private static Font font = null;

	private GCStringPrinter lastStringPrinter;

	private Color colorUrl;

	private int alpha = 255;
	
	private java.util.List<SWTSkinObjectText_UrlClickedListener> listUrlClickedListeners = new ArrayList<SWTSkinObjectText_UrlClickedListener>();

	private Color colorUrl2;

	protected boolean mouseDown;

	public SWTSkinObjectText2(SWTSkin skin,
			final SWTSkinProperties skinProperties, String sID,
			final String sConfigID, String[] typeParams, SWTSkinObject parent) {
		super(skin, skinProperties, sID, sConfigID, "text", parent);

		style = SWT.WRAP;

		String sAlign = skinProperties.getStringValue(sConfigID + ".align");
		if (sAlign != null) {
			int align = SWTSkinUtils.getAlignment(sAlign, SWT.NONE);
			if (align != SWT.NONE) {
				style |= align;
			}
		}

		String sVAlign = skinProperties.getStringValue(sConfigID + ".v-align");
		if (sVAlign != null) {
			int align = SWTSkinUtils.getAlignment(sVAlign, SWT.NONE);
			if (align != SWT.CENTER) {
				if (align != SWT.NONE) {
					style |= align;
				} else {
					style |= SWT.TOP;
				}
			}
		} else {
			style |= SWT.TOP;
		}

		if (skinProperties.getIntValue(sConfigID + ".border", 0) == 1) {
			style |= SWT.BORDER;
		}

		String sAntiAlias = skinProperties.getStringValue(sConfigID + ".antialias",
				(String) null);
		if (sAntiAlias != null && sAntiAlias.length() > 0) {
			antialiasMode = (sAntiAlias.equals("1") || sAntiAlias.toLowerCase().equals(
					"true")) ? SWT.ON : SWT.OFF;
		}

		relayoutOnTextChange = skinProperties.getBooleanValue(sConfigID
				+ ".text.relayoutOnChange", true);

		Composite createOn;
		if (parent == null) {
			createOn = skin.getShell();
		} else {
			createOn = (Composite) parent.getControl();
		}

		canvas = new Canvas(createOn, SWT.DOUBLE_BUFFERED) {
			Point ptMax = new Point(0, 0);

			// @see org.eclipse.swt.widgets.Composite#computeSize(int, int, boolean)
			public Point computeSize(int wHint, int hHint, boolean changed) {
				int border = getBorderWidth() * 2;
				Point pt = new Point(border, border);

				if (sDisplayText == null) {
					return pt;
				}

				Font existingFont = (Font) canvas.getData("font");
				Color existingColor = (Color) canvas.getData("color");

				GC gc = new GC(this);
				if (existingFont != null) {
					gc.setFont(existingFont);
				}
				if (existingColor != null) {
					gc.setForeground(existingColor);
				}
				if (antialiasMode != SWT.DEFAULT) {
					try {
						gc.setTextAntialias(antialiasMode);
					} catch (Exception e) {
						// Ignore ERROR_NO_GRAPHICS_LIBRARY error or any others
					}
				}

				gc.setAlpha(alpha);

				GCStringPrinter sp = new GCStringPrinter(gc, sDisplayText,
						new Rectangle(0, 0, wHint == -1 ? 999999 : wHint, hHint == -1
								? 999999 : hHint), true, false, style & SWT.WRAP);
				sp.calculateMetrics();
				pt = sp.getCalculatedSize();
				pt.x += border + hpadding * 2;
				pt.y += border + vpadding * 2;
				gc.dispose();

				if (isUnderline) {
					pt.y++;
				}
				if (hasShadow) {
					pt.x++;
				}
				if (isItalic) {
					pt.x += 4;
				}

				int fixedWidth = skinProperties.getIntValue(sConfigID + ".width", -1);
				if (fixedWidth >= 0) {
					pt.x = fixedWidth;
				}
				int fixedHeight = skinProperties.getIntValue(sConfigID + ".height", -1);
				if (fixedHeight >= 0) {
					pt.y = fixedHeight;
				}

				if (isVisible()) {
					if (pt.x > ptMax.x) {
						ptMax.x = pt.x;
					}
					if (pt.y > ptMax.y) {
						ptMax.y = pt.y;
					}
				}

				return pt;
			}
		};

		if (false && font == null) {
			Display display = createOn.getDisplay();
			FontData fd = new FontData("Arial", Utils.pixelsToPoint(10,
					display.getDPI().y), SWT.NORMAL);
			font = new Font(display, fd);
		}

		canvas.setData("font", font);
		setControl(canvas);
		if (typeParams.length > 1) {
			bIsTextDefault = true;
			sText = typeParams[1];

			for (int i = 2; i < typeParams.length; i++) {
				sText += ", " + typeParams[i];
			}
			this.sDisplayText = isAllcaps && sText != null ? sText.toUpperCase()
					: sText;
		}

		canvas.addMouseListener(new MouseListener() {
			private String lastDownURL;

			public void mouseUp(MouseEvent e) {
				mouseDown = false;
				if (lastStringPrinter != null) {
					URLInfo hitUrl = lastStringPrinter.getHitUrl(e.x, e.y);
					if (hitUrl != null) {
						
						SWTSkinObjectText_UrlClickedListener[] listeners = listUrlClickedListeners.toArray(new SWTSkinObjectText_UrlClickedListener[0]);
						for (SWTSkinObjectText_UrlClickedListener l : listeners) {
							if (l.urlClicked(hitUrl)) {
								return;
							}
						}

						String url = hitUrl.url;
						try {
  						ContentNetwork cn = ContentNetworkManagerFactory.getSingleton().getContentNetworkForURL(url);
  						if (cn != null) {
  							url = cn.appendURLSuffix(url, false, false);
  						}
  						if (url.contains("?")) {
  							url += "&";
  						} else {
  							url += "?";
  						}
  						url += "fromWeb=false&os.name=" + UrlUtils.encode(Constants.OSName)
  								+ "&os.version="
  								+ UrlUtils.encode(System.getProperty("os.version"))
  								+ "&java.version=" + UrlUtils.encode(Constants.JAVA_VERSION); 
						} catch (Throwable t) {
							// ignore
						}
						Utils.launch(url);
					}
				}
			}

			public void mouseDown(MouseEvent e) {
				mouseDown = true;
				if (lastStringPrinter != null) {
					URLInfo hitUrl = lastStringPrinter.getHitUrl(e.x, e.y);
					String curURL = hitUrl == null ? "" : hitUrl.url;
					
					if (curURL.equals(lastDownURL)) {
						lastDownURL = curURL;
						canvas.redraw();
					}
				}
			}

			public void mouseDoubleClick(MouseEvent e) {
			}
		});

		canvas.addMouseMoveListener(new MouseMoveListener() {
			public void mouseMove(MouseEvent e) {
				if (lastStringPrinter != null && lastStringPrinter.hasHitUrl()) {
					URLInfo hitUrl = lastStringPrinter.getHitUrl(e.x, e.y);
					canvas.setCursor(hitUrl == null ? null
							: canvas.getDisplay().getSystemCursor(SWT.CURSOR_HAND));
				}
			}
		});
		
		if (skinProperties.getBooleanValue(sConfigID + ".clipboardmenu", false)) {
			Menu menu = new Menu(canvas);
			MenuItem menuItem = new MenuItem(menu, SWT.PUSH);
			Messages.setLanguageText(menuItem, "MyTorrentsView.menu.thisColumn.toClipboard");
			menuItem.addSelectionListener(new SelectionListener() {
				public void widgetSelected(SelectionEvent e) {
					ClipboardCopy.copyToClipBoard(getText());
				}
				
				public void widgetDefaultSelected(SelectionEvent e) {
				}
			});
			canvas.setMenu(menu);
		}

		setAlwaysHookPaintListener(true);

		updateFont("");
	}

	public String switchSuffix(String suffix, int level, boolean walkUp,
			boolean walkDown) {
		suffix = super.switchSuffix(suffix, level, walkUp, walkDown);
		if (suffix == null) {
			return null;
		}

		String sPrefix = sConfigID + ".text";

		if (sText == null || bIsTextDefault) {
			String text = properties.getStringValue(sPrefix + suffix);
			if (text != null) {
				sText = text;
				this.sDisplayText = isAllcaps && sText != null ? sText.toUpperCase()
						: sText;
			}
		}

		final String fSuffix = suffix;
		Utils.execSWTThread(new AERunnable() {
			public void runSupport() {
				if (canvas == null || canvas.isDisposed()) {
					return;
				}
				updateFont(fSuffix);
			}
		});
		return suffix;
	}

	private void updateFont(String suffix) {
		String sPrefix = sConfigID + ".text";

		Color newColorURL = properties.getColor(sPrefix + ".urlcolor" + suffix);
		if (newColorURL != null) {
			colorUrl = newColorURL;
		}

		Color newColorURL2 = properties.getColor(sPrefix + ".urlcolor-pressed");
		if (newColorURL2 != null) {
			colorUrl2 = newColorURL2;
		}


		Color color = properties.getColor(sPrefix + ".color" + suffix);
		//System.out.println(this + "; " + sPrefix + ";" + suffix + "; " + color + "; " + text);
		if (color != null) {
			canvas.setData("color", color);
		}
		
		alpha  = properties.getIntValue(sConfigID + ".alpha", 255);

		hpadding = properties.getIntValue(sPrefix + ".h-padding", 0);
		vpadding = properties.getIntValue(sPrefix + ".v-padding", 0);

		Font existingFont = (Font) canvas.getData("Font" + suffix);
		if (existingFont != null && !existingFont.isDisposed()) {
			canvas.setData("font", existingFont);
		} else {
			boolean bNewFont = false;
			int iFontSize = -1;
			int iFontWeight = -1;
			String sFontFace = null;
			FontData[] tempFontData = canvas.getFont().getFontData();

			sFontFace = properties.getStringValue(sPrefix + ".font" + suffix);
			if (sFontFace != null) {
				tempFontData[0].setName(sFontFace);
				bNewFont = true;
			}

			String sStyle = properties.getStringValue(sPrefix + ".style" + suffix);
			if (sStyle != null) {
				isAllcaps = false;
				String[] sStyles = sStyle.toLowerCase().split(",");
				for (int i = 0; i < sStyles.length; i++) {
					String s = sStyles[i];

					if (s.equals("allcaps")) {
						isAllcaps = true;
					}

					if (s.equals("bold")) {
						if (iFontWeight == -1) {
							iFontWeight = SWT.BOLD;
						} else {
							iFontWeight |= SWT.BOLD;
						}
						bNewFont = true;
					}

					if (s.equals("italic")) {
						if (iFontWeight == -1) {
							iFontWeight = SWT.ITALIC;
						} else {
							iFontWeight |= SWT.ITALIC;
						}
						bNewFont = true;
						isItalic = true;
					} else {
						isItalic = false;
					}

					isUnderline = s.equals("underline");
					if (isUnderline) {
						canvas.addPaintListener(new PaintListener() {
							public void paintControl(PaintEvent e) {
								int x = 0;
								Point pt = e.gc.textExtent(sDisplayText);
								Point size = ((Control) e.widget).getSize();
								if (pt.x < size.x) {
									x = size.x - pt.x;
									size.x = pt.x;
								}
								e.gc.drawLine(x, size.y - 1, size.x - 1 + x, size.y - 1);
							}
						});
					}

					if (s.equals("strike")) {
						canvas.addPaintListener(new PaintListener() {
							public void paintControl(PaintEvent e) {
								Point size = ((Control) e.widget).getSize();
								int y = size.y / 2;
								e.gc.drawLine(0, y, size.x - 1, y);
							}
						});
					}

					if (s.equals("normal")) {
						bNewFont = true;
					}

					if (s.equals("shadow")) {
						hasShadow = true;
					}
				}
				this.sDisplayText = isAllcaps && sText != null ? sText.toUpperCase()
						: sText;
			}

			if (iFontWeight >= 0) {
				tempFontData[0].setStyle(iFontWeight);
			}

			String sSize = properties.getStringValue(sPrefix + ".size" + suffix);
			if (sSize != null) {
				FontData[] fd = canvas.getFont().getFontData();

				try {
					char firstChar = sSize.charAt(0);
					if (firstChar == '+' || firstChar == '-') {
						sSize = sSize.substring(1);
					}

					double dSize = NumberFormat.getInstance(Locale.US).parse(sSize).doubleValue();

					if (firstChar == '+') {
						iFontSize = (int) (fd[0].height + dSize);
					} else if (firstChar == '-') {
						iFontSize = (int) (fd[0].height - dSize);
					} else {
						if (sSize.endsWith("px")) {
							//iFontSize = Utils.getFontHeightFromPX(canvas.getFont(), null, (int) dSize);
							iFontSize = Utils.getFontHeightFromPX(canvas.getDisplay(),
									tempFontData, null, (int) dSize);
							//iFontSize = Utils.pixelsToPoint(dSize, canvas.getDisplay().getDPI().y);
						} else {
							iFontSize = (int) dSize;
						}
					}

					bNewFont = true;
				} catch (NumberFormatException e) {
					e.printStackTrace();
				} catch (ParseException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}

			if (bNewFont) {
				FontData[] fd = canvas.getFont().getFontData();

				if (iFontSize > 0) {
					fd[0].setHeight(iFontSize);
				}

				if (iFontWeight >= 0) {
					fd[0].setStyle(iFontWeight);
				}

				if (sFontFace != null) {
					fd[0].setName(sFontFace);
				}

				final Font canvasFont = new Font(canvas.getDisplay(), fd);
				canvas.setData("font", canvasFont);
				canvas.addDisposeListener(new DisposeListener() {
					public void widgetDisposed(DisposeEvent e) {
						canvasFont.dispose();
					}
				});

				canvas.setData("Font" + suffix, canvasFont);
			}
		}

		canvas.redraw();
	}

	/**
	 * @param searchText
	 */
	public void setText(String text) {
		if (text == null) {
			text = "";
		}

		if (text.equals(sText)) {
			return;
		}

		this.sText = text;
		this.sDisplayText = isAllcaps && sText != null ? sText.toUpperCase()
				: sText;
		this.sKey = null;
		bIsTextDefault = false;

		lastStringPrinter = null;
		// Doing execSWTThreadLater delays the relayout for too long at skin startup
		// Since there are a lot of async execs at skin startup, we generally
		// see the window a second or two before this async call would get called
		// (if it were async)
		Utils.execSWTThread(new AERunnable() {
			public void runSupport() {
				if (canvas != null && !canvas.isDisposed()) {
					canvas.setCursor(null);
					canvas.redraw();
					if (relayoutOnTextChange) {
						Utils.relayout(canvas);
					}
				}
			}
		});
	}

	// @see com.aelitis.azureus.ui.swt.skin.SWTSkinObjectBasic#paintControl(org.eclipse.swt.graphics.GC)
	public void paintControl(GC gc) {
		if (sText == null || sText.length() == 0) {
			return;
		}

		super.paintControl(gc);

		Composite composite = (Composite) control;
		Rectangle clientArea = composite.getClientArea();
		clientArea.x += hpadding;
		clientArea.width -= hpadding * 2;
		clientArea.y += vpadding;
		clientArea.height -= vpadding * 2;

		Font existingFont = (Font) canvas.getData("font");
		Color existingColor = (Color) canvas.getData("color");

		if (existingFont != null) {
			gc.setFont(existingFont);
		}

		if (existingColor != null) {
			gc.setForeground(existingColor);
		}

		if (antialiasMode != SWT.DEFAULT) {
			try {
				gc.setTextAntialias(antialiasMode);
			} catch (Exception ex) {
				// Ignore ERROR_NO_GRAPHICS_LIBRARY error or any others
			}
		}

		if (hasShadow) {
			Rectangle r = new Rectangle(clientArea.x + 1, clientArea.y + 1,
					clientArea.width, clientArea.height);

			Color foreground = gc.getForeground();
			Color color = ColorCache.getColor(gc.getDevice(), 0, 0, 0);
			gc.setForeground(color);
			gc.setAlpha(64);
			GCStringPrinter.printString(gc, sDisplayText, r, true, false, style);
			gc.setForeground(foreground);
		}

		gc.setAlpha(alpha);
		lastStringPrinter = new GCStringPrinter(gc, sDisplayText, clientArea, true,
				false, style);
		if (colorUrl != null) {
			lastStringPrinter.setUrlColor(colorUrl);
		}
		if (colorUrl2 != null && mouseDown) {
			lastStringPrinter.calculateMetrics();
			Display display = Display.getCurrent();
			Point cursorLocation = canvas.toControl(display.getCursorLocation());
			URLInfo hitUrl = lastStringPrinter.getHitUrl(cursorLocation.x, cursorLocation.y);
			if (hitUrl != null) {
				hitUrl.urlColor = colorUrl2;
			}
		}
		
		lastStringPrinter.printString();
	}

	public void setTextID(String key) {
		if (key == null) {
			setText("");
		}

		else if (key.equals(sKey)) {
			return;
		}

		this.sText = MessageText.getString(key);
		this.sDisplayText = isAllcaps && sText != null ? sText.toUpperCase()
				: sText;
		this.sKey = key;
		bIsTextDefault = false;

		Utils.execSWTThreadLater(0, new AERunnable() {
			public void runSupport() {
				if (canvas == null || canvas.isDisposed()) {
					return;
				}
				canvas.redraw();
				if (relayoutOnTextChange) {
					canvas.layout(true);
					Utils.relayout(canvas);
				}
			}
		});
	}

	public void setTextID(String key, String[] params) {
		if (key == null) {
			setText("");
		}

		/*
		 * KN: disabling the caching of the key since this is parameterized it may be called
		 * multiple times with different parameters
		 */
		//		else if (key.equals(sKey)) {
		//			return;
		//		}
		this.sText = MessageText.getString(key, params);
		this.sDisplayText = isAllcaps && sText != null ? sText.toUpperCase()
				: sText;
		this.sKey = key;
		bIsTextDefault = false;

		Utils.execSWTThreadLater(0, new AERunnable() {
			public void runSupport() {
				if (canvas == null || canvas.isDisposed()) {
					return;
				}
				canvas.redraw();
				if (relayoutOnTextChange) {
					canvas.layout(true);
					Utils.relayout(canvas);
				}
			}
		});
	}

	// @see com.aelitis.azureus.ui.swt.skin.SWTSkinObjectBasic#triggerListeners(int, java.lang.Object)
	public void triggerListeners(int eventType, Object params) {
		if (eventType == SWTSkinObjectListener.EVENT_LANGUAGE_CHANGE) {
			if (sKey != null) {
				setTextID(sKey);
			}
		}
		super.triggerListeners(eventType, params);
	}

	public int getStyle() {
		return style;
	}

	public void setStyle(int style) {
		this.style = style;
	}

	// @see com.aelitis.azureus.ui.swt.skin.SWTSkinObjectText#getText()
	public String getText() {
		return sDisplayText;
	}
	
	public void addUrlClickedListener(SWTSkinObjectText_UrlClickedListener l) {
		listUrlClickedListeners.add(l);
	}
	
	public void removeUrlClickedListener(SWTSkinObjectText_UrlClickedListener l) {
		listUrlClickedListeners.remove(l);
	}

	// @see com.aelitis.azureus.ui.swt.skin.SWTSkinObjectText#setTextColor(org.eclipse.swt.graphics.Color)
	public void setTextColor(final Color color) {
		Utils.execSWTThread(new AERunnable() {
			public void runSupport() {
				if (canvas == null || canvas.isDisposed()) {
					return;
				}
				canvas.setData("color", color);
				canvas.redraw();
			}
		});
	}
}
