package com.android.internal.telephony;

import android.R;
import android.app.AppGlobals;
import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.res.XmlResourceParser;
import android.database.ContentObserver;
import android.os.Binder;
import android.os.Handler;
import android.os.RemoteException;
import android.os.UserHandle;
import android.provider.Settings;
import android.telephony.PhoneNumberUtils;
import android.telephony.Rlog;
import android.util.AtomicFile;
import android.util.Xml;
import com.android.internal.util.FastXmlSerializer;
import com.android.internal.util.XmlUtils;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Pattern;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

public class SmsUsageMonitor {
    private static final String ATTR_COUNTRY = "country";
    private static final String ATTR_FREE = "free";
    private static final String ATTR_PACKAGE_NAME = "name";
    private static final String ATTR_PACKAGE_SMS_POLICY = "sms-policy";
    private static final String ATTR_PATTERN = "pattern";
    private static final String ATTR_PREMIUM = "premium";
    private static final String ATTR_STANDARD = "standard";
    static final int CATEGORY_FREE_SHORT_CODE = 1;
    static final int CATEGORY_NOT_SHORT_CODE = 0;
    public static final int CATEGORY_POSSIBLE_PREMIUM_SHORT_CODE = 3;
    static final int CATEGORY_PREMIUM_SHORT_CODE = 4;
    static final int CATEGORY_STANDARD_SHORT_CODE = 2;
    private static final boolean DBG = false;
    private static final int DEFAULT_SMS_CHECK_PERIOD = 60000;
    private static final int DEFAULT_SMS_MAX_COUNT = 30;
    public static final int PREMIUM_SMS_PERMISSION_ALWAYS_ALLOW = 3;
    public static final int PREMIUM_SMS_PERMISSION_ASK_USER = 1;
    public static final int PREMIUM_SMS_PERMISSION_NEVER_ALLOW = 2;
    public static final int PREMIUM_SMS_PERMISSION_UNKNOWN = 0;
    private static final String SHORT_CODE_PATH = "/data/misc/sms/codes";
    private static final String SMS_POLICY_FILE_DIRECTORY = "/data/misc/sms";
    private static final String SMS_POLICY_FILE_NAME = "premium_sms_policy.xml";
    private static final String TAG = "SmsUsageMonitor";
    private static final String TAG_PACKAGE = "package";
    private static final String TAG_SHORTCODE = "shortcode";
    private static final String TAG_SHORTCODES = "shortcodes";
    private static final String TAG_SMS_POLICY_BODY = "premium-sms-policy";
    private static final boolean VDBG = false;
    private final int mCheckPeriod;
    private final Context mContext;
    private String mCurrentCountry;
    private ShortCodePatternMatcher mCurrentPatternMatcher;
    private final int mMaxAllowed;
    private AtomicFile mPolicyFile;
    private final SettingsObserverHandler mSettingsObserverHandler;
    private final HashMap<String, ArrayList<Long>> mSmsStamp = new HashMap<>();
    private final AtomicBoolean mCheckEnabled = new AtomicBoolean(true);
    private final File mPatternFile = new File(SHORT_CODE_PATH);
    private long mPatternFileLastModified = 0;
    private final HashMap<String, Integer> mPremiumSmsPolicy = new HashMap<>();

    public static int mergeShortCodeCategories(int i, int i2) {
        return i > i2 ? i : i2;
    }

    private static final class ShortCodePatternMatcher {
        private final Pattern mFreeShortCodePattern;
        private final Pattern mPremiumShortCodePattern;
        private final Pattern mShortCodePattern;
        private final Pattern mStandardShortCodePattern;

        ShortCodePatternMatcher(String str, String str2, String str3, String str4) {
            Pattern patternCompile;
            Pattern patternCompile2;
            this.mShortCodePattern = str != null ? Pattern.compile(str) : null;
            if (str2 == null) {
                patternCompile = null;
            } else {
                patternCompile = Pattern.compile(str2);
            }
            this.mPremiumShortCodePattern = patternCompile;
            if (str3 == null) {
                patternCompile2 = null;
            } else {
                patternCompile2 = Pattern.compile(str3);
            }
            this.mFreeShortCodePattern = patternCompile2;
            this.mStandardShortCodePattern = str4 != null ? Pattern.compile(str4) : null;
        }

