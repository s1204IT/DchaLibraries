package android.widget;

import android.app.slice.Slice;
import android.content.Context;
import android.database.DataSetObserver;
import android.os.Parcelable;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.util.SparseArray;
import android.view.ContextMenu;
import android.view.RemotableViewMethod;
import android.view.View;
import android.view.ViewDebug;
import android.view.ViewGroup;
import android.view.ViewHierarchyEncoder;
import android.view.ViewStructure;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityManager;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.autofill.AutofillManager;
import android.widget.Adapter;

public abstract class AdapterView<T extends Adapter> extends ViewGroup {
    public static final int INVALID_POSITION = -1;
    public static final long INVALID_ROW_ID = Long.MIN_VALUE;
    public static final int ITEM_VIEW_TYPE_HEADER_OR_FOOTER = -2;
    public static final int ITEM_VIEW_TYPE_IGNORE = -1;
    static final int SYNC_FIRST_POSITION = 1;
    static final int SYNC_MAX_DURATION_MILLIS = 100;
    static final int SYNC_SELECTED_POSITION = 0;
    boolean mBlockLayoutRequests;
    boolean mDataChanged;
    private boolean mDesiredFocusableInTouchModeState;
    private int mDesiredFocusableState;
    private View mEmptyView;

    @ViewDebug.ExportedProperty(category = "scrolling")
    int mFirstPosition;
    boolean mInLayout;

    @ViewDebug.ExportedProperty(category = Slice.HINT_LIST)
    int mItemCount;
    private int mLayoutHeight;
    boolean mNeedSync;

    @ViewDebug.ExportedProperty(category = Slice.HINT_LIST)
    int mNextSelectedPosition;
    long mNextSelectedRowId;
    int mOldItemCount;
    int mOldSelectedPosition;
    long mOldSelectedRowId;
    OnItemClickListener mOnItemClickListener;
    OnItemLongClickListener mOnItemLongClickListener;
    OnItemSelectedListener mOnItemSelectedListener;
    private AdapterView<T>.SelectionNotifier mPendingSelectionNotifier;

    @ViewDebug.ExportedProperty(category = Slice.HINT_LIST)
    int mSelectedPosition;
    long mSelectedRowId;
    private AdapterView<T>.SelectionNotifier mSelectionNotifier;
    int mSpecificTop;
    long mSyncHeight;
    int mSyncMode;
    int mSyncPosition;
    long mSyncRowId;

    public interface OnItemClickListener {
        void onItemClick(AdapterView<?> adapterView, View view, int i, long j);
    }

    public interface OnItemLongClickListener {
        boolean onItemLongClick(AdapterView<?> adapterView, View view, int i, long j);
    }

    public interface OnItemSelectedListener {
        void onItemSelected(AdapterView<?> adapterView, View view, int i, long j);

        void onNothingSelected(AdapterView<?> adapterView);
    }

    public abstract T getAdapter();

    public abstract View getSelectedView();

    public abstract void setAdapter(T t);

    public abstract void setSelection(int i);

    public AdapterView(Context context) {
        this(context, null);
    }

    public AdapterView(Context context, AttributeSet attributeSet) {
        this(context, attributeSet, 0);
    }

    public AdapterView(Context context, AttributeSet attributeSet, int i) {
        this(context, attributeSet, i, 0);
    }

    public AdapterView(Context context, AttributeSet attributeSet, int i, int i2) {
        super(context, attributeSet, i, i2);
        this.mFirstPosition = 0;
        this.mSyncRowId = Long.MIN_VALUE;
        this.mNeedSync = false;
        this.mInLayout = false;
        this.mNextSelectedPosition = -1;
        this.mNextSelectedRowId = Long.MIN_VALUE;
        this.mSelectedPosition = -1;
        this.mSelectedRowId = Long.MIN_VALUE;
        this.mOldSelectedPosition = -1;
        this.mOldSelectedRowId = Long.MIN_VALUE;
        this.mDesiredFocusableState = 16;
        this.mBlockLayoutRequests = false;
        if (getImportantForAccessibility() == 0) {
            setImportantForAccessibility(1);
        }
        this.mDesiredFocusableState = getFocusable();
        if (this.mDesiredFocusableState == 16) {
            super.setFocusable(0);
        }
    }

