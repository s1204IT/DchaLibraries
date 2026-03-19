package com.android.server.pm;

import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManagerInternal;
import android.content.pm.PackageParser;
import android.content.pm.ProviderInfo;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.UserHandle;
import android.util.ArraySet;
import android.util.Log;
import com.mediatek.Manifest;
import com.mediatek.cta.CtaUtils;
import com.mediatek.internal.R;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

final class DefaultPermissionGrantPolicy {
    private static final String AUDIO_MIME_TYPE = "audio/mpeg";
    private static final Set<String> CALENDAR_PERMISSIONS;
    private static final Set<String> CAMERA_PERMISSIONS;
    private static final Set<String> CONTACTS_PERMISSIONS;
    private static final boolean DEBUG = false;
    private static final int DEFAULT_FLAGS = 786432;
    private static final Set<String> LOCATION_PERMISSIONS;
    private static final Set<String> MICROPHONE_PERMISSIONS;
    private static final Set<String> PHONE_PERMISSIONS = new ArraySet();
    private static final Set<String> SENSORS_PERMISSIONS;
    private static final Set<String> SMS_PERMISSIONS;
    private static final Set<String> STORAGE_PERMISSIONS;
    private static final String TAG = "DefaultPermGrantPolicy";
    private static HashSet<String> mForcePermReviewPkgs;
    private PackageManagerInternal.PackagesProvider mDialerAppPackagesProvider;
    private PackageManagerInternal.PackagesProvider mLocationPackagesProvider;
    private final PackageManagerService mService;
    private PackageManagerInternal.PackagesProvider mSimCallManagerPackagesProvider;
    private PackageManagerInternal.PackagesProvider mSmsAppPackagesProvider;
    private PackageManagerInternal.SyncAdapterPackagesProvider mSyncAdapterPackagesProvider;
    private PackageManagerInternal.PackagesProvider mVoiceInteractionPackagesProvider;

    static {
        PHONE_PERMISSIONS.add("android.permission.READ_PHONE_STATE");
        PHONE_PERMISSIONS.add("android.permission.CALL_PHONE");
        PHONE_PERMISSIONS.add("android.permission.READ_CALL_LOG");
        PHONE_PERMISSIONS.add("android.permission.WRITE_CALL_LOG");
        PHONE_PERMISSIONS.add("com.android.voicemail.permission.ADD_VOICEMAIL");
        PHONE_PERMISSIONS.add("android.permission.USE_SIP");
        PHONE_PERMISSIONS.add("android.permission.PROCESS_OUTGOING_CALLS");
        if (CtaUtils.isCtaSupported()) {
            PHONE_PERMISSIONS.add(Manifest.permission.CTA_CONFERENCE_CALL);
        }
        CONTACTS_PERMISSIONS = new ArraySet();
        CONTACTS_PERMISSIONS.add("android.permission.READ_CONTACTS");
        CONTACTS_PERMISSIONS.add("android.permission.WRITE_CONTACTS");
        CONTACTS_PERMISSIONS.add("android.permission.GET_ACCOUNTS");
        LOCATION_PERMISSIONS = new ArraySet();
        LOCATION_PERMISSIONS.add("android.permission.ACCESS_FINE_LOCATION");
        LOCATION_PERMISSIONS.add("android.permission.ACCESS_COARSE_LOCATION");
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
        if (CtaUtils.isCtaSupported()) {
            SMS_PERMISSIONS.add(Manifest.permission.CTA_SEND_MMS);
        }
        MICROPHONE_PERMISSIONS = new ArraySet();
        MICROPHONE_PERMISSIONS.add("android.permission.RECORD_AUDIO");
        CAMERA_PERMISSIONS = new ArraySet();
        CAMERA_PERMISSIONS.add("android.permission.CAMERA");
        SENSORS_PERMISSIONS = new ArraySet();
        SENSORS_PERMISSIONS.add("android.permission.BODY_SENSORS");
        STORAGE_PERMISSIONS = new ArraySet();
        STORAGE_PERMISSIONS.add("android.permission.READ_EXTERNAL_STORAGE");
        STORAGE_PERMISSIONS.add("android.permission.WRITE_EXTERNAL_STORAGE");
        mForcePermReviewPkgs = null;
    }

    public DefaultPermissionGrantPolicy(PackageManagerService service) {
        this.mService = service;
    }

    public void setLocationPackagesProviderLPw(PackageManagerInternal.PackagesProvider provider) {
        this.mLocationPackagesProvider = provider;
    }

    public void setVoiceInteractionPackagesProviderLPw(PackageManagerInternal.PackagesProvider provider) {
        this.mVoiceInteractionPackagesProvider = provider;
    }

    public void setSmsAppPackagesProviderLPw(PackageManagerInternal.PackagesProvider provider) {
        this.mSmsAppPackagesProvider = provider;
    }

    public void setDialerAppPackagesProviderLPw(PackageManagerInternal.PackagesProvider provider) {
        this.mDialerAppPackagesProvider = provider;
    }

    public void setSimCallManagerPackagesProviderLPw(PackageManagerInternal.PackagesProvider provider) {
        this.mSimCallManagerPackagesProvider = provider;
    }

