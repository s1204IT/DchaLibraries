package com.android.internal.telephony;

import android.R;
import android.app.AppOpsManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Binder;
import android.os.Process;
import android.os.UserHandle;
import android.provider.Settings;
import android.provider.Telephony;
import android.telephony.Rlog;
import android.telephony.SmsManager;
import android.telephony.TelephonyManager;
import com.android.internal.content.PackageMonitor;
import com.android.internal.logging.MetricsLogger;
import com.android.internal.telephony.gsm.SmsCbConstants;
import com.mediatek.internal.telephony.uicc.UsimPBMemInfo;
import com.mediatek.sms.SmsDbVisitor;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

public final class SmsApplication {
    private static final String BLUETOOTH_PACKAGE_NAME = "com.android.bluetooth";
    private static final boolean DEBUG_MULTIUSER = false;
    static final String LOG_TAG = "SmsApplication";
    private static final String MMS_SERVICE_PACKAGE_NAME = "com.android.mms.service";
    private static final String PHONE_PACKAGE_NAME = "com.android.phone";
    private static final String SCHEME_MMS = "mms";
    private static final String SCHEME_MMSTO = "mmsto";
    private static final String SCHEME_SMS = "sms";
    private static final String SCHEME_SMSTO = "smsto";
    private static final String TELEPHONY_PROVIDER_PACKAGE_NAME = "com.android.providers.telephony";
    private static SmsPackageMonitor sSmsPackageMonitor = null;

    public static class SmsApplicationData {
        public String mApplicationName;
        public String mMmsReceiverClass;
        public String mPackageName;
        public String mProviderChangedReceiverClass;
        public String mRespondViaMessageClass;
        public String mSendToClass;
        public String mSmsAppChangedReceiverClass;
        public String mSmsReceiverClass;
        public int mUid;

        public boolean isComplete() {
            if (this.mSmsReceiverClass == null || this.mMmsReceiverClass == null || this.mRespondViaMessageClass == null || this.mSendToClass == null) {
                return SmsApplication.DEBUG_MULTIUSER;
            }
            return true;
        }

        public SmsApplicationData(String applicationName, String packageName, int uid) {
            this.mApplicationName = applicationName;
            this.mPackageName = packageName;
            this.mUid = uid;
        }

        public String toString() {
            return "mApplicationName: " + this.mApplicationName + " mPackageName: " + this.mPackageName + " mSmsReceiverClass: " + this.mSmsReceiverClass + " mMmsReceiverClass: " + this.mMmsReceiverClass + " mRespondViaMessageClass: " + this.mRespondViaMessageClass + " mSendToClass: " + this.mSendToClass + " mSmsAppChangedClass: " + this.mSmsAppChangedReceiverClass + " mProviderChangedReceiverClass: " + this.mProviderChangedReceiverClass + " mUid: " + this.mUid;
        }
    }

    private static int getIncomingUserId(Context context) {
        int contextUserId = context.getUserId();
        int callingUid = Binder.getCallingUid();
        if (UserHandle.getAppId(callingUid) < 10000) {
            return contextUserId;
        }
        return UserHandle.getUserId(callingUid);
    }

    public static Collection<SmsApplicationData> getApplicationCollection(Context context) {
        int userId = getIncomingUserId(context);
        long token = Binder.clearCallingIdentity();
        try {
            return getApplicationCollectionInternal(context, userId);
        } finally {
            Binder.restoreCallingIdentity(token);
        }
    }

