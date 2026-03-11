package com.android.settings.core;

import android.os.Bundle;
import com.android.settings.overlay.FeatureFactory;
import com.android.settingslib.core.instrumentation.Instrumentable;
import com.android.settingslib.core.instrumentation.VisibilityLoggerMixin;
import com.android.settingslib.core.lifecycle.ObservableActivity;

public abstract class InstrumentedActivity extends ObservableActivity implements Instrumentable {
    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        getLifecycle().addObserver(new VisibilityLoggerMixin(getMetricsCategory(), FeatureFactory.getFactory(this).getMetricsFeatureProvider()));
    }
}
