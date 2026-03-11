package com.android.launcher2;

import android.appwidget.AppWidgetHostView;
import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Context;

class LauncherAppWidgetInfo extends ItemInfo {
    int appWidgetId;
    private boolean mHasNotifiedInitialWidgetSizeChanged;
    ComponentName providerName;
    int minWidth = -1;
    int minHeight = -1;
    AppWidgetHostView hostView = null;

    LauncherAppWidgetInfo(int appWidgetId, ComponentName providerName) {
        this.appWidgetId = -1;
        this.itemType = 4;
        this.appWidgetId = appWidgetId;
        this.providerName = providerName;
        this.spanX = -1;
        this.spanY = -1;
    }

    @Override
    void onAddToDatabase(Context context, ContentValues values) {
        super.onAddToDatabase(context, values);
        values.put("appWidgetId", Integer.valueOf(this.appWidgetId));
    }

    void onBindAppWidget(Launcher launcher) {
        if (!this.mHasNotifiedInitialWidgetSizeChanged) {
            notifyWidgetSizeChanged(launcher);
        }
    }

    void notifyWidgetSizeChanged(Launcher launcher) {
        AppWidgetResizeFrame.updateWidgetSizeRanges(this.hostView, launcher, this.spanX, this.spanY);
        this.mHasNotifiedInitialWidgetSizeChanged = true;
    }

    @Override
    public String toString() {
        return "AppWidget(id=" + Integer.toString(this.appWidgetId) + ")";
    }

    @Override
    void unbind() {
        super.unbind();
        this.hostView = null;
    }
}
