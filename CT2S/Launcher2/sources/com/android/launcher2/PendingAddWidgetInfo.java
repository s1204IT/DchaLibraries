package com.android.launcher2;

import android.appwidget.AppWidgetHostView;
import android.appwidget.AppWidgetProviderInfo;
import android.os.Bundle;
import android.os.Parcelable;

class PendingAddWidgetInfo extends PendingAddItemInfo {
    Bundle bindOptions;
    AppWidgetHostView boundWidget;
    Parcelable configurationData;
    AppWidgetProviderInfo info;
    String mimeType;

    public PendingAddWidgetInfo(AppWidgetProviderInfo i, String dataMimeType, Parcelable data) {
        this.bindOptions = null;
        this.itemType = 4;
        this.info = i;
        this.componentName = i.provider;
        if (dataMimeType != null && data != null) {
            this.mimeType = dataMimeType;
            this.configurationData = data;
        }
    }

    public PendingAddWidgetInfo(PendingAddWidgetInfo copy) {
        this.bindOptions = null;
        this.info = copy.info;
        this.boundWidget = copy.boundWidget;
        this.mimeType = copy.mimeType;
        this.configurationData = copy.configurationData;
        this.componentName = copy.componentName;
        this.itemType = copy.itemType;
        this.spanX = copy.spanX;
        this.spanY = copy.spanY;
        this.minSpanX = copy.minSpanX;
        this.minSpanY = copy.minSpanY;
        this.bindOptions = copy.bindOptions != null ? (Bundle) copy.bindOptions.clone() : null;
    }

    @Override
    public String toString() {
        return "Widget: " + this.componentName.toShortString();
    }
}
