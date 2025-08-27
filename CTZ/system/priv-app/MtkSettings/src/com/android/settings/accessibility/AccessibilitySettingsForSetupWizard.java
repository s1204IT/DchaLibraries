package com.android.settings.accessibility;

import android.accessibilityservice.AccessibilityServiceInfo;
import android.content.ComponentName;
import android.content.Context;
import android.content.pm.ServiceInfo;
import android.os.Bundle;
import android.support.v7.preference.Preference;
import android.text.TextUtils;
import android.view.accessibility.AccessibilityManager;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;

/* loaded from: classes.dex */
public class AccessibilitySettingsForSetupWizard extends SettingsPreferenceFragment implements Preference.OnPreferenceChangeListener {
    private Preference mDisplayMagnificationPreference;
    private Preference mScreenReaderPreference;
    private Preference mSelectToSpeakPreference;

    @Override // com.android.settingslib.core.instrumentation.Instrumentable
    public int getMetricsCategory() {
        return 367;
    }

    @Override // com.android.settings.SettingsPreferenceFragment, com.android.settingslib.core.lifecycle.ObservablePreferenceFragment, android.support.v14.preference.PreferenceFragment, android.app.Fragment
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        addPreferencesFromResource(R.xml.accessibility_settings_for_setup_wizard);
        this.mDisplayMagnificationPreference = findPreference("screen_magnification_preference");
        this.mScreenReaderPreference = findPreference("screen_reader_preference");
        this.mSelectToSpeakPreference = findPreference("select_to_speak_preference");
    }

    @Override // com.android.settings.SettingsPreferenceFragment, com.android.settings.core.InstrumentedPreferenceFragment, com.android.settingslib.core.lifecycle.ObservablePreferenceFragment, android.app.Fragment
    public void onResume() {
        super.onResume();
        updateAccessibilityServicePreference(this.mScreenReaderPreference, findService("com.google.android.marvin.talkback", "com.google.android.marvin.talkback.TalkBackService"));
        updateAccessibilityServicePreference(this.mSelectToSpeakPreference, findService("com.google.android.marvin.talkback", "com.google.android.accessibility.selecttospeak.SelectToSpeakService"));
        configureMagnificationPreferenceIfNeeded(this.mDisplayMagnificationPreference);
    }

    @Override // com.android.settings.SettingsPreferenceFragment, android.support.v14.preference.PreferenceFragment, android.app.Fragment
    public void onActivityCreated(Bundle bundle) {
        super.onActivityCreated(bundle);
        setHasOptionsMenu(false);
    }

    @Override // android.support.v7.preference.Preference.OnPreferenceChangeListener
    public boolean onPreferenceChange(Preference preference, Object obj) {
        return false;
    }

    @Override // android.support.v14.preference.PreferenceFragment, android.support.v7.preference.PreferenceManager.OnPreferenceTreeClickListener
    public boolean onPreferenceTreeClick(Preference preference) {
        if (this.mDisplayMagnificationPreference == preference) {
            this.mDisplayMagnificationPreference.getExtras().putBoolean("from_suw", true);
        }
        return super.onPreferenceTreeClick(preference);
    }

    private AccessibilityServiceInfo findService(String str, String str2) {
        for (AccessibilityServiceInfo accessibilityServiceInfo : ((AccessibilityManager) getActivity().getSystemService(AccessibilityManager.class)).getInstalledAccessibilityServiceList()) {
            ServiceInfo serviceInfo = accessibilityServiceInfo.getResolveInfo().serviceInfo;
            if (str.equals(serviceInfo.packageName) && str2.equals(serviceInfo.name)) {
                return accessibilityServiceInfo;
            }
        }
        return null;
    }

    private void updateAccessibilityServicePreference(Preference preference, AccessibilityServiceInfo accessibilityServiceInfo) {
        if (accessibilityServiceInfo == null) {
            getPreferenceScreen().removePreference(preference);
            return;
        }
        ServiceInfo serviceInfo = accessibilityServiceInfo.getResolveInfo().serviceInfo;
        String string = accessibilityServiceInfo.getResolveInfo().loadLabel(getPackageManager()).toString();
        preference.setTitle(string);
        ComponentName componentName = new ComponentName(serviceInfo.packageName, serviceInfo.name);
        preference.setKey(componentName.flattenToString());
        Bundle extras = preference.getExtras();
        extras.putParcelable("component_name", componentName);
        extras.putString("preference_key", preference.getKey());
        extras.putString("title", string);
        String strLoadDescription = accessibilityServiceInfo.loadDescription(getPackageManager());
        if (TextUtils.isEmpty(strLoadDescription)) {
            strLoadDescription = getString(R.string.accessibility_service_default_description);
        }
        extras.putString("summary", strLoadDescription);
    }

    private static void configureMagnificationPreferenceIfNeeded(Preference preference) {
        Context context = preference.getContext();
        if (!MagnificationPreferenceFragment.isApplicable(context.getResources())) {
            preference.setFragment(ToggleScreenMagnificationPreferenceFragmentForSetupWizard.class.getName());
            MagnificationGesturesPreferenceController.populateMagnificationGesturesPreferenceExtras(preference.getExtras(), context);
        }
    }
}
