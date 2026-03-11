package com.android.launcher3.widget;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import com.android.launcher3.BubbleTextView;
import com.android.launcher3.DeviceProfile;
import com.android.launcher3.Launcher;
import com.android.launcher3.LauncherAppState;
import com.android.launcher3.LauncherAppWidgetProviderInfo;
import com.android.launcher3.R;
import com.android.launcher3.Utilities;
import com.android.launcher3.WidgetPreviewLoader;
import com.android.launcher3.model.PackageItemInfo;
import com.android.launcher3.model.WidgetsModel;
import java.util.List;

public class WidgetsListAdapter extends RecyclerView.Adapter<WidgetsRowViewHolder> {
    private View.OnClickListener mIconClickListener;
    private View.OnLongClickListener mIconLongClickListener;
    private int mIndent = 0;
    private Launcher mLauncher;
    private LayoutInflater mLayoutInflater;
    private WidgetPreviewLoader mWidgetPreviewLoader;
    private WidgetsModel mWidgetsModel;

    public WidgetsListAdapter(Context context, View.OnClickListener iconClickListener, View.OnLongClickListener iconLongClickListener, Launcher launcher) {
        this.mLayoutInflater = LayoutInflater.from(context);
        this.mIconClickListener = iconClickListener;
        this.mIconLongClickListener = iconLongClickListener;
        this.mLauncher = launcher;
        setContainerHeight();
    }

    public void setWidgetsModel(WidgetsModel w) {
        this.mWidgetsModel = w;
    }

    @Override
    public int getItemCount() {
        if (this.mWidgetsModel == null) {
            return 0;
        }
        return this.mWidgetsModel.getPackageSize();
    }

    @Override
    public void onBindViewHolder(WidgetsRowViewHolder holder, int pos) {
        List<Object> infoList = this.mWidgetsModel.getSortedWidgets(pos);
        ViewGroup row = (ViewGroup) holder.getContent().findViewById(R.id.widgets_cell_list);
        int diff = infoList.size() - row.getChildCount();
        if (diff > 0) {
            for (int i = 0; i < diff; i++) {
                WidgetCell widget = (WidgetCell) this.mLayoutInflater.inflate(R.layout.widget_cell, row, false);
                widget.setOnClickListener(this.mIconClickListener);
                widget.setOnLongClickListener(this.mIconLongClickListener);
                ViewGroup.LayoutParams lp = widget.getLayoutParams();
                lp.height = widget.cellSize;
                lp.width = widget.cellSize;
                widget.setLayoutParams(lp);
                row.addView(widget);
            }
        } else if (diff < 0) {
            for (int i2 = infoList.size(); i2 < row.getChildCount(); i2++) {
                row.getChildAt(i2).setVisibility(8);
            }
        }
        PackageItemInfo infoOut = this.mWidgetsModel.getPackageItemInfo(pos);
        BubbleTextView tv = (BubbleTextView) holder.getContent().findViewById(R.id.section);
        tv.applyFromPackageItemInfo(infoOut);
        if (getWidgetPreviewLoader() == null) {
            return;
        }
        for (int i3 = 0; i3 < infoList.size(); i3++) {
            WidgetCell widget2 = (WidgetCell) row.getChildAt(i3);
            if (infoList.get(i3) instanceof LauncherAppWidgetProviderInfo) {
                LauncherAppWidgetProviderInfo info = (LauncherAppWidgetProviderInfo) infoList.get(i3);
                PendingAddWidgetInfo pawi = new PendingAddWidgetInfo(this.mLauncher, info, null);
                widget2.setTag(pawi);
                widget2.applyFromAppWidgetProviderInfo(info, this.mWidgetPreviewLoader);
            } else if (infoList.get(i3) instanceof ResolveInfo) {
                ResolveInfo info2 = (ResolveInfo) infoList.get(i3);
                PendingAddShortcutInfo pasi = new PendingAddShortcutInfo(info2.activityInfo);
                widget2.setTag(pasi);
                widget2.applyFromResolveInfo(this.mLauncher.getPackageManager(), info2, this.mWidgetPreviewLoader);
            }
            widget2.ensurePreview();
            widget2.setVisibility(0);
        }
    }

    @Override
    @TargetApi(17)
    public WidgetsRowViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        ViewGroup container = (ViewGroup) this.mLayoutInflater.inflate(R.layout.widgets_list_row_view, parent, false);
        LinearLayout cellList = (LinearLayout) container.findViewById(R.id.widgets_cell_list);
        if (Utilities.ATLEAST_JB_MR1) {
            cellList.setPaddingRelative(this.mIndent, 0, 1, 0);
        } else {
            cellList.setPadding(this.mIndent, 0, 1, 0);
        }
        return new WidgetsRowViewHolder(container);
    }

    @Override
    public void onViewRecycled(WidgetsRowViewHolder holder) {
        ViewGroup row = (ViewGroup) holder.getContent().findViewById(R.id.widgets_cell_list);
        for (int i = 0; i < row.getChildCount(); i++) {
            WidgetCell widget = (WidgetCell) row.getChildAt(i);
            widget.clear();
        }
    }

    @Override
    public boolean onFailedToRecycleView(WidgetsRowViewHolder holder) {
        return true;
    }

    @Override
    public long getItemId(int pos) {
        return pos;
    }

    private WidgetPreviewLoader getWidgetPreviewLoader() {
        if (this.mWidgetPreviewLoader == null) {
            this.mWidgetPreviewLoader = LauncherAppState.getInstance().getWidgetCache();
        }
        return this.mWidgetPreviewLoader;
    }

    private void setContainerHeight() {
        Resources r = this.mLauncher.getResources();
        DeviceProfile profile = this.mLauncher.getDeviceProfile();
        if (!profile.isLargeTablet && !profile.isTablet) {
            return;
        }
        this.mIndent = Utilities.pxFromDp(56.0f, r.getDisplayMetrics());
    }
}
