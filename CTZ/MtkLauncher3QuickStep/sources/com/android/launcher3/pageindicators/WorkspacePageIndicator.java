package com.android.launcher3.pageindicators;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.view.ViewCompat;
import android.util.AttributeSet;
import android.util.Property;
import android.view.View;
import android.view.ViewConfiguration;
import android.widget.FrameLayout;
import com.android.launcher3.DeviceProfile;
import com.android.launcher3.Insettable;
import com.android.launcher3.Launcher;
import com.android.launcher3.R;
import com.android.launcher3.Utilities;
import com.android.launcher3.uioverrides.WallpaperColorInfo;

public class WorkspacePageIndicator extends View implements Insettable, PageIndicator {
    private static final int ANIMATOR_COUNT = 3;
    public static final int BLACK_ALPHA = 165;
    private static final int LINE_ALPHA_ANIMATOR_INDEX = 0;
    private static final int NUM_PAGES_ANIMATOR_INDEX = 1;
    private static final int TOTAL_SCROLL_ANIMATOR_INDEX = 2;
    public static final int WHITE_ALPHA = 178;
    private int mActiveAlpha;
    private ValueAnimator[] mAnimators;
    private int mCurrentScroll;
    private final Handler mDelayedLineFadeHandler;
    private Runnable mHideLineRunnable;
    private final Launcher mLauncher;
    private final int mLineHeight;
    private Paint mLinePaint;
    private float mNumPagesFloat;
    private boolean mShouldAutoHide;
    private int mToAlpha;
    private int mTotalScroll;
    private static final int LINE_ANIMATE_DURATION = ViewConfiguration.getScrollBarFadeDuration();
    private static final int LINE_FADE_DELAY = ViewConfiguration.getScrollDefaultDelay();
    private static final Property<WorkspacePageIndicator, Integer> PAINT_ALPHA = new Property<WorkspacePageIndicator, Integer>(Integer.class, "paint_alpha") {
        @Override
        public Integer get(WorkspacePageIndicator workspacePageIndicator) {
            return Integer.valueOf(workspacePageIndicator.mLinePaint.getAlpha());
        }

        @Override
        public void set(WorkspacePageIndicator workspacePageIndicator, Integer num) {
            workspacePageIndicator.mLinePaint.setAlpha(num.intValue());
            workspacePageIndicator.invalidate();
        }
    };
    private static final Property<WorkspacePageIndicator, Float> NUM_PAGES = new Property<WorkspacePageIndicator, Float>(Float.class, "num_pages") {
        @Override
        public Float get(WorkspacePageIndicator workspacePageIndicator) {
            return Float.valueOf(workspacePageIndicator.mNumPagesFloat);
        }

        @Override
        public void set(WorkspacePageIndicator workspacePageIndicator, Float f) {
            workspacePageIndicator.mNumPagesFloat = f.floatValue();
            workspacePageIndicator.invalidate();
        }
    };
    private static final Property<WorkspacePageIndicator, Integer> TOTAL_SCROLL = new Property<WorkspacePageIndicator, Integer>(Integer.class, "total_scroll") {
        @Override
        public Integer get(WorkspacePageIndicator workspacePageIndicator) {
            return Integer.valueOf(workspacePageIndicator.mTotalScroll);
        }

        @Override
        public void set(WorkspacePageIndicator workspacePageIndicator, Integer num) {
            workspacePageIndicator.mTotalScroll = num.intValue();
            workspacePageIndicator.invalidate();
        }
    };

    public WorkspacePageIndicator(Context context) {
        this(context, null);
    }

    public WorkspacePageIndicator(Context context, AttributeSet attributeSet) {
        this(context, attributeSet, 0);
    }

    public WorkspacePageIndicator(Context context, AttributeSet attributeSet, int i) {
        super(context, attributeSet, i);
        this.mAnimators = new ValueAnimator[3];
        this.mDelayedLineFadeHandler = new Handler(Looper.getMainLooper());
        this.mShouldAutoHide = true;
        this.mActiveAlpha = 0;
        this.mHideLineRunnable = new Runnable() {
            @Override
            public final void run() {
                this.f$0.animateLineToAlpha(0);
            }
        };
        Resources resources = context.getResources();
        this.mLinePaint = new Paint();
        this.mLinePaint.setAlpha(0);
        this.mLauncher = Launcher.getLauncher(context);
        this.mLineHeight = resources.getDimensionPixelSize(R.dimen.dynamic_grid_page_indicator_line_height);
        boolean zSupportsDarkText = WallpaperColorInfo.getInstance(context).supportsDarkText();
        this.mActiveAlpha = zSupportsDarkText ? BLACK_ALPHA : WHITE_ALPHA;
        this.mLinePaint.setColor(zSupportsDarkText ? ViewCompat.MEASURED_STATE_MASK : -1);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (this.mTotalScroll == 0 || this.mNumPagesFloat == 0.0f) {
            return;
        }
        float fBoundToRange = Utilities.boundToRange(this.mCurrentScroll / this.mTotalScroll, 0.0f, 1.0f);
        int width = (int) (getWidth() / this.mNumPagesFloat);
        canvas.drawRoundRect((int) (fBoundToRange * (r1 - width)), (getHeight() / 2) - (this.mLineHeight / 2), width + r0, (getHeight() / 2) + (this.mLineHeight / 2), this.mLineHeight, this.mLineHeight, this.mLinePaint);
    }

