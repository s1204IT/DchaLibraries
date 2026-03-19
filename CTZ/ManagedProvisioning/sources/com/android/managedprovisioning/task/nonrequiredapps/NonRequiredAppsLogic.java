package com.android.managedprovisioning.task.nonrequiredapps;

import android.app.AppGlobals;
import android.app.admin.DevicePolicyManager;
import android.content.Context;
import android.content.pm.IPackageManager;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.util.Preconditions;
import com.android.managedprovisioning.common.IllegalProvisioningArgumentException;
import com.android.managedprovisioning.common.Utils;
import com.android.managedprovisioning.model.ProvisioningParams;
import java.util.Collections;
import java.util.Set;

public class NonRequiredAppsLogic {
    private final Context mContext;
    private final DevicePolicyManager mDevicePolicyManager;
    private final IPackageManager mIPackageManager;
    private final boolean mNewProfile;
    private final ProvisioningParams mParams;
    private final SystemAppsSnapshot mSnapshot;
    private final Utils mUtils;

    public NonRequiredAppsLogic(Context context, boolean z, ProvisioningParams provisioningParams) {
        this(context, AppGlobals.getPackageManager(), (DevicePolicyManager) context.getSystemService("device_policy"), z, provisioningParams, new SystemAppsSnapshot(context), new Utils());
    }

    @VisibleForTesting
    NonRequiredAppsLogic(Context context, IPackageManager iPackageManager, DevicePolicyManager devicePolicyManager, boolean z, ProvisioningParams provisioningParams, SystemAppsSnapshot systemAppsSnapshot, Utils utils) {
        this.mContext = context;
        this.mIPackageManager = (IPackageManager) Preconditions.checkNotNull(iPackageManager);
        this.mDevicePolicyManager = (DevicePolicyManager) Preconditions.checkNotNull(devicePolicyManager);
        this.mNewProfile = z;
        this.mParams = (ProvisioningParams) Preconditions.checkNotNull(provisioningParams);
        this.mSnapshot = (SystemAppsSnapshot) Preconditions.checkNotNull(systemAppsSnapshot);
        this.mUtils = (Utils) Preconditions.checkNotNull(utils);
    }

    public Set<String> getSystemAppsToRemove(int i) {
        if (!shouldDeleteSystemApps(i)) {
            return Collections.emptySet();
        }
        Set<String> currentSystemApps = this.mUtils.getCurrentSystemApps(this.mIPackageManager, i);
        if (!this.mNewProfile) {
            currentSystemApps.removeAll(this.mSnapshot.getSnapshot(i));
        }
        try {
            Set<String> disallowedSystemApps = this.mDevicePolicyManager.getDisallowedSystemApps(this.mParams.inferDeviceAdminComponentName(this.mUtils, this.mContext, i), i, this.mParams.provisioningAction);
            disallowedSystemApps.retainAll(currentSystemApps);
            return disallowedSystemApps;
        } catch (IllegalProvisioningArgumentException e) {
            throw new RuntimeException("Failed to infer device admin component name", e);
        }
    }

    public void maybeTakeSystemAppsSnapshot(int i) {
        if (shouldDeleteSystemApps(i)) {
            this.mSnapshot.takeNewSnapshot(i);
        }
    }

    private boolean shouldDeleteSystemApps(int i) {
        int i2 = getCase(i);
        return 3 == i2 || 1 == i2;
    }

    private int getCase(int i) {
        if (this.mNewProfile) {
            if (this.mParams.leaveAllSystemAppsEnabled) {
                return 2;
            }
            return 3;
        }
        if (this.mSnapshot.hasSnapshot(i)) {
            return 1;
        }
        return 0;
    }
}
