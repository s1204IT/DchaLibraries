package com.android.managedprovisioning.task;

import android.content.Context;
import com.android.internal.util.Preconditions;
import com.android.managedprovisioning.analytics.TimeLogger;
import com.android.managedprovisioning.model.ProvisioningParams;

public abstract class AbstractProvisioningTask {
    private final Callback mCallback;
    protected final Context mContext;
    protected final ProvisioningParams mProvisioningParams;
    private TimeLogger mTimeLogger;

    public interface Callback {
        void onError(AbstractProvisioningTask abstractProvisioningTask, int i);

        void onSuccess(AbstractProvisioningTask abstractProvisioningTask);
    }

    public abstract int getStatusMsgId();

    public abstract void run(int i);

    AbstractProvisioningTask(Context context, ProvisioningParams provisioningParams, Callback callback) {
        this.mContext = (Context) Preconditions.checkNotNull(context);
        this.mProvisioningParams = provisioningParams;
        this.mCallback = (Callback) Preconditions.checkNotNull(callback);
        this.mTimeLogger = new TimeLogger(context, getMetricsCategory());
    }

    protected final void success() {
        this.mCallback.onSuccess(this);
    }

    protected final void error(int i) {
        this.mCallback.onError(this, i);
    }

    protected void startTaskTimer() {
        this.mTimeLogger.start();
    }

    protected void stopTaskTimer() {
        this.mTimeLogger.stop();
    }

    protected int getMetricsCategory() {
        return 0;
    }
}
