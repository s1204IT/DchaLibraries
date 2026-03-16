package com.android.server.telecom;

import android.app.AppOpsManager;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.UserHandle;
import android.os.UserManager;
import android.telecom.PhoneAccount;
import android.telecom.PhoneAccountHandle;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.EventLog;
import com.android.internal.telecom.ITelecomService;
import com.android.internal.util.IndentingPrintWriter;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

public class TelecomService extends Service {
    private static final String TAG = TelecomService.class.getSimpleName();
    private AppOpsManager mAppOpsManager;
    private CallsManager mCallsManager;
    private Context mContext;
    private final MainThreadHandler mMainThreadHandler = new MainThreadHandler();
    private MissedCallNotifier mMissedCallNotifier;
    private PackageManager mPackageManager;
    private PhoneAccountRegistrar mPhoneAccountRegistrar;
    private TelecomServiceImpl mServiceImpl;
    private UserManager mUserManager;

    private static final class MainThreadRequest {
        public Object arg;
        public Object result;

        private MainThreadRequest() {
        }
    }

    private final class MainThreadHandler extends Handler {
        private MainThreadHandler() {
        }

        @Override
        public void handleMessage(Message message) {
            Object objValueOf;
            if (message.obj instanceof MainThreadRequest) {
                MainThreadRequest mainThreadRequest = (MainThreadRequest) message.obj;
                switch (message.what) {
                    case 1:
                        TelecomService.this.mCallsManager.getRinger().silence();
                        objValueOf = null;
                        break;
                    case 2:
                        TelecomService.this.mCallsManager.getInCallController().bringToForeground(message.arg1 == 1);
                        objValueOf = null;
                        break;
                    case 3:
                        objValueOf = Boolean.valueOf(TelecomService.this.endCallInternal());
                        break;
                    case 4:
                        TelecomService.this.acceptRingingCallInternal();
                        objValueOf = null;
                        break;
                    case 5:
                        TelecomService.this.mMissedCallNotifier.clearMissedCalls();
                        objValueOf = null;
                        break;
                    case 6:
                        objValueOf = Boolean.valueOf(TelecomService.this.mCallsManager.isTtySupported());
                        break;
                    case 7:
                        objValueOf = Integer.valueOf(TelecomService.this.mCallsManager.getCurrentTtyMode());
                        break;
                    case 8:
                        if (mainThreadRequest.arg == null || !(mainThreadRequest.arg instanceof Intent)) {
                            Log.w(this, "Invalid new incoming call request", new Object[0]);
                            objValueOf = null;
                            break;
                        } else {
                            CallReceiver.processIncomingCallIntent((Intent) mainThreadRequest.arg);
                            break;
                        }
                    default:
                        objValueOf = null;
                        break;
                }
                if (objValueOf != null) {
                    mainThreadRequest.result = objValueOf;
                    synchronized (mainThreadRequest) {
                        mainThreadRequest.notifyAll();
                    }
                }
            }
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(this, "onCreate", new Object[0]);
        this.mContext = this;
        this.mAppOpsManager = (AppOpsManager) this.mContext.getSystemService("appops");
        this.mServiceImpl = new TelecomServiceImpl();
        TelecomGlobals telecomGlobals = TelecomGlobals.getInstance();
        telecomGlobals.initialize(this);
        this.mMissedCallNotifier = telecomGlobals.getMissedCallNotifier();
        this.mPhoneAccountRegistrar = telecomGlobals.getPhoneAccountRegistrar();
        this.mCallsManager = telecomGlobals.getCallsManager();
        this.mUserManager = (UserManager) this.mContext.getSystemService("user");
        this.mPackageManager = this.mContext.getPackageManager();
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.d(this, "onBind", new Object[0]);
        return this.mServiceImpl;
    }

    class TelecomServiceImpl extends ITelecomService.Stub {
        TelecomServiceImpl() {
        }

        public boolean isVideoEnabled() {
            long jClearCallingIdentity = Binder.clearCallingIdentity();
            try {
                return TelecomService.this.getTelephonyManager().isVideoEnabled();
            } finally {
                Binder.restoreCallingIdentity(jClearCallingIdentity);
            }
        }

