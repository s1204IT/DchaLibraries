package android.support.design.widget;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.TimeInterpolator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Matrix;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.os.Build;
import android.support.design.animation.AnimationUtils;
import android.support.design.animation.AnimatorSetCompat;
import android.support.design.animation.ImageMatrixProperty;
import android.support.design.animation.MatrixEvaluator;
import android.support.design.animation.MotionSpec;
import android.support.design.ripple.RippleUtils;
import android.support.v4.content.ContextCompat;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.support.v4.view.ViewCompat;
import android.util.Property;
import android.view.View;
import android.view.ViewTreeObserver;
import java.util.ArrayList;
import java.util.List;

class FloatingActionButtonImpl {
    CircularBorderDrawable borderDrawable;
    Drawable contentBackground;
    Animator currentAnimator;
    private MotionSpec defaultHideMotionSpec;
    private MotionSpec defaultShowMotionSpec;
    float elevation;
    MotionSpec hideMotionSpec;
    float hoveredFocusedTranslationZ;
    int maxImageSize;
    private ViewTreeObserver.OnPreDrawListener preDrawListener;
    float pressedTranslationZ;
    Drawable rippleDrawable;
    private float rotation;
    ShadowDrawableWrapper shadowDrawable;
    final ShadowViewDelegate shadowViewDelegate;
    Drawable shapeDrawable;
    MotionSpec showMotionSpec;
    final VisibilityAwareImageButton view;
    static final TimeInterpolator ELEVATION_ANIM_INTERPOLATOR = AnimationUtils.FAST_OUT_LINEAR_IN_INTERPOLATOR;
    static final int[] PRESSED_ENABLED_STATE_SET = {android.R.attr.state_pressed, android.R.attr.state_enabled};
    static final int[] HOVERED_FOCUSED_ENABLED_STATE_SET = {android.R.attr.state_hovered, android.R.attr.state_focused, android.R.attr.state_enabled};
    static final int[] FOCUSED_ENABLED_STATE_SET = {android.R.attr.state_focused, android.R.attr.state_enabled};
    static final int[] HOVERED_ENABLED_STATE_SET = {android.R.attr.state_hovered, android.R.attr.state_enabled};
    static final int[] ENABLED_STATE_SET = {android.R.attr.state_enabled};
    static final int[] EMPTY_STATE_SET = new int[0];
    int animState = 0;
    float imageMatrixScale = 1.0f;
    private final Rect tmpRect = new Rect();
    private final RectF tmpRectF1 = new RectF();
    private final RectF tmpRectF2 = new RectF();
    private final Matrix tmpMatrix = new Matrix();
    private final StateListAnimator stateListAnimator = new StateListAnimator();

    interface InternalVisibilityChangedListener {
        void onHidden();

        void onShown();
    }

    FloatingActionButtonImpl(VisibilityAwareImageButton view, ShadowViewDelegate shadowViewDelegate) {
        this.view = view;
        this.shadowViewDelegate = shadowViewDelegate;
        this.stateListAnimator.addState(PRESSED_ENABLED_STATE_SET, createElevationAnimator(new ElevateToPressedTranslationZAnimation()));
        this.stateListAnimator.addState(HOVERED_FOCUSED_ENABLED_STATE_SET, createElevationAnimator(new ElevateToHoveredFocusedTranslationZAnimation()));
        this.stateListAnimator.addState(FOCUSED_ENABLED_STATE_SET, createElevationAnimator(new ElevateToHoveredFocusedTranslationZAnimation()));
        this.stateListAnimator.addState(HOVERED_ENABLED_STATE_SET, createElevationAnimator(new ElevateToHoveredFocusedTranslationZAnimation()));
        this.stateListAnimator.addState(ENABLED_STATE_SET, createElevationAnimator(new ResetElevationAnimation()));
        this.stateListAnimator.addState(EMPTY_STATE_SET, createElevationAnimator(new DisabledElevationAnimation()));
        this.rotation = this.view.getRotation();
    }

