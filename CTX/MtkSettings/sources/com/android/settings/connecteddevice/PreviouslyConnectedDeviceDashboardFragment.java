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

public class PreviouslyConnectedDeviceDashboardFragment extends DashboardFragment {
    public static final Indexable.SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER = new BaseSearchIndexProvider() {
        @Override
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

    @Override
    public int getHelpResource() {
        return R.string.help_url_previously_connected_devices;
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.previously_connected_devices;
    }

    @Override
    protected String getLogTag() {
        return "PreConnectedDeviceFrag";
    }

    @Override
    public int getMetricsCategory() {
        return 1370;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        ((SavedDeviceGroupController) use(SavedDeviceGroupController.class)).init(this);
    }
}
