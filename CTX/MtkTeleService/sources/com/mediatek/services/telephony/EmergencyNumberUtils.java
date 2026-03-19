package com.mediatek.services.telephony;

import android.os.Build;
import android.os.SystemProperties;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import com.android.phone.settings.SettingsConstants;
import com.android.services.telephony.Log;
import com.mediatek.internal.telephony.MtkPhoneNumberUtils;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

public class EmergencyNumberUtils {
    private static String sOp;
    private boolean mIsCdmaAlwaysNumber;
    private boolean mIsCdmaPreferredNumber;
    private boolean mIsGsmAlwaysNumber;
    private boolean mIsGsmOnlyNumber;
    private boolean mIsGsmPreferredNumber;
    private String mNumber;
    private static final boolean DBG = "eng".equals(Build.TYPE);
    private static final boolean MTK_C2K_SUPPORT = SettingsConstants.DUA_VAL_ON.equals(SystemProperties.get("ro.boot.opt_c2k_support"));
    private static final String[] PLMN_NO_C2K = {"206", "231", "244", "44010"};
    private static HashMap<String, String> sGsmOnlyEccMap = new HashMap<>();
    private static HashMap<String, String> sGsmPreferredEccMap = new HashMap<>();
    private static HashMap<String, String> sCdmaPreferredEccMap = new HashMap<>();

    static {
        parseEccListPreference();
        sOp = SystemProperties.get("persist.vendor.operator.optr", "OM");
    }

    public EmergencyNumberUtils(String str) {
        this.mNumber = str;
        log("Number:" + str + ", operator:" + sOp);
        this.mIsGsmAlwaysNumber = isGsmAlwaysNumber(this.mNumber);
        this.mIsCdmaAlwaysNumber = isCdmaAlwaysNumber(this.mNumber);
        this.mIsGsmOnlyNumber = isGsmOnlyNumber(this.mNumber);
        this.mIsGsmPreferredNumber = isGsmPreferredNumber(this.mNumber);
        this.mIsCdmaPreferredNumber = isCdmaPreferredNumber(this.mNumber);
    }

    private static void parseEccListPreference() {
        sGsmOnlyEccMap.clear();
        sGsmPreferredEccMap.clear();
        sCdmaPreferredEccMap.clear();
        try {
            XmlPullParser xmlPullParserNewPullParser = XmlPullParserFactory.newInstance().newPullParser();
            if (xmlPullParserNewPullParser == null) {
                return;
            }
            FileReader fileReader = new FileReader("/system/vendor/etc/ecc_list_preference.xml");
            xmlPullParserNewPullParser.setInput(fileReader);
            for (int eventType = xmlPullParserNewPullParser.getEventType(); eventType != 1; eventType = xmlPullParserNewPullParser.next()) {
                if (eventType == 2) {
                    String name = xmlPullParserNewPullParser.getName();
                    int attributeCount = xmlPullParserNewPullParser.getAttributeCount();
                    String str = null;
                    String str2 = null;
                    for (int i = 0; i < attributeCount; i++) {
                        String attributeName = xmlPullParserNewPullParser.getAttributeName(i);
                        String attributeValue = xmlPullParserNewPullParser.getAttributeValue(i);
                        if (attributeName.equals("Operator")) {
                            str = attributeValue;
                        } else if (attributeName.equals("EccList")) {
                            str2 = attributeValue;
                        }
                    }
                    if (str != null && str2 != null) {
                        if (name.equals("GsmOnly")) {
                            sGsmOnlyEccMap.put(str, str2);
                        } else if (name.equals("GsmPref")) {
                            sGsmPreferredEccMap.put(str, str2);
                        } else if (name.equals("CdmaPref")) {
                            sCdmaPreferredEccMap.put(str, str2);
                        }
                    }
                }
            }
            fileReader.close();
        } catch (FileNotFoundException e) {
            log("Ecc List Preference file not found");
            sGsmOnlyEccMap.put("OM", "112,000,08,118");
            sGsmOnlyEccMap.put("OP01", "112");
            sGsmPreferredEccMap.put("OM", "911,999");
            sGsmPreferredEccMap.put("OP01", "000,08,118,911,999");
            sGsmPreferredEccMap.put("OP20", "999");
            sCdmaPreferredEccMap.put("OM", "110,119,120,122");
            sCdmaPreferredEccMap.put("OP20", "110,119,120,122,911");
        } catch (IOException e2) {
            e2.printStackTrace();
        } catch (XmlPullParserException e3) {
            e3.printStackTrace();
        } catch (Exception e4) {
            e4.printStackTrace();
        }
    }

