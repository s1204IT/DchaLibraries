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
    private NinePatch mNinePatch;
    private NinePatchState mNinePatchState;
    private Insets mOpticalInsets;
    private Rect mPadding;
    private Paint mPaint;
    private int mTargetDensity;
    private PorterDuffColorFilter mTintFilter;

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
        this.mNinePatchState.mTargetDensity = this.mTargetDensity;
    }

    public NinePatchDrawable(Resources res, Bitmap bitmap, byte[] chunk, Rect padding, Rect opticalInsets, String srcName) {
        this(new NinePatchState(new NinePatch(bitmap, chunk, srcName), padding, opticalInsets), res);
        this.mNinePatchState.mTargetDensity = this.mTargetDensity;
    }

    @Deprecated
    public NinePatchDrawable(NinePatch patch) {
        this(new NinePatchState(patch, new Rect()), (Resources) null);
    }

    public NinePatchDrawable(Resources res, NinePatch patch) {
        this(new NinePatchState(patch, new Rect()), res);
        this.mNinePatchState.mTargetDensity = this.mTargetDensity;
    }

    public void setTargetDensity(Canvas canvas) {
        setTargetDensity(canvas.getDensity());
    }

    public void setTargetDensity(DisplayMetrics metrics) {
        setTargetDensity(metrics.densityDpi);
    }

    public void setTargetDensity(int density) {
        if (density != this.mTargetDensity) {
            if (density == 0) {
                density = 160;
            }
            this.mTargetDensity = density;
            if (this.mNinePatch != null) {
                computeBitmapSize();
            }
            invalidateSelf();
        }
    }

    private static Insets scaleFromDensity(Insets insets, int sdensity, int tdensity) {
        int left = Bitmap.scaleFromDensity(insets.left, sdensity, tdensity);
        int top = Bitmap.scaleFromDensity(insets.top, sdensity, tdensity);
        int right = Bitmap.scaleFromDensity(insets.right, sdensity, tdensity);
        int bottom = Bitmap.scaleFromDensity(insets.bottom, sdensity, tdensity);
        return Insets.of(left, top, right, bottom);
    }

    private void computeBitmapSize() {
        int sdensity = this.mNinePatch.getDensity();
        int tdensity = this.mTargetDensity;
        if (sdensity == tdensity) {
            this.mBitmapWidth = this.mNinePatch.getWidth();
            this.mBitmapHeight = this.mNinePatch.getHeight();
            this.mOpticalInsets = this.mNinePatchState.mOpticalInsets;
            return;
        }
        this.mBitmapWidth = Bitmap.scaleFromDensity(this.mNinePatch.getWidth(), sdensity, tdensity);
        this.mBitmapHeight = Bitmap.scaleFromDensity(this.mNinePatch.getHeight(), sdensity, tdensity);
        if (this.mNinePatchState.mPadding != null && this.mPadding != null) {
            Rect dest = this.mPadding;
            Rect src = this.mNinePatchState.mPadding;
            if (dest == src) {
                dest = new Rect(src);
                this.mPadding = dest;
            }
            dest.left = Bitmap.scaleFromDensity(src.left, sdensity, tdensity);
            dest.top = Bitmap.scaleFromDensity(src.top, sdensity, tdensity);
            dest.right = Bitmap.scaleFromDensity(src.right, sdensity, tdensity);
            dest.bottom = Bitmap.scaleFromDensity(src.bottom, sdensity, tdensity);
        }
        this.mOpticalInsets = scaleFromDensity(this.mNinePatchState.mOpticalInsets, sdensity, tdensity);
    }

    private void setNinePatch(NinePatch ninePatch) {
        if (this.mNinePatch != ninePatch) {
            this.mNinePatch = ninePatch;
            if (ninePatch != null) {
                computeBitmapSize();
            } else {
                this.mBitmapHeight = -1;
                this.mBitmapWidth = -1;
                this.mOpticalInsets = Insets.NONE;
            }
            invalidateSelf();
        }
    }

    @Override
    public void draw(Canvas canvas) {
        boolean clearColorFilter;
        int restoreAlpha;
        Rect bounds = getBounds();
        if (this.mTintFilter != null && getPaint().getColorFilter() == null) {
            this.mPaint.setColorFilter(this.mTintFilter);
            clearColorFilter = true;
        } else {
            clearColorFilter = false;
        }
        boolean needsMirroring = needsMirroring();
        if (needsMirroring) {
            canvas.translate(bounds.right - bounds.left, 0.0f);
            canvas.scale(-1.0f, 1.0f);
        }
        if (this.mNinePatchState.mBaseAlpha != 1.0f) {
            restoreAlpha = this.mPaint.getAlpha();
            this.mPaint.setAlpha((int) ((restoreAlpha * this.mNinePatchState.mBaseAlpha) + 0.5f));
        } else {
            restoreAlpha = -1;
        }
        this.mNinePatch.draw(canvas, bounds, this.mPaint);
        if (clearColorFilter) {
            this.mPaint.setColorFilter(null);
        }
        if (restoreAlpha >= 0) {
            this.mPaint.setAlpha(restoreAlpha);
        }
    }

    @Override
    public int getChangingConfigurations() {
        return super.getChangingConfigurations() | this.mNinePatchState.mChangingConfigurations;
    }

    @Override
    public boolean getPadding(Rect padding) {
        Rect scaledPadding = this.mPadding;
        if (scaledPadding == null) {
            return false;
        }
        if (needsMirroring()) {
            padding.set(scaledPadding.right, scaledPadding.top, scaledPadding.left, scaledPadding.bottom);
        } else {
            padding.set(scaledPadding);
        }
        return (((padding.left | padding.top) | padding.right) | padding.bottom) != 0;
    }

    @Override
    public void getOutline(Outline outline) {
        NinePatch.InsetStruct insets;
        Rect bounds = getBounds();
        if (!bounds.isEmpty()) {
            if (this.mNinePatchState != null && (insets = this.mNinePatchState.mNinePatch.getBitmap().getNinePatchInsets()) != null) {
                Rect outlineInsets = insets.outlineRect;
                outline.setRoundRect(outlineInsets.left + bounds.left, outlineInsets.top + bounds.top, bounds.right - outlineInsets.right, bounds.bottom - outlineInsets.bottom, insets.outlineRadius);
                outline.setAlpha(insets.outlineAlpha * (getAlpha() / 255.0f));
                return;
            }
            super.getOutline(outline);
        }
    }

    @Override
    public Insets getOpticalInsets() {
        return needsMirroring() ? Insets.of(this.mOpticalInsets.right, this.mOpticalInsets.top, this.mOpticalInsets.left, this.mOpticalInsets.bottom) : this.mOpticalInsets;
    }

    @Override
    public void setAlpha(int alpha) {
        if (this.mPaint != null || alpha != 255) {
            getPaint().setAlpha(alpha);
            invalidateSelf();
        }
    }

    @Override
    public int getAlpha() {
        if (this.mPaint == null) {
            return 255;
        }
        return getPaint().getAlpha();
    }

    @Override
    public void setColorFilter(ColorFilter cf) {
        if (this.mPaint != null || cf != null) {
            getPaint().setColorFilter(cf);
            invalidateSelf();
        }
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
        if (this.mPaint != null || dither) {
            getPaint().setDither(dither);
            invalidateSelf();
        }
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
    public void inflate(Resources r, XmlPullParser parser, AttributeSet attrs, Resources.Theme theme) throws XmlPullParserException, IOException {
        super.inflate(r, parser, attrs, theme);
        TypedArray a = obtainAttributes(r, theme, attrs, R.styleable.NinePatchDrawable);
        updateStateFromTypedArray(a);
        a.recycle();
    }

    private void updateStateFromTypedArray(TypedArray a) throws XmlPullParserException {
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
        if (tint != null) {
            state.mTint = tint;
        }
        initializeWithState(state, r);
        state.mTargetDensity = this.mTargetDensity;
    }

    @Override
    public void applyTheme(Resources.Theme t) {
        super.applyTheme(t);
        NinePatchState state = this.mNinePatchState;
        if (state != null && state.mThemeAttrs != null) {
            TypedArray a = t.resolveAttributes(state.mThemeAttrs, R.styleable.NinePatchDrawable);
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

    @Override
    public boolean canApplyTheme() {
        return (this.mNinePatchState == null || this.mNinePatchState.mThemeAttrs == null) ? false : true;
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
    public int getMinimumWidth() {
        return this.mBitmapWidth;
    }

    @Override
    public int getMinimumHeight() {
        return this.mBitmapHeight;
    }

    @Override
    public int getOpacity() {
        return (this.mNinePatch.hasAlpha() || (this.mPaint != null && this.mPaint.getAlpha() < 255)) ? -3 : -1;
    }

    @Override
    public Region getTransparentRegion() {
        return this.mNinePatch.getTransparentRegion(getBounds());
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
            this.mNinePatch = this.mNinePatchState.mNinePatch;
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
        if (state.mTint == null || state.mTintMode == null) {
            return false;
        }
        this.mTintFilter = updateTintFilter(this.mTintFilter, state.mTint, state.mTintMode);
        return true;
    }

    @Override
    public boolean isStateful() {
        NinePatchState s = this.mNinePatchState;
        return super.isStateful() || (s.mTint != null && s.mTint.isStateful());
    }

    static final class NinePatchState extends Drawable.ConstantState {
        boolean mAutoMirrored;
        float mBaseAlpha;
        int mChangingConfigurations;
        boolean mDither;
        NinePatch mNinePatch;
        Insets mOpticalInsets;
        Rect mPadding;
        int mTargetDensity;
        int[] mThemeAttrs;
        ColorStateList mTint;
        PorterDuff.Mode mTintMode;

        NinePatchState() {
            this.mThemeAttrs = null;
            this.mNinePatch = null;
            this.mTint = null;
            this.mTintMode = Drawable.DEFAULT_TINT_MODE;
            this.mPadding = null;
            this.mOpticalInsets = Insets.NONE;
            this.mBaseAlpha = 1.0f;
            this.mDither = false;
            this.mTargetDensity = 160;
            this.mAutoMirrored = false;
        }

        NinePatchState(NinePatch ninePatch, Rect padding) {
            this(ninePatch, padding, null, false, false);
        }

        NinePatchState(NinePatch ninePatch, Rect padding, Rect opticalInsets) {
            this(ninePatch, padding, opticalInsets, false, false);
        }

        NinePatchState(NinePatch ninePatch, Rect padding, Rect opticalInsets, boolean dither, boolean autoMirror) {
            this.mThemeAttrs = null;
            this.mNinePatch = null;
            this.mTint = null;
            this.mTintMode = Drawable.DEFAULT_TINT_MODE;
            this.mPadding = null;
            this.mOpticalInsets = Insets.NONE;
            this.mBaseAlpha = 1.0f;
            this.mDither = false;
            this.mTargetDensity = 160;
            this.mAutoMirrored = false;
            this.mNinePatch = ninePatch;
            this.mPadding = padding;
            this.mOpticalInsets = Insets.of(opticalInsets);
            this.mDither = dither;
            this.mAutoMirrored = autoMirror;
        }

        NinePatchState(NinePatchState state) {
            this.mThemeAttrs = null;
            this.mNinePatch = null;
            this.mTint = null;
            this.mTintMode = Drawable.DEFAULT_TINT_MODE;
            this.mPadding = null;
            this.mOpticalInsets = Insets.NONE;
            this.mBaseAlpha = 1.0f;
            this.mDither = false;
            this.mTargetDensity = 160;
            this.mAutoMirrored = false;
            this.mNinePatch = state.mNinePatch;
            this.mTint = state.mTint;
            this.mTintMode = state.mTintMode;
            this.mThemeAttrs = state.mThemeAttrs;
            this.mPadding = state.mPadding;
            this.mOpticalInsets = state.mOpticalInsets;
            this.mBaseAlpha = state.mBaseAlpha;
            this.mDither = state.mDither;
            this.mChangingConfigurations = state.mChangingConfigurations;
            this.mTargetDensity = state.mTargetDensity;
            this.mAutoMirrored = state.mAutoMirrored;
        }

        @Override
        public boolean canApplyTheme() {
            return this.mThemeAttrs != null;
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
            return new NinePatchDrawable(this, null);
        }

        @Override
        public Drawable newDrawable(Resources res) {
            return new NinePatchDrawable(this, res);
        }

        @Override
        public int getChangingConfigurations() {
            return this.mChangingConfigurations;
        }
    }

    private NinePatchDrawable(NinePatchState state, Resources res) {
        this.mOpticalInsets = Insets.NONE;
        this.mTargetDensity = 160;
        this.mBitmapWidth = -1;
        this.mBitmapHeight = -1;
        this.mNinePatchState = state;
        initializeWithState(this.mNinePatchState, res);
    }

    private void initializeWithState(NinePatchState state, Resources res) {
        if (res != null) {
            this.mTargetDensity = res.getDisplayMetrics().densityDpi;
        } else {
            this.mTargetDensity = state.mTargetDensity;
        }
        if (state.mDither) {
            setDither(state.mDither);
        }
        if (state.mPadding != null) {
            this.mPadding = new Rect(state.mPadding);
        }
        this.mTintFilter = updateTintFilter(this.mTintFilter, state.mTint, state.mTintMode);
        setNinePatch(state.mNinePatch);
    }
}
