package com.android.server.pm.permission;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManagerInternal;
import android.content.pm.PackageParser;
import android.content.pm.ProviderInfo;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Binder;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.UserHandle;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.util.Log;
import android.util.Slog;
import android.util.Xml;
import com.android.internal.util.XmlUtils;
import com.android.server.LocalServices;
import com.android.server.backup.BackupManagerService;
import com.android.server.pm.PackageManagerService;
import com.mediatek.cta.CtaManager;
import com.mediatek.cta.CtaManagerFactory;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

public final class DefaultPermissionGrantPolicy {
    private static final String ACTION_TRACK = "com.android.fitness.TRACK";
    private static final String ATTR_FIXED = "fixed";
    private static final String ATTR_NAME = "name";
    private static final String ATTR_PACKAGE = "package";
    private static final String AUDIO_MIME_TYPE = "audio/mpeg";
    private static final Set<String> CALENDAR_PERMISSIONS;
    private static final Set<String> CAMERA_PERMISSIONS;
    private static final Set<String> COARSE_LOCATION_PERMISSIONS;
    private static final Set<String> CONTACTS_PERMISSIONS;
    private static final CtaManager CTA_MANAGER;
    private static final boolean DEBUG = false;
    private static final int DEFAULT_FLAGS = 794624;
    private static final Set<String> LOCATION_PERMISSIONS;
    private static final Set<String> MICROPHONE_PERMISSIONS;
    private static final int MSG_READ_DEFAULT_PERMISSION_EXCEPTIONS = 1;
    private static final Set<String> PHONE_PERMISSIONS = new ArraySet();
    private static final Set<String> SENSORS_PERMISSIONS;
    private static final Set<String> SMS_PERMISSIONS;
    private static final Set<String> STORAGE_PERMISSIONS;
    private static final String TAG = "DefaultPermGrantPolicy";
    private static final String TAG_EXCEPTION = "exception";
    private static final String TAG_EXCEPTIONS = "exceptions";
    private static final String TAG_PERMISSION = "permission";
    private final Context mContext;
    private PackageManagerInternal.PackagesProvider mDialerAppPackagesProvider;
    private ArrayMap<String, List<DefaultPermissionGrant>> mGrantExceptions;
    private final Handler mHandler;
    private PackageManagerInternal.PackagesProvider mLocationPackagesProvider;
    private final DefaultPermissionGrantedCallback mPermissionGrantedCallback;
    private final PermissionManagerService mPermissionManager;
    private PackageManagerInternal.PackagesProvider mSimCallManagerPackagesProvider;
    private PackageManagerInternal.PackagesProvider mSmsAppPackagesProvider;
    private PackageManagerInternal.SyncAdapterPackagesProvider mSyncAdapterPackagesProvider;
    private PackageManagerInternal.PackagesProvider mUseOpenWifiAppPackagesProvider;
    private PackageManagerInternal.PackagesProvider mVoiceInteractionPackagesProvider;
    private final Object mLock = new Object();
    private final PackageManagerInternal mServiceInternal = (PackageManagerInternal) LocalServices.getService(PackageManagerInternal.class);

    public interface DefaultPermissionGrantedCallback {
        void onDefaultRuntimePermissionsGranted(int i);
    }

    static {
        PHONE_PERMISSIONS.add("android.permission.READ_PHONE_STATE");
        PHONE_PERMISSIONS.add("android.permission.CALL_PHONE");
        PHONE_PERMISSIONS.add("android.permission.READ_CALL_LOG");
        PHONE_PERMISSIONS.add("android.permission.WRITE_CALL_LOG");
        PHONE_PERMISSIONS.add("com.android.voicemail.permission.ADD_VOICEMAIL");
        PHONE_PERMISSIONS.add("android.permission.USE_SIP");
        PHONE_PERMISSIONS.add("android.permission.PROCESS_OUTGOING_CALLS");
        CONTACTS_PERMISSIONS = new ArraySet();
        CONTACTS_PERMISSIONS.add("android.permission.READ_CONTACTS");
        CONTACTS_PERMISSIONS.add("android.permission.WRITE_CONTACTS");
        CONTACTS_PERMISSIONS.add("android.permission.GET_ACCOUNTS");
        LOCATION_PERMISSIONS = new ArraySet();
        LOCATION_PERMISSIONS.add("android.permission.ACCESS_FINE_LOCATION");
        LOCATION_PERMISSIONS.add("android.permission.ACCESS_COARSE_LOCATION");
        COARSE_LOCATION_PERMISSIONS = new ArraySet();
        COARSE_LOCATION_PERMISSIONS.add("android.permission.ACCESS_COARSE_LOCATION");
        CALENDAR_PERMISSIONS = new ArraySet();
        CALENDAR_PERMISSIONS.add("android.permission.READ_CALENDAR");
        CALENDAR_PERMISSIONS.add("android.permission.WRITE_CALENDAR");
        SMS_PERMISSIONS = new ArraySet();
        SMS_PERMISSIONS.add("android.permission.SEND_SMS");
        SMS_PERMISSIONS.add("android.permission.RECEIVE_SMS");
        SMS_PERMISSIONS.add("android.permission.READ_SMS");
        SMS_PERMISSIONS.add("android.permission.RECEIVE_WAP_PUSH");
        SMS_PERMISSIONS.add("android.permission.RECEIVE_MMS");
        SMS_PERMISSIONS.add("android.permission.READ_CELL_BROADCASTS");
        MICROPHONE_PERMISSIONS = new ArraySet();
        MICROPHONE_PERMISSIONS.add("android.permission.RECORD_AUDIO");
        CAMERA_PERMISSIONS = new ArraySet();
        CAMERA_PERMISSIONS.add("android.permission.CAMERA");
        SENSORS_PERMISSIONS = new ArraySet();
        SENSORS_PERMISSIONS.add("android.permission.BODY_SENSORS");
        STORAGE_PERMISSIONS = new ArraySet();
        STORAGE_PERMISSIONS.add("android.permission.READ_EXTERNAL_STORAGE");
        STORAGE_PERMISSIONS.add("android.permission.WRITE_EXTERNAL_STORAGE");
        CTA_MANAGER = CtaManagerFactory.getInstance().makeCtaManager();
        if (CTA_MANAGER.isCtaSupported()) {
            PHONE_PERMISSIONS.add("com.mediatek.permission.CTA_CONFERENCE_CALL");
            SMS_PERMISSIONS.add("com.mediatek.permission.CTA_SEND_MMS");
        }
    }

    public DefaultPermissionGrantPolicy(Context context, Looper looper, DefaultPermissionGrantedCallback defaultPermissionGrantedCallback, PermissionManagerService permissionManagerService) {
        this.mContext = context;
        this.mHandler = new Handler(looper) {
            @Override
            public void handleMessage(Message message) {
                if (message.what == 1) {
                    synchronized (DefaultPermissionGrantPolicy.this.mLock) {
                        if (DefaultPermissionGrantPolicy.this.mGrantExceptions == null) {
                            DefaultPermissionGrantPolicy.this.mGrantExceptions = DefaultPermissionGrantPolicy.this.readDefaultPermissionExceptionsLocked();
                        }
                    }
                }
            }
        };
        this.mPermissionGrantedCallback = defaultPermissionGrantedCallback;
        this.mPermissionManager = permissionManagerService;
    }

    public void setLocationPackagesProvider(PackageManagerInternal.PackagesProvider packagesProvider) {
        synchronized (this.mLock) {
            this.mLocationPackagesProvider = packagesProvider;
        }
    }

    public void setVoiceInteractionPackagesProvider(PackageManagerInternal.PackagesProvider packagesProvider) {
        synchronized (this.mLock) {
            this.mVoiceInteractionPackagesProvider = packagesProvider;
        }
    }

