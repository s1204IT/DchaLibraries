package com.android.internal.telephony.dataconnection;

import android.hardware.radio.V1_0.ApnTypes;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.telephony.CarrierConfigManager;
import android.telephony.Rlog;
import android.telephony.ServiceState;
import android.text.TextUtils;
import android.util.Log;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.uicc.IccRecords;
import com.google.android.mms.pdu.CharacterSets;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;

public class ApnSetting {
    private static final boolean DBG = false;
    static final String LOG_TAG = "ApnSetting";
    static final String TAG = "ApnSetting";
    static final String V2_FORMAT_REGEX = "^\\[ApnSettingV2\\]\\s*";
    static final String V3_FORMAT_REGEX = "^\\[ApnSettingV3\\]\\s*";
    static final String V4_FORMAT_REGEX = "^\\[ApnSettingV4\\]\\s*";
    static final String V5_FORMAT_REGEX = "^\\[ApnSettingV5\\]\\s*";
    private static final boolean VDBG = false;
    private static Method sMethodFromStringEx;
    private static Method sMethodIsMeteredApnTypeEx;
    private static Method sMethodMvnoMatchesEx;
    private static Method sMethodgetApnBitmaskEx;
    public final String apn;
    public final int apnSetId;
    public final int authType;

    @Deprecated
    protected final int bearer;

    @Deprecated
    public final int bearerBitmask;
    public final String carrier;
    public final boolean carrierEnabled;
    public final int id;
    public final int maxConns;
    public final int maxConnsTime;
    public final String mmsPort;
    public final String mmsProxy;
    public final String mmsc;
    public final boolean modemCognitive;
    public final int mtu;
    public final String mvnoMatchData;
    public final String mvnoType;
    public final int networkTypeBitmask;
    public final String numeric;
    public final String password;
    public boolean permanentFailed;
    public final String port;
    public final int profileId;
    public final String protocol;
    public final String proxy;
    public final String roamingProtocol;
    public final String[] types;
    public final int typesBitmap;
    public final String user;
    public final int waitTime;

    static {
        Class<?> cls;
        try {
            cls = Class.forName("com.mediatek.internal.telephony.dataconnection.MtkApnSetting");
        } catch (Exception e) {
            Rlog.d("ApnSetting", e.toString());
            cls = null;
        }
        if (cls != null) {
            try {
                sMethodFromStringEx = cls.getDeclaredMethod("fromStringEx", String[].class, Integer.TYPE, String[].class, String.class, String.class, Boolean.TYPE, Integer.TYPE, Integer.TYPE, Boolean.TYPE, Integer.TYPE, Integer.TYPE, Integer.TYPE, Integer.TYPE, String.class, String.class, Integer.TYPE);
                sMethodFromStringEx.setAccessible(true);
            } catch (Exception e2) {
                Rlog.d("ApnSetting", e2.toString());
            }
            try {
                sMethodMvnoMatchesEx = cls.getDeclaredMethod("mvnoMatchesEx", IccRecords.class, String.class, String.class);
                sMethodMvnoMatchesEx.setAccessible(true);
            } catch (Exception e3) {
                Rlog.d("ApnSetting", e3.toString());
            }
            try {
                sMethodIsMeteredApnTypeEx = cls.getDeclaredMethod("isMeteredApnTypeEx", String.class, Phone.class);
                sMethodIsMeteredApnTypeEx.setAccessible(true);
            } catch (Exception e4) {
                Rlog.d("ApnSetting", e4.toString());
            }
            try {
                sMethodgetApnBitmaskEx = cls.getMethod("getApnBitmaskEx", String.class);
                sMethodgetApnBitmaskEx.setAccessible(true);
            } catch (Exception e5) {
                Rlog.d("ApnSetting", e5.toString());
            }
        }
    }

