package com.android.internal.telephony;

import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.os.Binder;
import android.os.Build;
import android.os.PersistableBundle;
import android.telephony.CarrierConfigManager;
import android.telephony.PhoneNumberUtils;
import android.telephony.Rlog;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import com.android.internal.telephony.HbpcdLookup;
import java.util.ArrayList;
import java.util.HashMap;

public class SmsNumberUtils {
    private static final int CDMA_HOME_NETWORK = 1;
    private static final int CDMA_ROAMING_NETWORK = 2;
    private static final int GSM_UMTS_NETWORK = 0;
    private static int MAX_COUNTRY_CODES_LENGTH = 0;
    private static final int MIN_COUNTRY_AREA_LOCAL_LENGTH = 10;
    private static final int NANP_CC = 1;
    private static final String NANP_IDD = "011";
    private static final int NANP_LONG_LENGTH = 11;
    private static final int NANP_MEDIUM_LENGTH = 10;
    private static final String NANP_NDD = "1";
    private static final int NANP_SHORT_LENGTH = 7;
    private static final int NP_CC_AREA_LOCAL = 104;
    private static final int NP_HOMEIDD_CC_AREA_LOCAL = 101;
    private static final int NP_INTERNATIONAL_BEGIN = 100;
    private static final int NP_LOCALIDD_CC_AREA_LOCAL = 103;
    private static final int NP_NANP_AREA_LOCAL = 2;
    private static final int NP_NANP_BEGIN = 1;
    private static final int NP_NANP_LOCAL = 1;
    private static final int NP_NANP_LOCALIDD_CC_AREA_LOCAL = 5;
    private static final int NP_NANP_NBPCD_CC_AREA_LOCAL = 4;
    private static final int NP_NANP_NBPCD_HOMEIDD_CC_AREA_LOCAL = 6;
    private static final int NP_NANP_NDD_AREA_LOCAL = 3;
    private static final int NP_NBPCD_CC_AREA_LOCAL = 102;
    private static final int NP_NBPCD_HOMEIDD_CC_AREA_LOCAL = 100;
    private static final int NP_NONE = 0;
    private static final String PLUS_SIGN = "+";
    private static final String TAG = "SmsNumberUtils";
    private static final boolean DBG = Build.IS_DEBUGGABLE;
    private static int[] ALL_COUNTRY_CODES = null;
    private static HashMap<String, ArrayList<String>> IDDS_MAPS = new HashMap<>();

    private static class NumberEntry {
        public String IDD;
        public int countryCode;
        public String number;

        public NumberEntry(String str) {
            this.number = str;
        }
    }