    public void setSmsAppPackagesProvider(PackageManagerInternal.PackagesProvider packagesProvider) {
        synchronized (this.mLock) {
            this.mSmsAppPackagesProvider = packagesProvider;
        }
    }

    public void setDialerAppPackagesProvider(PackageManagerInternal.PackagesProvider packagesProvider) {
        synchronized (this.mLock) {
            this.mDialerAppPackagesProvider = packagesProvider;
        }
    }

    public void setSimCallManagerPackagesProvider(PackageManagerInternal.PackagesProvider packagesProvider) {
        synchronized (this.mLock) {
            this.mSimCallManagerPackagesProvider = packagesProvider;
        }
    }

    public void setUseOpenWifiAppPackagesProvider(PackageManagerInternal.PackagesProvider packagesProvider) {
        synchronized (this.mLock) {
            this.mUseOpenWifiAppPackagesProvider = packagesProvider;
        }
    }

    public void setSyncAdapterPackagesProvider(PackageManagerInternal.SyncAdapterPackagesProvider syncAdapterPackagesProvider) {
        synchronized (this.mLock) {
            this.mSyncAdapterPackagesProvider = syncAdapterPackagesProvider;
        }
    }

    public void grantDefaultPermissions(int i) {
        grantPermissionsToSysComponentsAndPrivApps(i);
        grantDefaultSystemHandlerPermissions(i);
        grantDefaultPermissionExceptions(i);
    }

    private void grantRuntimePermissionsForPackage(int i, PackageParser.Package r6) {
        ArraySet arraySet = new ArraySet();
        for (String str : r6.requestedPermissions) {
            BasePermission permission = this.mPermissionManager.getPermission(str);
            if (permission != null && permission.isRuntime()) {
                arraySet.add(str);
            }
        }
        if (!arraySet.isEmpty()) {
            grantRuntimePermissions(r6, arraySet, true, i);
        }
    }

    private void grantAllRuntimePermissions(int i) {
        Log.i(TAG, "Granting all runtime permissions for user " + i);
        Iterator it = this.mServiceInternal.getPackageList().getPackageNames().iterator();
        while (it.hasNext()) {
            PackageParser.Package r1 = this.mServiceInternal.getPackage((String) it.next());
            if (r1 != null) {
                grantRuntimePermissionsForPackage(i, r1);
            }
        }
    }

    public void scheduleReadDefaultPermissionExceptions() {
        this.mHandler.sendEmptyMessage(1);
    }

    private void grantPermissionsToSysComponentsAndPrivApps(int i) {
        Log.i(TAG, "Granting permissions to platform components for user " + i);
        Iterator it = this.mServiceInternal.getPackageList().getPackageNames().iterator();
        while (it.hasNext()) {
            PackageParser.Package r1 = this.mServiceInternal.getPackage((String) it.next());
            if (r1 != null && isSysComponentOrPersistentPlatformSignedPrivApp(r1) && doesPackageSupportRuntimePermissions(r1) && !r1.requestedPermissions.isEmpty()) {
                grantRuntimePermissionsForPackage(i, r1);
            }
        }
    }

