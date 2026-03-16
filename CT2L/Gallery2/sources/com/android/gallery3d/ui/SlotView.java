package com.android.gallery3d.ui;

import android.graphics.Rect;
import android.os.Handler;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.animation.DecelerateInterpolator;
import com.android.gallery3d.anim.Animation;
import com.android.gallery3d.app.AbstractGalleryActivity;
import com.android.gallery3d.common.Utils;
import com.android.gallery3d.glrenderer.GLCanvas;

public class SlotView extends GLView {
    private boolean mDownInScrolling;
    private final GestureDetector mGestureDetector;
    private final Handler mHandler;
    private Listener mListener;
    private SlotRenderer mRenderer;
    private final ScrollerHelper mScroller;
    private UserInteractionListener mUIListener;
    private final Paper mPaper = new Paper();
    private boolean mMoreAnimation = false;
    private SlotAnimation mAnimation = null;
    private final Layout mLayout = new Layout(this);
    private int mStartIndex = -1;
    private int mOverscrollEffect = 0;
    private int[] mRequestRenderSlots = new int[16];
    private final Rect mTempRect = new Rect();

    public interface Listener {
        void onDown(int i);

        void onLongTap(int i);

        void onScrollPositionChanged(int i, int i2);

        void onSingleTapUp(int i);

        void onUp(boolean z);
    }

    public interface SlotRenderer {
        void onSlotSizeChanged(int i, int i2);

        void onVisibleRangeChanged(int i, int i2);

        void prepareDrawing();

        int renderSlot(GLCanvas gLCanvas, int i, int i2, int i3, int i4);
    }

    public static class Spec {
        public int slotWidth = -1;
        public int slotHeight = -1;
        public int slotHeightAdditional = 0;
        public int rowsLand = -1;
        public int rowsPort = -1;
        public int slotGap = -1;
    }

    public static class SimpleListener implements Listener {
        @Override
        public void onDown(int index) {
        }

        @Override
        public void onUp(boolean followedByLongPress) {
        }

        @Override
        public void onSingleTapUp(int index) {
        }

        @Override
        public void onLongTap(int index) {
        }

        @Override
        public void onScrollPositionChanged(int position, int total) {
        }
    }

    public SlotView(AbstractGalleryActivity activity, Spec spec) {
        this.mGestureDetector = new GestureDetector(activity, new MyGestureListener());
        this.mScroller = new ScrollerHelper(activity);
        this.mHandler = new SynchronizedHandler(activity.getGLRoot());
        setSlotSpec(spec);
    }

    public void setSlotRenderer(SlotRenderer slotDrawer) {
        this.mRenderer = slotDrawer;
        if (this.mRenderer == null) {
            return;
        }
        this.mRenderer.onSlotSizeChanged(this.mLayout.mSlotWidth, this.mLayout.mSlotHeight);
        this.mRenderer.onVisibleRangeChanged(getVisibleStart(), getVisibleEnd());
    }

    public void setCenterIndex(int index) {
        int slotCount = this.mLayout.mSlotCount;
        if (index >= 0 && index < slotCount) {
            Rect rect = this.mLayout.getSlotRect(index, this.mTempRect);
            int position = ((rect.left + rect.right) - getWidth()) / 2;
            setScrollPosition(position);
        }
    }

    public void makeSlotVisible(int index) {
        Rect rect = this.mLayout.getSlotRect(index, this.mTempRect);
        int visibleBegin = this.mScrollX;
        int visibleLength = getWidth();
        int visibleEnd = visibleBegin + visibleLength;
        int slotBegin = rect.left;
        int slotEnd = rect.right;
        int position = visibleBegin;
        if (visibleLength < slotEnd - slotBegin) {
            position = visibleBegin;
        } else if (slotBegin < visibleBegin) {
            position = slotBegin;
        } else if (slotEnd > visibleEnd) {
            position = slotEnd - visibleLength;
        }
        setScrollPosition(position);
    }

    public void setScrollPosition(int position) {
        int position2 = Utils.clamp(position, 0, this.mLayout.getScrollLimit());
        this.mScroller.setPosition(position2);
        updateScrollPosition(position2, false);
    }

    public void setSlotSpec(Spec spec) {
        this.mLayout.setSlotSpec(spec);
    }

    @Override
    public void addComponent(GLView view) {
        throw new UnsupportedOperationException();
    }

