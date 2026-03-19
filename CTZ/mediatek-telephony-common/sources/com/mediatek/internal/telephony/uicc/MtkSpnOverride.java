package com.mediatek.internal.telephony.uicc;

import android.content.Context;
import android.os.Environment;
import android.os.SystemProperties;
import android.telephony.Rlog;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.util.Xml;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneFactory;
import com.android.internal.util.XmlUtils;
import com.mediatek.internal.telephony.MtkGsmCdmaPhone;
import com.mediatek.internal.telephony.datasub.DataSubConstants;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

public class MtkSpnOverride {
    private static HashMap<String, String> CarrierVirtualSpnMapByEfGid1 = null;
    private static HashMap<String, String> CarrierVirtualSpnMapByEfPnn = null;
    private static HashMap<String, String> CarrierVirtualSpnMapByEfSpn = null;
    static final String LOG_TAG = "SpnOverride";
    static final String LOG_TAG_EX = "MtkSpnOverride";
    protected static final String OEM_SPN_OVERRIDE_PATH = "telephony/spn-conf.xml";
    protected static final String PARTNER_SPN_OVERRIDE_PATH = "etc/spn-conf.xml";
    private static final String PARTNER_VIRTUAL_SPN_BY_EF_GID1_OVERRIDE_PATH = "etc/virtual-spn-conf-by-efgid1.xml";
    private static final String PARTNER_VIRTUAL_SPN_BY_EF_PNN_OVERRIDE_PATH = "etc/virtual-spn-conf-by-efpnn.xml";
    private static final String PARTNER_VIRTUAL_SPN_BY_EF_SPN_OVERRIDE_PATH = "etc/virtual-spn-conf-by-efspn.xml";
    private static final String PARTNER_VIRTUAL_SPN_BY_IMSI_OVERRIDE_PATH = "etc/virtual-spn-conf-by-imsi.xml";
    static final Object sInstSync = new Object();
    private static MtkSpnOverride sInstance;
    private ArrayList CarrierVirtualSpnMapByImsi;
    protected HashMap<String, String> mCarrierSpnMap = new HashMap<>();

    public class VirtualSpnByImsi {
        public String name;
        public String pattern;

        public VirtualSpnByImsi(String str, String str2) {
            this.pattern = str;
            this.name = str2;
        }
    }

    public static MtkSpnOverride getInstance() {
        MtkSpnOverride mtkSpnOverride;
        synchronized (sInstSync) {
            if (sInstance == null) {
                sInstance = new MtkSpnOverride();
            }
            mtkSpnOverride = sInstance;
        }
        return mtkSpnOverride;
    }

    MtkSpnOverride() {
        loadSpnOverrides();
        CarrierVirtualSpnMapByEfSpn = new HashMap<>();
        loadVirtualSpnOverridesByEfSpn();
        this.CarrierVirtualSpnMapByImsi = new ArrayList();
        loadVirtualSpnOverridesByImsi();
        CarrierVirtualSpnMapByEfPnn = new HashMap<>();
        loadVirtualSpnOverridesByEfPnn();
        CarrierVirtualSpnMapByEfGid1 = new HashMap<>();
        loadVirtualSpnOverridesByEfGid1();
    }

    protected void loadSpnOverrides() {
        File file;
        Rlog.d(LOG_TAG_EX, "loadSpnOverrides");
        if (DataSubConstants.OPERATOR_OP09.equals(SystemProperties.get(DataSubConstants.PROPERTY_OPERATOR_OPTR, ""))) {
            file = new File(Environment.getVendorDirectory(), "etc/spn-conf-op09.xml");
            if (!file.exists()) {
                Rlog.d(LOG_TAG_EX, "No spn-conf-op09.xml file");
                file = new File(Environment.getRootDirectory(), PARTNER_SPN_OVERRIDE_PATH);
            }
        } else {
            file = new File(Environment.getRootDirectory(), PARTNER_SPN_OVERRIDE_PATH);
        }
        File file2 = new File(Environment.getOemDirectory(), OEM_SPN_OVERRIDE_PATH);
        if (file2.exists()) {
            long jLastModified = file2.lastModified();
            long jLastModified2 = file.lastModified();
            Rlog.d(LOG_TAG_EX, "SPN Timestamp: oemTime = " + jLastModified + " sysTime = " + jLastModified2);
            if (jLastModified > jLastModified2) {
                Rlog.d(LOG_TAG_EX, "SPN in OEM image is newer than System image");
                file = file2;
            }
        } else {
            Rlog.d(LOG_TAG_EX, "No SPN in OEM image = " + file2.getPath() + " Load SPN from system image");
        }
        try {
            FileReader fileReader = new FileReader(file);
            try {
                XmlPullParser xmlPullParserNewPullParser = Xml.newPullParser();
                xmlPullParserNewPullParser.setInput(fileReader);
                XmlUtils.beginDocument(xmlPullParserNewPullParser, "spnOverrides");
                while (true) {
                    XmlUtils.nextElement(xmlPullParserNewPullParser);
                    if ("spnOverride".equals(xmlPullParserNewPullParser.getName())) {
                        this.mCarrierSpnMap.put(xmlPullParserNewPullParser.getAttributeValue(null, "numeric"), xmlPullParserNewPullParser.getAttributeValue(null, "spn"));
                    } else {
                        fileReader.close();
                        return;
                    }
                }
            } catch (IOException e) {
                Rlog.w(LOG_TAG_EX, "Exception in spn-conf parser " + e);
            } catch (XmlPullParserException e2) {
                Rlog.w(LOG_TAG_EX, "Exception in spn-conf parser " + e2);
            }
        } catch (FileNotFoundException e3) {
            Rlog.w(LOG_TAG_EX, "Can not open " + file.getAbsolutePath());
        }
    }

