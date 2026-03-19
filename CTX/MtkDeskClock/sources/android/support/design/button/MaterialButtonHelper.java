package android.support.design.button;

import android.annotation.TargetApi;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.InsetDrawable;
import android.graphics.drawable.LayerDrawable;
import android.graphics.drawable.RippleDrawable;
import android.os.Build;
import android.support.annotation.Nullable;
import android.support.annotation.RestrictTo;
import android.support.design.internal.ViewUtils;
import android.support.design.resources.MaterialResources;
import android.support.design.ripple.RippleUtils;
import android.support.v4.graphics.drawable.DrawableCompat;

@RestrictTo({RestrictTo.Scope.LIBRARY_GROUP})
class MaterialButtonHelper {
    private static final float CORNER_RADIUS_ADJUSTMENT = 1.0E-5f;
    private static final int DEFAULT_BACKGROUND_COLOR = -1;
    private static final boolean IS_LOLLIPOP;

    @Nullable
    private GradientDrawable backgroundDrawableLollipop;

    @Nullable
    private ColorStateList backgroundTint;

    @Nullable
    private PorterDuff.Mode backgroundTintMode;

    @Nullable
    private GradientDrawable colorableBackgroundDrawableCompat;
    private int cornerRadius;
    private int insetBottom;
    private int insetLeft;
    private int insetRight;
    private int insetTop;

    @Nullable
    private GradientDrawable maskDrawableLollipop;
    private final MaterialButton materialButton;

    @Nullable
    private ColorStateList rippleColor;

    @Nullable
    private GradientDrawable rippleDrawableCompat;

    @Nullable
    private ColorStateList strokeColor;

    @Nullable
    private GradientDrawable strokeDrawableLollipop;
    private int strokeWidth;

    @Nullable
    private Drawable tintableBackgroundDrawableCompat;

    @Nullable
    private Drawable tintableRippleDrawableCompat;
    private final Paint buttonStrokePaint = new Paint(1);
    private final Rect bounds = new Rect();
    private final RectF rectF = new RectF();
    private boolean backgroundOverwritten = false;

    static {
        IS_LOLLIPOP = Build.VERSION.SDK_INT >= 21;
    }

    public MaterialButtonHelper(MaterialButton button) {
        this.materialButton = button;
    }

    public void loadFromAttributes(TypedArray attributes) {
        this.insetLeft = attributes.getDimensionPixelOffset(R.styleable.MaterialButton_android_insetLeft, 0);
        this.insetRight = attributes.getDimensionPixelOffset(R.styleable.MaterialButton_android_insetRight, 0);
        this.insetTop = attributes.getDimensionPixelOffset(R.styleable.MaterialButton_android_insetTop, 0);
        this.insetBottom = attributes.getDimensionPixelOffset(R.styleable.MaterialButton_android_insetBottom, 0);
        this.cornerRadius = attributes.getDimensionPixelSize(R.styleable.MaterialButton_cornerRadius, 0);
        this.strokeWidth = attributes.getDimensionPixelSize(R.styleable.MaterialButton_strokeWidth, 0);
        this.backgroundTintMode = ViewUtils.parseTintMode(attributes.getInt(R.styleable.MaterialButton_backgroundTintMode, -1), PorterDuff.Mode.SRC_IN);
        this.backgroundTint = MaterialResources.getColorStateList(this.materialButton.getContext(), attributes, R.styleable.MaterialButton_backgroundTint);
        this.strokeColor = MaterialResources.getColorStateList(this.materialButton.getContext(), attributes, R.styleable.MaterialButton_strokeColor);
        this.rippleColor = MaterialResources.getColorStateList(this.materialButton.getContext(), attributes, R.styleable.MaterialButton_rippleColor);
        this.buttonStrokePaint.setStyle(Paint.Style.STROKE);
        this.buttonStrokePaint.setStrokeWidth(this.strokeWidth);
        this.buttonStrokePaint.setColor(this.strokeColor != null ? this.strokeColor.getColorForState(this.materialButton.getDrawableState(), 0) : 0);
        this.materialButton.setInternalBackground(IS_LOLLIPOP ? createBackgroundLollipop() : createBackgroundCompat());
    }

    void setBackgroundOverwritten() {
        this.backgroundOverwritten = true;
        this.materialButton.setSupportBackgroundTintList(this.backgroundTint);
        this.materialButton.setSupportBackgroundTintMode(this.backgroundTintMode);
    }

    boolean isBackgroundOverwritten() {
        return this.backgroundOverwritten;
    }

    void drawStroke(@Nullable Canvas canvas) {
        if (canvas != null && this.strokeColor != null && this.strokeWidth > 0) {
            this.bounds.set(this.materialButton.getBackground().getBounds());
            this.rectF.set(this.bounds.left + (this.strokeWidth / 2.0f) + this.insetLeft, this.bounds.top + (this.strokeWidth / 2.0f) + this.insetTop, (this.bounds.right - (this.strokeWidth / 2.0f)) - this.insetRight, (this.bounds.bottom - (this.strokeWidth / 2.0f)) - this.insetBottom);
            float strokeCornerRadius = this.cornerRadius - (this.strokeWidth / 2.0f);
            canvas.drawRoundRect(this.rectF, strokeCornerRadius, strokeCornerRadius, this.buttonStrokePaint);
        }
    }

