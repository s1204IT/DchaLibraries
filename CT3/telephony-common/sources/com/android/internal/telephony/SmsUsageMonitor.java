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
    static final int CATEGORY_POSSIBLE_PREMIUM_SHORT_CODE = 3;
    static final int CATEGORY_PREMIUM_SHORT_CODE = 4;
    static final int CATEGORY_STANDARD_SHORT_CODE = 2;
    private static final boolean DBG = true;
    private static final int DEFAULT_SMS_CHECK_PERIOD = 60000;
    private static final int DEFAULT_SMS_MAX_COUNT = 30;
    public static final int PREMIUM_SMS_PERMISSION_ALWAYS_ALLOW = 3;
    public static final int PREMIUM_SMS_PERMISSION_ASK_USER = 1;
    public static final int PREMIUM_SMS_PERMISSION_NEVER_ALLOW = 2;
    public static final int PREMIUM_SMS_PERMISSION_UNKNOWN = 0;
    private static final String SHORT_CODE_PATH = "/data/misc/sms/codes";
    private static final String[] SKIP_SEND_LIMIT_PACKAGES = {"com.android.mms", "com.mediatek.autotest"};
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

    public static int mergeShortCodeCategories(int type1, int type2) {
        return type1 > type2 ? type1 : type2;
    }

    private static final class ShortCodePatternMatcher {
        private final Pattern mFreeShortCodePattern;
        private final Pattern mPremiumShortCodePattern;
        private final Pattern mShortCodePattern;
        private final Pattern mStandardShortCodePattern;

        ShortCodePatternMatcher(String shortCodeRegex, String premiumShortCodeRegex, String freeShortCodeRegex, String standardShortCodeRegex) {
            this.mShortCodePattern = shortCodeRegex != null ? Pattern.compile(shortCodeRegex) : null;
            this.mPremiumShortCodePattern = premiumShortCodeRegex != null ? Pattern.compile(premiumShortCodeRegex) : null;
            this.mFreeShortCodePattern = freeShortCodeRegex != null ? Pattern.compile(freeShortCodeRegex) : null;
            this.mStandardShortCodePattern = standardShortCodeRegex != null ? Pattern.compile(standardShortCodeRegex) : null;
        }

        int getNumberCategory(String phoneNumber) {
            if (this.mFreeShortCodePattern != null && this.mFreeShortCodePattern.matcher(phoneNumber).matches()) {
                return 1;
            }
            if (this.mStandardShortCodePattern != null && this.mStandardShortCodePattern.matcher(phoneNumber).matches()) {
                return 2;
            }
            if (this.mPremiumShortCodePattern != null && this.mPremiumShortCodePattern.matcher(phoneNumber).matches()) {
                return 4;
            }
            if (this.mShortCodePattern != null && this.mShortCodePattern.matcher(phoneNumber).matches()) {
                return 3;
            }
            return 0;
        }
    }

    private static class SettingsObserver extends ContentObserver {
        private final Context mContext;
        private final AtomicBoolean mEnabled;

        SettingsObserver(Handler handler, Context context, AtomicBoolean enabled) {
            super(handler);
            this.mContext = context;
            this.mEnabled = enabled;
            onChange(false);
        }

        @Override
        public void onChange(boolean selfChange) {
            this.mEnabled.set(Settings.Global.getInt(this.mContext.getContentResolver(), "sms_short_code_confirmation", 1) != 0);
        }
    }

    private static class SettingsObserverHandler extends Handler {
        SettingsObserverHandler(Context context, AtomicBoolean enabled) {
            ContentResolver resolver = context.getContentResolver();
            ContentObserver globalObserver = new SettingsObserver(this, context, enabled);
            resolver.registerContentObserver(Settings.Global.getUriFor("sms_short_code_confirmation"), false, globalObserver);
        }
    }

    public SmsUsageMonitor(Context context) {
        this.mContext = context;
        ContentResolver resolver = context.getContentResolver();
        this.mMaxAllowed = Settings.Global.getInt(resolver, "sms_outgoing_check_max_count", 30);
        this.mCheckPeriod = Settings.Global.getInt(resolver, "sms_outgoing_check_interval_ms", 60000);
        this.mSettingsObserverHandler = new SettingsObserverHandler(this.mContext, this.mCheckEnabled);
        loadPremiumSmsPolicyDb();
    }

    private ShortCodePatternMatcher getPatternMatcherFromFile(String country) throws Throwable {
        FileReader patternReader;
        FileReader patternReader2 = null;
        try {
            try {
                patternReader = new FileReader(this.mPatternFile);
            } catch (Throwable th) {
                th = th;
            }
        } catch (FileNotFoundException e) {
        } catch (XmlPullParserException e2) {
            e = e2;
        }
        try {
            XmlPullParser parser = Xml.newPullParser();
            parser.setInput(patternReader);
            ShortCodePatternMatcher patternMatcherFromXmlParser = getPatternMatcherFromXmlParser(parser, country);
            this.mPatternFileLastModified = this.mPatternFile.lastModified();
            if (patternReader != null) {
                try {
                    patternReader.close();
                } catch (IOException e3) {
                }
            }
            return patternMatcherFromXmlParser;
        } catch (FileNotFoundException e4) {
            patternReader2 = patternReader;
            Rlog.e(TAG, "Short Code Pattern File not found");
            this.mPatternFileLastModified = this.mPatternFile.lastModified();
            if (patternReader2 != null) {
                try {
                    patternReader2.close();
                } catch (IOException e5) {
                }
            }
            return null;
        } catch (XmlPullParserException e6) {
            e = e6;
            patternReader2 = patternReader;
            Rlog.e(TAG, "XML parser exception reading short code pattern file", e);
            this.mPatternFileLastModified = this.mPatternFile.lastModified();
            if (patternReader2 != null) {
                try {
                    patternReader2.close();
                } catch (IOException e7) {
                }
            }
            return null;
        } catch (Throwable th2) {
            th = th2;
            patternReader2 = patternReader;
            this.mPatternFileLastModified = this.mPatternFile.lastModified();
            if (patternReader2 != null) {
                try {
                    patternReader2.close();
                } catch (IOException e8) {
                }
            }
            throw th;
        }
    }

    private ShortCodePatternMatcher getPatternMatcherFromResource(String country) {
        XmlResourceParser parser = null;
        try {
            parser = this.mContext.getResources().getXml(R.bool.autofill_dialog_horizontal_space_included);
            return getPatternMatcherFromXmlParser(parser, country);
        } finally {
            if (parser != null) {
                parser.close();
            }
        }
    }

    private ShortCodePatternMatcher getPatternMatcherFromXmlParser(XmlPullParser parser, String country) {
        try {
            XmlUtils.beginDocument(parser, TAG_SHORTCODES);
            while (true) {
                XmlUtils.nextElement(parser);
                String element = parser.getName();
                if (element == null) {
                    Rlog.e(TAG, "Parsing pattern data found null");
                    break;
                }
                if (element.equals(TAG_SHORTCODE)) {
                    String currentCountry = parser.getAttributeValue(null, ATTR_COUNTRY);
                    if (country.equals(currentCountry)) {
                        String pattern = parser.getAttributeValue(null, ATTR_PATTERN);
                        String premium = parser.getAttributeValue(null, ATTR_PREMIUM);
                        String free = parser.getAttributeValue(null, ATTR_FREE);
                        String standard = parser.getAttributeValue(null, ATTR_STANDARD);
                        return new ShortCodePatternMatcher(pattern, premium, free, standard);
                    }
                } else {
                    Rlog.e(TAG, "Error: skipping unknown XML tag " + element);
                }
            }
        } catch (IOException e) {
            Rlog.e(TAG, "I/O exception reading short code patterns", e);
        } catch (XmlPullParserException e2) {
            Rlog.e(TAG, "XML parser exception reading short code patterns", e2);
        }
        Rlog.d(TAG, "Country (" + country + ") not found");
        return null;
    }

    void dispose() {
        this.mSmsStamp.clear();
    }

    public boolean check(String appName, int smsWaiting) {
        boolean zIsUnderLimit;
        for (String name : SKIP_SEND_LIMIT_PACKAGES) {
            if (appName.equals(name)) {
                return true;
            }
        }
        synchronized (this.mSmsStamp) {
            removeExpiredTimestamps();
            ArrayList<Long> sentList = this.mSmsStamp.get(appName);
            if (sentList == null) {
                sentList = new ArrayList<>();
                this.mSmsStamp.put(appName, sentList);
            }
            zIsUnderLimit = isUnderLimit(sentList, smsWaiting);
        }
        return zIsUnderLimit;
    }

    public int checkDestination(String destAddress, String countryIso) {
        synchronized (this.mSettingsObserverHandler) {
            if (PhoneNumberUtils.isEmergencyNumber(destAddress, countryIso)) {
                Rlog.d(TAG, "isEmergencyNumber");
                return 0;
            }
            if (!this.mCheckEnabled.get()) {
                Rlog.e(TAG, "check disabled");
                return 0;
            }
            if (countryIso != null && (this.mCurrentCountry == null || !countryIso.equals(this.mCurrentCountry) || this.mPatternFile.lastModified() != this.mPatternFileLastModified)) {
                if (this.mPatternFile.exists()) {
                    Rlog.d(TAG, "Loading SMS Short Code patterns from file");
                    this.mCurrentPatternMatcher = getPatternMatcherFromFile(countryIso);
                } else {
                    Rlog.d(TAG, "Loading SMS Short Code patterns from resource");
                    this.mCurrentPatternMatcher = getPatternMatcherFromResource(countryIso);
                }
                this.mCurrentCountry = countryIso;
            }
            if (this.mCurrentPatternMatcher != null) {
                return this.mCurrentPatternMatcher.getNumberCategory(destAddress);
            }
            Rlog.e(TAG, "No patterns for \"" + countryIso + "\": using generic short code rule");
            return destAddress.length() <= 5 ? 3 : 0;
        }
    }

    private void loadPremiumSmsPolicyDb() {
        synchronized (this.mPremiumSmsPolicy) {
            if (this.mPolicyFile == null) {
                File dir = new File(SMS_POLICY_FILE_DIRECTORY);
                this.mPolicyFile = new AtomicFile(new File(dir, SMS_POLICY_FILE_NAME));
                this.mPremiumSmsPolicy.clear();
                FileInputStream infile = null;
                try {
                    try {
                        try {
                            infile = this.mPolicyFile.openRead();
                            XmlPullParser parser = Xml.newPullParser();
                            parser.setInput(infile, StandardCharsets.UTF_8.name());
                            XmlUtils.beginDocument(parser, TAG_SMS_POLICY_BODY);
                            while (true) {
                                XmlUtils.nextElement(parser);
                                String element = parser.getName();
                                if (element == null) {
                                    break;
                                }
                                if (element.equals("package")) {
                                    String packageName = parser.getAttributeValue(null, "name");
                                    String policy = parser.getAttributeValue(null, ATTR_PACKAGE_SMS_POLICY);
                                    if (packageName == null) {
                                        Rlog.e(TAG, "Error: missing package name attribute");
                                    } else if (policy == null) {
                                        Rlog.e(TAG, "Error: missing package policy attribute");
                                    } else {
                                        try {
                                            this.mPremiumSmsPolicy.put(packageName, Integer.valueOf(Integer.parseInt(policy)));
                                        } catch (NumberFormatException e) {
                                            Rlog.e(TAG, "Error: non-numeric policy type " + policy);
                                        }
                                    }
                                } else {
                                    Rlog.e(TAG, "Error: skipping unknown XML tag " + element);
                                }
                            }
                            if (infile != null) {
                                try {
                                    infile.close();
                                } catch (IOException e2) {
                                }
                            }
                        } catch (NumberFormatException e3) {
                            Rlog.e(TAG, "Unable to parse premium SMS policy database", e3);
                            if (infile != null) {
                                try {
                                    infile.close();
                                } catch (IOException e4) {
                                }
                            }
                        }
                    } catch (FileNotFoundException e5) {
                        if (infile != null) {
                            try {
                                infile.close();
                            } catch (IOException e6) {
                            }
                        }
                    } catch (IOException e7) {
                        Rlog.e(TAG, "Unable to read premium SMS policy database", e7);
                        if (infile != null) {
                            try {
                                infile.close();
                            } catch (IOException e8) {
                            }
                        }
                    } catch (XmlPullParserException e9) {
                        Rlog.e(TAG, "Unable to parse premium SMS policy database", e9);
                        if (infile != null) {
                            try {
                                infile.close();
                            } catch (IOException e10) {
                            }
                        }
                    }
                } catch (Throwable th) {
                    if (infile != null) {
                        try {
                            infile.close();
                        } catch (IOException e11) {
                        }
                    }
                    throw th;
                }
            }
        }
    }

    private void writePremiumSmsPolicyDb() {
        synchronized (this.mPremiumSmsPolicy) {
            FileOutputStream outfile = null;
            try {
                outfile = this.mPolicyFile.startWrite();
                FastXmlSerializer fastXmlSerializer = new FastXmlSerializer();
                fastXmlSerializer.setOutput(outfile, StandardCharsets.UTF_8.name());
                fastXmlSerializer.startDocument(null, true);
                fastXmlSerializer.startTag(null, TAG_SMS_POLICY_BODY);
                for (Map.Entry<String, Integer> policy : this.mPremiumSmsPolicy.entrySet()) {
                    fastXmlSerializer.startTag(null, "package");
                    fastXmlSerializer.attribute(null, "name", policy.getKey());
                    fastXmlSerializer.attribute(null, ATTR_PACKAGE_SMS_POLICY, policy.getValue().toString());
                    fastXmlSerializer.endTag(null, "package");
                }
                fastXmlSerializer.endTag(null, TAG_SMS_POLICY_BODY);
                fastXmlSerializer.endDocument();
                this.mPolicyFile.finishWrite(outfile);
            } catch (IOException e) {
                Rlog.e(TAG, "Unable to write premium SMS policy database", e);
                if (outfile != null) {
                    this.mPolicyFile.failWrite(outfile);
                }
            }
        }
    }

    public int getPremiumSmsPermission(String packageName) {
        checkCallerIsSystemOrPhoneOrSameApp(packageName);
        synchronized (this.mPremiumSmsPolicy) {
            Integer policy = this.mPremiumSmsPolicy.get(packageName);
            if (policy == null) {
                return 0;
            }
            return policy.intValue();
        }
    }

    public void setPremiumSmsPermission(String packageName, int permission) {
        checkCallerIsSystemOrPhoneApp();
        if (permission < 1 || permission > 3) {
            throw new IllegalArgumentException("invalid SMS permission type " + permission);
        }
        synchronized (this.mPremiumSmsPolicy) {
            this.mPremiumSmsPolicy.put(packageName, Integer.valueOf(permission));
        }
        new Thread(new Runnable() {
            @Override
            public void run() {
                SmsUsageMonitor.this.writePremiumSmsPolicyDb();
            }
        }).start();
    }

    private static void checkCallerIsSystemOrPhoneOrSameApp(String pkg) {
        int uid = Binder.getCallingUid();
        int appId = UserHandle.getAppId(uid);
        if (appId == 1000 || appId == 1001 || uid == 0) {
            return;
        }
        try {
            ApplicationInfo ai = AppGlobals.getPackageManager().getApplicationInfo(pkg, 0, UserHandle.getCallingUserId());
            if (UserHandle.isSameApp(ai.uid, uid)) {
            } else {
                throw new SecurityException("Calling uid " + uid + " gave package" + pkg + " which is owned by uid " + ai.uid);
            }
        } catch (RemoteException re) {
            throw new SecurityException("Unknown package " + pkg + "\n" + re);
        }
    }

    private static void checkCallerIsSystemOrPhoneApp() {
        int uid = Binder.getCallingUid();
        int appId = UserHandle.getAppId(uid);
        if (appId == 1000 || appId == 1001 || uid == 0) {
        } else {
            throw new SecurityException("Disallowed call for uid " + uid);
        }
    }

    private void removeExpiredTimestamps() {
        long beginCheckPeriod = System.currentTimeMillis() - ((long) this.mCheckPeriod);
        synchronized (this.mSmsStamp) {
            Iterator<Map.Entry<String, ArrayList<Long>>> iter = this.mSmsStamp.entrySet().iterator();
            while (iter.hasNext()) {
                Map.Entry<String, ArrayList<Long>> entry = iter.next();
                ArrayList<Long> oldList = entry.getValue();
                if (oldList.isEmpty() || oldList.get(oldList.size() - 1).longValue() < beginCheckPeriod) {
                    iter.remove();
                }
            }
        }
    }

    private boolean isUnderLimit(ArrayList<Long> sent, int smsWaiting) {
        Long ct = Long.valueOf(System.currentTimeMillis());
        long beginCheckPeriod = ct.longValue() - ((long) this.mCheckPeriod);
        while (!sent.isEmpty() && sent.get(0).longValue() < beginCheckPeriod) {
            sent.remove(0);
        }
        if (sent.size() + smsWaiting > this.mMaxAllowed) {
            return false;
        }
        for (int i = 0; i < smsWaiting; i++) {
            sent.add(ct);
        }
        return true;
    }

    private static void log(String msg) {
        Rlog.d(TAG, msg);
    }
}
