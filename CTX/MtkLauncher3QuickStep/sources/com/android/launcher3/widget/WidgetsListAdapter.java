package com.android.launcher3.widget;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import com.android.launcher3.IconCache;
import com.android.launcher3.R;
import com.android.launcher3.WidgetPreviewLoader;
import com.android.launcher3.model.WidgetItem;
import com.android.launcher3.util.LabelComparator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

public class WidgetsListAdapter extends RecyclerView.Adapter<WidgetsRowViewHolder> {
    private static final boolean DEBUG = false;
    private static final String TAG = "WidgetsListAdapter";
    private boolean mApplyBitmapDeferred;
    private final WidgetsDiffReporter mDiffReporter;
    private ArrayList<WidgetListRowEntry> mEntries = new ArrayList<>();
    private final View.OnClickListener mIconClickListener;
    private final View.OnLongClickListener mIconLongClickListener;
    private final int mIndent;
    private final LayoutInflater mLayoutInflater;
    private final WidgetPreviewLoader mWidgetPreviewLoader;

    public WidgetsListAdapter(Context context, LayoutInflater layoutInflater, WidgetPreviewLoader widgetPreviewLoader, IconCache iconCache, View.OnClickListener onClickListener, View.OnLongClickListener onLongClickListener) {
        this.mLayoutInflater = layoutInflater;
        this.mWidgetPreviewLoader = widgetPreviewLoader;
        this.mIconClickListener = onClickListener;
        this.mIconLongClickListener = onLongClickListener;
        this.mIndent = context.getResources().getDimensionPixelSize(R.dimen.widget_section_indent);
        this.mDiffReporter = new WidgetsDiffReporter(iconCache, this);
    }

    public void setApplyBitmapDeferred(boolean z, RecyclerView recyclerView) {
        this.mApplyBitmapDeferred = z;
        for (int childCount = recyclerView.getChildCount() - 1; childCount >= 0; childCount--) {
            WidgetsRowViewHolder widgetsRowViewHolder = (WidgetsRowViewHolder) recyclerView.getChildViewHolder(recyclerView.getChildAt(childCount));
            for (int childCount2 = widgetsRowViewHolder.cellContainer.getChildCount() - 1; childCount2 >= 0; childCount2--) {
                View childAt = widgetsRowViewHolder.cellContainer.getChildAt(childCount2);
                if (childAt instanceof WidgetCell) {
                    ((WidgetCell) childAt).setApplyBitmapDeferred(this.mApplyBitmapDeferred);
                }
            }
        }
    }

    public void setWidgets(ArrayList<WidgetListRowEntry> arrayList) {
        WidgetListRowEntryComparator widgetListRowEntryComparator = new WidgetListRowEntryComparator();
        Collections.sort(arrayList, widgetListRowEntryComparator);
        this.mDiffReporter.process(this.mEntries, arrayList, widgetListRowEntryComparator);
    }

    @Override
    public int getItemCount() {
        return this.mEntries.size();
    }

    public String getSectionName(int i) {
        return this.mEntries.get(i).titleSectionName;
    }

    @Override
    public void onBindViewHolder(WidgetsRowViewHolder widgetsRowViewHolder, int i) {
        WidgetListRowEntry widgetListRowEntry = this.mEntries.get(i);
        ArrayList<WidgetItem> arrayList = widgetListRowEntry.widgets;
        ViewGroup viewGroup = widgetsRowViewHolder.cellContainer;
        int size = arrayList.size() + Math.max(0, arrayList.size() - 1);
        int childCount = viewGroup.getChildCount();
        if (size > childCount) {
            while (childCount < size) {
                if ((childCount & 1) == 1) {
                    this.mLayoutInflater.inflate(R.layout.widget_list_divider, viewGroup);
                } else {
                    WidgetCell widgetCell = (WidgetCell) this.mLayoutInflater.inflate(R.layout.widget_cell, viewGroup, false);
                    widgetCell.setOnClickListener(this.mIconClickListener);
                    widgetCell.setOnLongClickListener(this.mIconLongClickListener);
                    viewGroup.addView(widgetCell);
                }
                childCount++;
            }
        } else if (size < childCount) {
            while (size < childCount) {
                viewGroup.getChildAt(size).setVisibility(8);
                size++;
            }
        }
        widgetsRowViewHolder.title.applyFromPackageItemInfo(widgetListRowEntry.pkgItem);
        for (int i2 = 0; i2 < arrayList.size(); i2++) {
            int i3 = 2 * i2;
            WidgetCell widgetCell2 = (WidgetCell) viewGroup.getChildAt(i3);
            widgetCell2.applyFromCellItem(arrayList.get(i2), this.mWidgetPreviewLoader);
            widgetCell2.setApplyBitmapDeferred(this.mApplyBitmapDeferred);
            widgetCell2.ensurePreview();
            widgetCell2.setVisibility(0);
            if (i2 > 0) {
                viewGroup.getChildAt(i3 - 1).setVisibility(0);
            }
        }
    }

    @Override
    public WidgetsRowViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
        ViewGroup viewGroup2 = (ViewGroup) this.mLayoutInflater.inflate(R.layout.widgets_list_row_view, viewGroup, false);
        viewGroup2.findViewById(R.id.widgets_cell_list).setPaddingRelative(this.mIndent, 0, 1, 0);
        return new WidgetsRowViewHolder(viewGroup2);
    }

    @Override
    public void onViewRecycled(WidgetsRowViewHolder widgetsRowViewHolder) {
        int childCount = widgetsRowViewHolder.cellContainer.getChildCount();
        for (int i = 0; i < childCount; i += 2) {
            ((WidgetCell) widgetsRowViewHolder.cellContainer.getChildAt(i)).clear();
        }
    }

    @Override
    public boolean onFailedToRecycleView(WidgetsRowViewHolder widgetsRowViewHolder) {
        return true;
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    public static class WidgetListRowEntryComparator implements Comparator<WidgetListRowEntry> {
        private final LabelComparator mComparator = new LabelComparator();

        @Override
        public int compare(WidgetListRowEntry widgetListRowEntry, WidgetListRowEntry widgetListRowEntry2) {
            return this.mComparator.compare(widgetListRowEntry.pkgItem.title.toString(), widgetListRowEntry2.pkgItem.title.toString());
        }
    }
}
