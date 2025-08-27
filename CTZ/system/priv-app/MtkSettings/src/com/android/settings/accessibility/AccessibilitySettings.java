package com.android.settings.accessibility;

import android.accessibilityservice.AccessibilityServiceInfo;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.UserHandle;
import android.os.Vibrator;
import android.provider.SearchIndexableResource;
import android.provider.Settings;
import android.support.v14.preference.SwitchPreference;
import android.support.v4.content.ContextCompat;
import android.support.v7.preference.ListPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceCategory;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.view.KeyCharacterMap;
import android.view.accessibility.AccessibilityManager;
import com.android.internal.accessibility.AccessibilityShortcutController;
import com.android.internal.content.PackageMonitor;
import com.android.internal.view.RotationPolicy;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.Utils;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.search.Indexable;
import com.android.settingslib.RestrictedLockUtils;
import com.android.settingslib.RestrictedPreference;
import com.android.settingslib.accessibility.AccessibilityUtils;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/* loaded from: classes.dex */
public class AccessibilitySettings extends SettingsPreferenceFragment implements Preference.OnPreferenceChangeListener, Indexable {
    private Preference mAccessibilityShortcutPreferenceScreen;
    private Preference mAutoclickPreferenceScreen;
    private Preference mCaptioningPreferenceScreen;
    private Preference mDisplayDaltonizerPreferenceScreen;
    private Preference mDisplayMagnificationPreferenceScreen;
    private DevicePolicyManager mDpm;
    private Preference mFontSizePreferenceScreen;
    private int mLongPressTimeoutDefault;
    private ListPreference mSelectLongPressTimeoutPreference;
    private final SettingsContentObserver mSettingsContentObserver;
    private SwitchPreference mToggleDisableAnimationsPreference;
    private SwitchPreference mToggleHighTextContrastPreference;
    private SwitchPreference mToggleInversionPreference;
    private SwitchPreference mToggleLargePointerIconPreference;
    private SwitchPreference mToggleLockScreenRotationPreference;
    private SwitchPreference mToggleMasterMonoPreference;
    private SwitchPreference mTogglePowerButtonEndsCallPreference;
    private Preference mVibrationPreferenceScreen;
    private static final String[] CATEGORIES = {"screen_reader_category", "audio_and_captions_category", "display_category", "interaction_control_category", "experimental_category", "user_installed_services_category"};
    private static final String[] TOGGLE_ANIMATION_TARGETS = {"window_animation_scale", "transition_animation_scale", "animator_duration_scale"};
    public static final Indexable.SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER = new BaseSearchIndexProvider() { // from class: com.android.settings.accessibility.AccessibilitySettings.5
        @Override // com.android.settings.search.BaseSearchIndexProvider, com.android.settings.search.Indexable.SearchIndexProvider
        public List<SearchIndexableResource> getXmlResourcesToIndex(Context context, boolean z) {
            ArrayList arrayList = new ArrayList();
            SearchIndexableResource searchIndexableResource = new SearchIndexableResource(context);
            searchIndexableResource.xmlResId = R.xml.accessibility_settings;
            arrayList.add(searchIndexableResource);
            return arrayList;
        }

        @Override // com.android.settings.search.BaseSearchIndexProvider, com.android.settings.search.Indexable.SearchIndexProvider
        public List<String> getNonIndexableKeys(Context context) {
            List<String> nonIndexableKeys = super.getNonIndexableKeys(context);
            nonIndexableKeys.add("font_size_preference_screen");
            nonIndexableKeys.add("accessibility_settings_screen_zoom");
            nonIndexableKeys.add("tts_settings_preference");
            return nonIndexableKeys;
        }
    };
    private final Map<String, String> mLongPressTimeoutValueToTitleMap = new HashMap();
    private final Handler mHandler = new Handler();
    private final Runnable mUpdateRunnable = new Runnable() { // from class: com.android.settings.accessibility.AccessibilitySettings.1
        @Override // java.lang.Runnable
        public void run() throws Resources.NotFoundException {
            if (AccessibilitySettings.this.getActivity() != null) {
                AccessibilitySettings.this.updateServicePreferences();
            }
        }
    };
    private final PackageMonitor mSettingsPackageMonitor = new PackageMonitor() { // from class: com.android.settings.accessibility.AccessibilitySettings.2
        public void onPackageAdded(String str, int i) {
            sendUpdate();
        }

        public void onPackageAppeared(String str, int i) {
            sendUpdate();
        }

        public void onPackageDisappeared(String str, int i) {
            sendUpdate();
        }

        public void onPackageRemoved(String str, int i) {
            sendUpdate();
        }

        private void sendUpdate() {
            AccessibilitySettings.this.mHandler.postDelayed(AccessibilitySettings.this.mUpdateRunnable, 1000L);
        }
    };
    private final RotationPolicy.RotationPolicyListener mRotationPolicyListener = new RotationPolicy.RotationPolicyListener() { // from class: com.android.settings.accessibility.AccessibilitySettings.3
        public void onChange() {
            AccessibilitySettings.this.updateLockScreenRotationCheckbox();
        }
    };
    private final Map<String, PreferenceCategory> mCategoryToPrefCategoryMap = new ArrayMap();
    private final Map<Preference, PreferenceCategory> mServicePreferenceToPreferenceCategoryMap = new ArrayMap();
    private final Map<ComponentName, PreferenceCategory> mPreBundledServiceComponentToCategoryMap = new ArrayMap();

