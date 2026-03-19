package android.support.design.chip;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Outline;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.support.annotation.AnimatorRes;
import android.support.annotation.AttrRes;
import android.support.annotation.BoolRes;
import android.support.annotation.ColorInt;
import android.support.annotation.ColorRes;
import android.support.annotation.DimenRes;
import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.annotation.StyleRes;
import android.support.annotation.XmlRes;
import android.support.design.animation.MotionSpec;
import android.support.design.canvas.CanvasCompat;
import android.support.design.drawable.DrawableUtils;
import android.support.design.internal.ThemeEnforcement;
import android.support.design.resources.MaterialResources;
import android.support.design.resources.TextAppearance;
import android.support.design.ripple.RippleUtils;
import android.support.v4.graphics.ColorUtils;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.support.v4.graphics.drawable.TintAwareDrawable;
import android.support.v4.internal.view.SupportMenu;
import android.support.v4.text.BidiFormatter;
import android.support.v4.view.ViewCompat;
import android.support.v7.content.res.AppCompatResources;
import android.text.TextPaint;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Xml;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.Arrays;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

public class ChipDrawable extends Drawable implements TintAwareDrawable, Drawable.Callback {
    private static final boolean DEBUG = false;
    private static final int[] DEFAULT_STATE = {android.R.attr.state_enabled};
    private boolean checkable;

    @Nullable
    private Drawable checkedIcon;
    private boolean checkedIconEnabled;

    @Nullable
    private ColorStateList chipBackgroundColor;
    private float chipCornerRadius;
    private float chipEndPadding;

    @Nullable
    private Drawable chipIcon;
    private boolean chipIconEnabled;
    private float chipIconSize;
    private float chipMinHeight;
    private float chipStartPadding;

    @Nullable
    private ColorStateList chipStrokeColor;
    private float chipStrokeWidth;

    @Nullable
    private CharSequence chipText;
    private float chipTextWidth;

    @Nullable
    private Drawable closeIcon;
    private boolean closeIconEnabled;
    private float closeIconEndPadding;
    private float closeIconSize;
    private float closeIconStartPadding;
    private int[] closeIconStateSet;

    @Nullable
    private ColorStateList closeIconTint;

    @Nullable
    private ColorFilter colorFilter;

    @Nullable
    private ColorStateList compatRippleColor;
    private final Context context;
    private boolean currentChecked;

    @ColorInt
    private int currentChipBackgroundColor;

    @ColorInt
    private int currentChipStrokeColor;

    @ColorInt
    private int currentChipTextColor;

    @ColorInt
    private int currentCompatRippleColor;

    @ColorInt
    private int currentTint;

    @Nullable
    private final Paint debugPaint;

    @Nullable
    private MotionSpec hideMotionSpec;
    private float iconEndPadding;
    private float iconStartPadding;

    @Nullable
    private ColorStateList rippleColor;

    @Nullable
    private MotionSpec showMotionSpec;

    @Nullable
    private TextAppearance textAppearance;
    private float textEndPadding;
    private float textStartPadding;

    @Nullable
    private ColorStateList tint;

    @Nullable
    private PorterDuffColorFilter tintFilter;
    private boolean useCompatRipple;
    private final TextPaint textPaint = new TextPaint(1);
    private final Paint chipPaint = new Paint(1);
    private final Paint.FontMetrics fontMetrics = new Paint.FontMetrics();
    private final RectF rectF = new RectF();
    private final PointF pointF = new PointF();
    private int alpha = 255;

    @Nullable
    private PorterDuff.Mode tintMode = PorterDuff.Mode.SRC_IN;
    private WeakReference<Delegate> delegate = new WeakReference<>(null);
    private boolean chipTextWidthDirty = true;

    public interface Delegate {
        void onChipDrawableSizeChange();
    }

    public static ChipDrawable createFromAttributes(Context context, AttributeSet attrs, @AttrRes int defStyleAttr, @StyleRes int defStyleRes) {
        ChipDrawable chip = new ChipDrawable(context);
        chip.loadFromAttributes(attrs, defStyleAttr, defStyleRes);
        return chip;
    }

    public static ChipDrawable createFromResource(Context context, @XmlRes int id) {
        int type;
        try {
            XmlPullParser parser = context.getResources().getXml(id);
            do {
                type = parser.next();
                if (type == 2) {
                    break;
                }
            } while (type != 1);
            if (type != 2) {
                throw new XmlPullParserException("No start tag found");
            }
            if (!TextUtils.equals(parser.getName(), "chip")) {
                throw new XmlPullParserException("Must have a <chip> start tag");
            }
            AttributeSet attrs = Xml.asAttributeSet(parser);
            int style = attrs.getStyleAttribute();
            if (style == 0) {
                style = R.style.Widget_MaterialComponents_Chip_Entry;
            }
            return createFromAttributes(context, attrs, R.attr.chipStandaloneStyle, style);
        } catch (IOException | XmlPullParserException e) {
            Resources.NotFoundException exception = new Resources.NotFoundException("Can't load chip resource ID #0x" + Integer.toHexString(id));
            exception.initCause(e);
            throw exception;
        }
    }

    private ChipDrawable(Context context) {
        this.context = context;
        this.textPaint.density = context.getResources().getDisplayMetrics().density;
        this.debugPaint = null;
        if (this.debugPaint != null) {
            this.debugPaint.setStyle(Paint.Style.STROKE);
        }
        setState(DEFAULT_STATE);
        setCloseIconState(DEFAULT_STATE);
    }

