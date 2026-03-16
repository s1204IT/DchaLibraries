package android.widget;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.TypedArray;
import android.database.DataSetObserver;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.AbsSpinner;
import android.widget.AdapterView;
import android.widget.ListPopupWindow;
import android.widget.PopupWindow;
import com.android.internal.R;

public class Spinner extends AbsSpinner implements DialogInterface.OnClickListener {
    private static final int MAX_ITEMS_MEASURED = 15;
    public static final int MODE_DIALOG = 0;
    public static final int MODE_DROPDOWN = 1;
    private static final int MODE_THEME = -1;
    private static final String TAG = "Spinner";
    private boolean mDisableChildrenWhenDisabled;
    int mDropDownWidth;
    private ListPopupWindow.ForwardingListener mForwardingListener;
    private int mGravity;
    private SpinnerPopup mPopup;
    private DropDownAdapter mTempAdapter;
    private Rect mTempRect;

    private interface SpinnerPopup {
        void dismiss();

        Drawable getBackground();

        CharSequence getHintText();

        int getHorizontalOffset();

        int getVerticalOffset();

        boolean isShowing();

        void setAdapter(ListAdapter listAdapter);

        void setBackgroundDrawable(Drawable drawable);

        void setHorizontalOffset(int i);

        void setPromptText(CharSequence charSequence);

        void setVerticalOffset(int i);

        void show(int i, int i2);
    }

    public Spinner(Context context) {
        this(context, (AttributeSet) null);
    }

    public Spinner(Context context, int mode) {
        this(context, null, 16842881, mode);
    }

    public Spinner(Context context, AttributeSet attrs) {
        this(context, attrs, 16842881);
    }

