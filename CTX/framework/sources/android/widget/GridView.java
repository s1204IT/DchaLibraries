package android.widget;

import android.content.Context;
import android.content.Intent;
import android.content.res.TypedArray;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Trace;
import android.util.AttributeSet;
import android.util.MathUtils;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.RemotableViewMethod;
import android.view.SoundEffectConstants;
import android.view.View;
import android.view.ViewDebug;
import android.view.ViewGroup;
import android.view.ViewHierarchyEncoder;
import android.view.ViewRootImpl;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.accessibility.AccessibilityNodeProvider;
import android.view.animation.GridLayoutAnimationController;
import android.widget.AbsListView;
import android.widget.RemoteViews;
import com.android.internal.R;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@RemoteViews.RemoteView
public class GridView extends AbsListView {
    public static final int AUTO_FIT = -1;
    public static final int NO_STRETCH = 0;
    public static final int STRETCH_COLUMN_WIDTH = 2;
    public static final int STRETCH_SPACING = 1;
    public static final int STRETCH_SPACING_UNIFORM = 3;
    private int mColumnWidth;
    private int mGravity;
    private int mHorizontalSpacing;
    private int mNumColumns;
    private View mReferenceView;
    private View mReferenceViewInSelectedRow;
    private int mRequestedColumnWidth;
    private int mRequestedHorizontalSpacing;
    private int mRequestedNumColumns;
    private int mStretchMode;
    private final Rect mTempRect;
    private int mVerticalSpacing;

    @Retention(RetentionPolicy.SOURCE)
    public @interface StretchMode {
    }

    public GridView(Context context) {
        this(context, null);
    }

    public GridView(Context context, AttributeSet attributeSet) {
        this(context, attributeSet, 16842865);
    }

    public GridView(Context context, AttributeSet attributeSet, int i) {
        this(context, attributeSet, i, 0);
    }

    public GridView(Context context, AttributeSet attributeSet, int i, int i2) {
        super(context, attributeSet, i, i2);
        this.mNumColumns = -1;
        this.mHorizontalSpacing = 0;
        this.mVerticalSpacing = 0;
        this.mStretchMode = 2;
        this.mReferenceView = null;
        this.mReferenceViewInSelectedRow = null;
        this.mGravity = Gravity.START;
        this.mTempRect = new Rect();
        TypedArray typedArrayObtainStyledAttributes = context.obtainStyledAttributes(attributeSet, R.styleable.GridView, i, i2);
        setHorizontalSpacing(typedArrayObtainStyledAttributes.getDimensionPixelOffset(1, 0));
        setVerticalSpacing(typedArrayObtainStyledAttributes.getDimensionPixelOffset(2, 0));
        int i3 = typedArrayObtainStyledAttributes.getInt(3, 2);
        if (i3 >= 0) {
            setStretchMode(i3);
        }
        int dimensionPixelOffset = typedArrayObtainStyledAttributes.getDimensionPixelOffset(4, -1);
        if (dimensionPixelOffset > 0) {
            setColumnWidth(dimensionPixelOffset);
        }
        setNumColumns(typedArrayObtainStyledAttributes.getInt(5, 1));
        int i4 = typedArrayObtainStyledAttributes.getInt(0, -1);
        if (i4 >= 0) {
            setGravity(i4);
        }
        typedArrayObtainStyledAttributes.recycle();
    }

    @Override
    public ListAdapter getAdapter() {
        return this.mAdapter;
    }

    @Override
    @RemotableViewMethod(asyncImpl = "setRemoteViewsAdapterAsync")
    public void setRemoteViewsAdapter(Intent intent) {
        super.setRemoteViewsAdapter(intent);
    }

    @Override
    public void setAdapter(ListAdapter listAdapter) {
        int iLookForSelectablePosition;
        if (this.mAdapter != null && this.mDataSetObserver != null) {
            this.mAdapter.unregisterDataSetObserver(this.mDataSetObserver);
        }
        resetList();
        this.mRecycler.clear();
        this.mAdapter = listAdapter;
        this.mOldSelectedPosition = -1;
        this.mOldSelectedRowId = Long.MIN_VALUE;
        super.setAdapter(listAdapter);
        if (this.mAdapter != null) {
            this.mOldItemCount = this.mItemCount;
            this.mItemCount = this.mAdapter.getCount();
            this.mDataChanged = true;
            checkFocus();
            this.mDataSetObserver = new AbsListView.AdapterDataSetObserver();
            this.mAdapter.registerDataSetObserver(this.mDataSetObserver);
            this.mRecycler.setViewTypeCount(this.mAdapter.getViewTypeCount());
            if (this.mStackFromBottom) {
                iLookForSelectablePosition = lookForSelectablePosition(this.mItemCount - 1, false);
            } else {
                iLookForSelectablePosition = lookForSelectablePosition(0, true);
            }
            setSelectedPositionInt(iLookForSelectablePosition);
            setNextSelectedPositionInt(iLookForSelectablePosition);
            checkSelectionChanged();
        } else {
            checkFocus();
            checkSelectionChanged();
        }
        requestLayout();
    }

    @Override
    int lookForSelectablePosition(int i, boolean z) {
        if (this.mAdapter == null || isInTouchMode() || i < 0 || i >= this.mItemCount) {
            return -1;
        }
        return i;
    }

    @Override
    void fillGap(boolean z) {
        int listPaddingBottom;
        int i;
        int i2 = this.mNumColumns;
        int i3 = this.mVerticalSpacing;
        int childCount = getChildCount();
        int bottom = 0;
        if (z) {
            if ((this.mGroupFlags & 34) == 34) {
                bottom = getListPaddingTop();
            }
            if (childCount > 0) {
                bottom = getChildAt(childCount - 1).getBottom() + i3;
            }
            int i4 = this.mFirstPosition + childCount;
            if (this.mStackFromBottom) {
                i4 += i2 - 1;
            }
            fillDown(i4, bottom);
            correctTooHigh(i2, i3, getChildCount());
            return;
        }
        if ((this.mGroupFlags & 34) == 34) {
            listPaddingBottom = getListPaddingBottom();
        } else {
            listPaddingBottom = 0;
        }
        int top = childCount > 0 ? getChildAt(0).getTop() - i3 : getHeight() - listPaddingBottom;
        int i5 = this.mFirstPosition;
        if (!this.mStackFromBottom) {
            i = i5 - i2;
        } else {
            i = i5 - 1;
        }
        fillUp(i, top);
        correctTooLow(i2, i3, getChildCount());
    }

    private View fillDown(int i, int i2) {
        int i3 = this.mBottom - this.mTop;
        View view = null;
        if ((this.mGroupFlags & 34) == 34) {
            i3 -= this.mListPadding.bottom;
        }
        while (i2 < i3 && i < this.mItemCount) {
            View viewMakeRow = makeRow(i, i2, true);
            if (viewMakeRow != null) {
                view = viewMakeRow;
            }
            i2 = this.mReferenceView.getBottom() + this.mVerticalSpacing;
            i += this.mNumColumns;
        }
        setVisibleRangeHint(this.mFirstPosition, (this.mFirstPosition + getChildCount()) - 1);
        return view;
    }

