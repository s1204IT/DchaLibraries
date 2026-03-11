package com.android.settings.dashboard;

import android.app.Activity;
import android.support.v7.preference.Preference;
import com.android.settingslib.drawer.DashboardCategory;
import com.android.settingslib.drawer.Tile;
import java.util.List;

public interface DashboardFeatureProvider {
    void bindPreferenceToTile(Activity activity, int i, Preference preference, Tile tile, String str, int i2);

    List<DashboardCategory> getAllCategories();

    String getDashboardKeyForTile(Tile tile);

    DashboardCategory getTilesForCategory(String str);

    void openTileIntent(Activity activity, Tile tile);
}
