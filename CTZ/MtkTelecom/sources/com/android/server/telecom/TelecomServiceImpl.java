package com.android.server.telecom;

import android.app.ActivityManager;
import android.app.AppOpsManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Binder;
import android.os.Bundle;
import android.os.UserHandle;
import android.telecom.Log;
import android.telecom.PhoneAccount;
import android.telecom.PhoneAccountHandle;
import android.telecom.TelecomAnalytics;
import android.telecom.VideoProfile;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.util.EventLog;
import com.android.internal.telecom.ITelecomService;
import com.android.internal.util.IndentingPrintWriter;
import com.android.server.telecom.CallIntentProcessor;
import com.android.server.telecom.TelecomSystem;
import com.android.server.telecom.components.UserCallIntentProcessor;
import com.android.server.telecom.components.UserCallIntentProcessorFactory;
import com.android.server.telecom.settings.BlockedNumbersActivity;
import com.mediatek.server.telecom.MtkUtil;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.Collections;
import java.util.List;

public class TelecomServiceImpl {
    private AppOpsManager mAppOpsManager;
    private final ITelecomService.Stub mBinderImpl = new ITelecomService.Stub() {
        public PhoneAccountHandle getDefaultOutgoingPhoneAccount(String str, String str2) {
            try {
                Log.startSession("TSI.gDOPA");
                synchronized (TelecomServiceImpl.this.mLock) {
                    if (!TelecomServiceImpl.this.canReadPhoneState(str2, "getDefaultOutgoingPhoneAccount")) {
                        return null;
                    }
                    UserHandle callingUserHandle = Binder.getCallingUserHandle();
                    long jClearCallingIdentity = Binder.clearCallingIdentity();
                    try {
                        try {
                            return TelecomServiceImpl.this.mPhoneAccountRegistrar.getOutgoingPhoneAccountForScheme(str, callingUserHandle);
                        } catch (Exception e) {
                            Log.e(this, e, "getDefaultOutgoingPhoneAccount", new Object[0]);
                            throw e;
                        }
                    } finally {
                        Binder.restoreCallingIdentity(jClearCallingIdentity);
                    }
                }
            } finally {
                Log.endSession();
            }
        }

        public PhoneAccountHandle getUserSelectedOutgoingPhoneAccount() {
            PhoneAccountHandle userSelectedOutgoingPhoneAccount;
            synchronized (TelecomServiceImpl.this.mLock) {
                try {
                    try {
                        Log.startSession("TSI.gUSOPA");
                        userSelectedOutgoingPhoneAccount = TelecomServiceImpl.this.mPhoneAccountRegistrar.getUserSelectedOutgoingPhoneAccount(Binder.getCallingUserHandle());
                    } catch (Exception e) {
                        Log.e(this, e, "getUserSelectedOutgoingPhoneAccount", new Object[0]);
                        throw e;
                    }
                } finally {
                    Log.endSession();
                }
            }
            return userSelectedOutgoingPhoneAccount;
        }

        public void setUserSelectedOutgoingPhoneAccount(PhoneAccountHandle phoneAccountHandle) {
            try {
                Log.startSession("TSI.sUSOPA");
                synchronized (TelecomServiceImpl.this.mLock) {
                    TelecomServiceImpl.this.enforceModifyPermission();
                    UserHandle callingUserHandle = Binder.getCallingUserHandle();
                    long jClearCallingIdentity = Binder.clearCallingIdentity();
                    try {
                        try {
                            TelecomServiceImpl.this.mPhoneAccountRegistrar.setUserSelectedOutgoingPhoneAccount(phoneAccountHandle, callingUserHandle);
                        } catch (Exception e) {
                            Log.e(this, e, "setUserSelectedOutgoingPhoneAccount", new Object[0]);
                            throw e;
                        }
                    } finally {
                        Binder.restoreCallingIdentity(jClearCallingIdentity);
                    }
                }
            } finally {
                Log.endSession();
            }
        }

        public List<PhoneAccountHandle> getCallCapablePhoneAccounts(boolean z, String str) {
            List<PhoneAccountHandle> callCapablePhoneAccounts;
            if (TelecomServiceImpl.this.canReadPhoneState(str, "getDefaultOutgoingPhoneAccount")) {
                synchronized (TelecomServiceImpl.this.mLock) {
                    UserHandle callingUserHandle = Binder.getCallingUserHandle();
                    long jClearCallingIdentity = Binder.clearCallingIdentity();
                    try {
                        try {
                            callCapablePhoneAccounts = TelecomServiceImpl.this.mPhoneAccountRegistrar.getCallCapablePhoneAccounts(null, z, callingUserHandle);
                        } catch (Exception e) {
                            Log.e(this, e, "getCallCapablePhoneAccounts", new Object[0]);
                            throw e;
                        }
                    } finally {
                        Binder.restoreCallingIdentity(jClearCallingIdentity);
                    }
                }
                return callCapablePhoneAccounts;
            }
            return Collections.emptyList();
        }

        public List<PhoneAccountHandle> getSelfManagedPhoneAccounts(String str) {
            List<PhoneAccountHandle> selfManagedPhoneAccounts;
            try {
                Log.startSession("TSI.gSMPA");
                if (TelecomServiceImpl.this.canReadPhoneState(str, "Requires READ_PHONE_STATE permission.")) {
                    synchronized (TelecomServiceImpl.this.mLock) {
                        UserHandle callingUserHandle = Binder.getCallingUserHandle();
                        long jClearCallingIdentity = Binder.clearCallingIdentity();
                        try {
                            try {
                                selfManagedPhoneAccounts = TelecomServiceImpl.this.mPhoneAccountRegistrar.getSelfManagedPhoneAccounts(callingUserHandle);
                            } finally {
                                Binder.restoreCallingIdentity(jClearCallingIdentity);
                            }
                        } catch (Exception e) {
                            Log.e(this, e, "getSelfManagedPhoneAccounts", new Object[0]);
                            throw e;
                        }
                    }
                    return selfManagedPhoneAccounts;
                }
                throw new SecurityException("Requires READ_PHONE_STATE permission.");
            } finally {
                Log.endSession();
            }
        }

        public List<PhoneAccountHandle> getPhoneAccountsSupportingScheme(String str, String str2) {
            List<PhoneAccountHandle> callCapablePhoneAccounts;
            try {
                Log.startSession("TSI.gPASS");
                TelecomServiceImpl.this.enforceModifyPermission("getPhoneAccountsSupportingScheme requires MODIFY_PHONE_STATE");
                synchronized (TelecomServiceImpl.this.mLock) {
                    UserHandle callingUserHandle = Binder.getCallingUserHandle();
                    long jClearCallingIdentity = Binder.clearCallingIdentity();
                    try {
                        try {
                            callCapablePhoneAccounts = TelecomServiceImpl.this.mPhoneAccountRegistrar.getCallCapablePhoneAccounts(str, false, callingUserHandle);
                        } finally {
                            Binder.restoreCallingIdentity(jClearCallingIdentity);
                        }
                    } catch (Exception e) {
                        Log.e(this, e, "getPhoneAccountsSupportingScheme %s", new Object[]{str});
                        throw e;
                    }
                }
                return callCapablePhoneAccounts;
            } catch (SecurityException e2) {
                EventLog.writeEvent(1397638484, "62347125", Integer.valueOf(Binder.getCallingUid()), "getPhoneAccountsSupportingScheme: " + str2);
                return Collections.emptyList();
            } finally {
                Log.endSession();
            }
        }

        public List<PhoneAccountHandle> getPhoneAccountsForPackage(String str) {
            List<PhoneAccountHandle> phoneAccountsForPackage;
            try {
                TelecomServiceImpl.this.enforceCallingPackage(str);
                try {
                    TelecomServiceImpl.this.enforcePermission("android.permission.READ_PRIVILEGED_PHONE_STATE");
                    synchronized (TelecomServiceImpl.this.mLock) {
                        UserHandle callingUserHandle = Binder.getCallingUserHandle();
                        long jClearCallingIdentity = Binder.clearCallingIdentity();
                        try {
                            try {
                                Log.startSession("TSI.gPAFP");
                                phoneAccountsForPackage = TelecomServiceImpl.this.mPhoneAccountRegistrar.getPhoneAccountsForPackage(str, callingUserHandle);
                            } catch (Exception e) {
                                Log.e(this, e, "getPhoneAccountsForPackage %s", new Object[]{str});
                                throw e;
                            }
                        } finally {
                            Binder.restoreCallingIdentity(jClearCallingIdentity);
                            Log.endSession();
                        }
                    }
                    return phoneAccountsForPackage;
                } catch (SecurityException e2) {
                    EventLog.writeEvent(1397638484, "153995334", Integer.valueOf(Binder.getCallingUid()), "getPhoneAccountsForPackage: no permission");
                    throw e2;
                }
            } catch (SecurityException e3) {
                EventLog.writeEvent(1397638484, "153995334", Integer.valueOf(Binder.getCallingUid()), "getPhoneAccountsForPackage: invalid calling package");
                throw e3;
            }
        }

        public PhoneAccount getPhoneAccount(PhoneAccountHandle phoneAccountHandle) {
            PhoneAccount phoneAccount;
            synchronized (TelecomServiceImpl.this.mLock) {
                UserHandle callingUserHandle = Binder.getCallingUserHandle();
                long jClearCallingIdentity = Binder.clearCallingIdentity();
                try {
                    try {
                        phoneAccount = TelecomServiceImpl.this.mPhoneAccountRegistrar.getPhoneAccount(phoneAccountHandle, callingUserHandle, true);
                    } catch (Exception e) {
                        Log.e(this, e, "getPhoneAccount %s", new Object[]{phoneAccountHandle});
                        throw e;
                    }
                } finally {
                    Binder.restoreCallingIdentity(jClearCallingIdentity);
                }
            }
            return phoneAccount;
        }

        public int getAllPhoneAccountsCount() {
            int size;
            try {
                Log.startSession("TSI.gAPAC");
                try {
                    TelecomServiceImpl.this.enforceModifyPermission("getAllPhoneAccountsCount requires MODIFY_PHONE_STATE permission.");
                    synchronized (TelecomServiceImpl.this.mLock) {
                        try {
                            size = getAllPhoneAccounts().size();
                        } catch (Exception e) {
                            Log.e(this, e, "getAllPhoneAccountsCount", new Object[0]);
                            throw e;
                        }
                    }
                    return size;
                } catch (SecurityException e2) {
                    EventLog.writeEvent(1397638484, "62347125", Integer.valueOf(Binder.getCallingUid()), "getAllPhoneAccountsCount");
                    throw e2;
                }
            } finally {
                Log.endSession();
            }
        }

        public List<PhoneAccount> getAllPhoneAccounts() {
            List<PhoneAccount> allPhoneAccounts;
            synchronized (TelecomServiceImpl.this.mLock) {
                try {
                    Log.startSession("TSI.gAPA");
                    try {
                        TelecomServiceImpl.this.enforceModifyPermission("getAllPhoneAccounts requires MODIFY_PHONE_STATE permission.");
                        UserHandle callingUserHandle = Binder.getCallingUserHandle();
                        long jClearCallingIdentity = Binder.clearCallingIdentity();
                        try {
                            try {
                                allPhoneAccounts = TelecomServiceImpl.this.mPhoneAccountRegistrar.getAllPhoneAccounts(callingUserHandle);
                            } catch (Exception e) {
                                Log.e(this, e, "getAllPhoneAccounts", new Object[0]);
                                throw e;
                            }
                        } finally {
                            Binder.restoreCallingIdentity(jClearCallingIdentity);
                        }
                    } catch (SecurityException e2) {
                        EventLog.writeEvent(1397638484, "62347125", Integer.valueOf(Binder.getCallingUid()), "getAllPhoneAccounts");
                        throw e2;
                    }
                } finally {
                    Log.endSession();
                }
            }
            return allPhoneAccounts;
        }

        public List<PhoneAccountHandle> getAllPhoneAccountHandles() {
            List<PhoneAccountHandle> allPhoneAccountHandles;
            try {
                Log.startSession("TSI.gAPAH");
                try {
                    TelecomServiceImpl.this.enforceModifyPermission("getAllPhoneAccountHandles requires MODIFY_PHONE_STATE permission.");
                    synchronized (TelecomServiceImpl.this.mLock) {
                        UserHandle callingUserHandle = Binder.getCallingUserHandle();
                        long jClearCallingIdentity = Binder.clearCallingIdentity();
                        try {
                            try {
                                allPhoneAccountHandles = TelecomServiceImpl.this.mPhoneAccountRegistrar.getAllPhoneAccountHandles(callingUserHandle);
                            } catch (Exception e) {
                                Log.e(this, e, "getAllPhoneAccounts", new Object[0]);
                                throw e;
                            }
                        } finally {
                            Binder.restoreCallingIdentity(jClearCallingIdentity);
                        }
                    }
                    return allPhoneAccountHandles;
                } catch (SecurityException e2) {
                    EventLog.writeEvent(1397638484, "62347125", Integer.valueOf(Binder.getCallingUid()), "getAllPhoneAccountHandles");
                    throw e2;
                }
            } finally {
                Log.endSession();
            }
        }

        public PhoneAccountHandle getSimCallManager() {
            try {
                Log.startSession("TSI.gSCM");
                long jClearCallingIdentity = Binder.clearCallingIdentity();
                try {
                    return getSimCallManagerForUser(ActivityManager.getCurrentUser());
                } finally {
                    Binder.restoreCallingIdentity(jClearCallingIdentity);
                }
            } finally {
                Log.endSession();
            }
        }

        public PhoneAccountHandle getSimCallManagerForUser(int i) {
            PhoneAccountHandle simCallManager;
            synchronized (TelecomServiceImpl.this.mLock) {
                try {
                    try {
                        Log.startSession("TSI.gSCMFU");
                        int callingUid = Binder.getCallingUid();
                        long jClearCallingIdentity = Binder.clearCallingIdentity();
                        try {
                            if (i != ActivityManager.getCurrentUser()) {
                                TelecomServiceImpl.this.enforceCrossUserPermission(callingUid);
                            }
                            simCallManager = TelecomServiceImpl.this.mPhoneAccountRegistrar.getSimCallManager(UserHandle.of(i));
                        } finally {
                            Binder.restoreCallingIdentity(jClearCallingIdentity);
                        }
                    } catch (Exception e) {
                        Log.e(this, e, "getSimCallManager", new Object[0]);
                        throw e;
                    }
                } finally {
                    Log.endSession();
                }
            }
            return simCallManager;
        }

        public void registerPhoneAccount(PhoneAccount phoneAccount) {
            try {
                Log.startSession("TSI.rPA");
                synchronized (TelecomServiceImpl.this.mLock) {
                    if (TelecomServiceImpl.this.mContext.getApplicationContext().getResources().getBoolean(android.R.^attr-private.popupPromptView)) {
                        try {
                            TelecomServiceImpl.this.enforcePhoneAccountModificationForPackage(phoneAccount.getAccountHandle().getComponentName().getPackageName());
                            if (phoneAccount.hasCapabilities(2048)) {
                                TelecomServiceImpl.this.enforceRegisterSelfManaged();
                                if (phoneAccount.hasCapabilities(2) || phoneAccount.hasCapabilities(1) || phoneAccount.hasCapabilities(4)) {
                                    throw new SecurityException("Self-managed ConnectionServices cannot also be call capable, connection managers, or SIM accounts.");
                                }
                            }
                            if (phoneAccount.hasCapabilities(4)) {
                                TelecomServiceImpl.this.enforceRegisterSimSubscriptionPermission();
                            }
                            if (phoneAccount.hasCapabilities(32)) {
                                TelecomServiceImpl.this.enforceRegisterMultiUser();
                            }
                            TelecomServiceImpl.this.enforceUserHandleMatchesCaller(phoneAccount.getAccountHandle());
                            long jClearCallingIdentity = Binder.clearCallingIdentity();
                            try {
                                TelecomServiceImpl.this.mPhoneAccountRegistrar.registerPhoneAccount(phoneAccount);
                                return;
                            } finally {
                                Binder.restoreCallingIdentity(jClearCallingIdentity);
                            }
                        } catch (Exception e) {
                            Log.e(this, e, "registerPhoneAccount %s", new Object[]{phoneAccount});
                            throw e;
                        }
                    }
                    Log.w(this, "registerPhoneAccount not allowed on non-voice capable device.", new Object[0]);
                }
            } finally {
                Log.endSession();
            }
        }

        public void unregisterPhoneAccount(PhoneAccountHandle phoneAccountHandle) {
            synchronized (TelecomServiceImpl.this.mLock) {
                try {
                    try {
                        Log.startSession("TSI.uPA");
                        TelecomServiceImpl.this.enforcePhoneAccountModificationForPackage(phoneAccountHandle.getComponentName().getPackageName());
                        TelecomServiceImpl.this.enforceUserHandleMatchesCaller(phoneAccountHandle);
                        long jClearCallingIdentity = Binder.clearCallingIdentity();
                        try {
                            TelecomServiceImpl.this.mPhoneAccountRegistrar.unregisterPhoneAccount(phoneAccountHandle);
                        } finally {
                            Binder.restoreCallingIdentity(jClearCallingIdentity);
                        }
                    } catch (Exception e) {
                        Log.e(this, e, "unregisterPhoneAccount %s", new Object[]{phoneAccountHandle});
                        throw e;
                    }
                } finally {
                    Log.endSession();
                }
            }
        }

        public void clearAccounts(String str) {
            synchronized (TelecomServiceImpl.this.mLock) {
                try {
                    try {
                        Log.startSession("TSI.cA");
                        TelecomServiceImpl.this.enforcePhoneAccountModificationForPackage(str);
                        TelecomServiceImpl.this.mPhoneAccountRegistrar.clearAccounts(str, Binder.getCallingUserHandle());
                    } catch (Exception e) {
                        Log.e(this, e, "clearAccounts %s", new Object[]{str});
                        throw e;
                    }
                } finally {
                    Log.endSession();
                }
            }
        }

        public boolean isVoiceMailNumber(PhoneAccountHandle phoneAccountHandle, String str, String str2) {
            try {
                Log.startSession("TSI.iVMN");
                synchronized (TelecomServiceImpl.this.mLock) {
                    if (!TelecomServiceImpl.this.canReadPhoneState(str2, "isVoiceMailNumber")) {
                        return false;
                    }
                    if (!TelecomServiceImpl.this.isPhoneAccountHandleVisibleToCallingUser(phoneAccountHandle, Binder.getCallingUserHandle())) {
                        Log.d(this, "%s is not visible for the calling user [iVMN]", new Object[]{phoneAccountHandle});
                        return false;
                    }
                    long jClearCallingIdentity = Binder.clearCallingIdentity();
                    try {
                        try {
                            return TelecomServiceImpl.this.mPhoneAccountRegistrar.isVoiceMailNumber(phoneAccountHandle, str);
                        } catch (Exception e) {
                            Log.e(this, e, "getSubscriptionIdForPhoneAccount", new Object[0]);
                            throw e;
                        }
                    } finally {
                        Binder.restoreCallingIdentity(jClearCallingIdentity);
                    }
                }
            } finally {
                Log.endSession();
            }
        }

        public String getVoiceMailNumber(PhoneAccountHandle phoneAccountHandle, String str) {
            try {
                Log.startSession("TSI.gVMN");
                synchronized (TelecomServiceImpl.this.mLock) {
                    if (!TelecomServiceImpl.this.canReadPhoneState(str, "getVoiceMailNumber")) {
                        return null;
                    }
                    try {
                        if (!TelecomServiceImpl.this.isPhoneAccountHandleVisibleToCallingUser(phoneAccountHandle, Binder.getCallingUserHandle())) {
                            Log.d(this, "%s is not visible for the calling user [gVMN]", new Object[]{phoneAccountHandle});
                            return null;
                        }
                        int defaultVoiceSubId = TelecomServiceImpl.this.mSubscriptionManagerAdapter.getDefaultVoiceSubId();
                        if (phoneAccountHandle != null) {
                            defaultVoiceSubId = TelecomServiceImpl.this.mPhoneAccountRegistrar.getSubscriptionIdForPhoneAccount(phoneAccountHandle);
                        }
                        return TelecomServiceImpl.this.getTelephonyManager().getVoiceMailNumber(defaultVoiceSubId);
                    } catch (Exception e) {
                        Log.e(this, e, "getSubscriptionIdForPhoneAccount", new Object[0]);
                        throw e;
                    }
                }
            } finally {
                Log.endSession();
            }
        }

        public String getLine1Number(PhoneAccountHandle phoneAccountHandle, String str) {
            try {
                Log.startSession("getL1N");
                if (!TelecomServiceImpl.this.canReadPhoneState(str, "getLine1Number")) {
                    return null;
                }
                synchronized (TelecomServiceImpl.this.mLock) {
                    if (!TelecomServiceImpl.this.isPhoneAccountHandleVisibleToCallingUser(phoneAccountHandle, Binder.getCallingUserHandle())) {
                        Log.d(this, "%s is not visible for the calling user [gL1N]", new Object[]{phoneAccountHandle});
                        return null;
                    }
                    long jClearCallingIdentity = Binder.clearCallingIdentity();
                    try {
                        try {
                            return TelecomServiceImpl.this.getTelephonyManager().getLine1Number(TelecomServiceImpl.this.mPhoneAccountRegistrar.getSubscriptionIdForPhoneAccount(phoneAccountHandle));
                        } catch (Exception e) {
                            Log.e(this, e, "getSubscriptionIdForPhoneAccount", new Object[0]);
                            throw e;
                        }
                    } finally {
                        Binder.restoreCallingIdentity(jClearCallingIdentity);
                    }
                }
            } finally {
                Log.endSession();
            }
        }

        public void silenceRinger(String str) {
            try {
                Log.startSession("TSI.sR");
                synchronized (TelecomServiceImpl.this.mLock) {
                    TelecomServiceImpl.this.enforcePermissionOrPrivilegedDialer("android.permission.MODIFY_PHONE_STATE", str);
                    long jClearCallingIdentity = Binder.clearCallingIdentity();
                    try {
                        Log.i(this, "Silence Ringer requested by %s", new Object[]{str});
                        TelecomServiceImpl.this.mCallsManager.getCallAudioManager().silenceRingers();
                        TelecomServiceImpl.this.mCallsManager.getInCallController().silenceRinger();
                    } finally {
                        Binder.restoreCallingIdentity(jClearCallingIdentity);
                    }
                }
            } finally {
                Log.endSession();
            }
        }

        public ComponentName getDefaultPhoneApp() {
            try {
                Log.startSession("TSI.gDPA");
                Resources resources = TelecomServiceImpl.this.mContext.getResources();
                return new ComponentName(resources.getString(R.string.ui_default_package), resources.getString(R.string.dialer_default_class));
            } finally {
                Log.endSession();
            }
        }

        public String getDefaultDialerPackage() {
            try {
                Log.startSession("TSI.gDDP");
                long jClearCallingIdentity = Binder.clearCallingIdentity();
                try {
                    return TelecomServiceImpl.this.mDefaultDialerCache.getDefaultDialerApplication(ActivityManager.getCurrentUser());
                } finally {
                    Binder.restoreCallingIdentity(jClearCallingIdentity);
                }
            } finally {
                Log.endSession();
            }
        }

        public String getSystemDialerPackage() {
            try {
                Log.startSession("TSI.gSDP");
                return TelecomServiceImpl.this.mContext.getResources().getString(R.string.ui_default_package);
            } finally {
                Log.endSession();
            }
        }

        public boolean isInCall(String str) {
            boolean zHasOngoingCalls;
            try {
                Log.startSession("TSI.iIC");
                if (TelecomServiceImpl.this.canReadPhoneState(str, "isInCall")) {
                    synchronized (TelecomServiceImpl.this.mLock) {
                        zHasOngoingCalls = TelecomServiceImpl.this.mCallsManager.hasOngoingCalls();
                    }
                    return zHasOngoingCalls;
                }
                return false;
            } finally {
                Log.endSession();
            }
        }

        public boolean isInManagedCall(String str) {
            boolean zHasOngoingManagedCalls;
            try {
                Log.startSession("TSI.iIMC");
                if (TelecomServiceImpl.this.canReadPhoneState(str, "isInManagedCall")) {
                    synchronized (TelecomServiceImpl.this.mLock) {
                        zHasOngoingManagedCalls = TelecomServiceImpl.this.mCallsManager.hasOngoingManagedCalls();
                    }
                    return zHasOngoingManagedCalls;
                }
                throw new SecurityException("Only the default dialer or caller with READ_PHONE_STATE permission can use this method.");
            } finally {
                Log.endSession();
            }
        }

        public boolean isRinging(String str) {
            boolean zHasRingingCall;
            try {
                Log.startSession("TSI.iR");
                if (!TelecomServiceImpl.this.isPrivilegedDialerCalling(str)) {
                    try {
                        TelecomServiceImpl.this.enforceModifyPermission("isRinging requires MODIFY_PHONE_STATE permission.");
                    } catch (SecurityException e) {
                        EventLog.writeEvent(1397638484, "62347125", "isRinging: " + str);
                        throw e;
                    }
                }
                synchronized (TelecomServiceImpl.this.mLock) {
                    zHasRingingCall = TelecomServiceImpl.this.mCallsManager.hasRingingCall();
                }
                return zHasRingingCall;
            } finally {
                Log.endSession();
            }
        }

        public int getCallState() {
            int callState;
            try {
                Log.startSession("TSI.getCallState");
                synchronized (TelecomServiceImpl.this.mLock) {
                    callState = TelecomServiceImpl.this.mCallsManager.getCallState();
                }
                return callState;
            } finally {
                Log.endSession();
            }
        }

        public boolean endCall(String str) {
            boolean zEndCallInternal;
            try {
                Log.startSession("TSI.eC");
                synchronized (TelecomServiceImpl.this.mLock) {
                    if (!TelecomServiceImpl.this.enforceAnswerCallPermission(str, Binder.getCallingUid())) {
                        throw new SecurityException("requires ANSWER_PHONE_CALLS permission");
                    }
                    long jClearCallingIdentity = Binder.clearCallingIdentity();
                    try {
                        zEndCallInternal = TelecomServiceImpl.this.endCallInternal(str);
                    } finally {
                        Binder.restoreCallingIdentity(jClearCallingIdentity);
                    }
                }
                return zEndCallInternal;
            } finally {
                Log.endSession();
            }
        }

        public void acceptRingingCall(String str) {
            try {
                Log.startSession("TSI.aRC");
                synchronized (TelecomServiceImpl.this.mLock) {
                    if (!TelecomServiceImpl.this.enforceAnswerCallPermission(str, Binder.getCallingUid())) {
                        return;
                    }
                    long jClearCallingIdentity = Binder.clearCallingIdentity();
                    try {
                        TelecomServiceImpl.this.acceptRingingCallInternal(-1);
                    } finally {
                        Binder.restoreCallingIdentity(jClearCallingIdentity);
                    }
                }
            } finally {
                Log.endSession();
            }
        }

        public void acceptRingingCallWithVideoState(String str, int i) {
            try {
                Log.startSession("TSI.aRCWVS");
                synchronized (TelecomServiceImpl.this.mLock) {
                    if (!TelecomServiceImpl.this.enforceAnswerCallPermission(str, Binder.getCallingUid())) {
                        return;
                    }
                    long jClearCallingIdentity = Binder.clearCallingIdentity();
                    try {
                        TelecomServiceImpl.this.acceptRingingCallInternal(i);
                    } finally {
                        Binder.restoreCallingIdentity(jClearCallingIdentity);
                    }
                }
            } finally {
                Log.endSession();
            }
        }

        public void showInCallScreen(boolean z, String str) {
            try {
                Log.startSession("TSI.sICS");
                if (TelecomServiceImpl.this.canReadPhoneState(str, "showInCallScreen")) {
                    synchronized (TelecomServiceImpl.this.mLock) {
                        long jClearCallingIdentity = Binder.clearCallingIdentity();
                        try {
                            TelecomServiceImpl.this.mCallsManager.getInCallController().bringToForeground(z);
                        } finally {
                            Binder.restoreCallingIdentity(jClearCallingIdentity);
                        }
                    }
                }
            } finally {
                Log.endSession();
            }
        }

        public void cancelMissedCallsNotification(String str) {
            try {
                Log.startSession("TSI.cMCN");
                synchronized (TelecomServiceImpl.this.mLock) {
                    TelecomServiceImpl.this.enforcePermissionOrPrivilegedDialer("android.permission.MODIFY_PHONE_STATE", str);
                    UserHandle callingUserHandle = Binder.getCallingUserHandle();
                    long jClearCallingIdentity = Binder.clearCallingIdentity();
                    try {
                        TelecomServiceImpl.this.mCallsManager.getMissedCallNotifier().clearMissedCalls(callingUserHandle);
                    } finally {
                        Binder.restoreCallingIdentity(jClearCallingIdentity);
                    }
                }
            } finally {
                Log.endSession();
            }
        }

        public boolean handlePinMmi(String str, String str2) {
            try {
                Log.startSession("TSI.hPM");
                synchronized (TelecomServiceImpl.this.mLock) {
                    TelecomServiceImpl.this.enforcePermissionOrPrivilegedDialer("android.permission.MODIFY_PHONE_STATE", str2);
                }
                long jClearCallingIdentity = Binder.clearCallingIdentity();
                try {
                    return TelecomServiceImpl.this.getTelephonyManager().handlePinMmi(str);
                } finally {
                    Binder.restoreCallingIdentity(jClearCallingIdentity);
                }
            } finally {
                Log.endSession();
            }
        }

        public boolean handlePinMmiForPhoneAccount(PhoneAccountHandle phoneAccountHandle, String str, String str2) {
            int subscriptionIdForPhoneAccount;
            try {
                Log.startSession("TSI.hPMFPA");
                synchronized (TelecomServiceImpl.this.mLock) {
                    TelecomServiceImpl.this.enforcePermissionOrPrivilegedDialer("android.permission.MODIFY_PHONE_STATE", str2);
                    if (!TelecomServiceImpl.this.isPhoneAccountHandleVisibleToCallingUser(phoneAccountHandle, Binder.getCallingUserHandle())) {
                        Log.d(this, "%s is not visible for the calling user [hMMI]", new Object[]{phoneAccountHandle});
                        return false;
                    }
                    long jClearCallingIdentity = Binder.clearCallingIdentity();
                    try {
                        synchronized (TelecomServiceImpl.this.mLock) {
                            subscriptionIdForPhoneAccount = TelecomServiceImpl.this.mPhoneAccountRegistrar.getSubscriptionIdForPhoneAccount(phoneAccountHandle);
                        }
                        return TelecomServiceImpl.this.getTelephonyManager().handlePinMmiForSubscriber(subscriptionIdForPhoneAccount, str);
                    } finally {
                        Binder.restoreCallingIdentity(jClearCallingIdentity);
                    }
                }
            } finally {
                Log.endSession();
            }
        }

        public Uri getAdnUriForPhoneAccount(PhoneAccountHandle phoneAccountHandle, String str) {
            try {
                Log.startSession("TSI.aAUFPA");
                synchronized (TelecomServiceImpl.this.mLock) {
                    TelecomServiceImpl.this.enforcePermissionOrPrivilegedDialer("android.permission.MODIFY_PHONE_STATE", str);
                    if (!TelecomServiceImpl.this.isPhoneAccountHandleVisibleToCallingUser(phoneAccountHandle, Binder.getCallingUserHandle())) {
                        Log.d(this, "%s is not visible for the calling user [gA4PA]", new Object[]{phoneAccountHandle});
                        return null;
                    }
                    long jClearCallingIdentity = Binder.clearCallingIdentity();
                    try {
                        String str2 = "content://icc/adn/subId/" + TelecomServiceImpl.this.mPhoneAccountRegistrar.getSubscriptionIdForPhoneAccount(phoneAccountHandle);
                        Binder.restoreCallingIdentity(jClearCallingIdentity);
                        return Uri.parse(str2);
                    } catch (Throwable th) {
                        Binder.restoreCallingIdentity(jClearCallingIdentity);
                        throw th;
                    }
                }
            } finally {
                Log.endSession();
            }
        }

        public boolean isTtySupported(String str) {
            boolean zIsTtySupported;
            try {
                Log.startSession("TSI.iTS");
                if (TelecomServiceImpl.this.canReadPhoneState(str, "isTtySupported")) {
                    synchronized (TelecomServiceImpl.this.mLock) {
                        zIsTtySupported = TelecomServiceImpl.this.mCallsManager.isTtySupported();
                    }
                    return zIsTtySupported;
                }
                throw new SecurityException("Only default dialer or an app withREAD_PRIVILEGED_PHONE_STATE or READ_PHONE_STATE can call this api");
            } finally {
                Log.endSession();
            }
        }

        public int getCurrentTtyMode(String str) {
            int currentTtyMode;
            try {
                Log.startSession("TSI.gCTM");
                if (TelecomServiceImpl.this.canReadPhoneState(str, "getCurrentTtyMode")) {
                    synchronized (TelecomServiceImpl.this.mLock) {
                        currentTtyMode = TelecomServiceImpl.this.mCallsManager.getCurrentTtyMode();
                    }
                    return currentTtyMode;
                }
                return 0;
            } finally {
                Log.endSession();
            }
        }

        public void addNewIncomingCall(PhoneAccountHandle phoneAccountHandle, Bundle bundle) {
            try {
                Log.startSession("TSI.aNIC");
                synchronized (TelecomServiceImpl.this.mLock) {
                    Log.i(this, "Adding new incoming call with phoneAccountHandle %s", new Object[]{phoneAccountHandle});
                    if (phoneAccountHandle != null && phoneAccountHandle.getComponentName() != null) {
                        if (TelecomServiceImpl.this.isCallerSimCallManager() && TelephonyUtil.isPstnComponentName(phoneAccountHandle.getComponentName())) {
                            Log.v(this, "Allowing call manager to add incoming call with PSTN handle", new Object[0]);
                        } else {
                            TelecomServiceImpl.this.mAppOpsManager.checkPackage(Binder.getCallingUid(), phoneAccountHandle.getComponentName().getPackageName());
                            TelecomServiceImpl.this.enforceUserHandleMatchesCaller(phoneAccountHandle);
                            TelecomServiceImpl.this.enforcePhoneAccountIsRegisteredEnabled(phoneAccountHandle, Binder.getCallingUserHandle());
                            if (TelecomServiceImpl.this.isSelfManagedConnectionService(phoneAccountHandle)) {
                                TelecomServiceImpl.this.mContext.enforceCallingOrSelfPermission("android.permission.MANAGE_OWN_CALLS", "Self-managed phone accounts must have MANAGE_OWN_CALLS permission.");
                            }
                        }
                        long jClearCallingIdentity = Binder.clearCallingIdentity();
                        try {
                            Intent intent = new Intent("android.telecom.action.INCOMING_CALL");
                            intent.putExtra("android.telecom.extra.PHONE_ACCOUNT_HANDLE", phoneAccountHandle);
                            intent.putExtra("is_incoming_call", true);
                            if (bundle != null) {
                                bundle.setDefusable(true);
                                intent.putExtra("android.telecom.extra.INCOMING_CALL_EXTRAS", bundle);
                            }
                            TelecomServiceImpl.this.mCallIntentProcessorAdapter.processIncomingCallIntent(TelecomServiceImpl.this.mCallsManager, intent);
                            Binder.restoreCallingIdentity(jClearCallingIdentity);
                        } catch (Throwable th) {
                            Binder.restoreCallingIdentity(jClearCallingIdentity);
                            throw th;
                        }
                    } else {
                        Log.w(this, "Null phoneAccountHandle. Ignoring request to add new incoming call", new Object[0]);
                    }
                }
            } finally {
                Log.endSession();
            }
        }

        public void acceptHandover(Uri uri, int i, PhoneAccountHandle phoneAccountHandle) {
            try {
                Log.startSession("TSI.aHO");
                synchronized (TelecomServiceImpl.this.mLock) {
                    Log.i(this, "acceptHandover; srcAddr=%s, videoState=%s, dest=%s", new Object[]{Log.pii(uri), VideoProfile.videoStateToString(i), phoneAccountHandle});
                    if (phoneAccountHandle != null && phoneAccountHandle.getComponentName() != null) {
                        TelecomServiceImpl.this.mAppOpsManager.checkPackage(Binder.getCallingUid(), phoneAccountHandle.getComponentName().getPackageName());
                        TelecomServiceImpl.this.enforceUserHandleMatchesCaller(phoneAccountHandle);
                        TelecomServiceImpl.this.enforcePhoneAccountIsRegisteredEnabled(phoneAccountHandle, Binder.getCallingUserHandle());
                        if (TelecomServiceImpl.this.isSelfManagedConnectionService(phoneAccountHandle)) {
                            TelecomServiceImpl.this.mContext.enforceCallingOrSelfPermission("android.permission.MANAGE_OWN_CALLS", "Self-managed phone accounts must have MANAGE_OWN_CALLS permission.");
                        }
                        if (!TelecomServiceImpl.this.enforceAcceptHandoverPermission(phoneAccountHandle.getComponentName().getPackageName(), Binder.getCallingUid())) {
                            throw new SecurityException("App must be granted runtime ACCEPT_HANDOVER permission.");
                        }
                        long jClearCallingIdentity = Binder.clearCallingIdentity();
                        try {
                            TelecomServiceImpl.this.mCallsManager.acceptHandover(uri, i, phoneAccountHandle);
                            Binder.restoreCallingIdentity(jClearCallingIdentity);
                        } catch (Throwable th) {
                            Binder.restoreCallingIdentity(jClearCallingIdentity);
                            throw th;
                        }
                    } else {
                        Log.w(this, "Null phoneAccountHandle. Ignoring request to handover the call", new Object[0]);
                    }
                }
            } finally {
                Log.endSession();
            }
        }

        public void addNewUnknownCall(PhoneAccountHandle phoneAccountHandle, Bundle bundle) {
            try {
                Log.startSession("TSI.aNUC");
                try {
                    TelecomServiceImpl.this.enforceModifyPermission("addNewUnknownCall requires MODIFY_PHONE_STATE permission.");
                    synchronized (TelecomServiceImpl.this.mLock) {
                        if (phoneAccountHandle != null) {
                            try {
                                if (phoneAccountHandle.getComponentName() != null) {
                                    TelecomServiceImpl.this.mAppOpsManager.checkPackage(Binder.getCallingUid(), phoneAccountHandle.getComponentName().getPackageName());
                                    TelecomServiceImpl.this.enforceUserHandleMatchesCaller(phoneAccountHandle);
                                    TelecomServiceImpl.this.enforcePhoneAccountIsRegisteredEnabled(phoneAccountHandle, Binder.getCallingUserHandle());
                                    long jClearCallingIdentity = Binder.clearCallingIdentity();
                                    try {
                                        Intent intent = new Intent("android.telecom.action.NEW_UNKNOWN_CALL");
                                        if (bundle != null) {
                                            bundle.setDefusable(true);
                                            intent.putExtras(bundle);
                                        }
                                        intent.putExtra("is_unknown_call", true);
                                        intent.putExtra("android.telecom.extra.PHONE_ACCOUNT_HANDLE", phoneAccountHandle);
                                        TelecomServiceImpl.this.mCallIntentProcessorAdapter.processUnknownCallIntent(TelecomServiceImpl.this.mCallsManager, intent);
                                        Binder.restoreCallingIdentity(jClearCallingIdentity);
                                    } catch (Throwable th) {
                                        Binder.restoreCallingIdentity(jClearCallingIdentity);
                                        throw th;
                                    }
                                } else {
                                    Log.i(this, "Null phoneAccountHandle or not initiated by Telephony. Ignoring request to add new unknown call.", new Object[0]);
                                }
                            } catch (Throwable th2) {
                                throw th2;
                            }
                        }
                    }
                } catch (SecurityException e) {
                    EventLog.writeEvent(1397638484, "62347125", Integer.valueOf(Binder.getCallingUid()), "addNewUnknownCall");
                    throw e;
                }
            } finally {
                Log.endSession();
            }
        }

        public void placeCall(Uri uri, Bundle bundle, String str) {
            try {
                Log.startSession("TSI.pC");
                TelecomServiceImpl.this.enforceCallingPackage(str);
                PhoneAccountHandle phoneAccountHandle = null;
                if (bundle != null) {
                    phoneAccountHandle = (PhoneAccountHandle) bundle.getParcelable("android.telecom.extra.PHONE_ACCOUNT_HANDLE");
                    if (bundle.containsKey("android.telecom.extra.IS_HANDOVER")) {
                        bundle.remove("android.telecom.extra.IS_HANDOVER");
                    }
                }
                boolean z = false;
                boolean z2 = phoneAccountHandle != null && TelecomServiceImpl.this.isSelfManagedConnectionService(phoneAccountHandle);
                if (z2) {
                    TelecomServiceImpl.this.mContext.enforceCallingOrSelfPermission("android.permission.MANAGE_OWN_CALLS", "Self-managed ConnectionServices require MANAGE_OWN_CALLS permission.");
                    if (!str.equals(phoneAccountHandle.getComponentName().getPackageName()) && !TelecomServiceImpl.this.canCallPhone(str, "CALL_PHONE permission required to place calls.")) {
                        throw new SecurityException("Self-managed ConnectionServices can only place calls through their own ConnectionService.");
                    }
                } else if (!TelecomServiceImpl.this.canCallPhone(str, "placeCall")) {
                    throw new SecurityException("Package " + str + " is not allowed to place phone calls");
                }
                boolean z3 = TelecomServiceImpl.this.mAppOpsManager.noteOp(13, Binder.getCallingUid(), str) == 0;
                boolean z4 = TelecomServiceImpl.this.mContext.checkCallingPermission("android.permission.CALL_PHONE") == 0;
                boolean z5 = TelecomServiceImpl.this.mContext.checkCallingPermission("android.permission.CALL_PRIVILEGED") == 0;
                if (!MtkUtil.isConferenceInvitation(bundle) || MtkUtil.canConference(str, "placeCall")) {
                    synchronized (TelecomServiceImpl.this.mLock) {
                        UserHandle callingUserHandle = Binder.getCallingUserHandle();
                        long jClearCallingIdentity = Binder.clearCallingIdentity();
                        try {
                            Intent intent = new Intent(z5 ? "android.intent.action.CALL_PRIVILEGED" : "android.intent.action.CALL", uri);
                            if (bundle != null) {
                                bundle.setDefusable(true);
                                intent.putExtras(bundle);
                            }
                            UserCallIntentProcessor userCallIntentProcessorCreate = TelecomServiceImpl.this.mUserCallIntentProcessorFactory.create(TelecomServiceImpl.this.mContext, callingUserHandle);
                            if (z2 || (z3 && z4)) {
                                z = true;
                            }
                            userCallIntentProcessorCreate.processIntent(intent, str, z, true);
                        } finally {
                            Binder.restoreCallingIdentity(jClearCallingIdentity);
                        }
                    }
                }
            } finally {
                Log.endSession();
            }
        }

        public boolean enablePhoneAccount(PhoneAccountHandle phoneAccountHandle, boolean z) {
            boolean zEnablePhoneAccount;
            try {
                Log.startSession("TSI.ePA");
                TelecomServiceImpl.this.enforceModifyPermission();
                synchronized (TelecomServiceImpl.this.mLock) {
                    long jClearCallingIdentity = Binder.clearCallingIdentity();
                    try {
                        zEnablePhoneAccount = TelecomServiceImpl.this.mPhoneAccountRegistrar.enablePhoneAccount(phoneAccountHandle, z);
                    } finally {
                        Binder.restoreCallingIdentity(jClearCallingIdentity);
                    }
                }
                return zEnablePhoneAccount;
            } finally {
                Log.endSession();
            }
        }

        public boolean setDefaultDialer(String str) {
            boolean defaultDialer;
            try {
                Log.startSession("TSI.sDD");
                TelecomServiceImpl.this.enforcePermission("android.permission.MODIFY_PHONE_STATE");
                TelecomServiceImpl.this.enforcePermission("android.permission.WRITE_SECURE_SETTINGS");
                synchronized (TelecomServiceImpl.this.mLock) {
                    long jClearCallingIdentity = Binder.clearCallingIdentity();
                    try {
                        defaultDialer = TelecomServiceImpl.this.mDefaultDialerCache.setDefaultDialer(str, ActivityManager.getCurrentUser());
                        if (defaultDialer) {
                            Intent intent = new Intent("android.telecom.action.DEFAULT_DIALER_CHANGED");
                            intent.putExtra("android.telecom.extra.CHANGE_DEFAULT_DIALER_PACKAGE_NAME", str);
                            TelecomServiceImpl.this.mContext.sendBroadcastAsUser(intent, new UserHandle(ActivityManager.getCurrentUser()));
                        }
                    } finally {
                        Binder.restoreCallingIdentity(jClearCallingIdentity);
                    }
                }
                return defaultDialer;
            } finally {
                Log.endSession();
            }
        }

        public TelecomAnalytics dumpCallAnalytics() {
            try {
                Log.startSession("TSI.dCA");
                TelecomServiceImpl.this.enforcePermission("android.permission.DUMP");
                return Analytics.dumpToParcelableAnalytics();
            } finally {
                Log.endSession();
            }
        }

        protected void dump(FileDescriptor fileDescriptor, PrintWriter printWriter, String[] strArr) {
            if (TelecomServiceImpl.this.mContext.checkCallingOrSelfPermission("android.permission.DUMP") != 0) {
                printWriter.println("Permission Denial: can't dump TelecomService from from pid=" + Binder.getCallingPid() + ", uid=" + Binder.getCallingUid());
                return;
            }
            boolean z = false;
            if (strArr.length > 0 && "analytics".equals(strArr[0])) {
                Analytics.dumpToEncodedProto(printWriter, strArr);
                return;
            }
            if (strArr.length > 0 && "timeline".equalsIgnoreCase(strArr[0])) {
                z = true;
            }
            IndentingPrintWriter indentingPrintWriter = new IndentingPrintWriter(printWriter, "  ");
            if (TelecomServiceImpl.this.mCallsManager != null) {
                indentingPrintWriter.println("CallsManager: ");
                indentingPrintWriter.increaseIndent();
                TelecomServiceImpl.this.mCallsManager.dump(indentingPrintWriter);
                indentingPrintWriter.decreaseIndent();
                indentingPrintWriter.println("PhoneAccountRegistrar: ");
                indentingPrintWriter.increaseIndent();
                TelecomServiceImpl.this.mPhoneAccountRegistrar.dump(indentingPrintWriter);
                indentingPrintWriter.decreaseIndent();
                indentingPrintWriter.println("Analytics:");
                indentingPrintWriter.increaseIndent();
                Analytics.dump(indentingPrintWriter);
                indentingPrintWriter.decreaseIndent();
            }
            if (z) {
                Log.dumpEventsTimeline(indentingPrintWriter);
            } else {
                Log.dumpEvents(indentingPrintWriter);
            }
        }

        public Intent createManageBlockedNumbersIntent() {
            return BlockedNumbersActivity.getIntentForStartingActivity();
        }

        public boolean isIncomingCallPermitted(PhoneAccountHandle phoneAccountHandle) {
            boolean zIsIncomingCallPermitted;
            try {
                Log.startSession("TSI.iICP");
                TelecomServiceImpl.this.enforcePermission("android.permission.MANAGE_OWN_CALLS");
                synchronized (TelecomServiceImpl.this.mLock) {
                    long jClearCallingIdentity = Binder.clearCallingIdentity();
                    try {
                        zIsIncomingCallPermitted = TelecomServiceImpl.this.mCallsManager.isIncomingCallPermitted(phoneAccountHandle);
                    } finally {
                        Binder.restoreCallingIdentity(jClearCallingIdentity);
                    }
                }
                return zIsIncomingCallPermitted;
            } finally {
                Log.endSession();
            }
        }

        public boolean isOutgoingCallPermitted(PhoneAccountHandle phoneAccountHandle) {
            boolean zIsOutgoingCallPermitted;
            try {
                Log.startSession("TSI.iOCP");
                TelecomServiceImpl.this.enforcePermission("android.permission.MANAGE_OWN_CALLS");
                synchronized (TelecomServiceImpl.this.mLock) {
                    long jClearCallingIdentity = Binder.clearCallingIdentity();
                    try {
                        zIsOutgoingCallPermitted = TelecomServiceImpl.this.mCallsManager.isOutgoingCallPermitted(phoneAccountHandle);
                    } finally {
                        Binder.restoreCallingIdentity(jClearCallingIdentity);
                    }
                }
                return zIsOutgoingCallPermitted;
            } finally {
                Log.endSession();
            }
        }

        public void waitOnHandlers() {
            try {
                Log.startSession("TSI.wOH");
                TelecomServiceImpl.this.enforceModifyPermission();
                synchronized (TelecomServiceImpl.this.mLock) {
                    long jClearCallingIdentity = Binder.clearCallingIdentity();
                    try {
                        Log.i(this, "waitOnHandlers", new Object[0]);
                        TelecomServiceImpl.this.mCallsManager.waitOnHandlers();
                    } finally {
                        Binder.restoreCallingIdentity(jClearCallingIdentity);
                    }
                }
            } finally {
                Log.endSession();
            }
        }
    };
    private final CallIntentProcessor.Adapter mCallIntentProcessorAdapter;
    private CallsManager mCallsManager;
    private Context mContext;
    private final DefaultDialerCache mDefaultDialerCache;
    private final TelecomSystem.SyncRoot mLock;
    private PackageManager mPackageManager;
    private final PhoneAccountRegistrar mPhoneAccountRegistrar;
    private final SubscriptionManagerAdapter mSubscriptionManagerAdapter;
    private final UserCallIntentProcessorFactory mUserCallIntentProcessorFactory;

