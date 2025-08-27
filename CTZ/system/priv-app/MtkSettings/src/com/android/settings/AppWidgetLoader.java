package com.android.settings;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProviderInfo;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.Log;
import com.android.settings.AppWidgetLoader.LabelledItem;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/* loaded from: classes.dex */
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

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [106=4] */
    void putCustomAppWidgets(List<Item> list, Intent intent) {
        ArrayList arrayList;
        ArrayList arrayList2;
        ArrayList parcelableArrayListExtra = intent.getParcelableArrayListExtra("customInfo");
        if (parcelableArrayListExtra == null || parcelableArrayListExtra.size() == 0) {
            Log.i("AppWidgetAdapter", "EXTRA_CUSTOM_INFO not present.");
            arrayList = parcelableArrayListExtra;
            arrayList2 = null;
        } else {
            int size = parcelableArrayListExtra.size();
            for (int i = 0; i < size; i++) {
                Parcelable parcelable = (Parcelable) parcelableArrayListExtra.get(i);
                if (parcelable == null || !(parcelable instanceof AppWidgetProviderInfo)) {
                    Log.e("AppWidgetAdapter", "error using EXTRA_CUSTOM_INFO index=" + i);
                    break;
                }
            }
            ArrayList parcelableArrayListExtra2 = intent.getParcelableArrayListExtra("customExtras");
            if (parcelableArrayListExtra2 == null) {
                Log.e("AppWidgetAdapter", "EXTRA_CUSTOM_INFO without EXTRA_CUSTOM_EXTRAS");
                arrayList2 = parcelableArrayListExtra2;
                arrayList = null;
            } else {
                int size2 = parcelableArrayListExtra2.size();
                if (size != size2) {
                    Log.e("AppWidgetAdapter", "list size mismatch: EXTRA_CUSTOM_INFO: " + size + " EXTRA_CUSTOM_EXTRAS: " + size2);
                } else {
                    for (int i2 = 0; i2 < size2; i2++) {
                        Parcelable parcelable2 = (Parcelable) parcelableArrayListExtra2.get(i2);
                        if (parcelable2 == null || !(parcelable2 instanceof Bundle)) {
                            Log.e("AppWidgetAdapter", "error using EXTRA_CUSTOM_EXTRAS index=" + i2);
                        }
                    }
                    arrayList2 = parcelableArrayListExtra2;
                    arrayList = parcelableArrayListExtra;
                }
                arrayList = null;
                arrayList2 = null;
            }
        }
        putAppWidgetItems(arrayList, arrayList2, list, 0, true);
    }

    void putAppWidgetItems(List<AppWidgetProviderInfo> list, List<Bundle> list2, List<Item> list3, int i, boolean z) {
        if (list == null) {
            return;
        }
        int size = list.size();
        for (int i2 = 0; i2 < size; i2++) {
            AppWidgetProviderInfo appWidgetProviderInfo = list.get(i2);
            if (z || (appWidgetProviderInfo.widgetCategory & i) != 0) {
                list3.add(this.mItemConstructor.createItem(this.mContext, appWidgetProviderInfo, list2 != null ? list2.get(i2) : null));
            }
        }
    }

    protected List<Item> getItems(Intent intent) {
        boolean booleanExtra = intent.getBooleanExtra("customSort", true);
        ArrayList arrayList = new ArrayList();
        putInstalledAppWidgets(arrayList, intent.getIntExtra("categoryFilter", 1));
        if (booleanExtra) {
            putCustomAppWidgets(arrayList, intent);
        }
        Collections.sort(arrayList, new Comparator<Item>() { // from class: com.android.settings.AppWidgetLoader.1
            Collator mCollator = Collator.getInstance();

            /* JADX DEBUG: Method merged with bridge method: compare(Ljava/lang/Object;Ljava/lang/Object;)I */
            @Override // java.util.Comparator
            public int compare(Item item, Item item2) {
                return this.mCollator.compare(item.getLabel(), item2.getLabel());
            }
        });
        if (!booleanExtra) {
            ArrayList arrayList2 = new ArrayList();
            putCustomAppWidgets(arrayList2, intent);
            arrayList.addAll(arrayList2);
        }
        return arrayList;
    }

    void putInstalledAppWidgets(List<Item> list, int i) {
        putAppWidgetItems(this.mAppWidgetManager.getInstalledProviders(i), null, list, i, false);
    }
}
