package com.android.server.telecom;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.UserHandle;
import android.telecom.Log;
import android.text.TextUtils;
import android.util.ArraySet;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.util.Preconditions;
import com.android.server.telecom.TelecomSystem;
import com.mediatek.internal.telecom.IMtkConnectionService;
import java.util.Collections;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

abstract class ServiceBinder {
    private IBinder mBinder;
    protected final ComponentName mComponentName;
    private final Context mContext;
    private boolean mIsBindingAborted;
    protected final TelecomSystem.SyncRoot mLock;
    private IBinder mMtkBinder;
    private final String mServiceAction;
    private ServiceConnection mServiceConnection;
    private ServiceDeathRecipient mServiceDeathRecipient;
    private UserHandle mUserHandle;
    private final Set<BindCallback> mCallbacks = new ArraySet();
    private int mAssociatedCallCount = 0;
    private final Set<Listener> mListeners = Collections.newSetFromMap(new ConcurrentHashMap(8, 0.9f, 1));

    interface BindCallback {
        void onFailure();

        void onSuccess();
    }

    interface Listener<ServiceBinderClass extends ServiceBinder> {
        void onUnbind(ServiceBinderClass servicebinderclass);
    }

    protected abstract void removeMtkServiceInterface();

    protected abstract void removeServiceInterface();

    protected abstract void setMtkServiceInterface(IBinder iBinder);

    protected abstract void setServiceInterface(IBinder iBinder);

    final class Binder2 {
        Binder2() {
        }

        void bind(BindCallback bindCallback, Call call) {
            boolean zBindService;
            Log.d(ServiceBinder.this, "bind()", new Object[0]);
            ServiceBinder.this.clearAbort();
            if (!ServiceBinder.this.mCallbacks.isEmpty()) {
                ServiceBinder.this.mCallbacks.add(bindCallback);
                return;
            }
            ServiceBinder.this.mCallbacks.add(bindCallback);
            if (ServiceBinder.this.mServiceConnection == null) {
                Intent component = new Intent(ServiceBinder.this.mServiceAction).setComponent(ServiceBinder.this.mComponentName);
                ServiceBinderConnection serviceBinderConnection = ServiceBinder.this.new ServiceBinderConnection(call);
                Log.addEvent(call, "BIND_CS", ServiceBinder.this.mComponentName);
                if (ServiceBinder.this.mUserHandle == null) {
                    zBindService = ServiceBinder.this.mContext.bindService(component, serviceBinderConnection, 67108865);
                } else {
                    zBindService = ServiceBinder.this.mContext.bindServiceAsUser(component, serviceBinderConnection, 67108865, ServiceBinder.this.mUserHandle);
                }
                if (!zBindService) {
                    ServiceBinder.this.handleFailedConnection();
                    return;
                }
                return;
            }
            Log.d(ServiceBinder.this, "Service is already bound.", new Object[0]);
            Preconditions.checkNotNull(ServiceBinder.this.mBinder);
            ServiceBinder.this.handleSuccessfulConnection();
        }
    }

    private class ServiceDeathRecipient implements IBinder.DeathRecipient {
        private ComponentName mComponentName;
        final ServiceBinder this$0;

        @Override
        public void binderDied() {
            try {
                synchronized (this.this$0.mLock) {
                    Log.startSession("SDR.bD");
                    Log.i(this, "binderDied: ConnectionService %s died.", new Object[]{this.mComponentName});
                    this.this$0.logServiceDisconnected("binderDied");
                    this.this$0.handleDisconnect();
                }
            } finally {
                Log.endSession();
            }
        }
    }

    private final class ServiceBinderConnection implements ServiceConnection {
        private Call mCall;

        ServiceBinderConnection(Call call) {
            this.mCall = call;
        }

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            try {
                Log.startSession("SBC.oSC");
                synchronized (ServiceBinder.this.mLock) {
                    Log.i(this, "Service bound %s", new Object[]{componentName});
                    Log.addEvent(this.mCall, "CS_BOUND", componentName);
                    this.mCall = null;
                    if (ServiceBinder.this.mIsBindingAborted) {
                        ServiceBinder.this.clearAbort();
                        ServiceBinder.this.logServiceDisconnected("onServiceConnected");
                        ServiceBinder.this.mContext.unbindService(this);
                        ServiceBinder.this.handleFailedConnection();
                        return;
                    }
                    if (iBinder != null) {
                        ServiceBinder.this.mServiceDeathRecipient = null;
                        ServiceBinder.this.setMtkBinder(iBinder);
                        IBinder aospBinder = ServiceBinder.this.getAospBinder(iBinder);
                        ServiceBinder.this.mServiceConnection = this;
                        ServiceBinder.this.setBinder(aospBinder);
                        ServiceBinder.this.handleSuccessfulConnection();
                    }
                }
            } finally {
                Log.endSession();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            try {
                Log.startSession("SBC.oSD");
                synchronized (ServiceBinder.this.mLock) {
                    ServiceBinder.this.logServiceDisconnected("onServiceDisconnected");
                    ServiceBinder.this.mContext.unbindService(ServiceBinder.this.mServiceConnection);
                    ServiceBinder.this.handleDisconnect();
                }
            } finally {
                Log.endSession();
            }
        }
    }

