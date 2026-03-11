package com.android.launcher3.accessibility;

import android.view.View;
import android.view.accessibility.AccessibilityManager;

public class DragViewStateAnnouncer implements Runnable {
    private final View mTargetView;

    private DragViewStateAnnouncer(View view) {
        this.mTargetView = view;
    }

    public void announce(CharSequence msg) {
        this.mTargetView.setContentDescription(msg);
        this.mTargetView.removeCallbacks(this);
        this.mTargetView.postDelayed(this, 200L);
    }

    public void cancel() {
        this.mTargetView.removeCallbacks(this);
    }

    @Override
    public void run() {
        this.mTargetView.sendAccessibilityEvent(4);
    }

    public static DragViewStateAnnouncer createFor(View v) {
        if (((AccessibilityManager) v.getContext().getSystemService("accessibility")).isEnabled()) {
            return new DragViewStateAnnouncer(v);
        }
        return null;
    }
}
