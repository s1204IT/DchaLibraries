package android.graphics.drawable;

import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Color;
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
    private int mDensity;
    private final Rect mDirtyBounds;
    private final Rect mDrawingBounds;
    private RippleForeground[] mExitingRipples;
    private int mExitingRipplesCount;
    private boolean mForceSoftware;
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
    private RippleForeground mRipple;
    private boolean mRippleActive;
    private Paint mRipplePaint;
    private RippleState mState;
    private final Rect mTempRect;

    RippleDrawable() {
        this(new RippleState(null, null, null), null);
    }

    public RippleDrawable(ColorStateList colorStateList, Drawable drawable, Drawable drawable2) {
        this(new RippleState(null, null, null), null);
        if (colorStateList == null) {
            throw new IllegalArgumentException("RippleDrawable requires a non-null color");
        }
        if (drawable != null) {
            addLayer(drawable, null, 0, 0, 0, 0, 0);
        }
        if (drawable2 != null) {
            addLayer(drawable2, null, 16908334, 0, 0, 0, 0);
        }
        setColor(colorStateList);
        ensurePadding();
        refreshPadding();
        updateLocalState();
    }

    @Override
    public void jumpToCurrentState() {
        super.jumpToCurrentState();
        if (this.mRipple != null) {
            this.mRipple.end();
        }
        if (this.mBackground != null) {
            this.mBackground.jumpToFinal();
        }
        cancelExitingRipples();
    }

    private void cancelExitingRipples() {
        int i = this.mExitingRipplesCount;
        RippleForeground[] rippleForegroundArr = this.mExitingRipples;
        for (int i2 = 0; i2 < i; i2++) {
            rippleForegroundArr[i2].end();
        }
        if (rippleForegroundArr != null) {
            Arrays.fill(rippleForegroundArr, 0, i, (Object) null);
        }
        this.mExitingRipplesCount = 0;
        invalidateSelf(false);
    }

    @Override
    public int getOpacity() {
        return -3;
    }

    @Override
    protected boolean onStateChange(int[] iArr) {
        boolean zOnStateChange = super.onStateChange(iArr);
        boolean z = false;
        boolean z2 = false;
        boolean z3 = false;
        boolean z4 = false;
        boolean z5 = false;
        for (int i : iArr) {
            if (i == 16842910) {
                z2 = true;
            } else if (i == 16842908) {
                z5 = true;
            } else if (i == 16842919) {
                z3 = true;
            } else if (i == 16843623) {
                z4 = true;
            }
        }
        if (z2 && z3) {
            z = true;
        }
        setRippleActive(z);
        setBackgroundActive(z4, z5, z3);
        return zOnStateChange;
    }

    private void setRippleActive(boolean z) {
        if (this.mRippleActive != z) {
            this.mRippleActive = z;
            if (z) {
                tryRippleEnter();
            } else {
                tryRippleExit();
            }
        }
    }

    private void setBackgroundActive(boolean z, boolean z2, boolean z3) {
        if (this.mBackground == null && (z || z2)) {
            this.mBackground = new RippleBackground(this, this.mHotspotBounds, isBounded());
            this.mBackground.setup(this.mState.mMaxRadius, this.mDensity);
        }
        if (this.mBackground != null) {
            this.mBackground.setState(z2, z, z3);
        }
    }

    @Override
    protected void onBoundsChange(Rect rect) {
        super.onBoundsChange(rect);
        if (!this.mOverrideBounds) {
            this.mHotspotBounds.set(rect);
            onHotspotBoundsChanged();
        }
        int i = this.mExitingRipplesCount;
        RippleForeground[] rippleForegroundArr = this.mExitingRipples;
        for (int i2 = 0; i2 < i; i2++) {
            rippleForegroundArr[i2].onBoundsChange();
        }
        if (this.mBackground != null) {
            this.mBackground.onBoundsChange();
        }
        if (this.mRipple != null) {
            this.mRipple.onBoundsChange();
        }
        invalidateSelf();
    }

    @Override
    public boolean setVisible(boolean z, boolean z2) {
        boolean visible = super.setVisible(z, z2);
        if (!z) {
            clearHotspots();
        } else if (visible) {
            if (this.mRippleActive) {
                tryRippleEnter();
            }
            jumpToCurrentState();
        }
        return visible;
    }

    @Override
    public boolean isProjected() {
        if (isBounded()) {
            return false;
        }
        int i = this.mState.mMaxRadius;
        Rect bounds = getBounds();
        Rect rect = this.mHotspotBounds;
        if (i == -1 || i > rect.width() / 2 || i > rect.height() / 2) {
            return true;
        }
        return (bounds.equals(rect) || bounds.contains(rect)) ? false : true;
    }

    private boolean isBounded() {
        return getNumberOfLayers() > 0;
    }

    @Override
    public boolean isStateful() {
        return true;
    }

    @Override
    public boolean hasFocusStateSpecified() {
        return true;
    }

    public void setColor(ColorStateList colorStateList) {
        this.mState.mColor = colorStateList;
        invalidateSelf(false);
    }

    public void setRadius(int i) {
        this.mState.mMaxRadius = i;
        invalidateSelf(false);
    }

    public int getRadius() {
        return this.mState.mMaxRadius;
    }

    @Override
    public void inflate(Resources resources, XmlPullParser xmlPullParser, AttributeSet attributeSet, Resources.Theme theme) throws XmlPullParserException, IOException {
        TypedArray typedArrayObtainAttributes = obtainAttributes(resources, theme, attributeSet, R.styleable.RippleDrawable);
        setPaddingMode(1);
        super.inflate(resources, xmlPullParser, attributeSet, theme);
        updateStateFromTypedArray(typedArrayObtainAttributes);
        verifyRequiredAttributes(typedArrayObtainAttributes);
        typedArrayObtainAttributes.recycle();
        updateLocalState();
    }

    @Override
    public boolean setDrawableByLayerId(int i, Drawable drawable) {
        if (!super.setDrawableByLayerId(i, drawable)) {
            return false;
        }
        if (i == 16908334) {
            this.mMask = drawable;
            this.mHasValidMask = false;
            return true;
        }
        return true;
    }

    @Override
    public void setPaddingMode(int i) {
        super.setPaddingMode(i);
    }

    private void updateStateFromTypedArray(TypedArray typedArray) throws XmlPullParserException {
        RippleState rippleState = this.mState;
        rippleState.mChangingConfigurations |= typedArray.getChangingConfigurations();
        rippleState.mTouchThemeAttrs = typedArray.extractThemeAttrs();
        ColorStateList colorStateList = typedArray.getColorStateList(0);
        if (colorStateList != null) {
            this.mState.mColor = colorStateList;
        }
        this.mState.mMaxRadius = typedArray.getDimensionPixelSize(1, this.mState.mMaxRadius);
    }

    private void verifyRequiredAttributes(TypedArray typedArray) throws XmlPullParserException {
        if (this.mState.mColor == null) {
            if (this.mState.mTouchThemeAttrs == null || this.mState.mTouchThemeAttrs[0] == 0) {
                throw new XmlPullParserException(typedArray.getPositionDescription() + ": <ripple> requires a valid color attribute");
            }
        }
    }

    @Override
    public void applyTheme(Resources.Theme theme) {
        super.applyTheme(theme);
        RippleState rippleState = this.mState;
        if (rippleState == null) {
            return;
        }
        if (rippleState.mTouchThemeAttrs != null) {
            TypedArray typedArrayResolveAttributes = theme.resolveAttributes(rippleState.mTouchThemeAttrs, R.styleable.RippleDrawable);
            try {
                try {
                    updateStateFromTypedArray(typedArrayResolveAttributes);
                    verifyRequiredAttributes(typedArrayResolveAttributes);
                } catch (XmlPullParserException e) {
                    rethrowAsRuntimeException(e);
                }
            } finally {
                typedArrayResolveAttributes.recycle();
            }
        }
        if (rippleState.mColor != null && rippleState.mColor.canApplyTheme()) {
            rippleState.mColor = rippleState.mColor.obtainForTheme(theme);
        }
        updateLocalState();
    }

    @Override
    public boolean canApplyTheme() {
        return (this.mState != null && this.mState.canApplyTheme()) || super.canApplyTheme();
    }

    @Override
    public void setHotspot(float f, float f2) {
        if (this.mRipple == null || this.mBackground == null) {
            this.mPendingX = f;
            this.mPendingY = f2;
            this.mHasPending = true;
        }
        if (this.mRipple != null) {
            this.mRipple.move(f, f2);
        }
    }

    private void tryRippleEnter() {
        float fExactCenterX;
        float fExactCenterY;
        if (this.mExitingRipplesCount >= 10) {
            return;
        }
        if (this.mRipple == null) {
            if (this.mHasPending) {
                this.mHasPending = false;
                fExactCenterX = this.mPendingX;
                fExactCenterY = this.mPendingY;
            } else {
                fExactCenterX = this.mHotspotBounds.exactCenterX();
                fExactCenterY = this.mHotspotBounds.exactCenterY();
            }
            this.mRipple = new RippleForeground(this, this.mHotspotBounds, fExactCenterX, fExactCenterY, this.mForceSoftware);
        }
        this.mRipple.setup(this.mState.mMaxRadius, this.mDensity);
        this.mRipple.enter();
    }

    private void tryRippleExit() {
        if (this.mRipple != null) {
            if (this.mExitingRipples == null) {
                this.mExitingRipples = new RippleForeground[10];
            }
            RippleForeground[] rippleForegroundArr = this.mExitingRipples;
            int i = this.mExitingRipplesCount;
            this.mExitingRipplesCount = i + 1;
            rippleForegroundArr[i] = this.mRipple;
            this.mRipple.exit();
            this.mRipple = null;
        }
    }

    private void clearHotspots() {
        if (this.mRipple != null) {
            this.mRipple.end();
            this.mRipple = null;
            this.mRippleActive = false;
        }
        if (this.mBackground != null) {
            this.mBackground.setState(false, false, false);
        }
        cancelExitingRipples();
    }

    @Override
    public void setHotspotBounds(int i, int i2, int i3, int i4) {
        this.mOverrideBounds = true;
        this.mHotspotBounds.set(i, i2, i3, i4);
        onHotspotBoundsChanged();
    }

    @Override
    public void getHotspotBounds(Rect rect) {
        rect.set(this.mHotspotBounds);
    }

    private void onHotspotBoundsChanged() {
        int i = this.mExitingRipplesCount;
        RippleForeground[] rippleForegroundArr = this.mExitingRipples;
        for (int i2 = 0; i2 < i; i2++) {
            rippleForegroundArr[i2].onHotspotBoundsChanged();
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
        LayerDrawable.LayerState layerState = this.mLayerState;
        LayerDrawable.ChildDrawable[] childDrawableArr = layerState.mChildren;
        int i = layerState.mNumChildren;
        for (int i2 = 0; i2 < i; i2++) {
            if (childDrawableArr[i2].mId != 16908334) {
                childDrawableArr[i2].mDrawable.getOutline(outline);
                if (!outline.isEmpty()) {
                    return;
                }
            }
        }
    }

    @Override
    public void draw(Canvas canvas) {
        pruneRipples();
        Rect dirtyBounds = getDirtyBounds();
        int iSave = canvas.save(2);
        if (isBounded()) {
            canvas.clipRect(dirtyBounds);
        }
        drawContent(canvas);
        drawBackgroundAndRipples(canvas);
        canvas.restoreToCount(iSave);
    }

    @Override
    public void invalidateSelf() {
        invalidateSelf(true);
    }

    void invalidateSelf(boolean z) {
        super.invalidateSelf();
        if (z) {
            this.mHasValidMask = false;
        }
    }

    private void pruneRipples() {
        RippleForeground[] rippleForegroundArr = this.mExitingRipples;
        int i = this.mExitingRipplesCount;
        int i2 = 0;
        for (int i3 = 0; i3 < i; i3++) {
            if (!rippleForegroundArr[i3].hasFinishedExit()) {
                rippleForegroundArr[i2] = rippleForegroundArr[i3];
                i2++;
            }
        }
        for (int i4 = i2; i4 < i; i4++) {
            rippleForegroundArr[i4] = null;
        }
        this.mExitingRipplesCount = i2;
    }

    private void updateMaskShaderIfNeeded() {
        int maskType;
        if (this.mHasValidMask || (maskType = getMaskType()) == -1) {
            return;
        }
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
        int i = bounds.left;
        int i2 = bounds.top;
        this.mMaskCanvas.translate(-i, -i2);
        if (maskType == 2) {
            drawMask(this.mMaskCanvas);
        } else if (maskType == 1) {
            drawContent(this.mMaskCanvas);
        }
        this.mMaskCanvas.translate(i, i2);
    }

    private int getMaskType() {
        if (this.mRipple == null && this.mExitingRipplesCount <= 0 && (this.mBackground == null || !this.mBackground.isVisible())) {
            return -1;
        }
        if (this.mMask != null) {
            return this.mMask.getOpacity() == -1 ? 0 : 2;
        }
        LayerDrawable.ChildDrawable[] childDrawableArr = this.mLayerState.mChildren;
        int i = this.mLayerState.mNumChildren;
        for (int i2 = 0; i2 < i; i2++) {
            if (childDrawableArr[i2].mDrawable.getOpacity() != -1) {
                return 1;
            }
        }
        return 0;
    }

    private void drawContent(Canvas canvas) {
        LayerDrawable.ChildDrawable[] childDrawableArr = this.mLayerState.mChildren;
        int i = this.mLayerState.mNumChildren;
        for (int i2 = 0; i2 < i; i2++) {
            if (childDrawableArr[i2].mId != 16908334) {
                childDrawableArr[i2].mDrawable.draw(canvas);
            }
        }
    }

    private void drawBackgroundAndRipples(Canvas canvas) {
        RippleForeground rippleForeground = this.mRipple;
        RippleBackground rippleBackground = this.mBackground;
        int i = this.mExitingRipplesCount;
        if (rippleForeground == null && i <= 0 && (rippleBackground == null || !rippleBackground.isVisible())) {
            return;
        }
        float fExactCenterX = this.mHotspotBounds.exactCenterX();
        float fExactCenterY = this.mHotspotBounds.exactCenterY();
        canvas.translate(fExactCenterX, fExactCenterY);
        Paint ripplePaint = getRipplePaint();
        if (rippleBackground != null && rippleBackground.isVisible()) {
            rippleBackground.draw(canvas, ripplePaint);
        }
        if (i > 0) {
            RippleForeground[] rippleForegroundArr = this.mExitingRipples;
            for (int i2 = 0; i2 < i; i2++) {
                rippleForegroundArr[i2].draw(canvas, ripplePaint);
            }
        }
        if (rippleForeground != null) {
            rippleForeground.draw(canvas, ripplePaint);
        }
        canvas.translate(-fExactCenterX, -fExactCenterY);
    }

    private void drawMask(Canvas canvas) {
        this.mMask.draw(canvas);
    }

    Paint getRipplePaint() {
        if (this.mRipplePaint == null) {
            this.mRipplePaint = new Paint();
            this.mRipplePaint.setAntiAlias(true);
            this.mRipplePaint.setStyle(Paint.Style.FILL);
        }
        float fExactCenterX = this.mHotspotBounds.exactCenterX();
        float fExactCenterY = this.mHotspotBounds.exactCenterY();
        updateMaskShaderIfNeeded();
        if (this.mMaskShader != null) {
            Rect bounds = getBounds();
            this.mMaskMatrix.setTranslate(bounds.left - fExactCenterX, bounds.top - fExactCenterY);
            this.mMaskShader.setLocalMatrix(this.mMaskMatrix);
        }
        int colorForState = this.mState.mColor.getColorForState(getState(), -16777216);
        if (Color.alpha(colorForState) > 128) {
            colorForState = (colorForState & 16777215) | Integer.MIN_VALUE;
        }
        Paint paint = this.mRipplePaint;
        if (this.mMaskColorFilter != null) {
            this.mMaskColorFilter.setColor(colorForState | (-16777216));
            paint.setColor(colorForState & (-16777216));
            paint.setColorFilter(this.mMaskColorFilter);
            paint.setShader(this.mMaskShader);
        } else {
            paint.setColor(colorForState);
            paint.setColorFilter(null);
            paint.setShader(null);
        }
        return paint;
    }

    @Override
    public Rect getDirtyBounds() {
        if (!isBounded()) {
            Rect rect = this.mDrawingBounds;
            Rect rect2 = this.mDirtyBounds;
            rect2.set(rect);
            rect.setEmpty();
            int iExactCenterX = (int) this.mHotspotBounds.exactCenterX();
            int iExactCenterY = (int) this.mHotspotBounds.exactCenterY();
            Rect rect3 = this.mTempRect;
            RippleForeground[] rippleForegroundArr = this.mExitingRipples;
            int i = this.mExitingRipplesCount;
            for (int i2 = 0; i2 < i; i2++) {
                rippleForegroundArr[i2].getBounds(rect3);
                rect3.offset(iExactCenterX, iExactCenterY);
                rect.union(rect3);
            }
            RippleBackground rippleBackground = this.mBackground;
            if (rippleBackground != null) {
                rippleBackground.getBounds(rect3);
                rect3.offset(iExactCenterX, iExactCenterY);
                rect.union(rect3);
            }
            rect2.union(rect);
            rect2.union(super.getDirtyBounds());
            return rect2;
        }
        return getBounds();
    }

    public void setForceSoftware(boolean z) {
        this.mForceSoftware = z;
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
    RippleState createConstantState(LayerDrawable.LayerState layerState, Resources resources) {
        return new RippleState(layerState, this, resources);
    }

    static class RippleState extends LayerDrawable.LayerState {
        ColorStateList mColor;
        int mMaxRadius;
        int[] mTouchThemeAttrs;

        public RippleState(LayerDrawable.LayerState layerState, RippleDrawable rippleDrawable, Resources resources) {
            super(layerState, rippleDrawable, resources);
            this.mColor = ColorStateList.valueOf(Color.MAGENTA);
            this.mMaxRadius = -1;
            if (layerState != null && (layerState instanceof RippleState)) {
                RippleState rippleState = (RippleState) layerState;
                this.mTouchThemeAttrs = rippleState.mTouchThemeAttrs;
                this.mColor = rippleState.mColor;
                this.mMaxRadius = rippleState.mMaxRadius;
                if (rippleState.mDensity != this.mDensity) {
                    applyDensityScaling(layerState.mDensity, this.mDensity);
                }
            }
        }

        @Override
        protected void onDensityChanged(int i, int i2) {
            super.onDensityChanged(i, i2);
            applyDensityScaling(i, i2);
        }

        private void applyDensityScaling(int i, int i2) {
            if (this.mMaxRadius != -1) {
                this.mMaxRadius = Drawable.scaleFromDensity(this.mMaxRadius, i, i2, true);
            }
        }

        @Override
        public boolean canApplyTheme() {
            return this.mTouchThemeAttrs != null || (this.mColor != null && this.mColor.canApplyTheme()) || super.canApplyTheme();
        }

        @Override
        public Drawable newDrawable() {
            return new RippleDrawable(this, (Resources) null);
        }

        @Override
        public Drawable newDrawable(Resources resources) {
            return new RippleDrawable(this, resources);
        }

        @Override
        public int getChangingConfigurations() {
            return super.getChangingConfigurations() | (this.mColor != null ? this.mColor.getChangingConfigurations() : 0);
        }
    }

    private RippleDrawable(RippleState rippleState, Resources resources) {
        this.mTempRect = new Rect();
        this.mHotspotBounds = new Rect();
        this.mDrawingBounds = new Rect();
        this.mDirtyBounds = new Rect();
        this.mExitingRipplesCount = 0;
        this.mState = new RippleState(rippleState, this, resources);
        this.mLayerState = this.mState;
        this.mDensity = Drawable.resolveDensity(resources, this.mState.mDensity);
        if (this.mState.mNumChildren > 0) {
            ensurePadding();
            refreshPadding();
        }
        updateLocalState();
    }

    private void updateLocalState() {
        this.mMask = findDrawableByLayerId(16908334);
    }
}
