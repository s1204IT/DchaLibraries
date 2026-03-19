package com.android.server.utils;

import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Handler;
import android.os.IBinder;
import android.os.IInterface;
import android.os.RemoteException;
import android.os.SystemClock;
import android.os.UserHandle;
import android.util.Slog;
import com.android.server.utils.ManagedApplicationService;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Objects;

public class ManagedApplicationService {
    private static final int MAX_RETRY_COUNT = 4;
    private static final long MAX_RETRY_DURATION_MS = 16000;
    private static final long MIN_RETRY_DURATION_MS = 2000;
    public static final int RETRY_BEST_EFFORT = 3;
    public static final int RETRY_FOREVER = 1;
    public static final int RETRY_NEVER = 2;
    private static final long RETRY_RESET_TIME_MS = 64000;
    private IInterface mBoundInterface;
    private final BinderChecker mChecker;
    private final int mClientLabel;
    private final ComponentName mComponent;
    private ServiceConnection mConnection;
    private final Context mContext;
    private final EventCallback mEventCb;
    private final Handler mHandler;
    private final boolean mIsImportant;
    private long mLastRetryTimeMs;
    private PendingEvent mPendingEvent;
    private int mRetryCount;
    private final int mRetryType;
    private boolean mRetrying;
    private final String mSettingsAction;
    private final int mUserId;
    private final String TAG = getClass().getSimpleName();
    private final Runnable mRetryRunnable = new Runnable() {
        @Override
        public final void run() {
            this.f$0.doRetry();
        }
    };
    private final Object mLock = new Object();
    private long mNextRetryDurationMs = MIN_RETRY_DURATION_MS;

    public interface BinderChecker {
        IInterface asInterface(IBinder iBinder);

        boolean checkType(IInterface iInterface);
    }

    public interface EventCallback {
        void onServiceEvent(LogEvent logEvent);
    }

    public interface LogFormattable {
        String toLogString(SimpleDateFormat simpleDateFormat);
    }

    public interface PendingEvent {
        void runEvent(IInterface iInterface) throws RemoteException;
    }

    public static class LogEvent implements LogFormattable {
        public static final int EVENT_BINDING_DIED = 3;
        public static final int EVENT_CONNECTED = 1;
        public static final int EVENT_DISCONNECTED = 2;
        public static final int EVENT_STOPPED_PERMANENTLY = 4;
        public final ComponentName component;
        public final int event;
        public final long timestamp;

        public LogEvent(long j, ComponentName componentName, int i) {
            this.timestamp = j;
            this.component = componentName;
            this.event = i;
        }

        @Override
        public String toLogString(SimpleDateFormat simpleDateFormat) {
            StringBuilder sb = new StringBuilder();
            sb.append(simpleDateFormat.format(new Date(this.timestamp)));
            sb.append("   ");
            sb.append(eventToString(this.event));
            sb.append(" Managed Service: ");
            sb.append(this.component == null ? "None" : this.component.flattenToString());
            return sb.toString();
        }

        public static String eventToString(int i) {
            switch (i) {
                case 1:
                    return "Connected";
                case 2:
                    return "Disconnected";
                case 3:
                    return "Binding Died For";
                case 4:
                    return "Permanently Stopped";
                default:
                    return "Unknown Event Occurred";
            }
        }
    }

    private ManagedApplicationService(Context context, ComponentName componentName, int i, int i2, String str, BinderChecker binderChecker, boolean z, int i3, Handler handler, EventCallback eventCallback) {
        this.mContext = context;
        this.mComponent = componentName;
        this.mUserId = i;
        this.mClientLabel = i2;
        this.mSettingsAction = str;
        this.mChecker = binderChecker;
        this.mIsImportant = z;
        this.mRetryType = i3;
        this.mHandler = handler;
        this.mEventCb = eventCallback;
    }

    public static ManagedApplicationService build(Context context, ComponentName componentName, int i, int i2, String str, BinderChecker binderChecker, boolean z, int i3, Handler handler, EventCallback eventCallback) {
        return new ManagedApplicationService(context, componentName, i, i2, str, binderChecker, z, i3, handler, eventCallback);
    }

    public int getUserId() {
        return this.mUserId;
    }

