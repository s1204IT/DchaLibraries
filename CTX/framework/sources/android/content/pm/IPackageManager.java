package android.content.pm;

import android.content.ComponentName;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentSender;
import android.content.pm.IDexModuleRegisterCallback;
import android.content.pm.IOnPermissionsChangeListener;
import android.content.pm.IPackageDataObserver;
import android.content.pm.IPackageDeleteObserver;
import android.content.pm.IPackageDeleteObserver2;
import android.content.pm.IPackageInstaller;
import android.content.pm.IPackageMoveObserver;
import android.content.pm.IPackageStatsObserver;
import android.content.pm.dex.IArtManager;
import android.graphics.Bitmap;
import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.PersistableBundle;
import android.os.RemoteException;
import android.text.TextUtils;
import java.util.ArrayList;
import java.util.List;

public interface IPackageManager extends IInterface {
    boolean activitySupportsIntent(ComponentName componentName, Intent intent, String str) throws RemoteException;

    void addCrossProfileIntentFilter(IntentFilter intentFilter, String str, int i, int i2, int i3) throws RemoteException;

    void addOnPermissionsChangeListener(IOnPermissionsChangeListener iOnPermissionsChangeListener) throws RemoteException;

    boolean addPermission(PermissionInfo permissionInfo) throws RemoteException;

    boolean addPermissionAsync(PermissionInfo permissionInfo) throws RemoteException;

    void addPersistentPreferredActivity(IntentFilter intentFilter, ComponentName componentName, int i) throws RemoteException;

    void addPreferredActivity(IntentFilter intentFilter, int i, ComponentName[] componentNameArr, ComponentName componentName, int i2) throws RemoteException;

    boolean canForwardTo(Intent intent, String str, int i, int i2) throws RemoteException;

    boolean canRequestPackageInstalls(String str, int i) throws RemoteException;

    String[] canonicalToCurrentPackageNames(String[] strArr) throws RemoteException;

    void checkPackageStartable(String str, int i) throws RemoteException;

    int checkPermission(String str, String str2, int i) throws RemoteException;

    int checkSignatures(String str, String str2) throws RemoteException;

    int checkUidPermission(String str, int i) throws RemoteException;

    int checkUidSignatures(int i, int i2) throws RemoteException;

    void clearApplicationProfileData(String str) throws RemoteException;

    void clearApplicationUserData(String str, IPackageDataObserver iPackageDataObserver, int i) throws RemoteException;

    void clearCrossProfileIntentFilters(int i, String str) throws RemoteException;

    void clearPackagePersistentPreferredActivities(String str, int i) throws RemoteException;

    void clearPackagePreferredActivities(String str) throws RemoteException;

    String[] currentToCanonicalPackageNames(String[] strArr) throws RemoteException;

    void deleteApplicationCacheFiles(String str, IPackageDataObserver iPackageDataObserver) throws RemoteException;

    void deleteApplicationCacheFilesAsUser(String str, int i, IPackageDataObserver iPackageDataObserver) throws RemoteException;

    void deletePackageAsUser(String str, int i, IPackageDeleteObserver iPackageDeleteObserver, int i2, int i3) throws RemoteException;

    void deletePackageVersioned(VersionedPackage versionedPackage, IPackageDeleteObserver2 iPackageDeleteObserver2, int i, int i2) throws RemoteException;

    void deletePreloadsFileCache() throws RemoteException;

    void dumpProfiles(String str) throws RemoteException;

    void enterSafeMode() throws RemoteException;

    void extendVerificationTimeout(int i, int i2, long j) throws RemoteException;

    ResolveInfo findPersistentPreferredActivity(Intent intent, int i) throws RemoteException;

    void finishPackageInstall(int i, boolean z) throws RemoteException;

    void flushPackageRestrictionsAsUser(int i) throws RemoteException;

    void forceDexOpt(String str) throws RemoteException;

    void freeStorage(String str, long j, int i, IntentSender intentSender) throws RemoteException;

    void freeStorageAndNotify(String str, long j, int i, IPackageDataObserver iPackageDataObserver) throws RemoteException;

    ActivityInfo getActivityInfo(ComponentName componentName, int i, int i2) throws RemoteException;

    ParceledListSlice getAllIntentFilters(String str) throws RemoteException;

    List<String> getAllPackages() throws RemoteException;

    ParceledListSlice getAllPermissionGroups(int i) throws RemoteException;

    String[] getAppOpPermissionPackages(String str) throws RemoteException;

    int getApplicationEnabledSetting(String str, int i) throws RemoteException;

    boolean getApplicationHiddenSettingAsUser(String str, int i) throws RemoteException;

    ApplicationInfo getApplicationInfo(String str, int i, int i2) throws RemoteException;

    IArtManager getArtManager() throws RemoteException;

    boolean getBlockUninstallForUser(String str, int i) throws RemoteException;

    ChangedPackages getChangedPackages(int i, int i2) throws RemoteException;

    int getComponentEnabledSetting(ComponentName componentName, int i) throws RemoteException;

    byte[] getDefaultAppsBackup(int i) throws RemoteException;

    String getDefaultBrowserPackageName(int i) throws RemoteException;

    int getFlagsForUid(int i) throws RemoteException;

    CharSequence getHarmfulAppWarning(String str, int i) throws RemoteException;

    ComponentName getHomeActivities(List<ResolveInfo> list) throws RemoteException;

    int getInstallLocation() throws RemoteException;

    int getInstallReason(String str, int i) throws RemoteException;

    ParceledListSlice getInstalledApplications(int i, int i2) throws RemoteException;

    ParceledListSlice getInstalledPackages(int i, int i2) throws RemoteException;

    String getInstallerPackageName(String str) throws RemoteException;

    String getInstantAppAndroidId(String str, int i) throws RemoteException;

    byte[] getInstantAppCookie(String str, int i) throws RemoteException;

    Bitmap getInstantAppIcon(String str, int i) throws RemoteException;

    ComponentName getInstantAppInstallerComponent() throws RemoteException;

    ComponentName getInstantAppResolverComponent() throws RemoteException;

    ComponentName getInstantAppResolverSettingsComponent() throws RemoteException;

    ParceledListSlice getInstantApps(int i) throws RemoteException;

    InstrumentationInfo getInstrumentationInfo(ComponentName componentName, int i) throws RemoteException;

    byte[] getIntentFilterVerificationBackup(int i) throws RemoteException;

    ParceledListSlice getIntentFilterVerifications(String str) throws RemoteException;

    int getIntentVerificationStatus(String str, int i) throws RemoteException;

    KeySet getKeySetByAlias(String str, String str2) throws RemoteException;

    ResolveInfo getLastChosenActivity(Intent intent, String str, int i) throws RemoteException;

    int getMoveStatus(int i) throws RemoteException;

    String getNameForUid(int i) throws RemoteException;

    String[] getNamesForUids(int[] iArr) throws RemoteException;

    int[] getPackageGids(String str, int i, int i2) throws RemoteException;

    PackageInfo getPackageInfo(String str, int i, int i2) throws RemoteException;

    PackageInfo getPackageInfoVersioned(VersionedPackage versionedPackage, int i, int i2) throws RemoteException;

    IPackageInstaller getPackageInstaller() throws RemoteException;

    void getPackageSizeInfo(String str, int i, IPackageStatsObserver iPackageStatsObserver) throws RemoteException;

    int getPackageUid(String str, int i, int i2) throws RemoteException;

    String[] getPackagesForUid(int i) throws RemoteException;

    ParceledListSlice getPackagesHoldingPermissions(String[] strArr, int i, int i2) throws RemoteException;

    String getPermissionControllerPackageName() throws RemoteException;

    int getPermissionFlags(String str, String str2, int i) throws RemoteException;

    byte[] getPermissionGrantBackup(int i) throws RemoteException;

    PermissionGroupInfo getPermissionGroupInfo(String str, int i) throws RemoteException;

    PermissionInfo getPermissionInfo(String str, String str2, int i) throws RemoteException;

    ParceledListSlice getPersistentApplications(int i) throws RemoteException;

    int getPreferredActivities(List<IntentFilter> list, List<ComponentName> list2, String str) throws RemoteException;

    byte[] getPreferredActivityBackup(int i) throws RemoteException;

    int getPrivateFlagsForUid(int i) throws RemoteException;

    ProviderInfo getProviderInfo(ComponentName componentName, int i, int i2) throws RemoteException;

    ActivityInfo getReceiverInfo(ComponentName componentName, int i, int i2) throws RemoteException;

    ServiceInfo getServiceInfo(ComponentName componentName, int i, int i2) throws RemoteException;

    String getServicesSystemSharedLibraryPackageName() throws RemoteException;

    ParceledListSlice getSharedLibraries(String str, int i, int i2) throws RemoteException;

    String getSharedSystemSharedLibraryPackageName() throws RemoteException;

    KeySet getSigningKeySet(String str) throws RemoteException;

    PersistableBundle getSuspendedPackageAppExtras(String str, int i) throws RemoteException;

    ParceledListSlice getSystemAvailableFeatures() throws RemoteException;

    String[] getSystemSharedLibraryNames() throws RemoteException;

    String getSystemTextClassifierPackageName() throws RemoteException;

    int getUidForSharedUser(String str) throws RemoteException;

    VerifierDeviceIdentity getVerifierDeviceIdentity() throws RemoteException;

    void grantDefaultPermissionsToActiveLuiApp(String str, int i) throws RemoteException;

    void grantDefaultPermissionsToEnabledCarrierApps(String[] strArr, int i) throws RemoteException;

    void grantDefaultPermissionsToEnabledImsServices(String[] strArr, int i) throws RemoteException;

    void grantDefaultPermissionsToEnabledTelephonyDataServices(String[] strArr, int i) throws RemoteException;

    void grantRuntimePermission(String str, String str2, int i) throws RemoteException;

    boolean hasSigningCertificate(String str, byte[] bArr, int i) throws RemoteException;

    boolean hasSystemFeature(String str, int i) throws RemoteException;

    boolean hasSystemUidErrors() throws RemoteException;

    boolean hasUidSigningCertificate(int i, byte[] bArr, int i2) throws RemoteException;

    int installExistingPackageAsUser(String str, int i, int i2, int i3) throws RemoteException;

    boolean isFirstBoot() throws RemoteException;

    boolean isInstantApp(String str, int i) throws RemoteException;

    boolean isOnlyCoreApps() throws RemoteException;

    boolean isPackageAvailable(String str, int i) throws RemoteException;

    boolean isPackageDeviceAdminOnAnyUser(String str) throws RemoteException;

    boolean isPackageSignedByKeySet(String str, KeySet keySet) throws RemoteException;

    boolean isPackageSignedByKeySetExactly(String str, KeySet keySet) throws RemoteException;

    boolean isPackageStateProtected(String str, int i) throws RemoteException;

    boolean isPackageSuspendedForUser(String str, int i) throws RemoteException;

    boolean isPermissionEnforced(String str) throws RemoteException;

    boolean isPermissionRevokedByPolicy(String str, String str2, int i) throws RemoteException;

    boolean isProtectedBroadcast(String str) throws RemoteException;

    boolean isSafeMode() throws RemoteException;

    boolean isStorageLow() throws RemoteException;

    boolean isUidPrivileged(int i) throws RemoteException;

    boolean isUpgrade() throws RemoteException;

    void logAppProcessStartIfNeeded(String str, int i, String str2, String str3, int i2) throws RemoteException;

    int movePackage(String str, String str2) throws RemoteException;

    int movePrimaryStorage(String str) throws RemoteException;

    PackageCleanItem nextPackageToClean(PackageCleanItem packageCleanItem) throws RemoteException;

    void notifyDexLoad(String str, List<String> list, List<String> list2, String str2) throws RemoteException;

    void notifyPackageUse(String str, int i) throws RemoteException;

    boolean performDexOptMode(String str, boolean z, String str2, boolean z2, boolean z3, String str3) throws RemoteException;

    boolean performDexOptSecondary(String str, String str2, boolean z) throws RemoteException;

    void performFstrimIfNeeded() throws RemoteException;

    ParceledListSlice queryContentProviders(String str, int i, int i2, String str2) throws RemoteException;

    ParceledListSlice queryInstrumentation(String str, int i) throws RemoteException;

    ParceledListSlice queryIntentActivities(Intent intent, String str, int i, int i2) throws RemoteException;

    ParceledListSlice queryIntentActivityOptions(ComponentName componentName, Intent[] intentArr, String[] strArr, Intent intent, String str, int i, int i2) throws RemoteException;

    ParceledListSlice queryIntentContentProviders(Intent intent, String str, int i, int i2) throws RemoteException;

    ParceledListSlice queryIntentReceivers(Intent intent, String str, int i, int i2) throws RemoteException;

    ParceledListSlice queryIntentServices(Intent intent, String str, int i, int i2) throws RemoteException;

    ParceledListSlice queryPermissionsByGroup(String str, int i) throws RemoteException;

    void querySyncProviders(List<String> list, List<ProviderInfo> list2) throws RemoteException;

    void reconcileSecondaryDexFiles(String str) throws RemoteException;

    void registerDexModule(String str, String str2, boolean z, IDexModuleRegisterCallback iDexModuleRegisterCallback) throws RemoteException;

    void registerMoveCallback(IPackageMoveObserver iPackageMoveObserver) throws RemoteException;

    void removeOnPermissionsChangeListener(IOnPermissionsChangeListener iOnPermissionsChangeListener) throws RemoteException;

    void removePermission(String str) throws RemoteException;

    void replacePreferredActivity(IntentFilter intentFilter, int i, ComponentName[] componentNameArr, ComponentName componentName, int i2) throws RemoteException;

    void resetApplicationPreferences(int i) throws RemoteException;

    void resetRuntimePermissions() throws RemoteException;

    ProviderInfo resolveContentProvider(String str, int i, int i2) throws RemoteException;

    ResolveInfo resolveIntent(Intent intent, String str, int i, int i2) throws RemoteException;

    ResolveInfo resolveService(Intent intent, String str, int i, int i2) throws RemoteException;

    void restoreDefaultApps(byte[] bArr, int i) throws RemoteException;

    void restoreIntentFilterVerification(byte[] bArr, int i) throws RemoteException;

    void restorePermissionGrants(byte[] bArr, int i) throws RemoteException;

    void restorePreferredActivities(byte[] bArr, int i) throws RemoteException;

    void revokeDefaultPermissionsFromDisabledTelephonyDataServices(String[] strArr, int i) throws RemoteException;

    void revokeDefaultPermissionsFromLuiApps(String[] strArr, int i) throws RemoteException;

    void revokeRuntimePermission(String str, String str2, int i) throws RemoteException;

    boolean runBackgroundDexoptJob(List<String> list) throws RemoteException;

    void setApplicationCategoryHint(String str, int i, String str2) throws RemoteException;

    void setApplicationEnabledSetting(String str, int i, int i2, int i3, String str2) throws RemoteException;

    boolean setApplicationHiddenSettingAsUser(String str, boolean z, int i) throws RemoteException;

    boolean setBlockUninstallForUser(String str, boolean z, int i) throws RemoteException;

    void setComponentEnabledSetting(ComponentName componentName, int i, int i2, int i3) throws RemoteException;

    boolean setDefaultBrowserPackageName(String str, int i) throws RemoteException;

    void setHarmfulAppWarning(String str, CharSequence charSequence, int i) throws RemoteException;

    void setHomeActivity(ComponentName componentName, int i) throws RemoteException;

    boolean setInstallLocation(int i) throws RemoteException;

    void setInstallerPackageName(String str, String str2) throws RemoteException;

    boolean setInstantAppCookie(String str, byte[] bArr, int i) throws RemoteException;

    void setLastChosenActivity(Intent intent, String str, int i, IntentFilter intentFilter, int i2, ComponentName componentName) throws RemoteException;

    void setPackageStoppedState(String str, boolean z, int i) throws RemoteException;

    String[] setPackagesSuspendedAsUser(String[] strArr, boolean z, PersistableBundle persistableBundle, PersistableBundle persistableBundle2, String str, String str2, int i) throws RemoteException;

    void setPermissionEnforced(String str, boolean z) throws RemoteException;

    boolean setRequiredForSystemUser(String str, boolean z) throws RemoteException;

    void setUpdateAvailable(String str, boolean z) throws RemoteException;

    boolean shouldShowRequestPermissionRationale(String str, String str2, int i) throws RemoteException;

    void systemReady() throws RemoteException;

    void unregisterMoveCallback(IPackageMoveObserver iPackageMoveObserver) throws RemoteException;

    boolean updateIntentVerificationStatus(String str, int i, int i2) throws RemoteException;

    void updatePackagesIfNeeded() throws RemoteException;

    void updatePermissionFlags(String str, String str2, int i, int i2, int i3) throws RemoteException;

    void updatePermissionFlagsForAllApps(int i, int i2, int i3) throws RemoteException;

    void verifyIntentFilter(int i, int i2, List<String> list) throws RemoteException;

    void verifyPendingInstall(int i, int i2) throws RemoteException;

