package com.android.gallery3d.ui;

import android.content.Context;
import android.graphics.Rect;
import android.widget.Scroller;
import com.android.gallery3d.common.Utils;
import com.android.gallery3d.ui.PhotoView;
import com.android.gallery3d.util.GalleryUtils;
import com.android.gallery3d.util.RangeArray;
import com.android.gallery3d.util.RangeIntArray;
import com.mediatek.gallerybasic.base.MediaData;
import mf.org.apache.xerces.dom3.as.ASContentModel;

public class PositionController {
    private static final int ANIM_KIND_CAPTURE = 9;
    private static final int ANIM_KIND_DELETE = 8;
    private static final int ANIM_KIND_FLING = 6;
    private static final int ANIM_KIND_FLING_X = 7;
    private static final int ANIM_KIND_NONE = -1;
    private static final int ANIM_KIND_OPENING = 5;
    private static final int ANIM_KIND_SCALE = 1;
    private static final int ANIM_KIND_SCROLL = 0;
    private static final int ANIM_KIND_SLIDE = 3;
    private static final int ANIM_KIND_SNAPBACK = 2;
    private static final int ANIM_KIND_ZOOM = 4;
    private static final int BOX_MAX = 3;
    private static final int DEFAULT_DELETE_ANIMATION_DURATION = 200;
    private static final float FILM_MODE_LANDSCAPE_HEIGHT = 0.7f;
    private static final float FILM_MODE_LANDSCAPE_WIDTH = 0.7f;
    private static final float FILM_MODE_PORTRAIT_HEIGHT = 0.48f;
    private static final float FILM_MODE_PORTRAIT_WIDTH = 0.7f;
    public static final int IMAGE_AT_BOTTOM_EDGE = 8;
    public static final int IMAGE_AT_LEFT_EDGE = 1;
    public static final int IMAGE_AT_RIGHT_EDGE = 2;
    public static final int IMAGE_AT_TOP_EDGE = 4;
    private static final long LAST_ANIMATION = -2;
    private static final long NO_ANIMATION = -1;
    public static final float SCALE_LIMIT = 4.0f;
    private static final float SCALE_MAX_EXTRA = 1.4f;
    private static final float SCALE_MIN_EXTRA = 0.7f;
    private static final String TAG = "Gallery2/PositionController";
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
    public static final int SNAPBACK_ANIMATION_TIME = 600;
    private static final int MAX_DELETE_ANIMATION_DURATION = 400;
    public static final int CAPTURE_ANIMATION_TIME = 700;
    private static final int[] ANIM_TIME = {0, 0, SNAPBACK_ANIMATION_TIME, MAX_DELETE_ANIMATION_DURATION, 300, 300, 0, 0, 0, CAPTURE_ANIMATION_TIME};
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

