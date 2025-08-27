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

/* loaded from: classes.dex */
public class AppWidgetPickActivity extends ActivityPicker implements AppWidgetLoader.ItemConstructor<ActivityPicker.PickAdapter.Item> {
    private int mAppWidgetId;
    private AppWidgetLoader<ActivityPicker.PickAdapter.Item> mAppWidgetLoader;
    private AppWidgetManager mAppWidgetManager;
    List<ActivityPicker.PickAdapter.Item> mItems;
    private PackageManager mPackageManager;

    /* JADX DEBUG: Multi-variable search result rejected for r2v0, resolved type: com.android.settings.AppWidgetPickActivity */
    /* JADX WARN: Multi-variable type inference failed */
    @Override // com.android.settings.ActivityPicker
    public void onCreate(Bundle bundle) {
        this.mPackageManager = getPackageManager();
        this.mAppWidgetManager = AppWidgetManager.getInstance(this);
        this.mAppWidgetLoader = new AppWidgetLoader<>(this, this.mAppWidgetManager, this);
        super.onCreate(bundle);
        setResultData(0, null);
        Intent intent = getIntent();
        if (intent.hasExtra("appWidgetId")) {
            this.mAppWidgetId = intent.getIntExtra("appWidgetId", 0);
        } else {
            finish();
        }
    }

    /* JADX DEBUG: Type inference failed for r0v1. Raw type applied. Possible types: java.util.List<Item extends com.android.settings.AppWidgetLoader$LabelledItem>, java.util.List<com.android.settings.ActivityPicker$PickAdapter$Item> */
    @Override // com.android.settings.ActivityPicker
    protected List<ActivityPicker.PickAdapter.Item> getItems() {
        this.mItems = this.mAppWidgetLoader.getItems(getIntent());
        return this.mItems;
    }

    /* JADX DEBUG: Method merged with bridge method: createItem(Landroid/content/Context;Landroid/appwidget/AppWidgetProviderInfo;Landroid/os/Bundle;)Ljava/lang/Object; */
    /* JADX WARN: Can't rename method to resolve collision */
    @Override // com.android.settings.AppWidgetLoader.ItemConstructor
    public ActivityPicker.PickAdapter.Item createItem(Context context, AppWidgetProviderInfo appWidgetProviderInfo, Bundle bundle) throws Resources.NotFoundException, PackageManager.NameNotFoundException {
        String str = appWidgetProviderInfo.label;
        Drawable drawableForDensity = null;
        if (appWidgetProviderInfo.icon != 0) {
            try {
                int i = context.getResources().getDisplayMetrics().densityDpi;
                if (i == 160 || i == 213 || i == 240 || i == 320 || i != 480) {
                }
                drawableForDensity = this.mPackageManager.getResourcesForApplication(appWidgetProviderInfo.provider.getPackageName()).getDrawableForDensity(appWidgetProviderInfo.icon, (int) ((i * 0.75f) + 0.5f));
            } catch (PackageManager.NameNotFoundException e) {
                Log.w("AppWidgetPickActivity", "Can't load icon drawable 0x" + Integer.toHexString(appWidgetProviderInfo.icon) + " for provider: " + appWidgetProviderInfo.provider);
            }
            if (drawableForDensity == null) {
                Log.w("AppWidgetPickActivity", "Can't load icon drawable 0x" + Integer.toHexString(appWidgetProviderInfo.icon) + " for provider: " + appWidgetProviderInfo.provider);
            }
        }
        ActivityPicker.PickAdapter.Item item = new ActivityPicker.PickAdapter.Item(context, str, drawableForDensity);
        item.packageName = appWidgetProviderInfo.provider.getPackageName();
        item.className = appWidgetProviderInfo.provider.getClassName();
        item.extras = bundle;
        return item;
    }

    @Override // com.android.settings.ActivityPicker, android.content.DialogInterface.OnClickListener
    public void onClick(DialogInterface dialogInterface, int i) {
        Bundle bundle;
        Intent intentForPosition = getIntentForPosition(i);
        int i2 = -1;
        if (this.mItems.get(i).extras != null) {
            setResultData(-1, intentForPosition);
        } else {
            try {
                if (intentForPosition.getExtras() != null) {
                    bundle = intentForPosition.getExtras().getBundle("appWidgetOptions");
                } else {
                    bundle = null;
                }
                this.mAppWidgetManager.bindAppWidgetId(this.mAppWidgetId, intentForPosition.getComponent(), bundle);
            } catch (IllegalArgumentException e) {
                i2 = 0;
            }
            setResultData(i2, null);
        }
        finish();
    }

    void setResultData(int i, Intent intent) {
        if (intent == null) {
            intent = new Intent();
        }
        intent.putExtra("appWidgetId", this.mAppWidgetId);
        setResult(i, intent);
    }
}
