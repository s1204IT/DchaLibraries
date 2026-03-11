package com.android.launcher3;

import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import com.android.launcher3.compat.PackageInstallerCompat;

public class StylusEventHelper {
    private boolean mIsButtonPressed;
    private View mView;

    public StylusEventHelper(View view) {
        this.mView = view;
    }

    public boolean checkAndPerformStylusEvent(MotionEvent event) {
        float slop = ViewConfiguration.get(this.mView.getContext()).getScaledTouchSlop();
        if (!this.mView.isLongClickable()) {
            return false;
        }
        boolean stylusButtonPressed = isStylusButtonPressed(event);
        switch (event.getAction()) {
            case PackageInstallerCompat.STATUS_INSTALLED:
                this.mIsButtonPressed = false;
                if (stylusButtonPressed && this.mView.performLongClick()) {
                    this.mIsButtonPressed = true;
                    return true;
                }
                return false;
            case PackageInstallerCompat.STATUS_INSTALLING:
            case 3:
                this.mIsButtonPressed = false;
                return false;
            case PackageInstallerCompat.STATUS_FAILED:
                if (Utilities.pointInView(this.mView, event.getX(), event.getY(), slop)) {
                    if (!this.mIsButtonPressed && stylusButtonPressed && this.mView.performLongClick()) {
                        this.mIsButtonPressed = true;
                        return true;
                    }
                    if (this.mIsButtonPressed && !stylusButtonPressed) {
                        this.mIsButtonPressed = false;
                    }
                }
                return false;
            default:
                return false;
        }
    }

    public boolean inStylusButtonPressed() {
        return this.mIsButtonPressed;
    }

    private static boolean isStylusButtonPressed(MotionEvent event) {
        return event.getToolType(0) == 2 && (event.getButtonState() & 2) == 2;
    }
}
