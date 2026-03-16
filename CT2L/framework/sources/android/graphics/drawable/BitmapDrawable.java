package android.graphics.drawable;

import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
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
import android.view.Gravity;
import com.android.internal.R;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
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
        this.mBitmapState = new BitmapState((Bitmap) null);
    }

    @Deprecated
    public BitmapDrawable(Resources res) {
        this.mDstRect = new Rect();
        this.mTargetDensity = 160;
        this.mDstRectAndInsetsDirty = true;
        this.mOpticalInsets = Insets.NONE;
        this.mBitmapState = new BitmapState((Bitmap) null);
        this.mBitmapState.mTargetDensity = this.mTargetDensity;
    }

    @Deprecated
    public BitmapDrawable(Bitmap bitmap) {
        this(new BitmapState(bitmap), (Resources) null);
    }

    public BitmapDrawable(Resources res, Bitmap bitmap) {
        this(new BitmapState(bitmap), res);
        this.mBitmapState.mTargetDensity = this.mTargetDensity;
    }

    @Deprecated
    public BitmapDrawable(String filepath) {
        this(new BitmapState(BitmapFactory.decodeFile(filepath)), (Resources) null);
        if (this.mBitmapState.mBitmap == null) {
            Log.w("BitmapDrawable", "BitmapDrawable cannot decode " + filepath);
        }
    }

    public BitmapDrawable(Resources res, String filepath) {
        this(new BitmapState(BitmapFactory.decodeFile(filepath)), (Resources) null);
        this.mBitmapState.mTargetDensity = this.mTargetDensity;
        if (this.mBitmapState.mBitmap == null) {
            Log.w("BitmapDrawable", "BitmapDrawable cannot decode " + filepath);
        }
    }

    @Deprecated
    public BitmapDrawable(InputStream is) {
        this(new BitmapState(BitmapFactory.decodeStream(is)), (Resources) null);
        if (this.mBitmapState.mBitmap == null) {
            Log.w("BitmapDrawable", "BitmapDrawable cannot decode " + is);
        }
    }

    public BitmapDrawable(Resources res, InputStream is) {
        this(new BitmapState(BitmapFactory.decodeStream(is)), (Resources) null);
        this.mBitmapState.mTargetDensity = this.mTargetDensity;
        if (this.mBitmapState.mBitmap == null) {
            Log.w("BitmapDrawable", "BitmapDrawable cannot decode " + is);
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

    private void setBitmap(Bitmap bitmap) {
        if (this.mBitmapState.mBitmap != bitmap) {
            this.mBitmapState.mBitmap = bitmap;
            computeBitmapSize();
            invalidateSelf();
        }
    }

    public void setTargetDensity(Canvas canvas) {
        setTargetDensity(canvas.getDensity());
    }

    public void setTargetDensity(DisplayMetrics metrics) {
        setTargetDensity(metrics.densityDpi);
    }

    public void setTargetDensity(int density) {
        if (this.mTargetDensity != density) {
            if (density == 0) {
                density = 160;
            }
            this.mTargetDensity = density;
            if (this.mBitmapState.mBitmap != null) {
                computeBitmapSize();
            }
            invalidateSelf();
        }
    }

    public int getGravity() {
        return this.mBitmapState.mGravity;
    }

    public void setGravity(int gravity) {
        if (this.mBitmapState.mGravity != gravity) {
            this.mBitmapState.mGravity = gravity;
            this.mDstRectAndInsetsDirty = true;
            invalidateSelf();
        }
    }

    public void setMipMap(boolean mipMap) {
        if (this.mBitmapState.mBitmap != null) {
            this.mBitmapState.mBitmap.setHasMipMap(mipMap);
            invalidateSelf();
        }
    }

    public boolean hasMipMap() {
        return this.mBitmapState.mBitmap != null && this.mBitmapState.mBitmap.hasMipMap();
    }

    public void setAntiAlias(boolean aa) {
        this.mBitmapState.mPaint.setAntiAlias(aa);
        invalidateSelf();
    }

    public boolean hasAntiAlias() {
        return this.mBitmapState.mPaint.isAntiAlias();
    }

    @Override
    public void setFilterBitmap(boolean filter) {
        this.mBitmapState.mPaint.setFilterBitmap(filter);
        invalidateSelf();
    }

    @Override
    public void setDither(boolean dither) {
        this.mBitmapState.mPaint.setDither(dither);
        invalidateSelf();
    }

    public Shader.TileMode getTileModeX() {
        return this.mBitmapState.mTileModeX;
    }

    public Shader.TileMode getTileModeY() {
        return this.mBitmapState.mTileModeY;
    }

    public void setTileModeX(Shader.TileMode mode) {
        setTileModeXY(mode, this.mBitmapState.mTileModeY);
    }

    public final void setTileModeY(Shader.TileMode mode) {
        setTileModeXY(this.mBitmapState.mTileModeX, mode);
    }

    public void setTileModeXY(Shader.TileMode xmode, Shader.TileMode ymode) {
        BitmapState state = this.mBitmapState;
        if (state.mTileModeX != xmode || state.mTileModeY != ymode) {
            state.mTileModeX = xmode;
            state.mTileModeY = ymode;
            state.mRebuildShader = true;
            this.mDstRectAndInsetsDirty = true;
            invalidateSelf();
        }
    }

    @Override
    public void setAutoMirrored(boolean mirrored) {
        if (this.mBitmapState.mAutoMirrored != mirrored) {
            this.mBitmapState.mAutoMirrored = mirrored;
            invalidateSelf();
        }
    }

    @Override
    public final boolean isAutoMirrored() {
        return this.mBitmapState.mAutoMirrored;
    }

    @Override
    public int getChangingConfigurations() {
        return super.getChangingConfigurations() | this.mBitmapState.mChangingConfigurations;
    }

    private boolean needMirroring() {
        return isAutoMirrored() && getLayoutDirection() == 1;
    }

    private void updateMirrorMatrix(float dx) {
        if (this.mMirrorMatrix == null) {
            this.mMirrorMatrix = new Matrix();
        }
        this.mMirrorMatrix.setTranslate(dx, 0.0f);
        this.mMirrorMatrix.preScale(-1.0f, 1.0f);
    }

    @Override
    protected void onBoundsChange(Rect bounds) {
        this.mDstRectAndInsetsDirty = true;
        Shader shader = this.mBitmapState.mPaint.getShader();
        if (shader != null) {
            if (needMirroring()) {
                updateMirrorMatrix(bounds.right - bounds.left);
                shader.setLocalMatrix(this.mMirrorMatrix);
                this.mBitmapState.mPaint.setShader(shader);
            } else if (this.mMirrorMatrix != null) {
                this.mMirrorMatrix = null;
                shader.setLocalMatrix(Matrix.IDENTITY_MATRIX);
                this.mBitmapState.mPaint.setShader(shader);
            }
        }
    }

    @Override
    public void draw(Canvas canvas) {
        int restoreAlpha;
        boolean clearColorFilter;
        Bitmap bitmap = this.mBitmapState.mBitmap;
        if (bitmap != null) {
            BitmapState state = this.mBitmapState;
            Paint paint = state.mPaint;
            if (state.mRebuildShader) {
                Shader.TileMode tmx = state.mTileModeX;
                Shader.TileMode tmy = state.mTileModeY;
                if (tmx == null && tmy == null) {
                    paint.setShader(null);
                } else {
                    if (tmx == null) {
                        tmx = Shader.TileMode.CLAMP;
                    }
                    if (tmy == null) {
                        tmy = Shader.TileMode.CLAMP;
                    }
                    paint.setShader(new BitmapShader(bitmap, tmx, tmy));
                }
                state.mRebuildShader = false;
            }
            if (state.mBaseAlpha != 1.0f) {
                Paint p = getPaint();
                restoreAlpha = p.getAlpha();
                p.setAlpha((int) ((restoreAlpha * state.mBaseAlpha) + 0.5f));
            } else {
                restoreAlpha = -1;
            }
            if (this.mTintFilter != null && paint.getColorFilter() == null) {
                paint.setColorFilter(this.mTintFilter);
                clearColorFilter = true;
            } else {
                clearColorFilter = false;
            }
            updateDstRectAndInsetsIfDirty();
            Shader shader = paint.getShader();
            boolean needMirroring = needMirroring();
            if (shader == null) {
                if (needMirroring) {
                    canvas.save();
                    canvas.translate(this.mDstRect.right - this.mDstRect.left, 0.0f);
                    canvas.scale(-1.0f, 1.0f);
                }
                canvas.drawBitmap(bitmap, (Rect) null, this.mDstRect, paint);
                if (needMirroring) {
                    canvas.restore();
                }
            } else {
                if (needMirroring) {
                    updateMirrorMatrix(this.mDstRect.right - this.mDstRect.left);
                    shader.setLocalMatrix(this.mMirrorMatrix);
                    paint.setShader(shader);
                } else if (this.mMirrorMatrix != null) {
                    this.mMirrorMatrix = null;
                    shader.setLocalMatrix(Matrix.IDENTITY_MATRIX);
                    paint.setShader(shader);
                }
                canvas.drawRect(this.mDstRect, paint);
            }
            if (clearColorFilter) {
                paint.setColorFilter(null);
            }
            if (restoreAlpha >= 0) {
                paint.setAlpha(restoreAlpha);
            }
        }
    }

    private void updateDstRectAndInsetsIfDirty() {
        if (this.mDstRectAndInsetsDirty) {
            if (this.mBitmapState.mTileModeX == null && this.mBitmapState.mTileModeY == null) {
                Rect bounds = getBounds();
                int layoutDirection = getLayoutDirection();
                Gravity.apply(this.mBitmapState.mGravity, this.mBitmapWidth, this.mBitmapHeight, bounds, this.mDstRect, layoutDirection);
                int left = this.mDstRect.left - bounds.left;
                int top = this.mDstRect.top - bounds.top;
                int right = bounds.right - this.mDstRect.right;
                int bottom = bounds.bottom - this.mDstRect.bottom;
                this.mOpticalInsets = Insets.of(left, top, right, bottom);
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
        boolean opaqueOverShape = (this.mBitmapState.mBitmap == null || this.mBitmapState.mBitmap.hasAlpha()) ? false : true;
        outline.setAlpha(opaqueOverShape ? getAlpha() / 255.0f : 0.0f);
    }

    @Override
    public void setAlpha(int alpha) {
        int oldAlpha = this.mBitmapState.mPaint.getAlpha();
        if (alpha != oldAlpha) {
            this.mBitmapState.mPaint.setAlpha(alpha);
            invalidateSelf();
        }
    }

    @Override
    public int getAlpha() {
        return this.mBitmapState.mPaint.getAlpha();
    }

    @Override
    public void setColorFilter(ColorFilter cf) {
        this.mBitmapState.mPaint.setColorFilter(cf);
        invalidateSelf();
    }

    @Override
    public ColorFilter getColorFilter() {
        return this.mBitmapState.mPaint.getColorFilter();
    }

    @Override
    public void setTintList(ColorStateList tint) {
        this.mBitmapState.mTint = tint;
        this.mTintFilter = updateTintFilter(this.mTintFilter, tint, this.mBitmapState.mTintMode);
        invalidateSelf();
    }

    @Override
    public void setTintMode(PorterDuff.Mode tintMode) {
        this.mBitmapState.mTintMode = tintMode;
        this.mTintFilter = updateTintFilter(this.mTintFilter, this.mBitmapState.mTint, tintMode);
        invalidateSelf();
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
    protected boolean onStateChange(int[] stateSet) {
        BitmapState state = this.mBitmapState;
        if (state.mTint == null || state.mTintMode == null) {
            return false;
        }
        this.mTintFilter = updateTintFilter(this.mTintFilter, state.mTint, state.mTintMode);
        return true;
    }

    @Override
    public boolean isStateful() {
        BitmapState s = this.mBitmapState;
        return super.isStateful() || (s.mTint != null && s.mTint.isStateful());
    }

    @Override
    public void inflate(Resources r, XmlPullParser parser, AttributeSet attrs, Resources.Theme theme) throws XmlPullParserException, IOException {
        super.inflate(r, parser, attrs, theme);
        TypedArray a = obtainAttributes(r, theme, attrs, R.styleable.BitmapDrawable);
        updateStateFromTypedArray(a);
        verifyState(a);
        a.recycle();
    }

    private void verifyState(TypedArray a) throws XmlPullParserException {
        BitmapState state = this.mBitmapState;
        if (state.mBitmap == null) {
            throw new XmlPullParserException(a.getPositionDescription() + ": <bitmap> requires a valid src attribute");
        }
    }

    private void updateStateFromTypedArray(TypedArray a) throws XmlPullParserException {
        Resources r = a.getResources();
        BitmapState state = this.mBitmapState;
        state.mChangingConfigurations |= a.getChangingConfigurations();
        state.mThemeAttrs = a.extractThemeAttrs();
        int srcResId = a.getResourceId(1, 0);
        if (srcResId != 0) {
            Bitmap bitmap = BitmapFactory.decodeResource(r, srcResId);
            if (bitmap == null) {
                throw new XmlPullParserException(a.getPositionDescription() + ": <bitmap> requires a valid src attribute");
            }
            state.mBitmap = bitmap;
        }
        state.mTargetDensity = r.getDisplayMetrics().densityDpi;
        boolean defMipMap = state.mBitmap != null ? state.mBitmap.hasMipMap() : false;
        setMipMap(a.getBoolean(8, defMipMap));
        state.mAutoMirrored = a.getBoolean(9, state.mAutoMirrored);
        state.mBaseAlpha = a.getFloat(7, state.mBaseAlpha);
        int tintMode = a.getInt(10, -1);
        if (tintMode != -1) {
            state.mTintMode = Drawable.parseTintMode(tintMode, PorterDuff.Mode.SRC_IN);
        }
        ColorStateList tint = a.getColorStateList(5);
        if (tint != null) {
            state.mTint = tint;
        }
        Paint paint = this.mBitmapState.mPaint;
        paint.setAntiAlias(a.getBoolean(2, paint.isAntiAlias()));
        paint.setFilterBitmap(a.getBoolean(3, paint.isFilterBitmap()));
        paint.setDither(a.getBoolean(4, paint.isDither()));
        setGravity(a.getInt(0, state.mGravity));
        int tileMode = a.getInt(6, -2);
        if (tileMode != -2) {
            Shader.TileMode mode = parseTileMode(tileMode);
            setTileModeXY(mode, mode);
        }
        int tileModeX = a.getInt(11, -2);
        if (tileModeX != -2) {
            setTileModeX(parseTileMode(tileModeX));
        }
        int tileModeY = a.getInt(12, -2);
        if (tileModeY != -2) {
            setTileModeY(parseTileMode(tileModeY));
        }
        initializeWithState(state, r);
    }

    @Override
    public void applyTheme(Resources.Theme t) {
        super.applyTheme(t);
        BitmapState state = this.mBitmapState;
        if (state != null && state.mThemeAttrs != null) {
            TypedArray a = t.resolveAttributes(state.mThemeAttrs, R.styleable.BitmapDrawable);
            try {
                try {
                    updateStateFromTypedArray(a);
                } catch (XmlPullParserException e) {
                    throw new RuntimeException(e);
                }
            } finally {
                a.recycle();
            }
        }
    }

    private static Shader.TileMode parseTileMode(int tileMode) {
        switch (tileMode) {
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
        return (this.mBitmapState == null || this.mBitmapState.mThemeAttrs == null) ? false : true;
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
        this.mBitmapState.mChangingConfigurations = getChangingConfigurations();
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
            this.mTargetDensity = bitmapState.mTargetDensity;
            this.mBaseAlpha = bitmapState.mBaseAlpha;
            this.mPaint = new Paint(bitmapState.mPaint);
            this.mRebuildShader = bitmapState.mRebuildShader;
            this.mAutoMirrored = bitmapState.mAutoMirrored;
        }

        @Override
        public boolean canApplyTheme() {
            return this.mThemeAttrs != null;
        }

        @Override
        public int addAtlasableBitmaps(Collection<Bitmap> atlasList) {
            if (isAtlasable(this.mBitmap) && atlasList.add(this.mBitmap)) {
                return this.mBitmap.getWidth() * this.mBitmap.getHeight();
            }
            return 0;
        }

        @Override
        public Drawable newDrawable() {
            return new BitmapDrawable(this, null);
        }

        @Override
        public Drawable newDrawable(Resources res) {
            return new BitmapDrawable(this, res);
        }

        @Override
        public int getChangingConfigurations() {
            return this.mChangingConfigurations;
        }
    }

    private BitmapDrawable(BitmapState state, Resources res) {
        this.mDstRect = new Rect();
        this.mTargetDensity = 160;
        this.mDstRectAndInsetsDirty = true;
        this.mOpticalInsets = Insets.NONE;
        this.mBitmapState = state;
        initializeWithState(this.mBitmapState, res);
    }

    private void initializeWithState(BitmapState state, Resources res) {
        if (res != null) {
            this.mTargetDensity = res.getDisplayMetrics().densityDpi;
        } else {
            this.mTargetDensity = state.mTargetDensity;
        }
        this.mTintFilter = updateTintFilter(this.mTintFilter, state.mTint, state.mTintMode);
        computeBitmapSize();
    }
}
