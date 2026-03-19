package android.net;

import android.content.Context;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.telecom.Logging.Session;
import android.util.Slog;
import android.util.proto.ProtoOutputStream;
import java.util.Objects;

public class NetworkIdentity implements Comparable<NetworkIdentity> {

    @Deprecated
    public static final boolean COMBINE_SUBTYPE_ENABLED = true;
    public static final int SUBTYPE_COMBINED = -1;
    private static final String TAG = "NetworkIdentity";
    final boolean mDefaultNetwork;
    final boolean mMetered;
    final String mNetworkId;
    final boolean mRoaming;
    final int mSubType = -1;
    final String mSubscriberId;
    final int mType;

    public NetworkIdentity(int i, int i2, String str, String str2, boolean z, boolean z2, boolean z3) {
        this.mType = i;
        this.mSubscriberId = str;
        this.mNetworkId = str2;
        this.mRoaming = z;
        this.mMetered = z2;
        this.mDefaultNetwork = z3;
    }

    public int hashCode() {
        return Objects.hash(Integer.valueOf(this.mType), Integer.valueOf(this.mSubType), this.mSubscriberId, this.mNetworkId, Boolean.valueOf(this.mRoaming), Boolean.valueOf(this.mMetered), Boolean.valueOf(this.mDefaultNetwork));
    }

    public boolean equals(Object obj) {
        if (!(obj instanceof NetworkIdentity)) {
            return false;
        }
        NetworkIdentity networkIdentity = (NetworkIdentity) obj;
        return this.mType == networkIdentity.mType && this.mSubType == networkIdentity.mSubType && this.mRoaming == networkIdentity.mRoaming && Objects.equals(this.mSubscriberId, networkIdentity.mSubscriberId) && Objects.equals(this.mNetworkId, networkIdentity.mNetworkId) && this.mMetered == networkIdentity.mMetered && this.mDefaultNetwork == networkIdentity.mDefaultNetwork;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder("{");
        sb.append("type=");
        sb.append(ConnectivityManager.getNetworkTypeName(this.mType));
        sb.append(", subType=");
        sb.append("COMBINED");
        if (this.mSubscriberId != null) {
            sb.append(", subscriberId=");
            sb.append(scrubSubscriberId(this.mSubscriberId));
        }
        if (this.mNetworkId != null) {
            sb.append(", networkId=");
            sb.append(this.mNetworkId);
        }
        if (this.mRoaming) {
            sb.append(", ROAMING");
        }
        sb.append(", metered=");
        sb.append(this.mMetered);
        sb.append(", defaultNetwork=");
        sb.append(this.mDefaultNetwork);
        sb.append("}");
        return sb.toString();
    }

    public void writeToProto(ProtoOutputStream protoOutputStream, long j) {
        long jStart = protoOutputStream.start(j);
        protoOutputStream.write(1120986464257L, this.mType);
        if (this.mSubscriberId != null) {
            protoOutputStream.write(1138166333442L, scrubSubscriberId(this.mSubscriberId));
        }
        protoOutputStream.write(1138166333443L, this.mNetworkId);
        protoOutputStream.write(1133871366148L, this.mRoaming);
        protoOutputStream.write(1133871366149L, this.mMetered);
        protoOutputStream.write(1133871366150L, this.mDefaultNetwork);
        protoOutputStream.end(jStart);
    }

    public int getType() {
        return this.mType;
    }

    public int getSubType() {
        return this.mSubType;
    }

    public String getSubscriberId() {
        return this.mSubscriberId;
    }

    public String getNetworkId() {
        return this.mNetworkId;
    }

    public boolean getRoaming() {
        return this.mRoaming;
    }

    public boolean getMetered() {
        return this.mMetered;
    }

    public boolean getDefaultNetwork() {
        return this.mDefaultNetwork;
    }

    public static String scrubSubscriberId(String str) {
        if (Build.IS_ENG) {
            return str;
        }
        if (str != null) {
            return str.substring(0, Math.min(6, str.length())) + Session.TRUNCATE_STRING;
        }
        return "null";
    }

    public static String[] scrubSubscriberId(String[] strArr) {
        if (strArr == null) {
            return null;
        }
        String[] strArr2 = new String[strArr.length];
        for (int i = 0; i < strArr2.length; i++) {
            strArr2[i] = scrubSubscriberId(strArr[i]);
        }
        return strArr2;
    }

    public static NetworkIdentity buildNetworkIdentity(Context context, NetworkState networkState, boolean z) {
        String str;
        String ssid;
        int type = networkState.networkInfo.getType();
        int subtype = networkState.networkInfo.getSubtype();
        boolean z2 = !networkState.networkCapabilities.hasCapability(18);
        boolean z3 = !networkState.networkCapabilities.hasCapability(11);
        String str2 = null;
        if (ConnectivityManager.isNetworkTypeMobile(type)) {
            if (networkState.subscriberId == null && networkState.networkInfo.getState() != NetworkInfo.State.DISCONNECTED && networkState.networkInfo.getState() != NetworkInfo.State.UNKNOWN) {
                Slog.w(TAG, "Active mobile network without subscriber! ni = " + networkState.networkInfo);
            }
            str = null;
            str2 = networkState.subscriberId;
        } else if (type == 1) {
            if (networkState.networkId != null) {
                ssid = networkState.networkId;
            } else {
                WifiInfo connectionInfo = ((WifiManager) context.getSystemService("wifi")).getConnectionInfo();
                ssid = connectionInfo != null ? connectionInfo.getSSID() : null;
            }
            str = ssid;
        } else {
            str = null;
        }
        return new NetworkIdentity(type, subtype, str2, str, z2, z3, z);
    }

    @Override
    public int compareTo(NetworkIdentity networkIdentity) {
        int iCompare = Integer.compare(this.mType, networkIdentity.mType);
        if (iCompare == 0) {
            iCompare = Integer.compare(this.mSubType, networkIdentity.mSubType);
        }
        if (iCompare == 0 && this.mSubscriberId != null && networkIdentity.mSubscriberId != null) {
            iCompare = this.mSubscriberId.compareTo(networkIdentity.mSubscriberId);
        }
        if (iCompare == 0 && this.mNetworkId != null && networkIdentity.mNetworkId != null) {
            iCompare = this.mNetworkId.compareTo(networkIdentity.mNetworkId);
        }
        if (iCompare == 0) {
            iCompare = Boolean.compare(this.mRoaming, networkIdentity.mRoaming);
        }
        if (iCompare == 0) {
            iCompare = Boolean.compare(this.mMetered, networkIdentity.mMetered);
        }
        if (iCompare == 0) {
            return Boolean.compare(this.mDefaultNetwork, networkIdentity.mDefaultNetwork);
        }
        return iCompare;
    }
}
