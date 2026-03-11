package com.android.settings.notification;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.SwitchPreference;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import com.android.internal.widget.LockPatternUtils;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.Utils;
import com.android.settings.notification.NotificationAppList;

public class AppNotificationSettings extends SettingsPreferenceFragment {
    private static final boolean DEBUG = Log.isLoggable("AppNotificationSettings", 3);
    private NotificationAppList.AppRow mAppRow;
    private final NotificationAppList.Backend mBackend = new NotificationAppList.Backend();
    private SwitchPreference mBlock;
    private Context mContext;
    private boolean mCreated;
    private SwitchPreference mPriority;
    private SwitchPreference mSensitive;

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if (DEBUG) {
            Log.d("AppNotificationSettings", "onActivityCreated mCreated=" + this.mCreated);
        }
        if (this.mCreated) {
            Log.w("AppNotificationSettings", "onActivityCreated: ignoring duplicate call");
            return;
        }
        this.mCreated = true;
        if (this.mAppRow != null) {
            View content = getActivity().findViewById(R.id.main_content);
            ViewGroup contentParent = (ViewGroup) content.getParent();
            View bar = getActivity().getLayoutInflater().inflate(R.layout.app_notification_header, contentParent, false);
            ImageView appIcon = (ImageView) bar.findViewById(R.id.app_icon);
            appIcon.setImageDrawable(this.mAppRow.icon);
            TextView appName = (TextView) bar.findViewById(R.id.app_name);
            appName.setText(this.mAppRow.label);
            View appSettings = bar.findViewById(R.id.app_settings);
            if (this.mAppRow.settingsIntent == null) {
                appSettings.setVisibility(8);
            } else {
                appSettings.setClickable(true);
                appSettings.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        AppNotificationSettings.this.mContext.startActivity(AppNotificationSettings.this.mAppRow.settingsIntent);
                    }
                });
            }
            contentParent.addView(bar, 0);
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.mContext = getActivity();
        Intent intent = getActivity().getIntent();
        if (DEBUG) {
            Log.d("AppNotificationSettings", "onCreate getIntent()=" + intent);
        }
        if (intent == null) {
            Log.w("AppNotificationSettings", "No intent");
            toastAndFinish();
            return;
        }
        final int uid = intent.getIntExtra("app_uid", -1);
        final String pkg = intent.getStringExtra("app_package");
        if (uid == -1 || TextUtils.isEmpty(pkg)) {
            Log.w("AppNotificationSettings", "Missing extras: app_package was " + pkg + ", app_uid was " + uid);
            toastAndFinish();
            return;
        }
        if (DEBUG) {
            Log.d("AppNotificationSettings", "Load details for pkg=" + pkg + " uid=" + uid);
        }
        PackageManager pm = getPackageManager();
        PackageInfo info = findPackageInfo(pm, pkg, uid);
        if (info == null) {
            Log.w("AppNotificationSettings", "Failed to find package info: app_package was " + pkg + ", app_uid was " + uid);
            toastAndFinish();
            return;
        }
        addPreferencesFromResource(R.xml.app_notification_settings);
        this.mBlock = (SwitchPreference) findPreference("block");
        this.mPriority = (SwitchPreference) findPreference("priority");
        this.mSensitive = (SwitchPreference) findPreference("sensitive");
        boolean secure = new LockPatternUtils(getActivity()).isSecure();
        boolean enabled = getLockscreenNotificationsEnabled();
        boolean allowPrivate = getLockscreenAllowPrivateNotifications();
        if (!secure || !enabled || !allowPrivate) {
            getPreferenceScreen().removePreference(this.mSensitive);
        }
        this.mAppRow = NotificationAppList.loadAppRow(pm, info.applicationInfo, this.mBackend);
        if (intent.hasExtra("has_settings_intent")) {
            if (intent.getBooleanExtra("has_settings_intent", false)) {
                this.mAppRow.settingsIntent = (Intent) intent.getParcelableExtra("settings_intent");
            }
        } else {
            ArrayMap<String, NotificationAppList.AppRow> rows = new ArrayMap<>();
            rows.put(this.mAppRow.pkg, this.mAppRow);
            NotificationAppList.collectConfigActivities(getPackageManager(), rows);
        }
        this.mBlock.setChecked(this.mAppRow.banned);
        this.mPriority.setChecked(this.mAppRow.priority);
        if (this.mSensitive != null) {
            this.mSensitive.setChecked(this.mAppRow.sensitive);
        }
        this.mBlock.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                boolean block = ((Boolean) newValue).booleanValue();
                return AppNotificationSettings.this.mBackend.setNotificationsBanned(pkg, uid, block);
            }
        });
        this.mPriority.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                boolean priority = ((Boolean) newValue).booleanValue();
                return AppNotificationSettings.this.mBackend.setHighPriority(pkg, uid, priority);
            }
        });
        if (this.mSensitive != null) {
            this.mSensitive.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    boolean sensitive = ((Boolean) newValue).booleanValue();
                    return AppNotificationSettings.this.mBackend.setSensitive(pkg, uid, sensitive);
                }
            });
        }
        if (Utils.isSystemPackage(pm, info)) {
            getPreferenceScreen().removePreference(this.mBlock);
            this.mPriority.setDependency(null);
        }
    }

    private boolean getLockscreenNotificationsEnabled() {
        return Settings.Secure.getInt(getContentResolver(), "lock_screen_show_notifications", 0) != 0;
    }

    private boolean getLockscreenAllowPrivateNotifications() {
        return Settings.Secure.getInt(getContentResolver(), "lock_screen_allow_private_notifications", 0) != 0;
    }

    private void toastAndFinish() {
        Toast.makeText(this.mContext, R.string.app_not_found_dlg_text, 0).show();
        getActivity().finish();
    }

    private static PackageInfo findPackageInfo(PackageManager pm, String pkg, int uid) {
        String[] packages = pm.getPackagesForUid(uid);
        if (packages != null && pkg != null) {
            for (String p : packages) {
                if (pkg.equals(p)) {
                    try {
                        return pm.getPackageInfo(pkg, 64);
                    } catch (PackageManager.NameNotFoundException e) {
                        Log.w("AppNotificationSettings", "Failed to load package " + pkg, e);
                    }
                }
            }
        }
        return null;
    }
}
