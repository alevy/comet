package org.gudy.azureus2.ui.swt.components.shell;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.*;
import org.eclipse.swt.graphics.*;
import org.eclipse.swt.widgets.*;

import org.gudy.azureus2.core3.util.*;
import org.gudy.azureus2.ui.swt.Utils;
import org.gudy.azureus2.ui.swt.mainwindow.Colors;
import org.gudy.azureus2.ui.swt.mainwindow.IMainWindow;

import com.aelitis.azureus.ui.swt.UIFunctionsManagerSWT;
import com.aelitis.azureus.ui.swt.UIFunctionsSWT;
import com.aelitis.azureus.ui.swt.imageloader.ImageLoader;

public class LightBoxShell
{

	private Shell lbShell = null;

	private Shell parentShell = null;

	private int insetTop = 0;

	private int insetBottom = 0;

	private int insetLeft = 0;

	private int insetRight = 0;

	private boolean closeOnESC = false;

	private boolean isAlreadyOpened = false;

	private Display display;

	private UIFunctionsSWT uiFunctions;

	private boolean isBusy = false;

	/**
	 * An array to hold the off-line images for the spinner
	 */
	private Image[] spinnerImages = null;

	private Rectangle spinnerBounds = null;

	/**
	 * Indicates that the spinner is already animating
	 */
	private boolean busyAlready = false;

	/**
	 * The canvas to display the spinner
	 */
	private Canvas spinnerCanvas = null;

	private Rectangle shellBounds = null;

	private boolean parentActivatedOnce = false;

	public static final int RESIZE_VERTICAL = 1 << 1;

	public static final int RESIZE_HORIZONTAL = 1 << 2;

	private int styleMask = RESIZE_VERTICAL | RESIZE_HORIZONTAL;

	private int alphaLevel = 178;

	public LightBoxShell() {
		this(false);
	}

	/**
	 * Creates a LightBoxShell without opening it
	 * @param closeOnESC if <code>true</code> then the ESC key can be used to dismiss the lightbox
	 */
	public LightBoxShell(boolean closeOnESC) {
		this.closeOnESC = closeOnESC;

		parentShell = getUIFunctions().getMainShell();

		if (null == parentShell) {
			return;
		}
		IMainWindow mainWindow = getUIFunctions().getMainWindow();
		Rectangle r = mainWindow.getMetrics(IMainWindow.WINDOW_ELEMENT_STATUSBAR);
		setInsets(0, r.height, 0, 0);
		createControls();
	}

	public LightBoxShell(Shell parentShell, boolean closeOnESC) {
		this.parentShell = parentShell;
		this.closeOnESC = closeOnESC;
		createControls();
	}

	public void setInsets(int top, int bottom, int left, int right) {
		this.insetTop = top;
		this.insetBottom = bottom;
		this.insetLeft = left;
		this.insetRight = right;
		if (null != lbShell && false == lbShell.isDisposed()) {
			if (true == isAlreadyOpened()) {
				lbShell.setBounds(getBounds(true));
			}
		}
	}

	private void createControls() {
		if (null == parentShell) {
			return;
		}

		lbShell = new Shell(parentShell, SWT.NO_TRIM);

		if (true == Constants.isOSX) {
			getUIFunctions().createMainMenu(lbShell);
		}

		display = parentShell.getDisplay();

		/*
		* Trap and prevent the ESC key from closing the shell
		*/
		if (true == closeOnESC) {
			lbShell.addListener(SWT.Traverse, new Listener() {
				public void handleEvent(Event e) {
					if (e.detail == SWT.TRAVERSE_ESCAPE) {
						e.doit = false;
						close();
					}
				}
			});
		}

		/*
		 * For OSX add this listener to make sure that the parent shell and
		 * the lighbox shell behave like they are sandwiched together; without this
		 * then external applications can slide in between the parent shell and the
		 * lightbox which creates a strange visual effect 
		 */
		if (true == Constants.isOSX) {
			lbShell.addShellListener(new ShellAdapter() {
				public void shellActivated(ShellEvent e) {
					if (null != parentShell && false == parentShell.isDisposed()) {

						/*
						 * Making sure we are only performing this only once for each time the lbShell is activated;
						 * without this we will run into a StackOverflow as the 2 shells go back and forth activating each other
						 */
						if (false == parentActivatedOnce) {
							parentActivatedOnce = true;
							parentShell.forceActive();
						} else {
							parentActivatedOnce = false;
						}
					}
				}
			});
		}

		lbShell.addDisposeListener(new DisposeListener() {
			public void widgetDisposed(DisposeEvent e) {
				/*
				 * Disposing all the spinner images
				 */
				if (null != spinnerImages) {
					spinnerImages = null;
				}
			}

		});

		/*
		 * Add a listener to the parent shell and move/resize the lightbox to fit over it
		 */
		final ControlListener moveAndResizeListener = new ControlListener() {
			public void controlMoved(ControlEvent e) {
				shellBounds = null;
				getBounds();
				lbShell.setLocation(shellBounds.x, shellBounds.y);
			}

			public void controlResized(ControlEvent e) {

				if ((styleMask & RESIZE_HORIZONTAL) != 0
						&& (styleMask & RESIZE_VERTICAL) != 0) {
					shellBounds = null;
					getBounds();
					lbShell.setSize(shellBounds.width, shellBounds.height);
				} else if ((styleMask & RESIZE_HORIZONTAL) != 0) {
					shellBounds = null;
					getBounds();
					lbShell.setSize(shellBounds.width, lbShell.getSize().y);
				} else if ((styleMask & RESIZE_VERTICAL) != 0) {
					shellBounds = null;
					getBounds();
					lbShell.setSize(lbShell.getSize().x, shellBounds.height);
				}
			}
		};

		parentShell.addControlListener(moveAndResizeListener);

		/*
		 * When the lightbox is disposed remove the listener from the parent so we don't leave it dangling
		 */
		lbShell.addDisposeListener(new DisposeListener() {
			public void widgetDisposed(DisposeEvent e) {
				parentShell.removeControlListener(moveAndResizeListener);
			}
		});

	}

