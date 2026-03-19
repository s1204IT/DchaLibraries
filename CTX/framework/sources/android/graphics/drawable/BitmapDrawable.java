package android.graphics.drawable;

import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.ImageDecoder;
import android.graphics.Insets;
import android.graphics.Matrix;
import android.graphics.Outline;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.Rect;
import android.graphics.Shader;
import android.graphics.Xfermode;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import com.android.internal.R;
import java.io.FileInputStream;
import java.io.InputStream;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

public class BitmapDrawable extends Drawable {
    private static final int DEFAULT_PAINT_FLAGS = 6;
    private static final int TILE_MODE_CLAMP = 0;
    private static final int TILE_MODE_DISABLED = -1;
    private static final int TILE_MODE_MIRROR = 2;
    private static final int TILE_MODE_REPEAT = 1;
    private static final int TILE_MODE_UNDEFINED = -2;
    private int mBitmapHeight;
    private BitmapState mBitmapState;
    private int mBitmapWidth;
    private final Rect mDstRect;
    private boolean mDstRectAndInsetsDirty;
    private Matrix mMirrorMatrix;
    private boolean mMutated;
    private Insets mOpticalInsets;
    private int mTargetDensity;
    private PorterDuffColorFilter mTintFilter;

    @Deprecated
    public BitmapDrawable() {
        this.mDstRect = new Rect();
        this.mTargetDensity = 160;
        this.mDstRectAndInsetsDirty = true;
        this.mOpticalInsets = Insets.NONE;
        init(new BitmapState((Bitmap) null), null);
    }

    @Deprecated
    public BitmapDrawable(Resources resources) {
        this.mDstRect = new Rect();
        this.mTargetDensity = 160;
        this.mDstRectAndInsetsDirty = true;
        this.mOpticalInsets = Insets.NONE;
        init(new BitmapState((Bitmap) null), resources);
    }

    @Deprecated
    public BitmapDrawable(Bitmap bitmap) {
        this.mDstRect = new Rect();
        this.mTargetDensity = 160;
        this.mDstRectAndInsetsDirty = true;
        this.mOpticalInsets = Insets.NONE;
        init(new BitmapState(bitmap), null);
    }

    public BitmapDrawable(Resources resources, Bitmap bitmap) {
        this.mDstRect = new Rect();
        this.mTargetDensity = 160;
        this.mDstRectAndInsetsDirty = true;
        this.mOpticalInsets = Insets.NONE;
        init(new BitmapState(bitmap), resources);
    }

    @Deprecated
    public BitmapDrawable(String str) {
        this((Resources) null, str);
    }

    public BitmapDrawable(Resources resources, String str) throws Throwable {
        String str2;
        StringBuilder sb;
        FileInputStream fileInputStream;
        Throwable th;
        Throwable th2;
        this.mDstRect = new Rect();
        this.mTargetDensity = 160;
        this.mDstRectAndInsetsDirty = true;
        this.mOpticalInsets = Insets.NONE;
        Bitmap bitmap = null;
        try {
            fileInputStream = new FileInputStream(str);
        } catch (Exception e) {
        } catch (Throwable th3) {
            th = th3;
        }
        try {
            Bitmap bitmapDecodeBitmap = ImageDecoder.decodeBitmap(ImageDecoder.createSource(resources, fileInputStream), new ImageDecoder.OnHeaderDecodedListener() {
                @Override
                public final void onHeaderDecoded(ImageDecoder imageDecoder, ImageDecoder.ImageInfo imageInfo, ImageDecoder.Source source) {
                    imageDecoder.setAllocator(1);
                }
            });
            try {
                $closeResource(null, fileInputStream);
                init(new BitmapState(bitmapDecodeBitmap), resources);
            } catch (Exception e2) {
                bitmap = bitmapDecodeBitmap;
                init(new BitmapState(bitmap), resources);
                if (this.mBitmapState.mBitmap != null) {
                    return;
                }
                str2 = "BitmapDrawable";
                sb = new StringBuilder();
            } catch (Throwable th4) {
                th = th4;
                bitmap = bitmapDecodeBitmap;
                init(new BitmapState(bitmap), resources);
                if (this.mBitmapState.mBitmap == null) {
                    Log.w("BitmapDrawable", "BitmapDrawable cannot decode " + str);
                }
                throw th;
            }
            if (this.mBitmapState.mBitmap == null) {
                str2 = "BitmapDrawable";
                sb = new StringBuilder();
                sb.append("BitmapDrawable cannot decode ");
                sb.append(str);
                Log.w(str2, sb.toString());
            }
        } catch (Throwable th5) {
            try {
                throw th5;
            } catch (Throwable th6) {
                th = th5;
                th2 = th6;
                $closeResource(th, fileInputStream);
                throw th2;
            }
        }
    }

