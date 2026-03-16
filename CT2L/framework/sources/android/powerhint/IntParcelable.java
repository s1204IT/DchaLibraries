package android.powerhint;

import android.os.Parcel;
import android.os.Parcelable;

public class IntParcelable implements Parcelable {
    public static final Parcelable.Creator<IntParcelable> CREATOR = new Parcelable.Creator<IntParcelable>() {
        @Override
        public IntParcelable createFromParcel(Parcel in) {
            return new IntParcelable();
        }

        @Override
        public IntParcelable[] newArray(int size) {
            return new IntParcelable[size];
        }
    };
    int mData;

    private IntParcelable() {
    }

    public IntParcelable(int data) {
        this.mData = data;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(this.mData);
    }
}
