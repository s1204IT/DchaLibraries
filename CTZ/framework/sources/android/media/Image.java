package android.media;

import android.graphics.Rect;
import android.hardware.HardwareBuffer;
import java.nio.ByteBuffer;

public abstract class Image implements AutoCloseable {
    private Rect mCropRect;
    protected boolean mIsImageValid = false;

    @Override
    public abstract void close();

    public abstract int getFormat();

    public abstract int getHeight();

    public abstract Plane[] getPlanes();

    public abstract int getScalingMode();

    public abstract long getTimestamp();

    public abstract int getTransform();

    public abstract int getWidth();

    protected Image() {
    }

    protected void throwISEIfImageIsInvalid() {
        if (!this.mIsImageValid) {
            throw new IllegalStateException("Image is already closed");
        }
    }

    public HardwareBuffer getHardwareBuffer() {
        throwISEIfImageIsInvalid();
        return null;
    }

    public void setTimestamp(long j) {
        throwISEIfImageIsInvalid();
    }

    public Rect getCropRect() {
        throwISEIfImageIsInvalid();
        if (this.mCropRect == null) {
            return new Rect(0, 0, getWidth(), getHeight());
        }
        return new Rect(this.mCropRect);
    }

    public void setCropRect(Rect rect) {
        throwISEIfImageIsInvalid();
        if (rect != null) {
            Rect rect2 = new Rect(rect);
            if (!rect2.intersect(0, 0, getWidth(), getHeight())) {
                rect2.setEmpty();
            }
            rect = rect2;
        }
        this.mCropRect = rect;
    }

    boolean isAttachable() {
        throwISEIfImageIsInvalid();
        return false;
    }

    Object getOwner() {
        throwISEIfImageIsInvalid();
        return null;
    }

    long getNativeContext() {
        throwISEIfImageIsInvalid();
        return 0L;
    }

    public static abstract class Plane {
        public abstract ByteBuffer getBuffer();

        public abstract int getPixelStride();

        public abstract int getRowStride();

        protected Plane() {
        }
    }
}
