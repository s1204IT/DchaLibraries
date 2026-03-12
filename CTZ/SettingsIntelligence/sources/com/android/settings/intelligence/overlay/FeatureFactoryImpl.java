package com.android.settings.intelligence.overlay;

import android.content.Context;
import com.android.settings.intelligence.experiment.ExperimentFeatureProvider;
import com.android.settings.intelligence.instrumentation.MetricsFeatureProvider;
import com.android.settings.intelligence.search.SearchFeatureProvider;
import com.android.settings.intelligence.search.SearchFeatureProviderImpl;
import com.android.settings.intelligence.suggestions.SuggestionFeatureProvider;

public class FeatureFactoryImpl extends FeatureFactory {
    protected ExperimentFeatureProvider mExperimentFeatureProvider;
    protected MetricsFeatureProvider mMetricsFeatureProvider;
    protected SearchFeatureProvider mSearchFeatureProvider;
    protected SuggestionFeatureProvider mSuggestionFeatureProvider;

    @Override
    public MetricsFeatureProvider metricsFeatureProvider(Context context) {
        if (this.mMetricsFeatureProvider == null) {
            this.mMetricsFeatureProvider = new MetricsFeatureProvider(context.getApplicationContext());
        }
        return this.mMetricsFeatureProvider;
    }

    @Override
    public SuggestionFeatureProvider suggestionFeatureProvider() {
        if (this.mSuggestionFeatureProvider == null) {
            this.mSuggestionFeatureProvider = new SuggestionFeatureProvider();
        }
        return this.mSuggestionFeatureProvider;
    }

    @Override
    public ExperimentFeatureProvider experimentFeatureProvider() {
        if (this.mExperimentFeatureProvider == null) {
            this.mExperimentFeatureProvider = new ExperimentFeatureProvider();
        }
        return this.mExperimentFeatureProvider;
    }

    @Override
    public SearchFeatureProvider searchFeatureProvider() {
        if (this.mSearchFeatureProvider == null) {
            this.mSearchFeatureProvider = new SearchFeatureProviderImpl();
        }
        return this.mSearchFeatureProvider;
    }
}
