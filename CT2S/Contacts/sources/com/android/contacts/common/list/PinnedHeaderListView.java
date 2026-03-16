package com.android.contacts.common.list;

import android.R;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ListAdapter;
import com.android.contacts.common.util.ViewUtil;

public class PinnedHeaderListView extends AutoScrollListView implements AbsListView.OnScrollListener, AdapterView.OnItemSelectedListener {
    private PinnedHeaderAdapter mAdapter;
    private boolean mAnimating;
    private int mAnimationDuration;
    private long mAnimationTargetTime;
    private RectF mBounds;
    private int mHeaderPaddingStart;
    private boolean mHeaderTouched;
    private int mHeaderWidth;
    private PinnedHeader[] mHeaders;
    private AdapterView.OnItemSelectedListener mOnItemSelectedListener;
    private AbsListView.OnScrollListener mOnScrollListener;
    private int mScrollState;
    private boolean mScrollToSectionOnHeaderTouch;
    private int mSize;

    public interface PinnedHeaderAdapter {
        void configurePinnedHeaders(PinnedHeaderListView pinnedHeaderListView);

        int getPinnedHeaderCount();

        View getPinnedHeaderView(int i, View view, ViewGroup viewGroup);

        int getScrollPositionForHeader(int i);
    }

    private static final class PinnedHeader {
        int alpha;
        boolean animating;
        int height;
        int sourceY;
        int state;
        long targetTime;
        boolean targetVisible;
        int targetY;
        View view;
        boolean visible;
        int y;

        private PinnedHeader() {
        }
    }

    public PinnedHeaderListView(Context context) {
        this(context, null, R.attr.listViewStyle);
    }

    public PinnedHeaderListView(Context context, AttributeSet attrs) {
        this(context, attrs, R.attr.listViewStyle);
    }