    @Override
    protected void onLayout(boolean changeSize, int l, int t, int r, int b) {
        if (changeSize) {
            int visibleIndex = (this.mLayout.getVisibleStart() + this.mLayout.getVisibleEnd()) / 2;
            this.mLayout.setSize(r - l, b - t);
            makeSlotVisible(visibleIndex);
            if (this.mOverscrollEffect == 0) {
                this.mPaper.setSize(r - l, b - t);
            }
        }
    }

    public void startRisingAnimation() {
        this.mAnimation = new RisingAnimation();
        this.mAnimation.start();
        if (this.mLayout.mSlotCount != 0) {
            invalidate();
        }
    }

    private void updateScrollPosition(int position, boolean force) {
        if (force || position != this.mScrollX) {
            this.mScrollX = position;
            this.mLayout.setScrollPosition(position);
            onScrollPositionChanged(position);
        }
    }

    protected void onScrollPositionChanged(int newPosition) {
        int limit = this.mLayout.getScrollLimit();
        this.mListener.onScrollPositionChanged(newPosition, limit);
    }

    public Rect getSlotRect(int slotIndex) {
        return this.mLayout.getSlotRect(slotIndex, new Rect());
    }

    @Override
    protected boolean onTouch(MotionEvent event) {
        if (this.mUIListener != null) {
            this.mUIListener.onUserInteraction();
        }
        this.mGestureDetector.onTouchEvent(event);
        switch (event.getAction()) {
            case 0:
                this.mDownInScrolling = !this.mScroller.isFinished();
                this.mScroller.forceFinished();
                return true;
            case 1:
                this.mPaper.onRelease();
                invalidate();
                return true;
            default:
                return true;
        }
    }

    public void setListener(Listener listener) {
        this.mListener = listener;
    }

    private static int[] expandIntArray(int[] array, int capacity) {
        while (array.length < capacity) {
            array = new int[array.length * 2];
        }
        return array;
    }

