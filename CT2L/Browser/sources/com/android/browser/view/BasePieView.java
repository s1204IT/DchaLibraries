package com.android.browser.view;

import android.database.DataSetObserver;
import android.graphics.Canvas;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Adapter;
import com.android.browser.view.PieMenu;
import java.util.ArrayList;

public abstract class BasePieView implements PieMenu.PieView {
    protected Adapter mAdapter;
    protected int mChildHeight;
    protected int mChildWidth;
    protected int mCurrent;
    protected int mHeight;
    protected int mLeft;
    protected PieMenu.PieView.OnLayoutListener mListener;
    private DataSetObserver mObserver;
    protected int mTop;
    protected ArrayList<View> mViews;
    protected int mWidth;

    protected abstract int findChildAt(int i);

    public void setLayoutListener(PieMenu.PieView.OnLayoutListener l) {
        this.mListener = l;
    }

    public void setAdapter(Adapter adapter) {
        this.mAdapter = adapter;
        if (adapter == null) {
            if (this.mAdapter != null) {
                this.mAdapter.unregisterDataSetObserver(this.mObserver);
            }
            this.mViews = null;
            this.mCurrent = -1;
            return;
        }
        this.mObserver = new DataSetObserver() {
            @Override
            public void onChanged() {
                BasePieView.this.buildViews();
            }

            @Override
            public void onInvalidated() {
                BasePieView.this.mViews.clear();
            }
        };
        this.mAdapter.registerDataSetObserver(this.mObserver);
        setCurrent(0);
    }

    public void setCurrent(int ix) {
        this.mCurrent = ix;
    }

    protected void buildViews() {
        if (this.mAdapter != null) {
            int n = this.mAdapter.getCount();
            if (this.mViews == null) {
                this.mViews = new ArrayList<>(n);
            } else {
                this.mViews.clear();
            }
            this.mChildWidth = 0;
            this.mChildHeight = 0;
            for (int i = 0; i < n; i++) {
                View view = this.mAdapter.getView(i, null, null);
                view.measure(0, 0);
                this.mChildWidth = Math.max(this.mChildWidth, view.getMeasuredWidth());
                this.mChildHeight = Math.max(this.mChildHeight, view.getMeasuredHeight());
                this.mViews.add(view);
            }
        }
    }

    @Override
    public void layout(int anchorX, int anchorY, boolean left, float angle, int parentHeight) {
        if (this.mListener != null) {
            this.mListener.onLayout(anchorX, anchorY, left);
        }
    }

    protected void drawView(View view, Canvas canvas) {
        int state = canvas.save();
        canvas.translate(view.getLeft(), view.getTop());
        view.draw(canvas);
        canvas.restoreToCount(state);
    }

    @Override
    public boolean onTouchEvent(MotionEvent evt) {
        int action = evt.getActionMasked();
        int evtx = (int) evt.getX();
        int evty = (int) evt.getY();
        if (evtx < this.mLeft || evtx >= this.mLeft + this.mWidth || evty < this.mTop || evty >= this.mTop + this.mHeight) {
            return false;
        }
        switch (action) {
            case 1:
                this.mViews.get(this.mCurrent).performClick();
                this.mViews.get(this.mCurrent).setPressed(false);
                break;
            case 2:
                View v = this.mViews.get(this.mCurrent);
                setCurrent(Math.max(0, Math.min(this.mViews.size() - 1, findChildAt(evty))));
                View v1 = this.mViews.get(this.mCurrent);
                if (v != v1) {
                    v.setPressed(false);
                    v1.setPressed(true);
                }
                break;
        }
        return true;
    }
}
