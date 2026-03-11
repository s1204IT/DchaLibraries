package com.android.launcher3.widget;

import android.appwidget.AppWidgetHostView;
import android.content.Context;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import com.android.launcher3.AppWidgetResizeFrame;
import com.android.launcher3.DropTarget;
import com.android.launcher3.Launcher;
import com.android.launcher3.LauncherAppWidgetProviderInfo;
import com.android.launcher3.compat.AppWidgetManagerCompat;
import com.android.launcher3.dragndrop.DragController;
import com.android.launcher3.dragndrop.DragOptions;
import com.android.launcher3.views.BaseDragLayer;

public class WidgetHostViewLoader implements DragController.DragListener {
    private static final boolean LOGD = false;
    private static final String TAG = "WidgetHostViewLoader";
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
    public void onDragStart(DropTarget.DragObject dragObject, DragOptions dragOptions) {
        preloadWidget();
    }

    @Override
    public void onDragEnd() {
        this.mLauncher.getDragController().removeDragListener(this);
        this.mHandler.removeCallbacks(this.mBindWidgetRunnable);
        this.mHandler.removeCallbacks(this.mInflateWidgetRunnable);
        if (this.mWidgetLoadingId != -1) {
            this.mLauncher.getAppWidgetHost().deleteAppWidgetId(this.mWidgetLoadingId);
            this.mWidgetLoadingId = -1;
        }
        if (this.mInfo.boundWidget != null) {
            this.mLauncher.getDragLayer().removeView(this.mInfo.boundWidget);
            this.mLauncher.getAppWidgetHost().deleteAppWidgetId(this.mInfo.boundWidget.getAppWidgetId());
            this.mInfo.boundWidget = null;
        }
    }

    private boolean preloadWidget() {
        final LauncherAppWidgetProviderInfo launcherAppWidgetProviderInfo = this.mInfo.info;
        if (launcherAppWidgetProviderInfo.isCustomWidget()) {
            return false;
        }
        final Bundle defaultOptionsForWidget = getDefaultOptionsForWidget(this.mLauncher, this.mInfo);
        if (this.mInfo.getHandler().needsConfigure()) {
            this.mInfo.bindOptions = defaultOptionsForWidget;
            return false;
        }
        this.mBindWidgetRunnable = new Runnable() {
            @Override
            public void run() {
                WidgetHostViewLoader.this.mWidgetLoadingId = WidgetHostViewLoader.this.mLauncher.getAppWidgetHost().allocateAppWidgetId();
                if (AppWidgetManagerCompat.getInstance(WidgetHostViewLoader.this.mLauncher).bindAppWidgetIdIfAllowed(WidgetHostViewLoader.this.mWidgetLoadingId, launcherAppWidgetProviderInfo, defaultOptionsForWidget)) {
                    WidgetHostViewLoader.this.mHandler.post(WidgetHostViewLoader.this.mInflateWidgetRunnable);
                }
            }
        };
        this.mInflateWidgetRunnable = new Runnable() {
            @Override
            public void run() {
                if (WidgetHostViewLoader.this.mWidgetLoadingId == -1) {
                    return;
                }
                AppWidgetHostView appWidgetHostViewCreateView = WidgetHostViewLoader.this.mLauncher.getAppWidgetHost().createView((Context) WidgetHostViewLoader.this.mLauncher, WidgetHostViewLoader.this.mWidgetLoadingId, launcherAppWidgetProviderInfo);
                WidgetHostViewLoader.this.mInfo.boundWidget = appWidgetHostViewCreateView;
                WidgetHostViewLoader.this.mWidgetLoadingId = -1;
                appWidgetHostViewCreateView.setVisibility(4);
                int[] iArrEstimateItemSize = WidgetHostViewLoader.this.mLauncher.getWorkspace().estimateItemSize(WidgetHostViewLoader.this.mInfo);
                BaseDragLayer.LayoutParams layoutParams = new BaseDragLayer.LayoutParams(iArrEstimateItemSize[0], iArrEstimateItemSize[1]);
                layoutParams.y = 0;
                layoutParams.x = 0;
                layoutParams.customPosition = true;
                appWidgetHostViewCreateView.setLayoutParams(layoutParams);
                WidgetHostViewLoader.this.mLauncher.getDragLayer().addView(appWidgetHostViewCreateView);
                WidgetHostViewLoader.this.mView.setTag(WidgetHostViewLoader.this.mInfo);
            }
        };
        this.mHandler.post(this.mBindWidgetRunnable);
        return true;
    }

    public static Bundle getDefaultOptionsForWidget(Context context, PendingAddWidgetInfo pendingAddWidgetInfo) {
        Rect rect = new Rect();
        AppWidgetResizeFrame.getWidgetSizeRanges(context, pendingAddWidgetInfo.spanX, pendingAddWidgetInfo.spanY, rect);
        Rect defaultPaddingForWidget = AppWidgetHostView.getDefaultPaddingForWidget(context, pendingAddWidgetInfo.componentName, null);
        float f = context.getResources().getDisplayMetrics().density;
        int i = (int) ((defaultPaddingForWidget.left + defaultPaddingForWidget.right) / f);
        int i2 = (int) ((defaultPaddingForWidget.top + defaultPaddingForWidget.bottom) / f);
        Bundle bundle = new Bundle();
        bundle.putInt("appWidgetMinWidth", rect.left - i);
        bundle.putInt("appWidgetMinHeight", rect.top - i2);
        bundle.putInt("appWidgetMaxWidth", rect.right - i);
        bundle.putInt("appWidgetMaxHeight", rect.bottom - i2);
        return bundle;
    }
}
