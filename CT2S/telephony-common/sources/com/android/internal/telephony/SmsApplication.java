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
import com.android.internal.telephony.gsm.SmsCbConstants;
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
    private static SmsPackageMonitor sSmsPackageMonitor = null;

    public static class SmsApplicationData {
        public String mApplicationName;
        public String mMmsReceiverClass;
        public String mPackageName;
        public String mRespondViaMessageClass;
        public String mSendToClass;
        public String mSmsReceiverClass;
        public int mUid;

        public boolean isComplete() {
            return (this.mSmsReceiverClass == null || this.mMmsReceiverClass == null || this.mRespondViaMessageClass == null || this.mSendToClass == null) ? false : true;
        }

        public SmsApplicationData(String applicationName, String packageName, int uid) {
            this.mApplicationName = applicationName;
            this.mPackageName = packageName;
            this.mUid = uid;
        }
    }

    private static int getIncomingUserId(Context context) {
        int contextUserId = context.getUserId();
        int callingUid = Binder.getCallingUid();
        if (UserHandle.getAppId(callingUid) < 10000) {
            return contextUserId;
        }
        int contextUserId2 = UserHandle.getUserId(callingUid);
        return contextUserId2;
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
        PackageManager packageManager = context.getPackageManager();
        List<ResolveInfo> smsReceivers = packageManager.queryBroadcastReceivers(new Intent(Telephony.Sms.Intents.SMS_DELIVER_ACTION), 0, userId);
        HashMap<String, SmsApplicationData> receivers = new HashMap<>();
        for (ResolveInfo resolveInfo : smsReceivers) {
            ActivityInfo activityInfo = resolveInfo.activityInfo;
            if (activityInfo != null && "android.permission.BROADCAST_SMS".equals(activityInfo.permission)) {
                String packageName2 = activityInfo.packageName;
                if (!receivers.containsKey(packageName2)) {
                    String applicationName = resolveInfo.loadLabel(packageManager).toString();
                    SmsApplicationData smsApplicationData5 = new SmsApplicationData(applicationName, packageName2, activityInfo.applicationInfo.uid);
                    smsApplicationData5.mSmsReceiverClass = activityInfo.name;
                    receivers.put(packageName2, smsApplicationData5);
                }
            }
        }
        Intent intent = new Intent(Telephony.Sms.Intents.WAP_PUSH_DELIVER_ACTION);
        intent.setDataAndType(null, "application/vnd.wap.mms-message");
        List<ResolveInfo> mmsReceivers = packageManager.queryBroadcastReceivers(intent, 0, userId);
        for (ResolveInfo resolveInfo2 : mmsReceivers) {
            ActivityInfo activityInfo2 = resolveInfo2.activityInfo;
            if (activityInfo2 != null && "android.permission.BROADCAST_WAP_PUSH".equals(activityInfo2.permission) && (smsApplicationData4 = receivers.get(activityInfo2.packageName)) != null) {
                smsApplicationData4.mMmsReceiverClass = activityInfo2.name;
            }
        }
        List<ResolveInfo> respondServices = packageManager.queryIntentServicesAsUser(new Intent("android.intent.action.RESPOND_VIA_MESSAGE", Uri.fromParts(SCHEME_SMSTO, "", null)), 0, userId);
        for (ResolveInfo resolveInfo3 : respondServices) {
            ServiceInfo serviceInfo = resolveInfo3.serviceInfo;
            if (serviceInfo != null && "android.permission.SEND_RESPOND_VIA_MESSAGE".equals(serviceInfo.permission) && (smsApplicationData3 = receivers.get(serviceInfo.packageName)) != null) {
                smsApplicationData3.mRespondViaMessageClass = serviceInfo.name;
            }
        }
        List<ResolveInfo> sendToActivities = packageManager.queryIntentActivitiesAsUser(new Intent("android.intent.action.SENDTO", Uri.fromParts(SCHEME_SMSTO, "", null)), 0, userId);
        for (ResolveInfo resolveInfo4 : sendToActivities) {
            ActivityInfo activityInfo3 = resolveInfo4.activityInfo;
            if (activityInfo3 != null && (smsApplicationData2 = receivers.get(activityInfo3.packageName)) != null) {
                smsApplicationData2.mSendToClass = activityInfo3.name;
            }
        }
        for (ResolveInfo resolveInfo5 : smsReceivers) {
            ActivityInfo activityInfo4 = resolveInfo5.activityInfo;
            if (activityInfo4 != null && (smsApplicationData = receivers.get((packageName = activityInfo4.packageName))) != null && !smsApplicationData.isComplete()) {
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
            String defaultPackage = r.getString(R.string.config_systemAutomotiveProjection);
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
                try {
                    PackageInfo info = packageManager.getPackageInfo(PHONE_PACKAGE_NAME, 0);
                    int mode2 = appOps.checkOp(15, info.applicationInfo.uid, PHONE_PACKAGE_NAME);
                    if (mode2 != 0) {
                        Rlog.e(LOG_TAG, "com.android.phone lost OP_WRITE_SMS:  (fixing)");
                        appOps.setMode(15, info.applicationInfo.uid, PHONE_PACKAGE_NAME, 0);
                    }
                } catch (PackageManager.NameNotFoundException e) {
                    Rlog.e(LOG_TAG, "Phone package not found: com.android.phone");
                    applicationData = null;
                }
                try {
                    PackageInfo info2 = packageManager.getPackageInfo(BLUETOOTH_PACKAGE_NAME, 0);
                    int mode3 = appOps.checkOp(15, info2.applicationInfo.uid, BLUETOOTH_PACKAGE_NAME);
                    if (mode3 != 0) {
                        Rlog.e(LOG_TAG, "com.android.bluetooth lost OP_WRITE_SMS:  (fixing)");
                        appOps.setMode(15, info2.applicationInfo.uid, BLUETOOTH_PACKAGE_NAME, 0);
                    }
                } catch (PackageManager.NameNotFoundException e2) {
                    Rlog.e(LOG_TAG, "Bluetooth package not found: com.android.bluetooth");
                }
                try {
                    PackageInfo info3 = packageManager.getPackageInfo(MMS_SERVICE_PACKAGE_NAME, 0);
                    int mode4 = appOps.checkOp(15, info3.applicationInfo.uid, MMS_SERVICE_PACKAGE_NAME);
                    if (mode4 != 0) {
                        Rlog.e(LOG_TAG, "com.android.mms.service lost OP_WRITE_SMS:  (fixing)");
                        appOps.setMode(15, info3.applicationInfo.uid, MMS_SERVICE_PACKAGE_NAME, 0);
                        return applicationData;
                    }
                    return applicationData;
                } catch (PackageManager.NameNotFoundException e3) {
                    Rlog.e(LOG_TAG, "MmsService package not found: com.android.mms.service");
                    return null;
                }
            }
            return applicationData;
        }
        return applicationData;
    }

    public static void setDefaultApplication(String packageName, Context context) {
        TelephonyManager tm = (TelephonyManager) context.getSystemService("phone");
        if (tm.isSmsCapable()) {
            int userId = getIncomingUserId(context);
            long token = Binder.clearCallingIdentity();
            try {
                setDefaultApplicationInternal(packageName, context, userId);
            } finally {
                Binder.restoreCallingIdentity(token);
            }
        }
    }

    private static void setDefaultApplicationInternal(String packageName, Context context, int userId) {
        String oldPackageName = Settings.Secure.getStringForUser(context.getContentResolver(), "sms_default_application", userId);
        if (packageName == null || oldPackageName == null || !packageName.equals(oldPackageName)) {
            PackageManager packageManager = context.getPackageManager();
            Collection<SmsApplicationData> applications = getApplicationCollection(context);
            SmsApplicationData applicationData = getApplicationForPackage(applications, packageName);
            if (applicationData != null) {
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
                try {
                    PackageInfo info2 = packageManager.getPackageInfo(PHONE_PACKAGE_NAME, 0);
                    appOps.setMode(15, info2.applicationInfo.uid, PHONE_PACKAGE_NAME, 0);
                } catch (PackageManager.NameNotFoundException e2) {
                    Rlog.e(LOG_TAG, "Phone package not found: com.android.phone");
                }
                try {
                    PackageInfo info3 = packageManager.getPackageInfo(BLUETOOTH_PACKAGE_NAME, 0);
                    appOps.setMode(15, info3.applicationInfo.uid, BLUETOOTH_PACKAGE_NAME, 0);
                } catch (PackageManager.NameNotFoundException e3) {
                    Rlog.e(LOG_TAG, "Bluetooth package not found: com.android.bluetooth");
                }
                try {
                    PackageInfo info4 = packageManager.getPackageInfo(MMS_SERVICE_PACKAGE_NAME, 0);
                    appOps.setMode(15, info4.applicationInfo.uid, MMS_SERVICE_PACKAGE_NAME, 0);
                } catch (PackageManager.NameNotFoundException e4) {
                    Rlog.e(LOG_TAG, "MmsService package not found: com.android.mms.service");
                }
            }
        }
    }

    private static final class SmsPackageMonitor extends PackageMonitor {
        final Context mContext;

        public SmsPackageMonitor(Context context) {
            this.mContext = context;
        }

        public void onPackageDisappeared(String packageName, int reason) {
            onPackageChanged(packageName);
        }

        public void onPackageAppeared(String packageName, int reason) {
            onPackageChanged(packageName);
        }

        public void onPackageModified(String packageName) {
            onPackageChanged(packageName);
        }

        private void onPackageChanged(String packageName) {
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
            if (componentName != null) {
                SmsApplication.configurePreferredActivity(packageManager, componentName, userId);
            }
        }
    }

    public static void initSmsPackageMonitor(Context context) {
        sSmsPackageMonitor = new SmsPackageMonitor(context);
        sSmsPackageMonitor.register(context, context.getMainLooper(), UserHandle.ALL, false);
    }

    private static void configurePreferredActivity(PackageManager packageManager, ComponentName componentName, int userId) {
        replacePreferredActivity(packageManager, componentName, userId, SCHEME_SMS);
        replacePreferredActivity(packageManager, componentName, userId, SCHEME_SMSTO);
        replacePreferredActivity(packageManager, componentName, userId, SCHEME_MMS);
        replacePreferredActivity(packageManager, componentName, userId, SCHEME_MMSTO);
    }

    private static void replacePreferredActivity(PackageManager packageManager, ComponentName componentName, int userId, String scheme) {
        Intent intent = new Intent("android.intent.action.SENDTO", Uri.fromParts(scheme, "", null));
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
        ComponentName component = null;
        try {
            SmsApplicationData smsApplicationData = getApplication(context, updateIfNeeded, userId);
            if (smsApplicationData != null) {
                component = new ComponentName(smsApplicationData.mPackageName, smsApplicationData.mSmsReceiverClass);
            }
            return component;
        } finally {
            Binder.restoreCallingIdentity(token);
        }
    }

    public static ComponentName getDefaultMmsApplication(Context context, boolean updateIfNeeded) {
        int userId = getIncomingUserId(context);
        long token = Binder.clearCallingIdentity();
        ComponentName component = null;
        try {
            SmsApplicationData smsApplicationData = getApplication(context, updateIfNeeded, userId);
            if (smsApplicationData != null) {
                component = new ComponentName(smsApplicationData.mPackageName, smsApplicationData.mMmsReceiverClass);
            }
            return component;
        } finally {
            Binder.restoreCallingIdentity(token);
        }
    }

    public static ComponentName getDefaultRespondViaMessageApplication(Context context, boolean updateIfNeeded) {
        int userId = getIncomingUserId(context);
        long token = Binder.clearCallingIdentity();
        ComponentName component = null;
        try {
            SmsApplicationData smsApplicationData = getApplication(context, updateIfNeeded, userId);
            if (smsApplicationData != null) {
                component = new ComponentName(smsApplicationData.mPackageName, smsApplicationData.mRespondViaMessageClass);
            }
            return component;
        } finally {
            Binder.restoreCallingIdentity(token);
        }
    }

    public static ComponentName getDefaultSendToApplication(Context context, boolean updateIfNeeded) {
        int userId = getIncomingUserId(context);
        long token = Binder.clearCallingIdentity();
        ComponentName component = null;
        try {
            SmsApplicationData smsApplicationData = getApplication(context, updateIfNeeded, userId);
            if (smsApplicationData != null) {
                component = new ComponentName(smsApplicationData.mPackageName, smsApplicationData.mSendToClass);
            }
            return component;
        } finally {
            Binder.restoreCallingIdentity(token);
        }
    }

    public static boolean shouldWriteMessageForPackage(String packageName, Context context) {
        if (packageName == null || SmsManager.getDefault().getAutoPersisting()) {
            return true;
        }
        String defaultSmsPackage = null;
        ComponentName component = getDefaultSmsApplication(context, false);
        if (component != null) {
            defaultSmsPackage = component.getPackageName();
        }
        return (defaultSmsPackage == null || !defaultSmsPackage.equals(packageName)) && !packageName.equals(BLUETOOTH_PACKAGE_NAME);
    }
}
