package com.android.settings.support;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import com.android.settings.R;
import com.android.settings.overlay.FeatureFactory;
import com.android.settings.overlay.SupportFeatureProvider;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.search.Indexable;
import com.android.settings.search.SearchIndexableRaw;
import java.util.ArrayList;
import java.util.List;

/* loaded from: classes.dex */
public class SupportDashboardActivity extends Activity implements Indexable {
    public static final Indexable.SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER = new BaseSearchIndexProvider() { // from class: com.android.settings.support.SupportDashboardActivity.1
        @Override // com.android.settings.search.BaseSearchIndexProvider, com.android.settings.search.Indexable.SearchIndexProvider
        public List<SearchIndexableRaw> getRawDataToIndex(Context context, boolean z) {
            ArrayList arrayList = new ArrayList();
            SearchIndexableRaw searchIndexableRaw = new SearchIndexableRaw(context);
            searchIndexableRaw.title = context.getString(R.string.page_tab_title_support);
            searchIndexableRaw.screenTitle = context.getString(R.string.settings_label);
            searchIndexableRaw.summaryOn = context.getString(R.string.support_summary);
            searchIndexableRaw.iconResId = R.drawable.ic_homepage_support;
            searchIndexableRaw.intentTargetPackage = context.getPackageName();
            searchIndexableRaw.intentTargetClass = SupportDashboardActivity.class.getName();
            searchIndexableRaw.intentAction = "android.intent.action.MAIN";
            searchIndexableRaw.key = "support_dashboard_activity";
            arrayList.add(searchIndexableRaw);
            return arrayList;
        }

        @Override // com.android.settings.search.BaseSearchIndexProvider, com.android.settings.search.Indexable.SearchIndexProvider
        public List<String> getNonIndexableKeys(Context context) {
            List<String> nonIndexableKeys = super.getNonIndexableKeys(context);
            if (!context.getResources().getBoolean(R.bool.config_support_enabled)) {
                nonIndexableKeys.add("support_dashboard_activity");
            }
            return nonIndexableKeys;
        }
    };

    @Override // android.app.Activity
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        SupportFeatureProvider supportFeatureProvider = FeatureFactory.getFactory(this).getSupportFeatureProvider(this);
        if (supportFeatureProvider != null) {
            supportFeatureProvider.startSupportV2(this);
            finish();
        }
    }
}
