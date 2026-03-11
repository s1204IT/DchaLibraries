package com.android.launcher3;

import android.appwidget.AppWidgetHostView;
import android.content.ComponentName;
import android.content.Intent;
import android.os.Process;
import com.android.launcher3.LauncherSettings;
import com.android.launcher3.model.PackageItemInfo;
import com.android.launcher3.util.ContentWriter;

public class LauncherAppWidgetInfo extends ItemInfo {
    public static final int CUSTOM_WIDGET_ID = -100;
    public static final int FLAG_DIRECT_CONFIG = 32;
    public static final int FLAG_ID_ALLOCATED = 16;
    public static final int FLAG_ID_NOT_VALID = 1;
    public static final int FLAG_PROVIDER_NOT_READY = 2;
    public static final int FLAG_RESTORE_STARTED = 8;
    public static final int FLAG_UI_NOT_READY = 4;
    public static final int NO_ID = -1;
    public static final int RESTORE_COMPLETED = 0;
    public int appWidgetId;
    public Intent bindOptions;
    public int installProgress;
    private boolean mHasNotifiedInitialWidgetSizeChanged;
    public PackageItemInfo pendingItemInfo;
    public ComponentName providerName;
    public int restoreStatus;

    public LauncherAppWidgetInfo(int i, ComponentName componentName) {
        this.appWidgetId = -1;
        this.installProgress = -1;
        this.appWidgetId = i;
        this.providerName = componentName;
        if (isCustomWidget()) {
            this.itemType = 5;
        } else {
            this.itemType = 4;
        }
        this.spanX = -1;
        this.spanY = -1;
        this.user = Process.myUserHandle();
        this.restoreStatus = 0;
    }

    public LauncherAppWidgetInfo() {
        this.appWidgetId = -1;
        this.installProgress = -1;
        this.itemType = 4;
    }

    public boolean isCustomWidget() {
        return this.appWidgetId <= -100;
    }

    @Override
    public void onAddToDatabase(ContentWriter contentWriter) {
        super.onAddToDatabase(contentWriter);
        contentWriter.put(LauncherSettings.Favorites.APPWIDGET_ID, Integer.valueOf(this.appWidgetId)).put(LauncherSettings.Favorites.APPWIDGET_PROVIDER, this.providerName.flattenToString()).put(LauncherSettings.Favorites.RESTORED, Integer.valueOf(this.restoreStatus)).put(LauncherSettings.BaseLauncherColumns.INTENT, this.bindOptions);
    }

    void onBindAppWidget(Launcher launcher, AppWidgetHostView appWidgetHostView) {
        if (!this.mHasNotifiedInitialWidgetSizeChanged) {
            AppWidgetResizeFrame.updateWidgetSizeRanges(appWidgetHostView, launcher, this.spanX, this.spanY);
            this.mHasNotifiedInitialWidgetSizeChanged = true;
        }
    }

    @Override
    protected String dumpProperties() {
        return super.dumpProperties() + " appWidgetId=" + this.appWidgetId;
    }

    public final boolean isWidgetIdAllocated() {
        return (this.restoreStatus & 1) == 0 || (this.restoreStatus & 16) == 16;
    }

    public final boolean hasRestoreFlag(int i) {
        return (this.restoreStatus & i) == i;
    }
}
