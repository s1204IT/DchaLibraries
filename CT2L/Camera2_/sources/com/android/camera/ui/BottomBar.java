package com.android.camera.ui;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.graphics.drawable.TransitionDrawable;
import android.support.v4.view.MotionEventCompat;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.TouchDelegate;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import com.android.camera.CaptureLayoutHelper;
import com.android.camera.ShutterButton;
import com.android.camera.debug.Log;
import com.android.camera.util.ApiHelper;
import com.android.camera.util.CameraUtil;
import com.android.camera2.R;

public class BottomBar extends FrameLayout {
    private static final int CIRCLE_ANIM_DURATION_MS = 300;
    private static final int DRAWABLE_MAX_LEVEL = 10000;
    private static final int MODE_CANCEL = 3;
    private static final int MODE_CAPTURE = 0;
    private static final int MODE_INTENT = 1;
    private static final int MODE_INTENT_REVIEW = 2;
    private static final Log.Tag TAG = new Log.Tag("BottomBar");
    private AnimatedCircleDrawable mAnimatedCircleDrawable;
    private int mBackgroundAlpha;
    private final int mBackgroundAlphaDefault;
    private final int mBackgroundAlphaOverlay;
    private int mBackgroundColor;
    private int mBackgroundPressedColor;
    private ImageButton mCancelButton;
    private FrameLayout mCancelLayout;
    private FrameLayout mCaptureLayout;
    private CaptureLayoutHelper mCaptureLayoutHelper;
    private final float mCircleRadius;
    private ColorDrawable mColorDrawable;
    private boolean mDrawCircle;
    private TopRightWeightedLayout mIntentReviewLayout;
    private int mMode;
    private boolean mOverLayBottomBar;
    private RectF mRect;
    private ShutterButton mShutterButton;
    private final Drawable.ConstantState[] mShutterButtonBackgroundConstantStates;

    public BottomBar(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mBackgroundAlpha = MotionEventCompat.ACTION_MASK;
        this.mCaptureLayoutHelper = null;
        this.mRect = new RectF();
        this.mCircleRadius = getResources().getDimensionPixelSize(R.dimen.video_capture_circle_diameter) / 2;
        this.mBackgroundAlphaOverlay = getResources().getInteger(R.integer.bottom_bar_background_alpha_overlay);
        this.mBackgroundAlphaDefault = getResources().getInteger(R.integer.bottom_bar_background_alpha);
        TypedArray ar = context.getResources().obtainTypedArray(R.array.shutter_button_backgrounds);
        int len = ar.length();
        this.mShutterButtonBackgroundConstantStates = new Drawable.ConstantState[len];
        for (int i = 0; i < len; i++) {
            int drawableId = ar.getResourceId(i, -1);
            this.mShutterButtonBackgroundConstantStates[i] = context.getResources().getDrawable(drawableId).getConstantState();
        }
        ar.recycle();
    }

    private void setPaintColor(int alpha, int color) {
        if (this.mAnimatedCircleDrawable != null) {
            this.mAnimatedCircleDrawable.setColor(color);
            this.mAnimatedCircleDrawable.setAlpha(alpha);
        } else if (this.mColorDrawable != null) {
            this.mColorDrawable.setColor(color);
            this.mColorDrawable.setAlpha(alpha);
        }
        if (this.mIntentReviewLayout != null) {
            ColorDrawable intentBackground = (ColorDrawable) this.mIntentReviewLayout.getBackground();
            intentBackground.setColor(color);
            intentBackground.setAlpha(alpha);
        }
    }

    private void refreshPaintColor() {
        setPaintColor(this.mBackgroundAlpha, this.mBackgroundColor);
    }