	private UIFunctionsSWT getUIFunctions() {
		if (null == uiFunctions) {
			uiFunctions = UIFunctionsManagerSWT.getUIFunctionsSWT();
			if (null == uiFunctions) {
				throw new NullPointerException(
						"An initialized instance of UIFunctionsSWT is required to create a LightBoxShell");
			}
		}
		return uiFunctions;
	}

	public void open() {
		if (null != lbShell && false == lbShell.isDisposed()) {

			lbShell.setBounds(getBounds());
			isAlreadyOpened = true;

			/*
			 * Black mask with 30% transparency
			 */
			lbShell.setBackground(Colors.black);

			try {
				lbShell.setAlpha(alphaLevel);
			} catch (Throwable t) {
				//Do nothing if alpha is not supported
			}

			lbShell.open();
		}
	}

	public void close() {
		Utils.execSWTThread(new AERunnable() {
			public void runSupport() {
				if (null != lbShell && false == lbShell.isDisposed()) {
					lbShell.dispose();
				}
			}
		});
	}

	/**
	 * Returns the current bounds of the LightBox
	 * @return
	 */
	public Rectangle getBounds() {
		return getBounds(false);
	}

	/**
	 * Returns the bounds of the LightBox; recalculate before returning if specified
	 * @param recalculate
	 * @return if <code>true</code> then recalculate the bounds before returning
	 */
	private Rectangle getBounds(boolean recalculate) {
		if (false == recalculate && null != shellBounds) {
			return new Rectangle(shellBounds.x, shellBounds.y, shellBounds.width,
					shellBounds.height);
		}

		shellBounds = parentShell.getClientArea();
		Point parentLocation = parentShell.toDisplay(insetLeft, insetTop);
		shellBounds.x = parentLocation.x;
		shellBounds.y = parentLocation.y;
		shellBounds.width -= insetRight + insetLeft;
		shellBounds.height -= insetTop + insetBottom;
		return new Rectangle(shellBounds.x, shellBounds.y, shellBounds.width,
				shellBounds.height);
	}

	/**
	 * 
	 * Creates a stylized shell with pre-defined look and feel
	 * @param borderWidth
	 * @param closeLightboxOnExit if <code>true</code> then close the parent lightbox when this pop-up is closed; otherwise leave it opened
	 * @return
	 */
	public StyledShell createPopUpShell(int borderWidth,
			boolean closeLightboxOnExit) {
		return createPopUpShell(borderWidth, closeLightboxOnExit, true);
	}

	/**
	 * Creates a stylized shell with pre-defined look and feel
	 * @param borderWidth is used for the width of the custom dialog trim; is not in effect if standard trim is specified
	 * @param closeLightboxOnExit if <code>true</code> then close the parent lightbox when this pop-up is closed; otherwise leave it opened
	 * @param useCustomTrim if <code>true</code> use our custom dialog trim; otherwise use default OS dialog trims 
	 * @return
	 */
	public StyledShell createPopUpShell(int borderWidth,
			boolean closeLightboxOnExit, boolean useCustomTrim) {
		StyledShell newShell = new StyledShell(lbShell, borderWidth, useCustomTrim);

		if (true == closeLightboxOnExit) {
			newShell.addListener(SWT.Dispose, new Listener() {
				public void handleEvent(Event event) {
					close();
				}
			});

		}
		return newShell;
	}

