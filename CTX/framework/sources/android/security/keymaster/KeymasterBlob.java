package android.security.keymaster;

import android.os.Parcel;
import android.os.Parcelable;

public class KeymasterBlob implements Parcelable {
    public static final Parcelable.Creator<KeymasterBlob> CREATOR = new Parcelable.Creator<KeymasterBlob>() {
        @Override
        public KeymasterBlob createFromParcel(Parcel parcel) {
            return new KeymasterBlob(parcel);
        }

        @Override
        public KeymasterBlob[] newArray(int i) {
            return new KeymasterBlob[i];
        }
    };
    public byte[] blob;

    public KeymasterBlob(byte[] bArr) {
        this.blob = bArr;
    }

    protected KeymasterBlob(Parcel parcel) {
        this.blob = parcel.createByteArray();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeByteArray(this.blob);
    }
}
