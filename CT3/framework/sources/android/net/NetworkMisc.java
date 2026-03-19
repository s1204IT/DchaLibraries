package android.net;

import android.os.Parcel;
import android.os.Parcelable;

public class NetworkMisc implements Parcelable {
    public static final Parcelable.Creator<NetworkMisc> CREATOR = new Parcelable.Creator<NetworkMisc>() {
        @Override
        public NetworkMisc createFromParcel(Parcel in) {
            NetworkMisc networkMisc = new NetworkMisc();
            networkMisc.allowBypass = in.readInt() != 0;
            networkMisc.explicitlySelected = in.readInt() != 0;
            networkMisc.acceptUnvalidated = in.readInt() != 0;
            networkMisc.subscriberId = in.readString();
            return networkMisc;
        }

        @Override
        public NetworkMisc[] newArray(int size) {
            return new NetworkMisc[size];
        }
    };
    public boolean acceptUnvalidated;
    public boolean allowBypass;
    public boolean explicitlySelected;
    public String subscriberId;

    public NetworkMisc() {
    }

    public NetworkMisc(NetworkMisc nm) {
        if (nm == null) {
            return;
        }
        this.allowBypass = nm.allowBypass;
        this.explicitlySelected = nm.explicitlySelected;
        this.acceptUnvalidated = nm.acceptUnvalidated;
        this.subscriberId = nm.subscriberId;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel out, int flags) {
        out.writeInt(this.allowBypass ? 1 : 0);
        out.writeInt(this.explicitlySelected ? 1 : 0);
        out.writeInt(this.acceptUnvalidated ? 1 : 0);
        out.writeString(this.subscriberId);
    }
}