    private static void loadVirtualSpnOverridesByEfSpn() {
        Rlog.d(LOG_TAG_EX, "loadVirtualSpnOverridesByEfSpn");
        try {
            FileReader fileReader = new FileReader(new File(Environment.getVendorDirectory(), PARTNER_VIRTUAL_SPN_BY_EF_SPN_OVERRIDE_PATH));
            try {
                XmlPullParser xmlPullParserNewPullParser = Xml.newPullParser();
                xmlPullParserNewPullParser.setInput(fileReader);
                XmlUtils.beginDocument(xmlPullParserNewPullParser, "virtualSpnOverridesByEfSpn");
                while (true) {
                    XmlUtils.nextElement(xmlPullParserNewPullParser);
                    if ("virtualSpnOverride".equals(xmlPullParserNewPullParser.getName())) {
                        String attributeValue = xmlPullParserNewPullParser.getAttributeValue(null, "mccmncspn");
                        String attributeValue2 = xmlPullParserNewPullParser.getAttributeValue(null, "name");
                        Rlog.w(LOG_TAG_EX, "test mccmncspn = " + attributeValue + ", name = " + attributeValue2);
                        CarrierVirtualSpnMapByEfSpn.put(attributeValue, attributeValue2);
                    } else {
                        fileReader.close();
                        return;
                    }
                }
            } catch (IOException e) {
                Rlog.w(LOG_TAG_EX, "Exception in virtual-spn-conf-by-efspn parser " + e);
            } catch (XmlPullParserException e2) {
                Rlog.w(LOG_TAG_EX, "Exception in virtual-spn-conf-by-efspn parser " + e2);
            }
        } catch (FileNotFoundException e3) {
            Rlog.w(LOG_TAG_EX, "Can't open " + Environment.getVendorDirectory() + "/" + PARTNER_VIRTUAL_SPN_BY_EF_SPN_OVERRIDE_PATH);
        }
    }

    public String getSpnByEfSpn(String str, String str2) {
        if (str == null || str2 == null || str.isEmpty() || str2.isEmpty()) {
            return null;
        }
        return CarrierVirtualSpnMapByEfSpn.get(str + str2);
    }

    private void loadVirtualSpnOverridesByImsi() {
        Rlog.d(LOG_TAG_EX, "loadVirtualSpnOverridesByImsi");
        try {
            FileReader fileReader = new FileReader(new File(Environment.getVendorDirectory(), PARTNER_VIRTUAL_SPN_BY_IMSI_OVERRIDE_PATH));
            try {
                XmlPullParser xmlPullParserNewPullParser = Xml.newPullParser();
                xmlPullParserNewPullParser.setInput(fileReader);
                XmlUtils.beginDocument(xmlPullParserNewPullParser, "virtualSpnOverridesByImsi");
                while (true) {
                    XmlUtils.nextElement(xmlPullParserNewPullParser);
                    if ("virtualSpnOverride".equals(xmlPullParserNewPullParser.getName())) {
                        String attributeValue = xmlPullParserNewPullParser.getAttributeValue(null, "imsipattern");
                        String attributeValue2 = xmlPullParserNewPullParser.getAttributeValue(null, "name");
                        Rlog.w(LOG_TAG_EX, "test imsipattern = " + attributeValue + ", name = " + attributeValue2);
                        this.CarrierVirtualSpnMapByImsi.add(new VirtualSpnByImsi(attributeValue, attributeValue2));
                    } else {
                        fileReader.close();
                        return;
                    }
                }
            } catch (IOException e) {
                Rlog.w(LOG_TAG_EX, "Exception in virtual-spn-conf-by-imsi parser " + e);
            } catch (XmlPullParserException e2) {
                Rlog.w(LOG_TAG_EX, "Exception in virtual-spn-conf-by-imsi parser " + e2);
            }
        } catch (FileNotFoundException e3) {
            Rlog.w(LOG_TAG_EX, "Can't open " + Environment.getVendorDirectory() + "/" + PARTNER_VIRTUAL_SPN_BY_IMSI_OVERRIDE_PATH);
        }
    }

