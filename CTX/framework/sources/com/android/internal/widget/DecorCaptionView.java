package com.android.internal.widget;

import android.content.Context;
import android.graphics.Rect;
import android.os.RemoteException;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;
import android.view.Window;
import com.android.internal.R;
import com.android.internal.policy.PhoneWindow;
import java.util.ArrayList;

public class DecorCaptionView extends ViewGroup implements View.OnTouchListener, GestureDetector.OnGestureListener {
    private static final String TAG = "DecorCaptionView";
    private View mCaption;
    private boolean mCheckForDragging;
    private View mClickTarget;
    private View mClose;
    private final Rect mCloseRect;
    private View mContent;
    private int mDragSlop;
    private boolean mDragging;
    private GestureDetector mGestureDetector;
    private View mMaximize;
    private final Rect mMaximizeRect;
    private boolean mOverlayWithAppContent;
    private PhoneWindow mOwner;
    private boolean mShow;
    private ArrayList<View> mTouchDispatchList;
    private int mTouchDownX;
    private int mTouchDownY;

    public DecorCaptionView(Context context) {
        super(context);
        this.mOwner = null;
        this.mShow = false;
        this.mDragging = false;
        this.mOverlayWithAppContent = false;
        this.mTouchDispatchList = new ArrayList<>(2);
        this.mCloseRect = new Rect();
        this.mMaximizeRect = new Rect();
        init(context);
    }

