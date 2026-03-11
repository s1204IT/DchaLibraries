package com.android.settings.notification;

import android.app.NotificationManager;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.ResolveInfo;
import android.content.pm.UserInfo;
import android.os.Bundle;
import android.os.UserHandle;
import android.util.ArrayMap;
import android.util.EventLog;
import android.util.Log;
import android.view.Window;
import android.view.WindowManager;
import com.android.internal.widget.LockPatternUtils;
import com.android.settings.AppHeader;
import com.android.settings.R;
import com.android.settings.notification.NotificationBackend;
import com.android.settingslib.RestrictedSwitchPreference;
import java.util.List;

public class AppNotificationSettings extends NotificationSettingsBase {
    private NotificationBackend.AppRow mAppRow;
    private boolean mDndVisualEffectsSuppressed;
    private static final boolean DEBUG = Log.isLoggable("AppNotificationSettings", 3);
    private static final Intent APP_NOTIFICATION_PREFS_CATEGORY_INTENT = new Intent("android.intent.action.MAIN").addCategory("android.intent.category.NOTIFICATION_PREFERENCES");

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if (this.mAppRow == null) {
            return;
        }
        AppHeader.createAppHeader(this, this.mAppRow.icon, this.mAppRow.label, this.mAppRow.pkg, this.mAppRow.uid, this.mAppRow.settingsIntent);
    }

    @Override
    protected int getMetricsCategory() {
        return 72;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.app_notification_settings);
        this.mImportance = (ImportanceSeekBarPreference) findPreference("importance");
        this.mPriority = (RestrictedSwitchPreference) getPreferenceScreen().findPreference("bypass_dnd");
        this.mVisibilityOverride = (RestrictedDropDownPreference) getPreferenceScreen().findPreference("visibility_override");
        this.mBlock = (RestrictedSwitchPreference) getPreferenceScreen().findPreference("block");
        this.mSilent = (RestrictedSwitchPreference) getPreferenceScreen().findPreference("silent");
        if (this.mPkgInfo == null) {
            return;
        }
        this.mAppRow = this.mBackend.loadAppRow(this.mContext, this.mPm, this.mPkgInfo);
        NotificationManager.Policy policy = NotificationManager.from(this.mContext).getNotificationPolicy();
        this.mDndVisualEffectsSuppressed = (policy == null || policy.suppressedVisualEffects == 0) ? false : true;
        ArrayMap<String, NotificationBackend.AppRow> rows = new ArrayMap<>();
        rows.put(this.mAppRow.pkg, this.mAppRow);
        collectConfigActivities(rows);
        setupImportancePrefs(this.mAppRow.systemApp, this.mAppRow.appImportance, this.mAppRow.banned);
        setupPriorityPref(this.mAppRow.appBypassDnd);
        setupVisOverridePref(this.mAppRow.appVisOverride);
        updateDependents(this.mAppRow.appImportance);
    }

    @Override
    public void onResume() {
        super.onResume();
        getActivity().getWindow().addPrivateFlags(524288);
        EventLog.writeEvent(1397638484, "119115683", -1, "");
    }

    @Override
    public void onPause() {
        super.onPause();
        Window window = getActivity().getWindow();
        WindowManager.LayoutParams attrs = window.getAttributes();
        attrs.privateFlags &= -524289;
        window.setAttributes(attrs);
    }

    @Override
    protected void updateDependents(int importance) {
        LockPatternUtils utils = new LockPatternUtils(getActivity());
        boolean lockscreenSecure = utils.isSecure(UserHandle.myUserId());
        UserInfo parentUser = this.mUm.getProfileParent(UserHandle.myUserId());
        if (parentUser != null) {
            lockscreenSecure |= utils.isSecure(parentUser.id);
        }
        if (getPreferenceScreen().findPreference(this.mBlock.getKey()) != null) {
            setVisible(this.mSilent, checkCanBeVisible(1, importance));
            this.mSilent.setChecked(importance == 2);
        }
        RestrictedSwitchPreference restrictedSwitchPreference = this.mPriority;
        boolean z = checkCanBeVisible(3, importance) && !this.mDndVisualEffectsSuppressed;
        setVisible(restrictedSwitchPreference, z);
        RestrictedDropDownPreference restrictedDropDownPreference = this.mVisibilityOverride;
        if (!checkCanBeVisible(1, importance)) {
            lockscreenSecure = false;
        }
        setVisible(restrictedDropDownPreference, lockscreenSecure);
    }

    protected boolean checkCanBeVisible(int minImportanceVisible, int importance) {
        return importance == -1000 || importance >= minImportanceVisible;
    }

    private List<ResolveInfo> queryNotificationConfigActivities() {
        if (DEBUG) {
            Log.d("AppNotificationSettings", "APP_NOTIFICATION_PREFS_CATEGORY_INTENT is " + APP_NOTIFICATION_PREFS_CATEGORY_INTENT);
        }
        List<ResolveInfo> resolveInfos = this.mPm.queryIntentActivities(APP_NOTIFICATION_PREFS_CATEGORY_INTENT, 0);
        return resolveInfos;
    }

    private void collectConfigActivities(ArrayMap<String, NotificationBackend.AppRow> rows) {
        List<ResolveInfo> resolveInfos = queryNotificationConfigActivities();
        applyConfigActivities(rows, resolveInfos);
    }

    private void applyConfigActivities(ArrayMap<String, NotificationBackend.AppRow> rows, List<ResolveInfo> resolveInfos) {
        if (DEBUG) {
            Log.d("AppNotificationSettings", "Found " + resolveInfos.size() + " preference activities" + (resolveInfos.size() == 0 ? " ;_;" : ""));
        }
        for (ResolveInfo ri : resolveInfos) {
            ActivityInfo activityInfo = ri.activityInfo;
            ApplicationInfo appInfo = activityInfo.applicationInfo;
            NotificationBackend.AppRow row = rows.get(appInfo.packageName);
            if (row == null) {
                if (DEBUG) {
                    Log.v("AppNotificationSettings", "Ignoring notification preference activity (" + activityInfo.name + ") for unknown package " + activityInfo.packageName);
                }
            } else if (row.settingsIntent == null) {
                row.settingsIntent = new Intent(APP_NOTIFICATION_PREFS_CATEGORY_INTENT).setClassName(activityInfo.packageName, activityInfo.name);
            } else if (DEBUG) {
                Log.v("AppNotificationSettings", "Ignoring duplicate notification preference activity (" + activityInfo.name + ") for package " + activityInfo.packageName);
            }
        }
    }
}
