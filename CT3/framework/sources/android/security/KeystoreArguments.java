package android.security;

import android.os.Parcel;
import android.os.Parcelable;

public class KeystoreArguments implements Parcelable {
    public static final Parcelable.Creator<KeystoreArguments> CREATOR = new Parcelable.Creator<KeystoreArguments>() {
        @Override
        public KeystoreArguments createFromParcel(Parcel in) {
            return new KeystoreArguments(in, null);
        }

        @Override
        public KeystoreArguments[] newArray(int size) {
            return new KeystoreArguments[size];
        }
    };
    public byte[][] args;

    KeystoreArguments(Parcel in, KeystoreArguments keystoreArguments) {
        this(in);
    }

    public KeystoreArguments() {
        this.args = null;
    }

    public KeystoreArguments(byte[][] args) {
        this.args = args;
    }

    private KeystoreArguments(Parcel in) {
        readFromParcel(in);
    }

    @Override
    public void writeToParcel(Parcel out, int flags) {
        if (this.args == null) {
            out.writeInt(0);
            return;
        }
        out.writeInt(this.args.length);
        for (byte[] arg : this.args) {
            out.writeByteArray(arg);
        }
    }

    private void readFromParcel(Parcel in) {
        int length = in.readInt();
        this.args = new byte[length][];
        for (int i = 0; i < length; i++) {
            this.args[i] = in.createByteArray();
        }
    }

    @Override
    public int describeContents() {
        return 0;
    }
}