    private View makeRow(int i, int i2, boolean z) {
        int width;
        int iMin;
        int i3;
        int i4;
        int i5 = this.mColumnWidth;
        int i6 = this.mHorizontalSpacing;
        boolean zIsLayoutRtl = isLayoutRtl();
        boolean z2 = false;
        if (zIsLayoutRtl) {
            width = ((getWidth() - this.mListPadding.right) - i5) - (this.mStretchMode == 3 ? i6 : 0);
        } else {
            width = this.mListPadding.left + (this.mStretchMode == 3 ? i6 : 0);
        }
        if (!this.mStackFromBottom) {
            iMin = Math.min(i + this.mNumColumns, this.mItemCount);
            i3 = i;
        } else {
            iMin = i + 1;
            int iMax = Math.max(0, (i - this.mNumColumns) + 1);
            int i7 = iMin - iMax;
            if (i7 < this.mNumColumns) {
                width += (zIsLayoutRtl ? -1 : 1) * (this.mNumColumns - i7) * (i5 + i6);
            }
            i3 = iMax;
        }
        int i8 = iMin;
        boolean zShouldShowSelector = shouldShowSelector();
        boolean z3 = touchModeDrawsInPressedState();
        int i9 = this.mSelectedPosition;
        int i10 = zIsLayoutRtl ? -1 : 1;
        View viewMakeAndAddView = null;
        View view = null;
        int i11 = width;
        int i12 = i3;
        while (i12 < i8) {
            boolean z4 = i12 == i9 ? true : z2;
            if (!z) {
                i4 = i12 - i3;
            } else {
                i4 = -1;
            }
            int i13 = i12;
            int i14 = i9;
            viewMakeAndAddView = makeAndAddView(i12, i2, z, i11, z4, i4);
            i11 += i10 * i5;
            if (i13 < i8 - 1) {
                i11 += i10 * i6;
            }
            if (z4 && (zShouldShowSelector || z3)) {
                view = viewMakeAndAddView;
            }
            i12 = i13 + 1;
            i9 = i14;
            z2 = false;
        }
        this.mReferenceView = viewMakeAndAddView;
        if (view != null) {
            this.mReferenceViewInSelectedRow = this.mReferenceView;
        }
        return view;
    }

    private View fillUp(int i, int i2) {
        int i3;
        View view = null;
        if ((this.mGroupFlags & 34) == 34) {
            i3 = this.mListPadding.top;
        } else {
            i3 = 0;
        }
        while (i2 > i3 && i >= 0) {
            View viewMakeRow = makeRow(i, i2, false);
            if (viewMakeRow != null) {
                view = viewMakeRow;
            }
            i2 = this.mReferenceView.getTop() - this.mVerticalSpacing;
            this.mFirstPosition = i;
            i -= this.mNumColumns;
        }
        if (this.mStackFromBottom) {
            this.mFirstPosition = Math.max(0, i + 1);
        }
        setVisibleRangeHint(this.mFirstPosition, (this.mFirstPosition + getChildCount()) - 1);
        return view;
    }

    private View fillFromTop(int i) {
        this.mFirstPosition = Math.min(this.mFirstPosition, this.mSelectedPosition);
        this.mFirstPosition = Math.min(this.mFirstPosition, this.mItemCount - 1);
        if (this.mFirstPosition < 0) {
            this.mFirstPosition = 0;
        }
        this.mFirstPosition -= this.mFirstPosition % this.mNumColumns;
        return fillDown(this.mFirstPosition, i);
    }

    private View fillFromBottom(int i, int i2) {
        int iMin = (this.mItemCount - 1) - Math.min(Math.max(i, this.mSelectedPosition), this.mItemCount - 1);
        return fillUp((this.mItemCount - 1) - (iMin - (iMin % this.mNumColumns)), i2);
    }

    private View fillSelection(int i, int i2) {
        int i3;
        int iMax;
        int iReconcileSelectedPosition = reconcileSelectedPosition();
        int i4 = this.mNumColumns;
        int i5 = this.mVerticalSpacing;
        if (!this.mStackFromBottom) {
            iMax = iReconcileSelectedPosition - (iReconcileSelectedPosition % i4);
            i3 = -1;
        } else {
            int i6 = (this.mItemCount - 1) - iReconcileSelectedPosition;
            i3 = (this.mItemCount - 1) - (i6 - (i6 % i4));
            iMax = Math.max(0, (i3 - i4) + 1);
        }
        int verticalFadingEdgeLength = getVerticalFadingEdgeLength();
        View viewMakeRow = makeRow(this.mStackFromBottom ? i3 : iMax, getTopSelectionPixel(i, verticalFadingEdgeLength, iMax), true);
        this.mFirstPosition = iMax;
        View view = this.mReferenceView;
        if (!this.mStackFromBottom) {
            fillDown(iMax + i4, view.getBottom() + i5);
            pinToBottom(i2);
            fillUp(iMax - i4, view.getTop() - i5);
            adjustViewsUpOrDown();
        } else {
            offsetChildrenTopAndBottom(getBottomSelectionPixel(i2, verticalFadingEdgeLength, i4, iMax) - view.getBottom());
            fillUp(iMax - 1, view.getTop() - i5);
            pinToTop(i);
            fillDown(i3 + i4, view.getBottom() + i5);
            adjustViewsUpOrDown();
        }
        return viewMakeRow;
    }

    private void pinToTop(int i) {
        int top;
        if (this.mFirstPosition == 0 && (top = i - getChildAt(0).getTop()) < 0) {
            offsetChildrenTopAndBottom(top);
        }
    }

    private void pinToBottom(int i) {
        int bottom;
        int childCount = getChildCount();
        if (this.mFirstPosition + childCount == this.mItemCount && (bottom = i - getChildAt(childCount - 1).getBottom()) > 0) {
            offsetChildrenTopAndBottom(bottom);
        }
    }

    @Override
    int findMotionRow(int i) {
        int childCount = getChildCount();
        if (childCount > 0) {
            int i2 = this.mNumColumns;
            if (!this.mStackFromBottom) {
                for (int i3 = 0; i3 < childCount; i3 += i2) {
                    if (i <= getChildAt(i3).getBottom()) {
                        return this.mFirstPosition + i3;
                    }
                }
                return -1;
            }
            for (int i4 = childCount - 1; i4 >= 0; i4 -= i2) {
                if (i >= getChildAt(i4).getTop()) {
                    return this.mFirstPosition + i4;
                }
            }
            return -1;
        }
        return -1;
    }

    private View fillSpecific(int i, int i2) {
        int i3;
        int iMax;
        View viewFillDown;
        View viewFillUp;
        int i4 = this.mNumColumns;
        if (!this.mStackFromBottom) {
            iMax = i - (i % i4);
            i3 = -1;
        } else {
            int i5 = (this.mItemCount - 1) - i;
            i3 = (this.mItemCount - 1) - (i5 - (i5 % i4));
            iMax = Math.max(0, (i3 - i4) + 1);
        }
        View viewMakeRow = makeRow(this.mStackFromBottom ? i3 : iMax, i2, true);
        this.mFirstPosition = iMax;
        View view = this.mReferenceView;
        if (view == null) {
            return null;
        }
        int i6 = this.mVerticalSpacing;
        if (!this.mStackFromBottom) {
            View viewFillUp2 = fillUp(iMax - i4, view.getTop() - i6);
            adjustViewsUpOrDown();
            View viewFillDown2 = fillDown(iMax + i4, view.getBottom() + i6);
            int childCount = getChildCount();
            if (childCount > 0) {
                correctTooHigh(i4, i6, childCount);
            }
            viewFillDown = viewFillDown2;
            viewFillUp = viewFillUp2;
        } else {
            viewFillDown = fillDown(i3 + i4, view.getBottom() + i6);
            adjustViewsUpOrDown();
            viewFillUp = fillUp(iMax - 1, view.getTop() - i6);
            int childCount2 = getChildCount();
            if (childCount2 > 0) {
                correctTooLow(i4, i6, childCount2);
            }
        }
        if (viewMakeRow != null) {
            return viewMakeRow;
        }
        if (viewFillUp != null) {
            return viewFillUp;
        }
        return viewFillDown;
    }

