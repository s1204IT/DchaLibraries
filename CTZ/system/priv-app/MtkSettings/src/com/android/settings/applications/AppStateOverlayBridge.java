package com.android.settings.applications;

import android.content.Context;
import com.android.settings.applications.AppStateAppOpsBridge;
import com.android.settings.applications.AppStateBaseBridge;
import com.android.settingslib.applications.ApplicationsState;
import java.util.ArrayList;
import java.util.List;

/* loaded from: classes.dex */
public class AppStateOverlayBridge extends AppStateAppOpsBridge {
    private static final String[] PM_PERMISSION = {"android.permission.SYSTEM_ALERT_WINDOW"};
    public static final ApplicationsState.AppFilter FILTER_SYSTEM_ALERT_WINDOW = new ApplicationsState.AppFilter() { // from class: com.android.settings.applications.AppStateOverlayBridge.1
        @Override // com.android.settingslib.applications.ApplicationsState.AppFilter
        public void init() {
        }

        @Override // com.android.settingslib.applications.ApplicationsState.AppFilter
        public boolean filterApp(ApplicationsState.AppEntry appEntry) {
            return appEntry.extraInfo != null;
        }
    };

    public AppStateOverlayBridge(Context context, ApplicationsState applicationsState, AppStateBaseBridge.Callback callback) {
        super(context, applicationsState, callback, 24, PM_PERMISSION);
    }

    @Override // com.android.settings.applications.AppStateAppOpsBridge, com.android.settings.applications.AppStateBaseBridge
    protected void updateExtraInfo(ApplicationsState.AppEntry appEntry, String str, int i) {
        appEntry.extraInfo = getOverlayInfo(str, i);
    }

    public OverlayState getOverlayInfo(String str, int i) {
        return new OverlayState(super.getPermissionInfo(str, i));
    }

    public static class OverlayState extends AppStateAppOpsBridge.PermissionState {
        private static final List<String> DISABLE_PACKAGE_LIST = new ArrayList();
        public final boolean controlEnabled;

        static {
            DISABLE_PACKAGE_LIST.add("com.android.systemui");
        }

        public OverlayState(AppStateAppOpsBridge.PermissionState permissionState) {
            super(permissionState.packageName, permissionState.userHandle);
            this.packageInfo = permissionState.packageInfo;
            this.appOpMode = permissionState.appOpMode;
            this.permissionDeclared = permissionState.permissionDeclared;
            this.staticPermissionGranted = permissionState.staticPermissionGranted;
            this.controlEnabled = !DISABLE_PACKAGE_LIST.contains(permissionState.packageName);
        }
    }
}
