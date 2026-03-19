package com.android.launcher3.views;

import android.content.Context;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityEvent;
import android.widget.FrameLayout;
import com.android.launcher3.AbstractFloatingView;
import com.android.launcher3.BaseActivity;
import com.android.launcher3.BaseDraggingActivity;
import com.android.launcher3.InsettableFrameLayout;
import com.android.launcher3.Utilities;
import com.android.launcher3.util.MultiValueAlpha;
import com.android.launcher3.util.TouchController;
import java.util.ArrayList;

public abstract class BaseDragLayer<T extends BaseDraggingActivity> extends InsettableFrameLayout {
    protected TouchController mActiveController;
    protected final T mActivity;
    protected TouchController[] mControllers;
    protected final Rect mHitRect;
    private final MultiValueAlpha mMultiValueAlpha;
    protected final int[] mTmpXY;
    private TouchCompleteListener mTouchCompleteListener;

    public interface TouchCompleteListener {
        void onTouchComplete();
    }

    public BaseDragLayer(Context context, AttributeSet attributeSet, int i) {
        super(context, attributeSet);
        this.mTmpXY = new int[2];
        this.mHitRect = new Rect();
        this.mActivity = (T) BaseActivity.fromContext(context);
        this.mMultiValueAlpha = new MultiValueAlpha(this, i);
    }

    public boolean isEventOverView(View view, MotionEvent motionEvent) {
        getDescendantRectRelativeToSelf(view, this.mHitRect);
        return this.mHitRect.contains((int) motionEvent.getX(), (int) motionEvent.getY());
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent motionEvent) {
        int action = motionEvent.getAction();
        if (action == 1 || action == 3) {
            if (this.mTouchCompleteListener != null) {
                this.mTouchCompleteListener.onTouchComplete();
            }
            this.mTouchCompleteListener = null;
        } else if (action == 0) {
            this.mActivity.finishAutoCancelActionMode();
        }
        return findActiveController(motionEvent);
    }

