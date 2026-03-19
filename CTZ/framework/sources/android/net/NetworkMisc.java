package android.net;

import android.os.Parcel;
import android.os.Parcelable;

public class NetworkMisc implements Parcelable {
    public static final Parcelable.Creator<NetworkMisc> CREATOR = new Parcelable.Creator<NetworkMisc>() {
        @Override
        public NetworkMisc createFromParcel(Parcel parcel) {
            NetworkMisc networkMisc = new NetworkMisc();
            networkMisc.allowBypass = parcel.readInt() != 0;
            networkMisc.explicitlySelected = parcel.readInt() != 0;
            networkMisc.acceptUnvalidated = parcel.readInt() != 0;
            networkMisc.subscriberId = parcel.readString();
            networkMisc.provisioningNotificationDisabled = parcel.readInt() != 0;
            return networkMisc;
        }

        @Override
        public NetworkMisc[] newArray(int i) {
            return new NetworkMisc[i];
        }
    };
    public boolean acceptUnvalidated;
    public boolean allowBypass;
    public boolean explicitlySelected;
    public boolean provisioningNotificationDisabled;
    public String subscriberId;

    public NetworkMisc() {
    }

    public NetworkMisc(NetworkMisc networkMisc) {
        if (networkMisc != null) {
            this.allowBypass = networkMisc.allowBypass;
            this.explicitlySelected = networkMisc.explicitlySelected;
            this.acceptUnvalidated = networkMisc.acceptUnvalidated;
            this.subscriberId = networkMisc.subscriberId;
            this.provisioningNotificationDisabled = networkMisc.provisioningNotificationDisabled;
        }
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeInt(this.allowBypass ? 1 : 0);
        parcel.writeInt(this.explicitlySelected ? 1 : 0);
        parcel.writeInt(this.acceptUnvalidated ? 1 : 0);
        parcel.writeString(this.subscriberId);
        parcel.writeInt(this.provisioningNotificationDisabled ? 1 : 0);
    }
}