        public PhoneAccountHandle getDefaultOutgoingPhoneAccount(String str) {
            TelecomService.this.enforceReadPermission();
            long jClearCallingIdentity = Binder.clearCallingIdentity();
            try {
                try {
                    PhoneAccountHandle defaultOutgoingPhoneAccount = TelecomService.this.mPhoneAccountRegistrar.getDefaultOutgoingPhoneAccount(str);
                    if (defaultOutgoingPhoneAccount == null || TelecomService.this.isVisibleToCaller(defaultOutgoingPhoneAccount)) {
                        return defaultOutgoingPhoneAccount;
                    }
                    Log.w(this, "No account found for the calling user", new Object[0]);
                    return null;
                } catch (Exception e) {
                    Log.e(this, e, "getDefaultOutgoingPhoneAccount", new Object[0]);
                    throw e;
                }
            } finally {
                Binder.restoreCallingIdentity(jClearCallingIdentity);
            }
            Binder.restoreCallingIdentity(jClearCallingIdentity);
        }

        public PhoneAccountHandle getUserSelectedOutgoingPhoneAccount() throws Exception {
            try {
                PhoneAccountHandle userSelectedOutgoingPhoneAccount = TelecomService.this.mPhoneAccountRegistrar.getUserSelectedOutgoingPhoneAccount();
                if (!TelecomService.this.isVisibleToCaller(userSelectedOutgoingPhoneAccount)) {
                    Log.w(this, "No account found for the calling user", new Object[0]);
                    return null;
                }
                return userSelectedOutgoingPhoneAccount;
            } catch (Exception e) {
                Log.e(this, e, "getUserSelectedOutgoingPhoneAccount", new Object[0]);
                throw e;
            }
        }

        public void setUserSelectedOutgoingPhoneAccount(PhoneAccountHandle phoneAccountHandle) throws Exception {
            TelecomService.this.enforceModifyPermission();
            try {
                TelecomService.this.mPhoneAccountRegistrar.setUserSelectedOutgoingPhoneAccount(phoneAccountHandle);
            } catch (Exception e) {
                Log.e(this, e, "setUserSelectedOutgoingPhoneAccount", new Object[0]);
                throw e;
            }
        }

        public List<PhoneAccountHandle> getCallCapablePhoneAccounts() {
            TelecomService.this.enforceReadPermission();
            long jClearCallingIdentity = Binder.clearCallingIdentity();
            try {
                try {
                    return TelecomService.this.filterForAccountsVisibleToCaller(TelecomService.this.mPhoneAccountRegistrar.getCallCapablePhoneAccounts());
                } catch (Exception e) {
                    Log.e(this, e, "getCallCapablePhoneAccounts", new Object[0]);
                    throw e;
                }
            } finally {
                Binder.restoreCallingIdentity(jClearCallingIdentity);
            }
        }

        public List<PhoneAccountHandle> getPhoneAccountsSupportingScheme(String str) {
            TelecomService.this.enforceReadPermission();
            long jClearCallingIdentity = Binder.clearCallingIdentity();
            try {
                try {
                    return TelecomService.this.filterForAccountsVisibleToCaller(TelecomService.this.mPhoneAccountRegistrar.getCallCapablePhoneAccounts(str));
                } catch (Exception e) {
                    Log.e(this, e, "getPhoneAccountsSupportingScheme %s", str);
                    throw e;
                }
            } finally {
                Binder.restoreCallingIdentity(jClearCallingIdentity);
            }
        }

        public List<PhoneAccountHandle> getPhoneAccountsForPackage(String str) throws Exception {
            try {
                return TelecomService.this.filterForAccountsVisibleToCaller(TelecomService.this.mPhoneAccountRegistrar.getPhoneAccountsForPackage(str));
            } catch (Exception e) {
                Log.e(this, e, "getPhoneAccountsForPackage %s", str);
                throw e;
            }
        }

        public PhoneAccount getPhoneAccount(PhoneAccountHandle phoneAccountHandle) throws Exception {
            try {
                if (TelecomService.this.isVisibleToCaller(phoneAccountHandle)) {
                    return TelecomService.this.mPhoneAccountRegistrar.getPhoneAccountInternal(phoneAccountHandle);
                }
                Log.w(this, "%s is not visible for the calling user", phoneAccountHandle);
                return null;
            } catch (Exception e) {
                Log.e(this, e, "getPhoneAccount %s", phoneAccountHandle);
                throw e;
            }
        }

        public int getAllPhoneAccountsCount() throws Exception {
            try {
                return getAllPhoneAccounts().size();
            } catch (Exception e) {
                Log.e(this, e, "getAllPhoneAccountsCount", new Object[0]);
                throw e;
            }
        }

