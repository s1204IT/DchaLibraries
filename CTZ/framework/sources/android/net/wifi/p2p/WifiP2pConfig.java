package android.net.wifi.p2p;

import android.net.wifi.WifiEnterpriseConfig;
import android.net.wifi.WpsInfo;
import android.os.Parcel;
import android.os.Parcelable;

public class WifiP2pConfig implements Parcelable {
    public static final Parcelable.Creator<WifiP2pConfig> CREATOR = new Parcelable.Creator<WifiP2pConfig>() {
        @Override
        public WifiP2pConfig createFromParcel(Parcel parcel) {
            WifiP2pConfig wifiP2pConfig = new WifiP2pConfig();
            wifiP2pConfig.deviceAddress = parcel.readString();
            wifiP2pConfig.wps = (WpsInfo) parcel.readParcelable(null);
            wifiP2pConfig.groupOwnerIntent = parcel.readInt();
            wifiP2pConfig.netId = parcel.readInt();
            return wifiP2pConfig;
        }

        @Override
        public WifiP2pConfig[] newArray(int i) {
            return new WifiP2pConfig[i];
        }
    };
    public static final int MAX_GROUP_OWNER_INTENT = 15;
    public static final int MIN_GROUP_OWNER_INTENT = 0;
    public String deviceAddress;
    public int groupOwnerIntent;
    public String interfaceAddress;
    public int netId;
    private int preferOperFreq;
    public WpsInfo wps;

    public WifiP2pConfig() {
        this.deviceAddress = "";
        this.interfaceAddress = "00:00:00:00:00:00";
        this.groupOwnerIntent = -1;
        this.netId = -2;
        this.preferOperFreq = -1;
        this.wps = new WpsInfo();
        this.wps.setup = 0;
    }

    public void invalidate() {
        this.deviceAddress = "";
    }

    public WifiP2pConfig(String str) throws IllegalArgumentException {
        int i;
        this.deviceAddress = "";
        this.interfaceAddress = "00:00:00:00:00:00";
        this.groupOwnerIntent = -1;
        this.netId = -2;
        this.preferOperFreq = -1;
        String[] strArrSplit = str.split(WifiEnterpriseConfig.CA_CERT_ALIAS_DELIMITER);
        if (strArrSplit.length < 2 || !strArrSplit[0].equals("P2P-GO-NEG-REQUEST")) {
            throw new IllegalArgumentException("Malformed supplicant event");
        }
        this.deviceAddress = strArrSplit[1];
        this.wps = new WpsInfo();
        if (strArrSplit.length > 2) {
            try {
                i = Integer.parseInt(strArrSplit[2].split("=")[1]);
            } catch (NumberFormatException e) {
                i = 0;
            }
            if (i == 1) {
                this.wps.setup = 1;
                return;
            }
            switch (i) {
                case 4:
                    this.wps.setup = 0;
                    return;
                case 5:
                    this.wps.setup = 2;
                    return;
                default:
                    this.wps.setup = 0;
                    return;
            }
        }
    }

    public void setPreferOperFreq(int i) {
        this.preferOperFreq = i;
    }

    public int getPreferOperFreq() {
        return this.preferOperFreq;
    }

    public String toString() {
        StringBuffer stringBuffer = new StringBuffer();
        stringBuffer.append("\n address: ");
        stringBuffer.append(this.deviceAddress);
        stringBuffer.append("\n wps: ");
        stringBuffer.append(this.wps);
        stringBuffer.append("\n groupOwnerIntent: ");
        stringBuffer.append(this.groupOwnerIntent);
        stringBuffer.append("\n persist: ");
        stringBuffer.append(this.netId);
        return stringBuffer.toString();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public WifiP2pConfig(WifiP2pConfig wifiP2pConfig) {
        this.deviceAddress = "";
        this.interfaceAddress = "00:00:00:00:00:00";
        this.groupOwnerIntent = -1;
        this.netId = -2;
        this.preferOperFreq = -1;
        if (wifiP2pConfig != null) {
            this.deviceAddress = wifiP2pConfig.deviceAddress;
            this.wps = new WpsInfo(wifiP2pConfig.wps);
            this.groupOwnerIntent = wifiP2pConfig.groupOwnerIntent;
            this.netId = wifiP2pConfig.netId;
        }
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeString(this.deviceAddress);
        parcel.writeParcelable(this.wps, i);
        parcel.writeInt(this.groupOwnerIntent);
        parcel.writeInt(this.netId);
    }
}
