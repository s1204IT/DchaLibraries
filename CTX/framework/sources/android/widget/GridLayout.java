package android.widget;

import android.app.slice.Slice;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Insets;
import android.graphics.Paint;
import android.media.AudioSystem;
import android.net.wifi.WifiEnterpriseConfig;
import android.telecom.Logging.Session;
import android.util.AttributeSet;
import android.util.LogPrinter;
import android.util.Pair;
import android.util.Printer;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RemoteViews;
import com.android.internal.R;
import com.android.internal.content.NativeLibraryHelper;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

@RemoteViews.RemoteView
public class GridLayout extends ViewGroup {
    private static final int ALIGNMENT_MODE = 6;
    public static final int ALIGN_BOUNDS = 0;
    public static final int ALIGN_MARGINS = 1;
    private static final int CAN_STRETCH = 2;
    private static final int COLUMN_COUNT = 3;
    private static final int COLUMN_ORDER_PRESERVED = 4;
    private static final int DEFAULT_ALIGNMENT_MODE = 1;
    static final int DEFAULT_CONTAINER_MARGIN = 0;
    private static final int DEFAULT_COUNT = Integer.MIN_VALUE;
    private static final boolean DEFAULT_ORDER_PRESERVED = true;
    private static final int DEFAULT_ORIENTATION = 0;
    private static final boolean DEFAULT_USE_DEFAULT_MARGINS = false;
    public static final int HORIZONTAL = 0;
    private static final int INFLEXIBLE = 0;
    static final int MAX_SIZE = 100000;
    private static final int ORIENTATION = 0;
    private static final int ROW_COUNT = 1;
    private static final int ROW_ORDER_PRESERVED = 2;
    public static final int UNDEFINED = Integer.MIN_VALUE;
    static final int UNINITIALIZED_HASH = 0;
    private static final int USE_DEFAULT_MARGINS = 5;
    public static final int VERTICAL = 1;
    int mAlignmentMode;
    int mDefaultGap;
    final Axis mHorizontalAxis;
    int mLastLayoutParamsHashCode;
    int mOrientation;
    Printer mPrinter;
    boolean mUseDefaultMargins;
    final Axis mVerticalAxis;
    static final Printer LOG_PRINTER = new LogPrinter(3, GridLayout.class.getName());
    static final Printer NO_PRINTER = new Printer() {
        @Override
        public void println(String str) {
        }
    };
    static final Alignment UNDEFINED_ALIGNMENT = new Alignment() {
        @Override
        int getGravityOffset(View view, int i) {
            return Integer.MIN_VALUE;
        }

        @Override
        public int getAlignmentValue(View view, int i, int i2) {
            return Integer.MIN_VALUE;
        }
    };
    private static final Alignment LEADING = new Alignment() {
        @Override
        int getGravityOffset(View view, int i) {
            return 0;
        }

        @Override
        public int getAlignmentValue(View view, int i, int i2) {
            return 0;
        }
    };
    private static final Alignment TRAILING = new Alignment() {
        @Override
        int getGravityOffset(View view, int i) {
            return i;
        }

        @Override
        public int getAlignmentValue(View view, int i, int i2) {
            return i;
        }
    };
    public static final Alignment TOP = LEADING;
    public static final Alignment BOTTOM = TRAILING;
    public static final Alignment START = LEADING;
    public static final Alignment END = TRAILING;
    public static final Alignment LEFT = createSwitchingAlignment(START, END);
    public static final Alignment RIGHT = createSwitchingAlignment(END, START);
    public static final Alignment CENTER = new Alignment() {
        @Override
        int getGravityOffset(View view, int i) {
            return i >> 1;
        }

        @Override
        public int getAlignmentValue(View view, int i, int i2) {
            return i >> 1;
        }
    };
    public static final Alignment BASELINE = new Alignment() {
        @Override
        int getGravityOffset(View view, int i) {
            return 0;
        }

        @Override
        public int getAlignmentValue(View view, int i, int i2) {
            if (view.getVisibility() == 8) {
                return 0;
            }
            int baseline = view.getBaseline();
            if (baseline == -1) {
                return Integer.MIN_VALUE;
            }
            return baseline;
        }

        @Override
        public Bounds getBounds() {
            return new Bounds() {
                private int size;

                @Override
                protected void reset() {
                    super.reset();
                    this.size = Integer.MIN_VALUE;
                }

                @Override
                protected void include(int i, int i2) {
                    super.include(i, i2);
                    this.size = Math.max(this.size, i + i2);
                }

                @Override
                protected int size(boolean z) {
                    return Math.max(super.size(z), this.size);
                }

                @Override
                protected int getOffset(GridLayout gridLayout, View view, Alignment alignment, int i, boolean z) {
                    return Math.max(0, super.getOffset(gridLayout, view, alignment, i, z));
                }
            };
        }
    };
    public static final Alignment FILL = new Alignment() {
        @Override
        int getGravityOffset(View view, int i) {
            return 0;
        }

        @Override
        public int getAlignmentValue(View view, int i, int i2) {
            return Integer.MIN_VALUE;
        }

        @Override
        public int getSizeInCell(View view, int i, int i2) {
            return i2;
        }
    };

    @Retention(RetentionPolicy.SOURCE)
    public @interface AlignmentMode {
    }

    @Retention(RetentionPolicy.SOURCE)
    public @interface Orientation {
    }

    public GridLayout(Context context) {
        this(context, null);
    }

    public GridLayout(Context context, AttributeSet attributeSet) {
        this(context, attributeSet, 0);
    }

    public GridLayout(Context context, AttributeSet attributeSet, int i) {
        this(context, attributeSet, i, 0);
    }

    public GridLayout(Context context, AttributeSet attributeSet, int i, int i2) {
        super(context, attributeSet, i, i2);
        this.mHorizontalAxis = new Axis(true);
        this.mVerticalAxis = new Axis(false);
        this.mOrientation = 0;
        this.mUseDefaultMargins = false;
        this.mAlignmentMode = 1;
        this.mLastLayoutParamsHashCode = 0;
        this.mPrinter = LOG_PRINTER;
        this.mDefaultGap = context.getResources().getDimensionPixelOffset(R.dimen.default_gap);
        TypedArray typedArrayObtainStyledAttributes = context.obtainStyledAttributes(attributeSet, R.styleable.GridLayout, i, i2);
        try {
            setRowCount(typedArrayObtainStyledAttributes.getInt(1, Integer.MIN_VALUE));
            setColumnCount(typedArrayObtainStyledAttributes.getInt(3, Integer.MIN_VALUE));
            setOrientation(typedArrayObtainStyledAttributes.getInt(0, 0));
            setUseDefaultMargins(typedArrayObtainStyledAttributes.getBoolean(5, false));
            setAlignmentMode(typedArrayObtainStyledAttributes.getInt(6, 1));
            setRowOrderPreserved(typedArrayObtainStyledAttributes.getBoolean(2, true));
            setColumnOrderPreserved(typedArrayObtainStyledAttributes.getBoolean(4, true));
        } finally {
            typedArrayObtainStyledAttributes.recycle();
        }
    }

    public int getOrientation() {
        return this.mOrientation;
    }

    public void setOrientation(int i) {
        if (this.mOrientation != i) {
            this.mOrientation = i;
            invalidateStructure();
            requestLayout();
        }
    }

    public int getRowCount() {
        return this.mVerticalAxis.getCount();
    }

    public void setRowCount(int i) {
        this.mVerticalAxis.setCount(i);
        invalidateStructure();
        requestLayout();
    }

    public int getColumnCount() {
        return this.mHorizontalAxis.getCount();
    }

    public void setColumnCount(int i) {
        this.mHorizontalAxis.setCount(i);
        invalidateStructure();
        requestLayout();
    }

    public boolean getUseDefaultMargins() {
        return this.mUseDefaultMargins;
    }

