package com.android.managedprovisioning.provisioning;

public interface ProvisioningControllerCallback extends ProvisioningManagerCallback {
    void cleanUpCompleted();

    void provisioningTasksCompleted();
}
