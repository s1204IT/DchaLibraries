package com.mediatek.internal.telephony.dataconnection;

import android.os.Bundle;
import android.os.SystemProperties;
import android.telephony.Rlog;
import android.text.TextUtils;
import android.util.Log;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.dataconnection.ApnSetting;
import com.android.internal.telephony.uicc.IccRecords;
import com.mediatek.internal.telephony.OpTelephonyCustomizationUtils;
import com.mediatek.internal.telephony.ppl.PplMessageManager;
import java.util.Iterator;
import java.util.List;
import vendor.mediatek.hardware.radio.V3_0.MtkApnTypes;

public class MtkApnSetting extends ApnSetting {
    private static final boolean DBG = true;
    private static final String LOG_TAG = "MtkApnSetting";
    private static final boolean VDBG;
    private static IDataConnectionExt sDataConnectionExt;
    public final int inactiveTimer;

    static {
        VDBG = SystemProperties.get("ro.build.type").equals("eng");
        sDataConnectionExt = null;
    }

    public MtkApnSetting(int i, String str, String str2, String str3, String str4, String str5, String str6, String str7, String str8, String str9, String str10, int i2, String[] strArr, String str11, String str12, boolean z, int i3, int i4, int i5, boolean z2, int i6, int i7, int i8, int i9, String str13, String str14, int i10) {
        super(i, str, str2, str3, str4, str5, str6, str7, str8, str9, str10, i2, strArr, str11, str12, z, i3, i4, i5, z2, i6, i7, i8, i9, str13, str14);
        this.inactiveTimer = i10;
    }

    public MtkApnSetting(int i, String str, String str2, String str3, String str4, String str5, String str6, String str7, String str8, String str9, String str10, int i2, String[] strArr, String str11, String str12, boolean z, int i3, int i4, boolean z2, int i5, int i6, int i7, int i8, String str13, String str14, int i9) {
        super(i, str, str2, str3, str4, str5, str6, str7, str8, str9, str10, i2, strArr, str11, str12, z, i3, i4, z2, i5, i6, i7, i8, str13, str14);
        this.inactiveTimer = i9;
    }

    public MtkApnSetting(int i, String str, String str2, String str3, String str4, String str5, String str6, String str7, String str8, String str9, String str10, int i2, String[] strArr, String str11, String str12, boolean z, int i3, int i4, boolean z2, int i5, int i6, int i7, int i8, String str13, String str14, int i9, int i10) {
        super(i, str, str2, str3, str4, str5, str6, str7, str8, str9, str10, i2, strArr, str11, str12, z, i3, i4, z2, i5, i6, i7, i8, str13, str14, i9);
        this.inactiveTimer = i10;
    }

    public MtkApnSetting(MtkApnSetting mtkApnSetting) {
        this(mtkApnSetting.id, mtkApnSetting.numeric, mtkApnSetting.carrier, mtkApnSetting.apn, mtkApnSetting.proxy, mtkApnSetting.port, mtkApnSetting.mmsc, mtkApnSetting.mmsProxy, mtkApnSetting.mmsPort, mtkApnSetting.user, mtkApnSetting.password, mtkApnSetting.authType, mtkApnSetting.types, mtkApnSetting.protocol, mtkApnSetting.roamingProtocol, mtkApnSetting.carrierEnabled, mtkApnSetting.networkTypeBitmask, mtkApnSetting.profileId, mtkApnSetting.modemCognitive, mtkApnSetting.maxConns, mtkApnSetting.waitTime, mtkApnSetting.maxConnsTime, mtkApnSetting.mtu, mtkApnSetting.mvnoType, mtkApnSetting.mvnoMatchData, mtkApnSetting.apnSetId, mtkApnSetting.inactiveTimer);
    }

    private static ApnSetting fromStringEx(String[] strArr, int i, String[] strArr2, String str, String str2, boolean z, int i2, int i3, boolean z2, int i4, int i5, int i6, int i7, String str3, String str4, int i8) {
        int i9;
        if (strArr.length > 28) {
            try {
                i9 = Integer.parseInt(strArr[28]);
            } catch (NumberFormatException e) {
                Rlog.e(LOG_TAG, "NumberFormatException, inactive timer = " + strArr[28]);
                i9 = 0;
            }
        } else {
            i9 = 0;
        }
        return new MtkApnSetting(-1, strArr[10] + strArr[11], strArr[0], strArr[1], strArr[2], strArr[3], strArr[7], strArr[8], strArr[9], strArr[4], strArr[5], i, strArr2, str, str2, z, i2, i3, z2, i4, i5, i6, i7, str3, str4, i8, i9);
    }

    public String toString() {
        return super.toString() + ", " + this.inactiveTimer;
    }

    public boolean canHandleType(String str) {
        if (!this.carrierEnabled) {
            return false;
        }
        boolean z = !"ia".equalsIgnoreCase(str);
        for (String str2 : this.types) {
            if (VDBG) {
                Log.v(LOG_TAG, "canHandleType(): entry in types=" + str2 + ", reqType=" + str);
            }
            if (str2.equalsIgnoreCase(str) || (z && str2.equalsIgnoreCase("*") && !str.equalsIgnoreCase("ims") && !str.equalsIgnoreCase("emergency"))) {
                return true;
            }
            if (str2.equalsIgnoreCase("default") && str.equalsIgnoreCase("hipri")) {
                Log.d(LOG_TAG, "canHandleType(): use DEFAULT for HIPRI type");
                return true;
            }
        }
        return false;
    }

