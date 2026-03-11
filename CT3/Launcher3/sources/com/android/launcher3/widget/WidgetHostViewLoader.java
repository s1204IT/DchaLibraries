package com.android.launcher3.widget;

import android.appwidget.AppWidgetHostView;
import android.content.Context;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import com.android.launcher3.AppWidgetResizeFrame;
import com.android.launcher3.DragController;
import com.android.launcher3.DragLayer;
import com.android.launcher3.DragSource;
import com.android.launcher3.Launcher;
import com.android.launcher3.LauncherAppWidgetProviderInfo;
import com.android.launcher3.Utilities;
import com.android.launcher3.compat.AppWidgetManagerCompat;

public class WidgetHostViewLoader implements DragController.DragListener {
    final PendingAddWidgetInfo mInfo;
    Launcher mLauncher;
    final View mView;
    Runnable mInflateWidgetRunnable = null;
    private Runnable mBindWidgetRunnable = null;
    int mWidgetLoadingId = -1;
    Handler mHandler = new Handler();

    public WidgetHostViewLoader(Launcher launcher, View view) {
        this.mLauncher = launcher;
        this.mView = view;
        this.mInfo = (PendingAddWidgetInfo) view.getTag();
    }

    @Override
    public void onDragStart(DragSource source, Object info, int dragAction) {
    }

    @Override
    public void onDragEnd() {
        if (this.mLauncher.getDragController() != null) {
            this.mLauncher.getDragController().removeDragListener(this);
        }
        this.mHandler.removeCallbacks(this.mBindWidgetRunnable);
        this.mHandler.removeCallbacks(this.mInflateWidgetRunnable);
        if (this.mWidgetLoadingId != -1) {
            this.mLauncher.getAppWidgetHost().deleteAppWidgetId(this.mWidgetLoadingId);
            this.mWidgetLoadingId = -1;
        }
        if (this.mInfo.boundWidget == null) {
            return;
        }
        this.mLauncher.getDragLayer().removeView(this.mInfo.boundWidget);
        this.mLauncher.getAppWidgetHost().deleteAppWidgetId(this.mInfo.boundWidget.getAppWidgetId());
        this.mInfo.boundWidget = null;
    }

    public boolean preloadWidget() {
        final LauncherAppWidgetProviderInfo pInfo = this.mInfo.info;
        if (pInfo.isCustomWidget) {
            return false;
        }
        final Bundle options = getDefaultOptionsForWidget(this.mLauncher, this.mInfo);
        if (pInfo.configure != null) {
            this.mInfo.bindOptions = options;
            return false;
        }
        this.mBindWidgetRunnable = new Runnable() {
            @Override
            public void run() {
                WidgetHostViewLoader.this.mWidgetLoadingId = WidgetHostViewLoader.this.mLauncher.getAppWidgetHost().allocateAppWidgetId();
                if (!AppWidgetManagerCompat.getInstance(WidgetHostViewLoader.this.mLauncher).bindAppWidgetIdIfAllowed(WidgetHostViewLoader.this.mWidgetLoadingId, pInfo, options)) {
                    return;
                }
                WidgetHostViewLoader.this.mHandler.post(WidgetHostViewLoader.this.mInflateWidgetRunnable);
            }
        };
        this.mInflateWidgetRunnable = new Runnable() {
            @Override
            public void run() {
                if (WidgetHostViewLoader.this.mWidgetLoadingId == -1) {
                    return;
                }
                AppWidgetHostView hostView = WidgetHostViewLoader.this.mLauncher.getAppWidgetHost().createView((Context) WidgetHostViewLoader.this.mLauncher, WidgetHostViewLoader.this.mWidgetLoadingId, pInfo);
                WidgetHostViewLoader.this.mInfo.boundWidget = hostView;
                WidgetHostViewLoader.this.mWidgetLoadingId = -1;
                hostView.setVisibility(4);
                int[] unScaledSize = WidgetHostViewLoader.this.mLauncher.getWorkspace().estimateItemSize(WidgetHostViewLoader.this.mInfo, false);
                DragLayer.LayoutParams lp = new DragLayer.LayoutParams(unScaledSize[0], unScaledSize[1]);
                lp.y = 0;
                lp.x = 0;
                lp.customPosition = true;
                hostView.setLayoutParams(lp);
                WidgetHostViewLoader.this.mLauncher.getDragLayer().addView(hostView);
                WidgetHostViewLoader.this.mView.setTag(WidgetHostViewLoader.this.mInfo);
            }
        };
        this.mHandler.post(this.mBindWidgetRunnable);
        return true;
    }

    public static Bundle getDefaultOptionsForWidget(Launcher launcher, PendingAddWidgetInfo info) {
        Rect rect = new Rect();
        if (!Utilities.ATLEAST_JB_MR1) {
            return null;
        }
        AppWidgetResizeFrame.getWidgetSizeRanges(launcher, info.spanX, info.spanY, rect);
        Rect padding = AppWidgetHostView.getDefaultPaddingForWidget(launcher, info.componentName, null);
        float density = launcher.getResources().getDisplayMetrics().density;
        int xPaddingDips = (int) ((padding.left + padding.right) / density);
        int yPaddingDips = (int) ((padding.top + padding.bottom) / density);
        Bundle options = new Bundle();
        options.putInt("appWidgetMinWidth", rect.left - xPaddingDips);
        options.putInt("appWidgetMinHeight", rect.top - yPaddingDips);
        options.putInt("appWidgetMaxWidth", rect.right - xPaddingDips);
        options.putInt("appWidgetMaxHeight", rect.bottom - yPaddingDips);
        return options;
    }
}
