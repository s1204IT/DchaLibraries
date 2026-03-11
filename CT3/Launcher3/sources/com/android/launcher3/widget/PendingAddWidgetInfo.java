package com.android.launcher3.widget;

import android.appwidget.AppWidgetHostView;
import android.os.Bundle;
import android.os.Parcelable;
import com.android.launcher3.Launcher;
import com.android.launcher3.LauncherAppWidgetProviderInfo;
import com.android.launcher3.PendingAddItemInfo;
import com.android.launcher3.compat.AppWidgetManagerCompat;

public class PendingAddWidgetInfo extends PendingAddItemInfo {
    public Bundle bindOptions = null;
    public AppWidgetHostView boundWidget;
    public int icon;
    public LauncherAppWidgetProviderInfo info;
    public int previewImage;

    public PendingAddWidgetInfo(Launcher launcher, LauncherAppWidgetProviderInfo i, Parcelable data) {
        if (i.isCustomWidget) {
            this.itemType = 5;
        } else {
            this.itemType = 4;
        }
        this.info = i;
        this.user = AppWidgetManagerCompat.getInstance(launcher).getUser(i);
        this.componentName = i.provider;
        this.previewImage = i.previewImage;
        this.icon = i.icon;
        this.spanX = i.spanX;
        this.spanY = i.spanY;
        this.minSpanX = i.minSpanX;
        this.minSpanY = i.minSpanY;
    }

    @Override
    public String toString() {
        return String.format("PendingAddWidgetInfo package=%s, name=%s", this.componentName.getPackageName(), this.componentName.getShortClassName());
    }
}
