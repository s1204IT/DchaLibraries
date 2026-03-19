package android.widget;

import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.TypedArray;
import android.graphics.Rect;
import android.media.TtmlUtils;
import android.util.ArrayMap;
import android.util.AttributeSet;
import android.util.Pools;
import android.util.SparseArray;
import android.view.Gravity;
import android.view.RemotableViewMethod;
import android.view.View;
import android.view.ViewDebug;
import android.view.ViewGroup;
import android.view.ViewHierarchyEncoder;
import android.view.accessibility.AccessibilityEvent;
import android.widget.RemoteViews;
import com.android.internal.R;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.SortedSet;
import java.util.TreeSet;

@RemoteViews.RemoteView
public class RelativeLayout extends ViewGroup {
    public static final int ABOVE = 2;
    public static final int ALIGN_BASELINE = 4;
    public static final int ALIGN_BOTTOM = 8;
    public static final int ALIGN_END = 19;
    public static final int ALIGN_LEFT = 5;
    public static final int ALIGN_PARENT_BOTTOM = 12;
    public static final int ALIGN_PARENT_END = 21;
    public static final int ALIGN_PARENT_LEFT = 9;
    public static final int ALIGN_PARENT_RIGHT = 11;
    public static final int ALIGN_PARENT_START = 20;
    public static final int ALIGN_PARENT_TOP = 10;
    public static final int ALIGN_RIGHT = 7;
    public static final int ALIGN_START = 18;
    public static final int ALIGN_TOP = 6;
    public static final int BELOW = 3;
    public static final int CENTER_HORIZONTAL = 14;
    public static final int CENTER_IN_PARENT = 13;
    public static final int CENTER_VERTICAL = 15;
    private static final int DEFAULT_WIDTH = 65536;
    public static final int END_OF = 17;
    public static final int LEFT_OF = 0;
    public static final int RIGHT_OF = 1;
    public static final int START_OF = 16;
    public static final int TRUE = -1;
    private static final int VALUE_NOT_SET = Integer.MIN_VALUE;
    private static final int VERB_COUNT = 22;
    private boolean mAllowBrokenMeasureSpecs;
    private View mBaselineView;
    private final Rect mContentBounds;
    private boolean mDirtyHierarchy;
    private final DependencyGraph mGraph;
    private int mGravity;
    private int mIgnoreGravity;
    private boolean mMeasureVerticalWithPaddingMargin;
    private final Rect mSelfBounds;
    private View[] mSortedHorizontalChildren;
    private View[] mSortedVerticalChildren;
    private SortedSet<View> mTopToBottomLeftToRightSet;
    private static final int[] RULES_VERTICAL = {2, 3, 4, 6, 8};
    private static final int[] RULES_HORIZONTAL = {0, 1, 5, 7, 16, 17, 18, 19};

    public RelativeLayout(Context context) {
        this(context, null);
    }

    public RelativeLayout(Context context, AttributeSet attributeSet) {
        this(context, attributeSet, 0);
    }

    public RelativeLayout(Context context, AttributeSet attributeSet, int i) {
        this(context, attributeSet, i, 0);
    }

    public RelativeLayout(Context context, AttributeSet attributeSet, int i, int i2) {
        super(context, attributeSet, i, i2);
        this.mBaselineView = null;
        this.mGravity = 8388659;
        this.mContentBounds = new Rect();
        this.mSelfBounds = new Rect();
        this.mTopToBottomLeftToRightSet = null;
        this.mGraph = new DependencyGraph();
        this.mAllowBrokenMeasureSpecs = false;
        this.mMeasureVerticalWithPaddingMargin = false;
        initFromAttributes(context, attributeSet, i, i2);
        queryCompatibilityModes(context);
    }

    private void initFromAttributes(Context context, AttributeSet attributeSet, int i, int i2) {
        TypedArray typedArrayObtainStyledAttributes = context.obtainStyledAttributes(attributeSet, R.styleable.RelativeLayout, i, i2);
        this.mIgnoreGravity = typedArrayObtainStyledAttributes.getResourceId(1, -1);
        this.mGravity = typedArrayObtainStyledAttributes.getInt(0, this.mGravity);
        typedArrayObtainStyledAttributes.recycle();
    }

    private void queryCompatibilityModes(Context context) {
        int i = context.getApplicationInfo().targetSdkVersion;
        this.mAllowBrokenMeasureSpecs = i <= 17;
        this.mMeasureVerticalWithPaddingMargin = i >= 18;
    }

    @Override
    public boolean shouldDelayChildPressedState() {
        return false;
    }

    @RemotableViewMethod
    public void setIgnoreGravity(int i) {
        this.mIgnoreGravity = i;
    }

    public int getGravity() {
        return this.mGravity;
    }

    @RemotableViewMethod
    public void setGravity(int i) {
        if (this.mGravity != i) {
            if ((8388615 & i) == 0) {
                i |= Gravity.START;
            }
            if ((i & 112) == 0) {
                i |= 48;
            }
            this.mGravity = i;
            requestLayout();
        }
    }

    @RemotableViewMethod
    public void setHorizontalGravity(int i) {
        int i2 = i & Gravity.RELATIVE_HORIZONTAL_GRAVITY_MASK;
        if ((8388615 & this.mGravity) != i2) {
            this.mGravity = i2 | (this.mGravity & (-8388616));
            requestLayout();
        }
    }

    @RemotableViewMethod
    public void setVerticalGravity(int i) {
        int i2 = i & 112;
        if ((this.mGravity & 112) != i2) {
            this.mGravity = i2 | (this.mGravity & PackageManager.INSTALL_FAILED_NO_MATCHING_ABIS);
            requestLayout();
        }
    }

    @Override
    public int getBaseline() {
        return this.mBaselineView != null ? this.mBaselineView.getBaseline() : super.getBaseline();
    }

    @Override
    public void requestLayout() {
        super.requestLayout();
        this.mDirtyHierarchy = true;
    }

    private void sortChildren() {
        int childCount = getChildCount();
        if (this.mSortedVerticalChildren == null || this.mSortedVerticalChildren.length != childCount) {
            this.mSortedVerticalChildren = new View[childCount];
        }
        if (this.mSortedHorizontalChildren == null || this.mSortedHorizontalChildren.length != childCount) {
            this.mSortedHorizontalChildren = new View[childCount];
        }
        DependencyGraph dependencyGraph = this.mGraph;
        dependencyGraph.clear();
        for (int i = 0; i < childCount; i++) {
            dependencyGraph.add(getChildAt(i));
        }
        dependencyGraph.getSortedViews(this.mSortedVerticalChildren, RULES_VERTICAL);
        dependencyGraph.getSortedViews(this.mSortedHorizontalChildren, RULES_HORIZONTAL);
    }