    public void setSyncAdapterPackagesProviderLPw(PackageManagerInternal.SyncAdapterPackagesProvider provider) {
        this.mSyncAdapterPackagesProvider = provider;
    }

    public void grantDefaultPermissions(int userId) {
        grantPermissionsToSysComponentsAndPrivApps(userId);
        grantDefaultSystemHandlerPermissions(userId);
        if (!CtaUtils.isCtaSupported()) {
            return;
        }
        grantCtaPermToPreInstalledPackage(userId);
    }

    private void grantPermissionsToSysComponentsAndPrivApps(int userId) {
        Log.i(TAG, "Granting permissions to platform components for user " + userId);
        synchronized (this.mService.mPackages) {
            for (PackageParser.Package pkg : this.mService.mPackages.values()) {
                if (isSysComponentOrPersistentPlatformSignedPrivAppLPr(pkg) && doesPackageSupportRuntimePermissions(pkg) && !pkg.requestedPermissions.isEmpty()) {
                    Set<String> permissions = new ArraySet<>();
                    int permissionCount = pkg.requestedPermissions.size();
                    for (int i = 0; i < permissionCount; i++) {
                        String permission = (String) pkg.requestedPermissions.get(i);
                        BasePermission bp = this.mService.mSettings.mPermissions.get(permission);
                        if (bp != null && bp.isRuntime()) {
                            permissions.add(permission);
                        }
                    }
                    if (!permissions.isEmpty()) {
                        grantRuntimePermissionsLPw(pkg, permissions, true, userId);
                    }
                }
            }
        }
    }

