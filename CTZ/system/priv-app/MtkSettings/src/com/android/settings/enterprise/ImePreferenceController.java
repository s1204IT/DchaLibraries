package com.android.settings.enterprise;

import android.content.Context;
import android.support.v7.preference.Preference;
import com.android.settings.R;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settings.overlay.FeatureFactory;
import com.android.settingslib.core.AbstractPreferenceController;

/* loaded from: classes.dex */
public class ImePreferenceController extends AbstractPreferenceController implements PreferenceControllerMixin {
    private final EnterprisePrivacyFeatureProvider mFeatureProvider;

    public ImePreferenceController(Context context) {
        super(context);
        this.mFeatureProvider = FeatureFactory.getFactory(context).getEnterprisePrivacyFeatureProvider(context);
    }

    @Override // com.android.settingslib.core.AbstractPreferenceController
    public void updateState(Preference preference) {
        preference.setSummary(this.mContext.getResources().getString(R.string.enterprise_privacy_input_method_name, this.mFeatureProvider.getImeLabelIfOwnerSet()));
    }

    @Override // com.android.settingslib.core.AbstractPreferenceController
    public boolean isAvailable() {
        return this.mFeatureProvider.getImeLabelIfOwnerSet() != null;
    }

    @Override // com.android.settingslib.core.AbstractPreferenceController
    public String getPreferenceKey() {
        return "input_method";
    }
}