    private void grantDefaultSystemHandlerPermissions(int i) {
        PackageManagerInternal.PackagesProvider packagesProvider;
        PackageManagerInternal.PackagesProvider packagesProvider2;
        PackageManagerInternal.PackagesProvider packagesProvider3;
        PackageManagerInternal.PackagesProvider packagesProvider4;
        PackageManagerInternal.PackagesProvider packagesProvider5;
        PackageManagerInternal.PackagesProvider packagesProvider6;
        PackageManagerInternal.SyncAdapterPackagesProvider syncAdapterPackagesProvider;
        String[] packages;
        String[] packages2;
        String[] packages3;
        String[] packages4;
        String[] packages5;
        String[] packages6;
        String[] packages7;
        String[] packages8;
        PackageParser.Package systemPackage;
        PackageParser.Package defaultSystemHandlerActivityPackage;
        Log.i(TAG, "Granting permissions to default platform handlers for user " + i);
        synchronized (this.mLock) {
            packagesProvider = this.mLocationPackagesProvider;
            packagesProvider2 = this.mVoiceInteractionPackagesProvider;
            packagesProvider3 = this.mSmsAppPackagesProvider;
            packagesProvider4 = this.mDialerAppPackagesProvider;
            packagesProvider5 = this.mSimCallManagerPackagesProvider;
            packagesProvider6 = this.mUseOpenWifiAppPackagesProvider;
            syncAdapterPackagesProvider = this.mSyncAdapterPackagesProvider;
        }
        PackageParser.Package defaultSystemHandlerActivityPackage2 = null;
        if (packagesProvider2 != null) {
            packages = packagesProvider2.getPackages(i);
        } else {
            packages = null;
        }
        if (packagesProvider != null) {
            packages2 = packagesProvider.getPackages(i);
        } else {
            packages2 = null;
        }
        if (packagesProvider3 != null) {
            packages3 = packagesProvider3.getPackages(i);
        } else {
            packages3 = null;
        }
        if (packagesProvider4 != null) {
            packages4 = packagesProvider4.getPackages(i);
        } else {
            packages4 = null;
        }
        if (packagesProvider5 != null) {
            packages5 = packagesProvider5.getPackages(i);
        } else {
            packages5 = null;
        }
        if (packagesProvider6 != null) {
            packages6 = packagesProvider6.getPackages(i);
        } else {
            packages6 = null;
        }
        if (syncAdapterPackagesProvider != null) {
            packages7 = syncAdapterPackagesProvider.getPackages("com.android.contacts", i);
        } else {
            packages7 = null;
        }
        if (syncAdapterPackagesProvider != null) {
            packages8 = syncAdapterPackagesProvider.getPackages("com.android.calendar", i);
        } else {
            packages8 = null;
        }
        PackageParser.Package systemPackage2 = getSystemPackage(this.mServiceInternal.getKnownPackageName(2, i));
        if (systemPackage2 != null && doesPackageSupportRuntimePermissions(systemPackage2)) {
            grantRuntimePermissions(systemPackage2, STORAGE_PERMISSIONS, true, i);
        }
        PackageParser.Package systemPackage3 = getSystemPackage(this.mServiceInternal.getKnownPackageName(3, i));
        if (systemPackage3 != null && doesPackageSupportRuntimePermissions(systemPackage3)) {
            grantRuntimePermissions(systemPackage3, STORAGE_PERMISSIONS, true, i);
            grantRuntimePermissions(systemPackage3, PHONE_PERMISSIONS, false, i);
            grantRuntimePermissions(systemPackage3, SMS_PERMISSIONS, false, i);
        }
        PackageParser.Package systemPackage4 = getSystemPackage(this.mServiceInternal.getKnownPackageName(1, i));
        if (systemPackage4 != null && doesPackageSupportRuntimePermissions(systemPackage4)) {
            grantRuntimePermissions(systemPackage4, PHONE_PERMISSIONS, i);
            grantRuntimePermissions(systemPackage4, CONTACTS_PERMISSIONS, i);
            grantRuntimePermissions(systemPackage4, LOCATION_PERMISSIONS, i);
            grantRuntimePermissions(systemPackage4, CAMERA_PERMISSIONS, i);
        }
        PackageParser.Package defaultSystemHandlerActivityPackage3 = getDefaultSystemHandlerActivityPackage(new Intent("android.media.action.IMAGE_CAPTURE"), i);
        if (defaultSystemHandlerActivityPackage3 != null && doesPackageSupportRuntimePermissions(defaultSystemHandlerActivityPackage3)) {
            grantRuntimePermissions(defaultSystemHandlerActivityPackage3, CAMERA_PERMISSIONS, i);
            grantRuntimePermissions(defaultSystemHandlerActivityPackage3, MICROPHONE_PERMISSIONS, i);
            grantRuntimePermissions(defaultSystemHandlerActivityPackage3, STORAGE_PERMISSIONS, i);
        }
        PackageParser.Package defaultProviderAuthorityPackage = getDefaultProviderAuthorityPackage("media", i);
        if (defaultProviderAuthorityPackage != null) {
            grantRuntimePermissions(defaultProviderAuthorityPackage, STORAGE_PERMISSIONS, true, i);
            grantRuntimePermissions(defaultProviderAuthorityPackage, PHONE_PERMISSIONS, true, i);
        }
        PackageParser.Package defaultProviderAuthorityPackage2 = getDefaultProviderAuthorityPackage("downloads", i);
        if (defaultProviderAuthorityPackage2 != null) {
            grantRuntimePermissions(defaultProviderAuthorityPackage2, STORAGE_PERMISSIONS, true, i);
        }
        PackageParser.Package defaultSystemHandlerActivityPackage4 = getDefaultSystemHandlerActivityPackage(new Intent("android.intent.action.VIEW_DOWNLOADS"), i);
        if (defaultSystemHandlerActivityPackage4 != null && doesPackageSupportRuntimePermissions(defaultSystemHandlerActivityPackage4)) {
            grantRuntimePermissions(defaultSystemHandlerActivityPackage4, STORAGE_PERMISSIONS, true, i);
        }
        PackageParser.Package defaultProviderAuthorityPackage3 = getDefaultProviderAuthorityPackage("com.android.externalstorage.documents", i);
        if (defaultProviderAuthorityPackage3 != null) {
            grantRuntimePermissions(defaultProviderAuthorityPackage3, STORAGE_PERMISSIONS, true, i);
        }
        PackageParser.Package systemPackage5 = getSystemPackage(PackageManagerService.DEFAULT_CONTAINER_PACKAGE);
        if (systemPackage5 != null) {
            grantRuntimePermissions(systemPackage5, STORAGE_PERMISSIONS, true, i);
        }
        PackageParser.Package defaultSystemHandlerActivityPackage5 = getDefaultSystemHandlerActivityPackage(new Intent("android.credentials.INSTALL"), i);
        if (defaultSystemHandlerActivityPackage5 != null && doesPackageSupportRuntimePermissions(defaultSystemHandlerActivityPackage5)) {
            grantRuntimePermissions(defaultSystemHandlerActivityPackage5, STORAGE_PERMISSIONS, true, i);
        }
        if (packages4 != null) {
            for (String str : packages4) {
                PackageParser.Package systemPackage6 = getSystemPackage(str);
                if (systemPackage6 != null) {
                    grantDefaultPermissionsToDefaultSystemDialerApp(systemPackage6, i);
                }
            }
        } else {
            PackageParser.Package defaultSystemHandlerActivityPackage6 = getDefaultSystemHandlerActivityPackage(new Intent("android.intent.action.DIAL"), i);
            if (defaultSystemHandlerActivityPackage6 != null) {
                grantDefaultPermissionsToDefaultSystemDialerApp(defaultSystemHandlerActivityPackage6, i);
            }
        }
        if (packages5 != null) {
            for (String str2 : packages5) {
                PackageParser.Package systemPackage7 = getSystemPackage(str2);
                if (systemPackage7 != null) {
                    grantDefaultPermissionsToDefaultSimCallManager(systemPackage7, i);
                }
            }
        }
        if (packages6 != null) {
            for (String str3 : packages6) {
                PackageParser.Package systemPackage8 = getSystemPackage(str3);
                if (systemPackage8 != null) {
                    grantDefaultPermissionsToDefaultSystemUseOpenWifiApp(systemPackage8, i);
                }
            }
        }
        if (packages3 != null) {
            for (String str4 : packages3) {
                PackageParser.Package systemPackage9 = getSystemPackage(str4);
                if (systemPackage9 != null) {
                    grantDefaultPermissionsToDefaultSystemSmsApp(systemPackage9, i);
                }
            }
        } else {
            Intent intent = new Intent("android.intent.action.MAIN");
            intent.addCategory("android.intent.category.APP_MESSAGING");
            PackageParser.Package defaultSystemHandlerActivityPackage7 = getDefaultSystemHandlerActivityPackage(intent, i);
            if (defaultSystemHandlerActivityPackage7 != null) {
                grantDefaultPermissionsToDefaultSystemSmsApp(defaultSystemHandlerActivityPackage7, i);
            }
        }
        PackageParser.Package defaultSystemHandlerActivityPackage8 = getDefaultSystemHandlerActivityPackage(new Intent("android.provider.Telephony.SMS_CB_RECEIVED"), i);
        if (defaultSystemHandlerActivityPackage8 != null && doesPackageSupportRuntimePermissions(defaultSystemHandlerActivityPackage8)) {
            grantRuntimePermissions(defaultSystemHandlerActivityPackage8, SMS_PERMISSIONS, i);
        }
        PackageParser.Package defaultSystemHandlerServicePackage = getDefaultSystemHandlerServicePackage(new Intent("android.provider.Telephony.SMS_CARRIER_PROVISION"), i);
        if (defaultSystemHandlerServicePackage != null && doesPackageSupportRuntimePermissions(defaultSystemHandlerServicePackage)) {
            grantRuntimePermissions(defaultSystemHandlerServicePackage, SMS_PERMISSIONS, false, i);
        }
        Intent intent2 = new Intent("android.intent.action.MAIN");
        intent2.addCategory("android.intent.category.APP_CALENDAR");
        PackageParser.Package defaultSystemHandlerActivityPackage9 = getDefaultSystemHandlerActivityPackage(intent2, i);
        if (defaultSystemHandlerActivityPackage9 != null && doesPackageSupportRuntimePermissions(defaultSystemHandlerActivityPackage9)) {
            grantRuntimePermissions(defaultSystemHandlerActivityPackage9, CALENDAR_PERMISSIONS, i);
            grantRuntimePermissions(defaultSystemHandlerActivityPackage9, CONTACTS_PERMISSIONS, i);
        }
        PackageParser.Package defaultProviderAuthorityPackage4 = getDefaultProviderAuthorityPackage("com.android.calendar", i);
        if (defaultProviderAuthorityPackage4 != null) {
            grantRuntimePermissions(defaultProviderAuthorityPackage4, CONTACTS_PERMISSIONS, i);
            grantRuntimePermissions(defaultProviderAuthorityPackage4, CALENDAR_PERMISSIONS, true, i);
            grantRuntimePermissions(defaultProviderAuthorityPackage4, STORAGE_PERMISSIONS, i);
        }
        List<PackageParser.Package> headlessSyncAdapterPackages = getHeadlessSyncAdapterPackages(packages8, i);
        int size = headlessSyncAdapterPackages.size();
        for (int i2 = 0; i2 < size; i2++) {
            PackageParser.Package r6 = headlessSyncAdapterPackages.get(i2);
            if (doesPackageSupportRuntimePermissions(r6)) {
                grantRuntimePermissions(r6, CALENDAR_PERMISSIONS, i);
            }
        }
        Intent intent3 = new Intent("android.intent.action.MAIN");
        intent3.addCategory("android.intent.category.APP_CONTACTS");
        PackageParser.Package defaultSystemHandlerActivityPackage10 = getDefaultSystemHandlerActivityPackage(intent3, i);
        if (defaultSystemHandlerActivityPackage10 != null && doesPackageSupportRuntimePermissions(defaultSystemHandlerActivityPackage10)) {
            grantRuntimePermissions(defaultSystemHandlerActivityPackage10, CONTACTS_PERMISSIONS, i);
            grantRuntimePermissions(defaultSystemHandlerActivityPackage10, PHONE_PERMISSIONS, i);
        }
        List<PackageParser.Package> headlessSyncAdapterPackages2 = getHeadlessSyncAdapterPackages(packages7, i);
        int size2 = headlessSyncAdapterPackages2.size();
        for (int i3 = 0; i3 < size2; i3++) {
            PackageParser.Package r62 = headlessSyncAdapterPackages2.get(i3);
            if (doesPackageSupportRuntimePermissions(r62)) {
                grantRuntimePermissions(r62, CONTACTS_PERMISSIONS, i);
            }
        }
        PackageParser.Package defaultProviderAuthorityPackage5 = getDefaultProviderAuthorityPackage("com.android.contacts", i);
        if (defaultProviderAuthorityPackage5 != null) {
            grantRuntimePermissions(defaultProviderAuthorityPackage5, CONTACTS_PERMISSIONS, true, i);
            grantRuntimePermissions(defaultProviderAuthorityPackage5, PHONE_PERMISSIONS, true, i);
            grantRuntimePermissions(defaultProviderAuthorityPackage5, STORAGE_PERMISSIONS, i);
        }
        PackageParser.Package defaultSystemHandlerActivityPackage11 = getDefaultSystemHandlerActivityPackage(new Intent("android.app.action.PROVISION_MANAGED_DEVICE"), i);
        if (defaultSystemHandlerActivityPackage11 != null && doesPackageSupportRuntimePermissions(defaultSystemHandlerActivityPackage11)) {
            grantRuntimePermissions(defaultSystemHandlerActivityPackage11, CONTACTS_PERMISSIONS, i);
        }
        Intent intent4 = new Intent("android.intent.action.MAIN");
        intent4.addCategory("android.intent.category.APP_MAPS");
        PackageParser.Package defaultSystemHandlerActivityPackage12 = getDefaultSystemHandlerActivityPackage(intent4, i);
        if (defaultSystemHandlerActivityPackage12 != null && doesPackageSupportRuntimePermissions(defaultSystemHandlerActivityPackage12)) {
            grantRuntimePermissions(defaultSystemHandlerActivityPackage12, LOCATION_PERMISSIONS, i);
        }
        Intent intent5 = new Intent("android.intent.action.MAIN");
        intent5.addCategory("android.intent.category.APP_GALLERY");
        PackageParser.Package defaultSystemHandlerActivityPackage13 = getDefaultSystemHandlerActivityPackage(intent5, i);
        if (defaultSystemHandlerActivityPackage13 != null && doesPackageSupportRuntimePermissions(defaultSystemHandlerActivityPackage13)) {
            grantRuntimePermissions(defaultSystemHandlerActivityPackage13, STORAGE_PERMISSIONS, i);
        }
        Intent intent6 = new Intent("android.intent.action.MAIN");
        intent6.addCategory("android.intent.category.APP_EMAIL");
        PackageParser.Package defaultSystemHandlerActivityPackage14 = getDefaultSystemHandlerActivityPackage(intent6, i);
        if (defaultSystemHandlerActivityPackage14 != null && doesPackageSupportRuntimePermissions(defaultSystemHandlerActivityPackage14)) {
            grantRuntimePermissions(defaultSystemHandlerActivityPackage14, CONTACTS_PERMISSIONS, i);
            grantRuntimePermissions(defaultSystemHandlerActivityPackage14, CALENDAR_PERMISSIONS, i);
        }
        String knownPackageName = this.mServiceInternal.getKnownPackageName(4, i);
        if (knownPackageName != null) {
            defaultSystemHandlerActivityPackage2 = getPackage(knownPackageName);
        }
        if (defaultSystemHandlerActivityPackage2 == null) {
            Intent intent7 = new Intent("android.intent.action.MAIN");
            intent7.addCategory("android.intent.category.APP_BROWSER");
            defaultSystemHandlerActivityPackage2 = getDefaultSystemHandlerActivityPackage(intent7, i);
        }
        if (defaultSystemHandlerActivityPackage2 != null && doesPackageSupportRuntimePermissions(defaultSystemHandlerActivityPackage2)) {
            grantRuntimePermissions(defaultSystemHandlerActivityPackage2, LOCATION_PERMISSIONS, i);
        }
        if (packages != null) {
            for (String str5 : packages) {
                PackageParser.Package systemPackage10 = getSystemPackage(str5);
                if (systemPackage10 != null && doesPackageSupportRuntimePermissions(systemPackage10)) {
                    grantRuntimePermissions(systemPackage10, CONTACTS_PERMISSIONS, i);
                    grantRuntimePermissions(systemPackage10, CALENDAR_PERMISSIONS, i);
                    grantRuntimePermissions(systemPackage10, MICROPHONE_PERMISSIONS, i);
                    grantRuntimePermissions(systemPackage10, PHONE_PERMISSIONS, i);
                    grantRuntimePermissions(systemPackage10, SMS_PERMISSIONS, i);
                    grantRuntimePermissions(systemPackage10, LOCATION_PERMISSIONS, i);
                }
            }
        }
        if (ActivityManager.isLowRamDeviceStatic() && (defaultSystemHandlerActivityPackage = getDefaultSystemHandlerActivityPackage(new Intent("android.search.action.GLOBAL_SEARCH"), i)) != null && doesPackageSupportRuntimePermissions(defaultSystemHandlerActivityPackage)) {
            grantRuntimePermissions(defaultSystemHandlerActivityPackage, MICROPHONE_PERMISSIONS, false, i);
            grantRuntimePermissions(defaultSystemHandlerActivityPackage, LOCATION_PERMISSIONS, false, i);
        }
        Intent intent8 = new Intent("android.speech.RecognitionService");
        intent8.addCategory("android.intent.category.DEFAULT");
        PackageParser.Package defaultSystemHandlerServicePackage2 = getDefaultSystemHandlerServicePackage(intent8, i);
        if (defaultSystemHandlerServicePackage2 != null && doesPackageSupportRuntimePermissions(defaultSystemHandlerServicePackage2)) {
            grantRuntimePermissions(defaultSystemHandlerServicePackage2, MICROPHONE_PERMISSIONS, i);
        }
        if (packages2 != null) {
            for (String str6 : packages2) {
                PackageParser.Package systemPackage11 = getSystemPackage(str6);
                if (systemPackage11 != null && doesPackageSupportRuntimePermissions(systemPackage11)) {
                    grantRuntimePermissions(systemPackage11, CONTACTS_PERMISSIONS, i);
                    grantRuntimePermissions(systemPackage11, CALENDAR_PERMISSIONS, i);
                    grantRuntimePermissions(systemPackage11, MICROPHONE_PERMISSIONS, i);
                    grantRuntimePermissions(systemPackage11, PHONE_PERMISSIONS, i);
                    grantRuntimePermissions(systemPackage11, SMS_PERMISSIONS, i);
                    grantRuntimePermissions(systemPackage11, LOCATION_PERMISSIONS, true, i);
                    grantRuntimePermissions(systemPackage11, CAMERA_PERMISSIONS, i);
                    grantRuntimePermissions(systemPackage11, SENSORS_PERMISSIONS, i);
                    grantRuntimePermissions(systemPackage11, STORAGE_PERMISSIONS, i);
                }
            }
        }
        Intent intent9 = new Intent("android.intent.action.VIEW");
        intent9.addCategory("android.intent.category.DEFAULT");
        intent9.setDataAndType(Uri.fromFile(new File("foo.mp3")), AUDIO_MIME_TYPE);
        PackageParser.Package defaultSystemHandlerActivityPackage15 = getDefaultSystemHandlerActivityPackage(intent9, i);
        if (defaultSystemHandlerActivityPackage15 != null && doesPackageSupportRuntimePermissions(defaultSystemHandlerActivityPackage15)) {
            grantRuntimePermissions(defaultSystemHandlerActivityPackage15, STORAGE_PERMISSIONS, i);
        }
        Intent intent10 = new Intent("android.intent.action.MAIN");
        intent10.addCategory("android.intent.category.HOME");
        intent10.addCategory("android.intent.category.LAUNCHER_APP");
        PackageParser.Package defaultSystemHandlerActivityPackage16 = getDefaultSystemHandlerActivityPackage(intent10, i);
        if (defaultSystemHandlerActivityPackage16 != null && doesPackageSupportRuntimePermissions(defaultSystemHandlerActivityPackage16)) {
            grantRuntimePermissions(defaultSystemHandlerActivityPackage16, LOCATION_PERMISSIONS, false, i);
        }
        if (this.mContext.getPackageManager().hasSystemFeature("android.hardware.type.watch", 0)) {
            Intent intent11 = new Intent("android.intent.action.MAIN");
            intent11.addCategory("android.intent.category.HOME_MAIN");
            PackageParser.Package defaultSystemHandlerActivityPackage17 = getDefaultSystemHandlerActivityPackage(intent11, i);
            if (defaultSystemHandlerActivityPackage17 != null && doesPackageSupportRuntimePermissions(defaultSystemHandlerActivityPackage17)) {
                grantRuntimePermissions(defaultSystemHandlerActivityPackage17, CONTACTS_PERMISSIONS, false, i);
                grantRuntimePermissions(defaultSystemHandlerActivityPackage17, PHONE_PERMISSIONS, true, i);
                grantRuntimePermissions(defaultSystemHandlerActivityPackage17, MICROPHONE_PERMISSIONS, false, i);
                grantRuntimePermissions(defaultSystemHandlerActivityPackage17, LOCATION_PERMISSIONS, false, i);
            }
            PackageParser.Package defaultSystemHandlerActivityPackage18 = getDefaultSystemHandlerActivityPackage(new Intent(ACTION_TRACK), i);
            if (defaultSystemHandlerActivityPackage18 != null && doesPackageSupportRuntimePermissions(defaultSystemHandlerActivityPackage18)) {
                grantRuntimePermissions(defaultSystemHandlerActivityPackage18, SENSORS_PERMISSIONS, false, i);
                grantRuntimePermissions(defaultSystemHandlerActivityPackage18, LOCATION_PERMISSIONS, false, i);
            }
        }
        PackageParser.Package systemPackage12 = getSystemPackage("com.android.printspooler");
        if (systemPackage12 != null && doesPackageSupportRuntimePermissions(systemPackage12)) {
            grantRuntimePermissions(systemPackage12, LOCATION_PERMISSIONS, true, i);
        }
        PackageParser.Package defaultSystemHandlerActivityPackage19 = getDefaultSystemHandlerActivityPackage(new Intent("android.telephony.action.EMERGENCY_ASSISTANCE"), i);
        if (defaultSystemHandlerActivityPackage19 != null && doesPackageSupportRuntimePermissions(defaultSystemHandlerActivityPackage19)) {
            grantRuntimePermissions(defaultSystemHandlerActivityPackage19, CONTACTS_PERMISSIONS, true, i);
            grantRuntimePermissions(defaultSystemHandlerActivityPackage19, PHONE_PERMISSIONS, true, i);
        }
        Intent intent12 = new Intent("android.intent.action.VIEW");
        intent12.setType("vnd.android.cursor.item/ndef_msg");
        PackageParser.Package defaultSystemHandlerActivityPackage20 = getDefaultSystemHandlerActivityPackage(intent12, i);
        if (defaultSystemHandlerActivityPackage20 != null && doesPackageSupportRuntimePermissions(defaultSystemHandlerActivityPackage20)) {
            grantRuntimePermissions(defaultSystemHandlerActivityPackage20, CONTACTS_PERMISSIONS, false, i);
            grantRuntimePermissions(defaultSystemHandlerActivityPackage20, PHONE_PERMISSIONS, false, i);
        }
        PackageParser.Package defaultSystemHandlerActivityPackage21 = getDefaultSystemHandlerActivityPackage(new Intent("android.os.storage.action.MANAGE_STORAGE"), i);
        if (defaultSystemHandlerActivityPackage21 != null && doesPackageSupportRuntimePermissions(defaultSystemHandlerActivityPackage21)) {
            grantRuntimePermissions(defaultSystemHandlerActivityPackage21, STORAGE_PERMISSIONS, true, i);
        }
        PackageParser.Package systemPackage13 = getSystemPackage("com.android.companiondevicemanager");
        if (systemPackage13 != null && doesPackageSupportRuntimePermissions(systemPackage13)) {
            grantRuntimePermissions(systemPackage13, LOCATION_PERMISSIONS, true, i);
        }
        PackageParser.Package defaultSystemHandlerActivityPackage22 = getDefaultSystemHandlerActivityPackage(new Intent("android.intent.action.RINGTONE_PICKER"), i);
        if (defaultSystemHandlerActivityPackage22 != null && doesPackageSupportRuntimePermissions(defaultSystemHandlerActivityPackage22)) {
            grantRuntimePermissions(defaultSystemHandlerActivityPackage22, STORAGE_PERMISSIONS, true, i);
        }
        String systemTextClassifierPackageName = this.mContext.getPackageManager().getSystemTextClassifierPackageName();
        if (!TextUtils.isEmpty(systemTextClassifierPackageName) && (systemPackage = getSystemPackage(systemTextClassifierPackageName)) != null && doesPackageSupportRuntimePermissions(systemPackage)) {
            grantRuntimePermissions(systemPackage, PHONE_PERMISSIONS, false, i);
            grantRuntimePermissions(systemPackage, SMS_PERMISSIONS, false, i);
            grantRuntimePermissions(systemPackage, CALENDAR_PERMISSIONS, false, i);
            grantRuntimePermissions(systemPackage, LOCATION_PERMISSIONS, false, i);
            grantRuntimePermissions(systemPackage, CONTACTS_PERMISSIONS, false, i);
        }
        PackageParser.Package systemPackage14 = getSystemPackage(BackupManagerService.SHARED_BACKUP_AGENT_PACKAGE);
        if (systemPackage14 != null) {
            grantRuntimePermissions(systemPackage14, STORAGE_PERMISSIONS, true, i);
        }
        if (this.mPermissionGrantedCallback != null) {
            this.mPermissionGrantedCallback.onDefaultRuntimePermissionsGranted(i);
        }
    }

