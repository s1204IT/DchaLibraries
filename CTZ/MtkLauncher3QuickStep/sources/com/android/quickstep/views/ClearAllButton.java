package com.android.quickstep.views;

import android.content.Context;
import android.graphics.Rect;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.Button;

public class ClearAllButton extends Button {
    RecentsView mRecentsView;

    public ClearAllButton(Context context, @Nullable AttributeSet attributeSet) {
        super(context, attributeSet);
    }

    public void setRecentsView(RecentsView recentsView) {
        this.mRecentsView = recentsView;
    }

    @Override
    public void onInitializeAccessibilityNodeInfo(AccessibilityNodeInfo accessibilityNodeInfo) {
        super.onInitializeAccessibilityNodeInfo(accessibilityNodeInfo);
        accessibilityNodeInfo.setParent(this.mRecentsView);
    }

    @Override
    protected void onFocusChanged(boolean z, int i, Rect rect) {
        super.onFocusChanged(z, i, rect);
        if (z) {
            this.mRecentsView.revealClearAllButton();
        }
    }
}