    private void setCancelBackgroundColor(int alpha, int color) {
        LayerDrawable layerDrawable = (LayerDrawable) this.mCancelButton.getBackground();
        Drawable d = layerDrawable.getDrawable(0);
        if (d instanceof AnimatedCircleDrawable) {
            AnimatedCircleDrawable animatedCircleDrawable = (AnimatedCircleDrawable) d;
            animatedCircleDrawable.setColor(color);
            animatedCircleDrawable.setAlpha(alpha);
        } else if (d instanceof ColorDrawable) {
            ColorDrawable colorDrawable = (ColorDrawable) d;
            if (!ApiHelper.isLOrHigher()) {
                colorDrawable.setColor(color);
            }
            colorDrawable.setAlpha(alpha);
        }
    }

    private void setCaptureButtonUp() {
        setPaintColor(this.mBackgroundAlpha, this.mBackgroundColor);
    }

    private void setCaptureButtonDown() {
        if (!ApiHelper.isLOrHigher()) {
            setPaintColor(this.mBackgroundAlpha, this.mBackgroundPressedColor);
        }
    }

    private void setCancelButtonUp() {
        setCancelBackgroundColor(this.mBackgroundAlpha, this.mBackgroundColor);
    }

    private void setCancelButtonDown() {
        setCancelBackgroundColor(this.mBackgroundAlpha, this.mBackgroundPressedColor);
    }