    void setBackgroundDrawable(ColorStateList backgroundTint, PorterDuff.Mode backgroundTintMode, ColorStateList rippleColor, int borderWidth) {
        Drawable[] layers;
        this.shapeDrawable = DrawableCompat.wrap(createShapeDrawable());
        DrawableCompat.setTintList(this.shapeDrawable, backgroundTint);
        if (backgroundTintMode != null) {
            DrawableCompat.setTintMode(this.shapeDrawable, backgroundTintMode);
        }
        GradientDrawable touchFeedbackShape = createShapeDrawable();
        this.rippleDrawable = DrawableCompat.wrap(touchFeedbackShape);
        DrawableCompat.setTintList(this.rippleDrawable, RippleUtils.convertToRippleDrawableColor(rippleColor));
        if (borderWidth > 0) {
            this.borderDrawable = createBorderDrawable(borderWidth, backgroundTint);
            layers = new Drawable[]{this.borderDrawable, this.shapeDrawable, this.rippleDrawable};
        } else {
            this.borderDrawable = null;
            layers = new Drawable[]{this.shapeDrawable, this.rippleDrawable};
        }
        this.contentBackground = new LayerDrawable(layers);
        this.shadowDrawable = new ShadowDrawableWrapper(this.view.getContext(), this.contentBackground, this.shadowViewDelegate.getRadius(), this.elevation, this.pressedTranslationZ + this.elevation);
        this.shadowDrawable.setAddPaddingForCorners(false);
        this.shadowViewDelegate.setBackgroundDrawable(this.shadowDrawable);
    }

    void setBackgroundTintList(ColorStateList tint) {
        if (this.shapeDrawable != null) {
            DrawableCompat.setTintList(this.shapeDrawable, tint);
        }
        if (this.borderDrawable != null) {
            this.borderDrawable.setBorderTint(tint);
        }
    }

    void setBackgroundTintMode(PorterDuff.Mode tintMode) {
        if (this.shapeDrawable != null) {
            DrawableCompat.setTintMode(this.shapeDrawable, tintMode);
        }
    }

    final void setElevation(float elevation) {
        if (this.elevation != elevation) {
            this.elevation = elevation;
            onElevationsChanged(this.elevation, this.hoveredFocusedTranslationZ, this.pressedTranslationZ);
        }
    }

    float getElevation() {
        return this.elevation;
    }

    final void setHoveredFocusedTranslationZ(float translationZ) {
        if (this.hoveredFocusedTranslationZ != translationZ) {
            this.hoveredFocusedTranslationZ = translationZ;
            onElevationsChanged(this.elevation, this.hoveredFocusedTranslationZ, this.pressedTranslationZ);
        }
    }

    final void setPressedTranslationZ(float translationZ) {
        if (this.pressedTranslationZ != translationZ) {
            this.pressedTranslationZ = translationZ;
            onElevationsChanged(this.elevation, this.hoveredFocusedTranslationZ, this.pressedTranslationZ);
        }
    }

    final void setMaxImageSize(int maxImageSize) {
        if (this.maxImageSize != maxImageSize) {
            this.maxImageSize = maxImageSize;
            updateImageMatrixScale();
        }
    }

    final void updateImageMatrixScale() {
        setImageMatrixScale(this.imageMatrixScale);
    }

    final void setImageMatrixScale(float scale) {
        this.imageMatrixScale = scale;
        Matrix matrix = this.tmpMatrix;
        calculateImageMatrixFromScale(scale, matrix);
        this.view.setImageMatrix(matrix);
    }

    private void calculateImageMatrixFromScale(float scale, Matrix matrix) {
        matrix.reset();
        Drawable drawable = this.view.getDrawable();
        if (drawable != null && this.maxImageSize != 0) {
            RectF drawableBounds = this.tmpRectF1;
            RectF imageBounds = this.tmpRectF2;
            drawableBounds.set(0.0f, 0.0f, drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight());
            imageBounds.set(0.0f, 0.0f, this.maxImageSize, this.maxImageSize);
            matrix.setRectToRect(drawableBounds, imageBounds, Matrix.ScaleToFit.CENTER);
            matrix.postScale(scale, scale, this.maxImageSize / 2.0f, this.maxImageSize / 2.0f);
        }
    }

