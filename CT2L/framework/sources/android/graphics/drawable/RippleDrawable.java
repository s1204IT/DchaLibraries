package android.graphics.drawable;

import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Matrix;
import android.graphics.Outline;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.Rect;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import com.android.internal.R;
import java.io.IOException;
import java.util.Arrays;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

public class RippleDrawable extends LayerDrawable {
    private static final int MASK_CONTENT = 1;
    private static final int MASK_EXPLICIT = 2;
    private static final int MASK_NONE = 0;
    private static final int MASK_UNKNOWN = -1;
    private static final int MAX_RIPPLES = 10;
    public static final int RADIUS_AUTO = -1;
    private RippleBackground mBackground;
    private boolean mBackgroundActive;
    private float mDensity;
    private final Rect mDirtyBounds;
    private final Rect mDrawingBounds;
    private Ripple[] mExitingRipples;
    private int mExitingRipplesCount;
    private boolean mHasPending;
    private boolean mHasValidMask;
    private final Rect mHotspotBounds;
    private Drawable mMask;
    private Bitmap mMaskBuffer;
    private Canvas mMaskCanvas;
    private PorterDuffColorFilter mMaskColorFilter;
    private Matrix mMaskMatrix;
    private BitmapShader mMaskShader;
    private boolean mOverrideBounds;
    private float mPendingX;
    private float mPendingY;
    private Ripple mRipple;
    private boolean mRippleActive;
    private Paint mRipplePaint;
    private RippleState mState;
    private final Rect mTempRect;

    RippleDrawable() {
        this(new RippleState(null, null, null), null);
    }

    public RippleDrawable(ColorStateList color, Drawable content, Drawable mask) {
        this(new RippleState(null, null, null), null);
        if (color == null) {
            throw new IllegalArgumentException("RippleDrawable requires a non-null color");
        }
        if (content != null) {
            addLayer(content, null, 0, 0, 0, 0, 0);
        }
        if (mask != null) {
            addLayer(mask, null, 16908334, 0, 0, 0, 0);
        }
        setColor(color);
        ensurePadding();
        initializeFromState();
    }

    @Override
    public void jumpToCurrentState() {
        super.jumpToCurrentState();
        if (this.mRipple != null) {
            this.mRipple.jump();
        }
        if (this.mBackground != null) {
            this.mBackground.jump();
        }
        cancelExitingRipples();
        invalidateSelf();
    }

    private boolean cancelExitingRipples() {
        boolean needsDraw = false;
        int count = this.mExitingRipplesCount;
        Ripple[] ripples = this.mExitingRipples;
        for (int i = 0; i < count; i++) {
            needsDraw |= ripples[i].isHardwareAnimating();
            ripples[i].cancel();
        }
        if (ripples != null) {
            Arrays.fill(ripples, 0, count, (Object) null);
        }
        this.mExitingRipplesCount = 0;
        return needsDraw;
    }

    @Override
    public void setAlpha(int alpha) {
        super.setAlpha(alpha);
    }

    @Override
    public void setColorFilter(ColorFilter cf) {
        super.setColorFilter(cf);
    }

    @Override
    public int getOpacity() {
        return -3;
    }

    @Override
    protected boolean onStateChange(int[] stateSet) {
        boolean z = false;
        boolean changed = super.onStateChange(stateSet);
        boolean enabled = false;
        boolean pressed = false;
        boolean focused = false;
        for (int state : stateSet) {
            if (state == 16842910) {
                enabled = true;
            }
            if (state == 16842908) {
                focused = true;
            }
            if (state == 16842919) {
                pressed = true;
            }
        }
        setRippleActive(enabled && pressed);
        if (focused || (enabled && pressed)) {
            z = true;
        }
        setBackgroundActive(z, focused);
        return changed;
    }

    private void setRippleActive(boolean active) {
        if (this.mRippleActive != active) {
            this.mRippleActive = active;
            if (active) {
                tryRippleEnter();
            } else {
                tryRippleExit();
            }
        }
    }

    private void setBackgroundActive(boolean active, boolean focused) {
        if (this.mBackgroundActive != active) {
            this.mBackgroundActive = active;
            if (active) {
                tryBackgroundEnter(focused);
            } else {
                tryBackgroundExit();
            }
        }
    }

