package android.support.v17.leanback.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Rect;
import android.support.v17.leanback.R;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SimpleItemAnimator;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;

public abstract class BaseGridView extends RecyclerView {
    private boolean mAnimateChildLayout;
    RecyclerView.RecyclerListener mChainedRecyclerListener;
    private boolean mHasOverlappingRendering;
    int mInitialPrefetchItemCount;
    final GridLayoutManager mLayoutManager;
    private OnKeyInterceptListener mOnKeyInterceptListener;
    private OnMotionInterceptListener mOnMotionInterceptListener;
    private OnTouchInterceptListener mOnTouchInterceptListener;
    private OnUnhandledKeyListener mOnUnhandledKeyListener;
    private RecyclerView.ItemAnimator mSavedItemAnimator;

    public interface OnKeyInterceptListener {
        boolean onInterceptKeyEvent(KeyEvent keyEvent);
    }

    public interface OnMotionInterceptListener {
        boolean onInterceptMotionEvent(MotionEvent motionEvent);
    }

    public interface OnTouchInterceptListener {
        boolean onInterceptTouchEvent(MotionEvent motionEvent);
    }

    public interface OnUnhandledKeyListener {
        boolean onUnhandledKey(KeyEvent keyEvent);
    }

    BaseGridView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        this.mAnimateChildLayout = true;
        this.mHasOverlappingRendering = true;
        this.mInitialPrefetchItemCount = 4;
        this.mLayoutManager = new GridLayoutManager(this);
        setLayoutManager(this.mLayoutManager);
        setPreserveFocusAfterLayout(false);
        setDescendantFocusability(262144);
        setHasFixedSize(true);
        setChildrenDrawingOrderEnabled(true);
        setWillNotDraw(true);
        setOverScrollMode(2);
        ((SimpleItemAnimator) getItemAnimator()).setSupportsChangeAnimations(false);
        super.setRecyclerListener(new RecyclerView.RecyclerListener() {
            @Override
            public void onViewRecycled(RecyclerView.ViewHolder holder) {
                BaseGridView.this.mLayoutManager.onChildRecycled(holder);
                if (BaseGridView.this.mChainedRecyclerListener != null) {
                    BaseGridView.this.mChainedRecyclerListener.onViewRecycled(holder);
                }
            }
        });
    }

    void initBaseGridViewAttributes(Context context, AttributeSet attrs) {
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.lbBaseGridView);
        boolean throughFront = a.getBoolean(R.styleable.lbBaseGridView_focusOutFront, false);
        boolean throughEnd = a.getBoolean(R.styleable.lbBaseGridView_focusOutEnd, false);
        this.mLayoutManager.setFocusOutAllowed(throughFront, throughEnd);
        boolean throughSideStart = a.getBoolean(R.styleable.lbBaseGridView_focusOutSideStart, true);
        boolean throughSideEnd = a.getBoolean(R.styleable.lbBaseGridView_focusOutSideEnd, true);
        this.mLayoutManager.setFocusOutSideAllowed(throughSideStart, throughSideEnd);
        this.mLayoutManager.setVerticalSpacing(a.getDimensionPixelSize(R.styleable.lbBaseGridView_android_verticalSpacing, a.getDimensionPixelSize(R.styleable.lbBaseGridView_verticalMargin, 0)));
        this.mLayoutManager.setHorizontalSpacing(a.getDimensionPixelSize(R.styleable.lbBaseGridView_android_horizontalSpacing, a.getDimensionPixelSize(R.styleable.lbBaseGridView_horizontalMargin, 0)));
        if (a.hasValue(R.styleable.lbBaseGridView_android_gravity)) {
            setGravity(a.getInt(R.styleable.lbBaseGridView_android_gravity, 0));
        }
        a.recycle();
    }

    public void setFocusScrollStrategy(int scrollStrategy) {
        if (scrollStrategy != 0 && scrollStrategy != 1 && scrollStrategy != 2) {
            throw new IllegalArgumentException("Invalid scrollStrategy");
        }
        this.mLayoutManager.setFocusScrollStrategy(scrollStrategy);
        requestLayout();
    }

    public void setWindowAlignment(int windowAlignment) {
        this.mLayoutManager.setWindowAlignment(windowAlignment);
        requestLayout();
    }

    public void setWindowAlignmentOffsetPercent(float offsetPercent) {
        this.mLayoutManager.setWindowAlignmentOffsetPercent(offsetPercent);
        requestLayout();
    }

    public int getVerticalSpacing() {
        return this.mLayoutManager.getVerticalSpacing();
    }

    public void setOnChildViewHolderSelectedListener(OnChildViewHolderSelectedListener listener) {
        this.mLayoutManager.setOnChildViewHolderSelectedListener(listener);
    }

    public void addOnChildViewHolderSelectedListener(OnChildViewHolderSelectedListener listener) {
        this.mLayoutManager.addOnChildViewHolderSelectedListener(listener);
    }

    public void removeOnChildViewHolderSelectedListener(OnChildViewHolderSelectedListener listener) {
        this.mLayoutManager.removeOnChildViewHolderSelectedListener(listener);
    }

    public void setSelectedPosition(int position) {
        this.mLayoutManager.setSelection(position, 0);
    }

    public void setSelectedPositionSmooth(int position) {
        this.mLayoutManager.setSelectionSmooth(position);
    }

    public void setSelectedPosition(final int position, final ViewHolderTask task) {
        if (task != null) {
            RecyclerView.ViewHolder vh = findViewHolderForPosition(position);
            if (vh == null || hasPendingAdapterUpdates()) {
                addOnChildViewHolderSelectedListener(new OnChildViewHolderSelectedListener() {
                    @Override
                    public void onChildViewHolderSelectedAndPositioned(RecyclerView parent, RecyclerView.ViewHolder child, int selectedPosition, int subposition) {
                        if (selectedPosition == position) {
                            BaseGridView.this.removeOnChildViewHolderSelectedListener(this);
                            task.run(child);
                        }
                    }
                });
            } else {
                task.run(vh);
            }
        }
        setSelectedPosition(position);
    }

    public int getSelectedPosition() {
        return this.mLayoutManager.getSelection();
    }

    public void setAnimateChildLayout(boolean animateChildLayout) {
        if (this.mAnimateChildLayout != animateChildLayout) {
            this.mAnimateChildLayout = animateChildLayout;
            if (!this.mAnimateChildLayout) {
                this.mSavedItemAnimator = getItemAnimator();
                super.setItemAnimator(null);
            } else {
                super.setItemAnimator(this.mSavedItemAnimator);
            }
        }
    }

    public void setGravity(int gravity) {
        this.mLayoutManager.setGravity(gravity);
        requestLayout();
    }

    @Override
    public boolean onRequestFocusInDescendants(int direction, Rect previouslyFocusedRect) {
        return this.mLayoutManager.gridOnRequestFocusInDescendants(this, direction, previouslyFocusedRect);
    }

    @Override
    public int getChildDrawingOrder(int childCount, int i) {
        return this.mLayoutManager.getChildDrawingOrder(this, childCount, i);
    }

    final boolean isChildrenDrawingOrderEnabledInternal() {
        return isChildrenDrawingOrderEnabled();
    }

    @Override
    public View focusSearch(int direction) {
        View view;
        if (isFocused() && (view = this.mLayoutManager.findViewByPosition(this.mLayoutManager.getSelection())) != null) {
            return focusSearch(view, direction);
        }
        return super.focusSearch(direction);
    }

    @Override
    protected void onFocusChanged(boolean gainFocus, int direction, Rect previouslyFocusedRect) {
        super.onFocusChanged(gainFocus, direction, previouslyFocusedRect);
        this.mLayoutManager.onFocusChanged(gainFocus, direction, previouslyFocusedRect);
    }

    public void setPruneChild(boolean pruneChild) {
        this.mLayoutManager.setPruneChild(pruneChild);
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if ((this.mOnKeyInterceptListener == null || !this.mOnKeyInterceptListener.onInterceptKeyEvent(event)) && !super.dispatchKeyEvent(event)) {
            return this.mOnUnhandledKeyListener != null && this.mOnUnhandledKeyListener.onUnhandledKey(event);
        }
        return true;
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        if (this.mOnTouchInterceptListener != null && this.mOnTouchInterceptListener.onInterceptTouchEvent(event)) {
            return true;
        }
        return super.dispatchTouchEvent(event);
    }

    @Override
    protected boolean dispatchGenericFocusedEvent(MotionEvent event) {
        if (this.mOnMotionInterceptListener != null && this.mOnMotionInterceptListener.onInterceptMotionEvent(event)) {
            return true;
        }
        return super.dispatchGenericFocusedEvent(event);
    }

    @Override
    public boolean hasOverlappingRendering() {
        return this.mHasOverlappingRendering;
    }

    @Override
    public void onRtlPropertiesChanged(int layoutDirection) {
        this.mLayoutManager.onRtlPropertiesChanged(layoutDirection);
    }

    @Override
    public void setRecyclerListener(RecyclerView.RecyclerListener listener) {
        this.mChainedRecyclerListener = listener;
    }

    @Override
    public void scrollToPosition(int position) {
        if (this.mLayoutManager.isSlidingChildViews()) {
            this.mLayoutManager.setSelectionWithSub(position, 0, 0);
        } else {
            super.scrollToPosition(position);
        }
    }
}
