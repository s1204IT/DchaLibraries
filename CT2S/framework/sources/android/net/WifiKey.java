package android.net;

import android.os.Parcel;
import android.os.Parcelable;
import java.util.Objects;
import java.util.regex.Pattern;

public class WifiKey implements Parcelable {
    public final String bssid;
    public final String ssid;
    private static final Pattern SSID_PATTERN = Pattern.compile("(\".*\")|(0x[\\p{XDigit}]+)");
    private static final Pattern BSSID_PATTERN = Pattern.compile("([\\p{XDigit}]{2}:){5}[\\p{XDigit}]{2}");
    public static final Parcelable.Creator<WifiKey> CREATOR = new Parcelable.Creator<WifiKey>() {
        @Override
        public WifiKey createFromParcel(Parcel in) {
            return new WifiKey(in);
        }

        @Override
        public WifiKey[] newArray(int size) {
            return new WifiKey[size];
        }
    };

    public WifiKey(String ssid, String bssid) {
        if (!SSID_PATTERN.matcher(ssid).matches()) {
            throw new IllegalArgumentException("Invalid ssid: " + ssid);
        }
        if (!BSSID_PATTERN.matcher(bssid).matches()) {
            throw new IllegalArgumentException("Invalid bssid: " + bssid);
        }
        this.ssid = ssid;
        this.bssid = bssid;
    }

    private WifiKey(Parcel in) {
        this.ssid = in.readString();
        this.bssid = in.readString();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel out, int flags) {
        out.writeString(this.ssid);
        out.writeString(this.bssid);
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        WifiKey wifiKey = (WifiKey) o;
        return Objects.equals(this.ssid, wifiKey.ssid) && Objects.equals(this.bssid, wifiKey.bssid);
    }

    public int hashCode() {
        return Objects.hash(this.ssid, this.bssid);
    }

    public String toString() {
        return "WifiKey[SSID=" + this.ssid + ",BSSID=" + this.bssid + "]";
    }
}
