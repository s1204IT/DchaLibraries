package com.android.settings.notification;

import android.app.AutomaticZenRule;
import android.app.Fragment;
import android.content.Context;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;
import android.widget.Switch;
import com.android.settings.R;
import com.android.settings.applications.LayoutPreference;
import com.android.settings.widget.SwitchBar;
import com.android.settingslib.core.lifecycle.Lifecycle;

/* loaded from: classes.dex */
public class ZenAutomaticRuleSwitchPreferenceController extends AbstractZenModeAutomaticRulePreferenceController implements SwitchBar.OnSwitchChangeListener {
    private String mId;
    private AutomaticZenRule mRule;
    private SwitchBar mSwitchBar;

    public ZenAutomaticRuleSwitchPreferenceController(Context context, Fragment fragment, Lifecycle lifecycle) {
        super(context, "zen_automatic_rule_switch", fragment, lifecycle);
    }

    @Override // com.android.settings.notification.AbstractZenModePreferenceController, com.android.settingslib.core.AbstractPreferenceController
    public String getPreferenceKey() {
        return "zen_automatic_rule_switch";
    }

    @Override // com.android.settingslib.core.AbstractPreferenceController
    public boolean isAvailable() {
        return (this.mRule == null || this.mId == null) ? false : true;
    }

    @Override // com.android.settings.notification.AbstractZenModePreferenceController, com.android.settingslib.core.AbstractPreferenceController
    public void displayPreference(PreferenceScreen preferenceScreen) {
        super.displayPreference(preferenceScreen);
        this.mSwitchBar = (SwitchBar) ((LayoutPreference) preferenceScreen.findPreference("zen_automatic_rule_switch")).findViewById(R.id.switch_bar);
        if (this.mSwitchBar != null) {
            this.mSwitchBar.setSwitchBarText(R.string.zen_mode_use_automatic_rule, R.string.zen_mode_use_automatic_rule);
            try {
                this.mSwitchBar.addOnSwitchChangeListener(this);
            } catch (IllegalStateException e) {
            }
            this.mSwitchBar.show();
        }
    }

    public void onResume(AutomaticZenRule automaticZenRule, String str) {
        this.mRule = automaticZenRule;
        this.mId = str;
    }

    @Override // com.android.settings.notification.AbstractZenModeAutomaticRulePreferenceController, com.android.settingslib.core.AbstractPreferenceController
    public void updateState(Preference preference) {
        if (this.mRule != null) {
            this.mSwitchBar.setChecked(this.mRule.isEnabled());
        }
    }

    @Override // com.android.settings.widget.SwitchBar.OnSwitchChangeListener
    public void onSwitchChanged(Switch r2, boolean z) {
        if (z == this.mRule.isEnabled()) {
            return;
        }
        this.mRule.setEnabled(z);
        this.mBackend.setZenRule(this.mId, this.mRule);
    }
}
