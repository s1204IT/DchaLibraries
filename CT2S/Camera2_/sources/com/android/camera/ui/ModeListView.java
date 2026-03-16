package com.android.camera.ui;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.TimeInterpolator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.SystemClock;
import android.support.v4.view.MotionEventCompat;
import android.support.v4.view.ViewCompat;
import android.util.AttributeSet;
import android.util.SparseBooleanArray;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import com.android.camera.CaptureLayoutHelper;
import com.android.camera.app.CameraAppUI;
import com.android.camera.debug.Log;
import com.android.camera.ui.ModeSelectorItem;
import com.android.camera.ui.PreviewStatusListener;
import com.android.camera.util.CameraUtil;
import com.android.camera.util.Gusterpolator;
import com.android.camera.util.UsageStatistics;
import com.android.camera.widget.AnimationEffects;
import com.android.camera.widget.SettingsCling;
import com.android.camera2.R;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class ModeListView extends FrameLayout implements ModeSelectorItem.VisibleWidthChangedListener, PreviewStatusListener.PreviewAreaChangedListener {
    static final boolean $assertionsDisabled;
    private static final int BACKGROUND_TRANSPARENTCY = 153;
    private static final int DEFAULT_DURATION_MS = 200;
    private static final int DELAY_MS = 30;
    private static final int FLY_IN_DURATION_MS = 0;
    private static final int FLY_OUT_DURATION_MS = 850;
    private static final int HIDE_SHIMMY_DELAY_MS = 1000;
    private static final int HOLD_DURATION_MS = 0;
    private static final int NO_ITEM_SELECTED = -1;
    private static final int PREVIEW_DOWN_SAMPLE_FACTOR = 4;
    private static final float SCROLL_FACTOR = 0.5f;
    private static final int SCROLL_INTERVAL_MS = 50;
    private static final float SLOW_ZONE_PERCENTAGE = 0.2f;
    private static final float SNAP_BACK_THRESHOLD_RATIO = 0.33f;
    private static final int START_DELAY_MS = 100;
    private static final Log.Tag TAG;
    private static final int TOTAL_DURATION_MS = 850;
    private static final float VELOCITY_THRESHOLD = 2.0f;
    private final TimeInterpolator mAccordionInterpolator;
    private AnimatorSet mAnimatorSet;
    private CaptureLayoutHelper mCaptureLayoutHelper;
    private View mChildViewTouched;
    private final CurrentStateManager mCurrentStateManager;
    private long mCurrentTime;
    private int mFocusItem;
    private final GestureDetector mGestureDetector;
    private int mHeight;
    private int[] mInputPixels;
    private MotionEvent mLastChildTouchEvent;
    private long mLastDownTime;
    private long mLastScrollTime;
    private int mListBackgroundColor;
    private LinearLayout mListView;
    private float mModeListOpenFactor;
    private ModeListOpenListener mModeListOpenListener;
    private ModeSelectorItem[] mModeSelectorItems;
    private ModeSwitchListener mModeSwitchListener;
    private final GestureDetector.OnGestureListener mOnGestureListener;
    private int[] mOutputPixels;
    private final LinkedList<TimeBasedPosition> mPositionHistory;
    private CameraAppUI.CameraModuleScreenShotProvider mScreenShotProvider;
    private float mScrollTrendX;
    private float mScrollTrendY;
    private View mSettingsButton;
    private final int mSettingsButtonMargin;
    private SettingsCling mSettingsCling;
    private ArrayList<Integer> mSupportedModes;
    private int mTotalModes;
    private float mVelocityX;
    private ModeListVisibilityChangedListener mVisibilityChangedListener;
    private int mVisibleWidth;
    private int mWidth;

    public interface ModeListOpenListener {
        void onModeListClosed();

        void onModeListOpenProgress(float f);

        void onOpenFullScreen();
    }

    public interface ModeSwitchListener {
        int getCurrentModeIndex();

        void onModeSelected(int i);

        void onSettingsSelected();
    }

    static {
        $assertionsDisabled = !ModeListView.class.desiredAssertionStatus();
        TAG = new Log.Tag("ModeListView");
    }

    private class CurrentStateManager {
        private ModeListState mCurrentState;

        private CurrentStateManager() {
        }

        ModeListState getCurrentState() {
            return this.mCurrentState;
        }

        void setCurrentState(ModeListState state) {
            this.mCurrentState = state;
            state.onCurrentState();
        }
    }

    private abstract class ModeListState implements GestureDetector.OnGestureListener {
        protected AnimationEffects mCurrentAnimationEffects;

        private ModeListState() {
            this.mCurrentAnimationEffects = null;
        }

        public void onCurrentState() {
            ModeListView.this.showSettingsClingIfEnabled(false);
        }

        public void showSwitcherHint() {
        }

        public AnimationEffects getCurrentAnimationEffects() {
            return this.mCurrentAnimationEffects;
        }

        public boolean shouldHandleTouchEvent(MotionEvent ev) {
            return true;
        }

        public boolean onTouchEvent(MotionEvent ev) {
            return true;
        }

        public void onWindowFocusChanged(boolean hasFocus) {
        }

        public boolean onBackPressed() {
            return false;
        }

        public boolean onMenuPressed() {
            return false;
        }

        public boolean shouldHandleVisibilityChange(int visibility) {
            return true;
        }

        public void onItemSelected(ModeSelectorItem selectedItem) {
        }

        public void startModeSelectionAnimation() {
        }

        public void hide() {
        }

        public void hideAnimated() {
            hide();
        }

        @Override
        public boolean onDown(MotionEvent e) {
            return false;
        }

        @Override
        public void onShowPress(MotionEvent e) {
        }

        @Override
        public boolean onSingleTapUp(MotionEvent e) {
            return false;
        }

        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
            return false;
        }

        @Override
        public void onLongPress(MotionEvent e) {
        }

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            return false;
        }

        public void setfullhiddenState() {
        }
    }

    private class FullyHiddenState extends ModeListState {
        private Animator mAnimator;
        private boolean mShouldBeVisible;

        public FullyHiddenState() {
            super();
            this.mAnimator = null;
            this.mShouldBeVisible = false;
            ModeListView.this.reset();
        }

        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
            this.mShouldBeVisible = true;
            ModeListView.this.resetModeSelectors();
            ModeListView.this.mCurrentStateManager.setCurrentState(ModeListView.this.new ScrollingState());
            return true;
        }

        @Override
        public void showSwitcherHint() {
            this.mShouldBeVisible = true;
            ModeListView.this.mCurrentStateManager.setCurrentState(ModeListView.this.new ShimmyState());
        }

        @Override
        public boolean shouldHandleTouchEvent(MotionEvent ev) {
            return true;
        }

        @Override
        public boolean onTouchEvent(MotionEvent ev) {
            if (ev.getActionMasked() == 0) {
                ModeListView.this.mFocusItem = ModeListView.this.getFocusItem(ev.getX(), ev.getY());
                ModeListView.this.setSwipeMode(true);
            }
            return true;
        }

        @Override
        public boolean onMenuPressed() {
            if (this.mAnimator != null) {
                return false;
            }
            snapOpenAndShow();
            return true;
        }

        @Override
        public boolean shouldHandleVisibilityChange(int visibility) {
            if (this.mAnimator != null) {
                return false;
            }
            return visibility != 0 || this.mShouldBeVisible;
        }

        private void snapOpenAndShow() {
            this.mShouldBeVisible = true;
            ModeListView.this.setVisibility(0);
            this.mAnimator = ModeListView.this.snapToFullScreen();
            if (this.mAnimator == null) {
                ModeListView.this.mCurrentStateManager.setCurrentState(new FullyShownState());
                UsageStatistics.instance().controlUsed(10000);
            } else {
                this.mAnimator.addListener(new Animator.AnimatorListener() {
                    @Override
                    public void onAnimationStart(Animator animation) {
                    }

                    @Override
                    public void onAnimationEnd(Animator animation) {
                        FullyHiddenState.this.mAnimator = null;
                        ModeListView.this.mCurrentStateManager.setCurrentState(new FullyShownState());
                    }

                    @Override
                    public void onAnimationCancel(Animator animation) {
                    }

                    @Override
                    public void onAnimationRepeat(Animator animation) {
                    }
                });
            }
        }

        @Override
        public void onCurrentState() {
            super.onCurrentState();
            ModeListView.this.announceForAccessibility(ModeListView.this.getContext().getResources().getString(R.string.accessibility_mode_list_hidden));
        }
    }

    private class FullyShownState extends ModeListState {
        private Animator mAnimator;

        private FullyShownState() {
            super();
            this.mAnimator = null;
        }

        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
            if (distanceX > 0.0f) {
                ModeListView.this.cancelForwardingTouchEvent();
                ModeListView.this.mCurrentStateManager.setCurrentState(ModeListView.this.new ScrollingState());
                return true;
            }
            return true;
        }

        @Override
        public boolean shouldHandleTouchEvent(MotionEvent ev) {
            return this.mAnimator == null || !this.mAnimator.isRunning();
        }

        @Override
        public boolean onTouchEvent(MotionEvent ev) {
            if (ev.getActionMasked() == 0) {
                ModeListView.this.mFocusItem = -1;
                ModeListView.this.setSwipeMode(false);
                if (ModeListView.this.isTouchInsideList(ev)) {
                    ModeListView.this.mChildViewTouched = ModeListView.this.mModeSelectorItems[ModeListView.this.getFocusItem(ev.getX(), ev.getY())];
                }
            }
            ModeListView.this.forwardTouchEventToChild(ev);
            return true;
        }

        @Override
        public boolean onSingleTapUp(MotionEvent ev) {
            if (ModeListView.this.isTouchInsideList(ev)) {
                return true;
            }
            snapBackAndHide();
            return false;
        }

        @Override
        public boolean onBackPressed() {
            snapBackAndHide();
            return true;
        }

        @Override
        public boolean onMenuPressed() {
            snapBackAndHide();
            return true;
        }

        @Override
        public void onItemSelected(ModeSelectorItem selectedItem) {
            ModeListView.this.mCurrentStateManager.setCurrentState(ModeListView.this.new SelectedState(selectedItem));
        }

        private void snapBackAndHide() {
            this.mAnimator = ModeListView.this.snapBack(true);
            if (this.mAnimator == null) {
                ModeListView.this.mCurrentStateManager.setCurrentState(ModeListView.this.new FullyHiddenState());
            } else {
                this.mAnimator.addListener(new Animator.AnimatorListener() {
                    @Override
                    public void onAnimationStart(Animator animation) {
                    }

                    @Override
                    public void onAnimationEnd(Animator animation) {
                        FullyShownState.this.mAnimator = null;
                        ModeListView.this.mCurrentStateManager.setCurrentState(ModeListView.this.new FullyHiddenState());
                    }

                    @Override
                    public void onAnimationCancel(Animator animation) {
                    }

                    @Override
                    public void onAnimationRepeat(Animator animation) {
                    }
                });
            }
        }

        @Override
        public void hide() {
            if (this.mAnimator == null) {
                ModeListView.this.mCurrentStateManager.setCurrentState(ModeListView.this.new FullyHiddenState());
            } else {
                this.mAnimator.cancel();
            }
        }

        @Override
        public void onCurrentState() {
            ModeListView.this.announceForAccessibility(ModeListView.this.getContext().getResources().getString(R.string.accessibility_mode_list_shown));
            ModeListView.this.showSettingsClingIfEnabled(true);
        }
    }

    private class ShimmyState extends ModeListState {
        private Animator mAnimator;
        private final Runnable mHideShimmy;
        private boolean mStartHidingShimmyWhenWindowGainsFocus;

        public ShimmyState() {
            super();
            this.mStartHidingShimmyWhenWindowGainsFocus = false;
            this.mAnimator = null;
            this.mHideShimmy = new Runnable() {
                @Override
                public void run() {
                    ShimmyState.this.startHidingShimmy();
                }
            };
            ModeListView.this.setVisibility(0);
            ModeListView.this.mSettingsButton.setVisibility(4);
            ModeListView.this.mModeListOpenFactor = 0.0f;
            ModeListView.this.onModeListOpenRatioUpdate(0.0f);
            int maxVisibleWidth = ModeListView.this.mModeSelectorItems[0].getMaxVisibleWidth();
            for (int i = 0; i < ModeListView.this.mModeSelectorItems.length; i++) {
                ModeListView.this.mModeSelectorItems[i].setVisibleWidth(maxVisibleWidth);
            }
            if (ModeListView.this.hasWindowFocus()) {
                hideShimmyWithDelay();
            } else {
                this.mStartHidingShimmyWhenWindowGainsFocus = true;
            }
        }

        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
            cancelAnimation();
            ModeListView.this.cancelForwardingTouchEvent();
            ModeListView.this.mCurrentStateManager.setCurrentState(ModeListView.this.new ScrollingState());
            UsageStatistics.instance().controlUsed(10000);
            return true;
        }

        @Override
        public boolean shouldHandleTouchEvent(MotionEvent ev) {
            if (ev.getActionMasked() != 0) {
                return true;
            }
            if (ModeListView.this.isTouchInsideList(ev) && ev.getX() <= ModeListView.this.mModeSelectorItems[0].getMaxVisibleWidth()) {
                ModeListView.this.mChildViewTouched = ModeListView.this.mModeSelectorItems[ModeListView.this.getFocusItem(ev.getX(), ev.getY())];
                return true;
            }
            if (ModeListView.this.mLastDownTime == ev.getDownTime()) {
                return true;
            }
            ModeListView.this.mLastDownTime = ev.getDownTime();
            return false;
        }

        @Override
        public boolean onTouchEvent(MotionEvent ev) {
            if (ev.getActionMasked() == 0 && ev.getActionMasked() == 0) {
                ModeListView.this.mFocusItem = ModeListView.this.getFocusItem(ev.getX(), ev.getY());
                ModeListView.this.setSwipeMode(true);
            }
            ModeListView.this.forwardTouchEventToChild(ev);
            return true;
        }

        @Override
        public void onItemSelected(ModeSelectorItem selectedItem) {
            cancelAnimation();
            ModeListView.this.mCurrentStateManager.setCurrentState(ModeListView.this.new SelectedState(selectedItem));
        }

        private void hideShimmyWithDelay() {
            ModeListView.this.postDelayed(this.mHideShimmy, 1000L);
        }

        @Override
        public void onWindowFocusChanged(boolean hasFocus) {
            if (this.mStartHidingShimmyWhenWindowGainsFocus && hasFocus) {
                this.mStartHidingShimmyWhenWindowGainsFocus = false;
                hideShimmyWithDelay();
            }
        }

        private void startHidingShimmy() {
            if (this.mAnimator == null) {
                int maxVisibleWidth = ModeListView.this.mModeSelectorItems[0].getMaxVisibleWidth();
                this.mAnimator = ModeListView.this.animateListToWidth(-100, 850, Gusterpolator.INSTANCE, maxVisibleWidth, 0);
                this.mAnimator.addListener(new Animator.AnimatorListener() {
                    private boolean mSuccess = true;

                    @Override
                    public void onAnimationStart(Animator animation) {
                    }

                    @Override
                    public void onAnimationEnd(Animator animation) {
                        ShimmyState.this.mAnimator = null;
                        ShimmyState.this.onAnimationEnd(this.mSuccess);
                    }

                    @Override
                    public void onAnimationCancel(Animator animation) {
                        this.mSuccess = false;
                    }

                    @Override
                    public void onAnimationRepeat(Animator animation) {
                    }
                });
            }
        }

        private void cancelAnimation() {
            ModeListView.this.removeCallbacks(this.mHideShimmy);
            if (this.mAnimator != null && this.mAnimator.isRunning()) {
                this.mAnimator.cancel();
            } else {
                this.mAnimator = null;
                onAnimationEnd(false);
            }
        }

        @Override
        public void onCurrentState() {
            super.onCurrentState();
            ModeListView.this.disableA11yOnModeSelectorItems();
        }

        private void onAnimationEnd(boolean success) {
            ModeListView.this.mSettingsButton.setVisibility(0);
            if (success) {
                ModeListView.this.enableA11yOnModeSelectorItems();
                ModeListView.this.mModeListOpenFactor = 1.0f;
                ModeListView.this.mCurrentStateManager.setCurrentState(ModeListView.this.new FullyHiddenState());
            } else {
                final ValueAnimator openFactorAnimator = ValueAnimator.ofFloat(ModeListView.this.mModeListOpenFactor, 1.0f);
                openFactorAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                    @Override
                    public void onAnimationUpdate(ValueAnimator animation) {
                        ModeListView.this.mModeListOpenFactor = ((Float) openFactorAnimator.getAnimatedValue()).floatValue();
                        ModeListView.this.onVisibleWidthChanged(ModeListView.this.mVisibleWidth);
                    }
                });
                openFactorAnimator.addListener(new Animator.AnimatorListener() {
                    @Override
                    public void onAnimationStart(Animator animation) {
                    }

                    @Override
                    public void onAnimationEnd(Animator animation) {
                        ModeListView.this.mModeListOpenFactor = 1.0f;
                    }

                    @Override
                    public void onAnimationCancel(Animator animation) {
                    }

                    @Override
                    public void onAnimationRepeat(Animator animation) {
                    }
                });
                openFactorAnimator.start();
            }
        }

        @Override
        public void hide() {
            cancelAnimation();
            ModeListView.this.mCurrentStateManager.setCurrentState(ModeListView.this.new FullyHiddenState());
        }

        @Override
        public void hideAnimated() {
            cancelAnimation();
            ModeListView.this.animateListToWidth(0).addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    ModeListView.this.mCurrentStateManager.setCurrentState(ModeListView.this.new FullyHiddenState());
                }
            });
        }
    }

    private class ScrollingState extends ModeListState {
        private Animator mAnimator;

        public ScrollingState() {
            super();
            this.mAnimator = null;
            ModeListView.this.setVisibility(0);
        }

        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
            ModeListView.this.scroll(ModeListView.this.mFocusItem, distanceX * ModeListView.SCROLL_FACTOR, ModeListView.SCROLL_FACTOR * distanceY);
            return true;
        }

        @Override
        public boolean shouldHandleTouchEvent(MotionEvent ev) {
            return this.mAnimator == null;
        }

        @Override
        public boolean onTouchEvent(MotionEvent ev) {
            if (ev.getActionMasked() == 1 || ev.getActionMasked() == 3) {
                final boolean shouldSnapBack = ModeListView.this.shouldSnapBack();
                if (shouldSnapBack) {
                    this.mAnimator = ModeListView.this.snapBack();
                } else {
                    this.mAnimator = ModeListView.this.snapToFullScreen();
                }
                this.mAnimator.addListener(new Animator.AnimatorListener() {
                    @Override
                    public void onAnimationStart(Animator animation) {
                    }

                    @Override
                    public void onAnimationEnd(Animator animation) {
                        ScrollingState.this.mAnimator = null;
                        ModeListView.this.mFocusItem = -1;
                        if (shouldSnapBack) {
                            ModeListView.this.mCurrentStateManager.setCurrentState(ModeListView.this.new FullyHiddenState());
                        } else {
                            ModeListView.this.mCurrentStateManager.setCurrentState(new FullyShownState());
                            UsageStatistics.instance().controlUsed(10000);
                        }
                    }

                    @Override
                    public void onAnimationCancel(Animator animation) {
                    }

                    @Override
                    public void onAnimationRepeat(Animator animation) {
                    }
                });
            }
            return true;
        }
    }

    private class SelectedState extends ModeListState {
        public SelectedState(ModeSelectorItem selectedItem) {
            super();
            int modeId = selectedItem.getModeId();
            for (int i = 0; i < ModeListView.this.mModeSelectorItems.length; i++) {
                ModeListView.this.mModeSelectorItems[i].setSelected(false);
            }
            PeepholeAnimationEffect effect = ModeListView.this.new PeepholeAnimationEffect();
            effect.setSize(ModeListView.this.mWidth, ModeListView.this.mHeight);
            int[] location = new int[2];
            selectedItem.getIconCenterLocationInWindow(location);
            int iconX = location[0];
            int iconY = location[1];
            ModeListView.this.getLocationInWindow(location);
            int iconX2 = iconX - location[0];
            int iconY2 = iconY - location[1];
            effect.setAnimationStartingPosition(iconX2, iconY2);
            effect.setModeSpecificColor(selectedItem.getHighlightColor());
            if (ModeListView.this.mScreenShotProvider != null) {
                effect.setBackground(ModeListView.this.mScreenShotProvider.getPreviewFrame(4), ModeListView.this.mCaptureLayoutHelper.getPreviewRect());
                effect.setBackgroundOverlay(ModeListView.this.mScreenShotProvider.getPreviewOverlayAndControls());
            }
            this.mCurrentAnimationEffects = effect;
            effect.startFadeoutAnimation(null, selectedItem, iconX2, iconY2, modeId);
            ModeListView.this.invalidate();
        }

        @Override
        public boolean shouldHandleTouchEvent(MotionEvent ev) {
            return false;
        }

        @Override
        public void startModeSelectionAnimation() {
            this.mCurrentAnimationEffects.startAnimation(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    SelectedState.this.mCurrentAnimationEffects = null;
                    ModeListView.this.mCurrentStateManager.setCurrentState(ModeListView.this.new FullyHiddenState());
                }
            });
        }

        @Override
        public void hide() {
            if (!this.mCurrentAnimationEffects.cancelAnimation()) {
                this.mCurrentAnimationEffects = null;
                ModeListView.this.mCurrentStateManager.setCurrentState(ModeListView.this.new FullyHiddenState());
            }
        }

        @Override
        public void setfullhiddenState() {
            ModeListView.this.mCurrentStateManager.setCurrentState(ModeListView.this.new FullyHiddenState());
        }
    }

    public static abstract class ModeListVisibilityChangedListener {
        private Boolean mCurrentVisibility = null;

        public abstract void onVisibilityChanged(boolean z);

        private void onVisibilityEvent(boolean visible) {
            if (this.mCurrentVisibility == null || this.mCurrentVisibility.booleanValue() != visible) {
                this.mCurrentVisibility = Boolean.valueOf(visible);
                onVisibilityChanged(visible);
            }
        }
    }

    private static class TimeBasedPosition {
        private final float mPosition;
        private final long mTimeStamp;

        public TimeBasedPosition(float position, long time) {
            this.mPosition = position;
            this.mTimeStamp = time;
        }

        public float getPosition() {
            return this.mPosition;
        }

        public long getTimeStamp() {
            return this.mTimeStamp;
        }
    }

    private void onItemSelected(ModeSelectorItem selectedItem) {
        this.mCurrentStateManager.getCurrentState().onItemSelected(selectedItem);
    }

    private boolean isTouchInsideList(MotionEvent ev) {
        float x = ev.getX() - this.mListView.getX();
        float y = ev.getY() - this.mListView.getY();
        return x >= 0.0f && x <= ((float) this.mListView.getWidth()) && y >= 0.0f && y <= ((float) this.mListView.getHeight());
    }

    public ModeListView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mCurrentStateManager = new CurrentStateManager();
        this.mFocusItem = -1;
        this.mScreenShotProvider = null;
        this.mModeListOpenFactor = 1.0f;
        this.mChildViewTouched = null;
        this.mLastChildTouchEvent = null;
        this.mVisibleWidth = 0;
        this.mScrollTrendX = 0.0f;
        this.mScrollTrendY = 0.0f;
        this.mModeSwitchListener = null;
        this.mPositionHistory = new LinkedList<>();
        this.mLastDownTime = 0L;
        this.mCaptureLayoutHelper = null;
        this.mSettingsCling = null;
        this.mAccordionInterpolator = new TimeInterpolator() {
            @Override
            public float getInterpolation(float input) {
                if (input == 0.0f) {
                    return 0.0f;
                }
                if (input < 1.0f) {
                    float result = Gusterpolator.INSTANCE.getInterpolation(input / 1.0f);
                    return result * ModeListView.SCROLL_FACTOR;
                }
                if (input < 1.0f) {
                    return ModeListView.SCROLL_FACTOR;
                }
                float result2 = Gusterpolator.INSTANCE.getInterpolation((input - 1.0f) / (1.0f - 1.0f));
                return (result2 * ModeListView.SCROLL_FACTOR) + ModeListView.SCROLL_FACTOR;
            }
        };
        this.mOnGestureListener = new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
                ModeListView.this.mCurrentStateManager.getCurrentState().onScroll(e1, e2, distanceX, distanceY);
                ModeListView.this.mLastScrollTime = System.currentTimeMillis();
                return true;
            }

            @Override
            public boolean onSingleTapUp(MotionEvent ev) {
                ModeListView.this.mCurrentStateManager.getCurrentState().onSingleTapUp(ev);
                return true;
            }

            @Override
            public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
                ModeListView.this.mVelocityX = (velocityX / 1000.0f) * ModeListView.SCROLL_FACTOR;
                ModeListView.this.mCurrentStateManager.getCurrentState().onFling(e1, e2, velocityX, velocityY);
                return true;
            }

            @Override
            public boolean onDown(MotionEvent ev) {
                ModeListView.this.mVelocityX = 0.0f;
                ModeListView.this.mCurrentStateManager.getCurrentState().onDown(ev);
                return true;
            }
        };
        this.mGestureDetector = new GestureDetector(context, this.mOnGestureListener);
        this.mListBackgroundColor = getResources().getColor(R.color.mode_list_background);
        this.mSettingsButtonMargin = getResources().getDimensionPixelSize(R.dimen.mode_list_settings_icon_margin);
    }

    private void disableA11yOnModeSelectorItems() {
        View[] arr$ = this.mModeSelectorItems;
        for (View selectorItem : arr$) {
            selectorItem.setImportantForAccessibility(2);
        }
    }

    private void enableA11yOnModeSelectorItems() {
        View[] arr$ = this.mModeSelectorItems;
        for (View selectorItem : arr$) {
            selectorItem.setImportantForAccessibility(0);
        }
    }

    private void setBackgroundAlpha(int alpha) {
        int alpha2 = alpha & MotionEventCompat.ACTION_MASK;
        this.mListBackgroundColor &= ViewCompat.MEASURED_SIZE_MASK;
        this.mListBackgroundColor |= alpha2 << 24;
        setBackgroundColor(this.mListBackgroundColor);
    }

    public void init(List<Integer> modeIndexList) {
        int[] modeSequence = getResources().getIntArray(R.array.camera_modes_in_nav_drawer_if_supported);
        int[] visibleModes = getResources().getIntArray(R.array.camera_modes_always_visible);
        SparseBooleanArray modeIsSupported = new SparseBooleanArray();
        for (int i = 0; i < modeIndexList.size(); i++) {
            modeIsSupported.put(modeIndexList.get(i).intValue(), true);
        }
        for (int i2 : visibleModes) {
            modeIsSupported.put(i2, true);
        }
        this.mSupportedModes = new ArrayList<>();
        for (int mode : modeSequence) {
            if (modeIsSupported.get(mode, false)) {
                this.mSupportedModes.add(Integer.valueOf(mode));
            }
        }
        this.mTotalModes = this.mSupportedModes.size();
        initializeModeSelectorItems();
        this.mSettingsButton = findViewById(R.id.settings_button);
        this.mSettingsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ModeListView.this.post(new Runnable() {
                    @Override
                    public void run() {
                        ModeListView.this.mModeSwitchListener.onSettingsSelected();
                    }
                });
            }
        });
        onModeListOpenRatioUpdate(0.0f);
        if (this.mCurrentStateManager.getCurrentState() == null) {
            this.mCurrentStateManager.setCurrentState(new FullyHiddenState());
        }
    }

    public void setCameraModuleScreenShotProvider(CameraAppUI.CameraModuleScreenShotProvider provider) {
        this.mScreenShotProvider = provider;
    }

    private void initializeModeSelectorItems() {
        this.mModeSelectorItems = new ModeSelectorItem[this.mTotalModes];
        LayoutInflater inflater = (LayoutInflater) getContext().getSystemService("layout_inflater");
        this.mListView = (LinearLayout) findViewById(R.id.mode_list);
        for (int i = 0; i < this.mTotalModes; i++) {
            final ModeSelectorItem selectorItem = (ModeSelectorItem) inflater.inflate(R.layout.mode_selector, (ViewGroup) null);
            this.mListView.addView(selectorItem);
            if (i == 0) {
                selectorItem.setPadding(selectorItem.getPaddingLeft(), 0, selectorItem.getPaddingRight(), selectorItem.getPaddingBottom());
            }
            if (i == this.mTotalModes - 1) {
                selectorItem.setPadding(selectorItem.getPaddingLeft(), selectorItem.getPaddingTop(), selectorItem.getPaddingRight(), 0);
            }
            int modeId = getModeIndex(i);
            selectorItem.setHighlightColor(getResources().getColor(CameraUtil.getCameraThemeColorId(modeId, getContext())));
            selectorItem.setImageResource(CameraUtil.getCameraModeIconResId(modeId, getContext()));
            selectorItem.setText(CameraUtil.getCameraModeText(modeId, getContext()));
            selectorItem.setContentDescription(CameraUtil.getCameraModeContentDescription(modeId, getContext()));
            selectorItem.setModeId(modeId);
            selectorItem.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    ModeListView.this.onItemSelected(selectorItem);
                }
            });
            this.mModeSelectorItems[i] = selectorItem;
        }
        this.mModeSelectorItems[this.mTotalModes - 1].setVisibleWidthChangedListener(this);
        resetModeSelectors();
    }

    private int getModeIndex(int modeSelectorIndex) {
        if (modeSelectorIndex < this.mTotalModes && modeSelectorIndex >= 0) {
            return this.mSupportedModes.get(modeSelectorIndex).intValue();
        }
        Log.e(TAG, "Invalid mode selector index: " + modeSelectorIndex + ", total modes: " + this.mTotalModes);
        return getResources().getInteger(R.integer.camera_mode_photo);
    }

    private void onModeSelected(int modeIndex) {
        if (this.mModeSwitchListener != null) {
            this.mModeSwitchListener.onModeSelected(modeIndex);
        }
    }

    public void setModeSwitchListener(ModeSwitchListener listener) {
        this.mModeSwitchListener = listener;
    }

    public void setModeListOpenListener(ModeListOpenListener listener) {
        this.mModeListOpenListener = listener;
    }

    public void setVisibilityChangedListener(ModeListVisibilityChangedListener listener) {
        this.mVisibilityChangedListener = listener;
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        if (ev.getActionMasked() == 0) {
            this.mChildViewTouched = null;
        }
        if (!this.mCurrentStateManager.getCurrentState().shouldHandleTouchEvent(ev)) {
            return false;
        }
        getParent().requestDisallowInterceptTouchEvent(true);
        super.onTouchEvent(ev);
        this.mGestureDetector.onTouchEvent(ev);
        this.mCurrentStateManager.getCurrentState().onTouchEvent(ev);
        return true;
    }

    private void forwardTouchEventToChild(MotionEvent ev) {
        if (this.mChildViewTouched != null) {
            float x = ev.getX() - this.mListView.getX();
            float y = ev.getY() - this.mListView.getY();
            this.mLastChildTouchEvent = MotionEvent.obtain(ev);
            this.mLastChildTouchEvent.setLocation(x - this.mChildViewTouched.getLeft(), y - this.mChildViewTouched.getTop());
            this.mChildViewTouched.onTouchEvent(this.mLastChildTouchEvent);
        }
    }

    private void setSwipeMode(boolean swipeIn) {
        for (int i = 0; i < this.mModeSelectorItems.length; i++) {
            this.mModeSelectorItems[i].onSwipeModeChanged(swipeIn);
        }
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        this.mWidth = right - left;
        this.mHeight = ((bottom - top) - getPaddingTop()) - getPaddingBottom();
        updateModeListLayout();
        if (this.mCurrentStateManager.getCurrentState().getCurrentAnimationEffects() != null) {
            this.mCurrentStateManager.getCurrentState().getCurrentAnimationEffects().setSize(this.mWidth, this.mHeight);
        }
    }

    public void setCaptureLayoutHelper(CaptureLayoutHelper helper) {
        this.mCaptureLayoutHelper = helper;
    }

    @Override
    public void onPreviewAreaChanged(RectF previewArea) {
        if (getVisibility() == 0 && !hasWindowFocus()) {
            updateModeListLayout();
        }
    }

    private void updateModeListLayout() {
        if (this.mCaptureLayoutHelper == null) {
            Log.e(TAG, "Capture layout helper needs to be set first.");
            return;
        }
        RectF uncoveredPreviewArea = this.mCaptureLayoutHelper.getUncoveredPreviewRect();
        this.mListView.setTranslationX(uncoveredPreviewArea.left);
        this.mListView.setTranslationY(uncoveredPreviewArea.centerY() - (this.mListView.getMeasuredHeight() / 2));
        updateSettingsButtonLayout(uncoveredPreviewArea);
    }

    private void updateSettingsButtonLayout(RectF uncoveredPreviewArea) {
        if (this.mWidth > this.mHeight) {
            this.mSettingsButton.setTranslationX((uncoveredPreviewArea.right - this.mSettingsButtonMargin) - this.mSettingsButton.getMeasuredWidth());
            this.mSettingsButton.setTranslationY(uncoveredPreviewArea.top + this.mSettingsButtonMargin);
        } else {
            this.mSettingsButton.setTranslationX((uncoveredPreviewArea.right - this.mSettingsButtonMargin) - this.mSettingsButton.getMeasuredWidth());
            this.mSettingsButton.setTranslationY((uncoveredPreviewArea.bottom - this.mSettingsButtonMargin) - this.mSettingsButton.getMeasuredHeight());
        }
        if (this.mSettingsCling != null) {
            this.mSettingsCling.updatePosition(this.mSettingsButton);
        }
    }

    @Override
    public void draw(Canvas canvas) {
        ModeListState currentState = this.mCurrentStateManager.getCurrentState();
        AnimationEffects currentEffects = currentState.getCurrentAnimationEffects();
        if (currentEffects != null) {
            currentEffects.drawBackground(canvas);
            if (currentEffects.shouldDrawSuper()) {
                super.draw(canvas);
            }
            currentEffects.drawForeground(canvas);
            return;
        }
        super.draw(canvas);
    }

    public void setShouldShowSettingsCling(boolean show) {
        if (show) {
            if (this.mSettingsCling == null) {
                inflate(getContext(), R.layout.settings_cling, this);
                this.mSettingsCling = (SettingsCling) findViewById(R.id.settings_cling);
                return;
            }
            return;
        }
        if (this.mSettingsCling != null) {
            removeView(this.mSettingsCling);
            this.mSettingsCling = null;
        }
    }

    private void showSettingsClingIfEnabled(boolean show) {
        if (this.mSettingsCling != null) {
            int visibility = show ? 0 : 4;
            this.mSettingsCling.setVisibility(visibility);
        }
    }

    public void showModeSwitcherHint() {
        this.mCurrentStateManager.getCurrentState().showSwitcherHint();
    }

    public void setfullhiddenState() {
        this.mCurrentStateManager.getCurrentState().setfullhiddenState();
    }

    public void hide() {
        this.mCurrentStateManager.getCurrentState().hide();
    }

    public void hideAnimated() {
        this.mCurrentStateManager.getCurrentState().hideAnimated();
    }

    private void resetModeSelectors() {
        for (int i = 0; i < this.mModeSelectorItems.length; i++) {
            this.mModeSelectorItems[i].setVisibleWidth(0);
        }
    }

    private boolean isRunningAccordionAnimation() {
        return this.mAnimatorSet != null && this.mAnimatorSet.isRunning();
    }

    private int getFocusItem(float x, float y) {
        float x2 = x - this.mListView.getX();
        float y2 = y - this.mListView.getY();
        for (int i = 0; i < this.mModeSelectorItems.length; i++) {
            if (y2 <= this.mModeSelectorItems[i].getBottom()) {
                return i;
            }
        }
        int i2 = this.mModeSelectorItems.length - 1;
        return i2;
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        this.mCurrentStateManager.getCurrentState().onWindowFocusChanged(hasFocus);
    }

    @Override
    public void onVisibilityChanged(View v, int visibility) {
        super.onVisibilityChanged(v, visibility);
        if (visibility == 0) {
            if (this.mModeSwitchListener != null) {
                int modeId = this.mModeSwitchListener.getCurrentModeIndex();
                int parentMode = CameraUtil.getCameraModeParentModeId(modeId, getContext());
                for (int i = 0; i < this.mSupportedModes.size(); i++) {
                    if (this.mSupportedModes.get(i).intValue() == parentMode) {
                        this.mModeSelectorItems[i].setSelected(true);
                    }
                }
            }
            updateModeListLayout();
        } else {
            if (this.mModeSelectorItems != null) {
                for (int i2 = 0; i2 < this.mModeSelectorItems.length; i2++) {
                    this.mModeSelectorItems[i2].setSelected(false);
                }
            }
            if (this.mModeListOpenListener != null) {
                this.mModeListOpenListener.onModeListClosed();
            }
        }
        if (this.mVisibilityChangedListener != null) {
            this.mVisibilityChangedListener.onVisibilityEvent(getVisibility() == 0);
        }
    }

    @Override
    public void setVisibility(int visibility) {
        ModeListState currentState = this.mCurrentStateManager.getCurrentState();
        if (currentState == null || currentState.shouldHandleVisibilityChange(visibility)) {
            super.setVisibility(visibility);
        }
    }

    private void scroll(int itemId, float deltaX, float deltaY) {
        float longestWidth;
        this.mScrollTrendX = (this.mScrollTrendX * 0.3f) + (deltaX * 0.7f);
        this.mScrollTrendY = (this.mScrollTrendY * 0.3f) + (deltaY * 0.7f);
        this.mCurrentTime = SystemClock.uptimeMillis();
        if (itemId != -1) {
            longestWidth = this.mModeSelectorItems[itemId].getVisibleWidth();
        } else {
            longestWidth = this.mModeSelectorItems[0].getVisibleWidth();
        }
        int maxVisibleWidth = this.mModeSelectorItems[0].getMaxVisibleWidth();
        float newPosition = Math.max(Math.min(longestWidth - deltaX, getMaxMovementBasedOnPosition((int) longestWidth, maxVisibleWidth)), 0.0f);
        insertNewPosition(newPosition, this.mCurrentTime);
        for (int i = 0; i < this.mModeSelectorItems.length; i++) {
            this.mModeSelectorItems[i].setVisibleWidth(calculateVisibleWidthForItem(i, (int) newPosition));
        }
    }

    private int calculateVisibleWidthForItem(int itemId, int longestWidth) {
        if (itemId != this.mFocusItem && this.mFocusItem != -1) {
            int delay = Math.abs(itemId - this.mFocusItem) * DELAY_MS;
            return (int) getPosition(this.mCurrentTime - ((long) delay), this.mModeSelectorItems[itemId].getVisibleWidth());
        }
        return longestWidth;
    }

    private void insertNewPosition(float position, long time) {
        this.mPositionHistory.add(new TimeBasedPosition(position, time));
        long timeCutoff = time - ((long) ((this.mTotalModes - 1) * DELAY_MS));
        while (this.mPositionHistory.size() > 0) {
            TimeBasedPosition historyPosition = this.mPositionHistory.getFirst();
            if (historyPosition.getTimeStamp() < timeCutoff) {
                this.mPositionHistory.removeFirst();
            } else {
                return;
            }
        }
    }

    private float getPosition(long time, float currentPosition) {
        int i = 0;
        while (i < this.mPositionHistory.size()) {
            TimeBasedPosition historyPosition = this.mPositionHistory.get(i);
            if (historyPosition.getTimeStamp() <= time) {
                i++;
            } else {
                if (i == 0) {
                    return (historyPosition.getPosition() * 0.2f) + ((1.0f - 0.2f) * currentPosition);
                }
                TimeBasedPosition prevTimeBasedPosition = this.mPositionHistory.get(i - 1);
                float fraction = (time - prevTimeBasedPosition.getTimeStamp()) / (historyPosition.getTimeStamp() - prevTimeBasedPosition.getTimeStamp());
                return ((historyPosition.getPosition() - prevTimeBasedPosition.getPosition()) * fraction) + prevTimeBasedPosition.getPosition();
            }
        }
        Log.e(TAG, "Invalid time input for getPosition(). time: " + time);
        if (this.mPositionHistory.size() == 0) {
            Log.e(TAG, "TimeBasedPosition history size is 0");
        } else {
            Log.e(TAG, "First position recorded at " + this.mPositionHistory.getFirst().getTimeStamp() + " , last position recorded at " + this.mPositionHistory.getLast().getTimeStamp());
        }
        if ($assertionsDisabled || i < this.mPositionHistory.size()) {
            return i;
        }
        throw new AssertionError();
    }

    private void reset() {
        resetModeSelectors();
        this.mScrollTrendX = 0.0f;
        this.mScrollTrendY = 0.0f;
        setVisibility(4);
    }

    @Override
    public void onVisibleWidthChanged(int visibleWidth) {
        this.mVisibleWidth = visibleWidth;
        int maxVisibleWidth = this.mModeSelectorItems[0].getMaxVisibleWidth();
        int visibleWidth2 = Math.min(maxVisibleWidth, visibleWidth);
        if (visibleWidth2 != maxVisibleWidth) {
            cancelForwardingTouchEvent();
        }
        float openRatio = visibleWidth2 / maxVisibleWidth;
        onModeListOpenRatioUpdate(this.mModeListOpenFactor * openRatio);
    }

    private void onModeListOpenRatioUpdate(float openRatio) {
        for (int i = 0; i < this.mModeSelectorItems.length; i++) {
            this.mModeSelectorItems[i].setTextAlpha(openRatio);
        }
        setBackgroundAlpha((int) (153.0f * openRatio));
        if (this.mModeListOpenListener != null) {
            this.mModeListOpenListener.onModeListOpenProgress(openRatio);
        }
        if (this.mSettingsButton != null) {
            this.mSettingsButton.setAlpha(openRatio);
        }
    }

    private void cancelForwardingTouchEvent() {
        if (this.mChildViewTouched != null) {
            this.mLastChildTouchEvent.setAction(3);
            this.mChildViewTouched.onTouchEvent(this.mLastChildTouchEvent);
            this.mChildViewTouched = null;
        }
    }

    @Override
    public void onWindowVisibilityChanged(int visibility) {
        super.onWindowVisibilityChanged(visibility);
        if (visibility != 0) {
            this.mCurrentStateManager.getCurrentState().hide();
        }
    }

    public boolean onMenuPressed() {
        return this.mCurrentStateManager.getCurrentState().onMenuPressed();
    }

    private void snap() {
        if (shouldSnapBack()) {
            snapBack();
        } else {
            snapToFullScreen();
        }
    }

    private boolean shouldSnapBack() {
        int itemId = Math.max(0, this.mFocusItem);
        if (Math.abs(this.mVelocityX) > VELOCITY_THRESHOLD) {
            return this.mVelocityX < 0.0f;
        }
        if (this.mModeSelectorItems[itemId].getVisibleWidth() >= this.mModeSelectorItems[itemId].getMaxVisibleWidth() * SNAP_BACK_THRESHOLD_RATIO) {
            return Math.abs(this.mScrollTrendX) > Math.abs(this.mScrollTrendY) && this.mScrollTrendX > 0.0f;
        }
        return true;
    }

    public Animator snapBack(boolean withAnimation) {
        if (withAnimation) {
            return this.mVelocityX > -1.0f ? animateListToWidth(0) : animateListToWidthAtVelocity(this.mVelocityX, 0);
        }
        setVisibility(4);
        resetModeSelectors();
        return null;
    }

    private Animator snapBack() {
        return snapBack(true);
    }

    private Animator snapToFullScreen() {
        Animator animator;
        int focusItem = this.mFocusItem == -1 ? 0 : this.mFocusItem;
        int fullWidth = this.mModeSelectorItems[focusItem].getMaxVisibleWidth();
        if (this.mVelocityX <= VELOCITY_THRESHOLD) {
            animator = animateListToWidth(fullWidth);
        } else {
            animator = animateListToWidthAtVelocity(VELOCITY_THRESHOLD, fullWidth);
        }
        if (this.mModeListOpenListener != null) {
            this.mModeListOpenListener.onOpenFullScreen();
        }
        return animator;
    }

    private Animator animateListToWidth(int... width) {
        return animateListToWidth(0, 200, null, width);
    }

    private Animator animateListToWidth(int delay, int duration, TimeInterpolator interpolator, int... width) {
        ObjectAnimator animator;
        if (this.mAnimatorSet != null && this.mAnimatorSet.isRunning()) {
            this.mAnimatorSet.end();
        }
        ArrayList<Animator> animators = new ArrayList<>();
        boolean animateModeItemsInOrder = true;
        if (delay < 0) {
            animateModeItemsInOrder = false;
            delay *= -1;
        }
        for (int i = 0; i < this.mTotalModes; i++) {
            if (animateModeItemsInOrder) {
                animator = ObjectAnimator.ofInt(this.mModeSelectorItems[i], "visibleWidth", width);
            } else {
                animator = ObjectAnimator.ofInt(this.mModeSelectorItems[(this.mTotalModes - 1) - i], "visibleWidth", width);
            }
            animator.setDuration(duration);
            animator.setStartDelay(i * delay);
            animators.add(animator);
        }
        this.mAnimatorSet = new AnimatorSet();
        this.mAnimatorSet.playTogether(animators);
        this.mAnimatorSet.setInterpolator(interpolator);
        this.mAnimatorSet.start();
        return this.mAnimatorSet;
    }

    private Animator animateListToWidthAtVelocity(float velocity, int width) {
        if (this.mAnimatorSet != null && this.mAnimatorSet.isRunning()) {
            this.mAnimatorSet.end();
        }
        ArrayList<Animator> animators = new ArrayList<>();
        if (this.mFocusItem != -1) {
            int focusItem = this.mFocusItem;
        }
        for (int i = 0; i < this.mTotalModes; i++) {
            ObjectAnimator animator = ObjectAnimator.ofInt(this.mModeSelectorItems[i], "visibleWidth", width);
            int duration = (int) (width / velocity);
            animator.setDuration(duration);
            animators.add(animator);
        }
        this.mAnimatorSet = new AnimatorSet();
        this.mAnimatorSet.playTogether(animators);
        this.mAnimatorSet.setInterpolator(null);
        this.mAnimatorSet.start();
        return this.mAnimatorSet;
    }

    public boolean onBackPressed() {
        return this.mCurrentStateManager.getCurrentState().onBackPressed();
    }

    public void startModeSelectionAnimation() {
        this.mCurrentStateManager.getCurrentState().startModeSelectionAnimation();
    }

    public float getMaxMovementBasedOnPosition(int lastVisibleWidth, int maxWidth) {
        float position;
        int timeElapsed = (int) (System.currentTimeMillis() - this.mLastScrollTime);
        if (timeElapsed > SCROLL_INTERVAL_MS) {
            timeElapsed = SCROLL_INTERVAL_MS;
        }
        int slowZone = (int) (maxWidth * 0.2f);
        if (lastVisibleWidth < maxWidth - slowZone) {
            position = (timeElapsed * VELOCITY_THRESHOLD) + lastVisibleWidth;
        } else {
            float percentageIntoSlowZone = (lastVisibleWidth - (maxWidth - slowZone)) / slowZone;
            float velocity = (1.0f - percentageIntoSlowZone) * VELOCITY_THRESHOLD;
            position = (timeElapsed * velocity) + lastVisibleWidth;
        }
        return Math.min(maxWidth, position);
    }

    private class PeepholeAnimationEffect extends AnimationEffects {
        private static final int PEEP_HOLE_ANIMATION_DURATION_MS = 500;
        private static final int UNSET = -1;
        private Bitmap mBackground;
        private Bitmap mBackgroundOverlay;
        private TouchCircleDrawable mCircleDrawable;
        private ValueAnimator mFadeOutAlphaAnimator;
        private ValueAnimator mPeepHoleAnimator;
        private ValueAnimator mRevealAlphaAnimator;
        private final Paint mMaskPaint = new Paint();
        private final RectF mBackgroundDrawArea = new RectF();
        private int mPeepHoleCenterX = -1;
        private int mPeepHoleCenterY = -1;
        private float mRadius = 0.0f;
        private Paint mCirclePaint = new Paint();
        private Paint mCoverPaint = new Paint();

        public PeepholeAnimationEffect() {
            this.mMaskPaint.setAlpha(0);
            this.mMaskPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
            this.mCirclePaint.setColor(0);
            this.mCirclePaint.setAlpha(0);
            this.mCoverPaint.setColor(0);
            this.mCoverPaint.setAlpha(0);
            setupAnimators();
        }

        private void setupAnimators() {
            this.mFadeOutAlphaAnimator = ValueAnimator.ofInt(0, MotionEventCompat.ACTION_MASK);
            this.mFadeOutAlphaAnimator.setDuration(100L);
            this.mFadeOutAlphaAnimator.setInterpolator(Gusterpolator.INSTANCE);
            this.mFadeOutAlphaAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    PeepholeAnimationEffect.this.mCoverPaint.setAlpha(((Integer) animation.getAnimatedValue()).intValue());
                    ModeListView.this.invalidate();
                }
            });
            this.mFadeOutAlphaAnimator.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationStart(Animator animation) {
                    ModeListView.this.setLayerType(2, null);
                }

                @Override
                public void onAnimationEnd(Animator animation) {
                    ModeListView.this.setLayerType(0, null);
                }
            });
            this.mRevealAlphaAnimator = ValueAnimator.ofInt(MotionEventCompat.ACTION_MASK, 0);
            this.mRevealAlphaAnimator.setDuration(500L);
            this.mRevealAlphaAnimator.setInterpolator(Gusterpolator.INSTANCE);
            this.mRevealAlphaAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    int alpha = ((Integer) animation.getAnimatedValue()).intValue();
                    PeepholeAnimationEffect.this.mCirclePaint.setAlpha(alpha);
                    PeepholeAnimationEffect.this.mCoverPaint.setAlpha(alpha);
                }
            });
            this.mRevealAlphaAnimator.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationStart(Animator animation) {
                    ModeListView.this.setLayerType(2, null);
                }

                @Override
                public void onAnimationEnd(Animator animation) {
                    ModeListView.this.setLayerType(0, null);
                }
            });
            int horizontalDistanceToFarEdge = Math.max(this.mPeepHoleCenterX, ModeListView.this.mWidth - this.mPeepHoleCenterX);
            int verticalDistanceToFarEdge = Math.max(this.mPeepHoleCenterY, ModeListView.this.mHeight - this.mPeepHoleCenterY);
            int endRadius = (int) Math.sqrt((horizontalDistanceToFarEdge * horizontalDistanceToFarEdge) + (verticalDistanceToFarEdge * verticalDistanceToFarEdge));
            int startRadius = ModeListView.this.getResources().getDimensionPixelSize(R.dimen.mode_selector_icon_block_width) / 2;
            this.mPeepHoleAnimator = ValueAnimator.ofFloat(startRadius, endRadius);
            this.mPeepHoleAnimator.setDuration(500L);
            this.mPeepHoleAnimator.setInterpolator(Gusterpolator.INSTANCE);
            this.mPeepHoleAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    PeepholeAnimationEffect.this.mRadius = ((Float) PeepholeAnimationEffect.this.mPeepHoleAnimator.getAnimatedValue()).floatValue();
                    ModeListView.this.invalidate();
                }
            });
            this.mPeepHoleAnimator.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationStart(Animator animation) {
                    ModeListView.this.setLayerType(2, null);
                }

                @Override
                public void onAnimationEnd(Animator animation) {
                    ModeListView.this.setLayerType(0, null);
                }
            });
            int size = ModeListView.this.getContext().getResources().getDimensionPixelSize(R.dimen.mode_selector_icon_block_width);
            this.mCircleDrawable = new TouchCircleDrawable(ModeListView.this.getContext().getResources());
            this.mCircleDrawable.setSize(size, size);
            this.mCircleDrawable.setUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    ModeListView.this.invalidate();
                }
            });
        }

        @Override
        public void setSize(int width, int height) {
            ModeListView.this.mWidth = width;
            ModeListView.this.mHeight = height;
        }

        @Override
        public boolean onTouchEvent(MotionEvent event) {
            return true;
        }

        @Override
        public void drawForeground(Canvas canvas) {
            if (this.mPeepHoleAnimator != null) {
                canvas.drawCircle(this.mPeepHoleCenterX, this.mPeepHoleCenterY, this.mRadius, this.mMaskPaint);
                canvas.drawCircle(this.mPeepHoleCenterX, this.mPeepHoleCenterY, this.mRadius, this.mCirclePaint);
            }
        }

        public void setAnimationStartingPosition(int x, int y) {
            this.mPeepHoleCenterX = x;
            this.mPeepHoleCenterY = y;
        }

        public void setModeSpecificColor(int color) {
            this.mCirclePaint.setColor(16777215 & color);
        }

        public void setBackground(Bitmap background, RectF drawArea) {
            this.mBackground = background;
            this.mBackgroundDrawArea.set(drawArea);
        }

        public void setBackgroundOverlay(Bitmap overlay) {
            this.mBackgroundOverlay = overlay;
        }

        @Override
        public void drawBackground(Canvas canvas) {
            if (this.mBackground != null && this.mBackgroundOverlay != null) {
                canvas.drawBitmap(this.mBackground, (Rect) null, this.mBackgroundDrawArea, (Paint) null);
                canvas.drawPaint(this.mCoverPaint);
                canvas.drawBitmap(this.mBackgroundOverlay, 0.0f, 0.0f, (Paint) null);
                if (this.mCircleDrawable != null) {
                    this.mCircleDrawable.draw(canvas);
                }
            }
        }

        @Override
        public boolean shouldDrawSuper() {
            return this.mBackground == null || this.mBackgroundOverlay == null;
        }

        public void startFadeoutAnimation(Animator.AnimatorListener listener, final ModeSelectorItem selectedItem, int x, int y, final int modeId) {
            this.mCoverPaint.setColor(0);
            this.mCoverPaint.setAlpha(0);
            this.mCircleDrawable.setIconDrawable(selectedItem.getIcon().getIconDrawableClone(), selectedItem.getIcon().getIconDrawableSize());
            this.mCircleDrawable.setCenter(new Point(x, y));
            this.mCircleDrawable.setColor(selectedItem.getHighlightColor());
            this.mCircleDrawable.setAnimatorListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    ModeListView.this.post(new Runnable() {
                        @Override
                        public void run() {
                            selectedItem.setSelected(true);
                            ModeListView.this.onModeSelected(modeId);
                        }
                    });
                }
            });
            AnimatorSet s = new AnimatorSet();
            s.play(this.mFadeOutAlphaAnimator);
            if (listener != null) {
                s.addListener(listener);
            }
            this.mCircleDrawable.animate();
            s.start();
        }

        @Override
        public void startAnimation(Animator.AnimatorListener listener) {
            if (this.mPeepHoleAnimator == null || !this.mPeepHoleAnimator.isRunning()) {
                if (this.mPeepHoleCenterY == -1 || this.mPeepHoleCenterX == -1) {
                    this.mPeepHoleCenterX = ModeListView.this.mWidth / 2;
                    this.mPeepHoleCenterY = ModeListView.this.mHeight / 2;
                }
                this.mCirclePaint.setAlpha(MotionEventCompat.ACTION_MASK);
                this.mCoverPaint.setAlpha(MotionEventCompat.ACTION_MASK);
                AnimatorSet s = new AnimatorSet();
                s.play(this.mPeepHoleAnimator).with(this.mRevealAlphaAnimator);
                if (listener != null) {
                    s.addListener(listener);
                }
                s.start();
            }
        }

        @Override
        public void endAnimation() {
        }

        @Override
        public boolean cancelAnimation() {
            if (this.mPeepHoleAnimator == null || !this.mPeepHoleAnimator.isRunning()) {
                return false;
            }
            this.mPeepHoleAnimator.cancel();
            return true;
        }
    }
}
