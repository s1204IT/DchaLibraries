package android.content.pm;

import android.os.Parcel;
import android.os.Parcelable;
import java.security.PublicKey;

public class VerifierInfo implements Parcelable {
    public static final Parcelable.Creator<VerifierInfo> CREATOR = new Parcelable.Creator<VerifierInfo>() {
        @Override
        public VerifierInfo createFromParcel(Parcel parcel) {
            return new VerifierInfo(parcel);
        }

        @Override
        public VerifierInfo[] newArray(int i) {
            return new VerifierInfo[i];
        }
    };
    public final String packageName;
    public final PublicKey publicKey;

    public VerifierInfo(String str, PublicKey publicKey) {
        if (str == null || str.length() == 0) {
            throw new IllegalArgumentException("packageName must not be null or empty");
        }
        if (publicKey == null) {
            throw new IllegalArgumentException("publicKey must not be null");
        }
        this.packageName = str;
        this.publicKey = publicKey;
    }

    private VerifierInfo(Parcel parcel) {
        this.packageName = parcel.readString();
        this.publicKey = (PublicKey) parcel.readSerializable();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeString(this.packageName);
        parcel.writeSerializable(this.publicKey);
    }
}