    @Override
    public void setScroll(int i, int i2) {
        if (getAlpha() == 0.0f) {
            return;
        }
        animateLineToAlpha(this.mActiveAlpha);
        this.mCurrentScroll = i;
        if (this.mTotalScroll == 0) {
            this.mTotalScroll = i2;
        } else if (this.mTotalScroll != i2) {
            animateToTotalScroll(i2);
        } else {
            invalidate();
        }
        if (this.mShouldAutoHide) {
            hideAfterDelay();
        }
    }

    private void hideAfterDelay() {
        this.mDelayedLineFadeHandler.removeCallbacksAndMessages(null);
        this.mDelayedLineFadeHandler.postDelayed(this.mHideLineRunnable, LINE_FADE_DELAY);
    }

    @Override
    public void setActiveMarker(int i) {
    }

    @Override
    public void setMarkersCount(int i) {
        float f = i;
        if (Float.compare(f, this.mNumPagesFloat) == 0) {
            if (this.mAnimators[1] != null) {
                this.mAnimators[1].cancel();
                this.mAnimators[1] = null;
                return;
            }
            return;
        }
        setupAndRunAnimation(ObjectAnimator.ofFloat(this, NUM_PAGES, f), 1);
    }

    public void setShouldAutoHide(boolean z) {
        this.mShouldAutoHide = z;
        if (z && this.mLinePaint.getAlpha() > 0) {
            hideAfterDelay();
        } else if (!z) {
            this.mDelayedLineFadeHandler.removeCallbacksAndMessages(null);
        }
    }

    private void animateLineToAlpha(int i) {
        if (i == this.mToAlpha) {
            return;
        }
        this.mToAlpha = i;
        setupAndRunAnimation(ObjectAnimator.ofInt(this, PAINT_ALPHA, i), 0);
    }

    private void animateToTotalScroll(int i) {
        setupAndRunAnimation(ObjectAnimator.ofInt(this, TOTAL_SCROLL, i), 2);
    }

    private void setupAndRunAnimation(ValueAnimator valueAnimator, final int i) {
        if (this.mAnimators[i] != null) {
            this.mAnimators[i].cancel();
        }
        this.mAnimators[i] = valueAnimator;
        this.mAnimators[i].addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animator) {
                WorkspacePageIndicator.this.mAnimators[i] = null;
            }
        });
        this.mAnimators[i].setDuration(LINE_ANIMATE_DURATION);
        this.mAnimators[i].start();
    }

    public void pauseAnimations() {
        for (int i = 0; i < 3; i++) {
            if (this.mAnimators[i] != null) {
                this.mAnimators[i].pause();
            }
        }
    }

    public void skipAnimationsToEnd() {
        for (int i = 0; i < 3; i++) {
            if (this.mAnimators[i] != null) {
                this.mAnimators[i].end();
            }
        }
    }

    @Override
    public void setInsets(Rect rect) {
        DeviceProfile deviceProfile = this.mLauncher.getDeviceProfile();
        FrameLayout.LayoutParams layoutParams = (FrameLayout.LayoutParams) getLayoutParams();
        if (deviceProfile.isVerticalBarLayout()) {
            Rect rect2 = deviceProfile.workspacePadding;
            layoutParams.leftMargin = rect2.left + deviceProfile.workspaceCellPaddingXPx;
            layoutParams.rightMargin = rect2.right + deviceProfile.workspaceCellPaddingXPx;
            layoutParams.bottomMargin = rect2.bottom;
        } else {
            layoutParams.rightMargin = 0;
            layoutParams.leftMargin = 0;
            layoutParams.gravity = 81;
            layoutParams.bottomMargin = deviceProfile.hotseatBarSizePx + rect.bottom;
        }
        setLayoutParams(layoutParams);
    }
}