    @Override
    protected void onBoundsChange(Rect bounds) {
        super.onBoundsChange(bounds);
        if (!this.mOverrideBounds) {
            this.mHotspotBounds.set(bounds);
            onHotspotBoundsChanged();
        }
        invalidateSelf();
    }

    @Override
    public boolean setVisible(boolean visible, boolean restart) {
        boolean changed = super.setVisible(visible, restart);
        if (!visible) {
            clearHotspots();
        } else if (changed) {
            if (this.mRippleActive) {
                tryRippleEnter();
            }
            if (this.mBackgroundActive) {
                tryBackgroundEnter(false);
            }
            jumpToCurrentState();
        }
        return changed;
    }

    @Override
    public boolean isProjected() {
        return getNumberOfLayers() == 0;
    }

    @Override
    public boolean isStateful() {
        return true;
    }

    public void setColor(ColorStateList color) {
        this.mState.mColor = color;
        invalidateSelf();
    }

    @Override
    public void inflate(Resources r, XmlPullParser parser, AttributeSet attrs, Resources.Theme theme) throws XmlPullParserException, IOException {
        TypedArray a = obtainAttributes(r, theme, attrs, R.styleable.RippleDrawable);
        updateStateFromTypedArray(a);
        a.recycle();
        setPaddingMode(1);
        super.inflate(r, parser, attrs, theme);
        setTargetDensity(r.getDisplayMetrics());
        initializeFromState();
    }

    @Override
    public boolean setDrawableByLayerId(int id, Drawable drawable) {
        if (!super.setDrawableByLayerId(id, drawable)) {
            return false;
        }
        if (id == 16908334) {
            this.mMask = drawable;
        }
        return true;
    }

    @Override
    public void setPaddingMode(int mode) {
        super.setPaddingMode(mode);
    }

    private void updateStateFromTypedArray(TypedArray a) throws XmlPullParserException {
        RippleState state = this.mState;
        state.mChangingConfigurations |= a.getChangingConfigurations();
        state.mTouchThemeAttrs = a.extractThemeAttrs();
        ColorStateList color = a.getColorStateList(0);
        if (color != null) {
            this.mState.mColor = color;
        }
        verifyRequiredAttributes(a);
    }

    private void verifyRequiredAttributes(TypedArray a) throws XmlPullParserException {
        if (this.mState.mColor == null) {
            if (this.mState.mTouchThemeAttrs == null || this.mState.mTouchThemeAttrs[0] == 0) {
                throw new XmlPullParserException(a.getPositionDescription() + ": <ripple> requires a valid color attribute");
            }
        }
    }

    private void setTargetDensity(DisplayMetrics metrics) {
        if (this.mDensity != metrics.density) {
            this.mDensity = metrics.density;
            invalidateSelf();
        }
    }

    @Override
    public void applyTheme(Resources.Theme t) {
        super.applyTheme(t);
        RippleState state = this.mState;
        if (state != null && state.mTouchThemeAttrs != null) {
            TypedArray a = t.resolveAttributes(state.mTouchThemeAttrs, R.styleable.RippleDrawable);
            try {
                try {
                    updateStateFromTypedArray(a);
                    a.recycle();
                    initializeFromState();
                } catch (XmlPullParserException e) {
                    throw new RuntimeException(e);
                }
            } catch (Throwable th) {
                a.recycle();
                throw th;
            }
        }
    }

    @Override
    public boolean canApplyTheme() {
        return (this.mState != null && this.mState.canApplyTheme()) || super.canApplyTheme();
    }

    @Override
    public void setHotspot(float x, float y) {
        if (this.mRipple == null || this.mBackground == null) {
            this.mPendingX = x;
            this.mPendingY = y;
            this.mHasPending = true;
        }
        if (this.mRipple != null) {
            this.mRipple.move(x, y);
        }
    }

    private void tryBackgroundEnter(boolean focused) {
        if (this.mBackground == null) {
            this.mBackground = new RippleBackground(this, this.mHotspotBounds);
        }
        this.mBackground.setup(this.mState.mMaxRadius, this.mDensity);
        this.mBackground.enter(focused);
    }

