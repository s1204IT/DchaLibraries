package android.support.design.widget;

import android.animation.TimeInterpolator;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.os.Build;
import android.support.design.animation.AnimationUtils;
import android.support.v4.text.TextDirectionHeuristicsCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.view.ViewCompat;
import android.support.v7.widget.TintTypedArray;
import android.text.TextPaint;
import android.text.TextUtils;
import android.view.View;
import com.android.contacts.ContactPhotoManager;

final class CollapsingTextHelper {
    private static final Paint DEBUG_DRAW_PAINT;
    private static final boolean USE_SCALING_TEXTURE;
    private boolean boundsChanged;
    private float collapsedDrawX;
    private float collapsedDrawY;
    private int collapsedShadowColor;
    private float collapsedShadowDx;
    private float collapsedShadowDy;
    private float collapsedShadowRadius;
    private ColorStateList collapsedTextColor;
    private Typeface collapsedTypeface;
    private float currentDrawX;
    private float currentDrawY;
    private float currentTextSize;
    private Typeface currentTypeface;
    private boolean drawTitle;
    private float expandedDrawX;
    private float expandedDrawY;
    private float expandedFraction;
    private int expandedShadowColor;
    private float expandedShadowDx;
    private float expandedShadowDy;
    private float expandedShadowRadius;
    private ColorStateList expandedTextColor;
    private Bitmap expandedTitleTexture;
    private Typeface expandedTypeface;
    private boolean isRtl;
    private TimeInterpolator positionInterpolator;
    private float scale;
    private int[] state;
    private CharSequence text;
    private TimeInterpolator textSizeInterpolator;
    private CharSequence textToDraw;
    private float textureAscent;
    private float textureDescent;
    private Paint texturePaint;
    private boolean useTexture;
    private final View view;
    private int expandedTextGravity = 16;
    private int collapsedTextGravity = 16;
    private float expandedTextSize = 15.0f;
    private float collapsedTextSize = 15.0f;
    private final TextPaint textPaint = new TextPaint(129);
    private final TextPaint tmpPaint = new TextPaint(this.textPaint);
    private final Rect collapsedBounds = new Rect();
    private final Rect expandedBounds = new Rect();
    private final RectF currentBounds = new RectF();

    static {
        USE_SCALING_TEXTURE = Build.VERSION.SDK_INT < 18;
        DEBUG_DRAW_PAINT = null;
        if (DEBUG_DRAW_PAINT != null) {
            DEBUG_DRAW_PAINT.setAntiAlias(true);
            DEBUG_DRAW_PAINT.setColor(-65281);
        }
    }

    public CollapsingTextHelper(View view) {
        this.view = view;
    }

    void setTextSizeInterpolator(TimeInterpolator interpolator) {
        this.textSizeInterpolator = interpolator;
        recalculate();
    }

    void setPositionInterpolator(TimeInterpolator interpolator) {
        this.positionInterpolator = interpolator;
        recalculate();
    }

    void setExpandedTextSize(float textSize) {
        if (this.expandedTextSize != textSize) {
            this.expandedTextSize = textSize;
            recalculate();
        }
    }

    void setCollapsedTextColor(ColorStateList textColor) {
        if (this.collapsedTextColor != textColor) {
            this.collapsedTextColor = textColor;
            recalculate();
        }
    }

    void setExpandedTextColor(ColorStateList textColor) {
        if (this.expandedTextColor != textColor) {
            this.expandedTextColor = textColor;
            recalculate();
        }
    }

    void setExpandedBounds(int left, int top, int right, int bottom) {
        if (!rectEquals(this.expandedBounds, left, top, right, bottom)) {
            this.expandedBounds.set(left, top, right, bottom);
            this.boundsChanged = true;
            onBoundsChanged();
        }
    }

    void setCollapsedBounds(int left, int top, int right, int bottom) {
        if (!rectEquals(this.collapsedBounds, left, top, right, bottom)) {
            this.collapsedBounds.set(left, top, right, bottom);
            this.boundsChanged = true;
            onBoundsChanged();
        }
    }

