package android.media;

import android.media.Image;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.view.Surface;
import dalvik.system.VMRuntime;
import java.lang.ref.WeakReference;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.NioUtils;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

public class ImageReader implements AutoCloseable {
    private static final int ACQUIRE_MAX_IMAGES = 2;
    private static final int ACQUIRE_NO_BUFS = 1;
    private static final int ACQUIRE_SUCCESS = 0;
    private int mEstimatedNativeAllocBytes;
    private final int mFormat;
    private final int mHeight;
    private boolean mIsReaderValid;
    private OnImageAvailableListener mListener;
    private ListenerHandler mListenerHandler;
    private final int mMaxImages;
    private long mNativeContext;
    private final int mNumPlanes;
    private final Surface mSurface;
    private final int mWidth;
    private final Object mListenerLock = new Object();
    private final Object mCloseLock = new Object();
    private List<Image> mAcquiredImages = new CopyOnWriteArrayList();

    public interface OnImageAvailableListener {
        void onImageAvailable(ImageReader imageReader);
    }

    private static native void nativeClassInit();

    private native synchronized void nativeClose();

    private native synchronized int nativeDetachImage(Image image);

    private native synchronized Surface nativeGetSurface();

    private native synchronized int nativeImageSetup(Image image);

    private native synchronized void nativeInit(Object obj, int i, int i2, int i3, int i4);

    private native synchronized void nativeReleaseImage(Image image);

    public static ImageReader newInstance(int width, int height, int format, int maxImages) {
        return new ImageReader(width, height, format, maxImages);
    }