        public List<PhoneAccount> getAllPhoneAccounts() throws Exception {
            try {
                ArrayList<PhoneAccount> arrayList = new ArrayList(TelecomService.this.mPhoneAccountRegistrar.getAllPhoneAccounts().size());
                for (PhoneAccount phoneAccount : arrayList) {
                    if (TelecomService.this.isVisibleToCaller(phoneAccount)) {
                        arrayList.add(phoneAccount);
                    }
                }
                return arrayList;
            } catch (Exception e) {
                Log.e(this, e, "getAllPhoneAccounts", new Object[0]);
                throw e;
            }
        }

        public List<PhoneAccountHandle> getAllPhoneAccountHandles() throws Exception {
            try {
                return TelecomService.this.filterForAccountsVisibleToCaller(TelecomService.this.mPhoneAccountRegistrar.getAllPhoneAccountHandles());
            } catch (Exception e) {
                Log.e(this, e, "getAllPhoneAccounts", new Object[0]);
                throw e;
            }
        }

        public PhoneAccountHandle getSimCallManager() throws Exception {
            try {
                PhoneAccountHandle simCallManager = TelecomService.this.mPhoneAccountRegistrar.getSimCallManager();
                if (!TelecomService.this.isVisibleToCaller(simCallManager)) {
                    Log.w(this, "%s is not visible for the calling user", simCallManager);
                    return null;
                }
                return simCallManager;
            } catch (Exception e) {
                Log.e(this, e, "getSimCallManager", new Object[0]);
                throw e;
            }
        }

        public void setSimCallManager(PhoneAccountHandle phoneAccountHandle) throws Exception {
            TelecomService.this.enforceModifyPermission();
            try {
                TelecomService.this.mPhoneAccountRegistrar.setSimCallManager(phoneAccountHandle);
            } catch (Exception e) {
                Log.e(this, e, "setSimCallManager", new Object[0]);
                throw e;
            }
        }

        public List<PhoneAccountHandle> getSimCallManagers() {
            TelecomService.this.enforceReadPermission();
            long jClearCallingIdentity = Binder.clearCallingIdentity();
            try {
                try {
                    return TelecomService.this.filterForAccountsVisibleToCaller(TelecomService.this.mPhoneAccountRegistrar.getConnectionManagerPhoneAccounts());
                } catch (Exception e) {
                    Log.e(this, e, "getSimCallManagers", new Object[0]);
                    throw e;
                }
            } finally {
                Binder.restoreCallingIdentity(jClearCallingIdentity);
            }
        }

        public void registerPhoneAccount(PhoneAccount phoneAccount) throws Exception {
            try {
                TelecomService.this.enforcePhoneAccountModificationForPackage(phoneAccount.getAccountHandle().getComponentName().getPackageName());
                if (phoneAccount.hasCapabilities(2)) {
                    TelecomService.this.enforceRegisterCallProviderPermission();
                }
                if (phoneAccount.hasCapabilities(4)) {
                    TelecomService.this.enforceRegisterSimSubscriptionPermission();
                }
                if (phoneAccount.hasCapabilities(1)) {
                    TelecomService.this.enforceRegisterConnectionManagerPermission();
                }
                if (phoneAccount.hasCapabilities(32)) {
                    TelecomService.this.enforceRegisterMultiUser();
                }
                TelecomService.this.enforceUserHandleMatchesCaller(phoneAccount.getAccountHandle());
                TelecomService.this.mPhoneAccountRegistrar.registerPhoneAccount(phoneAccount);
            } catch (Exception e) {
                Log.e(this, e, "registerPhoneAccount %s", phoneAccount);
                throw e;
            }
        }

        public void unregisterPhoneAccount(PhoneAccountHandle phoneAccountHandle) throws Exception {
            try {
                TelecomService.this.enforcePhoneAccountModificationForPackage(phoneAccountHandle.getComponentName().getPackageName());
                TelecomService.this.enforceUserHandleMatchesCaller(phoneAccountHandle);
                TelecomService.this.mPhoneAccountRegistrar.unregisterPhoneAccount(phoneAccountHandle);
            } catch (Exception e) {
                Log.e(this, e, "unregisterPhoneAccount %s", phoneAccountHandle);
                throw e;
            }
        }

