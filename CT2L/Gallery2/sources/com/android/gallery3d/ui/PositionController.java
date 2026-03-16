package com.android.gallery3d.ui;

import android.content.Context;
import android.graphics.Rect;
import android.support.v4.app.FragmentManagerImpl;
import android.support.v4.app.NotificationCompat;
import android.widget.Scroller;
import com.android.gallery3d.common.Utils;
import com.android.gallery3d.ui.PhotoView;
import com.android.gallery3d.util.GalleryUtils;
import com.android.gallery3d.util.RangeArray;
import com.android.gallery3d.util.RangeIntArray;

class PositionController {
    private int mBoundBottom;
    private int mBoundLeft;
    private int mBoundRight;
    private int mBoundTop;
    private FilmRatio mFilmRatio;
    private Scroller mFilmScroller;
    private float mFocusX;
    private float mFocusY;
    private boolean mHasNext;
    private boolean mHasPrev;
    private boolean mInScale;
    private Listener mListener;
    private volatile Rect mOpenAnimationRect;
    private Platform mPlatform;
    boolean mPopFromTop;
    private static final int[] ANIM_TIME = {0, 0, 600, 400, 300, 300, 0, 0, 0, 700};
    private static final int[] CENTER_OUT_INDEX = new int[7];
    private static final int IMAGE_GAP = GalleryUtils.dpToPixel(16);
    private static final int HORIZONTAL_SLACK = GalleryUtils.dpToPixel(12);
    private boolean mExtraScalingRange = false;
    private boolean mFilmMode = false;
    private int mViewW = 1200;
    private int mViewH = 1200;
    private Rect mConstrainedFrame = new Rect();
    private boolean mConstrained = true;
    private RangeArray<Box> mBoxes = new RangeArray<>(-3, 3);
    private RangeArray<Gap> mGaps = new RangeArray<>(-3, 2);
    private RangeArray<Box> mTempBoxes = new RangeArray<>(-3, 3);
    private RangeArray<Gap> mTempGaps = new RangeArray<>(-3, 2);
    private RangeArray<Rect> mRects = new RangeArray<>(-3, 3);
    private FlingScroller mPageScroller = new FlingScroller();

    public interface Listener {
        void invalidate();

        boolean isHoldingDelete();

        boolean isHoldingDown();

        void onAbsorb(int i, int i2);

        void onPull(int i, int i2);
    }

    static {
        for (int i = 0; i < CENTER_OUT_INDEX.length; i++) {
            int j = (i + 1) / 2;
            if ((i & 1) == 0) {
                j = -j;
            }
            CENTER_OUT_INDEX[i] = j;
        }
    }

    public PositionController(Context context, Listener listener) {
        this.mPlatform = new Platform();
        this.mFilmRatio = new FilmRatio();
        this.mListener = listener;
        this.mFilmScroller = new Scroller(context, null, false);
        initPlatform();
        for (int i = -3; i <= 3; i++) {
            this.mBoxes.put(i, new Box());
            initBox(i);
            this.mRects.put(i, new Rect());
        }
        for (int i2 = -3; i2 < 3; i2++) {
            this.mGaps.put(i2, new Gap());
            initGap(i2);
        }
    }

    public void setViewSize(int viewW, int viewH) {
        if (viewW != this.mViewW || viewH != this.mViewH) {
            boolean wasMinimal = isAtMinimalScale();
            this.mViewW = viewW;
            this.mViewH = viewH;
            initPlatform();
            for (int i = -3; i <= 3; i++) {
                setBoxSize(i, viewW, viewH, true);
            }
            updateScaleAndGapLimit();
            if (wasMinimal) {
                Box b = this.mBoxes.get(0);
                b.mCurrentScale = b.mScaleMin;
            }
            if (!startOpeningAnimationIfNeeded()) {
                skipToFinalPosition();
            }
        }
    }

    public void setConstrainedFrame(Rect cFrame) {
        if (!this.mConstrainedFrame.equals(cFrame)) {
            this.mConstrainedFrame.set(cFrame);
            this.mPlatform.updateDefaultXY();
            updateScaleAndGapLimit();
            snapAndRedraw();
        }
    }

    public void forceImageSize(int index, PhotoView.Size s) {
        if (s.width != 0 && s.height != 0) {
            Box b = this.mBoxes.get(index);
            b.mImageW = s.width;
            b.mImageH = s.height;
        }
    }

    public void setImageSize(int index, PhotoView.Size s, Rect cFrame) {
        if (s.width != 0 && s.height != 0) {
            boolean needUpdate = false;
            if (cFrame != null && !this.mConstrainedFrame.equals(cFrame)) {
                this.mConstrainedFrame.set(cFrame);
                this.mPlatform.updateDefaultXY();
                needUpdate = true;
            }
            if (needUpdate | setBoxSize(index, s.width, s.height, false)) {
                updateScaleAndGapLimit();
                snapAndRedraw();
            }
        }
    }

    private boolean setBoxSize(int i, int width, int height, boolean isViewSize) {
        Box b = this.mBoxes.get(i);
        boolean wasViewSize = b.mUseViewSize;
        if (!wasViewSize && isViewSize) {
            return false;
        }
        b.mUseViewSize = isViewSize;
        if (width == b.mImageW && height == b.mImageH) {
            return false;
        }
        float ratio = width > height ? b.mImageW / width : b.mImageH / height;
        b.mImageW = width;
        b.mImageH = height;
        if ((wasViewSize && !isViewSize) || !this.mFilmMode) {
            b.mCurrentScale = getMinimalScale(b);
            b.mAnimationStartTime = -1L;
        } else {
            b.mCurrentScale *= ratio;
            b.mFromScale *= ratio;
            b.mToScale *= ratio;
        }
        if (i == 0) {
            this.mFocusX /= ratio;
            this.mFocusY /= ratio;
        }
        return true;
    }