    @Override
    protected void onMeasure(int i, int i2) {
        View viewFindViewById;
        View view;
        int i3;
        int i4;
        int i5 = 0;
        if (this.mDirtyHierarchy) {
            this.mDirtyHierarchy = false;
            sortChildren();
        }
        int mode = View.MeasureSpec.getMode(i);
        int mode2 = View.MeasureSpec.getMode(i2);
        int size = View.MeasureSpec.getSize(i);
        int size2 = View.MeasureSpec.getSize(i2);
        if (mode == 0) {
            size = -1;
        }
        if (mode2 == 0) {
            size2 = -1;
        }
        int i6 = mode == 1073741824 ? size : 0;
        int i7 = mode2 == 1073741824 ? size2 : 0;
        int i8 = this.mGravity & Gravity.RELATIVE_HORIZONTAL_GRAVITY_MASK;
        boolean z = (i8 == 8388611 || i8 == 0) ? false : true;
        int i9 = this.mGravity & 112;
        boolean z2 = (i9 == 48 || i9 == 0) ? false : true;
        if ((z || z2) && this.mIgnoreGravity != -1) {
            viewFindViewById = findViewById(this.mIgnoreGravity);
        } else {
            viewFindViewById = null;
        }
        boolean z3 = mode != 1073741824;
        boolean z4 = mode2 != 1073741824;
        int layoutDirection = getLayoutDirection();
        if (isLayoutRtl() && size == -1) {
            size = 65536;
        }
        View[] viewArr = this.mSortedHorizontalChildren;
        int length = viewArr.length;
        boolean z5 = false;
        while (i5 < length) {
            View view2 = viewArr[i5];
            View[] viewArr2 = viewArr;
            if (view2.getVisibility() != 8) {
                LayoutParams layoutParams = (LayoutParams) view2.getLayoutParams();
                applyHorizontalSizeRules(layoutParams, size, layoutParams.getRules(layoutDirection));
                measureChildHorizontal(view2, layoutParams, size, size2);
                if (positionChildHorizontal(view2, layoutParams, size, z3)) {
                    z5 = true;
                }
            }
            i5++;
            viewArr = viewArr2;
        }
        View[] viewArr3 = this.mSortedVerticalChildren;
        int length2 = viewArr3.length;
        int i10 = getContext().getApplicationInfo().targetSdkVersion;
        int i11 = Integer.MIN_VALUE;
        int iMax = Integer.MIN_VALUE;
        int iMin = Integer.MAX_VALUE;
        int iMin2 = Integer.MAX_VALUE;
        boolean z6 = false;
        int iResolveSize = i7;
        int iResolveSize2 = i6;
        int i12 = 0;
        while (i12 < length2) {
            View view3 = viewArr3[i12];
            int i13 = layoutDirection;
            View[] viewArr4 = viewArr3;
            if (view3.getVisibility() != 8) {
                LayoutParams layoutParams2 = (LayoutParams) view3.getLayoutParams();
                applyVerticalSizeRules(layoutParams2, size2, view3.getBaseline());
                measureChild(view3, layoutParams2, size, size2);
                if (positionChildVertical(view3, layoutParams2, size2, z4)) {
                    z6 = true;
                }
                if (z3) {
                    if (isLayoutRtl()) {
                        if (i10 < 19) {
                            iResolveSize2 = Math.max(iResolveSize2, size - layoutParams2.mLeft);
                            i4 = size2;
                        } else {
                            i4 = size2;
                            iResolveSize2 = Math.max(iResolveSize2, (size - layoutParams2.mLeft) + layoutParams2.leftMargin);
                        }
                    } else {
                        i4 = size2;
                        iResolveSize2 = i10 < 19 ? Math.max(iResolveSize2, layoutParams2.mRight) : Math.max(iResolveSize2, layoutParams2.mRight + layoutParams2.rightMargin);
                    }
                    if (z4) {
                    }
                    if (view3 == viewFindViewById) {
                        iMin = Math.min(iMin, layoutParams2.mLeft - layoutParams2.leftMargin);
                        iMin2 = Math.min(iMin2, layoutParams2.mTop - layoutParams2.topMargin);
                        if (view3 == viewFindViewById) {
                            int iMax2 = Math.max(i11, layoutParams2.mRight + layoutParams2.rightMargin);
                            iMax = Math.max(iMax, layoutParams2.mBottom + layoutParams2.bottomMargin);
                            i11 = iMax2;
                        }
                    }
                } else {
                    i4 = size2;
                    if (z4) {
                        iResolveSize = i10 < 19 ? Math.max(iResolveSize, layoutParams2.mBottom) : Math.max(iResolveSize, layoutParams2.mBottom + layoutParams2.bottomMargin);
                    }
                    if (view3 == viewFindViewById || z2) {
                        iMin = Math.min(iMin, layoutParams2.mLeft - layoutParams2.leftMargin);
                        iMin2 = Math.min(iMin2, layoutParams2.mTop - layoutParams2.topMargin);
                    }
                    if (view3 == viewFindViewById || z) {
                        int iMax22 = Math.max(i11, layoutParams2.mRight + layoutParams2.rightMargin);
                        iMax = Math.max(iMax, layoutParams2.mBottom + layoutParams2.bottomMargin);
                        i11 = iMax22;
                    }
                }
            } else {
                i4 = size2;
            }
            i12++;
            layoutDirection = i13;
            viewArr3 = viewArr4;
            size2 = i4;
        }
        View[] viewArr5 = viewArr3;
        int i14 = layoutDirection;
        int i15 = iMin;
        int i16 = iMin2;
        int i17 = i11;
        int i18 = iMax;
        int i19 = size;
        LayoutParams layoutParams3 = null;
        int i20 = 0;
        View view4 = null;
        while (i20 < length2) {
            View view5 = viewFindViewById;
            View view6 = viewArr5[i20];
            int i21 = i18;
            int i22 = i16;
            if (view6.getVisibility() != 8) {
                LayoutParams layoutParams4 = (LayoutParams) view6.getLayoutParams();
                if (view4 == null || layoutParams3 == null || compareLayoutPosition(layoutParams4, layoutParams3) < 0) {
                    layoutParams3 = layoutParams4;
                    view4 = view6;
                }
            }
            i20++;
            viewFindViewById = view5;
            i18 = i21;
            i16 = i22;
        }
        int i23 = i18;
        int i24 = i16;
        View view7 = viewFindViewById;
        this.mBaselineView = view4;
        if (z3) {
            int iMax3 = iResolveSize2 + this.mPaddingRight;
            if (this.mLayoutParams != null && this.mLayoutParams.width >= 0) {
                iMax3 = Math.max(iMax3, this.mLayoutParams.width);
            }
            iResolveSize2 = resolveSize(Math.max(iMax3, getSuggestedMinimumWidth()), i);
            if (z5) {
                int i25 = 0;
                while (i25 < length2) {
                    View view8 = viewArr5[i25];
                    if (view8.getVisibility() == 8) {
                        i3 = i14;
                    } else {
                        LayoutParams layoutParams5 = (LayoutParams) view8.getLayoutParams();
                        i3 = i14;
                        int[] rules = layoutParams5.getRules(i3);
                        if (rules[13] != 0 || rules[14] != 0) {
                            centerHorizontal(view8, layoutParams5, iResolveSize2);
                        } else if (rules[11] != 0) {
                            int measuredWidth = view8.getMeasuredWidth();
                            layoutParams5.mLeft = (iResolveSize2 - this.mPaddingRight) - measuredWidth;
                            layoutParams5.mRight = layoutParams5.mLeft + measuredWidth;
                        }
                    }
                    i25++;
                    i14 = i3;
                }
            }
        }
        int i26 = i14;
        if (z4) {
            int iMax4 = iResolveSize + this.mPaddingBottom;
            if (this.mLayoutParams != null && this.mLayoutParams.height >= 0) {
                iMax4 = Math.max(iMax4, this.mLayoutParams.height);
            }
            iResolveSize = resolveSize(Math.max(iMax4, getSuggestedMinimumHeight()), i2);
            if (z6) {
                for (int i27 = 0; i27 < length2; i27++) {
                    View view9 = viewArr5[i27];
                    if (view9.getVisibility() != 8) {
                        LayoutParams layoutParams6 = (LayoutParams) view9.getLayoutParams();
                        int[] rules2 = layoutParams6.getRules(i26);
                        if (rules2[13] != 0 || rules2[15] != 0) {
                            centerVertical(view9, layoutParams6, iResolveSize);
                        } else if (rules2[12] != 0) {
                            int measuredHeight = view9.getMeasuredHeight();
                            layoutParams6.mTop = (iResolveSize - this.mPaddingBottom) - measuredHeight;
                            layoutParams6.mBottom = layoutParams6.mTop + measuredHeight;
                        }
                    }
                }
            }
        }
        int i28 = iResolveSize;
        if (z || z2) {
            Rect rect = this.mSelfBounds;
            rect.set(this.mPaddingLeft, this.mPaddingTop, iResolveSize2 - this.mPaddingRight, i28 - this.mPaddingBottom);
            Rect rect2 = this.mContentBounds;
            Gravity.apply(this.mGravity, i17 - i15, i23 - i24, rect, rect2, i26);
            int i29 = rect2.left - i15;
            int i30 = rect2.top - i24;
            if (i29 != 0 || i30 != 0) {
                int i31 = 0;
                while (i31 < length2) {
                    View view10 = viewArr5[i31];
                    if (view10.getVisibility() != 8) {
                        view = view7;
                        if (view10 != view) {
                            LayoutParams layoutParams7 = (LayoutParams) view10.getLayoutParams();
                            if (z) {
                                LayoutParams.access$112(layoutParams7, i29);
                                LayoutParams.access$212(layoutParams7, i29);
                            }
                            if (z2) {
                                LayoutParams.access$412(layoutParams7, i30);
                                LayoutParams.access$312(layoutParams7, i30);
                            }
                        }
                    } else {
                        view = view7;
                    }
                    i31++;
                    view7 = view;
                }
            }
        }
        if (isLayoutRtl()) {
            int i32 = i19 - iResolveSize2;
            for (int i33 = 0; i33 < length2; i33++) {
                View view11 = viewArr5[i33];
                if (view11.getVisibility() != 8) {
                    LayoutParams layoutParams8 = (LayoutParams) view11.getLayoutParams();
                    LayoutParams.access$120(layoutParams8, i32);
                    LayoutParams.access$220(layoutParams8, i32);
                }
            }
        }
        setMeasuredDimension(iResolveSize2, i28);
    }

