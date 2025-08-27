package com.android.settings.connecteddevice;

import android.content.Context;
import android.content.res.Resources;
import com.android.settings.R;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.search.Indexable;
import com.android.settings.search.SearchIndexableRaw;
import java.util.ArrayList;
import java.util.List;

/* loaded from: classes.dex */
public class PreviouslyConnectedDeviceDashboardFragment extends DashboardFragment {
    public static final Indexable.SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER = new BaseSearchIndexProvider() { // from class: com.android.settings.connecteddevice.PreviouslyConnectedDeviceDashboardFragment.1
        @Override // com.android.settings.search.BaseSearchIndexProvider, com.android.settings.search.Indexable.SearchIndexProvider
        public List<SearchIndexableRaw> getRawDataToIndex(Context context, boolean z) {
            ArrayList arrayList = new ArrayList();
            Resources resources = context.getResources();
            SearchIndexableRaw searchIndexableRaw = new SearchIndexableRaw(context);
            searchIndexableRaw.key = "saved_device_list";
            searchIndexableRaw.title = resources.getString(R.string.connected_device_previously_connected_title);
            searchIndexableRaw.screenTitle = resources.getString(R.string.connected_device_previously_connected_title);
            arrayList.add(searchIndexableRaw);
            return arrayList;
        }
    };

    @Override // com.android.settings.support.actionbar.HelpResourceProvider
    public int getHelpResource() {
        return R.string.help_url_previously_connected_devices;
    }

    @Override // com.android.settings.dashboard.DashboardFragment, com.android.settings.core.InstrumentedPreferenceFragment
    protected int getPreferenceScreenResId() {
        return R.xml.previously_connected_devices;
    }

    @Override // com.android.settings.dashboard.DashboardFragment
    protected String getLogTag() {
        return "PreConnectedDeviceFrag";
    }

    @Override // com.android.settingslib.core.instrumentation.Instrumentable
    public int getMetricsCategory() {
        return 1370;
    }

    @Override // com.android.settings.dashboard.DashboardFragment, com.android.settings.core.InstrumentedPreferenceFragment, com.android.settingslib.core.lifecycle.ObservablePreferenceFragment, android.app.Fragment
    public void onAttach(Context context) {
        super.onAttach(context);
        ((SavedDeviceGroupController) use(SavedDeviceGroupController.class)).init(this);
    }
}
