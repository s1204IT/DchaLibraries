package com.android.settings.nfc;

import android.content.Context;
import android.os.UserHandle;
import com.android.settings.R;
import com.android.settingslib.RestrictedLockUtils;
import com.android.settingslib.RestrictedPreference;

/* loaded from: classes.dex */
public class AndroidBeamEnabler extends BaseNfcEnabler {
    private final boolean mBeamDisallowedBySystem;
    private final RestrictedPreference mPreference;

    public AndroidBeamEnabler(Context context, RestrictedPreference restrictedPreference) {
        super(context);
        this.mPreference = restrictedPreference;
        this.mBeamDisallowedBySystem = RestrictedLockUtils.hasBaseUserRestriction(context, "no_outgoing_beam", UserHandle.myUserId());
        if (!isNfcAvailable()) {
            this.mPreference.setEnabled(false);
        } else if (this.mBeamDisallowedBySystem) {
            this.mPreference.setEnabled(false);
        }
    }

    @Override // com.android.settings.nfc.BaseNfcEnabler
    protected void handleNfcStateChanged(int i) {
        switch (i) {
            case 1:
                this.mPreference.setEnabled(false);
                this.mPreference.setSummary(R.string.android_beam_disabled_summary);
                break;
            case 2:
                this.mPreference.setEnabled(false);
                break;
            case 3:
                if (this.mBeamDisallowedBySystem) {
                    this.mPreference.setDisabledByAdmin(null);
                    this.mPreference.setEnabled(false);
                } else {
                    this.mPreference.checkRestrictionAndSetDisabled("no_outgoing_beam");
                }
                if (this.mNfcAdapter.isNdefPushEnabled() && this.mPreference.isEnabled()) {
                    this.mPreference.setSummary(R.string.android_beam_on_summary);
                    break;
                } else {
                    this.mPreference.setSummary(R.string.android_beam_off_summary);
                    break;
                }
                break;
            case 4:
                this.mPreference.setEnabled(false);
                break;
        }
    }
}
