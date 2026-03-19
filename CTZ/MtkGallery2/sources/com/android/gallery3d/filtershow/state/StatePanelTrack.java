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

    public StatePanelTrack(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
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
        TypedArray typedArrayObtainStyledAttributes = getContext().obtainStyledAttributes(attributeSet, R.styleable.StatePanelTrack);
        this.mElemSize = typedArrayObtainStyledAttributes.getDimensionPixelSize(0, 0);
        this.mElemEndSize = typedArrayObtainStyledAttributes.getDimensionPixelSize(1, 0);
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
        this.mGestureDetector = new GestureDetector(context, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public void onLongPress(MotionEvent motionEvent) {
                StatePanelTrack.this.longPress(motionEvent);
            }

            @Override
            public boolean onDoubleTap(MotionEvent motionEvent) {
                StatePanelTrack.this.addDuplicate(motionEvent);
                return true;
            }
        });
    }

    private void addDuplicate(MotionEvent motionEvent) {
    }

    private void longPress(MotionEvent motionEvent) {
    }

    public void setAdapter(StateAdapter stateAdapter) {
        if (stateAdapter == null) {
            return;
        }
        this.mAdapter = stateAdapter;
        this.mAdapter.registerDataSetObserver(this.mObserver);
        this.mAdapter.setOrientation(getOrientation());
        fillContent(false);
        requestLayout();
    }

    public StateView findChildWithState(State state) {
        for (int i = 0; i < getChildCount(); i++) {
            StateView stateView = (StateView) getChildAt(i);
            if (stateView.getState() == state) {
                return stateView;
            }
        }
        return null;
    }

    @Override
    public void fillContent(boolean z) {
        if (!z) {
            setLayoutTransition(null);
        }
        int count = this.mAdapter.getCount();
        for (int i = 0; i < getChildCount(); i++) {
            StateView stateView = (StateView) getChildAt(i);
            stateView.resetPosition();
            if (!this.mAdapter.contains(stateView.getState())) {
                removeView(stateView);
            }
        }
        ViewGroup.LayoutParams layoutParams = new LinearLayout.LayoutParams(this.mElemWidth, this.mElemHeight);
        for (int i2 = 0; i2 < count; i2++) {
            if (findChildWithState(this.mAdapter.getItem(i2)) == null) {
                addView(this.mAdapter.getView(i2, null, this), i2, layoutParams);
            }
        }
        for (int i3 = 0; i3 < count; i3++) {
            State item = this.mAdapter.getItem(i3);
            StateView stateView2 = (StateView) getChildAt(i3);
            stateView2.setState(item);
            if (i3 == 0) {
                stateView2.setType(StateView.BEGIN);
            } else if (i3 == count - 1) {
                stateView2.setType(StateView.END);
            } else {
                stateView2.setType(StateView.DEFAULT);
            }
            stateView2.resetPosition();
        }
        if (!z) {
            setLayoutTransition(new LayoutTransition());
        }
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent motionEvent) {
        if (this.mCurrentView != null) {
            return true;
        }
        return false;
    }

    @Override
    public boolean onTouchEvent(MotionEvent motionEvent) {
        if (this.mCurrentView == null) {
            return false;
        }
        if (this.mTouchTime == 0) {
            this.mTouchTime = System.currentTimeMillis();
        }
        this.mGestureDetector.onTouchEvent(motionEvent);
        if (this.mTouchPoint == null) {
            this.mTouchPoint = new Point();
            this.mTouchPoint.x = (int) motionEvent.getX();
            this.mTouchPoint.y = (int) motionEvent.getY();
        }
        if (motionEvent.getActionMasked() == 2) {
            float y = motionEvent.getY() - this.mTouchPoint.y;
            float fAbs = 1.0f - (Math.abs(y) / this.mCurrentView.getHeight());
            if (getOrientation() == 1) {
                float x = motionEvent.getX() - this.mTouchPoint.x;
                fAbs = 1.0f - (Math.abs(x) / this.mCurrentView.getWidth());
                this.mCurrentView.setTranslationX(x);
            } else {
                this.mCurrentView.setTranslationY(y);
            }
            this.mCurrentView.setBackgroundAlpha(fAbs);
        }
        if (!this.mExited && this.mCurrentView != null && this.mCurrentView.getBackgroundAlpha() > this.mDeleteSlope && motionEvent.getActionMasked() == 1 && System.currentTimeMillis() - this.mTouchTime < this.mMaxTouchDelay) {
            FilterRepresentation filterRepresentation = this.mCurrentView.getState().getFilterRepresentation();
            this.mCurrentView.setSelected(true);
            if (filterRepresentation != MasterImage.getImage().getCurrentFilterRepresentation()) {
                ((FilterShowActivity) getContext()).showRepresentation(filterRepresentation);
                this.mCurrentView.setSelected(false);
            }
        }
        if (motionEvent.getActionMasked() == 1 || (!this.mStartedDrag && motionEvent.getActionMasked() == 3)) {
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
            int iFindChild = findChild(this.mCurrentView);
            if (iFindChild != -1) {
                State item = this.mAdapter.getItem(iFindChild);
                FilterRepresentation currentFilterRepresentation = MasterImage.getImage().getCurrentFilterRepresentation();
                FilterRepresentation filterRepresentation = item.getFilterRepresentation();
                this.mAdapter.remove(item);
                fillContent(true);
                if (currentFilterRepresentation != null && filterRepresentation != null && currentFilterRepresentation.getFilterClass() == filterRepresentation.getFilterClass()) {
                    ((FilterShowActivity) getContext()).backToMain();
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
    public View findChildAt(int i, int i2) {
        Rect rect = new Rect();
        int scrollX = getScrollX() + i;
        int scrollY = getScrollY() + i2;
        for (int i3 = 0; i3 < getChildCount(); i3++) {
            View childAt = getChildAt(i3);
            childAt.getHitRect(rect);
            if (rect.contains(scrollX, scrollY)) {
                return childAt;
            }
        }
        return null;
    }

    @Override
    public int findChild(View view) {
        for (int i = 0; i < getChildCount(); i++) {
            if (getChildAt(i) == view) {
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
    public void setCurrentView(View view) {
        this.mCurrentView = (StateView) view;
    }

    @Override
    public void setExited(boolean z) {
        this.mExited = z;
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
