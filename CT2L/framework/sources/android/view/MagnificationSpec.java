package android.view;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.Pools;

public class MagnificationSpec implements Parcelable {
    private static final int MAX_POOL_SIZE = 20;
    public float offsetX;
    public float offsetY;
    public float scale = 1.0f;
    private static final Pools.SynchronizedPool<MagnificationSpec> sPool = new Pools.SynchronizedPool<>(20);
    public static final Parcelable.Creator<MagnificationSpec> CREATOR = new Parcelable.Creator<MagnificationSpec>() {
        @Override
        public MagnificationSpec[] newArray(int size) {
            return new MagnificationSpec[size];
        }

        @Override
        public MagnificationSpec createFromParcel(Parcel parcel) {
            MagnificationSpec spec = MagnificationSpec.obtain();
            spec.initFromParcel(parcel);
            return spec;
        }
    };

    private MagnificationSpec() {
    }

    public void initialize(float scale, float offsetX, float offsetY) {
        if (scale < 1.0f) {
            throw new IllegalArgumentException("Scale must be greater than or equal to one!");
        }
        this.scale = scale;
        this.offsetX = offsetX;
        this.offsetY = offsetY;
    }

    public boolean isNop() {
        return this.scale == 1.0f && this.offsetX == 0.0f && this.offsetY == 0.0f;
    }

    public static MagnificationSpec obtain(MagnificationSpec other) {
        MagnificationSpec info = obtain();
        info.scale = other.scale;
        info.offsetX = other.offsetX;
        info.offsetY = other.offsetY;
        return info;
    }

    public static MagnificationSpec obtain() {
        MagnificationSpec spec = sPool.acquire();
        return spec != null ? spec : new MagnificationSpec();
    }

    public void recycle() {
        clear();
        sPool.release(this);
    }

    public void clear() {
        this.scale = 1.0f;
        this.offsetX = 0.0f;
        this.offsetY = 0.0f;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int flags) {
        parcel.writeFloat(this.scale);
        parcel.writeFloat(this.offsetX);
        parcel.writeFloat(this.offsetY);
        recycle();
    }

    public String toString() {
        return "<scale:" + this.scale + ",offsetX:" + this.offsetX + ",offsetY:" + this.offsetY + ">";
    }

    private void initFromParcel(Parcel parcel) {
        this.scale = parcel.readFloat();
        this.offsetX = parcel.readFloat();
        this.offsetY = parcel.readFloat();
    }
}
