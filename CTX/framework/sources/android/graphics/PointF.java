package android.graphics;

import android.os.Parcel;
import android.os.Parcelable;

public class PointF implements Parcelable {
    public static final Parcelable.Creator<PointF> CREATOR = new Parcelable.Creator<PointF>() {
        @Override
        public PointF createFromParcel(Parcel parcel) {
            PointF pointF = new PointF();
            pointF.readFromParcel(parcel);
            return pointF;
        }

        @Override
        public PointF[] newArray(int i) {
            return new PointF[i];
        }
    };
    public float x;
    public float y;

    public PointF() {
    }

    public PointF(float f, float f2) {
        this.x = f;
        this.y = f2;
    }

    public PointF(Point point) {
        this.x = point.x;
        this.y = point.y;
    }

    public final void set(float f, float f2) {
        this.x = f;
        this.y = f2;
    }

    public final void set(PointF pointF) {
        this.x = pointF.x;
        this.y = pointF.y;
    }

    public final void negate() {
        this.x = -this.x;
        this.y = -this.y;
    }

    public final void offset(float f, float f2) {
        this.x += f;
        this.y += f2;
    }

    public final boolean equals(float f, float f2) {
        return this.x == f && this.y == f2;
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        PointF pointF = (PointF) obj;
        if (Float.compare(pointF.x, this.x) == 0 && Float.compare(pointF.y, this.y) == 0) {
            return true;
        }
        return false;
    }

    public int hashCode() {
        return (31 * (this.x != 0.0f ? Float.floatToIntBits(this.x) : 0)) + (this.y != 0.0f ? Float.floatToIntBits(this.y) : 0);
    }

    public String toString() {
        return "PointF(" + this.x + ", " + this.y + ")";
    }

    public final float length() {
        return length(this.x, this.y);
    }

    public static float length(float f, float f2) {
        return (float) Math.hypot(f, f2);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeFloat(this.x);
        parcel.writeFloat(this.y);
    }

    public void readFromParcel(Parcel parcel) {
        this.x = parcel.readFloat();
        this.y = parcel.readFloat();
    }
}
