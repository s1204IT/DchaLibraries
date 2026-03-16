package com.android.deskclock.widget.sgv;

import android.R;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.database.DataSetObserver;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Point;
import android.graphics.Rect;
import android.os.Build;
import android.os.Handler;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.v4.util.SparseArrayCompat;
import android.support.v4.view.MotionEventCompat;
import android.support.v4.view.VelocityTrackerCompat;
import android.support.v4.view.ViewCompat;
import android.support.v4.widget.EdgeEffectCompat;
import android.util.AttributeSet;
import android.util.Log;
import android.util.SparseArray;
import android.view.DragEvent;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageView;
import com.android.deskclock.widget.sgv.SgvAnimationHelper;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StaggeredGridView extends ViewGroup {
    private static final String TAG = "Clock-" + StaggeredGridView.class.getSimpleName();
    private final int SCROLL_RESTORE_WINDOW_SIZE;
    private int mActivePointerId;
    private GridAdapter mAdapter;
    private SgvAnimationHelper.AnimationIn mAnimationInMode;
    private SgvAnimationHelper.AnimationOut mAnimationOutMode;
    private final EdgeEffectCompat mBottomEdge;
    private Rect mCachedDragViewRect;
    private final Map<Long, ViewRectPair> mChildRectsForAnimation;
    private int mColCount;
    private int mColCountSetting;
    private AnimatorSet mCurrentRunningAnimatorSet;
    private ScrollState mCurrentScrollState;
    private boolean mDataChanged;
    private Bitmap mDragBitmap;
    private final Runnable mDragScroller;
    private int mDragState;
    private ImageView mDragView;
    private View mEmptyView;
    private boolean mFastChildLayout;
    private int mFirstChangedPosition;
    private int mFirstPosition;
    private final int mFlingVelocity;
    private long mFocusedChildIdToScrollIntoView;
    private boolean mGuardAgainstJaggedEdges;
    private boolean mHasStableIds;
    private int mHeight;
    private int mHorizontalReorderingAreaSize;
    private boolean mInLayout;
    boolean mIsCurrentAnimationCanceled;
    private boolean mIsDragReorderingEnabled;
    private boolean mIsDragScrollerRunning;
    private boolean mIsRtlLayout;
    private int[] mItemBottoms;
    private int mItemCount;
    private int mItemMargin;
    private int[] mItemTops;
    private float mLastTouchY;
    private final SparseArrayCompat<LayoutRecord> mLayoutRecords;
    private int mLowerScrollBound;
    private final int mMaximumVelocity;
    private int mMinColWidth;
    private final AdapterDataSetObserver mObserver;
    private int mOffsetToAbsoluteX;
    private int mOffsetToAbsoluteY;
    private OnSizeChangedListener mOnSizeChangedListener;
    private final int mOverscrollDistance;
    private boolean mPopulating;
    private final RecycleBin mRecycler;
    private ReorderHelper mReorderHelper;
    private Handler mScrollHandler;
    private ScrollListener mScrollListener;
    private final OverScrollerSGV mScroller;
    private boolean mSmoothScrollbarEnabled;
    private final Rect mTempRect;
    private final EdgeEffectCompat mTopEdge;
    private int mTouchDownForDragStartX;
    private int mTouchDownForDragStartY;
    private int mTouchMode;
    private int mTouchOffsetToChildLeft;
    private int mTouchOffsetToChildTop;
    private float mTouchRemainderY;
    private final int mTouchSlop;
    private int mUpperScrollBound;
    private final VelocityTracker mVelocityTracker;
    private final List<View> mViewsToAnimateOut;
    private final WindowManager mWindowManager;
    private WindowManager.LayoutParams mWindowParams;

    public interface OnSizeChangedListener {
        void onSizeChanged(int i, int i2, int i3, int i4);
    }

    public interface ReorderListener {
        void onCancelDrag(View view);

        void onDrop(View view, int i, int i2);

        void onEnterReorderArea(View view, int i);

        void onPickedUp(View view);

        boolean onReorder(View view, long j, int i, int i2);
    }

    public interface ScrollListener {
        void onScrollChanged(int i, int i2, int i3);
    }

    private static final class LayoutRecord {
        public int column;
        public int height;
        public long id;
        private int[] mMargins;
        public int span;

        private LayoutRecord() {
            this.id = -1L;
        }

        private final void ensureMargins() {
            if (this.mMargins == null) {
                this.mMargins = new int[this.span * 2];
            }
        }

        public final int getMarginAbove(int col) {
            if (this.mMargins == null) {
                return 0;
            }
            return this.mMargins[col * 2];
        }

        public final int getMarginBelow(int col) {
            if (this.mMargins == null) {
                return 0;
            }
            return this.mMargins[(col * 2) + 1];
        }

        public final void setMarginAbove(int col, int margin) {
            if (this.mMargins != null || margin != 0) {
                ensureMargins();
                this.mMargins[col * 2] = margin;
            }
        }

        public final void setMarginBelow(int col, int margin) {
            if (this.mMargins != null || margin != 0) {
                ensureMargins();
                this.mMargins[(col * 2) + 1] = margin;
            }
        }

        public String toString() {
            String result = "LayoutRecord{c=" + this.column + ", id=" + this.id + " h=" + this.height + " s=" + this.span;
            if (this.mMargins != null) {
                String result2 = result + " margins[above, below](";
                for (int i = 0; i < this.mMargins.length; i += 2) {
                    result2 = result2 + "[" + this.mMargins[i] + ", " + this.mMargins[i + 1] + "]";
                }
                result = result2 + ")";
            }
            return result + "}";
        }
    }

    public StaggeredGridView(Context context) {
        this(context, null);
    }

    public StaggeredGridView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public StaggeredGridView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        this.SCROLL_RESTORE_WINDOW_SIZE = 10;
        this.mAnimationInMode = SgvAnimationHelper.AnimationIn.NONE;
        this.mAnimationOutMode = SgvAnimationHelper.AnimationOut.NONE;
        this.mCurrentRunningAnimatorSet = null;
        this.mIsCurrentAnimationCanceled = false;
        this.mColCountSetting = 2;
        this.mColCount = 2;
        this.mMinColWidth = 0;
        this.mItemMargin = 0;
        this.mTempRect = new Rect();
        this.mRecycler = new RecycleBin();
        this.mObserver = new AdapterDataSetObserver();
        this.mViewsToAnimateOut = new ArrayList();
        this.mLastTouchY = 0.0f;
        this.mVelocityTracker = VelocityTracker.obtain();
        this.mSmoothScrollbarEnabled = false;
        this.mChildRectsForAnimation = new HashMap();
        this.mLayoutRecords = new SparseArrayCompat<>();
        this.mDragScroller = new Runnable() {
            @Override
            public void run() {
                if (StaggeredGridView.this.mDragState != 0) {
                    boolean enableUpdate = true;
                    if (StaggeredGridView.this.mLastTouchY >= StaggeredGridView.this.mLowerScrollBound) {
                        if (StaggeredGridView.this.trackMotionScroll(-10, false)) {
                            enableUpdate = false;
                        }
                    } else if (StaggeredGridView.this.mLastTouchY <= StaggeredGridView.this.mUpperScrollBound && StaggeredGridView.this.trackMotionScroll(10, false)) {
                        enableUpdate = false;
                    }
                    StaggeredGridView.this.mReorderHelper.enableUpdatesOnDrag(enableUpdate);
                    StaggeredGridView.this.mScrollHandler.postDelayed(this, 5L);
                }
            }
        };
        ViewConfiguration vc = ViewConfiguration.get(context);
        this.mTouchSlop = vc.getScaledTouchSlop();
        this.mMaximumVelocity = vc.getScaledMaximumFlingVelocity();
        this.mFlingVelocity = vc.getScaledMinimumFlingVelocity();
        this.mScroller = new OverScrollerSGV(context);
        this.mTopEdge = new EdgeEffectCompat(context);
        this.mBottomEdge = new EdgeEffectCompat(context);
        setWillNotDraw(false);
        setClipToPadding(false);
        SgvAnimationHelper.initialize(context);
        this.mDragState = 0;
        this.mIsDragReorderingEnabled = true;
        this.mWindowManager = (WindowManager) context.getSystemService("window");
        ViewConfiguration configuration = ViewConfiguration.get(context);
        this.mOverscrollDistance = configuration.getScaledOverflingDistance();
        setMotionEventSplittingEnabled(false);
    }

    @SuppressLint({"NewApi"})
    public boolean isLayoutRtl() {
        return Build.VERSION.SDK_INT >= 17 && 1 == getLayoutDirection();
    }

    public void setColumnCount(int colCount) {
        if (colCount < 1 && colCount != -1) {
            throw new IllegalArgumentException("Column count must be at least 1 - received " + colCount);
        }
        boolean needsPopulate = colCount != this.mColCount;
        this.mColCountSetting = colCount;
        this.mColCount = colCount;
        if (needsPopulate) {
            clearAllState();
            this.mHorizontalReorderingAreaSize = 0;
            populate();
        }
    }

    public void setGuardAgainstJaggedEdges(boolean guardAgainstJaggedEdges) {
        this.mGuardAgainstJaggedEdges = guardAgainstJaggedEdges;
    }

    private View getChildAtCoordinate(int x, int y) {
        if (y < 0) {
            return null;
        }
        Rect frame = new Rect();
        int count = getChildCount();
        for (int i = 0; i < count; i++) {
            View childView = getChildAt(i);
            childView.getHitRect(frame);
            if (frame.contains(x, y)) {
                return getChildAt(i);
            }
        }
        return null;
    }

    private boolean isDragReorderingSupported() {
        return this.mIsDragReorderingEnabled && this.mReorderHelper != null && this.mReorderHelper.hasReorderListener();
    }

    private void initializeDragScrollParameters(int y) {
        this.mHeight = getHeight();
        this.mUpperScrollBound = Math.min(y - this.mTouchSlop, this.mHeight / 5);
        this.mLowerScrollBound = Math.max(this.mTouchSlop + y, (this.mHeight * 4) / 5);
    }

    private void startDragging(View draggedChild, int x, int y) {
        if (isDragReorderingSupported()) {
            this.mDragBitmap = createDraggedChildBitmap(draggedChild);
            if (this.mDragBitmap == null) {
                this.mReorderHelper.handleDragCancelled(draggedChild);
                return;
            }
            this.mTouchOffsetToChildLeft = x - draggedChild.getLeft();
            this.mTouchOffsetToChildTop = y - draggedChild.getTop();
            updateReorderStates(1);
            initializeDragScrollParameters(y);
            LayoutParams params = (LayoutParams) draggedChild.getLayoutParams();
            this.mReorderHelper.handleDragStart(draggedChild, params.position, params.id, new Point(this.mTouchDownForDragStartX, this.mTouchDownForDragStartY));
            Context context = getContext();
            this.mDragView = new ImageView(context);
            this.mDragView.setImageBitmap(this.mDragBitmap);
            this.mDragView.setAlpha(160);
            this.mWindowParams = new WindowManager.LayoutParams();
            this.mWindowParams.gravity = 8388659;
            this.mWindowParams.height = -2;
            this.mWindowParams.width = -2;
            this.mWindowParams.flags = 920;
            this.mWindowParams.format = -3;
            this.mWindowManager.addView(this.mDragView, this.mWindowParams);
            updateDraggedBitmapLocation(x, y);
        }
    }

    private Bitmap createDraggedChildBitmap(View view) {
        view.setDrawingCacheEnabled(true);
        Bitmap cache = view.getDrawingCache();
        Bitmap bitmap = null;
        if (cache != null) {
            try {
                bitmap = cache.copy(Bitmap.Config.ARGB_8888, false);
            } catch (OutOfMemoryError e) {
                Log.w(TAG, "Failed to copy bitmap from Drawing cache", e);
                bitmap = null;
            }
        }
        view.destroyDrawingCache();
        view.setDrawingCacheEnabled(false);
        return bitmap;
    }

    private void updateReorderStates(int state) throws IllegalStateException {
        boolean resetDraggedChildView = false;
        boolean resetDragProperties = false;
        this.mDragState = state;
        switch (state) {
            case 0:
            case 1:
                resetDraggedChildView = true;
                resetDragProperties = true;
                break;
            case 2:
                break;
            case 3:
                resetDragProperties = true;
                break;
            default:
                throw new IllegalStateException("Illegal drag state: " + this.mDragState);
        }
        if (resetDraggedChildView && this.mReorderHelper.getDraggedChild() != null) {
            this.mReorderHelper.clearDraggedChildId();
            this.mCachedDragViewRect = null;
        }
        if (resetDragProperties) {
            if (this.mDragView != null) {
                this.mDragView.setVisibility(4);
                this.mWindowManager.removeView(this.mDragView);
                this.mDragView.setImageDrawable(null);
                this.mDragView = null;
                if (this.mDragBitmap != null) {
                    this.mDragBitmap.recycle();
                    this.mDragBitmap = null;
                }
            }
            this.mReorderHelper.clearDraggedChild();
            this.mReorderHelper.clearDraggedOverChild();
        }
    }

    private void updateDraggedBitmapLocation(int x, int y) {
        int direction = this.mAdapter.getReorderingDirection();
        if ((direction & 1) == 1) {
            if (this.mDragBitmap != null && this.mDragBitmap.getWidth() > getWidth()) {
                this.mWindowParams.x = this.mOffsetToAbsoluteX;
            } else {
                this.mWindowParams.x = (x - this.mTouchOffsetToChildLeft) + this.mOffsetToAbsoluteX;
            }
        } else {
            this.mWindowParams.x = this.mOffsetToAbsoluteX;
        }
        if ((direction & 2) == 2) {
            this.mWindowParams.y = (y - this.mTouchOffsetToChildTop) + this.mOffsetToAbsoluteY;
        } else {
            this.mWindowParams.y = this.mOffsetToAbsoluteY;
        }
        this.mWindowManager.updateViewLayout(this.mDragView, this.mWindowParams);
    }

    private void handleDrag(int x, int y) {
        if (this.mDragState == 1) {
            updateDraggedBitmapLocation(x, y);
            if (this.mCurrentRunningAnimatorSet == null) {
                this.mReorderHelper.handleDrag(new Point(x, y));
            }
        }
    }

    public boolean isChildReorderable(int i) {
        return this.mAdapter.isDraggable(this.mFirstPosition + i);
    }

    private void handleDrop(int x, int y) {
        if (!this.mReorderHelper.hasReorderListener()) {
            updateReorderStates(0);
            return;
        }
        if (this.mReorderHelper.isOverReorderingArea()) {
            int left = this.mWindowParams.x - this.mOffsetToAbsoluteX;
            int top = this.mWindowParams.y - this.mOffsetToAbsoluteY;
            this.mCachedDragViewRect = new Rect(left, top, this.mDragView.getWidth() + left, this.mDragView.getHeight() + top);
            if (getChildCount() > 0) {
                View view = getChildAt(0);
                LayoutParams lp = (LayoutParams) view.getLayoutParams();
                if (lp.position > this.mReorderHelper.getDraggedChildPosition()) {
                    this.mFirstPosition--;
                }
            }
            this.mCurrentScrollState = getScrollState();
        }
        boolean reordered = this.mReorderHelper.handleDrop(new Point(x, y));
        if (reordered) {
            updateReorderStates(2);
        } else {
            updateReorderStates(0);
        }
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        this.mVelocityTracker.addMovement(ev);
        int action = ev.getAction() & 255;
        switch (action) {
            case 0:
                this.mOffsetToAbsoluteX = (int) (ev.getRawX() - ev.getX());
                this.mOffsetToAbsoluteY = (int) (ev.getRawY() - ev.getY());
                this.mTouchDownForDragStartX = (int) ev.getX();
                this.mTouchDownForDragStartY = (int) ev.getY();
                this.mVelocityTracker.clear();
                this.mScroller.abortAnimation();
                this.mLastTouchY = ev.getY();
                this.mActivePointerId = MotionEventCompat.getPointerId(ev, 0);
                this.mTouchRemainderY = 0.0f;
                if (this.mTouchMode != 2) {
                    return false;
                }
                this.mTouchMode = 1;
                return true;
            case 1:
            default:
                return false;
            case 2:
                int index = MotionEventCompat.findPointerIndex(ev, this.mActivePointerId);
                if (index < 0) {
                    Log.e(TAG, "onInterceptTouchEvent could not find pointer with id " + this.mActivePointerId + " - did StaggeredGridView receive an inconsistent event stream?");
                    return false;
                }
                float y = MotionEventCompat.getY(ev, index);
                float dy = (y - this.mLastTouchY) + this.mTouchRemainderY;
                int deltaY = (int) dy;
                this.mTouchRemainderY = dy - deltaY;
                if (Math.abs(dy) <= this.mTouchSlop) {
                    return false;
                }
                this.mTouchMode = 1;
                return true;
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        this.mVelocityTracker.addMovement(ev);
        int action = ev.getAction() & 255;
        switch (action) {
            case 0:
                resetScroller();
                this.mVelocityTracker.clear();
                this.mScroller.abortAnimation();
                this.mLastTouchY = ev.getY();
                this.mActivePointerId = MotionEventCompat.getPointerId(ev, 0);
                this.mTouchRemainderY = 0.0f;
                return true;
            case 1:
                this.mVelocityTracker.computeCurrentVelocity(1000, this.mMaximumVelocity);
                float velocity = VelocityTrackerCompat.getYVelocity(this.mVelocityTracker, this.mActivePointerId);
                if (Math.abs(velocity) > this.mFlingVelocity) {
                    this.mTouchMode = 2;
                    resetScroller();
                    this.mScroller.fling(0, 0, 0, (int) velocity, 0, 0, Integer.MIN_VALUE, Integer.MAX_VALUE);
                    this.mLastTouchY = 0.0f;
                    ViewCompat.postInvalidateOnAnimation(this);
                } else {
                    this.mTouchMode = 0;
                }
                return true;
            case 2:
                int index = MotionEventCompat.findPointerIndex(ev, this.mActivePointerId);
                if (index < 0) {
                    Log.e(TAG, "onInterceptTouchEvent could not find pointer with id " + this.mActivePointerId + " - did StaggeredGridView receive an inconsistent event stream?");
                    return false;
                }
                float y = MotionEventCompat.getY(ev, index);
                float dy = (y - this.mLastTouchY) + this.mTouchRemainderY;
                int deltaY = (int) dy;
                this.mTouchRemainderY = dy - deltaY;
                if (Math.abs(dy) > this.mTouchSlop) {
                    this.mTouchMode = 1;
                }
                if (this.mTouchMode == 1) {
                    this.mLastTouchY = y;
                    if (!trackMotionScroll(deltaY, true)) {
                        this.mVelocityTracker.clear();
                    }
                }
                return true;
            case 3:
                this.mTouchMode = 0;
                return true;
            default:
                return true;
        }
    }

    private void resetScroller() {
        this.mTouchMode = 0;
        this.mTopEdge.finish();
        this.mBottomEdge.finish();
        this.mScroller.abortAnimation();
    }

    @Override
    public boolean dispatchDragEvent(DragEvent event) {
        View child;
        if (!isDragReorderingSupported()) {
            return super.dispatchDragEvent(event);
        }
        switch (event.getAction()) {
            case 1:
                if (this.mReorderHelper.hasReorderListener() && this.mIsDragReorderingEnabled && (child = getChildAtCoordinate(this.mTouchDownForDragStartX, this.mTouchDownForDragStartY)) != null) {
                    startDragging(child, this.mTouchDownForDragStartX, this.mTouchDownForDragStartY);
                    break;
                }
                break;
            case 3:
            case 4:
                if (this.mDragState == 1) {
                    handleDrop((int) event.getX(), (int) event.getY());
                }
                break;
        }
        return super.dispatchDragEvent(event);
    }

    @Override
    public boolean onDragEvent(DragEvent ev) {
        if (!isDragReorderingSupported()) {
            return false;
        }
        int x = (int) ev.getX();
        int y = (int) ev.getY();
        switch (ev.getAction()) {
            case 2:
                if (this.mDragState == 1) {
                    handleDrag(x, y);
                    this.mLastTouchY = y;
                }
                if (!this.mIsDragScrollerRunning && (Math.abs(x - this.mTouchDownForDragStartX) >= this.mTouchSlop * 4 || Math.abs(y - this.mTouchDownForDragStartY) >= this.mTouchSlop * 4)) {
                    this.mIsDragScrollerRunning = true;
                    if (this.mScrollHandler == null) {
                        this.mScrollHandler = getHandler();
                    }
                    this.mScrollHandler.postDelayed(this.mDragScroller, 5L);
                }
                return true;
            case 3:
            case 4:
                if (this.mScrollHandler != null) {
                    this.mScrollHandler.removeCallbacks(this.mDragScroller);
                    this.mIsDragScrollerRunning = false;
                }
                return true;
            default:
                return false;
        }
    }

    private boolean trackMotionScroll(int deltaY, boolean allowOverScroll) {
        int overScrolledBy;
        int movedBy;
        int overScrollMode;
        int overhang;
        boolean up;
        boolean contentFits = contentFits();
        int allowOverhang = Math.abs(deltaY);
        if (!contentFits) {
            this.mPopulating = true;
            if (deltaY > 0) {
                overhang = fillUp(this.mFirstPosition - 1, allowOverhang);
                up = true;
            } else {
                overhang = fillDown(this.mFirstPosition + getChildCount(), allowOverhang);
                if (overhang < 0) {
                    overhang = 0;
                }
                up = false;
            }
            movedBy = Math.min(overhang, allowOverhang);
            offsetChildren(up ? movedBy : -movedBy);
            recycleOffscreenViews();
            this.mPopulating = false;
            overScrolledBy = allowOverhang - overhang;
        } else {
            overScrolledBy = allowOverhang;
            movedBy = 0;
        }
        if (allowOverScroll && (((overScrollMode = ViewCompat.getOverScrollMode(this)) == 0 || (overScrollMode == 1 && !contentFits)) && overScrolledBy > 0)) {
            EdgeEffectCompat edge = deltaY > 0 ? this.mTopEdge : this.mBottomEdge;
            edge.onPull(Math.abs(deltaY) / getHeight());
            ViewCompat.postInvalidateOnAnimation(this);
        }
        awakenScrollBars(0, true);
        return deltaY == 0 || movedBy != 0;
    }

    public final boolean contentFits() {
        if (this.mFirstPosition != 0 || getChildCount() != this.mItemCount) {
            return false;
        }
        int topmost = Integer.MAX_VALUE;
        int bottommost = Integer.MIN_VALUE;
        for (int i = 0; i < this.mColCount; i++) {
            if (this.mItemTops[i] < topmost) {
                topmost = this.mItemTops[i];
            }
            if (this.mItemBottoms[i] > bottommost) {
                bottommost = this.mItemBottoms[i];
            }
        }
        return topmost >= getPaddingTop() && bottommost <= getHeight() - getPaddingBottom();
    }

    private void recycleViewsInRange(int startIndex, int endIndex) {
        for (int i = endIndex; i >= startIndex; i--) {
            View child = getChildAt(i);
            if (this.mInLayout) {
                removeViewsInLayout(i, 1);
            } else {
                removeViewAt(i);
            }
            this.mRecycler.addScrap(child);
        }
    }

    private void recycleView(View view) {
        if (view != null) {
            if (this.mInLayout) {
                removeViewInLayout(view);
                invalidate();
            } else {
                removeView(view);
            }
            this.mRecycler.addScrap(view);
        }
    }

    private void recycleOffscreenViews() {
        if (getChildCount() != 0) {
            int height = getHeight();
            int clearAbove = -this.mItemMargin;
            int clearBelow = height + this.mItemMargin;
            for (int i = getChildCount() - 1; i >= 0; i--) {
                View child = getChildAt(i);
                if (child.getTop() <= clearBelow) {
                    break;
                }
                child.clearFocus();
                if (this.mInLayout) {
                    removeViewsInLayout(i, 1);
                } else {
                    removeViewAt(i);
                }
                this.mRecycler.addScrap(child);
            }
            while (getChildCount() > 0) {
                View child2 = getChildAt(0);
                if (child2.getBottom() >= clearAbove) {
                    break;
                }
                child2.clearFocus();
                if (this.mInLayout) {
                    removeViewsInLayout(0, 1);
                } else {
                    removeViewAt(0);
                }
                this.mRecycler.addScrap(child2);
                this.mFirstPosition++;
            }
            int childCount = getChildCount();
            if (childCount > 0) {
                Arrays.fill(this.mItemTops, Integer.MAX_VALUE);
                Arrays.fill(this.mItemBottoms, Integer.MIN_VALUE);
                for (int i2 = 0; i2 < childCount; i2++) {
                    View child3 = getChildAt(i2);
                    LayoutParams lp = (LayoutParams) child3.getLayoutParams();
                    int top = child3.getTop() - this.mItemMargin;
                    int bottom = child3.getBottom();
                    LayoutRecord rec = this.mLayoutRecords.get(this.mFirstPosition + i2);
                    if (rec == null) {
                        rec = recreateLayoutRecord(this.mFirstPosition + i2, child3, lp);
                    }
                    int span = Math.min(this.mColCount, lp.span);
                    for (int spanIndex = 0; spanIndex < span; spanIndex++) {
                        int col = this.mIsRtlLayout ? lp.column - spanIndex : lp.column + spanIndex;
                        int colTop = top - rec.getMarginAbove(spanIndex);
                        int colBottom = bottom + rec.getMarginBelow(spanIndex);
                        if (colTop < this.mItemTops[col]) {
                            this.mItemTops[col] = colTop;
                        }
                        if (colBottom > this.mItemBottoms[col]) {
                            this.mItemBottoms[col] = colBottom;
                        }
                    }
                }
                for (int col2 = 0; col2 < this.mColCount; col2++) {
                    if (this.mItemTops[col2] == Integer.MAX_VALUE) {
                        int top2 = getPaddingTop();
                        this.mItemTops[col2] = top2;
                        this.mItemBottoms[col2] = top2;
                    }
                }
            }
            this.mCurrentScrollState = getScrollState();
        }
    }

    private LayoutRecord recreateLayoutRecord(int position, View child, LayoutParams lp) {
        LayoutRecord rec = new LayoutRecord();
        this.mLayoutRecords.put(position, rec);
        rec.column = lp.column;
        rec.height = child.getHeight();
        rec.id = lp.id;
        rec.span = Math.min(this.mColCount, lp.span);
        return rec;
    }

    @Override
    public void computeScroll() {
        EdgeEffectCompat edge;
        if (this.mTouchMode == 3) {
            handleOverfling();
            return;
        }
        if (this.mScroller.computeScrollOffset()) {
            int overScrollMode = ViewCompat.getOverScrollMode(this);
            boolean supportsOverscroll = overScrollMode != 2;
            int y = this.mScroller.getCurrY();
            int dy = (int) (y - this.mLastTouchY);
            this.mLastTouchY = y;
            View motionView = (!supportsOverscroll || getChildCount() <= 0) ? null : getChildAt(0);
            int motionViewPrevTop = motionView != null ? motionView.getTop() : 0;
            boolean stopped = !trackMotionScroll(dy, false);
            if (!stopped && !this.mScroller.isFinished()) {
                this.mTouchMode = 0;
                ViewCompat.postInvalidateOnAnimation(this);
                return;
            }
            if (stopped && dy != 0 && supportsOverscroll) {
                if (motionView != null) {
                    int motionViewRealTop = motionView.getTop();
                    int overscroll = (-dy) - (motionViewRealTop - motionViewPrevTop);
                    overScrollBy(0, overscroll, 0, getScrollY(), 0, 0, 0, this.mOverscrollDistance, true);
                }
                if (dy > 0) {
                    edge = this.mTopEdge;
                    this.mBottomEdge.finish();
                } else {
                    edge = this.mBottomEdge;
                    this.mTopEdge.finish();
                }
                edge.onAbsorb(Math.abs((int) this.mScroller.getCurrVelocity()));
                if (this.mScroller.computeScrollOffset()) {
                    this.mScroller.notifyVerticalEdgeReached(getScrollY(), 0, this.mOverscrollDistance);
                }
                this.mTouchMode = 3;
                ViewCompat.postInvalidateOnAnimation(this);
                return;
            }
            this.mTouchMode = 0;
        }
    }

    private void handleOverfling() {
        if (this.mScroller.computeScrollOffset()) {
            int scrollY = getScrollY();
            int currY = this.mScroller.getCurrY();
            int deltaY = currY - scrollY;
            if (overScrollBy(0, deltaY, 0, scrollY, 0, 0, 0, this.mOverscrollDistance, false)) {
                boolean crossDown = scrollY <= 0 && currY > 0;
                boolean crossUp = scrollY >= 0 && currY < 0;
                if (crossDown || crossUp) {
                    int velocity = (int) this.mScroller.getCurrVelocity();
                    if (crossUp) {
                        int i = -velocity;
                    }
                    this.mTouchMode = 0;
                    this.mScroller.abortAnimation();
                    return;
                }
                if (this.mScroller.springBack(0, scrollY, 0, 0, 0, 0)) {
                    this.mTouchMode = 3;
                    ViewCompat.postInvalidateOnAnimation(this);
                    return;
                } else {
                    this.mTouchMode = 0;
                    return;
                }
            }
            ViewCompat.postInvalidateOnAnimation(this);
            return;
        }
        this.mTouchMode = 0;
        this.mScroller.abortAnimation();
    }

    @Override
    protected void onOverScrolled(int scrollX, int scrollY, boolean clampedX, boolean clampedY) {
        if (getScrollY() != scrollY) {
            scrollTo(0, scrollY);
        }
    }

    @Override
    public void draw(Canvas canvas) {
        super.draw(canvas);
        if (this.mTopEdge != null) {
            boolean needsInvalidate = false;
            if (!this.mTopEdge.isFinished()) {
                int restoreCount = canvas.save();
                canvas.translate(0.0f, 0.0f);
                this.mTopEdge.draw(canvas);
                canvas.restoreToCount(restoreCount);
                needsInvalidate = true;
            }
            if (!this.mBottomEdge.isFinished()) {
                int restoreCount2 = canvas.save();
                int width = getWidth();
                canvas.translate(-width, getHeight());
                canvas.rotate(180.0f, width, 0.0f);
                this.mBottomEdge.draw(canvas);
                canvas.restoreToCount(restoreCount2);
                needsInvalidate = true;
            }
            if (needsInvalidate) {
                ViewCompat.postInvalidateOnAnimation(this);
            }
        }
    }

    @Override
    public void requestLayout() {
        if (!this.mPopulating && !this.mFastChildLayout) {
            super.requestLayout();
        }
    }

    private void updateEmptyStatus() {
        if (this.mAdapter == null || this.mAdapter.isEmpty()) {
            if (this.mEmptyView != null) {
                this.mEmptyView.setVisibility(0);
                setVisibility(8);
                return;
            } else {
                setVisibility(0);
                return;
            }
        }
        if (this.mEmptyView != null) {
            this.mEmptyView.setVisibility(8);
        }
        setVisibility(0);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int colCount;
        int widthMode = View.MeasureSpec.getMode(widthMeasureSpec);
        int heightMode = View.MeasureSpec.getMode(heightMeasureSpec);
        int widthSize = View.MeasureSpec.getSize(widthMeasureSpec);
        int heightSize = View.MeasureSpec.getSize(heightMeasureSpec);
        if (widthMode != 1073741824) {
            Log.d(TAG, "onMeasure: must have an exact width or match_parent! Using fallback spec of EXACTLY " + widthSize);
        }
        if (heightMode != 1073741824) {
            Log.d(TAG, "onMeasure: must have an exact height or match_parent! Using fallback spec of EXACTLY " + heightSize);
        }
        setMeasuredDimension(widthSize, heightSize);
        if (this.mColCountSetting == -1 && (colCount = widthSize / this.mMinColWidth) != this.mColCount) {
            this.mColCount = colCount;
        }
        if (this.mHorizontalReorderingAreaSize == 0) {
            if (this.mColCount > 1) {
                int totalMarginWidth = this.mItemMargin * (this.mColCount + 1);
                int singleViewWidth = (widthSize - totalMarginWidth) / this.mColCount;
                this.mHorizontalReorderingAreaSize = singleViewWidth / 4;
                return;
            }
            this.mHorizontalReorderingAreaSize = 30;
        }
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        this.mIsRtlLayout = isLayoutRtl();
        this.mInLayout = true;
        populate();
        this.mInLayout = false;
        int width = r - l;
        int height = b - t;
        this.mTopEdge.setSize(width, height);
        this.mBottomEdge.setSize(width, height);
    }

    private void populate() {
        int colCount;
        if (getWidth() != 0 && getHeight() != 0 && this.mAdapter != null) {
            if (this.mColCount == -1 && (colCount = getWidth() / this.mMinColWidth) != this.mColCount) {
                this.mColCount = colCount;
            }
            int colCount2 = this.mColCount;
            if (this.mItemTops == null || this.mItemBottoms == null || this.mItemTops.length != colCount2 || this.mItemBottoms.length != colCount2) {
                this.mItemTops = new int[colCount2];
                this.mItemBottoms = new int[colCount2];
                this.mLayoutRecords.clear();
                if (this.mInLayout) {
                    removeAllViewsInLayout();
                } else {
                    removeAllViews();
                }
            }
            if (this.mDataChanged && this.mCurrentRunningAnimatorSet != null) {
                this.mCurrentRunningAnimatorSet.cancel();
                this.mCurrentRunningAnimatorSet = null;
            }
            if (isSelectionAtTop()) {
                this.mCurrentScrollState = null;
            }
            if (this.mCurrentScrollState != null) {
                restoreScrollPosition(this.mCurrentScrollState);
            } else {
                calculateLayoutStartOffsets(getPaddingTop());
            }
            this.mPopulating = true;
            this.mFocusedChildIdToScrollIntoView = -1L;
            View focusedChild = getFocusedChild();
            if (focusedChild != null) {
                LayoutParams lp = (LayoutParams) focusedChild.getLayoutParams();
                this.mFocusedChildIdToScrollIntoView = lp.id;
            }
            layoutChildren(this.mDataChanged);
            fillDown(this.mFirstPosition + getChildCount(), 0);
            fillUp(this.mFirstPosition - 1, 0);
            if ((isDragReorderingSupported() && this.mDragState == 2) || this.mDragState == 3) {
                this.mReorderHelper.clearDraggedChildId();
                updateReorderStates(0);
            }
            if (this.mDataChanged) {
                handleLayoutAnimation();
            }
            recycleOffscreenViews();
            this.mPopulating = false;
            this.mDataChanged = false;
        }
    }

    @Override
    public void scrollBy(int x, int y) {
        if (y != 0) {
            trackMotionScroll(y, false);
        }
    }

    private void offsetChildren(int offset) {
        int childCount = getChildCount();
        for (int i = 0; i < childCount; i++) {
            View child = getChildAt(i);
            child.offsetTopAndBottom(offset);
            LayoutParams lp = (LayoutParams) child.getLayoutParams();
            setReorderingArea(lp);
        }
        int colCount = this.mColCount;
        for (int i2 = 0; i2 < colCount; i2++) {
            int[] iArr = this.mItemTops;
            iArr[i2] = iArr[i2] + offset;
            int[] iArr2 = this.mItemBottoms;
            iArr2[i2] = iArr2[i2] + offset;
        }
        if (this.mScrollListener != null) {
            this.mScrollListener.onScrollChanged(offset, computeVerticalScrollOffset(), computeVerticalScrollRange());
        }
    }

    private void handleLayoutAnimation() throws IllegalStateException {
        List<Animator> animators = new ArrayList<>();
        ValueAnimator anim = ValueAnimator.ofFloat(0.0f, 1.0f);
        anim.setDuration(0L);
        animators.add(anim);
        addOutAnimatorsForStaleViews(animators, this.mAnimationOutMode);
        int animationInStartDelay = animators.size() > 0 ? SgvAnimationHelper.getDefaultAnimationDuration() / 2 : 0;
        addInAnimators(animators, this.mAnimationInMode, animationInStartDelay);
        if (animators != null && animators.size() > 0) {
            final AnimatorSet animatorSet = new AnimatorSet();
            animatorSet.playTogether(animators);
            animatorSet.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationStart(Animator animation) {
                    StaggeredGridView.this.mIsCurrentAnimationCanceled = false;
                    StaggeredGridView.this.mCurrentRunningAnimatorSet = animatorSet;
                }

                @Override
                public void onAnimationCancel(Animator animation) {
                    StaggeredGridView.this.mIsCurrentAnimationCanceled = true;
                }

                @Override
                public void onAnimationEnd(Animator animation) {
                    if (!StaggeredGridView.this.mIsCurrentAnimationCanceled) {
                        StaggeredGridView.this.resetAnimationMode();
                    }
                    StaggeredGridView.this.mCurrentRunningAnimatorSet = null;
                }
            });
            Log.v(TAG, "starting");
            animatorSet.start();
        } else {
            resetAnimationMode();
        }
        this.mViewsToAnimateOut.clear();
        this.mChildRectsForAnimation.clear();
    }

    private void resetAnimationMode() {
        this.mAnimationInMode = SgvAnimationHelper.AnimationIn.NONE;
        this.mAnimationOutMode = SgvAnimationHelper.AnimationOut.NONE;
    }

    private void addInAnimators(List<Animator> animators, SgvAnimationHelper.AnimationIn animationInMode, int startDelay) {
        if (animationInMode != SgvAnimationHelper.AnimationIn.NONE) {
            switch (animationInMode) {
                case FLY_UP_ALL_VIEWS:
                    addFlyInAllViewsAnimators(animators);
                    return;
                case EXPAND_NEW_VIEWS:
                    addUpdateViewPositionsAnimators(animators, true, SgvAnimationHelper.AnimationIn.EXPAND_NEW_VIEWS, startDelay);
                    return;
                case EXPAND_NEW_VIEWS_NO_CASCADE:
                    addUpdateViewPositionsAnimators(animators, false, SgvAnimationHelper.AnimationIn.EXPAND_NEW_VIEWS_NO_CASCADE, startDelay);
                    return;
                case SLIDE_IN_NEW_VIEWS:
                    addUpdateViewPositionsAnimators(animators, true, SgvAnimationHelper.AnimationIn.SLIDE_IN_NEW_VIEWS, startDelay);
                    return;
                case FLY_IN_NEW_VIEWS:
                    addUpdateViewPositionsAnimators(animators, true, SgvAnimationHelper.AnimationIn.FLY_IN_NEW_VIEWS, startDelay);
                    return;
                case FADE:
                    addUpdateViewPositionsAnimators(animators, true, SgvAnimationHelper.AnimationIn.FADE, startDelay);
                    return;
                default:
                    throw new IllegalStateException("Unknown animationInMode: " + this.mAnimationInMode);
            }
        }
    }

    private void addOutAnimatorsForStaleViews(List<Animator> animators, SgvAnimationHelper.AnimationOut animationOutMode) {
        if (animationOutMode != SgvAnimationHelper.AnimationOut.NONE) {
            for (View v : this.mViewsToAnimateOut) {
                List<Animator> viewAnimators = new ArrayList<>();
                switch (animationOutMode) {
                    case SLIDE:
                        LayoutParams lp = (LayoutParams) v.getLayoutParams();
                        int endTranslation = (int) (((double) v.getWidth()) * 1.5d);
                        if (lp.column < this.mColCount / 2) {
                            endTranslation = -endTranslation;
                        }
                        SgvAnimationHelper.addSlideOutAnimators(viewAnimators, v, (int) v.getTranslationX(), endTranslation);
                        break;
                    case COLLAPSE:
                        SgvAnimationHelper.addCollapseOutAnimators(viewAnimators, v);
                        break;
                    case FLY_DOWN:
                        SgvAnimationHelper.addFlyOutAnimators(viewAnimators, v, (int) v.getTranslationY(), getHeight());
                        break;
                    case FADE:
                        SgvAnimationHelper.addFadeAnimators(viewAnimators, v, v.getAlpha(), 0.0f);
                        break;
                    default:
                        throw new IllegalStateException("Unknown animationOutMode: " + animationOutMode);
                }
                if (viewAnimators.size() > 0) {
                    addStaleViewAnimationEndListener(v, viewAnimators);
                    animators.addAll(viewAnimators);
                }
            }
        }
    }

    private List<Animator> addFlyInAllViewsAnimators(List<Animator> animators) {
        int childCount = getChildCount();
        if (childCount == 0) {
            return null;
        }
        if (animators == null) {
            animators = new ArrayList<>();
        }
        for (int i = 0; i < childCount; i++) {
            int animationDelay = i * 50;
            View childToAnimate = getChildAt(i);
            float yTranslation = getHeight();
            float rotation = 25.0f;
            if (this.mIsCurrentAnimationCanceled) {
                yTranslation = childToAnimate.getTranslationY();
                rotation = childToAnimate.getRotation();
            }
            SgvAnimationHelper.addTranslationRotationAnimators(animators, childToAnimate, 0, (int) yTranslation, rotation, animationDelay);
        }
        return animators;
    }

    private List<Animator> addUpdateViewPositionsAnimators(List<Animator> animators, boolean cascadeAnimation, SgvAnimationHelper.AnimationIn animationInMode, int startDelay) {
        int childCount = getChildCount();
        if (childCount == 0) {
            return null;
        }
        if (animators == null) {
            animators = new ArrayList<>();
        }
        int viewsAnimated = 0;
        for (int i = 0; i < childCount; i++) {
            View childToAnimate = getChildAt(i);
            if (!this.mViewsToAnimateOut.contains(childToAnimate)) {
                int animationDelay = startDelay + (cascadeAnimation ? viewsAnimated * 50 : 0);
                LayoutParams lp = (LayoutParams) childToAnimate.getLayoutParams();
                ViewRectPair viewRectPair = this.mChildRectsForAnimation.get(Long.valueOf(lp.id));
                if (viewRectPair != null && viewRectPair.rect != null) {
                    if (animationInMode == SgvAnimationHelper.AnimationIn.FADE) {
                        SgvAnimationHelper.addFadeAnimators(animators, childToAnimate, 0.0f, 1.0f, animationDelay);
                    } else {
                        Rect oldRect = viewRectPair.rect;
                        int xTranslation = oldRect.left - childToAnimate.getLeft();
                        int yTranslation = oldRect.top - childToAnimate.getTop();
                        float rotation = childToAnimate.getRotation();
                        childToAnimate.setTranslationX(xTranslation);
                        childToAnimate.setTranslationY(yTranslation);
                        if (xTranslation != 0 || yTranslation != 0 || rotation != 0.0f) {
                            SgvAnimationHelper.addTranslationRotationAnimators(animators, childToAnimate, xTranslation, yTranslation, rotation, animationDelay);
                            viewsAnimated++;
                        }
                    }
                } else {
                    int yTranslation2 = animationInMode == SgvAnimationHelper.AnimationIn.FLY_IN_NEW_VIEWS ? getHeight() : 0;
                    int animationDelay2 = animationDelay + SgvAnimationHelper.getDefaultAnimationDuration();
                    childToAnimate.setTranslationX(0);
                    childToAnimate.setTranslationY(yTranslation2);
                    switch (animationInMode) {
                        case EXPAND_NEW_VIEWS:
                        case EXPAND_NEW_VIEWS_NO_CASCADE:
                            if (i == 0) {
                                childToAnimate.setAlpha(0.0f);
                                int offset = childToAnimate.getHeight() * (-1);
                                SgvAnimationHelper.addXYTranslationAnimators(animators, childToAnimate, 0, offset, animationDelay2);
                                SgvAnimationHelper.addFadeAnimators(animators, childToAnimate, 0.0f, 1.0f, animationDelay2);
                            } else {
                                SgvAnimationHelper.addExpandInAnimators(animators, childToAnimate, animationDelay2);
                            }
                            viewsAnimated++;
                            break;
                        case SLIDE_IN_NEW_VIEWS:
                            int startTranslation = (int) (((double) childToAnimate.getWidth()) * 1.5d);
                            if (lp.column < this.mColCount / 2) {
                                startTranslation = -startTranslation;
                            }
                            SgvAnimationHelper.addSlideInFromRightAnimators(animators, childToAnimate, startTranslation, animationDelay2);
                            viewsAnimated++;
                            break;
                        case FLY_IN_NEW_VIEWS:
                            SgvAnimationHelper.addTranslationRotationAnimators(animators, childToAnimate, 0, yTranslation2, 25.0f, animationDelay2);
                            viewsAnimated++;
                            break;
                        case FADE:
                            SgvAnimationHelper.addFadeAnimators(animators, childToAnimate, 0.0f, 1.0f, animationDelay2);
                            viewsAnimated++;
                            break;
                    }
                }
            }
        }
        return animators;
    }

    private void addStaleViewAnimationEndListener(final View view, List<Animator> viewAnimators) {
        if (viewAnimators != null) {
            for (Animator animator : viewAnimators) {
                animator.addListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        LayoutParams lp = (LayoutParams) view.getLayoutParams();
                        if (StaggeredGridView.this.mChildRectsForAnimation.containsKey(Long.valueOf(lp.id))) {
                            StaggeredGridView.this.mChildRectsForAnimation.remove(Long.valueOf(lp.id));
                        }
                        StaggeredGridView.this.recycleView(view);
                    }
                });
            }
        }
    }

    private void calculateLayoutStartOffsets(int offset) {
        int heightSpec;
        if (this.mFirstPosition != 0 && (!this.mGuardAgainstJaggedEdges || this.mFirstPosition < this.mFirstChangedPosition)) {
            System.arraycopy(this.mItemTops, 0, this.mItemBottoms, 0, this.mColCount);
            return;
        }
        int colWidth = (((getWidth() - getPaddingLeft()) - getPaddingRight()) - (this.mItemMargin * (this.mColCount - 1))) / this.mColCount;
        Arrays.fill(this.mItemTops, getPaddingTop());
        Arrays.fill(this.mItemBottoms, getPaddingTop());
        if (this.mDataChanged) {
            this.mLayoutRecords.clear();
        }
        for (int i = 0; i < this.mFirstPosition; i++) {
            LayoutRecord rec = this.mLayoutRecords.get(i);
            if (this.mDataChanged || rec == null) {
                View view = obtainView(i, null);
                LayoutParams lp = (LayoutParams) view.getLayoutParams();
                if (lp.height == -2) {
                    heightSpec = View.MeasureSpec.makeMeasureSpec(0, 0);
                } else {
                    heightSpec = View.MeasureSpec.makeMeasureSpec(lp.height, 1073741824);
                }
                int span = Math.min(this.mColCount, lp.span);
                int widthSize = (colWidth * span) + (this.mItemMargin * (span - 1));
                int widthSpec = View.MeasureSpec.makeMeasureSpec(widthSize, 1073741824);
                view.measure(widthSpec, heightSpec);
                int height = view.getMeasuredHeight();
                if (rec == null) {
                    rec = new LayoutRecord();
                    this.mLayoutRecords.put(i, rec);
                }
                rec.height = height;
                rec.id = lp.id;
                rec.span = span;
                this.mRecycler.addScrap(view);
            }
            int nextColumn = getNextColumnDown();
            if (rec.span > 1) {
                if (this.mIsRtlLayout) {
                    if (nextColumn + 1 < rec.span) {
                        nextColumn = this.mColCount - 1;
                    }
                } else if (this.mColCount - nextColumn < rec.span) {
                    nextColumn = 0;
                }
            }
            rec.column = nextColumn;
            int lowest = this.mItemBottoms[nextColumn] + this.mItemMargin;
            if (rec.span > 1) {
                for (int spanIndex = 0; spanIndex < rec.span; spanIndex++) {
                    int index = this.mIsRtlLayout ? nextColumn - spanIndex : nextColumn + spanIndex;
                    int bottom = this.mItemBottoms[index] + this.mItemMargin;
                    if (bottom > lowest) {
                        lowest = bottom;
                    }
                }
            }
            for (int spanIndex2 = 0; spanIndex2 < rec.span; spanIndex2++) {
                int col = this.mIsRtlLayout ? nextColumn - spanIndex2 : nextColumn + spanIndex2;
                this.mItemBottoms[col] = rec.height + lowest;
                if (Log.isLoggable(TAG, 2)) {
                    Log.v(TAG, " position: " + i + " bottoms: ");
                    for (int j = 0; j < this.mColCount; j++) {
                        Log.v(TAG, "    mItemBottoms[" + j + "]: " + this.mItemBottoms[j]);
                    }
                }
            }
        }
        int highestValue = Integer.MAX_VALUE;
        for (int k = 0; k < this.mColCount; k++) {
            if (this.mItemBottoms[k] < highestValue) {
                highestValue = this.mItemBottoms[k];
            }
        }
        for (int k2 = 0; k2 < this.mColCount; k2++) {
            this.mItemBottoms[k2] = (this.mItemBottoms[k2] - highestValue) + offset;
            this.mItemTops[k2] = this.mItemBottoms[k2];
        }
    }

    final void layoutChildren(boolean queryAdapter) {
        int childLeft;
        int childRight;
        int heightSpec;
        View newView;
        int paddingLeft = getPaddingLeft();
        int paddingRight = getPaddingRight();
        int itemMargin = this.mItemMargin;
        int availableWidth = ((getWidth() - paddingLeft) - paddingRight) - ((this.mColCount - 1) * itemMargin);
        int colWidth = availableWidth / this.mColCount;
        int remainder = availableWidth % this.mColCount;
        boolean viewsRemovedInLayout = false;
        boolean deferRecyclingForAnimation = this.mAnimationOutMode != SgvAnimationHelper.AnimationOut.NONE;
        if (!deferRecyclingForAnimation) {
            int childCount = getChildCount();
            int viewsToKeepOnScreen = this.mItemCount <= this.mFirstPosition ? 0 : this.mItemCount - this.mFirstPosition;
            if (childCount > viewsToKeepOnScreen) {
                recycleViewsInRange(viewsToKeepOnScreen, childCount - 1);
                viewsRemovedInLayout = true;
            }
        } else {
            this.mViewsToAnimateOut.clear();
        }
        for (int i = 0; i < getChildCount(); i++) {
            int position = this.mFirstPosition + i;
            View child = getChildAt(i);
            int highestAvailableLayoutPosition = this.mItemBottoms[getNextColumnDown()];
            if (deferRecyclingForAnimation && (position >= this.mItemCount || highestAvailableLayoutPosition >= getHeight())) {
                this.mViewsToAnimateOut.add(child);
            } else {
                LayoutParams lp = null;
                int col = -1;
                if (child != null) {
                    lp = (LayoutParams) child.getLayoutParams();
                    col = lp.column;
                }
                boolean needsLayout = queryAdapter || child == null || child.isLayoutRequested();
                if (queryAdapter) {
                    if (deferRecyclingForAnimation) {
                        newView = obtainView(position);
                    } else {
                        newView = obtainView(position, child);
                    }
                    lp = (LayoutParams) newView.getLayoutParams();
                    if (newView != child) {
                        if (child != null && !deferRecyclingForAnimation) {
                            this.mRecycler.addScrap(child);
                            removeViewInLayout(child);
                            viewsRemovedInLayout = true;
                        }
                        if (newView.getParent() == this) {
                            detachViewFromParent(newView);
                            attachViewToParent(newView, i, lp);
                        } else {
                            addViewInLayout(newView, i, lp);
                        }
                    }
                    child = newView;
                    lp.column = getNextColumnDown();
                    col = lp.column;
                }
                setReorderingArea(lp);
                int span = Math.min(this.mColCount, lp.span);
                if (span > 1) {
                    if (this.mIsRtlLayout) {
                        if (col + 1 < span) {
                            col = this.mColCount - 1;
                        }
                    } else if (this.mColCount - col < span) {
                        col = 0;
                    }
                    lp.column = col;
                }
                int widthSize = (colWidth * span) + ((span - 1) * itemMargin);
                if ((this.mIsRtlLayout && span == col + 1) || (!this.mIsRtlLayout && span + col == this.mColCount)) {
                    widthSize += remainder;
                }
                if (needsLayout) {
                    int widthSpec = View.MeasureSpec.makeMeasureSpec(widthSize, 1073741824);
                    if (lp.height == -2) {
                        heightSpec = View.MeasureSpec.makeMeasureSpec(0, 0);
                    } else {
                        heightSpec = View.MeasureSpec.makeMeasureSpec(lp.height, 1073741824);
                    }
                    child.measure(widthSpec, heightSpec);
                }
                int childTop = this.mItemBottoms[col] + this.mItemMargin;
                if (span > 1) {
                    int lowest = childTop;
                    for (int spanIndex = 0; spanIndex < span; spanIndex++) {
                        int index = this.mIsRtlLayout ? col - spanIndex : col + spanIndex;
                        int bottom = this.mItemBottoms[index] + this.mItemMargin;
                        if (bottom > lowest) {
                            lowest = bottom;
                        }
                    }
                    childTop = lowest;
                }
                int childHeight = child.getMeasuredHeight();
                int childBottom = childTop + childHeight;
                if (this.mIsRtlLayout) {
                    childRight = (getWidth() - paddingRight) - (((this.mColCount - col) - 1) * (colWidth + itemMargin));
                    childLeft = childRight - child.getMeasuredWidth();
                } else {
                    childLeft = paddingLeft + ((colWidth + itemMargin) * col);
                    childRight = childLeft + child.getMeasuredWidth();
                }
                child.layout(childLeft, childTop, childRight, childBottom);
                if (lp.id == this.mFocusedChildIdToScrollIntoView) {
                    child.requestFocus();
                }
                for (int spanIndex2 = 0; spanIndex2 < span; spanIndex2++) {
                    int index2 = this.mIsRtlLayout ? col - spanIndex2 : col + spanIndex2;
                    this.mItemBottoms[index2] = childBottom;
                }
                LayoutRecord rec = this.mLayoutRecords.get(position);
                if (rec == null) {
                    rec = new LayoutRecord();
                    this.mLayoutRecords.put(position, rec);
                }
                rec.column = lp.column;
                rec.height = childHeight;
                rec.id = lp.id;
                rec.span = span;
            }
        }
        if (viewsRemovedInLayout || deferRecyclingForAnimation) {
            invalidate();
        }
    }

    private void setReorderingArea(LayoutParams childLayoutParams) {
        boolean isLastColumn = childLayoutParams.column == this.mColCount + (-1);
        childLayoutParams.reorderingArea = this.mAdapter.getReorderingArea(childLayoutParams.position, isLastColumn);
    }

    final void invalidateLayoutRecordsBeforePosition(int position) {
        int endAt = 0;
        while (endAt < this.mLayoutRecords.size() && this.mLayoutRecords.keyAt(endAt) < position) {
            endAt++;
        }
        this.mLayoutRecords.removeAtRange(0, endAt);
    }

    final void invalidateLayoutRecordsAfterPosition(int position) {
        int beginAt = this.mLayoutRecords.size() - 1;
        while (beginAt >= 0 && this.mLayoutRecords.keyAt(beginAt) > position) {
            beginAt--;
        }
        int beginAt2 = beginAt + 1;
        this.mLayoutRecords.removeAtRange(beginAt2 + 1, this.mLayoutRecords.size() - beginAt2);
    }

    private void cacheChildRects() {
        int childCount = getChildCount();
        this.mChildRectsForAnimation.clear();
        long originalDraggedChildId = -1;
        if (isDragReorderingSupported()) {
            originalDraggedChildId = this.mReorderHelper.getDraggedChildId();
            if (this.mCachedDragViewRect != null && originalDraggedChildId != -1) {
                this.mChildRectsForAnimation.put(Long.valueOf(originalDraggedChildId), new ViewRectPair(this.mDragView, this.mCachedDragViewRect));
                this.mCachedDragViewRect = null;
            }
        }
        for (int i = 0; i < childCount; i++) {
            View child = getChildAt(i);
            LayoutParams lp = (LayoutParams) child.getLayoutParams();
            if (lp.id != originalDraggedChildId) {
                int childTop = (int) child.getY();
                int childBottom = childTop + child.getHeight();
                int childLeft = (int) child.getX();
                int childRight = childLeft + child.getWidth();
                Rect rect = new Rect(childLeft, childTop, childRight, childBottom);
                this.mChildRectsForAnimation.put(Long.valueOf(lp.id), new ViewRectPair(child, rect));
            }
        }
    }

    final int fillUp(int fromPosition, int overhang) {
        LayoutRecord rec;
        int heightSpec;
        int childLeft;
        int childRight;
        int paddingLeft = getPaddingLeft();
        int paddingRight = getPaddingRight();
        int itemMargin = this.mItemMargin;
        int availableWidth = ((getWidth() - paddingLeft) - paddingRight) - ((this.mColCount - 1) * itemMargin);
        int colWidth = availableWidth / this.mColCount;
        int remainder = availableWidth % this.mColCount;
        int gridTop = getPaddingTop();
        int fillTo = -overhang;
        int nextCol = getNextColumnUp();
        for (int position = fromPosition; nextCol >= 0 && this.mItemTops[nextCol] > fillTo && position >= 0; position--) {
            View child = obtainView(position, null);
            LayoutParams lp = (LayoutParams) child.getLayoutParams();
            if (child.getParent() != this) {
                if (this.mInLayout) {
                    addViewInLayout(child, 0, lp);
                } else {
                    addView(child, 0);
                }
            }
            int span = Math.min(this.mColCount, lp.span);
            if (span > 1) {
                rec = getNextRecordUp(position, span);
                nextCol = rec.column;
            } else {
                rec = this.mLayoutRecords.get(position);
            }
            boolean invalidateBefore = false;
            if (rec == null) {
                rec = new LayoutRecord();
                this.mLayoutRecords.put(position, rec);
                rec.column = nextCol;
                rec.span = span;
            } else if (span != rec.span) {
                rec.span = span;
                rec.column = nextCol;
                invalidateBefore = true;
            } else {
                nextCol = rec.column;
            }
            if (this.mHasStableIds) {
                rec.id = lp.id;
            }
            lp.column = nextCol;
            setReorderingArea(lp);
            int widthSize = (colWidth * span) + ((span - 1) * itemMargin);
            if ((this.mIsRtlLayout && span == nextCol + 1) || (!this.mIsRtlLayout && span + nextCol == this.mColCount)) {
                widthSize += remainder;
            }
            int widthSpec = View.MeasureSpec.makeMeasureSpec(widthSize, 1073741824);
            if (lp.height == -2) {
                heightSpec = View.MeasureSpec.makeMeasureSpec(0, 0);
            } else {
                heightSpec = View.MeasureSpec.makeMeasureSpec(lp.height, 1073741824);
            }
            child.measure(widthSpec, heightSpec);
            int childHeight = child.getMeasuredHeight();
            if (invalidateBefore || (childHeight != rec.height && rec.height > 0)) {
                invalidateLayoutRecordsBeforePosition(position);
            }
            rec.height = childHeight;
            for (int i = 0; i < span; i++) {
                int index = this.mIsRtlLayout ? nextCol - i : nextCol + i;
                int[] iArr = this.mItemTops;
                iArr[index] = iArr[index] + rec.getMarginBelow(i);
            }
            int startFrom = this.mItemTops[nextCol];
            int childTop = startFrom - childHeight;
            if (this.mIsRtlLayout) {
                childRight = (getWidth() - paddingRight) - (((this.mColCount - nextCol) - 1) * (colWidth + itemMargin));
                childLeft = childRight - child.getMeasuredWidth();
            } else {
                childLeft = paddingLeft + ((colWidth + itemMargin) * nextCol);
                childRight = childLeft + child.getMeasuredWidth();
            }
            child.layout(childLeft, childTop, childRight, startFrom);
            Log.v(TAG, "[fillUp] position: " + position + " id: " + lp.id + " childLeft: " + childLeft + " childTop: " + childTop + " column: " + rec.column + " childHeight:" + childHeight);
            for (int i2 = 0; i2 < span; i2++) {
                this.mItemTops[this.mIsRtlLayout ? nextCol - i2 : nextCol + i2] = (childTop - rec.getMarginAbove(i2)) - itemMargin;
            }
            if (lp.id == this.mFocusedChildIdToScrollIntoView) {
                child.requestFocus();
            }
            nextCol = getNextColumnUp();
            this.mFirstPosition = position;
        }
        int highestView = getHeight();
        for (int i3 = 0; i3 < this.mColCount; i3++) {
            if (this.mItemTops[i3] < highestView) {
                highestView = this.mItemTops[i3];
            }
        }
        return gridTop - highestView;
    }

    final int fillDown(int fromPosition, int overhang) {
        LayoutRecord rec;
        int heightSpec;
        int childLeft;
        int childRight;
        int paddingLeft = getPaddingLeft();
        int paddingRight = getPaddingRight();
        int itemMargin = this.mItemMargin;
        int availableWidth = ((getWidth() - paddingLeft) - paddingRight) - ((this.mColCount - 1) * itemMargin);
        int colWidth = availableWidth / this.mColCount;
        int remainder = availableWidth % this.mColCount;
        int gridBottom = getHeight() - getPaddingBottom();
        int fillTo = gridBottom + overhang;
        int nextCol = getNextColumnDown();
        for (int position = fromPosition; nextCol >= 0 && this.mItemBottoms[nextCol] < fillTo && position < this.mItemCount; position++) {
            View child = obtainView(position, null);
            LayoutParams lp = (LayoutParams) child.getLayoutParams();
            if (child.getParent() != this) {
                if (this.mInLayout) {
                    addViewInLayout(child, -1, lp);
                } else {
                    addView(child);
                }
            }
            int span = Math.min(this.mColCount, lp.span);
            if (span > 1) {
                rec = getNextRecordDown(position, span);
                nextCol = rec.column;
            } else {
                rec = this.mLayoutRecords.get(position);
            }
            boolean invalidateAfter = false;
            if (rec == null) {
                rec = new LayoutRecord();
                this.mLayoutRecords.put(position, rec);
                rec.column = nextCol;
                rec.span = span;
            } else if (span != rec.span) {
                rec.span = span;
                rec.column = nextCol;
                invalidateAfter = true;
            } else {
                nextCol = rec.column;
            }
            if (this.mHasStableIds) {
                rec.id = lp.id;
            }
            lp.column = nextCol;
            setReorderingArea(lp);
            int widthSize = (colWidth * span) + ((span - 1) * itemMargin);
            if ((this.mIsRtlLayout && span == nextCol + 1) || (!this.mIsRtlLayout && span + nextCol == this.mColCount)) {
                widthSize += remainder;
            }
            int widthSpec = View.MeasureSpec.makeMeasureSpec(widthSize, 1073741824);
            if (lp.height == -2) {
                heightSpec = View.MeasureSpec.makeMeasureSpec(0, 0);
            } else {
                heightSpec = View.MeasureSpec.makeMeasureSpec(lp.height, 1073741824);
            }
            child.measure(widthSpec, heightSpec);
            int childHeight = child.getMeasuredHeight();
            if (invalidateAfter || (childHeight != rec.height && rec.height > 0)) {
                invalidateLayoutRecordsAfterPosition(position);
            }
            rec.height = childHeight;
            for (int i = 0; i < span; i++) {
                int index = this.mIsRtlLayout ? nextCol - i : nextCol + i;
                int[] iArr = this.mItemBottoms;
                iArr[index] = iArr[index] + rec.getMarginAbove(i);
            }
            int startFrom = this.mItemBottoms[nextCol];
            int childTop = startFrom + itemMargin;
            int childBottom = childTop + childHeight;
            if (this.mIsRtlLayout) {
                childRight = (getWidth() - paddingRight) - (((this.mColCount - nextCol) - 1) * (colWidth + itemMargin));
                childLeft = childRight - child.getMeasuredWidth();
            } else {
                childLeft = paddingLeft + ((colWidth + itemMargin) * nextCol);
                childRight = childLeft + child.getMeasuredWidth();
            }
            Log.v(TAG, "[fillDown] position: " + position + " id: " + lp.id + " childLeft: " + childLeft + " childTop: " + childTop + " column: " + rec.column + " childHeight:" + childHeight);
            child.layout(childLeft, childTop, childRight, childBottom);
            for (int i2 = 0; i2 < span; i2++) {
                this.mItemBottoms[this.mIsRtlLayout ? nextCol - i2 : nextCol + i2] = rec.getMarginBelow(i2) + childBottom;
            }
            if (lp.id == this.mFocusedChildIdToScrollIntoView) {
                child.requestFocus();
            }
            nextCol = getNextColumnDown();
        }
        int lowestView = 0;
        for (int i3 = 0; i3 < this.mColCount; i3++) {
            int index2 = this.mIsRtlLayout ? this.mColCount - (i3 + 1) : i3;
            if (this.mItemBottoms[index2] > lowestView) {
                lowestView = this.mItemBottoms[index2];
            }
        }
        return lowestView - gridBottom;
    }

    final int getNextColumnUp() {
        int result = -1;
        int bottomMost = Integer.MIN_VALUE;
        int colCount = this.mColCount;
        for (int i = colCount - 1; i >= 0; i--) {
            int index = this.mIsRtlLayout ? colCount - (i + 1) : i;
            int top = this.mItemTops[index];
            if (top > bottomMost) {
                bottomMost = top;
                result = index;
            }
        }
        return result;
    }

    final LayoutRecord getNextRecordUp(int position, int span) {
        LayoutRecord rec = this.mLayoutRecords.get(position);
        if (rec == null || rec.span != span) {
            if (span > this.mColCount) {
                throw new IllegalStateException("Span larger than column count! Span:" + span + " ColumnCount:" + this.mColCount);
            }
            rec = new LayoutRecord();
            rec.span = span;
            this.mLayoutRecords.put(position, rec);
        }
        int targetCol = -1;
        int bottomMost = Integer.MIN_VALUE;
        int colCount = this.mColCount;
        if (this.mIsRtlLayout) {
            for (int i = span - 1; i < colCount; i++) {
                int top = Integer.MAX_VALUE;
                for (int j = i; j > i - span; j--) {
                    int singleTop = this.mItemTops[j];
                    if (singleTop < top) {
                        top = singleTop;
                    }
                }
                if (top > bottomMost) {
                    bottomMost = top;
                    targetCol = i;
                }
            }
        } else {
            for (int i2 = colCount - span; i2 >= 0; i2--) {
                int top2 = Integer.MAX_VALUE;
                for (int j2 = i2; j2 < i2 + span; j2++) {
                    int singleTop2 = this.mItemTops[j2];
                    if (singleTop2 < top2) {
                        top2 = singleTop2;
                    }
                }
                if (top2 > bottomMost) {
                    bottomMost = top2;
                    targetCol = i2;
                }
            }
        }
        rec.column = targetCol;
        for (int i3 = 0; i3 < span; i3++) {
            int nextCol = this.mIsRtlLayout ? targetCol - i3 : targetCol + i3;
            rec.setMarginBelow(i3, this.mItemTops[nextCol] - bottomMost);
        }
        return rec;
    }

    final int getNextColumnDown() {
        int topMost = Integer.MAX_VALUE;
        int result = 0;
        int colCount = this.mColCount;
        for (int i = 0; i < colCount; i++) {
            int index = this.mIsRtlLayout ? colCount - (i + 1) : i;
            int bottom = this.mItemBottoms[index];
            if (bottom < topMost) {
                topMost = bottom;
                result = index;
            }
        }
        return result;
    }

    final LayoutRecord getNextRecordDown(int position, int span) {
        LayoutRecord rec = this.mLayoutRecords.get(position);
        if (rec == null || rec.span != span) {
            if (span > this.mColCount) {
                throw new IllegalStateException("Span larger than column count! Span:" + span + " ColumnCount:" + this.mColCount);
            }
            rec = new LayoutRecord();
            rec.span = span;
            this.mLayoutRecords.put(position, rec);
        }
        int targetCol = -1;
        int topMost = Integer.MAX_VALUE;
        int colCount = this.mColCount;
        if (this.mIsRtlLayout) {
            for (int i = colCount - 1; i >= span - 1; i--) {
                int bottom = Integer.MIN_VALUE;
                for (int j = i; j > i - span; j--) {
                    int singleBottom = this.mItemBottoms[j];
                    if (singleBottom > bottom) {
                        bottom = singleBottom;
                    }
                }
                if (bottom < topMost) {
                    topMost = bottom;
                    targetCol = i;
                }
            }
        } else {
            for (int i2 = 0; i2 <= colCount - span; i2++) {
                int bottom2 = Integer.MIN_VALUE;
                for (int j2 = i2; j2 < i2 + span; j2++) {
                    int singleBottom2 = this.mItemBottoms[j2];
                    if (singleBottom2 > bottom2) {
                        bottom2 = singleBottom2;
                    }
                }
                if (bottom2 < topMost) {
                    topMost = bottom2;
                    targetCol = i2;
                }
            }
        }
        rec.column = targetCol;
        for (int i3 = 0; i3 < span; i3++) {
            int nextCol = this.mIsRtlLayout ? targetCol - i3 : targetCol + i3;
            rec.setMarginAbove(i3, topMost - this.mItemBottoms[nextCol]);
        }
        return rec;
    }

    private int getItemWidth(int itemColumnSpan) {
        int colWidth = (((getWidth() - getPaddingLeft()) - getPaddingRight()) - (this.mItemMargin * (this.mColCount - 1))) / this.mColCount;
        return (colWidth * itemColumnSpan) + (this.mItemMargin * (itemColumnSpan - 1));
    }

    final View obtainView(int position) {
        Object item = this.mAdapter.getItem(position);
        View scrap = null;
        int positionViewType = this.mAdapter.getItemViewType(item, position);
        long id = this.mAdapter.getItemId(item, position);
        ViewRectPair viewRectPair = this.mChildRectsForAnimation.get(Long.valueOf(id));
        if (viewRectPair != null) {
            scrap = viewRectPair.view;
        }
        int scrapViewType = (scrap == null || !(scrap.getLayoutParams() instanceof LayoutParams)) ? -1 : ((LayoutParams) scrap.getLayoutParams()).viewType;
        if (scrap == null || scrapViewType != positionViewType) {
            if (scrap != null) {
                recycleView(scrap);
            }
            scrap = this.mRecycler.getScrapView(positionViewType);
        }
        int itemColumnSpan = this.mAdapter.getItemColumnSpan(item, position);
        int itemWidth = getItemWidth(itemColumnSpan);
        View view = this.mAdapter.getView(item, position, scrap, this, itemWidth);
        ViewGroup.LayoutParams lp = view.getLayoutParams();
        if (view.getParent() != this) {
            if (lp == null) {
                lp = generateDefaultLayoutParams();
            } else if (!checkLayoutParams(lp)) {
                lp = generateLayoutParams(lp);
            }
            view.setLayoutParams(lp);
        }
        LayoutParams sglp = (LayoutParams) view.getLayoutParams();
        sglp.position = position;
        sglp.viewType = positionViewType;
        sglp.id = id;
        sglp.span = itemColumnSpan;
        if (isDragReorderingSupported() && this.mReorderHelper.getDraggedChildId() == id) {
            this.mReorderHelper.updateDraggedChildView(view);
            this.mReorderHelper.updateDraggedOverChildView(view);
        }
        return view;
    }

    final View obtainView(int position, View optScrap) {
        View view = this.mRecycler.getTransientStateView(position);
        Object item = this.mAdapter.getItem(position);
        int positionViewType = this.mAdapter.getItemViewType(item, position);
        if (view == null) {
            int optType = optScrap != null ? ((LayoutParams) optScrap.getLayoutParams()).viewType : -1;
            View scrap = optType == positionViewType ? optScrap : this.mRecycler.getScrapView(positionViewType);
            int itemColumnSpan = this.mAdapter.getItemColumnSpan(item, position);
            int itemWidth = getItemWidth(itemColumnSpan);
            view = this.mAdapter.getView(item, position, scrap, this, itemWidth);
            if (view != scrap && scrap != null) {
                this.mRecycler.addScrap(scrap);
            }
            ViewGroup.LayoutParams lp = view.getLayoutParams();
            if (view.getParent() != this) {
                if (lp == null) {
                    lp = generateDefaultLayoutParams();
                } else if (!checkLayoutParams(lp)) {
                    lp = generateLayoutParams(lp);
                }
                view.setLayoutParams(lp);
            }
        }
        LayoutParams sglp = (LayoutParams) view.getLayoutParams();
        sglp.position = position;
        sglp.viewType = positionViewType;
        long id = this.mAdapter.getItemIdFromView(view, position);
        sglp.id = id;
        sglp.span = this.mAdapter.getItemColumnSpan(item, position);
        if (isDragReorderingSupported() && this.mReorderHelper.getDraggedChildId() == id) {
            this.mReorderHelper.updateDraggedChildView(view);
            this.mReorderHelper.updateDraggedOverChildView(view);
        }
        return view;
    }

    public void setAdapter(GridAdapter adapter) {
        if (this.mAdapter != null) {
            this.mAdapter.unregisterDataSetObserver(this.mObserver);
        }
        clearAllState();
        this.mAdapter = adapter;
        this.mDataChanged = true;
        this.mItemCount = adapter != null ? adapter.getCount() : 0;
        if (adapter != null) {
            adapter.registerDataSetObserver(this.mObserver);
            this.mRecycler.setViewTypeCount(adapter.getViewTypeCount());
            this.mHasStableIds = adapter.hasStableIds();
        } else {
            this.mHasStableIds = false;
        }
        if (isDragReorderingSupported()) {
            updateReorderStates(0);
        }
        updateEmptyStatus();
    }

    private void clearAllState() {
        this.mLayoutRecords.clear();
        removeAllViews();
        this.mItemTops = null;
        this.mItemBottoms = null;
        setSelectionToTop();
        this.mRecycler.clear();
        this.mLastTouchY = 0.0f;
        this.mFirstChangedPosition = 0;
    }

    public void setSelectionToTop() {
        this.mCurrentScrollState = null;
        setFirstPositionAndOffsets(0, getPaddingTop());
    }

    private boolean isSelectionAtTop() {
        return this.mCurrentScrollState != null && this.mCurrentScrollState.getAdapterPosition() == 0 && this.mCurrentScrollState.getVerticalOffset() == this.mItemMargin;
    }

    public void setFirstPositionAndOffsets(int position, int offset) {
        this.mFirstPosition = position;
        if (this.mItemTops == null || this.mItemBottoms == null) {
            this.mItemTops = new int[this.mColCount];
            this.mItemBottoms = new int[this.mColCount];
        }
        calculateLayoutStartOffsets(offset);
    }

    private void restoreScrollPosition(ScrollState scrollState) {
        if (this.mAdapter != null && scrollState != null && this.mAdapter.getCount() != 0) {
            Log.v(TAG, "[restoreScrollPosition] " + scrollState);
            int targetPosition = 0;
            int originalPosition = scrollState.getAdapterPosition();
            int adapterCount = this.mAdapter.getCount();
            int i = 0;
            while (true) {
                if (i >= 10) {
                    break;
                }
                if (originalPosition + i < adapterCount) {
                    long itemId = this.mAdapter.getItemId(originalPosition + i);
                    if (itemId != -1 && itemId == scrollState.getItemId()) {
                        targetPosition = originalPosition + i;
                        break;
                    }
                    if (originalPosition - i >= 0 && originalPosition - i < adapterCount) {
                        long itemId2 = this.mAdapter.getItemId(originalPosition - i);
                        if (itemId2 != -1 && itemId2 == scrollState.getItemId()) {
                            targetPosition = originalPosition - i;
                            break;
                        }
                    }
                    i++;
                }
            }
            int offset = scrollState.getVerticalOffset() - this.mItemMargin;
            if (targetPosition == 0) {
                offset += getPaddingTop();
            }
            setFirstPositionAndOffsets(targetPosition, offset);
            this.mCurrentScrollState = null;
        }
    }

    public ScrollState getScrollState() {
        View v = getChildAt(0);
        if (v == null) {
            return null;
        }
        LayoutParams lp = (LayoutParams) v.getLayoutParams();
        int offset = lp.position == 0 ? v.getTop() - getPaddingTop() : v.getTop();
        return new ScrollState(lp.id, lp.position, offset);
    }

    @Override
    public boolean requestChildRectangleOnScreen(View child, Rect rectangle, boolean immediate) {
        rectangle.offset(child.getLeft() - child.getScrollX(), child.getTop() - child.getScrollY());
        return scrollToChildRect(rectangle, immediate);
    }

    private boolean scrollToChildRect(Rect rect, boolean immediate) {
        int delta = computeScrollDeltaToGetChildRectOnScreen(rect);
        boolean scroll = delta != 0;
        if (scroll) {
            scrollBy(0, delta);
        }
        return scroll;
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        if (this.mOnSizeChangedListener != null) {
            this.mOnSizeChangedListener.onSizeChanged(w, h, oldw, oldh);
        }
        View currentFocused = findFocus();
        if (currentFocused != null && this != currentFocused && isWithinDeltaOfScreen(currentFocused, 0, oldh)) {
            currentFocused.getDrawingRect(this.mTempRect);
            offsetDescendantRectToMyCoords(currentFocused, this.mTempRect);
            scrollBy(0, computeScrollDeltaToGetChildRectOnScreen(this.mTempRect));
        }
    }

    private boolean isWithinDeltaOfScreen(View descendant, int delta, int height) {
        descendant.getDrawingRect(this.mTempRect);
        offsetDescendantRectToMyCoords(descendant, this.mTempRect);
        return this.mTempRect.bottom + delta >= getScrollY() && this.mTempRect.top - delta <= getScrollY() + height;
    }

    @Override
    protected int computeVerticalScrollExtent() {
        int count = getChildCount();
        if (count <= 0) {
            return 0;
        }
        if (this.mSmoothScrollbarEnabled) {
            int rowCount = ((this.mColCount + count) - 1) / this.mColCount;
            int extent = rowCount * 100;
            View view = getChildAt(0);
            int top = view.getTop();
            int height = view.getHeight();
            if (height > 0) {
                extent += (top * 100) / height;
            }
            View view2 = getChildAt(count - 1);
            int bottom = view2.getBottom();
            int height2 = view2.getHeight();
            if (height2 > 0) {
                return extent - (((bottom - getHeight()) * 100) / height2);
            }
            return extent;
        }
        return 1;
    }

    @Override
    protected int computeVerticalScrollOffset() {
        int index;
        int firstPosition = this.mFirstPosition;
        int childCount = getChildCount();
        int paddingTop = getPaddingTop();
        if (firstPosition >= 0 && childCount > 0) {
            if (this.mSmoothScrollbarEnabled) {
                View view = getChildAt(0);
                int top = view.getTop();
                int currentTopViewHeight = view.getHeight();
                if (currentTopViewHeight > 0) {
                    int estimatedScrollOffset = ((firstPosition * 100) / this.mColCount) - ((top * 100) / currentTopViewHeight);
                    int rowCount = ((this.mItemCount + this.mColCount) - 1) / this.mColCount;
                    int overScrollCompensation = (int) ((getScrollY() / getHeight()) * rowCount * 100.0f);
                    int val = Math.max(estimatedScrollOffset + overScrollCompensation, 0);
                    if (firstPosition == 0 && paddingTop > 0) {
                        return val + (paddingTop - top) + this.mItemMargin;
                    }
                    return val;
                }
            } else {
                int count = this.mItemCount;
                if (firstPosition == 0) {
                    index = 0;
                } else if (firstPosition + childCount == count) {
                    index = count;
                } else {
                    index = firstPosition + (childCount / 2);
                }
                return (int) (firstPosition + (childCount * (index / count)));
            }
        }
        return paddingTop;
    }

    @Override
    protected int computeVerticalScrollRange() {
        int rowCount = ((this.mItemCount + this.mColCount) - 1) / this.mColCount;
        int result = Math.max(rowCount * 100, 0);
        if (this.mSmoothScrollbarEnabled) {
            if (getScrollY() != 0) {
                return result + Math.abs((int) ((getScrollY() / getHeight()) * rowCount * 100.0f));
            }
            return result;
        }
        return this.mItemCount;
    }

    protected int computeScrollDeltaToGetChildRectOnScreen(Rect rect) {
        if (getChildCount() == 0) {
            return 0;
        }
        int height = getHeight();
        int fadingEdge = getVerticalFadingEdgeLength();
        int screenTop = getScrollY();
        int screenBottom = screenTop + height;
        if (rect.top > 0) {
            screenTop += fadingEdge;
        }
        if (rect.bottom < getHeight()) {
            screenBottom -= fadingEdge;
        }
        if (rect.bottom > screenBottom && rect.top > screenTop) {
            if (rect.height() > height) {
                int scrollYDelta = screenTop - rect.top;
                return scrollYDelta;
            }
            int scrollYDelta2 = screenBottom - rect.bottom;
            return scrollYDelta2;
        }
        if (rect.top >= screenTop || rect.bottom >= screenBottom) {
            return 0;
        }
        if (rect.height() > height) {
            int scrollYDelta3 = screenBottom - rect.bottom;
            return scrollYDelta3;
        }
        int scrollYDelta4 = screenTop - rect.top;
        return scrollYDelta4;
    }

    @Override
    protected LayoutParams generateDefaultLayoutParams() {
        return new LayoutParams(-2);
    }

    @Override
    protected LayoutParams generateLayoutParams(ViewGroup.LayoutParams lp) {
        return new LayoutParams(lp);
    }

    @Override
    protected boolean checkLayoutParams(ViewGroup.LayoutParams lp) {
        return lp instanceof LayoutParams;
    }

    @Override
    public ViewGroup.LayoutParams generateLayoutParams(AttributeSet attrs) {
        return new LayoutParams(getContext(), attrs);
    }

    @Override
    public Parcelable onSaveInstanceState() {
        Parcelable superState = super.onSaveInstanceState();
        SavedState ss = new SavedState(superState);
        int position = this.mFirstPosition;
        ss.position = position;
        if (position >= 0 && this.mAdapter != null && position < this.mAdapter.getCount()) {
            ss.firstId = this.mAdapter.getItemId(position);
        }
        if (getChildCount() > 0) {
            ss.topOffset = position == 0 ? getChildAt(0).getTop() - getPaddingTop() : getChildAt(0).getTop();
        }
        return ss;
    }

    @Override
    public void onRestoreInstanceState(Parcelable state) {
        SavedState ss = (SavedState) state;
        super.onRestoreInstanceState(ss.getSuperState());
        this.mDataChanged = true;
        this.mFirstPosition = ss.position;
        this.mCurrentScrollState = new ScrollState(ss.firstId, ss.position, ss.topOffset);
        requestLayout();
    }

    public static class LayoutParams extends ViewGroup.LayoutParams {
        private static final int[] LAYOUT_ATTRS = {R.attr.layout_span};
        int column;
        long id;
        public int position;
        public int reorderingArea;
        public int span;
        int viewType;

        public LayoutParams(int height) {
            super(-1, height);
            this.span = 1;
            this.position = -1;
            this.id = -1L;
            this.reorderingArea = 0;
            if (this.height == -1) {
                Log.w(StaggeredGridView.TAG, "Constructing LayoutParams with height FILL_PARENT - impossible! Falling back to WRAP_CONTENT");
                this.height = -2;
            }
        }

        public LayoutParams(Context c, AttributeSet attrs) {
            super(c, attrs);
            this.span = 1;
            this.position = -1;
            this.id = -1L;
            this.reorderingArea = 0;
            if (this.width != -1) {
                Log.w(StaggeredGridView.TAG, "Inflation setting LayoutParams width to " + this.width + " - must be MATCH_PARENT");
                this.width = -1;
            }
            if (this.height == -1) {
                Log.w(StaggeredGridView.TAG, "Inflation setting LayoutParams height to MATCH_PARENT - impossible! Falling back to WRAP_CONTENT");
                this.height = -2;
            }
            TypedArray a = c.obtainStyledAttributes(attrs, LAYOUT_ATTRS);
            this.span = a.getInteger(0, 1);
            a.recycle();
        }

        public LayoutParams(ViewGroup.LayoutParams other) {
            super(other);
            this.span = 1;
            this.position = -1;
            this.id = -1L;
            this.reorderingArea = 0;
            if (this.width != -1) {
                Log.w(StaggeredGridView.TAG, "Constructing LayoutParams with width " + this.width + " - must be MATCH_PARENT");
                this.width = -1;
            }
            if (this.height == -1) {
                Log.w(StaggeredGridView.TAG, "Constructing LayoutParams with height MATCH_PARENT - impossible! Falling back to WRAP_CONTENT");
                this.height = -2;
            }
        }
    }

    private class RecycleBin {
        private int mMaxScrap;
        private ArrayList<View>[] mScrapViews;
        private SparseArray<View> mTransientStateViews;
        private int mViewTypeCount;

        private RecycleBin() {
        }

        public void setViewTypeCount(int viewTypeCount) {
            if (viewTypeCount < 1) {
                throw new IllegalArgumentException("Must have at least one view type (" + viewTypeCount + " types reported)");
            }
            if (viewTypeCount != this.mViewTypeCount) {
                ArrayList<View>[] scrapViews = new ArrayList[viewTypeCount];
                for (int i = 0; i < viewTypeCount; i++) {
                    scrapViews[i] = new ArrayList<>();
                }
                this.mViewTypeCount = viewTypeCount;
                this.mScrapViews = scrapViews;
            }
        }

        public void clear() {
            int typeCount = this.mViewTypeCount;
            for (int i = 0; i < typeCount; i++) {
                this.mScrapViews[i].clear();
            }
            if (this.mTransientStateViews != null) {
                this.mTransientStateViews.clear();
            }
        }

        public void clearTransientViews() {
            if (this.mTransientStateViews != null) {
                this.mTransientStateViews.clear();
            }
        }

        public void addScrap(View v) {
            if (v.getLayoutParams() instanceof LayoutParams) {
                LayoutParams lp = (LayoutParams) v.getLayoutParams();
                if (ViewCompat.hasTransientState(v)) {
                    if (this.mTransientStateViews == null) {
                        this.mTransientStateViews = new SparseArray<>();
                    }
                    this.mTransientStateViews.put(lp.position, v);
                    return;
                }
                int childCount = StaggeredGridView.this.getChildCount();
                if (childCount > this.mMaxScrap) {
                    this.mMaxScrap = childCount;
                }
                v.setTranslationX(0.0f);
                v.setTranslationY(0.0f);
                v.setRotation(0.0f);
                v.setAlpha(1.0f);
                v.setScaleY(1.0f);
                ArrayList<View> scrap = this.mScrapViews[lp.viewType];
                if (scrap.size() < this.mMaxScrap && !scrap.contains(v)) {
                    scrap.add(v);
                }
            }
        }

        public View getTransientStateView(int position) {
            if (this.mTransientStateViews == null) {
                return null;
            }
            View result = this.mTransientStateViews.get(position);
            if (result != null) {
                this.mTransientStateViews.remove(position);
                return result;
            }
            return result;
        }

        public View getScrapView(int type) {
            ArrayList<View> scrap = this.mScrapViews[type];
            if (scrap.isEmpty()) {
                return null;
            }
            int index = scrap.size() - 1;
            return scrap.remove(index);
        }
    }

    private class AdapterDataSetObserver extends DataSetObserver {
        private AdapterDataSetObserver() {
        }

        @Override
        public void onChanged() {
            StaggeredGridView.this.mDataChanged = true;
            StaggeredGridView.this.mItemCount = StaggeredGridView.this.mAdapter.getCount();
            StaggeredGridView.this.mFirstChangedPosition = StaggeredGridView.this.mAdapter.getFirstChangedPosition();
            if (StaggeredGridView.this.mFirstPosition >= StaggeredGridView.this.mItemCount) {
                StaggeredGridView.this.mFirstPosition = 0;
                StaggeredGridView.this.mCurrentScrollState = null;
            }
            StaggeredGridView.this.mRecycler.clearTransientViews();
            if (StaggeredGridView.this.mHasStableIds) {
                StaggeredGridView.this.cacheChildRects();
            } else {
                StaggeredGridView.this.mLayoutRecords.clear();
                int colCount = StaggeredGridView.this.mColCount;
                for (int i = 0; i < colCount; i++) {
                    StaggeredGridView.this.mItemBottoms[i] = StaggeredGridView.this.mItemTops[i];
                }
            }
            StaggeredGridView.this.updateEmptyStatus();
            StaggeredGridView.this.requestLayout();
        }

        @Override
        public void onInvalidated() {
        }
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
        long firstId;
        int position;
        int topOffset;

        SavedState(Parcelable superState) {
            super(superState);
            this.firstId = -1L;
        }

        private SavedState(Parcel in) {
            super(in);
            this.firstId = -1L;
            this.firstId = in.readLong();
            this.position = in.readInt();
            this.topOffset = in.readInt();
        }

        @Override
        public void writeToParcel(Parcel out, int flags) {
            super.writeToParcel(out, flags);
            out.writeLong(this.firstId);
            out.writeInt(this.position);
            out.writeInt(this.topOffset);
        }

        public String toString() {
            return "StaggereGridView.SavedState{" + Integer.toHexString(System.identityHashCode(this)) + " firstId=" + this.firstId + " position=" + this.position + "}";
        }
    }

    private static class ViewRectPair {
        public final Rect rect;
        public final View view;

        public ViewRectPair(View v, Rect r) {
            this.view = v;
            this.rect = r;
        }
    }

    public static class ScrollState implements Parcelable {
        public static final Parcelable.Creator<ScrollState> CREATOR = new Parcelable.Creator<ScrollState>() {
            @Override
            public ScrollState createFromParcel(Parcel source) {
                return new ScrollState(source);
            }

            @Override
            public ScrollState[] newArray(int size) {
                return new ScrollState[size];
            }
        };
        private final int mAdapterPosition;
        private final long mItemId;
        private int mVerticalOffset;

        public ScrollState(long itemId, int adapterPosition, int offset) {
            this.mItemId = itemId;
            this.mAdapterPosition = adapterPosition;
            this.mVerticalOffset = offset;
        }

        private ScrollState(Parcel in) {
            this.mItemId = in.readLong();
            this.mAdapterPosition = in.readInt();
            this.mVerticalOffset = in.readInt();
        }

        public long getItemId() {
            return this.mItemId;
        }

        public int getAdapterPosition() {
            return this.mAdapterPosition;
        }

        public int getVerticalOffset() {
            return this.mVerticalOffset;
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeLong(this.mItemId);
            dest.writeInt(this.mAdapterPosition);
            dest.writeInt(this.mVerticalOffset);
        }

        public String toString() {
            return "ScrollState {mItemId=" + this.mItemId + " mAdapterPosition=" + this.mAdapterPosition + " mVerticalOffset=" + this.mVerticalOffset + "}";
        }
    }
}