    private static Collection<SmsApplicationData> getApplicationCollectionInternal(Context context, int userId) {
        String packageName;
        SmsApplicationData smsApplicationData;
        SmsApplicationData smsApplicationData2;
        SmsApplicationData smsApplicationData3;
        SmsApplicationData smsApplicationData4;
        SmsApplicationData smsApplicationData5;
        SmsApplicationData smsApplicationData6;
        PackageManager packageManager = context.getPackageManager();
        List<ResolveInfo> smsReceivers = packageManager.queryBroadcastReceiversAsUser(new Intent(Telephony.Sms.Intents.SMS_DELIVER_ACTION), 0, userId);
        HashMap<String, SmsApplicationData> receivers = new HashMap<>();
        for (ResolveInfo resolveInfo : smsReceivers) {
            ActivityInfo activityInfo = resolveInfo.activityInfo;
            if (activityInfo != null && "android.permission.BROADCAST_SMS".equals(activityInfo.permission)) {
                String packageName2 = activityInfo.packageName;
                if (!receivers.containsKey(packageName2)) {
                    String applicationName = resolveInfo.loadLabel(packageManager).toString();
                    SmsApplicationData smsApplicationData7 = new SmsApplicationData(applicationName, packageName2, activityInfo.applicationInfo.uid);
                    smsApplicationData7.mSmsReceiverClass = activityInfo.name;
                    receivers.put(packageName2, smsApplicationData7);
                }
            }
        }
        Intent intent = new Intent(Telephony.Sms.Intents.WAP_PUSH_DELIVER_ACTION);
        intent.setDataAndType(null, "application/vnd.wap.mms-message");
        List<ResolveInfo> mmsReceivers = packageManager.queryBroadcastReceiversAsUser(intent, 0, userId);
        for (ResolveInfo resolveInfo2 : mmsReceivers) {
            ActivityInfo activityInfo2 = resolveInfo2.activityInfo;
            if (activityInfo2 != null && "android.permission.BROADCAST_WAP_PUSH".equals(activityInfo2.permission) && (smsApplicationData6 = receivers.get(activityInfo2.packageName)) != null) {
                smsApplicationData6.mMmsReceiverClass = activityInfo2.name;
            }
        }
        List<ResolveInfo> respondServices = packageManager.queryIntentServicesAsUser(new Intent("android.intent.action.RESPOND_VIA_MESSAGE", Uri.fromParts(SCHEME_SMSTO, UsimPBMemInfo.STRING_NOT_SET, null)), 0, userId);
        for (ResolveInfo resolveInfo3 : respondServices) {
            ServiceInfo serviceInfo = resolveInfo3.serviceInfo;
            if (serviceInfo != null && "android.permission.SEND_RESPOND_VIA_MESSAGE".equals(serviceInfo.permission) && (smsApplicationData5 = receivers.get(serviceInfo.packageName)) != null) {
                smsApplicationData5.mRespondViaMessageClass = serviceInfo.name;
            }
        }
        List<ResolveInfo> sendToActivities = packageManager.queryIntentActivitiesAsUser(new Intent("android.intent.action.SENDTO", Uri.fromParts(SCHEME_SMSTO, UsimPBMemInfo.STRING_NOT_SET, null)), 0, userId);
        for (ResolveInfo resolveInfo4 : sendToActivities) {
            ActivityInfo activityInfo3 = resolveInfo4.activityInfo;
            if (activityInfo3 != null && (smsApplicationData4 = receivers.get(activityInfo3.packageName)) != null) {
                smsApplicationData4.mSendToClass = activityInfo3.name;
            }
        }
        List<ResolveInfo> smsAppChangedReceivers = packageManager.queryBroadcastReceiversAsUser(new Intent(Telephony.Sms.Intents.ACTION_DEFAULT_SMS_PACKAGE_CHANGED), 0, userId);
        for (ResolveInfo resolveInfo5 : smsAppChangedReceivers) {
            ActivityInfo activityInfo4 = resolveInfo5.activityInfo;
            if (activityInfo4 != null && (smsApplicationData3 = receivers.get(activityInfo4.packageName)) != null) {
                smsApplicationData3.mSmsAppChangedReceiverClass = activityInfo4.name;
            }
        }
        List<ResolveInfo> providerChangedReceivers = packageManager.queryBroadcastReceiversAsUser(new Intent(Telephony.Sms.Intents.ACTION_EXTERNAL_PROVIDER_CHANGE), 0, userId);
        for (ResolveInfo resolveInfo6 : providerChangedReceivers) {
            ActivityInfo activityInfo5 = resolveInfo6.activityInfo;
            if (activityInfo5 != null && (smsApplicationData2 = receivers.get(activityInfo5.packageName)) != null) {
                smsApplicationData2.mProviderChangedReceiverClass = activityInfo5.name;
            }
        }
        for (ResolveInfo resolveInfo7 : smsReceivers) {
            ActivityInfo activityInfo6 = resolveInfo7.activityInfo;
            if (activityInfo6 != null && (smsApplicationData = receivers.get((packageName = activityInfo6.packageName))) != null && !smsApplicationData.isComplete()) {
                receivers.remove(packageName);
            }
        }
        return receivers.values();
    }