    @Deprecated
    public ApnSetting(int i, String str, String str2, String str3, String str4, String str5, String str6, String str7, String str8, String str9, String str10, int i2, String[] strArr, String str11, String str12, boolean z, int i3, int i4, int i5, boolean z2, int i6, int i7, int i8, int i9, String str13, String str14) {
        this.permanentFailed = false;
        this.id = i;
        this.numeric = str;
        this.carrier = str2;
        this.apn = str3;
        this.proxy = str4;
        this.port = str5;
        this.mmsc = str6;
        this.mmsProxy = str7;
        this.mmsPort = str8;
        this.user = str9;
        this.password = str10;
        this.authType = i2;
        this.types = new String[strArr.length];
        int apnBitmask = 0;
        for (int i10 = 0; i10 < strArr.length; i10++) {
            this.types[i10] = strArr[i10].toLowerCase();
            apnBitmask |= getApnBitmask(this.types[i10]);
        }
        this.typesBitmap = apnBitmask;
        this.protocol = str11;
        this.roamingProtocol = str12;
        this.carrierEnabled = z;
        this.bearer = i3;
        this.bearerBitmask = i4 | ServiceState.getBitmaskForTech(i3);
        this.profileId = i5;
        this.modemCognitive = z2;
        this.maxConns = i6;
        this.waitTime = i7;
        this.maxConnsTime = i8;
        this.mtu = i9;
        this.mvnoType = str13;
        this.mvnoMatchData = str14;
        this.apnSetId = 0;
        this.networkTypeBitmask = ServiceState.convertBearerBitmaskToNetworkTypeBitmask(this.bearerBitmask);
    }

    public ApnSetting(int i, String str, String str2, String str3, String str4, String str5, String str6, String str7, String str8, String str9, String str10, int i2, String[] strArr, String str11, String str12, boolean z, int i3, int i4, boolean z2, int i5, int i6, int i7, int i8, String str13, String str14) {
        this(i, str, str2, str3, str4, str5, str6, str7, str8, str9, str10, i2, strArr, str11, str12, z, i3, i4, z2, i5, i6, i7, i8, str13, str14, 0);
    }

    public ApnSetting(int i, String str, String str2, String str3, String str4, String str5, String str6, String str7, String str8, String str9, String str10, int i2, String[] strArr, String str11, String str12, boolean z, int i3, int i4, boolean z2, int i5, int i6, int i7, int i8, String str13, String str14, int i9) {
        this.permanentFailed = false;
        this.id = i;
        this.numeric = str;
        this.carrier = str2;
        this.apn = str3;
        this.proxy = str4;
        this.port = str5;
        this.mmsc = str6;
        this.mmsProxy = str7;
        this.mmsPort = str8;
        this.user = str9;
        this.password = str10;
        this.authType = i2;
        this.types = new String[strArr.length];
        int apnBitmask = 0;
        for (int i10 = 0; i10 < strArr.length; i10++) {
            this.types[i10] = strArr[i10].toLowerCase();
            apnBitmask |= getApnBitmask(this.types[i10]);
        }
        this.typesBitmap = apnBitmask;
        this.protocol = str11;
        this.roamingProtocol = str12;
        this.carrierEnabled = z;
        this.bearer = 0;
        this.bearerBitmask = ServiceState.convertNetworkTypeBitmaskToBearerBitmask(i3);
        this.networkTypeBitmask = i3;
        this.profileId = i4;
        this.modemCognitive = z2;
        this.maxConns = i5;
        this.waitTime = i6;
        this.maxConnsTime = i7;
        this.mtu = i8;
        this.mvnoType = str13;
        this.mvnoMatchData = str14;
        this.apnSetId = i9;
    }

    public ApnSetting(ApnSetting apnSetting) {
        this(apnSetting.id, apnSetting.numeric, apnSetting.carrier, apnSetting.apn, apnSetting.proxy, apnSetting.port, apnSetting.mmsc, apnSetting.mmsProxy, apnSetting.mmsPort, apnSetting.user, apnSetting.password, apnSetting.authType, apnSetting.types, apnSetting.protocol, apnSetting.roamingProtocol, apnSetting.carrierEnabled, apnSetting.networkTypeBitmask, apnSetting.profileId, apnSetting.modemCognitive, apnSetting.maxConns, apnSetting.waitTime, apnSetting.maxConnsTime, apnSetting.mtu, apnSetting.mvnoType, apnSetting.mvnoMatchData, apnSetting.apnSetId);
    }