    public PinnedHeaderListView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        this.mBounds = new RectF();
        this.mScrollToSectionOnHeaderTouch = false;
        this.mHeaderTouched = false;
        this.mAnimationDuration = 20;
        super.setOnScrollListener(this);
        super.setOnItemSelectedListener(this);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);
        this.mHeaderPaddingStart = getPaddingStart();
        this.mHeaderWidth = ((r - l) - this.mHeaderPaddingStart) - getPaddingEnd();
    }

    @Override
    public void setAdapter(ListAdapter adapter) {
        this.mAdapter = (PinnedHeaderAdapter) adapter;
        super.setAdapter(adapter);
    }

    @Override
    public void setOnScrollListener(AbsListView.OnScrollListener onScrollListener) {
        this.mOnScrollListener = onScrollListener;
        super.setOnScrollListener(this);
    }

    @Override
    public void setOnItemSelectedListener(AdapterView.OnItemSelectedListener listener) {
        this.mOnItemSelectedListener = listener;
        super.setOnItemSelectedListener(this);
    }

    @Override
    public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
        if (this.mAdapter != null) {
            int count = this.mAdapter.getPinnedHeaderCount();
            if (count != this.mSize) {
                this.mSize = count;
                if (this.mHeaders == null) {
                    this.mHeaders = new PinnedHeader[this.mSize];
                } else if (this.mHeaders.length < this.mSize) {
                    PinnedHeader[] headers = this.mHeaders;
                    this.mHeaders = new PinnedHeader[this.mSize];
                    System.arraycopy(headers, 0, this.mHeaders, 0, headers.length);
                }
            }
            for (int i = 0; i < this.mSize; i++) {
                if (this.mHeaders[i] == null) {
                    this.mHeaders[i] = new PinnedHeader();
                }
                this.mHeaders[i].view = this.mAdapter.getPinnedHeaderView(i, this.mHeaders[i].view, this);
            }
            this.mAnimationTargetTime = System.currentTimeMillis() + ((long) this.mAnimationDuration);
            this.mAdapter.configurePinnedHeaders(this);
            invalidateIfAnimating();
        }
        if (this.mOnScrollListener != null) {
            this.mOnScrollListener.onScroll(this, firstVisibleItem, visibleItemCount, totalItemCount);
        }
    }

    @Override
    protected float getTopFadingEdgeStrength() {
        if (this.mSize > 0) {
            return 0.0f;
        }
        return super.getTopFadingEdgeStrength();
    }

    @Override
    public void onScrollStateChanged(AbsListView view, int scrollState) {
        this.mScrollState = scrollState;
        if (this.mOnScrollListener != null) {
            this.mOnScrollListener.onScrollStateChanged(this, scrollState);
        }
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        int height = getHeight();
        int windowTop = 0;
        int windowBottom = height;
        int i = 0;
        while (true) {
            if (i >= this.mSize) {
                break;
            }
            PinnedHeader header = this.mHeaders[i];
            if (header.visible) {
                if (header.state == 0) {
                    windowTop = header.y + header.height;
                } else if (header.state == 1) {
                    windowBottom = header.y;
                    break;
                }
            }
            i++;
        }
        View selectedView = getSelectedView();
        if (selectedView != null) {
            if (selectedView.getTop() < windowTop) {
                setSelectionFromTop(position, windowTop);
            } else if (selectedView.getBottom() > windowBottom) {
                setSelectionFromTop(position, windowBottom - selectedView.getHeight());
            }
        }
        if (this.mOnItemSelectedListener != null) {
            this.mOnItemSelectedListener.onItemSelected(parent, view, position, id);
        }
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {
        if (this.mOnItemSelectedListener != null) {
            this.mOnItemSelectedListener.onNothingSelected(parent);
        }
    }

    public int getPinnedHeaderHeight(int viewIndex) {
        ensurePinnedHeaderLayout(viewIndex);
        return this.mHeaders[viewIndex].view.getHeight();
    }

    public void setHeaderPinnedAtTop(int viewIndex, int y, boolean animate) {
        ensurePinnedHeaderLayout(viewIndex);
        PinnedHeader header = this.mHeaders[viewIndex];
        header.visible = true;
        header.y = y;
        header.state = 0;
        header.animating = false;
    }

    public void setHeaderPinnedAtBottom(int viewIndex, int y, boolean animate) {
        ensurePinnedHeaderLayout(viewIndex);
        PinnedHeader header = this.mHeaders[viewIndex];
        header.state = 1;
        if (header.animating) {
            header.targetTime = this.mAnimationTargetTime;
            header.sourceY = header.y;
            header.targetY = y;
        } else {
            if (animate && (header.y != y || !header.visible)) {
                if (header.visible) {
                    header.sourceY = header.y;
                } else {
                    header.visible = true;
                    header.sourceY = header.height + y;
                }
                header.animating = true;
                header.targetVisible = true;
                header.targetTime = this.mAnimationTargetTime;
                header.targetY = y;
                return;
            }
            header.visible = true;
            header.y = y;
        }
    }

    public void setFadingHeader(int viewIndex, int position, boolean fade) {
        int bottom;
        int headerHeight;
        ensurePinnedHeaderLayout(viewIndex);
        View child = getChildAt(position - getFirstVisiblePosition());
        if (child != null) {
            PinnedHeader header = this.mHeaders[viewIndex];
            header.visible = true;
            header.state = 2;
            header.alpha = 255;
            header.animating = false;
            int top = getTotalTopPinnedHeaderHeight();
            header.y = top;
            if (fade && (bottom = child.getBottom() - top) < (headerHeight = header.height)) {
                int portion = bottom - headerHeight;
                header.alpha = ((headerHeight + portion) * 255) / headerHeight;
                header.y = top + portion;
            }
        }
    }

    public void setHeaderInvisible(int viewIndex, boolean animate) {
        PinnedHeader header = this.mHeaders[viewIndex];
        if (header.visible && ((animate || header.animating) && header.state == 1)) {
            header.sourceY = header.y;
            if (!header.animating) {
                header.visible = true;
                header.targetY = getBottom() + header.height;
            }
            header.animating = true;
            header.targetTime = this.mAnimationTargetTime;
            header.targetVisible = false;
            return;
        }
        header.visible = false;
    }

    private void ensurePinnedHeaderLayout(int viewIndex) {
        int widthSpec;
        int heightSpec;
        View view = this.mHeaders[viewIndex].view;
        if (view.isLayoutRequested()) {
            ViewGroup.LayoutParams layoutParams = view.getLayoutParams();
            if (layoutParams != null && layoutParams.width > 0) {
                widthSpec = View.MeasureSpec.makeMeasureSpec(layoutParams.width, 1073741824);
            } else {
                widthSpec = View.MeasureSpec.makeMeasureSpec(this.mHeaderWidth, 1073741824);
            }
            if (layoutParams != null && layoutParams.height > 0) {
                heightSpec = View.MeasureSpec.makeMeasureSpec(layoutParams.height, 1073741824);
            } else {
                heightSpec = View.MeasureSpec.makeMeasureSpec(0, 0);
            }
            view.measure(widthSpec, heightSpec);
            int height = view.getMeasuredHeight();
            this.mHeaders[viewIndex].height = height;
            view.layout(0, 0, view.getMeasuredWidth(), height);
        }
    }

    public int getTotalTopPinnedHeaderHeight() {
        int i = this.mSize;
        while (true) {
            i--;
            if (i < 0) {
                return 0;
            }
            PinnedHeader header = this.mHeaders[i];
            if (header.visible && header.state == 0) {
                return header.y + header.height;
            }
        }
    }

    public int getPositionAt(int y) {
        do {
            int position = pointToPosition(getPaddingLeft() + 1, y);
            if (position == -1) {
                y--;
            } else {
                return position;
            }
        } while (y > 0);
        return 0;
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        this.mHeaderTouched = false;
        if (super.onInterceptTouchEvent(ev)) {
            return true;
        }
        if (this.mScrollState == 0) {
            int y = (int) ev.getY();
            int x = (int) ev.getX();
            int i = this.mSize;
            while (true) {
                i--;
                if (i < 0) {
                    break;
                }
                PinnedHeader header = this.mHeaders[i];
                int padding = getPaddingLeft();
                if (header.visible && header.y <= y && header.y + header.height > y && x >= padding && header.view.getWidth() + padding >= x) {
                    this.mHeaderTouched = true;
                    if (this.mScrollToSectionOnHeaderTouch && ev.getAction() == 0) {
                        return smoothScrollToPartition(i);
                    }
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        if (!this.mHeaderTouched) {
            return super.onTouchEvent(ev);
        }
        if (ev.getAction() != 1) {
            return true;
        }
        this.mHeaderTouched = false;
        return true;
    }

    private boolean smoothScrollToPartition(int partition) {
        int position;
        if (this.mAdapter == null || (position = this.mAdapter.getScrollPositionForHeader(partition)) == -1) {
            return false;
        }
        int offset = 0;
        for (int i = 0; i < partition; i++) {
            PinnedHeader header = this.mHeaders[i];
            if (header.visible) {
                offset += header.height;
            }
        }
        smoothScrollToPositionFromTop(getHeaderViewsCount() + position, offset, 100);
        return true;
    }

    private void invalidateIfAnimating() {
        this.mAnimating = false;
        for (int i = 0; i < this.mSize; i++) {
            if (this.mHeaders[i].animating) {
                this.mAnimating = true;
                invalidate();
                return;
            }
        }
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        int newTop;
        long currentTime = this.mAnimating ? System.currentTimeMillis() : 0L;
        int top = 0;
        int bottom = getBottom();
        boolean hasVisibleHeaders = false;
        for (int i = 0; i < this.mSize; i++) {
            PinnedHeader header = this.mHeaders[i];
            if (header.visible) {
                hasVisibleHeaders = true;
                if (header.state == 1 && header.y < bottom) {
                    bottom = header.y;
                } else if ((header.state == 0 || header.state == 2) && (newTop = header.y + header.height) > top) {
                    top = newTop;
                }
            }
        }
        if (hasVisibleHeaders) {
            canvas.save();
        }
        super.dispatchDraw(canvas);
        if (hasVisibleHeaders) {
            canvas.restore();
            if (this.mSize > 0 && getFirstVisiblePosition() == 0) {
                View firstChild = getChildAt(0);
                PinnedHeader firstHeader = this.mHeaders[0];
                if (firstHeader != null) {
                    int firstHeaderTop = firstChild != null ? firstChild.getTop() : 0;
                    firstHeader.y = Math.max(firstHeader.y, firstHeaderTop);
                }
            }
            int i2 = this.mSize;
            while (true) {
                i2--;
                if (i2 < 0) {
                    break;
                }
                PinnedHeader header2 = this.mHeaders[i2];
                if (header2.visible && (header2.state == 0 || header2.state == 2)) {
                    drawHeader(canvas, header2, currentTime);
                }
            }
            for (int i3 = 0; i3 < this.mSize; i3++) {
                PinnedHeader header3 = this.mHeaders[i3];
                if (header3.visible && header3.state == 1) {
                    drawHeader(canvas, header3, currentTime);
                }
            }
        }
        invalidateIfAnimating();
    }

    private void drawHeader(Canvas canvas, PinnedHeader header, long currentTime) {
        if (header.animating) {
            int timeLeft = (int) (header.targetTime - currentTime);
            if (timeLeft <= 0) {
                header.y = header.targetY;
                header.visible = header.targetVisible;
                header.animating = false;
            } else {
                header.y = header.targetY + (((header.sourceY - header.targetY) * timeLeft) / this.mAnimationDuration);
            }
        }
        if (header.visible) {
            View view = header.view;
            int saveCount = canvas.save();
            int translateX = ViewUtil.isViewLayoutRtl(this) ? (getWidth() - this.mHeaderPaddingStart) - view.getWidth() : this.mHeaderPaddingStart;
            canvas.translate(translateX, header.y);
            if (header.state == 2) {
                this.mBounds.set(0.0f, 0.0f, view.getWidth(), view.getHeight());
                canvas.saveLayerAlpha(this.mBounds, header.alpha, 31);
            }
            view.draw(canvas);
            canvas.restoreToCount(saveCount);
        }
    }
}