    final void setShowMotionSpec(MotionSpec spec) {
        this.showMotionSpec = spec;
    }

    final void setHideMotionSpec(MotionSpec spec) {
        this.hideMotionSpec = spec;
    }

    void onElevationsChanged(float elevation, float hoveredFocusedTranslationZ, float pressedTranslationZ) {
        if (this.shadowDrawable != null) {
            this.shadowDrawable.setShadowSize(elevation, this.pressedTranslationZ + elevation);
            updatePadding();
        }
    }

    void onDrawableStateChanged(int[] state) {
        this.stateListAnimator.setState(state);
    }

    void jumpDrawableToCurrentState() {
        this.stateListAnimator.jumpToCurrentState();
    }

    void hide(final InternalVisibilityChangedListener listener, final boolean fromUser) {
        if (isOrWillBeHidden()) {
            return;
        }
        if (this.currentAnimator != null) {
            this.currentAnimator.cancel();
        }
        if (shouldAnimateVisibilityChange()) {
            AnimatorSet set = createAnimator(this.hideMotionSpec != null ? this.hideMotionSpec : getDefaultHideMotionSpec(), 0.0f, 0.0f, 0.0f);
            set.addListener(new AnimatorListenerAdapter() {
                private boolean cancelled;

                @Override
                public void onAnimationStart(Animator animation) {
                    FloatingActionButtonImpl.this.view.internalSetVisibility(0, fromUser);
                    FloatingActionButtonImpl.this.animState = 1;
                    FloatingActionButtonImpl.this.currentAnimator = animation;
                    this.cancelled = false;
                }

                @Override
                public void onAnimationCancel(Animator animation) {
                    this.cancelled = true;
                }

                @Override
                public void onAnimationEnd(Animator animation) {
                    FloatingActionButtonImpl.this.animState = 0;
                    FloatingActionButtonImpl.this.currentAnimator = null;
                    if (!this.cancelled) {
                        FloatingActionButtonImpl.this.view.internalSetVisibility(fromUser ? 8 : 4, fromUser);
                        if (listener != null) {
                            listener.onHidden();
                        }
                    }
                }
            });
            set.start();
        } else {
            this.view.internalSetVisibility(fromUser ? 8 : 4, fromUser);
            if (listener != null) {
                listener.onHidden();
            }
        }
    }

