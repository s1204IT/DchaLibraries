package com.android.server.tv;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.media.tv.ITvRemoteProvider;
import android.media.tv.ITvRemoteServiceInput;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.UserHandle;
import android.util.Log;
import android.util.Slog;
import java.io.PrintWriter;
import java.lang.ref.WeakReference;

final class TvRemoteProviderProxy implements ServiceConnection {
    private static final boolean DEBUG_KEY = false;
    protected static final String SERVICE_INTERFACE = "com.android.media.tv.remoteprovider.TvRemoteProvider";
    private Connection mActiveConnection;
    private boolean mBound;
    private final ComponentName mComponentName;
    private boolean mConnectionReady;
    private final Context mContext;
    private ProviderMethods mProviderMethods;
    private boolean mRunning;
    private final int mUid;
    private final int mUserId;
    private static final String TAG = "TvRemoteProvProxy";
    private static final boolean DEBUG = Log.isLoggable(TAG, 2);
    private final Object mLock = new Object();
    private final Handler mHandler = new Handler();

    public interface ProviderMethods {
        void clearInputBridge(TvRemoteProviderProxy tvRemoteProviderProxy, IBinder iBinder);

        void closeInputBridge(TvRemoteProviderProxy tvRemoteProviderProxy, IBinder iBinder);

        void openInputBridge(TvRemoteProviderProxy tvRemoteProviderProxy, IBinder iBinder, String str, int i, int i2, int i3);

        void sendKeyDown(TvRemoteProviderProxy tvRemoteProviderProxy, IBinder iBinder, int i);

        void sendKeyUp(TvRemoteProviderProxy tvRemoteProviderProxy, IBinder iBinder, int i);

        void sendPointerDown(TvRemoteProviderProxy tvRemoteProviderProxy, IBinder iBinder, int i, int i2, int i3);

        void sendPointerSync(TvRemoteProviderProxy tvRemoteProviderProxy, IBinder iBinder);

        void sendPointerUp(TvRemoteProviderProxy tvRemoteProviderProxy, IBinder iBinder, int i);

        void sendTimeStamp(TvRemoteProviderProxy tvRemoteProviderProxy, IBinder iBinder, long j);
    }

    public TvRemoteProviderProxy(Context context, ComponentName componentName, int i, int i2) {
        this.mContext = context;
        this.mComponentName = componentName;
        this.mUserId = i;
        this.mUid = i2;
    }

    public void dump(PrintWriter printWriter, String str) {
        printWriter.println(str + "Proxy");
        printWriter.println(str + "  mUserId=" + this.mUserId);
        printWriter.println(str + "  mRunning=" + this.mRunning);
        printWriter.println(str + "  mBound=" + this.mBound);
        printWriter.println(str + "  mActiveConnection=" + this.mActiveConnection);
        printWriter.println(str + "  mConnectionReady=" + this.mConnectionReady);
    }

    public void setProviderSink(ProviderMethods providerMethods) {
        this.mProviderMethods = providerMethods;
    }

    public boolean hasComponentName(String str, String str2) {
        return this.mComponentName.getPackageName().equals(str) && this.mComponentName.getClassName().equals(str2);
    }

    public void start() {
        if (!this.mRunning) {
            if (DEBUG) {
                Slog.d(TAG, this + ": Starting");
            }
            this.mRunning = true;
            updateBinding();
        }
    }

    public void stop() {
        if (this.mRunning) {
            if (DEBUG) {
                Slog.d(TAG, this + ": Stopping");
            }
            this.mRunning = false;
            updateBinding();
        }
    }

    public void rebindIfDisconnected() {
        synchronized (this.mLock) {
            if (this.mActiveConnection == null && shouldBind()) {
                unbind();
                bind();
            }
        }
    }

    private void updateBinding() {
        if (shouldBind()) {
            bind();
        } else {
            unbind();
        }
    }

    private boolean shouldBind() {
        return this.mRunning;
    }

    private void bind() {
        if (!this.mBound) {
            if (DEBUG) {
                Slog.d(TAG, this + ": Binding");
            }
            Intent intent = new Intent(SERVICE_INTERFACE);
            intent.setComponent(this.mComponentName);
            try {
                this.mBound = this.mContext.bindServiceAsUser(intent, this, 67108865, new UserHandle(this.mUserId));
                if (!this.mBound && DEBUG) {
                    Slog.d(TAG, this + ": Bind failed");
                }
            } catch (SecurityException e) {
                if (DEBUG) {
                    Slog.d(TAG, this + ": Bind failed", e);
                }
            }
        }
    }

