package com.android.gallery3d.ui;

import android.graphics.Rect;
import android.os.Handler;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.animation.DecelerateInterpolator;
import com.android.gallery3d.anim.Animation;
import com.android.gallery3d.app.AbstractGalleryActivity;
import com.android.gallery3d.app.GalleryActionBar;
import com.android.gallery3d.common.Utils;
import com.android.gallery3d.data.LocalVideo;
import com.android.gallery3d.data.MediaItem;
import com.android.gallery3d.glrenderer.GLCanvas;
import com.mediatek.gallery3d.layout.FancyHelper;
import com.mediatek.gallery3d.layout.FancyLayout;
import com.mediatek.gallery3d.layout.FancyPaper;
import com.mediatek.gallery3d.layout.Layout;
import com.mediatek.gallery3d.util.FeatureConfig;
import java.util.ArrayList;
import java.util.HashMap;

public class SlotView extends GLView implements Layout.DataChangeListener {
    public static final int COL_NUM = 2;
    private static final int INDEX_NONE = -1;
    public static final int LAND = 0;
    public static final int OVERSCROLL_3D = 0;
    public static final int OVERSCROLL_NONE = 2;
    public static final int OVERSCROLL_SYSTEM = 1;
    public static final int PORT = 1;
    public static final int RENDER_MORE_FRAME = 2;
    public static final int RENDER_MORE_PASS = 1;
    private static final String TAG = "Gallery2/SlotView";
    private GalleryActionBar mActionBar;
    private AbstractGalleryActivity mActivity;
    private SlotAnimation mAnimation;
    private int mBackUpSlotCount;
    private Spec mDefaultLayoutSpec;
    private boolean mDownInScrolling;
    private final GestureDetector mGestureDetector;
    private final Handler mHandler;
    private int mHeight;
    private Layout mLayout;
    public ArrayList<Layout> mLayoutArray;
    private Listener mListener;
    private boolean mMoreAnimation;
    private Spec mMultiWindowLayoutSpec;
    private int mOverscrollEffect;
    private final Paper mPaper;
    private SlotRenderer mRenderer;
    private int[] mRequestRenderSlots;
    private final ScrollerHelper mScroller;
    private ArrayList<SlotEntry> mSlotArray;
    public ArrayList mSlotEntry;
    private HashMap<Integer, ArrayList<SlotEntry>> mSlotMapByColumn;
    private int mStartIndex;
    private Layout mSwitchFromLayout;
    private final Rect mTempRect;
    private UserInteractionListener mUIListener;
    private int mWidth;
    private static final boolean mIsFancyLayoutSupported = FancyHelper.isFancyLayoutSupported();
    private static final boolean WIDE = !mIsFancyLayoutSupported;

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
        public int colsLand = -1;
        public int colsPort = -1;
    }

    public static class SimpleListener implements Listener {
        @Override
        public void onDown(int i) {
        }

        @Override
        public void onUp(boolean z) {
        }

        @Override
        public void onSingleTapUp(int i) {
        }

        @Override
        public void onLongTap(int i) {
        }

        @Override
        public void onScrollPositionChanged(int i, int i2) {
        }
    }

    public SlotView(AbstractGalleryActivity abstractGalleryActivity, Spec spec) {
        this.mPaper = mIsFancyLayoutSupported ? new FancyPaper() : new Paper();
        this.mMoreAnimation = false;
        this.mAnimation = null;
        this.mStartIndex = -1;
        this.mOverscrollEffect = 0;
        this.mRequestRenderSlots = new int[16];
        this.mTempRect = new Rect();
        this.mSlotArray = new ArrayList<>();
        this.mSlotMapByColumn = new HashMap<>();
        this.mSlotEntry = new ArrayList();
        this.mActivity = null;
        this.mLayoutArray = new ArrayList<>();
        this.mBackUpSlotCount = -1;
        this.mGestureDetector = new GestureDetector(abstractGalleryActivity, new MyGestureListener());
        this.mScroller = new ScrollerHelper(abstractGalleryActivity);
        this.mHandler = new SynchronizedHandler(abstractGalleryActivity.getGLRoot());
        this.mLayout = new DefaultLayout();
        setSlotSpec(spec);
    }

    public SlotView(AbstractGalleryActivity abstractGalleryActivity, Spec spec, boolean z) {
        this.mPaper = mIsFancyLayoutSupported ? new FancyPaper() : new Paper();
        this.mMoreAnimation = false;
        this.mAnimation = null;
        this.mStartIndex = -1;
        this.mOverscrollEffect = 0;
        this.mRequestRenderSlots = new int[16];
        this.mTempRect = new Rect();
        this.mSlotArray = new ArrayList<>();
        this.mSlotMapByColumn = new HashMap<>();
        this.mSlotEntry = new ArrayList();
        this.mActivity = null;
        this.mLayoutArray = new ArrayList<>();
        this.mBackUpSlotCount = -1;
        this.mGestureDetector = new GestureDetector(abstractGalleryActivity, new MyGestureListener());
        this.mScroller = new ScrollerHelper(abstractGalleryActivity);
        this.mHandler = new SynchronizedHandler(abstractGalleryActivity.getGLRoot());
        this.mActivity = abstractGalleryActivity;
        if (z) {
            this.mLayoutArray.add(0, new DefaultLayout(0));
            this.mLayoutArray.add(1, new FancyLayout(1));
            this.mLayoutArray.add(2, new DefaultLayout(2));
            switchLayout(1);
            for (int i = 0; i < 2; i++) {
                this.mSlotMapByColumn.put(Integer.valueOf(i), new ArrayList<>());
            }
        } else {
            this.mLayout = new DefaultLayout();
        }
        setSlotSpec(spec);
        if (z) {
            this.mLayout.setSlotArray(this.mSlotArray, this.mSlotMapByColumn);
        }
    }

    public void setSlotRenderer(SlotRenderer slotRenderer) {
        this.mRenderer = slotRenderer;
        if (this.mRenderer != null) {
            this.mRenderer.onSlotSizeChanged(this.mLayout.getSlotWidth(), this.mLayout.getSlotHeight());
            this.mRenderer.onVisibleRangeChanged(getVisibleStart(), getVisibleEnd());
        }
        if (mIsFancyLayoutSupported) {
            this.mLayout.setSlotRenderer(this.mRenderer);
        }
    }

    public void setCenterIndex(int i) {
        int height;
        int slotCount = this.mLayout.getSlotCount();
        if (i < 0 || i >= slotCount) {
            return;
        }
        Rect slotRect = this.mLayout.getSlotRect(i, this.mTempRect);
        if (WIDE) {
            height = ((slotRect.left + slotRect.right) - getWidth()) / 2;
        } else {
            height = ((slotRect.top + slotRect.bottom) - getHeight()) / 2;
        }
        setScrollPosition(height);
    }

    public void makeSlotVisible(int i) {
        Rect slotRect = this.mLayout.getSlotRect(i, this.mTempRect);
        int i2 = WIDE ? this.mScrollX : this.mScrollY;
        int width = WIDE ? getWidth() : getHeight();
        int i3 = i2 + width;
        int i4 = WIDE ? slotRect.left : slotRect.top;
        int i5 = WIDE ? slotRect.right : slotRect.bottom;
        if (width >= i5 - i4) {
            if (i4 >= i2) {
                if (i5 > i3) {
                    i2 = i5 - width;
                }
            } else {
                i2 = i4;
            }
        }
        setScrollPosition(i2);
    }

    public void setScrollPosition(int i) {
        int iClamp = Utils.clamp(i, 0, this.mLayout.getScrollLimit());
        this.mScroller.setPosition(iClamp);
        updateScrollPosition(iClamp, false);
    }

    public void setSlotSpec(Spec spec) {
        this.mLayout.setSlotSpec(spec);
        if (mIsFancyLayoutSupported) {
            this.mDefaultLayoutSpec = spec;
        }
    }

    @Override
    public void addComponent(GLView gLView) {
        throw new UnsupportedOperationException();
    }

    @Override
    protected void onLayout(boolean z, int i, int i2, int i3, int i4) {
        if (z) {
            int visibleStart = (this.mLayout.getVisibleStart() + this.mLayout.getVisibleEnd()) / 2;
            int i5 = i3 - i;
            this.mWidth = i5;
            int i6 = i4 - i2;
            this.mHeight = i6;
            this.mLayout.setSize(i5, i6);
            if (mIsFancyLayoutSupported && this.mActivity != null) {
                if (FeatureConfig.IS_TABLET) {
                    int i7 = this.mActivity.getResources().getConfiguration().orientation;
                    if (i6 > i5 && i7 == 1) {
                        this.mLayout.setViewHeightWhenPortrait(i6);
                    }
                } else if (i6 > i5) {
                    this.mLayout.setViewHeightWhenPortrait(i6);
                }
            }
            if (!(this.mLayout instanceof FancyLayout)) {
                makeSlotVisible(visibleStart);
            }
            if (this.mOverscrollEffect == 0) {
                this.mPaper.setSize(i5, i6);
            }
        }
    }

    public void startScatteringAnimation(RelativePosition relativePosition) {
        this.mAnimation = new ScatteringAnimation(relativePosition);
        this.mAnimation.start();
        if (this.mLayout.getSlotCount() != 0) {
            invalidate();
        }
    }

    public void startRisingAnimation() {
        this.mAnimation = new RisingAnimation();
        this.mAnimation.start();
        if (this.mLayout.getSlotCount() != 0) {
            invalidate();
        }
    }

    private void updateScrollPosition(int i, boolean z) {
        if (!z) {
            if (WIDE) {
                if (i == this.mScrollX) {
                    return;
                }
            } else if (i == this.mScrollY) {
                return;
            }
        }
        if (WIDE) {
            this.mScrollX = i;
        } else {
            this.mScrollY = i;
        }
        this.mLayout.setScrollPosition(i);
        onScrollPositionChanged(i);
    }

    protected void onScrollPositionChanged(int i) {
        int scrollLimit = this.mLayout.getScrollLimit();
        if (this.mListener != null) {
            this.mListener.onScrollPositionChanged(i, scrollLimit);
        }
    }

    public Rect getSlotRect(int i) {
        return this.mLayout.getSlotRect(i, new Rect());
    }

    @Override
    protected boolean onTouch(MotionEvent motionEvent) {
        if (this.mUIListener != null) {
            this.mUIListener.onUserInteraction();
        }
        this.mGestureDetector.onTouchEvent(motionEvent);
        switch (motionEvent.getAction()) {
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

    public void setUserInteractionListener(UserInteractionListener userInteractionListener) {
        this.mUIListener = userInteractionListener;
    }

    public void setOverscrollEffect(int i) {
        this.mOverscrollEffect = i;
        this.mScroller.setOverfling(i == 1);
    }

    private static int[] expandIntArray(int[] iArr, int i) {
        while (iArr.length < i) {
            iArr = new int[iArr.length * 2];
        }
        return iArr;
    }

    @Override
    protected void render(GLCanvas gLCanvas) {
        boolean zAdvanceAnimation;
        super.render(gLCanvas);
        if (this.mRenderer == null) {
            return;
        }
        this.mRenderer.prepareDrawing();
        long j = AnimationTime.get();
        boolean zAdvanceAnimation2 = this.mScroller.advanceAnimation(j) | this.mLayout.advanceAnimation(j);
        int i = this.mScrollX;
        updateScrollPosition(this.mScroller.getPosition(), false);
        if (this.mOverscrollEffect == 0) {
            int i2 = this.mScrollX;
            int scrollLimit = this.mLayout.getScrollLimit();
            if ((i > 0 && i2 == 0) || (i < scrollLimit && i2 == scrollLimit)) {
                float currVelocity = this.mScroller.getCurrVelocity();
                if (i2 == scrollLimit) {
                    currVelocity = -currVelocity;
                }
                if (!Float.isNaN(currVelocity)) {
                    this.mPaper.edgeReached(currVelocity);
                }
            }
            zAdvanceAnimation = this.mPaper.advanceAnimation();
        } else {
            zAdvanceAnimation = false;
        }
        boolean zCalculate = zAdvanceAnimation2 | zAdvanceAnimation;
        if (this.mAnimation != null) {
            zCalculate |= this.mAnimation.calculate(j);
        }
        gLCanvas.translate(-this.mScrollX, -this.mScrollY);
        int[] iArrExpandIntArray = expandIntArray(this.mRequestRenderSlots, this.mLayout.getVisibleEnd() - this.mLayout.getVisibleStart());
        boolean z = zCalculate;
        int i3 = 0;
        for (int visibleEnd = this.mLayout.getVisibleEnd() - 1; visibleEnd >= this.mLayout.getVisibleStart(); visibleEnd--) {
            int iRenderItem = renderItem(gLCanvas, visibleEnd, 0, zAdvanceAnimation);
            if ((iRenderItem & 2) != 0) {
                z = true;
            }
            if ((iRenderItem & 1) != 0) {
                iArrExpandIntArray[i3] = visibleEnd;
                i3++;
            }
        }
        int i4 = 1;
        while (i3 != 0) {
            int i5 = 0;
            boolean z2 = z;
            for (int i6 = 0; i6 < i3; i6++) {
                int iRenderItem2 = renderItem(gLCanvas, iArrExpandIntArray[i6], i4, zAdvanceAnimation);
                if ((iRenderItem2 & 2) != 0) {
                    z2 = true;
                }
                if ((iRenderItem2 & 1) != 0) {
                    iArrExpandIntArray[i5] = i6;
                    i5++;
                }
            }
            i4++;
            i3 = i5;
            z = z2;
        }
        gLCanvas.translate(this.mScrollX, this.mScrollY);
        if (z) {
            invalidate();
        }
        final UserInteractionListener userInteractionListener = this.mUIListener;
        if (this.mMoreAnimation && !z && userInteractionListener != null) {
            this.mHandler.post(new Runnable() {
                @Override
                public void run() {
                    userInteractionListener.onUserInteractionEnd();
                }
            });
        }
        this.mMoreAnimation = z;
    }

    private int renderItem(GLCanvas gLCanvas, int i, int i2, boolean z) {
        gLCanvas.save(3);
        Rect slotRect = this.mLayout.getSlotRect(i, this.mTempRect);
        if (z) {
            if (mIsFancyLayoutSupported) {
                gLCanvas.multiplyMatrix(this.mPaper.getTransform(slotRect, this.mScrollY), 0);
            } else {
                gLCanvas.multiplyMatrix(this.mPaper.getTransform(slotRect, this.mScrollX), 0);
            }
        } else {
            gLCanvas.translate(slotRect.left, slotRect.top, 0.0f);
        }
        if (this.mAnimation != null && this.mAnimation.isActive()) {
            this.mAnimation.apply(gLCanvas, i, slotRect);
        }
        int iRenderSlot = this.mRenderer.renderSlot(gLCanvas, i, i2, slotRect.right - slotRect.left, slotRect.bottom - slotRect.top);
        gLCanvas.restore();
        return iRenderSlot;
    }

    public static abstract class SlotAnimation extends Animation {
        protected float mProgress = 0.0f;

        public abstract void apply(GLCanvas gLCanvas, int i, Rect rect);

        public SlotAnimation() {
            setInterpolator(new DecelerateInterpolator(4.0f));
            setDuration(1500);
        }

        @Override
        protected void onCalculate(float f) {
            this.mProgress = f;
        }
    }

    public static class RisingAnimation extends SlotAnimation {
        private static final int RISING_DISTANCE = 128;

        @Override
        public void apply(GLCanvas gLCanvas, int i, Rect rect) {
            gLCanvas.translate(0.0f, 0.0f, 128.0f * (1.0f - this.mProgress));
        }
    }

    public static class ScatteringAnimation extends SlotAnimation {
        private int PHOTO_DISTANCE = 1000;
        private RelativePosition mCenter;

        public ScatteringAnimation(RelativePosition relativePosition) {
            this.mCenter = relativePosition;
        }

        @Override
        public void apply(GLCanvas gLCanvas, int i, Rect rect) {
            gLCanvas.translate((this.mCenter.getX() - rect.centerX()) * (1.0f - this.mProgress), (this.mCenter.getY() - rect.centerY()) * (1.0f - this.mProgress), i * this.PHOTO_DISTANCE * (1.0f - this.mProgress));
            gLCanvas.setAlpha(this.mProgress);
        }
    }

    public class DefaultLayout extends Layout {
        private int mContentLength;
        private boolean mForceRefreshFlag;
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

        public DefaultLayout(int i) {
            super(i);
            this.mVerticalPadding = new IntegerAnimation();
            this.mHorizontalPadding = new IntegerAnimation();
            this.mForceRefreshFlag = false;
        }

        public DefaultLayout() {
            this.mVerticalPadding = new IntegerAnimation();
            this.mHorizontalPadding = new IntegerAnimation();
            this.mForceRefreshFlag = false;
        }

        @Override
        public void setSlotSpec(Spec spec) {
            this.mSpec = spec;
        }

        @Override
        public boolean setSlotCount(int i) {
            if (!this.mForceRefreshFlag && i == this.mSlotCount) {
                return false;
            }
            if (this.mSlotCount != 0) {
                this.mHorizontalPadding.setEnabled(true);
                this.mVerticalPadding.setEnabled(true);
            }
            this.mSlotCount = i;
            int target = this.mHorizontalPadding.getTarget();
            int target2 = this.mVerticalPadding.getTarget();
            initLayoutParameters();
            return (target2 == this.mVerticalPadding.getTarget() && target == this.mHorizontalPadding.getTarget()) ? false : true;
        }

        @Override
        public int getSlotCount() {
            return this.mSlotCount;
        }

        @Override
        public Spec getSlotSpec() {
            return this.mSpec;
        }

        @Override
        public int getSlotGap() {
            return this.mSpec.slotGap;
        }

        @Override
        public int getViewWidth() {
            return this.mWidth;
        }

        @Override
        public int getViewHeight() {
            return this.mHeight;
        }

        @Override
        public void setForceRefreshFlag(boolean z) {
            this.mForceRefreshFlag = z;
        }

        @Override
        public void clearColumnArray(int i, boolean z) {
            ArrayList arrayList;
            if (SlotView.this.mSlotMapByColumn == null || SlotView.this.mSlotArray == null) {
                return;
            }
            for (int i2 = 0; i2 < 2 && (arrayList = (ArrayList) SlotView.this.mSlotMapByColumn.get(Integer.valueOf(i2))) != null; i2++) {
                if (z) {
                    arrayList.clear();
                } else {
                    for (int size = arrayList.size() - 1; size >= 0; size--) {
                        if (((SlotEntry) arrayList.get(size)).slotIndex >= i) {
                            arrayList.remove(size);
                        }
                    }
                }
            }
        }

        @Override
        public Rect getSlotRect(int i, Rect rect) {
            int i2;
            int i3;
            if (SlotView.WIDE) {
                int i4 = i / this.mUnitCount;
                i2 = i - (this.mUnitCount * i4);
                i3 = i4;
            } else {
                i2 = i / this.mUnitCount;
                i3 = i - (this.mUnitCount * i2);
            }
            int i5 = this.mHorizontalPadding.get() + (i3 * (this.mSlotWidth + this.mSlotGap));
            int i6 = this.mVerticalPadding.get() + (i2 * (this.mSlotHeight + this.mSlotGap));
            rect.set(i5, i6, this.mSlotWidth + i5, this.mSlotHeight + i6);
            return rect;
        }

        @Override
        public int getSlotWidth() {
            return this.mSlotWidth;
        }

        @Override
        public int getSlotHeight() {
            return this.mSlotHeight;
        }

        private void initLayoutParameters(int i, int i2, int i3, int i4, int[] iArr) {
            int i5 = (this.mSlotGap + i2) / (this.mSlotGap + i4);
            if (i5 == 0) {
                i5 = 1;
            }
            this.mUnitCount = i5;
            int iMin = Math.min(this.mUnitCount, this.mSlotCount);
            iArr[0] = (i2 - ((i4 * iMin) + ((iMin - 1) * this.mSlotGap))) / 2;
            int i6 = ((this.mSlotCount + this.mUnitCount) - 1) / this.mUnitCount;
            this.mContentLength = (i3 * i6) + ((i6 - 1) * this.mSlotGap);
            iArr[1] = Math.max(0, (i - this.mContentLength) / 2);
        }

        private void initLayoutParameters() {
            if (this.mSpec.slotWidth == -1) {
                if (SlotView.mIsFancyLayoutSupported) {
                    int i = this.mWidth > this.mHeight ? this.mSpec.colsLand : this.mSpec.colsPort;
                    this.mSlotGap = this.mSpec.slotGap;
                    this.mSlotWidth = Math.max(1, (this.mWidth - ((i - 1) * this.mSlotGap)) / i);
                    this.mSlotHeight = this.mSlotWidth;
                } else {
                    int i2 = this.mWidth > this.mHeight ? this.mSpec.rowsLand : this.mSpec.rowsPort;
                    this.mSlotGap = this.mSpec.slotGap;
                    this.mSlotHeight = Math.max(1, (this.mHeight - ((i2 - 1) * this.mSlotGap)) / i2);
                    this.mSlotWidth = this.mSlotHeight - this.mSpec.slotHeightAdditional;
                }
            } else {
                this.mSlotGap = 0;
                this.mSlotWidth = this.mSpec.slotWidth;
                this.mSlotHeight = this.mSpec.slotHeight;
            }
            if (SlotView.this.mRenderer != null) {
                SlotView.this.mRenderer.onSlotSizeChanged(this.mSlotWidth, this.mSlotHeight);
            }
            int[] iArr = new int[2];
            if (SlotView.WIDE) {
                initLayoutParameters(this.mWidth, this.mHeight, this.mSlotWidth, this.mSlotHeight, iArr);
                this.mVerticalPadding.startAnimateTo(iArr[0]);
                this.mHorizontalPadding.startAnimateTo(iArr[1]);
            } else {
                initLayoutParameters(this.mHeight, this.mWidth, this.mSlotHeight, this.mSlotWidth, iArr);
                this.mVerticalPadding.startAnimateTo(iArr[1]);
                this.mHorizontalPadding.startAnimateTo(iArr[0]);
            }
            updateVisibleSlotRange();
        }

        @Override
        public void setSize(int i, int i2) {
            this.mWidth = i;
            this.mHeight = i2;
            initLayoutParameters();
        }

        private void updateVisibleSlotRange() {
            int i = this.mScrollPosition;
            if (SlotView.WIDE) {
                setVisibleRange(Math.max(0, this.mUnitCount * (i / (this.mSlotWidth + this.mSlotGap))), Math.min(this.mSlotCount, this.mUnitCount * (((((i + this.mWidth) + this.mSlotWidth) + this.mSlotGap) - 1) / (this.mSlotWidth + this.mSlotGap))));
                return;
            }
            setVisibleRange(Math.max(0, this.mUnitCount * (i / (this.mSlotHeight + this.mSlotGap))), Math.min(this.mSlotCount, this.mUnitCount * (((((i + this.mHeight) + this.mSlotHeight) + this.mSlotGap) - 1) / (this.mSlotHeight + this.mSlotGap))));
        }

        @Override
        public void setScrollPosition(int i) {
            if (!this.mForceRefreshFlag && this.mScrollPosition == i) {
                return;
            }
            this.mScrollPosition = i;
            updateVisibleSlotRange();
        }

        private void setVisibleRange(int i, int i2) {
            if (!this.mForceRefreshFlag && i == this.mVisibleStart && i2 == this.mVisibleEnd) {
                return;
            }
            if (i < i2) {
                this.mVisibleStart = i;
                this.mVisibleEnd = i2;
            } else {
                this.mVisibleEnd = 0;
                this.mVisibleStart = 0;
            }
            if (SlotView.this.mRenderer != null) {
                SlotView.this.mRenderer.onVisibleRangeChanged(this.mVisibleStart, this.mVisibleEnd);
            }
        }

        @Override
        public int getVisibleStart() {
            return this.mVisibleStart;
        }

        @Override
        public int getVisibleEnd() {
            return this.mVisibleEnd;
        }

        @Override
        public int getSlotIndexByPosition(float f, float f2) {
            int i;
            int iRound = Math.round(f) + (SlotView.WIDE ? this.mScrollPosition : 0);
            int iRound2 = Math.round(f2);
            int i2 = SlotView.WIDE ? 0 : this.mScrollPosition;
            int i3 = iRound - this.mHorizontalPadding.get();
            int i4 = (iRound2 + i2) - this.mVerticalPadding.get();
            if (i3 < 0 || i4 < 0) {
                return -1;
            }
            int i5 = i3 / (this.mSlotWidth + this.mSlotGap);
            int i6 = i4 / (this.mSlotHeight + this.mSlotGap);
            if (SlotView.WIDE || i5 < this.mUnitCount) {
                if ((!SlotView.WIDE || i6 < this.mUnitCount) && i3 % (this.mSlotWidth + this.mSlotGap) < this.mSlotWidth && i4 % (this.mSlotHeight + this.mSlotGap) < this.mSlotHeight) {
                    if (SlotView.WIDE) {
                        i = (i5 * this.mUnitCount) + i6;
                    } else {
                        i = (i6 * this.mUnitCount) + i5;
                    }
                    if (i >= this.mSlotCount) {
                        return -1;
                    }
                    return i;
                }
                return -1;
            }
            return -1;
        }

        @Override
        public int getScrollLimit() {
            int i;
            int i2;
            if (SlotView.WIDE) {
                i = this.mContentLength;
                i2 = this.mWidth;
            } else {
                i = this.mContentLength;
                i2 = this.mHeight;
            }
            int i3 = i - i2;
            if (i3 <= 0) {
                return 0;
            }
            return i3;
        }

        @Override
        public boolean advanceAnimation(long j) {
            return this.mHorizontalPadding.calculate(j) | this.mVerticalPadding.calculate(j);
        }
    }

    private class MyGestureListener implements GestureDetector.OnGestureListener {
        private boolean isDown;

        private MyGestureListener() {
        }

        @Override
        public void onShowPress(MotionEvent motionEvent) {
            GLRoot gLRoot = SlotView.this.getGLRoot();
            gLRoot.lockRenderThread();
            try {
                if (!this.isDown) {
                    int slotIndexByPosition = SlotView.this.mLayout.getSlotIndexByPosition(motionEvent.getX(), motionEvent.getY());
                    if (slotIndexByPosition != -1) {
                        this.isDown = true;
                        SlotView.this.mListener.onDown(slotIndexByPosition);
                    }
                }
            } finally {
                gLRoot.unlockRenderThread();
            }
        }

        private void cancelDown(boolean z) {
            if (this.isDown) {
                this.isDown = false;
                SlotView.this.mListener.onUp(z);
            }
        }

        @Override
        public boolean onDown(MotionEvent motionEvent) {
            return false;
        }

        @Override
        public boolean onFling(MotionEvent motionEvent, MotionEvent motionEvent2, float f, float f2) {
            cancelDown(false);
            int scrollLimit = SlotView.this.mLayout.getScrollLimit();
            if (scrollLimit != 0) {
                if (!SlotView.WIDE) {
                    f = f2;
                }
                SlotView.this.mScroller.fling((int) (-f), 0, scrollLimit);
                if (SlotView.this.mUIListener != null) {
                    SlotView.this.mUIListener.onUserInteractionBegin();
                }
                SlotView.this.invalidate();
                return true;
            }
            return false;
        }

        @Override
        public boolean onScroll(MotionEvent motionEvent, MotionEvent motionEvent2, float f, float f2) {
            cancelDown(false);
            if (!SlotView.WIDE) {
                f = f2;
            }
            int iStartScroll = SlotView.this.mScroller.startScroll(Math.round(f), 0, SlotView.this.mLayout.getScrollLimit());
            if (SlotView.this.mOverscrollEffect == 0 && iStartScroll != 0) {
                SlotView.this.mPaper.overScroll(iStartScroll);
            }
            SlotView.this.invalidate();
            return true;
        }

        @Override
        public boolean onSingleTapUp(MotionEvent motionEvent) {
            int slotIndexByPosition;
            cancelDown(false);
            if (!SlotView.this.mDownInScrolling && (slotIndexByPosition = SlotView.this.mLayout.getSlotIndexByPosition(motionEvent.getX(), motionEvent.getY())) != -1) {
                SlotView.this.mListener.onSingleTapUp(slotIndexByPosition);
            }
            return true;
        }

        @Override
        public void onLongPress(MotionEvent motionEvent) {
            cancelDown(true);
            if (SlotView.this.mDownInScrolling) {
                return;
            }
            SlotView.this.lockRendering();
            try {
                int slotIndexByPosition = SlotView.this.mLayout.getSlotIndexByPosition(motionEvent.getX(), motionEvent.getY());
                if (slotIndexByPosition != -1) {
                    SlotView.this.mListener.onLongTap(slotIndexByPosition);
                }
            } finally {
                SlotView.this.unlockRendering();
            }
        }
    }

    public void setStartIndex(int i) {
        this.mStartIndex = i;
    }

    public boolean setSlotCount(int i) {
        boolean slotCount = this.mLayout.setSlotCount(i);
        if (mIsFancyLayoutSupported) {
            this.mBackUpSlotCount = i;
        }
        if (this.mStartIndex != -1) {
            setCenterIndex(this.mStartIndex);
            this.mStartIndex = -1;
        }
        setScrollPosition(WIDE ? this.mScrollX : this.mScrollY);
        return slotCount;
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

    public Rect getSlotRect(int i, GLView gLView) {
        Rect rect = new Rect();
        gLView.getBoundsOf(this, rect);
        Rect slotRect = getSlotRect(i);
        slotRect.offset(rect.left - getScrollX(), rect.top - getScrollY());
        return slotRect;
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

        public void setEnabled(boolean z) {
            this.mEnabled = z;
        }

        public void startAnimateTo(int i) {
            if (!this.mEnabled) {
                this.mCurrent = i;
                this.mTarget = i;
            } else {
                if (i == this.mTarget) {
                    return;
                }
                this.mFrom = this.mCurrent;
                this.mTarget = i;
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
        protected void onCalculate(float f) {
            this.mCurrent = Math.round(this.mFrom + ((this.mTarget - this.mFrom) * f));
            if (f == 1.0f) {
                this.mEnabled = false;
            }
        }
    }

    public static class SlotEntry {
        public String albumName;
        public int imageHeight;
        public int imageWidth;
        public int inWhichCol;
        public int inWhichRow;
        public boolean isLandCameraFolder;
        private boolean isVideo;
        public String mimeType;
        public int oriImageHeight;
        public int oriImageWidth;
        public int rotation;
        public int scaledHeight;
        public int scaledWidth;
        public int slotIndex;
        public Rect slotRect;

        public SlotEntry(int i, int i2, int i3, int i4, MediaItem mediaItem, int i5, int i6, int i7, boolean z, String str) {
            this.isLandCameraFolder = false;
            this.slotIndex = i;
            this.imageWidth = i3;
            this.imageHeight = i4;
            this.rotation = i5;
            this.oriImageWidth = mediaItem.getWidth();
            this.oriImageHeight = mediaItem.getHeight();
            this.mimeType = mediaItem.getMimeType();
            this.albumName = str;
            if (this.imageWidth <= 0 || this.imageHeight <= 0) {
                this.imageWidth = i6;
                this.imageHeight = i6;
                this.isLandCameraFolder = false;
                this.scaledHeight = i6;
                this.scaledWidth = i6;
                Log.d(SlotView.TAG, "<SlotEntry> imageWidth or imageHeight is 0, make default entry!!");
                return;
            }
            if (z && i2 == 0) {
                this.isLandCameraFolder = true;
                this.scaledWidth = (2 * i6) + i7;
                this.scaledHeight = Utils.clamp((this.imageHeight * this.scaledWidth) / this.imageWidth, (int) (this.scaledWidth * 0.4f), (int) (this.scaledWidth * 2.5f));
                this.isVideo = mediaItem.getMediaData().isVideo;
                if (!this.isVideo) {
                    this.scaledHeight = Math.round(this.scaledWidth / 1.78f);
                    return;
                }
                return;
            }
            this.scaledWidth = i6;
            this.scaledHeight = Utils.clamp((this.imageHeight * this.scaledWidth) / this.imageWidth, (int) (this.scaledWidth * 0.4f), (int) (this.scaledWidth * 2.5f));
        }

        public void update(int i, int i2) {
            if (this.imageWidth == 0 || this.imageHeight == 0) {
                this.scaledHeight = i;
                this.scaledWidth = i;
            } else {
                if (this.isLandCameraFolder) {
                    this.scaledWidth = (2 * i) + i2;
                    this.scaledHeight = Utils.clamp((this.imageHeight * this.scaledWidth) / this.imageWidth, (int) (this.scaledWidth * 0.4f), (int) (this.scaledWidth * 2.5f));
                    if (!this.isVideo) {
                        this.scaledHeight = Math.round(this.scaledWidth / 1.78f);
                        return;
                    }
                    return;
                }
                this.scaledWidth = i;
                this.scaledHeight = Utils.clamp((this.imageHeight * this.scaledWidth) / this.imageWidth, (int) (this.scaledWidth * 0.4f), (int) (this.scaledWidth * 2.5f));
            }
        }
    }

    public void setMultiWindowSpec(Spec spec) {
        this.mMultiWindowLayoutSpec = spec;
    }

    public void setActionBar(GalleryActionBar galleryActionBar) {
        this.mActionBar = galleryActionBar;
    }

    public void switchLayout(int i) {
        int visibleStart;
        if (this.mSwitchFromLayout != null) {
            visibleStart = this.mSwitchFromLayout.getVisibleStart();
            Log.d(TAG, "<switchLayout> <Fancy> visibleStart " + visibleStart);
        } else {
            visibleStart = -1;
        }
        switch (i) {
            case 0:
                if (this.mLayout != null && this.mLayout.getLayoutType() == 0) {
                    return;
                }
                Log.d(TAG, "<switchLayout> <Fancy> switch to DEFAULT_LAYOUT");
                this.mLayout = this.mLayoutArray.get(0);
                if (this.mSwitchFromLayout != null) {
                    this.mLayout.setForceRefreshFlag(true);
                    this.mLayout.setSlotSpec(this.mDefaultLayoutSpec);
                    setSlotCount(Math.max(0, this.mBackUpSlotCount));
                    if (this.mBackUpSlotCount <= 0) {
                        forceSetScrollPosition(0);
                    } else {
                        forceSetScrollPosition(this.mLayout.getSlotRect(Utils.clamp(visibleStart, 0, this.mBackUpSlotCount - 1), this.mTempRect).top);
                    }
                    this.mLayout.setForceRefreshFlag(false);
                }
                break;
                break;
            case 1:
                if (this.mLayout != null && this.mLayout.getLayoutType() == 1) {
                    return;
                }
                Log.d(TAG, "<switchLayout> <Fancy> switch to FANCY_LAYOUT");
                this.mLayout = this.mLayoutArray.get(1);
                this.mLayout.clearColumnArray(0, true);
                this.mLayout.refreshSlotMap(0);
                if (this.mWidth != 0 && this.mHeight != 0) {
                    Log.d(TAG, "<switchLayout> setSize, w=" + this.mWidth + ", h=" + this.mHeight);
                    this.mLayout.setSize(this.mWidth, this.mHeight);
                }
                int viewHeightWhenPortrait = this.mLayout.getViewHeightWhenPortrait();
                if (viewHeightWhenPortrait != -1) {
                    this.mLayout.setViewHeight(viewHeightWhenPortrait);
                } else {
                    this.mLayout.setViewHeight(Math.max(FancyHelper.getWidthPixels(), FancyHelper.getHeightPixels()));
                }
                if (this.mActionBar != null) {
                    this.mLayout.setActionBarHeight(this.mActionBar.getHeight());
                }
                if (this.mSwitchFromLayout != null) {
                    this.mLayout.setForceRefreshFlag(true);
                    this.mLayout.updateSlotCount(Math.max(0, this.mBackUpSlotCount));
                    if (this.mBackUpSlotCount <= 0) {
                        forceSetScrollPosition(0);
                    } else {
                        forceSetScrollPosition(this.mLayout.getSlotRect(Utils.clamp(visibleStart, 0, this.mBackUpSlotCount - 1), this.mTempRect).top);
                    }
                    this.mLayout.setForceRefreshFlag(false);
                }
                break;
                break;
            case 2:
                if (this.mLayout != null && this.mLayout.getLayoutType() == 2) {
                    return;
                }
                Log.d(TAG, "<switchLayout> switch to MULTI_WINDOW_LAYOUT");
                this.mLayout = this.mLayoutArray.get(2);
                if (this.mSwitchFromLayout != null) {
                    this.mLayout.setForceRefreshFlag(true);
                    this.mLayout.setSlotSpec(this.mMultiWindowLayoutSpec);
                    setSlotCount(Math.max(0, this.mBackUpSlotCount));
                    if (this.mBackUpSlotCount <= 0) {
                        forceSetScrollPosition(0);
                    } else {
                        forceSetScrollPosition(this.mLayout.getSlotRect(Utils.clamp(visibleStart, 0, this.mBackUpSlotCount - 1), this.mTempRect).top);
                    }
                    this.mLayout.setForceRefreshFlag(false);
                }
                break;
                break;
        }
        this.mSwitchFromLayout = this.mLayout;
    }

    @Override
    public void onDataChange(int i, MediaItem mediaItem, int i2, boolean z, String str) {
        if (this.mSlotArray == null || this.mLayout == null) {
            return;
        }
        if (i2 == 0) {
            this.mSlotArray.clear();
            this.mLayout.clearColumnArray(0, true);
            this.mLayout.refreshSlotMap(0);
            if (this.mLayout.getLayoutType() == 1) {
                this.mLayout.setForceRefreshFlag(true);
                this.mLayout.setScrollPosition(0);
                this.mLayout.setForceRefreshFlag(false);
            }
            invalidate();
            Log.d(TAG, "<onDataChange> <Fancy> size = 0, no album in Gallery");
            return;
        }
        int size = this.mSlotArray.size();
        if (i2 > 0 && size > i2) {
            for (int i3 = size - 1; i3 >= i2; i3--) {
                this.mSlotArray.remove(i3);
            }
            int i4 = i2 - 1;
            this.mLayout.clearColumnArray(i4, false);
            this.mLayout.onDataChange(i4, null, 0, false);
            this.mScroller.startScroll(0, 0, this.mLayout.getScrollLimit());
            invalidate();
        }
        if (mediaItem == null) {
            Log.d(TAG, "<onDataChange> <Fancy> index " + i + ", item == null, return!!!");
            return;
        }
        if (i < size) {
            SlotEntry slotEntry = this.mSlotArray.get(i);
            if (!z && !slotEntry.isLandCameraFolder && slotEntry.oriImageWidth == mediaItem.getWidth() && slotEntry.oriImageHeight == mediaItem.getHeight() && slotEntry.rotation == mediaItem.getRotation() && slotEntry.mimeType.equals(mediaItem.getMimeType())) {
                return;
            }
        }
        this.mLayout.clearColumnArray(i, false);
        addSlot(i, mediaItem, i2, z, str);
        Log.d(TAG, "<onDataChange> <Fancy> index " + i + ", size " + i2 + ", isCameraFolder " + z);
        this.mLayout.onDataChange(i, mediaItem, i2, z);
        this.mScroller.startScroll(0, 0, this.mLayout.getScrollLimit());
        invalidate();
    }

    private void addSlot(int i, MediaItem mediaItem, int i2, boolean z, String str) {
        int orientation;
        int i3;
        int i4;
        SlotEntry slotEntry;
        if (mediaItem == null || this.mLayout == null || this.mSlotArray == null) {
            return;
        }
        int screenWidthAtFancyMode = FancyHelper.getScreenWidthAtFancyMode();
        int slotGap = this.mLayout.getSlotGap();
        int i5 = 1;
        int i6 = (screenWidthAtFancyMode - (1 * slotGap)) / 2;
        if (mediaItem.getMediaType() == 4) {
            orientation = ((LocalVideo) mediaItem).getOrientation();
            if (orientation == -2) {
                SlotEntry slotEntry2 = new SlotEntry(i, orientation, i6, i6, mediaItem, orientation, i6, slotGap, false, str);
                if (this.mSlotArray.size() > i) {
                    this.mSlotArray.set(i, slotEntry2);
                    return;
                } else {
                    this.mSlotArray.add(slotEntry2);
                    return;
                }
            }
            if (orientation != 90 && orientation != 270) {
                i5 = 0;
            }
        } else {
            int rotation = mediaItem.getRotation();
            if (rotation == 90 || rotation == 270 ? mediaItem.getWidth() < mediaItem.getHeight() : mediaItem.getHeight() < mediaItem.getWidth()) {
                i5 = 0;
            }
            orientation = rotation;
        }
        int i7 = i5;
        int width = mediaItem.getWidth() > 0 ? mediaItem.getWidth() : mediaItem.getExtItem().getWidth();
        int height = mediaItem.getHeight() > 0 ? mediaItem.getHeight() : mediaItem.getExtItem().getHeight();
        if (width <= 0 || height <= 0) {
            Log.d(TAG, "<addSlot> <Fancy> width <= 0 || height <= 0, file path: " + mediaItem.getFilePath());
            i3 = i6;
            i4 = i3;
        } else {
            i3 = width;
            i4 = height;
        }
        if (orientation == 90 || orientation == 270) {
            float f = i4;
            slotEntry = new SlotEntry(i, i7, i4, ((float) i3) / f > 2.5f ? Math.round(f * 2.5f) : i3, mediaItem, orientation, i6, slotGap, z, str);
        } else {
            float f2 = i3;
            slotEntry = new SlotEntry(i, i7, i3, ((float) i4) / f2 > 2.5f ? Math.round(f2 * 2.5f) : i4, mediaItem, orientation, i6, slotGap, z, str);
        }
        if (this.mSlotArray.size() > i) {
            this.mSlotArray.set(i, slotEntry);
        } else {
            this.mSlotArray.add(slotEntry);
        }
    }

    public void forceSetScrollPosition(int i) {
        int iClamp = Utils.clamp(i, 0, this.mLayout.getScrollLimit());
        this.mScroller.setPosition(iClamp);
        updateScrollPosition(iClamp, true);
    }

    public void setPaddingSpec(int i, int i2) {
        if (this.mLayout != null) {
            this.mLayout.setPaddingSpec(i, i2);
        }
    }
}