        int getNumberCategory(String str) {
            if (this.mFreeShortCodePattern != null && this.mFreeShortCodePattern.matcher(str).matches()) {
                return 1;
            }
            if (this.mStandardShortCodePattern != null && this.mStandardShortCodePattern.matcher(str).matches()) {
                return 2;
            }
            if (this.mPremiumShortCodePattern != null && this.mPremiumShortCodePattern.matcher(str).matches()) {
                return 4;
            }
            if (this.mShortCodePattern != null && this.mShortCodePattern.matcher(str).matches()) {
                return 3;
            }
            return 0;
        }
    }

    private static class SettingsObserver extends ContentObserver {
        private final Context mContext;
        private final AtomicBoolean mEnabled;

        SettingsObserver(Handler handler, Context context, AtomicBoolean atomicBoolean) {
            super(handler);
            this.mContext = context;
            this.mEnabled = atomicBoolean;
            onChange(false);
        }

        @Override
        public void onChange(boolean z) {
            this.mEnabled.set(Settings.Global.getInt(this.mContext.getContentResolver(), "sms_short_code_confirmation", 1) != 0);
        }
    }

    private static class SettingsObserverHandler extends Handler {
        SettingsObserverHandler(Context context, AtomicBoolean atomicBoolean) {
            context.getContentResolver().registerContentObserver(Settings.Global.getUriFor("sms_short_code_confirmation"), false, new SettingsObserver(this, context, atomicBoolean));
        }
    }

    public SmsUsageMonitor(Context context) {
        this.mContext = context;
        ContentResolver contentResolver = context.getContentResolver();
        this.mMaxAllowed = Settings.Global.getInt(contentResolver, "sms_outgoing_check_max_count", 30);
        this.mCheckPeriod = Settings.Global.getInt(contentResolver, "sms_outgoing_check_interval_ms", 60000);
        this.mSettingsObserverHandler = new SettingsObserverHandler(this.mContext, this.mCheckEnabled);
        loadPremiumSmsPolicyDb();
    }

    private ShortCodePatternMatcher getPatternMatcherFromFile(String str) throws Throwable {
        FileReader fileReader;
        FileReader fileReader2 = null;
        try {
            try {
                fileReader = new FileReader(this.mPatternFile);
            } catch (Throwable th) {
                th = th;
                this.mPatternFileLastModified = this.mPatternFile.lastModified();
                if (0 != 0) {
                    try {
                        fileReader2.close();
                    } catch (IOException e) {
                    }
                }
                throw th;
            }
        } catch (FileNotFoundException e2) {
            fileReader = null;
        } catch (XmlPullParserException e3) {
            e = e3;
            fileReader = null;
        } catch (Throwable th2) {
            th = th2;
            this.mPatternFileLastModified = this.mPatternFile.lastModified();
            if (0 != 0) {
            }
            throw th;
        }
        try {
            XmlPullParser xmlPullParserNewPullParser = Xml.newPullParser();
            xmlPullParserNewPullParser.setInput(fileReader);
            ShortCodePatternMatcher patternMatcherFromXmlParser = getPatternMatcherFromXmlParser(xmlPullParserNewPullParser, str);
            this.mPatternFileLastModified = this.mPatternFile.lastModified();
            try {
                fileReader.close();
            } catch (IOException e4) {
            }
            return patternMatcherFromXmlParser;
        } catch (FileNotFoundException e5) {
            Rlog.e(TAG, "Short Code Pattern File not found");
            this.mPatternFileLastModified = this.mPatternFile.lastModified();
            if (fileReader != null) {
                try {
                    fileReader.close();
                } catch (IOException e6) {
                }
            }
            return null;
        } catch (XmlPullParserException e7) {
            e = e7;
            Rlog.e(TAG, "XML parser exception reading short code pattern file", e);
            this.mPatternFileLastModified = this.mPatternFile.lastModified();
            if (fileReader != null) {
            }
            return null;
        }
    }

