package com.android.music;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.ListView;

public class TouchInterceptor extends ListView {
    private Bitmap mDragBitmap;
    private DragListener mDragListener;
    private int mDragPointX;
    private int mDragPointY;
    private int mDragPos;
    private ImageView mDragView;
    private DropListener mDropListener;
    private GestureDetector mGestureDetector;
    private int mHeight;
    private int mItemHeightExpanded;
    private int mItemHeightHalf;
    private int mItemHeightNormal;
    private int mLowerBound;
    private RemoveListener mRemoveListener;
    private int mRemoveMode;
    private int mSrcDragPos;
    private Rect mTempRect;
    private final int mTouchSlop;
    private Drawable mTrashcan;
    private int mUpperBound;
    private WindowManager mWindowManager;
    private WindowManager.LayoutParams mWindowParams;
    private int mXOffset;
    private int mYOffset;

    public interface DragListener {
        void drag(int i, int i2);
    }

    public interface DropListener {
        void drop(int i, int i2);
    }

    public interface RemoveListener {
        void remove(int i);
    }

    public TouchInterceptor(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mRemoveMode = -1;
        this.mTempRect = new Rect();
        SharedPreferences pref = context.getSharedPreferences("Music", 3);
        this.mRemoveMode = pref.getInt("deletemode", -1);
        this.mTouchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
        Resources res = getResources();
        this.mItemHeightNormal = res.getDimensionPixelSize(R.dimen.normal_height);
        this.mItemHeightHalf = this.mItemHeightNormal / 2;
        this.mItemHeightExpanded = res.getDimensionPixelSize(R.dimen.expanded_height);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        if (this.mRemoveListener != null && this.mGestureDetector == null && this.mRemoveMode == 0) {
            this.mGestureDetector = new GestureDetector(getContext(), new GestureDetector.SimpleOnGestureListener() {
                @Override
                public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
                    if (TouchInterceptor.this.mDragView == null) {
                        return false;
                    }
                    if (velocityX <= 1000.0f) {
                        return true;
                    }
                    Rect r = TouchInterceptor.this.mTempRect;
                    TouchInterceptor.this.mDragView.getDrawingRect(r);
                    if (e2.getX() <= (r.right * 2) / 3) {
                        return true;
                    }
                    TouchInterceptor.this.stopDragging();
                    TouchInterceptor.this.mRemoveListener.remove(TouchInterceptor.this.mSrcDragPos);
                    TouchInterceptor.this.unExpandViews(true);
                    return true;
                }
            });
        }
        if (this.mDragListener != null || this.mDropListener != null) {
            switch (ev.getAction()) {
                case 0:
                    int x = (int) ev.getX();
                    int y = (int) ev.getY();
                    int itemnum = pointToPosition(x, y);
                    if (itemnum != -1) {
                        ViewGroup item = (ViewGroup) getChildAt(itemnum - getFirstVisiblePosition());
                        this.mDragPointX = x - item.getLeft();
                        this.mDragPointY = y - item.getTop();
                        this.mXOffset = ((int) ev.getRawX()) - x;
                        this.mYOffset = ((int) ev.getRawY()) - y;
                        if (x < 64) {
                            item.setDrawingCacheEnabled(true);
                            Bitmap bitmap = Bitmap.createBitmap(item.getDrawingCache());
                            startDragging(bitmap, x, y);
                            this.mDragPos = itemnum;
                            this.mSrcDragPos = this.mDragPos;
                            this.mHeight = getHeight();
                            int touchSlop = this.mTouchSlop;
                            this.mUpperBound = Math.min(y - touchSlop, this.mHeight / 3);
                            this.mLowerBound = Math.max(y + touchSlop, (this.mHeight * 2) / 3);
                            return false;
                        }
                        stopDragging();
                    }
                    break;
            }
        }
        return super.onInterceptTouchEvent(ev);
    }

    private int myPointToPosition(int x, int y) {
        int pos;
        if (y < 0 && (pos = myPointToPosition(x, this.mItemHeightNormal + y)) > 0) {
            return pos - 1;
        }
        Rect frame = this.mTempRect;
        int count = getChildCount();
        for (int i = count - 1; i >= 0; i--) {
            View child = getChildAt(i);
            child.getHitRect(frame);
            if (frame.contains(x, y)) {
                return getFirstVisiblePosition() + i;
            }
        }
        return -1;
    }

    private int getItemForPosition(int y) {
        int adjustedy = (y - this.mDragPointY) - this.mItemHeightHalf;
        int pos = myPointToPosition(0, adjustedy);
        if (pos >= 0) {
            if (pos <= this.mSrcDragPos) {
                return pos + 1;
            }
            return pos;
        }
        if (adjustedy < 0) {
            return 0;
        }
        return pos;
    }

    private void adjustScrollBounds(int y) {
        if (y >= this.mHeight / 3) {
            this.mUpperBound = this.mHeight / 3;
        }
        if (y <= (this.mHeight * 2) / 3) {
            this.mLowerBound = (this.mHeight * 2) / 3;
        }
    }

    private void unExpandViews(boolean deletion) {
        int i = 0;
        while (true) {
            View v = getChildAt(i);
            if (v == null) {
                if (deletion) {
                    int position = getFirstVisiblePosition();
                    int y = getChildAt(0).getTop();
                    setAdapter(getAdapter());
                    setSelectionFromTop(position, y);
                }
                try {
                    layoutChildren();
                    v = getChildAt(i);
                } catch (IllegalStateException e) {
                }
                if (v == null) {
                    return;
                }
            }
            ViewGroup.LayoutParams params = v.getLayoutParams();
            params.height = this.mItemHeightNormal;
            v.setLayoutParams(params);
            v.setVisibility(0);
            i++;
        }
    }

    private void doExpansion() {
        int childnum = this.mDragPos - getFirstVisiblePosition();
        if (this.mDragPos > this.mSrcDragPos) {
            childnum++;
        }
        int numheaders = getHeaderViewsCount();
        View first = getChildAt(this.mSrcDragPos - getFirstVisiblePosition());
        int i = 0;
        while (true) {
            View vv = getChildAt(i);
            if (vv != null) {
                int height = this.mItemHeightNormal;
                int visibility = 0;
                if (this.mDragPos < numheaders && i == numheaders) {
                    if (vv.equals(first)) {
                        visibility = 4;
                    } else {
                        height = this.mItemHeightExpanded;
                    }
                } else if (vv.equals(first)) {
                    if (this.mDragPos == this.mSrcDragPos || getPositionForView(vv) == getCount() - 1) {
                        visibility = 4;
                    } else {
                        height = 1;
                    }
                } else if (i == childnum && this.mDragPos >= numheaders && this.mDragPos < getCount() - 1) {
                    height = this.mItemHeightExpanded;
                }
                ViewGroup.LayoutParams params = vv.getLayoutParams();
                params.height = height;
                vv.setLayoutParams(params);
                vv.setVisibility(visibility);
                i++;
            } else {
                return;
            }
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        if (this.mGestureDetector != null) {
            this.mGestureDetector.onTouchEvent(ev);
        }
        if ((this.mDragListener != null || this.mDropListener != null) && this.mDragView != null) {
            int action = ev.getAction();
            switch (action) {
                case 0:
                case 2:
                    int x = (int) ev.getX();
                    int y = (int) ev.getY();
                    dragView(x, y);
                    int itemnum = getItemForPosition(y);
                    if (itemnum >= 0) {
                        if (action == 0 || itemnum != this.mDragPos) {
                            if (this.mDragListener != null) {
                                this.mDragListener.drag(this.mDragPos, itemnum);
                            }
                            this.mDragPos = itemnum;
                            doExpansion();
                        }
                        int speed = 0;
                        adjustScrollBounds(y);
                        if (y > this.mLowerBound) {
                            speed = getLastVisiblePosition() < getCount() + (-1) ? y > (this.mHeight + this.mLowerBound) / 2 ? 16 : 4 : 1;
                        } else if (y < this.mUpperBound) {
                            speed = y < this.mUpperBound / 2 ? -16 : -4;
                            if (getFirstVisiblePosition() == 0 && getChildAt(0).getTop() >= getPaddingTop()) {
                                speed = 0;
                            }
                        }
                        if (speed != 0) {
                            smoothScrollBy(speed, 30);
                        }
                    }
                    break;
                case 1:
                case 3:
                    Rect r = this.mTempRect;
                    this.mDragView.getDrawingRect(r);
                    stopDragging();
                    if (this.mRemoveMode == 1 && ev.getX() > (r.right * 3) / 4) {
                        if (this.mRemoveListener != null) {
                            this.mRemoveListener.remove(this.mSrcDragPos);
                        }
                        unExpandViews(true);
                    } else {
                        if (this.mDropListener != null && this.mDragPos >= 0 && this.mDragPos < getCount()) {
                            this.mDropListener.drop(this.mSrcDragPos, this.mDragPos);
                        }
                        unExpandViews(false);
                    }
                    break;
            }
            return true;
        }
        return super.onTouchEvent(ev);
    }

    private void startDragging(Bitmap bm, int x, int y) {
        stopDragging();
        this.mWindowParams = new WindowManager.LayoutParams();
        this.mWindowParams.gravity = 51;
        this.mWindowParams.x = (x - this.mDragPointX) + this.mXOffset;
        this.mWindowParams.y = (y - this.mDragPointY) + this.mYOffset;
        this.mWindowParams.height = -2;
        this.mWindowParams.width = -2;
        this.mWindowParams.flags = 920;
        this.mWindowParams.format = -3;
        this.mWindowParams.windowAnimations = 0;
        Context context = getContext();
        ImageView v = new ImageView(context);
        v.setBackgroundResource(R.drawable.playlist_tile_drag);
        v.setPadding(0, 0, 0, 0);
        v.setImageBitmap(bm);
        this.mDragBitmap = bm;
        this.mWindowManager = (WindowManager) context.getSystemService("window");
        this.mWindowManager.addView(v, this.mWindowParams);
        this.mDragView = v;
    }

    private void dragView(int x, int y) {
        if (this.mRemoveMode == 1) {
            float alpha = 1.0f;
            int width = this.mDragView.getWidth();
            if (x > width / 2) {
                alpha = (width - x) / (width / 2);
            }
            this.mWindowParams.alpha = alpha;
        }
        if (this.mRemoveMode == 0 || this.mRemoveMode == 2) {
            this.mWindowParams.x = (x - this.mDragPointX) + this.mXOffset;
        } else {
            this.mWindowParams.x = 0;
        }
        this.mWindowParams.y = (y - this.mDragPointY) + this.mYOffset;
        this.mWindowManager.updateViewLayout(this.mDragView, this.mWindowParams);
        if (this.mTrashcan != null) {
            int width2 = this.mDragView.getWidth();
            if (y > (getHeight() * 3) / 4) {
                this.mTrashcan.setLevel(2);
            } else if (width2 > 0 && x > width2 / 4) {
                this.mTrashcan.setLevel(1);
            } else {
                this.mTrashcan.setLevel(0);
            }
        }
    }

    private void stopDragging() {
        if (this.mDragView != null) {
            this.mDragView.setVisibility(8);
            WindowManager wm = (WindowManager) getContext().getSystemService("window");
            wm.removeView(this.mDragView);
            this.mDragView.setImageDrawable(null);
            this.mDragView = null;
        }
        if (this.mDragBitmap != null) {
            this.mDragBitmap.recycle();
            this.mDragBitmap = null;
        }
        if (this.mTrashcan != null) {
            this.mTrashcan.setLevel(0);
        }
    }

    public void setDropListener(DropListener l) {
        this.mDropListener = l;
    }

    public void setRemoveListener(RemoveListener l) {
        this.mRemoveListener = l;
    }
}
