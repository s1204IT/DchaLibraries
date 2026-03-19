package com.android.server.locksettings;

import android.app.AlarmManager;
import android.app.admin.DevicePolicyManager;
import android.app.trust.IStrongAuthTracker;
import android.content.Context;
import android.hardware.fingerprint.FingerprintManager;
import android.os.Handler;
import android.os.Message;
import android.os.RemoteCallbackList;
import android.os.RemoteException;
import android.os.SystemClock;
import android.util.ArrayMap;
import android.util.Slog;
import android.util.SparseIntArray;
import com.android.internal.widget.LockPatternUtils;

public class LockSettingsStrongAuth {
    private static final int MSG_REGISTER_TRACKER = 2;
    private static final int MSG_REMOVE_USER = 4;
    private static final int MSG_REQUIRE_STRONG_AUTH = 1;
    private static final int MSG_SCHEDULE_STRONG_AUTH_TIMEOUT = 5;
    private static final int MSG_UNREGISTER_TRACKER = 3;
    private static final String STRONG_AUTH_TIMEOUT_ALARM_TAG = "LockSettingsStrongAuth.timeoutForUser";
    private static final String TAG = "LockSettings";
    private AlarmManager mAlarmManager;
    private final Context mContext;
    private final int mDefaultStrongAuthFlags;
    private FingerprintManager mFingerprintManager;
    private final RemoteCallbackList<IStrongAuthTracker> mTrackers = new RemoteCallbackList<>();
    private final SparseIntArray mStrongAuthForUser = new SparseIntArray();
    private final ArrayMap<Integer, StrongAuthTimeoutAlarmListener> mStrongAuthTimeoutAlarmListenerForUser = new ArrayMap<>();
    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message message) {
            switch (message.what) {
                case 1:
                    LockSettingsStrongAuth.this.handleRequireStrongAuth(message.arg1, message.arg2);
                    break;
                case 2:
                    LockSettingsStrongAuth.this.handleAddStrongAuthTracker((IStrongAuthTracker) message.obj);
                    break;
                case 3:
                    LockSettingsStrongAuth.this.handleRemoveStrongAuthTracker((IStrongAuthTracker) message.obj);
                    break;
                case 4:
                    LockSettingsStrongAuth.this.handleRemoveUser(message.arg1);
                    break;
                case 5:
                    LockSettingsStrongAuth.this.handleScheduleStrongAuthTimeout(message.arg1);
                    break;
            }
        }
    };

    public LockSettingsStrongAuth(Context context) {
        this.mContext = context;
        this.mDefaultStrongAuthFlags = LockPatternUtils.StrongAuthTracker.getDefaultFlags(context);
        this.mAlarmManager = (AlarmManager) context.getSystemService(AlarmManager.class);
    }

    public void systemReady() {
        if (this.mContext.getPackageManager().hasSystemFeature("android.hardware.fingerprint")) {
            this.mFingerprintManager = (FingerprintManager) this.mContext.getSystemService(FingerprintManager.class);
        }
    }

    private void handleAddStrongAuthTracker(IStrongAuthTracker iStrongAuthTracker) {
        this.mTrackers.register(iStrongAuthTracker);
        for (int i = 0; i < this.mStrongAuthForUser.size(); i++) {
            try {
                iStrongAuthTracker.onStrongAuthRequiredChanged(this.mStrongAuthForUser.valueAt(i), this.mStrongAuthForUser.keyAt(i));
            } catch (RemoteException e) {
                Slog.e(TAG, "Exception while adding StrongAuthTracker.", e);
            }
        }
    }

    private void handleRemoveStrongAuthTracker(IStrongAuthTracker iStrongAuthTracker) {
        this.mTrackers.unregister(iStrongAuthTracker);
    }

    private void handleRequireStrongAuth(int i, int i2) {
        if (i2 == -1) {
            for (int i3 = 0; i3 < this.mStrongAuthForUser.size(); i3++) {
                handleRequireStrongAuthOneUser(i, this.mStrongAuthForUser.keyAt(i3));
            }
            return;
        }
        handleRequireStrongAuthOneUser(i, i2);
    }

    private void handleRequireStrongAuthOneUser(int i, int i2) {
        int i3;
        int i4 = this.mStrongAuthForUser.get(i2, this.mDefaultStrongAuthFlags);
        if (i == 0) {
            i3 = 0;
        } else {
            i3 = i | i4;
        }
        if (i4 != i3) {
            this.mStrongAuthForUser.put(i2, i3);
            notifyStrongAuthTrackers(i3, i2);
        }
    }

    private void handleRemoveUser(int i) {
        int iIndexOfKey = this.mStrongAuthForUser.indexOfKey(i);
        if (iIndexOfKey >= 0) {
            this.mStrongAuthForUser.removeAt(iIndexOfKey);
            notifyStrongAuthTrackers(this.mDefaultStrongAuthFlags, i);
        }
    }

    private void handleScheduleStrongAuthTimeout(int i) {
        long jElapsedRealtime = SystemClock.elapsedRealtime() + ((DevicePolicyManager) this.mContext.getSystemService("device_policy")).getRequiredStrongAuthTimeout(null, i);
        StrongAuthTimeoutAlarmListener strongAuthTimeoutAlarmListener = this.mStrongAuthTimeoutAlarmListenerForUser.get(Integer.valueOf(i));
        if (strongAuthTimeoutAlarmListener != null) {
            this.mAlarmManager.cancel(strongAuthTimeoutAlarmListener);
        } else {
            strongAuthTimeoutAlarmListener = new StrongAuthTimeoutAlarmListener(i);
            this.mStrongAuthTimeoutAlarmListenerForUser.put(Integer.valueOf(i), strongAuthTimeoutAlarmListener);
        }
        this.mAlarmManager.set(3, jElapsedRealtime, STRONG_AUTH_TIMEOUT_ALARM_TAG, strongAuthTimeoutAlarmListener, this.mHandler);
    }

    private void notifyStrongAuthTrackers(int i, int i2) {
        int iBeginBroadcast = this.mTrackers.beginBroadcast();
        while (iBeginBroadcast > 0) {
            iBeginBroadcast--;
            try {
                try {
                    this.mTrackers.getBroadcastItem(iBeginBroadcast).onStrongAuthRequiredChanged(i, i2);
                } catch (RemoteException e) {
                    Slog.e(TAG, "Exception while notifying StrongAuthTracker.", e);
                }
            } finally {
                this.mTrackers.finishBroadcast();
            }
        }
    }

    public void registerStrongAuthTracker(IStrongAuthTracker iStrongAuthTracker) {
        this.mHandler.obtainMessage(2, iStrongAuthTracker).sendToTarget();
    }

    public void unregisterStrongAuthTracker(IStrongAuthTracker iStrongAuthTracker) {
        this.mHandler.obtainMessage(3, iStrongAuthTracker).sendToTarget();
    }

    public void removeUser(int i) {
        this.mHandler.obtainMessage(4, i, 0).sendToTarget();
    }

    public void requireStrongAuth(int i, int i2) {
        if (i2 == -1 || i2 >= 0) {
            this.mHandler.obtainMessage(1, i, i2).sendToTarget();
            return;
        }
        throw new IllegalArgumentException("userId must be an explicit user id or USER_ALL");
    }

    public void reportUnlock(int i) {
        requireStrongAuth(0, i);
    }

    public void reportSuccessfulStrongAuthUnlock(int i) {
        if (this.mFingerprintManager != null) {
            this.mFingerprintManager.resetTimeout(null);
        }
        this.mHandler.obtainMessage(5, i, 0).sendToTarget();
    }

    private class StrongAuthTimeoutAlarmListener implements AlarmManager.OnAlarmListener {
        private final int mUserId;

        public StrongAuthTimeoutAlarmListener(int i) {
            this.mUserId = i;
        }

        @Override
        public void onAlarm() {
            LockSettingsStrongAuth.this.requireStrongAuth(16, this.mUserId);
        }
    }
}
