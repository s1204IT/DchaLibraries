package com.android.settings;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProviderInfo;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import com.android.settings.ActivityPicker;
import com.android.settings.AppWidgetLoader;
import java.util.List;

public class AppWidgetPickActivity extends ActivityPicker implements AppWidgetLoader.ItemConstructor<ActivityPicker.PickAdapter.Item> {
    private int mAppWidgetId;
    private AppWidgetLoader<ActivityPicker.PickAdapter.Item> mAppWidgetLoader;
    private AppWidgetManager mAppWidgetManager;
    List<ActivityPicker.PickAdapter.Item> mItems;
    private PackageManager mPackageManager;

    @Override
    public void onCreate(Bundle icicle) {
        this.mPackageManager = getPackageManager();
        this.mAppWidgetManager = AppWidgetManager.getInstance(this);
        this.mAppWidgetLoader = new AppWidgetLoader<>(this, this.mAppWidgetManager, this);
        super.onCreate(icicle);
        setResultData(0, null);
        Intent intent = getIntent();
        if (intent.hasExtra("appWidgetId")) {
            this.mAppWidgetId = intent.getIntExtra("appWidgetId", 0);
        } else {
            finish();
        }
    }

    @Override
    protected List<ActivityPicker.PickAdapter.Item> getItems() {
        this.mItems = this.mAppWidgetLoader.getItems(getIntent());
        return this.mItems;
    }

    @Override
    public ActivityPicker.PickAdapter.Item createItem(Context context, AppWidgetProviderInfo info, Bundle extras) {
        int density;
        CharSequence label = info.label;
        Drawable icon = null;
        if (info.icon != 0) {
            try {
                Resources res = context.getResources();
                density = res.getDisplayMetrics().densityDpi;
            } catch (PackageManager.NameNotFoundException e) {
                Log.w("AppWidgetPickActivity", "Can't load icon drawable 0x" + Integer.toHexString(info.icon) + " for provider: " + info.provider);
            }
            switch (density) {
                case 160:
                case 213:
                case 240:
                case 320:
                case 480:
                default:
                    int iconDensity = (int) ((density * 0.75f) + 0.5f);
                    Resources packageResources = this.mPackageManager.getResourcesForApplication(info.provider.getPackageName());
                    icon = packageResources.getDrawableForDensity(info.icon, iconDensity);
                    if (icon == null) {
                        Log.w("AppWidgetPickActivity", "Can't load icon drawable 0x" + Integer.toHexString(info.icon) + " for provider: " + info.provider);
                    }
                    break;
            }
            Log.w("AppWidgetPickActivity", "Can't load icon drawable 0x" + Integer.toHexString(info.icon) + " for provider: " + info.provider);
            if (icon == null) {
            }
        }
        ActivityPicker.PickAdapter.Item item = new ActivityPicker.PickAdapter.Item(context, label, icon);
        item.packageName = info.provider.getPackageName();
        item.className = info.provider.getClassName();
        item.extras = extras;
        return item;
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        int result;
        Intent intent = getIntentForPosition(which);
        ActivityPicker.PickAdapter.Item item = this.mItems.get(which);
        if (item.extras != null) {
            setResultData(-1, intent);
        } else {
            Bundle options = null;
            try {
                if (intent.getExtras() != null) {
                    options = intent.getExtras().getBundle("appWidgetOptions");
                }
                this.mAppWidgetManager.bindAppWidgetId(this.mAppWidgetId, intent.getComponent(), options);
                result = -1;
            } catch (IllegalArgumentException e) {
                result = 0;
            }
            setResultData(result, null);
        }
        finish();
    }

    void setResultData(int code, Intent intent) {
        Intent result = intent != null ? intent : new Intent();
        result.putExtra("appWidgetId", this.mAppWidgetId);
        setResult(code, result);
    }
}
