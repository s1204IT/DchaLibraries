package com.android.settings.applications;

import android.content.Context;
import com.android.settings.applications.AppStateAppOpsBridge;
import com.android.settings.applications.AppStateBaseBridge;
import com.android.settingslib.applications.ApplicationsState;

public class AppStateUsageBridge extends AppStateAppOpsBridge {
    private static final String[] PM_PERMISSION = {"android.permission.PACKAGE_USAGE_STATS"};
    public static final ApplicationsState.AppFilter FILTER_APP_USAGE = new ApplicationsState.AppFilter() {
        @Override
        public void init() {
        }

        @Override
        public boolean filterApp(ApplicationsState.AppEntry info) {
            return info.extraInfo != null;
        }
    };

    public AppStateUsageBridge(Context context, ApplicationsState appState, AppStateBaseBridge.Callback callback) {
        super(context, appState, callback, 43, PM_PERMISSION);
    }

    @Override
    protected void updateExtraInfo(ApplicationsState.AppEntry app, String pkg, int uid) {
        app.extraInfo = getUsageInfo(pkg, uid);
    }

    public UsageState getUsageInfo(String pkg, int uid) {
        AppStateAppOpsBridge.PermissionState permissionState = super.getPermissionInfo(pkg, uid);
        return new UsageState(permissionState);
    }

    public static class UsageState extends AppStateAppOpsBridge.PermissionState {
        public UsageState(AppStateAppOpsBridge.PermissionState permissionState) {
            super(permissionState.packageName, permissionState.userHandle);
            this.packageInfo = permissionState.packageInfo;
            this.appOpMode = permissionState.appOpMode;
            this.permissionDeclared = permissionState.permissionDeclared;
            this.staticPermissionGranted = permissionState.staticPermissionGranted;
        }
    }
}
