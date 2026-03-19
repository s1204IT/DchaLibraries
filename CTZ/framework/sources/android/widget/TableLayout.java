package android.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.util.SparseBooleanArray;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import com.android.internal.R;
import java.util.regex.Pattern;

public class TableLayout extends LinearLayout {
    private SparseBooleanArray mCollapsedColumns;
    private boolean mInitialized;
    private int[] mMaxWidths;
    private PassThroughHierarchyChangeListener mPassThroughListener;
    private boolean mShrinkAllColumns;
    private SparseBooleanArray mShrinkableColumns;
    private boolean mStretchAllColumns;
    private SparseBooleanArray mStretchableColumns;

    public TableLayout(Context context) {
        super(context);
        initTableLayout();
    }

    public TableLayout(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        TypedArray typedArrayObtainStyledAttributes = context.obtainStyledAttributes(attributeSet, R.styleable.TableLayout);
        String string = typedArrayObtainStyledAttributes.getString(0);
        if (string != null) {
            if (string.charAt(0) == '*') {
                this.mStretchAllColumns = true;
            } else {
                this.mStretchableColumns = parseColumns(string);
            }
        }
        String string2 = typedArrayObtainStyledAttributes.getString(1);
        if (string2 != null) {
            if (string2.charAt(0) == '*') {
                this.mShrinkAllColumns = true;
            } else {
                this.mShrinkableColumns = parseColumns(string2);
            }
        }
        String string3 = typedArrayObtainStyledAttributes.getString(2);
        if (string3 != null) {
            this.mCollapsedColumns = parseColumns(string3);
        }
        typedArrayObtainStyledAttributes.recycle();
        initTableLayout();
    }

    private static SparseBooleanArray parseColumns(String str) {
        SparseBooleanArray sparseBooleanArray = new SparseBooleanArray();
        for (String str2 : Pattern.compile("\\s*,\\s*").split(str)) {
            try {
                int i = Integer.parseInt(str2);
                if (i >= 0) {
                    sparseBooleanArray.put(i, true);
                }
            } catch (NumberFormatException e) {
            }
        }
        return sparseBooleanArray;
    }

    private void initTableLayout() {
        if (this.mCollapsedColumns == null) {
            this.mCollapsedColumns = new SparseBooleanArray();
        }
        if (this.mStretchableColumns == null) {
            this.mStretchableColumns = new SparseBooleanArray();
        }
        if (this.mShrinkableColumns == null) {
            this.mShrinkableColumns = new SparseBooleanArray();
        }
        setOrientation(1);
        this.mPassThroughListener = new PassThroughHierarchyChangeListener();
        super.setOnHierarchyChangeListener(this.mPassThroughListener);
        this.mInitialized = true;
    }

    @Override
    public void setOnHierarchyChangeListener(ViewGroup.OnHierarchyChangeListener onHierarchyChangeListener) {
        this.mPassThroughListener.mOnHierarchyChangeListener = onHierarchyChangeListener;
    }

    private void requestRowsLayout() {
        if (this.mInitialized) {
            int childCount = getChildCount();
            for (int i = 0; i < childCount; i++) {
                getChildAt(i).requestLayout();
            }
        }
    }

    @Override
    public void requestLayout() {
        if (this.mInitialized) {
            int childCount = getChildCount();
            for (int i = 0; i < childCount; i++) {
                getChildAt(i).forceLayout();
            }
        }
        super.requestLayout();
    }

    public boolean isShrinkAllColumns() {
        return this.mShrinkAllColumns;
    }

    public void setShrinkAllColumns(boolean z) {
        this.mShrinkAllColumns = z;
    }

    public boolean isStretchAllColumns() {
        return this.mStretchAllColumns;
    }

    public void setStretchAllColumns(boolean z) {
        this.mStretchAllColumns = z;
    }

    public void setColumnCollapsed(int i, boolean z) {
        this.mCollapsedColumns.put(i, z);
        int childCount = getChildCount();
        for (int i2 = 0; i2 < childCount; i2++) {
            View childAt = getChildAt(i2);
            if (childAt instanceof TableRow) {
                ((TableRow) childAt).setColumnCollapsed(i, z);
            }
        }
        requestRowsLayout();
    }

