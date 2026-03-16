package android.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.database.DataSetObserver;
import android.graphics.Rect;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.util.SparseArray;
import android.view.View;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.AdapterView;
import com.android.internal.R;

public abstract class AbsSpinner extends AdapterView<SpinnerAdapter> {
    SpinnerAdapter mAdapter;
    private DataSetObserver mDataSetObserver;
    int mHeightMeasureSpec;
    final RecycleBin mRecycler;
    int mSelectionBottomPadding;
    int mSelectionLeftPadding;
    int mSelectionRightPadding;
    int mSelectionTopPadding;
    final Rect mSpinnerPadding;
    private Rect mTouchFrame;
    int mWidthMeasureSpec;

    abstract void layout(int i, boolean z);

    public AbsSpinner(Context context) {
        super(context);
        this.mSelectionLeftPadding = 0;
        this.mSelectionTopPadding = 0;
        this.mSelectionRightPadding = 0;
        this.mSelectionBottomPadding = 0;
        this.mSpinnerPadding = new Rect();
        this.mRecycler = new RecycleBin();
        initAbsSpinner();
    }

    public AbsSpinner(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public AbsSpinner(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public AbsSpinner(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        this.mSelectionLeftPadding = 0;
        this.mSelectionTopPadding = 0;
        this.mSelectionRightPadding = 0;
        this.mSelectionBottomPadding = 0;
        this.mSpinnerPadding = new Rect();
        this.mRecycler = new RecycleBin();
        initAbsSpinner();
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.AbsSpinner, defStyleAttr, defStyleRes);
        CharSequence[] entries = a.getTextArray(0);
        if (entries != null) {
            ArrayAdapter<CharSequence> adapter = new ArrayAdapter<>(context, 17367048, entries);
            adapter.setDropDownViewResource(17367049);
            setAdapter((SpinnerAdapter) adapter);
        }
        a.recycle();
    }

    private void initAbsSpinner() {
        setFocusable(true);
        setWillNotDraw(false);
    }

    @Override
    public void setAdapter(SpinnerAdapter adapter) {
        if (this.mAdapter != null) {
            this.mAdapter.unregisterDataSetObserver(this.mDataSetObserver);
            resetList();
        }
        this.mAdapter = adapter;
        this.mOldSelectedPosition = -1;
        this.mOldSelectedRowId = Long.MIN_VALUE;
        if (this.mAdapter != null) {
            this.mOldItemCount = this.mItemCount;
            this.mItemCount = this.mAdapter.getCount();
            checkFocus();
            this.mDataSetObserver = new AdapterView.AdapterDataSetObserver();
            this.mAdapter.registerDataSetObserver(this.mDataSetObserver);
            int position = this.mItemCount > 0 ? 0 : -1;
            setSelectedPositionInt(position);
            setNextSelectedPositionInt(position);
            if (this.mItemCount == 0) {
                checkSelectionChanged();
            }
        } else {
            checkFocus();
            resetList();
            checkSelectionChanged();
        }
        requestLayout();
    }

    void resetList() {
        this.mDataChanged = false;
        this.mNeedSync = false;
        removeAllViewsInLayout();
        this.mOldSelectedPosition = -1;
        this.mOldSelectedRowId = Long.MIN_VALUE;
        setSelectedPositionInt(-1);
        setNextSelectedPositionInt(-1);
        invalidate();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int widthMode = View.MeasureSpec.getMode(widthMeasureSpec);
        this.mSpinnerPadding.left = this.mPaddingLeft > this.mSelectionLeftPadding ? this.mPaddingLeft : this.mSelectionLeftPadding;
        this.mSpinnerPadding.top = this.mPaddingTop > this.mSelectionTopPadding ? this.mPaddingTop : this.mSelectionTopPadding;
        this.mSpinnerPadding.right = this.mPaddingRight > this.mSelectionRightPadding ? this.mPaddingRight : this.mSelectionRightPadding;
        this.mSpinnerPadding.bottom = this.mPaddingBottom > this.mSelectionBottomPadding ? this.mPaddingBottom : this.mSelectionBottomPadding;
        if (this.mDataChanged) {
            handleDataChanged();
        }
        int preferredHeight = 0;
        int preferredWidth = 0;
        boolean needsMeasuring = true;
        int selectedPosition = getSelectedItemPosition();
        if (selectedPosition >= 0 && this.mAdapter != null && selectedPosition < this.mAdapter.getCount()) {
            View view = this.mRecycler.get(selectedPosition);
            if (view == null) {
                view = this.mAdapter.getView(selectedPosition, null, this);
                if (view.getImportantForAccessibility() == 0) {
                    view.setImportantForAccessibility(1);
                }
            }
            if (view != null) {
                this.mRecycler.put(selectedPosition, view);
                if (view.getLayoutParams() == null) {
                    this.mBlockLayoutRequests = true;
                    view.setLayoutParams(generateDefaultLayoutParams());
                    this.mBlockLayoutRequests = false;
                }
                measureChild(view, widthMeasureSpec, heightMeasureSpec);
                preferredHeight = getChildHeight(view) + this.mSpinnerPadding.top + this.mSpinnerPadding.bottom;
                preferredWidth = getChildWidth(view) + this.mSpinnerPadding.left + this.mSpinnerPadding.right;
                needsMeasuring = false;
            }
        }
        if (needsMeasuring) {
            preferredHeight = this.mSpinnerPadding.top + this.mSpinnerPadding.bottom;
            if (widthMode == 0) {
                preferredWidth = this.mSpinnerPadding.left + this.mSpinnerPadding.right;
            }
        }
        int preferredHeight2 = Math.max(preferredHeight, getSuggestedMinimumHeight());
        int preferredWidth2 = Math.max(preferredWidth, getSuggestedMinimumWidth());
        int heightSize = resolveSizeAndState(preferredHeight2, heightMeasureSpec, 0);
        int widthSize = resolveSizeAndState(preferredWidth2, widthMeasureSpec, 0);
        setMeasuredDimension(widthSize, heightSize);
        this.mHeightMeasureSpec = heightMeasureSpec;
        this.mWidthMeasureSpec = widthMeasureSpec;
    }

    int getChildHeight(View child) {
        return child.getMeasuredHeight();
    }

    int getChildWidth(View child) {
        return child.getMeasuredWidth();
    }

    @Override
    protected ViewGroup.LayoutParams generateDefaultLayoutParams() {
        return new ViewGroup.LayoutParams(-1, -2);
    }

    void recycleAllViews() {
        int childCount = getChildCount();
        RecycleBin recycleBin = this.mRecycler;
        int position = this.mFirstPosition;
        for (int i = 0; i < childCount; i++) {
            View v = getChildAt(i);
            int index = position + i;
            recycleBin.put(index, v);
        }
    }

    public void setSelection(int position, boolean animate) {
        boolean shouldAnimate = animate && this.mFirstPosition <= position && position <= (this.mFirstPosition + getChildCount()) + (-1);
        setSelectionInt(position, shouldAnimate);
    }

    @Override
    public void setSelection(int position) {
        setNextSelectedPositionInt(position);
        requestLayout();
        invalidate();
    }

    void setSelectionInt(int position, boolean animate) {
        if (position != this.mOldSelectedPosition) {
            this.mBlockLayoutRequests = true;
            int delta = position - this.mSelectedPosition;
            setNextSelectedPositionInt(position);
            layout(delta, animate);
            this.mBlockLayoutRequests = false;
        }
    }

    @Override
    public View getSelectedView() {
        if (this.mItemCount <= 0 || this.mSelectedPosition < 0) {
            return null;
        }
        return getChildAt(this.mSelectedPosition - this.mFirstPosition);
    }

    @Override
    public void requestLayout() {
        if (!this.mBlockLayoutRequests) {
            super.requestLayout();
        }
    }

    @Override
    public SpinnerAdapter getAdapter() {
        return this.mAdapter;
    }

    @Override
    public int getCount() {
        return this.mItemCount;
    }

    public int pointToPosition(int x, int y) {
        Rect frame = this.mTouchFrame;
        if (frame == null) {
            this.mTouchFrame = new Rect();
            frame = this.mTouchFrame;
        }
        int count = getChildCount();
        for (int i = count - 1; i >= 0; i--) {
            View child = getChildAt(i);
            if (child.getVisibility() == 0) {
                child.getHitRect(frame);
                if (frame.contains(x, y)) {
                    return this.mFirstPosition + i;
                }
            }
        }
        return -1;
    }

    static class SavedState extends View.BaseSavedState {
        public static final Parcelable.Creator<SavedState> CREATOR = new Parcelable.Creator<SavedState>() {
            @Override
            public SavedState createFromParcel(Parcel in) {
                return new SavedState(in);
            }

            @Override
            public SavedState[] newArray(int size) {
                return new SavedState[size];
            }
        };
        int position;
        long selectedId;

        SavedState(Parcelable superState) {
            super(superState);
        }

        SavedState(Parcel in) {
            super(in);
            this.selectedId = in.readLong();
            this.position = in.readInt();
        }

        @Override
        public void writeToParcel(Parcel out, int flags) {
            super.writeToParcel(out, flags);
            out.writeLong(this.selectedId);
            out.writeInt(this.position);
        }

        public String toString() {
            return "AbsSpinner.SavedState{" + Integer.toHexString(System.identityHashCode(this)) + " selectedId=" + this.selectedId + " position=" + this.position + "}";
        }
    }

    @Override
    public Parcelable onSaveInstanceState() {
        Parcelable superState = super.onSaveInstanceState();
        SavedState ss = new SavedState(superState);
        ss.selectedId = getSelectedItemId();
        if (ss.selectedId >= 0) {
            ss.position = getSelectedItemPosition();
        } else {
            ss.position = -1;
        }
        return ss;
    }

    @Override
    public void onRestoreInstanceState(Parcelable state) {
        SavedState ss = (SavedState) state;
        super.onRestoreInstanceState(ss.getSuperState());
        if (ss.selectedId >= 0) {
            this.mDataChanged = true;
            this.mNeedSync = true;
            this.mSyncRowId = ss.selectedId;
            this.mSyncPosition = ss.position;
            this.mSyncMode = 0;
            requestLayout();
        }
    }

    class RecycleBin {
        private final SparseArray<View> mScrapHeap = new SparseArray<>();

        RecycleBin() {
        }

        public void put(int position, View v) {
            this.mScrapHeap.put(position, v);
        }

        View get(int position) {
            View result = this.mScrapHeap.get(position);
            if (result != null) {
                this.mScrapHeap.delete(position);
            }
            return result;
        }

        void clear() {
            SparseArray<View> scrapHeap = this.mScrapHeap;
            int count = scrapHeap.size();
            for (int i = 0; i < count; i++) {
                View view = scrapHeap.valueAt(i);
                if (view != null) {
                    AbsSpinner.this.removeDetachedView(view, true);
                }
            }
            scrapHeap.clear();
        }
    }

    @Override
    public void onInitializeAccessibilityEvent(AccessibilityEvent event) {
        super.onInitializeAccessibilityEvent(event);
        event.setClassName(AbsSpinner.class.getName());
    }

    @Override
    public void onInitializeAccessibilityNodeInfo(AccessibilityNodeInfo info) {
        super.onInitializeAccessibilityNodeInfo(info);
        info.setClassName(AbsSpinner.class.getName());
    }
}