        public void clearAccounts(String str) throws Exception {
            try {
                TelecomService.this.enforcePhoneAccountModificationForPackage(str);
                TelecomService.this.mPhoneAccountRegistrar.clearAccounts(str, Binder.getCallingUserHandle());
            } catch (Exception e) {
                Log.e(this, e, "clearAccounts %s", str);
                throw e;
            }
        }

        public boolean isVoiceMailNumber(PhoneAccountHandle phoneAccountHandle, String str) throws Exception {
            TelecomService.this.enforceReadPermissionOrDefaultDialer();
            try {
                if (TelecomService.this.isVisibleToCaller(phoneAccountHandle)) {
                    return TelecomService.this.mPhoneAccountRegistrar.isVoiceMailNumber(phoneAccountHandle, str);
                }
                Log.w(this, "%s is not visible for the calling user", phoneAccountHandle);
                return false;
            } catch (Exception e) {
                Log.e(this, e, "getSubscriptionIdForPhoneAccount", new Object[0]);
                throw e;
            }
        }

        public boolean hasVoiceMailNumber(PhoneAccountHandle phoneAccountHandle) throws Exception {
            TelecomService.this.enforceReadPermissionOrDefaultDialer();
            try {
                if (TelecomService.this.isVisibleToCaller(phoneAccountHandle)) {
                    return TextUtils.isEmpty(TelecomService.this.getTelephonyManager().getVoiceMailNumber(TelecomService.this.mPhoneAccountRegistrar.getSubscriptionIdForPhoneAccount(phoneAccountHandle))) ? false : true;
                }
                Log.w(this, "%s is not visible for the calling user", phoneAccountHandle);
                return false;
            } catch (Exception e) {
                Log.e(this, e, "getSubscriptionIdForPhoneAccount", new Object[0]);
                throw e;
            }
        }

        public String getLine1Number(PhoneAccountHandle phoneAccountHandle) throws Exception {
            TelecomService.this.enforceReadPermissionOrDefaultDialer();
            try {
                if (TelecomService.this.isVisibleToCaller(phoneAccountHandle)) {
                    return TelecomService.this.getTelephonyManager().getLine1NumberForSubscriber(TelecomService.this.mPhoneAccountRegistrar.getSubscriptionIdForPhoneAccount(phoneAccountHandle));
                }
                Log.w(this, "%s is not visible for the calling user", phoneAccountHandle);
                return null;
            } catch (Exception e) {
                Log.e(this, e, "getSubscriptionIdForPhoneAccount", new Object[0]);
                throw e;
            }
        }

        public void silenceRinger() {
            Log.d(this, "silenceRinger", new Object[0]);
            TelecomService.this.enforceModifyPermission();
            TelecomService.this.sendRequestAsync(1, 0);
        }

        public ComponentName getDefaultPhoneApp() {
            Resources resources = TelecomService.this.mContext.getResources();
            return new ComponentName(resources.getString(R.string.ui_default_package), resources.getString(R.string.dialer_default_class));
        }

        public boolean isInCall() {
            TelecomService.this.enforceReadPermission();
            int callState = TelecomService.this.mCallsManager.getCallState();
            return callState == 2 || callState == 1;
        }

        public boolean isRinging() {
            TelecomService.this.enforceReadPermission();
            return TelecomService.this.mCallsManager.getCallState() == 1;
        }

        public int getCallState() {
            return TelecomService.this.mCallsManager.getCallState();
        }

        public boolean endCall() {
            TelecomService.this.enforceModifyPermission();
            return ((Boolean) TelecomService.this.sendRequest(3)).booleanValue();
        }

        public void acceptRingingCall() {
            TelecomService.this.enforceModifyPermission();
            TelecomService.this.sendRequestAsync(4, 0);
        }

        public void showInCallScreen(boolean z) {
            TelecomService.this.enforceReadPermissionOrDefaultDialer();
            TelecomService.this.sendRequestAsync(2, z ? 1 : 0);
        }

        public void cancelMissedCallsNotification() {
            TelecomService.this.enforceModifyPermissionOrDefaultDialer();
            TelecomService.this.sendRequestAsync(5, 0);
        }

        public boolean handlePinMmi(String str) {
            TelecomService.this.enforceModifyPermissionOrDefaultDialer();
            long jClearCallingIdentity = Binder.clearCallingIdentity();
            try {
                return TelecomService.this.getTelephonyManager().handlePinMmi(str);
            } finally {
                Binder.restoreCallingIdentity(jClearCallingIdentity);
            }
        }

