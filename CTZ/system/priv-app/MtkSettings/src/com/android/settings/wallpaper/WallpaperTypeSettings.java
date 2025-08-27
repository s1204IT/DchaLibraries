package com.android.settings.wallpaper;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.search.Indexable;
import com.android.settings.search.SearchIndexableRaw;
import java.util.ArrayList;
import java.util.List;

/* loaded from: classes.dex */
public class WallpaperTypeSettings extends SettingsPreferenceFragment implements Indexable {
    public static final Indexable.SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER = new BaseSearchIndexProvider() { // from class: com.android.settings.wallpaper.WallpaperTypeSettings.1
        @Override // com.android.settings.search.BaseSearchIndexProvider, com.android.settings.search.Indexable.SearchIndexProvider
        public List<SearchIndexableRaw> getRawDataToIndex(Context context, boolean z) {
            ArrayList arrayList = new ArrayList();
            Intent intent = new Intent("android.intent.action.SET_WALLPAPER");
            PackageManager packageManager = context.getPackageManager();
            List<ResolveInfo> listQueryIntentActivities = packageManager.queryIntentActivities(intent, 65536);
            String string = context.getString(R.string.config_wallpaper_picker_package);
            for (ResolveInfo resolveInfo : listQueryIntentActivities) {
                if (string.equals(resolveInfo.activityInfo.packageName)) {
                    CharSequence charSequenceLoadLabel = resolveInfo.loadLabel(packageManager);
                    if (charSequenceLoadLabel == null) {
                        charSequenceLoadLabel = resolveInfo.activityInfo.packageName;
                    }
                    SearchIndexableRaw searchIndexableRaw = new SearchIndexableRaw(context);
                    searchIndexableRaw.title = charSequenceLoadLabel.toString();
                    searchIndexableRaw.key = "wallpaper_type_settings";
                    searchIndexableRaw.screenTitle = context.getResources().getString(R.string.wallpaper_settings_fragment_title);
                    searchIndexableRaw.intentAction = "android.intent.action.SET_WALLPAPER";
                    searchIndexableRaw.intentTargetPackage = resolveInfo.activityInfo.packageName;
                    searchIndexableRaw.intentTargetClass = resolveInfo.activityInfo.name;
                    searchIndexableRaw.keywords = context.getString(R.string.keywords_wallpaper);
                    arrayList.add(searchIndexableRaw);
                }
            }
            return arrayList;
        }
    };

    @Override // com.android.settingslib.core.instrumentation.Instrumentable
    public int getMetricsCategory() {
        return 101;
    }

    @Override // com.android.settings.support.actionbar.HelpResourceProvider
    public int getHelpResource() {
        return R.string.help_uri_wallpaper;
    }

    @Override // com.android.settings.SettingsPreferenceFragment, com.android.settingslib.core.lifecycle.ObservablePreferenceFragment, android.support.v14.preference.PreferenceFragment, android.app.Fragment
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        addPreferencesFromResource(R.xml.wallpaper_settings);
        populateWallpaperTypes();
    }

    private void populateWallpaperTypes() {
        Intent intent = new Intent("android.intent.action.SET_WALLPAPER");
        PackageManager packageManager = getPackageManager();
        List<ResolveInfo> listQueryIntentActivities = packageManager.queryIntentActivities(intent, 65536);
        PreferenceScreen preferenceScreen = getPreferenceScreen();
        preferenceScreen.setOrderingAsAdded(false);
        for (ResolveInfo resolveInfo : listQueryIntentActivities) {
            Preference preference = new Preference(getPrefContext());
            Intent intentAddFlags = new Intent(intent).addFlags(33554432);
            intentAddFlags.setComponent(new ComponentName(resolveInfo.activityInfo.packageName, resolveInfo.activityInfo.name));
            preference.setIntent(intentAddFlags);
            CharSequence charSequenceLoadLabel = resolveInfo.loadLabel(packageManager);
            if (charSequenceLoadLabel == null) {
                charSequenceLoadLabel = resolveInfo.activityInfo.packageName;
            }
            preference.setTitle(charSequenceLoadLabel);
            preference.setIcon(resolveInfo.loadIcon(packageManager));
            preferenceScreen.addPreference(preference);
        }
    }

    @Override // android.support.v14.preference.PreferenceFragment, android.support.v7.preference.PreferenceManager.OnPreferenceTreeClickListener
    public boolean onPreferenceTreeClick(Preference preference) {
        if (preference.getIntent() == null) {
            return super.onPreferenceTreeClick(preference);
        }
        startActivity(preference.getIntent());
        finish();
        return true;
    }
}
