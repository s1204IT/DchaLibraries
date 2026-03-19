package android.net;

import android.net.wifi.WifiInfo;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.BackupUtils;
import android.util.Log;
import com.android.internal.util.ArrayUtils;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Objects;

public class NetworkTemplate implements Parcelable {
    private static final int BACKUP_VERSION = 1;
    public static final int MATCH_BLUETOOTH = 8;
    public static final int MATCH_ETHERNET = 5;
    public static final int MATCH_MOBILE = 1;
    public static final int MATCH_MOBILE_WILDCARD = 6;
    public static final int MATCH_PROXY = 9;
    public static final int MATCH_WIFI = 4;
    public static final int MATCH_WIFI_WILDCARD = 7;
    private static final String TAG = "NetworkTemplate";
    private final int mDefaultNetwork;
    private final int mMatchRule;
    private final String[] mMatchSubscriberIds;
    private final int mMetered;
    private final String mNetworkId;
    private final int mRoaming;
    private final String mSubscriberId;
    private static boolean sForceAllNetworkTypes = false;
    public static final Parcelable.Creator<NetworkTemplate> CREATOR = new Parcelable.Creator<NetworkTemplate>() {
        @Override
        public NetworkTemplate createFromParcel(Parcel parcel) {
            return new NetworkTemplate(parcel);
        }

        @Override
        public NetworkTemplate[] newArray(int i) {
            return new NetworkTemplate[i];
        }
    };

    private static boolean isKnownMatchRule(int i) {
        if (i != 1) {
            switch (i) {
                case 4:
                case 5:
                case 6:
                case 7:
                case 8:
                case 9:
                    break;
                default:
                    return false;
            }
        }
        return true;
    }

    public static void forceAllNetworkTypes() {
        sForceAllNetworkTypes = true;
    }

    public static NetworkTemplate buildTemplateMobileAll(String str) {
        return new NetworkTemplate(1, str, null);
    }

    public static NetworkTemplate buildTemplateMobileWildcard() {
        return new NetworkTemplate(6, null, null);
    }

    public static NetworkTemplate buildTemplateWifiWildcard() {
        return new NetworkTemplate(7, null, null);
    }

    @Deprecated
    public static NetworkTemplate buildTemplateWifi() {
        return buildTemplateWifiWildcard();
    }

    public static NetworkTemplate buildTemplateWifi(String str) {
        return new NetworkTemplate(4, null, str);
    }

    public static NetworkTemplate buildTemplateEthernet() {
        return new NetworkTemplate(5, null, null);
    }

    public static NetworkTemplate buildTemplateBluetooth() {
        return new NetworkTemplate(8, null, null);
    }

    public static NetworkTemplate buildTemplateProxy() {
        return new NetworkTemplate(9, null, null);
    }

    public NetworkTemplate(int i, String str, String str2) {
        this(i, str, new String[]{str}, str2);
    }

    public NetworkTemplate(int i, String str, String[] strArr, String str2) {
        this(i, str, strArr, str2, -1, -1, -1);
    }

    public NetworkTemplate(int i, String str, String[] strArr, String str2, int i2, int i3, int i4) {
        this.mMatchRule = i;
        this.mSubscriberId = str;
        this.mMatchSubscriberIds = strArr;
        this.mNetworkId = str2;
        this.mMetered = i2;
        this.mRoaming = i3;
        this.mDefaultNetwork = i4;
        if (!isKnownMatchRule(i)) {
            Log.e(TAG, "Unknown network template rule " + i + " will not match any identity.");
        }
    }