        public boolean handlePinMmiForPhoneAccount(PhoneAccountHandle phoneAccountHandle, String str) {
            boolean zHandlePinMmiForSubscriber = false;
            TelecomService.this.enforceModifyPermissionOrDefaultDialer();
            if (!TelecomService.this.isVisibleToCaller(phoneAccountHandle)) {
                Log.w(this, "%s is not visible for the calling user", phoneAccountHandle);
            } else {
                long jClearCallingIdentity = Binder.clearCallingIdentity();
                try {
                    zHandlePinMmiForSubscriber = TelecomService.this.getTelephonyManager().handlePinMmiForSubscriber(TelecomService.this.mPhoneAccountRegistrar.getSubscriptionIdForPhoneAccount(phoneAccountHandle), str);
                } finally {
                    Binder.restoreCallingIdentity(jClearCallingIdentity);
                }
            }
            return zHandlePinMmiForSubscriber;
        }

        public Uri getAdnUriForPhoneAccount(PhoneAccountHandle phoneAccountHandle) {
            TelecomService.this.enforceModifyPermissionOrDefaultDialer();
            if (!TelecomService.this.isVisibleToCaller(phoneAccountHandle)) {
                Log.w(this, "%s is not visible for the calling user", phoneAccountHandle);
                return null;
            }
            long jClearCallingIdentity = Binder.clearCallingIdentity();
            try {
                String str = "content://icc/adn/subId/" + TelecomService.this.mPhoneAccountRegistrar.getSubscriptionIdForPhoneAccount(phoneAccountHandle);
                Binder.restoreCallingIdentity(jClearCallingIdentity);
                return Uri.parse(str);
            } catch (Throwable th) {
                Binder.restoreCallingIdentity(jClearCallingIdentity);
                throw th;
            }
        }

        public boolean isTtySupported() {
            TelecomService.this.enforceReadPermission();
            return ((Boolean) TelecomService.this.sendRequest(6)).booleanValue();
        }

        public int getCurrentTtyMode() {
            TelecomService.this.enforceReadPermission();
            return ((Integer) TelecomService.this.sendRequest(7)).intValue();
        }

        public void addNewIncomingCall(PhoneAccountHandle phoneAccountHandle, Bundle bundle) {
            Log.i(this, "Adding new incoming call with phoneAccountHandle %s", phoneAccountHandle);
            if (phoneAccountHandle != null && phoneAccountHandle.getComponentName() != null) {
                TelecomService.this.mAppOpsManager.checkPackage(Binder.getCallingUid(), phoneAccountHandle.getComponentName().getPackageName());
                TelecomService.this.enforcePhoneAccountIsRegistered(phoneAccountHandle);
                TelecomService.this.enforceUserHandleMatchesCaller(phoneAccountHandle);
                Intent intent = new Intent("android.telecom.action.INCOMING_CALL");
                intent.putExtra("android.telecom.extra.PHONE_ACCOUNT_HANDLE", phoneAccountHandle);
                intent.putExtra("is_incoming_call", true);
                if (bundle != null) {
                    intent.putExtra("android.telecom.extra.INCOMING_CALL_EXTRAS", bundle);
                }
                TelecomService.this.sendRequestAsync(8, 0, intent);
                return;
            }
            Log.w(this, "Null phoneAccountHandle. Ignoring request to add new incoming call", new Object[0]);
        }

        public void addNewUnknownCall(PhoneAccountHandle phoneAccountHandle, Bundle bundle) {
            if (phoneAccountHandle != null && phoneAccountHandle.getComponentName() != null && TelephonyUtil.isPstnComponentName(phoneAccountHandle.getComponentName())) {
                TelecomService.this.mAppOpsManager.checkPackage(Binder.getCallingUid(), phoneAccountHandle.getComponentName().getPackageName());
                TelecomService.this.enforcePhoneAccountIsRegistered(phoneAccountHandle);
                TelecomService.this.enforceUserHandleMatchesCaller(phoneAccountHandle);
                Intent intent = new Intent("android.telecom.action.NEW_UNKNOWN_CALL");
                intent.setClass(TelecomService.this.mContext, CallReceiver.class);
                intent.setFlags(268435456);
                intent.putExtras(bundle);
                intent.putExtra("is_unknown_call", true);
                intent.putExtra("android.telecom.extra.PHONE_ACCOUNT_HANDLE", phoneAccountHandle);
                TelecomService.this.mContext.sendBroadcastAsUser(intent, phoneAccountHandle.getUserHandle());
                return;
            }
            Log.i(this, "Null phoneAccountHandle or not initiated by Telephony. Ignoring request to add new unknown call.", new Object[0]);
        }

