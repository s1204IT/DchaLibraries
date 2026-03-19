package android.net.wifi;

import android.net.NetworkInfo;
import android.net.NetworkUtils;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.EnumMap;
import java.util.Locale;

public class WifiInfo implements Parcelable {
    public static final Parcelable.Creator<WifiInfo> CREATOR;
    public static final String DEFAULT_MAC_ADDRESS = "02:00:00:00:00:00";
    public static final String FREQUENCY_UNITS = "MHz";
    public static final int INVALID_RSSI = -127;
    public static final String LINK_SPEED_UNITS = "Mbps";
    public static final int MAX_RSSI = 200;
    public static final int MIN_RSSI = -126;
    private static final String TAG = "WifiInfo";
    private static final EnumMap<SupplicantState, NetworkInfo.DetailedState> stateMap = new EnumMap<>(SupplicantState.class);
    private String mBSSID;
    private boolean mEphemeral;
    private int mFrequency;
    private InetAddress mIpAddress;
    private int mLinkSpeed;
    private String mMacAddress;
    private boolean mMeteredHint;
    private int mNetworkId;
    private int mRssi;
    private SupplicantState mSupplicantState;
    private WifiSsid mWifiSsid;
    public long rxSuccess;
    public double rxSuccessRate;
    public int score;
    public long txBad;
    public double txBadRate;
    public long txRetries;
    public double txRetriesRate;
    public long txSuccess;
    public double txSuccessRate;

    static {
        stateMap.put(SupplicantState.DISCONNECTED, NetworkInfo.DetailedState.DISCONNECTED);
        stateMap.put(SupplicantState.INTERFACE_DISABLED, NetworkInfo.DetailedState.DISCONNECTED);
        stateMap.put(SupplicantState.INACTIVE, NetworkInfo.DetailedState.IDLE);
        stateMap.put(SupplicantState.SCANNING, NetworkInfo.DetailedState.SCANNING);
        stateMap.put(SupplicantState.AUTHENTICATING, NetworkInfo.DetailedState.CONNECTING);
        stateMap.put(SupplicantState.ASSOCIATING, NetworkInfo.DetailedState.CONNECTING);
        stateMap.put(SupplicantState.ASSOCIATED, NetworkInfo.DetailedState.CONNECTING);
        stateMap.put(SupplicantState.FOUR_WAY_HANDSHAKE, NetworkInfo.DetailedState.AUTHENTICATING);
        stateMap.put(SupplicantState.GROUP_HANDSHAKE, NetworkInfo.DetailedState.AUTHENTICATING);
        stateMap.put(SupplicantState.COMPLETED, NetworkInfo.DetailedState.OBTAINING_IPADDR);
        stateMap.put(SupplicantState.DORMANT, NetworkInfo.DetailedState.DISCONNECTED);
        stateMap.put(SupplicantState.UNINITIALIZED, NetworkInfo.DetailedState.IDLE);
        stateMap.put(SupplicantState.INVALID, NetworkInfo.DetailedState.FAILED);
        CREATOR = new Parcelable.Creator<WifiInfo>() {
            @Override
            public WifiInfo createFromParcel(Parcel parcel) {
                WifiInfo wifiInfo = new WifiInfo();
                wifiInfo.setNetworkId(parcel.readInt());
                wifiInfo.setRssi(parcel.readInt());
                wifiInfo.setLinkSpeed(parcel.readInt());
                wifiInfo.setFrequency(parcel.readInt());
                if (parcel.readByte() == 1) {
                    try {
                        wifiInfo.setInetAddress(InetAddress.getByAddress(parcel.createByteArray()));
                    } catch (UnknownHostException e) {
                    }
                }
                if (parcel.readInt() == 1) {
                    wifiInfo.mWifiSsid = WifiSsid.CREATOR.createFromParcel(parcel);
                }
                wifiInfo.mBSSID = parcel.readString();
                wifiInfo.mMacAddress = parcel.readString();
                wifiInfo.mMeteredHint = parcel.readInt() != 0;
                wifiInfo.mEphemeral = parcel.readInt() != 0;
                wifiInfo.score = parcel.readInt();
                wifiInfo.txSuccessRate = parcel.readDouble();
                wifiInfo.txRetriesRate = parcel.readDouble();
                wifiInfo.txBadRate = parcel.readDouble();
                wifiInfo.rxSuccessRate = parcel.readDouble();
                wifiInfo.mSupplicantState = SupplicantState.CREATOR.createFromParcel(parcel);
                return wifiInfo;
            }

            @Override
            public WifiInfo[] newArray(int i) {
                return new WifiInfo[i];
            }
        };
    }