    public ComponentName getComponent() {
        return this.mComponent;
    }

    public boolean disconnectIfNotMatching(ComponentName componentName, int i) {
        if (matches(componentName, i)) {
            return false;
        }
        disconnect();
        return true;
    }

    public void sendEvent(PendingEvent pendingEvent) {
        IInterface iInterface;
        synchronized (this.mLock) {
            iInterface = this.mBoundInterface;
            if (iInterface == null) {
                this.mPendingEvent = pendingEvent;
            }
        }
        if (iInterface != null) {
            try {
                pendingEvent.runEvent(iInterface);
            } catch (RemoteException | RuntimeException e) {
                Slog.e(this.TAG, "Received exception from user service: ", e);
            }
        }
    }

    public void disconnect() {
        synchronized (this.mLock) {
            if (this.mConnection == null) {
                return;
            }
            this.mContext.unbindService(this.mConnection);
            this.mConnection = null;
            this.mBoundInterface = null;
        }
    }

    public void connect() {
        synchronized (this.mLock) {
            if (this.mConnection != null) {
                return;
            }
            Intent component = new Intent().setComponent(this.mComponent);
            if (this.mClientLabel != 0) {
                component.putExtra("android.intent.extra.client_label", this.mClientLabel);
            }
            if (this.mSettingsAction != null) {
                component.putExtra("android.intent.extra.client_intent", PendingIntent.getActivity(this.mContext, 0, new Intent(this.mSettingsAction), 0));
            }
            this.mConnection = new AnonymousClass1();
            int i = 67108865;
            if (this.mIsImportant) {
                i = 67108929;
            }
            try {
                if (!this.mContext.bindServiceAsUser(component, this.mConnection, i, new UserHandle(this.mUserId))) {
                    Slog.w(this.TAG, "Unable to bind service: " + component);
                    startRetriesLocked();
                }
            } catch (SecurityException e) {
                Slog.w(this.TAG, "Unable to bind service: " + component, e);
                startRetriesLocked();
            }
        }
    }

    class AnonymousClass1 implements ServiceConnection {
        AnonymousClass1() {
        }

        @Override
        public void onBindingDied(ComponentName componentName) {
            final long jCurrentTimeMillis = System.currentTimeMillis();
            Slog.w(ManagedApplicationService.this.TAG, "Service binding died: " + componentName);
            synchronized (ManagedApplicationService.this.mLock) {
                if (ManagedApplicationService.this.mConnection != this) {
                    return;
                }
                ManagedApplicationService.this.mHandler.post(new Runnable() {
                    @Override
                    public final void run() {
                        ManagedApplicationService.AnonymousClass1 anonymousClass1 = this.f$0;
                        ManagedApplicationService.this.mEventCb.onServiceEvent(new ManagedApplicationService.LogEvent(jCurrentTimeMillis, ManagedApplicationService.this.mComponent, 3));
                    }
                });
                ManagedApplicationService.this.mBoundInterface = null;
                ManagedApplicationService.this.startRetriesLocked();
            }
        }

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            IInterface iInterface;
            PendingEvent pendingEvent;
            final long jCurrentTimeMillis = System.currentTimeMillis();
            Slog.i(ManagedApplicationService.this.TAG, "Service connected: " + componentName);
            synchronized (ManagedApplicationService.this.mLock) {
                if (ManagedApplicationService.this.mConnection != this) {
                    return;
                }
                ManagedApplicationService.this.mHandler.post(new Runnable() {
                    @Override
                    public final void run() {
                        ManagedApplicationService.AnonymousClass1 anonymousClass1 = this.f$0;
                        ManagedApplicationService.this.mEventCb.onServiceEvent(new ManagedApplicationService.LogEvent(jCurrentTimeMillis, ManagedApplicationService.this.mComponent, 1));
                    }
                });
                ManagedApplicationService.this.stopRetriesLocked();
                ManagedApplicationService.this.mBoundInterface = null;
                if (ManagedApplicationService.this.mChecker != null) {
                    ManagedApplicationService.this.mBoundInterface = ManagedApplicationService.this.mChecker.asInterface(iBinder);
                    if (!ManagedApplicationService.this.mChecker.checkType(ManagedApplicationService.this.mBoundInterface)) {
                        ManagedApplicationService.this.mBoundInterface = null;
                        Slog.w(ManagedApplicationService.this.TAG, "Invalid binder from " + componentName);
                        ManagedApplicationService.this.startRetriesLocked();
                        return;
                    }
                    iInterface = ManagedApplicationService.this.mBoundInterface;
                    pendingEvent = ManagedApplicationService.this.mPendingEvent;
                    ManagedApplicationService.this.mPendingEvent = null;
                } else {
                    iInterface = null;
                    pendingEvent = null;
                }
                if (iInterface != null && pendingEvent != null) {
                    try {
                        pendingEvent.runEvent(iInterface);
                    } catch (RemoteException | RuntimeException e) {
                        Slog.e(ManagedApplicationService.this.TAG, "Received exception from user service: ", e);
                        ManagedApplicationService.this.startRetriesLocked();
                    }
                }
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            final long jCurrentTimeMillis = System.currentTimeMillis();
            Slog.w(ManagedApplicationService.this.TAG, "Service disconnected: " + componentName);
            synchronized (ManagedApplicationService.this.mLock) {
                if (ManagedApplicationService.this.mConnection != this) {
                    return;
                }
                ManagedApplicationService.this.mHandler.post(new Runnable() {
                    @Override
                    public final void run() {
                        ManagedApplicationService.AnonymousClass1 anonymousClass1 = this.f$0;
                        ManagedApplicationService.this.mEventCb.onServiceEvent(new ManagedApplicationService.LogEvent(jCurrentTimeMillis, ManagedApplicationService.this.mComponent, 2));
                    }
                });
                ManagedApplicationService.this.mBoundInterface = null;
                ManagedApplicationService.this.startRetriesLocked();
            }
        }
    }

