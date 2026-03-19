package com.android.internal.telephony.uicc;

import android.content.Context;
import android.os.Environment;
import android.os.SystemProperties;
import android.provider.Telephony;
import android.telephony.Rlog;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.util.Xml;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneFactory;
import com.android.internal.util.XmlUtils;
import com.mediatek.internal.telephony.uicc.UsimPBMemInfo;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

public class SpnOverride {
    private static HashMap<String, String> CarrierVirtualSpnMapByEfGid1 = null;
    private static HashMap<String, String> CarrierVirtualSpnMapByEfPnn = null;
    private static HashMap<String, String> CarrierVirtualSpnMapByEfSpn = null;
    static final String LOG_TAG = "SpnOverride";
    static final String OEM_SPN_OVERRIDE_PATH = "telephony/spn-conf.xml";
    static final String PARTNER_SPN_OVERRIDE_PATH = "etc/spn-conf.xml";
    private static final String PARTNER_VIRTUAL_SPN_BY_EF_GID1_OVERRIDE_PATH = "etc/virtual-spn-conf-by-efgid1.xml";
    private static final String PARTNER_VIRTUAL_SPN_BY_EF_PNN_OVERRIDE_PATH = "etc/virtual-spn-conf-by-efpnn.xml";
    private static final String PARTNER_VIRTUAL_SPN_BY_EF_SPN_OVERRIDE_PATH = "etc/virtual-spn-conf-by-efspn.xml";
    private static final String PARTNER_VIRTUAL_SPN_BY_IMSI_OVERRIDE_PATH = "etc/virtual-spn-conf-by-imsi.xml";
    static final Object sInstSync = new Object();
    private static SpnOverride sInstance;
    private ArrayList CarrierVirtualSpnMapByImsi;
    private HashMap<String, String> mCarrierSpnMap = new HashMap<>();

    public class VirtualSpnByImsi {
        public String name;
        public String pattern;

        public VirtualSpnByImsi(String pattern, String name) {
            this.pattern = pattern;
            this.name = name;
        }
    }

    public static SpnOverride getInstance() {
        synchronized (sInstSync) {
            if (sInstance == null) {
                sInstance = new SpnOverride();
            }
        }
        return sInstance;
    }

