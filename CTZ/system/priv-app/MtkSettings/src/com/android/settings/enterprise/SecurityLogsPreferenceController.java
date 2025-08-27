package com.android.settings.enterprise;

import android.content.Context;
import java.util.Date;

/* loaded from: classes.dex */
public class SecurityLogsPreferenceController extends AdminActionPreferenceControllerBase {
    public SecurityLogsPreferenceController(Context context) {
        super(context);
    }

    @Override // com.android.settings.enterprise.AdminActionPreferenceControllerBase
    protected Date getAdminActionTimestamp() {
        return this.mFeatureProvider.getLastSecurityLogRetrievalTime();
    }

    @Override // com.android.settings.enterprise.AdminActionPreferenceControllerBase, com.android.settingslib.core.AbstractPreferenceController
    public boolean isAvailable() {
        return this.mFeatureProvider.isSecurityLoggingEnabled() || this.mFeatureProvider.getLastSecurityLogRetrievalTime() != null;
    }

    @Override // com.android.settingslib.core.AbstractPreferenceController
    public String getPreferenceKey() {
        return "security_logs";
    }
}