    private boolean startOpeningAnimationIfNeeded() {
        if (this.mOpenAnimationRect == null) {
            return false;
        }
        Box b = this.mBoxes.get(0);
        if (b.mUseViewSize) {
            return false;
        }
        Rect r = this.mOpenAnimationRect;
        this.mOpenAnimationRect = null;
        this.mPlatform.mCurrentX = r.centerX() - (this.mViewW / 2);
        b.mCurrentY = r.centerY() - (this.mViewH / 2);
        b.mCurrentScale = Math.max(r.width() / b.mImageW, r.height() / b.mImageH);
        startAnimation(this.mPlatform.mDefaultX, 0, b.mScaleMin, 5);
        for (int i = -1; i < 1; i++) {
            Gap g = this.mGaps.get(i);
            g.mCurrentGap = this.mViewW;
            g.doAnimation(g.mDefaultSize, 5);
        }
        return true;
    }

    public void setFilmMode(boolean enabled) {
        if (enabled != this.mFilmMode) {
            this.mFilmMode = enabled;
            this.mPlatform.updateDefaultXY();
            updateScaleAndGapLimit();
            stopAnimation();
            snapAndRedraw();
        }
    }

    public void setExtraScalingRange(boolean enabled) {
        if (this.mExtraScalingRange != enabled) {
            this.mExtraScalingRange = enabled;
            if (!enabled) {
                snapAndRedraw();
            }
        }
    }

    private void updateScaleAndGapLimit() {
        for (int i = -3; i <= 3; i++) {
            Box b = this.mBoxes.get(i);
            b.mScaleMin = getMinimalScale(b);
            b.mScaleMax = getMaximalScale(b);
        }
        for (int i2 = -3; i2 < 3; i2++) {
            Gap g = this.mGaps.get(i2);
            g.mDefaultSize = getDefaultGapSize(i2);
        }
    }

    private int getDefaultGapSize(int i) {
        if (this.mFilmMode) {
            return IMAGE_GAP;
        }
        Box a = this.mBoxes.get(i);
        Box b = this.mBoxes.get(i + 1);
        return IMAGE_GAP + Math.max(gapToSide(a), gapToSide(b));
    }

    private int gapToSide(Box b) {
        return (int) (((this.mViewW - (getMinimalScale(b) * b.mImageW)) / 2.0f) + 0.5f);
    }

    public void stopAnimation() {
        this.mPlatform.mAnimationStartTime = -1L;
        for (int i = -3; i <= 3; i++) {
            this.mBoxes.get(i).mAnimationStartTime = -1L;
        }
        for (int i2 = -3; i2 < 3; i2++) {
            this.mGaps.get(i2).mAnimationStartTime = -1L;
        }
    }

    public void skipAnimation() {
        if (this.mPlatform.mAnimationStartTime != -1) {
            this.mPlatform.mCurrentX = this.mPlatform.mToX;
            this.mPlatform.mCurrentY = this.mPlatform.mToY;
            this.mPlatform.mAnimationStartTime = -1L;
        }
        for (int i = -3; i <= 3; i++) {
            Box b = this.mBoxes.get(i);
            if (b.mAnimationStartTime != -1) {
                b.mCurrentY = b.mToY;
                b.mCurrentScale = b.mToScale;
                b.mAnimationStartTime = -1L;
            }
        }
        for (int i2 = -3; i2 < 3; i2++) {
            Gap g = this.mGaps.get(i2);
            if (g.mAnimationStartTime != -1) {
                g.mCurrentGap = g.mToGap;
                g.mAnimationStartTime = -1L;
            }
        }
        redraw();
    }

    public void snapback() {
        snapAndRedraw();
    }

    public void skipToFinalPosition() {
        stopAnimation();
        snapAndRedraw();
        skipAnimation();
    }

    public void zoomIn(float tapX, float tapY, float targetScale) {
        Box b = this.mBoxes.get(0);
        float tempX = ((tapX - (this.mViewW / 2)) - this.mPlatform.mCurrentX) / b.mCurrentScale;
        float tempY = ((tapY - (this.mViewH / 2)) - b.mCurrentY) / b.mCurrentScale;
        int x = (int) (((-tempX) * targetScale) + 0.5f);
        int y = (int) (((-tempY) * targetScale) + 0.5f);
        calculateStableBound(targetScale);
        int targetX = Utils.clamp(x, this.mBoundLeft, this.mBoundRight);
        int targetY = Utils.clamp(y, this.mBoundTop, this.mBoundBottom);
        startAnimation(targetX, targetY, Utils.clamp(targetScale, b.mScaleMin, b.mScaleMax), 4);
    }

    public void resetToFullView() {
        Box b = this.mBoxes.get(0);
        startAnimation(this.mPlatform.mDefaultX, 0, b.mScaleMin, 4);
    }

    public void beginScale(float focusX, float focusY) {
        float focusX2 = focusX - (this.mViewW / 2);
        float focusY2 = focusY - (this.mViewH / 2);
        Box b = this.mBoxes.get(0);
        Platform p = this.mPlatform;
        this.mInScale = true;
        this.mFocusX = (int) (((focusX2 - p.mCurrentX) / b.mCurrentScale) + 0.5f);
        this.mFocusY = (int) (((focusY2 - b.mCurrentY) / b.mCurrentScale) + 0.5f);
    }

    public int scaleBy(float s, float focusX, float focusY) {
        float focusX2 = focusX - (this.mViewW / 2);
        float focusY2 = focusY - (this.mViewH / 2);
        Box b = this.mBoxes.get(0);
        Platform p = this.mPlatform;
        float s2 = b.clampScale(getTargetScale(b) * s);
        int x = this.mFilmMode ? p.mCurrentX : (int) ((focusX2 - (this.mFocusX * s2)) + 0.5f);
        int y = this.mFilmMode ? b.mCurrentY : (int) ((focusY2 - (this.mFocusY * s2)) + 0.5f);
        startAnimation(x, y, s2, 1);
        if (s2 < b.mScaleMin) {
            return -1;
        }
        return s2 <= b.mScaleMax ? 0 : 1;
    }

    public void endScale() {
        this.mInScale = false;
        snapAndRedraw();
    }

    public void startHorizontalSlide() {
        Box b = this.mBoxes.get(0);
        startAnimation(this.mPlatform.mDefaultX, 0, b.mScaleMin, 3);
    }

