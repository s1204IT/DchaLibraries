package android.graphics.drawable;

import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.DashPathEffect;
import android.graphics.LinearGradient;
import android.graphics.Outline;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.RadialGradient;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.SweepGradient;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import com.android.internal.R;
import java.io.IOException;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

public class GradientDrawable extends Drawable {
    private static final float DEFAULT_INNER_RADIUS_RATIO = 3.0f;
    private static final float DEFAULT_THICKNESS_RATIO = 9.0f;
    public static final int LINE = 2;
    public static final int LINEAR_GRADIENT = 0;
    public static final int OVAL = 1;
    public static final int RADIAL_GRADIENT = 1;
    private static final int RADIUS_TYPE_FRACTION = 1;
    private static final int RADIUS_TYPE_FRACTION_PARENT = 2;
    private static final int RADIUS_TYPE_PIXELS = 0;
    public static final int RECTANGLE = 0;
    public static final int RING = 3;
    public static final int SWEEP_GRADIENT = 2;
    private int mAlpha;
    private ColorFilter mColorFilter;
    private final Paint mFillPaint;
    private boolean mGradientIsDirty;
    private float mGradientRadius;
    private GradientState mGradientState;
    private Paint mLayerPaint;
    private boolean mMutated;
    private Rect mPadding;
    private final Path mPath;
    private boolean mPathIsDirty;
    private final RectF mRect;
    private Path mRingPath;
    private Paint mStrokePaint;
    private PorterDuffColorFilter mTintFilter;

    public enum Orientation {
        TOP_BOTTOM,
        TR_BL,
        RIGHT_LEFT,
        BR_TL,
        BOTTOM_TOP,
        BL_TR,
        LEFT_RIGHT,
        TL_BR
    }

    public GradientDrawable() {
        this(new GradientState(Orientation.TOP_BOTTOM, null));
    }

    public GradientDrawable(Orientation orientation, int[] colors) {
        this(new GradientState(orientation, colors));
    }

    @Override
    public boolean getPadding(Rect padding) {
        if (this.mPadding == null) {
            return super.getPadding(padding);
        }
        padding.set(this.mPadding);
        return true;
    }

    public void setCornerRadii(float[] radii) {
        this.mGradientState.setCornerRadii(radii);
        this.mPathIsDirty = true;
        invalidateSelf();
    }

    public void setCornerRadius(float radius) {
        this.mGradientState.setCornerRadius(radius);
        this.mPathIsDirty = true;
        invalidateSelf();
    }

    public void setStroke(int width, int color) {
        setStroke(width, color, 0.0f, 0.0f);
    }

    public void setStroke(int width, ColorStateList colorStateList) {
        setStroke(width, colorStateList, 0.0f, 0.0f);
    }

    public void setStroke(int width, int color, float dashWidth, float dashGap) {
        this.mGradientState.setStroke(width, ColorStateList.valueOf(color), dashWidth, dashGap);
        setStrokeInternal(width, color, dashWidth, dashGap);
    }

    public void setStroke(int width, ColorStateList colorStateList, float dashWidth, float dashGap) {
        int color;
        this.mGradientState.setStroke(width, colorStateList, dashWidth, dashGap);
        if (colorStateList == null) {
            color = 0;
        } else {
            int[] stateSet = getState();
            color = colorStateList.getColorForState(stateSet, 0);
        }
        setStrokeInternal(width, color, dashWidth, dashGap);
    }

    private void setStrokeInternal(int width, int color, float dashWidth, float dashGap) {
        if (this.mStrokePaint == null) {
            this.mStrokePaint = new Paint(1);
            this.mStrokePaint.setStyle(Paint.Style.STROKE);
        }
        this.mStrokePaint.setStrokeWidth(width);
        this.mStrokePaint.setColor(color);
        DashPathEffect e = null;
        if (dashWidth > 0.0f) {
            e = new DashPathEffect(new float[]{dashWidth, dashGap}, 0.0f);
        }
        this.mStrokePaint.setPathEffect(e);
        invalidateSelf();
    }

    public void setSize(int width, int height) {
        this.mGradientState.setSize(width, height);
        this.mPathIsDirty = true;
        invalidateSelf();
    }

    public void setShape(int shape) {
        this.mRingPath = null;
        this.mPathIsDirty = true;
        this.mGradientState.setShape(shape);
        invalidateSelf();
    }

    public void setGradientType(int gradient) {
        this.mGradientState.setGradientType(gradient);
        this.mGradientIsDirty = true;
        invalidateSelf();
    }

    public void setGradientCenter(float x, float y) {
        this.mGradientState.setGradientCenter(x, y);
        this.mGradientIsDirty = true;
        invalidateSelf();
    }

    public void setGradientRadius(float gradientRadius) {
        this.mGradientState.setGradientRadius(gradientRadius, 0);
        this.mGradientIsDirty = true;
        invalidateSelf();
    }

    public float getGradientRadius() {
        if (this.mGradientState.mGradient != 1) {
            return 0.0f;
        }
        ensureValidRect();
        return this.mGradientRadius;
    }

    public void setUseLevel(boolean useLevel) {
        this.mGradientState.mUseLevel = useLevel;
        this.mGradientIsDirty = true;
        invalidateSelf();
    }

    private int modulateAlpha(int alpha) {
        int scale = this.mAlpha + (this.mAlpha >> 7);
        return (alpha * scale) >> 8;
    }

    public Orientation getOrientation() {
        return this.mGradientState.mOrientation;
    }

    public void setOrientation(Orientation orientation) {
        this.mGradientState.mOrientation = orientation;
        this.mGradientIsDirty = true;
        invalidateSelf();
    }

    public void setColors(int[] colors) {
        this.mGradientState.setColors(colors);
        this.mGradientIsDirty = true;
        invalidateSelf();
    }

