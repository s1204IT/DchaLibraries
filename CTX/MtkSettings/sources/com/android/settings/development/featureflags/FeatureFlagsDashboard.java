package com.android.settings.development.featureflags;

import android.content.Context;
import com.android.settings.R;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.core.lifecycle.Lifecycle;
import java.util.ArrayList;
import java.util.List;

public class FeatureFlagsDashboard extends DashboardFragment {
    @Override
    public int getMetricsCategory() {
        return 1217;
    }

    @Override
    protected String getLogTag() {
        return "FeatureFlagsDashboard";
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.feature_flags_settings;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        ((FeatureFlagFooterPreferenceController) use(FeatureFlagFooterPreferenceController.class)).setFooterMixin(this.mFooterPreferenceMixin);
    }

    @Override
    public int getHelpResource() {
        return 0;
    }

    @Override
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