    private static SmsApplicationData getApplicationForPackage(Collection<SmsApplicationData> applications, String packageName) {
        if (packageName == null) {
            return null;
        }
        for (SmsApplicationData application : applications) {
            if (application.mPackageName.contentEquals(packageName)) {
                return application;
            }
        }
        return null;
    }

    private static SmsApplicationData getApplication(Context context, boolean updateIfNeeded, int userId) {
        TelephonyManager tm = (TelephonyManager) context.getSystemService("phone");
        if (!tm.isSmsCapable()) {
            return null;
        }
        Collection<SmsApplicationData> applications = getApplicationCollectionInternal(context, userId);
        String defaultApplication = Settings.Secure.getStringForUser(context.getContentResolver(), "sms_default_application", userId);
        SmsApplicationData applicationData = null;
        if (defaultApplication != null) {
            applicationData = getApplicationForPackage(applications, defaultApplication);
        }
        if (updateIfNeeded && applicationData == null) {
            Resources r = context.getResources();
            String defaultPackage = r.getString(R.string.config_systemFinancedDeviceController);
            applicationData = getApplicationForPackage(applications, defaultPackage);
            if (applicationData == null && applications.size() != 0) {
                applicationData = (SmsApplicationData) applications.toArray()[0];
            }
            if (applicationData != null) {
                setDefaultApplicationInternal(applicationData.mPackageName, context, userId);
            }
        }
        if (applicationData != null) {
            AppOpsManager appOps = (AppOpsManager) context.getSystemService("appops");
            if (updateIfNeeded || applicationData.mUid == Process.myUid()) {
                int mode = appOps.checkOp(15, applicationData.mUid, applicationData.mPackageName);
                if (mode != 0) {
                    Rlog.e(LOG_TAG, applicationData.mPackageName + " lost OP_WRITE_SMS: " + (updateIfNeeded ? " (fixing)" : " (no permission to fix)"));
                    if (updateIfNeeded) {
                        appOps.setMode(15, applicationData.mUid, applicationData.mPackageName, 0);
                    } else {
                        applicationData = null;
                    }
                }
            }
            if (updateIfNeeded) {
                PackageManager packageManager = context.getPackageManager();
                configurePreferredActivity(packageManager, new ComponentName(applicationData.mPackageName, applicationData.mSendToClass), userId);
                assignWriteSmsPermissionToSystemApp(context, packageManager, appOps, PHONE_PACKAGE_NAME);
                assignWriteSmsPermissionToSystemApp(context, packageManager, appOps, BLUETOOTH_PACKAGE_NAME);
                assignWriteSmsPermissionToSystemApp(context, packageManager, appOps, MMS_SERVICE_PACKAGE_NAME);
                assignWriteSmsPermissionToSystemApp(context, packageManager, appOps, TELEPHONY_PROVIDER_PACKAGE_NAME);
                String[] specialApps = SmsDbVisitor.getPackageNames();
                if (specialApps != null) {
                    for (String str : specialApps) {
                        assignWriteSmsPermissionToSystemApp(context, packageManager, appOps, str);
                    }
                }
                assignWriteSmsPermissionToSystemUid(appOps, 1001);
            }
        }
        return applicationData;
    }

