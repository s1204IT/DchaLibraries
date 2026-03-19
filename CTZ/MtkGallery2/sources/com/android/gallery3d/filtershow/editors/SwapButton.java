package com.android.gallery3d.filtershow.editors;

import android.content.Context;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.widget.Button;

public class SwapButton extends Button implements GestureDetector.OnGestureListener {
    public static int ANIM_DURATION = 200;
    private int mCurrentMenuIndex;
    private GestureDetector mDetector;
    private SwapButtonListener mListener;
    private Menu mMenu;

    public interface SwapButtonListener {
        void swapLeft(MenuItem menuItem);

        void swapRight(MenuItem menuItem);
    }

    public SwapButton(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        this.mDetector = new GestureDetector(context, this);
    }

    public void setListener(SwapButtonListener swapButtonListener) {
        this.mListener = swapButtonListener;
    }

    @Override
    public boolean onTouchEvent(MotionEvent motionEvent) {
        if (!this.mDetector.onTouchEvent(motionEvent)) {
            return super.onTouchEvent(motionEvent);
        }
        return true;
    }

    @Override
    public boolean onDown(MotionEvent motionEvent) {
        return true;
    }

    @Override
    public void onShowPress(MotionEvent motionEvent) {
    }

    @Override
    public boolean onSingleTapUp(MotionEvent motionEvent) {
        callOnClick();
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
        if (this.mMenu == null) {
            return false;
        }
        if (motionEvent.getX() - motionEvent2.getX() > 0.0f) {
            this.mCurrentMenuIndex++;
            if (this.mCurrentMenuIndex == this.mMenu.size()) {
                this.mCurrentMenuIndex = 0;
            }
            if (this.mListener != null) {
                this.mListener.swapRight(this.mMenu.getItem(this.mCurrentMenuIndex));
            }
        } else {
            this.mCurrentMenuIndex--;
            if (this.mCurrentMenuIndex < 0) {
                this.mCurrentMenuIndex = this.mMenu.size() - 1;
            }
            if (this.mListener != null) {
                this.mListener.swapLeft(this.mMenu.getItem(this.mCurrentMenuIndex));
            }
        }
        return true;
    }
}