    @Override
    public void draw(Canvas canvas) {
        if (ensureValidRect()) {
            int prevFillAlpha = this.mFillPaint.getAlpha();
            int prevStrokeAlpha = this.mStrokePaint != null ? this.mStrokePaint.getAlpha() : 0;
            int currFillAlpha = modulateAlpha(prevFillAlpha);
            int currStrokeAlpha = modulateAlpha(prevStrokeAlpha);
            boolean haveStroke = currStrokeAlpha > 0 && this.mStrokePaint != null && this.mStrokePaint.getStrokeWidth() > 0.0f;
            boolean haveFill = currFillAlpha > 0;
            GradientState st = this.mGradientState;
            ColorFilter colorFilter = this.mColorFilter != null ? this.mColorFilter : this.mTintFilter;
            boolean useLayer = haveStroke && haveFill && st.mShape != 2 && currStrokeAlpha < 255 && (this.mAlpha < 255 || colorFilter != null);
            if (useLayer) {
                if (this.mLayerPaint == null) {
                    this.mLayerPaint = new Paint();
                }
                this.mLayerPaint.setDither(st.mDither);
                this.mLayerPaint.setAlpha(this.mAlpha);
                this.mLayerPaint.setColorFilter(colorFilter);
                float rad = this.mStrokePaint.getStrokeWidth();
                canvas.saveLayer(this.mRect.left - rad, this.mRect.top - rad, this.mRect.right + rad, this.mRect.bottom + rad, this.mLayerPaint, 4);
                this.mFillPaint.setColorFilter(null);
                this.mStrokePaint.setColorFilter(null);
            } else {
                this.mFillPaint.setAlpha(currFillAlpha);
                this.mFillPaint.setDither(st.mDither);
                this.mFillPaint.setColorFilter(colorFilter);
                if (colorFilter != null && st.mColorStateList == null) {
                    this.mFillPaint.setColor(this.mAlpha << 24);
                }
                if (haveStroke) {
                    this.mStrokePaint.setAlpha(currStrokeAlpha);
                    this.mStrokePaint.setDither(st.mDither);
                    this.mStrokePaint.setColorFilter(colorFilter);
                }
            }
            switch (st.mShape) {
                case 0:
                    if (st.mRadiusArray != null) {
                        buildPathIfDirty();
                        canvas.drawPath(this.mPath, this.mFillPaint);
                        if (haveStroke) {
                            canvas.drawPath(this.mPath, this.mStrokePaint);
                        }
                    } else if (st.mRadius > 0.0f) {
                        float rad2 = Math.min(st.mRadius, Math.min(this.mRect.width(), this.mRect.height()) * 0.5f);
                        canvas.drawRoundRect(this.mRect, rad2, rad2, this.mFillPaint);
                        if (haveStroke) {
                            canvas.drawRoundRect(this.mRect, rad2, rad2, this.mStrokePaint);
                        }
                    } else {
                        if (this.mFillPaint.getColor() != 0 || colorFilter != null || this.mFillPaint.getShader() != null) {
                            canvas.drawRect(this.mRect, this.mFillPaint);
                        }
                        if (haveStroke) {
                            canvas.drawRect(this.mRect, this.mStrokePaint);
                        }
                    }
                    break;
                case 1:
                    canvas.drawOval(this.mRect, this.mFillPaint);
                    if (haveStroke) {
                        canvas.drawOval(this.mRect, this.mStrokePaint);
                    }
                    break;
                case 2:
                    RectF r = this.mRect;
                    float y = r.centerY();
                    if (haveStroke) {
                        canvas.drawLine(r.left, y, r.right, y, this.mStrokePaint);
                    }
                    break;
                case 3:
                    Path path = buildRing(st);
                    canvas.drawPath(path, this.mFillPaint);
                    if (haveStroke) {
                        canvas.drawPath(path, this.mStrokePaint);
                    }
                    break;
            }
            if (useLayer) {
                canvas.restore();
                return;
            }
            this.mFillPaint.setAlpha(prevFillAlpha);
            if (haveStroke) {
                this.mStrokePaint.setAlpha(prevStrokeAlpha);
            }
        }
    }

    private void buildPathIfDirty() {
        GradientState st = this.mGradientState;
        if (this.mPathIsDirty) {
            ensureValidRect();
            this.mPath.reset();
            this.mPath.addRoundRect(this.mRect, st.mRadiusArray, Path.Direction.CW);
            this.mPathIsDirty = false;
        }
    }

    private Path buildRing(GradientState st) {
        if (this.mRingPath != null && (!st.mUseLevelForShape || !this.mPathIsDirty)) {
            return this.mRingPath;
        }
        this.mPathIsDirty = false;
        float sweep = st.mUseLevelForShape ? (getLevel() * 360.0f) / 10000.0f : 360.0f;
        RectF bounds = new RectF(this.mRect);
        float x = bounds.width() / 2.0f;
        float y = bounds.height() / 2.0f;
        float thickness = st.mThickness != -1 ? st.mThickness : bounds.width() / st.mThicknessRatio;
        float radius = st.mInnerRadius != -1 ? st.mInnerRadius : bounds.width() / st.mInnerRadiusRatio;
        RectF innerBounds = new RectF(bounds);
        innerBounds.inset(x - radius, y - radius);
        RectF bounds2 = new RectF(innerBounds);
        bounds2.inset(-thickness, -thickness);
        if (this.mRingPath == null) {
            this.mRingPath = new Path();
        } else {
            this.mRingPath.reset();
        }
        Path ringPath = this.mRingPath;
        if (sweep < 360.0f && sweep > -360.0f) {
            ringPath.setFillType(Path.FillType.EVEN_ODD);
            ringPath.moveTo(x + radius, y);
            ringPath.lineTo(x + radius + thickness, y);
            ringPath.arcTo(bounds2, 0.0f, sweep, false);
            ringPath.arcTo(innerBounds, sweep, -sweep, false);
            ringPath.close();
            return ringPath;
        }
        ringPath.addOval(bounds2, Path.Direction.CW);
        ringPath.addOval(innerBounds, Path.Direction.CCW);
        return ringPath;
    }