    private void grantDefaultPermissionsToDefaultSystemDialerApp(PackageParser.Package r4, int i) {
        if (doesPackageSupportRuntimePermissions(r4)) {
            grantRuntimePermissions(r4, PHONE_PERMISSIONS, this.mContext.getPackageManager().hasSystemFeature("android.hardware.type.watch", 0), i);
            grantRuntimePermissions(r4, CONTACTS_PERMISSIONS, i);
            grantRuntimePermissions(r4, SMS_PERMISSIONS, i);
            grantRuntimePermissions(r4, MICROPHONE_PERMISSIONS, i);
            grantRuntimePermissions(r4, CAMERA_PERMISSIONS, i);
        }
    }

    private void grantDefaultPermissionsToDefaultSystemSmsApp(PackageParser.Package r2, int i) {
        if (doesPackageSupportRuntimePermissions(r2)) {
            grantRuntimePermissions(r2, PHONE_PERMISSIONS, i);
            grantRuntimePermissions(r2, CONTACTS_PERMISSIONS, i);
            grantRuntimePermissions(r2, SMS_PERMISSIONS, i);
            grantRuntimePermissions(r2, STORAGE_PERMISSIONS, i);
            grantRuntimePermissions(r2, MICROPHONE_PERMISSIONS, i);
            grantRuntimePermissions(r2, CAMERA_PERMISSIONS, i);
        }
    }

