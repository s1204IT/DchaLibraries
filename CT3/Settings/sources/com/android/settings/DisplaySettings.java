package com.android.settings;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.UiModeManager;
import android.app.admin.DevicePolicyManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.provider.SearchIndexableResource;
import android.provider.Settings;
import android.support.v14.preference.SwitchPreference;
import android.support.v7.preference.DropDownPreference;
import android.support.v7.preference.ListPreference;
import android.support.v7.preference.Preference;
import android.text.TextUtils;
import android.util.Log;
import com.android.internal.logging.MetricsLogger;
import com.android.internal.view.RotationPolicy;
import com.android.settings.accessibility.ToggleFontSizePreferenceFragment;
import com.android.settings.dashboard.SummaryLoader;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.search.Indexable;
import com.android.settingslib.RestrictedLockUtils;
import com.android.settingslib.RestrictedPreference;
import com.mediatek.settings.DisplaySettingsExt;
import com.mediatek.settings.FeatureOption;
import java.util.ArrayList;
import java.util.List;

public class DisplaySettings extends SettingsPreferenceFragment implements Preference.OnPreferenceChangeListener, Indexable {
    private SwitchPreference mAutoBrightnessPreference;
    private SwitchPreference mCameraDoubleTapPowerGesturePreference;
    private SwitchPreference mCameraGesturePreference;
    private Preference mCustomFontSizePref;
    private DisplaySettingsExt mDisplaySettingsExt;
    private SwitchPreference mDozePreference;
    private Preference mFontSizePref;
    private SwitchPreference mLiftToWakePreference;
    private ListPreference mNightModePreference;
    private PackageManager mPm;
    private Preference mScreenSaverPreference;
    private TimeoutListPreference mScreenTimeoutPreference;
    private SwitchPreference mTapToWakePreference;
    public static final SummaryLoader.SummaryProviderFactory SUMMARY_PROVIDER_FACTORY = new SummaryLoader.SummaryProviderFactory() {
        @Override
        public SummaryLoader.SummaryProvider createSummaryProvider(Activity activity, SummaryLoader summaryLoader) {
            return new SummaryProvider(activity, summaryLoader, null);
        }
    };
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
            if (!context.getResources().getBoolean(android.R.^attr-private.iconfactoryIconSize) || FeatureOption.MTK_GMO_RAM_OPTIMIZE) {
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
            if (!DisplaySettings.isTapToWakeAvailable(context.getResources())) {
                result.add("tap_to_wake");
            }
            if (!DisplaySettings.isCameraGestureAvailable(context.getResources())) {
                result.add("camera_gesture");
            }
            if (!DisplaySettings.isCameraDoubleTapPowerGestureAvailable(context.getResources())) {
                result.add("camera_double_tap_power_gesture");
            }
            if (!DisplaySettings.isVrDisplayModeAvailable(context)) {
                result.add("vr_display_pref");
            }
            return result;
        }
    };

    @Override
    protected int getMetricsCategory() {
        return 46;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        int rotateLockedResourceId;
        super.onCreate(savedInstanceState);
        final Activity activity = getActivity();
        activity.getContentResolver();
        this.mPm = getPackageManager();
        addPreferencesFromResource(R.xml.display_settings);
        this.mScreenSaverPreference = findPreference("screensaver");
        if (this.mScreenSaverPreference != null && !getResources().getBoolean(android.R.^attr-private.iconfactoryIconSize)) {
            getPreferenceScreen().removePreference(this.mScreenSaverPreference);
        }
        this.mScreenTimeoutPreference = (TimeoutListPreference) findPreference("screen_timeout");
        this.mFontSizePref = findPreference("font_size");
        this.mCustomFontSizePref = findPreference("custom_font_size");
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
        if (isTapToWakeAvailable(getResources())) {
            this.mTapToWakePreference = (SwitchPreference) findPreference("tap_to_wake");
            this.mTapToWakePreference.setOnPreferenceChangeListener(this);
        } else {
            removePreference("tap_to_wake");
        }
        if (isCameraGestureAvailable(getResources())) {
            this.mCameraGesturePreference = (SwitchPreference) findPreference("camera_gesture");
            this.mCameraGesturePreference.setOnPreferenceChangeListener(this);
        } else {
            removePreference("camera_gesture");
        }
        if (isCameraDoubleTapPowerGestureAvailable(getResources())) {
            this.mCameraDoubleTapPowerGesturePreference = (SwitchPreference) findPreference("camera_double_tap_power_gesture");
            this.mCameraDoubleTapPowerGesturePreference.setOnPreferenceChangeListener(this);
        } else {
            removePreference("camera_double_tap_power_gesture");
        }
        if (!this.mPm.hasSystemFeature("android.hardware.wifi.direct")) {
            getPreferenceScreen().removePreference(findPreference("wifi_display"));
        }
        this.mDisplaySettingsExt = new DisplaySettingsExt(getActivity());
        this.mDisplaySettingsExt.onCreate(getPreferenceScreen());
        if (RotationPolicy.isRotationLockToggleVisible(activity)) {
            DropDownPreference rotatePreference = (DropDownPreference) findPreference("auto_rotate");
            if (allowAllRotations(activity)) {
                rotateLockedResourceId = R.string.display_auto_rotate_stay_in_current;
            } else if (RotationPolicy.getRotationLockOrientation(activity) == 1) {
                rotateLockedResourceId = R.string.display_auto_rotate_stay_in_portrait;
            } else {
                rotateLockedResourceId = R.string.display_auto_rotate_stay_in_landscape;
            }
            rotatePreference.setEntries(new CharSequence[]{activity.getString(R.string.display_auto_rotate_rotate), activity.getString(rotateLockedResourceId)});
            rotatePreference.setEntryValues(new CharSequence[]{"0", "1"});
            this.mDisplaySettingsExt.setRotatePreference(rotatePreference);
            rotatePreference.setValueIndex(RotationPolicy.isRotationLocked(activity) ? 1 : 0);
            rotatePreference.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    boolean locked = Integer.parseInt((String) newValue) != 0;
                    MetricsLogger.action(DisplaySettings.this.getActivity(), 203, locked);
                    RotationPolicy.setRotationLock(activity, locked);
                    return true;
                }
            });
        } else {
            removePreference("auto_rotate");
        }
        if (isVrDisplayModeAvailable(activity)) {
            DropDownPreference vrDisplayPref = (DropDownPreference) findPreference("vr_display_pref");
            vrDisplayPref.setEntries(new CharSequence[]{activity.getString(R.string.display_vr_pref_low_persistence), activity.getString(R.string.display_vr_pref_off)});
            vrDisplayPref.setEntryValues(new CharSequence[]{"0", "1"});
            int currentUser = ActivityManager.getCurrentUser();
            int current = Settings.Secure.getIntForUser(activity.getContentResolver(), "vr_display_mode", 0, currentUser);
            vrDisplayPref.setValueIndex(current);
            vrDisplayPref.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    int i = Integer.parseInt((String) newValue);
                    int u = ActivityManager.getCurrentUser();
                    if (!Settings.Secure.putIntForUser(activity.getContentResolver(), "vr_display_mode", i, u)) {
                        Log.e("DisplaySettings", "Could not change setting for vr_display_mode");
                        return true;
                    }
                    return true;
                }
            });
        } else {
            removePreference("vr_display_pref");
        }
        this.mNightModePreference = (ListPreference) findPreference("night_mode");
        if (this.mNightModePreference == null) {
            return;
        }
        UiModeManager uiManager = (UiModeManager) getSystemService("uimode");
        int currentNightMode = uiManager.getNightMode();
        this.mNightModePreference.setValue(String.valueOf(currentNightMode));
        this.mNightModePreference.setOnPreferenceChangeListener(this);
    }

    private static boolean allowAllRotations(Context context) {
        return Resources.getSystem().getBoolean(android.R.^attr-private.dayHighlightColor);
    }

    public static boolean isLiftToWakeAvailable(Context context) {
        SensorManager sensors = (SensorManager) context.getSystemService("sensor");
        return (sensors == null || sensors.getDefaultSensor(23) == null) ? false : true;
    }

    public static boolean isDozeAvailable(Context context) {
        String name = Build.IS_DEBUGGABLE ? SystemProperties.get("debug.doze.component") : null;
        if (TextUtils.isEmpty(name)) {
            name = context.getResources().getString(android.R.string.CwMmi);
        }
        return !TextUtils.isEmpty(name);
    }

    public static boolean isTapToWakeAvailable(Resources res) {
        return res.getBoolean(android.R.^attr-private.notificationHeaderIconSize);
    }

    public static boolean isAutomaticBrightnessAvailable(Resources res) {
        return res.getBoolean(android.R.^attr-private.calendarViewMode);
    }

    public static boolean isCameraGestureAvailable(Resources res) {
        boolean configSet = res.getInteger(android.R.integer.config_extraFreeKbytesAbsolute) != -1;
        return configSet && !SystemProperties.getBoolean("gesture.disable_camera_launch", false);
    }

    public static boolean isCameraDoubleTapPowerGestureAvailable(Resources res) {
        return res.getBoolean(android.R.^attr-private.paddingBottomNoButtons);
    }

    public static boolean isVrDisplayModeAvailable(Context context) {
        PackageManager pm = context.getPackageManager();
        return pm.hasSystemFeature("android.hardware.vr.high_performance");
    }

    private void updateTimeoutPreferenceDescription(long currentTimeout) {
        String summary;
        TimeoutListPreference preference = this.mScreenTimeoutPreference;
        if (preference.isDisabledByAdmin()) {
            summary = getString(R.string.disabled_by_policy_title);
        } else if (currentTimeout < 0) {
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
                summary = getString(R.string.screen_timeout_summary, new Object[]{entries[best]});
            }
        }
        preference.setSummary(summary);
    }

    @Override
    public void onResume() {
        super.onResume();
        updateState();
        long currentTimeout = Settings.System.getLong(getActivity().getContentResolver(), "screen_off_timeout", 30000L);
        this.mScreenTimeoutPreference.setValue(String.valueOf(currentTimeout));
        this.mScreenTimeoutPreference.setOnPreferenceChangeListener(this);
        DevicePolicyManager dpm = (DevicePolicyManager) getActivity().getSystemService("device_policy");
        if (dpm != null) {
            RestrictedLockUtils.EnforcedAdmin admin = RestrictedLockUtils.checkIfMaximumTimeToLockIsSet(getActivity());
            long maxTimeout = dpm.getMaximumTimeToLockForUserAndProfiles(UserHandle.myUserId());
            this.mScreenTimeoutPreference.removeUnusableTimeouts(maxTimeout, admin);
        }
        updateTimeoutPreferenceDescription(currentTimeout);
        disablePreferenceIfManaged("wallpaper", "no_set_wallpaper");
        this.mDisplaySettingsExt.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        this.mDisplaySettingsExt.onPause();
    }

    private void updateState() {
        updateFontSizeSummary();
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
        if (this.mTapToWakePreference != null) {
            int value3 = Settings.Secure.getInt(getContentResolver(), "double_tap_to_wake", 0);
            this.mTapToWakePreference.setChecked(value3 != 0);
        }
        if (this.mCameraGesturePreference != null) {
            int value4 = Settings.Secure.getInt(getContentResolver(), "camera_gesture_disabled", 0);
            this.mCameraGesturePreference.setChecked(value4 == 0);
        }
        if (this.mCameraDoubleTapPowerGesturePreference == null) {
            return;
        }
        int value5 = Settings.Secure.getInt(getContentResolver(), "camera_double_tap_power_gesture_disabled", 0);
        this.mCameraDoubleTapPowerGesturePreference.setChecked(value5 == 0);
    }

    private void updateScreenSaverSummary() {
        if (this.mScreenSaverPreference == null) {
            return;
        }
        this.mScreenSaverPreference.setSummary(DreamSettings.getSummaryTextWithDreamName(getActivity()));
    }

    private void updateFontSizeSummary() {
        Context context = this.mDisplaySettingsExt.isCustomPrefPresent() ? this.mCustomFontSizePref.getContext() : this.mFontSizePref.getContext();
        float currentScale = Settings.System.getFloat(context.getContentResolver(), "font_scale", 1.0f);
        Resources res = context.getResources();
        String[] entries = this.mDisplaySettingsExt.getFontEntries(res.getStringArray(R.array.entries_font_size));
        String[] strEntryValues = this.mDisplaySettingsExt.getFontEntryValues(res.getStringArray(R.array.entryvalues_font_size));
        int index = ToggleFontSizePreferenceFragment.fontSizeValueToIndex(currentScale, strEntryValues);
        if (!this.mDisplaySettingsExt.isCustomPrefPresent()) {
            this.mFontSizePref.setSummary(entries[index]);
        } else {
            this.mCustomFontSizePref.setSummary(entries[index]);
        }
    }

    @Override
    public boolean onPreferenceTreeClick(Preference preference) {
        this.mDisplaySettingsExt.onPreferenceClick(preference);
        return super.onPreferenceTreeClick(preference);
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
        if (preference == this.mTapToWakePreference) {
            Settings.Secure.putInt(getContentResolver(), "double_tap_to_wake", ((Boolean) objValue).booleanValue() ? 1 : 0);
        }
        if (preference == this.mCameraGesturePreference) {
            Settings.Secure.putInt(getContentResolver(), "camera_gesture_disabled", ((Boolean) objValue).booleanValue() ? 0 : 1);
        }
        if (preference == this.mCameraDoubleTapPowerGesturePreference) {
            Settings.Secure.putInt(getContentResolver(), "camera_double_tap_power_gesture_disabled", ((Boolean) objValue).booleanValue() ? 0 : 1);
        }
        if (preference == this.mNightModePreference) {
            try {
                int value2 = Integer.parseInt((String) objValue);
                UiModeManager uiManager = (UiModeManager) getSystemService("uimode");
                uiManager.setNightMode(value2);
            } catch (NumberFormatException e2) {
                Log.e("DisplaySettings", "could not persist night mode setting", e2);
            }
        }
        return true;
    }

    @Override
    protected int getHelpResource() {
        return R.string.help_uri_display;
    }

    private void disablePreferenceIfManaged(String key, String restriction) {
        RestrictedPreference pref = (RestrictedPreference) findPreference(key);
        if (pref == null) {
            return;
        }
        pref.setDisabledByAdmin(null);
        if (RestrictedLockUtils.hasBaseUserRestriction(getActivity(), restriction, UserHandle.myUserId())) {
            pref.setEnabled(false);
        } else {
            pref.checkRestrictionAndSetDisabled(restriction);
        }
    }

    private static class SummaryProvider implements SummaryLoader.SummaryProvider {
        private final Context mContext;
        private final SummaryLoader mLoader;

        SummaryProvider(Context context, SummaryLoader loader, SummaryProvider summaryProvider) {
            this(context, loader);
        }

        private SummaryProvider(Context context, SummaryLoader loader) {
            this.mContext = context;
            this.mLoader = loader;
        }

        @Override
        public void setListening(boolean listening) {
            if (!listening) {
                return;
            }
            updateSummary();
        }

        private void updateSummary() {
            boolean auto = Settings.System.getInt(this.mContext.getContentResolver(), "screen_brightness_mode", 1) == 1;
            this.mLoader.setSummary(this, this.mContext.getString(auto ? R.string.display_summary_on : R.string.display_summary_off));
        }
    }
}