    private NetworkTemplate(Parcel parcel) {
        this.mMatchRule = parcel.readInt();
        this.mSubscriberId = parcel.readString();
        this.mMatchSubscriberIds = parcel.createStringArray();
        this.mNetworkId = parcel.readString();
        this.mMetered = parcel.readInt();
        this.mRoaming = parcel.readInt();
        this.mDefaultNetwork = parcel.readInt();
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeInt(this.mMatchRule);
        parcel.writeString(this.mSubscriberId);
        parcel.writeStringArray(this.mMatchSubscriberIds);
        parcel.writeString(this.mNetworkId);
        parcel.writeInt(this.mMetered);
        parcel.writeInt(this.mRoaming);
        parcel.writeInt(this.mDefaultNetwork);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder("NetworkTemplate: ");
        sb.append("matchRule=");
        sb.append(getMatchRuleName(this.mMatchRule));
        if (this.mSubscriberId != null) {
            sb.append(", subscriberId=");
            sb.append(NetworkIdentity.scrubSubscriberId(this.mSubscriberId));
        }
        if (this.mMatchSubscriberIds != null) {
            sb.append(", matchSubscriberIds=");
            sb.append(Arrays.toString(NetworkIdentity.scrubSubscriberId(this.mMatchSubscriberIds)));
        }
        if (this.mNetworkId != null) {
            sb.append(", networkId=");
            sb.append(this.mNetworkId);
        }
        if (this.mMetered != -1) {
            sb.append(", metered=");
            sb.append(NetworkStats.meteredToString(this.mMetered));
        }
        if (this.mRoaming != -1) {
            sb.append(", roaming=");
            sb.append(NetworkStats.roamingToString(this.mRoaming));
        }
        if (this.mDefaultNetwork != -1) {
            sb.append(", defaultNetwork=");
            sb.append(NetworkStats.defaultNetworkToString(this.mDefaultNetwork));
        }
        return sb.toString();
    }

    public int hashCode() {
        return Objects.hash(Integer.valueOf(this.mMatchRule), this.mSubscriberId, this.mNetworkId, Integer.valueOf(this.mMetered), Integer.valueOf(this.mRoaming), Integer.valueOf(this.mDefaultNetwork));
    }

    public boolean equals(Object obj) {
        if (!(obj instanceof NetworkTemplate)) {
            return false;
        }
        NetworkTemplate networkTemplate = (NetworkTemplate) obj;
        return this.mMatchRule == networkTemplate.mMatchRule && Objects.equals(this.mSubscriberId, networkTemplate.mSubscriberId) && Objects.equals(this.mNetworkId, networkTemplate.mNetworkId) && this.mMetered == networkTemplate.mMetered && this.mRoaming == networkTemplate.mRoaming && this.mDefaultNetwork == networkTemplate.mDefaultNetwork;
    }

    public boolean isMatchRuleMobile() {
        int i = this.mMatchRule;
        return i == 1 || i == 6;
    }

    public boolean isPersistable() {
        switch (this.mMatchRule) {
            case 6:
            case 7:
                return false;
            default:
                return true;
        }
    }

    public int getMatchRule() {
        return this.mMatchRule;
    }

    public String getSubscriberId() {
        return this.mSubscriberId;
    }

    public String getNetworkId() {
        return this.mNetworkId;
    }

    public boolean matches(NetworkIdentity networkIdentity) {
        if (!matchesMetered(networkIdentity) || !matchesRoaming(networkIdentity) || !matchesDefaultNetwork(networkIdentity)) {
            return false;
        }
        int i = this.mMatchRule;
        if (i == 1) {
            return matchesMobile(networkIdentity);
        }
        switch (i) {
        }
        return false;
    }

    private boolean matchesMetered(NetworkIdentity networkIdentity) {
        if (this.mMetered == -1) {
            return true;
        }
        if (this.mMetered == 1 && networkIdentity.mMetered) {
            return true;
        }
        return this.mMetered == 0 && !networkIdentity.mMetered;
    }

    private boolean matchesRoaming(NetworkIdentity networkIdentity) {
        if (this.mRoaming == -1) {
            return true;
        }
        if (this.mRoaming == 1 && networkIdentity.mRoaming) {
            return true;
        }
        return this.mRoaming == 0 && !networkIdentity.mRoaming;
    }

