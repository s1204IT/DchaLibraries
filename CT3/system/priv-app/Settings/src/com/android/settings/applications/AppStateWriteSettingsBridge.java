package com.android.settings.applications;

import android.content.Context;
import com.android.settings.applications.AppStateAppOpsBridge;
import com.android.settings.applications.AppStateBaseBridge;
import com.android.settingslib.applications.ApplicationsState;
/* loaded from: classes.dex */
public class AppStateWriteSettingsBridge extends AppStateAppOpsBridge {
    private static final String[] PM_PERMISSIONS = {"android.permission.WRITE_SETTINGS"};
    public static final ApplicationsState.AppFilter FILTER_WRITE_SETTINGS = new ApplicationsState.AppFilter() { // from class: com.android.settings.applications.AppStateWriteSettingsBridge.1
        @Override // com.android.settingslib.applications.ApplicationsState.AppFilter
        public void init() {
        }

        @Override // com.android.settingslib.applications.ApplicationsState.AppFilter
        public boolean filterApp(ApplicationsState.AppEntry info) {
            return info.extraInfo != null;
        }
    };

    public AppStateWriteSettingsBridge(Context context, ApplicationsState appState, AppStateBaseBridge.Callback callback) {
        super(context, appState, callback, 23, PM_PERMISSIONS);
    }

    @Override // com.android.settings.applications.AppStateAppOpsBridge, com.android.settings.applications.AppStateBaseBridge
    protected void updateExtraInfo(ApplicationsState.AppEntry app, String pkg, int uid) {
        app.extraInfo = getWriteSettingsInfo(pkg, uid);
    }

    public WriteSettingsState getWriteSettingsInfo(String pkg, int uid) {
        AppStateAppOpsBridge.PermissionState permissionState = super.getPermissionInfo(pkg, uid);
        return new WriteSettingsState(permissionState);
    }

    /* loaded from: classes.dex */
    public static class WriteSettingsState extends AppStateAppOpsBridge.PermissionState {
        public WriteSettingsState(AppStateAppOpsBridge.PermissionState permissionState) {
            super(permissionState.packageName, permissionState.userHandle);
            this.packageInfo = permissionState.packageInfo;
            this.appOpMode = permissionState.appOpMode;
            this.permissionDeclared = permissionState.permissionDeclared;
            this.staticPermissionGranted = permissionState.staticPermissionGranted;
        }
    }
}
