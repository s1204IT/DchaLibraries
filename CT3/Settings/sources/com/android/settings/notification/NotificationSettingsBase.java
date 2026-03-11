package com.android.settings.notification;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.UserHandle;
import android.os.UserManager;
import android.provider.Settings;
import android.support.v7.preference.Preference;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.notification.ImportanceSeekBarPreference;
import com.android.settings.notification.RestrictedDropDownPreference;
import com.android.settingslib.RestrictedLockUtils;
import com.android.settingslib.RestrictedSwitchPreference;
import java.util.ArrayList;

public abstract class NotificationSettingsBase extends SettingsPreferenceFragment {
    private static final boolean DEBUG = Log.isLoggable("NotifiSettingsBase", 3);
    protected RestrictedSwitchPreference mBlock;
    protected Context mContext;
    protected boolean mCreated;
    protected ImportanceSeekBarPreference mImportance;
    protected String mPkg;
    protected PackageInfo mPkgInfo;
    protected PackageManager mPm;
    protected RestrictedSwitchPreference mPriority;
    protected RestrictedSwitchPreference mSilent;
    protected RestrictedLockUtils.EnforcedAdmin mSuspendedAppsAdmin;
    protected int mUid;
    protected UserManager mUm;
    protected int mUserId;
    protected RestrictedDropDownPreference mVisibilityOverride;
    protected final NotificationBackend mBackend = new NotificationBackend();
    protected boolean mShowSlider = false;

