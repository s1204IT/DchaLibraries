package com.android.server.telecom.callfiltering;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.ResolveInfo;
import android.os.Binder;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.UserHandle;
import android.telecom.Log;
import android.text.TextUtils;
import com.android.internal.telecom.ICallScreeningAdapter;
import com.android.internal.telecom.ICallScreeningService;
import com.android.server.telecom.Call;
import com.android.server.telecom.CallsManager;
import com.android.server.telecom.DefaultDialerCache;
import com.android.server.telecom.ParcelableCallUtils;
import com.android.server.telecom.PhoneAccountRegistrar;
import com.android.server.telecom.TelecomSystem;
import com.android.server.telecom.callfiltering.IncomingCallFilter;
import java.util.List;

public class CallScreeningServiceFilter implements IncomingCallFilter.CallFilter {
    private Call mCall;
    private CallFilterResultCallback mCallback;
    private final CallsManager mCallsManager;
    private ServiceConnection mConnection;
    private final Context mContext;
    private final DefaultDialerCache mDefaultDialerCache;
    private final ParcelableCallUtils.Converter mParcelableCallUtilsConverter;
    private final PhoneAccountRegistrar mPhoneAccountRegistrar;
    private ICallScreeningService mService;
    private final TelecomSystem.SyncRoot mTelecomLock;
    private boolean mHasFinished = false;
    private CallFilteringResult mResult = new CallFilteringResult(true, false, true, true);

    private class CallScreeningServiceConnection implements ServiceConnection {
        private CallScreeningServiceConnection() {
        }

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            Log.startSession("CSCR.oSC");
            try {
                synchronized (CallScreeningServiceFilter.this.mTelecomLock) {
                    Log.addEvent(CallScreeningServiceFilter.this.mCall, "SCREENING_BOUND", componentName);
                    if (!CallScreeningServiceFilter.this.mHasFinished) {
                        CallScreeningServiceFilter.this.onServiceBound(ICallScreeningService.Stub.asInterface(iBinder));
                    }
                }
            } finally {
                Log.endSession();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            Log.startSession("CSCR.oSD");
            try {
                synchronized (CallScreeningServiceFilter.this.mTelecomLock) {
                    CallScreeningServiceFilter.this.finishCallScreening();
                }
            } finally {
                Log.endSession();
            }
        }
    }

    private class CallScreeningAdapter extends ICallScreeningAdapter.Stub {
        private CallScreeningAdapter() {
        }

        public void allowCall(String str) {
            Log.startSession("CSCR.aC");
            long jClearCallingIdentity = Binder.clearCallingIdentity();
            try {
                synchronized (CallScreeningServiceFilter.this.mTelecomLock) {
                    Log.d(this, "allowCall(%s)", new Object[]{str});
                    if (CallScreeningServiceFilter.this.mCall != null && CallScreeningServiceFilter.this.mCall.getId().equals(str)) {
                        CallScreeningServiceFilter.this.mResult = new CallFilteringResult(true, false, true, true);
                    } else {
                        Log.w(this, "allowCall, unknown call id: %s", new Object[]{str});
                    }
                    CallScreeningServiceFilter.this.finishCallScreening();
                }
            } finally {
                Binder.restoreCallingIdentity(jClearCallingIdentity);
                Log.endSession();
            }
        }

        public void disallowCall(String str, boolean z, boolean z2, boolean z3) {
            Log.startSession("CSCR.dC");
            long jClearCallingIdentity = Binder.clearCallingIdentity();
            try {
                synchronized (CallScreeningServiceFilter.this.mTelecomLock) {
                    Log.i(this, "disallowCall(%s), shouldReject: %b, shouldAddToCallLog: %b, shouldShowNotification: %b", new Object[]{str, Boolean.valueOf(z), Boolean.valueOf(z2), Boolean.valueOf(z3)});
                    if (CallScreeningServiceFilter.this.mCall != null && CallScreeningServiceFilter.this.mCall.getId().equals(str)) {
                        CallScreeningServiceFilter.this.mResult = new CallFilteringResult(false, z, z2, z3);
                    } else {
                        Log.w(this, "disallowCall, unknown call id: %s", new Object[]{str});
                    }
                    CallScreeningServiceFilter.this.finishCallScreening();
                }
            } finally {
                Binder.restoreCallingIdentity(jClearCallingIdentity);
                Log.endSession();
            }
        }
    }

