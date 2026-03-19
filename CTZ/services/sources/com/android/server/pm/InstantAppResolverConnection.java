package com.android.server.pm;

import android.app.IInstantAppResolver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.InstantAppResolveInfo;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.IRemoteCallback;
import android.os.RemoteException;
import android.os.SystemClock;
import android.util.Slog;
import android.util.TimedRemoteCaller;
import com.android.internal.annotations.GuardedBy;
import com.android.internal.os.BackgroundThread;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.TimeoutException;

final class InstantAppResolverConnection implements IBinder.DeathRecipient {
    private static final long BIND_SERVICE_TIMEOUT_MS;
    private static final long CALL_SERVICE_TIMEOUT_MS;
    private static final boolean DEBUG_INSTANT;
    private static final int STATE_BINDING = 1;
    private static final int STATE_IDLE = 0;
    private static final int STATE_PENDING = 2;
    private static final String TAG = "PackageManager";
    private final Context mContext;
    private final Intent mIntent;

    @GuardedBy("mLock")
    private IInstantAppResolver mRemoteInstance;
    private final Object mLock = new Object();
    private final GetInstantAppResolveInfoCaller mGetInstantAppResolveInfoCaller = new GetInstantAppResolveInfoCaller();
    private final ServiceConnection mServiceConnection = new MyServiceConnection(this, null);

    @GuardedBy("mLock")
    private int mBindState = 0;
    private final Handler mBgHandler = BackgroundThread.getHandler();

    public static abstract class PhaseTwoCallback {
        abstract void onPhaseTwoResolved(List<InstantAppResolveInfo> list, long j);
    }

    static {
        BIND_SERVICE_TIMEOUT_MS = Build.IS_ENG ? 500L : 300L;
        CALL_SERVICE_TIMEOUT_MS = Build.IS_ENG ? 200L : 100L;
        DEBUG_INSTANT = Build.IS_DEBUGGABLE;
    }

    public InstantAppResolverConnection(Context context, ComponentName componentName, String str) {
        this.mContext = context;
        this.mIntent = new Intent(str).setComponent(componentName);
    }

    public final List<InstantAppResolveInfo> getInstantAppResolveInfoList(Intent intent, int[] iArr, String str) throws ConnectionException {
        throwIfCalledOnMainThread();
        try {
            try {
                try {
                    List<InstantAppResolveInfo> instantAppResolveInfoList = this.mGetInstantAppResolveInfoCaller.getInstantAppResolveInfoList(getRemoteInstanceLazy(str), intent, iArr, str);
                    synchronized (this.mLock) {
                        this.mLock.notifyAll();
                    }
                    return instantAppResolveInfoList;
                } catch (RemoteException e) {
                    synchronized (this.mLock) {
                        this.mLock.notifyAll();
                        return null;
                    }
                } catch (TimeoutException e2) {
                    throw new ConnectionException(2);
                }
            } catch (InterruptedException e3) {
                throw new ConnectionException(3);
            } catch (TimeoutException e4) {
                throw new ConnectionException(1);
            }
        } catch (Throwable th) {
            synchronized (this.mLock) {
                this.mLock.notifyAll();
                throw th;
            }
        }
    }

    class AnonymousClass1 extends IRemoteCallback.Stub {
        final PhaseTwoCallback val$callback;
        final Handler val$callbackHandler;
        final long val$startTime;

        AnonymousClass1(Handler handler, PhaseTwoCallback phaseTwoCallback, long j) {
            this.val$callbackHandler = handler;
            this.val$callback = phaseTwoCallback;
            this.val$startTime = j;
        }

        public void sendResult(Bundle bundle) throws RemoteException {
            final ArrayList parcelableArrayList = bundle.getParcelableArrayList("android.app.extra.RESOLVE_INFO");
            Handler handler = this.val$callbackHandler;
            final PhaseTwoCallback phaseTwoCallback = this.val$callback;
            final long j = this.val$startTime;
            handler.post(new Runnable() {
                @Override
                public final void run() {
                    phaseTwoCallback.onPhaseTwoResolved(parcelableArrayList, j);
                }
            });
        }
    }

    public final void getInstantAppIntentFilterList(Intent intent, int[] iArr, String str, PhaseTwoCallback phaseTwoCallback, Handler handler, long j) throws ConnectionException {
        try {
            getRemoteInstanceLazy(str).getInstantAppIntentFilterList(intent, iArr, str, new AnonymousClass1(handler, phaseTwoCallback, j));
        } catch (RemoteException e) {
        } catch (InterruptedException e2) {
            throw new ConnectionException(3);
        } catch (TimeoutException e3) {
            throw new ConnectionException(1);
        }
    }