    public void setColor(int argb) {
        this.mGradientState.setColorStateList(ColorStateList.valueOf(argb));
        this.mFillPaint.setColor(argb);
        invalidateSelf();
    }

    public void setColor(ColorStateList colorStateList) {
        int color;
        this.mGradientState.setColorStateList(colorStateList);
        if (colorStateList == null) {
            color = 0;
        } else {
            int[] stateSet = getState();
            color = colorStateList.getColorForState(stateSet, 0);
        }
        this.mFillPaint.setColor(color);
        invalidateSelf();
    }

    @Override
    protected boolean onStateChange(int[] stateSet) {
        ColorStateList strokeStateList;
        boolean invalidateSelf = false;
        GradientState s = this.mGradientState;
        ColorStateList stateList = s.mColorStateList;
        if (stateList != null) {
            int newColor = stateList.getColorForState(stateSet, 0);
            int oldColor = this.mFillPaint.getColor();
            if (oldColor != newColor) {
                this.mFillPaint.setColor(newColor);
                invalidateSelf = true;
            }
        }
        Paint strokePaint = this.mStrokePaint;
        if (strokePaint != null && (strokeStateList = s.mStrokeColorStateList) != null) {
            int newStrokeColor = strokeStateList.getColorForState(stateSet, 0);
            int oldStrokeColor = strokePaint.getColor();
            if (oldStrokeColor != newStrokeColor) {
                strokePaint.setColor(newStrokeColor);
                invalidateSelf = true;
            }
        }
        if (s.mTint != null && s.mTintMode != null) {
            this.mTintFilter = updateTintFilter(this.mTintFilter, s.mTint, s.mTintMode);
            invalidateSelf = true;
        }
        if (!invalidateSelf) {
            return false;
        }
        invalidateSelf();
        return true;
    }

    @Override
    public boolean isStateful() {
        GradientState s = this.mGradientState;
        return super.isStateful() || (s.mColorStateList != null && s.mColorStateList.isStateful()) || ((s.mStrokeColorStateList != null && s.mStrokeColorStateList.isStateful()) || (s.mTint != null && s.mTint.isStateful()));
    }

    @Override
    public int getChangingConfigurations() {
        return super.getChangingConfigurations() | this.mGradientState.mChangingConfigurations;
    }

    @Override
    public void setAlpha(int alpha) {
        if (alpha != this.mAlpha) {
            this.mAlpha = alpha;
            invalidateSelf();
        }
    }

    @Override
    public int getAlpha() {
        return this.mAlpha;
    }

    @Override
    public void setDither(boolean dither) {
        if (dither != this.mGradientState.mDither) {
            this.mGradientState.mDither = dither;
            invalidateSelf();
        }
    }

    @Override
    public ColorFilter getColorFilter() {
        return this.mColorFilter;
    }

    @Override
    public void setColorFilter(ColorFilter cf) {
        if (cf != this.mColorFilter) {
            this.mColorFilter = cf;
            invalidateSelf();
        }
    }

    @Override
    public void setTintList(ColorStateList tint) {
        this.mGradientState.mTint = tint;
        this.mTintFilter = updateTintFilter(this.mTintFilter, tint, this.mGradientState.mTintMode);
        invalidateSelf();
    }

    @Override
    public void setTintMode(PorterDuff.Mode tintMode) {
        this.mGradientState.mTintMode = tintMode;
        this.mTintFilter = updateTintFilter(this.mTintFilter, this.mGradientState.mTint, tintMode);
        invalidateSelf();
    }

    @Override
    public int getOpacity() {
        return (this.mAlpha == 255 && this.mGradientState.mOpaqueOverBounds && isOpaqueForState()) ? -1 : -3;
    }

    @Override
    protected void onBoundsChange(Rect r) {
        super.onBoundsChange(r);
        this.mRingPath = null;
        this.mPathIsDirty = true;
        this.mGradientIsDirty = true;
    }

    @Override
    protected boolean onLevelChange(int level) {
        super.onLevelChange(level);
        this.mGradientIsDirty = true;
        this.mPathIsDirty = true;
        invalidateSelf();
        return true;
    }

