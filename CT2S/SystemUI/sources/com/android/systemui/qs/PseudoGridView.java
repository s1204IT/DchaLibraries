package com.android.systemui.qs;

import android.content.Context;
import android.content.res.TypedArray;
import android.database.DataSetObserver;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import com.android.systemui.R;
import java.lang.ref.WeakReference;

public class PseudoGridView extends ViewGroup {
    private int mHorizontalSpacing;
    private int mNumColumns;
    private int mVerticalSpacing;

    public PseudoGridView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mNumColumns = 3;
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.PseudoGridView);
        int N = a.getIndexCount();
        for (int i = 0; i < N; i++) {
            int attr = a.getIndex(i);
            switch (attr) {
                case 0:
                    this.mNumColumns = a.getInt(attr, 3);
                    break;
                case 1:
                    this.mVerticalSpacing = a.getDimensionPixelSize(attr, 0);
                    break;
                case 2:
                    this.mHorizontalSpacing = a.getDimensionPixelSize(attr, 0);
                    break;
            }
        }
        a.recycle();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        if (View.MeasureSpec.getMode(widthMeasureSpec) == 0) {
            throw new UnsupportedOperationException("Needs a maximum width");
        }
        int width = View.MeasureSpec.getSize(widthMeasureSpec);
        int childWidth = (width - ((this.mNumColumns - 1) * this.mHorizontalSpacing)) / this.mNumColumns;
        int childWidthSpec = View.MeasureSpec.makeMeasureSpec(childWidth, 1073741824);
        int totalHeight = 0;
        int children = getChildCount();
        int rows = ((this.mNumColumns + children) - 1) / this.mNumColumns;
        for (int row = 0; row < rows; row++) {
            int startOfRow = row * this.mNumColumns;
            int endOfRow = Math.min(this.mNumColumns + startOfRow, children);
            int maxHeight = 0;
            for (int i = startOfRow; i < endOfRow; i++) {
                View child = getChildAt(i);
                child.measure(childWidthSpec, 0);
                maxHeight = Math.max(maxHeight, child.getMeasuredHeight());
            }
            int maxHeightSpec = View.MeasureSpec.makeMeasureSpec(maxHeight, 1073741824);
            for (int i2 = startOfRow; i2 < endOfRow; i2++) {
                View child2 = getChildAt(i2);
                if (child2.getMeasuredHeight() != maxHeight) {
                    child2.measure(childWidthSpec, maxHeightSpec);
                }
            }
            totalHeight += maxHeight;
            if (row > 0) {
                totalHeight += this.mVerticalSpacing;
            }
        }
        setMeasuredDimension(width, getDefaultSize(totalHeight, heightMeasureSpec));
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        boolean isRtl = isLayoutRtl();
        int children = getChildCount();
        int rows = ((this.mNumColumns + children) - 1) / this.mNumColumns;
        int y = 0;
        for (int row = 0; row < rows; row++) {
            int x = isRtl ? getWidth() : 0;
            int maxHeight = 0;
            int startOfRow = row * this.mNumColumns;
            int endOfRow = Math.min(this.mNumColumns + startOfRow, children);
            for (int i = startOfRow; i < endOfRow; i++) {
                View child = getChildAt(i);
                int width = child.getMeasuredWidth();
                int height = child.getMeasuredHeight();
                if (isRtl) {
                    x -= width;
                }
                child.layout(x, y, x + width, y + height);
                maxHeight = Math.max(maxHeight, height);
                if (isRtl) {
                    x -= this.mHorizontalSpacing;
                } else {
                    x += this.mHorizontalSpacing + width;
                }
            }
            y += maxHeight;
            if (row > 0) {
                y += this.mVerticalSpacing;
            }
        }
    }

    public static class ViewGroupAdapterBridge extends DataSetObserver {
        private final BaseAdapter mAdapter;
        private boolean mReleased = false;
        private final WeakReference<ViewGroup> mViewGroup;

        public static void link(ViewGroup viewGroup, BaseAdapter adapter) {
            new ViewGroupAdapterBridge(viewGroup, adapter);
        }

        private ViewGroupAdapterBridge(ViewGroup viewGroup, BaseAdapter adapter) {
            this.mViewGroup = new WeakReference<>(viewGroup);
            this.mAdapter = adapter;
            this.mAdapter.registerDataSetObserver(this);
            refresh();
        }

        private void refresh() {
            if (!this.mReleased) {
                ViewGroup viewGroup = this.mViewGroup.get();
                if (viewGroup == null) {
                    release();
                    return;
                }
                int childCount = viewGroup.getChildCount();
                int adapterCount = this.mAdapter.getCount();
                int N = Math.max(childCount, adapterCount);
                for (int i = 0; i < N; i++) {
                    if (i < adapterCount) {
                        View oldView = null;
                        if (i < childCount) {
                            oldView = viewGroup.getChildAt(i);
                        }
                        View newView = this.mAdapter.getView(i, oldView, viewGroup);
                        if (oldView == null) {
                            viewGroup.addView(newView);
                        } else if (oldView != newView) {
                            viewGroup.removeViewAt(i);
                            viewGroup.addView(newView, i);
                        }
                    } else {
                        int lastIndex = viewGroup.getChildCount() - 1;
                        viewGroup.removeViewAt(lastIndex);
                    }
                }
            }
        }

        @Override
        public void onChanged() {
            refresh();
        }

        @Override
        public void onInvalidated() {
            release();
        }

        private void release() {
            if (!this.mReleased) {
                this.mReleased = true;
                this.mAdapter.unregisterDataSetObserver(this);
            }
        }
    }
}
