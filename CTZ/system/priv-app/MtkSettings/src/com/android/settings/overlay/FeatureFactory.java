package com.android.settings.overlay;

import android.content.Context;
import android.text.TextUtils;
import com.android.settings.R;
import com.android.settings.accounts.AccountFeatureProvider;
import com.android.settings.applications.ApplicationFeatureProvider;
import com.android.settings.dashboard.DashboardFeatureProvider;
import com.android.settings.dashboard.suggestions.SuggestionFeatureProvider;
import com.android.settings.enterprise.EnterprisePrivacyFeatureProvider;
import com.android.settings.fuelgauge.PowerUsageFeatureProvider;
import com.android.settings.gestures.AssistGestureFeatureProvider;
import com.android.settings.localepicker.LocaleFeatureProvider;
import com.android.settings.search.DeviceIndexFeatureProvider;
import com.android.settings.search.SearchFeatureProvider;
import com.android.settings.security.SecurityFeatureProvider;
import com.android.settings.slices.SlicesFeatureProvider;
import com.android.settings.users.UserFeatureProvider;
import com.android.settingslib.core.instrumentation.MetricsFeatureProvider;
/* loaded from: classes.dex */
public abstract class FeatureFactory {
    protected static FeatureFactory sFactory;

    public abstract AccountFeatureProvider getAccountFeatureProvider();

    public abstract ApplicationFeatureProvider getApplicationFeatureProvider(Context context);

    public abstract AssistGestureFeatureProvider getAssistGestureFeatureProvider();

    public abstract DashboardFeatureProvider getDashboardFeatureProvider(Context context);

    public abstract DeviceIndexFeatureProvider getDeviceIndexFeatureProvider();

    public abstract DockUpdaterFeatureProvider getDockUpdaterFeatureProvider();

    public abstract EnterprisePrivacyFeatureProvider getEnterprisePrivacyFeatureProvider(Context context);

    public abstract LocaleFeatureProvider getLocaleFeatureProvider();

    public abstract MetricsFeatureProvider getMetricsFeatureProvider();

    public abstract PowerUsageFeatureProvider getPowerUsageFeatureProvider(Context context);

    public abstract SearchFeatureProvider getSearchFeatureProvider();

    public abstract SecurityFeatureProvider getSecurityFeatureProvider();

    public abstract SlicesFeatureProvider getSlicesFeatureProvider();

    public abstract SuggestionFeatureProvider getSuggestionFeatureProvider(Context context);

    public abstract SupportFeatureProvider getSupportFeatureProvider(Context context);

    public abstract SurveyFeatureProvider getSurveyFeatureProvider(Context context);

    public abstract UserFeatureProvider getUserFeatureProvider(Context context);

    public static FeatureFactory getFactory(Context context) {
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

    /* loaded from: classes.dex */
    public static final class FactoryNotFoundException extends RuntimeException {
        public FactoryNotFoundException(Throwable th) {
            super("Unable to create factory. Did you misconfigure Proguard?", th);
        }
    }
}