    private void grantDefaultSystemHandlerPermissions(int userId) {
        PackageManagerInternal.PackagesProvider locationPackagesProvider;
        PackageManagerInternal.PackagesProvider voiceInteractionPackagesProvider;
        PackageManagerInternal.PackagesProvider smsAppPackagesProvider;
        PackageManagerInternal.PackagesProvider dialerAppPackagesProvider;
        PackageManagerInternal.PackagesProvider simCallManagerPackagesProvider;
        PackageManagerInternal.SyncAdapterPackagesProvider syncAdapterPackagesProvider;
        Log.i(TAG, "Granting permissions to default platform handlers for user " + userId);
        synchronized (this.mService.mPackages) {
            locationPackagesProvider = this.mLocationPackagesProvider;
            voiceInteractionPackagesProvider = this.mVoiceInteractionPackagesProvider;
            smsAppPackagesProvider = this.mSmsAppPackagesProvider;
            dialerAppPackagesProvider = this.mDialerAppPackagesProvider;
            simCallManagerPackagesProvider = this.mSimCallManagerPackagesProvider;
            syncAdapterPackagesProvider = this.mSyncAdapterPackagesProvider;
        }
        String[] packages = voiceInteractionPackagesProvider != null ? voiceInteractionPackagesProvider.getPackages(userId) : null;
        String[] packages2 = locationPackagesProvider != null ? locationPackagesProvider.getPackages(userId) : null;
        String[] packages3 = smsAppPackagesProvider != null ? smsAppPackagesProvider.getPackages(userId) : null;
        String[] packages4 = dialerAppPackagesProvider != null ? dialerAppPackagesProvider.getPackages(userId) : null;
        String[] packages5 = simCallManagerPackagesProvider != null ? simCallManagerPackagesProvider.getPackages(userId) : null;
        String[] packages6 = syncAdapterPackagesProvider != null ? syncAdapterPackagesProvider.getPackages("com.android.contacts", userId) : null;
        String[] packages7 = syncAdapterPackagesProvider != null ? syncAdapterPackagesProvider.getPackages("com.android.calendar", userId) : null;
        synchronized (this.mService.mPackages) {
            PackageParser.Package installerPackage = getSystemPackageLPr(this.mService.mRequiredInstallerPackage);
            if (installerPackage != null && doesPackageSupportRuntimePermissions(installerPackage)) {
                grantRuntimePermissionsLPw(installerPackage, STORAGE_PERMISSIONS, true, userId);
            }
            PackageParser.Package verifierPackage = getSystemPackageLPr(this.mService.mRequiredVerifierPackage);
            if (verifierPackage != null && doesPackageSupportRuntimePermissions(verifierPackage)) {
                grantRuntimePermissionsLPw(verifierPackage, STORAGE_PERMISSIONS, true, userId);
                grantRuntimePermissionsLPw(verifierPackage, PHONE_PERMISSIONS, false, userId);
                grantRuntimePermissionsLPw(verifierPackage, SMS_PERMISSIONS, false, userId);
            }
            PackageParser.Package setupPackage = getSystemPackageLPr(this.mService.mSetupWizardPackage);
            if (setupPackage != null && doesPackageSupportRuntimePermissions(setupPackage)) {
                grantRuntimePermissionsLPw(setupPackage, PHONE_PERMISSIONS, userId);
                grantRuntimePermissionsLPw(setupPackage, CONTACTS_PERMISSIONS, userId);
                grantRuntimePermissionsLPw(setupPackage, LOCATION_PERMISSIONS, userId);
                grantRuntimePermissionsLPw(setupPackage, CAMERA_PERMISSIONS, userId);
            }
            Intent cameraIntent = new Intent("android.media.action.IMAGE_CAPTURE");
            PackageParser.Package cameraPackage = getDefaultSystemHandlerActivityPackageLPr(cameraIntent, userId);
            if (cameraPackage != null && doesPackageSupportRuntimePermissions(cameraPackage)) {
                grantRuntimePermissionsLPw(cameraPackage, CAMERA_PERMISSIONS, userId);
                grantRuntimePermissionsLPw(cameraPackage, MICROPHONE_PERMISSIONS, userId);
                grantRuntimePermissionsLPw(cameraPackage, STORAGE_PERMISSIONS, userId);
            }
            PackageParser.Package mediaStorePackage = getDefaultProviderAuthorityPackageLPr("media", userId);
            if (mediaStorePackage != null) {
                grantRuntimePermissionsLPw(mediaStorePackage, STORAGE_PERMISSIONS, true, userId);
            }
            PackageParser.Package downloadsPackage = getDefaultProviderAuthorityPackageLPr("downloads", userId);
            if (downloadsPackage != null) {
                grantRuntimePermissionsLPw(downloadsPackage, STORAGE_PERMISSIONS, true, userId);
            }
            Intent downloadsUiIntent = new Intent("android.intent.action.VIEW_DOWNLOADS");
            PackageParser.Package downloadsUiPackage = getDefaultSystemHandlerActivityPackageLPr(downloadsUiIntent, userId);
            if (downloadsUiPackage != null && doesPackageSupportRuntimePermissions(downloadsUiPackage)) {
                grantRuntimePermissionsLPw(downloadsUiPackage, STORAGE_PERMISSIONS, true, userId);
            }
            PackageParser.Package storagePackage = getDefaultProviderAuthorityPackageLPr("com.android.externalstorage.documents", userId);
            if (storagePackage != null) {
                grantRuntimePermissionsLPw(storagePackage, STORAGE_PERMISSIONS, true, userId);
            }
            Intent certInstallerIntent = new Intent("android.credentials.INSTALL");
            PackageParser.Package certInstallerPackage = getDefaultSystemHandlerActivityPackageLPr(certInstallerIntent, userId);
            if (certInstallerPackage != null && doesPackageSupportRuntimePermissions(certInstallerPackage)) {
                grantRuntimePermissionsLPw(certInstallerPackage, STORAGE_PERMISSIONS, true, userId);
            }
            if (packages4 == null) {
                Intent dialerIntent = new Intent("android.intent.action.DIAL");
                PackageParser.Package dialerPackage = getDefaultSystemHandlerActivityPackageLPr(dialerIntent, userId);
                if (dialerPackage != null) {
                    grantDefaultPermissionsToDefaultSystemDialerAppLPr(dialerPackage, userId);
                }
            } else {
                for (String dialerAppPackageName : packages4) {
                    PackageParser.Package dialerPackage2 = getSystemPackageLPr(dialerAppPackageName);
                    if (dialerPackage2 != null) {
                        grantDefaultPermissionsToDefaultSystemDialerAppLPr(dialerPackage2, userId);
                    }
                }
            }
            if (packages5 != null) {
                for (String simCallManagerPackageName : packages5) {
                    PackageParser.Package simCallManagerPackage = getSystemPackageLPr(simCallManagerPackageName);
                    if (simCallManagerPackage != null) {
                        grantDefaultPermissionsToDefaultSimCallManagerLPr(simCallManagerPackage, userId);
                    }
                }
            }
            if (packages3 == null) {
                Intent smsIntent = new Intent("android.intent.action.MAIN");
                smsIntent.addCategory("android.intent.category.APP_MESSAGING");
                PackageParser.Package smsPackage = getDefaultSystemHandlerActivityPackageLPr(smsIntent, userId);
                if (smsPackage != null) {
                    grantDefaultPermissionsToDefaultSystemSmsAppLPr(smsPackage, userId);
                }
            } else {
                for (String smsPackageName : packages3) {
                    PackageParser.Package smsPackage2 = getSystemPackageLPr(smsPackageName);
                    if (smsPackage2 != null) {
                        grantDefaultPermissionsToDefaultSystemSmsAppLPr(smsPackage2, userId);
                    }
                }
            }
            Intent cbrIntent = new Intent("android.provider.Telephony.SMS_CB_RECEIVED");
            PackageParser.Package cbrPackage = getDefaultSystemHandlerActivityPackageLPr(cbrIntent, userId);
            if (cbrPackage != null && doesPackageSupportRuntimePermissions(cbrPackage)) {
                grantRuntimePermissionsLPw(cbrPackage, SMS_PERMISSIONS, userId);
            }
            Intent carrierProvIntent = new Intent("android.provider.Telephony.SMS_CARRIER_PROVISION");
            PackageParser.Package carrierProvPackage = getDefaultSystemHandlerServicePackageLPr(carrierProvIntent, userId);
            if (carrierProvPackage != null && doesPackageSupportRuntimePermissions(carrierProvPackage)) {
                grantRuntimePermissionsLPw(carrierProvPackage, SMS_PERMISSIONS, false, userId);
            }
            Intent calendarIntent = new Intent("android.intent.action.MAIN");
            calendarIntent.addCategory("android.intent.category.APP_CALENDAR");
            PackageParser.Package calendarPackage = getDefaultSystemHandlerActivityPackageLPr(calendarIntent, userId);
            if (calendarPackage != null && doesPackageSupportRuntimePermissions(calendarPackage)) {
                grantRuntimePermissionsLPw(calendarPackage, CALENDAR_PERMISSIONS, userId);
                grantRuntimePermissionsLPw(calendarPackage, CONTACTS_PERMISSIONS, userId);
            }
            PackageParser.Package calendarProviderPackage = getDefaultProviderAuthorityPackageLPr("com.android.calendar", userId);
            if (calendarProviderPackage != null) {
                grantRuntimePermissionsLPw(calendarProviderPackage, CONTACTS_PERMISSIONS, userId);
                grantRuntimePermissionsLPw(calendarProviderPackage, CALENDAR_PERMISSIONS, true, userId);
                grantRuntimePermissionsLPw(calendarProviderPackage, STORAGE_PERMISSIONS, userId);
            }
            List<PackageParser.Package> calendarSyncAdapters = getHeadlessSyncAdapterPackagesLPr(packages7, userId);
            int calendarSyncAdapterCount = calendarSyncAdapters.size();
            for (int i = 0; i < calendarSyncAdapterCount; i++) {
                PackageParser.Package calendarSyncAdapter = calendarSyncAdapters.get(i);
                if (doesPackageSupportRuntimePermissions(calendarSyncAdapter)) {
                    grantRuntimePermissionsLPw(calendarSyncAdapter, CALENDAR_PERMISSIONS, userId);
                }
            }
            Intent contactsIntent = new Intent("android.intent.action.MAIN");
            contactsIntent.addCategory("android.intent.category.APP_CONTACTS");
            PackageParser.Package contactsPackage = getDefaultSystemHandlerActivityPackageLPr(contactsIntent, userId);
            if (contactsPackage != null && doesPackageSupportRuntimePermissions(contactsPackage)) {
                grantRuntimePermissionsLPw(contactsPackage, CONTACTS_PERMISSIONS, userId);
                grantRuntimePermissionsLPw(contactsPackage, PHONE_PERMISSIONS, userId);
            }
            List<PackageParser.Package> contactsSyncAdapters = getHeadlessSyncAdapterPackagesLPr(packages6, userId);
            int contactsSyncAdapterCount = contactsSyncAdapters.size();
            for (int i2 = 0; i2 < contactsSyncAdapterCount; i2++) {
                PackageParser.Package contactsSyncAdapter = contactsSyncAdapters.get(i2);
                if (doesPackageSupportRuntimePermissions(contactsSyncAdapter)) {
                    grantRuntimePermissionsLPw(contactsSyncAdapter, CONTACTS_PERMISSIONS, userId);
                }
            }
            PackageParser.Package contactsProviderPackage = getDefaultProviderAuthorityPackageLPr("com.android.contacts", userId);
            if (contactsProviderPackage != null) {
                grantRuntimePermissionsLPw(contactsProviderPackage, CONTACTS_PERMISSIONS, true, userId);
                grantRuntimePermissionsLPw(contactsProviderPackage, PHONE_PERMISSIONS, true, userId);
                grantRuntimePermissionsLPw(contactsProviderPackage, STORAGE_PERMISSIONS, userId);
            }
            Intent deviceProvisionIntent = new Intent("android.app.action.PROVISION_MANAGED_DEVICE");
            PackageParser.Package deviceProvisionPackage = getDefaultSystemHandlerActivityPackageLPr(deviceProvisionIntent, userId);
            if (deviceProvisionPackage != null && doesPackageSupportRuntimePermissions(deviceProvisionPackage)) {
                grantRuntimePermissionsLPw(deviceProvisionPackage, CONTACTS_PERMISSIONS, userId);
            }
            Intent mapsIntent = new Intent("android.intent.action.MAIN");
            mapsIntent.addCategory("android.intent.category.APP_MAPS");
            PackageParser.Package mapsPackage = getDefaultSystemHandlerActivityPackageLPr(mapsIntent, userId);
            if (mapsPackage != null && doesPackageSupportRuntimePermissions(mapsPackage)) {
                grantRuntimePermissionsLPw(mapsPackage, LOCATION_PERMISSIONS, userId);
            }
            Intent galleryIntent = new Intent("android.intent.action.MAIN");
            galleryIntent.addCategory("android.intent.category.APP_GALLERY");
            PackageParser.Package galleryPackage = getDefaultSystemHandlerActivityPackageLPr(galleryIntent, userId);
            if (galleryPackage != null && doesPackageSupportRuntimePermissions(galleryPackage)) {
                grantRuntimePermissionsLPw(galleryPackage, STORAGE_PERMISSIONS, userId);
            }
            Intent emailIntent = new Intent("android.intent.action.MAIN");
            emailIntent.addCategory("android.intent.category.APP_EMAIL");
            PackageParser.Package emailPackage = getDefaultSystemHandlerActivityPackageLPr(emailIntent, userId);
            if (emailPackage != null && doesPackageSupportRuntimePermissions(emailPackage)) {
                grantRuntimePermissionsLPw(emailPackage, CONTACTS_PERMISSIONS, userId);
            }
            PackageParser.Package browserPackage = null;
            String defaultBrowserPackage = this.mService.getDefaultBrowserPackageName(userId);
            if (defaultBrowserPackage != null) {
                browserPackage = getPackageLPr(defaultBrowserPackage);
            }
            if (browserPackage == null) {
                Intent browserIntent = new Intent("android.intent.action.MAIN");
                browserIntent.addCategory("android.intent.category.APP_BROWSER");
                browserPackage = getDefaultSystemHandlerActivityPackageLPr(browserIntent, userId);
            }
            if (browserPackage != null && doesPackageSupportRuntimePermissions(browserPackage)) {
                grantRuntimePermissionsLPw(browserPackage, LOCATION_PERMISSIONS, userId);
            }
            if (packages != null) {
                for (String voiceInteractPackageName : packages) {
                    PackageParser.Package voiceInteractPackage = getSystemPackageLPr(voiceInteractPackageName);
                    if (voiceInteractPackage != null && doesPackageSupportRuntimePermissions(voiceInteractPackage)) {
                        grantRuntimePermissionsLPw(voiceInteractPackage, CONTACTS_PERMISSIONS, userId);
                        grantRuntimePermissionsLPw(voiceInteractPackage, CALENDAR_PERMISSIONS, userId);
                        grantRuntimePermissionsLPw(voiceInteractPackage, MICROPHONE_PERMISSIONS, userId);
                        grantRuntimePermissionsLPw(voiceInteractPackage, PHONE_PERMISSIONS, userId);
                        grantRuntimePermissionsLPw(voiceInteractPackage, SMS_PERMISSIONS, userId);
                        grantRuntimePermissionsLPw(voiceInteractPackage, LOCATION_PERMISSIONS, userId);
                    }
                }
            }
            Intent voiceRecoIntent = new Intent("android.speech.RecognitionService");
            voiceRecoIntent.addCategory("android.intent.category.DEFAULT");
            PackageParser.Package voiceRecoPackage = getDefaultSystemHandlerServicePackageLPr(voiceRecoIntent, userId);
            if (voiceRecoPackage != null && doesPackageSupportRuntimePermissions(voiceRecoPackage)) {
                grantRuntimePermissionsLPw(voiceRecoPackage, MICROPHONE_PERMISSIONS, userId);
            }
            if (packages2 != null) {
                for (String packageName : packages2) {
                    PackageParser.Package locationPackage = getSystemPackageLPr(packageName);
                    if (locationPackage != null && doesPackageSupportRuntimePermissions(locationPackage)) {
                        grantRuntimePermissionsLPw(locationPackage, CONTACTS_PERMISSIONS, userId);
                        grantRuntimePermissionsLPw(locationPackage, CALENDAR_PERMISSIONS, userId);
                        grantRuntimePermissionsLPw(locationPackage, MICROPHONE_PERMISSIONS, userId);
                        grantRuntimePermissionsLPw(locationPackage, PHONE_PERMISSIONS, userId);
                        grantRuntimePermissionsLPw(locationPackage, SMS_PERMISSIONS, userId);
                        grantRuntimePermissionsLPw(locationPackage, LOCATION_PERMISSIONS, true, userId);
                        grantRuntimePermissionsLPw(locationPackage, CAMERA_PERMISSIONS, userId);
                        grantRuntimePermissionsLPw(locationPackage, SENSORS_PERMISSIONS, userId);
                        grantRuntimePermissionsLPw(locationPackage, STORAGE_PERMISSIONS, userId);
                    }
                }
            }
            Intent musicIntent = new Intent("android.intent.action.VIEW");
            musicIntent.addCategory("android.intent.category.DEFAULT");
            musicIntent.setDataAndType(Uri.fromFile(new File("foo.mp3")), AUDIO_MIME_TYPE);
            PackageParser.Package musicPackage = getDefaultSystemHandlerActivityPackageLPr(musicIntent, userId);
            if (musicPackage != null && doesPackageSupportRuntimePermissions(musicPackage)) {
                grantRuntimePermissionsLPw(musicPackage, STORAGE_PERMISSIONS, userId);
            }
            if (this.mService.hasSystemFeature("android.hardware.type.watch", 0)) {
                Intent homeIntent = new Intent("android.intent.action.MAIN");
                homeIntent.addCategory("android.intent.category.HOME_MAIN");
                PackageParser.Package wearHomePackage = getDefaultSystemHandlerActivityPackageLPr(homeIntent, userId);
                if (wearHomePackage != null && doesPackageSupportRuntimePermissions(wearHomePackage)) {
                    grantRuntimePermissionsLPw(wearHomePackage, CONTACTS_PERMISSIONS, false, userId);
                    grantRuntimePermissionsLPw(wearHomePackage, PHONE_PERMISSIONS, true, userId);
                    grantRuntimePermissionsLPw(wearHomePackage, MICROPHONE_PERMISSIONS, false, userId);
                    grantRuntimePermissionsLPw(wearHomePackage, LOCATION_PERMISSIONS, false, userId);
                }
            }
            PackageParser.Package printSpoolerPackage = getSystemPackageLPr("com.android.printspooler");
            if (printSpoolerPackage != null && doesPackageSupportRuntimePermissions(printSpoolerPackage)) {
                grantRuntimePermissionsLPw(printSpoolerPackage, LOCATION_PERMISSIONS, true, userId);
            }
            Intent emergencyInfoIntent = new Intent("android.telephony.action.EMERGENCY_ASSISTANCE");
            PackageParser.Package emergencyInfoPckg = getDefaultSystemHandlerActivityPackageLPr(emergencyInfoIntent, userId);
            if (emergencyInfoPckg != null && doesPackageSupportRuntimePermissions(emergencyInfoPckg)) {
                grantRuntimePermissionsLPw(emergencyInfoPckg, CONTACTS_PERMISSIONS, true, userId);
                grantRuntimePermissionsLPw(emergencyInfoPckg, PHONE_PERMISSIONS, true, userId);
            }
            Intent nfcTagIntent = new Intent("android.intent.action.VIEW");
            nfcTagIntent.setType("vnd.android.cursor.item/ndef_msg");
            PackageParser.Package nfcTagPkg = getDefaultSystemHandlerActivityPackageLPr(nfcTagIntent, userId);
            if (nfcTagPkg != null && doesPackageSupportRuntimePermissions(nfcTagPkg)) {
                grantRuntimePermissionsLPw(nfcTagPkg, CONTACTS_PERMISSIONS, false, userId);
                grantRuntimePermissionsLPw(nfcTagPkg, PHONE_PERMISSIONS, false, userId);
            }
            this.mService.mSettings.onDefaultRuntimePermissionsGrantedLPr(userId);
        }
    }