    public boolean isColumnCollapsed(int i) {
        return this.mCollapsedColumns.get(i);
    }

    public void setColumnStretchable(int i, boolean z) {
        this.mStretchableColumns.put(i, z);
        requestRowsLayout();
    }

    public boolean isColumnStretchable(int i) {
        return this.mStretchAllColumns || this.mStretchableColumns.get(i);
    }

    public void setColumnShrinkable(int i, boolean z) {
        this.mShrinkableColumns.put(i, z);
        requestRowsLayout();
    }

    public boolean isColumnShrinkable(int i) {
        return this.mShrinkAllColumns || this.mShrinkableColumns.get(i);
    }

    private void trackCollapsedColumns(View view) {
        if (view instanceof TableRow) {
            TableRow tableRow = (TableRow) view;
            SparseBooleanArray sparseBooleanArray = this.mCollapsedColumns;
            int size = sparseBooleanArray.size();
            for (int i = 0; i < size; i++) {
                int iKeyAt = sparseBooleanArray.keyAt(i);
                boolean zValueAt = sparseBooleanArray.valueAt(i);
                if (zValueAt) {
                    tableRow.setColumnCollapsed(iKeyAt, zValueAt);
                }
            }
        }
    }

    @Override
    public void addView(View view) {
        super.addView(view);
        requestRowsLayout();
    }

    @Override
    public void addView(View view, int i) {
        super.addView(view, i);
        requestRowsLayout();
    }

    @Override
    public void addView(View view, ViewGroup.LayoutParams layoutParams) {
        super.addView(view, layoutParams);
        requestRowsLayout();
    }

    @Override
    public void addView(View view, int i, ViewGroup.LayoutParams layoutParams) {
        super.addView(view, i, layoutParams);
        requestRowsLayout();
    }

    @Override
    protected void onMeasure(int i, int i2) {
        measureVertical(i, i2);
    }

    @Override
    protected void onLayout(boolean z, int i, int i2, int i3, int i4) {
        layoutVertical(i, i2, i3, i4);
    }

    @Override
    void measureChildBeforeLayout(View view, int i, int i2, int i3, int i4, int i5) {
        if (view instanceof TableRow) {
            ((TableRow) view).setColumnsWidthConstraints(this.mMaxWidths);
        }
        super.measureChildBeforeLayout(view, i, i2, i3, i4, i5);
    }

    @Override
    void measureVertical(int i, int i2) {
        findLargestCells(i, i2);
        shrinkAndStretchColumns(i);
        super.measureVertical(i, i2);
    }

    private void findLargestCells(int i, int i2) {
        int childCount = getChildCount();
        boolean z = true;
        for (int i3 = 0; i3 < childCount; i3++) {
            View childAt = getChildAt(i3);
            if (childAt.getVisibility() != 8 && (childAt instanceof TableRow)) {
                TableRow tableRow = (TableRow) childAt;
                tableRow.getLayoutParams().height = -2;
                int[] columnsWidths = tableRow.getColumnsWidths(i, i2);
                int length = columnsWidths.length;
                if (z) {
                    if (this.mMaxWidths == null || this.mMaxWidths.length != length) {
                        this.mMaxWidths = new int[length];
                    }
                    System.arraycopy(columnsWidths, 0, this.mMaxWidths, 0, length);
                    z = false;
                } else {
                    int length2 = this.mMaxWidths.length;
                    int i4 = length - length2;
                    if (i4 > 0) {
                        int[] iArr = this.mMaxWidths;
                        this.mMaxWidths = new int[length];
                        System.arraycopy(iArr, 0, this.mMaxWidths, 0, iArr.length);
                        System.arraycopy(columnsWidths, iArr.length, this.mMaxWidths, iArr.length, i4);
                    }
                    int[] iArr2 = this.mMaxWidths;
                    int iMin = Math.min(length2, length);
                    for (int i5 = 0; i5 < iMin; i5++) {
                        iArr2[i5] = Math.max(iArr2[i5], columnsWidths[i5]);
                    }
                }
            }
        }
    }

