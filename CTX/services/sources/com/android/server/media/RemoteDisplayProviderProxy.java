package com.android.server.media;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.media.IRemoteDisplayCallback;
import android.media.IRemoteDisplayProvider;
import android.media.RemoteDisplayState;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.UserHandle;
import android.util.Log;
import android.util.Slog;
import java.io.PrintWriter;
import java.lang.ref.WeakReference;
import java.util.Objects;

final class RemoteDisplayProviderProxy implements ServiceConnection {
    private Connection mActiveConnection;
    private boolean mBound;
    private final ComponentName mComponentName;
    private boolean mConnectionReady;
    private final Context mContext;
    private int mDiscoveryMode;
    private RemoteDisplayState mDisplayState;
    private Callback mDisplayStateCallback;
    private final Runnable mDisplayStateChanged = new Runnable() {
        @Override
        public void run() {
            RemoteDisplayProviderProxy.this.mScheduledDisplayStateChangedCallback = false;
            if (RemoteDisplayProviderProxy.this.mDisplayStateCallback != null) {
                RemoteDisplayProviderProxy.this.mDisplayStateCallback.onDisplayStateChanged(RemoteDisplayProviderProxy.this, RemoteDisplayProviderProxy.this.mDisplayState);
            }
        }
    };
    private final Handler mHandler = new Handler();
    private boolean mRunning;
    private boolean mScheduledDisplayStateChangedCallback;
    private String mSelectedDisplayId;
    private final int mUserId;
    private static final String TAG = "RemoteDisplayProvider";
    private static final boolean DEBUG = Log.isLoggable(TAG, 3);

    public interface Callback {
        void onDisplayStateChanged(RemoteDisplayProviderProxy remoteDisplayProviderProxy, RemoteDisplayState remoteDisplayState);
    }

    public RemoteDisplayProviderProxy(Context context, ComponentName componentName, int i) {
        this.mContext = context;
        this.mComponentName = componentName;
        this.mUserId = i;
    }

    public void dump(PrintWriter printWriter, String str) {
        printWriter.println(str + "Proxy");
        printWriter.println(str + "  mUserId=" + this.mUserId);
        printWriter.println(str + "  mRunning=" + this.mRunning);
        printWriter.println(str + "  mBound=" + this.mBound);
        printWriter.println(str + "  mActiveConnection=" + this.mActiveConnection);
        printWriter.println(str + "  mConnectionReady=" + this.mConnectionReady);
        printWriter.println(str + "  mDiscoveryMode=" + this.mDiscoveryMode);
        printWriter.println(str + "  mSelectedDisplayId=" + this.mSelectedDisplayId);
        printWriter.println(str + "  mDisplayState=" + this.mDisplayState);
    }

    public void setCallback(Callback callback) {
        this.mDisplayStateCallback = callback;
    }

    public RemoteDisplayState getDisplayState() {
        return this.mDisplayState;
    }

    public void setDiscoveryMode(int i) {
        if (this.mDiscoveryMode != i) {
            this.mDiscoveryMode = i;
            if (this.mConnectionReady) {
                this.mActiveConnection.setDiscoveryMode(i);
            }
            updateBinding();
        }
    }

    public void setSelectedDisplay(String str) {
        if (!Objects.equals(this.mSelectedDisplayId, str)) {
            if (this.mConnectionReady && this.mSelectedDisplayId != null) {
                this.mActiveConnection.disconnect(this.mSelectedDisplayId);
            }
            this.mSelectedDisplayId = str;
            if (this.mConnectionReady && str != null) {
                this.mActiveConnection.connect(str);
            }
            updateBinding();
        }
    }

    public void setDisplayVolume(int i) {
        if (this.mConnectionReady && this.mSelectedDisplayId != null) {
            this.mActiveConnection.setVolume(this.mSelectedDisplayId, i);
        }
    }

    public void adjustDisplayVolume(int i) {
        if (this.mConnectionReady && this.mSelectedDisplayId != null) {
            this.mActiveConnection.adjustVolume(this.mSelectedDisplayId, i);
        }
    }

    public boolean hasComponentName(String str, String str2) {
        return this.mComponentName.getPackageName().equals(str) && this.mComponentName.getClassName().equals(str2);
    }