    private void unbind() {
        if (this.mBound) {
            if (DEBUG) {
                Slog.d(TAG, this + ": Unbinding");
            }
            this.mBound = false;
            disconnect();
            this.mContext.unbindService(this);
        }
    }

    @Override
    public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
        if (DEBUG) {
            Slog.d(TAG, this + ": onServiceConnected()");
        }
        if (this.mBound) {
            disconnect();
            ITvRemoteProvider iTvRemoteProviderAsInterface = ITvRemoteProvider.Stub.asInterface(iBinder);
            if (iTvRemoteProviderAsInterface != null) {
                Connection connection = new Connection(iTvRemoteProviderAsInterface);
                if (connection.register()) {
                    synchronized (this.mLock) {
                        this.mActiveConnection = connection;
                    }
                    if (DEBUG) {
                        Slog.d(TAG, this + ": Connected successfully.");
                        return;
                    }
                    return;
                }
                if (DEBUG) {
                    Slog.d(TAG, this + ": Registration failed");
                    return;
                }
                return;
            }
            Slog.e(TAG, this + ": Service returned invalid remote-control provider binder");
        }
    }

    @Override
    public void onServiceDisconnected(ComponentName componentName) {
        if (DEBUG) {
            Slog.d(TAG, this + ": Service disconnected");
        }
        disconnect();
    }

    private void onConnectionReady(Connection connection) {
        synchronized (this.mLock) {
            if (DEBUG) {
                Slog.d(TAG, "onConnectionReady");
            }
            if (this.mActiveConnection == connection) {
                if (DEBUG) {
                    Slog.d(TAG, "mConnectionReady = true");
                }
                this.mConnectionReady = true;
            }
        }
    }

    private void onConnectionDied(Connection connection) {
        if (this.mActiveConnection == connection) {
            if (DEBUG) {
                Slog.d(TAG, this + ": Service connection died");
            }
            disconnect();
        }
    }

    private void disconnect() {
        synchronized (this.mLock) {
            if (this.mActiveConnection != null) {
                this.mConnectionReady = false;
                this.mActiveConnection.dispose();
                this.mActiveConnection = null;
            }
        }
    }

    public void inputBridgeConnected(IBinder iBinder) {
        synchronized (this.mLock) {
            if (DEBUG) {
                Slog.d(TAG, this + ": inputBridgeConnected token: " + iBinder);
            }
            if (this.mConnectionReady) {
                this.mActiveConnection.onInputBridgeConnected(iBinder);
            }
        }
    }

    private final class Connection implements IBinder.DeathRecipient {
        private final RemoteServiceInputProvider mServiceInputProvider = new RemoteServiceInputProvider(this);
        private final ITvRemoteProvider mTvRemoteProvider;

        public Connection(ITvRemoteProvider iTvRemoteProvider) {
            this.mTvRemoteProvider = iTvRemoteProvider;
        }

        public boolean register() {
            if (TvRemoteProviderProxy.DEBUG) {
                Slog.d(TvRemoteProviderProxy.TAG, "Connection::register()");
            }
            try {
                this.mTvRemoteProvider.asBinder().linkToDeath(this, 0);
                this.mTvRemoteProvider.setRemoteServiceInputSink(this.mServiceInputProvider);
                TvRemoteProviderProxy.this.mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        TvRemoteProviderProxy.this.onConnectionReady(Connection.this);
                    }
                });
                return true;
            } catch (RemoteException e) {
                binderDied();
                return false;
            }
        }

        public void dispose() {
            if (TvRemoteProviderProxy.DEBUG) {
                Slog.d(TvRemoteProviderProxy.TAG, "Connection::dispose()");
            }
            this.mTvRemoteProvider.asBinder().unlinkToDeath(this, 0);
            this.mServiceInputProvider.dispose();
        }

        public void onInputBridgeConnected(IBinder iBinder) {
            if (TvRemoteProviderProxy.DEBUG) {
                Slog.d(TvRemoteProviderProxy.TAG, this + ": onInputBridgeConnected");
            }
            try {
                this.mTvRemoteProvider.onInputBridgeConnected(iBinder);
            } catch (RemoteException e) {
                Slog.e(TvRemoteProviderProxy.TAG, "Failed to deliver onInputBridgeConnected. ", e);
            }
        }

        @Override
        public void binderDied() {
            TvRemoteProviderProxy.this.mHandler.post(new Runnable() {
                @Override
                public void run() {
                    TvRemoteProviderProxy.this.onConnectionDied(Connection.this);
                }
            });
        }

        void openInputBridge(IBinder iBinder, String str, int i, int i2, int i3) {
            IBinder iBinder2;
            String str2;
            synchronized (TvRemoteProviderProxy.this.mLock) {
                if (TvRemoteProviderProxy.this.mActiveConnection != this || Binder.getCallingUid() != TvRemoteProviderProxy.this.mUid) {
                    if (TvRemoteProviderProxy.DEBUG) {
                        Slog.w(TvRemoteProviderProxy.TAG, "openInputBridge, Invalid connection or incorrect uid: " + Binder.getCallingUid());
                    }
                } else {
                    if (TvRemoteProviderProxy.DEBUG) {
                        StringBuilder sb = new StringBuilder();
                        sb.append(this);
                        sb.append(": openInputBridge, token=");
                        iBinder2 = iBinder;
                        sb.append(iBinder2);
                        sb.append(", name=");
                        str2 = str;
                        sb.append(str2);
                        Slog.d(TvRemoteProviderProxy.TAG, sb.toString());
                    } else {
                        iBinder2 = iBinder;
                        str2 = str;
                    }
                    long jClearCallingIdentity = Binder.clearCallingIdentity();
                    try {
                        if (TvRemoteProviderProxy.this.mProviderMethods != null) {
                            TvRemoteProviderProxy.this.mProviderMethods.openInputBridge(TvRemoteProviderProxy.this, iBinder2, str2, i, i2, i3);
                        }
                        Binder.restoreCallingIdentity(jClearCallingIdentity);
                    } catch (Throwable th) {
                        Binder.restoreCallingIdentity(jClearCallingIdentity);
                        throw th;
                    }
                }
            }
        }

        void closeInputBridge(IBinder iBinder) {
            synchronized (TvRemoteProviderProxy.this.mLock) {
                if (TvRemoteProviderProxy.this.mActiveConnection != this || Binder.getCallingUid() != TvRemoteProviderProxy.this.mUid) {
                    if (TvRemoteProviderProxy.DEBUG) {
                        Slog.w(TvRemoteProviderProxy.TAG, "closeInputBridge, Invalid connection or incorrect uid: " + Binder.getCallingUid());
                    }
                } else {
                    if (TvRemoteProviderProxy.DEBUG) {
                        Slog.d(TvRemoteProviderProxy.TAG, this + ": closeInputBridge, token=" + iBinder);
                    }
                    long jClearCallingIdentity = Binder.clearCallingIdentity();
                    try {
                        if (TvRemoteProviderProxy.this.mProviderMethods != null) {
                            TvRemoteProviderProxy.this.mProviderMethods.closeInputBridge(TvRemoteProviderProxy.this, iBinder);
                        }
                        Binder.restoreCallingIdentity(jClearCallingIdentity);
                    } catch (Throwable th) {
                        Binder.restoreCallingIdentity(jClearCallingIdentity);
                        throw th;
                    }
                }
            }
        }

        void clearInputBridge(IBinder iBinder) {
            synchronized (TvRemoteProviderProxy.this.mLock) {
                if (TvRemoteProviderProxy.this.mActiveConnection != this || Binder.getCallingUid() != TvRemoteProviderProxy.this.mUid) {
                    if (TvRemoteProviderProxy.DEBUG) {
                        Slog.w(TvRemoteProviderProxy.TAG, "clearInputBridge, Invalid connection or incorrect uid: " + Binder.getCallingUid());
                    }
                } else {
                    if (TvRemoteProviderProxy.DEBUG) {
                        Slog.d(TvRemoteProviderProxy.TAG, this + ": clearInputBridge, token=" + iBinder);
                    }
                    long jClearCallingIdentity = Binder.clearCallingIdentity();
                    try {
                        if (TvRemoteProviderProxy.this.mProviderMethods != null) {
                            TvRemoteProviderProxy.this.mProviderMethods.clearInputBridge(TvRemoteProviderProxy.this, iBinder);
                        }
                        Binder.restoreCallingIdentity(jClearCallingIdentity);
                    } catch (Throwable th) {
                        Binder.restoreCallingIdentity(jClearCallingIdentity);
                        throw th;
                    }
                }
            }
        }

        void sendTimestamp(IBinder iBinder, long j) {
            synchronized (TvRemoteProviderProxy.this.mLock) {
                if (TvRemoteProviderProxy.this.mActiveConnection != this || Binder.getCallingUid() != TvRemoteProviderProxy.this.mUid) {
                    if (TvRemoteProviderProxy.DEBUG) {
                        Slog.w(TvRemoteProviderProxy.TAG, "sendTimeStamp, Invalid connection or incorrect uid: " + Binder.getCallingUid());
                    }
                } else {
                    long jClearCallingIdentity = Binder.clearCallingIdentity();
                    try {
                        if (TvRemoteProviderProxy.this.mProviderMethods != null) {
                            TvRemoteProviderProxy.this.mProviderMethods.sendTimeStamp(TvRemoteProviderProxy.this, iBinder, j);
                        }
                        Binder.restoreCallingIdentity(jClearCallingIdentity);
                    } catch (Throwable th) {
                        Binder.restoreCallingIdentity(jClearCallingIdentity);
                        throw th;
                    }
                }
            }
        }

        void sendKeyDown(IBinder iBinder, int i) {
            synchronized (TvRemoteProviderProxy.this.mLock) {
                if (TvRemoteProviderProxy.this.mActiveConnection != this || Binder.getCallingUid() != TvRemoteProviderProxy.this.mUid) {
                    if (TvRemoteProviderProxy.DEBUG) {
                        Slog.w(TvRemoteProviderProxy.TAG, "sendKeyDown, Invalid connection or incorrect uid: " + Binder.getCallingUid());
                    }
                } else {
                    long jClearCallingIdentity = Binder.clearCallingIdentity();
                    try {
                        if (TvRemoteProviderProxy.this.mProviderMethods != null) {
                            TvRemoteProviderProxy.this.mProviderMethods.sendKeyDown(TvRemoteProviderProxy.this, iBinder, i);
                        }
                        Binder.restoreCallingIdentity(jClearCallingIdentity);
                    } catch (Throwable th) {
                        Binder.restoreCallingIdentity(jClearCallingIdentity);
                        throw th;
                    }
                }
            }
        }

        void sendKeyUp(IBinder iBinder, int i) {
            synchronized (TvRemoteProviderProxy.this.mLock) {
                if (TvRemoteProviderProxy.this.mActiveConnection != this || Binder.getCallingUid() != TvRemoteProviderProxy.this.mUid) {
                    if (TvRemoteProviderProxy.DEBUG) {
                        Slog.w(TvRemoteProviderProxy.TAG, "sendKeyUp, Invalid connection or incorrect uid: " + Binder.getCallingUid());
                    }
                } else {
                    long jClearCallingIdentity = Binder.clearCallingIdentity();
                    try {
                        if (TvRemoteProviderProxy.this.mProviderMethods != null) {
                            TvRemoteProviderProxy.this.mProviderMethods.sendKeyUp(TvRemoteProviderProxy.this, iBinder, i);
                        }
                        Binder.restoreCallingIdentity(jClearCallingIdentity);
                    } catch (Throwable th) {
                        Binder.restoreCallingIdentity(jClearCallingIdentity);
                        throw th;
                    }
                }
            }
        }

        void sendPointerDown(IBinder iBinder, int i, int i2, int i3) {
            synchronized (TvRemoteProviderProxy.this.mLock) {
                if (TvRemoteProviderProxy.this.mActiveConnection != this || Binder.getCallingUid() != TvRemoteProviderProxy.this.mUid) {
                    if (TvRemoteProviderProxy.DEBUG) {
                        Slog.w(TvRemoteProviderProxy.TAG, "sendPointerDown, Invalid connection or incorrect uid: " + Binder.getCallingUid());
                    }
                } else {
                    long jClearCallingIdentity = Binder.clearCallingIdentity();
                    try {
                        if (TvRemoteProviderProxy.this.mProviderMethods != null) {
                            TvRemoteProviderProxy.this.mProviderMethods.sendPointerDown(TvRemoteProviderProxy.this, iBinder, i, i2, i3);
                        }
                        Binder.restoreCallingIdentity(jClearCallingIdentity);
                    } catch (Throwable th) {
                        Binder.restoreCallingIdentity(jClearCallingIdentity);
                        throw th;
                    }
                }
            }
        }

        void sendPointerUp(IBinder iBinder, int i) {
            synchronized (TvRemoteProviderProxy.this.mLock) {
                if (TvRemoteProviderProxy.this.mActiveConnection != this || Binder.getCallingUid() != TvRemoteProviderProxy.this.mUid) {
                    if (TvRemoteProviderProxy.DEBUG) {
                        Slog.w(TvRemoteProviderProxy.TAG, "sendPointerUp, Invalid connection or incorrect uid: " + Binder.getCallingUid());
                    }
                } else {
                    long jClearCallingIdentity = Binder.clearCallingIdentity();
                    try {
                        if (TvRemoteProviderProxy.this.mProviderMethods != null) {
                            TvRemoteProviderProxy.this.mProviderMethods.sendPointerUp(TvRemoteProviderProxy.this, iBinder, i);
                        }
                        Binder.restoreCallingIdentity(jClearCallingIdentity);
                    } catch (Throwable th) {
                        Binder.restoreCallingIdentity(jClearCallingIdentity);
                        throw th;
                    }
                }
            }
        }

        void sendPointerSync(IBinder iBinder) {
            synchronized (TvRemoteProviderProxy.this.mLock) {
                if (TvRemoteProviderProxy.this.mActiveConnection != this || Binder.getCallingUid() != TvRemoteProviderProxy.this.mUid) {
                    if (TvRemoteProviderProxy.DEBUG) {
                        Slog.w(TvRemoteProviderProxy.TAG, "sendPointerSync, Invalid connection or incorrect uid: " + Binder.getCallingUid());
                    }
                } else {
                    long jClearCallingIdentity = Binder.clearCallingIdentity();
                    try {
                        if (TvRemoteProviderProxy.this.mProviderMethods != null) {
                            TvRemoteProviderProxy.this.mProviderMethods.sendPointerSync(TvRemoteProviderProxy.this, iBinder);
                        }
                        Binder.restoreCallingIdentity(jClearCallingIdentity);
                    } catch (Throwable th) {
                        Binder.restoreCallingIdentity(jClearCallingIdentity);
                        throw th;
                    }
                }
            }
        }
    }

    private static final class RemoteServiceInputProvider extends ITvRemoteServiceInput.Stub {
        private final WeakReference<Connection> mConnectionRef;

        public RemoteServiceInputProvider(Connection connection) {
            this.mConnectionRef = new WeakReference<>(connection);
        }

        public void dispose() {
            this.mConnectionRef.clear();
        }

        public void openInputBridge(IBinder iBinder, String str, int i, int i2, int i3) throws RemoteException {
            Connection connection = this.mConnectionRef.get();
            if (connection != null) {
                connection.openInputBridge(iBinder, str, i, i2, i3);
            }
        }

        public void closeInputBridge(IBinder iBinder) throws RemoteException {
            Connection connection = this.mConnectionRef.get();
            if (connection != null) {
                connection.closeInputBridge(iBinder);
            }
        }

        public void clearInputBridge(IBinder iBinder) throws RemoteException {
            Connection connection = this.mConnectionRef.get();
            if (connection != null) {
                connection.clearInputBridge(iBinder);
            }
        }

        public void sendTimestamp(IBinder iBinder, long j) throws RemoteException {
            Connection connection = this.mConnectionRef.get();
            if (connection != null) {
                connection.sendTimestamp(iBinder, j);
            }
        }

        public void sendKeyDown(IBinder iBinder, int i) throws RemoteException {
            Connection connection = this.mConnectionRef.get();
            if (connection != null) {
                connection.sendKeyDown(iBinder, i);
            }
        }

        public void sendKeyUp(IBinder iBinder, int i) throws RemoteException {
            Connection connection = this.mConnectionRef.get();
            if (connection != null) {
                connection.sendKeyUp(iBinder, i);
            }
        }

        public void sendPointerDown(IBinder iBinder, int i, int i2, int i3) throws RemoteException {
            Connection connection = this.mConnectionRef.get();
            if (connection != null) {
                connection.sendPointerDown(iBinder, i, i2, i3);
            }
        }

        public void sendPointerUp(IBinder iBinder, int i) throws RemoteException {
            Connection connection = this.mConnectionRef.get();
            if (connection != null) {
                connection.sendPointerUp(iBinder, i);
            }
        }

        public void sendPointerSync(IBinder iBinder) throws RemoteException {
            Connection connection = this.mConnectionRef.get();
            if (connection != null) {
                connection.sendPointerSync(iBinder);
            }
        }
    }
}
