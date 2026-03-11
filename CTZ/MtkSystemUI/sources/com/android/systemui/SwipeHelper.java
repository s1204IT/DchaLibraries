package com.android.systemui;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.RectF;
import android.os.Handler;
import android.util.ArrayMap;
import android.util.Property;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import com.android.systemui.classifier.FalsingManager;
import com.android.systemui.statusbar.ExpandableNotificationRow;
import com.android.systemui.statusbar.FlingAnimationUtils;

public class SwipeHelper {
    private final Callback mCallback;
    private boolean mCanCurrViewBeDimissed;
    private final Context mContext;
    private View mCurrView;
    private float mDensityScale;
    private boolean mDisableHwLayers;
    private boolean mDragging;
    private final boolean mFadeDependingOnAmountSwiped;
    private final FalsingManager mFalsingManager;
    private final int mFalsingThreshold;
    private final FlingAnimationUtils mFlingAnimationUtils;
    private float mInitialTouchPos;
    private boolean mLongPressSent;
    private boolean mMenuRowIntercepting;
    private float mPagingTouchSlop;
    private float mPerpendicularInitialTouchPos;
    private boolean mSnappingChild;
    private final int mSwipeDirection;
    private boolean mTouchAboveFalsingThreshold;
    private Runnable mWatchLongPress;
    private float mMinSwipeProgress = 0.0f;
    private float mMaxSwipeProgress = 1.0f;
    private float mTranslation = 0.0f;
    private final int[] mTmpPos = new int[2];
    private final ArrayMap<View, Animator> mDismissPendingMap = new ArrayMap<>();
    private final Handler mHandler = new Handler();
    private final VelocityTracker mVelocityTracker = VelocityTracker.obtain();
    private final long mLongPressTimeout = (long) (ViewConfiguration.getLongPressTimeout() * 1.5f);

    public SwipeHelper(int i, Callback callback, Context context) {
        this.mContext = context;
        this.mCallback = callback;
        this.mSwipeDirection = i;
        this.mPagingTouchSlop = ViewConfiguration.get(context).getScaledPagingTouchSlop();
        Resources resources = context.getResources();
        this.mDensityScale = resources.getDisplayMetrics().density;
        this.mFalsingThreshold = resources.getDimensionPixelSize(R.dimen.swipe_helper_falsing_threshold);
        this.mFadeDependingOnAmountSwiped = resources.getBoolean(R.bool.config_fadeDependingOnAmountSwiped);
        this.mFalsingManager = FalsingManager.getInstance(context);
        this.mFlingAnimationUtils = new FlingAnimationUtils(context, getMaxEscapeAnimDuration() / 1000.0f);
    }

    public void setDensityScale(float f) {
        this.mDensityScale = f;
    }

    public void setPagingTouchSlop(float f) {
        this.mPagingTouchSlop = f;
    }

    public void setDisableHardwareLayers(boolean z) {
        this.mDisableHwLayers = z;
    }

    private float getPos(MotionEvent motionEvent) {
        return this.mSwipeDirection == 0 ? motionEvent.getX() : motionEvent.getY();
    }

    private float getPerpendicularPos(MotionEvent motionEvent) {
        return this.mSwipeDirection == 0 ? motionEvent.getY() : motionEvent.getX();
    }

    protected float getTranslation(View view) {
        return this.mSwipeDirection == 0 ? view.getTranslationX() : view.getTranslationY();
    }

    private float getVelocity(VelocityTracker velocityTracker) {
        return this.mSwipeDirection == 0 ? velocityTracker.getXVelocity() : velocityTracker.getYVelocity();
    }

    protected ObjectAnimator createTranslationAnimation(View view, float f) {
        return ObjectAnimator.ofFloat(view, (Property<View, Float>) (this.mSwipeDirection == 0 ? View.TRANSLATION_X : View.TRANSLATION_Y), f);
    }

    protected Animator getViewTranslationAnimator(View view, float f, ValueAnimator.AnimatorUpdateListener animatorUpdateListener) {
        ObjectAnimator objectAnimatorCreateTranslationAnimation = createTranslationAnimation(view, f);
        if (animatorUpdateListener != null) {
            objectAnimatorCreateTranslationAnimation.addUpdateListener(animatorUpdateListener);
        }
        return objectAnimatorCreateTranslationAnimation;
    }

