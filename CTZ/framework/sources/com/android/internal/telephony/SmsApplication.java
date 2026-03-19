package com.android.internal.telephony;

import android.Manifest;
import android.app.AppOpsManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.net.Uri;
import android.os.Binder;
import android.os.Build;
import android.os.Process;
import android.os.UserHandle;
import android.provider.Settings;
import android.provider.Telephony;
import android.telephony.Rlog;
import android.telephony.SmsManager;
import android.telephony.TelephonyManager;
import android.util.Xml;
import com.android.internal.R;
import com.android.internal.content.PackageMonitor;
import com.android.internal.logging.MetricsLogger;
import com.android.internal.util.XmlUtils;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

public final class SmsApplication {
    public static final String BLUETOOTH_PACKAGE_NAME = "com.android.bluetooth";
    private static final boolean DEBUG_MULTIUSER = false;
    static final String LOG_TAG = "SmsApplication";
    private static final String MMS_SERVICE_PACKAGE_NAME = "com.android.mms.service";
    private static final String PHONE_PACKAGE_NAME = "com.android.phone";
    private static final String SCHEME_MMS = "mms";
    private static final String SCHEME_MMSTO = "mmsto";
    private static final String SCHEME_SMS = "sms";
    private static final String SCHEME_SMSTO = "smsto";
    private static final String SMS_DB_VISITOR_PATH = "/vendor/etc/smsdbvisitor.xml";
    private static final String TELEPHONY_PROVIDER_PACKAGE_NAME = "com.android.providers.telephony";
    private static SmsPackageMonitor sSmsPackageMonitor = null;
    private static final boolean ENG = "eng".equals(Build.TYPE);
    public static ArrayList<String> mSmsDbVisitorList = null;
    private static final Object mListLock = new Object();

    public static class SmsApplicationData {
        private String mApplicationName;
        private String mMmsReceiverClass;
        public String mPackageName;
        private String mProviderChangedReceiverClass;
        private String mRespondViaMessageClass;
        private String mSendToClass;
        private String mSimFullReceiverClass;
        private String mSmsAppChangedReceiverClass;
        public String mSmsReceiverClass;
        private int mUid;

        public boolean isComplete() {
            return (this.mSmsReceiverClass == null || this.mMmsReceiverClass == null || this.mRespondViaMessageClass == null || this.mSendToClass == null) ? false : true;
        }

        public SmsApplicationData(String str, int i) {
            this.mPackageName = str;
            this.mUid = i;
        }

        public String getApplicationName(Context context) {
            if (this.mApplicationName == null) {
                PackageManager packageManager = context.getPackageManager();
                try {
                    ApplicationInfo applicationInfoAsUser = packageManager.getApplicationInfoAsUser(this.mPackageName, 0, UserHandle.getUserId(this.mUid));
                    if (applicationInfoAsUser != null) {
                        CharSequence applicationLabel = packageManager.getApplicationLabel(applicationInfoAsUser);
                        this.mApplicationName = applicationLabel != null ? applicationLabel.toString() : null;
                    }
                } catch (PackageManager.NameNotFoundException e) {
                    return null;
                }
            }
            return this.mApplicationName;
        }

        public String toString() {
            return " mPackageName: " + this.mPackageName + " mSmsReceiverClass: " + this.mSmsReceiverClass + " mMmsReceiverClass: " + this.mMmsReceiverClass + " mRespondViaMessageClass: " + this.mRespondViaMessageClass + " mSendToClass: " + this.mSendToClass + " mSmsAppChangedClass: " + this.mSmsAppChangedReceiverClass + " mProviderChangedReceiverClass: " + this.mProviderChangedReceiverClass + " mSimFullReceiverClass: " + this.mSimFullReceiverClass + " mUid: " + this.mUid;
        }
    }

    private static int getIncomingUserId(Context context) {
        int userId = context.getUserId();
        int callingUid = Binder.getCallingUid();
        if (UserHandle.getAppId(callingUid) < 10000) {
            return userId;
        }
        return UserHandle.getUserId(callingUid);
    }

