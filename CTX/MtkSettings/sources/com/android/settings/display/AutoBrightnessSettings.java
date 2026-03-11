package com.android.settings.display;

import android.content.Context;
import android.os.Bundle;
import android.provider.SearchIndexableResource;
import com.android.settings.R;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.search.Indexable;
import java.util.Arrays;
import java.util.List;

public class AutoBrightnessSettings extends DashboardFragment {
    public static final Indexable.SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER = new BaseSearchIndexProvider() {
        @Override
        public List<SearchIndexableResource> getXmlResourcesToIndex(Context context, boolean z) {
            SearchIndexableResource searchIndexableResource = new SearchIndexableResource(context);
            searchIndexableResource.xmlResId = R.xml.auto_brightness_detail;
            return Arrays.asList(searchIndexableResource);
        }
    };

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        this.mFooterPreferenceMixin.createFooterPreference().setTitle(R.string.auto_brightness_description);
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.auto_brightness_detail;
    }

    @Override
    protected String getLogTag() {
        return "AutoBrightnessSettings";
    }

    @Override
    public int getMetricsCategory() {
        return 1381;
    }

    @Override
    public int getHelpResource() {
        return R.string.help_url_auto_brightness;
    }
}
