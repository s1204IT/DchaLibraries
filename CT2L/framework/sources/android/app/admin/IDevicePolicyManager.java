package android.app.admin;

import android.content.ComponentName;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ProxyInfo;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.PersistableBundle;
import android.os.RemoteCallback;
import android.os.RemoteException;
import android.os.UserHandle;
import java.util.List;

public interface IDevicePolicyManager extends IInterface {
    void addCrossProfileIntentFilter(ComponentName componentName, IntentFilter intentFilter, int i) throws RemoteException;

    boolean addCrossProfileWidgetProvider(ComponentName componentName, String str) throws RemoteException;

    void addPersistentPreferredActivity(ComponentName componentName, IntentFilter intentFilter, ComponentName componentName2) throws RemoteException;

    void clearCrossProfileIntentFilters(ComponentName componentName) throws RemoteException;

    void clearDeviceOwner(String str) throws RemoteException;

    void clearPackagePersistentPreferredActivities(ComponentName componentName, String str) throws RemoteException;

    void clearProfileOwner(ComponentName componentName) throws RemoteException;

    UserHandle createAndInitializeUser(ComponentName componentName, String str, String str2, ComponentName componentName2, Bundle bundle) throws RemoteException;

    UserHandle createUser(ComponentName componentName, String str) throws RemoteException;

    void enableSystemApp(ComponentName componentName, String str) throws RemoteException;

    int enableSystemAppWithIntent(ComponentName componentName, Intent intent) throws RemoteException;

    void enforceCanManageCaCerts(ComponentName componentName) throws RemoteException;

    String[] getAccountTypesWithManagementDisabled() throws RemoteException;

    String[] getAccountTypesWithManagementDisabledAsUser(int i) throws RemoteException;

    List<ComponentName> getActiveAdmins(int i) throws RemoteException;

    Bundle getApplicationRestrictions(ComponentName componentName, String str) throws RemoteException;

    boolean getAutoTimeRequired() throws RemoteException;

    boolean getCameraDisabled(ComponentName componentName, int i) throws RemoteException;

    boolean getCrossProfileCallerIdDisabled(ComponentName componentName) throws RemoteException;

    boolean getCrossProfileCallerIdDisabledForUser(int i) throws RemoteException;

    List<String> getCrossProfileWidgetProviders(ComponentName componentName) throws RemoteException;

    int getCurrentFailedPasswordAttempts(int i) throws RemoteException;

    String getDeviceOwner() throws RemoteException;

    String getDeviceOwnerName() throws RemoteException;

    ComponentName getGlobalProxyAdmin(int i) throws RemoteException;

    int getKeyguardDisabledFeatures(ComponentName componentName, int i) throws RemoteException;

    String[] getLockTaskPackages(ComponentName componentName) throws RemoteException;

    int getMaximumFailedPasswordsForWipe(ComponentName componentName, int i) throws RemoteException;

    long getMaximumTimeToLock(ComponentName componentName, int i) throws RemoteException;

    long getPasswordExpiration(ComponentName componentName, int i) throws RemoteException;

    long getPasswordExpirationTimeout(ComponentName componentName, int i) throws RemoteException;

    int getPasswordHistoryLength(ComponentName componentName, int i) throws RemoteException;

    int getPasswordMinimumLength(ComponentName componentName, int i) throws RemoteException;

    int getPasswordMinimumLetters(ComponentName componentName, int i) throws RemoteException;

    int getPasswordMinimumLowerCase(ComponentName componentName, int i) throws RemoteException;

    int getPasswordMinimumNonLetter(ComponentName componentName, int i) throws RemoteException;

    int getPasswordMinimumNumeric(ComponentName componentName, int i) throws RemoteException;

    int getPasswordMinimumSymbols(ComponentName componentName, int i) throws RemoteException;

    int getPasswordMinimumUpperCase(ComponentName componentName, int i) throws RemoteException;

    int getPasswordQuality(ComponentName componentName, int i) throws RemoteException;

    List getPermittedAccessibilityServices(ComponentName componentName) throws RemoteException;

    List getPermittedAccessibilityServicesForUser(int i) throws RemoteException;

    List getPermittedInputMethods(ComponentName componentName) throws RemoteException;

    List getPermittedInputMethodsForCurrentUser() throws RemoteException;

    ComponentName getProfileOwner(int i) throws RemoteException;

    String getProfileOwnerName(int i) throws RemoteException;

    int getProfileWithMinimumFailedPasswordsForWipe(int i) throws RemoteException;

    void getRemoveWarning(ComponentName componentName, RemoteCallback remoteCallback, int i) throws RemoteException;

    ComponentName getRestrictionsProvider(int i) throws RemoteException;

    boolean getScreenCaptureDisabled(ComponentName componentName, int i) throws RemoteException;

    boolean getStorageEncryption(ComponentName componentName, int i) throws RemoteException;

    int getStorageEncryptionStatus(int i) throws RemoteException;

    List<PersistableBundle> getTrustAgentConfiguration(ComponentName componentName, ComponentName componentName2, int i) throws RemoteException;

    boolean hasGrantedPolicy(ComponentName componentName, int i, int i2) throws RemoteException;

    boolean hasUserSetupCompleted() throws RemoteException;

    boolean installCaCert(ComponentName componentName, byte[] bArr) throws RemoteException;

    boolean installKeyPair(ComponentName componentName, byte[] bArr, byte[] bArr2, String str) throws RemoteException;

    boolean isActivePasswordSufficient(int i) throws RemoteException;

    boolean isAdminActive(ComponentName componentName, int i) throws RemoteException;

    boolean isApplicationHidden(ComponentName componentName, String str) throws RemoteException;

    boolean isDeviceOwner(String str) throws RemoteException;

    boolean isLockTaskPermitted(String str) throws RemoteException;

    boolean isMasterVolumeMuted(ComponentName componentName) throws RemoteException;

    boolean isRemovingAdmin(ComponentName componentName, int i) throws RemoteException;

    boolean isUninstallBlocked(ComponentName componentName, String str) throws RemoteException;

    void lockNow() throws RemoteException;

    void notifyLockTaskModeChanged(boolean z, String str, int i) throws RemoteException;

    boolean packageHasActiveAdmins(String str, int i) throws RemoteException;

    void removeActiveAdmin(ComponentName componentName, int i) throws RemoteException;

    boolean removeCrossProfileWidgetProvider(ComponentName componentName, String str) throws RemoteException;

    boolean removeUser(ComponentName componentName, UserHandle userHandle) throws RemoteException;

    void reportFailedPasswordAttempt(int i) throws RemoteException;

    void reportSuccessfulPasswordAttempt(int i) throws RemoteException;

    boolean resetPassword(String str, int i, int i2) throws RemoteException;

    void setAccountManagementDisabled(ComponentName componentName, String str, boolean z) throws RemoteException;

    void setActiveAdmin(ComponentName componentName, boolean z, int i) throws RemoteException;

    void setActivePasswordState(int i, int i2, int i3, int i4, int i5, int i6, int i7, int i8, int i9) throws RemoteException;

    boolean setApplicationHidden(ComponentName componentName, String str, boolean z) throws RemoteException;

    void setApplicationRestrictions(ComponentName componentName, String str, Bundle bundle) throws RemoteException;

    void setAutoTimeRequired(ComponentName componentName, int i, boolean z) throws RemoteException;

    void setCameraDisabled(ComponentName componentName, boolean z, int i) throws RemoteException;

    void setCrossProfileCallerIdDisabled(ComponentName componentName, boolean z) throws RemoteException;

    boolean setDeviceOwner(String str, String str2) throws RemoteException;

    ComponentName setGlobalProxy(ComponentName componentName, String str, String str2, int i) throws RemoteException;

    void setGlobalSetting(ComponentName componentName, String str, String str2) throws RemoteException;

    void setKeyguardDisabledFeatures(ComponentName componentName, int i, int i2) throws RemoteException;

    void setLockTaskPackages(ComponentName componentName, String[] strArr) throws RemoteException;

    void setMasterVolumeMuted(ComponentName componentName, boolean z) throws RemoteException;

    void setMaximumFailedPasswordsForWipe(ComponentName componentName, int i, int i2) throws RemoteException;

    void setMaximumTimeToLock(ComponentName componentName, long j, int i) throws RemoteException;

    void setPasswordExpirationTimeout(ComponentName componentName, long j, int i) throws RemoteException;

    void setPasswordHistoryLength(ComponentName componentName, int i, int i2) throws RemoteException;

    void setPasswordMinimumLength(ComponentName componentName, int i, int i2) throws RemoteException;

    void setPasswordMinimumLetters(ComponentName componentName, int i, int i2) throws RemoteException;

    void setPasswordMinimumLowerCase(ComponentName componentName, int i, int i2) throws RemoteException;

    void setPasswordMinimumNonLetter(ComponentName componentName, int i, int i2) throws RemoteException;

    void setPasswordMinimumNumeric(ComponentName componentName, int i, int i2) throws RemoteException;

    void setPasswordMinimumSymbols(ComponentName componentName, int i, int i2) throws RemoteException;

    void setPasswordMinimumUpperCase(ComponentName componentName, int i, int i2) throws RemoteException;

    void setPasswordQuality(ComponentName componentName, int i, int i2) throws RemoteException;