    public WifiInfo() {
        this.mMacAddress = "02:00:00:00:00:00";
        this.mWifiSsid = null;
        this.mBSSID = null;
        this.mNetworkId = -1;
        this.mSupplicantState = SupplicantState.UNINITIALIZED;
        this.mRssi = -127;
        this.mLinkSpeed = -1;
        this.mFrequency = -1;
    }

    public void reset() {
        setInetAddress(null);
        setBSSID(null);
        setSSID(null);
        setNetworkId(-1);
        setRssi(-127);
        setLinkSpeed(-1);
        setFrequency(-1);
        setMeteredHint(false);
        setEphemeral(false);
        this.txBad = 0L;
        this.txSuccess = 0L;
        this.rxSuccess = 0L;
        this.txRetries = 0L;
        this.txBadRate = 0.0d;
        this.txSuccessRate = 0.0d;
        this.rxSuccessRate = 0.0d;
        this.txRetriesRate = 0.0d;
        this.score = 0;
    }

    public WifiInfo(WifiInfo wifiInfo) {
        this.mMacAddress = "02:00:00:00:00:00";
        if (wifiInfo != null) {
            this.mSupplicantState = wifiInfo.mSupplicantState;
            this.mBSSID = wifiInfo.mBSSID;
            this.mWifiSsid = wifiInfo.mWifiSsid;
            this.mNetworkId = wifiInfo.mNetworkId;
            this.mRssi = wifiInfo.mRssi;
            this.mLinkSpeed = wifiInfo.mLinkSpeed;
            this.mFrequency = wifiInfo.mFrequency;
            this.mIpAddress = wifiInfo.mIpAddress;
            this.mMacAddress = wifiInfo.mMacAddress;
            this.mMeteredHint = wifiInfo.mMeteredHint;
            this.mEphemeral = wifiInfo.mEphemeral;
            this.txBad = wifiInfo.txBad;
            this.txRetries = wifiInfo.txRetries;
            this.txSuccess = wifiInfo.txSuccess;
            this.rxSuccess = wifiInfo.rxSuccess;
            this.txBadRate = wifiInfo.txBadRate;
            this.txRetriesRate = wifiInfo.txRetriesRate;
            this.txSuccessRate = wifiInfo.txSuccessRate;
            this.rxSuccessRate = wifiInfo.rxSuccessRate;
            this.score = wifiInfo.score;
        }
    }

    public void setSSID(WifiSsid wifiSsid) {
        this.mWifiSsid = wifiSsid;
    }

    public String getSSID() {
        if (this.mWifiSsid != null) {
            String string = this.mWifiSsid.toString();
            if (!TextUtils.isEmpty(string)) {
                return "\"" + string + "\"";
            }
            String hexString = this.mWifiSsid.getHexString();
            return hexString != null ? hexString : WifiSsid.NONE;
        }
        return WifiSsid.NONE;
    }

    public WifiSsid getWifiSsid() {
        return this.mWifiSsid;
    }

    public void setBSSID(String str) {
        this.mBSSID = str;
    }

    public String getBSSID() {
        return this.mBSSID;
    }

    public int getRssi() {
        return this.mRssi;
    }

    public void setRssi(int i) {
        if (i < -127) {
            i = -127;
        }
        if (i > 200) {
            i = 200;
        }
        this.mRssi = i;
    }

    public int getLinkSpeed() {
        return this.mLinkSpeed;
    }

    public void setLinkSpeed(int i) {
        this.mLinkSpeed = i;
    }

    public int getFrequency() {
        return this.mFrequency;
    }

    public void setFrequency(int i) {
        this.mFrequency = i;
    }

    public boolean is24GHz() {
        return ScanResult.is24GHz(this.mFrequency);
    }

    public boolean is5GHz() {
        return ScanResult.is5GHz(this.mFrequency);
    }

    public void setMacAddress(String str) {
        this.mMacAddress = str;
    }

    public String getMacAddress() {
        return this.mMacAddress;
    }

    public boolean hasRealMacAddress() {
        return (this.mMacAddress == null || "02:00:00:00:00:00".equals(this.mMacAddress)) ? false : true;
    }

    public void setMeteredHint(boolean z) {
        this.mMeteredHint = z;
    }

    public boolean getMeteredHint() {
        return this.mMeteredHint;
    }