	/**
	 * Centers and opens the given shell and closes the light box when the given shell is closed
	 * @param shellToOpen
	 */
	public void open(StyledShell shellToOpen) {
		if (null != shellToOpen && null != lbShell) {

			if (false == isAlreadyOpened) {
				open();
			}

			if (false == shellToOpen.isAlreadyOpened()) {
				shellToOpen.open();
			}
		}
	}

	public void setCursor(Cursor cursor) {
		if (null != lbShell && false == lbShell.isDisposed()) {
			lbShell.setCursor(cursor);
		}
	}

	public void setData(String key, Object value) {
		if (null != lbShell && false == lbShell.isDisposed()) {
			lbShell.setData(key, value);
		}
	}

	public Display getDisplay() {
		return display;
	}

	public boolean isAlreadyOpened() {
		return isAlreadyOpened;
	}

	public void addDisposeListener(DisposeListener listener) {
		lbShell.addDisposeListener(listener);
	}

	/**
	 * Show a spinning indicator that a process is busy
	 * @param value if <code>true</code> then show the spinner; if <code>false</code> then stop showing the spinner
	 * @param delayInMilli the delay in milliseconds before the spinner is shown; is only in effect when isBusy is <code>true</code>
	 */
	public void showBusy(boolean value, long delayInMilli) {
		if (value == isBusy) {
			return;
		}
		isBusy = value;

		if (true == isBusy && false == busyAlready) {
			showSpinner(Math.max(0, delayInMilli));
		}
	}

	private void showSpinner(final long delayInMilli) {

		/*
		 * Create the images off-line and store them in the array if not done already;
		 * we will use these to draw onto the canvas to animate the spinner
		 */
		if (null == spinnerImages) {
			ImageLoader imageLoader = ImageLoader.getInstance();
			spinnerImages = imageLoader.getImages("spinner_big");
			if (spinnerImages.length == 0) {
				return;
			}

			int w = 0;
			int h = 0;
			for (int i = 0; i < spinnerImages.length; i++) {
				Image image = spinnerImages[i];
				Rectangle bounds = image.getBounds();
				w = Math.max(w, bounds.width);
				h = Math.max(w, bounds.height);
			}
			spinnerBounds = new Rectangle(0, 0, w, h);
			
			lbShell.addDisposeListener(new DisposeListener() {
				public void widgetDisposed(DisposeEvent e) {
					ImageLoader imageLoader = ImageLoader.getInstance();
					imageLoader.releaseImage("spinner_big");
				}
			});
		}

		/*
		 * Adjust the spinner bounds to be centered on the lightbox shell itself
		 */
		Utils.centerRelativeTo(spinnerBounds, getBounds());
		Point to_lbShell = lbShell.toControl(spinnerBounds.x, spinnerBounds.y);
		spinnerBounds.x = to_lbShell.x;
		spinnerBounds.y = to_lbShell.y;

		final int[] imageDataIndex = new int[1];
		/*
		 * Create the canvas for the spinner; size the canvas to be just enough for the image
		 */
		if (null == spinnerCanvas) {
			spinnerCanvas = new Canvas(lbShell, SWT.NONE);
			spinnerCanvas.addPaintListener(new PaintListener() {
				public void paintControl(PaintEvent e) {
					e.gc.drawImage(spinnerImages[imageDataIndex[0]], 0, 0);
				}
			});
		}
		spinnerCanvas.setBackground(lbShell.getBackground());
		spinnerCanvas.setBounds(spinnerBounds);

		/*
		 * Spinner animation 
		 */
		
		Utils.execSWTThreadLater(100, new AERunnable() {
			public void runSupport() {
				if (!isBusy || spinnerImages == null) {
					busyAlready = false;
					return;
				}
				
				busyAlready = true;
				if (null != spinnerCanvas && false == spinnerCanvas.isDisposed()) {
					spinnerCanvas.redraw();
					spinnerCanvas.update();
				}

				/* 
				 * If we have just drawn the last image start over from the beginning
				 */
				if (imageDataIndex[0] == spinnerImages.length - 1) {
					imageDataIndex[0] = 0;
				} else {
					imageDataIndex[0]++;
				}

				Utils.execSWTThreadLater(100, this);
			}
		});
	}

	public int getStyleMask() {
		return styleMask;
	}

	public void setStyleMask(int styleMask) {
		this.styleMask = styleMask;
	}

	public int getAlphaLevel() {
		return alphaLevel;
	}

	public void setAlphaLevel(final int alphaLevel) {
		this.alphaLevel = alphaLevel;

		Utils.execSWTThread(new AERunnable() {
			public void runSupport() {
				if (lbShell.isDisposed()) {
					return;
				}
				try {
					lbShell.setAlpha(alphaLevel);
				} catch (Throwable t) {
					//Do nothing if alpha is not supported
				}
			}
		});
	}

	public void moveAbove(Control control) {
		lbShell.moveAbove(control);
	}

	public Shell getShell() {
		return lbShell;
	}
}
