package com.android.contacts.list;

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
import android.widget.TextView;
import com.android.contacts.ContactPhotoManager;
import com.android.contacts.util.ViewUtil;
import com.mediatek.contacts.util.Log;

public class PinnedHeaderListView extends AutoScrollListView implements AbsListView.OnScrollListener, AdapterView.OnItemSelectedListener {
    private static final String TAG = PinnedHeaderListView.class.getSimpleName();
    private PinnedHeaderAdapter mAdapter;
    private boolean mAnimating;
    private int mAnimationDuration;
    private long mAnimationTargetTime;
    private RectF mBounds;
    private boolean mDrawPinnedHeader;
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

    public PinnedHeaderListView(Context context, AttributeSet attributeSet) {
        this(context, attributeSet, R.attr.listViewStyle);
    }

    public PinnedHeaderListView(Context context, AttributeSet attributeSet, int i) {
        super(context, attributeSet, i);
        this.mBounds = new RectF();
        this.mScrollToSectionOnHeaderTouch = false;
        this.mHeaderTouched = false;
        this.mAnimationDuration = 20;
        this.mDrawPinnedHeader = true;
        super.setOnScrollListener(this);
        super.setOnItemSelectedListener(this);
    }

    @Override
    protected void onLayout(boolean z, int i, int i2, int i3, int i4) {
        this.mHeaderPaddingStart = getPaddingStart();
        this.mHeaderWidth = ((i3 - i) - this.mHeaderPaddingStart) - getPaddingEnd();
        super.onLayout(z, i, i2, i3, i4);
    }

    @Override
    public void setAdapter(ListAdapter listAdapter) {
        this.mAdapter = (PinnedHeaderAdapter) listAdapter;
        super.setAdapter(listAdapter);
    }

    @Override
    public void setOnScrollListener(AbsListView.OnScrollListener onScrollListener) {
        this.mOnScrollListener = onScrollListener;
        super.setOnScrollListener(this);
    }

    @Override
    public void setOnItemSelectedListener(AdapterView.OnItemSelectedListener onItemSelectedListener) {
        this.mOnItemSelectedListener = onItemSelectedListener;
        super.setOnItemSelectedListener(this);
    }

    @Override
    public void onScroll(AbsListView absListView, int i, int i2, int i3) {
        if (this.mAdapter != null) {
            int pinnedHeaderCount = this.mAdapter.getPinnedHeaderCount();
            if (pinnedHeaderCount != this.mSize) {
                Log.d(TAG, "[onScroll] header count:" + pinnedHeaderCount + ", mSize:" + this.mSize);
                this.mSize = pinnedHeaderCount;
                if (this.mHeaders == null) {
                    this.mHeaders = new PinnedHeader[this.mSize];
                } else if (this.mHeaders.length < this.mSize) {
                    PinnedHeader[] pinnedHeaderArr = this.mHeaders;
                    this.mHeaders = new PinnedHeader[this.mSize];
                    System.arraycopy(pinnedHeaderArr, 0, this.mHeaders, 0, pinnedHeaderArr.length);
                }
            }
            for (int i4 = 0; i4 < this.mSize; i4++) {
                if (this.mHeaders[i4] == null) {
                    this.mHeaders[i4] = new PinnedHeader();
                }
                this.mHeaders[i4].view = this.mAdapter.getPinnedHeaderView(i4, this.mHeaders[i4].view, this);
            }
            this.mAnimationTargetTime = System.currentTimeMillis() + ((long) this.mAnimationDuration);
            this.mAdapter.configurePinnedHeaders(this);
            invalidateIfAnimating();
        }
        if (this.mOnScrollListener != null) {
            this.mOnScrollListener.onScroll(this, i, i2, i3);
        }
    }

    @Override
    protected float getTopFadingEdgeStrength() {
        return this.mSize > 0 ? ContactPhotoManager.OFFSET_DEFAULT : super.getTopFadingEdgeStrength();
    }

    @Override
    public void onScrollStateChanged(AbsListView absListView, int i) {
        this.mScrollState = i;
        if (this.mOnScrollListener != null) {
            this.mOnScrollListener.onScrollStateChanged(this, i);
        }
    }

