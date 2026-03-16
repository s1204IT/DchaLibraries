package com.android.gallery3d.filtershow.state;

import android.animation.LayoutTransition;
import android.content.Context;
import android.content.res.TypedArray;
import android.database.DataSetObserver;
import android.graphics.Point;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Adapter;
import android.widget.LinearLayout;
import com.android.gallery3d.R;
import com.android.gallery3d.filtershow.FilterShowActivity;
import com.android.gallery3d.filtershow.filters.FilterRepresentation;
import com.android.gallery3d.filtershow.imageshow.MasterImage;

public class StatePanelTrack extends LinearLayout implements PanelTrack {
    private StateAdapter mAdapter;
    private StateView mCurrentSelectedView;
    private StateView mCurrentView;
    private float mDeleteSlope;
    private DragListener mDragListener;
    private int mElemEndSize;
    private int mElemHeight;
    private int mElemSize;
    private int mElemWidth;
    private int mEndElemHeight;
    private int mEndElemWidth;
    private boolean mExited;
    private GestureDetector mGestureDetector;
    private int mMaxTouchDelay;
    private DataSetObserver mObserver;
    private boolean mStartedDrag;
    private Point mTouchPoint;
    private long mTouchTime;

    public StatePanelTrack(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mExited = false;
        this.mStartedDrag = false;
        this.mDragListener = new DragListener(this);
        this.mDeleteSlope = 0.2f;
        this.mMaxTouchDelay = 300;
        this.mObserver = new DataSetObserver() {
            @Override
            public void onChanged() {
                super.onChanged();
                StatePanelTrack.this.fillContent(false);
            }

            @Override
            public void onInvalidated() {
                super.onInvalidated();
                StatePanelTrack.this.fillContent(false);
            }
        };
        TypedArray a = getContext().obtainStyledAttributes(attrs, R.styleable.StatePanelTrack);
        this.mElemSize = a.getDimensionPixelSize(0, 0);
        this.mElemEndSize = a.getDimensionPixelSize(1, 0);
        if (getOrientation() == 0) {
            this.mElemWidth = this.mElemSize;
            this.mElemHeight = -1;
            this.mEndElemWidth = this.mElemEndSize;
            this.mEndElemHeight = -1;
        } else {
            this.mElemWidth = -1;
            this.mElemHeight = this.mElemSize;
            this.mEndElemWidth = -1;
            this.mEndElemHeight = this.mElemEndSize;
        }
        GestureDetector.SimpleOnGestureListener simpleOnGestureListener = new GestureDetector.SimpleOnGestureListener() {
            @Override
            public void onLongPress(MotionEvent e) {
                StatePanelTrack.this.longPress(e);
            }

            @Override
            public boolean onDoubleTap(MotionEvent e) {
                StatePanelTrack.this.addDuplicate(e);
                return true;
            }
        };
        this.mGestureDetector = new GestureDetector(context, simpleOnGestureListener);
    }

    private void addDuplicate(MotionEvent e) {
    }

    private void longPress(MotionEvent e) {
    }

    public void setAdapter(StateAdapter adapter) {
        if (adapter != null) {
            this.mAdapter = adapter;
            this.mAdapter.registerDataSetObserver(this.mObserver);
            this.mAdapter.setOrientation(getOrientation());
            fillContent(false);
        }
        requestLayout();
    }

    public StateView findChildWithState(State state) {
        for (int i = 0; i < getChildCount(); i++) {
            StateView view = (StateView) getChildAt(i);
            if (view.getState() == state) {
                return view;
            }
        }
        return null;
    }

