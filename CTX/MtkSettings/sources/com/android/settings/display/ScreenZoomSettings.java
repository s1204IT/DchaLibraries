package com.android.settings.display;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Bundle;
import com.android.settings.PreviewSeekBarPreferenceFragment;
import com.android.settings.R;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.search.Indexable;
import com.android.settings.search.SearchIndexableRaw;
import com.android.settingslib.display.DisplayDensityUtils;
import java.util.ArrayList;
import java.util.List;

public class ScreenZoomSettings extends PreviewSeekBarPreferenceFragment implements Indexable {
    public static final Indexable.SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER = new BaseSearchIndexProvider() {
        @Override
        public List<SearchIndexableRaw> getRawDataToIndex(Context context, boolean z) {
            Resources resources = context.getResources();
            SearchIndexableRaw searchIndexableRaw = new SearchIndexableRaw(context);
            searchIndexableRaw.title = resources.getString(R.string.screen_zoom_title);
            searchIndexableRaw.key = "screen_zoom_settings";
            searchIndexableRaw.screenTitle = resources.getString(R.string.screen_zoom_title);
            searchIndexableRaw.keywords = resources.getString(R.string.screen_zoom_keywords);
            ArrayList arrayList = new ArrayList(1);
            arrayList.add(searchIndexableRaw);
            return arrayList;
        }
    };
    private int mDefaultDensity;
    private int[] mValues;

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        this.mActivityLayoutResId = R.layout.screen_zoom_activity;
        this.mPreviewSampleResIds = new int[]{R.layout.screen_zoom_preview_1, R.layout.screen_zoom_preview_2, R.layout.screen_zoom_preview_settings};
        DisplayDensityUtils displayDensityUtils = new DisplayDensityUtils(getContext());
        int currentIndex = displayDensityUtils.getCurrentIndex();
        if (currentIndex < 0) {
            int i = getResources().getDisplayMetrics().densityDpi;
            this.mValues = new int[]{i};
            this.mEntries = new String[]{getString(DisplayDensityUtils.SUMMARY_DEFAULT)};
            this.mInitialIndex = 0;
            this.mDefaultDensity = i;
        } else {
            this.mValues = displayDensityUtils.getValues();
            this.mEntries = displayDensityUtils.getEntries();
            this.mInitialIndex = currentIndex;
            this.mDefaultDensity = displayDensityUtils.getDefaultDensity();
        }
        getActivity().setTitle(R.string.screen_zoom_title);
    }

    @Override
    protected Configuration createConfig(Configuration configuration, int i) {
        Configuration configuration2 = new Configuration(configuration);
        configuration2.densityDpi = this.mValues[i];
        return configuration2;
    }

    @Override
    protected void commit() {
        int i = this.mValues[this.mCurrentIndex];
        if (i == this.mDefaultDensity) {
            DisplayDensityUtils.clearForcedDisplayDensity(0);
        } else {
            DisplayDensityUtils.setForcedDisplayDensity(0, i);
        }
    }

    @Override
    public int getHelpResource() {
        return R.string.help_url_display_size;
    }

    public int getMetricsCategory() {
        return 339;
    }
}