    private void handleDisconnect() {
        this.mServiceConnection = null;
        clearAbort();
        handleServiceDisconnected();
    }

    protected ServiceBinder(String str, ComponentName componentName, Context context, TelecomSystem.SyncRoot syncRoot, UserHandle userHandle) {
        Preconditions.checkState(!TextUtils.isEmpty(str));
        Preconditions.checkNotNull(componentName);
        this.mContext = context;
        this.mLock = syncRoot;
        this.mServiceAction = str;
        this.mComponentName = componentName;
        this.mUserHandle = userHandle;
    }

    final UserHandle getUserHandle() {
        return this.mUserHandle;
    }

    final void incrementAssociatedCallCount() {
        this.mAssociatedCallCount++;
        Log.v(this, "Call count increment %d, %s", new Object[]{Integer.valueOf(this.mAssociatedCallCount), this.mComponentName.flattenToShortString()});
    }

    final void decrementAssociatedCallCount() {
        decrementAssociatedCallCount(false);
    }

    final void decrementAssociatedCallCount(boolean z) {
        if (this.mAssociatedCallCount > 0) {
            this.mAssociatedCallCount--;
            Log.v(this, "Call count decrement %d, %s", new Object[]{Integer.valueOf(this.mAssociatedCallCount), this.mComponentName.flattenToShortString()});
            if (!z && this.mAssociatedCallCount == 0) {
                unbind();
                return;
            }
            return;
        }
        Log.wtf(this, "%s: ignoring a request to decrement mAssociatedCallCount below zero", new Object[]{this.mComponentName.getClassName()});
    }

    final void unbind() {
        if (this.mServiceConnection == null) {
            this.mIsBindingAborted = true;
            return;
        }
        logServiceDisconnected("unbind");
        unlinkDeathRecipient();
        this.mContext.unbindService(this.mServiceConnection);
        this.mServiceConnection = null;
        setMtkBinder(null);
        setBinder(null);
    }

    public final ComponentName getComponentName() {
        return this.mComponentName;
    }

    @VisibleForTesting
    public boolean isServiceValid(String str) {
        if (this.mBinder != null) {
            return true;
        }
        Log.w(this, "%s invoked while service is unbound", new Object[]{str});
        return false;
    }

    final void addListener(Listener listener) {
        this.mListeners.add(listener);
    }

    private void logServiceDisconnected(String str) {
        Log.i(this, "Service unbound %s, from %s.", new Object[]{this.mComponentName, str});
    }

    private void handleSuccessfulConnection() {
        Iterator<BindCallback> it = this.mCallbacks.iterator();
        while (it.hasNext()) {
            it.next().onSuccess();
        }
        this.mCallbacks.clear();
    }

    private void handleFailedConnection() {
        Iterator<BindCallback> it = this.mCallbacks.iterator();
        while (it.hasNext()) {
            it.next().onFailure();
        }
        this.mCallbacks.clear();
    }

    private void handleServiceDisconnected() {
        unlinkDeathRecipient();
        setMtkBinder(null);
        setBinder(null);
    }

    private void unlinkDeathRecipient() {
        if (this.mServiceDeathRecipient != null && this.mBinder != null) {
            if (!this.mBinder.unlinkToDeath(this.mServiceDeathRecipient, 0)) {
                Log.i(this, "unlinkDeathRecipient: failed to unlink %s", new Object[]{this.mComponentName});
            }
            this.mServiceDeathRecipient = null;
            return;
        }
        Log.w(this, "unlinkDeathRecipient: death recipient is null.", new Object[0]);
    }

    private void clearAbort() {
        this.mIsBindingAborted = false;
    }

    private void setBinder(IBinder iBinder) {
        if (this.mBinder != iBinder) {
            if (iBinder == null) {
                removeServiceInterface();
                this.mBinder = null;
                Iterator<Listener> it = this.mListeners.iterator();
                while (it.hasNext()) {
                    it.next().onUnbind(this);
                }
                return;
            }
            this.mBinder = iBinder;
            setServiceInterface(iBinder);
        }
    }

    public boolean isMtkServiceValid(String str) {
        if (this.mMtkBinder != null) {
            return true;
        }
        Log.w(this, "%s invoked while mediatek service is unbound", new Object[]{str});
        return false;
    }

    private void setMtkBinder(IBinder iBinder) {
        if (!TelephonyUtil.isPstnComponentName(this.mComponentName)) {
            Log.d(this, "not mediatek service, binder is aosp.", new Object[0]);
            return;
        }
        if (this.mMtkBinder != iBinder) {
            if (iBinder == null) {
                removeMtkServiceInterface();
                this.mMtkBinder = null;
            } else {
                this.mMtkBinder = iBinder;
                setMtkServiceInterface(iBinder);
            }
        }
    }

    private IBinder getAospBinder(IBinder iBinder) {
        if (this.mMtkBinder == null) {
            Log.w(this, "binder is aosp, return directly.", new Object[0]);
            return iBinder;
        }
        try {
            return IMtkConnectionService.Stub.asInterface(iBinder).getBinder();
        } catch (RemoteException e) {
            Log.e(this, e, "get aosp binder error! it shouldn't be!", new Object[0]);
            return null;
        }
    }
}