    public void setUseDefaultMargins(boolean z) {
        this.mUseDefaultMargins = z;
        requestLayout();
    }

    public int getAlignmentMode() {
        return this.mAlignmentMode;
    }

    public void setAlignmentMode(int i) {
        this.mAlignmentMode = i;
        requestLayout();
    }

    public boolean isRowOrderPreserved() {
        return this.mVerticalAxis.isOrderPreserved();
    }

    public void setRowOrderPreserved(boolean z) {
        this.mVerticalAxis.setOrderPreserved(z);
        invalidateStructure();
        requestLayout();
    }

    public boolean isColumnOrderPreserved() {
        return this.mHorizontalAxis.isOrderPreserved();
    }

    public void setColumnOrderPreserved(boolean z) {
        this.mHorizontalAxis.setOrderPreserved(z);
        invalidateStructure();
        requestLayout();
    }

    public Printer getPrinter() {
        return this.mPrinter;
    }

    public void setPrinter(Printer printer) {
        if (printer == null) {
            printer = NO_PRINTER;
        }
        this.mPrinter = printer;
    }

    static int max2(int[] iArr, int i) {
        for (int i2 : iArr) {
            i = Math.max(i, i2);
        }
        return i;
    }

    static <T> T[] append(T[] tArr, T[] tArr2) {
        T[] tArr3 = (T[]) ((Object[]) Array.newInstance(tArr.getClass().getComponentType(), tArr.length + tArr2.length));
        System.arraycopy(tArr, 0, tArr3, 0, tArr.length);
        System.arraycopy(tArr2, 0, tArr3, tArr.length, tArr2.length);
        return tArr3;
    }

    static Alignment getAlignment(int i, boolean z) {
        int i2;
        if (!z) {
            i2 = 112;
        } else {
            i2 = 7;
        }
        int i3 = (i & i2) >> (z ? 0 : 4);
        if (i3 == 1) {
            return CENTER;
        }
        if (i3 == 3) {
            return z ? LEFT : TOP;
        }
        if (i3 == 5) {
            return z ? RIGHT : BOTTOM;
        }
        if (i3 == 7) {
            return FILL;
        }
        if (i3 == 8388611) {
            return START;
        }
        if (i3 == 8388613) {
            return END;
        }
        return UNDEFINED_ALIGNMENT;
    }

    private int getDefaultMargin(View view, boolean z, boolean z2) {
        if (view.getClass() == Space.class) {
            return 0;
        }
        return this.mDefaultGap / 2;
    }

    private int getDefaultMargin(View view, boolean z, boolean z2, boolean z3) {
        return getDefaultMargin(view, z2, z3);
    }

    private int getDefaultMargin(View view, LayoutParams layoutParams, boolean z, boolean z2) {
        boolean z3;
        boolean z4 = false;
        if (!this.mUseDefaultMargins) {
            return 0;
        }
        Spec spec = z ? layoutParams.columnSpec : layoutParams.rowSpec;
        Axis axis = z ? this.mHorizontalAxis : this.mVerticalAxis;
        Interval interval = spec.span;
        if (z && isLayoutRtl()) {
            z3 = !z2;
        } else {
            z3 = z2;
        }
        if (!z3 ? interval.max == axis.getCount() : interval.min == 0) {
            z4 = true;
        }
        return getDefaultMargin(view, z4, z, z2);
    }

    int getMargin1(View view, boolean z, boolean z2) {
        int i;
        LayoutParams layoutParams = getLayoutParams(view);
        if (z) {
            i = z2 ? layoutParams.leftMargin : layoutParams.rightMargin;
        } else {
            i = z2 ? layoutParams.topMargin : layoutParams.bottomMargin;
        }
        return i == Integer.MIN_VALUE ? getDefaultMargin(view, layoutParams, z, z2) : i;
    }

    private int getMargin(View view, boolean z, boolean z2) {
        if (this.mAlignmentMode == 1) {
            return getMargin1(view, z, z2);
        }
        Axis axis = z ? this.mHorizontalAxis : this.mVerticalAxis;
        int[] leadingMargins = z2 ? axis.getLeadingMargins() : axis.getTrailingMargins();
        LayoutParams layoutParams = getLayoutParams(view);
        Spec spec = z ? layoutParams.columnSpec : layoutParams.rowSpec;
        return leadingMargins[z2 ? spec.span.min : spec.span.max];
    }

    private int getTotalMargin(View view, boolean z) {
        return getMargin(view, z, true) + getMargin(view, z, false);
    }

    private static boolean fits(int[] iArr, int i, int i2, int i3) {
        if (i3 > iArr.length) {
            return false;
        }
        while (i2 < i3) {
            if (iArr[i2] > i) {
                return false;
            }
            i2++;
        }
        return true;
    }

    private static void procrusteanFill(int[] iArr, int i, int i2, int i3) {
        int length = iArr.length;
        Arrays.fill(iArr, Math.min(i, length), Math.min(i2, length), i3);
    }

    private static void setCellGroup(LayoutParams layoutParams, int i, int i2, int i3, int i4) {
        layoutParams.setRowSpecSpan(new Interval(i, i2 + i));
        layoutParams.setColumnSpecSpan(new Interval(i3, i4 + i3));
    }

    private static int clip(Interval interval, boolean z, int i) {
        int size = interval.size();
        if (i == 0) {
            return size;
        }
        return Math.min(size, i - (z ? Math.min(interval.min, i) : 0));
    }

    private void validateLayoutParams() {
        boolean z = this.mOrientation == 0;
        Axis axis = z ? this.mHorizontalAxis : this.mVerticalAxis;
        int i = axis.definedCount != Integer.MIN_VALUE ? axis.definedCount : 0;
        int[] iArr = new int[i];
        int childCount = getChildCount();
        int i2 = 0;
        int i3 = 0;
        for (int i4 = 0; i4 < childCount; i4++) {
            LayoutParams layoutParams = (LayoutParams) getChildAt(i4).getLayoutParams();
            Spec spec = z ? layoutParams.rowSpec : layoutParams.columnSpec;
            Interval interval = spec.span;
            boolean z2 = spec.startDefined;
            int size = interval.size();
            if (z2) {
                i3 = interval.min;
            }
            Spec spec2 = z ? layoutParams.columnSpec : layoutParams.rowSpec;
            Interval interval2 = spec2.span;
            boolean z3 = spec2.startDefined;
            int iClip = clip(interval2, z3, i);
            if (z3) {
                i2 = interval2.min;
            }
            if (i != 0) {
                if (!z2 || !z3) {
                    while (true) {
                        int i5 = i2 + iClip;
                        if (fits(iArr, i3, i2, i5)) {
                            break;
                        }
                        if (z3) {
                            i3++;
                        } else if (i5 <= i) {
                            i2++;
                        } else {
                            i3++;
                            i2 = 0;
                        }
                    }
                }
                procrusteanFill(iArr, i2, i2 + iClip, i3 + size);
            }
            if (z) {
                setCellGroup(layoutParams, i3, size, i2, iClip);
            } else {
                setCellGroup(layoutParams, i2, iClip, i3, size);
            }
            i2 += iClip;
        }
    }

    private void invalidateStructure() {
        this.mLastLayoutParamsHashCode = 0;
        this.mHorizontalAxis.invalidateStructure();
        this.mVerticalAxis.invalidateStructure();
        invalidateValues();
    }

    private void invalidateValues() {
        if (this.mHorizontalAxis != null && this.mVerticalAxis != null) {
            this.mHorizontalAxis.invalidateValues();
            this.mVerticalAxis.invalidateValues();
        }
    }

    @Override
    protected void onSetLayoutParams(View view, ViewGroup.LayoutParams layoutParams) {
        super.onSetLayoutParams(view, layoutParams);
        if (!checkLayoutParams(layoutParams)) {
            handleInvalidParams("supplied LayoutParams are of the wrong type");
        }
        invalidateStructure();
    }