    public String getSpnByImsi(String str, String str2) {
        if (str == null || str2 == null || str.isEmpty() || str2.isEmpty()) {
            return null;
        }
        for (int i = 0; i < this.CarrierVirtualSpnMapByImsi.size(); i++) {
            VirtualSpnByImsi virtualSpnByImsi = (VirtualSpnByImsi) this.CarrierVirtualSpnMapByImsi.get(i);
            StringBuilder sb = new StringBuilder();
            sb.append("getSpnByImsi(): mccmnc = ");
            sb.append(str);
            sb.append(", imsi = ");
            sb.append(str2.length() >= 6 ? str2.substring(0, 6) : "xx");
            sb.append(", pattern = ");
            sb.append(virtualSpnByImsi.pattern);
            Rlog.d(LOG_TAG_EX, sb.toString());
            if (imsiMatches(virtualSpnByImsi.pattern, str + str2)) {
                return virtualSpnByImsi.name;
            }
        }
        return null;
    }

    public String isOperatorMvnoForImsi(String str, String str2) {
        if (str == null || str2 == null || str.isEmpty() || str2.isEmpty()) {
            return null;
        }
        for (int i = 0; i < this.CarrierVirtualSpnMapByImsi.size(); i++) {
            VirtualSpnByImsi virtualSpnByImsi = (VirtualSpnByImsi) this.CarrierVirtualSpnMapByImsi.get(i);
            StringBuilder sb = new StringBuilder();
            sb.append("isOperatorMvnoForImsi(): mccmnc = ");
            sb.append(str);
            sb.append(", imsi = ");
            sb.append(str2.length() >= 6 ? str2.substring(0, 6) : "xx");
            sb.append(", pattern = ");
            sb.append(virtualSpnByImsi.pattern);
            Rlog.w(LOG_TAG_EX, sb.toString());
            if (imsiMatches(virtualSpnByImsi.pattern, str + str2)) {
                return virtualSpnByImsi.pattern;
            }
        }
        return null;
    }