    private void loadFromAttributes(AttributeSet attrs, @AttrRes int defStyleAttr, @StyleRes int defStyleRes) {
        TypedArray a = ThemeEnforcement.obtainStyledAttributes(this.context, attrs, R.styleable.ChipDrawable, defStyleAttr, defStyleRes);
        setChipBackgroundColor(MaterialResources.getColorStateList(this.context, a, R.styleable.ChipDrawable_chipBackgroundColor));
        setChipMinHeight(a.getDimension(R.styleable.ChipDrawable_chipMinHeight, 0.0f));
        setChipCornerRadius(a.getDimension(R.styleable.ChipDrawable_chipCornerRadius, 0.0f));
        setChipStrokeColor(MaterialResources.getColorStateList(this.context, a, R.styleable.ChipDrawable_chipStrokeColor));
        setChipStrokeWidth(a.getDimension(R.styleable.ChipDrawable_chipStrokeWidth, 0.0f));
        setRippleColor(MaterialResources.getColorStateList(this.context, a, R.styleable.ChipDrawable_rippleColor));
        setChipText(a.getText(R.styleable.ChipDrawable_chipText));
        setTextAppearance(MaterialResources.getTextAppearance(this.context, a, R.styleable.ChipDrawable_android_textAppearance));
        setChipIconEnabled(a.getBoolean(R.styleable.ChipDrawable_chipIconEnabled, false));
        setChipIcon(MaterialResources.getDrawable(this.context, a, R.styleable.ChipDrawable_chipIcon));
        setChipIconSize(a.getDimension(R.styleable.ChipDrawable_chipIconSize, 0.0f));
        setCloseIconEnabled(a.getBoolean(R.styleable.ChipDrawable_closeIconEnabled, false));
        setCloseIcon(MaterialResources.getDrawable(this.context, a, R.styleable.ChipDrawable_closeIcon));
        setCloseIconTint(MaterialResources.getColorStateList(this.context, a, R.styleable.ChipDrawable_closeIconTint));
        setCloseIconSize(a.getDimension(R.styleable.ChipDrawable_closeIconSize, 0.0f));
        setCheckable(a.getBoolean(R.styleable.ChipDrawable_android_checkable, false));
        setCheckedIconEnabled(a.getBoolean(R.styleable.ChipDrawable_checkedIconEnabled, false));
        setCheckedIcon(MaterialResources.getDrawable(this.context, a, R.styleable.ChipDrawable_checkedIcon));
        setShowMotionSpec(MotionSpec.createFromAttribute(this.context, a, R.styleable.ChipDrawable_showMotionSpec));
        setHideMotionSpec(MotionSpec.createFromAttribute(this.context, a, R.styleable.ChipDrawable_hideMotionSpec));
        setChipStartPadding(a.getDimension(R.styleable.ChipDrawable_chipStartPadding, 0.0f));
        setIconStartPadding(a.getDimension(R.styleable.ChipDrawable_iconStartPadding, 0.0f));
        setIconEndPadding(a.getDimension(R.styleable.ChipDrawable_iconEndPadding, 0.0f));
        setTextStartPadding(a.getDimension(R.styleable.ChipDrawable_textStartPadding, 0.0f));
        setTextEndPadding(a.getDimension(R.styleable.ChipDrawable_textEndPadding, 0.0f));
        setCloseIconStartPadding(a.getDimension(R.styleable.ChipDrawable_closeIconStartPadding, 0.0f));
        setCloseIconEndPadding(a.getDimension(R.styleable.ChipDrawable_closeIconEndPadding, 0.0f));
        setChipEndPadding(a.getDimension(R.styleable.ChipDrawable_chipEndPadding, 0.0f));
        a.recycle();
    }

    public void setUseCompatRipple(boolean useCompatRipple) {
        if (this.useCompatRipple != useCompatRipple) {
            this.useCompatRipple = useCompatRipple;
            updateCompatRippleColor();
            onStateChange(getState());
        }
    }

    public boolean getUseCompatRipple() {
        return this.useCompatRipple;
    }

    public void setDelegate(@Nullable Delegate delegate) {
        this.delegate = new WeakReference<>(delegate);
    }

    protected void onSizeChange() {
        Delegate delegate = this.delegate.get();
        if (delegate != null) {
            delegate.onChipDrawableSizeChange();
        }
    }

    public void getChipTouchBounds(RectF bounds) {
        calculateChipTouchBounds(getBounds(), bounds);
    }

    public void getCloseIconTouchBounds(RectF bounds) {
        calculateCloseIconTouchBounds(getBounds(), bounds);
    }

    @Override
    public int getIntrinsicWidth() {
        return (int) (this.chipStartPadding + calculateChipIconWidth() + this.textStartPadding + getChipTextWidth() + this.textEndPadding + calculateCloseIconWidth() + this.chipEndPadding);
    }

    @Override
    public int getIntrinsicHeight() {
        return (int) this.chipMinHeight;
    }

    private boolean showsChipIcon() {
        return this.chipIconEnabled && this.chipIcon != null;
    }

    private boolean showsCheckedIcon() {
        return this.checkedIconEnabled && this.checkedIcon != null && this.currentChecked;
    }

    private boolean showsCloseIcon() {
        return this.closeIconEnabled && this.closeIcon != null;
    }

    private boolean canShowCheckedIcon() {
        return this.checkedIconEnabled && this.checkedIcon != null && this.checkable;
    }

    private float calculateChipIconWidth() {
        if (showsChipIcon() || showsCheckedIcon()) {
            return this.iconStartPadding + this.chipIconSize + this.iconEndPadding;
        }
        return 0.0f;
    }

    private float getChipTextWidth() {
        if (!this.chipTextWidthDirty) {
            return this.chipTextWidth;
        }
        this.chipTextWidth = calculateChipTextWidth(this.chipText);
        this.chipTextWidthDirty = false;
        return this.chipTextWidth;
    }

    private float calculateChipTextWidth(@Nullable CharSequence charSequence) {
        if (charSequence == null) {
            return 0.0f;
        }
        return this.textPaint.measureText(charSequence, 0, charSequence.length());
    }

    private float calculateCloseIconWidth() {
        if (showsCloseIcon()) {
            return this.closeIconStartPadding + this.closeIconSize + this.closeIconEndPadding;
        }
        return 0.0f;
    }

