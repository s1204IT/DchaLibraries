package androidx.car.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Parcelable;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.OrientationHelper;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.util.Log;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import androidx.car.R;
import androidx.car.widget.PagedScrollBarView;

public class PagedListView extends FrameLayout {
    private RecyclerView.Adapter<? extends RecyclerView.ViewHolder> mAdapter;
    private AlphaJumpOverlayView mAlphaJumpView;
    private int mDefaultMaxPages;
    private int mGutter;
    private int mGutterSize;
    private final Handler mHandler;
    private int mLastItemCount;
    private int mMaxPages;
    private boolean mNeedsFocus;
    private OnScrollListener mOnScrollListener;
    private OrientationHelper mOrientationHelper;
    private final Runnable mPaginationRunnable;
    private RecyclerView mRecyclerView;
    private final RecyclerView.OnScrollListener mRecyclerViewOnScrollListener;
    private int mRowsPerPage;
    private boolean mScrollBarEnabled;
    PagedScrollBarView mScrollBarView;
    private PagedSnapHelper mSnapHelper;
    private final Runnable mUpdatePaginationRunnable;

    public interface DividerVisibilityManager {
        boolean shouldHideDivider(int i);
    }

    public interface ItemCap {
        void setMaxItems(int i);
    }

    public PagedListView(Context context) {
        super(context);
        this.mHandler = new Handler();
        this.mRowsPerPage = -1;
        this.mDefaultMaxPages = 6;
        this.mRecyclerViewOnScrollListener = new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                if (PagedListView.this.mOnScrollListener != null) {
                    PagedListView.this.mOnScrollListener.onScrolled(recyclerView, dx, dy);
                    if (!PagedListView.this.isAtStart() && PagedListView.this.isAtEnd()) {
                        PagedListView.this.mOnScrollListener.onReachBottom();
                    }
                }
                PagedListView.this.updatePaginationButtons(false);
            }

            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                if (PagedListView.this.mOnScrollListener != null) {
                    PagedListView.this.mOnScrollListener.onScrollStateChanged(recyclerView, newState);
                }
                if (newState == 0) {
                    PagedListView.this.mHandler.postDelayed(PagedListView.this.mPaginationRunnable, 400L);
                }
            }
        };
        this.mPaginationRunnable = new Runnable() {
            @Override
            public void run() {
                boolean upPressed = PagedListView.this.mScrollBarView.isUpPressed();
                boolean downPressed = PagedListView.this.mScrollBarView.isDownPressed();
                if (upPressed && downPressed) {
                    return;
                }
                if (upPressed) {
                    PagedListView.this.pageUp();
                } else if (downPressed) {
                    PagedListView.this.pageDown();
                }
            }
        };
        this.mUpdatePaginationRunnable = new Runnable() {
            @Override
            public final void run() {
                this.f$0.updatePaginationButtons(true);
            }
        };
        init(context, null);
    }

    public PagedListView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mHandler = new Handler();
        this.mRowsPerPage = -1;
        this.mDefaultMaxPages = 6;
        this.mRecyclerViewOnScrollListener = new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                if (PagedListView.this.mOnScrollListener != null) {
                    PagedListView.this.mOnScrollListener.onScrolled(recyclerView, dx, dy);
                    if (!PagedListView.this.isAtStart() && PagedListView.this.isAtEnd()) {
                        PagedListView.this.mOnScrollListener.onReachBottom();
                    }
                }
                PagedListView.this.updatePaginationButtons(false);
            }

            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                if (PagedListView.this.mOnScrollListener != null) {
                    PagedListView.this.mOnScrollListener.onScrollStateChanged(recyclerView, newState);
                }
                if (newState == 0) {
                    PagedListView.this.mHandler.postDelayed(PagedListView.this.mPaginationRunnable, 400L);
                }
            }
        };
        this.mPaginationRunnable = new Runnable() {
            @Override
            public void run() {
                boolean upPressed = PagedListView.this.mScrollBarView.isUpPressed();
                boolean downPressed = PagedListView.this.mScrollBarView.isDownPressed();
                if (upPressed && downPressed) {
                    return;
                }
                if (upPressed) {
                    PagedListView.this.pageUp();
                } else if (downPressed) {
                    PagedListView.this.pageDown();
                }
            }
        };
        this.mUpdatePaginationRunnable = new Runnable() {
            @Override
            public final void run() {
                this.f$0.updatePaginationButtons(true);
            }
        };
        init(context, attrs);
    }

    public PagedListView(Context context, AttributeSet attrs, int defStyleAttrs) {
        super(context, attrs, defStyleAttrs);
        this.mHandler = new Handler();
        this.mRowsPerPage = -1;
        this.mDefaultMaxPages = 6;
        this.mRecyclerViewOnScrollListener = new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                if (PagedListView.this.mOnScrollListener != null) {
                    PagedListView.this.mOnScrollListener.onScrolled(recyclerView, dx, dy);
                    if (!PagedListView.this.isAtStart() && PagedListView.this.isAtEnd()) {
                        PagedListView.this.mOnScrollListener.onReachBottom();
                    }
                }
                PagedListView.this.updatePaginationButtons(false);
            }

            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                if (PagedListView.this.mOnScrollListener != null) {
                    PagedListView.this.mOnScrollListener.onScrollStateChanged(recyclerView, newState);
                }
                if (newState == 0) {
                    PagedListView.this.mHandler.postDelayed(PagedListView.this.mPaginationRunnable, 400L);
                }
            }
        };
        this.mPaginationRunnable = new Runnable() {
            @Override
            public void run() {
                boolean upPressed = PagedListView.this.mScrollBarView.isUpPressed();
                boolean downPressed = PagedListView.this.mScrollBarView.isDownPressed();
                if (upPressed && downPressed) {
                    return;
                }
                if (upPressed) {
                    PagedListView.this.pageUp();
                } else if (downPressed) {
                    PagedListView.this.pageDown();
                }
            }
        };
        this.mUpdatePaginationRunnable = new Runnable() {
            @Override
            public final void run() {
                this.f$0.updatePaginationButtons(true);
            }
        };
        init(context, attrs);
    }

    public PagedListView(Context context, AttributeSet attrs, int defStyleAttrs, int defStyleRes) {
        super(context, attrs, defStyleAttrs, defStyleRes);
        this.mHandler = new Handler();
        this.mRowsPerPage = -1;
        this.mDefaultMaxPages = 6;
        this.mRecyclerViewOnScrollListener = new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                if (PagedListView.this.mOnScrollListener != null) {
                    PagedListView.this.mOnScrollListener.onScrolled(recyclerView, dx, dy);
                    if (!PagedListView.this.isAtStart() && PagedListView.this.isAtEnd()) {
                        PagedListView.this.mOnScrollListener.onReachBottom();
                    }
                }
                PagedListView.this.updatePaginationButtons(false);
            }

            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                if (PagedListView.this.mOnScrollListener != null) {
                    PagedListView.this.mOnScrollListener.onScrollStateChanged(recyclerView, newState);
                }
                if (newState == 0) {
                    PagedListView.this.mHandler.postDelayed(PagedListView.this.mPaginationRunnable, 400L);
                }
            }
        };
        this.mPaginationRunnable = new Runnable() {
            @Override
            public void run() {
                boolean upPressed = PagedListView.this.mScrollBarView.isUpPressed();
                boolean downPressed = PagedListView.this.mScrollBarView.isDownPressed();
                if (upPressed && downPressed) {
                    return;
                }
                if (upPressed) {
                    PagedListView.this.pageUp();
                } else if (downPressed) {
                    PagedListView.this.pageDown();
                }
            }
        };
        this.mUpdatePaginationRunnable = new Runnable() {
            @Override
            public final void run() {
                this.f$0.updatePaginationButtons(true);
            }
        };
        init(context, attrs);
    }

    private void init(Context context, AttributeSet attrs) {
        int i;
        LayoutInflater.from(context).inflate(R.layout.car_paged_recycler_view, (ViewGroup) this, true);
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.PagedListView, R.attr.pagedListViewStyle, 0);
        this.mRecyclerView = (RecyclerView) findViewById(R.id.recycler_view);
        this.mMaxPages = getDefaultMaxPages();
        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(context, 1, false);
        this.mRecyclerView.setLayoutManager(layoutManager);
        this.mSnapHelper = new PagedSnapHelper(context);
        this.mSnapHelper.attachToRecyclerView(this.mRecyclerView);
        this.mRecyclerView.addOnScrollListener(this.mRecyclerViewOnScrollListener);
        this.mRecyclerView.getRecycledViewPool().setMaxRecycledViews(0, 12);
        if (a.getBoolean(R.styleable.PagedListView_verticallyCenterListContent, false)) {
            this.mRecyclerView.getLayoutParams().height = -2;
        }
        int defaultGutterSize = getResources().getDimensionPixelSize(R.dimen.car_margin);
        this.mGutterSize = a.getDimensionPixelSize(R.styleable.PagedListView_gutterSize, defaultGutterSize);
        if (a.hasValue(R.styleable.PagedListView_gutter)) {
            int gutter = a.getInt(R.styleable.PagedListView_gutter, 3);
            setGutter(gutter);
        } else if (a.hasValue(R.styleable.PagedListView_offsetScrollBar)) {
            boolean offsetScrollBar = a.getBoolean(R.styleable.PagedListView_offsetScrollBar, false);
            if (offsetScrollBar) {
                setGutter(1);
            }
        } else {
            setGutter(3);
        }
        if (a.getBoolean(R.styleable.PagedListView_showPagedListViewDivider, true)) {
            int dividerStartMargin = a.getDimensionPixelSize(R.styleable.PagedListView_dividerStartMargin, 0);
            int dividerEndMargin = a.getDimensionPixelSize(R.styleable.PagedListView_dividerEndMargin, 0);
            int dividerStartId = a.getResourceId(R.styleable.PagedListView_alignDividerStartTo, -1);
            int dividerEndId = a.getResourceId(R.styleable.PagedListView_alignDividerEndTo, -1);
            int listDividerColor = a.getResourceId(R.styleable.PagedListView_listDividerColor, R.color.car_list_divider);
            i = -1;
            this.mRecyclerView.addItemDecoration(new DividerDecoration(context, dividerStartMargin, dividerEndMargin, dividerStartId, dividerEndId, listDividerColor));
        } else {
            i = -1;
        }
        int itemSpacing = a.getDimensionPixelSize(R.styleable.PagedListView_itemSpacing, 0);
        if (itemSpacing > 0) {
            this.mRecyclerView.addItemDecoration(new ItemSpacingDecoration(itemSpacing));
        }
        int listContentTopMargin = a.getDimensionPixelSize(R.styleable.PagedListView_listContentTopOffset, 0);
        if (listContentTopMargin > 0) {
            this.mRecyclerView.addItemDecoration(new TopOffsetDecoration(listContentTopMargin));
        }
        setFocusable(false);
        this.mScrollBarEnabled = a.getBoolean(R.styleable.PagedListView_scrollBarEnabled, true);
        this.mScrollBarView = (PagedScrollBarView) findViewById(R.id.paged_scroll_view);
        this.mScrollBarView.setPaginationListener(new PagedScrollBarView.PaginationListener() {
            @Override
            public void onPaginate(int direction) {
                switch (direction) {
                    case 0:
                        PagedListView.this.pageUp();
                        if (PagedListView.this.mOnScrollListener != null) {
                            PagedListView.this.mOnScrollListener.onScrollUpButtonClicked();
                        }
                        break;
                    case 1:
                        PagedListView.this.pageDown();
                        if (PagedListView.this.mOnScrollListener != null) {
                            PagedListView.this.mOnScrollListener.onScrollDownButtonClicked();
                        }
                        break;
                    default:
                        Log.e("PagedListView", "Unknown pagination direction (" + direction + ")");
                        break;
                }
            }

            @Override
            public void onAlphaJump() {
                PagedListView.this.showAlphaJump();
            }
        });
        Drawable upButtonIcon = a.getDrawable(R.styleable.PagedListView_upButtonIcon);
        if (upButtonIcon != null) {
            setUpButtonIcon(upButtonIcon);
        }
        Drawable downButtonIcon = a.getDrawable(R.styleable.PagedListView_downButtonIcon);
        if (downButtonIcon != null) {
            setDownButtonIcon(downButtonIcon);
        }
        int scrollBarColor = a.getResourceId(R.styleable.PagedListView_scrollBarColor, i);
        if (scrollBarColor != i) {
            setScrollbarColor(scrollBarColor);
        }
        this.mScrollBarView.setVisibility(this.mScrollBarEnabled ? 0 : 8);
        if (this.mScrollBarEnabled) {
            int topMargin = a.getDimensionPixelSize(R.styleable.PagedListView_scrollBarTopMargin, ((ViewGroup.MarginLayoutParams) this.mScrollBarView.getLayoutParams()).topMargin);
            setScrollBarTopMargin(topMargin);
        } else {
            ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) this.mRecyclerView.getLayoutParams();
            params.setMarginStart(0);
        }
        if (a.hasValue(R.styleable.PagedListView_scrollBarContainerWidth)) {
            int carMargin = getResources().getDimensionPixelSize(R.dimen.car_margin);
            int scrollBarContainerWidth = a.getDimensionPixelSize(R.styleable.PagedListView_scrollBarContainerWidth, carMargin);
            setScrollBarContainerWidth(scrollBarContainerWidth);
        }
        int carMargin2 = R.styleable.PagedListView_dayNightStyle;
        if (a.hasValue(carMargin2)) {
            int dayNightStyle = a.getInt(R.styleable.PagedListView_dayNightStyle, 0);
            setDayNightStyle(dayNightStyle);
        } else {
            setDayNightStyle(0);
        }
        a.recycle();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        this.mHandler.removeCallbacks(this.mUpdatePaginationRunnable);
    }

    public void setGutter(int gutter) {
        this.mGutter = gutter;
        int startMargin = 0;
        int endMargin = 0;
        if ((this.mGutter & 1) != 0) {
            startMargin = this.mGutterSize;
        }
        if ((this.mGutter & 2) != 0) {
            endMargin = this.mGutterSize;
        }
        ViewGroup.MarginLayoutParams layoutParams = (ViewGroup.MarginLayoutParams) this.mRecyclerView.getLayoutParams();
        layoutParams.setMarginStart(startMargin);
        layoutParams.setMarginEnd(endMargin);
        this.mRecyclerView.setLayoutParams(layoutParams);
        this.mRecyclerView.setClipToPadding(startMargin == 0 && endMargin == 0);
    }

    public void setScrollBarContainerWidth(int width) {
        ViewGroup.LayoutParams layoutParams = this.mScrollBarView.getLayoutParams();
        layoutParams.width = width;
        this.mScrollBarView.requestLayout();
    }

    public void setScrollBarTopMargin(int topMargin) {
        ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) this.mScrollBarView.getLayoutParams();
        params.topMargin = topMargin;
        this.mScrollBarView.requestLayout();
    }

    public RecyclerView getRecyclerView() {
        return this.mRecyclerView;
    }

    public void scrollToPosition(int position) {
        RecyclerView.LayoutManager layoutManager = this.mRecyclerView.getLayoutManager();
        if (layoutManager == null) {
            return;
        }
        RecyclerView.SmoothScroller smoothScroller = this.mSnapHelper.createScroller(layoutManager);
        smoothScroller.setTargetPosition(position);
        layoutManager.startSmoothScroll(smoothScroller);
        this.mHandler.post(this.mUpdatePaginationRunnable);
    }

    public void snapToPosition(final int position) {
        RecyclerView.LayoutManager layoutManager = this.mRecyclerView.getLayoutManager();
        if (layoutManager == 0) {
            return;
        }
        int startPosition = position;
        if (layoutManager instanceof RecyclerView.SmoothScroller.ScrollVectorProvider) {
            PointF vector = ((RecyclerView.SmoothScroller.ScrollVectorProvider) layoutManager).computeScrollVectorForPosition(position);
            int offsetDirection = (vector == null || vector.y > 0.0f) ? -1 : 1;
            startPosition = Math.max(0, Math.min(startPosition + (offsetDirection * 2), layoutManager.getItemCount() - 1));
        } else {
            Log.w("PagedListView", "LayoutManager is not a ScrollVectorProvider, can't do snap animation.");
        }
        if (layoutManager instanceof LinearLayoutManager) {
            ((LinearLayoutManager) layoutManager).scrollToPositionWithOffset(startPosition, 0);
        } else {
            layoutManager.scrollToPosition(startPosition);
        }
        if (startPosition != position) {
            post(new Runnable() {
                @Override
                public final void run() {
                    this.f$0.scrollToPosition(position);
                }
            });
        }
    }

    public void setUpButtonIcon(Drawable icon) {
        this.mScrollBarView.setUpButtonIcon(icon);
    }

    public void setDownButtonIcon(Drawable icon) {
        this.mScrollBarView.setDownButtonIcon(icon);
    }

    public void setAdapter(RecyclerView.Adapter<? extends RecyclerView.ViewHolder> adapter) {
        this.mAdapter = adapter;
        this.mRecyclerView.setAdapter(adapter);
        updateMaxItems();
        updateAlphaJump();
    }

    public void setMaxPages(int maxPages) {
        this.mMaxPages = Math.max(-1, maxPages);
        updateMaxItems();
    }

    public void setScrollbarColor(int color) {
        this.mScrollBarView.setThumbColor(color);
    }

    public void setDayNightStyle(int dayNightStyle) {
        this.mScrollBarView.setDayNightStyle(dayNightStyle);
        int decorCount = this.mRecyclerView.getItemDecorationCount();
        for (int i = 0; i < decorCount; i++) {
            RecyclerView.ItemDecoration decor = this.mRecyclerView.getItemDecorationAt(i);
            if (decor instanceof DividerDecoration) {
                ((DividerDecoration) decor).updateDividerColor();
            }
        }
    }

    private OrientationHelper getOrientationHelper(RecyclerView.LayoutManager layoutManager) {
        if (this.mOrientationHelper == null || this.mOrientationHelper.getLayoutManager() != layoutManager) {
            this.mOrientationHelper = OrientationHelper.createVerticalHelper(layoutManager);
        }
        return this.mOrientationHelper;
    }

    public void pageUp() {
        if (this.mRecyclerView.getLayoutManager() == null || this.mRecyclerView.getChildCount() == 0) {
            return;
        }
        OrientationHelper orientationHelper = getOrientationHelper(this.mRecyclerView.getLayoutManager());
        int screenSize = this.mRecyclerView.getHeight();
        int scrollDistance = screenSize;
        int i = 0;
        while (true) {
            if (i >= this.mRecyclerView.getChildCount()) {
                break;
            }
            View child = this.mRecyclerView.getChildAt(i);
            if (child.getHeight() <= screenSize) {
                i++;
            } else if (orientationHelper.getDecoratedEnd(child) < screenSize) {
                scrollDistance = screenSize - orientationHelper.getDecoratedEnd(child);
            } else if ((-screenSize) < orientationHelper.getDecoratedStart(child) && orientationHelper.getDecoratedStart(child) < 0) {
                scrollDistance = Math.abs(orientationHelper.getDecoratedStart(child));
            }
        }
        this.mRecyclerView.smoothScrollBy(0, -scrollDistance);
    }

    public void pageDown() {
        if (this.mRecyclerView.getLayoutManager() == null || this.mRecyclerView.getChildCount() == 0) {
            return;
        }
        OrientationHelper orientationHelper = getOrientationHelper(this.mRecyclerView.getLayoutManager());
        int screenSize = this.mRecyclerView.getHeight();
        int scrollDistance = screenSize;
        View lastChild = this.mRecyclerView.getChildAt(this.mRecyclerView.getChildCount() - 1);
        if (this.mRecyclerView.getLayoutManager().isViewPartiallyVisible(lastChild, false, false) && (scrollDistance = orientationHelper.getDecoratedStart(lastChild)) < 0) {
            scrollDistance = screenSize;
        }
        int i = this.mRecyclerView.getChildCount() - 1;
        while (true) {
            if (i < 0) {
                break;
            }
            View child = this.mRecyclerView.getChildAt(i);
            if (child.getHeight() <= screenSize) {
                i--;
            } else if (orientationHelper.getDecoratedStart(child) > 0) {
                scrollDistance = orientationHelper.getDecoratedStart(child);
            } else if (screenSize < orientationHelper.getDecoratedEnd(child) && orientationHelper.getDecoratedEnd(child) < 2 * screenSize) {
                scrollDistance = orientationHelper.getDecoratedEnd(child) - screenSize;
            }
        }
        this.mRecyclerView.smoothScrollBy(0, scrollDistance);
    }

    private int getDefaultMaxPages() {
        return this.mDefaultMaxPages - 1;
    }

    @Override
    public void onLayout(boolean changed, int left, int top, int right, int bottom) {
        RecyclerView.LayoutManager layoutManager = this.mRecyclerView.getLayoutManager();
        if (layoutManager == null) {
            return;
        }
        View focusedChild = layoutManager.getFocusedChild();
        View firstBorn = layoutManager.getChildAt(0);
        super.onLayout(changed, left, top, right, bottom);
        if (this.mAdapter != null) {
            int itemCount = this.mAdapter.getItemCount();
            if (Log.isLoggable("PagedListView", 3)) {
                Log.d("PagedListView", String.format("onLayout hasFocus: %s, mLastItemCount: %s, itemCount: %s, focusedChild: %s, firstBorn: %s, isInTouchMode: %s, mNeedsFocus: %s", Boolean.valueOf(hasFocus()), Integer.valueOf(this.mLastItemCount), Integer.valueOf(itemCount), focusedChild, firstBorn, Boolean.valueOf(isInTouchMode()), Boolean.valueOf(this.mNeedsFocus)));
            }
            updateMaxItems();
            if (this.mNeedsFocus && itemCount > 0) {
                if (focusedChild == null) {
                    requestFocus();
                }
                this.mNeedsFocus = false;
            }
            if (itemCount > this.mLastItemCount && focusedChild == firstBorn) {
                requestFocus();
            }
            this.mLastItemCount = itemCount;
        }
        if (!this.mScrollBarEnabled) {
            return;
        }
        boolean isAtStart = isAtStart();
        boolean isAtEnd = isAtEnd();
        if ((!isAtStart || !isAtEnd) && layoutManager.getItemCount() != 0) {
            this.mScrollBarView.setVisibility(0);
            this.mScrollBarView.setUpEnabled(!isAtStart);
            this.mScrollBarView.setDownEnabled(!isAtEnd);
            if (this.mRecyclerView.getLayoutManager().canScrollVertically()) {
                this.mScrollBarView.setParametersInLayout(this.mRecyclerView.computeVerticalScrollRange(), this.mRecyclerView.computeVerticalScrollOffset(), this.mRecyclerView.computeVerticalScrollExtent());
                return;
            } else {
                this.mScrollBarView.setParametersInLayout(this.mRecyclerView.computeHorizontalScrollRange(), this.mRecyclerView.computeHorizontalScrollOffset(), this.mRecyclerView.computeHorizontalScrollExtent());
                return;
            }
        }
        this.mScrollBarView.setVisibility(4);
    }

    private void updatePaginationButtons(boolean animate) {
        if (!this.mScrollBarEnabled) {
            return;
        }
        boolean isAtStart = isAtStart();
        boolean isAtEnd = isAtEnd();
        RecyclerView.LayoutManager layoutManager = this.mRecyclerView.getLayoutManager();
        if ((isAtStart && isAtEnd) || layoutManager == null || layoutManager.getItemCount() == 0) {
            this.mScrollBarView.setVisibility(4);
        } else {
            this.mScrollBarView.setVisibility(0);
        }
        this.mScrollBarView.setUpEnabled(!isAtStart);
        this.mScrollBarView.setDownEnabled(!isAtEnd);
        if (layoutManager == null) {
            return;
        }
        if (this.mRecyclerView.getLayoutManager().canScrollVertically()) {
            this.mScrollBarView.setParameters(this.mRecyclerView.computeVerticalScrollRange(), this.mRecyclerView.computeVerticalScrollOffset(), this.mRecyclerView.computeVerticalScrollExtent(), animate);
        } else {
            this.mScrollBarView.setParameters(this.mRecyclerView.computeHorizontalScrollRange(), this.mRecyclerView.computeHorizontalScrollOffset(), this.mRecyclerView.computeHorizontalScrollExtent(), animate);
        }
        invalidate();
    }

    public boolean isAtStart() {
        return this.mSnapHelper.isAtStart(this.mRecyclerView.getLayoutManager());
    }

    public boolean isAtEnd() {
        return this.mSnapHelper.isAtEnd(this.mRecyclerView.getLayoutManager());
    }

    private void updateMaxItems() {
        if (this.mAdapter == null) {
            return;
        }
        updateRowsPerPage();
        if (!(this.mAdapter instanceof ItemCap)) {
            return;
        }
        int originalCount = this.mAdapter.getItemCount();
        ((ItemCap) this.mAdapter).setMaxItems(calculateMaxItemCount());
        int newCount = this.mAdapter.getItemCount();
        if (newCount == originalCount) {
            return;
        }
        if (newCount < originalCount) {
            this.mAdapter.notifyItemRangeRemoved(newCount, originalCount - newCount);
        } else {
            this.mAdapter.notifyItemRangeInserted(originalCount, newCount - originalCount);
        }
    }

    private int calculateMaxItemCount() {
        View firstChild;
        RecyclerView.LayoutManager layoutManager = this.mRecyclerView.getLayoutManager();
        if (layoutManager == null || (firstChild = layoutManager.getChildAt(0)) == null || firstChild.getHeight() == 0 || this.mMaxPages < 0) {
            return -1;
        }
        return this.mRowsPerPage * this.mMaxPages;
    }

    private void updateRowsPerPage() {
        RecyclerView.LayoutManager layoutManager = this.mRecyclerView.getLayoutManager();
        if (layoutManager == null) {
            this.mRowsPerPage = 1;
            return;
        }
        View firstChild = layoutManager.getChildAt(0);
        if (firstChild != null && firstChild.getHeight() != 0) {
            this.mRowsPerPage = Math.max(1, (getHeight() - getPaddingTop()) / firstChild.getHeight());
        } else {
            this.mRowsPerPage = 1;
        }
    }

    @Override
    public Parcelable onSaveInstanceState() {
        Bundle bundle = new Bundle();
        bundle.putParcelable("PagedListViewSuperState", super.onSaveInstanceState());
        SparseArray<Parcelable> recyclerViewState = new SparseArray<>();
        this.mRecyclerView.saveHierarchyState(recyclerViewState);
        bundle.putSparseParcelableArray("RecyclerViewState", recyclerViewState);
        return bundle;
    }

    @Override
    public void onRestoreInstanceState(Parcelable state) {
        if (!(state instanceof Bundle)) {
            super.onRestoreInstanceState(state);
            return;
        }
        Bundle bundle = (Bundle) state;
        this.mRecyclerView.restoreHierarchyState(bundle.getSparseParcelableArray("RecyclerViewState"));
        super.onRestoreInstanceState(bundle.getParcelable("PagedListViewSuperState"));
    }

    @Override
    protected void dispatchSaveInstanceState(SparseArray<Parcelable> container) {
        dispatchFreezeSelfOnly(container);
    }

    @Override
    protected void dispatchRestoreInstanceState(SparseArray<Parcelable> container) {
        dispatchThawSelfOnly(container);
    }

    private void updateAlphaJump() {
        boolean supportsAlphaJump = this.mAdapter instanceof IAlphaJumpAdapter;
        this.mScrollBarView.setShowAlphaJump(supportsAlphaJump);
    }

    private void showAlphaJump() {
        if (this.mAlphaJumpView == null && (this.mAdapter instanceof IAlphaJumpAdapter)) {
            this.mAlphaJumpView = new AlphaJumpOverlayView(getContext());
            this.mAlphaJumpView.init(this, (IAlphaJumpAdapter) this.mAdapter);
            addView(this.mAlphaJumpView);
        }
        this.mAlphaJumpView.setVisibility(0);
    }

    public static abstract class OnScrollListener {
        public void onReachBottom() {
        }

        public void onScrollUpButtonClicked() {
        }

        public void onScrollDownButtonClicked() {
        }

        public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
        }

        public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
        }
    }

    private static class ItemSpacingDecoration extends RecyclerView.ItemDecoration {
        private int mItemSpacing;

        private ItemSpacingDecoration(int itemSpacing) {
            this.mItemSpacing = itemSpacing;
        }

        @Override
        public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {
            super.getItemOffsets(outRect, view, parent, state);
            int position = parent.getChildAdapterPosition(view);
            if (position == state.getItemCount() - 1 && !(parent.getLayoutManager() instanceof GridLayoutManager)) {
                return;
            }
            outRect.bottom = this.mItemSpacing;
        }
    }

    private static class DividerDecoration extends RecyclerView.ItemDecoration {
        private final Context mContext;
        private final int mDividerEndId;
        private final int mDividerEndMargin;
        private final int mDividerHeight;
        private final int mDividerStartId;
        private final int mDividerStartMargin;
        private final int mListDividerColor;
        private final Paint mPaint;
        private DividerVisibilityManager mVisibilityManager;

        private DividerDecoration(Context context, int dividerStartMargin, int dividerEndMargin, int dividerStartId, int dividerEndId, int listDividerColor) {
            this.mContext = context;
            this.mDividerStartMargin = dividerStartMargin;
            this.mDividerEndMargin = dividerEndMargin;
            this.mDividerStartId = dividerStartId;
            this.mDividerEndId = dividerEndId;
            this.mListDividerColor = listDividerColor;
            this.mPaint = new Paint();
            this.mPaint.setColor(this.mContext.getColor(listDividerColor));
            this.mDividerHeight = this.mContext.getResources().getDimensionPixelSize(R.dimen.car_list_divider_height);
        }

        public void updateDividerColor() {
            this.mPaint.setColor(this.mContext.getColor(this.mListDividerColor));
        }

        @Override
        public void onDrawOver(Canvas c, RecyclerView parent, RecyclerView.State state) {
            View nextVerticalContainer;
            boolean usesGridLayoutManager = parent.getLayoutManager() instanceof GridLayoutManager;
            for (int i = 0; i < parent.getChildCount(); i++) {
                View container = parent.getChildAt(i);
                int itemPosition = parent.getChildAdapterPosition(container);
                if (!hideDividerForAdapterPosition(itemPosition)) {
                    if (usesGridLayoutManager) {
                        int lastItem = GridLayoutManagerUtils.getLastIndexOnSameRow(i, parent);
                        nextVerticalContainer = parent.getChildAt(lastItem + 1);
                    } else {
                        nextVerticalContainer = parent.getChildAt(i + 1);
                    }
                    if (nextVerticalContainer != null) {
                        int spacing = nextVerticalContainer.getTop() - container.getBottom();
                        drawDivider(c, container, spacing);
                    }
                }
            }
        }

        private void drawDivider(Canvas c, View container, int spacing) {
            View startChild = this.mDividerStartId != -1 ? container.findViewById(this.mDividerStartId) : container;
            View endChild = this.mDividerEndId != -1 ? container.findViewById(this.mDividerEndId) : container;
            if (startChild == null || endChild == null) {
                return;
            }
            Rect containerRect = new Rect();
            container.getGlobalVisibleRect(containerRect);
            Rect startRect = new Rect();
            startChild.getGlobalVisibleRect(startRect);
            Rect endRect = new Rect();
            endChild.getGlobalVisibleRect(endRect);
            int left = container.getLeft() + this.mDividerStartMargin + (startRect.left - containerRect.left);
            int right = (container.getRight() - this.mDividerEndMargin) - (endRect.right - containerRect.right);
            int bottom = container.getBottom() + ((spacing + this.mDividerHeight) / 2);
            int top = bottom - this.mDividerHeight;
            c.drawRect(left, top, right, bottom, this.mPaint);
        }

        @Override
        public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {
            super.getItemOffsets(outRect, view, parent, state);
            int pos = parent.getChildAdapterPosition(view);
            if (hideDividerForAdapterPosition(pos)) {
                return;
            }
            outRect.bottom = this.mDividerHeight;
        }

        private boolean hideDividerForAdapterPosition(int position) {
            return this.mVisibilityManager != null && this.mVisibilityManager.shouldHideDivider(position);
        }
    }

    private static class TopOffsetDecoration extends RecyclerView.ItemDecoration {
        private int mTopOffset;

        private TopOffsetDecoration(int topOffset) {
            this.mTopOffset = topOffset;
        }

        @Override
        public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {
            super.getItemOffsets(outRect, view, parent, state);
            int position = parent.getChildAdapterPosition(view);
            if ((parent.getLayoutManager() instanceof GridLayoutManager) && position < GridLayoutManagerUtils.getFirstRowItemCount(parent)) {
                outRect.top = this.mTopOffset;
            } else if (position == 0) {
                outRect.top = this.mTopOffset;
            }
        }
    }
}