    float calculateCollapsedTextWidth() {
        if (this.text == null) {
            return ContactPhotoManager.OFFSET_DEFAULT;
        }
        getTextPaintCollapsed(this.tmpPaint);
        return this.tmpPaint.measureText(this.text, 0, this.text.length());
    }

    float getCollapsedTextHeight() {
        getTextPaintCollapsed(this.tmpPaint);
        return -this.tmpPaint.ascent();
    }

    void getCollapsedTextActualBounds(RectF bounds) {
        boolean isRtl = calculateIsRtl(this.text);
        bounds.left = !isRtl ? this.collapsedBounds.left : this.collapsedBounds.right - calculateCollapsedTextWidth();
        bounds.top = this.collapsedBounds.top;
        bounds.right = !isRtl ? bounds.left + calculateCollapsedTextWidth() : this.collapsedBounds.right;
        bounds.bottom = this.collapsedBounds.top + getCollapsedTextHeight();
    }

    private void getTextPaintCollapsed(TextPaint textPaint) {
        textPaint.setTextSize(this.collapsedTextSize);
        textPaint.setTypeface(this.collapsedTypeface);
    }

    void onBoundsChanged() {
        this.drawTitle = this.collapsedBounds.width() > 0 && this.collapsedBounds.height() > 0 && this.expandedBounds.width() > 0 && this.expandedBounds.height() > 0;
    }

    void setExpandedTextGravity(int gravity) {
        if (this.expandedTextGravity != gravity) {
            this.expandedTextGravity = gravity;
            recalculate();
        }
    }

    void setCollapsedTextGravity(int gravity) {
        if (this.collapsedTextGravity != gravity) {
            this.collapsedTextGravity = gravity;
            recalculate();
        }
    }

    void setCollapsedTextAppearance(int resId) {
        TintTypedArray a = TintTypedArray.obtainStyledAttributes(this.view.getContext(), resId, android.support.v7.appcompat.R.styleable.TextAppearance);
        if (a.hasValue(android.support.v7.appcompat.R.styleable.TextAppearance_android_textColor)) {
            this.collapsedTextColor = a.getColorStateList(android.support.v7.appcompat.R.styleable.TextAppearance_android_textColor);
        }
        if (a.hasValue(android.support.v7.appcompat.R.styleable.TextAppearance_android_textSize)) {
            this.collapsedTextSize = a.getDimensionPixelSize(android.support.v7.appcompat.R.styleable.TextAppearance_android_textSize, (int) this.collapsedTextSize);
        }
        this.collapsedShadowColor = a.getInt(android.support.v7.appcompat.R.styleable.TextAppearance_android_shadowColor, 0);
        this.collapsedShadowDx = a.getFloat(android.support.v7.appcompat.R.styleable.TextAppearance_android_shadowDx, ContactPhotoManager.OFFSET_DEFAULT);
        this.collapsedShadowDy = a.getFloat(android.support.v7.appcompat.R.styleable.TextAppearance_android_shadowDy, ContactPhotoManager.OFFSET_DEFAULT);
        this.collapsedShadowRadius = a.getFloat(android.support.v7.appcompat.R.styleable.TextAppearance_android_shadowRadius, ContactPhotoManager.OFFSET_DEFAULT);
        a.recycle();
        if (Build.VERSION.SDK_INT >= 16) {
            this.collapsedTypeface = readFontFamilyTypeface(resId);
        }
        recalculate();
    }

    private Typeface readFontFamilyTypeface(int resId) {
        TypedArray a = this.view.getContext().obtainStyledAttributes(resId, new int[]{android.R.attr.fontFamily});
        try {
            String family = a.getString(0);
            if (family != null) {
                return Typeface.create(family, 0);
            }
            a.recycle();
            return null;
        } finally {
            a.recycle();
        }
    }

    void setTypefaces(Typeface typeface) {
        this.expandedTypeface = typeface;
        this.collapsedTypeface = typeface;
        recalculate();
    }

    void setExpansionFraction(float fraction) {
        float fraction2 = MathUtils.constrain(fraction, ContactPhotoManager.OFFSET_DEFAULT, 1.0f);
        if (fraction2 != this.expandedFraction) {
            this.expandedFraction = fraction2;
            calculateCurrentOffsets();
        }
    }

