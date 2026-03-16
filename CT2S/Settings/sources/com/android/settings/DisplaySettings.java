package com.android.settings;

import android.app.Activity;
import android.app.ActivityManagerNative;
import android.app.Dialog;
import android.app.admin.DevicePolicyManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Bundle;
import android.os.RemoteException;
import android.os.SystemProperties;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceScreen;
import android.preference.SwitchPreference;
import android.provider.SearchIndexableResource;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import com.android.internal.view.RotationPolicy;
import com.android.settings.notification.DropDownPreference;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.search.Indexable;
import java.util.ArrayList;
import java.util.List;

public class DisplaySettings extends SettingsPreferenceFragment implements Preference.OnPreferenceChangeListener, Preference.OnPreferenceClickListener, Indexable {
    public static final Indexable.SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER = new BaseSearchIndexProvider() {
        @Override
        public List<SearchIndexableResource> getXmlResourcesToIndex(Context context, boolean enabled) {
            ArrayList<SearchIndexableResource> result = new ArrayList<>();
            SearchIndexableResource sir = new SearchIndexableResource(context);
            sir.xmlResId = R.xml.display_settings;
            result.add(sir);
            return result;
        }

        @Override
        public List<String> getNonIndexableKeys(Context context) {
            ArrayList<String> result = new ArrayList<>();
            if (!context.getResources().getBoolean(android.R.^attr-private.gestureOverlayViewStyle)) {
                result.add("screensaver");
            }
            if (!DisplaySettings.isAutomaticBrightnessAvailable(context.getResources())) {
                result.add("auto_brightness");
            }
            if (!DisplaySettings.isLiftToWakeAvailable(context)) {
                result.add("lift_to_wake");
            }
            if (!DisplaySettings.isDozeAvailable(context)) {
                result.add("doze");
            }
            if (!RotationPolicy.isRotationLockToggleVisible(context)) {
                result.add("auto_rotate");
            }
            return result;
        }
    };
    private SwitchPreference mAutoBrightnessPreference;
    private final Configuration mCurConfig = new Configuration();
    private SwitchPreference mDozePreference;
    private WarnedListPreference mFontSizePref;
    private SwitchPreference mLiftToWakePreference;
    private Preference mScreenSaverPreference;
    private ListPreference mScreenTimeoutPreference;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        int rotateLockedResourceId;
        super.onCreate(savedInstanceState);
        final Activity activity = getActivity();
        ContentResolver resolver = activity.getContentResolver();
        addPreferencesFromResource(R.xml.display_settings);
        this.mScreenSaverPreference = findPreference("screensaver");
        if (this.mScreenSaverPreference != null && !getResources().getBoolean(android.R.^attr-private.gestureOverlayViewStyle)) {
            getPreferenceScreen().removePreference(this.mScreenSaverPreference);
        }
        this.mScreenTimeoutPreference = (ListPreference) findPreference("screen_timeout");
        long currentTimeout = Settings.System.getLong(resolver, "screen_off_timeout", 30000L);
        this.mScreenTimeoutPreference.setValue(String.valueOf(currentTimeout));
        this.mScreenTimeoutPreference.setOnPreferenceChangeListener(this);
        disableUnusableTimeouts(this.mScreenTimeoutPreference);
        updateTimeoutPreferenceDescription(currentTimeout);
        this.mFontSizePref = (WarnedListPreference) findPreference("font_size");
        this.mFontSizePref.setOnPreferenceChangeListener(this);
        this.mFontSizePref.setOnPreferenceClickListener(this);
        if (isAutomaticBrightnessAvailable(getResources())) {
            this.mAutoBrightnessPreference = (SwitchPreference) findPreference("auto_brightness");
            this.mAutoBrightnessPreference.setOnPreferenceChangeListener(this);
        } else {
            removePreference("auto_brightness");
        }
        if (isLiftToWakeAvailable(activity)) {
            this.mLiftToWakePreference = (SwitchPreference) findPreference("lift_to_wake");
            this.mLiftToWakePreference.setOnPreferenceChangeListener(this);
        } else {
            removePreference("lift_to_wake");
        }
        if (isDozeAvailable(activity)) {
            this.mDozePreference = (SwitchPreference) findPreference("doze");
            this.mDozePreference.setOnPreferenceChangeListener(this);
        } else {
            removePreference("doze");
        }
        if (RotationPolicy.isRotationLockToggleVisible(activity)) {
            DropDownPreference rotatePreference = (DropDownPreference) findPreference("auto_rotate");
            rotatePreference.addItem(activity.getString(R.string.display_auto_rotate_rotate), (Object) false);
            if (allowAllRotations(activity)) {
                rotateLockedResourceId = R.string.display_auto_rotate_stay_in_current;
            } else if (RotationPolicy.getRotationLockOrientation(activity) == 1) {
                rotateLockedResourceId = R.string.display_auto_rotate_stay_in_portrait;
            } else {
                rotateLockedResourceId = R.string.display_auto_rotate_stay_in_landscape;
            }
            rotatePreference.addItem(activity.getString(rotateLockedResourceId), (Object) true);
            rotatePreference.setSelectedItem(RotationPolicy.isRotationLocked(activity) ? 1 : 0);
            rotatePreference.setCallback(new DropDownPreference.Callback() {
                @Override
                public boolean onItemSelected(int pos, Object value) {
                    RotationPolicy.setRotationLock(activity, ((Boolean) value).booleanValue());
                    return true;
                }
            });
            return;
        }
        removePreference("auto_rotate");
    }

    private static boolean allowAllRotations(Context context) {
        return Resources.getSystem().getBoolean(android.R.^attr-private.colorSurfaceHighlight);
    }

    private static boolean isLiftToWakeAvailable(Context context) {
        SensorManager sensors = (SensorManager) context.getSystemService("sensor");
        return (sensors == null || sensors.getDefaultSensor(23) == null) ? false : true;
    }

    private static boolean isDozeAvailable(Context context) {
        String name = Build.IS_DEBUGGABLE ? SystemProperties.get("debug.doze.component") : null;
        if (TextUtils.isEmpty(name)) {
            name = context.getResources().getString(android.R.string.config_devicePolicyManagement);
        }
        return !TextUtils.isEmpty(name);
    }

    private static boolean isAutomaticBrightnessAvailable(Resources res) {
        return res.getBoolean(android.R.^attr-private.borderRight);
    }

    private void updateTimeoutPreferenceDescription(long currentTimeout) {
        String summary;
        ListPreference preference = this.mScreenTimeoutPreference;
        if (currentTimeout < 0) {
            summary = "";
        } else {
            CharSequence[] entries = preference.getEntries();
            CharSequence[] values = preference.getEntryValues();
            if (entries == null || entries.length == 0) {
                summary = "";
            } else {
                int best = 0;
                for (int i = 0; i < values.length; i++) {
                    long timeout = Long.parseLong(values[i].toString());
                    if (currentTimeout >= timeout) {
                        best = i;
                    }
                }
                summary = preference.getContext().getString(R.string.screen_timeout_summary, entries[best]);
            }
        }
        preference.setSummary(summary);
    }

    private void disableUnusableTimeouts(ListPreference screenTimeoutPreference) {
        DevicePolicyManager dpm = (DevicePolicyManager) getActivity().getSystemService("device_policy");
        long maxTimeout = dpm != null ? dpm.getMaximumTimeToLock(null) : 0L;
        if (maxTimeout != 0) {
            CharSequence[] entries = screenTimeoutPreference.getEntries();
            CharSequence[] values = screenTimeoutPreference.getEntryValues();
            ArrayList<CharSequence> revisedEntries = new ArrayList<>();
            ArrayList<CharSequence> revisedValues = new ArrayList<>();
            for (int i = 0; i < values.length; i++) {
                long timeout = Long.parseLong(values[i].toString());
                if (timeout <= maxTimeout) {
                    revisedEntries.add(entries[i]);
                    revisedValues.add(values[i]);
                }
            }
            if (revisedEntries.size() != entries.length || revisedValues.size() != values.length) {
                int userPreference = Integer.parseInt(screenTimeoutPreference.getValue());
                screenTimeoutPreference.setEntries((CharSequence[]) revisedEntries.toArray(new CharSequence[revisedEntries.size()]));
                screenTimeoutPreference.setEntryValues((CharSequence[]) revisedValues.toArray(new CharSequence[revisedValues.size()]));
                if (userPreference <= maxTimeout) {
                    screenTimeoutPreference.setValue(String.valueOf(userPreference));
                } else if (revisedValues.size() > 0 && Long.parseLong(revisedValues.get(revisedValues.size() - 1).toString()) == maxTimeout) {
                    screenTimeoutPreference.setValue(String.valueOf(maxTimeout));
                }
            }
            screenTimeoutPreference.setEnabled(revisedEntries.size() > 0);
        }
    }

    int floatToIndex(float val) {
        String[] indices = getResources().getStringArray(R.array.entryvalues_font_size);
        float lastVal = Float.parseFloat(indices[0]);
        for (int i = 1; i < indices.length; i++) {
            float thisVal = Float.parseFloat(indices[i]);
            if (val < ((thisVal - lastVal) * 0.5f) + lastVal) {
                return i - 1;
            }
            lastVal = thisVal;
        }
        return indices.length - 1;
    }

    public void readFontSizePreference(ListPreference pref) {
        try {
            this.mCurConfig.updateFrom(ActivityManagerNative.getDefault().getConfiguration());
        } catch (RemoteException e) {
            Log.w("DisplaySettings", "Unable to retrieve font size");
        }
        int index = floatToIndex(this.mCurConfig.fontScale);
        pref.setValueIndex(index);
        Resources res = getResources();
        String[] fontSizeNames = res.getStringArray(R.array.entries_font_size);
        pref.setSummary(String.format(res.getString(R.string.summary_font_size), fontSizeNames[index]));
    }

    @Override
    public void onResume() {
        super.onResume();
        updateState();
    }

    @Override
    public Dialog onCreateDialog(int dialogId) {
        if (dialogId == 1) {
            return Utils.buildGlobalChangeWarningDialog(getActivity(), R.string.global_font_change_title, new Runnable() {
                @Override
                public void run() {
                    DisplaySettings.this.mFontSizePref.click();
                }
            });
        }
        return null;
    }

    private void updateState() {
        readFontSizePreference(this.mFontSizePref);
        updateScreenSaverSummary();
        if (this.mAutoBrightnessPreference != null) {
            int brightnessMode = Settings.System.getInt(getContentResolver(), "screen_brightness_mode", 0);
            this.mAutoBrightnessPreference.setChecked(brightnessMode != 0);
        }
        if (this.mLiftToWakePreference != null) {
            int value = Settings.Secure.getInt(getContentResolver(), "wake_gesture_enabled", 0);
            this.mLiftToWakePreference.setChecked(value != 0);
        }
        if (this.mDozePreference != null) {
            int value2 = Settings.Secure.getInt(getContentResolver(), "doze_enabled", 1);
            this.mDozePreference.setChecked(value2 != 0);
        }
    }

    private void updateScreenSaverSummary() {
        if (this.mScreenSaverPreference != null) {
            this.mScreenSaverPreference.setSummary(DreamSettings.getSummaryTextWithDreamName(getActivity()));
        }
    }

    public void writeFontSizePreference(Object objValue) {
        try {
            this.mCurConfig.fontScale = Float.parseFloat(objValue.toString());
            ActivityManagerNative.getDefault().updatePersistentConfiguration(this.mCurConfig);
        } catch (RemoteException e) {
            Log.w("DisplaySettings", "Unable to save font size");
        }
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        return super.onPreferenceTreeClick(preferenceScreen, preference);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object objValue) {
        String key = preference.getKey();
        if ("screen_timeout".equals(key)) {
            try {
                int value = Integer.parseInt((String) objValue);
                Settings.System.putInt(getContentResolver(), "screen_off_timeout", value);
                updateTimeoutPreferenceDescription(value);
            } catch (NumberFormatException e) {
                Log.e("DisplaySettings", "could not persist screen timeout setting", e);
            }
        }
        if ("font_size".equals(key)) {
            writeFontSizePreference(objValue);
        }
        if (preference == this.mAutoBrightnessPreference) {
            boolean auto = ((Boolean) objValue).booleanValue();
            Settings.System.putInt(getContentResolver(), "screen_brightness_mode", auto ? 1 : 0);
        }
        if (preference == this.mLiftToWakePreference) {
            Settings.Secure.putInt(getContentResolver(), "wake_gesture_enabled", ((Boolean) objValue).booleanValue() ? 1 : 0);
        }
        if (preference == this.mDozePreference) {
            Settings.Secure.putInt(getContentResolver(), "doze_enabled", ((Boolean) objValue).booleanValue() ? 1 : 0);
        }
        return true;
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        if (preference == this.mFontSizePref) {
            if (Utils.hasMultipleUsers(getActivity())) {
                showDialog(1);
                return true;
            }
            this.mFontSizePref.click();
        }
        return false;
    }
}
