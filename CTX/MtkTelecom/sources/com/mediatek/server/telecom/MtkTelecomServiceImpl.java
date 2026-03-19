package com.mediatek.server.telecom;

import android.content.Context;
import android.os.Binder;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.UserHandle;
import android.telecom.Log;
import android.telecom.PhoneAccount;
import android.telecom.PhoneAccountHandle;
import android.telecom.VideoProfile;
import com.android.server.telecom.Call;
import com.android.server.telecom.CallsManager;
import com.android.server.telecom.PhoneAccountRegistrar;
import com.android.server.telecom.TelecomSystem;
import com.mediatek.internal.telecom.IMtkTelecomService;
import java.util.Iterator;
import java.util.List;

public class MtkTelecomServiceImpl extends IMtkTelecomService.Stub {
    private static final String TAG = MtkTelecomServiceImpl.class.getSimpleName();
    private final CallsManager mCallsManager;
    private final Context mContext;
    private final TelecomSystem.SyncRoot mLock;
    private final PhoneAccountRegistrar mPhoneAccountRegistrar;

    public MtkTelecomServiceImpl(Context context, CallsManager callsManager, PhoneAccountRegistrar phoneAccountRegistrar, TelecomSystem.SyncRoot syncRoot) {
        this.mContext = context;
        this.mCallsManager = callsManager;
        this.mPhoneAccountRegistrar = phoneAccountRegistrar;
        this.mLock = syncRoot;
        publish();
    }

    private void publish() {
        Log.i(TAG, "[publish]adding to ServiceManager", new Object[0]);
        ServiceManager.addService("mtk_telecom", this);
    }

    public boolean isInVideoCall(String str) throws RemoteException {
        try {
            Log.startSession("MTSI.iIVC");
            Log.i(TAG, "[isInVideoCall] from " + str, new Object[0]);
            synchronized (this.mLock) {
                Iterator<Call> it = this.mCallsManager.getCalls().iterator();
                while (it.hasNext()) {
                    if (!VideoProfile.isAudioOnly(it.next().getVideoState())) {
                        return true;
                    }
                }
                return false;
            }
        } finally {
            Log.endSession();
        }
    }

    public boolean isInVolteCall(String str) {
        try {
            Log.startSession("MTSI.iIVC");
            Log.i(TAG, "[isInVolteCall] from " + str, new Object[0]);
            synchronized (this.mLock) {
                for (Call call : this.mCallsManager.getCalls()) {
                    if (call.hasProperty(32768) && (call.getState() == 3 || call.getState() == 5 || call.getState() == 4 || call.getState() == 6)) {
                        return true;
                    }
                }
                return false;
            }
        } finally {
            Log.endSession();
        }
    }

    public List<PhoneAccount> getAllPhoneAccountsIncludingVirtual() throws RemoteException {
        List<PhoneAccount> allPhoneAccounts;
        UserHandle callingUserHandle = Binder.getCallingUserHandle();
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        try {
            Log.startSession("MTSI.gAPAIV");
            synchronized (this.mLock) {
                allPhoneAccounts = this.mPhoneAccountRegistrar.getAllPhoneAccounts(callingUserHandle);
            }
            return allPhoneAccounts;
        } finally {
            Binder.restoreCallingIdentity(jClearCallingIdentity);
            Log.endSession();
        }
    }

    public List<PhoneAccountHandle> getAllPhoneAccountHandlesIncludingVirtual() throws RemoteException {
        List<PhoneAccountHandle> allPhoneAccountHandles;
        UserHandle callingUserHandle = Binder.getCallingUserHandle();
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        try {
            Log.startSession("MTSI.gAPAHIV");
            synchronized (this.mLock) {
                allPhoneAccountHandles = this.mPhoneAccountRegistrar.getAllPhoneAccountHandles(callingUserHandle);
            }
            return allPhoneAccountHandles;
        } finally {
            Binder.restoreCallingIdentity(jClearCallingIdentity);
            Log.endSession();
        }
    }

    public boolean isInCall(String str) {
        boolean zHasOngoingCallsEx;
        try {
            Log.startSession("MTSI.iIC");
            synchronized (this.mLock) {
                zHasOngoingCallsEx = this.mCallsManager.hasOngoingCallsEx();
            }
            return zHasOngoingCallsEx;
        } finally {
            Log.endSession();
        }
    }
}
