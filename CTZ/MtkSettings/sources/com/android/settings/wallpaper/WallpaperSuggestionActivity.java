package com.android.settings.wallpaper;

import android.app.Activity;
import android.app.WallpaperManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import com.android.settings.R;
import com.android.settings.core.SubSettingLauncher;

public class WallpaperSuggestionActivity extends Activity {
    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        PackageManager packageManager = getPackageManager();
        Intent intentAddFlags = new Intent().setClassName(getString(R.string.config_wallpaper_picker_package), getString(R.string.config_wallpaper_picker_class)).addFlags(33554432);
        if (packageManager.resolveActivity(intentAddFlags, 0) != null) {
            startActivity(intentAddFlags);
        } else {
            startFallbackSuggestion();
        }
        finish();
    }

    void startFallbackSuggestion() {
        new SubSettingLauncher(this).setDestination(WallpaperTypeSettings.class.getName()).setTitle(R.string.wallpaper_suggestion_title).setSourceMetricsCategory(35).addFlags(33554432).launch();
    }

    public static boolean isSuggestionComplete(Context context) {
        return !isWallpaperServiceEnabled(context) || ((WallpaperManager) context.getSystemService("wallpaper")).getWallpaperId(1) > 0;
    }

    private static boolean isWallpaperServiceEnabled(Context context) {
        return context.getResources().getBoolean(android.R.^attr-private.horizontalProgressLayout);
    }
}