    public static Collection<SmsApplicationData> getApplicationCollection(Context context) {
        int incomingUserId = getIncomingUserId(context);
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        try {
            return getApplicationCollectionInternal(context, incomingUserId);
        } finally {
            Binder.restoreCallingIdentity(jClearCallingIdentity);
        }
    }

    private static Collection<SmsApplicationData> getApplicationCollectionInternal(Context context, int i) {
        SmsApplicationData smsApplicationData;
        SmsApplicationData smsApplicationData2;
        SmsApplicationData smsApplicationData3;
        SmsApplicationData smsApplicationData4;
        SmsApplicationData smsApplicationData5;
        SmsApplicationData smsApplicationData6;
        PackageManager packageManager = context.getPackageManager();
        List<ResolveInfo> listQueryBroadcastReceiversAsUser = packageManager.queryBroadcastReceiversAsUser(new Intent(Telephony.Sms.Intents.SMS_DELIVER_ACTION), 0, i);
        HashMap map = new HashMap();
        Iterator<ResolveInfo> it = listQueryBroadcastReceiversAsUser.iterator();
        while (it.hasNext()) {
            ActivityInfo activityInfo = it.next().activityInfo;
            if (activityInfo != null && Manifest.permission.BROADCAST_SMS.equals(activityInfo.permission)) {
                String str = activityInfo.packageName;
                if (!map.containsKey(str)) {
                    SmsApplicationData smsApplicationData7 = new SmsApplicationData(str, activityInfo.applicationInfo.uid);
                    smsApplicationData7.mSmsReceiverClass = activityInfo.name;
                    map.put(str, smsApplicationData7);
                }
            }
        }
        Intent intent = new Intent(Telephony.Sms.Intents.WAP_PUSH_DELIVER_ACTION);
        intent.setDataAndType(null, "application/vnd.wap.mms-message");
        Iterator<ResolveInfo> it2 = packageManager.queryBroadcastReceiversAsUser(intent, 0, i).iterator();
        while (it2.hasNext()) {
            ActivityInfo activityInfo2 = it2.next().activityInfo;
            if (activityInfo2 != null && Manifest.permission.BROADCAST_WAP_PUSH.equals(activityInfo2.permission) && (smsApplicationData6 = (SmsApplicationData) map.get(activityInfo2.packageName)) != null) {
                smsApplicationData6.mMmsReceiverClass = activityInfo2.name;
            }
        }
        Iterator<ResolveInfo> it3 = packageManager.queryIntentServicesAsUser(new Intent(TelephonyManager.ACTION_RESPOND_VIA_MESSAGE, Uri.fromParts(SCHEME_SMSTO, "", null)), 0, i).iterator();
        while (it3.hasNext()) {
            ServiceInfo serviceInfo = it3.next().serviceInfo;
            if (serviceInfo != null && Manifest.permission.SEND_RESPOND_VIA_MESSAGE.equals(serviceInfo.permission) && (smsApplicationData5 = (SmsApplicationData) map.get(serviceInfo.packageName)) != null) {
                smsApplicationData5.mRespondViaMessageClass = serviceInfo.name;
            }
        }
        Iterator<ResolveInfo> it4 = packageManager.queryIntentActivitiesAsUser(new Intent(Intent.ACTION_SENDTO, Uri.fromParts(SCHEME_SMSTO, "", null)), 0, i).iterator();
        while (it4.hasNext()) {
            ActivityInfo activityInfo3 = it4.next().activityInfo;
            if (activityInfo3 != null && (smsApplicationData4 = (SmsApplicationData) map.get(activityInfo3.packageName)) != null) {
                smsApplicationData4.mSendToClass = activityInfo3.name;
            }
        }
        Iterator<ResolveInfo> it5 = packageManager.queryBroadcastReceiversAsUser(new Intent(Telephony.Sms.Intents.ACTION_DEFAULT_SMS_PACKAGE_CHANGED), 0, i).iterator();
        while (it5.hasNext()) {
            ActivityInfo activityInfo4 = it5.next().activityInfo;
            if (activityInfo4 != null && (smsApplicationData3 = (SmsApplicationData) map.get(activityInfo4.packageName)) != null) {
                smsApplicationData3.mSmsAppChangedReceiverClass = activityInfo4.name;
            }
        }
        Iterator<ResolveInfo> it6 = packageManager.queryBroadcastReceiversAsUser(new Intent(Telephony.Sms.Intents.ACTION_EXTERNAL_PROVIDER_CHANGE), 0, i).iterator();
        while (it6.hasNext()) {
            ActivityInfo activityInfo5 = it6.next().activityInfo;
            if (activityInfo5 != null && (smsApplicationData2 = (SmsApplicationData) map.get(activityInfo5.packageName)) != null) {
                smsApplicationData2.mProviderChangedReceiverClass = activityInfo5.name;
            }
        }
        Iterator<ResolveInfo> it7 = packageManager.queryBroadcastReceiversAsUser(new Intent(Telephony.Sms.Intents.SIM_FULL_ACTION), 0, i).iterator();
        while (it7.hasNext()) {
            ActivityInfo activityInfo6 = it7.next().activityInfo;
            if (activityInfo6 != null && (smsApplicationData = (SmsApplicationData) map.get(activityInfo6.packageName)) != null) {
                smsApplicationData.mSimFullReceiverClass = activityInfo6.name;
            }
        }
        Iterator<ResolveInfo> it8 = listQueryBroadcastReceiversAsUser.iterator();
        while (it8.hasNext()) {
            ActivityInfo activityInfo7 = it8.next().activityInfo;
            if (activityInfo7 != null) {
                String str2 = activityInfo7.packageName;
                SmsApplicationData smsApplicationData8 = (SmsApplicationData) map.get(str2);
                if (smsApplicationData8 != null && !smsApplicationData8.isComplete()) {
                    map.remove(str2);
                }
            }
        }
        return map.values();
    }

