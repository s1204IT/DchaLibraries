package android.widget;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.database.DataSetObserver;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.SystemClock;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.IntProperty;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import com.android.internal.R;
import com.android.internal.widget.AutoScrollHelper;
import java.util.Locale;

public class ListPopupWindow {
    private static final boolean DEBUG = false;
    private static final int EXPAND_LIST_TIMEOUT = 250;
    public static final int INPUT_METHOD_FROM_FOCUSABLE = 0;
    public static final int INPUT_METHOD_NEEDED = 1;
    public static final int INPUT_METHOD_NOT_NEEDED = 2;
    public static final int MATCH_PARENT = -1;
    public static final int POSITION_PROMPT_ABOVE = 0;
    public static final int POSITION_PROMPT_BELOW = 1;
    private static final String TAG = "ListPopupWindow";
    public static final int WRAP_CONTENT = -2;
    private ListAdapter mAdapter;
    private Context mContext;
    private boolean mDropDownAlwaysVisible;
    private View mDropDownAnchorView;
    private int mDropDownGravity;
    private int mDropDownHeight;
    private int mDropDownHorizontalOffset;
    private DropDownListView mDropDownList;
    private Drawable mDropDownListHighlight;
    private int mDropDownVerticalOffset;
    private boolean mDropDownVerticalOffsetSet;
    private int mDropDownWidth;
    private boolean mForceIgnoreOutsideTouch;
    private Handler mHandler;
    private final ListSelectorHider mHideSelector;
    private AdapterView.OnItemClickListener mItemClickListener;
    private AdapterView.OnItemSelectedListener mItemSelectedListener;
    private int mLayoutDirection;
    int mListItemExpandMaximum;
    private boolean mModal;
    private DataSetObserver mObserver;
    private PopupWindow mPopup;
    private int mPromptPosition;
    private View mPromptView;
    private final ResizePopupRunnable mResizePopupRunnable;
    private final PopupScrollListener mScrollListener;
    private Runnable mShowDropDownRunnable;
    private Rect mTempRect;
    private final PopupTouchInterceptor mTouchInterceptor;

    public ListPopupWindow(Context context) {
        this(context, null, 16843519, 0);
    }

    public ListPopupWindow(Context context, AttributeSet attrs) {
        this(context, attrs, 16843519, 0);
    }

    public ListPopupWindow(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public ListPopupWindow(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        this.mDropDownHeight = -2;
        this.mDropDownWidth = -2;
        this.mDropDownGravity = 0;
        this.mDropDownAlwaysVisible = false;
        this.mForceIgnoreOutsideTouch = false;
        this.mListItemExpandMaximum = Integer.MAX_VALUE;
        this.mPromptPosition = 0;
        this.mResizePopupRunnable = new ResizePopupRunnable();
        this.mTouchInterceptor = new PopupTouchInterceptor();
        this.mScrollListener = new PopupScrollListener();
        this.mHideSelector = new ListSelectorHider();
        this.mHandler = new Handler();
        this.mTempRect = new Rect();
        this.mContext = context;
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.ListPopupWindow, defStyleAttr, defStyleRes);
        this.mDropDownHorizontalOffset = a.getDimensionPixelOffset(0, 0);
        this.mDropDownVerticalOffset = a.getDimensionPixelOffset(1, 0);
        if (this.mDropDownVerticalOffset != 0) {
            this.mDropDownVerticalOffsetSet = true;
        }
        a.recycle();
        this.mPopup = new PopupWindow(context, attrs, defStyleAttr, defStyleRes);
        this.mPopup.setInputMethodMode(1);
        Locale locale = this.mContext.getResources().getConfiguration().locale;
        this.mLayoutDirection = TextUtils.getLayoutDirectionFromLocale(locale);
    }

    public void setAdapter(ListAdapter adapter) {
        if (this.mObserver == null) {
            this.mObserver = new PopupDataSetObserver();
        } else if (this.mAdapter != null) {
            this.mAdapter.unregisterDataSetObserver(this.mObserver);
        }
        this.mAdapter = adapter;
        if (this.mAdapter != null) {
            adapter.registerDataSetObserver(this.mObserver);
        }
        if (this.mDropDownList != null) {
            this.mDropDownList.setAdapter(this.mAdapter);
        }
    }