    public static void setDefaultApplication(String packageName, Context context) {
        TelephonyManager tm = (TelephonyManager) context.getSystemService("phone");
        if (!tm.isSmsCapable()) {
            return;
        }
        int userId = getIncomingUserId(context);
        long token = Binder.clearCallingIdentity();
        try {
            setDefaultApplicationInternal(packageName, context, userId);
        } finally {
            Binder.restoreCallingIdentity(token);
        }
    }

    private static void setDefaultApplicationInternal(String packageName, Context context, int userId) {
        String oldPackageName = Settings.Secure.getStringForUser(context.getContentResolver(), "sms_default_application", userId);
        if (packageName != null && oldPackageName != null && packageName.equals(oldPackageName)) {
            return;
        }
        PackageManager packageManager = context.getPackageManager();
        Collection<SmsApplicationData> applications = getApplicationCollection(context);
        SmsApplicationData applicationForPackage = oldPackageName != null ? getApplicationForPackage(applications, oldPackageName) : null;
        SmsApplicationData applicationData = getApplicationForPackage(applications, packageName);
        if (applicationData == null) {
            return;
        }
        AppOpsManager appOps = (AppOpsManager) context.getSystemService("appops");
        if (oldPackageName != null) {
            try {
                PackageInfo info = packageManager.getPackageInfo(oldPackageName, SmsCbConstants.SERIAL_NUMBER_ETWS_EMERGENCY_USER_ALERT);
                appOps.setMode(15, info.applicationInfo.uid, oldPackageName, 1);
            } catch (PackageManager.NameNotFoundException e) {
                Rlog.w(LOG_TAG, "Old SMS package not found: " + oldPackageName);
            }
        }
        Settings.Secure.putStringForUser(context.getContentResolver(), "sms_default_application", applicationData.mPackageName, userId);
        configurePreferredActivity(packageManager, new ComponentName(applicationData.mPackageName, applicationData.mSendToClass), userId);
        appOps.setMode(15, applicationData.mUid, applicationData.mPackageName, 0);
        assignWriteSmsPermissionToSystemApp(context, packageManager, appOps, PHONE_PACKAGE_NAME);
        assignWriteSmsPermissionToSystemApp(context, packageManager, appOps, BLUETOOTH_PACKAGE_NAME);
        assignWriteSmsPermissionToSystemApp(context, packageManager, appOps, MMS_SERVICE_PACKAGE_NAME);
        assignWriteSmsPermissionToSystemApp(context, packageManager, appOps, TELEPHONY_PROVIDER_PACKAGE_NAME);
        String[] specialApps = SmsDbVisitor.getPackageNames();
        if (specialApps != null) {
            for (String str : specialApps) {
                assignWriteSmsPermissionToSystemApp(context, packageManager, appOps, str);
            }
        }
        assignWriteSmsPermissionToSystemUid(appOps, 1001);
        if (applicationForPackage != null && applicationForPackage.mSmsAppChangedReceiverClass != null) {
            Intent oldAppIntent = new Intent(Telephony.Sms.Intents.ACTION_DEFAULT_SMS_PACKAGE_CHANGED);
            ComponentName component = new ComponentName(applicationForPackage.mPackageName, applicationForPackage.mSmsAppChangedReceiverClass);
            oldAppIntent.setComponent(component);
            oldAppIntent.putExtra(Telephony.Sms.Intents.EXTRA_IS_DEFAULT_SMS_APP, DEBUG_MULTIUSER);
            context.sendBroadcast(oldAppIntent);
        }
        if (applicationData.mSmsAppChangedReceiverClass != null) {
            Intent intent = new Intent(Telephony.Sms.Intents.ACTION_DEFAULT_SMS_PACKAGE_CHANGED);
            ComponentName component2 = new ComponentName(applicationData.mPackageName, applicationData.mSmsAppChangedReceiverClass);
            intent.setComponent(component2);
            intent.putExtra(Telephony.Sms.Intents.EXTRA_IS_DEFAULT_SMS_APP, true);
            context.sendBroadcast(intent);
        }
        MetricsLogger.action(context, 266, applicationData.mPackageName);
    }