    final boolean setState(int[] state) {
        this.state = state;
        if (isStateful()) {
            recalculate();
            return true;
        }
        return false;
    }

    final boolean isStateful() {
        return (this.collapsedTextColor != null && this.collapsedTextColor.isStateful()) || (this.expandedTextColor != null && this.expandedTextColor.isStateful());
    }

    float getExpansionFraction() {
        return this.expandedFraction;
    }

    private void calculateCurrentOffsets() {
        calculateOffsets(this.expandedFraction);
    }

    private void calculateOffsets(float fraction) {
        interpolateBounds(fraction);
        this.currentDrawX = lerp(this.expandedDrawX, this.collapsedDrawX, fraction, this.positionInterpolator);
        this.currentDrawY = lerp(this.expandedDrawY, this.collapsedDrawY, fraction, this.positionInterpolator);
        setInterpolatedTextSize(lerp(this.expandedTextSize, this.collapsedTextSize, fraction, this.textSizeInterpolator));
        if (this.collapsedTextColor != this.expandedTextColor) {
            this.textPaint.setColor(blendColors(getCurrentExpandedTextColor(), getCurrentCollapsedTextColor(), fraction));
        } else {
            this.textPaint.setColor(getCurrentCollapsedTextColor());
        }
        this.textPaint.setShadowLayer(lerp(this.expandedShadowRadius, this.collapsedShadowRadius, fraction, null), lerp(this.expandedShadowDx, this.collapsedShadowDx, fraction, null), lerp(this.expandedShadowDy, this.collapsedShadowDy, fraction, null), blendColors(this.expandedShadowColor, this.collapsedShadowColor, fraction));
        ViewCompat.postInvalidateOnAnimation(this.view);
    }

    private int getCurrentExpandedTextColor() {
        if (this.state != null) {
            return this.expandedTextColor.getColorForState(this.state, 0);
        }
        return this.expandedTextColor.getDefaultColor();
    }

    int getCurrentCollapsedTextColor() {
        if (this.state != null) {
            return this.collapsedTextColor.getColorForState(this.state, 0);
        }
        return this.collapsedTextColor.getDefaultColor();
    }

    private void calculateBaseOffsets() {
        float f = this.currentTextSize;
        calculateUsingTextSize(this.collapsedTextSize);
        CharSequence charSequence = this.textToDraw;
        float fMeasureText = ContactPhotoManager.OFFSET_DEFAULT;
        float fMeasureText2 = charSequence != null ? this.textPaint.measureText(this.textToDraw, 0, this.textToDraw.length()) : 0.0f;
        int absoluteGravity = GravityCompat.getAbsoluteGravity(this.collapsedTextGravity, this.isRtl ? 1 : 0);
        int i = absoluteGravity & 112;
        if (i == 48) {
            this.collapsedDrawY = this.collapsedBounds.top - this.textPaint.ascent();
        } else if (i == 80) {
            this.collapsedDrawY = this.collapsedBounds.bottom;
        } else {
            this.collapsedDrawY = this.collapsedBounds.centerY() + (((this.textPaint.descent() - this.textPaint.ascent()) / 2.0f) - this.textPaint.descent());
        }
        int i2 = absoluteGravity & 8388615;
        if (i2 == 1) {
            this.collapsedDrawX = this.collapsedBounds.centerX() - (fMeasureText2 / 2.0f);
        } else if (i2 == 5) {
            this.collapsedDrawX = this.collapsedBounds.right - fMeasureText2;
        } else {
            this.collapsedDrawX = this.collapsedBounds.left;
        }
        calculateUsingTextSize(this.expandedTextSize);
        if (this.textToDraw != null) {
            fMeasureText = this.textPaint.measureText(this.textToDraw, 0, this.textToDraw.length());
        }
        float f2 = fMeasureText;
        int absoluteGravity2 = GravityCompat.getAbsoluteGravity(this.expandedTextGravity, this.isRtl ? 1 : 0);
        int i3 = absoluteGravity2 & 112;
        if (i3 == 48) {
            this.expandedDrawY = this.expandedBounds.top - this.textPaint.ascent();
        } else if (i3 == 80) {
            this.expandedDrawY = this.expandedBounds.bottom;
        } else {
            this.expandedDrawY = this.expandedBounds.centerY() + (((this.textPaint.descent() - this.textPaint.ascent()) / 2.0f) - this.textPaint.descent());
        }
        int i4 = absoluteGravity2 & 8388615;
        if (i4 == 1) {
            this.expandedDrawX = this.expandedBounds.centerX() - (f2 / 2.0f);
        } else if (i4 == 5) {
            this.expandedDrawX = this.expandedBounds.right - f2;
        } else {
            this.expandedDrawX = this.expandedBounds.left;
        }
        clearTexture();
        setInterpolatedTextSize(f);
    }

