package com.android.managedprovisioning.analytics;

import android.content.Context;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.util.Preconditions;

public class TimeLogger {
    private final AnalyticsUtils mAnalyticsUtils;
    private final int mCategory;
    private final Context mContext;
    private final MetricsLoggerWrapper mMetricsLoggerWrapper;
    private Long mStartTime;

    public TimeLogger(Context context, int i) {
        this(context, i, new MetricsLoggerWrapper(), new AnalyticsUtils());
    }

    @VisibleForTesting
    TimeLogger(Context context, int i, MetricsLoggerWrapper metricsLoggerWrapper, AnalyticsUtils analyticsUtils) {
        this.mContext = (Context) Preconditions.checkNotNull(context);
        this.mCategory = ((Integer) Preconditions.checkNotNull(Integer.valueOf(i))).intValue();
        this.mMetricsLoggerWrapper = (MetricsLoggerWrapper) Preconditions.checkNotNull(metricsLoggerWrapper);
        this.mAnalyticsUtils = (AnalyticsUtils) Preconditions.checkNotNull(analyticsUtils);
    }

    public void start() {
        this.mStartTime = this.mAnalyticsUtils.elapsedRealTime();
    }

    public void stop() {
        if (this.mStartTime != null) {
            int iLongValue = (int) (this.mAnalyticsUtils.elapsedRealTime().longValue() - this.mStartTime.longValue());
            this.mStartTime = null;
            this.mMetricsLoggerWrapper.logAction(this.mContext, this.mCategory, iLongValue);
        }
    }
}
