package com.android.settingslib.core.instrumentation;

import android.app.Activity;
import android.arch.lifecycle.Lifecycle;
import android.arch.lifecycle.LifecycleObserver;
import android.arch.lifecycle.OnLifecycleEvent;
import android.content.Intent;
import android.os.SystemClock;

/* loaded from: classes.dex */
public class VisibilityLoggerMixin implements LifecycleObserver {
    private final int mMetricsCategory;
    private MetricsFeatureProvider mMetricsFeature;
    private int mSourceMetricsCategory;
    private long mVisibleTimestamp;

    private VisibilityLoggerMixin() {
        this.mSourceMetricsCategory = 0;
        this.mMetricsCategory = 0;
    }

    public VisibilityLoggerMixin(int i, MetricsFeatureProvider metricsFeatureProvider) {
        this.mSourceMetricsCategory = 0;
        this.mMetricsCategory = i;
        this.mMetricsFeature = metricsFeatureProvider;
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
    public void onResume() {
        this.mVisibleTimestamp = SystemClock.elapsedRealtime();
        if (this.mMetricsFeature != null && this.mMetricsCategory != 0) {
            this.mMetricsFeature.visible(null, this.mSourceMetricsCategory, this.mMetricsCategory);
        }
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_PAUSE)
    public void onPause() {
        this.mVisibleTimestamp = 0L;
        if (this.mMetricsFeature != null && this.mMetricsCategory != 0) {
            this.mMetricsFeature.hidden(null, this.mMetricsCategory);
        }
    }

    public void setSourceMetricsCategory(Activity activity) {
        Intent intent;
        if (this.mSourceMetricsCategory != 0 || activity == null || (intent = activity.getIntent()) == null) {
            return;
        }
        this.mSourceMetricsCategory = intent.getIntExtra(":settings:source_metrics", 0);
    }

    public long elapsedTimeSinceVisible() {
        if (this.mVisibleTimestamp == 0) {
            return 0L;
        }
        return SystemClock.elapsedRealtime() - this.mVisibleTimestamp;
    }
}