    public static abstract class Stub extends Binder implements IPackageManager {
        private static final String DESCRIPTOR = "android.content.pm.IPackageManager";
        static final int TRANSACTION_activitySupportsIntent = 15;
        static final int TRANSACTION_addCrossProfileIntentFilter = 75;
        static final int TRANSACTION_addOnPermissionsChangeListener = 157;
        static final int TRANSACTION_addPermission = 21;
        static final int TRANSACTION_addPermissionAsync = 128;
        static final int TRANSACTION_addPersistentPreferredActivity = 73;
        static final int TRANSACTION_addPreferredActivity = 69;
        static final int TRANSACTION_canForwardTo = 44;
        static final int TRANSACTION_canRequestPackageInstalls = 180;
        static final int TRANSACTION_canonicalToCurrentPackageNames = 8;
        static final int TRANSACTION_checkPackageStartable = 1;
        static final int TRANSACTION_checkPermission = 19;
        static final int TRANSACTION_checkSignatures = 31;
        static final int TRANSACTION_checkUidPermission = 20;
        static final int TRANSACTION_checkUidSignatures = 32;
        static final int TRANSACTION_clearApplicationProfileData = 102;
        static final int TRANSACTION_clearApplicationUserData = 101;
        static final int TRANSACTION_clearCrossProfileIntentFilters = 76;
        static final int TRANSACTION_clearPackagePersistentPreferredActivities = 74;
        static final int TRANSACTION_clearPackagePreferredActivities = 71;
        static final int TRANSACTION_currentToCanonicalPackageNames = 7;
        static final int TRANSACTION_deleteApplicationCacheFiles = 99;
        static final int TRANSACTION_deleteApplicationCacheFilesAsUser = 100;
        static final int TRANSACTION_deletePackageAsUser = 63;
        static final int TRANSACTION_deletePackageVersioned = 64;
        static final int TRANSACTION_deletePreloadsFileCache = 181;
        static final int TRANSACTION_dumpProfiles = 118;
        static final int TRANSACTION_enterSafeMode = 107;
        static final int TRANSACTION_extendVerificationTimeout = 133;
        static final int TRANSACTION_findPersistentPreferredActivity = 43;
        static final int TRANSACTION_finishPackageInstall = 60;
        static final int TRANSACTION_flushPackageRestrictionsAsUser = 95;
        static final int TRANSACTION_forceDexOpt = 119;
        static final int TRANSACTION_freeStorage = 98;
        static final int TRANSACTION_freeStorageAndNotify = 97;
        static final int TRANSACTION_getActivityInfo = 14;
        static final int TRANSACTION_getAllIntentFilters = 138;
        static final int TRANSACTION_getAllPackages = 33;
        static final int TRANSACTION_getAllPermissionGroups = 12;
        static final int TRANSACTION_getAppOpPermissionPackages = 41;
        static final int TRANSACTION_getApplicationEnabledSetting = 93;
        static final int TRANSACTION_getApplicationHiddenSettingAsUser = 149;
        static final int TRANSACTION_getApplicationInfo = 13;
        static final int TRANSACTION_getArtManager = 186;
        static final int TRANSACTION_getBlockUninstallForUser = 152;
        static final int TRANSACTION_getChangedPackages = 176;
        static final int TRANSACTION_getComponentEnabledSetting = 91;
        static final int TRANSACTION_getDefaultAppsBackup = 82;
        static final int TRANSACTION_getDefaultBrowserPackageName = 140;
        static final int TRANSACTION_getFlagsForUid = 38;
        static final int TRANSACTION_getHarmfulAppWarning = 188;
        static final int TRANSACTION_getHomeActivities = 88;
        static final int TRANSACTION_getInstallLocation = 130;
        static final int TRANSACTION_getInstallReason = 178;
        static final int TRANSACTION_getInstalledApplications = 53;
        static final int TRANSACTION_getInstalledPackages = 51;
        static final int TRANSACTION_getInstallerPackageName = 65;
        static final int TRANSACTION_getInstantAppAndroidId = 185;
        static final int TRANSACTION_getInstantAppCookie = 168;
        static final int TRANSACTION_getInstantAppIcon = 170;
        static final int TRANSACTION_getInstantAppInstallerComponent = 184;
        static final int TRANSACTION_getInstantAppResolverComponent = 182;
        static final int TRANSACTION_getInstantAppResolverSettingsComponent = 183;
        static final int TRANSACTION_getInstantApps = 167;
        static final int TRANSACTION_getInstrumentationInfo = 58;
        static final int TRANSACTION_getIntentFilterVerificationBackup = 84;
        static final int TRANSACTION_getIntentFilterVerifications = 137;
        static final int TRANSACTION_getIntentVerificationStatus = 135;
        static final int TRANSACTION_getKeySetByAlias = 153;
        static final int TRANSACTION_getLastChosenActivity = 67;
        static final int TRANSACTION_getMoveStatus = 123;
        static final int TRANSACTION_getNameForUid = 35;
        static final int TRANSACTION_getNamesForUids = 36;
        static final int TRANSACTION_getPackageGids = 6;
        static final int TRANSACTION_getPackageInfo = 3;
        static final int TRANSACTION_getPackageInfoVersioned = 4;
        static final int TRANSACTION_getPackageInstaller = 150;
        static final int TRANSACTION_getPackageSizeInfo = 103;
        static final int TRANSACTION_getPackageUid = 5;
        static final int TRANSACTION_getPackagesForUid = 34;
        static final int TRANSACTION_getPackagesHoldingPermissions = 52;
        static final int TRANSACTION_getPermissionControllerPackageName = 166;
        static final int TRANSACTION_getPermissionFlags = 26;
        static final int TRANSACTION_getPermissionGrantBackup = 86;
        static final int TRANSACTION_getPermissionGroupInfo = 11;
        static final int TRANSACTION_getPermissionInfo = 9;
        static final int TRANSACTION_getPersistentApplications = 54;
        static final int TRANSACTION_getPreferredActivities = 72;
        static final int TRANSACTION_getPreferredActivityBackup = 80;
        static final int TRANSACTION_getPrivateFlagsForUid = 39;
        static final int TRANSACTION_getProviderInfo = 18;
        static final int TRANSACTION_getReceiverInfo = 16;
        static final int TRANSACTION_getServiceInfo = 17;
        static final int TRANSACTION_getServicesSystemSharedLibraryPackageName = 174;
        static final int TRANSACTION_getSharedLibraries = 179;
        static final int TRANSACTION_getSharedSystemSharedLibraryPackageName = 175;
        static final int TRANSACTION_getSigningKeySet = 154;
        static final int TRANSACTION_getSuspendedPackageAppExtras = 79;
        static final int TRANSACTION_getSystemAvailableFeatures = 105;
        static final int TRANSACTION_getSystemSharedLibraryNames = 104;
        static final int TRANSACTION_getSystemTextClassifierPackageName = 191;
        static final int TRANSACTION_getUidForSharedUser = 37;
        static final int TRANSACTION_getVerifierDeviceIdentity = 141;
        static final int TRANSACTION_grantDefaultPermissionsToActiveLuiApp = 163;
        static final int TRANSACTION_grantDefaultPermissionsToEnabledCarrierApps = 159;
        static final int TRANSACTION_grantDefaultPermissionsToEnabledImsServices = 160;
        static final int TRANSACTION_grantDefaultPermissionsToEnabledTelephonyDataServices = 161;
        static final int TRANSACTION_grantRuntimePermission = 23;
        static final int TRANSACTION_hasSigningCertificate = 189;
        static final int TRANSACTION_hasSystemFeature = 106;
        static final int TRANSACTION_hasSystemUidErrors = 110;
        static final int TRANSACTION_hasUidSigningCertificate = 190;
        static final int TRANSACTION_installExistingPackageAsUser = 131;
        static final int TRANSACTION_isFirstBoot = 142;
        static final int TRANSACTION_isInstantApp = 171;
        static final int TRANSACTION_isOnlyCoreApps = 143;
        static final int TRANSACTION_isPackageAvailable = 2;
        static final int TRANSACTION_isPackageDeviceAdminOnAnyUser = 177;
        static final int TRANSACTION_isPackageSignedByKeySet = 155;
        static final int TRANSACTION_isPackageSignedByKeySetExactly = 156;
        static final int TRANSACTION_isPackageStateProtected = 192;
        static final int TRANSACTION_isPackageSuspendedForUser = 78;
        static final int TRANSACTION_isPermissionEnforced = 146;
        static final int TRANSACTION_isPermissionRevokedByPolicy = 165;
        static final int TRANSACTION_isProtectedBroadcast = 30;
        static final int TRANSACTION_isSafeMode = 108;
        static final int TRANSACTION_isStorageLow = 147;
        static final int TRANSACTION_isUidPrivileged = 40;
        static final int TRANSACTION_isUpgrade = 144;
        static final int TRANSACTION_logAppProcessStartIfNeeded = 94;
        static final int TRANSACTION_movePackage = 126;
        static final int TRANSACTION_movePrimaryStorage = 127;
        static final int TRANSACTION_nextPackageToClean = 122;
        static final int TRANSACTION_notifyDexLoad = 114;
        static final int TRANSACTION_notifyPackageUse = 113;
        static final int TRANSACTION_performDexOptMode = 116;
        static final int TRANSACTION_performDexOptSecondary = 117;
        static final int TRANSACTION_performFstrimIfNeeded = 111;
        static final int TRANSACTION_queryContentProviders = 57;
        static final int TRANSACTION_queryInstrumentation = 59;
        static final int TRANSACTION_queryIntentActivities = 45;
        static final int TRANSACTION_queryIntentActivityOptions = 46;
        static final int TRANSACTION_queryIntentContentProviders = 50;
        static final int TRANSACTION_queryIntentReceivers = 47;
        static final int TRANSACTION_queryIntentServices = 49;
        static final int TRANSACTION_queryPermissionsByGroup = 10;
        static final int TRANSACTION_querySyncProviders = 56;
        static final int TRANSACTION_reconcileSecondaryDexFiles = 121;
        static final int TRANSACTION_registerDexModule = 115;
        static final int TRANSACTION_registerMoveCallback = 124;
        static final int TRANSACTION_removeOnPermissionsChangeListener = 158;
        static final int TRANSACTION_removePermission = 22;
        static final int TRANSACTION_replacePreferredActivity = 70;
        static final int TRANSACTION_resetApplicationPreferences = 66;
        static final int TRANSACTION_resetRuntimePermissions = 25;
        static final int TRANSACTION_resolveContentProvider = 55;
        static final int TRANSACTION_resolveIntent = 42;
        static final int TRANSACTION_resolveService = 48;
        static final int TRANSACTION_restoreDefaultApps = 83;
        static final int TRANSACTION_restoreIntentFilterVerification = 85;
        static final int TRANSACTION_restorePermissionGrants = 87;
        static final int TRANSACTION_restorePreferredActivities = 81;
        static final int TRANSACTION_revokeDefaultPermissionsFromDisabledTelephonyDataServices = 162;
        static final int TRANSACTION_revokeDefaultPermissionsFromLuiApps = 164;
        static final int TRANSACTION_revokeRuntimePermission = 24;
        static final int TRANSACTION_runBackgroundDexoptJob = 120;
        static final int TRANSACTION_setApplicationCategoryHint = 62;
        static final int TRANSACTION_setApplicationEnabledSetting = 92;
        static final int TRANSACTION_setApplicationHiddenSettingAsUser = 148;
        static final int TRANSACTION_setBlockUninstallForUser = 151;
        static final int TRANSACTION_setComponentEnabledSetting = 90;
        static final int TRANSACTION_setDefaultBrowserPackageName = 139;
        static final int TRANSACTION_setHarmfulAppWarning = 187;
        static final int TRANSACTION_setHomeActivity = 89;
        static final int TRANSACTION_setInstallLocation = 129;
        static final int TRANSACTION_setInstallerPackageName = 61;
        static final int TRANSACTION_setInstantAppCookie = 169;
        static final int TRANSACTION_setLastChosenActivity = 68;
        static final int TRANSACTION_setPackageStoppedState = 96;
        static final int TRANSACTION_setPackagesSuspendedAsUser = 77;
        static final int TRANSACTION_setPermissionEnforced = 145;
        static final int TRANSACTION_setRequiredForSystemUser = 172;
        static final int TRANSACTION_setUpdateAvailable = 173;
        static final int TRANSACTION_shouldShowRequestPermissionRationale = 29;
        static final int TRANSACTION_systemReady = 109;
        static final int TRANSACTION_unregisterMoveCallback = 125;
        static final int TRANSACTION_updateIntentVerificationStatus = 136;
        static final int TRANSACTION_updatePackagesIfNeeded = 112;
        static final int TRANSACTION_updatePermissionFlags = 27;
        static final int TRANSACTION_updatePermissionFlagsForAllApps = 28;
        static final int TRANSACTION_verifyIntentFilter = 134;
        static final int TRANSACTION_verifyPendingInstall = 132;

        public Stub() {
            attachInterface(this, DESCRIPTOR);
        }

        public static IPackageManager asInterface(IBinder iBinder) {
            if (iBinder == null) {
                return null;
            }
            IInterface iInterfaceQueryLocalInterface = iBinder.queryLocalInterface(DESCRIPTOR);
            if (iInterfaceQueryLocalInterface != null && (iInterfaceQueryLocalInterface instanceof IPackageManager)) {
                return (IPackageManager) iInterfaceQueryLocalInterface;
            }
            return new Proxy(iBinder);
        }

        @Override
        public IBinder asBinder() {
            return this;
        }