    private boolean matchesDefaultNetwork(NetworkIdentity networkIdentity) {
        if (this.mDefaultNetwork == -1) {
            return true;
        }
        if (this.mDefaultNetwork == 1 && networkIdentity.mDefaultNetwork) {
            return true;
        }
        return this.mDefaultNetwork == 0 && !networkIdentity.mDefaultNetwork;
    }

    public boolean matchesSubscriberId(String str) {
        return ArrayUtils.contains(this.mMatchSubscriberIds, str);
    }

    private boolean matchesMobile(NetworkIdentity networkIdentity) {
        if (networkIdentity.mType == 6) {
            return true;
        }
        return (sForceAllNetworkTypes || (networkIdentity.mType == 0 && networkIdentity.mMetered)) && !ArrayUtils.isEmpty(this.mMatchSubscriberIds) && ArrayUtils.contains(this.mMatchSubscriberIds, networkIdentity.mSubscriberId);
    }

    private boolean matchesWifi(NetworkIdentity networkIdentity) {
        if (networkIdentity.mType == 1) {
            return Objects.equals(WifiInfo.removeDoubleQuotes(this.mNetworkId), WifiInfo.removeDoubleQuotes(networkIdentity.mNetworkId));
        }
        return false;
    }

    private boolean matchesEthernet(NetworkIdentity networkIdentity) {
        if (networkIdentity.mType == 9) {
            return true;
        }
        return false;
    }

    private boolean matchesMobileWildcard(NetworkIdentity networkIdentity) {
        if (networkIdentity.mType == 6 || sForceAllNetworkTypes) {
            return true;
        }
        return networkIdentity.mType == 0 && networkIdentity.mMetered;
    }

    private boolean matchesWifiWildcard(NetworkIdentity networkIdentity) {
        int i = networkIdentity.mType;
        return i == 1 || i == 13;
    }

    private boolean matchesBluetooth(NetworkIdentity networkIdentity) {
        if (networkIdentity.mType == 7) {
            return true;
        }
        return false;
    }

    private boolean matchesProxy(NetworkIdentity networkIdentity) {
        return networkIdentity.mType == 16;
    }

    private static String getMatchRuleName(int i) {
        if (i == 1) {
            return "MOBILE";
        }
        switch (i) {
            case 4:
                return "WIFI";
            case 5:
                return "ETHERNET";
            case 6:
                return "MOBILE_WILDCARD";
            case 7:
                return "WIFI_WILDCARD";
            case 8:
                return "BLUETOOTH";
            case 9:
                return "PROXY";
            default:
                return "UNKNOWN(" + i + ")";
        }
    }

    public static NetworkTemplate normalize(NetworkTemplate networkTemplate, String[] strArr) {
        if (networkTemplate.isMatchRuleMobile() && ArrayUtils.contains(strArr, networkTemplate.mSubscriberId)) {
            return new NetworkTemplate(networkTemplate.mMatchRule, strArr[0], strArr, networkTemplate.mNetworkId);
        }
        return networkTemplate;
    }

    public byte[] getBytesForBackup() throws IOException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        DataOutputStream dataOutputStream = new DataOutputStream(byteArrayOutputStream);
        dataOutputStream.writeInt(1);
        dataOutputStream.writeInt(this.mMatchRule);
        BackupUtils.writeString(dataOutputStream, this.mSubscriberId);
        BackupUtils.writeString(dataOutputStream, this.mNetworkId);
        return byteArrayOutputStream.toByteArray();
    }

    public static NetworkTemplate getNetworkTemplateFromBackup(DataInputStream dataInputStream) throws BackupUtils.BadVersionException, IOException {
        int i = dataInputStream.readInt();
        if (i < 1 || i > 1) {
            throw new BackupUtils.BadVersionException("Unknown Backup Serialization Version");
        }
        int i2 = dataInputStream.readInt();
        String string = BackupUtils.readString(dataInputStream);
        String string2 = BackupUtils.readString(dataInputStream);
        if (!isKnownMatchRule(i2)) {
            throw new BackupUtils.BadVersionException("Restored network template contains unknown match rule " + i2);
        }
        return new NetworkTemplate(i2, string, string2);
    }
}