    void show(final InternalVisibilityChangedListener listener, final boolean fromUser) {
        if (isOrWillBeShown()) {
            return;
        }
        if (this.currentAnimator != null) {
            this.currentAnimator.cancel();
        }
        if (shouldAnimateVisibilityChange()) {
            if (this.view.getVisibility() != 0) {
                this.view.setAlpha(0.0f);
                this.view.setScaleY(0.0f);
                this.view.setScaleX(0.0f);
                setImageMatrixScale(0.0f);
            }
            AnimatorSet set = createAnimator(this.showMotionSpec != null ? this.showMotionSpec : getDefaultShowMotionSpec(), 1.0f, 1.0f, 1.0f);
            set.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationStart(Animator animation) {
                    FloatingActionButtonImpl.this.view.internalSetVisibility(0, fromUser);
                    FloatingActionButtonImpl.this.animState = 2;
                    FloatingActionButtonImpl.this.currentAnimator = animation;
                }

                @Override
                public void onAnimationEnd(Animator animation) {
                    FloatingActionButtonImpl.this.animState = 0;
                    FloatingActionButtonImpl.this.currentAnimator = null;
                    if (listener != null) {
                        listener.onShown();
                    }
                }
            });
            set.start();
            return;
        }
        this.view.internalSetVisibility(0, fromUser);
        this.view.setAlpha(1.0f);
        this.view.setScaleY(1.0f);
        this.view.setScaleX(1.0f);
        setImageMatrixScale(1.0f);
        if (listener != null) {
            listener.onShown();
        }
    }

    private MotionSpec getDefaultShowMotionSpec() {
        if (this.defaultShowMotionSpec == null) {
            this.defaultShowMotionSpec = MotionSpec.createFromResource(this.view.getContext(), R.animator.design_fab_show_motion_spec);
        }
        return this.defaultShowMotionSpec;
    }

    private MotionSpec getDefaultHideMotionSpec() {
        if (this.defaultHideMotionSpec == null) {
            this.defaultHideMotionSpec = MotionSpec.createFromResource(this.view.getContext(), R.animator.design_fab_hide_motion_spec);
        }
        return this.defaultHideMotionSpec;
    }

    private AnimatorSet createAnimator(MotionSpec spec, float opacity, float scale, float iconScale) {
        List<Animator> animators = new ArrayList<>();
        Animator animator = ObjectAnimator.ofFloat(this.view, (Property<VisibilityAwareImageButton, Float>) View.ALPHA, opacity);
        spec.getTiming("opacity").apply(animator);
        animators.add(animator);
        Animator animator2 = ObjectAnimator.ofFloat(this.view, (Property<VisibilityAwareImageButton, Float>) View.SCALE_X, scale);
        spec.getTiming("scale").apply(animator2);
        animators.add(animator2);
        Animator animator3 = ObjectAnimator.ofFloat(this.view, (Property<VisibilityAwareImageButton, Float>) View.SCALE_Y, scale);
        spec.getTiming("scale").apply(animator3);
        animators.add(animator3);
        calculateImageMatrixFromScale(iconScale, this.tmpMatrix);
        Animator animator4 = ObjectAnimator.ofObject(this.view, new ImageMatrixProperty(), new MatrixEvaluator(), new Matrix(this.tmpMatrix));
        spec.getTiming("iconScale").apply(animator4);
        animators.add(animator4);
        AnimatorSet set = new AnimatorSet();
        AnimatorSetCompat.playTogether(set, animators);
        return set;
    }

    final void updatePadding() {
        Rect rect = this.tmpRect;
        getPadding(rect);
        onPaddingUpdated(rect);
        this.shadowViewDelegate.setShadowPadding(rect.left, rect.top, rect.right, rect.bottom);
    }

    void getPadding(Rect rect) {
        this.shadowDrawable.getPadding(rect);
    }

    void onPaddingUpdated(Rect padding) {
    }

    void onAttachedToWindow() {
        if (requirePreDrawListener()) {
            ensurePreDrawListener();
            this.view.getViewTreeObserver().addOnPreDrawListener(this.preDrawListener);
        }
    }

    void onDetachedFromWindow() {
        if (this.preDrawListener != null) {
            this.view.getViewTreeObserver().removeOnPreDrawListener(this.preDrawListener);
            this.preDrawListener = null;
        }
    }

    boolean requirePreDrawListener() {
        return true;
    }

    CircularBorderDrawable createBorderDrawable(int borderWidth, ColorStateList backgroundTint) {
        Context context = this.view.getContext();
        CircularBorderDrawable borderDrawable = newCircularDrawable();
        borderDrawable.setGradientColors(ContextCompat.getColor(context, R.color.design_fab_stroke_top_outer_color), ContextCompat.getColor(context, R.color.design_fab_stroke_top_inner_color), ContextCompat.getColor(context, R.color.design_fab_stroke_end_inner_color), ContextCompat.getColor(context, R.color.design_fab_stroke_end_outer_color));
        borderDrawable.setBorderWidth(borderWidth);
        borderDrawable.setBorderTint(backgroundTint);
        return borderDrawable;
    }

    CircularBorderDrawable newCircularDrawable() {
        return new CircularBorderDrawable();
    }

    void onPreDraw() {
        float rotation = this.view.getRotation();
        if (this.rotation != rotation) {
            this.rotation = rotation;
            updateFromViewRotation();
        }
    }

    private void ensurePreDrawListener() {
        if (this.preDrawListener == null) {
            this.preDrawListener = new ViewTreeObserver.OnPreDrawListener() {
                @Override
                public boolean onPreDraw() {
                    FloatingActionButtonImpl.this.onPreDraw();
                    return true;
                }
            };
        }
    }

    GradientDrawable createShapeDrawable() {
        GradientDrawable d = newGradientDrawableForShape();
        d.setShape(1);
        d.setColor(-1);
        return d;
    }

    GradientDrawable newGradientDrawableForShape() {
        return new GradientDrawable();
    }

    boolean isOrWillBeShown() {
        return this.view.getVisibility() != 0 ? this.animState == 2 : this.animState != 1;
    }

    boolean isOrWillBeHidden() {
        return this.view.getVisibility() == 0 ? this.animState == 1 : this.animState != 2;
    }

    private ValueAnimator createElevationAnimator(ShadowAnimatorImpl impl) {
        ValueAnimator animator = new ValueAnimator();
        animator.setInterpolator(ELEVATION_ANIM_INTERPOLATOR);
        animator.setDuration(100L);
        animator.addListener(impl);
        animator.addUpdateListener(impl);
        animator.setFloatValues(0.0f, 1.0f);
        return animator;
    }

    private abstract class ShadowAnimatorImpl extends AnimatorListenerAdapter implements ValueAnimator.AnimatorUpdateListener {
        private float shadowSizeEnd;
        private float shadowSizeStart;
        private boolean validValues;

        protected abstract float getTargetShadowSize();

        private ShadowAnimatorImpl() {
        }

        @Override
        public void onAnimationUpdate(ValueAnimator animator) {
            if (!this.validValues) {
                this.shadowSizeStart = FloatingActionButtonImpl.this.shadowDrawable.getShadowSize();
                this.shadowSizeEnd = getTargetShadowSize();
                this.validValues = true;
            }
            FloatingActionButtonImpl.this.shadowDrawable.setShadowSize(this.shadowSizeStart + ((this.shadowSizeEnd - this.shadowSizeStart) * animator.getAnimatedFraction()));
        }

        @Override
        public void onAnimationEnd(Animator animator) {
            FloatingActionButtonImpl.this.shadowDrawable.setShadowSize(this.shadowSizeEnd);
            this.validValues = false;
        }
    }

    private class ResetElevationAnimation extends ShadowAnimatorImpl {
        ResetElevationAnimation() {
            super();
        }

        @Override
        protected float getTargetShadowSize() {
            return FloatingActionButtonImpl.this.elevation;
        }
    }

    private class ElevateToHoveredFocusedTranslationZAnimation extends ShadowAnimatorImpl {
        ElevateToHoveredFocusedTranslationZAnimation() {
            super();
        }

        @Override
        protected float getTargetShadowSize() {
            return FloatingActionButtonImpl.this.elevation + FloatingActionButtonImpl.this.hoveredFocusedTranslationZ;
        }
    }

    private class ElevateToPressedTranslationZAnimation extends ShadowAnimatorImpl {
        ElevateToPressedTranslationZAnimation() {
            super();
        }

        @Override
        protected float getTargetShadowSize() {
            return FloatingActionButtonImpl.this.elevation + FloatingActionButtonImpl.this.pressedTranslationZ;
        }
    }

    private class DisabledElevationAnimation extends ShadowAnimatorImpl {
        DisabledElevationAnimation() {
            super();
        }

        @Override
        protected float getTargetShadowSize() {
            return 0.0f;
        }
    }

    private boolean shouldAnimateVisibilityChange() {
        return ViewCompat.isLaidOut(this.view) && !this.view.isInEditMode();
    }

    private void updateFromViewRotation() {
        if (Build.VERSION.SDK_INT == 19) {
            if (this.rotation % 90.0f != 0.0f) {
                if (this.view.getLayerType() != 1) {
                    this.view.setLayerType(1, null);
                }
            } else if (this.view.getLayerType() != 0) {
                this.view.setLayerType(0, null);
            }
        }
        if (this.shadowDrawable != null) {
            this.shadowDrawable.setRotation(-this.rotation);
        }
        if (this.borderDrawable != null) {
            this.borderDrawable.setRotation(-this.rotation);
        }
    }
}
