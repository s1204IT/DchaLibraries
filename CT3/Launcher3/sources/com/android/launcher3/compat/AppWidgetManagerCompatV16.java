package com.android.launcher3.compat;

import android.annotation.TargetApi;
import android.app.Activity;
import android.appwidget.AppWidgetHost;
import android.appwidget.AppWidgetProviderInfo;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import com.android.launcher3.IconCache;
import com.android.launcher3.LauncherAppWidgetProviderInfo;
import com.android.launcher3.Utilities;
import com.android.launcher3.util.ComponentKey;
import java.util.HashMap;
import java.util.List;

class AppWidgetManagerCompatV16 extends AppWidgetManagerCompat {
    AppWidgetManagerCompatV16(Context context) {
        super(context);
    }

    @Override
    public List<AppWidgetProviderInfo> getAllProviders() {
        return this.mAppWidgetManager.getInstalledProviders();
    }

    @Override
    public String loadLabel(LauncherAppWidgetProviderInfo info) {
        return Utilities.trim(info.label);
    }

    @Override
    @TargetApi(17)
    public boolean bindAppWidgetIdIfAllowed(int appWidgetId, AppWidgetProviderInfo info, Bundle options) {
        if (Utilities.ATLEAST_JB_MR1) {
            return this.mAppWidgetManager.bindAppWidgetIdIfAllowed(appWidgetId, info.provider, options);
        }
        return this.mAppWidgetManager.bindAppWidgetIdIfAllowed(appWidgetId, info.provider);
    }

    @Override
    public UserHandleCompat getUser(LauncherAppWidgetProviderInfo info) {
        return UserHandleCompat.myUserHandle();
    }

    @Override
    public void startConfigActivity(AppWidgetProviderInfo info, int widgetId, Activity activity, AppWidgetHost host, int requestCode) {
        Intent intent = new Intent("android.appwidget.action.APPWIDGET_CONFIGURE");
        intent.setComponent(info.configure);
        intent.putExtra("appWidgetId", widgetId);
        Utilities.startActivityForResultSafely(activity, intent, requestCode);
    }

    @Override
    public Drawable loadPreview(AppWidgetProviderInfo info) {
        return this.mContext.getPackageManager().getDrawable(info.provider.getPackageName(), info.previewImage, null);
    }

    @Override
    public Drawable loadIcon(LauncherAppWidgetProviderInfo info, IconCache cache) {
        return cache.getFullResIcon(info.provider.getPackageName(), info.icon);
    }

    @Override
    public Bitmap getBadgeBitmap(LauncherAppWidgetProviderInfo info, Bitmap bitmap, int imageWidth, int imageHeight) {
        return bitmap;
    }

    @Override
    public LauncherAppWidgetProviderInfo findProvider(ComponentName provider, UserHandleCompat user) {
        for (AppWidgetProviderInfo info : this.mAppWidgetManager.getInstalledProviders()) {
            if (info.provider.equals(provider)) {
                return LauncherAppWidgetProviderInfo.fromProviderInfo(this.mContext, info);
            }
        }
        return null;
    }

    @Override
    public HashMap<ComponentKey, AppWidgetProviderInfo> getAllProvidersMap() {
        HashMap<ComponentKey, AppWidgetProviderInfo> result = new HashMap<>();
        UserHandleCompat user = UserHandleCompat.myUserHandle();
        for (AppWidgetProviderInfo info : this.mAppWidgetManager.getInstalledProviders()) {
            result.put(new ComponentKey(info.provider, user), info);
        }
        return result;
    }
}
