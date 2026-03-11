package com.android.settings.enterprise;

import android.content.Context;
import com.android.settings.R;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settingslib.core.AbstractPreferenceController;
import java.util.ArrayList;
import java.util.List;

public class EnterpriseSetDefaultAppsListFragment extends DashboardFragment {
    @Override
    public int getMetricsCategory() {
        return 940;
    }

    @Override
    protected String getLogTag() {
        return "EnterprisePrivacySettings";
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.enterprise_set_default_apps_settings;
    }

    @Override
    protected List<AbstractPreferenceController> createPreferenceControllers(Context context) {
        ArrayList arrayList = new ArrayList();
        arrayList.add(new EnterpriseSetDefaultAppsListPreferenceController(context, this, context.getPackageManager()));
        return arrayList;
    }
}
