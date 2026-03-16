package com.android.camera.widget;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import com.android.camera.MultiToggleImageButton;
import com.android.camera.ui.RadioOptions;
import com.android.camera.ui.TopRightWeightedLayout;
import com.android.camera.util.Gusterpolator;
import com.android.camera2.R;
import java.util.ArrayList;

public class ModeOptions extends FrameLayout {
    public static final int BAR_INVALID = -1;
    public static final int BAR_PANO = 1;
    public static final int BAR_STANDARD = 0;
    private static final int HIDE_ALPHA_ANIMATION_TIME = 200;
    private static final int PADDING_ANIMATION_TIME = 350;
    private static final int RADIUS_ANIMATION_TIME = 250;
    private static final int SHOW_ALPHA_ANIMATION_TIME = 350;
    private ViewGroup mActiveBar;
    private RectF mAnimateFrom;
    private int mBackgroundColor;
    private boolean mDrawCircle;
    private boolean mFill;
    private AnimatorSet mHiddenAnimator;
    private boolean mIsHiddenOrHiding;
    private boolean mIsPortrait;
    private ViewGroup mMainBar;
    private TopRightWeightedLayout mModeOptionsButtons;
    private RadioOptions mModeOptionsExposure;
    private RadioOptions mModeOptionsPano;
    private final Paint mPaint;
    private float mRadius;
    private View mViewToShowHide;
    private AnimatorSet mVisibleAnimator;

    public ModeOptions(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mPaint = new Paint();
        this.mAnimateFrom = new RectF();
        this.mRadius = 0.0f;
    }

    public void setViewToShowHide(View v) {
        this.mViewToShowHide = v;
    }