    private static boolean mvnoMatchesEx(IccRecords iccRecords, String str, String str2) {
        if (str.equalsIgnoreCase("pnn") && iccRecords.isOperatorMvnoForEfPnn() != null && iccRecords.isOperatorMvnoForEfPnn().equalsIgnoreCase(str2)) {
            return true;
        }
        return false;
    }

    private static Bundle isMeteredApnTypeEx(String str, Phone phone) {
        boolean zIsMeteredApnType;
        boolean dataRoaming = phone.getServiceState().getDataRoaming();
        if (sDataConnectionExt == null) {
            try {
                sDataConnectionExt = OpTelephonyCustomizationUtils.getOpFactory(phone.getContext()).makeDataConnectionExt(phone.getContext());
            } catch (Exception e) {
                Rlog.e(LOG_TAG, "sDataConnectionExt init fail. e: " + e);
                sDataConnectionExt = null;
            }
        }
        boolean z = false;
        if (sDataConnectionExt == null || !sDataConnectionExt.isMeteredApnTypeByLoad()) {
            zIsMeteredApnType = false;
        } else {
            zIsMeteredApnType = sDataConnectionExt.isMeteredApnType(str, dataRoaming);
            z = true;
        }
        if (TextUtils.equals(str, "preempt")) {
            zIsMeteredApnType = true;
            z = true;
        }
        Bundle bundle = new Bundle();
        bundle.putBoolean("useEx", z);
        bundle.putBoolean("result", zIsMeteredApnType);
        return bundle;
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

    public String toStringIgnoreName(boolean z) {
        StringBuilder sb = new StringBuilder();
        sb.append(this.id);
        if (!z) {
            sb.append(", ");
            sb.append(this.carrier);
        }
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
        sb.append(", ");
        sb.append(this.user);
        Rlog.d(LOG_TAG, "toStringIgnoreName: sb = " + sb.toString() + ", ignoreName: " + z);
        sb.append(", ");
        sb.append(this.password);
        return sb.toString();
    }

    public static String toStringIgnoreNameForList(List<ApnSetting> list, boolean z) {
        if (list == null || list.size() == 0) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        Iterator<ApnSetting> it = list.iterator();
        while (it.hasNext()) {
            sb.append(((MtkApnSetting) it.next()).toStringIgnoreName(z));
        }
        return sb.toString();
    }

    public static int getApnBitmaskEx(String str) {
        byte b;
        switch (str.hashCode()) {
            case 42:
                b = !str.equals("*") ? (byte) -1 : (byte) 15;
                break;
            case 3352:
                if (str.equals("ia")) {
                    b = 8;
                    break;
                }
                break;
            case 97545:
                if (str.equals("bip")) {
                    b = 13;
                    break;
                }
                break;
            case 98292:
                if (str.equals("cbs")) {
                    b = 7;
                    break;
                }
                break;
            case 99837:
                if (str.equals("dun")) {
                    b = 3;
                    break;
                }
                break;
            case 104399:
                if (str.equals("ims")) {
                    b = 6;
                    break;
                }
                break;
            case 108243:
                if (str.equals("mms")) {
                    b = 1;
                    break;
                }
                break;
            case 112738:
                if (str.equals("rcs")) {
                    b = 12;
                    break;
                }
                break;
            case 117478:
                if (str.equals("wap")) {
                    b = 10;
                    break;
                }
                break;
            case 3149046:
                if (str.equals("fota")) {
                    b = 5;
                    break;
                }
                break;
            case 3541982:
                if (str.equals("supl")) {
                    b = 2;
                    break;
                }
                break;
            case 3629217:
                if (str.equals("vsim")) {
                    b = 14;
                    break;
                }
                break;
            case 3673178:
                if (str.equals("xcap")) {
                    b = PplMessageManager.Type.INSTRUCTION_DESCRIPTION2;
                    break;
                }
                break;
            case 99285510:
                if (str.equals("hipri")) {
                    b = 4;
                    break;
                }
                break;
            case 1544803905:
                if (str.equals("default")) {
                    b = 0;
                    break;
                }
                break;
            case 1629013393:
                if (str.equals("emergency")) {
                    b = 9;
                    break;
                }
                break;
        }
        switch (b) {
            case 0:
                return 1;
            case 1:
                return 2;
            case 2:
                return 4;
            case 3:
                return 8;
            case 4:
                return 16;
            case 5:
                return 32;
            case 6:
                return 64;
            case 7:
                return 128;
            case 8:
                return 256;
            case 9:
                return 512;
            case 10:
                return 1024;
            case 11:
                return 2048;
            case 12:
                return 4096;
            case 13:
                return 8192;
            case 14:
                return 16384;
            case 15:
                return MtkApnTypes.MTKALL;
            default:
                return 0;
        }
    }
}