    private void grantDefaultPermissionsToDefaultSystemDialerAppLPr(PackageParser.Package dialerPackage, int userId) {
        if (!doesPackageSupportRuntimePermissions(dialerPackage)) {
            return;
        }
        boolean isPhonePermFixed = this.mService.hasSystemFeature("android.hardware.type.watch", 0);
        grantRuntimePermissionsLPw(dialerPackage, PHONE_PERMISSIONS, isPhonePermFixed, userId);
        grantRuntimePermissionsLPw(dialerPackage, CONTACTS_PERMISSIONS, userId);
        grantRuntimePermissionsLPw(dialerPackage, SMS_PERMISSIONS, userId);
        grantRuntimePermissionsLPw(dialerPackage, MICROPHONE_PERMISSIONS, userId);
    }

    private void grantDefaultPermissionsToDefaultSystemSmsAppLPr(PackageParser.Package smsPackage, int userId) {
        if (!doesPackageSupportRuntimePermissions(smsPackage)) {
            return;
        }
        grantRuntimePermissionsLPw(smsPackage, PHONE_PERMISSIONS, userId);
        grantRuntimePermissionsLPw(smsPackage, CONTACTS_PERMISSIONS, userId);
        grantRuntimePermissionsLPw(smsPackage, SMS_PERMISSIONS, userId);
    }

