package com.android.managedprovisioning.task;

import android.app.ActivityManager;
import android.app.IActivityManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.RemoteException;
import android.os.UserHandle;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.util.Preconditions;
import com.android.managedprovisioning.R;
import com.android.managedprovisioning.common.ProvisionLogger;
import com.android.managedprovisioning.model.ProvisioningParams;
import com.android.managedprovisioning.task.AbstractProvisioningTask;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

public class StartManagedProfileTask extends AbstractProvisioningTask {

    @VisibleForTesting
    static final IntentFilter UNLOCK_FILTER = new IntentFilter("android.intent.action.USER_UNLOCKED");
    private final IActivityManager mIActivityManager;

    public StartManagedProfileTask(Context context, ProvisioningParams provisioningParams, AbstractProvisioningTask.Callback callback) {
        this(ActivityManager.getService(), context, provisioningParams, callback);
    }

    @VisibleForTesting
    StartManagedProfileTask(IActivityManager iActivityManager, Context context, ProvisioningParams provisioningParams, AbstractProvisioningTask.Callback callback) {
        super(context, provisioningParams, callback);
        this.mIActivityManager = (IActivityManager) Preconditions.checkNotNull(iActivityManager);
    }

    @Override
    public void run(int i) {
        startTaskTimer();
        UserUnlockedReceiver userUnlockedReceiver = new UserUnlockedReceiver(i);
        this.mContext.registerReceiverAsUser(userUnlockedReceiver, new UserHandle(i), UNLOCK_FILTER, null, null);
        try {
            if (!this.mIActivityManager.startUserInBackground(i)) {
                ProvisionLogger.loge("Unable to start user in background: " + i);
                error(0);
                return;
            }
            if (userUnlockedReceiver.waitForUserUnlocked()) {
                this.mContext.unregisterReceiver(userUnlockedReceiver);
                stopTaskTimer();
                success();
            } else {
                ProvisionLogger.loge("Timeout whilst waiting for unlock of user: " + i);
                error(0);
            }
        } catch (RemoteException e) {
            ProvisionLogger.loge("Exception when starting user in background: " + i, e);
            error(0);
        } finally {
            this.mContext.unregisterReceiver(userUnlockedReceiver);
        }
    }

    @Override
    public int getStatusMsgId() {
        return R.string.progress_finishing_touches;
    }

    @Override
    protected int getMetricsCategory() {
        return 621;
    }

    @VisibleForTesting
    static class UserUnlockedReceiver extends BroadcastReceiver {
        private final int mUserId;
        private final Semaphore semaphore = new Semaphore(0);

        UserUnlockedReceiver(int i) {
            this.mUserId = i;
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            if (!"android.intent.action.USER_UNLOCKED".equals(intent.getAction())) {
                ProvisionLogger.logw("Unexpected intent: " + intent);
                return;
            }
            if (intent.getIntExtra("android.intent.extra.user_handle", -10000) == this.mUserId) {
                ProvisionLogger.logd("Received ACTION_USER_UNLOCKED for user " + this.mUserId);
                this.semaphore.release();
            }
        }

        public boolean waitForUserUnlocked() {
            ProvisionLogger.logd("Waiting for ACTION_USER_UNLOCKED");
            try {
                return this.semaphore.tryAcquire(120L, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                return false;
            }
        }
    }
}
