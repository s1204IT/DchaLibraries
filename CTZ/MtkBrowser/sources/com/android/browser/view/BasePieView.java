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

    public void setLayoutListener(PieMenu.PieView.OnLayoutListener onLayoutListener) {
        this.mListener = onLayoutListener;
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

    public void setCurrent(int i) {
        this.mCurrent = i;
    }

    protected void buildViews() {
        if (this.mAdapter != null) {
            int count = this.mAdapter.getCount();
            if (this.mViews == null) {
                this.mViews = new ArrayList<>(count);
            } else {
                this.mViews.clear();
            }
            this.mChildWidth = 0;
            this.mChildHeight = 0;
            for (int i = 0; i < count; i++) {
                View view = this.mAdapter.getView(i, null, null);
                view.measure(0, 0);
                this.mChildWidth = Math.max(this.mChildWidth, view.getMeasuredWidth());
                this.mChildHeight = Math.max(this.mChildHeight, view.getMeasuredHeight());
                this.mViews.add(view);
            }
        }
    }

    @Override
    public void layout(int i, int i2, boolean z, float f, int i3) {
        if (this.mListener != null) {
            this.mListener.onLayout(i, i2, z);
        }
    }

    protected void drawView(View view, Canvas canvas) {
        int iSave = canvas.save();
        canvas.translate(view.getLeft(), view.getTop());
        view.draw(canvas);
        canvas.restoreToCount(iSave);
    }

    @Override
    public boolean onTouchEvent(MotionEvent motionEvent) {
        int actionMasked = motionEvent.getActionMasked();
        int x = (int) motionEvent.getX();
        int y = (int) motionEvent.getY();
        if (x < this.mLeft || x >= this.mLeft + this.mWidth || y < this.mTop || y >= this.mTop + this.mHeight) {
            return false;
        }
        switch (actionMasked) {
            case 1:
                this.mViews.get(this.mCurrent).performClick();
                this.mViews.get(this.mCurrent).setPressed(false);
                return true;
            case 2:
                View view = this.mViews.get(this.mCurrent);
                setCurrent(Math.max(0, Math.min(this.mViews.size() - 1, findChildAt(y))));
                View view2 = this.mViews.get(this.mCurrent);
                if (view != view2) {
                    view.setPressed(false);
                    view2.setPressed(true);
                }
                return true;
            default:
                return true;
        }
    }
}