    private void grantDefaultPermissionsToDefaultSystemUseOpenWifiApp(PackageParser.Package r2, int i) {
        if (doesPackageSupportRuntimePermissions(r2)) {
            grantRuntimePermissions(r2, COARSE_LOCATION_PERMISSIONS, i);
        }
    }

    public void grantDefaultPermissionsToDefaultSmsApp(String str, int i) {
        PackageParser.Package r9;
        Log.i(TAG, "Granting permissions to default sms app for user:" + i);
        if (str != null && (r9 = getPackage(str)) != null && doesPackageSupportRuntimePermissions(r9)) {
            grantRuntimePermissions(r9, PHONE_PERMISSIONS, false, true, i);
            grantRuntimePermissions(r9, CONTACTS_PERMISSIONS, false, true, i);
            grantRuntimePermissions(r9, SMS_PERMISSIONS, false, true, i);
            grantRuntimePermissions(r9, STORAGE_PERMISSIONS, false, true, i);
            grantRuntimePermissions(r9, MICROPHONE_PERMISSIONS, false, true, i);
            grantRuntimePermissions(r9, CAMERA_PERMISSIONS, false, true, i);
        }
    }

    public void grantDefaultPermissionsToDefaultDialerApp(String str, int i) {
        PackageParser.Package r9;
        Log.i(TAG, "Granting permissions to default dialer app for user:" + i);
        if (str != null && (r9 = getPackage(str)) != null && doesPackageSupportRuntimePermissions(r9)) {
            grantRuntimePermissions(r9, PHONE_PERMISSIONS, false, true, i);
            grantRuntimePermissions(r9, CONTACTS_PERMISSIONS, false, true, i);
            grantRuntimePermissions(r9, SMS_PERMISSIONS, false, true, i);
            grantRuntimePermissions(r9, MICROPHONE_PERMISSIONS, false, true, i);
            grantRuntimePermissions(r9, CAMERA_PERMISSIONS, false, true, i);
        }
    }

