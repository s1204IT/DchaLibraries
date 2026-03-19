package android.telephony.data;

import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;
import android.provider.Telephony;
import android.telephony.ServiceState;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.Log;
import com.android.internal.telephony.PhoneConstants;
import com.android.internal.telephony.RILConstants;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Map;
import java.util.Objects;

public class ApnSetting implements Parcelable {
    private static final Map<Integer, String> APN_TYPE_INT_MAP;
    private static final Map<String, Integer> APN_TYPE_STRING_MAP = new ArrayMap();
    public static final int AUTH_TYPE_CHAP = 2;
    public static final int AUTH_TYPE_NONE = 0;
    public static final int AUTH_TYPE_PAP = 1;
    public static final int AUTH_TYPE_PAP_OR_CHAP = 3;
    public static final Parcelable.Creator<ApnSetting> CREATOR;
    private static final String LOG_TAG = "ApnSetting";
    public static final int MVNO_TYPE_GID = 2;
    public static final int MVNO_TYPE_ICCID = 3;
    public static final int MVNO_TYPE_IMSI = 1;
    private static final Map<Integer, String> MVNO_TYPE_INT_MAP;
    public static final int MVNO_TYPE_SPN = 0;
    private static final Map<String, Integer> MVNO_TYPE_STRING_MAP;
    private static final int NOT_IN_MAP_INT = -1;
    private static final int NO_PORT_SPECIFIED = -1;
    private static final Map<Integer, String> PROTOCOL_INT_MAP;
    public static final int PROTOCOL_IP = 0;
    public static final int PROTOCOL_IPV4V6 = 2;
    public static final int PROTOCOL_IPV6 = 1;
    public static final int PROTOCOL_PPP = 3;
    private static final Map<String, Integer> PROTOCOL_STRING_MAP;
    private static final int TYPE_ALL_BUT_IA = 767;
    public static final int TYPE_CBS = 128;
    public static final int TYPE_DEFAULT = 17;
    public static final int TYPE_DUN = 8;
    public static final int TYPE_EMERGENCY = 512;
    public static final int TYPE_FOTA = 32;
    public static final int TYPE_HIPRI = 16;
    public static final int TYPE_IA = 256;
    public static final int TYPE_IMS = 64;
    public static final int TYPE_MMS = 2;
    public static final int TYPE_SUPL = 4;
    private static final boolean VDBG = false;
    private final String mApnName;
    private final int mApnTypeBitmask;
    private final int mAuthType;
    private final boolean mCarrierEnabled;
    private final String mEntryName;
    private final int mId;
    private final int mMaxConns;
    private final int mMaxConnsTime;
    private final InetAddress mMmsProxyAddress;
    private final int mMmsProxyPort;
    private final Uri mMmsc;
    private final boolean mModemCognitive;
    private final int mMtu;
    private final String mMvnoMatchData;
    private final int mMvnoType;
    private final int mNetworkTypeBitmask;
    private final String mOperatorNumeric;
    private final String mPassword;
    private boolean mPermanentFailed;
    private final int mProfileId;
    private final int mProtocol;
    private final InetAddress mProxyAddress;
    private final int mProxyPort;
    private final int mRoamingProtocol;
    private final String mUser;
    private final int mWaitTime;

    @Retention(RetentionPolicy.SOURCE)
    public @interface ApnType {
    }

    @Retention(RetentionPolicy.SOURCE)
    public @interface AuthType {
    }

    @Retention(RetentionPolicy.SOURCE)
    public @interface MvnoType {
    }

    @Retention(RetentionPolicy.SOURCE)
    public @interface ProtocolType {
    }

