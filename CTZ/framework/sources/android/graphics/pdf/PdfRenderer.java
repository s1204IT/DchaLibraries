package android.graphics.pdf;

import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.Point;
import android.graphics.Rect;
import android.os.ParcelFileDescriptor;
import android.system.ErrnoException;
import android.system.Os;
import android.system.OsConstants;
import com.android.internal.util.Preconditions;
import dalvik.system.CloseGuard;
import java.io.IOException;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import libcore.io.IoUtils;

public final class PdfRenderer implements AutoCloseable {
    static final Object sPdfiumLock = new Object();
    private Page mCurrentPage;
    private ParcelFileDescriptor mInput;
    private long mNativeDocument;
    private final int mPageCount;
    private final CloseGuard mCloseGuard = CloseGuard.get();
    private final Point mTempPoint = new Point();

    @Retention(RetentionPolicy.SOURCE)
    public @interface RenderMode {
    }

    private static native void nativeClose(long j);

    private static native void nativeClosePage(long j);

    private static native long nativeCreate(int i, long j);

    private static native int nativeGetPageCount(long j);

    private static native long nativeOpenPageAndGetSize(long j, int i, Point point);

    private static native void nativeRenderPage(long j, long j2, Bitmap bitmap, int i, int i2, int i3, int i4, long j3, int i5);

    private static native boolean nativeScaleForPrinting(long j);

    public PdfRenderer(ParcelFileDescriptor parcelFileDescriptor) throws IOException {
        if (parcelFileDescriptor == null) {
            throw new NullPointerException("input cannot be null");
        }
        try {
            Os.lseek(parcelFileDescriptor.getFileDescriptor(), 0L, OsConstants.SEEK_SET);
            long j = Os.fstat(parcelFileDescriptor.getFileDescriptor()).st_size;
            this.mInput = parcelFileDescriptor;
            synchronized (sPdfiumLock) {
                this.mNativeDocument = nativeCreate(this.mInput.getFd(), j);
                try {
                    this.mPageCount = nativeGetPageCount(this.mNativeDocument);
                } catch (Throwable th) {
                    nativeClose(this.mNativeDocument);
                    this.mNativeDocument = 0L;
                    throw th;
                }
            }
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
        boolean zNativeScaleForPrinting;
        throwIfClosed();
        synchronized (sPdfiumLock) {
            zNativeScaleForPrinting = nativeScaleForPrinting(this.mNativeDocument);
        }
        return zNativeScaleForPrinting;
    }

    public Page openPage(int i) {
        throwIfClosed();
        throwIfPageOpened();
        throwIfPageNotInDocument(i);
        this.mCurrentPage = new Page(i);
        return this.mCurrentPage;
    }

    protected void finalize() throws Throwable {
        try {
            if (this.mCloseGuard != null) {
                this.mCloseGuard.warnIfOpen();
            }
            doClose();
        } finally {
            super.finalize();
        }
    }

    private void doClose() {
        if (this.mCurrentPage != null) {
            this.mCurrentPage.close();
            this.mCurrentPage = null;
        }
        if (this.mNativeDocument != 0) {
            synchronized (sPdfiumLock) {
                nativeClose(this.mNativeDocument);
            }
            this.mNativeDocument = 0L;
        }
        if (this.mInput != null) {
            IoUtils.closeQuietly(this.mInput);
            this.mInput = null;
        }
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

    private void throwIfPageNotInDocument(int i) {
        if (i < 0 || i >= this.mPageCount) {
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

        private Page(int i) {
            this.mCloseGuard = CloseGuard.get();
            Point point = PdfRenderer.this.mTempPoint;
            synchronized (PdfRenderer.sPdfiumLock) {
                this.mNativePage = PdfRenderer.nativeOpenPageAndGetSize(PdfRenderer.this.mNativeDocument, i, point);
            }
            this.mIndex = i;
            this.mWidth = point.x;
            this.mHeight = point.y;
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

        public void render(Bitmap bitmap, Rect rect, Matrix matrix, int i) {
            Matrix matrix2;
            if (this.mNativePage == 0) {
                throw new NullPointerException();
            }
            Bitmap bitmap2 = (Bitmap) Preconditions.checkNotNull(bitmap, "bitmap null");
            if (bitmap2.getConfig() != Bitmap.Config.ARGB_8888) {
                throw new IllegalArgumentException("Unsupported pixel format");
            }
            if (rect != null && (rect.left < 0 || rect.top < 0 || rect.right > bitmap2.getWidth() || rect.bottom > bitmap2.getHeight())) {
                throw new IllegalArgumentException("destBounds not in destination");
            }
            if (matrix == null || matrix.isAffine()) {
                if (i != 2 && i != 1) {
                    throw new IllegalArgumentException("Unsupported render mode");
                }
                if (i == 2 && i == 1) {
                    throw new IllegalArgumentException("Only single render mode supported");
                }
                int i2 = rect != null ? rect.left : 0;
                int i3 = rect != null ? rect.top : 0;
                int width = rect != null ? rect.right : bitmap2.getWidth();
                int height = rect != null ? rect.bottom : bitmap2.getHeight();
                if (matrix == null) {
                    Matrix matrix3 = new Matrix();
                    matrix3.postScale((width - i2) / getWidth(), (height - i3) / getHeight());
                    matrix3.postTranslate(i2, i3);
                    matrix2 = matrix3;
                } else {
                    matrix2 = matrix;
                }
                long j = matrix2.native_instance;
                synchronized (PdfRenderer.sPdfiumLock) {
                    PdfRenderer.nativeRenderPage(PdfRenderer.this.mNativeDocument, this.mNativePage, bitmap2, i2, i3, width, height, j, i);
                }
                return;
            }
            throw new IllegalArgumentException("transform not affine");
        }

        @Override
        public void close() {
            throwIfClosed();
            doClose();
        }

        protected void finalize() throws Throwable {
            try {
                if (this.mCloseGuard != null) {
                    this.mCloseGuard.warnIfOpen();
                }
                doClose();
            } finally {
                super.finalize();
            }
        }

        private void doClose() {
            if (this.mNativePage != 0) {
                synchronized (PdfRenderer.sPdfiumLock) {
                    PdfRenderer.nativeClosePage(this.mNativePage);
                }
                this.mNativePage = 0L;
            }
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
