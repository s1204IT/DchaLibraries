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

    public SwapButton(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mDetector = new GestureDetector(context, this);
    }

    public void setListener(SwapButtonListener listener) {
        this.mListener = listener;
    }

    @Override
    public boolean onTouchEvent(MotionEvent me) {
        if (this.mDetector.onTouchEvent(me)) {
            return true;
        }
        return super.onTouchEvent(me);
    }

    @Override
    public boolean onDown(MotionEvent e) {
        return true;
    }

    @Override
    public void onShowPress(MotionEvent e) {
    }

    @Override
    public boolean onSingleTapUp(MotionEvent e) {
        callOnClick();
        return true;
    }

    @Override
    public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
        return false;
    }

    @Override
    public void onLongPress(MotionEvent e) {
    }

    @Override
    public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
        if (this.mMenu == null) {
            return false;
        }
        if (e1.getX() - e2.getX() > 0.0f) {
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