    public static boolean isColorTransformAccelerated(Context context) {
        return context.getResources().getBoolean(android.R.^attr-private.lightRadius);
    }

    public AccessibilitySettings() {
        Collection collectionValues = AccessibilityShortcutController.getFrameworkShortcutFeaturesMap().values();
        ArrayList arrayList = new ArrayList(collectionValues.size());
        Iterator it = collectionValues.iterator();
        while (it.hasNext()) {
            arrayList.add(((AccessibilityShortcutController.ToggleableFrameworkFeatureInfo) it.next()).getSettingKey());
        }
        this.mSettingsContentObserver = new SettingsContentObserver(this.mHandler, arrayList) { // from class: com.android.settings.accessibility.AccessibilitySettings.4
            @Override // android.database.ContentObserver
            public void onChange(boolean z, Uri uri) throws Resources.NotFoundException {
                AccessibilitySettings.this.updateAllPreferences();
            }
        };
    }

    @Override // com.android.settingslib.core.instrumentation.Instrumentable
    public int getMetricsCategory() {
        return 2;
    }

    @Override // com.android.settings.support.actionbar.HelpResourceProvider
    public int getHelpResource() {
        return R.string.help_uri_accessibility;
    }

    @Override // com.android.settings.SettingsPreferenceFragment, com.android.settingslib.core.lifecycle.ObservablePreferenceFragment, android.support.v14.preference.PreferenceFragment, android.app.Fragment
    public void onCreate(Bundle bundle) throws Resources.NotFoundException {
        super.onCreate(bundle);
        addPreferencesFromResource(R.xml.accessibility_settings);
        initializeAllPreferences();
        this.mDpm = (DevicePolicyManager) getActivity().getSystemService("device_policy");
    }

    @Override // com.android.settings.SettingsPreferenceFragment, com.android.settings.core.InstrumentedPreferenceFragment, com.android.settingslib.core.lifecycle.ObservablePreferenceFragment, android.app.Fragment
    public void onResume() throws Resources.NotFoundException {
        super.onResume();
        updateAllPreferences();
        this.mSettingsPackageMonitor.register(getActivity(), getActivity().getMainLooper(), false);
        this.mSettingsContentObserver.register(getContentResolver());
        if (RotationPolicy.isRotationSupported(getActivity())) {
            RotationPolicy.registerRotationPolicyListener(getActivity(), this.mRotationPolicyListener);
        }
    }

    @Override // com.android.settingslib.core.lifecycle.ObservablePreferenceFragment, android.app.Fragment
    public void onPause() {
        this.mSettingsPackageMonitor.unregister();
        this.mSettingsContentObserver.unregister(getContentResolver());
        if (RotationPolicy.isRotationSupported(getActivity())) {
            RotationPolicy.unregisterRotationPolicyListener(getActivity(), this.mRotationPolicyListener);
        }
        super.onPause();
    }

    @Override // android.support.v7.preference.Preference.OnPreferenceChangeListener
    public boolean onPreferenceChange(Preference preference, Object obj) {
        if (this.mSelectLongPressTimeoutPreference == preference) {
            handleLongPressTimeoutPreferenceChange((String) obj);
            return true;
        }
        if (this.mToggleInversionPreference == preference) {
            handleToggleInversionPreferenceChange(((Boolean) obj).booleanValue());
            return true;
        }
        return false;
    }