    public interface SubscriptionManagerAdapter {
        int getDefaultVoiceSubId();
    }

    static class SubscriptionManagerAdapterImpl implements SubscriptionManagerAdapter {
        SubscriptionManagerAdapterImpl() {
        }

        @Override
        public int getDefaultVoiceSubId() {
            return SubscriptionManager.getDefaultVoiceSubscriptionId();
        }
    }

    private boolean enforceAnswerCallPermission(String str, int i) {
        try {
            enforceModifyPermission();
            return true;
        } catch (SecurityException e) {
            enforcePermission("android.permission.ANSWER_PHONE_CALLS");
            int iPermissionToOpCode = AppOpsManager.permissionToOpCode("android.permission.ANSWER_PHONE_CALLS");
            if (iPermissionToOpCode != -1 && this.mAppOpsManager.checkOp(iPermissionToOpCode, i, str) != 0) {
                return false;
            }
            return true;
        }
    }

    private boolean enforceAcceptHandoverPermission(String str, int i) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.ACCEPT_HANDOVER", "App requires ACCEPT_HANDOVER permission to accept handovers.");
        int iPermissionToOpCode = AppOpsManager.permissionToOpCode("android.permission.ACCEPT_HANDOVER");
        if (iPermissionToOpCode != 74 || this.mAppOpsManager.checkOp(iPermissionToOpCode, i, str) != 0) {
            return false;
        }
        return true;
    }

    public TelecomServiceImpl(Context context, CallsManager callsManager, PhoneAccountRegistrar phoneAccountRegistrar, CallIntentProcessor.Adapter adapter, UserCallIntentProcessorFactory userCallIntentProcessorFactory, DefaultDialerCache defaultDialerCache, SubscriptionManagerAdapter subscriptionManagerAdapter, TelecomSystem.SyncRoot syncRoot) {
        this.mContext = context;
        this.mAppOpsManager = (AppOpsManager) this.mContext.getSystemService("appops");
        this.mPackageManager = this.mContext.getPackageManager();
        this.mCallsManager = callsManager;
        this.mLock = syncRoot;
        this.mPhoneAccountRegistrar = phoneAccountRegistrar;
        this.mUserCallIntentProcessorFactory = userCallIntentProcessorFactory;
        this.mDefaultDialerCache = defaultDialerCache;
        this.mCallIntentProcessorAdapter = adapter;
        this.mSubscriptionManagerAdapter = subscriptionManagerAdapter;
    }

    public ITelecomService.Stub getBinder() {
        return this.mBinderImpl;
    }

    private boolean isPhoneAccountHandleVisibleToCallingUser(PhoneAccountHandle phoneAccountHandle, UserHandle userHandle) {
        return this.mPhoneAccountRegistrar.getPhoneAccount(phoneAccountHandle, userHandle) != null;
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
        try {
            return (this.mPackageManager.getApplicationInfo(str, 128).flags & 1) != 0;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    private void acceptRingingCallInternal(int i) {
        Call firstCallWithState = this.mCallsManager.getFirstCallWithState(4);
        if (firstCallWithState != null) {
            if (i == -1 || !isValidAcceptVideoState(i)) {
                i = firstCallWithState.getVideoState();
            }
            firstCallWithState.answer(i);
        }
    }

    private boolean endCallInternal(String str) {
        Call foregroundCall = this.mCallsManager.getForegroundCall();
        if (foregroundCall == null) {
            foregroundCall = this.mCallsManager.getFirstCallWithState(5, 3, 10, 4, 6);
        }
        if (foregroundCall == null) {
            return false;
        }
        if (foregroundCall.isEmergencyCall()) {
            EventLog.writeEvent(1397638484, "132438333", -1, "");
            return false;
        }
        if (foregroundCall.getState() == 4) {
            foregroundCall.reject(false, null, str);
        } else {
            foregroundCall.disconnect(0L, str);
        }
        return true;
    }

    private void enforcePhoneAccountIsRegisteredEnabled(PhoneAccountHandle phoneAccountHandle, UserHandle userHandle) {
        PhoneAccount phoneAccount = this.mPhoneAccountRegistrar.getPhoneAccount(phoneAccountHandle, userHandle);
        if (phoneAccount == null) {
            EventLog.writeEvent(1397638484, "26864502", Integer.valueOf(Binder.getCallingUid()), "R");
            throw new SecurityException("This PhoneAccountHandle is not registered for this user!");
        }
        if (!phoneAccount.isEnabled()) {
            EventLog.writeEvent(1397638484, "26864502", Integer.valueOf(Binder.getCallingUid()), "E");
            throw new SecurityException("This PhoneAccountHandle is not enabled for this user!");
        }
    }

    private void enforcePhoneAccountModificationForPackage(String str) {
        if (this.mContext.checkCallingOrSelfPermission("android.permission.MODIFY_PHONE_STATE") != 0) {
            enforceConnectionServiceFeature();
            enforceCallingPackage(str);
        }
    }

    private void enforcePermissionOrPrivilegedDialer(String str, String str2) {
        if (!isPrivilegedDialerCalling(str2)) {
            try {
                enforcePermission(str);
            } catch (SecurityException e) {
                Log.e(this, e, "Caller must be the default or system dialer, or have the permission %s to perform this operation.", new Object[]{str});
                throw e;
            }
        }
    }

    private void enforceCallingPackage(String str) {
        this.mAppOpsManager.checkPackage(Binder.getCallingUid(), str);
    }

    private void enforceConnectionServiceFeature() {
        enforceFeature("android.software.connectionservice");
    }

    private void enforceRegisterSimSubscriptionPermission() {
        enforcePermission("android.permission.REGISTER_SIM_SUBSCRIPTION");
    }

    private void enforceModifyPermission() {
        enforcePermission("android.permission.MODIFY_PHONE_STATE");
    }

    private void enforceModifyPermission(String str) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.MODIFY_PHONE_STATE", str);
    }

    private void enforcePermission(String str) {
        this.mContext.enforceCallingOrSelfPermission(str, null);
    }

    private void enforceRegisterSelfManaged() {
        this.mContext.enforceCallingPermission("android.permission.MANAGE_OWN_CALLS", null);
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

    private void enforceCrossUserPermission(int i) {
        if (i != 1000 && i != 0) {
            this.mContext.enforceCallingOrSelfPermission("android.permission.INTERACT_ACROSS_USERS_FULL", "Must be system or have INTERACT_ACROSS_USERS_FULL permission");
        }
    }

    private void enforceFeature(String str) {
        if (!this.mContext.getPackageManager().hasSystemFeature(str)) {
            throw new UnsupportedOperationException("System does not support feature " + str);
        }
    }

    private boolean canReadPhoneState(String str, String str2) {
        if (isPrivilegedDialerCalling(str)) {
            return true;
        }
        try {
            this.mContext.enforceCallingOrSelfPermission("android.permission.READ_PRIVILEGED_PHONE_STATE", str2);
            return true;
        } catch (SecurityException e) {
            this.mContext.enforceCallingOrSelfPermission("android.permission.READ_PHONE_STATE", str2);
            return this.mAppOpsManager.noteOp(51, Binder.getCallingUid(), str) == 0;
        }
    }

    private boolean isSelfManagedConnectionService(PhoneAccountHandle phoneAccountHandle) {
        PhoneAccount phoneAccountUnchecked;
        return (phoneAccountHandle == null || (phoneAccountUnchecked = this.mPhoneAccountRegistrar.getPhoneAccountUnchecked(phoneAccountHandle)) == null || !phoneAccountUnchecked.isSelfManaged()) ? false : true;
    }

    private boolean canCallPhone(String str, String str2) {
        if (isPrivilegedDialerCalling(str)) {
            return true;
        }
        this.mContext.enforceCallingOrSelfPermission("android.permission.CALL_PHONE", str2);
        return this.mAppOpsManager.noteOp(13, Binder.getCallingUid(), str) == 0;
    }

    private boolean isCallerSimCallManager() {
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        try {
            PhoneAccountHandle simCallManagerOfCurrentUser = this.mPhoneAccountRegistrar.getSimCallManagerOfCurrentUser();
            if (simCallManagerOfCurrentUser != null) {
                try {
                    this.mAppOpsManager.checkPackage(Binder.getCallingUid(), simCallManagerOfCurrentUser.getComponentName().getPackageName());
                    return true;
                } catch (SecurityException e) {
                    return false;
                }
            }
            return false;
        } finally {
            Binder.restoreCallingIdentity(jClearCallingIdentity);
        }
    }

    private boolean isPrivilegedDialerCalling(String str) {
        this.mAppOpsManager.checkPackage(Binder.getCallingUid(), str);
        return this.mDefaultDialerCache.isDefaultOrSystemDialer(str, Binder.getCallingUserHandle().getIdentifier());
    }

    private TelephonyManager getTelephonyManager() {
        return (TelephonyManager) this.mContext.getSystemService("phone");
    }

    private boolean isValidAcceptVideoState(int i) {
        return ((i & (-2)) & (-3)) == 0;
    }
}