    public static ApnSetting fromString(String str) {
        char c;
        int i;
        int i2;
        int i3;
        boolean z;
        int i4;
        int i5;
        int i6;
        int bitmaskFromString;
        String[] strArr;
        int i7;
        String str2;
        String str3;
        String str4;
        String str5;
        boolean z2;
        int i8;
        int i9;
        int i10;
        int i11;
        int i12;
        boolean z3;
        int i13;
        int i14;
        int i15;
        String strReplaceFirst = str;
        if (strReplaceFirst == null) {
            return null;
        }
        if (strReplaceFirst.matches("^\\[ApnSettingV5\\]\\s*.*")) {
            strReplaceFirst = strReplaceFirst.replaceFirst(V5_FORMAT_REGEX, "");
            c = 5;
        } else if (strReplaceFirst.matches("^\\[ApnSettingV4\\]\\s*.*")) {
            strReplaceFirst = strReplaceFirst.replaceFirst(V4_FORMAT_REGEX, "");
            c = 4;
        } else if (strReplaceFirst.matches("^\\[ApnSettingV3\\]\\s*.*")) {
            strReplaceFirst = strReplaceFirst.replaceFirst(V3_FORMAT_REGEX, "");
            c = 3;
        } else if (strReplaceFirst.matches("^\\[ApnSettingV2\\]\\s*.*")) {
            strReplaceFirst = strReplaceFirst.replaceFirst(V2_FORMAT_REGEX, "");
            c = 2;
        } else {
            c = 1;
        }
        String[] strArrSplit = strReplaceFirst.split("\\s*,\\s*");
        if (strArrSplit.length < 14) {
            return null;
        }
        try {
            i = Integer.parseInt(strArrSplit[12]);
        } catch (NumberFormatException e) {
            i = 0;
        }
        String str6 = "";
        String str7 = "";
        if (c == 1) {
            String[] strArr2 = new String[strArrSplit.length - 13];
            System.arraycopy(strArrSplit, 13, strArr2, 0, strArrSplit.length - 13);
            strArr = strArr2;
            str4 = "IP";
            z2 = true;
            i12 = 0;
            z3 = false;
            i8 = 0;
            bitmaskFromString = 0;
            i3 = 0;
            i9 = 0;
            i7 = 0;
            str2 = "";
            str3 = "";
            str5 = "IP";
            i10 = 0;
            i11 = 0;
        } else {
            if (strArrSplit.length < 18) {
                return null;
            }
            String[] strArrSplit2 = strArrSplit[13].split("\\s*\\|\\s*");
            String str8 = strArrSplit[14];
            String str9 = strArrSplit[15];
            boolean z4 = Boolean.parseBoolean(strArrSplit[16]);
            int bitmaskFromString2 = ServiceState.getBitmaskFromString(strArrSplit[17]);
            if (strArrSplit.length > 22) {
                z = Boolean.parseBoolean(strArrSplit[19]);
                try {
                    i13 = Integer.parseInt(strArrSplit[18]);
                } catch (NumberFormatException e2) {
                    i13 = 0;
                    i4 = 0;
                }
                try {
                    i4 = Integer.parseInt(strArrSplit[20]);
                    try {
                        i5 = Integer.parseInt(strArrSplit[21]);
                        try {
                            i3 = i13;
                            i2 = Integer.parseInt(strArrSplit[22]);
                        } catch (NumberFormatException e3) {
                            i3 = i13;
                            i2 = 0;
                        }
                    } catch (NumberFormatException e4) {
                        i5 = 0;
                    }
                } catch (NumberFormatException e5) {
                    i4 = 0;
                    i5 = i4;
                    i3 = i13;
                    i2 = 0;
                    if (strArrSplit.length <= 23) {
                    }
                    if (bitmaskFromString == 0) {
                    }
                    if (sMethodFromStringEx != null) {
                    }
                    Rlog.d("ApnSetting", e.toString());
                    return new ApnSetting(-1, strArrSplit[10] + strArrSplit[11], strArrSplit[0], strArrSplit[1], strArrSplit[2], strArrSplit[3], strArrSplit[7], strArrSplit[8], strArrSplit[9], strArrSplit[4], strArrSplit[5], i, strArr, str4, str5, z2, bitmaskFromString, i3, z3, i8, i9, i14, i10, str2, str3, i15);
                }
            } else {
                i2 = 0;
                i3 = 0;
                z = false;
                i4 = 0;
                i5 = 0;
            }
            if (strArrSplit.length <= 23) {
                try {
                    i6 = Integer.parseInt(strArrSplit[23]);
                } catch (NumberFormatException e6) {
                    i6 = 0;
                }
                if (strArrSplit.length > 25) {
                    str6 = strArrSplit[24];
                    str7 = strArrSplit[25];
                }
                bitmaskFromString = strArrSplit.length <= 26 ? ServiceState.getBitmaskFromString(strArrSplit[26]) : 0;
                if (strArrSplit.length <= 27) {
                    strArr = strArrSplit2;
                    i7 = Integer.parseInt(strArrSplit[27]);
                } else {
                    strArr = strArrSplit2;
                    i7 = 0;
                }
                str2 = str6;
                str3 = str7;
                str4 = str8;
                str5 = str9;
                z2 = z4;
                i8 = i4;
                i9 = i5;
                i10 = i6;
                i11 = i2;
                i12 = bitmaskFromString2;
                z3 = z;
            } else {
                i6 = 0;
                if (strArrSplit.length > 25) {
                }
                if (strArrSplit.length <= 26) {
                }
                if (strArrSplit.length <= 27) {
                }
                str2 = str6;
                str3 = str7;
                str4 = str8;
                str5 = str9;
                z2 = z4;
                i8 = i4;
                i9 = i5;
                i10 = i6;
                i11 = i2;
                i12 = bitmaskFromString2;
                z3 = z;
            }
        }
        if (bitmaskFromString == 0) {
            bitmaskFromString = ServiceState.convertBearerBitmaskToNetworkTypeBitmask(i12);
        }
        try {
        } catch (Exception e7) {
            e = e7;
            i14 = i11;
            i15 = i7;
        }
        if (sMethodFromStringEx != null) {
            i14 = i11;
            i15 = i7;
            return new ApnSetting(-1, strArrSplit[10] + strArrSplit[11], strArrSplit[0], strArrSplit[1], strArrSplit[2], strArrSplit[3], strArrSplit[7], strArrSplit[8], strArrSplit[9], strArrSplit[4], strArrSplit[5], i, strArr, str4, str5, z2, bitmaskFromString, i3, z3, i8, i9, i14, i10, str2, str3, i15);
        }
        Method method = sMethodFromStringEx;
        Object[] objArr = new Object[16];
        objArr[0] = strArrSplit;
        objArr[1] = Integer.valueOf(i);
        objArr[2] = strArr;
        objArr[3] = str4;
        objArr[4] = str5;
        objArr[5] = Boolean.valueOf(z2);
        objArr[6] = Integer.valueOf(bitmaskFromString);
        objArr[7] = Integer.valueOf(i3);
        objArr[8] = Boolean.valueOf(z3);
        objArr[9] = Integer.valueOf(i8);
        objArr[10] = Integer.valueOf(i9);
        objArr[11] = Integer.valueOf(i11);
        objArr[12] = Integer.valueOf(i10);
        objArr[13] = str2;
        objArr[14] = str3;
        i14 = i11;
        i15 = i7;
        try {
            objArr[15] = Integer.valueOf(i15);
            return (ApnSetting) method.invoke(null, objArr);
        } catch (Exception e8) {
            e = e8;
        }
        Rlog.d("ApnSetting", e.toString());
        return new ApnSetting(-1, strArrSplit[10] + strArrSplit[11], strArrSplit[0], strArrSplit[1], strArrSplit[2], strArrSplit[3], strArrSplit[7], strArrSplit[8], strArrSplit[9], strArrSplit[4], strArrSplit[5], i, strArr, str4, str5, z2, bitmaskFromString, i3, z3, i8, i9, i14, i10, str2, str3, i15);
    }