    public void grantDefaultPermissionsToDefaultSmsAppLPr(String packageName, int userId) {
        PackageParser.Package smsPackage;
        Log.i(TAG, "Granting permissions to default sms app for user:" + userId);
        if (packageName == null || (smsPackage = getPackageLPr(packageName)) == null || !doesPackageSupportRuntimePermissions(smsPackage)) {
            return;
        }
        grantRuntimePermissionsLPw(smsPackage, PHONE_PERMISSIONS, false, true, userId);
        grantRuntimePermissionsLPw(smsPackage, CONTACTS_PERMISSIONS, false, true, userId);
        grantRuntimePermissionsLPw(smsPackage, SMS_PERMISSIONS, false, true, userId);
    }

    public void grantDefaultPermissionsToDefaultDialerAppLPr(String packageName, int userId) {
        PackageParser.Package dialerPackage;
        Log.i(TAG, "Granting permissions to default dialer app for user:" + userId);
        if (packageName == null || (dialerPackage = getPackageLPr(packageName)) == null || !doesPackageSupportRuntimePermissions(dialerPackage)) {
            return;
        }
        grantRuntimePermissionsLPw(dialerPackage, PHONE_PERMISSIONS, false, true, userId);
        grantRuntimePermissionsLPw(dialerPackage, CONTACTS_PERMISSIONS, false, true, userId);
        grantRuntimePermissionsLPw(dialerPackage, SMS_PERMISSIONS, false, true, userId);
        grantRuntimePermissionsLPw(dialerPackage, MICROPHONE_PERMISSIONS, false, true, userId);
    }