    protected void setTranslation(View view, float f) {
        if (view == null) {
            return;
        }
        if (this.mSwipeDirection == 0) {
            view.setTranslationX(f);
        } else {
            view.setTranslationY(f);
        }
    }

    protected float getSize(View view) {
        return this.mSwipeDirection == 0 ? view.getMeasuredWidth() : view.getMeasuredHeight();
    }

    private float getSwipeProgressForOffset(View view, float f) {
        return Math.min(Math.max(this.mMinSwipeProgress, Math.abs(f / getSize(view))), this.mMaxSwipeProgress);
    }

    private float getSwipeAlpha(float f) {
        if (this.mFadeDependingOnAmountSwiped) {
            return Math.max(1.0f - f, 0.0f);
        }
        return 1.0f - Math.max(0.0f, Math.min(1.0f, f / 0.5f));
    }

    public void updateSwipeProgressFromOffset(View view, boolean z) {
        updateSwipeProgressFromOffset(view, z, getTranslation(view));
    }

    private void updateSwipeProgressFromOffset(View view, boolean z, float f) {
        float swipeProgressForOffset = getSwipeProgressForOffset(view, f);
        if (!this.mCallback.updateSwipeProgress(view, z, swipeProgressForOffset) && z) {
            if (!this.mDisableHwLayers) {
                if (swipeProgressForOffset != 0.0f && swipeProgressForOffset != 1.0f) {
                    view.setLayerType(2, null);
                } else {
                    view.setLayerType(0, null);
                }
            }
            view.setAlpha(getSwipeAlpha(swipeProgressForOffset));
        }
        invalidateGlobalRegion(view);
    }

    public static void invalidateGlobalRegion(View view) {
        invalidateGlobalRegion(view, new RectF(view.getLeft(), view.getTop(), view.getRight(), view.getBottom()));
    }

    public static void invalidateGlobalRegion(View view, RectF rectF) {
        while (view.getParent() != null && (view.getParent() instanceof View)) {
            view = (View) view.getParent();
            view.getMatrix().mapRect(rectF);
            view.invalidate((int) Math.floor(rectF.left), (int) Math.floor(rectF.top), (int) Math.ceil(rectF.right), (int) Math.ceil(rectF.bottom));
        }
    }

    public void cancelLongPress() {
        if (this.mWatchLongPress != null) {
            this.mHandler.removeCallbacks(this.mWatchLongPress);
            this.mWatchLongPress = null;
        }
    }

