package com.android.launcher3.views;

import android.R;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.util.Property;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.widget.TextView;
import com.android.launcher3.BaseRecyclerView;
import com.android.launcher3.Utilities;
import com.android.launcher3.graphics.FastScrollThumbDrawable;
import com.android.launcher3.util.Themes;

/* loaded from: classes.dex */
public class RecyclerViewFastScroller extends View {
    private static final float FAST_SCROLL_OVERLAY_Y_OFFSET_FACTOR = 0.75f;
    private static final int MAX_TRACK_ALPHA = 30;
    private static final int SCROLL_BAR_VIS_DURATION = 150;
    private static final int SCROLL_DELTA_THRESHOLD_DP = 4;
    private final boolean mCanThumbDetach;
    private final ViewConfiguration mConfig;
    private final float mDeltaThreshold;
    private int mDownX;
    private int mDownY;
    private int mDy;
    private boolean mIgnoreDragGesture;
    private boolean mIsDragging;
    private boolean mIsThumbDetached;
    private float mLastTouchY;
    private int mLastY;
    private final int mMaxWidth;
    private final int mMinWidth;
    private RecyclerView.OnScrollListener mOnScrollListener;
    private String mPopupSectionName;
    private TextView mPopupView;
    private boolean mPopupVisible;
    protected BaseRecyclerView mRv;
    protected final int mThumbHeight;
    protected int mThumbOffsetY;
    private final int mThumbPadding;
    private final Paint mThumbPaint;
    protected int mTouchOffsetY;
    private final Paint mTrackPaint;
    private int mWidth;
    private ObjectAnimator mWidthAnimator;
    private static final Rect sTempRect = new Rect();
    private static final Property<RecyclerViewFastScroller, Integer> TRACK_WIDTH = new Property<RecyclerViewFastScroller, Integer>(Integer.class, "width") { // from class: com.android.launcher3.views.RecyclerViewFastScroller.1
        /* JADX DEBUG: Method merged with bridge method: get(Ljava/lang/Object;)Ljava/lang/Object; */
        @Override // android.util.Property
        public Integer get(RecyclerViewFastScroller recyclerViewFastScroller) {
            return Integer.valueOf(recyclerViewFastScroller.mWidth);
        }

        /* JADX DEBUG: Method merged with bridge method: set(Ljava/lang/Object;Ljava/lang/Object;)V */
        @Override // android.util.Property
        public void set(RecyclerViewFastScroller recyclerViewFastScroller, Integer num) {
            recyclerViewFastScroller.setTrackWidth(num.intValue());
        }
    };

    public RecyclerViewFastScroller(Context context) {
        this(context, null);
    }

    public RecyclerViewFastScroller(Context context, AttributeSet attributeSet) {
        this(context, attributeSet, 0);
    }

    public RecyclerViewFastScroller(Context context, AttributeSet attributeSet, int i) throws Resources.NotFoundException {
        super(context, attributeSet, i);
        this.mDy = 0;
        this.mTrackPaint = new Paint();
        this.mTrackPaint.setColor(Themes.getAttrColor(context, R.attr.textColorPrimary));
        this.mTrackPaint.setAlpha(30);
        this.mThumbPaint = new Paint();
        this.mThumbPaint.setAntiAlias(true);
        this.mThumbPaint.setColor(Themes.getColorAccent(context));
        this.mThumbPaint.setStyle(Paint.Style.FILL);
        Resources resources = getResources();
        int dimensionPixelSize = resources.getDimensionPixelSize(com.android.launcher3.R.dimen.fastscroll_track_min_width);
        this.mMinWidth = dimensionPixelSize;
        this.mWidth = dimensionPixelSize;
        this.mMaxWidth = resources.getDimensionPixelSize(com.android.launcher3.R.dimen.fastscroll_track_max_width);
        this.mThumbPadding = resources.getDimensionPixelSize(com.android.launcher3.R.dimen.fastscroll_thumb_padding);
        this.mThumbHeight = resources.getDimensionPixelSize(com.android.launcher3.R.dimen.fastscroll_thumb_height);
        this.mConfig = ViewConfiguration.get(context);
        this.mDeltaThreshold = resources.getDisplayMetrics().density * 4.0f;
        TypedArray typedArrayObtainStyledAttributes = context.obtainStyledAttributes(attributeSet, com.android.launcher3.R.styleable.RecyclerViewFastScroller, i, 0);
        this.mCanThumbDetach = typedArrayObtainStyledAttributes.getBoolean(0, false);
        typedArrayObtainStyledAttributes.recycle();
    }