    SpnOverride() {
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

    boolean containsCarrier(String carrier) {
        return this.mCarrierSpnMap.containsKey(carrier);
    }

    String getSpn(String carrier) {
        return this.mCarrierSpnMap.get(carrier);
    }

    private void loadSpnOverrides() {
        File spnFile;
        Rlog.d(LOG_TAG, "loadSpnOverrides");
        if ("OP09".equals(SystemProperties.get("persist.operator.optr", UsimPBMemInfo.STRING_NOT_SET))) {
            spnFile = new File(Environment.getVendorDirectory(), "etc/spn-conf-op09.xml");
            if (!spnFile.exists()) {
                Rlog.d(LOG_TAG, "No spn-conf-op09.xml file");
                spnFile = new File(Environment.getRootDirectory(), PARTNER_SPN_OVERRIDE_PATH);
            }
        } else {
            spnFile = new File(Environment.getRootDirectory(), PARTNER_SPN_OVERRIDE_PATH);
        }
        File oemSpnFile = new File(Environment.getOemDirectory(), OEM_SPN_OVERRIDE_PATH);
        if (oemSpnFile.exists()) {
            long oemSpnTime = oemSpnFile.lastModified();
            long sysSpnTime = spnFile.lastModified();
            Rlog.d(LOG_TAG, "SPN Timestamp: oemTime = " + oemSpnTime + " sysTime = " + sysSpnTime);
            if (oemSpnTime > sysSpnTime) {
                Rlog.d(LOG_TAG, "SPN in OEM image is newer than System image");
                spnFile = oemSpnFile;
            }
        } else {
            Rlog.d(LOG_TAG, "No SPN in OEM image = " + oemSpnFile.getPath() + " Load SPN from system image");
        }
        try {
            FileReader spnReader = new FileReader(spnFile);
            try {
                XmlPullParser parser = Xml.newPullParser();
                parser.setInput(spnReader);
                XmlUtils.beginDocument(parser, "spnOverrides");
                while (true) {
                    XmlUtils.nextElement(parser);
                    String name = parser.getName();
                    if ("spnOverride".equals(name)) {
                        String numeric = parser.getAttributeValue(null, Telephony.Carriers.NUMERIC);
                        String data = parser.getAttributeValue(null, Telephony.Carriers.SPN);
                        this.mCarrierSpnMap.put(numeric, data);
                    } else {
                        spnReader.close();
                        return;
                    }
                }
            } catch (IOException e) {
                Rlog.w(LOG_TAG, "Exception in spn-conf parser " + e);
            } catch (XmlPullParserException e2) {
                Rlog.w(LOG_TAG, "Exception in spn-conf parser " + e2);
            }
        } catch (FileNotFoundException e3) {
            Rlog.w(LOG_TAG, "Can not open " + spnFile.getAbsolutePath());
        }
    }

    private static void loadVirtualSpnOverridesByEfSpn() {
        Rlog.d(LOG_TAG, "loadVirtualSpnOverridesByEfSpn");
        File spnFile = new File(Environment.getVendorDirectory(), PARTNER_VIRTUAL_SPN_BY_EF_SPN_OVERRIDE_PATH);
        try {
            FileReader spnReader = new FileReader(spnFile);
            try {
                XmlPullParser parser = Xml.newPullParser();
                parser.setInput(spnReader);
                XmlUtils.beginDocument(parser, "virtualSpnOverridesByEfSpn");
                while (true) {
                    XmlUtils.nextElement(parser);
                    String name = parser.getName();
                    if (!"virtualSpnOverride".equals(name)) {
                        spnReader.close();
                        return;
                    }
                    String mccmncspn = parser.getAttributeValue(null, "mccmncspn");
                    String spn = parser.getAttributeValue(null, "name");
                    Rlog.w(LOG_TAG, "test mccmncspn = " + mccmncspn + ", name = " + spn);
                    CarrierVirtualSpnMapByEfSpn.put(mccmncspn, spn);
                }
            } catch (IOException e) {
                Rlog.w(LOG_TAG, "Exception in virtual-spn-conf-by-efspn parser " + e);
            } catch (XmlPullParserException e2) {
                Rlog.w(LOG_TAG, "Exception in virtual-spn-conf-by-efspn parser " + e2);
            }
        } catch (FileNotFoundException e3) {
            Rlog.w(LOG_TAG, "Can't open " + Environment.getVendorDirectory() + "/" + PARTNER_VIRTUAL_SPN_BY_EF_SPN_OVERRIDE_PATH);
        }
    }

    public String getSpnByEfSpn(String mccmnc, String spn) {
        if (mccmnc == null || spn == null || mccmnc.isEmpty() || spn.isEmpty()) {
            return null;
        }
        return CarrierVirtualSpnMapByEfSpn.get(mccmnc + spn);
    }

    private void loadVirtualSpnOverridesByImsi() {
        Rlog.d(LOG_TAG, "loadVirtualSpnOverridesByImsi");
        File spnFile = new File(Environment.getVendorDirectory(), PARTNER_VIRTUAL_SPN_BY_IMSI_OVERRIDE_PATH);
        try {
            FileReader spnReader = new FileReader(spnFile);
            try {
                XmlPullParser parser = Xml.newPullParser();
                parser.setInput(spnReader);
                XmlUtils.beginDocument(parser, "virtualSpnOverridesByImsi");
                while (true) {
                    XmlUtils.nextElement(parser);
                    String name = parser.getName();
                    if (!"virtualSpnOverride".equals(name)) {
                        spnReader.close();
                        return;
                    }
                    String imsipattern = parser.getAttributeValue(null, "imsipattern");
                    String spn = parser.getAttributeValue(null, "name");
                    Rlog.w(LOG_TAG, "test imsipattern = " + imsipattern + ", name = " + spn);
                    this.CarrierVirtualSpnMapByImsi.add(new VirtualSpnByImsi(imsipattern, spn));
                }
            } catch (IOException e) {
                Rlog.w(LOG_TAG, "Exception in virtual-spn-conf-by-imsi parser " + e);
            } catch (XmlPullParserException e2) {
                Rlog.w(LOG_TAG, "Exception in virtual-spn-conf-by-imsi parser " + e2);
            }
        } catch (FileNotFoundException e3) {
            Rlog.w(LOG_TAG, "Can't open " + Environment.getVendorDirectory() + "/" + PARTNER_VIRTUAL_SPN_BY_IMSI_OVERRIDE_PATH);
        }
    }

    public String getSpnByImsi(String mccmnc, String imsi) {
        if (mccmnc == null || imsi == null || mccmnc.isEmpty() || imsi.isEmpty()) {
            return null;
        }
        for (int i = 0; i < this.CarrierVirtualSpnMapByImsi.size(); i++) {
            VirtualSpnByImsi vsbi = (VirtualSpnByImsi) this.CarrierVirtualSpnMapByImsi.get(i);
            Rlog.d(LOG_TAG, "getSpnByImsi(): mccmnc = " + mccmnc + ", imsi = " + imsi + ", pattern = " + vsbi.pattern);
            if (imsiMatches(vsbi.pattern, mccmnc + imsi)) {
                return vsbi.name;
            }
        }
        return null;
    }

    public String isOperatorMvnoForImsi(String mccmnc, String imsi) {
        if (mccmnc == null || imsi == null || mccmnc.isEmpty() || imsi.isEmpty()) {
            return null;
        }
        for (int i = 0; i < this.CarrierVirtualSpnMapByImsi.size(); i++) {
            VirtualSpnByImsi vsbi = (VirtualSpnByImsi) this.CarrierVirtualSpnMapByImsi.get(i);
            Rlog.w(LOG_TAG, "isOperatorMvnoForImsi(): mccmnc = " + mccmnc + ", imsi = " + imsi + ", pattern = " + vsbi.pattern);
            if (imsiMatches(vsbi.pattern, mccmnc + imsi)) {
                return vsbi.pattern;
            }
        }
        return null;
    }

    private boolean imsiMatches(String imsiDB, String imsiSIM) {
        int len = imsiDB.length();
        Rlog.d(LOG_TAG, "mvno match imsi = " + imsiSIM + "pattern = " + imsiDB);
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

    private static void loadVirtualSpnOverridesByEfPnn() {
        Rlog.d(LOG_TAG, "loadVirtualSpnOverridesByEfPnn");
        File spnFile = new File(Environment.getVendorDirectory(), PARTNER_VIRTUAL_SPN_BY_EF_PNN_OVERRIDE_PATH);
        try {
            FileReader spnReader = new FileReader(spnFile);
            try {
                XmlPullParser parser = Xml.newPullParser();
                parser.setInput(spnReader);
                XmlUtils.beginDocument(parser, "virtualSpnOverridesByEfPnn");
                while (true) {
                    XmlUtils.nextElement(parser);
                    String name = parser.getName();
                    if (!"virtualSpnOverride".equals(name)) {
                        spnReader.close();
                        return;
                    }
                    String mccmncpnn = parser.getAttributeValue(null, "mccmncpnn");
                    String spn = parser.getAttributeValue(null, "name");
                    Rlog.w(LOG_TAG, "test mccmncpnn = " + mccmncpnn + ", name = " + spn);
                    CarrierVirtualSpnMapByEfPnn.put(mccmncpnn, spn);
                }
            } catch (IOException e) {
                Rlog.w(LOG_TAG, "Exception in virtual-spn-conf-by-efpnn parser " + e);
            } catch (XmlPullParserException e2) {
                Rlog.w(LOG_TAG, "Exception in virtual-spn-conf-by-efpnn parser " + e2);
            }
        } catch (FileNotFoundException e3) {
            Rlog.w(LOG_TAG, "Can't open " + Environment.getVendorDirectory() + "/" + PARTNER_VIRTUAL_SPN_BY_EF_PNN_OVERRIDE_PATH);
        }
    }

    public String getSpnByEfPnn(String mccmnc, String pnn) {
        if (mccmnc == null || pnn == null || mccmnc.isEmpty() || pnn.isEmpty()) {
            return null;
        }
        return CarrierVirtualSpnMapByEfPnn.get(mccmnc + pnn);
    }

    private static void loadVirtualSpnOverridesByEfGid1() {
        Rlog.d(LOG_TAG, "loadVirtualSpnOverridesByEfGid1");
        File spnFile = new File(Environment.getVendorDirectory(), PARTNER_VIRTUAL_SPN_BY_EF_GID1_OVERRIDE_PATH);
        try {
            FileReader spnReader = new FileReader(spnFile);
            try {
                XmlPullParser parser = Xml.newPullParser();
                parser.setInput(spnReader);
                XmlUtils.beginDocument(parser, "virtualSpnOverridesByEfGid1");
                while (true) {
                    XmlUtils.nextElement(parser);
                    String name = parser.getName();
                    if (!"virtualSpnOverride".equals(name)) {
                        spnReader.close();
                        return;
                    }
                    String mccmncgid1 = parser.getAttributeValue(null, "mccmncgid1");
                    String spn = parser.getAttributeValue(null, "name");
                    Rlog.w(LOG_TAG, "test mccmncgid1 = " + mccmncgid1 + ", name = " + spn);
                    CarrierVirtualSpnMapByEfGid1.put(mccmncgid1, spn);
                }
            } catch (IOException e) {
                Rlog.w(LOG_TAG, "Exception in virtual-spn-conf-by-efgid1 parser " + e);
            } catch (XmlPullParserException e2) {
                Rlog.w(LOG_TAG, "Exception in virtual-spn-conf-by-efgid1 parser " + e2);
            }
        } catch (FileNotFoundException e3) {
            Rlog.w(LOG_TAG, "Can't open " + Environment.getVendorDirectory() + "/" + PARTNER_VIRTUAL_SPN_BY_EF_GID1_OVERRIDE_PATH);
        }
    }

    public String getSpnByEfGid1(String mccmnc, String gid1) {
        if (mccmnc == null || gid1 == null || mccmnc.isEmpty() || gid1.isEmpty()) {
            return null;
        }
        return CarrierVirtualSpnMapByEfGid1.get(mccmnc + gid1);
    }

    public String lookupOperatorName(int subId, String numeric, boolean desireLongName, Context context) {
        String operName = numeric;
        Phone phone = PhoneFactory.getPhone(SubscriptionManager.getPhoneId(subId));
        if (phone == null) {
            Rlog.w(LOG_TAG, "lookupOperatorName getPhone null");
            return numeric;
        }
        String mvnoOperName = getSpnByEfSpn(numeric, phone.getMvnoPattern(Telephony.Carriers.SPN));
        Rlog.d(LOG_TAG, "the result of searching mvnoOperName by EF_SPN: " + mvnoOperName);
        if (mvnoOperName == null) {
            mvnoOperName = getSpnByImsi(numeric, phone.getSubscriberId());
        }
        Rlog.d(LOG_TAG, "the result of searching mvnoOperName by IMSI: " + mvnoOperName);
        if (mvnoOperName == null) {
            mvnoOperName = getSpnByEfPnn(numeric, phone.getMvnoPattern(Telephony.Carriers.PNN));
        }
        Rlog.d(LOG_TAG, "the result of searching mvnoOperName by EF_PNN: " + mvnoOperName);
        if (mvnoOperName == null) {
            mvnoOperName = getSpnByEfGid1(numeric, phone.getMvnoPattern("gid"));
        }
        Rlog.d(LOG_TAG, "the result of searching mvnoOperName by EF_GID1: " + mvnoOperName);
        if (mvnoOperName != null) {
            operName = mvnoOperName;
        }
        boolean getFromResource = false;
        String ctName = context.getText(134545642).toString();
        String simCarrierName = TelephonyManager.from(context).getSimOperatorName(subId);
        Rlog.d(LOG_TAG, "ctName:" + ctName + ", simCarrierName:" + simCarrierName + ", subId:" + subId);
        if (ctName != null && ctName.equals(mvnoOperName)) {
            Rlog.d(LOG_TAG, "Get from resource.");
            getFromResource = true;
            mvnoOperName = null;
        }
        if (("20404".equals(numeric) || "45403".equals(numeric)) && phone.getPhoneType() == 2 && ctName != null && ctName.equals(simCarrierName)) {
            Rlog.d(LOG_TAG, "Special handle for roaming case!");
            getFromResource = true;
            mvnoOperName = null;
        }
        if (mvnoOperName == null && desireLongName) {
            if (numeric.equals("46000") || numeric.equals("46002") || numeric.equals("46004") || numeric.equals("46007") || numeric.equals("46008")) {
                String operName2 = context.getText(134545437).toString();
                return operName2;
            }
            if (numeric.equals("46001") || numeric.equals("46009") || numeric.equals("45407")) {
                String operName3 = context.getText(134545438).toString();
                return operName3;
            }
            if (numeric.equals("46003") || numeric.equals("46011") || getFromResource) {
                String operName4 = context.getText(134545507).toString();
                return operName4;
            }
            if (numeric.equals("46601")) {
                String operName5 = context.getText(134545439).toString();
                return operName5;
            }
            if (numeric.equals("46692")) {
                String operName6 = context.getText(134545440).toString();
                return operName6;
            }
            if (numeric.equals("46697")) {
                String operName7 = context.getText(134545441).toString();
                return operName7;
            }
            if (numeric.equals("99998")) {
                String operName8 = context.getText(134545442).toString();
                return operName8;
            }
            if (numeric.equals("99999")) {
                String operName9 = context.getText(134545443).toString();
                return operName9;
            }
            if (containsCarrier(numeric)) {
                String operName10 = getSpn(numeric);
                return operName10;
            }
            Rlog.d(LOG_TAG, "Can't find long operator name for " + numeric);
            return operName;
        }
        if (mvnoOperName != null || desireLongName) {
            return operName;
        }
        if (numeric.equals("46000") || numeric.equals("46002") || numeric.equals("46004") || numeric.equals("46007") || numeric.equals("46008")) {
            String operName11 = context.getText(134545444).toString();
            return operName11;
        }
        if (numeric.equals("46001") || numeric.equals("46009") || numeric.equals("45407")) {
            String operName12 = context.getText(134545445).toString();
            return operName12;
        }
        if (numeric.equals("46003") || numeric.equals("46011") || getFromResource) {
            String operName13 = context.getText(134545508).toString();
            return operName13;
        }
        if (numeric.equals("46601")) {
            String operName14 = context.getText(134545446).toString();
            return operName14;
        }
        if (numeric.equals("46692")) {
            String operName15 = context.getText(134545447).toString();
            return operName15;
        }
        if (numeric.equals("46697")) {
            String operName16 = context.getText(134545448).toString();
            return operName16;
        }
        if (numeric.equals("99997")) {
            String operName17 = context.getText(134545449).toString();
            return operName17;
        }
        if (numeric.equals("99999")) {
            String operName18 = context.getText(134545450).toString();
            return operName18;
        }
        Rlog.d(LOG_TAG, "Can't find short operator name for " + numeric);
        return operName;
    }

    public String lookupOperatorNameForDisplayName(int subId, String numeric, boolean desireLongName, Context context) {
        Phone phone = PhoneFactory.getPhone(SubscriptionManager.getPhoneId(subId));
        if (phone == null) {
            Rlog.w(LOG_TAG, "lookupOperatorName getPhone null");
            return null;
        }
        String mvnoOperName = getSpnByEfSpn(numeric, phone.getMvnoPattern(Telephony.Carriers.SPN));
        Rlog.w(LOG_TAG, "the result of searching mvnoOperName by EF_SPN: " + mvnoOperName);
        if (mvnoOperName == null) {
            mvnoOperName = getSpnByImsi(numeric, phone.getSubscriberId());
        }
        Rlog.w(LOG_TAG, "the result of searching mvnoOperName by IMSI: " + mvnoOperName);
        if (mvnoOperName == null) {
            mvnoOperName = getSpnByEfPnn(numeric, phone.getMvnoPattern(Telephony.Carriers.PNN));
        }
        Rlog.w(LOG_TAG, "the result of searching mvnoOperName by EF_PNN: " + mvnoOperName);
        if (mvnoOperName == null) {
            mvnoOperName = getSpnByEfGid1(numeric, phone.getMvnoPattern("gid"));
        }
        Rlog.w(LOG_TAG, "the result of searching mvnoOperName by EF_GID1: " + mvnoOperName);
        String operName = mvnoOperName != null ? mvnoOperName : null;
        boolean getFromResource = false;
        String ctName = context.getText(134545642).toString();
        String simCarrierName = TelephonyManager.from(context).getSimOperatorName(subId);
        Rlog.d(LOG_TAG, "ctName:" + ctName + ", simCarrierName:" + simCarrierName + ", subId:" + subId);
        if (ctName != null && ctName.equals(mvnoOperName)) {
            Rlog.d(LOG_TAG, "Get from resource.");
            getFromResource = true;
            mvnoOperName = null;
        }
        if (("20404".equals(numeric) || "45403".equals(numeric)) && phone.getPhoneType() == 2 && ctName != null && ctName.equals(simCarrierName)) {
            Rlog.d(LOG_TAG, "Special handle for roaming case!");
            getFromResource = true;
            mvnoOperName = null;
        }
        if (mvnoOperName == null && desireLongName) {
            if (numeric.equals("46000") || numeric.equals("46002") || numeric.equals("46004") || numeric.equals("46007") || numeric.equals("46008")) {
                return context.getText(134545437).toString();
            }
            if (numeric.equals("46001") || numeric.equals("46009") || numeric.equals("45407")) {
                return context.getText(134545438).toString();
            }
            if (numeric.equals("46003") || numeric.equals("46011") || getFromResource) {
                return context.getText(134545507).toString();
            }
            if (numeric.equals("46601")) {
                return context.getText(134545439).toString();
            }
            if (numeric.equals("46692")) {
                return context.getText(134545440).toString();
            }
            if (numeric.equals("46697")) {
                return context.getText(134545441).toString();
            }
            if (numeric.equals("99998")) {
                return context.getText(134545442).toString();
            }
            if (numeric.equals("99999")) {
                return context.getText(134545443).toString();
            }
            if (containsCarrier(numeric)) {
                return getSpn(numeric);
            }
            Rlog.w(LOG_TAG, "Can't find long operator name for " + numeric);
            return operName;
        }
        if (mvnoOperName != null || desireLongName) {
            return operName;
        }
        if (numeric.equals("46000") || numeric.equals("46002") || numeric.equals("46004") || numeric.equals("46007") || numeric.equals("46008")) {
            return context.getText(134545444).toString();
        }
        if (numeric.equals("46001") || numeric.equals("46009") || numeric.equals("45407")) {
            return context.getText(134545445).toString();
        }
        if (numeric.equals("46003") || numeric.equals("46011") || getFromResource) {
            return context.getText(134545508).toString();
        }
        if (numeric.equals("46601")) {
            return context.getText(134545446).toString();
        }
        if (numeric.equals("46692")) {
            return context.getText(134545447).toString();
        }
        if (numeric.equals("46697")) {
            return context.getText(134545448).toString();
        }
        if (numeric.equals("99997")) {
            return context.getText(134545449).toString();
        }
        if (numeric.equals("99999")) {
            return context.getText(134545450).toString();
        }
        Rlog.w(LOG_TAG, "Can't find short operator name for " + numeric);
        return operName;
    }

    public boolean containsCarrierEx(String carrier) {
        return containsCarrier(carrier);
    }

    public String getSpnEx(String carrier) {
        return getSpn(carrier);
    }
}