    private void correctTooHigh(int i, int i2, int i3) {
        if ((this.mFirstPosition + i3) - 1 == this.mItemCount - 1 && i3 > 0) {
            int bottom = ((this.mBottom - this.mTop) - this.mListPadding.bottom) - getChildAt(i3 - 1).getBottom();
            View childAt = getChildAt(0);
            int top = childAt.getTop();
            if (bottom > 0) {
                if (this.mFirstPosition > 0 || top < this.mListPadding.top) {
                    if (this.mFirstPosition == 0) {
                        bottom = Math.min(bottom, this.mListPadding.top - top);
                    }
                    offsetChildrenTopAndBottom(bottom);
                    if (this.mFirstPosition > 0) {
                        int i4 = this.mFirstPosition;
                        if (this.mStackFromBottom) {
                            i = 1;
                        }
                        fillUp(i4 - i, childAt.getTop() - i2);
                        adjustViewsUpOrDown();
                    }
                }
            }
        }
    }

    private void correctTooLow(int i, int i2, int i3) {
        if (this.mFirstPosition == 0 && i3 > 0) {
            int top = getChildAt(0).getTop();
            int i4 = this.mListPadding.top;
            int i5 = (this.mBottom - this.mTop) - this.mListPadding.bottom;
            int iMin = top - i4;
            View childAt = getChildAt(i3 - 1);
            int bottom = childAt.getBottom();
            int i6 = (this.mFirstPosition + i3) - 1;
            if (iMin > 0) {
                if (i6 < this.mItemCount - 1 || bottom > i5) {
                    if (i6 == this.mItemCount - 1) {
                        iMin = Math.min(iMin, bottom - i5);
                    }
                    offsetChildrenTopAndBottom(-iMin);
                    if (i6 < this.mItemCount - 1) {
                        if (!this.mStackFromBottom) {
                            i = 1;
                        }
                        fillDown(i6 + i, childAt.getBottom() + i2);
                        adjustViewsUpOrDown();
                    }
                }
            }
        }
    }

    private View fillFromSelection(int i, int i2, int i3) {
        int i4;
        int iMax;
        int verticalFadingEdgeLength = getVerticalFadingEdgeLength();
        int i5 = this.mSelectedPosition;
        int i6 = this.mNumColumns;
        int i7 = this.mVerticalSpacing;
        if (!this.mStackFromBottom) {
            iMax = i5 - (i5 % i6);
            i4 = -1;
        } else {
            int i8 = (this.mItemCount - 1) - i5;
            i4 = (this.mItemCount - 1) - (i8 - (i8 % i6));
            iMax = Math.max(0, (i4 - i6) + 1);
        }
        int topSelectionPixel = getTopSelectionPixel(i2, verticalFadingEdgeLength, iMax);
        int bottomSelectionPixel = getBottomSelectionPixel(i3, verticalFadingEdgeLength, i6, iMax);
        View viewMakeRow = makeRow(this.mStackFromBottom ? i4 : iMax, i, true);
        this.mFirstPosition = iMax;
        View view = this.mReferenceView;
        adjustForTopFadingEdge(view, topSelectionPixel, bottomSelectionPixel);
        adjustForBottomFadingEdge(view, topSelectionPixel, bottomSelectionPixel);
        if (!this.mStackFromBottom) {
            fillUp(iMax - i6, view.getTop() - i7);
            adjustViewsUpOrDown();
            fillDown(iMax + i6, view.getBottom() + i7);
        } else {
            fillDown(i4 + i6, view.getBottom() + i7);
            adjustViewsUpOrDown();
            fillUp(iMax - 1, view.getTop() - i7);
        }
        return viewMakeRow;
    }

    private int getBottomSelectionPixel(int i, int i2, int i3, int i4) {
        if ((i4 + i3) - 1 < this.mItemCount - 1) {
            return i - i2;
        }
        return i;
    }

    private int getTopSelectionPixel(int i, int i2, int i3) {
        if (i3 > 0) {
            return i + i2;
        }
        return i;
    }

    private void adjustForBottomFadingEdge(View view, int i, int i2) {
        if (view.getBottom() > i2) {
            offsetChildrenTopAndBottom(-Math.min(view.getTop() - i, view.getBottom() - i2));
        }
    }

    private void adjustForTopFadingEdge(View view, int i, int i2) {
        if (view.getTop() < i) {
            offsetChildrenTopAndBottom(Math.min(i - view.getTop(), i2 - view.getBottom()));
        }
    }

    @Override
    @RemotableViewMethod
    public void smoothScrollToPosition(int i) {
        super.smoothScrollToPosition(i);
    }

    @Override
    @RemotableViewMethod
    public void smoothScrollByOffset(int i) {
        super.smoothScrollByOffset(i);
    }

    private View moveSelection(int i, int i2, int i3) {
        int i4;
        int iMax;
        int i5;
        View viewMakeRow;
        View view;
        int top;
        int verticalFadingEdgeLength = getVerticalFadingEdgeLength();
        int i6 = this.mSelectedPosition;
        int i7 = this.mNumColumns;
        int i8 = this.mVerticalSpacing;
        if (!this.mStackFromBottom) {
            int i9 = i6 - i;
            iMax = i9 - (i9 % i7);
            i5 = i6 - (i6 % i7);
            i4 = -1;
        } else {
            int i10 = (this.mItemCount - 1) - i6;
            i4 = (this.mItemCount - 1) - (i10 - (i10 % i7));
            int iMax2 = Math.max(0, (i4 - i7) + 1);
            int i11 = (this.mItemCount - 1) - (i6 - i);
            iMax = Math.max(0, (((this.mItemCount - 1) - (i11 - (i11 % i7))) - i7) + 1);
            i5 = iMax2;
        }
        int i12 = i5 - iMax;
        int topSelectionPixel = getTopSelectionPixel(i2, verticalFadingEdgeLength, i5);
        int bottomSelectionPixel = getBottomSelectionPixel(i3, verticalFadingEdgeLength, i7, i5);
        this.mFirstPosition = i5;
        if (i12 > 0) {
            viewMakeRow = makeRow(this.mStackFromBottom ? i4 : i5, (this.mReferenceViewInSelectedRow != null ? this.mReferenceViewInSelectedRow.getBottom() : 0) + i8, true);
            view = this.mReferenceView;
            adjustForBottomFadingEdge(view, topSelectionPixel, bottomSelectionPixel);
        } else if (i12 < 0) {
            if (this.mReferenceViewInSelectedRow != null) {
                top = this.mReferenceViewInSelectedRow.getTop();
            } else {
                top = 0;
            }
            viewMakeRow = makeRow(this.mStackFromBottom ? i4 : i5, top - i8, false);
            view = this.mReferenceView;
            adjustForTopFadingEdge(view, topSelectionPixel, bottomSelectionPixel);
        } else {
            viewMakeRow = makeRow(this.mStackFromBottom ? i4 : i5, this.mReferenceViewInSelectedRow != null ? this.mReferenceViewInSelectedRow.getTop() : 0, true);
            view = this.mReferenceView;
        }
        if (!this.mStackFromBottom) {
            fillUp(i5 - i7, view.getTop() - i8);
            adjustViewsUpOrDown();
            fillDown(i5 + i7, view.getBottom() + i8);
        } else {
            fillDown(i4 + i7, view.getBottom() + i8);
            adjustViewsUpOrDown();
            fillUp(i5 - 1, view.getTop() - i8);
        }
        return viewMakeRow;
    }