    public void setRecyclerView(BaseRecyclerView baseRecyclerView, TextView textView) {
        if (this.mRv != null && this.mOnScrollListener != null) {
            this.mRv.removeOnScrollListener(this.mOnScrollListener);
        }
        this.mRv = baseRecyclerView;
        BaseRecyclerView baseRecyclerView2 = this.mRv;
        RecyclerView.OnScrollListener onScrollListener = new RecyclerView.OnScrollListener() { // from class: com.android.launcher3.views.RecyclerViewFastScroller.2
            @Override // android.support.v7.widget.RecyclerView.OnScrollListener
            public void onScrolled(RecyclerView recyclerView, int i, int i2) {
                RecyclerViewFastScroller.this.mDy = i2;
                RecyclerViewFastScroller.this.mRv.onUpdateScrollbar(i2);
            }
        };
        this.mOnScrollListener = onScrollListener;
        baseRecyclerView2.addOnScrollListener(onScrollListener);
        this.mPopupView = textView;
        this.mPopupView.setBackground(new FastScrollThumbDrawable(this.mThumbPaint, Utilities.isRtl(getResources())));
    }

    public void reattachThumbToScroll() {
        this.mIsThumbDetached = false;
    }

    public void setThumbOffsetY(int i) {
        if (this.mThumbOffsetY == i) {
            return;
        }
        this.mThumbOffsetY = i;
        invalidate();
    }

    public int getThumbOffsetY() {
        return this.mThumbOffsetY;
    }

    private void setTrackWidth(int i) {
        if (this.mWidth == i) {
            return;
        }
        this.mWidth = i;
        invalidate();
    }

    public int getThumbHeight() {
        return this.mThumbHeight;
    }

    public boolean isDraggingThumb() {
        return this.mIsDragging;
    }

    public boolean isThumbDetached() {
        return this.mIsThumbDetached;
    }

    public boolean handleTouchEvent(MotionEvent motionEvent, Point point) {
        int x = ((int) motionEvent.getX()) - point.x;
        int y = ((int) motionEvent.getY()) - point.y;
        switch (motionEvent.getAction()) {
            case 0:
                this.mDownX = x;
                this.mLastY = y;
                this.mDownY = y;
                if (Math.abs(this.mDy) < this.mDeltaThreshold && this.mRv.getScrollState() != 0) {
                    this.mRv.stopScroll();
                }
                if (isNearThumb(x, y)) {
                    this.mTouchOffsetY = this.mDownY - this.mThumbOffsetY;
                    break;
                } else if (this.mRv.supportsFastScrolling() && isNearScrollBar(this.mDownX)) {
                    calcTouchOffsetAndPrepToFastScroll(this.mDownY, this.mLastY);
                    updateFastScrollSectionNameAndThumbOffset(this.mLastY, y);
                    break;
                }
                break;
            case 1:
            case 3:
                this.mRv.onFastScrollCompleted();
                this.mTouchOffsetY = 0;
                this.mLastTouchY = 0.0f;
                this.mIgnoreDragGesture = false;
                if (this.mIsDragging) {
                    this.mIsDragging = false;
                    animatePopupVisibility(false);
                    showActiveScrollbar(false);
                    break;
                }
                break;
            case 2:
                this.mLastY = y;
                this.mIgnoreDragGesture |= Math.abs(y - this.mDownY) > this.mConfig.getScaledPagingTouchSlop();
                if (!this.mIsDragging && !this.mIgnoreDragGesture && this.mRv.supportsFastScrolling() && isNearThumb(this.mDownX, this.mLastY) && Math.abs(y - this.mDownY) > this.mConfig.getScaledTouchSlop()) {
                    calcTouchOffsetAndPrepToFastScroll(this.mDownY, this.mLastY);
                }
                if (this.mIsDragging) {
                    updateFastScrollSectionNameAndThumbOffset(this.mLastY, y);
                    break;
                }
                break;
        }
        return this.mIsDragging;
    }