    final LayoutParams getLayoutParams(View view) {
        return (LayoutParams) view.getLayoutParams();
    }

    private static void handleInvalidParams(String str) {
        throw new IllegalArgumentException(str + ". ");
    }

    private void checkLayoutParams(LayoutParams layoutParams, boolean z) {
        String str = z ? "column" : "row";
        Interval interval = (z ? layoutParams.columnSpec : layoutParams.rowSpec).span;
        if (interval.min != Integer.MIN_VALUE && interval.min < 0) {
            handleInvalidParams(str + " indices must be positive");
        }
        int i = (z ? this.mHorizontalAxis : this.mVerticalAxis).definedCount;
        if (i != Integer.MIN_VALUE) {
            if (interval.max > i) {
                handleInvalidParams(str + " indices (start + span) mustn't exceed the " + str + " count");
            }
            if (interval.size() > i) {
                handleInvalidParams(str + " span mustn't exceed the " + str + " count");
            }
        }
    }

    @Override
    protected boolean checkLayoutParams(ViewGroup.LayoutParams layoutParams) {
        if (!(layoutParams instanceof LayoutParams)) {
            return false;
        }
        LayoutParams layoutParams2 = (LayoutParams) layoutParams;
        checkLayoutParams(layoutParams2, true);
        checkLayoutParams(layoutParams2, false);
        return true;
    }

    @Override
    protected LayoutParams generateDefaultLayoutParams() {
        return new LayoutParams();
    }

    @Override
    public LayoutParams generateLayoutParams(AttributeSet attributeSet) {
        return new LayoutParams(getContext(), attributeSet);
    }

    @Override
    protected LayoutParams generateLayoutParams(ViewGroup.LayoutParams layoutParams) {
        if (sPreserveMarginParamsInLayoutParamConversion) {
            if (layoutParams instanceof LayoutParams) {
                return new LayoutParams((LayoutParams) layoutParams);
            }
            if (layoutParams instanceof ViewGroup.MarginLayoutParams) {
                return new LayoutParams((ViewGroup.MarginLayoutParams) layoutParams);
            }
        }
        return new LayoutParams(layoutParams);
    }

    private void drawLine(Canvas canvas, int i, int i2, int i3, int i4, Paint paint) {
        if (isLayoutRtl()) {
            int width = getWidth();
            canvas.drawLine(width - i, i2, width - i3, i4, paint);
        } else {
            canvas.drawLine(i, i2, i3, i4, paint);
        }
    }

    @Override
    protected void onDebugDrawMargins(Canvas canvas, Paint paint) {
        LayoutParams layoutParams = new LayoutParams();
        for (int i = 0; i < getChildCount(); i++) {
            View childAt = getChildAt(i);
            layoutParams.setMargins(getMargin1(childAt, true, true), getMargin1(childAt, false, true), getMargin1(childAt, true, false), getMargin1(childAt, false, false));
            layoutParams.onDebugDraw(childAt, canvas, paint);
        }
    }

    @Override
    protected void onDebugDraw(Canvas canvas) {
        Paint paint = new Paint();
        paint.setStyle(Paint.Style.STROKE);
        paint.setColor(Color.argb(50, 255, 255, 255));
        Insets opticalInsets = getOpticalInsets();
        int paddingTop = getPaddingTop() + opticalInsets.top;
        int paddingLeft = getPaddingLeft() + opticalInsets.left;
        int width = (getWidth() - getPaddingRight()) - opticalInsets.right;
        int height = (getHeight() - getPaddingBottom()) - opticalInsets.bottom;
        int[] iArr = this.mHorizontalAxis.locations;
        if (iArr != null) {
            for (int i : iArr) {
                int i2 = paddingLeft + i;
                drawLine(canvas, i2, paddingTop, i2, height, paint);
            }
        }
        int[] iArr2 = this.mVerticalAxis.locations;
        if (iArr2 != null) {
            for (int i3 : iArr2) {
                int i4 = paddingTop + i3;
                drawLine(canvas, paddingLeft, i4, width, i4, paint);
            }
        }
        super.onDebugDraw(canvas);
    }

    @Override
    public void onViewAdded(View view) {
        super.onViewAdded(view);
        invalidateStructure();
    }

    @Override
    public void onViewRemoved(View view) {
        super.onViewRemoved(view);
        invalidateStructure();
    }

    @Override
    protected void onChildVisibilityChanged(View view, int i, int i2) {
        super.onChildVisibilityChanged(view, i, i2);
        if (i == 8 || i2 == 8) {
            invalidateStructure();
        }
    }

    private int computeLayoutParamsHashCode() {
        int childCount = getChildCount();
        int iHashCode = 1;
        for (int i = 0; i < childCount; i++) {
            View childAt = getChildAt(i);
            if (childAt.getVisibility() != 8) {
                iHashCode = (31 * iHashCode) + ((LayoutParams) childAt.getLayoutParams()).hashCode();
            }
        }
        return iHashCode;
    }

    private void consistencyCheck() {
        if (this.mLastLayoutParamsHashCode == 0) {
            validateLayoutParams();
            this.mLastLayoutParamsHashCode = computeLayoutParamsHashCode();
        } else if (this.mLastLayoutParamsHashCode != computeLayoutParamsHashCode()) {
            this.mPrinter.println("The fields of some layout parameters were modified in between layout operations. Check the javadoc for GridLayout.LayoutParams#rowSpec.");
            invalidateStructure();
            consistencyCheck();
        }
    }

    private void measureChildWithMargins2(View view, int i, int i2, int i3, int i4) {
        view.measure(getChildMeasureSpec(i, getTotalMargin(view, true), i3), getChildMeasureSpec(i2, getTotalMargin(view, false), i4));
    }

    private void measureChildrenWithMargins(int i, int i2, boolean z) {
        int childCount = getChildCount();
        for (int i3 = 0; i3 < childCount; i3++) {
            View childAt = getChildAt(i3);
            if (childAt.getVisibility() != 8) {
                LayoutParams layoutParams = getLayoutParams(childAt);
                if (z) {
                    measureChildWithMargins2(childAt, i, i2, layoutParams.width, layoutParams.height);
                } else {
                    boolean z2 = this.mOrientation == 0;
                    Spec spec = z2 ? layoutParams.columnSpec : layoutParams.rowSpec;
                    if (spec.getAbsoluteAlignment(z2) == FILL) {
                        Interval interval = spec.span;
                        int[] locations = (z2 ? this.mHorizontalAxis : this.mVerticalAxis).getLocations();
                        int totalMargin = (locations[interval.max] - locations[interval.min]) - getTotalMargin(childAt, z2);
                        if (z2) {
                            measureChildWithMargins2(childAt, i, i2, totalMargin, layoutParams.height);
                        } else {
                            measureChildWithMargins2(childAt, i, i2, layoutParams.width, totalMargin);
                        }
                    }
                }
            }
        }
    }

    static int adjust(int i, int i2) {
        return View.MeasureSpec.makeMeasureSpec(View.MeasureSpec.getSize(i2 + i), View.MeasureSpec.getMode(i));
    }

    @Override
    protected void onMeasure(int i, int i2) {
        int measure;
        int measure2;
        consistencyCheck();
        invalidateValues();
        int paddingLeft = getPaddingLeft() + getPaddingRight();
        int paddingTop = getPaddingTop() + getPaddingBottom();
        int iAdjust = adjust(i, -paddingLeft);
        int iAdjust2 = adjust(i2, -paddingTop);
        measureChildrenWithMargins(iAdjust, iAdjust2, true);
        if (this.mOrientation == 0) {
            int measure3 = this.mHorizontalAxis.getMeasure(iAdjust);
            measureChildrenWithMargins(iAdjust, iAdjust2, false);
            measure = this.mVerticalAxis.getMeasure(iAdjust2);
            measure2 = measure3;
        } else {
            measure = this.mVerticalAxis.getMeasure(iAdjust2);
            measureChildrenWithMargins(iAdjust, iAdjust2, false);
            measure2 = this.mHorizontalAxis.getMeasure(iAdjust);
        }
        setMeasuredDimension(resolveSizeAndState(Math.max(measure2 + paddingLeft, getSuggestedMinimumWidth()), i, 0), resolveSizeAndState(Math.max(measure + paddingTop, getSuggestedMinimumHeight()), i2, 0));
    }

