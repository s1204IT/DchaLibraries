package com.android.settings.applications;

import android.content.Context;
import android.content.pm.PackageManager;
import com.android.settings.applications.AppStateBaseBridge;
import com.android.settings.notification.NotificationBackend;
import com.android.settingslib.applications.ApplicationsState;
import java.util.ArrayList;

public class AppStateNotificationBridge extends AppStateBaseBridge {
    private final Context mContext;
    private final NotificationBackend mNotifBackend;
    private final PackageManager mPm;
    public static final ApplicationsState.AppFilter FILTER_APP_NOTIFICATION_BLOCKED = new ApplicationsState.AppFilter() {
        @Override
        public void init() {
        }

        @Override
        public boolean filterApp(ApplicationsState.AppEntry info) {
            if (info == null || info.extraInfo == null || !(info.extraInfo instanceof NotificationBackend.AppRow)) {
                return false;
            }
            NotificationBackend.AppRow row = (NotificationBackend.AppRow) info.extraInfo;
            return row.banned;
        }
    };
    public static final ApplicationsState.AppFilter FILTER_APP_NOTIFICATION_SILENCED = new ApplicationsState.AppFilter() {
        @Override
        public void init() {
        }

        @Override
        public boolean filterApp(ApplicationsState.AppEntry info) {
            if (info == null || info.extraInfo == null) {
                return false;
            }
            NotificationBackend.AppRow row = (NotificationBackend.AppRow) info.extraInfo;
            return row.appImportance > 0 && row.appImportance < 3;
        }
    };
    public static final ApplicationsState.AppFilter FILTER_APP_NOTIFICATION_PRIORITY = new ApplicationsState.AppFilter() {
        @Override
        public void init() {
        }

        @Override
        public boolean filterApp(ApplicationsState.AppEntry info) {
            if (info == null || info.extraInfo == null) {
                return false;
            }
            return ((NotificationBackend.AppRow) info.extraInfo).appBypassDnd;
        }
    };
    public static final ApplicationsState.AppFilter FILTER_APP_NOTIFICATION_HIDE_SENSITIVE = new ApplicationsState.AppFilter() {
        @Override
        public void init() {
        }

        @Override
        public boolean filterApp(ApplicationsState.AppEntry info) {
            return info != null && info.extraInfo != null && ((NotificationBackend.AppRow) info.extraInfo).lockScreenSecure && ((NotificationBackend.AppRow) info.extraInfo).appVisOverride == 0;
        }
    };
    public static final ApplicationsState.AppFilter FILTER_APP_NOTIFICATION_HIDE_ALL = new ApplicationsState.AppFilter() {
        @Override
        public void init() {
        }

        @Override
        public boolean filterApp(ApplicationsState.AppEntry info) {
            return info != null && info.extraInfo != null && ((NotificationBackend.AppRow) info.extraInfo).lockScreenSecure && ((NotificationBackend.AppRow) info.extraInfo).appVisOverride == -1;
        }
    };

    public AppStateNotificationBridge(Context context, ApplicationsState appState, AppStateBaseBridge.Callback callback, NotificationBackend notifBackend) {
        super(appState, callback);
        this.mContext = context;
        this.mPm = this.mContext.getPackageManager();
        this.mNotifBackend = notifBackend;
    }

    @Override
    protected void loadAllExtraInfo() {
        ArrayList<ApplicationsState.AppEntry> apps = this.mAppSession.getAllApps();
        int N = apps.size();
        for (int i = 0; i < N; i++) {
            ApplicationsState.AppEntry app = apps.get(i);
            app.extraInfo = this.mNotifBackend.loadAppRow(this.mContext, this.mPm, app.info);
        }
    }

    @Override
    protected void updateExtraInfo(ApplicationsState.AppEntry app, String pkg, int uid) {
        app.extraInfo = this.mNotifBackend.loadAppRow(this.mContext, this.mPm, app.info);
    }
}