    private boolean ensureValidRect() {
        float x0;
        float y0;
        float x1;
        float y1;
        if (this.mGradientIsDirty) {
            this.mGradientIsDirty = false;
            Rect bounds = getBounds();
            float inset = 0.0f;
            if (this.mStrokePaint != null) {
                inset = this.mStrokePaint.getStrokeWidth() * 0.5f;
            }
            GradientState st = this.mGradientState;
            this.mRect.set(bounds.left + inset, bounds.top + inset, bounds.right - inset, bounds.bottom - inset);
            int[] colors = st.mColors;
            if (colors != null) {
                RectF r = this.mRect;
                if (st.mGradient == 0) {
                    float level = st.mUseLevel ? getLevel() / 10000.0f : 1.0f;
                    switch (st.mOrientation) {
                        case TOP_BOTTOM:
                            x0 = r.left;
                            y0 = r.top;
                            x1 = x0;
                            y1 = level * r.bottom;
                            break;
                        case TR_BL:
                            x0 = r.right;
                            y0 = r.top;
                            x1 = level * r.left;
                            y1 = level * r.bottom;
                            break;
                        case RIGHT_LEFT:
                            x0 = r.right;
                            y0 = r.top;
                            x1 = level * r.left;
                            y1 = y0;
                            break;
                        case BR_TL:
                            x0 = r.right;
                            y0 = r.bottom;
                            x1 = level * r.left;
                            y1 = level * r.top;
                            break;
                        case BOTTOM_TOP:
                            x0 = r.left;
                            y0 = r.bottom;
                            x1 = x0;
                            y1 = level * r.top;
                            break;
                        case BL_TR:
                            x0 = r.left;
                            y0 = r.bottom;
                            x1 = level * r.right;
                            y1 = level * r.top;
                            break;
                        case LEFT_RIGHT:
                            x0 = r.left;
                            y0 = r.top;
                            x1 = level * r.right;
                            y1 = y0;
                            break;
                        default:
                            x0 = r.left;
                            y0 = r.top;
                            x1 = level * r.right;
                            y1 = level * r.bottom;
                            break;
                    }
                    this.mFillPaint.setShader(new LinearGradient(x0, y0, x1, y1, colors, st.mPositions, Shader.TileMode.CLAMP));
                } else if (st.mGradient == 1) {
                    float x02 = r.left + ((r.right - r.left) * st.mCenterX);
                    float y02 = r.top + ((r.bottom - r.top) * st.mCenterY);
                    float radius = st.mGradientRadius;
                    if (st.mGradientRadiusType == 1) {
                        float width = st.mWidth >= 0 ? st.mWidth : r.width();
                        float height = st.mHeight >= 0 ? st.mHeight : r.height();
                        radius *= Math.min(width, height);
                    } else if (st.mGradientRadiusType == 2) {
                        radius *= Math.min(r.width(), r.height());
                    }
                    if (st.mUseLevel) {
                        radius *= getLevel() / 10000.0f;
                    }
                    this.mGradientRadius = radius;
                    if (radius <= 0.0f) {
                        radius = 0.001f;
                    }
                    this.mFillPaint.setShader(new RadialGradient(x02, y02, radius, colors, (float[]) null, Shader.TileMode.CLAMP));
                } else if (st.mGradient == 2) {
                    float x03 = r.left + ((r.right - r.left) * st.mCenterX);
                    float y03 = r.top + ((r.bottom - r.top) * st.mCenterY);
                    int[] tempColors = colors;
                    float[] tempPositions = null;
                    if (st.mUseLevel) {
                        tempColors = st.mTempColors;
                        int length = colors.length;
                        if (tempColors == null || tempColors.length != length + 1) {
                            tempColors = new int[length + 1];
                            st.mTempColors = tempColors;
                        }
                        System.arraycopy(colors, 0, tempColors, 0, length);
                        tempColors[length] = colors[length - 1];
                        tempPositions = st.mTempPositions;
                        float fraction = 1.0f / (length - 1);
                        if (tempPositions == null || tempPositions.length != length + 1) {
                            tempPositions = new float[length + 1];
                            st.mTempPositions = tempPositions;
                        }
                        float level2 = getLevel() / 10000.0f;
                        for (int i = 0; i < length; i++) {
                            tempPositions[i] = i * fraction * level2;
                        }
                        tempPositions[length] = 1.0f;
                    }
                    this.mFillPaint.setShader(new SweepGradient(x03, y03, tempColors, tempPositions));
                }
                if (st.mColorStateList == null) {
                    this.mFillPaint.setColor(-16777216);
                }
            }
        }
        return !this.mRect.isEmpty();
    }

    @Override
    public void inflate(Resources r, XmlPullParser parser, AttributeSet attrs, Resources.Theme theme) throws XmlPullParserException, IOException {
        TypedArray a = obtainAttributes(r, theme, attrs, R.styleable.GradientDrawable);
        super.inflateWithAttributes(r, parser, a, 2);
        updateStateFromTypedArray(a);
        a.recycle();
        inflateChildElements(r, parser, attrs, theme);
        this.mGradientState.computeOpacity();
    }

    @Override
    public void applyTheme(Resources.Theme t) {
        super.applyTheme(t);
        GradientState state = this.mGradientState;
        if (state != null) {
            if (state.mThemeAttrs != null) {
                TypedArray a = t.resolveAttributes(state.mThemeAttrs, R.styleable.GradientDrawable);
                updateStateFromTypedArray(a);
                a.recycle();
            }
            applyThemeChildElements(t);
            state.computeOpacity();
        }
    }

    private void updateStateFromTypedArray(TypedArray a) {
        GradientState state = this.mGradientState;
        state.mChangingConfigurations |= a.getChangingConfigurations();
        state.mThemeAttrs = a.extractThemeAttrs();
        state.mShape = a.getInt(3, state.mShape);
        state.mDither = a.getBoolean(0, state.mDither);
        if (state.mShape == 3) {
            state.mInnerRadius = a.getDimensionPixelSize(7, state.mInnerRadius);
            if (state.mInnerRadius == -1) {
                state.mInnerRadiusRatio = a.getFloat(4, state.mInnerRadiusRatio);
            }
            state.mThickness = a.getDimensionPixelSize(8, state.mThickness);
            if (state.mThickness == -1) {
                state.mThicknessRatio = a.getFloat(5, state.mThicknessRatio);
            }
            state.mUseLevelForShape = a.getBoolean(6, state.mUseLevelForShape);
        }
        int tintMode = a.getInt(9, -1);
        if (tintMode != -1) {
            state.mTintMode = Drawable.parseTintMode(tintMode, PorterDuff.Mode.SRC_IN);
        }
        ColorStateList tint = a.getColorStateList(1);
        if (tint != null) {
            state.mTint = tint;
        }
        this.mTintFilter = updateTintFilter(this.mTintFilter, state.mTint, state.mTintMode);
    }