    @Override
    public void onItemSelected(AdapterView<?> adapterView, View view, int i, long j) {
        int height = getHeight();
        int i2 = 0;
        int i3 = 0;
        while (true) {
            if (i2 >= this.mSize) {
                break;
            }
            PinnedHeader pinnedHeader = this.mHeaders[i2];
            if (pinnedHeader.visible) {
                if (pinnedHeader.state == 0) {
                    i3 = pinnedHeader.y + pinnedHeader.height;
                } else if (pinnedHeader.state == 1) {
                    height = pinnedHeader.y;
                    break;
                }
            }
            i2++;
        }
        View selectedView = getSelectedView();
        if (selectedView != null) {
            if (selectedView.getTop() < i3) {
                setSelectionFromTop(i, i3);
            } else if (selectedView.getBottom() > height) {
                setSelectionFromTop(i, height - selectedView.getHeight());
            }
        }
        if (this.mOnItemSelectedListener != null) {
            this.mOnItemSelectedListener.onItemSelected(adapterView, view, i, j);
        }
    }

    @Override
    public void onNothingSelected(AdapterView<?> adapterView) {
        if (this.mOnItemSelectedListener != null) {
            this.mOnItemSelectedListener.onNothingSelected(adapterView);
        }
    }

    public int getPinnedHeaderHeight(int i) {
        ensurePinnedHeaderLayout(i);
        return this.mHeaders[i].view.getHeight();
    }

    public void setHeaderPinnedAtTop(int i, int i2, boolean z) {
        ensurePinnedHeaderLayout(i);
        PinnedHeader pinnedHeader = this.mHeaders[i];
        pinnedHeader.visible = true;
        pinnedHeader.y = i2;
        pinnedHeader.state = 0;
        pinnedHeader.animating = false;
    }

    public void setHeaderPinnedAtBottom(int i, int i2, boolean z) {
        ensurePinnedHeaderLayout(i);
        PinnedHeader pinnedHeader = this.mHeaders[i];
        pinnedHeader.state = 1;
        if (pinnedHeader.animating) {
            pinnedHeader.targetTime = this.mAnimationTargetTime;
            pinnedHeader.sourceY = pinnedHeader.y;
            pinnedHeader.targetY = i2;
        } else {
            if (z && (pinnedHeader.y != i2 || !pinnedHeader.visible)) {
                if (pinnedHeader.visible) {
                    pinnedHeader.sourceY = pinnedHeader.y;
                } else {
                    pinnedHeader.visible = true;
                    pinnedHeader.sourceY = pinnedHeader.height + i2;
                }
                pinnedHeader.animating = true;
                pinnedHeader.targetVisible = true;
                pinnedHeader.targetTime = this.mAnimationTargetTime;
                pinnedHeader.targetY = i2;
                return;
            }
            pinnedHeader.visible = true;
            pinnedHeader.y = i2;
        }
    }

    public void setFadingHeader(int i, int i2, boolean z) {
        int bottom;
        int i3;
        if (!this.mDrawPinnedHeader) {
            return;
        }
        ensurePinnedHeaderLayout(i);
        View childAt = getChildAt(i2 - getFirstVisiblePosition());
        if (childAt == null) {
            return;
        }
        PinnedHeader pinnedHeader = this.mHeaders[i];
        pinnedHeader.visible = !((TextView) pinnedHeader.view).getText().toString().isEmpty();
        pinnedHeader.state = 2;
        pinnedHeader.alpha = 255;
        pinnedHeader.animating = false;
        int totalTopPinnedHeaderHeight = getTotalTopPinnedHeaderHeight();
        pinnedHeader.y = totalTopPinnedHeaderHeight;
        if (z && (bottom = childAt.getBottom() - totalTopPinnedHeaderHeight) < (i3 = pinnedHeader.height)) {
            int i4 = bottom - i3;
            pinnedHeader.alpha = (255 * (i3 + i4)) / i3;
            pinnedHeader.y = totalTopPinnedHeaderHeight + i4;
        }
    }

