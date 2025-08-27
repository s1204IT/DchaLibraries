package com.android.settings.enterprise;

import android.content.Context;
import java.util.Date;

/* loaded from: classes.dex */
public class NetworkLogsPreferenceController extends AdminActionPreferenceControllerBase {
    public NetworkLogsPreferenceController(Context context) {
        super(context);
    }

    @Override // com.android.settings.enterprise.AdminActionPreferenceControllerBase
    protected Date getAdminActionTimestamp() {
        return this.mFeatureProvider.getLastNetworkLogRetrievalTime();
    }

    @Override // com.android.settings.enterprise.AdminActionPreferenceControllerBase, com.android.settingslib.core.AbstractPreferenceController
    public boolean isAvailable() {
        return this.mFeatureProvider.isNetworkLoggingEnabled() || this.mFeatureProvider.getLastNetworkLogRetrievalTime() != null;
    }

    @Override // com.android.settingslib.core.AbstractPreferenceController
    public String getPreferenceKey() {
        return "network_logs";
    }
}