    private static String formatNumber(Context context, String str, String str2, int i) throws Throwable {
        String strSubstring;
        if (str == null) {
            throw new IllegalArgumentException("number is null");
        }
        if (str2 == null || str2.trim().length() == 0) {
            throw new IllegalArgumentException("activeMcc is null or empty!");
        }
        String strExtractNetworkPortion = PhoneNumberUtils.extractNetworkPortion(str);
        if (strExtractNetworkPortion == null || strExtractNetworkPortion.length() == 0) {
            throw new IllegalArgumentException("Number is invalid!");
        }
        NumberEntry numberEntry = new NumberEntry(strExtractNetworkPortion);
        ArrayList<String> allIDDs = getAllIDDs(context, str2);
        int iCheckNANP = checkNANP(numberEntry, allIDDs);
        if (DBG) {
            Rlog.d(TAG, "NANP type: " + getNumberPlanType(iCheckNANP));
        }
        if (iCheckNANP == 1 || iCheckNANP == 2 || iCheckNANP == 3) {
            return strExtractNetworkPortion;
        }
        if (iCheckNANP == 4) {
            if (i == 1 || i == 2) {
                return strExtractNetworkPortion.substring(1);
            }
            return strExtractNetworkPortion;
        }
        if (iCheckNANP == 5) {
            if (i == 1) {
                return strExtractNetworkPortion;
            }
            if (i == 0) {
                return PLUS_SIGN + strExtractNetworkPortion.substring(numberEntry.IDD != null ? numberEntry.IDD.length() : 0);
            }
            if (i == 2) {
                return strExtractNetworkPortion.substring(numberEntry.IDD != null ? numberEntry.IDD.length() : 0);
            }
        }
        int iCheckInternationalNumberPlan = checkInternationalNumberPlan(context, numberEntry, allIDDs, NANP_IDD);
        if (DBG) {
            Rlog.d(TAG, "International type: " + getNumberPlanType(iCheckInternationalNumberPlan));
        }
        switch (iCheckInternationalNumberPlan) {
            case 100:
                strSubstring = i == 0 ? strExtractNetworkPortion.substring(1) : null;
                break;
            case 101:
                strSubstring = strExtractNetworkPortion;
                break;
            case 102:
                strSubstring = NANP_IDD + strExtractNetworkPortion.substring(1);
                break;
            case NP_LOCALIDD_CC_AREA_LOCAL:
                if (i == 0 || i == 2) {
                    strSubstring = NANP_IDD + strExtractNetworkPortion.substring(numberEntry.IDD != null ? numberEntry.IDD.length() : 0);
                    break;
                }
                break;
            case NP_CC_AREA_LOCAL:
                int i2 = numberEntry.countryCode;
                if (!inExceptionListForNpCcAreaLocal(numberEntry) && strExtractNetworkPortion.length() >= 11 && i2 != 1) {
                    strSubstring = NANP_IDD + strExtractNetworkPortion;
                    break;
                }
                break;
            default:
                if (strExtractNetworkPortion.startsWith(PLUS_SIGN) && (i == 1 || i == 2)) {
                    strSubstring = strExtractNetworkPortion.startsWith("+011") ? strExtractNetworkPortion.substring(1) : NANP_IDD + strExtractNetworkPortion.substring(1);
                    break;
                }
                break;
        }
        return strSubstring == null ? strExtractNetworkPortion : strSubstring;
    }

    private static ArrayList<String> getAllIDDs(Context context, String str) throws Throwable {
        String str2;
        String[] strArr;
        Cursor cursorQuery;
        ArrayList<String> arrayList = IDDS_MAPS.get(str);
        if (arrayList != null) {
            return arrayList;
        }
        ArrayList<String> arrayList2 = new ArrayList<>();
        String[] strArr2 = {HbpcdLookup.MccIdd.IDD, "MCC"};
        Cursor cursor = null;
        if (str != null) {
            str2 = "MCC=?";
            strArr = new String[]{str};
        } else {
            str2 = null;
            strArr = null;
        }
        try {
            try {
                cursorQuery = context.getContentResolver().query(HbpcdLookup.MccIdd.CONTENT_URI, strArr2, str2, strArr, null);
            } catch (SQLException e) {
                e = e;
            }
        } catch (Throwable th) {
            th = th;
        }
        try {
            if (cursorQuery.getCount() > 0) {
                while (cursorQuery.moveToNext()) {
                    String string = cursorQuery.getString(0);
                    if (!arrayList2.contains(string)) {
                        arrayList2.add(string);
                    }
                }
            }
            if (cursorQuery != null) {
                cursorQuery.close();
            }
        } catch (SQLException e2) {
            e = e2;
            cursor = cursorQuery;
            Rlog.e(TAG, "Can't access HbpcdLookup database", e);
            if (cursor != null) {
                cursor.close();
            }
        } catch (Throwable th2) {
            th = th2;
            cursor = cursorQuery;
            if (cursor != null) {
                cursor.close();
            }
            throw th;
        }
        IDDS_MAPS.put(str, arrayList2);
        if (DBG) {
            Rlog.d(TAG, "MCC = " + str + ", all IDDs = " + arrayList2);
        }
        return arrayList2;
    }