    static {
        APN_TYPE_STRING_MAP.put(PhoneConstants.APN_TYPE_ALL, 767);
        APN_TYPE_STRING_MAP.put(PhoneConstants.APN_TYPE_DEFAULT, 17);
        APN_TYPE_STRING_MAP.put(PhoneConstants.APN_TYPE_MMS, 2);
        APN_TYPE_STRING_MAP.put(PhoneConstants.APN_TYPE_SUPL, 4);
        APN_TYPE_STRING_MAP.put(PhoneConstants.APN_TYPE_DUN, 8);
        APN_TYPE_STRING_MAP.put(PhoneConstants.APN_TYPE_HIPRI, 16);
        APN_TYPE_STRING_MAP.put(PhoneConstants.APN_TYPE_FOTA, 32);
        APN_TYPE_STRING_MAP.put(PhoneConstants.APN_TYPE_IMS, 64);
        APN_TYPE_STRING_MAP.put(PhoneConstants.APN_TYPE_CBS, 128);
        APN_TYPE_STRING_MAP.put(PhoneConstants.APN_TYPE_IA, 256);
        APN_TYPE_STRING_MAP.put(PhoneConstants.APN_TYPE_EMERGENCY, 512);
        APN_TYPE_INT_MAP = new ArrayMap();
        APN_TYPE_INT_MAP.put(17, PhoneConstants.APN_TYPE_DEFAULT);
        APN_TYPE_INT_MAP.put(2, PhoneConstants.APN_TYPE_MMS);
        APN_TYPE_INT_MAP.put(4, PhoneConstants.APN_TYPE_SUPL);
        APN_TYPE_INT_MAP.put(8, PhoneConstants.APN_TYPE_DUN);
        APN_TYPE_INT_MAP.put(16, PhoneConstants.APN_TYPE_HIPRI);
        APN_TYPE_INT_MAP.put(32, PhoneConstants.APN_TYPE_FOTA);
        APN_TYPE_INT_MAP.put(64, PhoneConstants.APN_TYPE_IMS);
        APN_TYPE_INT_MAP.put(128, PhoneConstants.APN_TYPE_CBS);
        APN_TYPE_INT_MAP.put(256, PhoneConstants.APN_TYPE_IA);
        APN_TYPE_INT_MAP.put(512, PhoneConstants.APN_TYPE_EMERGENCY);
        PROTOCOL_STRING_MAP = new ArrayMap();
        PROTOCOL_STRING_MAP.put(RILConstants.SETUP_DATA_PROTOCOL_IP, 0);
        PROTOCOL_STRING_MAP.put(RILConstants.SETUP_DATA_PROTOCOL_IPV6, 1);
        PROTOCOL_STRING_MAP.put(RILConstants.SETUP_DATA_PROTOCOL_IPV4V6, 2);
        PROTOCOL_STRING_MAP.put("PPP", 3);
        PROTOCOL_INT_MAP = new ArrayMap();
        PROTOCOL_INT_MAP.put(0, RILConstants.SETUP_DATA_PROTOCOL_IP);
        PROTOCOL_INT_MAP.put(1, RILConstants.SETUP_DATA_PROTOCOL_IPV6);
        PROTOCOL_INT_MAP.put(2, RILConstants.SETUP_DATA_PROTOCOL_IPV4V6);
        PROTOCOL_INT_MAP.put(3, "PPP");
        MVNO_TYPE_STRING_MAP = new ArrayMap();
        MVNO_TYPE_STRING_MAP.put("spn", 0);
        MVNO_TYPE_STRING_MAP.put("imsi", 1);
        MVNO_TYPE_STRING_MAP.put("gid", 2);
        MVNO_TYPE_STRING_MAP.put("iccid", 3);
        MVNO_TYPE_INT_MAP = new ArrayMap();
        MVNO_TYPE_INT_MAP.put(0, "spn");
        MVNO_TYPE_INT_MAP.put(1, "imsi");
        MVNO_TYPE_INT_MAP.put(2, "gid");
        MVNO_TYPE_INT_MAP.put(3, "iccid");
        CREATOR = new Parcelable.Creator<ApnSetting>() {
            @Override
            public ApnSetting createFromParcel(Parcel parcel) {
                return ApnSetting.readFromParcel(parcel);
            }

            @Override
            public ApnSetting[] newArray(int i) {
                return new ApnSetting[i];
            }
        };
    }

    public int getMtu() {
        return this.mMtu;
    }

    public int getProfileId() {
        return this.mProfileId;
    }

    public boolean getModemCognitive() {
        return this.mModemCognitive;
    }

    public int getMaxConns() {
        return this.mMaxConns;
    }

    public int getWaitTime() {
        return this.mWaitTime;
    }

    public int getMaxConnsTime() {
        return this.mMaxConnsTime;
    }

    public String getMvnoMatchData() {
        return this.mMvnoMatchData;
    }

    public boolean getPermanentFailed() {
        return this.mPermanentFailed;
    }

    public void setPermanentFailed(boolean z) {
        this.mPermanentFailed = z;
    }

    public String getEntryName() {
        return this.mEntryName;
    }

    public String getApnName() {
        return this.mApnName;
    }

    public InetAddress getProxyAddress() {
        return this.mProxyAddress;
    }

    public int getProxyPort() {
        return this.mProxyPort;
    }

    public Uri getMmsc() {
        return this.mMmsc;
    }