    public void grantDefaultPermissionsToDefaultUseOpenWifiApp(String str, int i) {
        PackageParser.Package r3;
        Log.i(TAG, "Granting permissions to default Use Open WiFi app for user:" + i);
        if (str != null && (r3 = getPackage(str)) != null && doesPackageSupportRuntimePermissions(r3)) {
            grantRuntimePermissions(r3, COARSE_LOCATION_PERMISSIONS, false, true, i);
        }
    }

    private void grantDefaultPermissionsToDefaultSimCallManager(PackageParser.Package r4, int i) {
        Log.i(TAG, "Granting permissions to sim call manager for user:" + i);
        if (doesPackageSupportRuntimePermissions(r4)) {
            grantRuntimePermissions(r4, PHONE_PERMISSIONS, i);
            grantRuntimePermissions(r4, MICROPHONE_PERMISSIONS, i);
        }
    }

    public void grantDefaultPermissionsToDefaultSimCallManager(String str, int i) {
        PackageParser.Package r1;
        if (str != null && (r1 = getPackage(str)) != null) {
            grantDefaultPermissionsToDefaultSimCallManager(r1, i);
        }
    }

    public void grantDefaultPermissionsToEnabledCarrierApps(String[] strArr, int i) {
        Log.i(TAG, "Granting permissions to enabled carrier apps for user:" + i);
        if (strArr == null) {
            return;
        }
        for (String str : strArr) {
            PackageParser.Package systemPackage = getSystemPackage(str);
            if (systemPackage != null && doesPackageSupportRuntimePermissions(systemPackage)) {
                grantRuntimePermissions(systemPackage, PHONE_PERMISSIONS, i);
                grantRuntimePermissions(systemPackage, LOCATION_PERMISSIONS, i);
                grantRuntimePermissions(systemPackage, SMS_PERMISSIONS, i);
            }
        }
    }

    public void grantDefaultPermissionsToEnabledImsServices(String[] strArr, int i) {
        Log.i(TAG, "Granting permissions to enabled ImsServices for user:" + i);
        if (strArr == null) {
            return;
        }
        for (String str : strArr) {
            PackageParser.Package systemPackage = getSystemPackage(str);
            if (systemPackage != null && doesPackageSupportRuntimePermissions(systemPackage)) {
                grantRuntimePermissions(systemPackage, PHONE_PERMISSIONS, i);
                grantRuntimePermissions(systemPackage, MICROPHONE_PERMISSIONS, i);
                grantRuntimePermissions(systemPackage, LOCATION_PERMISSIONS, i);
                grantRuntimePermissions(systemPackage, CAMERA_PERMISSIONS, i);
                grantRuntimePermissions(systemPackage, CONTACTS_PERMISSIONS, i);
            }
        }
    }

    public void grantDefaultPermissionsToEnabledTelephonyDataServices(String[] strArr, int i) {
        Log.i(TAG, "Granting permissions to enabled data services for user:" + i);
        if (strArr == null) {
            return;
        }
        for (String str : strArr) {
            PackageParser.Package systemPackage = getSystemPackage(str);
            if (systemPackage != null && doesPackageSupportRuntimePermissions(systemPackage)) {
                grantRuntimePermissions(systemPackage, PHONE_PERMISSIONS, true, i);
                grantRuntimePermissions(systemPackage, LOCATION_PERMISSIONS, true, i);
            }
        }
    }

    public void revokeDefaultPermissionsFromDisabledTelephonyDataServices(String[] strArr, int i) {
        Log.i(TAG, "Revoking permissions from disabled data services for user:" + i);
        if (strArr == null) {
            return;
        }
        for (String str : strArr) {
            PackageParser.Package systemPackage = getSystemPackage(str);
            if (systemPackage != null && doesPackageSupportRuntimePermissions(systemPackage)) {
                revokeRuntimePermissions(systemPackage, PHONE_PERMISSIONS, true, i);
                revokeRuntimePermissions(systemPackage, LOCATION_PERMISSIONS, true, i);
            }
        }
    }

    public void grantDefaultPermissionsToActiveLuiApp(String str, int i) {
        PackageParser.Package systemPackage;
        Log.i(TAG, "Granting permissions to active LUI app for user:" + i);
        if (str != null && (systemPackage = getSystemPackage(str)) != null && doesPackageSupportRuntimePermissions(systemPackage)) {
            grantRuntimePermissions(systemPackage, CAMERA_PERMISSIONS, true, i);
        }
    }

    public void revokeDefaultPermissionsFromLuiApps(String[] strArr, int i) {
        Log.i(TAG, "Revoke permissions from LUI apps for user:" + i);
        if (strArr == null) {
            return;
        }
        for (String str : strArr) {
            PackageParser.Package systemPackage = getSystemPackage(str);
            if (systemPackage != null && doesPackageSupportRuntimePermissions(systemPackage)) {
                revokeRuntimePermissions(systemPackage, CAMERA_PERMISSIONS, true, i);
            }
        }
    }

    public void grantDefaultPermissionsToDefaultBrowser(String str, int i) {
        PackageParser.Package systemPackage;
        Log.i(TAG, "Granting permissions to default browser for user:" + i);
        if (str != null && (systemPackage = getSystemPackage(str)) != null && doesPackageSupportRuntimePermissions(systemPackage)) {
            grantRuntimePermissions(systemPackage, LOCATION_PERMISSIONS, false, false, i);
        }
    }

    private PackageParser.Package getDefaultSystemHandlerActivityPackage(Intent intent, int i) {
        ResolveInfo resolveInfoResolveIntent = this.mServiceInternal.resolveIntent(intent, intent.resolveType(this.mContext.getContentResolver()), DEFAULT_FLAGS, i, false, Binder.getCallingUid());
        if (resolveInfoResolveIntent == null || resolveInfoResolveIntent.activityInfo == null || this.mServiceInternal.isResolveActivityComponent(resolveInfoResolveIntent.activityInfo)) {
            return null;
        }
        return getSystemPackage(resolveInfoResolveIntent.activityInfo.packageName);
    }