    public void startCaptureAnimationSlide(int offset) {
        Box b = this.mBoxes.get(0);
        Box n = this.mBoxes.get(offset);
        Gap g = this.mGaps.get(offset);
        this.mPlatform.doAnimation(this.mPlatform.mDefaultX, this.mPlatform.mDefaultY, 9);
        b.doAnimation(0, b.mScaleMin, 9);
        n.doAnimation(0, n.mScaleMin, 9);
        g.doAnimation(g.mDefaultSize, 9);
        redraw();
    }

    private boolean canScroll() {
        Box b = this.mBoxes.get(0);
        if (b.mAnimationStartTime == -1) {
            return true;
        }
        switch (b.mAnimationKind) {
            case 0:
            case FragmentManagerImpl.ANIM_STYLE_FADE_EXIT:
            case 7:
                break;
        }
        return true;
    }

    public void scrollPage(int dx, int dy) {
        if (canScroll()) {
            Box b = this.mBoxes.get(0);
            Platform p = this.mPlatform;
            calculateStableBound(b.mCurrentScale);
            int x = p.mCurrentX + dx;
            int y = b.mCurrentY + dy;
            if (this.mBoundTop != this.mBoundBottom) {
                if (y < this.mBoundTop) {
                    this.mListener.onPull(this.mBoundTop - y, 2);
                } else if (y > this.mBoundBottom) {
                    this.mListener.onPull(y - this.mBoundBottom, 0);
                }
            }
            int y2 = Utils.clamp(y, this.mBoundTop, this.mBoundBottom);
            if (!this.mHasPrev && x > this.mBoundRight) {
                int pixels = x - this.mBoundRight;
                this.mListener.onPull(pixels, 1);
                x = this.mBoundRight;
            } else if (!this.mHasNext && x < this.mBoundLeft) {
                int pixels2 = this.mBoundLeft - x;
                this.mListener.onPull(pixels2, 3);
                x = this.mBoundLeft;
            }
            startAnimation(x, y2, b.mCurrentScale, 0);
        }
    }

    public void scrollFilmX(int dx) {
        if (canScroll()) {
            Box b = this.mBoxes.get(0);
            Platform p = this.mPlatform;
            if (b.mAnimationStartTime != -1) {
                switch (b.mAnimationKind) {
                    case 0:
                    case FragmentManagerImpl.ANIM_STYLE_FADE_EXIT:
                    case 7:
                        break;
                }
            }
            int x = (p.mCurrentX + dx) - this.mPlatform.mDefaultX;
            if (!this.mHasPrev && x > 0) {
                this.mListener.onPull(x, 1);
                x = 0;
            } else if (!this.mHasNext && x < 0) {
                this.mListener.onPull(-x, 3);
                x = 0;
            }
            startAnimation(x + this.mPlatform.mDefaultX, b.mCurrentY, b.mCurrentScale, 0);
        }
    }

    public void scrollFilmY(int boxIndex, int dy) {
        if (canScroll()) {
            Box b = this.mBoxes.get(boxIndex);
            int y = b.mCurrentY + dy;
            b.doAnimation(y, b.mCurrentScale, 0);
            redraw();
        }
    }

    public boolean flingPage(int velocityX, int velocityY) {
        Box b = this.mBoxes.get(0);
        Platform p = this.mPlatform;
        if (viewWiderThanScaledImage(b.mCurrentScale) && viewTallerThanScaledImage(b.mCurrentScale)) {
            return false;
        }
        int edges = getImageAtEdges();
        if ((velocityX > 0 && (edges & 1) != 0) || (velocityX < 0 && (edges & 2) != 0)) {
            velocityX = 0;
        }
        if ((velocityY > 0 && (edges & 4) != 0) || (velocityY < 0 && (edges & 8) != 0)) {
            velocityY = 0;
        }
        if (velocityX == 0 && velocityY == 0) {
            return false;
        }
        this.mPageScroller.fling(p.mCurrentX, b.mCurrentY, velocityX, velocityY, this.mBoundLeft, this.mBoundRight, this.mBoundTop, this.mBoundBottom);
        int targetX = this.mPageScroller.getFinalX();
        int targetY = this.mPageScroller.getFinalY();
        ANIM_TIME[6] = this.mPageScroller.getDuration();
        return startAnimation(targetX, targetY, b.mCurrentScale, 6);
    }

    public boolean flingFilmX(int velocityX) {
        if (velocityX == 0) {
            return false;
        }
        Box b = this.mBoxes.get(0);
        Platform p = this.mPlatform;
        int defaultX = p.mDefaultX;
        if (!this.mHasPrev && p.mCurrentX >= defaultX) {
            return false;
        }
        if (!this.mHasNext && p.mCurrentX <= defaultX) {
            return false;
        }
        this.mFilmScroller.fling(p.mCurrentX, 0, velocityX, 0, Integer.MIN_VALUE, Integer.MAX_VALUE, 0, 0);
        int targetX = this.mFilmScroller.getFinalX();
        return startAnimation(targetX, b.mCurrentY, b.mCurrentScale, 7);
    }

    public int flingFilmY(int boxIndex, int velocityY) {
        int duration;
        Box b = this.mBoxes.get(boxIndex);
        int h = heightOf(b);
        int targetY = (velocityY < 0 || (velocityY == 0 && b.mCurrentY <= 0)) ? (((-this.mViewH) / 2) - ((h + 1) / 2)) - 3 : ((this.mViewH + 1) / 2) + (h / 2) + 3;
        if (velocityY != 0) {
            int duration2 = (int) ((Math.abs(targetY - b.mCurrentY) * 1000.0f) / Math.abs(velocityY));
            duration = Math.min(400, duration2);
        } else {
            duration = 200;
        }
        ANIM_TIME[8] = duration;
        if (b.doAnimation(targetY, b.mCurrentScale, 8)) {
            redraw();
            return duration;
        }
        return -1;
    }

    public int hitTest(int x, int y) {
        for (int i = 0; i < 7; i++) {
            int j = CENTER_OUT_INDEX[i];
            Rect r = this.mRects.get(j);
            if (r.contains(x, y)) {
                return j;
            }
        }
        return Integer.MAX_VALUE;
    }

