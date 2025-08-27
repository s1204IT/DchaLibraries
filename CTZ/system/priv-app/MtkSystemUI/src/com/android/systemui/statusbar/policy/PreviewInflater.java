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

/* loaded from: classes.dex */
public class PreviewInflater {
    private Context mContext;
    private LockPatternUtils mLockPatternUtils;

    public PreviewInflater(Context context, LockPatternUtils lockPatternUtils) {
        this.mContext = context;
        this.mLockPatternUtils = lockPatternUtils;
    }

    public View inflatePreview(Intent intent) {
        return inflatePreview(getWidgetInfo(intent));
    }

    public View inflatePreviewFromService(ComponentName componentName) {
        return inflatePreview(getWidgetInfoFromService(componentName));
    }

    private KeyguardPreviewContainer inflatePreview(WidgetInfo widgetInfo) {
        View viewInflateWidgetView;
        if (widgetInfo == null || (viewInflateWidgetView = inflateWidgetView(widgetInfo)) == null) {
            return null;
        }
        KeyguardPreviewContainer keyguardPreviewContainer = new KeyguardPreviewContainer(this.mContext, null);
        keyguardPreviewContainer.addView(viewInflateWidgetView);
        return keyguardPreviewContainer;
    }

    private View inflateWidgetView(WidgetInfo widgetInfo) throws PackageManager.NameNotFoundException {
        try {
            Context contextCreatePackageContext = this.mContext.createPackageContext(widgetInfo.contextPackage, 4);
            return ((LayoutInflater) contextCreatePackageContext.getSystemService("layout_inflater")).cloneInContext(contextCreatePackageContext).inflate(widgetInfo.layoutId, (ViewGroup) null, false);
        } catch (PackageManager.NameNotFoundException | RuntimeException e) {
            Log.w("PreviewInflater", "Error creating widget view", e);
            return null;
        }
    }

    private WidgetInfo getWidgetInfoFromService(ComponentName componentName) {
        try {
            return getWidgetInfoFromMetaData(componentName.getPackageName(), this.mContext.getPackageManager().getServiceInfo(componentName, 128).metaData);
        } catch (PackageManager.NameNotFoundException e) {
            Log.w("PreviewInflater", "Failed to load preview; " + componentName.flattenToShortString() + " not found", e);
            return null;
        }
    }

    private WidgetInfo getWidgetInfoFromMetaData(String str, Bundle bundle) {
        int i;
        if (bundle == null || (i = bundle.getInt("com.android.keyguard.layout")) == 0) {
            return null;
        }
        WidgetInfo widgetInfo = new WidgetInfo();
        widgetInfo.contextPackage = str;
        widgetInfo.layoutId = i;
        return widgetInfo;
    }

    private WidgetInfo getWidgetInfo(Intent intent) {
        PackageManager packageManager = this.mContext.getPackageManager();
        List listQueryIntentActivitiesAsUser = packageManager.queryIntentActivitiesAsUser(intent, 851968, KeyguardUpdateMonitor.getCurrentUser());
        if (listQueryIntentActivitiesAsUser.size() == 0) {
            return null;
        }
        ResolveInfo resolveInfoResolveActivityAsUser = packageManager.resolveActivityAsUser(intent, 852096, KeyguardUpdateMonitor.getCurrentUser());
        if (wouldLaunchResolverActivity(resolveInfoResolveActivityAsUser, listQueryIntentActivitiesAsUser) || resolveInfoResolveActivityAsUser == null || resolveInfoResolveActivityAsUser.activityInfo == null) {
            return null;
        }
        return getWidgetInfoFromMetaData(resolveInfoResolveActivityAsUser.activityInfo.packageName, resolveInfoResolveActivityAsUser.activityInfo.metaData);
    }

    public static boolean wouldLaunchResolverActivity(Context context, Intent intent, int i) {
        return getTargetActivityInfo(context, intent, i, false) == null;
    }

    public static ActivityInfo getTargetActivityInfo(Context context, Intent intent, int i, boolean z) {
        int i2;
        ResolveInfo resolveInfoResolveActivityAsUser;
        PackageManager packageManager = context.getPackageManager();
        if (!z) {
            i2 = 851968;
        } else {
            i2 = 65536;
        }
        List listQueryIntentActivitiesAsUser = packageManager.queryIntentActivitiesAsUser(intent, i2, i);
        if (listQueryIntentActivitiesAsUser.size() == 0 || (resolveInfoResolveActivityAsUser = packageManager.resolveActivityAsUser(intent, i2 | 128, i)) == null || wouldLaunchResolverActivity(resolveInfoResolveActivityAsUser, listQueryIntentActivitiesAsUser)) {
            return null;
        }
        return resolveInfoResolveActivityAsUser.activityInfo;
    }

    private static boolean wouldLaunchResolverActivity(ResolveInfo resolveInfo, List<ResolveInfo> list) {
        for (int i = 0; i < list.size(); i++) {
            ResolveInfo resolveInfo2 = list.get(i);
            if (resolveInfo2.activityInfo.name.equals(resolveInfo.activityInfo.name) && resolveInfo2.activityInfo.packageName.equals(resolveInfo.activityInfo.packageName)) {
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