        protected void dump(FileDescriptor fileDescriptor, PrintWriter printWriter, String[] strArr) {
            if (TelecomService.this.mContext.checkCallingOrSelfPermission("android.permission.DUMP") != 0) {
                printWriter.println("Permission Denial: can't dump TelecomService from from pid=" + Binder.getCallingPid() + ", uid=" + Binder.getCallingUid());
                return;
            }
            IndentingPrintWriter indentingPrintWriter = new IndentingPrintWriter(printWriter, "  ");
            if (TelecomService.this.mCallsManager != null) {
                indentingPrintWriter.println("mCallsManager: ");
                indentingPrintWriter.increaseIndent();
                TelecomService.this.mCallsManager.dump(indentingPrintWriter);
                indentingPrintWriter.decreaseIndent();
                indentingPrintWriter.println("mPhoneAccountRegistrar: ");
                indentingPrintWriter.increaseIndent();
                TelecomService.this.mPhoneAccountRegistrar.dump(indentingPrintWriter);
                indentingPrintWriter.decreaseIndent();
            }
        }
    }

    private boolean isVisibleToCaller(PhoneAccountHandle phoneAccountHandle) {
        if (phoneAccountHandle == null) {
            return false;
        }
        return isVisibleToCaller(this.mPhoneAccountRegistrar.getPhoneAccountInternal(phoneAccountHandle));
    }

    private boolean isVisibleToCaller(PhoneAccount phoneAccount) {
        List<UserHandle> arrayList;
        if (phoneAccount == null) {
            return false;
        }
        if (phoneAccount.hasCapabilities(32)) {
            return true;
        }
        UserHandle userHandle = phoneAccount.getAccountHandle().getUserHandle();
        if (userHandle == null) {
            return false;
        }
        if (isCallerSystemApp()) {
            arrayList = this.mUserManager.getUserProfiles();
        } else {
            arrayList = new ArrayList<>(1);
            arrayList.add(Binder.getCallingUserHandle());
        }
        return arrayList.contains(userHandle);
    }

    private List<PhoneAccountHandle> filterForAccountsVisibleToCaller(List<PhoneAccountHandle> list) {
        ArrayList arrayList = new ArrayList(list.size());
        for (PhoneAccountHandle phoneAccountHandle : list) {
            if (isVisibleToCaller(phoneAccountHandle)) {
                arrayList.add(phoneAccountHandle);
            }
        }
        return arrayList;
    }

    private boolean isCallerSystemApp() {
        for (String str : this.mPackageManager.getPackagesForUid(Binder.getCallingUid())) {
            if (isPackageSystemApp(str)) {
                return true;
            }
        }
        return false;
    }

    private boolean isPackageSystemApp(String str) {
        if ((this.mPackageManager.getApplicationInfo(str, 128).flags & 1) != 0) {
            return true;
        }
        return false;
    }

    private void acceptRingingCallInternal() {
        Call firstCallWithState = this.mCallsManager.getFirstCallWithState(4);
        if (firstCallWithState != null) {
            firstCallWithState.answer(firstCallWithState.getVideoState());
        }
    }

    private boolean endCallInternal() {
        Call foregroundCall = this.mCallsManager.getForegroundCall();
        if (foregroundCall == null) {
            foregroundCall = this.mCallsManager.getFirstCallWithState(5, 3, 4, 6);
        }
        if (foregroundCall == null) {
            return false;
        }
        if (foregroundCall.getState() == 4) {
            foregroundCall.reject(false, null);
        } else {
            foregroundCall.disconnect();
        }
        return true;
    }

    private void enforcePhoneAccountIsRegistered(PhoneAccountHandle phoneAccountHandle) {
        if (this.mPhoneAccountRegistrar.getPhoneAccount(phoneAccountHandle) == null) {
            EventLog.writeEvent(1397638484, "26864502", Integer.valueOf(Binder.getCallingUid()), "R");
            throw new SecurityException("This PhoneAccountHandle is not registered to a valid PhoneAccount!");
        }
    }

