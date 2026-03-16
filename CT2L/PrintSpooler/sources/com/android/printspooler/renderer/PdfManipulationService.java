package com.android.printspooler.renderer;

import android.app.Service;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.pdf.PdfEditor;
import android.graphics.pdf.PdfRenderer;
import android.os.IBinder;
import android.os.ParcelFileDescriptor;
import android.os.RemoteException;
import android.print.PageRange;
import android.print.PrintAttributes;
import android.util.Log;
import com.android.printspooler.renderer.IPdfEditor;
import com.android.printspooler.renderer.IPdfRenderer;
import com.android.printspooler.util.BitmapSerializeUtils;
import com.android.printspooler.util.PageRangeUtils;
import java.io.IOException;
import libcore.io.IoUtils;

public final class PdfManipulationService extends Service {
    @Override
    public IBinder onBind(Intent intent) {
        String action;
        action = intent.getAction();
        switch (action) {
            case "com.android.printspooler.renderer.ACTION_GET_RENDERER":
                return new PdfRendererImpl();
            case "com.android.printspooler.renderer.ACTION_GET_EDITOR":
                return new PdfEditorImpl();
            default:
                throw new IllegalArgumentException("Invalid intent action:" + action);
        }
    }

    private final class PdfRendererImpl extends IPdfRenderer.Stub {
        private Bitmap mBitmap;
        private final Object mLock;
        private PdfRenderer mRenderer;

        private PdfRendererImpl() {
            this.mLock = new Object();
        }

        @Override
        public int openDocument(ParcelFileDescriptor source) throws RemoteException {
            int pageCount;
            Exception e;
            synchronized (this.mLock) {
                try {
                    throwIfOpened();
                    this.mRenderer = new PdfRenderer(source);
                    pageCount = this.mRenderer.getPageCount();
                } catch (IOException e2) {
                    e = e2;
                    IoUtils.closeQuietly(source);
                    Log.e("PdfManipulationService", "Cannot open file", e);
                    pageCount = -2;
                } catch (IllegalStateException e3) {
                    e = e3;
                    IoUtils.closeQuietly(source);
                    Log.e("PdfManipulationService", "Cannot open file", e);
                    pageCount = -2;
                } catch (SecurityException e4) {
                    IoUtils.closeQuietly(source);
                    Log.e("PdfManipulationService", "Cannot open file", e4);
                    pageCount = -3;
                }
            }
            return pageCount;
        }

        @Override
        public void renderPage(int pageIndex, int bitmapWidth, int bitmapHeight, PrintAttributes attributes, ParcelFileDescriptor destination) {
            float displayScale;
            synchronized (this.mLock) {
                try {
                    throwIfNotOpened();
                    PdfRenderer.Page page = this.mRenderer.openPage(pageIndex);
                    int srcWidthPts = page.getWidth();
                    int srcHeightPts = page.getHeight();
                    int dstWidthPts = PdfManipulationService.pointsFromMils(attributes.getMediaSize().getWidthMils());
                    int dstHeightPts = PdfManipulationService.pointsFromMils(attributes.getMediaSize().getHeightMils());
                    boolean scaleContent = this.mRenderer.shouldScaleForPrinting();
                    boolean contentLandscape = !attributes.getMediaSize().isPortrait();
                    Matrix matrix = new Matrix();
                    if (scaleContent) {
                        displayScale = Math.min(bitmapWidth / srcWidthPts, bitmapHeight / srcHeightPts);
                    } else if (contentLandscape) {
                        displayScale = bitmapHeight / dstHeightPts;
                    } else {
                        displayScale = bitmapWidth / dstWidthPts;
                    }
                    matrix.postScale(displayScale, displayScale);
                    Configuration configuration = PdfManipulationService.this.getResources().getConfiguration();
                    if (configuration.getLayoutDirection() == 1) {
                        matrix.postTranslate(bitmapWidth - (srcWidthPts * displayScale), 0.0f);
                    }
                    PrintAttributes.Margins minMargins = attributes.getMinMargins();
                    int paddingLeftPts = PdfManipulationService.pointsFromMils(minMargins.getLeftMils());
                    int paddingTopPts = PdfManipulationService.pointsFromMils(minMargins.getTopMils());
                    int paddingRightPts = PdfManipulationService.pointsFromMils(minMargins.getRightMils());
                    int paddingBottomPts = PdfManipulationService.pointsFromMils(minMargins.getBottomMils());
                    Rect clip = new Rect();
                    clip.left = (int) (paddingLeftPts * displayScale);
                    clip.top = (int) (paddingTopPts * displayScale);
                    clip.right = (int) (bitmapWidth - (paddingRightPts * displayScale));
                    clip.bottom = (int) (bitmapHeight - (paddingBottomPts * displayScale));
                    Bitmap bitmap = getBitmapForSize(bitmapWidth, bitmapHeight);
                    page.render(bitmap, clip, matrix, 1);
                    page.close();
                    BitmapSerializeUtils.writeBitmapPixels(bitmap, destination);
                } finally {
                    IoUtils.closeQuietly(destination);
                }
            }
        }