    private static SmsApplicationData getApplicationForPackage(Collection<SmsApplicationData> collection, String str) {
        if (str == null) {
            return null;
        }
        for (SmsApplicationData smsApplicationData : collection) {
            if (smsApplicationData.mPackageName.contentEquals(str)) {
                return smsApplicationData;
            }
        }
        return null;
    }

    public static SmsApplicationData getApplication(Context context, boolean z, int i) {
        SmsApplicationData applicationForPackage;
        if (!((TelephonyManager) context.getSystemService("phone")).isSmsCapable()) {
            return null;
        }
        Collection<SmsApplicationData> applicationCollectionInternal = getApplicationCollectionInternal(context, i);
        String stringForUser = Settings.Secure.getStringForUser(context.getContentResolver(), Settings.Secure.SMS_DEFAULT_APPLICATION, i);
        if (stringForUser != null) {
            applicationForPackage = getApplicationForPackage(applicationCollectionInternal, stringForUser);
        } else {
            applicationForPackage = null;
        }
        if (z && applicationForPackage == null) {
            applicationForPackage = getApplicationForPackage(applicationCollectionInternal, context.getResources().getString(R.string.default_sms_application));
            if (applicationForPackage == null && applicationCollectionInternal.size() != 0) {
                applicationForPackage = (SmsApplicationData) applicationCollectionInternal.toArray()[0];
            }
            if (applicationForPackage != null) {
                setDefaultApplicationInternal(applicationForPackage.mPackageName, context, i);
            }
        }
        if (applicationForPackage != null) {
            AppOpsManager appOpsManager = (AppOpsManager) context.getSystemService(Context.APP_OPS_SERVICE);
            if ((z || applicationForPackage.mUid == Process.myUid()) && appOpsManager.checkOp(15, applicationForPackage.mUid, applicationForPackage.mPackageName) != 0) {
                StringBuilder sb = new StringBuilder();
                sb.append(applicationForPackage.mPackageName);
                sb.append(" lost OP_WRITE_SMS: ");
                sb.append(z ? " (fixing)" : " (no permission to fix)");
                Rlog.e(LOG_TAG, sb.toString());
                if (z) {
                    appOpsManager.setMode(15, applicationForPackage.mUid, applicationForPackage.mPackageName, 0);
                } else {
                    applicationForPackage = null;
                }
            }
            if (z) {
                PackageManager packageManager = context.getPackageManager();
                configurePreferredActivity(packageManager, new ComponentName(applicationForPackage.mPackageName, applicationForPackage.mSendToClass), i);
                assignWriteSmsPermissionToSystemApp(context, packageManager, appOpsManager, "com.android.phone");
                assignWriteSmsPermissionToSystemApp(context, packageManager, appOpsManager, BLUETOOTH_PACKAGE_NAME);
                assignWriteSmsPermissionToSystemApp(context, packageManager, appOpsManager, MMS_SERVICE_PACKAGE_NAME);
                assignWriteSmsPermissionToSystemApp(context, packageManager, appOpsManager, TELEPHONY_PROVIDER_PACKAGE_NAME);
                if (mSmsDbVisitorList == null) {
                    loadSmsDbVisitor();
                }
                if (mSmsDbVisitorList != null) {
                    for (int i2 = 0; i2 < mSmsDbVisitorList.size(); i2++) {
                        assignWriteSmsPermissionToSystemApp(context, packageManager, appOpsManager, mSmsDbVisitorList.get(i2));
                    }
                }
                assignWriteSmsPermissionToSystemUid(appOpsManager, 1001);
            }
        }
        return applicationForPackage;
    }