    private PackageParser.Package getDefaultSystemHandlerServicePackage(Intent intent, int i) {
        List listQueryIntentServices = this.mServiceInternal.queryIntentServices(intent, DEFAULT_FLAGS, Binder.getCallingUid(), i);
        if (listQueryIntentServices == null) {
            return null;
        }
        int size = listQueryIntentServices.size();
        for (int i2 = 0; i2 < size; i2++) {
            PackageParser.Package systemPackage = getSystemPackage(((ResolveInfo) listQueryIntentServices.get(i2)).serviceInfo.packageName);
            if (systemPackage != null) {
                return systemPackage;
            }
        }
        return null;
    }

    private List<PackageParser.Package> getHeadlessSyncAdapterPackages(String[] strArr, int i) {
        PackageParser.Package systemPackage;
        ArrayList arrayList = new ArrayList();
        Intent intent = new Intent("android.intent.action.MAIN");
        intent.addCategory("android.intent.category.LAUNCHER");
        for (String str : strArr) {
            intent.setPackage(str);
            if (this.mServiceInternal.resolveIntent(intent, intent.resolveType(this.mContext.getContentResolver()), DEFAULT_FLAGS, i, false, Binder.getCallingUid()) == null && (systemPackage = getSystemPackage(str)) != null) {
                arrayList.add(systemPackage);
            }
        }
        return arrayList;
    }

    private PackageParser.Package getDefaultProviderAuthorityPackage(String str, int i) {
        ProviderInfo providerInfoResolveContentProvider = this.mServiceInternal.resolveContentProvider(str, DEFAULT_FLAGS, i);
        if (providerInfoResolveContentProvider != null) {
            return getSystemPackage(providerInfoResolveContentProvider.packageName);
        }
        return null;
    }

    private PackageParser.Package getPackage(String str) {
        return this.mServiceInternal.getPackage(str);
    }

    private PackageParser.Package getSystemPackage(String str) {
        PackageParser.Package r3 = getPackage(str);
        if (r3 == null || !r3.isSystem() || isSysComponentOrPersistentPlatformSignedPrivApp(r3)) {
            return null;
        }
        return r3;
    }

    private void grantRuntimePermissions(PackageParser.Package r7, Set<String> set, int i) {
        grantRuntimePermissions(r7, set, false, false, i);
    }

    private void grantRuntimePermissions(PackageParser.Package r7, Set<String> set, boolean z, int i) {
        grantRuntimePermissions(r7, set, z, false, i);
    }

    private void revokeRuntimePermissions(PackageParser.Package r9, Set<String> set, boolean z, int i) {
        if (r9.requestedPermissions.isEmpty()) {
            return;
        }
        ArraySet arraySet = new ArraySet(r9.requestedPermissions);
        for (String str : set) {
            if (arraySet.contains(str)) {
                int permissionFlagsTEMP = this.mServiceInternal.getPermissionFlagsTEMP(str, r9.packageName, i);
                if ((permissionFlagsTEMP & 32) != 0 && (permissionFlagsTEMP & 4) == 0 && ((permissionFlagsTEMP & 16) == 0 || z)) {
                    this.mServiceInternal.revokeRuntimePermission(r9.packageName, str, i, false);
                    this.mServiceInternal.updatePermissionFlagsTEMP(str, r9.packageName, 32, 0, i);
                }
            }
        }
    }

    private void grantRuntimePermissions(PackageParser.Package r17, Set<String> set, boolean z, boolean z2, int i) {
        int i2;
        String str;
        PackageParser.Package disabledPackage;
        if (r17.requestedPermissions.isEmpty()) {
            return;
        }
        ArrayList arrayList = r17.requestedPermissions;
        ArraySet arraySet = null;
        if (!z2 && r17.isUpdatedSystemApp() && (disabledPackage = this.mServiceInternal.getDisabledPackage(r17.packageName)) != null) {
            if (disabledPackage.requestedPermissions.isEmpty()) {
                return;
            }
            if (!arrayList.equals(disabledPackage.requestedPermissions)) {
                arraySet = new ArraySet(arrayList);
                arrayList = disabledPackage.requestedPermissions;
            }
        }
        ArrayList arrayList2 = arrayList;
        Set set2 = arraySet;
        int size = arrayList2.size();
        for (int i3 = 0; i3 < size; i3++) {
            String str2 = (String) arrayList2.get(i3);
            if ((set2 == null || set2.contains(str2)) && set.contains(str2)) {
                int permissionFlagsTEMP = this.mServiceInternal.getPermissionFlagsTEMP(str2, r17.packageName, i);
                if (permissionFlagsTEMP == 0 || z2) {
                    if ((permissionFlagsTEMP & 4) == 0) {
                        this.mServiceInternal.grantRuntimePermission(r17.packageName, str2, i, false);
                        if (z) {
                            i2 = 48;
                        } else {
                            i2 = 32;
                        }
                        int i4 = i2;
                        str = str2;
                        this.mServiceInternal.updatePermissionFlagsTEMP(str2, r17.packageName, i4, i4, i);
                    }
                } else {
                    str = str2;
                }
                if ((permissionFlagsTEMP & 32) != 0 && (permissionFlagsTEMP & 16) != 0 && !z) {
                    this.mServiceInternal.updatePermissionFlagsTEMP(str, r17.packageName, 16, 0, i);
                }
            }
        }
    }

    private boolean isSysComponentOrPersistentPlatformSignedPrivApp(PackageParser.Package r6) {
        if (UserHandle.getAppId(r6.applicationInfo.uid) < 10000) {
            return true;
        }
        if (!r6.isPrivileged()) {
            return false;
        }
        PackageParser.Package disabledPackage = this.mServiceInternal.getDisabledPackage(r6.packageName);
        if (disabledPackage != null) {
            if ((disabledPackage.applicationInfo.flags & 8) == 0) {
                return false;
            }
        } else if ((r6.applicationInfo.flags & 8) == 0) {
            return false;
        }
        PackageParser.Package r0 = getPackage(this.mServiceInternal.getKnownPackageName(0, 0));
        return r6.mSigningDetails.hasAncestorOrSelf(r0.mSigningDetails) || r0.mSigningDetails.checkCapability(r6.mSigningDetails, 4);
    }

    private void grantDefaultPermissionExceptions(int i) {
        this.mHandler.removeMessages(1);
        synchronized (this.mLock) {
            if (this.mGrantExceptions == null) {
                this.mGrantExceptions = readDefaultPermissionExceptionsLocked();
            }
        }
        int size = this.mGrantExceptions.size();
        ArraySet arraySet = null;
        int i2 = 0;
        while (i2 < size) {
            PackageParser.Package systemPackage = getSystemPackage(this.mGrantExceptions.keyAt(i2));
            List<DefaultPermissionGrant> listValueAt = this.mGrantExceptions.valueAt(i2);
            int size2 = listValueAt.size();
            ArraySet arraySet2 = arraySet;
            for (int i3 = 0; i3 < size2; i3++) {
                DefaultPermissionGrant defaultPermissionGrant = listValueAt.get(i3);
                if (arraySet2 == null) {
                    arraySet2 = new ArraySet();
                } else {
                    arraySet2.clear();
                }
                arraySet2.add(defaultPermissionGrant.name);
                grantRuntimePermissions(systemPackage, arraySet2, defaultPermissionGrant.fixed, i);
            }
            i2++;
            arraySet = arraySet2;
        }
    }

