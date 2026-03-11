package com.android.settings.notification;

import android.app.AutomaticZenRule;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;
import android.util.Log;
import android.widget.Toast;
import com.android.settings.R;
import com.android.settingslib.core.AbstractPreferenceController;

public abstract class ZenModeRuleSettingsBase extends ZenModeSettingsBase {
    protected static final boolean DEBUG = ZenModeSettingsBase.DEBUG;
    protected Context mContext;
    protected boolean mDisableListeners;
    protected ZenAutomaticRuleHeaderPreferenceController mHeader;
    protected String mId;
    protected AutomaticZenRule mRule;
    protected ZenAutomaticRuleSwitchPreferenceController mSwitch;

    protected abstract void onCreateInternal();

    protected abstract boolean setRule(AutomaticZenRule automaticZenRule);

    protected abstract void updateControlsInternal();

    @Override
    public void onCreate(Bundle bundle) {
        this.mContext = getActivity();
        Intent intent = getActivity().getIntent();
        if (DEBUG) {
            Log.d("ZenModeSettings", "onCreate getIntent()=" + intent);
        }
        if (intent == null) {
            Log.w("ZenModeSettings", "No intent");
            toastAndFinish();
            return;
        }
        this.mId = intent.getStringExtra("android.service.notification.extra.RULE_ID");
        if (this.mId == null) {
            Log.w("ZenModeSettings", "rule id is null");
            toastAndFinish();
            return;
        }
        if (DEBUG) {
            Log.d("ZenModeSettings", "mId=" + this.mId);
        }
        if (refreshRuleOrFinish()) {
            return;
        }
        super.onCreate(bundle);
        onCreateInternal();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (isUiRestricted()) {
            return;
        }
        updateControls();
    }

    @Override
    public int getHelpResource() {
        return R.string.help_uri_interruptions;
    }

    protected void updateHeader() {
        PreferenceScreen preferenceScreen = getPreferenceScreen();
        this.mSwitch.onResume(this.mRule, this.mId);
        this.mSwitch.displayPreference(preferenceScreen);
        updatePreference(this.mSwitch);
        this.mHeader.onResume(this.mRule, this.mId);
        this.mHeader.displayPreference(preferenceScreen);
        updatePreference(this.mHeader);
    }

    private void updatePreference(AbstractPreferenceController abstractPreferenceController) {
        PreferenceScreen preferenceScreen = getPreferenceScreen();
        if (!abstractPreferenceController.isAvailable()) {
            return;
        }
        String preferenceKey = abstractPreferenceController.getPreferenceKey();
        Preference preferenceFindPreference = preferenceScreen.findPreference(preferenceKey);
        if (preferenceFindPreference == null) {
            Log.d("ZenModeSettings", String.format("Cannot find preference with key %s in Controller %s", preferenceKey, abstractPreferenceController.getClass().getSimpleName()));
        } else {
            abstractPreferenceController.updateState(preferenceFindPreference);
        }
    }

    protected void updateRule(Uri uri) {
        this.mRule.setConditionId(uri);
        this.mBackend.setZenRule(this.mId, this.mRule);
    }

    @Override
    protected void onZenModeConfigChanged() {
        super.onZenModeConfigChanged();
        if (!refreshRuleOrFinish()) {
            updateControls();
        }
    }

    private boolean refreshRuleOrFinish() {
        this.mRule = getZenRule();
        if (DEBUG) {
            Log.d("ZenModeSettings", "mRule=" + this.mRule);
        }
        if (!setRule(this.mRule)) {
            toastAndFinish();
            return true;
        }
        return false;
    }

    private void toastAndFinish() {
        Toast.makeText(this.mContext, R.string.zen_mode_rule_not_found_text, 0).show();
        getActivity().finish();
    }

    private AutomaticZenRule getZenRule() {
        return NotificationManager.from(this.mContext).getAutomaticZenRule(this.mId);
    }

    private void updateControls() {
        this.mDisableListeners = true;
        updateControlsInternal();
        updateHeader();
        this.mDisableListeners = false;
    }
}