    public void setPromptPosition(int position) {
        this.mPromptPosition = position;
    }

    public int getPromptPosition() {
        return this.mPromptPosition;
    }

    public void setModal(boolean modal) {
        this.mModal = modal;
        this.mPopup.setFocusable(modal);
    }

    public boolean isModal() {
        return this.mModal;
    }

    public void setForceIgnoreOutsideTouch(boolean forceIgnoreOutsideTouch) {
        this.mForceIgnoreOutsideTouch = forceIgnoreOutsideTouch;
    }

    public void setDropDownAlwaysVisible(boolean dropDownAlwaysVisible) {
        this.mDropDownAlwaysVisible = dropDownAlwaysVisible;
    }

    public boolean isDropDownAlwaysVisible() {
        return this.mDropDownAlwaysVisible;
    }

    public void setSoftInputMode(int mode) {
        this.mPopup.setSoftInputMode(mode);
    }

    public int getSoftInputMode() {
        return this.mPopup.getSoftInputMode();
    }

    public void setListSelector(Drawable selector) {
        this.mDropDownListHighlight = selector;
    }

    public Drawable getBackground() {
        return this.mPopup.getBackground();
    }

    public void setBackgroundDrawable(Drawable d) {
        this.mPopup.setBackgroundDrawable(d);
    }

    public void setAnimationStyle(int animationStyle) {
        this.mPopup.setAnimationStyle(animationStyle);
    }

    public int getAnimationStyle() {
        return this.mPopup.getAnimationStyle();
    }

    public View getAnchorView() {
        return this.mDropDownAnchorView;
    }

    public void setAnchorView(View anchor) {
        this.mDropDownAnchorView = anchor;
    }

    public int getHorizontalOffset() {
        return this.mDropDownHorizontalOffset;
    }

    public void setHorizontalOffset(int offset) {
        this.mDropDownHorizontalOffset = offset;
    }

    public int getVerticalOffset() {
        if (this.mDropDownVerticalOffsetSet) {
            return this.mDropDownVerticalOffset;
        }
        return 0;
    }

    public void setVerticalOffset(int offset) {
        this.mDropDownVerticalOffset = offset;
        this.mDropDownVerticalOffsetSet = true;
    }

    public void setDropDownGravity(int gravity) {
        this.mDropDownGravity = gravity;
    }

    public int getWidth() {
        return this.mDropDownWidth;
    }

    public void setWidth(int width) {
        this.mDropDownWidth = width;
    }

    public void setContentWidth(int width) {
        Drawable popupBackground = this.mPopup.getBackground();
        if (popupBackground != null) {
            popupBackground.getPadding(this.mTempRect);
            this.mDropDownWidth = this.mTempRect.left + this.mTempRect.right + width;
        } else {
            setWidth(width);
        }
    }

    public int getHeight() {
        return this.mDropDownHeight;
    }

    public void setHeight(int height) {
        this.mDropDownHeight = height;
    }

    public void setOnItemClickListener(AdapterView.OnItemClickListener clickListener) {
        this.mItemClickListener = clickListener;
    }

    public void setOnItemSelectedListener(AdapterView.OnItemSelectedListener selectedListener) {
        this.mItemSelectedListener = selectedListener;
    }

    public void setPromptView(View prompt) {
        boolean showing = isShowing();
        if (showing) {
            removePromptView();
        }
        this.mPromptView = prompt;
        if (showing) {
            show();
        }
    }

    public void postShow() {
        this.mHandler.post(this.mShowDropDownRunnable);
    }

