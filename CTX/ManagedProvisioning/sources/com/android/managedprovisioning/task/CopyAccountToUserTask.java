package com.android.managedprovisioning.task;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.content.Context;
import android.os.UserHandle;
import com.android.internal.annotations.VisibleForTesting;
import com.android.managedprovisioning.R;
import com.android.managedprovisioning.analytics.ProvisioningAnalyticsTracker;
import com.android.managedprovisioning.common.ProvisionLogger;
import com.android.managedprovisioning.model.ProvisioningParams;
import com.android.managedprovisioning.task.AbstractProvisioningTask;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class CopyAccountToUserTask extends AbstractProvisioningTask {
    private final ProvisioningAnalyticsTracker mProvisioningAnalyticsTracker;
    private final int mSourceUserId;

    public CopyAccountToUserTask(int i, Context context, ProvisioningParams provisioningParams, AbstractProvisioningTask.Callback callback) {
        super(context, provisioningParams, callback);
        this.mSourceUserId = i;
        this.mProvisioningAnalyticsTracker = ProvisioningAnalyticsTracker.getInstance();
    }

    @Override
    public void run(int i) {
        startTaskTimer();
        if (maybeCopyAccount(i)) {
            stopTaskTimer();
        }
        success();
    }

    @Override
    public int getStatusMsgId() {
        return R.string.progress_finishing_touches;
    }

    @Override
    protected int getMetricsCategory() {
        return 619;
    }

    @VisibleForTesting
    boolean maybeCopyAccount(int i) {
        Account account = this.mProvisioningParams.accountToMigrate;
        UserHandle userHandleOf = UserHandle.of(this.mSourceUserId);
        UserHandle userHandleOf2 = UserHandle.of(i);
        if (account == null) {
            ProvisionLogger.logd("No account to migrate.");
            return false;
        }
        if (userHandleOf.equals(userHandleOf2)) {
            ProvisionLogger.loge("sourceUser and targetUser are the same, won't migrate account.");
            return false;
        }
        ProvisionLogger.logd("Attempting to copy account from " + userHandleOf + " to " + userHandleOf2);
        try {
        } catch (AuthenticatorException | IOException e) {
            this.mProvisioningAnalyticsTracker.logCopyAccountStatus(this.mContext, 4);
            ProvisionLogger.loge("Exception copying account to " + userHandleOf2, e);
        } catch (OperationCanceledException e2) {
            this.mProvisioningAnalyticsTracker.logCopyAccountStatus(this.mContext, 3);
            ProvisionLogger.loge("Exception copying account to " + userHandleOf2, e2);
        }
        if (((Boolean) ((AccountManager) this.mContext.getSystemService("account")).copyAccountToUser(account, userHandleOf, userHandleOf2, null, null).getResult(180L, TimeUnit.SECONDS)).booleanValue()) {
            ProvisionLogger.logi("Copied account to " + userHandleOf2);
            this.mProvisioningAnalyticsTracker.logCopyAccountStatus(this.mContext, 1);
            return true;
        }
        this.mProvisioningAnalyticsTracker.logCopyAccountStatus(this.mContext, 2);
        ProvisionLogger.loge("Could not copy account to " + userHandleOf2);
        return false;
    }
}