    private int compareLayoutPosition(LayoutParams layoutParams, LayoutParams layoutParams2) {
        int i = layoutParams.mTop - layoutParams2.mTop;
        if (i != 0) {
            return i;
        }
        return layoutParams.mLeft - layoutParams2.mLeft;
    }

    private void measureChild(View view, LayoutParams layoutParams, int i, int i2) {
        view.measure(getChildMeasureSpec(layoutParams.mLeft, layoutParams.mRight, layoutParams.width, layoutParams.leftMargin, layoutParams.rightMargin, this.mPaddingLeft, this.mPaddingRight, i), getChildMeasureSpec(layoutParams.mTop, layoutParams.mBottom, layoutParams.height, layoutParams.topMargin, layoutParams.bottomMargin, this.mPaddingTop, this.mPaddingBottom, i2));
    }

    private void measureChildHorizontal(View view, LayoutParams layoutParams, int i, int i2) {
        int iMax;
        int iMakeMeasureSpec;
        int childMeasureSpec = getChildMeasureSpec(layoutParams.mLeft, layoutParams.mRight, layoutParams.width, layoutParams.leftMargin, layoutParams.rightMargin, this.mPaddingLeft, this.mPaddingRight, i);
        int i3 = 1073741824;
        if (i2 < 0 && !this.mAllowBrokenMeasureSpecs) {
            if (layoutParams.height >= 0) {
                iMakeMeasureSpec = View.MeasureSpec.makeMeasureSpec(layoutParams.height, 1073741824);
            } else {
                iMakeMeasureSpec = View.MeasureSpec.makeMeasureSpec(0, 0);
            }
        } else {
            if (this.mMeasureVerticalWithPaddingMargin) {
                iMax = Math.max(0, (((i2 - this.mPaddingTop) - this.mPaddingBottom) - layoutParams.topMargin) - layoutParams.bottomMargin);
            } else {
                iMax = Math.max(0, i2);
            }
            if (layoutParams.height != -1) {
                i3 = Integer.MIN_VALUE;
            }
            iMakeMeasureSpec = View.MeasureSpec.makeMeasureSpec(iMax, i3);
        }
        view.measure(childMeasureSpec, iMakeMeasureSpec);
    }

