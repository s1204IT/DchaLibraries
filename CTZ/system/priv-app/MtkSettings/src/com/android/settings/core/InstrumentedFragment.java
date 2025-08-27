package com.android.settings.core;

import android.content.Context;
import com.android.settings.overlay.FeatureFactory;
import com.android.settings.survey.SurveyMixin;
import com.android.settingslib.core.instrumentation.Instrumentable;
import com.android.settingslib.core.instrumentation.MetricsFeatureProvider;
import com.android.settingslib.core.instrumentation.VisibilityLoggerMixin;
import com.android.settingslib.core.lifecycle.ObservableFragment;

/* loaded from: classes.dex */
public abstract class InstrumentedFragment extends ObservableFragment implements Instrumentable {
    protected MetricsFeatureProvider mMetricsFeatureProvider;
    private VisibilityLoggerMixin mVisibilityLoggerMixin;

    @Override // com.android.settingslib.core.lifecycle.ObservableFragment, android.app.Fragment
    public void onAttach(Context context) {
        this.mMetricsFeatureProvider = FeatureFactory.getFactory(context).getMetricsFeatureProvider();
        this.mVisibilityLoggerMixin = new VisibilityLoggerMixin(getMetricsCategory(), this.mMetricsFeatureProvider);
        getLifecycle().addObserver(this.mVisibilityLoggerMixin);
        getLifecycle().addObserver(new SurveyMixin(this, getClass().getSimpleName()));
        super.onAttach(context);
    }

    @Override // com.android.settingslib.core.lifecycle.ObservableFragment, android.app.Fragment
    public void onResume() {
        this.mVisibilityLoggerMixin.setSourceMetricsCategory(getActivity());
        super.onResume();
    }
}