    private static void $closeResource(Throwable th, AutoCloseable autoCloseable) throws Exception {
        if (th == null) {
            autoCloseable.close();
            return;
        }
        try {
            autoCloseable.close();
        } catch (Throwable th2) {
            th.addSuppressed(th2);
        }
    }

    @Deprecated
    public BitmapDrawable(InputStream inputStream) {
        this((Resources) null, inputStream);
    }

    public BitmapDrawable(Resources resources, InputStream inputStream) {
        String str;
        StringBuilder sb;
        this.mDstRect = new Rect();
        this.mTargetDensity = 160;
        this.mDstRectAndInsetsDirty = true;
        this.mOpticalInsets = Insets.NONE;
        try {
            init(new BitmapState(ImageDecoder.decodeBitmap(ImageDecoder.createSource(resources, inputStream), new ImageDecoder.OnHeaderDecodedListener() {
                @Override
                public final void onHeaderDecoded(ImageDecoder imageDecoder, ImageDecoder.ImageInfo imageInfo, ImageDecoder.Source source) {
                    imageDecoder.setAllocator(1);
                }
            })), resources);
        } catch (Exception e) {
            init(new BitmapState((Bitmap) null), resources);
            if (this.mBitmapState.mBitmap == null) {
                str = "BitmapDrawable";
                sb = new StringBuilder();
            } else {
                return;
            }
        } catch (Throwable th) {
            init(new BitmapState((Bitmap) null), resources);
            if (this.mBitmapState.mBitmap == null) {
                Log.w("BitmapDrawable", "BitmapDrawable cannot decode " + inputStream);
            }
            throw th;
        }
        if (this.mBitmapState.mBitmap == null) {
            str = "BitmapDrawable";
            sb = new StringBuilder();
            sb.append("BitmapDrawable cannot decode ");
            sb.append(inputStream);
            Log.w(str, sb.toString());
        }
    }

    public final Paint getPaint() {
        return this.mBitmapState.mPaint;
    }

    public final Bitmap getBitmap() {
        return this.mBitmapState.mBitmap;
    }

    private void computeBitmapSize() {
        Bitmap bitmap = this.mBitmapState.mBitmap;
        if (bitmap != null) {
            this.mBitmapWidth = bitmap.getScaledWidth(this.mTargetDensity);
            this.mBitmapHeight = bitmap.getScaledHeight(this.mTargetDensity);
        } else {
            this.mBitmapHeight = -1;
            this.mBitmapWidth = -1;
        }
    }

    public void setBitmap(Bitmap bitmap) {
        if (this.mBitmapState.mBitmap != bitmap) {
            this.mBitmapState.mBitmap = bitmap;
            computeBitmapSize();
            invalidateSelf();
        }
    }

    public void setTargetDensity(Canvas canvas) {
        setTargetDensity(canvas.getDensity());
    }

    public void setTargetDensity(DisplayMetrics displayMetrics) {
        setTargetDensity(displayMetrics.densityDpi);
    }

    public void setTargetDensity(int i) {
        if (this.mTargetDensity != i) {
            if (i == 0) {
                i = 160;
            }
            this.mTargetDensity = i;
            if (this.mBitmapState.mBitmap != null) {
                computeBitmapSize();
            }
            invalidateSelf();
        }
    }

    public int getGravity() {
        return this.mBitmapState.mGravity;
    }

    public void setGravity(int i) {
        if (this.mBitmapState.mGravity != i) {
            this.mBitmapState.mGravity = i;
            this.mDstRectAndInsetsDirty = true;
            invalidateSelf();
        }
    }

    public void setMipMap(boolean z) {
        if (this.mBitmapState.mBitmap != null) {
            this.mBitmapState.mBitmap.setHasMipMap(z);
            invalidateSelf();
        }
    }