    private int getMeasurement(View view, boolean z) {
        return z ? view.getMeasuredWidth() : view.getMeasuredHeight();
    }

    final int getMeasurementIncludingMargin(View view, boolean z) {
        if (view.getVisibility() == 8) {
            return 0;
        }
        return getMeasurement(view, z) + getTotalMargin(view, z);
    }

    @Override
    public void requestLayout() {
        super.requestLayout();
        invalidateValues();
    }

    @Override
    protected void onLayout(boolean z, int i, int i2, int i3, int i4) {
        int[] iArr;
        int[] iArr2;
        consistencyCheck();
        int i5 = i3 - i;
        int paddingLeft = getPaddingLeft();
        int paddingTop = getPaddingTop();
        int paddingRight = getPaddingRight();
        int paddingBottom = getPaddingBottom();
        this.mHorizontalAxis.layout((i5 - paddingLeft) - paddingRight);
        this.mVerticalAxis.layout(((i4 - i2) - paddingTop) - paddingBottom);
        int[] locations = this.mHorizontalAxis.getLocations();
        int[] locations2 = this.mVerticalAxis.getLocations();
        int childCount = getChildCount();
        boolean z2 = false;
        int i6 = 0;
        while (i6 < childCount) {
            View childAt = getChildAt(i6);
            if (childAt.getVisibility() == 8) {
                iArr = locations;
                iArr2 = locations2;
            } else {
                LayoutParams layoutParams = getLayoutParams(childAt);
                Spec spec = layoutParams.columnSpec;
                Spec spec2 = layoutParams.rowSpec;
                Interval interval = spec.span;
                Interval interval2 = spec2.span;
                int i7 = locations[interval.min];
                int i8 = locations2[interval2.min];
                int i9 = locations[interval.max] - i7;
                int i10 = locations2[interval2.max] - i8;
                int measurement = getMeasurement(childAt, true);
                int measurement2 = getMeasurement(childAt, z2);
                Alignment absoluteAlignment = spec.getAbsoluteAlignment(true);
                Alignment absoluteAlignment2 = spec2.getAbsoluteAlignment(z2);
                Bounds value = this.mHorizontalAxis.getGroupBounds().getValue(i6);
                Bounds value2 = this.mVerticalAxis.getGroupBounds().getValue(i6);
                int gravityOffset = absoluteAlignment.getGravityOffset(childAt, i9 - value.size(true));
                int gravityOffset2 = absoluteAlignment2.getGravityOffset(childAt, i10 - value2.size(true));
                int margin = getMargin(childAt, true, true);
                iArr = locations;
                int margin2 = getMargin(childAt, false, true);
                int margin3 = getMargin(childAt, true, false);
                int i11 = margin + margin3;
                int margin4 = margin2 + getMargin(childAt, false, false);
                iArr2 = locations2;
                int offset = value.getOffset(this, childAt, absoluteAlignment, measurement + i11, true);
                int offset2 = value2.getOffset(this, childAt, absoluteAlignment2, measurement2 + margin4, false);
                int sizeInCell = absoluteAlignment.getSizeInCell(childAt, measurement, i9 - i11);
                int sizeInCell2 = absoluteAlignment2.getSizeInCell(childAt, measurement2, i10 - margin4);
                int i12 = i7 + gravityOffset + offset;
                int i13 = !isLayoutRtl() ? paddingLeft + margin + i12 : (((i5 - sizeInCell) - paddingRight) - margin3) - i12;
                int i14 = paddingTop + i8 + gravityOffset2 + offset2 + margin2;
                if (sizeInCell != childAt.getMeasuredWidth() || sizeInCell2 != childAt.getMeasuredHeight()) {
                    childAt.measure(View.MeasureSpec.makeMeasureSpec(sizeInCell, 1073741824), View.MeasureSpec.makeMeasureSpec(sizeInCell2, 1073741824));
                }
                childAt.layout(i13, i14, sizeInCell + i13, sizeInCell2 + i14);
            }
            i6++;
            locations = iArr;
            locations2 = iArr2;
            z2 = false;
        }
    }

    @Override
    public CharSequence getAccessibilityClassName() {
        return GridLayout.class.getName();
    }

    final class Axis {
        static final boolean $assertionsDisabled = false;
        private static final int COMPLETE = 2;
        private static final int NEW = 0;
        private static final int PENDING = 1;
        public Arc[] arcs;
        public boolean arcsValid;
        PackedMap<Interval, MutableInt> backwardLinks;
        public boolean backwardLinksValid;
        public int definedCount;
        public int[] deltas;
        PackedMap<Interval, MutableInt> forwardLinks;
        public boolean forwardLinksValid;
        PackedMap<Spec, Bounds> groupBounds;
        public boolean groupBoundsValid;
        public boolean hasWeights;
        public boolean hasWeightsValid;
        public final boolean horizontal;
        public int[] leadingMargins;
        public boolean leadingMarginsValid;
        public int[] locations;
        public boolean locationsValid;
        private int maxIndex;
        boolean orderPreserved;
        private MutableInt parentMax;
        private MutableInt parentMin;
        public int[] trailingMargins;
        public boolean trailingMarginsValid;

        private Axis(boolean z) {
            this.definedCount = Integer.MIN_VALUE;
            this.maxIndex = Integer.MIN_VALUE;
            this.groupBoundsValid = false;
            this.forwardLinksValid = false;
            this.backwardLinksValid = false;
            this.leadingMarginsValid = false;
            this.trailingMarginsValid = false;
            this.arcsValid = false;
            this.locationsValid = false;
            this.hasWeightsValid = false;
            this.orderPreserved = true;
            this.parentMin = new MutableInt(0);
            this.parentMax = new MutableInt(-100000);
            this.horizontal = z;
        }

        private int calculateMaxIndex() {
            int childCount = GridLayout.this.getChildCount();
            int iMax = -1;
            for (int i = 0; i < childCount; i++) {
                LayoutParams layoutParams = GridLayout.this.getLayoutParams(GridLayout.this.getChildAt(i));
                Interval interval = (this.horizontal ? layoutParams.columnSpec : layoutParams.rowSpec).span;
                iMax = Math.max(Math.max(Math.max(iMax, interval.min), interval.max), interval.size());
            }
            if (iMax == -1) {
                return Integer.MIN_VALUE;
            }
            return iMax;
        }

        private int getMaxIndex() {
            if (this.maxIndex == Integer.MIN_VALUE) {
                this.maxIndex = Math.max(0, calculateMaxIndex());
            }
            return this.maxIndex;
        }

        public int getCount() {
            return Math.max(this.definedCount, getMaxIndex());
        }

        public void setCount(int i) {
            if (i != Integer.MIN_VALUE && i < getMaxIndex()) {
                StringBuilder sb = new StringBuilder();
                sb.append(this.horizontal ? "column" : "row");
                sb.append("Count must be greater than or equal to the maximum of all grid indices (and spans) defined in the LayoutParams of each child");
                GridLayout.handleInvalidParams(sb.toString());
            }
            this.definedCount = i;
        }

        public boolean isOrderPreserved() {
            return this.orderPreserved;
        }

        public void setOrderPreserved(boolean z) {
            this.orderPreserved = z;
            invalidateStructure();
        }

