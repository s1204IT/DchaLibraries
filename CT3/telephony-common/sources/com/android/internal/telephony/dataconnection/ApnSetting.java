package com.android.internal.telephony.dataconnection;

import android.content.Context;
import android.os.PersistableBundle;
import android.os.SystemProperties;
import android.provider.Telephony;
import android.telephony.CarrierConfigManager;
import android.telephony.Rlog;
import android.telephony.ServiceState;
import android.text.TextUtils;
import android.util.Log;
import com.android.internal.telephony.uicc.IccRecords;
import com.google.android.mms.pdu.CharacterSets;
import com.mediatek.internal.telephony.ImsSwitchController;
import com.mediatek.internal.telephony.uicc.UsimPBMemInfo;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;

public class ApnSetting {
    private static final boolean DBG = true;
    static final String LOG_TAG = "ApnSetting";
    static final String V2_FORMAT_REGEX = "^\\[ApnSettingV2\\]\\s*";
    static final String V3_FORMAT_REGEX = "^\\[ApnSettingV3\\]\\s*";
    private static final boolean VDBG;
    private static HashMap<Integer, HashSet<String>> sMeteredApnTypes;
    private static HashMap<Integer, HashSet<String>> sMeteredRoamingApnTypes;
    public final String apn;
    public final int authType;
    private final int bearer;
    public final int bearerBitmask;
    public final String carrier;
    public final boolean carrierEnabled;
    public final int id;
    public final int inactiveTimer;
    public final int maxConns;
    public final int maxConnsTime;
    public final String mmsPort;
    public final String mmsProxy;
    public final String mmsc;
    public final boolean modemCognitive;
    public final int mtu;
    public final String mvnoMatchData;
    public final String mvnoType;
    public final String numeric;
    public final String password;
    public boolean permanentFailed;
    public final String port;
    public final int profileId;
    public final String protocol;
    public final String proxy;
    public final String roamingProtocol;
    public final String[] types;
    public final String user;
    public final int waitTime;

    static {
        VDBG = SystemProperties.get("ro.build.type").equals("eng");
        sMeteredApnTypes = new HashMap<>();
        sMeteredRoamingApnTypes = new HashMap<>();
    }

    public ApnSetting(int id, String numeric, String carrier, String apn, String proxy, String port, String mmsc, String mmsProxy, String mmsPort, String user, String password, int authType, String[] types, String protocol, String roamingProtocol, boolean carrierEnabled, int bearer, int bearerBitmask, int profileId, boolean modemCognitive, int maxConns, int waitTime, int maxConnsTime, int mtu, String mvnoType, String mvnoMatchData, int inactiveTimer) {
        this.permanentFailed = false;
        this.id = id;
        this.numeric = numeric;
        this.carrier = carrier;
        this.apn = apn;
        this.proxy = proxy;
        this.port = port;
        this.mmsc = mmsc;
        this.mmsProxy = mmsProxy;
        this.mmsPort = mmsPort;
        this.user = user;
        this.password = password;
        this.authType = authType;
        this.types = new String[types.length];
        for (int i = 0; i < types.length; i++) {
            this.types[i] = types[i].toLowerCase(Locale.ROOT);
        }
        this.protocol = protocol;
        this.roamingProtocol = roamingProtocol;
        this.carrierEnabled = carrierEnabled;
        this.bearer = bearer;
        this.bearerBitmask = ServiceState.getBitmaskForTech(bearer) | bearerBitmask;
        this.profileId = profileId;
        this.modemCognitive = modemCognitive;
        this.maxConns = maxConns;
        this.waitTime = waitTime;
        this.maxConnsTime = maxConnsTime;
        this.mtu = mtu;
        this.mvnoType = mvnoType;
        this.mvnoMatchData = mvnoMatchData;
        this.inactiveTimer = inactiveTimer;
    }

    public ApnSetting(ApnSetting apn) {
        this(apn.id, apn.numeric, apn.carrier, apn.apn, apn.proxy, apn.port, apn.mmsc, apn.mmsProxy, apn.mmsPort, apn.user, apn.password, apn.authType, apn.types, apn.protocol, apn.roamingProtocol, apn.carrierEnabled, apn.bearer, apn.bearerBitmask, apn.profileId, apn.modemCognitive, apn.maxConns, apn.waitTime, apn.maxConnsTime, apn.mtu, apn.mvnoType, apn.mvnoMatchData, apn.inactiveTimer);
    }

