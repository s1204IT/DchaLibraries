package com.android.settings.notification;

import android.app.NotificationManager;
import android.os.Bundle;
import android.support.v14.preference.SwitchPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;
import android.util.Log;
import com.android.internal.logging.MetricsLogger;
import com.android.settings.R;

public class ZenModeVisualInterruptionSettings extends ZenModeSettingsBase {
    private boolean mDisableListeners;
    private NotificationManager.Policy mPolicy;
    private SwitchPreference mScreenOff;
    private SwitchPreference mScreenOn;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.zen_mode_visual_interruptions_settings);
        PreferenceScreen root = getPreferenceScreen();
        this.mPolicy = NotificationManager.from(this.mContext).getNotificationPolicy();
        this.mScreenOff = (SwitchPreference) root.findPreference("screenOff");
        if (!getResources().getBoolean(android.R.^attr-private.dreamActivityCloseExitAnimation)) {
            this.mScreenOff.setSummary(R.string.zen_mode_screen_off_summary_no_led);
        }
        this.mScreenOff.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                if (ZenModeVisualInterruptionSettings.this.mDisableListeners) {
                    return true;
                }
                boolean val = ((Boolean) newValue).booleanValue();
                MetricsLogger.action(ZenModeVisualInterruptionSettings.this.mContext, 263, val);
                if (ZenModeVisualInterruptionSettings.DEBUG) {
                    Log.d("ZenModeSettings", "onPrefChange suppressWhenScreenOff=" + val);
                }
                ZenModeVisualInterruptionSettings.this.savePolicy(ZenModeVisualInterruptionSettings.this.getNewSuppressedEffects(val, 1));
                return true;
            }
        });
        this.mScreenOn = (SwitchPreference) root.findPreference("screenOn");
        this.mScreenOn.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                if (ZenModeVisualInterruptionSettings.this.mDisableListeners) {
                    return true;
                }
                boolean val = ((Boolean) newValue).booleanValue();
                MetricsLogger.action(ZenModeVisualInterruptionSettings.this.mContext, 269, val);
                if (ZenModeVisualInterruptionSettings.DEBUG) {
                    Log.d("ZenModeSettings", "onPrefChange suppressWhenScreenOn=" + val);
                }
                ZenModeVisualInterruptionSettings.this.savePolicy(ZenModeVisualInterruptionSettings.this.getNewSuppressedEffects(val, 2));
                return true;
            }
        });
    }

    @Override
    protected int getMetricsCategory() {
        return 262;
    }

    @Override
    protected void onZenModeChanged() {
    }

    @Override
    protected void onZenModeConfigChanged() {
        this.mPolicy = NotificationManager.from(this.mContext).getNotificationPolicy();
        updateControls();
    }

    private void updateControls() {
        this.mDisableListeners = true;
        this.mScreenOff.setChecked(isEffectSuppressed(1));
        this.mScreenOn.setChecked(isEffectSuppressed(2));
        this.mDisableListeners = false;
    }

    private boolean isEffectSuppressed(int effect) {
        return (this.mPolicy.suppressedVisualEffects & effect) != 0;
    }

    public int getNewSuppressedEffects(boolean suppress, int effectType) {
        int effects = this.mPolicy.suppressedVisualEffects;
        if (suppress) {
            return effects | effectType;
        }
        return effects & (~effectType);
    }

    public void savePolicy(int suppressedVisualEffects) {
        this.mPolicy = new NotificationManager.Policy(this.mPolicy.priorityCategories, this.mPolicy.priorityCallSenders, this.mPolicy.priorityMessageSenders, suppressedVisualEffects);
        NotificationManager.from(this.mContext).setNotificationPolicy(this.mPolicy);
    }
}
