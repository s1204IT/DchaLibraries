package com.android.server.appwidget;

import android.content.Context;
import com.android.server.AppWidgetBackupBridge;
import com.android.server.SystemService;

public class AppWidgetService extends SystemService {
    private final AppWidgetServiceImpl mImpl;

    public AppWidgetService(Context context) {
        super(context);
        this.mImpl = new AppWidgetServiceImpl(context);
    }

    @Override
    public void onStart() {
        publishBinderService("appwidget", this.mImpl);
        AppWidgetBackupBridge.register(this.mImpl);
    }

    @Override
    public void onBootPhase(int phase) {
        if (phase != 600) {
            return;
        }
        this.mImpl.setSafeMode(isSafeMode());
    }

    @Override
    public void onUnlockUser(int userHandle) {
        this.mImpl.onUserUnlocked(userHandle);
    }

    @Override
    public void onStopUser(int userHandle) {
        this.mImpl.onUserStopped(userHandle);
    }

    @Override
    public void onSwitchUser(int userHandle) {
        this.mImpl.reloadWidgetsMaskedStateForGroup(userHandle);
    }
}
