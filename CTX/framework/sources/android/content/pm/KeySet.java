package android.content.pm;

import android.os.IBinder;
import android.os.Parcel;
import android.os.Parcelable;

public class KeySet implements Parcelable {
    public static final Parcelable.Creator<KeySet> CREATOR = new Parcelable.Creator<KeySet>() {
        @Override
        public KeySet createFromParcel(Parcel parcel) {
            return KeySet.readFromParcel(parcel);
        }

        @Override
        public KeySet[] newArray(int i) {
            return new KeySet[i];
        }
    };
    private IBinder token;

    public KeySet(IBinder iBinder) {
        if (iBinder == null) {
            throw new NullPointerException("null value for KeySet IBinder token");
        }
        this.token = iBinder;
    }

    public IBinder getToken() {
        return this.token;
    }

    public boolean equals(Object obj) {
        return (obj instanceof KeySet) && this.token == ((KeySet) obj).token;
    }

    public int hashCode() {
        return this.token.hashCode();
    }

    private static KeySet readFromParcel(Parcel parcel) {
        return new KeySet(parcel.readStrongBinder());
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeStrongBinder(this.token);
    }

    @Override
    public int describeContents() {
        return 0;
    }
}
