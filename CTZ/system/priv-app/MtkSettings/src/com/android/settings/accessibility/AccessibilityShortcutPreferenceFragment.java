package com.android.settings.accessibility;

import android.accessibilityservice.AccessibilityServiceInfo;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.database.ContentObserver;
import android.os.Bundle;
import android.os.Handler;
import android.os.UserHandle;
import android.provider.Settings;
import android.support.v14.preference.SwitchPreference;
import android.support.v7.preference.Preference;
import android.view.accessibility.AccessibilityManager;
import android.widget.Switch;
import com.android.internal.accessibility.AccessibilityShortcutController;
import com.android.settings.R;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.search.Indexable;
import com.android.settings.widget.SwitchBar;
import com.android.settingslib.accessibility.AccessibilityUtils;

/* loaded from: classes.dex */
public class AccessibilityShortcutPreferenceFragment extends ToggleFeaturePreferenceFragment implements Indexable {
    public static final Indexable.SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER = new BaseSearchIndexProvider() { // from class: com.android.settings.accessibility.AccessibilityShortcutPreferenceFragment.2
        @Override // com.android.settings.search.BaseSearchIndexProvider
        protected boolean isPageSearchEnabled(Context context) {
            return false;
        }
    };
    private final ContentObserver mContentObserver = new ContentObserver(new Handler()) { // from class: com.android.settings.accessibility.AccessibilityShortcutPreferenceFragment.1
        @Override // android.database.ContentObserver
        public void onChange(boolean z) {
            AccessibilityShortcutPreferenceFragment.this.updatePreferences();
        }
    };
    private SwitchPreference mOnLockScreenSwitchPreference;
    private Preference mServicePreference;

    @Override // com.android.settingslib.core.instrumentation.Instrumentable
    public int getMetricsCategory() {
        return 6;
    }

    @Override // com.android.settings.support.actionbar.HelpResourceProvider
    public int getHelpResource() {
        return R.string.help_url_accessibility_shortcut;
    }

