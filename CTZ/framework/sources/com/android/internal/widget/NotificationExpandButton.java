package com.android.internal.widget;

import android.content.Context;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RemoteViews;

@RemoteViews.RemoteView
public class NotificationExpandButton extends ImageView {
    public NotificationExpandButton(Context context) {
        super(context);
    }

    public NotificationExpandButton(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
    }

    public NotificationExpandButton(Context context, AttributeSet attributeSet, int i) {
        super(context, attributeSet, i);
    }

    public NotificationExpandButton(Context context, AttributeSet attributeSet, int i, int i2) {
        super(context, attributeSet, i, i2);
    }

    @Override
    public void getBoundsOnScreen(Rect rect, boolean z) {
        super.getBoundsOnScreen(rect, z);
        extendRectToMinTouchSize(rect);
    }

    private void extendRectToMinTouchSize(Rect rect) {
        int i = (int) (getResources().getDisplayMetrics().density * 48.0f);
        int i2 = i / 2;
        rect.left = rect.centerX() - i2;
        rect.right = rect.left + i;
        rect.top = rect.centerY() - i2;
        rect.bottom = rect.top + i;
    }

    @Override
    public void onInitializeAccessibilityNodeInfo(AccessibilityNodeInfo accessibilityNodeInfo) {
        super.onInitializeAccessibilityNodeInfo(accessibilityNodeInfo);
        accessibilityNodeInfo.setClassName(Button.class.getName());
    }
}