    private IInstantAppResolver getRemoteInstanceLazy(String str) throws InterruptedException, TimeoutException, ConnectionException {
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        try {
            return bind(str);
        } finally {
            Binder.restoreCallingIdentity(jClearCallingIdentity);
        }
    }

    @GuardedBy("mLock")
    private void waitForBindLocked(String str) throws InterruptedException, TimeoutException {
        long jUptimeMillis = SystemClock.uptimeMillis();
        while (this.mBindState != 0 && this.mRemoteInstance == null) {
            long jUptimeMillis2 = BIND_SERVICE_TIMEOUT_MS - (SystemClock.uptimeMillis() - jUptimeMillis);
            if (jUptimeMillis2 <= 0) {
                throw new TimeoutException("[" + str + "] Didn't bind to resolver in time!");
            }
            this.mLock.wait(jUptimeMillis2);
        }
    }

    private android.app.IInstantAppResolver bind(java.lang.String r11) throws com.android.server.pm.InstantAppResolverConnection.ConnectionException, java.util.concurrent.TimeoutException, java.lang.InterruptedException {
        r0 = r10.mLock;
        synchronized (r0) {
            ;
            if (r10.mRemoteInstance != null) {
                r11 = r10.mRemoteInstance;
                return r11;
            } else {
                if (r10.mBindState != 2) {
                    r1 = false;
                } else {
                    if (com.android.server.pm.InstantAppResolverConnection.DEBUG_INSTANT) {
                        r5 = new java.lang.StringBuilder();
                        r5.append("[");
                        r5.append(r11);
                        r5.append("] Previous bind timed out; waiting for connection");
                        android.util.Slog.i(com.android.server.pm.InstantAppResolverConnection.TAG, r5.toString());
                    }
                    waitForBindLocked(r11);
                    if (r10.mRemoteInstance != null) {
                        r1 = r10.mRemoteInstance;
                        return r1;
                    } else {
                        r1 = false;
                    }
                }
                if (r10.mBindState == 1) {
                    if (com.android.server.pm.InstantAppResolverConnection.DEBUG_INSTANT) {
                        r2 = new java.lang.StringBuilder();
                        r2.append("[");
                        r2.append(r11);
                        r2.append("] Another thread is binding; waiting for connection");
                        android.util.Slog.i(com.android.server.pm.InstantAppResolverConnection.TAG, r2.toString());
                    }
                    waitForBindLocked(r11);
                    if (r10.mRemoteInstance != null) {
                        r11 = r10.mRemoteInstance;
                        return r11;
                    } else {
                        throw new com.android.server.pm.InstantAppResolverConnection.ConnectionException(1);
                    }
                } else {
                    r10.mBindState = 1;
                    if (r1) {
                        if (com.android.server.pm.InstantAppResolverConnection.DEBUG_INSTANT) {
                            r5 = new java.lang.StringBuilder();
                            r5.append("[");
                            r5.append(r11);
                            r5.append("] Previous connection never established; rebinding");
                            android.util.Slog.i(com.android.server.pm.InstantAppResolverConnection.TAG, r5.toString());
                        }
                        r10.mContext.unbindService(r10.mServiceConnection);
                    }
                    if (com.android.server.pm.InstantAppResolverConnection.DEBUG_INSTANT) {
                        r5 = new java.lang.StringBuilder();
                        r5.append("[");
                        r5.append(r11);
                        r5.append("] Binding to instant app resolver");
                        android.util.Slog.v(com.android.server.pm.InstantAppResolverConnection.TAG, r5.toString());
                    }
                    r1 = r10.mContext.bindServiceAsUser(r10.mIntent, r10.mServiceConnection, 67108865, android.os.UserHandle.SYSTEM);
                    if (r1) {
                        r4 = r10.mLock;
                        synchronized (r4) {
                            ;
                            waitForBindLocked(r11);
                            r11 = r10.mRemoteInstance;
                            r0 = r10.mLock;
                            synchronized (r0) {
                                ;
                                if (!r1 || r11 != null) {
                                    r10.mBindState = 0;
                                } else {
                                    r10.mBindState = 2;
                                }
                                r10.mLock.notifyAll();
                            }
                            return r11;
                        }
                    } else {
                        r6 = new java.lang.StringBuilder();
                        r6.append("[");
                        r6.append(r11);
                        r6.append("] Failed to bind to: ");
                        r6.append(r10.mIntent);
                        android.util.Slog.w(com.android.server.pm.InstantAppResolverConnection.TAG, r6.toString());
                        throw new com.android.server.pm.InstantAppResolverConnection.ConnectionException(1);
                    }
                }
            }
        }
    }

