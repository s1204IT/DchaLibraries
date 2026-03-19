package com.android.internal.net;

import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;

public class VpnProfile implements Cloneable, Parcelable {
    public static final Parcelable.Creator<VpnProfile> CREATOR = new Parcelable.Creator<VpnProfile>() {
        @Override
        public VpnProfile createFromParcel(Parcel parcel) {
            return new VpnProfile(parcel);
        }

        @Override
        public VpnProfile[] newArray(int i) {
            return new VpnProfile[i];
        }
    };
    private static final String TAG = "VpnProfile";
    public static final int TYPE_IPSEC_HYBRID_RSA = 5;
    public static final int TYPE_IPSEC_XAUTH_PSK = 3;
    public static final int TYPE_IPSEC_XAUTH_RSA = 4;
    public static final int TYPE_L2TP_IPSEC_PSK = 1;
    public static final int TYPE_L2TP_IPSEC_RSA = 2;
    public static final int TYPE_MAX = 5;
    public static final int TYPE_PPTP = 0;
    public String dnsServers;
    public String ipsecCaCert;
    public String ipsecIdentifier;
    public String ipsecSecret;
    public String ipsecServerCert;
    public String ipsecUserCert;
    public final String key;
    public String l2tpSecret;
    public boolean mppe;
    public String name;
    public String password;
    public String routes;
    public boolean saveLogin;
    public String searchDomains;
    public String server;
    public int type;
    public String username;

    public VpnProfile(String str) {
        this.name = "";
        this.type = 0;
        this.server = "";
        this.username = "";
        this.password = "";
        this.dnsServers = "";
        this.searchDomains = "";
        this.routes = "";
        this.mppe = true;
        this.l2tpSecret = "";
        this.ipsecIdentifier = "";
        this.ipsecSecret = "";
        this.ipsecUserCert = "";
        this.ipsecCaCert = "";
        this.ipsecServerCert = "";
        this.saveLogin = false;
        this.key = str;
    }

    public VpnProfile(Parcel parcel) {
        boolean z;
        this.name = "";
        this.type = 0;
        this.server = "";
        this.username = "";
        this.password = "";
        this.dnsServers = "";
        this.searchDomains = "";
        this.routes = "";
        this.mppe = true;
        this.l2tpSecret = "";
        this.ipsecIdentifier = "";
        this.ipsecSecret = "";
        this.ipsecUserCert = "";
        this.ipsecCaCert = "";
        this.ipsecServerCert = "";
        this.saveLogin = false;
        this.key = parcel.readString();
        this.name = parcel.readString();
        this.type = parcel.readInt();
        this.server = parcel.readString();
        this.username = parcel.readString();
        this.password = parcel.readString();
        this.dnsServers = parcel.readString();
        this.searchDomains = parcel.readString();
        this.routes = parcel.readString();
        if (parcel.readInt() == 0) {
            z = false;
        } else {
            z = true;
        }
        this.mppe = z;
        this.l2tpSecret = parcel.readString();
        this.ipsecIdentifier = parcel.readString();
        this.ipsecSecret = parcel.readString();
        this.ipsecUserCert = parcel.readString();
        this.ipsecCaCert = parcel.readString();
        this.ipsecServerCert = parcel.readString();
        this.saveLogin = parcel.readInt() != 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeString(this.key);
        parcel.writeString(this.name);
        parcel.writeInt(this.type);
        parcel.writeString(this.server);
        parcel.writeString(this.username);
        parcel.writeString(this.password);
        parcel.writeString(this.dnsServers);
        parcel.writeString(this.searchDomains);
        parcel.writeString(this.routes);
        parcel.writeInt(this.mppe ? 1 : 0);
        parcel.writeString(this.l2tpSecret);
        parcel.writeString(this.ipsecIdentifier);
        parcel.writeString(this.ipsecSecret);
        parcel.writeString(this.ipsecUserCert);
        parcel.writeString(this.ipsecCaCert);
        parcel.writeString(this.ipsecServerCert);
        parcel.writeInt(this.saveLogin ? 1 : 0);
    }

    public static VpnProfile decode(String str, byte[] bArr) {
        if (str == null) {
            return null;
        }
        try {
            String[] strArrSplit = new String(bArr, StandardCharsets.UTF_8).split("\u0000", -1);
            if (strArrSplit.length >= 14 && strArrSplit.length <= 15) {
                VpnProfile vpnProfile = new VpnProfile(str);
                boolean z = false;
                vpnProfile.name = strArrSplit[0];
                vpnProfile.type = Integer.parseInt(strArrSplit[1]);
                if (vpnProfile.type >= 0 && vpnProfile.type <= 5) {
                    vpnProfile.server = strArrSplit[2];
                    vpnProfile.username = strArrSplit[3];
                    vpnProfile.password = strArrSplit[4];
                    vpnProfile.dnsServers = strArrSplit[5];
                    vpnProfile.searchDomains = strArrSplit[6];
                    vpnProfile.routes = strArrSplit[7];
                    vpnProfile.mppe = Boolean.parseBoolean(strArrSplit[8]);
                    vpnProfile.l2tpSecret = strArrSplit[9];
                    vpnProfile.ipsecIdentifier = strArrSplit[10];
                    vpnProfile.ipsecSecret = strArrSplit[11];
                    vpnProfile.ipsecUserCert = strArrSplit[12];
                    vpnProfile.ipsecCaCert = strArrSplit[13];
                    vpnProfile.ipsecServerCert = strArrSplit.length > 14 ? strArrSplit[14] : "";
                    if (!vpnProfile.username.isEmpty() || !vpnProfile.password.isEmpty()) {
                        z = true;
                    }
                    vpnProfile.saveLogin = z;
                    return vpnProfile;
                }
                return null;
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    public byte[] encode() {
        StringBuilder sb = new StringBuilder(this.name);
        sb.append((char) 0);
        sb.append(this.type);
        sb.append((char) 0);
        sb.append(this.server);
        sb.append((char) 0);
        sb.append(this.saveLogin ? this.username : "");
        sb.append((char) 0);
        sb.append(this.saveLogin ? this.password : "");
        sb.append((char) 0);
        sb.append(this.dnsServers);
        sb.append((char) 0);
        sb.append(this.searchDomains);
        sb.append((char) 0);
        sb.append(this.routes);
        sb.append((char) 0);
        sb.append(this.mppe);
        sb.append((char) 0);
        sb.append(this.l2tpSecret);
        sb.append((char) 0);
        sb.append(this.ipsecIdentifier);
        sb.append((char) 0);
        sb.append(this.ipsecSecret);
        sb.append((char) 0);
        sb.append(this.ipsecUserCert);
        sb.append((char) 0);
        sb.append(this.ipsecCaCert);
        sb.append((char) 0);
        sb.append(this.ipsecServerCert);
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    public boolean isValidLockdownProfile() {
        return isTypeValidForLockdown() && isServerAddressNumeric() && hasDns() && areDnsAddressesNumeric();
    }

    public boolean isTypeValidForLockdown() {
        return this.type != 0;
    }

    public boolean isServerAddressNumeric() {
        try {
            InetAddress.parseNumericAddress(this.server);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    public boolean hasDns() {
        return !TextUtils.isEmpty(this.dnsServers);
    }

    public boolean areDnsAddressesNumeric() {
        try {
            for (String str : this.dnsServers.split(" +")) {
                InetAddress.parseNumericAddress(str);
            }
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    @Override
    public int describeContents() {
        return 0;
    }
}
