package android.hardware.fingerprint;

import android.hardware.biometrics.BiometricAuthenticator;
import android.os.Parcel;
import android.os.Parcelable;

public final class Fingerprint extends BiometricAuthenticator.BiometricIdentifier {
    public static final Parcelable.Creator<Fingerprint> CREATOR = new Parcelable.Creator<Fingerprint>() {
        @Override
        public Fingerprint createFromParcel(Parcel parcel) {
            return new Fingerprint(parcel);
        }

        @Override
        public Fingerprint[] newArray(int i) {
            return new Fingerprint[i];
        }
    };
    private long mDeviceId;
    private int mFingerId;
    private int mGroupId;
    private CharSequence mName;

    public Fingerprint(CharSequence charSequence, int i, int i2, long j) {
        this.mName = charSequence;
        this.mGroupId = i;
        this.mFingerId = i2;
        this.mDeviceId = j;
    }

    private Fingerprint(Parcel parcel) {
        this.mName = parcel.readString();
        this.mGroupId = parcel.readInt();
        this.mFingerId = parcel.readInt();
        this.mDeviceId = parcel.readLong();
    }

    public CharSequence getName() {
        return this.mName;
    }

    public int getFingerId() {
        return this.mFingerId;
    }

    public int getGroupId() {
        return this.mGroupId;
    }

    public long getDeviceId() {
        return this.mDeviceId;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeString(this.mName.toString());
        parcel.writeInt(this.mGroupId);
        parcel.writeInt(this.mFingerId);
        parcel.writeLong(this.mDeviceId);
    }
}
