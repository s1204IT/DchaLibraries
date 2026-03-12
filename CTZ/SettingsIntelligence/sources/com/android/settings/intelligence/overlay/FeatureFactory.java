package com.android.settings.intelligence.overlay;

import android.content.Context;
import android.text.TextUtils;
import com.android.settings.intelligence.R;
import com.android.settings.intelligence.experiment.ExperimentFeatureProvider;
import com.android.settings.intelligence.instrumentation.MetricsFeatureProvider;
import com.android.settings.intelligence.search.SearchFeatureProvider;
import com.android.settings.intelligence.suggestions.SuggestionFeatureProvider;

public abstract class FeatureFactory {
    protected static FeatureFactory sFactory;

    public abstract ExperimentFeatureProvider experimentFeatureProvider();

    public abstract MetricsFeatureProvider metricsFeatureProvider(Context context);

    public abstract SearchFeatureProvider searchFeatureProvider();

    public abstract SuggestionFeatureProvider suggestionFeatureProvider();

    public static FeatureFactory get(Context context) {
        if (sFactory != null) {
            return sFactory;
        }
        String string = context.getString(R.string.config_featureFactory);
        if (TextUtils.isEmpty(string)) {
            throw new UnsupportedOperationException("No feature factory configured");
        }
        try {
            sFactory = (FeatureFactory) context.getClassLoader().loadClass(string).newInstance();
            return sFactory;
        } catch (ClassNotFoundException | IllegalAccessException | InstantiationException e) {
            throw new FactoryNotFoundException(e);
        }
    }

    public static final class FactoryNotFoundException extends RuntimeException {
        public FactoryNotFoundException(Throwable th) {
            super("Unable to create factory. Did you misconfigure Proguard?", th);
        }
    }
}
