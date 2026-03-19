package com.android.managedprovisioning.ota;

import com.android.managedprovisioning.common.ProvisionLogger;
import com.android.managedprovisioning.task.AbstractProvisioningTask;

public class TaskExecutor implements AbstractProvisioningTask.Callback {
    public synchronized void execute(int i, AbstractProvisioningTask abstractProvisioningTask) {
        abstractProvisioningTask.run(i);
    }

    @Override
    public void onSuccess(AbstractProvisioningTask abstractProvisioningTask) {
        ProvisionLogger.logd("Task ran successfully: " + abstractProvisioningTask.getClass().getSimpleName());
    }

    @Override
    public void onError(AbstractProvisioningTask abstractProvisioningTask, int i) {
        ProvisionLogger.logd("Error running task: " + abstractProvisioningTask.getClass().getSimpleName());
    }
}