    public void setOnItemClickListener(OnItemClickListener onItemClickListener) {
        this.mOnItemClickListener = onItemClickListener;
    }

    public final OnItemClickListener getOnItemClickListener() {
        return this.mOnItemClickListener;
    }

    public boolean performItemClick(View view, int i, long j) {
        boolean z = false;
        if (this.mOnItemClickListener != null) {
            playSoundEffect(0);
            this.mOnItemClickListener.onItemClick(this, view, i, j);
            z = true;
        }
        if (view != null) {
            view.sendAccessibilityEvent(1);
        }
        return z;
    }

    public void setOnItemLongClickListener(OnItemLongClickListener onItemLongClickListener) {
        if (!isLongClickable()) {
            setLongClickable(true);
        }
        this.mOnItemLongClickListener = onItemLongClickListener;
    }

    public final OnItemLongClickListener getOnItemLongClickListener() {
        return this.mOnItemLongClickListener;
    }

    public void setOnItemSelectedListener(OnItemSelectedListener onItemSelectedListener) {
        this.mOnItemSelectedListener = onItemSelectedListener;
    }

    public final OnItemSelectedListener getOnItemSelectedListener() {
        return this.mOnItemSelectedListener;
    }

    public static class AdapterContextMenuInfo implements ContextMenu.ContextMenuInfo {
        public long id;
        public int position;
        public View targetView;

        public AdapterContextMenuInfo(View view, int i, long j) {
            this.targetView = view;
            this.position = i;
            this.id = j;
        }
    }

    @Override
    public void addView(View view) {
        throw new UnsupportedOperationException("addView(View) is not supported in AdapterView");
    }

    @Override
    public void addView(View view, int i) {
        throw new UnsupportedOperationException("addView(View, int) is not supported in AdapterView");
    }

    @Override
    public void addView(View view, ViewGroup.LayoutParams layoutParams) {
        throw new UnsupportedOperationException("addView(View, LayoutParams) is not supported in AdapterView");
    }

    @Override
    public void addView(View view, int i, ViewGroup.LayoutParams layoutParams) {
        throw new UnsupportedOperationException("addView(View, int, LayoutParams) is not supported in AdapterView");
    }

    @Override
    public void removeView(View view) {
        throw new UnsupportedOperationException("removeView(View) is not supported in AdapterView");
    }

    @Override
    public void removeViewAt(int i) {
        throw new UnsupportedOperationException("removeViewAt(int) is not supported in AdapterView");
    }

    @Override
    public void removeAllViews() {
        throw new UnsupportedOperationException("removeAllViews() is not supported in AdapterView");
    }

    @Override
    protected void onLayout(boolean z, int i, int i2, int i3, int i4) {
        this.mLayoutHeight = getHeight();
    }

    @ViewDebug.CapturedViewProperty
    public int getSelectedItemPosition() {
        return this.mNextSelectedPosition;
    }

    @ViewDebug.CapturedViewProperty
    public long getSelectedItemId() {
        return this.mNextSelectedRowId;
    }

    public Object getSelectedItem() {
        Adapter adapter = getAdapter();
        int selectedItemPosition = getSelectedItemPosition();
        if (adapter != null && adapter.getCount() > 0 && selectedItemPosition >= 0) {
            return adapter.getItem(selectedItemPosition);
        }
        return null;
    }

    @ViewDebug.CapturedViewProperty
    public int getCount() {
        return this.mItemCount;
    }

    public int getPositionForView(View view) {
        while (true) {
            try {
                View view2 = (View) view.getParent();
                if (view2 == null || view2.equals(this)) {
                    break;
                }
                view = view2;
            } catch (ClassCastException e) {
                return -1;
            }
        }
        if (view != null) {
            int childCount = getChildCount();
            for (int i = 0; i < childCount; i++) {
                if (getChildAt(i).equals(view)) {
                    return this.mFirstPosition + i;
                }
            }
        }
        return -1;
    }