    public boolean hasMipMap() {
        return this.mBitmapState.mBitmap != null && this.mBitmapState.mBitmap.hasMipMap();
    }

    public void setAntiAlias(boolean z) {
        this.mBitmapState.mPaint.setAntiAlias(z);
        invalidateSelf();
    }

    public boolean hasAntiAlias() {
        return this.mBitmapState.mPaint.isAntiAlias();
    }

    @Override
    public void setFilterBitmap(boolean z) {
        this.mBitmapState.mPaint.setFilterBitmap(z);
        invalidateSelf();
    }

    @Override
    public boolean isFilterBitmap() {
        return this.mBitmapState.mPaint.isFilterBitmap();
    }

    @Override
    public void setDither(boolean z) {
        this.mBitmapState.mPaint.setDither(z);
        invalidateSelf();
    }

    public Shader.TileMode getTileModeX() {
        return this.mBitmapState.mTileModeX;
    }

    public Shader.TileMode getTileModeY() {
        return this.mBitmapState.mTileModeY;
    }

    public void setTileModeX(Shader.TileMode tileMode) {
        setTileModeXY(tileMode, this.mBitmapState.mTileModeY);
    }

    public final void setTileModeY(Shader.TileMode tileMode) {
        setTileModeXY(this.mBitmapState.mTileModeX, tileMode);
    }

    public void setTileModeXY(Shader.TileMode tileMode, Shader.TileMode tileMode2) {
        BitmapState bitmapState = this.mBitmapState;
        if (bitmapState.mTileModeX != tileMode || bitmapState.mTileModeY != tileMode2) {
            bitmapState.mTileModeX = tileMode;
            bitmapState.mTileModeY = tileMode2;
            bitmapState.mRebuildShader = true;
            this.mDstRectAndInsetsDirty = true;
            invalidateSelf();
        }
    }

    @Override
    public void setAutoMirrored(boolean z) {
        if (this.mBitmapState.mAutoMirrored != z) {
            this.mBitmapState.mAutoMirrored = z;
            invalidateSelf();
        }
    }

    @Override
    public final boolean isAutoMirrored() {
        return this.mBitmapState.mAutoMirrored;
    }

    @Override
    public int getChangingConfigurations() {
        return super.getChangingConfigurations() | this.mBitmapState.getChangingConfigurations();
    }

    private boolean needMirroring() {
        return isAutoMirrored() && getLayoutDirection() == 1;
    }

    @Override
    protected void onBoundsChange(Rect rect) {
        this.mDstRectAndInsetsDirty = true;
        Bitmap bitmap = this.mBitmapState.mBitmap;
        Shader shader = this.mBitmapState.mPaint.getShader();
        if (bitmap != null && shader != null) {
            updateShaderMatrix(bitmap, this.mBitmapState.mPaint, shader, needMirroring());
        }
    }

    @Override
    public void draw(Canvas canvas) {
        int alpha;
        Bitmap bitmap = this.mBitmapState.mBitmap;
        if (bitmap == null) {
            return;
        }
        BitmapState bitmapState = this.mBitmapState;
        Paint paint = bitmapState.mPaint;
        boolean z = false;
        if (bitmapState.mRebuildShader) {
            Shader.TileMode tileMode = bitmapState.mTileModeX;
            Shader.TileMode tileMode2 = bitmapState.mTileModeY;
            if (tileMode == null && tileMode2 == null) {
                paint.setShader(null);
            } else {
                if (tileMode == null) {
                    tileMode = Shader.TileMode.CLAMP;
                }
                if (tileMode2 == null) {
                    tileMode2 = Shader.TileMode.CLAMP;
                }
                paint.setShader(new BitmapShader(bitmap, tileMode, tileMode2));
            }
            bitmapState.mRebuildShader = false;
        }
        if (bitmapState.mBaseAlpha != 1.0f) {
            Paint paint2 = getPaint();
            alpha = paint2.getAlpha();
            paint2.setAlpha((int) ((alpha * bitmapState.mBaseAlpha) + 0.5f));
        } else {
            alpha = -1;
        }
        if (this.mTintFilter != null && paint.getColorFilter() == null) {
            paint.setColorFilter(this.mTintFilter);
            z = true;
        }
        updateDstRectAndInsetsIfDirty();
        Shader shader = paint.getShader();
        boolean zNeedMirroring = needMirroring();
        if (shader == null) {
            if (zNeedMirroring) {
                canvas.save();
                canvas.translate(this.mDstRect.right - this.mDstRect.left, 0.0f);
                canvas.scale(-1.0f, 1.0f);
            }
            canvas.drawBitmap(bitmap, (Rect) null, this.mDstRect, paint);
            if (zNeedMirroring) {
                canvas.restore();
            }
        } else {
            updateShaderMatrix(bitmap, paint, shader, zNeedMirroring);
            canvas.drawRect(this.mDstRect, paint);
        }
        if (z) {
            paint.setColorFilter(null);
        }
        if (alpha >= 0) {
            paint.setAlpha(alpha);
        }
    }