        private PackedMap<Spec, Bounds> createGroupBounds() {
            Assoc assocOf = Assoc.of(Spec.class, Bounds.class);
            int childCount = GridLayout.this.getChildCount();
            for (int i = 0; i < childCount; i++) {
                LayoutParams layoutParams = GridLayout.this.getLayoutParams(GridLayout.this.getChildAt(i));
                Spec spec = this.horizontal ? layoutParams.columnSpec : layoutParams.rowSpec;
                assocOf.put(spec, spec.getAbsoluteAlignment(this.horizontal).getBounds());
            }
            return assocOf.pack();
        }

        private void computeGroupBounds() {
            for (Bounds bounds : this.groupBounds.values) {
                bounds.reset();
            }
            int childCount = GridLayout.this.getChildCount();
            for (int i = 0; i < childCount; i++) {
                View childAt = GridLayout.this.getChildAt(i);
                LayoutParams layoutParams = GridLayout.this.getLayoutParams(childAt);
                Spec spec = this.horizontal ? layoutParams.columnSpec : layoutParams.rowSpec;
                this.groupBounds.getValue(i).include(GridLayout.this, childAt, spec, this, GridLayout.this.getMeasurementIncludingMargin(childAt, this.horizontal) + (spec.weight == 0.0f ? 0 : getDeltas()[i]));
            }
        }

        public PackedMap<Spec, Bounds> getGroupBounds() {
            if (this.groupBounds == null) {
                this.groupBounds = createGroupBounds();
            }
            if (!this.groupBoundsValid) {
                computeGroupBounds();
                this.groupBoundsValid = true;
            }
            return this.groupBounds;
        }

        private PackedMap<Interval, MutableInt> createLinks(boolean z) {
            Assoc assocOf = Assoc.of(Interval.class, MutableInt.class);
            Spec[] specArr = getGroupBounds().keys;
            int length = specArr.length;
            for (int i = 0; i < length; i++) {
                assocOf.put(z ? specArr[i].span : specArr[i].span.inverse(), new MutableInt());
            }
            return assocOf.pack();
        }

        private void computeLinks(PackedMap<Interval, MutableInt> packedMap, boolean z) {
            for (MutableInt mutableInt : packedMap.values) {
                mutableInt.reset();
            }
            Bounds[] boundsArr = getGroupBounds().values;
            for (int i = 0; i < boundsArr.length; i++) {
                int size = boundsArr[i].size(z);
                MutableInt value = packedMap.getValue(i);
                int i2 = value.value;
                if (!z) {
                    size = -size;
                }
                value.value = Math.max(i2, size);
            }
        }

        private PackedMap<Interval, MutableInt> getForwardLinks() {
            if (this.forwardLinks == null) {
                this.forwardLinks = createLinks(true);
            }
            if (!this.forwardLinksValid) {
                computeLinks(this.forwardLinks, true);
                this.forwardLinksValid = true;
            }
            return this.forwardLinks;
        }

        private PackedMap<Interval, MutableInt> getBackwardLinks() {
            if (this.backwardLinks == null) {
                this.backwardLinks = createLinks(false);
            }
            if (!this.backwardLinksValid) {
                computeLinks(this.backwardLinks, false);
                this.backwardLinksValid = true;
            }
            return this.backwardLinks;
        }

        private void include(List<Arc> list, Interval interval, MutableInt mutableInt, boolean z) {
            if (interval.size() == 0) {
                return;
            }
            if (z) {
                Iterator<Arc> it = list.iterator();
                while (it.hasNext()) {
                    if (it.next().span.equals(interval)) {
                        return;
                    }
                }
            }
            list.add(new Arc(interval, mutableInt));
        }

        private void include(List<Arc> list, Interval interval, MutableInt mutableInt) {
            include(list, interval, mutableInt, true);
        }

        Arc[][] groupArcsByFirstVertex(Arc[] arcArr) {
            int count = getCount() + 1;
            Arc[][] arcArr2 = new Arc[count][];
            int[] iArr = new int[count];
            for (Arc arc : arcArr) {
                int i = arc.span.min;
                iArr[i] = iArr[i] + 1;
            }
            for (int i2 = 0; i2 < iArr.length; i2++) {
                arcArr2[i2] = new Arc[iArr[i2]];
            }
            Arrays.fill(iArr, 0);
            for (Arc arc2 : arcArr) {
                int i3 = arc2.span.min;
                Arc[] arcArr3 = arcArr2[i3];
                int i4 = iArr[i3];
                iArr[i3] = i4 + 1;
                arcArr3[i4] = arc2;
            }
            return arcArr2;
        }

        private Arc[] topologicalSort(final Arc[] arcArr) {
            return new Object() {
                static final boolean $assertionsDisabled = false;
                Arc[][] arcsByVertex;
                int cursor;
                Arc[] result;
                int[] visited;

                {
                    this.result = new Arc[arcArr.length];
                    this.cursor = this.result.length - 1;
                    this.arcsByVertex = Axis.this.groupArcsByFirstVertex(arcArr);
                    this.visited = new int[Axis.this.getCount() + 1];
                }

                void walk(int i) {
                    switch (this.visited[i]) {
                        case 0:
                            this.visited[i] = 1;
                            for (Arc arc : this.arcsByVertex[i]) {
                                walk(arc.span.max);
                                Arc[] arcArr2 = this.result;
                                int i2 = this.cursor;
                                this.cursor = i2 - 1;
                                arcArr2[i2] = arc;
                            }
                            this.visited[i] = 2;
                            break;
                    }
                }

                Arc[] sort() {
                    int length = this.arcsByVertex.length;
                    for (int i = 0; i < length; i++) {
                        walk(i);
                    }
                    return this.result;
                }
            }.sort();
        }

        private Arc[] topologicalSort(List<Arc> list) {
            return topologicalSort((Arc[]) list.toArray(new Arc[list.size()]));
        }

        private void addComponentSizes(List<Arc> list, PackedMap<Interval, MutableInt> packedMap) {
            for (int i = 0; i < packedMap.keys.length; i++) {
                include(list, packedMap.keys[i], packedMap.values[i], false);
            }
        }

        private Arc[] createArcs() {
            ArrayList arrayList = new ArrayList();
            ArrayList arrayList2 = new ArrayList();
            addComponentSizes(arrayList, getForwardLinks());
            addComponentSizes(arrayList2, getBackwardLinks());
            if (this.orderPreserved) {
                int i = 0;
                while (i < getCount()) {
                    int i2 = i + 1;
                    include(arrayList, new Interval(i, i2), new MutableInt(0));
                    i = i2;
                }
            }
            int count = getCount();
            include(arrayList, new Interval(0, count), this.parentMin, false);
            include(arrayList2, new Interval(count, 0), this.parentMax, false);
            return (Arc[]) GridLayout.append(topologicalSort(arrayList), topologicalSort(arrayList2));
        }

        private void computeArcs() {
            getForwardLinks();
            getBackwardLinks();
        }

        public Arc[] getArcs() {
            if (this.arcs == null) {
                this.arcs = createArcs();
            }
            if (!this.arcsValid) {
                computeArcs();
                this.arcsValid = true;
            }
            return this.arcs;
        }

        private boolean relax(int[] iArr, Arc arc) {
            if (!arc.valid) {
                return false;
            }
            Interval interval = arc.span;
            int i = interval.min;
            int i2 = interval.max;
            int i3 = iArr[i] + arc.value.value;
            if (i3 <= iArr[i2]) {
                return false;
            }
            iArr[i2] = i3;
            return true;
        }

        private void init(int[] iArr) {
            Arrays.fill(iArr, 0);
        }

        private String arcsToString(List<Arc> list) {
            String str = this.horizontal ? "x" : "y";
            StringBuilder sb = new StringBuilder();
            boolean z = true;
            for (Arc arc : list) {
                if (z) {
                    z = false;
                } else {
                    sb.append(", ");
                }
                int i = arc.span.min;
                int i2 = arc.span.max;
                int i3 = arc.value.value;
                sb.append(i < i2 ? str + i2 + NativeLibraryHelper.CLEAR_ABI_OVERRIDE + str + i + ">=" + i3 : str + i + NativeLibraryHelper.CLEAR_ABI_OVERRIDE + str + i2 + "<=" + (-i3));
            }
            return sb.toString();
        }

