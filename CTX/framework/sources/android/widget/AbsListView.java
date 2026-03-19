package android.widget;

import android.app.slice.Slice;
import android.content.Context;
import android.content.Intent;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.TransitionDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.StrictMode;
import android.os.Trace;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.util.Log;
import android.util.LongSparseArray;
import android.util.SparseArray;
import android.util.SparseBooleanArray;
import android.util.StateSet;
import android.view.ActionMode;
import android.view.ContextMenu;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.PointerIcon;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewDebug;
import android.view.ViewGroup;
import android.view.ViewHierarchyEncoder;
import android.view.ViewParent;
import android.view.ViewTreeObserver;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityManager;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.animation.Interpolator;
import android.view.animation.LinearInterpolator;
import android.view.inputmethod.BaseInputConnection;
import android.view.inputmethod.CompletionInfo;
import android.view.inputmethod.CorrectionInfo;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.ExtractedText;
import android.view.inputmethod.ExtractedTextRequest;
import android.view.inputmethod.InputConnection;
import android.view.inputmethod.InputContentInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.Filter;
import android.widget.RemoteViews;
import android.widget.RemoteViewsAdapter;
import com.android.internal.R;
import java.util.ArrayList;
import java.util.List;

public abstract class AbsListView extends AdapterView<ListAdapter> implements TextWatcher, ViewTreeObserver.OnGlobalLayoutListener, Filter.FilterListener, ViewTreeObserver.OnTouchModeChangeListener, RemoteViewsAdapter.RemoteAdapterConnectionCallback {
    private static final int CHECK_POSITION_SEARCH_DISTANCE = 20;
    public static final int CHOICE_MODE_MULTIPLE = 2;
    public static final int CHOICE_MODE_MULTIPLE_MODAL = 3;
    public static final int CHOICE_MODE_NONE = 0;
    public static final int CHOICE_MODE_SINGLE = 1;
    private static final int INVALID_POINTER = -1;
    static final int LAYOUT_FORCE_BOTTOM = 3;
    static final int LAYOUT_FORCE_TOP = 1;
    static final int LAYOUT_MOVE_SELECTION = 6;
    static final int LAYOUT_NORMAL = 0;
    static final int LAYOUT_SET_SELECTION = 2;
    static final int LAYOUT_SPECIFIC = 4;
    static final int LAYOUT_SYNC = 5;
    static final int OVERSCROLL_LIMIT_DIVISOR = 3;
    private static final boolean PROFILE_FLINGING = false;
    private static final boolean PROFILE_SCROLLING = false;
    private static final String TAG = "AbsListView";
    static final int TOUCH_MODE_DONE_WAITING = 2;
    static final int TOUCH_MODE_DOWN = 0;
    static final int TOUCH_MODE_FLING = 4;
    private static final int TOUCH_MODE_OFF = 1;
    private static final int TOUCH_MODE_ON = 0;
    static final int TOUCH_MODE_OVERFLING = 6;
    static final int TOUCH_MODE_OVERSCROLL = 5;
    static final int TOUCH_MODE_REST = -1;
    static final int TOUCH_MODE_SCROLL = 3;
    static final int TOUCH_MODE_TAP = 1;
    private static final int TOUCH_MODE_UNKNOWN = -1;
    public static final int TRANSCRIPT_MODE_ALWAYS_SCROLL = 2;
    public static final int TRANSCRIPT_MODE_DISABLED = 0;
    public static final int TRANSCRIPT_MODE_NORMAL = 1;
    static final Interpolator sLinearInterpolator = new LinearInterpolator();
    private ListItemAccessibilityDelegate mAccessibilityDelegate;
    private int mActivePointerId;
    ListAdapter mAdapter;
    boolean mAdapterHasStableIds;
    private int mCacheColorHint;
    boolean mCachingActive;
    boolean mCachingStarted;
    SparseBooleanArray mCheckStates;
    LongSparseArray<Integer> mCheckedIdStates;
    int mCheckedItemCount;
    ActionMode mChoiceActionMode;
    int mChoiceMode;
    private Runnable mClearScrollingCache;
    private ContextMenu.ContextMenuInfo mContextMenuInfo;
    AdapterDataSetObserver mDataSetObserver;
    private InputConnection mDefInputConnection;
    private boolean mDeferNotifyDataSetChanged;
    private float mDensityScale;
    private int mDirection;
    boolean mDrawSelectorOnTop;
    private EdgeEffect mEdgeGlowBottom;
    private EdgeEffect mEdgeGlowTop;
    private FastScroller mFastScroll;
    boolean mFastScrollAlwaysVisible;
    boolean mFastScrollEnabled;
    private int mFastScrollStyle;
    private boolean mFiltered;
    private int mFirstPositionDistanceGuess;
    private boolean mFlingProfilingStarted;
    private FlingRunnable mFlingRunnable;
    private StrictMode.Span mFlingStrictSpan;
    private boolean mForceTranscriptScroll;
    private boolean mGlobalLayoutListenerAddedFilter;
    private boolean mHasPerformedLongPress;
    private boolean mIsChildViewEnabled;
    private boolean mIsDetaching;
    final boolean[] mIsScrap;
    private int mLastAccessibilityScrollEventFromIndex;
    private int mLastAccessibilityScrollEventToIndex;
    private int mLastHandledItemCount;
    private int mLastPositionDistanceGuess;
    private int mLastScrollState;
    private int mLastTouchMode;
    int mLastY;
    int mLayoutMode;
    Rect mListPadding;
    private int mMaximumVelocity;
    private int mMinimumVelocity;
    int mMotionCorrection;
    int mMotionPosition;
    int mMotionViewNewTop;
    int mMotionViewOriginalTop;
    int mMotionX;
    int mMotionY;
    MultiChoiceModeWrapper mMultiChoiceModeCallback;
    private int mNestedYOffset;
    private OnScrollListener mOnScrollListener;
    int mOverflingDistance;
    int mOverscrollDistance;
    int mOverscrollMax;
    private final Thread mOwnerThread;
    private CheckForKeyLongPress mPendingCheckForKeyLongPress;
    private CheckForLongPress mPendingCheckForLongPress;
    private CheckForTap mPendingCheckForTap;
    private SavedState mPendingSync;
    private PerformClick mPerformClick;
    PopupWindow mPopup;
    private boolean mPopupHidden;
    Runnable mPositionScrollAfterLayout;
    AbsPositionScroller mPositionScroller;
    private InputConnectionWrapper mPublicInputConnection;
    final RecycleBin mRecycler;
    private RemoteViewsAdapter mRemoteAdapter;
    int mResurrectToPosition;
    private final int[] mScrollConsumed;
    View mScrollDown;
    private final int[] mScrollOffset;
    private boolean mScrollProfilingStarted;
    private StrictMode.Span mScrollStrictSpan;
    View mScrollUp;
    boolean mScrollingCacheEnabled;
    int mSelectedTop;
    int mSelectionBottomPadding;
    int mSelectionLeftPadding;
    int mSelectionRightPadding;
    int mSelectionTopPadding;
    Drawable mSelector;
    int mSelectorPosition;
    Rect mSelectorRect;
    private int[] mSelectorState;
    private boolean mSmoothScrollbarEnabled;
    boolean mStackFromBottom;
    EditText mTextFilter;
    private boolean mTextFilterEnabled;
    private final float[] mTmpPoint;
    private Rect mTouchFrame;
    int mTouchMode;
    private Runnable mTouchModeReset;
    private int mTouchSlop;
    private int mTranscriptMode;
    private float mVelocityScale;
    private VelocityTracker mVelocityTracker;
    private float mVerticalScrollFactor;
    int mWidthMeasureSpec;

    public interface MultiChoiceModeListener extends ActionMode.Callback {
        void onItemCheckedStateChanged(ActionMode actionMode, int i, long j, boolean z);
    }

    public interface OnScrollListener {
        public static final int SCROLL_STATE_FLING = 2;
        public static final int SCROLL_STATE_IDLE = 0;
        public static final int SCROLL_STATE_TOUCH_SCROLL = 1;

        void onScroll(AbsListView absListView, int i, int i2, int i3);

        void onScrollStateChanged(AbsListView absListView, int i);
    }

    public interface RecyclerListener {
        void onMovedToScrapHeap(View view);
    }

    public interface SelectionBoundsAdjuster {
        void adjustListItemSelectionBounds(Rect rect);
    }

    abstract void fillGap(boolean z);

    abstract int findMotionRow(int i);

    abstract void setSelectionInt(int i);

    public AbsListView(Context context) {
        super(context);
        this.mChoiceMode = 0;
        this.mLayoutMode = 0;
        this.mDeferNotifyDataSetChanged = false;
        this.mDrawSelectorOnTop = false;
        this.mSelectorPosition = -1;
        this.mSelectorRect = new Rect();
        this.mRecycler = new RecycleBin();
        this.mSelectionLeftPadding = 0;
        this.mSelectionTopPadding = 0;
        this.mSelectionRightPadding = 0;
        this.mSelectionBottomPadding = 0;
        this.mListPadding = new Rect();
        this.mWidthMeasureSpec = 0;
        this.mTouchMode = -1;
        this.mSelectedTop = 0;
        this.mSmoothScrollbarEnabled = true;
        this.mResurrectToPosition = -1;
        this.mContextMenuInfo = null;
        this.mLastTouchMode = -1;
        this.mScrollProfilingStarted = false;
        this.mFlingProfilingStarted = false;
        this.mScrollStrictSpan = null;
        this.mFlingStrictSpan = null;
        this.mLastScrollState = 0;
        this.mVelocityScale = 1.0f;
        this.mIsScrap = new boolean[1];
        this.mScrollOffset = new int[2];
        this.mScrollConsumed = new int[2];
        this.mTmpPoint = new float[2];
        this.mNestedYOffset = 0;
        this.mActivePointerId = -1;
        this.mDirection = 0;
        initAbsListView();
        this.mOwnerThread = Thread.currentThread();
        setVerticalScrollBarEnabled(true);
        TypedArray typedArrayObtainStyledAttributes = context.obtainStyledAttributes(R.styleable.View);
        initializeScrollbarsInternal(typedArrayObtainStyledAttributes);
        typedArrayObtainStyledAttributes.recycle();
    }

    public AbsListView(Context context, AttributeSet attributeSet) {
        this(context, attributeSet, 16842858);
    }

    public AbsListView(Context context, AttributeSet attributeSet, int i) {
        this(context, attributeSet, i, 0);
    }

    public AbsListView(Context context, AttributeSet attributeSet, int i, int i2) {
        super(context, attributeSet, i, i2);
        this.mChoiceMode = 0;
        this.mLayoutMode = 0;
        this.mDeferNotifyDataSetChanged = false;
        this.mDrawSelectorOnTop = false;
        this.mSelectorPosition = -1;
        this.mSelectorRect = new Rect();
        this.mRecycler = new RecycleBin();
        this.mSelectionLeftPadding = 0;
        this.mSelectionTopPadding = 0;
        this.mSelectionRightPadding = 0;
        this.mSelectionBottomPadding = 0;
        this.mListPadding = new Rect();
        this.mWidthMeasureSpec = 0;
        this.mTouchMode = -1;
        this.mSelectedTop = 0;
        this.mSmoothScrollbarEnabled = true;
        this.mResurrectToPosition = -1;
        this.mContextMenuInfo = null;
        this.mLastTouchMode = -1;
        this.mScrollProfilingStarted = false;
        this.mFlingProfilingStarted = false;
        this.mScrollStrictSpan = null;
        this.mFlingStrictSpan = null;
        this.mLastScrollState = 0;
        this.mVelocityScale = 1.0f;
        this.mIsScrap = new boolean[1];
        this.mScrollOffset = new int[2];
        this.mScrollConsumed = new int[2];
        this.mTmpPoint = new float[2];
        this.mNestedYOffset = 0;
        this.mActivePointerId = -1;
        this.mDirection = 0;
        initAbsListView();
        this.mOwnerThread = Thread.currentThread();
        TypedArray typedArrayObtainStyledAttributes = context.obtainStyledAttributes(attributeSet, R.styleable.AbsListView, i, i2);
        Drawable drawable = typedArrayObtainStyledAttributes.getDrawable(0);
        if (drawable != null) {
            setSelector(drawable);
        }
        this.mDrawSelectorOnTop = typedArrayObtainStyledAttributes.getBoolean(1, false);
        setStackFromBottom(typedArrayObtainStyledAttributes.getBoolean(2, false));
        setScrollingCacheEnabled(typedArrayObtainStyledAttributes.getBoolean(3, true));
        setTextFilterEnabled(typedArrayObtainStyledAttributes.getBoolean(4, false));
        setTranscriptMode(typedArrayObtainStyledAttributes.getInt(5, 0));
        setCacheColorHint(typedArrayObtainStyledAttributes.getColor(6, 0));
        setSmoothScrollbarEnabled(typedArrayObtainStyledAttributes.getBoolean(9, true));
        setChoiceMode(typedArrayObtainStyledAttributes.getInt(7, 0));
        setFastScrollEnabled(typedArrayObtainStyledAttributes.getBoolean(8, false));
        setFastScrollStyle(typedArrayObtainStyledAttributes.getResourceId(11, 0));
        setFastScrollAlwaysVisible(typedArrayObtainStyledAttributes.getBoolean(10, false));
        typedArrayObtainStyledAttributes.recycle();
        if (context.getResources().getConfiguration().uiMode == 6) {
            setRevealOnFocusHint(false);
        }
    }

    private void initAbsListView() {
        setClickable(true);
        setFocusableInTouchMode(true);
        setWillNotDraw(false);
        setAlwaysDrawnWithCacheEnabled(false);
        setScrollingCacheEnabled(true);
        ViewConfiguration viewConfiguration = ViewConfiguration.get(this.mContext);
        this.mTouchSlop = viewConfiguration.getScaledTouchSlop();
        this.mVerticalScrollFactor = viewConfiguration.getScaledVerticalScrollFactor();
        this.mMinimumVelocity = viewConfiguration.getScaledMinimumFlingVelocity();
        this.mMaximumVelocity = viewConfiguration.getScaledMaximumFlingVelocity();
        this.mOverscrollDistance = viewConfiguration.getScaledOverscrollDistance();
        this.mOverflingDistance = viewConfiguration.getScaledOverflingDistance();
        this.mDensityScale = getContext().getResources().getDisplayMetrics().density;
    }

    @Override
    public void setOverScrollMode(int i) {
        if (i != 2) {
            if (this.mEdgeGlowTop == null) {
                Context context = getContext();
                this.mEdgeGlowTop = new EdgeEffect(context);
                this.mEdgeGlowBottom = new EdgeEffect(context);
            }
        } else {
            this.mEdgeGlowTop = null;
            this.mEdgeGlowBottom = null;
        }
        super.setOverScrollMode(i);
    }

    @Override
    public void setAdapter(ListAdapter listAdapter) {
        if (listAdapter != null) {
            this.mAdapterHasStableIds = this.mAdapter.hasStableIds();
            if (this.mChoiceMode != 0 && this.mAdapterHasStableIds && this.mCheckedIdStates == null) {
                this.mCheckedIdStates = new LongSparseArray<>();
            }
        }
        clearChoices();
    }

    public int getCheckedItemCount() {
        return this.mCheckedItemCount;
    }

    public boolean isItemChecked(int i) {
        if (this.mChoiceMode != 0 && this.mCheckStates != null) {
            return this.mCheckStates.get(i);
        }
        return false;
    }

    public int getCheckedItemPosition() {
        if (this.mChoiceMode == 1 && this.mCheckStates != null && this.mCheckStates.size() == 1) {
            return this.mCheckStates.keyAt(0);
        }
        return -1;
    }

    public SparseBooleanArray getCheckedItemPositions() {
        if (this.mChoiceMode != 0) {
            return this.mCheckStates;
        }
        return null;
    }

    public long[] getCheckedItemIds() {
        if (this.mChoiceMode == 0 || this.mCheckedIdStates == null || this.mAdapter == null) {
            return new long[0];
        }
        LongSparseArray<Integer> longSparseArray = this.mCheckedIdStates;
        int size = longSparseArray.size();
        long[] jArr = new long[size];
        for (int i = 0; i < size; i++) {
            jArr[i] = longSparseArray.keyAt(i);
        }
        return jArr;
    }

    public void clearChoices() {
        if (this.mCheckStates != null) {
            this.mCheckStates.clear();
        }
        if (this.mCheckedIdStates != null) {
            this.mCheckedIdStates.clear();
        }
        this.mCheckedItemCount = 0;
    }

    public void setItemChecked(int i, boolean z) {
        boolean z2;
        if (this.mChoiceMode == 0) {
            return;
        }
        if (z && this.mChoiceMode == 3 && this.mChoiceActionMode == null) {
            if (this.mMultiChoiceModeCallback == null || !this.mMultiChoiceModeCallback.hasWrappedCallback()) {
                throw new IllegalStateException("AbsListView: attempted to start selection mode for CHOICE_MODE_MULTIPLE_MODAL but no choice mode callback was supplied. Call setMultiChoiceModeListener to set a callback.");
            }
            this.mChoiceActionMode = startActionMode(this.mMultiChoiceModeCallback);
        }
        if (this.mChoiceMode == 2 || this.mChoiceMode == 3) {
            boolean z3 = this.mCheckStates.get(i);
            this.mCheckStates.put(i, z);
            if (this.mCheckedIdStates != null && this.mAdapter.hasStableIds()) {
                if (z) {
                    this.mCheckedIdStates.put(this.mAdapter.getItemId(i), Integer.valueOf(i));
                } else {
                    this.mCheckedIdStates.delete(this.mAdapter.getItemId(i));
                }
            }
            z2 = z3 != z;
            if (z2) {
                if (z) {
                    this.mCheckedItemCount++;
                } else {
                    this.mCheckedItemCount--;
                }
            }
            if (this.mChoiceActionMode != null) {
                this.mMultiChoiceModeCallback.onItemCheckedStateChanged(this.mChoiceActionMode, i, this.mAdapter.getItemId(i), z);
            }
        } else {
            boolean z4 = this.mCheckedIdStates != null && this.mAdapter.hasStableIds();
            z2 = isItemChecked(i) != z;
            if (z || isItemChecked(i)) {
                this.mCheckStates.clear();
                if (z4) {
                    this.mCheckedIdStates.clear();
                }
            }
            if (z) {
                this.mCheckStates.put(i, true);
                if (z4) {
                    this.mCheckedIdStates.put(this.mAdapter.getItemId(i), Integer.valueOf(i));
                }
                this.mCheckedItemCount = 1;
            } else if (this.mCheckStates.size() == 0 || !this.mCheckStates.valueAt(0)) {
                this.mCheckedItemCount = 0;
            }
        }
        if (!this.mInLayout && !this.mBlockLayoutRequests && z2) {
            this.mDataChanged = true;
            rememberSyncState();
            requestLayout();
        }
    }

    @Override
    public boolean performItemClick(View view, int i, long j) {
        boolean z;
        boolean z2 = false;
        if (this.mChoiceMode != 0) {
            if (this.mChoiceMode == 2 || (this.mChoiceMode == 3 && this.mChoiceActionMode != null)) {
                boolean z3 = !this.mCheckStates.get(i, false);
                this.mCheckStates.put(i, z3);
                if (this.mCheckedIdStates != null && this.mAdapter.hasStableIds()) {
                    if (z3) {
                        this.mCheckedIdStates.put(this.mAdapter.getItemId(i), Integer.valueOf(i));
                    } else {
                        this.mCheckedIdStates.delete(this.mAdapter.getItemId(i));
                    }
                }
                if (z3) {
                    this.mCheckedItemCount++;
                } else {
                    this.mCheckedItemCount--;
                }
                if (this.mChoiceActionMode != null) {
                    this.mMultiChoiceModeCallback.onItemCheckedStateChanged(this.mChoiceActionMode, i, j, z3);
                } else {
                    z2 = true;
                }
                z = z2;
                z2 = true;
            } else if (this.mChoiceMode == 1) {
                if (!this.mCheckStates.get(i, false)) {
                    this.mCheckStates.clear();
                    this.mCheckStates.put(i, true);
                    if (this.mCheckedIdStates != null && this.mAdapter.hasStableIds()) {
                        this.mCheckedIdStates.clear();
                        this.mCheckedIdStates.put(this.mAdapter.getItemId(i), Integer.valueOf(i));
                    }
                    this.mCheckedItemCount = 1;
                } else if (this.mCheckStates.size() == 0 || !this.mCheckStates.valueAt(0)) {
                    this.mCheckedItemCount = 0;
                }
                z = true;
                z2 = true;
            } else {
                z = true;
            }
            if (z2) {
                updateOnScreenCheckedViews();
            }
            z2 = true;
        } else {
            z = true;
        }
        if (z) {
            return z2 | super.performItemClick(view, i, j);
        }
        return z2;
    }

    private void updateOnScreenCheckedViews() {
        int i = this.mFirstPosition;
        int childCount = getChildCount();
        boolean z = getContext().getApplicationInfo().targetSdkVersion >= 11;
        for (int i2 = 0; i2 < childCount; i2++) {
            View childAt = getChildAt(i2);
            int i3 = i + i2;
            if (childAt instanceof Checkable) {
                ((Checkable) childAt).setChecked(this.mCheckStates.get(i3));
            } else if (z) {
                childAt.setActivated(this.mCheckStates.get(i3));
            }
        }
    }

    public int getChoiceMode() {
        return this.mChoiceMode;
    }

    public void setChoiceMode(int i) {
        this.mChoiceMode = i;
        if (this.mChoiceActionMode != null) {
            this.mChoiceActionMode.finish();
            this.mChoiceActionMode = null;
        }
        if (this.mChoiceMode != 0) {
            if (this.mCheckStates == null) {
                this.mCheckStates = new SparseBooleanArray(0);
            }
            if (this.mCheckedIdStates == null && this.mAdapter != null && this.mAdapter.hasStableIds()) {
                this.mCheckedIdStates = new LongSparseArray<>(0);
            }
            if (this.mChoiceMode == 3) {
                clearChoices();
                setLongClickable(true);
            }
        }
    }

    public void setMultiChoiceModeListener(MultiChoiceModeListener multiChoiceModeListener) {
        if (this.mMultiChoiceModeCallback == null) {
            this.mMultiChoiceModeCallback = new MultiChoiceModeWrapper();
        }
        this.mMultiChoiceModeCallback.setWrapped(multiChoiceModeListener);
    }

    private boolean contentFits() {
        int childCount = getChildCount();
        if (childCount == 0) {
            return true;
        }
        if (childCount != this.mItemCount) {
            return false;
        }
        return getChildAt(0).getTop() >= this.mListPadding.top && getChildAt(childCount - 1).getBottom() <= getHeight() - this.mListPadding.bottom;
    }

    public void setFastScrollEnabled(final boolean z) {
        if (this.mFastScrollEnabled != z) {
            this.mFastScrollEnabled = z;
            if (isOwnerThread()) {
                setFastScrollerEnabledUiThread(z);
            } else {
                post(new Runnable() {
                    @Override
                    public void run() {
                        AbsListView.this.setFastScrollerEnabledUiThread(z);
                    }
                });
            }
        }
    }

    private void setFastScrollerEnabledUiThread(boolean z) {
        if (this.mFastScroll != null) {
            this.mFastScroll.setEnabled(z);
        } else if (z) {
            this.mFastScroll = new FastScroller(this, this.mFastScrollStyle);
            this.mFastScroll.setEnabled(true);
        }
        resolvePadding();
        if (this.mFastScroll != null) {
            this.mFastScroll.updateLayout();
        }
    }