    private static void assignWriteSmsPermissionToSystemApp(Context context, PackageManager packageManager, AppOpsManager appOps, String packageName) {
        int result = packageManager.checkSignatures(context.getPackageName(), packageName);
        if (result != 0) {
            Rlog.e(LOG_TAG, packageName + " does not have system signature");
            return;
        }
        try {
            PackageInfo info = packageManager.getPackageInfo(packageName, 0);
            int mode = appOps.checkOp(15, info.applicationInfo.uid, packageName);
            if (mode == 0) {
                return;
            }
            Rlog.w(LOG_TAG, packageName + " does not have OP_WRITE_SMS:  (fixing)");
            appOps.setMode(15, info.applicationInfo.uid, packageName, 0);
        } catch (PackageManager.NameNotFoundException e) {
            Rlog.e(LOG_TAG, "Package not found: " + packageName);
        }
    }

    private static void assignWriteSmsPermissionToSystemUid(AppOpsManager appOps, int uid) {
        appOps.setUidMode(15, uid, 0);
    }

    private static final class SmsPackageMonitor extends PackageMonitor {
        final Context mContext;

        public SmsPackageMonitor(Context context) {
            this.mContext = context;
        }

        public void onPackageDisappeared(String packageName, int reason) {
            onPackageChanged();
        }

        public void onPackageAppeared(String packageName, int reason) {
            onPackageChanged();
        }

        public void onPackageModified(String packageName) {
            onPackageChanged();
        }

        private void onPackageChanged() {
            PackageManager packageManager = this.mContext.getPackageManager();
            Context userContext = this.mContext;
            int userId = getSendingUserId();
            if (userId != 0) {
                try {
                    userContext = this.mContext.createPackageContextAsUser(this.mContext.getPackageName(), 0, new UserHandle(userId));
                } catch (PackageManager.NameNotFoundException e) {
                }
            }
            ComponentName componentName = SmsApplication.getDefaultSendToApplication(userContext, true);
            if (componentName == null) {
                return;
            }
            SmsApplication.configurePreferredActivity(packageManager, componentName, userId);
        }
    }

    public static void initSmsPackageMonitor(Context context) {
        sSmsPackageMonitor = new SmsPackageMonitor(context);
        sSmsPackageMonitor.register(context, context.getMainLooper(), UserHandle.ALL, DEBUG_MULTIUSER);
    }

    private static void configurePreferredActivity(PackageManager packageManager, ComponentName componentName, int userId) {
        replacePreferredActivity(packageManager, componentName, userId, SCHEME_SMS);
        replacePreferredActivity(packageManager, componentName, userId, SCHEME_SMSTO);
        replacePreferredActivity(packageManager, componentName, userId, SCHEME_MMS);
        replacePreferredActivity(packageManager, componentName, userId, SCHEME_MMSTO);
    }

    private static void replacePreferredActivity(PackageManager packageManager, ComponentName componentName, int userId, String scheme) {
        Intent intent = new Intent("android.intent.action.SENDTO", Uri.fromParts(scheme, UsimPBMemInfo.STRING_NOT_SET, null));
        List<ResolveInfo> resolveInfoList = packageManager.queryIntentActivitiesAsUser(intent, 65600, userId);
        int n = resolveInfoList.size();
        ComponentName[] set = new ComponentName[n];
        for (int i = 0; i < n; i++) {
            ResolveInfo info = resolveInfoList.get(i);
            set[i] = new ComponentName(info.activityInfo.packageName, info.activityInfo.name);
        }
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("android.intent.action.SENDTO");
        intentFilter.addCategory("android.intent.category.DEFAULT");
        intentFilter.addDataScheme(scheme);
        packageManager.replacePreferredActivityAsUser(intentFilter, 2129920, set, componentName, userId);
    }

    public static SmsApplicationData getSmsApplicationData(String packageName, Context context) {
        Collection<SmsApplicationData> applications = getApplicationCollection(context);
        return getApplicationForPackage(applications, packageName);
    }

