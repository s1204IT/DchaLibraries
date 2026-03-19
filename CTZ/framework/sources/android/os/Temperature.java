package android.os;

import android.os.Parcelable;

public class Temperature implements Parcelable {
    public static final Parcelable.Creator<Temperature> CREATOR = new Parcelable.Creator<Temperature>() {
        @Override
        public Temperature createFromParcel(Parcel parcel) {
            return new Temperature(parcel);
        }

        @Override
        public Temperature[] newArray(int i) {
            return new Temperature[i];
        }
    };
    private int mType;
    private float mValue;

    public Temperature() {
        this(-3.4028235E38f, Integer.MIN_VALUE);
    }

    public Temperature(float f, int i) {
        this.mValue = f;
        this.mType = i;
    }

    public float getValue() {
        return this.mValue;
    }

    public int getType() {
        return this.mType;
    }

    private Temperature(Parcel parcel) {
        readFromParcel(parcel);
    }

    public void readFromParcel(Parcel parcel) {
        this.mValue = parcel.readFloat();
        this.mType = parcel.readInt();
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeFloat(this.mValue);
        parcel.writeInt(this.mType);
    }

    @Override
    public int describeContents() {
        return 0;
    }
}