    public String getFlattenedComponentName() {
        return this.mComponentName.flattenToShortString();
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
        if (this.mActiveConnection == null && shouldBind()) {
            unbind();
            bind();
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
        if (this.mRunning) {
            if (this.mDiscoveryMode != 0 || this.mSelectedDisplayId != null) {
                return true;
            }
            return false;
        }
        return false;
    }

    private void bind() {
        if (!this.mBound) {
            if (DEBUG) {
                Slog.d(TAG, this + ": Binding");
            }
            Intent intent = new Intent("com.android.media.remotedisplay.RemoteDisplayProvider");
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
            Slog.d(TAG, this + ": Connected");
        }
        if (this.mBound) {
            disconnect();
            IRemoteDisplayProvider iRemoteDisplayProviderAsInterface = IRemoteDisplayProvider.Stub.asInterface(iBinder);
            if (iRemoteDisplayProviderAsInterface != null) {
                Connection connection = new Connection(iRemoteDisplayProviderAsInterface);
                if (connection.register()) {
                    this.mActiveConnection = connection;
                    return;
                } else {
                    if (DEBUG) {
                        Slog.d(TAG, this + ": Registration failed");
                        return;
                    }
                    return;
                }
            }
            Slog.e(TAG, this + ": Service returned invalid remote display provider binder");
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
        if (this.mActiveConnection == connection) {
            this.mConnectionReady = true;
            if (this.mDiscoveryMode != 0) {
                this.mActiveConnection.setDiscoveryMode(this.mDiscoveryMode);
            }
            if (this.mSelectedDisplayId != null) {
                this.mActiveConnection.connect(this.mSelectedDisplayId);
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

    private void onDisplayStateChanged(Connection connection, RemoteDisplayState remoteDisplayState) {
        if (this.mActiveConnection == connection) {
            if (DEBUG) {
                Slog.d(TAG, this + ": State changed, state=" + remoteDisplayState);
            }
            setDisplayState(remoteDisplayState);
        }
    }

    private void disconnect() {
        if (this.mActiveConnection != null) {
            if (this.mSelectedDisplayId != null) {
                this.mActiveConnection.disconnect(this.mSelectedDisplayId);
            }
            this.mConnectionReady = false;
            this.mActiveConnection.dispose();
            this.mActiveConnection = null;
            setDisplayState(null);
        }
    }

    private void setDisplayState(RemoteDisplayState remoteDisplayState) {
        if (!Objects.equals(this.mDisplayState, remoteDisplayState)) {
            this.mDisplayState = remoteDisplayState;
            if (!this.mScheduledDisplayStateChangedCallback) {
                this.mScheduledDisplayStateChangedCallback = true;
                this.mHandler.post(this.mDisplayStateChanged);
            }
        }
    }

    public String toString() {
        return "Service connection " + this.mComponentName.flattenToShortString();
    }

    private final class Connection implements IBinder.DeathRecipient {
        private final ProviderCallback mCallback = new ProviderCallback(this);
        private final IRemoteDisplayProvider mProvider;

        public Connection(IRemoteDisplayProvider iRemoteDisplayProvider) {
            this.mProvider = iRemoteDisplayProvider;
        }

        public boolean register() {
            try {
                this.mProvider.asBinder().linkToDeath(this, 0);
                this.mProvider.setCallback(this.mCallback);
                RemoteDisplayProviderProxy.this.mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        RemoteDisplayProviderProxy.this.onConnectionReady(Connection.this);
                    }
                });
                return true;
            } catch (RemoteException e) {
                binderDied();
                return false;
            }
        }

        public void dispose() {
            this.mProvider.asBinder().unlinkToDeath(this, 0);
            this.mCallback.dispose();
        }

        public void setDiscoveryMode(int i) {
            try {
                this.mProvider.setDiscoveryMode(i);
            } catch (RemoteException e) {
                Slog.e(RemoteDisplayProviderProxy.TAG, "Failed to deliver request to set discovery mode.", e);
            }
        }

        public void connect(String str) {
            try {
                this.mProvider.connect(str);
            } catch (RemoteException e) {
                Slog.e(RemoteDisplayProviderProxy.TAG, "Failed to deliver request to connect to display.", e);
            }
        }

        public void disconnect(String str) {
            try {
                this.mProvider.disconnect(str);
            } catch (RemoteException e) {
                Slog.e(RemoteDisplayProviderProxy.TAG, "Failed to deliver request to disconnect from display.", e);
            }
        }

        public void setVolume(String str, int i) {
            try {
                this.mProvider.setVolume(str, i);
            } catch (RemoteException e) {
                Slog.e(RemoteDisplayProviderProxy.TAG, "Failed to deliver request to set display volume.", e);
            }
        }

        public void adjustVolume(String str, int i) {
            try {
                this.mProvider.adjustVolume(str, i);
            } catch (RemoteException e) {
                Slog.e(RemoteDisplayProviderProxy.TAG, "Failed to deliver request to adjust display volume.", e);
            }
        }

        @Override
        public void binderDied() {
            RemoteDisplayProviderProxy.this.mHandler.post(new Runnable() {
                @Override
                public void run() {
                    RemoteDisplayProviderProxy.this.onConnectionDied(Connection.this);
                }
            });
        }

        void postStateChanged(final RemoteDisplayState remoteDisplayState) {
            RemoteDisplayProviderProxy.this.mHandler.post(new Runnable() {
                @Override
                public void run() {
                    RemoteDisplayProviderProxy.this.onDisplayStateChanged(Connection.this, remoteDisplayState);
                }
            });
        }
    }

    private static final class ProviderCallback extends IRemoteDisplayCallback.Stub {
        private final WeakReference<Connection> mConnectionRef;

        public ProviderCallback(Connection connection) {
            this.mConnectionRef = new WeakReference<>(connection);
        }

        public void dispose() {
            this.mConnectionRef.clear();
        }

        public void onStateChanged(RemoteDisplayState remoteDisplayState) throws RemoteException {
            Connection connection = this.mConnectionRef.get();
            if (connection != null) {
                connection.postStateChanged(remoteDisplayState);
            }
        }
    }
}