    public void setFastScrollStyle(int i) {
        if (this.mFastScroll == null) {
            this.mFastScrollStyle = i;
        } else {
            this.mFastScroll.setStyle(i);
        }
    }

    public void setFastScrollAlwaysVisible(final boolean z) {
        if (this.mFastScrollAlwaysVisible != z) {
            if (z && !this.mFastScrollEnabled) {
                setFastScrollEnabled(true);
            }
            this.mFastScrollAlwaysVisible = z;
            if (isOwnerThread()) {
                setFastScrollerAlwaysVisibleUiThread(z);
            } else {
                post(new Runnable() {
                    @Override
                    public void run() {
                        AbsListView.this.setFastScrollerAlwaysVisibleUiThread(z);
                    }
                });
            }
        }
    }

    private void setFastScrollerAlwaysVisibleUiThread(boolean z) {
        if (this.mFastScroll != null) {
            this.mFastScroll.setAlwaysShow(z);
        }
    }

    private boolean isOwnerThread() {
        return this.mOwnerThread == Thread.currentThread();
    }

    public boolean isFastScrollAlwaysVisible() {
        return this.mFastScroll == null ? this.mFastScrollEnabled && this.mFastScrollAlwaysVisible : this.mFastScroll.isEnabled() && this.mFastScroll.isAlwaysShowEnabled();
    }

    @Override
    public int getVerticalScrollbarWidth() {
        if (this.mFastScroll != null && this.mFastScroll.isEnabled()) {
            return Math.max(super.getVerticalScrollbarWidth(), this.mFastScroll.getWidth());
        }
        return super.getVerticalScrollbarWidth();
    }

    @ViewDebug.ExportedProperty
    public boolean isFastScrollEnabled() {
        if (this.mFastScroll == null) {
            return this.mFastScrollEnabled;
        }
        return this.mFastScroll.isEnabled();
    }

    @Override
    public void setVerticalScrollbarPosition(int i) {
        super.setVerticalScrollbarPosition(i);
        if (this.mFastScroll != null) {
            this.mFastScroll.setScrollbarPosition(i);
        }
    }

    @Override
    public void setScrollBarStyle(int i) {
        super.setScrollBarStyle(i);
        if (this.mFastScroll != null) {
            this.mFastScroll.setScrollBarStyle(i);
        }
    }

    @Override
    protected boolean isVerticalScrollBarHidden() {
        return isFastScrollEnabled();
    }

    public void setSmoothScrollbarEnabled(boolean z) {
        this.mSmoothScrollbarEnabled = z;
    }

    @ViewDebug.ExportedProperty
    public boolean isSmoothScrollbarEnabled() {
        return this.mSmoothScrollbarEnabled;
    }

    public void setOnScrollListener(OnScrollListener onScrollListener) {
        this.mOnScrollListener = onScrollListener;
        invokeOnItemScrollListener();
    }

    void invokeOnItemScrollListener() {
        if (this.mFastScroll != null) {
            this.mFastScroll.onScroll(this.mFirstPosition, getChildCount(), this.mItemCount);
        }
        if (this.mOnScrollListener != null) {
            this.mOnScrollListener.onScroll(this, this.mFirstPosition, getChildCount(), this.mItemCount);
        }
        onScrollChanged(0, 0, 0, 0);
    }

    @Override
    public void sendAccessibilityEventUnchecked(AccessibilityEvent accessibilityEvent) {
        if (accessibilityEvent.getEventType() == 4096) {
            int firstVisiblePosition = getFirstVisiblePosition();
            int lastVisiblePosition = getLastVisiblePosition();
            if (this.mLastAccessibilityScrollEventFromIndex == firstVisiblePosition && this.mLastAccessibilityScrollEventToIndex == lastVisiblePosition) {
                return;
            }
            this.mLastAccessibilityScrollEventFromIndex = firstVisiblePosition;
            this.mLastAccessibilityScrollEventToIndex = lastVisiblePosition;
        }
        super.sendAccessibilityEventUnchecked(accessibilityEvent);
    }

    @Override
    public CharSequence getAccessibilityClassName() {
        return AbsListView.class.getName();
    }