    @Override
    public void onFinishInflate() {
        this.mIsHiddenOrHiding = true;
        this.mBackgroundColor = getResources().getColor(R.color.mode_options_background);
        this.mPaint.setAntiAlias(true);
        this.mPaint.setColor(this.mBackgroundColor);
        this.mModeOptionsButtons = (TopRightWeightedLayout) findViewById(R.id.mode_options_buttons);
        this.mModeOptionsPano = (RadioOptions) findViewById(R.id.mode_options_pano);
        this.mModeOptionsExposure = (RadioOptions) findViewById(R.id.mode_options_exposure);
        TopRightWeightedLayout topRightWeightedLayout = this.mModeOptionsButtons;
        this.mActiveBar = topRightWeightedLayout;
        this.mMainBar = topRightWeightedLayout;
        ImageButton exposureButton = (ImageButton) findViewById(R.id.exposure_button);
        exposureButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ModeOptions.this.mActiveBar = ModeOptions.this.mModeOptionsExposure;
                ModeOptions.this.mMainBar.setVisibility(4);
                ModeOptions.this.mActiveBar.setVisibility(0);
            }
        });
    }

    public void setMainBar(int b) {
        for (int i = 0; i < getChildCount(); i++) {
            getChildAt(i).setVisibility(4);
        }
        switch (b) {
            case 0:
                TopRightWeightedLayout topRightWeightedLayout = this.mModeOptionsButtons;
                this.mActiveBar = topRightWeightedLayout;
                this.mMainBar = topRightWeightedLayout;
                break;
            case 1:
                RadioOptions radioOptions = this.mModeOptionsPano;
                this.mActiveBar = radioOptions;
                this.mMainBar = radioOptions;
                break;
        }
        this.mMainBar.setVisibility(0);
    }

    public int getMainBar() {
        if (this.mMainBar == this.mModeOptionsButtons) {
            return 0;
        }
        if (this.mMainBar == this.mModeOptionsPano) {
            return 1;
        }
        return -1;
    }

    @Override
    public void onWindowVisibilityChanged(int visibility) {
        super.onWindowVisibilityChanged(visibility);
        if (visibility != 0 && !this.mIsHiddenOrHiding) {
            setVisibility(4);
            if (this.mMainBar != null) {
                this.mMainBar.setVisibility(0);
            }
            if (this.mActiveBar != null && this.mActiveBar != this.mMainBar) {
                this.mActiveBar.setVisibility(4);
            }
            if (this.mViewToShowHide != null) {
                this.mViewToShowHide.setVisibility(0);
            }
            this.mIsHiddenOrHiding = true;
        }
    }

    @Override
    public void onLayout(boolean changed, int left, int top, int right, int bottom) {
        float rLeft;
        float rTop;
        if (changed) {
            this.mIsPortrait = getResources().getConfiguration().orientation == 1;
            int buttonSize = getResources().getDimensionPixelSize(R.dimen.option_button_circle_size);
            int buttonPadding = getResources().getDimensionPixelSize(R.dimen.mode_options_toggle_padding);
            if (this.mIsPortrait) {
                rLeft = (getWidth() - buttonPadding) - buttonSize;
                rTop = (getHeight() - buttonSize) / 2.0f;
            } else {
                rLeft = buttonPadding;
                rTop = buttonPadding;
            }
            float rRight = rLeft + buttonSize;
            float rBottom = rTop + buttonSize;
            this.mAnimateFrom.set(rLeft, rTop, rRight, rBottom);
            setupAnimators();
            setupToggleButtonParams();
        }
        super.onLayout(changed, left, top, right, bottom);
    }

    @Override
    public void onDraw(Canvas canvas) {
        if (this.mDrawCircle) {
            canvas.drawCircle(this.mAnimateFrom.centerX(), this.mAnimateFrom.centerY(), this.mRadius, this.mPaint);
        } else if (this.mFill) {
            canvas.drawPaint(this.mPaint);
        }
        super.onDraw(canvas);
    }

    private void setupToggleButtonParams() {
        int size = this.mIsPortrait ? getHeight() : getWidth();
        for (int i = 0; i < this.mModeOptionsButtons.getChildCount(); i++) {
            View button = this.mModeOptionsButtons.getChildAt(i);
            if (button instanceof MultiToggleImageButton) {
                MultiToggleImageButton toggleButton = (MultiToggleImageButton) button;
                toggleButton.setParentSize(size);
                toggleButton.setAnimDirection(this.mIsPortrait ? 0 : 1);
            }
        }
    }

    private void setupAnimators() {
        final View button;
        if (this.mVisibleAnimator != null) {
            this.mVisibleAnimator.end();
        }
        if (this.mHiddenAnimator != null) {
            this.mHiddenAnimator.end();
        }
        float fullSize = this.mIsPortrait ? getWidth() : getHeight();
        ValueAnimator radiusAnimator = ValueAnimator.ofFloat(this.mAnimateFrom.width() / 2.0f, fullSize - (this.mAnimateFrom.width() / 2.0f));
        radiusAnimator.setDuration(250L);
        radiusAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                ModeOptions.this.mRadius = ((Float) animation.getAnimatedValue()).floatValue();
                ModeOptions.this.mDrawCircle = true;
                ModeOptions.this.mFill = false;
            }
        });
        radiusAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                ModeOptions.this.mDrawCircle = false;
                ModeOptions.this.mFill = true;
            }
        });
        ValueAnimator alphaAnimator = ValueAnimator.ofFloat(0.0f, 1.0f);
        alphaAnimator.setDuration(350L);
        alphaAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                ModeOptions.this.mActiveBar.setAlpha(((Float) animation.getAnimatedValue()).floatValue());
            }
        });
        alphaAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                ModeOptions.this.mActiveBar.setAlpha(1.0f);
            }
        });
        int deltaX = getResources().getDimensionPixelSize(R.dimen.mode_options_buttons_anim_delta_x);
        int childCount = this.mActiveBar.getChildCount();
        ArrayList<Animator> paddingAnimators = new ArrayList<>();
        for (int i = 0; i < childCount; i++) {
            if (this.mIsPortrait) {
                button = this.mActiveBar.getChildAt(i);
            } else {
                button = this.mActiveBar.getChildAt((childCount - 1) - i);
            }
            ValueAnimator paddingAnimator = ValueAnimator.ofFloat((childCount - i) * deltaX, 0.0f);
            paddingAnimator.setDuration(350L);
            paddingAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    if (ModeOptions.this.mIsPortrait) {
                        button.setTranslationX(((Float) animation.getAnimatedValue()).floatValue());
                    } else {
                        button.setTranslationY(-((Float) animation.getAnimatedValue()).floatValue());
                    }
                    ModeOptions.this.invalidate();
                }
            });
            paddingAnimators.add(paddingAnimator);
        }
        AnimatorSet paddingAnimatorSet = new AnimatorSet();
        paddingAnimatorSet.playTogether(paddingAnimators);
        this.mVisibleAnimator = new AnimatorSet();
        this.mVisibleAnimator.setInterpolator(Gusterpolator.INSTANCE);
        this.mVisibleAnimator.playTogether(radiusAnimator, alphaAnimator, paddingAnimatorSet);
        ValueAnimator radiusAnimator2 = ValueAnimator.ofFloat(fullSize - (this.mAnimateFrom.width() / 2.0f), this.mAnimateFrom.width() / 2.0f);
        radiusAnimator2.setDuration(250L);
        radiusAnimator2.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                ModeOptions.this.mRadius = ((Float) animation.getAnimatedValue()).floatValue();
                ModeOptions.this.mDrawCircle = true;
                ModeOptions.this.mFill = false;
                ModeOptions.this.invalidate();
            }
        });
        radiusAnimator2.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                if (ModeOptions.this.mViewToShowHide != null) {
                    ModeOptions.this.mViewToShowHide.setVisibility(0);
                    ModeOptions.this.mDrawCircle = false;
                    ModeOptions.this.mFill = false;
                    ModeOptions.this.invalidate();
                }
            }
        });
        ValueAnimator alphaAnimator2 = ValueAnimator.ofFloat(1.0f, 0.0f);
        alphaAnimator2.setDuration(200L);
        alphaAnimator2.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                ModeOptions.this.mActiveBar.setAlpha(((Float) animation.getAnimatedValue()).floatValue());
                ModeOptions.this.invalidate();
            }
        });
        alphaAnimator2.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                ModeOptions.this.setVisibility(4);
                if (ModeOptions.this.mActiveBar != ModeOptions.this.mMainBar) {
                    ModeOptions.this.mActiveBar.setAlpha(1.0f);
                    ModeOptions.this.mActiveBar.setVisibility(4);
                }
                ModeOptions.this.mMainBar.setAlpha(1.0f);
                ModeOptions.this.mMainBar.setVisibility(0);
                ModeOptions.this.mActiveBar = ModeOptions.this.mMainBar;
                ModeOptions.this.invalidate();
            }
        });
        this.mHiddenAnimator = new AnimatorSet();
        this.mHiddenAnimator.setInterpolator(Gusterpolator.INSTANCE);
        this.mHiddenAnimator.playTogether(radiusAnimator2, alphaAnimator2);
    }

    public void animateVisible() {
        if (this.mIsHiddenOrHiding) {
            if (this.mViewToShowHide != null) {
                this.mViewToShowHide.setVisibility(4);
            }
            this.mHiddenAnimator.cancel();
            this.mVisibleAnimator.end();
            setVisibility(0);
            this.mVisibleAnimator.start();
        }
        this.mIsHiddenOrHiding = false;
    }

    public void animateHidden() {
        if (!this.mIsHiddenOrHiding) {
            this.mVisibleAnimator.cancel();
            this.mHiddenAnimator.end();
            this.mHiddenAnimator.start();
        }
        this.mIsHiddenOrHiding = true;
    }
}
