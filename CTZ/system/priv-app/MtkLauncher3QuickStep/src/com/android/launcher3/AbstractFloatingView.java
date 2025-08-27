package com.android.launcher3;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.AttributeSet;
import android.util.Pair;
import android.view.MotionEvent;
import android.view.View;
import android.widget.LinearLayout;
import com.android.launcher3.compat.AccessibilityManagerCompat;
import com.android.launcher3.util.TouchController;
import com.android.launcher3.views.BaseDragLayer;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/* loaded from: classes.dex */
public abstract class AbstractFloatingView extends LinearLayout implements TouchController {
    public static final int TYPE_ACCESSIBLE = 447;
    public static final int TYPE_ACTION_POPUP = 2;
    public static final int TYPE_ALL = 511;
    public static final int TYPE_DISCOVERY_BOUNCE = 64;
    public static final int TYPE_FOLDER = 1;
    public static final int TYPE_HIDE_BACK_BUTTON = 96;
    public static final int TYPE_ON_BOARD_POPUP = 32;
    public static final int TYPE_OPTIONS_POPUP = 256;
    public static final int TYPE_QUICKSTEP_PREVIEW = 64;
    public static final int TYPE_REBIND_SAFE = 112;
    public static final int TYPE_TASK_MENU = 128;
    public static final int TYPE_WIDGETS_BOTTOM_SHEET = 4;
    public static final int TYPE_WIDGETS_FULL_SHEET = 16;
    public static final int TYPE_WIDGET_RESIZE_FRAME = 8;
    protected boolean mIsOpen;

    @Retention(RetentionPolicy.SOURCE)
    public @interface FloatingViewType {
    }

    protected abstract void handleClose(boolean z);

    protected abstract boolean isOfType(int i);

    public abstract void logActionCommand(int i);

    public AbstractFloatingView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
    }

    public AbstractFloatingView(Context context, AttributeSet attributeSet, int i) {
        super(context, attributeSet, i);
    }

    @Override // android.view.View
    @SuppressLint({"ClickableViewAccessibility"})
    public boolean onTouchEvent(MotionEvent motionEvent) {
        return true;
    }

    public final void close(boolean z) {
        boolean z2 = z & (!Utilities.isPowerSaverPreventingAnimation(getContext()));
        if (this.mIsOpen) {
            BaseActivity.fromContext(getContext()).getUserEventDispatcher().resetElapsedContainerMillis("container closed");
        }
        handleClose(z2);
        this.mIsOpen = false;
    }

    public final boolean isOpen() {
        return this.mIsOpen;
    }

    protected void onWidgetsBound() {
    }

    public boolean onBackPressed() {
        logActionCommand(1);
        close(true);
        return true;
    }

    @Override // com.android.launcher3.util.TouchController
    public boolean onControllerTouchEvent(MotionEvent motionEvent) {
        return false;
    }

    protected void announceAccessibilityChanges() {
        Pair<View, String> accessibilityTarget = getAccessibilityTarget();
        if (accessibilityTarget == null || !AccessibilityManagerCompat.isAccessibilityEnabled(getContext())) {
            return;
        }
        AccessibilityManagerCompat.sendCustomAccessibilityEvent((View) accessibilityTarget.first, 32, (String) accessibilityTarget.second);
        if (this.mIsOpen) {
            sendAccessibilityEvent(8);
        }
        BaseDraggingActivity.fromContext(getContext()).getDragLayer().sendAccessibilityEvent(2048);
    }

    protected Pair<View, String> getAccessibilityTarget() {
        return null;
    }

    protected static <T extends AbstractFloatingView> T getOpenView(BaseDraggingActivity baseDraggingActivity, int i) {
        BaseDragLayer dragLayer = baseDraggingActivity.getDragLayer();
        for (int childCount = dragLayer.getChildCount() - 1; childCount >= 0; childCount--) {
            View childAt = dragLayer.getChildAt(childCount);
            if (childAt instanceof AbstractFloatingView) {
                T t = (T) childAt;
                if (t.isOfType(i) && t.isOpen()) {
                    return t;
                }
            }
        }
        return null;
    }

    public static void closeOpenContainer(BaseDraggingActivity baseDraggingActivity, int i) {
        AbstractFloatingView openView = getOpenView(baseDraggingActivity, i);
        if (openView != null) {
            openView.close(true);
        }
    }

    public static void closeOpenViews(BaseDraggingActivity baseDraggingActivity, boolean z, int i) {
        BaseDragLayer dragLayer = baseDraggingActivity.getDragLayer();
        for (int childCount = dragLayer.getChildCount() - 1; childCount >= 0; childCount--) {
            View childAt = dragLayer.getChildAt(childCount);
            if (childAt instanceof AbstractFloatingView) {
                AbstractFloatingView abstractFloatingView = (AbstractFloatingView) childAt;
                if (abstractFloatingView.isOfType(i)) {
                    abstractFloatingView.close(z);
                }
            }
        }
    }

    public static void closeAllOpenViews(BaseDraggingActivity baseDraggingActivity, boolean z) {
        closeOpenViews(baseDraggingActivity, z, 511);
        baseDraggingActivity.finishAutoCancelActionMode();
    }

    public static void closeAllOpenViews(BaseDraggingActivity baseDraggingActivity) {
        closeAllOpenViews(baseDraggingActivity, true);
    }

    public static AbstractFloatingView getTopOpenView(BaseDraggingActivity baseDraggingActivity) {
        return getTopOpenViewWithType(baseDraggingActivity, 511);
    }

    public static AbstractFloatingView getTopOpenViewWithType(BaseDraggingActivity baseDraggingActivity, int i) {
        return getOpenView(baseDraggingActivity, i);
    }
}