    public DecorCaptionView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        this.mOwner = null;
        this.mShow = false;
        this.mDragging = false;
        this.mOverlayWithAppContent = false;
        this.mTouchDispatchList = new ArrayList<>(2);
        this.mCloseRect = new Rect();
        this.mMaximizeRect = new Rect();
        init(context);
    }

    public DecorCaptionView(Context context, AttributeSet attributeSet, int i) {
        super(context, attributeSet, i);
        this.mOwner = null;
        this.mShow = false;
        this.mDragging = false;
        this.mOverlayWithAppContent = false;
        this.mTouchDispatchList = new ArrayList<>(2);
        this.mCloseRect = new Rect();
        this.mMaximizeRect = new Rect();
        init(context);
    }

    private void init(Context context) {
        this.mDragSlop = ViewConfiguration.get(context).getScaledTouchSlop();
        this.mGestureDetector = new GestureDetector(context, this);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        this.mCaption = getChildAt(0);
    }

    public void setPhoneWindow(PhoneWindow phoneWindow, boolean z) {
        this.mOwner = phoneWindow;
        this.mShow = z;
        this.mOverlayWithAppContent = phoneWindow.isOverlayWithDecorCaptionEnabled();
        if (this.mOverlayWithAppContent) {
            this.mCaption.setBackgroundColor(0);
        }
        updateCaptionVisibility();
        this.mOwner.getDecorView().setOutlineProvider(ViewOutlineProvider.BOUNDS);
        this.mMaximize = findViewById(R.id.maximize_window);
        this.mClose = findViewById(R.id.close_window);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent motionEvent) {
        if (motionEvent.getAction() == 0) {
            int x = (int) motionEvent.getX();
            int y = (int) motionEvent.getY();
            if (this.mMaximizeRect.contains(x, y)) {
                this.mClickTarget = this.mMaximize;
            }
            if (this.mCloseRect.contains(x, y)) {
                this.mClickTarget = this.mClose;
            }
        }
        return this.mClickTarget != null;
    }

    @Override
    public boolean onTouchEvent(MotionEvent motionEvent) {
        if (this.mClickTarget != null) {
            this.mGestureDetector.onTouchEvent(motionEvent);
            int action = motionEvent.getAction();
            if (action == 1 || action == 3) {
                this.mClickTarget = null;
            }
            return true;
        }
        return false;
    }

    @Override
    public boolean onTouch(View view, MotionEvent motionEvent) {
        int x = (int) motionEvent.getX();
        int y = (int) motionEvent.getY();
        boolean z = motionEvent.getToolType(motionEvent.getActionIndex()) == 3;
        boolean z2 = (motionEvent.getButtonState() & 1) != 0;
        switch (motionEvent.getActionMasked()) {
            case 0:
                if (!this.mShow) {
                    return false;
                }
                if (!z || z2) {
                    this.mCheckForDragging = true;
                    this.mTouchDownX = x;
                    this.mTouchDownY = y;
                }
                break;
                break;
            case 1:
            case 3:
                if (this.mDragging) {
                    this.mDragging = false;
                    return !this.mCheckForDragging;
                }
                break;
            case 2:
                if (!this.mDragging && this.mCheckForDragging && (z || passedSlop(x, y))) {
                    this.mCheckForDragging = false;
                    this.mDragging = true;
                    startMovingTask(motionEvent.getRawX(), motionEvent.getRawY());
                }
                break;
        }
        return this.mDragging || this.mCheckForDragging;
    }

    @Override
    public ArrayList<View> buildTouchDispatchChildList() {
        this.mTouchDispatchList.ensureCapacity(3);
        if (this.mCaption != null) {
            this.mTouchDispatchList.add(this.mCaption);
        }
        if (this.mContent != null) {
            this.mTouchDispatchList.add(this.mContent);
        }
        return this.mTouchDispatchList;
    }

    @Override
    public boolean shouldDelayChildPressedState() {
        return false;
    }

    private boolean passedSlop(int i, int i2) {
        return Math.abs(i - this.mTouchDownX) > this.mDragSlop || Math.abs(i2 - this.mTouchDownY) > this.mDragSlop;
    }

    public void onConfigurationChanged(boolean z) {
        this.mShow = z;
        updateCaptionVisibility();
    }

    @Override
    public void addView(View view, int i, ViewGroup.LayoutParams layoutParams) {
        if (!(layoutParams instanceof ViewGroup.MarginLayoutParams)) {
            throw new IllegalArgumentException("params " + layoutParams + " must subclass MarginLayoutParams");
        }
        if (i >= 2 || getChildCount() >= 2) {
            throw new IllegalStateException("DecorCaptionView can only handle 1 client view");
        }
        super.addView(view, 0, layoutParams);
        this.mContent = view;
    }

    @Override
    protected void onMeasure(int i, int i2) {
        int measuredHeight;
        if (this.mCaption.getVisibility() != 8) {
            measureChildWithMargins(this.mCaption, i, 0, i2, 0);
            measuredHeight = this.mCaption.getMeasuredHeight();
        } else {
            measuredHeight = 0;
        }
        int i3 = measuredHeight;
        if (this.mContent != null) {
            if (this.mOverlayWithAppContent) {
                measureChildWithMargins(this.mContent, i, 0, i2, 0);
            } else {
                measureChildWithMargins(this.mContent, i, 0, i2, i3);
            }
        }
        setMeasuredDimension(View.MeasureSpec.getSize(i), View.MeasureSpec.getSize(i2));
    }

    @Override
    protected void onLayout(boolean z, int i, int i2, int i3, int i4) {
        int bottom;
        if (this.mCaption.getVisibility() != 8) {
            this.mCaption.layout(0, 0, this.mCaption.getMeasuredWidth(), this.mCaption.getMeasuredHeight());
            bottom = this.mCaption.getBottom() - this.mCaption.getTop();
            this.mMaximize.getHitRect(this.mMaximizeRect);
            this.mClose.getHitRect(this.mCloseRect);
        } else {
            this.mMaximizeRect.setEmpty();
            this.mCloseRect.setEmpty();
            bottom = 0;
        }
        if (this.mContent != null) {
            if (this.mOverlayWithAppContent) {
                this.mContent.layout(0, 0, this.mContent.getMeasuredWidth(), this.mContent.getMeasuredHeight());
            } else {
                this.mContent.layout(0, bottom, this.mContent.getMeasuredWidth(), this.mContent.getMeasuredHeight() + bottom);
            }
        }
        this.mOwner.notifyRestrictedCaptionAreaCallback(this.mMaximize.getLeft(), this.mMaximize.getTop(), this.mClose.getRight(), this.mClose.getBottom());
    }

    private boolean isFillingScreen() {
        return ((getWindowSystemUiVisibility() | getSystemUiVisibility()) & 2565) != 0;
    }

    private void updateCaptionVisibility() {
        boolean z;
        if (isFillingScreen() || !this.mShow) {
            z = true;
        } else {
            z = false;
        }
        this.mCaption.setVisibility(z ? 8 : 0);
        this.mCaption.setOnTouchListener(this);
    }

    private void maximizeWindow() {
        Window.WindowControllerCallback windowControllerCallback = this.mOwner.getWindowControllerCallback();
        if (windowControllerCallback != null) {
            try {
                windowControllerCallback.exitFreeformMode();
            } catch (RemoteException e) {
                Log.e(TAG, "Cannot change task workspace.");
            }
        }
    }

    public boolean isCaptionShowing() {
        return this.mShow;
    }

    public int getCaptionHeight() {
        if (this.mCaption != null) {
            return this.mCaption.getHeight();
        }
        return 0;
    }

    public void removeContentView() {
        if (this.mContent != null) {
            removeView(this.mContent);
            this.mContent = null;
        }
    }

    public View getCaption() {
        return this.mCaption;
    }

    @Override
    public ViewGroup.LayoutParams generateLayoutParams(AttributeSet attributeSet) {
        return new ViewGroup.MarginLayoutParams(getContext(), attributeSet);
    }

    @Override
    protected ViewGroup.LayoutParams generateDefaultLayoutParams() {
        return new ViewGroup.MarginLayoutParams(-1, -1);
    }

    @Override
    protected ViewGroup.LayoutParams generateLayoutParams(ViewGroup.LayoutParams layoutParams) {
        return new ViewGroup.MarginLayoutParams(layoutParams);
    }

    @Override
    protected boolean checkLayoutParams(ViewGroup.LayoutParams layoutParams) {
        return layoutParams instanceof ViewGroup.MarginLayoutParams;
    }

    @Override
    public boolean onDown(MotionEvent motionEvent) {
        return false;
    }

    @Override
    public void onShowPress(MotionEvent motionEvent) {
    }

    @Override
    public boolean onSingleTapUp(MotionEvent motionEvent) {
        if (this.mClickTarget == this.mMaximize) {
            maximizeWindow();
        } else if (this.mClickTarget == this.mClose) {
            this.mOwner.dispatchOnWindowDismissed(true, false);
        }
        return true;
    }

    @Override
    public boolean onScroll(MotionEvent motionEvent, MotionEvent motionEvent2, float f, float f2) {
        return false;
    }

    @Override
    public void onLongPress(MotionEvent motionEvent) {
    }

    @Override
    public boolean onFling(MotionEvent motionEvent, MotionEvent motionEvent2, float f, float f2) {
        return false;
    }
}
