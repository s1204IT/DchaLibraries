package com.android.settings.notification;

import android.app.NotificationManager;
import android.os.Bundle;
import android.service.notification.ZenModeConfig;
import android.support.v14.preference.SwitchPreference;
import android.support.v7.preference.DropDownPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;
import android.util.Log;
import com.android.internal.logging.MetricsLogger;
import com.android.settings.R;
import com.android.settings.search.Indexable;

public class ZenModePrioritySettings extends ZenModeSettingsBase implements Indexable {
    private DropDownPreference mCalls;
    private boolean mDisableListeners;
    private SwitchPreference mEvents;
    private DropDownPreference mMessages;
    private NotificationManager.Policy mPolicy;
    private SwitchPreference mReminders;
    private SwitchPreference mRepeatCallers;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.zen_mode_priority_settings);
        PreferenceScreen root = getPreferenceScreen();
        this.mPolicy = NotificationManager.from(this.mContext).getNotificationPolicy();
        this.mReminders = (SwitchPreference) root.findPreference("reminders");
        this.mReminders.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                if (ZenModePrioritySettings.this.mDisableListeners) {
                    return true;
                }
                boolean val = ((Boolean) newValue).booleanValue();
                MetricsLogger.action(ZenModePrioritySettings.this.mContext, 167, val);
                if (ZenModePrioritySettings.DEBUG) {
                    Log.d("ZenModeSettings", "onPrefChange allowReminders=" + val);
                }
                ZenModePrioritySettings.this.savePolicy(ZenModePrioritySettings.this.getNewPriorityCategories(val, 1), ZenModePrioritySettings.this.mPolicy.priorityCallSenders, ZenModePrioritySettings.this.mPolicy.priorityMessageSenders, ZenModePrioritySettings.this.mPolicy.suppressedVisualEffects);
                return true;
            }
        });
        this.mEvents = (SwitchPreference) root.findPreference("events");
        this.mEvents.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                if (ZenModePrioritySettings.this.mDisableListeners) {
                    return true;
                }
                boolean val = ((Boolean) newValue).booleanValue();
                MetricsLogger.action(ZenModePrioritySettings.this.mContext, 168, val);
                if (ZenModePrioritySettings.DEBUG) {
                    Log.d("ZenModeSettings", "onPrefChange allowEvents=" + val);
                }
                ZenModePrioritySettings.this.savePolicy(ZenModePrioritySettings.this.getNewPriorityCategories(val, 2), ZenModePrioritySettings.this.mPolicy.priorityCallSenders, ZenModePrioritySettings.this.mPolicy.priorityMessageSenders, ZenModePrioritySettings.this.mPolicy.suppressedVisualEffects);
                return true;
            }
        });
        this.mMessages = (DropDownPreference) root.findPreference("messages");
        addSources(this.mMessages);
        this.mMessages.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                if (ZenModePrioritySettings.this.mDisableListeners) {
                    return false;
                }
                int val = Integer.parseInt((String) newValue);
                boolean allowMessages = val != -1;
                int allowMessagesFrom = val == -1 ? ZenModePrioritySettings.this.mPolicy.priorityMessageSenders : val;
                MetricsLogger.action(ZenModePrioritySettings.this.mContext, 169, val);
                if (ZenModePrioritySettings.DEBUG) {
                    Log.d("ZenModeSettings", "onPrefChange allowMessages=" + allowMessages + " allowMessagesFrom=" + ZenModeConfig.sourceToString(allowMessagesFrom));
                }
                ZenModePrioritySettings.this.savePolicy(ZenModePrioritySettings.this.getNewPriorityCategories(allowMessages, 4), ZenModePrioritySettings.this.mPolicy.priorityCallSenders, allowMessagesFrom, ZenModePrioritySettings.this.mPolicy.suppressedVisualEffects);
                return true;
            }
        });
        this.mCalls = (DropDownPreference) root.findPreference("calls");
        addSources(this.mCalls);
        this.mCalls.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                if (ZenModePrioritySettings.this.mDisableListeners) {
                    return false;
                }
                int val = Integer.parseInt((String) newValue);
                boolean allowCalls = val != -1;
                int allowCallsFrom = val == -1 ? ZenModePrioritySettings.this.mPolicy.priorityCallSenders : val;
                MetricsLogger.action(ZenModePrioritySettings.this.mContext, 170, val);
                if (ZenModePrioritySettings.DEBUG) {
                    Log.d("ZenModeSettings", "onPrefChange allowCalls=" + allowCalls + " allowCallsFrom=" + ZenModeConfig.sourceToString(allowCallsFrom));
                }
                ZenModePrioritySettings.this.savePolicy(ZenModePrioritySettings.this.getNewPriorityCategories(allowCalls, 8), allowCallsFrom, ZenModePrioritySettings.this.mPolicy.priorityMessageSenders, ZenModePrioritySettings.this.mPolicy.suppressedVisualEffects);
                return true;
            }
        });
        this.mRepeatCallers = (SwitchPreference) root.findPreference("repeat_callers");
        this.mRepeatCallers.setSummary(this.mContext.getString(R.string.zen_mode_repeat_callers_summary, Integer.valueOf(this.mContext.getResources().getInteger(android.R.integer.config_dreamsBatteryLevelDrainCutoff))));
        this.mRepeatCallers.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                if (ZenModePrioritySettings.this.mDisableListeners) {
                    return true;
                }
                boolean val = ((Boolean) newValue).booleanValue();
                MetricsLogger.action(ZenModePrioritySettings.this.mContext, 171, val);
                if (ZenModePrioritySettings.DEBUG) {
                    Log.d("ZenModeSettings", "onPrefChange allowRepeatCallers=" + val);
                }
                int priorityCategories = ZenModePrioritySettings.this.getNewPriorityCategories(val, 16);
                ZenModePrioritySettings.this.savePolicy(priorityCategories, ZenModePrioritySettings.this.mPolicy.priorityCallSenders, ZenModePrioritySettings.this.mPolicy.priorityMessageSenders, ZenModePrioritySettings.this.mPolicy.suppressedVisualEffects);
                return true;
            }
        });
        updateControls();
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
        if (this.mCalls != null) {
            this.mCalls.setValue(Integer.toString(isPriorityCategoryEnabled(8) ? this.mPolicy.priorityCallSenders : -1));
        }
        this.mMessages.setValue(Integer.toString(isPriorityCategoryEnabled(4) ? this.mPolicy.priorityMessageSenders : -1));
        this.mReminders.setChecked(isPriorityCategoryEnabled(1));
        this.mEvents.setChecked(isPriorityCategoryEnabled(2));
        this.mRepeatCallers.setChecked(isPriorityCategoryEnabled(16));
        SwitchPreference switchPreference = this.mRepeatCallers;
        boolean z = (isPriorityCategoryEnabled(8) && this.mPolicy.priorityCallSenders == 0) ? false : true;
        switchPreference.setVisible(z);
        this.mDisableListeners = false;
    }

    @Override
    protected int getMetricsCategory() {
        return 141;
    }

    private static void addSources(DropDownPreference pref) {
        pref.setEntries(new CharSequence[]{pref.getContext().getString(R.string.zen_mode_from_anyone), pref.getContext().getString(R.string.zen_mode_from_contacts), pref.getContext().getString(R.string.zen_mode_from_starred), pref.getContext().getString(R.string.zen_mode_from_none)});
        pref.setEntryValues(new CharSequence[]{Integer.toString(0), Integer.toString(1), Integer.toString(2), Integer.toString(-1)});
    }

    private boolean isPriorityCategoryEnabled(int categoryType) {
        return (this.mPolicy.priorityCategories & categoryType) != 0;
    }

    public int getNewPriorityCategories(boolean allow, int categoryType) {
        int priorityCategories = this.mPolicy.priorityCategories;
        if (allow) {
            return priorityCategories | categoryType;
        }
        return priorityCategories & (~categoryType);
    }

    public void savePolicy(int priorityCategories, int priorityCallSenders, int priorityMessageSenders, int suppressedVisualEffects) {
        this.mPolicy = new NotificationManager.Policy(priorityCategories, priorityCallSenders, priorityMessageSenders, suppressedVisualEffects);
        NotificationManager.from(this.mContext).setNotificationPolicy(this.mPolicy);
    }
}
