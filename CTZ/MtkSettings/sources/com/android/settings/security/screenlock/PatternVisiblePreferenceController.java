package com.android.settings.security.screenlock;

import android.content.Context;
import android.support.v7.preference.Preference;
import android.support.v7.preference.TwoStatePreference;
import com.android.internal.widget.LockPatternUtils;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settingslib.core.AbstractPreferenceController;

public class PatternVisiblePreferenceController extends AbstractPreferenceController implements Preference.OnPreferenceChangeListener, PreferenceControllerMixin {
    private final LockPatternUtils mLockPatternUtils;
    private final int mUserId;

    public PatternVisiblePreferenceController(Context context, int i, LockPatternUtils lockPatternUtils) {
        super(context);
        this.mUserId = i;
        this.mLockPatternUtils = lockPatternUtils;
    }

    @Override
    public boolean isAvailable() {
        return isPatternLock();
    }

    @Override
    public String getPreferenceKey() {
        return "visiblepattern";
    }

    @Override
    public void updateState(Preference preference) {
        ((TwoStatePreference) preference).setChecked(this.mLockPatternUtils.isVisiblePatternEnabled(this.mUserId));
    }

    private boolean isPatternLock() {
        return this.mLockPatternUtils.isSecure(this.mUserId) && this.mLockPatternUtils.getKeyguardStoredPasswordQuality(this.mUserId) == 65536;
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object obj) {
        this.mLockPatternUtils.setVisiblePatternEnabled(((Boolean) obj).booleanValue(), this.mUserId);
        return true;
    }
}