    private void updateShaderMatrix(Bitmap bitmap, Paint paint, Shader shader, boolean z) {
        int density = bitmap.getDensity();
        int i = this.mTargetDensity;
        boolean z2 = (density == 0 || density == i) ? false : true;
        if (z2 || z) {
            Matrix orCreateMirrorMatrix = getOrCreateMirrorMatrix();
            orCreateMirrorMatrix.reset();
            if (z) {
                orCreateMirrorMatrix.setTranslate(this.mDstRect.right - this.mDstRect.left, 0.0f);
                orCreateMirrorMatrix.setScale(-1.0f, 1.0f);
            }
            if (z2) {
                float f = i / density;
                orCreateMirrorMatrix.postScale(f, f);
            }
            shader.setLocalMatrix(orCreateMirrorMatrix);
        } else {
            this.mMirrorMatrix = null;
            shader.setLocalMatrix(Matrix.IDENTITY_MATRIX);
        }
        paint.setShader(shader);
    }

    private Matrix getOrCreateMirrorMatrix() {
        if (this.mMirrorMatrix == null) {
            this.mMirrorMatrix = new Matrix();
        }
        return this.mMirrorMatrix;
    }

    private void updateDstRectAndInsetsIfDirty() {
        if (this.mDstRectAndInsetsDirty) {
            if (this.mBitmapState.mTileModeX == null && this.mBitmapState.mTileModeY == null) {
                Rect bounds = getBounds();
                Gravity.apply(this.mBitmapState.mGravity, this.mBitmapWidth, this.mBitmapHeight, bounds, this.mDstRect, getLayoutDirection());
                this.mOpticalInsets = Insets.of(this.mDstRect.left - bounds.left, this.mDstRect.top - bounds.top, bounds.right - this.mDstRect.right, bounds.bottom - this.mDstRect.bottom);
            } else {
                copyBounds(this.mDstRect);
                this.mOpticalInsets = Insets.NONE;
            }
        }
        this.mDstRectAndInsetsDirty = false;
    }

    @Override
    public Insets getOpticalInsets() {
        updateDstRectAndInsetsIfDirty();
        return this.mOpticalInsets;
    }

    @Override
    public void getOutline(Outline outline) {
        updateDstRectAndInsetsIfDirty();
        outline.setRect(this.mDstRect);
        outline.setAlpha(this.mBitmapState.mBitmap != null && !this.mBitmapState.mBitmap.hasAlpha() ? getAlpha() / 255.0f : 0.0f);
    }

    @Override
    public void setAlpha(int i) {
        if (i != this.mBitmapState.mPaint.getAlpha()) {
            this.mBitmapState.mPaint.setAlpha(i);
            invalidateSelf();
        }
    }

    @Override
    public int getAlpha() {
        return this.mBitmapState.mPaint.getAlpha();
    }

    @Override
    public void setColorFilter(ColorFilter colorFilter) {
        this.mBitmapState.mPaint.setColorFilter(colorFilter);
        invalidateSelf();
    }

    @Override
    public ColorFilter getColorFilter() {
        return this.mBitmapState.mPaint.getColorFilter();
    }

    @Override
    public void setTintList(ColorStateList colorStateList) {
        BitmapState bitmapState = this.mBitmapState;
        if (bitmapState.mTint != colorStateList) {
            bitmapState.mTint = colorStateList;
            this.mTintFilter = updateTintFilter(this.mTintFilter, colorStateList, this.mBitmapState.mTintMode);
            invalidateSelf();
        }
    }