    private void redraw() {
        layoutAndSetPosition();
        this.mListener.invalidate();
    }

    private void snapAndRedraw() {
        this.mPlatform.startSnapback();
        for (int i = -3; i <= 3; i++) {
            this.mBoxes.get(i).startSnapback();
        }
        for (int i2 = -3; i2 < 3; i2++) {
            this.mGaps.get(i2).startSnapback();
        }
        this.mFilmRatio.startSnapback();
        redraw();
    }

    private boolean startAnimation(int targetX, int targetY, float targetScale, int kind) {
        boolean changed = false | this.mPlatform.doAnimation(targetX, this.mPlatform.mDefaultY, kind) | this.mBoxes.get(0).doAnimation(targetY, targetScale, kind);
        if (changed) {
            redraw();
        }
        return changed;
    }

    public void advanceAnimation() {
        boolean changed = false | this.mPlatform.advanceAnimation();
        for (int i = -3; i <= 3; i++) {
            changed |= this.mBoxes.get(i).advanceAnimation();
        }
        for (int i2 = -3; i2 < 3; i2++) {
            changed |= this.mGaps.get(i2).advanceAnimation();
        }
        if (changed | this.mFilmRatio.advanceAnimation()) {
            redraw();
        }
    }

    public boolean inOpeningAnimation() {
        return (this.mPlatform.mAnimationKind == 5 && this.mPlatform.mAnimationStartTime != -1) || (this.mBoxes.get(0).mAnimationKind == 5 && this.mBoxes.get(0).mAnimationStartTime != -1);
    }

    private int widthOf(Box b) {
        return (int) ((b.mImageW * b.mCurrentScale) + 0.5f);
    }

    private int heightOf(Box b) {
        return (int) ((b.mImageH * b.mCurrentScale) + 0.5f);
    }

    private int widthOf(Box b, float scale) {
        return (int) ((b.mImageW * scale) + 0.5f);
    }

    private int heightOf(Box b, float scale) {
        return (int) ((b.mImageH * scale) + 0.5f);
    }

    private void layoutAndSetPosition() {
        for (int i = 0; i < 7; i++) {
            convertBoxToRect(CENTER_OUT_INDEX[i]);
        }
    }

    private void convertBoxToRect(int i) {
        Box b = this.mBoxes.get(i);
        Rect r = this.mRects.get(i);
        int y = b.mCurrentY + this.mPlatform.mCurrentY + (this.mViewH / 2);
        int w = widthOf(b);
        int h = heightOf(b);
        if (i == 0) {
            int x = this.mPlatform.mCurrentX + (this.mViewW / 2);
            r.left = x - (w / 2);
            r.right = r.left + w;
        } else if (i > 0) {
            Rect a = this.mRects.get(i - 1);
            Gap g = this.mGaps.get(i - 1);
            r.left = a.right + g.mCurrentGap;
            r.right = r.left + w;
        } else {
            Rect a2 = this.mRects.get(i + 1);
            Gap g2 = this.mGaps.get(i);
            r.right = a2.left - g2.mCurrentGap;
            r.left = r.right - w;
        }
        r.top = y - (h / 2);
        r.bottom = r.top + h;
    }

    public Rect getPosition(int index) {
        return this.mRects.get(index);
    }

    private void initPlatform() {
        this.mPlatform.updateDefaultXY();
        this.mPlatform.mCurrentX = this.mPlatform.mDefaultX;
        this.mPlatform.mCurrentY = this.mPlatform.mDefaultY;
        this.mPlatform.mAnimationStartTime = -1L;
    }

    private void initBox(int index) {
        Box b = this.mBoxes.get(index);
        b.mImageW = this.mViewW;
        b.mImageH = this.mViewH;
        b.mUseViewSize = true;
        b.mScaleMin = getMinimalScale(b);
        b.mScaleMax = getMaximalScale(b);
        b.mCurrentY = 0;
        b.mCurrentScale = b.mScaleMin;
        b.mAnimationStartTime = -1L;
        b.mAnimationKind = -1;
    }

    private void initBox(int index, PhotoView.Size size) {
        if (size.width == 0 || size.height == 0) {
            initBox(index);
            return;
        }
        Box b = this.mBoxes.get(index);
        b.mImageW = size.width;
        b.mImageH = size.height;
        b.mUseViewSize = false;
        b.mScaleMin = getMinimalScale(b);
        b.mScaleMax = getMaximalScale(b);
        b.mCurrentY = 0;
        b.mCurrentScale = b.mScaleMin;
        b.mAnimationStartTime = -1L;
        b.mAnimationKind = -1;
    }

    private void initGap(int index) {
        Gap g = this.mGaps.get(index);
        g.mDefaultSize = getDefaultGapSize(index);
        g.mCurrentGap = g.mDefaultSize;
        g.mAnimationStartTime = -1L;
    }

    private void initGap(int index, int size) {
        Gap g = this.mGaps.get(index);
        g.mDefaultSize = getDefaultGapSize(index);
        g.mCurrentGap = size;
        g.mAnimationStartTime = -1L;
    }

