package com.android.server.backup.transport;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.SystemClock;
import android.os.UserHandle;
import android.text.format.DateFormat;
import android.util.ArrayMap;
import android.util.EventLog;
import com.android.internal.annotations.GuardedBy;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.backup.IBackupTransport;
import com.android.internal.util.Preconditions;
import com.android.server.EventLogTags;
import com.android.server.backup.BackupManagerConstants;
import dalvik.system.CloseGuard;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.ref.WeakReference;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public class TransportClient {
    private static final int LOG_BUFFER_SIZE = 5;

    @VisibleForTesting
    static final String TAG = "TransportClient";
    private final Intent mBindIntent;
    private final CloseGuard mCloseGuard;
    private final ServiceConnection mConnection;
    private final Context mContext;
    private final String mCreatorLogString;
    private final String mIdentifier;
    private final Handler mListenerHandler;

    @GuardedBy("mStateLock")
    private final Map<TransportConnectionListener, String> mListeners;

    @GuardedBy("mLogBufferLock")
    private final List<String> mLogBuffer;
    private final Object mLogBufferLock;
    private final String mPrefixForLog;

    @GuardedBy("mStateLock")
    private int mState;
    private final Object mStateLock;

    @GuardedBy("mStateLock")
    private volatile IBackupTransport mTransport;
    private final ComponentName mTransportComponent;
    private final TransportStats mTransportStats;

    @Retention(RetentionPolicy.SOURCE)
    private @interface State {
        public static final int BOUND_AND_CONNECTING = 2;
        public static final int CONNECTED = 3;
        public static final int IDLE = 1;
        public static final int UNUSABLE = 0;
    }

    @Retention(RetentionPolicy.SOURCE)
    private @interface Transition {
        public static final int DOWN = -1;
        public static final int NO_TRANSITION = 0;
        public static final int UP = 1;
    }

    TransportClient(Context context, TransportStats transportStats, Intent intent, ComponentName componentName, String str, String str2) {
        this(context, transportStats, intent, componentName, str, str2, new Handler(Looper.getMainLooper()));
    }

    @VisibleForTesting
    TransportClient(Context context, TransportStats transportStats, Intent intent, ComponentName componentName, String str, String str2, Handler handler) {
        this.mStateLock = new Object();
        this.mLogBufferLock = new Object();
        this.mCloseGuard = CloseGuard.get();
        this.mLogBuffer = new LinkedList();
        this.mListeners = new ArrayMap();
        this.mState = 1;
        this.mContext = context;
        this.mTransportStats = transportStats;
        this.mTransportComponent = componentName;
        this.mBindIntent = intent;
        this.mIdentifier = str;
        this.mCreatorLogString = str2;
        this.mListenerHandler = handler;
        this.mConnection = new TransportConnection(context, this);
        this.mPrefixForLog = this.mTransportComponent.getShortClassName().replaceFirst(".*\\.", BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS) + "#" + this.mIdentifier + ":";
        this.mCloseGuard.open("markAsDisposed");
    }

    public ComponentName getTransportComponent() {
        return this.mTransportComponent;
    }

    public void connectAsync(TransportConnectionListener transportConnectionListener, String str) {
        synchronized (this.mStateLock) {
            checkStateIntegrityLocked();
            switch (this.mState) {
                case 0:
                    log(5, str, "Async connect: UNUSABLE client");
                    notifyListener(transportConnectionListener, null, str);
                    break;
                case 1:
                    if (this.mContext.bindServiceAsUser(this.mBindIntent, this.mConnection, 1, UserHandle.SYSTEM)) {
                        log(3, str, "Async connect: service bound, connecting");
                        setStateLocked(2, null);
                        this.mListeners.put(transportConnectionListener, str);
                    } else {
                        log(6, "Async connect: bindService returned false");
                        this.mContext.unbindService(this.mConnection);
                        notifyListener(transportConnectionListener, null, str);
                    }
                    break;
                case 2:
                    log(3, str, "Async connect: already connecting, adding listener");
                    this.mListeners.put(transportConnectionListener, str);
                    break;
                case 3:
                    log(3, str, "Async connect: reusing transport");
                    notifyListener(transportConnectionListener, this.mTransport, str);
                    break;
            }
        }
    }

    public void unbind(String str) {
        synchronized (this.mStateLock) {
            checkStateIntegrityLocked();
            log(3, str, "Unbind requested (was " + stateToString(this.mState) + ")");
            switch (this.mState) {
                case 2:
                    setStateLocked(1, null);
                    this.mContext.unbindService(this.mConnection);
                    notifyListenersAndClearLocked(null);
                    break;
                case 3:
                    setStateLocked(1, null);
                    this.mContext.unbindService(this.mConnection);
                    break;
            }
        }
    }

    public void markAsDisposed() {
        synchronized (this.mStateLock) {
            Preconditions.checkState(this.mState < 2, "Can't mark as disposed if still bound");
            this.mCloseGuard.close();
        }
    }

    public IBackupTransport connect(String str) {
        Preconditions.checkState(!Looper.getMainLooper().isCurrentThread(), "Can't call connect() on main thread");
        IBackupTransport iBackupTransport = this.mTransport;
        if (iBackupTransport != null) {
            log(3, str, "Sync connect: reusing transport");
            return iBackupTransport;
        }
        synchronized (this.mStateLock) {
            if (this.mState == 0) {
                log(5, str, "Sync connect: UNUSABLE client");
                return null;
            }
            final CompletableFuture completableFuture = new CompletableFuture();
            TransportConnectionListener transportConnectionListener = new TransportConnectionListener() {
                @Override
                public final void onTransportConnectionResult(IBackupTransport iBackupTransport2, TransportClient transportClient) {
                    completableFuture.complete(iBackupTransport2);
                }
            };
            long jElapsedRealtime = SystemClock.elapsedRealtime();
            log(3, str, "Sync connect: calling async");
            connectAsync(transportConnectionListener, str);
            try {
                IBackupTransport iBackupTransport2 = (IBackupTransport) completableFuture.get();
                long jElapsedRealtime2 = SystemClock.elapsedRealtime() - jElapsedRealtime;
                this.mTransportStats.registerConnectionTime(this.mTransportComponent, jElapsedRealtime2);
                log(3, str, String.format(Locale.US, "Connect took %d ms", Long.valueOf(jElapsedRealtime2)));
                return iBackupTransport2;
            } catch (InterruptedException | ExecutionException e) {
                log(6, str, e.getClass().getSimpleName() + " while waiting for transport: " + e.getMessage());
                return null;
            }
        }
    }

    public IBackupTransport connectOrThrow(String str) throws TransportNotAvailableException {
        IBackupTransport iBackupTransportConnect = connect(str);
        if (iBackupTransportConnect == null) {
            log(6, str, "Transport connection failed");
            throw new TransportNotAvailableException();
        }
        return iBackupTransportConnect;
    }

    public IBackupTransport getConnectedTransport(String str) throws TransportNotAvailableException {
        IBackupTransport iBackupTransport = this.mTransport;
        if (iBackupTransport == null) {
            log(6, str, "Transport not connected");
            throw new TransportNotAvailableException();
        }
        return iBackupTransport;
    }

    public String toString() {
        return "TransportClient{" + this.mTransportComponent.flattenToShortString() + "#" + this.mIdentifier + "}";
    }

    protected void finalize() throws Throwable {
        synchronized (this.mStateLock) {
            this.mCloseGuard.warnIfOpen();
            if (this.mState >= 2) {
                log(6, "TransportClient.finalize()", "Dangling TransportClient created in [" + this.mCreatorLogString + "] being GC'ed. Left bound, unbinding...");
                try {
                    unbind("TransportClient.finalize()");
                } catch (IllegalStateException e) {
                }
            }
        }
    }

    private void onServiceConnected(IBinder iBinder) {
        IBackupTransport iBackupTransportAsInterface = IBackupTransport.Stub.asInterface(iBinder);
        synchronized (this.mStateLock) {
            checkStateIntegrityLocked();
            if (this.mState != 0) {
                log(3, "Transport connected");
                setStateLocked(3, iBackupTransportAsInterface);
                notifyListenersAndClearLocked(iBackupTransportAsInterface);
            }
        }
    }

    private void onServiceDisconnected() {
        synchronized (this.mStateLock) {
            log(6, "Service disconnected: client UNUSABLE");
            setStateLocked(0, null);
            try {
                this.mContext.unbindService(this.mConnection);
            } catch (IllegalArgumentException e) {
                log(5, "Exception trying to unbind onServiceDisconnected(): " + e.getMessage());
            }
        }
    }

    private void onBindingDied() {
        synchronized (this.mStateLock) {
            checkStateIntegrityLocked();
            log(6, "Binding died: client UNUSABLE");
            switch (this.mState) {
                case 1:
                    log(6, "Unexpected state transition IDLE => UNUSABLE");
                    setStateLocked(0, null);
                    break;
                case 2:
                    setStateLocked(0, null);
                    this.mContext.unbindService(this.mConnection);
                    notifyListenersAndClearLocked(null);
                    break;
                case 3:
                    setStateLocked(0, null);
                    this.mContext.unbindService(this.mConnection);
                    break;
            }
        }
    }

    private void notifyListener(final TransportConnectionListener transportConnectionListener, final IBackupTransport iBackupTransport, String str) {
        log(4, "Notifying [" + str + "] transport = " + (iBackupTransport != null ? "IBackupTransport" : "null"));
        this.mListenerHandler.post(new Runnable() {
            @Override
            public final void run() {
                transportConnectionListener.onTransportConnectionResult(iBackupTransport, this.f$0);
            }
        });
    }

    @GuardedBy("mStateLock")
    private void notifyListenersAndClearLocked(IBackupTransport iBackupTransport) {
        for (Map.Entry<TransportConnectionListener, String> entry : this.mListeners.entrySet()) {
            notifyListener(entry.getKey(), iBackupTransport, entry.getValue());
        }
        this.mListeners.clear();
    }

    @GuardedBy("mStateLock")
    private void setStateLocked(int i, IBackupTransport iBackupTransport) {
        log(2, "State: " + stateToString(this.mState) + " => " + stateToString(i));
        onStateTransition(this.mState, i);
        this.mState = i;
        this.mTransport = iBackupTransport;
    }

    private void onStateTransition(int i, int i2) {
        String strFlattenToShortString = this.mTransportComponent.flattenToShortString();
        int iTransitionThroughState = transitionThroughState(i, i2, 2);
        int iTransitionThroughState2 = transitionThroughState(i, i2, 3);
        if (iTransitionThroughState != 0) {
            EventLog.writeEvent(EventLogTags.BACKUP_TRANSPORT_LIFECYCLE, strFlattenToShortString, Integer.valueOf(iTransitionThroughState == 1 ? 1 : 0));
        }
        if (iTransitionThroughState2 != 0) {
            EventLog.writeEvent(EventLogTags.BACKUP_TRANSPORT_CONNECTION, strFlattenToShortString, Integer.valueOf(iTransitionThroughState2 == 1 ? 1 : 0));
        }
    }

    private int transitionThroughState(int i, int i2, int i3) {
        if (i < i3 && i3 <= i2) {
            return 1;
        }
        if (i >= i3 && i3 > i2) {
            return -1;
        }
        return 0;
    }

    @GuardedBy("mStateLock")
    private void checkStateIntegrityLocked() {
        switch (this.mState) {
            case 0:
                checkState(this.mListeners.isEmpty(), "Unexpected listeners when state = UNUSABLE");
                checkState(this.mTransport == null, "Transport expected to be null when state = UNUSABLE");
                break;
            case 1:
                break;
            case 2:
                checkState(this.mTransport == null, "Transport expected to be null when state = BOUND_AND_CONNECTING");
                return;
            case 3:
                checkState(this.mListeners.isEmpty(), "Unexpected listeners when state = CONNECTED");
                checkState(this.mTransport != null, "Transport expected to be non-null when state = CONNECTED");
                return;
            default:
                checkState(false, "Unexpected state = " + stateToString(this.mState));
                return;
        }
        checkState(this.mListeners.isEmpty(), "Unexpected listeners when state = IDLE");
        checkState(this.mTransport == null, "Transport expected to be null when state = IDLE");
    }

    private void checkState(boolean z, String str) {
        if (!z) {
            log(6, str);
        }
    }

    private String stateToString(int i) {
        switch (i) {
            case 0:
                return "UNUSABLE";
            case 1:
                return "IDLE";
            case 2:
                return "BOUND_AND_CONNECTING";
            case 3:
                return "CONNECTED";
            default:
                return "<UNKNOWN = " + i + ">";
        }
    }

    private void log(int i, String str) {
        TransportUtils.log(i, TAG, TransportUtils.formatMessage(this.mPrefixForLog, null, str));
        saveLogEntry(TransportUtils.formatMessage(null, null, str));
    }

    private void log(int i, String str, String str2) {
        TransportUtils.log(i, TAG, TransportUtils.formatMessage(this.mPrefixForLog, str, str2));
        saveLogEntry(TransportUtils.formatMessage(null, str, str2));
    }

    private void saveLogEntry(String str) {
        String str2 = ((Object) DateFormat.format("yyyy-MM-dd HH:mm:ss", System.currentTimeMillis())) + " " + str;
        synchronized (this.mLogBufferLock) {
            if (this.mLogBuffer.size() == 5) {
                this.mLogBuffer.remove(this.mLogBuffer.size() - 1);
            }
            this.mLogBuffer.add(0, str2);
        }
    }

    List<String> getLogBuffer() {
        List<String> listUnmodifiableList;
        synchronized (this.mLogBufferLock) {
            listUnmodifiableList = Collections.unmodifiableList(this.mLogBuffer);
        }
        return listUnmodifiableList;
    }

    private static class TransportConnection implements ServiceConnection {
        private final Context mContext;
        private final WeakReference<TransportClient> mTransportClientRef;

        private TransportConnection(Context context, TransportClient transportClient) {
            this.mContext = context;
            this.mTransportClientRef = new WeakReference<>(transportClient);
        }

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            TransportClient transportClient = this.mTransportClientRef.get();
            if (transportClient != null) {
                transportClient.onServiceConnected(iBinder);
            } else {
                referenceLost("TransportConnection.onServiceConnected()");
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            TransportClient transportClient = this.mTransportClientRef.get();
            if (transportClient != null) {
                transportClient.onServiceDisconnected();
            } else {
                referenceLost("TransportConnection.onServiceDisconnected()");
            }
        }

        @Override
        public void onBindingDied(ComponentName componentName) {
            TransportClient transportClient = this.mTransportClientRef.get();
            if (transportClient != null) {
                transportClient.onBindingDied();
            } else {
                referenceLost("TransportConnection.onBindingDied()");
            }
        }

        private void referenceLost(String str) {
            this.mContext.unbindService(this);
            TransportUtils.log(4, TransportClient.TAG, str + " called but TransportClient reference has been GC'ed");
        }
    }
}
