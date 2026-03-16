package android.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Insets;
import android.graphics.Paint;
import android.media.AudioSystem;
import android.util.AttributeSet;
import android.util.LogPrinter;
import android.util.Pair;
import android.util.Printer;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.RemoteViews;
import com.android.internal.R;
import com.android.internal.content.NativeLibraryHelper;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
        public void println(String x) {
        }
    };
    static final Alignment UNDEFINED_ALIGNMENT = new Alignment() {
        @Override
        int getGravityOffset(View view, int cellDelta) {
            return Integer.MIN_VALUE;
        }

        @Override
        public int getAlignmentValue(View view, int viewSize, int mode) {
            return Integer.MIN_VALUE;
        }
    };
    private static final Alignment LEADING = new Alignment() {
        @Override
        int getGravityOffset(View view, int cellDelta) {
            return 0;
        }

        @Override
        public int getAlignmentValue(View view, int viewSize, int mode) {
            return 0;
        }
    };
    private static final Alignment TRAILING = new Alignment() {
        @Override
        int getGravityOffset(View view, int cellDelta) {
            return cellDelta;
        }

        @Override
        public int getAlignmentValue(View view, int viewSize, int mode) {
            return viewSize;
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
        int getGravityOffset(View view, int cellDelta) {
            return cellDelta >> 1;
        }

        @Override
        public int getAlignmentValue(View view, int viewSize, int mode) {
            return viewSize >> 1;
        }
    };
    public static final Alignment BASELINE = new Alignment() {
        @Override
        int getGravityOffset(View view, int cellDelta) {
            return 0;
        }

        @Override
        public int getAlignmentValue(View view, int viewSize, int mode) {
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
                protected void include(int before, int after) {
                    super.include(before, after);
                    this.size = Math.max(this.size, before + after);
                }

                @Override
                protected int size(boolean min) {
                    return Math.max(super.size(min), this.size);
                }

                @Override
                protected int getOffset(GridLayout gl, View c, Alignment a, int size, boolean hrz) {
                    return Math.max(0, super.getOffset(gl, c, a, size, hrz));
                }
            };
        }
    };
    public static final Alignment FILL = new Alignment() {
        @Override
        int getGravityOffset(View view, int cellDelta) {
            return 0;
        }

        @Override
        public int getAlignmentValue(View view, int viewSize, int mode) {
            return Integer.MIN_VALUE;
        }

        @Override
        public int getSizeInCell(View view, int viewSize, int cellSize) {
            return cellSize;
        }
    };

    public GridLayout(Context context) {
        this(context, null);
    }

    public GridLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public GridLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public GridLayout(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        this.mHorizontalAxis = new Axis(true);
        this.mVerticalAxis = new Axis(false);
        this.mOrientation = 0;
        this.mUseDefaultMargins = false;
        this.mAlignmentMode = 1;
        this.mLastLayoutParamsHashCode = 0;
        this.mPrinter = LOG_PRINTER;
        this.mDefaultGap = context.getResources().getDimensionPixelOffset(R.dimen.default_gap);
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.GridLayout, defStyleAttr, defStyleRes);
        try {
            setRowCount(a.getInt(1, Integer.MIN_VALUE));
            setColumnCount(a.getInt(3, Integer.MIN_VALUE));
            setOrientation(a.getInt(0, 0));
            setUseDefaultMargins(a.getBoolean(5, false));
            setAlignmentMode(a.getInt(6, 1));
            setRowOrderPreserved(a.getBoolean(2, true));
            setColumnOrderPreserved(a.getBoolean(4, true));
        } finally {
            a.recycle();
        }
    }

    public int getOrientation() {
        return this.mOrientation;
    }

    public void setOrientation(int orientation) {
        if (this.mOrientation != orientation) {
            this.mOrientation = orientation;
            invalidateStructure();
            requestLayout();
        }
    }

    public int getRowCount() {
        return this.mVerticalAxis.getCount();
    }

    public void setRowCount(int rowCount) {
        this.mVerticalAxis.setCount(rowCount);
        invalidateStructure();
        requestLayout();
    }

    public int getColumnCount() {
        return this.mHorizontalAxis.getCount();
    }

    public void setColumnCount(int columnCount) {
        this.mHorizontalAxis.setCount(columnCount);
        invalidateStructure();
        requestLayout();
    }

    public boolean getUseDefaultMargins() {
        return this.mUseDefaultMargins;
    }

    public void setUseDefaultMargins(boolean useDefaultMargins) {
        this.mUseDefaultMargins = useDefaultMargins;
        requestLayout();
    }

    public int getAlignmentMode() {
        return this.mAlignmentMode;
    }

    public void setAlignmentMode(int alignmentMode) {
        this.mAlignmentMode = alignmentMode;
        requestLayout();
    }

    public boolean isRowOrderPreserved() {
        return this.mVerticalAxis.isOrderPreserved();
    }

    public void setRowOrderPreserved(boolean rowOrderPreserved) {
        this.mVerticalAxis.setOrderPreserved(rowOrderPreserved);
        invalidateStructure();
        requestLayout();
    }

    public boolean isColumnOrderPreserved() {
        return this.mHorizontalAxis.isOrderPreserved();
    }

    public void setColumnOrderPreserved(boolean columnOrderPreserved) {
        this.mHorizontalAxis.setOrderPreserved(columnOrderPreserved);
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

    static int max2(int[] a, int valueIfEmpty) {
        int result = valueIfEmpty;
        for (int i : a) {
            result = Math.max(result, i);
        }
        return result;
    }

    static <T> T[] append(T[] tArr, T[] tArr2) {
        T[] tArr3 = (T[]) ((Object[]) Array.newInstance(tArr.getClass().getComponentType(), tArr.length + tArr2.length));
        System.arraycopy(tArr, 0, tArr3, 0, tArr.length);
        System.arraycopy(tArr2, 0, tArr3, tArr.length, tArr2.length);
        return tArr3;
    }

    static Alignment getAlignment(int gravity, boolean horizontal) {
        int mask = horizontal ? 7 : 112;
        int shift = horizontal ? 0 : 4;
        int flags = (gravity & mask) >> shift;
        switch (flags) {
            case 1:
                return CENTER;
            case 3:
                return horizontal ? LEFT : TOP;
            case 5:
                return horizontal ? RIGHT : BOTTOM;
            case 7:
                return FILL;
            case Gravity.START:
                return START;
            case Gravity.END:
                return END;
            default:
                return UNDEFINED_ALIGNMENT;
        }
    }

    private int getDefaultMargin(View c, boolean horizontal, boolean leading) {
        if (c.getClass() == Space.class) {
            return 0;
        }
        return this.mDefaultGap / 2;
    }

    private int getDefaultMargin(View c, boolean isAtEdge, boolean horizontal, boolean leading) {
        return getDefaultMargin(c, horizontal, leading);
    }

    private int getDefaultMargin(View c, LayoutParams p, boolean horizontal, boolean leading) {
        boolean isAtEdge = true;
        if (!this.mUseDefaultMargins) {
            return 0;
        }
        Spec spec = horizontal ? p.columnSpec : p.rowSpec;
        Axis axis = horizontal ? this.mHorizontalAxis : this.mVerticalAxis;
        Interval span = spec.span;
        boolean leading1 = (horizontal && isLayoutRtl()) ? !leading : leading;
        if (leading1) {
            if (span.min != 0) {
                isAtEdge = false;
            }
        } else if (span.max != axis.getCount()) {
            isAtEdge = false;
        }
        return getDefaultMargin(c, isAtEdge, horizontal, leading);
    }

    int getMargin1(View view, boolean horizontal, boolean leading) {
        int margin;
        LayoutParams lp = getLayoutParams(view);
        if (horizontal) {
            margin = leading ? lp.leftMargin : lp.rightMargin;
        } else {
            margin = leading ? lp.topMargin : lp.bottomMargin;
        }
        if (margin != Integer.MIN_VALUE) {
            return margin;
        }
        int margin2 = getDefaultMargin(view, lp, horizontal, leading);
        return margin2;
    }

    private int getMargin(View view, boolean horizontal, boolean leading) {
        if (this.mAlignmentMode == 1) {
            return getMargin1(view, horizontal, leading);
        }
        Axis axis = horizontal ? this.mHorizontalAxis : this.mVerticalAxis;
        int[] margins = leading ? axis.getLeadingMargins() : axis.getTrailingMargins();
        LayoutParams lp = getLayoutParams(view);
        Spec spec = horizontal ? lp.columnSpec : lp.rowSpec;
        int index = leading ? spec.span.min : spec.span.max;
        return margins[index];
    }

    private int getTotalMargin(View child, boolean horizontal) {
        return getMargin(child, horizontal, true) + getMargin(child, horizontal, false);
    }

    private static boolean fits(int[] a, int value, int start, int end) {
        if (end > a.length) {
            return false;
        }
        for (int i = start; i < end; i++) {
            if (a[i] > value) {
                return false;
            }
        }
        return true;
    }

    private static void procrusteanFill(int[] a, int start, int end, int value) {
        int length = a.length;
        Arrays.fill(a, Math.min(start, length), Math.min(end, length), value);
    }

    private static void setCellGroup(LayoutParams lp, int row, int rowSpan, int col, int colSpan) {
        lp.setRowSpecSpan(new Interval(row, row + rowSpan));
        lp.setColumnSpecSpan(new Interval(col, col + colSpan));
    }

    private static int clip(Interval minorRange, boolean minorWasDefined, int count) {
        int size = minorRange.size();
        if (count != 0) {
            int min = minorWasDefined ? Math.min(minorRange.min, count) : 0;
            return Math.min(size, count - min);
        }
        return size;
    }

    private void validateLayoutParams() {
        boolean horizontal = this.mOrientation == 0;
        Axis axis = horizontal ? this.mHorizontalAxis : this.mVerticalAxis;
        int count = axis.definedCount != Integer.MIN_VALUE ? axis.definedCount : 0;
        int major = 0;
        int minor = 0;
        int[] maxSizes = new int[count];
        int N = getChildCount();
        for (int i = 0; i < N; i++) {
            LayoutParams lp = (LayoutParams) getChildAt(i).getLayoutParams();
            Spec majorSpec = horizontal ? lp.rowSpec : lp.columnSpec;
            Interval majorRange = majorSpec.span;
            boolean majorWasDefined = majorSpec.startDefined;
            int majorSpan = majorRange.size();
            if (majorWasDefined) {
                major = majorRange.min;
            }
            Spec minorSpec = horizontal ? lp.columnSpec : lp.rowSpec;
            Interval minorRange = minorSpec.span;
            boolean minorWasDefined = minorSpec.startDefined;
            int minorSpan = clip(minorRange, minorWasDefined, count);
            if (minorWasDefined) {
                minor = minorRange.min;
            }
            if (count != 0) {
                if (!majorWasDefined || !minorWasDefined) {
                    while (!fits(maxSizes, major, minor, minor + minorSpan)) {
                        if (minorWasDefined) {
                            major++;
                        } else if (minor + minorSpan <= count) {
                            minor++;
                        } else {
                            minor = 0;
                            major++;
                        }
                    }
                }
                procrusteanFill(maxSizes, minor, minor + minorSpan, major + majorSpan);
            }
            if (horizontal) {
                setCellGroup(lp, major, majorSpan, minor, minorSpan);
            } else {
                setCellGroup(lp, minor, minorSpan, major, majorSpan);
            }
            minor += minorSpan;
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
    protected void onSetLayoutParams(View child, ViewGroup.LayoutParams layoutParams) {
        super.onSetLayoutParams(child, layoutParams);
        if (!checkLayoutParams(layoutParams)) {
            handleInvalidParams("supplied LayoutParams are of the wrong type");
        }
        invalidateStructure();
    }

    final LayoutParams getLayoutParams(View c) {
        return (LayoutParams) c.getLayoutParams();
    }

    private static void handleInvalidParams(String msg) {
        throw new IllegalArgumentException(msg + ". ");
    }

    private void checkLayoutParams(LayoutParams lp, boolean horizontal) {
        String groupName = horizontal ? "column" : "row";
        Spec spec = horizontal ? lp.columnSpec : lp.rowSpec;
        Interval span = spec.span;
        if (span.min != Integer.MIN_VALUE && span.min < 0) {
            handleInvalidParams(groupName + " indices must be positive");
        }
        Axis axis = horizontal ? this.mHorizontalAxis : this.mVerticalAxis;
        int count = axis.definedCount;
        if (count != Integer.MIN_VALUE) {
            if (span.max > count) {
                handleInvalidParams(groupName + " indices (start + span) mustn't exceed the " + groupName + " count");
            }
            if (span.size() > count) {
                handleInvalidParams(groupName + " span mustn't exceed the " + groupName + " count");
            }
        }
    }

    @Override
    protected boolean checkLayoutParams(ViewGroup.LayoutParams p) {
        if (!(p instanceof LayoutParams)) {
            return false;
        }
        LayoutParams lp = (LayoutParams) p;
        checkLayoutParams(lp, true);
        checkLayoutParams(lp, false);
        return true;
    }

    @Override
    protected LayoutParams generateDefaultLayoutParams() {
        return new LayoutParams();
    }

    @Override
    public LayoutParams generateLayoutParams(AttributeSet attrs) {
        return new LayoutParams(getContext(), attrs);
    }

    @Override
    protected LayoutParams generateLayoutParams(ViewGroup.LayoutParams p) {
        return new LayoutParams(p);
    }

    private void drawLine(Canvas graphics, int x1, int y1, int x2, int y2, Paint paint) {
        if (isLayoutRtl()) {
            int width = getWidth();
            graphics.drawLine(width - x1, y1, width - x2, y2, paint);
        } else {
            graphics.drawLine(x1, y1, x2, y2, paint);
        }
    }

    @Override
    protected void onDebugDrawMargins(Canvas canvas, Paint paint) {
        LayoutParams lp = new LayoutParams();
        for (int i = 0; i < getChildCount(); i++) {
            View c = getChildAt(i);
            lp.setMargins(getMargin1(c, true, true), getMargin1(c, false, true), getMargin1(c, true, false), getMargin1(c, false, false));
            lp.onDebugDraw(c, canvas, paint);
        }
    }

    @Override
    protected void onDebugDraw(Canvas canvas) {
        Paint paint = new Paint();
        paint.setStyle(Paint.Style.STROKE);
        paint.setColor(Color.argb(50, 255, 255, 255));
        Insets insets = getOpticalInsets();
        int top = getPaddingTop() + insets.top;
        int left = getPaddingLeft() + insets.left;
        int right = (getWidth() - getPaddingRight()) - insets.right;
        int bottom = (getHeight() - getPaddingBottom()) - insets.bottom;
        int[] xs = this.mHorizontalAxis.locations;
        if (xs != null) {
            for (int i : xs) {
                int x = left + i;
                drawLine(canvas, x, top, x, bottom, paint);
            }
        }
        int[] ys = this.mVerticalAxis.locations;
        if (ys != null) {
            for (int i2 : ys) {
                int y = top + i2;
                drawLine(canvas, left, y, right, y, paint);
            }
        }
        super.onDebugDraw(canvas);
    }

    @Override
    protected void onViewAdded(View child) {
        super.onViewAdded(child);
        invalidateStructure();
    }

    @Override
    protected void onViewRemoved(View child) {
        super.onViewRemoved(child);
        invalidateStructure();
    }

    @Override
    protected void onChildVisibilityChanged(View child, int oldVisibility, int newVisibility) {
        super.onChildVisibilityChanged(child, oldVisibility, newVisibility);
        if (oldVisibility == 8 || newVisibility == 8) {
            invalidateStructure();
        }
    }

    private int computeLayoutParamsHashCode() {
        int result = 1;
        int N = getChildCount();
        for (int i = 0; i < N; i++) {
            View c = getChildAt(i);
            if (c.getVisibility() != 8) {
                LayoutParams lp = (LayoutParams) c.getLayoutParams();
                result = (result * 31) + lp.hashCode();
            }
        }
        return result;
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

    private void measureChildWithMargins2(View child, int parentWidthSpec, int parentHeightSpec, int childWidth, int childHeight) {
        int childWidthSpec = getChildMeasureSpec(parentWidthSpec, getTotalMargin(child, true), childWidth);
        int childHeightSpec = getChildMeasureSpec(parentHeightSpec, getTotalMargin(child, false), childHeight);
        child.measure(childWidthSpec, childHeightSpec);
    }

    private void measureChildrenWithMargins(int widthSpec, int heightSpec, boolean firstPass) {
        int N = getChildCount();
        for (int i = 0; i < N; i++) {
            View c = getChildAt(i);
            if (c.getVisibility() != 8) {
                LayoutParams lp = getLayoutParams(c);
                if (firstPass) {
                    measureChildWithMargins2(c, widthSpec, heightSpec, lp.width, lp.height);
                    this.mHorizontalAxis.recordOriginalMeasurement(i);
                    this.mVerticalAxis.recordOriginalMeasurement(i);
                } else {
                    boolean horizontal = this.mOrientation == 0;
                    Spec spec = horizontal ? lp.columnSpec : lp.rowSpec;
                    if (spec.alignment == FILL) {
                        Interval span = spec.span;
                        Axis axis = horizontal ? this.mHorizontalAxis : this.mVerticalAxis;
                        int[] locations = axis.getLocations();
                        int cellSize = locations[span.max] - locations[span.min];
                        int viewSize = cellSize - getTotalMargin(c, horizontal);
                        if (horizontal) {
                            measureChildWithMargins2(c, widthSpec, heightSpec, viewSize, lp.height);
                        } else {
                            measureChildWithMargins2(c, widthSpec, heightSpec, lp.width, viewSize);
                        }
                    }
                }
            }
        }
    }

    static int adjust(int measureSpec, int delta) {
        return View.MeasureSpec.makeMeasureSpec(View.MeasureSpec.getSize(measureSpec + delta), View.MeasureSpec.getMode(measureSpec));
    }

    @Override
    protected void onMeasure(int widthSpec, int heightSpec) {
        int heightSansPadding;
        int widthSansPadding;
        consistencyCheck();
        invalidateValues();
        int hPadding = getPaddingLeft() + getPaddingRight();
        int vPadding = getPaddingTop() + getPaddingBottom();
        int widthSpecSansPadding = adjust(widthSpec, -hPadding);
        int heightSpecSansPadding = adjust(heightSpec, -vPadding);
        measureChildrenWithMargins(widthSpecSansPadding, heightSpecSansPadding, true);
        if (this.mOrientation == 0) {
            widthSansPadding = this.mHorizontalAxis.getMeasure(widthSpecSansPadding);
            measureChildrenWithMargins(widthSpecSansPadding, heightSpecSansPadding, false);
            heightSansPadding = this.mVerticalAxis.getMeasure(heightSpecSansPadding);
        } else {
            heightSansPadding = this.mVerticalAxis.getMeasure(heightSpecSansPadding);
            measureChildrenWithMargins(widthSpecSansPadding, heightSpecSansPadding, false);
            widthSansPadding = this.mHorizontalAxis.getMeasure(widthSpecSansPadding);
        }
        int measuredWidth = Math.max(widthSansPadding + hPadding, getSuggestedMinimumWidth());
        int measuredHeight = Math.max(heightSansPadding + vPadding, getSuggestedMinimumHeight());
        setMeasuredDimension(resolveSizeAndState(measuredWidth, widthSpec, 0), resolveSizeAndState(measuredHeight, heightSpec, 0));
    }

    private int getMeasurement(View c, boolean horizontal) {
        return horizontal ? c.getMeasuredWidth() : c.getMeasuredHeight();
    }

    final int getMeasurementIncludingMargin(View c, boolean horizontal) {
        if (c.getVisibility() == 8) {
            return 0;
        }
        return getMeasurement(c, horizontal) + getTotalMargin(c, horizontal);
    }

    @Override
    public void requestLayout() {
        super.requestLayout();
        invalidateValues();
    }

    final Alignment getAlignment(Alignment alignment, boolean horizontal) {
        return alignment != UNDEFINED_ALIGNMENT ? alignment : horizontal ? START : BASELINE;
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        consistencyCheck();
        int targetWidth = right - left;
        int targetHeight = bottom - top;
        int paddingLeft = getPaddingLeft();
        int paddingTop = getPaddingTop();
        int paddingRight = getPaddingRight();
        int paddingBottom = getPaddingBottom();
        this.mHorizontalAxis.layout((targetWidth - paddingLeft) - paddingRight);
        this.mVerticalAxis.layout((targetHeight - paddingTop) - paddingBottom);
        int[] hLocations = this.mHorizontalAxis.getLocations();
        int[] vLocations = this.mVerticalAxis.getLocations();
        int N = getChildCount();
        for (int i = 0; i < N; i++) {
            View c = getChildAt(i);
            if (c.getVisibility() != 8) {
                LayoutParams lp = getLayoutParams(c);
                Spec columnSpec = lp.columnSpec;
                Spec rowSpec = lp.rowSpec;
                Interval colSpan = columnSpec.span;
                Interval rowSpan = rowSpec.span;
                int x1 = hLocations[colSpan.min];
                int y1 = vLocations[rowSpan.min];
                int x2 = hLocations[colSpan.max];
                int y2 = vLocations[rowSpan.max];
                int cellWidth = x2 - x1;
                int cellHeight = y2 - y1;
                int pWidth = getMeasurement(c, true);
                int pHeight = getMeasurement(c, false);
                Alignment hAlign = getAlignment(columnSpec.alignment, true);
                Alignment vAlign = getAlignment(rowSpec.alignment, false);
                Bounds boundsX = this.mHorizontalAxis.getGroupBounds().getValue(i);
                Bounds boundsY = this.mVerticalAxis.getGroupBounds().getValue(i);
                int gravityOffsetX = hAlign.getGravityOffset(c, cellWidth - boundsX.size(true));
                int gravityOffsetY = vAlign.getGravityOffset(c, cellHeight - boundsY.size(true));
                int leftMargin = getMargin(c, true, true);
                int topMargin = getMargin(c, false, true);
                int rightMargin = getMargin(c, true, false);
                int bottomMargin = getMargin(c, false, false);
                int sumMarginsX = leftMargin + rightMargin;
                int sumMarginsY = topMargin + bottomMargin;
                int alignmentOffsetX = boundsX.getOffset(this, c, hAlign, pWidth + sumMarginsX, true);
                int alignmentOffsetY = boundsY.getOffset(this, c, vAlign, pHeight + sumMarginsY, false);
                int width = hAlign.getSizeInCell(c, pWidth, cellWidth - sumMarginsX);
                int height = vAlign.getSizeInCell(c, pHeight, cellHeight - sumMarginsY);
                int dx = x1 + gravityOffsetX + alignmentOffsetX;
                int cx = !isLayoutRtl() ? paddingLeft + leftMargin + dx : (((targetWidth - width) - paddingRight) - rightMargin) - dx;
                int cy = paddingTop + y1 + gravityOffsetY + alignmentOffsetY + topMargin;
                if (width != c.getMeasuredWidth() || height != c.getMeasuredHeight()) {
                    c.measure(View.MeasureSpec.makeMeasureSpec(width, 1073741824), View.MeasureSpec.makeMeasureSpec(height, 1073741824));
                }
                c.layout(cx, cy, cx + width, cy + height);
            }
        }
    }

    @Override
    public void onInitializeAccessibilityEvent(AccessibilityEvent event) {
        super.onInitializeAccessibilityEvent(event);
        event.setClassName(GridLayout.class.getName());
    }

    @Override
    public void onInitializeAccessibilityNodeInfo(AccessibilityNodeInfo info) {
        super.onInitializeAccessibilityNodeInfo(info);
        info.setClassName(GridLayout.class.getName());
    }

    final class Axis {
        static final boolean $assertionsDisabled;
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
        public int[] originalMeasurements;
        private MutableInt parentMax;
        private MutableInt parentMin;
        public int[] trailingMargins;
        public boolean trailingMarginsValid;

        static {
            $assertionsDisabled = !GridLayout.class.desiredAssertionStatus();
        }

        private Axis(boolean horizontal) {
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
            this.horizontal = horizontal;
        }

        private int calculateMaxIndex() {
            int result = -1;
            int N = GridLayout.this.getChildCount();
            for (int i = 0; i < N; i++) {
                View c = GridLayout.this.getChildAt(i);
                LayoutParams params = GridLayout.this.getLayoutParams(c);
                Spec spec = this.horizontal ? params.columnSpec : params.rowSpec;
                Interval span = spec.span;
                result = Math.max(Math.max(Math.max(result, span.min), span.max), span.size());
            }
            if (result == -1) {
                return Integer.MIN_VALUE;
            }
            return result;
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

        public void setCount(int count) {
            if (count != Integer.MIN_VALUE && count < getMaxIndex()) {
                GridLayout.handleInvalidParams((this.horizontal ? "column" : "row") + "Count must be greater than or equal to the maximum of all grid indices (and spans) defined in the LayoutParams of each child");
            }
            this.definedCount = count;
        }

        public boolean isOrderPreserved() {
            return this.orderPreserved;
        }

        public void setOrderPreserved(boolean orderPreserved) {
            this.orderPreserved = orderPreserved;
            invalidateStructure();
        }

        private PackedMap<Spec, Bounds> createGroupBounds() {
            Assoc<Spec, Bounds> assoc = Assoc.of(Spec.class, Bounds.class);
            int N = GridLayout.this.getChildCount();
            for (int i = 0; i < N; i++) {
                View c = GridLayout.this.getChildAt(i);
                LayoutParams lp = GridLayout.this.getLayoutParams(c);
                Spec spec = this.horizontal ? lp.columnSpec : lp.rowSpec;
                Bounds bounds = GridLayout.this.getAlignment(spec.alignment, this.horizontal).getBounds();
                assoc.put(spec, bounds);
            }
            return assoc.pack();
        }

        private void computeGroupBounds() {
            Bounds[] values = this.groupBounds.values;
            for (Bounds bounds : values) {
                bounds.reset();
            }
            int N = GridLayout.this.getChildCount();
            for (int i = 0; i < N; i++) {
                View c = GridLayout.this.getChildAt(i);
                LayoutParams lp = GridLayout.this.getLayoutParams(c);
                Spec spec = this.horizontal ? lp.columnSpec : lp.rowSpec;
                int size = spec.weight == 0.0f ? GridLayout.this.getMeasurementIncludingMargin(c, this.horizontal) : getOriginalMeasurements()[i] + getDeltas()[i];
                this.groupBounds.getValue(i).include(GridLayout.this, c, spec, this, size);
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

        private PackedMap<Interval, MutableInt> createLinks(boolean min) {
            Assoc<Interval, MutableInt> result = Assoc.of(Interval.class, MutableInt.class);
            Spec[] keys = getGroupBounds().keys;
            int N = keys.length;
            for (int i = 0; i < N; i++) {
                Interval span = min ? keys[i].span : keys[i].span.inverse();
                result.put(span, new MutableInt());
            }
            return result.pack();
        }

        private void computeLinks(PackedMap<Interval, MutableInt> links, boolean min) {
            MutableInt[] spans = links.values;
            for (MutableInt mutableInt : spans) {
                mutableInt.reset();
            }
            Bounds[] bounds = getGroupBounds().values;
            for (int i = 0; i < bounds.length; i++) {
                int size = bounds[i].size(min);
                MutableInt valueHolder = links.getValue(i);
                int i2 = valueHolder.value;
                if (!min) {
                    size = -size;
                }
                valueHolder.value = Math.max(i2, size);
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

        private void include(List<Arc> arcs, Interval key, MutableInt size, boolean ignoreIfAlreadyPresent) {
            if (key.size() != 0) {
                if (ignoreIfAlreadyPresent) {
                    for (Arc arc : arcs) {
                        Interval span = arc.span;
                        if (span.equals(key)) {
                            return;
                        }
                    }
                }
                arcs.add(new Arc(key, size));
            }
        }

        private void include(List<Arc> arcs, Interval key, MutableInt size) {
            include(arcs, key, size, true);
        }

        Arc[][] groupArcsByFirstVertex(Arc[] arcs) {
            int N = getCount() + 1;
            Arc[][] result = new Arc[N][];
            int[] sizes = new int[N];
            for (Arc arc : arcs) {
                int i = arc.span.min;
                sizes[i] = sizes[i] + 1;
            }
            for (int i2 = 0; i2 < sizes.length; i2++) {
                result[i2] = new Arc[sizes[i2]];
            }
            Arrays.fill(sizes, 0);
            for (Arc arc2 : arcs) {
                int i3 = arc2.span.min;
                Arc[] arcArr = result[i3];
                int i4 = sizes[i3];
                sizes[i3] = i4 + 1;
                arcArr[i4] = arc2;
            }
            return result;
        }

        private Arc[] topologicalSort(final Arc[] arcs) {
            return new Object() {
                static final boolean $assertionsDisabled;
                Arc[][] arcsByVertex;
                int cursor;
                Arc[] result;
                int[] visited;

                static {
                    $assertionsDisabled = !GridLayout.class.desiredAssertionStatus();
                }

                {
                    this.result = new Arc[arcs.length];
                    this.cursor = this.result.length - 1;
                    this.arcsByVertex = Axis.this.groupArcsByFirstVertex(arcs);
                    this.visited = new int[Axis.this.getCount() + 1];
                }

                void walk(int loc) {
                    switch (this.visited[loc]) {
                        case 0:
                            this.visited[loc] = 1;
                            Arc[] arr$ = this.arcsByVertex[loc];
                            for (Arc arc : arr$) {
                                walk(arc.span.max);
                                Arc[] arcArr = this.result;
                                int i = this.cursor;
                                this.cursor = i - 1;
                                arcArr[i] = arc;
                            }
                            this.visited[loc] = 2;
                            return;
                        case 1:
                            if (!$assertionsDisabled) {
                                throw new AssertionError();
                            }
                            return;
                        default:
                            return;
                    }
                }

                Arc[] sort() {
                    int N = this.arcsByVertex.length;
                    for (int loc = 0; loc < N; loc++) {
                        walk(loc);
                    }
                    if ($assertionsDisabled || this.cursor == -1) {
                        return this.result;
                    }
                    throw new AssertionError();
                }
            }.sort();
        }

        private Arc[] topologicalSort(List<Arc> arcs) {
            return topologicalSort((Arc[]) arcs.toArray(new Arc[arcs.size()]));
        }

        private void addComponentSizes(List<Arc> result, PackedMap<Interval, MutableInt> links) {
            for (int i = 0; i < links.keys.length; i++) {
                Interval key = links.keys[i];
                include(result, key, links.values[i], false);
            }
        }

        private Arc[] createArcs() {
            List<Arc> mins = new ArrayList<>();
            List<Arc> maxs = new ArrayList<>();
            addComponentSizes(mins, getForwardLinks());
            addComponentSizes(maxs, getBackwardLinks());
            if (this.orderPreserved) {
                for (int i = 0; i < getCount(); i++) {
                    include(mins, new Interval(i, i + 1), new MutableInt(0));
                }
            }
            int N = getCount();
            include(mins, new Interval(0, N), this.parentMin, false);
            include(maxs, new Interval(N, 0), this.parentMax, false);
            Arc[] sMins = topologicalSort(mins);
            Arc[] sMaxs = topologicalSort(maxs);
            return (Arc[]) GridLayout.append(sMins, sMaxs);
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

        private boolean relax(int[] locations, Arc entry) {
            if (!entry.valid) {
                return false;
            }
            Interval span = entry.span;
            int u = span.min;
            int v = span.max;
            int value = entry.value.value;
            int candidate = locations[u] + value;
            if (candidate <= locations[v]) {
                return false;
            }
            locations[v] = candidate;
            return true;
        }

        private void init(int[] locations) {
            Arrays.fill(locations, 0);
        }

        private String arcsToString(List<Arc> arcs) {
            String var = this.horizontal ? "x" : "y";
            StringBuilder result = new StringBuilder();
            boolean first = true;
            for (Arc arc : arcs) {
                if (first) {
                    first = false;
                } else {
                    result = result.append(", ");
                }
                int src = arc.span.min;
                int dst = arc.span.max;
                int value = arc.value.value;
                result.append(src < dst ? var + dst + NativeLibraryHelper.CLEAR_ABI_OVERRIDE + var + src + ">=" + value : var + src + NativeLibraryHelper.CLEAR_ABI_OVERRIDE + var + dst + "<=" + (-value));
            }
            return result.toString();
        }

        private void logError(String axisName, Arc[] arcs, boolean[] culprits0) {
            List<Arc> culprits = new ArrayList<>();
            List<Arc> removed = new ArrayList<>();
            for (int c = 0; c < arcs.length; c++) {
                Arc arc = arcs[c];
                if (culprits0[c]) {
                    culprits.add(arc);
                }
                if (!arc.valid) {
                    removed.add(arc);
                }
            }
            GridLayout.this.mPrinter.println(axisName + " constraints: " + arcsToString(culprits) + " are inconsistent; permanently removing: " + arcsToString(removed) + ". ");
        }

        private boolean solve(Arc[] arcs, int[] locations) {
            return solve(arcs, locations, true);
        }

        private boolean solve(Arc[] arcs, int[] locations, boolean modifyOnError) {
            String axisName = this.horizontal ? "horizontal" : "vertical";
            int N = getCount() + 1;
            boolean[] originalCulprits = null;
            for (int p = 0; p < arcs.length; p++) {
                init(locations);
                for (int i = 0; i < N; i++) {
                    boolean changed = false;
                    for (Arc arc : arcs) {
                        changed |= relax(locations, arc);
                    }
                    if (!changed) {
                        if (originalCulprits != null) {
                            logError(axisName, arcs, originalCulprits);
                        }
                        return true;
                    }
                }
                if (!modifyOnError) {
                    return false;
                }
                boolean[] culprits = new boolean[arcs.length];
                for (int i2 = 0; i2 < N; i2++) {
                    int length = arcs.length;
                    for (int j = 0; j < length; j++) {
                        culprits[j] = culprits[j] | relax(locations, arcs[j]);
                    }
                }
                if (p == 0) {
                    originalCulprits = culprits;
                }
                int i3 = 0;
                while (true) {
                    if (i3 >= arcs.length) {
                        break;
                    }
                    if (culprits[i3]) {
                        Arc arc2 = arcs[i3];
                        if (arc2.span.min >= arc2.span.max) {
                            arc2.valid = false;
                            break;
                        }
                    }
                    i3++;
                }
            }
            return true;
        }

        private void computeMargins(boolean leading) {
            int[] margins = leading ? this.leadingMargins : this.trailingMargins;
            int N = GridLayout.this.getChildCount();
            for (int i = 0; i < N; i++) {
                View c = GridLayout.this.getChildAt(i);
                if (c.getVisibility() != 8) {
                    LayoutParams lp = GridLayout.this.getLayoutParams(c);
                    Spec spec = this.horizontal ? lp.columnSpec : lp.rowSpec;
                    Interval span = spec.span;
                    int index = leading ? span.min : span.max;
                    margins[index] = Math.max(margins[index], GridLayout.this.getMargin1(c, this.horizontal, leading));
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

        private boolean solve(int[] a) {
            return solve(getArcs(), a);
        }

        private boolean computeHasWeights() {
            int N = GridLayout.this.getChildCount();
            for (int i = 0; i < N; i++) {
                LayoutParams lp = GridLayout.this.getLayoutParams(GridLayout.this.getChildAt(i));
                Spec spec = this.horizontal ? lp.columnSpec : lp.rowSpec;
                if (spec.weight != 0.0f) {
                    return true;
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

        public int[] getOriginalMeasurements() {
            if (this.originalMeasurements == null) {
                this.originalMeasurements = new int[GridLayout.this.getChildCount()];
            }
            return this.originalMeasurements;
        }

        private void recordOriginalMeasurement(int i) {
            if (hasWeights()) {
                getOriginalMeasurements()[i] = GridLayout.this.getMeasurementIncludingMargin(GridLayout.this.getChildAt(i), this.horizontal);
            }
        }

        public int[] getDeltas() {
            if (this.deltas == null) {
                this.deltas = new int[GridLayout.this.getChildCount()];
            }
            return this.deltas;
        }

        private void shareOutDelta(int totalDelta, float totalWeight) {
            Arrays.fill(this.deltas, 0);
            int N = GridLayout.this.getChildCount();
            for (int i = 0; i < N; i++) {
                View c = GridLayout.this.getChildAt(i);
                LayoutParams lp = GridLayout.this.getLayoutParams(c);
                Spec spec = this.horizontal ? lp.columnSpec : lp.rowSpec;
                float weight = spec.weight;
                if (weight != 0.0f) {
                    int delta = Math.round((totalDelta * weight) / totalWeight);
                    this.deltas[i] = delta;
                    totalDelta -= delta;
                    totalWeight -= weight;
                }
            }
        }

        private void solveAndDistributeSpace(int[] a) {
            Arrays.fill(getDeltas(), 0);
            solve(a);
            int deltaMax = (this.parentMin.value * GridLayout.this.getChildCount()) + 1;
            if (deltaMax >= 2) {
                int deltaMin = 0;
                float totalWeight = calculateTotalWeight();
                int validDelta = -1;
                boolean validSolution = true;
                while (deltaMin < deltaMax) {
                    int delta = (deltaMin + deltaMax) / 2;
                    invalidateValues();
                    shareOutDelta(delta, totalWeight);
                    validSolution = solve(getArcs(), a, false);
                    if (validSolution) {
                        validDelta = delta;
                        deltaMin = delta + 1;
                    } else {
                        deltaMax = delta;
                    }
                }
                if (validDelta > 0 && !validSolution) {
                    invalidateValues();
                    shareOutDelta(validDelta, totalWeight);
                    solve(a);
                }
            }
        }

        private float calculateTotalWeight() {
            float totalWeight = 0.0f;
            int N = GridLayout.this.getChildCount();
            for (int i = 0; i < N; i++) {
                View c = GridLayout.this.getChildAt(i);
                LayoutParams lp = GridLayout.this.getLayoutParams(c);
                Spec spec = this.horizontal ? lp.columnSpec : lp.rowSpec;
                totalWeight += spec.weight;
            }
            return totalWeight;
        }

        private void computeLocations(int[] a) {
            if (!hasWeights()) {
                solve(a);
            } else {
                solveAndDistributeSpace(a);
            }
            if (!this.orderPreserved) {
                int a0 = a[0];
                int N = a.length;
                for (int i = 0; i < N; i++) {
                    a[i] = a[i] - a0;
                }
            }
        }

        public int[] getLocations() {
            if (this.locations == null) {
                int N = getCount() + 1;
                this.locations = new int[N];
            }
            if (!this.locationsValid) {
                computeLocations(this.locations);
                this.locationsValid = true;
            }
            return this.locations;
        }

        private int size(int[] locations) {
            return locations[getCount()];
        }

        private void setParentConstraints(int min, int max) {
            this.parentMin.value = min;
            this.parentMax.value = -max;
            this.locationsValid = false;
        }

        private int getMeasure(int min, int max) {
            setParentConstraints(min, max);
            return size(getLocations());
        }

        public int getMeasure(int measureSpec) {
            int mode = View.MeasureSpec.getMode(measureSpec);
            int size = View.MeasureSpec.getSize(measureSpec);
            switch (mode) {
                case Integer.MIN_VALUE:
                    return getMeasure(0, size);
                case 0:
                    return getMeasure(0, 100000);
                case 1073741824:
                    return getMeasure(size, size);
                default:
                    if ($assertionsDisabled) {
                        return 0;
                    }
                    throw new AssertionError();
            }
        }

        public void layout(int size) {
            setParentConstraints(size, size);
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
            this.originalMeasurements = null;
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

        private LayoutParams(int width, int height, int left, int top, int right, int bottom, Spec rowSpec, Spec columnSpec) {
            super(width, height);
            this.rowSpec = Spec.UNDEFINED;
            this.columnSpec = Spec.UNDEFINED;
            setMargins(left, top, right, bottom);
            this.rowSpec = rowSpec;
            this.columnSpec = columnSpec;
        }

        public LayoutParams(Spec rowSpec, Spec columnSpec) {
            this(-2, -2, Integer.MIN_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE, rowSpec, columnSpec);
        }

        public LayoutParams() {
            this(Spec.UNDEFINED, Spec.UNDEFINED);
        }

        public LayoutParams(ViewGroup.LayoutParams params) {
            super(params);
            this.rowSpec = Spec.UNDEFINED;
            this.columnSpec = Spec.UNDEFINED;
        }

        public LayoutParams(ViewGroup.MarginLayoutParams params) {
            super(params);
            this.rowSpec = Spec.UNDEFINED;
            this.columnSpec = Spec.UNDEFINED;
        }

        public LayoutParams(LayoutParams source) {
            super((ViewGroup.MarginLayoutParams) source);
            this.rowSpec = Spec.UNDEFINED;
            this.columnSpec = Spec.UNDEFINED;
            this.rowSpec = source.rowSpec;
            this.columnSpec = source.columnSpec;
        }

        public LayoutParams(Context context, AttributeSet attrs) {
            super(context, attrs);
            this.rowSpec = Spec.UNDEFINED;
            this.columnSpec = Spec.UNDEFINED;
            reInitSuper(context, attrs);
            init(context, attrs);
        }

        private void reInitSuper(Context context, AttributeSet attrs) {
            TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.ViewGroup_MarginLayout);
            try {
                int margin = a.getDimensionPixelSize(2, Integer.MIN_VALUE);
                this.leftMargin = a.getDimensionPixelSize(3, margin);
                this.topMargin = a.getDimensionPixelSize(4, margin);
                this.rightMargin = a.getDimensionPixelSize(5, margin);
                this.bottomMargin = a.getDimensionPixelSize(6, margin);
            } finally {
                a.recycle();
            }
        }

        private void init(Context context, AttributeSet attrs) {
            TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.GridLayout_Layout);
            try {
                int gravity = a.getInt(0, 0);
                int column = a.getInt(1, Integer.MIN_VALUE);
                int colSpan = a.getInt(4, DEFAULT_SPAN_SIZE);
                float colWeight = a.getFloat(6, 0.0f);
                this.columnSpec = GridLayout.spec(column, colSpan, GridLayout.getAlignment(gravity, true), colWeight);
                int row = a.getInt(2, Integer.MIN_VALUE);
                int rowSpan = a.getInt(3, DEFAULT_SPAN_SIZE);
                float rowWeight = a.getFloat(5, 0.0f);
                this.rowSpec = GridLayout.spec(row, rowSpan, GridLayout.getAlignment(gravity, false), rowWeight);
            } finally {
                a.recycle();
            }
        }

        public void setGravity(int gravity) {
            this.rowSpec = this.rowSpec.copyWriteAlignment(GridLayout.getAlignment(gravity, false));
            this.columnSpec = this.columnSpec.copyWriteAlignment(GridLayout.getAlignment(gravity, true));
        }

        @Override
        protected void setBaseAttributes(TypedArray attributes, int widthAttr, int heightAttr) {
            this.width = attributes.getLayoutDimension(widthAttr, -2);
            this.height = attributes.getLayoutDimension(heightAttr, -2);
        }

        final void setRowSpecSpan(Interval span) {
            this.rowSpec = this.rowSpec.copyWriteSpan(span);
        }

        final void setColumnSpecSpan(Interval span) {
            this.columnSpec = this.columnSpec.copyWriteSpan(span);
        }

        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            LayoutParams that = (LayoutParams) o;
            return this.columnSpec.equals(that.columnSpec) && this.rowSpec.equals(that.rowSpec);
        }

        public int hashCode() {
            int result = this.rowSpec.hashCode();
            return (result * 31) + this.columnSpec.hashCode();
        }
    }

    static final class Arc {
        public final Interval span;
        public boolean valid = true;
        public final MutableInt value;

        public Arc(Interval span, MutableInt value) {
            this.span = span;
            this.value = value;
        }

        public String toString() {
            return this.span + " " + (!this.valid ? "+>" : "->") + " " + this.value;
        }
    }

    static final class MutableInt {
        public int value;

        public MutableInt() {
            reset();
        }

        public MutableInt(int value) {
            this.value = value;
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

        private Assoc(Class<K> keyType, Class<V> valueType) {
            this.keyType = keyType;
            this.valueType = valueType;
        }

        public static <K, V> Assoc<K, V> of(Class<K> keyType, Class<V> valueType) {
            return new Assoc<>(keyType, valueType);
        }

        public void put(K key, V value) {
            add(Pair.create(key, value));
        }

        public PackedMap<K, V> pack() {
            int N = size();
            Object[] objArr = (Object[]) Array.newInstance((Class<?>) this.keyType, N);
            Object[] objArr2 = (Object[]) Array.newInstance((Class<?>) this.valueType, N);
            for (int i = 0; i < N; i++) {
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

        private static <K> int[] createIndex(K[] keys) {
            int size = keys.length;
            int[] result = new int[size];
            Map<K, Integer> keyToIndex = new HashMap<>();
            for (int i = 0; i < size; i++) {
                K key = keys[i];
                Integer index = keyToIndex.get(key);
                if (index == null) {
                    index = Integer.valueOf(keyToIndex.size());
                    keyToIndex.put(key, index);
                }
                result[i] = index.intValue();
            }
            return result;
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

        protected void include(int before, int after) {
            this.before = Math.max(this.before, before);
            this.after = Math.max(this.after, after);
        }

        protected int size(boolean min) {
            if (min || !GridLayout.canStretch(this.flexibility)) {
                return this.before + this.after;
            }
            return 100000;
        }

        protected int getOffset(GridLayout gl, View c, Alignment a, int size, boolean horizontal) {
            return this.before - a.getAlignmentValue(c, size, gl.getLayoutMode());
        }

        protected final void include(GridLayout gl, View c, Spec spec, Axis axis, int size) {
            this.flexibility &= spec.getFlexibility();
            boolean horizontal = axis.horizontal;
            Alignment alignment = gl.getAlignment(spec.alignment, horizontal);
            int before = alignment.getAlignmentValue(c, size, gl.getLayoutMode());
            include(before, size - before);
        }

        public String toString() {
            return "Bounds{before=" + this.before + ", after=" + this.after + '}';
        }
    }

    static final class Interval {
        public final int max;
        public final int min;

        public Interval(int min, int max) {
            this.min = min;
            this.max = max;
        }

        int size() {
            return this.max - this.min;
        }

        Interval inverse() {
            return new Interval(this.max, this.min);
        }

        public boolean equals(Object that) {
            if (this == that) {
                return true;
            }
            if (that == null || getClass() != that.getClass()) {
                return false;
            }
            Interval interval = (Interval) that;
            return this.max == interval.max && this.min == interval.min;
        }

        public int hashCode() {
            int result = this.min;
            return (result * 31) + this.max;
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

        private Spec(boolean startDefined, Interval span, Alignment alignment, float weight) {
            this.startDefined = startDefined;
            this.span = span;
            this.alignment = alignment;
            this.weight = weight;
        }

        private Spec(boolean startDefined, int start, int size, Alignment alignment, float weight) {
            this(startDefined, new Interval(start, start + size), alignment, weight);
        }

        final Spec copyWriteSpan(Interval span) {
            return new Spec(this.startDefined, span, this.alignment, this.weight);
        }

        final Spec copyWriteAlignment(Alignment alignment) {
            return new Spec(this.startDefined, this.span, alignment, this.weight);
        }

        final int getFlexibility() {
            return (this.alignment == GridLayout.UNDEFINED_ALIGNMENT && this.weight == 0.0f) ? 0 : 2;
        }

        public boolean equals(Object that) {
            if (this == that) {
                return true;
            }
            if (that == null || getClass() != that.getClass()) {
                return false;
            }
            Spec spec = (Spec) that;
            return this.alignment.equals(spec.alignment) && this.span.equals(spec.span);
        }

        public int hashCode() {
            int result = this.span.hashCode();
            return (result * 31) + this.alignment.hashCode();
        }
    }

    public static Spec spec(int start, int size, Alignment alignment, float weight) {
        return new Spec(start != Integer.MIN_VALUE, start, size, alignment, weight);
    }

    public static Spec spec(int start, Alignment alignment, float weight) {
        return spec(start, 1, alignment, weight);
    }

    public static Spec spec(int start, int size, float weight) {
        return spec(start, size, UNDEFINED_ALIGNMENT, weight);
    }

    public static Spec spec(int start, float weight) {
        return spec(start, 1, weight);
    }

    public static Spec spec(int start, int size, Alignment alignment) {
        return spec(start, size, alignment, 0.0f);
    }

    public static Spec spec(int start, Alignment alignment) {
        return spec(start, 1, alignment);
    }

    public static Spec spec(int start, int size) {
        return spec(start, size, UNDEFINED_ALIGNMENT);
    }

    public static Spec spec(int start) {
        return spec(start, 1);
    }

    public static abstract class Alignment {
        abstract int getAlignmentValue(View view, int i, int i2);

        abstract int getGravityOffset(View view, int i);

        Alignment() {
        }

        int getSizeInCell(View view, int viewSize, int cellSize) {
            return viewSize;
        }

        Bounds getBounds() {
            return new Bounds();
        }
    }

    private static Alignment createSwitchingAlignment(final Alignment ltr, final Alignment rtl) {
        return new Alignment() {
            @Override
            int getGravityOffset(View view, int cellDelta) {
                return (!view.isLayoutRtl() ? ltr : rtl).getGravityOffset(view, cellDelta);
            }

            @Override
            public int getAlignmentValue(View view, int viewSize, int mode) {
                return (!view.isLayoutRtl() ? ltr : rtl).getAlignmentValue(view, viewSize, mode);
            }
        };
    }

    static boolean canStretch(int flexibility) {
        return (flexibility & 2) != 0;
    }
}