    private void tryBackgroundExit() {
        if (this.mBackground != null) {
            this.mBackground.exit();
        }
    }

    private void tryRippleEnter() {
        float x;
        float y;
        if (this.mExitingRipplesCount < 10) {
            if (this.mRipple == null) {
                if (this.mHasPending) {
                    this.mHasPending = false;
                    x = this.mPendingX;
                    y = this.mPendingY;
                } else {
                    x = this.mHotspotBounds.exactCenterX();
                    y = this.mHotspotBounds.exactCenterY();
                }
                this.mRipple = new Ripple(this, this.mHotspotBounds, x, y);
            }
            this.mRipple.setup(this.mState.mMaxRadius, this.mDensity);
            this.mRipple.enter();
        }
    }

    private void tryRippleExit() {
        if (this.mRipple != null) {
            if (this.mExitingRipples == null) {
                this.mExitingRipples = new Ripple[10];
            }
            Ripple[] rippleArr = this.mExitingRipples;
            int i = this.mExitingRipplesCount;
            this.mExitingRipplesCount = i + 1;
            rippleArr[i] = this.mRipple;
            this.mRipple.exit();
            this.mRipple = null;
        }
    }

    private void clearHotspots() {
        if (this.mRipple != null) {
            this.mRipple.cancel();
            this.mRipple = null;
            this.mRippleActive = false;
        }
        if (this.mBackground != null) {
            this.mBackground.cancel();
            this.mBackground = null;
            this.mBackgroundActive = false;
        }
        cancelExitingRipples();
        invalidateSelf();
    }

    @Override
    public void setHotspotBounds(int left, int top, int right, int bottom) {
        this.mOverrideBounds = true;
        this.mHotspotBounds.set(left, top, right, bottom);
        onHotspotBoundsChanged();
    }

    @Override
    public void getHotspotBounds(Rect outRect) {
        outRect.set(this.mHotspotBounds);
    }

    private void onHotspotBoundsChanged() {
        int count = this.mExitingRipplesCount;
        Ripple[] ripples = this.mExitingRipples;
        for (int i = 0; i < count; i++) {
            ripples[i].onHotspotBoundsChanged();
        }
        if (this.mRipple != null) {
            this.mRipple.onHotspotBoundsChanged();
        }
        if (this.mBackground != null) {
            this.mBackground.onHotspotBoundsChanged();
        }
    }

    @Override
    public void getOutline(Outline outline) {
        LayerDrawable.LayerState state = this.mLayerState;
        LayerDrawable.ChildDrawable[] children = state.mChildren;
        int N = state.mNum;
        for (int i = 0; i < N; i++) {
            if (children[i].mId != 16908334) {
                children[i].mDrawable.getOutline(outline);
                if (!outline.isEmpty()) {
                    return;
                }
            }
        }
    }

    @Override
    public void draw(Canvas canvas) {
        Rect bounds = getDirtyBounds();
        int saveCount = canvas.save(2);
        canvas.clipRect(bounds);
        drawContent(canvas);
        drawBackgroundAndRipples(canvas);
        canvas.restoreToCount(saveCount);
    }

    @Override
    public void invalidateSelf() {
        super.invalidateSelf();
        this.mHasValidMask = false;
    }