    boolean setPermittedAccessibilityServices(ComponentName componentName, List list) throws RemoteException;

    boolean setPermittedInputMethods(ComponentName componentName, List list) throws RemoteException;

    void setProfileEnabled(ComponentName componentName) throws RemoteException;

    void setProfileName(ComponentName componentName, String str) throws RemoteException;

    boolean setProfileOwner(ComponentName componentName, String str, int i) throws RemoteException;

    void setRecommendedGlobalProxy(ComponentName componentName, ProxyInfo proxyInfo) throws RemoteException;

    void setRestrictionsProvider(ComponentName componentName, ComponentName componentName2) throws RemoteException;

    void setScreenCaptureDisabled(ComponentName componentName, int i, boolean z) throws RemoteException;

    void setSecureSetting(ComponentName componentName, String str, String str2) throws RemoteException;

    int setStorageEncryption(ComponentName componentName, boolean z, int i) throws RemoteException;

    void setTrustAgentConfiguration(ComponentName componentName, ComponentName componentName2, PersistableBundle persistableBundle, int i) throws RemoteException;

    void setUninstallBlocked(ComponentName componentName, String str, boolean z) throws RemoteException;

    void setUserRestriction(ComponentName componentName, String str, boolean z) throws RemoteException;

    boolean switchUser(ComponentName componentName, UserHandle userHandle) throws RemoteException;

    void uninstallCaCert(ComponentName componentName, String str) throws RemoteException;

    void wipeData(int i, int i2) throws RemoteException;