    private void shrinkAndStretchColumns(int i) {
        if (this.mMaxWidths == null) {
            return;
        }
        int i2 = 0;
        for (int i3 : this.mMaxWidths) {
            i2 += i3;
        }
        int size = (View.MeasureSpec.getSize(i) - this.mPaddingLeft) - this.mPaddingRight;
        if (i2 > size && (this.mShrinkAllColumns || this.mShrinkableColumns.size() > 0)) {
            mutateColumnsWidth(this.mShrinkableColumns, this.mShrinkAllColumns, size, i2);
        } else if (i2 < size) {
            if (this.mStretchAllColumns || this.mStretchableColumns.size() > 0) {
                mutateColumnsWidth(this.mStretchableColumns, this.mStretchAllColumns, size, i2);
            }
        }
    }

    private void mutateColumnsWidth(SparseBooleanArray sparseBooleanArray, boolean z, int i, int i2) {
        int size;
        int[] iArr = this.mMaxWidths;
        int length = iArr.length;
        if (!z) {
            size = sparseBooleanArray.size();
        } else {
            size = length;
        }
        int i3 = (i - i2) / size;
        int childCount = getChildCount();
        for (int i4 = 0; i4 < childCount; i4++) {
            View childAt = getChildAt(i4);
            if (childAt instanceof TableRow) {
                childAt.forceLayout();
            }
        }
        if (!z) {
            int i5 = 0;
            for (int i6 = 0; i6 < size; i6++) {
                int iKeyAt = sparseBooleanArray.keyAt(i6);
                if (sparseBooleanArray.valueAt(i6)) {
                    if (iKeyAt < length) {
                        iArr[iKeyAt] = iArr[iKeyAt] + i3;
                    } else {
                        i5++;
                    }
                }
            }
            if (i5 > 0 && i5 < size) {
                int i7 = (i3 * i5) / (size - i5);
                for (int i8 = 0; i8 < size; i8++) {
                    int iKeyAt2 = sparseBooleanArray.keyAt(i8);
                    if (sparseBooleanArray.valueAt(i8) && iKeyAt2 < length) {
                        if (i7 > iArr[iKeyAt2]) {
                            iArr[iKeyAt2] = 0;
                        } else {
                            iArr[iKeyAt2] = iArr[iKeyAt2] + i7;
                        }
                    }
                }
                return;
            }
            return;
        }
        for (int i9 = 0; i9 < size; i9++) {
            iArr[i9] = iArr[i9] + i3;
        }
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
        return TableLayout.class.getName();
    }

    public static class LayoutParams extends LinearLayout.LayoutParams {
        public LayoutParams(Context context, AttributeSet attributeSet) {
            super(context, attributeSet);
        }

        public LayoutParams(int i, int i2) {
            super(-1, i2);
        }

        public LayoutParams(int i, int i2, float f) {
            super(-1, i2, f);
        }

        public LayoutParams() {
            super(-1, -2);
        }

        public LayoutParams(ViewGroup.LayoutParams layoutParams) {
            super(layoutParams);
            this.width = -1;
        }

        public LayoutParams(ViewGroup.MarginLayoutParams marginLayoutParams) {
            super(marginLayoutParams);
            this.width = -1;
            if (marginLayoutParams instanceof LayoutParams) {
                this.weight = ((LayoutParams) marginLayoutParams).weight;
            }
        }

        @Override
        protected void setBaseAttributes(TypedArray typedArray, int i, int i2) {
            this.width = -1;
            if (typedArray.hasValue(i2)) {
                this.height = typedArray.getLayoutDimension(i2, "layout_height");
            } else {
                this.height = -2;
            }
        }
    }

    private class PassThroughHierarchyChangeListener implements ViewGroup.OnHierarchyChangeListener {
        private ViewGroup.OnHierarchyChangeListener mOnHierarchyChangeListener;

        private PassThroughHierarchyChangeListener() {
        }

        @Override
        public void onChildViewAdded(View view, View view2) {
            TableLayout.this.trackCollapsedColumns(view2);
            if (this.mOnHierarchyChangeListener != null) {
                this.mOnHierarchyChangeListener.onChildViewAdded(view, view2);
            }
        }

        @Override
        public void onChildViewRemoved(View view, View view2) {
            if (this.mOnHierarchyChangeListener != null) {
                this.mOnHierarchyChangeListener.onChildViewRemoved(view, view2);
            }
        }
    }
}
