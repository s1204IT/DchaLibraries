package android.telecom;

import android.os.Parcel;
import android.os.Parcelable;

public final class CameraCapabilities implements Parcelable {
    public static final Parcelable.Creator<CameraCapabilities> CREATOR = new Parcelable.Creator<CameraCapabilities>() {
        @Override
        public CameraCapabilities createFromParcel(Parcel source) {
            boolean supportsZoom = source.readByte() != 0;
            float maxZoom = source.readFloat();
            int width = source.readInt();
            int height = source.readInt();
            return new CameraCapabilities(supportsZoom, maxZoom, width, height);
        }

        @Override
        public CameraCapabilities[] newArray(int size) {
            return new CameraCapabilities[size];
        }
    };
    private final int mHeight;
    private final float mMaxZoom;
    private final int mWidth;
    private final boolean mZoomSupported;

    public CameraCapabilities(boolean zoomSupported, float maxZoom, int width, int height) {
        this.mZoomSupported = zoomSupported;
        this.mMaxZoom = maxZoom;
        this.mWidth = width;
        this.mHeight = height;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeByte((byte) (isZoomSupported() ? 1 : 0));
        dest.writeFloat(getMaxZoom());
        dest.writeInt(getWidth());
        dest.writeInt(getHeight());
    }

    public boolean isZoomSupported() {
        return this.mZoomSupported;
    }

    public float getMaxZoom() {
        return this.mMaxZoom;
    }

    public int getWidth() {
        return this.mWidth;
    }

    public int getHeight() {
        return this.mHeight;
    }
}