    public InetAddress getMmsProxyAddress() {
        return this.mMmsProxyAddress;
    }

    public int getMmsProxyPort() {
        return this.mMmsProxyPort;
    }

    public String getUser() {
        return this.mUser;
    }

    public String getPassword() {
        return this.mPassword;
    }

    public int getAuthType() {
        return this.mAuthType;
    }

    public int getApnTypeBitmask() {
        return this.mApnTypeBitmask;
    }

    public int getId() {
        return this.mId;
    }

    public String getOperatorNumeric() {
        return this.mOperatorNumeric;
    }

    public int getProtocol() {
        return this.mProtocol;
    }

    public int getRoamingProtocol() {
        return this.mRoamingProtocol;
    }

    public boolean isEnabled() {
        return this.mCarrierEnabled;
    }

    public int getNetworkTypeBitmask() {
        return this.mNetworkTypeBitmask;
    }

    public int getMvnoType() {
        return this.mMvnoType;
    }

    private ApnSetting(Builder builder) {
        this.mPermanentFailed = false;
        this.mEntryName = builder.mEntryName;
        this.mApnName = builder.mApnName;
        this.mProxyAddress = builder.mProxyAddress;
        this.mProxyPort = builder.mProxyPort;
        this.mMmsc = builder.mMmsc;
        this.mMmsProxyAddress = builder.mMmsProxyAddress;
        this.mMmsProxyPort = builder.mMmsProxyPort;
        this.mUser = builder.mUser;
        this.mPassword = builder.mPassword;
        this.mAuthType = builder.mAuthType;
        this.mApnTypeBitmask = builder.mApnTypeBitmask;
        this.mId = builder.mId;
        this.mOperatorNumeric = builder.mOperatorNumeric;
        this.mProtocol = builder.mProtocol;
        this.mRoamingProtocol = builder.mRoamingProtocol;
        this.mMtu = builder.mMtu;
        this.mCarrierEnabled = builder.mCarrierEnabled;
        this.mNetworkTypeBitmask = builder.mNetworkTypeBitmask;
        this.mProfileId = builder.mProfileId;
        this.mModemCognitive = builder.mModemCognitive;
        this.mMaxConns = builder.mMaxConns;
        this.mWaitTime = builder.mWaitTime;
        this.mMaxConnsTime = builder.mMaxConnsTime;
        this.mMvnoType = builder.mMvnoType;
        this.mMvnoMatchData = builder.mMvnoMatchData;
    }

    public static ApnSetting makeApnSetting(int i, String str, String str2, String str3, InetAddress inetAddress, int i2, Uri uri, InetAddress inetAddress2, int i3, String str4, String str5, int i4, int i5, int i6, int i7, boolean z, int i8, int i9, boolean z2, int i10, int i11, int i12, int i13, int i14, String str6) {
        return new Builder().setId(i).setOperatorNumeric(str).setEntryName(str2).setApnName(str3).setProxyAddress(inetAddress).setProxyPort(i2).setMmsc(uri).setMmsProxyAddress(inetAddress2).setMmsProxyPort(i3).setUser(str4).setPassword(str5).setAuthType(i4).setApnTypeBitmask(i5).setProtocol(i6).setRoamingProtocol(i7).setCarrierEnabled(z).setNetworkTypeBitmask(i8).setProfileId(i9).setModemCognitive(z2).setMaxConns(i10).setWaitTime(i11).setMaxConnsTime(i12).setMtu(i13).setMvnoType(i14).setMvnoMatchData(str6).build();
    }

