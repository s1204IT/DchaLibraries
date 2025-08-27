package com.android.settings.display;

import android.content.Context;
import android.provider.SearchIndexableResource;
import com.android.internal.hardware.AmbientDisplayConfiguration;
import com.android.settings.R;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settings.display.AmbientDisplayAlwaysOnPreferenceController;
import com.android.settings.gestures.DoubleTapScreenPreferenceController;
import com.android.settings.gestures.PickupGesturePreferenceController;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.search.Indexable;
import java.util.ArrayList;
import java.util.List;

/* loaded from: classes.dex */
public class AmbientDisplaySettings extends DashboardFragment {
    public static final Indexable.SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER = new BaseSearchIndexProvider() { // from class: com.android.settings.display.AmbientDisplaySettings.1
        @Override // com.android.settings.search.BaseSearchIndexProvider, com.android.settings.search.Indexable.SearchIndexProvider
        public List<SearchIndexableResource> getXmlResourcesToIndex(Context context, boolean z) {
            ArrayList arrayList = new ArrayList();
            SearchIndexableResource searchIndexableResource = new SearchIndexableResource(context);
            searchIndexableResource.xmlResId = R.xml.ambient_display_settings;
            arrayList.add(searchIndexableResource);
            return arrayList;
        }
    };
    private AmbientDisplayConfiguration mConfig;

    @Override // com.android.settings.dashboard.DashboardFragment, com.android.settings.core.InstrumentedPreferenceFragment, com.android.settingslib.core.lifecycle.ObservablePreferenceFragment, android.app.Fragment
    public void onAttach(Context context) {
        super.onAttach(context);
        ((AmbientDisplayAlwaysOnPreferenceController) use(AmbientDisplayAlwaysOnPreferenceController.class)).setConfig(getConfig(context)).setCallback(new AmbientDisplayAlwaysOnPreferenceController.OnPreferenceChangedCallback() { // from class: com.android.settings.display.-$$Lambda$AmbientDisplaySettings$EZV3GOIvjt5KUMzbyw8y8_onopw
            @Override // com.android.settings.display.AmbientDisplayAlwaysOnPreferenceController.OnPreferenceChangedCallback
            public final void onPreferenceChanged() {
                this.f$0.updatePreferenceStates();
            }
        });
        ((AmbientDisplayNotificationsPreferenceController) use(AmbientDisplayNotificationsPreferenceController.class)).setConfig(getConfig(context));
        ((DoubleTapScreenPreferenceController) use(DoubleTapScreenPreferenceController.class)).setConfig(getConfig(context));
        ((PickupGesturePreferenceController) use(PickupGesturePreferenceController.class)).setConfig(getConfig(context));
    }

    @Override // com.android.settings.dashboard.DashboardFragment
    protected String getLogTag() {
        return "AmbientDisplaySettings";
    }

    @Override // com.android.settings.dashboard.DashboardFragment, com.android.settings.core.InstrumentedPreferenceFragment
    protected int getPreferenceScreenResId() {
        return R.xml.ambient_display_settings;
    }

    @Override // com.android.settingslib.core.instrumentation.Instrumentable
    public int getMetricsCategory() {
        return 1003;
    }

    private AmbientDisplayConfiguration getConfig(Context context) {
        if (this.mConfig == null) {
            this.mConfig = new AmbientDisplayConfiguration(context);
        }
        return this.mConfig;
    }
}