    @Override
    protected void render(GLCanvas canvas) {
        int newCount;
        super.render(canvas);
        if (this.mRenderer != null) {
            this.mRenderer.prepareDrawing();
            long animTime = AnimationTime.get();
            boolean more = this.mScroller.advanceAnimation(animTime);
            boolean more2 = more | this.mLayout.advanceAnimation(animTime);
            int oldX = this.mScrollX;
            updateScrollPosition(this.mScroller.getPosition(), false);
            boolean paperActive = false;
            if (this.mOverscrollEffect == 0) {
                int newX = this.mScrollX;
                int limit = this.mLayout.getScrollLimit();
                if ((oldX > 0 && newX == 0) || (oldX < limit && newX == limit)) {
                    float v = this.mScroller.getCurrVelocity();
                    if (newX == limit) {
                        v = -v;
                    }
                    if (!Float.isNaN(v)) {
                        this.mPaper.edgeReached(v);
                    }
                }
                paperActive = this.mPaper.advanceAnimation();
            }
            boolean more3 = more2 | paperActive;
            if (this.mAnimation != null) {
                more3 |= this.mAnimation.calculate(animTime);
            }
            canvas.translate(-this.mScrollX, -this.mScrollY);
            int requestCount = 0;
            int[] requestedSlot = expandIntArray(this.mRequestRenderSlots, this.mLayout.mVisibleEnd - this.mLayout.mVisibleStart);
            for (int i = this.mLayout.mVisibleEnd - 1; i >= this.mLayout.mVisibleStart; i--) {
                int r = renderItem(canvas, i, 0, paperActive);
                if ((r & 2) != 0) {
                    more3 = true;
                }
                if ((r & 1) != 0) {
                    requestedSlot[requestCount] = i;
                    requestCount++;
                }
            }
            int pass = 1;
            while (requestCount != 0) {
                int newCount2 = 0;
                int i2 = 0;
                while (true) {
                    newCount = newCount2;
                    if (i2 < requestCount) {
                        int r2 = renderItem(canvas, requestedSlot[i2], pass, paperActive);
                        if ((r2 & 2) != 0) {
                            more3 = true;
                        }
                        if ((r2 & 1) != 0) {
                            newCount2 = newCount + 1;
                            requestedSlot[newCount] = i2;
                        } else {
                            newCount2 = newCount;
                        }
                        i2++;
                    }
                }
                requestCount = newCount;
                pass++;
            }
            canvas.translate(this.mScrollX, this.mScrollY);
            if (more3) {
                invalidate();
            }
            final UserInteractionListener listener = this.mUIListener;
            if (this.mMoreAnimation && !more3 && listener != null) {
                this.mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        listener.onUserInteractionEnd();
                    }
                });
            }
            this.mMoreAnimation = more3;
        }
    }

    private int renderItem(GLCanvas canvas, int index, int pass, boolean paperActive) {
        canvas.save(3);
        Rect rect = this.mLayout.getSlotRect(index, this.mTempRect);
        if (paperActive) {
            canvas.multiplyMatrix(this.mPaper.getTransform(rect, this.mScrollX), 0);
        } else {
            canvas.translate(rect.left, rect.top, 0.0f);
        }
        if (this.mAnimation != null && this.mAnimation.isActive()) {
            this.mAnimation.apply(canvas, index, rect);
        }
        int result = this.mRenderer.renderSlot(canvas, index, pass, rect.right - rect.left, rect.bottom - rect.top);
        canvas.restore();
        return result;
    }

    public static abstract class SlotAnimation extends Animation {
        protected float mProgress = 0.0f;

        public abstract void apply(GLCanvas gLCanvas, int i, Rect rect);

        public SlotAnimation() {
            setInterpolator(new DecelerateInterpolator(4.0f));
            setDuration(1500);
        }

        @Override
        protected void onCalculate(float progress) {
            this.mProgress = progress;
        }
    }

    public static class RisingAnimation extends SlotAnimation {
        @Override
        public void apply(GLCanvas canvas, int slotIndex, Rect target) {
            canvas.translate(0.0f, 0.0f, 128.0f * (1.0f - this.mProgress));
        }
    }

    public class Layout {
        private int mContentLength;
        private int mHeight;
        private IntegerAnimation mHorizontalPadding;
        private int mScrollPosition;
        private int mSlotCount;
        private int mSlotGap;
        private int mSlotHeight;
        private int mSlotWidth;
        private Spec mSpec;
        private int mUnitCount;
        private IntegerAnimation mVerticalPadding;
        private int mVisibleEnd;
        private int mVisibleStart;
        private int mWidth;
        final SlotView this$0;

        public Layout(SlotView slotView) {
            this.this$0 = slotView;
            this.mVerticalPadding = new IntegerAnimation();
            this.mHorizontalPadding = new IntegerAnimation();
        }

        public void setSlotSpec(Spec spec) {
            this.mSpec = spec;
        }

        public boolean setSlotCount(int slotCount) {
            if (slotCount == this.mSlotCount) {
                return false;
            }
            if (this.mSlotCount != 0) {
                this.mHorizontalPadding.setEnabled(true);
                this.mVerticalPadding.setEnabled(true);
            }
            this.mSlotCount = slotCount;
            int hPadding = this.mHorizontalPadding.getTarget();
            int vPadding = this.mVerticalPadding.getTarget();
            initLayoutParameters();
            return (vPadding == this.mVerticalPadding.getTarget() && hPadding == this.mHorizontalPadding.getTarget()) ? false : true;
        }

        public Rect getSlotRect(int index, Rect rect) {
            int col = index / this.mUnitCount;
            int row = index - (this.mUnitCount * col);
            int x = this.mHorizontalPadding.get() + ((this.mSlotWidth + this.mSlotGap) * col);
            int y = this.mVerticalPadding.get() + ((this.mSlotHeight + this.mSlotGap) * row);
            rect.set(x, y, this.mSlotWidth + x, this.mSlotHeight + y);
            return rect;
        }

        private void initLayoutParameters(int majorLength, int minorLength, int majorUnitSize, int minorUnitSize, int[] padding) {
            int unitCount = (this.mSlotGap + minorLength) / (this.mSlotGap + minorUnitSize);
            if (unitCount == 0) {
                unitCount = 1;
            }
            this.mUnitCount = unitCount;
            int availableUnits = Math.min(this.mUnitCount, this.mSlotCount);
            int usedMinorLength = (availableUnits * minorUnitSize) + ((availableUnits - 1) * this.mSlotGap);
            padding[0] = (minorLength - usedMinorLength) / 2;
            int count = ((this.mSlotCount + this.mUnitCount) - 1) / this.mUnitCount;
            this.mContentLength = (count * majorUnitSize) + ((count - 1) * this.mSlotGap);
            padding[1] = Math.max(0, (majorLength - this.mContentLength) / 2);
        }

        private void initLayoutParameters() {
            if (this.mSpec.slotWidth != -1) {
                this.mSlotGap = 0;
                this.mSlotWidth = this.mSpec.slotWidth;
                this.mSlotHeight = this.mSpec.slotHeight;
            } else {
                int rows = this.mWidth > this.mHeight ? this.mSpec.rowsLand : this.mSpec.rowsPort;
                this.mSlotGap = this.mSpec.slotGap;
                this.mSlotHeight = Math.max(1, (this.mHeight - ((rows - 1) * this.mSlotGap)) / rows);
                this.mSlotWidth = this.mSlotHeight - this.mSpec.slotHeightAdditional;
            }
            if (this.this$0.mRenderer != null) {
                this.this$0.mRenderer.onSlotSizeChanged(this.mSlotWidth, this.mSlotHeight);
            }
            int[] padding = new int[2];
            initLayoutParameters(this.mWidth, this.mHeight, this.mSlotWidth, this.mSlotHeight, padding);
            this.mVerticalPadding.startAnimateTo(padding[0]);
            this.mHorizontalPadding.startAnimateTo(padding[1]);
            updateVisibleSlotRange();
        }

        public void setSize(int width, int height) {
            this.mWidth = width;
            this.mHeight = height;
            initLayoutParameters();
        }

        private void updateVisibleSlotRange() {
            int position = this.mScrollPosition;
            int startCol = position / (this.mSlotWidth + this.mSlotGap);
            int start = Math.max(0, this.mUnitCount * startCol);
            int endCol = ((((this.mWidth + position) + this.mSlotWidth) + this.mSlotGap) - 1) / (this.mSlotWidth + this.mSlotGap);
            int end = Math.min(this.mSlotCount, this.mUnitCount * endCol);
            setVisibleRange(start, end);
        }

        public void setScrollPosition(int position) {
            if (this.mScrollPosition != position) {
                this.mScrollPosition = position;
                updateVisibleSlotRange();
            }
        }

        private void setVisibleRange(int start, int end) {
            if (start != this.mVisibleStart || end != this.mVisibleEnd) {
                if (start < end) {
                    this.mVisibleStart = start;
                    this.mVisibleEnd = end;
                } else {
                    this.mVisibleEnd = 0;
                    this.mVisibleStart = 0;
                }
                if (this.this$0.mRenderer != null) {
                    this.this$0.mRenderer.onVisibleRangeChanged(this.mVisibleStart, this.mVisibleEnd);
                }
            }
        }

        public int getVisibleStart() {
            return this.mVisibleStart;
        }

        public int getVisibleEnd() {
            return this.mVisibleEnd;
        }

        public int getSlotIndexByPosition(float x, float y) {
            int absoluteX = Math.round(x) + this.mScrollPosition;
            int absoluteY = Math.round(y) + 0;
            int absoluteX2 = absoluteX - this.mHorizontalPadding.get();
            int absoluteY2 = absoluteY - this.mVerticalPadding.get();
            if (absoluteX2 < 0 || absoluteY2 < 0) {
                return -1;
            }
            int columnIdx = absoluteX2 / (this.mSlotWidth + this.mSlotGap);
            int rowIdx = absoluteY2 / (this.mSlotHeight + this.mSlotGap);
            if (rowIdx >= this.mUnitCount || absoluteX2 % (this.mSlotWidth + this.mSlotGap) >= this.mSlotWidth || absoluteY2 % (this.mSlotHeight + this.mSlotGap) >= this.mSlotHeight) {
                return -1;
            }
            int index = (this.mUnitCount * columnIdx) + rowIdx;
            if (index >= this.mSlotCount) {
                index = -1;
            }
            return index;
        }

        public int getScrollLimit() {
            int limit = this.mContentLength - this.mWidth;
            if (limit <= 0) {
                return 0;
            }
            return limit;
        }

        public boolean advanceAnimation(long animTime) {
            return this.mVerticalPadding.calculate(animTime) | this.mHorizontalPadding.calculate(animTime);
        }
    }

    private class MyGestureListener implements GestureDetector.OnGestureListener {
        private boolean isDown;

        private MyGestureListener() {
        }

        @Override
        public void onShowPress(MotionEvent e) {
            GLRoot root = SlotView.this.getGLRoot();
            root.lockRenderThread();
            try {
                if (!this.isDown) {
                    int index = SlotView.this.mLayout.getSlotIndexByPosition(e.getX(), e.getY());
                    if (index != -1) {
                        this.isDown = true;
                        SlotView.this.mListener.onDown(index);
                    }
                }
            } finally {
                root.unlockRenderThread();
            }
        }

        private void cancelDown(boolean byLongPress) {
            if (this.isDown) {
                this.isDown = false;
                SlotView.this.mListener.onUp(byLongPress);
            }
        }

        @Override
        public boolean onDown(MotionEvent e) {
            return false;
        }

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            cancelDown(false);
            int scrollLimit = SlotView.this.mLayout.getScrollLimit();
            if (scrollLimit == 0) {
                return false;
            }
            SlotView.this.mScroller.fling((int) (-velocityX), 0, scrollLimit);
            if (SlotView.this.mUIListener != null) {
                SlotView.this.mUIListener.onUserInteractionBegin();
            }
            SlotView.this.invalidate();
            return true;
        }

        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
            cancelDown(false);
            int overDistance = SlotView.this.mScroller.startScroll(Math.round(distanceX), 0, SlotView.this.mLayout.getScrollLimit());
            if (SlotView.this.mOverscrollEffect == 0 && overDistance != 0) {
                SlotView.this.mPaper.overScroll(overDistance);
            }
            SlotView.this.invalidate();
            return true;
        }

        @Override
        public boolean onSingleTapUp(MotionEvent e) {
            int index;
            cancelDown(false);
            if (!SlotView.this.mDownInScrolling && (index = SlotView.this.mLayout.getSlotIndexByPosition(e.getX(), e.getY())) != -1) {
                SlotView.this.mListener.onSingleTapUp(index);
            }
            return true;
        }

        @Override
        public void onLongPress(MotionEvent e) {
            cancelDown(true);
            if (!SlotView.this.mDownInScrolling) {
                SlotView.this.lockRendering();
                try {
                    int index = SlotView.this.mLayout.getSlotIndexByPosition(e.getX(), e.getY());
                    if (index != -1) {
                        SlotView.this.mListener.onLongTap(index);
                    }
                } finally {
                    SlotView.this.unlockRendering();
                }
            }
        }
    }

    public boolean setSlotCount(int slotCount) {
        boolean changed = this.mLayout.setSlotCount(slotCount);
        if (this.mStartIndex != -1) {
            setCenterIndex(this.mStartIndex);
            this.mStartIndex = -1;
        }
        setScrollPosition(this.mScrollX);
        return changed;
    }

    public int getVisibleStart() {
        return this.mLayout.getVisibleStart();
    }

    public int getVisibleEnd() {
        return this.mLayout.getVisibleEnd();
    }

    public int getScrollX() {
        return this.mScrollX;
    }

    public int getScrollY() {
        return this.mScrollY;
    }

    public Rect getSlotRect(int slotIndex, GLView rootPane) {
        Rect offset = new Rect();
        rootPane.getBoundsOf(this, offset);
        Rect r = getSlotRect(slotIndex);
        r.offset(offset.left - getScrollX(), offset.top - getScrollY());
        return r;
    }

    private static class IntegerAnimation extends Animation {
        private int mCurrent;
        private boolean mEnabled;
        private int mFrom;
        private int mTarget;

        private IntegerAnimation() {
            this.mCurrent = 0;
            this.mFrom = 0;
            this.mEnabled = false;
        }

        public void setEnabled(boolean enabled) {
            this.mEnabled = enabled;
        }

        public void startAnimateTo(int target) {
            if (!this.mEnabled) {
                this.mCurrent = target;
                this.mTarget = target;
            } else if (target != this.mTarget) {
                this.mFrom = this.mCurrent;
                this.mTarget = target;
                setDuration(180);
                start();
            }
        }

        public int get() {
            return this.mCurrent;
        }

        public int getTarget() {
            return this.mTarget;
        }

        @Override
        protected void onCalculate(float progress) {
            this.mCurrent = Math.round(this.mFrom + ((this.mTarget - this.mFrom) * progress));
            if (progress == 1.0f) {
                this.mEnabled = false;
            }
        }
    }
}