    @Override
    public void setTintMode(PorterDuff.Mode mode) {
        BitmapState bitmapState = this.mBitmapState;
        if (bitmapState.mTintMode != mode) {
            bitmapState.mTintMode = mode;
            this.mTintFilter = updateTintFilter(this.mTintFilter, this.mBitmapState.mTint, mode);
            invalidateSelf();
        }
    }

    public ColorStateList getTint() {
        return this.mBitmapState.mTint;
    }

    public PorterDuff.Mode getTintMode() {
        return this.mBitmapState.mTintMode;
    }

    @Override
    public void setXfermode(Xfermode xfermode) {
        this.mBitmapState.mPaint.setXfermode(xfermode);
        invalidateSelf();
    }

    @Override
    public Drawable mutate() {
        if (!this.mMutated && super.mutate() == this) {
            this.mBitmapState = new BitmapState(this.mBitmapState);
            this.mMutated = true;
        }
        return this;
    }

    @Override
    public void clearMutated() {
        super.clearMutated();
        this.mMutated = false;
    }

    @Override
    protected boolean onStateChange(int[] iArr) {
        BitmapState bitmapState = this.mBitmapState;
        if (bitmapState.mTint != null && bitmapState.mTintMode != null) {
            this.mTintFilter = updateTintFilter(this.mTintFilter, bitmapState.mTint, bitmapState.mTintMode);
            return true;
        }
        return false;
    }

    @Override
    public boolean isStateful() {
        return (this.mBitmapState.mTint != null && this.mBitmapState.mTint.isStateful()) || super.isStateful();
    }

    @Override
    public boolean hasFocusStateSpecified() {
        return this.mBitmapState.mTint != null && this.mBitmapState.mTint.hasFocusStateSpecified();
    }

    @Override
    public void inflate(Resources resources, XmlPullParser xmlPullParser, AttributeSet attributeSet, Resources.Theme theme) throws Throwable {
        super.inflate(resources, xmlPullParser, attributeSet, theme);
        TypedArray typedArrayObtainAttributes = obtainAttributes(resources, theme, attributeSet, R.styleable.BitmapDrawable);
        updateStateFromTypedArray(typedArrayObtainAttributes, this.mSrcDensityOverride);
        verifyRequiredAttributes(typedArrayObtainAttributes);
        typedArrayObtainAttributes.recycle();
        updateLocalState(resources);
    }

    private void verifyRequiredAttributes(TypedArray typedArray) throws XmlPullParserException {
        BitmapState bitmapState = this.mBitmapState;
        if (bitmapState.mBitmap == null) {
            if (bitmapState.mThemeAttrs == null || bitmapState.mThemeAttrs[1] == 0) {
                throw new XmlPullParserException(typedArray.getPositionDescription() + ": <bitmap> requires a valid 'src' attribute");
            }
        }
    }

