package com.android.launcher3.widget;

import com.android.launcher3.model.PackageItemInfo;
import com.android.launcher3.model.WidgetItem;
import java.util.ArrayList;

/* loaded from: classes.dex */
public class WidgetListRowEntry {
    public final PackageItemInfo pkgItem;
    public String titleSectionName;
    public final ArrayList<WidgetItem> widgets;

    public WidgetListRowEntry(PackageItemInfo packageItemInfo, ArrayList<WidgetItem> arrayList) {
        this.pkgItem = packageItemInfo;
        this.widgets = arrayList;
    }

    public String toString() {
        return this.pkgItem.packageName + ":" + this.widgets.size();
    }
}