    private void grantDefaultPermissionsToDefaultSimCallManagerLPr(PackageParser.Package simCallManagerPackage, int userId) {
        Log.i(TAG, "Granting permissions to sim call manager for user:" + userId);
        if (!doesPackageSupportRuntimePermissions(simCallManagerPackage)) {
            return;
        }
        grantRuntimePermissionsLPw(simCallManagerPackage, PHONE_PERMISSIONS, userId);
        grantRuntimePermissionsLPw(simCallManagerPackage, MICROPHONE_PERMISSIONS, userId);
    }

    public void grantDefaultPermissionsToDefaultSimCallManagerLPr(String packageName, int userId) {
        PackageParser.Package simCallManagerPackage;
        if (packageName == null || (simCallManagerPackage = getPackageLPr(packageName)) == null) {
            return;
        }
        grantDefaultPermissionsToDefaultSimCallManagerLPr(simCallManagerPackage, userId);
    }

    public void grantDefaultPermissionsToEnabledCarrierAppsLPr(String[] packageNames, int userId) {
        Log.i(TAG, "Granting permissions to enabled carrier apps for user:" + userId);
        if (packageNames == null) {
            return;
        }
        for (String packageName : packageNames) {
            PackageParser.Package carrierPackage = getSystemPackageLPr(packageName);
            if (carrierPackage != null && doesPackageSupportRuntimePermissions(carrierPackage)) {
                grantRuntimePermissionsLPw(carrierPackage, PHONE_PERMISSIONS, userId);
                grantRuntimePermissionsLPw(carrierPackage, LOCATION_PERMISSIONS, userId);
                grantRuntimePermissionsLPw(carrierPackage, SMS_PERMISSIONS, userId);
            }
        }
    }

