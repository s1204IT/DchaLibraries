package android.graphics;

import java.io.InputStream;
import java.io.OutputStream;

public class Picture {
    private static final int WORKING_STREAM_STORAGE = 16384;
    private long mNativePicture;
    private PictureCanvas mRecordingCanvas;
    private boolean mRequiresHwAcceleration;

    private static native long nativeBeginRecording(long j, int i, int i2);

    private static native long nativeConstructor(long j);

    private static native long nativeCreateFromStream(InputStream inputStream, byte[] bArr);

    private static native void nativeDestructor(long j);

    private static native void nativeDraw(long j, long j2);

    private static native void nativeEndRecording(long j);

    private static native int nativeGetHeight(long j);

    private static native int nativeGetWidth(long j);

    private static native boolean nativeWriteToStream(long j, OutputStream outputStream, byte[] bArr);

    public Picture() {
        this(nativeConstructor(0L));
    }

    public Picture(Picture picture) {
        this(nativeConstructor(picture != null ? picture.mNativePicture : 0L));
    }

    private Picture(long j) {
        if (j == 0) {
            throw new RuntimeException();
        }
        this.mNativePicture = j;
    }

    protected void finalize() throws Throwable {
        try {
            nativeDestructor(this.mNativePicture);
            this.mNativePicture = 0L;
        } finally {
            super.finalize();
        }
    }

    public Canvas beginRecording(int i, int i2) {
        if (this.mRecordingCanvas != null) {
            throw new IllegalStateException("Picture already recording, must call #endRecording()");
        }
        this.mRecordingCanvas = new PictureCanvas(this, nativeBeginRecording(this.mNativePicture, i, i2));
        this.mRequiresHwAcceleration = false;
        return this.mRecordingCanvas;
    }

    public void endRecording() {
        if (this.mRecordingCanvas != null) {
            this.mRequiresHwAcceleration = this.mRecordingCanvas.mHoldsHwBitmap;
            this.mRecordingCanvas = null;
            nativeEndRecording(this.mNativePicture);
        }
    }

    public int getWidth() {
        return nativeGetWidth(this.mNativePicture);
    }

    public int getHeight() {
        return nativeGetHeight(this.mNativePicture);
    }

    public boolean requiresHardwareAcceleration() {
        return this.mRequiresHwAcceleration;
    }

    public void draw(Canvas canvas) {
        if (this.mRecordingCanvas != null) {
            endRecording();
        }
        if (this.mRequiresHwAcceleration && !canvas.isHardwareAccelerated()) {
            canvas.onHwBitmapInSwMode();
        }
        nativeDraw(canvas.getNativeCanvasWrapper(), this.mNativePicture);
    }

    @Deprecated
    public static Picture createFromStream(InputStream inputStream) {
        return new Picture(nativeCreateFromStream(inputStream, new byte[16384]));
    }

    @Deprecated
    public void writeToStream(OutputStream outputStream) {
        if (outputStream == null) {
            throw new NullPointerException();
        }
        if (!nativeWriteToStream(this.mNativePicture, outputStream, new byte[16384])) {
            throw new RuntimeException();
        }
    }

    private static class PictureCanvas extends Canvas {
        boolean mHoldsHwBitmap;
        private final Picture mPicture;

        public PictureCanvas(Picture picture, long j) {
            super(j);
            this.mPicture = picture;
            this.mDensity = 0;
        }

        @Override
        public void setBitmap(Bitmap bitmap) {
            throw new RuntimeException("Cannot call setBitmap on a picture canvas");
        }

        @Override
        public void drawPicture(Picture picture) {
            if (this.mPicture == picture) {
                throw new RuntimeException("Cannot draw a picture into its recording canvas");
            }
            super.drawPicture(picture);
        }

        @Override
        protected void onHwBitmapInSwMode() {
            this.mHoldsHwBitmap = true;
        }
    }
}