        @Override
        public void closeDocument() {
            synchronized (this.mLock) {
                throwIfNotOpened();
                this.mRenderer.close();
                this.mRenderer = null;
            }
        }

        private Bitmap getBitmapForSize(int width, int height) {
            if (this.mBitmap != null) {
                if (this.mBitmap.getWidth() == width && this.mBitmap.getHeight() == height) {
                    this.mBitmap.eraseColor(-1);
                    return this.mBitmap;
                }
                this.mBitmap.recycle();
            }
            this.mBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            this.mBitmap.eraseColor(-1);
            return this.mBitmap;
        }

        private void throwIfOpened() {
            if (this.mRenderer != null) {
                throw new IllegalStateException("Already opened");
            }
        }

        private void throwIfNotOpened() {
            if (this.mRenderer == null) {
                throw new IllegalStateException("Not opened");
            }
        }
    }

    private final class PdfEditorImpl extends IPdfEditor.Stub {
        private PdfEditor mEditor;
        private final Object mLock;

        private PdfEditorImpl() {
            this.mLock = new Object();
        }

        @Override
        public int openDocument(ParcelFileDescriptor source) throws RemoteException {
            Exception e;
            int pageCount;
            synchronized (this.mLock) {
                try {
                    throwIfOpened();
                    this.mEditor = new PdfEditor(source);
                    pageCount = this.mEditor.getPageCount();
                } catch (IOException e2) {
                    e = e2;
                    IoUtils.closeQuietly(source);
                    Log.e("PdfManipulationService", "Cannot open file", e);
                    throw new RemoteException(e.toString());
                } catch (IllegalStateException e3) {
                    e = e3;
                    IoUtils.closeQuietly(source);
                    Log.e("PdfManipulationService", "Cannot open file", e);
                    throw new RemoteException(e.toString());
                }
            }
            return pageCount;
        }

        @Override
        public void removePages(PageRange[] ranges) {
            synchronized (this.mLock) {
                throwIfNotOpened();
                PageRange[] ranges2 = PageRangeUtils.normalize(ranges);
                int rangeCount = ranges2.length;
                for (int i = rangeCount - 1; i >= 0; i--) {
                    PageRange range = ranges2[i];
                    for (int j = range.getEnd(); j >= range.getStart(); j--) {
                        this.mEditor.removePage(j);
                    }
                }
            }
        }

