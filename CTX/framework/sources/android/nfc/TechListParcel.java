package android.nfc;

import android.os.Parcel;
import android.os.Parcelable;

public class TechListParcel implements Parcelable {
    public static final Parcelable.Creator<TechListParcel> CREATOR = new Parcelable.Creator<TechListParcel>() {
        @Override
        public TechListParcel createFromParcel(Parcel parcel) {
            int i = parcel.readInt();
            String[][] strArr = new String[i][];
            for (int i2 = 0; i2 < i; i2++) {
                strArr[i2] = parcel.readStringArray();
            }
            return new TechListParcel(strArr);
        }

        @Override
        public TechListParcel[] newArray(int i) {
            return new TechListParcel[i];
        }
    };
    private String[][] mTechLists;

    public TechListParcel(String[]... strArr) {
        this.mTechLists = strArr;
    }

    public String[][] getTechLists() {
        return this.mTechLists;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        int length = this.mTechLists.length;
        parcel.writeInt(length);
        for (int i2 = 0; i2 < length; i2++) {
            parcel.writeStringArray(this.mTechLists[i2]);
        }
    }
}