    private void updateMaskShaderIfNeeded() {
        int maskType;
        if (!this.mHasValidMask && (maskType = getMaskType()) != -1) {
            this.mHasValidMask = true;
            Rect bounds = getBounds();
            if (maskType == 0 || bounds.isEmpty()) {
                if (this.mMaskBuffer != null) {
                    this.mMaskBuffer.recycle();
                    this.mMaskBuffer = null;
                    this.mMaskShader = null;
                    this.mMaskCanvas = null;
                }
                this.mMaskMatrix = null;
                this.mMaskColorFilter = null;
                return;
            }
            if (this.mMaskBuffer == null || this.mMaskBuffer.getWidth() != bounds.width() || this.mMaskBuffer.getHeight() != bounds.height()) {
                if (this.mMaskBuffer != null) {
                    this.mMaskBuffer.recycle();
                }
                this.mMaskBuffer = Bitmap.createBitmap(bounds.width(), bounds.height(), Bitmap.Config.ALPHA_8);
                this.mMaskShader = new BitmapShader(this.mMaskBuffer, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP);
                this.mMaskCanvas = new Canvas(this.mMaskBuffer);
            } else {
                this.mMaskBuffer.eraseColor(0);
            }
            if (this.mMaskMatrix == null) {
                this.mMaskMatrix = new Matrix();
            } else {
                this.mMaskMatrix.reset();
            }
            if (this.mMaskColorFilter == null) {
                this.mMaskColorFilter = new PorterDuffColorFilter(0, PorterDuff.Mode.SRC_IN);
            }
            if (maskType == 2) {
                drawMask(this.mMaskCanvas);
            } else if (maskType == 1) {
                drawContent(this.mMaskCanvas);
            }
        }
    }

    private int getMaskType() {
        if (this.mRipple == null && this.mExitingRipplesCount <= 0 && (this.mBackground == null || !this.mBackground.shouldDraw())) {
            return -1;
        }
        if (this.mMask != null) {
            return this.mMask.getOpacity() == -1 ? 0 : 2;
        }
        LayerDrawable.ChildDrawable[] array = this.mLayerState.mChildren;
        int count = this.mLayerState.mNum;
        for (int i = 0; i < count; i++) {
            if (array[i].mDrawable.getOpacity() != -1) {
                return 1;
            }
        }
        return 0;
    }

    void removeRipple(Ripple ripple) {
        Ripple[] ripples = this.mExitingRipples;
        int count = this.mExitingRipplesCount;
        int index = getRippleIndex(ripple);
        if (index >= 0) {
            System.arraycopy(ripples, index + 1, ripples, index, count - (index + 1));
            ripples[count - 1] = null;
            this.mExitingRipplesCount--;
            invalidateSelf();
        }
    }

    private int getRippleIndex(Ripple ripple) {
        Ripple[] ripples = this.mExitingRipples;
        int count = this.mExitingRipplesCount;
        for (int i = 0; i < count; i++) {
            if (ripples[i] == ripple) {
                return i;
            }
        }
        return -1;
    }

    private void drawContent(Canvas canvas) {
        LayerDrawable.ChildDrawable[] array = this.mLayerState.mChildren;
        int count = this.mLayerState.mNum;
        for (int i = 0; i < count; i++) {
            if (array[i].mId != 16908334) {
                array[i].mDrawable.draw(canvas);
            }
        }
    }

    private void drawBackgroundAndRipples(Canvas canvas) {
        Ripple active = this.mRipple;
        RippleBackground background = this.mBackground;
        int count = this.mExitingRipplesCount;
        if (active != null || count > 0 || (background != null && background.shouldDraw())) {
            float x = this.mHotspotBounds.exactCenterX();
            float y = this.mHotspotBounds.exactCenterY();
            canvas.translate(x, y);
            updateMaskShaderIfNeeded();
            if (this.mMaskShader != null) {
                this.mMaskMatrix.setTranslate(-x, -y);
                this.mMaskShader.setLocalMatrix(this.mMaskMatrix);
            }
            int color = this.mState.mColor.getColorForState(getState(), -16777216);
            int halfAlpha = (Color.alpha(color) / 2) << 24;
            Paint p = getRipplePaint();
            if (this.mMaskColorFilter != null) {
                int fullAlphaColor = color | (-16777216);
                this.mMaskColorFilter.setColor(fullAlphaColor);
                p.setColor(halfAlpha);
                p.setColorFilter(this.mMaskColorFilter);
                p.setShader(this.mMaskShader);
            } else {
                int halfAlphaColor = (16777215 & color) | halfAlpha;
                p.setColor(halfAlphaColor);
                p.setColorFilter(null);
                p.setShader(null);
            }
            if (background != null && background.shouldDraw()) {
                background.draw(canvas, p);
            }
            if (count > 0) {
                Ripple[] ripples = this.mExitingRipples;
                for (int i = 0; i < count; i++) {
                    ripples[i].draw(canvas, p);
                }
            }
            if (active != null) {
                active.draw(canvas, p);
            }
            canvas.translate(-x, -y);
        }
    }