    public static abstract class Stub extends Binder implements IDevicePolicyManager {
        private static final String DESCRIPTOR = "android.app.admin.IDevicePolicyManager";
        static final int TRANSACTION_addCrossProfileIntentFilter = 77;
        static final int TRANSACTION_addCrossProfileWidgetProvider = 111;
        static final int TRANSACTION_addPersistentPreferredActivity = 70;
        static final int TRANSACTION_clearCrossProfileIntentFilters = 78;
        static final int TRANSACTION_clearDeviceOwner = 58;
        static final int TRANSACTION_clearPackagePersistentPreferredActivities = 71;
        static final int TRANSACTION_clearProfileOwner = 64;
        static final int TRANSACTION_createAndInitializeUser = 88;
        static final int TRANSACTION_createUser = 87;
        static final int TRANSACTION_enableSystemApp = 91;
        static final int TRANSACTION_enableSystemAppWithIntent = 92;
        static final int TRANSACTION_enforceCanManageCaCerts = 68;
        static final int TRANSACTION_getAccountTypesWithManagementDisabled = 94;
        static final int TRANSACTION_getAccountTypesWithManagementDisabledAsUser = 95;
        static final int TRANSACTION_getActiveAdmins = 46;
        static final int TRANSACTION_getApplicationRestrictions = 73;
        static final int TRANSACTION_getAutoTimeRequired = 115;
        static final int TRANSACTION_getCameraDisabled = 39;
        static final int TRANSACTION_getCrossProfileCallerIdDisabled = 107;
        static final int TRANSACTION_getCrossProfileCallerIdDisabledForUser = 108;
        static final int TRANSACTION_getCrossProfileWidgetProviders = 113;
        static final int TRANSACTION_getCurrentFailedPasswordAttempts = 23;
        static final int TRANSACTION_getDeviceOwner = 56;
        static final int TRANSACTION_getDeviceOwnerName = 57;
        static final int TRANSACTION_getGlobalProxyAdmin = 33;
        static final int TRANSACTION_getKeyguardDisabledFeatures = 43;
        static final int TRANSACTION_getLockTaskPackages = 97;
        static final int TRANSACTION_getMaximumFailedPasswordsForWipe = 26;
        static final int TRANSACTION_getMaximumTimeToLock = 29;
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
        static final int TRANSACTION_getPermittedAccessibilityServices = 80;
        static final int TRANSACTION_getPermittedAccessibilityServicesForUser = 81;
        static final int TRANSACTION_getPermittedInputMethods = 83;
        static final int TRANSACTION_getPermittedInputMethodsForCurrentUser = 84;
        static final int TRANSACTION_getProfileOwner = 60;
        static final int TRANSACTION_getProfileOwnerName = 61;
        static final int TRANSACTION_getProfileWithMinimumFailedPasswordsForWipe = 24;
        static final int TRANSACTION_getRemoveWarning = 48;
        static final int TRANSACTION_getRestrictionsProvider = 75;
        static final int TRANSACTION_getScreenCaptureDisabled = 41;
        static final int TRANSACTION_getStorageEncryption = 36;
        static final int TRANSACTION_getStorageEncryptionStatus = 37;
        static final int TRANSACTION_getTrustAgentConfiguration = 110;
        static final int TRANSACTION_hasGrantedPolicy = 50;
        static final int TRANSACTION_hasUserSetupCompleted = 65;
        static final int TRANSACTION_installCaCert = 66;
        static final int TRANSACTION_installKeyPair = 69;
        static final int TRANSACTION_isActivePasswordSufficient = 22;
        static final int TRANSACTION_isAdminActive = 45;
        static final int TRANSACTION_isApplicationHidden = 86;
        static final int TRANSACTION_isDeviceOwner = 55;
        static final int TRANSACTION_isLockTaskPermitted = 98;
        static final int TRANSACTION_isMasterVolumeMuted = 102;
        static final int TRANSACTION_isRemovingAdmin = 116;
        static final int TRANSACTION_isUninstallBlocked = 105;
        static final int TRANSACTION_lockNow = 30;
        static final int TRANSACTION_notifyLockTaskModeChanged = 103;
        static final int TRANSACTION_packageHasActiveAdmins = 47;
        static final int TRANSACTION_removeActiveAdmin = 49;
        static final int TRANSACTION_removeCrossProfileWidgetProvider = 112;
        static final int TRANSACTION_removeUser = 89;
        static final int TRANSACTION_reportFailedPasswordAttempt = 52;
        static final int TRANSACTION_reportSuccessfulPasswordAttempt = 53;
        static final int TRANSACTION_resetPassword = 27;
        static final int TRANSACTION_setAccountManagementDisabled = 93;
        static final int TRANSACTION_setActiveAdmin = 44;
        static final int TRANSACTION_setActivePasswordState = 51;
        static final int TRANSACTION_setApplicationHidden = 85;
        static final int TRANSACTION_setApplicationRestrictions = 72;
        static final int TRANSACTION_setAutoTimeRequired = 114;
        static final int TRANSACTION_setCameraDisabled = 38;
        static final int TRANSACTION_setCrossProfileCallerIdDisabled = 106;
        static final int TRANSACTION_setDeviceOwner = 54;
        static final int TRANSACTION_setGlobalProxy = 32;
        static final int TRANSACTION_setGlobalSetting = 99;
        static final int TRANSACTION_setKeyguardDisabledFeatures = 42;
        static final int TRANSACTION_setLockTaskPackages = 96;
        static final int TRANSACTION_setMasterVolumeMuted = 101;
        static final int TRANSACTION_setMaximumFailedPasswordsForWipe = 25;
        static final int TRANSACTION_setMaximumTimeToLock = 28;
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
        static final int TRANSACTION_setPermittedAccessibilityServices = 79;
        static final int TRANSACTION_setPermittedInputMethods = 82;
        static final int TRANSACTION_setProfileEnabled = 62;
        static final int TRANSACTION_setProfileName = 63;
        static final int TRANSACTION_setProfileOwner = 59;
        static final int TRANSACTION_setRecommendedGlobalProxy = 34;
        static final int TRANSACTION_setRestrictionsProvider = 74;
        static final int TRANSACTION_setScreenCaptureDisabled = 40;
        static final int TRANSACTION_setSecureSetting = 100;
        static final int TRANSACTION_setStorageEncryption = 35;
        static final int TRANSACTION_setTrustAgentConfiguration = 109;
        static final int TRANSACTION_setUninstallBlocked = 104;
        static final int TRANSACTION_setUserRestriction = 76;
        static final int TRANSACTION_switchUser = 90;
        static final int TRANSACTION_uninstallCaCert = 67;
        static final int TRANSACTION_wipeData = 31;

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
            ComponentName _arg0;
            ComponentName _arg02;
            ComponentName _arg03;
            ComponentName _arg04;
            ComponentName _arg05;
            ComponentName _arg06;
            ComponentName _arg1;
            ComponentName _arg07;
            ComponentName _arg12;
            PersistableBundle _arg2;
            ComponentName _arg08;
            ComponentName _arg09;
            ComponentName _arg010;
            ComponentName _arg011;
            ComponentName _arg012;
            ComponentName _arg013;
            ComponentName _arg014;
            ComponentName _arg015;
            ComponentName _arg016;
            ComponentName _arg017;
            ComponentName _arg018;
            ComponentName _arg019;
            Intent _arg13;
            ComponentName _arg020;
            ComponentName _arg021;
            UserHandle _arg14;
            ComponentName _arg022;
            UserHandle _arg15;
            ComponentName _arg023;
            ComponentName _arg3;
            Bundle _arg4;
            ComponentName _arg024;
            ComponentName _arg025;
            ComponentName _arg026;
            ComponentName _arg027;
            ComponentName _arg028;
            ComponentName _arg029;
            ComponentName _arg030;
            ComponentName _arg031;
            ComponentName _arg032;
            IntentFilter _arg16;
            ComponentName _arg033;
            ComponentName _arg034;
            ComponentName _arg17;
            ComponentName _arg035;
            ComponentName _arg036;
            Bundle _arg22;
            ComponentName _arg037;
            ComponentName _arg038;
            IntentFilter _arg18;
            ComponentName _arg23;
            ComponentName _arg039;
            ComponentName _arg040;
            ComponentName _arg041;
            ComponentName _arg042;
            ComponentName _arg043;
            ComponentName _arg044;
            ComponentName _arg045;
            ComponentName _arg046;
            ComponentName _arg047;
            ComponentName _arg048;
            ComponentName _arg049;
            RemoteCallback _arg19;
            ComponentName _arg050;
            ComponentName _arg051;
            ComponentName _arg052;
            ComponentName _arg053;
            ComponentName _arg054;
            ComponentName _arg055;
            ComponentName _arg056;
            ComponentName _arg057;
            ComponentName _arg058;
            ComponentName _arg059;
            ComponentName _arg060;
            ProxyInfo _arg110;
            ComponentName _arg061;
            ComponentName _arg062;
            ComponentName _arg063;
            ComponentName _arg064;
            ComponentName _arg065;
            ComponentName _arg066;
            ComponentName _arg067;
            ComponentName _arg068;
            ComponentName _arg069;
            ComponentName _arg070;
            ComponentName _arg071;
            ComponentName _arg072;
            ComponentName _arg073;
            ComponentName _arg074;
            ComponentName _arg075;
            ComponentName _arg076;
            ComponentName _arg077;
            ComponentName _arg078;
            ComponentName _arg079;
            ComponentName _arg080;
            ComponentName _arg081;
            ComponentName _arg082;
            ComponentName _arg083;
            ComponentName _arg084;
            ComponentName _arg085;
            ComponentName _arg086;
            switch (code) {
                case 1:
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        _arg086 = ComponentName.CREATOR.createFromParcel(data);
                    } else {
                        _arg086 = null;
                    }
                    int _arg111 = data.readInt();
                    int _arg24 = data.readInt();
                    setPasswordQuality(_arg086, _arg111, _arg24);
                    reply.writeNoException();
                    return true;
                case 2:
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        _arg085 = ComponentName.CREATOR.createFromParcel(data);
                    } else {
                        _arg085 = null;
                    }
                    int _arg112 = data.readInt();
                    int _result = getPasswordQuality(_arg085, _arg112);
                    reply.writeNoException();
                    reply.writeInt(_result);
                    return true;
                case 3:
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        _arg084 = ComponentName.CREATOR.createFromParcel(data);
                    } else {
                        _arg084 = null;
                    }
                    int _arg113 = data.readInt();
                    int _arg25 = data.readInt();
                    setPasswordMinimumLength(_arg084, _arg113, _arg25);
                    reply.writeNoException();
                    return true;
                case 4:
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        _arg083 = ComponentName.CREATOR.createFromParcel(data);
                    } else {
                        _arg083 = null;
                    }
                    int _arg114 = data.readInt();
                    int _result2 = getPasswordMinimumLength(_arg083, _arg114);
                    reply.writeNoException();
                    reply.writeInt(_result2);
                    return true;
                case 5:
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        _arg082 = ComponentName.CREATOR.createFromParcel(data);
                    } else {
                        _arg082 = null;
                    }
                    int _arg115 = data.readInt();
                    int _arg26 = data.readInt();
                    setPasswordMinimumUpperCase(_arg082, _arg115, _arg26);
                    reply.writeNoException();
                    return true;
                case 6:
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        _arg081 = ComponentName.CREATOR.createFromParcel(data);
                    } else {
                        _arg081 = null;
                    }
                    int _arg116 = data.readInt();
                    int _result3 = getPasswordMinimumUpperCase(_arg081, _arg116);
                    reply.writeNoException();
                    reply.writeInt(_result3);
                    return true;
                case 7:
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        _arg080 = ComponentName.CREATOR.createFromParcel(data);
                    } else {
                        _arg080 = null;
                    }
                    int _arg117 = data.readInt();
                    int _arg27 = data.readInt();
                    setPasswordMinimumLowerCase(_arg080, _arg117, _arg27);
                    reply.writeNoException();
                    return true;
                case 8:
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        _arg079 = ComponentName.CREATOR.createFromParcel(data);
                    } else {
                        _arg079 = null;
                    }
                    int _arg118 = data.readInt();
                    int _result4 = getPasswordMinimumLowerCase(_arg079, _arg118);
                    reply.writeNoException();
                    reply.writeInt(_result4);
                    return true;
                case 9:
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        _arg078 = ComponentName.CREATOR.createFromParcel(data);
                    } else {
                        _arg078 = null;
                    }
                    int _arg119 = data.readInt();
                    int _arg28 = data.readInt();
                    setPasswordMinimumLetters(_arg078, _arg119, _arg28);
                    reply.writeNoException();
                    return true;
                case 10:
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        _arg077 = ComponentName.CREATOR.createFromParcel(data);
                    } else {
                        _arg077 = null;
                    }
                    int _arg120 = data.readInt();
                    int _result5 = getPasswordMinimumLetters(_arg077, _arg120);
                    reply.writeNoException();
                    reply.writeInt(_result5);
                    return true;
                case 11:
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        _arg076 = ComponentName.CREATOR.createFromParcel(data);
                    } else {
                        _arg076 = null;
                    }
                    int _arg121 = data.readInt();
                    int _arg29 = data.readInt();
                    setPasswordMinimumNumeric(_arg076, _arg121, _arg29);
                    reply.writeNoException();
                    return true;
                case 12:
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        _arg075 = ComponentName.CREATOR.createFromParcel(data);
                    } else {
                        _arg075 = null;
                    }
                    int _arg122 = data.readInt();
                    int _result6 = getPasswordMinimumNumeric(_arg075, _arg122);
                    reply.writeNoException();
                    reply.writeInt(_result6);
                    return true;
                case 13:
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        _arg074 = ComponentName.CREATOR.createFromParcel(data);
                    } else {
                        _arg074 = null;
                    }
                    int _arg123 = data.readInt();
                    int _arg210 = data.readInt();
                    setPasswordMinimumSymbols(_arg074, _arg123, _arg210);
                    reply.writeNoException();
                    return true;
                case 14:
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        _arg073 = ComponentName.CREATOR.createFromParcel(data);
                    } else {
                        _arg073 = null;
                    }
                    int _arg124 = data.readInt();
                    int _result7 = getPasswordMinimumSymbols(_arg073, _arg124);
                    reply.writeNoException();
                    reply.writeInt(_result7);
                    return true;
                case 15:
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        _arg072 = ComponentName.CREATOR.createFromParcel(data);
                    } else {
                        _arg072 = null;
                    }
                    int _arg125 = data.readInt();
                    int _arg211 = data.readInt();
                    setPasswordMinimumNonLetter(_arg072, _arg125, _arg211);
                    reply.writeNoException();
                    return true;
                case 16:
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        _arg071 = ComponentName.CREATOR.createFromParcel(data);
                    } else {
                        _arg071 = null;
                    }
                    int _arg126 = data.readInt();
                    int _result8 = getPasswordMinimumNonLetter(_arg071, _arg126);
                    reply.writeNoException();
                    reply.writeInt(_result8);
                    return true;
                case 17:
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        _arg070 = ComponentName.CREATOR.createFromParcel(data);
                    } else {
                        _arg070 = null;
                    }
                    int _arg127 = data.readInt();
                    int _arg212 = data.readInt();
                    setPasswordHistoryLength(_arg070, _arg127, _arg212);
                    reply.writeNoException();
                    return true;
                case 18:
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        _arg069 = ComponentName.CREATOR.createFromParcel(data);
                    } else {
                        _arg069 = null;
                    }
                    int _arg128 = data.readInt();
                    int _result9 = getPasswordHistoryLength(_arg069, _arg128);
                    reply.writeNoException();
                    reply.writeInt(_result9);
                    return true;
                case 19:
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        _arg068 = ComponentName.CREATOR.createFromParcel(data);
                    } else {
                        _arg068 = null;
                    }
                    long _arg129 = data.readLong();
                    int _arg213 = data.readInt();
                    setPasswordExpirationTimeout(_arg068, _arg129, _arg213);
                    reply.writeNoException();
                    return true;
                case 20:
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        _arg067 = ComponentName.CREATOR.createFromParcel(data);
                    } else {
                        _arg067 = null;
                    }
                    int _arg130 = data.readInt();
                    long _result10 = getPasswordExpirationTimeout(_arg067, _arg130);
                    reply.writeNoException();
                    reply.writeLong(_result10);
                    return true;
                case 21:
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        _arg066 = ComponentName.CREATOR.createFromParcel(data);
                    } else {
                        _arg066 = null;
                    }
                    int _arg131 = data.readInt();
                    long _result11 = getPasswordExpiration(_arg066, _arg131);
                    reply.writeNoException();
                    reply.writeLong(_result11);
                    return true;
                case 22:
                    data.enforceInterface(DESCRIPTOR);
                    int _arg087 = data.readInt();
                    boolean _result12 = isActivePasswordSufficient(_arg087);
                    reply.writeNoException();
                    reply.writeInt(_result12 ? 1 : 0);
                    return true;
                case 23:
                    data.enforceInterface(DESCRIPTOR);
                    int _arg088 = data.readInt();
                    int _result13 = getCurrentFailedPasswordAttempts(_arg088);
                    reply.writeNoException();
                    reply.writeInt(_result13);
                    return true;
                case 24:
                    data.enforceInterface(DESCRIPTOR);
                    int _arg089 = data.readInt();
                    int _result14 = getProfileWithMinimumFailedPasswordsForWipe(_arg089);
                    reply.writeNoException();
                    reply.writeInt(_result14);
                    return true;
                case 25:
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        _arg065 = ComponentName.CREATOR.createFromParcel(data);
                    } else {
                        _arg065 = null;
                    }
                    int _arg132 = data.readInt();
                    int _arg214 = data.readInt();
                    setMaximumFailedPasswordsForWipe(_arg065, _arg132, _arg214);
                    reply.writeNoException();
                    return true;
                case 26:
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        _arg064 = ComponentName.CREATOR.createFromParcel(data);
                    } else {
                        _arg064 = null;
                    }
                    int _arg133 = data.readInt();
                    int _result15 = getMaximumFailedPasswordsForWipe(_arg064, _arg133);
                    reply.writeNoException();
                    reply.writeInt(_result15);
                    return true;
                case 27:
                    data.enforceInterface(DESCRIPTOR);
                    String _arg090 = data.readString();
                    int _arg134 = data.readInt();
                    int _arg215 = data.readInt();
                    boolean _result16 = resetPassword(_arg090, _arg134, _arg215);
                    reply.writeNoException();
                    reply.writeInt(_result16 ? 1 : 0);
                    return true;
                case 28:
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        _arg063 = ComponentName.CREATOR.createFromParcel(data);
                    } else {
                        _arg063 = null;
                    }
                    long _arg135 = data.readLong();
                    int _arg216 = data.readInt();
                    setMaximumTimeToLock(_arg063, _arg135, _arg216);
                    reply.writeNoException();
                    return true;
                case 29:
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        _arg062 = ComponentName.CREATOR.createFromParcel(data);
                    } else {
                        _arg062 = null;
                    }
                    int _arg136 = data.readInt();
                    long _result17 = getMaximumTimeToLock(_arg062, _arg136);
                    reply.writeNoException();
                    reply.writeLong(_result17);
                    return true;
                case 30:
                    data.enforceInterface(DESCRIPTOR);
                    lockNow();
                    reply.writeNoException();
                    return true;
                case 31:
                    data.enforceInterface(DESCRIPTOR);
                    int _arg091 = data.readInt();
                    int _arg137 = data.readInt();
                    wipeData(_arg091, _arg137);
                    reply.writeNoException();
                    return true;
                case 32:
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        _arg061 = ComponentName.CREATOR.createFromParcel(data);
                    } else {
                        _arg061 = null;
                    }
                    String _arg138 = data.readString();
                    String _arg217 = data.readString();
                    int _arg32 = data.readInt();
                    ComponentName _result18 = setGlobalProxy(_arg061, _arg138, _arg217, _arg32);
                    reply.writeNoException();
                    if (_result18 != null) {
                        reply.writeInt(1);
                        _result18.writeToParcel(reply, 1);
                    } else {
                        reply.writeInt(0);
                    }
                    return true;
                case 33:
                    data.enforceInterface(DESCRIPTOR);
                    int _arg092 = data.readInt();
                    ComponentName _result19 = getGlobalProxyAdmin(_arg092);
                    reply.writeNoException();
                    if (_result19 != null) {
                        reply.writeInt(1);
                        _result19.writeToParcel(reply, 1);
                    } else {
                        reply.writeInt(0);
                    }
                    return true;
                case 34:
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        _arg060 = ComponentName.CREATOR.createFromParcel(data);
                    } else {
                        _arg060 = null;
                    }
                    if (data.readInt() != 0) {
                        _arg110 = ProxyInfo.CREATOR.createFromParcel(data);
                    } else {
                        _arg110 = null;
                    }
                    setRecommendedGlobalProxy(_arg060, _arg110);
                    reply.writeNoException();
                    return true;
                case 35:
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        _arg059 = ComponentName.CREATOR.createFromParcel(data);
                    } else {
                        _arg059 = null;
                    }
                    boolean _arg139 = data.readInt() != 0;
                    int _arg218 = data.readInt();
                    int _result20 = setStorageEncryption(_arg059, _arg139, _arg218);
                    reply.writeNoException();
                    reply.writeInt(_result20);
                    return true;
                case 36:
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        _arg058 = ComponentName.CREATOR.createFromParcel(data);
                    } else {
                        _arg058 = null;
                    }
                    int _arg140 = data.readInt();
                    boolean _result21 = getStorageEncryption(_arg058, _arg140);
                    reply.writeNoException();
                    reply.writeInt(_result21 ? 1 : 0);
                    return true;
                case 37:
                    data.enforceInterface(DESCRIPTOR);
                    int _arg093 = data.readInt();
                    int _result22 = getStorageEncryptionStatus(_arg093);
                    reply.writeNoException();
                    reply.writeInt(_result22);
                    return true;
                case 38:
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        _arg057 = ComponentName.CREATOR.createFromParcel(data);
                    } else {
                        _arg057 = null;
                    }
                    boolean _arg141 = data.readInt() != 0;
                    int _arg219 = data.readInt();
                    setCameraDisabled(_arg057, _arg141, _arg219);
                    reply.writeNoException();
                    return true;
                case 39:
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        _arg056 = ComponentName.CREATOR.createFromParcel(data);
                    } else {
                        _arg056 = null;
                    }
                    int _arg142 = data.readInt();
                    boolean _result23 = getCameraDisabled(_arg056, _arg142);
                    reply.writeNoException();
                    reply.writeInt(_result23 ? 1 : 0);
                    return true;
                case 40:
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        _arg055 = ComponentName.CREATOR.createFromParcel(data);
                    } else {
                        _arg055 = null;
                    }
                    int _arg143 = data.readInt();
                    boolean _arg220 = data.readInt() != 0;
                    setScreenCaptureDisabled(_arg055, _arg143, _arg220);
                    reply.writeNoException();
                    return true;
                case 41:
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        _arg054 = ComponentName.CREATOR.createFromParcel(data);
                    } else {
                        _arg054 = null;
                    }
                    int _arg144 = data.readInt();
                    boolean _result24 = getScreenCaptureDisabled(_arg054, _arg144);
                    reply.writeNoException();
                    reply.writeInt(_result24 ? 1 : 0);
                    return true;
                case 42:
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        _arg053 = ComponentName.CREATOR.createFromParcel(data);
                    } else {
                        _arg053 = null;
                    }
                    int _arg145 = data.readInt();
                    int _arg221 = data.readInt();
                    setKeyguardDisabledFeatures(_arg053, _arg145, _arg221);
                    reply.writeNoException();
                    return true;
                case 43:
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        _arg052 = ComponentName.CREATOR.createFromParcel(data);
                    } else {
                        _arg052 = null;
                    }
                    int _arg146 = data.readInt();
                    int _result25 = getKeyguardDisabledFeatures(_arg052, _arg146);
                    reply.writeNoException();
                    reply.writeInt(_result25);
                    return true;
                case 44:
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        _arg051 = ComponentName.CREATOR.createFromParcel(data);
                    } else {
                        _arg051 = null;
                    }
                    boolean _arg147 = data.readInt() != 0;
                    int _arg222 = data.readInt();
                    setActiveAdmin(_arg051, _arg147, _arg222);
                    reply.writeNoException();
                    return true;
                case 45:
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        _arg050 = ComponentName.CREATOR.createFromParcel(data);
                    } else {
                        _arg050 = null;
                    }
                    int _arg148 = data.readInt();
                    boolean _result26 = isAdminActive(_arg050, _arg148);
                    reply.writeNoException();
                    reply.writeInt(_result26 ? 1 : 0);
                    return true;
                case 46:
                    data.enforceInterface(DESCRIPTOR);
                    int _arg094 = data.readInt();
                    List<ComponentName> _result27 = getActiveAdmins(_arg094);
                    reply.writeNoException();
                    reply.writeTypedList(_result27);
                    return true;
                case 47:
                    data.enforceInterface(DESCRIPTOR);
                    String _arg095 = data.readString();
                    int _arg149 = data.readInt();
                    boolean _result28 = packageHasActiveAdmins(_arg095, _arg149);
                    reply.writeNoException();
                    reply.writeInt(_result28 ? 1 : 0);
                    return true;
                case 48:
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        _arg049 = ComponentName.CREATOR.createFromParcel(data);
                    } else {
                        _arg049 = null;
                    }
                    if (data.readInt() != 0) {
                        _arg19 = RemoteCallback.CREATOR.createFromParcel(data);
                    } else {
                        _arg19 = null;
                    }
                    int _arg223 = data.readInt();
                    getRemoveWarning(_arg049, _arg19, _arg223);
                    reply.writeNoException();
                    return true;
                case 49:
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        _arg048 = ComponentName.CREATOR.createFromParcel(data);
                    } else {
                        _arg048 = null;
                    }
                    int _arg150 = data.readInt();
                    removeActiveAdmin(_arg048, _arg150);
                    reply.writeNoException();
                    return true;
                case 50:
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        _arg047 = ComponentName.CREATOR.createFromParcel(data);
                    } else {
                        _arg047 = null;
                    }
                    int _arg151 = data.readInt();
                    int _arg224 = data.readInt();
                    boolean _result29 = hasGrantedPolicy(_arg047, _arg151, _arg224);
                    reply.writeNoException();
                    reply.writeInt(_result29 ? 1 : 0);
                    return true;
                case 51:
                    data.enforceInterface(DESCRIPTOR);
                    int _arg096 = data.readInt();
                    int _arg152 = data.readInt();
                    int _arg225 = data.readInt();
                    int _arg33 = data.readInt();
                    int _arg42 = data.readInt();
                    int _arg5 = data.readInt();
                    int _arg6 = data.readInt();
                    int _arg7 = data.readInt();
                    int _arg8 = data.readInt();
                    setActivePasswordState(_arg096, _arg152, _arg225, _arg33, _arg42, _arg5, _arg6, _arg7, _arg8);
                    reply.writeNoException();
                    return true;
                case 52:
                    data.enforceInterface(DESCRIPTOR);
                    int _arg097 = data.readInt();
                    reportFailedPasswordAttempt(_arg097);
                    reply.writeNoException();
                    return true;
                case 53:
                    data.enforceInterface(DESCRIPTOR);
                    int _arg098 = data.readInt();
                    reportSuccessfulPasswordAttempt(_arg098);
                    reply.writeNoException();
                    return true;
                case 54:
                    data.enforceInterface(DESCRIPTOR);
                    String _arg099 = data.readString();
                    String _arg153 = data.readString();
                    boolean _result30 = setDeviceOwner(_arg099, _arg153);
                    reply.writeNoException();
                    reply.writeInt(_result30 ? 1 : 0);
                    return true;
                case 55:
                    data.enforceInterface(DESCRIPTOR);
                    String _arg0100 = data.readString();
                    boolean _result31 = isDeviceOwner(_arg0100);
                    reply.writeNoException();
                    reply.writeInt(_result31 ? 1 : 0);
                    return true;
                case 56:
                    data.enforceInterface(DESCRIPTOR);
                    String _result32 = getDeviceOwner();
                    reply.writeNoException();
                    reply.writeString(_result32);
                    return true;
                case 57:
                    data.enforceInterface(DESCRIPTOR);
                    String _result33 = getDeviceOwnerName();
                    reply.writeNoException();
                    reply.writeString(_result33);
                    return true;
                case 58:
                    data.enforceInterface(DESCRIPTOR);
                    String _arg0101 = data.readString();
                    clearDeviceOwner(_arg0101);
                    reply.writeNoException();
                    return true;
                case 59:
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        _arg046 = ComponentName.CREATOR.createFromParcel(data);
                    } else {
                        _arg046 = null;
                    }
                    String _arg154 = data.readString();
                    int _arg226 = data.readInt();
                    boolean _result34 = setProfileOwner(_arg046, _arg154, _arg226);
                    reply.writeNoException();
                    reply.writeInt(_result34 ? 1 : 0);
                    return true;
                case 60:
                    data.enforceInterface(DESCRIPTOR);
                    int _arg0102 = data.readInt();
                    ComponentName _result35 = getProfileOwner(_arg0102);
                    reply.writeNoException();
                    if (_result35 != null) {
                        reply.writeInt(1);
                        _result35.writeToParcel(reply, 1);
                    } else {
                        reply.writeInt(0);
                    }
                    return true;
                case 61:
                    data.enforceInterface(DESCRIPTOR);
                    int _arg0103 = data.readInt();
                    String _result36 = getProfileOwnerName(_arg0103);
                    reply.writeNoException();
                    reply.writeString(_result36);
                    return true;
                case 62:
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        _arg045 = ComponentName.CREATOR.createFromParcel(data);
                    } else {
                        _arg045 = null;
                    }
                    setProfileEnabled(_arg045);
                    reply.writeNoException();
                    return true;
                case 63:
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        _arg044 = ComponentName.CREATOR.createFromParcel(data);
                    } else {
                        _arg044 = null;
                    }
                    String _arg155 = data.readString();
                    setProfileName(_arg044, _arg155);
                    reply.writeNoException();
                    return true;
                case 64:
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        _arg043 = ComponentName.CREATOR.createFromParcel(data);
                    } else {
                        _arg043 = null;
                    }
                    clearProfileOwner(_arg043);
                    reply.writeNoException();
                    return true;
                case 65:
                    data.enforceInterface(DESCRIPTOR);
                    boolean _result37 = hasUserSetupCompleted();
                    reply.writeNoException();
                    reply.writeInt(_result37 ? 1 : 0);
                    return true;
                case 66:
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        _arg042 = ComponentName.CREATOR.createFromParcel(data);
                    } else {
                        _arg042 = null;
                    }
                    byte[] _arg156 = data.createByteArray();
                    boolean _result38 = installCaCert(_arg042, _arg156);
                    reply.writeNoException();
                    reply.writeInt(_result38 ? 1 : 0);
                    return true;
                case 67:
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        _arg041 = ComponentName.CREATOR.createFromParcel(data);
                    } else {
                        _arg041 = null;
                    }
                    String _arg157 = data.readString();
                    uninstallCaCert(_arg041, _arg157);
                    reply.writeNoException();
                    return true;
                case 68:
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        _arg040 = ComponentName.CREATOR.createFromParcel(data);
                    } else {
                        _arg040 = null;
                    }
                    enforceCanManageCaCerts(_arg040);
                    reply.writeNoException();
                    return true;
                case 69:
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        _arg039 = ComponentName.CREATOR.createFromParcel(data);
                    } else {
                        _arg039 = null;
                    }
                    byte[] _arg158 = data.createByteArray();
                    byte[] _arg227 = data.createByteArray();
                    String _arg34 = data.readString();
                    boolean _result39 = installKeyPair(_arg039, _arg158, _arg227, _arg34);
                    reply.writeNoException();
                    reply.writeInt(_result39 ? 1 : 0);
                    return true;
                case 70:
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        _arg038 = ComponentName.CREATOR.createFromParcel(data);
                    } else {
                        _arg038 = null;
                    }
                    if (data.readInt() != 0) {
                        _arg18 = IntentFilter.CREATOR.createFromParcel(data);
                    } else {
                        _arg18 = null;
                    }
                    if (data.readInt() != 0) {
                        _arg23 = ComponentName.CREATOR.createFromParcel(data);
                    } else {
                        _arg23 = null;
                    }
                    addPersistentPreferredActivity(_arg038, _arg18, _arg23);
                    reply.writeNoException();
                    return true;
                case 71:
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        _arg037 = ComponentName.CREATOR.createFromParcel(data);
                    } else {
                        _arg037 = null;
                    }
                    String _arg159 = data.readString();
                    clearPackagePersistentPreferredActivities(_arg037, _arg159);
                    reply.writeNoException();
                    return true;
                case 72:
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        _arg036 = ComponentName.CREATOR.createFromParcel(data);
                    } else {
                        _arg036 = null;
                    }
                    String _arg160 = data.readString();
                    if (data.readInt() != 0) {
                        _arg22 = Bundle.CREATOR.createFromParcel(data);
                    } else {
                        _arg22 = null;
                    }
                    setApplicationRestrictions(_arg036, _arg160, _arg22);
                    reply.writeNoException();
                    return true;
                case 73:
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        _arg035 = ComponentName.CREATOR.createFromParcel(data);
                    } else {
                        _arg035 = null;
                    }
                    String _arg161 = data.readString();
                    Bundle _result40 = getApplicationRestrictions(_arg035, _arg161);
                    reply.writeNoException();
                    if (_result40 != null) {
                        reply.writeInt(1);
                        _result40.writeToParcel(reply, 1);
                    } else {
                        reply.writeInt(0);
                    }
                    return true;
                case 74:
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        _arg034 = ComponentName.CREATOR.createFromParcel(data);
                    } else {
                        _arg034 = null;
                    }
                    if (data.readInt() != 0) {
                        _arg17 = ComponentName.CREATOR.createFromParcel(data);
                    } else {
                        _arg17 = null;
                    }
                    setRestrictionsProvider(_arg034, _arg17);
                    reply.writeNoException();
                    return true;
                case 75:
                    data.enforceInterface(DESCRIPTOR);
                    int _arg0104 = data.readInt();
                    ComponentName _result41 = getRestrictionsProvider(_arg0104);
                    reply.writeNoException();
                    if (_result41 != null) {
                        reply.writeInt(1);
                        _result41.writeToParcel(reply, 1);
                    } else {
                        reply.writeInt(0);
                    }
                    return true;
                case 76:
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        _arg033 = ComponentName.CREATOR.createFromParcel(data);
                    } else {
                        _arg033 = null;
                    }
                    String _arg162 = data.readString();
                    boolean _arg228 = data.readInt() != 0;
                    setUserRestriction(_arg033, _arg162, _arg228);
                    reply.writeNoException();
                    return true;
                case 77:
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        _arg032 = ComponentName.CREATOR.createFromParcel(data);
                    } else {
                        _arg032 = null;
                    }
                    if (data.readInt() != 0) {
                        _arg16 = IntentFilter.CREATOR.createFromParcel(data);
                    } else {
                        _arg16 = null;
                    }
                    int _arg229 = data.readInt();
                    addCrossProfileIntentFilter(_arg032, _arg16, _arg229);
                    reply.writeNoException();
                    return true;
                case 78:
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        _arg031 = ComponentName.CREATOR.createFromParcel(data);
                    } else {
                        _arg031 = null;
                    }
                    clearCrossProfileIntentFilters(_arg031);
                    reply.writeNoException();
                    return true;
                case 79:
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        _arg030 = ComponentName.CREATOR.createFromParcel(data);
                    } else {
                        _arg030 = null;
                    }
                    ClassLoader cl = getClass().getClassLoader();
                    List _arg163 = data.readArrayList(cl);
                    boolean _result42 = setPermittedAccessibilityServices(_arg030, _arg163);
                    reply.writeNoException();
                    reply.writeInt(_result42 ? 1 : 0);
                    return true;
                case 80:
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        _arg029 = ComponentName.CREATOR.createFromParcel(data);
                    } else {
                        _arg029 = null;
                    }
                    List _result43 = getPermittedAccessibilityServices(_arg029);
                    reply.writeNoException();
                    reply.writeList(_result43);
                    return true;
                case 81:
                    data.enforceInterface(DESCRIPTOR);
                    int _arg0105 = data.readInt();
                    List _result44 = getPermittedAccessibilityServicesForUser(_arg0105);
                    reply.writeNoException();
                    reply.writeList(_result44);
                    return true;
                case 82:
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        _arg028 = ComponentName.CREATOR.createFromParcel(data);
                    } else {
                        _arg028 = null;
                    }
                    ClassLoader cl2 = getClass().getClassLoader();
                    List _arg164 = data.readArrayList(cl2);
                    boolean _result45 = setPermittedInputMethods(_arg028, _arg164);
                    reply.writeNoException();
                    reply.writeInt(_result45 ? 1 : 0);
                    return true;
                case 83:
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        _arg027 = ComponentName.CREATOR.createFromParcel(data);
                    } else {
                        _arg027 = null;
                    }
                    List _result46 = getPermittedInputMethods(_arg027);
                    reply.writeNoException();
                    reply.writeList(_result46);
                    return true;
                case 84:
                    data.enforceInterface(DESCRIPTOR);
                    List _result47 = getPermittedInputMethodsForCurrentUser();
                    reply.writeNoException();
                    reply.writeList(_result47);
                    return true;
                case 85:
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        _arg026 = ComponentName.CREATOR.createFromParcel(data);
                    } else {
                        _arg026 = null;
                    }
                    String _arg165 = data.readString();
                    boolean _arg230 = data.readInt() != 0;
                    boolean _result48 = setApplicationHidden(_arg026, _arg165, _arg230);
                    reply.writeNoException();
                    reply.writeInt(_result48 ? 1 : 0);
                    return true;
                case 86:
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        _arg025 = ComponentName.CREATOR.createFromParcel(data);
                    } else {
                        _arg025 = null;
                    }
                    String _arg166 = data.readString();
                    boolean _result49 = isApplicationHidden(_arg025, _arg166);
                    reply.writeNoException();
                    reply.writeInt(_result49 ? 1 : 0);
                    return true;
                case 87:
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        _arg024 = ComponentName.CREATOR.createFromParcel(data);
                    } else {
                        _arg024 = null;
                    }
                    String _arg167 = data.readString();
                    UserHandle _result50 = createUser(_arg024, _arg167);
                    reply.writeNoException();
                    if (_result50 != null) {
                        reply.writeInt(1);
                        _result50.writeToParcel(reply, 1);
                    } else {
                        reply.writeInt(0);
                    }
                    return true;
                case 88:
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        _arg023 = ComponentName.CREATOR.createFromParcel(data);
                    } else {
                        _arg023 = null;
                    }
                    String _arg168 = data.readString();
                    String _arg231 = data.readString();
                    if (data.readInt() != 0) {
                        _arg3 = ComponentName.CREATOR.createFromParcel(data);
                    } else {
                        _arg3 = null;
                    }
                    if (data.readInt() != 0) {
                        _arg4 = Bundle.CREATOR.createFromParcel(data);
                    } else {
                        _arg4 = null;
                    }
                    UserHandle _result51 = createAndInitializeUser(_arg023, _arg168, _arg231, _arg3, _arg4);
                    reply.writeNoException();
                    if (_result51 != null) {
                        reply.writeInt(1);
                        _result51.writeToParcel(reply, 1);
                    } else {
                        reply.writeInt(0);
                    }
                    return true;
                case 89:
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        _arg022 = ComponentName.CREATOR.createFromParcel(data);
                    } else {
                        _arg022 = null;
                    }
                    if (data.readInt() != 0) {
                        _arg15 = UserHandle.CREATOR.createFromParcel(data);
                    } else {
                        _arg15 = null;
                    }
                    boolean _result52 = removeUser(_arg022, _arg15);
                    reply.writeNoException();
                    reply.writeInt(_result52 ? 1 : 0);
                    return true;
                case 90:
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        _arg021 = ComponentName.CREATOR.createFromParcel(data);
                    } else {
                        _arg021 = null;
                    }
                    if (data.readInt() != 0) {
                        _arg14 = UserHandle.CREATOR.createFromParcel(data);
                    } else {
                        _arg14 = null;
                    }
                    boolean _result53 = switchUser(_arg021, _arg14);
                    reply.writeNoException();
                    reply.writeInt(_result53 ? 1 : 0);
                    return true;
                case 91:
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        _arg020 = ComponentName.CREATOR.createFromParcel(data);
                    } else {
                        _arg020 = null;
                    }
                    String _arg169 = data.readString();
                    enableSystemApp(_arg020, _arg169);
                    reply.writeNoException();
                    return true;
                case 92:
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        _arg019 = ComponentName.CREATOR.createFromParcel(data);
                    } else {
                        _arg019 = null;
                    }
                    if (data.readInt() != 0) {
                        _arg13 = Intent.CREATOR.createFromParcel(data);
                    } else {
                        _arg13 = null;
                    }
                    int _result54 = enableSystemAppWithIntent(_arg019, _arg13);
                    reply.writeNoException();
                    reply.writeInt(_result54);
                    return true;
                case 93:
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        _arg018 = ComponentName.CREATOR.createFromParcel(data);
                    } else {
                        _arg018 = null;
                    }
                    String _arg170 = data.readString();
                    boolean _arg232 = data.readInt() != 0;
                    setAccountManagementDisabled(_arg018, _arg170, _arg232);
                    reply.writeNoException();
                    return true;
                case 94:
                    data.enforceInterface(DESCRIPTOR);
                    String[] _result55 = getAccountTypesWithManagementDisabled();
                    reply.writeNoException();
                    reply.writeStringArray(_result55);
                    return true;
                case 95:
                    data.enforceInterface(DESCRIPTOR);
                    int _arg0106 = data.readInt();
                    String[] _result56 = getAccountTypesWithManagementDisabledAsUser(_arg0106);
                    reply.writeNoException();
                    reply.writeStringArray(_result56);
                    return true;
                case 96:
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        _arg017 = ComponentName.CREATOR.createFromParcel(data);
                    } else {
                        _arg017 = null;
                    }
                    String[] _arg171 = data.createStringArray();
                    setLockTaskPackages(_arg017, _arg171);
                    reply.writeNoException();
                    return true;
                case 97:
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        _arg016 = ComponentName.CREATOR.createFromParcel(data);
                    } else {
                        _arg016 = null;
                    }
                    String[] _result57 = getLockTaskPackages(_arg016);
                    reply.writeNoException();
                    reply.writeStringArray(_result57);
                    return true;
                case 98:
                    data.enforceInterface(DESCRIPTOR);
                    String _arg0107 = data.readString();
                    boolean _result58 = isLockTaskPermitted(_arg0107);
                    reply.writeNoException();
                    reply.writeInt(_result58 ? 1 : 0);
                    return true;
                case 99:
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        _arg015 = ComponentName.CREATOR.createFromParcel(data);
                    } else {
                        _arg015 = null;
                    }
                    String _arg172 = data.readString();
                    String _arg233 = data.readString();
                    setGlobalSetting(_arg015, _arg172, _arg233);
                    reply.writeNoException();
                    return true;
                case 100:
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        _arg014 = ComponentName.CREATOR.createFromParcel(data);
                    } else {
                        _arg014 = null;
                    }
                    String _arg173 = data.readString();
                    String _arg234 = data.readString();
                    setSecureSetting(_arg014, _arg173, _arg234);
                    reply.writeNoException();
                    return true;
                case 101:
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        _arg013 = ComponentName.CREATOR.createFromParcel(data);
                    } else {
                        _arg013 = null;
                    }
                    boolean _arg174 = data.readInt() != 0;
                    setMasterVolumeMuted(_arg013, _arg174);
                    reply.writeNoException();
                    return true;
                case 102:
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        _arg012 = ComponentName.CREATOR.createFromParcel(data);
                    } else {
                        _arg012 = null;
                    }
                    boolean _result59 = isMasterVolumeMuted(_arg012);
                    reply.writeNoException();
                    reply.writeInt(_result59 ? 1 : 0);
                    return true;
                case 103:
                    data.enforceInterface(DESCRIPTOR);
                    boolean _arg0108 = data.readInt() != 0;
                    String _arg175 = data.readString();
                    int _arg235 = data.readInt();
                    notifyLockTaskModeChanged(_arg0108, _arg175, _arg235);
                    reply.writeNoException();
                    return true;
                case 104:
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        _arg011 = ComponentName.CREATOR.createFromParcel(data);
                    } else {
                        _arg011 = null;
                    }
                    String _arg176 = data.readString();
                    boolean _arg236 = data.readInt() != 0;
                    setUninstallBlocked(_arg011, _arg176, _arg236);
                    reply.writeNoException();
                    return true;
                case 105:
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        _arg010 = ComponentName.CREATOR.createFromParcel(data);
                    } else {
                        _arg010 = null;
                    }
                    String _arg177 = data.readString();
                    boolean _result60 = isUninstallBlocked(_arg010, _arg177);
                    reply.writeNoException();
                    reply.writeInt(_result60 ? 1 : 0);
                    return true;
                case 106:
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        _arg09 = ComponentName.CREATOR.createFromParcel(data);
                    } else {
                        _arg09 = null;
                    }
                    boolean _arg178 = data.readInt() != 0;
                    setCrossProfileCallerIdDisabled(_arg09, _arg178);
                    reply.writeNoException();
                    return true;
                case 107:
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        _arg08 = ComponentName.CREATOR.createFromParcel(data);
                    } else {
                        _arg08 = null;
                    }
                    boolean _result61 = getCrossProfileCallerIdDisabled(_arg08);
                    reply.writeNoException();
                    reply.writeInt(_result61 ? 1 : 0);
                    return true;
                case 108:
                    data.enforceInterface(DESCRIPTOR);
                    int _arg0109 = data.readInt();
                    boolean _result62 = getCrossProfileCallerIdDisabledForUser(_arg0109);
                    reply.writeNoException();
                    reply.writeInt(_result62 ? 1 : 0);
                    return true;
                case 109:
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        _arg07 = ComponentName.CREATOR.createFromParcel(data);
                    } else {
                        _arg07 = null;
                    }
                    if (data.readInt() != 0) {
                        _arg12 = ComponentName.CREATOR.createFromParcel(data);
                    } else {
                        _arg12 = null;
                    }
                    if (data.readInt() != 0) {
                        _arg2 = PersistableBundle.CREATOR.createFromParcel(data);
                    } else {
                        _arg2 = null;
                    }
                    int _arg35 = data.readInt();
                    setTrustAgentConfiguration(_arg07, _arg12, _arg2, _arg35);
                    reply.writeNoException();
                    return true;
                case 110:
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        _arg06 = ComponentName.CREATOR.createFromParcel(data);
                    } else {
                        _arg06 = null;
                    }
                    if (data.readInt() != 0) {
                        _arg1 = ComponentName.CREATOR.createFromParcel(data);
                    } else {
                        _arg1 = null;
                    }
                    int _arg237 = data.readInt();
                    List<PersistableBundle> _result63 = getTrustAgentConfiguration(_arg06, _arg1, _arg237);
                    reply.writeNoException();
                    reply.writeTypedList(_result63);
                    return true;
                case 111:
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        _arg05 = ComponentName.CREATOR.createFromParcel(data);
                    } else {
                        _arg05 = null;
                    }
                    String _arg179 = data.readString();
                    boolean _result64 = addCrossProfileWidgetProvider(_arg05, _arg179);
                    reply.writeNoException();
                    reply.writeInt(_result64 ? 1 : 0);
                    return true;
                case 112:
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        _arg04 = ComponentName.CREATOR.createFromParcel(data);
                    } else {
                        _arg04 = null;
                    }
                    String _arg180 = data.readString();
                    boolean _result65 = removeCrossProfileWidgetProvider(_arg04, _arg180);
                    reply.writeNoException();
                    reply.writeInt(_result65 ? 1 : 0);
                    return true;
                case 113:
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        _arg03 = ComponentName.CREATOR.createFromParcel(data);
                    } else {
                        _arg03 = null;
                    }
                    List<String> _result66 = getCrossProfileWidgetProviders(_arg03);
                    reply.writeNoException();
                    reply.writeStringList(_result66);
                    return true;
                case 114:
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        _arg02 = ComponentName.CREATOR.createFromParcel(data);
                    } else {
                        _arg02 = null;
                    }
                    int _arg181 = data.readInt();
                    boolean _arg238 = data.readInt() != 0;
                    setAutoTimeRequired(_arg02, _arg181, _arg238);
                    reply.writeNoException();
                    return true;
                case 115:
                    data.enforceInterface(DESCRIPTOR);
                    boolean _result67 = getAutoTimeRequired();
                    reply.writeNoException();
                    reply.writeInt(_result67 ? 1 : 0);
                    return true;
                case 116:
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        _arg0 = ComponentName.CREATOR.createFromParcel(data);
                    } else {
                        _arg0 = null;
                    }
                    int _arg182 = data.readInt();
                    boolean _result68 = isRemovingAdmin(_arg0, _arg182);
                    reply.writeNoException();
                    reply.writeInt(_result68 ? 1 : 0);
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
            public void setPasswordQuality(ComponentName who, int quality, int userHandle) throws RemoteException {
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
                    _data.writeInt(userHandle);
                    this.mRemote.transact(1, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public int getPasswordQuality(ComponentName who, int userHandle) throws RemoteException {
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
            public void setPasswordMinimumLength(ComponentName who, int length, int userHandle) throws RemoteException {
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
                    _data.writeInt(userHandle);
                    this.mRemote.transact(3, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public int getPasswordMinimumLength(ComponentName who, int userHandle) throws RemoteException {
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
            public void setPasswordMinimumUpperCase(ComponentName who, int length, int userHandle) throws RemoteException {
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
                    _data.writeInt(userHandle);
                    this.mRemote.transact(5, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public int getPasswordMinimumUpperCase(ComponentName who, int userHandle) throws RemoteException {
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
            public void setPasswordMinimumLowerCase(ComponentName who, int length, int userHandle) throws RemoteException {
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
                    _data.writeInt(userHandle);
                    this.mRemote.transact(7, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public int getPasswordMinimumLowerCase(ComponentName who, int userHandle) throws RemoteException {
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
            public void setPasswordMinimumLetters(ComponentName who, int length, int userHandle) throws RemoteException {
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
                    _data.writeInt(userHandle);
                    this.mRemote.transact(9, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public int getPasswordMinimumLetters(ComponentName who, int userHandle) throws RemoteException {
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
            public void setPasswordMinimumNumeric(ComponentName who, int length, int userHandle) throws RemoteException {
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
                    _data.writeInt(userHandle);
                    this.mRemote.transact(11, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public int getPasswordMinimumNumeric(ComponentName who, int userHandle) throws RemoteException {
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
            public void setPasswordMinimumSymbols(ComponentName who, int length, int userHandle) throws RemoteException {
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
                    _data.writeInt(userHandle);
                    this.mRemote.transact(13, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public int getPasswordMinimumSymbols(ComponentName who, int userHandle) throws RemoteException {
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
            public void setPasswordMinimumNonLetter(ComponentName who, int length, int userHandle) throws RemoteException {
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
                    _data.writeInt(userHandle);
                    this.mRemote.transact(15, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public int getPasswordMinimumNonLetter(ComponentName who, int userHandle) throws RemoteException {
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
            public void setPasswordHistoryLength(ComponentName who, int length, int userHandle) throws RemoteException {
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
                    _data.writeInt(userHandle);
                    this.mRemote.transact(17, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public int getPasswordHistoryLength(ComponentName who, int userHandle) throws RemoteException {
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
            public void setPasswordExpirationTimeout(ComponentName who, long expiration, int userHandle) throws RemoteException {
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
                    _data.writeInt(userHandle);
                    this.mRemote.transact(19, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public long getPasswordExpirationTimeout(ComponentName who, int userHandle) throws RemoteException {
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
            public long getPasswordExpiration(ComponentName who, int userHandle) throws RemoteException {
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
            public boolean isActivePasswordSufficient(int userHandle) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(userHandle);
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
            public int getCurrentFailedPasswordAttempts(int userHandle) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(userHandle);
                    this.mRemote.transact(23, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public int getProfileWithMinimumFailedPasswordsForWipe(int userHandle) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(userHandle);
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
            public void setMaximumFailedPasswordsForWipe(ComponentName admin, int num, int userHandle) throws RemoteException {
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
                    _data.writeInt(userHandle);
                    this.mRemote.transact(25, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public int getMaximumFailedPasswordsForWipe(ComponentName admin, int userHandle) throws RemoteException {
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
                    this.mRemote.transact(26, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public boolean resetPassword(String password, int flags, int userHandle) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(password);
                    _data.writeInt(flags);
                    _data.writeInt(userHandle);
                    this.mRemote.transact(27, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readInt() != 0;
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void setMaximumTimeToLock(ComponentName who, long timeMs, int userHandle) throws RemoteException {
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
                    _data.writeInt(userHandle);
                    this.mRemote.transact(28, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public long getMaximumTimeToLock(ComponentName who, int userHandle) throws RemoteException {
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
                    this.mRemote.transact(29, _data, _reply, 0);
                    _reply.readException();
                    long _result = _reply.readLong();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void lockNow() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(30, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void wipeData(int flags, int userHandle) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(flags);
                    _data.writeInt(userHandle);
                    this.mRemote.transact(31, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public ComponentName setGlobalProxy(ComponentName admin, String proxySpec, String exclusionList, int userHandle) throws RemoteException {
                ComponentName _result;
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
                    _data.writeInt(userHandle);
                    this.mRemote.transact(32, _data, _reply, 0);
                    _reply.readException();
                    if (_reply.readInt() != 0) {
                        _result = ComponentName.CREATOR.createFromParcel(_reply);
                    } else {
                        _result = null;
                    }
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public ComponentName getGlobalProxyAdmin(int userHandle) throws RemoteException {
                ComponentName _result;
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(userHandle);
                    this.mRemote.transact(33, _data, _reply, 0);
                    _reply.readException();
                    if (_reply.readInt() != 0) {
                        _result = ComponentName.CREATOR.createFromParcel(_reply);
                    } else {
                        _result = null;
                    }
                    return _result;
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
                    this.mRemote.transact(34, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public int setStorageEncryption(ComponentName who, boolean encrypt, int userHandle) throws RemoteException {
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
                    _data.writeInt(userHandle);
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
                    this.mRemote.transact(36, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readInt() != 0;
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public int getStorageEncryptionStatus(int userHandle) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(userHandle);
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
            public void setCameraDisabled(ComponentName who, boolean disabled, int userHandle) throws RemoteException {
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
                    _data.writeInt(userHandle);
                    this.mRemote.transact(38, _data, _reply, 0);
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
                    this.mRemote.transact(39, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readInt() != 0;
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void setScreenCaptureDisabled(ComponentName who, int userHandle, boolean disabled) throws RemoteException {
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
                    _data.writeInt(disabled ? 1 : 0);
                    this.mRemote.transact(40, _data, _reply, 0);
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
            public void setKeyguardDisabledFeatures(ComponentName who, int which, int userHandle) throws RemoteException {
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
                    _data.writeInt(userHandle);
                    this.mRemote.transact(42, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public int getKeyguardDisabledFeatures(ComponentName who, int userHandle) throws RemoteException {
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
                    this.mRemote.transact(43, _data, _reply, 0);
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
                    this.mRemote.transact(44, _data, _reply, 0);
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
                    this.mRemote.transact(45, _data, _reply, 0);
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
                    this.mRemote.transact(46, _data, _reply, 0);
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
                    this.mRemote.transact(47, _data, _reply, 0);
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
                    this.mRemote.transact(48, _data, _reply, 0);
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
                    this.mRemote.transact(49, _data, _reply, 0);
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
                    this.mRemote.transact(51, _data, _reply, 0);
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
                    this.mRemote.transact(52, _data, _reply, 0);
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
                    this.mRemote.transact(53, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public boolean setDeviceOwner(String packageName, String ownerName) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(packageName);
                    _data.writeString(ownerName);
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
            public boolean isDeviceOwner(String packageName) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(packageName);
                    this.mRemote.transact(55, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readInt() != 0;
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public String getDeviceOwner() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(56, _data, _reply, 0);
                    _reply.readException();
                    String _result = _reply.readString();
                    return _result;
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
                    this.mRemote.transact(57, _data, _reply, 0);
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
                    this.mRemote.transact(58, _data, _reply, 0);
                    _reply.readException();
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
                    this.mRemote.transact(59, _data, _reply, 0);
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
                ComponentName _result;
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(userHandle);
                    this.mRemote.transact(60, _data, _reply, 0);
                    _reply.readException();
                    if (_reply.readInt() != 0) {
                        _result = ComponentName.CREATOR.createFromParcel(_reply);
                    } else {
                        _result = null;
                    }
                    return _result;
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
                    this.mRemote.transact(61, _data, _reply, 0);
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
                    this.mRemote.transact(62, _data, _reply, 0);
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
                    this.mRemote.transact(63, _data, _reply, 0);
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
                    this.mRemote.transact(64, _data, _reply, 0);
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
                    this.mRemote.transact(65, _data, _reply, 0);
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
                    this.mRemote.transact(66, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readInt() != 0;
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void uninstallCaCert(ComponentName admin, String alias) throws RemoteException {
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
                    _data.writeString(alias);
                    this.mRemote.transact(67, _data, _reply, 0);
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
                    this.mRemote.transact(68, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public boolean installKeyPair(ComponentName who, byte[] privKeyBuffer, byte[] certBuffer, String alias) throws RemoteException {
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
                    _data.writeString(alias);
                    this.mRemote.transact(69, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readInt() != 0;
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
                    this.mRemote.transact(70, _data, _reply, 0);
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
                    this.mRemote.transact(71, _data, _reply, 0);
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
                    this.mRemote.transact(72, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public Bundle getApplicationRestrictions(ComponentName who, String packageName) throws RemoteException {
                Bundle _result;
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
                    this.mRemote.transact(73, _data, _reply, 0);
                    _reply.readException();
                    if (_reply.readInt() != 0) {
                        _result = Bundle.CREATOR.createFromParcel(_reply);
                    } else {
                        _result = null;
                    }
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
                    this.mRemote.transact(74, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public ComponentName getRestrictionsProvider(int userHandle) throws RemoteException {
                ComponentName _result;
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(userHandle);
                    this.mRemote.transact(75, _data, _reply, 0);
                    _reply.readException();
                    if (_reply.readInt() != 0) {
                        _result = ComponentName.CREATOR.createFromParcel(_reply);
                    } else {
                        _result = null;
                    }
                    return _result;
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
                    this.mRemote.transact(76, _data, _reply, 0);
                    _reply.readException();
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
                    this.mRemote.transact(77, _data, _reply, 0);
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
                    this.mRemote.transact(78, _data, _reply, 0);
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
                    this.mRemote.transact(80, _data, _reply, 0);
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
                    this.mRemote.transact(81, _data, _reply, 0);
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
                    this.mRemote.transact(83, _data, _reply, 0);
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
                    this.mRemote.transact(84, _data, _reply, 0);
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
                    this.mRemote.transact(86, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readInt() != 0;
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public UserHandle createUser(ComponentName who, String name) throws RemoteException {
                UserHandle _result;
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
                    this.mRemote.transact(87, _data, _reply, 0);
                    _reply.readException();
                    if (_reply.readInt() != 0) {
                        _result = UserHandle.CREATOR.createFromParcel(_reply);
                    } else {
                        _result = null;
                    }
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public UserHandle createAndInitializeUser(ComponentName who, String name, String profileOwnerName, ComponentName profileOwnerComponent, Bundle adminExtras) throws RemoteException {
                UserHandle _result;
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
                    _data.writeString(profileOwnerName);
                    if (profileOwnerComponent != null) {
                        _data.writeInt(1);
                        profileOwnerComponent.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    if (adminExtras != null) {
                        _data.writeInt(1);
                        adminExtras.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(88, _data, _reply, 0);
                    _reply.readException();
                    if (_reply.readInt() != 0) {
                        _result = UserHandle.CREATOR.createFromParcel(_reply);
                    } else {
                        _result = null;
                    }
                    return _result;
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
                    this.mRemote.transact(90, _data, _reply, 0);
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
                    this.mRemote.transact(91, _data, _reply, 0);
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
                    this.mRemote.transact(92, _data, _reply, 0);
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
                    this.mRemote.transact(93, _data, _reply, 0);
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
                    this.mRemote.transact(94, _data, _reply, 0);
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
                    this.mRemote.transact(95, _data, _reply, 0);
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
                    this.mRemote.transact(96, _data, _reply, 0);
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
                    this.mRemote.transact(97, _data, _reply, 0);
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
                    this.mRemote.transact(98, _data, _reply, 0);
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
                    this.mRemote.transact(99, _data, _reply, 0);
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
                    this.mRemote.transact(100, _data, _reply, 0);
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
                    this.mRemote.transact(101, _data, _reply, 0);
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
            public void notifyLockTaskModeChanged(boolean isEnabled, String pkg, int userId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(isEnabled ? 1 : 0);
                    _data.writeString(pkg);
                    _data.writeInt(userId);
                    this.mRemote.transact(103, _data, _reply, 0);
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
                    this.mRemote.transact(104, _data, _reply, 0);
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
                    this.mRemote.transact(105, _data, _reply, 0);
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
                    this.mRemote.transact(106, _data, _reply, 0);
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
            public boolean getCrossProfileCallerIdDisabledForUser(int userId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(userId);
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
            public void setTrustAgentConfiguration(ComponentName admin, ComponentName agent, PersistableBundle args, int userId) throws RemoteException {
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
                    _data.writeInt(userId);
                    this.mRemote.transact(109, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public List<PersistableBundle> getTrustAgentConfiguration(ComponentName admin, ComponentName agent, int userId) throws RemoteException {
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
                    this.mRemote.transact(110, _data, _reply, 0);
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
                    this.mRemote.transact(113, _data, _reply, 0);
                    _reply.readException();
                    List<String> _result = _reply.createStringArrayList();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void setAutoTimeRequired(ComponentName who, int userHandle, boolean required) throws RemoteException {
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
                    _data.writeInt(required ? 1 : 0);
                    this.mRemote.transact(114, _data, _reply, 0);
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
                    this.mRemote.transact(116, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readInt() != 0;
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }
        }
    }
}
