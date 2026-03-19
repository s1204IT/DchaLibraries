package android.net.wifi;

import android.os.Parcel;
import android.os.Parcelable;

public class WpsInfo implements Parcelable {
    public static final Parcelable.Creator<WpsInfo> CREATOR = new Parcelable.Creator<WpsInfo>() {
        @Override
        public WpsInfo createFromParcel(Parcel in) {
            WpsInfo config = new WpsInfo();
            config.setup = in.readInt();
            config.BSSID = in.readString();
            config.pin = in.readString();
            config.ssid = in.readString();
            config.authentication = in.readString();
            config.encryption = in.readString();
            config.key = in.readString();
            return config;
        }

        @Override
        public WpsInfo[] newArray(int size) {
            return new WpsInfo[size];
        }
    };
    public static final int DISPLAY = 1;
    public static final int INVALID = 4;
    public static final int KEYPAD = 2;
    public static final int LABEL = 3;
    public static final int PBC = 0;
    public String BSSID;
    public String authentication;
    public String encryption;
    public String key;
    public String pin;
    public int setup;
    public String ssid;

    public WpsInfo() {
        this.setup = 4;
        this.BSSID = null;
        this.pin = null;
        this.ssid = null;
        this.authentication = null;
        this.encryption = null;
        this.key = null;
    }

    public String toString() {
        StringBuffer sbuf = new StringBuffer();
        sbuf.append(" setup: ").append(this.setup);
        sbuf.append('\n');
        sbuf.append(" BSSID: ").append(this.BSSID);
        sbuf.append('\n');
        sbuf.append(" pin: ").append(this.pin);
        sbuf.append('\n');
        sbuf.append(" SSID: ").append(this.ssid);
        sbuf.append('\n');
        sbuf.append(" authentication: ").append(this.authentication);
        sbuf.append('\n');
        sbuf.append(" encryption: ").append(this.encryption);
        sbuf.append('\n');
        sbuf.append(" key: ").append(this.key);
        sbuf.append('\n');
        return sbuf.toString();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public WpsInfo(WpsInfo source) {
        if (source == null) {
            return;
        }
        this.setup = source.setup;
        this.BSSID = source.BSSID;
        this.pin = source.pin;
        this.ssid = source.ssid;
        this.authentication = source.authentication;
        this.encryption = source.encryption;
        this.key = source.key;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(this.setup);
        dest.writeString(this.BSSID);
        dest.writeString(this.pin);
        dest.writeString(this.ssid);
        dest.writeString(this.authentication);
        dest.writeString(this.encryption);
        dest.writeString(this.key);
    }
}
