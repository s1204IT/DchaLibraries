package android.app.admin;

import android.content.ComponentName;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ParceledListSlice;
import android.graphics.Bitmap;
import android.net.ProxyInfo;
import android.net.Uri;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.PersistableBundle;
import android.os.RemoteCallback;
import android.os.RemoteException;
import android.os.UserHandle;
import android.text.TextUtils;
import java.util.List;

public interface IDevicePolicyManager extends IInterface {
    void addCrossProfileIntentFilter(ComponentName componentName, IntentFilter intentFilter, int i) throws RemoteException;

    boolean addCrossProfileWidgetProvider(ComponentName componentName, String str) throws RemoteException;

    void addPersistentPreferredActivity(ComponentName componentName, IntentFilter intentFilter, ComponentName componentName2) throws RemoteException;

    boolean approveCaCert(String str, int i, boolean z) throws RemoteException;

    void choosePrivateKeyAlias(int i, Uri uri, String str, IBinder iBinder) throws RemoteException;

    void clearCrossProfileIntentFilters(ComponentName componentName) throws RemoteException;

    void clearDeviceOwner(String str) throws RemoteException;

    void clearPackagePersistentPreferredActivities(ComponentName componentName, String str) throws RemoteException;

    void clearProfileOwner(ComponentName componentName) throws RemoteException;

    UserHandle createAndManageUser(ComponentName componentName, String str, ComponentName componentName2, PersistableBundle persistableBundle, int i) throws RemoteException;

    void enableSystemApp(ComponentName componentName, String str) throws RemoteException;

    int enableSystemAppWithIntent(ComponentName componentName, Intent intent) throws RemoteException;

    void enforceCanManageCaCerts(ComponentName componentName) throws RemoteException;

    void forceRemoveActiveAdmin(ComponentName componentName, int i) throws RemoteException;

    String[] getAccountTypesWithManagementDisabled() throws RemoteException;

    String[] getAccountTypesWithManagementDisabledAsUser(int i) throws RemoteException;

    List<ComponentName> getActiveAdmins(int i) throws RemoteException;

    String getAlwaysOnVpnPackage(ComponentName componentName) throws RemoteException;

    Bundle getApplicationRestrictions(ComponentName componentName, String str) throws RemoteException;

    String getApplicationRestrictionsManagingPackage(ComponentName componentName) throws RemoteException;

    boolean getAutoTimeRequired() throws RemoteException;

    boolean getBluetoothContactSharingDisabled(ComponentName componentName) throws RemoteException;

    boolean getBluetoothContactSharingDisabledForUser(int i) throws RemoteException;

    boolean getCameraDisabled(ComponentName componentName, int i) throws RemoteException;

    String getCertInstallerPackage(ComponentName componentName) throws RemoteException;

    boolean getCrossProfileCallerIdDisabled(ComponentName componentName) throws RemoteException;

    boolean getCrossProfileCallerIdDisabledForUser(int i) throws RemoteException;

    boolean getCrossProfileContactsSearchDisabled(ComponentName componentName) throws RemoteException;

    boolean getCrossProfileContactsSearchDisabledForUser(int i) throws RemoteException;

    List<String> getCrossProfileWidgetProviders(ComponentName componentName) throws RemoteException;

    int getCurrentFailedPasswordAttempts(int i, boolean z) throws RemoteException;

    ComponentName getDeviceOwnerComponent(boolean z) throws RemoteException;

    CharSequence getDeviceOwnerLockScreenInfo() throws RemoteException;

    String getDeviceOwnerName() throws RemoteException;

    int getDeviceOwnerUserId() throws RemoteException;

    boolean getDoNotAskCredentialsOnBoot() throws RemoteException;

    boolean getForceEphemeralUsers(ComponentName componentName) throws RemoteException;

    ComponentName getGlobalProxyAdmin(int i) throws RemoteException;

    List<String> getKeepUninstalledPackages(ComponentName componentName) throws RemoteException;

    int getKeyguardDisabledFeatures(ComponentName componentName, int i, boolean z) throws RemoteException;

    String[] getLockTaskPackages(ComponentName componentName) throws RemoteException;

    CharSequence getLongSupportMessage(ComponentName componentName) throws RemoteException;

    CharSequence getLongSupportMessageForUser(ComponentName componentName, int i) throws RemoteException;

    int getMaximumFailedPasswordsForWipe(ComponentName componentName, int i, boolean z) throws RemoteException;

    long getMaximumTimeToLock(ComponentName componentName, int i, boolean z) throws RemoteException;

    long getMaximumTimeToLockForUserAndProfiles(int i) throws RemoteException;

    int getOrganizationColor(ComponentName componentName) throws RemoteException;

    int getOrganizationColorForUser(int i) throws RemoteException;

    CharSequence getOrganizationName(ComponentName componentName) throws RemoteException;

    CharSequence getOrganizationNameForUser(int i) throws RemoteException;

    long getPasswordExpiration(ComponentName componentName, int i, boolean z) throws RemoteException;

    long getPasswordExpirationTimeout(ComponentName componentName, int i, boolean z) throws RemoteException;

    int getPasswordHistoryLength(ComponentName componentName, int i, boolean z) throws RemoteException;

    int getPasswordMinimumLength(ComponentName componentName, int i, boolean z) throws RemoteException;

    int getPasswordMinimumLetters(ComponentName componentName, int i, boolean z) throws RemoteException;

    int getPasswordMinimumLowerCase(ComponentName componentName, int i, boolean z) throws RemoteException;

    int getPasswordMinimumNonLetter(ComponentName componentName, int i, boolean z) throws RemoteException;

    int getPasswordMinimumNumeric(ComponentName componentName, int i, boolean z) throws RemoteException;

    int getPasswordMinimumSymbols(ComponentName componentName, int i, boolean z) throws RemoteException;

    int getPasswordMinimumUpperCase(ComponentName componentName, int i, boolean z) throws RemoteException;

    int getPasswordQuality(ComponentName componentName, int i, boolean z) throws RemoteException;

    int getPermissionGrantState(ComponentName componentName, String str, String str2) throws RemoteException;

    int getPermissionPolicy(ComponentName componentName) throws RemoteException;

    List getPermittedAccessibilityServices(ComponentName componentName) throws RemoteException;

    List getPermittedAccessibilityServicesForUser(int i) throws RemoteException;

    List getPermittedInputMethods(ComponentName componentName) throws RemoteException;

    List getPermittedInputMethodsForCurrentUser() throws RemoteException;

    ComponentName getProfileOwner(int i) throws RemoteException;

    String getProfileOwnerName(int i) throws RemoteException;

    int getProfileWithMinimumFailedPasswordsForWipe(int i, boolean z) throws RemoteException;

    void getRemoveWarning(ComponentName componentName, RemoteCallback remoteCallback, int i) throws RemoteException;

    ComponentName getRestrictionsProvider(int i) throws RemoteException;

    boolean getScreenCaptureDisabled(ComponentName componentName, int i) throws RemoteException;

    CharSequence getShortSupportMessage(ComponentName componentName) throws RemoteException;

    CharSequence getShortSupportMessageForUser(ComponentName componentName, int i) throws RemoteException;

    boolean getStorageEncryption(ComponentName componentName, int i) throws RemoteException;

    int getStorageEncryptionStatus(String str, int i) throws RemoteException;

    SystemUpdatePolicy getSystemUpdatePolicy() throws RemoteException;

    List<PersistableBundle> getTrustAgentConfiguration(ComponentName componentName, ComponentName componentName2, int i, boolean z) throws RemoteException;

    int getUserProvisioningState() throws RemoteException;

    Bundle getUserRestrictions(ComponentName componentName) throws RemoteException;

    String getWifiMacAddress(ComponentName componentName) throws RemoteException;

    boolean hasGrantedPolicy(ComponentName componentName, int i, int i2) throws RemoteException;

    boolean hasUserSetupCompleted() throws RemoteException;

    boolean installCaCert(ComponentName componentName, byte[] bArr) throws RemoteException;

    boolean installKeyPair(ComponentName componentName, byte[] bArr, byte[] bArr2, byte[] bArr3, String str, boolean z) throws RemoteException;

    boolean isAccessibilityServicePermittedByAdmin(ComponentName componentName, String str, int i) throws RemoteException;

    boolean isActivePasswordSufficient(int i, boolean z) throws RemoteException;

    boolean isAdminActive(ComponentName componentName, int i) throws RemoteException;

    boolean isAffiliatedUser() throws RemoteException;

    boolean isApplicationHidden(ComponentName componentName, String str) throws RemoteException;

    boolean isCaCertApproved(String str, int i) throws RemoteException;

    boolean isCallerApplicationRestrictionsManagingPackage() throws RemoteException;

    boolean isInputMethodPermittedByAdmin(ComponentName componentName, String str, int i) throws RemoteException;

    boolean isLockTaskPermitted(String str) throws RemoteException;

    boolean isManagedProfile(ComponentName componentName) throws RemoteException;

    boolean isMasterVolumeMuted(ComponentName componentName) throws RemoteException;

    boolean isPackageSuspended(ComponentName componentName, String str) throws RemoteException;

    boolean isProfileActivePasswordSufficientForParent(int i) throws RemoteException;

    boolean isProvisioningAllowed(String str) throws RemoteException;

    boolean isRemovingAdmin(ComponentName componentName, int i) throws RemoteException;

    boolean isSecurityLoggingEnabled(ComponentName componentName) throws RemoteException;

    boolean isSeparateProfileChallengeAllowed(int i) throws RemoteException;

    boolean isSystemOnlyUser(ComponentName componentName) throws RemoteException;

    boolean isUninstallBlocked(ComponentName componentName, String str) throws RemoteException;

    boolean isUninstallInQueue(String str) throws RemoteException;

    void lockNow(boolean z) throws RemoteException;

    void notifyLockTaskModeChanged(boolean z, String str, int i) throws RemoteException;

    void notifyPendingSystemUpdate(long j) throws RemoteException;

    boolean packageHasActiveAdmins(String str, int i) throws RemoteException;

    void reboot(ComponentName componentName) throws RemoteException;

    void removeActiveAdmin(ComponentName componentName, int i) throws RemoteException;

    boolean removeCrossProfileWidgetProvider(ComponentName componentName, String str) throws RemoteException;

    boolean removeKeyPair(ComponentName componentName, String str) throws RemoteException;

    boolean removeUser(ComponentName componentName, UserHandle userHandle) throws RemoteException;

    void reportFailedFingerprintAttempt(int i) throws RemoteException;

    void reportFailedPasswordAttempt(int i) throws RemoteException;

    void reportKeyguardDismissed(int i) throws RemoteException;

    void reportKeyguardSecured(int i) throws RemoteException;

    void reportPasswordChanged(int i) throws RemoteException;

    void reportSuccessfulFingerprintAttempt(int i) throws RemoteException;

    void reportSuccessfulPasswordAttempt(int i) throws RemoteException;

    boolean requestBugreport(ComponentName componentName) throws RemoteException;

    boolean resetPassword(String str, int i) throws RemoteException;

    ParceledListSlice retrievePreRebootSecurityLogs(ComponentName componentName) throws RemoteException;

    ParceledListSlice retrieveSecurityLogs(ComponentName componentName) throws RemoteException;

    void setAccountManagementDisabled(ComponentName componentName, String str, boolean z) throws RemoteException;

    void setActiveAdmin(ComponentName componentName, boolean z, int i) throws RemoteException;

    void setActivePasswordState(int i, int i2, int i3, int i4, int i5, int i6, int i7, int i8, int i9) throws RemoteException;

    void setAffiliationIds(ComponentName componentName, List<String> list) throws RemoteException;

    boolean setAlwaysOnVpnPackage(ComponentName componentName, String str, boolean z) throws RemoteException;

    boolean setApplicationHidden(ComponentName componentName, String str, boolean z) throws RemoteException;

    void setApplicationRestrictions(ComponentName componentName, String str, Bundle bundle) throws RemoteException;

    boolean setApplicationRestrictionsManagingPackage(ComponentName componentName, String str) throws RemoteException;

    void setAutoTimeRequired(ComponentName componentName, boolean z) throws RemoteException;

    void setBluetoothContactSharingDisabled(ComponentName componentName, boolean z) throws RemoteException;

    void setCameraDisabled(ComponentName componentName, boolean z) throws RemoteException;

    void setCertInstallerPackage(ComponentName componentName, String str) throws RemoteException;

    void setCrossProfileCallerIdDisabled(ComponentName componentName, boolean z) throws RemoteException;

    void setCrossProfileContactsSearchDisabled(ComponentName componentName, boolean z) throws RemoteException;

    boolean setDeviceOwner(ComponentName componentName, String str, int i) throws RemoteException;

    void setDeviceOwnerLockScreenInfo(ComponentName componentName, CharSequence charSequence) throws RemoteException;

    void setForceEphemeralUsers(ComponentName componentName, boolean z) throws RemoteException;

    ComponentName setGlobalProxy(ComponentName componentName, String str, String str2) throws RemoteException;

    void setGlobalSetting(ComponentName componentName, String str, String str2) throws RemoteException;

    void setKeepUninstalledPackages(ComponentName componentName, List<String> list) throws RemoteException;

    boolean setKeyguardDisabled(ComponentName componentName, boolean z) throws RemoteException;

    void setKeyguardDisabledFeatures(ComponentName componentName, int i, boolean z) throws RemoteException;

    void setLockTaskPackages(ComponentName componentName, String[] strArr) throws RemoteException;

    void setLongSupportMessage(ComponentName componentName, CharSequence charSequence) throws RemoteException;

    void setMasterVolumeMuted(ComponentName componentName, boolean z) throws RemoteException;

    void setMaximumFailedPasswordsForWipe(ComponentName componentName, int i, boolean z) throws RemoteException;

    void setMaximumTimeToLock(ComponentName componentName, long j, boolean z) throws RemoteException;

    void setOrganizationColor(ComponentName componentName, int i) throws RemoteException;

    void setOrganizationColorForUser(int i, int i2) throws RemoteException;

    void setOrganizationName(ComponentName componentName, CharSequence charSequence) throws RemoteException;

    String[] setPackagesSuspended(ComponentName componentName, String[] strArr, boolean z) throws RemoteException;

    void setPasswordExpirationTimeout(ComponentName componentName, long j, boolean z) throws RemoteException;

    void setPasswordHistoryLength(ComponentName componentName, int i, boolean z) throws RemoteException;

    void setPasswordMinimumLength(ComponentName componentName, int i, boolean z) throws RemoteException;

    void setPasswordMinimumLetters(ComponentName componentName, int i, boolean z) throws RemoteException;

    void setPasswordMinimumLowerCase(ComponentName componentName, int i, boolean z) throws RemoteException;

    void setPasswordMinimumNonLetter(ComponentName componentName, int i, boolean z) throws RemoteException;

    void setPasswordMinimumNumeric(ComponentName componentName, int i, boolean z) throws RemoteException;

    void setPasswordMinimumSymbols(ComponentName componentName, int i, boolean z) throws RemoteException;

    void setPasswordMinimumUpperCase(ComponentName componentName, int i, boolean z) throws RemoteException;

    void setPasswordQuality(ComponentName componentName, int i, boolean z) throws RemoteException;

    boolean setPermissionGrantState(ComponentName componentName, String str, String str2, int i) throws RemoteException;

    void setPermissionPolicy(ComponentName componentName, int i) throws RemoteException;

    boolean setPermittedAccessibilityServices(ComponentName componentName, List list) throws RemoteException;

    boolean setPermittedInputMethods(ComponentName componentName, List list) throws RemoteException;

    void setProfileEnabled(ComponentName componentName) throws RemoteException;

    void setProfileName(ComponentName componentName, String str) throws RemoteException;

    boolean setProfileOwner(ComponentName componentName, String str, int i) throws RemoteException;

    void setRecommendedGlobalProxy(ComponentName componentName, ProxyInfo proxyInfo) throws RemoteException;

    void setRestrictionsProvider(ComponentName componentName, ComponentName componentName2) throws RemoteException;

    void setScreenCaptureDisabled(ComponentName componentName, boolean z) throws RemoteException;

    void setSecureSetting(ComponentName componentName, String str, String str2) throws RemoteException;

    void setSecurityLoggingEnabled(ComponentName componentName, boolean z) throws RemoteException;

    void setShortSupportMessage(ComponentName componentName, CharSequence charSequence) throws RemoteException;

    boolean setStatusBarDisabled(ComponentName componentName, boolean z) throws RemoteException;

    int setStorageEncryption(ComponentName componentName, boolean z) throws RemoteException;

    void setSystemUpdatePolicy(ComponentName componentName, SystemUpdatePolicy systemUpdatePolicy) throws RemoteException;

    void setTrustAgentConfiguration(ComponentName componentName, ComponentName componentName2, PersistableBundle persistableBundle, boolean z) throws RemoteException;

    void setUninstallBlocked(ComponentName componentName, String str, boolean z) throws RemoteException;

    void setUserIcon(ComponentName componentName, Bitmap bitmap) throws RemoteException;

    void setUserProvisioningState(int i, int i2) throws RemoteException;

    void setUserRestriction(ComponentName componentName, String str, boolean z) throws RemoteException;

    void startManagedQuickContact(String str, long j, boolean z, long j2, Intent intent) throws RemoteException;

    boolean switchUser(ComponentName componentName, UserHandle userHandle) throws RemoteException;

    void uninstallCaCerts(ComponentName componentName, String[] strArr) throws RemoteException;

    void uninstallPackageWithActiveAdmins(String str) throws RemoteException;

    void wipeData(int i) throws RemoteException;