    protected boolean findActiveController(MotionEvent motionEvent) {
        this.mActiveController = null;
        AbstractFloatingView topOpenView = AbstractFloatingView.getTopOpenView(this.mActivity);
        if (topOpenView != null && topOpenView.onControllerInterceptTouchEvent(motionEvent)) {
            this.mActiveController = topOpenView;
            return true;
        }
        for (TouchController touchController : this.mControllers) {
            if (touchController.onControllerInterceptTouchEvent(motionEvent)) {
                this.mActiveController = touchController;
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean onRequestSendAccessibilityEvent(View view, AccessibilityEvent accessibilityEvent) {
        AbstractFloatingView topOpenViewWithType = AbstractFloatingView.getTopOpenViewWithType(this.mActivity, AbstractFloatingView.TYPE_ACCESSIBLE);
        if (topOpenViewWithType != null) {
            if (view == topOpenViewWithType) {
                return super.onRequestSendAccessibilityEvent(view, accessibilityEvent);
            }
            return false;
        }
        return super.onRequestSendAccessibilityEvent(view, accessibilityEvent);
    }

    @Override
    public void addChildrenForAccessibility(ArrayList<View> arrayList) {
        AbstractFloatingView topOpenViewWithType = AbstractFloatingView.getTopOpenViewWithType(this.mActivity, AbstractFloatingView.TYPE_ACCESSIBLE);
        if (topOpenViewWithType != null) {
            addAccessibleChildToList(topOpenViewWithType, arrayList);
        } else {
            super.addChildrenForAccessibility(arrayList);
        }
    }

    protected void addAccessibleChildToList(View view, ArrayList<View> arrayList) {
        if (view.isImportantForAccessibility()) {
            arrayList.add(view);
        } else {
            view.addChildrenForAccessibility(arrayList);
        }
    }

    @Override
    public void onViewRemoved(final View view) {
        super.onViewRemoved(view);
        if (view instanceof AbstractFloatingView) {
            postDelayed(new Runnable() {
                @Override
                public final void run() {
                    BaseDragLayer.lambda$onViewRemoved$0(view);
                }
            }, 16L);
        }
    }

    static void lambda$onViewRemoved$0(View view) {
        AbstractFloatingView abstractFloatingView = (AbstractFloatingView) view;
        if (abstractFloatingView.isOpen()) {
            abstractFloatingView.close(false);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent motionEvent) {
        int action = motionEvent.getAction();
        if (action == 1 || action == 3) {
            if (this.mTouchCompleteListener != null) {
                this.mTouchCompleteListener.onTouchComplete();
            }
            this.mTouchCompleteListener = null;
        }
        if (this.mActiveController != null) {
            return this.mActiveController.onControllerTouchEvent(motionEvent);
        }
        return findActiveController(motionEvent);
    }

    public float getDescendantRectRelativeToSelf(View view, Rect rect) {
        this.mTmpXY[0] = 0;
        this.mTmpXY[1] = 0;
        float descendantCoordRelativeToSelf = getDescendantCoordRelativeToSelf(view, this.mTmpXY);
        rect.set(this.mTmpXY[0], this.mTmpXY[1], (int) (this.mTmpXY[0] + (view.getMeasuredWidth() * descendantCoordRelativeToSelf)), (int) (this.mTmpXY[1] + (view.getMeasuredHeight() * descendantCoordRelativeToSelf)));
        return descendantCoordRelativeToSelf;
    }

    public float getLocationInDragLayer(View view, int[] iArr) {
        iArr[0] = 0;
        iArr[1] = 0;
        return getDescendantCoordRelativeToSelf(view, iArr);
    }

    public float getDescendantCoordRelativeToSelf(View view, int[] iArr) {
        return getDescendantCoordRelativeToSelf(view, iArr, false);
    }

    public float getDescendantCoordRelativeToSelf(View view, int[] iArr, boolean z) {
        return Utilities.getDescendantCoordRelativeToAncestor(view, this, iArr, z);
    }

    public void mapCoordInSelfToDescendant(View view, int[] iArr) {
        Utilities.mapCoordInSelfToDescendant(view, this, iArr);
    }

    public void getViewRectRelativeToSelf(View view, Rect rect) {
        int[] iArr = new int[2];
        getLocationInWindow(iArr);
        int i = iArr[0];
        int i2 = iArr[1];
        view.getLocationInWindow(iArr);
        int i3 = iArr[0] - i;
        int i4 = iArr[1] - i2;
        rect.set(i3, i4, view.getMeasuredWidth() + i3, view.getMeasuredHeight() + i4);
    }

    @Override
    public boolean dispatchUnhandledMove(View view, int i) {
        return AbstractFloatingView.getTopOpenView(this.mActivity) != null;
    }

    @Override
    protected boolean onRequestFocusInDescendants(int i, Rect rect) {
        AbstractFloatingView topOpenView = AbstractFloatingView.getTopOpenView(this.mActivity);
        if (topOpenView != null) {
            return topOpenView.requestFocus(i, rect);
        }
        return super.onRequestFocusInDescendants(i, rect);
    }

    @Override
    public void addFocusables(ArrayList<View> arrayList, int i, int i2) {
        AbstractFloatingView topOpenView = AbstractFloatingView.getTopOpenView(this.mActivity);
        if (topOpenView != null) {
            topOpenView.addFocusables(arrayList, i);
        } else {
            super.addFocusables(arrayList, i, i2);
        }
    }

    public void setTouchCompleteListener(TouchCompleteListener touchCompleteListener) {
        this.mTouchCompleteListener = touchCompleteListener;
    }

    @Override
    public LayoutParams generateLayoutParams(AttributeSet attributeSet) {
        return new LayoutParams(getContext(), attributeSet);
    }

    @Override
    protected LayoutParams generateDefaultLayoutParams() {
        return new LayoutParams(-2, -2);
    }

    @Override
    protected boolean checkLayoutParams(ViewGroup.LayoutParams layoutParams) {
        return layoutParams instanceof LayoutParams;
    }

    @Override
    protected LayoutParams generateLayoutParams(ViewGroup.LayoutParams layoutParams) {
        return new LayoutParams(layoutParams);
    }

    public MultiValueAlpha.AlphaProperty getAlphaProperty(int i) {
        return this.mMultiValueAlpha.getProperty(i);
    }

    public static class LayoutParams extends InsettableFrameLayout.LayoutParams {
        public boolean customPosition;
        public int x;
        public int y;

        public LayoutParams(Context context, AttributeSet attributeSet) {
            super(context, attributeSet);
            this.customPosition = false;
        }

        public LayoutParams(int i, int i2) {
            super(i, i2);
            this.customPosition = false;
        }

        public LayoutParams(ViewGroup.LayoutParams layoutParams) {
            super(layoutParams);
            this.customPosition = false;
        }

        public void setWidth(int i) {
            this.width = i;
        }

        public int getWidth() {
            return this.width;
        }

        public void setHeight(int i) {
            this.height = i;
        }

        public int getHeight() {
            return this.height;
        }

        public void setX(int i) {
            this.x = i;
        }

        public int getX() {
            return this.x;
        }

        public void setY(int i) {
            this.y = i;
        }

        public int getY() {
            return this.y;
        }
    }

    @Override
    protected void onLayout(boolean z, int i, int i2, int i3, int i4) {
        super.onLayout(z, i, i2, i3, i4);
        int childCount = getChildCount();
        for (int i5 = 0; i5 < childCount; i5++) {
            View childAt = getChildAt(i5);
            FrameLayout.LayoutParams layoutParams = (FrameLayout.LayoutParams) childAt.getLayoutParams();
            if (layoutParams instanceof LayoutParams) {
                LayoutParams layoutParams2 = (LayoutParams) layoutParams;
                if (layoutParams2.customPosition) {
                    childAt.layout(layoutParams2.x, layoutParams2.y, layoutParams2.x + layoutParams2.width, layoutParams2.y + layoutParams2.height);
                }
            }
        }
    }
}
