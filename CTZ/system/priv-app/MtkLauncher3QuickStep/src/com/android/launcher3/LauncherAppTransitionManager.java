package com.android.launcher3;

import android.app.ActivityOptions;
import android.content.Context;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.view.View;

/* loaded from: classes.dex */
public class LauncherAppTransitionManager {
    public static LauncherAppTransitionManager newInstance(Context context) {
        return (LauncherAppTransitionManager) Utilities.getOverrideObject(LauncherAppTransitionManager.class, context, R.string.app_transition_manager_class);
    }

    public ActivityOptions getActivityLaunchOptions(Launcher launcher, View view) {
        int iWidth;
        int paddingTop;
        Drawable icon;
        if (Utilities.ATLEAST_MARSHMALLOW) {
            int measuredWidth = view.getMeasuredWidth();
            int measuredHeight = view.getMeasuredHeight();
            int iWidth2 = 0;
            if ((view instanceof BubbleTextView) && (icon = ((BubbleTextView) view).getIcon()) != null) {
                Rect bounds = icon.getBounds();
                iWidth2 = (measuredWidth - bounds.width()) / 2;
                paddingTop = view.getPaddingTop();
                iWidth = bounds.width();
                measuredHeight = bounds.height();
            } else {
                iWidth = measuredWidth;
                paddingTop = 0;
            }
            return ActivityOptions.makeClipRevealAnimation(view, iWidth2, paddingTop, iWidth, measuredHeight);
        }
        if (Utilities.ATLEAST_LOLLIPOP_MR1) {
            return ActivityOptions.makeCustomAnimation(launcher, R.anim.task_open_enter, R.anim.no_anim);
        }
        return null;
    }
}
