package com.android.settings.accessibility;

import android.accessibilityservice.AccessibilityServiceInfo;
import android.app.ActivityManagerNative;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.RemoteException;
import android.os.UserHandle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceScreen;
import android.preference.SwitchPreference;
import android.provider.SearchIndexableResource;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.KeyCharacterMap;
import android.view.accessibility.AccessibilityManager;
import com.android.internal.content.PackageMonitor;
import com.android.internal.view.RotationPolicy;
import com.android.settings.DialogCreatable;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.Utils;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.search.Indexable;
import com.android.settings.search.SearchIndexableRaw;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class AccessibilitySettings extends SettingsPreferenceFragment implements Preference.OnPreferenceChangeListener, DialogCreatable, Indexable {
    private PreferenceScreen mCaptioningPreferenceScreen;
    private PreferenceScreen mDisplayDaltonizerPreferenceScreen;
    private PreferenceScreen mDisplayMagnificationPreferenceScreen;
    private DevicePolicyManager mDpm;
    private PreferenceScreen mGlobalGesturePreferenceScreen;
    private int mLongPressTimeoutDefault;
    private Preference mNoServicesMessagePreference;
    private ListPreference mSelectLongPressTimeoutPreference;
    private PreferenceCategory mServicesCategory;
    private PreferenceCategory mSystemsCategory;
    private SwitchPreference mToggleHighTextContrastPreference;
    private SwitchPreference mToggleInversionPreference;
    private SwitchPreference mToggleLargeTextPreference;
    private SwitchPreference mToggleLockScreenRotationPreference;
    private SwitchPreference mTogglePowerButtonEndsCallPreference;
    private SwitchPreference mToggleSpeakPasswordPreference;
    static final TextUtils.SimpleStringSplitter sStringColonSplitter = new TextUtils.SimpleStringSplitter(':');
    static final Set<ComponentName> sInstalledServices = new HashSet();
    public static final Indexable.SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER = new BaseSearchIndexProvider() {
        @Override
        public List<SearchIndexableRaw> getRawDataToIndex(Context context, boolean enabled) {
            List<SearchIndexableRaw> indexables = new ArrayList<>();
            PackageManager packageManager = context.getPackageManager();
            AccessibilityManager accessibilityManager = (AccessibilityManager) context.getSystemService("accessibility");
            String screenTitle = context.getResources().getString(R.string.accessibility_services_title);
            List<AccessibilityServiceInfo> services = accessibilityManager.getInstalledAccessibilityServiceList();
            int serviceCount = services.size();
            for (int i = 0; i < serviceCount; i++) {
                AccessibilityServiceInfo service = services.get(i);
                if (service != null && service.getResolveInfo() != null) {
                    ServiceInfo serviceInfo = service.getResolveInfo().serviceInfo;
                    ComponentName componentName = new ComponentName(serviceInfo.packageName, serviceInfo.name);
                    SearchIndexableRaw indexable = new SearchIndexableRaw(context);
                    indexable.key = componentName.flattenToString();
                    indexable.title = service.getResolveInfo().loadLabel(packageManager).toString();
                    indexable.summaryOn = context.getString(R.string.accessibility_feature_state_on);
                    indexable.summaryOff = context.getString(R.string.accessibility_feature_state_off);
                    indexable.screenTitle = screenTitle;
                    indexables.add(indexable);
                }
            }
            return indexables;
        }

        @Override
        public List<SearchIndexableResource> getXmlResourcesToIndex(Context context, boolean enabled) {
            List<SearchIndexableResource> indexables = new ArrayList<>();
            SearchIndexableResource indexable = new SearchIndexableResource(context);
            indexable.xmlResId = R.xml.accessibility_settings;
            indexables.add(indexable);
            return indexables;
        }
    };
    private final Map<String, String> mLongPressTimeoutValuetoTitleMap = new HashMap();
    private final Configuration mCurConfig = new Configuration();
    private final Handler mHandler = new Handler();
    private final Runnable mUpdateRunnable = new Runnable() {
        @Override
        public void run() {
            AccessibilitySettings.this.loadInstalledServices();
            AccessibilitySettings.this.updateServicesPreferences();
        }
    };
    private final PackageMonitor mSettingsPackageMonitor = new PackageMonitor() {
        public void onPackageAdded(String packageName, int uid) {
            sendUpdate();
        }

        public void onPackageAppeared(String packageName, int reason) {
            sendUpdate();
        }

        public void onPackageDisappeared(String packageName, int reason) {
            sendUpdate();
        }

        public void onPackageRemoved(String packageName, int uid) {
            sendUpdate();
        }

        private void sendUpdate() {
            AccessibilitySettings.this.mHandler.postDelayed(AccessibilitySettings.this.mUpdateRunnable, 1000L);
        }
    };
    private final SettingsContentObserver mSettingsContentObserver = new SettingsContentObserver(this.mHandler) {
        @Override
        public void onChange(boolean selfChange, Uri uri) {
            AccessibilitySettings.this.loadInstalledServices();
            AccessibilitySettings.this.updateServicesPreferences();
        }
    };
    private final RotationPolicy.RotationPolicyListener mRotationPolicyListener = new RotationPolicy.RotationPolicyListener() {
        public void onChange() {
            AccessibilitySettings.this.updateLockScreenRotationCheckbox();
        }
    };

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        addPreferencesFromResource(R.xml.accessibility_settings);
        initializeAllPreferences();
        this.mDpm = (DevicePolicyManager) getActivity().getSystemService("device_policy");
    }

    @Override
    public void onResume() {
        super.onResume();
        loadInstalledServices();
        updateAllPreferences();
        this.mSettingsPackageMonitor.register(getActivity(), getActivity().getMainLooper(), false);
        this.mSettingsContentObserver.register(getContentResolver());
        if (RotationPolicy.isRotationSupported(getActivity())) {
            RotationPolicy.registerRotationPolicyListener(getActivity(), this.mRotationPolicyListener);
        }
    }

    @Override
    public void onPause() {
        this.mSettingsPackageMonitor.unregister();
        this.mSettingsContentObserver.unregister(getContentResolver());
        if (RotationPolicy.isRotationSupported(getActivity())) {
            RotationPolicy.unregisterRotationPolicyListener(getActivity(), this.mRotationPolicyListener);
        }
        super.onPause();
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (this.mSelectLongPressTimeoutPreference == preference) {
            handleLongPressTimeoutPreferenceChange((String) newValue);
            return true;
        }
        if (this.mToggleInversionPreference == preference) {
            handleToggleInversionPreferenceChange(((Boolean) newValue).booleanValue());
            return true;
        }
        return false;
    }

    private void handleLongPressTimeoutPreferenceChange(String stringValue) {
        Settings.Secure.putInt(getContentResolver(), "long_press_timeout", Integer.parseInt(stringValue));
        this.mSelectLongPressTimeoutPreference.setSummary(this.mLongPressTimeoutValuetoTitleMap.get(stringValue));
    }

    private void handleToggleInversionPreferenceChange(boolean checked) {
        Settings.Secure.putInt(getContentResolver(), "accessibility_display_inversion_enabled", checked ? 1 : 0);
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        if (this.mToggleLargeTextPreference == preference) {
            handleToggleLargeTextPreferenceClick();
            return true;
        }
        if (this.mToggleHighTextContrastPreference == preference) {
            handleToggleTextContrastPreferenceClick();
            return true;
        }
        if (this.mTogglePowerButtonEndsCallPreference == preference) {
            handleTogglePowerButtonEndsCallPreferenceClick();
            return true;
        }
        if (this.mToggleLockScreenRotationPreference == preference) {
            handleLockScreenRotationPreferenceClick();
            return true;
        }
        if (this.mToggleSpeakPasswordPreference == preference) {
            handleToggleSpeakPasswordPreferenceClick();
            return true;
        }
        if (this.mGlobalGesturePreferenceScreen == preference) {
            handleToggleEnableAccessibilityGesturePreferenceClick();
            return true;
        }
        if (this.mDisplayMagnificationPreferenceScreen == preference) {
            handleDisplayMagnificationPreferenceScreenClick();
            return true;
        }
        return super.onPreferenceTreeClick(preferenceScreen, preference);
    }

    private void handleToggleLargeTextPreferenceClick() {
        try {
            this.mCurConfig.fontScale = this.mToggleLargeTextPreference.isChecked() ? 1.3f : 1.0f;
            ActivityManagerNative.getDefault().updatePersistentConfiguration(this.mCurConfig);
        } catch (RemoteException e) {
        }
    }

    private void handleToggleTextContrastPreferenceClick() {
        Settings.Secure.putInt(getContentResolver(), "high_text_contrast_enabled", this.mToggleHighTextContrastPreference.isChecked() ? 1 : 0);
    }

    private void handleTogglePowerButtonEndsCallPreferenceClick() {
        Settings.Secure.putInt(getContentResolver(), "incall_power_button_behavior", this.mTogglePowerButtonEndsCallPreference.isChecked() ? 2 : 1);
    }

    private void handleLockScreenRotationPreferenceClick() {
        RotationPolicy.setRotationLockForAccessibility(getActivity(), !this.mToggleLockScreenRotationPreference.isChecked());
    }

    private void handleToggleSpeakPasswordPreferenceClick() {
        Settings.Secure.putInt(getContentResolver(), "speak_password", this.mToggleSpeakPasswordPreference.isChecked() ? 1 : 0);
    }

    private void handleToggleEnableAccessibilityGesturePreferenceClick() {
        Bundle extras = this.mGlobalGesturePreferenceScreen.getExtras();
        extras.putString("title", getString(R.string.accessibility_global_gesture_preference_title));
        extras.putString("summary", getString(R.string.accessibility_global_gesture_preference_description));
        extras.putBoolean("checked", Settings.Global.getInt(getContentResolver(), "enable_accessibility_global_gesture_enabled", 0) == 1);
        super.onPreferenceTreeClick(this.mGlobalGesturePreferenceScreen, this.mGlobalGesturePreferenceScreen);
    }

    private void handleDisplayMagnificationPreferenceScreenClick() {
        Bundle extras = this.mDisplayMagnificationPreferenceScreen.getExtras();
        extras.putString("title", getString(R.string.accessibility_screen_magnification_title));
        extras.putCharSequence("summary", getActivity().getResources().getText(R.string.accessibility_screen_magnification_summary));
        extras.putBoolean("checked", Settings.Secure.getInt(getContentResolver(), "accessibility_display_magnification_enabled", 0) == 1);
        super.onPreferenceTreeClick(this.mDisplayMagnificationPreferenceScreen, this.mDisplayMagnificationPreferenceScreen);
    }

    private void initializeAllPreferences() {
        this.mServicesCategory = (PreferenceCategory) findPreference("services_category");
        this.mSystemsCategory = (PreferenceCategory) findPreference("system_category");
        this.mToggleLargeTextPreference = (SwitchPreference) findPreference("toggle_large_text_preference");
        this.mToggleHighTextContrastPreference = (SwitchPreference) findPreference("toggle_high_text_contrast_preference");
        this.mToggleInversionPreference = (SwitchPreference) findPreference("toggle_inversion_preference");
        this.mToggleInversionPreference.setOnPreferenceChangeListener(this);
        this.mTogglePowerButtonEndsCallPreference = (SwitchPreference) findPreference("toggle_power_button_ends_call_preference");
        if (!KeyCharacterMap.deviceHasKey(26) || !Utils.isVoiceCapable(getActivity())) {
            this.mSystemsCategory.removePreference(this.mTogglePowerButtonEndsCallPreference);
        }
        this.mToggleLockScreenRotationPreference = (SwitchPreference) findPreference("toggle_lock_screen_rotation_preference");
        if (!RotationPolicy.isRotationSupported(getActivity())) {
            this.mSystemsCategory.removePreference(this.mToggleLockScreenRotationPreference);
        }
        this.mToggleSpeakPasswordPreference = (SwitchPreference) findPreference("toggle_speak_password_preference");
        this.mSelectLongPressTimeoutPreference = (ListPreference) findPreference("select_long_press_timeout_preference");
        this.mSelectLongPressTimeoutPreference.setOnPreferenceChangeListener(this);
        if (this.mLongPressTimeoutValuetoTitleMap.size() == 0) {
            String[] timeoutValues = getResources().getStringArray(R.array.long_press_timeout_selector_values);
            this.mLongPressTimeoutDefault = Integer.parseInt(timeoutValues[0]);
            String[] timeoutTitles = getResources().getStringArray(R.array.long_press_timeout_selector_titles);
            int timeoutValueCount = timeoutValues.length;
            for (int i = 0; i < timeoutValueCount; i++) {
                this.mLongPressTimeoutValuetoTitleMap.put(timeoutValues[i], timeoutTitles[i]);
            }
        }
        this.mCaptioningPreferenceScreen = (PreferenceScreen) findPreference("captioning_preference_screen");
        this.mDisplayMagnificationPreferenceScreen = (PreferenceScreen) findPreference("screen_magnification_preference_screen");
        this.mDisplayDaltonizerPreferenceScreen = (PreferenceScreen) findPreference("daltonizer_preference_screen");
        this.mGlobalGesturePreferenceScreen = (PreferenceScreen) findPreference("enable_global_gesture_preference_screen");
        int longPressOnPowerBehavior = getActivity().getResources().getInteger(android.R.integer.config_burnInProtectionMinVerticalOffset);
        if (!KeyCharacterMap.deviceHasKey(26) || longPressOnPowerBehavior != 1) {
            this.mSystemsCategory.removePreference(this.mGlobalGesturePreferenceScreen);
        }
    }

    private void updateAllPreferences() {
        updateServicesPreferences();
        updateSystemPreferences();
    }

    private void updateServicesPreferences() {
        String serviceEnabledString;
        String summaryString;
        this.mServicesCategory.removeAll();
        AccessibilityManager accessibilityManager = AccessibilityManager.getInstance(getActivity());
        List<AccessibilityServiceInfo> installedServices = accessibilityManager.getInstalledAccessibilityServiceList();
        Set<ComponentName> enabledServices = AccessibilityUtils.getEnabledServicesFromSettings(getActivity());
        List<String> permittedServices = this.mDpm.getPermittedAccessibilityServices(UserHandle.myUserId());
        boolean accessibilityEnabled = Settings.Secure.getInt(getContentResolver(), "accessibility_enabled", 0) == 1;
        int count = installedServices.size();
        for (int i = 0; i < count; i++) {
            AccessibilityServiceInfo info = installedServices.get(i);
            PreferenceScreen preference = getPreferenceManager().createPreferenceScreen(getActivity());
            String title = info.getResolveInfo().loadLabel(getPackageManager()).toString();
            ServiceInfo serviceInfo = info.getResolveInfo().serviceInfo;
            ComponentName componentName = new ComponentName(serviceInfo.packageName, serviceInfo.name);
            preference.setKey(componentName.flattenToString());
            preference.setTitle(title);
            boolean serviceEnabled = accessibilityEnabled && enabledServices.contains(componentName);
            if (serviceEnabled) {
                serviceEnabledString = getString(R.string.accessibility_feature_state_on);
            } else {
                serviceEnabledString = getString(R.string.accessibility_feature_state_off);
            }
            String packageName = serviceInfo.packageName;
            boolean serviceAllowed = permittedServices == null || permittedServices.contains(packageName);
            preference.setEnabled(serviceAllowed || serviceEnabled);
            if (serviceAllowed) {
                summaryString = serviceEnabledString;
            } else {
                summaryString = getString(R.string.accessibility_feature_or_input_method_not_allowed);
            }
            preference.setSummary(summaryString);
            preference.setOrder(i);
            preference.setFragment(ToggleAccessibilityServicePreferenceFragment.class.getName());
            preference.setPersistent(true);
            Bundle extras = preference.getExtras();
            extras.putString("preference_key", preference.getKey());
            extras.putBoolean("checked", serviceEnabled);
            extras.putString("title", title);
            String description = info.loadDescription(getPackageManager());
            if (TextUtils.isEmpty(description)) {
                description = getString(R.string.accessibility_service_default_description);
            }
            extras.putString("summary", description);
            String settingsClassName = info.getSettingsActivityName();
            if (!TextUtils.isEmpty(settingsClassName)) {
                extras.putString("settings_title", getString(R.string.accessibility_menu_item_settings));
                extras.putString("settings_component_name", new ComponentName(info.getResolveInfo().serviceInfo.packageName, settingsClassName).flattenToString());
            }
            extras.putParcelable("component_name", componentName);
            this.mServicesCategory.addPreference(preference);
        }
        if (this.mServicesCategory.getPreferenceCount() == 0) {
            if (this.mNoServicesMessagePreference == null) {
                this.mNoServicesMessagePreference = new Preference(getActivity());
                this.mNoServicesMessagePreference.setPersistent(false);
                this.mNoServicesMessagePreference.setLayoutResource(R.layout.text_description_preference);
                this.mNoServicesMessagePreference.setSelectable(false);
                this.mNoServicesMessagePreference.setSummary(getString(R.string.accessibility_no_services_installed));
            }
            this.mServicesCategory.addPreference(this.mNoServicesMessagePreference);
        }
    }

    private void updateSystemPreferences() {
        try {
            this.mCurConfig.updateFrom(ActivityManagerNative.getDefault().getConfiguration());
        } catch (RemoteException e) {
        }
        this.mToggleLargeTextPreference.setChecked(this.mCurConfig.fontScale == 1.3f);
        this.mToggleHighTextContrastPreference.setChecked(Settings.Secure.getInt(getContentResolver(), "high_text_contrast_enabled", 0) == 1);
        this.mToggleInversionPreference.setChecked(Settings.Secure.getInt(getContentResolver(), "accessibility_display_inversion_enabled", 0) == 1);
        if (KeyCharacterMap.deviceHasKey(26) && Utils.isVoiceCapable(getActivity())) {
            int incallPowerBehavior = Settings.Secure.getInt(getContentResolver(), "incall_power_button_behavior", 1);
            boolean powerButtonEndsCall = incallPowerBehavior == 2;
            this.mTogglePowerButtonEndsCallPreference.setChecked(powerButtonEndsCall);
        }
        updateLockScreenRotationCheckbox();
        boolean speakPasswordEnabled = Settings.Secure.getInt(getContentResolver(), "speak_password", 0) != 0;
        this.mToggleSpeakPasswordPreference.setChecked(speakPasswordEnabled);
        int longPressTimeout = Settings.Secure.getInt(getContentResolver(), "long_press_timeout", this.mLongPressTimeoutDefault);
        String value = String.valueOf(longPressTimeout);
        this.mSelectLongPressTimeoutPreference.setValue(value);
        this.mSelectLongPressTimeoutPreference.setSummary(this.mLongPressTimeoutValuetoTitleMap.get(value));
        updateFeatureSummary("accessibility_captioning_enabled", this.mCaptioningPreferenceScreen);
        updateFeatureSummary("accessibility_display_magnification_enabled", this.mDisplayMagnificationPreferenceScreen);
        updateFeatureSummary("accessibility_display_daltonizer_enabled", this.mDisplayDaltonizerPreferenceScreen);
        boolean globalGestureEnabled = Settings.Global.getInt(getContentResolver(), "enable_accessibility_global_gesture_enabled", 0) == 1;
        if (globalGestureEnabled) {
            this.mGlobalGesturePreferenceScreen.setSummary(R.string.accessibility_global_gesture_preference_summary_on);
        } else {
            this.mGlobalGesturePreferenceScreen.setSummary(R.string.accessibility_global_gesture_preference_summary_off);
        }
    }

    private void updateFeatureSummary(String prefKey, Preference pref) {
        boolean enabled = Settings.Secure.getInt(getContentResolver(), prefKey, 0) == 1;
        pref.setSummary(enabled ? R.string.accessibility_feature_state_on : R.string.accessibility_feature_state_off);
    }

    private void updateLockScreenRotationCheckbox() {
        Context context = getActivity();
        if (context != null) {
            this.mToggleLockScreenRotationPreference.setChecked(!RotationPolicy.isRotationLocked(context));
        }
    }

    private void loadInstalledServices() {
        Set<ComponentName> installedServices = sInstalledServices;
        installedServices.clear();
        List<AccessibilityServiceInfo> installedServiceInfos = AccessibilityManager.getInstance(getActivity()).getInstalledAccessibilityServiceList();
        if (installedServiceInfos != null) {
            int installedServiceInfoCount = installedServiceInfos.size();
            for (int i = 0; i < installedServiceInfoCount; i++) {
                ResolveInfo resolveInfo = installedServiceInfos.get(i).getResolveInfo();
                ComponentName installedService = new ComponentName(resolveInfo.serviceInfo.packageName, resolveInfo.serviceInfo.name);
                installedServices.add(installedService);
            }
        }
    }
}