    @Override
    public void draw(@NonNull Canvas canvas) {
        Rect bounds = getBounds();
        if (bounds.isEmpty() || getAlpha() == 0) {
            return;
        }
        int saveCount = 0;
        if (this.alpha < 255) {
            saveCount = CanvasCompat.saveLayerAlpha(canvas, bounds.left, bounds.top, bounds.right, bounds.bottom, this.alpha);
        }
        drawChipBackground(canvas, bounds);
        drawChipStroke(canvas, bounds);
        drawCompatRipple(canvas, bounds);
        drawChipIcon(canvas, bounds);
        drawCheckedIcon(canvas, bounds);
        drawChipText(canvas, bounds);
        drawCloseIcon(canvas, bounds);
        drawDebug(canvas, bounds);
        if (this.alpha < 255) {
            canvas.restoreToCount(saveCount);
        }
    }

    private void drawChipBackground(@NonNull Canvas canvas, Rect bounds) {
        this.chipPaint.setColor(this.currentChipBackgroundColor);
        this.chipPaint.setStyle(Paint.Style.FILL);
        this.chipPaint.setColorFilter(getTintColorFilter());
        this.rectF.set(bounds);
        canvas.drawRoundRect(this.rectF, this.chipCornerRadius, this.chipCornerRadius, this.chipPaint);
    }

    private void drawChipStroke(@NonNull Canvas canvas, Rect bounds) {
        if (this.chipStrokeWidth > 0.0f) {
            this.chipPaint.setColor(this.currentChipStrokeColor);
            this.chipPaint.setStyle(Paint.Style.STROKE);
            this.chipPaint.setColorFilter(getTintColorFilter());
            this.rectF.set(bounds.left + (this.chipStrokeWidth / 2.0f), bounds.top + (this.chipStrokeWidth / 2.0f), bounds.right - (this.chipStrokeWidth / 2.0f), bounds.bottom - (this.chipStrokeWidth / 2.0f));
            float strokeCornerRadius = this.chipCornerRadius - (this.chipStrokeWidth / 2.0f);
            canvas.drawRoundRect(this.rectF, strokeCornerRadius, strokeCornerRadius, this.chipPaint);
        }
    }

    private void drawCompatRipple(@NonNull Canvas canvas, Rect bounds) {
        this.chipPaint.setColor(this.currentCompatRippleColor);
        this.chipPaint.setStyle(Paint.Style.FILL);
        this.rectF.set(bounds);
        canvas.drawRoundRect(this.rectF, this.chipCornerRadius, this.chipCornerRadius, this.chipPaint);
    }

    private void drawChipIcon(@NonNull Canvas canvas, Rect bounds) {
        if (showsChipIcon()) {
            calculateChipIconBounds(bounds, this.rectF);
            float tx = this.rectF.left;
            float ty = this.rectF.top;
            canvas.translate(tx, ty);
            this.chipIcon.setBounds(0, 0, (int) this.rectF.width(), (int) this.rectF.height());
            this.chipIcon.draw(canvas);
            canvas.translate(-tx, -ty);
        }
    }

    private void drawCheckedIcon(@NonNull Canvas canvas, Rect bounds) {
        if (showsCheckedIcon()) {
            calculateChipIconBounds(bounds, this.rectF);
            float tx = this.rectF.left;
            float ty = this.rectF.top;
            canvas.translate(tx, ty);
            this.checkedIcon.setBounds(0, 0, (int) this.rectF.width(), (int) this.rectF.height());
            this.checkedIcon.draw(canvas);
            canvas.translate(-tx, -ty);
        }
    }

    private void drawChipText(@NonNull Canvas canvas, Rect bounds) {
        if (this.chipText != null) {
            Paint.Align align = calculateChipTextOrigin(bounds, this.pointF);
            calculateChipTextBounds(bounds, this.rectF);
            if (this.textAppearance != null) {
                this.textPaint.drawableState = getState();
                this.textAppearance.updateDrawState(this.context, this.textPaint);
            }
            this.textPaint.setTextAlign(align);
            boolean clip = getChipTextWidth() > this.rectF.width();
            int saveCount = 0;
            if (clip) {
                saveCount = canvas.save();
                canvas.clipRect(this.rectF);
            }
            canvas.drawText(this.chipText, 0, this.chipText.length(), this.pointF.x, this.pointF.y, this.textPaint);
            if (clip) {
                canvas.restoreToCount(saveCount);
            }
        }
    }

    private void drawCloseIcon(@NonNull Canvas canvas, Rect bounds) {
        if (showsCloseIcon()) {
            calculateCloseIconBounds(bounds, this.rectF);
            float tx = this.rectF.left;
            float ty = this.rectF.top;
            canvas.translate(tx, ty);
            this.closeIcon.setBounds(0, 0, (int) this.rectF.width(), (int) this.rectF.height());
            this.closeIcon.draw(canvas);
            canvas.translate(-tx, -ty);
        }
    }

    private void drawDebug(@NonNull Canvas canvas, Rect bounds) {
        if (this.debugPaint != null) {
            this.debugPaint.setColor(ColorUtils.setAlphaComponent(ViewCompat.MEASURED_STATE_MASK, 127));
            canvas.drawRect(bounds, this.debugPaint);
            if (showsChipIcon() || showsCheckedIcon()) {
                calculateChipIconBounds(bounds, this.rectF);
                canvas.drawRect(this.rectF, this.debugPaint);
            }
            if (this.chipText != null) {
                canvas.drawLine(bounds.left, bounds.exactCenterY(), bounds.right, bounds.exactCenterY(), this.debugPaint);
            }
            if (showsCloseIcon()) {
                calculateCloseIconBounds(bounds, this.rectF);
                canvas.drawRect(this.rectF, this.debugPaint);
            }
            this.debugPaint.setColor(ColorUtils.setAlphaComponent(SupportMenu.CATEGORY_MASK, 127));
            calculateChipTouchBounds(bounds, this.rectF);
            canvas.drawRect(this.rectF, this.debugPaint);
            this.debugPaint.setColor(ColorUtils.setAlphaComponent(-16711936, 127));
            calculateCloseIconTouchBounds(bounds, this.rectF);
            canvas.drawRect(this.rectF, this.debugPaint);
        }
    }