    private void handleLongPressTimeoutPreferenceChange(String str) {
        Settings.Secure.putInt(getContentResolver(), "long_press_timeout", Integer.parseInt(str));
        this.mSelectLongPressTimeoutPreference.setSummary(this.mLongPressTimeoutValueToTitleMap.get(str));
    }

    private void handleToggleInversionPreferenceChange(boolean z) {
        Settings.Secure.putInt(getContentResolver(), "accessibility_display_inversion_enabled", z ? 1 : 0);
    }

    @Override // android.support.v14.preference.PreferenceFragment, android.support.v7.preference.PreferenceManager.OnPreferenceTreeClickListener
    public boolean onPreferenceTreeClick(Preference preference) {
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
        if (this.mToggleLargePointerIconPreference == preference) {
            handleToggleLargePointerIconPreferenceClick();
            return true;
        }
        if (this.mToggleDisableAnimationsPreference == preference) {
            handleToggleDisableAnimations();
            return true;
        }
        if (this.mToggleMasterMonoPreference == preference) {
            handleToggleMasterMonoPreferenceClick();
            return true;
        }
        return super.onPreferenceTreeClick(preference);
    }

    public static CharSequence getServiceSummary(Context context, AccessibilityServiceInfo accessibilityServiceInfo, boolean z) {
        String string;
        if (z) {
            string = context.getString(R.string.accessibility_summary_state_enabled);
        } else {
            string = context.getString(R.string.accessibility_summary_state_disabled);
        }
        CharSequence charSequenceLoadSummary = accessibilityServiceInfo.loadSummary(context.getPackageManager());
        return TextUtils.isEmpty(charSequenceLoadSummary) ? string : context.getString(R.string.preference_summary_default_combination, string, charSequenceLoadSummary);
    }

    private void handleToggleTextContrastPreferenceClick() {
        Settings.Secure.putInt(getContentResolver(), "high_text_contrast_enabled", this.mToggleHighTextContrastPreference.isChecked() ? 1 : 0);
    }

    private void handleTogglePowerButtonEndsCallPreferenceClick() {
        int i;
        ContentResolver contentResolver = getContentResolver();
        if (this.mTogglePowerButtonEndsCallPreference.isChecked()) {
            i = 2;
        } else {
            i = 1;
        }
        Settings.Secure.putInt(contentResolver, "incall_power_button_behavior", i);
    }

    private void handleLockScreenRotationPreferenceClick() {
        RotationPolicy.setRotationLockForAccessibility(getActivity(), !this.mToggleLockScreenRotationPreference.isChecked());
    }

    private void handleToggleLargePointerIconPreferenceClick() {
        Settings.Secure.putInt(getContentResolver(), "accessibility_large_pointer_icon", this.mToggleLargePointerIconPreference.isChecked() ? 1 : 0);
    }

    private void handleToggleDisableAnimations() {
        String str = this.mToggleDisableAnimationsPreference.isChecked() ? "0" : "1";
        for (String str2 : TOGGLE_ANIMATION_TARGETS) {
            Settings.Global.putString(getContentResolver(), str2, str);
        }
    }

    private void handleToggleMasterMonoPreferenceClick() {
        Settings.System.putIntForUser(getContentResolver(), "master_mono", this.mToggleMasterMonoPreference.isChecked() ? 1 : 0, -2);
    }

