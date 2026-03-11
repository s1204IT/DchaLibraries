package com.android.settings.accessibility;

import android.accessibilityservice.AccessibilityServiceInfo;
import android.content.ComponentName;
import android.content.pm.ServiceInfo;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v7.preference.Preference;
import android.text.TextUtils;
import android.view.accessibility.AccessibilityManager;
import com.android.settings.DialogCreatable;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import java.util.List;

public class AccessibilitySettingsForSetupWizard extends SettingsPreferenceFragment implements DialogCreatable, Preference.OnPreferenceChangeListener {
    private static final String TAG = AccessibilitySettingsForSetupWizard.class.getSimpleName();
    private Preference mDisplayMagnificationPreference;
    private Preference mScreenReaderPreference;

    @Override
    protected int getMetricsCategory() {
        return 367;
    }

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        addPreferencesFromResource(R.xml.accessibility_settings_for_setup_wizard);
        this.mDisplayMagnificationPreference = findPreference("screen_magnification_preference");
        this.mScreenReaderPreference = findPreference("screen_reader_preference");
    }

    @Override
    public void onResume() {
        super.onResume();
        updateScreenReaderPreference();
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        setHasOptionsMenu(false);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        return false;
    }

    @Override
    public boolean onPreferenceTreeClick(Preference preference) {
        if (this.mDisplayMagnificationPreference == preference) {
            Bundle extras = this.mDisplayMagnificationPreference.getExtras();
            extras.putString("title", getString(R.string.accessibility_screen_magnification_title));
            extras.putCharSequence("summary", getText(R.string.accessibility_screen_magnification_summary));
            extras.putBoolean("checked", Settings.Secure.getInt(getContentResolver(), "accessibility_display_magnification_enabled", 0) == 1);
        }
        return super.onPreferenceTreeClick(preference);
    }

    private AccessibilityServiceInfo findFirstServiceWithSpokenFeedback() {
        AccessibilityManager manager = (AccessibilityManager) getActivity().getSystemService(AccessibilityManager.class);
        List<AccessibilityServiceInfo> accessibilityServices = manager.getInstalledAccessibilityServiceList();
        for (AccessibilityServiceInfo info : accessibilityServices) {
            if ((info.feedbackType & 1) != 0) {
                return info;
            }
        }
        return null;
    }

    private void updateScreenReaderPreference() {
        AccessibilityServiceInfo info = findFirstServiceWithSpokenFeedback();
        if (info == null) {
            this.mScreenReaderPreference.setEnabled(false);
            return;
        }
        this.mScreenReaderPreference.setEnabled(true);
        ServiceInfo serviceInfo = info.getResolveInfo().serviceInfo;
        String title = info.getResolveInfo().loadLabel(getPackageManager()).toString();
        this.mScreenReaderPreference.setTitle(title);
        ComponentName componentName = new ComponentName(serviceInfo.packageName, serviceInfo.name);
        this.mScreenReaderPreference.setKey(componentName.flattenToString());
        Bundle extras = this.mScreenReaderPreference.getExtras();
        extras.putParcelable("component_name", componentName);
        extras.putString("preference_key", this.mScreenReaderPreference.getKey());
        extras.putString("title", title);
        String description = info.loadDescription(getPackageManager());
        if (TextUtils.isEmpty(description)) {
            description = getString(R.string.accessibility_service_default_description);
        }
        extras.putString("summary", description);
    }
}