    @Override
    public void onFinishInflate() {
        this.mCaptureLayout = (FrameLayout) findViewById(R.id.bottombar_capture);
        this.mCancelLayout = (FrameLayout) findViewById(R.id.bottombar_cancel);
        this.mCancelLayout.setVisibility(8);
        this.mIntentReviewLayout = (TopRightWeightedLayout) findViewById(R.id.bottombar_intent_review);
        this.mShutterButton = (ShutterButton) findViewById(R.id.shutter_button);
        this.mShutterButton.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getActionMasked() == 0) {
                    BottomBar.this.setCaptureButtonDown();
                    return false;
                }
                if (1 == event.getActionMasked() || 3 == event.getActionMasked()) {
                    BottomBar.this.setCaptureButtonUp();
                    return false;
                }
                if (2 == event.getActionMasked()) {
                    BottomBar.this.mRect.set(0.0f, 0.0f, BottomBar.this.getWidth(), BottomBar.this.getHeight());
                    if (!BottomBar.this.mRect.contains(event.getX(), event.getY())) {
                        BottomBar.this.setCaptureButtonUp();
                        return false;
                    }
                    return false;
                }
                return false;
            }
        });
        this.mCancelButton = (ImageButton) findViewById(R.id.shutter_cancel_button);
        this.mCancelButton.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getActionMasked() == 0) {
                    BottomBar.this.setCancelButtonDown();
                    return false;
                }
                if (1 == event.getActionMasked() || 3 == event.getActionMasked()) {
                    BottomBar.this.setCancelButtonUp();
                    return false;
                }
                if (2 == event.getActionMasked()) {
                    BottomBar.this.mRect.set(0.0f, 0.0f, BottomBar.this.getWidth(), BottomBar.this.getHeight());
                    if (!BottomBar.this.mRect.contains(event.getX(), event.getY())) {
                        BottomBar.this.setCancelButtonUp();
                        return false;
                    }
                    return false;
                }
                return false;
            }
        });
        extendTouchAreaToMatchParent(R.id.done_button);
    }

    private void extendTouchAreaToMatchParent(int id) {
        final View button = findViewById(id);
        final View parent = (View) button.getParent();
        parent.post(new Runnable() {
            @Override
            public void run() {
                Rect parentRect = new Rect();
                parent.getHitRect(parentRect);
                Rect buttonRect = new Rect();
                button.getHitRect(buttonRect);
                int widthDiff = parentRect.width() - buttonRect.width();
                int heightDiff = parentRect.height() - buttonRect.height();
                buttonRect.left -= widthDiff / 2;
                buttonRect.right += widthDiff / 2;
                buttonRect.top -= heightDiff / 2;
                buttonRect.bottom += heightDiff / 2;
                parent.setTouchDelegate(new TouchDelegate(buttonRect, button));
            }
        });
    }

    public void transitionToCapture() {
        this.mCaptureLayout.setVisibility(0);
        this.mCancelLayout.setVisibility(8);
        this.mIntentReviewLayout.setVisibility(8);
        this.mMode = 0;
    }

    public void transitionToCancel() {
        this.mCaptureLayout.setVisibility(8);
        this.mIntentReviewLayout.setVisibility(8);
        this.mCancelLayout.setVisibility(0);
        this.mMode = 3;
    }

    public void transitionToIntentCaptureLayout() {
        this.mIntentReviewLayout.setVisibility(8);
        this.mCaptureLayout.setVisibility(0);
        this.mCancelLayout.setVisibility(8);
        this.mMode = 1;
    }

    public void transitionToIntentReviewLayout() {
        this.mCaptureLayout.setVisibility(8);
        this.mIntentReviewLayout.setVisibility(0);
        this.mCancelLayout.setVisibility(8);
        this.mMode = 2;
    }

    public boolean isInIntentReview() {
        return this.mMode == 2;
    }

    private void setButtonImageLevels(int level) {
        ((ImageButton) findViewById(R.id.cancel_button)).setImageLevel(level);
        ((ImageButton) findViewById(R.id.done_button)).setImageLevel(level);
        ((ImageButton) findViewById(R.id.retake_button)).setImageLevel(level);
    }

    private void setOverlayBottomBar(boolean overlay) {
        this.mOverLayBottomBar = overlay;
        if (overlay) {
            setBackgroundAlpha(this.mBackgroundAlphaOverlay);
            setButtonImageLevels(1);
        } else {
            setBackgroundAlpha(this.mBackgroundAlphaDefault);
            setButtonImageLevels(0);
        }
    }

    public void setCaptureLayoutHelper(CaptureLayoutHelper helper) {
        this.mCaptureLayoutHelper = helper;
    }

    @Override
    public void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int measureWidth = View.MeasureSpec.getSize(widthMeasureSpec);
        int measureHeight = View.MeasureSpec.getSize(heightMeasureSpec);
        if (measureWidth != 0 && measureHeight != 0) {
            if (this.mCaptureLayoutHelper == null) {
                super.onMeasure(widthMeasureSpec, heightMeasureSpec);
                Log.e(TAG, "Capture layout helper needs to be set first.");
            } else {
                RectF bottomBarRect = this.mCaptureLayoutHelper.getBottomBarRect();
                super.onMeasure(View.MeasureSpec.makeMeasureSpec((int) bottomBarRect.width(), 1073741824), View.MeasureSpec.makeMeasureSpec((int) bottomBarRect.height(), 1073741824));
                boolean shouldOverlayBottomBar = this.mCaptureLayoutHelper.shouldOverlayBottomBar();
                setOverlayBottomBar(shouldOverlayBottomBar);
            }
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return true;
    }

    @Override
    public void setBackgroundColor(int color) {
        this.mBackgroundColor = color;
        setPaintColor(this.mBackgroundAlpha, this.mBackgroundColor);
        setCancelBackgroundColor(this.mBackgroundAlpha, this.mBackgroundColor);
    }

    private void setBackgroundPressedColor(int color) {
        if (!ApiHelper.isLOrHigher()) {
            this.mBackgroundPressedColor = color;
        }
    }

    private LayerDrawable applyCircleDrawableToShutterBackground(LayerDrawable shutterBackground) {
        Drawable d = shutterBackground.findDrawableByLayerId(R.id.circle_item);
        if (d != null) {
            Drawable animatedCircleDrawable = new AnimatedCircleDrawable((int) this.mCircleRadius);
            animatedCircleDrawable.setLevel(10000);
            shutterBackground.setDrawableByLayerId(R.id.circle_item, animatedCircleDrawable);
        }
        return shutterBackground;
    }

    private LayerDrawable newDrawableFromConstantState(Drawable.ConstantState constantState) {
        return (LayerDrawable) constantState.newDrawable(getContext().getResources());
    }

    private void setupShutterBackgroundForModeIndex(int index) {
        LayerDrawable shutterBackground = applyCircleDrawableToShutterBackground(newDrawableFromConstantState(this.mShutterButtonBackgroundConstantStates[index]));
        this.mShutterButton.setBackground(shutterBackground);
        this.mCancelButton.setBackground(applyCircleDrawableToShutterBackground(newDrawableFromConstantState(this.mShutterButtonBackgroundConstantStates[index])));
        Drawable d = shutterBackground.getDrawable(0);
        this.mAnimatedCircleDrawable = null;
        this.mColorDrawable = null;
        if (d instanceof AnimatedCircleDrawable) {
            this.mAnimatedCircleDrawable = (AnimatedCircleDrawable) d;
        } else if (d instanceof ColorDrawable) {
            this.mColorDrawable = (ColorDrawable) d;
        }
        int colorId = CameraUtil.getCameraThemeColorId(index, getContext());
        int pressedColor = getContext().getResources().getColor(colorId);
        setBackgroundPressedColor(pressedColor);
        refreshPaintColor();
    }

    public void setColorsForModeIndex(int index) {
        setupShutterBackgroundForModeIndex(index);
    }

    public void setBackgroundAlpha(int alpha) {
        this.mBackgroundAlpha = alpha;
        setPaintColor(this.mBackgroundAlpha, this.mBackgroundColor);
        setCancelBackgroundColor(this.mBackgroundAlpha, this.mBackgroundColor);
    }

    public void setShutterButtonEnabled(final boolean enabled) {
        this.mShutterButton.post(new Runnable() {
            @Override
            public void run() {
                BottomBar.this.mShutterButton.setEnabled(enabled);
                BottomBar.this.setShutterButtonImportantToA11y(enabled);
            }
        });
    }

    public void setShutterButtonImportantToA11y(boolean important) {
        if (important) {
            this.mShutterButton.setImportantForAccessibility(0);
        } else {
            this.mShutterButton.setImportantForAccessibility(2);
        }
    }

    public boolean isShutterButtonEnabled() {
        return this.mShutterButton.isEnabled();
    }

    private TransitionDrawable crossfadeDrawable(Drawable from, Drawable to) {
        Drawable[] arrayDrawable = {from, to};
        TransitionDrawable transitionDrawable = new TransitionDrawable(arrayDrawable);
        transitionDrawable.setCrossFadeEnabled(true);
        return transitionDrawable;
    }

    public void setShutterButtonIcon(int resId) {
        Drawable iconDrawable = getResources().getDrawable(resId);
        if (iconDrawable != null) {
            iconDrawable = iconDrawable.mutate();
        }
        this.mShutterButton.setImageDrawable(iconDrawable);
    }

    public void animateToVideoStop(int resId) {
        if (this.mOverLayBottomBar && this.mAnimatedCircleDrawable != null) {
            this.mAnimatedCircleDrawable.animateToSmallRadius();
            this.mDrawCircle = true;
        }
        TransitionDrawable transitionDrawable = crossfadeDrawable(this.mShutterButton.getDrawable(), getResources().getDrawable(resId));
        this.mShutterButton.setImageDrawable(transitionDrawable);
        transitionDrawable.startTransition(300);
    }

    public void animateToFullSize(int resId) {
        if (this.mDrawCircle && this.mAnimatedCircleDrawable != null) {
            this.mAnimatedCircleDrawable.animateToFullSize();
            this.mDrawCircle = false;
        }
        TransitionDrawable transitionDrawable = crossfadeDrawable(this.mShutterButton.getDrawable(), getResources().getDrawable(resId));
        this.mShutterButton.setImageDrawable(transitionDrawable);
        transitionDrawable.startTransition(300);
    }
}
