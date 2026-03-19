package android.net.wifi.p2p.link;

import android.net.ProxyInfo;
import android.os.Parcel;
import android.os.Parcelable;

public class WifiP2pLinkInfo implements Parcelable {
    public static final Parcelable.Creator<WifiP2pLinkInfo> CREATOR = new Parcelable.Creator<WifiP2pLinkInfo>() {
        @Override
        public WifiP2pLinkInfo createFromParcel(Parcel in) {
            WifiP2pLinkInfo info = new WifiP2pLinkInfo();
            info.interfaceAddress = in.readString();
            info.linkInfo = in.readString();
            return info;
        }

        @Override
        public WifiP2pLinkInfo[] newArray(int size) {
            return new WifiP2pLinkInfo[size];
        }
    };
    public String interfaceAddress;
    public String linkInfo;

    public WifiP2pLinkInfo() {
        this.interfaceAddress = ProxyInfo.LOCAL_EXCL_LIST;
        this.linkInfo = ProxyInfo.LOCAL_EXCL_LIST;
    }

    public String toString() {
        StringBuffer sbuf = new StringBuffer();
        sbuf.append("interfaceAddress=").append(this.interfaceAddress);
        sbuf.append(" linkInfo=").append(this.linkInfo);
        return sbuf.toString();
    }

    public WifiP2pLinkInfo(WifiP2pLinkInfo source) {
        this.interfaceAddress = ProxyInfo.LOCAL_EXCL_LIST;
        this.linkInfo = ProxyInfo.LOCAL_EXCL_LIST;
        if (source == null) {
            return;
        }
        this.interfaceAddress = source.interfaceAddress;
        this.linkInfo = source.linkInfo;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.interfaceAddress);
        dest.writeString(this.linkInfo);
    }
}
