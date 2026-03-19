package android.graphics.drawable;

import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Insets;
import android.graphics.NinePatch;
import android.graphics.Outline;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.Rect;
import android.graphics.Region;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import com.android.internal.R;
import dalvik.system.BlockGuard;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

public class NinePatchDrawable extends Drawable {
    private static final boolean DEFAULT_DITHER = false;
    private int mBitmapHeight;
    private int mBitmapWidth;
    private boolean mMutated;
    private NinePatchState mNinePatchState;
    private Insets mOpticalInsets;
    private Rect mOutlineInsets;
    private float mOutlineRadius;
    private Rect mPadding;
    private Paint mPaint;
    private int mTargetDensity;
    private Rect mTempRect;
    private PorterDuffColorFilter mTintFilter;

    NinePatchDrawable(NinePatchState state, Resources res, NinePatchDrawable ninePatchDrawable) {
        this(state, res);
    }

    NinePatchDrawable() {
        this.mOpticalInsets = Insets.NONE;
        this.mTargetDensity = 160;
        this.mBitmapWidth = -1;
        this.mBitmapHeight = -1;
        this.mNinePatchState = new NinePatchState();
    }

    @Deprecated
    public NinePatchDrawable(Bitmap bitmap, byte[] chunk, Rect padding, String srcName) {
        this(new NinePatchState(new NinePatch(bitmap, chunk, srcName), padding), (Resources) null);
    }

    public NinePatchDrawable(Resources res, Bitmap bitmap, byte[] chunk, Rect padding, String srcName) {
        this(new NinePatchState(new NinePatch(bitmap, chunk, srcName), padding), res);
    }

    public NinePatchDrawable(Resources res, Bitmap bitmap, byte[] chunk, Rect padding, Rect opticalInsets, String srcName) {
        this(new NinePatchState(new NinePatch(bitmap, chunk, srcName), padding, opticalInsets), res);
    }

    @Deprecated
    public NinePatchDrawable(NinePatch patch) {
        this(new NinePatchState(patch, new Rect()), (Resources) null);
    }

    public NinePatchDrawable(Resources res, NinePatch patch) {
        this(new NinePatchState(patch, new Rect()), res);
    }

    public void setTargetDensity(Canvas canvas) {
        setTargetDensity(canvas.getDensity());
    }

    public void setTargetDensity(DisplayMetrics metrics) {
        setTargetDensity(metrics.densityDpi);
    }

    public void setTargetDensity(int density) {
        if (density == 0) {
            density = 160;
        }
        if (this.mTargetDensity == density) {
            return;
        }
        this.mTargetDensity = density;
        computeBitmapSize();
        invalidateSelf();
    }

    @Override
    public void draw(Canvas canvas) {
        boolean clearColorFilter;
        int restoreAlpha;
        NinePatchState state = this.mNinePatchState;
        Rect bounds = getBounds();
        int restoreToCount = -1;
        if (this.mTintFilter != null && getPaint().getColorFilter() == null) {
            this.mPaint.setColorFilter(this.mTintFilter);
            clearColorFilter = true;
        } else {
            clearColorFilter = false;
        }
        if (state.mBaseAlpha != 1.0f) {
            restoreAlpha = getPaint().getAlpha();
            this.mPaint.setAlpha((int) ((restoreAlpha * state.mBaseAlpha) + 0.5f));
        } else {
            restoreAlpha = -1;
        }
        boolean needsDensityScaling = canvas.getDensity() == 0;
        if (needsDensityScaling) {
            restoreToCount = canvas.save();
            float scale = this.mTargetDensity / state.mNinePatch.getDensity();
            float px = bounds.left;
            float py = bounds.top;
            canvas.scale(scale, scale, px, py);
            if (this.mTempRect == null) {
                this.mTempRect = new Rect();
            }
            Rect scaledBounds = this.mTempRect;
            scaledBounds.left = bounds.left;
            scaledBounds.top = bounds.top;
            scaledBounds.right = bounds.left + Math.round(bounds.width() / scale);
            scaledBounds.bottom = bounds.top + Math.round(bounds.height() / scale);
            bounds = scaledBounds;
        }
        boolean needsMirroring = needsMirroring();
        if (needsMirroring) {
            if (restoreToCount < 0) {
                restoreToCount = canvas.save();
            }
            float cx = (bounds.left + bounds.right) / 2.0f;
            float cy = (bounds.top + bounds.bottom) / 2.0f;
            canvas.scale(-1.0f, 1.0f, cx, cy);
        }
        state.mNinePatch.draw(canvas, bounds, this.mPaint);
        if (restoreToCount >= 0) {
            canvas.restoreToCount(restoreToCount);
        }
        if (clearColorFilter) {
            this.mPaint.setColorFilter(null);
        }
        if (restoreAlpha < 0) {
            return;
        }
        this.mPaint.setAlpha(restoreAlpha);
    }

