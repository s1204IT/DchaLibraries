package com.android.launcher3.widget;

import android.support.v7.widget.RecyclerView;
import com.android.launcher3.IconCache;
import com.android.launcher3.model.PackageItemInfo;
import com.android.launcher3.widget.WidgetsListAdapter;
import java.util.ArrayList;
import java.util.Iterator;

public class WidgetsDiffReporter {
    private static final boolean DEBUG = false;
    private static final String TAG = "WidgetsDiffReporter";
    private final IconCache mIconCache;
    private final RecyclerView.Adapter mListener;

    public WidgetsDiffReporter(IconCache iconCache, RecyclerView.Adapter adapter) {
        this.mIconCache = iconCache;
        this.mListener = adapter;
    }

    public void process(ArrayList<WidgetListRowEntry> arrayList, ArrayList<WidgetListRowEntry> arrayList2, WidgetsListAdapter.WidgetListRowEntryComparator widgetListRowEntryComparator) {
        if (arrayList.isEmpty() || arrayList2.isEmpty()) {
            if (arrayList.size() != arrayList2.size()) {
                arrayList.clear();
                arrayList.addAll(arrayList2);
                this.mListener.notifyDataSetChanged();
                return;
            }
            return;
        }
        Iterator it = ((ArrayList) arrayList.clone()).iterator();
        Iterator<WidgetListRowEntry> it2 = arrayList2.iterator();
        WidgetListRowEntry widgetListRowEntry = (WidgetListRowEntry) it.next();
        WidgetListRowEntry next = it2.next();
        while (true) {
            int iComparePackageName = comparePackageName(widgetListRowEntry, next, widgetListRowEntryComparator);
            if (iComparePackageName < 0) {
                int iIndexOf = arrayList.indexOf(widgetListRowEntry);
                this.mListener.notifyItemRemoved(iIndexOf);
                arrayList.remove(iIndexOf);
                widgetListRowEntry = it.hasNext() ? (WidgetListRowEntry) it.next() : null;
            } else {
                if (iComparePackageName > 0) {
                    int iIndexOf2 = widgetListRowEntry != null ? arrayList.indexOf(widgetListRowEntry) : arrayList.size();
                    arrayList.add(iIndexOf2, next);
                    next = it2.hasNext() ? it2.next() : null;
                    this.mListener.notifyItemInserted(iIndexOf2);
                } else {
                    if (!isSamePackageItemInfo(widgetListRowEntry.pkgItem, next.pkgItem) || !widgetListRowEntry.widgets.equals(next.widgets)) {
                        int iIndexOf3 = arrayList.indexOf(widgetListRowEntry);
                        arrayList.set(iIndexOf3, next);
                        this.mListener.notifyItemChanged(iIndexOf3);
                    }
                    widgetListRowEntry = it.hasNext() ? (WidgetListRowEntry) it.next() : null;
                    if (it2.hasNext()) {
                        next = it2.next();
                    }
                }
                next = next;
            }
            if (widgetListRowEntry == null && next == null) {
                return;
            }
        }
    }

    private int comparePackageName(WidgetListRowEntry widgetListRowEntry, WidgetListRowEntry widgetListRowEntry2, WidgetsListAdapter.WidgetListRowEntryComparator widgetListRowEntryComparator) {
        if (widgetListRowEntry == null && widgetListRowEntry2 == null) {
            throw new IllegalStateException("Cannot compare PackageItemInfo if both rows are null.");
        }
        if (widgetListRowEntry == null && widgetListRowEntry2 != null) {
            return 1;
        }
        if (widgetListRowEntry != null && widgetListRowEntry2 == null) {
            return -1;
        }
        return widgetListRowEntryComparator.compare(widgetListRowEntry, widgetListRowEntry2);
    }

    private boolean isSamePackageItemInfo(PackageItemInfo packageItemInfo, PackageItemInfo packageItemInfo2) {
        return packageItemInfo.iconBitmap.equals(packageItemInfo2.iconBitmap) && !this.mIconCache.isDefaultIcon(packageItemInfo.iconBitmap, packageItemInfo.user);
    }
}
