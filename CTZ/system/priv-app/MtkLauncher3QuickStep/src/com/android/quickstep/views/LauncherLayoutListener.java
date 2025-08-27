package com.android.quickstep.views;

import android.graphics.Rect;
import android.view.MotionEvent;
import com.android.launcher3.AbstractFloatingView;
import com.android.launcher3.Insettable;
import com.android.launcher3.Launcher;
import com.android.quickstep.ActivityControlHelper;
import com.android.quickstep.WindowTransformSwipeHandler;

/* loaded from: classes.dex */
public class LauncherLayoutListener extends AbstractFloatingView implements Insettable, ActivityControlHelper.LayoutListener {
    private WindowTransformSwipeHandler mHandler;
    private final Launcher mLauncher;

    public LauncherLayoutListener(Launcher launcher) {
        super(launcher, null);
        this.mLauncher = launcher;
        setVisibility(4);
        launcher.getRotationHelper().setStateHandlerRequest(2);
    }

    @Override // com.android.quickstep.ActivityControlHelper.LayoutListener
    public void setHandler(WindowTransformSwipeHandler windowTransformSwipeHandler) {
        this.mHandler = windowTransformSwipeHandler;
    }

    @Override // com.android.launcher3.Insettable
    public void setInsets(Rect rect) {
        if (this.mHandler != null) {
            this.mHandler.buildAnimationController();
        }
    }

    @Override // com.android.launcher3.util.TouchController
    public boolean onControllerInterceptTouchEvent(MotionEvent motionEvent) {
        return false;
    }

    @Override // com.android.launcher3.AbstractFloatingView
    protected void handleClose(boolean z) {
        if (this.mIsOpen) {
            this.mIsOpen = false;
            this.mLauncher.getDragLayer().removeView(this);
            if (this.mHandler != null) {
                this.mHandler.layoutListenerClosed();
            }
        }
    }

    @Override // com.android.quickstep.ActivityControlHelper.LayoutListener
    public void open() {
        if (!this.mIsOpen) {
            this.mLauncher.getDragLayer().addView(this);
            this.mIsOpen = true;
        }
    }

    @Override // android.widget.LinearLayout, android.view.View
    protected void onMeasure(int i, int i2) {
        setMeasuredDimension(1, 1);
    }

    @Override // com.android.launcher3.AbstractFloatingView
    public void logActionCommand(int i) {
    }

    @Override // com.android.launcher3.AbstractFloatingView
    protected boolean isOfType(int i) {
        return (i & 64) != 0;
    }

    @Override // com.android.quickstep.ActivityControlHelper.LayoutListener
    public void finish() {
        setHandler(null);
        close(false);
        this.mLauncher.getRotationHelper().setStateHandlerRequest(0);
    }
}