    public static ApnSetting makeApnSetting(Cursor cursor) {
        int types = parseTypes(cursor.getString(cursor.getColumnIndexOrThrow("type")));
        int iConvertBearerBitmaskToNetworkTypeBitmask = cursor.getInt(cursor.getColumnIndexOrThrow(Telephony.Carriers.NETWORK_TYPE_BITMASK));
        if (iConvertBearerBitmaskToNetworkTypeBitmask == 0) {
            iConvertBearerBitmaskToNetworkTypeBitmask = ServiceState.convertBearerBitmaskToNetworkTypeBitmask(cursor.getInt(cursor.getColumnIndexOrThrow(Telephony.Carriers.BEARER_BITMASK)));
        }
        return makeApnSetting(cursor.getInt(cursor.getColumnIndexOrThrow("_id")), cursor.getString(cursor.getColumnIndexOrThrow(Telephony.Carriers.NUMERIC)), cursor.getString(cursor.getColumnIndexOrThrow("name")), cursor.getString(cursor.getColumnIndexOrThrow("apn")), inetAddressFromString(cursor.getString(cursor.getColumnIndexOrThrow("proxy"))), portFromString(cursor.getString(cursor.getColumnIndexOrThrow("port"))), UriFromString(cursor.getString(cursor.getColumnIndexOrThrow(Telephony.Carriers.MMSC))), inetAddressFromString(cursor.getString(cursor.getColumnIndexOrThrow(Telephony.Carriers.MMSPROXY))), portFromString(cursor.getString(cursor.getColumnIndexOrThrow(Telephony.Carriers.MMSPORT))), cursor.getString(cursor.getColumnIndexOrThrow("user")), cursor.getString(cursor.getColumnIndexOrThrow("password")), cursor.getInt(cursor.getColumnIndexOrThrow(Telephony.Carriers.AUTH_TYPE)), types, nullToNotInMapInt(PROTOCOL_STRING_MAP.get(cursor.getString(cursor.getColumnIndexOrThrow("protocol")))), nullToNotInMapInt(PROTOCOL_STRING_MAP.get(cursor.getString(cursor.getColumnIndexOrThrow(Telephony.Carriers.ROAMING_PROTOCOL)))), cursor.getInt(cursor.getColumnIndexOrThrow(Telephony.Carriers.CARRIER_ENABLED)) == 1, iConvertBearerBitmaskToNetworkTypeBitmask, cursor.getInt(cursor.getColumnIndexOrThrow(Telephony.Carriers.PROFILE_ID)), cursor.getInt(cursor.getColumnIndexOrThrow(Telephony.Carriers.MODEM_COGNITIVE)) == 1, cursor.getInt(cursor.getColumnIndexOrThrow(Telephony.Carriers.MAX_CONNS)), cursor.getInt(cursor.getColumnIndexOrThrow(Telephony.Carriers.WAIT_TIME)), cursor.getInt(cursor.getColumnIndexOrThrow(Telephony.Carriers.MAX_CONNS_TIME)), cursor.getInt(cursor.getColumnIndexOrThrow("mtu")), nullToNotInMapInt(MVNO_TYPE_STRING_MAP.get(cursor.getString(cursor.getColumnIndexOrThrow("mvno_type")))), cursor.getString(cursor.getColumnIndexOrThrow("mvno_match_data")));
    }

    public static ApnSetting makeApnSetting(ApnSetting apnSetting) {
        return makeApnSetting(apnSetting.mId, apnSetting.mOperatorNumeric, apnSetting.mEntryName, apnSetting.mApnName, apnSetting.mProxyAddress, apnSetting.mProxyPort, apnSetting.mMmsc, apnSetting.mMmsProxyAddress, apnSetting.mMmsProxyPort, apnSetting.mUser, apnSetting.mPassword, apnSetting.mAuthType, apnSetting.mApnTypeBitmask, apnSetting.mProtocol, apnSetting.mRoamingProtocol, apnSetting.mCarrierEnabled, apnSetting.mNetworkTypeBitmask, apnSetting.mProfileId, apnSetting.mModemCognitive, apnSetting.mMaxConns, apnSetting.mWaitTime, apnSetting.mMaxConnsTime, apnSetting.mMtu, apnSetting.mMvnoType, apnSetting.mMvnoMatchData);
    }

    public String toString() {
        return "[ApnSettingV4] " + this.mEntryName + ", " + this.mId + ", " + this.mOperatorNumeric + ", " + this.mApnName + ", " + inetAddressToString(this.mProxyAddress) + ", " + UriToString(this.mMmsc) + ", " + inetAddressToString(this.mMmsProxyAddress) + ", " + portToString(this.mMmsProxyPort) + ", " + portToString(this.mProxyPort) + ", " + this.mAuthType + ", " + TextUtils.join(" | ", deParseTypes(this.mApnTypeBitmask).split(",")) + ", , " + this.mProtocol + ", " + this.mRoamingProtocol + ", " + this.mCarrierEnabled + ", " + this.mProfileId + ", " + this.mModemCognitive + ", " + this.mMaxConns + ", " + this.mWaitTime + ", " + this.mMaxConnsTime + ", " + this.mMtu + ", " + this.mMvnoType + ", " + this.mMvnoMatchData + ", " + this.mPermanentFailed + ", " + this.mNetworkTypeBitmask;
    }

    public boolean hasMvnoParams() {
        return (this.mMvnoType == -1 || TextUtils.isEmpty(this.mMvnoMatchData)) ? false : true;
    }