    public int getFirstVisiblePosition() {
        return this.mFirstPosition;
    }

    public int getLastVisiblePosition() {
        return (this.mFirstPosition + getChildCount()) - 1;
    }

    @RemotableViewMethod
    public void setEmptyView(View view) {
        this.mEmptyView = view;
        boolean z = true;
        if (view != null && view.getImportantForAccessibility() == 0) {
            view.setImportantForAccessibility(1);
        }
        Adapter adapter = getAdapter();
        if (adapter != null && !adapter.isEmpty()) {
            z = false;
        }
        updateEmptyStatus(z);
    }

    public View getEmptyView() {
        return this.mEmptyView;
    }

    boolean isInFilterMode() {
        return false;
    }

    @Override
    public void setFocusable(int i) {
        boolean z;
        Adapter adapter = getAdapter();
        if (adapter == null || adapter.getCount() == 0) {
            z = true;
        } else {
            z = false;
        }
        this.mDesiredFocusableState = i;
        if ((i & 17) == 0) {
            this.mDesiredFocusableInTouchModeState = false;
        }
        if (z && !isInFilterMode()) {
            i = 0;
        }
        super.setFocusable(i);
    }

    @Override
    public void setFocusableInTouchMode(boolean z) {
        Adapter adapter = getAdapter();
        boolean z2 = false;
        boolean z3 = adapter == null || adapter.getCount() == 0;
        this.mDesiredFocusableInTouchModeState = z;
        if (z) {
            this.mDesiredFocusableState = 1;
        }
        if (z && (!z3 || isInFilterMode())) {
            z2 = true;
        }
        super.setFocusableInTouchMode(z2);
    }

    void checkFocus() {
        Adapter adapter = getAdapter();
        boolean z = true;
        boolean z2 = !(adapter == null || adapter.getCount() == 0) || isInFilterMode();
        super.setFocusableInTouchMode(z2 && this.mDesiredFocusableInTouchModeState);
        super.setFocusable(z2 ? this.mDesiredFocusableState : 0);
        if (this.mEmptyView != null) {
            if (adapter != null && !adapter.isEmpty()) {
                z = false;
            }
            updateEmptyStatus(z);
        }
    }

    private void updateEmptyStatus(boolean z) {
        if (isInFilterMode()) {
            z = false;
        }
        if (z) {
            if (this.mEmptyView != null) {
                this.mEmptyView.setVisibility(0);
                setVisibility(8);
            } else {
                setVisibility(0);
            }
            if (this.mDataChanged) {
                onLayout(false, this.mLeft, this.mTop, this.mRight, this.mBottom);
                return;
            }
            return;
        }
        if (this.mEmptyView != null) {
            this.mEmptyView.setVisibility(8);
        }
        setVisibility(0);
    }

    public Object getItemAtPosition(int i) {
        Adapter adapter = getAdapter();
        if (adapter == null || i < 0) {
            return null;
        }
        return adapter.getItem(i);
    }

    public long getItemIdAtPosition(int i) {
        Adapter adapter = getAdapter();
        if (adapter == null || i < 0) {
            return Long.MIN_VALUE;
        }
        return adapter.getItemId(i);
    }

    @Override
    public void setOnClickListener(View.OnClickListener onClickListener) {
        throw new RuntimeException("Don't call setOnClickListener for an AdapterView. You probably want setOnItemClickListener instead");
    }

    @Override
    protected void dispatchSaveInstanceState(SparseArray<Parcelable> sparseArray) {
        dispatchFreezeSelfOnly(sparseArray);
    }

    @Override
    protected void dispatchRestoreInstanceState(SparseArray<Parcelable> sparseArray) {
        dispatchThawSelfOnly(sparseArray);
    }

    class AdapterDataSetObserver extends DataSetObserver {
        private Parcelable mInstanceState = null;

        AdapterDataSetObserver() {
        }