    public void show() {
        int widthSpec;
        int heightSpec;
        int height = buildDropDown();
        int widthSpec2 = 0;
        int heightSpec2 = 0;
        boolean noInputMethod = isInputMethodNotNeeded();
        this.mPopup.setAllowScrollingAnchorParent(!noInputMethod);
        if (this.mPopup.isShowing()) {
            if (this.mDropDownWidth == -1) {
                widthSpec = -1;
            } else if (this.mDropDownWidth == -2) {
                widthSpec = getAnchorView().getWidth();
            } else {
                widthSpec = this.mDropDownWidth;
            }
            if (this.mDropDownHeight == -1) {
                heightSpec = noInputMethod ? height : -1;
                if (noInputMethod) {
                    this.mPopup.setWindowLayoutMode(this.mDropDownWidth == -1 ? -1 : 0, 0);
                } else {
                    this.mPopup.setWindowLayoutMode(this.mDropDownWidth == -1 ? -1 : 0, -1);
                }
            } else {
                heightSpec = this.mDropDownHeight == -2 ? height : this.mDropDownHeight;
            }
            this.mPopup.setOutsideTouchable((this.mForceIgnoreOutsideTouch || this.mDropDownAlwaysVisible) ? false : true);
            this.mPopup.update(getAnchorView(), this.mDropDownHorizontalOffset, this.mDropDownVerticalOffset, widthSpec, heightSpec);
            return;
        }
        if (this.mDropDownWidth == -1) {
            widthSpec2 = -1;
        } else if (this.mDropDownWidth == -2) {
            this.mPopup.setWidth(getAnchorView().getWidth());
        } else {
            this.mPopup.setWidth(this.mDropDownWidth);
        }
        if (this.mDropDownHeight == -1) {
            heightSpec2 = -1;
        } else if (this.mDropDownHeight == -2) {
            this.mPopup.setHeight(height);
        } else {
            this.mPopup.setHeight(this.mDropDownHeight);
        }
        this.mPopup.setWindowLayoutMode(widthSpec2, heightSpec2);
        this.mPopup.setClipToScreenEnabled(true);
        this.mPopup.setOutsideTouchable((this.mForceIgnoreOutsideTouch || this.mDropDownAlwaysVisible) ? false : true);
        this.mPopup.setTouchInterceptor(this.mTouchInterceptor);
        this.mPopup.showAsDropDown(getAnchorView(), this.mDropDownHorizontalOffset, this.mDropDownVerticalOffset, this.mDropDownGravity);
        this.mDropDownList.setSelection(-1);
        if (!this.mModal || this.mDropDownList.isInTouchMode()) {
            clearListSelection();
        }
        if (!this.mModal) {
            this.mHandler.post(this.mHideSelector);
        }
    }

    public void dismiss() {
        this.mPopup.dismiss();
        removePromptView();
        this.mPopup.setContentView(null);
        this.mDropDownList = null;
        this.mHandler.removeCallbacks(this.mResizePopupRunnable);
    }

    public void setOnDismissListener(PopupWindow.OnDismissListener listener) {
        this.mPopup.setOnDismissListener(listener);
    }

    private void removePromptView() {
        if (this.mPromptView != null) {
            ViewParent parent = this.mPromptView.getParent();
            if (parent instanceof ViewGroup) {
                ViewGroup group = (ViewGroup) parent;
                group.removeView(this.mPromptView);
            }
        }
    }

    public void setInputMethodMode(int mode) {
        this.mPopup.setInputMethodMode(mode);
    }

    public int getInputMethodMode() {
        return this.mPopup.getInputMethodMode();
    }

    public void setSelection(int position) {
        DropDownListView list = this.mDropDownList;
        if (!isShowing() || list == null) {
            return;
        }
        list.mListSelectionHidden = false;
        list.setSelection(position);
        if (list.getChoiceMode() != 0) {
            list.setItemChecked(position, true);
        }
    }

    public void clearListSelection() {
        DropDownListView list = this.mDropDownList;
        if (list == null) {
            return;
        }
        list.mListSelectionHidden = true;
        list.hideSelector();
        list.requestLayout();
    }

    public boolean isShowing() {
        return this.mPopup.isShowing();
    }

    public boolean isInputMethodNotNeeded() {
        return this.mPopup.getInputMethodMode() == 2;
    }

    public boolean performItemClick(int position) {
        if (!isShowing()) {
            return false;
        }
        if (this.mItemClickListener != null) {
            DropDownListView list = this.mDropDownList;
            View child = list.getChildAt(position - list.getFirstVisiblePosition());
            ListAdapter adapter = list.getAdapter();
            this.mItemClickListener.onItemClick(list, child, position, adapter.getItemId(position));
        }
        return true;
    }

    public Object getSelectedItem() {
        if (isShowing()) {
            return this.mDropDownList.getSelectedItem();
        }
        return null;
    }

    public int getSelectedItemPosition() {
        if (isShowing()) {
            return this.mDropDownList.getSelectedItemPosition();
        }
        return -1;
    }

    public long getSelectedItemId() {
        if (isShowing()) {
            return this.mDropDownList.getSelectedItemId();
        }
        return Long.MIN_VALUE;
    }

