package com.android.internal.net;

import android.app.PendingIntent;
import android.net.NetworkInfo;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

public class LegacyVpnInfo implements Parcelable {
    public static final Parcelable.Creator<LegacyVpnInfo> CREATOR = new Parcelable.Creator<LegacyVpnInfo>() {
        @Override
        public LegacyVpnInfo createFromParcel(Parcel parcel) {
            LegacyVpnInfo legacyVpnInfo = new LegacyVpnInfo();
            legacyVpnInfo.key = parcel.readString();
            legacyVpnInfo.state = parcel.readInt();
            legacyVpnInfo.intent = (PendingIntent) parcel.readParcelable(null);
            return legacyVpnInfo;
        }

        @Override
        public LegacyVpnInfo[] newArray(int i) {
            return new LegacyVpnInfo[i];
        }
    };
    public static final int STATE_CONNECTED = 3;
    public static final int STATE_CONNECTING = 2;
    public static final int STATE_DISCONNECTED = 0;
    public static final int STATE_FAILED = 5;
    public static final int STATE_INITIALIZING = 1;
    public static final int STATE_TIMEOUT = 4;
    private static final String TAG = "LegacyVpnInfo";
    public PendingIntent intent;
    public String key;
    public int state = -1;

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeString(this.key);
        parcel.writeInt(this.state);
        parcel.writeParcelable(this.intent, i);
    }

    public static int stateFromNetworkInfo(NetworkInfo networkInfo) {
        switch (networkInfo.getDetailedState()) {
            case CONNECTING:
                break;
            case CONNECTED:
                break;
            case DISCONNECTED:
                break;
            case FAILED:
                break;
            default:
                Log.w(TAG, "Unhandled state " + networkInfo.getDetailedState() + " ; treating as disconnected");
                break;
        }
        return 0;
    }
}