    private void initializeAllPreferences() throws Resources.NotFoundException {
        for (int i = 0; i < CATEGORIES.length; i++) {
            this.mCategoryToPrefCategoryMap.put(CATEGORIES[i], (PreferenceCategory) findPreference(CATEGORIES[i]));
        }
        this.mToggleHighTextContrastPreference = (SwitchPreference) findPreference("toggle_high_text_contrast_preference");
        this.mToggleInversionPreference = (SwitchPreference) findPreference("toggle_inversion_preference");
        this.mToggleInversionPreference.setOnPreferenceChangeListener(this);
        this.mTogglePowerButtonEndsCallPreference = (SwitchPreference) findPreference("toggle_power_button_ends_call_preference");
        if (!KeyCharacterMap.deviceHasKey(26) || !Utils.isVoiceCapable(getActivity())) {
            this.mCategoryToPrefCategoryMap.get("interaction_control_category").removePreference(this.mTogglePowerButtonEndsCallPreference);
        }
        this.mToggleLockScreenRotationPreference = (SwitchPreference) findPreference("toggle_lock_screen_rotation_preference");
        if (!RotationPolicy.isRotationSupported(getActivity())) {
            this.mCategoryToPrefCategoryMap.get("interaction_control_category").removePreference(this.mToggleLockScreenRotationPreference);
        }
        this.mToggleLargePointerIconPreference = (SwitchPreference) findPreference("toggle_large_pointer_icon");
        this.mToggleDisableAnimationsPreference = (SwitchPreference) findPreference("toggle_disable_animations");
        this.mToggleMasterMonoPreference = (SwitchPreference) findPreference("toggle_master_mono");
        this.mSelectLongPressTimeoutPreference = (ListPreference) findPreference("select_long_press_timeout_preference");
        this.mSelectLongPressTimeoutPreference.setOnPreferenceChangeListener(this);
        if (this.mLongPressTimeoutValueToTitleMap.size() == 0) {
            String[] stringArray = getResources().getStringArray(R.array.long_press_timeout_selector_values);
            this.mLongPressTimeoutDefault = Integer.parseInt(stringArray[0]);
            String[] stringArray2 = getResources().getStringArray(R.array.long_press_timeout_selector_titles);
            int length = stringArray.length;
            for (int i2 = 0; i2 < length; i2++) {
                this.mLongPressTimeoutValueToTitleMap.put(stringArray[i2], stringArray2[i2]);
            }
        }
        this.mCaptioningPreferenceScreen = findPreference("captioning_preference_screen");
        this.mDisplayMagnificationPreferenceScreen = findPreference("magnification_preference_screen");
        configureMagnificationPreferenceIfNeeded(this.mDisplayMagnificationPreferenceScreen);
        this.mFontSizePreferenceScreen = findPreference("font_size_preference_screen");
        this.mAutoclickPreferenceScreen = findPreference("autoclick_preference_screen");
        this.mDisplayDaltonizerPreferenceScreen = findPreference("daltonizer_preference_screen");
        this.mAccessibilityShortcutPreferenceScreen = findPreference("accessibility_shortcut_preference");
        this.mVibrationPreferenceScreen = findPreference("vibration_preference_screen");
    }

    private void updateAllPreferences() throws Resources.NotFoundException {
        updateSystemPreferences();
        updateServicePreferences();
    }