    private int getChildMeasureSpec(int i, int i2, int i3, int i4, int i5, int i6, int i7, int i8) {
        int i9;
        int i10;
        boolean z = i8 < 0;
        if (z && !this.mAllowBrokenMeasureSpecs) {
            if (i != Integer.MIN_VALUE && i2 != Integer.MIN_VALUE) {
                i3 = Math.max(0, i2 - i);
            } else if (i3 < 0) {
                i3 = 0;
                i = 0;
            }
            return View.MeasureSpec.makeMeasureSpec(i3, i);
        }
        if (i == Integer.MIN_VALUE) {
            i9 = i4 + i6;
        } else {
            i9 = i;
        }
        if (i2 == Integer.MIN_VALUE) {
            i10 = (i8 - i7) - i5;
        } else {
            i10 = i2;
        }
        int i11 = i10 - i9;
        if (i != Integer.MIN_VALUE && i2 != Integer.MIN_VALUE) {
            i = z ? 0 : 1073741824;
            i3 = Math.max(0, i11);
        } else if (i3 >= 0) {
            if (i11 >= 0) {
                i3 = Math.min(i11, i3);
            }
        } else if (i3 == -1) {
            i = z ? 0 : 1073741824;
            i3 = Math.max(0, i11);
        } else if (i3 != -2 || i11 < 0) {
            i3 = 0;
            i = 0;
        } else {
            i3 = i11;
            i = Integer.MIN_VALUE;
        }
        return View.MeasureSpec.makeMeasureSpec(i3, i);
    }

    private boolean positionChildHorizontal(View view, LayoutParams layoutParams, int i, boolean z) {
        int[] rules = layoutParams.getRules(getLayoutDirection());
        if (layoutParams.mLeft != Integer.MIN_VALUE || layoutParams.mRight == Integer.MIN_VALUE) {
            if (layoutParams.mLeft == Integer.MIN_VALUE || layoutParams.mRight != Integer.MIN_VALUE) {
                if (layoutParams.mLeft == Integer.MIN_VALUE && layoutParams.mRight == Integer.MIN_VALUE) {
                    if (rules[13] != 0 || rules[14] != 0) {
                        if (!z) {
                            centerHorizontal(view, layoutParams, i);
                        } else {
                            positionAtEdge(view, layoutParams, i);
                        }
                        return true;
                    }
                    positionAtEdge(view, layoutParams, i);
                }
            } else {
                layoutParams.mRight = layoutParams.mLeft + view.getMeasuredWidth();
            }
        } else {
            layoutParams.mLeft = layoutParams.mRight - view.getMeasuredWidth();
        }
        return rules[21] != 0;
    }

    private void positionAtEdge(View view, LayoutParams layoutParams, int i) {
        if (isLayoutRtl()) {
            layoutParams.mRight = (i - this.mPaddingRight) - layoutParams.rightMargin;
            layoutParams.mLeft = layoutParams.mRight - view.getMeasuredWidth();
        } else {
            layoutParams.mLeft = this.mPaddingLeft + layoutParams.leftMargin;
            layoutParams.mRight = layoutParams.mLeft + view.getMeasuredWidth();
        }
    }

    private boolean positionChildVertical(View view, LayoutParams layoutParams, int i, boolean z) {
        int[] rules = layoutParams.getRules();
        if (layoutParams.mTop != Integer.MIN_VALUE || layoutParams.mBottom == Integer.MIN_VALUE) {
            if (layoutParams.mTop == Integer.MIN_VALUE || layoutParams.mBottom != Integer.MIN_VALUE) {
                if (layoutParams.mTop == Integer.MIN_VALUE && layoutParams.mBottom == Integer.MIN_VALUE) {
                    if (rules[13] != 0 || rules[15] != 0) {
                        if (!z) {
                            centerVertical(view, layoutParams, i);
                        } else {
                            layoutParams.mTop = this.mPaddingTop + layoutParams.topMargin;
                            layoutParams.mBottom = layoutParams.mTop + view.getMeasuredHeight();
                        }
                        return true;
                    }
                    layoutParams.mTop = this.mPaddingTop + layoutParams.topMargin;
                    layoutParams.mBottom = layoutParams.mTop + view.getMeasuredHeight();
                }
            } else {
                layoutParams.mBottom = layoutParams.mTop + view.getMeasuredHeight();
            }
        } else {
            layoutParams.mTop = layoutParams.mBottom - view.getMeasuredHeight();
        }
        return rules[12] != 0;
    }

    private void applyHorizontalSizeRules(LayoutParams layoutParams, int i, int[] iArr) {
        layoutParams.mLeft = Integer.MIN_VALUE;
        layoutParams.mRight = Integer.MIN_VALUE;
        LayoutParams relatedViewParams = getRelatedViewParams(iArr, 0);
        if (relatedViewParams == null) {
            if (layoutParams.alignWithParent && iArr[0] != 0 && i >= 0) {
                layoutParams.mRight = (i - this.mPaddingRight) - layoutParams.rightMargin;
            }
        } else {
            layoutParams.mRight = relatedViewParams.mLeft - (relatedViewParams.leftMargin + layoutParams.rightMargin);
        }
        LayoutParams relatedViewParams2 = getRelatedViewParams(iArr, 1);
        if (relatedViewParams2 == null) {
            if (layoutParams.alignWithParent && iArr[1] != 0) {
                layoutParams.mLeft = this.mPaddingLeft + layoutParams.leftMargin;
            }
        } else {
            layoutParams.mLeft = relatedViewParams2.mRight + relatedViewParams2.rightMargin + layoutParams.leftMargin;
        }
        LayoutParams relatedViewParams3 = getRelatedViewParams(iArr, 5);
        if (relatedViewParams3 == null) {
            if (layoutParams.alignWithParent && iArr[5] != 0) {
                layoutParams.mLeft = this.mPaddingLeft + layoutParams.leftMargin;
            }
        } else {
            layoutParams.mLeft = relatedViewParams3.mLeft + layoutParams.leftMargin;
        }
        LayoutParams relatedViewParams4 = getRelatedViewParams(iArr, 7);
        if (relatedViewParams4 == null) {
            if (layoutParams.alignWithParent && iArr[7] != 0 && i >= 0) {
                layoutParams.mRight = (i - this.mPaddingRight) - layoutParams.rightMargin;
            }
        } else {
            layoutParams.mRight = relatedViewParams4.mRight - layoutParams.rightMargin;
        }
        if (iArr[9] != 0) {
            layoutParams.mLeft = this.mPaddingLeft + layoutParams.leftMargin;
        }
        if (iArr[11] == 0 || i < 0) {
            return;
        }
        layoutParams.mRight = (i - this.mPaddingRight) - layoutParams.rightMargin;
    }