    public static void setDefaultApplication(String str, Context context) {
        if (!((TelephonyManager) context.getSystemService("phone")).isSmsCapable()) {
            return;
        }
        int incomingUserId = getIncomingUserId(context);
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        try {
            setDefaultApplicationInternal(str, context, incomingUserId);
        } finally {
            Binder.restoreCallingIdentity(jClearCallingIdentity);
        }
    }

    private static void setDefaultApplicationInternal(String str, Context context, int i) {
        String stringForUser = Settings.Secure.getStringForUser(context.getContentResolver(), Settings.Secure.SMS_DEFAULT_APPLICATION, i);
        if (str != null && stringForUser != null && str.equals(stringForUser)) {
            return;
        }
        PackageManager packageManager = context.getPackageManager();
        Collection<SmsApplicationData> applicationCollection = getApplicationCollection(context);
        SmsApplicationData applicationForPackage = stringForUser != null ? getApplicationForPackage(applicationCollection, stringForUser) : null;
        SmsApplicationData applicationForPackage2 = getApplicationForPackage(applicationCollection, str);
        if (applicationForPackage2 != null) {
            AppOpsManager appOpsManager = (AppOpsManager) context.getSystemService(Context.APP_OPS_SERVICE);
            if (stringForUser != null) {
                try {
                    appOpsManager.setMode(15, packageManager.getPackageInfoAsUser(stringForUser, 0, i).applicationInfo.uid, stringForUser, 1);
                } catch (PackageManager.NameNotFoundException e) {
                    Rlog.w(LOG_TAG, "Old SMS package not found: " + stringForUser);
                }
            }
            Settings.Secure.putStringForUser(context.getContentResolver(), Settings.Secure.SMS_DEFAULT_APPLICATION, applicationForPackage2.mPackageName, i);
            configurePreferredActivity(packageManager, new ComponentName(applicationForPackage2.mPackageName, applicationForPackage2.mSendToClass), i);
            appOpsManager.setMode(15, applicationForPackage2.mUid, applicationForPackage2.mPackageName, 0);
            assignWriteSmsPermissionToSystemApp(context, packageManager, appOpsManager, "com.android.phone");
            assignWriteSmsPermissionToSystemApp(context, packageManager, appOpsManager, BLUETOOTH_PACKAGE_NAME);
            assignWriteSmsPermissionToSystemApp(context, packageManager, appOpsManager, MMS_SERVICE_PACKAGE_NAME);
            assignWriteSmsPermissionToSystemApp(context, packageManager, appOpsManager, TELEPHONY_PROVIDER_PACKAGE_NAME);
            if (mSmsDbVisitorList == null) {
                loadSmsDbVisitor();
            }
            if (mSmsDbVisitorList != null) {
                for (int i2 = 0; i2 < mSmsDbVisitorList.size(); i2++) {
                    assignWriteSmsPermissionToSystemApp(context, packageManager, appOpsManager, mSmsDbVisitorList.get(i2));
                }
            }
            assignWriteSmsPermissionToSystemUid(appOpsManager, 1001);
            if (applicationForPackage != null && applicationForPackage.mSmsAppChangedReceiverClass != null) {
                Intent intent = new Intent(Telephony.Sms.Intents.ACTION_DEFAULT_SMS_PACKAGE_CHANGED);
                intent.setComponent(new ComponentName(applicationForPackage.mPackageName, applicationForPackage.mSmsAppChangedReceiverClass));
                intent.putExtra(Telephony.Sms.Intents.EXTRA_IS_DEFAULT_SMS_APP, false);
                context.sendBroadcast(intent);
            }
            if (applicationForPackage2.mSmsAppChangedReceiverClass != null) {
                Intent intent2 = new Intent(Telephony.Sms.Intents.ACTION_DEFAULT_SMS_PACKAGE_CHANGED);
                intent2.setComponent(new ComponentName(applicationForPackage2.mPackageName, applicationForPackage2.mSmsAppChangedReceiverClass));
                intent2.putExtra(Telephony.Sms.Intents.EXTRA_IS_DEFAULT_SMS_APP, true);
                context.sendBroadcast(intent2);
            }
            MetricsLogger.action(context, 266, applicationForPackage2.mPackageName);
        }
    }