        @Override
        public boolean onTransact(int i, Parcel parcel, Parcel parcel2, int i2) throws RemoteException {
            ComponentName componentNameCreateFromParcel;
            ComponentName componentNameCreateFromParcel2;
            Intent intentCreateFromParcel;
            IntentFilter intentFilterCreateFromParcel;
            ComponentName componentNameCreateFromParcel3;
            IntentFilter intentFilterCreateFromParcel2;
            IntentFilter intentFilterCreateFromParcel3;
            IntentFilter intentFilterCreateFromParcel4;
            IntentFilter intentFilterCreateFromParcel5;
            PersistableBundle persistableBundleCreateFromParcel;
            IntentSender intentSenderCreateFromParcel;
            if (i == 1598968902) {
                parcel2.writeString(DESCRIPTOR);
                return true;
            }
            switch (i) {
                case 1:
                    parcel.enforceInterface(DESCRIPTOR);
                    checkPackageStartable(parcel.readString(), parcel.readInt());
                    parcel2.writeNoException();
                    return true;
                case 2:
                    parcel.enforceInterface(DESCRIPTOR);
                    boolean zIsPackageAvailable = isPackageAvailable(parcel.readString(), parcel.readInt());
                    parcel2.writeNoException();
                    parcel2.writeInt(zIsPackageAvailable ? 1 : 0);
                    return true;
                case 3:
                    parcel.enforceInterface(DESCRIPTOR);
                    PackageInfo packageInfo = getPackageInfo(parcel.readString(), parcel.readInt(), parcel.readInt());
                    parcel2.writeNoException();
                    if (packageInfo != null) {
                        parcel2.writeInt(1);
                        packageInfo.writeToParcel(parcel2, 1);
                    } else {
                        parcel2.writeInt(0);
                    }
                    return true;
                case 4:
                    parcel.enforceInterface(DESCRIPTOR);
                    PackageInfo packageInfoVersioned = getPackageInfoVersioned(parcel.readInt() != 0 ? VersionedPackage.CREATOR.createFromParcel(parcel) : null, parcel.readInt(), parcel.readInt());
                    parcel2.writeNoException();
                    if (packageInfoVersioned != null) {
                        parcel2.writeInt(1);
                        packageInfoVersioned.writeToParcel(parcel2, 1);
                    } else {
                        parcel2.writeInt(0);
                    }
                    return true;
                case 5:
                    parcel.enforceInterface(DESCRIPTOR);
                    int packageUid = getPackageUid(parcel.readString(), parcel.readInt(), parcel.readInt());
                    parcel2.writeNoException();
                    parcel2.writeInt(packageUid);
                    return true;
                case 6:
                    parcel.enforceInterface(DESCRIPTOR);
                    int[] packageGids = getPackageGids(parcel.readString(), parcel.readInt(), parcel.readInt());
                    parcel2.writeNoException();
                    parcel2.writeIntArray(packageGids);
                    return true;
                case 7:
                    parcel.enforceInterface(DESCRIPTOR);
                    String[] strArrCurrentToCanonicalPackageNames = currentToCanonicalPackageNames(parcel.createStringArray());
                    parcel2.writeNoException();
                    parcel2.writeStringArray(strArrCurrentToCanonicalPackageNames);
                    return true;
                case 8:
                    parcel.enforceInterface(DESCRIPTOR);
                    String[] strArrCanonicalToCurrentPackageNames = canonicalToCurrentPackageNames(parcel.createStringArray());
                    parcel2.writeNoException();
                    parcel2.writeStringArray(strArrCanonicalToCurrentPackageNames);
                    return true;
                case 9:
                    parcel.enforceInterface(DESCRIPTOR);
                    PermissionInfo permissionInfo = getPermissionInfo(parcel.readString(), parcel.readString(), parcel.readInt());
                    parcel2.writeNoException();
                    if (permissionInfo != null) {
                        parcel2.writeInt(1);
                        permissionInfo.writeToParcel(parcel2, 1);
                    } else {
                        parcel2.writeInt(0);
                    }
                    return true;
                case 10:
                    parcel.enforceInterface(DESCRIPTOR);
                    ParceledListSlice parceledListSliceQueryPermissionsByGroup = queryPermissionsByGroup(parcel.readString(), parcel.readInt());
                    parcel2.writeNoException();
                    if (parceledListSliceQueryPermissionsByGroup != null) {
                        parcel2.writeInt(1);
                        parceledListSliceQueryPermissionsByGroup.writeToParcel(parcel2, 1);
                    } else {
                        parcel2.writeInt(0);
                    }
                    return true;
                case 11:
                    parcel.enforceInterface(DESCRIPTOR);
                    PermissionGroupInfo permissionGroupInfo = getPermissionGroupInfo(parcel.readString(), parcel.readInt());
                    parcel2.writeNoException();
                    if (permissionGroupInfo != null) {
                        parcel2.writeInt(1);
                        permissionGroupInfo.writeToParcel(parcel2, 1);
                    } else {
                        parcel2.writeInt(0);
                    }
                    return true;
                case 12:
                    parcel.enforceInterface(DESCRIPTOR);
                    ParceledListSlice allPermissionGroups = getAllPermissionGroups(parcel.readInt());
                    parcel2.writeNoException();
                    if (allPermissionGroups != null) {
                        parcel2.writeInt(1);
                        allPermissionGroups.writeToParcel(parcel2, 1);
                    } else {
                        parcel2.writeInt(0);
                    }
                    return true;
                case 13:
                    parcel.enforceInterface(DESCRIPTOR);
                    ApplicationInfo applicationInfo = getApplicationInfo(parcel.readString(), parcel.readInt(), parcel.readInt());
                    parcel2.writeNoException();
                    if (applicationInfo != null) {
                        parcel2.writeInt(1);
                        applicationInfo.writeToParcel(parcel2, 1);
                    } else {
                        parcel2.writeInt(0);
                    }
                    return true;
                case 14:
                    parcel.enforceInterface(DESCRIPTOR);
                    ActivityInfo activityInfo = getActivityInfo(parcel.readInt() != 0 ? ComponentName.CREATOR.createFromParcel(parcel) : null, parcel.readInt(), parcel.readInt());
                    parcel2.writeNoException();
                    if (activityInfo != null) {
                        parcel2.writeInt(1);
                        activityInfo.writeToParcel(parcel2, 1);
                    } else {
                        parcel2.writeInt(0);
                    }
                    return true;
                case 15:
                    parcel.enforceInterface(DESCRIPTOR);
                    if (parcel.readInt() != 0) {
                        componentNameCreateFromParcel = ComponentName.CREATOR.createFromParcel(parcel);
                    } else {
                        componentNameCreateFromParcel = null;
                    }
                    boolean zActivitySupportsIntent = activitySupportsIntent(componentNameCreateFromParcel, parcel.readInt() != 0 ? Intent.CREATOR.createFromParcel(parcel) : null, parcel.readString());
                    parcel2.writeNoException();
                    parcel2.writeInt(zActivitySupportsIntent ? 1 : 0);
                    return true;
                case 16:
                    parcel.enforceInterface(DESCRIPTOR);
                    ActivityInfo receiverInfo = getReceiverInfo(parcel.readInt() != 0 ? ComponentName.CREATOR.createFromParcel(parcel) : null, parcel.readInt(), parcel.readInt());
                    parcel2.writeNoException();
                    if (receiverInfo != null) {
                        parcel2.writeInt(1);
                        receiverInfo.writeToParcel(parcel2, 1);
                    } else {
                        parcel2.writeInt(0);
                    }
                    return true;
                case 17:
                    parcel.enforceInterface(DESCRIPTOR);
                    ServiceInfo serviceInfo = getServiceInfo(parcel.readInt() != 0 ? ComponentName.CREATOR.createFromParcel(parcel) : null, parcel.readInt(), parcel.readInt());
                    parcel2.writeNoException();
                    if (serviceInfo != null) {
                        parcel2.writeInt(1);
                        serviceInfo.writeToParcel(parcel2, 1);
                    } else {
                        parcel2.writeInt(0);
                    }
                    return true;
                case 18:
                    parcel.enforceInterface(DESCRIPTOR);
                    ProviderInfo providerInfo = getProviderInfo(parcel.readInt() != 0 ? ComponentName.CREATOR.createFromParcel(parcel) : null, parcel.readInt(), parcel.readInt());
                    parcel2.writeNoException();
                    if (providerInfo != null) {
                        parcel2.writeInt(1);
                        providerInfo.writeToParcel(parcel2, 1);
                    } else {
                        parcel2.writeInt(0);
                    }
                    return true;
                case 19:
                    parcel.enforceInterface(DESCRIPTOR);
                    int iCheckPermission = checkPermission(parcel.readString(), parcel.readString(), parcel.readInt());
                    parcel2.writeNoException();
                    parcel2.writeInt(iCheckPermission);
                    return true;
                case 20:
                    parcel.enforceInterface(DESCRIPTOR);
                    int iCheckUidPermission = checkUidPermission(parcel.readString(), parcel.readInt());
                    parcel2.writeNoException();
                    parcel2.writeInt(iCheckUidPermission);
                    return true;
                case 21:
                    parcel.enforceInterface(DESCRIPTOR);
                    boolean zAddPermission = addPermission(parcel.readInt() != 0 ? PermissionInfo.CREATOR.createFromParcel(parcel) : null);
                    parcel2.writeNoException();
                    parcel2.writeInt(zAddPermission ? 1 : 0);
                    return true;
                case 22:
                    parcel.enforceInterface(DESCRIPTOR);
                    removePermission(parcel.readString());
                    parcel2.writeNoException();
                    return true;
                case 23:
                    parcel.enforceInterface(DESCRIPTOR);
                    grantRuntimePermission(parcel.readString(), parcel.readString(), parcel.readInt());
                    parcel2.writeNoException();
                    return true;
                case 24:
                    parcel.enforceInterface(DESCRIPTOR);
                    revokeRuntimePermission(parcel.readString(), parcel.readString(), parcel.readInt());
                    parcel2.writeNoException();
                    return true;
                case 25:
                    parcel.enforceInterface(DESCRIPTOR);
                    resetRuntimePermissions();
                    parcel2.writeNoException();
                    return true;
                case 26:
                    parcel.enforceInterface(DESCRIPTOR);
                    int permissionFlags = getPermissionFlags(parcel.readString(), parcel.readString(), parcel.readInt());
                    parcel2.writeNoException();
                    parcel2.writeInt(permissionFlags);
                    return true;
                case 27:
                    parcel.enforceInterface(DESCRIPTOR);
                    updatePermissionFlags(parcel.readString(), parcel.readString(), parcel.readInt(), parcel.readInt(), parcel.readInt());
                    parcel2.writeNoException();
                    return true;
                case 28:
                    parcel.enforceInterface(DESCRIPTOR);
                    updatePermissionFlagsForAllApps(parcel.readInt(), parcel.readInt(), parcel.readInt());
                    parcel2.writeNoException();
                    return true;
                case 29:
                    parcel.enforceInterface(DESCRIPTOR);
                    boolean zShouldShowRequestPermissionRationale = shouldShowRequestPermissionRationale(parcel.readString(), parcel.readString(), parcel.readInt());
                    parcel2.writeNoException();
                    parcel2.writeInt(zShouldShowRequestPermissionRationale ? 1 : 0);
                    return true;
                case 30:
                    parcel.enforceInterface(DESCRIPTOR);
                    boolean zIsProtectedBroadcast = isProtectedBroadcast(parcel.readString());
                    parcel2.writeNoException();
                    parcel2.writeInt(zIsProtectedBroadcast ? 1 : 0);
                    return true;
                case 31:
                    parcel.enforceInterface(DESCRIPTOR);
                    int iCheckSignatures = checkSignatures(parcel.readString(), parcel.readString());
                    parcel2.writeNoException();
                    parcel2.writeInt(iCheckSignatures);
                    return true;
                case 32:
                    parcel.enforceInterface(DESCRIPTOR);
                    int iCheckUidSignatures = checkUidSignatures(parcel.readInt(), parcel.readInt());
                    parcel2.writeNoException();
                    parcel2.writeInt(iCheckUidSignatures);
                    return true;
                case 33:
                    parcel.enforceInterface(DESCRIPTOR);
                    List<String> allPackages = getAllPackages();
                    parcel2.writeNoException();
                    parcel2.writeStringList(allPackages);
                    return true;
                case 34:
                    parcel.enforceInterface(DESCRIPTOR);
                    String[] packagesForUid = getPackagesForUid(parcel.readInt());
                    parcel2.writeNoException();
                    parcel2.writeStringArray(packagesForUid);
                    return true;
                case 35:
                    parcel.enforceInterface(DESCRIPTOR);
                    String nameForUid = getNameForUid(parcel.readInt());
                    parcel2.writeNoException();
                    parcel2.writeString(nameForUid);
                    return true;
                case 36:
                    parcel.enforceInterface(DESCRIPTOR);
                    String[] namesForUids = getNamesForUids(parcel.createIntArray());
                    parcel2.writeNoException();
                    parcel2.writeStringArray(namesForUids);
                    return true;
                case 37:
                    parcel.enforceInterface(DESCRIPTOR);
                    int uidForSharedUser = getUidForSharedUser(parcel.readString());
                    parcel2.writeNoException();
                    parcel2.writeInt(uidForSharedUser);
                    return true;
                case 38:
                    parcel.enforceInterface(DESCRIPTOR);
                    int flagsForUid = getFlagsForUid(parcel.readInt());
                    parcel2.writeNoException();
                    parcel2.writeInt(flagsForUid);
                    return true;
                case 39:
                    parcel.enforceInterface(DESCRIPTOR);
                    int privateFlagsForUid = getPrivateFlagsForUid(parcel.readInt());
                    parcel2.writeNoException();
                    parcel2.writeInt(privateFlagsForUid);
                    return true;
                case 40:
                    parcel.enforceInterface(DESCRIPTOR);
                    boolean zIsUidPrivileged = isUidPrivileged(parcel.readInt());
                    parcel2.writeNoException();
                    parcel2.writeInt(zIsUidPrivileged ? 1 : 0);
                    return true;
                case 41:
                    parcel.enforceInterface(DESCRIPTOR);
                    String[] appOpPermissionPackages = getAppOpPermissionPackages(parcel.readString());
                    parcel2.writeNoException();
                    parcel2.writeStringArray(appOpPermissionPackages);
                    return true;
                case 42:
                    parcel.enforceInterface(DESCRIPTOR);
                    ResolveInfo resolveInfoResolveIntent = resolveIntent(parcel.readInt() != 0 ? Intent.CREATOR.createFromParcel(parcel) : null, parcel.readString(), parcel.readInt(), parcel.readInt());
                    parcel2.writeNoException();
                    if (resolveInfoResolveIntent != null) {
                        parcel2.writeInt(1);
                        resolveInfoResolveIntent.writeToParcel(parcel2, 1);
                    } else {
                        parcel2.writeInt(0);
                    }
                    return true;
                case 43:
                    parcel.enforceInterface(DESCRIPTOR);
                    ResolveInfo resolveInfoFindPersistentPreferredActivity = findPersistentPreferredActivity(parcel.readInt() != 0 ? Intent.CREATOR.createFromParcel(parcel) : null, parcel.readInt());
                    parcel2.writeNoException();
                    if (resolveInfoFindPersistentPreferredActivity != null) {
                        parcel2.writeInt(1);
                        resolveInfoFindPersistentPreferredActivity.writeToParcel(parcel2, 1);
                    } else {
                        parcel2.writeInt(0);
                    }
                    return true;
                case 44:
                    parcel.enforceInterface(DESCRIPTOR);
                    boolean zCanForwardTo = canForwardTo(parcel.readInt() != 0 ? Intent.CREATOR.createFromParcel(parcel) : null, parcel.readString(), parcel.readInt(), parcel.readInt());
                    parcel2.writeNoException();
                    parcel2.writeInt(zCanForwardTo ? 1 : 0);
                    return true;
                case 45:
                    parcel.enforceInterface(DESCRIPTOR);
                    ParceledListSlice parceledListSliceQueryIntentActivities = queryIntentActivities(parcel.readInt() != 0 ? Intent.CREATOR.createFromParcel(parcel) : null, parcel.readString(), parcel.readInt(), parcel.readInt());
                    parcel2.writeNoException();
                    if (parceledListSliceQueryIntentActivities != null) {
                        parcel2.writeInt(1);
                        parceledListSliceQueryIntentActivities.writeToParcel(parcel2, 1);
                    } else {
                        parcel2.writeInt(0);
                    }
                    return true;
                case 46:
                    parcel.enforceInterface(DESCRIPTOR);
                    if (parcel.readInt() != 0) {
                        componentNameCreateFromParcel2 = ComponentName.CREATOR.createFromParcel(parcel);
                    } else {
                        componentNameCreateFromParcel2 = null;
                    }
                    ParceledListSlice parceledListSliceQueryIntentActivityOptions = queryIntentActivityOptions(componentNameCreateFromParcel2, (Intent[]) parcel.createTypedArray(Intent.CREATOR), parcel.createStringArray(), parcel.readInt() != 0 ? Intent.CREATOR.createFromParcel(parcel) : null, parcel.readString(), parcel.readInt(), parcel.readInt());
                    parcel2.writeNoException();
                    if (parceledListSliceQueryIntentActivityOptions != null) {
                        parcel2.writeInt(1);
                        parceledListSliceQueryIntentActivityOptions.writeToParcel(parcel2, 1);
                    } else {
                        parcel2.writeInt(0);
                    }
                    return true;
                case 47:
                    parcel.enforceInterface(DESCRIPTOR);
                    ParceledListSlice parceledListSliceQueryIntentReceivers = queryIntentReceivers(parcel.readInt() != 0 ? Intent.CREATOR.createFromParcel(parcel) : null, parcel.readString(), parcel.readInt(), parcel.readInt());
                    parcel2.writeNoException();
                    if (parceledListSliceQueryIntentReceivers != null) {
                        parcel2.writeInt(1);
                        parceledListSliceQueryIntentReceivers.writeToParcel(parcel2, 1);
                    } else {
                        parcel2.writeInt(0);
                    }
                    return true;
                case 48:
                    parcel.enforceInterface(DESCRIPTOR);
                    ResolveInfo resolveInfoResolveService = resolveService(parcel.readInt() != 0 ? Intent.CREATOR.createFromParcel(parcel) : null, parcel.readString(), parcel.readInt(), parcel.readInt());
                    parcel2.writeNoException();
                    if (resolveInfoResolveService != null) {
                        parcel2.writeInt(1);
                        resolveInfoResolveService.writeToParcel(parcel2, 1);
                    } else {
                        parcel2.writeInt(0);
                    }
                    return true;
                case 49:
                    parcel.enforceInterface(DESCRIPTOR);
                    ParceledListSlice parceledListSliceQueryIntentServices = queryIntentServices(parcel.readInt() != 0 ? Intent.CREATOR.createFromParcel(parcel) : null, parcel.readString(), parcel.readInt(), parcel.readInt());
                    parcel2.writeNoException();
                    if (parceledListSliceQueryIntentServices != null) {
                        parcel2.writeInt(1);
                        parceledListSliceQueryIntentServices.writeToParcel(parcel2, 1);
                    } else {
                        parcel2.writeInt(0);
                    }
                    return true;
                case 50:
                    parcel.enforceInterface(DESCRIPTOR);
                    ParceledListSlice parceledListSliceQueryIntentContentProviders = queryIntentContentProviders(parcel.readInt() != 0 ? Intent.CREATOR.createFromParcel(parcel) : null, parcel.readString(), parcel.readInt(), parcel.readInt());
                    parcel2.writeNoException();
                    if (parceledListSliceQueryIntentContentProviders != null) {
                        parcel2.writeInt(1);
                        parceledListSliceQueryIntentContentProviders.writeToParcel(parcel2, 1);
                    } else {
                        parcel2.writeInt(0);
                    }
                    return true;
                case 51:
                    parcel.enforceInterface(DESCRIPTOR);
                    ParceledListSlice installedPackages = getInstalledPackages(parcel.readInt(), parcel.readInt());
                    parcel2.writeNoException();
                    if (installedPackages != null) {
                        parcel2.writeInt(1);
                        installedPackages.writeToParcel(parcel2, 1);
                    } else {
                        parcel2.writeInt(0);
                    }
                    return true;
                case 52:
                    parcel.enforceInterface(DESCRIPTOR);
                    ParceledListSlice packagesHoldingPermissions = getPackagesHoldingPermissions(parcel.createStringArray(), parcel.readInt(), parcel.readInt());
                    parcel2.writeNoException();
                    if (packagesHoldingPermissions != null) {
                        parcel2.writeInt(1);
                        packagesHoldingPermissions.writeToParcel(parcel2, 1);
                    } else {
                        parcel2.writeInt(0);
                    }
                    return true;
                case 53:
                    parcel.enforceInterface(DESCRIPTOR);
                    ParceledListSlice installedApplications = getInstalledApplications(parcel.readInt(), parcel.readInt());
                    parcel2.writeNoException();
                    if (installedApplications != null) {
                        parcel2.writeInt(1);
                        installedApplications.writeToParcel(parcel2, 1);
                    } else {
                        parcel2.writeInt(0);
                    }
                    return true;
                case 54:
                    parcel.enforceInterface(DESCRIPTOR);
                    ParceledListSlice persistentApplications = getPersistentApplications(parcel.readInt());
                    parcel2.writeNoException();
                    if (persistentApplications != null) {
                        parcel2.writeInt(1);
                        persistentApplications.writeToParcel(parcel2, 1);
                    } else {
                        parcel2.writeInt(0);
                    }
                    return true;
                case 55:
                    parcel.enforceInterface(DESCRIPTOR);
                    ProviderInfo providerInfoResolveContentProvider = resolveContentProvider(parcel.readString(), parcel.readInt(), parcel.readInt());
                    parcel2.writeNoException();
                    if (providerInfoResolveContentProvider != null) {
                        parcel2.writeInt(1);
                        providerInfoResolveContentProvider.writeToParcel(parcel2, 1);
                    } else {
                        parcel2.writeInt(0);
                    }
                    return true;
                case 56:
                    parcel.enforceInterface(DESCRIPTOR);
                    ArrayList<String> arrayListCreateStringArrayList = parcel.createStringArrayList();
                    ArrayList arrayListCreateTypedArrayList = parcel.createTypedArrayList(ProviderInfo.CREATOR);
                    querySyncProviders(arrayListCreateStringArrayList, arrayListCreateTypedArrayList);
                    parcel2.writeNoException();
                    parcel2.writeStringList(arrayListCreateStringArrayList);
                    parcel2.writeTypedList(arrayListCreateTypedArrayList);
                    return true;
                case 57:
                    parcel.enforceInterface(DESCRIPTOR);
                    ParceledListSlice parceledListSliceQueryContentProviders = queryContentProviders(parcel.readString(), parcel.readInt(), parcel.readInt(), parcel.readString());
                    parcel2.writeNoException();
                    if (parceledListSliceQueryContentProviders != null) {
                        parcel2.writeInt(1);
                        parceledListSliceQueryContentProviders.writeToParcel(parcel2, 1);
                    } else {
                        parcel2.writeInt(0);
                    }
                    return true;
                case 58:
                    parcel.enforceInterface(DESCRIPTOR);
                    InstrumentationInfo instrumentationInfo = getInstrumentationInfo(parcel.readInt() != 0 ? ComponentName.CREATOR.createFromParcel(parcel) : null, parcel.readInt());
                    parcel2.writeNoException();
                    if (instrumentationInfo != null) {
                        parcel2.writeInt(1);
                        instrumentationInfo.writeToParcel(parcel2, 1);
                    } else {
                        parcel2.writeInt(0);
                    }
                    return true;
                case 59:
                    parcel.enforceInterface(DESCRIPTOR);
                    ParceledListSlice parceledListSliceQueryInstrumentation = queryInstrumentation(parcel.readString(), parcel.readInt());
                    parcel2.writeNoException();
                    if (parceledListSliceQueryInstrumentation != null) {
                        parcel2.writeInt(1);
                        parceledListSliceQueryInstrumentation.writeToParcel(parcel2, 1);
                    } else {
                        parcel2.writeInt(0);
                    }
                    return true;
                case 60:
                    parcel.enforceInterface(DESCRIPTOR);
                    finishPackageInstall(parcel.readInt(), parcel.readInt() != 0);
                    parcel2.writeNoException();
                    return true;
                case 61:
                    parcel.enforceInterface(DESCRIPTOR);
                    setInstallerPackageName(parcel.readString(), parcel.readString());
                    parcel2.writeNoException();
                    return true;
                case 62:
                    parcel.enforceInterface(DESCRIPTOR);
                    setApplicationCategoryHint(parcel.readString(), parcel.readInt(), parcel.readString());
                    parcel2.writeNoException();
                    return true;
                case 63:
                    parcel.enforceInterface(DESCRIPTOR);
                    deletePackageAsUser(parcel.readString(), parcel.readInt(), IPackageDeleteObserver.Stub.asInterface(parcel.readStrongBinder()), parcel.readInt(), parcel.readInt());
                    parcel2.writeNoException();
                    return true;
                case 64:
                    parcel.enforceInterface(DESCRIPTOR);
                    deletePackageVersioned(parcel.readInt() != 0 ? VersionedPackage.CREATOR.createFromParcel(parcel) : null, IPackageDeleteObserver2.Stub.asInterface(parcel.readStrongBinder()), parcel.readInt(), parcel.readInt());
                    parcel2.writeNoException();
                    return true;
                case 65:
                    parcel.enforceInterface(DESCRIPTOR);
                    String installerPackageName = getInstallerPackageName(parcel.readString());
                    parcel2.writeNoException();
                    parcel2.writeString(installerPackageName);
                    return true;
                case 66:
                    parcel.enforceInterface(DESCRIPTOR);
                    resetApplicationPreferences(parcel.readInt());
                    parcel2.writeNoException();
                    return true;
                case 67:
                    parcel.enforceInterface(DESCRIPTOR);
                    ResolveInfo lastChosenActivity = getLastChosenActivity(parcel.readInt() != 0 ? Intent.CREATOR.createFromParcel(parcel) : null, parcel.readString(), parcel.readInt());
                    parcel2.writeNoException();
                    if (lastChosenActivity != null) {
                        parcel2.writeInt(1);
                        lastChosenActivity.writeToParcel(parcel2, 1);
                    } else {
                        parcel2.writeInt(0);
                    }
                    return true;
                case 68:
                    parcel.enforceInterface(DESCRIPTOR);
                    if (parcel.readInt() != 0) {
                        intentCreateFromParcel = Intent.CREATOR.createFromParcel(parcel);
                    } else {
                        intentCreateFromParcel = null;
                    }
                    String string = parcel.readString();
                    int i3 = parcel.readInt();
                    if (parcel.readInt() != 0) {
                        intentFilterCreateFromParcel = IntentFilter.CREATOR.createFromParcel(parcel);
                    } else {
                        intentFilterCreateFromParcel = null;
                    }
                    int i4 = parcel.readInt();
                    if (parcel.readInt() != 0) {
                        componentNameCreateFromParcel3 = ComponentName.CREATOR.createFromParcel(parcel);
                    } else {
                        componentNameCreateFromParcel3 = null;
                    }
                    setLastChosenActivity(intentCreateFromParcel, string, i3, intentFilterCreateFromParcel, i4, componentNameCreateFromParcel3);
                    parcel2.writeNoException();
                    return true;
                case 69:
                    parcel.enforceInterface(DESCRIPTOR);
                    if (parcel.readInt() != 0) {
                        intentFilterCreateFromParcel2 = IntentFilter.CREATOR.createFromParcel(parcel);
                    } else {
                        intentFilterCreateFromParcel2 = null;
                    }
                    addPreferredActivity(intentFilterCreateFromParcel2, parcel.readInt(), (ComponentName[]) parcel.createTypedArray(ComponentName.CREATOR), parcel.readInt() != 0 ? ComponentName.CREATOR.createFromParcel(parcel) : null, parcel.readInt());
                    parcel2.writeNoException();
                    return true;
                case 70:
                    parcel.enforceInterface(DESCRIPTOR);
                    if (parcel.readInt() != 0) {
                        intentFilterCreateFromParcel3 = IntentFilter.CREATOR.createFromParcel(parcel);
                    } else {
                        intentFilterCreateFromParcel3 = null;
                    }
                    replacePreferredActivity(intentFilterCreateFromParcel3, parcel.readInt(), (ComponentName[]) parcel.createTypedArray(ComponentName.CREATOR), parcel.readInt() != 0 ? ComponentName.CREATOR.createFromParcel(parcel) : null, parcel.readInt());
                    parcel2.writeNoException();
                    return true;
                case 71:
                    parcel.enforceInterface(DESCRIPTOR);
                    clearPackagePreferredActivities(parcel.readString());
                    parcel2.writeNoException();
                    return true;
                case 72:
                    parcel.enforceInterface(DESCRIPTOR);
                    ArrayList arrayList = new ArrayList();
                    ArrayList arrayList2 = new ArrayList();
                    int preferredActivities = getPreferredActivities(arrayList, arrayList2, parcel.readString());
                    parcel2.writeNoException();
                    parcel2.writeInt(preferredActivities);
                    parcel2.writeTypedList(arrayList);
                    parcel2.writeTypedList(arrayList2);
                    return true;
                case 73:
                    parcel.enforceInterface(DESCRIPTOR);
                    if (parcel.readInt() != 0) {
                        intentFilterCreateFromParcel4 = IntentFilter.CREATOR.createFromParcel(parcel);
                    } else {
                        intentFilterCreateFromParcel4 = null;
                    }
                    addPersistentPreferredActivity(intentFilterCreateFromParcel4, parcel.readInt() != 0 ? ComponentName.CREATOR.createFromParcel(parcel) : null, parcel.readInt());
                    parcel2.writeNoException();
                    return true;
                case 74:
                    parcel.enforceInterface(DESCRIPTOR);
                    clearPackagePersistentPreferredActivities(parcel.readString(), parcel.readInt());
                    parcel2.writeNoException();
                    return true;
                case 75:
                    parcel.enforceInterface(DESCRIPTOR);
                    if (parcel.readInt() != 0) {
                        intentFilterCreateFromParcel5 = IntentFilter.CREATOR.createFromParcel(parcel);
                    } else {
                        intentFilterCreateFromParcel5 = null;
                    }
                    addCrossProfileIntentFilter(intentFilterCreateFromParcel5, parcel.readString(), parcel.readInt(), parcel.readInt(), parcel.readInt());
                    parcel2.writeNoException();
                    return true;
                case 76:
                    parcel.enforceInterface(DESCRIPTOR);
                    clearCrossProfileIntentFilters(parcel.readInt(), parcel.readString());
                    parcel2.writeNoException();
                    return true;
                case 77:
                    parcel.enforceInterface(DESCRIPTOR);
                    String[] strArrCreateStringArray = parcel.createStringArray();
                    boolean z = parcel.readInt() != 0;
                    if (parcel.readInt() != 0) {
                        persistableBundleCreateFromParcel = PersistableBundle.CREATOR.createFromParcel(parcel);
                    } else {
                        persistableBundleCreateFromParcel = null;
                    }
                    String[] packagesSuspendedAsUser = setPackagesSuspendedAsUser(strArrCreateStringArray, z, persistableBundleCreateFromParcel, parcel.readInt() != 0 ? PersistableBundle.CREATOR.createFromParcel(parcel) : null, parcel.readString(), parcel.readString(), parcel.readInt());
                    parcel2.writeNoException();
                    parcel2.writeStringArray(packagesSuspendedAsUser);
                    return true;
                case 78:
                    parcel.enforceInterface(DESCRIPTOR);
                    boolean zIsPackageSuspendedForUser = isPackageSuspendedForUser(parcel.readString(), parcel.readInt());
                    parcel2.writeNoException();
                    parcel2.writeInt(zIsPackageSuspendedForUser ? 1 : 0);
                    return true;
                case 79:
                    parcel.enforceInterface(DESCRIPTOR);
                    PersistableBundle suspendedPackageAppExtras = getSuspendedPackageAppExtras(parcel.readString(), parcel.readInt());
                    parcel2.writeNoException();
                    if (suspendedPackageAppExtras != null) {
                        parcel2.writeInt(1);
                        suspendedPackageAppExtras.writeToParcel(parcel2, 1);
                    } else {
                        parcel2.writeInt(0);
                    }
                    return true;
                case 80:
                    parcel.enforceInterface(DESCRIPTOR);
                    byte[] preferredActivityBackup = getPreferredActivityBackup(parcel.readInt());
                    parcel2.writeNoException();
                    parcel2.writeByteArray(preferredActivityBackup);
                    return true;
                case 81:
                    parcel.enforceInterface(DESCRIPTOR);
                    restorePreferredActivities(parcel.createByteArray(), parcel.readInt());
                    parcel2.writeNoException();
                    return true;
                case 82:
                    parcel.enforceInterface(DESCRIPTOR);
                    byte[] defaultAppsBackup = getDefaultAppsBackup(parcel.readInt());
                    parcel2.writeNoException();
                    parcel2.writeByteArray(defaultAppsBackup);
                    return true;
                case 83:
                    parcel.enforceInterface(DESCRIPTOR);
                    restoreDefaultApps(parcel.createByteArray(), parcel.readInt());
                    parcel2.writeNoException();
                    return true;
                case 84:
                    parcel.enforceInterface(DESCRIPTOR);
                    byte[] intentFilterVerificationBackup = getIntentFilterVerificationBackup(parcel.readInt());
                    parcel2.writeNoException();
                    parcel2.writeByteArray(intentFilterVerificationBackup);
                    return true;
                case 85:
                    parcel.enforceInterface(DESCRIPTOR);
                    restoreIntentFilterVerification(parcel.createByteArray(), parcel.readInt());
                    parcel2.writeNoException();
                    return true;
                case 86:
                    parcel.enforceInterface(DESCRIPTOR);
                    byte[] permissionGrantBackup = getPermissionGrantBackup(parcel.readInt());
                    parcel2.writeNoException();
                    parcel2.writeByteArray(permissionGrantBackup);
                    return true;
                case 87:
                    parcel.enforceInterface(DESCRIPTOR);
                    restorePermissionGrants(parcel.createByteArray(), parcel.readInt());
                    parcel2.writeNoException();
                    return true;
                case 88:
                    parcel.enforceInterface(DESCRIPTOR);
                    ArrayList arrayList3 = new ArrayList();
                    ComponentName homeActivities = getHomeActivities(arrayList3);
                    parcel2.writeNoException();
                    if (homeActivities != null) {
                        parcel2.writeInt(1);
                        homeActivities.writeToParcel(parcel2, 1);
                    } else {
                        parcel2.writeInt(0);
                    }
                    parcel2.writeTypedList(arrayList3);
                    return true;
                case 89:
                    parcel.enforceInterface(DESCRIPTOR);
                    setHomeActivity(parcel.readInt() != 0 ? ComponentName.CREATOR.createFromParcel(parcel) : null, parcel.readInt());
                    parcel2.writeNoException();
                    return true;
                case 90:
                    parcel.enforceInterface(DESCRIPTOR);
                    setComponentEnabledSetting(parcel.readInt() != 0 ? ComponentName.CREATOR.createFromParcel(parcel) : null, parcel.readInt(), parcel.readInt(), parcel.readInt());
                    parcel2.writeNoException();
                    return true;
                case 91:
                    parcel.enforceInterface(DESCRIPTOR);
                    int componentEnabledSetting = getComponentEnabledSetting(parcel.readInt() != 0 ? ComponentName.CREATOR.createFromParcel(parcel) : null, parcel.readInt());
                    parcel2.writeNoException();
                    parcel2.writeInt(componentEnabledSetting);
                    return true;
                case 92:
                    parcel.enforceInterface(DESCRIPTOR);
                    setApplicationEnabledSetting(parcel.readString(), parcel.readInt(), parcel.readInt(), parcel.readInt(), parcel.readString());
                    parcel2.writeNoException();
                    return true;
                case 93:
                    parcel.enforceInterface(DESCRIPTOR);
                    int applicationEnabledSetting = getApplicationEnabledSetting(parcel.readString(), parcel.readInt());
                    parcel2.writeNoException();
                    parcel2.writeInt(applicationEnabledSetting);
                    return true;
                case 94:
                    parcel.enforceInterface(DESCRIPTOR);
                    logAppProcessStartIfNeeded(parcel.readString(), parcel.readInt(), parcel.readString(), parcel.readString(), parcel.readInt());
                    parcel2.writeNoException();
                    return true;
                case 95:
                    parcel.enforceInterface(DESCRIPTOR);
                    flushPackageRestrictionsAsUser(parcel.readInt());
                    parcel2.writeNoException();
                    return true;
                case 96:
                    parcel.enforceInterface(DESCRIPTOR);
                    setPackageStoppedState(parcel.readString(), parcel.readInt() != 0, parcel.readInt());
                    parcel2.writeNoException();
                    return true;
                case 97:
                    parcel.enforceInterface(DESCRIPTOR);
                    freeStorageAndNotify(parcel.readString(), parcel.readLong(), parcel.readInt(), IPackageDataObserver.Stub.asInterface(parcel.readStrongBinder()));
                    parcel2.writeNoException();
                    return true;
                case 98:
                    parcel.enforceInterface(DESCRIPTOR);
                    String string2 = parcel.readString();
                    long j = parcel.readLong();
                    int i5 = parcel.readInt();
                    if (parcel.readInt() != 0) {
                        intentSenderCreateFromParcel = IntentSender.CREATOR.createFromParcel(parcel);
                    } else {
                        intentSenderCreateFromParcel = null;
                    }
                    freeStorage(string2, j, i5, intentSenderCreateFromParcel);
                    parcel2.writeNoException();
                    return true;
                case 99:
                    parcel.enforceInterface(DESCRIPTOR);
                    deleteApplicationCacheFiles(parcel.readString(), IPackageDataObserver.Stub.asInterface(parcel.readStrongBinder()));
                    parcel2.writeNoException();
                    return true;
                case 100:
                    parcel.enforceInterface(DESCRIPTOR);
                    deleteApplicationCacheFilesAsUser(parcel.readString(), parcel.readInt(), IPackageDataObserver.Stub.asInterface(parcel.readStrongBinder()));
                    parcel2.writeNoException();
                    return true;
                case 101:
                    parcel.enforceInterface(DESCRIPTOR);
                    clearApplicationUserData(parcel.readString(), IPackageDataObserver.Stub.asInterface(parcel.readStrongBinder()), parcel.readInt());
                    parcel2.writeNoException();
                    return true;
                case 102:
                    parcel.enforceInterface(DESCRIPTOR);
                    clearApplicationProfileData(parcel.readString());
                    parcel2.writeNoException();
                    return true;
                case 103:
                    parcel.enforceInterface(DESCRIPTOR);
                    getPackageSizeInfo(parcel.readString(), parcel.readInt(), IPackageStatsObserver.Stub.asInterface(parcel.readStrongBinder()));
                    parcel2.writeNoException();
                    return true;
                case 104:
                    parcel.enforceInterface(DESCRIPTOR);
                    String[] systemSharedLibraryNames = getSystemSharedLibraryNames();
                    parcel2.writeNoException();
                    parcel2.writeStringArray(systemSharedLibraryNames);
                    return true;
                case 105:
                    parcel.enforceInterface(DESCRIPTOR);
                    ParceledListSlice systemAvailableFeatures = getSystemAvailableFeatures();
                    parcel2.writeNoException();
                    if (systemAvailableFeatures != null) {
                        parcel2.writeInt(1);
                        systemAvailableFeatures.writeToParcel(parcel2, 1);
                    } else {
                        parcel2.writeInt(0);
                    }
                    return true;
                case 106:
                    parcel.enforceInterface(DESCRIPTOR);
                    boolean zHasSystemFeature = hasSystemFeature(parcel.readString(), parcel.readInt());
                    parcel2.writeNoException();
                    parcel2.writeInt(zHasSystemFeature ? 1 : 0);
                    return true;
                case 107:
                    parcel.enforceInterface(DESCRIPTOR);
                    enterSafeMode();
                    parcel2.writeNoException();
                    return true;
                case 108:
                    parcel.enforceInterface(DESCRIPTOR);
                    boolean zIsSafeMode = isSafeMode();
                    parcel2.writeNoException();
                    parcel2.writeInt(zIsSafeMode ? 1 : 0);
                    return true;
                case 109:
                    parcel.enforceInterface(DESCRIPTOR);
                    systemReady();
                    parcel2.writeNoException();
                    return true;
                case 110:
                    parcel.enforceInterface(DESCRIPTOR);
                    boolean zHasSystemUidErrors = hasSystemUidErrors();
                    parcel2.writeNoException();
                    parcel2.writeInt(zHasSystemUidErrors ? 1 : 0);
                    return true;
                case 111:
                    parcel.enforceInterface(DESCRIPTOR);
                    performFstrimIfNeeded();
                    parcel2.writeNoException();
                    return true;
                case 112:
                    parcel.enforceInterface(DESCRIPTOR);
                    updatePackagesIfNeeded();
                    parcel2.writeNoException();
                    return true;
                case 113:
                    parcel.enforceInterface(DESCRIPTOR);
                    notifyPackageUse(parcel.readString(), parcel.readInt());
                    return true;
                case 114:
                    parcel.enforceInterface(DESCRIPTOR);
                    notifyDexLoad(parcel.readString(), parcel.createStringArrayList(), parcel.createStringArrayList(), parcel.readString());
                    return true;
                case 115:
                    parcel.enforceInterface(DESCRIPTOR);
                    registerDexModule(parcel.readString(), parcel.readString(), parcel.readInt() != 0, IDexModuleRegisterCallback.Stub.asInterface(parcel.readStrongBinder()));
                    return true;
                case 116:
                    parcel.enforceInterface(DESCRIPTOR);
                    boolean zPerformDexOptMode = performDexOptMode(parcel.readString(), parcel.readInt() != 0, parcel.readString(), parcel.readInt() != 0, parcel.readInt() != 0, parcel.readString());
                    parcel2.writeNoException();
                    parcel2.writeInt(zPerformDexOptMode ? 1 : 0);
                    return true;
                case 117:
                    parcel.enforceInterface(DESCRIPTOR);
                    boolean zPerformDexOptSecondary = performDexOptSecondary(parcel.readString(), parcel.readString(), parcel.readInt() != 0);
                    parcel2.writeNoException();
                    parcel2.writeInt(zPerformDexOptSecondary ? 1 : 0);
                    return true;
                case 118:
                    parcel.enforceInterface(DESCRIPTOR);
                    dumpProfiles(parcel.readString());
                    parcel2.writeNoException();
                    return true;
                case 119:
                    parcel.enforceInterface(DESCRIPTOR);
                    forceDexOpt(parcel.readString());
                    parcel2.writeNoException();
                    return true;
                case 120:
                    parcel.enforceInterface(DESCRIPTOR);
                    boolean zRunBackgroundDexoptJob = runBackgroundDexoptJob(parcel.createStringArrayList());
                    parcel2.writeNoException();
                    parcel2.writeInt(zRunBackgroundDexoptJob ? 1 : 0);
                    return true;
                case 121:
                    parcel.enforceInterface(DESCRIPTOR);
                    reconcileSecondaryDexFiles(parcel.readString());
                    parcel2.writeNoException();
                    return true;
                case 122:
                    parcel.enforceInterface(DESCRIPTOR);
                    PackageCleanItem packageCleanItemNextPackageToClean = nextPackageToClean(parcel.readInt() != 0 ? PackageCleanItem.CREATOR.createFromParcel(parcel) : null);
                    parcel2.writeNoException();
                    if (packageCleanItemNextPackageToClean != null) {
                        parcel2.writeInt(1);
                        packageCleanItemNextPackageToClean.writeToParcel(parcel2, 1);
                    } else {
                        parcel2.writeInt(0);
                    }
                    return true;
                case 123:
                    parcel.enforceInterface(DESCRIPTOR);
                    int moveStatus = getMoveStatus(parcel.readInt());
                    parcel2.writeNoException();
                    parcel2.writeInt(moveStatus);
                    return true;
                case 124:
                    parcel.enforceInterface(DESCRIPTOR);
                    registerMoveCallback(IPackageMoveObserver.Stub.asInterface(parcel.readStrongBinder()));
                    parcel2.writeNoException();
                    return true;
                case 125:
                    parcel.enforceInterface(DESCRIPTOR);
                    unregisterMoveCallback(IPackageMoveObserver.Stub.asInterface(parcel.readStrongBinder()));
                    parcel2.writeNoException();
                    return true;
                case 126:
                    parcel.enforceInterface(DESCRIPTOR);
                    int iMovePackage = movePackage(parcel.readString(), parcel.readString());
                    parcel2.writeNoException();
                    parcel2.writeInt(iMovePackage);
                    return true;
                case 127:
                    parcel.enforceInterface(DESCRIPTOR);
                    int iMovePrimaryStorage = movePrimaryStorage(parcel.readString());
                    parcel2.writeNoException();
                    parcel2.writeInt(iMovePrimaryStorage);
                    return true;
                case 128:
                    parcel.enforceInterface(DESCRIPTOR);
                    boolean zAddPermissionAsync = addPermissionAsync(parcel.readInt() != 0 ? PermissionInfo.CREATOR.createFromParcel(parcel) : null);
                    parcel2.writeNoException();
                    parcel2.writeInt(zAddPermissionAsync ? 1 : 0);
                    return true;
                case 129:
                    parcel.enforceInterface(DESCRIPTOR);
                    boolean installLocation = setInstallLocation(parcel.readInt());
                    parcel2.writeNoException();
                    parcel2.writeInt(installLocation ? 1 : 0);
                    return true;
                case 130:
                    parcel.enforceInterface(DESCRIPTOR);
                    int installLocation2 = getInstallLocation();
                    parcel2.writeNoException();
                    parcel2.writeInt(installLocation2);
                    return true;
                case 131:
                    parcel.enforceInterface(DESCRIPTOR);
                    int iInstallExistingPackageAsUser = installExistingPackageAsUser(parcel.readString(), parcel.readInt(), parcel.readInt(), parcel.readInt());
                    parcel2.writeNoException();
                    parcel2.writeInt(iInstallExistingPackageAsUser);
                    return true;
                case 132:
                    parcel.enforceInterface(DESCRIPTOR);
                    verifyPendingInstall(parcel.readInt(), parcel.readInt());
                    parcel2.writeNoException();
                    return true;
                case 133:
                    parcel.enforceInterface(DESCRIPTOR);
                    extendVerificationTimeout(parcel.readInt(), parcel.readInt(), parcel.readLong());
                    parcel2.writeNoException();
                    return true;
                case 134:
                    parcel.enforceInterface(DESCRIPTOR);
                    verifyIntentFilter(parcel.readInt(), parcel.readInt(), parcel.createStringArrayList());
                    parcel2.writeNoException();
                    return true;
                case 135:
                    parcel.enforceInterface(DESCRIPTOR);
                    int intentVerificationStatus = getIntentVerificationStatus(parcel.readString(), parcel.readInt());
                    parcel2.writeNoException();
                    parcel2.writeInt(intentVerificationStatus);
                    return true;
                case 136:
                    parcel.enforceInterface(DESCRIPTOR);
                    boolean zUpdateIntentVerificationStatus = updateIntentVerificationStatus(parcel.readString(), parcel.readInt(), parcel.readInt());
                    parcel2.writeNoException();
                    parcel2.writeInt(zUpdateIntentVerificationStatus ? 1 : 0);
                    return true;
                case 137:
                    parcel.enforceInterface(DESCRIPTOR);
                    ParceledListSlice intentFilterVerifications = getIntentFilterVerifications(parcel.readString());
                    parcel2.writeNoException();
                    if (intentFilterVerifications != null) {
                        parcel2.writeInt(1);
                        intentFilterVerifications.writeToParcel(parcel2, 1);
                    } else {
                        parcel2.writeInt(0);
                    }
                    return true;
                case 138:
                    parcel.enforceInterface(DESCRIPTOR);
                    ParceledListSlice allIntentFilters = getAllIntentFilters(parcel.readString());
                    parcel2.writeNoException();
                    if (allIntentFilters != null) {
                        parcel2.writeInt(1);
                        allIntentFilters.writeToParcel(parcel2, 1);
                    } else {
                        parcel2.writeInt(0);
                    }
                    return true;
                case 139:
                    parcel.enforceInterface(DESCRIPTOR);
                    boolean defaultBrowserPackageName = setDefaultBrowserPackageName(parcel.readString(), parcel.readInt());
                    parcel2.writeNoException();
                    parcel2.writeInt(defaultBrowserPackageName ? 1 : 0);
                    return true;
                case 140:
                    parcel.enforceInterface(DESCRIPTOR);
                    String defaultBrowserPackageName2 = getDefaultBrowserPackageName(parcel.readInt());
                    parcel2.writeNoException();
                    parcel2.writeString(defaultBrowserPackageName2);
                    return true;
                case 141:
                    parcel.enforceInterface(DESCRIPTOR);
                    VerifierDeviceIdentity verifierDeviceIdentity = getVerifierDeviceIdentity();
                    parcel2.writeNoException();
                    if (verifierDeviceIdentity != null) {
                        parcel2.writeInt(1);
                        verifierDeviceIdentity.writeToParcel(parcel2, 1);
                    } else {
                        parcel2.writeInt(0);
                    }
                    return true;
                case 142:
                    parcel.enforceInterface(DESCRIPTOR);
                    boolean zIsFirstBoot = isFirstBoot();
                    parcel2.writeNoException();
                    parcel2.writeInt(zIsFirstBoot ? 1 : 0);
                    return true;
                case 143:
                    parcel.enforceInterface(DESCRIPTOR);
                    boolean zIsOnlyCoreApps = isOnlyCoreApps();
                    parcel2.writeNoException();
                    parcel2.writeInt(zIsOnlyCoreApps ? 1 : 0);
                    return true;
                case 144:
                    parcel.enforceInterface(DESCRIPTOR);
                    boolean zIsUpgrade = isUpgrade();
                    parcel2.writeNoException();
                    parcel2.writeInt(zIsUpgrade ? 1 : 0);
                    return true;
                case 145:
                    parcel.enforceInterface(DESCRIPTOR);
                    setPermissionEnforced(parcel.readString(), parcel.readInt() != 0);
                    parcel2.writeNoException();
                    return true;
                case 146:
                    parcel.enforceInterface(DESCRIPTOR);
                    boolean zIsPermissionEnforced = isPermissionEnforced(parcel.readString());
                    parcel2.writeNoException();
                    parcel2.writeInt(zIsPermissionEnforced ? 1 : 0);
                    return true;
                case 147:
                    parcel.enforceInterface(DESCRIPTOR);
                    boolean zIsStorageLow = isStorageLow();
                    parcel2.writeNoException();
                    parcel2.writeInt(zIsStorageLow ? 1 : 0);
                    return true;
                case 148:
                    parcel.enforceInterface(DESCRIPTOR);
                    boolean applicationHiddenSettingAsUser = setApplicationHiddenSettingAsUser(parcel.readString(), parcel.readInt() != 0, parcel.readInt());
                    parcel2.writeNoException();
                    parcel2.writeInt(applicationHiddenSettingAsUser ? 1 : 0);
                    return true;
                case 149:
                    parcel.enforceInterface(DESCRIPTOR);
                    boolean applicationHiddenSettingAsUser2 = getApplicationHiddenSettingAsUser(parcel.readString(), parcel.readInt());
                    parcel2.writeNoException();
                    parcel2.writeInt(applicationHiddenSettingAsUser2 ? 1 : 0);
                    return true;
                case 150:
                    parcel.enforceInterface(DESCRIPTOR);
                    IPackageInstaller packageInstaller = getPackageInstaller();
                    parcel2.writeNoException();
                    parcel2.writeStrongBinder(packageInstaller != null ? packageInstaller.asBinder() : null);
                    return true;
                case 151:
                    parcel.enforceInterface(DESCRIPTOR);
                    boolean blockUninstallForUser = setBlockUninstallForUser(parcel.readString(), parcel.readInt() != 0, parcel.readInt());
                    parcel2.writeNoException();
                    parcel2.writeInt(blockUninstallForUser ? 1 : 0);
                    return true;
                case 152:
                    parcel.enforceInterface(DESCRIPTOR);
                    boolean blockUninstallForUser2 = getBlockUninstallForUser(parcel.readString(), parcel.readInt());
                    parcel2.writeNoException();
                    parcel2.writeInt(blockUninstallForUser2 ? 1 : 0);
                    return true;
                case 153:
                    parcel.enforceInterface(DESCRIPTOR);
                    KeySet keySetByAlias = getKeySetByAlias(parcel.readString(), parcel.readString());
                    parcel2.writeNoException();
                    if (keySetByAlias != null) {
                        parcel2.writeInt(1);
                        keySetByAlias.writeToParcel(parcel2, 1);
                    } else {
                        parcel2.writeInt(0);
                    }
                    return true;
                case 154:
                    parcel.enforceInterface(DESCRIPTOR);
                    KeySet signingKeySet = getSigningKeySet(parcel.readString());
                    parcel2.writeNoException();
                    if (signingKeySet != null) {
                        parcel2.writeInt(1);
                        signingKeySet.writeToParcel(parcel2, 1);
                    } else {
                        parcel2.writeInt(0);
                    }
                    return true;
                case 155:
                    parcel.enforceInterface(DESCRIPTOR);
                    boolean zIsPackageSignedByKeySet = isPackageSignedByKeySet(parcel.readString(), parcel.readInt() != 0 ? KeySet.CREATOR.createFromParcel(parcel) : null);
                    parcel2.writeNoException();
                    parcel2.writeInt(zIsPackageSignedByKeySet ? 1 : 0);
                    return true;
                case 156:
                    parcel.enforceInterface(DESCRIPTOR);
                    boolean zIsPackageSignedByKeySetExactly = isPackageSignedByKeySetExactly(parcel.readString(), parcel.readInt() != 0 ? KeySet.CREATOR.createFromParcel(parcel) : null);
                    parcel2.writeNoException();
                    parcel2.writeInt(zIsPackageSignedByKeySetExactly ? 1 : 0);
                    return true;
                case 157:
                    parcel.enforceInterface(DESCRIPTOR);
                    addOnPermissionsChangeListener(IOnPermissionsChangeListener.Stub.asInterface(parcel.readStrongBinder()));
                    parcel2.writeNoException();
                    return true;
                case 158:
                    parcel.enforceInterface(DESCRIPTOR);
                    removeOnPermissionsChangeListener(IOnPermissionsChangeListener.Stub.asInterface(parcel.readStrongBinder()));
                    parcel2.writeNoException();
                    return true;
                case 159:
                    parcel.enforceInterface(DESCRIPTOR);
                    grantDefaultPermissionsToEnabledCarrierApps(parcel.createStringArray(), parcel.readInt());
                    parcel2.writeNoException();
                    return true;
                case 160:
                    parcel.enforceInterface(DESCRIPTOR);
                    grantDefaultPermissionsToEnabledImsServices(parcel.createStringArray(), parcel.readInt());
                    parcel2.writeNoException();
                    return true;
                case 161:
                    parcel.enforceInterface(DESCRIPTOR);
                    grantDefaultPermissionsToEnabledTelephonyDataServices(parcel.createStringArray(), parcel.readInt());
                    parcel2.writeNoException();
                    return true;
                case 162:
                    parcel.enforceInterface(DESCRIPTOR);
                    revokeDefaultPermissionsFromDisabledTelephonyDataServices(parcel.createStringArray(), parcel.readInt());
                    parcel2.writeNoException();
                    return true;
                case 163:
                    parcel.enforceInterface(DESCRIPTOR);
                    grantDefaultPermissionsToActiveLuiApp(parcel.readString(), parcel.readInt());
                    parcel2.writeNoException();
                    return true;
                case 164:
                    parcel.enforceInterface(DESCRIPTOR);
                    revokeDefaultPermissionsFromLuiApps(parcel.createStringArray(), parcel.readInt());
                    parcel2.writeNoException();
                    return true;
                case 165:
                    parcel.enforceInterface(DESCRIPTOR);
                    boolean zIsPermissionRevokedByPolicy = isPermissionRevokedByPolicy(parcel.readString(), parcel.readString(), parcel.readInt());
                    parcel2.writeNoException();
                    parcel2.writeInt(zIsPermissionRevokedByPolicy ? 1 : 0);
                    return true;
                case 166:
                    parcel.enforceInterface(DESCRIPTOR);
                    String permissionControllerPackageName = getPermissionControllerPackageName();
                    parcel2.writeNoException();
                    parcel2.writeString(permissionControllerPackageName);
                    return true;
                case 167:
                    parcel.enforceInterface(DESCRIPTOR);
                    ParceledListSlice instantApps = getInstantApps(parcel.readInt());
                    parcel2.writeNoException();
                    if (instantApps != null) {
                        parcel2.writeInt(1);
                        instantApps.writeToParcel(parcel2, 1);
                    } else {
                        parcel2.writeInt(0);
                    }
                    return true;
                case 168:
                    parcel.enforceInterface(DESCRIPTOR);
                    byte[] instantAppCookie = getInstantAppCookie(parcel.readString(), parcel.readInt());
                    parcel2.writeNoException();
                    parcel2.writeByteArray(instantAppCookie);
                    return true;
                case 169:
                    parcel.enforceInterface(DESCRIPTOR);
                    boolean instantAppCookie2 = setInstantAppCookie(parcel.readString(), parcel.createByteArray(), parcel.readInt());
                    parcel2.writeNoException();
                    parcel2.writeInt(instantAppCookie2 ? 1 : 0);
                    return true;
                case 170:
                    parcel.enforceInterface(DESCRIPTOR);
                    Bitmap instantAppIcon = getInstantAppIcon(parcel.readString(), parcel.readInt());
                    parcel2.writeNoException();
                    if (instantAppIcon != null) {
                        parcel2.writeInt(1);
                        instantAppIcon.writeToParcel(parcel2, 1);
                    } else {
                        parcel2.writeInt(0);
                    }
                    return true;
                case 171:
                    parcel.enforceInterface(DESCRIPTOR);
                    boolean zIsInstantApp = isInstantApp(parcel.readString(), parcel.readInt());
                    parcel2.writeNoException();
                    parcel2.writeInt(zIsInstantApp ? 1 : 0);
                    return true;
                case 172:
                    parcel.enforceInterface(DESCRIPTOR);
                    boolean requiredForSystemUser = setRequiredForSystemUser(parcel.readString(), parcel.readInt() != 0);
                    parcel2.writeNoException();
                    parcel2.writeInt(requiredForSystemUser ? 1 : 0);
                    return true;
                case 173:
                    parcel.enforceInterface(DESCRIPTOR);
                    setUpdateAvailable(parcel.readString(), parcel.readInt() != 0);
                    parcel2.writeNoException();
                    return true;
                case 174:
                    parcel.enforceInterface(DESCRIPTOR);
                    String servicesSystemSharedLibraryPackageName = getServicesSystemSharedLibraryPackageName();
                    parcel2.writeNoException();
                    parcel2.writeString(servicesSystemSharedLibraryPackageName);
                    return true;
                case 175:
                    parcel.enforceInterface(DESCRIPTOR);
                    String sharedSystemSharedLibraryPackageName = getSharedSystemSharedLibraryPackageName();
                    parcel2.writeNoException();
                    parcel2.writeString(sharedSystemSharedLibraryPackageName);
                    return true;
                case 176:
                    parcel.enforceInterface(DESCRIPTOR);
                    ChangedPackages changedPackages = getChangedPackages(parcel.readInt(), parcel.readInt());
                    parcel2.writeNoException();
                    if (changedPackages != null) {
                        parcel2.writeInt(1);
                        changedPackages.writeToParcel(parcel2, 1);
                    } else {
                        parcel2.writeInt(0);
                    }
                    return true;
                case 177:
                    parcel.enforceInterface(DESCRIPTOR);
                    boolean zIsPackageDeviceAdminOnAnyUser = isPackageDeviceAdminOnAnyUser(parcel.readString());
                    parcel2.writeNoException();
                    parcel2.writeInt(zIsPackageDeviceAdminOnAnyUser ? 1 : 0);
                    return true;
                case 178:
                    parcel.enforceInterface(DESCRIPTOR);
                    int installReason = getInstallReason(parcel.readString(), parcel.readInt());
                    parcel2.writeNoException();
                    parcel2.writeInt(installReason);
                    return true;
                case 179:
                    parcel.enforceInterface(DESCRIPTOR);
                    ParceledListSlice sharedLibraries = getSharedLibraries(parcel.readString(), parcel.readInt(), parcel.readInt());
                    parcel2.writeNoException();
                    if (sharedLibraries != null) {
                        parcel2.writeInt(1);
                        sharedLibraries.writeToParcel(parcel2, 1);
                    } else {
                        parcel2.writeInt(0);
                    }
                    return true;
                case 180:
                    parcel.enforceInterface(DESCRIPTOR);
                    boolean zCanRequestPackageInstalls = canRequestPackageInstalls(parcel.readString(), parcel.readInt());
                    parcel2.writeNoException();
                    parcel2.writeInt(zCanRequestPackageInstalls ? 1 : 0);
                    return true;
                case 181:
                    parcel.enforceInterface(DESCRIPTOR);
                    deletePreloadsFileCache();
                    parcel2.writeNoException();
                    return true;
                case 182:
                    parcel.enforceInterface(DESCRIPTOR);
                    ComponentName instantAppResolverComponent = getInstantAppResolverComponent();
                    parcel2.writeNoException();
                    if (instantAppResolverComponent != null) {
                        parcel2.writeInt(1);
                        instantAppResolverComponent.writeToParcel(parcel2, 1);
                    } else {
                        parcel2.writeInt(0);
                    }
                    return true;
                case 183:
                    parcel.enforceInterface(DESCRIPTOR);
                    ComponentName instantAppResolverSettingsComponent = getInstantAppResolverSettingsComponent();
                    parcel2.writeNoException();
                    if (instantAppResolverSettingsComponent != null) {
                        parcel2.writeInt(1);
                        instantAppResolverSettingsComponent.writeToParcel(parcel2, 1);
                    } else {
                        parcel2.writeInt(0);
                    }
                    return true;
                case 184:
                    parcel.enforceInterface(DESCRIPTOR);
                    ComponentName instantAppInstallerComponent = getInstantAppInstallerComponent();
                    parcel2.writeNoException();
                    if (instantAppInstallerComponent != null) {
                        parcel2.writeInt(1);
                        instantAppInstallerComponent.writeToParcel(parcel2, 1);
                    } else {
                        parcel2.writeInt(0);
                    }
                    return true;
                case 185:
                    parcel.enforceInterface(DESCRIPTOR);
                    String instantAppAndroidId = getInstantAppAndroidId(parcel.readString(), parcel.readInt());
                    parcel2.writeNoException();
                    parcel2.writeString(instantAppAndroidId);
                    return true;
                case 186:
                    parcel.enforceInterface(DESCRIPTOR);
                    IArtManager artManager = getArtManager();
                    parcel2.writeNoException();
                    parcel2.writeStrongBinder(artManager != null ? artManager.asBinder() : null);
                    return true;
                case 187:
                    parcel.enforceInterface(DESCRIPTOR);
                    setHarmfulAppWarning(parcel.readString(), parcel.readInt() != 0 ? TextUtils.CHAR_SEQUENCE_CREATOR.createFromParcel(parcel) : null, parcel.readInt());
                    parcel2.writeNoException();
                    return true;
                case 188:
                    parcel.enforceInterface(DESCRIPTOR);
                    CharSequence harmfulAppWarning = getHarmfulAppWarning(parcel.readString(), parcel.readInt());
                    parcel2.writeNoException();
                    if (harmfulAppWarning != null) {
                        parcel2.writeInt(1);
                        TextUtils.writeToParcel(harmfulAppWarning, parcel2, 1);
                    } else {
                        parcel2.writeInt(0);
                    }
                    return true;
                case 189:
                    parcel.enforceInterface(DESCRIPTOR);
                    boolean zHasSigningCertificate = hasSigningCertificate(parcel.readString(), parcel.createByteArray(), parcel.readInt());
                    parcel2.writeNoException();
                    parcel2.writeInt(zHasSigningCertificate ? 1 : 0);
                    return true;
                case 190:
                    parcel.enforceInterface(DESCRIPTOR);
                    boolean zHasUidSigningCertificate = hasUidSigningCertificate(parcel.readInt(), parcel.createByteArray(), parcel.readInt());
                    parcel2.writeNoException();
                    parcel2.writeInt(zHasUidSigningCertificate ? 1 : 0);
                    return true;
                case 191:
                    parcel.enforceInterface(DESCRIPTOR);
                    String systemTextClassifierPackageName = getSystemTextClassifierPackageName();
                    parcel2.writeNoException();
                    parcel2.writeString(systemTextClassifierPackageName);
                    return true;
                case 192:
                    parcel.enforceInterface(DESCRIPTOR);
                    boolean zIsPackageStateProtected = isPackageStateProtected(parcel.readString(), parcel.readInt());
                    parcel2.writeNoException();
                    parcel2.writeInt(zIsPackageStateProtected ? 1 : 0);
                    return true;
                default:
                    return super.onTransact(i, parcel, parcel2, i2);
            }
        }

