package com.android.settings.security.screenlock;

import android.content.Context;
import android.support.v7.preference.Preference;
import android.support.v7.preference.TwoStatePreference;
import android.text.TextUtils;
import com.android.internal.widget.LockPatternUtils;
import com.android.settings.R;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settings.overlay.FeatureFactory;
import com.android.settings.security.trustagent.TrustAgentManager;
import com.android.settingslib.core.AbstractPreferenceController;

/* loaded from: classes.dex */
public class PowerButtonInstantLockPreferenceController extends AbstractPreferenceController implements Preference.OnPreferenceChangeListener, PreferenceControllerMixin {
    private final LockPatternUtils mLockPatternUtils;
    private final TrustAgentManager mTrustAgentManager;
    private final int mUserId;

    public PowerButtonInstantLockPreferenceController(Context context, int i, LockPatternUtils lockPatternUtils) {
        super(context);
        this.mUserId = i;
        this.mLockPatternUtils = lockPatternUtils;
        this.mTrustAgentManager = FeatureFactory.getFactory(context).getSecurityFeatureProvider().getTrustAgentManager();
    }

    @Override // com.android.settingslib.core.AbstractPreferenceController
    public boolean isAvailable() {
        if (!this.mLockPatternUtils.isSecure(this.mUserId)) {
            return false;
        }
        int keyguardStoredPasswordQuality = this.mLockPatternUtils.getKeyguardStoredPasswordQuality(this.mUserId);
        return keyguardStoredPasswordQuality == 65536 || keyguardStoredPasswordQuality == 131072 || keyguardStoredPasswordQuality == 196608 || keyguardStoredPasswordQuality == 262144 || keyguardStoredPasswordQuality == 327680 || keyguardStoredPasswordQuality == 393216 || keyguardStoredPasswordQuality == 524288;
    }

    @Override // com.android.settingslib.core.AbstractPreferenceController
    public void updateState(Preference preference) throws Throwable {
        ((TwoStatePreference) preference).setChecked(this.mLockPatternUtils.getPowerButtonInstantlyLocks(this.mUserId));
        CharSequence activeTrustAgentLabel = this.mTrustAgentManager.getActiveTrustAgentLabel(this.mContext, this.mLockPatternUtils);
        if (TextUtils.isEmpty(activeTrustAgentLabel)) {
            preference.setSummary(R.string.summary_placeholder);
        } else {
            preference.setSummary(this.mContext.getString(R.string.lockpattern_settings_power_button_instantly_locks_summary, activeTrustAgentLabel));
        }
    }

    @Override // com.android.settingslib.core.AbstractPreferenceController
    public String getPreferenceKey() {
        return "power_button_instantly_locks";
    }

    @Override // android.support.v7.preference.Preference.OnPreferenceChangeListener
    public boolean onPreferenceChange(Preference preference, Object obj) {
        this.mLockPatternUtils.setPowerButtonInstantlyLocks(((Boolean) obj).booleanValue(), this.mUserId);
        return true;
    }
}