    private void calculateChipIconBounds(Rect bounds, RectF outBounds) {
        outBounds.setEmpty();
        if (showsChipIcon() || showsCheckedIcon()) {
            float offsetFromStart = this.chipStartPadding + this.iconStartPadding;
            if (DrawableCompat.getLayoutDirection(this) == 0) {
                outBounds.left = bounds.left + offsetFromStart;
                outBounds.right = outBounds.left + this.chipIconSize;
            } else {
                outBounds.right = bounds.right - offsetFromStart;
                outBounds.left = outBounds.right - this.chipIconSize;
            }
            outBounds.top = bounds.exactCenterY() - (this.chipIconSize / 2.0f);
            outBounds.bottom = outBounds.top + this.chipIconSize;
        }
    }

    private Paint.Align calculateChipTextOrigin(Rect bounds, PointF pointF) {
        pointF.set(0.0f, 0.0f);
        Paint.Align align = Paint.Align.LEFT;
        if (this.chipText != null) {
            float offsetFromStart = this.chipStartPadding + calculateChipIconWidth() + this.textStartPadding;
            if (DrawableCompat.getLayoutDirection(this) == 0) {
                pointF.x = bounds.left + offsetFromStart;
                align = Paint.Align.LEFT;
            } else {
                pointF.x = bounds.right - offsetFromStart;
                align = Paint.Align.RIGHT;
            }
            pointF.y = bounds.centerY() - calculateChipTextCenterFromBaseline();
        }
        return align;
    }

    private float calculateChipTextCenterFromBaseline() {
        this.textPaint.getFontMetrics(this.fontMetrics);
        return (this.fontMetrics.descent + this.fontMetrics.ascent) / 2.0f;
    }

    private void calculateChipTextBounds(Rect bounds, RectF outBounds) {
        outBounds.setEmpty();
        if (this.chipText != null) {
            float offsetFromStart = this.chipStartPadding + calculateChipIconWidth() + this.textStartPadding;
            float offsetFromEnd = this.chipEndPadding + calculateCloseIconWidth() + this.textEndPadding;
            if (DrawableCompat.getLayoutDirection(this) == 0) {
                outBounds.left = bounds.left + offsetFromStart;
                outBounds.right = bounds.right - offsetFromEnd;
            } else {
                outBounds.left = bounds.left + offsetFromEnd;
                outBounds.right = bounds.right - offsetFromStart;
            }
            outBounds.top = bounds.top;
            outBounds.bottom = bounds.bottom;
        }
    }

    private void calculateCloseIconBounds(Rect bounds, RectF outBounds) {
        outBounds.setEmpty();
        if (showsCloseIcon()) {
            float offsetFromEnd = this.chipEndPadding + this.closeIconEndPadding;
            if (DrawableCompat.getLayoutDirection(this) == 0) {
                outBounds.right = bounds.right - offsetFromEnd;
                outBounds.left = outBounds.right - this.closeIconSize;
            } else {
                outBounds.left = bounds.left + offsetFromEnd;
                outBounds.right = outBounds.left + this.closeIconSize;
            }
            outBounds.top = bounds.exactCenterY() - (this.closeIconSize / 2.0f);
            outBounds.bottom = outBounds.top + this.closeIconSize;
        }
    }

    private void calculateChipTouchBounds(Rect bounds, RectF outBounds) {
        outBounds.set(bounds);
        if (showsCloseIcon()) {
            float offsetFromEnd = this.chipEndPadding + this.closeIconEndPadding + this.closeIconSize + this.closeIconStartPadding + this.textEndPadding;
            if (DrawableCompat.getLayoutDirection(this) == 0) {
                outBounds.right = bounds.right - offsetFromEnd;
            } else {
                outBounds.left = bounds.left + offsetFromEnd;
            }
        }
    }

    private void calculateCloseIconTouchBounds(Rect bounds, RectF outBounds) {
        outBounds.setEmpty();
        if (showsCloseIcon()) {
            float offsetFromEnd = this.chipEndPadding + this.closeIconEndPadding + this.closeIconSize + this.closeIconStartPadding + this.textEndPadding;
            if (DrawableCompat.getLayoutDirection(this) == 0) {
                outBounds.right = bounds.right;
                outBounds.left = outBounds.right - offsetFromEnd;
            } else {
                outBounds.left = bounds.left;
                outBounds.right = bounds.left + offsetFromEnd;
            }
            outBounds.top = bounds.top;
            outBounds.bottom = bounds.bottom;
        }
    }

    @Override
    public boolean isStateful() {
        return isStateful(this.chipBackgroundColor) || isStateful(this.chipStrokeColor) || (this.useCompatRipple && isStateful(this.compatRippleColor)) || isStateful(this.textAppearance) || canShowCheckedIcon() || isStateful(this.chipIcon) || isStateful(this.checkedIcon) || isStateful(this.tint);
    }

    public boolean isCloseIconStateful() {
        return isStateful(this.closeIcon);
    }

    public boolean setCloseIconState(@NonNull int[] stateSet) {
        if (!Arrays.equals(this.closeIconStateSet, stateSet)) {
            this.closeIconStateSet = stateSet;
            if (showsCloseIcon()) {
                return onStateChange(getState(), stateSet);
            }
            return false;
        }
        return false;
    }

    @NonNull
    public int[] getCloseIconState() {
        return this.closeIconStateSet;
    }

    @Override
    protected boolean onStateChange(int[] state) {
        return onStateChange(state, getCloseIconState());
    }

