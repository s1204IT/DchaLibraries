package android.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.media.TtmlUtils;
import android.util.AttributeSet;
import android.util.SparseIntArray;
import android.view.Gravity;
import android.view.View;
import android.view.ViewDebug;
import android.view.ViewGroup;
import android.view.ViewHierarchyEncoder;
import android.widget.LinearLayout;
import com.android.internal.R;

public class TableRow extends LinearLayout {
    private ChildrenTracker mChildrenTracker;
    private SparseIntArray mColumnToChildIndex;
    private int[] mColumnWidths;
    private int[] mConstrainedColumnWidths;
    private int mNumColumns;

    public TableRow(Context context) {
        super(context);
        this.mNumColumns = 0;
        initTableRow();
    }

    public TableRow(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        this.mNumColumns = 0;
        initTableRow();
    }

    private void initTableRow() {
        ViewGroup.OnHierarchyChangeListener onHierarchyChangeListener = this.mOnHierarchyChangeListener;
        this.mChildrenTracker = new ChildrenTracker();
        if (onHierarchyChangeListener != null) {
            this.mChildrenTracker.setOnHierarchyChangeListener(onHierarchyChangeListener);
        }
        super.setOnHierarchyChangeListener(this.mChildrenTracker);
    }

    @Override
    public void setOnHierarchyChangeListener(ViewGroup.OnHierarchyChangeListener onHierarchyChangeListener) {
        this.mChildrenTracker.setOnHierarchyChangeListener(onHierarchyChangeListener);
    }

    void setColumnCollapsed(int i, boolean z) {
        View virtualChildAt = getVirtualChildAt(i);
        if (virtualChildAt != null) {
            virtualChildAt.setVisibility(z ? 8 : 0);
        }
    }

    @Override
    protected void onMeasure(int i, int i2) {
        measureHorizontal(i, i2);
    }

    @Override
    protected void onLayout(boolean z, int i, int i2, int i3, int i4) {
        layoutHorizontal(i, i2, i3, i4);
    }

    @Override
    public View getVirtualChildAt(int i) {
        if (this.mColumnToChildIndex == null) {
            mapIndexAndColumns();
        }
        int i2 = this.mColumnToChildIndex.get(i, -1);
        if (i2 != -1) {
            return getChildAt(i2);
        }
        return null;
    }

    @Override
    public int getVirtualChildCount() {
        if (this.mColumnToChildIndex == null) {
            mapIndexAndColumns();
        }
        return this.mNumColumns;
    }

    private void mapIndexAndColumns() {
        if (this.mColumnToChildIndex == null) {
            int childCount = getChildCount();
            this.mColumnToChildIndex = new SparseIntArray();
            SparseIntArray sparseIntArray = this.mColumnToChildIndex;
            int i = 0;
            int i2 = 0;
            while (i < childCount) {
                LayoutParams layoutParams = (LayoutParams) getChildAt(i).getLayoutParams();
                if (layoutParams.column >= i2) {
                    i2 = layoutParams.column;
                }
                int i3 = i2;
                int i4 = 0;
                while (i4 < layoutParams.span) {
                    sparseIntArray.put(i3, i);
                    i4++;
                    i3++;
                }
                i++;
                i2 = i3;
            }
            this.mNumColumns = i2;
        }
    }

    @Override
    int measureNullChild(int i) {
        return this.mConstrainedColumnWidths[i];
    }

    @Override
    void measureChildBeforeLayout(View view, int i, int i2, int i3, int i4, int i5) {
        if (this.mConstrainedColumnWidths != null) {
            LayoutParams layoutParams = (LayoutParams) view.getLayoutParams();
            int i6 = 1073741824;
            int i7 = layoutParams.span;
            int[] iArr = this.mConstrainedColumnWidths;
            int i8 = 0;
            for (int i9 = 0; i9 < i7; i9++) {
                i8 += iArr[i + i9];
            }
            int i10 = layoutParams.gravity;
            boolean zIsHorizontal = Gravity.isHorizontal(i10);
            if (zIsHorizontal) {
                i6 = Integer.MIN_VALUE;
            }
            view.measure(View.MeasureSpec.makeMeasureSpec(Math.max(0, (i8 - layoutParams.leftMargin) - layoutParams.rightMargin), i6), getChildMeasureSpec(i4, this.mPaddingTop + this.mPaddingBottom + layoutParams.topMargin + layoutParams.bottomMargin + i5, layoutParams.height));
            if (zIsHorizontal) {
                layoutParams.mOffset[1] = i8 - view.getMeasuredWidth();
                int absoluteGravity = Gravity.getAbsoluteGravity(i10, getLayoutDirection()) & 7;
                if (absoluteGravity == 1) {
                    layoutParams.mOffset[0] = layoutParams.mOffset[1] / 2;
                    return;
                } else {
                    if (absoluteGravity != 3 && absoluteGravity == 5) {
                        layoutParams.mOffset[0] = layoutParams.mOffset[1];
                        return;
                    }
                    return;
                }
            }
            int[] iArr2 = layoutParams.mOffset;
            layoutParams.mOffset[1] = 0;
            iArr2[0] = 0;
            return;
        }
        super.measureChildBeforeLayout(view, i, i2, i3, i4, i5);
    }

    @Override
    int getChildrenSkipCount(View view, int i) {
        return ((LayoutParams) view.getLayoutParams()).span - 1;
    }

    @Override
    int getLocationOffset(View view) {
        return ((LayoutParams) view.getLayoutParams()).mOffset[0];
    }

    @Override
    int getNextLocationOffset(View view) {
        return ((LayoutParams) view.getLayoutParams()).mOffset[1];
    }

