package com.android.server.timezone;

import android.R;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ProviderInfo;
import android.content.pm.ResolveInfo;
import android.os.UserHandle;
import android.util.Slog;
import java.util.List;

final class PackageTrackerHelperImpl implements ConfigHelper, PackageManagerHelper {
    private static final String TAG = "PackageTrackerHelperImpl";
    private final Context mContext;
    private final PackageManager mPackageManager;

    PackageTrackerHelperImpl(Context context) {
        this.mContext = context;
        this.mPackageManager = context.getPackageManager();
    }

    @Override
    public boolean isTrackingEnabled() {
        return this.mContext.getResources().getBoolean(R.^attr-private.pointerIconNodrop);
    }

    @Override
    public String getUpdateAppPackageName() {
        return this.mContext.getResources().getString(R.string.app_category_accessibility);
    }

    @Override
    public String getDataAppPackageName() {
        return this.mContext.getResources().getString(R.string.app_blocked_title);
    }

    @Override
    public int getCheckTimeAllowedMillis() {
        return this.mContext.getResources().getInteger(R.integer.config_esim_bootstrap_data_limit_bytes);
    }

    @Override
    public int getFailedCheckRetryCount() {
        return this.mContext.getResources().getInteger(R.integer.config_emergency_call_wait_for_connection_timeout_millis);
    }

    @Override
    public long getInstalledPackageVersion(String str) throws PackageManager.NameNotFoundException {
        return this.mPackageManager.getPackageInfo(str, 32768).getLongVersionCode();
    }

    @Override
    public boolean isPrivilegedApp(String str) throws PackageManager.NameNotFoundException {
        return this.mPackageManager.getPackageInfo(str, 32768).applicationInfo.isPrivilegedApp();
    }

    @Override
    public boolean usesPermission(String str, String str2) throws PackageManager.NameNotFoundException {
        PackageInfo packageInfo = this.mPackageManager.getPackageInfo(str, 36864);
        if (packageInfo.requestedPermissions == null) {
            return false;
        }
        for (String str3 : packageInfo.requestedPermissions) {
            if (str2.equals(str3)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean contentProviderRegistered(String str, String str2) {
        ProviderInfo providerInfoResolveContentProviderAsUser = this.mPackageManager.resolveContentProviderAsUser(str, 32768, UserHandle.SYSTEM.getIdentifier());
        if (providerInfoResolveContentProviderAsUser == null) {
            Slog.i(TAG, "contentProviderRegistered: No content provider registered with authority=" + str);
            return false;
        }
        if (!str2.equals(providerInfoResolveContentProviderAsUser.applicationInfo.packageName)) {
            Slog.i(TAG, "contentProviderRegistered: App with packageName=" + str2 + " does not expose the a content provider with authority=" + str);
            return false;
        }
        return true;
    }

    @Override
    public boolean receiverRegistered(Intent intent, String str) throws PackageManager.NameNotFoundException {
        List listQueryBroadcastReceiversAsUser = this.mPackageManager.queryBroadcastReceiversAsUser(intent, 32768, UserHandle.SYSTEM);
        if (listQueryBroadcastReceiversAsUser.size() != 1) {
            Slog.i(TAG, "receiverRegistered: Zero or multiple broadcast receiver registered for intent=" + intent + ", found=" + listQueryBroadcastReceiversAsUser);
            return false;
        }
        boolean zEquals = str.equals(((ResolveInfo) listQueryBroadcastReceiversAsUser.get(0)).activityInfo.permission);
        if (!zEquals) {
            Slog.i(TAG, "receiverRegistered: Broadcast receiver registered for intent=" + intent + " must require permission " + str);
        }
        return zEquals;
    }
}
