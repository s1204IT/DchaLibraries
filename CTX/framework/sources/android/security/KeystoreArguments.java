package android.security;

import android.os.Parcel;
import android.os.Parcelable;

public class KeystoreArguments implements Parcelable {
    public static final Parcelable.Creator<KeystoreArguments> CREATOR = new Parcelable.Creator<KeystoreArguments>() {
        @Override
        public KeystoreArguments createFromParcel(Parcel parcel) {
            return new KeystoreArguments(parcel);
        }

        @Override
        public KeystoreArguments[] newArray(int i) {
            return new KeystoreArguments[i];
        }
    };
    public byte[][] args;

    public KeystoreArguments() {
        this.args = null;
    }

    public KeystoreArguments(byte[][] bArr) {
        this.args = bArr;
    }

    private KeystoreArguments(Parcel parcel) {
        readFromParcel(parcel);
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        if (this.args == null) {
            parcel.writeInt(0);
            return;
        }
        parcel.writeInt(this.args.length);
        for (byte[] bArr : this.args) {
            parcel.writeByteArray(bArr);
        }
    }

    private void readFromParcel(Parcel parcel) {
        int i = parcel.readInt();
        this.args = new byte[i][];
        for (int i2 = 0; i2 < i; i2++) {
            this.args[i2] = parcel.createByteArray();
        }
    }

    @Override
    public int describeContents() {
        return 0;
    }
}