    private ShortCodePatternMatcher getPatternMatcherFromResource(String str) throws Throwable {
        XmlResourceParser xmlResourceParser = null;
        try {
            XmlResourceParser xml = this.mContext.getResources().getXml(R.xml.password_kbd_symbols);
            try {
                ShortCodePatternMatcher patternMatcherFromXmlParser = getPatternMatcherFromXmlParser(xml, str);
                if (xml != null) {
                    xml.close();
                }
                return patternMatcherFromXmlParser;
            } catch (Throwable th) {
                th = th;
                xmlResourceParser = xml;
                if (xmlResourceParser != null) {
                    xmlResourceParser.close();
                }
                throw th;
            }
        } catch (Throwable th2) {
            th = th2;
        }
    }

    private ShortCodePatternMatcher getPatternMatcherFromXmlParser(XmlPullParser xmlPullParser, String str) {
        try {
            XmlUtils.beginDocument(xmlPullParser, TAG_SHORTCODES);
            while (true) {
                XmlUtils.nextElement(xmlPullParser);
                String name = xmlPullParser.getName();
                if (name == null) {
                    Rlog.e(TAG, "Parsing pattern data found null");
                    break;
                }
                if (name.equals(TAG_SHORTCODE)) {
                    if (str.equals(xmlPullParser.getAttributeValue(null, ATTR_COUNTRY))) {
                        return new ShortCodePatternMatcher(xmlPullParser.getAttributeValue(null, ATTR_PATTERN), xmlPullParser.getAttributeValue(null, ATTR_PREMIUM), xmlPullParser.getAttributeValue(null, ATTR_FREE), xmlPullParser.getAttributeValue(null, ATTR_STANDARD));
                    }
                } else {
                    Rlog.e(TAG, "Error: skipping unknown XML tag " + name);
                }
            }
        } catch (IOException e) {
            Rlog.e(TAG, "I/O exception reading short code patterns", e);
        } catch (XmlPullParserException e2) {
            Rlog.e(TAG, "XML parser exception reading short code patterns", e2);
        }
        return null;
    }

    void dispose() {
        this.mSmsStamp.clear();
    }

    public boolean check(String str, int i) {
        boolean zIsUnderLimit;
        synchronized (this.mSmsStamp) {
            removeExpiredTimestamps();
            ArrayList<Long> arrayList = this.mSmsStamp.get(str);
            if (arrayList == null) {
                arrayList = new ArrayList<>();
                this.mSmsStamp.put(str, arrayList);
            }
            zIsUnderLimit = isUnderLimit(arrayList, i);
        }
        return zIsUnderLimit;
    }

    public int checkDestination(String str, String str2) {
        synchronized (this.mSettingsObserverHandler) {
            if (PhoneNumberUtils.isEmergencyNumber(str, str2)) {
                return 0;
            }
            if (!this.mCheckEnabled.get()) {
                return 0;
            }
            if (str2 != null && (this.mCurrentCountry == null || !str2.equals(this.mCurrentCountry) || this.mPatternFile.lastModified() != this.mPatternFileLastModified)) {
                if (this.mPatternFile.exists()) {
                    this.mCurrentPatternMatcher = getPatternMatcherFromFile(str2);
                } else {
                    this.mCurrentPatternMatcher = getPatternMatcherFromResource(str2);
                }
                this.mCurrentCountry = str2;
            }
            if (this.mCurrentPatternMatcher != null) {
                return this.mCurrentPatternMatcher.getNumberCategory(str);
            }
            Rlog.e(TAG, "No patterns for \"" + str2 + "\": using generic short code rule");
            return str.length() <= 5 ? 3 : 0;
        }
    }