    private static boolean isNumberMatched(String str, String[] strArr) {
        for (String str2 : strArr) {
            if (str.equals(str2)) {
                return true;
            }
        }
        return false;
    }

    public static boolean isGsmAlwaysNumber(String str) {
        boolean z = MtkPhoneNumberUtils.isEmergencyNumberExt(str, 1) && !MtkPhoneNumberUtils.isEmergencyNumberExt(str, 2);
        log("isGsmAlwaysNumber = " + z);
        return z;
    }

    public static boolean isCdmaAlwaysNumber(String str) {
        if (!isNetworkSupportCdma()) {
            return false;
        }
        boolean z = true;
        if (MtkPhoneNumberUtils.isEmergencyNumberExt(str, 1) || !MtkPhoneNumberUtils.isEmergencyNumberExt(str, 2)) {
            z = false;
        }
        log("isCdmaAlwaysNumber = " + z);
        return z;
    }

    public static boolean isGsmOnlyNumber(String str) {
        String str2;
        if (sGsmOnlyEccMap.containsKey(sOp)) {
            str2 = sGsmOnlyEccMap.get(sOp);
        } else {
            str2 = sGsmOnlyEccMap.get("OM");
        }
        boolean zIsNumberMatched = TextUtils.isEmpty(str2) ? false : isNumberMatched(str, str2.split(","));
        log("isGsmOnlyNumber = " + zIsNumberMatched + ", eccList = " + str2);
        return zIsNumberMatched;
    }

    public static boolean isGsmPreferredNumber(String str) {
        String str2;
        if (sGsmPreferredEccMap.containsKey(sOp)) {
            str2 = sGsmPreferredEccMap.get(sOp);
        } else {
            str2 = sGsmPreferredEccMap.get("OM");
        }
        boolean zIsNumberMatched = TextUtils.isEmpty(str2) ? false : isNumberMatched(str, str2.split(","));
        log("isGsmPreferredNumber = " + zIsNumberMatched + ", eccList = " + str2);
        return zIsNumberMatched;
    }

    public static boolean isCdmaPreferredNumber(String str) {
        String str2;
        if (!isNetworkSupportCdma()) {
            return false;
        }
        if (sCdmaPreferredEccMap.containsKey(sOp)) {
            str2 = sCdmaPreferredEccMap.get(sOp);
        } else {
            str2 = sCdmaPreferredEccMap.get("OM");
        }
        boolean zIsNumberMatched = TextUtils.isEmpty(str2) ? false : isNumberMatched(str, str2.split(","));
        log("isCdmaPreferredNumber = " + zIsNumberMatched + ", eccList = " + str2);
        return zIsNumberMatched;
    }

    public boolean isGsmAlwaysNumber() {
        return this.mIsGsmAlwaysNumber;
    }

    public boolean isCdmaAlwaysNumber() {
        return this.mIsCdmaAlwaysNumber;
    }

    public boolean isGsmOnlyNumber() {
        return this.mIsGsmOnlyNumber;
    }

    public boolean isGsmPreferredNumber() {
        return this.mIsGsmPreferredNumber;
    }

    public boolean isCdmaPreferredNumber() {
        return this.mIsCdmaPreferredNumber;
    }

    private static void log(String str) {
        if (DBG) {
            Log.i("ECCNumUtils", str, new Object[0]);
        }
    }

    private static boolean isNetworkSupportCdma() {
        if (!MTK_C2K_SUPPORT) {
            return false;
        }
        for (int i = 0; i < TelephonyManager.getDefault().getPhoneCount(); i++) {
            String networkOperatorForPhone = TelephonyManager.getDefault().getNetworkOperatorForPhone(i);
            if (!TextUtils.isEmpty(networkOperatorForPhone) && networkOperatorForPhone.length() >= 3) {
                for (int i2 = 0; i2 < PLMN_NO_C2K.length; i2++) {
                    if (networkOperatorForPhone.equals(PLMN_NO_C2K[i2]) || networkOperatorForPhone.substring(0, 3).equals(PLMN_NO_C2K[i2])) {
                        log("isNetworkSupportCdma() false, plmn = " + networkOperatorForPhone);
                        return false;
                    }
                }
            }
        }
        return true;
    }
}