    private boolean determineColumns(int i) {
        int i2 = this.mRequestedHorizontalSpacing;
        int i3 = this.mStretchMode;
        int i4 = this.mRequestedColumnWidth;
        if (this.mRequestedNumColumns == -1) {
            if (i4 > 0) {
                this.mNumColumns = (i + i2) / (i4 + i2);
            } else {
                this.mNumColumns = 2;
            }
        } else {
            this.mNumColumns = this.mRequestedNumColumns;
        }
        if (this.mNumColumns <= 0) {
            this.mNumColumns = 1;
        }
        boolean z = false;
        if (i3 == 0) {
            this.mColumnWidth = i4;
            this.mHorizontalSpacing = i2;
        } else {
            int i5 = (i - (this.mNumColumns * i4)) - ((this.mNumColumns - 1) * i2);
            if (i5 < 0) {
                z = true;
            }
            switch (i3) {
                case 1:
                    this.mColumnWidth = i4;
                    if (this.mNumColumns > 1) {
                        this.mHorizontalSpacing = i2 + (i5 / (this.mNumColumns - 1));
                    } else {
                        this.mHorizontalSpacing = i2 + i5;
                    }
                    break;
                case 2:
                    this.mColumnWidth = i4 + (i5 / this.mNumColumns);
                    this.mHorizontalSpacing = i2;
                    break;
                case 3:
                    this.mColumnWidth = i4;
                    if (this.mNumColumns > 1) {
                        this.mHorizontalSpacing = i2 + (i5 / (this.mNumColumns + 1));
                    } else {
                        this.mHorizontalSpacing = i2 + i5;
                    }
                    break;
            }
        }
        return z;
    }

    @Override
    protected void onMeasure(int i, int i2) {
        int measuredHeight;
        int i3;
        super.onMeasure(i, i2);
        int mode = View.MeasureSpec.getMode(i);
        int mode2 = View.MeasureSpec.getMode(i2);
        int size = View.MeasureSpec.getSize(i);
        int size2 = View.MeasureSpec.getSize(i2);
        if (mode == 0) {
            if (this.mColumnWidth > 0) {
                i3 = this.mColumnWidth + this.mListPadding.left + this.mListPadding.right;
            } else {
                i3 = this.mListPadding.left + this.mListPadding.right;
            }
            size = i3 + getVerticalScrollbarWidth();
        }
        boolean zDetermineColumns = determineColumns((size - this.mListPadding.left) - this.mListPadding.right);
        int i4 = 0;
        this.mItemCount = this.mAdapter == null ? 0 : this.mAdapter.getCount();
        int i5 = this.mItemCount;
        if (i5 > 0) {
            View viewObtainView = obtainView(0, this.mIsScrap);
            AbsListView.LayoutParams layoutParams = (AbsListView.LayoutParams) viewObtainView.getLayoutParams();
            if (layoutParams == null) {
                layoutParams = (AbsListView.LayoutParams) generateDefaultLayoutParams();
                viewObtainView.setLayoutParams(layoutParams);
            }
            layoutParams.viewType = this.mAdapter.getItemViewType(0);
            layoutParams.isEnabled = this.mAdapter.isEnabled(0);
            layoutParams.forceAdd = true;
            viewObtainView.measure(getChildMeasureSpec(View.MeasureSpec.makeMeasureSpec(this.mColumnWidth, 1073741824), 0, layoutParams.width), getChildMeasureSpec(View.MeasureSpec.makeSafeMeasureSpec(View.MeasureSpec.getSize(i2), 0), 0, layoutParams.height));
            measuredHeight = viewObtainView.getMeasuredHeight();
            combineMeasuredStates(0, viewObtainView.getMeasuredState());
            if (this.mRecycler.shouldRecycleViewType(layoutParams.viewType)) {
                this.mRecycler.addScrapView(viewObtainView, -1);
            }
        } else {
            measuredHeight = 0;
        }
        if (mode2 == 0) {
            size2 = this.mListPadding.top + this.mListPadding.bottom + measuredHeight + (getVerticalFadingEdgeLength() * 2);
        }
        if (mode2 == Integer.MIN_VALUE) {
            int i6 = this.mListPadding.top + this.mListPadding.bottom;
            int i7 = this.mNumColumns;
            while (true) {
                if (i4 < i5) {
                    i6 += measuredHeight;
                    i4 += i7;
                    if (i4 < i5) {
                        i6 += this.mVerticalSpacing;
                    }
                    if (i6 >= size2) {
                        break;
                    }
                } else {
                    size2 = i6;
                    break;
                }
            }
        }
        if (mode == Integer.MIN_VALUE && this.mRequestedNumColumns != -1 && ((this.mRequestedNumColumns * this.mColumnWidth) + ((this.mRequestedNumColumns - 1) * this.mHorizontalSpacing) + this.mListPadding.left + this.mListPadding.right > size || zDetermineColumns)) {
            size |= 16777216;
        }
        setMeasuredDimension(size, size2);
        this.mWidthMeasureSpec = i;
    }

    @Override
    protected void attachLayoutAnimationParameters(View view, ViewGroup.LayoutParams layoutParams, int i, int i2) {
        GridLayoutAnimationController.AnimationParameters animationParameters = (GridLayoutAnimationController.AnimationParameters) layoutParams.layoutAnimationParameters;
        if (animationParameters == null) {
            animationParameters = new GridLayoutAnimationController.AnimationParameters();
            layoutParams.layoutAnimationParameters = animationParameters;
        }
        animationParameters.count = i2;
        animationParameters.index = i;
        animationParameters.columnsCount = this.mNumColumns;
        animationParameters.rowsCount = i2 / this.mNumColumns;
        if (!this.mStackFromBottom) {
            animationParameters.column = i % this.mNumColumns;
            animationParameters.row = i / this.mNumColumns;
        } else {
            int i3 = (i2 - 1) - i;
            animationParameters.column = (this.mNumColumns - 1) - (i3 % this.mNumColumns);
            animationParameters.row = (animationParameters.rowsCount - 1) - (i3 / this.mNumColumns);
        }
    }