    public static ApnSetting fromString(String data) {
        int version;
        int authType;
        String[] typeArray;
        String protocol;
        String roamingProtocol;
        boolean z;
        if (data == null) {
            return null;
        }
        if (data.matches("^\\[ApnSettingV3\\]\\s*.*")) {
            version = 3;
            data = data.replaceFirst(V3_FORMAT_REGEX, UsimPBMemInfo.STRING_NOT_SET);
        } else if (data.matches("^\\[ApnSettingV2\\]\\s*.*")) {
            version = 2;
            data = data.replaceFirst(V2_FORMAT_REGEX, UsimPBMemInfo.STRING_NOT_SET);
        } else {
            version = 1;
        }
        String[] a = data.split("\\s*,\\s*");
        if (a.length < 14) {
            return null;
        }
        try {
            authType = Integer.parseInt(a[12]);
        } catch (NumberFormatException e) {
            authType = 0;
        }
        int bearerBitmask = 0;
        int profileId = 0;
        boolean modemCognitive = false;
        int maxConns = 0;
        int waitTime = 0;
        int maxConnsTime = 0;
        int mtu = 0;
        String mvnoType = UsimPBMemInfo.STRING_NOT_SET;
        String mvnoMatchData = UsimPBMemInfo.STRING_NOT_SET;
        int inactiveTimer = 0;
        if (version == 1) {
            typeArray = new String[a.length - 13];
            System.arraycopy(a, 13, typeArray, 0, a.length - 13);
            protocol = "IP";
            roamingProtocol = "IP";
            z = true;
        } else {
            if (a.length < 18) {
                return null;
            }
            typeArray = a[13].split("\\s*\\|\\s*");
            protocol = a[14];
            roamingProtocol = a[15];
            z = Boolean.parseBoolean(a[16]);
            bearerBitmask = ServiceState.getBitmaskFromString(a[17]);
            if (a.length > 22) {
                modemCognitive = Boolean.parseBoolean(a[19]);
                try {
                    profileId = Integer.parseInt(a[18]);
                    maxConns = Integer.parseInt(a[20]);
                    waitTime = Integer.parseInt(a[21]);
                    maxConnsTime = Integer.parseInt(a[22]);
                } catch (NumberFormatException e2) {
                }
            }
            if (a.length > 23) {
                try {
                    mtu = Integer.parseInt(a[23]);
                } catch (NumberFormatException e3) {
                }
            }
            if (a.length > 25) {
                mvnoType = a[24];
                mvnoMatchData = a[25];
            }
            if (a.length > 26) {
                try {
                    inactiveTimer = Integer.parseInt(a[26]);
                } catch (NumberFormatException e4) {
                    Rlog.e(LOG_TAG, "NumberFormatException, inactive timer = " + a[26]);
                }
            }
        }
        return new ApnSetting(-1, a[10] + a[11], a[0], a[1], a[2], a[3], a[7], a[8], a[9], a[4], a[5], authType, typeArray, protocol, roamingProtocol, z, 0, bearerBitmask, profileId, modemCognitive, maxConns, waitTime, maxConnsTime, mtu, mvnoType, mvnoMatchData, inactiveTimer);
    }

    public static List<ApnSetting> arrayFromString(String data) {
        List<ApnSetting> retVal = new ArrayList<>();
        if (TextUtils.isEmpty(data)) {
            return retVal;
        }
        String[] apnStrings = data.split("\\s*;\\s*");
        for (String apnString : apnStrings) {
            ApnSetting apn = fromString(apnString);
            if (apn != null) {
                retVal.add(apn);
            }
        }
        return retVal;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("[ApnSettingV3] ").append(this.carrier).append(", ").append(this.id).append(", ").append(this.numeric).append(", ").append(this.apn).append(", ").append(this.proxy).append(", ").append(this.mmsc).append(", ").append(this.mmsProxy).append(", ").append(this.mmsPort).append(", ").append(this.port).append(", ").append(this.authType).append(", ");
        for (int i = 0; i < this.types.length; i++) {
            sb.append(this.types[i]);
            if (i < this.types.length - 1) {
                sb.append(" | ");
            }
        }
        sb.append(", ").append(this.protocol);
        sb.append(", ").append(this.roamingProtocol);
        sb.append(", ").append(this.carrierEnabled);
        sb.append(", ").append(this.bearer);
        sb.append(", ").append(this.bearerBitmask);
        sb.append(", ").append(this.profileId);
        sb.append(", ").append(this.modemCognitive);
        sb.append(", ").append(this.maxConns);
        sb.append(", ").append(this.waitTime);
        sb.append(", ").append(this.maxConnsTime);
        sb.append(", ").append(this.mtu);
        sb.append(", ").append(this.mvnoType);
        sb.append(", ").append(this.mvnoMatchData);
        sb.append(", ").append(this.permanentFailed);
        sb.append(", ").append(this.inactiveTimer);
        return sb.toString();
    }