    private static int checkNANP(NumberEntry numberEntry, ArrayList<String> arrayList) {
        String strSubstring;
        boolean z;
        String str = numberEntry.number;
        if (str.length() == 7) {
            char cCharAt = str.charAt(0);
            if (cCharAt < '2' || cCharAt > '9') {
                z = false;
                if (!z) {
                    return 1;
                }
            } else {
                for (int i = 1; i < 7; i++) {
                    if (!PhoneNumberUtils.isISODigit(str.charAt(i))) {
                        z = false;
                        break;
                    }
                }
                z = true;
                if (!z) {
                }
            }
        } else if (str.length() == 10) {
            if (isNANP(str)) {
                return 2;
            }
        } else if (str.length() == 11) {
            if (isNANP(str)) {
                return 3;
            }
        } else if (str.startsWith(PLUS_SIGN)) {
            String strSubstring2 = str.substring(1);
            if (strSubstring2.length() == 11) {
                if (isNANP(strSubstring2)) {
                    return 4;
                }
            } else if (strSubstring2.startsWith(NANP_IDD) && strSubstring2.length() == 14 && isNANP(strSubstring2.substring(3))) {
                return 6;
            }
        } else {
            for (String str2 : arrayList) {
                if (str.startsWith(str2) && (strSubstring = str.substring(str2.length())) != null && strSubstring.startsWith(String.valueOf(1)) && isNANP(strSubstring)) {
                    numberEntry.IDD = str2;
                    return 5;
                }
            }
        }
        return 0;
    }

    private static boolean isNANP(String str) {
        if (str.length() == 10 || (str.length() == 11 && str.startsWith(NANP_NDD))) {
            if (str.length() == 11) {
                str = str.substring(1);
            }
            return PhoneNumberUtils.isNanp(str);
        }
        return false;
    }

    private static int checkInternationalNumberPlan(Context context, NumberEntry numberEntry, ArrayList<String> arrayList, String str) {
        int countryCode;
        int countryCode2;
        String str2 = numberEntry.number;
        if (str2.startsWith(PLUS_SIGN)) {
            String strSubstring = str2.substring(1);
            if (strSubstring.startsWith(str)) {
                int countryCode3 = getCountryCode(context, strSubstring.substring(str.length()));
                if (countryCode3 > 0) {
                    numberEntry.countryCode = countryCode3;
                    return 100;
                }
                return 0;
            }
            int countryCode4 = getCountryCode(context, strSubstring);
            if (countryCode4 > 0) {
                numberEntry.countryCode = countryCode4;
                return 102;
            }
            return 0;
        }
        if (str2.startsWith(str)) {
            int countryCode5 = getCountryCode(context, str2.substring(str.length()));
            if (countryCode5 > 0) {
                numberEntry.countryCode = countryCode5;
                return 101;
            }
            return 0;
        }
        for (String str3 : arrayList) {
            if (str2.startsWith(str3) && (countryCode2 = getCountryCode(context, str2.substring(str3.length()))) > 0) {
                numberEntry.countryCode = countryCode2;
                numberEntry.IDD = str3;
                return NP_LOCALIDD_CC_AREA_LOCAL;
            }
        }
        if (!str2.startsWith("0") && (countryCode = getCountryCode(context, str2)) > 0) {
            numberEntry.countryCode = countryCode;
            return NP_CC_AREA_LOCAL;
        }
        return 0;
    }

    private static int getCountryCode(Context context, String str) {
        int[] allCountryCodes;
        if (str.length() < 10 || (allCountryCodes = getAllCountryCodes(context)) == null) {
            return -1;
        }
        int[] iArr = new int[MAX_COUNTRY_CODES_LENGTH];
        int i = 0;
        while (i < MAX_COUNTRY_CODES_LENGTH) {
            int i2 = i + 1;
            iArr[i] = Integer.parseInt(str.substring(0, i2));
            i = i2;
        }
        for (int i3 : allCountryCodes) {
            for (int i4 = 0; i4 < MAX_COUNTRY_CODES_LENGTH; i4++) {
                if (i3 == iArr[i4]) {
                    if (DBG) {
                        Rlog.d(TAG, "Country code = " + i3);
                    }
                    return i3;
                }
            }
        }
        return -1;
    }

