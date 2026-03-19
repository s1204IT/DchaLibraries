package android.hardware.display;

import android.os.Parcel;
import android.os.Parcelable;

public final class Curve implements Parcelable {
    public static final Parcelable.Creator<Curve> CREATOR = new Parcelable.Creator<Curve>() {
        @Override
        public Curve createFromParcel(Parcel parcel) {
            return new Curve(parcel.createFloatArray(), parcel.createFloatArray());
        }

        @Override
        public Curve[] newArray(int i) {
            return new Curve[i];
        }
    };
    private final float[] mX;
    private final float[] mY;

    public Curve(float[] fArr, float[] fArr2) {
        this.mX = fArr;
        this.mY = fArr2;
    }

    public float[] getX() {
        return this.mX;
    }

    public float[] getY() {
        return this.mY;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeFloatArray(this.mX);
        parcel.writeFloatArray(this.mY);
    }

    @Override
    public int describeContents() {
        return 0;
    }
}