    private Drawable createBackgroundCompat() {
        this.colorableBackgroundDrawableCompat = new GradientDrawable();
        this.colorableBackgroundDrawableCompat.setCornerRadius(this.cornerRadius + CORNER_RADIUS_ADJUSTMENT);
        this.colorableBackgroundDrawableCompat.setColor(-1);
        this.tintableBackgroundDrawableCompat = DrawableCompat.wrap(this.colorableBackgroundDrawableCompat);
        DrawableCompat.setTintList(this.tintableBackgroundDrawableCompat, this.backgroundTint);
        if (this.backgroundTintMode != null) {
            DrawableCompat.setTintMode(this.tintableBackgroundDrawableCompat, this.backgroundTintMode);
        }
        this.rippleDrawableCompat = new GradientDrawable();
        this.rippleDrawableCompat.setCornerRadius(this.cornerRadius + CORNER_RADIUS_ADJUSTMENT);
        this.rippleDrawableCompat.setColor(-1);
        this.tintableRippleDrawableCompat = DrawableCompat.wrap(this.rippleDrawableCompat);
        DrawableCompat.setTintList(this.tintableRippleDrawableCompat, this.rippleColor);
        return wrapDrawableWithInset(new LayerDrawable(new Drawable[]{this.tintableBackgroundDrawableCompat, this.tintableRippleDrawableCompat}));
    }

    private InsetDrawable wrapDrawableWithInset(Drawable drawable) {
        return new InsetDrawable(drawable, this.insetLeft, this.insetTop, this.insetRight, this.insetBottom);
    }

    void setSupportBackgroundTintList(@Nullable ColorStateList tintList) {
        if (this.backgroundTint != tintList) {
            this.backgroundTint = tintList;
            if (IS_LOLLIPOP) {
                updateTintAndTintModeLollipop();
            } else if (this.tintableBackgroundDrawableCompat != null) {
                DrawableCompat.setTintList(this.tintableBackgroundDrawableCompat, this.backgroundTint);
            }
        }
    }

    ColorStateList getSupportBackgroundTintList() {
        return this.backgroundTint;
    }

    void setSupportBackgroundTintMode(@Nullable PorterDuff.Mode mode) {
        if (this.backgroundTintMode != mode) {
            this.backgroundTintMode = mode;
            if (IS_LOLLIPOP) {
                updateTintAndTintModeLollipop();
            } else if (this.tintableBackgroundDrawableCompat != null && this.backgroundTintMode != null) {
                DrawableCompat.setTintMode(this.tintableBackgroundDrawableCompat, this.backgroundTintMode);
            }
        }
    }

    PorterDuff.Mode getSupportBackgroundTintMode() {
        return this.backgroundTintMode;
    }

    private void updateTintAndTintModeLollipop() {
        if (this.backgroundDrawableLollipop != null) {
            DrawableCompat.setTintList(this.backgroundDrawableLollipop, this.backgroundTint);
            if (this.backgroundTintMode != null) {
                DrawableCompat.setTintMode(this.backgroundDrawableLollipop, this.backgroundTintMode);
            }
        }
    }

    @TargetApi(21)
    private Drawable createBackgroundLollipop() {
        this.backgroundDrawableLollipop = new GradientDrawable();
        this.backgroundDrawableLollipop.setCornerRadius(this.cornerRadius + CORNER_RADIUS_ADJUSTMENT);
        this.backgroundDrawableLollipop.setColor(-1);
        updateTintAndTintModeLollipop();
        this.strokeDrawableLollipop = new GradientDrawable();
        this.strokeDrawableLollipop.setCornerRadius(this.cornerRadius + CORNER_RADIUS_ADJUSTMENT);
        this.strokeDrawableLollipop.setColor(0);
        this.strokeDrawableLollipop.setStroke(this.strokeWidth, this.strokeColor);
        LayerDrawable layerDrawable = new LayerDrawable(new Drawable[]{this.backgroundDrawableLollipop, this.strokeDrawableLollipop});
        InsetDrawable bgInsetDrawable = wrapDrawableWithInset(layerDrawable);
        this.maskDrawableLollipop = new GradientDrawable();
        this.maskDrawableLollipop.setCornerRadius(this.cornerRadius + CORNER_RADIUS_ADJUSTMENT);
        this.maskDrawableLollipop.setColor(-1);
        return new MaterialButtonBackgroundDrawable(RippleUtils.convertToRippleDrawableColor(this.rippleColor), bgInsetDrawable, this.maskDrawableLollipop);
    }

