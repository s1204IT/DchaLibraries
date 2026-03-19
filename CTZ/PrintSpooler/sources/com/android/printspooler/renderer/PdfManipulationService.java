package com.android.printspooler.renderer;

import android.app.Service;
import android.content.Intent;
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
        byte b;
        String action = intent.getAction();
        int iHashCode = action.hashCode();
        if (iHashCode != -431502969) {
            b = (iHashCode == -160217507 && action.equals("com.android.printspooler.renderer.ACTION_GET_RENDERER")) ? (byte) 0 : (byte) -1;
        } else if (action.equals("com.android.printspooler.renderer.ACTION_GET_EDITOR")) {
            b = 1;
        }
        switch (b) {
            case 0:
                return new PdfRendererImpl();
            case 1:
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
        public int openDocument(ParcelFileDescriptor parcelFileDescriptor) throws RemoteException {
            int pageCount;
            synchronized (this.mLock) {
                try {
                    try {
                        try {
                            throwIfOpened();
                            this.mRenderer = new PdfRenderer(parcelFileDescriptor);
                            pageCount = this.mRenderer.getPageCount();
                        } catch (SecurityException e) {
                            IoUtils.closeQuietly(parcelFileDescriptor);
                            Log.e("PdfManipulationService", "Cannot open file", e);
                            return -3;
                        }
                    } catch (IOException | IllegalStateException e2) {
                        IoUtils.closeQuietly(parcelFileDescriptor);
                        Log.e("PdfManipulationService", "Cannot open file", e2);
                        return -2;
                    }
                } catch (Throwable th) {
                    throw th;
                }
            }
            return pageCount;
        }

        @Override
        public void renderPage(int i, int i2, int i3, PrintAttributes printAttributes, ParcelFileDescriptor parcelFileDescriptor) {
            PdfRenderer.Page pageOpenPage;
            Throwable th;
            float fMin;
            synchronized (this.mLock) {
                try {
                    try {
                        throwIfNotOpened();
                        pageOpenPage = this.mRenderer.openPage(i);
                        th = null;
                    } catch (Throwable th2) {
                        Log.e("PdfManipulationService", "Cannot render page", th2);
                    }
                    try {
                        int width = pageOpenPage.getWidth();
                        int height = pageOpenPage.getHeight();
                        int iPointsFromMils = PdfManipulationService.pointsFromMils(printAttributes.getMediaSize().getWidthMils());
                        int iPointsFromMils2 = PdfManipulationService.pointsFromMils(printAttributes.getMediaSize().getHeightMils());
                        boolean zShouldScaleForPrinting = this.mRenderer.shouldScaleForPrinting();
                        boolean z = !printAttributes.getMediaSize().isPortrait();
                        Matrix matrix = new Matrix();
                        if (zShouldScaleForPrinting) {
                            fMin = Math.min(i2 / width, i3 / height);
                        } else if (z) {
                            fMin = i3 / iPointsFromMils2;
                        } else {
                            fMin = i2 / iPointsFromMils;
                        }
                        matrix.postScale(fMin, fMin);
                        if (PdfManipulationService.this.getResources().getConfiguration().getLayoutDirection() == 1) {
                            matrix.postTranslate(i2 - (width * fMin), 0.0f);
                        }
                        PrintAttributes.Margins minMargins = printAttributes.getMinMargins();
                        int iPointsFromMils3 = PdfManipulationService.pointsFromMils(minMargins.getLeftMils());
                        int iPointsFromMils4 = PdfManipulationService.pointsFromMils(minMargins.getTopMils());
                        int iPointsFromMils5 = PdfManipulationService.pointsFromMils(minMargins.getRightMils());
                        int iPointsFromMils6 = PdfManipulationService.pointsFromMils(minMargins.getBottomMils());
                        Rect rect = new Rect();
                        rect.left = (int) (iPointsFromMils3 * fMin);
                        rect.top = (int) (iPointsFromMils4 * fMin);
                        rect.right = (int) (i2 - (iPointsFromMils5 * fMin));
                        rect.bottom = (int) (i3 - (iPointsFromMils6 * fMin));
                        Bitmap bitmapForSize = getBitmapForSize(i2, i3);
                        pageOpenPage.render(bitmapForSize, rect, matrix, 1);
                        BitmapSerializeUtils.writeBitmapPixels(bitmapForSize, parcelFileDescriptor);
                        if (pageOpenPage != null) {
                            pageOpenPage.close();
                        }
                    } catch (Throwable th3) {
                        if (pageOpenPage != null) {
                            if (0 != 0) {
                                try {
                                    pageOpenPage.close();
                                } catch (Throwable th4) {
                                    th.addSuppressed(th4);
                                }
                            } else {
                                pageOpenPage.close();
                            }
                        }
                        throw th3;
                    }
                } finally {
                    IoUtils.closeQuietly(parcelFileDescriptor);
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

        private Bitmap getBitmapForSize(int i, int i2) {
            if (this.mBitmap != null) {
                if (this.mBitmap.getWidth() == i && this.mBitmap.getHeight() == i2) {
                    this.mBitmap.eraseColor(-1);
                    return this.mBitmap;
                }
                this.mBitmap.recycle();
            }
            this.mBitmap = Bitmap.createBitmap(i, i2, Bitmap.Config.ARGB_8888);
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
        public int openDocument(ParcelFileDescriptor parcelFileDescriptor) throws RemoteException {
            int pageCount;
            synchronized (this.mLock) {
                try {
                    try {
                        throwIfOpened();
                        this.mEditor = new PdfEditor(parcelFileDescriptor);
                        pageCount = this.mEditor.getPageCount();
                    } catch (IOException | IllegalStateException e) {
                        IoUtils.closeQuietly(parcelFileDescriptor);
                        Log.e("PdfManipulationService", "Cannot open file", e);
                        throw new RemoteException(e.toString());
                    }
                } catch (Throwable th) {
                    throw th;
                }
            }
            return pageCount;
        }

        @Override
        public void removePages(PageRange[] pageRangeArr) {
            int end;
            synchronized (this.mLock) {
                throwIfNotOpened();
                PageRange[] pageRangeArrNormalize = PageRangeUtils.normalize(pageRangeArr);
                int pageCount = this.mEditor.getPageCount() - 1;
                for (int length = pageRangeArrNormalize.length - 1; length >= 0; length--) {
                    PageRange pageRange = pageRangeArrNormalize[length];
                    if (pageRange.getEnd() <= pageCount) {
                        for (end = pageRange.getEnd(); end >= pageRange.getStart(); end--) {
                            this.mEditor.removePage(end);
                        }
                    } else if (pageRange.getStart() <= pageCount) {
                        pageRange = new PageRange(pageRange.getStart(), pageCount);
                        while (end >= pageRange.getStart()) {
                        }
                    }
                }
            }
        }

        @Override
        public void applyPrintAttributes(PrintAttributes printAttributes) {
            float fMin;
            synchronized (this.mLock) {
                throwIfNotOpened();
                Rect rect = new Rect();
                Rect rect2 = new Rect();
                Matrix matrix = new Matrix();
                int i = 0;
                boolean z = true;
                if (PdfManipulationService.this.getResources().getConfiguration().getLayoutDirection() != 1) {
                    z = false;
                }
                int iPointsFromMils = PdfManipulationService.pointsFromMils(printAttributes.getMediaSize().getWidthMils());
                int iPointsFromMils2 = PdfManipulationService.pointsFromMils(printAttributes.getMediaSize().getHeightMils());
                boolean zShouldScaleForPrinting = this.mEditor.shouldScaleForPrinting();
                int pageCount = this.mEditor.getPageCount();
                while (i < pageCount) {
                    if (!this.mEditor.getPageMediaBox(i, rect)) {
                        Log.e("PdfManipulationService", "Malformed PDF file");
                        return;
                    }
                    int iWidth = rect.width();
                    int iHeight = rect.height();
                    rect.right = iPointsFromMils;
                    rect.bottom = iPointsFromMils2;
                    this.mEditor.setPageMediaBox(i, rect);
                    matrix.setTranslate(0.0f, iHeight - iPointsFromMils2);
                    if (zShouldScaleForPrinting) {
                        fMin = Math.min(iPointsFromMils / iWidth, iPointsFromMils2 / iHeight);
                        matrix.postScale(fMin, fMin);
                    } else {
                        fMin = 1.0f;
                    }
                    if (this.mEditor.getPageCropBox(i, rect2)) {
                        rect2.left = (int) ((rect2.left * fMin) + 0.5f);
                        rect2.top = (int) ((rect2.top * fMin) + 0.5f);
                        rect2.right = (int) ((rect2.right * fMin) + 0.5f);
                        rect2.bottom = (int) ((rect2.bottom * fMin) + 0.5f);
                        rect2.intersect(rect);
                        this.mEditor.setPageCropBox(i, rect2);
                    }
                    if (z) {
                        matrix.postTranslate(iPointsFromMils - ((int) ((iWidth * fMin) + 0.5f)), 0.0f);
                    }
                    PrintAttributes.Margins minMargins = printAttributes.getMinMargins();
                    int iPointsFromMils3 = PdfManipulationService.pointsFromMils(minMargins.getLeftMils());
                    int iPointsFromMils4 = PdfManipulationService.pointsFromMils(minMargins.getTopMils());
                    int iPointsFromMils5 = PdfManipulationService.pointsFromMils(minMargins.getRightMils());
                    int iPointsFromMils6 = PdfManipulationService.pointsFromMils(minMargins.getBottomMils());
                    Rect rect3 = new Rect(rect);
                    rect3.left += iPointsFromMils3;
                    rect3.top += iPointsFromMils4;
                    rect3.right -= iPointsFromMils5;
                    rect3.bottom -= iPointsFromMils6;
                    this.mEditor.setTransformAndClip(i, matrix, rect3);
                    i++;
                    rect = rect;
                }
            }
        }

        @Override
        public void write(ParcelFileDescriptor parcelFileDescriptor) throws RemoteException {
            synchronized (this.mLock) {
                try {
                    try {
                        throwIfNotOpened();
                        this.mEditor.write(parcelFileDescriptor);
                    } catch (IOException | IllegalStateException e) {
                        IoUtils.closeQuietly(parcelFileDescriptor);
                        Log.e("PdfManipulationService", "Error writing PDF to file.", e);
                        throw new RemoteException(e.toString());
                    }
                } catch (Throwable th) {
                    throw th;
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

    private static int pointsFromMils(int i) {
        return (int) ((i / 1000.0f) * 72.0f);
    }
}
