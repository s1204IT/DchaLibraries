package com.android.systemui.recent;

import android.animation.LayoutTransition;
import android.content.Context;
import android.content.res.Configuration;
import android.database.DataSetObserver;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.FloatMath;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewTreeObserver;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import com.android.systemui.R;
import com.android.systemui.SwipeHelper;
import com.android.systemui.recent.RecentsPanelView;
import java.util.HashSet;
import java.util.Iterator;

public class RecentsHorizontalScrollView extends HorizontalScrollView implements SwipeHelper.Callback, RecentsPanelView.RecentsScrollView {
    private static final boolean DEBUG = RecentsPanelView.DEBUG;
    private RecentsPanelView.TaskDescriptionAdapter mAdapter;
    private RecentsCallback mCallback;
    private FadedEdgeDrawHelper mFadedEdgeDrawHelper;
    protected int mLastScrollPosition;
    private LinearLayout mLinearLayout;
    private int mNumItemsInOneScreenful;
    private Runnable mOnScrollListener;
    private HashSet<View> mRecycledViews;
    private SwipeHelper mSwipeHelper;

    public RecentsHorizontalScrollView(Context context, AttributeSet attrs) {
        super(context, attrs, 0);
        this.mSwipeHelper = new SwipeHelper(1, this, context);
        this.mFadedEdgeDrawHelper = FadedEdgeDrawHelper.create(context, attrs, this, false);
        this.mRecycledViews = new HashSet<>();
    }

    @Override
    public void setMinSwipeAlpha(float minAlpha) {
        this.mSwipeHelper.setMinSwipeProgress(minAlpha);
    }

    public int scrollPositionOfMostRecent() {
        return this.mLinearLayout.getWidth() - getWidth();
    }

    private void addToRecycledViews(View v) {
        if (this.mRecycledViews.size() < this.mNumItemsInOneScreenful) {
            this.mRecycledViews.add(v);
        }
    }

    @Override
    public View findViewForTask(int persistentTaskId) {
        for (int i = 0; i < this.mLinearLayout.getChildCount(); i++) {
            View v = this.mLinearLayout.getChildAt(i);
            RecentsPanelView.ViewHolder holder = (RecentsPanelView.ViewHolder) v.getTag();
            if (holder.taskDescription.persistentTaskId == persistentTaskId) {
                return v;
            }
        }
        return null;
    }

