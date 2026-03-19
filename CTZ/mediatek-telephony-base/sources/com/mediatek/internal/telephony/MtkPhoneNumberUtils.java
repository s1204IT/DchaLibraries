package com.mediatek.internal.telephony;

import android.content.Context;
import android.location.Country;
import android.location.CountryDetector;
import android.os.SystemProperties;
import android.telephony.PhoneNumberUtils;
import android.telephony.Rlog;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import com.android.i18n.phonenumbers.ShortNumberInfo;
import com.mediatek.internal.telephony.cdma.pluscode.IPlusCodeUtils;
import com.mediatek.internal.telephony.cdma.pluscode.PlusCodeProcessor;
import com.mediatek.telephony.MtkTelephonyManagerEx;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

public class MtkPhoneNumberUtils {
    private static final String CLIR_OFF = "#31#";
    private static final String CLIR_ON = "*31#";
    public static final int FORMAT_JAPAN = 2;
    public static final int FORMAT_NANP = 1;
    public static final int FORMAT_UNKNOWN = 0;
    private static final String ICCID_CN_PREFIX = "8986";
    static final String LOG_TAG = "MtkPhoneNumberUtils";
    private static final int MAX_ECC_NUM_TO_MD_PER_AT = 10;
    private static final int MAX_ECC_NUM_TO_MD_TOTAL = 15;
    private static final int MAX_SIM_NUM = 4;
    private static final int MIN_MATCH = 7;
    private static final int MIN_MATCH_CTA = 11;
    public static final char PAUSE = ',';
    private static final char PLUS_SIGN_CHAR = '+';
    private static final String PLUS_SIGN_STRING = "+";
    public static final int TOA_International = 145;
    public static final int TOA_Unknown = 129;
    private static final boolean VDBG = false;
    public static final char WAIT = ';';
    public static final char WILD = 'N';
    private static int sSpecificEccCat = -1;
    private static final String[] SIM_RECORDS_PROPERTY_ECC_LIST = {"vendor.ril.ecclist", "vendor.ril.ecclist1", "vendor.ril.ecclist2", "vendor.ril.ecclist3"};
    private static final String[] CDMA_SIM_RECORDS_PROPERTY_ECC_LIST = {"vendor.ril.cdma.ecclist", "vendor.ril.cdma.ecclist1", "vendor.ril.cdma.ecclist2", "vendor.ril.cdma.ecclist3"};
    private static final String[] NETWORK_ECC_LIST = {"vendor.ril.ecc.service.category.list", "vendor.ril.ecc.service.category.list.1", "vendor.ril.ecc.service.category.list.2", "vendor.ril.ecc.service.category.list.3"};
    private static final String[] PROPERTY_RIL_FULL_UICC_TYPE = {"vendor.gsm.ril.fulluicctype", "vendor.gsm.ril.fulluicctype.2", "vendor.gsm.ril.fulluicctype.3", "vendor.gsm.ril.fulluicctype.4"};
    private static final String[] UICC_PLMN_PROPERTY = {"vendor.gsm.ril.uicc.mccmnc", "vendor.gsm.ril.uicc.mccmnc.1", "vendor.gsm.ril.uicc.mccmnc.2", "vendor.gsm.ril.uicc.mccmnc.3"};
    private static IPlusCodeUtils sPlusCodeUtils = null;
    private static boolean sIsCtaSupport = false;
    private static boolean sIsCtaSet = false;
    private static boolean sIsC2kSupport = false;
    private static boolean sIsOP09Support = false;
    private static EccSource sXmlEcc = null;
    private static EccSource sCtaEcc = null;
    private static EccSource sNetworkEcc = null;
    private static EccSource sSimEcc = null;
    private static EccSource sPropertyEcc = null;
    private static EccSource sTestEcc = null;
    private static ArrayList<EccSource> sAllEccSource = null;
    private static final String[] NANP_COUNTRIES = {"US", "CA", "AS", "AI", "AG", "BS", "BB", "BM", "VG", "KY", "DM", "DO", "GD", "GU", "JM", "PR", "MS", "MP", "KN", "LC", "VC", "TT", "TC", "VI"};
    private static final String[] COUNTRIES_NOT_USE_ECC_LIB = {"AU", "BD", "CO", "FR", "NG"};

    static {
        initialize();
    }

    public static byte[] numberToCalledPartyBCD(String str) {
        return PhoneNumberUtils.numberToCalledPartyBCD(str);
    }

    public static String calledPartyBCDFragmentToString(byte[] bArr, int i, int i2) {
        return PhoneNumberUtils.calledPartyBCDFragmentToString(bArr, i, i2);
    }

    public static String calledPartyBCDToString(byte[] bArr, int i, int i2) {
        return PhoneNumberUtils.calledPartyBCDToString(bArr, i, i2);
    }

    public static String stripSeparators(String str) {
        return PhoneNumberUtils.stripSeparators(str);
    }

    public static String extractNetworkPortion(String str) {
        return PhoneNumberUtils.extractNetworkPortion(str);
    }

    public static String stringFromStringAndTOA(String str, int i) {
        return PhoneNumberUtils.stringFromStringAndTOA(str, i);
    }

    public static String convertPreDial(String str) {
        return PhoneNumberUtils.convertPreDial(str);
    }

