package com.android.settings.enterprise;

import android.content.Context;
import android.support.v7.preference.Preference;
import com.android.settings.R;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settings.overlay.FeatureFactory;
import com.android.settingslib.core.AbstractPreferenceController;

/* loaded from: classes.dex */
public abstract class FailedPasswordWipePreferenceControllerBase extends AbstractPreferenceController implements PreferenceControllerMixin {
    protected final EnterprisePrivacyFeatureProvider mFeatureProvider;

    protected abstract int getMaximumFailedPasswordsBeforeWipe();

    public FailedPasswordWipePreferenceControllerBase(Context context) {
        super(context);
        this.mFeatureProvider = FeatureFactory.getFactory(context).getEnterprisePrivacyFeatureProvider(context);
    }

    @Override // com.android.settingslib.core.AbstractPreferenceController
    public void updateState(Preference preference) {
        int maximumFailedPasswordsBeforeWipe = getMaximumFailedPasswordsBeforeWipe();
        preference.setSummary(this.mContext.getResources().getQuantityString(R.plurals.enterprise_privacy_number_failed_password_wipe, maximumFailedPasswordsBeforeWipe, Integer.valueOf(maximumFailedPasswordsBeforeWipe)));
    }

    @Override // com.android.settingslib.core.AbstractPreferenceController
    public boolean isAvailable() {
        return getMaximumFailedPasswordsBeforeWipe() > 0;
    }
}