        @Override
        public void applyPrintAttributes(PrintAttributes attributes) {
            float scale;
            synchronized (this.mLock) {
                throwIfNotOpened();
                Rect mediaBox = new Rect();
                Rect cropBox = new Rect();
                Matrix transform = new Matrix();
                boolean contentPortrait = attributes.getMediaSize().isPortrait();
                boolean layoutDirectionRtl = PdfManipulationService.this.getResources().getConfiguration().getLayoutDirection() == 1;
                int dstWidthPts = contentPortrait ? PdfManipulationService.pointsFromMils(attributes.getMediaSize().getWidthMils()) : PdfManipulationService.pointsFromMils(attributes.getMediaSize().getHeightMils());
                int dstHeightPts = contentPortrait ? PdfManipulationService.pointsFromMils(attributes.getMediaSize().getHeightMils()) : PdfManipulationService.pointsFromMils(attributes.getMediaSize().getWidthMils());
                boolean scaleForPrinting = this.mEditor.shouldScaleForPrinting();
                int pageCount = this.mEditor.getPageCount();
                for (int i = 0; i < pageCount; i++) {
                    if (!this.mEditor.getPageMediaBox(i, mediaBox)) {
                        Log.e("PdfManipulationService", "Malformed PDF file");
                        return;
                    }
                    int srcWidthPts = mediaBox.width();
                    int srcHeightPts = mediaBox.height();
                    mediaBox.right = dstWidthPts;
                    mediaBox.bottom = dstHeightPts;
                    this.mEditor.setPageMediaBox(i, mediaBox);
                    transform.setTranslate(0.0f, srcHeightPts - dstHeightPts);
                    if (!contentPortrait) {
                        transform.postRotate(270.0f);
                        transform.postTranslate(0.0f, dstHeightPts);
                    }
                    if (scaleForPrinting) {
                        if (contentPortrait) {
                            scale = Math.min(dstWidthPts / srcWidthPts, dstHeightPts / srcHeightPts);
                            transform.postScale(scale, scale);
                        } else {
                            scale = Math.min(dstWidthPts / srcHeightPts, dstHeightPts / srcWidthPts);
                            transform.postScale(scale, scale, mediaBox.left, mediaBox.bottom);
                        }
                    } else {
                        scale = 1.0f;
                    }
                    if (this.mEditor.getPageCropBox(i, cropBox)) {
                        cropBox.left = (int) ((cropBox.left * scale) + 0.5f);
                        cropBox.top = (int) ((cropBox.top * scale) + 0.5f);
                        cropBox.right = (int) ((cropBox.right * scale) + 0.5f);
                        cropBox.bottom = (int) ((cropBox.bottom * scale) + 0.5f);
                        cropBox.intersect(mediaBox);
                        this.mEditor.setPageCropBox(i, cropBox);
                    }
                    if (layoutDirectionRtl) {
                        float dx = contentPortrait ? dstWidthPts - ((int) ((srcWidthPts * scale) + 0.5f)) : 0.0f;
                        float dy = contentPortrait ? 0.0f : -(dstHeightPts - ((int) ((srcWidthPts * scale) + 0.5f)));
                        transform.postTranslate(dx, dy);
                    }
                    PrintAttributes.Margins minMargins = attributes.getMinMargins();
                    int paddingLeftPts = PdfManipulationService.pointsFromMils(minMargins.getLeftMils());
                    int paddingTopPts = PdfManipulationService.pointsFromMils(minMargins.getTopMils());
                    int paddingRightPts = PdfManipulationService.pointsFromMils(minMargins.getRightMils());
                    int paddingBottomPts = PdfManipulationService.pointsFromMils(minMargins.getBottomMils());
                    Rect clip = new Rect(mediaBox);
                    clip.left += paddingLeftPts;
                    clip.top += paddingTopPts;
                    clip.right -= paddingRightPts;
                    clip.bottom -= paddingBottomPts;
                    this.mEditor.setTransformAndClip(i, transform, clip);
                }
            }
        }

        @Override
        public void write(ParcelFileDescriptor destination) throws RemoteException {
            Exception e;
            synchronized (this.mLock) {
                try {
                    throwIfNotOpened();
                    this.mEditor.write(destination);
                } catch (IOException e2) {
                    e = e2;
                    IoUtils.closeQuietly(destination);
                    Log.e("PdfManipulationService", "Error writing PDF to file.", e);
                    throw new RemoteException(e.toString());
                } catch (IllegalStateException e3) {
                    e = e3;
                    IoUtils.closeQuietly(destination);
                    Log.e("PdfManipulationService", "Error writing PDF to file.", e);
                    throw new RemoteException(e.toString());
                }
            }
        }

        @Override
        public void closeDocument() {
            synchronized (this.mLock) {
                throwIfNotOpened();
                this.mEditor.close();
                this.mEditor = null;
            }
        }

        private void throwIfOpened() {
            if (this.mEditor != null) {
                throw new IllegalStateException("Already opened");
            }
        }

        private void throwIfNotOpened() {
            if (this.mEditor == null) {
                throw new IllegalStateException("Not opened");
            }
        }
    }

    private static int pointsFromMils(int mils) {
        return (int) ((mils / 1000.0f) * 72.0f);
    }
}
