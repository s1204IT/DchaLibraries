package com.android.systemui.qs;

import android.content.Context;
import android.content.res.TypedArray;
import android.database.DataSetObserver;
import android.os.Handler;
import android.util.AttributeSet;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import com.android.systemui.R;

public class AutoSizingList extends LinearLayout {
    private ListAdapter mAdapter;
    private final Runnable mBindChildren;
    private int mCount;
    private final DataSetObserver mDataObserver;
    private final Handler mHandler;
    private final int mItemSize;

    public AutoSizingList(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mBindChildren = new Runnable() {
            @Override
            public void run() {
                AutoSizingList.this.rebindChildren();
            }
        };
        this.mDataObserver = new DataSetObserver() {
            @Override
            public void onChanged() {
                if (AutoSizingList.this.mCount > AutoSizingList.this.getDesiredCount()) {
                    AutoSizingList.this.mCount = AutoSizingList.this.getDesiredCount();
                }
                AutoSizingList.this.postRebindChildren();
            }

            @Override
            public void onInvalidated() {
                AutoSizingList.this.postRebindChildren();
            }
        };
        this.mHandler = new Handler();
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.AutoSizingList);
        this.mItemSize = a.getDimensionPixelSize(0, 0);
    }

    public void setAdapter(ListAdapter adapter) {
        if (this.mAdapter != null) {
            this.mAdapter.unregisterDataSetObserver(this.mDataObserver);
        }
        this.mAdapter = adapter;
        if (adapter == null) {
            return;
        }
        adapter.registerDataSetObserver(this.mDataObserver);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int count;
        int requestedHeight = View.MeasureSpec.getSize(heightMeasureSpec);
        if (requestedHeight != 0 && this.mCount != (count = Math.min(requestedHeight / this.mItemSize, getDesiredCount()))) {
            postRebindChildren();
            this.mCount = count;
        }
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    public int getDesiredCount() {
        if (this.mAdapter != null) {
            return this.mAdapter.getCount();
        }
        return 0;
    }

    public void postRebindChildren() {
        this.mHandler.post(this.mBindChildren);
    }

    public void rebindChildren() {
        if (this.mAdapter == null) {
            return;
        }
        int i = 0;
        while (i < this.mCount) {
            View childAt = i < getChildCount() ? getChildAt(i) : null;
            View newView = this.mAdapter.getView(i, childAt, this);
            if (newView != childAt) {
                if (childAt != null) {
                    removeView(childAt);
                }
                addView(newView, i);
            }
            i++;
        }
        while (getChildCount() > this.mCount) {
            removeViewAt(getChildCount() - 1);
        }
    }
}