    @Override
    public void onInitializeAccessibilityNodeInfoInternal(AccessibilityNodeInfo accessibilityNodeInfo) {
        super.onInitializeAccessibilityNodeInfoInternal(accessibilityNodeInfo);
        if (isEnabled()) {
            if (canScrollUp()) {
                accessibilityNodeInfo.addAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_SCROLL_BACKWARD);
                accessibilityNodeInfo.addAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_SCROLL_UP);
                accessibilityNodeInfo.setScrollable(true);
            }
            if (canScrollDown()) {
                accessibilityNodeInfo.addAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_SCROLL_FORWARD);
                accessibilityNodeInfo.addAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_SCROLL_DOWN);
                accessibilityNodeInfo.setScrollable(true);
            }
        }
        accessibilityNodeInfo.removeAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_CLICK);
        accessibilityNodeInfo.setClickable(false);
    }

    int getSelectionModeForAccessibility() {
        switch (getChoiceMode()) {
        }
        return 0;
    }

    @Override
    public boolean performAccessibilityActionInternal(int i, Bundle bundle) {
        if (super.performAccessibilityActionInternal(i, bundle)) {
            return true;
        }
        if (i != 4096) {
            if (i == 8192 || i == 16908344) {
                if (!isEnabled() || !canScrollUp()) {
                    return false;
                }
                smoothScrollBy(-((getHeight() - this.mListPadding.top) - this.mListPadding.bottom), 200);
                return true;
            }
            if (i != 16908346) {
                return false;
            }
        }
        if (!isEnabled() || !canScrollDown()) {
            return false;
        }
        smoothScrollBy((getHeight() - this.mListPadding.top) - this.mListPadding.bottom, 200);
        return true;
    }

    @Override
    public View findViewByAccessibilityIdTraversal(int i) {
        if (i == getAccessibilityViewId()) {
            return this;
        }
        return super.findViewByAccessibilityIdTraversal(i);
    }

    @ViewDebug.ExportedProperty
    public boolean isScrollingCacheEnabled() {
        return this.mScrollingCacheEnabled;
    }

    public void setScrollingCacheEnabled(boolean z) {
        if (this.mScrollingCacheEnabled && !z) {
            clearScrollingCache();
        }
        this.mScrollingCacheEnabled = z;
    }

    public void setTextFilterEnabled(boolean z) {
        this.mTextFilterEnabled = z;
    }

    @ViewDebug.ExportedProperty
    public boolean isTextFilterEnabled() {
        return this.mTextFilterEnabled;
    }

    @Override
    public void getFocusedRect(Rect rect) {
        View selectedView = getSelectedView();
        if (selectedView != null && selectedView.getParent() == this) {
            selectedView.getFocusedRect(rect);
            offsetDescendantRectToMyCoords(selectedView, rect);
        } else {
            super.getFocusedRect(rect);
        }
    }

    private void useDefaultSelector() {
        setSelector(getContext().getDrawable(17301602));
    }

    @ViewDebug.ExportedProperty
    public boolean isStackFromBottom() {
        return this.mStackFromBottom;
    }

    public void setStackFromBottom(boolean z) {
        if (this.mStackFromBottom != z) {
            this.mStackFromBottom = z;
            requestLayoutIfNecessary();
        }
    }

    void requestLayoutIfNecessary() {
        if (getChildCount() > 0) {
            resetList();
            requestLayout();
            invalidate();
        }
    }

    static class SavedState extends View.BaseSavedState {
        public static final Parcelable.Creator<SavedState> CREATOR = new Parcelable.Creator<SavedState>() {
            @Override
            public SavedState createFromParcel(Parcel parcel) {
                return new SavedState(parcel);
            }

            @Override
            public SavedState[] newArray(int i) {
                return new SavedState[i];
            }
        };
        LongSparseArray<Integer> checkIdState;
        SparseBooleanArray checkState;
        int checkedItemCount;
        String filter;
        long firstId;
        int height;
        boolean inActionMode;
        int position;
        long selectedId;
        int viewTop;

        SavedState(Parcelable parcelable) {
            super(parcelable);
        }

        private SavedState(Parcel parcel) {
            super(parcel);
            this.selectedId = parcel.readLong();
            this.firstId = parcel.readLong();
            this.viewTop = parcel.readInt();
            this.position = parcel.readInt();
            this.height = parcel.readInt();
            this.filter = parcel.readString();
            this.inActionMode = parcel.readByte() != 0;
            this.checkedItemCount = parcel.readInt();
            this.checkState = parcel.readSparseBooleanArray();
            int i = parcel.readInt();
            if (i > 0) {
                this.checkIdState = new LongSparseArray<>();
                for (int i2 = 0; i2 < i; i2++) {
                    this.checkIdState.put(parcel.readLong(), Integer.valueOf(parcel.readInt()));
                }
            }
        }

        @Override
        public void writeToParcel(Parcel parcel, int i) {
            super.writeToParcel(parcel, i);
            parcel.writeLong(this.selectedId);
            parcel.writeLong(this.firstId);
            parcel.writeInt(this.viewTop);
            parcel.writeInt(this.position);
            parcel.writeInt(this.height);
            parcel.writeString(this.filter);
            parcel.writeByte(this.inActionMode ? (byte) 1 : (byte) 0);
            parcel.writeInt(this.checkedItemCount);
            parcel.writeSparseBooleanArray(this.checkState);
            int size = this.checkIdState != null ? this.checkIdState.size() : 0;
            parcel.writeInt(size);
            for (int i2 = 0; i2 < size; i2++) {
                parcel.writeLong(this.checkIdState.keyAt(i2));
                parcel.writeInt(this.checkIdState.valueAt(i2).intValue());
            }
        }

        public String toString() {
            return "AbsListView.SavedState{" + Integer.toHexString(System.identityHashCode(this)) + " selectedId=" + this.selectedId + " firstId=" + this.firstId + " viewTop=" + this.viewTop + " position=" + this.position + " height=" + this.height + " filter=" + this.filter + " checkState=" + this.checkState + "}";
        }
    }

    @Override
    public Parcelable onSaveInstanceState() {
        EditText editText;
        Editable text;
        dismissPopup();
        SavedState savedState = new SavedState(super.onSaveInstanceState());
        if (this.mPendingSync != null) {
            savedState.selectedId = this.mPendingSync.selectedId;
            savedState.firstId = this.mPendingSync.firstId;
            savedState.viewTop = this.mPendingSync.viewTop;
            savedState.position = this.mPendingSync.position;
            savedState.height = this.mPendingSync.height;
            savedState.filter = this.mPendingSync.filter;
            savedState.inActionMode = this.mPendingSync.inActionMode;
            savedState.checkedItemCount = this.mPendingSync.checkedItemCount;
            savedState.checkState = this.mPendingSync.checkState;
            savedState.checkIdState = this.mPendingSync.checkIdState;
            return savedState;
        }
        boolean z = getChildCount() > 0 && this.mItemCount > 0;
        long selectedItemId = getSelectedItemId();
        savedState.selectedId = selectedItemId;
        savedState.height = getHeight();
        if (selectedItemId >= 0) {
            savedState.viewTop = this.mSelectedTop;
            savedState.position = getSelectedItemPosition();
            savedState.firstId = -1L;
        } else if (z && this.mFirstPosition > 0) {
            savedState.viewTop = getChildAt(0).getTop();
            int i = this.mFirstPosition;
            if (i >= this.mItemCount) {
                i = this.mItemCount - 1;
            }
            savedState.position = i;
            savedState.firstId = this.mAdapter.getItemId(i);
        } else {
            savedState.viewTop = 0;
            savedState.firstId = -1L;
            savedState.position = 0;
        }
        savedState.filter = null;
        if (this.mFiltered && (editText = this.mTextFilter) != null && (text = editText.getText()) != null) {
            savedState.filter = text.toString();
        }
        savedState.inActionMode = this.mChoiceMode == 3 && this.mChoiceActionMode != null;
        if (this.mCheckStates != null) {
            savedState.checkState = this.mCheckStates.m36clone();
        }
        if (this.mCheckedIdStates != null) {
            LongSparseArray<Integer> longSparseArray = new LongSparseArray<>();
            int size = this.mCheckedIdStates.size();
            for (int i2 = 0; i2 < size; i2++) {
                longSparseArray.put(this.mCheckedIdStates.keyAt(i2), this.mCheckedIdStates.valueAt(i2));
            }
            savedState.checkIdState = longSparseArray;
        }
        savedState.checkedItemCount = this.mCheckedItemCount;
        if (this.mRemoteAdapter != null) {
            this.mRemoteAdapter.saveRemoteViewsCache();
        }
        return savedState;
    }

    @Override
    public void onRestoreInstanceState(Parcelable parcelable) {
        SavedState savedState = (SavedState) parcelable;
        super.onRestoreInstanceState(savedState.getSuperState());
        this.mDataChanged = true;
        this.mSyncHeight = savedState.height;
        if (savedState.selectedId >= 0) {
            this.mNeedSync = true;
            this.mPendingSync = savedState;
            this.mSyncRowId = savedState.selectedId;
            this.mSyncPosition = savedState.position;
            this.mSpecificTop = savedState.viewTop;
            this.mSyncMode = 0;
        } else if (savedState.firstId >= 0) {
            setSelectedPositionInt(-1);
            setNextSelectedPositionInt(-1);
            this.mSelectorPosition = -1;
            this.mNeedSync = true;
            this.mPendingSync = savedState;
            this.mSyncRowId = savedState.firstId;
            this.mSyncPosition = savedState.position;
            this.mSpecificTop = savedState.viewTop;
            this.mSyncMode = 1;
        }
        setFilterText(savedState.filter);
        if (savedState.checkState != null) {
            this.mCheckStates = savedState.checkState;
        }
        if (savedState.checkIdState != null) {
            this.mCheckedIdStates = savedState.checkIdState;
        }
        this.mCheckedItemCount = savedState.checkedItemCount;
        if (savedState.inActionMode && this.mChoiceMode == 3 && this.mMultiChoiceModeCallback != null) {
            this.mChoiceActionMode = startActionMode(this.mMultiChoiceModeCallback);
        }
        requestLayout();
    }

    private boolean acceptFilter() {
        return this.mTextFilterEnabled && (getAdapter() instanceof Filterable) && ((Filterable) getAdapter()).getFilter() != null;
    }

    public void setFilterText(String str) {
        if (this.mTextFilterEnabled && !TextUtils.isEmpty(str)) {
            createTextFilter(false);
            this.mTextFilter.setText(str);
            this.mTextFilter.setSelection(str.length());
            if (this.mAdapter instanceof Filterable) {
                if (this.mPopup == null) {
                    ((Filterable) this.mAdapter).getFilter().filter(str);
                }
                this.mFiltered = true;
                this.mDataSetObserver.clearSavedState();
            }
        }
    }

    public CharSequence getTextFilter() {
        if (this.mTextFilterEnabled && this.mTextFilter != null) {
            return this.mTextFilter.getText();
        }
        return null;
    }

    @Override
    protected void onFocusChanged(boolean z, int i, Rect rect) {
        super.onFocusChanged(z, i, rect);
        if (z && this.mSelectedPosition < 0 && !isInTouchMode()) {
            if (!isAttachedToWindow() && this.mAdapter != null) {
                this.mDataChanged = true;
                this.mOldItemCount = this.mItemCount;
                this.mItemCount = this.mAdapter.getCount();
            }
            resurrectSelection();
        }
    }

    @Override
    public void requestLayout() {
        if (!this.mBlockLayoutRequests && !this.mInLayout) {
            super.requestLayout();
        }
    }

    void resetList() {
        removeAllViewsInLayout();
        this.mFirstPosition = 0;
        this.mDataChanged = false;
        this.mPositionScrollAfterLayout = null;
        this.mNeedSync = false;
        this.mPendingSync = null;
        this.mOldSelectedPosition = -1;
        this.mOldSelectedRowId = Long.MIN_VALUE;
        setSelectedPositionInt(-1);
        setNextSelectedPositionInt(-1);
        this.mSelectedTop = 0;
        this.mSelectorPosition = -1;
        this.mSelectorRect.setEmpty();
        invalidate();
    }

    @Override
    protected int computeVerticalScrollExtent() {
        int childCount = getChildCount();
        if (childCount <= 0) {
            return 0;
        }
        if (!this.mSmoothScrollbarEnabled) {
            return 1;
        }
        int i = childCount * 100;
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
        int i = this.mFirstPosition;
        int childCount = getChildCount();
        int i2 = 0;
        if (i >= 0 && childCount > 0) {
            if (this.mSmoothScrollbarEnabled) {
                View childAt = getChildAt(0);
                int top = childAt.getTop();
                int height = childAt.getHeight();
                if (height > 0) {
                    return Math.max(((i * 100) - ((top * 100) / height)) + ((int) ((this.mScrollY / getHeight()) * this.mItemCount * 100.0f)), 0);
                }
            } else {
                int i3 = this.mItemCount;
                if (i != 0) {
                    if (i + childCount != i3) {
                        i2 = (childCount / 2) + i;
                    } else {
                        i2 = i3;
                    }
                }
                return (int) (i + (childCount * (i2 / i3)));
            }
        }
        return 0;
    }

    @Override
    protected int computeVerticalScrollRange() {
        if (this.mSmoothScrollbarEnabled) {
            int iMax = Math.max(this.mItemCount * 100, 0);
            if (this.mScrollY != 0) {
                return iMax + Math.abs((int) ((this.mScrollY / getHeight()) * this.mItemCount * 100.0f));
            }
            return iMax;
        }
        return this.mItemCount;
    }

    @Override
    protected float getTopFadingEdgeStrength() {
        int childCount = getChildCount();
        float topFadingEdgeStrength = super.getTopFadingEdgeStrength();
        if (childCount == 0) {
            return topFadingEdgeStrength;
        }
        if (this.mFirstPosition > 0) {
            return 1.0f;
        }
        return getChildAt(0).getTop() < this.mPaddingTop ? (-(r0 - this.mPaddingTop)) / getVerticalFadingEdgeLength() : topFadingEdgeStrength;
    }

    @Override
    protected float getBottomFadingEdgeStrength() {
        int childCount = getChildCount();
        float bottomFadingEdgeStrength = super.getBottomFadingEdgeStrength();
        if (childCount == 0) {
            return bottomFadingEdgeStrength;
        }
        if ((this.mFirstPosition + childCount) - 1 < this.mItemCount - 1) {
            return 1.0f;
        }
        int bottom = getChildAt(childCount - 1).getBottom();
        int height = getHeight();
        float verticalFadingEdgeLength = getVerticalFadingEdgeLength();
        if (bottom <= height - this.mPaddingBottom) {
            return bottomFadingEdgeStrength;
        }
        return ((bottom - height) + this.mPaddingBottom) / verticalFadingEdgeLength;
    }

    @Override
    protected void onMeasure(int i, int i2) {
        if (this.mSelector == null) {
            useDefaultSelector();
        }
        Rect rect = this.mListPadding;
        rect.left = this.mSelectionLeftPadding + this.mPaddingLeft;
        rect.top = this.mSelectionTopPadding + this.mPaddingTop;
        rect.right = this.mSelectionRightPadding + this.mPaddingRight;
        rect.bottom = this.mSelectionBottomPadding + this.mPaddingBottom;
        if (this.mTranscriptMode == 1) {
            int childCount = getChildCount();
            int height = getHeight() - getPaddingBottom();
            View childAt = getChildAt(childCount - 1);
            this.mForceTranscriptScroll = this.mFirstPosition + childCount >= this.mLastHandledItemCount && (childAt != null ? childAt.getBottom() : height) <= height;
        }
    }

    @Override
    protected void onLayout(boolean z, int i, int i2, int i3, int i4) {
        super.onLayout(z, i, i2, i3, i4);
        this.mInLayout = true;
        int childCount = getChildCount();
        if (z) {
            for (int i5 = 0; i5 < childCount; i5++) {
                getChildAt(i5).forceLayout();
            }
            this.mRecycler.markChildrenDirty();
        }
        layoutChildren();
        this.mOverscrollMax = (i4 - i2) / 3;
        if (this.mFastScroll != null) {
            this.mFastScroll.onItemCountChanged(getChildCount(), this.mItemCount);
        }
        this.mInLayout = false;
    }

    @Override
    protected boolean setFrame(int i, int i2, int i3, int i4) {
        boolean frame = super.setFrame(i, i2, i3, i4);
        if (frame) {
            boolean z = getWindowVisibility() == 0;
            if (this.mFiltered && z && this.mPopup != null && this.mPopup.isShowing()) {
                positionPopup();
            }
        }
        return frame;
    }

    protected void layoutChildren() {
    }

    View getAccessibilityFocusedChild(View view) {
        boolean z;
        ViewParent parent = view.getParent();
        while (true) {
            z = parent instanceof View;
            if (!z || parent == this) {
                break;
            }
            view = parent;
            parent = parent.getParent();
        }
        if (!z) {
            return null;
        }
        return view;
    }

    void updateScrollIndicators() {
        if (this.mScrollUp != null) {
            this.mScrollUp.setVisibility(canScrollUp() ? 0 : 4);
        }
        if (this.mScrollDown != null) {
            this.mScrollDown.setVisibility(canScrollDown() ? 0 : 4);
        }
    }

    private boolean canScrollUp() {
        boolean z = this.mFirstPosition > 0;
        return (z || getChildCount() <= 0) ? z : getChildAt(0).getTop() < this.mListPadding.top;
    }

    private boolean canScrollDown() {
        int childCount = getChildCount();
        boolean z = this.mFirstPosition + childCount < this.mItemCount;
        return (z || childCount <= 0) ? z : getChildAt(childCount - 1).getBottom() > this.mBottom - this.mListPadding.bottom;
    }

    @Override
    @ViewDebug.ExportedProperty
    public View getSelectedView() {
        if (this.mItemCount > 0 && this.mSelectedPosition >= 0) {
            return getChildAt(this.mSelectedPosition - this.mFirstPosition);
        }
        return null;
    }

    public int getListPaddingTop() {
        return this.mListPadding.top;
    }

    public int getListPaddingBottom() {
        return this.mListPadding.bottom;
    }

    public int getListPaddingLeft() {
        return this.mListPadding.left;
    }

    public int getListPaddingRight() {
        return this.mListPadding.right;
    }

    View obtainView(int i, boolean[] zArr) {
        View view;
        Trace.traceBegin(8L, "obtainView");
        zArr[0] = false;
        View transientStateView = this.mRecycler.getTransientStateView(i);
        if (transientStateView != null) {
            if (((LayoutParams) transientStateView.getLayoutParams()).viewType == this.mAdapter.getItemViewType(i) && (view = this.mAdapter.getView(i, transientStateView, this)) != transientStateView) {
                setItemViewLayoutParams(view, i);
                this.mRecycler.addScrapView(view, i);
            }
            zArr[0] = true;
            transientStateView.dispatchFinishTemporaryDetach();
            return transientStateView;
        }
        View scrapView = this.mRecycler.getScrapView(i);
        View view2 = this.mAdapter.getView(i, scrapView, this);
        if (scrapView != null) {
            if (view2 != scrapView) {
                this.mRecycler.addScrapView(scrapView, i);
            } else if (view2.isTemporarilyDetached()) {
                zArr[0] = true;
                view2.dispatchFinishTemporaryDetach();
            }
        }
        if (this.mCacheColorHint != 0) {
            view2.setDrawingCacheBackgroundColor(this.mCacheColorHint);
        }
        if (view2.getImportantForAccessibility() == 0) {
            view2.setImportantForAccessibility(1);
        }
        setItemViewLayoutParams(view2, i);
        if (AccessibilityManager.getInstance(this.mContext).isEnabled()) {
            if (this.mAccessibilityDelegate == null) {
                this.mAccessibilityDelegate = new ListItemAccessibilityDelegate();
            }
            if (view2.getAccessibilityDelegate() == null) {
                view2.setAccessibilityDelegate(this.mAccessibilityDelegate);
            }
        }
        Trace.traceEnd(8L);
        return view2;
    }

    private void setItemViewLayoutParams(View view, int i) {
        LayoutParams layoutParams;
        ViewGroup.LayoutParams layoutParams2 = view.getLayoutParams();
        if (layoutParams2 == null) {
            layoutParams = (LayoutParams) generateDefaultLayoutParams();
        } else if (!checkLayoutParams(layoutParams2)) {
            layoutParams = (LayoutParams) generateLayoutParams(layoutParams2);
        } else {
            layoutParams = (LayoutParams) layoutParams2;
        }
        if (this.mAdapterHasStableIds) {
            layoutParams.itemId = this.mAdapter.getItemId(i);
        }
        layoutParams.viewType = this.mAdapter.getItemViewType(i);
        layoutParams.isEnabled = this.mAdapter.isEnabled(i);
        if (layoutParams != layoutParams2) {
            view.setLayoutParams(layoutParams);
        }
    }

    class ListItemAccessibilityDelegate extends View.AccessibilityDelegate {
        ListItemAccessibilityDelegate() {
        }

        @Override
        public void onInitializeAccessibilityNodeInfo(View view, AccessibilityNodeInfo accessibilityNodeInfo) {
            super.onInitializeAccessibilityNodeInfo(view, accessibilityNodeInfo);
            AbsListView.this.onInitializeAccessibilityNodeInfoForItem(view, AbsListView.this.getPositionForView(view), accessibilityNodeInfo);
        }

        @Override
        public boolean performAccessibilityAction(View view, int i, Bundle bundle) {
            boolean z;
            if (super.performAccessibilityAction(view, i, bundle)) {
                return true;
            }
            int positionForView = AbsListView.this.getPositionForView(view);
            if (positionForView == -1 || AbsListView.this.mAdapter == null || positionForView >= AbsListView.this.mAdapter.getCount()) {
                return false;
            }
            ViewGroup.LayoutParams layoutParams = view.getLayoutParams();
            if (layoutParams instanceof LayoutParams) {
                z = ((LayoutParams) layoutParams).isEnabled;
            } else {
                z = false;
            }
            if (!AbsListView.this.isEnabled() || !z) {
                return false;
            }
            if (i == 4) {
                if (AbsListView.this.getSelectedItemPosition() == positionForView) {
                    return false;
                }
                AbsListView.this.setSelection(positionForView);
                return true;
            }
            if (i == 8) {
                if (AbsListView.this.getSelectedItemPosition() != positionForView) {
                    return false;
                }
                AbsListView.this.setSelection(-1);
                return true;
            }
            if (i == 16) {
                if (!AbsListView.this.isItemClickable(view)) {
                    return false;
                }
                return AbsListView.this.performItemClick(view, positionForView, AbsListView.this.getItemIdAtPosition(positionForView));
            }
            if (i != 32 || !AbsListView.this.isLongClickable()) {
                return false;
            }
            return AbsListView.this.performLongPress(view, positionForView, AbsListView.this.getItemIdAtPosition(positionForView));
        }
    }

    public void onInitializeAccessibilityNodeInfoForItem(View view, int i, AccessibilityNodeInfo accessibilityNodeInfo) {
        boolean z;
        if (i == -1) {
            return;
        }
        ViewGroup.LayoutParams layoutParams = view.getLayoutParams();
        if (layoutParams instanceof LayoutParams) {
            z = ((LayoutParams) layoutParams).isEnabled;
        } else {
            z = false;
        }
        if (!isEnabled() || !z) {
            accessibilityNodeInfo.setEnabled(false);
            return;
        }
        if (i == getSelectedItemPosition()) {
            accessibilityNodeInfo.setSelected(true);
            accessibilityNodeInfo.addAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_CLEAR_SELECTION);
        } else {
            accessibilityNodeInfo.addAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_SELECT);
        }
        if (isItemClickable(view)) {
            accessibilityNodeInfo.addAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_CLICK);
            accessibilityNodeInfo.setClickable(true);
        }
        if (isLongClickable()) {
            accessibilityNodeInfo.addAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_LONG_CLICK);
            accessibilityNodeInfo.setLongClickable(true);
        }
    }

    private boolean isItemClickable(View view) {
        return !view.hasExplicitFocusable();
    }

    void positionSelectorLikeTouch(int i, View view, float f, float f2) {
        positionSelector(i, view, true, f, f2);
    }

    void positionSelectorLikeFocus(int i, View view) {
        if (this.mSelector != null && this.mSelectorPosition != i && i != -1) {
            Rect rect = this.mSelectorRect;
            positionSelector(i, view, true, rect.exactCenterX(), rect.exactCenterY());
        } else {
            positionSelector(i, view);
        }
    }

    void positionSelector(int i, View view) {
        positionSelector(i, view, false, -1.0f, -1.0f);
    }

    private void positionSelector(int i, View view, boolean z, float f, float f2) {
        boolean z2 = i != this.mSelectorPosition;
        if (i != -1) {
            this.mSelectorPosition = i;
        }
        Rect rect = this.mSelectorRect;
        rect.set(view.getLeft(), view.getTop(), view.getRight(), view.getBottom());
        if (view instanceof SelectionBoundsAdjuster) {
            ((SelectionBoundsAdjuster) view).adjustListItemSelectionBounds(rect);
        }
        rect.left -= this.mSelectionLeftPadding;
        rect.top -= this.mSelectionTopPadding;
        rect.right += this.mSelectionRightPadding;
        rect.bottom += this.mSelectionBottomPadding;
        boolean zIsEnabled = view.isEnabled();
        if (this.mIsChildViewEnabled != zIsEnabled) {
            this.mIsChildViewEnabled = zIsEnabled;
        }
        Drawable drawable = this.mSelector;
        if (drawable != null) {
            if (z2) {
                drawable.setVisible(false, false);
                drawable.setState(StateSet.NOTHING);
            }
            drawable.setBounds(rect);
            if (z2) {
                if (getVisibility() == 0) {
                    drawable.setVisible(true, false);
                }
                updateSelectorState();
            }
            if (z) {
                drawable.setHotspot(f, f2);
            }
        }
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        int iSave = 0;
        boolean z = (this.mGroupFlags & 34) == 34;
        if (z) {
            iSave = canvas.save();
            int i = this.mScrollX;
            int i2 = this.mScrollY;
            canvas.clipRect(this.mPaddingLeft + i, this.mPaddingTop + i2, ((i + this.mRight) - this.mLeft) - this.mPaddingRight, ((i2 + this.mBottom) - this.mTop) - this.mPaddingBottom);
            this.mGroupFlags &= -35;
        }
        boolean z2 = this.mDrawSelectorOnTop;
        if (!z2) {
            drawSelector(canvas);
        }
        super.dispatchDraw(canvas);
        if (z2) {
            drawSelector(canvas);
        }
        if (z) {
            canvas.restoreToCount(iSave);
            this.mGroupFlags |= 34;
        }
    }

    @Override
    protected boolean isPaddingOffsetRequired() {
        return (this.mGroupFlags & 34) != 34;
    }

    @Override
    protected int getLeftPaddingOffset() {
        if ((this.mGroupFlags & 34) == 34) {
            return 0;
        }
        return -this.mPaddingLeft;
    }

    @Override
    protected int getTopPaddingOffset() {
        if ((this.mGroupFlags & 34) == 34) {
            return 0;
        }
        return -this.mPaddingTop;
    }

    @Override
    protected int getRightPaddingOffset() {
        if ((this.mGroupFlags & 34) == 34) {
            return 0;
        }
        return this.mPaddingRight;
    }

    @Override
    protected int getBottomPaddingOffset() {
        if ((this.mGroupFlags & 34) == 34) {
            return 0;
        }
        return this.mPaddingBottom;
    }

    @Override
    protected void internalSetPadding(int i, int i2, int i3, int i4) {
        super.internalSetPadding(i, i2, i3, i4);
        if (isLayoutRequested()) {
            handleBoundsChange();
        }
    }

    @Override
    protected void onSizeChanged(int i, int i2, int i3, int i4) {
        handleBoundsChange();
        if (this.mFastScroll != null) {
            this.mFastScroll.onSizeChanged(i, i2, i3, i4);
        }
    }

    void handleBoundsChange() {
        int childCount;
        if (!this.mInLayout && (childCount = getChildCount()) > 0) {
            this.mDataChanged = true;
            rememberSyncState();
            for (int i = 0; i < childCount; i++) {
                View childAt = getChildAt(i);
                ViewGroup.LayoutParams layoutParams = childAt.getLayoutParams();
                if (layoutParams == null || layoutParams.width < 1 || layoutParams.height < 1) {
                    childAt.forceLayout();
                }
            }
        }
    }

    boolean touchModeDrawsInPressedState() {
        switch (this.mTouchMode) {
            case 1:
            case 2:
                return true;
            default:
                return false;
        }
    }

    boolean shouldShowSelector() {
        return (isFocused() && !isInTouchMode()) || (touchModeDrawsInPressedState() && isPressed());
    }

    private void drawSelector(Canvas canvas) {
        if (shouldDrawSelector()) {
            Drawable drawable = this.mSelector;
            drawable.setBounds(this.mSelectorRect);
            drawable.draw(canvas);
        }
    }

    public final boolean shouldDrawSelector() {
        return !this.mSelectorRect.isEmpty();
    }

    public void setDrawSelectorOnTop(boolean z) {
        this.mDrawSelectorOnTop = z;
    }

    public void setSelector(int i) {
        setSelector(getContext().getDrawable(i));
    }

    public void setSelector(Drawable drawable) {
        if (this.mSelector != null) {
            this.mSelector.setCallback(null);
            unscheduleDrawable(this.mSelector);
        }
        this.mSelector = drawable;
        Rect rect = new Rect();
        drawable.getPadding(rect);
        this.mSelectionLeftPadding = rect.left;
        this.mSelectionTopPadding = rect.top;
        this.mSelectionRightPadding = rect.right;
        this.mSelectionBottomPadding = rect.bottom;
        drawable.setCallback(this);
        updateSelectorState();
    }

    public Drawable getSelector() {
        return this.mSelector;
    }

    void keyPressed() {
        if (!isEnabled() || !isClickable()) {
            return;
        }
        Drawable drawable = this.mSelector;
        Rect rect = this.mSelectorRect;
        if (drawable != null) {
            if ((isFocused() || touchModeDrawsInPressedState()) && !rect.isEmpty()) {
                View childAt = getChildAt(this.mSelectedPosition - this.mFirstPosition);
                if (childAt != null) {
                    if (childAt.hasExplicitFocusable()) {
                        return;
                    } else {
                        childAt.setPressed(true);
                    }
                }
                setPressed(true);
                boolean zIsLongClickable = isLongClickable();
                Drawable current = drawable.getCurrent();
                if (current != null && (current instanceof TransitionDrawable)) {
                    if (zIsLongClickable) {
                        ((TransitionDrawable) current).startTransition(ViewConfiguration.getLongPressTimeout());
                    } else {
                        ((TransitionDrawable) current).resetTransition();
                    }
                }
                if (zIsLongClickable && !this.mDataChanged) {
                    if (this.mPendingCheckForKeyLongPress == null) {
                        this.mPendingCheckForKeyLongPress = new CheckForKeyLongPress();
                    }
                    this.mPendingCheckForKeyLongPress.rememberWindowAttachCount();
                    postDelayed(this.mPendingCheckForKeyLongPress, ViewConfiguration.getLongPressTimeout());
                }
            }
        }
    }

    public void setScrollIndicators(View view, View view2) {
        this.mScrollUp = view;
        this.mScrollDown = view2;
    }

    void updateSelectorState() {
        Drawable drawable = this.mSelector;
        if (drawable != null && drawable.isStateful()) {
            if (shouldShowSelector()) {
                if (drawable.setState(getDrawableStateForSelector())) {
                    invalidateDrawable(drawable);
                    return;
                }
                return;
            }
            drawable.setState(StateSet.NOTHING);
        }
    }

    @Override
    protected void drawableStateChanged() {
        super.drawableStateChanged();
        updateSelectorState();
    }

    private int[] getDrawableStateForSelector() {
        if (this.mIsChildViewEnabled) {
            return super.getDrawableState();
        }
        int i = ENABLED_STATE_SET[0];
        int[] iArrOnCreateDrawableState = onCreateDrawableState(1);
        int length = iArrOnCreateDrawableState.length - 1;
        while (true) {
            if (length >= 0) {
                if (iArrOnCreateDrawableState[length] == i) {
                    break;
                }
                length--;
            } else {
                length = -1;
                break;
            }
        }
        if (length >= 0) {
            System.arraycopy(iArrOnCreateDrawableState, length + 1, iArrOnCreateDrawableState, length, (iArrOnCreateDrawableState.length - length) - 1);
        }
        return iArrOnCreateDrawableState;
    }

    @Override
    public boolean verifyDrawable(Drawable drawable) {
        return this.mSelector == drawable || super.verifyDrawable(drawable);
    }

    @Override
    public void jumpDrawablesToCurrentState() {
        super.jumpDrawablesToCurrentState();
        if (this.mSelector != null) {
            this.mSelector.jumpToCurrentState();
        }
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        ViewTreeObserver viewTreeObserver = getViewTreeObserver();
        viewTreeObserver.addOnTouchModeChangeListener(this);
        if (this.mTextFilterEnabled && this.mPopup != null && !this.mGlobalLayoutListenerAddedFilter) {
            viewTreeObserver.addOnGlobalLayoutListener(this);
        }
        if (this.mAdapter != null && this.mDataSetObserver == null) {
            this.mDataSetObserver = new AdapterDataSetObserver();
            this.mAdapter.registerDataSetObserver(this.mDataSetObserver);
            this.mDataChanged = true;
            this.mOldItemCount = this.mItemCount;
            this.mItemCount = this.mAdapter.getCount();
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        this.mIsDetaching = true;
        dismissPopup();
        this.mRecycler.clear();
        ViewTreeObserver viewTreeObserver = getViewTreeObserver();
        viewTreeObserver.removeOnTouchModeChangeListener(this);
        if (this.mTextFilterEnabled && this.mPopup != null) {
            viewTreeObserver.removeOnGlobalLayoutListener(this);
            this.mGlobalLayoutListenerAddedFilter = false;
        }
        if (this.mAdapter != null && this.mDataSetObserver != null) {
            this.mAdapter.unregisterDataSetObserver(this.mDataSetObserver);
            this.mDataSetObserver = null;
        }
        if (this.mScrollStrictSpan != null) {
            this.mScrollStrictSpan.finish();
            this.mScrollStrictSpan = null;
        }
        if (this.mFlingStrictSpan != null) {
            this.mFlingStrictSpan.finish();
            this.mFlingStrictSpan = null;
        }
        if (this.mFlingRunnable != null) {
            removeCallbacks(this.mFlingRunnable);
        }
        if (this.mPositionScroller != null) {
            this.mPositionScroller.stop();
        }
        if (this.mClearScrollingCache != null) {
            removeCallbacks(this.mClearScrollingCache);
        }
        if (this.mPerformClick != null) {
            removeCallbacks(this.mPerformClick);
        }
        if (this.mTouchModeReset != null) {
            removeCallbacks(this.mTouchModeReset);
            this.mTouchModeReset.run();
        }
        this.mIsDetaching = false;
    }

    @Override
    public void onWindowFocusChanged(boolean z) {
        super.onWindowFocusChanged(z);
        int i = !isInTouchMode() ? 1 : 0;
        if (!z) {
            setChildrenDrawingCacheEnabled(false);
            if (this.mFlingRunnable != null) {
                removeCallbacks(this.mFlingRunnable);
                this.mFlingRunnable.mSuppressIdleStateChangeCall = false;
                this.mFlingRunnable.endFling();
                if (this.mPositionScroller != null) {
                    this.mPositionScroller.stop();
                }
                if (this.mScrollY != 0) {
                    this.mScrollY = 0;
                    invalidateParentCaches();
                    finishGlows();
                    invalidate();
                }
            }
            dismissPopup();
            if (i == 1) {
                this.mResurrectToPosition = this.mSelectedPosition;
            }
        } else {
            if (this.mFiltered && !this.mPopupHidden) {
                showPopup();
            }
            if (i != this.mLastTouchMode && this.mLastTouchMode != -1) {
                if (i == 1) {
                    resurrectSelection();
                } else {
                    hideSelector();
                    this.mLayoutMode = 0;
                    layoutChildren();
                }
            }
        }
        this.mLastTouchMode = i;
    }

    @Override
    public void onRtlPropertiesChanged(int i) {
        super.onRtlPropertiesChanged(i);
        if (this.mFastScroll != null) {
            this.mFastScroll.setScrollbarPosition(getVerticalScrollbarPosition());
        }
    }

    ContextMenu.ContextMenuInfo createContextMenuInfo(View view, int i, long j) {
        return new AdapterView.AdapterContextMenuInfo(view, i, j);
    }

    @Override
    public void onCancelPendingInputEvents() {
        super.onCancelPendingInputEvents();
        if (this.mPerformClick != null) {
            removeCallbacks(this.mPerformClick);
        }
        if (this.mPendingCheckForTap != null) {
            removeCallbacks(this.mPendingCheckForTap);
        }
        if (this.mPendingCheckForLongPress != null) {
            removeCallbacks(this.mPendingCheckForLongPress);
        }
        if (this.mPendingCheckForKeyLongPress != null) {
            removeCallbacks(this.mPendingCheckForKeyLongPress);
        }
    }

    private class WindowRunnnable {
        private int mOriginalAttachCount;

        private WindowRunnnable() {
        }

        public void rememberWindowAttachCount() {
            this.mOriginalAttachCount = AbsListView.this.getWindowAttachCount();
        }

        public boolean sameWindow() {
            return AbsListView.this.getWindowAttachCount() == this.mOriginalAttachCount;
        }
    }

    private class PerformClick extends WindowRunnnable implements Runnable {
        int mClickMotionPosition;

        private PerformClick() {
            super();
        }

        @Override
        public void run() {
            View childAt;
            if (AbsListView.this.mDataChanged) {
                return;
            }
            ListAdapter listAdapter = AbsListView.this.mAdapter;
            int i = this.mClickMotionPosition;
            if (listAdapter != null && AbsListView.this.mItemCount > 0 && i != -1 && i < listAdapter.getCount() && sameWindow() && listAdapter.isEnabled(i) && (childAt = AbsListView.this.getChildAt(i - AbsListView.this.mFirstPosition)) != null) {
                AbsListView.this.performItemClick(childAt, i, listAdapter.getItemId(i));
            }
        }
    }

    private class CheckForLongPress extends WindowRunnnable implements Runnable {
        private static final int INVALID_COORD = -1;
        private float mX;
        private float mY;

        private CheckForLongPress() {
            super();
            this.mX = -1.0f;
            this.mY = -1.0f;
        }

        private void setCoords(float f, float f2) {
            this.mX = f;
            this.mY = f2;
        }

        @Override
        public void run() {
            boolean zPerformLongPress;
            View childAt = AbsListView.this.getChildAt(AbsListView.this.mMotionPosition - AbsListView.this.mFirstPosition);
            if (childAt != null) {
                int i = AbsListView.this.mMotionPosition;
                long itemId = AbsListView.this.mAdapter.getItemId(AbsListView.this.mMotionPosition);
                if (!sameWindow() || AbsListView.this.mDataChanged) {
                    zPerformLongPress = false;
                } else if (this.mX != -1.0f && this.mY != -1.0f) {
                    zPerformLongPress = AbsListView.this.performLongPress(childAt, i, itemId, this.mX, this.mY);
                } else {
                    zPerformLongPress = AbsListView.this.performLongPress(childAt, i, itemId);
                }
                if (zPerformLongPress) {
                    AbsListView.this.mHasPerformedLongPress = true;
                    AbsListView.this.mTouchMode = -1;
                    AbsListView.this.setPressed(false);
                    childAt.setPressed(false);
                    return;
                }
                AbsListView.this.mTouchMode = 2;
            }
        }
    }

    private class CheckForKeyLongPress extends WindowRunnnable implements Runnable {
        private CheckForKeyLongPress() {
            super();
        }

        @Override
        public void run() {
            boolean zPerformLongPress;
            if (AbsListView.this.isPressed() && AbsListView.this.mSelectedPosition >= 0) {
                View childAt = AbsListView.this.getChildAt(AbsListView.this.mSelectedPosition - AbsListView.this.mFirstPosition);
                if (!AbsListView.this.mDataChanged) {
                    if (sameWindow()) {
                        zPerformLongPress = AbsListView.this.performLongPress(childAt, AbsListView.this.mSelectedPosition, AbsListView.this.mSelectedRowId);
                    } else {
                        zPerformLongPress = false;
                    }
                    if (zPerformLongPress) {
                        AbsListView.this.setPressed(false);
                        childAt.setPressed(false);
                        return;
                    }
                    return;
                }
                AbsListView.this.setPressed(false);
                if (childAt != null) {
                    childAt.setPressed(false);
                }
            }
        }
    }

    private boolean performStylusButtonPressAction(MotionEvent motionEvent) {
        View childAt;
        if (this.mChoiceMode != 3 || this.mChoiceActionMode != null || (childAt = getChildAt(this.mMotionPosition - this.mFirstPosition)) == null || !performLongPress(childAt, this.mMotionPosition, this.mAdapter.getItemId(this.mMotionPosition))) {
            return false;
        }
        this.mTouchMode = -1;
        setPressed(false);
        childAt.setPressed(false);
        return true;
    }

    boolean performLongPress(View view, int i, long j) {
        return performLongPress(view, i, j, -1.0f, -1.0f);
    }

    boolean performLongPress(View view, int i, long j, float f, float f2) {
        boolean zShowContextMenuForChild;
        if (this.mChoiceMode == 3) {
            if (this.mChoiceActionMode == null) {
                ActionMode actionModeStartActionMode = startActionMode(this.mMultiChoiceModeCallback);
                this.mChoiceActionMode = actionModeStartActionMode;
                if (actionModeStartActionMode != null) {
                    setItemChecked(i, true);
                    performHapticFeedback(0);
                }
            }
            return true;
        }
        if (this.mOnItemLongClickListener != null) {
            zShowContextMenuForChild = this.mOnItemLongClickListener.onItemLongClick(this, view, i, j);
        } else {
            zShowContextMenuForChild = false;
        }
        if (!zShowContextMenuForChild) {
            this.mContextMenuInfo = createContextMenuInfo(view, i, j);
            if (f != -1.0f && f2 != -1.0f) {
                zShowContextMenuForChild = super.showContextMenuForChild(this, f, f2);
            } else {
                zShowContextMenuForChild = super.showContextMenuForChild(this);
            }
        }
        if (zShowContextMenuForChild) {
            performHapticFeedback(0);
        }
        return zShowContextMenuForChild;
    }

    @Override
    protected ContextMenu.ContextMenuInfo getContextMenuInfo() {
        return this.mContextMenuInfo;
    }

    @Override
    public boolean showContextMenu() {
        return showContextMenuInternal(0.0f, 0.0f, false);
    }

    @Override
    public boolean showContextMenu(float f, float f2) {
        return showContextMenuInternal(f, f2, true);
    }

    private boolean showContextMenuInternal(float f, float f2, boolean z) {
        int iPointToPosition = pointToPosition((int) f, (int) f2);
        if (iPointToPosition != -1) {
            long itemId = this.mAdapter.getItemId(iPointToPosition);
            View childAt = getChildAt(iPointToPosition - this.mFirstPosition);
            if (childAt != null) {
                this.mContextMenuInfo = createContextMenuInfo(childAt, iPointToPosition, itemId);
                if (z) {
                    return super.showContextMenuForChild(this, f, f2);
                }
                return super.showContextMenuForChild(this);
            }
        }
        if (z) {
            return super.showContextMenu(f, f2);
        }
        return super.showContextMenu();
    }

    @Override
    public boolean showContextMenuForChild(View view) {
        if (isShowingContextMenuWithCoords()) {
            return false;
        }
        return showContextMenuForChildInternal(view, 0.0f, 0.0f, false);
    }

    @Override
    public boolean showContextMenuForChild(View view, float f, float f2) {
        return showContextMenuForChildInternal(view, f, f2, true);
    }

    private boolean showContextMenuForChildInternal(View view, float f, float f2, boolean z) {
        int positionForView = getPositionForView(view);
        boolean zOnItemLongClick = false;
        if (positionForView < 0) {
            return false;
        }
        long itemId = this.mAdapter.getItemId(positionForView);
        if (this.mOnItemLongClickListener != null) {
            zOnItemLongClick = this.mOnItemLongClickListener.onItemLongClick(this, view, positionForView, itemId);
        }
        if (!zOnItemLongClick) {
            this.mContextMenuInfo = createContextMenuInfo(getChildAt(positionForView - this.mFirstPosition), positionForView, itemId);
            if (z) {
                return super.showContextMenuForChild(view, f, f2);
            }
            return super.showContextMenuForChild(view);
        }
        return zOnItemLongClick;
    }

    @Override
    public boolean onKeyDown(int i, KeyEvent keyEvent) {
        return false;
    }

    @Override
    public boolean onKeyUp(int i, KeyEvent keyEvent) {
        if (KeyEvent.isConfirmKey(i)) {
            if (!isEnabled()) {
                return true;
            }
            if (isClickable() && isPressed() && this.mSelectedPosition >= 0 && this.mAdapter != null && this.mSelectedPosition < this.mAdapter.getCount()) {
                View childAt = getChildAt(this.mSelectedPosition - this.mFirstPosition);
                if (childAt != null) {
                    performItemClick(childAt, this.mSelectedPosition, this.mSelectedRowId);
                    childAt.setPressed(false);
                }
                setPressed(false);
                return true;
            }
        }
        return super.onKeyUp(i, keyEvent);
    }

    @Override
    protected void dispatchSetPressed(boolean z) {
    }

    @Override
    public void dispatchDrawableHotspotChanged(float f, float f2) {
    }

    public int pointToPosition(int i, int i2) {
        Rect rect = this.mTouchFrame;
        if (rect == null) {
            this.mTouchFrame = new Rect();
            rect = this.mTouchFrame;
        }
        for (int childCount = getChildCount() - 1; childCount >= 0; childCount--) {
            View childAt = getChildAt(childCount);
            if (childAt.getVisibility() == 0) {
                childAt.getHitRect(rect);
                if (rect.contains(i, i2)) {
                    return this.mFirstPosition + childCount;
                }
            }
        }
        return -1;
    }

    public long pointToRowId(int i, int i2) {
        int iPointToPosition = pointToPosition(i, i2);
        if (iPointToPosition >= 0) {
            return this.mAdapter.getItemId(iPointToPosition);
        }
        return Long.MIN_VALUE;
    }

    private final class CheckForTap implements Runnable {
        float x;
        float y;

        private CheckForTap() {
        }

        @Override
        public void run() {
            if (AbsListView.this.mTouchMode == 0) {
                AbsListView.this.mTouchMode = 1;
                View childAt = AbsListView.this.getChildAt(AbsListView.this.mMotionPosition - AbsListView.this.mFirstPosition);
                if (childAt != null && !childAt.hasExplicitFocusable()) {
                    AbsListView.this.mLayoutMode = 0;
                    if (!AbsListView.this.mDataChanged) {
                        float[] fArr = AbsListView.this.mTmpPoint;
                        fArr[0] = this.x;
                        fArr[1] = this.y;
                        AbsListView.this.transformPointToViewLocal(fArr, childAt);
                        childAt.drawableHotspotChanged(fArr[0], fArr[1]);
                        childAt.setPressed(true);
                        AbsListView.this.setPressed(true);
                        AbsListView.this.layoutChildren();
                        AbsListView.this.positionSelector(AbsListView.this.mMotionPosition, childAt);
                        AbsListView.this.refreshDrawableState();
                        int longPressTimeout = ViewConfiguration.getLongPressTimeout();
                        boolean zIsLongClickable = AbsListView.this.isLongClickable();
                        if (AbsListView.this.mSelector != null) {
                            Drawable current = AbsListView.this.mSelector.getCurrent();
                            if (current != null && (current instanceof TransitionDrawable)) {
                                if (zIsLongClickable) {
                                    ((TransitionDrawable) current).startTransition(longPressTimeout);
                                } else {
                                    ((TransitionDrawable) current).resetTransition();
                                }
                            }
                            AbsListView.this.mSelector.setHotspot(this.x, this.y);
                        }
                        if (zIsLongClickable) {
                            if (AbsListView.this.mPendingCheckForLongPress == null) {
                                AbsListView.this.mPendingCheckForLongPress = new CheckForLongPress();
                            }
                            AbsListView.this.mPendingCheckForLongPress.setCoords(this.x, this.y);
                            AbsListView.this.mPendingCheckForLongPress.rememberWindowAttachCount();
                            AbsListView.this.postDelayed(AbsListView.this.mPendingCheckForLongPress, longPressTimeout);
                            return;
                        }
                        AbsListView.this.mTouchMode = 2;
                        return;
                    }
                    AbsListView.this.mTouchMode = 2;
                }
            }
        }
    }

    private boolean startScrollIfNeeded(int i, int i2, MotionEvent motionEvent) {
        int i3 = i2 - this.mMotionY;
        int iAbs = Math.abs(i3);
        boolean z = this.mScrollY != 0;
        if ((!z && iAbs <= this.mTouchSlop) || (getNestedScrollAxes() & 2) != 0) {
            return false;
        }
        createScrollingCache();
        if (z) {
            this.mTouchMode = 5;
            this.mMotionCorrection = 0;
        } else {
            this.mTouchMode = 3;
            this.mMotionCorrection = i3 > 0 ? this.mTouchSlop : -this.mTouchSlop;
        }
        removeCallbacks(this.mPendingCheckForLongPress);
        setPressed(false);
        View childAt = getChildAt(this.mMotionPosition - this.mFirstPosition);
        if (childAt != null) {
            childAt.setPressed(false);
        }
        reportScrollStateChange(1);
        ViewParent parent = getParent();
        if (parent != null) {
            parent.requestDisallowInterceptTouchEvent(true);
        }
        scrollIfNeeded(i, i2, motionEvent);
        return true;
    }

    private void scrollIfNeeded(int i, int i2, MotionEvent motionEvent) {
        int i3;
        int i4;
        int i5;
        int i6;
        int i7;
        int i8;
        int i9;
        int childCount;
        int top;
        boolean zTrackMotionScroll;
        ViewParent parent;
        int i10 = i2 - this.mMotionY;
        if (this.mLastY == Integer.MIN_VALUE) {
            i10 -= this.mMotionCorrection;
        }
        int i11 = 0;
        if (dispatchNestedPreScroll(0, this.mLastY != Integer.MIN_VALUE ? this.mLastY - i2 : -i10, this.mScrollConsumed, this.mScrollOffset)) {
            int i12 = i10 + this.mScrollConsumed[1];
            int i13 = -this.mScrollOffset[1];
            i4 = this.mScrollConsumed[1];
            if (motionEvent != null) {
                motionEvent.offsetLocation(0.0f, this.mScrollOffset[1]);
                this.mNestedYOffset += this.mScrollOffset[1];
            }
            i3 = i12;
            i5 = i13;
        } else {
            i3 = i10;
            i4 = 0;
            i5 = 0;
        }
        int i14 = this.mLastY != Integer.MIN_VALUE ? (i2 - this.mLastY) + i4 : i3;
        if (this.mTouchMode == 3) {
            if (this.mScrollStrictSpan == null) {
                this.mScrollStrictSpan = StrictMode.enterCriticalSpan("AbsListView-scroll");
            }
            if (i2 != this.mLastY) {
                if ((this.mGroupFlags & 524288) == 0 && Math.abs(i3) > this.mTouchSlop && (parent = getParent()) != null) {
                    parent.requestDisallowInterceptTouchEvent(true);
                }
                if (this.mMotionPosition >= 0) {
                    childCount = this.mMotionPosition - this.mFirstPosition;
                } else {
                    childCount = getChildCount() / 2;
                }
                View childAt = getChildAt(childCount);
                if (childAt != null) {
                    top = childAt.getTop();
                } else {
                    top = 0;
                }
                if (i14 != 0) {
                    zTrackMotionScroll = trackMotionScroll(i3, i14);
                } else {
                    zTrackMotionScroll = false;
                }
                View childAt2 = getChildAt(childCount);
                if (childAt2 != null) {
                    int top2 = childAt2.getTop();
                    if (zTrackMotionScroll) {
                        int i15 = (-i14) - (top2 - top);
                        if (dispatchNestedScroll(0, i15 - i14, 0, i15, this.mScrollOffset)) {
                            i11 = 0 - this.mScrollOffset[1];
                            if (motionEvent != null) {
                                motionEvent.offsetLocation(0.0f, this.mScrollOffset[1]);
                                this.mNestedYOffset += this.mScrollOffset[1];
                            }
                        } else {
                            int i16 = i14;
                            boolean zOverScrollBy = overScrollBy(0, i15, 0, this.mScrollY, 0, 0, 0, this.mOverscrollDistance, true);
                            if (zOverScrollBy && this.mVelocityTracker != null) {
                                this.mVelocityTracker.clear();
                            }
                            int overScrollMode = getOverScrollMode();
                            if (overScrollMode == 0 || (overScrollMode == 1 && !contentFits())) {
                                if (!zOverScrollBy) {
                                    this.mDirection = 0;
                                    this.mTouchMode = 5;
                                }
                                if (i16 > 0) {
                                    this.mEdgeGlowTop.onPull((-i15) / getHeight(), i / getWidth());
                                    if (!this.mEdgeGlowBottom.isFinished()) {
                                        this.mEdgeGlowBottom.onRelease();
                                    }
                                    invalidateTopGlow();
                                } else if (i16 < 0) {
                                    this.mEdgeGlowBottom.onPull(i15 / getHeight(), 1.0f - (i / getWidth()));
                                    if (!this.mEdgeGlowTop.isFinished()) {
                                        this.mEdgeGlowTop.onRelease();
                                    }
                                    invalidateBottomGlow();
                                }
                            }
                        }
                    }
                    this.mMotionY = i2 + i11 + i5;
                }
                this.mLastY = i2 + i11 + i5;
                return;
            }
            return;
        }
        int i17 = i14;
        if (this.mTouchMode == 5 && i2 != this.mLastY) {
            int i18 = this.mScrollY;
            int i19 = i18 - i17;
            int i20 = i2 > this.mLastY ? 1 : -1;
            if (this.mDirection == 0) {
                this.mDirection = i20;
            }
            int i21 = -i17;
            if ((i19 < 0 && i18 >= 0) || (i19 > 0 && i18 <= 0)) {
                int i22 = -i18;
                i6 = i22;
                i7 = i17 + i22;
            } else {
                i6 = i21;
                i7 = 0;
            }
            if (i6 != 0) {
                i8 = i7;
                int i23 = i6;
                i9 = i20;
                overScrollBy(0, i6, 0, this.mScrollY, 0, 0, 0, this.mOverscrollDistance, true);
                int overScrollMode2 = getOverScrollMode();
                if (overScrollMode2 == 0 || (overScrollMode2 == 1 && !contentFits())) {
                    if (i3 > 0) {
                        this.mEdgeGlowTop.onPull(i23 / getHeight(), i / getWidth());
                        if (!this.mEdgeGlowBottom.isFinished()) {
                            this.mEdgeGlowBottom.onRelease();
                        }
                        invalidateTopGlow();
                    } else if (i3 < 0) {
                        this.mEdgeGlowBottom.onPull(i23 / getHeight(), 1.0f - (i / getWidth()));
                        if (!this.mEdgeGlowTop.isFinished()) {
                            this.mEdgeGlowTop.onRelease();
                        }
                        invalidateBottomGlow();
                    }
                }
            } else {
                i8 = i7;
                i9 = i20;
            }
            if (i8 != 0) {
                if (this.mScrollY != 0) {
                    this.mScrollY = 0;
                    invalidateParentIfNeeded();
                }
                trackMotionScroll(i8, i8);
                this.mTouchMode = 3;
                int iFindClosestMotionRow = findClosestMotionRow(i2);
                this.mMotionCorrection = 0;
                View childAt3 = getChildAt(iFindClosestMotionRow - this.mFirstPosition);
                this.mMotionViewOriginalTop = childAt3 != null ? childAt3.getTop() : 0;
                this.mMotionY = i2 + i5;
                this.mMotionPosition = iFindClosestMotionRow;
            }
            this.mLastY = 0 + i2 + i5;
            this.mDirection = i9;
        }
    }

    private void invalidateTopGlow() {
        if (this.mEdgeGlowTop == null) {
            return;
        }
        boolean clipToPadding = getClipToPadding();
        int i = clipToPadding ? this.mPaddingTop : 0;
        invalidate(clipToPadding ? this.mPaddingLeft : 0, i, clipToPadding ? getWidth() - this.mPaddingRight : getWidth(), this.mEdgeGlowTop.getMaxHeight() + i);
    }

    private void invalidateBottomGlow() {
        if (this.mEdgeGlowBottom == null) {
            return;
        }
        boolean clipToPadding = getClipToPadding();
        int height = clipToPadding ? getHeight() - this.mPaddingBottom : getHeight();
        invalidate(clipToPadding ? this.mPaddingLeft : 0, height - this.mEdgeGlowBottom.getMaxHeight(), clipToPadding ? getWidth() - this.mPaddingRight : getWidth(), height);
    }

    @Override
    public void onTouchModeChanged(boolean z) {
        if (z) {
            hideSelector();
            if (getHeight() > 0 && getChildCount() > 0) {
                layoutChildren();
            }
            updateSelectorState();
            return;
        }
        int i = this.mTouchMode;
        if (i == 5 || i == 6) {
            if (this.mFlingRunnable != null) {
                this.mFlingRunnable.endFling();
            }
            if (this.mPositionScroller != null) {
                this.mPositionScroller.stop();
            }
            if (this.mScrollY != 0) {
                this.mScrollY = 0;
                invalidateParentCaches();
                finishGlows();
                invalidate();
            }
        }
    }

    @Override
    protected boolean handleScrollBarDragging(MotionEvent motionEvent) {
        return false;
    }

    @Override
    public boolean onTouchEvent(MotionEvent motionEvent) {
        if (!isEnabled()) {
            return isClickable() || isLongClickable();
        }
        if (this.mPositionScroller != null) {
            this.mPositionScroller.stop();
        }
        if (this.mIsDetaching || !isAttachedToWindow()) {
            return false;
        }
        startNestedScroll(2);
        if (this.mFastScroll != null && this.mFastScroll.onTouchEvent(motionEvent)) {
            return true;
        }
        initVelocityTrackerIfNotExists();
        MotionEvent motionEventObtain = MotionEvent.obtain(motionEvent);
        int actionMasked = motionEvent.getActionMasked();
        if (actionMasked == 0) {
            this.mNestedYOffset = 0;
        }
        motionEventObtain.offsetLocation(0.0f, this.mNestedYOffset);
        switch (actionMasked) {
            case 0:
                onTouchDown(motionEvent);
                break;
            case 1:
                onTouchUp(motionEvent);
                break;
            case 2:
                onTouchMove(motionEvent, motionEventObtain);
                break;
            case 3:
                onTouchCancel();
                break;
            case 5:
                int actionIndex = motionEvent.getActionIndex();
                int pointerId = motionEvent.getPointerId(actionIndex);
                int x = (int) motionEvent.getX(actionIndex);
                int y = (int) motionEvent.getY(actionIndex);
                this.mMotionCorrection = 0;
                this.mActivePointerId = pointerId;
                this.mMotionX = x;
                this.mMotionY = y;
                int iPointToPosition = pointToPosition(x, y);
                if (iPointToPosition >= 0) {
                    this.mMotionViewOriginalTop = getChildAt(iPointToPosition - this.mFirstPosition).getTop();
                    this.mMotionPosition = iPointToPosition;
                }
                this.mLastY = y;
                break;
            case 6:
                onSecondaryPointerUp(motionEvent);
                int i = this.mMotionX;
                int i2 = this.mMotionY;
                int iPointToPosition2 = pointToPosition(i, i2);
                if (iPointToPosition2 >= 0) {
                    this.mMotionViewOriginalTop = getChildAt(iPointToPosition2 - this.mFirstPosition).getTop();
                    this.mMotionPosition = iPointToPosition2;
                }
                this.mLastY = i2;
                break;
        }
        if (this.mVelocityTracker != null) {
            this.mVelocityTracker.addMovement(motionEventObtain);
        }
        motionEventObtain.recycle();
        return true;
    }

    private void onTouchDown(MotionEvent motionEvent) {
        this.mHasPerformedLongPress = false;
        this.mActivePointerId = motionEvent.getPointerId(0);
        hideSelector();
        if (this.mTouchMode == 6) {
            this.mFlingRunnable.endFling();
            if (this.mPositionScroller != null) {
                this.mPositionScroller.stop();
            }
            this.mTouchMode = 5;
            this.mMotionX = (int) motionEvent.getX();
            this.mMotionY = (int) motionEvent.getY();
            this.mLastY = this.mMotionY;
            this.mMotionCorrection = 0;
            this.mDirection = 0;
        } else {
            int x = (int) motionEvent.getX();
            int y = (int) motionEvent.getY();
            int iPointToPosition = pointToPosition(x, y);
            if (!this.mDataChanged) {
                if (this.mTouchMode == 4) {
                    createScrollingCache();
                    this.mTouchMode = 3;
                    this.mMotionCorrection = 0;
                    iPointToPosition = findMotionRow(y);
                    this.mFlingRunnable.flywheelTouch();
                } else if (iPointToPosition >= 0 && getAdapter().isEnabled(iPointToPosition)) {
                    this.mTouchMode = 0;
                    if (this.mPendingCheckForTap == null) {
                        this.mPendingCheckForTap = new CheckForTap();
                    }
                    this.mPendingCheckForTap.x = motionEvent.getX();
                    this.mPendingCheckForTap.y = motionEvent.getY();
                    postDelayed(this.mPendingCheckForTap, ViewConfiguration.getTapTimeout());
                }
            }
            if (iPointToPosition >= 0) {
                this.mMotionViewOriginalTop = getChildAt(iPointToPosition - this.mFirstPosition).getTop();
            }
            this.mMotionX = x;
            this.mMotionY = y;
            this.mMotionPosition = iPointToPosition;
            this.mLastY = Integer.MIN_VALUE;
        }
        if (this.mTouchMode == 0 && this.mMotionPosition != -1 && performButtonActionOnTouchDown(motionEvent)) {
            removeCallbacks(this.mPendingCheckForTap);
        }
    }

    private void onTouchMove(MotionEvent motionEvent, MotionEvent motionEvent2) {
        if (this.mHasPerformedLongPress) {
            return;
        }
        int iFindPointerIndex = motionEvent.findPointerIndex(this.mActivePointerId);
        if (iFindPointerIndex == -1) {
            this.mActivePointerId = motionEvent.getPointerId(0);
            iFindPointerIndex = 0;
        }
        if (this.mDataChanged) {
            layoutChildren();
        }
        int y = (int) motionEvent.getY(iFindPointerIndex);
        int i = this.mTouchMode;
        if (i != 5) {
            switch (i) {
                case 0:
                case 1:
                case 2:
                    if (!startScrollIfNeeded((int) motionEvent.getX(iFindPointerIndex), y, motionEvent2)) {
                        View childAt = getChildAt(this.mMotionPosition - this.mFirstPosition);
                        float x = motionEvent.getX(iFindPointerIndex);
                        float f = y;
                        if (!pointInView(x, f, this.mTouchSlop)) {
                            setPressed(false);
                            if (childAt != null) {
                                childAt.setPressed(false);
                            }
                            removeCallbacks(this.mTouchMode == 0 ? this.mPendingCheckForTap : this.mPendingCheckForLongPress);
                            this.mTouchMode = 2;
                            updateSelectorState();
                        } else if (childAt != null) {
                            float[] fArr = this.mTmpPoint;
                            fArr[0] = x;
                            fArr[1] = f;
                            transformPointToViewLocal(fArr, childAt);
                            childAt.drawableHotspotChanged(fArr[0], fArr[1]);
                        }
                        break;
                    }
                    break;
            }
            return;
        }
        scrollIfNeeded((int) motionEvent.getX(iFindPointerIndex), y, motionEvent2);
    }

    private void onTouchUp(MotionEvent motionEvent) {
        int i = this.mTouchMode;
        if (i != 5) {
            switch (i) {
                case 0:
                case 1:
                case 2:
                    int i2 = this.mMotionPosition;
                    final View childAt = getChildAt(i2 - this.mFirstPosition);
                    if (childAt != null) {
                        if (this.mTouchMode != 0) {
                            childAt.setPressed(false);
                        }
                        float x = motionEvent.getX();
                        if ((x > ((float) this.mListPadding.left) && x < ((float) (getWidth() - this.mListPadding.right))) && !childAt.hasExplicitFocusable()) {
                            if (this.mPerformClick == null) {
                                this.mPerformClick = new PerformClick();
                            }
                            final PerformClick performClick = this.mPerformClick;
                            performClick.mClickMotionPosition = i2;
                            performClick.rememberWindowAttachCount();
                            this.mResurrectToPosition = i2;
                            if (this.mTouchMode == 0 || this.mTouchMode == 1) {
                                removeCallbacks(this.mTouchMode == 0 ? this.mPendingCheckForTap : this.mPendingCheckForLongPress);
                                this.mLayoutMode = 0;
                                if (!this.mDataChanged && this.mAdapter.isEnabled(i2)) {
                                    this.mTouchMode = 1;
                                    setSelectedPositionInt(this.mMotionPosition);
                                    layoutChildren();
                                    childAt.setPressed(true);
                                    positionSelector(this.mMotionPosition, childAt);
                                    setPressed(true);
                                    if (this.mSelector != null) {
                                        Drawable current = this.mSelector.getCurrent();
                                        if (current != null && (current instanceof TransitionDrawable)) {
                                            ((TransitionDrawable) current).resetTransition();
                                        }
                                        this.mSelector.setHotspot(x, motionEvent.getY());
                                    }
                                    if (this.mTouchModeReset != null) {
                                        removeCallbacks(this.mTouchModeReset);
                                    }
                                    this.mTouchModeReset = new Runnable() {
                                        @Override
                                        public void run() {
                                            AbsListView.this.mTouchModeReset = null;
                                            AbsListView.this.mTouchMode = -1;
                                            childAt.setPressed(false);
                                            AbsListView.this.setPressed(false);
                                            if (!AbsListView.this.mDataChanged && !AbsListView.this.mIsDetaching && AbsListView.this.isAttachedToWindow()) {
                                                performClick.run();
                                            }
                                        }
                                    };
                                    postDelayed(this.mTouchModeReset, ViewConfiguration.getPressedStateDuration());
                                    return;
                                }
                                this.mTouchMode = -1;
                                updateSelectorState();
                                return;
                            }
                            if (!this.mDataChanged && this.mAdapter.isEnabled(i2)) {
                                performClick.run();
                            }
                        }
                    }
                    this.mTouchMode = -1;
                    updateSelectorState();
                    break;
                case 3:
                    int childCount = getChildCount();
                    if (childCount > 0) {
                        int top = getChildAt(0).getTop();
                        int bottom = getChildAt(childCount - 1).getBottom();
                        int i3 = this.mListPadding.top;
                        int height = getHeight() - this.mListPadding.bottom;
                        if (this.mFirstPosition == 0 && top >= i3 && this.mFirstPosition + childCount < this.mItemCount && bottom <= getHeight() - height) {
                            this.mTouchMode = -1;
                            reportScrollStateChange(0);
                        } else {
                            VelocityTracker velocityTracker = this.mVelocityTracker;
                            velocityTracker.computeCurrentVelocity(1000, this.mMaximumVelocity);
                            int yVelocity = (int) (velocityTracker.getYVelocity(this.mActivePointerId) * this.mVelocityScale);
                            boolean z = Math.abs(yVelocity) > this.mMinimumVelocity;
                            if (z && ((this.mFirstPosition != 0 || top != i3 - this.mOverscrollDistance) && (this.mFirstPosition + childCount != this.mItemCount || bottom != height + this.mOverscrollDistance))) {
                                int i4 = -yVelocity;
                                float f = i4;
                                if (!dispatchNestedPreFling(0.0f, f)) {
                                    if (this.mFlingRunnable == null) {
                                        this.mFlingRunnable = new FlingRunnable();
                                    }
                                    reportScrollStateChange(2);
                                    this.mFlingRunnable.start(i4);
                                    dispatchNestedFling(0.0f, f, true);
                                } else {
                                    this.mTouchMode = -1;
                                    reportScrollStateChange(0);
                                }
                            } else {
                                this.mTouchMode = -1;
                                reportScrollStateChange(0);
                                if (this.mFlingRunnable != null) {
                                    this.mFlingRunnable.endFling();
                                }
                                if (this.mPositionScroller != null) {
                                    this.mPositionScroller.stop();
                                }
                                if (z) {
                                    float f2 = -yVelocity;
                                    if (!dispatchNestedPreFling(0.0f, f2)) {
                                        dispatchNestedFling(0.0f, f2, false);
                                    }
                                }
                            }
                        }
                    } else {
                        this.mTouchMode = -1;
                        reportScrollStateChange(0);
                    }
                    break;
            }
        } else {
            if (this.mFlingRunnable == null) {
                this.mFlingRunnable = new FlingRunnable();
            }
            VelocityTracker velocityTracker2 = this.mVelocityTracker;
            velocityTracker2.computeCurrentVelocity(1000, this.mMaximumVelocity);
            int yVelocity2 = (int) velocityTracker2.getYVelocity(this.mActivePointerId);
            reportScrollStateChange(2);
            if (Math.abs(yVelocity2) > this.mMinimumVelocity) {
                this.mFlingRunnable.startOverfling(-yVelocity2);
            } else {
                this.mFlingRunnable.startSpringback();
            }
        }
        setPressed(false);
        if (this.mEdgeGlowTop != null) {
            this.mEdgeGlowTop.onRelease();
            this.mEdgeGlowBottom.onRelease();
        }
        invalidate();
        removeCallbacks(this.mPendingCheckForLongPress);
        recycleVelocityTracker();
        this.mActivePointerId = -1;
        if (this.mScrollStrictSpan != null) {
            this.mScrollStrictSpan.finish();
            this.mScrollStrictSpan = null;
        }
    }

    private void onTouchCancel() {
        switch (this.mTouchMode) {
            case 5:
                if (this.mFlingRunnable == null) {
                    this.mFlingRunnable = new FlingRunnable();
                }
                this.mFlingRunnable.startSpringback();
                break;
            case 6:
                break;
            default:
                this.mTouchMode = -1;
                setPressed(false);
                View childAt = getChildAt(this.mMotionPosition - this.mFirstPosition);
                if (childAt != null) {
                    childAt.setPressed(false);
                }
                clearScrollingCache();
                removeCallbacks(this.mPendingCheckForLongPress);
                recycleVelocityTracker();
                break;
        }
        if (this.mEdgeGlowTop != null) {
            this.mEdgeGlowTop.onRelease();
            this.mEdgeGlowBottom.onRelease();
        }
        this.mActivePointerId = -1;
    }

    @Override
    protected void onOverScrolled(int i, int i2, boolean z, boolean z2) {
        if (this.mScrollY != i2) {
            onScrollChanged(this.mScrollX, i2, this.mScrollX, this.mScrollY);
            this.mScrollY = i2;
            invalidateParentIfNeeded();
            awakenScrollBars();
        }
    }

    @Override
    public boolean onGenericMotionEvent(MotionEvent motionEvent) {
        float axisValue;
        int actionButton;
        int action = motionEvent.getAction();
        if (action == 8) {
            if (motionEvent.isFromSource(2)) {
                axisValue = motionEvent.getAxisValue(9);
            } else if (motionEvent.isFromSource(4194304)) {
                axisValue = motionEvent.getAxisValue(26);
            } else {
                axisValue = 0.0f;
            }
            int iRound = Math.round(axisValue * this.mVerticalScrollFactor);
            if (iRound != 0 && !trackMotionScroll(iRound, iRound)) {
                return true;
            }
        } else if (action == 11 && motionEvent.isFromSource(2) && (((actionButton = motionEvent.getActionButton()) == 32 || actionButton == 2) && ((this.mTouchMode == 0 || this.mTouchMode == 1) && performStylusButtonPressAction(motionEvent)))) {
            removeCallbacks(this.mPendingCheckForLongPress);
            removeCallbacks(this.mPendingCheckForTap);
        }
        return super.onGenericMotionEvent(motionEvent);
    }

    public void fling(int i) {
        if (this.mFlingRunnable == null) {
            this.mFlingRunnable = new FlingRunnable();
        }
        reportScrollStateChange(2);
        this.mFlingRunnable.start(i);
    }

    @Override
    public boolean onStartNestedScroll(View view, View view2, int i) {
        return (i & 2) != 0;
    }

    @Override
    public void onNestedScrollAccepted(View view, View view2, int i) {
        super.onNestedScrollAccepted(view, view2, i);
        startNestedScroll(2);
    }

    @Override
    public void onNestedScroll(View view, int i, int i2, int i3, int i4) {
        View childAt = getChildAt(getChildCount() / 2);
        int top = 0;
        int top2 = childAt != null ? childAt.getTop() : 0;
        if (childAt != null) {
            int i5 = -i4;
            if (!trackMotionScroll(i5, i5)) {
                return;
            }
        }
        if (childAt != null) {
            top = childAt.getTop() - top2;
            i4 -= top;
        }
        dispatchNestedScroll(0, top, 0, i4, null);
    }

    @Override
    public boolean onNestedFling(View view, float f, float f2, boolean z) {
        int childCount = getChildCount();
        if (!z && childCount > 0) {
            int i = (int) f2;
            if (canScrollList(i) && Math.abs(f2) > this.mMinimumVelocity) {
                reportScrollStateChange(2);
                if (this.mFlingRunnable == null) {
                    this.mFlingRunnable = new FlingRunnable();
                }
                if (!dispatchNestedPreFling(0.0f, f2)) {
                    this.mFlingRunnable.start(i);
                    return true;
                }
                return true;
            }
        }
        return dispatchNestedFling(f, f2, z);
    }

    @Override
    public void draw(Canvas canvas) {
        int width;
        int height;
        int i;
        int i2;
        super.draw(canvas);
        if (this.mEdgeGlowTop != null) {
            int i3 = this.mScrollY;
            boolean clipToPadding = getClipToPadding();
            if (clipToPadding) {
                width = (getWidth() - this.mPaddingLeft) - this.mPaddingRight;
                height = (getHeight() - this.mPaddingTop) - this.mPaddingBottom;
                i = this.mPaddingLeft;
                i2 = this.mPaddingTop;
            } else {
                width = getWidth();
                height = getHeight();
                i = 0;
                i2 = 0;
            }
            if (!this.mEdgeGlowTop.isFinished()) {
                int iSave = canvas.save();
                canvas.clipRect(i, i2, i + width, this.mEdgeGlowTop.getMaxHeight() + i2);
                canvas.translate(i, Math.min(0, this.mFirstPositionDistanceGuess + i3) + i2);
                this.mEdgeGlowTop.setSize(width, height);
                if (this.mEdgeGlowTop.draw(canvas)) {
                    invalidateTopGlow();
                }
                canvas.restoreToCount(iSave);
            }
            if (!this.mEdgeGlowBottom.isFinished()) {
                int iSave2 = canvas.save();
                int i4 = i2 + height;
                canvas.clipRect(i, i4 - this.mEdgeGlowBottom.getMaxHeight(), i + width, i4);
                canvas.translate((-width) + i, Math.max(getHeight(), i3 + this.mLastPositionDistanceGuess) - (clipToPadding ? this.mPaddingBottom : 0));
                canvas.rotate(180.0f, width, 0.0f);
                this.mEdgeGlowBottom.setSize(width, height);
                if (this.mEdgeGlowBottom.draw(canvas)) {
                    invalidateBottomGlow();
                }
                canvas.restoreToCount(iSave2);
            }
        }
    }

    private void initOrResetVelocityTracker() {
        if (this.mVelocityTracker == null) {
            this.mVelocityTracker = VelocityTracker.obtain();
        } else {
            this.mVelocityTracker.clear();
        }
    }

    private void initVelocityTrackerIfNotExists() {
        if (this.mVelocityTracker == null) {
            this.mVelocityTracker = VelocityTracker.obtain();
        }
    }

    private void recycleVelocityTracker() {
        if (this.mVelocityTracker != null) {
            this.mVelocityTracker.recycle();
            this.mVelocityTracker = null;
        }
    }

    @Override
    public void requestDisallowInterceptTouchEvent(boolean z) {
        if (z) {
            recycleVelocityTracker();
        }
        super.requestDisallowInterceptTouchEvent(z);
    }

    @Override
    public boolean onInterceptHoverEvent(MotionEvent motionEvent) {
        if (this.mFastScroll != null && this.mFastScroll.onInterceptHoverEvent(motionEvent)) {
            return true;
        }
        return super.onInterceptHoverEvent(motionEvent);
    }

    @Override
    public PointerIcon onResolvePointerIcon(MotionEvent motionEvent, int i) {
        PointerIcon pointerIconOnResolvePointerIcon;
        if (this.mFastScroll != null && (pointerIconOnResolvePointerIcon = this.mFastScroll.onResolvePointerIcon(motionEvent, i)) != null) {
            return pointerIconOnResolvePointerIcon;
        }
        return super.onResolvePointerIcon(motionEvent, i);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent motionEvent) {
        int actionMasked = motionEvent.getActionMasked();
        if (this.mPositionScroller != null) {
            this.mPositionScroller.stop();
        }
        if (this.mIsDetaching || !isAttachedToWindow()) {
            return false;
        }
        if (this.mFastScroll != null && this.mFastScroll.onInterceptTouchEvent(motionEvent)) {
            return true;
        }
        if (actionMasked != 6) {
            switch (actionMasked) {
                case 0:
                    int i = this.mTouchMode;
                    if (i == 6 || i == 5) {
                        this.mMotionCorrection = 0;
                        return true;
                    }
                    int x = (int) motionEvent.getX();
                    int y = (int) motionEvent.getY();
                    this.mActivePointerId = motionEvent.getPointerId(0);
                    int iFindMotionRow = findMotionRow(y);
                    if (i != 4 && iFindMotionRow >= 0) {
                        this.mMotionViewOriginalTop = getChildAt(iFindMotionRow - this.mFirstPosition).getTop();
                        this.mMotionX = x;
                        this.mMotionY = y;
                        this.mMotionPosition = iFindMotionRow;
                        this.mTouchMode = 0;
                        clearScrollingCache();
                    }
                    this.mLastY = Integer.MIN_VALUE;
                    initOrResetVelocityTracker();
                    this.mVelocityTracker.addMovement(motionEvent);
                    this.mNestedYOffset = 0;
                    startNestedScroll(2);
                    if (i == 4) {
                        return true;
                    }
                    break;
                case 1:
                case 3:
                    this.mTouchMode = -1;
                    this.mActivePointerId = -1;
                    recycleVelocityTracker();
                    reportScrollStateChange(0);
                    stopNestedScroll();
                    break;
                case 2:
                    if (this.mTouchMode == 0) {
                        int iFindPointerIndex = motionEvent.findPointerIndex(this.mActivePointerId);
                        if (iFindPointerIndex == -1) {
                            this.mActivePointerId = motionEvent.getPointerId(0);
                            iFindPointerIndex = 0;
                        }
                        int y2 = (int) motionEvent.getY(iFindPointerIndex);
                        initVelocityTrackerIfNotExists();
                        this.mVelocityTracker.addMovement(motionEvent);
                        if (startScrollIfNeeded((int) motionEvent.getX(iFindPointerIndex), y2, null)) {
                            return true;
                        }
                    }
                    break;
            }
        } else {
            onSecondaryPointerUp(motionEvent);
        }
        return false;
    }

    private void onSecondaryPointerUp(MotionEvent motionEvent) {
        int action = (motionEvent.getAction() & 65280) >> 8;
        if (motionEvent.getPointerId(action) == this.mActivePointerId) {
            int i = action == 0 ? 1 : 0;
            this.mMotionX = (int) motionEvent.getX(i);
            this.mMotionY = (int) motionEvent.getY(i);
            this.mMotionCorrection = 0;
            this.mActivePointerId = motionEvent.getPointerId(i);
        }
    }

    @Override
    public void addTouchables(ArrayList<View> arrayList) {
        int childCount = getChildCount();
        int i = this.mFirstPosition;
        ListAdapter listAdapter = this.mAdapter;
        if (listAdapter == null) {
            return;
        }
        for (int i2 = 0; i2 < childCount; i2++) {
            View childAt = getChildAt(i2);
            if (listAdapter.isEnabled(i + i2)) {
                arrayList.add(childAt);
            }
            childAt.addTouchables(arrayList);
        }
    }

    void reportScrollStateChange(int i) {
        if (i != this.mLastScrollState && this.mOnScrollListener != null) {
            this.mLastScrollState = i;
            this.mOnScrollListener.onScrollStateChanged(this, i);
        }
    }

    private class FlingRunnable implements Runnable {
        private static final int FLYWHEEL_TIMEOUT = 40;
        private final Runnable mCheckFlywheel = new Runnable() {
            @Override
            public void run() {
                int i = AbsListView.this.mActivePointerId;
                VelocityTracker velocityTracker = AbsListView.this.mVelocityTracker;
                OverScroller overScroller = FlingRunnable.this.mScroller;
                if (velocityTracker == null || i == -1) {
                    return;
                }
                velocityTracker.computeCurrentVelocity(1000, AbsListView.this.mMaximumVelocity);
                float f = -velocityTracker.getYVelocity(i);
                if (Math.abs(f) >= AbsListView.this.mMinimumVelocity && overScroller.isScrollingInDirection(0.0f, f)) {
                    AbsListView.this.postDelayed(this, 40L);
                    return;
                }
                FlingRunnable.this.endFling();
                AbsListView.this.mTouchMode = 3;
                AbsListView.this.reportScrollStateChange(1);
            }
        };
        private int mLastFlingY;
        private final OverScroller mScroller;
        private boolean mSuppressIdleStateChangeCall;

        FlingRunnable() {
            this.mScroller = new OverScroller(AbsListView.this.getContext());
        }

        void start(int i) {
            int i2 = i < 0 ? Integer.MAX_VALUE : 0;
            this.mLastFlingY = i2;
            this.mScroller.setInterpolator(null);
            this.mScroller.fling(0, i2, 0, i, 0, Integer.MAX_VALUE, 0, Integer.MAX_VALUE);
            AbsListView.this.mTouchMode = 4;
            this.mSuppressIdleStateChangeCall = false;
            AbsListView.this.postOnAnimation(this);
            if (AbsListView.this.mFlingStrictSpan == null) {
                AbsListView.this.mFlingStrictSpan = StrictMode.enterCriticalSpan("AbsListView-fling");
            }
        }

        void startSpringback() {
            this.mSuppressIdleStateChangeCall = false;
            if (this.mScroller.springBack(0, AbsListView.this.mScrollY, 0, 0, 0, 0)) {
                AbsListView.this.mTouchMode = 6;
                AbsListView.this.invalidate();
                AbsListView.this.postOnAnimation(this);
            } else {
                AbsListView.this.mTouchMode = -1;
                AbsListView.this.reportScrollStateChange(0);
            }
        }

        void startOverfling(int i) {
            this.mScroller.setInterpolator(null);
            this.mScroller.fling(0, AbsListView.this.mScrollY, 0, i, 0, 0, Integer.MIN_VALUE, Integer.MAX_VALUE, 0, AbsListView.this.getHeight());
            AbsListView.this.mTouchMode = 6;
            this.mSuppressIdleStateChangeCall = false;
            AbsListView.this.invalidate();
            AbsListView.this.postOnAnimation(this);
        }

        void edgeReached(int i) {
            this.mScroller.notifyVerticalEdgeReached(AbsListView.this.mScrollY, 0, AbsListView.this.mOverflingDistance);
            int overScrollMode = AbsListView.this.getOverScrollMode();
            if (overScrollMode == 0 || (overScrollMode == 1 && !AbsListView.this.contentFits())) {
                AbsListView.this.mTouchMode = 6;
                int currVelocity = (int) this.mScroller.getCurrVelocity();
                if (i > 0) {
                    AbsListView.this.mEdgeGlowTop.onAbsorb(currVelocity);
                } else {
                    AbsListView.this.mEdgeGlowBottom.onAbsorb(currVelocity);
                }
            } else {
                AbsListView.this.mTouchMode = -1;
                if (AbsListView.this.mPositionScroller != null) {
                    AbsListView.this.mPositionScroller.stop();
                }
            }
            AbsListView.this.invalidate();
            AbsListView.this.postOnAnimation(this);
        }

        void startScroll(int i, int i2, boolean z, boolean z2) {
            int i3 = i < 0 ? Integer.MAX_VALUE : 0;
            this.mLastFlingY = i3;
            this.mScroller.setInterpolator(z ? AbsListView.sLinearInterpolator : null);
            this.mScroller.startScroll(0, i3, 0, i, i2);
            AbsListView.this.mTouchMode = 4;
            this.mSuppressIdleStateChangeCall = z2;
            AbsListView.this.postOnAnimation(this);
        }

        void endFling() {
            AbsListView.this.mTouchMode = -1;
            AbsListView.this.removeCallbacks(this);
            AbsListView.this.removeCallbacks(this.mCheckFlywheel);
            if (!this.mSuppressIdleStateChangeCall) {
                AbsListView.this.reportScrollStateChange(0);
            }
            AbsListView.this.clearScrollingCache();
            this.mScroller.abortAnimation();
            if (AbsListView.this.mFlingStrictSpan != null) {
                AbsListView.this.mFlingStrictSpan.finish();
                AbsListView.this.mFlingStrictSpan = null;
            }
        }

        void flywheelTouch() {
            AbsListView.this.postDelayed(this.mCheckFlywheel, 40L);
        }

        @Override
        public void run() {
            int iMax;
            int top;
            int i = AbsListView.this.mTouchMode;
            boolean z = false;
            if (i != 6) {
                switch (i) {
                    case 3:
                        if (this.mScroller.isFinished()) {
                            return;
                        }
                        break;
                    case 4:
                        break;
                    default:
                        endFling();
                        return;
                }
                if (AbsListView.this.mDataChanged) {
                    AbsListView.this.layoutChildren();
                }
                if (AbsListView.this.mItemCount == 0 || AbsListView.this.getChildCount() == 0) {
                    endFling();
                    return;
                }
                OverScroller overScroller = this.mScroller;
                boolean zComputeScrollOffset = overScroller.computeScrollOffset();
                int currY = overScroller.getCurrY();
                int i2 = this.mLastFlingY - currY;
                if (i2 > 0) {
                    AbsListView.this.mMotionPosition = AbsListView.this.mFirstPosition;
                    AbsListView.this.mMotionViewOriginalTop = AbsListView.this.getChildAt(0).getTop();
                    iMax = Math.min(((AbsListView.this.getHeight() - AbsListView.this.mPaddingBottom) - AbsListView.this.mPaddingTop) - 1, i2);
                } else {
                    int childCount = AbsListView.this.getChildCount() - 1;
                    AbsListView.this.mMotionPosition = AbsListView.this.mFirstPosition + childCount;
                    AbsListView.this.mMotionViewOriginalTop = AbsListView.this.getChildAt(childCount).getTop();
                    iMax = Math.max(-(((AbsListView.this.getHeight() - AbsListView.this.mPaddingBottom) - AbsListView.this.mPaddingTop) - 1), i2);
                }
                View childAt = AbsListView.this.getChildAt(AbsListView.this.mMotionPosition - AbsListView.this.mFirstPosition);
                if (childAt != null) {
                    top = childAt.getTop();
                } else {
                    top = 0;
                }
                boolean zTrackMotionScroll = AbsListView.this.trackMotionScroll(iMax, iMax);
                if (zTrackMotionScroll && iMax != 0) {
                    z = true;
                }
                if (z) {
                    if (childAt != null) {
                        AbsListView.this.overScrollBy(0, -(iMax - (childAt.getTop() - top)), 0, AbsListView.this.mScrollY, 0, 0, 0, AbsListView.this.mOverflingDistance, false);
                    }
                    if (zComputeScrollOffset) {
                        edgeReached(iMax);
                        return;
                    }
                    return;
                }
                if (zComputeScrollOffset && !z) {
                    if (zTrackMotionScroll) {
                        AbsListView.this.invalidate();
                    }
                    this.mLastFlingY = currY;
                    AbsListView.this.postOnAnimation(this);
                    return;
                }
                endFling();
                return;
            }
            OverScroller overScroller2 = this.mScroller;
            if (overScroller2.computeScrollOffset()) {
                int i3 = AbsListView.this.mScrollY;
                int currY2 = overScroller2.getCurrY();
                if (AbsListView.this.overScrollBy(0, currY2 - i3, 0, i3, 0, 0, 0, AbsListView.this.mOverflingDistance, false)) {
                    boolean z2 = i3 <= 0 && currY2 > 0;
                    if (i3 >= 0 && currY2 < 0) {
                        z = true;
                    }
                    if (z2 || z) {
                        int currVelocity = (int) overScroller2.getCurrVelocity();
                        if (z) {
                            currVelocity = -currVelocity;
                        }
                        overScroller2.abortAnimation();
                        start(currVelocity);
                        return;
                    }
                    startSpringback();
                    return;
                }
                AbsListView.this.invalidate();
                AbsListView.this.postOnAnimation(this);
                return;
            }
            endFling();
        }
    }

    public void setFriction(float f) {
        if (this.mFlingRunnable == null) {
            this.mFlingRunnable = new FlingRunnable();
        }
        this.mFlingRunnable.mScroller.setFriction(f);
    }

    public void setVelocityScale(float f) {
        this.mVelocityScale = f;
    }

    AbsPositionScroller createPositionScroller() {
        return new PositionScroller();
    }

    public void smoothScrollToPosition(int i) {
        if (this.mPositionScroller == null) {
            this.mPositionScroller = createPositionScroller();
        }
        this.mPositionScroller.start(i);
    }

    public void smoothScrollToPositionFromTop(int i, int i2, int i3) {
        if (this.mPositionScroller == null) {
            this.mPositionScroller = createPositionScroller();
        }
        this.mPositionScroller.startWithOffset(i, i2, i3);
    }

    public void smoothScrollToPositionFromTop(int i, int i2) {
        if (this.mPositionScroller == null) {
            this.mPositionScroller = createPositionScroller();
        }
        this.mPositionScroller.startWithOffset(i, i2);
    }

    public void smoothScrollToPosition(int i, int i2) {
        if (this.mPositionScroller == null) {
            this.mPositionScroller = createPositionScroller();
        }
        this.mPositionScroller.start(i, i2);
    }

    public void smoothScrollBy(int i, int i2) {
        smoothScrollBy(i, i2, false, false);
    }

    void smoothScrollBy(int i, int i2, boolean z, boolean z2) {
        if (this.mFlingRunnable == null) {
            this.mFlingRunnable = new FlingRunnable();
        }
        int i3 = this.mFirstPosition;
        int childCount = getChildCount();
        int i4 = i3 + childCount;
        int paddingTop = getPaddingTop();
        int height = getHeight() - getPaddingBottom();
        if (i == 0 || this.mItemCount == 0 || childCount == 0 || ((i3 == 0 && getChildAt(0).getTop() == paddingTop && i < 0) || (i4 == this.mItemCount && getChildAt(childCount - 1).getBottom() == height && i > 0))) {
            this.mFlingRunnable.endFling();
            if (this.mPositionScroller != null) {
                this.mPositionScroller.stop();
                return;
            }
            return;
        }
        reportScrollStateChange(2);
        this.mFlingRunnable.startScroll(i, i2, z, z2);
    }

    void smoothScrollByOffset(int i) {
        int lastVisiblePosition;
        View childAt;
        if (i < 0) {
            lastVisiblePosition = getFirstVisiblePosition();
        } else if (i > 0) {
            lastVisiblePosition = getLastVisiblePosition();
        } else {
            lastVisiblePosition = -1;
        }
        if (lastVisiblePosition > -1 && (childAt = getChildAt(lastVisiblePosition - getFirstVisiblePosition())) != null) {
            if (childAt.getGlobalVisibleRect(new Rect())) {
                float fWidth = (r2.width() * r2.height()) / (childAt.getWidth() * childAt.getHeight());
                if (i < 0 && fWidth < 0.75f) {
                    lastVisiblePosition++;
                } else if (i > 0 && fWidth < 0.75f) {
                    lastVisiblePosition--;
                }
            }
            smoothScrollToPosition(Math.max(0, Math.min(getCount(), lastVisiblePosition + i)));
        }
    }

    private void createScrollingCache() {
        if (this.mScrollingCacheEnabled && !this.mCachingStarted && !isHardwareAccelerated()) {
            setChildrenDrawnWithCacheEnabled(true);
            setChildrenDrawingCacheEnabled(true);
            this.mCachingActive = true;
            this.mCachingStarted = true;
        }
    }

    private void clearScrollingCache() {
        if (!isHardwareAccelerated()) {
            if (this.mClearScrollingCache == null) {
                this.mClearScrollingCache = new Runnable() {
                    @Override
                    public void run() {
                        if (AbsListView.this.mCachingStarted) {
                            AbsListView absListView = AbsListView.this;
                            AbsListView.this.mCachingActive = false;
                            absListView.mCachingStarted = false;
                            AbsListView.this.setChildrenDrawnWithCacheEnabled(false);
                            if ((AbsListView.this.mPersistentDrawingCache & 2) == 0) {
                                AbsListView.this.setChildrenDrawingCacheEnabled(false);
                            }
                            if (!AbsListView.this.isAlwaysDrawnWithCacheEnabled()) {
                                AbsListView.this.invalidate();
                            }
                        }
                    }
                };
            }
            post(this.mClearScrollingCache);
        }
    }

    public void scrollListBy(int i) {
        int i2 = -i;
        trackMotionScroll(i2, i2);
    }

    public boolean canScrollList(int i) {
        int childCount = getChildCount();
        if (childCount == 0) {
            return false;
        }
        int i2 = this.mFirstPosition;
        Rect rect = this.mListPadding;
        if (i > 0) {
            int bottom = getChildAt(childCount - 1).getBottom();
            if (i2 + childCount >= this.mItemCount && bottom <= getHeight() - rect.bottom) {
                return false;
            }
            return true;
        }
        int top = getChildAt(0).getTop();
        if (i2 <= 0 && top >= rect.top) {
            return false;
        }
        return true;
    }

    boolean trackMotionScroll(int i, int i2) {
        int i3;
        int i4;
        int iMin;
        int i5;
        int i6;
        int i7;
        boolean z;
        int i8;
        int childCount = getChildCount();
        if (childCount == 0) {
            return true;
        }
        int top = getChildAt(0).getTop();
        int i9 = childCount - 1;
        int bottom = getChildAt(i9).getBottom();
        Rect rect = this.mListPadding;
        if ((this.mGroupFlags & 34) == 34) {
            i3 = rect.top;
            i4 = rect.bottom;
        } else {
            i3 = 0;
            i4 = 0;
        }
        int i10 = i3 - top;
        int height = bottom - (getHeight() - i4);
        int height2 = (getHeight() - this.mPaddingBottom) - this.mPaddingTop;
        if (i < 0) {
            iMin = Math.max(-(height2 - 1), i);
        } else {
            iMin = Math.min(height2 - 1, i);
        }
        int iMax = i2 < 0 ? Math.max(-(height2 - 1), i2) : Math.min(height2 - 1, i2);
        int i11 = this.mFirstPosition;
        if (i11 == 0) {
            this.mFirstPositionDistanceGuess = top - rect.top;
        } else {
            this.mFirstPositionDistanceGuess += iMax;
        }
        int i12 = i11 + childCount;
        if (i12 == this.mItemCount) {
            this.mLastPositionDistanceGuess = rect.bottom + bottom;
        } else {
            this.mLastPositionDistanceGuess += iMax;
        }
        boolean z2 = i11 == 0 && top >= rect.top && iMax >= 0;
        boolean z3 = i12 == this.mItemCount && bottom <= getHeight() - rect.bottom && iMax <= 0;
        if (z2 || z3) {
            if (iMax != 0) {
                return true;
            }
            return false;
        }
        boolean z4 = iMax < 0;
        boolean zIsInTouchMode = isInTouchMode();
        if (zIsInTouchMode) {
            hideSelector();
        }
        int headerViewsCount = getHeaderViewsCount();
        int footerViewsCount = this.mItemCount - getFooterViewsCount();
        if (z4) {
            int i13 = -iMax;
            if ((this.mGroupFlags & 34) == 34) {
                i13 += rect.top;
            }
            int i14 = 0;
            i6 = 0;
            while (i14 < childCount) {
                View childAt = getChildAt(i14);
                if (childAt.getBottom() >= i13) {
                    break;
                }
                i6++;
                int i15 = i11 + i14;
                if (i15 < headerViewsCount || i15 >= footerViewsCount) {
                    i8 = childCount;
                } else {
                    childAt.clearAccessibilityFocus();
                    i8 = childCount;
                    this.mRecycler.addScrapView(childAt, i15);
                }
                i14++;
                childCount = i8;
            }
            i5 = 0;
        } else {
            int height3 = getHeight() - iMax;
            if ((this.mGroupFlags & 34) == 34) {
                height3 -= rect.bottom;
            }
            int i16 = i9;
            i5 = 0;
            i6 = 0;
            while (i16 >= 0) {
                View childAt2 = getChildAt(i16);
                if (childAt2.getTop() <= height3) {
                    break;
                }
                i6++;
                int i17 = i11 + i16;
                if (i17 >= headerViewsCount && i17 < footerViewsCount) {
                    childAt2.clearAccessibilityFocus();
                    this.mRecycler.addScrapView(childAt2, i17);
                }
                int i18 = i16;
                i16--;
                i5 = i18;
            }
        }
        this.mMotionViewNewTop = this.mMotionViewOriginalTop + iMin;
        boolean z5 = true;
        this.mBlockLayoutRequests = true;
        if (i6 > 0) {
            detachViewsFromParent(i5, i6);
            this.mRecycler.removeSkippedScrap();
        }
        if (!awakenScrollBars()) {
            invalidate();
        }
        offsetChildrenTopAndBottom(iMax);
        if (z4) {
            this.mFirstPosition += i6;
        }
        int iAbs = Math.abs(iMax);
        if (i10 < iAbs || height < iAbs) {
            fillGap(z4);
        }
        this.mRecycler.fullyDetachScrapViews();
        if (zIsInTouchMode || this.mSelectedPosition == -1) {
            if (this.mSelectorPosition != -1 && (i7 = this.mSelectorPosition - this.mFirstPosition) >= 0 && i7 < getChildCount()) {
                positionSelector(this.mSelectorPosition, getChildAt(i7));
            } else {
                z5 = false;
            }
        } else {
            int i19 = this.mSelectedPosition - this.mFirstPosition;
            if (i19 < 0 || i19 >= getChildCount()) {
                z = false;
            } else {
                positionSelector(this.mSelectedPosition, getChildAt(i19));
                z = true;
            }
            z5 = z;
        }
        if (!z5) {
            this.mSelectorRect.setEmpty();
        }
        this.mBlockLayoutRequests = false;
        invokeOnItemScrollListener();
        return false;
    }

    int getHeaderViewsCount() {
        return 0;
    }

    int getFooterViewsCount() {
        return 0;
    }

    void hideSelector() {
        if (this.mSelectedPosition != -1) {
            if (this.mLayoutMode != 4) {
                this.mResurrectToPosition = this.mSelectedPosition;
            }
            if (this.mNextSelectedPosition >= 0 && this.mNextSelectedPosition != this.mSelectedPosition) {
                this.mResurrectToPosition = this.mNextSelectedPosition;
            }
            setSelectedPositionInt(-1);
            setNextSelectedPositionInt(-1);
            this.mSelectedTop = 0;
        }
    }

    int reconcileSelectedPosition() {
        int i = this.mSelectedPosition;
        if (i < 0) {
            i = this.mResurrectToPosition;
        }
        return Math.min(Math.max(0, i), this.mItemCount - 1);
    }

    int findClosestMotionRow(int i) {
        if (getChildCount() == 0) {
            return -1;
        }
        int iFindMotionRow = findMotionRow(i);
        return iFindMotionRow != -1 ? iFindMotionRow : (this.mFirstPosition + r0) - 1;
    }

    public void invalidateViews() {
        this.mDataChanged = true;
        rememberSyncState();
        requestLayout();
        invalidate();
    }

    boolean resurrectSelectionIfNeeded() {
        if (this.mSelectedPosition < 0 && resurrectSelection()) {
            updateSelectorState();
            return true;
        }
        return false;
    }

    boolean resurrectSelection() {
        boolean z;
        int top;
        int iLookForSelectablePosition;
        int childCount = getChildCount();
        if (childCount <= 0) {
            return false;
        }
        int i = this.mListPadding.top;
        int i2 = (this.mBottom - this.mTop) - this.mListPadding.bottom;
        int i3 = this.mFirstPosition;
        int i4 = this.mResurrectToPosition;
        if (i4 >= i3 && i4 < i3 + childCount) {
            View childAt = getChildAt(i4 - this.mFirstPosition);
            top = childAt.getTop();
            int bottom = childAt.getBottom();
            if (top < i) {
                top = i + getVerticalFadingEdgeLength();
            } else if (bottom > i2) {
                top = (i2 - childAt.getMeasuredHeight()) - getVerticalFadingEdgeLength();
            }
        } else {
            if (i4 >= i3) {
                int i5 = this.mItemCount;
                int i6 = i3 + childCount;
                int i7 = i6 - 1;
                int i8 = childCount - 1;
                int i9 = 0;
                int verticalFadingEdgeLength = i2;
                int i10 = i8;
                while (true) {
                    if (i10 < 0) {
                        z = false;
                        i4 = i7;
                        top = i9;
                        break;
                    }
                    View childAt2 = getChildAt(i10);
                    int top2 = childAt2.getTop();
                    int bottom2 = childAt2.getBottom();
                    if (i10 == i8) {
                        if (i6 < i5 || bottom2 > verticalFadingEdgeLength) {
                            verticalFadingEdgeLength -= getVerticalFadingEdgeLength();
                        }
                        i9 = top2;
                    }
                    if (bottom2 <= verticalFadingEdgeLength) {
                        i4 = i3 + i10;
                        z = false;
                        top = top2;
                        break;
                    }
                    i10--;
                }
                this.mResurrectToPosition = -1;
                removeCallbacks(this.mFlingRunnable);
                if (this.mPositionScroller != null) {
                    this.mPositionScroller.stop();
                }
                this.mTouchMode = -1;
                clearScrollingCache();
                this.mSpecificTop = top;
                iLookForSelectablePosition = lookForSelectablePosition(i4, z);
                if (iLookForSelectablePosition >= i3 || iLookForSelectablePosition > getLastVisiblePosition()) {
                    iLookForSelectablePosition = -1;
                } else {
                    this.mLayoutMode = 4;
                    updateSelectorState();
                    setSelectionInt(iLookForSelectablePosition);
                    invokeOnItemScrollListener();
                }
                reportScrollStateChange(0);
                return iLookForSelectablePosition < 0;
            }
            int i11 = 0;
            int verticalFadingEdgeLength2 = i;
            int i12 = 0;
            while (true) {
                if (i12 >= childCount) {
                    top = i11;
                    i4 = i3;
                    break;
                }
                top = getChildAt(i12).getTop();
                if (i12 == 0) {
                    if (i3 > 0 || top < verticalFadingEdgeLength2) {
                        verticalFadingEdgeLength2 += getVerticalFadingEdgeLength();
                    }
                    i11 = top;
                }
                if (top >= verticalFadingEdgeLength2) {
                    i4 = i3 + i12;
                    break;
                }
                i12++;
            }
        }
        z = true;
        this.mResurrectToPosition = -1;
        removeCallbacks(this.mFlingRunnable);
        if (this.mPositionScroller != null) {
        }
        this.mTouchMode = -1;
        clearScrollingCache();
        this.mSpecificTop = top;
        iLookForSelectablePosition = lookForSelectablePosition(i4, z);
        if (iLookForSelectablePosition >= i3) {
            iLookForSelectablePosition = -1;
        }
        reportScrollStateChange(0);
        if (iLookForSelectablePosition < 0) {
        }
    }

    void confirmCheckedPositionsById() {
        boolean z;
        this.mCheckStates.clear();
        int i = 0;
        boolean z2 = false;
        while (i < this.mCheckedIdStates.size()) {
            long jKeyAt = this.mCheckedIdStates.keyAt(i);
            int iIntValue = this.mCheckedIdStates.valueAt(i).intValue();
            if (jKeyAt != this.mAdapter.getItemId(iIntValue)) {
                int iMax = Math.max(0, iIntValue - 20);
                int iMin = Math.min(iIntValue + 20, this.mItemCount);
                while (true) {
                    if (iMax >= iMin) {
                        z = false;
                        break;
                    } else if (jKeyAt != this.mAdapter.getItemId(iMax)) {
                        iMax++;
                    } else {
                        this.mCheckStates.put(iMax, true);
                        this.mCheckedIdStates.setValueAt(i, Integer.valueOf(iMax));
                        z = true;
                        break;
                    }
                }
                if (!z) {
                    this.mCheckedIdStates.delete(jKeyAt);
                    i--;
                    this.mCheckedItemCount--;
                    if (this.mChoiceActionMode != null && this.mMultiChoiceModeCallback != null) {
                        this.mMultiChoiceModeCallback.onItemCheckedStateChanged(this.mChoiceActionMode, iIntValue, jKeyAt, false);
                    }
                    z2 = true;
                }
            } else {
                this.mCheckStates.put(iIntValue, true);
            }
            i++;
        }
        if (z2 && this.mChoiceActionMode != null) {
            this.mChoiceActionMode.invalidate();
        }
    }

    @Override
    protected void handleDataChanged() {
        int i;
        int i2 = this.mItemCount;
        int i3 = this.mLastHandledItemCount;
        this.mLastHandledItemCount = this.mItemCount;
        if (this.mChoiceMode != 0 && this.mAdapter != null && this.mAdapter.hasStableIds()) {
            confirmCheckedPositionsById();
        }
        this.mRecycler.clearTransientStateViews();
        if (i2 > 0) {
            if (this.mNeedSync) {
                this.mNeedSync = false;
                this.mPendingSync = null;
                if (this.mTranscriptMode == 2) {
                    this.mLayoutMode = 3;
                    return;
                }
                if (this.mTranscriptMode == 1) {
                    if (this.mForceTranscriptScroll) {
                        this.mForceTranscriptScroll = false;
                        this.mLayoutMode = 3;
                        return;
                    }
                    int childCount = getChildCount();
                    int height = getHeight() - getPaddingBottom();
                    View childAt = getChildAt(childCount - 1);
                    int bottom = childAt != null ? childAt.getBottom() : height;
                    if (this.mFirstPosition + childCount >= i3 && bottom <= height) {
                        this.mLayoutMode = 3;
                        return;
                    }
                    awakenScrollBars();
                }
                switch (this.mSyncMode) {
                    case 0:
                        if (isInTouchMode()) {
                            this.mLayoutMode = 5;
                            this.mSyncPosition = Math.min(Math.max(0, this.mSyncPosition), i2 - 1);
                        } else {
                            int iFindSyncPosition = findSyncPosition();
                            if (iFindSyncPosition >= 0 && lookForSelectablePosition(iFindSyncPosition, true) == iFindSyncPosition) {
                                this.mSyncPosition = iFindSyncPosition;
                                if (this.mSyncHeight == getHeight()) {
                                    this.mLayoutMode = 5;
                                } else {
                                    this.mLayoutMode = 2;
                                }
                                setNextSelectedPositionInt(iFindSyncPosition);
                            }
                        }
                        break;
                    case 1:
                        this.mLayoutMode = 5;
                        this.mSyncPosition = Math.min(Math.max(0, this.mSyncPosition), i2 - 1);
                        break;
                }
                return;
            }
            if (!isInTouchMode()) {
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
                if (iLookForSelectablePosition >= 0) {
                    setNextSelectedPositionInt(iLookForSelectablePosition);
                    return;
                }
                int iLookForSelectablePosition2 = lookForSelectablePosition(i, false);
                if (iLookForSelectablePosition2 >= 0) {
                    setNextSelectedPositionInt(iLookForSelectablePosition2);
                    return;
                }
            } else if (this.mResurrectToPosition >= 0) {
                return;
            }
        }
        this.mLayoutMode = this.mStackFromBottom ? 3 : 1;
        this.mSelectedPosition = -1;
        this.mSelectedRowId = Long.MIN_VALUE;
        this.mNextSelectedPosition = -1;
        this.mNextSelectedRowId = Long.MIN_VALUE;
        this.mNeedSync = false;
        this.mPendingSync = null;
        this.mSelectorPosition = -1;
        checkSelectionChanged();
    }

    @Override
    protected void onDisplayHint(int i) {
        super.onDisplayHint(i);
        if (i != 0) {
            if (i == 4 && this.mPopup != null && this.mPopup.isShowing()) {
                dismissPopup();
            }
        } else if (this.mFiltered && this.mPopup != null && !this.mPopup.isShowing()) {
            showPopup();
        }
        this.mPopupHidden = i == 4;
    }

    private void dismissPopup() {
        if (this.mPopup != null) {
            this.mPopup.dismiss();
        }
    }

    private void showPopup() {
        if (getWindowVisibility() == 0) {
            createTextFilter(true);
            positionPopup();
            checkFocus();
        }
    }

    private void positionPopup() {
        int i = getResources().getDisplayMetrics().heightPixels;
        int[] iArr = new int[2];
        getLocationOnScreen(iArr);
        int height = ((i - iArr[1]) - getHeight()) + ((int) (this.mDensityScale * 20.0f));
        if (!this.mPopup.isShowing()) {
            this.mPopup.showAtLocation(this, 81, iArr[0], height);
        } else {
            this.mPopup.update(iArr[0], height, -1, -1);
        }
    }

    static int getDistance(Rect rect, Rect rect2, int i) {
        int iWidth;
        int iHeight;
        int iWidth2;
        int iHeight2;
        if (i == 17) {
            iWidth = rect.left;
            iHeight = rect.top + (rect.height() / 2);
            iWidth2 = rect2.right;
            iHeight2 = rect2.top + (rect2.height() / 2);
        } else if (i == 33) {
            iWidth = rect.left + (rect.width() / 2);
            iHeight = rect.top;
            iWidth2 = rect2.left + (rect2.width() / 2);
            iHeight2 = rect2.bottom;
        } else if (i == 66) {
            iWidth = rect.right;
            iHeight = rect.top + (rect.height() / 2);
            iWidth2 = rect2.left;
            iHeight2 = rect2.top + (rect2.height() / 2);
        } else if (i == 130) {
            iWidth = rect.left + (rect.width() / 2);
            iHeight = rect.bottom;
            iWidth2 = rect2.left + (rect2.width() / 2);
            iHeight2 = rect2.top;
        } else {
            switch (i) {
                case 1:
                case 2:
                    iWidth = rect.right + (rect.width() / 2);
                    iHeight = rect.top + (rect.height() / 2);
                    iWidth2 = rect2.left + (rect2.width() / 2);
                    iHeight2 = rect2.top + (rect2.height() / 2);
                    break;
                default:
                    throw new IllegalArgumentException("direction must be one of {FOCUS_UP, FOCUS_DOWN, FOCUS_LEFT, FOCUS_RIGHT, FOCUS_FORWARD, FOCUS_BACKWARD}.");
            }
        }
        int i2 = iWidth2 - iWidth;
        int i3 = iHeight2 - iHeight;
        return (i3 * i3) + (i2 * i2);
    }

    @Override
    protected boolean isInFilterMode() {
        return this.mFiltered;
    }

    boolean sendToTextFilter(int i, int i2, KeyEvent keyEvent) {
        boolean z;
        boolean z2;
        boolean z3;
        if (!acceptFilter()) {
            return false;
        }
        if (i != 4) {
            if (i == 62) {
                z3 = this.mFiltered;
                z2 = false;
            } else if (i != 66) {
                switch (i) {
                    case 19:
                    case 20:
                    case 21:
                    case 22:
                    case 23:
                        z3 = false;
                        z2 = false;
                        break;
                    default:
                        z2 = false;
                        z3 = true;
                        break;
                }
            }
        } else if (this.mFiltered && this.mPopup != null && this.mPopup.isShowing()) {
            if (keyEvent.getAction() == 0 && keyEvent.getRepeatCount() == 0) {
                KeyEvent.DispatcherState keyDispatcherState = getKeyDispatcherState();
                if (keyDispatcherState != null) {
                    keyDispatcherState.startTracking(keyEvent, this);
                }
            } else {
                if (keyEvent.getAction() == 1 && keyEvent.isTracking() && !keyEvent.isCanceled()) {
                    this.mTextFilter.setText("");
                }
                z = false;
                z2 = z;
                z3 = false;
            }
            z = true;
            z2 = z;
            z3 = false;
        } else {
            z = false;
            z2 = z;
            z3 = false;
        }
        if (!z3) {
            return z2;
        }
        createTextFilter(true);
        KeyEvent keyEventChangeTimeRepeat = keyEvent.getRepeatCount() > 0 ? KeyEvent.changeTimeRepeat(keyEvent, keyEvent.getEventTime(), 0) : keyEvent;
        switch (keyEvent.getAction()) {
            case 0:
                return this.mTextFilter.onKeyDown(i, keyEventChangeTimeRepeat);
            case 1:
                return this.mTextFilter.onKeyUp(i, keyEventChangeTimeRepeat);
            case 2:
                return this.mTextFilter.onKeyMultiple(i, i2, keyEvent);
            default:
                return z2;
        }
    }

    @Override
    public InputConnection onCreateInputConnection(EditorInfo editorInfo) {
        if (isTextFilterEnabled()) {
            if (this.mPublicInputConnection == null) {
                this.mDefInputConnection = new BaseInputConnection((View) this, false);
                this.mPublicInputConnection = new InputConnectionWrapper(editorInfo);
            }
            editorInfo.inputType = 177;
            editorInfo.imeOptions = 6;
            return this.mPublicInputConnection;
        }
        return null;
    }

    private class InputConnectionWrapper implements InputConnection {
        private final EditorInfo mOutAttrs;
        private InputConnection mTarget;

        public InputConnectionWrapper(EditorInfo editorInfo) {
            this.mOutAttrs = editorInfo;
        }

        private InputConnection getTarget() {
            if (this.mTarget == null) {
                this.mTarget = AbsListView.this.getTextFilterInput().onCreateInputConnection(this.mOutAttrs);
            }
            return this.mTarget;
        }

        @Override
        public boolean reportFullscreenMode(boolean z) {
            return AbsListView.this.mDefInputConnection.reportFullscreenMode(z);
        }

        @Override
        public boolean performEditorAction(int i) {
            if (i != 6) {
                return false;
            }
            InputMethodManager inputMethodManager = (InputMethodManager) AbsListView.this.getContext().getSystemService(InputMethodManager.class);
            if (inputMethodManager != null) {
                inputMethodManager.hideSoftInputFromWindow(AbsListView.this.getWindowToken(), 0);
                return true;
            }
            return true;
        }

        @Override
        public boolean sendKeyEvent(KeyEvent keyEvent) {
            return AbsListView.this.mDefInputConnection.sendKeyEvent(keyEvent);
        }

        @Override
        public CharSequence getTextBeforeCursor(int i, int i2) {
            return this.mTarget == null ? "" : this.mTarget.getTextBeforeCursor(i, i2);
        }

        @Override
        public CharSequence getTextAfterCursor(int i, int i2) {
            return this.mTarget == null ? "" : this.mTarget.getTextAfterCursor(i, i2);
        }

        @Override
        public CharSequence getSelectedText(int i) {
            return this.mTarget == null ? "" : this.mTarget.getSelectedText(i);
        }

        @Override
        public int getCursorCapsMode(int i) {
            if (this.mTarget == null) {
                return 16384;
            }
            return this.mTarget.getCursorCapsMode(i);
        }

        @Override
        public ExtractedText getExtractedText(ExtractedTextRequest extractedTextRequest, int i) {
            return getTarget().getExtractedText(extractedTextRequest, i);
        }

        @Override
        public boolean deleteSurroundingText(int i, int i2) {
            return getTarget().deleteSurroundingText(i, i2);
        }

        @Override
        public boolean deleteSurroundingTextInCodePoints(int i, int i2) {
            return getTarget().deleteSurroundingTextInCodePoints(i, i2);
        }

        @Override
        public boolean setComposingText(CharSequence charSequence, int i) {
            return getTarget().setComposingText(charSequence, i);
        }

        @Override
        public boolean setComposingRegion(int i, int i2) {
            return getTarget().setComposingRegion(i, i2);
        }

        @Override
        public boolean finishComposingText() {
            return this.mTarget == null || this.mTarget.finishComposingText();
        }

        @Override
        public boolean commitText(CharSequence charSequence, int i) {
            return getTarget().commitText(charSequence, i);
        }

        @Override
        public boolean commitCompletion(CompletionInfo completionInfo) {
            return getTarget().commitCompletion(completionInfo);
        }

        @Override
        public boolean commitCorrection(CorrectionInfo correctionInfo) {
            return getTarget().commitCorrection(correctionInfo);
        }

        @Override
        public boolean setSelection(int i, int i2) {
            return getTarget().setSelection(i, i2);
        }

        @Override
        public boolean performContextMenuAction(int i) {
            return getTarget().performContextMenuAction(i);
        }

        @Override
        public boolean beginBatchEdit() {
            return getTarget().beginBatchEdit();
        }

        @Override
        public boolean endBatchEdit() {
            return getTarget().endBatchEdit();
        }

        @Override
        public boolean clearMetaKeyStates(int i) {
            return getTarget().clearMetaKeyStates(i);
        }

        @Override
        public boolean performPrivateCommand(String str, Bundle bundle) {
            return getTarget().performPrivateCommand(str, bundle);
        }

        @Override
        public boolean requestCursorUpdates(int i) {
            return getTarget().requestCursorUpdates(i);
        }

        @Override
        public Handler getHandler() {
            return getTarget().getHandler();
        }

        @Override
        public void closeConnection() {
            getTarget().closeConnection();
        }

        @Override
        public boolean commitContent(InputContentInfo inputContentInfo, int i, Bundle bundle) {
            return getTarget().commitContent(inputContentInfo, i, bundle);
        }
    }

    @Override
    public boolean checkInputConnectionProxy(View view) {
        return view == this.mTextFilter;
    }

    private void createTextFilter(boolean z) {
        if (this.mPopup == null) {
            PopupWindow popupWindow = new PopupWindow(getContext());
            popupWindow.setFocusable(false);
            popupWindow.setTouchable(false);
            popupWindow.setInputMethodMode(2);
            popupWindow.setContentView(getTextFilterInput());
            popupWindow.setWidth(-2);
            popupWindow.setHeight(-2);
            popupWindow.setBackgroundDrawable(null);
            this.mPopup = popupWindow;
            getViewTreeObserver().addOnGlobalLayoutListener(this);
            this.mGlobalLayoutListenerAddedFilter = true;
        }
        if (z) {
            this.mPopup.setAnimationStyle(R.style.Animation_TypingFilter);
        } else {
            this.mPopup.setAnimationStyle(R.style.Animation_TypingFilterRestore);
        }
    }

    private EditText getTextFilterInput() {
        if (this.mTextFilter == null) {
            this.mTextFilter = (EditText) LayoutInflater.from(getContext()).inflate(R.layout.typing_filter, (ViewGroup) null);
            this.mTextFilter.setRawInputType(177);
            this.mTextFilter.setImeOptions(268435456);
            this.mTextFilter.addTextChangedListener(this);
        }
        return this.mTextFilter;
    }

    public void clearTextFilter() {
        if (this.mFiltered) {
            getTextFilterInput().setText("");
            this.mFiltered = false;
            if (this.mPopup != null && this.mPopup.isShowing()) {
                dismissPopup();
            }
        }
    }

    public boolean hasTextFilter() {
        return this.mFiltered;
    }

    @Override
    public void onGlobalLayout() {
        if (isShown()) {
            if (this.mFiltered && this.mPopup != null && !this.mPopup.isShowing() && !this.mPopupHidden) {
                showPopup();
                return;
            }
            return;
        }
        if (this.mPopup != null && this.mPopup.isShowing()) {
            dismissPopup();
        }
    }

    @Override
    public void beforeTextChanged(CharSequence charSequence, int i, int i2, int i3) {
    }

    @Override
    public void onTextChanged(CharSequence charSequence, int i, int i2, int i3) {
        if (isTextFilterEnabled()) {
            createTextFilter(true);
            int length = charSequence.length();
            boolean zIsShowing = this.mPopup.isShowing();
            if (!zIsShowing && length > 0) {
                showPopup();
                this.mFiltered = true;
            } else if (zIsShowing && length == 0) {
                dismissPopup();
                this.mFiltered = false;
            }
            if (this.mAdapter instanceof Filterable) {
                Filter filter = ((Filterable) this.mAdapter).getFilter();
                if (filter != null) {
                    filter.filter(charSequence, this);
                    return;
                }
                throw new IllegalStateException("You cannot call onTextChanged with a non filterable adapter");
            }
        }
    }

    @Override
    public void afterTextChanged(Editable editable) {
    }

    @Override
    public void onFilterComplete(int i) {
        if (this.mSelectedPosition < 0 && i > 0) {
            this.mResurrectToPosition = -1;
            resurrectSelection();
        }
    }

    @Override
    protected ViewGroup.LayoutParams generateDefaultLayoutParams() {
        return new LayoutParams(-1, -2, 0);
    }

    @Override
    protected ViewGroup.LayoutParams generateLayoutParams(ViewGroup.LayoutParams layoutParams) {
        return new LayoutParams(layoutParams);
    }

    @Override
    public LayoutParams generateLayoutParams(AttributeSet attributeSet) {
        return new LayoutParams(getContext(), attributeSet);
    }

    @Override
    protected boolean checkLayoutParams(ViewGroup.LayoutParams layoutParams) {
        return layoutParams instanceof LayoutParams;
    }

    public void setTranscriptMode(int i) {
        this.mTranscriptMode = i;
    }

    public int getTranscriptMode() {
        return this.mTranscriptMode;
    }

    @Override
    public int getSolidColor() {
        return this.mCacheColorHint;
    }

    public void setCacheColorHint(int i) {
        if (i != this.mCacheColorHint) {
            this.mCacheColorHint = i;
            int childCount = getChildCount();
            for (int i2 = 0; i2 < childCount; i2++) {
                getChildAt(i2).setDrawingCacheBackgroundColor(i);
            }
            this.mRecycler.setCacheColorHint(i);
        }
    }

    @ViewDebug.ExportedProperty(category = "drawing")
    public int getCacheColorHint() {
        return this.mCacheColorHint;
    }

    public void reclaimViews(List<View> list) {
        int childCount = getChildCount();
        RecyclerListener recyclerListener = this.mRecycler.mRecyclerListener;
        for (int i = 0; i < childCount; i++) {
            View childAt = getChildAt(i);
            LayoutParams layoutParams = (LayoutParams) childAt.getLayoutParams();
            if (layoutParams != null && this.mRecycler.shouldRecycleViewType(layoutParams.viewType)) {
                list.add(childAt);
                childAt.setAccessibilityDelegate(null);
                if (recyclerListener != null) {
                    recyclerListener.onMovedToScrapHeap(childAt);
                }
            }
        }
        this.mRecycler.reclaimScrapViews(list);
        removeAllViewsInLayout();
    }

    private void finishGlows() {
        if (this.mEdgeGlowTop != null) {
            this.mEdgeGlowTop.finish();
            this.mEdgeGlowBottom.finish();
        }
    }

    public void setRemoteViewsAdapter(Intent intent) {
        setRemoteViewsAdapter(intent, false);
    }

    public Runnable setRemoteViewsAdapterAsync(Intent intent) {
        return new RemoteViewsAdapter.AsyncRemoteAdapterAction(this, intent);
    }

    @Override
    public void setRemoteViewsAdapter(Intent intent, boolean z) {
        if (this.mRemoteAdapter != null && new Intent.FilterComparison(intent).equals(new Intent.FilterComparison(this.mRemoteAdapter.getRemoteViewsServiceIntent()))) {
            return;
        }
        this.mDeferNotifyDataSetChanged = false;
        this.mRemoteAdapter = new RemoteViewsAdapter(getContext(), intent, this, z);
        if (this.mRemoteAdapter.isDataReady()) {
            setAdapter((ListAdapter) this.mRemoteAdapter);
        }
    }

    public void setRemoteViewsOnClickHandler(RemoteViews.OnClickHandler onClickHandler) {
        if (this.mRemoteAdapter != null) {
            this.mRemoteAdapter.setRemoteViewsOnClickHandler(onClickHandler);
        }
    }

    @Override
    public void deferNotifyDataSetChanged() {
        this.mDeferNotifyDataSetChanged = true;
    }

    @Override
    public boolean onRemoteAdapterConnected() {
        if (this.mRemoteAdapter != this.mAdapter) {
            setAdapter((ListAdapter) this.mRemoteAdapter);
            if (this.mDeferNotifyDataSetChanged) {
                this.mRemoteAdapter.notifyDataSetChanged();
                this.mDeferNotifyDataSetChanged = false;
            }
            return false;
        }
        if (this.mRemoteAdapter == null) {
            return false;
        }
        this.mRemoteAdapter.superNotifyDataSetChanged();
        return true;
    }

    @Override
    public void onRemoteAdapterDisconnected() {
    }

    void setVisibleRangeHint(int i, int i2) {
        if (this.mRemoteAdapter != null) {
            this.mRemoteAdapter.setVisibleRangeHint(i, i2);
        }
    }

    public void setRecyclerListener(RecyclerListener recyclerListener) {
        this.mRecycler.mRecyclerListener = recyclerListener;
    }

    class AdapterDataSetObserver extends AdapterView<ListAdapter>.AdapterDataSetObserver {
        AdapterDataSetObserver() {
            super();
        }

        @Override
        public void onChanged() {
            super.onChanged();
            if (AbsListView.this.mFastScroll != null) {
                AbsListView.this.mFastScroll.onSectionsChanged();
            }
        }

        @Override
        public void onInvalidated() {
            super.onInvalidated();
            if (AbsListView.this.mFastScroll != null) {
                AbsListView.this.mFastScroll.onSectionsChanged();
            }
        }
    }

    class MultiChoiceModeWrapper implements MultiChoiceModeListener {
        private MultiChoiceModeListener mWrapped;

        MultiChoiceModeWrapper() {
        }

        public void setWrapped(MultiChoiceModeListener multiChoiceModeListener) {
            this.mWrapped = multiChoiceModeListener;
        }

        public boolean hasWrappedCallback() {
            return this.mWrapped != null;
        }

        @Override
        public boolean onCreateActionMode(ActionMode actionMode, Menu menu) {
            if (!this.mWrapped.onCreateActionMode(actionMode, menu)) {
                return false;
            }
            AbsListView.this.setLongClickable(false);
            return true;
        }

        @Override
        public boolean onPrepareActionMode(ActionMode actionMode, Menu menu) {
            return this.mWrapped.onPrepareActionMode(actionMode, menu);
        }

        @Override
        public boolean onActionItemClicked(ActionMode actionMode, MenuItem menuItem) {
            return this.mWrapped.onActionItemClicked(actionMode, menuItem);
        }

        @Override
        public void onDestroyActionMode(ActionMode actionMode) {
            this.mWrapped.onDestroyActionMode(actionMode);
            AbsListView.this.mChoiceActionMode = null;
            AbsListView.this.clearChoices();
            AbsListView.this.mDataChanged = true;
            AbsListView.this.rememberSyncState();
            AbsListView.this.requestLayout();
            AbsListView.this.setLongClickable(true);
        }

        @Override
        public void onItemCheckedStateChanged(ActionMode actionMode, int i, long j, boolean z) {
            this.mWrapped.onItemCheckedStateChanged(actionMode, i, j, z);
            if (AbsListView.this.getCheckedItemCount() == 0) {
                actionMode.finish();
            }
        }
    }

    public static class LayoutParams extends ViewGroup.LayoutParams {

        @ViewDebug.ExportedProperty(category = Slice.HINT_LIST)
        boolean forceAdd;
        boolean isEnabled;
        long itemId;

        @ViewDebug.ExportedProperty(category = Slice.HINT_LIST)
        boolean recycledHeaderFooter;
        int scrappedFromPosition;

        @ViewDebug.ExportedProperty(category = Slice.HINT_LIST, mapping = {@ViewDebug.IntToString(from = -1, to = "ITEM_VIEW_TYPE_IGNORE"), @ViewDebug.IntToString(from = -2, to = "ITEM_VIEW_TYPE_HEADER_OR_FOOTER")})
        int viewType;

        public LayoutParams(Context context, AttributeSet attributeSet) {
            super(context, attributeSet);
            this.itemId = -1L;
        }

        public LayoutParams(int i, int i2) {
            super(i, i2);
            this.itemId = -1L;
        }

        public LayoutParams(int i, int i2, int i3) {
            super(i, i2);
            this.itemId = -1L;
            this.viewType = i3;
        }

        public LayoutParams(ViewGroup.LayoutParams layoutParams) {
            super(layoutParams);
            this.itemId = -1L;
        }

        @Override
        protected void encodeProperties(ViewHierarchyEncoder viewHierarchyEncoder) {
            super.encodeProperties(viewHierarchyEncoder);
            viewHierarchyEncoder.addProperty("list:viewType", this.viewType);
            viewHierarchyEncoder.addProperty("list:recycledHeaderFooter", this.recycledHeaderFooter);
            viewHierarchyEncoder.addProperty("list:forceAdd", this.forceAdd);
            viewHierarchyEncoder.addProperty("list:isEnabled", this.isEnabled);
        }
    }

    class RecycleBin {
        private View[] mActiveViews = new View[0];
        private ArrayList<View> mCurrentScrap;
        private int mFirstActivePosition;
        private RecyclerListener mRecyclerListener;
        private ArrayList<View>[] mScrapViews;
        private ArrayList<View> mSkippedScrap;
        private SparseArray<View> mTransientStateViews;
        private LongSparseArray<View> mTransientStateViewsById;
        private int mViewTypeCount;

        RecycleBin() {
        }

        public void setViewTypeCount(int i) {
            if (i < 1) {
                throw new IllegalArgumentException("Can't have a viewTypeCount < 1");
            }
            ArrayList<View>[] arrayListArr = new ArrayList[i];
            for (int i2 = 0; i2 < i; i2++) {
                arrayListArr[i2] = new ArrayList<>();
            }
            this.mViewTypeCount = i;
            this.mCurrentScrap = arrayListArr[0];
            this.mScrapViews = arrayListArr;
        }

        public void markChildrenDirty() {
            if (this.mViewTypeCount == 1) {
                ArrayList<View> arrayList = this.mCurrentScrap;
                int size = arrayList.size();
                for (int i = 0; i < size; i++) {
                    arrayList.get(i).forceLayout();
                }
            } else {
                int i2 = this.mViewTypeCount;
                for (int i3 = 0; i3 < i2; i3++) {
                    ArrayList<View> arrayList2 = this.mScrapViews[i3];
                    int size2 = arrayList2.size();
                    for (int i4 = 0; i4 < size2; i4++) {
                        arrayList2.get(i4).forceLayout();
                    }
                }
            }
            if (this.mTransientStateViews != null) {
                int size3 = this.mTransientStateViews.size();
                for (int i5 = 0; i5 < size3; i5++) {
                    this.mTransientStateViews.valueAt(i5).forceLayout();
                }
            }
            if (this.mTransientStateViewsById != null) {
                int size4 = this.mTransientStateViewsById.size();
                for (int i6 = 0; i6 < size4; i6++) {
                    this.mTransientStateViewsById.valueAt(i6).forceLayout();
                }
            }
        }

        public boolean shouldRecycleViewType(int i) {
            return i >= 0;
        }

        void clear() {
            if (this.mViewTypeCount == 1) {
                clearScrap(this.mCurrentScrap);
            } else {
                int i = this.mViewTypeCount;
                for (int i2 = 0; i2 < i; i2++) {
                    clearScrap(this.mScrapViews[i2]);
                }
            }
            clearTransientStateViews();
        }

        void fillActiveViews(int i, int i2) {
            if (this.mActiveViews.length < i) {
                this.mActiveViews = new View[i];
            }
            this.mFirstActivePosition = i2;
            View[] viewArr = this.mActiveViews;
            for (int i3 = 0; i3 < i; i3++) {
                View childAt = AbsListView.this.getChildAt(i3);
                LayoutParams layoutParams = (LayoutParams) childAt.getLayoutParams();
                if (layoutParams != null && layoutParams.viewType != -2) {
                    viewArr[i3] = childAt;
                    layoutParams.scrappedFromPosition = i2 + i3;
                }
            }
        }

        View getActiveView(int i) {
            int i2 = i - this.mFirstActivePosition;
            View[] viewArr = this.mActiveViews;
            if (i2 < 0 || i2 >= viewArr.length) {
                return null;
            }
            View view = viewArr[i2];
            viewArr[i2] = null;
            return view;
        }

        View getTransientStateView(int i) {
            int iIndexOfKey;
            if (AbsListView.this.mAdapter != null && AbsListView.this.mAdapterHasStableIds && this.mTransientStateViewsById != null) {
                long itemId = AbsListView.this.mAdapter.getItemId(i);
                View view = this.mTransientStateViewsById.get(itemId);
                this.mTransientStateViewsById.remove(itemId);
                return view;
            }
            if (this.mTransientStateViews != null && (iIndexOfKey = this.mTransientStateViews.indexOfKey(i)) >= 0) {
                View viewValueAt = this.mTransientStateViews.valueAt(iIndexOfKey);
                this.mTransientStateViews.removeAt(iIndexOfKey);
                return viewValueAt;
            }
            return null;
        }

        void clearTransientStateViews() {
            SparseArray<View> sparseArray = this.mTransientStateViews;
            if (sparseArray != null) {
                int size = sparseArray.size();
                for (int i = 0; i < size; i++) {
                    removeDetachedView(sparseArray.valueAt(i), false);
                }
                sparseArray.clear();
            }
            LongSparseArray<View> longSparseArray = this.mTransientStateViewsById;
            if (longSparseArray != null) {
                int size2 = longSparseArray.size();
                for (int i2 = 0; i2 < size2; i2++) {
                    removeDetachedView(longSparseArray.valueAt(i2), false);
                }
                longSparseArray.clear();
            }
        }

        View getScrapView(int i) {
            int itemViewType = AbsListView.this.mAdapter.getItemViewType(i);
            if (itemViewType < 0) {
                return null;
            }
            if (this.mViewTypeCount == 1) {
                return retrieveFromScrap(this.mCurrentScrap, i);
            }
            if (itemViewType >= this.mScrapViews.length) {
                return null;
            }
            return retrieveFromScrap(this.mScrapViews[itemViewType], i);
        }

        void addScrapView(View view, int i) {
            LayoutParams layoutParams = (LayoutParams) view.getLayoutParams();
            if (layoutParams == null) {
                return;
            }
            layoutParams.scrappedFromPosition = i;
            int i2 = layoutParams.viewType;
            if (!shouldRecycleViewType(i2)) {
                if (i2 != -2) {
                    getSkippedScrap().add(view);
                    return;
                }
                return;
            }
            view.dispatchStartTemporaryDetach();
            AbsListView.this.notifyViewAccessibilityStateChangedIfNeeded(1);
            if (view.hasTransientState()) {
                if (AbsListView.this.mAdapter != null && AbsListView.this.mAdapterHasStableIds) {
                    if (this.mTransientStateViewsById == null) {
                        this.mTransientStateViewsById = new LongSparseArray<>();
                    }
                    this.mTransientStateViewsById.put(layoutParams.itemId, view);
                    return;
                } else if (!AbsListView.this.mDataChanged) {
                    if (this.mTransientStateViews == null) {
                        this.mTransientStateViews = new SparseArray<>();
                    }
                    this.mTransientStateViews.put(i, view);
                    return;
                } else {
                    clearScrapForRebind(view);
                    getSkippedScrap().add(view);
                    return;
                }
            }
            clearScrapForRebind(view);
            if (this.mViewTypeCount == 1) {
                this.mCurrentScrap.add(view);
            } else {
                this.mScrapViews[i2].add(view);
            }
            if (this.mRecyclerListener != null) {
                this.mRecyclerListener.onMovedToScrapHeap(view);
            }
        }

        private ArrayList<View> getSkippedScrap() {
            if (this.mSkippedScrap == null) {
                this.mSkippedScrap = new ArrayList<>();
            }
            return this.mSkippedScrap;
        }

        void removeSkippedScrap() {
            if (this.mSkippedScrap == null) {
                return;
            }
            int size = this.mSkippedScrap.size();
            for (int i = 0; i < size; i++) {
                removeDetachedView(this.mSkippedScrap.get(i), false);
            }
            this.mSkippedScrap.clear();
        }

        void scrapActiveViews() {
            View[] viewArr = this.mActiveViews;
            boolean z = this.mRecyclerListener != null;
            boolean z2 = this.mViewTypeCount > 1;
            ArrayList<View> arrayList = this.mCurrentScrap;
            for (int length = viewArr.length - 1; length >= 0; length--) {
                View view = viewArr[length];
                if (view != null) {
                    LayoutParams layoutParams = (LayoutParams) view.getLayoutParams();
                    int i = layoutParams.viewType;
                    viewArr[length] = null;
                    if (view.hasTransientState()) {
                        view.dispatchStartTemporaryDetach();
                        if (AbsListView.this.mAdapter != null && AbsListView.this.mAdapterHasStableIds) {
                            if (this.mTransientStateViewsById == null) {
                                this.mTransientStateViewsById = new LongSparseArray<>();
                            }
                            this.mTransientStateViewsById.put(AbsListView.this.mAdapter.getItemId(this.mFirstActivePosition + length), view);
                        } else if (!AbsListView.this.mDataChanged) {
                            if (this.mTransientStateViews == null) {
                                this.mTransientStateViews = new SparseArray<>();
                            }
                            this.mTransientStateViews.put(this.mFirstActivePosition + length, view);
                        } else if (i != -2) {
                            removeDetachedView(view, false);
                        }
                    } else if (!shouldRecycleViewType(i)) {
                        if (i != -2) {
                            removeDetachedView(view, false);
                        }
                    } else {
                        if (z2) {
                            arrayList = this.mScrapViews[i];
                        }
                        layoutParams.scrappedFromPosition = this.mFirstActivePosition + length;
                        removeDetachedView(view, false);
                        arrayList.add(view);
                        if (z) {
                            this.mRecyclerListener.onMovedToScrapHeap(view);
                        }
                    }
                }
            }
            pruneScrapViews();
        }

        void fullyDetachScrapViews() {
            int i = this.mViewTypeCount;
            ArrayList<View>[] arrayListArr = this.mScrapViews;
            for (int i2 = 0; i2 < i; i2++) {
                ArrayList<View> arrayList = arrayListArr[i2];
                for (int size = arrayList.size() - 1; size >= 0; size--) {
                    View view = arrayList.get(size);
                    if (view.isTemporarilyDetached()) {
                        removeDetachedView(view, false);
                    }
                }
            }
        }

        private void pruneScrapViews() {
            int length = this.mActiveViews.length;
            int i = this.mViewTypeCount;
            ArrayList<View>[] arrayListArr = this.mScrapViews;
            for (int i2 = 0; i2 < i; i2++) {
                ArrayList<View> arrayList = arrayListArr[i2];
                int size = arrayList.size();
                while (size > length) {
                    size--;
                    arrayList.remove(size);
                }
            }
            SparseArray<View> sparseArray = this.mTransientStateViews;
            if (sparseArray != null) {
                int i3 = 0;
                while (i3 < sparseArray.size()) {
                    View viewValueAt = sparseArray.valueAt(i3);
                    if (!viewValueAt.hasTransientState()) {
                        removeDetachedView(viewValueAt, false);
                        sparseArray.removeAt(i3);
                        i3--;
                    }
                    i3++;
                }
            }
            LongSparseArray<View> longSparseArray = this.mTransientStateViewsById;
            if (longSparseArray != null) {
                int i4 = 0;
                while (i4 < longSparseArray.size()) {
                    View viewValueAt2 = longSparseArray.valueAt(i4);
                    if (!viewValueAt2.hasTransientState()) {
                        removeDetachedView(viewValueAt2, false);
                        longSparseArray.removeAt(i4);
                        i4--;
                    }
                    i4++;
                }
            }
        }

        void reclaimScrapViews(List<View> list) {
            if (this.mViewTypeCount == 1) {
                list.addAll(this.mCurrentScrap);
                return;
            }
            int i = this.mViewTypeCount;
            ArrayList<View>[] arrayListArr = this.mScrapViews;
            for (int i2 = 0; i2 < i; i2++) {
                list.addAll(arrayListArr[i2]);
            }
        }

        void setCacheColorHint(int i) {
            if (this.mViewTypeCount == 1) {
                ArrayList<View> arrayList = this.mCurrentScrap;
                int size = arrayList.size();
                for (int i2 = 0; i2 < size; i2++) {
                    arrayList.get(i2).setDrawingCacheBackgroundColor(i);
                }
            } else {
                int i3 = this.mViewTypeCount;
                for (int i4 = 0; i4 < i3; i4++) {
                    ArrayList<View> arrayList2 = this.mScrapViews[i4];
                    int size2 = arrayList2.size();
                    for (int i5 = 0; i5 < size2; i5++) {
                        arrayList2.get(i5).setDrawingCacheBackgroundColor(i);
                    }
                }
            }
            for (View view : this.mActiveViews) {
                if (view != null) {
                    view.setDrawingCacheBackgroundColor(i);
                }
            }
        }

        private View retrieveFromScrap(ArrayList<View> arrayList, int i) {
            int size = arrayList.size();
            if (size > 0) {
                int i2 = size - 1;
                for (int i3 = i2; i3 >= 0; i3--) {
                    LayoutParams layoutParams = (LayoutParams) arrayList.get(i3).getLayoutParams();
                    if (AbsListView.this.mAdapterHasStableIds) {
                        if (AbsListView.this.mAdapter.getItemId(i) == layoutParams.itemId) {
                            return arrayList.remove(i3);
                        }
                    } else if (layoutParams.scrappedFromPosition == i) {
                        View viewRemove = arrayList.remove(i3);
                        clearScrapForRebind(viewRemove);
                        return viewRemove;
                    }
                }
                View viewRemove2 = arrayList.remove(i2);
                clearScrapForRebind(viewRemove2);
                return viewRemove2;
            }
            return null;
        }

        private void clearScrap(ArrayList<View> arrayList) {
            int size = arrayList.size();
            for (int i = 0; i < size; i++) {
                removeDetachedView(arrayList.remove((size - 1) - i), false);
            }
        }

        private void clearScrapForRebind(View view) {
            view.clearAccessibilityFocus();
            view.setAccessibilityDelegate(null);
        }

        private void removeDetachedView(View view, boolean z) {
            view.setAccessibilityDelegate(null);
            AbsListView.this.removeDetachedView(view, z);
        }
    }

    int getHeightForPosition(int i) {
        int firstVisiblePosition = getFirstVisiblePosition();
        int childCount = getChildCount();
        int i2 = i - firstVisiblePosition;
        if (i2 >= 0 && i2 < childCount) {
            return getChildAt(i2).getHeight();
        }
        View viewObtainView = obtainView(i, this.mIsScrap);
        viewObtainView.measure(this.mWidthMeasureSpec, 0);
        int measuredHeight = viewObtainView.getMeasuredHeight();
        this.mRecycler.addScrapView(viewObtainView, i);
        return measuredHeight;
    }

    public void setSelectionFromTop(int i, int i2) {
        if (this.mAdapter == null) {
            return;
        }
        if (!isInTouchMode()) {
            i = lookForSelectablePosition(i, true);
            if (i >= 0) {
                setNextSelectedPositionInt(i);
            }
        } else {
            this.mResurrectToPosition = i;
        }
        if (i >= 0) {
            this.mLayoutMode = 4;
            this.mSpecificTop = this.mListPadding.top + i2;
            if (this.mNeedSync) {
                this.mSyncPosition = i;
                this.mSyncRowId = this.mAdapter.getItemId(i);
            }
            if (this.mPositionScroller != null) {
                this.mPositionScroller.stop();
            }
            requestLayout();
        }
    }

    @Override
    protected void encodeProperties(ViewHierarchyEncoder viewHierarchyEncoder) {
        super.encodeProperties(viewHierarchyEncoder);
        viewHierarchyEncoder.addProperty("drawing:cacheColorHint", getCacheColorHint());
        viewHierarchyEncoder.addProperty("list:fastScrollEnabled", isFastScrollEnabled());
        viewHierarchyEncoder.addProperty("list:scrollingCacheEnabled", isScrollingCacheEnabled());
        viewHierarchyEncoder.addProperty("list:smoothScrollbarEnabled", isSmoothScrollbarEnabled());
        viewHierarchyEncoder.addProperty("list:stackFromBottom", isStackFromBottom());
        viewHierarchyEncoder.addProperty("list:textFilterEnabled", isTextFilterEnabled());
        View selectedView = getSelectedView();
        if (selectedView != null) {
            viewHierarchyEncoder.addPropertyKey("selectedView");
            selectedView.encode(viewHierarchyEncoder);
        }
    }

    static abstract class AbsPositionScroller {
        public abstract void start(int i);

        public abstract void start(int i, int i2);

        public abstract void startWithOffset(int i, int i2);

        public abstract void startWithOffset(int i, int i2, int i3);

        public abstract void stop();

        AbsPositionScroller() {
        }
    }

    class PositionScroller extends AbsPositionScroller implements Runnable {
        private static final int MOVE_DOWN_BOUND = 3;
        private static final int MOVE_DOWN_POS = 1;
        private static final int MOVE_OFFSET = 5;
        private static final int MOVE_UP_BOUND = 4;
        private static final int MOVE_UP_POS = 2;
        private static final int SCROLL_DURATION = 200;
        private int mBoundPos;
        private final int mExtraScroll;
        private int mLastSeenPos;
        private int mMode;
        private int mOffsetFromTop;
        private int mScrollDuration;
        private int mTargetPos;

        PositionScroller() {
            this.mExtraScroll = ViewConfiguration.get(AbsListView.this.mContext).getScaledFadingEdgeLength();
        }

        @Override
        public void start(final int i) {
            int i2;
            stop();
            if (AbsListView.this.mDataChanged) {
                AbsListView.this.mPositionScrollAfterLayout = new Runnable() {
                    @Override
                    public void run() {
                        PositionScroller.this.start(i);
                    }
                };
                return;
            }
            int childCount = AbsListView.this.getChildCount();
            if (childCount == 0) {
                return;
            }
            int i3 = AbsListView.this.mFirstPosition;
            int i4 = (childCount + i3) - 1;
            int iMax = Math.max(0, Math.min(AbsListView.this.getCount() - 1, i));
            if (iMax < i3) {
                i2 = (i3 - iMax) + 1;
                this.mMode = 2;
            } else if (iMax > i4) {
                i2 = (iMax - i4) + 1;
                this.mMode = 1;
            } else {
                scrollToVisible(iMax, -1, 200);
                return;
            }
            if (i2 > 0) {
                this.mScrollDuration = 200 / i2;
            } else {
                this.mScrollDuration = 200;
            }
            this.mTargetPos = iMax;
            this.mBoundPos = -1;
            this.mLastSeenPos = -1;
            AbsListView.this.postOnAnimation(this);
        }

        @Override
        public void start(final int i, final int i2) {
            int i3;
            stop();
            if (i2 == -1) {
                start(i);
                return;
            }
            if (AbsListView.this.mDataChanged) {
                AbsListView.this.mPositionScrollAfterLayout = new Runnable() {
                    @Override
                    public void run() {
                        PositionScroller.this.start(i, i2);
                    }
                };
                return;
            }
            int childCount = AbsListView.this.getChildCount();
            if (childCount == 0) {
                return;
            }
            int i4 = AbsListView.this.mFirstPosition;
            int i5 = (childCount + i4) - 1;
            int iMax = Math.max(0, Math.min(AbsListView.this.getCount() - 1, i));
            if (iMax < i4) {
                int i6 = i5 - i2;
                if (i6 < 1) {
                    return;
                }
                int i7 = (i4 - iMax) + 1;
                i3 = i6 - 1;
                if (i3 < i7) {
                    this.mMode = 4;
                } else {
                    this.mMode = 2;
                    i3 = i7;
                }
            } else if (iMax > i5) {
                int i8 = i2 - i4;
                if (i8 < 1) {
                    return;
                }
                i3 = (iMax - i5) + 1;
                int i9 = i8 - 1;
                if (i9 < i3) {
                    this.mMode = 3;
                    i3 = i9;
                } else {
                    this.mMode = 1;
                }
            } else {
                scrollToVisible(iMax, i2, 200);
                return;
            }
            if (i3 > 0) {
                this.mScrollDuration = 200 / i3;
            } else {
                this.mScrollDuration = 200;
            }
            this.mTargetPos = iMax;
            this.mBoundPos = i2;
            this.mLastSeenPos = -1;
            AbsListView.this.postOnAnimation(this);
        }

        @Override
        public void startWithOffset(int i, int i2) {
            startWithOffset(i, i2, 200);
        }

        @Override
        public void startWithOffset(final int i, final int i2, final int i3) {
            int i4;
            stop();
            if (AbsListView.this.mDataChanged) {
                AbsListView.this.mPositionScrollAfterLayout = new Runnable() {
                    @Override
                    public void run() {
                        PositionScroller.this.startWithOffset(i, i2, i3);
                    }
                };
                return;
            }
            int childCount = AbsListView.this.getChildCount();
            if (childCount == 0) {
                return;
            }
            int paddingTop = i2 + AbsListView.this.getPaddingTop();
            this.mTargetPos = Math.max(0, Math.min(AbsListView.this.getCount() - 1, i));
            this.mOffsetFromTop = paddingTop;
            this.mBoundPos = -1;
            this.mLastSeenPos = -1;
            this.mMode = 5;
            int i5 = AbsListView.this.mFirstPosition;
            int i6 = (i5 + childCount) - 1;
            if (this.mTargetPos < i5) {
                i4 = i5 - this.mTargetPos;
            } else if (this.mTargetPos > i6) {
                i4 = this.mTargetPos - i6;
            } else {
                AbsListView.this.smoothScrollBy(AbsListView.this.getChildAt(this.mTargetPos - i5).getTop() - paddingTop, i3, true, false);
                return;
            }
            float f = i4 / childCount;
            if (f >= 1.0f) {
                i3 = (int) (i3 / f);
            }
            this.mScrollDuration = i3;
            this.mLastSeenPos = -1;
            AbsListView.this.postOnAnimation(this);
        }

        private void scrollToVisible(int i, int i2, int i3) {
            int iMin;
            int i4 = AbsListView.this.mFirstPosition;
            int childCount = (AbsListView.this.getChildCount() + i4) - 1;
            int i5 = AbsListView.this.mListPadding.top;
            int height = AbsListView.this.getHeight() - AbsListView.this.mListPadding.bottom;
            if (i < i4 || i > childCount) {
                Log.w(AbsListView.TAG, "scrollToVisible called with targetPos " + i + " not visible [" + i4 + ", " + childCount + "]");
            }
            if (i2 < i4 || i2 > childCount) {
                i2 = -1;
            }
            View childAt = AbsListView.this.getChildAt(i - i4);
            int top = childAt.getTop();
            int bottom = childAt.getBottom();
            if (bottom > height) {
                iMin = bottom - height;
            } else {
                iMin = 0;
            }
            if (top < i5) {
                iMin = top - i5;
            }
            if (iMin == 0) {
                return;
            }
            if (i2 >= 0) {
                View childAt2 = AbsListView.this.getChildAt(i2 - i4);
                int top2 = childAt2.getTop();
                int bottom2 = childAt2.getBottom();
                int iAbs = Math.abs(iMin);
                if (iMin < 0 && bottom2 + iAbs > height) {
                    iMin = Math.max(0, bottom2 - height);
                } else if (iMin > 0 && top2 - iAbs < i5) {
                    iMin = Math.min(0, top2 - i5);
                }
            }
            AbsListView.this.smoothScrollBy(iMin, i3);
        }

        @Override
        public void stop() {
            AbsListView.this.removeCallbacks(this);
        }

        @Override
        public void run() {
            float top;
            float height;
            int height2 = AbsListView.this.getHeight();
            int i = AbsListView.this.mFirstPosition;
            switch (this.mMode) {
                case 1:
                    int childCount = AbsListView.this.getChildCount() - 1;
                    int i2 = i + childCount;
                    if (childCount >= 0) {
                        if (i2 == this.mLastSeenPos) {
                            AbsListView.this.postOnAnimation(this);
                        } else {
                            View childAt = AbsListView.this.getChildAt(childCount);
                            AbsListView.this.smoothScrollBy((childAt.getHeight() - (height2 - childAt.getTop())) + (i2 < AbsListView.this.mItemCount - 1 ? Math.max(AbsListView.this.mListPadding.bottom, this.mExtraScroll) : AbsListView.this.mListPadding.bottom), this.mScrollDuration, true, i2 < this.mTargetPos);
                            this.mLastSeenPos = i2;
                            if (i2 < this.mTargetPos) {
                                AbsListView.this.postOnAnimation(this);
                            }
                        }
                        break;
                    }
                    break;
                case 2:
                    if (i == this.mLastSeenPos) {
                        AbsListView.this.postOnAnimation(this);
                        break;
                    } else {
                        View childAt2 = AbsListView.this.getChildAt(0);
                        if (childAt2 != null) {
                            AbsListView.this.smoothScrollBy(childAt2.getTop() - (i > 0 ? Math.max(this.mExtraScroll, AbsListView.this.mListPadding.top) : AbsListView.this.mListPadding.top), this.mScrollDuration, true, i > this.mTargetPos);
                            this.mLastSeenPos = i;
                            if (i > this.mTargetPos) {
                                AbsListView.this.postOnAnimation(this);
                            }
                            break;
                        }
                    }
                    break;
                case 3:
                    int childCount2 = AbsListView.this.getChildCount();
                    if (i == this.mBoundPos || childCount2 <= 1 || childCount2 + i >= AbsListView.this.mItemCount) {
                        AbsListView.this.reportScrollStateChange(0);
                    } else {
                        int i3 = i + 1;
                        if (i3 == this.mLastSeenPos) {
                            AbsListView.this.postOnAnimation(this);
                        } else {
                            View childAt3 = AbsListView.this.getChildAt(1);
                            int height3 = childAt3.getHeight();
                            int top2 = childAt3.getTop();
                            int iMax = Math.max(AbsListView.this.mListPadding.bottom, this.mExtraScroll);
                            if (i3 < this.mBoundPos) {
                                AbsListView.this.smoothScrollBy(Math.max(0, (height3 + top2) - iMax), this.mScrollDuration, true, true);
                                this.mLastSeenPos = i3;
                                AbsListView.this.postOnAnimation(this);
                            } else if (top2 > iMax) {
                                AbsListView.this.smoothScrollBy(top2 - iMax, this.mScrollDuration, true, false);
                            } else {
                                AbsListView.this.reportScrollStateChange(0);
                            }
                        }
                    }
                    break;
                case 4:
                    int childCount3 = AbsListView.this.getChildCount() - 2;
                    if (childCount3 >= 0) {
                        int i4 = i + childCount3;
                        if (i4 == this.mLastSeenPos) {
                            AbsListView.this.postOnAnimation(this);
                        } else {
                            View childAt4 = AbsListView.this.getChildAt(childCount3);
                            int height4 = childAt4.getHeight();
                            int top3 = childAt4.getTop();
                            int i5 = height2 - top3;
                            int iMax2 = Math.max(AbsListView.this.mListPadding.top, this.mExtraScroll);
                            this.mLastSeenPos = i4;
                            if (i4 > this.mBoundPos) {
                                AbsListView.this.smoothScrollBy(-(i5 - iMax2), this.mScrollDuration, true, true);
                                AbsListView.this.postOnAnimation(this);
                            } else {
                                int i6 = height2 - iMax2;
                                int i7 = top3 + height4;
                                if (i6 > i7) {
                                    AbsListView.this.smoothScrollBy(-(i6 - i7), this.mScrollDuration, true, false);
                                } else {
                                    AbsListView.this.reportScrollStateChange(0);
                                }
                            }
                        }
                        break;
                    }
                    break;
                case 5:
                    if (this.mLastSeenPos == i) {
                        AbsListView.this.postOnAnimation(this);
                    } else {
                        this.mLastSeenPos = i;
                        int childCount4 = AbsListView.this.getChildCount();
                        int i8 = this.mTargetPos;
                        int i9 = (i + childCount4) - 1;
                        int height5 = AbsListView.this.getChildAt(0).getHeight();
                        int height6 = AbsListView.this.getChildAt(childCount4 - 1).getHeight();
                        float f = height5;
                        float f2 = 0.0f;
                        if (f != 0.0f) {
                            top = (height5 + r6.getTop()) / f;
                        } else {
                            top = 1.0f;
                        }
                        float f3 = height6;
                        if (f3 != 0.0f) {
                            height = ((height6 + AbsListView.this.getHeight()) - r8.getBottom()) / f3;
                        } else {
                            height = 1.0f;
                        }
                        if (i8 < i) {
                            f2 = (i - i8) + (1.0f - top) + 1.0f;
                        } else if (i8 > i9) {
                            f2 = (i8 - i9) + (1.0f - height);
                        }
                        float fMin = Math.min(Math.abs(f2 / childCount4), 1.0f);
                        if (i8 < i) {
                            AbsListView.this.smoothScrollBy((int) ((-AbsListView.this.getHeight()) * fMin), (int) (this.mScrollDuration * fMin), true, true);
                            AbsListView.this.postOnAnimation(this);
                        } else if (i8 > i9) {
                            AbsListView.this.smoothScrollBy((int) (AbsListView.this.getHeight() * fMin), (int) (this.mScrollDuration * fMin), true, true);
                            AbsListView.this.postOnAnimation(this);
                        } else {
                            AbsListView.this.smoothScrollBy(AbsListView.this.getChildAt(i8 - i).getTop() - this.mOffsetFromTop, (int) (this.mScrollDuration * (Math.abs(r0) / AbsListView.this.getHeight())), true, false);
                        }
                    }
                    break;
            }
        }
    }
}