    public void setEphemeral(boolean z) {
        this.mEphemeral = z;
    }

    public boolean isEphemeral() {
        return this.mEphemeral;
    }

    public void setNetworkId(int i) {
        this.mNetworkId = i;
    }

    public int getNetworkId() {
        return this.mNetworkId;
    }

    public SupplicantState getSupplicantState() {
        return this.mSupplicantState;
    }

    public void setSupplicantState(SupplicantState supplicantState) {
        this.mSupplicantState = supplicantState;
    }

    public void setInetAddress(InetAddress inetAddress) {
        this.mIpAddress = inetAddress;
    }

    public int getIpAddress() {
        if (this.mIpAddress instanceof Inet4Address) {
            return NetworkUtils.inetAddressToInt((Inet4Address) this.mIpAddress);
        }
        return 0;
    }

    public boolean getHiddenSSID() {
        if (this.mWifiSsid == null) {
            return false;
        }
        return this.mWifiSsid.isHidden();
    }

    public static NetworkInfo.DetailedState getDetailedStateOf(SupplicantState supplicantState) {
        return stateMap.get(supplicantState);
    }

    void setSupplicantState(String str) {
        this.mSupplicantState = valueOf(str);
    }

    static SupplicantState valueOf(String str) {
        if ("4WAY_HANDSHAKE".equalsIgnoreCase(str)) {
            return SupplicantState.FOUR_WAY_HANDSHAKE;
        }
        try {
            return SupplicantState.valueOf(str.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return SupplicantState.INVALID;
        }
    }

    public static String removeDoubleQuotes(String str) {
        if (str == null) {
            return null;
        }
        int length = str.length();
        if (length > 1 && str.charAt(0) == '\"') {
            int i = length - 1;
            if (str.charAt(i) == '\"') {
                return str.substring(1, i);
            }
        }
        return str;
    }

    public String toString() {
        StringBuffer stringBuffer = new StringBuffer();
        stringBuffer.append("SSID: ");
        stringBuffer.append(this.mWifiSsid == null ? WifiSsid.NONE : this.mWifiSsid);
        stringBuffer.append(", BSSID: ");
        stringBuffer.append(this.mBSSID == null ? "<none>" : this.mBSSID);
        stringBuffer.append(", MAC: ");
        stringBuffer.append(this.mMacAddress == null ? "<none>" : this.mMacAddress);
        stringBuffer.append(", Supplicant state: ");
        stringBuffer.append(this.mSupplicantState != null ? this.mSupplicantState : "<none>");
        stringBuffer.append(", RSSI: ");
        stringBuffer.append(this.mRssi);
        stringBuffer.append(", Link speed: ");
        stringBuffer.append(this.mLinkSpeed);
        stringBuffer.append(LINK_SPEED_UNITS);
        stringBuffer.append(", Frequency: ");
        stringBuffer.append(this.mFrequency);
        stringBuffer.append(FREQUENCY_UNITS);
        stringBuffer.append(", Net ID: ");
        stringBuffer.append(this.mNetworkId);
        stringBuffer.append(", Metered hint: ");
        stringBuffer.append(this.mMeteredHint);
        stringBuffer.append(", score: ");
        stringBuffer.append(Integer.toString(this.score));
        return stringBuffer.toString();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeInt(this.mNetworkId);
        parcel.writeInt(this.mRssi);
        parcel.writeInt(this.mLinkSpeed);
        parcel.writeInt(this.mFrequency);
        if (this.mIpAddress != null) {
            parcel.writeByte((byte) 1);
            parcel.writeByteArray(this.mIpAddress.getAddress());
        } else {
            parcel.writeByte((byte) 0);
        }
        if (this.mWifiSsid != null) {
            parcel.writeInt(1);
            this.mWifiSsid.writeToParcel(parcel, i);
        } else {
            parcel.writeInt(0);
        }
        parcel.writeString(this.mBSSID);
        parcel.writeString(this.mMacAddress);
        parcel.writeInt(this.mMeteredHint ? 1 : 0);
        parcel.writeInt(this.mEphemeral ? 1 : 0);
        parcel.writeInt(this.score);
        parcel.writeDouble(this.txSuccessRate);
        parcel.writeDouble(this.txRetriesRate);
        parcel.writeDouble(this.txBadRate);
        parcel.writeDouble(this.rxSuccessRate);
        this.mSupplicantState.writeToParcel(parcel, i);
    }
}