    @Override
    public int getChangingConfigurations() {
        return super.getChangingConfigurations() | this.mNinePatchState.getChangingConfigurations();
    }

    @Override
    public boolean getPadding(Rect padding) {
        if (this.mPadding != null) {
            padding.set(this.mPadding);
            return (((padding.left | padding.top) | padding.right) | padding.bottom) != 0;
        }
        return super.getPadding(padding);
    }

    @Override
    public void getOutline(Outline outline) {
        NinePatch.InsetStruct insets;
        Rect bounds = getBounds();
        if (bounds.isEmpty()) {
            return;
        }
        if (this.mNinePatchState != null && this.mOutlineInsets != null && (insets = this.mNinePatchState.mNinePatch.getBitmap().getNinePatchInsets()) != null) {
            outline.setRoundRect(this.mOutlineInsets.left + bounds.left, this.mOutlineInsets.top + bounds.top, bounds.right - this.mOutlineInsets.right, bounds.bottom - this.mOutlineInsets.bottom, this.mOutlineRadius);
            outline.setAlpha(insets.outlineAlpha * (getAlpha() / 255.0f));
        } else {
            super.getOutline(outline);
        }
    }

    @Override
    public Insets getOpticalInsets() {
        Insets opticalInsets = this.mOpticalInsets;
        if (needsMirroring()) {
            return Insets.of(opticalInsets.right, opticalInsets.top, opticalInsets.left, opticalInsets.bottom);
        }
        return opticalInsets;
    }

    @Override
    public void setAlpha(int alpha) {
        if (this.mPaint == null && alpha == 255) {
            return;
        }
        getPaint().setAlpha(alpha);
        invalidateSelf();
    }

    @Override
    public int getAlpha() {
        if (this.mPaint == null) {
            return 255;
        }
        return getPaint().getAlpha();
    }

    @Override
    public void setColorFilter(ColorFilter colorFilter) {
        if (this.mPaint == null && colorFilter == null) {
            return;
        }
        getPaint().setColorFilter(colorFilter);
        invalidateSelf();
    }

    @Override
    public void setTintList(ColorStateList tint) {
        this.mNinePatchState.mTint = tint;
        this.mTintFilter = updateTintFilter(this.mTintFilter, tint, this.mNinePatchState.mTintMode);
        invalidateSelf();
    }

    @Override
    public void setTintMode(PorterDuff.Mode tintMode) {
        this.mNinePatchState.mTintMode = tintMode;
        this.mTintFilter = updateTintFilter(this.mTintFilter, this.mNinePatchState.mTint, tintMode);
        invalidateSelf();
    }

    @Override
    public void setDither(boolean dither) {
        if (this.mPaint == null && !dither) {
            return;
        }
        getPaint().setDither(dither);
        invalidateSelf();
    }

    @Override
    public void setAutoMirrored(boolean mirrored) {
        this.mNinePatchState.mAutoMirrored = mirrored;
    }

    private boolean needsMirroring() {
        return isAutoMirrored() && getLayoutDirection() == 1;
    }

    @Override
    public boolean isAutoMirrored() {
        return this.mNinePatchState.mAutoMirrored;
    }