    public boolean canHandleType(int i) {
        return this.mCarrierEnabled && (this.mApnTypeBitmask & i) == i;
    }

    private boolean typeSameAny(ApnSetting apnSetting, ApnSetting apnSetting2) {
        if ((apnSetting.mApnTypeBitmask & apnSetting2.mApnTypeBitmask) != 0) {
            return true;
        }
        return false;
    }

    public boolean equals(Object obj) {
        if (!(obj instanceof ApnSetting)) {
            return false;
        }
        ApnSetting apnSetting = (ApnSetting) obj;
        return this.mEntryName.equals(apnSetting.mEntryName) && Objects.equals(Integer.valueOf(this.mId), Integer.valueOf(apnSetting.mId)) && Objects.equals(this.mOperatorNumeric, apnSetting.mOperatorNumeric) && Objects.equals(this.mApnName, apnSetting.mApnName) && Objects.equals(this.mProxyAddress, apnSetting.mProxyAddress) && Objects.equals(this.mMmsc, apnSetting.mMmsc) && Objects.equals(this.mMmsProxyAddress, apnSetting.mMmsProxyAddress) && Objects.equals(Integer.valueOf(this.mMmsProxyPort), Integer.valueOf(apnSetting.mMmsProxyPort)) && Objects.equals(Integer.valueOf(this.mProxyPort), Integer.valueOf(apnSetting.mProxyPort)) && Objects.equals(this.mUser, apnSetting.mUser) && Objects.equals(this.mPassword, apnSetting.mPassword) && Objects.equals(Integer.valueOf(this.mAuthType), Integer.valueOf(apnSetting.mAuthType)) && Objects.equals(Integer.valueOf(this.mApnTypeBitmask), Integer.valueOf(apnSetting.mApnTypeBitmask)) && Objects.equals(Integer.valueOf(this.mProtocol), Integer.valueOf(apnSetting.mProtocol)) && Objects.equals(Integer.valueOf(this.mRoamingProtocol), Integer.valueOf(apnSetting.mRoamingProtocol)) && Objects.equals(Boolean.valueOf(this.mCarrierEnabled), Boolean.valueOf(apnSetting.mCarrierEnabled)) && Objects.equals(Integer.valueOf(this.mProfileId), Integer.valueOf(apnSetting.mProfileId)) && Objects.equals(Boolean.valueOf(this.mModemCognitive), Boolean.valueOf(apnSetting.mModemCognitive)) && Objects.equals(Integer.valueOf(this.mMaxConns), Integer.valueOf(apnSetting.mMaxConns)) && Objects.equals(Integer.valueOf(this.mWaitTime), Integer.valueOf(apnSetting.mWaitTime)) && Objects.equals(Integer.valueOf(this.mMaxConnsTime), Integer.valueOf(apnSetting.mMaxConnsTime)) && Objects.equals(Integer.valueOf(this.mMtu), Integer.valueOf(apnSetting.mMtu)) && Objects.equals(Integer.valueOf(this.mMvnoType), Integer.valueOf(apnSetting.mMvnoType)) && Objects.equals(this.mMvnoMatchData, apnSetting.mMvnoMatchData) && Objects.equals(Integer.valueOf(this.mNetworkTypeBitmask), Integer.valueOf(apnSetting.mNetworkTypeBitmask));
    }