    private boolean imsiMatches(String str, String str2) {
        String strSubstring;
        int length = str.length();
        StringBuilder sb = new StringBuilder();
        sb.append("mvno match imsi = ");
        if (str2 == null) {
            strSubstring = "";
        } else {
            strSubstring = str2.length() >= 6 ? str2.substring(0, 6) : "xx";
        }
        sb.append(strSubstring);
        sb.append("pattern = ");
        sb.append(str);
        Rlog.d(LOG_TAG_EX, sb.toString());
        if (length <= 0 || str2 == null || length > str2.length()) {
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

    private static void loadVirtualSpnOverridesByEfPnn() {
        Rlog.d(LOG_TAG_EX, "loadVirtualSpnOverridesByEfPnn");
        try {
            FileReader fileReader = new FileReader(new File(Environment.getVendorDirectory(), PARTNER_VIRTUAL_SPN_BY_EF_PNN_OVERRIDE_PATH));
            try {
                XmlPullParser xmlPullParserNewPullParser = Xml.newPullParser();
                xmlPullParserNewPullParser.setInput(fileReader);
                XmlUtils.beginDocument(xmlPullParserNewPullParser, "virtualSpnOverridesByEfPnn");
                while (true) {
                    XmlUtils.nextElement(xmlPullParserNewPullParser);
                    if ("virtualSpnOverride".equals(xmlPullParserNewPullParser.getName())) {
                        String attributeValue = xmlPullParserNewPullParser.getAttributeValue(null, "mccmncpnn");
                        String attributeValue2 = xmlPullParserNewPullParser.getAttributeValue(null, "name");
                        Rlog.w(LOG_TAG_EX, "test mccmncpnn = " + attributeValue + ", name = " + attributeValue2);
                        CarrierVirtualSpnMapByEfPnn.put(attributeValue, attributeValue2);
                    } else {
                        fileReader.close();
                        return;
                    }
                }
            } catch (IOException e) {
                Rlog.w(LOG_TAG_EX, "Exception in virtual-spn-conf-by-efpnn parser " + e);
            } catch (XmlPullParserException e2) {
                Rlog.w(LOG_TAG_EX, "Exception in virtual-spn-conf-by-efpnn parser " + e2);
            }
        } catch (FileNotFoundException e3) {
            Rlog.w(LOG_TAG_EX, "Can't open " + Environment.getVendorDirectory() + "/" + PARTNER_VIRTUAL_SPN_BY_EF_PNN_OVERRIDE_PATH);
        }
    }

    public String getSpnByEfPnn(String str, String str2) {
        if (str == null || str2 == null || str.isEmpty() || str2.isEmpty()) {
            return null;
        }
        return CarrierVirtualSpnMapByEfPnn.get(str + str2);
    }

    private static void loadVirtualSpnOverridesByEfGid1() {
        Rlog.d(LOG_TAG_EX, "loadVirtualSpnOverridesByEfGid1");
        try {
            FileReader fileReader = new FileReader(new File(Environment.getVendorDirectory(), PARTNER_VIRTUAL_SPN_BY_EF_GID1_OVERRIDE_PATH));
            try {
                XmlPullParser xmlPullParserNewPullParser = Xml.newPullParser();
                xmlPullParserNewPullParser.setInput(fileReader);
                XmlUtils.beginDocument(xmlPullParserNewPullParser, "virtualSpnOverridesByEfGid1");
                while (true) {
                    XmlUtils.nextElement(xmlPullParserNewPullParser);
                    if ("virtualSpnOverride".equals(xmlPullParserNewPullParser.getName())) {
                        String attributeValue = xmlPullParserNewPullParser.getAttributeValue(null, "mccmncgid1");
                        String attributeValue2 = xmlPullParserNewPullParser.getAttributeValue(null, "name");
                        Rlog.w(LOG_TAG_EX, "test mccmncgid1 = " + attributeValue + ", name = " + attributeValue2);
                        CarrierVirtualSpnMapByEfGid1.put(attributeValue, attributeValue2);
                    } else {
                        fileReader.close();
                        return;
                    }
                }
            } catch (IOException e) {
                Rlog.w(LOG_TAG_EX, "Exception in virtual-spn-conf-by-efgid1 parser " + e);
            } catch (XmlPullParserException e2) {
                Rlog.w(LOG_TAG_EX, "Exception in virtual-spn-conf-by-efgid1 parser " + e2);
            }
        } catch (FileNotFoundException e3) {
            Rlog.w(LOG_TAG_EX, "Can't open " + Environment.getVendorDirectory() + "/" + PARTNER_VIRTUAL_SPN_BY_EF_GID1_OVERRIDE_PATH);
        }
    }

    public String getSpnByEfGid1(String str, String str2) {
        if (str == null || str2 == null || str.isEmpty() || str2.isEmpty()) {
            return null;
        }
        return CarrierVirtualSpnMapByEfGid1.get(str + str2);
    }

    public String getSpnByPattern(int i, String str) {
        MtkGsmCdmaPhone phone = PhoneFactory.getPhone(SubscriptionManager.getPhoneId(i));
        MtkGsmCdmaPhone mtkGsmCdmaPhone = phone;
        String spnByEfSpn = getSpnByEfSpn(str, mtkGsmCdmaPhone.getMvnoPattern("spn"));
        Rlog.d(LOG_TAG_EX, "the result of searching mvnoOperName by EF_SPN: " + spnByEfSpn);
        if (spnByEfSpn == null) {
            spnByEfSpn = getSpnByImsi(str, phone.getSubscriberId());
            Rlog.d(LOG_TAG_EX, "the result of searching mvnoOperName by IMSI: " + spnByEfSpn);
        }
        if (spnByEfSpn == null) {
            spnByEfSpn = getSpnByEfPnn(str, mtkGsmCdmaPhone.getMvnoPattern("pnn"));
            Rlog.d(LOG_TAG_EX, "the result of searching mvnoOperName by EF_PNN: " + spnByEfSpn);
        }
        if (spnByEfSpn == null) {
            String spnByEfGid1 = getSpnByEfGid1(str, mtkGsmCdmaPhone.getMvnoPattern("gid"));
            Rlog.d(LOG_TAG_EX, "the result of searching mvnoOperName by EF_GID1: " + spnByEfGid1);
            return spnByEfGid1;
        }
        return spnByEfSpn;
    }

    private boolean isForceGetCtSpnFromRes(int i, String str, Context context, String str2) {
        boolean z;
        Phone phone = PhoneFactory.getPhone(SubscriptionManager.getPhoneId(i));
        String string = context.getText(134545641).toString();
        String simOperatorName = TelephonyManager.from(context).getSimOperatorName(i);
        Rlog.d(LOG_TAG_EX, "ctName:" + string + ", simCarrierName:" + simOperatorName + ", subId:" + i);
        if (string == null || !(string.equals(str2) || string.equals(simOperatorName))) {
            z = false;
        } else {
            Rlog.d(LOG_TAG_EX, "Get from resource.");
            z = true;
        }
        if (("20404".equals(str) || "45403".equals(str)) && phone.getPhoneType() == 2 && string != null && string.equals(simOperatorName)) {
            Rlog.d(LOG_TAG_EX, "Special handle for roaming case!");
            return true;
        }
        return z;
    }

    public String getSpnByNumeric(String str, boolean z, Context context) {
        return getSpnByNumeric(str, z, context, false, true);
    }

    private String getSpnByNumeric(String str, boolean z, Context context, boolean z2, boolean z3) {
        if (z) {
            if (str.equals("46000") || str.equals("46002") || str.equals("46004") || str.equals("46007") || str.equals("46008")) {
                return context.getText(134545437).toString();
            }
            if (str.equals("46001") || str.equals("46009") || str.equals("45407")) {
                return context.getText(134545438).toString();
            }
            if (str.equals("46003") || str.equals("46011") || z2) {
                return context.getText(134545507).toString();
            }
            if (str.equals("46601")) {
                return context.getText(134545439).toString();
            }
            if (str.equals("46692")) {
                return context.getText(134545440).toString();
            }
            if (str.equals("46697")) {
                return context.getText(134545441).toString();
            }
            if (str.equals("99998")) {
                return context.getText(134545442).toString();
            }
            if (str.equals("99999")) {
                return context.getText(134545443).toString();
            }
            if (z3 && containsCarrier(str)) {
                return getSpn(str);
            }
            Rlog.d(LOG_TAG_EX, "Can't find long operator name for " + str);
        } else if (!z) {
            if (str.equals("46000") || str.equals("46002") || str.equals("46004") || str.equals("46007") || str.equals("46008")) {
                return context.getText(134545444).toString();
            }
            if (str.equals("46001") || str.equals("46009") || str.equals("45407")) {
                return context.getText(134545445).toString();
            }
            if (str.equals("46003") || str.equals("46011") || z2) {
                return context.getText(134545508).toString();
            }
            if (str.equals("46601")) {
                return context.getText(134545446).toString();
            }
            if (str.equals("46692")) {
                return context.getText(134545447).toString();
            }
            if (str.equals("46697")) {
                return context.getText(134545448).toString();
            }
            if (str.equals("99997")) {
                return context.getText(134545449).toString();
            }
            if (str.equals("99999")) {
                return context.getText(134545450).toString();
            }
            Rlog.d(LOG_TAG_EX, "Can't find short operator name for " + str);
        }
        return null;
    }

    public String lookupOperatorName(int i, String str, boolean z, Context context, String str2) {
        String spnByNumeric;
        if (PhoneFactory.getPhone(SubscriptionManager.getPhoneId(i)) == null) {
            Rlog.w(LOG_TAG_EX, "lookupOperatorName getPhone null");
            return str2;
        }
        String spnByPattern = getSpnByPattern(i, str);
        boolean zIsForceGetCtSpnFromRes = isForceGetCtSpnFromRes(i, str, context, spnByPattern);
        if (spnByPattern == null || zIsForceGetCtSpnFromRes) {
            spnByNumeric = getSpnByNumeric(str, z, context, zIsForceGetCtSpnFromRes, true);
        } else {
            spnByNumeric = spnByPattern;
        }
        return spnByNumeric == null ? str2 : spnByNumeric;
    }

    public String lookupOperatorName(int i, String str, boolean z, Context context) {
        return lookupOperatorName(i, str, z, context, str);
    }

    public String lookupOperatorNameForDisplayName(int i, String str, boolean z, Context context) {
        return lookupOperatorName(i, str, z, context, null);
    }

    public boolean containsCarrier(String str) {
        return this.mCarrierSpnMap.containsKey(str);
    }

    public String getSpn(String str) {
        return this.mCarrierSpnMap.get(str);
    }

    public boolean containsCarrierEx(String str) {
        return containsCarrier(str);
    }

    public String getSpnEx(String str) {
        return getSpn(str);
    }
}