    @Override
    public void setFilterBitmap(boolean filter) {
        getPaint().setFilterBitmap(filter);
        invalidateSelf();
    }

    @Override
    public boolean isFilterBitmap() {
        if (this.mPaint != null) {
            return getPaint().isFilterBitmap();
        }
        return false;
    }

    @Override
    public void inflate(Resources r, XmlPullParser parser, AttributeSet attrs, Resources.Theme theme) throws XmlPullParserException, BlockGuard.BlockGuardPolicyException, IOException {
        super.inflate(r, parser, attrs, theme);
        TypedArray a = obtainAttributes(r, theme, attrs, R.styleable.NinePatchDrawable);
        updateStateFromTypedArray(a);
        a.recycle();
        updateLocalState(r);
    }

    private void updateStateFromTypedArray(TypedArray a) throws XmlPullParserException, BlockGuard.BlockGuardPolicyException {
        Resources r = a.getResources();
        NinePatchState state = this.mNinePatchState;
        state.mChangingConfigurations |= a.getChangingConfigurations();
        state.mThemeAttrs = a.extractThemeAttrs();
        state.mDither = a.getBoolean(1, state.mDither);
        int srcResId = a.getResourceId(0, 0);
        if (srcResId != 0) {
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inDither = !state.mDither;
            options.inScreenDensity = r.getDisplayMetrics().noncompatDensityDpi;
            Rect padding = new Rect();
            Rect opticalInsets = new Rect();
            Bitmap bitmap = null;
            try {
                TypedValue value = new TypedValue();
                InputStream is = r.openRawResource(srcResId, value);
                bitmap = BitmapFactory.decodeResourceStream(r, value, is, padding, options);
                is.close();
            } catch (IOException e) {
            }
            if (bitmap == null) {
                throw new XmlPullParserException(a.getPositionDescription() + ": <nine-patch> requires a valid src attribute");
            }
            if (bitmap.getNinePatchChunk() == null) {
                throw new XmlPullParserException(a.getPositionDescription() + ": <nine-patch> requires a valid 9-patch source image");
            }
            bitmap.getOpticalInsets(opticalInsets);
            state.mNinePatch = new NinePatch(bitmap, bitmap.getNinePatchChunk());
            state.mPadding = padding;
            state.mOpticalInsets = Insets.of(opticalInsets);
        }
        state.mAutoMirrored = a.getBoolean(4, state.mAutoMirrored);
        state.mBaseAlpha = a.getFloat(3, state.mBaseAlpha);
        int tintMode = a.getInt(5, -1);
        if (tintMode != -1) {
            state.mTintMode = Drawable.parseTintMode(tintMode, PorterDuff.Mode.SRC_IN);
        }
        ColorStateList tint = a.getColorStateList(2);
        if (tint == null) {
            return;
        }
        state.mTint = tint;
    }

    @Override
    public void applyTheme(Resources.Theme t) {
        super.applyTheme(t);
        NinePatchState state = this.mNinePatchState;
        if (state == null) {
            return;
        }
        if (state.mThemeAttrs != null) {
            TypedArray a = t.resolveAttributes(state.mThemeAttrs, R.styleable.NinePatchDrawable);
            try {
                updateStateFromTypedArray(a);
            } catch (XmlPullParserException e) {
                rethrowAsRuntimeException(e);
            } finally {
                a.recycle();
            }
        }
        if (state.mTint != null && state.mTint.canApplyTheme()) {
            state.mTint = state.mTint.obtainForTheme(t);
        }
        updateLocalState(t.getResources());
    }

    @Override
    public boolean canApplyTheme() {
        if (this.mNinePatchState != null) {
            return this.mNinePatchState.canApplyTheme();
        }
        return false;
    }

    public Paint getPaint() {
        if (this.mPaint == null) {
            this.mPaint = new Paint();
            this.mPaint.setDither(false);
        }
        return this.mPaint;
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
        return (this.mNinePatchState.mNinePatch.hasAlpha() || (this.mPaint != null && this.mPaint.getAlpha() < 255)) ? -3 : -1;
    }