    public boolean equals(Object obj, boolean z) {
        if (!(obj instanceof ApnSetting)) {
            return false;
        }
        ApnSetting apnSetting = (ApnSetting) obj;
        if (!this.mEntryName.equals(apnSetting.mEntryName) || !Objects.equals(this.mOperatorNumeric, apnSetting.mOperatorNumeric) || !Objects.equals(this.mApnName, apnSetting.mApnName) || !Objects.equals(this.mProxyAddress, apnSetting.mProxyAddress) || !Objects.equals(this.mMmsc, apnSetting.mMmsc) || !Objects.equals(this.mMmsProxyAddress, apnSetting.mMmsProxyAddress) || !Objects.equals(Integer.valueOf(this.mMmsProxyPort), Integer.valueOf(apnSetting.mMmsProxyPort)) || !Objects.equals(Integer.valueOf(this.mProxyPort), Integer.valueOf(apnSetting.mProxyPort)) || !Objects.equals(this.mUser, apnSetting.mUser) || !Objects.equals(this.mPassword, apnSetting.mPassword) || !Objects.equals(Integer.valueOf(this.mAuthType), Integer.valueOf(apnSetting.mAuthType)) || !Objects.equals(Integer.valueOf(this.mApnTypeBitmask), Integer.valueOf(apnSetting.mApnTypeBitmask))) {
            return false;
        }
        if (z || Objects.equals(Integer.valueOf(this.mProtocol), Integer.valueOf(apnSetting.mProtocol))) {
            return (!z || Objects.equals(Integer.valueOf(this.mRoamingProtocol), Integer.valueOf(apnSetting.mRoamingProtocol))) && Objects.equals(Boolean.valueOf(this.mCarrierEnabled), Boolean.valueOf(apnSetting.mCarrierEnabled)) && Objects.equals(Integer.valueOf(this.mProfileId), Integer.valueOf(apnSetting.mProfileId)) && Objects.equals(Boolean.valueOf(this.mModemCognitive), Boolean.valueOf(apnSetting.mModemCognitive)) && Objects.equals(Integer.valueOf(this.mMaxConns), Integer.valueOf(apnSetting.mMaxConns)) && Objects.equals(Integer.valueOf(this.mWaitTime), Integer.valueOf(apnSetting.mWaitTime)) && Objects.equals(Integer.valueOf(this.mMaxConnsTime), Integer.valueOf(apnSetting.mMaxConnsTime)) && Objects.equals(Integer.valueOf(this.mMtu), Integer.valueOf(apnSetting.mMtu)) && Objects.equals(Integer.valueOf(this.mMvnoType), Integer.valueOf(apnSetting.mMvnoType)) && Objects.equals(this.mMvnoMatchData, apnSetting.mMvnoMatchData);
        }
        return false;
    }

    public boolean similar(ApnSetting apnSetting) {
        return !canHandleType(8) && !apnSetting.canHandleType(8) && Objects.equals(this.mApnName, apnSetting.mApnName) && !typeSameAny(this, apnSetting) && xorEquals(this.mProxyAddress, apnSetting.mProxyAddress) && xorEqualsPort(this.mProxyPort, apnSetting.mProxyPort) && xorEquals(Integer.valueOf(this.mProtocol), Integer.valueOf(apnSetting.mProtocol)) && xorEquals(Integer.valueOf(this.mRoamingProtocol), Integer.valueOf(apnSetting.mRoamingProtocol)) && Objects.equals(Boolean.valueOf(this.mCarrierEnabled), Boolean.valueOf(apnSetting.mCarrierEnabled)) && Objects.equals(Integer.valueOf(this.mProfileId), Integer.valueOf(apnSetting.mProfileId)) && Objects.equals(Integer.valueOf(this.mMvnoType), Integer.valueOf(apnSetting.mMvnoType)) && Objects.equals(this.mMvnoMatchData, apnSetting.mMvnoMatchData) && xorEquals(this.mMmsc, apnSetting.mMmsc) && xorEquals(this.mMmsProxyAddress, apnSetting.mMmsProxyAddress) && xorEqualsPort(this.mMmsProxyPort, apnSetting.mMmsProxyPort) && Objects.equals(Integer.valueOf(this.mNetworkTypeBitmask), Integer.valueOf(apnSetting.mNetworkTypeBitmask));
    }

    private boolean xorEquals(String str, String str2) {
        return Objects.equals(str, str2) || TextUtils.isEmpty(str) || TextUtils.isEmpty(str2);
    }

    private boolean xorEquals(Object obj, Object obj2) {
        return obj == null || obj2 == null || obj.equals(obj2);
    }

    private boolean xorEqualsPort(int i, int i2) {
        return i == -1 || i2 == -1 || Objects.equals(Integer.valueOf(i), Integer.valueOf(i2));
    }

    private String deParseTypes(int i) {
        ArrayList arrayList = new ArrayList();
        for (Integer num : APN_TYPE_INT_MAP.keySet()) {
            if ((num.intValue() & i) == num.intValue()) {
                arrayList.add(APN_TYPE_INT_MAP.get(num));
            }
        }
        return TextUtils.join(",", arrayList);
    }

    private String nullToEmpty(String str) {
        return str == null ? "" : str;
    }

