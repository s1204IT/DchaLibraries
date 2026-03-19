package android.content.pm;

import android.content.ComponentName;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentSender;
import android.content.pm.IOnPermissionsChangeListener;
import android.content.pm.IPackageDataObserver;
import android.content.pm.IPackageDeleteObserver;
import android.content.pm.IPackageDeleteObserver2;
import android.content.pm.IPackageInstallObserver2;
import android.content.pm.IPackageInstaller;
import android.content.pm.IPackageMoveObserver;
import android.content.pm.IPackageStatsObserver;
import android.graphics.Bitmap;
import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;
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

    String[] canonicalToCurrentPackageNames(String[] strArr) throws RemoteException;

    int checkAPKSignatures(String str) throws RemoteException;

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

    void deletePackage(String str, IPackageDeleteObserver2 iPackageDeleteObserver2, int i, int i2) throws RemoteException;

    void deletePackageAsUser(String str, IPackageDeleteObserver iPackageDeleteObserver, int i, int i2) throws RemoteException;

    void dumpProfiles(String str) throws RemoteException;

    void enterSafeMode() throws RemoteException;

    void extendVerificationTimeout(int i, int i2, long j) throws RemoteException;

    void finishPackageInstall(int i, boolean z) throws RemoteException;

    void flushPackageRestrictionsAsUser(int i) throws RemoteException;

    void forceDexOpt(String str) throws RemoteException;

    void freeStorage(String str, long j, IntentSender intentSender) throws RemoteException;

    void freeStorageAndNotify(String str, long j, IPackageDataObserver iPackageDataObserver) throws RemoteException;

    ActivityInfo getActivityInfo(ComponentName componentName, int i, int i2) throws RemoteException;

    ParceledListSlice getAllIntentFilters(String str) throws RemoteException;

    List<String> getAllPackages() throws RemoteException;

    ParceledListSlice getAllPermissionGroups(int i) throws RemoteException;

    String[] getAppOpPermissionPackages(String str) throws RemoteException;

    int getApplicationEnabledSetting(String str, int i) throws RemoteException;

    boolean getApplicationHiddenSettingAsUser(String str, int i) throws RemoteException;

    ApplicationInfo getApplicationInfo(String str, int i, int i2) throws RemoteException;

    boolean getBlockUninstallForUser(String str, int i) throws RemoteException;

    int getComponentEnabledSetting(ComponentName componentName, int i) throws RemoteException;

    byte[] getDefaultAppsBackup(int i) throws RemoteException;

    String getDefaultBrowserPackageName(int i) throws RemoteException;

    byte[] getEphemeralApplicationCookie(String str, int i) throws RemoteException;

    Bitmap getEphemeralApplicationIcon(String str, int i) throws RemoteException;

    ParceledListSlice getEphemeralApplications(int i) throws RemoteException;

    int getFlagsForUid(int i) throws RemoteException;

    ComponentName getHomeActivities(List<ResolveInfo> list) throws RemoteException;

    int getInstallLocation() throws RemoteException;

    ParceledListSlice getInstalledApplications(int i, int i2) throws RemoteException;

    ParceledListSlice getInstalledPackages(int i, int i2) throws RemoteException;

    String getInstallerPackageName(String str) throws RemoteException;

    InstrumentationInfo getInstrumentationInfo(ComponentName componentName, int i) throws RemoteException;

    byte[] getIntentFilterVerificationBackup(int i) throws RemoteException;

    ParceledListSlice getIntentFilterVerifications(String str) throws RemoteException;

    int getIntentVerificationStatus(String str, int i) throws RemoteException;

    KeySet getKeySetByAlias(String str, String str2) throws RemoteException;

    ResolveInfo getLastChosenActivity(Intent intent, String str, int i) throws RemoteException;

    int getMoveStatus(int i) throws RemoteException;

    String getNameForUid(int i) throws RemoteException;

    int[] getPackageGids(String str, int i, int i2) throws RemoteException;

    PackageInfo getPackageInfo(String str, int i, int i2) throws RemoteException;

    IPackageInstaller getPackageInstaller() throws RemoteException;

    void getPackageSizeInfo(String str, int i, IPackageStatsObserver iPackageStatsObserver) throws RemoteException;

    int getPackageUid(String str, int i, int i2) throws RemoteException;

    String[] getPackagesForUid(int i) throws RemoteException;

    ParceledListSlice getPackagesHoldingPermissions(String[] strArr, int i, int i2) throws RemoteException;

    List<String> getPermRecordPerms(String str) throws RemoteException;

    List<String> getPermRecordPkgs() throws RemoteException;

    PermissionRecords getPermRecords(String str, String str2) throws RemoteException;

    String getPermissionControllerPackageName() throws RemoteException;

    int getPermissionFlags(String str, String str2, int i) throws RemoteException;

    byte[] getPermissionGrantBackup(int i) throws RemoteException;

    PermissionGroupInfo getPermissionGroupInfo(String str, int i) throws RemoteException;

    PermissionInfo getPermissionInfo(String str, int i) throws RemoteException;

    ParceledListSlice getPersistentApplications(int i) throws RemoteException;

    int getPreferredActivities(List<IntentFilter> list, List<ComponentName> list2, String str) throws RemoteException;

    byte[] getPreferredActivityBackup(int i) throws RemoteException;

    List<String> getPreviousCodePaths(String str) throws RemoteException;

    int getPrivateFlagsForUid(int i) throws RemoteException;

    ProviderInfo getProviderInfo(ComponentName componentName, int i, int i2) throws RemoteException;

    ActivityInfo getReceiverInfo(ComponentName componentName, int i, int i2) throws RemoteException;

    ServiceInfo getServiceInfo(ComponentName componentName, int i, int i2) throws RemoteException;

    String getServicesSystemSharedLibraryPackageName() throws RemoteException;

    String getSharedSystemSharedLibraryPackageName() throws RemoteException;

    KeySet getSigningKeySet(String str) throws RemoteException;

    ParceledListSlice getSystemAvailableFeatures() throws RemoteException;

    String[] getSystemSharedLibraryNames() throws RemoteException;

    int getUidForSharedUser(String str) throws RemoteException;

    VerifierDeviceIdentity getVerifierDeviceIdentity() throws RemoteException;

    void grantDefaultPermissionsToEnabledCarrierApps(String[] strArr, int i) throws RemoteException;

    void grantRuntimePermission(String str, String str2, int i) throws RemoteException;

    boolean hasSystemFeature(String str, int i) throws RemoteException;

    boolean hasSystemUidErrors() throws RemoteException;

    int installExistingPackageAsUser(String str, int i) throws RemoteException;

    void installPackageAsUser(String str, IPackageInstallObserver2 iPackageInstallObserver2, int i, String str2, int i2) throws RemoteException;

    boolean isEphemeralApplication(String str, int i) throws RemoteException;

    boolean isFirstBoot() throws RemoteException;

    boolean isOnlyCoreApps() throws RemoteException;

    boolean isPackageAvailable(String str, int i) throws RemoteException;

    boolean isPackageDeviceAdminOnAnyUser(String str) throws RemoteException;

    boolean isPackageSignedByKeySet(String str, KeySet keySet) throws RemoteException;

    boolean isPackageSignedByKeySetExactly(String str, KeySet keySet) throws RemoteException;

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

    void notifyPackageUse(String str, int i) throws RemoteException;

    boolean performDexOpt(String str, boolean z, int i, boolean z2) throws RemoteException;

    boolean performDexOptIfNeeded(String str) throws RemoteException;

    boolean performDexOptMode(String str, boolean z, String str2, boolean z2) throws RemoteException;

    void performFstrimIfNeeded() throws RemoteException;

    ParceledListSlice queryContentProviders(String str, int i, int i2) throws RemoteException;

    ParceledListSlice queryInstrumentation(String str, int i) throws RemoteException;

    ParceledListSlice queryIntentActivities(Intent intent, String str, int i, int i2) throws RemoteException;

    ParceledListSlice queryIntentActivityOptions(ComponentName componentName, Intent[] intentArr, String[] strArr, Intent intent, String str, int i, int i2) throws RemoteException;

    ParceledListSlice queryIntentContentProviders(Intent intent, String str, int i, int i2) throws RemoteException;

    ParceledListSlice queryIntentReceivers(Intent intent, String str, int i, int i2) throws RemoteException;

    ParceledListSlice queryIntentServices(Intent intent, String str, int i, int i2) throws RemoteException;

    ParceledListSlice queryPermissionsByGroup(String str, int i) throws RemoteException;

    void querySyncProviders(List<String> list, List<ProviderInfo> list2) throws RemoteException;

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

    void revokeRuntimePermission(String str, String str2, int i) throws RemoteException;

    void setApplicationEnabledSetting(String str, int i, int i2, int i3, String str2) throws RemoteException;

    boolean setApplicationHiddenSettingAsUser(String str, boolean z, int i) throws RemoteException;

    boolean setBlockUninstallForUser(String str, boolean z, int i) throws RemoteException;

    void setComponentEnabledSetting(ComponentName componentName, int i, int i2, int i3) throws RemoteException;

    boolean setDefaultBrowserPackageName(String str, int i) throws RemoteException;

    boolean setEphemeralApplicationCookie(String str, byte[] bArr, int i) throws RemoteException;

    void setHomeActivity(ComponentName componentName, int i) throws RemoteException;

    boolean setInstallLocation(int i) throws RemoteException;

    void setInstallerPackageName(String str, String str2) throws RemoteException;

    void setLastChosenActivity(Intent intent, String str, int i, IntentFilter intentFilter, int i2, ComponentName componentName) throws RemoteException;

    void setPackageStoppedState(String str, boolean z, int i) throws RemoteException;

    String[] setPackagesSuspendedAsUser(String[] strArr, boolean z, int i) throws RemoteException;

    void setPermissionEnforced(String str, boolean z) throws RemoteException;

    boolean setRequiredForSystemUser(String str, boolean z) throws RemoteException;

    boolean shouldShowRequestPermissionRationale(String str, String str2, int i) throws RemoteException;

    void systemReady() throws RemoteException;

    void unregisterMoveCallback(IPackageMoveObserver iPackageMoveObserver) throws RemoteException;

    void updateExternalMediaStatus(boolean z, boolean z2) throws RemoteException;

    boolean updateIntentVerificationStatus(String str, int i, int i2) throws RemoteException;

    void updatePackagesIfNeeded() throws RemoteException;

    void updatePermissionFlags(String str, String str2, int i, int i2, int i3) throws RemoteException;

    void updatePermissionFlagsForAllApps(int i, int i2, int i3) throws RemoteException;

    void verifyIntentFilter(int i, int i2, List<String> list) throws RemoteException;

    void verifyPendingInstall(int i, int i2) throws RemoteException;

    public static abstract class Stub extends Binder implements IPackageManager {
        private static final String DESCRIPTOR = "android.content.pm.IPackageManager";
        static final int TRANSACTION_activitySupportsIntent = 14;
        static final int TRANSACTION_addCrossProfileIntentFilter = 72;
        static final int TRANSACTION_addOnPermissionsChangeListener = 152;
        static final int TRANSACTION_addPermission = 20;
        static final int TRANSACTION_addPermissionAsync = 122;
        static final int TRANSACTION_addPersistentPreferredActivity = 70;
        static final int TRANSACTION_addPreferredActivity = 66;
        static final int TRANSACTION_canForwardTo = 41;
        static final int TRANSACTION_canonicalToCurrentPackageNames = 7;
        static final int TRANSACTION_checkAPKSignatures = 139;
        static final int TRANSACTION_checkPackageStartable = 1;
        static final int TRANSACTION_checkPermission = 18;
        static final int TRANSACTION_checkSignatures = 30;
        static final int TRANSACTION_checkUidPermission = 19;
        static final int TRANSACTION_checkUidSignatures = 31;
        static final int TRANSACTION_clearApplicationProfileData = 98;
        static final int TRANSACTION_clearApplicationUserData = 97;
        static final int TRANSACTION_clearCrossProfileIntentFilters = 73;
        static final int TRANSACTION_clearPackagePersistentPreferredActivities = 71;
        static final int TRANSACTION_clearPackagePreferredActivities = 68;
        static final int TRANSACTION_currentToCanonicalPackageNames = 6;
        static final int TRANSACTION_deleteApplicationCacheFiles = 95;
        static final int TRANSACTION_deleteApplicationCacheFilesAsUser = 96;
        static final int TRANSACTION_deletePackage = 61;
        static final int TRANSACTION_deletePackageAsUser = 60;
        static final int TRANSACTION_dumpProfiles = 113;
        static final int TRANSACTION_enterSafeMode = 103;
        static final int TRANSACTION_extendVerificationTimeout = 127;
        static final int TRANSACTION_finishPackageInstall = 58;
        static final int TRANSACTION_flushPackageRestrictionsAsUser = 91;
        static final int TRANSACTION_forceDexOpt = 114;
        static final int TRANSACTION_freeStorage = 94;
        static final int TRANSACTION_freeStorageAndNotify = 93;
        static final int TRANSACTION_getActivityInfo = 13;
        static final int TRANSACTION_getAllIntentFilters = 132;
        static final int TRANSACTION_getAllPackages = 32;
        static final int TRANSACTION_getAllPermissionGroups = 11;
        static final int TRANSACTION_getAppOpPermissionPackages = 39;
        static final int TRANSACTION_getApplicationEnabledSetting = 89;
        static final int TRANSACTION_getApplicationHiddenSettingAsUser = 144;
        static final int TRANSACTION_getApplicationInfo = 12;
        static final int TRANSACTION_getBlockUninstallForUser = 147;
        static final int TRANSACTION_getComponentEnabledSetting = 87;
        static final int TRANSACTION_getDefaultAppsBackup = 78;
        static final int TRANSACTION_getDefaultBrowserPackageName = 134;
        static final int TRANSACTION_getEphemeralApplicationCookie = 158;
        static final int TRANSACTION_getEphemeralApplicationIcon = 160;
        static final int TRANSACTION_getEphemeralApplications = 157;
        static final int TRANSACTION_getFlagsForUid = 36;
        static final int TRANSACTION_getHomeActivities = 84;
        static final int TRANSACTION_getInstallLocation = 124;
        static final int TRANSACTION_getInstalledApplications = 50;
        static final int TRANSACTION_getInstalledPackages = 48;
        static final int TRANSACTION_getInstallerPackageName = 62;
        static final int TRANSACTION_getInstrumentationInfo = 55;
        static final int TRANSACTION_getIntentFilterVerificationBackup = 80;
        static final int TRANSACTION_getIntentFilterVerifications = 131;
        static final int TRANSACTION_getIntentVerificationStatus = 129;
        static final int TRANSACTION_getKeySetByAlias = 148;
        static final int TRANSACTION_getLastChosenActivity = 64;
        static final int TRANSACTION_getMoveStatus = 117;
        static final int TRANSACTION_getNameForUid = 34;
        static final int TRANSACTION_getPackageGids = 5;
        static final int TRANSACTION_getPackageInfo = 3;
        static final int TRANSACTION_getPackageInstaller = 145;
        static final int TRANSACTION_getPackageSizeInfo = 99;
        static final int TRANSACTION_getPackageUid = 4;
        static final int TRANSACTION_getPackagesForUid = 33;
        static final int TRANSACTION_getPackagesHoldingPermissions = 49;
        static final int TRANSACTION_getPermRecordPerms = 168;
        static final int TRANSACTION_getPermRecordPkgs = 167;
        static final int TRANSACTION_getPermRecords = 169;
        static final int TRANSACTION_getPermissionControllerPackageName = 156;
        static final int TRANSACTION_getPermissionFlags = 25;
        static final int TRANSACTION_getPermissionGrantBackup = 82;
        static final int TRANSACTION_getPermissionGroupInfo = 10;
        static final int TRANSACTION_getPermissionInfo = 8;
        static final int TRANSACTION_getPersistentApplications = 51;
        static final int TRANSACTION_getPreferredActivities = 69;
        static final int TRANSACTION_getPreferredActivityBackup = 76;
        static final int TRANSACTION_getPreviousCodePaths = 166;
        static final int TRANSACTION_getPrivateFlagsForUid = 37;
        static final int TRANSACTION_getProviderInfo = 17;
        static final int TRANSACTION_getReceiverInfo = 15;
        static final int TRANSACTION_getServiceInfo = 16;
        static final int TRANSACTION_getServicesSystemSharedLibraryPackageName = 163;
        static final int TRANSACTION_getSharedSystemSharedLibraryPackageName = 164;
        static final int TRANSACTION_getSigningKeySet = 149;
        static final int TRANSACTION_getSystemAvailableFeatures = 101;
        static final int TRANSACTION_getSystemSharedLibraryNames = 100;
        static final int TRANSACTION_getUidForSharedUser = 35;
        static final int TRANSACTION_getVerifierDeviceIdentity = 135;
        static final int TRANSACTION_grantDefaultPermissionsToEnabledCarrierApps = 154;
        static final int TRANSACTION_grantRuntimePermission = 22;
        static final int TRANSACTION_hasSystemFeature = 102;
        static final int TRANSACTION_hasSystemUidErrors = 106;
        static final int TRANSACTION_installExistingPackageAsUser = 125;
        static final int TRANSACTION_installPackageAsUser = 57;
        static final int TRANSACTION_isEphemeralApplication = 161;
        static final int TRANSACTION_isFirstBoot = 136;
        static final int TRANSACTION_isOnlyCoreApps = 137;
        static final int TRANSACTION_isPackageAvailable = 2;
        static final int TRANSACTION_isPackageDeviceAdminOnAnyUser = 165;
        static final int TRANSACTION_isPackageSignedByKeySet = 150;
        static final int TRANSACTION_isPackageSignedByKeySetExactly = 151;
        static final int TRANSACTION_isPackageSuspendedForUser = 75;
        static final int TRANSACTION_isPermissionEnforced = 141;
        static final int TRANSACTION_isPermissionRevokedByPolicy = 155;
        static final int TRANSACTION_isProtectedBroadcast = 29;
        static final int TRANSACTION_isSafeMode = 104;
        static final int TRANSACTION_isStorageLow = 142;
        static final int TRANSACTION_isUidPrivileged = 38;
        static final int TRANSACTION_isUpgrade = 138;
        static final int TRANSACTION_logAppProcessStartIfNeeded = 90;
        static final int TRANSACTION_movePackage = 120;
        static final int TRANSACTION_movePrimaryStorage = 121;
        static final int TRANSACTION_nextPackageToClean = 116;
        static final int TRANSACTION_notifyPackageUse = 109;
        static final int TRANSACTION_performDexOpt = 111;
        static final int TRANSACTION_performDexOptIfNeeded = 110;
        static final int TRANSACTION_performDexOptMode = 112;
        static final int TRANSACTION_performFstrimIfNeeded = 107;
        static final int TRANSACTION_queryContentProviders = 54;
        static final int TRANSACTION_queryInstrumentation = 56;
        static final int TRANSACTION_queryIntentActivities = 42;
        static final int TRANSACTION_queryIntentActivityOptions = 43;
        static final int TRANSACTION_queryIntentContentProviders = 47;
        static final int TRANSACTION_queryIntentReceivers = 44;
        static final int TRANSACTION_queryIntentServices = 46;
        static final int TRANSACTION_queryPermissionsByGroup = 9;
        static final int TRANSACTION_querySyncProviders = 53;
        static final int TRANSACTION_registerMoveCallback = 118;
        static final int TRANSACTION_removeOnPermissionsChangeListener = 153;
        static final int TRANSACTION_removePermission = 21;
        static final int TRANSACTION_replacePreferredActivity = 67;
        static final int TRANSACTION_resetApplicationPreferences = 63;
        static final int TRANSACTION_resetRuntimePermissions = 24;
        static final int TRANSACTION_resolveContentProvider = 52;
        static final int TRANSACTION_resolveIntent = 40;
        static final int TRANSACTION_resolveService = 45;
        static final int TRANSACTION_restoreDefaultApps = 79;
        static final int TRANSACTION_restoreIntentFilterVerification = 81;
        static final int TRANSACTION_restorePermissionGrants = 83;
        static final int TRANSACTION_restorePreferredActivities = 77;
        static final int TRANSACTION_revokeRuntimePermission = 23;
        static final int TRANSACTION_setApplicationEnabledSetting = 88;
        static final int TRANSACTION_setApplicationHiddenSettingAsUser = 143;
        static final int TRANSACTION_setBlockUninstallForUser = 146;
        static final int TRANSACTION_setComponentEnabledSetting = 86;
        static final int TRANSACTION_setDefaultBrowserPackageName = 133;
        static final int TRANSACTION_setEphemeralApplicationCookie = 159;
        static final int TRANSACTION_setHomeActivity = 85;
        static final int TRANSACTION_setInstallLocation = 123;
        static final int TRANSACTION_setInstallerPackageName = 59;
        static final int TRANSACTION_setLastChosenActivity = 65;
        static final int TRANSACTION_setPackageStoppedState = 92;
        static final int TRANSACTION_setPackagesSuspendedAsUser = 74;
        static final int TRANSACTION_setPermissionEnforced = 140;
        static final int TRANSACTION_setRequiredForSystemUser = 162;
        static final int TRANSACTION_shouldShowRequestPermissionRationale = 28;
        static final int TRANSACTION_systemReady = 105;
        static final int TRANSACTION_unregisterMoveCallback = 119;
        static final int TRANSACTION_updateExternalMediaStatus = 115;
        static final int TRANSACTION_updateIntentVerificationStatus = 130;
        static final int TRANSACTION_updatePackagesIfNeeded = 108;
        static final int TRANSACTION_updatePermissionFlags = 26;
        static final int TRANSACTION_updatePermissionFlagsForAllApps = 27;
        static final int TRANSACTION_verifyIntentFilter = 128;
        static final int TRANSACTION_verifyPendingInstall = 126;

        public Stub() {
            attachInterface(this, DESCRIPTOR);
        }

        public static IPackageManager asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
            if (iin != null && (iin instanceof IPackageManager)) {
                return (IPackageManager) iin;
            }
            return new Proxy(obj);
        }

        @Override
        public IBinder asBinder() {
            return this;
        }

        @Override
        public boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
            KeySet keySetCreateFromParcel;
            KeySet keySetCreateFromParcel2;
            PermissionInfo permissionInfoCreateFromParcel;
            PackageCleanItem packageCleanItemCreateFromParcel;
            IntentSender intentSenderCreateFromParcel;
            ComponentName componentNameCreateFromParcel;
            ComponentName componentNameCreateFromParcel2;
            ComponentName componentNameCreateFromParcel3;
            IntentFilter intentFilterCreateFromParcel;
            IntentFilter intentFilterCreateFromParcel2;
            ComponentName componentNameCreateFromParcel4;
            IntentFilter intentFilterCreateFromParcel3;
            ComponentName componentNameCreateFromParcel5;
            IntentFilter intentFilterCreateFromParcel4;
            ComponentName componentNameCreateFromParcel6;
            Intent intentCreateFromParcel;
            IntentFilter intentFilterCreateFromParcel5;
            ComponentName componentNameCreateFromParcel7;
            Intent intentCreateFromParcel2;
            ComponentName componentNameCreateFromParcel8;
            Intent intentCreateFromParcel3;
            Intent intentCreateFromParcel4;
            Intent intentCreateFromParcel5;
            Intent intentCreateFromParcel6;
            ComponentName componentNameCreateFromParcel9;
            Intent intentCreateFromParcel7;
            Intent intentCreateFromParcel8;
            Intent intentCreateFromParcel9;
            Intent intentCreateFromParcel10;
            PermissionInfo permissionInfoCreateFromParcel2;
            ComponentName componentNameCreateFromParcel10;
            ComponentName componentNameCreateFromParcel11;
            ComponentName componentNameCreateFromParcel12;
            ComponentName componentNameCreateFromParcel13;
            Intent intentCreateFromParcel11;
            ComponentName componentNameCreateFromParcel14;
            switch (code) {
                case 1:
                    data.enforceInterface(DESCRIPTOR);
                    String _arg0 = data.readString();
                    int _arg1 = data.readInt();
                    checkPackageStartable(_arg0, _arg1);
                    reply.writeNoException();
                    return true;
                case 2:
                    data.enforceInterface(DESCRIPTOR);
                    String _arg02 = data.readString();
                    int _arg12 = data.readInt();
                    boolean _result = isPackageAvailable(_arg02, _arg12);
                    reply.writeNoException();
                    reply.writeInt(_result ? 1 : 0);
                    return true;
                case 3:
                    data.enforceInterface(DESCRIPTOR);
                    String _arg03 = data.readString();
                    int _arg13 = data.readInt();
                    int _arg2 = data.readInt();
                    PackageInfo _result2 = getPackageInfo(_arg03, _arg13, _arg2);
                    reply.writeNoException();
                    if (_result2 != null) {
                        reply.writeInt(1);
                        _result2.writeToParcel(reply, 1);
                        return true;
                    }
                    reply.writeInt(0);
                    return true;
                case 4:
                    data.enforceInterface(DESCRIPTOR);
                    String _arg04 = data.readString();
                    int _arg14 = data.readInt();
                    int _arg22 = data.readInt();
                    int _result3 = getPackageUid(_arg04, _arg14, _arg22);
                    reply.writeNoException();
                    reply.writeInt(_result3);
                    return true;
                case 5:
                    data.enforceInterface(DESCRIPTOR);
                    String _arg05 = data.readString();
                    int _arg15 = data.readInt();
                    int _arg23 = data.readInt();
                    int[] _result4 = getPackageGids(_arg05, _arg15, _arg23);
                    reply.writeNoException();
                    reply.writeIntArray(_result4);
                    return true;
                case 6:
                    data.enforceInterface(DESCRIPTOR);
                    String[] _arg06 = data.createStringArray();
                    String[] _result5 = currentToCanonicalPackageNames(_arg06);
                    reply.writeNoException();
                    reply.writeStringArray(_result5);
                    return true;
                case 7:
                    data.enforceInterface(DESCRIPTOR);
                    String[] _arg07 = data.createStringArray();
                    String[] _result6 = canonicalToCurrentPackageNames(_arg07);
                    reply.writeNoException();
                    reply.writeStringArray(_result6);
                    return true;
                case 8:
                    data.enforceInterface(DESCRIPTOR);
                    String _arg08 = data.readString();
                    int _arg16 = data.readInt();
                    PermissionInfo _result7 = getPermissionInfo(_arg08, _arg16);
                    reply.writeNoException();
                    if (_result7 != null) {
                        reply.writeInt(1);
                        _result7.writeToParcel(reply, 1);
                        return true;
                    }
                    reply.writeInt(0);
                    return true;
                case 9:
                    data.enforceInterface(DESCRIPTOR);
                    String _arg09 = data.readString();
                    int _arg17 = data.readInt();
                    ParceledListSlice _result8 = queryPermissionsByGroup(_arg09, _arg17);
                    reply.writeNoException();
                    if (_result8 != null) {
                        reply.writeInt(1);
                        _result8.writeToParcel(reply, 1);
                        return true;
                    }
                    reply.writeInt(0);
                    return true;
                case 10:
                    data.enforceInterface(DESCRIPTOR);
                    String _arg010 = data.readString();
                    int _arg18 = data.readInt();
                    PermissionGroupInfo _result9 = getPermissionGroupInfo(_arg010, _arg18);
                    reply.writeNoException();
                    if (_result9 != null) {
                        reply.writeInt(1);
                        _result9.writeToParcel(reply, 1);
                        return true;
                    }
                    reply.writeInt(0);
                    return true;
                case 11:
                    data.enforceInterface(DESCRIPTOR);
                    int _arg011 = data.readInt();
                    ParceledListSlice _result10 = getAllPermissionGroups(_arg011);
                    reply.writeNoException();
                    if (_result10 != null) {
                        reply.writeInt(1);
                        _result10.writeToParcel(reply, 1);
                        return true;
                    }
                    reply.writeInt(0);
                    return true;
                case 12:
                    data.enforceInterface(DESCRIPTOR);
                    String _arg012 = data.readString();
                    int _arg19 = data.readInt();
                    int _arg24 = data.readInt();
                    ApplicationInfo _result11 = getApplicationInfo(_arg012, _arg19, _arg24);
                    reply.writeNoException();
                    if (_result11 != null) {
                        reply.writeInt(1);
                        _result11.writeToParcel(reply, 1);
                        return true;
                    }
                    reply.writeInt(0);
                    return true;
                case 13:
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        componentNameCreateFromParcel14 = ComponentName.CREATOR.createFromParcel(data);
                    } else {
                        componentNameCreateFromParcel14 = null;
                    }
                    int _arg110 = data.readInt();
                    int _arg25 = data.readInt();
                    ActivityInfo _result12 = getActivityInfo(componentNameCreateFromParcel14, _arg110, _arg25);
                    reply.writeNoException();
                    if (_result12 != null) {
                        reply.writeInt(1);
                        _result12.writeToParcel(reply, 1);
                        return true;
                    }
                    reply.writeInt(0);
                    return true;
                case 14:
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        componentNameCreateFromParcel13 = ComponentName.CREATOR.createFromParcel(data);
                    } else {
                        componentNameCreateFromParcel13 = null;
                    }
                    if (data.readInt() != 0) {
                        intentCreateFromParcel11 = Intent.CREATOR.createFromParcel(data);
                    } else {
                        intentCreateFromParcel11 = null;
                    }
                    String _arg26 = data.readString();
                    boolean _result13 = activitySupportsIntent(componentNameCreateFromParcel13, intentCreateFromParcel11, _arg26);
                    reply.writeNoException();
                    reply.writeInt(_result13 ? 1 : 0);
                    return true;
                case 15:
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        componentNameCreateFromParcel12 = ComponentName.CREATOR.createFromParcel(data);
                    } else {
                        componentNameCreateFromParcel12 = null;
                    }
                    int _arg111 = data.readInt();
                    int _arg27 = data.readInt();
                    ActivityInfo _result14 = getReceiverInfo(componentNameCreateFromParcel12, _arg111, _arg27);
                    reply.writeNoException();
                    if (_result14 != null) {
                        reply.writeInt(1);
                        _result14.writeToParcel(reply, 1);
                        return true;
                    }
                    reply.writeInt(0);
                    return true;
                case 16:
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        componentNameCreateFromParcel11 = ComponentName.CREATOR.createFromParcel(data);
                    } else {
                        componentNameCreateFromParcel11 = null;
                    }
                    int _arg112 = data.readInt();
                    int _arg28 = data.readInt();
                    ServiceInfo _result15 = getServiceInfo(componentNameCreateFromParcel11, _arg112, _arg28);
                    reply.writeNoException();
                    if (_result15 != null) {
                        reply.writeInt(1);
                        _result15.writeToParcel(reply, 1);
                        return true;
                    }
                    reply.writeInt(0);
                    return true;
                case 17:
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        componentNameCreateFromParcel10 = ComponentName.CREATOR.createFromParcel(data);
                    } else {
                        componentNameCreateFromParcel10 = null;
                    }
                    int _arg113 = data.readInt();
                    int _arg29 = data.readInt();
                    ProviderInfo _result16 = getProviderInfo(componentNameCreateFromParcel10, _arg113, _arg29);
                    reply.writeNoException();
                    if (_result16 != null) {
                        reply.writeInt(1);
                        _result16.writeToParcel(reply, 1);
                        return true;
                    }
                    reply.writeInt(0);
                    return true;
                case 18:
                    data.enforceInterface(DESCRIPTOR);
                    String _arg013 = data.readString();
                    String _arg114 = data.readString();
                    int _arg210 = data.readInt();
                    int _result17 = checkPermission(_arg013, _arg114, _arg210);
                    reply.writeNoException();
                    reply.writeInt(_result17);
                    return true;
                case 19:
                    data.enforceInterface(DESCRIPTOR);
                    String _arg014 = data.readString();
                    int _arg115 = data.readInt();
                    int _result18 = checkUidPermission(_arg014, _arg115);
                    reply.writeNoException();
                    reply.writeInt(_result18);
                    return true;
                case 20:
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        permissionInfoCreateFromParcel2 = PermissionInfo.CREATOR.createFromParcel(data);
                    } else {
                        permissionInfoCreateFromParcel2 = null;
                    }
                    boolean _result19 = addPermission(permissionInfoCreateFromParcel2);
                    reply.writeNoException();
                    reply.writeInt(_result19 ? 1 : 0);
                    return true;
                case 21:
                    data.enforceInterface(DESCRIPTOR);
                    String _arg015 = data.readString();
                    removePermission(_arg015);
                    reply.writeNoException();
                    return true;
                case 22:
                    data.enforceInterface(DESCRIPTOR);
                    String _arg016 = data.readString();
                    String _arg116 = data.readString();
                    int _arg211 = data.readInt();
                    grantRuntimePermission(_arg016, _arg116, _arg211);
                    reply.writeNoException();
                    return true;
                case 23:
                    data.enforceInterface(DESCRIPTOR);
                    String _arg017 = data.readString();
                    String _arg117 = data.readString();
                    int _arg212 = data.readInt();
                    revokeRuntimePermission(_arg017, _arg117, _arg212);
                    reply.writeNoException();
                    return true;
                case 24:
                    data.enforceInterface(DESCRIPTOR);
                    resetRuntimePermissions();
                    reply.writeNoException();
                    return true;
                case 25:
                    data.enforceInterface(DESCRIPTOR);
                    String _arg018 = data.readString();
                    String _arg118 = data.readString();
                    int _arg213 = data.readInt();
                    int _result20 = getPermissionFlags(_arg018, _arg118, _arg213);
                    reply.writeNoException();
                    reply.writeInt(_result20);
                    return true;
                case 26:
                    data.enforceInterface(DESCRIPTOR);
                    String _arg019 = data.readString();
                    String _arg119 = data.readString();
                    int _arg214 = data.readInt();
                    int _arg3 = data.readInt();
                    int _arg4 = data.readInt();
                    updatePermissionFlags(_arg019, _arg119, _arg214, _arg3, _arg4);
                    reply.writeNoException();
                    return true;
                case 27:
                    data.enforceInterface(DESCRIPTOR);
                    int _arg020 = data.readInt();
                    int _arg120 = data.readInt();
                    int _arg215 = data.readInt();
                    updatePermissionFlagsForAllApps(_arg020, _arg120, _arg215);
                    reply.writeNoException();
                    return true;
                case 28:
                    data.enforceInterface(DESCRIPTOR);
                    String _arg021 = data.readString();
                    String _arg121 = data.readString();
                    int _arg216 = data.readInt();
                    boolean _result21 = shouldShowRequestPermissionRationale(_arg021, _arg121, _arg216);
                    reply.writeNoException();
                    reply.writeInt(_result21 ? 1 : 0);
                    return true;
                case 29:
                    data.enforceInterface(DESCRIPTOR);
                    String _arg022 = data.readString();
                    boolean _result22 = isProtectedBroadcast(_arg022);
                    reply.writeNoException();
                    reply.writeInt(_result22 ? 1 : 0);
                    return true;
                case 30:
                    data.enforceInterface(DESCRIPTOR);
                    String _arg023 = data.readString();
                    String _arg122 = data.readString();
                    int _result23 = checkSignatures(_arg023, _arg122);
                    reply.writeNoException();
                    reply.writeInt(_result23);
                    return true;
                case 31:
                    data.enforceInterface(DESCRIPTOR);
                    int _arg024 = data.readInt();
                    int _arg123 = data.readInt();
                    int _result24 = checkUidSignatures(_arg024, _arg123);
                    reply.writeNoException();
                    reply.writeInt(_result24);
                    return true;
                case 32:
                    data.enforceInterface(DESCRIPTOR);
                    List<String> _result25 = getAllPackages();
                    reply.writeNoException();
                    reply.writeStringList(_result25);
                    return true;
                case 33:
                    data.enforceInterface(DESCRIPTOR);
                    int _arg025 = data.readInt();
                    String[] _result26 = getPackagesForUid(_arg025);
                    reply.writeNoException();
                    reply.writeStringArray(_result26);
                    return true;
                case 34:
                    data.enforceInterface(DESCRIPTOR);
                    int _arg026 = data.readInt();
                    String _result27 = getNameForUid(_arg026);
                    reply.writeNoException();
                    reply.writeString(_result27);
                    return true;
                case 35:
                    data.enforceInterface(DESCRIPTOR);
                    String _arg027 = data.readString();
                    int _result28 = getUidForSharedUser(_arg027);
                    reply.writeNoException();
                    reply.writeInt(_result28);
                    return true;
                case 36:
                    data.enforceInterface(DESCRIPTOR);
                    int _arg028 = data.readInt();
                    int _result29 = getFlagsForUid(_arg028);
                    reply.writeNoException();
                    reply.writeInt(_result29);
                    return true;
                case 37:
                    data.enforceInterface(DESCRIPTOR);
                    int _arg029 = data.readInt();
                    int _result30 = getPrivateFlagsForUid(_arg029);
                    reply.writeNoException();
                    reply.writeInt(_result30);
                    return true;
                case 38:
                    data.enforceInterface(DESCRIPTOR);
                    int _arg030 = data.readInt();
                    boolean _result31 = isUidPrivileged(_arg030);
                    reply.writeNoException();
                    reply.writeInt(_result31 ? 1 : 0);
                    return true;
                case 39:
                    data.enforceInterface(DESCRIPTOR);
                    String _arg031 = data.readString();
                    String[] _result32 = getAppOpPermissionPackages(_arg031);
                    reply.writeNoException();
                    reply.writeStringArray(_result32);
                    return true;
                case 40:
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        intentCreateFromParcel10 = Intent.CREATOR.createFromParcel(data);
                    } else {
                        intentCreateFromParcel10 = null;
                    }
                    String _arg124 = data.readString();
                    int _arg217 = data.readInt();
                    int _arg32 = data.readInt();
                    ResolveInfo _result33 = resolveIntent(intentCreateFromParcel10, _arg124, _arg217, _arg32);
                    reply.writeNoException();
                    if (_result33 != null) {
                        reply.writeInt(1);
                        _result33.writeToParcel(reply, 1);
                        return true;
                    }
                    reply.writeInt(0);
                    return true;
                case 41:
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        intentCreateFromParcel9 = Intent.CREATOR.createFromParcel(data);
                    } else {
                        intentCreateFromParcel9 = null;
                    }
                    String _arg125 = data.readString();
                    int _arg218 = data.readInt();
                    int _arg33 = data.readInt();
                    boolean _result34 = canForwardTo(intentCreateFromParcel9, _arg125, _arg218, _arg33);
                    reply.writeNoException();
                    reply.writeInt(_result34 ? 1 : 0);
                    return true;
                case 42:
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        intentCreateFromParcel8 = Intent.CREATOR.createFromParcel(data);
                    } else {
                        intentCreateFromParcel8 = null;
                    }
                    String _arg126 = data.readString();
                    int _arg219 = data.readInt();
                    int _arg34 = data.readInt();
                    ParceledListSlice _result35 = queryIntentActivities(intentCreateFromParcel8, _arg126, _arg219, _arg34);
                    reply.writeNoException();
                    if (_result35 != null) {
                        reply.writeInt(1);
                        _result35.writeToParcel(reply, 1);
                        return true;
                    }
                    reply.writeInt(0);
                    return true;
                case 43:
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        componentNameCreateFromParcel9 = ComponentName.CREATOR.createFromParcel(data);
                    } else {
                        componentNameCreateFromParcel9 = null;
                    }
                    Intent[] _arg127 = (Intent[]) data.createTypedArray(Intent.CREATOR);
                    String[] _arg220 = data.createStringArray();
                    if (data.readInt() != 0) {
                        intentCreateFromParcel7 = Intent.CREATOR.createFromParcel(data);
                    } else {
                        intentCreateFromParcel7 = null;
                    }
                    String _arg42 = data.readString();
                    int _arg5 = data.readInt();
                    int _arg6 = data.readInt();
                    ParceledListSlice _result36 = queryIntentActivityOptions(componentNameCreateFromParcel9, _arg127, _arg220, intentCreateFromParcel7, _arg42, _arg5, _arg6);
                    reply.writeNoException();
                    if (_result36 != null) {
                        reply.writeInt(1);
                        _result36.writeToParcel(reply, 1);
                        return true;
                    }
                    reply.writeInt(0);
                    return true;
                case 44:
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        intentCreateFromParcel6 = Intent.CREATOR.createFromParcel(data);
                    } else {
                        intentCreateFromParcel6 = null;
                    }
                    String _arg128 = data.readString();
                    int _arg221 = data.readInt();
                    int _arg35 = data.readInt();
                    ParceledListSlice _result37 = queryIntentReceivers(intentCreateFromParcel6, _arg128, _arg221, _arg35);
                    reply.writeNoException();
                    if (_result37 != null) {
                        reply.writeInt(1);
                        _result37.writeToParcel(reply, 1);
                        return true;
                    }
                    reply.writeInt(0);
                    return true;
                case 45:
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        intentCreateFromParcel5 = Intent.CREATOR.createFromParcel(data);
                    } else {
                        intentCreateFromParcel5 = null;
                    }
                    String _arg129 = data.readString();
                    int _arg222 = data.readInt();
                    int _arg36 = data.readInt();
                    ResolveInfo _result38 = resolveService(intentCreateFromParcel5, _arg129, _arg222, _arg36);
                    reply.writeNoException();
                    if (_result38 != null) {
                        reply.writeInt(1);
                        _result38.writeToParcel(reply, 1);
                        return true;
                    }
                    reply.writeInt(0);
                    return true;
                case 46:
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        intentCreateFromParcel4 = Intent.CREATOR.createFromParcel(data);
                    } else {
                        intentCreateFromParcel4 = null;
                    }
                    String _arg130 = data.readString();
                    int _arg223 = data.readInt();
                    int _arg37 = data.readInt();
                    ParceledListSlice _result39 = queryIntentServices(intentCreateFromParcel4, _arg130, _arg223, _arg37);
                    reply.writeNoException();
                    if (_result39 != null) {
                        reply.writeInt(1);
                        _result39.writeToParcel(reply, 1);
                        return true;
                    }
                    reply.writeInt(0);
                    return true;
                case 47:
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        intentCreateFromParcel3 = Intent.CREATOR.createFromParcel(data);
                    } else {
                        intentCreateFromParcel3 = null;
                    }
                    String _arg131 = data.readString();
                    int _arg224 = data.readInt();
                    int _arg38 = data.readInt();
                    ParceledListSlice _result40 = queryIntentContentProviders(intentCreateFromParcel3, _arg131, _arg224, _arg38);
                    reply.writeNoException();
                    if (_result40 != null) {
                        reply.writeInt(1);
                        _result40.writeToParcel(reply, 1);
                        return true;
                    }
                    reply.writeInt(0);
                    return true;
                case 48:
                    data.enforceInterface(DESCRIPTOR);
                    int _arg032 = data.readInt();
                    int _arg132 = data.readInt();
                    ParceledListSlice _result41 = getInstalledPackages(_arg032, _arg132);
                    reply.writeNoException();
                    if (_result41 != null) {
                        reply.writeInt(1);
                        _result41.writeToParcel(reply, 1);
                        return true;
                    }
                    reply.writeInt(0);
                    return true;
                case 49:
                    data.enforceInterface(DESCRIPTOR);
                    String[] _arg033 = data.createStringArray();
                    int _arg133 = data.readInt();
                    int _arg225 = data.readInt();
                    ParceledListSlice _result42 = getPackagesHoldingPermissions(_arg033, _arg133, _arg225);
                    reply.writeNoException();
                    if (_result42 != null) {
                        reply.writeInt(1);
                        _result42.writeToParcel(reply, 1);
                        return true;
                    }
                    reply.writeInt(0);
                    return true;
                case 50:
                    data.enforceInterface(DESCRIPTOR);
                    int _arg034 = data.readInt();
                    int _arg134 = data.readInt();
                    ParceledListSlice _result43 = getInstalledApplications(_arg034, _arg134);
                    reply.writeNoException();
                    if (_result43 != null) {
                        reply.writeInt(1);
                        _result43.writeToParcel(reply, 1);
                        return true;
                    }
                    reply.writeInt(0);
                    return true;
                case 51:
                    data.enforceInterface(DESCRIPTOR);
                    int _arg035 = data.readInt();
                    ParceledListSlice _result44 = getPersistentApplications(_arg035);
                    reply.writeNoException();
                    if (_result44 != null) {
                        reply.writeInt(1);
                        _result44.writeToParcel(reply, 1);
                        return true;
                    }
                    reply.writeInt(0);
                    return true;
                case 52:
                    data.enforceInterface(DESCRIPTOR);
                    String _arg036 = data.readString();
                    int _arg135 = data.readInt();
                    int _arg226 = data.readInt();
                    ProviderInfo _result45 = resolveContentProvider(_arg036, _arg135, _arg226);
                    reply.writeNoException();
                    if (_result45 != null) {
                        reply.writeInt(1);
                        _result45.writeToParcel(reply, 1);
                        return true;
                    }
                    reply.writeInt(0);
                    return true;
                case 53:
                    data.enforceInterface(DESCRIPTOR);
                    List<String> _arg037 = data.createStringArrayList();
                    ArrayList arrayListCreateTypedArrayList = data.createTypedArrayList(ProviderInfo.CREATOR);
                    querySyncProviders(_arg037, arrayListCreateTypedArrayList);
                    reply.writeNoException();
                    reply.writeStringList(_arg037);
                    reply.writeTypedList(arrayListCreateTypedArrayList);
                    return true;
                case 54:
                    data.enforceInterface(DESCRIPTOR);
                    String _arg038 = data.readString();
                    int _arg136 = data.readInt();
                    int _arg227 = data.readInt();
                    ParceledListSlice _result46 = queryContentProviders(_arg038, _arg136, _arg227);
                    reply.writeNoException();
                    if (_result46 != null) {
                        reply.writeInt(1);
                        _result46.writeToParcel(reply, 1);
                        return true;
                    }
                    reply.writeInt(0);
                    return true;
                case 55:
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        componentNameCreateFromParcel8 = ComponentName.CREATOR.createFromParcel(data);
                    } else {
                        componentNameCreateFromParcel8 = null;
                    }
                    int _arg137 = data.readInt();
                    InstrumentationInfo _result47 = getInstrumentationInfo(componentNameCreateFromParcel8, _arg137);
                    reply.writeNoException();
                    if (_result47 != null) {
                        reply.writeInt(1);
                        _result47.writeToParcel(reply, 1);
                        return true;
                    }
                    reply.writeInt(0);
                    return true;
                case 56:
                    data.enforceInterface(DESCRIPTOR);
                    String _arg039 = data.readString();
                    int _arg138 = data.readInt();
                    ParceledListSlice _result48 = queryInstrumentation(_arg039, _arg138);
                    reply.writeNoException();
                    if (_result48 != null) {
                        reply.writeInt(1);
                        _result48.writeToParcel(reply, 1);
                        return true;
                    }
                    reply.writeInt(0);
                    return true;
                case 57:
                    data.enforceInterface(DESCRIPTOR);
                    String _arg040 = data.readString();
                    IPackageInstallObserver2 _arg139 = IPackageInstallObserver2.Stub.asInterface(data.readStrongBinder());
                    int _arg228 = data.readInt();
                    String _arg39 = data.readString();
                    int _arg43 = data.readInt();
                    installPackageAsUser(_arg040, _arg139, _arg228, _arg39, _arg43);
                    reply.writeNoException();
                    return true;
                case 58:
                    data.enforceInterface(DESCRIPTOR);
                    int _arg041 = data.readInt();
                    boolean _arg140 = data.readInt() != 0;
                    finishPackageInstall(_arg041, _arg140);
                    reply.writeNoException();
                    return true;
                case 59:
                    data.enforceInterface(DESCRIPTOR);
                    String _arg042 = data.readString();
                    String _arg141 = data.readString();
                    setInstallerPackageName(_arg042, _arg141);
                    reply.writeNoException();
                    return true;
                case 60:
                    data.enforceInterface(DESCRIPTOR);
                    String _arg043 = data.readString();
                    IPackageDeleteObserver _arg142 = IPackageDeleteObserver.Stub.asInterface(data.readStrongBinder());
                    int _arg229 = data.readInt();
                    int _arg310 = data.readInt();
                    deletePackageAsUser(_arg043, _arg142, _arg229, _arg310);
                    reply.writeNoException();
                    return true;
                case 61:
                    data.enforceInterface(DESCRIPTOR);
                    String _arg044 = data.readString();
                    IPackageDeleteObserver2 _arg143 = IPackageDeleteObserver2.Stub.asInterface(data.readStrongBinder());
                    int _arg230 = data.readInt();
                    int _arg311 = data.readInt();
                    deletePackage(_arg044, _arg143, _arg230, _arg311);
                    reply.writeNoException();
                    return true;
                case 62:
                    data.enforceInterface(DESCRIPTOR);
                    String _arg045 = data.readString();
                    String _result49 = getInstallerPackageName(_arg045);
                    reply.writeNoException();
                    reply.writeString(_result49);
                    return true;
                case 63:
                    data.enforceInterface(DESCRIPTOR);
                    int _arg046 = data.readInt();
                    resetApplicationPreferences(_arg046);
                    reply.writeNoException();
                    return true;
                case 64:
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        intentCreateFromParcel2 = Intent.CREATOR.createFromParcel(data);
                    } else {
                        intentCreateFromParcel2 = null;
                    }
                    String _arg144 = data.readString();
                    int _arg231 = data.readInt();
                    ResolveInfo _result50 = getLastChosenActivity(intentCreateFromParcel2, _arg144, _arg231);
                    reply.writeNoException();
                    if (_result50 != null) {
                        reply.writeInt(1);
                        _result50.writeToParcel(reply, 1);
                        return true;
                    }
                    reply.writeInt(0);
                    return true;
                case 65:
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        intentCreateFromParcel = Intent.CREATOR.createFromParcel(data);
                    } else {
                        intentCreateFromParcel = null;
                    }
                    String _arg145 = data.readString();
                    int _arg232 = data.readInt();
                    if (data.readInt() != 0) {
                        intentFilterCreateFromParcel5 = IntentFilter.CREATOR.createFromParcel(data);
                    } else {
                        intentFilterCreateFromParcel5 = null;
                    }
                    int _arg44 = data.readInt();
                    if (data.readInt() != 0) {
                        componentNameCreateFromParcel7 = ComponentName.CREATOR.createFromParcel(data);
                    } else {
                        componentNameCreateFromParcel7 = null;
                    }
                    setLastChosenActivity(intentCreateFromParcel, _arg145, _arg232, intentFilterCreateFromParcel5, _arg44, componentNameCreateFromParcel7);
                    reply.writeNoException();
                    return true;
                case 66:
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        intentFilterCreateFromParcel4 = IntentFilter.CREATOR.createFromParcel(data);
                    } else {
                        intentFilterCreateFromParcel4 = null;
                    }
                    int _arg146 = data.readInt();
                    ComponentName[] _arg233 = (ComponentName[]) data.createTypedArray(ComponentName.CREATOR);
                    if (data.readInt() != 0) {
                        componentNameCreateFromParcel6 = ComponentName.CREATOR.createFromParcel(data);
                    } else {
                        componentNameCreateFromParcel6 = null;
                    }
                    int _arg45 = data.readInt();
                    addPreferredActivity(intentFilterCreateFromParcel4, _arg146, _arg233, componentNameCreateFromParcel6, _arg45);
                    reply.writeNoException();
                    return true;
                case 67:
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        intentFilterCreateFromParcel3 = IntentFilter.CREATOR.createFromParcel(data);
                    } else {
                        intentFilterCreateFromParcel3 = null;
                    }
                    int _arg147 = data.readInt();
                    ComponentName[] _arg234 = (ComponentName[]) data.createTypedArray(ComponentName.CREATOR);
                    if (data.readInt() != 0) {
                        componentNameCreateFromParcel5 = ComponentName.CREATOR.createFromParcel(data);
                    } else {
                        componentNameCreateFromParcel5 = null;
                    }
                    int _arg46 = data.readInt();
                    replacePreferredActivity(intentFilterCreateFromParcel3, _arg147, _arg234, componentNameCreateFromParcel5, _arg46);
                    reply.writeNoException();
                    return true;
                case 68:
                    data.enforceInterface(DESCRIPTOR);
                    String _arg047 = data.readString();
                    clearPackagePreferredActivities(_arg047);
                    reply.writeNoException();
                    return true;
                case 69:
                    data.enforceInterface(DESCRIPTOR);
                    ArrayList arrayList = new ArrayList();
                    ArrayList arrayList2 = new ArrayList();
                    String _arg235 = data.readString();
                    int _result51 = getPreferredActivities(arrayList, arrayList2, _arg235);
                    reply.writeNoException();
                    reply.writeInt(_result51);
                    reply.writeTypedList(arrayList);
                    reply.writeTypedList(arrayList2);
                    return true;
                case 70:
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        intentFilterCreateFromParcel2 = IntentFilter.CREATOR.createFromParcel(data);
                    } else {
                        intentFilterCreateFromParcel2 = null;
                    }
                    if (data.readInt() != 0) {
                        componentNameCreateFromParcel4 = ComponentName.CREATOR.createFromParcel(data);
                    } else {
                        componentNameCreateFromParcel4 = null;
                    }
                    int _arg236 = data.readInt();
                    addPersistentPreferredActivity(intentFilterCreateFromParcel2, componentNameCreateFromParcel4, _arg236);
                    reply.writeNoException();
                    return true;
                case 71:
                    data.enforceInterface(DESCRIPTOR);
                    String _arg048 = data.readString();
                    int _arg148 = data.readInt();
                    clearPackagePersistentPreferredActivities(_arg048, _arg148);
                    reply.writeNoException();
                    return true;
                case 72:
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        intentFilterCreateFromParcel = IntentFilter.CREATOR.createFromParcel(data);
                    } else {
                        intentFilterCreateFromParcel = null;
                    }
                    String _arg149 = data.readString();
                    int _arg237 = data.readInt();
                    int _arg312 = data.readInt();
                    int _arg47 = data.readInt();
                    addCrossProfileIntentFilter(intentFilterCreateFromParcel, _arg149, _arg237, _arg312, _arg47);
                    reply.writeNoException();
                    return true;
                case 73:
                    data.enforceInterface(DESCRIPTOR);
                    int _arg049 = data.readInt();
                    String _arg150 = data.readString();
                    clearCrossProfileIntentFilters(_arg049, _arg150);
                    reply.writeNoException();
                    return true;
                case 74:
                    data.enforceInterface(DESCRIPTOR);
                    String[] _arg050 = data.createStringArray();
                    boolean _arg151 = data.readInt() != 0;
                    int _arg238 = data.readInt();
                    String[] _result52 = setPackagesSuspendedAsUser(_arg050, _arg151, _arg238);
                    reply.writeNoException();
                    reply.writeStringArray(_result52);
                    return true;
                case 75:
                    data.enforceInterface(DESCRIPTOR);
                    String _arg051 = data.readString();
                    int _arg152 = data.readInt();
                    boolean _result53 = isPackageSuspendedForUser(_arg051, _arg152);
                    reply.writeNoException();
                    reply.writeInt(_result53 ? 1 : 0);
                    return true;
                case 76:
                    data.enforceInterface(DESCRIPTOR);
                    int _arg052 = data.readInt();
                    byte[] _result54 = getPreferredActivityBackup(_arg052);
                    reply.writeNoException();
                    reply.writeByteArray(_result54);
                    return true;
                case 77:
                    data.enforceInterface(DESCRIPTOR);
                    byte[] _arg053 = data.createByteArray();
                    int _arg153 = data.readInt();
                    restorePreferredActivities(_arg053, _arg153);
                    reply.writeNoException();
                    return true;
                case 78:
                    data.enforceInterface(DESCRIPTOR);
                    int _arg054 = data.readInt();
                    byte[] _result55 = getDefaultAppsBackup(_arg054);
                    reply.writeNoException();
                    reply.writeByteArray(_result55);
                    return true;
                case 79:
                    data.enforceInterface(DESCRIPTOR);
                    byte[] _arg055 = data.createByteArray();
                    int _arg154 = data.readInt();
                    restoreDefaultApps(_arg055, _arg154);
                    reply.writeNoException();
                    return true;
                case 80:
                    data.enforceInterface(DESCRIPTOR);
                    int _arg056 = data.readInt();
                    byte[] _result56 = getIntentFilterVerificationBackup(_arg056);
                    reply.writeNoException();
                    reply.writeByteArray(_result56);
                    return true;
                case 81:
                    data.enforceInterface(DESCRIPTOR);
                    byte[] _arg057 = data.createByteArray();
                    int _arg155 = data.readInt();
                    restoreIntentFilterVerification(_arg057, _arg155);
                    reply.writeNoException();
                    return true;
                case 82:
                    data.enforceInterface(DESCRIPTOR);
                    int _arg058 = data.readInt();
                    byte[] _result57 = getPermissionGrantBackup(_arg058);
                    reply.writeNoException();
                    reply.writeByteArray(_result57);
                    return true;
                case 83:
                    data.enforceInterface(DESCRIPTOR);
                    byte[] _arg059 = data.createByteArray();
                    int _arg156 = data.readInt();
                    restorePermissionGrants(_arg059, _arg156);
                    reply.writeNoException();
                    return true;
                case 84:
                    data.enforceInterface(DESCRIPTOR);
                    ArrayList arrayList3 = new ArrayList();
                    ComponentName _result58 = getHomeActivities(arrayList3);
                    reply.writeNoException();
                    if (_result58 != null) {
                        reply.writeInt(1);
                        _result58.writeToParcel(reply, 1);
                    } else {
                        reply.writeInt(0);
                    }
                    reply.writeTypedList(arrayList3);
                    return true;
                case 85:
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        componentNameCreateFromParcel3 = ComponentName.CREATOR.createFromParcel(data);
                    } else {
                        componentNameCreateFromParcel3 = null;
                    }
                    int _arg157 = data.readInt();
                    setHomeActivity(componentNameCreateFromParcel3, _arg157);
                    reply.writeNoException();
                    return true;
                case 86:
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        componentNameCreateFromParcel2 = ComponentName.CREATOR.createFromParcel(data);
                    } else {
                        componentNameCreateFromParcel2 = null;
                    }
                    int _arg158 = data.readInt();
                    int _arg239 = data.readInt();
                    int _arg313 = data.readInt();
                    setComponentEnabledSetting(componentNameCreateFromParcel2, _arg158, _arg239, _arg313);
                    reply.writeNoException();
                    return true;
                case 87:
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        componentNameCreateFromParcel = ComponentName.CREATOR.createFromParcel(data);
                    } else {
                        componentNameCreateFromParcel = null;
                    }
                    int _arg159 = data.readInt();
                    int _result59 = getComponentEnabledSetting(componentNameCreateFromParcel, _arg159);
                    reply.writeNoException();
                    reply.writeInt(_result59);
                    return true;
                case 88:
                    data.enforceInterface(DESCRIPTOR);
                    String _arg060 = data.readString();
                    int _arg160 = data.readInt();
                    int _arg240 = data.readInt();
                    int _arg314 = data.readInt();
                    String _arg48 = data.readString();
                    setApplicationEnabledSetting(_arg060, _arg160, _arg240, _arg314, _arg48);
                    reply.writeNoException();
                    return true;
                case 89:
                    data.enforceInterface(DESCRIPTOR);
                    String _arg061 = data.readString();
                    int _arg161 = data.readInt();
                    int _result60 = getApplicationEnabledSetting(_arg061, _arg161);
                    reply.writeNoException();
                    reply.writeInt(_result60);
                    return true;
                case 90:
                    data.enforceInterface(DESCRIPTOR);
                    String _arg062 = data.readString();
                    int _arg162 = data.readInt();
                    String _arg241 = data.readString();
                    String _arg315 = data.readString();
                    int _arg49 = data.readInt();
                    logAppProcessStartIfNeeded(_arg062, _arg162, _arg241, _arg315, _arg49);
                    reply.writeNoException();
                    return true;
                case 91:
                    data.enforceInterface(DESCRIPTOR);
                    int _arg063 = data.readInt();
                    flushPackageRestrictionsAsUser(_arg063);
                    reply.writeNoException();
                    return true;
                case 92:
                    data.enforceInterface(DESCRIPTOR);
                    String _arg064 = data.readString();
                    boolean _arg163 = data.readInt() != 0;
                    int _arg242 = data.readInt();
                    setPackageStoppedState(_arg064, _arg163, _arg242);
                    reply.writeNoException();
                    return true;
                case 93:
                    data.enforceInterface(DESCRIPTOR);
                    String _arg065 = data.readString();
                    long _arg164 = data.readLong();
                    IPackageDataObserver _arg243 = IPackageDataObserver.Stub.asInterface(data.readStrongBinder());
                    freeStorageAndNotify(_arg065, _arg164, _arg243);
                    reply.writeNoException();
                    return true;
                case 94:
                    data.enforceInterface(DESCRIPTOR);
                    String _arg066 = data.readString();
                    long _arg165 = data.readLong();
                    if (data.readInt() != 0) {
                        intentSenderCreateFromParcel = IntentSender.CREATOR.createFromParcel(data);
                    } else {
                        intentSenderCreateFromParcel = null;
                    }
                    freeStorage(_arg066, _arg165, intentSenderCreateFromParcel);
                    reply.writeNoException();
                    return true;
                case 95:
                    data.enforceInterface(DESCRIPTOR);
                    String _arg067 = data.readString();
                    IPackageDataObserver _arg166 = IPackageDataObserver.Stub.asInterface(data.readStrongBinder());
                    deleteApplicationCacheFiles(_arg067, _arg166);
                    reply.writeNoException();
                    return true;
                case 96:
                    data.enforceInterface(DESCRIPTOR);
                    String _arg068 = data.readString();
                    int _arg167 = data.readInt();
                    IPackageDataObserver _arg244 = IPackageDataObserver.Stub.asInterface(data.readStrongBinder());
                    deleteApplicationCacheFilesAsUser(_arg068, _arg167, _arg244);
                    reply.writeNoException();
                    return true;
                case 97:
                    data.enforceInterface(DESCRIPTOR);
                    String _arg069 = data.readString();
                    IPackageDataObserver _arg168 = IPackageDataObserver.Stub.asInterface(data.readStrongBinder());
                    int _arg245 = data.readInt();
                    clearApplicationUserData(_arg069, _arg168, _arg245);
                    reply.writeNoException();
                    return true;
                case 98:
                    data.enforceInterface(DESCRIPTOR);
                    String _arg070 = data.readString();
                    clearApplicationProfileData(_arg070);
                    reply.writeNoException();
                    return true;
                case 99:
                    data.enforceInterface(DESCRIPTOR);
                    String _arg071 = data.readString();
                    int _arg169 = data.readInt();
                    IPackageStatsObserver _arg246 = IPackageStatsObserver.Stub.asInterface(data.readStrongBinder());
                    getPackageSizeInfo(_arg071, _arg169, _arg246);
                    reply.writeNoException();
                    return true;
                case 100:
                    data.enforceInterface(DESCRIPTOR);
                    String[] _result61 = getSystemSharedLibraryNames();
                    reply.writeNoException();
                    reply.writeStringArray(_result61);
                    return true;
                case 101:
                    data.enforceInterface(DESCRIPTOR);
                    ParceledListSlice _result62 = getSystemAvailableFeatures();
                    reply.writeNoException();
                    if (_result62 != null) {
                        reply.writeInt(1);
                        _result62.writeToParcel(reply, 1);
                        return true;
                    }
                    reply.writeInt(0);
                    return true;
                case 102:
                    data.enforceInterface(DESCRIPTOR);
                    String _arg072 = data.readString();
                    int _arg170 = data.readInt();
                    boolean _result63 = hasSystemFeature(_arg072, _arg170);
                    reply.writeNoException();
                    reply.writeInt(_result63 ? 1 : 0);
                    return true;
                case 103:
                    data.enforceInterface(DESCRIPTOR);
                    enterSafeMode();
                    reply.writeNoException();
                    return true;
                case 104:
                    data.enforceInterface(DESCRIPTOR);
                    boolean _result64 = isSafeMode();
                    reply.writeNoException();
                    reply.writeInt(_result64 ? 1 : 0);
                    return true;
                case 105:
                    data.enforceInterface(DESCRIPTOR);
                    systemReady();
                    reply.writeNoException();
                    return true;
                case 106:
                    data.enforceInterface(DESCRIPTOR);
                    boolean _result65 = hasSystemUidErrors();
                    reply.writeNoException();
                    reply.writeInt(_result65 ? 1 : 0);
                    return true;
                case 107:
                    data.enforceInterface(DESCRIPTOR);
                    performFstrimIfNeeded();
                    reply.writeNoException();
                    return true;
                case 108:
                    data.enforceInterface(DESCRIPTOR);
                    updatePackagesIfNeeded();
                    reply.writeNoException();
                    return true;
                case 109:
                    data.enforceInterface(DESCRIPTOR);
                    String _arg073 = data.readString();
                    int _arg171 = data.readInt();
                    notifyPackageUse(_arg073, _arg171);
                    reply.writeNoException();
                    return true;
                case 110:
                    data.enforceInterface(DESCRIPTOR);
                    String _arg074 = data.readString();
                    boolean _result66 = performDexOptIfNeeded(_arg074);
                    reply.writeNoException();
                    reply.writeInt(_result66 ? 1 : 0);
                    return true;
                case 111:
                    data.enforceInterface(DESCRIPTOR);
                    String _arg075 = data.readString();
                    boolean _arg172 = data.readInt() != 0;
                    int _arg247 = data.readInt();
                    boolean _arg316 = data.readInt() != 0;
                    boolean _result67 = performDexOpt(_arg075, _arg172, _arg247, _arg316);
                    reply.writeNoException();
                    reply.writeInt(_result67 ? 1 : 0);
                    return true;
                case 112:
                    data.enforceInterface(DESCRIPTOR);
                    String _arg076 = data.readString();
                    boolean _arg173 = data.readInt() != 0;
                    String _arg248 = data.readString();
                    boolean _arg317 = data.readInt() != 0;
                    boolean _result68 = performDexOptMode(_arg076, _arg173, _arg248, _arg317);
                    reply.writeNoException();
                    reply.writeInt(_result68 ? 1 : 0);
                    return true;
                case 113:
                    data.enforceInterface(DESCRIPTOR);
                    String _arg077 = data.readString();
                    dumpProfiles(_arg077);
                    reply.writeNoException();
                    return true;
                case 114:
                    data.enforceInterface(DESCRIPTOR);
                    String _arg078 = data.readString();
                    forceDexOpt(_arg078);
                    reply.writeNoException();
                    return true;
                case 115:
                    data.enforceInterface(DESCRIPTOR);
                    boolean _arg079 = data.readInt() != 0;
                    boolean _arg174 = data.readInt() != 0;
                    updateExternalMediaStatus(_arg079, _arg174);
                    reply.writeNoException();
                    return true;
                case 116:
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        packageCleanItemCreateFromParcel = PackageCleanItem.CREATOR.createFromParcel(data);
                    } else {
                        packageCleanItemCreateFromParcel = null;
                    }
                    PackageCleanItem _result69 = nextPackageToClean(packageCleanItemCreateFromParcel);
                    reply.writeNoException();
                    if (_result69 != null) {
                        reply.writeInt(1);
                        _result69.writeToParcel(reply, 1);
                        return true;
                    }
                    reply.writeInt(0);
                    return true;
                case 117:
                    data.enforceInterface(DESCRIPTOR);
                    int _arg080 = data.readInt();
                    int _result70 = getMoveStatus(_arg080);
                    reply.writeNoException();
                    reply.writeInt(_result70);
                    return true;
                case 118:
                    data.enforceInterface(DESCRIPTOR);
                    IPackageMoveObserver _arg081 = IPackageMoveObserver.Stub.asInterface(data.readStrongBinder());
                    registerMoveCallback(_arg081);
                    reply.writeNoException();
                    return true;
                case 119:
                    data.enforceInterface(DESCRIPTOR);
                    IPackageMoveObserver _arg082 = IPackageMoveObserver.Stub.asInterface(data.readStrongBinder());
                    unregisterMoveCallback(_arg082);
                    reply.writeNoException();
                    return true;
                case 120:
                    data.enforceInterface(DESCRIPTOR);
                    String _arg083 = data.readString();
                    String _arg175 = data.readString();
                    int _result71 = movePackage(_arg083, _arg175);
                    reply.writeNoException();
                    reply.writeInt(_result71);
                    return true;
                case 121:
                    data.enforceInterface(DESCRIPTOR);
                    String _arg084 = data.readString();
                    int _result72 = movePrimaryStorage(_arg084);
                    reply.writeNoException();
                    reply.writeInt(_result72);
                    return true;
                case 122:
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        permissionInfoCreateFromParcel = PermissionInfo.CREATOR.createFromParcel(data);
                    } else {
                        permissionInfoCreateFromParcel = null;
                    }
                    boolean _result73 = addPermissionAsync(permissionInfoCreateFromParcel);
                    reply.writeNoException();
                    reply.writeInt(_result73 ? 1 : 0);
                    return true;
                case 123:
                    data.enforceInterface(DESCRIPTOR);
                    int _arg085 = data.readInt();
                    boolean _result74 = setInstallLocation(_arg085);
                    reply.writeNoException();
                    reply.writeInt(_result74 ? 1 : 0);
                    return true;
                case 124:
                    data.enforceInterface(DESCRIPTOR);
                    int _result75 = getInstallLocation();
                    reply.writeNoException();
                    reply.writeInt(_result75);
                    return true;
                case 125:
                    data.enforceInterface(DESCRIPTOR);
                    String _arg086 = data.readString();
                    int _arg176 = data.readInt();
                    int _result76 = installExistingPackageAsUser(_arg086, _arg176);
                    reply.writeNoException();
                    reply.writeInt(_result76);
                    return true;
                case 126:
                    data.enforceInterface(DESCRIPTOR);
                    int _arg087 = data.readInt();
                    int _arg177 = data.readInt();
                    verifyPendingInstall(_arg087, _arg177);
                    reply.writeNoException();
                    return true;
                case 127:
                    data.enforceInterface(DESCRIPTOR);
                    int _arg088 = data.readInt();
                    int _arg178 = data.readInt();
                    long _arg249 = data.readLong();
                    extendVerificationTimeout(_arg088, _arg178, _arg249);
                    reply.writeNoException();
                    return true;
                case 128:
                    data.enforceInterface(DESCRIPTOR);
                    int _arg089 = data.readInt();
                    int _arg179 = data.readInt();
                    List<String> _arg250 = data.createStringArrayList();
                    verifyIntentFilter(_arg089, _arg179, _arg250);
                    reply.writeNoException();
                    return true;
                case 129:
                    data.enforceInterface(DESCRIPTOR);
                    String _arg090 = data.readString();
                    int _arg180 = data.readInt();
                    int _result77 = getIntentVerificationStatus(_arg090, _arg180);
                    reply.writeNoException();
                    reply.writeInt(_result77);
                    return true;
                case 130:
                    data.enforceInterface(DESCRIPTOR);
                    String _arg091 = data.readString();
                    int _arg181 = data.readInt();
                    int _arg251 = data.readInt();
                    boolean _result78 = updateIntentVerificationStatus(_arg091, _arg181, _arg251);
                    reply.writeNoException();
                    reply.writeInt(_result78 ? 1 : 0);
                    return true;
                case 131:
                    data.enforceInterface(DESCRIPTOR);
                    String _arg092 = data.readString();
                    ParceledListSlice _result79 = getIntentFilterVerifications(_arg092);
                    reply.writeNoException();
                    if (_result79 != null) {
                        reply.writeInt(1);
                        _result79.writeToParcel(reply, 1);
                        return true;
                    }
                    reply.writeInt(0);
                    return true;
                case 132:
                    data.enforceInterface(DESCRIPTOR);
                    String _arg093 = data.readString();
                    ParceledListSlice _result80 = getAllIntentFilters(_arg093);
                    reply.writeNoException();
                    if (_result80 != null) {
                        reply.writeInt(1);
                        _result80.writeToParcel(reply, 1);
                        return true;
                    }
                    reply.writeInt(0);
                    return true;
                case 133:
                    data.enforceInterface(DESCRIPTOR);
                    String _arg094 = data.readString();
                    int _arg182 = data.readInt();
                    boolean _result81 = setDefaultBrowserPackageName(_arg094, _arg182);
                    reply.writeNoException();
                    reply.writeInt(_result81 ? 1 : 0);
                    return true;
                case 134:
                    data.enforceInterface(DESCRIPTOR);
                    int _arg095 = data.readInt();
                    String _result82 = getDefaultBrowserPackageName(_arg095);
                    reply.writeNoException();
                    reply.writeString(_result82);
                    return true;
                case 135:
                    data.enforceInterface(DESCRIPTOR);
                    VerifierDeviceIdentity _result83 = getVerifierDeviceIdentity();
                    reply.writeNoException();
                    if (_result83 != null) {
                        reply.writeInt(1);
                        _result83.writeToParcel(reply, 1);
                        return true;
                    }
                    reply.writeInt(0);
                    return true;
                case 136:
                    data.enforceInterface(DESCRIPTOR);
                    boolean _result84 = isFirstBoot();
                    reply.writeNoException();
                    reply.writeInt(_result84 ? 1 : 0);
                    return true;
                case 137:
                    data.enforceInterface(DESCRIPTOR);
                    boolean _result85 = isOnlyCoreApps();
                    reply.writeNoException();
                    reply.writeInt(_result85 ? 1 : 0);
                    return true;
                case 138:
                    data.enforceInterface(DESCRIPTOR);
                    boolean _result86 = isUpgrade();
                    reply.writeNoException();
                    reply.writeInt(_result86 ? 1 : 0);
                    return true;
                case 139:
                    data.enforceInterface(DESCRIPTOR);
                    String _arg096 = data.readString();
                    int _result87 = checkAPKSignatures(_arg096);
                    reply.writeNoException();
                    reply.writeInt(_result87);
                    return true;
                case 140:
                    data.enforceInterface(DESCRIPTOR);
                    String _arg097 = data.readString();
                    boolean _arg183 = data.readInt() != 0;
                    setPermissionEnforced(_arg097, _arg183);
                    reply.writeNoException();
                    return true;
                case 141:
                    data.enforceInterface(DESCRIPTOR);
                    String _arg098 = data.readString();
                    boolean _result88 = isPermissionEnforced(_arg098);
                    reply.writeNoException();
                    reply.writeInt(_result88 ? 1 : 0);
                    return true;
                case 142:
                    data.enforceInterface(DESCRIPTOR);
                    boolean _result89 = isStorageLow();
                    reply.writeNoException();
                    reply.writeInt(_result89 ? 1 : 0);
                    return true;
                case 143:
                    data.enforceInterface(DESCRIPTOR);
                    String _arg099 = data.readString();
                    boolean _arg184 = data.readInt() != 0;
                    int _arg252 = data.readInt();
                    boolean _result90 = setApplicationHiddenSettingAsUser(_arg099, _arg184, _arg252);
                    reply.writeNoException();
                    reply.writeInt(_result90 ? 1 : 0);
                    return true;
                case 144:
                    data.enforceInterface(DESCRIPTOR);
                    String _arg0100 = data.readString();
                    int _arg185 = data.readInt();
                    boolean _result91 = getApplicationHiddenSettingAsUser(_arg0100, _arg185);
                    reply.writeNoException();
                    reply.writeInt(_result91 ? 1 : 0);
                    return true;
                case 145:
                    data.enforceInterface(DESCRIPTOR);
                    IPackageInstaller _result92 = getPackageInstaller();
                    reply.writeNoException();
                    reply.writeStrongBinder(_result92 != null ? _result92.asBinder() : null);
                    return true;
                case 146:
                    data.enforceInterface(DESCRIPTOR);
                    String _arg0101 = data.readString();
                    boolean _arg186 = data.readInt() != 0;
                    int _arg253 = data.readInt();
                    boolean _result93 = setBlockUninstallForUser(_arg0101, _arg186, _arg253);
                    reply.writeNoException();
                    reply.writeInt(_result93 ? 1 : 0);
                    return true;
                case 147:
                    data.enforceInterface(DESCRIPTOR);
                    String _arg0102 = data.readString();
                    int _arg187 = data.readInt();
                    boolean _result94 = getBlockUninstallForUser(_arg0102, _arg187);
                    reply.writeNoException();
                    reply.writeInt(_result94 ? 1 : 0);
                    return true;
                case 148:
                    data.enforceInterface(DESCRIPTOR);
                    String _arg0103 = data.readString();
                    String _arg188 = data.readString();
                    KeySet _result95 = getKeySetByAlias(_arg0103, _arg188);
                    reply.writeNoException();
                    if (_result95 != null) {
                        reply.writeInt(1);
                        _result95.writeToParcel(reply, 1);
                        return true;
                    }
                    reply.writeInt(0);
                    return true;
                case 149:
                    data.enforceInterface(DESCRIPTOR);
                    String _arg0104 = data.readString();
                    KeySet _result96 = getSigningKeySet(_arg0104);
                    reply.writeNoException();
                    if (_result96 != null) {
                        reply.writeInt(1);
                        _result96.writeToParcel(reply, 1);
                        return true;
                    }
                    reply.writeInt(0);
                    return true;
                case 150:
                    data.enforceInterface(DESCRIPTOR);
                    String _arg0105 = data.readString();
                    if (data.readInt() != 0) {
                        keySetCreateFromParcel2 = KeySet.CREATOR.createFromParcel(data);
                    } else {
                        keySetCreateFromParcel2 = null;
                    }
                    boolean _result97 = isPackageSignedByKeySet(_arg0105, keySetCreateFromParcel2);
                    reply.writeNoException();
                    reply.writeInt(_result97 ? 1 : 0);
                    return true;
                case 151:
                    data.enforceInterface(DESCRIPTOR);
                    String _arg0106 = data.readString();
                    if (data.readInt() != 0) {
                        keySetCreateFromParcel = KeySet.CREATOR.createFromParcel(data);
                    } else {
                        keySetCreateFromParcel = null;
                    }
                    boolean _result98 = isPackageSignedByKeySetExactly(_arg0106, keySetCreateFromParcel);
                    reply.writeNoException();
                    reply.writeInt(_result98 ? 1 : 0);
                    return true;
                case 152:
                    data.enforceInterface(DESCRIPTOR);
                    IOnPermissionsChangeListener _arg0107 = IOnPermissionsChangeListener.Stub.asInterface(data.readStrongBinder());
                    addOnPermissionsChangeListener(_arg0107);
                    reply.writeNoException();
                    return true;
                case 153:
                    data.enforceInterface(DESCRIPTOR);
                    IOnPermissionsChangeListener _arg0108 = IOnPermissionsChangeListener.Stub.asInterface(data.readStrongBinder());
                    removeOnPermissionsChangeListener(_arg0108);
                    reply.writeNoException();
                    return true;
                case 154:
                    data.enforceInterface(DESCRIPTOR);
                    String[] _arg0109 = data.createStringArray();
                    int _arg189 = data.readInt();
                    grantDefaultPermissionsToEnabledCarrierApps(_arg0109, _arg189);
                    reply.writeNoException();
                    return true;
                case 155:
                    data.enforceInterface(DESCRIPTOR);
                    String _arg0110 = data.readString();
                    String _arg190 = data.readString();
                    int _arg254 = data.readInt();
                    boolean _result99 = isPermissionRevokedByPolicy(_arg0110, _arg190, _arg254);
                    reply.writeNoException();
                    reply.writeInt(_result99 ? 1 : 0);
                    return true;
                case 156:
                    data.enforceInterface(DESCRIPTOR);
                    String _result100 = getPermissionControllerPackageName();
                    reply.writeNoException();
                    reply.writeString(_result100);
                    return true;
                case 157:
                    data.enforceInterface(DESCRIPTOR);
                    int _arg0111 = data.readInt();
                    ParceledListSlice _result101 = getEphemeralApplications(_arg0111);
                    reply.writeNoException();
                    if (_result101 != null) {
                        reply.writeInt(1);
                        _result101.writeToParcel(reply, 1);
                        return true;
                    }
                    reply.writeInt(0);
                    return true;
                case 158:
                    data.enforceInterface(DESCRIPTOR);
                    String _arg0112 = data.readString();
                    int _arg191 = data.readInt();
                    byte[] _result102 = getEphemeralApplicationCookie(_arg0112, _arg191);
                    reply.writeNoException();
                    reply.writeByteArray(_result102);
                    return true;
                case 159:
                    data.enforceInterface(DESCRIPTOR);
                    String _arg0113 = data.readString();
                    byte[] _arg192 = data.createByteArray();
                    int _arg255 = data.readInt();
                    boolean _result103 = setEphemeralApplicationCookie(_arg0113, _arg192, _arg255);
                    reply.writeNoException();
                    reply.writeInt(_result103 ? 1 : 0);
                    return true;
                case 160:
                    data.enforceInterface(DESCRIPTOR);
                    String _arg0114 = data.readString();
                    int _arg193 = data.readInt();
                    Bitmap _result104 = getEphemeralApplicationIcon(_arg0114, _arg193);
                    reply.writeNoException();
                    if (_result104 != null) {
                        reply.writeInt(1);
                        _result104.writeToParcel(reply, 1);
                        return true;
                    }
                    reply.writeInt(0);
                    return true;
                case 161:
                    data.enforceInterface(DESCRIPTOR);
                    String _arg0115 = data.readString();
                    int _arg194 = data.readInt();
                    boolean _result105 = isEphemeralApplication(_arg0115, _arg194);
                    reply.writeNoException();
                    reply.writeInt(_result105 ? 1 : 0);
                    return true;
                case 162:
                    data.enforceInterface(DESCRIPTOR);
                    String _arg0116 = data.readString();
                    boolean _arg195 = data.readInt() != 0;
                    boolean _result106 = setRequiredForSystemUser(_arg0116, _arg195);
                    reply.writeNoException();
                    reply.writeInt(_result106 ? 1 : 0);
                    return true;
                case 163:
                    data.enforceInterface(DESCRIPTOR);
                    String _result107 = getServicesSystemSharedLibraryPackageName();
                    reply.writeNoException();
                    reply.writeString(_result107);
                    return true;
                case 164:
                    data.enforceInterface(DESCRIPTOR);
                    String _result108 = getSharedSystemSharedLibraryPackageName();
                    reply.writeNoException();
                    reply.writeString(_result108);
                    return true;
                case 165:
                    data.enforceInterface(DESCRIPTOR);
                    String _arg0117 = data.readString();
                    boolean _result109 = isPackageDeviceAdminOnAnyUser(_arg0117);
                    reply.writeNoException();
                    reply.writeInt(_result109 ? 1 : 0);
                    return true;
                case 166:
                    data.enforceInterface(DESCRIPTOR);
                    String _arg0118 = data.readString();
                    List<String> _result110 = getPreviousCodePaths(_arg0118);
                    reply.writeNoException();
                    reply.writeStringList(_result110);
                    return true;
                case 167:
                    data.enforceInterface(DESCRIPTOR);
                    List<String> _result111 = getPermRecordPkgs();
                    reply.writeNoException();
                    reply.writeStringList(_result111);
                    return true;
                case 168:
                    data.enforceInterface(DESCRIPTOR);
                    String _arg0119 = data.readString();
                    List<String> _result112 = getPermRecordPerms(_arg0119);
                    reply.writeNoException();
                    reply.writeStringList(_result112);
                    return true;
                case 169:
                    data.enforceInterface(DESCRIPTOR);
                    String _arg0120 = data.readString();
                    String _arg196 = data.readString();
                    PermissionRecords _result113 = getPermRecords(_arg0120, _arg196);
                    reply.writeNoException();
                    if (_result113 != null) {
                        reply.writeInt(1);
                        _result113.writeToParcel(reply, 1);
                        return true;
                    }
                    reply.writeInt(0);
                    return true;
                case IBinder.INTERFACE_TRANSACTION:
                    reply.writeString(DESCRIPTOR);
                    return true;
                default:
                    return super.onTransact(code, data, reply, flags);
            }
        }

        private static class Proxy implements IPackageManager {
            private IBinder mRemote;

            Proxy(IBinder remote) {
                this.mRemote = remote;
            }

            @Override
            public IBinder asBinder() {
                return this.mRemote;
            }

            public String getInterfaceDescriptor() {
                return Stub.DESCRIPTOR;
            }

            @Override
            public void checkPackageStartable(String packageName, int userId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(packageName);
                    _data.writeInt(userId);
                    this.mRemote.transact(1, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public boolean isPackageAvailable(String packageName, int userId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(packageName);
                    _data.writeInt(userId);
                    this.mRemote.transact(2, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readInt() != 0;
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public PackageInfo getPackageInfo(String packageName, int flags, int userId) throws RemoteException {
                PackageInfo packageInfoCreateFromParcel;
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(packageName);
                    _data.writeInt(flags);
                    _data.writeInt(userId);
                    this.mRemote.transact(3, _data, _reply, 0);
                    _reply.readException();
                    if (_reply.readInt() != 0) {
                        packageInfoCreateFromParcel = PackageInfo.CREATOR.createFromParcel(_reply);
                    } else {
                        packageInfoCreateFromParcel = null;
                    }
                    return packageInfoCreateFromParcel;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public int getPackageUid(String packageName, int flags, int userId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(packageName);
                    _data.writeInt(flags);
                    _data.writeInt(userId);
                    this.mRemote.transact(4, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public int[] getPackageGids(String packageName, int flags, int userId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(packageName);
                    _data.writeInt(flags);
                    _data.writeInt(userId);
                    this.mRemote.transact(5, _data, _reply, 0);
                    _reply.readException();
                    int[] _result = _reply.createIntArray();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public String[] currentToCanonicalPackageNames(String[] names) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStringArray(names);
                    this.mRemote.transact(6, _data, _reply, 0);
                    _reply.readException();
                    String[] _result = _reply.createStringArray();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public String[] canonicalToCurrentPackageNames(String[] names) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStringArray(names);
                    this.mRemote.transact(7, _data, _reply, 0);
                    _reply.readException();
                    String[] _result = _reply.createStringArray();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public PermissionInfo getPermissionInfo(String name, int flags) throws RemoteException {
                PermissionInfo permissionInfoCreateFromParcel;
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(name);
                    _data.writeInt(flags);
                    this.mRemote.transact(8, _data, _reply, 0);
                    _reply.readException();
                    if (_reply.readInt() != 0) {
                        permissionInfoCreateFromParcel = PermissionInfo.CREATOR.createFromParcel(_reply);
                    } else {
                        permissionInfoCreateFromParcel = null;
                    }
                    return permissionInfoCreateFromParcel;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public ParceledListSlice queryPermissionsByGroup(String group, int flags) throws RemoteException {
                ParceledListSlice parceledListSliceCreateFromParcel;
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(group);
                    _data.writeInt(flags);
                    this.mRemote.transact(9, _data, _reply, 0);
                    _reply.readException();
                    if (_reply.readInt() != 0) {
                        parceledListSliceCreateFromParcel = ParceledListSlice.CREATOR.createFromParcel(_reply);
                    } else {
                        parceledListSliceCreateFromParcel = null;
                    }
                    return parceledListSliceCreateFromParcel;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public PermissionGroupInfo getPermissionGroupInfo(String name, int flags) throws RemoteException {
                PermissionGroupInfo permissionGroupInfoCreateFromParcel;
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(name);
                    _data.writeInt(flags);
                    this.mRemote.transact(10, _data, _reply, 0);
                    _reply.readException();
                    if (_reply.readInt() != 0) {
                        permissionGroupInfoCreateFromParcel = PermissionGroupInfo.CREATOR.createFromParcel(_reply);
                    } else {
                        permissionGroupInfoCreateFromParcel = null;
                    }
                    return permissionGroupInfoCreateFromParcel;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public ParceledListSlice getAllPermissionGroups(int flags) throws RemoteException {
                ParceledListSlice parceledListSliceCreateFromParcel;
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(flags);
                    this.mRemote.transact(11, _data, _reply, 0);
                    _reply.readException();
                    if (_reply.readInt() != 0) {
                        parceledListSliceCreateFromParcel = ParceledListSlice.CREATOR.createFromParcel(_reply);
                    } else {
                        parceledListSliceCreateFromParcel = null;
                    }
                    return parceledListSliceCreateFromParcel;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public ApplicationInfo getApplicationInfo(String packageName, int flags, int userId) throws RemoteException {
                ApplicationInfo applicationInfoCreateFromParcel;
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(packageName);
                    _data.writeInt(flags);
                    _data.writeInt(userId);
                    this.mRemote.transact(12, _data, _reply, 0);
                    _reply.readException();
                    if (_reply.readInt() != 0) {
                        applicationInfoCreateFromParcel = ApplicationInfo.CREATOR.createFromParcel(_reply);
                    } else {
                        applicationInfoCreateFromParcel = null;
                    }
                    return applicationInfoCreateFromParcel;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public ActivityInfo getActivityInfo(ComponentName className, int flags, int userId) throws RemoteException {
                ActivityInfo activityInfoCreateFromParcel;
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (className != null) {
                        _data.writeInt(1);
                        className.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeInt(flags);
                    _data.writeInt(userId);
                    this.mRemote.transact(13, _data, _reply, 0);
                    _reply.readException();
                    if (_reply.readInt() != 0) {
                        activityInfoCreateFromParcel = ActivityInfo.CREATOR.createFromParcel(_reply);
                    } else {
                        activityInfoCreateFromParcel = null;
                    }
                    return activityInfoCreateFromParcel;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public boolean activitySupportsIntent(ComponentName className, Intent intent, String resolvedType) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (className != null) {
                        _data.writeInt(1);
                        className.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    if (intent != null) {
                        _data.writeInt(1);
                        intent.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeString(resolvedType);
                    this.mRemote.transact(14, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readInt() != 0;
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public ActivityInfo getReceiverInfo(ComponentName className, int flags, int userId) throws RemoteException {
                ActivityInfo activityInfoCreateFromParcel;
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (className != null) {
                        _data.writeInt(1);
                        className.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeInt(flags);
                    _data.writeInt(userId);
                    this.mRemote.transact(15, _data, _reply, 0);
                    _reply.readException();
                    if (_reply.readInt() != 0) {
                        activityInfoCreateFromParcel = ActivityInfo.CREATOR.createFromParcel(_reply);
                    } else {
                        activityInfoCreateFromParcel = null;
                    }
                    return activityInfoCreateFromParcel;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public ServiceInfo getServiceInfo(ComponentName className, int flags, int userId) throws RemoteException {
                ServiceInfo serviceInfoCreateFromParcel;
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (className != null) {
                        _data.writeInt(1);
                        className.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeInt(flags);
                    _data.writeInt(userId);
                    this.mRemote.transact(16, _data, _reply, 0);
                    _reply.readException();
                    if (_reply.readInt() != 0) {
                        serviceInfoCreateFromParcel = ServiceInfo.CREATOR.createFromParcel(_reply);
                    } else {
                        serviceInfoCreateFromParcel = null;
                    }
                    return serviceInfoCreateFromParcel;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public ProviderInfo getProviderInfo(ComponentName className, int flags, int userId) throws RemoteException {
                ProviderInfo providerInfoCreateFromParcel;
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (className != null) {
                        _data.writeInt(1);
                        className.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeInt(flags);
                    _data.writeInt(userId);
                    this.mRemote.transact(17, _data, _reply, 0);
                    _reply.readException();
                    if (_reply.readInt() != 0) {
                        providerInfoCreateFromParcel = ProviderInfo.CREATOR.createFromParcel(_reply);
                    } else {
                        providerInfoCreateFromParcel = null;
                    }
                    return providerInfoCreateFromParcel;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public int checkPermission(String permName, String pkgName, int userId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(permName);
                    _data.writeString(pkgName);
                    _data.writeInt(userId);
                    this.mRemote.transact(18, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public int checkUidPermission(String permName, int uid) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(permName);
                    _data.writeInt(uid);
                    this.mRemote.transact(19, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public boolean addPermission(PermissionInfo info) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (info != null) {
                        _data.writeInt(1);
                        info.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(20, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readInt() != 0;
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void removePermission(String name) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(name);
                    this.mRemote.transact(21, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void grantRuntimePermission(String packageName, String permissionName, int userId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(packageName);
                    _data.writeString(permissionName);
                    _data.writeInt(userId);
                    this.mRemote.transact(22, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void revokeRuntimePermission(String packageName, String permissionName, int userId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(packageName);
                    _data.writeString(permissionName);
                    _data.writeInt(userId);
                    this.mRemote.transact(23, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void resetRuntimePermissions() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(24, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public int getPermissionFlags(String permissionName, String packageName, int userId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(permissionName);
                    _data.writeString(packageName);
                    _data.writeInt(userId);
                    this.mRemote.transact(25, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void updatePermissionFlags(String permissionName, String packageName, int flagMask, int flagValues, int userId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(permissionName);
                    _data.writeString(packageName);
                    _data.writeInt(flagMask);
                    _data.writeInt(flagValues);
                    _data.writeInt(userId);
                    this.mRemote.transact(26, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void updatePermissionFlagsForAllApps(int flagMask, int flagValues, int userId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(flagMask);
                    _data.writeInt(flagValues);
                    _data.writeInt(userId);
                    this.mRemote.transact(27, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public boolean shouldShowRequestPermissionRationale(String permissionName, String packageName, int userId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(permissionName);
                    _data.writeString(packageName);
                    _data.writeInt(userId);
                    this.mRemote.transact(28, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readInt() != 0;
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public boolean isProtectedBroadcast(String actionName) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(actionName);
                    this.mRemote.transact(29, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readInt() != 0;
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public int checkSignatures(String pkg1, String pkg2) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(pkg1);
                    _data.writeString(pkg2);
                    this.mRemote.transact(30, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public int checkUidSignatures(int uid1, int uid2) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(uid1);
                    _data.writeInt(uid2);
                    this.mRemote.transact(31, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public List<String> getAllPackages() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(32, _data, _reply, 0);
                    _reply.readException();
                    List<String> _result = _reply.createStringArrayList();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public String[] getPackagesForUid(int uid) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(uid);
                    this.mRemote.transact(33, _data, _reply, 0);
                    _reply.readException();
                    String[] _result = _reply.createStringArray();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public String getNameForUid(int uid) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(uid);
                    this.mRemote.transact(34, _data, _reply, 0);
                    _reply.readException();
                    String _result = _reply.readString();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public int getUidForSharedUser(String sharedUserName) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(sharedUserName);
                    this.mRemote.transact(35, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public int getFlagsForUid(int uid) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(uid);
                    this.mRemote.transact(36, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public int getPrivateFlagsForUid(int uid) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(uid);
                    this.mRemote.transact(37, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public boolean isUidPrivileged(int uid) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(uid);
                    this.mRemote.transact(38, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readInt() != 0;
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public String[] getAppOpPermissionPackages(String permissionName) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(permissionName);
                    this.mRemote.transact(39, _data, _reply, 0);
                    _reply.readException();
                    String[] _result = _reply.createStringArray();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public ResolveInfo resolveIntent(Intent intent, String resolvedType, int flags, int userId) throws RemoteException {
                ResolveInfo resolveInfoCreateFromParcel;
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (intent != null) {
                        _data.writeInt(1);
                        intent.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeString(resolvedType);
                    _data.writeInt(flags);
                    _data.writeInt(userId);
                    this.mRemote.transact(40, _data, _reply, 0);
                    _reply.readException();
                    if (_reply.readInt() != 0) {
                        resolveInfoCreateFromParcel = ResolveInfo.CREATOR.createFromParcel(_reply);
                    } else {
                        resolveInfoCreateFromParcel = null;
                    }
                    return resolveInfoCreateFromParcel;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public boolean canForwardTo(Intent intent, String resolvedType, int sourceUserId, int targetUserId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (intent != null) {
                        _data.writeInt(1);
                        intent.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeString(resolvedType);
                    _data.writeInt(sourceUserId);
                    _data.writeInt(targetUserId);
                    this.mRemote.transact(41, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readInt() != 0;
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public ParceledListSlice queryIntentActivities(Intent intent, String resolvedType, int flags, int userId) throws RemoteException {
                ParceledListSlice parceledListSliceCreateFromParcel;
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (intent != null) {
                        _data.writeInt(1);
                        intent.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeString(resolvedType);
                    _data.writeInt(flags);
                    _data.writeInt(userId);
                    this.mRemote.transact(42, _data, _reply, 0);
                    _reply.readException();
                    if (_reply.readInt() != 0) {
                        parceledListSliceCreateFromParcel = ParceledListSlice.CREATOR.createFromParcel(_reply);
                    } else {
                        parceledListSliceCreateFromParcel = null;
                    }
                    return parceledListSliceCreateFromParcel;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public ParceledListSlice queryIntentActivityOptions(ComponentName caller, Intent[] specifics, String[] specificTypes, Intent intent, String resolvedType, int flags, int userId) throws RemoteException {
                ParceledListSlice parceledListSliceCreateFromParcel;
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (caller != null) {
                        _data.writeInt(1);
                        caller.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeTypedArray(specifics, 0);
                    _data.writeStringArray(specificTypes);
                    if (intent != null) {
                        _data.writeInt(1);
                        intent.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeString(resolvedType);
                    _data.writeInt(flags);
                    _data.writeInt(userId);
                    this.mRemote.transact(43, _data, _reply, 0);
                    _reply.readException();
                    if (_reply.readInt() != 0) {
                        parceledListSliceCreateFromParcel = ParceledListSlice.CREATOR.createFromParcel(_reply);
                    } else {
                        parceledListSliceCreateFromParcel = null;
                    }
                    return parceledListSliceCreateFromParcel;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public ParceledListSlice queryIntentReceivers(Intent intent, String resolvedType, int flags, int userId) throws RemoteException {
                ParceledListSlice parceledListSliceCreateFromParcel;
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (intent != null) {
                        _data.writeInt(1);
                        intent.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeString(resolvedType);
                    _data.writeInt(flags);
                    _data.writeInt(userId);
                    this.mRemote.transact(44, _data, _reply, 0);
                    _reply.readException();
                    if (_reply.readInt() != 0) {
                        parceledListSliceCreateFromParcel = ParceledListSlice.CREATOR.createFromParcel(_reply);
                    } else {
                        parceledListSliceCreateFromParcel = null;
                    }
                    return parceledListSliceCreateFromParcel;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public ResolveInfo resolveService(Intent intent, String resolvedType, int flags, int userId) throws RemoteException {
                ResolveInfo resolveInfoCreateFromParcel;
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (intent != null) {
                        _data.writeInt(1);
                        intent.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeString(resolvedType);
                    _data.writeInt(flags);
                    _data.writeInt(userId);
                    this.mRemote.transact(45, _data, _reply, 0);
                    _reply.readException();
                    if (_reply.readInt() != 0) {
                        resolveInfoCreateFromParcel = ResolveInfo.CREATOR.createFromParcel(_reply);
                    } else {
                        resolveInfoCreateFromParcel = null;
                    }
                    return resolveInfoCreateFromParcel;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public ParceledListSlice queryIntentServices(Intent intent, String resolvedType, int flags, int userId) throws RemoteException {
                ParceledListSlice parceledListSliceCreateFromParcel;
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (intent != null) {
                        _data.writeInt(1);
                        intent.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeString(resolvedType);
                    _data.writeInt(flags);
                    _data.writeInt(userId);
                    this.mRemote.transact(46, _data, _reply, 0);
                    _reply.readException();
                    if (_reply.readInt() != 0) {
                        parceledListSliceCreateFromParcel = ParceledListSlice.CREATOR.createFromParcel(_reply);
                    } else {
                        parceledListSliceCreateFromParcel = null;
                    }
                    return parceledListSliceCreateFromParcel;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public ParceledListSlice queryIntentContentProviders(Intent intent, String resolvedType, int flags, int userId) throws RemoteException {
                ParceledListSlice parceledListSliceCreateFromParcel;
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (intent != null) {
                        _data.writeInt(1);
                        intent.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeString(resolvedType);
                    _data.writeInt(flags);
                    _data.writeInt(userId);
                    this.mRemote.transact(47, _data, _reply, 0);
                    _reply.readException();
                    if (_reply.readInt() != 0) {
                        parceledListSliceCreateFromParcel = ParceledListSlice.CREATOR.createFromParcel(_reply);
                    } else {
                        parceledListSliceCreateFromParcel = null;
                    }
                    return parceledListSliceCreateFromParcel;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public ParceledListSlice getInstalledPackages(int flags, int userId) throws RemoteException {
                ParceledListSlice parceledListSliceCreateFromParcel;
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(flags);
                    _data.writeInt(userId);
                    this.mRemote.transact(48, _data, _reply, 0);
                    _reply.readException();
                    if (_reply.readInt() != 0) {
                        parceledListSliceCreateFromParcel = ParceledListSlice.CREATOR.createFromParcel(_reply);
                    } else {
                        parceledListSliceCreateFromParcel = null;
                    }
                    return parceledListSliceCreateFromParcel;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public ParceledListSlice getPackagesHoldingPermissions(String[] permissions, int flags, int userId) throws RemoteException {
                ParceledListSlice parceledListSliceCreateFromParcel;
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStringArray(permissions);
                    _data.writeInt(flags);
                    _data.writeInt(userId);
                    this.mRemote.transact(49, _data, _reply, 0);
                    _reply.readException();
                    if (_reply.readInt() != 0) {
                        parceledListSliceCreateFromParcel = ParceledListSlice.CREATOR.createFromParcel(_reply);
                    } else {
                        parceledListSliceCreateFromParcel = null;
                    }
                    return parceledListSliceCreateFromParcel;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public ParceledListSlice getInstalledApplications(int flags, int userId) throws RemoteException {
                ParceledListSlice parceledListSliceCreateFromParcel;
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(flags);
                    _data.writeInt(userId);
                    this.mRemote.transact(50, _data, _reply, 0);
                    _reply.readException();
                    if (_reply.readInt() != 0) {
                        parceledListSliceCreateFromParcel = ParceledListSlice.CREATOR.createFromParcel(_reply);
                    } else {
                        parceledListSliceCreateFromParcel = null;
                    }
                    return parceledListSliceCreateFromParcel;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public ParceledListSlice getPersistentApplications(int flags) throws RemoteException {
                ParceledListSlice parceledListSliceCreateFromParcel;
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(flags);
                    this.mRemote.transact(51, _data, _reply, 0);
                    _reply.readException();
                    if (_reply.readInt() != 0) {
                        parceledListSliceCreateFromParcel = ParceledListSlice.CREATOR.createFromParcel(_reply);
                    } else {
                        parceledListSliceCreateFromParcel = null;
                    }
                    return parceledListSliceCreateFromParcel;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public ProviderInfo resolveContentProvider(String name, int flags, int userId) throws RemoteException {
                ProviderInfo providerInfoCreateFromParcel;
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(name);
                    _data.writeInt(flags);
                    _data.writeInt(userId);
                    this.mRemote.transact(52, _data, _reply, 0);
                    _reply.readException();
                    if (_reply.readInt() != 0) {
                        providerInfoCreateFromParcel = ProviderInfo.CREATOR.createFromParcel(_reply);
                    } else {
                        providerInfoCreateFromParcel = null;
                    }
                    return providerInfoCreateFromParcel;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void querySyncProviders(List<String> outNames, List<ProviderInfo> outInfo) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStringList(outNames);
                    _data.writeTypedList(outInfo);
                    this.mRemote.transact(53, _data, _reply, 0);
                    _reply.readException();
                    _reply.readStringList(outNames);
                    _reply.readTypedList(outInfo, ProviderInfo.CREATOR);
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public ParceledListSlice queryContentProviders(String processName, int uid, int flags) throws RemoteException {
                ParceledListSlice parceledListSliceCreateFromParcel;
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(processName);
                    _data.writeInt(uid);
                    _data.writeInt(flags);
                    this.mRemote.transact(54, _data, _reply, 0);
                    _reply.readException();
                    if (_reply.readInt() != 0) {
                        parceledListSliceCreateFromParcel = ParceledListSlice.CREATOR.createFromParcel(_reply);
                    } else {
                        parceledListSliceCreateFromParcel = null;
                    }
                    return parceledListSliceCreateFromParcel;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public InstrumentationInfo getInstrumentationInfo(ComponentName className, int flags) throws RemoteException {
                InstrumentationInfo instrumentationInfoCreateFromParcel;
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (className != null) {
                        _data.writeInt(1);
                        className.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeInt(flags);
                    this.mRemote.transact(55, _data, _reply, 0);
                    _reply.readException();
                    if (_reply.readInt() != 0) {
                        instrumentationInfoCreateFromParcel = InstrumentationInfo.CREATOR.createFromParcel(_reply);
                    } else {
                        instrumentationInfoCreateFromParcel = null;
                    }
                    return instrumentationInfoCreateFromParcel;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public ParceledListSlice queryInstrumentation(String targetPackage, int flags) throws RemoteException {
                ParceledListSlice parceledListSliceCreateFromParcel;
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(targetPackage);
                    _data.writeInt(flags);
                    this.mRemote.transact(56, _data, _reply, 0);
                    _reply.readException();
                    if (_reply.readInt() != 0) {
                        parceledListSliceCreateFromParcel = ParceledListSlice.CREATOR.createFromParcel(_reply);
                    } else {
                        parceledListSliceCreateFromParcel = null;
                    }
                    return parceledListSliceCreateFromParcel;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void installPackageAsUser(String originPath, IPackageInstallObserver2 observer, int flags, String installerPackageName, int userId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(originPath);
                    _data.writeStrongBinder(observer != null ? observer.asBinder() : null);
                    _data.writeInt(flags);
                    _data.writeString(installerPackageName);
                    _data.writeInt(userId);
                    this.mRemote.transact(57, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void finishPackageInstall(int token, boolean didLaunch) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(token);
                    _data.writeInt(didLaunch ? 1 : 0);
                    this.mRemote.transact(58, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void setInstallerPackageName(String targetPackage, String installerPackageName) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(targetPackage);
                    _data.writeString(installerPackageName);
                    this.mRemote.transact(59, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void deletePackageAsUser(String packageName, IPackageDeleteObserver observer, int userId, int flags) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(packageName);
                    _data.writeStrongBinder(observer != null ? observer.asBinder() : null);
                    _data.writeInt(userId);
                    _data.writeInt(flags);
                    this.mRemote.transact(60, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void deletePackage(String packageName, IPackageDeleteObserver2 observer, int userId, int flags) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(packageName);
                    _data.writeStrongBinder(observer != null ? observer.asBinder() : null);
                    _data.writeInt(userId);
                    _data.writeInt(flags);
                    this.mRemote.transact(61, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public String getInstallerPackageName(String packageName) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(packageName);
                    this.mRemote.transact(62, _data, _reply, 0);
                    _reply.readException();
                    String _result = _reply.readString();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void resetApplicationPreferences(int userId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(userId);
                    this.mRemote.transact(63, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public ResolveInfo getLastChosenActivity(Intent intent, String resolvedType, int flags) throws RemoteException {
                ResolveInfo resolveInfoCreateFromParcel;
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (intent != null) {
                        _data.writeInt(1);
                        intent.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeString(resolvedType);
                    _data.writeInt(flags);
                    this.mRemote.transact(64, _data, _reply, 0);
                    _reply.readException();
                    if (_reply.readInt() != 0) {
                        resolveInfoCreateFromParcel = ResolveInfo.CREATOR.createFromParcel(_reply);
                    } else {
                        resolveInfoCreateFromParcel = null;
                    }
                    return resolveInfoCreateFromParcel;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void setLastChosenActivity(Intent intent, String resolvedType, int flags, IntentFilter filter, int match, ComponentName activity) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (intent != null) {
                        _data.writeInt(1);
                        intent.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeString(resolvedType);
                    _data.writeInt(flags);
                    if (filter != null) {
                        _data.writeInt(1);
                        filter.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeInt(match);
                    if (activity != null) {
                        _data.writeInt(1);
                        activity.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(65, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void addPreferredActivity(IntentFilter filter, int match, ComponentName[] set, ComponentName activity, int userId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (filter != null) {
                        _data.writeInt(1);
                        filter.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeInt(match);
                    _data.writeTypedArray(set, 0);
                    if (activity != null) {
                        _data.writeInt(1);
                        activity.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeInt(userId);
                    this.mRemote.transact(66, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void replacePreferredActivity(IntentFilter filter, int match, ComponentName[] set, ComponentName activity, int userId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (filter != null) {
                        _data.writeInt(1);
                        filter.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeInt(match);
                    _data.writeTypedArray(set, 0);
                    if (activity != null) {
                        _data.writeInt(1);
                        activity.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeInt(userId);
                    this.mRemote.transact(67, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void clearPackagePreferredActivities(String packageName) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(packageName);
                    this.mRemote.transact(68, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public int getPreferredActivities(List<IntentFilter> outFilters, List<ComponentName> outActivities, String packageName) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(packageName);
                    this.mRemote.transact(69, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    _reply.readTypedList(outFilters, IntentFilter.CREATOR);
                    _reply.readTypedList(outActivities, ComponentName.CREATOR);
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void addPersistentPreferredActivity(IntentFilter filter, ComponentName activity, int userId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (filter != null) {
                        _data.writeInt(1);
                        filter.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    if (activity != null) {
                        _data.writeInt(1);
                        activity.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeInt(userId);
                    this.mRemote.transact(70, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void clearPackagePersistentPreferredActivities(String packageName, int userId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(packageName);
                    _data.writeInt(userId);
                    this.mRemote.transact(71, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void addCrossProfileIntentFilter(IntentFilter intentFilter, String ownerPackage, int sourceUserId, int targetUserId, int flags) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (intentFilter != null) {
                        _data.writeInt(1);
                        intentFilter.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeString(ownerPackage);
                    _data.writeInt(sourceUserId);
                    _data.writeInt(targetUserId);
                    _data.writeInt(flags);
                    this.mRemote.transact(72, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void clearCrossProfileIntentFilters(int sourceUserId, String ownerPackage) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(sourceUserId);
                    _data.writeString(ownerPackage);
                    this.mRemote.transact(73, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public String[] setPackagesSuspendedAsUser(String[] packageNames, boolean suspended, int userId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStringArray(packageNames);
                    _data.writeInt(suspended ? 1 : 0);
                    _data.writeInt(userId);
                    this.mRemote.transact(74, _data, _reply, 0);
                    _reply.readException();
                    String[] _result = _reply.createStringArray();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public boolean isPackageSuspendedForUser(String packageName, int userId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(packageName);
                    _data.writeInt(userId);
                    this.mRemote.transact(75, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readInt() != 0;
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public byte[] getPreferredActivityBackup(int userId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(userId);
                    this.mRemote.transact(76, _data, _reply, 0);
                    _reply.readException();
                    byte[] _result = _reply.createByteArray();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void restorePreferredActivities(byte[] backup, int userId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeByteArray(backup);
                    _data.writeInt(userId);
                    this.mRemote.transact(77, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public byte[] getDefaultAppsBackup(int userId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(userId);
                    this.mRemote.transact(78, _data, _reply, 0);
                    _reply.readException();
                    byte[] _result = _reply.createByteArray();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void restoreDefaultApps(byte[] backup, int userId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeByteArray(backup);
                    _data.writeInt(userId);
                    this.mRemote.transact(79, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public byte[] getIntentFilterVerificationBackup(int userId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(userId);
                    this.mRemote.transact(80, _data, _reply, 0);
                    _reply.readException();
                    byte[] _result = _reply.createByteArray();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void restoreIntentFilterVerification(byte[] backup, int userId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeByteArray(backup);
                    _data.writeInt(userId);
                    this.mRemote.transact(81, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public byte[] getPermissionGrantBackup(int userId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(userId);
                    this.mRemote.transact(82, _data, _reply, 0);
                    _reply.readException();
                    byte[] _result = _reply.createByteArray();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void restorePermissionGrants(byte[] backup, int userId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeByteArray(backup);
                    _data.writeInt(userId);
                    this.mRemote.transact(83, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public ComponentName getHomeActivities(List<ResolveInfo> outHomeCandidates) throws RemoteException {
                ComponentName componentNameCreateFromParcel;
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(84, _data, _reply, 0);
                    _reply.readException();
                    if (_reply.readInt() != 0) {
                        componentNameCreateFromParcel = ComponentName.CREATOR.createFromParcel(_reply);
                    } else {
                        componentNameCreateFromParcel = null;
                    }
                    _reply.readTypedList(outHomeCandidates, ResolveInfo.CREATOR);
                    return componentNameCreateFromParcel;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void setHomeActivity(ComponentName className, int userId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (className != null) {
                        _data.writeInt(1);
                        className.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeInt(userId);
                    this.mRemote.transact(85, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void setComponentEnabledSetting(ComponentName componentName, int newState, int flags, int userId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (componentName != null) {
                        _data.writeInt(1);
                        componentName.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeInt(newState);
                    _data.writeInt(flags);
                    _data.writeInt(userId);
                    this.mRemote.transact(86, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public int getComponentEnabledSetting(ComponentName componentName, int userId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (componentName != null) {
                        _data.writeInt(1);
                        componentName.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeInt(userId);
                    this.mRemote.transact(87, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void setApplicationEnabledSetting(String packageName, int newState, int flags, int userId, String callingPackage) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(packageName);
                    _data.writeInt(newState);
                    _data.writeInt(flags);
                    _data.writeInt(userId);
                    _data.writeString(callingPackage);
                    this.mRemote.transact(88, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public int getApplicationEnabledSetting(String packageName, int userId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(packageName);
                    _data.writeInt(userId);
                    this.mRemote.transact(89, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void logAppProcessStartIfNeeded(String processName, int uid, String seinfo, String apkFile, int pid) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(processName);
                    _data.writeInt(uid);
                    _data.writeString(seinfo);
                    _data.writeString(apkFile);
                    _data.writeInt(pid);
                    this.mRemote.transact(90, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void flushPackageRestrictionsAsUser(int userId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(userId);
                    this.mRemote.transact(91, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void setPackageStoppedState(String packageName, boolean stopped, int userId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(packageName);
                    _data.writeInt(stopped ? 1 : 0);
                    _data.writeInt(userId);
                    this.mRemote.transact(92, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void freeStorageAndNotify(String volumeUuid, long freeStorageSize, IPackageDataObserver observer) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(volumeUuid);
                    _data.writeLong(freeStorageSize);
                    _data.writeStrongBinder(observer != null ? observer.asBinder() : null);
                    this.mRemote.transact(93, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void freeStorage(String volumeUuid, long freeStorageSize, IntentSender pi) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(volumeUuid);
                    _data.writeLong(freeStorageSize);
                    if (pi != null) {
                        _data.writeInt(1);
                        pi.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(94, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void deleteApplicationCacheFiles(String packageName, IPackageDataObserver observer) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(packageName);
                    _data.writeStrongBinder(observer != null ? observer.asBinder() : null);
                    this.mRemote.transact(95, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void deleteApplicationCacheFilesAsUser(String packageName, int userId, IPackageDataObserver observer) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(packageName);
                    _data.writeInt(userId);
                    _data.writeStrongBinder(observer != null ? observer.asBinder() : null);
                    this.mRemote.transact(96, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void clearApplicationUserData(String packageName, IPackageDataObserver observer, int userId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(packageName);
                    _data.writeStrongBinder(observer != null ? observer.asBinder() : null);
                    _data.writeInt(userId);
                    this.mRemote.transact(97, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void clearApplicationProfileData(String packageName) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(packageName);
                    this.mRemote.transact(98, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void getPackageSizeInfo(String packageName, int userHandle, IPackageStatsObserver observer) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(packageName);
                    _data.writeInt(userHandle);
                    _data.writeStrongBinder(observer != null ? observer.asBinder() : null);
                    this.mRemote.transact(99, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public String[] getSystemSharedLibraryNames() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(100, _data, _reply, 0);
                    _reply.readException();
                    String[] _result = _reply.createStringArray();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public ParceledListSlice getSystemAvailableFeatures() throws RemoteException {
                ParceledListSlice parceledListSliceCreateFromParcel;
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(101, _data, _reply, 0);
                    _reply.readException();
                    if (_reply.readInt() != 0) {
                        parceledListSliceCreateFromParcel = ParceledListSlice.CREATOR.createFromParcel(_reply);
                    } else {
                        parceledListSliceCreateFromParcel = null;
                    }
                    return parceledListSliceCreateFromParcel;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public boolean hasSystemFeature(String name, int version) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(name);
                    _data.writeInt(version);
                    this.mRemote.transact(102, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readInt() != 0;
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void enterSafeMode() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(103, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public boolean isSafeMode() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(104, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readInt() != 0;
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void systemReady() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(105, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public boolean hasSystemUidErrors() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(106, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readInt() != 0;
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void performFstrimIfNeeded() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(107, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void updatePackagesIfNeeded() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(108, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void notifyPackageUse(String packageName, int reason) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(packageName);
                    _data.writeInt(reason);
                    this.mRemote.transact(109, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public boolean performDexOptIfNeeded(String packageName) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(packageName);
                    this.mRemote.transact(110, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readInt() != 0;
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public boolean performDexOpt(String packageName, boolean checkProfiles, int compileReason, boolean force) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(packageName);
                    _data.writeInt(checkProfiles ? 1 : 0);
                    _data.writeInt(compileReason);
                    _data.writeInt(force ? 1 : 0);
                    this.mRemote.transact(111, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readInt() != 0;
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public boolean performDexOptMode(String packageName, boolean checkProfiles, String targetCompilerFilter, boolean force) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(packageName);
                    _data.writeInt(checkProfiles ? 1 : 0);
                    _data.writeString(targetCompilerFilter);
                    _data.writeInt(force ? 1 : 0);
                    this.mRemote.transact(112, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readInt() != 0;
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void dumpProfiles(String packageName) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(packageName);
                    this.mRemote.transact(113, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void forceDexOpt(String packageName) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(packageName);
                    this.mRemote.transact(114, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void updateExternalMediaStatus(boolean mounted, boolean reportStatus) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(mounted ? 1 : 0);
                    _data.writeInt(reportStatus ? 1 : 0);
                    this.mRemote.transact(115, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public PackageCleanItem nextPackageToClean(PackageCleanItem lastPackage) throws RemoteException {
                PackageCleanItem packageCleanItemCreateFromParcel;
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (lastPackage != null) {
                        _data.writeInt(1);
                        lastPackage.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(116, _data, _reply, 0);
                    _reply.readException();
                    if (_reply.readInt() != 0) {
                        packageCleanItemCreateFromParcel = PackageCleanItem.CREATOR.createFromParcel(_reply);
                    } else {
                        packageCleanItemCreateFromParcel = null;
                    }
                    return packageCleanItemCreateFromParcel;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public int getMoveStatus(int moveId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(moveId);
                    this.mRemote.transact(117, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void registerMoveCallback(IPackageMoveObserver callback) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(callback != null ? callback.asBinder() : null);
                    this.mRemote.transact(118, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void unregisterMoveCallback(IPackageMoveObserver callback) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(callback != null ? callback.asBinder() : null);
                    this.mRemote.transact(119, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public int movePackage(String packageName, String volumeUuid) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(packageName);
                    _data.writeString(volumeUuid);
                    this.mRemote.transact(120, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public int movePrimaryStorage(String volumeUuid) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(volumeUuid);
                    this.mRemote.transact(121, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public boolean addPermissionAsync(PermissionInfo info) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (info != null) {
                        _data.writeInt(1);
                        info.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(122, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readInt() != 0;
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public boolean setInstallLocation(int loc) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(loc);
                    this.mRemote.transact(123, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readInt() != 0;
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public int getInstallLocation() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(124, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public int installExistingPackageAsUser(String packageName, int userId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(packageName);
                    _data.writeInt(userId);
                    this.mRemote.transact(125, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void verifyPendingInstall(int id, int verificationCode) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(id);
                    _data.writeInt(verificationCode);
                    this.mRemote.transact(126, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void extendVerificationTimeout(int id, int verificationCodeAtTimeout, long millisecondsToDelay) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(id);
                    _data.writeInt(verificationCodeAtTimeout);
                    _data.writeLong(millisecondsToDelay);
                    this.mRemote.transact(127, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void verifyIntentFilter(int id, int verificationCode, List<String> failedDomains) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(id);
                    _data.writeInt(verificationCode);
                    _data.writeStringList(failedDomains);
                    this.mRemote.transact(128, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public int getIntentVerificationStatus(String packageName, int userId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(packageName);
                    _data.writeInt(userId);
                    this.mRemote.transact(129, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public boolean updateIntentVerificationStatus(String packageName, int status, int userId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(packageName);
                    _data.writeInt(status);
                    _data.writeInt(userId);
                    this.mRemote.transact(130, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readInt() != 0;
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public ParceledListSlice getIntentFilterVerifications(String packageName) throws RemoteException {
                ParceledListSlice parceledListSliceCreateFromParcel;
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(packageName);
                    this.mRemote.transact(131, _data, _reply, 0);
                    _reply.readException();
                    if (_reply.readInt() != 0) {
                        parceledListSliceCreateFromParcel = ParceledListSlice.CREATOR.createFromParcel(_reply);
                    } else {
                        parceledListSliceCreateFromParcel = null;
                    }
                    return parceledListSliceCreateFromParcel;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public ParceledListSlice getAllIntentFilters(String packageName) throws RemoteException {
                ParceledListSlice parceledListSliceCreateFromParcel;
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(packageName);
                    this.mRemote.transact(132, _data, _reply, 0);
                    _reply.readException();
                    if (_reply.readInt() != 0) {
                        parceledListSliceCreateFromParcel = ParceledListSlice.CREATOR.createFromParcel(_reply);
                    } else {
                        parceledListSliceCreateFromParcel = null;
                    }
                    return parceledListSliceCreateFromParcel;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public boolean setDefaultBrowserPackageName(String packageName, int userId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(packageName);
                    _data.writeInt(userId);
                    this.mRemote.transact(133, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readInt() != 0;
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public String getDefaultBrowserPackageName(int userId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(userId);
                    this.mRemote.transact(134, _data, _reply, 0);
                    _reply.readException();
                    String _result = _reply.readString();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public VerifierDeviceIdentity getVerifierDeviceIdentity() throws RemoteException {
                VerifierDeviceIdentity verifierDeviceIdentityCreateFromParcel;
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(135, _data, _reply, 0);
                    _reply.readException();
                    if (_reply.readInt() != 0) {
                        verifierDeviceIdentityCreateFromParcel = VerifierDeviceIdentity.CREATOR.createFromParcel(_reply);
                    } else {
                        verifierDeviceIdentityCreateFromParcel = null;
                    }
                    return verifierDeviceIdentityCreateFromParcel;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public boolean isFirstBoot() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(136, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readInt() != 0;
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public boolean isOnlyCoreApps() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(137, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readInt() != 0;
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public boolean isUpgrade() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(138, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readInt() != 0;
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public int checkAPKSignatures(String pkg) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(pkg);
                    this.mRemote.transact(139, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void setPermissionEnforced(String permission, boolean enforced) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(permission);
                    _data.writeInt(enforced ? 1 : 0);
                    this.mRemote.transact(140, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public boolean isPermissionEnforced(String permission) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(permission);
                    this.mRemote.transact(141, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readInt() != 0;
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public boolean isStorageLow() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(142, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readInt() != 0;
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public boolean setApplicationHiddenSettingAsUser(String packageName, boolean hidden, int userId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(packageName);
                    _data.writeInt(hidden ? 1 : 0);
                    _data.writeInt(userId);
                    this.mRemote.transact(143, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readInt() != 0;
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public boolean getApplicationHiddenSettingAsUser(String packageName, int userId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(packageName);
                    _data.writeInt(userId);
                    this.mRemote.transact(144, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readInt() != 0;
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public IPackageInstaller getPackageInstaller() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(145, _data, _reply, 0);
                    _reply.readException();
                    IPackageInstaller _result = IPackageInstaller.Stub.asInterface(_reply.readStrongBinder());
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public boolean setBlockUninstallForUser(String packageName, boolean blockUninstall, int userId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(packageName);
                    _data.writeInt(blockUninstall ? 1 : 0);
                    _data.writeInt(userId);
                    this.mRemote.transact(146, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readInt() != 0;
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public boolean getBlockUninstallForUser(String packageName, int userId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(packageName);
                    _data.writeInt(userId);
                    this.mRemote.transact(147, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readInt() != 0;
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public KeySet getKeySetByAlias(String packageName, String alias) throws RemoteException {
                KeySet keySetCreateFromParcel;
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(packageName);
                    _data.writeString(alias);
                    this.mRemote.transact(148, _data, _reply, 0);
                    _reply.readException();
                    if (_reply.readInt() != 0) {
                        keySetCreateFromParcel = KeySet.CREATOR.createFromParcel(_reply);
                    } else {
                        keySetCreateFromParcel = null;
                    }
                    return keySetCreateFromParcel;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public KeySet getSigningKeySet(String packageName) throws RemoteException {
                KeySet keySetCreateFromParcel;
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(packageName);
                    this.mRemote.transact(149, _data, _reply, 0);
                    _reply.readException();
                    if (_reply.readInt() != 0) {
                        keySetCreateFromParcel = KeySet.CREATOR.createFromParcel(_reply);
                    } else {
                        keySetCreateFromParcel = null;
                    }
                    return keySetCreateFromParcel;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public boolean isPackageSignedByKeySet(String packageName, KeySet ks) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(packageName);
                    if (ks != null) {
                        _data.writeInt(1);
                        ks.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(150, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readInt() != 0;
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public boolean isPackageSignedByKeySetExactly(String packageName, KeySet ks) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(packageName);
                    if (ks != null) {
                        _data.writeInt(1);
                        ks.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(151, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readInt() != 0;
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void addOnPermissionsChangeListener(IOnPermissionsChangeListener listener) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(listener != null ? listener.asBinder() : null);
                    this.mRemote.transact(152, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void removeOnPermissionsChangeListener(IOnPermissionsChangeListener listener) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(listener != null ? listener.asBinder() : null);
                    this.mRemote.transact(153, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void grantDefaultPermissionsToEnabledCarrierApps(String[] packageNames, int userId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStringArray(packageNames);
                    _data.writeInt(userId);
                    this.mRemote.transact(154, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public boolean isPermissionRevokedByPolicy(String permission, String packageName, int userId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(permission);
                    _data.writeString(packageName);
                    _data.writeInt(userId);
                    this.mRemote.transact(155, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readInt() != 0;
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public String getPermissionControllerPackageName() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(156, _data, _reply, 0);
                    _reply.readException();
                    String _result = _reply.readString();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public ParceledListSlice getEphemeralApplications(int userId) throws RemoteException {
                ParceledListSlice parceledListSliceCreateFromParcel;
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(userId);
                    this.mRemote.transact(157, _data, _reply, 0);
                    _reply.readException();
                    if (_reply.readInt() != 0) {
                        parceledListSliceCreateFromParcel = ParceledListSlice.CREATOR.createFromParcel(_reply);
                    } else {
                        parceledListSliceCreateFromParcel = null;
                    }
                    return parceledListSliceCreateFromParcel;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public byte[] getEphemeralApplicationCookie(String packageName, int userId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(packageName);
                    _data.writeInt(userId);
                    this.mRemote.transact(158, _data, _reply, 0);
                    _reply.readException();
                    byte[] _result = _reply.createByteArray();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public boolean setEphemeralApplicationCookie(String packageName, byte[] cookie, int userId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(packageName);
                    _data.writeByteArray(cookie);
                    _data.writeInt(userId);
                    this.mRemote.transact(159, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readInt() != 0;
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public Bitmap getEphemeralApplicationIcon(String packageName, int userId) throws RemoteException {
                Bitmap bitmapCreateFromParcel;
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(packageName);
                    _data.writeInt(userId);
                    this.mRemote.transact(160, _data, _reply, 0);
                    _reply.readException();
                    if (_reply.readInt() != 0) {
                        bitmapCreateFromParcel = Bitmap.CREATOR.createFromParcel(_reply);
                    } else {
                        bitmapCreateFromParcel = null;
                    }
                    return bitmapCreateFromParcel;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public boolean isEphemeralApplication(String packageName, int userId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(packageName);
                    _data.writeInt(userId);
                    this.mRemote.transact(161, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readInt() != 0;
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public boolean setRequiredForSystemUser(String packageName, boolean systemUserApp) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(packageName);
                    _data.writeInt(systemUserApp ? 1 : 0);
                    this.mRemote.transact(162, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readInt() != 0;
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public String getServicesSystemSharedLibraryPackageName() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(163, _data, _reply, 0);
                    _reply.readException();
                    String _result = _reply.readString();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public String getSharedSystemSharedLibraryPackageName() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(164, _data, _reply, 0);
                    _reply.readException();
                    String _result = _reply.readString();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public boolean isPackageDeviceAdminOnAnyUser(String packageName) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(packageName);
                    this.mRemote.transact(165, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readInt() != 0;
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public List<String> getPreviousCodePaths(String packageName) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(packageName);
                    this.mRemote.transact(166, _data, _reply, 0);
                    _reply.readException();
                    List<String> _result = _reply.createStringArrayList();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public List<String> getPermRecordPkgs() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(167, _data, _reply, 0);
                    _reply.readException();
                    List<String> _result = _reply.createStringArrayList();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public List<String> getPermRecordPerms(String packageName) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(packageName);
                    this.mRemote.transact(168, _data, _reply, 0);
                    _reply.readException();
                    List<String> _result = _reply.createStringArrayList();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public PermissionRecords getPermRecords(String packageName, String permName) throws RemoteException {
                PermissionRecords permissionRecordsCreateFromParcel;
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(packageName);
                    _data.writeString(permName);
                    this.mRemote.transact(169, _data, _reply, 0);
                    _reply.readException();
                    if (_reply.readInt() != 0) {
                        permissionRecordsCreateFromParcel = PermissionRecords.CREATOR.createFromParcel(_reply);
                    } else {
                        permissionRecordsCreateFromParcel = null;
                    }
                    return permissionRecordsCreateFromParcel;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }
        }
    }
}
