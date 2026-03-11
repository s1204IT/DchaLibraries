package com.android.systemui.statusbar.phone;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.ScrollView;

public class ObservableScrollView extends ScrollView {
    private boolean mBlockFlinging;
    private boolean mHandlingTouchEvent;
    private int mLastOverscrollAmount;
    private float mLastX;
    private float mLastY;
    private Listener mListener;
    private boolean mTouchCancelled;
    private boolean mTouchEnabled;

    public interface Listener {
        void onOverscrolled(float f, float f2, int i);

        void onScrollChanged();
    }

    public ObservableScrollView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        this.mTouchEnabled = true;
    }

    private int getMaxScrollY() {
        if (getChildCount() > 0) {
            return Math.max(0, getChildAt(0).getHeight() - ((getHeight() - this.mPaddingBottom) - this.mPaddingTop));
        }
        return 0;
    }

    @Override
    public boolean onTouchEvent(MotionEvent motionEvent) {
        this.mHandlingTouchEvent = true;
        this.mLastX = motionEvent.getX();
        this.mLastY = motionEvent.getY();
        boolean zOnTouchEvent = super.onTouchEvent(motionEvent);
        this.mHandlingTouchEvent = false;
        return zOnTouchEvent;
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent motionEvent) {
        this.mHandlingTouchEvent = true;
        this.mLastX = motionEvent.getX();
        this.mLastY = motionEvent.getY();
        boolean zOnInterceptTouchEvent = super.onInterceptTouchEvent(motionEvent);
        this.mHandlingTouchEvent = false;
        return zOnInterceptTouchEvent;
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent motionEvent) {
        if (motionEvent.getAction() == 0) {
            if (!this.mTouchEnabled) {
                this.mTouchCancelled = true;
                return false;
            }
            this.mTouchCancelled = false;
        } else {
            if (this.mTouchCancelled) {
                return false;
            }
            if (!this.mTouchEnabled) {
                MotionEvent motionEventObtain = MotionEvent.obtain(motionEvent);
                motionEventObtain.setAction(3);
                super.dispatchTouchEvent(motionEventObtain);
                motionEventObtain.recycle();
                this.mTouchCancelled = true;
                return false;
            }
        }
        return super.dispatchTouchEvent(motionEvent);
    }

    @Override
    protected void onScrollChanged(int i, int i2, int i3, int i4) {
        super.onScrollChanged(i, i2, i3, i4);
        if (this.mListener != null) {
            this.mListener.onScrollChanged();
        }
    }

    @Override
    protected boolean overScrollBy(int i, int i2, int i3, int i4, int i5, int i6, int i7, int i8, boolean z) {
        this.mLastOverscrollAmount = Math.max(0, (i4 + i2) - getMaxScrollY());
        return super.overScrollBy(i, i2, i3, i4, i5, i6, i7, i8, z);
    }

    @Override
    public void fling(int i) {
        if (!this.mBlockFlinging) {
            super.fling(i);
        }
    }

    @Override
    protected void onOverScrolled(int i, int i2, boolean z, boolean z2) {
        super.onOverScrolled(i, i2, z, z2);
        if (this.mListener != null && this.mLastOverscrollAmount > 0) {
            this.mListener.onOverscrolled(this.mLastX, this.mLastY, this.mLastOverscrollAmount);
        }
    }
}