    private void interpolateBounds(float fraction) {
        this.currentBounds.left = lerp(this.expandedBounds.left, this.collapsedBounds.left, fraction, this.positionInterpolator);
        this.currentBounds.top = lerp(this.expandedDrawY, this.collapsedDrawY, fraction, this.positionInterpolator);
        this.currentBounds.right = lerp(this.expandedBounds.right, this.collapsedBounds.right, fraction, this.positionInterpolator);
        this.currentBounds.bottom = lerp(this.expandedBounds.bottom, this.collapsedBounds.bottom, fraction, this.positionInterpolator);
    }

    public void draw(Canvas canvas) {
        float ascent;
        float descent;
        int saveCount = canvas.save();
        if (this.textToDraw != null && this.drawTitle) {
            float x = this.currentDrawX;
            float y = this.currentDrawY;
            boolean drawTexture = this.useTexture && this.expandedTitleTexture != null;
            if (drawTexture) {
                ascent = this.textureAscent * this.scale;
                descent = this.textureDescent * this.scale;
            } else {
                ascent = this.textPaint.ascent() * this.scale;
                descent = this.textPaint.descent() * this.scale;
            }
            float ascent2 = ascent;
            if (drawTexture) {
                y += ascent2;
            }
            float y2 = y;
            if (this.scale != 1.0f) {
                canvas.scale(this.scale, this.scale, x, y2);
            }
            if (drawTexture) {
                canvas.drawBitmap(this.expandedTitleTexture, x, y2, this.texturePaint);
            } else {
                canvas.drawText(this.textToDraw, 0, this.textToDraw.length(), x, y2, this.textPaint);
            }
        }
        canvas.restoreToCount(saveCount);
    }

    private boolean calculateIsRtl(CharSequence text) {
        boolean defaultIsRtl = ViewCompat.getLayoutDirection(this.view) == 1;
        return (defaultIsRtl ? TextDirectionHeuristicsCompat.FIRSTSTRONG_RTL : TextDirectionHeuristicsCompat.FIRSTSTRONG_LTR).isRtl(text, 0, text.length());
    }

    private void setInterpolatedTextSize(float textSize) {
        calculateUsingTextSize(textSize);
        this.useTexture = USE_SCALING_TEXTURE && this.scale != 1.0f;
        if (this.useTexture) {
            ensureExpandedTexture();
        }
        ViewCompat.postInvalidateOnAnimation(this.view);
    }