    public static ComponentName getDefaultSmsApplication(Context context, boolean updateIfNeeded) {
        int userId = getIncomingUserId(context);
        long token = Binder.clearCallingIdentity();
        ComponentName componentName = null;
        try {
            SmsApplicationData smsApplicationData = getApplication(context, updateIfNeeded, userId);
            if (smsApplicationData != null) {
                componentName = new ComponentName(smsApplicationData.mPackageName, smsApplicationData.mSmsReceiverClass);
            }
            return componentName;
        } finally {
            Binder.restoreCallingIdentity(token);
        }
    }

    public static ComponentName getDefaultMmsApplication(Context context, boolean updateIfNeeded) {
        int userId = getIncomingUserId(context);
        long token = Binder.clearCallingIdentity();
        ComponentName componentName = null;
        try {
            SmsApplicationData smsApplicationData = getApplication(context, updateIfNeeded, userId);
            if (smsApplicationData != null) {
                componentName = new ComponentName(smsApplicationData.mPackageName, smsApplicationData.mMmsReceiverClass);
            }
            return componentName;
        } finally {
            Binder.restoreCallingIdentity(token);
        }
    }

    public static ComponentName getDefaultRespondViaMessageApplication(Context context, boolean updateIfNeeded) {
        int userId = getIncomingUserId(context);
        long token = Binder.clearCallingIdentity();
        ComponentName componentName = null;
        try {
            SmsApplicationData smsApplicationData = getApplication(context, updateIfNeeded, userId);
            if (smsApplicationData != null) {
                componentName = new ComponentName(smsApplicationData.mPackageName, smsApplicationData.mRespondViaMessageClass);
            }
            return componentName;
        } finally {
            Binder.restoreCallingIdentity(token);
        }
    }

    public static ComponentName getDefaultSendToApplication(Context context, boolean updateIfNeeded) {
        int userId = getIncomingUserId(context);
        long token = Binder.clearCallingIdentity();
        ComponentName componentName = null;
        try {
            SmsApplicationData smsApplicationData = getApplication(context, updateIfNeeded, userId);
            if (smsApplicationData != null) {
                componentName = new ComponentName(smsApplicationData.mPackageName, smsApplicationData.mSendToClass);
            }
            return componentName;
        } finally {
            Binder.restoreCallingIdentity(token);
        }
    }

    public static ComponentName getDefaultExternalTelephonyProviderChangedApplication(Context context, boolean updateIfNeeded) {
        int userId = getIncomingUserId(context);
        long token = Binder.clearCallingIdentity();
        ComponentName componentName = null;
        try {
            SmsApplicationData smsApplicationData = getApplication(context, updateIfNeeded, userId);
            if (smsApplicationData != null && smsApplicationData.mProviderChangedReceiverClass != null) {
                componentName = new ComponentName(smsApplicationData.mPackageName, smsApplicationData.mProviderChangedReceiverClass);
            }
            return componentName;
        } finally {
            Binder.restoreCallingIdentity(token);
        }
    }

    public static boolean shouldWriteMessageForPackage(String packageName, Context context) {
        if (!SmsManager.getDefault().getAutoPersisting() && isDefaultSmsApplication(context, packageName)) {
            return DEBUG_MULTIUSER;
        }
        return true;
    }

    public static boolean isDefaultSmsApplication(Context context, String packageName) {
        if (packageName == null) {
            return DEBUG_MULTIUSER;
        }
        String defaultSmsPackage = getDefaultSmsApplicationPackageName(context);
        if ((defaultSmsPackage != null && defaultSmsPackage.equals(packageName)) || BLUETOOTH_PACKAGE_NAME.equals(packageName)) {
            return true;
        }
        String[] specialApps = SmsDbVisitor.getPackageNames();
        if (specialApps != null) {
            for (String str : specialApps) {
                if (packageName.equals(str)) {
                    return true;
                }
            }
        }
        return DEBUG_MULTIUSER;
    }

    private static String getDefaultSmsApplicationPackageName(Context context) {
        ComponentName component = getDefaultSmsApplication(context, DEBUG_MULTIUSER);
        if (component != null) {
            return component.getPackageName();
        }
        return null;
    }
}
