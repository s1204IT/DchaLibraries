package android.net.wifi;

import android.os.Parcel;
import android.os.Parcelable;

public class PPPOEConfig implements Parcelable {
    public static final Parcelable.Creator<PPPOEConfig> CREATOR = new Parcelable.Creator<PPPOEConfig>() {
        @Override
        public PPPOEConfig createFromParcel(Parcel in) {
            PPPOEConfig result = new PPPOEConfig();
            result.username = in.readString();
            result.password = in.readString();
            result.interf = in.readString();
            result.lcp_echo_interval = in.readInt();
            result.lcp_echo_failure = in.readInt();
            result.mtu = in.readInt();
            result.mru = in.readInt();
            result.timeout = in.readInt();
            result.MSS = in.readInt();
            return result;
        }

        @Override
        public PPPOEConfig[] newArray(int size) {
            return new PPPOEConfig[size];
        }
    };
    public int MSS;
    public String interf;
    public int lcp_echo_failure;
    public int lcp_echo_interval;
    public int mru;
    public int mtu;
    public String password;
    public int timeout;
    public String username;

    public String toString() {
        StringBuffer sbuf = new StringBuffer();
        sbuf.append(" username: ").append(this.username);
        sbuf.append('\n');
        sbuf.append(" password: ").append(this.password);
        sbuf.append('\n');
        sbuf.append(" interf: ").append(this.interf);
        sbuf.append("\n");
        sbuf.append(" lcp_echo_interval: ").append(this.lcp_echo_interval);
        sbuf.append("\n");
        sbuf.append(" lcp_echo_failure: ").append(this.lcp_echo_failure);
        sbuf.append("\n");
        sbuf.append(" mtu: ").append(this.mtu);
        sbuf.append("\n");
        sbuf.append(" mru: ").append(this.mru);
        sbuf.append("\n");
        sbuf.append(" timeout: ").append(this.timeout);
        sbuf.append("\n");
        sbuf.append(" MSS: ").append(this.MSS);
        sbuf.append("\n");
        return sbuf.toString();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public PPPOEConfig() {
        this.interf = "wlan0";
        this.lcp_echo_interval = 10;
        this.lcp_echo_failure = 2;
        this.mtu = 1492;
        this.mru = 1492;
        this.timeout = 10;
        this.MSS = 1412;
    }

    public PPPOEConfig(PPPOEConfig source) {
        this.interf = "wlan0";
        this.lcp_echo_interval = 10;
        this.lcp_echo_failure = 2;
        this.mtu = 1492;
        this.mru = 1492;
        this.timeout = 10;
        this.MSS = 1412;
        if (source == null) {
            return;
        }
        this.username = source.username;
        this.password = source.password;
        this.interf = source.interf;
        this.lcp_echo_interval = source.lcp_echo_interval;
        this.lcp_echo_failure = source.lcp_echo_failure;
        this.mtu = source.mtu;
        this.mru = source.mru;
        this.timeout = source.timeout;
        this.MSS = source.MSS;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.username);
        dest.writeString(this.password);
        dest.writeString(this.interf);
        dest.writeInt(this.lcp_echo_interval);
        dest.writeInt(this.lcp_echo_failure);
        dest.writeInt(this.mtu);
        dest.writeInt(this.mru);
        dest.writeInt(this.timeout);
        dest.writeInt(this.MSS);
    }
}