    private File[] getDefaultPermissionFiles() {
        ArrayList arrayList = new ArrayList();
        File file = new File(Environment.getRootDirectory(), "etc/default-permissions");
        if (file.isDirectory() && file.canRead()) {
            Collections.addAll(arrayList, file.listFiles());
        }
        File file2 = new File(Environment.getVendorDirectory(), "etc/default-permissions");
        if (file2.isDirectory() && file2.canRead()) {
            Collections.addAll(arrayList, file2.listFiles());
        }
        File file3 = new File(Environment.getOdmDirectory(), "etc/default-permissions");
        if (file3.isDirectory() && file3.canRead()) {
            Collections.addAll(arrayList, file3.listFiles());
        }
        File file4 = new File(Environment.getProductDirectory(), "etc/default-permissions");
        if (file4.isDirectory() && file4.canRead()) {
            Collections.addAll(arrayList, file4.listFiles());
        }
        if (this.mContext.getPackageManager().hasSystemFeature("android.hardware.type.embedded", 0)) {
            File file5 = new File(Environment.getOemDirectory(), "etc/default-permissions");
            if (file5.isDirectory() && file5.canRead()) {
                Collections.addAll(arrayList, file5.listFiles());
            }
        }
        if (arrayList.isEmpty()) {
            return null;
        }
        return (File[]) arrayList.toArray(new File[0]);
    }

    private ArrayMap<String, List<DefaultPermissionGrant>> readDefaultPermissionExceptionsLocked() {
        File[] defaultPermissionFiles = getDefaultPermissionFiles();
        if (defaultPermissionFiles == null) {
            return new ArrayMap<>(0);
        }
        ArrayMap<String, List<DefaultPermissionGrant>> arrayMap = new ArrayMap<>();
        for (File file : defaultPermissionFiles) {
            if (!file.getPath().endsWith(".xml")) {
                Slog.i(TAG, "Non-xml file " + file + " in " + file.getParent() + " directory, ignoring");
            } else if (!file.canRead()) {
                Slog.w(TAG, "Default permissions file " + file + " cannot be read");
            } else {
                try {
                    InputStream bufferedInputStream = new BufferedInputStream(new FileInputStream(file));
                    Throwable th = null;
                    try {
                        XmlPullParser xmlPullParserNewPullParser = Xml.newPullParser();
                        xmlPullParserNewPullParser.setInput(bufferedInputStream, null);
                        parse(xmlPullParserNewPullParser, arrayMap);
                        bufferedInputStream.close();
                    } catch (Throwable th2) {
                        if (th != null) {
                            try {
                                bufferedInputStream.close();
                            } catch (Throwable th3) {
                                th.addSuppressed(th3);
                            }
                        } else {
                            bufferedInputStream.close();
                        }
                        throw th2;
                    }
                } catch (IOException | XmlPullParserException e) {
                    Slog.w(TAG, "Error reading default permissions file " + file, e);
                }
            }
        }
        return arrayMap;
    }

    private void parse(XmlPullParser xmlPullParser, Map<String, List<DefaultPermissionGrant>> map) throws XmlPullParserException, IOException {
        int depth = xmlPullParser.getDepth();
        while (true) {
            int next = xmlPullParser.next();
            if (next != 1) {
                if (next != 3 || xmlPullParser.getDepth() > depth) {
                    if (next != 3 && next != 4) {
                        if (TAG_EXCEPTIONS.equals(xmlPullParser.getName())) {
                            parseExceptions(xmlPullParser, map);
                        } else {
                            Log.e(TAG, "Unknown tag " + xmlPullParser.getName());
                        }
                    }
                } else {
                    return;
                }
            } else {
                return;
            }
        }
    }

    private void parseExceptions(XmlPullParser xmlPullParser, Map<String, List<DefaultPermissionGrant>> map) throws XmlPullParserException, IOException {
        int depth = xmlPullParser.getDepth();
        while (true) {
            int next = xmlPullParser.next();
            if (next != 1) {
                if (next != 3 || xmlPullParser.getDepth() > depth) {
                    if (next != 3 && next != 4) {
                        if (TAG_EXCEPTION.equals(xmlPullParser.getName())) {
                            String attributeValue = xmlPullParser.getAttributeValue(null, "package");
                            List<DefaultPermissionGrant> arrayList = map.get(attributeValue);
                            if (arrayList == null) {
                                PackageParser.Package systemPackage = getSystemPackage(attributeValue);
                                if (systemPackage == null) {
                                    Log.w(TAG, "Unknown package:" + attributeValue);
                                    XmlUtils.skipCurrentTag(xmlPullParser);
                                } else if (!doesPackageSupportRuntimePermissions(systemPackage)) {
                                    Log.w(TAG, "Skipping non supporting runtime permissions package:" + attributeValue);
                                    XmlUtils.skipCurrentTag(xmlPullParser);
                                } else {
                                    arrayList = new ArrayList<>();
                                    map.put(attributeValue, arrayList);
                                }
                            }
                            parsePermission(xmlPullParser, arrayList);
                        } else {
                            Log.e(TAG, "Unknown tag " + xmlPullParser.getName() + "under <exceptions>");
                        }
                    }
                } else {
                    return;
                }
            } else {
                return;
            }
        }
    }

    private void parsePermission(XmlPullParser xmlPullParser, List<DefaultPermissionGrant> list) throws XmlPullParserException, IOException {
        int depth = xmlPullParser.getDepth();
        while (true) {
            int next = xmlPullParser.next();
            if (next != 1) {
                if (next != 3 || xmlPullParser.getDepth() > depth) {
                    if (next != 3 && next != 4) {
                        if (TAG_PERMISSION.contains(xmlPullParser.getName())) {
                            String attributeValue = xmlPullParser.getAttributeValue(null, "name");
                            if (attributeValue == null) {
                                Log.w(TAG, "Mandatory name attribute missing for permission tag");
                                XmlUtils.skipCurrentTag(xmlPullParser);
                            } else {
                                list.add(new DefaultPermissionGrant(attributeValue, XmlUtils.readBooleanAttribute(xmlPullParser, ATTR_FIXED)));
                            }
                        } else {
                            Log.e(TAG, "Unknown tag " + xmlPullParser.getName() + "under <exception>");
                        }
                    }
                } else {
                    return;
                }
            } else {
                return;
            }
        }
    }

    private static boolean doesPackageSupportRuntimePermissions(PackageParser.Package r1) {
        return r1.applicationInfo.targetSdkVersion > 22;
    }

    private static final class DefaultPermissionGrant {
        final boolean fixed;
        final String name;

        public DefaultPermissionGrant(String str, boolean z) {
            this.name = str;
            this.fixed = z;
        }
    }

    public void grantCtaPermToPreInstalledPackage(int i) {
        Log.d(TAG, "grantCtaPermToPreInstalledPackage userId = " + i);
        synchronized (this.mLock) {
            Iterator it = this.mServiceInternal.getPackageList().getPackageNames().iterator();
            while (it.hasNext()) {
                PackageParser.Package r5 = this.mServiceInternal.getPackage((String) it.next());
                if (doesPackageSupportRuntimePermissions(r5) && !r5.requestedPermissions.isEmpty()) {
                    ArraySet arraySet = new ArraySet();
                    for (String str : r5.requestedPermissions) {
                        BasePermission permission = this.mPermissionManager.getPermission(str);
                        if (permission != null && permission.isRuntime() && CTA_MANAGER.isCtaOnlyPermission(permission.getName())) {
                            arraySet.add(str);
                        }
                    }
                    if (!arraySet.isEmpty()) {
                        grantRuntimePermissions(r5, arraySet, isSysComponentOrPersistentPlatformSignedPrivApp(r5), false, i);
                    }
                }
            }
        }
    }
}
