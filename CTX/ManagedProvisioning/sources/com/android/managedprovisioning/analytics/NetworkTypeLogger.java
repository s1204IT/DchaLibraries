package com.android.managedprovisioning.analytics;

import android.content.Context;
import android.net.NetworkInfo;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.util.Preconditions;
import com.android.managedprovisioning.common.Utils;

public class NetworkTypeLogger {
    private final Context mContext;
    private final MetricsLoggerWrapper mMetricsLoggerWrapper;
    private final Utils mUtils;

    public NetworkTypeLogger(Context context) {
        this(context, new Utils(), new MetricsLoggerWrapper());
    }

    @VisibleForTesting
    NetworkTypeLogger(Context context, Utils utils, MetricsLoggerWrapper metricsLoggerWrapper) {
        this.mContext = (Context) Preconditions.checkNotNull(context);
        this.mUtils = (Utils) Preconditions.checkNotNull(utils);
        this.mMetricsLoggerWrapper = (MetricsLoggerWrapper) Preconditions.checkNotNull(metricsLoggerWrapper);
    }

    public void log() {
        NetworkInfo activeNetworkInfo = this.mUtils.getActiveNetworkInfo(this.mContext);
        if (this.mUtils.isConnectedToNetwork(this.mContext)) {
            this.mMetricsLoggerWrapper.logAction(this.mContext, 610, activeNetworkInfo.getType());
        } else {
            this.mMetricsLoggerWrapper.logAction(this.mContext, 610, "network_type_not_connected");
        }
    }
}