    public static abstract class Stub extends Binder implements IDevicePolicyManager {
        private static final String DESCRIPTOR = "android.app.admin.IDevicePolicyManager";
        static final int TRANSACTION_addCrossProfileIntentFilter = 102;
        static final int TRANSACTION_addCrossProfileWidgetProvider = 144;
        static final int TRANSACTION_addPersistentPreferredActivity = 91;
        static final int TRANSACTION_approveCaCert = 82;
        static final int TRANSACTION_choosePrivateKeyAlias = 86;
        static final int TRANSACTION_clearCrossProfileIntentFilters = 103;
        static final int TRANSACTION_clearDeviceOwner = 66;
        static final int TRANSACTION_clearPackagePersistentPreferredActivities = 92;
        static final int TRANSACTION_clearProfileOwner = 73;
        static final int TRANSACTION_createAndManageUser = 114;
        static final int TRANSACTION_enableSystemApp = 117;
        static final int TRANSACTION_enableSystemAppWithIntent = 118;
        static final int TRANSACTION_enforceCanManageCaCerts = 81;
        static final int TRANSACTION_forceRemoveActiveAdmin = 53;
        static final int TRANSACTION_getAccountTypesWithManagementDisabled = 120;
        static final int TRANSACTION_getAccountTypesWithManagementDisabledAsUser = 121;
        static final int TRANSACTION_getActiveAdmins = 49;
        static final int TRANSACTION_getAlwaysOnVpnPackage = 90;
        static final int TRANSACTION_getApplicationRestrictions = 94;
        static final int TRANSACTION_getApplicationRestrictionsManagingPackage = 96;
        static final int TRANSACTION_getAutoTimeRequired = 148;
        static final int TRANSACTION_getBluetoothContactSharingDisabled = 140;
        static final int TRANSACTION_getBluetoothContactSharingDisabledForUser = 141;
        static final int TRANSACTION_getCameraDisabled = 42;
        static final int TRANSACTION_getCertInstallerPackage = 88;
        static final int TRANSACTION_getCrossProfileCallerIdDisabled = 133;
        static final int TRANSACTION_getCrossProfileCallerIdDisabledForUser = 134;
        static final int TRANSACTION_getCrossProfileContactsSearchDisabled = 136;
        static final int TRANSACTION_getCrossProfileContactsSearchDisabledForUser = 137;
        static final int TRANSACTION_getCrossProfileWidgetProviders = 146;
        static final int TRANSACTION_getCurrentFailedPasswordAttempts = 24;
        static final int TRANSACTION_getDeviceOwnerComponent = 64;
        static final int TRANSACTION_getDeviceOwnerLockScreenInfo = 76;
        static final int TRANSACTION_getDeviceOwnerName = 65;
        static final int TRANSACTION_getDeviceOwnerUserId = 67;
        static final int TRANSACTION_getDoNotAskCredentialsOnBoot = 157;
        static final int TRANSACTION_getForceEphemeralUsers = 150;
        static final int TRANSACTION_getGlobalProxyAdmin = 35;
        static final int TRANSACTION_getKeepUninstalledPackages = 165;
        static final int TRANSACTION_getKeyguardDisabledFeatures = 46;
        static final int TRANSACTION_getLockTaskPackages = 123;
        static final int TRANSACTION_getLongSupportMessage = 173;
        static final int TRANSACTION_getLongSupportMessageForUser = 175;
        static final int TRANSACTION_getMaximumFailedPasswordsForWipe = 27;
        static final int TRANSACTION_getMaximumTimeToLock = 30;
        static final int TRANSACTION_getMaximumTimeToLockForUserAndProfiles = 31;
        static final int TRANSACTION_getOrganizationColor = 179;
        static final int TRANSACTION_getOrganizationColorForUser = 180;
        static final int TRANSACTION_getOrganizationName = 182;
        static final int TRANSACTION_getOrganizationNameForUser = 183;
        static final int TRANSACTION_getPasswordExpiration = 21;
        static final int TRANSACTION_getPasswordExpirationTimeout = 20;
        static final int TRANSACTION_getPasswordHistoryLength = 18;
        static final int TRANSACTION_getPasswordMinimumLength = 4;
        static final int TRANSACTION_getPasswordMinimumLetters = 10;
        static final int TRANSACTION_getPasswordMinimumLowerCase = 8;
        static final int TRANSACTION_getPasswordMinimumNonLetter = 16;
        static final int TRANSACTION_getPasswordMinimumNumeric = 12;
        static final int TRANSACTION_getPasswordMinimumSymbols = 14;
        static final int TRANSACTION_getPasswordMinimumUpperCase = 6;
        static final int TRANSACTION_getPasswordQuality = 2;
        static final int TRANSACTION_getPermissionGrantState = 162;
        static final int TRANSACTION_getPermissionPolicy = 160;
        static final int TRANSACTION_getPermittedAccessibilityServices = 105;
        static final int TRANSACTION_getPermittedAccessibilityServicesForUser = 106;
        static final int TRANSACTION_getPermittedInputMethods = 109;
        static final int TRANSACTION_getPermittedInputMethodsForCurrentUser = 110;
        static final int TRANSACTION_getProfileOwner = 69;
        static final int TRANSACTION_getProfileOwnerName = 70;
        static final int TRANSACTION_getProfileWithMinimumFailedPasswordsForWipe = 25;
        static final int TRANSACTION_getRemoveWarning = 51;
        static final int TRANSACTION_getRestrictionsProvider = 99;
        static final int TRANSACTION_getScreenCaptureDisabled = 44;
        static final int TRANSACTION_getShortSupportMessage = 171;
        static final int TRANSACTION_getShortSupportMessageForUser = 174;
        static final int TRANSACTION_getStorageEncryption = 38;
        static final int TRANSACTION_getStorageEncryptionStatus = 39;
        static final int TRANSACTION_getSystemUpdatePolicy = 154;
        static final int TRANSACTION_getTrustAgentConfiguration = 143;
        static final int TRANSACTION_getUserProvisioningState = 184;
        static final int TRANSACTION_getUserRestrictions = 101;
        static final int TRANSACTION_getWifiMacAddress = 168;
        static final int TRANSACTION_hasGrantedPolicy = 54;
        static final int TRANSACTION_hasUserSetupCompleted = 74;
        static final int TRANSACTION_installCaCert = 79;
        static final int TRANSACTION_installKeyPair = 84;
        static final int TRANSACTION_isAccessibilityServicePermittedByAdmin = 107;
        static final int TRANSACTION_isActivePasswordSufficient = 22;
        static final int TRANSACTION_isAdminActive = 48;
        static final int TRANSACTION_isAffiliatedUser = 187;
        static final int TRANSACTION_isApplicationHidden = 113;
        static final int TRANSACTION_isCaCertApproved = 83;
        static final int TRANSACTION_isCallerApplicationRestrictionsManagingPackage = 97;
        static final int TRANSACTION_isInputMethodPermittedByAdmin = 111;
        static final int TRANSACTION_isLockTaskPermitted = 124;
        static final int TRANSACTION_isManagedProfile = 166;
        static final int TRANSACTION_isMasterVolumeMuted = 128;
        static final int TRANSACTION_isPackageSuspended = 78;
        static final int TRANSACTION_isProfileActivePasswordSufficientForParent = 23;
        static final int TRANSACTION_isProvisioningAllowed = 163;
        static final int TRANSACTION_isRemovingAdmin = 151;
        static final int TRANSACTION_isSecurityLoggingEnabled = 189;
        static final int TRANSACTION_isSeparateProfileChallengeAllowed = 176;
        static final int TRANSACTION_isSystemOnlyUser = 167;
        static final int TRANSACTION_isUninstallBlocked = 131;
        static final int TRANSACTION_isUninstallInQueue = 192;
        static final int TRANSACTION_lockNow = 32;
        static final int TRANSACTION_notifyLockTaskModeChanged = 129;
        static final int TRANSACTION_notifyPendingSystemUpdate = 158;
        static final int TRANSACTION_packageHasActiveAdmins = 50;
        static final int TRANSACTION_reboot = 169;
        static final int TRANSACTION_removeActiveAdmin = 52;
        static final int TRANSACTION_removeCrossProfileWidgetProvider = 145;
        static final int TRANSACTION_removeKeyPair = 85;
        static final int TRANSACTION_removeUser = 115;
        static final int TRANSACTION_reportFailedFingerprintAttempt = 59;
        static final int TRANSACTION_reportFailedPasswordAttempt = 57;
        static final int TRANSACTION_reportKeyguardDismissed = 61;
        static final int TRANSACTION_reportKeyguardSecured = 62;
        static final int TRANSACTION_reportPasswordChanged = 56;
        static final int TRANSACTION_reportSuccessfulFingerprintAttempt = 60;
        static final int TRANSACTION_reportSuccessfulPasswordAttempt = 58;
        static final int TRANSACTION_requestBugreport = 40;
        static final int TRANSACTION_resetPassword = 28;
        static final int TRANSACTION_retrievePreRebootSecurityLogs = 191;
        static final int TRANSACTION_retrieveSecurityLogs = 190;
        static final int TRANSACTION_setAccountManagementDisabled = 119;
        static final int TRANSACTION_setActiveAdmin = 47;
        static final int TRANSACTION_setActivePasswordState = 55;
        static final int TRANSACTION_setAffiliationIds = 186;
        static final int TRANSACTION_setAlwaysOnVpnPackage = 89;
        static final int TRANSACTION_setApplicationHidden = 112;
        static final int TRANSACTION_setApplicationRestrictions = 93;
        static final int TRANSACTION_setApplicationRestrictionsManagingPackage = 95;
        static final int TRANSACTION_setAutoTimeRequired = 147;
        static final int TRANSACTION_setBluetoothContactSharingDisabled = 139;
        static final int TRANSACTION_setCameraDisabled = 41;
        static final int TRANSACTION_setCertInstallerPackage = 87;
        static final int TRANSACTION_setCrossProfileCallerIdDisabled = 132;
        static final int TRANSACTION_setCrossProfileContactsSearchDisabled = 135;
        static final int TRANSACTION_setDeviceOwner = 63;
        static final int TRANSACTION_setDeviceOwnerLockScreenInfo = 75;
        static final int TRANSACTION_setForceEphemeralUsers = 149;
        static final int TRANSACTION_setGlobalProxy = 34;
        static final int TRANSACTION_setGlobalSetting = 125;
        static final int TRANSACTION_setKeepUninstalledPackages = 164;
        static final int TRANSACTION_setKeyguardDisabled = 155;
        static final int TRANSACTION_setKeyguardDisabledFeatures = 45;
        static final int TRANSACTION_setLockTaskPackages = 122;
        static final int TRANSACTION_setLongSupportMessage = 172;
        static final int TRANSACTION_setMasterVolumeMuted = 127;
        static final int TRANSACTION_setMaximumFailedPasswordsForWipe = 26;
        static final int TRANSACTION_setMaximumTimeToLock = 29;
        static final int TRANSACTION_setOrganizationColor = 177;
        static final int TRANSACTION_setOrganizationColorForUser = 178;
        static final int TRANSACTION_setOrganizationName = 181;
        static final int TRANSACTION_setPackagesSuspended = 77;
        static final int TRANSACTION_setPasswordExpirationTimeout = 19;
        static final int TRANSACTION_setPasswordHistoryLength = 17;
        static final int TRANSACTION_setPasswordMinimumLength = 3;
        static final int TRANSACTION_setPasswordMinimumLetters = 9;
        static final int TRANSACTION_setPasswordMinimumLowerCase = 7;
        static final int TRANSACTION_setPasswordMinimumNonLetter = 15;
        static final int TRANSACTION_setPasswordMinimumNumeric = 11;
        static final int TRANSACTION_setPasswordMinimumSymbols = 13;
        static final int TRANSACTION_setPasswordMinimumUpperCase = 5;
        static final int TRANSACTION_setPasswordQuality = 1;
        static final int TRANSACTION_setPermissionGrantState = 161;
        static final int TRANSACTION_setPermissionPolicy = 159;
        static final int TRANSACTION_setPermittedAccessibilityServices = 104;
        static final int TRANSACTION_setPermittedInputMethods = 108;
        static final int TRANSACTION_setProfileEnabled = 71;
        static final int TRANSACTION_setProfileName = 72;
        static final int TRANSACTION_setProfileOwner = 68;
        static final int TRANSACTION_setRecommendedGlobalProxy = 36;
        static final int TRANSACTION_setRestrictionsProvider = 98;
        static final int TRANSACTION_setScreenCaptureDisabled = 43;
        static final int TRANSACTION_setSecureSetting = 126;
        static final int TRANSACTION_setSecurityLoggingEnabled = 188;
        static final int TRANSACTION_setShortSupportMessage = 170;
        static final int TRANSACTION_setStatusBarDisabled = 156;
        static final int TRANSACTION_setStorageEncryption = 37;
        static final int TRANSACTION_setSystemUpdatePolicy = 153;
        static final int TRANSACTION_setTrustAgentConfiguration = 142;
        static final int TRANSACTION_setUninstallBlocked = 130;
        static final int TRANSACTION_setUserIcon = 152;
        static final int TRANSACTION_setUserProvisioningState = 185;
        static final int TRANSACTION_setUserRestriction = 100;
        static final int TRANSACTION_startManagedQuickContact = 138;
        static final int TRANSACTION_switchUser = 116;
        static final int TRANSACTION_uninstallCaCerts = 80;
        static final int TRANSACTION_uninstallPackageWithActiveAdmins = 193;
        static final int TRANSACTION_wipeData = 33;

        public Stub() {
            attachInterface(this, DESCRIPTOR);
        }

        public static IDevicePolicyManager asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
            if (iin != null && (iin instanceof IDevicePolicyManager)) {
                return (IDevicePolicyManager) iin;
            }
            return new Proxy(obj);
        }

        @Override
        public IBinder asBinder() {
            return this;
        }

