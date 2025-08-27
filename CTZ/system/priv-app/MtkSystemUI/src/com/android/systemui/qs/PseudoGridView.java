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

/* loaded from: classes.dex */
public class PseudoGridView extends ViewGroup {
    private int mHorizontalSpacing;
    private int mNumColumns;
    private int mVerticalSpacing;

    public PseudoGridView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        this.mNumColumns = 3;
        TypedArray typedArrayObtainStyledAttributes = context.obtainStyledAttributes(attributeSet, R.styleable.PseudoGridView);
        int indexCount = typedArrayObtainStyledAttributes.getIndexCount();
        for (int i = 0; i < indexCount; i++) {
            int index = typedArrayObtainStyledAttributes.getIndex(i);
            switch (index) {
                case 0:
                    this.mHorizontalSpacing = typedArrayObtainStyledAttributes.getDimensionPixelSize(index, 0);
                    break;
                case 1:
                    this.mNumColumns = typedArrayObtainStyledAttributes.getInt(index, 3);
                    break;
                case 2:
                    this.mVerticalSpacing = typedArrayObtainStyledAttributes.getDimensionPixelSize(index, 0);
                    break;
            }
        }
        typedArrayObtainStyledAttributes.recycle();
    }

    @Override // android.view.View
    protected void onMeasure(int i, int i2) {
        if (View.MeasureSpec.getMode(i) == 0) {
            throw new UnsupportedOperationException("Needs a maximum width");
        }
        int size = View.MeasureSpec.getSize(i);
        int iMakeMeasureSpec = View.MeasureSpec.makeMeasureSpec((size - ((this.mNumColumns - 1) * this.mHorizontalSpacing)) / this.mNumColumns, 1073741824);
        int childCount = getChildCount();
        int i3 = ((this.mNumColumns + childCount) - 1) / this.mNumColumns;
        int i4 = 0;
        for (int i5 = 0; i5 < i3; i5++) {
            int i6 = this.mNumColumns * i5;
            int iMin = Math.min(this.mNumColumns + i6, childCount);
            int iMax = 0;
            for (int i7 = i6; i7 < iMin; i7++) {
                View childAt = getChildAt(i7);
                childAt.measure(iMakeMeasureSpec, 0);
                iMax = Math.max(iMax, childAt.getMeasuredHeight());
            }
            int iMakeMeasureSpec2 = View.MeasureSpec.makeMeasureSpec(iMax, 1073741824);
            while (i6 < iMin) {
                View childAt2 = getChildAt(i6);
                if (childAt2.getMeasuredHeight() != iMax) {
                    childAt2.measure(iMakeMeasureSpec, iMakeMeasureSpec2);
                }
                i6++;
            }
            i4 += iMax;
            if (i5 > 0) {
                i4 += this.mVerticalSpacing;
            }
        }
        setMeasuredDimension(size, resolveSizeAndState(i4, i2, 0));
    }

    @Override // android.view.ViewGroup, android.view.View
    protected void onLayout(boolean z, int i, int i2, int i3, int i4) {
        boolean zIsLayoutRtl = isLayoutRtl();
        int childCount = getChildCount();
        int i5 = ((this.mNumColumns + childCount) - 1) / this.mNumColumns;
        int i6 = 0;
        for (int i7 = 0; i7 < i5; i7++) {
            int width = zIsLayoutRtl ? getWidth() : 0;
            int i8 = this.mNumColumns * i7;
            int iMin = Math.min(this.mNumColumns + i8, childCount);
            int i9 = width;
            int iMax = 0;
            while (i8 < iMin) {
                View childAt = getChildAt(i8);
                int measuredWidth = childAt.getMeasuredWidth();
                int measuredHeight = childAt.getMeasuredHeight();
                if (zIsLayoutRtl) {
                    i9 -= measuredWidth;
                }
                childAt.layout(i9, i6, i9 + measuredWidth, i6 + measuredHeight);
                iMax = Math.max(iMax, measuredHeight);
                if (zIsLayoutRtl) {
                    i9 -= this.mHorizontalSpacing;
                } else {
                    i9 += measuredWidth + this.mHorizontalSpacing;
                }
                i8++;
            }
            i6 += iMax;
            if (i7 > 0) {
                i6 += this.mVerticalSpacing;
            }
        }
    }

    public static class ViewGroupAdapterBridge extends DataSetObserver {
        private final BaseAdapter mAdapter;
        private boolean mReleased = false;
        private final WeakReference<ViewGroup> mViewGroup;

        public static void link(ViewGroup viewGroup, BaseAdapter baseAdapter) {
            new ViewGroupAdapterBridge(viewGroup, baseAdapter);
        }

        private ViewGroupAdapterBridge(ViewGroup viewGroup, BaseAdapter baseAdapter) {
            this.mViewGroup = new WeakReference<>(viewGroup);
            this.mAdapter = baseAdapter;
            this.mAdapter.registerDataSetObserver(this);
            refresh();
        }

        private void refresh() {
            if (this.mReleased) {
                return;
            }
            ViewGroup viewGroup = this.mViewGroup.get();
            if (viewGroup == null) {
                release();
                return;
            }
            int childCount = viewGroup.getChildCount();
            int count = this.mAdapter.getCount();
            int iMax = Math.max(childCount, count);
            for (int i = 0; i < iMax; i++) {
                if (i < count) {
                    View childAt = null;
                    if (i < childCount) {
                        childAt = viewGroup.getChildAt(i);
                    }
                    View view = this.mAdapter.getView(i, childAt, viewGroup);
                    if (childAt == null) {
                        viewGroup.addView(view);
                    } else if (childAt != view) {
                        viewGroup.removeViewAt(i);
                        viewGroup.addView(view, i);
                    }
                } else {
                    viewGroup.removeViewAt(viewGroup.getChildCount() - 1);
                }
            }
        }

        @Override // android.database.DataSetObserver
        public void onChanged() {
            refresh();
        }

        @Override // android.database.DataSetObserver
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
