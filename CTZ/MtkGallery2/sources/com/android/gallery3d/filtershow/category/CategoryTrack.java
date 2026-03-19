package com.android.gallery3d.filtershow.category;

import android.content.Context;
import android.database.DataSetObserver;
import android.util.AttributeSet;
import android.widget.LinearLayout;
import com.android.gallery3d.R;

public class CategoryTrack extends LinearLayout {
    private CategoryAdapter mAdapter;
    private DataSetObserver mDataSetObserver;
    private int mElemSize;

    public CategoryTrack(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
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
        this.mElemSize = getContext().obtainStyledAttributes(attributeSet, R.styleable.CategoryTrack).getDimensionPixelSize(0, 0);
    }

    public void setAdapter(CategoryAdapter categoryAdapter) {
        this.mAdapter = categoryAdapter;
        this.mAdapter.registerDataSetObserver(this.mDataSetObserver);
        fillContent();
    }

    public void fillContent() {
        removeAllViews();
        this.mAdapter.setItemWidth(this.mElemSize);
        this.mAdapter.setItemHeight(-1);
        int count = this.mAdapter.getCount();
        for (int i = 0; i < count; i++) {
            addView(this.mAdapter.getView(i, null, this), i);
        }
        requestLayout();
    }

    @Override
    public void invalidate() {
        for (int i = 0; i < getChildCount(); i++) {
            getChildAt(i).invalidate();
        }
    }
}