    public boolean onInterceptTouchEvent(final MotionEvent motionEvent) {
        if (this.mCurrView instanceof ExpandableNotificationRow) {
            this.mMenuRowIntercepting = ((ExpandableNotificationRow) this.mCurrView).getProvider().onInterceptTouchEvent(this.mCurrView, motionEvent);
        }
        switch (motionEvent.getAction()) {
            case 0:
                this.mTouchAboveFalsingThreshold = false;
                this.mDragging = false;
                this.mSnappingChild = false;
                this.mLongPressSent = false;
                this.mVelocityTracker.clear();
                this.mCurrView = this.mCallback.getChildAtPosition(motionEvent);
                if (this.mCurrView != null) {
                    onDownUpdate(this.mCurrView, motionEvent);
                    this.mCanCurrViewBeDimissed = this.mCallback.canChildBeDismissed(this.mCurrView);
                    this.mVelocityTracker.addMovement(motionEvent);
                    this.mInitialTouchPos = getPos(motionEvent);
                    this.mPerpendicularInitialTouchPos = getPerpendicularPos(motionEvent);
                    this.mTranslation = getTranslation(this.mCurrView);
                    if (this.mWatchLongPress == null) {
                        this.mWatchLongPress = new Runnable() {
                            @Override
                            public void run() {
                                if (SwipeHelper.this.mCurrView != null && !SwipeHelper.this.mLongPressSent) {
                                    SwipeHelper.this.mLongPressSent = true;
                                    SwipeHelper.this.mCurrView.getLocationOnScreen(SwipeHelper.this.mTmpPos);
                                    int rawX = ((int) motionEvent.getRawX()) - SwipeHelper.this.mTmpPos[0];
                                    int rawY = ((int) motionEvent.getRawY()) - SwipeHelper.this.mTmpPos[1];
                                    if (SwipeHelper.this.mCurrView instanceof ExpandableNotificationRow) {
                                        SwipeHelper.this.mCurrView.sendAccessibilityEvent(2);
                                        ((ExpandableNotificationRow) SwipeHelper.this.mCurrView).doLongClickCallback(rawX, rawY);
                                    }
                                }
                            }
                        };
                    }
                    this.mHandler.postDelayed(this.mWatchLongPress, this.mLongPressTimeout);
                }
                break;
            case 1:
            case 3:
                boolean z = this.mDragging || this.mLongPressSent || this.mMenuRowIntercepting;
                this.mDragging = false;
                this.mCurrView = null;
                this.mLongPressSent = false;
                this.mMenuRowIntercepting = false;
                cancelLongPress();
                if (z) {
                    return true;
                }
                break;
            case 2:
                if (this.mCurrView != null && !this.mLongPressSent) {
                    this.mVelocityTracker.addMovement(motionEvent);
                    float pos = getPos(motionEvent);
                    float perpendicularPos = getPerpendicularPos(motionEvent);
                    float f = pos - this.mInitialTouchPos;
                    float f2 = perpendicularPos - this.mPerpendicularInitialTouchPos;
                    if (Math.abs(f) > this.mPagingTouchSlop && Math.abs(f) > Math.abs(f2)) {
                        if (this.mCallback.canChildBeDragged(this.mCurrView)) {
                            this.mCallback.onBeginDrag(this.mCurrView);
                            this.mDragging = true;
                            this.mInitialTouchPos = getPos(motionEvent);
                            this.mTranslation = getTranslation(this.mCurrView);
                        }
                        cancelLongPress();
                    }
                }
                break;
        }
        return this.mDragging || this.mLongPressSent || this.mMenuRowIntercepting;
    }

    public void dismissChild(View view, float f, boolean z) {
        dismissChild(view, f, null, 0L, z, 0L, false);
    }

