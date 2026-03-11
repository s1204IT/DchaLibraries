package com.android.launcher3;

import android.annotation.TargetApi;
import android.content.ComponentName;
import android.content.Context;
import android.os.Bundle;
import android.os.UserManager;
import android.util.AttributeSet;
import android.util.Pair;
import com.android.launcher3.DropTarget;
import com.android.launcher3.compat.UserHandleCompat;

public class UninstallDropTarget extends ButtonDropTarget {

    public interface UninstallSource {
        void deferCompleteDropAfterUninstallActivity();

        void onUninstallActivityReturned(boolean z);
    }

    public UninstallDropTarget(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public UninstallDropTarget(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        this.mHoverColor = getResources().getColor(R.color.uninstall_target_hover_tint);
        setDrawable(R.drawable.ic_uninstall_launcher);
    }

    @Override
    protected boolean supportsDrop(DragSource source, Object info) {
        return supportsDrop(getContext(), info);
    }

    @TargetApi(18)
    public static boolean supportsDrop(Context context, Object info) {
        if (Utilities.ATLEAST_JB_MR2) {
            UserManager userManager = (UserManager) context.getSystemService("user");
            Bundle restrictions = userManager.getUserRestrictions();
            if (restrictions.getBoolean("no_control_apps", false) || restrictions.getBoolean("no_uninstall_apps", false)) {
                return false;
            }
        }
        Pair<ComponentName, Integer> componentInfo = getAppInfoFlags(info);
        return (componentInfo == null || (((Integer) componentInfo.second).intValue() & 1) == 0) ? false : true;
    }

    private static Pair<ComponentName, Integer> getAppInfoFlags(Object item) {
        if (item instanceof AppInfo) {
            AppInfo info = (AppInfo) item;
            return Pair.create(info.componentName, Integer.valueOf(info.flags));
        }
        if (item instanceof ShortcutInfo) {
            ShortcutInfo info2 = (ShortcutInfo) item;
            ComponentName component = info2.getTargetComponent();
            if (info2.itemType == 0 && component != null) {
                return Pair.create(component, Integer.valueOf(info2.flags));
            }
        }
        return null;
    }

    @Override
    public void onDrop(DropTarget.DragObject d) {
        if (d.dragSource instanceof UninstallSource) {
            ((UninstallSource) d.dragSource).deferCompleteDropAfterUninstallActivity();
        }
        super.onDrop(d);
    }

    @Override
    void completeDrop(final DropTarget.DragObject d) {
        final Pair<ComponentName, Integer> componentInfo = getAppInfoFlags(d.dragInfo);
        final UserHandleCompat user = ((ItemInfo) d.dragInfo).user;
        if (startUninstallActivity(this.mLauncher, d.dragInfo)) {
            Runnable checkIfUninstallWasSuccess = new Runnable() {
                @Override
                public void run() {
                    String packageName = ((ComponentName) componentInfo.first).getPackageName();
                    boolean uninstallSuccessful = !AllAppsList.packageHasActivities(UninstallDropTarget.this.getContext(), packageName, user);
                    UninstallDropTarget.this.sendUninstallResult(d.dragSource, uninstallSuccessful);
                }
            };
            this.mLauncher.addOnResumeCallback(checkIfUninstallWasSuccess);
        } else {
            sendUninstallResult(d.dragSource, false);
        }
    }

    public static boolean startUninstallActivity(Launcher launcher, Object info) {
        Pair<ComponentName, Integer> componentInfo = getAppInfoFlags(info);
        UserHandleCompat user = ((ItemInfo) info).user;
        return launcher.startApplicationUninstallActivity((ComponentName) componentInfo.first, ((Integer) componentInfo.second).intValue(), user);
    }

    void sendUninstallResult(DragSource target, boolean result) {
        if (!(target instanceof UninstallSource)) {
            return;
        }
        ((UninstallSource) target).onUninstallActivityReturned(result);
    }
}