    private boolean onStateChange(int[] chipState, int[] closeIconState) {
        boolean invalidate = super.onStateChange(chipState);
        boolean sizeChanged = false;
        int newChipBackgroundColor = this.chipBackgroundColor != null ? this.chipBackgroundColor.getColorForState(chipState, this.currentChipBackgroundColor) : 0;
        if (this.currentChipBackgroundColor != newChipBackgroundColor) {
            this.currentChipBackgroundColor = newChipBackgroundColor;
            invalidate = true;
        }
        int newChipStrokeColor = this.chipStrokeColor != null ? this.chipStrokeColor.getColorForState(chipState, this.currentChipStrokeColor) : 0;
        if (this.currentChipStrokeColor != newChipStrokeColor) {
            this.currentChipStrokeColor = newChipStrokeColor;
            invalidate = true;
        }
        int newCompatRippleColor = this.compatRippleColor != null ? this.compatRippleColor.getColorForState(chipState, this.currentCompatRippleColor) : 0;
        if (this.currentCompatRippleColor != newCompatRippleColor) {
            this.currentCompatRippleColor = newCompatRippleColor;
            if (this.useCompatRipple) {
                invalidate = true;
            }
        }
        int newChipTextColor = (this.textAppearance == null || this.textAppearance.textColor == null) ? 0 : this.textAppearance.textColor.getColorForState(chipState, this.currentChipTextColor);
        if (this.currentChipTextColor != newChipTextColor) {
            this.currentChipTextColor = newChipTextColor;
            invalidate = true;
        }
        boolean newChecked = hasState(getState(), android.R.attr.state_checked) && this.checkable;
        if (this.currentChecked != newChecked && this.checkedIcon != null) {
            float oldChipIconWidth = calculateChipIconWidth();
            this.currentChecked = newChecked;
            float newChipIconWidth = calculateChipIconWidth();
            invalidate = true;
            if (oldChipIconWidth != newChipIconWidth) {
                sizeChanged = true;
            }
        }
        int newTint = this.tint != null ? this.tint.getColorForState(chipState, this.currentTint) : 0;
        if (this.currentTint != newTint) {
            this.currentTint = newTint;
            this.tintFilter = DrawableUtils.updateTintFilter(this, this.tint, this.tintMode);
            invalidate = true;
        }
        if (isStateful(this.chipIcon)) {
            invalidate |= this.chipIcon.setState(chipState);
        }
        if (isStateful(this.checkedIcon)) {
            invalidate |= this.checkedIcon.setState(chipState);
        }
        if (isStateful(this.closeIcon)) {
            invalidate |= this.closeIcon.setState(closeIconState);
        }
        if (invalidate) {
            invalidateSelf();
        }
        if (sizeChanged) {
            onSizeChange();
        }
        return invalidate;
    }

    private static boolean isStateful(@Nullable ColorStateList colorStateList) {
        return colorStateList != null && colorStateList.isStateful();
    }

    private static boolean isStateful(@Nullable Drawable drawable) {
        return drawable != null && drawable.isStateful();
    }

    private static boolean isStateful(@Nullable TextAppearance textAppearance) {
        return (textAppearance == null || textAppearance.textColor == null || !textAppearance.textColor.isStateful()) ? false : true;
    }

    @Override
    @TargetApi(23)
    public boolean onLayoutDirectionChanged(int layoutDirection) {
        boolean invalidate = super.onLayoutDirectionChanged(layoutDirection);
        if (showsChipIcon()) {
            invalidate |= this.chipIcon.setLayoutDirection(layoutDirection);
        }
        if (showsCheckedIcon()) {
            invalidate |= this.checkedIcon.setLayoutDirection(layoutDirection);
        }
        if (showsCloseIcon()) {
            invalidate |= this.closeIcon.setLayoutDirection(layoutDirection);
        }
        if (invalidate) {
            invalidateSelf();
            return true;
        }
        return true;
    }

    @Override
    protected boolean onLevelChange(int level) {
        boolean invalidate = super.onLevelChange(level);
        if (showsChipIcon()) {
            invalidate |= this.chipIcon.setLevel(level);
        }
        if (showsCheckedIcon()) {
            invalidate |= this.checkedIcon.setLevel(level);
        }
        if (showsCloseIcon()) {
            invalidate |= this.closeIcon.setLevel(level);
        }
        if (invalidate) {
            invalidateSelf();
        }
        return invalidate;
    }

    @Override
    public boolean setVisible(boolean visible, boolean restart) {
        boolean invalidate = super.setVisible(visible, restart);
        if (showsChipIcon()) {
            invalidate |= this.chipIcon.setVisible(visible, restart);
        }
        if (showsCheckedIcon()) {
            invalidate |= this.checkedIcon.setVisible(visible, restart);
        }
        if (showsCloseIcon()) {
            invalidate |= this.closeIcon.setVisible(visible, restart);
        }
        if (invalidate) {
            invalidateSelf();
        }
        return invalidate;
    }

    @Override
    public void setAlpha(int alpha) {
        if (this.alpha != alpha) {
            this.alpha = alpha;
            invalidateSelf();
        }
    }

    @Override
    public int getAlpha() {
        return this.alpha;
    }

    @Override
    public void setColorFilter(@Nullable ColorFilter colorFilter) {
        if (this.colorFilter != colorFilter) {
            this.colorFilter = colorFilter;
            invalidateSelf();
        }
    }

    @Override
    @Nullable
    public ColorFilter getColorFilter() {
        return this.colorFilter;
    }

    @Override
    public void setTintList(@Nullable ColorStateList tint) {
        if (this.tint != tint) {
            this.tint = tint;
            onStateChange(getState());
        }
    }

    @Override
    public void setTintMode(@NonNull PorterDuff.Mode tintMode) {
        if (this.tintMode != tintMode) {
            this.tintMode = tintMode;
            this.tintFilter = DrawableUtils.updateTintFilter(this, this.tint, tintMode);
            invalidateSelf();
        }
    }

    @Override
    public int getOpacity() {
        return -3;
    }

    @Override
    @TargetApi(21)
    public void getOutline(@NonNull Outline outline) {
        Rect bounds = getBounds();
        if (!bounds.isEmpty()) {
            outline.setRoundRect(bounds, this.chipCornerRadius);
        } else {
            outline.setRoundRect(0, 0, getIntrinsicWidth(), getIntrinsicHeight(), this.chipCornerRadius);
        }
        outline.setAlpha(getAlpha() / 255.0f);
    }

    @Override
    public void invalidateDrawable(@NonNull Drawable who) {
        Drawable.Callback callback = getCallback();
        if (callback != null) {
            callback.invalidateDrawable(this);
        }
    }

    @Override
    public void scheduleDrawable(@NonNull Drawable who, @NonNull Runnable what, long when) {
        Drawable.Callback callback = getCallback();
        if (callback != null) {
            callback.scheduleDrawable(this, what, when);
        }
    }

