package com.android.settings.display;

import android.app.admin.DevicePolicyManager;
import android.content.Context;
import android.os.UserHandle;
import android.provider.Settings;
import android.support.v7.preference.Preference;
import android.util.Log;
import com.android.settings.R;
import com.android.settings.TimeoutListPreference;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settingslib.RestrictedLockUtils;
import com.android.settingslib.core.AbstractPreferenceController;
import com.mediatek.settings.UtilsExt;
import com.mediatek.settings.ext.ISettingsMiscExt;

/* loaded from: classes.dex */
public class TimeoutPreferenceController extends AbstractPreferenceController implements Preference.OnPreferenceChangeListener, PreferenceControllerMixin {
    private ISettingsMiscExt mExt;
    private final String mScreenTimeoutKey;

    public TimeoutPreferenceController(Context context, String str) {
        super(context);
        this.mExt = null;
        this.mScreenTimeoutKey = str;
    }

    @Override // com.android.settingslib.core.AbstractPreferenceController
    public boolean isAvailable() {
        return true;
    }

    @Override // com.android.settingslib.core.AbstractPreferenceController
    public String getPreferenceKey() {
        return this.mScreenTimeoutKey;
    }

    @Override // com.android.settingslib.core.AbstractPreferenceController
    public void updateState(Preference preference) throws NumberFormatException {
        TimeoutListPreference timeoutListPreference = (TimeoutListPreference) preference;
        long j = Settings.System.getLong(this.mContext.getContentResolver(), "screen_off_timeout", 30000L);
        timeoutListPreference.setValue(String.valueOf(j));
        DevicePolicyManager devicePolicyManager = (DevicePolicyManager) this.mContext.getSystemService("device_policy");
        if (devicePolicyManager != null) {
            timeoutListPreference.removeUnusableTimeouts(devicePolicyManager.getMaximumTimeToLock(null, UserHandle.myUserId()), RestrictedLockUtils.checkIfMaximumTimeToLockIsSet(this.mContext));
        }
        updateTimeoutPreferenceDescription(timeoutListPreference, j);
        RestrictedLockUtils.EnforcedAdmin enforcedAdminCheckIfRestrictionEnforced = RestrictedLockUtils.checkIfRestrictionEnforced(this.mContext, "no_config_screen_timeout", UserHandle.myUserId());
        if (enforcedAdminCheckIfRestrictionEnforced != null) {
            timeoutListPreference.removeUnusableTimeouts(0L, enforcedAdminCheckIfRestrictionEnforced);
        }
    }

    @Override // android.support.v7.preference.Preference.OnPreferenceChangeListener
    public boolean onPreferenceChange(Preference preference, Object obj) throws NumberFormatException {
        try {
            int i = Integer.parseInt((String) obj);
            Settings.System.putInt(this.mContext.getContentResolver(), "screen_off_timeout", i);
            updateTimeoutPreferenceDescription((TimeoutListPreference) preference, i);
            return true;
        } catch (NumberFormatException e) {
            Log.e("TimeoutPrefContr", "could not persist screen timeout setting", e);
            return true;
        }
    }

    public static CharSequence getTimeoutDescription(long j, CharSequence[] charSequenceArr, CharSequence[] charSequenceArr2) {
        if (j < 0 || charSequenceArr == null || charSequenceArr2 == null || charSequenceArr2.length != charSequenceArr.length) {
            return null;
        }
        for (int i = 0; i < charSequenceArr2.length; i++) {
            if (j == Long.parseLong(charSequenceArr2[i].toString())) {
                return charSequenceArr[i];
            }
        }
        return null;
    }

    private void updateTimeoutPreferenceDescription(TimeoutListPreference timeoutListPreference, long j) {
        String string;
        CharSequence[] entries = timeoutListPreference.getEntries();
        CharSequence[] entryValues = timeoutListPreference.getEntryValues();
        if (timeoutListPreference.isDisabledByAdmin()) {
            string = this.mContext.getString(R.string.disabled_by_policy_title);
        } else {
            CharSequence timeoutDescription = getTimeoutDescription(j, entries, entryValues);
            if (timeoutDescription == null) {
                string = "";
            } else {
                string = this.mContext.getString(R.string.screen_timeout_summary, timeoutDescription);
            }
        }
        timeoutListPreference.setSummary(string);
        if (this.mExt == null) {
            this.mExt = UtilsExt.getMiscPlugin(this.mContext);
        }
        if (this.mExt != null) {
            this.mExt.setTimeoutPrefTitle(timeoutListPreference);
        }
    }
}