    private static void assignWriteSmsPermissionToSystemApp(Context context, PackageManager packageManager, AppOpsManager appOpsManager, String str) {
        if (packageManager.checkSignatures(context.getPackageName(), str) != 0) {
            Rlog.e(LOG_TAG, str + " does not have system signature");
            return;
        }
        try {
            PackageInfo packageInfo = packageManager.getPackageInfo(str, 0);
            if (appOpsManager.checkOp(15, packageInfo.applicationInfo.uid, str) != 0) {
                Rlog.w(LOG_TAG, str + " does not have OP_WRITE_SMS:  (fixing)");
                appOpsManager.setMode(15, packageInfo.applicationInfo.uid, str, 0);
            }
        } catch (PackageManager.NameNotFoundException e) {
            Rlog.e(LOG_TAG, "Package not found: " + str);
        }
    }

    private static void assignWriteSmsPermissionToSystemUid(AppOpsManager appOpsManager, int i) {
        appOpsManager.setUidMode(15, i, 0);
    }

    private static final class SmsPackageMonitor extends PackageMonitor {
        final Context mContext;

        public SmsPackageMonitor(Context context) {
            this.mContext = context;
        }

        @Override
        public void onPackageDisappeared(String str, int i) throws PackageManager.NameNotFoundException {
            onPackageChanged();
        }

        @Override
        public void onPackageAppeared(String str, int i) throws PackageManager.NameNotFoundException {
            onPackageChanged();
        }

        @Override
        public void onPackageModified(String str) throws PackageManager.NameNotFoundException {
            onPackageChanged();
        }

        private void onPackageChanged() throws PackageManager.NameNotFoundException {
            PackageManager packageManager = this.mContext.getPackageManager();
            Context contextCreatePackageContextAsUser = this.mContext;
            int sendingUserId = getSendingUserId();
            if (sendingUserId != 0) {
                try {
                    contextCreatePackageContextAsUser = this.mContext.createPackageContextAsUser(this.mContext.getPackageName(), 0, new UserHandle(sendingUserId));
                } catch (PackageManager.NameNotFoundException e) {
                }
            }
            ComponentName defaultSendToApplication = SmsApplication.getDefaultSendToApplication(contextCreatePackageContextAsUser, true);
            if (defaultSendToApplication != null) {
                SmsApplication.configurePreferredActivity(packageManager, defaultSendToApplication, sendingUserId);
            }
        }
    }

    public static void initSmsPackageMonitor(Context context) {
        sSmsPackageMonitor = new SmsPackageMonitor(context);
        sSmsPackageMonitor.register(context, context.getMainLooper(), UserHandle.ALL, false);
    }

