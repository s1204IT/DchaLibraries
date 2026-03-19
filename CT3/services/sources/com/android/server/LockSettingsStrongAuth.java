package com.android.server;

import android.app.trust.IStrongAuthTracker;
import android.content.Context;
import android.os.DeadObjectException;
import android.os.Handler;
import android.os.Message;
import android.os.RemoteException;
import android.util.Slog;
import android.util.SparseIntArray;
import com.android.internal.widget.LockPatternUtils;
import java.util.ArrayList;

public class LockSettingsStrongAuth {
    private static final int MSG_REGISTER_TRACKER = 2;
    private static final int MSG_REMOVE_USER = 4;
    private static final int MSG_REQUIRE_STRONG_AUTH = 1;
    private static final int MSG_UNREGISTER_TRACKER = 3;
    private static final String TAG = "LockSettings";
    private final int mDefaultStrongAuthFlags;
    private final ArrayList<IStrongAuthTracker> mStrongAuthTrackers = new ArrayList<>();
    private final SparseIntArray mStrongAuthForUser = new SparseIntArray();
    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 1:
                    LockSettingsStrongAuth.this.handleRequireStrongAuth(msg.arg1, msg.arg2);
                    break;
                case 2:
                    LockSettingsStrongAuth.this.handleAddStrongAuthTracker((IStrongAuthTracker) msg.obj);
                    break;
                case 3:
                    LockSettingsStrongAuth.this.handleRemoveStrongAuthTracker((IStrongAuthTracker) msg.obj);
                    break;
                case 4:
                    LockSettingsStrongAuth.this.handleRemoveUser(msg.arg1);
                    break;
            }
        }
    };

    public LockSettingsStrongAuth(Context context) {
        this.mDefaultStrongAuthFlags = LockPatternUtils.StrongAuthTracker.getDefaultFlags(context);
    }

    private void handleAddStrongAuthTracker(IStrongAuthTracker tracker) {
        for (int i = 0; i < this.mStrongAuthTrackers.size(); i++) {
            if (this.mStrongAuthTrackers.get(i).asBinder() == tracker.asBinder()) {
                return;
            }
        }
        this.mStrongAuthTrackers.add(tracker);
        for (int i2 = 0; i2 < this.mStrongAuthForUser.size(); i2++) {
            int key = this.mStrongAuthForUser.keyAt(i2);
            int value = this.mStrongAuthForUser.valueAt(i2);
            try {
                tracker.onStrongAuthRequiredChanged(value, key);
            } catch (RemoteException e) {
                Slog.e(TAG, "Exception while adding StrongAuthTracker.", e);
            }
        }
    }

    private void handleRemoveStrongAuthTracker(IStrongAuthTracker tracker) {
        for (int i = 0; i < this.mStrongAuthTrackers.size(); i++) {
            if (this.mStrongAuthTrackers.get(i).asBinder() == tracker.asBinder()) {
                this.mStrongAuthTrackers.remove(i);
                return;
            }
        }
    }

    private void handleRequireStrongAuth(int strongAuthReason, int userId) {
        if (userId == -1) {
            for (int i = 0; i < this.mStrongAuthForUser.size(); i++) {
                int key = this.mStrongAuthForUser.keyAt(i);
                handleRequireStrongAuthOneUser(strongAuthReason, key);
            }
            return;
        }
        handleRequireStrongAuthOneUser(strongAuthReason, userId);
    }

    private void handleRequireStrongAuthOneUser(int strongAuthReason, int userId) {
        int oldValue = this.mStrongAuthForUser.get(userId, this.mDefaultStrongAuthFlags);
        int newValue = strongAuthReason != 0 ? oldValue | strongAuthReason : 0;
        if (oldValue == newValue) {
            return;
        }
        this.mStrongAuthForUser.put(userId, newValue);
        notifyStrongAuthTrackers(newValue, userId);
    }

    private void handleRemoveUser(int userId) {
        int index = this.mStrongAuthForUser.indexOfKey(userId);
        if (index < 0) {
            return;
        }
        this.mStrongAuthForUser.removeAt(index);
        notifyStrongAuthTrackers(this.mDefaultStrongAuthFlags, userId);
    }

    private void notifyStrongAuthTrackers(int strongAuthReason, int userId) {
        int i = 0;
        while (i < this.mStrongAuthTrackers.size()) {
            try {
                this.mStrongAuthTrackers.get(i).onStrongAuthRequiredChanged(strongAuthReason, userId);
            } catch (DeadObjectException e) {
                Slog.d(TAG, "Removing dead StrongAuthTracker.");
                this.mStrongAuthTrackers.remove(i);
                i--;
            } catch (RemoteException e2) {
                Slog.e(TAG, "Exception while notifying StrongAuthTracker.", e2);
            }
            i++;
        }
    }

    public void registerStrongAuthTracker(IStrongAuthTracker tracker) {
        this.mHandler.obtainMessage(2, tracker).sendToTarget();
    }

    public void unregisterStrongAuthTracker(IStrongAuthTracker tracker) {
        this.mHandler.obtainMessage(3, tracker).sendToTarget();
    }

    public void removeUser(int userId) {
        this.mHandler.obtainMessage(4, userId, 0).sendToTarget();
    }

    public void requireStrongAuth(int strongAuthReason, int userId) {
        if (userId == -1 || userId >= 0) {
            this.mHandler.obtainMessage(1, strongAuthReason, userId).sendToTarget();
            return;
        }
        throw new IllegalArgumentException("userId must be an explicit user id or USER_ALL");
    }

    public void reportUnlock(int userId) {
        requireStrongAuth(0, userId);
    }
}