    private void loadPremiumSmsPolicyDb() {
        Throwable th;
        FileInputStream fileInputStreamOpenRead;
        XmlPullParserException e;
        NumberFormatException e2;
        IOException e3;
        synchronized (this.mPremiumSmsPolicy) {
            if (this.mPolicyFile == null) {
                ?? atomicFile = new AtomicFile(new File(new File(SMS_POLICY_FILE_DIRECTORY), SMS_POLICY_FILE_NAME));
                this.mPolicyFile = atomicFile;
                this.mPremiumSmsPolicy.clear();
                try {
                    try {
                        try {
                            fileInputStreamOpenRead = this.mPolicyFile.openRead();
                            try {
                                try {
                                    XmlPullParser xmlPullParserNewPullParser = Xml.newPullParser();
                                    xmlPullParserNewPullParser.setInput(fileInputStreamOpenRead, StandardCharsets.UTF_8.name());
                                    XmlUtils.beginDocument(xmlPullParserNewPullParser, TAG_SMS_POLICY_BODY);
                                    while (true) {
                                        XmlUtils.nextElement(xmlPullParserNewPullParser);
                                        String name = xmlPullParserNewPullParser.getName();
                                        if (name == null) {
                                            break;
                                        }
                                        if (name.equals(TAG_PACKAGE)) {
                                            String attributeValue = xmlPullParserNewPullParser.getAttributeValue(null, ATTR_PACKAGE_NAME);
                                            String attributeValue2 = xmlPullParserNewPullParser.getAttributeValue(null, ATTR_PACKAGE_SMS_POLICY);
                                            if (attributeValue == null) {
                                                Rlog.e(TAG, "Error: missing package name attribute");
                                            } else if (attributeValue2 == null) {
                                                Rlog.e(TAG, "Error: missing package policy attribute");
                                            } else {
                                                try {
                                                    this.mPremiumSmsPolicy.put(attributeValue, Integer.valueOf(Integer.parseInt(attributeValue2)));
                                                } catch (NumberFormatException e4) {
                                                    Rlog.e(TAG, "Error: non-numeric policy type " + attributeValue2);
                                                }
                                            }
                                        } else {
                                            Rlog.e(TAG, "Error: skipping unknown XML tag " + name);
                                        }
                                    }
                                } catch (NumberFormatException e5) {
                                    e2 = e5;
                                    Rlog.e(TAG, "Unable to parse premium SMS policy database", e2);
                                    if (fileInputStreamOpenRead != null) {
                                        fileInputStreamOpenRead.close();
                                    }
                                }
                            } catch (FileNotFoundException e6) {
                                if (fileInputStreamOpenRead != null) {
                                    fileInputStreamOpenRead.close();
                                }
                            } catch (IOException e7) {
                                e3 = e7;
                                Rlog.e(TAG, "Unable to read premium SMS policy database", e3);
                                if (fileInputStreamOpenRead != null) {
                                    fileInputStreamOpenRead.close();
                                }
                            } catch (XmlPullParserException e8) {
                                e = e8;
                                Rlog.e(TAG, "Unable to parse premium SMS policy database", e);
                                if (fileInputStreamOpenRead != null) {
                                    fileInputStreamOpenRead.close();
                                }
                            }
                        } catch (Throwable th2) {
                            th = th2;
                            if (atomicFile != 0) {
                                try {
                                    atomicFile.close();
                                } catch (IOException e9) {
                                }
                            }
                            throw th;
                        }
                    } catch (FileNotFoundException e10) {
                        fileInputStreamOpenRead = null;
                    } catch (IOException e11) {
                        fileInputStreamOpenRead = null;
                        e3 = e11;
                    } catch (NumberFormatException e12) {
                        fileInputStreamOpenRead = null;
                        e2 = e12;
                    } catch (XmlPullParserException e13) {
                        fileInputStreamOpenRead = null;
                        e = e13;
                    } catch (Throwable th3) {
                        atomicFile = 0;
                        th = th3;
                        if (atomicFile != 0) {
                        }
                        throw th;
                    }
                    if (fileInputStreamOpenRead != null) {
                        fileInputStreamOpenRead.close();
                    }
                } catch (IOException e14) {
                }
            }
        }
    }