    public boolean hasMvnoParams() {
        return (TextUtils.isEmpty(this.mvnoType) || TextUtils.isEmpty(this.mvnoMatchData)) ? false : true;
    }

    public boolean canHandleType(String type) {
        if (!this.carrierEnabled) {
            return false;
        }
        for (String t : this.types) {
            if (VDBG) {
                Log.d(LOG_TAG, "canHandleType(): entry in types=" + t + ", reqType=" + type);
            }
            if (type.equalsIgnoreCase("dun")) {
                if (t.equalsIgnoreCase("tethering") || t.equalsIgnoreCase("dun")) {
                    if (this.types.length == 1) {
                        Log.d(LOG_TAG, "canHandleType(): use TETHERING for HIPRI type");
                        return true;
                    }
                    Log.d(LOG_TAG, "canHandleType(): not TETHERING only APN settings");
                    return false;
                }
            } else {
                if (t.equalsIgnoreCase(type) || !(!t.equalsIgnoreCase(CharacterSets.MIMENAME_ANY_CHARSET) || type.equalsIgnoreCase(ImsSwitchController.IMS_SERVICE) || type.equalsIgnoreCase("emergency"))) {
                    return true;
                }
                if (t.equalsIgnoreCase("default") && type.equalsIgnoreCase("hipri")) {
                    Log.d(LOG_TAG, "canHandleType(): use DEFAULT for HIPRI type");
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean imsiMatches(String imsiDB, String imsiSIM) {
        int len = imsiDB.length();
        if (len <= 0 || len > imsiSIM.length()) {
            return false;
        }
        for (int idx = 0; idx < len; idx++) {
            char c = imsiDB.charAt(idx);
            if (c != 'x' && c != 'X' && c != imsiSIM.charAt(idx)) {
                return false;
            }
        }
        return true;
    }

    public static boolean mvnoMatches(IccRecords r, String mvnoType, String mvnoMatchData) {
        if (mvnoType.equalsIgnoreCase(Telephony.Carriers.SPN)) {
            if (r.getServiceProviderName() != null && r.getServiceProviderName().equalsIgnoreCase(mvnoMatchData)) {
                return true;
            }
        } else if (mvnoType.equalsIgnoreCase(Telephony.Carriers.IMSI)) {
            String imsiSIM = r.getIMSI();
            if (imsiSIM != null && imsiMatches(mvnoMatchData, imsiSIM)) {
                return true;
            }
        } else if (mvnoType.equalsIgnoreCase("gid")) {
            String gid1 = r.getGid1();
            int mvno_match_data_length = mvnoMatchData.length();
            if (gid1 != null && gid1.length() >= mvno_match_data_length && gid1.substring(0, mvno_match_data_length).equalsIgnoreCase(mvnoMatchData)) {
                return true;
            }
        } else if (mvnoType.equalsIgnoreCase(Telephony.Carriers.PNN) && r.isOperatorMvnoForEfPnn() != null && r.isOperatorMvnoForEfPnn().equalsIgnoreCase(mvnoMatchData)) {
            return true;
        }
        return false;
    }

    public static boolean isMeteredApnType(String type, Context context, int subId, boolean isRoaming) {
        String carrierConfig;
        HashMap<Integer, HashSet<String>> meteredApnTypesCache = isRoaming ? sMeteredApnTypes : sMeteredRoamingApnTypes;
        if (isRoaming) {
            carrierConfig = "carrier_metered_roaming_apn_types_strings";
        } else {
            carrierConfig = "carrier_metered_apn_types_strings";
        }
        synchronized (meteredApnTypesCache) {
            HashSet<String> meteredApnSet = meteredApnTypesCache.get(Integer.valueOf(subId));
            if (meteredApnSet == null) {
                CarrierConfigManager configManager = (CarrierConfigManager) context.getSystemService("carrier_config");
                if (configManager == null) {
                    Rlog.e(LOG_TAG, "Carrier config service is not available");
                    return true;
                }
                PersistableBundle b = configManager.getConfigForSubId(subId);
                if (b == null) {
                    Rlog.e(LOG_TAG, "Can't get the config. subId = " + subId);
                    return true;
                }
                String[] meteredApnTypes = b.getStringArray(carrierConfig);
                if (meteredApnTypes == null) {
                    Rlog.e(LOG_TAG, carrierConfig + " is not available. subId = " + subId);
                    return true;
                }
                meteredApnSet = new HashSet<>(Arrays.asList(meteredApnTypes));
                meteredApnTypesCache.put(Integer.valueOf(subId), meteredApnSet);
                Rlog.d(LOG_TAG, "For subId = " + subId + ", metered APN types are " + Arrays.toString(meteredApnSet.toArray()) + " isRoaming: " + isRoaming);
            }
            if (meteredApnSet.contains(CharacterSets.MIMENAME_ANY_CHARSET)) {
                Rlog.d(LOG_TAG, "All APN types are metered. isRoaming: " + isRoaming);
                return true;
            }
            if (meteredApnSet.contains(type)) {
                Rlog.d(LOG_TAG, type + " is metered. isRoaming: " + isRoaming);
                return true;
            }
            if (type.equals(CharacterSets.MIMENAME_ANY_CHARSET) && meteredApnSet.size() > 0) {
                Rlog.d(LOG_TAG, "APN_TYPE_ALL APN is metered. isRoaming: " + isRoaming);
                return true;
            }
            Rlog.d(LOG_TAG, type + " is not metered. isRoaming: " + isRoaming);
            return false;
        }
    }

    public boolean isMetered(Context context, int subId, boolean isRoaming) {
        for (String type : this.types) {
            if (isMeteredApnType(type, context, subId, isRoaming)) {
                Rlog.d(LOG_TAG, "Metered. APN = " + toString() + "isRoaming: " + isRoaming);
                return true;
            }
        }
        Rlog.d(LOG_TAG, "Not metered. APN = " + toString() + "isRoaming: " + isRoaming);
        return false;
    }

    public boolean equals(Object o) {
        if (!(o instanceof ApnSetting)) {
            return false;
        }
        ApnSetting other = (ApnSetting) o;
        if (this.carrier.equals(other.carrier) && this.id == other.id && this.numeric.equals(other.numeric) && this.apn.equals(other.apn) && this.proxy.equals(other.proxy) && this.mmsc.equals(other.mmsc) && this.mmsProxy.equals(other.mmsProxy) && this.port.equals(other.port) && this.authType == other.authType && Arrays.deepEquals(this.types, other.types) && this.protocol.equals(other.protocol) && this.roamingProtocol.equals(other.roamingProtocol) && this.carrierEnabled == other.carrierEnabled && this.bearer == other.bearer && this.bearerBitmask == other.bearerBitmask && this.profileId == other.profileId && this.modemCognitive == other.modemCognitive && this.maxConns == other.maxConns && this.waitTime == other.waitTime && this.maxConnsTime == other.maxConnsTime && this.mtu == other.mtu && this.mvnoType.equals(other.mvnoType)) {
            return this.mvnoMatchData.equals(other.mvnoMatchData);
        }
        return false;
    }

    public String toStringIgnoreName(boolean ignoreName) {
        StringBuilder sb = new StringBuilder();
        sb.append(this.id);
        if (!ignoreName) {
            sb.append(", ").append(this.carrier);
        }
        sb.append(", ").append(this.numeric).append(", ").append(this.apn).append(", ").append(this.proxy).append(", ").append(this.mmsc).append(", ").append(this.mmsProxy).append(", ").append(this.mmsPort).append(", ").append(this.port).append(", ").append(this.authType).append(", ");
        for (int i = 0; i < this.types.length; i++) {
            sb.append(this.types[i]);
            if (i < this.types.length - 1) {
                sb.append(" | ");
            }
        }
        sb.append(", ").append(this.protocol);
        sb.append(", ").append(this.roamingProtocol);
        sb.append(", ").append(this.carrierEnabled);
        sb.append(", ").append(this.bearer);
        sb.append(", ").append(this.bearerBitmask);
        sb.append(", ").append(this.profileId);
        sb.append(", ").append(this.modemCognitive);
        sb.append(", ").append(this.maxConns);
        sb.append(", ").append(this.waitTime);
        sb.append(", ").append(this.maxConnsTime);
        sb.append(", ").append(this.mtu);
        sb.append(", ").append(this.mvnoType);
        sb.append(", ").append(this.mvnoMatchData);
        sb.append(", ").append(this.user);
        Rlog.d(LOG_TAG, "toStringIgnoreName: sb = " + sb.toString() + ", ignoreName: " + ignoreName);
        sb.append(", ").append(this.password);
        return sb.toString();
    }

    public static String toStringIgnoreNameForList(List<ApnSetting> apnSettings, boolean ignoreName) {
        if (apnSettings == null || apnSettings.size() == 0) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        for (ApnSetting t : apnSettings) {
            sb.append(t.toStringIgnoreName(ignoreName));
        }
        return sb.toString();
    }
}
