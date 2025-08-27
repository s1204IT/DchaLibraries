package com.android.settings;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.provider.SearchIndexableResource;
import android.support.v7.preference.PreferenceScreen;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.search.Indexable;
import java.util.Arrays;
import java.util.List;

/* loaded from: classes.dex */
public class LegalSettings extends SettingsPreferenceFragment implements Indexable {
    static final String KEY_WALLPAPER_ATTRIBUTIONS = "wallpaper_attributions";
    public static final Indexable.SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER = new BaseSearchIndexProvider() { // from class: com.android.settings.LegalSettings.1
        @Override // com.android.settings.search.BaseSearchIndexProvider, com.android.settings.search.Indexable.SearchIndexProvider
        public List<SearchIndexableResource> getXmlResourcesToIndex(Context context, boolean z) {
            SearchIndexableResource searchIndexableResource = new SearchIndexableResource(context);
            searchIndexableResource.xmlResId = R.xml.about_legal;
            return Arrays.asList(searchIndexableResource);
        }

        @Override // com.android.settings.search.BaseSearchIndexProvider, com.android.settings.search.Indexable.SearchIndexProvider
        public List<String> getNonIndexableKeys(Context context) {
            List<String> nonIndexableKeys = super.getNonIndexableKeys(context);
            if (!checkIntentAction(context, "android.settings.TERMS")) {
                nonIndexableKeys.add("terms");
            }
            if (!checkIntentAction(context, "android.settings.LICENSE")) {
                nonIndexableKeys.add("license");
            }
            if (!checkIntentAction(context, "android.settings.COPYRIGHT")) {
                nonIndexableKeys.add("copyright");
            }
            if (!checkIntentAction(context, "android.settings.WEBVIEW_LICENSE")) {
                nonIndexableKeys.add("webview_license");
            }
            nonIndexableKeys.add(LegalSettings.KEY_WALLPAPER_ATTRIBUTIONS);
            return nonIndexableKeys;
        }

        private boolean checkIntentAction(Context context, String str) {
            List<ResolveInfo> listQueryIntentActivities = context.getPackageManager().queryIntentActivities(new Intent(str), 0);
            int size = listQueryIntentActivities.size();
            for (int i = 0; i < size; i++) {
                if ((listQueryIntentActivities.get(i).activityInfo.applicationInfo.flags & 1) != 0) {
                    return true;
                }
            }
            return false;
        }
    };

    @Override // com.android.settings.SettingsPreferenceFragment, com.android.settingslib.core.lifecycle.ObservablePreferenceFragment, android.support.v14.preference.PreferenceFragment, android.app.Fragment
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        addPreferencesFromResource(R.xml.about_legal);
        Activity activity = getActivity();
        PreferenceScreen preferenceScreen = getPreferenceScreen();
        Utils.updatePreferenceToSpecificActivityOrRemove(activity, preferenceScreen, "terms", 1);
        Utils.updatePreferenceToSpecificActivityOrRemove(activity, preferenceScreen, "license", 1);
        Utils.updatePreferenceToSpecificActivityOrRemove(activity, preferenceScreen, "copyright", 1);
        Utils.updatePreferenceToSpecificActivityOrRemove(activity, preferenceScreen, "webview_license", 1);
        checkWallpaperAttributionAvailability(activity);
    }

    @Override // com.android.settingslib.core.instrumentation.Instrumentable
    public int getMetricsCategory() {
        return 225;
    }

    void checkWallpaperAttributionAvailability(Context context) {
        if (!context.getResources().getBoolean(R.bool.config_show_wallpaper_attribution)) {
            removePreference(KEY_WALLPAPER_ATTRIBUTIONS);
        }
    }
}