        @Override
        public void onChanged() {
            AdapterView.this.mDataChanged = true;
            AdapterView.this.mOldItemCount = AdapterView.this.mItemCount;
            AdapterView.this.mItemCount = AdapterView.this.getAdapter().getCount();
            if (AdapterView.this.getAdapter().hasStableIds() && this.mInstanceState != null && AdapterView.this.mOldItemCount == 0 && AdapterView.this.mItemCount > 0) {
                AdapterView.this.onRestoreInstanceState(this.mInstanceState);
                this.mInstanceState = null;
            } else {
                AdapterView.this.rememberSyncState();
            }
            AdapterView.this.checkFocus();
            AdapterView.this.requestLayout();
        }

        @Override
        public void onInvalidated() {
            AdapterView.this.mDataChanged = true;
            if (AdapterView.this.getAdapter().hasStableIds()) {
                this.mInstanceState = AdapterView.this.onSaveInstanceState();
            }
            AdapterView.this.mOldItemCount = AdapterView.this.mItemCount;
            AdapterView.this.mItemCount = 0;
            AdapterView.this.mSelectedPosition = -1;
            AdapterView.this.mSelectedRowId = Long.MIN_VALUE;
            AdapterView.this.mNextSelectedPosition = -1;
            AdapterView.this.mNextSelectedRowId = Long.MIN_VALUE;
            AdapterView.this.mNeedSync = false;
            AdapterView.this.checkFocus();
            AdapterView.this.requestLayout();
        }

        public void clearSavedState() {
            this.mInstanceState = null;
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        removeCallbacks(this.mSelectionNotifier);
    }

    private class SelectionNotifier implements Runnable {
        private SelectionNotifier() {
        }

        @Override
        public void run() {
            AdapterView.this.mPendingSelectionNotifier = null;
            if (!AdapterView.this.mDataChanged || AdapterView.this.getViewRootImpl() == null || !AdapterView.this.getViewRootImpl().isLayoutRequested()) {
                AdapterView.this.dispatchOnItemSelected();
            } else if (AdapterView.this.getAdapter() != null) {
                AdapterView.this.mPendingSelectionNotifier = this;
            }
        }
    }

    void selectionChanged() {
        this.mPendingSelectionNotifier = null;
        if (this.mOnItemSelectedListener != null || AccessibilityManager.getInstance(this.mContext).isEnabled()) {
            if (this.mInLayout || this.mBlockLayoutRequests) {
                if (this.mSelectionNotifier == null) {
                    this.mSelectionNotifier = new SelectionNotifier();
                } else {
                    removeCallbacks(this.mSelectionNotifier);
                }
                post(this.mSelectionNotifier);
            } else {
                dispatchOnItemSelected();
            }
        }
        AutofillManager autofillManager = (AutofillManager) this.mContext.getSystemService(AutofillManager.class);
        if (autofillManager != null) {
            autofillManager.notifyValueChanged(this);
        }
    }

    private void dispatchOnItemSelected() {
        fireOnSelected();
        performAccessibilityActionsOnSelected();
    }

    private void fireOnSelected() {
        if (this.mOnItemSelectedListener == null) {
            return;
        }
        int selectedItemPosition = getSelectedItemPosition();
        if (selectedItemPosition >= 0) {
            this.mOnItemSelectedListener.onItemSelected(this, getSelectedView(), selectedItemPosition, getAdapter().getItemId(selectedItemPosition));
        } else {
            this.mOnItemSelectedListener.onNothingSelected(this);
        }
    }

    private void performAccessibilityActionsOnSelected() {
        if (AccessibilityManager.getInstance(this.mContext).isEnabled() && getSelectedItemPosition() >= 0) {
            sendAccessibilityEvent(4);
        }
    }

    @Override
    public boolean dispatchPopulateAccessibilityEventInternal(AccessibilityEvent accessibilityEvent) {
        View selectedView = getSelectedView();
        if (selectedView != null && selectedView.getVisibility() == 0 && selectedView.dispatchPopulateAccessibilityEvent(accessibilityEvent)) {
            return true;
        }
        return false;
    }