    protected void updateServicePreferences() throws Resources.NotFoundException {
        Drawable drawableLoadIcon;
        List<AccessibilityServiceInfo> list;
        boolean z;
        PreferenceCategory preferenceCategory;
        ArrayList arrayList = new ArrayList(this.mServicePreferenceToPreferenceCategoryMap.keySet());
        for (int i = 0; i < arrayList.size(); i++) {
            Preference preference = (Preference) arrayList.get(i);
            this.mServicePreferenceToPreferenceCategoryMap.get(preference).removePreference(preference);
        }
        initializePreBundledServicesMapFromArray("screen_reader_category", R.array.config_preinstalled_screen_reader_services);
        initializePreBundledServicesMapFromArray("audio_and_captions_category", R.array.config_preinstalled_audio_and_caption_services);
        initializePreBundledServicesMapFromArray("display_category", R.array.config_preinstalled_display_services);
        initializePreBundledServicesMapFromArray("interaction_control_category", R.array.config_preinstalled_interaction_control_services);
        AccessibilityManager accessibilityManager = AccessibilityManager.getInstance(getActivity());
        List<AccessibilityServiceInfo> installedAccessibilityServiceList = accessibilityManager.getInstalledAccessibilityServiceList();
        List<AccessibilityServiceInfo> enabledAccessibilityServiceList = accessibilityManager.getEnabledAccessibilityServiceList(-1);
        Set<ComponentName> enabledServicesFromSettings = AccessibilityUtils.getEnabledServicesFromSettings(getActivity());
        List permittedAccessibilityServices = this.mDpm.getPermittedAccessibilityServices(UserHandle.myUserId());
        PreferenceCategory preferenceCategory2 = this.mCategoryToPrefCategoryMap.get("user_installed_services_category");
        if (findPreference("user_installed_services_category") == null) {
            getPreferenceScreen().addPreference(preferenceCategory2);
        }
        int size = installedAccessibilityServiceList.size();
        int i2 = 0;
        while (i2 < size) {
            AccessibilityServiceInfo accessibilityServiceInfo = installedAccessibilityServiceList.get(i2);
            ResolveInfo resolveInfo = accessibilityServiceInfo.getResolveInfo();
            RestrictedPreference restrictedPreference = new RestrictedPreference(preferenceCategory2.getContext());
            String string = resolveInfo.loadLabel(getPackageManager()).toString();
            if (resolveInfo.getIconResource() == 0) {
                drawableLoadIcon = ContextCompat.getDrawable(getContext(), R.mipmap.ic_accessibility_generic);
            } else {
                drawableLoadIcon = resolveInfo.loadIcon(getPackageManager());
            }
            ServiceInfo serviceInfo = resolveInfo.serviceInfo;
            String str = serviceInfo.packageName;
            List<AccessibilityServiceInfo> list2 = installedAccessibilityServiceList;
            ComponentName componentName = new ComponentName(str, serviceInfo.name);
            restrictedPreference.setKey(componentName.flattenToString());
            restrictedPreference.setTitle(string);
            Utils.setSafeIcon(restrictedPreference, drawableLoadIcon);
            boolean zContains = enabledServicesFromSettings.contains(componentName);
            String strLoadDescription = accessibilityServiceInfo.loadDescription(getPackageManager());
            if (TextUtils.isEmpty(strLoadDescription)) {
                strLoadDescription = getString(R.string.accessibility_service_default_description);
            }
            if (zContains && AccessibilityUtils.hasServiceCrashed(str, serviceInfo.name, enabledAccessibilityServiceList)) {
                restrictedPreference.setSummary(R.string.accessibility_summary_state_stopped);
                strLoadDescription = getString(R.string.accessibility_description_state_stopped);
            } else {
                restrictedPreference.setSummary(getServiceSummary(getContext(), accessibilityServiceInfo, zContains));
            }
            if (!(permittedAccessibilityServices == null || permittedAccessibilityServices.contains(str)) && !zContains) {
                list = enabledAccessibilityServiceList;
                RestrictedLockUtils.EnforcedAdmin enforcedAdminCheckIfAccessibilityServiceDisallowed = RestrictedLockUtils.checkIfAccessibilityServiceDisallowed(getActivity(), str, UserHandle.myUserId());
                if (enforcedAdminCheckIfAccessibilityServiceDisallowed != null) {
                    restrictedPreference.setDisabledByAdmin(enforcedAdminCheckIfAccessibilityServiceDisallowed);
                } else {
                    restrictedPreference.setEnabled(false);
                }
                z = true;
            } else {
                list = enabledAccessibilityServiceList;
                z = true;
                restrictedPreference.setEnabled(true);
            }
            restrictedPreference.setFragment(ToggleAccessibilityServicePreferenceFragment.class.getName());
            restrictedPreference.setPersistent(z);
            Bundle extras = restrictedPreference.getExtras();
            Set<ComponentName> set = enabledServicesFromSettings;
            extras.putString("preference_key", restrictedPreference.getKey());
            extras.putBoolean("checked", zContains);
            extras.putString("title", string);
            extras.putParcelable("resolve_info", resolveInfo);
            extras.putString("summary", strLoadDescription);
            String settingsActivityName = accessibilityServiceInfo.getSettingsActivityName();
            if (!TextUtils.isEmpty(settingsActivityName)) {
                extras.putString("settings_title", getString(R.string.accessibility_menu_item_settings));
                extras.putString("settings_component_name", new ComponentName(str, settingsActivityName).flattenToString());
            }
            extras.putParcelable("component_name", componentName);
            if (this.mPreBundledServiceComponentToCategoryMap.containsKey(componentName)) {
                preferenceCategory = this.mPreBundledServiceComponentToCategoryMap.get(componentName);
            } else {
                preferenceCategory = preferenceCategory2;
            }
            restrictedPreference.setOrder(-1);
            preferenceCategory.addPreference(restrictedPreference);
            this.mServicePreferenceToPreferenceCategoryMap.put(restrictedPreference, preferenceCategory);
            i2++;
            installedAccessibilityServiceList = list2;
            enabledAccessibilityServiceList = list;
            enabledServicesFromSettings = set;
        }
        if (preferenceCategory2.getPreferenceCount() == 0) {
            getPreferenceScreen().removePreference(preferenceCategory2);
        }
    }

