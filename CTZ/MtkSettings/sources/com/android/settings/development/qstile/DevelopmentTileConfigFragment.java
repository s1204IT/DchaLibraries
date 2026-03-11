package com.android.settings.development.qstile;

import android.content.Context;
import com.android.settings.R;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settingslib.core.AbstractPreferenceController;
import java.util.ArrayList;
import java.util.List;

public class DevelopmentTileConfigFragment extends DashboardFragment {
    @Override
    protected String getLogTag() {
        return "DevelopmentTileConfig";
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.development_tile_settings;
    }

    @Override
    protected List<AbstractPreferenceController> createPreferenceControllers(Context context) {
        ArrayList arrayList = new ArrayList();
        arrayList.add(new DevelopmentTilePreferenceController(context));
        return arrayList;
    }

    @Override
    public int getMetricsCategory() {
        return 1224;
    }
}