    private static int[] getAllCountryCodes(Context context) throws Throwable {
        Throwable th;
        Cursor cursorQuery;
        SQLException e;
        if (ALL_COUNTRY_CODES != null) {
            return ALL_COUNTRY_CODES;
        }
        try {
            cursorQuery = context.getContentResolver().query(HbpcdLookup.MccLookup.CONTENT_URI, new String[]{HbpcdLookup.MccLookup.COUNTRY_CODE}, null, null, null);
            try {
                try {
                    if (cursorQuery.getCount() > 0) {
                        ALL_COUNTRY_CODES = new int[cursorQuery.getCount()];
                        int i = 0;
                        while (cursorQuery.moveToNext()) {
                            int i2 = cursorQuery.getInt(0);
                            int i3 = i + 1;
                            ALL_COUNTRY_CODES[i] = i2;
                            int length = String.valueOf(i2).trim().length();
                            if (length > MAX_COUNTRY_CODES_LENGTH) {
                                MAX_COUNTRY_CODES_LENGTH = length;
                            }
                            i = i3;
                        }
                    }
                } catch (SQLException e2) {
                    e = e2;
                    Rlog.e(TAG, "Can't access HbpcdLookup database", e);
                    if (cursorQuery != null) {
                    }
                }
            } catch (Throwable th2) {
                th = th2;
                if (cursorQuery != null) {
                    cursorQuery.close();
                }
                throw th;
            }
        } catch (SQLException e3) {
            e = e3;
            cursorQuery = null;
        } catch (Throwable th3) {
            th = th3;
            cursorQuery = null;
            if (cursorQuery != null) {
            }
            throw th;
        }
        if (cursorQuery != null) {
            cursorQuery.close();
        }
        return ALL_COUNTRY_CODES;
    }

    private static boolean inExceptionListForNpCcAreaLocal(NumberEntry numberEntry) {
        int i = numberEntry.countryCode;
        return numberEntry.number.length() == 12 && (i == 7 || i == 20 || i == 65 || i == 90);
    }

    private static String getNumberPlanType(int i) {
        String str = "Number Plan type (" + i + "): ";
        if (i == 1) {
            return "NP_NANP_LOCAL";
        }
        if (i == 2) {
            return "NP_NANP_AREA_LOCAL";
        }
        if (i == 3) {
            return "NP_NANP_NDD_AREA_LOCAL";
        }
        if (i == 4) {
            return "NP_NANP_NBPCD_CC_AREA_LOCAL";
        }
        if (i == 5) {
            return "NP_NANP_LOCALIDD_CC_AREA_LOCAL";
        }
        if (i == 6) {
            return "NP_NANP_NBPCD_HOMEIDD_CC_AREA_LOCAL";
        }
        if (i == 100) {
            return "NP_NBPCD_HOMEIDD_CC_AREA_LOCAL";
        }
        if (i == 101) {
            return "NP_HOMEIDD_CC_AREA_LOCAL";
        }
        if (i == 102) {
            return "NP_NBPCD_CC_AREA_LOCAL";
        }
        if (i == NP_LOCALIDD_CC_AREA_LOCAL) {
            return "NP_LOCALIDD_CC_AREA_LOCAL";
        }
        if (i == NP_CC_AREA_LOCAL) {
            return "NP_CC_AREA_LOCAL";
        }
        return "Unknown type";
    }