    public CallScreeningServiceFilter(Context context, CallsManager callsManager, PhoneAccountRegistrar phoneAccountRegistrar, DefaultDialerCache defaultDialerCache, ParcelableCallUtils.Converter converter, TelecomSystem.SyncRoot syncRoot) {
        this.mContext = context;
        this.mPhoneAccountRegistrar = phoneAccountRegistrar;
        this.mCallsManager = callsManager;
        this.mDefaultDialerCache = defaultDialerCache;
        this.mParcelableCallUtilsConverter = converter;
        this.mTelecomLock = syncRoot;
    }

    @Override
    public void startFilterLookup(Call call, CallFilterResultCallback callFilterResultCallback) {
        if (this.mHasFinished) {
            Log.w(this, "Attempting to reuse CallScreeningServiceFilter. Ignoring.", new Object[0]);
            return;
        }
        Log.addEvent(call, "SCREENING_SENT");
        this.mCall = call;
        this.mCallback = callFilterResultCallback;
        if (!bindService()) {
            Log.i(this, "Could not bind to call screening service", new Object[0]);
            finishCallScreening();
        }
    }

    private void finishCallScreening() {
        if (!this.mHasFinished) {
            Log.addEvent(this.mCall, "SCREENING_COMPLETED", this.mResult);
            this.mCallback.onCallFilteringComplete(this.mCall, this.mResult);
            if (this.mConnection != null) {
                this.mContext.unbindService(this.mConnection);
                this.mConnection = null;
            }
            this.mService = null;
            this.mHasFinished = true;
        }
    }

    private boolean bindService() {
        String defaultDialerApplication = this.mDefaultDialerCache.getDefaultDialerApplication(-2);
        if (TextUtils.isEmpty(defaultDialerApplication)) {
            Log.i(this, "Default dialer is empty. Not performing call screening.", new Object[0]);
            return false;
        }
        Intent intent = new Intent("android.telecom.CallScreeningService").setPackage(defaultDialerApplication);
        List listQueryIntentServicesAsUser = this.mContext.getPackageManager().queryIntentServicesAsUser(intent, 0, this.mCallsManager.getCurrentUserHandle().getIdentifier());
        if (listQueryIntentServicesAsUser.isEmpty()) {
            Log.i(this, "There are no call screening services installed on this device.", new Object[0]);
            return false;
        }
        ResolveInfo resolveInfo = (ResolveInfo) listQueryIntentServicesAsUser.get(0);
        if (resolveInfo.serviceInfo == null) {
            Log.w(this, "The call screening service has invalid service info", new Object[0]);
            return false;
        }
        if (resolveInfo.serviceInfo.permission == null || !resolveInfo.serviceInfo.permission.equals("android.permission.BIND_SCREENING_SERVICE")) {
            Log.w(this, "CallScreeningService must require BIND_SCREENING_SERVICE permission: " + resolveInfo.serviceInfo.packageName, new Object[0]);
            return false;
        }
        ComponentName componentName = new ComponentName(resolveInfo.serviceInfo.packageName, resolveInfo.serviceInfo.name);
        Log.addEvent(this.mCall, "BIND_SCREENING", componentName);
        intent.setComponent(componentName);
        CallScreeningServiceConnection callScreeningServiceConnection = new CallScreeningServiceConnection();
        if (!this.mContext.bindServiceAsUser(intent, callScreeningServiceConnection, 67108865, UserHandle.CURRENT)) {
            return false;
        }
        Log.d(this, "bindService, found service, waiting for it to connect", new Object[0]);
        this.mConnection = callScreeningServiceConnection;
        return true;
    }

    private void onServiceBound(ICallScreeningService iCallScreeningService) {
        this.mService = iCallScreeningService;
        try {
            this.mService.screenCall(new CallScreeningAdapter(), this.mParcelableCallUtilsConverter.toParcelableCall(this.mCall, false, this.mPhoneAccountRegistrar));
        } catch (RemoteException e) {
            Log.e(this, e, "Failed to set the call screening adapter.", new Object[0]);
            finishCallScreening();
        }
    }
}