    private void updateStateFromTypedArray(TypedArray typedArray, int i) throws Throwable {
        int i2;
        Bitmap bitmapDecodeBitmap;
        InputStream inputStreamOpenRawResource;
        Throwable th;
        Resources resources = typedArray.getResources();
        BitmapState bitmapState = this.mBitmapState;
        bitmapState.mChangingConfigurations |= typedArray.getChangingConfigurations();
        bitmapState.mThemeAttrs = typedArray.extractThemeAttrs();
        bitmapState.mSrcDensityOverride = i;
        bitmapState.mTargetDensity = Drawable.resolveDensity(resources, 0);
        int resourceId = typedArray.getResourceId(1, 0);
        if (resourceId != 0) {
            TypedValue typedValue = new TypedValue();
            resources.getValueForDensity(resourceId, i, typedValue, true);
            if (i > 0 && typedValue.density > 0 && typedValue.density != 65535) {
                if (typedValue.density == i) {
                    typedValue.density = resources.getDisplayMetrics().densityDpi;
                } else {
                    typedValue.density = (typedValue.density * resources.getDisplayMetrics().densityDpi) / i;
                }
            }
            if (typedValue.density != 0) {
                if (typedValue.density != 65535) {
                    i2 = typedValue.density;
                } else {
                    i2 = 0;
                }
            } else {
                i2 = 160;
            }
            try {
                inputStreamOpenRawResource = resources.openRawResource(resourceId, typedValue);
            } catch (Exception e) {
                bitmapDecodeBitmap = null;
            }
            try {
                bitmapDecodeBitmap = ImageDecoder.decodeBitmap(ImageDecoder.createSource(resources, inputStreamOpenRawResource, i2), new ImageDecoder.OnHeaderDecodedListener() {
                    @Override
                    public final void onHeaderDecoded(ImageDecoder imageDecoder, ImageDecoder.ImageInfo imageInfo, ImageDecoder.Source source) {
                        imageDecoder.setAllocator(1);
                    }
                });
                if (inputStreamOpenRawResource != null) {
                    try {
                        $closeResource(null, inputStreamOpenRawResource);
                    } catch (Exception e2) {
                    }
                }
                if (bitmapDecodeBitmap != null) {
                    throw new XmlPullParserException(typedArray.getPositionDescription() + ": <bitmap> requires a valid 'src' attribute");
                }
                bitmapState.mBitmap = bitmapDecodeBitmap;
            } catch (Throwable th2) {
                th = th2;
                th = null;
                if (inputStreamOpenRawResource != null) {
                }
            }
        }
        setMipMap(typedArray.getBoolean(8, bitmapState.mBitmap != null ? bitmapState.mBitmap.hasMipMap() : false));
        bitmapState.mAutoMirrored = typedArray.getBoolean(9, bitmapState.mAutoMirrored);
        bitmapState.mBaseAlpha = typedArray.getFloat(7, bitmapState.mBaseAlpha);
        int i3 = typedArray.getInt(10, -1);
        if (i3 != -1) {
            bitmapState.mTintMode = Drawable.parseTintMode(i3, PorterDuff.Mode.SRC_IN);
        }
        ColorStateList colorStateList = typedArray.getColorStateList(5);
        if (colorStateList != null) {
            bitmapState.mTint = colorStateList;
        }
        Paint paint = this.mBitmapState.mPaint;
        paint.setAntiAlias(typedArray.getBoolean(2, paint.isAntiAlias()));
        paint.setFilterBitmap(typedArray.getBoolean(3, paint.isFilterBitmap()));
        paint.setDither(typedArray.getBoolean(4, paint.isDither()));
        setGravity(typedArray.getInt(0, bitmapState.mGravity));
        int i4 = typedArray.getInt(6, -2);
        if (i4 != -2) {
            Shader.TileMode tileMode = parseTileMode(i4);
            setTileModeXY(tileMode, tileMode);
        }
        int i5 = typedArray.getInt(11, -2);
        if (i5 != -2) {
            setTileModeX(parseTileMode(i5));
        }
        int i6 = typedArray.getInt(12, -2);
        if (i6 != -2) {
            setTileModeY(parseTileMode(i6));
        }
    }

    @Override
    public void applyTheme(Resources.Theme theme) {
        super.applyTheme(theme);
        BitmapState bitmapState = this.mBitmapState;
        if (bitmapState == null) {
            return;
        }
        if (bitmapState.mThemeAttrs != null) {
            TypedArray typedArrayResolveAttributes = theme.resolveAttributes(bitmapState.mThemeAttrs, R.styleable.BitmapDrawable);
            try {
                try {
                    updateStateFromTypedArray(typedArrayResolveAttributes, bitmapState.mSrcDensityOverride);
                } catch (XmlPullParserException e) {
                    rethrowAsRuntimeException(e);
                }
            } finally {
                typedArrayResolveAttributes.recycle();
            }
        }
        if (bitmapState.mTint != null && bitmapState.mTint.canApplyTheme()) {
            bitmapState.mTint = bitmapState.mTint.obtainForTheme(theme);
        }
        updateLocalState(theme.getResources());
    }

    private static Shader.TileMode parseTileMode(int i) {
        switch (i) {
            case 0:
                return Shader.TileMode.CLAMP;
            case 1:
                return Shader.TileMode.REPEAT;
            case 2:
                return Shader.TileMode.MIRROR;
            default:
                return null;
        }
    }

    @Override
    public boolean canApplyTheme() {
        return this.mBitmapState != null && this.mBitmapState.canApplyTheme();
    }

    @Override
    public int getIntrinsicWidth() {
        return this.mBitmapWidth;
    }

    @Override
    public int getIntrinsicHeight() {
        return this.mBitmapHeight;
    }

