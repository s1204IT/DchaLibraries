package android.media;

import android.graphics.Rect;
import android.hardware.camera2.utils.SurfaceUtils;
import android.media.Image;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Size;
import android.view.Surface;
import dalvik.system.VMRuntime;
import java.lang.ref.WeakReference;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.NioUtils;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class ImageWriter implements AutoCloseable {
    private int mEstimatedNativeAllocBytes;
    private OnImageReleasedListener mListener;
    private ListenerHandler mListenerHandler;
    private final int mMaxImages;
    private long mNativeContext;
    private int mWriterFormat;
    private final Object mListenerLock = new Object();
    private List<Image> mDequeuedImages = new CopyOnWriteArrayList();

    public interface OnImageReleasedListener {
        void onImageReleased(ImageWriter imageWriter);
    }

    private native synchronized void cancelImage(long j, Image image);

    private native synchronized int nativeAttachAndQueueImage(long j, long j2, int i, long j3, int i2, int i3, int i4, int i5);

    private static native void nativeClassInit();

    private native synchronized void nativeClose(long j);

    private native synchronized void nativeDequeueInputImage(long j, Image image);

    private native synchronized long nativeInit(Object obj, Surface surface, int i);

    private native synchronized void nativeQueueInputImage(long j, Image image, long j2, int i, int i2, int i3, int i4);

    public static ImageWriter newInstance(Surface surface, int maxImages) {
        return new ImageWriter(surface, maxImages);
    }

    protected ImageWriter(Surface surface, int maxImages) {
        if (surface == null || maxImages < 1) {
            throw new IllegalArgumentException("Illegal input argument: surface " + surface + ", maxImages: " + maxImages);
        }
        this.mMaxImages = maxImages;
        this.mNativeContext = nativeInit(new WeakReference(this), surface, maxImages);
        Size surfSize = SurfaceUtils.getSurfaceSize(surface);
        int format = SurfaceUtils.getSurfaceFormat(surface);
        this.mEstimatedNativeAllocBytes = ImageUtils.getEstimatedNativeAllocBytes(surfSize.getWidth(), surfSize.getHeight(), format, 1);
        VMRuntime.getRuntime().registerNativeAllocation(this.mEstimatedNativeAllocBytes);
    }

    public int getMaxImages() {
        return this.mMaxImages;
    }

    public Image dequeueInputImage() {
        if (this.mWriterFormat == 34) {
            throw new IllegalStateException("PRIVATE format ImageWriter doesn't support this operation since the images are inaccessible to the application!");
        }
        if (this.mDequeuedImages.size() >= this.mMaxImages) {
            throw new IllegalStateException("Already dequeued max number of Images " + this.mMaxImages);
        }
        WriterSurfaceImage newImage = new WriterSurfaceImage(this);
        nativeDequeueInputImage(this.mNativeContext, newImage);
        this.mDequeuedImages.add(newImage);
        newImage.mIsImageValid = true;
        return newImage;
    }

    public void queueInputImage(Image image) {
        if (image == null) {
            throw new IllegalArgumentException("image shouldn't be null");
        }
        boolean ownedByMe = isImageOwnedByMe(image);
        if (ownedByMe && !((WriterSurfaceImage) image).mIsImageValid) {
            throw new IllegalStateException("Image from ImageWriter is invalid");
        }
        if (!ownedByMe) {
            if (!(image.getOwner() instanceof ImageReader)) {
                throw new IllegalArgumentException("Only images from ImageReader can be queued to ImageWriter, other image source is not supported yet!");
            }
            ImageReader prevOwner = (ImageReader) image.getOwner();
            if (image.getFormat() == 34) {
                prevOwner.detachImage(image);
                attachAndQueueInputImage(image);
                image.close();
                return;
            } else {
                Image inputImage = dequeueInputImage();
                inputImage.setTimestamp(image.getTimestamp());
                inputImage.setCropRect(image.getCropRect());
                ImageUtils.imageCopy(image, inputImage);
                image.close();
                image = inputImage;
                ownedByMe = true;
            }
        }
        Rect crop = image.getCropRect();
        nativeQueueInputImage(this.mNativeContext, image, image.getTimestamp(), crop.left, crop.top, crop.right, crop.bottom);
        if (!ownedByMe) {
            return;
        }
        this.mDequeuedImages.remove(image);
        WriterSurfaceImage wi = (WriterSurfaceImage) image;
        wi.clearSurfacePlanes();
        wi.mIsImageValid = false;
    }

    public int getFormat() {
        return this.mWriterFormat;
    }

    public void setOnImageReleasedListener(OnImageReleasedListener listener, Handler handler) {
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
        setOnImageReleasedListener(null, null);
        for (Image image : this.mDequeuedImages) {
            image.close();
        }
        this.mDequeuedImages.clear();
        nativeClose(this.mNativeContext);
        this.mNativeContext = 0L;
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

    private void attachAndQueueInputImage(Image image) {
        if (image == null) {
            throw new IllegalArgumentException("image shouldn't be null");
        }
        if (isImageOwnedByMe(image)) {
            throw new IllegalArgumentException("Can not attach an image that is owned ImageWriter already");
        }
        if (!image.isAttachable()) {
            throw new IllegalStateException("Image was not detached from last owner, or image  is not detachable");
        }
        Rect crop = image.getCropRect();
        nativeAttachAndQueueImage(this.mNativeContext, image.getNativeContext(), image.getFormat(), image.getTimestamp(), crop.left, crop.top, crop.right, crop.bottom);
    }

    private final class ListenerHandler extends Handler {
        public ListenerHandler(Looper looper) {
            super(looper, null, true);
        }

        @Override
        public void handleMessage(Message msg) {
            OnImageReleasedListener listener;
            synchronized (ImageWriter.this.mListenerLock) {
                listener = ImageWriter.this.mListener;
            }
            if (listener == null) {
                return;
            }
            listener.onImageReleased(ImageWriter.this);
        }
    }

    private static void postEventFromNative(Object selfRef) {
        Handler handler;
        WeakReference<ImageWriter> weakSelf = (WeakReference) selfRef;
        ImageWriter iw = weakSelf.get();
        if (iw == null) {
            return;
        }
        synchronized (iw.mListenerLock) {
            handler = iw.mListenerHandler;
        }
        if (handler == null) {
            return;
        }
        handler.sendEmptyMessage(0);
    }

    private void abortImage(Image image) {
        if (image == null) {
            throw new IllegalArgumentException("image shouldn't be null");
        }
        if (!this.mDequeuedImages.contains(image)) {
            throw new IllegalStateException("It is illegal to abort some image that is not dequeued yet");
        }
        WriterSurfaceImage wi = (WriterSurfaceImage) image;
        if (!wi.mIsImageValid) {
            return;
        }
        cancelImage(this.mNativeContext, image);
        this.mDequeuedImages.remove(image);
        wi.clearSurfacePlanes();
        wi.mIsImageValid = false;
    }

    private boolean isImageOwnedByMe(Image image) {
        if (!(image instanceof WriterSurfaceImage)) {
            return false;
        }
        WriterSurfaceImage wi = (WriterSurfaceImage) image;
        return wi.getOwner() == this;
    }

    private static class WriterSurfaceImage extends Image {
        private long mNativeBuffer;
        private ImageWriter mOwner;
        private SurfacePlane[] mPlanes;
        private int mNativeFenceFd = -1;
        private int mHeight = -1;
        private int mWidth = -1;
        private int mFormat = -1;
        private final long DEFAULT_TIMESTAMP = Long.MIN_VALUE;
        private long mTimestamp = Long.MIN_VALUE;

        private native synchronized SurfacePlane[] nativeCreatePlanes(int i, int i2);

        private native synchronized int nativeGetFormat();

        private native synchronized int nativeGetHeight();

        private native synchronized int nativeGetWidth();

        public WriterSurfaceImage(ImageWriter writer) {
            this.mOwner = writer;
        }

        @Override
        public int getFormat() {
            throwISEIfImageIsInvalid();
            if (this.mFormat == -1) {
                this.mFormat = nativeGetFormat();
            }
            return this.mFormat;
        }

        @Override
        public int getWidth() {
            throwISEIfImageIsInvalid();
            if (this.mWidth == -1) {
                this.mWidth = nativeGetWidth();
            }
            return this.mWidth;
        }

        @Override
        public int getHeight() {
            throwISEIfImageIsInvalid();
            if (this.mHeight == -1) {
                this.mHeight = nativeGetHeight();
            }
            return this.mHeight;
        }

        @Override
        public long getTimestamp() {
            throwISEIfImageIsInvalid();
            return this.mTimestamp;
        }

        @Override
        public void setTimestamp(long timestamp) {
            throwISEIfImageIsInvalid();
            this.mTimestamp = timestamp;
        }

        @Override
        public Image.Plane[] getPlanes() {
            throwISEIfImageIsInvalid();
            if (this.mPlanes == null) {
                int numPlanes = ImageUtils.getNumPlanesForFormat(getFormat());
                this.mPlanes = nativeCreatePlanes(numPlanes, getOwner().getFormat());
            }
            return (Image.Plane[]) this.mPlanes.clone();
        }

        @Override
        boolean isAttachable() {
            throwISEIfImageIsInvalid();
            return false;
        }

        @Override
        ImageWriter getOwner() {
            throwISEIfImageIsInvalid();
            return this.mOwner;
        }

        @Override
        long getNativeContext() {
            throwISEIfImageIsInvalid();
            return this.mNativeBuffer;
        }

        @Override
        public void close() {
            if (!this.mIsImageValid) {
                return;
            }
            getOwner().abortImage(this);
        }

        protected final void finalize() throws Throwable {
            try {
                close();
            } finally {
                super.finalize();
            }
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
            public int getRowStride() {
                WriterSurfaceImage.this.throwISEIfImageIsInvalid();
                return this.mRowStride;
            }

            @Override
            public int getPixelStride() {
                WriterSurfaceImage.this.throwISEIfImageIsInvalid();
                return this.mPixelStride;
            }

            @Override
            public ByteBuffer getBuffer() {
                WriterSurfaceImage.this.throwISEIfImageIsInvalid();
                return this.mBuffer;
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