    @Override
    public void unscheduleDrawable(@NonNull Drawable who, @NonNull Runnable what) {
        Drawable.Callback callback = getCallback();
        if (callback != null) {
            callback.unscheduleDrawable(this, what);
        }
    }

    private void unapplyChildDrawable(@Nullable Drawable drawable) {
        if (drawable != null) {
            drawable.setCallback(null);
        }
    }

    private void applyChildDrawable(@Nullable Drawable drawable) {
        if (drawable != null) {
            drawable.setCallback(this);
            DrawableCompat.setLayoutDirection(drawable, DrawableCompat.getLayoutDirection(this));
            drawable.setLevel(getLevel());
            drawable.setVisible(isVisible(), false);
            if (drawable == this.closeIcon) {
                if (drawable.isStateful()) {
                    drawable.setState(getCloseIconState());
                }
                DrawableCompat.setTintList(drawable, this.closeIconTint);
            } else if (drawable.isStateful()) {
                drawable.setState(getState());
            }
        }
    }

    @Nullable
    private ColorFilter getTintColorFilter() {
        return this.colorFilter != null ? this.colorFilter : this.tintFilter;
    }

    private void updateCompatRippleColor() {
        this.compatRippleColor = this.useCompatRipple ? RippleUtils.convertToRippleDrawableColor(this.rippleColor) : null;
    }

    private static boolean hasState(@Nullable int[] stateSet, @AttrRes int state) {
        if (stateSet == null) {
            return false;
        }
        for (int s : stateSet) {
            if (s == state) {
                return true;
            }
        }
        return false;
    }

    @Nullable
    public ColorStateList getChipBackgroundColor() {
        return this.chipBackgroundColor;
    }

    public void setChipBackgroundColorResource(@ColorRes int id) {
        setChipBackgroundColor(AppCompatResources.getColorStateList(this.context, id));
    }

    public void setChipBackgroundColor(@Nullable ColorStateList chipBackgroundColor) {
        if (this.chipBackgroundColor != chipBackgroundColor) {
            this.chipBackgroundColor = chipBackgroundColor;
            onStateChange(getState());
        }
    }

    public float getChipMinHeight() {
        return this.chipMinHeight;
    }

    public void setChipMinHeightResource(@DimenRes int id) {
        setChipMinHeight(this.context.getResources().getDimension(id));
    }

    public void setChipMinHeight(float chipMinHeight) {
        if (this.chipMinHeight != chipMinHeight) {
            this.chipMinHeight = chipMinHeight;
            invalidateSelf();
            onSizeChange();
        }
    }

    public float getChipCornerRadius() {
        return this.chipCornerRadius;
    }

    public void setChipCornerRadiusResource(@DimenRes int id) {
        setChipCornerRadius(this.context.getResources().getDimension(id));
    }

    public void setChipCornerRadius(float chipCornerRadius) {
        if (this.chipCornerRadius != chipCornerRadius) {
            this.chipCornerRadius = chipCornerRadius;
            invalidateSelf();
        }
    }

    @Nullable
    public ColorStateList getChipStrokeColor() {
        return this.chipStrokeColor;
    }

    public void setChipStrokeColorResource(@ColorRes int id) {
        setChipStrokeColor(AppCompatResources.getColorStateList(this.context, id));
    }

    public void setChipStrokeColor(@Nullable ColorStateList chipStrokeColor) {
        if (this.chipStrokeColor != chipStrokeColor) {
            this.chipStrokeColor = chipStrokeColor;
            onStateChange(getState());
        }
    }

    public float getChipStrokeWidth() {
        return this.chipStrokeWidth;
    }

    public void setChipStrokeWidthResource(@DimenRes int id) {
        setChipStrokeWidth(this.context.getResources().getDimension(id));
    }

    public void setChipStrokeWidth(float chipStrokeWidth) {
        if (this.chipStrokeWidth != chipStrokeWidth) {
            this.chipStrokeWidth = chipStrokeWidth;
            this.chipPaint.setStrokeWidth(chipStrokeWidth);
            invalidateSelf();
        }
    }

    @Nullable
    public ColorStateList getRippleColor() {
        return this.rippleColor;
    }

    public void setRippleColorResource(@ColorRes int id) {
        setRippleColor(AppCompatResources.getColorStateList(this.context, id));
    }

    public void setRippleColor(@Nullable ColorStateList rippleColor) {
        if (this.rippleColor != rippleColor) {
            this.rippleColor = rippleColor;
            updateCompatRippleColor();
            onStateChange(getState());
        }
    }

    @Nullable
    public CharSequence getChipText() {
        return this.chipText;
    }

    public void setChipTextResource(@StringRes int id) {
        setChipText(this.context.getResources().getString(id));
    }

    public void setChipText(@Nullable CharSequence chipText) {
        if (this.chipText != chipText) {
            this.chipText = BidiFormatter.getInstance().unicodeWrap(chipText);
            this.chipTextWidthDirty = true;
            invalidateSelf();
            onSizeChange();
        }
    }

    @Nullable
    public TextAppearance getTextAppearance() {
        return this.textAppearance;
    }

    public void setTextAppearanceResource(@StyleRes int id) {
        setTextAppearance(new TextAppearance(this.context, id));
    }

    public void setTextAppearance(@Nullable TextAppearance textAppearance) {
        if (this.textAppearance != textAppearance) {
            this.textAppearance = textAppearance;
            if (textAppearance != null) {
                textAppearance.updateMeasureState(this.context, this.textPaint);
                this.chipTextWidthDirty = true;
            }
            onStateChange(getState());
            onSizeChange();
        }
    }

    public boolean isChipIconEnabled() {
        return this.chipIconEnabled;
    }

    public void setChipIconEnabledResource(@BoolRes int id) {
        setChipIconEnabled(this.context.getResources().getBoolean(id));
    }

