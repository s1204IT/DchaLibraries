package com.android.settings.security.screenlock;

import android.app.admin.DevicePolicyManager;
import android.content.Context;
import android.os.UserHandle;
import android.provider.Settings;
import android.support.v7.preference.Preference;
import android.text.TextUtils;
import android.util.Log;
import com.android.internal.widget.LockPatternUtils;
import com.android.settings.R;
import com.android.settings.TimeoutListPreference;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settings.overlay.FeatureFactory;
import com.android.settings.security.trustagent.TrustAgentManager;
import com.android.settingslib.RestrictedLockUtils;
import com.android.settingslib.core.AbstractPreferenceController;

public class LockAfterTimeoutPreferenceController extends AbstractPreferenceController implements Preference.OnPreferenceChangeListener, PreferenceControllerMixin {
    private final DevicePolicyManager mDPM;
    private final LockPatternUtils mLockPatternUtils;
    private final TrustAgentManager mTrustAgentManager;
    private final int mUserId;

    public LockAfterTimeoutPreferenceController(Context context, int i, LockPatternUtils lockPatternUtils) {
        super(context);
        this.mUserId = i;
        this.mLockPatternUtils = lockPatternUtils;
        this.mDPM = (DevicePolicyManager) context.getSystemService("device_policy");
        this.mTrustAgentManager = FeatureFactory.getFactory(context).getSecurityFeatureProvider().getTrustAgentManager();
    }

    @Override
    public boolean isAvailable() {
        if (!this.mLockPatternUtils.isSecure(this.mUserId)) {
            return false;
        }
        int keyguardStoredPasswordQuality = this.mLockPatternUtils.getKeyguardStoredPasswordQuality(this.mUserId);
        return keyguardStoredPasswordQuality == 65536 || keyguardStoredPasswordQuality == 131072 || keyguardStoredPasswordQuality == 196608 || keyguardStoredPasswordQuality == 262144 || keyguardStoredPasswordQuality == 327680 || keyguardStoredPasswordQuality == 393216 || keyguardStoredPasswordQuality == 524288;
    }

    @Override
    public String getPreferenceKey() {
        return "lock_after_timeout";
    }

    @Override
    public void updateState(Preference preference) throws Throwable {
        TimeoutListPreference timeoutListPreference = (TimeoutListPreference) preference;
        setupLockAfterPreference(timeoutListPreference);
        updateLockAfterPreferenceSummary(timeoutListPreference);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object obj) throws Throwable {
        try {
            Settings.Secure.putInt(this.mContext.getContentResolver(), "lock_screen_lock_after_timeout", Integer.parseInt((String) obj));
            updateState(preference);
            return true;
        } catch (NumberFormatException e) {
            Log.e("PrefControllerMixin", "could not persist lockAfter timeout setting", e);
            return true;
        }
    }

    private void setupLockAfterPreference(TimeoutListPreference timeoutListPreference) {
        timeoutListPreference.setValue(String.valueOf(Settings.Secure.getLong(this.mContext.getContentResolver(), "lock_screen_lock_after_timeout", 5000L)));
        if (this.mDPM != null) {
            timeoutListPreference.removeUnusableTimeouts(Math.max(0L, this.mDPM.getMaximumTimeToLock(null, UserHandle.myUserId()) - ((long) Math.max(0, Settings.System.getInt(this.mContext.getContentResolver(), "screen_off_timeout", 0)))), RestrictedLockUtils.checkIfMaximumTimeToLockIsSet(this.mContext));
        }
    }

    private void updateLockAfterPreferenceSummary(TimeoutListPreference timeoutListPreference) throws Throwable {
        CharSequence string;
        if (timeoutListPreference.isDisabledByAdmin()) {
            string = this.mContext.getText(R.string.disabled_by_policy_title);
        } else {
            long j = Settings.Secure.getLong(this.mContext.getContentResolver(), "lock_screen_lock_after_timeout", 5000L);
            CharSequence[] entries = timeoutListPreference.getEntries();
            CharSequence[] entryValues = timeoutListPreference.getEntryValues();
            int i = 0;
            for (int i2 = 0; i2 < entryValues.length; i2++) {
                if (j >= Long.valueOf(entryValues[i2].toString()).longValue()) {
                    i = i2;
                }
            }
            CharSequence activeTrustAgentLabel = this.mTrustAgentManager.getActiveTrustAgentLabel(this.mContext, this.mLockPatternUtils);
            if (TextUtils.isEmpty(activeTrustAgentLabel)) {
                string = this.mContext.getString(R.string.lock_after_timeout_summary, entries[i]);
            } else if (Long.valueOf(entryValues[i].toString()).longValue() == 0) {
                string = this.mContext.getString(R.string.lock_immediately_summary_with_exception, activeTrustAgentLabel);
            } else {
                string = this.mContext.getString(R.string.lock_after_timeout_summary_with_exception, entries[i], activeTrustAgentLabel);
            }
        }
        timeoutListPreference.setSummary(string);
    }
}
