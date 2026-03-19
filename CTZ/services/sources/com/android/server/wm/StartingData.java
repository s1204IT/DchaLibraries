package com.android.server.wm;

import com.android.server.policy.WindowManagerPolicy;

public abstract class StartingData {
    protected final WindowManagerService mService;

    abstract WindowManagerPolicy.StartingSurface createStartingSurface(AppWindowToken appWindowToken);

    protected StartingData(WindowManagerService windowManagerService) {
        this.mService = windowManagerService;
    }
}
