package android.media;

import android.graphics.ImageFormat;
import android.media.Image;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.view.Surface;
import java.lang.ref.WeakReference;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.NioUtils;

public class ImageReader implements AutoCloseable {
    private static final int ACQUIRE_MAX_IMAGES = 2;
    private static final int ACQUIRE_NO_BUFS = 1;
    private static final int ACQUIRE_SUCCESS = 0;
    private final int mFormat;
    private final int mHeight;
    private OnImageAvailableListener mListener;
    private ListenerHandler mListenerHandler;
    private final Object mListenerLock = new Object();
    private final int mMaxImages;
    private long mNativeContext;
    private final int mNumPlanes;
    private final Surface mSurface;
    private final int mWidth;

    public interface OnImageAvailableListener {
        void onImageAvailable(ImageReader imageReader);
    }

    private static native void nativeClassInit();

    private native synchronized void nativeClose();

    private native synchronized Surface nativeGetSurface();

    private native synchronized int nativeImageSetup(Image image);

    private native synchronized void nativeInit(Object obj, int i, int i2, int i3, int i4);

    private native synchronized void nativeReleaseImage(Image image);

    public static ImageReader newInstance(int width, int height, int format, int maxImages) {
        return new ImageReader(width, height, format, maxImages);
    }

    protected ImageReader(int width, int height, int format, int maxImages) {
        this.mWidth = width;
        this.mHeight = height;
        this.mFormat = format;
        this.mMaxImages = maxImages;
        if (width < 1 || height < 1) {
            throw new IllegalArgumentException("The image dimensions must be positive");
        }
        if (this.mMaxImages < 1) {
            throw new IllegalArgumentException("Maximum outstanding image count must be at least 1");
        }
        if (format == 17) {
            throw new IllegalArgumentException("NV21 format is not supported");
        }
        this.mNumPlanes = getNumPlanesFromFormat();
        nativeInit(new WeakReference(this), width, height, format, maxImages);
        this.mSurface = nativeGetSurface();
    }

    public int getWidth() {
        return this.mWidth;
    }

    public int getHeight() {
        return this.mHeight;
    }

    public int getImageFormat() {
        return this.mFormat;
    }

    public int getMaxImages() {
        return this.mMaxImages;
    }

    public Surface getSurface() {
        return this.mSurface;
    }

    public Image acquireLatestImage() {
        Image image = acquireNextImage();
        if (image == null) {
            return null;
        }
        while (true) {
            try {
                Image next = acquireNextImageNoThrowISE();
                if (next == null) {
                    break;
                }
                image.close();
                image = next;
            } finally {
                if (image != null) {
                    image.close();
                }
            }
        }
        Image image2 = image;
        image = null;
    }

    public Image acquireNextImageNoThrowISE() {
        SurfaceImage si = new SurfaceImage();
        if (acquireNextSurfaceImage(si) == 0) {
            return si;
        }
        return null;
    }

    private int acquireNextSurfaceImage(SurfaceImage si) {
        int status = nativeImageSetup(si);
        switch (status) {
            case 0:
                si.createSurfacePlanes();
                si.setImageValid(true);
            case 1:
            case 2:
                return status;
            default:
                throw new AssertionError("Unknown nativeImageSetup return code " + status);
        }
    }

    public Image acquireNextImage() {
        SurfaceImage si = new SurfaceImage();
        int status = acquireNextSurfaceImage(si);
        switch (status) {
            case 0:
                return si;
            case 1:
                return null;
            case 2:
                throw new IllegalStateException(String.format("maxImages (%d) has already been acquired, call #close before acquiring more.", Integer.valueOf(this.mMaxImages)));
            default:
                throw new AssertionError("Unknown nativeImageSetup return code " + status);
        }
    }

    private void releaseImage(Image i) {
        if (!(i instanceof SurfaceImage)) {
            throw new IllegalArgumentException("This image was not produced by an ImageReader");
        }
        SurfaceImage si = (SurfaceImage) i;
        if (si.getReader() != this) {
            throw new IllegalArgumentException("This image was not produced by this ImageReader");
        }
        si.clearSurfacePlanes();
        nativeReleaseImage(i);
        si.setImageValid(false);
    }

    public void setOnImageAvailableListener(OnImageAvailableListener listener, Handler handler) {
        synchronized (this.mListenerLock) {
            if (listener != null) {
                Looper looper = handler != null ? handler.getLooper() : Looper.myLooper();
                if (looper == null) {
                    throw new IllegalArgumentException("handler is null but the current thread is not a looper");
                }
                if (this.mListenerHandler == null || this.mListenerHandler.getLooper() != looper) {
                    this.mListenerHandler = new ListenerHandler(looper);
                }
                this.mListener = listener;
            } else {
                this.mListener = null;
                this.mListenerHandler = null;
            }
        }
    }

    @Override
    public void close() {
        setOnImageAvailableListener(null, null);
        nativeClose();
    }

    protected void finalize() throws Throwable {
        try {
            close();
        } finally {
            super.finalize();
        }
    }

    private int getNumPlanesFromFormat() {
        switch (this.mFormat) {
            case 1:
            case 2:
            case 3:
            case 4:
            case 20:
            case 32:
            case 37:
            case 256:
            case ImageFormat.Y8:
            case ImageFormat.Y16:
                return 1;
            case 16:
                return 2;
            case 17:
            case 35:
            case ImageFormat.YV12:
                return 3;
            default:
                throw new UnsupportedOperationException(String.format("Invalid format specified %d", Integer.valueOf(this.mFormat)));
        }
    }

