package com.android.settings.enterprise;

import android.content.Context;
import com.android.settings.core.BasePreferenceController;
import com.android.settings.overlay.FeatureFactory;

/* loaded from: classes.dex */
public class BackupsEnabledPreferenceController extends BasePreferenceController {
    private static final String KEY_BACKUPS_ENABLED = "backups_enabled";
    private final EnterprisePrivacyFeatureProvider mFeatureProvider;

    public BackupsEnabledPreferenceController(Context context) {
        super(context, KEY_BACKUPS_ENABLED);
        this.mFeatureProvider = FeatureFactory.getFactory(context).getEnterprisePrivacyFeatureProvider(context);
    }

    @Override // com.android.settings.core.BasePreferenceController
    public int getAvailabilityStatus() {
        return this.mFeatureProvider.areBackupsMandatory() ? 0 : 3;
    }
}