    public void grantDefaultPermissionsToDefaultBrowserLPr(String packageName, int userId) {
        PackageParser.Package browserPackage;
        Log.i(TAG, "Granting permissions to default browser for user:" + userId);
        if (packageName == null || (browserPackage = getSystemPackageLPr(packageName)) == null || !doesPackageSupportRuntimePermissions(browserPackage)) {
            return;
        }
        grantRuntimePermissionsLPw(browserPackage, LOCATION_PERMISSIONS, false, false, userId);
    }

    private PackageParser.Package getDefaultSystemHandlerActivityPackageLPr(Intent intent, int userId) {
        ResolveInfo handler = this.mService.resolveIntent(intent, intent.resolveType(this.mService.mContext.getContentResolver()), DEFAULT_FLAGS, userId);
        if (handler == null || handler.activityInfo == null) {
            return null;
        }
        ActivityInfo activityInfo = handler.activityInfo;
        if (activityInfo.packageName.equals(this.mService.mResolveActivity.packageName) && activityInfo.name.equals(this.mService.mResolveActivity.name)) {
            return null;
        }
        return getSystemPackageLPr(handler.activityInfo.packageName);
    }

    private PackageParser.Package getDefaultSystemHandlerServicePackageLPr(Intent intent, int userId) {
        List<ResolveInfo> handlers = this.mService.queryIntentServices(intent, intent.resolveType(this.mService.mContext.getContentResolver()), DEFAULT_FLAGS, userId).getList();
        if (handlers == null) {
            return null;
        }
        int handlerCount = handlers.size();
        for (int i = 0; i < handlerCount; i++) {
            ResolveInfo handler = handlers.get(i);
            PackageParser.Package handlerPackage = getSystemPackageLPr(handler.serviceInfo.packageName);
            if (handlerPackage != null) {
                return handlerPackage;
            }
        }
        return null;
    }

    private List<PackageParser.Package> getHeadlessSyncAdapterPackagesLPr(String[] syncAdapterPackageNames, int userId) {
        PackageParser.Package syncAdapterPackage;
        List<PackageParser.Package> syncAdapterPackages = new ArrayList<>();
        Intent homeIntent = new Intent("android.intent.action.MAIN");
        homeIntent.addCategory("android.intent.category.LAUNCHER");
        for (String syncAdapterPackageName : syncAdapterPackageNames) {
            homeIntent.setPackage(syncAdapterPackageName);
            ResolveInfo homeActivity = this.mService.resolveIntent(homeIntent, homeIntent.resolveType(this.mService.mContext.getContentResolver()), DEFAULT_FLAGS, userId);
            if (homeActivity == null && (syncAdapterPackage = getSystemPackageLPr(syncAdapterPackageName)) != null) {
                syncAdapterPackages.add(syncAdapterPackage);
            }
        }
        return syncAdapterPackages;
    }

    private PackageParser.Package getDefaultProviderAuthorityPackageLPr(String authority, int userId) {
        ProviderInfo provider = this.mService.resolveContentProvider(authority, DEFAULT_FLAGS, userId);
        if (provider != null) {
            return getSystemPackageLPr(provider.packageName);
        }
        return null;
    }

    private PackageParser.Package getPackageLPr(String packageName) {
        return this.mService.mPackages.get(packageName);
    }

    private PackageParser.Package getSystemPackageLPr(String packageName) {
        PackageParser.Package pkg = getPackageLPr(packageName);
        if (pkg == null || !pkg.isSystemApp() || isSysComponentOrPersistentPlatformSignedPrivAppLPr(pkg)) {
            return null;
        }
        return pkg;
    }

    private void grantRuntimePermissionsLPw(PackageParser.Package pkg, Set<String> permissions, int userId) {
        grantRuntimePermissionsLPw(pkg, permissions, false, false, userId);
    }

    private void grantRuntimePermissionsLPw(PackageParser.Package pkg, Set<String> permissions, boolean systemFixed, int userId) {
        grantRuntimePermissionsLPw(pkg, permissions, systemFixed, false, userId);
    }