    @Override
    protected void layoutChildren() throws Throwable {
        boolean z;
        int i;
        View childAt;
        View childAt2;
        View childAt3;
        int positionForView;
        AccessibilityNodeInfo accessibilityFocusedVirtualView;
        View accessibilityFocusedHost;
        int i2;
        View viewFillFromTop;
        View childAt4;
        View accessibilityFocusedChild;
        boolean z2 = this.mBlockLayoutRequests;
        if (!z2) {
            this.mBlockLayoutRequests = true;
        }
        try {
            super.layoutChildren();
            invalidate();
            if (this.mAdapter == null) {
                resetList();
                invokeOnItemScrollListener();
                if (z2) {
                    return;
                }
                this.mBlockLayoutRequests = false;
                return;
            }
            int top = this.mListPadding.top;
            int i3 = (this.mBottom - this.mTop) - this.mListPadding.bottom;
            int childCount = getChildCount();
            switch (this.mLayoutMode) {
                case 1:
                case 3:
                case 4:
                case 5:
                    i = 0;
                    childAt = null;
                    childAt2 = null;
                    childAt3 = null;
                    break;
                case 2:
                    int i4 = this.mNextSelectedPosition - this.mFirstPosition;
                    if (i4 >= 0 && i4 < childCount) {
                        childAt = getChildAt(i4);
                        i = 0;
                        childAt2 = null;
                        childAt3 = null;
                    } else {
                        i = 0;
                        childAt = null;
                        childAt2 = null;
                        childAt3 = null;
                    }
                    break;
                case 6:
                    i = this.mNextSelectedPosition >= 0 ? this.mNextSelectedPosition - this.mSelectedPosition : 0;
                    childAt = null;
                    childAt2 = null;
                    childAt3 = null;
                    break;
                default:
                    int i5 = this.mSelectedPosition - this.mFirstPosition;
                    childAt2 = (i5 < 0 || i5 >= childCount) ? null : getChildAt(i5);
                    childAt3 = getChildAt(0);
                    childAt = null;
                    i = 0;
                    break;
            }
            boolean z3 = this.mDataChanged;
            if (z3) {
                handleDataChanged();
            }
            if (this.mItemCount == 0) {
                resetList();
                invokeOnItemScrollListener();
                if (z2) {
                    return;
                }
                this.mBlockLayoutRequests = false;
                return;
            }
            setSelectedPositionInt(this.mNextSelectedPosition);
            ViewRootImpl viewRootImpl = getViewRootImpl();
            if (viewRootImpl == null || (accessibilityFocusedHost = viewRootImpl.getAccessibilityFocusedHost()) == null || (accessibilityFocusedChild = getAccessibilityFocusedChild(accessibilityFocusedHost)) == null) {
                positionForView = -1;
                accessibilityFocusedVirtualView = null;
                accessibilityFocusedHost = null;
            } else {
                if (!z3 || accessibilityFocusedChild.hasTransientState() || this.mAdapterHasStableIds) {
                    accessibilityFocusedVirtualView = viewRootImpl.getAccessibilityFocusedVirtualView();
                } else {
                    accessibilityFocusedVirtualView = null;
                    accessibilityFocusedHost = null;
                }
                positionForView = getPositionForView(accessibilityFocusedChild);
            }
            int i6 = this.mFirstPosition;
            AbsListView.RecycleBin recycleBin = this.mRecycler;
            try {
                if (z3) {
                    int i7 = 0;
                    while (i7 < childCount) {
                        boolean z4 = z2;
                        recycleBin.addScrapView(getChildAt(i7), i6 + i7);
                        i7++;
                        z2 = z4;
                        positionForView = positionForView;
                    }
                    z = z2;
                    i2 = positionForView;
                } else {
                    z = z2;
                    i2 = positionForView;
                    recycleBin.fillActiveViews(childCount, i6);
                }
                detachAllViewsFromParent();
                recycleBin.removeSkippedScrap();
                switch (this.mLayoutMode) {
                    case 1:
                        this.mFirstPosition = 0;
                        viewFillFromTop = fillFromTop(top);
                        adjustViewsUpOrDown();
                        break;
                    case 2:
                        viewFillFromTop = childAt == null ? fillSelection(top, i3) : fillFromSelection(childAt.getTop(), top, i3);
                        break;
                    case 3:
                        viewFillFromTop = fillUp(this.mItemCount - 1, i3);
                        adjustViewsUpOrDown();
                        break;
                    case 4:
                        viewFillFromTop = fillSpecific(this.mSelectedPosition, this.mSpecificTop);
                        break;
                    case 5:
                        viewFillFromTop = fillSpecific(this.mSyncPosition, this.mSpecificTop);
                        break;
                    case 6:
                        viewFillFromTop = moveSelection(i, top, i3);
                        break;
                    default:
                        if (childCount != 0) {
                            if (this.mSelectedPosition >= 0 && this.mSelectedPosition < this.mItemCount) {
                                int i8 = this.mSelectedPosition;
                                if (childAt2 != null) {
                                    top = childAt2.getTop();
                                }
                                viewFillFromTop = fillSpecific(i8, top);
                            } else if (this.mFirstPosition >= this.mItemCount) {
                                viewFillFromTop = fillSpecific(0, top);
                            } else {
                                int i9 = this.mFirstPosition;
                                if (childAt3 != null) {
                                    top = childAt3.getTop();
                                }
                                viewFillFromTop = fillSpecific(i9, top);
                            }
                        } else if (!this.mStackFromBottom) {
                            setSelectedPositionInt((this.mAdapter == null || isInTouchMode()) ? -1 : 0);
                            viewFillFromTop = fillFromTop(top);
                        } else {
                            int i10 = this.mItemCount - 1;
                            setSelectedPositionInt((this.mAdapter == null || isInTouchMode()) ? -1 : i10);
                            viewFillFromTop = fillFromBottom(i10, i3);
                        }
                        break;
                }
                recycleBin.scrapActiveViews();
                if (viewFillFromTop != null) {
                    positionSelector(-1, viewFillFromTop);
                    this.mSelectedTop = viewFillFromTop.getTop();
                } else if (this.mTouchMode > 0 && this.mTouchMode < 3) {
                    View childAt5 = getChildAt(this.mMotionPosition - this.mFirstPosition);
                    if (childAt5 != null) {
                        positionSelector(this.mMotionPosition, childAt5);
                    }
                } else if (this.mSelectedPosition != -1) {
                    View childAt6 = getChildAt(this.mSelectorPosition - this.mFirstPosition);
                    if (childAt6 != null) {
                        positionSelector(this.mSelectorPosition, childAt6);
                    }
                } else {
                    this.mSelectedTop = 0;
                    this.mSelectorRect.setEmpty();
                }
                if (viewRootImpl != null && viewRootImpl.getAccessibilityFocusedHost() == null) {
                    if (accessibilityFocusedHost == null || !accessibilityFocusedHost.isAttachedToWindow()) {
                        int i11 = i2;
                        if (i11 != -1 && (childAt4 = getChildAt(MathUtils.constrain(i11 - this.mFirstPosition, 0, getChildCount() - 1))) != null) {
                            childAt4.requestAccessibilityFocus();
                        }
                    } else {
                        AccessibilityNodeProvider accessibilityNodeProvider = accessibilityFocusedHost.getAccessibilityNodeProvider();
                        if (accessibilityFocusedVirtualView == null || accessibilityNodeProvider == null) {
                            accessibilityFocusedHost.requestAccessibilityFocus();
                        } else {
                            accessibilityNodeProvider.performAction(AccessibilityNodeInfo.getVirtualDescendantId(accessibilityFocusedVirtualView.getSourceNodeId()), 64, null);
                        }
                    }
                }
                this.mLayoutMode = 0;
                this.mDataChanged = false;
                if (this.mPositionScrollAfterLayout != null) {
                    post(this.mPositionScrollAfterLayout);
                    this.mPositionScrollAfterLayout = null;
                }
                this.mNeedSync = false;
                setNextSelectedPositionInt(this.mSelectedPosition);
                updateScrollIndicators();
                if (this.mItemCount > 0) {
                    checkSelectionChanged();
                }
                invokeOnItemScrollListener();
                if (z) {
                    return;
                }
                this.mBlockLayoutRequests = false;
            } catch (Throwable th) {
                th = th;
                if (!z) {
                    this.mBlockLayoutRequests = false;
                }
                throw th;
            }
        } catch (Throwable th2) {
            th = th2;
            z = z2;
        }
    }