    public static List<ApnSetting> arrayFromString(String str) {
        ArrayList arrayList = new ArrayList();
        if (TextUtils.isEmpty(str)) {
            return arrayList;
        }
        for (String str2 : str.split("\\s*;\\s*")) {
            ApnSetting apnSettingFromString = fromString(str2);
            if (apnSettingFromString != null) {
                arrayList.add(apnSettingFromString);
            }
        }
        return arrayList;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("[ApnSettingV5] ");
        sb.append(this.carrier);
        sb.append(", ");
        sb.append(this.id);
        sb.append(", ");
        sb.append(this.numeric);
        sb.append(", ");
        sb.append(this.apn);
        sb.append(", ");
        sb.append(this.proxy);
        sb.append(", ");
        sb.append(this.mmsc);
        sb.append(", ");
        sb.append(this.mmsProxy);
        sb.append(", ");
        sb.append(this.mmsPort);
        sb.append(", ");
        sb.append(this.port);
        sb.append(", ");
        sb.append(this.authType);
        sb.append(", ");
        for (int i = 0; i < this.types.length; i++) {
            sb.append(this.types[i]);
            if (i < this.types.length - 1) {
                sb.append(" | ");
            }
        }
        sb.append(", ");
        sb.append(this.protocol);
        sb.append(", ");
        sb.append(this.roamingProtocol);
        sb.append(", ");
        sb.append(this.carrierEnabled);
        sb.append(", ");
        sb.append(this.bearer);
        sb.append(", ");
        sb.append(this.bearerBitmask);
        sb.append(", ");
        sb.append(this.profileId);
        sb.append(", ");
        sb.append(this.modemCognitive);
        sb.append(", ");
        sb.append(this.maxConns);
        sb.append(", ");
        sb.append(this.waitTime);
        sb.append(", ");
        sb.append(this.maxConnsTime);
        sb.append(", ");
        sb.append(this.mtu);
        sb.append(", ");
        sb.append(this.mvnoType);
        sb.append(", ");
        sb.append(this.mvnoMatchData);
        sb.append(", ");
        sb.append(this.permanentFailed);
        sb.append(", ");
        sb.append(this.networkTypeBitmask);
        sb.append(", ");
        sb.append(this.apnSetId);
        return sb.toString();
    }