    abstract void updateDependents(int i);

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if (DEBUG) {
            Log.d("NotifiSettingsBase", "onActivityCreated mCreated=" + this.mCreated);
        }
        if (this.mCreated) {
            Log.w("NotifiSettingsBase", "onActivityCreated: ignoring duplicate call");
        } else {
            this.mCreated = true;
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.mContext = getActivity();
        Intent intent = getActivity().getIntent();
        Bundle args = getArguments();
        if (DEBUG) {
            Log.d("NotifiSettingsBase", "onCreate getIntent()=" + intent);
        }
        if (intent == null && args == null) {
            Log.w("NotifiSettingsBase", "No intent");
            toastAndFinish();
            return;
        }
        this.mPm = getPackageManager();
        this.mUm = (UserManager) this.mContext.getSystemService("user");
        this.mPkg = (args == null || !args.containsKey("package")) ? intent.getStringExtra("app_package") : args.getString("package");
        this.mUid = (args == null || !args.containsKey("uid")) ? intent.getIntExtra("app_uid", -1) : args.getInt("uid");
        if (this.mUid == -1 || TextUtils.isEmpty(this.mPkg)) {
            Log.w("NotifiSettingsBase", "Missing extras: app_package was " + this.mPkg + ", app_uid was " + this.mUid);
            toastAndFinish();
            return;
        }
        this.mUserId = UserHandle.getUserId(this.mUid);
        if (DEBUG) {
            Log.d("NotifiSettingsBase", "Load details for pkg=" + this.mPkg + " uid=" + this.mUid);
        }
        this.mPkgInfo = findPackageInfo(this.mPkg, this.mUid);
        if (this.mPkgInfo == null) {
            Log.w("NotifiSettingsBase", "Failed to find package info: app_package was " + this.mPkg + ", app_uid was " + this.mUid);
            toastAndFinish();
        } else {
            this.mSuspendedAppsAdmin = RestrictedLockUtils.checkIfApplicationIsSuspended(this.mContext, this.mPkg, this.mUserId);
            this.mShowSlider = Settings.Secure.getInt(getContentResolver(), "show_importance_slider", 0) == 1;
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (this.mUid != -1 && getPackageManager().getPackagesForUid(this.mUid) == null) {
            finish();
            return;
        }
        this.mSuspendedAppsAdmin = RestrictedLockUtils.checkIfApplicationIsSuspended(this.mContext, this.mPkg, this.mUserId);
        if (this.mImportance != null) {
            this.mImportance.setDisabledByAdmin(this.mSuspendedAppsAdmin);
        }
        if (this.mPriority != null) {
            this.mPriority.setDisabledByAdmin(this.mSuspendedAppsAdmin);
        }
        if (this.mBlock != null) {
            this.mBlock.setDisabledByAdmin(this.mSuspendedAppsAdmin);
        }
        if (this.mSilent != null) {
            this.mSilent.setDisabledByAdmin(this.mSuspendedAppsAdmin);
        }
        if (this.mVisibilityOverride == null) {
            return;
        }
        this.mVisibilityOverride.setDisabledByAdmin(this.mSuspendedAppsAdmin);
    }

    protected void setupImportancePrefs(boolean isSystemApp, int importance, boolean banned) {
        if (this.mShowSlider) {
            setVisible(this.mBlock, false);
            setVisible(this.mSilent, false);
            this.mImportance.setDisabledByAdmin(this.mSuspendedAppsAdmin);
            this.mImportance.setMinimumProgress(isSystemApp ? 1 : 0);
            this.mImportance.setMax(5);
            this.mImportance.setProgress(importance);
            this.mImportance.setAutoOn(importance == -1000);
            this.mImportance.setCallback(new ImportanceSeekBarPreference.Callback() {
                @Override
                public void onImportanceChanged(int progress, boolean fromUser) {
                    if (fromUser) {
                        NotificationSettingsBase.this.mBackend.setImportance(NotificationSettingsBase.this.mPkg, NotificationSettingsBase.this.mUid, progress);
                    }
                    NotificationSettingsBase.this.updateDependents(progress);
                }
            });
            return;
        }
        setVisible(this.mImportance, false);
        if (isSystemApp) {
            setVisible(this.mBlock, false);
            setVisible(this.mSilent, false);
        } else {
            this.mBlock.setChecked(importance != 0 ? banned : true);
            this.mBlock.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    boolean blocked = ((Boolean) newValue).booleanValue();
                    int importance2 = blocked ? 0 : -1000;
                    NotificationSettingsBase.this.mBackend.setImportance(NotificationSettingsBase.this.mPkgInfo.packageName, NotificationSettingsBase.this.mUid, importance2);
                    NotificationSettingsBase.this.updateDependents(importance2);
                    return true;
                }
            });
            this.mSilent.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    boolean silenced = ((Boolean) newValue).booleanValue();
                    int importance2 = silenced ? 2 : -1000;
                    NotificationSettingsBase.this.mBackend.setImportance(NotificationSettingsBase.this.mPkgInfo.packageName, NotificationSettingsBase.this.mUid, importance2);
                    NotificationSettingsBase.this.updateDependents(importance2);
                    return true;
                }
            });
            updateDependents(banned ? 0 : importance);
        }
    }

    protected void setupPriorityPref(boolean priority) {
        this.mPriority.setDisabledByAdmin(this.mSuspendedAppsAdmin);
        this.mPriority.setChecked(priority);
        this.mPriority.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                boolean bypassZenMode = ((Boolean) newValue).booleanValue();
                return NotificationSettingsBase.this.mBackend.setBypassZenMode(NotificationSettingsBase.this.mPkgInfo.packageName, NotificationSettingsBase.this.mUid, bypassZenMode);
            }
        });
    }

    protected void setupVisOverridePref(int sensitive) {
        ArrayList<CharSequence> entries = new ArrayList<>();
        ArrayList<CharSequence> values = new ArrayList<>();
        this.mVisibilityOverride.clearRestrictedItems();
        if (getLockscreenNotificationsEnabled() && getLockscreenAllowPrivateNotifications()) {
            String summaryShowEntry = getString(R.string.lock_screen_notifications_summary_show);
            String summaryShowEntryValue = Integer.toString(-1000);
            entries.add(summaryShowEntry);
            values.add(summaryShowEntryValue);
            setRestrictedIfNotificationFeaturesDisabled(summaryShowEntry, summaryShowEntryValue, 12);
        }
        String summaryHideEntry = getString(R.string.lock_screen_notifications_summary_hide);
        String summaryHideEntryValue = Integer.toString(0);
        entries.add(summaryHideEntry);
        values.add(summaryHideEntryValue);
        setRestrictedIfNotificationFeaturesDisabled(summaryHideEntry, summaryHideEntryValue, 4);
        entries.add(getString(R.string.lock_screen_notifications_summary_disable));
        values.add(Integer.toString(-1));
        this.mVisibilityOverride.setEntries((CharSequence[]) entries.toArray(new CharSequence[entries.size()]));
        this.mVisibilityOverride.setEntryValues((CharSequence[]) values.toArray(new CharSequence[values.size()]));
        if (sensitive == -1000) {
            this.mVisibilityOverride.setValue(Integer.toString(getGlobalVisibility()));
        } else {
            this.mVisibilityOverride.setValue(Integer.toString(sensitive));
        }
        this.mVisibilityOverride.setSummary("%s");
        this.mVisibilityOverride.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                int sensitive2 = Integer.parseInt((String) newValue);
                if (sensitive2 == NotificationSettingsBase.this.getGlobalVisibility()) {
                    sensitive2 = -1000;
                }
                NotificationSettingsBase.this.mBackend.setVisibilityOverride(NotificationSettingsBase.this.mPkgInfo.packageName, NotificationSettingsBase.this.mUid, sensitive2);
                return true;
            }
        });
    }

    private void setRestrictedIfNotificationFeaturesDisabled(CharSequence entry, CharSequence entryValue, int keyguardNotificationFeatures) {
        RestrictedLockUtils.EnforcedAdmin admin = RestrictedLockUtils.checkIfKeyguardFeaturesDisabled(this.mContext, keyguardNotificationFeatures, this.mUserId);
        if (admin == null) {
            return;
        }
        RestrictedDropDownPreference.RestrictedItem item = new RestrictedDropDownPreference.RestrictedItem(entry, entryValue, admin);
        this.mVisibilityOverride.addRestrictedItem(item);
    }

    public int getGlobalVisibility() {
        if (!getLockscreenNotificationsEnabled()) {
            return -1;
        }
        if (getLockscreenAllowPrivateNotifications()) {
            return -1000;
        }
        return 0;
    }

    protected boolean getLockscreenNotificationsEnabled() {
        return Settings.Secure.getInt(getContentResolver(), "lock_screen_show_notifications", 0) != 0;
    }

    protected boolean getLockscreenAllowPrivateNotifications() {
        return Settings.Secure.getInt(getContentResolver(), "lock_screen_allow_private_notifications", 0) != 0;
    }

    protected void setVisible(Preference p, boolean visible) {
        boolean isVisible = getPreferenceScreen().findPreference(p.getKey()) != null;
        if (isVisible == visible) {
            return;
        }
        if (visible) {
            getPreferenceScreen().addPreference(p);
        } else {
            getPreferenceScreen().removePreference(p);
        }
    }

    protected void toastAndFinish() {
        Toast.makeText(this.mContext, R.string.app_not_found_dlg_text, 0).show();
        getActivity().finish();
    }

    private PackageInfo findPackageInfo(String pkg, int uid) {
        String[] packages = this.mPm.getPackagesForUid(uid);
        if (packages != null && pkg != null) {
            for (String p : packages) {
                if (pkg.equals(p)) {
                    try {
                        return this.mPm.getPackageInfo(pkg, 64);
                    } catch (PackageManager.NameNotFoundException e) {
                        Log.w("NotifiSettingsBase", "Failed to load package " + pkg, e);
                    }
                }
            }
        }
        return null;
    }
}
