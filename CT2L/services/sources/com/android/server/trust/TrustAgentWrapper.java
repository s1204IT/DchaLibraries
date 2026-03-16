package com.android.server.trust;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.admin.DevicePolicyManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.net.Uri;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.PersistableBundle;
import android.os.RemoteException;
import android.os.SystemClock;
import android.os.UserHandle;
import android.service.trust.ITrustAgentService;
import android.service.trust.ITrustAgentServiceCallback;
import android.util.Log;
import android.util.Slog;
import java.util.List;

public class TrustAgentWrapper {
    private static final String DATA_DURATION = "duration";
    private static final boolean DEBUG = false;
    private static final String EXTRA_COMPONENT_NAME = "componentName";
    private static final int MSG_GRANT_TRUST = 1;
    private static final int MSG_MANAGING_TRUST = 6;
    private static final int MSG_RESTART_TIMEOUT = 4;
    private static final int MSG_REVOKE_TRUST = 2;
    private static final int MSG_SET_TRUST_AGENT_FEATURES_COMPLETED = 5;
    private static final int MSG_TRUST_TIMEOUT = 3;
    private static final String PERMISSION = "android.permission.PROVIDE_TRUST_AGENT";
    private static final long RESTART_TIMEOUT_MILLIS = 300000;
    private static final String TAG = "TrustAgentWrapper";
    private static final String TRUST_EXPIRED_ACTION = "android.server.trust.TRUST_EXPIRED_ACTION";
    private final Intent mAlarmIntent;
    private AlarmManager mAlarmManager;
    private PendingIntent mAlarmPendingIntent;
    private boolean mBound;
    private final Context mContext;
    private boolean mManagingTrust;
    private long mMaximumTimeToLock;
    private CharSequence mMessage;
    private final ComponentName mName;
    private long mScheduledRestartUptimeMillis;
    private IBinder mSetTrustAgentFeaturesToken;
    private ITrustAgentService mTrustAgentService;
    private boolean mTrustDisabledByDpm;
    private final TrustManagerService mTrustManagerService;
    private boolean mTrusted;
    private final int mUserId;
    private final BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            ComponentName component = (ComponentName) intent.getParcelableExtra(TrustAgentWrapper.EXTRA_COMPONENT_NAME);
            if (TrustAgentWrapper.TRUST_EXPIRED_ACTION.equals(intent.getAction()) && TrustAgentWrapper.this.mName.equals(component)) {
                TrustAgentWrapper.this.mHandler.removeMessages(3);
                TrustAgentWrapper.this.mHandler.sendEmptyMessage(3);
            }
        }
    };
    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            String string;
            long duration;
            switch (msg.what) {
                case 1:
                    if (TrustAgentWrapper.this.isConnected()) {
                        TrustAgentWrapper.this.mTrusted = true;
                        TrustAgentWrapper.this.mMessage = (CharSequence) msg.obj;
                        boolean initiatedByUser = msg.arg1 != 0 ? true : TrustAgentWrapper.DEBUG;
                        long durationMs = msg.getData().getLong(TrustAgentWrapper.DATA_DURATION);
                        if (durationMs > 0) {
                            if (TrustAgentWrapper.this.mMaximumTimeToLock != 0) {
                                duration = Math.min(durationMs, TrustAgentWrapper.this.mMaximumTimeToLock);
                            } else {
                                duration = durationMs;
                            }
                            long expiration = SystemClock.elapsedRealtime() + duration;
                            TrustAgentWrapper.this.mAlarmPendingIntent = PendingIntent.getBroadcast(TrustAgentWrapper.this.mContext, 0, TrustAgentWrapper.this.mAlarmIntent, 268435456);
                            TrustAgentWrapper.this.mAlarmManager.set(2, expiration, TrustAgentWrapper.this.mAlarmPendingIntent);
                        }
                        TrustArchive trustArchive = TrustAgentWrapper.this.mTrustManagerService.mArchive;
                        int i = TrustAgentWrapper.this.mUserId;
                        ComponentName componentName = TrustAgentWrapper.this.mName;
                        if (TrustAgentWrapper.this.mMessage != null) {
                            string = TrustAgentWrapper.this.mMessage.toString();
                        } else {
                            string = null;
                        }
                        trustArchive.logGrantTrust(i, componentName, string, durationMs, initiatedByUser);
                        TrustAgentWrapper.this.mTrustManagerService.updateTrust(TrustAgentWrapper.this.mUserId, initiatedByUser);
                        return;
                    }
                    Log.w(TrustAgentWrapper.TAG, "Agent is not connected, cannot grant trust: " + TrustAgentWrapper.this.mName.flattenToShortString());
                    return;
                case 2:
                    break;
                case 3:
                    TrustAgentWrapper.this.mTrustManagerService.mArchive.logTrustTimeout(TrustAgentWrapper.this.mUserId, TrustAgentWrapper.this.mName);
                    TrustAgentWrapper.this.onTrustTimeout();
                    break;
                case 4:
                    TrustAgentWrapper.this.destroy();
                    TrustAgentWrapper.this.mTrustManagerService.resetAgent(TrustAgentWrapper.this.mName, TrustAgentWrapper.this.mUserId);
                    return;
                case 5:
                    IBinder token = (IBinder) msg.obj;
                    boolean result = msg.arg1 != 0 ? true : TrustAgentWrapper.DEBUG;
                    if (TrustAgentWrapper.this.mSetTrustAgentFeaturesToken == token) {
                        TrustAgentWrapper.this.mSetTrustAgentFeaturesToken = null;
                        if (TrustAgentWrapper.this.mTrustDisabledByDpm && result) {
                            TrustAgentWrapper.this.mTrustDisabledByDpm = TrustAgentWrapper.DEBUG;
                            TrustAgentWrapper.this.mTrustManagerService.updateTrust(TrustAgentWrapper.this.mUserId, TrustAgentWrapper.DEBUG);
                            return;
                        }
                        return;
                    }
                    return;
                case 6:
                    TrustAgentWrapper.this.mManagingTrust = msg.arg1 != 0 ? true : TrustAgentWrapper.DEBUG;
                    if (!TrustAgentWrapper.this.mManagingTrust) {
                        TrustAgentWrapper.this.mTrusted = TrustAgentWrapper.DEBUG;
                        TrustAgentWrapper.this.mMessage = null;
                    }
                    TrustAgentWrapper.this.mTrustManagerService.mArchive.logManagingTrust(TrustAgentWrapper.this.mUserId, TrustAgentWrapper.this.mName, TrustAgentWrapper.this.mManagingTrust);
                    TrustAgentWrapper.this.mTrustManagerService.updateTrust(TrustAgentWrapper.this.mUserId, TrustAgentWrapper.DEBUG);
                    return;
                default:
                    return;
            }
            TrustAgentWrapper.this.mTrusted = TrustAgentWrapper.DEBUG;
            TrustAgentWrapper.this.mMessage = null;
            TrustAgentWrapper.this.mHandler.removeMessages(3);
            if (msg.what == 2) {
                TrustAgentWrapper.this.mTrustManagerService.mArchive.logRevokeTrust(TrustAgentWrapper.this.mUserId, TrustAgentWrapper.this.mName);
            }
            TrustAgentWrapper.this.mTrustManagerService.updateTrust(TrustAgentWrapper.this.mUserId, TrustAgentWrapper.DEBUG);
        }
    };
    private ITrustAgentServiceCallback mCallback = new ITrustAgentServiceCallback.Stub() {
        public void grantTrust(CharSequence userMessage, long durationMs, boolean initiatedByUser) {
            Message msg = TrustAgentWrapper.this.mHandler.obtainMessage(1, initiatedByUser ? 1 : 0, 0, userMessage);
            msg.getData().putLong(TrustAgentWrapper.DATA_DURATION, durationMs);
            msg.sendToTarget();
        }

        public void revokeTrust() {
            TrustAgentWrapper.this.mHandler.sendEmptyMessage(2);
        }

        public void setManagingTrust(boolean managingTrust) {
            TrustAgentWrapper.this.mHandler.obtainMessage(6, managingTrust ? 1 : 0, 0).sendToTarget();
        }

        public void onConfigureCompleted(boolean result, IBinder token) {
            TrustAgentWrapper.this.mHandler.obtainMessage(5, result ? 1 : 0, 0, token).sendToTarget();
        }
    };
    private final ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            TrustAgentWrapper.this.mHandler.removeMessages(4);
            TrustAgentWrapper.this.mTrustAgentService = ITrustAgentService.Stub.asInterface(service);
            TrustAgentWrapper.this.mTrustManagerService.mArchive.logAgentConnected(TrustAgentWrapper.this.mUserId, name);
            TrustAgentWrapper.this.setCallback(TrustAgentWrapper.this.mCallback);
            TrustAgentWrapper.this.updateDevicePolicyFeatures();
            if (TrustAgentWrapper.this.mTrustManagerService.isDeviceLockedInner(TrustAgentWrapper.this.mUserId)) {
                TrustAgentWrapper.this.onDeviceLocked();
            } else {
                TrustAgentWrapper.this.onDeviceUnlocked();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            TrustAgentWrapper.this.mTrustAgentService = null;
            TrustAgentWrapper.this.mManagingTrust = TrustAgentWrapper.DEBUG;
            TrustAgentWrapper.this.mSetTrustAgentFeaturesToken = null;
            TrustAgentWrapper.this.mTrustManagerService.mArchive.logAgentDied(TrustAgentWrapper.this.mUserId, name);
            TrustAgentWrapper.this.mHandler.sendEmptyMessage(2);
            if (TrustAgentWrapper.this.mBound) {
                TrustAgentWrapper.this.scheduleRestart();
            }
        }
    };

    public TrustAgentWrapper(Context context, TrustManagerService trustManagerService, Intent intent, UserHandle user) {
        this.mContext = context;
        this.mTrustManagerService = trustManagerService;
        this.mAlarmManager = (AlarmManager) this.mContext.getSystemService("alarm");
        this.mUserId = user.getIdentifier();
        this.mName = intent.getComponent();
        this.mAlarmIntent = new Intent(TRUST_EXPIRED_ACTION).putExtra(EXTRA_COMPONENT_NAME, this.mName);
        this.mAlarmIntent.setData(Uri.parse(this.mAlarmIntent.toUri(1)));
        this.mAlarmIntent.setPackage(context.getPackageName());
        IntentFilter alarmFilter = new IntentFilter(TRUST_EXPIRED_ACTION);
        alarmFilter.addDataScheme(this.mAlarmIntent.getScheme());
        String pathUri = this.mAlarmIntent.toUri(1);
        alarmFilter.addDataPath(pathUri, 0);
        this.mContext.registerReceiver(this.mBroadcastReceiver, alarmFilter, PERMISSION, null);
        scheduleRestart();
        this.mBound = context.bindServiceAsUser(intent, this.mConnection, 1, user);
        if (!this.mBound) {
            Log.e(TAG, "Can't bind to TrustAgent " + this.mName.flattenToShortString());
        }
    }

    private void onError(Exception e) {
        Slog.w(TAG, "Remote Exception", e);
    }

    private void onTrustTimeout() {
        try {
            if (this.mTrustAgentService != null) {
                this.mTrustAgentService.onTrustTimeout();
            }
        } catch (RemoteException e) {
            onError(e);
        }
    }

    public void onUnlockAttempt(boolean successful) {
        try {
            if (this.mTrustAgentService != null) {
                this.mTrustAgentService.onUnlockAttempt(successful);
            }
        } catch (RemoteException e) {
            onError(e);
        }
    }

    public void onDeviceLocked() {
        try {
            if (this.mTrustAgentService != null) {
                this.mTrustAgentService.onDeviceLocked();
            }
        } catch (RemoteException e) {
            onError(e);
        }
    }

    public void onDeviceUnlocked() {
        try {
            if (this.mTrustAgentService != null) {
                this.mTrustAgentService.onDeviceUnlocked();
            }
        } catch (RemoteException e) {
            onError(e);
        }
    }

    private void setCallback(ITrustAgentServiceCallback callback) {
        try {
            if (this.mTrustAgentService != null) {
                this.mTrustAgentService.setCallback(callback);
            }
        } catch (RemoteException e) {
            onError(e);
        }
    }

    boolean updateDevicePolicyFeatures() {
        boolean trustDisabled = DEBUG;
        try {
            if (this.mTrustAgentService != null) {
                DevicePolicyManager dpm = (DevicePolicyManager) this.mContext.getSystemService("device_policy");
                if ((dpm.getKeyguardDisabledFeatures(null, this.mUserId) & 16) != 0) {
                    List<PersistableBundle> config = dpm.getTrustAgentConfiguration(null, this.mName, this.mUserId);
                    trustDisabled = true;
                    if (config != null && config.size() > 0) {
                        this.mSetTrustAgentFeaturesToken = new Binder();
                        this.mTrustAgentService.onConfigure(config, this.mSetTrustAgentFeaturesToken);
                    }
                }
                long maxTimeToLock = dpm.getMaximumTimeToLock(null);
                if (maxTimeToLock != this.mMaximumTimeToLock) {
                    this.mMaximumTimeToLock = maxTimeToLock;
                    if (this.mAlarmPendingIntent != null) {
                        this.mAlarmManager.cancel(this.mAlarmPendingIntent);
                        this.mAlarmPendingIntent = null;
                        this.mHandler.sendEmptyMessage(3);
                    }
                }
            }
        } catch (RemoteException e) {
            onError(e);
        }
        if (this.mTrustDisabledByDpm != trustDisabled) {
            this.mTrustDisabledByDpm = trustDisabled;
            this.mTrustManagerService.updateTrust(this.mUserId, DEBUG);
        }
        return trustDisabled;
    }

    public boolean isTrusted() {
        if (this.mTrusted && this.mManagingTrust && !this.mTrustDisabledByDpm) {
            return true;
        }
        return DEBUG;
    }

    public boolean isManagingTrust() {
        if (!this.mManagingTrust || this.mTrustDisabledByDpm) {
            return DEBUG;
        }
        return true;
    }

    public CharSequence getMessage() {
        return this.mMessage;
    }

    public void destroy() {
        this.mHandler.removeMessages(4);
        if (this.mBound) {
            this.mTrustManagerService.mArchive.logAgentStopped(this.mUserId, this.mName);
            this.mContext.unbindService(this.mConnection);
            this.mBound = DEBUG;
            this.mTrustAgentService = null;
            this.mSetTrustAgentFeaturesToken = null;
            this.mHandler.sendEmptyMessage(2);
        }
    }

    public boolean isConnected() {
        if (this.mTrustAgentService != null) {
            return true;
        }
        return DEBUG;
    }

    public boolean isBound() {
        return this.mBound;
    }

    public long getScheduledRestartUptimeMillis() {
        return this.mScheduledRestartUptimeMillis;
    }

    private void scheduleRestart() {
        this.mHandler.removeMessages(4);
        this.mScheduledRestartUptimeMillis = SystemClock.uptimeMillis() + RESTART_TIMEOUT_MILLIS;
        this.mHandler.sendEmptyMessageAtTime(4, this.mScheduledRestartUptimeMillis);
    }
}