    private void throwIfCalledOnMainThread() {
        if (Thread.currentThread() == this.mContext.getMainLooper().getThread()) {
            throw new RuntimeException("Cannot invoke on the main thread");
        }
    }

    void optimisticBind() {
        this.mBgHandler.post(new Runnable() {
            @Override
            public final void run() {
                InstantAppResolverConnection.lambda$optimisticBind$0(this.f$0);
            }
        });
    }

    public static void lambda$optimisticBind$0(InstantAppResolverConnection instantAppResolverConnection) {
        try {
            if (instantAppResolverConnection.bind("Optimistic Bind") != null && DEBUG_INSTANT) {
                Slog.i(TAG, "Optimistic bind succeeded.");
            }
        } catch (ConnectionException | InterruptedException | TimeoutException e) {
            Slog.e(TAG, "Optimistic bind failed.", e);
        }
    }

    @Override
    public void binderDied() {
        if (DEBUG_INSTANT) {
            Slog.d(TAG, "Binder to instant app resolver died");
        }
        synchronized (this.mLock) {
            handleBinderDiedLocked();
        }
        optimisticBind();
    }

    @GuardedBy("mLock")
    private void handleBinderDiedLocked() {
        if (this.mRemoteInstance != null) {
            try {
                this.mRemoteInstance.asBinder().unlinkToDeath(this, 0);
            } catch (NoSuchElementException e) {
            }
        }
        this.mRemoteInstance = null;
    }

    public static class ConnectionException extends Exception {
        public static final int FAILURE_BIND = 1;
        public static final int FAILURE_CALL = 2;
        public static final int FAILURE_INTERRUPTED = 3;
        public final int failure;

        public ConnectionException(int i) {
            this.failure = i;
        }
    }

    private final class MyServiceConnection implements ServiceConnection {
        private MyServiceConnection() {
        }

        MyServiceConnection(InstantAppResolverConnection instantAppResolverConnection, AnonymousClass1 anonymousClass1) {
            this();
        }

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            if (InstantAppResolverConnection.DEBUG_INSTANT) {
                Slog.d(InstantAppResolverConnection.TAG, "Connected to instant app resolver");
            }
            synchronized (InstantAppResolverConnection.this.mLock) {
                InstantAppResolverConnection.this.mRemoteInstance = IInstantAppResolver.Stub.asInterface(iBinder);
                if (InstantAppResolverConnection.this.mBindState == 2) {
                    InstantAppResolverConnection.this.mBindState = 0;
                }
                try {
                    iBinder.linkToDeath(InstantAppResolverConnection.this, 0);
                } catch (RemoteException e) {
                    InstantAppResolverConnection.this.handleBinderDiedLocked();
                }
                InstantAppResolverConnection.this.mLock.notifyAll();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            if (InstantAppResolverConnection.DEBUG_INSTANT) {
                Slog.d(InstantAppResolverConnection.TAG, "Disconnected from instant app resolver");
            }
            synchronized (InstantAppResolverConnection.this.mLock) {
                InstantAppResolverConnection.this.handleBinderDiedLocked();
            }
        }
    }

    private static final class GetInstantAppResolveInfoCaller extends TimedRemoteCaller<List<InstantAppResolveInfo>> {
        private final IRemoteCallback mCallback;

        public GetInstantAppResolveInfoCaller() {
            super(InstantAppResolverConnection.CALL_SERVICE_TIMEOUT_MS);
            this.mCallback = new IRemoteCallback.Stub() {
                public void sendResult(Bundle bundle) throws RemoteException {
                    GetInstantAppResolveInfoCaller.this.onRemoteMethodResult(bundle.getParcelableArrayList("android.app.extra.RESOLVE_INFO"), bundle.getInt("android.app.extra.SEQUENCE", -1));
                }
            };
        }

        public List<InstantAppResolveInfo> getInstantAppResolveInfoList(IInstantAppResolver iInstantAppResolver, Intent intent, int[] iArr, String str) throws TimeoutException, RemoteException {
            int iOnBeforeRemoteCall = onBeforeRemoteCall();
            iInstantAppResolver.getInstantAppResolveInfoList(intent, iArr, str, iOnBeforeRemoteCall, this.mCallback);
            return (List) getResultTimed(iOnBeforeRemoteCall);
        }
    }
}
