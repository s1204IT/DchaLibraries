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

/* loaded from: classes.dex */
public class AutoSizingList extends LinearLayout {
    private ListAdapter mAdapter;
    private final Runnable mBindChildren;
    private int mCount;
    private final DataSetObserver mDataObserver;
    private boolean mEnableAutoSizing;
    private final Handler mHandler;
    private final int mItemSize;

    public AutoSizingList(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        this.mBindChildren = new Runnable() { // from class: com.android.systemui.qs.AutoSizingList.1
            @Override // java.lang.Runnable
            public void run() {
                AutoSizingList.this.rebindChildren();
            }
        };
        this.mDataObserver = new DataSetObserver() { // from class: com.android.systemui.qs.AutoSizingList.2
            @Override // android.database.DataSetObserver
            public void onChanged() {
                if (AutoSizingList.this.mCount > AutoSizingList.this.getDesiredCount()) {
                    AutoSizingList.this.mCount = AutoSizingList.this.getDesiredCount();
                }
                AutoSizingList.this.postRebindChildren();
            }

            @Override // android.database.DataSetObserver
            public void onInvalidated() {
                AutoSizingList.this.postRebindChildren();
            }
        };
        this.mHandler = new Handler();
        TypedArray typedArrayObtainStyledAttributes = context.obtainStyledAttributes(attributeSet, R.styleable.AutoSizingList);
        this.mItemSize = typedArrayObtainStyledAttributes.getDimensionPixelSize(1, 0);
        this.mEnableAutoSizing = typedArrayObtainStyledAttributes.getBoolean(0, true);
        typedArrayObtainStyledAttributes.recycle();
    }

    public void setAdapter(ListAdapter listAdapter) {
        if (this.mAdapter != null) {
            this.mAdapter.unregisterDataSetObserver(this.mDataObserver);
        }
        this.mAdapter = listAdapter;
        if (listAdapter != null) {
            listAdapter.registerDataSetObserver(this.mDataObserver);
        }
    }

    @Override // android.widget.LinearLayout, android.view.View
    protected void onMeasure(int i, int i2) {
        int itemCount;
        int size = View.MeasureSpec.getSize(i2);
        if (size != 0 && this.mCount != (itemCount = getItemCount(size))) {
            postRebindChildren();
            this.mCount = itemCount;
        }
        super.onMeasure(i, i2);
    }

    private int getItemCount(int i) {
        int desiredCount = getDesiredCount();
        return this.mEnableAutoSizing ? Math.min(i / this.mItemSize, desiredCount) : desiredCount;
    }

    private int getDesiredCount() {
        if (this.mAdapter != null) {
            return this.mAdapter.getCount();
        }
        return 0;
    }

    private void postRebindChildren() {
        this.mHandler.post(this.mBindChildren);
    }

    private void rebindChildren() {
        if (this.mAdapter == null) {
            return;
        }
        int i = 0;
        while (i < this.mCount) {
            View childAt = i < getChildCount() ? getChildAt(i) : null;
            View view = this.mAdapter.getView(i, childAt, this);
            if (view != childAt) {
                if (childAt != null) {
                    removeView(childAt);
                }
                addView(view, i);
            }
            i++;
        }
        while (getChildCount() > this.mCount) {
            removeViewAt(getChildCount() - 1);
        }
    }
}
