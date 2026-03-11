package com.android.settings.notification;

import android.app.NotificationManager;
import android.os.Bundle;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;
import com.android.settings.R;

public class ZenModeSettings extends ZenModeSettingsBase {
    private NotificationManager.Policy mPolicy;
    private Preference mPrioritySettings;
    private Preference mVisualSettings;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.zen_mode_settings);
        PreferenceScreen root = getPreferenceScreen();
        this.mPrioritySettings = root.findPreference("priority_settings");
        this.mVisualSettings = root.findPreference("visual_interruptions_settings");
        this.mPolicy = NotificationManager.from(this.mContext).getNotificationPolicy();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (!isUiRestricted()) {
        }
    }

    @Override
    protected int getMetricsCategory() {
        return 76;
    }

    @Override
    protected void onZenModeChanged() {
        updateControls();
    }

    @Override
    protected void onZenModeConfigChanged() {
        this.mPolicy = NotificationManager.from(this.mContext).getNotificationPolicy();
        updateControls();
    }

    private void updateControls() {
        updatePrioritySettingsSummary();
        updateVisualSettingsSummary();
    }

    private void updatePrioritySettingsSummary() {
        String s = appendLowercase(appendLowercase(getResources().getString(R.string.zen_mode_alarms), isCategoryEnabled(this.mPolicy, 1), R.string.zen_mode_reminders), isCategoryEnabled(this.mPolicy, 2), R.string.zen_mode_events);
        if (isCategoryEnabled(this.mPolicy, 4)) {
            if (this.mPolicy.priorityMessageSenders == 0) {
                s = appendLowercase(s, true, R.string.zen_mode_all_messages);
            } else {
                s = appendLowercase(s, true, R.string.zen_mode_selected_messages);
            }
        }
        if (isCategoryEnabled(this.mPolicy, 8)) {
            if (this.mPolicy.priorityCallSenders == 0) {
                s = appendLowercase(s, true, R.string.zen_mode_all_callers);
            } else {
                s = appendLowercase(s, true, R.string.zen_mode_selected_callers);
            }
        } else if (isCategoryEnabled(this.mPolicy, 16)) {
            s = appendLowercase(s, true, R.string.zen_mode_repeat_callers);
        }
        this.mPrioritySettings.setSummary(s);
    }

    private void updateVisualSettingsSummary() {
        String s = getString(R.string.zen_mode_all_visual_interruptions);
        if (isEffectSuppressed(2) && isEffectSuppressed(1)) {
            s = getString(R.string.zen_mode_no_visual_interruptions);
        } else if (isEffectSuppressed(2)) {
            s = getString(R.string.zen_mode_screen_on_visual_interruptions);
        } else if (isEffectSuppressed(1)) {
            s = getString(R.string.zen_mode_screen_off_visual_interruptions);
        }
        this.mVisualSettings.setSummary(s);
    }

    private boolean isEffectSuppressed(int effect) {
        return (this.mPolicy.suppressedVisualEffects & effect) != 0;
    }

    private boolean isCategoryEnabled(NotificationManager.Policy policy, int categoryType) {
        return (policy.priorityCategories & categoryType) != 0;
    }

    private String appendLowercase(String s, boolean condition, int resId) {
        if (condition) {
            return getResources().getString(R.string.join_many_items_middle, s, getResources().getString(resId).toLowerCase());
        }
        return s;
    }

    @Override
    protected int getHelpResource() {
        return R.string.help_uri_interruptions;
    }
}