    private void drawMask(Canvas canvas) {
        this.mMask.draw(canvas);
    }

    private Paint getRipplePaint() {
        if (this.mRipplePaint == null) {
            this.mRipplePaint = new Paint();
            this.mRipplePaint.setAntiAlias(true);
            this.mRipplePaint.setStyle(Paint.Style.FILL);
        }
        return this.mRipplePaint;
    }

    @Override
    public Rect getDirtyBounds() {
        if (!isProjected()) {
            return getBounds();
        }
        Rect drawingBounds = this.mDrawingBounds;
        Rect dirtyBounds = this.mDirtyBounds;
        dirtyBounds.set(drawingBounds);
        drawingBounds.setEmpty();
        int cX = (int) this.mHotspotBounds.exactCenterX();
        int cY = (int) this.mHotspotBounds.exactCenterY();
        Rect rippleBounds = this.mTempRect;
        Ripple[] activeRipples = this.mExitingRipples;
        int N = this.mExitingRipplesCount;
        for (int i = 0; i < N; i++) {
            activeRipples[i].getBounds(rippleBounds);
            rippleBounds.offset(cX, cY);
            drawingBounds.union(rippleBounds);
        }
        RippleBackground background = this.mBackground;
        if (background != null) {
            background.getBounds(rippleBounds);
            rippleBounds.offset(cX, cY);
            drawingBounds.union(rippleBounds);
        }
        dirtyBounds.union(drawingBounds);
        dirtyBounds.union(super.getDirtyBounds());
        return dirtyBounds;
    }

    @Override
    public Drawable.ConstantState getConstantState() {
        return this.mState;
    }

    @Override
    public Drawable mutate() {
        super.mutate();
        this.mState = (RippleState) this.mLayerState;
        this.mMask = findDrawableByLayerId(16908334);
        return this;
    }

    @Override
    RippleState createConstantState(LayerDrawable.LayerState state, Resources res) {
        return new RippleState(state, this, res);
    }

    static class RippleState extends LayerDrawable.LayerState {
        ColorStateList mColor;
        int mMaxRadius;
        int[] mTouchThemeAttrs;

        public RippleState(LayerDrawable.LayerState orig, RippleDrawable owner, Resources res) {
            super(orig, owner, res);
            this.mColor = ColorStateList.valueOf(Color.MAGENTA);
            this.mMaxRadius = -1;
            if (orig != null && (orig instanceof RippleState)) {
                RippleState origs = (RippleState) orig;
                this.mTouchThemeAttrs = origs.mTouchThemeAttrs;
                this.mColor = origs.mColor;
                this.mMaxRadius = origs.mMaxRadius;
            }
        }

        @Override
        public boolean canApplyTheme() {
            return this.mTouchThemeAttrs != null || super.canApplyTheme();
        }

        @Override
        public Drawable newDrawable() {
            return new RippleDrawable(this, (Resources) null);
        }

        @Override
        public Drawable newDrawable(Resources res) {
            return new RippleDrawable(this, res);
        }
    }

    public void setMaxRadius(int maxRadius) {
        if (maxRadius != -1 && maxRadius < 0) {
            throw new IllegalArgumentException("maxRadius must be RADIUS_AUTO or >= 0");
        }
        this.mState.mMaxRadius = maxRadius;
    }

    public int getMaxRadius() {
        return this.mState.mMaxRadius;
    }

    private RippleDrawable(RippleState state, Resources res) {
        this.mTempRect = new Rect();
        this.mHotspotBounds = new Rect();
        this.mDrawingBounds = new Rect();
        this.mDirtyBounds = new Rect();
        this.mExitingRipplesCount = 0;
        this.mDensity = 1.0f;
        this.mState = new RippleState(state, this, res);
        this.mLayerState = this.mState;
        if (this.mState.mNum > 0) {
            ensurePadding();
        }
        if (res != null) {
            this.mDensity = res.getDisplayMetrics().density;
        }
        initializeFromState();
    }

    private void initializeFromState() {
        this.mMask = findDrawableByLayerId(16908334);
    }
}
