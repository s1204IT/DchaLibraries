package com.android.launcher3.compat;

import android.annotation.TargetApi;
import android.app.Activity;
import android.appwidget.AppWidgetHost;
import android.appwidget.AppWidgetProviderInfo;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Process;
import android.os.UserHandle;
import android.os.UserManager;
import android.widget.Toast;
import com.android.launcher3.IconCache;
import com.android.launcher3.LauncherAppWidgetProviderInfo;
import com.android.launcher3.R;
import com.android.launcher3.util.ComponentKey;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

@TargetApi(21)
class AppWidgetManagerCompatVL extends AppWidgetManagerCompat {
    private final PackageManager mPm;
    private final UserManager mUserManager;

    AppWidgetManagerCompatVL(Context context) {
        super(context);
        this.mPm = context.getPackageManager();
        this.mUserManager = (UserManager) context.getSystemService("user");
    }

    @Override
    public List<AppWidgetProviderInfo> getAllProviders() {
        ArrayList<AppWidgetProviderInfo> providers = new ArrayList<>();
        for (UserHandle user : this.mUserManager.getUserProfiles()) {
            providers.addAll(this.mAppWidgetManager.getInstalledProvidersForProfile(user));
        }
        return providers;
    }

    @Override
    public String loadLabel(LauncherAppWidgetProviderInfo info) {
        return info.getLabel(this.mPm);
    }

    @Override
    public boolean bindAppWidgetIdIfAllowed(int appWidgetId, AppWidgetProviderInfo info, Bundle options) {
        return this.mAppWidgetManager.bindAppWidgetIdIfAllowed(appWidgetId, info.getProfile(), info.provider, options);
    }

    @Override
    public UserHandleCompat getUser(LauncherAppWidgetProviderInfo info) {
        if (info.isCustomWidget) {
            return UserHandleCompat.myUserHandle();
        }
        return UserHandleCompat.fromUser(info.getProfile());
    }

    @Override
    public void startConfigActivity(AppWidgetProviderInfo info, int widgetId, Activity activity, AppWidgetHost host, int requestCode) {
        try {
            host.startAppWidgetConfigureActivityForResult(activity, widgetId, 0, requestCode, null);
        } catch (ActivityNotFoundException | SecurityException e) {
            Toast.makeText(activity, R.string.activity_not_found, 0).show();
        }
    }

    @Override
    public Drawable loadPreview(AppWidgetProviderInfo info) {
        return info.loadPreviewImage(this.mContext, 0);
    }

    @Override
    public Drawable loadIcon(LauncherAppWidgetProviderInfo info, IconCache cache) {
        return info.getIcon(this.mContext, cache);
    }

    @Override
    public Bitmap getBadgeBitmap(LauncherAppWidgetProviderInfo info, Bitmap bitmap, int imageWidth, int imageHeight) {
        if (info.isCustomWidget || info.getProfile().equals(Process.myUserHandle())) {
            return bitmap;
        }
        Resources res = this.mContext.getResources();
        int badgeMinTop = res.getDimensionPixelSize(R.dimen.profile_badge_minimum_top);
        int badgeSize = Math.min(res.getDimensionPixelSize(R.dimen.profile_badge_size), Math.min(imageWidth, imageHeight - badgeMinTop));
        Rect badgeLocation = new Rect(0, 0, badgeSize, badgeSize);
        int top = Math.max(imageHeight - badgeSize, badgeMinTop);
        if (res.getConfiguration().getLayoutDirection() == 1) {
            badgeLocation.offset(0, top);
        } else {
            badgeLocation.offset(bitmap.getWidth() - badgeSize, top);
        }
        Drawable drawable = this.mPm.getUserBadgedDrawableForDensity(new BitmapDrawable(res, bitmap), info.getProfile(), badgeLocation, 0);
        if (drawable instanceof BitmapDrawable) {
            return ((BitmapDrawable) drawable).getBitmap();
        }
        bitmap.eraseColor(0);
        Canvas c = new Canvas(bitmap);
        drawable.setBounds(0, 0, bitmap.getWidth(), bitmap.getHeight());
        drawable.draw(c);
        c.setBitmap(null);
        return bitmap;
    }

    @Override
    public LauncherAppWidgetProviderInfo findProvider(ComponentName provider, UserHandleCompat user) {
        for (AppWidgetProviderInfo info : this.mAppWidgetManager.getInstalledProvidersForProfile(user.getUser())) {
            if (info.provider.equals(provider)) {
                return LauncherAppWidgetProviderInfo.fromProviderInfo(this.mContext, info);
            }
        }
        return null;
    }

    @Override
    public HashMap<ComponentKey, AppWidgetProviderInfo> getAllProvidersMap() {
        HashMap<ComponentKey, AppWidgetProviderInfo> result = new HashMap<>();
        for (UserHandle user : this.mUserManager.getUserProfiles()) {
            UserHandleCompat userHandle = UserHandleCompat.fromUser(user);
            for (AppWidgetProviderInfo info : this.mAppWidgetManager.getInstalledProvidersForProfile(user)) {
                result.put(new ComponentKey(info.provider, userHandle), info);
            }
        }
        return result;
    }
}
