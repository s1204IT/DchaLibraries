package android.powerhint;

import android.os.Parcel;
import android.os.Parcelable;

public class StringParcelable implements Parcelable {
    public static final Parcelable.Creator<StringParcelable> CREATOR = new Parcelable.Creator<StringParcelable>() {
        @Override
        public StringParcelable createFromParcel(Parcel in) {
            return new StringParcelable();
        }

        @Override
        public StringParcelable[] newArray(int size) {
            return new StringParcelable[size];
        }
    };
    String mData;

    private StringParcelable() {
    }

    public StringParcelable(String data) {
        this.mData = data;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.mData);
    }
}
