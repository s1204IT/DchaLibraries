package com.android.managedprovisioning.manageduser;

import android.content.Context;
import com.android.internal.annotations.VisibleForTesting;
import com.android.managedprovisioning.task.nonrequiredapps.SystemAppsSnapshot;

public class ManagedUserCreationController {
    private final boolean mLeaveAllSystemAppsEnabled;
    private final SystemAppsSnapshot mSystemAppsSnapshot;
    private final int mUserId;

    public ManagedUserCreationController(int i, boolean z, Context context) {
        this(i, z, new SystemAppsSnapshot(context));
    }

    @VisibleForTesting
    ManagedUserCreationController(int i, boolean z, SystemAppsSnapshot systemAppsSnapshot) {
        this.mUserId = i;
        this.mLeaveAllSystemAppsEnabled = z;
        this.mSystemAppsSnapshot = systemAppsSnapshot;
    }

    public void run() {
        if (this.mUserId != -10000 && !this.mLeaveAllSystemAppsEnabled) {
            this.mSystemAppsSnapshot.takeNewSnapshot(this.mUserId);
        }
    }
}
