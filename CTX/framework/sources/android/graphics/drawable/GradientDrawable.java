package android.graphics.drawable;

import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.DashPathEffect;
import android.graphics.Insets;
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
import android.graphics.Xfermode;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import com.android.internal.R;
import com.android.internal.app.DumpHeapActivity;
import java.io.IOException;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
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

    @Retention(RetentionPolicy.SOURCE)
    public @interface GradientType {
    }

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

    @Retention(RetentionPolicy.SOURCE)
    public @interface RadiusType {
    }

    @Retention(RetentionPolicy.SOURCE)
    public @interface Shape {
    }

    public GradientDrawable() {
        this(new GradientState(Orientation.TOP_BOTTOM, (int[]) null), (Resources) null);
    }

    public GradientDrawable(Orientation orientation, int[] iArr) {
        this(new GradientState(orientation, iArr), (Resources) null);
    }

    @Override
    public boolean getPadding(Rect rect) {
        if (this.mPadding != null) {
            rect.set(this.mPadding);
            return true;
        }
        return super.getPadding(rect);
    }

    public void setCornerRadii(float[] fArr) {
        this.mGradientState.setCornerRadii(fArr);
        this.mPathIsDirty = true;
        invalidateSelf();
    }

    public float[] getCornerRadii() {
        return (float[]) this.mGradientState.mRadiusArray.clone();
    }

    public void setCornerRadius(float f) {
        this.mGradientState.setCornerRadius(f);
        this.mPathIsDirty = true;
        invalidateSelf();
    }

    public float getCornerRadius() {
        return this.mGradientState.mRadius;
    }

    public void setStroke(int i, int i2) {
        setStroke(i, i2, 0.0f, 0.0f);
    }

    public void setStroke(int i, ColorStateList colorStateList) {
        setStroke(i, colorStateList, 0.0f, 0.0f);
    }

    public void setStroke(int i, int i2, float f, float f2) {
        this.mGradientState.setStroke(i, ColorStateList.valueOf(i2), f, f2);
        setStrokeInternal(i, i2, f, f2);
    }

    public void setStroke(int i, ColorStateList colorStateList, float f, float f2) {
        this.mGradientState.setStroke(i, colorStateList, f, f2);
        int colorForState = 0;
        if (colorStateList != null) {
            colorForState = colorStateList.getColorForState(getState(), 0);
        }
        setStrokeInternal(i, colorForState, f, f2);
    }

    private void setStrokeInternal(int i, int i2, float f, float f2) {
        if (this.mStrokePaint == null) {
            this.mStrokePaint = new Paint(1);
            this.mStrokePaint.setStyle(Paint.Style.STROKE);
        }
        this.mStrokePaint.setStrokeWidth(i);
        this.mStrokePaint.setColor(i2);
        DashPathEffect dashPathEffect = null;
        if (f > 0.0f) {
            dashPathEffect = new DashPathEffect(new float[]{f, f2}, 0.0f);
        }
        this.mStrokePaint.setPathEffect(dashPathEffect);
        invalidateSelf();
    }

    public void setSize(int i, int i2) {
        this.mGradientState.setSize(i, i2);
        this.mPathIsDirty = true;
        invalidateSelf();
    }

    public void setShape(int i) {
        this.mRingPath = null;
        this.mPathIsDirty = true;
        this.mGradientState.setShape(i);
        invalidateSelf();
    }

    public int getShape() {
        return this.mGradientState.mShape;
    }

    public void setGradientType(int i) {
        this.mGradientState.setGradientType(i);
        this.mGradientIsDirty = true;
        invalidateSelf();
    }

    public int getGradientType() {
        return this.mGradientState.mGradient;
    }

    public void setGradientCenter(float f, float f2) {
        this.mGradientState.setGradientCenter(f, f2);
        this.mGradientIsDirty = true;
        invalidateSelf();
    }

    public float getGradientCenterX() {
        return this.mGradientState.mCenterX;
    }

    public float getGradientCenterY() {
        return this.mGradientState.mCenterY;
    }

    public void setGradientRadius(float f) {
        this.mGradientState.setGradientRadius(f, 0);
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

    public void setUseLevel(boolean z) {
        this.mGradientState.mUseLevel = z;
        this.mGradientIsDirty = true;
        invalidateSelf();
    }

    public boolean getUseLevel() {
        return this.mGradientState.mUseLevel;
    }

    private int modulateAlpha(int i) {
        return (i * (this.mAlpha + (this.mAlpha >> 7))) >> 8;
    }

    public Orientation getOrientation() {
        return this.mGradientState.mOrientation;
    }

    public void setOrientation(Orientation orientation) {
        this.mGradientState.mOrientation = orientation;
        this.mGradientIsDirty = true;
        invalidateSelf();
    }

    public void setColors(int[] iArr) {
        this.mGradientState.setGradientColors(iArr);
        this.mGradientIsDirty = true;
        invalidateSelf();
    }

    public int[] getColors() {
        if (this.mGradientState.mGradientColors == null) {
            return null;
        }
        return (int[]) this.mGradientState.mGradientColors.clone();
    }

    @Override
    public void draw(Canvas canvas) {
        if (!ensureValidRect()) {
            return;
        }
        int alpha = this.mFillPaint.getAlpha();
        int alpha2 = this.mStrokePaint != null ? this.mStrokePaint.getAlpha() : 0;
        int iModulateAlpha = modulateAlpha(alpha);
        int iModulateAlpha2 = modulateAlpha(alpha2);
        boolean z = iModulateAlpha2 > 0 && this.mStrokePaint != null && this.mStrokePaint.getStrokeWidth() > 0.0f;
        boolean z2 = iModulateAlpha > 0;
        GradientState gradientState = this.mGradientState;
        ColorFilter colorFilter = this.mColorFilter != null ? this.mColorFilter : this.mTintFilter;
        boolean z3 = z && z2 && gradientState.mShape != 2 && iModulateAlpha2 < 255 && (this.mAlpha < 255 || colorFilter != null);
        if (z3) {
            if (this.mLayerPaint == null) {
                this.mLayerPaint = new Paint();
            }
            this.mLayerPaint.setDither(gradientState.mDither);
            this.mLayerPaint.setAlpha(this.mAlpha);
            this.mLayerPaint.setColorFilter(colorFilter);
            float strokeWidth = this.mStrokePaint.getStrokeWidth();
            canvas.saveLayer(this.mRect.left - strokeWidth, this.mRect.top - strokeWidth, this.mRect.right + strokeWidth, this.mRect.bottom + strokeWidth, this.mLayerPaint);
            this.mFillPaint.setColorFilter(null);
            this.mStrokePaint.setColorFilter(null);
        } else {
            this.mFillPaint.setAlpha(iModulateAlpha);
            this.mFillPaint.setDither(gradientState.mDither);
            this.mFillPaint.setColorFilter(colorFilter);
            if (colorFilter != null && gradientState.mSolidColors == null) {
                this.mFillPaint.setColor(this.mAlpha << 24);
            }
            if (z) {
                this.mStrokePaint.setAlpha(iModulateAlpha2);
                this.mStrokePaint.setDither(gradientState.mDither);
                this.mStrokePaint.setColorFilter(colorFilter);
            }
        }
        switch (gradientState.mShape) {
            case 0:
                if (gradientState.mRadiusArray != null) {
                    buildPathIfDirty();
                    canvas.drawPath(this.mPath, this.mFillPaint);
                    if (z) {
                        canvas.drawPath(this.mPath, this.mStrokePaint);
                    }
                } else if (gradientState.mRadius > 0.0f) {
                    float fMin = Math.min(gradientState.mRadius, Math.min(this.mRect.width(), this.mRect.height()) * 0.5f);
                    canvas.drawRoundRect(this.mRect, fMin, fMin, this.mFillPaint);
                    if (z) {
                        canvas.drawRoundRect(this.mRect, fMin, fMin, this.mStrokePaint);
                    }
                } else {
                    if (this.mFillPaint.getColor() != 0 || colorFilter != null || this.mFillPaint.getShader() != null) {
                        canvas.drawRect(this.mRect, this.mFillPaint);
                    }
                    if (z) {
                        canvas.drawRect(this.mRect, this.mStrokePaint);
                    }
                }
                break;
            case 1:
                canvas.drawOval(this.mRect, this.mFillPaint);
                if (z) {
                    canvas.drawOval(this.mRect, this.mStrokePaint);
                }
                break;
            case 2:
                RectF rectF = this.mRect;
                float fCenterY = rectF.centerY();
                if (z) {
                    canvas.drawLine(rectF.left, fCenterY, rectF.right, fCenterY, this.mStrokePaint);
                }
                break;
            case 3:
                Path pathBuildRing = buildRing(gradientState);
                canvas.drawPath(pathBuildRing, this.mFillPaint);
                if (z) {
                    canvas.drawPath(pathBuildRing, this.mStrokePaint);
                }
                break;
        }
        if (z3) {
            canvas.restore();
            return;
        }
        this.mFillPaint.setAlpha(alpha);
        if (z) {
            this.mStrokePaint.setAlpha(alpha2);
        }
    }

    @Override
    public void setXfermode(Xfermode xfermode) {
        super.setXfermode(xfermode);
        this.mFillPaint.setXfermode(xfermode);
    }

    public void setAntiAlias(boolean z) {
        this.mFillPaint.setAntiAlias(z);
    }

    private void buildPathIfDirty() {
        GradientState gradientState = this.mGradientState;
        if (this.mPathIsDirty) {
            ensureValidRect();
            this.mPath.reset();
            this.mPath.addRoundRect(this.mRect, gradientState.mRadiusArray, Path.Direction.CW);
            this.mPathIsDirty = false;
        }
    }

    private Path buildRing(GradientState gradientState) {
        if (this.mRingPath != null && (!gradientState.mUseLevelForShape || !this.mPathIsDirty)) {
            return this.mRingPath;
        }
        this.mPathIsDirty = false;
        float level = gradientState.mUseLevelForShape ? (getLevel() * 360.0f) / 10000.0f : 360.0f;
        RectF rectF = new RectF(this.mRect);
        float fWidth = rectF.width() / 2.0f;
        float fHeight = rectF.height() / 2.0f;
        float fWidth2 = gradientState.mThickness != -1 ? gradientState.mThickness : rectF.width() / gradientState.mThicknessRatio;
        float fWidth3 = gradientState.mInnerRadius != -1 ? gradientState.mInnerRadius : rectF.width() / gradientState.mInnerRadiusRatio;
        RectF rectF2 = new RectF(rectF);
        rectF2.inset(fWidth - fWidth3, fHeight - fWidth3);
        RectF rectF3 = new RectF(rectF2);
        float f = -fWidth2;
        rectF3.inset(f, f);
        if (this.mRingPath == null) {
            this.mRingPath = new Path();
        } else {
            this.mRingPath.reset();
        }
        Path path = this.mRingPath;
        if (level < 360.0f && level > -360.0f) {
            path.setFillType(Path.FillType.EVEN_ODD);
            float f2 = fWidth + fWidth3;
            path.moveTo(f2, fHeight);
            path.lineTo(f2 + fWidth2, fHeight);
            path.arcTo(rectF3, 0.0f, level, false);
            path.arcTo(rectF2, level, -level, false);
            path.close();
        } else {
            path.addOval(rectF3, Path.Direction.CW);
            path.addOval(rectF2, Path.Direction.CCW);
        }
        return path;
    }

    public void setColor(int i) {
        this.mGradientState.setSolidColors(ColorStateList.valueOf(i));
        this.mFillPaint.setColor(i);
        invalidateSelf();
    }

    public void setColor(ColorStateList colorStateList) {
        this.mGradientState.setSolidColors(colorStateList);
        int colorForState = 0;
        if (colorStateList != null) {
            colorForState = colorStateList.getColorForState(getState(), 0);
        }
        this.mFillPaint.setColor(colorForState);
        invalidateSelf();
    }

    public ColorStateList getColor() {
        return this.mGradientState.mSolidColors;
    }

    @Override
    protected boolean onStateChange(int[] iArr) {
        boolean z;
        ColorStateList colorStateList;
        int colorForState;
        int colorForState2;
        GradientState gradientState = this.mGradientState;
        ColorStateList colorStateList2 = gradientState.mSolidColors;
        if (colorStateList2 == null || this.mFillPaint.getColor() == (colorForState2 = colorStateList2.getColorForState(iArr, 0))) {
            z = false;
        } else {
            this.mFillPaint.setColor(colorForState2);
            z = true;
        }
        Paint paint = this.mStrokePaint;
        if (paint != null && (colorStateList = gradientState.mStrokeColors) != null && paint.getColor() != (colorForState = colorStateList.getColorForState(iArr, 0))) {
            paint.setColor(colorForState);
            z = true;
        }
        if (gradientState.mTint != null && gradientState.mTintMode != null) {
            this.mTintFilter = updateTintFilter(this.mTintFilter, gradientState.mTint, gradientState.mTintMode);
            z = true;
        }
        if (!z) {
            return false;
        }
        invalidateSelf();
        return true;
    }

    @Override
    public boolean isStateful() {
        GradientState gradientState = this.mGradientState;
        return super.isStateful() || (gradientState.mSolidColors != null && gradientState.mSolidColors.isStateful()) || ((gradientState.mStrokeColors != null && gradientState.mStrokeColors.isStateful()) || (gradientState.mTint != null && gradientState.mTint.isStateful()));
    }

    @Override
    public boolean hasFocusStateSpecified() {
        GradientState gradientState = this.mGradientState;
        return (gradientState.mSolidColors != null && gradientState.mSolidColors.hasFocusStateSpecified()) || (gradientState.mStrokeColors != null && gradientState.mStrokeColors.hasFocusStateSpecified()) || (gradientState.mTint != null && gradientState.mTint.hasFocusStateSpecified());
    }

    @Override
    public int getChangingConfigurations() {
        return super.getChangingConfigurations() | this.mGradientState.getChangingConfigurations();
    }

    @Override
    public void setAlpha(int i) {
        if (i != this.mAlpha) {
            this.mAlpha = i;
            invalidateSelf();
        }
    }

    @Override
    public int getAlpha() {
        return this.mAlpha;
    }

    @Override
    public void setDither(boolean z) {
        if (z != this.mGradientState.mDither) {
            this.mGradientState.mDither = z;
            invalidateSelf();
        }
    }

    @Override
    public ColorFilter getColorFilter() {
        return this.mColorFilter;
    }

    @Override
    public void setColorFilter(ColorFilter colorFilter) {
        if (colorFilter != this.mColorFilter) {
            this.mColorFilter = colorFilter;
            invalidateSelf();
        }
    }

    @Override
    public void setTintList(ColorStateList colorStateList) {
        this.mGradientState.mTint = colorStateList;
        this.mTintFilter = updateTintFilter(this.mTintFilter, colorStateList, this.mGradientState.mTintMode);
        invalidateSelf();
    }

    @Override
    public void setTintMode(PorterDuff.Mode mode) {
        this.mGradientState.mTintMode = mode;
        this.mTintFilter = updateTintFilter(this.mTintFilter, this.mGradientState.mTint, mode);
        invalidateSelf();
    }

    @Override
    public int getOpacity() {
        return (this.mAlpha == 255 && this.mGradientState.mOpaqueOverBounds && isOpaqueForState()) ? -1 : -3;
    }

    @Override
    protected void onBoundsChange(Rect rect) {
        super.onBoundsChange(rect);
        this.mRingPath = null;
        this.mPathIsDirty = true;
        this.mGradientIsDirty = true;
    }

    @Override
    protected boolean onLevelChange(int i) {
        super.onLevelChange(i);
        this.mGradientIsDirty = true;
        this.mPathIsDirty = true;
        invalidateSelf();
        return true;
    }

    private boolean ensureValidRect() {
        float strokeWidth;
        float f;
        float f2;
        float f3;
        float f4;
        float f5;
        float f6;
        float f7;
        float f8;
        float f9;
        float f10;
        float f11;
        float f12;
        float f13;
        if (this.mGradientIsDirty) {
            this.mGradientIsDirty = false;
            Rect bounds = getBounds();
            if (this.mStrokePaint != null) {
                strokeWidth = this.mStrokePaint.getStrokeWidth() * 0.5f;
            } else {
                strokeWidth = 0.0f;
            }
            GradientState gradientState = this.mGradientState;
            this.mRect.set(bounds.left + strokeWidth, bounds.top + strokeWidth, bounds.right - strokeWidth, bounds.bottom - strokeWidth);
            int[] iArr = gradientState.mGradientColors;
            if (iArr != null) {
                RectF rectF = this.mRect;
                if (gradientState.mGradient == 0) {
                    float level = gradientState.mUseLevel ? getLevel() / 10000.0f : 1.0f;
                    switch (gradientState.mOrientation) {
                        case TOP_BOTTOM:
                            f = rectF.left;
                            f2 = rectF.top;
                            f3 = level * rectF.bottom;
                            f10 = f;
                            f11 = f10;
                            f12 = f2;
                            f13 = f3;
                            break;
                        case TR_BL:
                            f4 = rectF.right;
                            f5 = rectF.top;
                            f6 = rectF.left * level;
                            f3 = level * rectF.bottom;
                            f10 = f4;
                            f12 = f5;
                            f11 = f6;
                            f13 = f3;
                            break;
                        case RIGHT_LEFT:
                            f7 = rectF.right;
                            f8 = rectF.top;
                            f9 = level * rectF.left;
                            f10 = f7;
                            f12 = f8;
                            f13 = f12;
                            f11 = f9;
                            break;
                        case BR_TL:
                            f4 = rectF.right;
                            f5 = rectF.bottom;
                            f6 = rectF.left * level;
                            f3 = level * rectF.top;
                            f10 = f4;
                            f12 = f5;
                            f11 = f6;
                            f13 = f3;
                            break;
                        case BOTTOM_TOP:
                            f = rectF.left;
                            f2 = rectF.bottom;
                            f3 = level * rectF.top;
                            f10 = f;
                            f11 = f10;
                            f12 = f2;
                            f13 = f3;
                            break;
                        case BL_TR:
                            f4 = rectF.left;
                            f5 = rectF.bottom;
                            f6 = rectF.right * level;
                            f3 = level * rectF.top;
                            f10 = f4;
                            f12 = f5;
                            f11 = f6;
                            f13 = f3;
                            break;
                        case LEFT_RIGHT:
                            f7 = rectF.left;
                            f8 = rectF.top;
                            f9 = level * rectF.right;
                            f10 = f7;
                            f12 = f8;
                            f13 = f12;
                            f11 = f9;
                            break;
                        default:
                            f4 = rectF.left;
                            f5 = rectF.top;
                            f6 = rectF.right * level;
                            f3 = level * rectF.bottom;
                            f10 = f4;
                            f12 = f5;
                            f11 = f6;
                            f13 = f3;
                            break;
                    }
                    this.mFillPaint.setShader(new LinearGradient(f10, f12, f11, f13, iArr, gradientState.mPositions, Shader.TileMode.CLAMP));
                } else if (gradientState.mGradient == 1) {
                    float f14 = rectF.left + ((rectF.right - rectF.left) * gradientState.mCenterX);
                    float f15 = rectF.top + ((rectF.bottom - rectF.top) * gradientState.mCenterY);
                    float fMin = gradientState.mGradientRadius;
                    if (gradientState.mGradientRadiusType == 1) {
                        fMin *= Math.min(gradientState.mWidth >= 0 ? gradientState.mWidth : rectF.width(), gradientState.mHeight >= 0 ? gradientState.mHeight : rectF.height());
                    } else if (gradientState.mGradientRadiusType == 2) {
                        fMin *= Math.min(rectF.width(), rectF.height());
                    }
                    if (gradientState.mUseLevel) {
                        fMin *= getLevel() / 10000.0f;
                    }
                    this.mGradientRadius = fMin;
                    if (fMin <= 0.0f) {
                        fMin = 0.001f;
                    }
                    this.mFillPaint.setShader(new RadialGradient(f14, f15, fMin, iArr, (float[]) null, Shader.TileMode.CLAMP));
                } else if (gradientState.mGradient == 2) {
                    float f16 = rectF.left + ((rectF.right - rectF.left) * gradientState.mCenterX);
                    float f17 = rectF.top + ((rectF.bottom - rectF.top) * gradientState.mCenterY);
                    float[] fArr = null;
                    if (gradientState.mUseLevel) {
                        int[] iArr2 = gradientState.mTempColors;
                        int length = iArr.length;
                        if (iArr2 == null || iArr2.length != length + 1) {
                            iArr2 = new int[length + 1];
                            gradientState.mTempColors = iArr2;
                        }
                        System.arraycopy(iArr, 0, iArr2, 0, length);
                        int i = length - 1;
                        iArr2[length] = iArr[i];
                        float[] fArr2 = gradientState.mTempPositions;
                        float f18 = 1.0f / i;
                        if (fArr2 == null || fArr2.length != length + 1) {
                            fArr2 = new float[length + 1];
                            gradientState.mTempPositions = fArr2;
                        }
                        float level2 = getLevel() / 10000.0f;
                        for (int i2 = 0; i2 < length; i2++) {
                            fArr2[i2] = i2 * f18 * level2;
                        }
                        fArr2[length] = 1.0f;
                        int[] iArr3 = iArr2;
                        fArr = fArr2;
                        iArr = iArr3;
                    }
                    this.mFillPaint.setShader(new SweepGradient(f16, f17, iArr, fArr));
                }
                if (gradientState.mSolidColors == null) {
                    this.mFillPaint.setColor(-16777216);
                }
            }
        }
        return !this.mRect.isEmpty();
    }

    @Override
    public void inflate(Resources resources, XmlPullParser xmlPullParser, AttributeSet attributeSet, Resources.Theme theme) throws XmlPullParserException, IOException {
        super.inflate(resources, xmlPullParser, attributeSet, theme);
        this.mGradientState.setDensity(Drawable.resolveDensity(resources, 0));
        TypedArray typedArrayObtainAttributes = obtainAttributes(resources, theme, attributeSet, R.styleable.GradientDrawable);
        updateStateFromTypedArray(typedArrayObtainAttributes);
        typedArrayObtainAttributes.recycle();
        inflateChildElements(resources, xmlPullParser, attributeSet, theme);
        updateLocalState(resources);
    }

    @Override
    public void applyTheme(Resources.Theme theme) {
        super.applyTheme(theme);
        GradientState gradientState = this.mGradientState;
        if (gradientState == null) {
            return;
        }
        gradientState.setDensity(Drawable.resolveDensity(theme.getResources(), 0));
        if (gradientState.mThemeAttrs != null) {
            TypedArray typedArrayResolveAttributes = theme.resolveAttributes(gradientState.mThemeAttrs, R.styleable.GradientDrawable);
            updateStateFromTypedArray(typedArrayResolveAttributes);
            typedArrayResolveAttributes.recycle();
        }
        if (gradientState.mTint != null && gradientState.mTint.canApplyTheme()) {
            gradientState.mTint = gradientState.mTint.obtainForTheme(theme);
        }
        if (gradientState.mSolidColors != null && gradientState.mSolidColors.canApplyTheme()) {
            gradientState.mSolidColors = gradientState.mSolidColors.obtainForTheme(theme);
        }
        if (gradientState.mStrokeColors != null && gradientState.mStrokeColors.canApplyTheme()) {
            gradientState.mStrokeColors = gradientState.mStrokeColors.obtainForTheme(theme);
        }
        applyThemeChildElements(theme);
        updateLocalState(theme.getResources());
    }

    private void updateStateFromTypedArray(TypedArray typedArray) {
        GradientState gradientState = this.mGradientState;
        gradientState.mChangingConfigurations |= typedArray.getChangingConfigurations();
        gradientState.mThemeAttrs = typedArray.extractThemeAttrs();
        gradientState.mShape = typedArray.getInt(3, gradientState.mShape);
        gradientState.mDither = typedArray.getBoolean(0, gradientState.mDither);
        if (gradientState.mShape == 3) {
            gradientState.mInnerRadius = typedArray.getDimensionPixelSize(7, gradientState.mInnerRadius);
            if (gradientState.mInnerRadius == -1) {
                gradientState.mInnerRadiusRatio = typedArray.getFloat(4, gradientState.mInnerRadiusRatio);
            }
            gradientState.mThickness = typedArray.getDimensionPixelSize(8, gradientState.mThickness);
            if (gradientState.mThickness == -1) {
                gradientState.mThicknessRatio = typedArray.getFloat(5, gradientState.mThicknessRatio);
            }
            gradientState.mUseLevelForShape = typedArray.getBoolean(6, gradientState.mUseLevelForShape);
        }
        int i = typedArray.getInt(9, -1);
        if (i != -1) {
            gradientState.mTintMode = Drawable.parseTintMode(i, PorterDuff.Mode.SRC_IN);
        }
        ColorStateList colorStateList = typedArray.getColorStateList(1);
        if (colorStateList != null) {
            gradientState.mTint = colorStateList;
        }
        gradientState.mOpticalInsets = Insets.of(typedArray.getDimensionPixelSize(11, gradientState.mOpticalInsets.left), typedArray.getDimensionPixelSize(13, gradientState.mOpticalInsets.top), typedArray.getDimensionPixelSize(12, gradientState.mOpticalInsets.right), typedArray.getDimensionPixelSize(10, gradientState.mOpticalInsets.bottom));
    }

    @Override
    public boolean canApplyTheme() {
        return (this.mGradientState != null && this.mGradientState.canApplyTheme()) || super.canApplyTheme();
    }

    private void applyThemeChildElements(Resources.Theme theme) {
        GradientState gradientState = this.mGradientState;
        if (gradientState.mAttrSize != null) {
            TypedArray typedArrayResolveAttributes = theme.resolveAttributes(gradientState.mAttrSize, R.styleable.GradientDrawableSize);
            updateGradientDrawableSize(typedArrayResolveAttributes);
            typedArrayResolveAttributes.recycle();
        }
        if (gradientState.mAttrGradient != null) {
            TypedArray typedArrayResolveAttributes2 = theme.resolveAttributes(gradientState.mAttrGradient, R.styleable.GradientDrawableGradient);
            try {
                try {
                    updateGradientDrawableGradient(theme.getResources(), typedArrayResolveAttributes2);
                } catch (XmlPullParserException e) {
                    rethrowAsRuntimeException(e);
                }
                typedArrayResolveAttributes2.recycle();
            } finally {
                typedArrayResolveAttributes2.recycle();
            }
        }
        if (gradientState.mAttrSolid != null) {
            TypedArray typedArrayResolveAttributes3 = theme.resolveAttributes(gradientState.mAttrSolid, R.styleable.GradientDrawableSolid);
            updateGradientDrawableSolid(typedArrayResolveAttributes3);
            typedArrayResolveAttributes3.recycle();
        }
        if (gradientState.mAttrStroke != null) {
            TypedArray typedArrayResolveAttributes4 = theme.resolveAttributes(gradientState.mAttrStroke, R.styleable.GradientDrawableStroke);
            updateGradientDrawableStroke(typedArrayResolveAttributes4);
            typedArrayResolveAttributes4.recycle();
        }
        if (gradientState.mAttrCorners != null) {
            updateDrawableCorners(theme.resolveAttributes(gradientState.mAttrCorners, R.styleable.DrawableCorners));
        }
        if (gradientState.mAttrPadding != null) {
            updateGradientDrawablePadding(theme.resolveAttributes(gradientState.mAttrPadding, R.styleable.GradientDrawablePadding));
        }
    }

    private void inflateChildElements(Resources resources, XmlPullParser xmlPullParser, AttributeSet attributeSet, Resources.Theme theme) throws XmlPullParserException, IOException {
        int depth = xmlPullParser.getDepth() + 1;
        while (true) {
            int next = xmlPullParser.next();
            if (next != 1) {
                int depth2 = xmlPullParser.getDepth();
                if (depth2 >= depth || next != 3) {
                    if (next == 2 && depth2 <= depth) {
                        String name = xmlPullParser.getName();
                        if (name.equals(DumpHeapActivity.KEY_SIZE)) {
                            TypedArray typedArrayObtainAttributes = obtainAttributes(resources, theme, attributeSet, R.styleable.GradientDrawableSize);
                            updateGradientDrawableSize(typedArrayObtainAttributes);
                            typedArrayObtainAttributes.recycle();
                        } else if (name.equals("gradient")) {
                            TypedArray typedArrayObtainAttributes2 = obtainAttributes(resources, theme, attributeSet, R.styleable.GradientDrawableGradient);
                            updateGradientDrawableGradient(resources, typedArrayObtainAttributes2);
                            typedArrayObtainAttributes2.recycle();
                        } else if (name.equals("solid")) {
                            TypedArray typedArrayObtainAttributes3 = obtainAttributes(resources, theme, attributeSet, R.styleable.GradientDrawableSolid);
                            updateGradientDrawableSolid(typedArrayObtainAttributes3);
                            typedArrayObtainAttributes3.recycle();
                        } else if (name.equals("stroke")) {
                            TypedArray typedArrayObtainAttributes4 = obtainAttributes(resources, theme, attributeSet, R.styleable.GradientDrawableStroke);
                            updateGradientDrawableStroke(typedArrayObtainAttributes4);
                            typedArrayObtainAttributes4.recycle();
                        } else if (name.equals("corners")) {
                            TypedArray typedArrayObtainAttributes5 = obtainAttributes(resources, theme, attributeSet, R.styleable.DrawableCorners);
                            updateDrawableCorners(typedArrayObtainAttributes5);
                            typedArrayObtainAttributes5.recycle();
                        } else if (name.equals("padding")) {
                            TypedArray typedArrayObtainAttributes6 = obtainAttributes(resources, theme, attributeSet, R.styleable.GradientDrawablePadding);
                            updateGradientDrawablePadding(typedArrayObtainAttributes6);
                            typedArrayObtainAttributes6.recycle();
                        } else {
                            Log.w("drawable", "Bad element under <shape>: " + name);
                        }
                    }
                } else {
                    return;
                }
            } else {
                return;
            }
        }
    }

    private void updateGradientDrawablePadding(TypedArray typedArray) {
        GradientState gradientState = this.mGradientState;
        gradientState.mChangingConfigurations |= typedArray.getChangingConfigurations();
        gradientState.mAttrPadding = typedArray.extractThemeAttrs();
        if (gradientState.mPadding == null) {
            gradientState.mPadding = new Rect();
        }
        Rect rect = gradientState.mPadding;
        rect.set(typedArray.getDimensionPixelOffset(0, rect.left), typedArray.getDimensionPixelOffset(1, rect.top), typedArray.getDimensionPixelOffset(2, rect.right), typedArray.getDimensionPixelOffset(3, rect.bottom));
        this.mPadding = rect;
    }

    private void updateDrawableCorners(TypedArray typedArray) {
        GradientState gradientState = this.mGradientState;
        gradientState.mChangingConfigurations |= typedArray.getChangingConfigurations();
        gradientState.mAttrCorners = typedArray.extractThemeAttrs();
        int dimensionPixelSize = typedArray.getDimensionPixelSize(0, (int) gradientState.mRadius);
        setCornerRadius(dimensionPixelSize);
        int dimensionPixelSize2 = typedArray.getDimensionPixelSize(1, dimensionPixelSize);
        int dimensionPixelSize3 = typedArray.getDimensionPixelSize(2, dimensionPixelSize);
        int dimensionPixelSize4 = typedArray.getDimensionPixelSize(3, dimensionPixelSize);
        int dimensionPixelSize5 = typedArray.getDimensionPixelSize(4, dimensionPixelSize);
        if (dimensionPixelSize2 != dimensionPixelSize || dimensionPixelSize3 != dimensionPixelSize || dimensionPixelSize4 != dimensionPixelSize || dimensionPixelSize5 != dimensionPixelSize) {
            float f = dimensionPixelSize2;
            float f2 = dimensionPixelSize3;
            float f3 = dimensionPixelSize5;
            float f4 = dimensionPixelSize4;
            setCornerRadii(new float[]{f, f, f2, f2, f3, f3, f4, f4});
        }
    }

    private void updateGradientDrawableStroke(TypedArray typedArray) {
        GradientState gradientState = this.mGradientState;
        gradientState.mChangingConfigurations |= typedArray.getChangingConfigurations();
        gradientState.mAttrStroke = typedArray.extractThemeAttrs();
        int dimensionPixelSize = typedArray.getDimensionPixelSize(0, Math.max(0, gradientState.mStrokeWidth));
        float dimension = typedArray.getDimension(2, gradientState.mStrokeDashWidth);
        ColorStateList colorStateList = typedArray.getColorStateList(1);
        if (colorStateList == null) {
            colorStateList = gradientState.mStrokeColors;
        }
        if (dimension != 0.0f) {
            setStroke(dimensionPixelSize, colorStateList, dimension, typedArray.getDimension(3, gradientState.mStrokeDashGap));
        } else {
            setStroke(dimensionPixelSize, colorStateList);
        }
    }

    private void updateGradientDrawableSolid(TypedArray typedArray) {
        GradientState gradientState = this.mGradientState;
        gradientState.mChangingConfigurations |= typedArray.getChangingConfigurations();
        gradientState.mAttrSolid = typedArray.extractThemeAttrs();
        ColorStateList colorStateList = typedArray.getColorStateList(0);
        if (colorStateList != null) {
            setColor(colorStateList);
        }
    }

    private void updateGradientDrawableGradient(Resources resources, TypedArray typedArray) throws XmlPullParserException {
        float dimension;
        GradientState gradientState = this.mGradientState;
        gradientState.mChangingConfigurations |= typedArray.getChangingConfigurations();
        gradientState.mAttrGradient = typedArray.extractThemeAttrs();
        gradientState.mCenterX = getFloatOrFraction(typedArray, 5, gradientState.mCenterX);
        gradientState.mCenterY = getFloatOrFraction(typedArray, 6, gradientState.mCenterY);
        gradientState.mUseLevel = typedArray.getBoolean(2, gradientState.mUseLevel);
        gradientState.mGradient = typedArray.getInt(4, gradientState.mGradient);
        int i = 0;
        int color = typedArray.getColor(0, 0);
        boolean zHasValue = typedArray.hasValue(8);
        int color2 = typedArray.getColor(8, 0);
        int color3 = typedArray.getColor(1, 0);
        if (zHasValue) {
            gradientState.mGradientColors = new int[3];
            gradientState.mGradientColors[0] = color;
            gradientState.mGradientColors[1] = color2;
            gradientState.mGradientColors[2] = color3;
            gradientState.mPositions = new float[3];
            gradientState.mPositions[0] = 0.0f;
            gradientState.mPositions[1] = gradientState.mCenterX != 0.5f ? gradientState.mCenterX : gradientState.mCenterY;
            gradientState.mPositions[2] = 1.0f;
        } else {
            gradientState.mGradientColors = new int[2];
            gradientState.mGradientColors[0] = color;
            gradientState.mGradientColors[1] = color3;
        }
        if (gradientState.mGradient == 0) {
            int i2 = ((int) typedArray.getFloat(3, gradientState.mAngle)) % 360;
            if (i2 % 45 != 0) {
                throw new XmlPullParserException(typedArray.getPositionDescription() + "<gradient> tag requires 'angle' attribute to be a multiple of 45");
            }
            gradientState.mAngle = i2;
            if (i2 == 0) {
                gradientState.mOrientation = Orientation.LEFT_RIGHT;
                return;
            }
            if (i2 == 45) {
                gradientState.mOrientation = Orientation.BL_TR;
                return;
            }
            if (i2 == 90) {
                gradientState.mOrientation = Orientation.BOTTOM_TOP;
                return;
            }
            if (i2 == 135) {
                gradientState.mOrientation = Orientation.BR_TL;
                return;
            }
            if (i2 == 180) {
                gradientState.mOrientation = Orientation.RIGHT_LEFT;
                return;
            }
            if (i2 == 225) {
                gradientState.mOrientation = Orientation.TR_BL;
                return;
            } else if (i2 == 270) {
                gradientState.mOrientation = Orientation.TOP_BOTTOM;
                return;
            } else {
                if (i2 == 315) {
                    gradientState.mOrientation = Orientation.TL_BR;
                    return;
                }
                return;
            }
        }
        TypedValue typedValuePeekValue = typedArray.peekValue(7);
        if (typedValuePeekValue == null) {
            if (gradientState.mGradient == 1) {
                throw new XmlPullParserException(typedArray.getPositionDescription() + "<gradient> tag requires 'gradientRadius' attribute with radial type");
            }
            return;
        }
        if (typedValuePeekValue.type == 6) {
            dimension = typedValuePeekValue.getFraction(1.0f, 1.0f);
            i = ((typedValuePeekValue.data >> 0) & 15) == 1 ? 2 : 1;
        } else {
            dimension = typedValuePeekValue.type == 5 ? typedValuePeekValue.getDimension(resources.getDisplayMetrics()) : typedValuePeekValue.getFloat();
        }
        gradientState.mGradientRadius = dimension;
        gradientState.mGradientRadiusType = i;
    }

    private void updateGradientDrawableSize(TypedArray typedArray) {
        GradientState gradientState = this.mGradientState;
        gradientState.mChangingConfigurations |= typedArray.getChangingConfigurations();
        gradientState.mAttrSize = typedArray.extractThemeAttrs();
        gradientState.mWidth = typedArray.getDimensionPixelSize(1, gradientState.mWidth);
        gradientState.mHeight = typedArray.getDimensionPixelSize(0, gradientState.mHeight);
    }

    private static float getFloatOrFraction(TypedArray typedArray, int i, float f) {
        TypedValue typedValuePeekValue = typedArray.peekValue(i);
        if (typedValuePeekValue != null) {
            return typedValuePeekValue.type == 6 ? typedValuePeekValue.getFraction(1.0f, 1.0f) : typedValuePeekValue.getFloat();
        }
        return f;
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
    public Insets getOpticalInsets() {
        return this.mGradientState.mOpticalInsets;
    }

    @Override
    public Drawable.ConstantState getConstantState() {
        this.mGradientState.mChangingConfigurations = getChangingConfigurations();
        return this.mGradientState;
    }

    private boolean isOpaqueForState() {
        if (this.mGradientState.mStrokeWidth < 0 || this.mStrokePaint == null || isOpaque(this.mStrokePaint.getColor())) {
            return this.mGradientState.mGradientColors != null || isOpaque(this.mFillPaint.getColor());
        }
        return false;
    }

    @Override
    public void getOutline(Outline outline) {
        float fModulateAlpha;
        GradientState gradientState = this.mGradientState;
        Rect bounds = getBounds();
        float fMin = 0.0f;
        if (!(gradientState.mOpaqueOverShape && (this.mGradientState.mStrokeWidth <= 0 || this.mStrokePaint == null || this.mStrokePaint.getAlpha() == this.mFillPaint.getAlpha()))) {
            fModulateAlpha = 0.0f;
        } else {
            fModulateAlpha = modulateAlpha(this.mFillPaint.getAlpha()) / 255.0f;
        }
        outline.setAlpha(fModulateAlpha);
        switch (gradientState.mShape) {
            case 0:
                if (gradientState.mRadiusArray != null) {
                    buildPathIfDirty();
                    outline.setConvexPath(this.mPath);
                } else {
                    if (gradientState.mRadius > 0.0f) {
                        fMin = Math.min(gradientState.mRadius, Math.min(bounds.width(), bounds.height()) * 0.5f);
                    }
                    outline.setRoundRect(bounds, fMin);
                }
                break;
            case 1:
                outline.setOval(bounds);
                break;
            case 2:
                float strokeWidth = this.mStrokePaint == null ? 1.0E-4f : this.mStrokePaint.getStrokeWidth() * 0.5f;
                float fCenterY = bounds.centerY();
                outline.setRect(bounds.left, (int) Math.floor(fCenterY - strokeWidth), bounds.right, (int) Math.ceil(fCenterY + strokeWidth));
                break;
        }
    }

    @Override
    public Drawable mutate() {
        if (!this.mMutated && super.mutate() == this) {
            this.mGradientState = new GradientState(this.mGradientState, (Resources) null);
            updateLocalState(null);
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
        int mDensity;
        public boolean mDither;
        public int mGradient;
        public int[] mGradientColors;
        float mGradientRadius;
        int mGradientRadiusType;
        public int mHeight;
        public int mInnerRadius;
        public float mInnerRadiusRatio;
        boolean mOpaqueOverBounds;
        boolean mOpaqueOverShape;
        public Insets mOpticalInsets;
        public Orientation mOrientation;
        public Rect mPadding;
        public float[] mPositions;
        public float mRadius;
        public float[] mRadiusArray;
        public int mShape;
        public ColorStateList mSolidColors;
        public ColorStateList mStrokeColors;
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

        public GradientState(Orientation orientation, int[] iArr) {
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
            this.mOpticalInsets = Insets.NONE;
            this.mCenterX = 0.5f;
            this.mCenterY = 0.5f;
            this.mGradientRadius = 0.5f;
            this.mGradientRadiusType = 0;
            this.mUseLevel = false;
            this.mUseLevelForShape = true;
            this.mTint = null;
            this.mTintMode = Drawable.DEFAULT_TINT_MODE;
            this.mDensity = 160;
            this.mOrientation = orientation;
            setGradientColors(iArr);
        }

        public GradientState(GradientState gradientState, Resources resources) {
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
            this.mOpticalInsets = Insets.NONE;
            this.mCenterX = 0.5f;
            this.mCenterY = 0.5f;
            this.mGradientRadius = 0.5f;
            this.mGradientRadiusType = 0;
            this.mUseLevel = false;
            this.mUseLevelForShape = true;
            this.mTint = null;
            this.mTintMode = Drawable.DEFAULT_TINT_MODE;
            this.mDensity = 160;
            this.mChangingConfigurations = gradientState.mChangingConfigurations;
            this.mShape = gradientState.mShape;
            this.mGradient = gradientState.mGradient;
            this.mAngle = gradientState.mAngle;
            this.mOrientation = gradientState.mOrientation;
            this.mSolidColors = gradientState.mSolidColors;
            if (gradientState.mGradientColors != null) {
                this.mGradientColors = (int[]) gradientState.mGradientColors.clone();
            }
            if (gradientState.mPositions != null) {
                this.mPositions = (float[]) gradientState.mPositions.clone();
            }
            this.mStrokeColors = gradientState.mStrokeColors;
            this.mStrokeWidth = gradientState.mStrokeWidth;
            this.mStrokeDashWidth = gradientState.mStrokeDashWidth;
            this.mStrokeDashGap = gradientState.mStrokeDashGap;
            this.mRadius = gradientState.mRadius;
            if (gradientState.mRadiusArray != null) {
                this.mRadiusArray = (float[]) gradientState.mRadiusArray.clone();
            }
            if (gradientState.mPadding != null) {
                this.mPadding = new Rect(gradientState.mPadding);
            }
            this.mWidth = gradientState.mWidth;
            this.mHeight = gradientState.mHeight;
            this.mInnerRadiusRatio = gradientState.mInnerRadiusRatio;
            this.mThicknessRatio = gradientState.mThicknessRatio;
            this.mInnerRadius = gradientState.mInnerRadius;
            this.mThickness = gradientState.mThickness;
            this.mDither = gradientState.mDither;
            this.mOpticalInsets = gradientState.mOpticalInsets;
            this.mCenterX = gradientState.mCenterX;
            this.mCenterY = gradientState.mCenterY;
            this.mGradientRadius = gradientState.mGradientRadius;
            this.mGradientRadiusType = gradientState.mGradientRadiusType;
            this.mUseLevel = gradientState.mUseLevel;
            this.mUseLevelForShape = gradientState.mUseLevelForShape;
            this.mOpaqueOverBounds = gradientState.mOpaqueOverBounds;
            this.mOpaqueOverShape = gradientState.mOpaqueOverShape;
            this.mTint = gradientState.mTint;
            this.mTintMode = gradientState.mTintMode;
            this.mThemeAttrs = gradientState.mThemeAttrs;
            this.mAttrSize = gradientState.mAttrSize;
            this.mAttrGradient = gradientState.mAttrGradient;
            this.mAttrSolid = gradientState.mAttrSolid;
            this.mAttrStroke = gradientState.mAttrStroke;
            this.mAttrCorners = gradientState.mAttrCorners;
            this.mAttrPadding = gradientState.mAttrPadding;
            this.mDensity = Drawable.resolveDensity(resources, gradientState.mDensity);
            if (gradientState.mDensity != this.mDensity) {
                applyDensityScaling(gradientState.mDensity, this.mDensity);
            }
        }

        public final void setDensity(int i) {
            if (this.mDensity != i) {
                int i2 = this.mDensity;
                this.mDensity = i;
                applyDensityScaling(i2, i);
            }
        }

        private void applyDensityScaling(int i, int i2) {
            if (this.mInnerRadius > 0) {
                this.mInnerRadius = Drawable.scaleFromDensity(this.mInnerRadius, i, i2, true);
            }
            if (this.mThickness > 0) {
                this.mThickness = Drawable.scaleFromDensity(this.mThickness, i, i2, true);
            }
            if (this.mOpticalInsets != Insets.NONE) {
                this.mOpticalInsets = Insets.of(Drawable.scaleFromDensity(this.mOpticalInsets.left, i, i2, true), Drawable.scaleFromDensity(this.mOpticalInsets.top, i, i2, true), Drawable.scaleFromDensity(this.mOpticalInsets.right, i, i2, true), Drawable.scaleFromDensity(this.mOpticalInsets.bottom, i, i2, true));
            }
            if (this.mPadding != null) {
                this.mPadding.left = Drawable.scaleFromDensity(this.mPadding.left, i, i2, false);
                this.mPadding.top = Drawable.scaleFromDensity(this.mPadding.top, i, i2, false);
                this.mPadding.right = Drawable.scaleFromDensity(this.mPadding.right, i, i2, false);
                this.mPadding.bottom = Drawable.scaleFromDensity(this.mPadding.bottom, i, i2, false);
            }
            if (this.mRadius > 0.0f) {
                this.mRadius = Drawable.scaleFromDensity(this.mRadius, i, i2);
            }
            if (this.mRadiusArray != null) {
                this.mRadiusArray[0] = Drawable.scaleFromDensity((int) this.mRadiusArray[0], i, i2, true);
                this.mRadiusArray[1] = Drawable.scaleFromDensity((int) this.mRadiusArray[1], i, i2, true);
                this.mRadiusArray[2] = Drawable.scaleFromDensity((int) this.mRadiusArray[2], i, i2, true);
                this.mRadiusArray[3] = Drawable.scaleFromDensity((int) this.mRadiusArray[3], i, i2, true);
            }
            if (this.mStrokeWidth > 0) {
                this.mStrokeWidth = Drawable.scaleFromDensity(this.mStrokeWidth, i, i2, true);
            }
            if (this.mStrokeDashWidth > 0.0f) {
                this.mStrokeDashWidth = Drawable.scaleFromDensity(this.mStrokeDashGap, i, i2);
            }
            if (this.mStrokeDashGap > 0.0f) {
                this.mStrokeDashGap = Drawable.scaleFromDensity(this.mStrokeDashGap, i, i2);
            }
            if (this.mGradientRadiusType == 0) {
                this.mGradientRadius = Drawable.scaleFromDensity(this.mGradientRadius, i, i2);
            }
            if (this.mWidth > 0) {
                this.mWidth = Drawable.scaleFromDensity(this.mWidth, i, i2, true);
            }
            if (this.mHeight > 0) {
                this.mHeight = Drawable.scaleFromDensity(this.mHeight, i, i2, true);
            }
        }

        @Override
        public boolean canApplyTheme() {
            return (this.mThemeAttrs == null && this.mAttrSize == null && this.mAttrGradient == null && this.mAttrSolid == null && this.mAttrStroke == null && this.mAttrCorners == null && this.mAttrPadding == null && (this.mTint == null || !this.mTint.canApplyTheme()) && ((this.mStrokeColors == null || !this.mStrokeColors.canApplyTheme()) && ((this.mSolidColors == null || !this.mSolidColors.canApplyTheme()) && !super.canApplyTheme()))) ? false : true;
        }

        @Override
        public Drawable newDrawable() {
            return new GradientDrawable(this, null);
        }

        @Override
        public Drawable newDrawable(Resources resources) {
            GradientState gradientState;
            if (Drawable.resolveDensity(resources, this.mDensity) != this.mDensity) {
                gradientState = new GradientState(this, resources);
            } else {
                gradientState = this;
            }
            return new GradientDrawable(gradientState, resources);
        }

        @Override
        public int getChangingConfigurations() {
            return this.mChangingConfigurations | (this.mStrokeColors != null ? this.mStrokeColors.getChangingConfigurations() : 0) | (this.mSolidColors != null ? this.mSolidColors.getChangingConfigurations() : 0) | (this.mTint != null ? this.mTint.getChangingConfigurations() : 0);
        }

        public void setShape(int i) {
            this.mShape = i;
            computeOpacity();
        }

        public void setGradientType(int i) {
            this.mGradient = i;
        }

        public void setGradientCenter(float f, float f2) {
            this.mCenterX = f;
            this.mCenterY = f2;
        }

        public void setGradientColors(int[] iArr) {
            this.mGradientColors = iArr;
            this.mSolidColors = null;
            computeOpacity();
        }

        public void setSolidColors(ColorStateList colorStateList) {
            this.mGradientColors = null;
            this.mSolidColors = colorStateList;
            computeOpacity();
        }

        private void computeOpacity() {
            boolean z = false;
            this.mOpaqueOverBounds = false;
            this.mOpaqueOverShape = false;
            if (this.mGradientColors != null) {
                for (int i = 0; i < this.mGradientColors.length; i++) {
                    if (!GradientDrawable.isOpaque(this.mGradientColors[i])) {
                        return;
                    }
                }
            }
            if (this.mGradientColors == null && this.mSolidColors == null) {
                return;
            }
            this.mOpaqueOverShape = true;
            if (this.mShape == 0 && this.mRadius <= 0.0f && this.mRadiusArray == null) {
                z = true;
            }
            this.mOpaqueOverBounds = z;
        }

        public void setStroke(int i, ColorStateList colorStateList, float f, float f2) {
            this.mStrokeWidth = i;
            this.mStrokeColors = colorStateList;
            this.mStrokeDashWidth = f;
            this.mStrokeDashGap = f2;
            computeOpacity();
        }

        public void setCornerRadius(float f) {
            if (f < 0.0f) {
                f = 0.0f;
            }
            this.mRadius = f;
            this.mRadiusArray = null;
            computeOpacity();
        }

        public void setCornerRadii(float[] fArr) {
            this.mRadiusArray = fArr;
            if (fArr == null) {
                this.mRadius = 0.0f;
            }
            computeOpacity();
        }

        public void setSize(int i, int i2) {
            this.mWidth = i;
            this.mHeight = i2;
        }

        public void setGradientRadius(float f, int i) {
            this.mGradientRadius = f;
            this.mGradientRadiusType = i;
        }
    }

    static boolean isOpaque(int i) {
        return ((i >> 24) & 255) == 255;
    }

    private GradientDrawable(GradientState gradientState, Resources resources) {
        this.mFillPaint = new Paint(1);
        this.mAlpha = 255;
        this.mPath = new Path();
        this.mRect = new RectF();
        this.mPathIsDirty = true;
        this.mGradientState = gradientState;
        updateLocalState(resources);
    }

    private void updateLocalState(Resources resources) {
        GradientState gradientState = this.mGradientState;
        if (gradientState.mSolidColors != null) {
            this.mFillPaint.setColor(gradientState.mSolidColors.getColorForState(getState(), 0));
        } else if (gradientState.mGradientColors == null) {
            this.mFillPaint.setColor(0);
        } else {
            this.mFillPaint.setColor(-16777216);
        }
        this.mPadding = gradientState.mPadding;
        if (gradientState.mStrokeWidth >= 0) {
            this.mStrokePaint = new Paint(1);
            this.mStrokePaint.setStyle(Paint.Style.STROKE);
            this.mStrokePaint.setStrokeWidth(gradientState.mStrokeWidth);
            if (gradientState.mStrokeColors != null) {
                this.mStrokePaint.setColor(gradientState.mStrokeColors.getColorForState(getState(), 0));
            }
            if (gradientState.mStrokeDashWidth != 0.0f) {
                this.mStrokePaint.setPathEffect(new DashPathEffect(new float[]{gradientState.mStrokeDashWidth, gradientState.mStrokeDashGap}, 0.0f));
            }
        }
        this.mTintFilter = updateTintFilter(this.mTintFilter, gradientState.mTint, gradientState.mTintMode);
        this.mGradientIsDirty = true;
        gradientState.computeOpacity();
    }
}