    private void writePremiumSmsPolicyDb() {
        FileOutputStream fileOutputStreamStartWrite;
        IOException e;
        synchronized (this.mPremiumSmsPolicy) {
            try {
                fileOutputStreamStartWrite = this.mPolicyFile.startWrite();
            } catch (IOException e2) {
                fileOutputStreamStartWrite = null;
                e = e2;
            }
            try {
                FastXmlSerializer fastXmlSerializer = new FastXmlSerializer();
                fastXmlSerializer.setOutput(fileOutputStreamStartWrite, StandardCharsets.UTF_8.name());
                fastXmlSerializer.startDocument(null, true);
                fastXmlSerializer.startTag(null, TAG_SMS_POLICY_BODY);
                for (Map.Entry<String, Integer> entry : this.mPremiumSmsPolicy.entrySet()) {
                    fastXmlSerializer.startTag(null, TAG_PACKAGE);
                    fastXmlSerializer.attribute(null, ATTR_PACKAGE_NAME, entry.getKey());
                    fastXmlSerializer.attribute(null, ATTR_PACKAGE_SMS_POLICY, entry.getValue().toString());
                    fastXmlSerializer.endTag(null, TAG_PACKAGE);
                }
                fastXmlSerializer.endTag(null, TAG_SMS_POLICY_BODY);
                fastXmlSerializer.endDocument();
                this.mPolicyFile.finishWrite(fileOutputStreamStartWrite);
            } catch (IOException e3) {
                e = e3;
                Rlog.e(TAG, "Unable to write premium SMS policy database", e);
                if (fileOutputStreamStartWrite != null) {
                    this.mPolicyFile.failWrite(fileOutputStreamStartWrite);
                }
            }
        }
    }

    public int getPremiumSmsPermission(String str) {
        checkCallerIsSystemOrPhoneOrSameApp(str);
        synchronized (this.mPremiumSmsPolicy) {
            Integer num = this.mPremiumSmsPolicy.get(str);
            if (num == null) {
                return 0;
            }
            return num.intValue();
        }
    }

    public void setPremiumSmsPermission(String str, int i) {
        checkCallerIsSystemOrPhoneApp();
        if (i < 1 || i > 3) {
            throw new IllegalArgumentException("invalid SMS permission type " + i);
        }
        synchronized (this.mPremiumSmsPolicy) {
            this.mPremiumSmsPolicy.put(str, Integer.valueOf(i));
        }
        new Thread(new Runnable() {
            @Override
            public void run() {
                SmsUsageMonitor.this.writePremiumSmsPolicyDb();
            }
        }).start();
    }

    private static void checkCallerIsSystemOrPhoneOrSameApp(String str) {
        int callingUid = Binder.getCallingUid();
        int appId = UserHandle.getAppId(callingUid);
        if (appId == 1000 || appId == 1001 || callingUid == 0) {
            return;
        }
        try {
            ApplicationInfo applicationInfo = AppGlobals.getPackageManager().getApplicationInfo(str, 0, UserHandle.getCallingUserId());
            if (!UserHandle.isSameApp(applicationInfo.uid, callingUid)) {
                throw new SecurityException("Calling uid " + callingUid + " gave package" + str + " which is owned by uid " + applicationInfo.uid);
            }
        } catch (RemoteException e) {
            throw new SecurityException("Unknown package " + str + "\n" + e);
        }
    }

    private static void checkCallerIsSystemOrPhoneApp() {
        int callingUid = Binder.getCallingUid();
        int appId = UserHandle.getAppId(callingUid);
        if (appId == 1000 || appId == 1001 || callingUid == 0) {
            return;
        }
        throw new SecurityException("Disallowed call for uid " + callingUid);
    }

    private void removeExpiredTimestamps() {
        long jCurrentTimeMillis = System.currentTimeMillis() - ((long) this.mCheckPeriod);
        synchronized (this.mSmsStamp) {
            Iterator<Map.Entry<String, ArrayList<Long>>> it = this.mSmsStamp.entrySet().iterator();
            while (it.hasNext()) {
                ArrayList<Long> value = it.next().getValue();
                if (value.isEmpty() || value.get(value.size() - 1).longValue() < jCurrentTimeMillis) {
                    it.remove();
                }
            }
        }
    }

    private boolean isUnderLimit(ArrayList<Long> arrayList, int i) {
        int i2;
        Long lValueOf = Long.valueOf(System.currentTimeMillis());
        long jLongValue = lValueOf.longValue() - ((long) this.mCheckPeriod);
        while (true) {
            if (arrayList.isEmpty() || arrayList.get(0).longValue() >= jLongValue) {
                break;
            }
            arrayList.remove(0);
        }
        if (arrayList.size() + i > this.mMaxAllowed) {
            return false;
        }
        for (i2 = 0; i2 < i; i2++) {
            arrayList.add(lValueOf);
        }
        return true;
    }

    private static void log(String str) {
        Rlog.d(TAG, str);
    }
}