    private View makeAndAddView(int i, int i2, boolean z, int i3, boolean z2, int i4) {
        View activeView;
        if (!this.mDataChanged && (activeView = this.mRecycler.getActiveView(i)) != null) {
            setupChild(activeView, i, i2, z, i3, z2, true, i4);
            return activeView;
        }
        View viewObtainView = obtainView(i, this.mIsScrap);
        setupChild(viewObtainView, i, i2, z, i3, z2, this.mIsScrap[0], i4);
        return viewObtainView;
    }

    private void setupChild(View view, int i, int i2, boolean z, int i3, boolean z2, boolean z3, int i4) {
        int i5;
        int i6;
        Trace.traceBegin(8L, "setupGridItem");
        boolean z4 = z2 && shouldShowSelector();
        boolean z5 = z4 != view.isSelected();
        int i7 = this.mTouchMode;
        boolean z6 = i7 > 0 && i7 < 3 && this.mMotionPosition == i;
        boolean z7 = z6 != view.isPressed();
        boolean z8 = !z3 || z5 || view.isLayoutRequested();
        AbsListView.LayoutParams layoutParams = (AbsListView.LayoutParams) view.getLayoutParams();
        if (layoutParams == null) {
            layoutParams = (AbsListView.LayoutParams) generateDefaultLayoutParams();
        }
        AbsListView.LayoutParams layoutParams2 = layoutParams;
        layoutParams2.viewType = this.mAdapter.getItemViewType(i);
        layoutParams2.isEnabled = this.mAdapter.isEnabled(i);
        if (z5) {
            view.setSelected(z4);
            if (z4) {
                requestFocus();
            }
        }
        if (z7) {
            view.setPressed(z6);
        }
        if (this.mChoiceMode != 0 && this.mCheckStates != null) {
            if (view instanceof Checkable) {
                ((Checkable) view).setChecked(this.mCheckStates.get(i));
            } else if (getContext().getApplicationInfo().targetSdkVersion >= 11) {
                view.setActivated(this.mCheckStates.get(i));
            }
        }
        if (z3 && !layoutParams2.forceAdd) {
            attachViewToParent(view, i4, layoutParams2);
            if (!z3 || ((AbsListView.LayoutParams) view.getLayoutParams()).scrappedFromPosition != i) {
                view.jumpDrawablesToCurrentState();
            }
        } else {
            layoutParams2.forceAdd = false;
            addViewInLayout(view, i4, layoutParams2, true);
        }
        if (z8) {
            view.measure(ViewGroup.getChildMeasureSpec(View.MeasureSpec.makeMeasureSpec(this.mColumnWidth, 1073741824), 0, layoutParams2.width), ViewGroup.getChildMeasureSpec(View.MeasureSpec.makeMeasureSpec(0, 0), 0, layoutParams2.height));
        } else {
            cleanupLayoutState(view);
        }
        int measuredWidth = view.getMeasuredWidth();
        int measuredHeight = view.getMeasuredHeight();
        if (!z) {
            i5 = i2 - measuredHeight;
        } else {
            i5 = i2;
        }
        int absoluteGravity = Gravity.getAbsoluteGravity(this.mGravity, getLayoutDirection()) & 7;
        if (absoluteGravity != 1) {
            if (absoluteGravity != 3 && absoluteGravity == 5) {
                i6 = (i3 + this.mColumnWidth) - measuredWidth;
            } else {
                i6 = i3;
            }
        } else {
            i6 = i3 + ((this.mColumnWidth - measuredWidth) / 2);
        }
        if (z8) {
            view.layout(i6, i5, measuredWidth + i6, measuredHeight + i5);
        } else {
            view.offsetLeftAndRight(i6 - view.getLeft());
            view.offsetTopAndBottom(i5 - view.getTop());
        }
        if (this.mCachingStarted && !view.isDrawingCacheEnabled()) {
            view.setDrawingCacheEnabled(true);
        }
        Trace.traceEnd(8L);
    }

    @Override
    public void setSelection(int i) {
        if (!isInTouchMode()) {
            setNextSelectedPositionInt(i);
        } else {
            this.mResurrectToPosition = i;
        }
        this.mLayoutMode = 2;
        if (this.mPositionScroller != null) {
            this.mPositionScroller.stop();
        }
        requestLayout();
    }

    @Override
    void setSelectionInt(int i) throws Throwable {
        int i2 = this.mNextSelectedPosition;
        if (this.mPositionScroller != null) {
            this.mPositionScroller.stop();
        }
        setNextSelectedPositionInt(i);
        layoutChildren();
        int i3 = this.mStackFromBottom ? (this.mItemCount - 1) - this.mNextSelectedPosition : this.mNextSelectedPosition;
        if (this.mStackFromBottom) {
            i2 = (this.mItemCount - 1) - i2;
        }
        if (i3 / this.mNumColumns != i2 / this.mNumColumns) {
            awakenScrollBars();
        }
    }

    @Override
    public boolean onKeyDown(int i, KeyEvent keyEvent) {
        return commonKey(i, 1, keyEvent);
    }

    @Override
    public boolean onKeyMultiple(int i, int i2, KeyEvent keyEvent) {
        return commonKey(i, i2, keyEvent);
    }

    @Override
    public boolean onKeyUp(int i, KeyEvent keyEvent) {
        return commonKey(i, 1, keyEvent);
    }

    private boolean commonKey(int i, int i2, KeyEvent keyEvent) throws Throwable {
        boolean zResurrectSelectionIfNeeded;
        if (this.mAdapter == null) {
            return false;
        }
        if (this.mDataChanged) {
            layoutChildren();
        }
        int action = keyEvent.getAction();
        if (KeyEvent.isConfirmKey(i) && keyEvent.hasNoModifiers() && action != 1) {
            zResurrectSelectionIfNeeded = resurrectSelectionIfNeeded();
            if (!zResurrectSelectionIfNeeded && keyEvent.getRepeatCount() == 0 && getChildCount() > 0) {
                keyPressed();
                zResurrectSelectionIfNeeded = true;
            }
        } else {
            zResurrectSelectionIfNeeded = false;
        }
        if (!zResurrectSelectionIfNeeded && action != 1) {
            switch (i) {
                case 19:
                    if (!keyEvent.hasNoModifiers()) {
                        if (keyEvent.hasModifiers(2)) {
                            if (resurrectSelectionIfNeeded() || fullScroll(33)) {
                            }
                        }
                    } else {
                        zResurrectSelectionIfNeeded = resurrectSelectionIfNeeded() || arrowScroll(33);
                        break;
                    }
                    break;
                case 20:
                    if (keyEvent.hasNoModifiers()) {
                        if (resurrectSelectionIfNeeded() || arrowScroll(130)) {
                        }
                    } else if (keyEvent.hasModifiers(2)) {
                        if (resurrectSelectionIfNeeded() || fullScroll(130)) {
                        }
                    }
                    break;
                case 21:
                    if (keyEvent.hasNoModifiers()) {
                        if (resurrectSelectionIfNeeded() || arrowScroll(17)) {
                        }
                    }
                    break;
                case 22:
                    if (keyEvent.hasNoModifiers()) {
                        if (resurrectSelectionIfNeeded() || arrowScroll(66)) {
                        }
                    }
                    break;
                case 61:
                    if (keyEvent.hasNoModifiers()) {
                        if (resurrectSelectionIfNeeded() || sequenceScroll(2)) {
                        }
                    } else if (keyEvent.hasModifiers(1)) {
                        if (resurrectSelectionIfNeeded() || sequenceScroll(1)) {
                        }
                    }
                    break;
                case 92:
                    if (keyEvent.hasNoModifiers()) {
                        if (resurrectSelectionIfNeeded() || pageScroll(33)) {
                        }
                    } else if (keyEvent.hasModifiers(2)) {
                        if (resurrectSelectionIfNeeded() || fullScroll(33)) {
                        }
                    }
                    break;
                case 93:
                    if (keyEvent.hasNoModifiers()) {
                        if (resurrectSelectionIfNeeded() || pageScroll(130)) {
                        }
                    } else if (keyEvent.hasModifiers(2)) {
                        if (resurrectSelectionIfNeeded() || fullScroll(130)) {
                        }
                    }
                    break;
                case 122:
                    if (keyEvent.hasNoModifiers()) {
                        if (resurrectSelectionIfNeeded() || fullScroll(33)) {
                        }
                    }
                    break;
                case 123:
                    if (keyEvent.hasNoModifiers()) {
                        if (resurrectSelectionIfNeeded() || fullScroll(130)) {
                        }
                    }
                    break;
            }
        }
        if (zResurrectSelectionIfNeeded || sendToTextFilter(i, i2, keyEvent)) {
            return true;
        }
        switch (action) {
            case 0:
                return super.onKeyDown(i, keyEvent);
            case 1:
                return super.onKeyUp(i, keyEvent);
            case 2:
                return super.onKeyMultiple(i, i2, keyEvent);
            default:
                return false;
        }
    }