    public ContentValues toContentValues() {
        ContentValues contentValues = new ContentValues();
        contentValues.put(Telephony.Carriers.NUMERIC, nullToEmpty(this.mOperatorNumeric));
        contentValues.put("name", nullToEmpty(this.mEntryName));
        contentValues.put("apn", nullToEmpty(this.mApnName));
        contentValues.put("proxy", this.mProxyAddress == null ? "" : inetAddressToString(this.mProxyAddress));
        contentValues.put("port", portToString(this.mProxyPort));
        contentValues.put(Telephony.Carriers.MMSC, this.mMmsc == null ? "" : UriToString(this.mMmsc));
        contentValues.put(Telephony.Carriers.MMSPORT, portToString(this.mMmsProxyPort));
        contentValues.put(Telephony.Carriers.MMSPROXY, this.mMmsProxyAddress == null ? "" : inetAddressToString(this.mMmsProxyAddress));
        contentValues.put("user", nullToEmpty(this.mUser));
        contentValues.put("password", nullToEmpty(this.mPassword));
        contentValues.put(Telephony.Carriers.AUTH_TYPE, Integer.valueOf(this.mAuthType));
        contentValues.put("type", nullToEmpty(deParseTypes(this.mApnTypeBitmask)));
        contentValues.put("protocol", nullToEmpty(PROTOCOL_INT_MAP.get(Integer.valueOf(this.mProtocol))));
        contentValues.put(Telephony.Carriers.ROAMING_PROTOCOL, nullToEmpty(PROTOCOL_INT_MAP.get(Integer.valueOf(this.mRoamingProtocol))));
        contentValues.put(Telephony.Carriers.CARRIER_ENABLED, Boolean.valueOf(this.mCarrierEnabled));
        contentValues.put("mvno_type", nullToEmpty(MVNO_TYPE_INT_MAP.get(Integer.valueOf(this.mMvnoType))));
        contentValues.put(Telephony.Carriers.NETWORK_TYPE_BITMASK, Integer.valueOf(this.mNetworkTypeBitmask));
        return contentValues;
    }

    public static int parseTypes(String str) {
        if (TextUtils.isEmpty(str)) {
            return 767;
        }
        int iIntValue = 0;
        for (String str2 : str.split(",")) {
            Integer num = APN_TYPE_STRING_MAP.get(str2);
            if (num != null) {
                iIntValue |= num.intValue();
            }
        }
        return iIntValue;
    }

    private static Uri UriFromString(String str) {
        if (TextUtils.isEmpty(str)) {
            return null;
        }
        return Uri.parse(str);
    }

    private static String UriToString(Uri uri) {
        return uri == null ? "" : uri.toString();
    }

    private static InetAddress inetAddressFromString(String str) {
        if (TextUtils.isEmpty(str)) {
            return null;
        }
        try {
            return InetAddress.getByName(str);
        } catch (UnknownHostException e) {
            Log.e(LOG_TAG, "Can't parse InetAddress from string: unknown host.");
            return null;
        }
    }

    private static String inetAddressToString(InetAddress inetAddress) {
        if (inetAddress == null) {
            return null;
        }
        String string = inetAddress.toString();
        if (TextUtils.isEmpty(string)) {
            return null;
        }
        String strSubstring = string.substring(0, string.indexOf("/"));
        String strSubstring2 = string.substring(string.indexOf("/") + 1);
        if (TextUtils.isEmpty(strSubstring) && TextUtils.isEmpty(strSubstring2)) {
            return null;
        }
        return TextUtils.isEmpty(strSubstring) ? strSubstring2 : strSubstring;
    }

    private static int portFromString(String str) {
        if (!TextUtils.isEmpty(str)) {
            try {
                return Integer.parseInt(str);
            } catch (NumberFormatException e) {
                Log.e(LOG_TAG, "Can't parse port from String");
            }
        }
        return -1;
    }

    private static String portToString(int i) {
        return i == -1 ? "" : Integer.toString(i);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeInt(this.mId);
        parcel.writeString(this.mOperatorNumeric);
        parcel.writeString(this.mEntryName);
        parcel.writeString(this.mApnName);
        parcel.writeValue(this.mProxyAddress);
        parcel.writeInt(this.mProxyPort);
        parcel.writeValue(this.mMmsc);
        parcel.writeValue(this.mMmsProxyAddress);
        parcel.writeInt(this.mMmsProxyPort);
        parcel.writeString(this.mUser);
        parcel.writeString(this.mPassword);
        parcel.writeInt(this.mAuthType);
        parcel.writeInt(this.mApnTypeBitmask);
        parcel.writeInt(this.mProtocol);
        parcel.writeInt(this.mRoamingProtocol);
        parcel.writeInt(this.mCarrierEnabled ? 1 : 0);
        parcel.writeInt(this.mMvnoType);
        parcel.writeInt(this.mNetworkTypeBitmask);
    }

