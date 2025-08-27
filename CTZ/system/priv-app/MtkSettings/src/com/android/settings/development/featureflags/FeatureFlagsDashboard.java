package com.android.settings.development.featureflags;

import android.content.Context;
import com.android.settings.R;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.core.lifecycle.Lifecycle;
import java.util.ArrayList;
import java.util.List;

/* loaded from: classes.dex */
public class FeatureFlagsDashboard extends DashboardFragment {
    @Override // com.android.settingslib.core.instrumentation.Instrumentable
    public int getMetricsCategory() {
        return 1217;
    }

    @Override // com.android.settings.dashboard.DashboardFragment
    protected String getLogTag() {
        return "FeatureFlagsDashboard";
    }

    @Override // com.android.settings.dashboard.DashboardFragment, com.android.settings.core.InstrumentedPreferenceFragment
    protected int getPreferenceScreenResId() {
        return R.xml.feature_flags_settings;
    }

    @Override // com.android.settings.dashboard.DashboardFragment, com.android.settings.core.InstrumentedPreferenceFragment, com.android.settingslib.core.lifecycle.ObservablePreferenceFragment, android.app.Fragment
    public void onAttach(Context context) {
        super.onAttach(context);
        ((FeatureFlagFooterPreferenceController) use(FeatureFlagFooterPreferenceController.class)).setFooterMixin(this.mFooterPreferenceMixin);
    }

    @Override // com.android.settings.support.actionbar.HelpResourceProvider
    public int getHelpResource() {
        return 0;
    }

    @Override // com.android.settings.dashboard.DashboardFragment
    protected List<AbstractPreferenceController> createPreferenceControllers(Context context) {
        ArrayList arrayList = new ArrayList();
        Lifecycle lifecycle = getLifecycle();
        FeatureFlagFooterPreferenceController featureFlagFooterPreferenceController = new FeatureFlagFooterPreferenceController(context);
        arrayList.add(new FeatureFlagsPreferenceController(context, lifecycle));
        arrayList.add(featureFlagFooterPreferenceController);
        lifecycle.addObserver(featureFlagFooterPreferenceController);
        return arrayList;
    }
}
