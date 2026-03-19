package android.net;

import android.annotation.SystemApi;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiSsid;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;
import android.util.Log;
import java.util.Objects;

@SystemApi
public class NetworkKey implements Parcelable {
    public static final Parcelable.Creator<NetworkKey> CREATOR = new Parcelable.Creator<NetworkKey>() {
        @Override
        public NetworkKey createFromParcel(Parcel parcel) {
            return new NetworkKey(parcel);
        }

        @Override
        public NetworkKey[] newArray(int i) {
            return new NetworkKey[i];
        }
    };
    private static final String TAG = "NetworkKey";
    public static final int TYPE_WIFI = 1;
    public final int type;
    public final WifiKey wifiKey;

    public static NetworkKey createFromScanResult(ScanResult scanResult) {
        if (scanResult != null && scanResult.wifiSsid != null) {
            String string = scanResult.wifiSsid.toString();
            String str = scanResult.BSSID;
            if (!TextUtils.isEmpty(string) && !string.equals(WifiSsid.NONE) && !TextUtils.isEmpty(str)) {
                try {
                    return new NetworkKey(new WifiKey(String.format("\"%s\"", string), str));
                } catch (IllegalArgumentException e) {
                    Log.e(TAG, "Unable to create WifiKey.", e);
                    return null;
                }
            }
        }
        return null;
    }

    public static NetworkKey createFromWifiInfo(WifiInfo wifiInfo) {
        if (wifiInfo != null) {
            String ssid = wifiInfo.getSSID();
            String bssid = wifiInfo.getBSSID();
            if (!TextUtils.isEmpty(ssid) && !ssid.equals(WifiSsid.NONE) && !TextUtils.isEmpty(bssid)) {
                try {
                    return new NetworkKey(new WifiKey(ssid, bssid));
                } catch (IllegalArgumentException e) {
                    Log.e(TAG, "Unable to create WifiKey.", e);
                    return null;
                }
            }
        }
        return null;
    }

    public NetworkKey(WifiKey wifiKey) {
        this.type = 1;
        this.wifiKey = wifiKey;
    }

    private NetworkKey(Parcel parcel) {
        this.type = parcel.readInt();
        if (this.type == 1) {
            this.wifiKey = WifiKey.CREATOR.createFromParcel(parcel);
            return;
        }
        throw new IllegalArgumentException("Parcel has unknown type: " + this.type);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeInt(this.type);
        if (this.type == 1) {
            this.wifiKey.writeToParcel(parcel, i);
            return;
        }
        throw new IllegalStateException("NetworkKey has unknown type " + this.type);
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        NetworkKey networkKey = (NetworkKey) obj;
        if (this.type == networkKey.type && Objects.equals(this.wifiKey, networkKey.wifiKey)) {
            return true;
        }
        return false;
    }

    public int hashCode() {
        return Objects.hash(Integer.valueOf(this.type), this.wifiKey);
    }

    public String toString() {
        if (this.type == 1) {
            return this.wifiKey.toString();
        }
        return "InvalidKey";
    }
}