        private void logError(String str, Arc[] arcArr, boolean[] zArr) {
            ArrayList arrayList = new ArrayList();
            ArrayList arrayList2 = new ArrayList();
            for (int i = 0; i < arcArr.length; i++) {
                Arc arc = arcArr[i];
                if (zArr[i]) {
                    arrayList.add(arc);
                }
                if (!arc.valid) {
                    arrayList2.add(arc);
                }
            }
            GridLayout.this.mPrinter.println(str + " constraints: " + arcsToString(arrayList) + " are inconsistent; permanently removing: " + arcsToString(arrayList2) + ". ");
        }

        private boolean solve(Arc[] arcArr, int[] iArr) {
            return solve(arcArr, iArr, true);
        }

        private boolean solve(Arc[] arcArr, int[] iArr, boolean z) {
            String str = this.horizontal ? Slice.HINT_HORIZONTAL : "vertical";
            int count = getCount() + 1;
            boolean[] zArr = null;
            for (int i = 0; i < arcArr.length; i++) {
                init(iArr);
                for (int i2 = 0; i2 < count; i2++) {
                    boolean zRelax = false;
                    for (Arc arc : arcArr) {
                        zRelax |= relax(iArr, arc);
                    }
                    if (!zRelax) {
                        if (zArr != null) {
                            logError(str, arcArr, zArr);
                        }
                        return true;
                    }
                }
                if (!z) {
                    return false;
                }
                boolean[] zArr2 = new boolean[arcArr.length];
                for (int i3 = 0; i3 < count; i3++) {
                    int length = arcArr.length;
                    for (int i4 = 0; i4 < length; i4++) {
                        zArr2[i4] = zArr2[i4] | relax(iArr, arcArr[i4]);
                    }
                }
                if (i == 0) {
                    zArr = zArr2;
                }
                int i5 = 0;
                while (true) {
                    if (i5 >= arcArr.length) {
                        break;
                    }
                    if (zArr2[i5]) {
                        Arc arc2 = arcArr[i5];
                        if (arc2.span.min >= arc2.span.max) {
                            arc2.valid = false;
                            break;
                        }
                    }
                    i5++;
                }
            }
            return true;
        }

        private void computeMargins(boolean z) {
            int[] iArr = z ? this.leadingMargins : this.trailingMargins;
            int childCount = GridLayout.this.getChildCount();
            for (int i = 0; i < childCount; i++) {
                View childAt = GridLayout.this.getChildAt(i);
                if (childAt.getVisibility() != 8) {
                    LayoutParams layoutParams = GridLayout.this.getLayoutParams(childAt);
                    Interval interval = (this.horizontal ? layoutParams.columnSpec : layoutParams.rowSpec).span;
                    int i2 = z ? interval.min : interval.max;
                    iArr[i2] = Math.max(iArr[i2], GridLayout.this.getMargin1(childAt, this.horizontal, z));
                }
            }
        }

        public int[] getLeadingMargins() {
            if (this.leadingMargins == null) {
                this.leadingMargins = new int[getCount() + 1];
            }
            if (!this.leadingMarginsValid) {
                computeMargins(true);
                this.leadingMarginsValid = true;
            }
            return this.leadingMargins;
        }

        public int[] getTrailingMargins() {
            if (this.trailingMargins == null) {
                this.trailingMargins = new int[getCount() + 1];
            }
            if (!this.trailingMarginsValid) {
                computeMargins(false);
                this.trailingMarginsValid = true;
            }
            return this.trailingMargins;
        }

        private boolean solve(int[] iArr) {
            return solve(getArcs(), iArr);
        }

        private boolean computeHasWeights() {
            int childCount = GridLayout.this.getChildCount();
            for (int i = 0; i < childCount; i++) {
                View childAt = GridLayout.this.getChildAt(i);
                if (childAt.getVisibility() != 8) {
                    LayoutParams layoutParams = GridLayout.this.getLayoutParams(childAt);
                    if ((this.horizontal ? layoutParams.columnSpec : layoutParams.rowSpec).weight != 0.0f) {
                        return true;
                    }
                }
            }
            return false;
        }

        private boolean hasWeights() {
            if (!this.hasWeightsValid) {
                this.hasWeights = computeHasWeights();
                this.hasWeightsValid = true;
            }
            return this.hasWeights;
        }

        public int[] getDeltas() {
            if (this.deltas == null) {
                this.deltas = new int[GridLayout.this.getChildCount()];
            }
            return this.deltas;
        }

        private void shareOutDelta(int i, float f) {
            Arrays.fill(this.deltas, 0);
            int childCount = GridLayout.this.getChildCount();
            for (int i2 = 0; i2 < childCount; i2++) {
                View childAt = GridLayout.this.getChildAt(i2);
                if (childAt.getVisibility() != 8) {
                    LayoutParams layoutParams = GridLayout.this.getLayoutParams(childAt);
                    float f2 = (this.horizontal ? layoutParams.columnSpec : layoutParams.rowSpec).weight;
                    if (f2 != 0.0f) {
                        int iRound = Math.round((i * f2) / f);
                        this.deltas[i2] = iRound;
                        i -= iRound;
                        f -= f2;
                    }
                }
            }
        }

        private void solveAndDistributeSpace(int[] iArr) {
            Arrays.fill(getDeltas(), 0);
            solve(iArr);
            int childCount = (this.parentMin.value * GridLayout.this.getChildCount()) + 1;
            if (childCount < 2) {
                return;
            }
            float fCalculateTotalWeight = calculateTotalWeight();
            int i = -1;
            boolean z = true;
            int i2 = childCount;
            int i3 = 0;
            while (i3 < i2) {
                int i4 = (int) ((((long) i3) + ((long) i2)) / 2);
                invalidateValues();
                shareOutDelta(i4, fCalculateTotalWeight);
                boolean zSolve = solve(getArcs(), iArr, false);
                if (zSolve) {
                    i3 = i4 + 1;
                    i = i4;
                } else {
                    i2 = i4;
                }
                z = zSolve;
            }
            if (i > 0 && !z) {
                invalidateValues();
                shareOutDelta(i, fCalculateTotalWeight);
                solve(iArr);
            }
        }

        private float calculateTotalWeight() {
            int childCount = GridLayout.this.getChildCount();
            float f = 0.0f;
            for (int i = 0; i < childCount; i++) {
                View childAt = GridLayout.this.getChildAt(i);
                if (childAt.getVisibility() != 8) {
                    LayoutParams layoutParams = GridLayout.this.getLayoutParams(childAt);
                    f += (this.horizontal ? layoutParams.columnSpec : layoutParams.rowSpec).weight;
                }
            }
            return f;
        }

        private void computeLocations(int[] iArr) {
            if (!hasWeights()) {
                solve(iArr);
            } else {
                solveAndDistributeSpace(iArr);
            }
            if (!this.orderPreserved) {
                int i = iArr[0];
                int length = iArr.length;
                for (int i2 = 0; i2 < length; i2++) {
                    iArr[i2] = iArr[i2] - i;
                }
            }
        }

        public int[] getLocations() {
            if (this.locations == null) {
                this.locations = new int[getCount() + 1];
            }
            if (!this.locationsValid) {
                computeLocations(this.locations);
                this.locationsValid = true;
            }
            return this.locations;
        }

        private int size(int[] iArr) {
            return iArr[getCount()];
        }

        private void setParentConstraints(int i, int i2) {
            this.parentMin.value = i;
            this.parentMax.value = -i2;
            this.locationsValid = false;
        }

        private int getMeasure(int i, int i2) {
            setParentConstraints(i, i2);
            return size(getLocations());
        }