    public boolean hasMvnoParams() {
        return (TextUtils.isEmpty(this.mvnoType) || TextUtils.isEmpty(this.mvnoMatchData)) ? false : true;
    }

    public boolean canHandleType(String str) {
        if (!this.carrierEnabled) {
            return false;
        }
        boolean z = !"ia".equalsIgnoreCase(str);
        for (String str2 : this.types) {
            if (str2.equalsIgnoreCase(str) || ((z && str2.equalsIgnoreCase(CharacterSets.MIMENAME_ANY_CHARSET)) || (str2.equalsIgnoreCase("default") && str.equalsIgnoreCase("hipri")))) {
                return true;
            }
        }
        return false;
    }

    private static boolean iccidMatches(String str, String str2) {
        for (String str3 : str.split(",")) {
            if (str2.startsWith(str3)) {
                Log.d("ApnSetting", "mvno icc id match found");
                return true;
            }
        }
        return false;
    }

    public static boolean imsiMatches(String str, String str2) {
        int length = str.length();
        if (length <= 0 || length > str2.length()) {
            return false;
        }
        for (int i = 0; i < length; i++) {
            char cCharAt = str.charAt(i);
            if (cCharAt != 'x' && cCharAt != 'X' && cCharAt != str2.charAt(i)) {
                return false;
            }
        }
        return true;
    }

    public static boolean mvnoMatches(IccRecords iccRecords, String str, String str2) {
        String iccId;
        if (str.equalsIgnoreCase("spn")) {
            if (iccRecords.getServiceProviderName() != null && iccRecords.getServiceProviderName().equalsIgnoreCase(str2)) {
                return true;
            }
        } else if (str.equalsIgnoreCase("imsi")) {
            String imsi = iccRecords.getIMSI();
            if (imsi != null && imsiMatches(str2, imsi)) {
                return true;
            }
        } else if (str.equalsIgnoreCase("gid")) {
            String gid1 = iccRecords.getGid1();
            int length = str2.length();
            if (gid1 != null && gid1.length() >= length && gid1.substring(0, length).equalsIgnoreCase(str2)) {
                return true;
            }
        } else if (str.equalsIgnoreCase("iccid") && (iccId = iccRecords.getIccId()) != null && iccidMatches(str2, iccId)) {
            return true;
        }
        try {
            if (sMethodMvnoMatchesEx != null) {
                return ((Boolean) sMethodMvnoMatchesEx.invoke(null, iccRecords, str, str2)).booleanValue();
            }
        } catch (Exception e) {
            Rlog.d("ApnSetting", e.toString());
        }
        return false;
    }