    private static ApnSetting readFromParcel(Parcel parcel) {
        return makeApnSetting(parcel.readInt(), parcel.readString(), parcel.readString(), parcel.readString(), (InetAddress) parcel.readValue(InetAddress.class.getClassLoader()), parcel.readInt(), (Uri) parcel.readValue(Uri.class.getClassLoader()), (InetAddress) parcel.readValue(InetAddress.class.getClassLoader()), parcel.readInt(), parcel.readString(), parcel.readString(), parcel.readInt(), parcel.readInt(), parcel.readInt(), parcel.readInt(), parcel.readInt() > 0, parcel.readInt(), 0, false, 0, 0, 0, 0, parcel.readInt(), null);
    }

    private static int nullToNotInMapInt(Integer num) {
        if (num == null) {
            return -1;
        }
        return num.intValue();
    }

    public static class Builder {
        private String mApnName;
        private int mApnTypeBitmask;
        private int mAuthType;
        private boolean mCarrierEnabled;
        private String mEntryName;
        private int mId;
        private int mMaxConns;
        private int mMaxConnsTime;
        private InetAddress mMmsProxyAddress;
        private Uri mMmsc;
        private boolean mModemCognitive;
        private int mMtu;
        private String mMvnoMatchData;
        private int mNetworkTypeBitmask;
        private String mOperatorNumeric;
        private String mPassword;
        private int mProfileId;
        private InetAddress mProxyAddress;
        private String mUser;
        private int mWaitTime;
        private int mProxyPort = -1;
        private int mMmsProxyPort = -1;
        private int mProtocol = -1;
        private int mRoamingProtocol = -1;
        private int mMvnoType = -1;

        private Builder setId(int i) {
            this.mId = i;
            return this;
        }

        public Builder setMtu(int i) {
            this.mMtu = i;
            return this;
        }

        public Builder setProfileId(int i) {
            this.mProfileId = i;
            return this;
        }

        public Builder setModemCognitive(boolean z) {
            this.mModemCognitive = z;
            return this;
        }

        public Builder setMaxConns(int i) {
            this.mMaxConns = i;
            return this;
        }

        public Builder setWaitTime(int i) {
            this.mWaitTime = i;
            return this;
        }

        public Builder setMaxConnsTime(int i) {
            this.mMaxConnsTime = i;
            return this;
        }

        public Builder setMvnoMatchData(String str) {
            this.mMvnoMatchData = str;
            return this;
        }

        public Builder setEntryName(String str) {
            this.mEntryName = str;
            return this;
        }

        public Builder setApnName(String str) {
            this.mApnName = str;
            return this;
        }

        public Builder setProxyAddress(InetAddress inetAddress) {
            this.mProxyAddress = inetAddress;
            return this;
        }

        public Builder setProxyPort(int i) {
            this.mProxyPort = i;
            return this;
        }

        public Builder setMmsc(Uri uri) {
            this.mMmsc = uri;
            return this;
        }

        public Builder setMmsProxyAddress(InetAddress inetAddress) {
            this.mMmsProxyAddress = inetAddress;
            return this;
        }

        public Builder setMmsProxyPort(int i) {
            this.mMmsProxyPort = i;
            return this;
        }

        public Builder setUser(String str) {
            this.mUser = str;
            return this;
        }

        public Builder setPassword(String str) {
            this.mPassword = str;
            return this;
        }

        public Builder setAuthType(int i) {
            this.mAuthType = i;
            return this;
        }

        public Builder setApnTypeBitmask(int i) {
            this.mApnTypeBitmask = i;
            return this;
        }

        public Builder setOperatorNumeric(String str) {
            this.mOperatorNumeric = str;
            return this;
        }

        public Builder setProtocol(int i) {
            this.mProtocol = i;
            return this;
        }

        public Builder setRoamingProtocol(int i) {
            this.mRoamingProtocol = i;
            return this;
        }

        public Builder setCarrierEnabled(boolean z) {
            this.mCarrierEnabled = z;
            return this;
        }

        public Builder setNetworkTypeBitmask(int i) {
            this.mNetworkTypeBitmask = i;
            return this;
        }

        public Builder setMvnoType(int i) {
            this.mMvnoType = i;
            return this;
        }

        public ApnSetting build() {
            if ((this.mApnTypeBitmask & 1023) == 0 || TextUtils.isEmpty(this.mApnName) || TextUtils.isEmpty(this.mEntryName)) {
                return null;
            }
            return new ApnSetting(this);
        }
    }
}