    public void update() {
        for (int i = 0; i < this.mLinearLayout.getChildCount(); i++) {
            View v = this.mLinearLayout.getChildAt(i);
            addToRecycledViews(v);
            this.mAdapter.recycleView(v);
        }
        LayoutTransition transitioner = getLayoutTransition();
        setLayoutTransition(null);
        this.mLinearLayout.removeAllViews();
        Iterator<View> recycledViews = this.mRecycledViews.iterator();
        for (int i2 = 0; i2 < this.mAdapter.getCount(); i2++) {
            View old = null;
            if (recycledViews.hasNext()) {
                View old2 = recycledViews.next();
                old = old2;
                recycledViews.remove();
                old.setVisibility(0);
            }
            final View view = this.mAdapter.getView(i2, old, this.mLinearLayout);
            if (this.mFadedEdgeDrawHelper != null) {
                this.mFadedEdgeDrawHelper.addViewCallback(view);
            }
            View.OnTouchListener noOpListener = new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v2, MotionEvent event) {
                    return true;
                }
            };
            view.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v2) {
                    RecentsHorizontalScrollView.this.mCallback.dismiss();
                }
            });
            view.setSoundEffectsEnabled(false);
            View.OnClickListener launchAppListener = new View.OnClickListener() {
                @Override
                public void onClick(View v2) {
                    RecentsHorizontalScrollView.this.mCallback.handleOnClick(view);
                }
            };
            RecentsPanelView.ViewHolder holder = (RecentsPanelView.ViewHolder) view.getTag();
            final View thumbnailView = holder.thumbnailView;
            View.OnLongClickListener longClickListener = new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v2) {
                    View anchorView = view.findViewById(R.id.app_description);
                    RecentsHorizontalScrollView.this.mCallback.handleLongPress(view, anchorView, thumbnailView);
                    return true;
                }
            };
            thumbnailView.setClickable(true);
            thumbnailView.setOnClickListener(launchAppListener);
            thumbnailView.setOnLongClickListener(longClickListener);
            View appTitle = view.findViewById(R.id.app_label);
            appTitle.setContentDescription(" ");
            appTitle.setOnTouchListener(noOpListener);
            this.mLinearLayout.addView(view);
        }
        setLayoutTransition(transitioner);
        ViewTreeObserver.OnGlobalLayoutListener updateScroll = new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                RecentsHorizontalScrollView.this.mLastScrollPosition = RecentsHorizontalScrollView.this.scrollPositionOfMostRecent();
                RecentsHorizontalScrollView.this.scrollTo(RecentsHorizontalScrollView.this.mLastScrollPosition, 0);
                ViewTreeObserver observer = RecentsHorizontalScrollView.this.getViewTreeObserver();
                if (observer.isAlive()) {
                    observer.removeOnGlobalLayoutListener(this);
                }
            }
        };
        getViewTreeObserver().addOnGlobalLayoutListener(updateScroll);
    }

    @Override
    public void removeViewInLayout(View view) {
        dismissChild(view);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        if (DEBUG) {
            Log.v("RecentsPanelView", "onInterceptTouchEvent()");
        }
        return this.mSwipeHelper.onInterceptTouchEvent(ev) || super.onInterceptTouchEvent(ev);
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        return this.mSwipeHelper.onTouchEvent(ev) || super.onTouchEvent(ev);
    }

    @Override
    public boolean canChildBeDismissed(View v) {
        return true;
    }

    @Override
    public boolean isAntiFalsingNeeded() {
        return false;
    }

    @Override
    public float getFalsingThresholdFactor() {
        return 1.0f;
    }

    public void dismissChild(View v) {
        this.mSwipeHelper.dismissChild(v, 0.0f);
    }

    @Override
    public void onChildDismissed(View v) {
        addToRecycledViews(v);
        this.mLinearLayout.removeView(v);
        this.mCallback.handleSwipe(v);
        View contentView = getChildContentView(v);
        contentView.setAlpha(1.0f);
        contentView.setTranslationY(0.0f);
    }

    @Override
    public void onBeginDrag(View v) {
        requestDisallowInterceptTouchEvent(true);
    }

    @Override
    public void onDragCancelled(View v) {
    }

    @Override
    public void onChildSnappedBack(View animView) {
    }

    @Override
    public boolean updateSwipeProgress(View animView, boolean dismissable, float swipeProgress) {
        return false;
    }

    @Override
    public View getChildAtPosition(MotionEvent ev) {
        float x = ev.getX() + getScrollX();
        float y = ev.getY() + getScrollY();
        for (int i = 0; i < this.mLinearLayout.getChildCount(); i++) {
            View item = this.mLinearLayout.getChildAt(i);
            if (x >= item.getLeft() && x < item.getRight() && y >= item.getTop() && y < item.getBottom()) {
                return item;
            }
        }
        return null;
    }

    @Override
    public View getChildContentView(View v) {
        return v.findViewById(R.id.recent_item);
    }

    @Override
    public void drawFadedEdges(Canvas canvas, int left, int right, int top, int bottom) {
        if (this.mFadedEdgeDrawHelper != null) {
            this.mFadedEdgeDrawHelper.drawCallback(canvas, left, right, top, bottom, getScrollX(), getScrollY(), 0.0f, 0.0f, getLeftFadingEdgeStrength(), getRightFadingEdgeStrength(), getPaddingTop());
        }
    }

    @Override
    protected void onScrollChanged(int l, int t, int oldl, int oldt) {
        super.onScrollChanged(l, t, oldl, oldt);
        if (this.mOnScrollListener != null) {
            this.mOnScrollListener.run();
        }
    }

    @Override
    public void setOnScrollListener(Runnable listener) {
        this.mOnScrollListener = listener;
    }

    @Override
    public int getVerticalFadingEdgeLength() {
        return this.mFadedEdgeDrawHelper != null ? this.mFadedEdgeDrawHelper.getVerticalFadingEdgeLength() : super.getVerticalFadingEdgeLength();
    }

    @Override
    public int getHorizontalFadingEdgeLength() {
        return this.mFadedEdgeDrawHelper != null ? this.mFadedEdgeDrawHelper.getHorizontalFadingEdgeLength() : super.getHorizontalFadingEdgeLength();
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        setScrollbarFadingEnabled(true);
        this.mLinearLayout = (LinearLayout) findViewById(R.id.recents_linear_layout);
        int leftPadding = getContext().getResources().getDimensionPixelOffset(R.dimen.status_bar_recents_thumbnail_left_margin);
        setOverScrollEffectPadding(leftPadding, 0);
    }

    @Override
    public void onAttachedToWindow() {
        if (this.mFadedEdgeDrawHelper != null) {
            this.mFadedEdgeDrawHelper.onAttachedToWindowCallback(this.mLinearLayout, isHardwareAccelerated());
        }
    }

    @Override
    protected void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        float densityScale = getResources().getDisplayMetrics().density;
        this.mSwipeHelper.setDensityScale(densityScale);
        float pagingTouchSlop = ViewConfiguration.get(getContext()).getScaledPagingTouchSlop();
        this.mSwipeHelper.setPagingTouchSlop(pagingTouchSlop);
    }

    private void setOverScrollEffectPadding(int leftPadding, int i) {
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        LayoutTransition transition = this.mLinearLayout.getLayoutTransition();
        if (transition == null || !transition.isRunning()) {
            this.mLastScrollPosition = scrollPositionOfMostRecent();
            post(new Runnable() {
                @Override
                public void run() {
                    LayoutTransition transition2 = RecentsHorizontalScrollView.this.mLinearLayout.getLayoutTransition();
                    if (transition2 == null || !transition2.isRunning()) {
                        RecentsHorizontalScrollView.this.scrollTo(RecentsHorizontalScrollView.this.mLastScrollPosition, 0);
                    }
                }
            });
        }
    }

    @Override
    public void setAdapter(RecentsPanelView.TaskDescriptionAdapter adapter) {
        this.mAdapter = adapter;
        this.mAdapter.registerDataSetObserver(new DataSetObserver() {
            @Override
            public void onChanged() {
                RecentsHorizontalScrollView.this.update();
            }

            @Override
            public void onInvalidated() {
                RecentsHorizontalScrollView.this.update();
            }
        });
        DisplayMetrics dm = getResources().getDisplayMetrics();
        int childWidthMeasureSpec = View.MeasureSpec.makeMeasureSpec(dm.widthPixels, Integer.MIN_VALUE);
        int childheightMeasureSpec = View.MeasureSpec.makeMeasureSpec(dm.heightPixels, Integer.MIN_VALUE);
        View child = this.mAdapter.createView(this.mLinearLayout);
        child.measure(childWidthMeasureSpec, childheightMeasureSpec);
        this.mNumItemsInOneScreenful = (int) FloatMath.ceil(dm.widthPixels / child.getMeasuredWidth());
        addToRecycledViews(child);
        for (int i = 0; i < this.mNumItemsInOneScreenful - 1; i++) {
            addToRecycledViews(this.mAdapter.createView(this.mLinearLayout));
        }
    }

    @Override
    public int numItemsInOneScreenful() {
        return this.mNumItemsInOneScreenful;
    }

    @Override
    public void setLayoutTransition(LayoutTransition transition) {
        this.mLinearLayout.setLayoutTransition(transition);
    }

    @Override
    public void setCallback(RecentsCallback callback) {
        this.mCallback = callback;
    }
}