    public static boolean isMeteredApnType(String str, Phone phone) {
        String str2;
        if (phone == null) {
            return true;
        }
        try {
            if (sMethodIsMeteredApnTypeEx != null) {
                Bundle bundle = (Bundle) sMethodIsMeteredApnTypeEx.invoke(null, str, phone);
                if (bundle.getBoolean("useEx")) {
                    return bundle.getBoolean("result");
                }
            }
        } catch (Exception e) {
            Rlog.d("ApnSetting", e.toString());
        }
        boolean dataRoaming = phone.getServiceState().getDataRoaming();
        boolean z = phone.getServiceState().getRilDataRadioTechnology() == 18;
        int subId = phone.getSubId();
        if (z) {
            str2 = "carrier_metered_iwlan_apn_types_strings";
        } else if (dataRoaming) {
            str2 = "carrier_metered_roaming_apn_types_strings";
        } else {
            str2 = "carrier_metered_apn_types_strings";
        }
        CarrierConfigManager carrierConfigManager = (CarrierConfigManager) phone.getContext().getSystemService("carrier_config");
        if (carrierConfigManager == null) {
            Rlog.e("ApnSetting", "Carrier config service is not available");
            return true;
        }
        PersistableBundle configForSubId = carrierConfigManager.getConfigForSubId(subId);
        if (configForSubId == null) {
            Rlog.e("ApnSetting", "Can't get the config. subId = " + subId);
            return true;
        }
        String[] stringArray = configForSubId.getStringArray(str2);
        if (stringArray == null) {
            Rlog.e("ApnSetting", str2 + " is not available. subId = " + subId);
            return true;
        }
        HashSet hashSet = new HashSet(Arrays.asList(stringArray));
        if (hashSet.contains(CharacterSets.MIMENAME_ANY_CHARSET) || hashSet.contains(str)) {
            return true;
        }
        if (str.equals(CharacterSets.MIMENAME_ANY_CHARSET) && hashSet.size() > 0) {
            return true;
        }
        return false;
    }

    public boolean isMetered(Phone phone) {
        if (phone == null) {
            return true;
        }
        for (String str : this.types) {
            if (isMeteredApnType(str, phone)) {
                return true;
            }
        }
        return false;
    }

    public boolean equals(Object obj) {
        if (!(obj instanceof ApnSetting)) {
            return false;
        }
        ApnSetting apnSetting = (ApnSetting) obj;
        return this.carrier.equals(apnSetting.carrier) && this.id == apnSetting.id && this.numeric.equals(apnSetting.numeric) && this.apn.equals(apnSetting.apn) && this.proxy.equals(apnSetting.proxy) && this.mmsc.equals(apnSetting.mmsc) && this.mmsProxy.equals(apnSetting.mmsProxy) && TextUtils.equals(this.mmsPort, apnSetting.mmsPort) && this.port.equals(apnSetting.port) && TextUtils.equals(this.user, apnSetting.user) && TextUtils.equals(this.password, apnSetting.password) && this.authType == apnSetting.authType && Arrays.deepEquals(this.types, apnSetting.types) && this.typesBitmap == apnSetting.typesBitmap && this.protocol.equals(apnSetting.protocol) && this.roamingProtocol.equals(apnSetting.roamingProtocol) && this.carrierEnabled == apnSetting.carrierEnabled && this.bearer == apnSetting.bearer && this.bearerBitmask == apnSetting.bearerBitmask && this.profileId == apnSetting.profileId && this.modemCognitive == apnSetting.modemCognitive && this.maxConns == apnSetting.maxConns && this.waitTime == apnSetting.waitTime && this.maxConnsTime == apnSetting.maxConnsTime && this.mtu == apnSetting.mtu && this.mvnoType.equals(apnSetting.mvnoType) && this.mvnoMatchData.equals(apnSetting.mvnoMatchData) && this.networkTypeBitmask == apnSetting.networkTypeBitmask && this.apnSetId == apnSetting.apnSetId;
    }