    public void moveBox(int[] fromIndex, boolean hasPrev, boolean hasNext, boolean constrained, PhotoView.Size[] sizes) {
        int k;
        this.mHasPrev = hasPrev;
        this.mHasNext = hasNext;
        RangeIntArray from = new RangeIntArray(fromIndex, -3, 3);
        layoutAndSetPosition();
        for (int i = -3; i <= 3; i++) {
            Box b = this.mBoxes.get(i);
            Rect r = this.mRects.get(i);
            b.mAbsoluteX = r.centerX() - (this.mViewW / 2);
        }
        for (int i2 = -3; i2 <= 3; i2++) {
            this.mTempBoxes.put(i2, this.mBoxes.get(i2));
            this.mBoxes.put(i2, null);
        }
        for (int i3 = -3; i3 < 3; i3++) {
            this.mTempGaps.put(i3, this.mGaps.get(i3));
            this.mGaps.put(i3, null);
        }
        for (int i4 = -3; i4 <= 3; i4++) {
            int j = from.get(i4);
            if (j != Integer.MAX_VALUE) {
                this.mBoxes.put(i4, this.mTempBoxes.get(j));
                this.mTempBoxes.put(j, null);
            }
        }
        for (int i5 = -3; i5 < 3; i5++) {
            int j2 = from.get(i5);
            if (j2 != Integer.MAX_VALUE && (k = from.get(i5 + 1)) != Integer.MAX_VALUE && j2 + 1 == k) {
                this.mGaps.put(i5, this.mTempGaps.get(j2));
                this.mTempGaps.put(j2, null);
            }
        }
        int k2 = -3;
        for (int i6 = -3; i6 <= 3; i6++) {
            if (this.mBoxes.get(i6) == null) {
                while (this.mTempBoxes.get(k2) == null) {
                    k2++;
                }
                this.mBoxes.put(i6, this.mTempBoxes.get(k2));
                initBox(i6, sizes[i6 + 3]);
                k2++;
            }
        }
        int first = -3;
        while (first <= 3 && from.get(first) == Integer.MAX_VALUE) {
            first++;
        }
        int last = 3;
        while (last >= -3 && from.get(last) == Integer.MAX_VALUE) {
            last--;
        }
        if (first > 3) {
            this.mBoxes.get(0).mAbsoluteX = this.mPlatform.mCurrentX;
            last = 0;
            first = 0;
        }
        for (int i7 = Math.max(0, first + 1); i7 < last; i7++) {
            if (from.get(i7) == Integer.MAX_VALUE) {
                Box a = this.mBoxes.get(i7 - 1);
                Box b2 = this.mBoxes.get(i7);
                int wa = widthOf(a);
                int wb = widthOf(b2);
                b2.mAbsoluteX = a.mAbsoluteX + (wa - (wa / 2)) + (wb / 2) + getDefaultGapSize(i7);
                if (this.mPopFromTop) {
                    b2.mCurrentY = -((this.mViewH / 2) + (heightOf(b2) / 2));
                } else {
                    b2.mCurrentY = (this.mViewH / 2) + (heightOf(b2) / 2);
                }
            }
        }
        for (int i8 = Math.min(-1, last - 1); i8 > first; i8--) {
            if (from.get(i8) == Integer.MAX_VALUE) {
                Box a2 = this.mBoxes.get(i8 + 1);
                Box b3 = this.mBoxes.get(i8);
                int wa2 = widthOf(a2);
                int wb2 = widthOf(b3);
                b3.mAbsoluteX = ((a2.mAbsoluteX - (wa2 / 2)) - (wb2 - (wb2 / 2))) - getDefaultGapSize(i8);
                if (this.mPopFromTop) {
                    b3.mCurrentY = -((this.mViewH / 2) + (heightOf(b3) / 2));
                } else {
                    b3.mCurrentY = (this.mViewH / 2) + (heightOf(b3) / 2);
                }
            }
        }
        int k3 = -3;
        for (int i9 = -3; i9 < 3; i9++) {
            if (this.mGaps.get(i9) == null) {
                while (this.mTempGaps.get(k3) == null) {
                    k3++;
                }
                int k4 = k3 + 1;
                this.mGaps.put(i9, this.mTempGaps.get(k3));
                Box a3 = this.mBoxes.get(i9);
                Box b4 = this.mBoxes.get(i9 + 1);
                int wa3 = widthOf(a3);
                int wb3 = widthOf(b4);
                if (i9 >= first && i9 < last) {
                    int g = ((b4.mAbsoluteX - a3.mAbsoluteX) - (wb3 / 2)) - (wa3 - (wa3 / 2));
                    initGap(i9, g);
                    k3 = k4;
                } else {
                    initGap(i9);
                    k3 = k4;
                }
            }
        }
        for (int i10 = first - 1; i10 >= -3; i10--) {
            Box a4 = this.mBoxes.get(i10 + 1);
            Box b5 = this.mBoxes.get(i10);
            int wa4 = widthOf(a4);
            int wb4 = widthOf(b5);
            Gap g2 = this.mGaps.get(i10);
            b5.mAbsoluteX = ((a4.mAbsoluteX - (wa4 / 2)) - (wb4 - (wb4 / 2))) - g2.mCurrentGap;
        }
        for (int i11 = last + 1; i11 <= 3; i11++) {
            Box a5 = this.mBoxes.get(i11 - 1);
            Box b6 = this.mBoxes.get(i11);
            int wa5 = widthOf(a5);
            int wb5 = widthOf(b6);
            Gap g3 = this.mGaps.get(i11 - 1);
            b6.mAbsoluteX = a5.mAbsoluteX + (wa5 - (wa5 / 2)) + (wb5 / 2) + g3.mCurrentGap;
        }
        int dx = this.mBoxes.get(0).mAbsoluteX - this.mPlatform.mCurrentX;
        this.mPlatform.mCurrentX += dx;
        this.mPlatform.mFromX += dx;
        this.mPlatform.mToX += dx;
        this.mPlatform.mFlingOffset += dx;
        if (this.mConstrained != constrained) {
            this.mConstrained = constrained;
            this.mPlatform.updateDefaultXY();
            updateScaleAndGapLimit();
        }
        snapAndRedraw();
    }

    public boolean isAtMinimalScale() {
        Box b = this.mBoxes.get(0);
        return isAlmostEqual(b.mCurrentScale, b.mScaleMin);
    }

    public boolean isCenter() {
        Box b = this.mBoxes.get(0);
        return this.mPlatform.mCurrentX == this.mPlatform.mDefaultX && b.mCurrentY == 0;
    }

    public int getImageWidth() {
        Box b = this.mBoxes.get(0);
        return b.mImageW;
    }

    public int getImageHeight() {
        Box b = this.mBoxes.get(0);
        return b.mImageH;
    }

    public float getImageScale() {
        Box b = this.mBoxes.get(0);
        return b.mCurrentScale;
    }

