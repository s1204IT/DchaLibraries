package com.android.settings.enterprise;

import android.content.Context;
import java.util.Date;

public class SecurityLogsPreferenceController extends AdminActionPreferenceControllerBase {
    public SecurityLogsPreferenceController(Context context) {
        super(context);
    }

    @Override
    protected Date getAdminActionTimestamp() {
        return this.mFeatureProvider.getLastSecurityLogRetrievalTime();
    }

    @Override
    public boolean isAvailable() {
        return this.mFeatureProvider.isSecurityLoggingEnabled() || this.mFeatureProvider.getLastSecurityLogRetrievalTime() != null;
    }

    @Override
    public String getPreferenceKey() {
        return "security_logs";
    }
}