    @Override
    public boolean canApplyTheme() {
        return (this.mGradientState != null && this.mGradientState.canApplyTheme()) || super.canApplyTheme();
    }

    private void applyThemeChildElements(Resources.Theme t) {
        TypedArray a;
        GradientState st = this.mGradientState;
        if (st.mAttrSize != null) {
            a = t.resolveAttributes(st.mAttrSize, R.styleable.GradientDrawableSize);
            updateGradientDrawableSize(a);
        }
        if (st.mAttrGradient != null) {
            a = t.resolveAttributes(st.mAttrGradient, R.styleable.GradientDrawableGradient);
            try {
                try {
                    updateGradientDrawableGradient(t.getResources(), a);
                    a.recycle();
                } catch (XmlPullParserException e) {
                    throw new RuntimeException(e);
                }
            } finally {
                a.recycle();
            }
        }
        if (st.mAttrSolid != null) {
            TypedArray a2 = t.resolveAttributes(st.mAttrSolid, R.styleable.GradientDrawableSolid);
            updateGradientDrawableSolid(a2);
            a2.recycle();
        }
        if (st.mAttrStroke != null) {
            TypedArray a3 = t.resolveAttributes(st.mAttrStroke, R.styleable.GradientDrawableStroke);
            updateGradientDrawableStroke(a3);
            a3.recycle();
        }
        if (st.mAttrCorners != null) {
            a = t.resolveAttributes(st.mAttrCorners, R.styleable.DrawableCorners);
            updateDrawableCorners(a);
        }
        if (st.mAttrPadding != null) {
            a = t.resolveAttributes(st.mAttrPadding, R.styleable.GradientDrawablePadding);
            updateGradientDrawablePadding(a);
        }
    }

    private void inflateChildElements(Resources r, XmlPullParser parser, AttributeSet attrs, Resources.Theme theme) throws XmlPullParserException, IOException {
        int innerDepth = parser.getDepth() + 1;
        while (true) {
            int type = parser.next();
            if (type == 1) {
                return;
            }
            int depth = parser.getDepth();
            if (depth >= innerDepth || type != 3) {
                if (type == 2 && depth <= innerDepth) {
                    String name = parser.getName();
                    if (name.equals("size")) {
                        TypedArray a = obtainAttributes(r, theme, attrs, R.styleable.GradientDrawableSize);
                        updateGradientDrawableSize(a);
                        a.recycle();
                    } else if (name.equals("gradient")) {
                        TypedArray a2 = obtainAttributes(r, theme, attrs, R.styleable.GradientDrawableGradient);
                        updateGradientDrawableGradient(r, a2);
                        a2.recycle();
                    } else if (name.equals("solid")) {
                        TypedArray a3 = obtainAttributes(r, theme, attrs, R.styleable.GradientDrawableSolid);
                        updateGradientDrawableSolid(a3);
                        a3.recycle();
                    } else if (name.equals("stroke")) {
                        TypedArray a4 = obtainAttributes(r, theme, attrs, R.styleable.GradientDrawableStroke);
                        updateGradientDrawableStroke(a4);
                        a4.recycle();
                    } else if (name.equals("corners")) {
                        TypedArray a5 = obtainAttributes(r, theme, attrs, R.styleable.DrawableCorners);
                        updateDrawableCorners(a5);
                        a5.recycle();
                    } else if (name.equals("padding")) {
                        TypedArray a6 = obtainAttributes(r, theme, attrs, R.styleable.GradientDrawablePadding);
                        updateGradientDrawablePadding(a6);
                        a6.recycle();
                    } else {
                        Log.w("drawable", "Bad element under <shape>: " + name);
                    }
                }
            } else {
                return;
            }
        }
    }

    private void updateGradientDrawablePadding(TypedArray a) {
        GradientState st = this.mGradientState;
        st.mChangingConfigurations |= a.getChangingConfigurations();
        st.mAttrPadding = a.extractThemeAttrs();
        if (st.mPadding == null) {
            st.mPadding = new Rect();
        }
        Rect pad = st.mPadding;
        pad.set(a.getDimensionPixelOffset(0, pad.left), a.getDimensionPixelOffset(1, pad.top), a.getDimensionPixelOffset(2, pad.right), a.getDimensionPixelOffset(3, pad.bottom));
        this.mPadding = pad;
    }

    private void updateDrawableCorners(TypedArray a) {
        GradientState st = this.mGradientState;
        st.mChangingConfigurations |= a.getChangingConfigurations();
        st.mAttrCorners = a.extractThemeAttrs();
        int radius = a.getDimensionPixelSize(0, (int) st.mRadius);
        setCornerRadius(radius);
        int topLeftRadius = a.getDimensionPixelSize(1, radius);
        int topRightRadius = a.getDimensionPixelSize(2, radius);
        int bottomLeftRadius = a.getDimensionPixelSize(3, radius);
        int bottomRightRadius = a.getDimensionPixelSize(4, radius);
        if (topLeftRadius != radius || topRightRadius != radius || bottomLeftRadius != radius || bottomRightRadius != radius) {
            setCornerRadii(new float[]{topLeftRadius, topLeftRadius, topRightRadius, topRightRadius, bottomRightRadius, bottomRightRadius, bottomLeftRadius, bottomLeftRadius});
        }
    }

    private void updateGradientDrawableStroke(TypedArray a) {
        GradientState st = this.mGradientState;
        st.mChangingConfigurations |= a.getChangingConfigurations();
        st.mAttrStroke = a.extractThemeAttrs();
        int defaultStrokeWidth = Math.max(0, st.mStrokeWidth);
        int width = a.getDimensionPixelSize(0, defaultStrokeWidth);
        float dashWidth = a.getDimension(2, st.mStrokeDashWidth);
        ColorStateList colorStateList = a.getColorStateList(1);
        if (colorStateList == null) {
            colorStateList = st.mStrokeColorStateList;
        }
        if (dashWidth != 0.0f) {
            float dashGap = a.getDimension(3, st.mStrokeDashGap);
            setStroke(width, colorStateList, dashWidth, dashGap);
        } else {
            setStroke(width, colorStateList);
        }
    }

