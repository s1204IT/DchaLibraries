package android.graphics.pdf;

import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.Point;
import android.graphics.Rect;
import android.os.ParcelFileDescriptor;
import android.system.ErrnoException;
import android.system.OsConstants;
import dalvik.system.CloseGuard;
import java.io.IOException;
import libcore.io.Libcore;

public final class PdfRenderer implements AutoCloseable {
    private Page mCurrentPage;
    private ParcelFileDescriptor mInput;
    private final long mNativeDocument;
    private final int mPageCount;
    private final CloseGuard mCloseGuard = CloseGuard.get();
    private final Point mTempPoint = new Point();

    private static native void nativeClose(long j);

    private static native void nativeClosePage(long j);

    private static native long nativeCreate(int i, long j);

    private static native int nativeGetPageCount(long j);

    private static native long nativeOpenPageAndGetSize(long j, int i, Point point);

    private static native void nativeRenderPage(long j, long j2, long j3, int i, int i2, int i3, int i4, long j4, int i5);

    private static native boolean nativeScaleForPrinting(long j);

    public PdfRenderer(ParcelFileDescriptor input) throws IOException {
        if (input == null) {
            throw new NullPointerException("input cannot be null");
        }
        try {
            Libcore.os.lseek(input.getFileDescriptor(), 0L, OsConstants.SEEK_SET);
            long size = Libcore.os.fstat(input.getFileDescriptor()).st_size;
            this.mInput = input;
            this.mNativeDocument = nativeCreate(this.mInput.getFd(), size);
            this.mPageCount = nativeGetPageCount(this.mNativeDocument);
            this.mCloseGuard.open("close");
        } catch (ErrnoException e) {
            throw new IllegalArgumentException("file descriptor not seekable");
        }
    }

    @Override
    public void close() {
        throwIfClosed();
        throwIfPageOpened();
        doClose();
    }

    public int getPageCount() {
        throwIfClosed();
        return this.mPageCount;
    }

    public boolean shouldScaleForPrinting() {
        throwIfClosed();
        return nativeScaleForPrinting(this.mNativeDocument);
    }

    public Page openPage(int index) {
        throwIfClosed();
        throwIfPageOpened();
        throwIfPageNotInDocument(index);
        this.mCurrentPage = new Page(index);
        return this.mCurrentPage;
    }

    protected void finalize() throws Throwable {
        try {
            this.mCloseGuard.warnIfOpen();
            if (this.mInput != null) {
                doClose();
            }
        } finally {
            super.finalize();
        }
    }

    private void doClose() {
        if (this.mCurrentPage != null) {
            this.mCurrentPage.close();
        }
        nativeClose(this.mNativeDocument);
        try {
            this.mInput.close();
        } catch (IOException e) {
        }
        this.mInput = null;
        this.mCloseGuard.close();
    }

    private void throwIfClosed() {
        if (this.mInput == null) {
            throw new IllegalStateException("Already closed");
        }
    }

    private void throwIfPageOpened() {
        if (this.mCurrentPage != null) {
            throw new IllegalStateException("Current page not closed");
        }
    }

    private void throwIfPageNotInDocument(int pageIndex) {
        if (pageIndex < 0 || pageIndex >= this.mPageCount) {
            throw new IllegalArgumentException("Invalid page index");
        }
    }

    public final class Page implements AutoCloseable {
        public static final int RENDER_MODE_FOR_DISPLAY = 1;
        public static final int RENDER_MODE_FOR_PRINT = 2;
        private final CloseGuard mCloseGuard;
        private final int mHeight;
        private final int mIndex;
        private long mNativePage;
        private final int mWidth;

        private Page(int index) {
            this.mCloseGuard = CloseGuard.get();
            Point size = PdfRenderer.this.mTempPoint;
            this.mNativePage = PdfRenderer.nativeOpenPageAndGetSize(PdfRenderer.this.mNativeDocument, index, size);
            this.mIndex = index;
            this.mWidth = size.x;
            this.mHeight = size.y;
            this.mCloseGuard.open("close");
        }

        public int getIndex() {
            return this.mIndex;
        }

        public int getWidth() {
            return this.mWidth;
        }

        public int getHeight() {
            return this.mHeight;
        }

        public void render(Bitmap destination, Rect destClip, Matrix transform, int renderMode) {
            if (destination.getConfig() != Bitmap.Config.ARGB_8888) {
                throw new IllegalArgumentException("Unsupported pixel format");
            }
            if (destClip != null && (destClip.left < 0 || destClip.top < 0 || destClip.right > destination.getWidth() || destClip.bottom > destination.getHeight())) {
                throw new IllegalArgumentException("destBounds not in destination");
            }
            if (transform != null && !transform.isAffine()) {
                throw new IllegalArgumentException("transform not affine");
            }
            if (renderMode != 2 && renderMode != 1) {
                throw new IllegalArgumentException("Unsupported render mode");
            }
            if (renderMode == 2 && renderMode == 1) {
                throw new IllegalArgumentException("Only single render mode supported");
            }
            int contentLeft = destClip != null ? destClip.left : 0;
            int contentTop = destClip != null ? destClip.top : 0;
            int contentRight = destClip != null ? destClip.right : destination.getWidth();
            int contentBottom = destClip != null ? destClip.bottom : destination.getHeight();
            long transformPtr = transform != null ? transform.native_instance : 0L;
            PdfRenderer.nativeRenderPage(PdfRenderer.this.mNativeDocument, this.mNativePage, destination.mNativeBitmap, contentLeft, contentTop, contentRight, contentBottom, transformPtr, renderMode);
        }

        @Override
        public void close() {
            throwIfClosed();
            doClose();
        }

        protected void finalize() throws Throwable {
            try {
                this.mCloseGuard.warnIfOpen();
                if (this.mNativePage != 0) {
                    doClose();
                }
            } finally {
                super.finalize();
            }
        }

        private void doClose() {
            PdfRenderer.nativeClosePage(this.mNativePage);
            this.mNativePage = 0L;
            this.mCloseGuard.close();
            PdfRenderer.this.mCurrentPage = null;
        }

        private void throwIfClosed() {
            if (this.mNativePage == 0) {
                throw new IllegalStateException("Already closed");
            }
        }
    }
}