    private void initializePreBundledServicesMapFromArray(String str, int i) throws Resources.NotFoundException {
        String[] stringArray = getResources().getStringArray(i);
        PreferenceCategory preferenceCategory = this.mCategoryToPrefCategoryMap.get(str);
        for (String str2 : stringArray) {
            this.mPreBundledServiceComponentToCategoryMap.put(ComponentName.unflattenFromString(str2), preferenceCategory);
        }
    }

    protected void updateSystemPreferences() {
        if (isColorTransformAccelerated(getContext())) {
            PreferenceCategory preferenceCategory = this.mCategoryToPrefCategoryMap.get("experimental_category");
            PreferenceCategory preferenceCategory2 = this.mCategoryToPrefCategoryMap.get("display_category");
            preferenceCategory.removePreference(this.mToggleInversionPreference);
            preferenceCategory.removePreference(this.mDisplayDaltonizerPreferenceScreen);
            this.mToggleInversionPreference.setOrder(this.mToggleLargePointerIconPreference.getOrder());
            this.mDisplayDaltonizerPreferenceScreen.setOrder(this.mToggleInversionPreference.getOrder());
            this.mToggleInversionPreference.setSummary(R.string.summary_empty);
            preferenceCategory2.addPreference(this.mToggleInversionPreference);
            preferenceCategory2.addPreference(this.mDisplayDaltonizerPreferenceScreen);
        }
        this.mToggleHighTextContrastPreference.setChecked(Settings.Secure.getInt(getContentResolver(), "high_text_contrast_enabled", 0) == 1);
        this.mToggleInversionPreference.setChecked(Settings.Secure.getInt(getContentResolver(), "accessibility_display_inversion_enabled", 0) == 1);
        if (KeyCharacterMap.deviceHasKey(26) && Utils.isVoiceCapable(getActivity())) {
            this.mTogglePowerButtonEndsCallPreference.setChecked(Settings.Secure.getInt(getContentResolver(), "incall_power_button_behavior", 1) == 2);
        }
        updateLockScreenRotationCheckbox();
        this.mToggleLargePointerIconPreference.setChecked(Settings.Secure.getInt(getContentResolver(), "accessibility_large_pointer_icon", 0) != 0);
        updateDisableAnimationsToggle();
        updateMasterMono();
        String strValueOf = String.valueOf(Settings.Secure.getInt(getContentResolver(), "long_press_timeout", this.mLongPressTimeoutDefault));
        this.mSelectLongPressTimeoutPreference.setValue(strValueOf);
        this.mSelectLongPressTimeoutPreference.setSummary(this.mLongPressTimeoutValueToTitleMap.get(strValueOf));
        updateVibrationSummary(this.mVibrationPreferenceScreen);
        updateFeatureSummary("accessibility_captioning_enabled", this.mCaptioningPreferenceScreen);
        updateFeatureSummary("accessibility_display_daltonizer_enabled", this.mDisplayDaltonizerPreferenceScreen);
        updateMagnificationSummary(this.mDisplayMagnificationPreferenceScreen);
        updateFontSizeSummary(this.mFontSizePreferenceScreen);
        updateAutoclickSummary(this.mAutoclickPreferenceScreen);
        updateAccessibilityShortcut(this.mAccessibilityShortcutPreferenceScreen);
    }

    private void updateMagnificationSummary(Preference preference) {
        int i;
        boolean z = Settings.Secure.getInt(getContentResolver(), "accessibility_display_magnification_enabled", 0) == 1;
        boolean z2 = Settings.Secure.getInt(getContentResolver(), "accessibility_display_magnification_navbar_enabled", 0) == 1;
        if (!z && !z2) {
            i = R.string.accessibility_feature_state_off;
        } else if (!z && z2) {
            i = R.string.accessibility_screen_magnification_navbar_title;
        } else if (z && !z2) {
            i = R.string.accessibility_screen_magnification_gestures_title;
        } else {
            i = R.string.accessibility_screen_magnification_state_navbar_gesture;
        }
        preference.setSummary(i);
    }

    private void updateFeatureSummary(String str, Preference preference) {
        preference.setSummary(Settings.Secure.getInt(getContentResolver(), str, 0) == 1 ? R.string.accessibility_feature_state_on : R.string.accessibility_feature_state_off);
    }