    private void applyVerticalSizeRules(LayoutParams layoutParams, int i, int i2) {
        int[] rules = layoutParams.getRules();
        int relatedViewBaselineOffset = getRelatedViewBaselineOffset(rules);
        if (relatedViewBaselineOffset != -1) {
            if (i2 != -1) {
                relatedViewBaselineOffset -= i2;
            }
            layoutParams.mTop = relatedViewBaselineOffset;
            layoutParams.mBottom = Integer.MIN_VALUE;
            return;
        }
        layoutParams.mTop = Integer.MIN_VALUE;
        layoutParams.mBottom = Integer.MIN_VALUE;
        LayoutParams relatedViewParams = getRelatedViewParams(rules, 2);
        if (relatedViewParams == null) {
            if (layoutParams.alignWithParent && rules[2] != 0 && i >= 0) {
                layoutParams.mBottom = (i - this.mPaddingBottom) - layoutParams.bottomMargin;
            }
        } else {
            layoutParams.mBottom = relatedViewParams.mTop - (relatedViewParams.topMargin + layoutParams.bottomMargin);
        }
        LayoutParams relatedViewParams2 = getRelatedViewParams(rules, 3);
        if (relatedViewParams2 == null) {
            if (layoutParams.alignWithParent && rules[3] != 0) {
                layoutParams.mTop = this.mPaddingTop + layoutParams.topMargin;
            }
        } else {
            layoutParams.mTop = relatedViewParams2.mBottom + relatedViewParams2.bottomMargin + layoutParams.topMargin;
        }
        LayoutParams relatedViewParams3 = getRelatedViewParams(rules, 6);
        if (relatedViewParams3 == null) {
            if (layoutParams.alignWithParent && rules[6] != 0) {
                layoutParams.mTop = this.mPaddingTop + layoutParams.topMargin;
            }
        } else {
            layoutParams.mTop = relatedViewParams3.mTop + layoutParams.topMargin;
        }
        LayoutParams relatedViewParams4 = getRelatedViewParams(rules, 8);
        if (relatedViewParams4 == null) {
            if (layoutParams.alignWithParent && rules[8] != 0 && i >= 0) {
                layoutParams.mBottom = (i - this.mPaddingBottom) - layoutParams.bottomMargin;
            }
        } else {
            layoutParams.mBottom = relatedViewParams4.mBottom - layoutParams.bottomMargin;
        }
        if (rules[10] != 0) {
            layoutParams.mTop = this.mPaddingTop + layoutParams.topMargin;
        }
        if (rules[12] == 0 || i < 0) {
            return;
        }
        layoutParams.mBottom = (i - this.mPaddingBottom) - layoutParams.bottomMargin;
    }

    private View getRelatedView(int[] iArr, int i) {
        DependencyGraph.Node node;
        int i2 = iArr[i];
        if (i2 == 0 || (node = (DependencyGraph.Node) this.mGraph.mKeyNodes.get(i2)) == null) {
            return null;
        }
        View view = node.view;
        while (view.getVisibility() == 8) {
            DependencyGraph.Node node2 = (DependencyGraph.Node) this.mGraph.mKeyNodes.get(((LayoutParams) view.getLayoutParams()).getRules(view.getLayoutDirection())[i]);
            if (node2 == null || view == node2.view) {
                return null;
            }
            view = node2.view;
        }
        return view;
    }

    private LayoutParams getRelatedViewParams(int[] iArr, int i) {
        View relatedView = getRelatedView(iArr, i);
        if (relatedView != null && (relatedView.getLayoutParams() instanceof LayoutParams)) {
            return (LayoutParams) relatedView.getLayoutParams();
        }
        return null;
    }

    private int getRelatedViewBaselineOffset(int[] iArr) {
        int baseline;
        View relatedView = getRelatedView(iArr, 4);
        if (relatedView == null || (baseline = relatedView.getBaseline()) == -1 || !(relatedView.getLayoutParams() instanceof LayoutParams)) {
            return -1;
        }
        return ((LayoutParams) relatedView.getLayoutParams()).mTop + baseline;
    }

    private static void centerHorizontal(View view, LayoutParams layoutParams, int i) {
        int measuredWidth = view.getMeasuredWidth();
        int i2 = (i - measuredWidth) / 2;
        layoutParams.mLeft = i2;
        layoutParams.mRight = i2 + measuredWidth;
    }

    private static void centerVertical(View view, LayoutParams layoutParams, int i) {
        int measuredHeight = view.getMeasuredHeight();
        int i2 = (i - measuredHeight) / 2;
        layoutParams.mTop = i2;
        layoutParams.mBottom = i2 + measuredHeight;
    }

    @Override
    protected void onLayout(boolean z, int i, int i2, int i3, int i4) {
        int childCount = getChildCount();
        for (int i5 = 0; i5 < childCount; i5++) {
            View childAt = getChildAt(i5);
            if (childAt.getVisibility() != 8) {
                LayoutParams layoutParams = (LayoutParams) childAt.getLayoutParams();
                childAt.layout(layoutParams.mLeft, layoutParams.mTop, layoutParams.mRight, layoutParams.mBottom);
            }
        }
    }

    @Override
    public LayoutParams generateLayoutParams(AttributeSet attributeSet) {
        return new LayoutParams(getContext(), attributeSet);
    }

    @Override
    protected ViewGroup.LayoutParams generateDefaultLayoutParams() {
        return new LayoutParams(-2, -2);
    }

    @Override
    protected boolean checkLayoutParams(ViewGroup.LayoutParams layoutParams) {
        return layoutParams instanceof LayoutParams;
    }

