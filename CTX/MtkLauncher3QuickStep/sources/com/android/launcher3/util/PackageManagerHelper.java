package com.android.launcher3.util;

import android.app.AppOpsManager;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.LauncherActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Bundle;
import android.os.UserHandle;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;
import com.android.launcher3.AppInfo;
import com.android.launcher3.ItemInfo;
import com.android.launcher3.LauncherAppWidgetInfo;
import com.android.launcher3.PendingAddItemInfo;
import com.android.launcher3.PromiseAppInfo;
import com.android.launcher3.R;
import com.android.launcher3.ShortcutInfo;
import com.android.launcher3.Utilities;
import com.android.launcher3.compat.LauncherAppsCompat;
import java.net.URISyntaxException;
import java.util.List;

public class PackageManagerHelper {
    private static final String TAG = "PackageManagerHelper";
    private final Context mContext;
    private final LauncherAppsCompat mLauncherApps;
    private final PackageManager mPm;

    public PackageManagerHelper(Context context) {
        this.mContext = context;
        this.mPm = context.getPackageManager();
        this.mLauncherApps = LauncherAppsCompat.getInstance(context);
    }

    public boolean isAppOnSdcard(String str, UserHandle userHandle) {
        ApplicationInfo applicationInfo = this.mLauncherApps.getApplicationInfo(str, 8192, userHandle);
        return (applicationInfo == null || (applicationInfo.flags & 262144) == 0) ? false : true;
    }

    public boolean isAppSuspended(String str, UserHandle userHandle) {
        ApplicationInfo applicationInfo = this.mLauncherApps.getApplicationInfo(str, 0, userHandle);
        return applicationInfo != null && isAppSuspended(applicationInfo);
    }

    public boolean isSafeMode() {
        return this.mContext.getPackageManager().isSafeMode();
    }

    public Intent getAppLaunchIntent(String str, UserHandle userHandle) {
        List<LauncherActivityInfo> activityList = this.mLauncherApps.getActivityList(str, userHandle);
        if (activityList.isEmpty()) {
            return null;
        }
        return AppInfo.makeLaunchIntent(activityList.get(0));
    }

    public static boolean isAppSuspended(ApplicationInfo applicationInfo) {
        return Utilities.ATLEAST_NOUGAT && (applicationInfo.flags & 1073741824) != 0;
    }

    public boolean hasPermissionForActivity(Intent intent, String str) {
        ResolveInfo resolveInfoResolveActivity = this.mPm.resolveActivity(intent, 0);
        if (resolveInfoResolveActivity == null) {
            return false;
        }
        if (TextUtils.isEmpty(resolveInfoResolveActivity.activityInfo.permission)) {
            return true;
        }
        if (TextUtils.isEmpty(str) || this.mPm.checkPermission(resolveInfoResolveActivity.activityInfo.permission, str) != 0) {
            return false;
        }
        if (!Utilities.ATLEAST_MARSHMALLOW || TextUtils.isEmpty(AppOpsManager.permissionToOp(resolveInfoResolveActivity.activityInfo.permission))) {
            return true;
        }
        try {
            return this.mPm.getApplicationInfo(str, 0).targetSdkVersion >= 23;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    public Intent getMarketIntent(String str) {
        return new Intent("android.intent.action.VIEW").setData(new Uri.Builder().scheme("market").authority("details").appendQueryParameter("id", str).build()).putExtra("android.intent.extra.REFERRER", new Uri.Builder().scheme("android-app").authority(this.mContext.getPackageName()).build());
    }

    public static Intent getMarketSearchIntent(Context context, String str) {
        try {
            Intent uri = Intent.parseUri(context.getString(R.string.market_search_intent), 0);
            if (!TextUtils.isEmpty(str)) {
                uri.setData(uri.getData().buildUpon().appendQueryParameter("q", str).build());
            }
            return uri;
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    public void startDetailsActivityForInfo(ItemInfo itemInfo, Rect rect, Bundle bundle) {
        if (itemInfo instanceof PromiseAppInfo) {
            this.mContext.startActivity(((PromiseAppInfo) itemInfo).getMarketIntent(this.mContext));
            return;
        }
        ComponentName targetComponent = null;
        if (itemInfo instanceof AppInfo) {
            targetComponent = ((AppInfo) itemInfo).componentName;
        } else if (itemInfo instanceof ShortcutInfo) {
            targetComponent = itemInfo.getTargetComponent();
        } else if (itemInfo instanceof PendingAddItemInfo) {
            targetComponent = ((PendingAddItemInfo) itemInfo).componentName;
        } else if (itemInfo instanceof LauncherAppWidgetInfo) {
            targetComponent = ((LauncherAppWidgetInfo) itemInfo).providerName;
        }
        if (targetComponent != null) {
            try {
                this.mLauncherApps.showAppDetailsForProfile(targetComponent, itemInfo.user, rect, bundle);
            } catch (ActivityNotFoundException | SecurityException e) {
                Toast.makeText(this.mContext, R.string.activity_not_found, 0).show();
                Log.e(TAG, "Unable to launch settings", e);
            }
        }
    }
}
