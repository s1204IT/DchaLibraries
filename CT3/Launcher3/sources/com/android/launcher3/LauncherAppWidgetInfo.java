package com.android.launcher3;

import android.appwidget.AppWidgetHostView;
import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Context;
import com.android.launcher3.compat.UserHandleCompat;

public class LauncherAppWidgetInfo extends ItemInfo {
    int appWidgetId;
    private boolean mHasNotifiedInitialWidgetSizeChanged;
    ComponentName providerName;
    int restoreStatus;
    int installProgress = -1;
    AppWidgetHostView hostView = null;

    LauncherAppWidgetInfo(int appWidgetId, ComponentName providerName) {
        this.appWidgetId = -1;
        if (appWidgetId == -100) {
            this.itemType = 5;
        } else {
            this.itemType = 4;
        }
        this.appWidgetId = appWidgetId;
        this.providerName = providerName;
        this.spanX = -1;
        this.spanY = -1;
        this.user = UserHandleCompat.myUserHandle();
        this.restoreStatus = 0;
    }

    public boolean isCustomWidget() {
        return this.appWidgetId == -100;
    }

    @Override
    void onAddToDatabase(Context context, ContentValues values) {
        super.onAddToDatabase(context, values);
        values.put("appWidgetId", Integer.valueOf(this.appWidgetId));
        values.put("appWidgetProvider", this.providerName.flattenToString());
        values.put("restored", Integer.valueOf(this.restoreStatus));
    }

    void onBindAppWidget(Launcher launcher) {
        if (this.mHasNotifiedInitialWidgetSizeChanged) {
            return;
        }
        AppWidgetResizeFrame.updateWidgetSizeRanges(this.hostView, launcher, this.spanX, this.spanY);
        this.mHasNotifiedInitialWidgetSizeChanged = true;
    }

    @Override
    public String toString() {
        return "AppWidget(id=" + Integer.toString(this.appWidgetId) + " screenId=" + this.screenId + " cellX=" + this.cellX + " cellY=" + this.cellY + " spanX=" + this.spanX + " spanY=" + this.spanY + " providerName = " + this.providerName + ")";
    }

    @Override
    void unbind() {
        super.unbind();
        this.hostView = null;
    }

    public final boolean isWidgetIdValid() {
        return (this.restoreStatus & 1) == 0;
    }

    public final boolean hasRestoreFlag(int flag) {
        return (this.restoreStatus & flag) == flag;
    }
}