    private static void configurePreferredActivity(PackageManager packageManager, ComponentName componentName, int i) {
        replacePreferredActivity(packageManager, componentName, i, SCHEME_SMS);
        replacePreferredActivity(packageManager, componentName, i, SCHEME_SMSTO);
        replacePreferredActivity(packageManager, componentName, i, "mms");
        replacePreferredActivity(packageManager, componentName, i, SCHEME_MMSTO);
    }

    private static void replacePreferredActivity(PackageManager packageManager, ComponentName componentName, int i, String str) {
        List<ResolveInfo> listQueryIntentActivitiesAsUser = packageManager.queryIntentActivitiesAsUser(new Intent(Intent.ACTION_SENDTO, Uri.fromParts(str, "", null)), 65600, i);
        int size = listQueryIntentActivitiesAsUser.size();
        ComponentName[] componentNameArr = new ComponentName[size];
        for (int i2 = 0; i2 < size; i2++) {
            ResolveInfo resolveInfo = listQueryIntentActivitiesAsUser.get(i2);
            componentNameArr[i2] = new ComponentName(resolveInfo.activityInfo.packageName, resolveInfo.activityInfo.name);
        }
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Intent.ACTION_SENDTO);
        intentFilter.addCategory(Intent.CATEGORY_DEFAULT);
        intentFilter.addDataScheme(str);
        packageManager.replacePreferredActivityAsUser(intentFilter, 2129920, componentNameArr, componentName, i);
    }

    public static SmsApplicationData getSmsApplicationData(String str, Context context) {
        return getApplicationForPackage(getApplicationCollection(context), str);
    }

    public static ComponentName getDefaultSmsApplication(Context context, boolean z) {
        int incomingUserId = getIncomingUserId(context);
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        ComponentName componentName = null;
        try {
            SmsApplicationData application = getApplication(context, z, incomingUserId);
            if (application != null) {
                componentName = new ComponentName(application.mPackageName, application.mSmsReceiverClass);
            }
            return componentName;
        } finally {
            Binder.restoreCallingIdentity(jClearCallingIdentity);
        }
    }

    public static ComponentName getDefaultMmsApplication(Context context, boolean z) {
        int incomingUserId = getIncomingUserId(context);
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        ComponentName componentName = null;
        try {
            SmsApplicationData application = getApplication(context, z, incomingUserId);
            if (application != null) {
                componentName = new ComponentName(application.mPackageName, application.mMmsReceiverClass);
            }
            return componentName;
        } finally {
            Binder.restoreCallingIdentity(jClearCallingIdentity);
        }
    }

    public static ComponentName getDefaultRespondViaMessageApplication(Context context, boolean z) {
        int incomingUserId = getIncomingUserId(context);
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        ComponentName componentName = null;
        try {
            SmsApplicationData application = getApplication(context, z, incomingUserId);
            if (application != null) {
                componentName = new ComponentName(application.mPackageName, application.mRespondViaMessageClass);
            }
            return componentName;
        } finally {
            Binder.restoreCallingIdentity(jClearCallingIdentity);
        }
    }

    public static ComponentName getDefaultSendToApplication(Context context, boolean z) {
        int incomingUserId = getIncomingUserId(context);
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        ComponentName componentName = null;
        try {
            SmsApplicationData application = getApplication(context, z, incomingUserId);
            if (application != null) {
                componentName = new ComponentName(application.mPackageName, application.mSendToClass);
            }
            return componentName;
        } finally {
            Binder.restoreCallingIdentity(jClearCallingIdentity);
        }
    }

    public static ComponentName getDefaultExternalTelephonyProviderChangedApplication(Context context, boolean z) {
        int incomingUserId = getIncomingUserId(context);
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        ComponentName componentName = null;
        try {
            SmsApplicationData application = getApplication(context, z, incomingUserId);
            if (application != null && application.mProviderChangedReceiverClass != null) {
                componentName = new ComponentName(application.mPackageName, application.mProviderChangedReceiverClass);
            }
            return componentName;
        } finally {
            Binder.restoreCallingIdentity(jClearCallingIdentity);
        }
    }

    public static ComponentName getDefaultSimFullApplication(Context context, boolean z) {
        int incomingUserId = getIncomingUserId(context);
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        ComponentName componentName = null;
        try {
            SmsApplicationData application = getApplication(context, z, incomingUserId);
            if (application != null && application.mSimFullReceiverClass != null) {
                componentName = new ComponentName(application.mPackageName, application.mSimFullReceiverClass);
            }
            return componentName;
        } finally {
            Binder.restoreCallingIdentity(jClearCallingIdentity);
        }
    }

    public static boolean shouldWriteMessageForPackage(String str, Context context) {
        if (SmsManager.getDefault().getAutoPersisting()) {
            return true;
        }
        return !isDefaultSmsApplication(context, str);
    }

    public static boolean isDefaultSmsApplication(Context context, String str) {
        if (str == null) {
            return false;
        }
        String defaultSmsApplicationPackageName = getDefaultSmsApplicationPackageName(context);
        if ((defaultSmsApplicationPackageName != null && defaultSmsApplicationPackageName.equals(str)) || BLUETOOTH_PACKAGE_NAME.equals(str)) {
            return true;
        }
        if (mSmsDbVisitorList == null) {
            loadSmsDbVisitor();
        }
        if (mSmsDbVisitorList != null) {
            for (int i = 0; i < mSmsDbVisitorList.size(); i++) {
                if (str.equals(mSmsDbVisitorList.get(i))) {
                    return true;
                }
            }
        }
        return false;
    }

    private static String getDefaultSmsApplicationPackageName(Context context) {
        ComponentName defaultSmsApplication = getDefaultSmsApplication(context, false);
        if (defaultSmsApplication != null) {
            return defaultSmsApplication.getPackageName();
        }
        return null;
    }

    public static void loadSmsDbVisitor() {
        XmlPullParser xmlPullParserNewPullParser;
        synchronized (mListLock) {
            if (mSmsDbVisitorList == null) {
                if (ENG) {
                    Rlog.w(LOG_TAG, "load smsdbvisitor.xml...");
                }
                mSmsDbVisitorList = new ArrayList<>();
                File file = new File(SMS_DB_VISITOR_PATH);
                try {
                    FileReader fileReader = new FileReader(file);
                    try {
                        xmlPullParserNewPullParser = Xml.newPullParser();
                        xmlPullParserNewPullParser.setInput(fileReader);
                        XmlUtils.beginDocument(xmlPullParserNewPullParser, "SmsDbVisitor");
                    } catch (IOException e) {
                        Rlog.w(LOG_TAG, "Exception in smsdbvisitor parser " + e);
                    } catch (XmlPullParserException e2) {
                        Rlog.w(LOG_TAG, "Exception in smsdbvisitor parser " + e2);
                    }
                    while (true) {
                        XmlUtils.nextElement(xmlPullParserNewPullParser);
                        if (!"SmsDbVisitor".equals(xmlPullParserNewPullParser.getName())) {
                            break;
                        }
                        mSmsDbVisitorList.add(xmlPullParserNewPullParser.getAttributeValue(null, "package"));
                    }
                    fileReader.close();
                    if (ENG) {
                        Rlog.d(LOG_TAG, "SMS db visitor list size=" + mSmsDbVisitorList.size());
                    }
                } catch (FileNotFoundException e3) {
                    Rlog.w(LOG_TAG, "Can not open " + file.getAbsolutePath());
                }
            } else {
                Rlog.d(LOG_TAG, "smsdbvisitor is already loaded");
            }
        }
    }

    public static boolean shouldWriteMessageForPackage(String str, Context context, int i) {
        try {
            Class<?> cls = Class.forName("com.mediatek.internal.telephony.MtkSmsApplication");
            Method declaredMethod = cls.getDeclaredMethod("shouldWriteMessageForPackage", String.class, Context.class, Integer.TYPE);
            Object[] objArr = {str, context, Integer.valueOf(i)};
            Rlog.d(LOG_TAG, "invoke redirect to " + cls.getName() + "." + declaredMethod.getName());
            return ((Boolean) declaredMethod.invoke(null, objArr)).booleanValue();
        } catch (Exception e) {
            Rlog.w(LOG_TAG, "No MtkCarrierConfigManager! Do nothing. - " + e);
            return shouldWriteMessageForPackage(str, context);
        }
    }
}