    public static boolean isNonSeparator(String str) {
        int length = str.length();
        for (int i = 0; i < length; i++) {
            if (!PhoneNumberUtils.isNonSeparator(str.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    public static int getFormatTypeFromCountryCode(String str) {
        int length = NANP_COUNTRIES.length;
        for (int i = 0; i < length; i++) {
            if (NANP_COUNTRIES[i].compareToIgnoreCase(str) == 0) {
                return 1;
            }
        }
        if ("jp".compareToIgnoreCase(str) != 0) {
            return 0;
        }
        return 2;
    }

    private static int findDialableIndexFromPostDialStr(String str) {
        for (int i = 0; i < str.length(); i++) {
            if (PhoneNumberUtils.isReallyDialable(str.charAt(i))) {
                return i;
            }
        }
        return -1;
    }

    private static String appendPwCharBackToOrigDialStr(int i, String str, String str2) {
        if (i == 1) {
            return str + str2.charAt(0);
        }
        return str.concat(str2.substring(0, i));
    }

    public static boolean isEmergencyNumber(String str) {
        return PhoneNumberUtils.isEmergencyNumber(str);
    }

    public static boolean isEmergencyNumber(int i, String str) {
        return PhoneNumberUtils.isEmergencyNumber(i, str);
    }

    public static class EccEntry {
        public static final String CATEGORY_ATTR = "Category";
        public static final String CDMA_ECC_LIST = "cdma_ecc_list.xml";
        public static final String CDMA_SS_ECC_LIST = "cdma_ecc_list_ss.xml";
        public static final String CONDITION_ATTR = "Condition";
        public static final String ECC_ALWAYS = "1";
        public static final String ECC_ATTR = "Ecc";
        public static final String ECC_ENTRY_TAG = "EccEntry";
        public static final String ECC_FOR_MMI = "2";
        public static final String ECC_LIST = "ecc_list.xml";
        public static final String ECC_LIST_PATH = "/system/vendor/etc/";
        public static final String ECC_NO_SIM = "0";
        public static final String PLMN_ATTR = "Plmn";
        public static final String PROPERTY_COUNT = "ro.vendor.semc.ecclist.num";
        public static final String PROPERTY_NON_ECC = "ro.vendor.semc.ecclist.non_ecc.";
        public static final String PROPERTY_NUMBER = "ro.vendor.semc.ecclist.number.";
        public static final String PROPERTY_PLMN = "ro.vendor.semc.ecclist.plmn.";
        public static final String PROPERTY_PREFIX = "ro.vendor.semc.ecclist.";
        public static final String PROPERTY_TYPE = "ro.vendor.semc.ecclist.type.";
        public static final String[] PROPERTY_TYPE_KEY = {"police", "ambulance", "firebrigade", "marineguard", "mountainrescue"};
        public static final Short[] PROPERTY_TYPE_VALUE = {(short) 1, (short) 2, (short) 4, (short) 8, (short) 16};
        private String mCategory;
        private String mCondition;
        private String mEcc;
        private String mName;
        private String mPlmn;

        public EccEntry() {
            this.mEcc = new String("");
            this.mCategory = new String("");
            this.mCondition = new String("");
            this.mPlmn = new String("");
        }

        public EccEntry(String str, String str2) {
            this.mName = str;
            this.mEcc = str2;
        }

        public void setName(String str) {
            this.mName = str;
        }

        public String getName() {
            return this.mName;
        }

        public void setEcc(String str) {
            this.mEcc = str;
        }

        public void setCategory(String str) {
            this.mCategory = str;
        }

        public void setCondition(String str) {
            this.mCondition = str;
        }

        public void setPlmn(String str) {
            this.mPlmn = str;
        }

        public String getEcc() {
            return this.mEcc;
        }

        public String getCategory() {
            return this.mCategory;
        }

        public String getCondition() {
            return this.mCondition;
        }

        public String getPlmn() {
            return this.mPlmn;
        }

        public String toString() {
            return "\nEcc=" + getEcc() + ", " + CATEGORY_ATTR + "=" + getCategory() + ", " + CONDITION_ATTR + "=" + getCondition() + ", " + PLMN_ATTR + "=" + getPlmn() + ", name=" + getName();
        }
    }

    private static class EccSource {
        private static final String PROP_VZW_DEVICE_TYPE = "persist.vendor.vzw_device_type";
        private int mPhoneType;
        protected ArrayList<EccEntry> mEccList = null;
        protected ArrayList<EccEntry> mCdmaEccList = null;

        public EccSource(int i) {
            this.mPhoneType = 0;
            this.mPhoneType = i;
            parseEccList();
        }

        public boolean isEmergencyNumber(String str, int i, int i2) {
            return false;
        }

        public boolean isMatch(String str, String str2) {
            String str3 = str + MtkPhoneNumberUtils.PLUS_SIGN_CHAR;
            if (!"3".equals(SystemProperties.get(PROP_VZW_DEVICE_TYPE, "0")) && !"4".equals(SystemProperties.get(PROP_VZW_DEVICE_TYPE, "0"))) {
                return str.equals(str2) || str3.equals(str2);
            }
            StringBuilder sb = new StringBuilder();
            sb.append("*272");
            sb.append(str);
            return str.equals(str2) || str3.equals(str2) || sb.toString().equals(str2);
        }

        public boolean isMatch(String str, String str2, String str3) {
            if (isMatch(str, str2) && isEccPlmnMatch(str3)) {
                return true;
            }
            return false;
        }

        public synchronized int getServiceCategory(String str, int i) {
            if (this.mEccList != null) {
                for (EccEntry eccEntry : this.mEccList) {
                    String ecc = eccEntry.getEcc();
                    if (isMatch(ecc, str)) {
                        MtkPhoneNumberUtils.log("[getServiceCategory] match customized, ECC: " + ecc + ", Category= " + eccEntry.getCategory());
                        return Integer.parseInt(eccEntry.getCategory());
                    }
                }
            }
            return -1;
        }

        public synchronized void addToEccList(ArrayList<EccEntry> arrayList) {
            if (this.mEccList != null && arrayList != null) {
                for (EccEntry eccEntry : this.mEccList) {
                    if (TextUtils.isEmpty(eccEntry.getPlmn())) {
                        boolean z = false;
                        Iterator<EccEntry> it = arrayList.iterator();
                        while (true) {
                            if (!it.hasNext()) {
                                break;
                            }
                            if (eccEntry.getEcc().equals(it.next().getEcc())) {
                                z = true;
                                break;
                            }
                        }
                        if (!z) {
                            arrayList.add(eccEntry);
                        }
                    }
                }
            }
        }

        public synchronized void parseEccList() {
        }

        public synchronized boolean isSpecialEmergencyNumber(String str) {
            return isSpecialEmergencyNumber(Integer.MAX_VALUE, str);
        }

        public synchronized boolean isSpecialEmergencyNumber(int i, String str) {
            if (this.mEccList != null) {
                boolean zEquals = SystemProperties.get("ro.vendor.mtk_ril_mode").equals("c6m_1rild");
                boolean z = TelephonyManager.getDefault().getCurrentPhoneType(i) == 1;
                boolean zIsSimInsert = isSimInsert(i, 1);
                boolean zIsCt4GDualModeCard = isCt4GDualModeCard(i);
                boolean z2 = !(zEquals || !z || zIsCt4GDualModeCard) || (z && zIsSimInsert && !zIsCt4GDualModeCard && isSimReady(i));
                MtkPhoneNumberUtils.dlog("[isSpecialEmergencyNumber] subId: " + i + ", number: " + str + ", eccApCtrl: " + zEquals + ", isGsmPhone: " + z + ", isGsmSimInserted: " + zIsSimInsert + ", isCt4G: " + zIsCt4GDualModeCard + ", isNeedCheckSpecial: " + z2);
                if (z2) {
                    for (EccEntry eccEntry : this.mEccList) {
                        if (eccEntry.getCondition().equals(EccEntry.ECC_FOR_MMI) && isMatch(eccEntry.getEcc(), str, eccEntry.getPlmn())) {
                            MtkPhoneNumberUtils.dlog("[isSpecialEmergencyNumber] match customized ecc");
                            return true;
                        }
                    }
                }
            }
            MtkPhoneNumberUtils.dlog("[isSpecialEmergencyNumber] return false number: " + str);
            return false;
        }

        public boolean isPhoneTypeSupport(int i) {
            return (i & this.mPhoneType) != 0;
        }

        public boolean isSimInsert(int i) {
            return isSimInsert(Integer.MAX_VALUE, i);
        }

        public boolean isSimInsert(int i, int i2) {
            String[] strArr = i2 == 2 ? MtkPhoneNumberUtils.CDMA_SIM_RECORDS_PROPERTY_ECC_LIST : MtkPhoneNumberUtils.SIM_RECORDS_PROPERTY_ECC_LIST;
            boolean zHasIccCard = true;
            if (Integer.MAX_VALUE == i) {
                int i3 = 0;
                while (true) {
                    if (i3 < 4) {
                        if (!TextUtils.isEmpty(SystemProperties.get(strArr[i3]))) {
                            break;
                        }
                        i3++;
                    } else {
                        zHasIccCard = false;
                        break;
                    }
                }
                if (i2 != 2) {
                    return zHasIccCard;
                }
                TelephonyManager telephonyManager = TelephonyManager.getDefault();
                int simCount = telephonyManager.getSimCount();
                int i4 = -1;
                int i5 = 0;
                while (true) {
                    if (i5 < simCount) {
                        int[] subId = SubscriptionManager.getSubId(i5);
                        if (subId != null && subId.length > 0) {
                            i4 = subId[0];
                        }
                        if (telephonyManager.getCurrentPhoneType(i4) == 2) {
                            break;
                        }
                        i5++;
                    } else {
                        i5 = -1;
                        i4 = -1;
                        break;
                    }
                }
                if (i4 != -1) {
                    zHasIccCard = telephonyManager.hasIccCard(i5);
                }
                MtkPhoneNumberUtils.vlog("[isSimInsert] CDMA subId:" + i4 + ", slotId:" + i5 + ", bSIMInserted:" + zHasIccCard);
                return zHasIccCard;
            }
            int slotIndex = SubscriptionManager.getSlotIndex(i);
            return SubscriptionManager.isValidSlotIndex(slotIndex) && !TextUtils.isEmpty(SystemProperties.get(strArr[slotIndex]));
        }

        public static boolean isEccPlmnMatch(String str) {
            if (TextUtils.isEmpty(str)) {
                return true;
            }
            for (int i = 0; i < TelephonyManager.getDefault().getPhoneCount(); i++) {
                String networkOperatorForPhone = TelephonyManager.getDefault().getNetworkOperatorForPhone(i);
                MtkPhoneNumberUtils.vlog("[isEccPlmnMatch] NW PLMN: " + networkOperatorForPhone);
                if (TextUtils.isEmpty(networkOperatorForPhone)) {
                    networkOperatorForPhone = TelephonyManager.getDefault().getSimOperatorNumericForPhone(i);
                    MtkPhoneNumberUtils.vlog("[isEccPlmnMatch] SIM PLMN: " + networkOperatorForPhone);
                    if (TextUtils.isEmpty(networkOperatorForPhone)) {
                        networkOperatorForPhone = SystemProperties.get(MtkPhoneNumberUtils.UICC_PLMN_PROPERTY[i]);
                        MtkPhoneNumberUtils.vlog("[isEccPlmnMatch] UICC PLMN: " + networkOperatorForPhone);
                    }
                    if (TextUtils.isEmpty(networkOperatorForPhone) || "N/A".equals(networkOperatorForPhone)) {
                        networkOperatorForPhone = MtkTelephonyManagerEx.getDefault().getLocatedPlmn(i);
                    }
                    if (TextUtils.isEmpty(networkOperatorForPhone)) {
                        continue;
                    } else {
                        String str2 = networkOperatorForPhone.substring(0, 3) + " " + networkOperatorForPhone.substring(3);
                        MtkPhoneNumberUtils.vlog("[isEccPlmnMatch] PLMN (" + i + "): " + str2 + ", strPlmn: " + str);
                        if (str2.equals(str) || (str.substring(4).compareToIgnoreCase("FFF") == 0 && str.substring(0, 3).equals(str2.substring(0, 3)))) {
                            MtkPhoneNumberUtils.vlog("[isEccPlmnMatch] PLMN matched strPlmn: " + str);
                            return true;
                        }
                    }
                }
            }
            return false;
        }

        private boolean isSimReady(int i) {
            int simState = TelephonyManager.getDefault().getSimState(SubscriptionManager.getSlotIndex(i));
            MtkPhoneNumberUtils.dlog("[isSimReady] subId: " + i + ", state: " + simState);
            return simState == 5;
        }

        private boolean isCt4GDualModeCard(int i) {
            int slotIndex = SubscriptionManager.getSlotIndex(i);
            if (SubscriptionManager.isValidSlotIndex(slotIndex)) {
                String str = SystemProperties.get(MtkPhoneNumberUtils.PROPERTY_RIL_FULL_UICC_TYPE[slotIndex]);
                if (!TextUtils.isEmpty(str) && str.indexOf("CSIM") >= 0 && str.indexOf("USIM") >= 0) {
                    return true;
                }
                return false;
            }
            return false;
        }
    }

    private static class XmlEccSource extends EccSource {
        public XmlEccSource(int i) {
            super(i);
        }

        @Override
        public synchronized void parseEccList() {
            String str = SystemProperties.get("persist.vendor.operator.optr");
            this.mEccList = new ArrayList<>();
            String str2 = "/system/vendor/etc/ecc_list.xml";
            if (!TextUtils.isEmpty(str)) {
                str2 = "/system/vendor/etc/ecc_list_" + str + ".xml";
                if (!new File(str2).exists()) {
                    MtkPhoneNumberUtils.log("[parseEccList] OP ECC file not exist, xmlPath: " + str2);
                    str2 = "/system/vendor/etc/ecc_list.xml";
                }
            }
            MtkPhoneNumberUtils.log("[parseEccList] Read ECC list from " + str2);
            parseFromXml(str2, this.mEccList);
            if (MtkPhoneNumberUtils.sIsC2kSupport) {
                this.mCdmaEccList = new ArrayList<>();
                String str3 = "/system/vendor/etc/cdma_ecc_list.xml";
                if ("ss".equals(SystemProperties.get("persist.vendor.radio.multisim.config")) && !"OP12".equals(str)) {
                    str3 = "/system/vendor/etc/cdma_ecc_list_ss.xml";
                } else if (!TextUtils.isEmpty(str)) {
                    str3 = "/system/vendor/etc/cdma_ecc_list_" + str + ".xml";
                    if (!new File(str2).exists()) {
                        str3 = "/system/vendor/etc/cdma_ecc_list.xml";
                    }
                }
                MtkPhoneNumberUtils.log("[parseEccList] Read CDMA ECC list from " + str3);
                parseFromXml(str3, this.mCdmaEccList);
            }
            MtkPhoneNumberUtils.dlog("[parseEccList] GSM XML ECC list: " + this.mEccList);
            MtkPhoneNumberUtils.dlog("[parseEccList] CDMA XML ECC list: " + this.mCdmaEccList);
        }

        @Override
        public synchronized boolean isEmergencyNumber(String str, int i, int i2) {
            try {
                ArrayList<EccEntry> arrayList = i2 == 2 ? this.mCdmaEccList : this.mEccList;
                MtkPhoneNumberUtils.vlog("[isEmergencyNumber] eccList: " + arrayList);
                if (isSimInsert(i2)) {
                    if (arrayList != null) {
                        for (EccEntry eccEntry : arrayList) {
                            if (!eccEntry.getCondition().equals("0") && isMatch(eccEntry.getEcc(), str, eccEntry.getPlmn())) {
                                MtkPhoneNumberUtils.log("[isEmergencyNumber] match XML ECC (w/ SIM) for phoneType: " + i2);
                                return true;
                            }
                        }
                    }
                } else if (arrayList != null) {
                    for (EccEntry eccEntry2 : arrayList) {
                        if (isMatch(eccEntry2.getEcc(), str, eccEntry2.getPlmn())) {
                            MtkPhoneNumberUtils.log("[isEmergencyNumber] match XML ECC (w/o SIM) for phoneType: " + i2);
                            return true;
                        }
                    }
                }
                MtkPhoneNumberUtils.vlog("[isEmergencyNumber] no match XML ECC for phoneType: " + i2);
                return false;
            } catch (Throwable th) {
                throw th;
            }
        }

        @Override
        public synchronized int getServiceCategory(String str, int i) {
            if (this.mEccList != null) {
                for (EccEntry eccEntry : this.mEccList) {
                    String ecc = eccEntry.getEcc();
                    if (isMatch(ecc, str, eccEntry.getPlmn())) {
                        MtkPhoneNumberUtils.log("[getServiceCategory] match xml customized, ECC: " + ecc + ", Category= " + eccEntry.getCategory() + ", plmn: " + eccEntry.getPlmn());
                        return Integer.parseInt(eccEntry.getCategory());
                    }
                }
            }
            return -1;
        }

        private synchronized void parseFromXml(String str, ArrayList<EccEntry> arrayList) {
            XmlPullParser xmlPullParserNewPullParser;
            try {
                xmlPullParserNewPullParser = XmlPullParserFactory.newInstance().newPullParser();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (XmlPullParserException e2) {
                e2.printStackTrace();
            }
            if (xmlPullParserNewPullParser == null) {
                MtkPhoneNumberUtils.log("[parseFromXml] XmlPullParserFactory.newPullParser() return null");
                return;
            }
            FileReader fileReader = new FileReader(str);
            xmlPullParserNewPullParser.setInput(fileReader);
            EccEntry eccEntry = null;
            for (int eventType = xmlPullParserNewPullParser.getEventType(); eventType != 1; eventType = xmlPullParserNewPullParser.next()) {
                switch (eventType) {
                    case 2:
                        if (xmlPullParserNewPullParser.getName().equals(EccEntry.ECC_ENTRY_TAG)) {
                            EccEntry eccEntry2 = new EccEntry();
                            int attributeCount = xmlPullParserNewPullParser.getAttributeCount();
                            for (int i = 0; i < attributeCount; i++) {
                                String attributeName = xmlPullParserNewPullParser.getAttributeName(i);
                                String attributeValue = xmlPullParserNewPullParser.getAttributeValue(i);
                                if (attributeName.equals(EccEntry.ECC_ATTR)) {
                                    eccEntry2.setEcc(attributeValue);
                                } else if (attributeName.equals(EccEntry.CATEGORY_ATTR)) {
                                    eccEntry2.setCategory(attributeValue);
                                } else if (attributeName.equals(EccEntry.CONDITION_ATTR)) {
                                    eccEntry2.setCondition(attributeValue);
                                } else if (attributeName.equals(EccEntry.PLMN_ATTR)) {
                                    eccEntry2.setPlmn(attributeValue);
                                }
                            }
                            eccEntry = eccEntry2;
                        }
                        break;
                    case 3:
                        if (xmlPullParserNewPullParser.getName().equals(EccEntry.ECC_ENTRY_TAG) && eccEntry != null) {
                            arrayList.add(eccEntry);
                        }
                        break;
                }
            }
            fileReader.close();
        }
    }

    private static class NetworkEccSource extends EccSource {
        public NetworkEccSource(int i) {
            super(i);
        }

        @Override
        public synchronized boolean isEmergencyNumber(String str, int i, int i2) {
            if (!isPhoneTypeSupport(i2)) {
                return false;
            }
            if (i == Integer.MAX_VALUE) {
                for (int i3 = 0; i3 < 4; i3++) {
                    String str2 = SystemProperties.get(MtkPhoneNumberUtils.NETWORK_ECC_LIST[i3]);
                    if (!TextUtils.isEmpty(str2)) {
                        MtkPhoneNumberUtils.dlog("[isEmergencyNumber] network list [" + i3 + "]:" + str2);
                        String[] strArrSplit = str2.split(";");
                        int length = strArrSplit.length;
                        for (int i4 = 0; i4 < length; i4++) {
                            String str3 = strArrSplit[i4];
                            if (!str3.isEmpty()) {
                                String[] strArrSplit2 = str3.split(",");
                                if (2 == strArrSplit2.length && isMatch(strArrSplit2[0], str)) {
                                    MtkPhoneNumberUtils.log("[isEmergencyNumber] match network ECC for phoneType: " + i2);
                                    return true;
                                }
                            }
                        }
                    }
                }
            } else {
                int slotIndex = SubscriptionManager.getSlotIndex(i);
                if (SubscriptionManager.isValidSlotIndex(slotIndex)) {
                    String str4 = SystemProperties.get(MtkPhoneNumberUtils.NETWORK_ECC_LIST[slotIndex]);
                    if (!TextUtils.isEmpty(str4)) {
                        MtkPhoneNumberUtils.dlog("[isEmergencyNumber]ril.ecc.service.category.list[" + slotIndex + "]" + str4);
                        String[] strArrSplit3 = str4.split(";");
                        int length2 = strArrSplit3.length;
                        for (int i5 = 0; i5 < length2; i5++) {
                            String str5 = strArrSplit3[i5];
                            if (!str5.isEmpty()) {
                                String[] strArrSplit4 = str5.split(",");
                                if (2 == strArrSplit4.length && isMatch(strArrSplit4[0], str)) {
                                    MtkPhoneNumberUtils.log("[isEmergencyNumber] match network ECC for phoneType: " + i2);
                                    return true;
                                }
                            }
                        }
                    }
                }
            }
            return false;
        }

        @Override
        public synchronized int getServiceCategory(String str, int i) {
            if (i == Integer.MAX_VALUE) {
                for (int i2 = 0; i2 < 4; i2++) {
                    String str2 = SystemProperties.get(MtkPhoneNumberUtils.NETWORK_ECC_LIST[i2]);
                    if (!TextUtils.isEmpty(str2)) {
                        MtkPhoneNumberUtils.log("[getServiceCategory] Network ECC List: " + str2);
                        String[] strArrSplit = str2.split(";");
                        int length = strArrSplit.length;
                        for (int i3 = 0; i3 < length; i3++) {
                            String str3 = strArrSplit[i3];
                            if (!str3.isEmpty()) {
                                String[] strArrSplit2 = str3.split(",");
                                if (2 == strArrSplit2.length && isMatch(strArrSplit2[0], str)) {
                                    MtkPhoneNumberUtils.log("[getServiceCategory] match network, Ecc= " + str + ", Category= " + Integer.parseInt(strArrSplit2[1]));
                                    return Integer.parseInt(strArrSplit2[1]);
                                }
                            }
                        }
                    }
                }
            } else {
                int slotIndex = SubscriptionManager.getSlotIndex(i);
                if (SubscriptionManager.isValidSlotIndex(slotIndex)) {
                    String str4 = SystemProperties.get(MtkPhoneNumberUtils.NETWORK_ECC_LIST[slotIndex]);
                    if (!TextUtils.isEmpty(str4)) {
                        MtkPhoneNumberUtils.log("[getServiceCategory] Network ECC List: " + str4);
                        String[] strArrSplit3 = str4.split(";");
                        int length2 = strArrSplit3.length;
                        for (int i4 = 0; i4 < length2; i4++) {
                            String str5 = strArrSplit3[i4];
                            if (!str5.isEmpty()) {
                                String[] strArrSplit4 = str5.split(",");
                                if (2 == strArrSplit4.length && isMatch(strArrSplit4[0], str)) {
                                    MtkPhoneNumberUtils.log("[getServiceCategory] match network, Ecc= " + str + ", Category= " + Integer.parseInt(strArrSplit4[1]));
                                    return Integer.parseInt(strArrSplit4[1]);
                                }
                            }
                        }
                    }
                }
            }
            return -1;
        }
    }

    private static class SimEccSource extends EccSource {
        public SimEccSource(int i) {
            super(i);
        }

        @Override
        public synchronized boolean isEmergencyNumber(String str, int i, int i2) {
            if (i2 == 2) {
                for (int i3 = 0; i3 < 4; i3++) {
                    String str2 = SystemProperties.get(MtkPhoneNumberUtils.CDMA_SIM_RECORDS_PROPERTY_ECC_LIST[i3]);
                    if (!TextUtils.isEmpty(str2)) {
                        for (String str3 : str2.split(",")) {
                            if (isMatch(str3, str)) {
                                MtkPhoneNumberUtils.log("[isEmergencyNumber] match CDMA SIM ECC for phoneType: " + i2);
                                return true;
                            }
                        }
                    }
                }
            } else {
                for (int i4 = 0; i4 < 4; i4++) {
                    String str4 = SystemProperties.get(MtkPhoneNumberUtils.SIM_RECORDS_PROPERTY_ECC_LIST[i4]);
                    if (!TextUtils.isEmpty(str4)) {
                        for (String str5 : str4.split(";")) {
                            if (!str5.isEmpty()) {
                                String[] strArrSplit = str5.split(",");
                                if (2 == strArrSplit.length && isMatch(strArrSplit[0], str)) {
                                    MtkPhoneNumberUtils.log("[isEmergencyNumber] match GSM SIM ECC for phoneType: " + i2);
                                    return true;
                                }
                            }
                        }
                    }
                }
            }
            return false;
        }

        @Override
        public synchronized int getServiceCategory(String str, int i) {
            for (int i2 = 0; i2 < 4; i2++) {
                String str2 = SystemProperties.get(MtkPhoneNumberUtils.SIM_RECORDS_PROPERTY_ECC_LIST[i2]);
                if (!TextUtils.isEmpty(str2)) {
                    MtkPhoneNumberUtils.dlog("[getServiceCategory] list[" + i2 + "]: " + str2);
                    String[] strArrSplit = str2.split(";");
                    int length = strArrSplit.length;
                    for (int i3 = 0; i3 < length; i3++) {
                        String str3 = strArrSplit[i3];
                        if (!str3.isEmpty()) {
                            String[] strArrSplit2 = str3.split(",");
                            if (2 == strArrSplit2.length && isMatch(strArrSplit2[0], str)) {
                                return Integer.parseInt(strArrSplit2[1]);
                            }
                        }
                    }
                }
            }
            return -1;
        }
    }

    private static class CtaEccSource extends EccSource {
        private static String[] sCtaList = {"120", "122", "119", "110"};

        public CtaEccSource(int i) {
            super(i);
        }

        @Override
        public synchronized void parseEccList() {
            this.mEccList = new ArrayList<>();
            for (String str : sCtaList) {
                EccEntry eccEntry = new EccEntry();
                eccEntry.setEcc(str);
                eccEntry.setCategory("0");
                eccEntry.setCondition(EccEntry.ECC_FOR_MMI);
                this.mEccList.add(eccEntry);
            }
            MtkPhoneNumberUtils.dlog("[parseEccList] CTA ECC list: " + this.mEccList);
        }

        @Override
        public synchronized boolean isEmergencyNumber(String str, int i, int i2) {
            if (isPhoneTypeSupport(i2) && isNeedCheckCtaSet() && this.mEccList != null) {
                Iterator<EccEntry> it = this.mEccList.iterator();
                while (it.hasNext()) {
                    if (isMatch(it.next().getEcc(), str)) {
                        MtkPhoneNumberUtils.log("[isEmergencyNumber] match CTA ECC for phoneType: " + i2);
                        return true;
                    }
                }
            }
            return false;
        }

        @Override
        public synchronized void addToEccList(ArrayList<EccEntry> arrayList) {
            if (isNeedCheckCtaSet()) {
                super.addToEccList(arrayList);
            }
        }

        @Override
        public synchronized int getServiceCategory(String str, int i) {
            return -1;
        }

        private boolean isNeedCheckCtaSet() {
            if (!isSimInsert(1) && !isSimInsert(2)) {
                MtkPhoneNumberUtils.vlog("[isNeedCheckCtaSet] No SIM insert, return true: ");
                return true;
            }
            int simCount = TelephonyManager.getDefault().getSimCount();
            for (int i = 0; i < simCount; i++) {
                try {
                    String simSerialNumber = MtkTelephonyManagerEx.getDefault().getSimSerialNumber(i);
                    MtkPhoneNumberUtils.vlog("[isNeedCheckCtaSet] strIccid[" + i + "]: " + simSerialNumber);
                    if (!TextUtils.isEmpty(simSerialNumber) && simSerialNumber.startsWith(MtkPhoneNumberUtils.ICCID_CN_PREFIX)) {
                        return true;
                    }
                } catch (NullPointerException e) {
                    MtkPhoneNumberUtils.log("[isNeedCheckCtaSet] NullPointerException:" + e);
                    return true;
                } catch (Exception e2) {
                    MtkPhoneNumberUtils.log("[isNeedCheckCtaSet] Exception: " + e2);
                    return true;
                }
            }
            return false;
        }

        @Override
        public synchronized boolean isSpecialEmergencyNumber(int i, String str) {
            if (!isNeedCheckCtaSet()) {
                return false;
            }
            return super.isSpecialEmergencyNumber(i, str);
        }
    }

    private static class PropertyEccSource extends EccSource {
        public PropertyEccSource(int i) {
            super(i);
        }

        @Override
        public synchronized void parseEccList() {
            String str = SystemProperties.get(EccEntry.PROPERTY_COUNT);
            if (TextUtils.isEmpty(str)) {
                MtkPhoneNumberUtils.log("[parseEccList] empty property");
                return;
            }
            this.mEccList = new ArrayList<>();
            int i = Integer.parseInt(str);
            for (int i2 = 0; i2 < i; i2++) {
                String str2 = SystemProperties.get(EccEntry.PROPERTY_NUMBER + i2);
                if (!TextUtils.isEmpty(str2)) {
                    EccEntry eccEntry = new EccEntry();
                    eccEntry.setEcc(str2);
                    String str3 = SystemProperties.get(EccEntry.PROPERTY_TYPE + i2);
                    if (!TextUtils.isEmpty(str3)) {
                        String[] strArrSplit = str3.split(" ");
                        int length = strArrSplit.length;
                        int i3 = 0;
                        short s = 0;
                        while (i3 < length) {
                            String str4 = strArrSplit[i3];
                            short sShortValue = s;
                            for (int i4 = 0; i4 < EccEntry.PROPERTY_TYPE_KEY.length; i4++) {
                                if (str4.equals(EccEntry.PROPERTY_TYPE_KEY[i4])) {
                                    sShortValue = (short) (sShortValue | EccEntry.PROPERTY_TYPE_VALUE[i4].shortValue());
                                }
                            }
                            i3++;
                            s = sShortValue;
                        }
                        eccEntry.setCategory(Short.toString(s));
                    } else {
                        eccEntry.setCategory("0");
                    }
                    String str5 = SystemProperties.get(EccEntry.PROPERTY_NON_ECC + i2);
                    if (TextUtils.isEmpty(str5) || str5.equals("false")) {
                        eccEntry.setCondition(EccEntry.ECC_ALWAYS);
                    } else {
                        eccEntry.setCondition("0");
                    }
                    String str6 = SystemProperties.get(EccEntry.PROPERTY_PLMN + i2);
                    if (!TextUtils.isEmpty(str6)) {
                        eccEntry.setPlmn(str6);
                    }
                    this.mEccList.add(eccEntry);
                }
            }
            MtkPhoneNumberUtils.dlog("[parseEccList] property ECC list: " + this.mEccList);
        }

        @Override
        public synchronized boolean isEmergencyNumber(String str, int i, int i2) {
            if (!isPhoneTypeSupport(i2)) {
                return false;
            }
            if (isSimInsert(i2)) {
                if (this.mEccList != null) {
                    for (EccEntry eccEntry : this.mEccList) {
                        if (!eccEntry.getCondition().equals("0") && isMatch(eccEntry.getEcc(), str) && isEccPlmnMatch(eccEntry.getPlmn())) {
                            MtkPhoneNumberUtils.log("[isEmergencyNumber] match property ECC(w/ SIM) for phoneType:" + i2);
                            return true;
                        }
                    }
                }
            } else if (this.mEccList != null) {
                Iterator<EccEntry> it = this.mEccList.iterator();
                while (it.hasNext()) {
                    if (isMatch(it.next().getEcc(), str)) {
                        MtkPhoneNumberUtils.log("[isEmergencyNumber] match property ECC(w/o SIM) for phoneType:" + i2);
                        return true;
                    }
                }
            }
            return false;
        }
    }

    private static class TestEccSource extends EccSource {
        private static final String TEST_ECC_LIST = "persist.vendor.radio.mtk.testecc";

        public TestEccSource(int i) {
            super(i);
        }

        @Override
        public synchronized boolean isEmergencyNumber(String str, int i, int i2) {
            String str2 = SystemProperties.get(TEST_ECC_LIST);
            if (TextUtils.isEmpty(str2)) {
                return false;
            }
            if (isSimInsert(1) || isSimInsert(2)) {
                MtkPhoneNumberUtils.dlog("[isEmergencyNumber] test ECC list: " + str2);
                String[] strArrSplit = str2.split(",");
                int length = strArrSplit.length;
                for (int i3 = 0; i3 < length; i3++) {
                    String str3 = strArrSplit[i3];
                    if (!str3.isEmpty() && isMatch(str3, str)) {
                        MtkPhoneNumberUtils.dlog("[isEmergencyNumber] match test ECC for phoneType: " + i2);
                        return true;
                    }
                }
            }
            return false;
        }
    }

    private static void initialize() {
        sIsCtaSupport = EccEntry.ECC_ALWAYS.equals(SystemProperties.get("ro.vendor.mtk_cta_support"));
        sIsCtaSet = EccEntry.ECC_ALWAYS.equals(SystemProperties.get("ro.vendor.mtk_cta_set"));
        boolean z = false;
        sIsC2kSupport = SystemProperties.get("ro.boot.opt_ps1_rat").indexOf(67) >= 0;
        if ("OP09".equals(SystemProperties.get("persist.vendor.operator.optr")) && ("SEGDEFAULT".equals(SystemProperties.get("persist.vendor.operator.seg")) || "SEGC".equals(SystemProperties.get("persist.vendor.operator.seg")))) {
            z = true;
        }
        sIsOP09Support = z;
        log("Init: sIsCtaSupport: " + sIsCtaSupport + ", sIsCtaSet: " + sIsCtaSet + ", sIsC2kSupport: " + sIsC2kSupport + ", sIsOP09Support: " + sIsOP09Support);
        sPlusCodeUtils = PlusCodeProcessor.getPlusCodeUtils();
        initEccSource();
    }

    private static void initEccSource() {
        sAllEccSource = new ArrayList<>();
        sNetworkEcc = new NetworkEccSource(1);
        sPropertyEcc = new PropertyEccSource(1);
        if (sIsC2kSupport) {
            sXmlEcc = new XmlEccSource(3);
            sSimEcc = new SimEccSource(3);
            sTestEcc = new TestEccSource(3);
        } else {
            sXmlEcc = new XmlEccSource(1);
            sSimEcc = new SimEccSource(1);
            sTestEcc = new TestEccSource(1);
        }
        sAllEccSource.add(sNetworkEcc);
        sAllEccSource.add(sSimEcc);
        sAllEccSource.add(sXmlEcc);
        sAllEccSource.add(sPropertyEcc);
        sAllEccSource.add(sTestEcc);
        if (sIsCtaSet) {
            sCtaEcc = new CtaEccSource(1);
            sAllEccSource.add(sCtaEcc);
        }
    }

    public static String cdmaCheckAndProcessPlusCode(String str) {
        String strPreProcessPlusCode = preProcessPlusCode(str);
        if (strPreProcessPlusCode != null && !strPreProcessPlusCode.equals(str)) {
            return strPreProcessPlusCode;
        }
        if (!TextUtils.isEmpty(str) && PhoneNumberUtils.isReallyDialable(str.charAt(0)) && isNonSeparator(str)) {
            String networkCountryIso = TelephonyManager.getDefault().getNetworkCountryIso();
            String simCountryIso = TelephonyManager.getDefault().getSimCountryIso();
            if (!TextUtils.isEmpty(networkCountryIso) && !TextUtils.isEmpty(simCountryIso)) {
                return PhoneNumberUtils.cdmaCheckAndProcessPlusCodeByNumberFormat(str, getFormatTypeFromCountryCode(networkCountryIso), getFormatTypeFromCountryCode(simCountryIso));
            }
        }
        return str;
    }

    public static String cdmaCheckAndProcessPlusCodeForSms(String str) {
        String strPreProcessPlusCodeForSms = preProcessPlusCodeForSms(str);
        if (strPreProcessPlusCodeForSms != null && !strPreProcessPlusCodeForSms.equals(str)) {
            return strPreProcessPlusCodeForSms;
        }
        if (!TextUtils.isEmpty(str) && PhoneNumberUtils.isReallyDialable(str.charAt(0)) && isNonSeparator(str)) {
            String simCountryIso = TelephonyManager.getDefault().getSimCountryIso();
            if (!TextUtils.isEmpty(simCountryIso)) {
                int formatTypeFromCountryCode = getFormatTypeFromCountryCode(simCountryIso);
                return PhoneNumberUtils.cdmaCheckAndProcessPlusCodeByNumberFormat(str, formatTypeFromCountryCode, formatTypeFromCountryCode);
            }
        }
        return str;
    }

    public static String extractCLIRPortion(String str) {
        String strGroup;
        if (str == null) {
            return null;
        }
        Matcher matcher = Pattern.compile("^([*][#]|[*]{1,2}|[#]{1,2})([0-9]{2,3})([*])([+]?[0-9]+)(.*)(#)$").matcher(str);
        if (matcher.matches()) {
            return matcher.group(4);
        }
        if (str.startsWith(CLIR_ON) || str.startsWith(CLIR_OFF)) {
            dlog(str + " Start with *31# or #31#, return " + str.substring(4));
            return str.substring(4);
        }
        if (str.indexOf(PLUS_SIGN_STRING) != -1 && str.indexOf(PLUS_SIGN_STRING) == str.lastIndexOf(PLUS_SIGN_STRING)) {
            Matcher matcher2 = Pattern.compile("(^[#*])(.*)([#*])(.*)(#)$").matcher(str);
            if (matcher2.matches()) {
                if ("".equals(matcher2.group(2))) {
                    dlog(str + " matcher pattern1, return empty string.");
                    return "";
                }
                String strGroup2 = matcher2.group(4);
                if (strGroup2 != null && strGroup2.length() > 1 && strGroup2.charAt(0) == '+') {
                    dlog(str + " matcher pattern1, return " + strGroup2);
                    return strGroup2;
                }
            } else {
                Matcher matcher3 = Pattern.compile("(^[#*])(.*)([#*])(.*)").matcher(str);
                if (matcher3.matches() && (strGroup = matcher3.group(4)) != null && strGroup.length() > 1 && strGroup.charAt(0) == '+') {
                    dlog(str + " matcher pattern2, return " + strGroup);
                    return strGroup;
                }
            }
        }
        return str;
    }

    public static String prependPlusToNumber(String str) {
        StringBuilder sb;
        String string = str.toString();
        Matcher matcher = Pattern.compile("^([*][#]|[*]{1,2}|[#]{1,2})([0-9]{2,3})([*])([0-9]+)(.*)(#)$").matcher(string);
        if (matcher.matches()) {
            sb = new StringBuilder();
            sb.append(matcher.group(1));
            sb.append(matcher.group(2));
            sb.append(matcher.group(3));
            sb.append(PLUS_SIGN_STRING);
            sb.append(matcher.group(4));
            sb.append(matcher.group(5));
            sb.append(matcher.group(6));
        } else {
            Matcher matcher2 = Pattern.compile("(^[#*])(.*)([#*])(.*)(#)$").matcher(string);
            if (matcher2.matches()) {
                if ("".equals(matcher2.group(2))) {
                    sb = new StringBuilder();
                    sb.append(matcher2.group(1));
                    sb.append(matcher2.group(3));
                    sb.append(matcher2.group(4));
                    sb.append(matcher2.group(5));
                    sb.append(PLUS_SIGN_STRING);
                } else {
                    sb = new StringBuilder();
                    sb.append(matcher2.group(1));
                    sb.append(matcher2.group(2));
                    sb.append(matcher2.group(3));
                    sb.append(PLUS_SIGN_STRING);
                    sb.append(matcher2.group(4));
                    sb.append(matcher2.group(5));
                }
            } else {
                Matcher matcher3 = Pattern.compile("(^[#*])(.*)([#*])(.*)").matcher(string);
                if (matcher3.matches()) {
                    sb = new StringBuilder();
                    sb.append(matcher3.group(1));
                    sb.append(matcher3.group(2));
                    sb.append(matcher3.group(3));
                    sb.append(PLUS_SIGN_STRING);
                    sb.append(matcher3.group(4));
                } else {
                    StringBuilder sb2 = new StringBuilder();
                    sb2.append(PLUS_SIGN_CHAR);
                    sb2.append(string);
                    sb = sb2;
                }
            }
        }
        return sb.toString();
    }

    private static String preProcessPlusCode(String str) {
        String strExtractNetworkPortionAlt;
        int formatTypeFromCountryCode;
        if (!TextUtils.isEmpty(str) && PhoneNumberUtils.isReallyDialable(str.charAt(0)) && isNonSeparator(str)) {
            String networkCountryIso = TelephonyManager.getDefault().getNetworkCountryIso();
            String simCountryIso = TelephonyManager.getDefault().getSimCountryIso();
            if (TextUtils.isEmpty(networkCountryIso) || TextUtils.isEmpty(simCountryIso) || (formatTypeFromCountryCode = getFormatTypeFromCountryCode(networkCountryIso)) != getFormatTypeFromCountryCode(simCountryIso) || formatTypeFromCountryCode != 1) {
                dlog("preProcessPlusCode, before format number:" + str);
                if (str.lastIndexOf(PLUS_SIGN_STRING) != -1) {
                    String str2 = null;
                    String strSubstring = str;
                    while (true) {
                        strExtractNetworkPortionAlt = PhoneNumberUtils.extractNetworkPortionAlt(strSubstring);
                        if (strExtractNetworkPortionAlt != null && strExtractNetworkPortionAlt.charAt(0) == '+' && strExtractNetworkPortionAlt.length() > 1) {
                            if (sPlusCodeUtils.canFormatPlusToIddNdd()) {
                                strExtractNetworkPortionAlt = sPlusCodeUtils.replacePlusCodeWithIddNdd(strExtractNetworkPortionAlt);
                            } else {
                                dlog("preProcessPlusCode, can't format plus code.");
                                return str;
                            }
                        }
                        dlog("preProcessPlusCode, networkDialStr:" + strExtractNetworkPortionAlt);
                        if (!TextUtils.isEmpty(strExtractNetworkPortionAlt)) {
                            if (str2 != null) {
                                strExtractNetworkPortionAlt = str2.concat(strExtractNetworkPortionAlt);
                            }
                            String strExtractPostDialPortion = PhoneNumberUtils.extractPostDialPortion(strSubstring);
                            if (!TextUtils.isEmpty(strExtractPostDialPortion)) {
                                int iFindDialableIndexFromPostDialStr = findDialableIndexFromPostDialStr(strExtractPostDialPortion);
                                if (iFindDialableIndexFromPostDialStr >= 1) {
                                    strExtractNetworkPortionAlt = appendPwCharBackToOrigDialStr(iFindDialableIndexFromPostDialStr, strExtractNetworkPortionAlt, strExtractPostDialPortion);
                                    strSubstring = strExtractPostDialPortion.substring(iFindDialableIndexFromPostDialStr);
                                } else {
                                    if (iFindDialableIndexFromPostDialStr < 0) {
                                        strExtractPostDialPortion = "";
                                    }
                                    Rlog.e(LOG_TAG, "preProcessPlusCode, wrong postDialStr:" + strExtractPostDialPortion);
                                }
                            }
                            dlog("preProcessPlusCode, postDialStr:" + strExtractPostDialPortion + ", tempDialStr:" + strSubstring);
                            if (TextUtils.isEmpty(strExtractPostDialPortion) || TextUtils.isEmpty(strSubstring)) {
                                break;
                            }
                            str2 = strExtractNetworkPortionAlt;
                        } else {
                            Rlog.e(LOG_TAG, "preProcessPlusCode, null newDialStr:" + strExtractNetworkPortionAlt);
                            return str;
                        }
                    }
                    str = strExtractNetworkPortionAlt;
                }
                dlog("preProcessPlusCode, after format number:" + str);
            } else {
                dlog("preProcessPlusCode, no need format, currIso:" + networkCountryIso + ", defaultIso:" + simCountryIso);
            }
        }
        return str;
    }

    private static String preProcessPlusCodeForSms(String str) {
        dlog("preProcessPlusCodeForSms ENTER.");
        if (TextUtils.isEmpty(str) || !str.startsWith(PLUS_SIGN_STRING) || !PhoneNumberUtils.isReallyDialable(str.charAt(0)) || !isNonSeparator(str) || getFormatTypeFromCountryCode(TelephonyManager.getDefault().getSimCountryIso()) == 1 || !sPlusCodeUtils.canFormatPlusCodeForSms()) {
            return str;
        }
        String strReplacePlusCodeForSms = sPlusCodeUtils.replacePlusCodeForSms(str);
        if (TextUtils.isEmpty(strReplacePlusCodeForSms)) {
            dlog("preProcessPlusCodeForSms, can't handle the plus code by PlusCodeUtils");
            return str;
        }
        dlog("preProcessPlusCodeForSms, new dialStr = " + strReplacePlusCodeForSms);
        return strReplacePlusCodeForSms;
    }

    public static boolean isLocalEmergencyNumberInternal(int i, String str, Context context, boolean z) {
        Country countryDetectCountry;
        String country;
        CountryDetector countryDetector = (CountryDetector) context.getSystemService("country_detector");
        if (countryDetector != null) {
            countryDetectCountry = countryDetector.detectCountry();
        } else {
            countryDetectCountry = null;
        }
        if (countryDetectCountry != null) {
            country = countryDetectCountry.getCountryIso();
        } else {
            country = context.getResources().getConfiguration().locale.getCountry();
            Rlog.w(LOG_TAG, "No CountryDetector; falling back to countryIso based on locale: " + country);
        }
        return isEmergencyNumberExt(i, str, country, z);
    }

    public static boolean isEmergencyNumberExt(int i, String str, String str2, boolean z) {
        boolean zConnectsToEmergencyNumber;
        boolean zIsEmergencyNumberExt;
        if (TextUtils.isEmpty(str) || PhoneNumberUtils.isUriNumber(str)) {
            return false;
        }
        String strExtractNetworkPortionAlt = PhoneNumberUtils.extractNetworkPortionAlt(str);
        dlog("[isEmergencyNumberExt] number: " + strExtractNetworkPortionAlt + ", subId: " + i + ", iso: " + str2 + ", useExactMatch: " + z);
        if (i == Integer.MAX_VALUE || i == -1) {
            int queryPhoneType = getQueryPhoneType(i);
            if ((queryPhoneType & 1) != 0 && isEmergencyNumberExt(strExtractNetworkPortionAlt, 1, i)) {
                return true;
            }
            if ((queryPhoneType & 2) != 0 && isEmergencyNumberExt(strExtractNetworkPortionAlt, 2, i)) {
                return true;
            }
        } else {
            if (TelephonyManager.getDefault().getCurrentPhoneType(i) == 2) {
                zIsEmergencyNumberExt = isEmergencyNumberExt(strExtractNetworkPortionAlt, 2, i);
            } else {
                zIsEmergencyNumberExt = isEmergencyNumberExt(strExtractNetworkPortionAlt, 1, i);
            }
            if (zIsEmergencyNumberExt) {
                return true;
            }
        }
        if (str2 != null && shouldCheckGoogleEcc(str2) && !isMmiCode(strExtractNetworkPortionAlt)) {
            ShortNumberInfo shortNumberInfo = ShortNumberInfo.getInstance();
            if (z) {
                zConnectsToEmergencyNumber = shortNumberInfo.isEmergencyNumber(strExtractNetworkPortionAlt, str2);
            } else {
                zConnectsToEmergencyNumber = shortNumberInfo.connectsToEmergencyNumber(strExtractNetworkPortionAlt, str2);
            }
            dlog("[isEmergencyNumberExt] AOSP check return: " + zConnectsToEmergencyNumber + ", iso: " + str2 + ", useExactMatch: " + z);
            return zConnectsToEmergencyNumber;
        }
        dlog("[isEmergencyNumber] no match ");
        return false;
    }

    private static boolean shouldCheckGoogleEcc(String str) {
        for (int i = 0; i < COUNTRIES_NOT_USE_ECC_LIB.length; i++) {
            if (str.equals(COUNTRIES_NOT_USE_ECC_LIB[i])) {
                dlog("[shouldCheckGoogleEcc] should not check for iso: " + str);
                return false;
            }
        }
        return true;
    }

    public static boolean isEmergencyNumberExt(String str, int i) {
        dlog("[isEmergencyNumberExt], number:" + str + ", phoneType:" + i);
        return isEmergencyNumberExt(str, i, Integer.MAX_VALUE);
    }

    public static boolean isEmergencyNumberExt(String str, int i, int i2) {
        vlog("[isEmergencyNumberExt], number:" + str + ", phoneType:" + i);
        if (isHighPriorityAccessEmergencyNumber(str)) {
            return true;
        }
        Iterator<EccSource> it = sAllEccSource.iterator();
        while (it.hasNext()) {
            if (it.next().isEmergencyNumber(str, i2, i)) {
                return true;
            }
        }
        dlog("[isEmergencyNumberExt] no match for phoneType: " + i);
        return false;
    }

    public static boolean isSpecialEmergencyNumber(String str) {
        return isSpecialEmergencyNumber(Integer.MAX_VALUE, str);
    }

    public static boolean isSpecialEmergencyNumber(int i, String str) {
        if (sNetworkEcc.isEmergencyNumber(str, i, 1) || sSimEcc.isEmergencyNumber(str, i, 1)) {
            return false;
        }
        Iterator<EccSource> it = sAllEccSource.iterator();
        while (it.hasNext()) {
            if (it.next().isSpecialEmergencyNumber(i, str)) {
                return true;
            }
        }
        log("[isSpecialEmergencyNumber] not special ecc");
        return false;
    }

    public static ArrayList<String> getEccList() {
        ArrayList<EccEntry> arrayList = new ArrayList<>();
        if (sIsCtaSet) {
            sCtaEcc.addToEccList(arrayList);
        }
        sPropertyEcc.addToEccList(arrayList);
        sXmlEcc.addToEccList(arrayList);
        dlog("[getEccList] ECC list: " + arrayList);
        int size = arrayList.size() <= 15 ? arrayList.size() : 15;
        ArrayList<String> arrayList2 = new ArrayList<>();
        int i = 0;
        do {
            int i2 = size <= 10 ? size : 10;
            String str = i2 + "";
            for (int i3 = 0; i3 < i2; i3++) {
                EccEntry eccEntry = arrayList.get((i * 10) + i3);
                if (eccEntry != null) {
                    String str2 = (str + ",\"" + eccEntry.getEcc() + "\"") + "," + eccEntry.getCategory();
                    String condition = eccEntry.getCondition();
                    if (condition.equals(EccEntry.ECC_FOR_MMI) || !TextUtils.isEmpty(eccEntry.getPlmn())) {
                        condition = "0";
                    }
                    str = str2 + "," + condition;
                }
            }
            dlog("[getEccList] syncAtString: " + str);
            arrayList2.add(str);
            i++;
            size += -10;
        } while (size > 0);
        return arrayList2;
    }

    public static String getSpecialEccList() {
        String str = "";
        if (sXmlEcc == null) {
            return "";
        }
        for (EccEntry eccEntry : sXmlEcc.mEccList) {
            if (eccEntry.getCondition().equals(EccEntry.ECC_FOR_MMI)) {
                str = str + eccEntry.getEcc() + "," + eccEntry.getPlmn() + ",";
            }
        }
        dlog("[DBG]getSpecialEccList: " + str);
        return str;
    }

    public static void setSpecificEccCategory(int i) {
        log("[setSpecificEccCategory] set ECC category: " + i);
        sSpecificEccCat = i;
    }

    public static int getServiceCategoryFromEcc(String str) {
        return getServiceCategoryFromEccBySubId(str, Integer.MAX_VALUE);
    }

    public static int getServiceCategoryFromEccBySubId(String str, int i) {
        if (sSpecificEccCat >= 0) {
            log("[getServiceCategoryFromEccBySubId] specific ECC category: " + sSpecificEccCat);
            int i2 = sSpecificEccCat;
            sSpecificEccCat = -1;
            return i2;
        }
        Iterator<EccSource> it = sAllEccSource.iterator();
        while (it.hasNext()) {
            int serviceCategory = it.next().getServiceCategory(str, i);
            if (serviceCategory > 0) {
                return serviceCategory;
            }
        }
        log("[getServiceCategoryFromEccBySubId] no matched, ECC: " + str + ", subId: " + i);
        return 0;
    }

    private static int getQueryPhoneType(int i) {
        boolean z;
        boolean z2;
        int i2;
        int i3;
        boolean z3;
        int phoneCount = TelephonyManager.getDefault().getPhoneCount();
        if (SystemProperties.getInt("vendor.gsm.gcf.testmode", 0) == 2) {
            return 1;
        }
        if (sIsC2kSupport) {
            z2 = false;
            z = false;
            for (int i4 = 0; i4 < phoneCount; i4++) {
                int currentPhoneTypeForSlot = TelephonyManager.getDefault().getCurrentPhoneTypeForSlot(i4);
                if (currentPhoneTypeForSlot == 1) {
                    z2 = true;
                } else if (currentPhoneTypeForSlot == 2) {
                    z = true;
                }
            }
            if (!z2 && !isSimInsert()) {
            }
            if (sIsC2kSupport && phoneCount > 1 && !z) {
                int[] iArr = new int[phoneCount];
                for (i2 = 0; i2 < phoneCount; i2++) {
                    try {
                        iArr[i2] = MtkTelephonyManagerEx.getDefault().getIccAppFamily(i2);
                    } catch (NullPointerException e) {
                        log("getIccAppFamily, NullPointerException:" + e);
                    }
                }
                for (i3 = 0; i3 < phoneCount; i3++) {
                    if (iArr[i3] >= 2 || isCt3gDualModeCard(i3)) {
                        log("[getQueryPhoneType] Slot" + i3 + " is roaming");
                        z3 = true;
                        break;
                    }
                }
                z3 = false;
                if (!z3) {
                    int i5 = 0;
                    while (true) {
                        if (i5 >= phoneCount) {
                            break;
                        }
                        if (iArr[i5] != 0) {
                            i5++;
                        } else {
                            vlog("[getQueryPhoneType] Slot" + i5 + " no card");
                            z = true;
                            break;
                        }
                    }
                }
            }
            int i6 = z2 ? 1 : 0;
            if (z) {
                i6 |= 2;
            }
            vlog("[getQueryPhoneType] needQueryGsm:" + z2 + ", needQueryCdma:" + z + ", ret: " + i6);
            return i6;
        }
        z = false;
        z2 = true;
        if (sIsC2kSupport) {
            int[] iArr2 = new int[phoneCount];
            while (i2 < phoneCount) {
            }
            while (i3 < phoneCount) {
            }
            z3 = false;
            if (!z3) {
            }
        }
        if (z2) {
        }
        if (z) {
        }
        vlog("[getQueryPhoneType] needQueryGsm:" + z2 + ", needQueryCdma:" + z + ", ret: " + i6);
        return i6;
    }

    private static boolean isCt3gDualModeCard(int i) {
        String[] strArr = {"vendor.gsm.ril.ct3g", "vendor.gsm.ril.ct3g.2", "vendor.gsm.ril.ct3g.3", "vendor.gsm.ril.ct3g.4"};
        if (i < 0 || i >= strArr.length) {
            return false;
        }
        return EccEntry.ECC_ALWAYS.equals(SystemProperties.get(strArr[i]));
    }

    public static int getMinMatch() {
        sIsCtaSupport = EccEntry.ECC_ALWAYS.equals(SystemProperties.get("ro.vendor.mtk_cta_support"));
        sIsOP09Support = "OP09".equals(SystemProperties.get("persist.vendor.operator.optr")) && ("SEGDEFAULT".equals(SystemProperties.get("persist.vendor.operator.seg")) || "SEGC".equals(SystemProperties.get("persist.vendor.operator.seg")));
        if (sIsOP09Support || sIsCtaSupport) {
            log("[DBG] getMinMatch return 11 for CTA/OP09");
            return 11;
        }
        log("[DBG] getMinMatch return 7");
        return 7;
    }

    private static boolean isMmiCode(String str) {
        if (Pattern.compile("((\\*|#|\\*#|\\*\\*|##)(\\d{2,3})(\\*([^*#]*)(\\*([^*#]*)(\\*([^*#]*)(\\*([^*#]*))?)?)?)?#)(.*)").matcher(str).matches() || str.endsWith("#")) {
            return true;
        }
        return false;
    }

    private static boolean isSimInsert() {
        int phoneCount = TelephonyManager.getDefault().getPhoneCount();
        for (int i = 0; i < phoneCount; i++) {
            if (TelephonyManager.getDefault().hasIccCard(i)) {
                return true;
            }
        }
        return false;
    }

    public static int getDefaultVoiceSubId() {
        return Integer.MAX_VALUE;
    }

    public static boolean isHighPriorityAccessEmergencyNumber(String str) {
        if (!"OP12".equals(SystemProperties.get("persist.vendor.operator.optr")) || !Pattern.compile("^[*|#]272[*|#]{0,1}911$").matcher(str).matches()) {
            return false;
        }
        dlog("[isHighPriorityAccessEmergencyNumber] return true");
        return true;
    }

    private static void log(String str) {
        Rlog.i(LOG_TAG, str);
    }

    private static void dlog(String str) {
        Rlog.d(LOG_TAG, str);
    }

    private static void vlog(String str) {
    }
}