    private void updateGradientDrawableSolid(TypedArray a) {
        GradientState st = this.mGradientState;
        st.mChangingConfigurations |= a.getChangingConfigurations();
        st.mAttrSolid = a.extractThemeAttrs();
        ColorStateList colorStateList = a.getColorStateList(0);
        if (colorStateList != null) {
            setColor(colorStateList);
        }
    }

    private void updateGradientDrawableGradient(Resources r, TypedArray a) throws XmlPullParserException {
        float radius;
        int radiusType;
        GradientState st = this.mGradientState;
        st.mChangingConfigurations |= a.getChangingConfigurations();
        st.mAttrGradient = a.extractThemeAttrs();
        st.mCenterX = getFloatOrFraction(a, 5, st.mCenterX);
        st.mCenterY = getFloatOrFraction(a, 6, st.mCenterY);
        st.mUseLevel = a.getBoolean(2, st.mUseLevel);
        st.mGradient = a.getInt(4, st.mGradient);
        int startColor = a.getColor(0, 0);
        boolean hasCenterColor = a.hasValue(8);
        int centerColor = a.getColor(8, 0);
        int endColor = a.getColor(1, 0);
        if (hasCenterColor) {
            st.mColors = new int[3];
            st.mColors[0] = startColor;
            st.mColors[1] = centerColor;
            st.mColors[2] = endColor;
            st.mPositions = new float[3];
            st.mPositions[0] = 0.0f;
            st.mPositions[1] = st.mCenterX != 0.5f ? st.mCenterX : st.mCenterY;
            st.mPositions[2] = 1.0f;
        } else {
            st.mColors = new int[2];
            st.mColors[0] = startColor;
            st.mColors[1] = endColor;
        }
        if (st.mGradient == 0) {
            int angle = ((int) a.getFloat(3, st.mAngle)) % 360;
            if (angle % 45 != 0) {
                throw new XmlPullParserException(a.getPositionDescription() + "<gradient> tag requires 'angle' attribute to be a multiple of 45");
            }
            st.mAngle = angle;
            switch (angle) {
                case 0:
                    st.mOrientation = Orientation.LEFT_RIGHT;
                    return;
                case 45:
                    st.mOrientation = Orientation.BL_TR;
                    return;
                case 90:
                    st.mOrientation = Orientation.BOTTOM_TOP;
                    return;
                case 135:
                    st.mOrientation = Orientation.BR_TL;
                    return;
                case 180:
                    st.mOrientation = Orientation.RIGHT_LEFT;
                    return;
                case 225:
                    st.mOrientation = Orientation.TR_BL;
                    return;
                case R.styleable.Theme_textUnderlineColor:
                    st.mOrientation = Orientation.TOP_BOTTOM;
                    return;
                case 315:
                    st.mOrientation = Orientation.TL_BR;
                    return;
                default:
                    return;
            }
        }
        TypedValue tv = a.peekValue(7);
        if (tv != null) {
            if (tv.type == 6) {
                radius = tv.getFraction(1.0f, 1.0f);
                int unit = (tv.data >> 0) & 15;
                if (unit == 1) {
                    radiusType = 2;
                } else {
                    radiusType = 1;
                }
            } else if (tv.type == 5) {
                radius = tv.getDimension(r.getDisplayMetrics());
                radiusType = 0;
            } else {
                radius = tv.getFloat();
                radiusType = 0;
            }
            st.mGradientRadius = radius;
            st.mGradientRadiusType = radiusType;
            return;
        }
        if (st.mGradient == 1) {
            throw new XmlPullParserException(a.getPositionDescription() + "<gradient> tag requires 'gradientRadius' attribute with radial type");
        }
    }

    private void updateGradientDrawableSize(TypedArray a) {
        GradientState st = this.mGradientState;
        st.mChangingConfigurations |= a.getChangingConfigurations();
        st.mAttrSize = a.extractThemeAttrs();
        st.mWidth = a.getDimensionPixelSize(1, st.mWidth);
        st.mHeight = a.getDimensionPixelSize(0, st.mHeight);
    }

    private static float getFloatOrFraction(TypedArray a, int index, float defaultValue) {
        TypedValue tv = a.peekValue(index);
        if (tv == null) {
            return defaultValue;
        }
        boolean vIsFraction = tv.type == 6;
        if (!vIsFraction) {
            float v = tv.getFloat();
            return v;
        }
        float v2 = tv.getFraction(1.0f, 1.0f);
        return v2;
    }

    @Override
    public int getIntrinsicWidth() {
        return this.mGradientState.mWidth;
    }

    @Override
    public int getIntrinsicHeight() {
        return this.mGradientState.mHeight;
    }

    @Override
    public Drawable.ConstantState getConstantState() {
        this.mGradientState.mChangingConfigurations = getChangingConfigurations();
        return this.mGradientState;
    }

    private boolean isOpaqueForState() {
        return (this.mGradientState.mStrokeWidth < 0 || this.mStrokePaint == null || isOpaque(this.mStrokePaint.getColor())) && isOpaque(this.mFillPaint.getColor());
    }