    @Override
    protected ViewGroup.LayoutParams generateLayoutParams(ViewGroup.LayoutParams layoutParams) {
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

    @Override
    public boolean dispatchPopulateAccessibilityEventInternal(AccessibilityEvent accessibilityEvent) {
        if (this.mTopToBottomLeftToRightSet == null) {
            this.mTopToBottomLeftToRightSet = new TreeSet(new TopToBottomLeftToRightComparator());
        }
        int childCount = getChildCount();
        for (int i = 0; i < childCount; i++) {
            this.mTopToBottomLeftToRightSet.add(getChildAt(i));
        }
        for (View view : this.mTopToBottomLeftToRightSet) {
            if (view.getVisibility() == 0 && view.dispatchPopulateAccessibilityEvent(accessibilityEvent)) {
                this.mTopToBottomLeftToRightSet.clear();
                return true;
            }
        }
        this.mTopToBottomLeftToRightSet.clear();
        return false;
    }

    @Override
    public CharSequence getAccessibilityClassName() {
        return RelativeLayout.class.getName();
    }

    private class TopToBottomLeftToRightComparator implements Comparator<View> {
        private TopToBottomLeftToRightComparator() {
        }

        @Override
        public int compare(View view, View view2) {
            int top = view.getTop() - view2.getTop();
            if (top != 0) {
                return top;
            }
            int left = view.getLeft() - view2.getLeft();
            if (left != 0) {
                return left;
            }
            int height = view.getHeight() - view2.getHeight();
            if (height != 0) {
                return height;
            }
            int width = view.getWidth() - view2.getWidth();
            if (width != 0) {
                return width;
            }
            return 0;
        }
    }

    public static class LayoutParams extends ViewGroup.MarginLayoutParams {

        @ViewDebug.ExportedProperty(category = TtmlUtils.TAG_LAYOUT)
        public boolean alignWithParent;
        private int mBottom;
        private int[] mInitialRules;
        private boolean mIsRtlCompatibilityMode;
        private int mLeft;
        private boolean mNeedsLayoutResolution;
        private int mRight;

        @ViewDebug.ExportedProperty(category = TtmlUtils.TAG_LAYOUT, indexMapping = {@ViewDebug.IntToString(from = 2, to = "above"), @ViewDebug.IntToString(from = 4, to = "alignBaseline"), @ViewDebug.IntToString(from = 8, to = "alignBottom"), @ViewDebug.IntToString(from = 5, to = "alignLeft"), @ViewDebug.IntToString(from = 12, to = "alignParentBottom"), @ViewDebug.IntToString(from = 9, to = "alignParentLeft"), @ViewDebug.IntToString(from = 11, to = "alignParentRight"), @ViewDebug.IntToString(from = 10, to = "alignParentTop"), @ViewDebug.IntToString(from = 7, to = "alignRight"), @ViewDebug.IntToString(from = 6, to = "alignTop"), @ViewDebug.IntToString(from = 3, to = "below"), @ViewDebug.IntToString(from = 14, to = "centerHorizontal"), @ViewDebug.IntToString(from = 13, to = "center"), @ViewDebug.IntToString(from = 15, to = "centerVertical"), @ViewDebug.IntToString(from = 0, to = "leftOf"), @ViewDebug.IntToString(from = 1, to = "rightOf"), @ViewDebug.IntToString(from = 18, to = "alignStart"), @ViewDebug.IntToString(from = 19, to = "alignEnd"), @ViewDebug.IntToString(from = 20, to = "alignParentStart"), @ViewDebug.IntToString(from = 21, to = "alignParentEnd"), @ViewDebug.IntToString(from = 16, to = "startOf"), @ViewDebug.IntToString(from = 17, to = "endOf")}, mapping = {@ViewDebug.IntToString(from = -1, to = "true"), @ViewDebug.IntToString(from = 0, to = "false/NO_ID")}, resolveId = true)
        private int[] mRules;
        private boolean mRulesChanged;
        private int mTop;

        static int access$112(LayoutParams layoutParams, int i) {
            int i2 = layoutParams.mLeft + i;
            layoutParams.mLeft = i2;
            return i2;
        }

        static int access$120(LayoutParams layoutParams, int i) {
            int i2 = layoutParams.mLeft - i;
            layoutParams.mLeft = i2;
            return i2;
        }

        static int access$212(LayoutParams layoutParams, int i) {
            int i2 = layoutParams.mRight + i;
            layoutParams.mRight = i2;
            return i2;
        }

        static int access$220(LayoutParams layoutParams, int i) {
            int i2 = layoutParams.mRight - i;
            layoutParams.mRight = i2;
            return i2;
        }

        static int access$312(LayoutParams layoutParams, int i) {
            int i2 = layoutParams.mBottom + i;
            layoutParams.mBottom = i2;
            return i2;
        }

        static int access$412(LayoutParams layoutParams, int i) {
            int i2 = layoutParams.mTop + i;
            layoutParams.mTop = i2;
            return i2;
        }

        public LayoutParams(Context context, AttributeSet attributeSet) {
            super(context, attributeSet);
            this.mRules = new int[22];
            this.mInitialRules = new int[22];
            this.mRulesChanged = false;
            this.mIsRtlCompatibilityMode = false;
            TypedArray typedArrayObtainStyledAttributes = context.obtainStyledAttributes(attributeSet, R.styleable.RelativeLayout_Layout);
            this.mIsRtlCompatibilityMode = context.getApplicationInfo().targetSdkVersion < 17 || !context.getApplicationInfo().hasRtlSupport();
            int[] iArr = this.mRules;
            int[] iArr2 = this.mInitialRules;
            int indexCount = typedArrayObtainStyledAttributes.getIndexCount();
            for (int i = 0; i < indexCount; i++) {
                int index = typedArrayObtainStyledAttributes.getIndex(i);
                switch (index) {
                    case 0:
                        iArr[0] = typedArrayObtainStyledAttributes.getResourceId(index, 0);
                        break;
                    case 1:
                        iArr[1] = typedArrayObtainStyledAttributes.getResourceId(index, 0);
                        break;
                    case 2:
                        iArr[2] = typedArrayObtainStyledAttributes.getResourceId(index, 0);
                        break;
                    case 3:
                        iArr[3] = typedArrayObtainStyledAttributes.getResourceId(index, 0);
                        break;
                    case 4:
                        iArr[4] = typedArrayObtainStyledAttributes.getResourceId(index, 0);
                        break;
                    case 5:
                        iArr[5] = typedArrayObtainStyledAttributes.getResourceId(index, 0);
                        break;
                    case 6:
                        iArr[6] = typedArrayObtainStyledAttributes.getResourceId(index, 0);
                        break;
                    case 7:
                        iArr[7] = typedArrayObtainStyledAttributes.getResourceId(index, 0);
                        break;
                    case 8:
                        iArr[8] = typedArrayObtainStyledAttributes.getResourceId(index, 0);
                        break;
                    case 9:
                        iArr[9] = typedArrayObtainStyledAttributes.getBoolean(index, false) ? -1 : 0;
                        break;
                    case 10:
                        iArr[10] = typedArrayObtainStyledAttributes.getBoolean(index, false) ? -1 : 0;
                        break;
                    case 11:
                        iArr[11] = typedArrayObtainStyledAttributes.getBoolean(index, false) ? -1 : 0;
                        break;
                    case 12:
                        iArr[12] = typedArrayObtainStyledAttributes.getBoolean(index, false) ? -1 : 0;
                        break;
                    case 13:
                        iArr[13] = typedArrayObtainStyledAttributes.getBoolean(index, false) ? -1 : 0;
                        break;
                    case 14:
                        iArr[14] = typedArrayObtainStyledAttributes.getBoolean(index, false) ? -1 : 0;
                        break;
                    case 15:
                        iArr[15] = typedArrayObtainStyledAttributes.getBoolean(index, false) ? -1 : 0;
                        break;
                    case 16:
                        this.alignWithParent = typedArrayObtainStyledAttributes.getBoolean(index, false);
                        break;
                    case 17:
                        iArr[16] = typedArrayObtainStyledAttributes.getResourceId(index, 0);
                        break;
                    case 18:
                        iArr[17] = typedArrayObtainStyledAttributes.getResourceId(index, 0);
                        break;
                    case 19:
                        iArr[18] = typedArrayObtainStyledAttributes.getResourceId(index, 0);
                        break;
                    case 20:
                        iArr[19] = typedArrayObtainStyledAttributes.getResourceId(index, 0);
                        break;
                    case 21:
                        iArr[20] = typedArrayObtainStyledAttributes.getBoolean(index, false) ? -1 : 0;
                        break;
                    case 22:
                        iArr[21] = typedArrayObtainStyledAttributes.getBoolean(index, false) ? -1 : 0;
                        break;
                }
            }
            this.mRulesChanged = true;
            System.arraycopy(iArr, 0, iArr2, 0, 22);
            typedArrayObtainStyledAttributes.recycle();
        }

        public LayoutParams(int i, int i2) {
            super(i, i2);
            this.mRules = new int[22];
            this.mInitialRules = new int[22];
            this.mRulesChanged = false;
            this.mIsRtlCompatibilityMode = false;
        }

        public LayoutParams(ViewGroup.LayoutParams layoutParams) {
            super(layoutParams);
            this.mRules = new int[22];
            this.mInitialRules = new int[22];
            this.mRulesChanged = false;
            this.mIsRtlCompatibilityMode = false;
        }

        public LayoutParams(ViewGroup.MarginLayoutParams marginLayoutParams) {
            super(marginLayoutParams);
            this.mRules = new int[22];
            this.mInitialRules = new int[22];
            this.mRulesChanged = false;
            this.mIsRtlCompatibilityMode = false;
        }

        public LayoutParams(LayoutParams layoutParams) {
            super((ViewGroup.MarginLayoutParams) layoutParams);
            this.mRules = new int[22];
            this.mInitialRules = new int[22];
            this.mRulesChanged = false;
            this.mIsRtlCompatibilityMode = false;
            this.mIsRtlCompatibilityMode = layoutParams.mIsRtlCompatibilityMode;
            this.mRulesChanged = layoutParams.mRulesChanged;
            this.alignWithParent = layoutParams.alignWithParent;
            System.arraycopy(layoutParams.mRules, 0, this.mRules, 0, 22);
            System.arraycopy(layoutParams.mInitialRules, 0, this.mInitialRules, 0, 22);
        }

        @Override
        public String debug(String str) {
            return str + "ViewGroup.LayoutParams={ width=" + sizeToString(this.width) + ", height=" + sizeToString(this.height) + " }";
        }

        public void addRule(int i) {
            addRule(i, -1);
        }

        public void addRule(int i, int i2) {
            if (!this.mNeedsLayoutResolution && isRelativeRule(i) && this.mInitialRules[i] != 0 && i2 == 0) {
                this.mNeedsLayoutResolution = true;
            }
            this.mRules[i] = i2;
            this.mInitialRules[i] = i2;
            this.mRulesChanged = true;
        }

        public void removeRule(int i) {
            addRule(i, 0);
        }

        public int getRule(int i) {
            return this.mRules[i];
        }

        private boolean hasRelativeRules() {
            return (this.mInitialRules[16] == 0 && this.mInitialRules[17] == 0 && this.mInitialRules[18] == 0 && this.mInitialRules[19] == 0 && this.mInitialRules[20] == 0 && this.mInitialRules[21] == 0) ? false : true;
        }

        private boolean isRelativeRule(int i) {
            return i == 16 || i == 17 || i == 18 || i == 19 || i == 20 || i == 21;
        }

        private void resolveRules(int i) {
            char c = i == 1 ? (char) 1 : (char) 0;
            System.arraycopy(this.mInitialRules, 0, this.mRules, 0, 22);
            if (this.mIsRtlCompatibilityMode) {
                if (this.mRules[18] != 0) {
                    if (this.mRules[5] == 0) {
                        this.mRules[5] = this.mRules[18];
                    }
                    this.mRules[18] = 0;
                }
                if (this.mRules[19] != 0) {
                    if (this.mRules[7] == 0) {
                        this.mRules[7] = this.mRules[19];
                    }
                    this.mRules[19] = 0;
                }
                if (this.mRules[16] != 0) {
                    if (this.mRules[0] == 0) {
                        this.mRules[0] = this.mRules[16];
                    }
                    this.mRules[16] = 0;
                }
                if (this.mRules[17] != 0) {
                    if (this.mRules[1] == 0) {
                        this.mRules[1] = this.mRules[17];
                    }
                    this.mRules[17] = 0;
                }
                if (this.mRules[20] != 0) {
                    if (this.mRules[9] == 0) {
                        this.mRules[9] = this.mRules[20];
                    }
                    this.mRules[20] = 0;
                }
                if (this.mRules[21] != 0) {
                    if (this.mRules[11] == 0) {
                        this.mRules[11] = this.mRules[21];
                    }
                    this.mRules[21] = 0;
                }
            } else {
                if ((this.mRules[18] != 0 || this.mRules[19] != 0) && (this.mRules[5] != 0 || this.mRules[7] != 0)) {
                    this.mRules[5] = 0;
                    this.mRules[7] = 0;
                }
                if (this.mRules[18] != 0) {
                    this.mRules[c != 0 ? (char) 7 : (char) 5] = this.mRules[18];
                    this.mRules[18] = 0;
                }
                if (this.mRules[19] != 0) {
                    this.mRules[c != 0 ? (char) 5 : (char) 7] = this.mRules[19];
                    this.mRules[19] = 0;
                }
                if ((this.mRules[16] != 0 || this.mRules[17] != 0) && (this.mRules[0] != 0 || this.mRules[1] != 0)) {
                    this.mRules[0] = 0;
                    this.mRules[1] = 0;
                }
                if (this.mRules[16] != 0) {
                    this.mRules[c] = this.mRules[16];
                    this.mRules[16] = 0;
                }
                if (this.mRules[17] != 0) {
                    this.mRules[c ^ 1] = this.mRules[17];
                    this.mRules[17] = 0;
                }
                if ((this.mRules[20] != 0 || this.mRules[21] != 0) && (this.mRules[9] != 0 || this.mRules[11] != 0)) {
                    this.mRules[9] = 0;
                    this.mRules[11] = 0;
                }
                if (this.mRules[20] != 0) {
                    this.mRules[c != 0 ? (char) 11 : '\t'] = this.mRules[20];
                    this.mRules[20] = 0;
                }
                if (this.mRules[21] != 0) {
                    this.mRules[c != 0 ? '\t' : (char) 11] = this.mRules[21];
                    this.mRules[21] = 0;
                }
            }
            this.mRulesChanged = false;
            this.mNeedsLayoutResolution = false;
        }

        public int[] getRules(int i) {
            resolveLayoutDirection(i);
            return this.mRules;
        }

        public int[] getRules() {
            return this.mRules;
        }

        @Override
        public void resolveLayoutDirection(int i) {
            if (shouldResolveLayoutDirection(i)) {
                resolveRules(i);
            }
            super.resolveLayoutDirection(i);
        }

        private boolean shouldResolveLayoutDirection(int i) {
            return (this.mNeedsLayoutResolution || hasRelativeRules()) && (this.mRulesChanged || i != getLayoutDirection());
        }

        @Override
        protected void encodeProperties(ViewHierarchyEncoder viewHierarchyEncoder) {
            super.encodeProperties(viewHierarchyEncoder);
            viewHierarchyEncoder.addProperty("layout:alignWithParent", this.alignWithParent);
        }
    }

    private static class DependencyGraph {
        private SparseArray<Node> mKeyNodes;
        private ArrayList<Node> mNodes;
        private ArrayDeque<Node> mRoots;

        private DependencyGraph() {
            this.mNodes = new ArrayList<>();
            this.mKeyNodes = new SparseArray<>();
            this.mRoots = new ArrayDeque<>();
        }

        void clear() {
            ArrayList<Node> arrayList = this.mNodes;
            int size = arrayList.size();
            for (int i = 0; i < size; i++) {
                arrayList.get(i).release();
            }
            arrayList.clear();
            this.mKeyNodes.clear();
            this.mRoots.clear();
        }

        void add(View view) {
            int id = view.getId();
            Node nodeAcquire = Node.acquire(view);
            if (id != -1) {
                this.mKeyNodes.put(id, nodeAcquire);
            }
            this.mNodes.add(nodeAcquire);
        }

        void getSortedViews(View[] viewArr, int... iArr) {
            ArrayDeque<Node> arrayDequeFindRoots = findRoots(iArr);
            int i = 0;
            while (true) {
                Node nodePollLast = arrayDequeFindRoots.pollLast();
                if (nodePollLast == null) {
                    break;
                }
                View view = nodePollLast.view;
                int id = view.getId();
                int i2 = i + 1;
                viewArr[i] = view;
                ArrayMap<Node, DependencyGraph> arrayMap = nodePollLast.dependents;
                int size = arrayMap.size();
                for (int i3 = 0; i3 < size; i3++) {
                    Node nodeKeyAt = arrayMap.keyAt(i3);
                    SparseArray<Node> sparseArray = nodeKeyAt.dependencies;
                    sparseArray.remove(id);
                    if (sparseArray.size() == 0) {
                        arrayDequeFindRoots.add(nodeKeyAt);
                    }
                }
                i = i2;
            }
            if (i < viewArr.length) {
                throw new IllegalStateException("Circular dependencies cannot exist in RelativeLayout");
            }
        }

        private ArrayDeque<Node> findRoots(int[] iArr) {
            Node node;
            SparseArray<Node> sparseArray = this.mKeyNodes;
            ArrayList<Node> arrayList = this.mNodes;
            int size = arrayList.size();
            for (int i = 0; i < size; i++) {
                Node node2 = arrayList.get(i);
                node2.dependents.clear();
                node2.dependencies.clear();
            }
            for (int i2 = 0; i2 < size; i2++) {
                Node node3 = arrayList.get(i2);
                int[] iArr2 = ((LayoutParams) node3.view.getLayoutParams()).mRules;
                for (int i3 : iArr) {
                    int i4 = iArr2[i3];
                    if (i4 > 0 && (node = sparseArray.get(i4)) != null && node != node3) {
                        node.dependents.put(node3, this);
                        node3.dependencies.put(i4, node);
                    }
                }
            }
            ArrayDeque<Node> arrayDeque = this.mRoots;
            arrayDeque.clear();
            for (int i5 = 0; i5 < size; i5++) {
                Node node4 = arrayList.get(i5);
                if (node4.dependencies.size() == 0) {
                    arrayDeque.addLast(node4);
                }
            }
            return arrayDeque;
        }

        static class Node {
            private static final int POOL_LIMIT = 100;
            private static final Pools.SynchronizedPool<Node> sPool = new Pools.SynchronizedPool<>(100);
            View view;
            final ArrayMap<Node, DependencyGraph> dependents = new ArrayMap<>();
            final SparseArray<Node> dependencies = new SparseArray<>();

            Node() {
            }

            static Node acquire(View view) {
                Node nodeAcquire = sPool.acquire();
                if (nodeAcquire == null) {
                    nodeAcquire = new Node();
                }
                nodeAcquire.view = view;
                return nodeAcquire;
            }

            void release() {
                this.view = null;
                this.dependents.clear();
                this.dependencies.clear();
                sPool.release(this);
            }
        }
    }
}