    protected ImageReader(int width, int height, int format, int maxImages) {
        this.mIsReaderValid = false;
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
        this.mNumPlanes = ImageUtils.getNumPlanesForFormat(this.mFormat);
        nativeInit(new WeakReference(this), width, height, format, maxImages);
        this.mSurface = nativeGetSurface();
        this.mIsReaderValid = true;
        this.mEstimatedNativeAllocBytes = ImageUtils.getEstimatedNativeAllocBytes(width, height, format, 1);
        VMRuntime.getRuntime().registerNativeAllocation(this.mEstimatedNativeAllocBytes);
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
                    Image result = image;
                    return result;
                }
                image.close();
                image = next;
            } catch (Throwable th) {
                if (image != null) {
                    image.close();
                }
                throw th;
            }
        }
    }

    public Image acquireNextImageNoThrowISE() {
        SurfaceImage si = new SurfaceImage(this.mFormat);
        if (acquireNextSurfaceImage(si) == 0) {
            return si;
        }
        return null;
    }

    private int acquireNextSurfaceImage(SurfaceImage si) {
        int status;
        synchronized (this.mCloseLock) {
            status = 1;
            if (this.mIsReaderValid) {
                status = nativeImageSetup(si);
            }
            switch (status) {
                case 0:
                    si.mIsImageValid = true;
                    break;
                case 1:
                case 2:
                    break;
                default:
                    throw new AssertionError("Unknown nativeImageSetup return code " + status);
            }
            if (status == 0) {
                this.mAcquiredImages.add(si);
            }
        }
        return status;
    }

    public Image acquireNextImage() {
        SurfaceImage si = new SurfaceImage(this.mFormat);
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
        if (!si.mIsImageValid) {
            return;
        }
        if (si.getReader() != this || !this.mAcquiredImages.contains(i)) {
            throw new IllegalArgumentException("This image was not produced by this ImageReader");
        }
        si.clearSurfacePlanes();
        nativeReleaseImage(i);
        si.mIsImageValid = false;
        this.mAcquiredImages.remove(i);
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
        if (this.mSurface != null) {
            this.mSurface.release();
        }
        synchronized (this.mCloseLock) {
            this.mIsReaderValid = false;
            for (Image image : this.mAcquiredImages) {
                image.close();
            }
            this.mAcquiredImages.clear();
            nativeClose();
        }
        if (this.mEstimatedNativeAllocBytes <= 0) {
            return;
        }
        VMRuntime.getRuntime().registerNativeFree(this.mEstimatedNativeAllocBytes);
        this.mEstimatedNativeAllocBytes = 0;
    }

    protected void finalize() throws Throwable {
        try {
            close();
        } finally {
            super.finalize();
        }
    }

    void detachImage(Image image) {
        if (image == null) {
            throw new IllegalArgumentException("input image must not be null");
        }
        if (!isImageOwnedbyMe(image)) {
            throw new IllegalArgumentException("Trying to detach an image that is not owned by this ImageReader");
        }
        SurfaceImage si = (SurfaceImage) image;
        si.throwISEIfImageIsInvalid();
        if (si.isAttachable()) {
            throw new IllegalStateException("Image was already detached from this ImageReader");
        }
        nativeDetachImage(image);
        si.setDetached(true);
    }

    private boolean isImageOwnedbyMe(Image image) {
        if (!(image instanceof SurfaceImage)) {
            return false;
        }
        SurfaceImage si = (SurfaceImage) image;
        return si.getReader() == this;
    }

    private static void postEventFromNative(Object selfRef) {
        Handler handler;
        WeakReference<ImageReader> weakSelf = (WeakReference) selfRef;
        ImageReader ir = weakSelf.get();
        if (ir == null) {
            return;
        }
        synchronized (ir.mListenerLock) {
            handler = ir.mListenerHandler;
        }
        if (handler == null) {
            return;
        }
        handler.sendEmptyMessage(0);
    }

    private final class ListenerHandler extends Handler {
        public ListenerHandler(Looper looper) {
            super(looper, null, true);
        }

        @Override
        public void handleMessage(Message msg) {
            OnImageAvailableListener listener;
            boolean isReaderValid;
            synchronized (ImageReader.this.mListenerLock) {
                listener = ImageReader.this.mListener;
            }
            synchronized (ImageReader.this.mCloseLock) {
                isReaderValid = ImageReader.this.mIsReaderValid;
            }
            if (listener == null || !isReaderValid) {
                return;
            }
            listener.onImageAvailable(ImageReader.this);
        }
    }

    private class SurfaceImage extends Image {
        private int mFormat;
        private AtomicBoolean mIsDetached = new AtomicBoolean(false);
        private long mNativeBuffer;
        private SurfacePlane[] mPlanes;
        private long mTimestamp;

        private native synchronized SurfacePlane[] nativeCreatePlanes(int i, int i2);

        private native synchronized int nativeGetFormat(int i);

        private native synchronized int nativeGetHeight();

        private native synchronized int nativeGetWidth();

        public SurfaceImage(int format) {
            this.mFormat = 0;
            this.mFormat = format;
        }

        @Override
        public void close() {
            ImageReader.this.releaseImage(this);
        }

        public ImageReader getReader() {
            return ImageReader.this;
        }

        @Override
        public int getFormat() {
            throwISEIfImageIsInvalid();
            int readerFormat = ImageReader.this.getImageFormat();
            if (readerFormat != 34) {
                readerFormat = nativeGetFormat(readerFormat);
            }
            this.mFormat = readerFormat;
            return this.mFormat;
        }

        @Override
        public int getWidth() {
            throwISEIfImageIsInvalid();
            switch (getFormat()) {
                case 36:
                case 256:
                case 257:
                    int width = ImageReader.this.getWidth();
                    return width;
                default:
                    int width2 = nativeGetWidth();
                    return width2;
            }
        }

        @Override
        public int getHeight() {
            throwISEIfImageIsInvalid();
            switch (getFormat()) {
                case 36:
                case 256:
                case 257:
                    int height = ImageReader.this.getHeight();
                    return height;
                default:
                    int height2 = nativeGetHeight();
                    return height2;
            }
        }

        @Override
        public long getTimestamp() {
            throwISEIfImageIsInvalid();
            return this.mTimestamp;
        }

        @Override
        public void setTimestamp(long timestampNs) {
            throwISEIfImageIsInvalid();
            this.mTimestamp = timestampNs;
        }

        @Override
        public Image.Plane[] getPlanes() {
            throwISEIfImageIsInvalid();
            if (this.mPlanes == null) {
                this.mPlanes = nativeCreatePlanes(ImageReader.this.mNumPlanes, ImageReader.this.mFormat);
            }
            return (Image.Plane[]) this.mPlanes.clone();
        }

        protected final void finalize() throws Throwable {
            try {
                close();
            } finally {
                super.finalize();
            }
        }

        @Override
        boolean isAttachable() {
            throwISEIfImageIsInvalid();
            return this.mIsDetached.get();
        }

        @Override
        ImageReader getOwner() {
            throwISEIfImageIsInvalid();
            return ImageReader.this;
        }

        @Override
        long getNativeContext() {
            throwISEIfImageIsInvalid();
            return this.mNativeBuffer;
        }

        private void setDetached(boolean detached) {
            throwISEIfImageIsInvalid();
            this.mIsDetached.getAndSet(detached);
        }

        private void clearSurfacePlanes() {
            if (!this.mIsImageValid || this.mPlanes == null) {
                return;
            }
            for (int i = 0; i < this.mPlanes.length; i++) {
                if (this.mPlanes[i] != null) {
                    this.mPlanes[i].clearBuffer();
                    this.mPlanes[i] = null;
                }
            }
        }

        private class SurfacePlane extends Image.Plane {
            private ByteBuffer mBuffer;
            private final int mPixelStride;
            private final int mRowStride;

            private SurfacePlane(int rowStride, int pixelStride, ByteBuffer buffer) {
                this.mRowStride = rowStride;
                this.mPixelStride = pixelStride;
                this.mBuffer = buffer;
                this.mBuffer.order(ByteOrder.nativeOrder());
            }

            @Override
            public ByteBuffer getBuffer() {
                SurfaceImage.this.throwISEIfImageIsInvalid();
                return this.mBuffer;
            }

            @Override
            public int getPixelStride() {
                SurfaceImage.this.throwISEIfImageIsInvalid();
                if (ImageReader.this.mFormat == 36) {
                    throw new UnsupportedOperationException("getPixelStride is not supported for RAW_PRIVATE plane");
                }
                return this.mPixelStride;
            }

            @Override
            public int getRowStride() {
                SurfaceImage.this.throwISEIfImageIsInvalid();
                if (ImageReader.this.mFormat == 36) {
                    throw new UnsupportedOperationException("getRowStride is not supported for RAW_PRIVATE plane");
                }
                return this.mRowStride;
            }

            private void clearBuffer() {
                if (this.mBuffer == null) {
                    return;
                }
                if (this.mBuffer.isDirect()) {
                    NioUtils.freeDirectBuffer(this.mBuffer);
                }
                this.mBuffer = null;
            }
        }
    }

    static {
        System.loadLibrary("media_jni");
        nativeClassInit();
    }
}
