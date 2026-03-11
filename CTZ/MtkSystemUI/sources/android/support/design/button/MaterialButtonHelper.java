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
import android.os.Build;
import android.support.design.internal.ViewUtils;
import android.support.design.resources.MaterialResources;
import android.support.design.ripple.RippleUtils;
import android.support.v4.graphics.drawable.DrawableCompat;

class MaterialButtonHelper {
    private static final boolean IS_LOLLIPOP;
    private GradientDrawable backgroundDrawableLollipop;
    private ColorStateList backgroundTint;
    private PorterDuff.Mode backgroundTintMode;
    private GradientDrawable colorableBackgroundDrawableCompat;
    private int cornerRadius;
    private int insetBottom;
    private int insetLeft;
    private int insetRight;
    private int insetTop;
    private GradientDrawable maskDrawableLollipop;
    private final MaterialButton materialButton;
    private ColorStateList rippleColor;
    private GradientDrawable rippleDrawableCompat;
    private ColorStateList strokeColor;
    private GradientDrawable strokeDrawableLollipop;
    private int strokeWidth;
    private Drawable tintableBackgroundDrawableCompat;
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

    void drawStroke(Canvas canvas) {
        if (canvas != null && this.strokeColor != null && this.strokeWidth > 0) {
            this.bounds.set(this.materialButton.getBackground().getBounds());
            this.rectF.set(this.bounds.left + (this.strokeWidth / 2.0f) + this.insetLeft, this.bounds.top + (this.strokeWidth / 2.0f) + this.insetTop, (this.bounds.right - (this.strokeWidth / 2.0f)) - this.insetRight, (this.bounds.bottom - (this.strokeWidth / 2.0f)) - this.insetBottom);
            float strokeCornerRadius = this.cornerRadius - (this.strokeWidth / 2.0f);
            canvas.drawRoundRect(this.rectF, strokeCornerRadius, strokeCornerRadius, this.buttonStrokePaint);
        }
    }

    private Drawable createBackgroundCompat() {
        this.colorableBackgroundDrawableCompat = new GradientDrawable();
        this.colorableBackgroundDrawableCompat.setCornerRadius(this.cornerRadius + 1.0E-5f);
        this.colorableBackgroundDrawableCompat.setColor(-1);
        this.tintableBackgroundDrawableCompat = DrawableCompat.wrap(this.colorableBackgroundDrawableCompat);
        DrawableCompat.setTintList(this.tintableBackgroundDrawableCompat, this.backgroundTint);
        if (this.backgroundTintMode != null) {
            DrawableCompat.setTintMode(this.tintableBackgroundDrawableCompat, this.backgroundTintMode);
        }
        this.rippleDrawableCompat = new GradientDrawable();
        this.rippleDrawableCompat.setCornerRadius(this.cornerRadius + 1.0E-5f);
        this.rippleDrawableCompat.setColor(-1);
        this.tintableRippleDrawableCompat = DrawableCompat.wrap(this.rippleDrawableCompat);
        DrawableCompat.setTintList(this.tintableRippleDrawableCompat, this.rippleColor);
        return wrapDrawableWithInset(new LayerDrawable(new Drawable[]{this.tintableBackgroundDrawableCompat, this.tintableRippleDrawableCompat}));
    }

    private InsetDrawable wrapDrawableWithInset(Drawable drawable) {
        return new InsetDrawable(drawable, this.insetLeft, this.insetTop, this.insetRight, this.insetBottom);
    }

    void setSupportBackgroundTintList(ColorStateList tintList) {
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

    void setSupportBackgroundTintMode(PorterDuff.Mode mode) {
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
        this.backgroundDrawableLollipop.setCornerRadius(this.cornerRadius + 1.0E-5f);
        this.backgroundDrawableLollipop.setColor(-1);
        updateTintAndTintModeLollipop();
        this.strokeDrawableLollipop = new GradientDrawable();
        this.strokeDrawableLollipop.setCornerRadius(this.cornerRadius + 1.0E-5f);
        this.strokeDrawableLollipop.setColor(0);
        this.strokeDrawableLollipop.setStroke(this.strokeWidth, this.strokeColor);
        LayerDrawable layerDrawable = new LayerDrawable(new Drawable[]{this.backgroundDrawableLollipop, this.strokeDrawableLollipop});
        InsetDrawable bgInsetDrawable = wrapDrawableWithInset(layerDrawable);
        this.maskDrawableLollipop = new GradientDrawable();
        this.maskDrawableLollipop.setCornerRadius(this.cornerRadius + 1.0E-5f);
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
}
