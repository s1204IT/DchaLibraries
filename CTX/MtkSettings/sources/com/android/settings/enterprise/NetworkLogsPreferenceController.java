package com.android.settings.enterprise;

import android.content.Context;
import java.util.Date;

public class NetworkLogsPreferenceController extends AdminActionPreferenceControllerBase {
    public NetworkLogsPreferenceController(Context context) {
        super(context);
    }

    @Override
    protected Date getAdminActionTimestamp() {
        return this.mFeatureProvider.getLastNetworkLogRetrievalTime();
    }

    @Override
    public boolean isAvailable() {
        return this.mFeatureProvider.isNetworkLoggingEnabled() || this.mFeatureProvider.getLastNetworkLogRetrievalTime() != null;
    }

    @Override
    public String getPreferenceKey() {
        return "network_logs";
    }
}