    private boolean matches(ComponentName componentName, int i) {
        return Objects.equals(this.mComponent, componentName) && this.mUserId == i;
    }

    private void startRetriesLocked() {
        if (checkAndDeliverServiceDiedCbLocked()) {
            disconnect();
        } else {
            if (this.mRetrying) {
                return;
            }
            this.mRetrying = true;
            queueRetryLocked();
        }
    }

    private void stopRetriesLocked() {
        this.mRetrying = false;
        this.mHandler.removeCallbacks(this.mRetryRunnable);
    }

    private void queueRetryLocked() {
        long jUptimeMillis = SystemClock.uptimeMillis();
        if (jUptimeMillis - this.mLastRetryTimeMs > RETRY_RESET_TIME_MS) {
            this.mNextRetryDurationMs = MIN_RETRY_DURATION_MS;
            this.mRetryCount = 0;
        }
        this.mLastRetryTimeMs = jUptimeMillis;
        this.mHandler.postDelayed(this.mRetryRunnable, this.mNextRetryDurationMs);
        this.mNextRetryDurationMs = Math.min(2 * this.mNextRetryDurationMs, MAX_RETRY_DURATION_MS);
        this.mRetryCount++;
    }

    private boolean checkAndDeliverServiceDiedCbLocked() {
        if (this.mRetryType == 2 || (this.mRetryType == 3 && this.mRetryCount >= 4)) {
            Slog.e(this.TAG, "Service " + this.mComponent + " has died too much, not retrying.");
            if (this.mEventCb != null) {
                final long jCurrentTimeMillis = System.currentTimeMillis();
                this.mHandler.post(new Runnable() {
                    @Override
                    public final void run() {
                        ManagedApplicationService managedApplicationService = this.f$0;
                        managedApplicationService.mEventCb.onServiceEvent(new ManagedApplicationService.LogEvent(jCurrentTimeMillis, managedApplicationService.mComponent, 4));
                    }
                });
                return true;
            }
            return true;
        }
        return false;
    }

    private void doRetry() {
        synchronized (this.mLock) {
            if (this.mConnection == null) {
                return;
            }
            if (this.mRetrying) {
                Slog.i(this.TAG, "Attempting to reconnect " + this.mComponent + "...");
                disconnect();
                if (checkAndDeliverServiceDiedCbLocked()) {
                    return;
                }
                queueRetryLocked();
                connect();
            }
        }
    }
}
