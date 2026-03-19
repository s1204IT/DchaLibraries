package com.android.server.pm;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.EphemeralResolveInfo;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.IRemoteCallback;
import android.os.RemoteException;
import android.os.SystemClock;
import android.os.UserHandle;
import android.util.TimedRemoteCaller;
import com.android.internal.app.IEphemeralResolver;
import com.mediatek.datashaping.DataShapingUtils;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeoutException;

final class EphemeralResolverConnection {
    private static final long BIND_SERVICE_TIMEOUT_MS;
    private final Context mContext;
    private final Intent mIntent;
    private IEphemeralResolver mRemoteInstance;
    private final Object mLock = new Object();
    private final GetEphemeralResolveInfoCaller mGetEphemeralResolveInfoCaller = new GetEphemeralResolveInfoCaller();
    private final ServiceConnection mServiceConnection = new MyServiceConnection(this, null);

    static {
        BIND_SERVICE_TIMEOUT_MS = "eng".equals(Build.TYPE) ? 300 : 200;
    }

    public EphemeralResolverConnection(Context context, ComponentName componentName) {
        this.mContext = context;
        this.mIntent = new Intent().setComponent(componentName);
    }

    public final List<EphemeralResolveInfo> getEphemeralResolveInfoList(int hashPrefix) {
        Object obj;
        throwIfCalledOnMainThread();
        try {
            List<EphemeralResolveInfo> ephemeralResolveInfoList = this.mGetEphemeralResolveInfoCaller.getEphemeralResolveInfoList(getRemoteInstanceLazy(), hashPrefix);
            synchronized (this.mLock) {
                this.mLock.notifyAll();
            }
            return ephemeralResolveInfoList;
        } catch (RemoteException e) {
            obj = this.mLock;
            synchronized (obj) {
                this.mLock.notifyAll();
                return null;
            }
        } catch (TimeoutException e2) {
            obj = this.mLock;
            synchronized (obj) {
                this.mLock.notifyAll();
                return null;
            }
        } catch (Throwable th) {
            synchronized (this.mLock) {
                this.mLock.notifyAll();
                throw th;
            }
        }
    }

    public void dump(FileDescriptor fd, PrintWriter pw, String prefix) {
        synchronized (this.mLock) {
            pw.append((CharSequence) prefix).append("bound=").append((CharSequence) (this.mRemoteInstance != null ? "true" : "false")).println();
            pw.flush();
            try {
                getRemoteInstanceLazy().asBinder().dump(fd, new String[]{prefix});
            } catch (RemoteException e) {
            } catch (TimeoutException e2) {
            }
        }
    }

    private IEphemeralResolver getRemoteInstanceLazy() throws TimeoutException {
        synchronized (this.mLock) {
            if (this.mRemoteInstance != null) {
                return this.mRemoteInstance;
            }
            bindLocked();
            return this.mRemoteInstance;
        }
    }

    private void bindLocked() throws TimeoutException {
        if (this.mRemoteInstance != null) {
            return;
        }
        this.mContext.bindServiceAsUser(this.mIntent, this.mServiceConnection, 67108865, UserHandle.SYSTEM);
        long startMillis = SystemClock.uptimeMillis();
        while (this.mRemoteInstance == null) {
            long elapsedMillis = SystemClock.uptimeMillis() - startMillis;
            long remainingMillis = BIND_SERVICE_TIMEOUT_MS - elapsedMillis;
            if (remainingMillis <= 0) {
                throw new TimeoutException("Didn't bind to resolver in time.");
            }
            try {
                this.mLock.wait(remainingMillis);
            } catch (InterruptedException e) {
            }
        }
        this.mLock.notifyAll();
    }

    private void throwIfCalledOnMainThread() {
        if (Thread.currentThread() != this.mContext.getMainLooper().getThread()) {
        } else {
            throw new RuntimeException("Cannot invoke on the main thread");
        }
    }

    private final class MyServiceConnection implements ServiceConnection {
        MyServiceConnection(EphemeralResolverConnection this$0, MyServiceConnection myServiceConnection) {
            this();
        }

        private MyServiceConnection() {
        }

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            synchronized (EphemeralResolverConnection.this.mLock) {
                EphemeralResolverConnection.this.mRemoteInstance = IEphemeralResolver.Stub.asInterface(service);
                EphemeralResolverConnection.this.mLock.notifyAll();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            synchronized (EphemeralResolverConnection.this.mLock) {
                EphemeralResolverConnection.this.mRemoteInstance = null;
            }
        }
    }

    private static final class GetEphemeralResolveInfoCaller extends TimedRemoteCaller<List<EphemeralResolveInfo>> {
        private final IRemoteCallback mCallback;

        public GetEphemeralResolveInfoCaller() {
            super(DataShapingUtils.CLOSING_DELAY_BUFFER_FOR_MUSIC);
            this.mCallback = new IRemoteCallback.Stub() {
                public void sendResult(Bundle data) throws RemoteException {
                    ArrayList<EphemeralResolveInfo> resolveList = data.getParcelableArrayList("com.android.internal.app.RESOLVE_INFO");
                    int sequence = data.getInt("com.android.internal.app.SEQUENCE", -1);
                    GetEphemeralResolveInfoCaller.this.onRemoteMethodResult(resolveList, sequence);
                }
            };
        }

        public List<EphemeralResolveInfo> getEphemeralResolveInfoList(IEphemeralResolver target, int hashPrefix) throws TimeoutException, RemoteException {
            int sequence = onBeforeRemoteCall();
            target.getEphemeralResolveInfoList(this.mCallback, hashPrefix, sequence);
            return (List) getResultTimed(sequence);
        }
    }
}
