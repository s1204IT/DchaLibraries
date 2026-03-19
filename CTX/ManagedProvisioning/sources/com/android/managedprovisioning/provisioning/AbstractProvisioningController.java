package com.android.managedprovisioning.provisioning;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.util.Preconditions;
import com.android.managedprovisioning.analytics.ProvisioningAnalyticsTracker;
import com.android.managedprovisioning.common.ProvisionLogger;
import com.android.managedprovisioning.finalization.FinalizationController;
import com.android.managedprovisioning.model.ProvisioningParams;
import com.android.managedprovisioning.task.AbstractProvisioningTask;
import java.util.ArrayList;
import java.util.List;

public abstract class AbstractProvisioningController implements AbstractProvisioningTask.Callback {

    @VisibleForTesting
    static final int MSG_RUN_TASK = 1;
    private final ProvisioningControllerCallback mCallback;
    protected final Context mContext;
    protected int mCurrentTaskIndex;
    private final FinalizationController mFinalizationController;
    protected final ProvisioningParams mParams;
    protected int mUserId;
    private Handler mWorkerHandler;
    private int mStatus = 0;
    private List<AbstractProvisioningTask> mTasks = new ArrayList();
    private final ProvisioningAnalyticsTracker mProvisioningAnalyticsTracker = ProvisioningAnalyticsTracker.getInstance();

    protected abstract int getErrorMsgId(AbstractProvisioningTask abstractProvisioningTask, int i);

    protected abstract int getErrorTitle();

    protected abstract boolean getRequireFactoryReset(AbstractProvisioningTask abstractProvisioningTask, int i);

    protected abstract void performCleanup();

    protected abstract void setUpTasks();

    AbstractProvisioningController(Context context, ProvisioningParams provisioningParams, int i, ProvisioningControllerCallback provisioningControllerCallback, FinalizationController finalizationController) {
        this.mContext = (Context) Preconditions.checkNotNull(context);
        this.mParams = (ProvisioningParams) Preconditions.checkNotNull(provisioningParams);
        this.mUserId = i;
        this.mCallback = (ProvisioningControllerCallback) Preconditions.checkNotNull(provisioningControllerCallback);
        this.mFinalizationController = (FinalizationController) Preconditions.checkNotNull(finalizationController);
        setUpTasks();
    }

    protected synchronized void addTasks(AbstractProvisioningTask... abstractProvisioningTaskArr) {
        for (AbstractProvisioningTask abstractProvisioningTask : abstractProvisioningTaskArr) {
            this.mTasks.add(abstractProvisioningTask);
        }
    }

    public synchronized void start(Looper looper) {
        start(new ProvisioningTaskHandler(looper));
    }

    @VisibleForTesting
    void start(Handler handler) {
        if (this.mStatus != 0) {
            return;
        }
        this.mWorkerHandler = (Handler) Preconditions.checkNotNull(handler);
        this.mStatus = 1;
        runTask(0);
    }

    public synchronized void cancel() {
        if (this.mStatus != 1 && this.mStatus != 2 && this.mStatus != 5 && this.mStatus != 6 && this.mStatus != 4) {
            ProvisionLogger.logd("Cancel called, but status is " + this.mStatus);
            return;
        }
        ProvisionLogger.logd("ProvisioningController: cancelled");
        this.mStatus = 5;
        cleanup(6);
    }

    public synchronized void preFinalize() {
        if (this.mStatus != 2) {
            return;
        }
        this.mStatus = 3;
        this.mFinalizationController.provisioningInitiallyDone(this.mParams);
        this.mCallback.preFinalizationCompleted();
    }

    private void runTask(int i) {
        AbstractProvisioningTask abstractProvisioningTask = this.mTasks.get(i);
        this.mWorkerHandler.sendMessage(this.mWorkerHandler.obtainMessage(1, this.mUserId, 0, abstractProvisioningTask));
        this.mCallback.progressUpdate(abstractProvisioningTask.getStatusMsgId());
    }

    private void tasksCompleted() {
        this.mStatus = 2;
        this.mCurrentTaskIndex = -1;
        this.mCallback.provisioningTasksCompleted();
    }

    @Override
    public synchronized void onSuccess(AbstractProvisioningTask abstractProvisioningTask) {
        if (this.mStatus != 1) {
            return;
        }
        this.mCurrentTaskIndex++;
        if (this.mCurrentTaskIndex == this.mTasks.size()) {
            tasksCompleted();
        } else {
            runTask(this.mCurrentTaskIndex);
        }
    }

    @Override
    public synchronized void onError(AbstractProvisioningTask abstractProvisioningTask, int i) {
        this.mStatus = 4;
        cleanup(4);
        this.mProvisioningAnalyticsTracker.logProvisioningError(this.mContext, abstractProvisioningTask, i);
        this.mCallback.error(getErrorTitle(), getErrorMsgId(abstractProvisioningTask, i), getRequireFactoryReset(abstractProvisioningTask, i));
    }

    private void cleanup(final int i) {
        this.mWorkerHandler.post(new Runnable() {
            @Override
            public final void run() {
                AbstractProvisioningController.lambda$cleanup$0(this.f$0, i);
            }
        });
    }

    public static void lambda$cleanup$0(AbstractProvisioningController abstractProvisioningController, int i) {
        abstractProvisioningController.performCleanup();
        abstractProvisioningController.mStatus = i;
        abstractProvisioningController.mCallback.cleanUpCompleted();
    }

    protected static class ProvisioningTaskHandler extends Handler {
        public ProvisioningTaskHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message message) {
            if (message.what == 1) {
                AbstractProvisioningTask abstractProvisioningTask = (AbstractProvisioningTask) message.obj;
                ProvisionLogger.logd("Running task: " + abstractProvisioningTask.getClass().getSimpleName());
                abstractProvisioningTask.run(message.arg1);
                return;
            }
            ProvisionLogger.loge("Unknown message: " + message.what);
        }
    }
}
