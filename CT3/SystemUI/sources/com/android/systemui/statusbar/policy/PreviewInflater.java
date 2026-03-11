package com.android.systemui.statusbar.policy;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import com.android.internal.widget.LockPatternUtils;
import com.android.keyguard.KeyguardUpdateMonitor;
import com.android.systemui.statusbar.phone.KeyguardPreviewContainer;
import java.util.List;

public class PreviewInflater {
    private Context mContext;
    private LockPatternUtils mLockPatternUtils;

    public PreviewInflater(Context context, LockPatternUtils lockPatternUtils) {
        this.mContext = context;
        this.mLockPatternUtils = lockPatternUtils;
    }

    public View inflatePreview(Intent intent) {
        WidgetInfo info = getWidgetInfo(intent);
        return inflatePreview(info);
    }

    public View inflatePreviewFromService(ComponentName componentName) {
        WidgetInfo info = getWidgetInfoFromService(componentName);
        return inflatePreview(info);
    }

    private KeyguardPreviewContainer inflatePreview(WidgetInfo info) {
        View v;
        if (info == null || (v = inflateWidgetView(info)) == null) {
            return null;
        }
        KeyguardPreviewContainer container = new KeyguardPreviewContainer(this.mContext, null);
        container.addView(v);
        return container;
    }

    private View inflateWidgetView(WidgetInfo widgetInfo) {
        try {
            Context appContext = this.mContext.createPackageContext(widgetInfo.contextPackage, 4);
            LayoutInflater appInflater = (LayoutInflater) appContext.getSystemService("layout_inflater");
            View widgetView = appInflater.cloneInContext(appContext).inflate(widgetInfo.layoutId, (ViewGroup) null, false);
            return widgetView;
        } catch (PackageManager.NameNotFoundException | RuntimeException e) {
            Log.w("PreviewInflater", "Error creating widget view", e);
            return null;
        }
    }

    private WidgetInfo getWidgetInfoFromService(ComponentName componentName) {
        PackageManager packageManager = this.mContext.getPackageManager();
        try {
            Bundle metaData = packageManager.getServiceInfo(componentName, 128).metaData;
            return getWidgetInfoFromMetaData(componentName.getPackageName(), metaData);
        } catch (PackageManager.NameNotFoundException e) {
            Log.w("PreviewInflater", "Failed to load preview; " + componentName.flattenToShortString() + " not found", e);
            return null;
        }
    }

    private WidgetInfo getWidgetInfoFromMetaData(String contextPackage, Bundle metaData) {
        int layoutId;
        WidgetInfo widgetInfo = null;
        if (metaData == null || (layoutId = metaData.getInt("com.android.keyguard.layout")) == 0) {
            return null;
        }
        WidgetInfo info = new WidgetInfo(widgetInfo);
        info.contextPackage = contextPackage;
        info.layoutId = layoutId;
        return info;
    }

    private WidgetInfo getWidgetInfo(Intent intent) {
        PackageManager packageManager = this.mContext.getPackageManager();
        List<ResolveInfo> appList = packageManager.queryIntentActivitiesAsUser(intent, 65536, KeyguardUpdateMonitor.getCurrentUser());
        if (appList.size() == 0) {
            return null;
        }
        ResolveInfo resolved = packageManager.resolveActivityAsUser(intent, 65664, KeyguardUpdateMonitor.getCurrentUser());
        if (wouldLaunchResolverActivity(resolved, appList) || resolved == null || resolved.activityInfo == null) {
            return null;
        }
        return getWidgetInfoFromMetaData(resolved.activityInfo.packageName, resolved.activityInfo.metaData);
    }

    public static boolean wouldLaunchResolverActivity(Context ctx, Intent intent, int currentUserId) {
        return getTargetActivityInfo(ctx, intent, currentUserId) == null;
    }

    public static ActivityInfo getTargetActivityInfo(Context ctx, Intent intent, int currentUserId) {
        ResolveInfo resolved;
        PackageManager packageManager = ctx.getPackageManager();
        List<ResolveInfo> appList = packageManager.queryIntentActivitiesAsUser(intent, 65536, currentUserId);
        if (appList.size() == 0 || (resolved = packageManager.resolveActivityAsUser(intent, 65664, currentUserId)) == null || wouldLaunchResolverActivity(resolved, appList)) {
            return null;
        }
        return resolved.activityInfo;
    }

    private static boolean wouldLaunchResolverActivity(ResolveInfo resolved, List<ResolveInfo> appList) {
        for (int i = 0; i < appList.size(); i++) {
            ResolveInfo tmp = appList.get(i);
            if (tmp.activityInfo.name.equals(resolved.activityInfo.name) && tmp.activityInfo.packageName.equals(resolved.activityInfo.packageName)) {
                return false;
            }
        }
        return true;
    }

    private static class WidgetInfo {
        String contextPackage;
        int layoutId;

        WidgetInfo(WidgetInfo widgetInfo) {
            this();
        }

        private WidgetInfo() {
        }
    }
}