    public int getImageAtEdges() {
        Box b = this.mBoxes.get(0);
        Platform p = this.mPlatform;
        calculateStableBound(b.mCurrentScale);
        int edges = 0;
        if (p.mCurrentX <= this.mBoundLeft) {
            edges = 0 | 2;
        }
        if (p.mCurrentX >= this.mBoundRight) {
            edges |= 1;
        }
        if (b.mCurrentY <= this.mBoundTop) {
            edges |= 8;
        }
        if (b.mCurrentY >= this.mBoundBottom) {
            return edges | 4;
        }
        return edges;
    }

    public boolean isScrolling() {
        return (this.mPlatform.mAnimationStartTime == -1 || this.mPlatform.mCurrentX == this.mPlatform.mToX) ? false : true;
    }

    public void stopScrolling() {
        if (this.mPlatform.mAnimationStartTime != -1) {
            if (this.mFilmMode) {
                this.mFilmScroller.forceFinished(true);
            }
            Platform platform = this.mPlatform;
            Platform platform2 = this.mPlatform;
            int i = this.mPlatform.mCurrentX;
            platform2.mToX = i;
            platform.mFromX = i;
        }
    }

    public float getFilmRatio() {
        return this.mFilmRatio.mCurrentRatio;
    }

    public void setPopFromTop(boolean top) {
        this.mPopFromTop = top;
    }

    public boolean hasDeletingBox() {
        for (int i = -3; i <= 3; i++) {
            if (this.mBoxes.get(i).mAnimationKind == 8) {
                return true;
            }
        }
        return false;
    }

    private float getMinimalScale(Box b) {
        int viewW;
        int viewH;
        float wFactor = 1.0f;
        float hFactor = 1.0f;
        if (!this.mFilmMode && this.mConstrained && !this.mConstrainedFrame.isEmpty() && b == this.mBoxes.get(0)) {
            viewW = this.mConstrainedFrame.width();
            viewH = this.mConstrainedFrame.height();
        } else {
            viewW = this.mViewW;
            viewH = this.mViewH;
        }
        if (this.mFilmMode) {
            if (this.mViewH > this.mViewW) {
                wFactor = 0.7f;
                hFactor = 0.48f;
            } else {
                wFactor = 0.7f;
                hFactor = 0.7f;
            }
        }
        float s = Math.min((viewW * wFactor) / b.mImageW, (viewH * hFactor) / b.mImageH);
        return Math.min(4.0f, s);
    }

    private float getMaximalScale(Box b) {
        if (this.mFilmMode) {
            return getMinimalScale(b);
        }
        if (!this.mConstrained || this.mConstrainedFrame.isEmpty()) {
            return 4.0f;
        }
        return getMinimalScale(b);
    }

    private static boolean isAlmostEqual(float a, float b) {
        float diff = a - b;
        if (diff < 0.0f) {
            diff = -diff;
        }
        return diff < 0.02f;
    }

    private void calculateStableBound(float scale, int horizontalSlack) {
        Box b = this.mBoxes.get(0);
        int w = widthOf(b, scale);
        int h = heightOf(b, scale);
        this.mBoundLeft = (((this.mViewW + 1) / 2) - ((w + 1) / 2)) - horizontalSlack;
        this.mBoundRight = ((w / 2) - (this.mViewW / 2)) + horizontalSlack;
        this.mBoundTop = ((this.mViewH + 1) / 2) - ((h + 1) / 2);
        this.mBoundBottom = (h / 2) - (this.mViewH / 2);
        if (viewTallerThanScaledImage(scale)) {
            this.mBoundBottom = 0;
            this.mBoundTop = 0;
        }
        if (viewWiderThanScaledImage(scale)) {
            int i = this.mPlatform.mDefaultX;
            this.mBoundRight = i;
            this.mBoundLeft = i;
        }
    }

    private void calculateStableBound(float scale) {
        calculateStableBound(scale, 0);
    }

    private boolean viewTallerThanScaledImage(float scale) {
        return this.mViewH >= heightOf(this.mBoxes.get(0), scale);
    }

    private boolean viewWiderThanScaledImage(float scale) {
        return this.mViewW >= widthOf(this.mBoxes.get(0), scale);
    }

    private float getTargetScale(Box b) {
        return b.mAnimationStartTime == -1 ? b.mCurrentScale : b.mToScale;
    }

    private static abstract class Animatable {
        public int mAnimationDuration;
        public int mAnimationKind;
        public long mAnimationStartTime;

        protected abstract boolean interpolate(float f);

        public abstract boolean startSnapback();

        private Animatable() {
        }

        public boolean advanceAnimation() {
            float progress;
            float progress2;
            if (this.mAnimationStartTime == -1) {
                return false;
            }
            if (this.mAnimationStartTime == -2) {
                this.mAnimationStartTime = -1L;
                return startSnapback();
            }
            if (this.mAnimationDuration == 0) {
                progress = 1.0f;
            } else {
                long now = AnimationTime.get();
                progress = (now - this.mAnimationStartTime) / this.mAnimationDuration;
            }
            if (progress >= 1.0f) {
                progress2 = 1.0f;
            } else {
                progress2 = applyInterpolationCurve(this.mAnimationKind, progress);
            }
            boolean done = interpolate(progress2);
            if (done) {
                this.mAnimationStartTime = -2L;
            }
            return true;
        }

        private static float applyInterpolationCurve(int kind, float progress) {
            float f = 1.0f - progress;
            switch (kind) {
                case 0:
                case FragmentManagerImpl.ANIM_STYLE_FADE_EXIT:
                case 7:
                case NotificationCompat.FLAG_ONLY_ALERT_ONCE:
                case 9:
                    return 1.0f - f;
                case 1:
                case 5:
                    return 1.0f - (f * f);
                case 2:
                case 3:
                case 4:
                    return 1.0f - ((((f * f) * f) * f) * f);
                default:
                    return progress;
            }
        }
    }

    private class Platform extends Animatable {
        public int mCurrentX;
        public int mCurrentY;
        public int mDefaultX;
        public int mDefaultY;
        public int mFlingOffset;
        public int mFromX;
        public int mFromY;
        public int mToX;
        public int mToY;

        private Platform() {
            super();
        }