    public void setChipIconEnabled(boolean chipIconEnabled) {
        if (this.chipIconEnabled != chipIconEnabled) {
            boolean oldShowsChipIcon = showsChipIcon();
            this.chipIconEnabled = chipIconEnabled;
            boolean newShowsChipIcon = showsChipIcon();
            boolean changed = oldShowsChipIcon != newShowsChipIcon;
            if (changed) {
                if (newShowsChipIcon) {
                    applyChildDrawable(this.chipIcon);
                } else {
                    unapplyChildDrawable(this.chipIcon);
                }
                invalidateSelf();
                onSizeChange();
            }
        }
    }

    @Nullable
    public Drawable getChipIcon() {
        return this.chipIcon;
    }

    public void setChipIconResource(@DrawableRes int id) {
        setChipIcon(AppCompatResources.getDrawable(this.context, id));
    }

    public void setChipIcon(@Nullable Drawable chipIcon) {
        Drawable oldChipIcon = this.chipIcon;
        if (oldChipIcon != chipIcon) {
            float oldChipIconWidth = calculateChipIconWidth();
            this.chipIcon = chipIcon;
            float newChipIconWidth = calculateChipIconWidth();
            unapplyChildDrawable(oldChipIcon);
            if (showsChipIcon()) {
                applyChildDrawable(this.chipIcon);
            }
            invalidateSelf();
            if (oldChipIconWidth != newChipIconWidth) {
                onSizeChange();
            }
        }
    }

    public float getChipIconSize() {
        return this.chipIconSize;
    }

    public void setChipIconSizeResource(@DimenRes int id) {
        setChipIconSize(this.context.getResources().getDimension(id));
    }

    public void setChipIconSize(float chipIconSize) {
        if (this.chipIconSize != chipIconSize) {
            float oldChipIconWidth = calculateChipIconWidth();
            this.chipIconSize = chipIconSize;
            float newChipIconWidth = calculateChipIconWidth();
            invalidateSelf();
            if (oldChipIconWidth != newChipIconWidth) {
                onSizeChange();
            }
        }
    }

    public boolean isCloseIconEnabled() {
        return this.closeIconEnabled;
    }

    public void setCloseIconEnabledResource(@BoolRes int id) {
        setCloseIconEnabled(this.context.getResources().getBoolean(id));
    }

    public void setCloseIconEnabled(boolean closeIconEnabled) {
        if (this.closeIconEnabled != closeIconEnabled) {
            boolean oldShowsCloseIcon = showsCloseIcon();
            this.closeIconEnabled = closeIconEnabled;
            boolean newShowsCloseIcon = showsCloseIcon();
            boolean changed = oldShowsCloseIcon != newShowsCloseIcon;
            if (changed) {
                if (newShowsCloseIcon) {
                    applyChildDrawable(this.closeIcon);
                } else {
                    unapplyChildDrawable(this.closeIcon);
                }
                invalidateSelf();
                onSizeChange();
            }
        }
    }

    @Nullable
    public Drawable getCloseIcon() {
        return this.closeIcon;
    }

    public void setCloseIconResource(@DrawableRes int id) {
        setCloseIcon(AppCompatResources.getDrawable(this.context, id));
    }

    public void setCloseIcon(@Nullable Drawable closeIcon) {
        Drawable oldCloseIcon = this.closeIcon != null ? DrawableCompat.unwrap(this.closeIcon) : null;
        if (oldCloseIcon != closeIcon) {
            float oldCloseIconWidth = calculateCloseIconWidth();
            this.closeIcon = closeIcon != null ? DrawableCompat.wrap(closeIcon).mutate() : null;
            float newCloseIconWidth = calculateCloseIconWidth();
            unapplyChildDrawable(oldCloseIcon);
            if (showsCloseIcon()) {
                applyChildDrawable(this.closeIcon);
            }
            invalidateSelf();
            if (oldCloseIconWidth != newCloseIconWidth) {
                onSizeChange();
            }
        }
    }

    @Nullable
    public ColorStateList getCloseIconTint() {
        return this.closeIconTint;
    }

    public void setCloseIconTintResource(@ColorRes int id) {
        setCloseIconTint(AppCompatResources.getColorStateList(this.context, id));
    }

    public void setCloseIconTint(@Nullable ColorStateList closeIconTint) {
        if (this.closeIconTint != closeIconTint) {
            this.closeIconTint = closeIconTint;
            if (showsCloseIcon()) {
                DrawableCompat.setTintList(this.closeIcon, closeIconTint);
            }
            onStateChange(getState());
        }
    }

    public float getCloseIconSize() {
        return this.closeIconSize;
    }

    public void setCloseIconSizeResource(@DimenRes int id) {
        setCloseIconSize(this.context.getResources().getDimension(id));
    }

    public void setCloseIconSize(float closeIconSize) {
        if (this.closeIconSize != closeIconSize) {
            this.closeIconSize = closeIconSize;
            invalidateSelf();
            if (showsCloseIcon()) {
                onSizeChange();
            }
        }
    }

    public boolean isCheckable() {
        return this.checkable;
    }

    public void setCheckableResource(@BoolRes int id) {
        setCheckable(this.context.getResources().getBoolean(id));
    }

    public void setCheckable(boolean checkable) {
        if (this.checkable != checkable) {
            this.checkable = checkable;
            float oldChipIconWidth = calculateChipIconWidth();
            if (!checkable && this.currentChecked) {
                this.currentChecked = false;
            }
            float newChipIconWidth = calculateChipIconWidth();
            invalidateSelf();
            if (oldChipIconWidth != newChipIconWidth) {
                onSizeChange();
            }
        }
    }

    public boolean isCheckedIconEnabled() {
        return this.checkedIconEnabled;
    }

    public void setCheckedIconEnabledResource(@BoolRes int id) {
        setCheckedIconEnabled(this.context.getResources().getBoolean(id));
    }

    public void setCheckedIconEnabled(boolean checkedIconEnabled) {
        if (this.checkedIconEnabled != checkedIconEnabled) {
            boolean oldShowsCheckedIcon = showsCheckedIcon();
            this.checkedIconEnabled = checkedIconEnabled;
            boolean newShowsCheckedIcon = showsCheckedIcon();
            boolean changed = oldShowsCheckedIcon != newShowsCheckedIcon;
            if (changed) {
                if (newShowsCheckedIcon) {
                    applyChildDrawable(this.checkedIcon);
                } else {
                    unapplyChildDrawable(this.checkedIcon);
                }
                invalidateSelf();
                onSizeChange();
            }
        }
    }