        public int getMeasure(int i) {
            int mode = View.MeasureSpec.getMode(i);
            int size = View.MeasureSpec.getSize(i);
            if (mode == Integer.MIN_VALUE) {
                return getMeasure(0, size);
            }
            if (mode == 0) {
                return getMeasure(0, 100000);
            }
            if (mode != 1073741824) {
                return 0;
            }
            return getMeasure(size, size);
        }

        public void layout(int i) {
            setParentConstraints(i, i);
            getLocations();
        }

        public void invalidateStructure() {
            this.maxIndex = Integer.MIN_VALUE;
            this.groupBounds = null;
            this.forwardLinks = null;
            this.backwardLinks = null;
            this.leadingMargins = null;
            this.trailingMargins = null;
            this.arcs = null;
            this.locations = null;
            this.deltas = null;
            this.hasWeightsValid = false;
            invalidateValues();
        }

        public void invalidateValues() {
            this.groupBoundsValid = false;
            this.forwardLinksValid = false;
            this.backwardLinksValid = false;
            this.leadingMarginsValid = false;
            this.trailingMarginsValid = false;
            this.arcsValid = false;
            this.locationsValid = false;
        }
    }

    public static class LayoutParams extends ViewGroup.MarginLayoutParams {
        private static final int BOTTOM_MARGIN = 6;
        private static final int COLUMN = 1;
        private static final int COLUMN_SPAN = 4;
        private static final int COLUMN_WEIGHT = 6;
        private static final int DEFAULT_COLUMN = Integer.MIN_VALUE;
        private static final int DEFAULT_HEIGHT = -2;
        private static final int DEFAULT_MARGIN = Integer.MIN_VALUE;
        private static final int DEFAULT_ROW = Integer.MIN_VALUE;
        private static final Interval DEFAULT_SPAN = new Interval(Integer.MIN_VALUE, AudioSystem.DEVICE_IN_COMMUNICATION);
        private static final int DEFAULT_SPAN_SIZE = DEFAULT_SPAN.size();
        private static final int DEFAULT_WIDTH = -2;
        private static final int GRAVITY = 0;
        private static final int LEFT_MARGIN = 3;
        private static final int MARGIN = 2;
        private static final int RIGHT_MARGIN = 5;
        private static final int ROW = 2;
        private static final int ROW_SPAN = 3;
        private static final int ROW_WEIGHT = 5;
        private static final int TOP_MARGIN = 4;
        public Spec columnSpec;
        public Spec rowSpec;

        private LayoutParams(int i, int i2, int i3, int i4, int i5, int i6, Spec spec, Spec spec2) {
            super(i, i2);
            this.rowSpec = Spec.UNDEFINED;
            this.columnSpec = Spec.UNDEFINED;
            setMargins(i3, i4, i5, i6);
            this.rowSpec = spec;
            this.columnSpec = spec2;
        }

        public LayoutParams(Spec spec, Spec spec2) {
            this(-2, -2, Integer.MIN_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE, spec, spec2);
        }

        public LayoutParams() {
            this(Spec.UNDEFINED, Spec.UNDEFINED);
        }

        public LayoutParams(ViewGroup.LayoutParams layoutParams) {
            super(layoutParams);
            this.rowSpec = Spec.UNDEFINED;
            this.columnSpec = Spec.UNDEFINED;
        }

        public LayoutParams(ViewGroup.MarginLayoutParams marginLayoutParams) {
            super(marginLayoutParams);
            this.rowSpec = Spec.UNDEFINED;
            this.columnSpec = Spec.UNDEFINED;
        }

        public LayoutParams(LayoutParams layoutParams) {
            super((ViewGroup.MarginLayoutParams) layoutParams);
            this.rowSpec = Spec.UNDEFINED;
            this.columnSpec = Spec.UNDEFINED;
            this.rowSpec = layoutParams.rowSpec;
            this.columnSpec = layoutParams.columnSpec;
        }

        public LayoutParams(Context context, AttributeSet attributeSet) {
            super(context, attributeSet);
            this.rowSpec = Spec.UNDEFINED;
            this.columnSpec = Spec.UNDEFINED;
            reInitSuper(context, attributeSet);
            init(context, attributeSet);
        }

        private void reInitSuper(Context context, AttributeSet attributeSet) {
            TypedArray typedArrayObtainStyledAttributes = context.obtainStyledAttributes(attributeSet, R.styleable.ViewGroup_MarginLayout);
            try {
                int dimensionPixelSize = typedArrayObtainStyledAttributes.getDimensionPixelSize(2, Integer.MIN_VALUE);
                this.leftMargin = typedArrayObtainStyledAttributes.getDimensionPixelSize(3, dimensionPixelSize);
                this.topMargin = typedArrayObtainStyledAttributes.getDimensionPixelSize(4, dimensionPixelSize);
                this.rightMargin = typedArrayObtainStyledAttributes.getDimensionPixelSize(5, dimensionPixelSize);
                this.bottomMargin = typedArrayObtainStyledAttributes.getDimensionPixelSize(6, dimensionPixelSize);
            } finally {
                typedArrayObtainStyledAttributes.recycle();
            }
        }

        private void init(Context context, AttributeSet attributeSet) {
            TypedArray typedArrayObtainStyledAttributes = context.obtainStyledAttributes(attributeSet, R.styleable.GridLayout_Layout);
            try {
                int i = typedArrayObtainStyledAttributes.getInt(0, 0);
                this.columnSpec = GridLayout.spec(typedArrayObtainStyledAttributes.getInt(1, Integer.MIN_VALUE), typedArrayObtainStyledAttributes.getInt(4, DEFAULT_SPAN_SIZE), GridLayout.getAlignment(i, true), typedArrayObtainStyledAttributes.getFloat(6, 0.0f));
                this.rowSpec = GridLayout.spec(typedArrayObtainStyledAttributes.getInt(2, Integer.MIN_VALUE), typedArrayObtainStyledAttributes.getInt(3, DEFAULT_SPAN_SIZE), GridLayout.getAlignment(i, false), typedArrayObtainStyledAttributes.getFloat(5, 0.0f));
            } finally {
                typedArrayObtainStyledAttributes.recycle();
            }
        }

        public void setGravity(int i) {
            this.rowSpec = this.rowSpec.copyWriteAlignment(GridLayout.getAlignment(i, false));
            this.columnSpec = this.columnSpec.copyWriteAlignment(GridLayout.getAlignment(i, true));
        }

        @Override
        protected void setBaseAttributes(TypedArray typedArray, int i, int i2) {
            this.width = typedArray.getLayoutDimension(i, -2);
            this.height = typedArray.getLayoutDimension(i2, -2);
        }

        final void setRowSpecSpan(Interval interval) {
            this.rowSpec = this.rowSpec.copyWriteSpan(interval);
        }