        @Override
        public boolean startSnapback() {
            int x;
            if (this.mAnimationStartTime != -1) {
                return false;
            }
            if ((this.mAnimationKind == 0 && PositionController.this.mListener.isHoldingDown()) || PositionController.this.mInScale) {
                return false;
            }
            Box b = (Box) PositionController.this.mBoxes.get(0);
            float scaleMin = PositionController.this.mExtraScalingRange ? b.mScaleMin * 0.7f : b.mScaleMin;
            float scaleMax = PositionController.this.mExtraScalingRange ? b.mScaleMax * 1.4f : b.mScaleMax;
            float scale = Utils.clamp(b.mCurrentScale, scaleMin, scaleMax);
            int x2 = this.mCurrentX;
            int y = this.mDefaultY;
            if (!PositionController.this.mFilmMode) {
                PositionController.this.calculateStableBound(scale, PositionController.HORIZONTAL_SLACK);
                if (!PositionController.this.viewWiderThanScaledImage(scale)) {
                    float scaleDiff = b.mCurrentScale - scale;
                    x2 += (int) ((PositionController.this.mFocusX * scaleDiff) + 0.5f);
                }
                x = Utils.clamp(x2, PositionController.this.mBoundLeft, PositionController.this.mBoundRight);
            } else {
                x = this.mDefaultX;
            }
            if (this.mCurrentX == x && this.mCurrentY == y) {
                return false;
            }
            return doAnimation(x, y, 2);
        }

        public void updateDefaultXY() {
            if (PositionController.this.mConstrained && !PositionController.this.mConstrainedFrame.isEmpty()) {
                this.mDefaultX = PositionController.this.mConstrainedFrame.centerX() - (PositionController.this.mViewW / 2);
                this.mDefaultY = PositionController.this.mFilmMode ? 0 : PositionController.this.mConstrainedFrame.centerY() - (PositionController.this.mViewH / 2);
            } else {
                this.mDefaultX = 0;
                this.mDefaultY = 0;
            }
        }

        private boolean doAnimation(int targetX, int targetY, int kind) {
            if (this.mCurrentX == targetX && this.mCurrentY == targetY) {
                return false;
            }
            this.mAnimationKind = kind;
            this.mFromX = this.mCurrentX;
            this.mFromY = this.mCurrentY;
            this.mToX = targetX;
            this.mToY = targetY;
            this.mAnimationStartTime = AnimationTime.startTime();
            this.mAnimationDuration = PositionController.ANIM_TIME[kind];
            this.mFlingOffset = 0;
            advanceAnimation();
            return true;
        }

        @Override
        protected boolean interpolate(float progress) {
            if (this.mAnimationKind == 6) {
                return interpolateFlingPage(progress);
            }
            if (this.mAnimationKind == 7) {
                return interpolateFlingFilm(progress);
            }
            return interpolateLinear(progress);
        }

        private boolean interpolateFlingFilm(float progress) {
            PositionController.this.mFilmScroller.computeScrollOffset();
            this.mCurrentX = PositionController.this.mFilmScroller.getCurrX() + this.mFlingOffset;
            int dir = -1;
            if (this.mCurrentX < this.mDefaultX) {
                if (!PositionController.this.mHasNext) {
                    dir = 3;
                }
            } else if (this.mCurrentX > this.mDefaultX && !PositionController.this.mHasPrev) {
                dir = 1;
            }
            if (dir != -1) {
                PositionController.this.mFilmScroller.forceFinished(true);
                this.mCurrentX = this.mDefaultX;
            }
            return PositionController.this.mFilmScroller.isFinished();
        }

        private boolean interpolateFlingPage(float progress) {
            PositionController.this.mPageScroller.computeScrollOffset(progress);
            Box b = (Box) PositionController.this.mBoxes.get(0);
            PositionController.this.calculateStableBound(b.mCurrentScale);
            int oldX = this.mCurrentX;
            this.mCurrentX = PositionController.this.mPageScroller.getCurrX();
            if (oldX <= PositionController.this.mBoundLeft || this.mCurrentX != PositionController.this.mBoundLeft) {
                if (oldX < PositionController.this.mBoundRight && this.mCurrentX == PositionController.this.mBoundRight) {
                    int v = (int) (PositionController.this.mPageScroller.getCurrVelocityX() + 0.5f);
                    PositionController.this.mListener.onAbsorb(v, 1);
                }
            } else {
                int v2 = (int) ((-PositionController.this.mPageScroller.getCurrVelocityX()) + 0.5f);
                PositionController.this.mListener.onAbsorb(v2, 3);
            }
            return progress >= 1.0f;
        }

        private boolean interpolateLinear(float progress) {
            if (progress >= 1.0f) {
                this.mCurrentX = this.mToX;
                this.mCurrentY = this.mToY;
                return true;
            }
            if (this.mAnimationKind == 9) {
                progress = CaptureAnimation.calculateSlide(progress);
            }
            this.mCurrentX = (int) (this.mFromX + ((this.mToX - this.mFromX) * progress));
            this.mCurrentY = (int) (this.mFromY + ((this.mToY - this.mFromY) * progress));
            if (this.mAnimationKind == 9) {
                return false;
            }
            return this.mCurrentX == this.mToX && this.mCurrentY == this.mToY;
        }
    }

    private class Box extends Animatable {
        public int mAbsoluteX;
        public float mCurrentScale;
        public int mCurrentY;
        public float mFromScale;
        public int mFromY;
        public int mImageH;
        public int mImageW;
        public float mScaleMax;
        public float mScaleMin;
        public float mToScale;
        public int mToY;
        public boolean mUseViewSize;

        private Box() {
            super();
        }

