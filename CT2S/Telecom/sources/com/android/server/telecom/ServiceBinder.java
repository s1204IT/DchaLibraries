package com.android.server.telecom;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.IInterface;
import android.os.UserHandle;
import android.text.TextUtils;
import android.util.ArraySet;
import com.android.internal.util.Preconditions;
import java.util.Collections;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

abstract class ServiceBinder<ServiceInterface extends IInterface> {
    private IBinder mBinder;
    private final ComponentName mComponentName;
    private final Context mContext;
    private boolean mIsBindingAborted;
    private final String mServiceAction;
    private ServiceConnection mServiceConnection;
    private UserHandle mUserHandle;
    private final Set<BindCallback> mCallbacks = new ArraySet();
    private int mAssociatedCallCount = 0;
    private final Set<Listener> mListeners = Collections.newSetFromMap(new ConcurrentHashMap(8, 0.9f, 1));

    interface BindCallback {
        void onFailure();

        void onSuccess();
    }

    interface Listener<ServiceBinderClass extends ServiceBinder<?>> {
        void onUnbind(ServiceBinderClass servicebinderclass);
    }

    protected abstract void setServiceInterface(IBinder iBinder);

    final class Binder {
        Binder() {
        }

        void bind(BindCallback bindCallback) {
            ThreadUtil.checkOnMainThread();
            Log.d(ServiceBinder.this, "bind()", new Object[0]);
            ServiceBinder.this.clearAbort();
            if (!ServiceBinder.this.mCallbacks.isEmpty()) {
                ServiceBinder.this.mCallbacks.add(bindCallback);
                return;
            }
            ServiceBinder.this.mCallbacks.add(bindCallback);
            if (ServiceBinder.this.mServiceConnection == null) {
                Intent component = new Intent(ServiceBinder.this.mServiceAction).setComponent(ServiceBinder.this.mComponentName);
                ServiceBinderConnection serviceBinderConnection = new ServiceBinderConnection();
                Log.d(ServiceBinder.this, "Binding to service with intent: %s", component);
                if (!(ServiceBinder.this.mUserHandle != null ? ServiceBinder.this.mContext.bindServiceAsUser(component, serviceBinderConnection, 1, ServiceBinder.this.mUserHandle) : ServiceBinder.this.mContext.bindService(component, serviceBinderConnection, 1))) {
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

    private final class ServiceBinderConnection implements ServiceConnection {
        private ServiceBinderConnection() {
        }

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            ThreadUtil.checkOnMainThread();
            Log.i(this, "Service bound %s", componentName);
            if (ServiceBinder.this.mIsBindingAborted) {
                ServiceBinder.this.clearAbort();
                ServiceBinder.this.logServiceDisconnected("onServiceConnected");
                ServiceBinder.this.mContext.unbindService(this);
                ServiceBinder.this.handleFailedConnection();
                return;
            }
            ServiceBinder.this.mServiceConnection = this;
            ServiceBinder.this.setBinder(iBinder);
            ServiceBinder.this.handleSuccessfulConnection();
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            ServiceBinder.this.logServiceDisconnected("onServiceDisconnected");
            ServiceBinder.this.mServiceConnection = null;
            ServiceBinder.this.clearAbort();
            ServiceBinder.this.handleServiceDisconnected();
        }
    }

    protected ServiceBinder(String str, ComponentName componentName, Context context, UserHandle userHandle) {
        Preconditions.checkState(TextUtils.isEmpty(str) ? false : true);
        Preconditions.checkNotNull(componentName);
        this.mContext = context;
        this.mServiceAction = str;
        this.mComponentName = componentName;
        this.mUserHandle = userHandle;
    }

    final void incrementAssociatedCallCount() {
        this.mAssociatedCallCount++;
        Log.v(this, "Call count increment %d, %s", Integer.valueOf(this.mAssociatedCallCount), this.mComponentName.flattenToShortString());
    }

    final void decrementAssociatedCallCount() {
        if (this.mAssociatedCallCount > 0) {
            this.mAssociatedCallCount--;
            Log.v(this, "Call count decrement %d, %s", Integer.valueOf(this.mAssociatedCallCount), this.mComponentName.flattenToShortString());
            if (this.mAssociatedCallCount == 0) {
                unbind();
                return;
            }
            return;
        }
        Log.wtf(this, "%s: ignoring a request to decrement mAssociatedCallCount below zero", this.mComponentName.getClassName());
    }

    final void unbind() {
        ThreadUtil.checkOnMainThread();
        if (this.mServiceConnection == null) {
            this.mIsBindingAborted = true;
            return;
        }
        logServiceDisconnected("unbind");
        this.mContext.unbindService(this.mServiceConnection);
        this.mServiceConnection = null;
        setBinder(null);
    }

    final ComponentName getComponentName() {
        return this.mComponentName;
    }

    final boolean isServiceValid(String str) {
        if (this.mBinder != null) {
            return true;
        }
        Log.w(this, "%s invoked while service is unbound", str);
        return false;
    }

    final void addListener(Listener listener) {
        this.mListeners.add(listener);
    }

    private void logServiceDisconnected(String str) {
        Log.i(this, "Service unbound %s, from %s.", this.mComponentName, str);
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
        setBinder(null);
    }

    private void clearAbort() {
        this.mIsBindingAborted = false;
    }

    private void setBinder(IBinder iBinder) {
        if (this.mBinder != iBinder) {
            this.mBinder = iBinder;
            setServiceInterface(iBinder);
            if (iBinder == null) {
                Iterator<Listener> it = this.mListeners.iterator();
                while (it.hasNext()) {
                    it.next().onUnbind(this);
                }
            }
        }
    }
}