    @Override
    public void fillContent(boolean animate) {
        if (!animate) {
            setLayoutTransition(null);
        }
        int n = this.mAdapter.getCount();
        for (int i = 0; i < getChildCount(); i++) {
            StateView child = (StateView) getChildAt(i);
            child.resetPosition();
            if (!this.mAdapter.contains(child.getState())) {
                removeView(child);
            }
        }
        ViewGroup.LayoutParams params = new LinearLayout.LayoutParams(this.mElemWidth, this.mElemHeight);
        for (int i2 = 0; i2 < n; i2++) {
            State s = this.mAdapter.getItem(i2);
            if (findChildWithState(s) == null) {
                addView(this.mAdapter.getView(i2, null, this), i2, params);
            }
        }
        for (int i3 = 0; i3 < n; i3++) {
            State state = this.mAdapter.getItem(i3);
            StateView view = (StateView) getChildAt(i3);
            view.setState(state);
            if (i3 == 0) {
                view.setType(StateView.BEGIN);
            } else if (i3 == n - 1) {
                view.setType(StateView.END);
            } else {
                view.setType(StateView.DEFAULT);
            }
            view.resetPosition();
        }
        if (!animate) {
            setLayoutTransition(new LayoutTransition());
        }
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent event) {
        return this.mCurrentView != null;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (this.mCurrentView == null) {
            return false;
        }
        if (this.mTouchTime == 0) {
            this.mTouchTime = System.currentTimeMillis();
        }
        this.mGestureDetector.onTouchEvent(event);
        if (this.mTouchPoint == null) {
            this.mTouchPoint = new Point();
            this.mTouchPoint.x = (int) event.getX();
            this.mTouchPoint.y = (int) event.getY();
        }
        if (event.getActionMasked() == 2) {
            float translation = event.getY() - this.mTouchPoint.y;
            float alpha = 1.0f - (Math.abs(translation) / this.mCurrentView.getHeight());
            if (getOrientation() == 1) {
                float translation2 = event.getX() - this.mTouchPoint.x;
                alpha = 1.0f - (Math.abs(translation2) / this.mCurrentView.getWidth());
                this.mCurrentView.setTranslationX(translation2);
            } else {
                this.mCurrentView.setTranslationY(translation);
            }
            this.mCurrentView.setBackgroundAlpha(alpha);
        }
        if (!this.mExited && this.mCurrentView != null && this.mCurrentView.getBackgroundAlpha() > this.mDeleteSlope && event.getActionMasked() == 1 && System.currentTimeMillis() - this.mTouchTime < this.mMaxTouchDelay) {
            FilterRepresentation representation = this.mCurrentView.getState().getFilterRepresentation();
            this.mCurrentView.setSelected(true);
            if (representation != MasterImage.getImage().getCurrentFilterRepresentation()) {
                FilterShowActivity activity = (FilterShowActivity) getContext();
                activity.showRepresentation(representation);
                this.mCurrentView.setSelected(false);
            }
        }
        if (event.getActionMasked() == 1 || (!this.mStartedDrag && event.getActionMasked() == 3)) {
            checkEndState();
            if (this.mCurrentView != null && this.mCurrentView.getState().getFilterRepresentation().getEditorId() == R.id.imageOnlyEditor) {
                this.mCurrentView.setSelected(false);
            }
        }
        return true;
    }

    @Override
    public void checkEndState() {
        this.mTouchPoint = null;
        this.mTouchTime = 0L;
        if (this.mExited || this.mCurrentView.getBackgroundAlpha() < this.mDeleteSlope) {
            int origin = findChild(this.mCurrentView);
            if (origin != -1) {
                State current = this.mAdapter.getItem(origin);
                FilterRepresentation currentRep = MasterImage.getImage().getCurrentFilterRepresentation();
                FilterRepresentation removedRep = current.getFilterRepresentation();
                this.mAdapter.remove(current);
                fillContent(true);
                if (currentRep != null && removedRep != null && currentRep.getFilterClass() == removedRep.getFilterClass()) {
                    FilterShowActivity activity = (FilterShowActivity) getContext();
                    activity.backToMain();
                    return;
                }
            }
        } else {
            this.mCurrentView.setBackgroundAlpha(1.0f);
            this.mCurrentView.setTranslationX(0.0f);
            this.mCurrentView.setTranslationY(0.0f);
        }
        if (this.mCurrentSelectedView != null) {
            this.mCurrentSelectedView.invalidate();
        }
        if (this.mCurrentView != null) {
            this.mCurrentView.invalidate();
        }
        this.mCurrentView = null;
        this.mExited = false;
        this.mStartedDrag = false;
    }

    @Override
    public View findChildAt(int x, int y) {
        Rect frame = new Rect();
        int scrolledXInt = getScrollX() + x;
        int scrolledYInt = getScrollY() + y;
        for (int i = 0; i < getChildCount(); i++) {
            View child = getChildAt(i);
            child.getHitRect(frame);
            if (frame.contains(scrolledXInt, scrolledYInt)) {
                return child;
            }
        }
        return null;
    }

    @Override
    public int findChild(View view) {
        for (int i = 0; i < getChildCount(); i++) {
            View child = getChildAt(i);
            if (child == view) {
                return i;
            }
        }
        return -1;
    }

    @Override
    public StateView getCurrentView() {
        return this.mCurrentView;
    }

    @Override
    public void setCurrentView(View currentView) {
        this.mCurrentView = (StateView) currentView;
    }

    @Override
    public void setExited(boolean value) {
        this.mExited = value;
    }

    @Override
    public Point getTouchPoint() {
        return this.mTouchPoint;
    }

    @Override
    public Adapter getAdapter() {
        return this.mAdapter;
    }
}