    @Override
    public void getOutline(Outline outline) {
        GradientState st = this.mGradientState;
        Rect bounds = getBounds();
        outline.setAlpha((st.mOpaqueOverShape && isOpaqueForState()) ? this.mAlpha / 255.0f : 0.0f);
        switch (st.mShape) {
            case 0:
                if (st.mRadiusArray != null) {
                    buildPathIfDirty();
                    outline.setConvexPath(this.mPath);
                } else {
                    float rad = 0.0f;
                    if (st.mRadius > 0.0f) {
                        rad = Math.min(st.mRadius, Math.min(bounds.width(), bounds.height()) * 0.5f);
                    }
                    outline.setRoundRect(bounds, rad);
                }
                break;
            case 1:
                outline.setOval(bounds);
                break;
            case 2:
                float halfStrokeWidth = this.mStrokePaint == null ? 1.0E-4f : this.mStrokePaint.getStrokeWidth() * 0.5f;
                float centerY = bounds.centerY();
                int top = (int) Math.floor(centerY - halfStrokeWidth);
                int bottom = (int) Math.ceil(centerY + halfStrokeWidth);
                outline.setRect(bounds.left, top, bounds.right, bottom);
                break;
        }
    }

    @Override
    public Drawable mutate() {
        if (!this.mMutated && super.mutate() == this) {
            this.mGradientState = new GradientState(this.mGradientState);
            initializeWithState(this.mGradientState);
            this.mMutated = true;
        }
        return this;
    }

    @Override
    public void clearMutated() {
        super.clearMutated();
        this.mMutated = false;
    }

    static final class GradientState extends Drawable.ConstantState {
        public int mAngle;
        int[] mAttrCorners;
        int[] mAttrGradient;
        int[] mAttrPadding;
        int[] mAttrSize;
        int[] mAttrSolid;
        int[] mAttrStroke;
        float mCenterX;
        float mCenterY;
        public int mChangingConfigurations;
        public ColorStateList mColorStateList;
        public int[] mColors;
        public boolean mDither;
        public int mGradient;
        float mGradientRadius;
        int mGradientRadiusType;
        public int mHeight;
        public int mInnerRadius;
        public float mInnerRadiusRatio;
        boolean mOpaqueOverBounds;
        boolean mOpaqueOverShape;
        public Orientation mOrientation;
        public Rect mPadding;
        public float[] mPositions;
        public float mRadius;
        public float[] mRadiusArray;
        public int mShape;
        public ColorStateList mStrokeColorStateList;
        public float mStrokeDashGap;
        public float mStrokeDashWidth;
        public int mStrokeWidth;
        public int[] mTempColors;
        public float[] mTempPositions;
        int[] mThemeAttrs;
        public int mThickness;
        public float mThicknessRatio;
        ColorStateList mTint;
        PorterDuff.Mode mTintMode;
        boolean mUseLevel;
        boolean mUseLevelForShape;
        public int mWidth;

        GradientState(Orientation orientation, int[] colors) {
            this.mShape = 0;
            this.mGradient = 0;
            this.mAngle = 0;
            this.mStrokeWidth = -1;
            this.mStrokeDashWidth = 0.0f;
            this.mStrokeDashGap = 0.0f;
            this.mRadius = 0.0f;
            this.mRadiusArray = null;
            this.mPadding = null;
            this.mWidth = -1;
            this.mHeight = -1;
            this.mInnerRadiusRatio = GradientDrawable.DEFAULT_INNER_RADIUS_RATIO;
            this.mThicknessRatio = GradientDrawable.DEFAULT_THICKNESS_RATIO;
            this.mInnerRadius = -1;
            this.mThickness = -1;
            this.mDither = false;
            this.mCenterX = 0.5f;
            this.mCenterY = 0.5f;
            this.mGradientRadius = 0.5f;
            this.mGradientRadiusType = 0;
            this.mUseLevel = false;
            this.mUseLevelForShape = true;
            this.mTint = null;
            this.mTintMode = Drawable.DEFAULT_TINT_MODE;
            this.mOrientation = orientation;
            setColors(colors);
        }

        public GradientState(GradientState state) {
            this.mShape = 0;
            this.mGradient = 0;
            this.mAngle = 0;
            this.mStrokeWidth = -1;
            this.mStrokeDashWidth = 0.0f;
            this.mStrokeDashGap = 0.0f;
            this.mRadius = 0.0f;
            this.mRadiusArray = null;
            this.mPadding = null;
            this.mWidth = -1;
            this.mHeight = -1;
            this.mInnerRadiusRatio = GradientDrawable.DEFAULT_INNER_RADIUS_RATIO;
            this.mThicknessRatio = GradientDrawable.DEFAULT_THICKNESS_RATIO;
            this.mInnerRadius = -1;
            this.mThickness = -1;
            this.mDither = false;
            this.mCenterX = 0.5f;
            this.mCenterY = 0.5f;
            this.mGradientRadius = 0.5f;
            this.mGradientRadiusType = 0;
            this.mUseLevel = false;
            this.mUseLevelForShape = true;
            this.mTint = null;
            this.mTintMode = Drawable.DEFAULT_TINT_MODE;
            this.mChangingConfigurations = state.mChangingConfigurations;
            this.mShape = state.mShape;
            this.mGradient = state.mGradient;
            this.mAngle = state.mAngle;
            this.mOrientation = state.mOrientation;
            this.mColorStateList = state.mColorStateList;
            if (state.mColors != null) {
                this.mColors = (int[]) state.mColors.clone();
            }
            if (state.mPositions != null) {
                this.mPositions = (float[]) state.mPositions.clone();
            }
            this.mStrokeColorStateList = state.mStrokeColorStateList;
            this.mStrokeWidth = state.mStrokeWidth;
            this.mStrokeDashWidth = state.mStrokeDashWidth;
            this.mStrokeDashGap = state.mStrokeDashGap;
            this.mRadius = state.mRadius;
            if (state.mRadiusArray != null) {
                this.mRadiusArray = (float[]) state.mRadiusArray.clone();
            }
            if (state.mPadding != null) {
                this.mPadding = new Rect(state.mPadding);
            }
            this.mWidth = state.mWidth;
            this.mHeight = state.mHeight;
            this.mInnerRadiusRatio = state.mInnerRadiusRatio;
            this.mThicknessRatio = state.mThicknessRatio;
            this.mInnerRadius = state.mInnerRadius;
            this.mThickness = state.mThickness;
            this.mDither = state.mDither;
            this.mCenterX = state.mCenterX;
            this.mCenterY = state.mCenterY;
            this.mGradientRadius = state.mGradientRadius;
            this.mGradientRadiusType = state.mGradientRadiusType;
            this.mUseLevel = state.mUseLevel;
            this.mUseLevelForShape = state.mUseLevelForShape;
            this.mOpaqueOverBounds = state.mOpaqueOverBounds;
            this.mOpaqueOverShape = state.mOpaqueOverShape;
            this.mTint = state.mTint;
            this.mTintMode = state.mTintMode;
            this.mThemeAttrs = state.mThemeAttrs;
            this.mAttrSize = state.mAttrSize;
            this.mAttrGradient = state.mAttrGradient;
            this.mAttrSolid = state.mAttrSolid;
            this.mAttrStroke = state.mAttrStroke;
            this.mAttrCorners = state.mAttrCorners;
            this.mAttrPadding = state.mAttrPadding;
        }

