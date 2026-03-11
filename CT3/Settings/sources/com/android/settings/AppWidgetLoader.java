package com.android.settings;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProviderInfo;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import com.android.settings.AppWidgetLoader.LabelledItem;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class AppWidgetLoader<Item extends LabelledItem> {
    private AppWidgetManager mAppWidgetManager;
    private Context mContext;
    ItemConstructor<Item> mItemConstructor;

    public interface ItemConstructor<Item> {
        Item createItem(Context context, AppWidgetProviderInfo appWidgetProviderInfo, Bundle bundle);
    }

    interface LabelledItem {
        CharSequence getLabel();
    }

    public AppWidgetLoader(Context context, AppWidgetManager appWidgetManager, ItemConstructor<Item> itemConstructor) {
        this.mContext = context;
        this.mAppWidgetManager = appWidgetManager;
        this.mItemConstructor = itemConstructor;
    }

    void putCustomAppWidgets(List<Item> items, Intent intent) {
        ArrayList<Bundle> customExtras = null;
        ArrayList<AppWidgetProviderInfo> customInfo = intent.getParcelableArrayListExtra("customInfo");
        if (customInfo == null || customInfo.size() == 0) {
            Log.i("AppWidgetAdapter", "EXTRA_CUSTOM_INFO not present.");
        } else {
            int customInfoSize = customInfo.size();
            for (int i = 0; i < customInfoSize; i++) {
                AppWidgetProviderInfo p = customInfo.get(i);
                if (p == null || !(p instanceof AppWidgetProviderInfo)) {
                    customInfo = null;
                    Log.e("AppWidgetAdapter", "error using EXTRA_CUSTOM_INFO index=" + i);
                    break;
                }
            }
            customExtras = intent.getParcelableArrayListExtra("customExtras");
            if (customExtras == null) {
                customInfo = null;
                Log.e("AppWidgetAdapter", "EXTRA_CUSTOM_INFO without EXTRA_CUSTOM_EXTRAS");
            } else {
                int customExtrasSize = customExtras.size();
                if (customInfoSize != customExtrasSize) {
                    customInfo = null;
                    customExtras = null;
                    Log.e("AppWidgetAdapter", "list size mismatch: EXTRA_CUSTOM_INFO: " + customInfoSize + " EXTRA_CUSTOM_EXTRAS: " + customExtrasSize);
                } else {
                    for (int i2 = 0; i2 < customExtrasSize; i2++) {
                        Bundle p2 = customExtras.get(i2);
                        if (p2 == null || !(p2 instanceof Bundle)) {
                            customInfo = null;
                            customExtras = null;
                            Log.e("AppWidgetAdapter", "error using EXTRA_CUSTOM_EXTRAS index=" + i2);
                            break;
                        }
                    }
                }
            }
        }
        putAppWidgetItems(customInfo, customExtras, items, 0, true);
    }

    void putAppWidgetItems(List<AppWidgetProviderInfo> appWidgets, List<Bundle> customExtras, List<Item> items, int categoryFilter, boolean ignoreFilter) {
        if (appWidgets == null) {
            return;
        }
        int size = appWidgets.size();
        for (int i = 0; i < size; i++) {
            AppWidgetProviderInfo info = appWidgets.get(i);
            if (ignoreFilter || (info.widgetCategory & categoryFilter) != 0) {
                Item item = this.mItemConstructor.createItem(this.mContext, info, customExtras != null ? customExtras.get(i) : null);
                items.add(item);
            }
        }
    }

    protected List<Item> getItems(Intent intent) {
        boolean sortCustomAppWidgets = intent.getBooleanExtra("customSort", true);
        List<Item> items = new ArrayList<>();
        int categoryFilter = intent.getIntExtra("categoryFilter", 1);
        putInstalledAppWidgets(items, categoryFilter);
        if (sortCustomAppWidgets) {
            putCustomAppWidgets(items, intent);
        }
        Collections.sort(items, new Comparator<Item>() {
            Collator mCollator = Collator.getInstance();

            @Override
            public int compare(Item lhs, Item rhs) {
                return this.mCollator.compare(lhs.getLabel(), rhs.getLabel());
            }
        });
        if (!sortCustomAppWidgets) {
            List<Item> customItems = new ArrayList<>();
            putCustomAppWidgets(customItems, intent);
            items.addAll(customItems);
        }
        return items;
    }

    void putInstalledAppWidgets(List<Item> items, int categoryFilter) {
        List installed = this.mAppWidgetManager.getInstalledProviders(categoryFilter);
        putAppWidgetItems(installed, null, items, categoryFilter, false);
    }
}
