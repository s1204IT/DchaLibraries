package com.android.settings.applications;

import com.android.settings.applications.AppStateBaseBridge;
import com.android.settings.fuelgauge.PowerWhitelistBackend;
import com.android.settingslib.applications.ApplicationsState;
import java.util.ArrayList;

public class AppStatePowerBridge extends AppStateBaseBridge {
    public static final ApplicationsState.AppFilter FILTER_POWER_WHITELISTED = new ApplicationsState.CompoundFilter(ApplicationsState.FILTER_WITHOUT_DISABLED_UNTIL_USED, new ApplicationsState.AppFilter() {
        @Override
        public void init() {
        }

        @Override
        public boolean filterApp(ApplicationsState.AppEntry info) {
            return info.extraInfo == Boolean.TRUE;
        }
    });
    private final PowerWhitelistBackend mBackend;

    public static class HighPowerState {
    }

    public AppStatePowerBridge(ApplicationsState appState, AppStateBaseBridge.Callback callback) {
        super(appState, callback);
        this.mBackend = PowerWhitelistBackend.getInstance();
    }

    @Override
    protected void loadAllExtraInfo() {
        ArrayList<ApplicationsState.AppEntry> apps = this.mAppSession.getAllApps();
        int N = apps.size();
        for (int i = 0; i < N; i++) {
            ApplicationsState.AppEntry app = apps.get(i);
            app.extraInfo = this.mBackend.isWhitelisted(app.info.packageName) ? Boolean.TRUE : Boolean.FALSE;
        }
    }

    @Override
    protected void updateExtraInfo(ApplicationsState.AppEntry app, String pkg, int uid) {
        app.extraInfo = this.mBackend.isWhitelisted(pkg) ? Boolean.TRUE : Boolean.FALSE;
    }
}