        @Override
        public boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
            ComponentName componentNameCreateFromParcel;
            ComponentName componentNameCreateFromParcel2;
            ComponentName componentNameCreateFromParcel3;
            ComponentName componentNameCreateFromParcel4;
            ComponentName componentNameCreateFromParcel5;
            ComponentName componentNameCreateFromParcel6;
            ComponentName componentNameCreateFromParcel7;
            CharSequence charSequence;
            ComponentName componentNameCreateFromParcel8;
            ComponentName componentNameCreateFromParcel9;
            ComponentName componentNameCreateFromParcel10;
            ComponentName componentNameCreateFromParcel11;
            ComponentName componentNameCreateFromParcel12;
            ComponentName componentNameCreateFromParcel13;
            CharSequence charSequence2;
            ComponentName componentNameCreateFromParcel14;
            ComponentName componentNameCreateFromParcel15;
            CharSequence charSequence3;
            ComponentName componentNameCreateFromParcel16;
            ComponentName componentNameCreateFromParcel17;
            ComponentName componentNameCreateFromParcel18;
            ComponentName componentNameCreateFromParcel19;
            ComponentName componentNameCreateFromParcel20;
            ComponentName componentNameCreateFromParcel21;
            ComponentName componentNameCreateFromParcel22;
            ComponentName componentNameCreateFromParcel23;
            ComponentName componentNameCreateFromParcel24;
            ComponentName componentNameCreateFromParcel25;
            ComponentName componentNameCreateFromParcel26;
            ComponentName componentNameCreateFromParcel27;
            ComponentName componentNameCreateFromParcel28;
            SystemUpdatePolicy systemUpdatePolicyCreateFromParcel;
            ComponentName componentNameCreateFromParcel29;
            Bitmap bitmapCreateFromParcel;
            ComponentName componentNameCreateFromParcel30;
            ComponentName componentNameCreateFromParcel31;
            ComponentName componentNameCreateFromParcel32;
            ComponentName componentNameCreateFromParcel33;
            ComponentName componentNameCreateFromParcel34;
            ComponentName componentNameCreateFromParcel35;
            ComponentName componentNameCreateFromParcel36;
            ComponentName componentNameCreateFromParcel37;
            ComponentName componentNameCreateFromParcel38;
            ComponentName componentNameCreateFromParcel39;
            ComponentName componentNameCreateFromParcel40;
            PersistableBundle persistableBundleCreateFromParcel;
            ComponentName componentNameCreateFromParcel41;
            ComponentName componentNameCreateFromParcel42;
            Intent intentCreateFromParcel;
            ComponentName componentNameCreateFromParcel43;
            ComponentName componentNameCreateFromParcel44;
            ComponentName componentNameCreateFromParcel45;
            ComponentName componentNameCreateFromParcel46;
            ComponentName componentNameCreateFromParcel47;
            ComponentName componentNameCreateFromParcel48;
            ComponentName componentNameCreateFromParcel49;
            ComponentName componentNameCreateFromParcel50;
            ComponentName componentNameCreateFromParcel51;
            ComponentName componentNameCreateFromParcel52;
            ComponentName componentNameCreateFromParcel53;
            ComponentName componentNameCreateFromParcel54;
            ComponentName componentNameCreateFromParcel55;
            ComponentName componentNameCreateFromParcel56;
            Intent intentCreateFromParcel2;
            ComponentName componentNameCreateFromParcel57;
            ComponentName componentNameCreateFromParcel58;
            UserHandle userHandleCreateFromParcel;
            ComponentName componentNameCreateFromParcel59;
            UserHandle userHandleCreateFromParcel2;
            ComponentName componentNameCreateFromParcel60;
            ComponentName componentNameCreateFromParcel61;
            PersistableBundle persistableBundleCreateFromParcel2;
            ComponentName componentNameCreateFromParcel62;
            ComponentName componentNameCreateFromParcel63;
            ComponentName componentNameCreateFromParcel64;
            ComponentName componentNameCreateFromParcel65;
            ComponentName componentNameCreateFromParcel66;
            ComponentName componentNameCreateFromParcel67;
            ComponentName componentNameCreateFromParcel68;
            ComponentName componentNameCreateFromParcel69;
            ComponentName componentNameCreateFromParcel70;
            ComponentName componentNameCreateFromParcel71;
            IntentFilter intentFilterCreateFromParcel;
            ComponentName componentNameCreateFromParcel72;
            ComponentName componentNameCreateFromParcel73;
            ComponentName componentNameCreateFromParcel74;
            ComponentName componentNameCreateFromParcel75;
            ComponentName componentNameCreateFromParcel76;
            ComponentName componentNameCreateFromParcel77;
            ComponentName componentNameCreateFromParcel78;
            ComponentName componentNameCreateFromParcel79;
            Bundle bundleCreateFromParcel;
            ComponentName componentNameCreateFromParcel80;
            ComponentName componentNameCreateFromParcel81;
            IntentFilter intentFilterCreateFromParcel2;
            ComponentName componentNameCreateFromParcel82;
            ComponentName componentNameCreateFromParcel83;
            ComponentName componentNameCreateFromParcel84;
            ComponentName componentNameCreateFromParcel85;
            ComponentName componentNameCreateFromParcel86;
            Uri uriCreateFromParcel;
            ComponentName componentNameCreateFromParcel87;
            ComponentName componentNameCreateFromParcel88;
            ComponentName componentNameCreateFromParcel89;
            ComponentName componentNameCreateFromParcel90;
            ComponentName componentNameCreateFromParcel91;
            ComponentName componentNameCreateFromParcel92;
            ComponentName componentNameCreateFromParcel93;
            ComponentName componentNameCreateFromParcel94;
            CharSequence charSequence4;
            ComponentName componentNameCreateFromParcel95;
            ComponentName componentNameCreateFromParcel96;
            ComponentName componentNameCreateFromParcel97;
            ComponentName componentNameCreateFromParcel98;
            ComponentName componentNameCreateFromParcel99;
            ComponentName componentNameCreateFromParcel100;
            ComponentName componentNameCreateFromParcel101;
            ComponentName componentNameCreateFromParcel102;
            ComponentName componentNameCreateFromParcel103;
            RemoteCallback remoteCallbackCreateFromParcel;
            ComponentName componentNameCreateFromParcel104;
            ComponentName componentNameCreateFromParcel105;
            ComponentName componentNameCreateFromParcel106;
            ComponentName componentNameCreateFromParcel107;
            ComponentName componentNameCreateFromParcel108;
            ComponentName componentNameCreateFromParcel109;
            ComponentName componentNameCreateFromParcel110;
            ComponentName componentNameCreateFromParcel111;
            ComponentName componentNameCreateFromParcel112;
            ComponentName componentNameCreateFromParcel113;
            ComponentName componentNameCreateFromParcel114;
            ComponentName componentNameCreateFromParcel115;
            ProxyInfo proxyInfoCreateFromParcel;
            ComponentName componentNameCreateFromParcel116;
            ComponentName componentNameCreateFromParcel117;
            ComponentName componentNameCreateFromParcel118;
            ComponentName componentNameCreateFromParcel119;
            ComponentName componentNameCreateFromParcel120;
            ComponentName componentNameCreateFromParcel121;
            ComponentName componentNameCreateFromParcel122;
            ComponentName componentNameCreateFromParcel123;
            ComponentName componentNameCreateFromParcel124;
            ComponentName componentNameCreateFromParcel125;
            ComponentName componentNameCreateFromParcel126;
            ComponentName componentNameCreateFromParcel127;
            ComponentName componentNameCreateFromParcel128;
            ComponentName componentNameCreateFromParcel129;
            ComponentName componentNameCreateFromParcel130;
            ComponentName componentNameCreateFromParcel131;
            ComponentName componentNameCreateFromParcel132;
            ComponentName componentNameCreateFromParcel133;
            ComponentName componentNameCreateFromParcel134;
            ComponentName componentNameCreateFromParcel135;
            ComponentName componentNameCreateFromParcel136;
            ComponentName componentNameCreateFromParcel137;
            ComponentName componentNameCreateFromParcel138;
            ComponentName componentNameCreateFromParcel139;
            ComponentName componentNameCreateFromParcel140;
            ComponentName componentNameCreateFromParcel141;
            switch (code) {
                case 1:
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        componentNameCreateFromParcel141 = ComponentName.CREATOR.createFromParcel(data);
                    } else {
                        componentNameCreateFromParcel141 = null;
                    }
                    int _arg1 = data.readInt();
                    boolean _arg2 = data.readInt() != 0;
                    setPasswordQuality(componentNameCreateFromParcel141, _arg1, _arg2);
                    reply.writeNoException();
                    return true;
                case 2:
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        componentNameCreateFromParcel140 = ComponentName.CREATOR.createFromParcel(data);
                    } else {
                        componentNameCreateFromParcel140 = null;
                    }
                    int _arg12 = data.readInt();
                    boolean _arg22 = data.readInt() != 0;
                    int _result = getPasswordQuality(componentNameCreateFromParcel140, _arg12, _arg22);
                    reply.writeNoException();
                    reply.writeInt(_result);
                    return true;
                case 3:
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        componentNameCreateFromParcel139 = ComponentName.CREATOR.createFromParcel(data);
                    } else {
                        componentNameCreateFromParcel139 = null;
                    }
                    int _arg13 = data.readInt();
                    boolean _arg23 = data.readInt() != 0;
                    setPasswordMinimumLength(componentNameCreateFromParcel139, _arg13, _arg23);
                    reply.writeNoException();
                    return true;
                case 4:
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        componentNameCreateFromParcel138 = ComponentName.CREATOR.createFromParcel(data);
                    } else {
                        componentNameCreateFromParcel138 = null;
                    }
                    int _arg14 = data.readInt();
                    boolean _arg24 = data.readInt() != 0;
                    int _result2 = getPasswordMinimumLength(componentNameCreateFromParcel138, _arg14, _arg24);
                    reply.writeNoException();
                    reply.writeInt(_result2);
                    return true;
                case 5:
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        componentNameCreateFromParcel137 = ComponentName.CREATOR.createFromParcel(data);
                    } else {
                        componentNameCreateFromParcel137 = null;
                    }
                    int _arg15 = data.readInt();
                    boolean _arg25 = data.readInt() != 0;
                    setPasswordMinimumUpperCase(componentNameCreateFromParcel137, _arg15, _arg25);
                    reply.writeNoException();
                    return true;
                case 6:
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        componentNameCreateFromParcel136 = ComponentName.CREATOR.createFromParcel(data);
                    } else {
                        componentNameCreateFromParcel136 = null;
                    }
                    int _arg16 = data.readInt();
                    boolean _arg26 = data.readInt() != 0;
                    int _result3 = getPasswordMinimumUpperCase(componentNameCreateFromParcel136, _arg16, _arg26);
                    reply.writeNoException();
                    reply.writeInt(_result3);
                    return true;
                case 7:
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        componentNameCreateFromParcel135 = ComponentName.CREATOR.createFromParcel(data);
                    } else {
                        componentNameCreateFromParcel135 = null;
                    }
                    int _arg17 = data.readInt();
                    boolean _arg27 = data.readInt() != 0;
                    setPasswordMinimumLowerCase(componentNameCreateFromParcel135, _arg17, _arg27);
                    reply.writeNoException();
                    return true;
                case 8:
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        componentNameCreateFromParcel134 = ComponentName.CREATOR.createFromParcel(data);
                    } else {
                        componentNameCreateFromParcel134 = null;
                    }
                    int _arg18 = data.readInt();
                    boolean _arg28 = data.readInt() != 0;
                    int _result4 = getPasswordMinimumLowerCase(componentNameCreateFromParcel134, _arg18, _arg28);
                    reply.writeNoException();
                    reply.writeInt(_result4);
                    return true;
                case 9:
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        componentNameCreateFromParcel133 = ComponentName.CREATOR.createFromParcel(data);
                    } else {
                        componentNameCreateFromParcel133 = null;
                    }
                    int _arg19 = data.readInt();
                    boolean _arg29 = data.readInt() != 0;
                    setPasswordMinimumLetters(componentNameCreateFromParcel133, _arg19, _arg29);
                    reply.writeNoException();
                    return true;
                case 10:
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        componentNameCreateFromParcel132 = ComponentName.CREATOR.createFromParcel(data);
                    } else {
                        componentNameCreateFromParcel132 = null;
                    }
                    int _arg110 = data.readInt();
                    boolean _arg210 = data.readInt() != 0;
                    int _result5 = getPasswordMinimumLetters(componentNameCreateFromParcel132, _arg110, _arg210);
                    reply.writeNoException();
                    reply.writeInt(_result5);
                    return true;
                case 11:
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        componentNameCreateFromParcel131 = ComponentName.CREATOR.createFromParcel(data);
                    } else {
                        componentNameCreateFromParcel131 = null;
                    }
                    int _arg111 = data.readInt();
                    boolean _arg211 = data.readInt() != 0;
                    setPasswordMinimumNumeric(componentNameCreateFromParcel131, _arg111, _arg211);
                    reply.writeNoException();
                    return true;
                case 12:
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        componentNameCreateFromParcel130 = ComponentName.CREATOR.createFromParcel(data);
                    } else {
                        componentNameCreateFromParcel130 = null;
                    }
                    int _arg112 = data.readInt();
                    boolean _arg212 = data.readInt() != 0;
                    int _result6 = getPasswordMinimumNumeric(componentNameCreateFromParcel130, _arg112, _arg212);
                    reply.writeNoException();
                    reply.writeInt(_result6);
                    return true;
                case 13:
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        componentNameCreateFromParcel129 = ComponentName.CREATOR.createFromParcel(data);
                    } else {
                        componentNameCreateFromParcel129 = null;
                    }
                    int _arg113 = data.readInt();
                    boolean _arg213 = data.readInt() != 0;
                    setPasswordMinimumSymbols(componentNameCreateFromParcel129, _arg113, _arg213);
                    reply.writeNoException();
                    return true;
                case 14:
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        componentNameCreateFromParcel128 = ComponentName.CREATOR.createFromParcel(data);
                    } else {
                        componentNameCreateFromParcel128 = null;
                    }
                    int _arg114 = data.readInt();
                    boolean _arg214 = data.readInt() != 0;
                    int _result7 = getPasswordMinimumSymbols(componentNameCreateFromParcel128, _arg114, _arg214);
                    reply.writeNoException();
                    reply.writeInt(_result7);
                    return true;
                case 15:
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        componentNameCreateFromParcel127 = ComponentName.CREATOR.createFromParcel(data);
                    } else {
                        componentNameCreateFromParcel127 = null;
                    }
                    int _arg115 = data.readInt();
                    boolean _arg215 = data.readInt() != 0;
                    setPasswordMinimumNonLetter(componentNameCreateFromParcel127, _arg115, _arg215);
                    reply.writeNoException();
                    return true;
                case 16:
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        componentNameCreateFromParcel126 = ComponentName.CREATOR.createFromParcel(data);
                    } else {
                        componentNameCreateFromParcel126 = null;
                    }
                    int _arg116 = data.readInt();
                    boolean _arg216 = data.readInt() != 0;
                    int _result8 = getPasswordMinimumNonLetter(componentNameCreateFromParcel126, _arg116, _arg216);
                    reply.writeNoException();
                    reply.writeInt(_result8);
                    return true;
                case 17:
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        componentNameCreateFromParcel125 = ComponentName.CREATOR.createFromParcel(data);
                    } else {
                        componentNameCreateFromParcel125 = null;
                    }
                    int _arg117 = data.readInt();
                    boolean _arg217 = data.readInt() != 0;
                    setPasswordHistoryLength(componentNameCreateFromParcel125, _arg117, _arg217);
                    reply.writeNoException();
                    return true;
                case 18:
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        componentNameCreateFromParcel124 = ComponentName.CREATOR.createFromParcel(data);
                    } else {
                        componentNameCreateFromParcel124 = null;
                    }
                    int _arg118 = data.readInt();
                    boolean _arg218 = data.readInt() != 0;
                    int _result9 = getPasswordHistoryLength(componentNameCreateFromParcel124, _arg118, _arg218);
                    reply.writeNoException();
                    reply.writeInt(_result9);
                    return true;
                case 19:
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        componentNameCreateFromParcel123 = ComponentName.CREATOR.createFromParcel(data);
                    } else {
                        componentNameCreateFromParcel123 = null;
                    }
                    long _arg119 = data.readLong();
                    boolean _arg219 = data.readInt() != 0;
                    setPasswordExpirationTimeout(componentNameCreateFromParcel123, _arg119, _arg219);
                    reply.writeNoException();
                    return true;
                case 20:
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        componentNameCreateFromParcel122 = ComponentName.CREATOR.createFromParcel(data);
                    } else {
                        componentNameCreateFromParcel122 = null;
                    }
                    int _arg120 = data.readInt();
                    boolean _arg220 = data.readInt() != 0;
                    long _result10 = getPasswordExpirationTimeout(componentNameCreateFromParcel122, _arg120, _arg220);
                    reply.writeNoException();
                    reply.writeLong(_result10);
                    return true;
                case 21:
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        componentNameCreateFromParcel121 = ComponentName.CREATOR.createFromParcel(data);
                    } else {
                        componentNameCreateFromParcel121 = null;
                    }
                    int _arg121 = data.readInt();
                    boolean _arg221 = data.readInt() != 0;
                    long _result11 = getPasswordExpiration(componentNameCreateFromParcel121, _arg121, _arg221);
                    reply.writeNoException();
                    reply.writeLong(_result11);
                    return true;
                case 22:
                    data.enforceInterface(DESCRIPTOR);
                    int _arg0 = data.readInt();
                    boolean _arg122 = data.readInt() != 0;
                    boolean _result12 = isActivePasswordSufficient(_arg0, _arg122);
                    reply.writeNoException();
                    reply.writeInt(_result12 ? 1 : 0);
                    return true;
                case 23:
                    data.enforceInterface(DESCRIPTOR);
                    int _arg02 = data.readInt();
                    boolean _result13 = isProfileActivePasswordSufficientForParent(_arg02);
                    reply.writeNoException();
                    reply.writeInt(_result13 ? 1 : 0);
                    return true;
                case 24:
                    data.enforceInterface(DESCRIPTOR);
                    int _arg03 = data.readInt();
                    boolean _arg123 = data.readInt() != 0;
                    int _result14 = getCurrentFailedPasswordAttempts(_arg03, _arg123);
                    reply.writeNoException();
                    reply.writeInt(_result14);
                    return true;
                case 25:
                    data.enforceInterface(DESCRIPTOR);
                    int _arg04 = data.readInt();
                    boolean _arg124 = data.readInt() != 0;
                    int _result15 = getProfileWithMinimumFailedPasswordsForWipe(_arg04, _arg124);
                    reply.writeNoException();
                    reply.writeInt(_result15);
                    return true;
                case 26:
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        componentNameCreateFromParcel120 = ComponentName.CREATOR.createFromParcel(data);
                    } else {
                        componentNameCreateFromParcel120 = null;
                    }
                    int _arg125 = data.readInt();
                    boolean _arg222 = data.readInt() != 0;
                    setMaximumFailedPasswordsForWipe(componentNameCreateFromParcel120, _arg125, _arg222);
                    reply.writeNoException();
                    return true;
                case 27:
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        componentNameCreateFromParcel119 = ComponentName.CREATOR.createFromParcel(data);
                    } else {
                        componentNameCreateFromParcel119 = null;
                    }
                    int _arg126 = data.readInt();
                    boolean _arg223 = data.readInt() != 0;
                    int _result16 = getMaximumFailedPasswordsForWipe(componentNameCreateFromParcel119, _arg126, _arg223);
                    reply.writeNoException();
                    reply.writeInt(_result16);
                    return true;
                case 28:
                    data.enforceInterface(DESCRIPTOR);
                    String _arg05 = data.readString();
                    int _arg127 = data.readInt();
                    boolean _result17 = resetPassword(_arg05, _arg127);
                    reply.writeNoException();
                    reply.writeInt(_result17 ? 1 : 0);
                    return true;
                case 29:
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        componentNameCreateFromParcel118 = ComponentName.CREATOR.createFromParcel(data);
                    } else {
                        componentNameCreateFromParcel118 = null;
                    }
                    long _arg128 = data.readLong();
                    boolean _arg224 = data.readInt() != 0;
                    setMaximumTimeToLock(componentNameCreateFromParcel118, _arg128, _arg224);
                    reply.writeNoException();
                    return true;
                case 30:
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        componentNameCreateFromParcel117 = ComponentName.CREATOR.createFromParcel(data);
                    } else {
                        componentNameCreateFromParcel117 = null;
                    }
                    int _arg129 = data.readInt();
                    boolean _arg225 = data.readInt() != 0;
                    long _result18 = getMaximumTimeToLock(componentNameCreateFromParcel117, _arg129, _arg225);
                    reply.writeNoException();
                    reply.writeLong(_result18);
                    return true;
                case 31:
                    data.enforceInterface(DESCRIPTOR);
                    int _arg06 = data.readInt();
                    long _result19 = getMaximumTimeToLockForUserAndProfiles(_arg06);
                    reply.writeNoException();
                    reply.writeLong(_result19);
                    return true;
                case 32:
                    data.enforceInterface(DESCRIPTOR);
                    boolean _arg07 = data.readInt() != 0;
                    lockNow(_arg07);
                    reply.writeNoException();
                    return true;
                case 33:
                    data.enforceInterface(DESCRIPTOR);
                    int _arg08 = data.readInt();
                    wipeData(_arg08);
                    reply.writeNoException();
                    return true;
                case 34:
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        componentNameCreateFromParcel116 = ComponentName.CREATOR.createFromParcel(data);
                    } else {
                        componentNameCreateFromParcel116 = null;
                    }
                    String _arg130 = data.readString();
                    String _arg226 = data.readString();
                    ComponentName _result20 = setGlobalProxy(componentNameCreateFromParcel116, _arg130, _arg226);
                    reply.writeNoException();
                    if (_result20 != null) {
                        reply.writeInt(1);
                        _result20.writeToParcel(reply, 1);
                        return true;
                    }
                    reply.writeInt(0);
                    return true;
                case 35:
                    data.enforceInterface(DESCRIPTOR);
                    int _arg09 = data.readInt();
                    ComponentName _result21 = getGlobalProxyAdmin(_arg09);
                    reply.writeNoException();
                    if (_result21 != null) {
                        reply.writeInt(1);
                        _result21.writeToParcel(reply, 1);
                        return true;
                    }
                    reply.writeInt(0);
                    return true;
                case 36:
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        componentNameCreateFromParcel115 = ComponentName.CREATOR.createFromParcel(data);
                    } else {
                        componentNameCreateFromParcel115 = null;
                    }
                    if (data.readInt() != 0) {
                        proxyInfoCreateFromParcel = ProxyInfo.CREATOR.createFromParcel(data);
                    } else {
                        proxyInfoCreateFromParcel = null;
                    }
                    setRecommendedGlobalProxy(componentNameCreateFromParcel115, proxyInfoCreateFromParcel);
                    reply.writeNoException();
                    return true;
                case 37:
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        componentNameCreateFromParcel114 = ComponentName.CREATOR.createFromParcel(data);
                    } else {
                        componentNameCreateFromParcel114 = null;
                    }
                    boolean _arg131 = data.readInt() != 0;
                    int _result22 = setStorageEncryption(componentNameCreateFromParcel114, _arg131);
                    reply.writeNoException();
                    reply.writeInt(_result22);
                    return true;
                case 38:
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        componentNameCreateFromParcel113 = ComponentName.CREATOR.createFromParcel(data);
                    } else {
                        componentNameCreateFromParcel113 = null;
                    }
                    int _arg132 = data.readInt();
                    boolean _result23 = getStorageEncryption(componentNameCreateFromParcel113, _arg132);
                    reply.writeNoException();
                    reply.writeInt(_result23 ? 1 : 0);
                    return true;
                case 39:
                    data.enforceInterface(DESCRIPTOR);
                    String _arg010 = data.readString();
                    int _arg133 = data.readInt();
                    int _result24 = getStorageEncryptionStatus(_arg010, _arg133);
                    reply.writeNoException();
                    reply.writeInt(_result24);
                    return true;
                case 40:
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        componentNameCreateFromParcel112 = ComponentName.CREATOR.createFromParcel(data);
                    } else {
                        componentNameCreateFromParcel112 = null;
                    }
                    boolean _result25 = requestBugreport(componentNameCreateFromParcel112);
                    reply.writeNoException();
                    reply.writeInt(_result25 ? 1 : 0);
                    return true;
                case 41:
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        componentNameCreateFromParcel111 = ComponentName.CREATOR.createFromParcel(data);
                    } else {
                        componentNameCreateFromParcel111 = null;
                    }
                    boolean _arg134 = data.readInt() != 0;
                    setCameraDisabled(componentNameCreateFromParcel111, _arg134);
                    reply.writeNoException();
                    return true;
                case 42:
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        componentNameCreateFromParcel110 = ComponentName.CREATOR.createFromParcel(data);
                    } else {
                        componentNameCreateFromParcel110 = null;
                    }
                    int _arg135 = data.readInt();
                    boolean _result26 = getCameraDisabled(componentNameCreateFromParcel110, _arg135);
                    reply.writeNoException();
                    reply.writeInt(_result26 ? 1 : 0);
                    return true;
                case 43:
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        componentNameCreateFromParcel109 = ComponentName.CREATOR.createFromParcel(data);
                    } else {
                        componentNameCreateFromParcel109 = null;
                    }
                    boolean _arg136 = data.readInt() != 0;
                    setScreenCaptureDisabled(componentNameCreateFromParcel109, _arg136);
                    reply.writeNoException();
                    return true;
                case 44:
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        componentNameCreateFromParcel108 = ComponentName.CREATOR.createFromParcel(data);
                    } else {
                        componentNameCreateFromParcel108 = null;
                    }
                    int _arg137 = data.readInt();
                    boolean _result27 = getScreenCaptureDisabled(componentNameCreateFromParcel108, _arg137);
                    reply.writeNoException();
                    reply.writeInt(_result27 ? 1 : 0);
                    return true;
                case 45:
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        componentNameCreateFromParcel107 = ComponentName.CREATOR.createFromParcel(data);
                    } else {
                        componentNameCreateFromParcel107 = null;
                    }
                    int _arg138 = data.readInt();
                    boolean _arg227 = data.readInt() != 0;
                    setKeyguardDisabledFeatures(componentNameCreateFromParcel107, _arg138, _arg227);
                    reply.writeNoException();
                    return true;
                case 46:
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        componentNameCreateFromParcel106 = ComponentName.CREATOR.createFromParcel(data);
                    } else {
                        componentNameCreateFromParcel106 = null;
                    }
                    int _arg139 = data.readInt();
                    boolean _arg228 = data.readInt() != 0;
                    int _result28 = getKeyguardDisabledFeatures(componentNameCreateFromParcel106, _arg139, _arg228);
                    reply.writeNoException();
                    reply.writeInt(_result28);
                    return true;
                case 47:
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        componentNameCreateFromParcel105 = ComponentName.CREATOR.createFromParcel(data);
                    } else {
                        componentNameCreateFromParcel105 = null;
                    }
                    boolean _arg140 = data.readInt() != 0;
                    int _arg229 = data.readInt();
                    setActiveAdmin(componentNameCreateFromParcel105, _arg140, _arg229);
                    reply.writeNoException();
                    return true;
                case 48:
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        componentNameCreateFromParcel104 = ComponentName.CREATOR.createFromParcel(data);
                    } else {
                        componentNameCreateFromParcel104 = null;
                    }
                    int _arg141 = data.readInt();
                    boolean _result29 = isAdminActive(componentNameCreateFromParcel104, _arg141);
                    reply.writeNoException();
                    reply.writeInt(_result29 ? 1 : 0);
                    return true;
                case 49:
                    data.enforceInterface(DESCRIPTOR);
                    int _arg011 = data.readInt();
                    List<ComponentName> _result30 = getActiveAdmins(_arg011);
                    reply.writeNoException();
                    reply.writeTypedList(_result30);
                    return true;
                case 50:
                    data.enforceInterface(DESCRIPTOR);
                    String _arg012 = data.readString();
                    int _arg142 = data.readInt();
                    boolean _result31 = packageHasActiveAdmins(_arg012, _arg142);
                    reply.writeNoException();
                    reply.writeInt(_result31 ? 1 : 0);
                    return true;
                case 51:
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        componentNameCreateFromParcel103 = ComponentName.CREATOR.createFromParcel(data);
                    } else {
                        componentNameCreateFromParcel103 = null;
                    }
                    if (data.readInt() != 0) {
                        remoteCallbackCreateFromParcel = RemoteCallback.CREATOR.createFromParcel(data);
                    } else {
                        remoteCallbackCreateFromParcel = null;
                    }
                    int _arg230 = data.readInt();
                    getRemoveWarning(componentNameCreateFromParcel103, remoteCallbackCreateFromParcel, _arg230);
                    reply.writeNoException();
                    return true;
                case 52:
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        componentNameCreateFromParcel102 = ComponentName.CREATOR.createFromParcel(data);
                    } else {
                        componentNameCreateFromParcel102 = null;
                    }
                    int _arg143 = data.readInt();
                    removeActiveAdmin(componentNameCreateFromParcel102, _arg143);
                    reply.writeNoException();
                    return true;
                case 53:
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        componentNameCreateFromParcel101 = ComponentName.CREATOR.createFromParcel(data);
                    } else {
                        componentNameCreateFromParcel101 = null;
                    }
                    int _arg144 = data.readInt();
                    forceRemoveActiveAdmin(componentNameCreateFromParcel101, _arg144);
                    reply.writeNoException();
                    return true;
                case 54:
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        componentNameCreateFromParcel100 = ComponentName.CREATOR.createFromParcel(data);
                    } else {
                        componentNameCreateFromParcel100 = null;
                    }
                    int _arg145 = data.readInt();
                    int _arg231 = data.readInt();
                    boolean _result32 = hasGrantedPolicy(componentNameCreateFromParcel100, _arg145, _arg231);
                    reply.writeNoException();
                    reply.writeInt(_result32 ? 1 : 0);
                    return true;
                case 55:
                    data.enforceInterface(DESCRIPTOR);
                    int _arg013 = data.readInt();
                    int _arg146 = data.readInt();
                    int _arg232 = data.readInt();
                    int _arg3 = data.readInt();
                    int _arg4 = data.readInt();
                    int _arg5 = data.readInt();
                    int _arg6 = data.readInt();
                    int _arg7 = data.readInt();
                    int _arg8 = data.readInt();
                    setActivePasswordState(_arg013, _arg146, _arg232, _arg3, _arg4, _arg5, _arg6, _arg7, _arg8);
                    reply.writeNoException();
                    return true;
                case 56:
                    data.enforceInterface(DESCRIPTOR);
                    int _arg014 = data.readInt();
                    reportPasswordChanged(_arg014);
                    reply.writeNoException();
                    return true;
                case 57:
                    data.enforceInterface(DESCRIPTOR);
                    int _arg015 = data.readInt();
                    reportFailedPasswordAttempt(_arg015);
                    reply.writeNoException();
                    return true;
                case 58:
                    data.enforceInterface(DESCRIPTOR);
                    int _arg016 = data.readInt();
                    reportSuccessfulPasswordAttempt(_arg016);
                    reply.writeNoException();
                    return true;
                case 59:
                    data.enforceInterface(DESCRIPTOR);
                    int _arg017 = data.readInt();
                    reportFailedFingerprintAttempt(_arg017);
                    reply.writeNoException();
                    return true;
                case 60:
                    data.enforceInterface(DESCRIPTOR);
                    int _arg018 = data.readInt();
                    reportSuccessfulFingerprintAttempt(_arg018);
                    reply.writeNoException();
                    return true;
                case 61:
                    data.enforceInterface(DESCRIPTOR);
                    int _arg019 = data.readInt();
                    reportKeyguardDismissed(_arg019);
                    reply.writeNoException();
                    return true;
                case 62:
                    data.enforceInterface(DESCRIPTOR);
                    int _arg020 = data.readInt();
                    reportKeyguardSecured(_arg020);
                    reply.writeNoException();
                    return true;
                case 63:
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        componentNameCreateFromParcel99 = ComponentName.CREATOR.createFromParcel(data);
                    } else {
                        componentNameCreateFromParcel99 = null;
                    }
                    String _arg147 = data.readString();
                    int _arg233 = data.readInt();
                    boolean _result33 = setDeviceOwner(componentNameCreateFromParcel99, _arg147, _arg233);
                    reply.writeNoException();
                    reply.writeInt(_result33 ? 1 : 0);
                    return true;
                case 64:
                    data.enforceInterface(DESCRIPTOR);
                    boolean _arg021 = data.readInt() != 0;
                    ComponentName _result34 = getDeviceOwnerComponent(_arg021);
                    reply.writeNoException();
                    if (_result34 != null) {
                        reply.writeInt(1);
                        _result34.writeToParcel(reply, 1);
                        return true;
                    }
                    reply.writeInt(0);
                    return true;
                case 65:
                    data.enforceInterface(DESCRIPTOR);
                    String _result35 = getDeviceOwnerName();
                    reply.writeNoException();
                    reply.writeString(_result35);
                    return true;
                case 66:
                    data.enforceInterface(DESCRIPTOR);
                    String _arg022 = data.readString();
                    clearDeviceOwner(_arg022);
                    reply.writeNoException();
                    return true;
                case 67:
                    data.enforceInterface(DESCRIPTOR);
                    int _result36 = getDeviceOwnerUserId();
                    reply.writeNoException();
                    reply.writeInt(_result36);
                    return true;
                case 68:
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        componentNameCreateFromParcel98 = ComponentName.CREATOR.createFromParcel(data);
                    } else {
                        componentNameCreateFromParcel98 = null;
                    }
                    String _arg148 = data.readString();
                    int _arg234 = data.readInt();
                    boolean _result37 = setProfileOwner(componentNameCreateFromParcel98, _arg148, _arg234);
                    reply.writeNoException();
                    reply.writeInt(_result37 ? 1 : 0);
                    return true;
                case 69:
                    data.enforceInterface(DESCRIPTOR);
                    int _arg023 = data.readInt();
                    ComponentName _result38 = getProfileOwner(_arg023);
                    reply.writeNoException();
                    if (_result38 != null) {
                        reply.writeInt(1);
                        _result38.writeToParcel(reply, 1);
                        return true;
                    }
                    reply.writeInt(0);
                    return true;
                case 70:
                    data.enforceInterface(DESCRIPTOR);
                    int _arg024 = data.readInt();
                    String _result39 = getProfileOwnerName(_arg024);
                    reply.writeNoException();
                    reply.writeString(_result39);
                    return true;
                case 71:
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        componentNameCreateFromParcel97 = ComponentName.CREATOR.createFromParcel(data);
                    } else {
                        componentNameCreateFromParcel97 = null;
                    }
                    setProfileEnabled(componentNameCreateFromParcel97);
                    reply.writeNoException();
                    return true;
                case 72:
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        componentNameCreateFromParcel96 = ComponentName.CREATOR.createFromParcel(data);
                    } else {
                        componentNameCreateFromParcel96 = null;
                    }
                    String _arg149 = data.readString();
                    setProfileName(componentNameCreateFromParcel96, _arg149);
                    reply.writeNoException();
                    return true;
                case 73:
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        componentNameCreateFromParcel95 = ComponentName.CREATOR.createFromParcel(data);
                    } else {
                        componentNameCreateFromParcel95 = null;
                    }
                    clearProfileOwner(componentNameCreateFromParcel95);
                    reply.writeNoException();
                    return true;
                case 74:
                    data.enforceInterface(DESCRIPTOR);
                    boolean _result40 = hasUserSetupCompleted();
                    reply.writeNoException();
                    reply.writeInt(_result40 ? 1 : 0);
                    return true;
                case 75:
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        componentNameCreateFromParcel94 = ComponentName.CREATOR.createFromParcel(data);
                    } else {
                        componentNameCreateFromParcel94 = null;
                    }
                    if (data.readInt() != 0) {
                        charSequence4 = (CharSequence) TextUtils.CHAR_SEQUENCE_CREATOR.createFromParcel(data);
                    } else {
                        charSequence4 = null;
                    }
                    setDeviceOwnerLockScreenInfo(componentNameCreateFromParcel94, charSequence4);
                    reply.writeNoException();
                    return true;
                case 76:
                    data.enforceInterface(DESCRIPTOR);
                    CharSequence _result41 = getDeviceOwnerLockScreenInfo();
                    reply.writeNoException();
                    if (_result41 != null) {
                        reply.writeInt(1);
                        TextUtils.writeToParcel(_result41, reply, 1);
                        return true;
                    }
                    reply.writeInt(0);
                    return true;
                case 77:
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        componentNameCreateFromParcel93 = ComponentName.CREATOR.createFromParcel(data);
                    } else {
                        componentNameCreateFromParcel93 = null;
                    }
                    String[] _arg150 = data.createStringArray();
                    boolean _arg235 = data.readInt() != 0;
                    String[] _result42 = setPackagesSuspended(componentNameCreateFromParcel93, _arg150, _arg235);
                    reply.writeNoException();
                    reply.writeStringArray(_result42);
                    return true;
                case 78:
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        componentNameCreateFromParcel92 = ComponentName.CREATOR.createFromParcel(data);
                    } else {
                        componentNameCreateFromParcel92 = null;
                    }
                    String _arg151 = data.readString();
                    boolean _result43 = isPackageSuspended(componentNameCreateFromParcel92, _arg151);
                    reply.writeNoException();
                    reply.writeInt(_result43 ? 1 : 0);
                    return true;
                case 79:
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        componentNameCreateFromParcel91 = ComponentName.CREATOR.createFromParcel(data);
                    } else {
                        componentNameCreateFromParcel91 = null;
                    }
                    byte[] _arg152 = data.createByteArray();
                    boolean _result44 = installCaCert(componentNameCreateFromParcel91, _arg152);
                    reply.writeNoException();
                    reply.writeInt(_result44 ? 1 : 0);
                    return true;
                case 80:
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        componentNameCreateFromParcel90 = ComponentName.CREATOR.createFromParcel(data);
                    } else {
                        componentNameCreateFromParcel90 = null;
                    }
                    String[] _arg153 = data.createStringArray();
                    uninstallCaCerts(componentNameCreateFromParcel90, _arg153);
                    reply.writeNoException();
                    return true;
                case 81:
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        componentNameCreateFromParcel89 = ComponentName.CREATOR.createFromParcel(data);
                    } else {
                        componentNameCreateFromParcel89 = null;
                    }
                    enforceCanManageCaCerts(componentNameCreateFromParcel89);
                    reply.writeNoException();
                    return true;
                case 82:
                    data.enforceInterface(DESCRIPTOR);
                    String _arg025 = data.readString();
                    int _arg154 = data.readInt();
                    boolean _arg236 = data.readInt() != 0;
                    boolean _result45 = approveCaCert(_arg025, _arg154, _arg236);
                    reply.writeNoException();
                    reply.writeInt(_result45 ? 1 : 0);
                    return true;
                case 83:
                    data.enforceInterface(DESCRIPTOR);
                    String _arg026 = data.readString();
                    int _arg155 = data.readInt();
                    boolean _result46 = isCaCertApproved(_arg026, _arg155);
                    reply.writeNoException();
                    reply.writeInt(_result46 ? 1 : 0);
                    return true;
                case 84:
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        componentNameCreateFromParcel88 = ComponentName.CREATOR.createFromParcel(data);
                    } else {
                        componentNameCreateFromParcel88 = null;
                    }
                    byte[] _arg156 = data.createByteArray();
                    byte[] _arg237 = data.createByteArray();
                    byte[] _arg32 = data.createByteArray();
                    String _arg42 = data.readString();
                    boolean _arg52 = data.readInt() != 0;
                    boolean _result47 = installKeyPair(componentNameCreateFromParcel88, _arg156, _arg237, _arg32, _arg42, _arg52);
                    reply.writeNoException();
                    reply.writeInt(_result47 ? 1 : 0);
                    return true;
                case 85:
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        componentNameCreateFromParcel87 = ComponentName.CREATOR.createFromParcel(data);
                    } else {
                        componentNameCreateFromParcel87 = null;
                    }
                    String _arg157 = data.readString();
                    boolean _result48 = removeKeyPair(componentNameCreateFromParcel87, _arg157);
                    reply.writeNoException();
                    reply.writeInt(_result48 ? 1 : 0);
                    return true;
                case 86:
                    data.enforceInterface(DESCRIPTOR);
                    int _arg027 = data.readInt();
                    if (data.readInt() != 0) {
                        uriCreateFromParcel = Uri.CREATOR.createFromParcel(data);
                    } else {
                        uriCreateFromParcel = null;
                    }
                    String _arg238 = data.readString();
                    IBinder _arg33 = data.readStrongBinder();
                    choosePrivateKeyAlias(_arg027, uriCreateFromParcel, _arg238, _arg33);
                    reply.writeNoException();
                    return true;
                case 87:
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        componentNameCreateFromParcel86 = ComponentName.CREATOR.createFromParcel(data);
                    } else {
                        componentNameCreateFromParcel86 = null;
                    }
                    String _arg158 = data.readString();
                    setCertInstallerPackage(componentNameCreateFromParcel86, _arg158);
                    reply.writeNoException();
                    return true;
                case 88:
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        componentNameCreateFromParcel85 = ComponentName.CREATOR.createFromParcel(data);
                    } else {
                        componentNameCreateFromParcel85 = null;
                    }
                    String _result49 = getCertInstallerPackage(componentNameCreateFromParcel85);
                    reply.writeNoException();
                    reply.writeString(_result49);
                    return true;
                case 89:
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        componentNameCreateFromParcel84 = ComponentName.CREATOR.createFromParcel(data);
                    } else {
                        componentNameCreateFromParcel84 = null;
                    }
                    String _arg159 = data.readString();
                    boolean _arg239 = data.readInt() != 0;
                    boolean _result50 = setAlwaysOnVpnPackage(componentNameCreateFromParcel84, _arg159, _arg239);
                    reply.writeNoException();
                    reply.writeInt(_result50 ? 1 : 0);
                    return true;
                case 90:
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        componentNameCreateFromParcel83 = ComponentName.CREATOR.createFromParcel(data);
                    } else {
                        componentNameCreateFromParcel83 = null;
                    }
                    String _result51 = getAlwaysOnVpnPackage(componentNameCreateFromParcel83);
                    reply.writeNoException();
                    reply.writeString(_result51);
                    return true;
                case 91:
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        componentNameCreateFromParcel81 = ComponentName.CREATOR.createFromParcel(data);
                    } else {
                        componentNameCreateFromParcel81 = null;
                    }
                    if (data.readInt() != 0) {
                        intentFilterCreateFromParcel2 = IntentFilter.CREATOR.createFromParcel(data);
                    } else {
                        intentFilterCreateFromParcel2 = null;
                    }
                    if (data.readInt() != 0) {
                        componentNameCreateFromParcel82 = ComponentName.CREATOR.createFromParcel(data);
                    } else {
                        componentNameCreateFromParcel82 = null;
                    }
                    addPersistentPreferredActivity(componentNameCreateFromParcel81, intentFilterCreateFromParcel2, componentNameCreateFromParcel82);
                    reply.writeNoException();
                    return true;
                case 92:
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        componentNameCreateFromParcel80 = ComponentName.CREATOR.createFromParcel(data);
                    } else {
                        componentNameCreateFromParcel80 = null;
                    }
                    String _arg160 = data.readString();
                    clearPackagePersistentPreferredActivities(componentNameCreateFromParcel80, _arg160);
                    reply.writeNoException();
                    return true;
                case 93:
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        componentNameCreateFromParcel79 = ComponentName.CREATOR.createFromParcel(data);
                    } else {
                        componentNameCreateFromParcel79 = null;
                    }
                    String _arg161 = data.readString();
                    if (data.readInt() != 0) {
                        bundleCreateFromParcel = Bundle.CREATOR.createFromParcel(data);
                    } else {
                        bundleCreateFromParcel = null;
                    }
                    setApplicationRestrictions(componentNameCreateFromParcel79, _arg161, bundleCreateFromParcel);
                    reply.writeNoException();
                    return true;
                case 94:
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        componentNameCreateFromParcel78 = ComponentName.CREATOR.createFromParcel(data);
                    } else {
                        componentNameCreateFromParcel78 = null;
                    }
                    String _arg162 = data.readString();
                    Bundle _result52 = getApplicationRestrictions(componentNameCreateFromParcel78, _arg162);
                    reply.writeNoException();
                    if (_result52 != null) {
                        reply.writeInt(1);
                        _result52.writeToParcel(reply, 1);
                        return true;
                    }
                    reply.writeInt(0);
                    return true;
                case 95:
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        componentNameCreateFromParcel77 = ComponentName.CREATOR.createFromParcel(data);
                    } else {
                        componentNameCreateFromParcel77 = null;
                    }
                    String _arg163 = data.readString();
                    boolean _result53 = setApplicationRestrictionsManagingPackage(componentNameCreateFromParcel77, _arg163);
                    reply.writeNoException();
                    reply.writeInt(_result53 ? 1 : 0);
                    return true;
                case 96:
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        componentNameCreateFromParcel76 = ComponentName.CREATOR.createFromParcel(data);
                    } else {
                        componentNameCreateFromParcel76 = null;
                    }
                    String _result54 = getApplicationRestrictionsManagingPackage(componentNameCreateFromParcel76);
                    reply.writeNoException();
                    reply.writeString(_result54);
                    return true;
                case 97:
                    data.enforceInterface(DESCRIPTOR);
                    boolean _result55 = isCallerApplicationRestrictionsManagingPackage();
                    reply.writeNoException();
                    reply.writeInt(_result55 ? 1 : 0);
                    return true;
                case 98:
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        componentNameCreateFromParcel74 = ComponentName.CREATOR.createFromParcel(data);
                    } else {
                        componentNameCreateFromParcel74 = null;
                    }
                    if (data.readInt() != 0) {
                        componentNameCreateFromParcel75 = ComponentName.CREATOR.createFromParcel(data);
                    } else {
                        componentNameCreateFromParcel75 = null;
                    }
                    setRestrictionsProvider(componentNameCreateFromParcel74, componentNameCreateFromParcel75);
                    reply.writeNoException();
                    return true;
                case 99:
                    data.enforceInterface(DESCRIPTOR);
                    int _arg028 = data.readInt();
                    ComponentName _result56 = getRestrictionsProvider(_arg028);
                    reply.writeNoException();
                    if (_result56 != null) {
                        reply.writeInt(1);
                        _result56.writeToParcel(reply, 1);
                        return true;
                    }
                    reply.writeInt(0);
                    return true;
                case 100:
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        componentNameCreateFromParcel73 = ComponentName.CREATOR.createFromParcel(data);
                    } else {
                        componentNameCreateFromParcel73 = null;
                    }
                    String _arg164 = data.readString();
                    boolean _arg240 = data.readInt() != 0;
                    setUserRestriction(componentNameCreateFromParcel73, _arg164, _arg240);
                    reply.writeNoException();
                    return true;
                case 101:
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        componentNameCreateFromParcel72 = ComponentName.CREATOR.createFromParcel(data);
                    } else {
                        componentNameCreateFromParcel72 = null;
                    }
                    Bundle _result57 = getUserRestrictions(componentNameCreateFromParcel72);
                    reply.writeNoException();
                    if (_result57 != null) {
                        reply.writeInt(1);
                        _result57.writeToParcel(reply, 1);
                        return true;
                    }
                    reply.writeInt(0);
                    return true;
                case 102:
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        componentNameCreateFromParcel71 = ComponentName.CREATOR.createFromParcel(data);
                    } else {
                        componentNameCreateFromParcel71 = null;
                    }
                    if (data.readInt() != 0) {
                        intentFilterCreateFromParcel = IntentFilter.CREATOR.createFromParcel(data);
                    } else {
                        intentFilterCreateFromParcel = null;
                    }
                    int _arg241 = data.readInt();
                    addCrossProfileIntentFilter(componentNameCreateFromParcel71, intentFilterCreateFromParcel, _arg241);
                    reply.writeNoException();
                    return true;
                case 103:
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        componentNameCreateFromParcel70 = ComponentName.CREATOR.createFromParcel(data);
                    } else {
                        componentNameCreateFromParcel70 = null;
                    }
                    clearCrossProfileIntentFilters(componentNameCreateFromParcel70);
                    reply.writeNoException();
                    return true;
                case 104:
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        componentNameCreateFromParcel69 = ComponentName.CREATOR.createFromParcel(data);
                    } else {
                        componentNameCreateFromParcel69 = null;
                    }
                    ClassLoader cl = getClass().getClassLoader();
                    List _arg165 = data.readArrayList(cl);
                    boolean _result58 = setPermittedAccessibilityServices(componentNameCreateFromParcel69, _arg165);
                    reply.writeNoException();
                    reply.writeInt(_result58 ? 1 : 0);
                    return true;
                case 105:
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        componentNameCreateFromParcel68 = ComponentName.CREATOR.createFromParcel(data);
                    } else {
                        componentNameCreateFromParcel68 = null;
                    }
                    List _result59 = getPermittedAccessibilityServices(componentNameCreateFromParcel68);
                    reply.writeNoException();
                    reply.writeList(_result59);
                    return true;
                case 106:
                    data.enforceInterface(DESCRIPTOR);
                    int _arg029 = data.readInt();
                    List _result60 = getPermittedAccessibilityServicesForUser(_arg029);
                    reply.writeNoException();
                    reply.writeList(_result60);
                    return true;
                case 107:
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        componentNameCreateFromParcel67 = ComponentName.CREATOR.createFromParcel(data);
                    } else {
                        componentNameCreateFromParcel67 = null;
                    }
                    String _arg166 = data.readString();
                    int _arg242 = data.readInt();
                    boolean _result61 = isAccessibilityServicePermittedByAdmin(componentNameCreateFromParcel67, _arg166, _arg242);
                    reply.writeNoException();
                    reply.writeInt(_result61 ? 1 : 0);
                    return true;
                case 108:
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        componentNameCreateFromParcel66 = ComponentName.CREATOR.createFromParcel(data);
                    } else {
                        componentNameCreateFromParcel66 = null;
                    }
                    ClassLoader cl2 = getClass().getClassLoader();
                    List _arg167 = data.readArrayList(cl2);
                    boolean _result62 = setPermittedInputMethods(componentNameCreateFromParcel66, _arg167);
                    reply.writeNoException();
                    reply.writeInt(_result62 ? 1 : 0);
                    return true;
                case 109:
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        componentNameCreateFromParcel65 = ComponentName.CREATOR.createFromParcel(data);
                    } else {
                        componentNameCreateFromParcel65 = null;
                    }
                    List _result63 = getPermittedInputMethods(componentNameCreateFromParcel65);
                    reply.writeNoException();
                    reply.writeList(_result63);
                    return true;
                case 110:
                    data.enforceInterface(DESCRIPTOR);
                    List _result64 = getPermittedInputMethodsForCurrentUser();
                    reply.writeNoException();
                    reply.writeList(_result64);
                    return true;
                case 111:
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        componentNameCreateFromParcel64 = ComponentName.CREATOR.createFromParcel(data);
                    } else {
                        componentNameCreateFromParcel64 = null;
                    }
                    String _arg168 = data.readString();
                    int _arg243 = data.readInt();
                    boolean _result65 = isInputMethodPermittedByAdmin(componentNameCreateFromParcel64, _arg168, _arg243);
                    reply.writeNoException();
                    reply.writeInt(_result65 ? 1 : 0);
                    return true;
                case 112:
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        componentNameCreateFromParcel63 = ComponentName.CREATOR.createFromParcel(data);
                    } else {
                        componentNameCreateFromParcel63 = null;
                    }
                    String _arg169 = data.readString();
                    boolean _arg244 = data.readInt() != 0;
                    boolean _result66 = setApplicationHidden(componentNameCreateFromParcel63, _arg169, _arg244);
                    reply.writeNoException();
                    reply.writeInt(_result66 ? 1 : 0);
                    return true;
                case 113:
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        componentNameCreateFromParcel62 = ComponentName.CREATOR.createFromParcel(data);
                    } else {
                        componentNameCreateFromParcel62 = null;
                    }
                    String _arg170 = data.readString();
                    boolean _result67 = isApplicationHidden(componentNameCreateFromParcel62, _arg170);
                    reply.writeNoException();
                    reply.writeInt(_result67 ? 1 : 0);
                    return true;
                case 114:
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        componentNameCreateFromParcel60 = ComponentName.CREATOR.createFromParcel(data);
                    } else {
                        componentNameCreateFromParcel60 = null;
                    }
                    String _arg171 = data.readString();
                    if (data.readInt() != 0) {
                        componentNameCreateFromParcel61 = ComponentName.CREATOR.createFromParcel(data);
                    } else {
                        componentNameCreateFromParcel61 = null;
                    }
                    if (data.readInt() != 0) {
                        persistableBundleCreateFromParcel2 = PersistableBundle.CREATOR.createFromParcel(data);
                    } else {
                        persistableBundleCreateFromParcel2 = null;
                    }
                    int _arg43 = data.readInt();
                    UserHandle _result68 = createAndManageUser(componentNameCreateFromParcel60, _arg171, componentNameCreateFromParcel61, persistableBundleCreateFromParcel2, _arg43);
                    reply.writeNoException();
                    if (_result68 != null) {
                        reply.writeInt(1);
                        _result68.writeToParcel(reply, 1);
                        return true;
                    }
                    reply.writeInt(0);
                    return true;
                case 115:
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        componentNameCreateFromParcel59 = ComponentName.CREATOR.createFromParcel(data);
                    } else {
                        componentNameCreateFromParcel59 = null;
                    }
                    if (data.readInt() != 0) {
                        userHandleCreateFromParcel2 = UserHandle.CREATOR.createFromParcel(data);
                    } else {
                        userHandleCreateFromParcel2 = null;
                    }
                    boolean _result69 = removeUser(componentNameCreateFromParcel59, userHandleCreateFromParcel2);
                    reply.writeNoException();
                    reply.writeInt(_result69 ? 1 : 0);
                    return true;
                case 116:
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        componentNameCreateFromParcel58 = ComponentName.CREATOR.createFromParcel(data);
                    } else {
                        componentNameCreateFromParcel58 = null;
                    }
                    if (data.readInt() != 0) {
                        userHandleCreateFromParcel = UserHandle.CREATOR.createFromParcel(data);
                    } else {
                        userHandleCreateFromParcel = null;
                    }
                    boolean _result70 = switchUser(componentNameCreateFromParcel58, userHandleCreateFromParcel);
                    reply.writeNoException();
                    reply.writeInt(_result70 ? 1 : 0);
                    return true;
                case 117:
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        componentNameCreateFromParcel57 = ComponentName.CREATOR.createFromParcel(data);
                    } else {
                        componentNameCreateFromParcel57 = null;
                    }
                    String _arg172 = data.readString();
                    enableSystemApp(componentNameCreateFromParcel57, _arg172);
                    reply.writeNoException();
                    return true;
                case 118:
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        componentNameCreateFromParcel56 = ComponentName.CREATOR.createFromParcel(data);
                    } else {
                        componentNameCreateFromParcel56 = null;
                    }
                    if (data.readInt() != 0) {
                        intentCreateFromParcel2 = Intent.CREATOR.createFromParcel(data);
                    } else {
                        intentCreateFromParcel2 = null;
                    }
                    int _result71 = enableSystemAppWithIntent(componentNameCreateFromParcel56, intentCreateFromParcel2);
                    reply.writeNoException();
                    reply.writeInt(_result71);
                    return true;
                case 119:
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        componentNameCreateFromParcel55 = ComponentName.CREATOR.createFromParcel(data);
                    } else {
                        componentNameCreateFromParcel55 = null;
                    }
                    String _arg173 = data.readString();
                    boolean _arg245 = data.readInt() != 0;
                    setAccountManagementDisabled(componentNameCreateFromParcel55, _arg173, _arg245);
                    reply.writeNoException();
                    return true;
                case 120:
                    data.enforceInterface(DESCRIPTOR);
                    String[] _result72 = getAccountTypesWithManagementDisabled();
                    reply.writeNoException();
                    reply.writeStringArray(_result72);
                    return true;
                case 121:
                    data.enforceInterface(DESCRIPTOR);
                    int _arg030 = data.readInt();
                    String[] _result73 = getAccountTypesWithManagementDisabledAsUser(_arg030);
                    reply.writeNoException();
                    reply.writeStringArray(_result73);
                    return true;
                case 122:
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        componentNameCreateFromParcel54 = ComponentName.CREATOR.createFromParcel(data);
                    } else {
                        componentNameCreateFromParcel54 = null;
                    }
                    String[] _arg174 = data.createStringArray();
                    setLockTaskPackages(componentNameCreateFromParcel54, _arg174);
                    reply.writeNoException();
                    return true;
                case 123:
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        componentNameCreateFromParcel53 = ComponentName.CREATOR.createFromParcel(data);
                    } else {
                        componentNameCreateFromParcel53 = null;
                    }
                    String[] _result74 = getLockTaskPackages(componentNameCreateFromParcel53);
                    reply.writeNoException();
                    reply.writeStringArray(_result74);
                    return true;
                case 124:
                    data.enforceInterface(DESCRIPTOR);
                    String _arg031 = data.readString();
                    boolean _result75 = isLockTaskPermitted(_arg031);
                    reply.writeNoException();
                    reply.writeInt(_result75 ? 1 : 0);
                    return true;
                case 125:
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        componentNameCreateFromParcel52 = ComponentName.CREATOR.createFromParcel(data);
                    } else {
                        componentNameCreateFromParcel52 = null;
                    }
                    String _arg175 = data.readString();
                    String _arg246 = data.readString();
                    setGlobalSetting(componentNameCreateFromParcel52, _arg175, _arg246);
                    reply.writeNoException();
                    return true;
                case 126:
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        componentNameCreateFromParcel51 = ComponentName.CREATOR.createFromParcel(data);
                    } else {
                        componentNameCreateFromParcel51 = null;
                    }
                    String _arg176 = data.readString();
                    String _arg247 = data.readString();
                    setSecureSetting(componentNameCreateFromParcel51, _arg176, _arg247);
                    reply.writeNoException();
                    return true;
                case 127:
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        componentNameCreateFromParcel50 = ComponentName.CREATOR.createFromParcel(data);
                    } else {
                        componentNameCreateFromParcel50 = null;
                    }
                    boolean _arg177 = data.readInt() != 0;
                    setMasterVolumeMuted(componentNameCreateFromParcel50, _arg177);
                    reply.writeNoException();
                    return true;
                case 128:
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        componentNameCreateFromParcel49 = ComponentName.CREATOR.createFromParcel(data);
                    } else {
                        componentNameCreateFromParcel49 = null;
                    }
                    boolean _result76 = isMasterVolumeMuted(componentNameCreateFromParcel49);
                    reply.writeNoException();
                    reply.writeInt(_result76 ? 1 : 0);
                    return true;
                case 129:
                    data.enforceInterface(DESCRIPTOR);
                    boolean _arg032 = data.readInt() != 0;
                    String _arg178 = data.readString();
                    int _arg248 = data.readInt();
                    notifyLockTaskModeChanged(_arg032, _arg178, _arg248);
                    reply.writeNoException();
                    return true;
                case 130:
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        componentNameCreateFromParcel48 = ComponentName.CREATOR.createFromParcel(data);
                    } else {
                        componentNameCreateFromParcel48 = null;
                    }
                    String _arg179 = data.readString();
                    boolean _arg249 = data.readInt() != 0;
                    setUninstallBlocked(componentNameCreateFromParcel48, _arg179, _arg249);
                    reply.writeNoException();
                    return true;
                case 131:
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        componentNameCreateFromParcel47 = ComponentName.CREATOR.createFromParcel(data);
                    } else {
                        componentNameCreateFromParcel47 = null;
                    }
                    String _arg180 = data.readString();
                    boolean _result77 = isUninstallBlocked(componentNameCreateFromParcel47, _arg180);
                    reply.writeNoException();
                    reply.writeInt(_result77 ? 1 : 0);
                    return true;
                case 132:
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        componentNameCreateFromParcel46 = ComponentName.CREATOR.createFromParcel(data);
                    } else {
                        componentNameCreateFromParcel46 = null;
                    }
                    boolean _arg181 = data.readInt() != 0;
                    setCrossProfileCallerIdDisabled(componentNameCreateFromParcel46, _arg181);
                    reply.writeNoException();
                    return true;
                case 133:
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        componentNameCreateFromParcel45 = ComponentName.CREATOR.createFromParcel(data);
                    } else {
                        componentNameCreateFromParcel45 = null;
                    }
                    boolean _result78 = getCrossProfileCallerIdDisabled(componentNameCreateFromParcel45);
                    reply.writeNoException();
                    reply.writeInt(_result78 ? 1 : 0);
                    return true;
                case 134:
                    data.enforceInterface(DESCRIPTOR);
                    int _arg033 = data.readInt();
                    boolean _result79 = getCrossProfileCallerIdDisabledForUser(_arg033);
                    reply.writeNoException();
                    reply.writeInt(_result79 ? 1 : 0);
                    return true;
                case 135:
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        componentNameCreateFromParcel44 = ComponentName.CREATOR.createFromParcel(data);
                    } else {
                        componentNameCreateFromParcel44 = null;
                    }
                    boolean _arg182 = data.readInt() != 0;
                    setCrossProfileContactsSearchDisabled(componentNameCreateFromParcel44, _arg182);
                    reply.writeNoException();
                    return true;
                case 136:
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        componentNameCreateFromParcel43 = ComponentName.CREATOR.createFromParcel(data);
                    } else {
                        componentNameCreateFromParcel43 = null;
                    }
                    boolean _result80 = getCrossProfileContactsSearchDisabled(componentNameCreateFromParcel43);
                    reply.writeNoException();
                    reply.writeInt(_result80 ? 1 : 0);
                    return true;
                case 137:
                    data.enforceInterface(DESCRIPTOR);
                    int _arg034 = data.readInt();
                    boolean _result81 = getCrossProfileContactsSearchDisabledForUser(_arg034);
                    reply.writeNoException();
                    reply.writeInt(_result81 ? 1 : 0);
                    return true;
                case 138:
                    data.enforceInterface(DESCRIPTOR);
                    String _arg035 = data.readString();
                    long _arg183 = data.readLong();
                    boolean _arg250 = data.readInt() != 0;
                    long _arg34 = data.readLong();
                    if (data.readInt() != 0) {
                        intentCreateFromParcel = Intent.CREATOR.createFromParcel(data);
                    } else {
                        intentCreateFromParcel = null;
                    }
                    startManagedQuickContact(_arg035, _arg183, _arg250, _arg34, intentCreateFromParcel);
                    reply.writeNoException();
                    return true;
                case 139:
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        componentNameCreateFromParcel42 = ComponentName.CREATOR.createFromParcel(data);
                    } else {
                        componentNameCreateFromParcel42 = null;
                    }
                    boolean _arg184 = data.readInt() != 0;
                    setBluetoothContactSharingDisabled(componentNameCreateFromParcel42, _arg184);
                    reply.writeNoException();
                    return true;
                case 140:
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        componentNameCreateFromParcel41 = ComponentName.CREATOR.createFromParcel(data);
                    } else {
                        componentNameCreateFromParcel41 = null;
                    }
                    boolean _result82 = getBluetoothContactSharingDisabled(componentNameCreateFromParcel41);
                    reply.writeNoException();
                    reply.writeInt(_result82 ? 1 : 0);
                    return true;
                case 141:
                    data.enforceInterface(DESCRIPTOR);
                    int _arg036 = data.readInt();
                    boolean _result83 = getBluetoothContactSharingDisabledForUser(_arg036);
                    reply.writeNoException();
                    reply.writeInt(_result83 ? 1 : 0);
                    return true;
                case 142:
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        componentNameCreateFromParcel39 = ComponentName.CREATOR.createFromParcel(data);
                    } else {
                        componentNameCreateFromParcel39 = null;
                    }
                    if (data.readInt() != 0) {
                        componentNameCreateFromParcel40 = ComponentName.CREATOR.createFromParcel(data);
                    } else {
                        componentNameCreateFromParcel40 = null;
                    }
                    if (data.readInt() != 0) {
                        persistableBundleCreateFromParcel = PersistableBundle.CREATOR.createFromParcel(data);
                    } else {
                        persistableBundleCreateFromParcel = null;
                    }
                    boolean _arg35 = data.readInt() != 0;
                    setTrustAgentConfiguration(componentNameCreateFromParcel39, componentNameCreateFromParcel40, persistableBundleCreateFromParcel, _arg35);
                    reply.writeNoException();
                    return true;
                case 143:
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        componentNameCreateFromParcel37 = ComponentName.CREATOR.createFromParcel(data);
                    } else {
                        componentNameCreateFromParcel37 = null;
                    }
                    if (data.readInt() != 0) {
                        componentNameCreateFromParcel38 = ComponentName.CREATOR.createFromParcel(data);
                    } else {
                        componentNameCreateFromParcel38 = null;
                    }
                    int _arg251 = data.readInt();
                    boolean _arg36 = data.readInt() != 0;
                    List<PersistableBundle> _result84 = getTrustAgentConfiguration(componentNameCreateFromParcel37, componentNameCreateFromParcel38, _arg251, _arg36);
                    reply.writeNoException();
                    reply.writeTypedList(_result84);
                    return true;
                case 144:
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        componentNameCreateFromParcel36 = ComponentName.CREATOR.createFromParcel(data);
                    } else {
                        componentNameCreateFromParcel36 = null;
                    }
                    String _arg185 = data.readString();
                    boolean _result85 = addCrossProfileWidgetProvider(componentNameCreateFromParcel36, _arg185);
                    reply.writeNoException();
                    reply.writeInt(_result85 ? 1 : 0);
                    return true;
                case 145:
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        componentNameCreateFromParcel35 = ComponentName.CREATOR.createFromParcel(data);
                    } else {
                        componentNameCreateFromParcel35 = null;
                    }
                    String _arg186 = data.readString();
                    boolean _result86 = removeCrossProfileWidgetProvider(componentNameCreateFromParcel35, _arg186);
                    reply.writeNoException();
                    reply.writeInt(_result86 ? 1 : 0);
                    return true;
                case 146:
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        componentNameCreateFromParcel34 = ComponentName.CREATOR.createFromParcel(data);
                    } else {
                        componentNameCreateFromParcel34 = null;
                    }
                    List<String> _result87 = getCrossProfileWidgetProviders(componentNameCreateFromParcel34);
                    reply.writeNoException();
                    reply.writeStringList(_result87);
                    return true;
                case 147:
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        componentNameCreateFromParcel33 = ComponentName.CREATOR.createFromParcel(data);
                    } else {
                        componentNameCreateFromParcel33 = null;
                    }
                    boolean _arg187 = data.readInt() != 0;
                    setAutoTimeRequired(componentNameCreateFromParcel33, _arg187);
                    reply.writeNoException();
                    return true;
                case 148:
                    data.enforceInterface(DESCRIPTOR);
                    boolean _result88 = getAutoTimeRequired();
                    reply.writeNoException();
                    reply.writeInt(_result88 ? 1 : 0);
                    return true;
                case 149:
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        componentNameCreateFromParcel32 = ComponentName.CREATOR.createFromParcel(data);
                    } else {
                        componentNameCreateFromParcel32 = null;
                    }
                    boolean _arg188 = data.readInt() != 0;
                    setForceEphemeralUsers(componentNameCreateFromParcel32, _arg188);
                    reply.writeNoException();
                    return true;
                case 150:
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        componentNameCreateFromParcel31 = ComponentName.CREATOR.createFromParcel(data);
                    } else {
                        componentNameCreateFromParcel31 = null;
                    }
                    boolean _result89 = getForceEphemeralUsers(componentNameCreateFromParcel31);
                    reply.writeNoException();
                    reply.writeInt(_result89 ? 1 : 0);
                    return true;
                case 151:
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        componentNameCreateFromParcel30 = ComponentName.CREATOR.createFromParcel(data);
                    } else {
                        componentNameCreateFromParcel30 = null;
                    }
                    int _arg189 = data.readInt();
                    boolean _result90 = isRemovingAdmin(componentNameCreateFromParcel30, _arg189);
                    reply.writeNoException();
                    reply.writeInt(_result90 ? 1 : 0);
                    return true;
                case 152:
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        componentNameCreateFromParcel29 = ComponentName.CREATOR.createFromParcel(data);
                    } else {
                        componentNameCreateFromParcel29 = null;
                    }
                    if (data.readInt() != 0) {
                        bitmapCreateFromParcel = Bitmap.CREATOR.createFromParcel(data);
                    } else {
                        bitmapCreateFromParcel = null;
                    }
                    setUserIcon(componentNameCreateFromParcel29, bitmapCreateFromParcel);
                    reply.writeNoException();
                    return true;
                case 153:
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        componentNameCreateFromParcel28 = ComponentName.CREATOR.createFromParcel(data);
                    } else {
                        componentNameCreateFromParcel28 = null;
                    }
                    if (data.readInt() != 0) {
                        systemUpdatePolicyCreateFromParcel = SystemUpdatePolicy.CREATOR.createFromParcel(data);
                    } else {
                        systemUpdatePolicyCreateFromParcel = null;
                    }
                    setSystemUpdatePolicy(componentNameCreateFromParcel28, systemUpdatePolicyCreateFromParcel);
                    reply.writeNoException();
                    return true;
                case 154:
                    data.enforceInterface(DESCRIPTOR);
                    SystemUpdatePolicy _result91 = getSystemUpdatePolicy();
                    reply.writeNoException();
                    if (_result91 != null) {
                        reply.writeInt(1);
                        _result91.writeToParcel(reply, 1);
                        return true;
                    }
                    reply.writeInt(0);
                    return true;
                case 155:
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        componentNameCreateFromParcel27 = ComponentName.CREATOR.createFromParcel(data);
                    } else {
                        componentNameCreateFromParcel27 = null;
                    }
                    boolean _arg190 = data.readInt() != 0;
                    boolean _result92 = setKeyguardDisabled(componentNameCreateFromParcel27, _arg190);
                    reply.writeNoException();
                    reply.writeInt(_result92 ? 1 : 0);
                    return true;
                case 156:
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        componentNameCreateFromParcel26 = ComponentName.CREATOR.createFromParcel(data);
                    } else {
                        componentNameCreateFromParcel26 = null;
                    }
                    boolean _arg191 = data.readInt() != 0;
                    boolean _result93 = setStatusBarDisabled(componentNameCreateFromParcel26, _arg191);
                    reply.writeNoException();
                    reply.writeInt(_result93 ? 1 : 0);
                    return true;
                case 157:
                    data.enforceInterface(DESCRIPTOR);
                    boolean _result94 = getDoNotAskCredentialsOnBoot();
                    reply.writeNoException();
                    reply.writeInt(_result94 ? 1 : 0);
                    return true;
                case 158:
                    data.enforceInterface(DESCRIPTOR);
                    long _arg037 = data.readLong();
                    notifyPendingSystemUpdate(_arg037);
                    reply.writeNoException();
                    return true;
                case 159:
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        componentNameCreateFromParcel25 = ComponentName.CREATOR.createFromParcel(data);
                    } else {
                        componentNameCreateFromParcel25 = null;
                    }
                    int _arg192 = data.readInt();
                    setPermissionPolicy(componentNameCreateFromParcel25, _arg192);
                    reply.writeNoException();
                    return true;
                case 160:
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        componentNameCreateFromParcel24 = ComponentName.CREATOR.createFromParcel(data);
                    } else {
                        componentNameCreateFromParcel24 = null;
                    }
                    int _result95 = getPermissionPolicy(componentNameCreateFromParcel24);
                    reply.writeNoException();
                    reply.writeInt(_result95);
                    return true;
                case 161:
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        componentNameCreateFromParcel23 = ComponentName.CREATOR.createFromParcel(data);
                    } else {
                        componentNameCreateFromParcel23 = null;
                    }
                    String _arg193 = data.readString();
                    String _arg252 = data.readString();
                    int _arg37 = data.readInt();
                    boolean _result96 = setPermissionGrantState(componentNameCreateFromParcel23, _arg193, _arg252, _arg37);
                    reply.writeNoException();
                    reply.writeInt(_result96 ? 1 : 0);
                    return true;
                case 162:
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        componentNameCreateFromParcel22 = ComponentName.CREATOR.createFromParcel(data);
                    } else {
                        componentNameCreateFromParcel22 = null;
                    }
                    String _arg194 = data.readString();
                    String _arg253 = data.readString();
                    int _result97 = getPermissionGrantState(componentNameCreateFromParcel22, _arg194, _arg253);
                    reply.writeNoException();
                    reply.writeInt(_result97);
                    return true;
                case 163:
                    data.enforceInterface(DESCRIPTOR);
                    String _arg038 = data.readString();
                    boolean _result98 = isProvisioningAllowed(_arg038);
                    reply.writeNoException();
                    reply.writeInt(_result98 ? 1 : 0);
                    return true;
                case 164:
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        componentNameCreateFromParcel21 = ComponentName.CREATOR.createFromParcel(data);
                    } else {
                        componentNameCreateFromParcel21 = null;
                    }
                    List<String> _arg195 = data.createStringArrayList();
                    setKeepUninstalledPackages(componentNameCreateFromParcel21, _arg195);
                    reply.writeNoException();
                    return true;
                case 165:
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        componentNameCreateFromParcel20 = ComponentName.CREATOR.createFromParcel(data);
                    } else {
                        componentNameCreateFromParcel20 = null;
                    }
                    List<String> _result99 = getKeepUninstalledPackages(componentNameCreateFromParcel20);
                    reply.writeNoException();
                    reply.writeStringList(_result99);
                    return true;
                case 166:
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        componentNameCreateFromParcel19 = ComponentName.CREATOR.createFromParcel(data);
                    } else {
                        componentNameCreateFromParcel19 = null;
                    }
                    boolean _result100 = isManagedProfile(componentNameCreateFromParcel19);
                    reply.writeNoException();
                    reply.writeInt(_result100 ? 1 : 0);
                    return true;
                case 167:
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        componentNameCreateFromParcel18 = ComponentName.CREATOR.createFromParcel(data);
                    } else {
                        componentNameCreateFromParcel18 = null;
                    }
                    boolean _result101 = isSystemOnlyUser(componentNameCreateFromParcel18);
                    reply.writeNoException();
                    reply.writeInt(_result101 ? 1 : 0);
                    return true;
                case 168:
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        componentNameCreateFromParcel17 = ComponentName.CREATOR.createFromParcel(data);
                    } else {
                        componentNameCreateFromParcel17 = null;
                    }
                    String _result102 = getWifiMacAddress(componentNameCreateFromParcel17);
                    reply.writeNoException();
                    reply.writeString(_result102);
                    return true;
                case 169:
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        componentNameCreateFromParcel16 = ComponentName.CREATOR.createFromParcel(data);
                    } else {
                        componentNameCreateFromParcel16 = null;
                    }
                    reboot(componentNameCreateFromParcel16);
                    reply.writeNoException();
                    return true;
                case 170:
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        componentNameCreateFromParcel15 = ComponentName.CREATOR.createFromParcel(data);
                    } else {
                        componentNameCreateFromParcel15 = null;
                    }
                    if (data.readInt() != 0) {
                        charSequence3 = (CharSequence) TextUtils.CHAR_SEQUENCE_CREATOR.createFromParcel(data);
                    } else {
                        charSequence3 = null;
                    }
                    setShortSupportMessage(componentNameCreateFromParcel15, charSequence3);
                    reply.writeNoException();
                    return true;
                case 171:
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        componentNameCreateFromParcel14 = ComponentName.CREATOR.createFromParcel(data);
                    } else {
                        componentNameCreateFromParcel14 = null;
                    }
                    CharSequence _result103 = getShortSupportMessage(componentNameCreateFromParcel14);
                    reply.writeNoException();
                    if (_result103 != null) {
                        reply.writeInt(1);
                        TextUtils.writeToParcel(_result103, reply, 1);
                        return true;
                    }
                    reply.writeInt(0);
                    return true;
                case 172:
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        componentNameCreateFromParcel13 = ComponentName.CREATOR.createFromParcel(data);
                    } else {
                        componentNameCreateFromParcel13 = null;
                    }
                    if (data.readInt() != 0) {
                        charSequence2 = (CharSequence) TextUtils.CHAR_SEQUENCE_CREATOR.createFromParcel(data);
                    } else {
                        charSequence2 = null;
                    }
                    setLongSupportMessage(componentNameCreateFromParcel13, charSequence2);
                    reply.writeNoException();
                    return true;
                case 173:
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        componentNameCreateFromParcel12 = ComponentName.CREATOR.createFromParcel(data);
                    } else {
                        componentNameCreateFromParcel12 = null;
                    }
                    CharSequence _result104 = getLongSupportMessage(componentNameCreateFromParcel12);
                    reply.writeNoException();
                    if (_result104 != null) {
                        reply.writeInt(1);
                        TextUtils.writeToParcel(_result104, reply, 1);
                        return true;
                    }
                    reply.writeInt(0);
                    return true;
                case 174:
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        componentNameCreateFromParcel11 = ComponentName.CREATOR.createFromParcel(data);
                    } else {
                        componentNameCreateFromParcel11 = null;
                    }
                    int _arg196 = data.readInt();
                    CharSequence _result105 = getShortSupportMessageForUser(componentNameCreateFromParcel11, _arg196);
                    reply.writeNoException();
                    if (_result105 != null) {
                        reply.writeInt(1);
                        TextUtils.writeToParcel(_result105, reply, 1);
                        return true;
                    }
                    reply.writeInt(0);
                    return true;
                case 175:
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        componentNameCreateFromParcel10 = ComponentName.CREATOR.createFromParcel(data);
                    } else {
                        componentNameCreateFromParcel10 = null;
                    }
                    int _arg197 = data.readInt();
                    CharSequence _result106 = getLongSupportMessageForUser(componentNameCreateFromParcel10, _arg197);
                    reply.writeNoException();
                    if (_result106 != null) {
                        reply.writeInt(1);
                        TextUtils.writeToParcel(_result106, reply, 1);
                        return true;
                    }
                    reply.writeInt(0);
                    return true;
                case 176:
                    data.enforceInterface(DESCRIPTOR);
                    int _arg039 = data.readInt();
                    boolean _result107 = isSeparateProfileChallengeAllowed(_arg039);
                    reply.writeNoException();
                    reply.writeInt(_result107 ? 1 : 0);
                    return true;
                case 177:
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        componentNameCreateFromParcel9 = ComponentName.CREATOR.createFromParcel(data);
                    } else {
                        componentNameCreateFromParcel9 = null;
                    }
                    int _arg198 = data.readInt();
                    setOrganizationColor(componentNameCreateFromParcel9, _arg198);
                    reply.writeNoException();
                    return true;
                case 178:
                    data.enforceInterface(DESCRIPTOR);
                    int _arg040 = data.readInt();
                    int _arg199 = data.readInt();
                    setOrganizationColorForUser(_arg040, _arg199);
                    reply.writeNoException();
                    return true;
                case 179:
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        componentNameCreateFromParcel8 = ComponentName.CREATOR.createFromParcel(data);
                    } else {
                        componentNameCreateFromParcel8 = null;
                    }
                    int _result108 = getOrganizationColor(componentNameCreateFromParcel8);
                    reply.writeNoException();
                    reply.writeInt(_result108);
                    return true;
                case 180:
                    data.enforceInterface(DESCRIPTOR);
                    int _arg041 = data.readInt();
                    int _result109 = getOrganizationColorForUser(_arg041);
                    reply.writeNoException();
                    reply.writeInt(_result109);
                    return true;
                case 181:
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        componentNameCreateFromParcel7 = ComponentName.CREATOR.createFromParcel(data);
                    } else {
                        componentNameCreateFromParcel7 = null;
                    }
                    if (data.readInt() != 0) {
                        charSequence = (CharSequence) TextUtils.CHAR_SEQUENCE_CREATOR.createFromParcel(data);
                    } else {
                        charSequence = null;
                    }
                    setOrganizationName(componentNameCreateFromParcel7, charSequence);
                    reply.writeNoException();
                    return true;
                case 182:
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        componentNameCreateFromParcel6 = ComponentName.CREATOR.createFromParcel(data);
                    } else {
                        componentNameCreateFromParcel6 = null;
                    }
                    CharSequence _result110 = getOrganizationName(componentNameCreateFromParcel6);
                    reply.writeNoException();
                    if (_result110 != null) {
                        reply.writeInt(1);
                        TextUtils.writeToParcel(_result110, reply, 1);
                        return true;
                    }
                    reply.writeInt(0);
                    return true;
                case 183:
                    data.enforceInterface(DESCRIPTOR);
                    int _arg042 = data.readInt();
                    CharSequence _result111 = getOrganizationNameForUser(_arg042);
                    reply.writeNoException();
                    if (_result111 != null) {
                        reply.writeInt(1);
                        TextUtils.writeToParcel(_result111, reply, 1);
                        return true;
                    }
                    reply.writeInt(0);
                    return true;
                case 184:
                    data.enforceInterface(DESCRIPTOR);
                    int _result112 = getUserProvisioningState();
                    reply.writeNoException();
                    reply.writeInt(_result112);
                    return true;
                case 185:
                    data.enforceInterface(DESCRIPTOR);
                    int _arg043 = data.readInt();
                    int _arg1100 = data.readInt();
                    setUserProvisioningState(_arg043, _arg1100);
                    reply.writeNoException();
                    return true;
                case 186:
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        componentNameCreateFromParcel5 = ComponentName.CREATOR.createFromParcel(data);
                    } else {
                        componentNameCreateFromParcel5 = null;
                    }
                    List<String> _arg1101 = data.createStringArrayList();
                    setAffiliationIds(componentNameCreateFromParcel5, _arg1101);
                    reply.writeNoException();
                    return true;
                case 187:
                    data.enforceInterface(DESCRIPTOR);
                    boolean _result113 = isAffiliatedUser();
                    reply.writeNoException();
                    reply.writeInt(_result113 ? 1 : 0);
                    return true;
                case 188:
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        componentNameCreateFromParcel4 = ComponentName.CREATOR.createFromParcel(data);
                    } else {
                        componentNameCreateFromParcel4 = null;
                    }
                    boolean _arg1102 = data.readInt() != 0;
                    setSecurityLoggingEnabled(componentNameCreateFromParcel4, _arg1102);
                    reply.writeNoException();
                    return true;
                case 189:
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        componentNameCreateFromParcel3 = ComponentName.CREATOR.createFromParcel(data);
                    } else {
                        componentNameCreateFromParcel3 = null;
                    }
                    boolean _result114 = isSecurityLoggingEnabled(componentNameCreateFromParcel3);
                    reply.writeNoException();
                    reply.writeInt(_result114 ? 1 : 0);
                    return true;
                case 190:
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        componentNameCreateFromParcel2 = ComponentName.CREATOR.createFromParcel(data);
                    } else {
                        componentNameCreateFromParcel2 = null;
                    }
                    ParceledListSlice _result115 = retrieveSecurityLogs(componentNameCreateFromParcel2);
                    reply.writeNoException();
                    if (_result115 != null) {
                        reply.writeInt(1);
                        _result115.writeToParcel(reply, 1);
                        return true;
                    }
                    reply.writeInt(0);
                    return true;
                case 191:
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        componentNameCreateFromParcel = ComponentName.CREATOR.createFromParcel(data);
                    } else {
                        componentNameCreateFromParcel = null;
                    }
                    ParceledListSlice _result116 = retrievePreRebootSecurityLogs(componentNameCreateFromParcel);
                    reply.writeNoException();
                    if (_result116 != null) {
                        reply.writeInt(1);
                        _result116.writeToParcel(reply, 1);
                        return true;
                    }
                    reply.writeInt(0);
                    return true;
                case 192:
                    data.enforceInterface(DESCRIPTOR);
                    String _arg044 = data.readString();
                    boolean _result117 = isUninstallInQueue(_arg044);
                    reply.writeNoException();
                    reply.writeInt(_result117 ? 1 : 0);
                    return true;
                case 193:
                    data.enforceInterface(DESCRIPTOR);
                    String _arg045 = data.readString();
                    uninstallPackageWithActiveAdmins(_arg045);
                    reply.writeNoException();
                    return true;
                case IBinder.INTERFACE_TRANSACTION:
                    reply.writeString(DESCRIPTOR);
                    return true;
                default:
                    return super.onTransact(code, data, reply, flags);
            }
        }

        private static class Proxy implements IDevicePolicyManager {
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
            public void setPasswordQuality(ComponentName who, int quality, boolean parent) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (who != null) {
                        _data.writeInt(1);
                        who.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeInt(quality);
                    _data.writeInt(parent ? 1 : 0);
                    this.mRemote.transact(1, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public int getPasswordQuality(ComponentName who, int userHandle, boolean parent) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (who != null) {
                        _data.writeInt(1);
                        who.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeInt(userHandle);
                    _data.writeInt(parent ? 1 : 0);
                    this.mRemote.transact(2, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void setPasswordMinimumLength(ComponentName who, int length, boolean parent) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (who != null) {
                        _data.writeInt(1);
                        who.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeInt(length);
                    _data.writeInt(parent ? 1 : 0);
                    this.mRemote.transact(3, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public int getPasswordMinimumLength(ComponentName who, int userHandle, boolean parent) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (who != null) {
                        _data.writeInt(1);
                        who.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeInt(userHandle);
                    _data.writeInt(parent ? 1 : 0);
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
            public void setPasswordMinimumUpperCase(ComponentName who, int length, boolean parent) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (who != null) {
                        _data.writeInt(1);
                        who.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeInt(length);
                    _data.writeInt(parent ? 1 : 0);
                    this.mRemote.transact(5, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public int getPasswordMinimumUpperCase(ComponentName who, int userHandle, boolean parent) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (who != null) {
                        _data.writeInt(1);
                        who.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeInt(userHandle);
                    _data.writeInt(parent ? 1 : 0);
                    this.mRemote.transact(6, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void setPasswordMinimumLowerCase(ComponentName who, int length, boolean parent) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (who != null) {
                        _data.writeInt(1);
                        who.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeInt(length);
                    _data.writeInt(parent ? 1 : 0);
                    this.mRemote.transact(7, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public int getPasswordMinimumLowerCase(ComponentName who, int userHandle, boolean parent) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (who != null) {
                        _data.writeInt(1);
                        who.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeInt(userHandle);
                    _data.writeInt(parent ? 1 : 0);
                    this.mRemote.transact(8, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void setPasswordMinimumLetters(ComponentName who, int length, boolean parent) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (who != null) {
                        _data.writeInt(1);
                        who.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeInt(length);
                    _data.writeInt(parent ? 1 : 0);
                    this.mRemote.transact(9, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public int getPasswordMinimumLetters(ComponentName who, int userHandle, boolean parent) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (who != null) {
                        _data.writeInt(1);
                        who.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeInt(userHandle);
                    _data.writeInt(parent ? 1 : 0);
                    this.mRemote.transact(10, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void setPasswordMinimumNumeric(ComponentName who, int length, boolean parent) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (who != null) {
                        _data.writeInt(1);
                        who.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeInt(length);
                    _data.writeInt(parent ? 1 : 0);
                    this.mRemote.transact(11, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public int getPasswordMinimumNumeric(ComponentName who, int userHandle, boolean parent) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (who != null) {
                        _data.writeInt(1);
                        who.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeInt(userHandle);
                    _data.writeInt(parent ? 1 : 0);
                    this.mRemote.transact(12, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void setPasswordMinimumSymbols(ComponentName who, int length, boolean parent) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (who != null) {
                        _data.writeInt(1);
                        who.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeInt(length);
                    _data.writeInt(parent ? 1 : 0);
                    this.mRemote.transact(13, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public int getPasswordMinimumSymbols(ComponentName who, int userHandle, boolean parent) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (who != null) {
                        _data.writeInt(1);
                        who.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeInt(userHandle);
                    _data.writeInt(parent ? 1 : 0);
                    this.mRemote.transact(14, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void setPasswordMinimumNonLetter(ComponentName who, int length, boolean parent) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (who != null) {
                        _data.writeInt(1);
                        who.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeInt(length);
                    _data.writeInt(parent ? 1 : 0);
                    this.mRemote.transact(15, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public int getPasswordMinimumNonLetter(ComponentName who, int userHandle, boolean parent) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (who != null) {
                        _data.writeInt(1);
                        who.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeInt(userHandle);
                    _data.writeInt(parent ? 1 : 0);
                    this.mRemote.transact(16, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void setPasswordHistoryLength(ComponentName who, int length, boolean parent) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (who != null) {
                        _data.writeInt(1);
                        who.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeInt(length);
                    _data.writeInt(parent ? 1 : 0);
                    this.mRemote.transact(17, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public int getPasswordHistoryLength(ComponentName who, int userHandle, boolean parent) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (who != null) {
                        _data.writeInt(1);
                        who.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeInt(userHandle);
                    _data.writeInt(parent ? 1 : 0);
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
            public void setPasswordExpirationTimeout(ComponentName who, long expiration, boolean parent) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (who != null) {
                        _data.writeInt(1);
                        who.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeLong(expiration);
                    _data.writeInt(parent ? 1 : 0);
                    this.mRemote.transact(19, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public long getPasswordExpirationTimeout(ComponentName who, int userHandle, boolean parent) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (who != null) {
                        _data.writeInt(1);
                        who.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeInt(userHandle);
                    _data.writeInt(parent ? 1 : 0);
                    this.mRemote.transact(20, _data, _reply, 0);
                    _reply.readException();
                    long _result = _reply.readLong();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public long getPasswordExpiration(ComponentName who, int userHandle, boolean parent) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (who != null) {
                        _data.writeInt(1);
                        who.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeInt(userHandle);
                    _data.writeInt(parent ? 1 : 0);
                    this.mRemote.transact(21, _data, _reply, 0);
                    _reply.readException();
                    long _result = _reply.readLong();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public boolean isActivePasswordSufficient(int userHandle, boolean parent) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(userHandle);
                    _data.writeInt(parent ? 1 : 0);
                    this.mRemote.transact(22, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readInt() != 0;
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public boolean isProfileActivePasswordSufficientForParent(int userHandle) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(userHandle);
                    this.mRemote.transact(23, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readInt() != 0;
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public int getCurrentFailedPasswordAttempts(int userHandle, boolean parent) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(userHandle);
                    _data.writeInt(parent ? 1 : 0);
                    this.mRemote.transact(24, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public int getProfileWithMinimumFailedPasswordsForWipe(int userHandle, boolean parent) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(userHandle);
                    _data.writeInt(parent ? 1 : 0);
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
            public void setMaximumFailedPasswordsForWipe(ComponentName admin, int num, boolean parent) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (admin != null) {
                        _data.writeInt(1);
                        admin.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeInt(num);
                    _data.writeInt(parent ? 1 : 0);
                    this.mRemote.transact(26, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public int getMaximumFailedPasswordsForWipe(ComponentName admin, int userHandle, boolean parent) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (admin != null) {
                        _data.writeInt(1);
                        admin.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeInt(userHandle);
                    _data.writeInt(parent ? 1 : 0);
                    this.mRemote.transact(27, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public boolean resetPassword(String password, int flags) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(password);
                    _data.writeInt(flags);
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
            public void setMaximumTimeToLock(ComponentName who, long timeMs, boolean parent) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (who != null) {
                        _data.writeInt(1);
                        who.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeLong(timeMs);
                    _data.writeInt(parent ? 1 : 0);
                    this.mRemote.transact(29, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public long getMaximumTimeToLock(ComponentName who, int userHandle, boolean parent) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (who != null) {
                        _data.writeInt(1);
                        who.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeInt(userHandle);
                    _data.writeInt(parent ? 1 : 0);
                    this.mRemote.transact(30, _data, _reply, 0);
                    _reply.readException();
                    long _result = _reply.readLong();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public long getMaximumTimeToLockForUserAndProfiles(int userHandle) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(userHandle);
                    this.mRemote.transact(31, _data, _reply, 0);
                    _reply.readException();
                    long _result = _reply.readLong();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void lockNow(boolean parent) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(parent ? 1 : 0);
                    this.mRemote.transact(32, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void wipeData(int flags) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(flags);
                    this.mRemote.transact(33, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public ComponentName setGlobalProxy(ComponentName admin, String proxySpec, String exclusionList) throws RemoteException {
                ComponentName componentNameCreateFromParcel;
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (admin != null) {
                        _data.writeInt(1);
                        admin.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeString(proxySpec);
                    _data.writeString(exclusionList);
                    this.mRemote.transact(34, _data, _reply, 0);
                    _reply.readException();
                    if (_reply.readInt() != 0) {
                        componentNameCreateFromParcel = ComponentName.CREATOR.createFromParcel(_reply);
                    } else {
                        componentNameCreateFromParcel = null;
                    }
                    return componentNameCreateFromParcel;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public ComponentName getGlobalProxyAdmin(int userHandle) throws RemoteException {
                ComponentName componentNameCreateFromParcel;
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(userHandle);
                    this.mRemote.transact(35, _data, _reply, 0);
                    _reply.readException();
                    if (_reply.readInt() != 0) {
                        componentNameCreateFromParcel = ComponentName.CREATOR.createFromParcel(_reply);
                    } else {
                        componentNameCreateFromParcel = null;
                    }
                    return componentNameCreateFromParcel;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void setRecommendedGlobalProxy(ComponentName admin, ProxyInfo proxyInfo) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (admin != null) {
                        _data.writeInt(1);
                        admin.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    if (proxyInfo != null) {
                        _data.writeInt(1);
                        proxyInfo.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(36, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public int setStorageEncryption(ComponentName who, boolean encrypt) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (who != null) {
                        _data.writeInt(1);
                        who.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeInt(encrypt ? 1 : 0);
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
            public boolean getStorageEncryption(ComponentName who, int userHandle) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (who != null) {
                        _data.writeInt(1);
                        who.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeInt(userHandle);
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
            public int getStorageEncryptionStatus(String callerPackage, int userHandle) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(callerPackage);
                    _data.writeInt(userHandle);
                    this.mRemote.transact(39, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public boolean requestBugreport(ComponentName who) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (who != null) {
                        _data.writeInt(1);
                        who.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(40, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readInt() != 0;
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void setCameraDisabled(ComponentName who, boolean disabled) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (who != null) {
                        _data.writeInt(1);
                        who.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeInt(disabled ? 1 : 0);
                    this.mRemote.transact(41, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public boolean getCameraDisabled(ComponentName who, int userHandle) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (who != null) {
                        _data.writeInt(1);
                        who.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeInt(userHandle);
                    this.mRemote.transact(42, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readInt() != 0;
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void setScreenCaptureDisabled(ComponentName who, boolean disabled) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (who != null) {
                        _data.writeInt(1);
                        who.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeInt(disabled ? 1 : 0);
                    this.mRemote.transact(43, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public boolean getScreenCaptureDisabled(ComponentName who, int userHandle) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (who != null) {
                        _data.writeInt(1);
                        who.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeInt(userHandle);
                    this.mRemote.transact(44, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readInt() != 0;
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void setKeyguardDisabledFeatures(ComponentName who, int which, boolean parent) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (who != null) {
                        _data.writeInt(1);
                        who.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeInt(which);
                    _data.writeInt(parent ? 1 : 0);
                    this.mRemote.transact(45, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public int getKeyguardDisabledFeatures(ComponentName who, int userHandle, boolean parent) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (who != null) {
                        _data.writeInt(1);
                        who.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeInt(userHandle);
                    _data.writeInt(parent ? 1 : 0);
                    this.mRemote.transact(46, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void setActiveAdmin(ComponentName policyReceiver, boolean refreshing, int userHandle) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (policyReceiver != null) {
                        _data.writeInt(1);
                        policyReceiver.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeInt(refreshing ? 1 : 0);
                    _data.writeInt(userHandle);
                    this.mRemote.transact(47, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public boolean isAdminActive(ComponentName policyReceiver, int userHandle) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (policyReceiver != null) {
                        _data.writeInt(1);
                        policyReceiver.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeInt(userHandle);
                    this.mRemote.transact(48, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readInt() != 0;
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public List<ComponentName> getActiveAdmins(int userHandle) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(userHandle);
                    this.mRemote.transact(49, _data, _reply, 0);
                    _reply.readException();
                    List<ComponentName> _result = _reply.createTypedArrayList(ComponentName.CREATOR);
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public boolean packageHasActiveAdmins(String packageName, int userHandle) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(packageName);
                    _data.writeInt(userHandle);
                    this.mRemote.transact(50, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readInt() != 0;
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void getRemoveWarning(ComponentName policyReceiver, RemoteCallback result, int userHandle) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (policyReceiver != null) {
                        _data.writeInt(1);
                        policyReceiver.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    if (result != null) {
                        _data.writeInt(1);
                        result.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeInt(userHandle);
                    this.mRemote.transact(51, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void removeActiveAdmin(ComponentName policyReceiver, int userHandle) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (policyReceiver != null) {
                        _data.writeInt(1);
                        policyReceiver.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeInt(userHandle);
                    this.mRemote.transact(52, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void forceRemoveActiveAdmin(ComponentName policyReceiver, int userHandle) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (policyReceiver != null) {
                        _data.writeInt(1);
                        policyReceiver.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeInt(userHandle);
                    this.mRemote.transact(53, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public boolean hasGrantedPolicy(ComponentName policyReceiver, int usesPolicy, int userHandle) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (policyReceiver != null) {
                        _data.writeInt(1);
                        policyReceiver.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeInt(usesPolicy);
                    _data.writeInt(userHandle);
                    this.mRemote.transact(54, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readInt() != 0;
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void setActivePasswordState(int quality, int length, int letters, int uppercase, int lowercase, int numbers, int symbols, int nonletter, int userHandle) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(quality);
                    _data.writeInt(length);
                    _data.writeInt(letters);
                    _data.writeInt(uppercase);
                    _data.writeInt(lowercase);
                    _data.writeInt(numbers);
                    _data.writeInt(symbols);
                    _data.writeInt(nonletter);
                    _data.writeInt(userHandle);
                    this.mRemote.transact(55, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void reportPasswordChanged(int userId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(userId);
                    this.mRemote.transact(56, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void reportFailedPasswordAttempt(int userHandle) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(userHandle);
                    this.mRemote.transact(57, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void reportSuccessfulPasswordAttempt(int userHandle) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(userHandle);
                    this.mRemote.transact(58, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void reportFailedFingerprintAttempt(int userHandle) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(userHandle);
                    this.mRemote.transact(59, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void reportSuccessfulFingerprintAttempt(int userHandle) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(userHandle);
                    this.mRemote.transact(60, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void reportKeyguardDismissed(int userHandle) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(userHandle);
                    this.mRemote.transact(61, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void reportKeyguardSecured(int userHandle) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(userHandle);
                    this.mRemote.transact(62, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public boolean setDeviceOwner(ComponentName who, String ownerName, int userId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (who != null) {
                        _data.writeInt(1);
                        who.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeString(ownerName);
                    _data.writeInt(userId);
                    this.mRemote.transact(63, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readInt() != 0;
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public ComponentName getDeviceOwnerComponent(boolean callingUserOnly) throws RemoteException {
                ComponentName componentNameCreateFromParcel;
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(callingUserOnly ? 1 : 0);
                    this.mRemote.transact(64, _data, _reply, 0);
                    _reply.readException();
                    if (_reply.readInt() != 0) {
                        componentNameCreateFromParcel = ComponentName.CREATOR.createFromParcel(_reply);
                    } else {
                        componentNameCreateFromParcel = null;
                    }
                    return componentNameCreateFromParcel;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public String getDeviceOwnerName() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(65, _data, _reply, 0);
                    _reply.readException();
                    String _result = _reply.readString();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void clearDeviceOwner(String packageName) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(packageName);
                    this.mRemote.transact(66, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public int getDeviceOwnerUserId() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(67, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public boolean setProfileOwner(ComponentName who, String ownerName, int userHandle) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (who != null) {
                        _data.writeInt(1);
                        who.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeString(ownerName);
                    _data.writeInt(userHandle);
                    this.mRemote.transact(68, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readInt() != 0;
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public ComponentName getProfileOwner(int userHandle) throws RemoteException {
                ComponentName componentNameCreateFromParcel;
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(userHandle);
                    this.mRemote.transact(69, _data, _reply, 0);
                    _reply.readException();
                    if (_reply.readInt() != 0) {
                        componentNameCreateFromParcel = ComponentName.CREATOR.createFromParcel(_reply);
                    } else {
                        componentNameCreateFromParcel = null;
                    }
                    return componentNameCreateFromParcel;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public String getProfileOwnerName(int userHandle) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(userHandle);
                    this.mRemote.transact(70, _data, _reply, 0);
                    _reply.readException();
                    String _result = _reply.readString();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void setProfileEnabled(ComponentName who) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (who != null) {
                        _data.writeInt(1);
                        who.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(71, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void setProfileName(ComponentName who, String profileName) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (who != null) {
                        _data.writeInt(1);
                        who.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeString(profileName);
                    this.mRemote.transact(72, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void clearProfileOwner(ComponentName who) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (who != null) {
                        _data.writeInt(1);
                        who.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(73, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public boolean hasUserSetupCompleted() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(74, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readInt() != 0;
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void setDeviceOwnerLockScreenInfo(ComponentName who, CharSequence deviceOwnerInfo) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (who != null) {
                        _data.writeInt(1);
                        who.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    if (deviceOwnerInfo != null) {
                        _data.writeInt(1);
                        TextUtils.writeToParcel(deviceOwnerInfo, _data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(75, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public CharSequence getDeviceOwnerLockScreenInfo() throws RemoteException {
                CharSequence charSequence;
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(76, _data, _reply, 0);
                    _reply.readException();
                    if (_reply.readInt() != 0) {
                        charSequence = (CharSequence) TextUtils.CHAR_SEQUENCE_CREATOR.createFromParcel(_reply);
                    } else {
                        charSequence = null;
                    }
                    return charSequence;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public String[] setPackagesSuspended(ComponentName admin, String[] packageNames, boolean suspended) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (admin != null) {
                        _data.writeInt(1);
                        admin.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeStringArray(packageNames);
                    _data.writeInt(suspended ? 1 : 0);
                    this.mRemote.transact(77, _data, _reply, 0);
                    _reply.readException();
                    String[] _result = _reply.createStringArray();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public boolean isPackageSuspended(ComponentName admin, String packageName) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (admin != null) {
                        _data.writeInt(1);
                        admin.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeString(packageName);
                    this.mRemote.transact(78, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readInt() != 0;
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public boolean installCaCert(ComponentName admin, byte[] certBuffer) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (admin != null) {
                        _data.writeInt(1);
                        admin.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeByteArray(certBuffer);
                    this.mRemote.transact(79, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readInt() != 0;
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void uninstallCaCerts(ComponentName admin, String[] aliases) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (admin != null) {
                        _data.writeInt(1);
                        admin.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeStringArray(aliases);
                    this.mRemote.transact(80, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void enforceCanManageCaCerts(ComponentName admin) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (admin != null) {
                        _data.writeInt(1);
                        admin.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(81, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public boolean approveCaCert(String alias, int userHandle, boolean approval) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(alias);
                    _data.writeInt(userHandle);
                    _data.writeInt(approval ? 1 : 0);
                    this.mRemote.transact(82, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readInt() != 0;
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public boolean isCaCertApproved(String alias, int userHandle) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(alias);
                    _data.writeInt(userHandle);
                    this.mRemote.transact(83, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readInt() != 0;
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public boolean installKeyPair(ComponentName who, byte[] privKeyBuffer, byte[] certBuffer, byte[] certChainBuffer, String alias, boolean requestAccess) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (who != null) {
                        _data.writeInt(1);
                        who.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeByteArray(privKeyBuffer);
                    _data.writeByteArray(certBuffer);
                    _data.writeByteArray(certChainBuffer);
                    _data.writeString(alias);
                    _data.writeInt(requestAccess ? 1 : 0);
                    this.mRemote.transact(84, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readInt() != 0;
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public boolean removeKeyPair(ComponentName who, String alias) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (who != null) {
                        _data.writeInt(1);
                        who.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeString(alias);
                    this.mRemote.transact(85, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readInt() != 0;
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void choosePrivateKeyAlias(int uid, Uri uri, String alias, IBinder aliasCallback) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(uid);
                    if (uri != null) {
                        _data.writeInt(1);
                        uri.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeString(alias);
                    _data.writeStrongBinder(aliasCallback);
                    this.mRemote.transact(86, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void setCertInstallerPackage(ComponentName who, String installerPackage) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (who != null) {
                        _data.writeInt(1);
                        who.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeString(installerPackage);
                    this.mRemote.transact(87, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public String getCertInstallerPackage(ComponentName who) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (who != null) {
                        _data.writeInt(1);
                        who.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(88, _data, _reply, 0);
                    _reply.readException();
                    String _result = _reply.readString();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public boolean setAlwaysOnVpnPackage(ComponentName who, String vpnPackage, boolean lockdown) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (who != null) {
                        _data.writeInt(1);
                        who.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeString(vpnPackage);
                    _data.writeInt(lockdown ? 1 : 0);
                    this.mRemote.transact(89, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readInt() != 0;
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public String getAlwaysOnVpnPackage(ComponentName who) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (who != null) {
                        _data.writeInt(1);
                        who.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(90, _data, _reply, 0);
                    _reply.readException();
                    String _result = _reply.readString();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void addPersistentPreferredActivity(ComponentName admin, IntentFilter filter, ComponentName activity) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (admin != null) {
                        _data.writeInt(1);
                        admin.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
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
                    this.mRemote.transact(91, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void clearPackagePersistentPreferredActivities(ComponentName admin, String packageName) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (admin != null) {
                        _data.writeInt(1);
                        admin.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeString(packageName);
                    this.mRemote.transact(92, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void setApplicationRestrictions(ComponentName who, String packageName, Bundle settings) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (who != null) {
                        _data.writeInt(1);
                        who.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeString(packageName);
                    if (settings != null) {
                        _data.writeInt(1);
                        settings.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(93, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public Bundle getApplicationRestrictions(ComponentName who, String packageName) throws RemoteException {
                Bundle bundleCreateFromParcel;
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (who != null) {
                        _data.writeInt(1);
                        who.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeString(packageName);
                    this.mRemote.transact(94, _data, _reply, 0);
                    _reply.readException();
                    if (_reply.readInt() != 0) {
                        bundleCreateFromParcel = Bundle.CREATOR.createFromParcel(_reply);
                    } else {
                        bundleCreateFromParcel = null;
                    }
                    return bundleCreateFromParcel;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public boolean setApplicationRestrictionsManagingPackage(ComponentName admin, String packageName) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (admin != null) {
                        _data.writeInt(1);
                        admin.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeString(packageName);
                    this.mRemote.transact(95, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readInt() != 0;
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public String getApplicationRestrictionsManagingPackage(ComponentName admin) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (admin != null) {
                        _data.writeInt(1);
                        admin.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(96, _data, _reply, 0);
                    _reply.readException();
                    String _result = _reply.readString();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public boolean isCallerApplicationRestrictionsManagingPackage() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(97, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readInt() != 0;
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void setRestrictionsProvider(ComponentName who, ComponentName provider) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (who != null) {
                        _data.writeInt(1);
                        who.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    if (provider != null) {
                        _data.writeInt(1);
                        provider.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(98, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public ComponentName getRestrictionsProvider(int userHandle) throws RemoteException {
                ComponentName componentNameCreateFromParcel;
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(userHandle);
                    this.mRemote.transact(99, _data, _reply, 0);
                    _reply.readException();
                    if (_reply.readInt() != 0) {
                        componentNameCreateFromParcel = ComponentName.CREATOR.createFromParcel(_reply);
                    } else {
                        componentNameCreateFromParcel = null;
                    }
                    return componentNameCreateFromParcel;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void setUserRestriction(ComponentName who, String key, boolean enable) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (who != null) {
                        _data.writeInt(1);
                        who.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeString(key);
                    _data.writeInt(enable ? 1 : 0);
                    this.mRemote.transact(100, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public Bundle getUserRestrictions(ComponentName who) throws RemoteException {
                Bundle bundleCreateFromParcel;
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (who != null) {
                        _data.writeInt(1);
                        who.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(101, _data, _reply, 0);
                    _reply.readException();
                    if (_reply.readInt() != 0) {
                        bundleCreateFromParcel = Bundle.CREATOR.createFromParcel(_reply);
                    } else {
                        bundleCreateFromParcel = null;
                    }
                    return bundleCreateFromParcel;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void addCrossProfileIntentFilter(ComponentName admin, IntentFilter filter, int flags) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (admin != null) {
                        _data.writeInt(1);
                        admin.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    if (filter != null) {
                        _data.writeInt(1);
                        filter.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeInt(flags);
                    this.mRemote.transact(102, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void clearCrossProfileIntentFilters(ComponentName admin) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (admin != null) {
                        _data.writeInt(1);
                        admin.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(103, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public boolean setPermittedAccessibilityServices(ComponentName admin, List packageList) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (admin != null) {
                        _data.writeInt(1);
                        admin.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeList(packageList);
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
            public List getPermittedAccessibilityServices(ComponentName admin) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (admin != null) {
                        _data.writeInt(1);
                        admin.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(105, _data, _reply, 0);
                    _reply.readException();
                    ClassLoader cl = getClass().getClassLoader();
                    List _result = _reply.readArrayList(cl);
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public List getPermittedAccessibilityServicesForUser(int userId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(userId);
                    this.mRemote.transact(106, _data, _reply, 0);
                    _reply.readException();
                    ClassLoader cl = getClass().getClassLoader();
                    List _result = _reply.readArrayList(cl);
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public boolean isAccessibilityServicePermittedByAdmin(ComponentName admin, String packageName, int userId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (admin != null) {
                        _data.writeInt(1);
                        admin.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeString(packageName);
                    _data.writeInt(userId);
                    this.mRemote.transact(107, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readInt() != 0;
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public boolean setPermittedInputMethods(ComponentName admin, List packageList) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (admin != null) {
                        _data.writeInt(1);
                        admin.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeList(packageList);
                    this.mRemote.transact(108, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readInt() != 0;
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public List getPermittedInputMethods(ComponentName admin) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (admin != null) {
                        _data.writeInt(1);
                        admin.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(109, _data, _reply, 0);
                    _reply.readException();
                    ClassLoader cl = getClass().getClassLoader();
                    List _result = _reply.readArrayList(cl);
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public List getPermittedInputMethodsForCurrentUser() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(110, _data, _reply, 0);
                    _reply.readException();
                    ClassLoader cl = getClass().getClassLoader();
                    List _result = _reply.readArrayList(cl);
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public boolean isInputMethodPermittedByAdmin(ComponentName admin, String packageName, int userId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (admin != null) {
                        _data.writeInt(1);
                        admin.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeString(packageName);
                    _data.writeInt(userId);
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
            public boolean setApplicationHidden(ComponentName admin, String packageName, boolean hidden) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (admin != null) {
                        _data.writeInt(1);
                        admin.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeString(packageName);
                    _data.writeInt(hidden ? 1 : 0);
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
            public boolean isApplicationHidden(ComponentName admin, String packageName) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (admin != null) {
                        _data.writeInt(1);
                        admin.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeString(packageName);
                    this.mRemote.transact(113, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readInt() != 0;
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public UserHandle createAndManageUser(ComponentName who, String name, ComponentName profileOwner, PersistableBundle adminExtras, int flags) throws RemoteException {
                UserHandle userHandleCreateFromParcel;
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (who != null) {
                        _data.writeInt(1);
                        who.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeString(name);
                    if (profileOwner != null) {
                        _data.writeInt(1);
                        profileOwner.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    if (adminExtras != null) {
                        _data.writeInt(1);
                        adminExtras.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeInt(flags);
                    this.mRemote.transact(114, _data, _reply, 0);
                    _reply.readException();
                    if (_reply.readInt() != 0) {
                        userHandleCreateFromParcel = UserHandle.CREATOR.createFromParcel(_reply);
                    } else {
                        userHandleCreateFromParcel = null;
                    }
                    return userHandleCreateFromParcel;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public boolean removeUser(ComponentName who, UserHandle userHandle) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (who != null) {
                        _data.writeInt(1);
                        who.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    if (userHandle != null) {
                        _data.writeInt(1);
                        userHandle.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(115, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readInt() != 0;
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public boolean switchUser(ComponentName who, UserHandle userHandle) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (who != null) {
                        _data.writeInt(1);
                        who.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    if (userHandle != null) {
                        _data.writeInt(1);
                        userHandle.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(116, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readInt() != 0;
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void enableSystemApp(ComponentName admin, String packageName) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (admin != null) {
                        _data.writeInt(1);
                        admin.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeString(packageName);
                    this.mRemote.transact(117, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public int enableSystemAppWithIntent(ComponentName admin, Intent intent) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (admin != null) {
                        _data.writeInt(1);
                        admin.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    if (intent != null) {
                        _data.writeInt(1);
                        intent.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(118, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void setAccountManagementDisabled(ComponentName who, String accountType, boolean disabled) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (who != null) {
                        _data.writeInt(1);
                        who.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeString(accountType);
                    _data.writeInt(disabled ? 1 : 0);
                    this.mRemote.transact(119, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public String[] getAccountTypesWithManagementDisabled() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(120, _data, _reply, 0);
                    _reply.readException();
                    String[] _result = _reply.createStringArray();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public String[] getAccountTypesWithManagementDisabledAsUser(int userId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(userId);
                    this.mRemote.transact(121, _data, _reply, 0);
                    _reply.readException();
                    String[] _result = _reply.createStringArray();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void setLockTaskPackages(ComponentName who, String[] packages) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (who != null) {
                        _data.writeInt(1);
                        who.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeStringArray(packages);
                    this.mRemote.transact(122, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public String[] getLockTaskPackages(ComponentName who) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (who != null) {
                        _data.writeInt(1);
                        who.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(123, _data, _reply, 0);
                    _reply.readException();
                    String[] _result = _reply.createStringArray();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public boolean isLockTaskPermitted(String pkg) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(pkg);
                    this.mRemote.transact(124, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readInt() != 0;
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void setGlobalSetting(ComponentName who, String setting, String value) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (who != null) {
                        _data.writeInt(1);
                        who.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeString(setting);
                    _data.writeString(value);
                    this.mRemote.transact(125, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void setSecureSetting(ComponentName who, String setting, String value) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (who != null) {
                        _data.writeInt(1);
                        who.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeString(setting);
                    _data.writeString(value);
                    this.mRemote.transact(126, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void setMasterVolumeMuted(ComponentName admin, boolean on) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (admin != null) {
                        _data.writeInt(1);
                        admin.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeInt(on ? 1 : 0);
                    this.mRemote.transact(127, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public boolean isMasterVolumeMuted(ComponentName admin) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (admin != null) {
                        _data.writeInt(1);
                        admin.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(128, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readInt() != 0;
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void notifyLockTaskModeChanged(boolean isEnabled, String pkg, int userId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(isEnabled ? 1 : 0);
                    _data.writeString(pkg);
                    _data.writeInt(userId);
                    this.mRemote.transact(129, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void setUninstallBlocked(ComponentName admin, String packageName, boolean uninstallBlocked) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (admin != null) {
                        _data.writeInt(1);
                        admin.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeString(packageName);
                    _data.writeInt(uninstallBlocked ? 1 : 0);
                    this.mRemote.transact(130, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public boolean isUninstallBlocked(ComponentName admin, String packageName) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (admin != null) {
                        _data.writeInt(1);
                        admin.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeString(packageName);
                    this.mRemote.transact(131, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readInt() != 0;
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void setCrossProfileCallerIdDisabled(ComponentName who, boolean disabled) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (who != null) {
                        _data.writeInt(1);
                        who.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeInt(disabled ? 1 : 0);
                    this.mRemote.transact(132, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public boolean getCrossProfileCallerIdDisabled(ComponentName who) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (who != null) {
                        _data.writeInt(1);
                        who.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
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
            public boolean getCrossProfileCallerIdDisabledForUser(int userId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(userId);
                    this.mRemote.transact(134, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readInt() != 0;
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void setCrossProfileContactsSearchDisabled(ComponentName who, boolean disabled) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (who != null) {
                        _data.writeInt(1);
                        who.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeInt(disabled ? 1 : 0);
                    this.mRemote.transact(135, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public boolean getCrossProfileContactsSearchDisabled(ComponentName who) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (who != null) {
                        _data.writeInt(1);
                        who.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
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
            public boolean getCrossProfileContactsSearchDisabledForUser(int userId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(userId);
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
            public void startManagedQuickContact(String lookupKey, long contactId, boolean isContactIdIgnored, long directoryId, Intent originalIntent) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(lookupKey);
                    _data.writeLong(contactId);
                    _data.writeInt(isContactIdIgnored ? 1 : 0);
                    _data.writeLong(directoryId);
                    if (originalIntent != null) {
                        _data.writeInt(1);
                        originalIntent.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(138, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void setBluetoothContactSharingDisabled(ComponentName who, boolean disabled) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (who != null) {
                        _data.writeInt(1);
                        who.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeInt(disabled ? 1 : 0);
                    this.mRemote.transact(139, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public boolean getBluetoothContactSharingDisabled(ComponentName who) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (who != null) {
                        _data.writeInt(1);
                        who.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(140, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readInt() != 0;
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public boolean getBluetoothContactSharingDisabledForUser(int userId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(userId);
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
            public void setTrustAgentConfiguration(ComponentName admin, ComponentName agent, PersistableBundle args, boolean parent) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (admin != null) {
                        _data.writeInt(1);
                        admin.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    if (agent != null) {
                        _data.writeInt(1);
                        agent.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    if (args != null) {
                        _data.writeInt(1);
                        args.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeInt(parent ? 1 : 0);
                    this.mRemote.transact(142, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public List<PersistableBundle> getTrustAgentConfiguration(ComponentName admin, ComponentName agent, int userId, boolean parent) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (admin != null) {
                        _data.writeInt(1);
                        admin.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    if (agent != null) {
                        _data.writeInt(1);
                        agent.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeInt(userId);
                    _data.writeInt(parent ? 1 : 0);
                    this.mRemote.transact(143, _data, _reply, 0);
                    _reply.readException();
                    List<PersistableBundle> _result = _reply.createTypedArrayList(PersistableBundle.CREATOR);
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public boolean addCrossProfileWidgetProvider(ComponentName admin, String packageName) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (admin != null) {
                        _data.writeInt(1);
                        admin.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeString(packageName);
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
            public boolean removeCrossProfileWidgetProvider(ComponentName admin, String packageName) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (admin != null) {
                        _data.writeInt(1);
                        admin.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeString(packageName);
                    this.mRemote.transact(145, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readInt() != 0;
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public List<String> getCrossProfileWidgetProviders(ComponentName admin) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (admin != null) {
                        _data.writeInt(1);
                        admin.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(146, _data, _reply, 0);
                    _reply.readException();
                    List<String> _result = _reply.createStringArrayList();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void setAutoTimeRequired(ComponentName who, boolean required) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (who != null) {
                        _data.writeInt(1);
                        who.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeInt(required ? 1 : 0);
                    this.mRemote.transact(147, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public boolean getAutoTimeRequired() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(148, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readInt() != 0;
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void setForceEphemeralUsers(ComponentName who, boolean forceEpehemeralUsers) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (who != null) {
                        _data.writeInt(1);
                        who.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeInt(forceEpehemeralUsers ? 1 : 0);
                    this.mRemote.transact(149, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public boolean getForceEphemeralUsers(ComponentName who) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (who != null) {
                        _data.writeInt(1);
                        who.writeToParcel(_data, 0);
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
            public boolean isRemovingAdmin(ComponentName adminReceiver, int userHandle) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (adminReceiver != null) {
                        _data.writeInt(1);
                        adminReceiver.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeInt(userHandle);
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
            public void setUserIcon(ComponentName admin, Bitmap icon) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (admin != null) {
                        _data.writeInt(1);
                        admin.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    if (icon != null) {
                        _data.writeInt(1);
                        icon.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(152, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void setSystemUpdatePolicy(ComponentName who, SystemUpdatePolicy policy) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (who != null) {
                        _data.writeInt(1);
                        who.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    if (policy != null) {
                        _data.writeInt(1);
                        policy.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(153, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public SystemUpdatePolicy getSystemUpdatePolicy() throws RemoteException {
                SystemUpdatePolicy systemUpdatePolicyCreateFromParcel;
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(154, _data, _reply, 0);
                    _reply.readException();
                    if (_reply.readInt() != 0) {
                        systemUpdatePolicyCreateFromParcel = SystemUpdatePolicy.CREATOR.createFromParcel(_reply);
                    } else {
                        systemUpdatePolicyCreateFromParcel = null;
                    }
                    return systemUpdatePolicyCreateFromParcel;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public boolean setKeyguardDisabled(ComponentName admin, boolean disabled) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (admin != null) {
                        _data.writeInt(1);
                        admin.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeInt(disabled ? 1 : 0);
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
            public boolean setStatusBarDisabled(ComponentName who, boolean disabled) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (who != null) {
                        _data.writeInt(1);
                        who.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeInt(disabled ? 1 : 0);
                    this.mRemote.transact(156, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readInt() != 0;
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public boolean getDoNotAskCredentialsOnBoot() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(157, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readInt() != 0;
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void notifyPendingSystemUpdate(long updateReceivedTime) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeLong(updateReceivedTime);
                    this.mRemote.transact(158, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void setPermissionPolicy(ComponentName admin, int policy) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (admin != null) {
                        _data.writeInt(1);
                        admin.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeInt(policy);
                    this.mRemote.transact(159, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public int getPermissionPolicy(ComponentName admin) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (admin != null) {
                        _data.writeInt(1);
                        admin.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(160, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public boolean setPermissionGrantState(ComponentName admin, String packageName, String permission, int grantState) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (admin != null) {
                        _data.writeInt(1);
                        admin.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeString(packageName);
                    _data.writeString(permission);
                    _data.writeInt(grantState);
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
            public int getPermissionGrantState(ComponentName admin, String packageName, String permission) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (admin != null) {
                        _data.writeInt(1);
                        admin.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeString(packageName);
                    _data.writeString(permission);
                    this.mRemote.transact(162, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public boolean isProvisioningAllowed(String action) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(action);
                    this.mRemote.transact(163, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readInt() != 0;
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void setKeepUninstalledPackages(ComponentName admin, List<String> packageList) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (admin != null) {
                        _data.writeInt(1);
                        admin.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeStringList(packageList);
                    this.mRemote.transact(164, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public List<String> getKeepUninstalledPackages(ComponentName admin) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (admin != null) {
                        _data.writeInt(1);
                        admin.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(165, _data, _reply, 0);
                    _reply.readException();
                    List<String> _result = _reply.createStringArrayList();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public boolean isManagedProfile(ComponentName admin) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (admin != null) {
                        _data.writeInt(1);
                        admin.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(166, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readInt() != 0;
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public boolean isSystemOnlyUser(ComponentName admin) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (admin != null) {
                        _data.writeInt(1);
                        admin.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(167, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readInt() != 0;
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public String getWifiMacAddress(ComponentName admin) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (admin != null) {
                        _data.writeInt(1);
                        admin.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(168, _data, _reply, 0);
                    _reply.readException();
                    String _result = _reply.readString();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void reboot(ComponentName admin) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (admin != null) {
                        _data.writeInt(1);
                        admin.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(169, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void setShortSupportMessage(ComponentName admin, CharSequence message) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (admin != null) {
                        _data.writeInt(1);
                        admin.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    if (message != null) {
                        _data.writeInt(1);
                        TextUtils.writeToParcel(message, _data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(170, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public CharSequence getShortSupportMessage(ComponentName admin) throws RemoteException {
                CharSequence charSequence;
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (admin != null) {
                        _data.writeInt(1);
                        admin.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(171, _data, _reply, 0);
                    _reply.readException();
                    if (_reply.readInt() != 0) {
                        charSequence = (CharSequence) TextUtils.CHAR_SEQUENCE_CREATOR.createFromParcel(_reply);
                    } else {
                        charSequence = null;
                    }
                    return charSequence;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void setLongSupportMessage(ComponentName admin, CharSequence message) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (admin != null) {
                        _data.writeInt(1);
                        admin.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    if (message != null) {
                        _data.writeInt(1);
                        TextUtils.writeToParcel(message, _data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(172, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public CharSequence getLongSupportMessage(ComponentName admin) throws RemoteException {
                CharSequence charSequence;
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (admin != null) {
                        _data.writeInt(1);
                        admin.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(173, _data, _reply, 0);
                    _reply.readException();
                    if (_reply.readInt() != 0) {
                        charSequence = (CharSequence) TextUtils.CHAR_SEQUENCE_CREATOR.createFromParcel(_reply);
                    } else {
                        charSequence = null;
                    }
                    return charSequence;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public CharSequence getShortSupportMessageForUser(ComponentName admin, int userHandle) throws RemoteException {
                CharSequence charSequence;
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (admin != null) {
                        _data.writeInt(1);
                        admin.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeInt(userHandle);
                    this.mRemote.transact(174, _data, _reply, 0);
                    _reply.readException();
                    if (_reply.readInt() != 0) {
                        charSequence = (CharSequence) TextUtils.CHAR_SEQUENCE_CREATOR.createFromParcel(_reply);
                    } else {
                        charSequence = null;
                    }
                    return charSequence;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public CharSequence getLongSupportMessageForUser(ComponentName admin, int userHandle) throws RemoteException {
                CharSequence charSequence;
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (admin != null) {
                        _data.writeInt(1);
                        admin.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeInt(userHandle);
                    this.mRemote.transact(175, _data, _reply, 0);
                    _reply.readException();
                    if (_reply.readInt() != 0) {
                        charSequence = (CharSequence) TextUtils.CHAR_SEQUENCE_CREATOR.createFromParcel(_reply);
                    } else {
                        charSequence = null;
                    }
                    return charSequence;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public boolean isSeparateProfileChallengeAllowed(int userHandle) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(userHandle);
                    this.mRemote.transact(176, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readInt() != 0;
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void setOrganizationColor(ComponentName admin, int color) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (admin != null) {
                        _data.writeInt(1);
                        admin.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeInt(color);
                    this.mRemote.transact(177, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void setOrganizationColorForUser(int color, int userId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(color);
                    _data.writeInt(userId);
                    this.mRemote.transact(178, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public int getOrganizationColor(ComponentName admin) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (admin != null) {
                        _data.writeInt(1);
                        admin.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(179, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public int getOrganizationColorForUser(int userHandle) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(userHandle);
                    this.mRemote.transact(180, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void setOrganizationName(ComponentName admin, CharSequence title) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (admin != null) {
                        _data.writeInt(1);
                        admin.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    if (title != null) {
                        _data.writeInt(1);
                        TextUtils.writeToParcel(title, _data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(181, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public CharSequence getOrganizationName(ComponentName admin) throws RemoteException {
                CharSequence charSequence;
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (admin != null) {
                        _data.writeInt(1);
                        admin.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(182, _data, _reply, 0);
                    _reply.readException();
                    if (_reply.readInt() != 0) {
                        charSequence = (CharSequence) TextUtils.CHAR_SEQUENCE_CREATOR.createFromParcel(_reply);
                    } else {
                        charSequence = null;
                    }
                    return charSequence;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public CharSequence getOrganizationNameForUser(int userHandle) throws RemoteException {
                CharSequence charSequence;
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(userHandle);
                    this.mRemote.transact(183, _data, _reply, 0);
                    _reply.readException();
                    if (_reply.readInt() != 0) {
                        charSequence = (CharSequence) TextUtils.CHAR_SEQUENCE_CREATOR.createFromParcel(_reply);
                    } else {
                        charSequence = null;
                    }
                    return charSequence;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public int getUserProvisioningState() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(184, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void setUserProvisioningState(int state, int userHandle) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(state);
                    _data.writeInt(userHandle);
                    this.mRemote.transact(185, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void setAffiliationIds(ComponentName admin, List<String> ids) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (admin != null) {
                        _data.writeInt(1);
                        admin.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeStringList(ids);
                    this.mRemote.transact(186, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public boolean isAffiliatedUser() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(187, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readInt() != 0;
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void setSecurityLoggingEnabled(ComponentName admin, boolean enabled) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (admin != null) {
                        _data.writeInt(1);
                        admin.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeInt(enabled ? 1 : 0);
                    this.mRemote.transact(188, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public boolean isSecurityLoggingEnabled(ComponentName admin) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (admin != null) {
                        _data.writeInt(1);
                        admin.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(189, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readInt() != 0;
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public ParceledListSlice retrieveSecurityLogs(ComponentName admin) throws RemoteException {
                ParceledListSlice parceledListSliceCreateFromParcel;
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (admin != null) {
                        _data.writeInt(1);
                        admin.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(190, _data, _reply, 0);
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
            public ParceledListSlice retrievePreRebootSecurityLogs(ComponentName admin) throws RemoteException {
                ParceledListSlice parceledListSliceCreateFromParcel;
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (admin != null) {
                        _data.writeInt(1);
                        admin.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(191, _data, _reply, 0);
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
            public boolean isUninstallInQueue(String packageName) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(packageName);
                    this.mRemote.transact(192, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readInt() != 0;
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void uninstallPackageWithActiveAdmins(String packageName) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(packageName);
                    this.mRemote.transact(193, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }
        }
    }
}
