package android.support.design.widget;

import android.support.v4.view.ViewCompat;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.widget.OverScroller;
import com.mediatek.gallerybasic.base.Generator;

abstract class HeaderBehavior<V extends View> extends ViewOffsetBehavior<V> {
    private Runnable flingRunnable;
    private boolean isBeingDragged;
    private int lastMotionY;
    OverScroller scroller;
    private VelocityTracker velocityTracker;
    private int activePointerId = -1;
    private int touchSlop = -1;

    @Override
    public boolean onInterceptTouchEvent(CoordinatorLayout parent, V child, MotionEvent ev) {
        int pointerIndex;
        if (this.touchSlop < 0) {
            this.touchSlop = ViewConfiguration.get(parent.getContext()).getScaledTouchSlop();
        }
        int action = ev.getAction();
        if (action == 2 && this.isBeingDragged) {
            return true;
        }
        switch (ev.getActionMasked()) {
            case 0:
                this.isBeingDragged = false;
                int x = (int) ev.getX();
                int y = (int) ev.getY();
                if (canDragView(child) && parent.isPointInChildBounds(child, x, y)) {
                    this.lastMotionY = y;
                    this.activePointerId = ev.getPointerId(0);
                    ensureVelocityTracker();
                }
                break;
            case 1:
            case Generator.STATE_GENERATED_FAIL:
                this.isBeingDragged = false;
                this.activePointerId = -1;
                if (this.velocityTracker != null) {
                    this.velocityTracker.recycle();
                    this.velocityTracker = null;
                }
                break;
            case 2:
                int activePointerId = this.activePointerId;
                if (activePointerId != -1 && (pointerIndex = ev.findPointerIndex(activePointerId)) != -1) {
                    int y2 = (int) ev.getY(pointerIndex);
                    int yDiff = Math.abs(y2 - this.lastMotionY);
                    if (yDiff > this.touchSlop) {
                        this.isBeingDragged = true;
                        this.lastMotionY = y2;
                    }
                }
                break;
        }
        if (this.velocityTracker != null) {
            this.velocityTracker.addMovement(ev);
        }
        return this.isBeingDragged;
    }

    @Override
    public boolean onTouchEvent(CoordinatorLayout parent, V child, MotionEvent ev) {
        if (this.touchSlop < 0) {
            this.touchSlop = ViewConfiguration.get(parent.getContext()).getScaledTouchSlop();
        }
        switch (ev.getActionMasked()) {
            case 0:
                int x = (int) ev.getX();
                int y = (int) ev.getY();
                if (parent.isPointInChildBounds(child, x, y) && canDragView(child)) {
                    this.lastMotionY = y;
                    this.activePointerId = ev.getPointerId(0);
                    ensureVelocityTracker();
                    if (this.velocityTracker != null) {
                        this.velocityTracker.addMovement(ev);
                    }
                    break;
                }
                break;
            case 1:
                if (this.velocityTracker != null) {
                    this.velocityTracker.addMovement(ev);
                    this.velocityTracker.computeCurrentVelocity(1000);
                    float yvel = this.velocityTracker.getYVelocity(this.activePointerId);
                    fling(parent, child, -getScrollRangeForDragFling(child), 0, yvel);
                }
                this.isBeingDragged = false;
                this.activePointerId = -1;
                if (this.velocityTracker != null) {
                    this.velocityTracker.recycle();
                    this.velocityTracker = null;
                }
                if (this.velocityTracker != null) {
                }
                break;
            case 2:
                int activePointerIndex = ev.findPointerIndex(this.activePointerId);
                if (activePointerIndex != -1) {
                    int y2 = (int) ev.getY(activePointerIndex);
                    int dy = this.lastMotionY - y2;
                    if (!this.isBeingDragged && Math.abs(dy) > this.touchSlop) {
                        this.isBeingDragged = true;
                        dy = dy > 0 ? dy - this.touchSlop : dy + this.touchSlop;
                    }
                    if (this.isBeingDragged) {
                        this.lastMotionY = y2;
                        scroll(parent, child, dy, getMaxDragOffset(child), 0);
                    }
                    if (this.velocityTracker != null) {
                    }
                }
                break;
            case Generator.STATE_GENERATED_FAIL:
                this.isBeingDragged = false;
                this.activePointerId = -1;
                if (this.velocityTracker != null) {
                }
                if (this.velocityTracker != null) {
                }
                break;
            default:
                if (this.velocityTracker != null) {
                }
                break;
        }
        return false;
    }

    int setHeaderTopBottomOffset(CoordinatorLayout parent, V header, int newOffset) {
        return setHeaderTopBottomOffset(parent, header, newOffset, Integer.MIN_VALUE, Integer.MAX_VALUE);
    }

    int setHeaderTopBottomOffset(CoordinatorLayout parent, V header, int newOffset, int minOffset, int maxOffset) {
        int newOffset2;
        int curOffset = getTopAndBottomOffset();
        if (minOffset == 0 || curOffset < minOffset || curOffset > maxOffset || curOffset == (newOffset2 = MathUtils.constrain(newOffset, minOffset, maxOffset))) {
            return 0;
        }
        setTopAndBottomOffset(newOffset2);
        int consumed = curOffset - newOffset2;
        return consumed;
    }

    int getTopBottomOffsetForScrollingSibling() {
        return getTopAndBottomOffset();
    }

    final int scroll(CoordinatorLayout coordinatorLayout, V header, int dy, int minOffset, int maxOffset) {
        return setHeaderTopBottomOffset(coordinatorLayout, header, getTopBottomOffsetForScrollingSibling() - dy, minOffset, maxOffset);
    }

    final boolean fling(CoordinatorLayout coordinatorLayout, V layout, int minOffset, int maxOffset, float velocityY) {
        if (this.flingRunnable != null) {
            layout.removeCallbacks(this.flingRunnable);
            this.flingRunnable = null;
        }
        if (this.scroller == null) {
            this.scroller = new OverScroller(layout.getContext());
        }
        this.scroller.fling(0, getTopAndBottomOffset(), 0, Math.round(velocityY), 0, 0, minOffset, maxOffset);
        if (this.scroller.computeScrollOffset()) {
            this.flingRunnable = new FlingRunnable(coordinatorLayout, layout);
            ViewCompat.postOnAnimation(layout, this.flingRunnable);
            return true;
        }
        onFlingFinished(coordinatorLayout, layout);
        return false;
    }

    void onFlingFinished(CoordinatorLayout parent, V layout) {
    }

    boolean canDragView(V view) {
        return false;
    }

    int getMaxDragOffset(V view) {
        return -view.getHeight();
    }

    int getScrollRangeForDragFling(V view) {
        return view.getHeight();
    }

    private void ensureVelocityTracker() {
        if (this.velocityTracker == null) {
            this.velocityTracker = VelocityTracker.obtain();
        }
    }

    private class FlingRunnable implements Runnable {
        private final V layout;
        private final CoordinatorLayout parent;

        FlingRunnable(CoordinatorLayout parent, V layout) {
            this.parent = parent;
            this.layout = layout;
        }

        @Override
        public void run() {
            if (this.layout != null && HeaderBehavior.this.scroller != null) {
                if (HeaderBehavior.this.scroller.computeScrollOffset()) {
                    HeaderBehavior.this.setHeaderTopBottomOffset(this.parent, this.layout, HeaderBehavior.this.scroller.getCurrY());
                    ViewCompat.postOnAnimation(this.layout, this);
                } else {
                    HeaderBehavior.this.onFlingFinished(this.parent, this.layout);
                }
            }
        }
    }
}