    @Nullable
    public Drawable getCheckedIcon() {
        return this.checkedIcon;
    }

    public void setCheckedIconResource(@DrawableRes int id) {
        setCheckedIcon(AppCompatResources.getDrawable(this.context, id));
    }

    public void setCheckedIcon(@Nullable Drawable checkedIcon) {
        Drawable oldCheckedIcon = this.checkedIcon;
        if (oldCheckedIcon != checkedIcon) {
            float oldChipIconWidth = calculateChipIconWidth();
            this.checkedIcon = checkedIcon;
            float newChipIconWidth = calculateChipIconWidth();
            unapplyChildDrawable(this.checkedIcon);
            applyChildDrawable(this.checkedIcon);
            invalidateSelf();
            if (oldChipIconWidth != newChipIconWidth) {
                onSizeChange();
            }
        }
    }

    @Nullable
    public MotionSpec getShowMotionSpec() {
        return this.showMotionSpec;
    }

    public void setShowMotionSpecResource(@AnimatorRes int id) {
        setShowMotionSpec(MotionSpec.createFromResource(this.context, id));
    }

    public void setShowMotionSpec(@Nullable MotionSpec showMotionSpec) {
        this.showMotionSpec = showMotionSpec;
    }

    @Nullable
    public MotionSpec getHideMotionSpec() {
        return this.hideMotionSpec;
    }

    public void setHideMotionSpecResource(@AnimatorRes int id) {
        setHideMotionSpec(MotionSpec.createFromResource(this.context, id));
    }

    public void setHideMotionSpec(@Nullable MotionSpec hideMotionSpec) {
        this.hideMotionSpec = hideMotionSpec;
    }

    public float getChipStartPadding() {
        return this.chipStartPadding;
    }

    public void setChipStartPaddingResource(@DimenRes int id) {
        setChipStartPadding(this.context.getResources().getDimension(id));
    }

    public void setChipStartPadding(float chipStartPadding) {
        if (this.chipStartPadding != chipStartPadding) {
            this.chipStartPadding = chipStartPadding;
            invalidateSelf();
            onSizeChange();
        }
    }

    public float getIconStartPadding() {
        return this.iconStartPadding;
    }

    public void setIconStartPaddingResource(@DimenRes int id) {
        setIconStartPadding(this.context.getResources().getDimension(id));
    }

    public void setIconStartPadding(float iconStartPadding) {
        if (this.iconStartPadding != iconStartPadding) {
            float oldChipIconWidth = calculateChipIconWidth();
            this.iconStartPadding = iconStartPadding;
            float newChipIconWidth = calculateChipIconWidth();
            invalidateSelf();
            if (oldChipIconWidth != newChipIconWidth) {
                onSizeChange();
            }
        }
    }

    public float getIconEndPadding() {
        return this.iconEndPadding;
    }

    public void setIconEndPaddingResource(@DimenRes int id) {
        setIconEndPadding(this.context.getResources().getDimension(id));
    }

    public void setIconEndPadding(float iconEndPadding) {
        if (this.iconEndPadding != iconEndPadding) {
            float oldChipIconWidth = calculateChipIconWidth();
            this.iconEndPadding = iconEndPadding;
            float newChipIconWidth = calculateChipIconWidth();
            invalidateSelf();
            if (oldChipIconWidth != newChipIconWidth) {
                onSizeChange();
            }
        }
    }

    public float getTextStartPadding() {
        return this.textStartPadding;
    }

    public void setTextStartPaddingResource(@DimenRes int id) {
        setTextStartPadding(this.context.getResources().getDimension(id));
    }

    public void setTextStartPadding(float textStartPadding) {
        if (this.textStartPadding != textStartPadding) {
            this.textStartPadding = textStartPadding;
            invalidateSelf();
            onSizeChange();
        }
    }

    public float getTextEndPadding() {
        return this.textEndPadding;
    }

    public void setTextEndPaddingResource(@DimenRes int id) {
        setTextEndPadding(this.context.getResources().getDimension(id));
    }

    public void setTextEndPadding(float textEndPadding) {
        if (this.textEndPadding != textEndPadding) {
            this.textEndPadding = textEndPadding;
            invalidateSelf();
            onSizeChange();
        }
    }

    public float getCloseIconStartPadding() {
        return this.closeIconStartPadding;
    }

    public void setCloseIconStartPaddingResource(@DimenRes int id) {
        setCloseIconStartPadding(this.context.getResources().getDimension(id));
    }

    public void setCloseIconStartPadding(float closeIconStartPadding) {
        if (this.closeIconStartPadding != closeIconStartPadding) {
            this.closeIconStartPadding = closeIconStartPadding;
            invalidateSelf();
            if (showsCloseIcon()) {
                onSizeChange();
            }
        }
    }

    public float getCloseIconEndPadding() {
        return this.closeIconEndPadding;
    }

    public void setCloseIconEndPaddingResource(@DimenRes int id) {
        setCloseIconEndPadding(this.context.getResources().getDimension(id));
    }

    public void setCloseIconEndPadding(float closeIconEndPadding) {
        if (this.closeIconEndPadding != closeIconEndPadding) {
            this.closeIconEndPadding = closeIconEndPadding;
            invalidateSelf();
            if (showsCloseIcon()) {
                onSizeChange();
            }
        }
    }

    public float getChipEndPadding() {
        return this.chipEndPadding;
    }

    public void setChipEndPaddingResource(@DimenRes int id) {
        setChipEndPadding(this.context.getResources().getDimension(id));
    }

    public void setChipEndPadding(float chipEndPadding) {
        if (this.chipEndPadding != chipEndPadding) {
            this.chipEndPadding = chipEndPadding;
            invalidateSelf();
            onSizeChange();
        }
    }
}
