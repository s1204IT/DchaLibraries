package com.android.quickstep.views;

import android.graphics.Rect;
import android.view.MotionEvent;
import com.android.launcher3.AbstractFloatingView;
import com.android.launcher3.Insettable;
import com.android.launcher3.Launcher;
import com.android.quickstep.ActivityControlHelper;
import com.android.quickstep.WindowTransformSwipeHandler;

public class LauncherLayoutListener extends AbstractFloatingView implements Insettable, ActivityControlHelper.LayoutListener {
    private WindowTransformSwipeHandler mHandler;
    private final Launcher mLauncher;

    public LauncherLayoutListener(Launcher launcher) {
        super(launcher, null);
        this.mLauncher = launcher;
        setVisibility(4);
        launcher.getRotationHelper().setStateHandlerRequest(2);
    }

    @Override
    public void setHandler(WindowTransformSwipeHandler windowTransformSwipeHandler) {
        this.mHandler = windowTransformSwipeHandler;
    }

    @Override
    public void setInsets(Rect rect) {
        if (this.mHandler != null) {
            this.mHandler.buildAnimationController();
        }
    }

    @Override
    public boolean onControllerInterceptTouchEvent(MotionEvent motionEvent) {
        return false;
    }

    @Override
    protected void handleClose(boolean z) {
        if (this.mIsOpen) {
            this.mIsOpen = false;
            this.mLauncher.getDragLayer().removeView(this);
            if (this.mHandler != null) {
                this.mHandler.layoutListenerClosed();
            }
        }
    }

    @Override
    public void open() {
        if (!this.mIsOpen) {
            this.mLauncher.getDragLayer().addView(this);
            this.mIsOpen = true;
        }
    }

    @Override
    protected void onMeasure(int i, int i2) {
        setMeasuredDimension(1, 1);
    }

    @Override
    public void logActionCommand(int i) {
    }

    @Override
    protected boolean isOfType(int i) {
        return (i & 64) != 0;
    }

    @Override
    public void finish() {
        setHandler(null);
        close(false);
        this.mLauncher.getRotationHelper().setStateHandlerRequest(0);
    }
}