    void updateMaskBounds(int height, int width) {
        if (this.maskDrawableLollipop != null) {
            this.maskDrawableLollipop.setBounds(this.insetLeft, this.insetTop, width - this.insetRight, height - this.insetBottom);
        }
    }

    void setBackgroundColor(int color) {
        if (IS_LOLLIPOP && this.backgroundDrawableLollipop != null) {
            this.backgroundDrawableLollipop.setColor(color);
        } else if (!IS_LOLLIPOP && this.colorableBackgroundDrawableCompat != null) {
            this.colorableBackgroundDrawableCompat.setColor(color);
        }
    }

    void setRippleColor(@Nullable ColorStateList rippleColor) {
        if (this.rippleColor != rippleColor) {
            this.rippleColor = rippleColor;
            if (IS_LOLLIPOP && (this.materialButton.getBackground() instanceof RippleDrawable)) {
                ((RippleDrawable) this.materialButton.getBackground()).setColor(rippleColor);
            } else if (!IS_LOLLIPOP && this.tintableRippleDrawableCompat != null) {
                DrawableCompat.setTintList(this.tintableRippleDrawableCompat, rippleColor);
            }
        }
    }

    @Nullable
    ColorStateList getRippleColor() {
        return this.rippleColor;
    }

    void setStrokeColor(@Nullable ColorStateList strokeColor) {
        if (this.strokeColor != strokeColor) {
            this.strokeColor = strokeColor;
            this.buttonStrokePaint.setColor(strokeColor != null ? strokeColor.getColorForState(this.materialButton.getDrawableState(), 0) : 0);
            updateStroke();
        }
    }

    @Nullable
    ColorStateList getStrokeColor() {
        return this.strokeColor;
    }

    void setStrokeWidth(int strokeWidth) {
        if (this.strokeWidth != strokeWidth) {
            this.strokeWidth = strokeWidth;
            this.buttonStrokePaint.setStrokeWidth(strokeWidth);
            updateStroke();
        }
    }

    int getStrokeWidth() {
        return this.strokeWidth;
    }

    private void updateStroke() {
        if (IS_LOLLIPOP && this.strokeDrawableLollipop != null) {
            this.materialButton.setInternalBackground(createBackgroundLollipop());
        } else if (!IS_LOLLIPOP) {
            this.materialButton.invalidate();
        }
    }

    void setCornerRadius(int cornerRadius) {
        if (this.cornerRadius != cornerRadius) {
            this.cornerRadius = cornerRadius;
            if (IS_LOLLIPOP && this.backgroundDrawableLollipop != null && this.strokeDrawableLollipop != null && this.maskDrawableLollipop != null) {
                if (Build.VERSION.SDK_INT == 21) {
                    unwrapBackgroundDrawable().setCornerRadius(cornerRadius + CORNER_RADIUS_ADJUSTMENT);
                    unwrapStrokeDrawable().setCornerRadius(cornerRadius + CORNER_RADIUS_ADJUSTMENT);
                }
                this.backgroundDrawableLollipop.setCornerRadius(cornerRadius + CORNER_RADIUS_ADJUSTMENT);
                this.strokeDrawableLollipop.setCornerRadius(cornerRadius + CORNER_RADIUS_ADJUSTMENT);
                this.maskDrawableLollipop.setCornerRadius(cornerRadius + CORNER_RADIUS_ADJUSTMENT);
                return;
            }
            if (!IS_LOLLIPOP && this.colorableBackgroundDrawableCompat != null && this.rippleDrawableCompat != null) {
                this.colorableBackgroundDrawableCompat.setCornerRadius(cornerRadius + CORNER_RADIUS_ADJUSTMENT);
                this.rippleDrawableCompat.setCornerRadius(cornerRadius + CORNER_RADIUS_ADJUSTMENT);
                this.materialButton.invalidate();
            }
        }
    }

    int getCornerRadius() {
        return this.cornerRadius;
    }

    @Nullable
    private GradientDrawable unwrapStrokeDrawable() {
        if (IS_LOLLIPOP && this.materialButton.getBackground() != null) {
            RippleDrawable background = (RippleDrawable) this.materialButton.getBackground();
            InsetDrawable insetDrawable = (InsetDrawable) background.getDrawable(0);
            LayerDrawable layerDrawable = (LayerDrawable) insetDrawable.getDrawable();
            return (GradientDrawable) layerDrawable.getDrawable(1);
        }
        return null;
    }

    @Nullable
    private GradientDrawable unwrapBackgroundDrawable() {
        if (IS_LOLLIPOP && this.materialButton.getBackground() != null) {
            RippleDrawable background = (RippleDrawable) this.materialButton.getBackground();
            InsetDrawable insetDrawable = (InsetDrawable) background.getDrawable(0);
            LayerDrawable layerDrawable = (LayerDrawable) insetDrawable.getDrawable();
            return (GradientDrawable) layerDrawable.getDrawable(0);
        }
        return null;
    }
}
