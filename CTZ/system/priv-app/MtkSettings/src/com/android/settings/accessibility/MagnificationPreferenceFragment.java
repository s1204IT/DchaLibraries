package com.android.settings.accessibility;

import android.accessibilityservice.AccessibilityServiceInfo;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.res.Resources;
import android.os.Bundle;
import android.provider.SearchIndexableResource;
import android.provider.Settings;
import android.support.v7.preference.Preference;
import android.text.TextUtils;
import android.view.accessibility.AccessibilityManager;
import com.android.settings.R;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.search.Indexable;
import java.util.Arrays;
import java.util.List;

/* loaded from: classes.dex */
public final class MagnificationPreferenceFragment extends DashboardFragment {
    static final int OFF = 0;
    static final int ON = 1;
    public static final Indexable.SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER = new BaseSearchIndexProvider() { // from class: com.android.settings.accessibility.MagnificationPreferenceFragment.1
        @Override // com.android.settings.search.BaseSearchIndexProvider, com.android.settings.search.Indexable.SearchIndexProvider
        public List<SearchIndexableResource> getXmlResourcesToIndex(Context context, boolean z) {
            SearchIndexableResource searchIndexableResource = new SearchIndexableResource(context);
            searchIndexableResource.xmlResId = R.xml.accessibility_magnification_settings;
            return Arrays.asList(searchIndexableResource);
        }

        @Override // com.android.settings.search.BaseSearchIndexProvider
        protected boolean isPageSearchEnabled(Context context) {
            return MagnificationPreferenceFragment.isApplicable(context.getResources());
        }

        @Override // com.android.settings.search.BaseSearchIndexProvider, com.android.settings.search.Indexable.SearchIndexProvider
        public List<String> getNonIndexableKeys(Context context) {
            List<String> nonIndexableKeys = super.getNonIndexableKeys(context);
            nonIndexableKeys.add("magnification_preference_screen_title");
            return nonIndexableKeys;
        }
    };
    private boolean mLaunchedFromSuw = false;

    @Override // com.android.settingslib.core.instrumentation.Instrumentable
    public int getMetricsCategory() {
        return 922;
    }

    @Override // com.android.settings.dashboard.DashboardFragment
    protected String getLogTag() {
        return "MagnificationPreferenceFragment";
    }

    @Override // com.android.settings.support.actionbar.HelpResourceProvider
    public int getHelpResource() {
        return R.string.help_url_magnification;
    }

    @Override // com.android.settings.dashboard.DashboardFragment, com.android.settings.core.InstrumentedPreferenceFragment
    protected int getPreferenceScreenResId() {
        return R.xml.accessibility_magnification_settings;
    }

    @Override // com.android.settings.dashboard.DashboardFragment, com.android.settings.core.InstrumentedPreferenceFragment, com.android.settingslib.core.lifecycle.ObservablePreferenceFragment, android.app.Fragment
    public void onAttach(Context context) {
        super.onAttach(context);
        Bundle arguments = getArguments();
        if (arguments != null && arguments.containsKey("from_suw")) {
            this.mLaunchedFromSuw = arguments.getBoolean("from_suw");
        }
        ((MagnificationGesturesPreferenceController) use(MagnificationGesturesPreferenceController.class)).setIsFromSUW(this.mLaunchedFromSuw);
        ((MagnificationNavbarPreferenceController) use(MagnificationNavbarPreferenceController.class)).setIsFromSUW(this.mLaunchedFromSuw);
    }

    @Override // com.android.settings.dashboard.DashboardFragment, android.support.v14.preference.PreferenceFragment, android.support.v7.preference.PreferenceManager.OnPreferenceTreeClickListener
    public boolean onPreferenceTreeClick(Preference preference) {
        if (this.mLaunchedFromSuw) {
            preference.setFragment(ToggleScreenMagnificationPreferenceFragmentForSetupWizard.class.getName());
            Bundle extras = preference.getExtras();
            extras.putInt("help_uri_resource", 0);
            extras.putBoolean("need_search_icon_in_action_bar", false);
        }
        return super.onPreferenceTreeClick(preference);
    }

    static CharSequence getConfigurationWarningStringForSecureSettingsKey(String str, Context context) {
        if (!"accessibility_display_magnification_navbar_enabled".equals(str) || Settings.Secure.getInt(context.getContentResolver(), "accessibility_display_magnification_navbar_enabled", 0) == 0) {
            return null;
        }
        AccessibilityManager accessibilityManager = (AccessibilityManager) context.getSystemService("accessibility");
        String string = Settings.Secure.getString(context.getContentResolver(), "accessibility_button_target_component");
        if (!TextUtils.isEmpty(string) && !"com.android.server.accessibility.MagnificationController".equals(string)) {
            ComponentName componentNameUnflattenFromString = ComponentName.unflattenFromString(string);
            List<AccessibilityServiceInfo> enabledAccessibilityServiceList = accessibilityManager.getEnabledAccessibilityServiceList(-1);
            int size = enabledAccessibilityServiceList.size();
            for (int i = 0; i < size; i++) {
                AccessibilityServiceInfo accessibilityServiceInfo = enabledAccessibilityServiceList.get(i);
                if (accessibilityServiceInfo.getComponentName().equals(componentNameUnflattenFromString)) {
                    return context.getString(R.string.accessibility_screen_magnification_navbar_configuration_warning, accessibilityServiceInfo.getResolveInfo().loadLabel(context.getPackageManager()));
                }
            }
        }
        return null;
    }

    static boolean isChecked(ContentResolver contentResolver, String str) {
        return Settings.Secure.getInt(contentResolver, str, 0) == 1;
    }

    static boolean setChecked(ContentResolver contentResolver, String str, boolean z) {
        return Settings.Secure.putInt(contentResolver, str, z ? 1 : 0);
    }

    static boolean isApplicable(Resources resources) {
        return resources.getBoolean(android.R.^attr-private.lockPatternStyle);
    }
}