    private void grantRuntimePermissionsLPw(PackageParser.Package pkg, Set<String> permissions, boolean systemFixed, boolean isDefaultPhoneOrSms, int userId) {
        PackageSetting sysPs;
        if (pkg.requestedPermissions.isEmpty()) {
            return;
        }
        List<String> requestedPermissions = pkg.requestedPermissions;
        Set<String> grantablePermissions = null;
        if (!isDefaultPhoneOrSms && pkg.isUpdatedSystemApp() && (sysPs = this.mService.mSettings.getDisabledSystemPkgLPr(pkg.packageName)) != null) {
            if (sysPs.pkg.requestedPermissions.isEmpty()) {
                return;
            }
            if (!requestedPermissions.equals(sysPs.pkg.requestedPermissions)) {
                grantablePermissions = new ArraySet<>(requestedPermissions);
                requestedPermissions = sysPs.pkg.requestedPermissions;
            }
        }
        int grantablePermissionCount = requestedPermissions.size();
        for (int i = 0; i < grantablePermissionCount; i++) {
            String permission = requestedPermissions.get(i);
            if ((grantablePermissions == null || grantablePermissions.contains(permission)) && permissions.contains(permission)) {
                int flags = this.mService.getPermissionFlags(permission, pkg.packageName, userId);
                boolean forceGrantCtaPerm = CtaUtils.isCtaOnlyPermission(permission) && (flags & 64) != 0 && (flags & 1) == 0;
                if (flags == 0 || isDefaultPhoneOrSms || forceGrantCtaPerm || isNeedToGrantAppOfForceShowList(pkg.packageName, flags)) {
                    if ((flags & 20) == 0) {
                        this.mService.grantRuntimePermission(pkg.packageName, permission, userId);
                        int newFlags = 32;
                        if (systemFixed) {
                            newFlags = 48;
                        }
                        this.mService.updatePermissionFlags(permission, pkg.packageName, newFlags, newFlags, userId);
                        if ((flags & 32) == 0 && (flags & 16) != 0 && !systemFixed) {
                            this.mService.updatePermissionFlags(permission, pkg.packageName, 16, 0, userId);
                        }
                    }
                } else if ((flags & 32) == 0) {
                }
            }
        }
    }

    private boolean isListedInForceShow(String pkgName) {
        if (mForcePermReviewPkgs == null) {
            mForcePermReviewPkgs = new HashSet<>(Arrays.asList(this.mService.mContext.getResources().getStringArray(R.array.force_review_pkgs)));
        }
        return mForcePermReviewPkgs.contains(pkgName);
    }

    private boolean isNeedToGrantAppOfForceShowList(String pkg, int flags) {
        if (!CtaUtils.isCtaSupported()) {
            return false;
        }
        boolean isReviewNeeded = (flags & 64) != 0;
        boolean isNotUserSet = (flags & 1) == 0;
        boolean isListed = isListedInForceShow(pkg);
        if (isReviewNeeded && isNotUserSet) {
            return isListed;
        }
        return false;
    }

    private boolean isSysComponentOrPersistentPlatformSignedPrivAppLPr(PackageParser.Package pkg) {
        if (UserHandle.getAppId(pkg.applicationInfo.uid) < 10000) {
            return true;
        }
        if (!pkg.isPrivilegedApp()) {
            return false;
        }
        PackageSetting sysPkg = this.mService.mSettings.getDisabledSystemPkgLPr(pkg.packageName);
        if (sysPkg != null && sysPkg.pkg != null) {
            if ((sysPkg.pkg.applicationInfo.flags & 8) == 0) {
                return false;
            }
        } else if ((pkg.applicationInfo.flags & 8) == 0) {
            return false;
        }
        return PackageManagerService.compareSignatures(this.mService.mPlatformPackage.mSignatures, pkg.mSignatures) == 0;
    }

    private static boolean doesPackageSupportRuntimePermissions(PackageParser.Package pkg) {
        return pkg.applicationInfo.targetSdkVersion > 22;
    }

    public void grantCtaPermToPreInstalledPackage(int userId) {
        Log.d(TAG, "grantCtaPermToPreInstalledPackage userId = " + userId);
        synchronized (this.mService.mPackages) {
            for (PackageParser.Package pkg : this.mService.mPackages.values()) {
                if (doesPackageSupportRuntimePermissions(pkg) && !pkg.requestedPermissions.isEmpty()) {
                    Set<String> permissions = new ArraySet<>();
                    int permissionCount = pkg.requestedPermissions.size();
                    for (int i = 0; i < permissionCount; i++) {
                        String permission = (String) pkg.requestedPermissions.get(i);
                        BasePermission bp = this.mService.mSettings.mPermissions.get(permission);
                        if (bp != null && bp.isRuntime() && CtaUtils.isCtaOnlyPermission(bp.name)) {
                            Log.d(TAG, "grantCtaPermToPreInstalledPackage grant " + permission + " to pre-installed package: " + pkg.packageName);
                            permissions.add(permission);
                        }
                    }
                    if (!permissions.isEmpty()) {
                        grantRuntimePermissionsLPw(pkg, permissions, isSysComponentOrPersistentPlatformSignedPrivAppLPr(pkg), false, userId);
                    }
                }
            }
        }
    }
}