    private void calcTouchOffsetAndPrepToFastScroll(int i, int i2) {
        this.mIsDragging = true;
        if (this.mCanThumbDetach) {
            this.mIsThumbDetached = true;
        }
        this.mTouchOffsetY += i2 - i;
        animatePopupVisibility(true);
        showActiveScrollbar(true);
    }

    private void updateFastScrollSectionNameAndThumbOffset(int i, int i2) {
        int scrollbarTrackHeight = this.mRv.getScrollbarTrackHeight() - this.mThumbHeight;
        float fMax = Math.max(0, Math.min(scrollbarTrackHeight, i2 - this.mTouchOffsetY));
        String strScrollToPositionAtProgress = this.mRv.scrollToPositionAtProgress(fMax / scrollbarTrackHeight);
        if (!strScrollToPositionAtProgress.equals(this.mPopupSectionName)) {
            this.mPopupSectionName = strScrollToPositionAtProgress;
            this.mPopupView.setText(strScrollToPositionAtProgress);
        }
        animatePopupVisibility(!strScrollToPositionAtProgress.isEmpty());
        updatePopupY(i);
        this.mLastTouchY = fMax;
        setThumbOffsetY((int) this.mLastTouchY);
    }

    @Override // android.view.View
    public void onDraw(Canvas canvas) {
        if (this.mThumbOffsetY < 0) {
            return;
        }
        int iSave = canvas.save();
        canvas.translate(getWidth() / 2, this.mRv.getScrollBarTop());
        float f = this.mWidth / 2;
        canvas.drawRoundRect(-f, 0.0f, f, this.mRv.getScrollbarTrackHeight(), this.mWidth, this.mWidth, this.mTrackPaint);
        canvas.translate(0.0f, this.mThumbOffsetY);
        float f2 = f + this.mThumbPadding;
        float f3 = this.mWidth + this.mThumbPadding + this.mThumbPadding;
        canvas.drawRoundRect(-f2, 0.0f, f2, this.mThumbHeight, f3, f3, this.mThumbPaint);
        canvas.restoreToCount(iSave);
    }

    private void showActiveScrollbar(boolean z) {
        if (this.mWidthAnimator != null) {
            this.mWidthAnimator.cancel();
        }
        Property<RecyclerViewFastScroller, Integer> property = TRACK_WIDTH;
        int[] iArr = new int[1];
        iArr[0] = z ? this.mMaxWidth : this.mMinWidth;
        this.mWidthAnimator = ObjectAnimator.ofInt(this, property, iArr);
        this.mWidthAnimator.setDuration(150L);
        this.mWidthAnimator.start();
    }

    private boolean isNearThumb(int i, int i2) {
        int i3 = i2 - this.mThumbOffsetY;
        return i >= 0 && i < getWidth() && i3 >= 0 && i3 <= this.mThumbHeight;
    }

    public boolean shouldBlockIntercept(int i, int i2) {
        return isNearThumb(i, i2);
    }

    public boolean isNearScrollBar(int i) {
        return i >= (getWidth() - this.mMaxWidth) / 2 && i <= (getWidth() + this.mMaxWidth) / 2;
    }

    private void animatePopupVisibility(boolean z) {
        if (this.mPopupVisible != z) {
            this.mPopupVisible = z;
            this.mPopupView.animate().cancel();
            this.mPopupView.animate().alpha(z ? 1.0f : 0.0f).setDuration(z ? 200L : 150L).start();
        }
    }

    private void updatePopupY(int i) {
        this.mPopupView.setTranslationY(Utilities.boundToRange((i - (0.75f * this.mPopupView.getHeight())) + this.mRv.getScrollBarTop(), this.mMaxWidth, (this.mRv.getScrollbarTrackHeight() - this.mMaxWidth) - r0));
    }

    public boolean isHitInParent(float f, float f2, Point point) {
        if (this.mThumbOffsetY < 0) {
            return false;
        }
        getHitRect(sTempRect);
        sTempRect.top += this.mRv.getScrollBarTop();
        if (point != null) {
            point.set(sTempRect.left, sTempRect.top);
        }
        return sTempRect.contains((int) f, (int) f2);
    }

    @Override // android.view.View
    public boolean hasOverlappingRendering() {
        return false;
    }
}