    @Override
    public int getOpacity() {
        Bitmap bitmap;
        return (this.mBitmapState.mGravity == 119 && (bitmap = this.mBitmapState.mBitmap) != null && !bitmap.hasAlpha() && this.mBitmapState.mPaint.getAlpha() >= 255) ? -1 : -3;
    }

    @Override
    public final Drawable.ConstantState getConstantState() {
        this.mBitmapState.mChangingConfigurations |= getChangingConfigurations();
        return this.mBitmapState;
    }

    static final class BitmapState extends Drawable.ConstantState {
        boolean mAutoMirrored;
        float mBaseAlpha;
        Bitmap mBitmap;
        int mChangingConfigurations;
        int mGravity;
        final Paint mPaint;
        boolean mRebuildShader;
        int mSrcDensityOverride;
        int mTargetDensity;
        int[] mThemeAttrs;
        Shader.TileMode mTileModeX;
        Shader.TileMode mTileModeY;
        ColorStateList mTint;
        PorterDuff.Mode mTintMode;

        BitmapState(Bitmap bitmap) {
            this.mThemeAttrs = null;
            this.mBitmap = null;
            this.mTint = null;
            this.mTintMode = Drawable.DEFAULT_TINT_MODE;
            this.mGravity = 119;
            this.mBaseAlpha = 1.0f;
            this.mTileModeX = null;
            this.mTileModeY = null;
            this.mSrcDensityOverride = 0;
            this.mTargetDensity = 160;
            this.mAutoMirrored = false;
            this.mBitmap = bitmap;
            this.mPaint = new Paint(6);
        }

        BitmapState(BitmapState bitmapState) {
            this.mThemeAttrs = null;
            this.mBitmap = null;
            this.mTint = null;
            this.mTintMode = Drawable.DEFAULT_TINT_MODE;
            this.mGravity = 119;
            this.mBaseAlpha = 1.0f;
            this.mTileModeX = null;
            this.mTileModeY = null;
            this.mSrcDensityOverride = 0;
            this.mTargetDensity = 160;
            this.mAutoMirrored = false;
            this.mBitmap = bitmapState.mBitmap;
            this.mTint = bitmapState.mTint;
            this.mTintMode = bitmapState.mTintMode;
            this.mThemeAttrs = bitmapState.mThemeAttrs;
            this.mChangingConfigurations = bitmapState.mChangingConfigurations;
            this.mGravity = bitmapState.mGravity;
            this.mTileModeX = bitmapState.mTileModeX;
            this.mTileModeY = bitmapState.mTileModeY;
            this.mSrcDensityOverride = bitmapState.mSrcDensityOverride;
            this.mTargetDensity = bitmapState.mTargetDensity;
            this.mBaseAlpha = bitmapState.mBaseAlpha;
            this.mPaint = new Paint(bitmapState.mPaint);
            this.mRebuildShader = bitmapState.mRebuildShader;
            this.mAutoMirrored = bitmapState.mAutoMirrored;
        }

        @Override
        public boolean canApplyTheme() {
            return this.mThemeAttrs != null || (this.mTint != null && this.mTint.canApplyTheme());
        }

        @Override
        public Drawable newDrawable() {
            return new BitmapDrawable(this, null);
        }

        @Override
        public Drawable newDrawable(Resources resources) {
            return new BitmapDrawable(this, resources);
        }

        @Override
        public int getChangingConfigurations() {
            return this.mChangingConfigurations | (this.mTint != null ? this.mTint.getChangingConfigurations() : 0);
        }
    }

    private BitmapDrawable(BitmapState bitmapState, Resources resources) {
        this.mDstRect = new Rect();
        this.mTargetDensity = 160;
        this.mDstRectAndInsetsDirty = true;
        this.mOpticalInsets = Insets.NONE;
        init(bitmapState, resources);
    }

    private void init(BitmapState bitmapState, Resources resources) {
        this.mBitmapState = bitmapState;
        updateLocalState(resources);
        if (this.mBitmapState != null && resources != null) {
            this.mBitmapState.mTargetDensity = this.mTargetDensity;
        }
    }

    private void updateLocalState(Resources resources) {
        this.mTargetDensity = resolveDensity(resources, this.mBitmapState.mTargetDensity);
        this.mTintFilter = updateTintFilter(this.mTintFilter, this.mBitmapState.mTint, this.mBitmapState.mTintMode);
        computeBitmapSize();
    }
}