    @VisibleForTesting(visibility = VisibleForTesting.Visibility.PACKAGE)
    public boolean equals(Object obj, boolean z) {
        if (!(obj instanceof ApnSetting)) {
            return false;
        }
        ApnSetting apnSetting = (ApnSetting) obj;
        if (!this.carrier.equals(apnSetting.carrier) || !this.numeric.equals(apnSetting.numeric) || !this.apn.equals(apnSetting.apn) || !this.proxy.equals(apnSetting.proxy) || !this.mmsc.equals(apnSetting.mmsc) || !this.mmsProxy.equals(apnSetting.mmsProxy) || !TextUtils.equals(this.mmsPort, apnSetting.mmsPort) || !this.port.equals(apnSetting.port) || !TextUtils.equals(this.user, apnSetting.user) || !TextUtils.equals(this.password, apnSetting.password) || this.authType != apnSetting.authType || !Arrays.deepEquals(this.types, apnSetting.types) || this.typesBitmap != apnSetting.typesBitmap) {
            return false;
        }
        if (z || this.protocol.equals(apnSetting.protocol)) {
            return (!z || this.roamingProtocol.equals(apnSetting.roamingProtocol)) && this.carrierEnabled == apnSetting.carrierEnabled && this.profileId == apnSetting.profileId && this.modemCognitive == apnSetting.modemCognitive && this.maxConns == apnSetting.maxConns && this.waitTime == apnSetting.waitTime && this.maxConnsTime == apnSetting.maxConnsTime && this.mtu == apnSetting.mtu && this.mvnoType.equals(apnSetting.mvnoType) && this.mvnoMatchData.equals(apnSetting.mvnoMatchData) && this.apnSetId == apnSetting.apnSetId;
        }
        return false;
    }

    public boolean similar(ApnSetting apnSetting) {
        return !canHandleType("dun") && !apnSetting.canHandleType("dun") && Objects.equals(this.apn, apnSetting.apn) && !typeSameAny(this, apnSetting) && xorEquals(this.proxy, apnSetting.proxy) && xorEquals(this.port, apnSetting.port) && xorEquals(this.protocol, apnSetting.protocol) && xorEquals(this.roamingProtocol, apnSetting.roamingProtocol) && this.carrierEnabled == apnSetting.carrierEnabled && this.bearerBitmask == apnSetting.bearerBitmask && this.profileId == apnSetting.profileId && Objects.equals(this.mvnoType, apnSetting.mvnoType) && Objects.equals(this.mvnoMatchData, apnSetting.mvnoMatchData) && xorEquals(this.mmsc, apnSetting.mmsc) && xorEquals(this.mmsProxy, apnSetting.mmsProxy) && xorEquals(this.mmsPort, apnSetting.mmsPort) && this.networkTypeBitmask == apnSetting.networkTypeBitmask && this.apnSetId == apnSetting.apnSetId;
    }

    private boolean typeSameAny(ApnSetting apnSetting, ApnSetting apnSetting2) {
        for (int i = 0; i < apnSetting.types.length; i++) {
            for (int i2 = 0; i2 < apnSetting2.types.length; i2++) {
                if (apnSetting.types[i].equals(CharacterSets.MIMENAME_ANY_CHARSET) || apnSetting2.types[i2].equals(CharacterSets.MIMENAME_ANY_CHARSET) || apnSetting.types[i].equals(apnSetting2.types[i2])) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean xorEquals(String str, String str2) {
        return Objects.equals(str, str2) || TextUtils.isEmpty(str) || TextUtils.isEmpty(str2);
    }

    private static int getApnBitmask(String str) {
        try {
            if (sMethodgetApnBitmaskEx != null) {
                return ((Integer) sMethodgetApnBitmaskEx.invoke(null, str)).intValue();
            }
        } catch (Exception e) {
            Rlog.d("ApnSetting", e.toString());
        }
        switch (str) {
            case "default":
                return 1;
            case "mms":
                return 2;
            case "supl":
                return 4;
            case "dun":
                return 8;
            case "hipri":
                return 16;
            case "fota":
                return 32;
            case "ims":
                return 64;
            case "cbs":
                return 128;
            case "ia":
                return 256;
            case "emergency":
                return 512;
            case "*":
                return ApnTypes.ALL;
            default:
                return 0;
        }
    }
}
