/**
 * 
 */
package com.aelitis.azureus.ui.swt.skin;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.*;
import org.eclipse.swt.graphics.*;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.widgets.*;

import org.gudy.azureus2.core3.util.*;
import org.gudy.azureus2.ui.swt.Utils;

import com.aelitis.azureus.ui.swt.imageloader.ImageLoader;
import com.aelitis.azureus.ui.swt.imageloader.ImageLoader.ImageDownloaderListener;

/**
 * @author TuxPaper
 * @created Jun 12, 2006
 *
 */
public class SWTSkinObjectImage
	extends SWTSkinObjectBasic
{
	protected static final Long DRAW_SCALE = new Long(1);

	protected static final Long DRAW_STRETCH = new Long(2);

	protected static final Long DRAW_NORMAL = new Long(0);

	protected static final Long DRAW_TILE = new Long(3);

	protected static final Long DRAW_CENTER = new Long(4);

	protected static final Long DRAW_HCENTER = new Long(5);

	private Canvas canvas;

	private boolean customImage;

	private String customImageID;

	private String currentImageID;

	private static PaintListener paintListener;

	private int h_align;

	static {
		paintListener = new PaintListener() {
			public void paintControl(PaintEvent e) {
				SWTSkinObject so = (SWTSkinObject) e.widget.getData("SkinObject");
				try {
					e.gc.setAdvanced(true);
					e.gc.setInterpolation(SWT.HIGH);
				} catch (Exception ex) {
				}

				Canvas control = (Canvas) e.widget;
				Image imgSrc = (Image) control.getData("image");
				Image imgRight = null;
				Image imgLeft = null;
				String idToRelease = null;
				ImageLoader imageLoader = null;

				if (imgSrc == null) {
					SWTSkinObjectImage soImage = (SWTSkinObjectImage) control.getData("SkinObject");
					imageLoader = soImage.getSkin().getImageLoader(
							soImage.getProperties());
					String imageID = (String) control.getData("ImageID");
					if (imageLoader.imageExists(imageID)) {
						idToRelease = imageID;
						Image[] images = imageLoader.getImages(imageID);
						if (images.length == 3) {
							imgLeft = images[0];
							imgSrc = images[1];
							imgRight = images[2];
						} else {
							imgSrc = images[0];
						}
					} else {
						return;
					}
				}
				Rectangle imgSrcBounds = imgSrc.getBounds();
				Point size = control.getSize();

				Long drawMode = (Long) control.getData("drawmode");

				if (drawMode == DRAW_STRETCH) {
					e.gc.drawImage(imgSrc, 0, 0, imgSrcBounds.width, imgSrcBounds.height,
							0, 0, size.x, size.y);
				} else if (drawMode == DRAW_CENTER || drawMode == DRAW_NORMAL) {
					e.gc.drawImage(imgSrc, (size.x - imgSrcBounds.width) / 2,
							(size.y - imgSrcBounds.height) / 2);
				} else if (drawMode == DRAW_HCENTER) {
					e.gc.drawImage(imgSrc, (size.x - imgSrcBounds.width) / 2, 0);
				} else if (drawMode == DRAW_SCALE) {
					// TODO: real scale..
					e.gc.drawImage(imgSrc, 0, 0, imgSrcBounds.width, imgSrcBounds.height,
							0, 0, size.x, size.y);
				} else {
					int x0 = 0;
					int y0 = 0;
					int x1 = size.x;
					int y1 = size.y;

					if (imgRight == null) {
						imgRight = (Image) control.getData("image-right");
					}
					if (imgRight != null) {
						int width = imgRight.getBounds().width;

						x1 -= width;
					}

					if (imgLeft == null) {
						imgLeft = (Image) control.getData("image-left");
					}
					if (imgLeft != null) {
						// TODO: Tile down
						e.gc.drawImage(imgLeft, 0, 0);

						x0 += imgLeft.getBounds().width;
					}

					for (int y = y0; y < y1; y += imgSrcBounds.height) {
						for (int x = x0; x < x1; x += imgSrcBounds.width) {
							e.gc.drawImage(imgSrc, x, y);
						}
					}

					if (imgRight != null) {
						// TODO: Tile down
						e.gc.drawImage(imgRight, x1, 0);
					}
				}
				if (idToRelease != null && imageLoader != null) {
					imageLoader.releaseImage(idToRelease);
				}
			}
		};
	}

	/**
	 * @param skin 
	 * 
	 */
	public SWTSkinObjectImage(SWTSkin skin, SWTSkinProperties skinProperties,
			String sID, String sConfigID, String sImageID, SWTSkinObject parent) {
		super(skin, skinProperties, sID, sConfigID, "image", parent);
		setControl(createImageWidget(sConfigID, sImageID));
		customImage = false;
		customImageID = null;
	}

	private Canvas createImageWidget(String sConfigID, String sImageID) {
		currentImageID = sImageID;
		int style = SWT.WRAP | SWT.DOUBLE_BUFFERED;

		String sAlign = properties.getStringValue(sConfigID + ".align");
		if (sAlign != null && !Constants.isUnix) {
			h_align = SWTSkinUtils.getAlignment(sAlign, SWT.NONE);
			if (h_align != SWT.NONE) {
				style |= h_align;
			}
		}

		if (properties.getIntValue(sConfigID + ".border", 0) == 1) {
			style |= SWT.BORDER;
		}

		Composite createOn;
		if (parent == null) {
			createOn = skin.getShell();
		} else {
			createOn = (Composite) parent.getControl();
		}

		canvas = new Canvas(createOn, style);
		canvas.setData("SkinObject", this);

//		 {
//				public Point computeSize(int wHint, int hHint) {
//					Object image = canvas.getData("image");
//					Object imageID = canvas.getData("ImageID");
//					if (image == null
//							&& (imageID == null || ((String) imageID).length() == 0)) {
//						return new Point(0, 0);
//					}
//					return super.computeSize(wHint, hHint);
//				};
//
//				public Point computeSize(int wHint, int hHint, boolean changed) {
//					Object image = canvas.getData("image");
//					Object imageID = canvas.getData("ImageID");
//					if (image == null
//							&& (imageID == null || ((String) imageID).length() == 0)) {
//						return new Point(0, 0);
//					}
//					return super.computeSize(wHint, hHint, changed);
//				};
//			};

		Color color = properties.getColor(sConfigID + ".color");
		if (color != null) {
			canvas.setBackground(color);
		}

		final String sURL = properties.getStringValue(sConfigID + ".url");
		if (sURL != null && sURL.length() > 0) {
			canvas.setToolTipText(sURL);
			canvas.addListener(SWT.MouseUp, new Listener() {
				public void handleEvent(Event arg0) {
					Utils.launch(UrlUtils.encode(sURL));
				}
			});
		}

		String sCursor = properties.getStringValue(sConfigID + ".cursor");
		if (sCursor != null && sCursor.length() > 0) {
			if (sCursor.equalsIgnoreCase("hand")) {
				canvas.addListener(SWT.MouseEnter,
						skin.getHandCursorListener(canvas.getDisplay()));
				canvas.addListener(SWT.MouseExit,
						skin.getHandCursorListener(canvas.getDisplay()));
			}
		}

		//		SWTBGImagePainter painter = (SWTBGImagePainter) parent.getData("BGPainter");
		//		if (painter != null) {
		//			canvas.addListener(SWT.Paint, painter);
		//		}
		canvas.addDisposeListener(new DisposeListener() {
			public void widgetDisposed(DisposeEvent e) {
				String oldImageID = (String) canvas.getData("ImageID");
				if (oldImageID != null && canvas.getData("image") != null) {
					ImageLoader imageLoader = skin.getImageLoader(properties);
					imageLoader.releaseImage(oldImageID);
				}
			}
		});

		// needed to set paint listener and canvas size
		reallySetImage();

		return canvas;
	}
	
	public void setVisible(boolean visible) {
		super.setVisible(visible);

		if (visible) {
			reallySetImage();
		}
	}

	//protected void setCanvasImage(String sConfigID, AECallback<Image> callback) {
	protected void setCanvasImage(String sImageID, AECallback callback) {
		setCanvasImage(sConfigID, sImageID, callback);
	}

	//private void setCanvasImage(final String sConfigID, final String sImageID, AECallback<Image> callback) {
	private void setCanvasImage(final String sConfigID, final String sImageID,
			AECallback callback) {
		Utils.execSWTThread(new AERunnableWithCallback(callback) {

			public Object runSupport() {
				if (canvas == null || canvas.isDisposed()) {
					return null;
				}

				String oldImageID = (String) canvas.getData("ImageID");
				if (sImageID != null && sImageID.equals(oldImageID)) {
					return null;
				}

				ImageLoader imageLoader = skin.getImageLoader(properties);

				if (oldImageID != null && canvas.getData("image") != null) {
					imageLoader.releaseImage(oldImageID);
				}

				Image[] images = sImageID == null || sImageID.length() == 0 ? null
						: imageLoader.getImages(sImageID);

				Image image = null;

				if (images.length == 3) {
					Image imageLeft = images[0];
					if (ImageLoader.isRealImage(imageLeft)) {
						canvas.setData("image-left", imageLeft);
					}

					image = images[1];

					Image imageRight = images[2];
					if (ImageLoader.isRealImage(imageRight)) {
						canvas.setData("image-right", imageRight);
					}
				} else if (images.length > 0) {
					image = images[0];
				}

				if (image == null) {
					image = ImageLoader.noImage;
				}

				String sDrawMode = properties.getStringValue(sConfigID + ".drawmode");
				if (sDrawMode == null) {
					sDrawMode = properties.getStringValue(
							SWTSkinObjectImage.this.sConfigID + ".drawmode", "");
				}

				//allowImageDimming = sDrawMode.equalsIgnoreCase("dim");

				Long drawMode;
				if (sDrawMode.equals("scale")) {
					drawMode = DRAW_SCALE;
				} else if (sDrawMode.equals("stretch")) {
					drawMode = DRAW_STRETCH;
				} else if (sDrawMode.equals("center")) {
					drawMode = DRAW_CENTER;
				} else if (sDrawMode.equals("h-center")) {
					drawMode = DRAW_HCENTER;
				} else if (sDrawMode.equalsIgnoreCase("tile")) {
					drawMode = DRAW_TILE;
				} else {
					drawMode = DRAW_NORMAL;
				}
				canvas.setData("drawmode", drawMode);

				Rectangle imgBounds = image.getBounds();
				if (drawMode != DRAW_CENTER && drawMode != DRAW_HCENTER
						&& drawMode != DRAW_STRETCH) {
					canvas.setSize(imgBounds.width, imgBounds.height);
				}
				//canvas.setData("image", image);

				if (drawMode == DRAW_TILE || drawMode == DRAW_NORMAL) {
					// XXX Huh? A tile of one? :)
					FormData fd = (FormData) canvas.getLayoutData();
					if (fd == null) {
						fd = new FormData(imgBounds.width, imgBounds.height);
					} else {
						fd.width = imgBounds.width;
						fd.height = imgBounds.height;
					}
					canvas.setLayoutData(fd);
					Utils.relayout(canvas);
				}
				
				// remove in case already added
				canvas.removePaintListener(paintListener);

				canvas.addPaintListener(paintListener);
				canvas.setData("ImageID", sImageID);

				canvas.redraw();

				SWTSkinUtils.addMouseImageChangeListeners(canvas);
				imageLoader.releaseImage(sImageID);
				return null;
			}
		});
	}

	// @see com.aelitis.azureus.ui.swt.skin.SWTSkinObject#setBackground(java.lang.String, java.lang.String)
	public void setBackground(String sConfigID, String sSuffix) {
		// No background for images?
	}

	// @see com.aelitis.azureus.ui.swt.skin.SWTSkinObject#switchSuffix(java.lang.String)
	public String switchSuffix(String suffix, int level, boolean walkUp,
			boolean walkDown) {
		suffix = super.switchSuffix(suffix, level, walkUp, walkDown);
		if (customImage) {
			return suffix;
		}
		if (suffix == null) {
			return null;
		}
		
		final String fSuffix = suffix;

		Utils.execSWTThread(new AERunnable() {

			public void runSupport() {
				currentImageID = (customImageID == null ? (sConfigID + ".image")
						: customImageID)
						+ fSuffix;
				if (isVisible()) {
					reallySetImage();
				}
			}
		});

		return suffix;
	}

	protected void reallySetImage() {
		if (currentImageID == null || customImage) {
			return;
		}

		ImageLoader imageLoader = skin.getImageLoader(properties);
		boolean imageExists = imageLoader.imageExists(currentImageID);
		if (!imageExists && imageLoader.imageExists(currentImageID + ".image")) {
			currentImageID = sConfigID + ".image";
			imageExists = true;
		}
		if (!imageExists && suffixes != null) {
			for (int i = suffixes.length - 1; i >= 0; i--) {
				String suffixToRemove = suffixes[i];
				if (suffixToRemove != null) {
					currentImageID = currentImageID.substring(0, currentImageID.length()
							- suffixToRemove.length());
					if (imageLoader.imageExists(currentImageID)) {
						imageExists = true;
						break;
					}
				}
			}
		}

		if (imageExists) {
			setCanvasImage(currentImageID, null);
		} else {
			Utils.execSWTThread(new AERunnable() {
				public void runSupport() {
					FormData fd = (FormData) canvas.getLayoutData();
					if (fd == null) {
						fd = new FormData(0, 0);
					} else {
						fd.width = 0;
						fd.height = 0;
					}
					canvas.setLayoutData(fd);
					Utils.relayout(canvas);
				}
			});
		}
	}

	public void setImage(final Image image) {
		Utils.execSWTThread(new AERunnable() {
			public void runSupport() {
				customImage = true;
				customImageID = null;
				canvas.setData("image", image);
				canvas.setData("ImageID", null);
				canvas.setData("image-left", null);
				canvas.setData("image-right", null);

				canvas.removePaintListener(paintListener);
				canvas.addPaintListener(paintListener);

				Utils.relayout(canvas);
				canvas.redraw();
			}
		});
	}

	public void setImageByID(String sConfigID, AECallback callback) {
		if (customImage == false && customImageID != null
				&& customImageID.equals(sConfigID)) {
			if (callback != null) {
				callback.callbackFailure(null);
			}
			return;
		}
		customImage = false;
		customImageID = sConfigID;

		String sImageID = sConfigID + getSuffix();
		ImageLoader imageLoader = skin.getImageLoader(properties);
		Image image = imageLoader.getImage(sImageID);
		if (ImageLoader.isRealImage(image)) {
			setCanvasImage(sConfigID, sImageID, callback);
		} else {
			setCanvasImage(sConfigID, sConfigID, callback);
		}
		imageLoader.releaseImage(sImageID);
		return;
	}

	public void setImageUrl(final String url) {
		if (customImage == false && customImageID != null
				&& customImageID.equals(url)) {
			return;
		}
		customImage = false;
		customImageID = url;

		Utils.execSWTThread(new AERunnable() {
			public void runSupport() {
				final ImageLoader imageLoader = skin.getImageLoader(properties);
				imageLoader.getUrlImage(url, new ImageDownloaderListener() {
					public void imageDownloaded(Image image, boolean returnedImmediately) {
						setCanvasImage(url, null);
						imageLoader.releaseImage(url);
					}
				});
			}
		});
	}
}
