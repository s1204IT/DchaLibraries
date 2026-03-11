package com.android.launcher3.touch;

import android.graphics.PointF;
import android.graphics.Rect;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import com.android.launcher3.AbstractFloatingView;
import com.android.launcher3.CellLayout;
import com.android.launcher3.DeviceProfile;
import com.android.launcher3.Launcher;
import com.android.launcher3.LauncherState;
import com.android.launcher3.Workspace;
import com.android.launcher3.dragndrop.DragLayer;
import com.android.launcher3.views.OptionsPopupView;

public class WorkspaceTouchListener implements View.OnTouchListener, Runnable {
    private static final int STATE_CANCELLED = 0;
    private static final int STATE_COMPLETED = 3;
    private static final int STATE_PENDING_PARENT_INFORM = 2;
    private static final int STATE_REQUESTED = 1;
    private final Launcher mLauncher;
    private final Workspace mWorkspace;
    private final Rect mTempRect = new Rect();
    private final PointF mTouchDownPoint = new PointF();
    private int mLongPressState = 0;

    public WorkspaceTouchListener(Launcher launcher, Workspace workspace) {
        this.mLauncher = launcher;
        this.mWorkspace = workspace;
    }

    @Override
    public boolean onTouch(View view, MotionEvent motionEvent) {
        boolean z;
        int actionMasked = motionEvent.getActionMasked();
        if (actionMasked == 0) {
            boolean zCanHandleLongPress = canHandleLongPress();
            if (zCanHandleLongPress) {
                DeviceProfile deviceProfile = this.mLauncher.getDeviceProfile();
                DragLayer dragLayer = this.mLauncher.getDragLayer();
                Rect insets = deviceProfile.getInsets();
                this.mTempRect.set(insets.left, insets.top, dragLayer.getWidth() - insets.right, dragLayer.getHeight() - insets.bottom);
                this.mTempRect.inset(deviceProfile.edgeMarginPx, deviceProfile.edgeMarginPx);
                zCanHandleLongPress = this.mTempRect.contains((int) motionEvent.getX(), (int) motionEvent.getY());
            }
            cancelLongPress();
            if (zCanHandleLongPress) {
                this.mLongPressState = 1;
                this.mTouchDownPoint.set(motionEvent.getX(), motionEvent.getY());
                this.mWorkspace.postDelayed(this, ViewConfiguration.getLongPressTimeout());
            }
            this.mWorkspace.onTouchEvent(motionEvent);
            return true;
        }
        if (this.mLongPressState == 2) {
            motionEvent.setAction(3);
            this.mWorkspace.onTouchEvent(motionEvent);
            motionEvent.setAction(actionMasked);
            this.mLongPressState = 3;
        }
        if (this.mLongPressState != 3) {
            if (this.mLongPressState == 1) {
                this.mWorkspace.onTouchEvent(motionEvent);
                if (this.mWorkspace.isHandlingTouch()) {
                    cancelLongPress();
                }
                z = true;
            } else {
                z = false;
            }
        } else {
            z = true;
        }
        if ((actionMasked == 1 || actionMasked == 6) && !this.mWorkspace.isTouchActive() && ((CellLayout) this.mWorkspace.getChildAt(this.mWorkspace.getCurrentPage())) != null) {
            this.mWorkspace.onWallpaperTap(motionEvent);
        }
        if (actionMasked == 1 || actionMasked == 3) {
            cancelLongPress();
        }
        return z;
    }

    private boolean canHandleLongPress() {
        return AbstractFloatingView.getTopOpenView(this.mLauncher) == null && this.mLauncher.isInState(LauncherState.NORMAL);
    }

    private void cancelLongPress() {
        this.mWorkspace.removeCallbacks(this);
        this.mLongPressState = 0;
    }

    @Override
    public void run() {
        if (this.mLongPressState == 1) {
            if (canHandleLongPress()) {
                this.mLongPressState = 2;
                this.mWorkspace.getParent().requestDisallowInterceptTouchEvent(true);
                this.mWorkspace.performHapticFeedback(0, 1);
                this.mLauncher.getUserEventDispatcher().logActionOnContainer(1, 0, 1, this.mWorkspace.getCurrentPage());
                OptionsPopupView.showDefaultOptions(this.mLauncher, this.mTouchDownPoint.x, this.mTouchDownPoint.y);
                return;
            }
            cancelLongPress();
        }
    }
}