    int[] getColumnsWidths(int i, int i2) {
        int childMeasureSpec;
        int virtualChildCount = getVirtualChildCount();
        if (this.mColumnWidths == null || virtualChildCount != this.mColumnWidths.length) {
            this.mColumnWidths = new int[virtualChildCount];
        }
        int[] iArr = this.mColumnWidths;
        for (int i3 = 0; i3 < virtualChildCount; i3++) {
            View virtualChildAt = getVirtualChildAt(i3);
            if (virtualChildAt != null && virtualChildAt.getVisibility() != 8) {
                LayoutParams layoutParams = (LayoutParams) virtualChildAt.getLayoutParams();
                if (layoutParams.span == 1) {
                    switch (layoutParams.width) {
                        case -2:
                            childMeasureSpec = getChildMeasureSpec(i, 0, -2);
                            break;
                        case -1:
                            childMeasureSpec = View.MeasureSpec.makeSafeMeasureSpec(View.MeasureSpec.getSize(i2), 0);
                            break;
                        default:
                            childMeasureSpec = View.MeasureSpec.makeMeasureSpec(layoutParams.width, 1073741824);
                            break;
                    }
                    virtualChildAt.measure(childMeasureSpec, childMeasureSpec);
                    iArr[i3] = virtualChildAt.getMeasuredWidth() + layoutParams.leftMargin + layoutParams.rightMargin;
                } else {
                    iArr[i3] = 0;
                }
            } else {
                iArr[i3] = 0;
            }
        }
        return iArr;
    }

    void setColumnsWidthConstraints(int[] iArr) {
        if (iArr == null || iArr.length < getVirtualChildCount()) {
            throw new IllegalArgumentException("columnWidths should be >= getVirtualChildCount()");
        }
        this.mConstrainedColumnWidths = iArr;
    }

    @Override
    public LayoutParams generateLayoutParams(AttributeSet attributeSet) {
        return new LayoutParams(getContext(), attributeSet);
    }

    @Override
    protected LinearLayout.LayoutParams generateDefaultLayoutParams() {
        return new LayoutParams();
    }

    @Override
    protected boolean checkLayoutParams(ViewGroup.LayoutParams layoutParams) {
        return layoutParams instanceof LayoutParams;
    }

    @Override
    protected LinearLayout.LayoutParams generateLayoutParams(ViewGroup.LayoutParams layoutParams) {
        return new LayoutParams(layoutParams);
    }

    @Override
    public CharSequence getAccessibilityClassName() {
        return TableRow.class.getName();
    }

    public static class LayoutParams extends LinearLayout.LayoutParams {
        private static final int LOCATION = 0;
        private static final int LOCATION_NEXT = 1;

        @ViewDebug.ExportedProperty(category = TtmlUtils.TAG_LAYOUT)
        public int column;
        private int[] mOffset;

        @ViewDebug.ExportedProperty(category = TtmlUtils.TAG_LAYOUT)
        public int span;

        public LayoutParams(Context context, AttributeSet attributeSet) {
            super(context, attributeSet);
            this.mOffset = new int[2];
            TypedArray typedArrayObtainStyledAttributes = context.obtainStyledAttributes(attributeSet, R.styleable.TableRow_Cell);
            this.column = typedArrayObtainStyledAttributes.getInt(0, -1);
            this.span = typedArrayObtainStyledAttributes.getInt(1, 1);
            if (this.span <= 1) {
                this.span = 1;
            }
            typedArrayObtainStyledAttributes.recycle();
        }

        public LayoutParams(int i, int i2) {
            super(i, i2);
            this.mOffset = new int[2];
            this.column = -1;
            this.span = 1;
        }

        public LayoutParams(int i, int i2, float f) {
            super(i, i2, f);
            this.mOffset = new int[2];
            this.column = -1;
            this.span = 1;
        }

        public LayoutParams() {
            super(-1, -2);
            this.mOffset = new int[2];
            this.column = -1;
            this.span = 1;
        }

        public LayoutParams(int i) {
            this();
            this.column = i;
        }

        public LayoutParams(ViewGroup.LayoutParams layoutParams) {
            super(layoutParams);
            this.mOffset = new int[2];
        }

        public LayoutParams(ViewGroup.MarginLayoutParams marginLayoutParams) {
            super(marginLayoutParams);
            this.mOffset = new int[2];
        }

        @Override
        protected void setBaseAttributes(TypedArray typedArray, int i, int i2) {
            if (typedArray.hasValue(i)) {
                this.width = typedArray.getLayoutDimension(i, "layout_width");
            } else {
                this.width = -1;
            }
            if (typedArray.hasValue(i2)) {
                this.height = typedArray.getLayoutDimension(i2, "layout_height");
            } else {
                this.height = -2;
            }
        }

        @Override
        protected void encodeProperties(ViewHierarchyEncoder viewHierarchyEncoder) {
            super.encodeProperties(viewHierarchyEncoder);
            viewHierarchyEncoder.addProperty("layout:column", this.column);
            viewHierarchyEncoder.addProperty("layout:span", this.span);
        }
    }

    private class ChildrenTracker implements ViewGroup.OnHierarchyChangeListener {
        private ViewGroup.OnHierarchyChangeListener listener;

        private ChildrenTracker() {
        }

        private void setOnHierarchyChangeListener(ViewGroup.OnHierarchyChangeListener onHierarchyChangeListener) {
            this.listener = onHierarchyChangeListener;
        }

        @Override
        public void onChildViewAdded(View view, View view2) {
            TableRow.this.mColumnToChildIndex = null;
            if (this.listener != null) {
                this.listener.onChildViewAdded(view, view2);
            }
        }

        @Override
        public void onChildViewRemoved(View view, View view2) {
            TableRow.this.mColumnToChildIndex = null;
            if (this.listener != null) {
                this.listener.onChildViewRemoved(view, view2);
            }
        }
    }
}
