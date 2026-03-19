package com.android.managedprovisioning.provisioning;

public interface ProvisioningManagerCallback {
    void error(int i, int i2, boolean z);

    void preFinalizationCompleted();

    void progressUpdate(int i);
}
