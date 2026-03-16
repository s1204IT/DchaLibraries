package com.android.gallery3d.filtershow.category;

import android.content.Context;
import android.content.res.TypedArray;
import android.database.DataSetObserver;
import android.util.AttributeSet;
import android.view.View;
import android.widget.LinearLayout;
import com.android.gallery3d.R;

public class CategoryTrack extends LinearLayout {
    private CategoryAdapter mAdapter;
    private DataSetObserver mDataSetObserver;
    private int mElemSize;

    public CategoryTrack(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mDataSetObserver = new DataSetObserver() {
            @Override
            public void onChanged() {
                super.onChanged();
                if (CategoryTrack.this.getChildCount() != CategoryTrack.this.mAdapter.getCount()) {
                    CategoryTrack.this.fillContent();
                } else {
                    CategoryTrack.this.invalidate();
                }
            }

            @Override
            public void onInvalidated() {
                super.onInvalidated();
                CategoryTrack.this.fillContent();
            }
        };
        TypedArray a = getContext().obtainStyledAttributes(attrs, R.styleable.CategoryTrack);
        this.mElemSize = a.getDimensionPixelSize(0, 0);
    }

    public void setAdapter(CategoryAdapter adapter) {
        this.mAdapter = adapter;
        this.mAdapter.registerDataSetObserver(this.mDataSetObserver);
        fillContent();
    }

    public void fillContent() {
        removeAllViews();
        this.mAdapter.setItemWidth(this.mElemSize);
        this.mAdapter.setItemHeight(-1);
        int n = this.mAdapter.getCount();
        for (int i = 0; i < n; i++) {
            View view = this.mAdapter.getView(i, null, this);
            addView(view, i);
        }
        requestLayout();
    }

    @Override
    public void invalidate() {
        for (int i = 0; i < getChildCount(); i++) {
            View child = getChildAt(i);
            child.invalidate();
        }
    }
}
