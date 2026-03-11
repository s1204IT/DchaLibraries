package com.android.launcher3;

import android.view.View;

public class CheckLongPressHelper {
    boolean mHasPerformedLongPress;
    View.OnLongClickListener mListener;
    private int mLongPressTimeout = 300;
    private CheckForLongPress mPendingCheckForLongPress;
    View mView;

    class CheckForLongPress implements Runnable {
        CheckForLongPress() {
        }

        @Override
        public void run() {
            boolean handled;
            if (CheckLongPressHelper.this.mView.getParent() == null || !CheckLongPressHelper.this.mView.hasWindowFocus() || CheckLongPressHelper.this.mHasPerformedLongPress) {
                return;
            }
            if (CheckLongPressHelper.this.mListener != null) {
                handled = CheckLongPressHelper.this.mListener.onLongClick(CheckLongPressHelper.this.mView);
            } else {
                handled = CheckLongPressHelper.this.mView.performLongClick();
            }
            if (!handled) {
                return;
            }
            CheckLongPressHelper.this.mView.setPressed(false);
            CheckLongPressHelper.this.mHasPerformedLongPress = true;
        }
    }

    public CheckLongPressHelper(View v) {
        this.mView = v;
    }

    public void setLongPressTimeout(int longPressTimeout) {
        this.mLongPressTimeout = longPressTimeout;
    }

    public void postCheckForLongPress() {
        this.mHasPerformedLongPress = false;
        if (this.mPendingCheckForLongPress == null) {
            this.mPendingCheckForLongPress = new CheckForLongPress();
        }
        this.mView.postDelayed(this.mPendingCheckForLongPress, this.mLongPressTimeout);
    }

    public void cancelLongPress() {
        this.mHasPerformedLongPress = false;
        if (this.mPendingCheckForLongPress == null) {
            return;
        }
        this.mView.removeCallbacks(this.mPendingCheckForLongPress);
        this.mPendingCheckForLongPress = null;
    }

    public boolean hasPerformedLongPress() {
        return this.mHasPerformedLongPress;
    }
}