        @Override
        public boolean canApplyTheme() {
            return (this.mThemeAttrs == null && this.mAttrSize == null && this.mAttrGradient == null && this.mAttrSolid == null && this.mAttrStroke == null && this.mAttrCorners == null && this.mAttrPadding == null && !super.canApplyTheme()) ? false : true;
        }

        @Override
        public Drawable newDrawable() {
            return new GradientDrawable(this);
        }

        @Override
        public Drawable newDrawable(Resources res) {
            return new GradientDrawable(this);
        }

        @Override
        public int getChangingConfigurations() {
            return this.mChangingConfigurations;
        }

        public void setShape(int shape) {
            this.mShape = shape;
            computeOpacity();
        }

        public void setGradientType(int gradient) {
            this.mGradient = gradient;
        }

        public void setGradientCenter(float x, float y) {
            this.mCenterX = x;
            this.mCenterY = y;
        }

        public void setColors(int[] colors) {
            this.mColors = colors;
            this.mColorStateList = null;
            computeOpacity();
        }

        public void setColorStateList(ColorStateList colorStateList) {
            this.mColors = null;
            this.mColorStateList = colorStateList;
            computeOpacity();
        }

        private void computeOpacity() {
            this.mOpaqueOverBounds = false;
            this.mOpaqueOverShape = false;
            if (this.mColors != null) {
                for (int i = 0; i < this.mColors.length; i++) {
                    if (!GradientDrawable.isOpaque(this.mColors[i])) {
                        return;
                    }
                }
            }
            if (this.mColors != null || this.mColorStateList != null) {
                this.mOpaqueOverShape = true;
                this.mOpaqueOverBounds = this.mShape == 0 && this.mRadius <= 0.0f && this.mRadiusArray == null;
            }
        }

        public void setStroke(int width, ColorStateList colorStateList, float dashWidth, float dashGap) {
            this.mStrokeWidth = width;
            this.mStrokeColorStateList = colorStateList;
            this.mStrokeDashWidth = dashWidth;
            this.mStrokeDashGap = dashGap;
            computeOpacity();
        }

        public void setCornerRadius(float radius) {
            if (radius < 0.0f) {
                radius = 0.0f;
            }
            this.mRadius = radius;
            this.mRadiusArray = null;
        }

        public void setCornerRadii(float[] radii) {
            this.mRadiusArray = radii;
            if (radii == null) {
                this.mRadius = 0.0f;
            }
        }

        public void setSize(int width, int height) {
            this.mWidth = width;
            this.mHeight = height;
        }

        public void setGradientRadius(float gradientRadius, int type) {
            this.mGradientRadius = gradientRadius;
            this.mGradientRadiusType = type;
        }
    }

    static boolean isOpaque(int color) {
        return ((color >> 24) & 255) == 255;
    }

    private GradientDrawable(GradientState state) {
        this.mFillPaint = new Paint(1);
        this.mAlpha = 255;
        this.mPath = new Path();
        this.mRect = new RectF();
        this.mPathIsDirty = true;
        this.mGradientState = state;
        initializeWithState(this.mGradientState);
        this.mGradientIsDirty = true;
        this.mMutated = false;
    }

    private void initializeWithState(GradientState state) {
        if (state.mColorStateList != null) {
            int[] currentState = getState();
            int stateColor = state.mColorStateList.getColorForState(currentState, 0);
            this.mFillPaint.setColor(stateColor);
        } else if (state.mColors == null) {
            this.mFillPaint.setColor(0);
        } else {
            this.mFillPaint.setColor(-16777216);
        }
        this.mPadding = state.mPadding;
        if (state.mStrokeWidth >= 0) {
            this.mStrokePaint = new Paint(1);
            this.mStrokePaint.setStyle(Paint.Style.STROKE);
            this.mStrokePaint.setStrokeWidth(state.mStrokeWidth);
            if (state.mStrokeColorStateList != null) {
                int[] currentState2 = getState();
                int strokeStateColor = state.mStrokeColorStateList.getColorForState(currentState2, 0);
                this.mStrokePaint.setColor(strokeStateColor);
            }
            if (state.mStrokeDashWidth != 0.0f) {
                DashPathEffect e = new DashPathEffect(new float[]{state.mStrokeDashWidth, state.mStrokeDashGap}, 0.0f);
                this.mStrokePaint.setPathEffect(e);
            }
        }
        this.mTintFilter = updateTintFilter(this.mTintFilter, state.mTint, state.mTintMode);
    }
}