        private static class Proxy implements IPackageManager {
            private IBinder mRemote;

            Proxy(IBinder iBinder) {
                this.mRemote = iBinder;
            }

            @Override
            public IBinder asBinder() {
                return this.mRemote;
            }

            public String getInterfaceDescriptor() {
                return Stub.DESCRIPTOR;
            }

            @Override
            public void checkPackageStartable(String str, int i) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeString(str);
                    parcelObtain.writeInt(i);
                    this.mRemote.transact(1, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public boolean isPackageAvailable(String str, int i) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeString(str);
                    parcelObtain.writeInt(i);
                    this.mRemote.transact(2, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt() != 0;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public PackageInfo getPackageInfo(String str, int i, int i2) throws RemoteException {
                PackageInfo packageInfoCreateFromParcel;
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeString(str);
                    parcelObtain.writeInt(i);
                    parcelObtain.writeInt(i2);
                    this.mRemote.transact(3, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    if (parcelObtain2.readInt() != 0) {
                        packageInfoCreateFromParcel = PackageInfo.CREATOR.createFromParcel(parcelObtain2);
                    } else {
                        packageInfoCreateFromParcel = null;
                    }
                    return packageInfoCreateFromParcel;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public PackageInfo getPackageInfoVersioned(VersionedPackage versionedPackage, int i, int i2) throws RemoteException {
                PackageInfo packageInfoCreateFromParcel;
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (versionedPackage != null) {
                        parcelObtain.writeInt(1);
                        versionedPackage.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    parcelObtain.writeInt(i);
                    parcelObtain.writeInt(i2);
                    this.mRemote.transact(4, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    if (parcelObtain2.readInt() != 0) {
                        packageInfoCreateFromParcel = PackageInfo.CREATOR.createFromParcel(parcelObtain2);
                    } else {
                        packageInfoCreateFromParcel = null;
                    }
                    return packageInfoCreateFromParcel;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public int getPackageUid(String str, int i, int i2) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeString(str);
                    parcelObtain.writeInt(i);
                    parcelObtain.writeInt(i2);
                    this.mRemote.transact(5, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public int[] getPackageGids(String str, int i, int i2) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeString(str);
                    parcelObtain.writeInt(i);
                    parcelObtain.writeInt(i2);
                    this.mRemote.transact(6, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.createIntArray();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public String[] currentToCanonicalPackageNames(String[] strArr) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeStringArray(strArr);
                    this.mRemote.transact(7, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.createStringArray();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public String[] canonicalToCurrentPackageNames(String[] strArr) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeStringArray(strArr);
                    this.mRemote.transact(8, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.createStringArray();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public PermissionInfo getPermissionInfo(String str, String str2, int i) throws RemoteException {
                PermissionInfo permissionInfoCreateFromParcel;
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeString(str);
                    parcelObtain.writeString(str2);
                    parcelObtain.writeInt(i);
                    this.mRemote.transact(9, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    if (parcelObtain2.readInt() != 0) {
                        permissionInfoCreateFromParcel = PermissionInfo.CREATOR.createFromParcel(parcelObtain2);
                    } else {
                        permissionInfoCreateFromParcel = null;
                    }
                    return permissionInfoCreateFromParcel;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public ParceledListSlice queryPermissionsByGroup(String str, int i) throws RemoteException {
                ParceledListSlice parceledListSliceCreateFromParcel;
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeString(str);
                    parcelObtain.writeInt(i);
                    this.mRemote.transact(10, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    if (parcelObtain2.readInt() != 0) {
                        parceledListSliceCreateFromParcel = ParceledListSlice.CREATOR.createFromParcel(parcelObtain2);
                    } else {
                        parceledListSliceCreateFromParcel = null;
                    }
                    return parceledListSliceCreateFromParcel;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public PermissionGroupInfo getPermissionGroupInfo(String str, int i) throws RemoteException {
                PermissionGroupInfo permissionGroupInfoCreateFromParcel;
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeString(str);
                    parcelObtain.writeInt(i);
                    this.mRemote.transact(11, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    if (parcelObtain2.readInt() != 0) {
                        permissionGroupInfoCreateFromParcel = PermissionGroupInfo.CREATOR.createFromParcel(parcelObtain2);
                    } else {
                        permissionGroupInfoCreateFromParcel = null;
                    }
                    return permissionGroupInfoCreateFromParcel;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public ParceledListSlice getAllPermissionGroups(int i) throws RemoteException {
                ParceledListSlice parceledListSliceCreateFromParcel;
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeInt(i);
                    this.mRemote.transact(12, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    if (parcelObtain2.readInt() != 0) {
                        parceledListSliceCreateFromParcel = ParceledListSlice.CREATOR.createFromParcel(parcelObtain2);
                    } else {
                        parceledListSliceCreateFromParcel = null;
                    }
                    return parceledListSliceCreateFromParcel;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public ApplicationInfo getApplicationInfo(String str, int i, int i2) throws RemoteException {
                ApplicationInfo applicationInfoCreateFromParcel;
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeString(str);
                    parcelObtain.writeInt(i);
                    parcelObtain.writeInt(i2);
                    this.mRemote.transact(13, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    if (parcelObtain2.readInt() != 0) {
                        applicationInfoCreateFromParcel = ApplicationInfo.CREATOR.createFromParcel(parcelObtain2);
                    } else {
                        applicationInfoCreateFromParcel = null;
                    }
                    return applicationInfoCreateFromParcel;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public ActivityInfo getActivityInfo(ComponentName componentName, int i, int i2) throws RemoteException {
                ActivityInfo activityInfoCreateFromParcel;
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (componentName != null) {
                        parcelObtain.writeInt(1);
                        componentName.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    parcelObtain.writeInt(i);
                    parcelObtain.writeInt(i2);
                    this.mRemote.transact(14, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    if (parcelObtain2.readInt() != 0) {
                        activityInfoCreateFromParcel = ActivityInfo.CREATOR.createFromParcel(parcelObtain2);
                    } else {
                        activityInfoCreateFromParcel = null;
                    }
                    return activityInfoCreateFromParcel;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public boolean activitySupportsIntent(ComponentName componentName, Intent intent, String str) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    boolean z = true;
                    if (componentName != null) {
                        parcelObtain.writeInt(1);
                        componentName.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    if (intent != null) {
                        parcelObtain.writeInt(1);
                        intent.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    parcelObtain.writeString(str);
                    this.mRemote.transact(15, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    if (parcelObtain2.readInt() == 0) {
                        z = false;
                    }
                    return z;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public ActivityInfo getReceiverInfo(ComponentName componentName, int i, int i2) throws RemoteException {
                ActivityInfo activityInfoCreateFromParcel;
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (componentName != null) {
                        parcelObtain.writeInt(1);
                        componentName.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    parcelObtain.writeInt(i);
                    parcelObtain.writeInt(i2);
                    this.mRemote.transact(16, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    if (parcelObtain2.readInt() != 0) {
                        activityInfoCreateFromParcel = ActivityInfo.CREATOR.createFromParcel(parcelObtain2);
                    } else {
                        activityInfoCreateFromParcel = null;
                    }
                    return activityInfoCreateFromParcel;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public ServiceInfo getServiceInfo(ComponentName componentName, int i, int i2) throws RemoteException {
                ServiceInfo serviceInfoCreateFromParcel;
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (componentName != null) {
                        parcelObtain.writeInt(1);
                        componentName.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    parcelObtain.writeInt(i);
                    parcelObtain.writeInt(i2);
                    this.mRemote.transact(17, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    if (parcelObtain2.readInt() != 0) {
                        serviceInfoCreateFromParcel = ServiceInfo.CREATOR.createFromParcel(parcelObtain2);
                    } else {
                        serviceInfoCreateFromParcel = null;
                    }
                    return serviceInfoCreateFromParcel;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public ProviderInfo getProviderInfo(ComponentName componentName, int i, int i2) throws RemoteException {
                ProviderInfo providerInfoCreateFromParcel;
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (componentName != null) {
                        parcelObtain.writeInt(1);
                        componentName.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    parcelObtain.writeInt(i);
                    parcelObtain.writeInt(i2);
                    this.mRemote.transact(18, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    if (parcelObtain2.readInt() != 0) {
                        providerInfoCreateFromParcel = ProviderInfo.CREATOR.createFromParcel(parcelObtain2);
                    } else {
                        providerInfoCreateFromParcel = null;
                    }
                    return providerInfoCreateFromParcel;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public int checkPermission(String str, String str2, int i) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeString(str);
                    parcelObtain.writeString(str2);
                    parcelObtain.writeInt(i);
                    this.mRemote.transact(19, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public int checkUidPermission(String str, int i) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeString(str);
                    parcelObtain.writeInt(i);
                    this.mRemote.transact(20, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public boolean addPermission(PermissionInfo permissionInfo) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    boolean z = true;
                    if (permissionInfo != null) {
                        parcelObtain.writeInt(1);
                        permissionInfo.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    this.mRemote.transact(21, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    if (parcelObtain2.readInt() == 0) {
                        z = false;
                    }
                    return z;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void removePermission(String str) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeString(str);
                    this.mRemote.transact(22, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void grantRuntimePermission(String str, String str2, int i) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeString(str);
                    parcelObtain.writeString(str2);
                    parcelObtain.writeInt(i);
                    this.mRemote.transact(23, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void revokeRuntimePermission(String str, String str2, int i) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeString(str);
                    parcelObtain.writeString(str2);
                    parcelObtain.writeInt(i);
                    this.mRemote.transact(24, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void resetRuntimePermissions() throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(25, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public int getPermissionFlags(String str, String str2, int i) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeString(str);
                    parcelObtain.writeString(str2);
                    parcelObtain.writeInt(i);
                    this.mRemote.transact(26, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void updatePermissionFlags(String str, String str2, int i, int i2, int i3) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeString(str);
                    parcelObtain.writeString(str2);
                    parcelObtain.writeInt(i);
                    parcelObtain.writeInt(i2);
                    parcelObtain.writeInt(i3);
                    this.mRemote.transact(27, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void updatePermissionFlagsForAllApps(int i, int i2, int i3) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeInt(i);
                    parcelObtain.writeInt(i2);
                    parcelObtain.writeInt(i3);
                    this.mRemote.transact(28, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public boolean shouldShowRequestPermissionRationale(String str, String str2, int i) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeString(str);
                    parcelObtain.writeString(str2);
                    parcelObtain.writeInt(i);
                    this.mRemote.transact(29, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt() != 0;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public boolean isProtectedBroadcast(String str) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeString(str);
                    this.mRemote.transact(30, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt() != 0;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public int checkSignatures(String str, String str2) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeString(str);
                    parcelObtain.writeString(str2);
                    this.mRemote.transact(31, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public int checkUidSignatures(int i, int i2) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeInt(i);
                    parcelObtain.writeInt(i2);
                    this.mRemote.transact(32, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public List<String> getAllPackages() throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(33, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.createStringArrayList();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public String[] getPackagesForUid(int i) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeInt(i);
                    this.mRemote.transact(34, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.createStringArray();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public String getNameForUid(int i) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeInt(i);
                    this.mRemote.transact(35, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readString();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public String[] getNamesForUids(int[] iArr) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeIntArray(iArr);
                    this.mRemote.transact(36, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.createStringArray();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public int getUidForSharedUser(String str) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeString(str);
                    this.mRemote.transact(37, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public int getFlagsForUid(int i) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeInt(i);
                    this.mRemote.transact(38, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public int getPrivateFlagsForUid(int i) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeInt(i);
                    this.mRemote.transact(39, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public boolean isUidPrivileged(int i) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeInt(i);
                    this.mRemote.transact(40, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt() != 0;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public String[] getAppOpPermissionPackages(String str) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeString(str);
                    this.mRemote.transact(41, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.createStringArray();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public ResolveInfo resolveIntent(Intent intent, String str, int i, int i2) throws RemoteException {
                ResolveInfo resolveInfoCreateFromParcel;
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (intent != null) {
                        parcelObtain.writeInt(1);
                        intent.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    parcelObtain.writeString(str);
                    parcelObtain.writeInt(i);
                    parcelObtain.writeInt(i2);
                    this.mRemote.transact(42, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    if (parcelObtain2.readInt() != 0) {
                        resolveInfoCreateFromParcel = ResolveInfo.CREATOR.createFromParcel(parcelObtain2);
                    } else {
                        resolveInfoCreateFromParcel = null;
                    }
                    return resolveInfoCreateFromParcel;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public ResolveInfo findPersistentPreferredActivity(Intent intent, int i) throws RemoteException {
                ResolveInfo resolveInfoCreateFromParcel;
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (intent != null) {
                        parcelObtain.writeInt(1);
                        intent.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    parcelObtain.writeInt(i);
                    this.mRemote.transact(43, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    if (parcelObtain2.readInt() != 0) {
                        resolveInfoCreateFromParcel = ResolveInfo.CREATOR.createFromParcel(parcelObtain2);
                    } else {
                        resolveInfoCreateFromParcel = null;
                    }
                    return resolveInfoCreateFromParcel;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public boolean canForwardTo(Intent intent, String str, int i, int i2) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    boolean z = true;
                    if (intent != null) {
                        parcelObtain.writeInt(1);
                        intent.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    parcelObtain.writeString(str);
                    parcelObtain.writeInt(i);
                    parcelObtain.writeInt(i2);
                    this.mRemote.transact(44, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    if (parcelObtain2.readInt() == 0) {
                        z = false;
                    }
                    return z;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public ParceledListSlice queryIntentActivities(Intent intent, String str, int i, int i2) throws RemoteException {
                ParceledListSlice parceledListSliceCreateFromParcel;
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (intent != null) {
                        parcelObtain.writeInt(1);
                        intent.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    parcelObtain.writeString(str);
                    parcelObtain.writeInt(i);
                    parcelObtain.writeInt(i2);
                    this.mRemote.transact(45, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    if (parcelObtain2.readInt() != 0) {
                        parceledListSliceCreateFromParcel = ParceledListSlice.CREATOR.createFromParcel(parcelObtain2);
                    } else {
                        parceledListSliceCreateFromParcel = null;
                    }
                    return parceledListSliceCreateFromParcel;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public ParceledListSlice queryIntentActivityOptions(ComponentName componentName, Intent[] intentArr, String[] strArr, Intent intent, String str, int i, int i2) throws RemoteException {
                ParceledListSlice parceledListSliceCreateFromParcel;
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (componentName != null) {
                        parcelObtain.writeInt(1);
                        componentName.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    parcelObtain.writeTypedArray(intentArr, 0);
                    parcelObtain.writeStringArray(strArr);
                    if (intent != null) {
                        parcelObtain.writeInt(1);
                        intent.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    parcelObtain.writeString(str);
                    parcelObtain.writeInt(i);
                    parcelObtain.writeInt(i2);
                    this.mRemote.transact(46, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    if (parcelObtain2.readInt() != 0) {
                        parceledListSliceCreateFromParcel = ParceledListSlice.CREATOR.createFromParcel(parcelObtain2);
                    } else {
                        parceledListSliceCreateFromParcel = null;
                    }
                    return parceledListSliceCreateFromParcel;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public ParceledListSlice queryIntentReceivers(Intent intent, String str, int i, int i2) throws RemoteException {
                ParceledListSlice parceledListSliceCreateFromParcel;
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (intent != null) {
                        parcelObtain.writeInt(1);
                        intent.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    parcelObtain.writeString(str);
                    parcelObtain.writeInt(i);
                    parcelObtain.writeInt(i2);
                    this.mRemote.transact(47, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    if (parcelObtain2.readInt() != 0) {
                        parceledListSliceCreateFromParcel = ParceledListSlice.CREATOR.createFromParcel(parcelObtain2);
                    } else {
                        parceledListSliceCreateFromParcel = null;
                    }
                    return parceledListSliceCreateFromParcel;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public ResolveInfo resolveService(Intent intent, String str, int i, int i2) throws RemoteException {
                ResolveInfo resolveInfoCreateFromParcel;
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (intent != null) {
                        parcelObtain.writeInt(1);
                        intent.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    parcelObtain.writeString(str);
                    parcelObtain.writeInt(i);
                    parcelObtain.writeInt(i2);
                    this.mRemote.transact(48, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    if (parcelObtain2.readInt() != 0) {
                        resolveInfoCreateFromParcel = ResolveInfo.CREATOR.createFromParcel(parcelObtain2);
                    } else {
                        resolveInfoCreateFromParcel = null;
                    }
                    return resolveInfoCreateFromParcel;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public ParceledListSlice queryIntentServices(Intent intent, String str, int i, int i2) throws RemoteException {
                ParceledListSlice parceledListSliceCreateFromParcel;
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (intent != null) {
                        parcelObtain.writeInt(1);
                        intent.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    parcelObtain.writeString(str);
                    parcelObtain.writeInt(i);
                    parcelObtain.writeInt(i2);
                    this.mRemote.transact(49, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    if (parcelObtain2.readInt() != 0) {
                        parceledListSliceCreateFromParcel = ParceledListSlice.CREATOR.createFromParcel(parcelObtain2);
                    } else {
                        parceledListSliceCreateFromParcel = null;
                    }
                    return parceledListSliceCreateFromParcel;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public ParceledListSlice queryIntentContentProviders(Intent intent, String str, int i, int i2) throws RemoteException {
                ParceledListSlice parceledListSliceCreateFromParcel;
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (intent != null) {
                        parcelObtain.writeInt(1);
                        intent.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    parcelObtain.writeString(str);
                    parcelObtain.writeInt(i);
                    parcelObtain.writeInt(i2);
                    this.mRemote.transact(50, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    if (parcelObtain2.readInt() != 0) {
                        parceledListSliceCreateFromParcel = ParceledListSlice.CREATOR.createFromParcel(parcelObtain2);
                    } else {
                        parceledListSliceCreateFromParcel = null;
                    }
                    return parceledListSliceCreateFromParcel;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public ParceledListSlice getInstalledPackages(int i, int i2) throws RemoteException {
                ParceledListSlice parceledListSliceCreateFromParcel;
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeInt(i);
                    parcelObtain.writeInt(i2);
                    this.mRemote.transact(51, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    if (parcelObtain2.readInt() != 0) {
                        parceledListSliceCreateFromParcel = ParceledListSlice.CREATOR.createFromParcel(parcelObtain2);
                    } else {
                        parceledListSliceCreateFromParcel = null;
                    }
                    return parceledListSliceCreateFromParcel;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public ParceledListSlice getPackagesHoldingPermissions(String[] strArr, int i, int i2) throws RemoteException {
                ParceledListSlice parceledListSliceCreateFromParcel;
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeStringArray(strArr);
                    parcelObtain.writeInt(i);
                    parcelObtain.writeInt(i2);
                    this.mRemote.transact(52, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    if (parcelObtain2.readInt() != 0) {
                        parceledListSliceCreateFromParcel = ParceledListSlice.CREATOR.createFromParcel(parcelObtain2);
                    } else {
                        parceledListSliceCreateFromParcel = null;
                    }
                    return parceledListSliceCreateFromParcel;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public ParceledListSlice getInstalledApplications(int i, int i2) throws RemoteException {
                ParceledListSlice parceledListSliceCreateFromParcel;
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeInt(i);
                    parcelObtain.writeInt(i2);
                    this.mRemote.transact(53, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    if (parcelObtain2.readInt() != 0) {
                        parceledListSliceCreateFromParcel = ParceledListSlice.CREATOR.createFromParcel(parcelObtain2);
                    } else {
                        parceledListSliceCreateFromParcel = null;
                    }
                    return parceledListSliceCreateFromParcel;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public ParceledListSlice getPersistentApplications(int i) throws RemoteException {
                ParceledListSlice parceledListSliceCreateFromParcel;
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeInt(i);
                    this.mRemote.transact(54, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    if (parcelObtain2.readInt() != 0) {
                        parceledListSliceCreateFromParcel = ParceledListSlice.CREATOR.createFromParcel(parcelObtain2);
                    } else {
                        parceledListSliceCreateFromParcel = null;
                    }
                    return parceledListSliceCreateFromParcel;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public ProviderInfo resolveContentProvider(String str, int i, int i2) throws RemoteException {
                ProviderInfo providerInfoCreateFromParcel;
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeString(str);
                    parcelObtain.writeInt(i);
                    parcelObtain.writeInt(i2);
                    this.mRemote.transact(55, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    if (parcelObtain2.readInt() != 0) {
                        providerInfoCreateFromParcel = ProviderInfo.CREATOR.createFromParcel(parcelObtain2);
                    } else {
                        providerInfoCreateFromParcel = null;
                    }
                    return providerInfoCreateFromParcel;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void querySyncProviders(List<String> list, List<ProviderInfo> list2) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeStringList(list);
                    parcelObtain.writeTypedList(list2);
                    this.mRemote.transact(56, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    parcelObtain2.readStringList(list);
                    parcelObtain2.readTypedList(list2, ProviderInfo.CREATOR);
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public ParceledListSlice queryContentProviders(String str, int i, int i2, String str2) throws RemoteException {
                ParceledListSlice parceledListSliceCreateFromParcel;
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeString(str);
                    parcelObtain.writeInt(i);
                    parcelObtain.writeInt(i2);
                    parcelObtain.writeString(str2);
                    this.mRemote.transact(57, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    if (parcelObtain2.readInt() != 0) {
                        parceledListSliceCreateFromParcel = ParceledListSlice.CREATOR.createFromParcel(parcelObtain2);
                    } else {
                        parceledListSliceCreateFromParcel = null;
                    }
                    return parceledListSliceCreateFromParcel;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public InstrumentationInfo getInstrumentationInfo(ComponentName componentName, int i) throws RemoteException {
                InstrumentationInfo instrumentationInfoCreateFromParcel;
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (componentName != null) {
                        parcelObtain.writeInt(1);
                        componentName.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    parcelObtain.writeInt(i);
                    this.mRemote.transact(58, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    if (parcelObtain2.readInt() != 0) {
                        instrumentationInfoCreateFromParcel = InstrumentationInfo.CREATOR.createFromParcel(parcelObtain2);
                    } else {
                        instrumentationInfoCreateFromParcel = null;
                    }
                    return instrumentationInfoCreateFromParcel;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public ParceledListSlice queryInstrumentation(String str, int i) throws RemoteException {
                ParceledListSlice parceledListSliceCreateFromParcel;
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeString(str);
                    parcelObtain.writeInt(i);
                    this.mRemote.transact(59, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    if (parcelObtain2.readInt() != 0) {
                        parceledListSliceCreateFromParcel = ParceledListSlice.CREATOR.createFromParcel(parcelObtain2);
                    } else {
                        parceledListSliceCreateFromParcel = null;
                    }
                    return parceledListSliceCreateFromParcel;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void finishPackageInstall(int i, boolean z) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeInt(i);
                    parcelObtain.writeInt(z ? 1 : 0);
                    this.mRemote.transact(60, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void setInstallerPackageName(String str, String str2) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeString(str);
                    parcelObtain.writeString(str2);
                    this.mRemote.transact(61, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void setApplicationCategoryHint(String str, int i, String str2) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeString(str);
                    parcelObtain.writeInt(i);
                    parcelObtain.writeString(str2);
                    this.mRemote.transact(62, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void deletePackageAsUser(String str, int i, IPackageDeleteObserver iPackageDeleteObserver, int i2, int i3) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeString(str);
                    parcelObtain.writeInt(i);
                    parcelObtain.writeStrongBinder(iPackageDeleteObserver != null ? iPackageDeleteObserver.asBinder() : null);
                    parcelObtain.writeInt(i2);
                    parcelObtain.writeInt(i3);
                    this.mRemote.transact(63, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void deletePackageVersioned(VersionedPackage versionedPackage, IPackageDeleteObserver2 iPackageDeleteObserver2, int i, int i2) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (versionedPackage != null) {
                        parcelObtain.writeInt(1);
                        versionedPackage.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    parcelObtain.writeStrongBinder(iPackageDeleteObserver2 != null ? iPackageDeleteObserver2.asBinder() : null);
                    parcelObtain.writeInt(i);
                    parcelObtain.writeInt(i2);
                    this.mRemote.transact(64, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public String getInstallerPackageName(String str) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeString(str);
                    this.mRemote.transact(65, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readString();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void resetApplicationPreferences(int i) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeInt(i);
                    this.mRemote.transact(66, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public ResolveInfo getLastChosenActivity(Intent intent, String str, int i) throws RemoteException {
                ResolveInfo resolveInfoCreateFromParcel;
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (intent != null) {
                        parcelObtain.writeInt(1);
                        intent.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    parcelObtain.writeString(str);
                    parcelObtain.writeInt(i);
                    this.mRemote.transact(67, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    if (parcelObtain2.readInt() != 0) {
                        resolveInfoCreateFromParcel = ResolveInfo.CREATOR.createFromParcel(parcelObtain2);
                    } else {
                        resolveInfoCreateFromParcel = null;
                    }
                    return resolveInfoCreateFromParcel;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void setLastChosenActivity(Intent intent, String str, int i, IntentFilter intentFilter, int i2, ComponentName componentName) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (intent != null) {
                        parcelObtain.writeInt(1);
                        intent.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    parcelObtain.writeString(str);
                    parcelObtain.writeInt(i);
                    if (intentFilter != null) {
                        parcelObtain.writeInt(1);
                        intentFilter.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    parcelObtain.writeInt(i2);
                    if (componentName != null) {
                        parcelObtain.writeInt(1);
                        componentName.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    this.mRemote.transact(68, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void addPreferredActivity(IntentFilter intentFilter, int i, ComponentName[] componentNameArr, ComponentName componentName, int i2) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (intentFilter != null) {
                        parcelObtain.writeInt(1);
                        intentFilter.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    parcelObtain.writeInt(i);
                    parcelObtain.writeTypedArray(componentNameArr, 0);
                    if (componentName != null) {
                        parcelObtain.writeInt(1);
                        componentName.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    parcelObtain.writeInt(i2);
                    this.mRemote.transact(69, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void replacePreferredActivity(IntentFilter intentFilter, int i, ComponentName[] componentNameArr, ComponentName componentName, int i2) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (intentFilter != null) {
                        parcelObtain.writeInt(1);
                        intentFilter.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    parcelObtain.writeInt(i);
                    parcelObtain.writeTypedArray(componentNameArr, 0);
                    if (componentName != null) {
                        parcelObtain.writeInt(1);
                        componentName.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    parcelObtain.writeInt(i2);
                    this.mRemote.transact(70, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void clearPackagePreferredActivities(String str) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeString(str);
                    this.mRemote.transact(71, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public int getPreferredActivities(List<IntentFilter> list, List<ComponentName> list2, String str) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeString(str);
                    this.mRemote.transact(72, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    int i = parcelObtain2.readInt();
                    parcelObtain2.readTypedList(list, IntentFilter.CREATOR);
                    parcelObtain2.readTypedList(list2, ComponentName.CREATOR);
                    return i;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void addPersistentPreferredActivity(IntentFilter intentFilter, ComponentName componentName, int i) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (intentFilter != null) {
                        parcelObtain.writeInt(1);
                        intentFilter.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    if (componentName != null) {
                        parcelObtain.writeInt(1);
                        componentName.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    parcelObtain.writeInt(i);
                    this.mRemote.transact(73, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void clearPackagePersistentPreferredActivities(String str, int i) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeString(str);
                    parcelObtain.writeInt(i);
                    this.mRemote.transact(74, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void addCrossProfileIntentFilter(IntentFilter intentFilter, String str, int i, int i2, int i3) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (intentFilter != null) {
                        parcelObtain.writeInt(1);
                        intentFilter.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    parcelObtain.writeString(str);
                    parcelObtain.writeInt(i);
                    parcelObtain.writeInt(i2);
                    parcelObtain.writeInt(i3);
                    this.mRemote.transact(75, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void clearCrossProfileIntentFilters(int i, String str) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeInt(i);
                    parcelObtain.writeString(str);
                    this.mRemote.transact(76, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public String[] setPackagesSuspendedAsUser(String[] strArr, boolean z, PersistableBundle persistableBundle, PersistableBundle persistableBundle2, String str, String str2, int i) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeStringArray(strArr);
                    parcelObtain.writeInt(z ? 1 : 0);
                    if (persistableBundle != null) {
                        parcelObtain.writeInt(1);
                        persistableBundle.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    if (persistableBundle2 != null) {
                        parcelObtain.writeInt(1);
                        persistableBundle2.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    parcelObtain.writeString(str);
                    parcelObtain.writeString(str2);
                    parcelObtain.writeInt(i);
                    this.mRemote.transact(77, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.createStringArray();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public boolean isPackageSuspendedForUser(String str, int i) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeString(str);
                    parcelObtain.writeInt(i);
                    this.mRemote.transact(78, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt() != 0;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public PersistableBundle getSuspendedPackageAppExtras(String str, int i) throws RemoteException {
                PersistableBundle persistableBundleCreateFromParcel;
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeString(str);
                    parcelObtain.writeInt(i);
                    this.mRemote.transact(79, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    if (parcelObtain2.readInt() != 0) {
                        persistableBundleCreateFromParcel = PersistableBundle.CREATOR.createFromParcel(parcelObtain2);
                    } else {
                        persistableBundleCreateFromParcel = null;
                    }
                    return persistableBundleCreateFromParcel;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public byte[] getPreferredActivityBackup(int i) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeInt(i);
                    this.mRemote.transact(80, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.createByteArray();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void restorePreferredActivities(byte[] bArr, int i) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeByteArray(bArr);
                    parcelObtain.writeInt(i);
                    this.mRemote.transact(81, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public byte[] getDefaultAppsBackup(int i) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeInt(i);
                    this.mRemote.transact(82, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.createByteArray();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void restoreDefaultApps(byte[] bArr, int i) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeByteArray(bArr);
                    parcelObtain.writeInt(i);
                    this.mRemote.transact(83, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public byte[] getIntentFilterVerificationBackup(int i) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeInt(i);
                    this.mRemote.transact(84, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.createByteArray();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void restoreIntentFilterVerification(byte[] bArr, int i) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeByteArray(bArr);
                    parcelObtain.writeInt(i);
                    this.mRemote.transact(85, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public byte[] getPermissionGrantBackup(int i) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeInt(i);
                    this.mRemote.transact(86, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.createByteArray();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void restorePermissionGrants(byte[] bArr, int i) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeByteArray(bArr);
                    parcelObtain.writeInt(i);
                    this.mRemote.transact(87, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public ComponentName getHomeActivities(List<ResolveInfo> list) throws RemoteException {
                ComponentName componentNameCreateFromParcel;
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(88, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    if (parcelObtain2.readInt() != 0) {
                        componentNameCreateFromParcel = ComponentName.CREATOR.createFromParcel(parcelObtain2);
                    } else {
                        componentNameCreateFromParcel = null;
                    }
                    parcelObtain2.readTypedList(list, ResolveInfo.CREATOR);
                    return componentNameCreateFromParcel;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void setHomeActivity(ComponentName componentName, int i) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (componentName != null) {
                        parcelObtain.writeInt(1);
                        componentName.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    parcelObtain.writeInt(i);
                    this.mRemote.transact(89, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void setComponentEnabledSetting(ComponentName componentName, int i, int i2, int i3) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (componentName != null) {
                        parcelObtain.writeInt(1);
                        componentName.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    parcelObtain.writeInt(i);
                    parcelObtain.writeInt(i2);
                    parcelObtain.writeInt(i3);
                    this.mRemote.transact(90, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public int getComponentEnabledSetting(ComponentName componentName, int i) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (componentName != null) {
                        parcelObtain.writeInt(1);
                        componentName.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    parcelObtain.writeInt(i);
                    this.mRemote.transact(91, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void setApplicationEnabledSetting(String str, int i, int i2, int i3, String str2) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeString(str);
                    parcelObtain.writeInt(i);
                    parcelObtain.writeInt(i2);
                    parcelObtain.writeInt(i3);
                    parcelObtain.writeString(str2);
                    this.mRemote.transact(92, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public int getApplicationEnabledSetting(String str, int i) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeString(str);
                    parcelObtain.writeInt(i);
                    this.mRemote.transact(93, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void logAppProcessStartIfNeeded(String str, int i, String str2, String str3, int i2) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeString(str);
                    parcelObtain.writeInt(i);
                    parcelObtain.writeString(str2);
                    parcelObtain.writeString(str3);
                    parcelObtain.writeInt(i2);
                    this.mRemote.transact(94, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void flushPackageRestrictionsAsUser(int i) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeInt(i);
                    this.mRemote.transact(95, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void setPackageStoppedState(String str, boolean z, int i) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeString(str);
                    parcelObtain.writeInt(z ? 1 : 0);
                    parcelObtain.writeInt(i);
                    this.mRemote.transact(96, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void freeStorageAndNotify(String str, long j, int i, IPackageDataObserver iPackageDataObserver) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeString(str);
                    parcelObtain.writeLong(j);
                    parcelObtain.writeInt(i);
                    parcelObtain.writeStrongBinder(iPackageDataObserver != null ? iPackageDataObserver.asBinder() : null);
                    this.mRemote.transact(97, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void freeStorage(String str, long j, int i, IntentSender intentSender) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeString(str);
                    parcelObtain.writeLong(j);
                    parcelObtain.writeInt(i);
                    if (intentSender != null) {
                        parcelObtain.writeInt(1);
                        intentSender.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    this.mRemote.transact(98, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void deleteApplicationCacheFiles(String str, IPackageDataObserver iPackageDataObserver) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeString(str);
                    parcelObtain.writeStrongBinder(iPackageDataObserver != null ? iPackageDataObserver.asBinder() : null);
                    this.mRemote.transact(99, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void deleteApplicationCacheFilesAsUser(String str, int i, IPackageDataObserver iPackageDataObserver) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeString(str);
                    parcelObtain.writeInt(i);
                    parcelObtain.writeStrongBinder(iPackageDataObserver != null ? iPackageDataObserver.asBinder() : null);
                    this.mRemote.transact(100, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void clearApplicationUserData(String str, IPackageDataObserver iPackageDataObserver, int i) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeString(str);
                    parcelObtain.writeStrongBinder(iPackageDataObserver != null ? iPackageDataObserver.asBinder() : null);
                    parcelObtain.writeInt(i);
                    this.mRemote.transact(101, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void clearApplicationProfileData(String str) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeString(str);
                    this.mRemote.transact(102, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void getPackageSizeInfo(String str, int i, IPackageStatsObserver iPackageStatsObserver) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeString(str);
                    parcelObtain.writeInt(i);
                    parcelObtain.writeStrongBinder(iPackageStatsObserver != null ? iPackageStatsObserver.asBinder() : null);
                    this.mRemote.transact(103, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public String[] getSystemSharedLibraryNames() throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(104, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.createStringArray();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public ParceledListSlice getSystemAvailableFeatures() throws RemoteException {
                ParceledListSlice parceledListSliceCreateFromParcel;
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(105, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    if (parcelObtain2.readInt() != 0) {
                        parceledListSliceCreateFromParcel = ParceledListSlice.CREATOR.createFromParcel(parcelObtain2);
                    } else {
                        parceledListSliceCreateFromParcel = null;
                    }
                    return parceledListSliceCreateFromParcel;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public boolean hasSystemFeature(String str, int i) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeString(str);
                    parcelObtain.writeInt(i);
                    this.mRemote.transact(106, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt() != 0;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void enterSafeMode() throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(107, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public boolean isSafeMode() throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(108, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt() != 0;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void systemReady() throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(109, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public boolean hasSystemUidErrors() throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(110, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt() != 0;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void performFstrimIfNeeded() throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(111, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void updatePackagesIfNeeded() throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(112, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void notifyPackageUse(String str, int i) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeString(str);
                    parcelObtain.writeInt(i);
                    this.mRemote.transact(113, parcelObtain, null, 1);
                } finally {
                    parcelObtain.recycle();
                }
            }

            @Override
            public void notifyDexLoad(String str, List<String> list, List<String> list2, String str2) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeString(str);
                    parcelObtain.writeStringList(list);
                    parcelObtain.writeStringList(list2);
                    parcelObtain.writeString(str2);
                    this.mRemote.transact(114, parcelObtain, null, 1);
                } finally {
                    parcelObtain.recycle();
                }
            }

            @Override
            public void registerDexModule(String str, String str2, boolean z, IDexModuleRegisterCallback iDexModuleRegisterCallback) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeString(str);
                    parcelObtain.writeString(str2);
                    parcelObtain.writeInt(z ? 1 : 0);
                    parcelObtain.writeStrongBinder(iDexModuleRegisterCallback != null ? iDexModuleRegisterCallback.asBinder() : null);
                    this.mRemote.transact(115, parcelObtain, null, 1);
                } finally {
                    parcelObtain.recycle();
                }
            }

            @Override
            public boolean performDexOptMode(String str, boolean z, String str2, boolean z2, boolean z3, String str3) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeString(str);
                    parcelObtain.writeInt(z ? 1 : 0);
                    parcelObtain.writeString(str2);
                    parcelObtain.writeInt(z2 ? 1 : 0);
                    parcelObtain.writeInt(z3 ? 1 : 0);
                    parcelObtain.writeString(str3);
                    this.mRemote.transact(116, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt() != 0;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public boolean performDexOptSecondary(String str, String str2, boolean z) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeString(str);
                    parcelObtain.writeString(str2);
                    parcelObtain.writeInt(z ? 1 : 0);
                    this.mRemote.transact(117, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt() != 0;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void dumpProfiles(String str) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeString(str);
                    this.mRemote.transact(118, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void forceDexOpt(String str) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeString(str);
                    this.mRemote.transact(119, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public boolean runBackgroundDexoptJob(List<String> list) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeStringList(list);
                    this.mRemote.transact(120, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt() != 0;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void reconcileSecondaryDexFiles(String str) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeString(str);
                    this.mRemote.transact(121, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public PackageCleanItem nextPackageToClean(PackageCleanItem packageCleanItem) throws RemoteException {
                PackageCleanItem packageCleanItemCreateFromParcel;
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (packageCleanItem != null) {
                        parcelObtain.writeInt(1);
                        packageCleanItem.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    this.mRemote.transact(122, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    if (parcelObtain2.readInt() != 0) {
                        packageCleanItemCreateFromParcel = PackageCleanItem.CREATOR.createFromParcel(parcelObtain2);
                    } else {
                        packageCleanItemCreateFromParcel = null;
                    }
                    return packageCleanItemCreateFromParcel;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public int getMoveStatus(int i) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeInt(i);
                    this.mRemote.transact(123, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void registerMoveCallback(IPackageMoveObserver iPackageMoveObserver) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeStrongBinder(iPackageMoveObserver != null ? iPackageMoveObserver.asBinder() : null);
                    this.mRemote.transact(124, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void unregisterMoveCallback(IPackageMoveObserver iPackageMoveObserver) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeStrongBinder(iPackageMoveObserver != null ? iPackageMoveObserver.asBinder() : null);
                    this.mRemote.transact(125, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public int movePackage(String str, String str2) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeString(str);
                    parcelObtain.writeString(str2);
                    this.mRemote.transact(126, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public int movePrimaryStorage(String str) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeString(str);
                    this.mRemote.transact(127, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public boolean addPermissionAsync(PermissionInfo permissionInfo) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    boolean z = true;
                    if (permissionInfo != null) {
                        parcelObtain.writeInt(1);
                        permissionInfo.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    this.mRemote.transact(128, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    if (parcelObtain2.readInt() == 0) {
                        z = false;
                    }
                    return z;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public boolean setInstallLocation(int i) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeInt(i);
                    this.mRemote.transact(129, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt() != 0;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public int getInstallLocation() throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(130, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public int installExistingPackageAsUser(String str, int i, int i2, int i3) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeString(str);
                    parcelObtain.writeInt(i);
                    parcelObtain.writeInt(i2);
                    parcelObtain.writeInt(i3);
                    this.mRemote.transact(131, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void verifyPendingInstall(int i, int i2) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeInt(i);
                    parcelObtain.writeInt(i2);
                    this.mRemote.transact(132, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void extendVerificationTimeout(int i, int i2, long j) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeInt(i);
                    parcelObtain.writeInt(i2);
                    parcelObtain.writeLong(j);
                    this.mRemote.transact(133, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void verifyIntentFilter(int i, int i2, List<String> list) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeInt(i);
                    parcelObtain.writeInt(i2);
                    parcelObtain.writeStringList(list);
                    this.mRemote.transact(134, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public int getIntentVerificationStatus(String str, int i) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeString(str);
                    parcelObtain.writeInt(i);
                    this.mRemote.transact(135, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public boolean updateIntentVerificationStatus(String str, int i, int i2) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeString(str);
                    parcelObtain.writeInt(i);
                    parcelObtain.writeInt(i2);
                    this.mRemote.transact(136, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt() != 0;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public ParceledListSlice getIntentFilterVerifications(String str) throws RemoteException {
                ParceledListSlice parceledListSliceCreateFromParcel;
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeString(str);
                    this.mRemote.transact(137, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    if (parcelObtain2.readInt() != 0) {
                        parceledListSliceCreateFromParcel = ParceledListSlice.CREATOR.createFromParcel(parcelObtain2);
                    } else {
                        parceledListSliceCreateFromParcel = null;
                    }
                    return parceledListSliceCreateFromParcel;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public ParceledListSlice getAllIntentFilters(String str) throws RemoteException {
                ParceledListSlice parceledListSliceCreateFromParcel;
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeString(str);
                    this.mRemote.transact(138, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    if (parcelObtain2.readInt() != 0) {
                        parceledListSliceCreateFromParcel = ParceledListSlice.CREATOR.createFromParcel(parcelObtain2);
                    } else {
                        parceledListSliceCreateFromParcel = null;
                    }
                    return parceledListSliceCreateFromParcel;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public boolean setDefaultBrowserPackageName(String str, int i) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeString(str);
                    parcelObtain.writeInt(i);
                    this.mRemote.transact(139, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt() != 0;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public String getDefaultBrowserPackageName(int i) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeInt(i);
                    this.mRemote.transact(140, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readString();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public VerifierDeviceIdentity getVerifierDeviceIdentity() throws RemoteException {
                VerifierDeviceIdentity verifierDeviceIdentityCreateFromParcel;
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(141, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    if (parcelObtain2.readInt() != 0) {
                        verifierDeviceIdentityCreateFromParcel = VerifierDeviceIdentity.CREATOR.createFromParcel(parcelObtain2);
                    } else {
                        verifierDeviceIdentityCreateFromParcel = null;
                    }
                    return verifierDeviceIdentityCreateFromParcel;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public boolean isFirstBoot() throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(142, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt() != 0;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public boolean isOnlyCoreApps() throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(143, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt() != 0;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public boolean isUpgrade() throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(144, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt() != 0;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void setPermissionEnforced(String str, boolean z) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeString(str);
                    parcelObtain.writeInt(z ? 1 : 0);
                    this.mRemote.transact(145, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public boolean isPermissionEnforced(String str) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeString(str);
                    this.mRemote.transact(146, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt() != 0;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public boolean isStorageLow() throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(147, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt() != 0;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public boolean setApplicationHiddenSettingAsUser(String str, boolean z, int i) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeString(str);
                    parcelObtain.writeInt(z ? 1 : 0);
                    parcelObtain.writeInt(i);
                    this.mRemote.transact(148, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt() != 0;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public boolean getApplicationHiddenSettingAsUser(String str, int i) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeString(str);
                    parcelObtain.writeInt(i);
                    this.mRemote.transact(149, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt() != 0;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public IPackageInstaller getPackageInstaller() throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(150, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return IPackageInstaller.Stub.asInterface(parcelObtain2.readStrongBinder());
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public boolean setBlockUninstallForUser(String str, boolean z, int i) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeString(str);
                    parcelObtain.writeInt(z ? 1 : 0);
                    parcelObtain.writeInt(i);
                    this.mRemote.transact(151, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt() != 0;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public boolean getBlockUninstallForUser(String str, int i) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeString(str);
                    parcelObtain.writeInt(i);
                    this.mRemote.transact(152, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt() != 0;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public KeySet getKeySetByAlias(String str, String str2) throws RemoteException {
                KeySet keySetCreateFromParcel;
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeString(str);
                    parcelObtain.writeString(str2);
                    this.mRemote.transact(153, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    if (parcelObtain2.readInt() != 0) {
                        keySetCreateFromParcel = KeySet.CREATOR.createFromParcel(parcelObtain2);
                    } else {
                        keySetCreateFromParcel = null;
                    }
                    return keySetCreateFromParcel;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public KeySet getSigningKeySet(String str) throws RemoteException {
                KeySet keySetCreateFromParcel;
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeString(str);
                    this.mRemote.transact(154, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    if (parcelObtain2.readInt() != 0) {
                        keySetCreateFromParcel = KeySet.CREATOR.createFromParcel(parcelObtain2);
                    } else {
                        keySetCreateFromParcel = null;
                    }
                    return keySetCreateFromParcel;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public boolean isPackageSignedByKeySet(String str, KeySet keySet) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeString(str);
                    boolean z = true;
                    if (keySet != null) {
                        parcelObtain.writeInt(1);
                        keySet.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    this.mRemote.transact(155, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    if (parcelObtain2.readInt() == 0) {
                        z = false;
                    }
                    return z;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public boolean isPackageSignedByKeySetExactly(String str, KeySet keySet) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeString(str);
                    boolean z = true;
                    if (keySet != null) {
                        parcelObtain.writeInt(1);
                        keySet.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    this.mRemote.transact(156, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    if (parcelObtain2.readInt() == 0) {
                        z = false;
                    }
                    return z;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void addOnPermissionsChangeListener(IOnPermissionsChangeListener iOnPermissionsChangeListener) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeStrongBinder(iOnPermissionsChangeListener != null ? iOnPermissionsChangeListener.asBinder() : null);
                    this.mRemote.transact(157, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void removeOnPermissionsChangeListener(IOnPermissionsChangeListener iOnPermissionsChangeListener) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeStrongBinder(iOnPermissionsChangeListener != null ? iOnPermissionsChangeListener.asBinder() : null);
                    this.mRemote.transact(158, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void grantDefaultPermissionsToEnabledCarrierApps(String[] strArr, int i) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeStringArray(strArr);
                    parcelObtain.writeInt(i);
                    this.mRemote.transact(159, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void grantDefaultPermissionsToEnabledImsServices(String[] strArr, int i) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeStringArray(strArr);
                    parcelObtain.writeInt(i);
                    this.mRemote.transact(160, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void grantDefaultPermissionsToEnabledTelephonyDataServices(String[] strArr, int i) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeStringArray(strArr);
                    parcelObtain.writeInt(i);
                    this.mRemote.transact(161, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void revokeDefaultPermissionsFromDisabledTelephonyDataServices(String[] strArr, int i) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeStringArray(strArr);
                    parcelObtain.writeInt(i);
                    this.mRemote.transact(162, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void grantDefaultPermissionsToActiveLuiApp(String str, int i) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeString(str);
                    parcelObtain.writeInt(i);
                    this.mRemote.transact(163, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void revokeDefaultPermissionsFromLuiApps(String[] strArr, int i) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeStringArray(strArr);
                    parcelObtain.writeInt(i);
                    this.mRemote.transact(164, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public boolean isPermissionRevokedByPolicy(String str, String str2, int i) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeString(str);
                    parcelObtain.writeString(str2);
                    parcelObtain.writeInt(i);
                    this.mRemote.transact(165, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt() != 0;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public String getPermissionControllerPackageName() throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(166, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readString();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public ParceledListSlice getInstantApps(int i) throws RemoteException {
                ParceledListSlice parceledListSliceCreateFromParcel;
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeInt(i);
                    this.mRemote.transact(167, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    if (parcelObtain2.readInt() != 0) {
                        parceledListSliceCreateFromParcel = ParceledListSlice.CREATOR.createFromParcel(parcelObtain2);
                    } else {
                        parceledListSliceCreateFromParcel = null;
                    }
                    return parceledListSliceCreateFromParcel;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public byte[] getInstantAppCookie(String str, int i) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeString(str);
                    parcelObtain.writeInt(i);
                    this.mRemote.transact(168, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.createByteArray();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public boolean setInstantAppCookie(String str, byte[] bArr, int i) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeString(str);
                    parcelObtain.writeByteArray(bArr);
                    parcelObtain.writeInt(i);
                    this.mRemote.transact(169, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt() != 0;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public Bitmap getInstantAppIcon(String str, int i) throws RemoteException {
                Bitmap bitmapCreateFromParcel;
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeString(str);
                    parcelObtain.writeInt(i);
                    this.mRemote.transact(170, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    if (parcelObtain2.readInt() != 0) {
                        bitmapCreateFromParcel = Bitmap.CREATOR.createFromParcel(parcelObtain2);
                    } else {
                        bitmapCreateFromParcel = null;
                    }
                    return bitmapCreateFromParcel;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public boolean isInstantApp(String str, int i) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeString(str);
                    parcelObtain.writeInt(i);
                    this.mRemote.transact(171, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt() != 0;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public boolean setRequiredForSystemUser(String str, boolean z) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeString(str);
                    parcelObtain.writeInt(z ? 1 : 0);
                    this.mRemote.transact(172, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt() != 0;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void setUpdateAvailable(String str, boolean z) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeString(str);
                    parcelObtain.writeInt(z ? 1 : 0);
                    this.mRemote.transact(173, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public String getServicesSystemSharedLibraryPackageName() throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(174, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readString();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public String getSharedSystemSharedLibraryPackageName() throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(175, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readString();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public ChangedPackages getChangedPackages(int i, int i2) throws RemoteException {
                ChangedPackages changedPackagesCreateFromParcel;
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeInt(i);
                    parcelObtain.writeInt(i2);
                    this.mRemote.transact(176, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    if (parcelObtain2.readInt() != 0) {
                        changedPackagesCreateFromParcel = ChangedPackages.CREATOR.createFromParcel(parcelObtain2);
                    } else {
                        changedPackagesCreateFromParcel = null;
                    }
                    return changedPackagesCreateFromParcel;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public boolean isPackageDeviceAdminOnAnyUser(String str) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeString(str);
                    this.mRemote.transact(177, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt() != 0;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public int getInstallReason(String str, int i) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeString(str);
                    parcelObtain.writeInt(i);
                    this.mRemote.transact(178, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public ParceledListSlice getSharedLibraries(String str, int i, int i2) throws RemoteException {
                ParceledListSlice parceledListSliceCreateFromParcel;
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeString(str);
                    parcelObtain.writeInt(i);
                    parcelObtain.writeInt(i2);
                    this.mRemote.transact(179, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    if (parcelObtain2.readInt() != 0) {
                        parceledListSliceCreateFromParcel = ParceledListSlice.CREATOR.createFromParcel(parcelObtain2);
                    } else {
                        parceledListSliceCreateFromParcel = null;
                    }
                    return parceledListSliceCreateFromParcel;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public boolean canRequestPackageInstalls(String str, int i) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeString(str);
                    parcelObtain.writeInt(i);
                    this.mRemote.transact(180, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt() != 0;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void deletePreloadsFileCache() throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(181, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public ComponentName getInstantAppResolverComponent() throws RemoteException {
                ComponentName componentNameCreateFromParcel;
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(182, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    if (parcelObtain2.readInt() != 0) {
                        componentNameCreateFromParcel = ComponentName.CREATOR.createFromParcel(parcelObtain2);
                    } else {
                        componentNameCreateFromParcel = null;
                    }
                    return componentNameCreateFromParcel;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public ComponentName getInstantAppResolverSettingsComponent() throws RemoteException {
                ComponentName componentNameCreateFromParcel;
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(183, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    if (parcelObtain2.readInt() != 0) {
                        componentNameCreateFromParcel = ComponentName.CREATOR.createFromParcel(parcelObtain2);
                    } else {
                        componentNameCreateFromParcel = null;
                    }
                    return componentNameCreateFromParcel;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public ComponentName getInstantAppInstallerComponent() throws RemoteException {
                ComponentName componentNameCreateFromParcel;
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(184, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    if (parcelObtain2.readInt() != 0) {
                        componentNameCreateFromParcel = ComponentName.CREATOR.createFromParcel(parcelObtain2);
                    } else {
                        componentNameCreateFromParcel = null;
                    }
                    return componentNameCreateFromParcel;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public String getInstantAppAndroidId(String str, int i) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeString(str);
                    parcelObtain.writeInt(i);
                    this.mRemote.transact(185, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readString();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public IArtManager getArtManager() throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(186, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return IArtManager.Stub.asInterface(parcelObtain2.readStrongBinder());
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void setHarmfulAppWarning(String str, CharSequence charSequence, int i) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeString(str);
                    if (charSequence != null) {
                        parcelObtain.writeInt(1);
                        TextUtils.writeToParcel(charSequence, parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    parcelObtain.writeInt(i);
                    this.mRemote.transact(187, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public CharSequence getHarmfulAppWarning(String str, int i) throws RemoteException {
                CharSequence charSequenceCreateFromParcel;
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeString(str);
                    parcelObtain.writeInt(i);
                    this.mRemote.transact(188, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    if (parcelObtain2.readInt() != 0) {
                        charSequenceCreateFromParcel = TextUtils.CHAR_SEQUENCE_CREATOR.createFromParcel(parcelObtain2);
                    } else {
                        charSequenceCreateFromParcel = null;
                    }
                    return charSequenceCreateFromParcel;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public boolean hasSigningCertificate(String str, byte[] bArr, int i) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeString(str);
                    parcelObtain.writeByteArray(bArr);
                    parcelObtain.writeInt(i);
                    this.mRemote.transact(189, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt() != 0;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public boolean hasUidSigningCertificate(int i, byte[] bArr, int i2) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeInt(i);
                    parcelObtain.writeByteArray(bArr);
                    parcelObtain.writeInt(i2);
                    this.mRemote.transact(190, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt() != 0;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public String getSystemTextClassifierPackageName() throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(191, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readString();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public boolean isPackageStateProtected(String str, int i) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeString(str);
                    parcelObtain.writeInt(i);
                    this.mRemote.transact(192, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt() != 0;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }
        }
    }
}