    public void setHeaderInvisible(int i, boolean z) {
        PinnedHeader pinnedHeader = this.mHeaders[i];
        if (pinnedHeader.visible && ((z || pinnedHeader.animating) && pinnedHeader.state == 1)) {
            pinnedHeader.sourceY = pinnedHeader.y;
            if (!pinnedHeader.animating) {
                pinnedHeader.visible = true;
                pinnedHeader.targetY = getBottom() + pinnedHeader.height;
            }
            pinnedHeader.animating = true;
            pinnedHeader.targetTime = this.mAnimationTargetTime;
            pinnedHeader.targetVisible = false;
            return;
        }
        pinnedHeader.visible = false;
    }

    private void ensurePinnedHeaderLayout(int i) {
        int iMakeMeasureSpec;
        int iMakeMeasureSpec2;
        View view = this.mHeaders[i].view;
        if (view.isLayoutRequested()) {
            ViewGroup.LayoutParams layoutParams = view.getLayoutParams();
            if (layoutParams != null && layoutParams.width > 0) {
                iMakeMeasureSpec = View.MeasureSpec.makeMeasureSpec(layoutParams.width, 1073741824);
            } else {
                iMakeMeasureSpec = View.MeasureSpec.makeMeasureSpec(this.mHeaderWidth, 1073741824);
            }
            if (layoutParams != null && layoutParams.height > 0) {
                iMakeMeasureSpec2 = View.MeasureSpec.makeMeasureSpec(layoutParams.height, 1073741824);
            } else {
                iMakeMeasureSpec2 = View.MeasureSpec.makeMeasureSpec(0, 0);
            }
            view.measure(iMakeMeasureSpec, iMakeMeasureSpec2);
            int measuredHeight = view.getMeasuredHeight();
            this.mHeaders[i].height = measuredHeight;
            view.layout(0, 0, view.getMeasuredWidth(), measuredHeight);
        }
    }

    public int getTotalTopPinnedHeaderHeight() {
        int i = this.mSize;
        while (true) {
            i--;
            if (i >= 0) {
                PinnedHeader pinnedHeader = this.mHeaders[i];
                if (pinnedHeader.visible && pinnedHeader.state == 0) {
                    return pinnedHeader.y + pinnedHeader.height;
                }
            } else {
                return 0;
            }
        }
    }