    private void updateAutoclickSummary(Preference preference) {
        if (!(Settings.Secure.getInt(getContentResolver(), "accessibility_autoclick_enabled", 0) == 1)) {
            preference.setSummary(R.string.accessibility_feature_state_off);
        } else {
            preference.setSummary(ToggleAutoclickPreferenceFragment.getAutoclickPreferenceSummary(getResources(), Settings.Secure.getInt(getContentResolver(), "accessibility_autoclick_delay", 600)));
        }
    }

    private void updateFontSizeSummary(Preference preference) {
        float f = Settings.System.getFloat(getContext().getContentResolver(), "font_scale", 1.0f);
        Resources resources = getContext().getResources();
        preference.setSummary(resources.getStringArray(R.array.entries_font_size)[ToggleFontSizePreferenceFragment.fontSizeValueToIndex(f, resources.getStringArray(R.array.entryvalues_font_size))]);
    }

    void updateVibrationSummary(Preference preference) {
        Context context = getContext();
        Vibrator vibrator = (Vibrator) context.getSystemService(Vibrator.class);
        int i = Settings.System.getInt(context.getContentResolver(), "notification_vibration_intensity", vibrator.getDefaultNotificationVibrationIntensity());
        CharSequence intensityString = VibrationIntensityPreferenceController.getIntensityString(context, i);
        int i2 = Settings.System.getInt(context.getContentResolver(), "haptic_feedback_intensity", vibrator.getDefaultHapticFeedbackIntensity());
        Object intensityString2 = VibrationIntensityPreferenceController.getIntensityString(context, i2);
        if (this.mVibrationPreferenceScreen == null) {
            this.mVibrationPreferenceScreen = findPreference("vibration_preference_screen");
        }
        if (i == i2) {
            this.mVibrationPreferenceScreen.setSummary(intensityString);
        } else {
            this.mVibrationPreferenceScreen.setSummary(getString(R.string.accessibility_vibration_summary, new Object[]{intensityString, intensityString2}));
        }
    }

    private void updateLockScreenRotationCheckbox() {
        if (getActivity() != null) {
            this.mToggleLockScreenRotationPreference.setChecked(!RotationPolicy.isRotationLocked(r0));
        }
    }

    private void updateDisableAnimationsToggle() {
        String[] strArr = TOGGLE_ANIMATION_TARGETS;
        int length = strArr.length;
        boolean z = false;
        int i = 0;
        while (true) {
            if (i < length) {
                if (!TextUtils.equals(Settings.Global.getString(getContentResolver(), strArr[i]), "0")) {
                    break;
                } else {
                    i++;
                }
            } else {
                z = true;
                break;
            }
        }
        this.mToggleDisableAnimationsPreference.setChecked(z);
    }

    private void updateMasterMono() {
        this.mToggleMasterMonoPreference.setChecked(Settings.System.getIntForUser(getContentResolver(), "master_mono", 0, -2) == 1);
    }

    private void updateAccessibilityShortcut(Preference preference) {
        CharSequence string;
        if (AccessibilityManager.getInstance(getActivity()).getInstalledAccessibilityServiceList().isEmpty()) {
            this.mAccessibilityShortcutPreferenceScreen.setSummary(getString(R.string.accessibility_no_services_installed));
            this.mAccessibilityShortcutPreferenceScreen.setEnabled(false);
            return;
        }
        this.mAccessibilityShortcutPreferenceScreen.setEnabled(true);
        if (AccessibilityUtils.isShortcutEnabled(getContext(), UserHandle.myUserId())) {
            string = AccessibilityShortcutPreferenceFragment.getServiceName(getContext());
        } else {
            string = getString(R.string.accessibility_feature_state_off);
        }
        this.mAccessibilityShortcutPreferenceScreen.setSummary(string);
    }

    private static void configureMagnificationPreferenceIfNeeded(Preference preference) {
        Context context = preference.getContext();
        if (!MagnificationPreferenceFragment.isApplicable(context.getResources())) {
            preference.setFragment(ToggleScreenMagnificationPreferenceFragment.class.getName());
            MagnificationGesturesPreferenceController.populateMagnificationGesturesPreferenceExtras(preference.getExtras(), context);
        }
    }
}