        @Override
        public boolean startSnapback() {
            int y;
            float scale;
            if (this.mAnimationStartTime != -1) {
                return false;
            }
            if (this.mAnimationKind == 0 && PositionController.this.mListener.isHoldingDown()) {
                return false;
            }
            if (this.mAnimationKind == 8 && PositionController.this.mListener.isHoldingDelete()) {
                return false;
            }
            if (PositionController.this.mInScale && this == PositionController.this.mBoxes.get(0)) {
                return false;
            }
            int y2 = this.mCurrentY;
            if (this == PositionController.this.mBoxes.get(0)) {
                float scaleMin = PositionController.this.mExtraScalingRange ? this.mScaleMin * 0.7f : this.mScaleMin;
                float scaleMax = PositionController.this.mExtraScalingRange ? this.mScaleMax * 1.4f : this.mScaleMax;
                scale = Utils.clamp(this.mCurrentScale, scaleMin, scaleMax);
                if (!PositionController.this.mFilmMode) {
                    PositionController.this.calculateStableBound(scale, PositionController.HORIZONTAL_SLACK);
                    if (!PositionController.this.viewTallerThanScaledImage(scale)) {
                        float scaleDiff = this.mCurrentScale - scale;
                        y2 += (int) ((PositionController.this.mFocusY * scaleDiff) + 0.5f);
                    }
                    y = Utils.clamp(y2, PositionController.this.mBoundTop, PositionController.this.mBoundBottom);
                } else {
                    y = 0;
                }
            } else {
                y = 0;
                scale = this.mScaleMin;
            }
            if (this.mCurrentY == y && this.mCurrentScale == scale) {
                return false;
            }
            return doAnimation(y, scale, 2);
        }

        private boolean doAnimation(int targetY, float targetScale, int kind) {
            float targetScale2 = clampScale(targetScale);
            if (this.mCurrentY == targetY && this.mCurrentScale == targetScale2 && kind != 9) {
                return false;
            }
            this.mAnimationKind = kind;
            this.mFromY = this.mCurrentY;
            this.mFromScale = this.mCurrentScale;
            this.mToY = targetY;
            this.mToScale = targetScale2;
            this.mAnimationStartTime = AnimationTime.startTime();
            this.mAnimationDuration = PositionController.ANIM_TIME[kind];
            advanceAnimation();
            return true;
        }

        public float clampScale(float s) {
            return Utils.clamp(s, 0.7f * this.mScaleMin, 1.4f * this.mScaleMax);
        }

        @Override
        protected boolean interpolate(float progress) {
            return this.mAnimationKind == 6 ? interpolateFlingPage(progress) : interpolateLinear(progress);
        }

        private boolean interpolateFlingPage(float progress) {
            PositionController.this.mPageScroller.computeScrollOffset(progress);
            PositionController.this.calculateStableBound(this.mCurrentScale);
            int oldY = this.mCurrentY;
            this.mCurrentY = PositionController.this.mPageScroller.getCurrY();
            if (oldY <= PositionController.this.mBoundTop || this.mCurrentY != PositionController.this.mBoundTop) {
                if (oldY < PositionController.this.mBoundBottom && this.mCurrentY == PositionController.this.mBoundBottom) {
                    int v = (int) (PositionController.this.mPageScroller.getCurrVelocityY() + 0.5f);
                    PositionController.this.mListener.onAbsorb(v, 0);
                }
            } else {
                int v2 = (int) ((-PositionController.this.mPageScroller.getCurrVelocityY()) + 0.5f);
                PositionController.this.mListener.onAbsorb(v2, 2);
            }
            return progress >= 1.0f;
        }

        private boolean interpolateLinear(float progress) {
            if (progress >= 1.0f) {
                this.mCurrentY = this.mToY;
                this.mCurrentScale = this.mToScale;
                return true;
            }
            this.mCurrentY = (int) (this.mFromY + ((this.mToY - this.mFromY) * progress));
            this.mCurrentScale = this.mFromScale + ((this.mToScale - this.mFromScale) * progress);
            if (this.mAnimationKind != 9) {
                return this.mCurrentY == this.mToY && this.mCurrentScale == this.mToScale;
            }
            float f = CaptureAnimation.calculateScale(progress);
            this.mCurrentScale *= f;
            return false;
        }
    }

    private class Gap extends Animatable {
        public int mCurrentGap;
        public int mDefaultSize;
        public int mFromGap;
        public int mToGap;

        private Gap() {
            super();
        }

        @Override
        public boolean startSnapback() {
            if (this.mAnimationStartTime != -1) {
                return false;
            }
            return doAnimation(this.mDefaultSize, 2);
        }

        public boolean doAnimation(int targetSize, int kind) {
            if (this.mCurrentGap == targetSize && kind != 9) {
                return false;
            }
            this.mAnimationKind = kind;
            this.mFromGap = this.mCurrentGap;
            this.mToGap = targetSize;
            this.mAnimationStartTime = AnimationTime.startTime();
            this.mAnimationDuration = PositionController.ANIM_TIME[this.mAnimationKind];
            advanceAnimation();
            return true;
        }

        @Override
        protected boolean interpolate(float progress) {
            if (progress >= 1.0f) {
                this.mCurrentGap = this.mToGap;
                return true;
            }
            this.mCurrentGap = (int) (this.mFromGap + ((this.mToGap - this.mFromGap) * progress));
            if (this.mAnimationKind != 9) {
                return this.mCurrentGap == this.mToGap;
            }
            float f = CaptureAnimation.calculateScale(progress);
            this.mCurrentGap = (int) (this.mCurrentGap * f);
            return false;
        }
    }

    private class FilmRatio extends Animatable {
        public float mCurrentRatio;
        public float mFromRatio;
        public float mToRatio;

        private FilmRatio() {
            super();
        }

        @Override
        public boolean startSnapback() {
            float target = PositionController.this.mFilmMode ? 1.0f : 0.0f;
            if (target == this.mToRatio) {
                return false;
            }
            return doAnimation(target, 2);
        }

        private boolean doAnimation(float targetRatio, int kind) {
            this.mAnimationKind = kind;
            this.mFromRatio = this.mCurrentRatio;
            this.mToRatio = targetRatio;
            this.mAnimationStartTime = AnimationTime.startTime();
            this.mAnimationDuration = PositionController.ANIM_TIME[this.mAnimationKind];
            advanceAnimation();
            return true;
        }

        @Override
        protected boolean interpolate(float progress) {
            if (progress >= 1.0f) {
                this.mCurrentRatio = this.mToRatio;
                return true;
            }
            this.mCurrentRatio = this.mFromRatio + ((this.mToRatio - this.mFromRatio) * progress);
            return this.mCurrentRatio == this.mToRatio;
        }
    }
}