    @Override
    public boolean onRequestSendAccessibilityEventInternal(View view, AccessibilityEvent accessibilityEvent) {
        if (super.onRequestSendAccessibilityEventInternal(view, accessibilityEvent)) {
            AccessibilityEvent accessibilityEventObtain = AccessibilityEvent.obtain();
            onInitializeAccessibilityEvent(accessibilityEventObtain);
            view.dispatchPopulateAccessibilityEvent(accessibilityEventObtain);
            accessibilityEvent.appendRecord(accessibilityEventObtain);
            return true;
        }
        return false;
    }

    @Override
    public CharSequence getAccessibilityClassName() {
        return AdapterView.class.getName();
    }

    @Override
    public void onInitializeAccessibilityNodeInfoInternal(AccessibilityNodeInfo accessibilityNodeInfo) {
        super.onInitializeAccessibilityNodeInfoInternal(accessibilityNodeInfo);
        accessibilityNodeInfo.setScrollable(isScrollableForAccessibility());
        View selectedView = getSelectedView();
        if (selectedView != null) {
            accessibilityNodeInfo.setEnabled(selectedView.isEnabled());
        }
    }

    @Override
    public void onInitializeAccessibilityEventInternal(AccessibilityEvent accessibilityEvent) {
        super.onInitializeAccessibilityEventInternal(accessibilityEvent);
        accessibilityEvent.setScrollable(isScrollableForAccessibility());
        View selectedView = getSelectedView();
        if (selectedView != null) {
            accessibilityEvent.setEnabled(selectedView.isEnabled());
        }
        accessibilityEvent.setCurrentItemIndex(getSelectedItemPosition());
        accessibilityEvent.setFromIndex(getFirstVisiblePosition());
        accessibilityEvent.setToIndex(getLastVisiblePosition());
        accessibilityEvent.setItemCount(getCount());
    }

    private boolean isScrollableForAccessibility() {
        int count;
        Adapter adapter = getAdapter();
        if (adapter == null || (count = adapter.getCount()) <= 0) {
            return false;
        }
        if (getFirstVisiblePosition() <= 0 && getLastVisiblePosition() >= count - 1) {
            return false;
        }
        return true;
    }

    @Override
    protected boolean canAnimate() {
        return super.canAnimate() && this.mItemCount > 0;
    }

    void handleDataChanged() {
        boolean z;
        int i;
        int i2 = this.mItemCount;
        boolean z2 = true;
        if (i2 <= 0) {
            z2 = false;
        } else if (this.mNeedSync) {
            this.mNeedSync = false;
            int iFindSyncPosition = findSyncPosition();
            if (iFindSyncPosition < 0 || lookForSelectablePosition(iFindSyncPosition, true) != iFindSyncPosition) {
                z = false;
            } else {
                setNextSelectedPositionInt(iFindSyncPosition);
                z = true;
            }
            if (!z) {
                int selectedItemPosition = getSelectedItemPosition();
                if (selectedItemPosition >= i2) {
                    i = i2 - 1;
                } else {
                    i = selectedItemPosition;
                }
                if (i < 0) {
                    i = 0;
                }
                int iLookForSelectablePosition = lookForSelectablePosition(i, true);
                if (iLookForSelectablePosition < 0) {
                    iLookForSelectablePosition = lookForSelectablePosition(i, false);
                }
                if (iLookForSelectablePosition >= 0) {
                    setNextSelectedPositionInt(iLookForSelectablePosition);
                    checkSelectionChanged();
                }
            } else {
                z2 = z;
            }
        }
        if (!z2) {
            this.mSelectedPosition = -1;
            this.mSelectedRowId = Long.MIN_VALUE;
            this.mNextSelectedPosition = -1;
            this.mNextSelectedRowId = Long.MIN_VALUE;
            this.mNeedSync = false;
            checkSelectionChanged();
        }
        notifySubtreeAccessibilityStateChangedIfNeeded();
    }

    void checkSelectionChanged() {
        if (this.mSelectedPosition != this.mOldSelectedPosition || this.mSelectedRowId != this.mOldSelectedRowId) {
            selectionChanged();
            this.mOldSelectedPosition = this.mSelectedPosition;
            this.mOldSelectedRowId = this.mSelectedRowId;
        }
        if (this.mPendingSelectionNotifier != null) {
            this.mPendingSelectionNotifier.run();
        }
    }

