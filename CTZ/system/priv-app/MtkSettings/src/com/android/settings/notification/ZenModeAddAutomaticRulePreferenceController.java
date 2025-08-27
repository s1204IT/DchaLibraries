package com.android.settings.notification;

import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;
import com.android.settings.notification.ZenRuleSelectionDialog;
import com.android.settings.utils.ZenServiceListing;
import com.android.settingslib.core.lifecycle.Lifecycle;

/* loaded from: classes.dex */
public class ZenModeAddAutomaticRulePreferenceController extends AbstractZenModeAutomaticRulePreferenceController implements Preference.OnPreferenceClickListener {
    private final ZenServiceListing mZenServiceListing;

    public ZenModeAddAutomaticRulePreferenceController(Context context, Fragment fragment, ZenServiceListing zenServiceListing, Lifecycle lifecycle) {
        super(context, "zen_mode_add_automatic_rule", fragment, lifecycle);
        this.mZenServiceListing = zenServiceListing;
    }

    @Override // com.android.settings.notification.AbstractZenModePreferenceController, com.android.settingslib.core.AbstractPreferenceController
    public String getPreferenceKey() {
        return "zen_mode_add_automatic_rule";
    }

    @Override // com.android.settingslib.core.AbstractPreferenceController
    public boolean isAvailable() {
        return true;
    }

    @Override // com.android.settings.notification.AbstractZenModePreferenceController, com.android.settingslib.core.AbstractPreferenceController
    public void displayPreference(PreferenceScreen preferenceScreen) {
        super.displayPreference(preferenceScreen);
        Preference preferenceFindPreference = preferenceScreen.findPreference("zen_mode_add_automatic_rule");
        preferenceFindPreference.setPersistent(false);
        preferenceFindPreference.setOnPreferenceClickListener(this);
    }

    @Override // android.support.v7.preference.Preference.OnPreferenceClickListener
    public boolean onPreferenceClick(Preference preference) {
        ZenRuleSelectionDialog.show(this.mContext, this.mParent, new RuleSelectionListener(), this.mZenServiceListing);
        return true;
    }

    public class RuleSelectionListener implements ZenRuleSelectionDialog.PositiveClickListener {
        public RuleSelectionListener() {
        }

        @Override // com.android.settings.notification.ZenRuleSelectionDialog.PositiveClickListener
        public void onSystemRuleSelected(ZenRuleInfo zenRuleInfo, Fragment fragment) {
            ZenModeAddAutomaticRulePreferenceController.this.showNameRuleDialog(zenRuleInfo, fragment);
        }

        @Override // com.android.settings.notification.ZenRuleSelectionDialog.PositiveClickListener
        public void onExternalRuleSelected(ZenRuleInfo zenRuleInfo, Fragment fragment) {
            fragment.startActivity(new Intent().setComponent(zenRuleInfo.configurationActivity));
        }
    }
}