    public Spinner(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0, -1);
    }

    public Spinner(Context context, AttributeSet attrs, int defStyleAttr, int mode) {
        this(context, attrs, defStyleAttr, 0, mode);
    }

    public Spinner(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes, int mode) {
        super(context, attrs, defStyleAttr, defStyleRes);
        this.mTempRect = new Rect();
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.Spinner, defStyleAttr, defStyleRes);
        switch (mode == -1 ? a.getInt(5, 0) : mode) {
            case 0:
                this.mPopup = new DialogPopup();
                break;
            case 1:
                final DropdownPopup popup = new DropdownPopup(context, attrs, defStyleAttr, defStyleRes);
                this.mDropDownWidth = a.getLayoutDimension(4, -2);
                popup.setBackgroundDrawable(a.getDrawable(2));
                this.mPopup = popup;
                this.mForwardingListener = new ListPopupWindow.ForwardingListener(this) {
                    @Override
                    public ListPopupWindow getPopup() {
                        return popup;
                    }

                    @Override
                    public boolean onForwardingStarted() {
                        if (!Spinner.this.mPopup.isShowing()) {
                            Spinner.this.mPopup.show(Spinner.this.getTextDirection(), Spinner.this.getTextAlignment());
                            return true;
                        }
                        return true;
                    }
                };
                break;
        }
        this.mGravity = a.getInt(0, 17);
        this.mPopup.setPromptText(a.getString(3));
        this.mDisableChildrenWhenDisabled = a.getBoolean(8, false);
        a.recycle();
        if (this.mTempAdapter != null) {
            this.mPopup.setAdapter(this.mTempAdapter);
            this.mTempAdapter = null;
        }
    }

    public void setPopupBackgroundDrawable(Drawable background) {
        if (!(this.mPopup instanceof DropdownPopup)) {
            Log.e(TAG, "setPopupBackgroundDrawable: incompatible spinner mode; ignoring...");
        } else {
            ((DropdownPopup) this.mPopup).setBackgroundDrawable(background);
        }
    }

    public void setPopupBackgroundResource(int resId) {
        setPopupBackgroundDrawable(getContext().getDrawable(resId));
    }

    public Drawable getPopupBackground() {
        return this.mPopup.getBackground();
    }

    public void setDropDownVerticalOffset(int pixels) {
        this.mPopup.setVerticalOffset(pixels);
    }

    public int getDropDownVerticalOffset() {
        return this.mPopup.getVerticalOffset();
    }

    public void setDropDownHorizontalOffset(int pixels) {
        this.mPopup.setHorizontalOffset(pixels);
    }

    public int getDropDownHorizontalOffset() {
        return this.mPopup.getHorizontalOffset();
    }

    public void setDropDownWidth(int pixels) {
        if (!(this.mPopup instanceof DropdownPopup)) {
            Log.e(TAG, "Cannot set dropdown width for MODE_DIALOG, ignoring");
        } else {
            this.mDropDownWidth = pixels;
        }
    }

    public int getDropDownWidth() {
        return this.mDropDownWidth;
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        if (this.mDisableChildrenWhenDisabled) {
            int count = getChildCount();
            for (int i = 0; i < count; i++) {
                getChildAt(i).setEnabled(enabled);
            }
        }
    }

    public void setGravity(int gravity) {
        if (this.mGravity != gravity) {
            if ((gravity & 7) == 0) {
                gravity |= Gravity.START;
            }
            this.mGravity = gravity;
            requestLayout();
        }
    }

    public int getGravity() {
        return this.mGravity;
    }

    @Override
    public void setAdapter(SpinnerAdapter adapter) {
        super.setAdapter(adapter);
        this.mRecycler.clear();
        int targetSdkVersion = this.mContext.getApplicationInfo().targetSdkVersion;
        if (targetSdkVersion >= 21 && adapter != null && adapter.getViewTypeCount() != 1) {
            throw new IllegalArgumentException("Spinner adapter view type count must be 1");
        }
        if (this.mPopup != null) {
            this.mPopup.setAdapter(new DropDownAdapter(adapter));
        } else {
            this.mTempAdapter = new DropDownAdapter(adapter);
        }
    }

    @Override
    public int getBaseline() {
        int childBaseline;
        View child = null;
        if (getChildCount() > 0) {
            child = getChildAt(0);
        } else if (this.mAdapter != null && this.mAdapter.getCount() > 0) {
            child = makeView(0, false);
            this.mRecycler.put(0, child);
        }
        if (child == null || (childBaseline = child.getBaseline()) < 0) {
            return -1;
        }
        return child.getTop() + childBaseline;
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (this.mPopup != null && this.mPopup.isShowing()) {
            this.mPopup.dismiss();
        }
    }

    @Override
    public void setOnItemClickListener(AdapterView.OnItemClickListener l) {
        throw new RuntimeException("setOnItemClickListener cannot be used with a spinner.");
    }

    public void setOnItemClickListenerInt(AdapterView.OnItemClickListener l) {
        super.setOnItemClickListener(l);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (this.mForwardingListener == null || !this.mForwardingListener.onTouch(this, event)) {
            return super.onTouchEvent(event);
        }
        return true;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        if (this.mPopup != null && View.MeasureSpec.getMode(widthMeasureSpec) == Integer.MIN_VALUE) {
            int measuredWidth = getMeasuredWidth();
            setMeasuredDimension(Math.min(Math.max(measuredWidth, measureContentWidth(getAdapter(), getBackground())), View.MeasureSpec.getSize(widthMeasureSpec)), getMeasuredHeight());
        }
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);
        this.mInLayout = true;
        layout(0, false);
        this.mInLayout = false;
    }

    @Override
    void layout(int delta, boolean animate) {
        int childrenLeft = this.mSpinnerPadding.left;
        int childrenWidth = ((this.mRight - this.mLeft) - this.mSpinnerPadding.left) - this.mSpinnerPadding.right;
        if (this.mDataChanged) {
            handleDataChanged();
        }
        if (this.mItemCount == 0) {
            resetList();
            return;
        }
        if (this.mNextSelectedPosition >= 0) {
            setSelectedPositionInt(this.mNextSelectedPosition);
        }
        recycleAllViews();
        removeAllViewsInLayout();
        this.mFirstPosition = this.mSelectedPosition;
        if (this.mAdapter != null) {
            View sel = makeView(this.mSelectedPosition, true);
            int width = sel.getMeasuredWidth();
            int selectedOffset = childrenLeft;
            int layoutDirection = getLayoutDirection();
            int absoluteGravity = Gravity.getAbsoluteGravity(this.mGravity, layoutDirection);
            switch (absoluteGravity & 7) {
                case 1:
                    selectedOffset = ((childrenWidth / 2) + childrenLeft) - (width / 2);
                    break;
                case 5:
                    selectedOffset = (childrenLeft + childrenWidth) - width;
                    break;
            }
            sel.offsetLeftAndRight(selectedOffset);
        }
        this.mRecycler.clear();
        invalidate();
        checkSelectionChanged();
        this.mDataChanged = false;
        this.mNeedSync = false;
        setNextSelectedPositionInt(this.mSelectedPosition);
    }

    private View makeView(int position, boolean addChild) {
        View child;
        if (!this.mDataChanged && (child = this.mRecycler.get(position)) != null) {
            setUpChild(child, addChild);
            return child;
        }
        View child2 = this.mAdapter.getView(position, null, this);
        setUpChild(child2, addChild);
        return child2;
    }

    private void setUpChild(View child, boolean addChild) {
        ViewGroup.LayoutParams lp = child.getLayoutParams();
        if (lp == null) {
            lp = generateDefaultLayoutParams();
        }
        if (addChild) {
            addViewInLayout(child, 0, lp);
        }
        child.setSelected(hasFocus());
        if (this.mDisableChildrenWhenDisabled) {
            child.setEnabled(isEnabled());
        }
        int childHeightSpec = ViewGroup.getChildMeasureSpec(this.mHeightMeasureSpec, this.mSpinnerPadding.top + this.mSpinnerPadding.bottom, lp.height);
        int childWidthSpec = ViewGroup.getChildMeasureSpec(this.mWidthMeasureSpec, this.mSpinnerPadding.left + this.mSpinnerPadding.right, lp.width);
        child.measure(childWidthSpec, childHeightSpec);
        int childTop = this.mSpinnerPadding.top + ((((getMeasuredHeight() - this.mSpinnerPadding.bottom) - this.mSpinnerPadding.top) - child.getMeasuredHeight()) / 2);
        int childBottom = childTop + child.getMeasuredHeight();
        int width = child.getMeasuredWidth();
        int childRight = 0 + width;
        child.layout(0, childTop, childRight, childBottom);
    }

    @Override
    public boolean performClick() {
        boolean handled = super.performClick();
        if (!handled) {
            handled = true;
            if (!this.mPopup.isShowing()) {
                this.mPopup.show(getTextDirection(), getTextAlignment());
            }
        }
        return handled;
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        setSelection(which);
        dialog.dismiss();
    }

    @Override
    public void onInitializeAccessibilityEvent(AccessibilityEvent event) {
        super.onInitializeAccessibilityEvent(event);
        event.setClassName(Spinner.class.getName());
    }

    @Override
    public void onInitializeAccessibilityNodeInfo(AccessibilityNodeInfo info) {
        super.onInitializeAccessibilityNodeInfo(info);
        info.setClassName(Spinner.class.getName());
        if (this.mAdapter != null) {
            info.setCanOpenPopup(true);
        }
    }

    public void setPrompt(CharSequence prompt) {
        this.mPopup.setPromptText(prompt);
    }

    public void setPromptId(int promptId) {
        setPrompt(getContext().getText(promptId));
    }

    public CharSequence getPrompt() {
        return this.mPopup.getHintText();
    }

    int measureContentWidth(SpinnerAdapter adapter, Drawable background) {
        if (adapter == null) {
            return 0;
        }
        int width = 0;
        View itemView = null;
        int itemType = 0;
        int widthMeasureSpec = View.MeasureSpec.makeMeasureSpec(0, 0);
        int heightMeasureSpec = View.MeasureSpec.makeMeasureSpec(0, 0);
        int start = Math.max(0, getSelectedItemPosition());
        int end = Math.min(adapter.getCount(), start + 15);
        int count = end - start;
        for (int i = Math.max(0, start - (15 - count)); i < end; i++) {
            int positionType = adapter.getItemViewType(i);
            if (positionType != itemType) {
                itemType = positionType;
                itemView = null;
            }
            itemView = adapter.getView(i, itemView, this);
            if (itemView.getLayoutParams() == null) {
                itemView.setLayoutParams(new ViewGroup.LayoutParams(-2, -2));
            }
            itemView.measure(widthMeasureSpec, heightMeasureSpec);
            width = Math.max(width, itemView.getMeasuredWidth());
        }
        if (background != null) {
            background.getPadding(this.mTempRect);
            return width + this.mTempRect.left + this.mTempRect.right;
        }
        return width;
    }

    @Override
    public Parcelable onSaveInstanceState() {
        SavedState ss = new SavedState(super.onSaveInstanceState());
        ss.showDropdown = this.mPopup != null && this.mPopup.isShowing();
        return ss;
    }

    @Override
    public void onRestoreInstanceState(Parcelable state) {
        ViewTreeObserver vto;
        SavedState ss = (SavedState) state;
        super.onRestoreInstanceState(ss.getSuperState());
        if (ss.showDropdown && (vto = getViewTreeObserver()) != null) {
            ViewTreeObserver.OnGlobalLayoutListener listener = new ViewTreeObserver.OnGlobalLayoutListener() {
                @Override
                public void onGlobalLayout() {
                    if (!Spinner.this.mPopup.isShowing()) {
                        Spinner.this.mPopup.show(Spinner.this.getTextDirection(), Spinner.this.getTextAlignment());
                    }
                    ViewTreeObserver vto2 = Spinner.this.getViewTreeObserver();
                    if (vto2 != null) {
                        vto2.removeOnGlobalLayoutListener(this);
                    }
                }
            };
            vto.addOnGlobalLayoutListener(listener);
        }
    }

    static class SavedState extends AbsSpinner.SavedState {
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
        boolean showDropdown;

        SavedState(Parcelable superState) {
            super(superState);
        }

        private SavedState(Parcel in) {
            super(in);
            this.showDropdown = in.readByte() != 0;
        }

        @Override
        public void writeToParcel(Parcel out, int flags) {
            super.writeToParcel(out, flags);
            out.writeByte((byte) (this.showDropdown ? 1 : 0));
        }
    }

    private static class DropDownAdapter implements ListAdapter, SpinnerAdapter {
        private SpinnerAdapter mAdapter;
        private ListAdapter mListAdapter;

        public DropDownAdapter(SpinnerAdapter adapter) {
            this.mAdapter = adapter;
            if (adapter instanceof ListAdapter) {
                this.mListAdapter = (ListAdapter) adapter;
            }
        }

        @Override
        public int getCount() {
            if (this.mAdapter == null) {
                return 0;
            }
            return this.mAdapter.getCount();
        }

        @Override
        public Object getItem(int position) {
            if (this.mAdapter == null) {
                return null;
            }
            return this.mAdapter.getItem(position);
        }

        @Override
        public long getItemId(int position) {
            if (this.mAdapter == null) {
                return -1L;
            }
            return this.mAdapter.getItemId(position);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            return getDropDownView(position, convertView, parent);
        }

        @Override
        public View getDropDownView(int position, View convertView, ViewGroup parent) {
            if (this.mAdapter == null) {
                return null;
            }
            return this.mAdapter.getDropDownView(position, convertView, parent);
        }

        @Override
        public boolean hasStableIds() {
            return this.mAdapter != null && this.mAdapter.hasStableIds();
        }

        @Override
        public void registerDataSetObserver(DataSetObserver observer) {
            if (this.mAdapter != null) {
                this.mAdapter.registerDataSetObserver(observer);
            }
        }

        @Override
        public void unregisterDataSetObserver(DataSetObserver observer) {
            if (this.mAdapter != null) {
                this.mAdapter.unregisterDataSetObserver(observer);
            }
        }

        @Override
        public boolean areAllItemsEnabled() {
            ListAdapter adapter = this.mListAdapter;
            if (adapter != null) {
                return adapter.areAllItemsEnabled();
            }
            return true;
        }

        @Override
        public boolean isEnabled(int position) {
            ListAdapter adapter = this.mListAdapter;
            if (adapter != null) {
                return adapter.isEnabled(position);
            }
            return true;
        }

        @Override
        public int getItemViewType(int position) {
            return 0;
        }

        @Override
        public int getViewTypeCount() {
            return 1;
        }

        @Override
        public boolean isEmpty() {
            return getCount() == 0;
        }
    }

    private class DialogPopup implements SpinnerPopup, DialogInterface.OnClickListener {
        private ListAdapter mListAdapter;
        private AlertDialog mPopup;
        private CharSequence mPrompt;

        private DialogPopup() {
        }

        @Override
        public void dismiss() {
            if (this.mPopup != null) {
                this.mPopup.dismiss();
                this.mPopup = null;
            }
        }

        @Override
        public boolean isShowing() {
            if (this.mPopup != null) {
                return this.mPopup.isShowing();
            }
            return false;
        }

        @Override
        public void setAdapter(ListAdapter adapter) {
            this.mListAdapter = adapter;
        }

        @Override
        public void setPromptText(CharSequence hintText) {
            this.mPrompt = hintText;
        }

        @Override
        public CharSequence getHintText() {
            return this.mPrompt;
        }

        @Override
        public void show(int textDirection, int textAlignment) {
            if (this.mListAdapter != null) {
                AlertDialog.Builder builder = new AlertDialog.Builder(Spinner.this.getContext());
                if (this.mPrompt != null) {
                    builder.setTitle(this.mPrompt);
                }
                this.mPopup = builder.setSingleChoiceItems(this.mListAdapter, Spinner.this.getSelectedItemPosition(), this).create();
                ListView listView = this.mPopup.getListView();
                listView.setTextDirection(textDirection);
                listView.setTextAlignment(textAlignment);
                this.mPopup.show();
            }
        }

        @Override
        public void onClick(DialogInterface dialog, int which) {
            Spinner.this.setSelection(which);
            if (Spinner.this.mOnItemClickListener != null) {
                Spinner.this.performItemClick(null, which, this.mListAdapter.getItemId(which));
            }
            dismiss();
        }

        @Override
        public void setBackgroundDrawable(Drawable bg) {
            Log.e(Spinner.TAG, "Cannot set popup background for MODE_DIALOG, ignoring");
        }

        @Override
        public void setVerticalOffset(int px) {
            Log.e(Spinner.TAG, "Cannot set vertical offset for MODE_DIALOG, ignoring");
        }

        @Override
        public void setHorizontalOffset(int px) {
            Log.e(Spinner.TAG, "Cannot set horizontal offset for MODE_DIALOG, ignoring");
        }

        @Override
        public Drawable getBackground() {
            return null;
        }

        @Override
        public int getVerticalOffset() {
            return 0;
        }

        @Override
        public int getHorizontalOffset() {
            return 0;
        }
    }

    private class DropdownPopup extends ListPopupWindow implements SpinnerPopup {
        private ListAdapter mAdapter;
        private CharSequence mHintText;

        public DropdownPopup(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
            super(context, attrs, defStyleAttr, defStyleRes);
            setAnchorView(Spinner.this);
            setModal(true);
            setPromptPosition(0);
            setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView parent, View v, int position, long id) {
                    Spinner.this.setSelection(position);
                    if (Spinner.this.mOnItemClickListener != null) {
                        Spinner.this.performItemClick(v, position, DropdownPopup.this.mAdapter.getItemId(position));
                    }
                    DropdownPopup.this.dismiss();
                }
            });
        }

        @Override
        public void setAdapter(ListAdapter adapter) {
            super.setAdapter(adapter);
            this.mAdapter = adapter;
        }

        @Override
        public CharSequence getHintText() {
            return this.mHintText;
        }

        @Override
        public void setPromptText(CharSequence hintText) {
            this.mHintText = hintText;
        }

        void computeContentWidth() {
            int hOffset;
            Drawable background = getBackground();
            int hOffset2 = 0;
            if (background != null) {
                background.getPadding(Spinner.this.mTempRect);
                hOffset2 = Spinner.this.isLayoutRtl() ? Spinner.this.mTempRect.right : -Spinner.this.mTempRect.left;
            } else {
                Rect rect = Spinner.this.mTempRect;
                Spinner.this.mTempRect.right = 0;
                rect.left = 0;
            }
            int spinnerPaddingLeft = Spinner.this.getPaddingLeft();
            int spinnerPaddingRight = Spinner.this.getPaddingRight();
            int spinnerWidth = Spinner.this.getWidth();
            if (Spinner.this.mDropDownWidth == -2) {
                int contentWidth = Spinner.this.measureContentWidth((SpinnerAdapter) this.mAdapter, getBackground());
                int contentWidthLimit = (Spinner.this.mContext.getResources().getDisplayMetrics().widthPixels - Spinner.this.mTempRect.left) - Spinner.this.mTempRect.right;
                if (contentWidth > contentWidthLimit) {
                    contentWidth = contentWidthLimit;
                }
                setContentWidth(Math.max(contentWidth, (spinnerWidth - spinnerPaddingLeft) - spinnerPaddingRight));
            } else if (Spinner.this.mDropDownWidth == -1) {
                setContentWidth((spinnerWidth - spinnerPaddingLeft) - spinnerPaddingRight);
            } else {
                setContentWidth(Spinner.this.mDropDownWidth);
            }
            if (Spinner.this.isLayoutRtl()) {
                hOffset = hOffset2 + ((spinnerWidth - spinnerPaddingRight) - getWidth());
            } else {
                hOffset = hOffset2 + spinnerPaddingLeft;
            }
            setHorizontalOffset(hOffset);
        }

        @Override
        public void show(int textDirection, int textAlignment) {
            ViewTreeObserver vto;
            boolean wasShowing = isShowing();
            computeContentWidth();
            setInputMethodMode(2);
            super.show();
            ListView listView = getListView();
            listView.setChoiceMode(1);
            listView.setTextDirection(textDirection);
            listView.setTextAlignment(textAlignment);
            setSelection(Spinner.this.getSelectedItemPosition());
            if (!wasShowing && (vto = Spinner.this.getViewTreeObserver()) != null) {
                final ViewTreeObserver.OnGlobalLayoutListener layoutListener = new ViewTreeObserver.OnGlobalLayoutListener() {
                    @Override
                    public void onGlobalLayout() {
                        if (!Spinner.this.isVisibleToUser()) {
                            DropdownPopup.this.dismiss();
                        } else {
                            DropdownPopup.this.computeContentWidth();
                            DropdownPopup.super.show();
                        }
                    }
                };
                vto.addOnGlobalLayoutListener(layoutListener);
                setOnDismissListener(new PopupWindow.OnDismissListener() {
                    @Override
                    public void onDismiss() {
                        ViewTreeObserver vto2 = Spinner.this.getViewTreeObserver();
                        if (vto2 != null) {
                            vto2.removeOnGlobalLayoutListener(layoutListener);
                        }
                    }
                });
            }
        }
    }
}
