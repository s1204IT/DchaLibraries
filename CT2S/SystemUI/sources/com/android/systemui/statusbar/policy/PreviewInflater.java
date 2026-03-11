package com.android.systemui.statusbar.policy;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import com.android.internal.widget.LockPatternUtils;
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
        View v;
        WidgetInfo info = getWidgetInfo(intent);
        if (info != null && (v = inflateWidgetView(info)) != null) {
            KeyguardPreviewContainer container = new KeyguardPreviewContainer(this.mContext, null);
            container.addView(v);
            return container;
        }
        return null;
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

    private WidgetInfo getWidgetInfo(Intent intent) {
        WidgetInfo info = new WidgetInfo();
        PackageManager packageManager = this.mContext.getPackageManager();
        List<ResolveInfo> appList = packageManager.queryIntentActivitiesAsUser(intent, 65536, this.mLockPatternUtils.getCurrentUser());
        if (appList.size() == 0) {
            return null;
        }
        ResolveInfo resolved = packageManager.resolveActivityAsUser(intent, 65664, this.mLockPatternUtils.getCurrentUser());
        if (wouldLaunchResolverActivity(resolved, appList)) {
            return null;
        }
        if (resolved == null || resolved.activityInfo == null) {
            return null;
        }
        if (resolved.activityInfo.metaData == null || resolved.activityInfo.metaData.isEmpty()) {
            return null;
        }
        int layoutId = resolved.activityInfo.metaData.getInt("com.android.keyguard.layout");
        if (layoutId == 0) {
            return null;
        }
        info.contextPackage = resolved.activityInfo.packageName;
        info.layoutId = layoutId;
        return info;
    }

    public static boolean wouldLaunchResolverActivity(Context ctx, Intent intent, int currentUserId) {
        PackageManager packageManager = ctx.getPackageManager();
        List<ResolveInfo> appList = packageManager.queryIntentActivitiesAsUser(intent, 65536, currentUserId);
        if (appList.size() == 0) {
            return false;
        }
        ResolveInfo resolved = packageManager.resolveActivityAsUser(intent, 65664, currentUserId);
        return wouldLaunchResolverActivity(resolved, appList);
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

        private WidgetInfo() {
        }
    }
}