    public static String filterDestAddr(Phone phone, String str) {
        String number;
        int networkType;
        String strSubstring;
        if (DBG) {
            Rlog.d(TAG, "enter filterDestAddr. destAddr=\"" + Rlog.pii(TAG, str) + "\"");
        }
        if (str == null || !PhoneNumberUtils.isGlobalPhoneNumber(str)) {
            Rlog.w(TAG, "destAddr" + Rlog.pii(TAG, str) + " is not a global phone number! Nothing changed.");
            return str;
        }
        String networkOperator = TelephonyManager.from(phone.getContext()).getNetworkOperator(phone.getSubId());
        if (needToConvert(phone) && (networkType = getNetworkType(phone)) != -1 && !TextUtils.isEmpty(networkOperator) && (strSubstring = networkOperator.substring(0, 3)) != null && strSubstring.trim().length() > 0) {
            number = formatNumber(phone.getContext(), str, strSubstring, networkType);
        } else {
            number = null;
        }
        if (DBG) {
            StringBuilder sb = new StringBuilder();
            sb.append("destAddr is ");
            sb.append(number != null ? "formatted." : "not formatted.");
            Rlog.d(TAG, sb.toString());
            StringBuilder sb2 = new StringBuilder();
            sb2.append("leave filterDestAddr, new destAddr=\"");
            sb2.append(number != null ? Rlog.pii(TAG, number) : Rlog.pii(TAG, str));
            sb2.append("\"");
            Rlog.d(TAG, sb2.toString());
        }
        return number != null ? number : str;
    }

    private static int getNetworkType(Phone phone) {
        int phoneType = phone.getPhoneType();
        if (phoneType == 1) {
            return 0;
        }
        if (phoneType == 2) {
            return isInternationalRoaming(phone) ? 2 : 1;
        }
        if (DBG) {
            Rlog.w(TAG, "warning! unknown mPhoneType value=" + phoneType);
        }
        return -1;
    }

    private static boolean isInternationalRoaming(Phone phone) {
        boolean z;
        boolean zEquals;
        String networkCountryIsoForPhone = TelephonyManager.from(phone.getContext()).getNetworkCountryIsoForPhone(phone.getPhoneId());
        String simCountryIsoForPhone = TelephonyManager.from(phone.getContext()).getSimCountryIsoForPhone(phone.getPhoneId());
        if (TextUtils.isEmpty(networkCountryIsoForPhone) || TextUtils.isEmpty(simCountryIsoForPhone) || simCountryIsoForPhone.equals(networkCountryIsoForPhone)) {
            z = false;
        } else {
            z = true;
        }
        if (z) {
            if ("us".equals(simCountryIsoForPhone)) {
                zEquals = "vi".equals(networkCountryIsoForPhone);
            } else if ("vi".equals(simCountryIsoForPhone)) {
                zEquals = "us".equals(networkCountryIsoForPhone);
            } else {
                return z;
            }
            return !zEquals;
        }
        return z;
    }

    private static boolean needToConvert(Phone phone) {
        PersistableBundle config;
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        try {
            CarrierConfigManager carrierConfigManager = (CarrierConfigManager) phone.getContext().getSystemService("carrier_config");
            if (carrierConfigManager != null && (config = carrierConfigManager.getConfig()) != null) {
                return config.getBoolean("sms_requires_destination_number_conversion_bool");
            }
            Binder.restoreCallingIdentity(jClearCallingIdentity);
            return false;
        } finally {
            Binder.restoreCallingIdentity(jClearCallingIdentity);
        }
    }

    private static boolean compareGid1(Phone phone, String str) {
        String groupIdLevel1 = phone.getGroupIdLevel1();
        boolean z = true;
        if (TextUtils.isEmpty(str)) {
            if (DBG) {
                Rlog.d(TAG, "compareGid1 serviceGid is empty, return true");
            }
            return true;
        }
        int length = str.length();
        if (groupIdLevel1 == null || groupIdLevel1.length() < length || !groupIdLevel1.substring(0, length).equalsIgnoreCase(str)) {
            if (DBG) {
                Rlog.d(TAG, " gid1 " + groupIdLevel1 + " serviceGid1 " + str);
            }
            z = false;
        }
        if (DBG) {
            StringBuilder sb = new StringBuilder();
            sb.append("compareGid1 is ");
            sb.append(z ? "Same" : "Different");
            Rlog.d(TAG, sb.toString());
        }
        return z;
    }
}
