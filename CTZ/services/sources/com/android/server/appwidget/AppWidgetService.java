package com.android.server.appwidget;

import android.content.Context;
import com.android.server.AppWidgetBackupBridge;
import com.android.server.FgThread;
import com.android.server.SystemService;

public class AppWidgetService extends SystemService {
    private final AppWidgetServiceImpl mImpl;

    public AppWidgetService(Context context) {
        super(context);
        this.mImpl = new AppWidgetServiceImpl(context);
    }

    @Override
    public void onStart() {
        this.mImpl.onStart();
        publishBinderService("appwidget", this.mImpl);
        AppWidgetBackupBridge.register(this.mImpl);
    }

    @Override
    public void onBootPhase(int i) {
        if (i == 550) {
            this.mImpl.setSafeMode(isSafeMode());
        }
    }

    @Override
    public void onUnlockUser(final int i) {
        FgThread.getHandler().post(new Runnable() {
            @Override
            public final void run() {
                this.f$0.mImpl.onUserUnlocked(i);
            }
        });
    }

    @Override
    public void onStopUser(int i) {
        this.mImpl.onUserStopped(i);
    }

    @Override
    public void onSwitchUser(int i) {
        this.mImpl.reloadWidgetsMaskedStateForGroup(i);
    }
}