    boolean pageScroll(int i) throws Throwable {
        int iMin;
        if (i == 33) {
            iMin = Math.max(0, this.mSelectedPosition - getChildCount());
        } else if (i == 130) {
            iMin = Math.min(this.mItemCount - 1, this.mSelectedPosition + getChildCount());
        } else {
            iMin = -1;
        }
        if (iMin < 0) {
            return false;
        }
        setSelectionInt(iMin);
        invokeOnItemScrollListener();
        awakenScrollBars();
        return true;
    }

    boolean fullScroll(int i) throws Throwable {
        boolean z = true;
        if (i == 33) {
            this.mLayoutMode = 2;
            setSelectionInt(0);
            invokeOnItemScrollListener();
        } else if (i == 130) {
            this.mLayoutMode = 2;
            setSelectionInt(this.mItemCount - 1);
            invokeOnItemScrollListener();
        } else {
            z = false;
        }
        if (z) {
            awakenScrollBars();
        }
        return z;
    }

    boolean arrowScroll(int i) throws Throwable {
        int iMin;
        int iMax;
        boolean z;
        int i2 = this.mSelectedPosition;
        int i3 = this.mNumColumns;
        boolean z2 = true;
        if (!this.mStackFromBottom) {
            iMax = (i2 / i3) * i3;
            iMin = Math.min((iMax + i3) - 1, this.mItemCount - 1);
        } else {
            iMin = (this.mItemCount - 1) - ((((this.mItemCount - 1) - i2) / i3) * i3);
            iMax = Math.max(0, (iMin - i3) + 1);
        }
        if (i == 33) {
            if (iMax > 0) {
                this.mLayoutMode = 6;
                setSelectionInt(Math.max(0, i2 - i3));
                z = true;
            }
            z = false;
        } else {
            if (i == 130 && iMin < this.mItemCount - 1) {
                this.mLayoutMode = 6;
                setSelectionInt(Math.min(i3 + i2, this.mItemCount - 1));
                z = true;
            }
            z = false;
        }
        boolean zIsLayoutRtl = isLayoutRtl();
        if (i2 > iMax && ((i == 17 && !zIsLayoutRtl) || (i == 66 && zIsLayoutRtl))) {
            this.mLayoutMode = 6;
            setSelectionInt(Math.max(0, i2 - 1));
        } else if (i2 < iMin && ((i == 17 && zIsLayoutRtl) || (i == 66 && !zIsLayoutRtl))) {
            this.mLayoutMode = 6;
            setSelectionInt(Math.min(i2 + 1, this.mItemCount - 1));
        } else {
            z2 = z;
        }
        if (z2) {
            playSoundEffect(SoundEffectConstants.getContantForFocusDirection(i));
            invokeOnItemScrollListener();
        }
        if (z2) {
            awakenScrollBars();
        }
        return z2;
    }

    boolean sequenceScroll(int i) throws Throwable {
        int iMin;
        int iMax;
        int i2 = this.mSelectedPosition;
        int i3 = this.mNumColumns;
        int i4 = this.mItemCount;
        boolean z = false;
        boolean z2 = true;
        if (!this.mStackFromBottom) {
            int i5 = (i2 / i3) * i3;
            iMin = Math.min((i3 + i5) - 1, i4 - 1);
            iMax = i5;
        } else {
            int i6 = i4 - 1;
            iMin = i6 - (((i6 - i2) / i3) * i3);
            iMax = Math.max(0, (iMin - i3) + 1);
        }
        switch (i) {
            case 1:
                if (i2 > 0) {
                    this.mLayoutMode = 6;
                    setSelectionInt(i2 - 1);
                    if (i2 == iMax) {
                        z = true;
                    }
                } else {
                    z2 = false;
                }
                break;
            case 2:
                if (i2 < i4 - 1) {
                    this.mLayoutMode = 6;
                    setSelectionInt(i2 + 1);
                    if (i2 == iMin) {
                    }
                } else {
                    z2 = false;
                    break;
                }
                break;
            default:
                z2 = false;
                break;
        }
        if (z2) {
            playSoundEffect(SoundEffectConstants.getContantForFocusDirection(i));
            invokeOnItemScrollListener();
        }
        if (z) {
            awakenScrollBars();
        }
        return z2;
    }

    @Override
    protected void onFocusChanged(boolean z, int i, Rect rect) {
        super.onFocusChanged(z, i, rect);
        int i2 = -1;
        if (z && rect != null) {
            rect.offset(this.mScrollX, this.mScrollY);
            Rect rect2 = this.mTempRect;
            int i3 = Integer.MAX_VALUE;
            int childCount = getChildCount();
            for (int i4 = 0; i4 < childCount; i4++) {
                if (isCandidateSelection(i4, i)) {
                    View childAt = getChildAt(i4);
                    childAt.getDrawingRect(rect2);
                    offsetDescendantRectToMyCoords(childAt, rect2);
                    int distance = getDistance(rect, rect2, i);
                    if (distance < i3) {
                        i2 = i4;
                        i3 = distance;
                    }
                }
            }
        }
        if (i2 >= 0) {
            setSelection(i2 + this.mFirstPosition);
        } else {
            requestLayout();
        }
    }