        void onRelease();
    }

    static {
        int i = 0;
        while (i < CENTER_OUT_INDEX.length) {
            int i2 = i + 1;
            int i3 = i2 / 2;
            if ((i & 1) == 0) {
                i3 = -i3;
            }
            CENTER_OUT_INDEX[i] = i3;
            i = i2;
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

    public void setOpenAnimationRect(Rect rect) {
        this.mOpenAnimationRect = rect;
    }

    public void setViewSize(int i, int i2) {
        if (i == this.mViewW && i2 == this.mViewH) {
            return;
        }
        boolean zIsAtMinimalScale = isAtMinimalScale();
        this.mViewW = i;
        this.mViewH = i2;
        initPlatform();
        for (int i3 = -3; i3 <= 3; i3++) {
            setBoxSize(i3, i, i2, true);
        }
        updateScaleAndGapLimit();
        if (zIsAtMinimalScale) {
            Box box = this.mBoxes.get(0);
            box.mCurrentScale = box.mScaleMin;
        }
        if (!startOpeningAnimationIfNeeded()) {
            skipToFinalPosition();
        }
    }

    public void setConstrainedFrame(Rect rect) {
        if (this.mConstrainedFrame.equals(rect)) {
            return;
        }
        this.mConstrainedFrame.set(rect);
        this.mPlatform.updateDefaultXY();
        updateScaleAndGapLimit();
        snapAndRedraw();
    }

    public void forceImageSize(int i, PhotoView.Size size) {
        if (size.width == 0 || size.height == 0) {
            return;
        }
        Box box = this.mBoxes.get(i);
        box.mImageW = size.width;
        box.mImageH = size.height;
    }

    public void setImageSize(int i, PhotoView.Size size, Rect rect, boolean z) {
        boolean z2;
        if (size.width == 0 || size.height == 0) {
            return;
        }
        boolean z3 = true;
        if (rect == null || this.mConstrainedFrame.equals(rect)) {
            z2 = false;
            z3 = false;
        } else {
            this.mConstrainedFrame.set(rect);
            this.mPlatform.updateDefaultXY();
            z2 = i == 0;
        }
        if (!setBoxSize(i, size.width, size.height, false) && !z3) {
            return;
        }
        updateScaleAndGapLimit();
        snapAndRedraw();
        if (z2 || z) {
            skipToFinalPosition();
        }
    }

    private boolean setBoxSize(int i, int i2, int i3, boolean z) {
        float f;
        Box box = this.mBoxes.get(i);
        boolean z2 = box.mUseViewSize;
        if (!z2 && z) {
            return false;
        }
        box.mUseViewSize = z;
        if (i2 == box.mImageW && i3 == box.mImageH) {
            return false;
        }
        if (i2 > i3) {
            f = box.mImageW / i2;
        } else {
            f = box.mImageH / i3;
        }
        box.mImageW = i2;
        box.mImageH = i3;
        if ((z2 && !z) || !this.mFilmMode) {
            box.mCurrentScale = getMinimalScale(box);
            box.mAnimationStartTime = -1L;
        } else {
            box.mCurrentScale *= f;
            box.mFromScale *= f;
            box.mToScale *= f;
        }
        if (i == 0) {
            this.mFocusX /= f;
            this.mFocusY /= f;
            return true;
        }
        return true;
    }

    private boolean startOpeningAnimationIfNeeded() {
        if (this.mOpenAnimationRect == null) {
            return false;
        }
        Box box = this.mBoxes.get(0);
        if (box.mUseViewSize) {
            return false;
        }
        Rect rect = this.mOpenAnimationRect;
        this.mOpenAnimationRect = null;
        this.mPlatform.mCurrentX = rect.centerX() - (this.mViewW / 2);
        box.mCurrentY = rect.centerY() - (this.mViewH / 2);
        box.mCurrentScale = Math.max(rect.width() / box.mImageW, rect.height() / box.mImageH);
        startAnimation(this.mPlatform.mDefaultX, 0, box.mScaleMin, 5);
        for (int i = -1; i < 1; i++) {
            Gap gap = this.mGaps.get(i);
            gap.mCurrentGap = this.mViewW;
            gap.doAnimation(gap.mDefaultSize, 5);
        }
        return true;
    }

    public void setFilmMode(boolean z) {
        if (z == this.mFilmMode) {
            return;
        }
        this.mFilmMode = z;
        this.mPlatform.updateDefaultXY();
        updateScaleAndGapLimit();
        stopAnimation();
        snapAndRedraw();
    }

    public void setExtraScalingRange(boolean z) {
        if (this.mExtraScalingRange == z) {
            return;
        }
        this.mExtraScalingRange = z;
        if (!z) {
            snapAndRedraw();
        }
    }

    private void updateScaleAndGapLimit() {
        for (int i = -3; i <= 3; i++) {
            Box box = this.mBoxes.get(i);
            box.mScaleMin = getMinimalScale(box);
            box.mScaleMax = getMaximalScale(box);
        }
        for (int i2 = -3; i2 < 3; i2++) {
            this.mGaps.get(i2).mDefaultSize = getDefaultGapSize(i2);
        }
    }

    private int getDefaultGapSize(int i) {
        if (this.mFilmMode) {
            return IMAGE_GAP;
        }
        return IMAGE_GAP + Math.max(gapToSide(this.mBoxes.get(i)), gapToSide(this.mBoxes.get(i + 1)));
    }

    private int gapToSide(Box box) {
        return (int) (((this.mViewW - (getMinimalScale(box) * box.mImageW)) / 2.0f) + 0.5f);
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
            Box box = this.mBoxes.get(i);
            if (box.mAnimationStartTime != -1) {
                box.mCurrentY = box.mToY;
                box.mCurrentScale = box.mToScale;
                box.mAnimationStartTime = -1L;
            }
        }
        for (int i2 = -3; i2 < 3; i2++) {
            Gap gap = this.mGaps.get(i2);
            if (gap.mAnimationStartTime != -1) {
                gap.mCurrentGap = gap.mToGap;
                gap.mAnimationStartTime = -1L;
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

    public void zoomIn(float f, float f2, float f3) {
        Box box = this.mBoxes.get(0);
        float f4 = ((f - (this.mViewW / 2)) - this.mPlatform.mCurrentX) / box.mCurrentScale;
        int i = (int) (((-(((f2 - (this.mViewH / 2)) - box.mCurrentY) / box.mCurrentScale)) * f3) + 0.5f);
        calculateStableBound(f3);
        startAnimation(Utils.clamp((int) (((-f4) * f3) + 0.5f), this.mBoundLeft, this.mBoundRight), Utils.clamp(i, this.mBoundTop, this.mBoundBottom), Utils.clamp(f3, box.mScaleMin, box.mScaleMax), 4);
    }

    public void resetToFullView() {
        startAnimation(this.mPlatform.mDefaultX, 0, this.mBoxes.get(0).mScaleMin, 4);
    }

    public void beginScale(float f, float f2) {
        float f3 = f - (this.mViewW / 2);
        float f4 = f2 - (this.mViewH / 2);
        Box box = this.mBoxes.get(0);
        Platform platform = this.mPlatform;
        this.mInScale = true;
        this.mFocusX = (int) (((f3 - platform.mCurrentX) / box.mCurrentScale) + 0.5f);
        this.mFocusY = (int) (((f4 - box.mCurrentY) / box.mCurrentScale) + 0.5f);
    }

    public int scaleBy(float f, float f2, float f3) {
        float f4 = f2 - (this.mViewW / 2);
        float f5 = f3 - (this.mViewH / 2);
        Box box = this.mBoxes.get(0);
        Platform platform = this.mPlatform;
        float fClampScale = box.clampScale(f * getTargetScale(box));
        startAnimation(this.mFilmMode ? platform.mCurrentX : (int) ((f4 - (this.mFocusX * fClampScale)) + 0.5f), this.mFilmMode ? box.mCurrentY : (int) ((f5 - (this.mFocusY * fClampScale)) + 0.5f), fClampScale, 1);
        if (fClampScale < box.mScaleMin) {
            return -1;
        }
        return fClampScale > box.mScaleMax ? 1 : 0;
    }

    public void endScale() {
        this.mInScale = false;
        snapAndRedraw();
    }

    public void startHorizontalSlide() {
        startAnimation(this.mPlatform.mDefaultX, 0, this.mBoxes.get(0).mScaleMin, 3);
    }

    public void startCaptureAnimationSlide(int i) {
        Box box = this.mBoxes.get(0);
        Box box2 = this.mBoxes.get(i);
        Gap gap = this.mGaps.get(i);
        this.mPlatform.doAnimation(this.mPlatform.mDefaultX, this.mPlatform.mDefaultY, 9);
        box.doAnimation(0, box.mScaleMin, 9);
        box2.doAnimation(0, box2.mScaleMin, 9);
        gap.doAnimation(gap.mDefaultSize, 9);
        redraw();
    }

    private boolean canScroll() {
        int i;
        Box box = this.mBoxes.get(0);
        if (box.mAnimationStartTime != -1 && (i = box.mAnimationKind) != 0) {
            switch (i) {
                case 6:
                case 7:
                    break;
                default:
                    return false;
            }
        }
        return true;
    }

    public void scrollPage(int i, int i2) {
        if (canScroll()) {
            Box box = this.mBoxes.get(0);
            Platform platform = this.mPlatform;
            calculateStableBound(box.mCurrentScale);
            int i3 = platform.mCurrentX + i;
            int i4 = box.mCurrentY + i2;
            if (this.mBoundTop != this.mBoundBottom) {
                if (i4 < this.mBoundTop) {
                    this.mListener.onPull(this.mBoundTop - i4, 2);
                } else if (i4 > this.mBoundBottom) {
                    this.mListener.onPull(i4 - this.mBoundBottom, 0);
                }
            }
            int iClamp = Utils.clamp(i4, this.mBoundTop, this.mBoundBottom);
            if (!this.mHasPrev && i3 > this.mBoundRight) {
                this.mListener.onPull(i3 - this.mBoundRight, 1);
                i3 = this.mBoundRight;
            } else if (!this.mHasNext && i3 < this.mBoundLeft) {
                this.mListener.onPull(this.mBoundLeft - i3, 3);
                i3 = this.mBoundLeft;
            }
            startAnimation(i3, iClamp, box.mCurrentScale, 0);
        }
    }

    public void scrollFilmX(int i) {
        int i2;
        if (canScroll()) {
            Box box = this.mBoxes.get(0);
            Platform platform = this.mPlatform;
            if (box.mAnimationStartTime != -1 && (i2 = box.mAnimationKind) != 0) {
                switch (i2) {
                }
            }
            int i3 = (platform.mCurrentX + i) - this.mPlatform.mDefaultX;
            if (!this.mHasPrev && i3 > 0) {
                this.mListener.onPull(i3, 1);
            } else {
                if (!this.mHasNext && i3 < 0) {
                    this.mListener.onPull(-i3, 3);
                }
                startAnimation(i3 + this.mPlatform.mDefaultX, box.mCurrentY, box.mCurrentScale, 0);
            }
            i3 = 0;
            startAnimation(i3 + this.mPlatform.mDefaultX, box.mCurrentY, box.mCurrentScale, 0);
        }
    }

    public void scrollFilmY(int i, int i2) {
        if (canScroll()) {
            Box box = this.mBoxes.get(i);
            box.doAnimation(box.mCurrentY + i2, box.mCurrentScale, 0);
            redraw();
        }
    }

    public boolean flingPage(int i, int i2) {
        Box box = this.mBoxes.get(0);
        Platform platform = this.mPlatform;
        if (viewWiderThanScaledImage(box.mCurrentScale) && viewTallerThanScaledImage(box.mCurrentScale)) {
            return false;
        }
        int imageAtEdges = getImageAtEdges();
        int i3 = ((i <= 0 || (imageAtEdges & 1) == 0) && (i >= 0 || (imageAtEdges & 2) == 0)) ? i : 0;
        int i4 = ((i2 <= 0 || (imageAtEdges & 4) == 0) && (i2 >= 0 || (imageAtEdges & 8) == 0)) ? i2 : 0;
        if (i3 == 0 && i4 == 0) {
            return false;
        }
        this.mPageScroller.fling(platform.mCurrentX, box.mCurrentY, i3, i4, this.mBoundLeft, this.mBoundRight, this.mBoundTop, this.mBoundBottom);
        int finalX = this.mPageScroller.getFinalX();
        int finalY = this.mPageScroller.getFinalY();
        ANIM_TIME[6] = this.mPageScroller.getDuration();
        return startAnimation(finalX, finalY, box.mCurrentScale, 6);
    }

    public boolean flingFilmX(int i) {
        if (i == 0) {
            return false;
        }
        Box box = this.mBoxes.get(0);
        Platform platform = this.mPlatform;
        int i2 = platform.mDefaultX;
        if ((!this.mHasPrev && platform.mCurrentX >= i2) || (!this.mHasNext && platform.mCurrentX <= i2)) {
            return false;
        }
        this.mFilmScroller.fling(platform.mCurrentX, 0, i, 0, Integer.MIN_VALUE, ASContentModel.AS_UNBOUNDED, 0, 0);
        return startAnimation(this.mFilmScroller.getFinalX(), box.mCurrentY, box.mCurrentScale, 7);
    }

    public int flingFilmY(int i, int i2) {
        int i3;
        int iMin;
        Box box = this.mBoxes.get(i);
        int iHeightOf = heightOf(box);
        if (i2 < 0 || (i2 == 0 && box.mCurrentY <= 0)) {
            i3 = (((-this.mViewH) / 2) - ((iHeightOf + 1) / 2)) - 3;
        } else {
            i3 = ((this.mViewH + 1) / 2) + (iHeightOf / 2) + 3;
        }
        if (i2 != 0) {
            iMin = Math.min(MAX_DELETE_ANIMATION_DURATION, (int) ((Math.abs(i3 - box.mCurrentY) * 1000.0f) / Math.abs(i2)));
        } else {
            iMin = DEFAULT_DELETE_ANIMATION_DURATION;
        }
        ANIM_TIME[8] = iMin;
        if (box.doAnimation(i3, box.mCurrentScale, 8)) {
            redraw();
            return iMin;
        }
        return -1;
    }

    public int hitTest(int i, int i2) {
        for (int i3 = 0; i3 < 7; i3++) {
            int i4 = CENTER_OUT_INDEX[i3];
            if (this.mRects.get(i4).contains(i, i2)) {
                return i4;
            }
        }
        return ASContentModel.AS_UNBOUNDED;
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

    private boolean startAnimation(int i, int i2, float f, int i3) {
        boolean zDoAnimation = this.mPlatform.doAnimation(i, this.mPlatform.mDefaultY, i3) | false | this.mBoxes.get(0).doAnimation(i2, f, i3);
        if (zDoAnimation) {
            redraw();
        }
        return zDoAnimation;
    }

    public void advanceAnimation() {
        boolean zAdvanceAnimation = this.mPlatform.advanceAnimation() | false;
        for (int i = -3; i <= 3; i++) {
            zAdvanceAnimation |= this.mBoxes.get(i).advanceAnimation();
        }
        for (int i2 = -3; i2 < 3; i2++) {
            zAdvanceAnimation |= this.mGaps.get(i2).advanceAnimation();
        }
        if (this.mFilmRatio.advanceAnimation() | zAdvanceAnimation) {
            redraw();
        }
    }

    public boolean inOpeningAnimation() {
        return (this.mPlatform.mAnimationKind == 5 && this.mPlatform.mAnimationStartTime != -1) || (this.mBoxes.get(0).mAnimationKind == 5 && this.mBoxes.get(0).mAnimationStartTime != -1);
    }

    private int widthOf(Box box) {
        return (int) ((box.mImageW * box.mCurrentScale) + 0.5f);
    }

    private int heightOf(Box box) {
        return (int) ((box.mImageH * box.mCurrentScale) + 0.5f);
    }

    private int widthOf(Box box, float f) {
        return (int) ((box.mImageW * f) + 0.5f);
    }

    private int heightOf(Box box, float f) {
        return (int) ((box.mImageH * f) + 0.5f);
    }

    private void layoutAndSetPosition() {
        for (int i = 0; i < 7; i++) {
            convertBoxToRect(CENTER_OUT_INDEX[i]);
        }
    }

    private void dumpState() {
        int i = -3;
        for (int i2 = -3; i2 < 3; i2++) {
            com.mediatek.gallery3d.util.Log.d(TAG, "Gap " + i2 + ": " + this.mGaps.get(i2).mCurrentGap);
        }
        for (int i3 = 0; i3 < 7; i3++) {
            dumpRect(CENTER_OUT_INDEX[i3]);
        }
        while (i <= 3) {
            int i4 = i + 1;
            for (int i5 = i4; i5 <= 3; i5++) {
                if (Rect.intersects(this.mRects.get(i), this.mRects.get(i5))) {
                    com.mediatek.gallery3d.util.Log.d(TAG, "rect " + i + " and rect " + i5 + "intersects!");
                }
            }
            i = i4;
        }
    }

    private void dumpRect(int i) {
        StringBuilder sb = new StringBuilder();
        Rect rect = this.mRects.get(i);
        sb.append("Rect " + i + ":");
        sb.append("(");
        sb.append(rect.centerX());
        sb.append(",");
        sb.append(rect.centerY());
        sb.append(") [");
        sb.append(rect.width());
        sb.append("x");
        sb.append(rect.height());
        sb.append("]");
        com.mediatek.gallery3d.util.Log.d(TAG, sb.toString());
    }

    private void convertBoxToRect(int i) {
        Box box = this.mBoxes.get(i);
        Rect rect = this.mRects.get(i);
        int i2 = box.mCurrentY + this.mPlatform.mCurrentY + (this.mViewH / 2);
        int iWidthOf = widthOf(box);
        int iHeightOf = heightOf(box);
        if (i == 0) {
            rect.left = (this.mPlatform.mCurrentX + (this.mViewW / 2)) - (iWidthOf / 2);
            rect.right = rect.left + iWidthOf;
        } else if (i > 0) {
            int i3 = i - 1;
            rect.left = this.mRects.get(i3).right + this.mGaps.get(i3).mCurrentGap;
            rect.right = rect.left + iWidthOf;
        } else {
            rect.right = this.mRects.get(i + 1).left - this.mGaps.get(i).mCurrentGap;
            rect.left = rect.right - iWidthOf;
        }
        rect.top = i2 - (iHeightOf / 2);
        rect.bottom = rect.top + iHeightOf;
    }

    public Rect getPosition(int i) {
        return this.mRects.get(i);
    }

    private void initPlatform() {
        this.mPlatform.updateDefaultXY();
        this.mPlatform.mCurrentX = this.mPlatform.mDefaultX;
        this.mPlatform.mCurrentY = this.mPlatform.mDefaultY;
        this.mPlatform.mAnimationStartTime = -1L;
    }

    private void initBox(int i) {
        Box box = this.mBoxes.get(i);
        box.mImageW = this.mViewW;
        box.mImageH = this.mViewH;
        box.mUseViewSize = true;
        box.mScaleMin = getMinimalScale(box);
        box.mScaleMax = getMaximalScale(box);
        box.mCurrentY = 0;
        box.mCurrentScale = box.mScaleMin;
        box.mAnimationStartTime = -1L;
        box.mAnimationKind = -1;
    }

    private void initBox(int i, PhotoView.Size size) {
        if (size.width == 0 || size.height == 0) {
            initBox(i);
            return;
        }
        Box box = this.mBoxes.get(i);
        box.mImageW = size.width;
        box.mImageH = size.height;
        box.mUseViewSize = false;
        box.mScaleMin = getMinimalScale(box);
        box.mScaleMax = getMaximalScale(box);
        box.mCurrentY = 0;
        box.mCurrentScale = box.mScaleMin;
        box.mAnimationStartTime = -1L;
        box.mAnimationKind = -1;
    }

    private void initGap(int i) {
        Gap gap = this.mGaps.get(i);
        gap.mDefaultSize = getDefaultGapSize(i);
        gap.mCurrentGap = gap.mDefaultSize;
        gap.mAnimationStartTime = -1L;
    }

    private void initGap(int i, int i2) {
        Gap gap = this.mGaps.get(i);
        gap.mDefaultSize = getDefaultGapSize(i);
        gap.mCurrentGap = i2;
        gap.mAnimationStartTime = -1L;
    }

    private void debugMoveBox(int[] iArr) {
        StringBuilder sb = new StringBuilder("moveBox:");
        for (int i = 0; i < iArr.length; i++) {
            if (iArr[i] == Integer.MAX_VALUE) {
                sb.append(" N");
            } else {
                sb.append(" ");
                sb.append(iArr[i]);
            }
        }
        com.mediatek.gallery3d.util.Log.d(TAG, sb.toString());
    }

    public void moveBox(int[] iArr, boolean z, boolean z2, boolean z3, PhotoView.Size[] sizeArr) {
        int i;
        this.mHasPrev = z;
        this.mHasNext = z2;
        RangeIntArray rangeIntArray = new RangeIntArray(iArr, -3, 3);
        RangeArray<Box> rangeArray = new RangeArray<>(-3, 3);
        layoutAndSetPosition();
        for (int i2 = -3; i2 <= 3; i2++) {
            this.mBoxes.get(i2).mAbsoluteX = this.mRects.get(i2).centerX() - (this.mViewW / 2);
        }
        for (int i3 = -3; i3 <= 3; i3++) {
            this.mTempBoxes.put(i3, this.mBoxes.get(i3));
            rangeArray.put(i3, null);
        }
        for (int i4 = -3; i4 < 3; i4++) {
            this.mTempGaps.put(i4, this.mGaps.get(i4));
            this.mGaps.put(i4, null);
        }
        for (int i5 = -3; i5 <= 3; i5++) {
            int i6 = rangeIntArray.get(i5);
            if (i6 != Integer.MAX_VALUE) {
                rangeArray.put(i5, this.mTempBoxes.get(i6));
                this.mTempBoxes.put(i6, null);
            }
        }
        for (int i7 = -3; i7 < 3; i7++) {
            int i8 = rangeIntArray.get(i7);
            if (i8 != Integer.MAX_VALUE && (i = rangeIntArray.get(i7 + 1)) != Integer.MAX_VALUE && i8 + 1 == i) {
                this.mGaps.put(i7, this.mTempGaps.get(i8));
                this.mTempGaps.put(i8, null);
            }
        }
        int i9 = -3;
        for (int i10 = -3; i10 <= 3; i10++) {
            if (rangeArray.get(i10) == null) {
                while (this.mTempBoxes.get(i9) == null) {
                    i9++;
                }
                rangeArray.put(i10, this.mTempBoxes.get(i9));
                this.mBoxes.put(i10, this.mTempBoxes.get(i9));
                initBox(i10, sizeArr[i10 + 3]);
                i9++;
            }
        }
        this.mBoxes = rangeArray;
        int i11 = -3;
        while (i11 <= 3 && rangeIntArray.get(i11) == Integer.MAX_VALUE) {
            i11++;
        }
        int i12 = 3;
        while (i12 >= -3 && rangeIntArray.get(i12) == Integer.MAX_VALUE) {
            i12--;
        }
        if (i11 > 3) {
            this.mBoxes.get(0).mAbsoluteX = this.mPlatform.mCurrentX;
            i11 = 0;
            i12 = 0;
        }
        for (int iMax = Math.max(0, i11 + 1); iMax < i12; iMax++) {
            if (rangeIntArray.get(iMax) == Integer.MAX_VALUE) {
                Box box = this.mBoxes.get(iMax - 1);
                Box box2 = this.mBoxes.get(iMax);
                int iWidthOf = widthOf(box);
                box2.mAbsoluteX = box.mAbsoluteX + (iWidthOf - (iWidthOf / 2)) + (widthOf(box2) / 2) + getDefaultGapSize(iMax);
                if (this.mPopFromTop) {
                    box2.mCurrentY = -((this.mViewH / 2) + (heightOf(box2) / 2));
                } else {
                    box2.mCurrentY = (this.mViewH / 2) + (heightOf(box2) / 2);
                }
            }
        }
        for (int iMin = Math.min(-1, i12 - 1); iMin > i11; iMin--) {
            if (rangeIntArray.get(iMin) == Integer.MAX_VALUE) {
                Box box3 = this.mBoxes.get(iMin + 1);
                Box box4 = this.mBoxes.get(iMin);
                int iWidthOf2 = widthOf(box3);
                int iWidthOf3 = widthOf(box4);
                box4.mAbsoluteX = ((box3.mAbsoluteX - (iWidthOf2 / 2)) - (iWidthOf3 - (iWidthOf3 / 2))) - getDefaultGapSize(iMin);
                if (this.mPopFromTop) {
                    box4.mCurrentY = -((this.mViewH / 2) + (heightOf(box4) / 2));
                } else {
                    box4.mCurrentY = (this.mViewH / 2) + (heightOf(box4) / 2);
                }
            }
        }
        int i13 = -3;
        for (int i14 = -3; i14 < 3; i14++) {
            if (this.mGaps.get(i14) == null) {
                while (this.mTempGaps.get(i13) == null) {
                    i13++;
                }
                int i15 = i13 + 1;
                this.mGaps.put(i14, this.mTempGaps.get(i13));
                Box box5 = this.mBoxes.get(i14);
                Box box6 = this.mBoxes.get(i14 + 1);
                int iWidthOf4 = widthOf(box5);
                int iWidthOf5 = widthOf(box6);
                if (i14 >= i11 && i14 < i12) {
                    initGap(i14, ((box6.mAbsoluteX - box5.mAbsoluteX) - (iWidthOf5 / 2)) - (iWidthOf4 - (iWidthOf4 / 2)));
                } else {
                    initGap(i14);
                }
                i13 = i15;
            }
        }
        for (int i16 = i11 - 1; i16 >= -3; i16--) {
            Box box7 = this.mBoxes.get(i16 + 1);
            Box box8 = this.mBoxes.get(i16);
            int iWidthOf6 = widthOf(box7);
            int iWidthOf7 = widthOf(box8);
            box8.mAbsoluteX = ((box7.mAbsoluteX - (iWidthOf6 / 2)) - (iWidthOf7 - (iWidthOf7 / 2))) - this.mGaps.get(i16).mCurrentGap;
        }
        while (true) {
            i12++;
            if (i12 > 3) {
                break;
            }
            int i17 = i12 - 1;
            Box box9 = this.mBoxes.get(i17);
            Box box10 = this.mBoxes.get(i12);
            int iWidthOf8 = widthOf(box9);
            box10.mAbsoluteX = box9.mAbsoluteX + (iWidthOf8 - (iWidthOf8 / 2)) + (widthOf(box10) / 2) + this.mGaps.get(i17).mCurrentGap;
        }
        int i18 = this.mBoxes.get(0).mAbsoluteX - this.mPlatform.mCurrentX;
        this.mPlatform.mCurrentX += i18;
        this.mPlatform.mFromX += i18;
        this.mPlatform.mToX += i18;
        this.mPlatform.mFlingOffset += i18;
        if (this.mConstrained != z3) {
            this.mConstrained = z3;
            this.mPlatform.updateDefaultXY();
            updateScaleAndGapLimit();
        }
        snapAndRedraw();
    }

    public boolean isAtMinimalScale() {
        Box box = this.mBoxes.get(0);
        return isAlmostEqual(box.mCurrentScale, box.mScaleMin);
    }

    public boolean isCenter() {
        return this.mPlatform.mCurrentX == this.mPlatform.mDefaultX && this.mBoxes.get(0).mCurrentY == 0;
    }

    public int getImageWidth() {
        return this.mBoxes.get(0).mImageW;
    }

    public int getImageHeight() {
        return this.mBoxes.get(0).mImageH;
    }

    public float getImageScale() {
        return this.mBoxes.get(0).mCurrentScale;
    }

    public int getImageAtEdges() {
        Box box = this.mBoxes.get(0);
        Platform platform = this.mPlatform;
        calculateStableBound(box.mCurrentScale);
        int i = platform.mCurrentX <= this.mBoundLeft ? 2 : 0;
        if (platform.mCurrentX >= this.mBoundRight) {
            i |= 1;
        }
        if (box.mCurrentY <= this.mBoundTop) {
            i |= 8;
        }
        if (box.mCurrentY >= this.mBoundBottom) {
            return i | 4;
        }
        return i;
    }

    public boolean isScrolling() {
        return (this.mPlatform.mAnimationStartTime == -1 || this.mPlatform.mCurrentX == this.mPlatform.mToX) ? false : true;
    }

    public void stopScrolling() {
        if (this.mPlatform.mAnimationStartTime == -1) {
            return;
        }
        if (this.mFilmMode) {
            this.mFilmScroller.forceFinished(true);
        }
        Platform platform = this.mPlatform;
        Platform platform2 = this.mPlatform;
        int i = this.mPlatform.mCurrentX;
        platform2.mToX = i;
        platform.mFromX = i;
    }

    public float getFilmRatio() {
        return this.mFilmRatio.mCurrentRatio;
    }

    public void setPopFromTop(boolean z) {
        this.mPopFromTop = z;
    }

    public boolean hasDeletingBox() {
        for (int i = -3; i <= 3; i++) {
            if (this.mBoxes.get(i).mAnimationKind == 8) {
                return true;
            }
        }
        return false;
    }

    private float getMinimalScale(Box box) {
        int iWidth;
        int iHeight;
        if (!this.mFilmMode && this.mConstrained && !this.mConstrainedFrame.isEmpty() && box == this.mBoxes.get(0)) {
            iWidth = this.mConstrainedFrame.width();
            iHeight = this.mConstrainedFrame.height();
        } else {
            iWidth = this.mViewW;
            iHeight = this.mViewH;
        }
        float f = 1.0f;
        float f2 = 0.7f;
        if (!this.mFilmMode) {
            f2 = 1.0f;
        } else if (this.mViewH > this.mViewW) {
            f = FILM_MODE_PORTRAIT_HEIGHT;
        } else {
            f = 0.7f;
        }
        return Math.min(4.0f, Math.min((f2 * iWidth) / box.mImageW, (f * iHeight) / box.mImageH));
    }

    private float getMaximalScale(Box box) {
        if (this.mFilmMode) {
            return getMinimalScale(box);
        }
        if (!this.mConstrained || this.mConstrainedFrame.isEmpty()) {
            return 4.0f;
        }
        return getMinimalScale(box);
    }

    private static boolean isAlmostEqual(float f, float f2) {
        float f3 = f - f2;
        if (f3 < 0.0f) {
            f3 = -f3;
        }
        return f3 < 0.02f;
    }

    private void calculateStableBound(float f, int i) {
        Box box = this.mBoxes.get(0);
        int iWidthOf = widthOf(box, f);
        int iHeightOf = heightOf(box, f);
        this.mBoundLeft = (((this.mViewW + 1) / 2) - ((iWidthOf + 1) / 2)) - i;
        this.mBoundRight = ((iWidthOf / 2) - (this.mViewW / 2)) + i;
        this.mBoundTop = ((this.mViewH + 1) / 2) - ((iHeightOf + 1) / 2);
        this.mBoundBottom = (iHeightOf / 2) - (this.mViewH / 2);
        if (viewTallerThanScaledImage(f)) {
            this.mBoundBottom = 0;
            this.mBoundTop = 0;
        }
        if (viewWiderThanScaledImage(f)) {
            int i2 = this.mPlatform.mDefaultX;
            this.mBoundRight = i2;
            this.mBoundLeft = i2;
        }
    }

    private void calculateStableBound(float f) {
        calculateStableBound(f, 0);
    }

    private boolean viewTallerThanScaledImage(float f) {
        return this.mViewH >= heightOf(this.mBoxes.get(0), f);
    }

    private boolean viewWiderThanScaledImage(float f) {
        return this.mViewW >= widthOf(this.mBoxes.get(0), f);
    }

    private float getTargetScale(Box box) {
        return box.mAnimationStartTime == -1 ? box.mCurrentScale : box.mToScale;
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
            float f;
            if (this.mAnimationStartTime == -1) {
                return false;
            }
            if (this.mAnimationStartTime == PositionController.LAST_ANIMATION) {
                this.mAnimationStartTime = -1L;
                return startSnapback();
            }
            if (this.mAnimationDuration != 0) {
                f = (AnimationTime.get() - this.mAnimationStartTime) / this.mAnimationDuration;
            } else {
                f = 1.0f;
            }
            if (interpolate(f < 1.0f ? applyInterpolationCurve(this.mAnimationKind, f) : 1.0f)) {
                this.mAnimationStartTime = PositionController.LAST_ANIMATION;
                return true;
            }
            return true;
        }

        private static float applyInterpolationCurve(int i, float f) {
            float f2 = 1.0f - f;
            switch (i) {
                case 0:
                case 6:
                case 7:
                case 8:
                case 9:
                    return 1.0f - f2;
                case 1:
                case 5:
                    return 1.0f - (f2 * f2);
                case 2:
                case 3:
                case 4:
                    return 1.0f - ((((f2 * f2) * f2) * f2) * f2);
                default:
                    return f;
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
            int iClamp;
            if (this.mAnimationStartTime != -1) {
                return false;
            }
            if ((this.mAnimationKind == 0 && PositionController.this.mListener.isHoldingDown()) || PositionController.this.mInScale) {
                return false;
            }
            Box box = (Box) PositionController.this.mBoxes.get(0);
            float fClamp = Utils.clamp(box.mCurrentScale, PositionController.this.mExtraScalingRange ? box.mScaleMin * 0.7f : box.mScaleMin, PositionController.this.mExtraScalingRange ? box.mScaleMax * PositionController.SCALE_MAX_EXTRA : box.mScaleMax);
            int i = this.mCurrentX;
            int i2 = this.mDefaultY;
            if (!PositionController.this.mFilmMode) {
                PositionController.this.calculateStableBound(fClamp, PositionController.HORIZONTAL_SLACK);
                if (!PositionController.this.viewWiderThanScaledImage(fClamp)) {
                    i += (int) ((PositionController.this.mFocusX * (box.mCurrentScale - fClamp)) + 0.5f);
                }
                iClamp = Utils.clamp(i, PositionController.this.mBoundLeft, PositionController.this.mBoundRight);
            } else {
                iClamp = this.mDefaultX;
            }
            if (this.mCurrentX == iClamp && this.mCurrentY == i2) {
                return false;
            }
            return doAnimation(iClamp, i2, 2);
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

        private boolean doAnimation(int i, int i2, int i3) {
            if (this.mCurrentX == i && this.mCurrentY == i2) {
                return false;
            }
            this.mAnimationKind = i3;
            this.mFromX = this.mCurrentX;
            this.mFromY = this.mCurrentY;
            this.mToX = i;
            this.mToY = i2;
            this.mAnimationStartTime = AnimationTime.startTime();
            this.mAnimationDuration = PositionController.ANIM_TIME[i3];
            this.mFlingOffset = 0;
            advanceAnimation();
            return true;
        }

        @Override
        protected boolean interpolate(float f) {
            if (this.mAnimationKind == 6) {
                return interpolateFlingPage(f);
            }
            if (this.mAnimationKind == 7) {
                return interpolateFlingFilm(f);
            }
            return interpolateLinear(f);
        }

        private boolean interpolateFlingFilm(float f) {
            byte b;
            PositionController.this.mFilmScroller.computeScrollOffset();
            this.mCurrentX = PositionController.this.mFilmScroller.getCurrX() + this.mFlingOffset;
            if (this.mCurrentX < this.mDefaultX) {
                b = !PositionController.this.mHasNext ? (byte) 3 : (byte) -1;
            } else if (this.mCurrentX > this.mDefaultX && !PositionController.this.mHasPrev) {
                b = 1;
            }
            if (b != -1) {
                PositionController.this.mFilmScroller.forceFinished(true);
                this.mCurrentX = this.mDefaultX;
            }
            return PositionController.this.mFilmScroller.isFinished();
        }

        private boolean interpolateFlingPage(float f) {
            PositionController.this.mPageScroller.computeScrollOffset(f);
            PositionController.this.calculateStableBound(((Box) PositionController.this.mBoxes.get(0)).mCurrentScale);
            int i = this.mCurrentX;
            this.mCurrentX = PositionController.this.mPageScroller.getCurrX();
            if (i <= PositionController.this.mBoundLeft || this.mCurrentX != PositionController.this.mBoundLeft) {
                if (i < PositionController.this.mBoundRight && this.mCurrentX == PositionController.this.mBoundRight) {
                    PositionController.this.mListener.onAbsorb((int) (PositionController.this.mPageScroller.getCurrVelocityX() + 0.5f), 1);
                }
            } else {
                PositionController.this.mListener.onAbsorb((int) ((-PositionController.this.mPageScroller.getCurrVelocityX()) + 0.5f), 3);
            }
            return f >= 1.0f;
        }

        private boolean interpolateLinear(float f) {
            if (f >= 1.0f) {
                this.mCurrentX = this.mToX;
                this.mCurrentY = this.mToY;
                return true;
            }
            if (this.mAnimationKind == 9) {
                f = CaptureAnimation.calculateSlide(f);
            }
            this.mCurrentX = (int) (this.mFromX + ((this.mToX - this.mFromX) * f));
            this.mCurrentY = (int) (this.mFromY + (f * (this.mToY - this.mFromY)));
            return this.mAnimationKind != 9 && this.mCurrentX == this.mToX && this.mCurrentY == this.mToY;
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
        public MediaData mMediaData;
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
            float fClamp;
            int iClamp;
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
            int i = this.mCurrentY;
            if (this == PositionController.this.mBoxes.get(0)) {
                fClamp = Utils.clamp(this.mCurrentScale, PositionController.this.mExtraScalingRange ? this.mScaleMin * 0.7f : this.mScaleMin, PositionController.this.mExtraScalingRange ? this.mScaleMax * PositionController.SCALE_MAX_EXTRA : this.mScaleMax);
                if (!PositionController.this.mFilmMode) {
                    PositionController.this.calculateStableBound(fClamp, PositionController.HORIZONTAL_SLACK);
                    if (!PositionController.this.viewTallerThanScaledImage(fClamp)) {
                        i += (int) ((PositionController.this.mFocusY * (this.mCurrentScale - fClamp)) + 0.5f);
                    }
                    iClamp = Utils.clamp(i, PositionController.this.mBoundTop, PositionController.this.mBoundBottom);
                } else {
                    iClamp = 0;
                }
            } else {
                fClamp = this.mScaleMin;
                iClamp = 0;
            }
            if (this.mCurrentY == iClamp && this.mCurrentScale == fClamp) {
                return false;
            }
            return doAnimation(iClamp, fClamp, 2);
        }

        private boolean doAnimation(int i, float f, int i2) {
            float fClampScale = clampScale(f);
            if (this.mCurrentY == i && this.mCurrentScale == fClampScale && i2 != 9) {
                return false;
            }
            this.mAnimationKind = i2;
            this.mFromY = this.mCurrentY;
            this.mFromScale = this.mCurrentScale;
            this.mToY = i;
            this.mToScale = fClampScale;
            this.mAnimationStartTime = AnimationTime.startTime();
            this.mAnimationDuration = PositionController.ANIM_TIME[i2];
            advanceAnimation();
            return true;
        }

        public float clampScale(float f) {
            return Utils.clamp(f, 0.7f * this.mScaleMin, PositionController.SCALE_MAX_EXTRA * this.mScaleMax);
        }

        @Override
        protected boolean interpolate(float f) {
            if (this.mAnimationKind == 6) {
                return interpolateFlingPage(f);
            }
            return interpolateLinear(f);
        }

        private boolean interpolateFlingPage(float f) {
            PositionController.this.mPageScroller.computeScrollOffset(f);
            PositionController.this.calculateStableBound(this.mCurrentScale);
            int i = this.mCurrentY;
            this.mCurrentY = PositionController.this.mPageScroller.getCurrY();
            if (i <= PositionController.this.mBoundTop || this.mCurrentY != PositionController.this.mBoundTop) {
                if (i < PositionController.this.mBoundBottom && this.mCurrentY == PositionController.this.mBoundBottom) {
                    PositionController.this.mListener.onAbsorb((int) (PositionController.this.mPageScroller.getCurrVelocityY() + 0.5f), 0);
                }
            } else {
                PositionController.this.mListener.onAbsorb((int) ((-PositionController.this.mPageScroller.getCurrVelocityY()) + 0.5f), 2);
            }
            return f >= 1.0f;
        }

        private boolean interpolateLinear(float f) {
            if (f >= 1.0f) {
                this.mCurrentY = this.mToY;
                this.mCurrentScale = this.mToScale;
                return true;
            }
            this.mCurrentY = (int) (this.mFromY + ((this.mToY - this.mFromY) * f));
            this.mCurrentScale = this.mFromScale + ((this.mToScale - this.mFromScale) * f);
            if (this.mAnimationKind != 9) {
                return this.mCurrentY == this.mToY && this.mCurrentScale == this.mToScale;
            }
            this.mCurrentScale *= CaptureAnimation.calculateScale(f);
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

        public boolean doAnimation(int i, int i2) {
            if (this.mCurrentGap == i && i2 != 9) {
                return false;
            }
            this.mAnimationKind = i2;
            this.mFromGap = this.mCurrentGap;
            this.mToGap = i;
            this.mAnimationStartTime = AnimationTime.startTime();
            this.mAnimationDuration = PositionController.ANIM_TIME[this.mAnimationKind];
            advanceAnimation();
            return true;
        }

        @Override
        protected boolean interpolate(float f) {
            if (f >= 1.0f) {
                this.mCurrentGap = this.mToGap;
                return true;
            }
            this.mCurrentGap = (int) (this.mFromGap + ((this.mToGap - this.mFromGap) * f));
            if (this.mAnimationKind != 9) {
                return this.mCurrentGap == this.mToGap;
            }
            this.mCurrentGap = (int) (this.mCurrentGap * CaptureAnimation.calculateScale(f));
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
            float f = PositionController.this.mFilmMode ? 1.0f : 0.0f;
            if (f == this.mToRatio) {
                return false;
            }
            return doAnimation(f, 2);
        }

        private boolean doAnimation(float f, int i) {
            this.mAnimationKind = i;
            this.mFromRatio = this.mCurrentRatio;
            this.mToRatio = f;
            this.mAnimationStartTime = AnimationTime.startTime();
            this.mAnimationDuration = PositionController.ANIM_TIME[this.mAnimationKind];
            advanceAnimation();
            return true;
        }

        @Override
        protected boolean interpolate(float f) {
            if (f >= 1.0f) {
                this.mCurrentRatio = this.mToRatio;
                return true;
            }
            this.mCurrentRatio = this.mFromRatio + (f * (this.mToRatio - this.mFromRatio));
            return this.mCurrentRatio == this.mToRatio;
        }
    }

    public int hitTestIgnoreVertical(int i, int i2) {
        for (int i3 = 0; i3 < 7; i3++) {
            int i4 = CENTER_OUT_INDEX[i3];
            Rect rect = this.mRects.get(i4);
            if (i >= rect.left && i <= rect.right) {
                return i4;
            }
        }
        return ASContentModel.AS_UNBOUNDED;
    }

    public void setMediaData(int i, MediaData mediaData) {
        Box box = this.mBoxes.get(i);
        if (mediaData == box.mMediaData) {
            return;
        }
        box.mMediaData = mediaData;
    }
}