    public int getPositionAt(int i) {
        do {
            int iPointToPosition = pointToPosition(getPaddingLeft() + 1, i);
            if (iPointToPosition != -1) {
                return iPointToPosition;
            }
            i--;
        } while (i > 0);
        return 0;
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent motionEvent) {
        int width;
        this.mHeaderTouched = false;
        if (super.onInterceptTouchEvent(motionEvent)) {
            return true;
        }
        if (this.mScrollState == 0) {
            int y = (int) motionEvent.getY();
            int x = (int) motionEvent.getX();
            int i = this.mSize;
            while (true) {
                i--;
                if (i < 0) {
                    break;
                }
                PinnedHeader pinnedHeader = this.mHeaders[i];
                if (ViewUtil.isViewLayoutRtl(this)) {
                    width = (getWidth() - this.mHeaderPaddingStart) - pinnedHeader.view.getWidth();
                } else {
                    width = this.mHeaderPaddingStart;
                }
                if (pinnedHeader.visible && pinnedHeader.y <= y && pinnedHeader.y + pinnedHeader.height > y && x >= width && width + pinnedHeader.view.getWidth() >= x) {
                    this.mHeaderTouched = true;
                    if (this.mScrollToSectionOnHeaderTouch && motionEvent.getAction() == 0) {
                        return smoothScrollToPartition(i);
                    }
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public boolean onTouchEvent(MotionEvent motionEvent) {
        if (this.mHeaderTouched) {
            if (motionEvent.getAction() == 1) {
                this.mHeaderTouched = false;
            }
            return true;
        }
        return super.onTouchEvent(motionEvent);
    }

    private boolean smoothScrollToPartition(int i) {
        int scrollPositionForHeader;
        if (this.mAdapter == null || (scrollPositionForHeader = this.mAdapter.getScrollPositionForHeader(i)) == -1) {
            return false;
        }
        int i2 = 0;
        for (int i3 = 0; i3 < i; i3++) {
            PinnedHeader pinnedHeader = this.mHeaders[i3];
            if (pinnedHeader.visible) {
                i2 += pinnedHeader.height;
            }
        }
        smoothScrollToPositionFromTop(scrollPositionForHeader + getHeaderViewsCount(), i2, 100);
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
        int i;
        long jCurrentTimeMillis = this.mAnimating ? System.currentTimeMillis() : 0L;
        int bottom = getBottom();
        boolean z = false;
        int i2 = 0;
        for (int i3 = 0; i3 < this.mSize; i3++) {
            PinnedHeader pinnedHeader = this.mHeaders[i3];
            if (pinnedHeader.visible) {
                if (pinnedHeader.state == 1 && pinnedHeader.y < bottom) {
                    bottom = pinnedHeader.y;
                } else if ((pinnedHeader.state == 0 || pinnedHeader.state == 2) && (i = pinnedHeader.y + pinnedHeader.height) > i2) {
                    i2 = i;
                }
                z = true;
            }
        }
        if (z) {
            canvas.save();
        }
        super.dispatchDraw(canvas);
        if (z) {
            canvas.restore();
            if (this.mSize > 0 && getFirstVisiblePosition() == 0) {
                View childAt = getChildAt(0);
                PinnedHeader pinnedHeader2 = this.mHeaders[0];
                if (pinnedHeader2 != null) {
                    pinnedHeader2.y = Math.max(pinnedHeader2.y, childAt != null ? childAt.getTop() : 0);
                }
            }
            int i4 = this.mSize;
            while (true) {
                i4--;
                if (i4 < 0) {
                    break;
                }
                PinnedHeader pinnedHeader3 = this.mHeaders[i4];
                if (pinnedHeader3.visible && (pinnedHeader3.state == 0 || pinnedHeader3.state == 2)) {
                    drawHeader(canvas, pinnedHeader3, jCurrentTimeMillis);
                }
            }
            for (int i5 = 0; i5 < this.mSize; i5++) {
                PinnedHeader pinnedHeader4 = this.mHeaders[i5];
                if (pinnedHeader4.visible && pinnedHeader4.state == 1) {
                    drawHeader(canvas, pinnedHeader4, jCurrentTimeMillis);
                }
            }
        }
        invalidateIfAnimating();
    }

    private void drawHeader(Canvas canvas, PinnedHeader pinnedHeader, long j) {
        int width;
        if (!this.mDrawPinnedHeader) {
            return;
        }
        if (pinnedHeader.animating) {
            int i = (int) (pinnedHeader.targetTime - j);
            if (i <= 0) {
                int i2 = pinnedHeader.y;
                pinnedHeader.y = pinnedHeader.targetY;
                pinnedHeader.visible = pinnedHeader.targetVisible;
                pinnedHeader.animating = false;
                if (i2 != pinnedHeader.y) {
                    postInvalidate();
                }
            } else {
                pinnedHeader.y = pinnedHeader.targetY + (((pinnedHeader.sourceY - pinnedHeader.targetY) * i) / this.mAnimationDuration);
            }
        }
        if (pinnedHeader.visible) {
            View view = pinnedHeader.view;
            int iSave = canvas.save();
            if (ViewUtil.isViewLayoutRtl(this)) {
                width = (getWidth() - this.mHeaderPaddingStart) - view.getWidth();
            } else {
                width = this.mHeaderPaddingStart;
            }
            canvas.translate(width, pinnedHeader.y);
            if (pinnedHeader.state == 2) {
                this.mBounds.set(ContactPhotoManager.OFFSET_DEFAULT, ContactPhotoManager.OFFSET_DEFAULT, view.getWidth(), view.getHeight());
                canvas.saveLayerAlpha(this.mBounds, pinnedHeader.alpha, 31);
            }
            view.draw(canvas);
            canvas.restoreToCount(iSave);
        }
    }

    public void setDrawPinnedHeader(boolean z) {
        this.mDrawPinnedHeader = z;
    }
}