    private void enforcePhoneAccountModificationForPackage(String str) {
        if (this.mContext.checkCallingOrSelfPermission("android.permission.MODIFY_PHONE_STATE") != 0) {
            enforceConnectionServiceFeature();
            enforceCallingPackage(str);
        }
    }

    private void enforceReadPermissionOrDefaultDialer() {
        if (!isDefaultDialerCalling()) {
            enforceReadPermission();
        }
    }

    private void enforceModifyPermissionOrDefaultDialer() {
        if (!isDefaultDialerCalling()) {
            enforceModifyPermission();
        }
    }

    private void enforceCallingPackage(String str) {
        this.mAppOpsManager.checkPackage(Binder.getCallingUid(), str);
    }

    private void enforceConnectionServiceFeature() {
        enforceFeature("android.software.connectionservice");
    }

    private void enforceRegisterCallProviderPermission() {
        enforcePermission("android.permission.REGISTER_CALL_PROVIDER");
    }

    private void enforceRegisterSimSubscriptionPermission() {
        enforcePermission("android.permission.REGISTER_SIM_SUBSCRIPTION");
    }

    private void enforceRegisterConnectionManagerPermission() {
        enforcePermission("android.permission.REGISTER_CONNECTION_MANAGER");
    }

    private void enforceReadPermission() {
        enforcePermission("android.permission.READ_PHONE_STATE");
    }

    private void enforceModifyPermission() {
        enforcePermission("android.permission.MODIFY_PHONE_STATE");
    }

    private void enforcePermission(String str) {
        this.mContext.enforceCallingOrSelfPermission(str, null);
    }

    private void enforceRegisterMultiUser() {
        if (!isCallerSystemApp()) {
            throw new SecurityException("CAPABILITY_MULTI_USER is only available to system apps.");
        }
    }

    private void enforceUserHandleMatchesCaller(PhoneAccountHandle phoneAccountHandle) {
        if (!Binder.getCallingUserHandle().equals(phoneAccountHandle.getUserHandle())) {
            throw new SecurityException("Calling UserHandle does not match PhoneAccountHandle's");
        }
    }

    private void enforceFeature(String str) {
        if (!this.mContext.getPackageManager().hasSystemFeature(str)) {
            throw new UnsupportedOperationException("System does not support feature " + str);
        }
    }

    private boolean isDefaultDialerCalling() {
        ComponentName defaultPhoneAppInternal = getDefaultPhoneAppInternal();
        if (defaultPhoneAppInternal == null) {
            return false;
        }
        try {
            this.mAppOpsManager.checkPackage(Binder.getCallingUid(), defaultPhoneAppInternal.getPackageName());
            return true;
        } catch (SecurityException e) {
            Log.e(TAG, (Throwable) e, "Could not get default dialer.", new Object[0]);
            return false;
        }
    }

    private ComponentName getDefaultPhoneAppInternal() {
        Resources resources = this.mContext.getResources();
        return new ComponentName(resources.getString(R.string.ui_default_package), resources.getString(R.string.dialer_default_class));
    }

    private TelephonyManager getTelephonyManager() {
        return (TelephonyManager) this.mContext.getSystemService("phone");
    }

    private MainThreadRequest sendRequestAsync(int i, int i2) {
        return sendRequestAsync(i, i2, null);
    }

    private MainThreadRequest sendRequestAsync(int i, int i2, Object obj) {
        MainThreadRequest mainThreadRequest = new MainThreadRequest();
        mainThreadRequest.arg = obj;
        this.mMainThreadHandler.obtainMessage(i, i2, 0, mainThreadRequest).sendToTarget();
        return mainThreadRequest;
    }

    private Object sendRequest(int i) {
        if (Looper.myLooper() == this.mMainThreadHandler.getLooper()) {
            MainThreadRequest mainThreadRequest = new MainThreadRequest();
            this.mMainThreadHandler.handleMessage(this.mMainThreadHandler.obtainMessage(i, mainThreadRequest));
            return mainThreadRequest.result;
        }
        MainThreadRequest mainThreadRequestSendRequestAsync = sendRequestAsync(i, 0);
        synchronized (mainThreadRequestSendRequestAsync) {
            while (mainThreadRequestSendRequestAsync.result == null) {
                try {
                    mainThreadRequestSendRequestAsync.wait();
                } catch (InterruptedException e) {
                }
            }
        }
        return mainThreadRequestSendRequestAsync.result;
    }
}