    private void calculateUsingTextSize(float textSize) {
        float newTextSize;
        float availableWidth;
        if (this.text == null) {
            return;
        }
        float collapsedWidth = this.collapsedBounds.width();
        float expandedWidth = this.expandedBounds.width();
        boolean updateDrawText = false;
        if (isClose(textSize, this.collapsedTextSize)) {
            newTextSize = this.collapsedTextSize;
            this.scale = 1.0f;
            if (this.currentTypeface != this.collapsedTypeface) {
                this.currentTypeface = this.collapsedTypeface;
                updateDrawText = true;
            }
            availableWidth = collapsedWidth;
        } else {
            newTextSize = this.expandedTextSize;
            if (this.currentTypeface != this.expandedTypeface) {
                this.currentTypeface = this.expandedTypeface;
                updateDrawText = true;
            }
            if (isClose(textSize, this.expandedTextSize)) {
                this.scale = 1.0f;
            } else {
                this.scale = textSize / this.expandedTextSize;
            }
            float textSizeRatio = this.collapsedTextSize / this.expandedTextSize;
            float scaledDownWidth = expandedWidth * textSizeRatio;
            if (scaledDownWidth > collapsedWidth) {
                float availableWidth2 = Math.min(collapsedWidth / textSizeRatio, expandedWidth);
                availableWidth = availableWidth2;
            } else {
                availableWidth = expandedWidth;
            }
        }
        if (availableWidth > ContactPhotoManager.OFFSET_DEFAULT) {
            updateDrawText = this.currentTextSize != newTextSize || this.boundsChanged || updateDrawText;
            this.currentTextSize = newTextSize;
            this.boundsChanged = false;
        }
        if (this.textToDraw == null || updateDrawText) {
            this.textPaint.setTextSize(this.currentTextSize);
            this.textPaint.setTypeface(this.currentTypeface);
            this.textPaint.setLinearText(this.scale != 1.0f);
            CharSequence title = TextUtils.ellipsize(this.text, this.textPaint, availableWidth, TextUtils.TruncateAt.END);
            if (!TextUtils.equals(title, this.textToDraw)) {
                this.textToDraw = title;
                this.isRtl = calculateIsRtl(this.textToDraw);
            }
        }
    }

    private void ensureExpandedTexture() {
        if (this.expandedTitleTexture != null || this.expandedBounds.isEmpty() || TextUtils.isEmpty(this.textToDraw)) {
            return;
        }
        calculateOffsets(ContactPhotoManager.OFFSET_DEFAULT);
        this.textureAscent = this.textPaint.ascent();
        this.textureDescent = this.textPaint.descent();
        int w = Math.round(this.textPaint.measureText(this.textToDraw, 0, this.textToDraw.length()));
        int h = Math.round(this.textureDescent - this.textureAscent);
        if (w <= 0 || h <= 0) {
            return;
        }
        this.expandedTitleTexture = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(this.expandedTitleTexture);
        c.drawText(this.textToDraw, 0, this.textToDraw.length(), ContactPhotoManager.OFFSET_DEFAULT, h - this.textPaint.descent(), this.textPaint);
        if (this.texturePaint == null) {
            this.texturePaint = new Paint(3);
        }
    }

    public void recalculate() {
        if (this.view.getHeight() > 0 && this.view.getWidth() > 0) {
            calculateBaseOffsets();
            calculateCurrentOffsets();
        }
    }

    void setText(CharSequence text) {
        if (text == null || !text.equals(this.text)) {
            this.text = text;
            this.textToDraw = null;
            clearTexture();
            recalculate();
        }
    }

    CharSequence getText() {
        return this.text;
    }

    private void clearTexture() {
        if (this.expandedTitleTexture != null) {
            this.expandedTitleTexture.recycle();
            this.expandedTitleTexture = null;
        }
    }

    private static boolean isClose(float value, float targetValue) {
        return Math.abs(value - targetValue) < 0.001f;
    }

    ColorStateList getCollapsedTextColor() {
        return this.collapsedTextColor;
    }

    private static int blendColors(int color1, int color2, float ratio) {
        float inverseRatio = 1.0f - ratio;
        float a = (Color.alpha(color1) * inverseRatio) + (Color.alpha(color2) * ratio);
        float r = (Color.red(color1) * inverseRatio) + (Color.red(color2) * ratio);
        float g = (Color.green(color1) * inverseRatio) + (Color.green(color2) * ratio);
        float b = (Color.blue(color1) * inverseRatio) + (Color.blue(color2) * ratio);
        return Color.argb((int) a, (int) r, (int) g, (int) b);
    }

    private static float lerp(float startValue, float endValue, float fraction, TimeInterpolator interpolator) {
        if (interpolator != null) {
            fraction = interpolator.getInterpolation(fraction);
        }
        return AnimationUtils.lerp(startValue, endValue, fraction);
    }

    private static boolean rectEquals(Rect r, int left, int top, int right, int bottom) {
        return r.left == left && r.top == top && r.right == right && r.bottom == bottom;
    }
}
