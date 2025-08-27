package com.android.settings.security.trustagent;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.UserHandle;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceCategory;
import android.support.v7.preference.PreferenceScreen;
import android.text.TextUtils;
import com.android.internal.widget.LockPatternUtils;
import com.android.settings.R;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settings.overlay.FeatureFactory;
import com.android.settings.password.ChooseLockSettingsHelper;
import com.android.settings.security.SecurityFeatureProvider;
import com.android.settings.security.SecuritySettings;
import com.android.settings.security.trustagent.TrustAgentManager;
import com.android.settingslib.RestrictedPreference;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.core.lifecycle.Lifecycle;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnCreate;
import com.android.settingslib.core.lifecycle.events.OnResume;
import com.android.settingslib.core.lifecycle.events.OnSaveInstanceState;
import java.util.List;

/* loaded from: classes.dex */
public class TrustAgentListPreferenceController extends AbstractPreferenceController implements PreferenceControllerMixin, LifecycleObserver, OnCreate, OnResume, OnSaveInstanceState {
    private static final int MY_USER_ID = UserHandle.myUserId();
    static final String PREF_KEY_SECURITY_CATEGORY = "security_category";
    static final String PREF_KEY_TRUST_AGENT = "trust_agent";
    private final SecuritySettings mHost;
    private final LockPatternUtils mLockPatternUtils;
    private PreferenceCategory mSecurityCategory;
    private Intent mTrustAgentClickIntent;
    private final TrustAgentManager mTrustAgentManager;

    public TrustAgentListPreferenceController(Context context, SecuritySettings securitySettings, Lifecycle lifecycle) {
        super(context);
        SecurityFeatureProvider securityFeatureProvider = FeatureFactory.getFactory(context).getSecurityFeatureProvider();
        this.mHost = securitySettings;
        this.mLockPatternUtils = securityFeatureProvider.getLockPatternUtils(context);
        this.mTrustAgentManager = securityFeatureProvider.getTrustAgentManager();
        if (lifecycle != null) {
            lifecycle.addObserver(this);
        }
    }

    @Override // com.android.settingslib.core.AbstractPreferenceController
    public boolean isAvailable() {
        return this.mContext.getResources().getBoolean(R.bool.config_show_trust_agent_click_intent);
    }

    @Override // com.android.settingslib.core.AbstractPreferenceController
    public String getPreferenceKey() {
        return PREF_KEY_TRUST_AGENT;
    }

    @Override // com.android.settingslib.core.AbstractPreferenceController
    public void displayPreference(PreferenceScreen preferenceScreen) throws Throwable {
        super.displayPreference(preferenceScreen);
        this.mSecurityCategory = (PreferenceCategory) preferenceScreen.findPreference(PREF_KEY_SECURITY_CATEGORY);
        updateTrustAgents();
    }

    @Override // com.android.settingslib.core.lifecycle.events.OnCreate
    public void onCreate(Bundle bundle) {
        if (bundle != null && bundle.containsKey("trust_agent_click_intent")) {
            this.mTrustAgentClickIntent = (Intent) bundle.getParcelable("trust_agent_click_intent");
        }
    }

    @Override // com.android.settingslib.core.lifecycle.events.OnSaveInstanceState
    public void onSaveInstanceState(Bundle bundle) {
        if (this.mTrustAgentClickIntent != null) {
            bundle.putParcelable("trust_agent_click_intent", this.mTrustAgentClickIntent);
        }
    }

    @Override // com.android.settingslib.core.AbstractPreferenceController
    public boolean handlePreferenceTreeClick(Preference preference) {
        if (!TextUtils.equals(preference.getKey(), getPreferenceKey())) {
            return super.handlePreferenceTreeClick(preference);
        }
        ChooseLockSettingsHelper chooseLockSettingsHelper = new ChooseLockSettingsHelper(this.mHost.getActivity(), this.mHost);
        this.mTrustAgentClickIntent = preference.getIntent();
        if (!chooseLockSettingsHelper.launchConfirmationActivity(126, preference.getTitle()) && this.mTrustAgentClickIntent != null) {
            this.mHost.startActivity(this.mTrustAgentClickIntent);
            this.mTrustAgentClickIntent = null;
            return true;
        }
        return true;
    }

    @Override // com.android.settingslib.core.lifecycle.events.OnResume
    public void onResume() throws Throwable {
        updateTrustAgents();
    }

    private void updateTrustAgents() throws Throwable {
        if (this.mSecurityCategory == null) {
            return;
        }
        while (true) {
            Preference preferenceFindPreference = this.mSecurityCategory.findPreference(PREF_KEY_TRUST_AGENT);
            if (preferenceFindPreference == null) {
                break;
            } else {
                this.mSecurityCategory.removePreference(preferenceFindPreference);
            }
        }
        if (!isAvailable()) {
            return;
        }
        boolean zIsSecure = this.mLockPatternUtils.isSecure(MY_USER_ID);
        List<TrustAgentManager.TrustAgentComponentInfo> activeTrustAgents = this.mTrustAgentManager.getActiveTrustAgents(this.mContext, this.mLockPatternUtils);
        if (activeTrustAgents == null) {
            return;
        }
        for (TrustAgentManager.TrustAgentComponentInfo trustAgentComponentInfo : activeTrustAgents) {
            RestrictedPreference restrictedPreference = new RestrictedPreference(this.mSecurityCategory.getContext());
            restrictedPreference.setKey(PREF_KEY_TRUST_AGENT);
            restrictedPreference.setTitle(trustAgentComponentInfo.title);
            restrictedPreference.setSummary(trustAgentComponentInfo.summary);
            restrictedPreference.setIntent(new Intent("android.intent.action.MAIN").setComponent(trustAgentComponentInfo.componentName));
            restrictedPreference.setDisabledByAdmin(trustAgentComponentInfo.admin);
            if (!restrictedPreference.isDisabledByAdmin() && !zIsSecure) {
                restrictedPreference.setEnabled(false);
                restrictedPreference.setSummary(R.string.disabled_because_no_backup_security);
            }
            this.mSecurityCategory.addPreference(restrictedPreference);
        }
    }

    public boolean handleActivityResult(int i, int i2) {
        if (i == 126 && i2 == -1) {
            if (this.mTrustAgentClickIntent != null) {
                this.mHost.startActivity(this.mTrustAgentClickIntent);
                this.mTrustAgentClickIntent = null;
                return true;
            }
            return true;
        }
        return false;
    }
}