    @Override // com.android.settings.accessibility.ToggleFeaturePreferenceFragment, com.android.settings.SettingsPreferenceFragment, com.android.settingslib.core.lifecycle.ObservablePreferenceFragment, android.support.v14.preference.PreferenceFragment, android.app.Fragment
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        this.mServicePreference = findPreference("accessibility_shortcut_service");
        this.mOnLockScreenSwitchPreference = (SwitchPreference) findPreference("accessibility_shortcut_on_lock_screen");
        this.mOnLockScreenSwitchPreference.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() { // from class: com.android.settings.accessibility.-$$Lambda$AccessibilityShortcutPreferenceFragment$v5UnURHl-V2dl7gTZw_kdUDDZ6E
            @Override // android.support.v7.preference.Preference.OnPreferenceChangeListener
            public final boolean onPreferenceChange(Preference preference, Object obj) {
                return AccessibilityShortcutPreferenceFragment.lambda$onCreate$0(this.f$0, preference, obj);
            }
        });
        this.mFooterPreferenceMixin.createFooterPreference().setTitle(R.string.accessibility_shortcut_description);
    }

    public static /* synthetic */ boolean lambda$onCreate$0(AccessibilityShortcutPreferenceFragment accessibilityShortcutPreferenceFragment, Preference preference, Object obj) {
        Settings.Secure.putInt(accessibilityShortcutPreferenceFragment.getContentResolver(), "accessibility_shortcut_on_lock_screen", ((Boolean) obj).booleanValue() ? 1 : 0);
        return true;
    }

    @Override // com.android.settings.SettingsPreferenceFragment, com.android.settings.core.InstrumentedPreferenceFragment, com.android.settingslib.core.lifecycle.ObservablePreferenceFragment, android.app.Fragment
    public void onResume() {
        super.onResume();
        updatePreferences();
        getContentResolver().registerContentObserver(Settings.Secure.getUriFor("accessibility_shortcut_dialog_shown"), false, this.mContentObserver);
    }

    @Override // com.android.settingslib.core.lifecycle.ObservablePreferenceFragment, android.app.Fragment
    public void onPause() {
        getContentResolver().unregisterContentObserver(this.mContentObserver);
        super.onPause();
    }

    @Override // com.android.settings.core.InstrumentedPreferenceFragment
    protected int getPreferenceScreenResId() {
        return R.xml.accessibility_shortcut_settings;
    }

    @Override // com.android.settings.accessibility.ToggleFeaturePreferenceFragment
    protected void onInstallSwitchBarToggleSwitch() {
        super.onInstallSwitchBarToggleSwitch();
        this.mSwitchBar.addOnSwitchChangeListener(new SwitchBar.OnSwitchChangeListener() { // from class: com.android.settings.accessibility.-$$Lambda$AccessibilityShortcutPreferenceFragment$B1JGpZUcoOdF9ofKXLGiPDgZ6Bo
            @Override // com.android.settings.widget.SwitchBar.OnSwitchChangeListener
            public final void onSwitchChanged(Switch r2, boolean z) {
                AccessibilityShortcutPreferenceFragment.lambda$onInstallSwitchBarToggleSwitch$1(this.f$0, r2, z);
            }
        });
    }

    public static /* synthetic */ void lambda$onInstallSwitchBarToggleSwitch$1(AccessibilityShortcutPreferenceFragment accessibilityShortcutPreferenceFragment, Switch r2, boolean z) {
        Context context = accessibilityShortcutPreferenceFragment.getContext();
        if (z && !shortcutFeatureAvailable(context)) {
            Settings.Secure.putInt(accessibilityShortcutPreferenceFragment.getContentResolver(), "accessibility_shortcut_enabled", 1);
            accessibilityShortcutPreferenceFragment.mServicePreference.setEnabled(true);
            accessibilityShortcutPreferenceFragment.mServicePreference.performClick();
            return;
        }
        accessibilityShortcutPreferenceFragment.onPreferenceToggled("accessibility_shortcut_enabled", z);
    }

    @Override // com.android.settings.accessibility.ToggleFeaturePreferenceFragment
    protected void onPreferenceToggled(String str, boolean z) {
        Settings.Secure.putInt(getContentResolver(), str, z ? 1 : 0);
        updatePreferences();
    }

    private void updatePreferences() {
        ContentResolver contentResolver = getContentResolver();
        Context context = getContext();
        this.mServicePreference.setSummary(getServiceName(context));
        if (!shortcutFeatureAvailable(context)) {
            Settings.Secure.putInt(getContentResolver(), "accessibility_shortcut_enabled", 0);
        }
        this.mSwitchBar.setChecked(Settings.Secure.getInt(contentResolver, "accessibility_shortcut_enabled", 1) == 1);
        this.mOnLockScreenSwitchPreference.setChecked(Settings.Secure.getInt(contentResolver, "accessibility_shortcut_on_lock_screen", Settings.Secure.getInt(contentResolver, "accessibility_shortcut_dialog_shown", 0)) == 1);
        this.mServicePreference.setEnabled(this.mToggleSwitch.isChecked());
        this.mOnLockScreenSwitchPreference.setEnabled(this.mToggleSwitch.isChecked());
    }

    public static CharSequence getServiceName(Context context) {
        if (!shortcutFeatureAvailable(context)) {
            return context.getString(R.string.accessibility_no_service_selected);
        }
        AccessibilityServiceInfo serviceInfo = getServiceInfo(context);
        if (serviceInfo != null) {
            return serviceInfo.getResolveInfo().loadLabel(context.getPackageManager());
        }
        return ((AccessibilityShortcutController.ToggleableFrameworkFeatureInfo) AccessibilityShortcutController.getFrameworkShortcutFeaturesMap().get(getShortcutComponent(context))).getLabel(context);
    }

    private static AccessibilityServiceInfo getServiceInfo(Context context) {
        return AccessibilityManager.getInstance(context).getInstalledServiceInfoWithComponentName(getShortcutComponent(context));
    }

    private static boolean shortcutFeatureAvailable(Context context) {
        ComponentName shortcutComponent = getShortcutComponent(context);
        if (shortcutComponent == null) {
            return false;
        }
        return AccessibilityShortcutController.getFrameworkShortcutFeaturesMap().containsKey(shortcutComponent) || getServiceInfo(context) != null;
    }

    private static ComponentName getShortcutComponent(Context context) {
        String shortcutTargetServiceComponentNameString = AccessibilityUtils.getShortcutTargetServiceComponentNameString(context, UserHandle.myUserId());
        if (shortcutTargetServiceComponentNameString == null) {
            return null;
        }
        return ComponentName.unflattenFromString(shortcutTargetServiceComponentNameString);
    }
}