    public void dismissChild(final View view, float f, final Runnable runnable, long j, boolean z, long j2, boolean z2) {
        float size;
        long jMin;
        final boolean zCanChildBeDismissed = this.mCallback.canChildBeDismissed(view);
        boolean z3 = false;
        boolean z4 = view.getLayoutDirection() == 1;
        boolean z5 = f == 0.0f && (getTranslation(view) == 0.0f || z2) && this.mSwipeDirection == 1;
        boolean z6 = f == 0.0f && (getTranslation(view) == 0.0f || z2) && z4;
        if ((Math.abs(f) > getEscapeVelocity() && f < 0.0f) || (getTranslation(view) < 0.0f && !z2)) {
            z3 = true;
        }
        if (z3 || z6 || z5) {
            size = -getSize(view);
        } else {
            size = getSize(view);
        }
        float f2 = size;
        if (j2 == 0) {
            if (f != 0.0f) {
                jMin = Math.min(400L, (int) ((Math.abs(f2 - getTranslation(view)) * 1000.0f) / Math.abs(f)));
            } else {
                jMin = 200;
            }
        } else {
            jMin = j2;
        }
        if (!this.mDisableHwLayers) {
            view.setLayerType(2, null);
        }
        Animator viewTranslationAnimator = getViewTranslationAnimator(view, f2, new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                SwipeHelper.this.onTranslationUpdate(view, ((Float) valueAnimator.getAnimatedValue()).floatValue(), zCanChildBeDismissed);
            }
        });
        if (viewTranslationAnimator == null) {
            return;
        }
        if (z) {
            viewTranslationAnimator.setInterpolator(Interpolators.FAST_OUT_LINEAR_IN);
            viewTranslationAnimator.setDuration(jMin);
        } else {
            this.mFlingAnimationUtils.applyDismissing(viewTranslationAnimator, getTranslation(view), f2, f, getSize(view));
        }
        if (j > 0) {
            viewTranslationAnimator.setStartDelay(j);
        }
        viewTranslationAnimator.addListener(new AnimatorListenerAdapter() {
            private boolean mCancelled;

            @Override
            public void onAnimationCancel(Animator animator) {
                this.mCancelled = true;
            }

            @Override
            public void onAnimationEnd(Animator animator) {
                boolean zIsRemoved;
                SwipeHelper.this.updateSwipeProgressFromOffset(view, zCanChildBeDismissed);
                SwipeHelper.this.mDismissPendingMap.remove(view);
                if (view instanceof ExpandableNotificationRow) {
                    zIsRemoved = ((ExpandableNotificationRow) view).isRemoved();
                } else {
                    zIsRemoved = false;
                }
                if (!this.mCancelled || zIsRemoved) {
                    SwipeHelper.this.mCallback.onChildDismissed(view);
                }
                if (runnable != null) {
                    runnable.run();
                }
                if (!SwipeHelper.this.mDisableHwLayers) {
                    view.setLayerType(0, null);
                }
            }
        });
        prepareDismissAnimation(view, viewTranslationAnimator);
        this.mDismissPendingMap.put(view, viewTranslationAnimator);
        viewTranslationAnimator.start();
    }

    protected void prepareDismissAnimation(View view, Animator animator) {
    }

    public void snapChild(final View view, final float f, float f2) {
        final boolean zCanChildBeDismissed = this.mCallback.canChildBeDismissed(view);
        Animator viewTranslationAnimator = getViewTranslationAnimator(view, f, new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                SwipeHelper.this.onTranslationUpdate(view, ((Float) valueAnimator.getAnimatedValue()).floatValue(), zCanChildBeDismissed);
            }
        });
        if (viewTranslationAnimator == null) {
            return;
        }
        viewTranslationAnimator.setDuration(150);
        viewTranslationAnimator.addListener(new AnimatorListenerAdapter() {
            boolean wasCancelled = false;

            @Override
            public void onAnimationCancel(Animator animator) {
                this.wasCancelled = true;
            }

            @Override
            public void onAnimationEnd(Animator animator) {
                SwipeHelper.this.mSnappingChild = false;
                if (!this.wasCancelled) {
                    SwipeHelper.this.updateSwipeProgressFromOffset(view, zCanChildBeDismissed);
                    SwipeHelper.this.mCallback.onChildSnappedBack(view, f);
                }
            }
        });
        prepareSnapBackAnimation(view, viewTranslationAnimator);
        this.mSnappingChild = true;
        viewTranslationAnimator.start();
    }

    protected void prepareSnapBackAnimation(View view, Animator animator) {
    }

    public void onDownUpdate(View view, MotionEvent motionEvent) {
    }

    protected void onMoveUpdate(View view, MotionEvent motionEvent, float f, float f2) {
    }

    public void onTranslationUpdate(View view, float f, boolean z) {
        updateSwipeProgressFromOffset(view, z, f);
    }

    private void snapChildInstantly(View view) {
        boolean zCanChildBeDismissed = this.mCallback.canChildBeDismissed(view);
        setTranslation(view, 0.0f);
        updateSwipeProgressFromOffset(view, zCanChildBeDismissed);
    }

    public void snapChildIfNeeded(View view, boolean z, float f) {
        if ((this.mDragging && this.mCurrView == view) || this.mSnappingChild) {
            return;
        }
        Animator animator = this.mDismissPendingMap.get(view);
        boolean z2 = true;
        if (animator != null) {
            animator.cancel();
        } else if (getTranslation(view) == 0.0f) {
            z2 = false;
        }
        if (z2) {
            if (z) {
                snapChild(view, f, 0.0f);
            } else {
                snapChildInstantly(view);
            }
        }
    }

    public boolean onTouchEvent(MotionEvent motionEvent) {
        if (this.mLongPressSent && !this.mMenuRowIntercepting) {
            return true;
        }
        if (!this.mDragging && !this.mMenuRowIntercepting) {
            if (this.mCallback.getChildAtPosition(motionEvent) != null) {
                onInterceptTouchEvent(motionEvent);
                return true;
            }
            cancelLongPress();
            return false;
        }
        this.mVelocityTracker.addMovement(motionEvent);
        switch (motionEvent.getAction()) {
            case 1:
            case 3:
                if (this.mCurrView != null) {
                    this.mVelocityTracker.computeCurrentVelocity(1000, getMaxVelocity());
                    float velocity = getVelocity(this.mVelocityTracker);
                    if (!handleUpEvent(motionEvent, this.mCurrView, velocity, getTranslation(this.mCurrView))) {
                        if (isDismissGesture(motionEvent)) {
                            dismissChild(this.mCurrView, velocity, !swipedFastEnough());
                        } else {
                            this.mCallback.onDragCancelled(this.mCurrView);
                            snapChild(this.mCurrView, 0.0f, velocity);
                        }
                        this.mCurrView = null;
                    }
                    this.mDragging = false;
                }
                return true;
            case 2:
            case 4:
                if (this.mCurrView != null) {
                    float pos = getPos(motionEvent) - this.mInitialTouchPos;
                    float fAbs = Math.abs(pos);
                    if (fAbs >= getFalsingThreshold()) {
                        this.mTouchAboveFalsingThreshold = true;
                    }
                    if (!this.mCallback.canChildBeDismissed(this.mCurrView)) {
                        float size = getSize(this.mCurrView);
                        float f = 0.3f * size;
                        if (fAbs < size) {
                            pos = ((float) Math.sin(((double) (pos / size)) * 1.5707963267948966d)) * f;
                        } else if (pos <= 0.0f) {
                            pos = -f;
                        } else {
                            pos = f;
                        }
                    }
                    setTranslation(this.mCurrView, this.mTranslation + pos);
                    updateSwipeProgressFromOffset(this.mCurrView, this.mCanCurrViewBeDimissed);
                    onMoveUpdate(this.mCurrView, motionEvent, this.mTranslation + pos, pos);
                }
                return true;
            default:
                return true;
        }
    }

    private int getFalsingThreshold() {
        return (int) (this.mFalsingThreshold * this.mCallback.getFalsingThresholdFactor());
    }

    private float getMaxVelocity() {
        return 4000.0f * this.mDensityScale;
    }

    protected float getEscapeVelocity() {
        return getUnscaledEscapeVelocity() * this.mDensityScale;
    }

    protected float getUnscaledEscapeVelocity() {
        return 500.0f;
    }

    protected long getMaxEscapeAnimDuration() {
        return 400L;
    }

    protected boolean swipedFarEnough() {
        return Math.abs(getTranslation(this.mCurrView)) > 0.6f * getSize(this.mCurrView);
    }

    public boolean isDismissGesture(MotionEvent motionEvent) {
        return motionEvent.getActionMasked() == 1 && !isFalseGesture(motionEvent) && (swipedFastEnough() || swipedFarEnough()) && this.mCallback.canChildBeDismissed(this.mCurrView);
    }

    public boolean isFalseGesture(MotionEvent motionEvent) {
        boolean zIsAntiFalsingNeeded = this.mCallback.isAntiFalsingNeeded();
        if (this.mFalsingManager.isClassiferEnabled()) {
            if (!zIsAntiFalsingNeeded || !this.mFalsingManager.isFalseTouch()) {
                return false;
            }
        } else if (!zIsAntiFalsingNeeded || this.mTouchAboveFalsingThreshold) {
            return false;
        }
        return true;
    }

    protected boolean swipedFastEnough() {
        float velocity = getVelocity(this.mVelocityTracker);
        float translation = getTranslation(this.mCurrView);
        if (Math.abs(velocity) > getEscapeVelocity()) {
            return ((velocity > 0.0f ? 1 : (velocity == 0.0f ? 0 : -1)) > 0) == ((translation > 0.0f ? 1 : (translation == 0.0f ? 0 : -1)) > 0);
        }
        return false;
    }

    protected boolean handleUpEvent(MotionEvent motionEvent, View view, float f, float f2) {
        return false;
    }

    public interface Callback {
        boolean canChildBeDismissed(View view);

        View getChildAtPosition(MotionEvent motionEvent);

        float getFalsingThresholdFactor();

        boolean isAntiFalsingNeeded();

        void onBeginDrag(View view);

        void onChildDismissed(View view);

        void onChildSnappedBack(View view, float f);

        void onDragCancelled(View view);

        boolean updateSwipeProgress(View view, boolean z, float f);

        default boolean canChildBeDragged(View view) {
            return true;
        }
    }
}