    int findSyncPosition() {
        int i = this.mItemCount;
        if (i == 0) {
            return -1;
        }
        long j = this.mSyncRowId;
        int i2 = this.mSyncPosition;
        if (j == Long.MIN_VALUE) {
            return -1;
        }
        int i3 = i - 1;
        int iMin = Math.min(i3, Math.max(0, i2));
        long jUptimeMillis = SystemClock.uptimeMillis() + 100;
        Adapter adapter = getAdapter();
        if (adapter == null) {
            return -1;
        }
        int i4 = iMin;
        int i5 = i4;
        boolean z = false;
        while (SystemClock.uptimeMillis() <= jUptimeMillis) {
            if (adapter.getItemId(iMin) == j) {
                return iMin;
            }
            boolean z2 = i4 == i3;
            boolean z3 = i5 == 0;
            if (z2 && z3) {
                break;
            }
            if (z3 || (z && !z2)) {
                i4++;
                z = false;
                iMin = i4;
            } else if (z2 || (!z && !z3)) {
                i5--;
                z = true;
                iMin = i5;
            }
        }
        return -1;
    }

    int lookForSelectablePosition(int i, boolean z) {
        return i;
    }

    void setSelectedPositionInt(int i) {
        this.mSelectedPosition = i;
        this.mSelectedRowId = getItemIdAtPosition(i);
    }

    void setNextSelectedPositionInt(int i) {
        this.mNextSelectedPosition = i;
        this.mNextSelectedRowId = getItemIdAtPosition(i);
        if (this.mNeedSync && this.mSyncMode == 0 && i >= 0) {
            this.mSyncPosition = i;
            this.mSyncRowId = this.mNextSelectedRowId;
        }
    }

    void rememberSyncState() {
        if (getChildCount() > 0) {
            this.mNeedSync = true;
            this.mSyncHeight = this.mLayoutHeight;
            if (this.mSelectedPosition >= 0) {
                View childAt = getChildAt(this.mSelectedPosition - this.mFirstPosition);
                this.mSyncRowId = this.mNextSelectedRowId;
                this.mSyncPosition = this.mNextSelectedPosition;
                if (childAt != null) {
                    this.mSpecificTop = childAt.getTop();
                }
                this.mSyncMode = 0;
                return;
            }
            View childAt2 = getChildAt(0);
            Adapter adapter = getAdapter();
            if (this.mFirstPosition >= 0 && this.mFirstPosition < adapter.getCount()) {
                this.mSyncRowId = adapter.getItemId(this.mFirstPosition);
            } else {
                this.mSyncRowId = -1L;
            }
            this.mSyncPosition = this.mFirstPosition;
            if (childAt2 != null) {
                this.mSpecificTop = childAt2.getTop();
            }
            this.mSyncMode = 1;
        }
    }

    @Override
    protected void encodeProperties(ViewHierarchyEncoder viewHierarchyEncoder) {
        super.encodeProperties(viewHierarchyEncoder);
        viewHierarchyEncoder.addProperty("scrolling:firstPosition", this.mFirstPosition);
        viewHierarchyEncoder.addProperty("list:nextSelectedPosition", this.mNextSelectedPosition);
        viewHierarchyEncoder.addProperty("list:nextSelectedRowId", this.mNextSelectedRowId);
        viewHierarchyEncoder.addProperty("list:selectedPosition", this.mSelectedPosition);
        viewHierarchyEncoder.addProperty("list:itemCount", this.mItemCount);
    }

    @Override
    public void onProvideAutofillStructure(ViewStructure viewStructure, int i) {
        CharSequence[] autofillOptions;
        super.onProvideAutofillStructure(viewStructure, i);
        Adapter adapter = getAdapter();
        if (adapter != null && (autofillOptions = adapter.getAutofillOptions()) != null) {
            viewStructure.setAutofillOptions(autofillOptions);
        }
    }
}
