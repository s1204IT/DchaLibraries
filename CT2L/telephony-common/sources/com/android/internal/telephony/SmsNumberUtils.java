package com.android.internal.telephony;

import android.R;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.os.Build;
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

        public NumberEntry(String number) {
            this.number = number;
        }
    }

    private static String formatNumber(Context context, String number, String activeMcc, int networkType) {
        if (number == null) {
            throw new IllegalArgumentException("number is null");
        }
        if (activeMcc == null || activeMcc.trim().length() == 0) {
            throw new IllegalArgumentException("activeMcc is null or empty!");
        }
        String networkPortionNumber = PhoneNumberUtils.extractNetworkPortion(number);
        if (networkPortionNumber == null || networkPortionNumber.length() == 0) {
            throw new IllegalArgumentException("Number is invalid!");
        }
        NumberEntry numberEntry = new NumberEntry(networkPortionNumber);
        ArrayList<String> allIDDs = getAllIDDs(context, activeMcc);
        int nanpState = checkNANP(numberEntry, allIDDs);
        if (DBG) {
            Rlog.d(TAG, "NANP type: " + getNumberPlanType(nanpState));
        }
        if (nanpState != 1 && nanpState != 2 && nanpState != 3) {
            if (nanpState == 4) {
                if (networkType == 1 || networkType == 2) {
                    return networkPortionNumber.substring(1);
                }
                return networkPortionNumber;
            }
            if (nanpState == 5) {
                if (networkType != 1) {
                    if (networkType == 0) {
                        int iddLength = numberEntry.IDD != null ? numberEntry.IDD.length() : 0;
                        return PLUS_SIGN + networkPortionNumber.substring(iddLength);
                    }
                    if (networkType == 2) {
                        int iddLength2 = numberEntry.IDD != null ? numberEntry.IDD.length() : 0;
                        return networkPortionNumber.substring(iddLength2);
                    }
                } else {
                    return networkPortionNumber;
                }
            }
            int internationalState = checkInternationalNumberPlan(context, numberEntry, allIDDs, NANP_IDD);
            if (DBG) {
                Rlog.d(TAG, "International type: " + getNumberPlanType(internationalState));
            }
            String returnNumber = null;
            switch (internationalState) {
                case 100:
                    if (networkType == 0) {
                        returnNumber = networkPortionNumber.substring(1);
                    }
                    break;
                case NP_HOMEIDD_CC_AREA_LOCAL:
                    returnNumber = networkPortionNumber;
                    break;
                case NP_NBPCD_CC_AREA_LOCAL:
                    returnNumber = NANP_IDD + networkPortionNumber.substring(1);
                    break;
                case NP_LOCALIDD_CC_AREA_LOCAL:
                    if (networkType == 0 || networkType == 2) {
                        int iddLength3 = numberEntry.IDD != null ? numberEntry.IDD.length() : 0;
                        returnNumber = NANP_IDD + networkPortionNumber.substring(iddLength3);
                    }
                    break;
                case NP_CC_AREA_LOCAL:
                    int countryCode = numberEntry.countryCode;
                    if (!inExceptionListForNpCcAreaLocal(numberEntry) && networkPortionNumber.length() >= 11 && countryCode != 1) {
                        returnNumber = NANP_IDD + networkPortionNumber;
                    }
                    break;
                default:
                    if (networkPortionNumber.startsWith(PLUS_SIGN) && (networkType == 1 || networkType == 2)) {
                        returnNumber = networkPortionNumber.startsWith("+011") ? networkPortionNumber.substring(1) : NANP_IDD + networkPortionNumber.substring(1);
                    }
                    break;
            }
            if (returnNumber == null) {
                returnNumber = networkPortionNumber;
            }
            return returnNumber;
        }
        return networkPortionNumber;
    }

    private static ArrayList<String> getAllIDDs(Context context, String mcc) {
        ArrayList<String> allIDDs = IDDS_MAPS.get(mcc);
        if (allIDDs != null) {
            return allIDDs;
        }
        ArrayList<String> allIDDs2 = new ArrayList<>();
        String[] projection = {HbpcdLookup.MccIdd.IDD, "MCC"};
        String where = null;
        String[] selectionArgs = null;
        if (mcc != null) {
            where = "MCC=?";
            selectionArgs = new String[]{mcc};
        }
        Cursor cursor = null;
        try {
            try {
                cursor = context.getContentResolver().query(HbpcdLookup.MccIdd.CONTENT_URI, projection, where, selectionArgs, null);
                if (cursor.getCount() > 0) {
                    while (cursor.moveToNext()) {
                        String idd = cursor.getString(0);
                        if (!allIDDs2.contains(idd)) {
                            allIDDs2.add(idd);
                        }
                    }
                }
                if (cursor != null) {
                    cursor.close();
                }
            } catch (SQLException e) {
                Rlog.e(TAG, "Can't access HbpcdLookup database", e);
                if (cursor != null) {
                    cursor.close();
                }
            }
            IDDS_MAPS.put(mcc, allIDDs2);
            if (DBG) {
                Rlog.d(TAG, "MCC = " + mcc + ", all IDDs = " + allIDDs2);
            }
            return allIDDs2;
        } catch (Throwable th) {
            if (cursor != null) {
                cursor.close();
            }
            throw th;
        }
    }

    private static int checkNANP(NumberEntry numberEntry, ArrayList<String> allIDDs) {
        String number2;
        boolean isNANP = false;
        String number = numberEntry.number;
        if (number.length() == 7) {
            char firstChar = number.charAt(0);
            if (firstChar >= '2' && firstChar <= '9') {
                isNANP = true;
                int i = 1;
                while (true) {
                    if (i >= 7) {
                        break;
                    }
                    char c = number.charAt(i);
                    if (PhoneNumberUtils.isISODigit(c)) {
                        i++;
                    } else {
                        isNANP = false;
                        break;
                    }
                }
            }
            if (isNANP) {
                return 1;
            }
        } else if (number.length() == 10) {
            if (isNANP(number)) {
                return 2;
            }
        } else if (number.length() == 11) {
            if (isNANP(number)) {
                return 3;
            }
        } else if (number.startsWith(PLUS_SIGN)) {
            String number3 = number.substring(1);
            if (number3.length() == 11) {
                if (isNANP(number3)) {
                    return 4;
                }
            } else if (number3.startsWith(NANP_IDD) && number3.length() == 14 && isNANP(number3.substring(3))) {
                return 6;
            }
        } else {
            for (String idd : allIDDs) {
                if (number.startsWith(idd) && (number2 = number.substring(idd.length())) != null && number2.startsWith(String.valueOf(1)) && isNANP(number2)) {
                    numberEntry.IDD = idd;
                    return 5;
                }
            }
        }
        return 0;
    }

    private static boolean isNANP(String number) {
        if (number.length() != 10 && (number.length() != 11 || !number.startsWith(NANP_NDD))) {
            return false;
        }
        if (number.length() == 11) {
            number = number.substring(1);
        }
        return PhoneNumberUtils.isNanp(number);
    }

    private static int checkInternationalNumberPlan(Context context, NumberEntry numberEntry, ArrayList<String> allIDDs, String homeIDD) {
        int countryCode;
        String number = numberEntry.number;
        if (number.startsWith(PLUS_SIGN)) {
            String numberNoNBPCD = number.substring(1);
            if (numberNoNBPCD.startsWith(homeIDD)) {
                String numberCountryAreaLocal = numberNoNBPCD.substring(homeIDD.length());
                int countryCode2 = getCountryCode(context, numberCountryAreaLocal);
                if (countryCode2 > 0) {
                    numberEntry.countryCode = countryCode2;
                    return 100;
                }
            } else {
                int countryCode3 = getCountryCode(context, numberNoNBPCD);
                if (countryCode3 > 0) {
                    numberEntry.countryCode = countryCode3;
                    return NP_NBPCD_CC_AREA_LOCAL;
                }
            }
        } else if (number.startsWith(homeIDD)) {
            String numberCountryAreaLocal2 = number.substring(homeIDD.length());
            int countryCode4 = getCountryCode(context, numberCountryAreaLocal2);
            if (countryCode4 > 0) {
                numberEntry.countryCode = countryCode4;
                return NP_HOMEIDD_CC_AREA_LOCAL;
            }
        } else {
            for (String exitCode : allIDDs) {
                if (number.startsWith(exitCode)) {
                    String numberNoIDD = number.substring(exitCode.length());
                    int countryCode5 = getCountryCode(context, numberNoIDD);
                    if (countryCode5 > 0) {
                        numberEntry.countryCode = countryCode5;
                        numberEntry.IDD = exitCode;
                        return NP_LOCALIDD_CC_AREA_LOCAL;
                    }
                }
            }
            if (!number.startsWith("0") && (countryCode = getCountryCode(context, number)) > 0) {
                numberEntry.countryCode = countryCode;
                return NP_CC_AREA_LOCAL;
            }
        }
        return 0;
    }

    private static int getCountryCode(Context context, String number) {
        int[] allCCs;
        if (number.length() < 10 || (allCCs = getAllCountryCodes(context)) == null) {
            return -1;
        }
        int[] ccArray = new int[MAX_COUNTRY_CODES_LENGTH];
        for (int i = 0; i < MAX_COUNTRY_CODES_LENGTH; i++) {
            ccArray[i] = Integer.valueOf(number.substring(0, i + 1)).intValue();
        }
        for (int tempCC : allCCs) {
            for (int j = 0; j < MAX_COUNTRY_CODES_LENGTH; j++) {
                if (tempCC == ccArray[j]) {
                    if (DBG) {
                        Rlog.d(TAG, "Country code = " + tempCC);
                    }
                    return tempCC;
                }
            }
        }
        return -1;
    }

    private static int[] getAllCountryCodes(Context context) {
        if (ALL_COUNTRY_CODES != null) {
            return ALL_COUNTRY_CODES;
        }
        Cursor cursor = null;
        try {
            try {
                String[] projection = {HbpcdLookup.MccLookup.COUNTRY_CODE};
                cursor = context.getContentResolver().query(HbpcdLookup.MccLookup.CONTENT_URI, projection, null, null, null);
                if (cursor.getCount() > 0) {
                    ALL_COUNTRY_CODES = new int[cursor.getCount()];
                    int i = 0;
                    while (true) {
                        int i2 = i;
                        if (!cursor.moveToNext()) {
                            break;
                        }
                        int countryCode = cursor.getInt(0);
                        i = i2 + 1;
                        ALL_COUNTRY_CODES[i2] = countryCode;
                        int length = String.valueOf(countryCode).trim().length();
                        if (length > MAX_COUNTRY_CODES_LENGTH) {
                            MAX_COUNTRY_CODES_LENGTH = length;
                        }
                    }
                }
            } catch (SQLException e) {
                Rlog.e(TAG, "Can't access HbpcdLookup database", e);
                if (cursor != null) {
                    cursor.close();
                }
            }
            return ALL_COUNTRY_CODES;
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    private static boolean inExceptionListForNpCcAreaLocal(NumberEntry numberEntry) {
        int countryCode = numberEntry.countryCode;
        return numberEntry.number.length() == 12 && (countryCode == 7 || countryCode == 20 || countryCode == 65 || countryCode == 90);
    }

    private static String getNumberPlanType(int state) {
        String str = "Number Plan type (" + state + "): ";
        if (state == 1) {
            return "NP_NANP_LOCAL";
        }
        if (state == 2) {
            return "NP_NANP_AREA_LOCAL";
        }
        if (state == 3) {
            return "NP_NANP_NDD_AREA_LOCAL";
        }
        if (state == 4) {
            return "NP_NANP_NBPCD_CC_AREA_LOCAL";
        }
        if (state == 5) {
            return "NP_NANP_LOCALIDD_CC_AREA_LOCAL";
        }
        if (state == 6) {
            return "NP_NANP_NBPCD_HOMEIDD_CC_AREA_LOCAL";
        }
        if (state == 100) {
            return "NP_NBPCD_IDD_CC_AREA_LOCAL";
        }
        if (state == NP_HOMEIDD_CC_AREA_LOCAL) {
            return "NP_IDD_CC_AREA_LOCAL";
        }
        if (state == NP_NBPCD_CC_AREA_LOCAL) {
            return "NP_NBPCD_CC_AREA_LOCAL";
        }
        if (state == NP_LOCALIDD_CC_AREA_LOCAL) {
            return "NP_IDD_CC_AREA_LOCAL";
        }
        if (state == NP_CC_AREA_LOCAL) {
            return "NP_CC_AREA_LOCAL";
        }
        return "Unknown type";
    }

    public static String filterDestAddr(PhoneBase phoneBase, String destAddr) {
        int networkType;
        String networkMcc;
        if (DBG) {
            Rlog.d(TAG, "enter filterDestAddr. destAddr=\"" + destAddr + "\"");
        }
        if (destAddr == null || !PhoneNumberUtils.isGlobalPhoneNumber(destAddr)) {
            Rlog.w(TAG, "destAddr" + destAddr + " is not a global phone number!");
            return destAddr;
        }
        String networkOperator = TelephonyManager.getDefault().getNetworkOperator();
        String result = null;
        if (needToConvert(phoneBase) && (networkType = getNetworkType(phoneBase)) != -1 && !TextUtils.isEmpty(networkOperator) && (networkMcc = networkOperator.substring(0, 3)) != null && networkMcc.trim().length() > 0) {
            result = formatNumber(phoneBase.getContext(), destAddr, networkMcc, networkType);
        }
        if (DBG) {
            Rlog.d(TAG, "leave filterDestAddr, new destAddr=\"" + result + "\"");
        }
        return result == null ? destAddr : result;
    }

    private static int getNetworkType(PhoneBase phoneBase) {
        int phoneType = TelephonyManager.getDefault().getPhoneType();
        if (phoneType == 1) {
            return 0;
        }
        if (phoneType == 2) {
            if (isInternationalRoaming(phoneBase)) {
                return 2;
            }
            return 1;
        }
        if (DBG) {
            Rlog.w(TAG, "warning! unknown mPhoneType value=" + phoneType);
            return -1;
        }
        return -1;
    }

    private static boolean isInternationalRoaming(PhoneBase phoneBase) {
        String operatorIsoCountry = TelephonyManager.getDefault().getNetworkCountryIsoForPhone(phoneBase.getPhoneId());
        String simIsoCountry = TelephonyManager.getDefault().getSimCountryIsoForPhone(phoneBase.getPhoneId());
        boolean internationalRoaming = (TextUtils.isEmpty(operatorIsoCountry) || TextUtils.isEmpty(simIsoCountry) || simIsoCountry.equals(operatorIsoCountry)) ? false : true;
        if (internationalRoaming) {
            if ("us".equals(simIsoCountry)) {
                return !"vi".equals(operatorIsoCountry);
            }
            if ("vi".equals(simIsoCountry)) {
                return !"us".equals(operatorIsoCountry);
            }
            return internationalRoaming;
        }
        return internationalRoaming;
    }

    private static boolean needToConvert(PhoneBase phoneBase) {
        String[] needToConvertArray;
        boolean bNeedToConvert = false;
        String[] listArray = phoneBase.getContext().getResources().getStringArray(R.array.config_defaultImperceptibleKillingExemptionProcStates);
        if (listArray == null || listArray.length <= 0) {
            return false;
        }
        for (int i = 0; i < listArray.length; i++) {
            if (!TextUtils.isEmpty(listArray[i]) && (needToConvertArray = listArray[i].split(";")) != null && needToConvertArray.length > 0) {
                if (needToConvertArray.length == 1) {
                    bNeedToConvert = "true".equalsIgnoreCase(needToConvertArray[0]);
                } else if (needToConvertArray.length == 2 && !TextUtils.isEmpty(needToConvertArray[1]) && compareGid1(phoneBase, needToConvertArray[1])) {
                    boolean bNeedToConvert2 = "true".equalsIgnoreCase(needToConvertArray[0]);
                    return bNeedToConvert2;
                }
            }
        }
        return bNeedToConvert;
    }

    private static boolean compareGid1(PhoneBase phoneBase, String serviceGid1) {
        String gid1 = phoneBase.getGroupIdLevel1();
        boolean ret = true;
        if (TextUtils.isEmpty(serviceGid1)) {
            if (DBG) {
                Rlog.d(TAG, "compareGid1 serviceGid is empty, return true");
            }
            return true;
        }
        int gid_length = serviceGid1.length();
        if (gid1 == null || gid1.length() < gid_length || !gid1.substring(0, gid_length).equalsIgnoreCase(serviceGid1)) {
            if (DBG) {
                Rlog.d(TAG, " gid1 " + gid1 + " serviceGid1 " + serviceGid1);
            }
            ret = false;
        }
        if (DBG) {
            Rlog.d(TAG, "compareGid1 is " + (ret ? "Same" : "Different"));
        }
        return ret;
    }
}
