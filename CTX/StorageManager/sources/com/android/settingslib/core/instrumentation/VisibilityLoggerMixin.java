package com.android.settingslib.core.instrumentation;

import android.arch.lifecycle.Lifecycle;
import android.arch.lifecycle.LifecycleObserver;
import android.arch.lifecycle.OnLifecycleEvent;
import android.os.SystemClock;

public class VisibilityLoggerMixin implements LifecycleObserver {
    private MetricsFeatureProvider mMetricsFeature;
    private long mVisibleTimestamp;
    private int mSourceMetricsCategory = 0;
    private final int mMetricsCategory = 0;

    private VisibilityLoggerMixin() {
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
}
