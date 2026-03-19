package com.android.server.wm;

import android.app.ActivityManager;
import com.android.server.policy.WindowManagerPolicy;

class SnapshotStartingData extends StartingData {
    private final WindowManagerService mService;
    private final ActivityManager.TaskSnapshot mSnapshot;

    SnapshotStartingData(WindowManagerService windowManagerService, ActivityManager.TaskSnapshot taskSnapshot) {
        super(windowManagerService);
        this.mService = windowManagerService;
        this.mSnapshot = taskSnapshot;
    }

    @Override
    WindowManagerPolicy.StartingSurface createStartingSurface(AppWindowToken appWindowToken) {
        return this.mService.mTaskSnapshotController.createStartingSurface(appWindowToken, this.mSnapshot);
    }
}
