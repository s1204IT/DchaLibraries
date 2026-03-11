package com.android.launcher2;

import android.appwidget.AppWidgetProviderInfo;
import android.content.pm.ResolveInfo;

public class InstallWidgetReceiver {

    public static class WidgetMimeTypeHandlerData {
        public ResolveInfo resolveInfo;
        public AppWidgetProviderInfo widgetInfo;

        public WidgetMimeTypeHandlerData(ResolveInfo rInfo, AppWidgetProviderInfo wInfo) {
            this.resolveInfo = rInfo;
            this.widgetInfo = wInfo;
        }
    }
}