    public View getSelectedView() {
        if (isShowing()) {
            return this.mDropDownList.getSelectedView();
        }
        return null;
    }

    public ListView getListView() {
        return this.mDropDownList;
    }

    void setListItemExpandMax(int max) {
        this.mListItemExpandMaximum = max;
    }

    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (!isShowing() || keyCode == 62) {
            return false;
        }
        if (this.mDropDownList.getSelectedItemPosition() < 0 && KeyEvent.isConfirmKey(keyCode)) {
            return false;
        }
        int curIndex = this.mDropDownList.getSelectedItemPosition();
        boolean below = !this.mPopup.isAboveAnchor();
        ListAdapter adapter = this.mAdapter;
        int firstItem = Integer.MAX_VALUE;
        int lastItem = Integer.MIN_VALUE;
        if (adapter != null) {
            boolean allEnabled = adapter.areAllItemsEnabled();
            firstItem = allEnabled ? 0 : this.mDropDownList.lookForSelectablePosition(0, true);
            lastItem = allEnabled ? adapter.getCount() - 1 : this.mDropDownList.lookForSelectablePosition(adapter.getCount() - 1, false);
        }
        if ((below && keyCode == 19 && curIndex <= firstItem) || (!below && keyCode == 20 && curIndex >= lastItem)) {
            clearListSelection();
            this.mPopup.setInputMethodMode(1);
            show();
            return true;
        }
        this.mDropDownList.mListSelectionHidden = false;
        boolean consumed = this.mDropDownList.onKeyDown(keyCode, event);
        if (!consumed) {
            return (below && keyCode == 20) ? curIndex == lastItem : !below && keyCode == 19 && curIndex == firstItem;
        }
        this.mPopup.setInputMethodMode(2);
        this.mDropDownList.requestFocusFromTouch();
        show();
        switch (keyCode) {
            case 19:
            case 20:
            case 23:
            case 66:
                break;
        }
        return true;
    }

    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (!isShowing() || this.mDropDownList.getSelectedItemPosition() < 0) {
            return false;
        }
        boolean consumed = this.mDropDownList.onKeyUp(keyCode, event);
        if (consumed && KeyEvent.isConfirmKey(keyCode)) {
            dismiss();
            return consumed;
        }
        return consumed;
    }

    public boolean onKeyPreIme(int keyCode, KeyEvent event) {
        if (keyCode == 4 && isShowing()) {
            View anchorView = this.mDropDownAnchorView;
            if (event.getAction() == 0 && event.getRepeatCount() == 0) {
                KeyEvent.DispatcherState state = anchorView.getKeyDispatcherState();
                if (state == null) {
                    return true;
                }
                state.startTracking(event, this);
                return true;
            }
            if (event.getAction() == 1) {
                KeyEvent.DispatcherState state2 = anchorView.getKeyDispatcherState();
                if (state2 != null) {
                    state2.handleUpEvent(event);
                }
                if (event.isTracking() && !event.isCanceled()) {
                    dismiss();
                    return true;
                }
            }
        }
        return false;
    }

    public View.OnTouchListener createDragToOpenListener(View src) {
        return new ForwardingListener(src) {
            @Override
            public ListPopupWindow getPopup() {
                return ListPopupWindow.this;
            }
        };
    }

    private int buildDropDown() {
        int iMakeMeasureSpec;
        int measuredHeight = 0;
        if (this.mDropDownList == null) {
            Context context = this.mContext;
            this.mShowDropDownRunnable = new Runnable() {
                @Override
                public void run() {
                    View view = ListPopupWindow.this.getAnchorView();
                    if (view != null && view.getWindowToken() != null) {
                        ListPopupWindow.this.show();
                    }
                }
            };
            this.mDropDownList = new DropDownListView(context, !this.mModal);
            if (this.mDropDownListHighlight != null) {
                this.mDropDownList.setSelector(this.mDropDownListHighlight);
            }
            this.mDropDownList.setAdapter(this.mAdapter);
            this.mDropDownList.setOnItemClickListener(this.mItemClickListener);
            this.mDropDownList.setFocusable(true);
            this.mDropDownList.setFocusableInTouchMode(true);
            this.mDropDownList.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    DropDownListView dropDownList;
                    if (position == -1 || (dropDownList = ListPopupWindow.this.mDropDownList) == null) {
                        return;
                    }
                    dropDownList.mListSelectionHidden = false;
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {
                }
            });
            this.mDropDownList.setOnScrollListener(this.mScrollListener);
            if (this.mItemSelectedListener != null) {
                this.mDropDownList.setOnItemSelectedListener(this.mItemSelectedListener);
            }
            DropDownListView dropDownListView = this.mDropDownList;
            View view = this.mPromptView;
            View view2 = dropDownListView;
            if (view != null) {
                LinearLayout linearLayout = new LinearLayout(context);
                linearLayout.setOrientation(1);
                LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(-1, 0, 1.0f);
                switch (this.mPromptPosition) {
                    case 0:
                        linearLayout.addView(view);
                        linearLayout.addView(dropDownListView, layoutParams);
                        break;
                    case 1:
                        linearLayout.addView(dropDownListView, layoutParams);
                        linearLayout.addView(view);
                        break;
                    default:
                        Log.e(TAG, "Invalid hint position " + this.mPromptPosition);
                        break;
                }
                view.measure(View.MeasureSpec.makeMeasureSpec(this.mDropDownWidth, Integer.MIN_VALUE), 0);
                LinearLayout.LayoutParams layoutParams2 = (LinearLayout.LayoutParams) view.getLayoutParams();
                measuredHeight = view.getMeasuredHeight() + layoutParams2.topMargin + layoutParams2.bottomMargin;
                view2 = linearLayout;
            }
            this.mPopup.setContentView(view2);
        } else {
            View view3 = this.mPromptView;
            if (view3 != null) {
                LinearLayout.LayoutParams layoutParams3 = (LinearLayout.LayoutParams) view3.getLayoutParams();
                measuredHeight = view3.getMeasuredHeight() + layoutParams3.topMargin + layoutParams3.bottomMargin;
            }
        }
        int i = 0;
        Drawable background = this.mPopup.getBackground();
        if (background != null) {
            background.getPadding(this.mTempRect);
            i = this.mTempRect.top + this.mTempRect.bottom;
            if (!this.mDropDownVerticalOffsetSet) {
                this.mDropDownVerticalOffset = -this.mTempRect.top;
            }
        } else {
            this.mTempRect.setEmpty();
        }
        int maxAvailableHeight = this.mPopup.getMaxAvailableHeight(getAnchorView(), this.mDropDownVerticalOffset, this.mPopup.getInputMethodMode() == 2);
        if (this.mDropDownAlwaysVisible || this.mDropDownHeight == -1) {
            return maxAvailableHeight + i;
        }
        switch (this.mDropDownWidth) {
            case -2:
                iMakeMeasureSpec = View.MeasureSpec.makeMeasureSpec(this.mContext.getResources().getDisplayMetrics().widthPixels - (this.mTempRect.left + this.mTempRect.right), Integer.MIN_VALUE);
                break;
            case -1:
                iMakeMeasureSpec = View.MeasureSpec.makeMeasureSpec(this.mContext.getResources().getDisplayMetrics().widthPixels - (this.mTempRect.left + this.mTempRect.right), 1073741824);
                break;
            default:
                iMakeMeasureSpec = View.MeasureSpec.makeMeasureSpec(this.mDropDownWidth, 1073741824);
                break;
        }
        int iMeasureHeightOfChildren = this.mDropDownList.measureHeightOfChildren(iMakeMeasureSpec, 0, -1, maxAvailableHeight - measuredHeight, -1);
        if (iMeasureHeightOfChildren > 0) {
            measuredHeight += i;
        }
        return iMeasureHeightOfChildren + measuredHeight;
    }

    public static abstract class ForwardingListener implements View.OnTouchListener, View.OnAttachStateChangeListener {
        private int mActivePointerId;
        private Runnable mDisallowIntercept;
        private boolean mForwarding;
        private final float mScaledTouchSlop;
        private final View mSrc;
        private Runnable mTriggerLongPress;
        private boolean mWasLongPress;
        private final int mTapTimeout = ViewConfiguration.getTapTimeout();
        private final int mLongPressTimeout = (this.mTapTimeout + ViewConfiguration.getLongPressTimeout()) / 2;

        public abstract ListPopupWindow getPopup();

        public ForwardingListener(View src) {
            this.mSrc = src;
            this.mScaledTouchSlop = ViewConfiguration.get(src.getContext()).getScaledTouchSlop();
            src.addOnAttachStateChangeListener(this);
        }

        @Override
        public boolean onTouch(View v, MotionEvent event) {
            boolean forwarding;
            boolean wasForwarding = this.mForwarding;
            if (wasForwarding) {
                forwarding = onTouchForwarded(event) || !onForwardingStopped();
            } else {
                forwarding = onTouchObserved(event) && onForwardingStarted();
                if (forwarding) {
                    long now = SystemClock.uptimeMillis();
                    MotionEvent e = MotionEvent.obtain(now, now, 3, 0.0f, 0.0f, 0);
                    this.mSrc.onTouchEvent(e);
                    e.recycle();
                }
            }
            this.mForwarding = forwarding;
            return forwarding || wasForwarding;
        }

        @Override
        public void onViewAttachedToWindow(View v) {
        }

        @Override
        public void onViewDetachedFromWindow(View v) {
            this.mForwarding = false;
            this.mActivePointerId = -1;
            if (this.mDisallowIntercept != null) {
                this.mSrc.removeCallbacks(this.mDisallowIntercept);
            }
        }

        protected boolean onForwardingStarted() {
            ListPopupWindow popup = getPopup();
            if (popup != null && !popup.isShowing()) {
                popup.show();
                return true;
            }
            return true;
        }

        protected boolean onForwardingStopped() {
            ListPopupWindow popup = getPopup();
            if (popup != null && popup.isShowing()) {
                popup.dismiss();
                return true;
            }
            return true;
        }

        private boolean onTouchObserved(MotionEvent srcEvent) {
            View src = this.mSrc;
            if (!src.isEnabled()) {
                return false;
            }
            int actionMasked = srcEvent.getActionMasked();
            switch (actionMasked) {
                case 0:
                    this.mActivePointerId = srcEvent.getPointerId(0);
                    this.mWasLongPress = false;
                    if (this.mDisallowIntercept == null) {
                        this.mDisallowIntercept = new DisallowIntercept();
                    }
                    src.postDelayed(this.mDisallowIntercept, this.mTapTimeout);
                    if (this.mTriggerLongPress == null) {
                        this.mTriggerLongPress = new TriggerLongPress();
                    }
                    src.postDelayed(this.mTriggerLongPress, this.mLongPressTimeout);
                    break;
                case 1:
                case 3:
                    clearCallbacks();
                    break;
                case 2:
                    int activePointerIndex = srcEvent.findPointerIndex(this.mActivePointerId);
                    if (activePointerIndex >= 0) {
                        float x = srcEvent.getX(activePointerIndex);
                        float y = srcEvent.getY(activePointerIndex);
                        if (!src.pointInView(x, y, this.mScaledTouchSlop)) {
                            clearCallbacks();
                            src.getParent().requestDisallowInterceptTouchEvent(true);
                        }
                    }
                    break;
            }
            return false;
        }

        private void clearCallbacks() {
            if (this.mTriggerLongPress != null) {
                this.mSrc.removeCallbacks(this.mTriggerLongPress);
            }
            if (this.mDisallowIntercept != null) {
                this.mSrc.removeCallbacks(this.mDisallowIntercept);
            }
        }

        private void onLongPress() {
            clearCallbacks();
            View src = this.mSrc;
            if (src.isEnabled() && !src.isLongClickable() && onForwardingStarted()) {
                src.getParent().requestDisallowInterceptTouchEvent(true);
                long now = SystemClock.uptimeMillis();
                MotionEvent e = MotionEvent.obtain(now, now, 3, 0.0f, 0.0f, 0);
                src.onTouchEvent(e);
                e.recycle();
                this.mForwarding = true;
                this.mWasLongPress = true;
            }
        }

        private boolean onTouchForwarded(MotionEvent srcEvent) {
            DropDownListView dst;
            View src = this.mSrc;
            ListPopupWindow popup = getPopup();
            if (popup == null || !popup.isShowing() || (dst = popup.mDropDownList) == null || !dst.isShown()) {
                return false;
            }
            MotionEvent dstEvent = MotionEvent.obtainNoHistory(srcEvent);
            src.toGlobalMotionEvent(dstEvent);
            dst.toLocalMotionEvent(dstEvent);
            boolean handled = dst.onForwardedEvent(dstEvent, this.mActivePointerId);
            dstEvent.recycle();
            int action = srcEvent.getActionMasked();
            boolean keepForwarding = (action == 1 || action == 3) ? false : true;
            return handled && keepForwarding;
        }

        private class DisallowIntercept implements Runnable {
            private DisallowIntercept() {
            }

            @Override
            public void run() {
                ViewParent parent = ForwardingListener.this.mSrc.getParent();
                parent.requestDisallowInterceptTouchEvent(true);
            }
        }

        private class TriggerLongPress implements Runnable {
            private TriggerLongPress() {
            }

            @Override
            public void run() {
                ForwardingListener.this.onLongPress();
            }
        }
    }

    private static class DropDownListView extends ListView {
        private static final int CLICK_ANIM_ALPHA = 128;
        private static final long CLICK_ANIM_DURATION = 150;
        private static final IntProperty<Drawable> DRAWABLE_ALPHA = new IntProperty<Drawable>("alpha") {
            @Override
            public void setValue(Drawable object, int value) {
                object.setAlpha(value);
            }

            @Override
            public Integer get(Drawable object) {
                return Integer.valueOf(object.getAlpha());
            }
        };
        private Animator mClickAnimation;
        private boolean mDrawsInPressedState;
        private boolean mHijackFocus;
        private boolean mListSelectionHidden;
        private AutoScrollHelper.AbsListViewAutoScroller mScrollHelper;

        public DropDownListView(Context context, boolean hijackFocus) {
            super(context, null, 16842861);
            this.mHijackFocus = hijackFocus;
            setCacheColorHint(0);
        }

        public boolean onForwardedEvent(MotionEvent event, int activePointerId) {
            boolean handledEvent = true;
            boolean clearPressedItem = false;
            int actionMasked = event.getActionMasked();
            switch (actionMasked) {
                case 1:
                    handledEvent = false;
                case 2:
                    int activeIndex = event.findPointerIndex(activePointerId);
                    if (activeIndex < 0) {
                        handledEvent = false;
                    } else {
                        int x = (int) event.getX(activeIndex);
                        int y = (int) event.getY(activeIndex);
                        int position = pointToPosition(x, y);
                        if (position == -1) {
                            clearPressedItem = true;
                        } else {
                            View child = getChildAt(position - getFirstVisiblePosition());
                            setPressedItem(child, position, x, y);
                            handledEvent = true;
                            if (actionMasked == 1) {
                                clickPressedItem(child, position);
                            }
                        }
                    }
                    break;
                case 3:
                    handledEvent = false;
                    break;
            }
            if (!handledEvent || clearPressedItem) {
                clearPressedItem();
            }
            if (handledEvent) {
                if (this.mScrollHelper == null) {
                    this.mScrollHelper = new AutoScrollHelper.AbsListViewAutoScroller(this);
                }
                this.mScrollHelper.setEnabled(true);
                this.mScrollHelper.onTouch(this, event);
            } else if (this.mScrollHelper != null) {
                this.mScrollHelper.setEnabled(false);
            }
            return handledEvent;
        }

        private void clickPressedItem(final View child, final int position) {
            final long id = getItemIdAtPosition(position);
            Animator anim = ObjectAnimator.ofInt(this.mSelector, DRAWABLE_ALPHA, 255, 128, 255);
            anim.setDuration(CLICK_ANIM_DURATION);
            anim.setInterpolator(new AccelerateDecelerateInterpolator());
            anim.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    DropDownListView.this.performItemClick(child, position, id);
                }
            });
            anim.start();
            if (this.mClickAnimation != null) {
                this.mClickAnimation.cancel();
            }
            this.mClickAnimation = anim;
        }

        private void clearPressedItem() {
            this.mDrawsInPressedState = false;
            setPressed(false);
            updateSelectorState();
            View motionView = getChildAt(this.mMotionPosition - this.mFirstPosition);
            if (motionView != null) {
                motionView.setPressed(false);
            }
            if (this.mClickAnimation != null) {
                this.mClickAnimation.cancel();
                this.mClickAnimation = null;
            }
        }

        private void setPressedItem(View child, int position, float x, float y) {
            this.mDrawsInPressedState = true;
            drawableHotspotChanged(x, y);
            if (!isPressed()) {
                setPressed(true);
            }
            if (this.mDataChanged) {
                layoutChildren();
            }
            View motionView = getChildAt(this.mMotionPosition - this.mFirstPosition);
            if (motionView != null && motionView != child && motionView.isPressed()) {
                motionView.setPressed(false);
            }
            this.mMotionPosition = position;
            float childX = x - child.getLeft();
            float childY = y - child.getTop();
            child.drawableHotspotChanged(childX, childY);
            if (!child.isPressed()) {
                child.setPressed(true);
            }
            setSelectedPositionInt(position);
            positionSelectorLikeTouch(position, child, x, y);
            refreshDrawableState();
            if (this.mClickAnimation != null) {
                this.mClickAnimation.cancel();
                this.mClickAnimation = null;
            }
        }

        @Override
        boolean touchModeDrawsInPressedState() {
            return this.mDrawsInPressedState || super.touchModeDrawsInPressedState();
        }

        @Override
        View obtainView(int position, boolean[] isScrap) {
            View view = super.obtainView(position, isScrap);
            if (view instanceof TextView) {
                ((TextView) view).setHorizontallyScrolling(true);
            }
            return view;
        }

        @Override
        public boolean isInTouchMode() {
            return (this.mHijackFocus && this.mListSelectionHidden) || super.isInTouchMode();
        }

        @Override
        public boolean hasWindowFocus() {
            return this.mHijackFocus || super.hasWindowFocus();
        }

        @Override
        public boolean isFocused() {
            return this.mHijackFocus || super.isFocused();
        }

        @Override
        public boolean hasFocus() {
            return this.mHijackFocus || super.hasFocus();
        }
    }

    private class PopupDataSetObserver extends DataSetObserver {
        private PopupDataSetObserver() {
        }

        @Override
        public void onChanged() {
            if (ListPopupWindow.this.isShowing()) {
                ListPopupWindow.this.show();
            }
        }

        @Override
        public void onInvalidated() {
            ListPopupWindow.this.dismiss();
        }
    }

    private class ListSelectorHider implements Runnable {
        private ListSelectorHider() {
        }

        @Override
        public void run() {
            ListPopupWindow.this.clearListSelection();
        }
    }

    private class ResizePopupRunnable implements Runnable {
        private ResizePopupRunnable() {
        }

        @Override
        public void run() {
            if (ListPopupWindow.this.mDropDownList != null && ListPopupWindow.this.mDropDownList.getCount() > ListPopupWindow.this.mDropDownList.getChildCount() && ListPopupWindow.this.mDropDownList.getChildCount() <= ListPopupWindow.this.mListItemExpandMaximum) {
                ListPopupWindow.this.mPopup.setInputMethodMode(2);
                ListPopupWindow.this.show();
            }
        }
    }

    private class PopupTouchInterceptor implements View.OnTouchListener {
        private PopupTouchInterceptor() {
        }

        @Override
        public boolean onTouch(View v, MotionEvent event) {
            int action = event.getAction();
            int x = (int) event.getX();
            int y = (int) event.getY();
            if (action == 0 && ListPopupWindow.this.mPopup != null && ListPopupWindow.this.mPopup.isShowing() && x >= 0 && x < ListPopupWindow.this.mPopup.getWidth() && y >= 0 && y < ListPopupWindow.this.mPopup.getHeight()) {
                ListPopupWindow.this.mHandler.postDelayed(ListPopupWindow.this.mResizePopupRunnable, 250L);
                return false;
            }
            if (action == 1) {
                ListPopupWindow.this.mHandler.removeCallbacks(ListPopupWindow.this.mResizePopupRunnable);
                return false;
            }
            return false;
        }
    }

    private class PopupScrollListener implements AbsListView.OnScrollListener {
        private PopupScrollListener() {
        }

        @Override
        public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
        }

        @Override
        public void onScrollStateChanged(AbsListView view, int scrollState) {
            if (scrollState == 1 && !ListPopupWindow.this.isInputMethodNotNeeded() && ListPopupWindow.this.mPopup.getContentView() != null) {
                ListPopupWindow.this.mHandler.removeCallbacks(ListPopupWindow.this.mResizePopupRunnable);
                ListPopupWindow.this.mResizePopupRunnable.run();
            }
        }
    }
}
