package android.media;

import android.graphics.ImageFormat;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.hardware.HardwareBuffer;
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
import java.util.Iterator;
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

    private native synchronized int nativeAttachAndQueueImage(long j, long j2, int i, long j3, int i2, int i3, int i4, int i5, int i6, int i7);

    private static native void nativeClassInit();

    private native synchronized void nativeClose(long j);

    private native synchronized void nativeDequeueInputImage(long j, Image image);

    private native synchronized long nativeInit(Object obj, Surface surface, int i, int i2);

    private native synchronized void nativeQueueInputImage(long j, Image image, long j2, int i, int i2, int i3, int i4, int i5, int i6);

    public static ImageWriter newInstance(Surface surface, int i) {
        return new ImageWriter(surface, i, 0);
    }

    public static ImageWriter newInstance(Surface surface, int i, int i2) {
        if (!ImageFormat.isPublicFormat(i2) && !PixelFormat.isPublicFormat(i2)) {
            throw new IllegalArgumentException("Invalid format is specified: " + i2);
        }
        return new ImageWriter(surface, i, i2);
    }

    protected ImageWriter(Surface surface, int i, int i2) {
        if (surface == null || i < 1) {
            throw new IllegalArgumentException("Illegal input argument: surface " + surface + ", maxImages: " + i);
        }
        this.mMaxImages = i;
        i2 = i2 == 0 ? SurfaceUtils.getSurfaceFormat(surface) : i2;
        this.mNativeContext = nativeInit(new WeakReference(this), surface, i, i2);
        Size surfaceSize = SurfaceUtils.getSurfaceSize(surface);
        this.mEstimatedNativeAllocBytes = ImageUtils.getEstimatedNativeAllocBytes(surfaceSize.getWidth(), surfaceSize.getHeight(), i2, 1);
        VMRuntime.getRuntime().registerNativeAllocation(this.mEstimatedNativeAllocBytes);
    }

    public int getMaxImages() {
        return this.mMaxImages;
    }

    public Image dequeueInputImage() {
        if (this.mDequeuedImages.size() >= this.mMaxImages) {
            throw new IllegalStateException("Already dequeued max number of Images " + this.mMaxImages);
        }
        WriterSurfaceImage writerSurfaceImage = new WriterSurfaceImage(this);
        nativeDequeueInputImage(this.mNativeContext, writerSurfaceImage);
        this.mDequeuedImages.add(writerSurfaceImage);
        writerSurfaceImage.mIsImageValid = true;
        return writerSurfaceImage;
    }

    public void queueInputImage(Image image) {
        if (image == null) {
            throw new IllegalArgumentException("image shouldn't be null");
        }
        boolean zIsImageOwnedByMe = isImageOwnedByMe(image);
        if (zIsImageOwnedByMe && !((WriterSurfaceImage) image).mIsImageValid) {
            throw new IllegalStateException("Image from ImageWriter is invalid");
        }
        if (!zIsImageOwnedByMe) {
            if (!(image.getOwner() instanceof ImageReader)) {
                throw new IllegalArgumentException("Only images from ImageReader can be queued to ImageWriter, other image source is not supported yet!");
            }
            ((ImageReader) image.getOwner()).detachImage(image);
            attachAndQueueInputImage(image);
            image.close();
            return;
        }
        Rect cropRect = image.getCropRect();
        nativeQueueInputImage(this.mNativeContext, image, image.getTimestamp(), cropRect.left, cropRect.top, cropRect.right, cropRect.bottom, image.getTransform(), image.getScalingMode());
        if (zIsImageOwnedByMe) {
            this.mDequeuedImages.remove(image);
            WriterSurfaceImage writerSurfaceImage = (WriterSurfaceImage) image;
            writerSurfaceImage.clearSurfacePlanes();
            writerSurfaceImage.mIsImageValid = false;
        }
    }

    public int getFormat() {
        return this.mWriterFormat;
    }

    public void setOnImageReleasedListener(OnImageReleasedListener onImageReleasedListener, Handler handler) {
        synchronized (this.mListenerLock) {
            if (onImageReleasedListener != null) {
                Looper looper = handler != null ? handler.getLooper() : Looper.myLooper();
                if (looper == null) {
                    throw new IllegalArgumentException("handler is null but the current thread is not a looper");
                }
                if (this.mListenerHandler == null || this.mListenerHandler.getLooper() != looper) {
                    this.mListenerHandler = new ListenerHandler(looper);
                }
                this.mListener = onImageReleasedListener;
            } else {
                this.mListener = null;
                this.mListenerHandler = null;
            }
        }
    }

    @Override
    public void close() {
        setOnImageReleasedListener(null, null);
        Iterator<Image> it = this.mDequeuedImages.iterator();
        while (it.hasNext()) {
            it.next().close();
        }
        this.mDequeuedImages.clear();
        nativeClose(this.mNativeContext);
        this.mNativeContext = 0L;
        if (this.mEstimatedNativeAllocBytes > 0) {
            VMRuntime.getRuntime().registerNativeFree(this.mEstimatedNativeAllocBytes);
            this.mEstimatedNativeAllocBytes = 0;
        }
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
        Rect cropRect = image.getCropRect();
        nativeAttachAndQueueImage(this.mNativeContext, image.getNativeContext(), image.getFormat(), image.getTimestamp(), cropRect.left, cropRect.top, cropRect.right, cropRect.bottom, image.getTransform(), image.getScalingMode());
    }

    private final class ListenerHandler extends Handler {
        public ListenerHandler(Looper looper) {
            super(looper, null, true);
        }

        @Override
        public void handleMessage(Message message) {
            OnImageReleasedListener onImageReleasedListener;
            synchronized (ImageWriter.this.mListenerLock) {
                onImageReleasedListener = ImageWriter.this.mListener;
            }
            if (onImageReleasedListener != null) {
                onImageReleasedListener.onImageReleased(ImageWriter.this);
            }
        }
    }

    private static void postEventFromNative(Object obj) {
        ListenerHandler listenerHandler;
        ImageWriter imageWriter = (ImageWriter) ((WeakReference) obj).get();
        if (imageWriter == null) {
            return;
        }
        synchronized (imageWriter.mListenerLock) {
            listenerHandler = imageWriter.mListenerHandler;
        }
        if (listenerHandler != null) {
            listenerHandler.sendEmptyMessage(0);
        }
    }

    private void abortImage(Image image) {
        if (image == null) {
            throw new IllegalArgumentException("image shouldn't be null");
        }
        if (!this.mDequeuedImages.contains(image)) {
            throw new IllegalStateException("It is illegal to abort some image that is not dequeued yet");
        }
        WriterSurfaceImage writerSurfaceImage = (WriterSurfaceImage) image;
        if (!writerSurfaceImage.mIsImageValid) {
            return;
        }
        cancelImage(this.mNativeContext, image);
        this.mDequeuedImages.remove(image);
        writerSurfaceImage.clearSurfacePlanes();
        writerSurfaceImage.mIsImageValid = false;
    }

    private boolean isImageOwnedByMe(Image image) {
        return (image instanceof WriterSurfaceImage) && ((WriterSurfaceImage) image).getOwner() == this;
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
        private int mTransform = 0;
        private int mScalingMode = 0;

        private native synchronized SurfacePlane[] nativeCreatePlanes(int i, int i2);

        private native synchronized int nativeGetFormat();

        private native synchronized HardwareBuffer nativeGetHardwareBuffer();

        private native synchronized int nativeGetHeight();

        private native synchronized int nativeGetWidth();

        public WriterSurfaceImage(ImageWriter imageWriter) {
            this.mOwner = imageWriter;
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
        public int getTransform() {
            throwISEIfImageIsInvalid();
            return this.mTransform;
        }

        @Override
        public int getScalingMode() {
            throwISEIfImageIsInvalid();
            return this.mScalingMode;
        }

        @Override
        public long getTimestamp() {
            throwISEIfImageIsInvalid();
            return this.mTimestamp;
        }

        @Override
        public void setTimestamp(long j) {
            throwISEIfImageIsInvalid();
            this.mTimestamp = j;
        }

        @Override
        public HardwareBuffer getHardwareBuffer() {
            throwISEIfImageIsInvalid();
            return nativeGetHardwareBuffer();
        }

        @Override
        public Image.Plane[] getPlanes() {
            throwISEIfImageIsInvalid();
            if (this.mPlanes == null) {
                this.mPlanes = nativeCreatePlanes(ImageUtils.getNumPlanesForFormat(getFormat()), getOwner().getFormat());
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
            if (this.mIsImageValid) {
                getOwner().abortImage(this);
            }
        }

        protected final void finalize() throws Throwable {
            try {
                close();
            } finally {
                super.finalize();
            }
        }

        private void clearSurfacePlanes() {
            if (this.mIsImageValid && this.mPlanes != null) {
                for (int i = 0; i < this.mPlanes.length; i++) {
                    if (this.mPlanes[i] != null) {
                        this.mPlanes[i].clearBuffer();
                        this.mPlanes[i] = null;
                    }
                }
            }
        }

        private class SurfacePlane extends Image.Plane {
            private ByteBuffer mBuffer;
            private final int mPixelStride;
            private final int mRowStride;

            private SurfacePlane(int i, int i2, ByteBuffer byteBuffer) {
                this.mRowStride = i;
                this.mPixelStride = i2;
                this.mBuffer = byteBuffer;
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
