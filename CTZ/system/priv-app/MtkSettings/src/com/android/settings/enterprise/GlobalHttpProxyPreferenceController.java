package com.android.settings.enterprise;

import android.content.Context;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settings.overlay.FeatureFactory;
import com.android.settingslib.core.AbstractPreferenceController;

/* loaded from: classes.dex */
public class GlobalHttpProxyPreferenceController extends AbstractPreferenceController implements PreferenceControllerMixin {
    private final EnterprisePrivacyFeatureProvider mFeatureProvider;

    public GlobalHttpProxyPreferenceController(Context context) {
        super(context);
        this.mFeatureProvider = FeatureFactory.getFactory(context).getEnterprisePrivacyFeatureProvider(context);
    }

    @Override // com.android.settingslib.core.AbstractPreferenceController
    public boolean isAvailable() {
        return this.mFeatureProvider.isGlobalHttpProxySet();
    }

    @Override // com.android.settingslib.core.AbstractPreferenceController
    public String getPreferenceKey() {
        return "global_http_proxy";
    }
}
