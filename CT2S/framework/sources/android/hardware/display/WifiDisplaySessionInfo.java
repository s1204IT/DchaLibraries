package android.hardware.display;

import android.net.ProxyInfo;
import android.os.Parcel;
import android.os.Parcelable;

public final class WifiDisplaySessionInfo implements Parcelable {
    public static final Parcelable.Creator<WifiDisplaySessionInfo> CREATOR = new Parcelable.Creator<WifiDisplaySessionInfo>() {
        @Override
        public WifiDisplaySessionInfo createFromParcel(Parcel in) {
            boolean client = in.readInt() != 0;
            int session = in.readInt();
            String group = in.readString();
            String pp = in.readString();
            String ip = in.readString();
            return new WifiDisplaySessionInfo(client, session, group, pp, ip);
        }

        @Override
        public WifiDisplaySessionInfo[] newArray(int size) {
            return new WifiDisplaySessionInfo[size];
        }
    };
    private final boolean mClient;
    private final String mGroupId;
    private final String mIP;
    private final String mPassphrase;
    private final int mSessionId;

    public WifiDisplaySessionInfo() {
        this(true, 0, ProxyInfo.LOCAL_EXCL_LIST, ProxyInfo.LOCAL_EXCL_LIST, ProxyInfo.LOCAL_EXCL_LIST);
    }

    public WifiDisplaySessionInfo(boolean client, int session, String group, String pp, String ip) {
        this.mClient = client;
        this.mSessionId = session;
        this.mGroupId = group;
        this.mPassphrase = pp;
        this.mIP = ip;
    }

    public boolean isClient() {
        return this.mClient;
    }

    public int getSessionId() {
        return this.mSessionId;
    }

    public String getGroupId() {
        return this.mGroupId;
    }

    public String getPassphrase() {
        return this.mPassphrase;
    }

    public String getIP() {
        return this.mIP;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(this.mClient ? 1 : 0);
        dest.writeInt(this.mSessionId);
        dest.writeString(this.mGroupId);
        dest.writeString(this.mPassphrase);
        dest.writeString(this.mIP);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public String toString() {
        return "WifiDisplaySessionInfo:\n    Client/Owner: " + (this.mClient ? "Client" : "Owner") + "\n    GroupId: " + this.mGroupId + "\n    Passphrase: " + this.mPassphrase + "\n    SessionId: " + this.mSessionId + "\n    IP Address: " + this.mIP;
    }
}
