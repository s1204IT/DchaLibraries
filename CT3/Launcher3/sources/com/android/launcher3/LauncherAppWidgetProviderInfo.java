package com.android.launcher3;

import android.annotation.TargetApi;
import android.appwidget.AppWidgetHostView;
import android.appwidget.AppWidgetProviderInfo;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Parcel;

public class LauncherAppWidgetProviderInfo extends AppWidgetProviderInfo {
    public boolean isCustomWidget;
    public int minSpanX;
    public int minSpanY;
    public int spanX;
    public int spanY;

    public static LauncherAppWidgetProviderInfo fromProviderInfo(Context context, AppWidgetProviderInfo info) {
        Parcel p = Parcel.obtain();
        info.writeToParcel(p, 0);
        p.setDataPosition(0);
        LauncherAppWidgetProviderInfo lawpi = new LauncherAppWidgetProviderInfo(p);
        p.recycle();
        return lawpi;
    }

    public LauncherAppWidgetProviderInfo(Parcel in) {
        super(in);
        this.isCustomWidget = false;
        initSpans();
    }

    public void initSpans() {
        LauncherAppState app = LauncherAppState.getInstance();
        InvariantDeviceProfile idp = app.getInvariantDeviceProfile();
        Rect paddingLand = idp.landscapeProfile.getWorkspacePadding(false);
        Rect paddingPort = idp.portraitProfile.getWorkspacePadding(false);
        float smallestCellWidth = DeviceProfile.calculateCellWidth(Math.min((idp.landscapeProfile.widthPx - paddingLand.left) - paddingLand.right, (idp.portraitProfile.widthPx - paddingPort.left) - paddingPort.right), idp.numColumns);
        float smallestCellHeight = DeviceProfile.calculateCellWidth(Math.min((idp.landscapeProfile.heightPx - paddingLand.top) - paddingLand.bottom, (idp.portraitProfile.heightPx - paddingPort.top) - paddingPort.bottom), idp.numRows);
        Rect widgetPadding = AppWidgetHostView.getDefaultPaddingForWidget(app.getContext(), this.provider, null);
        this.spanX = Math.max(1, (int) Math.ceil(((this.minWidth + widgetPadding.left) + widgetPadding.right) / smallestCellWidth));
        this.spanY = Math.max(1, (int) Math.ceil(((this.minHeight + widgetPadding.top) + widgetPadding.bottom) / smallestCellHeight));
        this.minSpanX = Math.max(1, (int) Math.ceil(((this.minResizeWidth + widgetPadding.left) + widgetPadding.right) / smallestCellWidth));
        this.minSpanY = Math.max(1, (int) Math.ceil(((this.minResizeHeight + widgetPadding.top) + widgetPadding.bottom) / smallestCellHeight));
    }

    @TargetApi(21)
    public String getLabel(PackageManager packageManager) {
        if (this.isCustomWidget) {
            return Utilities.trim(this.label);
        }
        return super.loadLabel(packageManager);
    }

    @TargetApi(21)
    public Drawable getIcon(Context context, IconCache cache) {
        if (this.isCustomWidget) {
            return cache.getFullResIcon(this.provider.getPackageName(), this.icon);
        }
        return super.loadIcon(context, LauncherAppState.getInstance().getInvariantDeviceProfile().fillResIconDpi);
    }

    public Point getMinSpans(InvariantDeviceProfile idp, Context context) {
        return new Point((this.resizeMode & 1) != 0 ? this.minSpanX : -1, (this.resizeMode & 2) != 0 ? this.minSpanY : -1);
    }
}
