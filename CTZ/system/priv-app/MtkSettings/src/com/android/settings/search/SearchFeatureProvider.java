package com.android.settings.search;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.view.View;
import android.widget.Toolbar;
import com.android.settings.overlay.FeatureFactory;

/* loaded from: classes.dex */
public interface SearchFeatureProvider {
    public static final Intent SEARCH_UI_INTENT = new Intent("com.android.settings.action.SETTINGS_SEARCH");

    SearchIndexableResources getSearchIndexableResources();

    void verifyLaunchSearchResultPageCaller(Context context, ComponentName componentName) throws SecurityException, IllegalArgumentException;

    default String getSettingsIntelligencePkgName() {
        return "com.android.settings.intelligence";
    }

    default void initSearchToolbar(final Activity activity, Toolbar toolbar) {
        if (activity == null || toolbar == null) {
            return;
        }
        toolbar.setOnClickListener(new View.OnClickListener() { // from class: com.android.settings.search.-$$Lambda$SearchFeatureProvider$7ZGLG3tZpGqHgt7m_jMbwikwfJM
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                SearchFeatureProvider.lambda$initSearchToolbar$0(this.f$0, activity, view);
            }
        });
    }

    static /* synthetic */ void lambda$initSearchToolbar$0(SearchFeatureProvider searchFeatureProvider, Activity activity, View view) {
        Intent intent = SEARCH_UI_INTENT;
        intent.setPackage(searchFeatureProvider.getSettingsIntelligencePkgName());
        FeatureFactory.getFactory(activity.getApplicationContext()).getSlicesFeatureProvider().indexSliceDataAsync(activity.getApplicationContext());
        activity.startActivityForResult(intent, 0);
    }
}