        final void setColumnSpecSpan(Interval interval) {
            this.columnSpec = this.columnSpec.copyWriteSpan(interval);
        }

        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null || getClass() != obj.getClass()) {
                return false;
            }
            LayoutParams layoutParams = (LayoutParams) obj;
            if (this.columnSpec.equals(layoutParams.columnSpec) && this.rowSpec.equals(layoutParams.rowSpec)) {
                return true;
            }
            return false;
        }

        public int hashCode() {
            return (31 * this.rowSpec.hashCode()) + this.columnSpec.hashCode();
        }
    }

    static final class Arc {
        public final Interval span;
        public boolean valid = true;
        public final MutableInt value;

        public Arc(Interval interval, MutableInt mutableInt) {
            this.span = interval;
            this.value = mutableInt;
        }

        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append(this.span);
            sb.append(WifiEnterpriseConfig.CA_CERT_ALIAS_DELIMITER);
            sb.append(!this.valid ? "+>" : Session.SUBSESSION_SEPARATION_CHAR);
            sb.append(WifiEnterpriseConfig.CA_CERT_ALIAS_DELIMITER);
            sb.append(this.value);
            return sb.toString();
        }
    }

    static final class MutableInt {
        public int value;

        public MutableInt() {
            reset();
        }

        public MutableInt(int i) {
            this.value = i;
        }

        public void reset() {
            this.value = Integer.MIN_VALUE;
        }

        public String toString() {
            return Integer.toString(this.value);
        }
    }

    static final class Assoc<K, V> extends ArrayList<Pair<K, V>> {
        private final Class<K> keyType;
        private final Class<V> valueType;

        private Assoc(Class<K> cls, Class<V> cls2) {
            this.keyType = cls;
            this.valueType = cls2;
        }

        public static <K, V> Assoc<K, V> of(Class<K> cls, Class<V> cls2) {
            return new Assoc<>(cls, cls2);
        }

        public void put(K k, V v) {
            add(Pair.create(k, v));
        }

        public PackedMap<K, V> pack() {
            int size = size();
            Object[] objArr = (Object[]) Array.newInstance((Class<?>) this.keyType, size);
            Object[] objArr2 = (Object[]) Array.newInstance((Class<?>) this.valueType, size);
            for (int i = 0; i < size; i++) {
                objArr[i] = get(i).first;
                objArr2[i] = get(i).second;
            }
            return new PackedMap<>(objArr, objArr2);
        }
    }

    static final class PackedMap<K, V> {
        public final int[] index;
        public final K[] keys;
        public final V[] values;

        private PackedMap(K[] kArr, V[] vArr) {
            this.index = createIndex(kArr);
            this.keys = (K[]) compact(kArr, this.index);
            this.values = (V[]) compact(vArr, this.index);
        }

        public V getValue(int i) {
            return this.values[this.index[i]];
        }

        private static <K> int[] createIndex(K[] kArr) {
            int length = kArr.length;
            int[] iArr = new int[length];
            HashMap map = new HashMap();
            for (int i = 0; i < length; i++) {
                K k = kArr[i];
                Integer numValueOf = (Integer) map.get(k);
                if (numValueOf == null) {
                    numValueOf = Integer.valueOf(map.size());
                    map.put(k, numValueOf);
                }
                iArr[i] = numValueOf.intValue();
            }
            return iArr;
        }

        private static <K> K[] compact(K[] kArr, int[] iArr) {
            int length = kArr.length;
            K[] kArr2 = (K[]) ((Object[]) Array.newInstance(kArr.getClass().getComponentType(), GridLayout.max2(iArr, -1) + 1));
            for (int i = 0; i < length; i++) {
                kArr2[iArr[i]] = kArr[i];
            }
            return kArr2;
        }
    }

    static class Bounds {
        public int after;
        public int before;
        public int flexibility;

        private Bounds() {
            reset();
        }

        protected void reset() {
            this.before = Integer.MIN_VALUE;
            this.after = Integer.MIN_VALUE;
            this.flexibility = 2;
        }

        protected void include(int i, int i2) {
            this.before = Math.max(this.before, i);
            this.after = Math.max(this.after, i2);
        }

        protected int size(boolean z) {
            if (!z && GridLayout.canStretch(this.flexibility)) {
                return 100000;
            }
            return this.before + this.after;
        }

        protected int getOffset(GridLayout gridLayout, View view, Alignment alignment, int i, boolean z) {
            return this.before - alignment.getAlignmentValue(view, i, gridLayout.getLayoutMode());
        }

        protected final void include(GridLayout gridLayout, View view, Spec spec, Axis axis, int i) {
            this.flexibility &= spec.getFlexibility();
            boolean z = axis.horizontal;
            int alignmentValue = spec.getAbsoluteAlignment(axis.horizontal).getAlignmentValue(view, i, gridLayout.getLayoutMode());
            include(alignmentValue, i - alignmentValue);
        }

        public String toString() {
            return "Bounds{before=" + this.before + ", after=" + this.after + '}';
        }
    }

    static final class Interval {
        public final int max;
        public final int min;

        public Interval(int i, int i2) {
            this.min = i;
            this.max = i2;
        }

        int size() {
            return this.max - this.min;
        }

        Interval inverse() {
            return new Interval(this.max, this.min);
        }

        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null || getClass() != obj.getClass()) {
                return false;
            }
            Interval interval = (Interval) obj;
            if (this.max == interval.max && this.min == interval.min) {
                return true;
            }
            return false;
        }

        public int hashCode() {
            return (31 * this.min) + this.max;
        }

        public String toString() {
            return "[" + this.min + ", " + this.max + "]";
        }
    }

    public static class Spec {
        static final float DEFAULT_WEIGHT = 0.0f;
        static final Spec UNDEFINED = GridLayout.spec(Integer.MIN_VALUE);
        final Alignment alignment;
        final Interval span;
        final boolean startDefined;
        final float weight;

        private Spec(boolean z, Interval interval, Alignment alignment, float f) {
            this.startDefined = z;
            this.span = interval;
            this.alignment = alignment;
            this.weight = f;
        }

        private Spec(boolean z, int i, int i2, Alignment alignment, float f) {
            this(z, new Interval(i, i2 + i), alignment, f);
        }

        private Alignment getAbsoluteAlignment(boolean z) {
            if (this.alignment != GridLayout.UNDEFINED_ALIGNMENT) {
                return this.alignment;
            }
            if (this.weight == 0.0f) {
                return z ? GridLayout.START : GridLayout.BASELINE;
            }
            return GridLayout.FILL;
        }

        final Spec copyWriteSpan(Interval interval) {
            return new Spec(this.startDefined, interval, this.alignment, this.weight);
        }

        final Spec copyWriteAlignment(Alignment alignment) {
            return new Spec(this.startDefined, this.span, alignment, this.weight);
        }

        final int getFlexibility() {
            return (this.alignment == GridLayout.UNDEFINED_ALIGNMENT && this.weight == 0.0f) ? 0 : 2;
        }

        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null || getClass() != obj.getClass()) {
                return false;
            }
            Spec spec = (Spec) obj;
            if (this.alignment.equals(spec.alignment) && this.span.equals(spec.span)) {
                return true;
            }
            return false;
        }

        public int hashCode() {
            return (31 * this.span.hashCode()) + this.alignment.hashCode();
        }
    }

    public static Spec spec(int i, int i2, Alignment alignment, float f) {
        return new Spec(i != Integer.MIN_VALUE, i, i2, alignment, f);
    }

    public static Spec spec(int i, Alignment alignment, float f) {
        return spec(i, 1, alignment, f);
    }

    public static Spec spec(int i, int i2, float f) {
        return spec(i, i2, UNDEFINED_ALIGNMENT, f);
    }

    public static Spec spec(int i, float f) {
        return spec(i, 1, f);
    }

    public static Spec spec(int i, int i2, Alignment alignment) {
        return spec(i, i2, alignment, 0.0f);
    }

    public static Spec spec(int i, Alignment alignment) {
        return spec(i, 1, alignment);
    }

    public static Spec spec(int i, int i2) {
        return spec(i, i2, UNDEFINED_ALIGNMENT);
    }

    public static Spec spec(int i) {
        return spec(i, 1);
    }

    public static abstract class Alignment {
        abstract int getAlignmentValue(View view, int i, int i2);

        abstract int getGravityOffset(View view, int i);

        Alignment() {
        }

        int getSizeInCell(View view, int i, int i2) {
            return i;
        }

        Bounds getBounds() {
            return new Bounds();
        }
    }

    private static Alignment createSwitchingAlignment(final Alignment alignment, final Alignment alignment2) {
        return new Alignment() {
            @Override
            int getGravityOffset(View view, int i) {
                return (!view.isLayoutRtl() ? alignment : alignment2).getGravityOffset(view, i);
            }

            @Override
            public int getAlignmentValue(View view, int i, int i2) {
                return (!view.isLayoutRtl() ? alignment : alignment2).getAlignmentValue(view, i, i2);
            }
        };
    }

    static boolean canStretch(int i) {
        return (i & 2) != 0;
    }
}