    private static void postEventFromNative(Object selfRef) {
        Handler handler;
        WeakReference<ImageReader> weakSelf = (WeakReference) selfRef;
        ImageReader ir = weakSelf.get();
        if (ir != null) {
            synchronized (ir.mListenerLock) {
                handler = ir.mListenerHandler;
            }
            if (handler != null) {
                handler.sendEmptyMessage(0);
            }
        }
    }

    private final class ListenerHandler extends Handler {
        public ListenerHandler(Looper looper) {
            super(looper, null, true);
        }

        @Override
        public void handleMessage(Message msg) {
            OnImageAvailableListener listener;
            synchronized (ImageReader.this.mListenerLock) {
                listener = ImageReader.this.mListener;
            }
            if (listener != null) {
                listener.onImageAvailable(ImageReader.this);
            }
        }
    }

    private class SurfaceImage extends Image {
        private long mLockedBuffer;
        private SurfacePlane[] mPlanes;
        private long mTimestamp;
        private int mHeight = -1;
        private int mWidth = -1;
        private boolean mIsImageValid = false;

        private native synchronized SurfacePlane nativeCreatePlane(int i, int i2);

        private native synchronized int nativeGetHeight();

        private native synchronized int nativeGetWidth();

        private native synchronized ByteBuffer nativeImageGetBuffer(int i, int i2);

        public SurfaceImage() {
        }

        @Override
        public void close() {
            if (this.mIsImageValid) {
                ImageReader.this.releaseImage(this);
            }
        }

        public ImageReader getReader() {
            return ImageReader.this;
        }

        @Override
        public int getFormat() {
            if (this.mIsImageValid) {
                return ImageReader.this.mFormat;
            }
            throw new IllegalStateException("Image is already released");
        }

        @Override
        public int getWidth() {
            if (this.mIsImageValid) {
                if (this.mWidth == -1) {
                    this.mWidth = getFormat() == 256 ? ImageReader.this.getWidth() : nativeGetWidth();
                }
                return this.mWidth;
            }
            throw new IllegalStateException("Image is already released");
        }

        @Override
        public int getHeight() {
            if (this.mIsImageValid) {
                if (this.mHeight == -1) {
                    this.mHeight = getFormat() == 256 ? ImageReader.this.getHeight() : nativeGetHeight();
                }
                return this.mHeight;
            }
            throw new IllegalStateException("Image is already released");
        }

        @Override
        public long getTimestamp() {
            if (this.mIsImageValid) {
                return this.mTimestamp;
            }
            throw new IllegalStateException("Image is already released");
        }

        @Override
        public Image.Plane[] getPlanes() {
            if (this.mIsImageValid) {
                return (Image.Plane[]) this.mPlanes.clone();
            }
            throw new IllegalStateException("Image is already released");
        }

        protected final void finalize() throws Throwable {
            try {
                close();
            } finally {
                super.finalize();
            }
        }

        private void setImageValid(boolean isValid) {
            this.mIsImageValid = isValid;
        }

        private boolean isImageValid() {
            return this.mIsImageValid;
        }

        private void clearSurfacePlanes() {
            if (this.mIsImageValid) {
                for (int i = 0; i < this.mPlanes.length; i++) {
                    if (this.mPlanes[i] != null) {
                        this.mPlanes[i].clearBuffer();
                        this.mPlanes[i] = null;
                    }
                }
            }
        }

        private void createSurfacePlanes() {
            this.mPlanes = new SurfacePlane[ImageReader.this.mNumPlanes];
            for (int i = 0; i < ImageReader.this.mNumPlanes; i++) {
                this.mPlanes[i] = nativeCreatePlane(i, ImageReader.this.mFormat);
            }
        }

        private class SurfacePlane extends Image.Plane {
            private ByteBuffer mBuffer;
            private final int mIndex;
            private final int mPixelStride;
            private final int mRowStride;

            private SurfacePlane(int index, int rowStride, int pixelStride) {
                this.mIndex = index;
                this.mRowStride = rowStride;
                this.mPixelStride = pixelStride;
            }

            @Override
            public ByteBuffer getBuffer() {
                if (!SurfaceImage.this.isImageValid()) {
                    throw new IllegalStateException("Image is already released");
                }
                if (this.mBuffer == null) {
                    this.mBuffer = SurfaceImage.this.nativeImageGetBuffer(this.mIndex, ImageReader.this.mFormat);
                    return this.mBuffer.order(ByteOrder.nativeOrder());
                }
                return this.mBuffer;
            }

            @Override
            public int getPixelStride() {
                if (SurfaceImage.this.isImageValid()) {
                    return this.mPixelStride;
                }
                throw new IllegalStateException("Image is already released");
            }

            @Override
            public int getRowStride() {
                if (SurfaceImage.this.isImageValid()) {
                    return this.mRowStride;
                }
                throw new IllegalStateException("Image is already released");
            }

            private void clearBuffer() {
                if (this.mBuffer != null) {
                    if (this.mBuffer.isDirect()) {
                        NioUtils.freeDirectBuffer(this.mBuffer);
                    }
                    this.mBuffer = null;
                }
            }
        }
    }

    static {
        System.loadLibrary("media_jni");
        nativeClassInit();
    }
}