    private boolean isCandidateSelection(int i, int i2) {
        int iMin;
        int iMax;
        int childCount = getChildCount();
        int i3 = childCount - 1;
        int i4 = i3 - i;
        if (!this.mStackFromBottom) {
            iMax = i - (i % this.mNumColumns);
            iMin = Math.min((this.mNumColumns + iMax) - 1, childCount);
        } else {
            iMin = i3 - (i4 - (i4 % this.mNumColumns));
            iMax = Math.max(0, (iMin - this.mNumColumns) + 1);
        }
        if (i2 == 17) {
            return i == iMin;
        }
        if (i2 == 33) {
            return iMin == i3;
        }
        if (i2 == 66) {
            return i == iMax;
        }
        if (i2 == 130) {
            return iMax == 0;
        }
        switch (i2) {
            case 1:
                return i == iMin && iMin == i3;
            case 2:
                return i == iMax && iMax == 0;
            default:
                throw new IllegalArgumentException("direction must be one of {FOCUS_UP, FOCUS_DOWN, FOCUS_LEFT, FOCUS_RIGHT, FOCUS_FORWARD, FOCUS_BACKWARD}.");
        }
    }

    public void setGravity(int i) {
        if (this.mGravity != i) {
            this.mGravity = i;
            requestLayoutIfNecessary();
        }
    }

    public int getGravity() {
        return this.mGravity;
    }

    public void setHorizontalSpacing(int i) {
        if (i != this.mRequestedHorizontalSpacing) {
            this.mRequestedHorizontalSpacing = i;
            requestLayoutIfNecessary();
        }
    }

    public int getHorizontalSpacing() {
        return this.mHorizontalSpacing;
    }

    public int getRequestedHorizontalSpacing() {
        return this.mRequestedHorizontalSpacing;
    }

    public void setVerticalSpacing(int i) {
        if (i != this.mVerticalSpacing) {
            this.mVerticalSpacing = i;
            requestLayoutIfNecessary();
        }
    }

    public int getVerticalSpacing() {
        return this.mVerticalSpacing;
    }

    public void setStretchMode(int i) {
        if (i != this.mStretchMode) {
            this.mStretchMode = i;
            requestLayoutIfNecessary();
        }
    }

    public int getStretchMode() {
        return this.mStretchMode;
    }

    public void setColumnWidth(int i) {
        if (i != this.mRequestedColumnWidth) {
            this.mRequestedColumnWidth = i;
            requestLayoutIfNecessary();
        }
    }

    public int getColumnWidth() {
        return this.mColumnWidth;
    }

    public int getRequestedColumnWidth() {
        return this.mRequestedColumnWidth;
    }

    public void setNumColumns(int i) {
        if (i != this.mRequestedNumColumns) {
            this.mRequestedNumColumns = i;
            requestLayoutIfNecessary();
        }
    }

    @ViewDebug.ExportedProperty
    public int getNumColumns() {
        return this.mNumColumns;
    }

    private void adjustViewsUpOrDown() {
        int childCount = getChildCount();
        if (childCount > 0) {
            int i = 0;
            if (!this.mStackFromBottom) {
                int top = getChildAt(0).getTop() - this.mListPadding.top;
                if (this.mFirstPosition != 0) {
                    top -= this.mVerticalSpacing;
                }
                if (top >= 0) {
                    i = top;
                }
            } else {
                int bottom = getChildAt(childCount - 1).getBottom() - (getHeight() - this.mListPadding.bottom);
                if (this.mFirstPosition + childCount < this.mItemCount) {
                    bottom += this.mVerticalSpacing;
                }
                if (bottom <= 0) {
                    i = bottom;
                }
            }
            if (i != 0) {
                offsetChildrenTopAndBottom(-i);
            }
        }
    }

    @Override
    protected int computeVerticalScrollExtent() {
        int childCount = getChildCount();
        if (childCount <= 0) {
            return 0;
        }
        int i = (((childCount + r2) - 1) / this.mNumColumns) * 100;
        View childAt = getChildAt(0);
        int top = childAt.getTop();
        int height = childAt.getHeight();
        if (height > 0) {
            i += (top * 100) / height;
        }
        View childAt2 = getChildAt(childCount - 1);
        int bottom = childAt2.getBottom();
        int height2 = childAt2.getHeight();
        if (height2 > 0) {
            return i - (((bottom - getHeight()) * 100) / height2);
        }
        return i;
    }

    @Override
    protected int computeVerticalScrollOffset() {
        if (this.mFirstPosition >= 0 && getChildCount() > 0) {
            View childAt = getChildAt(0);
            int top = childAt.getTop();
            int height = childAt.getHeight();
            if (height > 0) {
                int i = this.mNumColumns;
                int i2 = ((this.mItemCount + i) - 1) / i;
                return Math.max(((((this.mFirstPosition + (isStackFromBottom() ? (i2 * i) - this.mItemCount : 0)) / i) * 100) - ((top * 100) / height)) + ((int) ((this.mScrollY / getHeight()) * i2 * 100.0f)), 0);
            }
        }
        return 0;
    }

    @Override
    protected int computeVerticalScrollRange() {
        int i = ((this.mItemCount + r0) - 1) / this.mNumColumns;
        int iMax = Math.max(i * 100, 0);
        if (this.mScrollY != 0) {
            return iMax + Math.abs((int) ((this.mScrollY / getHeight()) * i * 100.0f));
        }
        return iMax;
    }

    @Override
    public CharSequence getAccessibilityClassName() {
        return GridView.class.getName();
    }

    @Override
    public void onInitializeAccessibilityNodeInfoInternal(AccessibilityNodeInfo accessibilityNodeInfo) {
        super.onInitializeAccessibilityNodeInfoInternal(accessibilityNodeInfo);
        int numColumns = getNumColumns();
        int count = getCount() / numColumns;
        accessibilityNodeInfo.setCollectionInfo(AccessibilityNodeInfo.CollectionInfo.obtain(count, numColumns, false, getSelectionModeForAccessibility()));
        if (numColumns > 0 || count > 0) {
            accessibilityNodeInfo.addAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_SCROLL_TO_POSITION);
        }
    }

    @Override
    public boolean performAccessibilityActionInternal(int i, Bundle bundle) {
        if (super.performAccessibilityActionInternal(i, bundle)) {
            return true;
        }
        if (i == 16908343) {
            int numColumns = getNumColumns();
            int i2 = bundle.getInt(AccessibilityNodeInfo.ACTION_ARGUMENT_ROW_INT, -1);
            int iMin = Math.min(numColumns * i2, getCount() - 1);
            if (i2 >= 0) {
                smoothScrollToPosition(iMin);
                return true;
            }
            return false;
        }
        return false;
    }

    @Override
    public void onInitializeAccessibilityNodeInfoForItem(View view, int i, AccessibilityNodeInfo accessibilityNodeInfo) {
        int i2;
        int i3;
        super.onInitializeAccessibilityNodeInfoForItem(view, i, accessibilityNodeInfo);
        int count = getCount();
        int numColumns = getNumColumns();
        int i4 = count / numColumns;
        if (!this.mStackFromBottom) {
            i3 = i % numColumns;
            i2 = i / numColumns;
        } else {
            int i5 = (count - 1) - i;
            i2 = (i4 - 1) - (i5 / numColumns);
            i3 = (numColumns - 1) - (i5 % numColumns);
        }
        AbsListView.LayoutParams layoutParams = (AbsListView.LayoutParams) view.getLayoutParams();
        accessibilityNodeInfo.setCollectionItemInfo(AccessibilityNodeInfo.CollectionItemInfo.obtain(i2, 1, i3, 1, layoutParams != null && layoutParams.viewType == -2, isItemChecked(i)));
    }

    @Override
    protected void encodeProperties(ViewHierarchyEncoder viewHierarchyEncoder) {
        super.encodeProperties(viewHierarchyEncoder);
        viewHierarchyEncoder.addProperty("numColumns", getNumColumns());
    }
}