    @Override
    public Region getTransparentRegion() {
        return this.mNinePatchState.mNinePatch.getTransparentRegion(getBounds());
    }

    @Override
    public Drawable.ConstantState getConstantState() {
        this.mNinePatchState.mChangingConfigurations = getChangingConfigurations();
        return this.mNinePatchState;
    }

    @Override
    public Drawable mutate() {
        if (!this.mMutated && super.mutate() == this) {
            this.mNinePatchState = new NinePatchState(this.mNinePatchState);
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
        NinePatchState state = this.mNinePatchState;
        if (state.mTint != null && state.mTintMode != null) {
            this.mTintFilter = updateTintFilter(this.mTintFilter, state.mTint, state.mTintMode);
            return true;
        }
        return false;
    }

    @Override
    public boolean isStateful() {
        NinePatchState s = this.mNinePatchState;
        if (super.isStateful()) {
            return true;
        }
        if (s.mTint != null) {
            return s.mTint.isStateful();
        }
        return false;
    }

    static final class NinePatchState extends Drawable.ConstantState {
        boolean mAutoMirrored;
        float mBaseAlpha;
        int mChangingConfigurations;
        boolean mDither;
        NinePatch mNinePatch;
        Insets mOpticalInsets;
        Rect mPadding;
        int[] mThemeAttrs;
        ColorStateList mTint;
        PorterDuff.Mode mTintMode;

        NinePatchState() {
            this.mNinePatch = null;
            this.mTint = null;
            this.mTintMode = NinePatchDrawable.DEFAULT_TINT_MODE;
            this.mPadding = null;
            this.mOpticalInsets = Insets.NONE;
            this.mBaseAlpha = 1.0f;
            this.mDither = false;
            this.mAutoMirrored = false;
        }

        NinePatchState(NinePatch ninePatch, Rect padding) {
            this(ninePatch, padding, null, false, false);
        }

        NinePatchState(NinePatch ninePatch, Rect padding, Rect opticalInsets) {
            this(ninePatch, padding, opticalInsets, false, false);
        }

        NinePatchState(NinePatch ninePatch, Rect padding, Rect opticalInsets, boolean dither, boolean autoMirror) {
            this.mNinePatch = null;
            this.mTint = null;
            this.mTintMode = NinePatchDrawable.DEFAULT_TINT_MODE;
            this.mPadding = null;
            this.mOpticalInsets = Insets.NONE;
            this.mBaseAlpha = 1.0f;
            this.mDither = false;
            this.mAutoMirrored = false;
            this.mNinePatch = ninePatch;
            this.mPadding = padding;
            this.mOpticalInsets = Insets.of(opticalInsets);
            this.mDither = dither;
            this.mAutoMirrored = autoMirror;
        }

        NinePatchState(NinePatchState orig) {
            this.mNinePatch = null;
            this.mTint = null;
            this.mTintMode = NinePatchDrawable.DEFAULT_TINT_MODE;
            this.mPadding = null;
            this.mOpticalInsets = Insets.NONE;
            this.mBaseAlpha = 1.0f;
            this.mDither = false;
            this.mAutoMirrored = false;
            this.mChangingConfigurations = orig.mChangingConfigurations;
            this.mNinePatch = orig.mNinePatch;
            this.mTint = orig.mTint;
            this.mTintMode = orig.mTintMode;
            this.mPadding = orig.mPadding;
            this.mOpticalInsets = orig.mOpticalInsets;
            this.mBaseAlpha = orig.mBaseAlpha;
            this.mDither = orig.mDither;
            this.mAutoMirrored = orig.mAutoMirrored;
            this.mThemeAttrs = orig.mThemeAttrs;
        }

        @Override
        public boolean canApplyTheme() {
            if (this.mThemeAttrs != null || (this.mTint != null && this.mTint.canApplyTheme())) {
                return true;
            }
            return super.canApplyTheme();
        }

        @Override
        public int addAtlasableBitmaps(Collection<Bitmap> atlasList) {
            Bitmap bitmap = this.mNinePatch.getBitmap();
            if (isAtlasable(bitmap) && atlasList.add(bitmap)) {
                return bitmap.getWidth() * bitmap.getHeight();
            }
            return 0;
        }

        @Override
        public Drawable newDrawable() {
            return new NinePatchDrawable(this, null, 0 == true ? 1 : 0);
        }

        @Override
        public Drawable newDrawable(Resources res) {
            return new NinePatchDrawable(this, res, null);
        }

        @Override
        public int getChangingConfigurations() {
            return (this.mTint != null ? this.mTint.getChangingConfigurations() : 0) | this.mChangingConfigurations;
        }
    }

    private void computeBitmapSize() {
        NinePatch ninePatch = this.mNinePatchState.mNinePatch;
        if (ninePatch == null) {
            return;
        }
        int sourceDensity = ninePatch.getDensity();
        int targetDensity = this.mTargetDensity;
        Insets sourceOpticalInsets = this.mNinePatchState.mOpticalInsets;
        if (sourceOpticalInsets != Insets.NONE) {
            int left = Drawable.scaleFromDensity(sourceOpticalInsets.left, sourceDensity, targetDensity, true);
            int top = Drawable.scaleFromDensity(sourceOpticalInsets.top, sourceDensity, targetDensity, true);
            int right = Drawable.scaleFromDensity(sourceOpticalInsets.right, sourceDensity, targetDensity, true);
            int bottom = Drawable.scaleFromDensity(sourceOpticalInsets.bottom, sourceDensity, targetDensity, true);
            this.mOpticalInsets = Insets.of(left, top, right, bottom);
        } else {
            this.mOpticalInsets = Insets.NONE;
        }
        Rect sourcePadding = this.mNinePatchState.mPadding;
        if (sourcePadding != null) {
            if (this.mPadding == null) {
                this.mPadding = new Rect();
            }
            this.mPadding.left = Drawable.scaleFromDensity(sourcePadding.left, sourceDensity, targetDensity, false);
            this.mPadding.top = Drawable.scaleFromDensity(sourcePadding.top, sourceDensity, targetDensity, false);
            this.mPadding.right = Drawable.scaleFromDensity(sourcePadding.right, sourceDensity, targetDensity, false);
            this.mPadding.bottom = Drawable.scaleFromDensity(sourcePadding.bottom, sourceDensity, targetDensity, false);
        } else {
            this.mPadding = null;
        }
        this.mBitmapHeight = Drawable.scaleFromDensity(ninePatch.getHeight(), sourceDensity, targetDensity, true);
        this.mBitmapWidth = Drawable.scaleFromDensity(ninePatch.getWidth(), sourceDensity, targetDensity, true);
        NinePatch.InsetStruct insets = ninePatch.getBitmap().getNinePatchInsets();
        if (insets != null) {
            Rect outlineRect = insets.outlineRect;
            this.mOutlineInsets = NinePatch.InsetStruct.scaleInsets(outlineRect.left, outlineRect.top, outlineRect.right, outlineRect.bottom, targetDensity / sourceDensity);
            this.mOutlineRadius = Drawable.scaleFromDensity(insets.outlineRadius, sourceDensity, targetDensity);
            return;
        }
        this.mOutlineInsets = null;
    }

    private NinePatchDrawable(NinePatchState state, Resources res) {
        this.mOpticalInsets = Insets.NONE;
        this.mTargetDensity = 160;
        this.mBitmapWidth = -1;
        this.mBitmapHeight = -1;
        this.mNinePatchState = state;
        updateLocalState(res);
    }

    private void updateLocalState(Resources res) {
        NinePatchState state = this.mNinePatchState;
        if (state.mDither) {
            setDither(state.mDither);
        }
        if (res == null && state.mNinePatch != null) {
            this.mTargetDensity = state.mNinePatch.getDensity();
        } else {
            this.mTargetDensity = Drawable.resolveDensity(res, this.mTargetDensity);
        }
        this.mTintFilter = updateTintFilter(this.mTintFilter, state.mTint, state.mTintMode);
        computeBitmapSize();
    }
}
