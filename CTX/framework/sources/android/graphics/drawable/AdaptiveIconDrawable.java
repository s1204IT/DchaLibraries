package android.graphics.drawable;

import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Matrix;
import android.graphics.Outline;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.Region;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.PathParser;
import com.android.internal.R;
import java.io.IOException;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

public class AdaptiveIconDrawable extends Drawable implements Drawable.Callback {
    private static final int BACKGROUND_ID = 0;
    private static final float DEFAULT_VIEW_PORT_SCALE = 0.6666667f;
    private static final float EXTRA_INSET_PERCENTAGE = 0.25f;
    private static final int FOREGROUND_ID = 1;
    public static final float MASK_SIZE = 100.0f;
    private static final float SAFEZONE_SCALE = 0.9166667f;
    private static Path sMask;
    private final Canvas mCanvas;
    private boolean mChildRequestedInvalidation;
    private Rect mHotspotBounds;
    LayerState mLayerState;
    private Bitmap mLayersBitmap;
    private Shader mLayersShader;
    private final Path mMask;
    private Bitmap mMaskBitmap;
    private final Matrix mMaskMatrix;
    private boolean mMutated;
    private Paint mPaint;
    private boolean mSuspendChildInvalidation;
    private final Rect mTmpOutRect;
    private final Region mTransparentRegion;

    AdaptiveIconDrawable() {
        this((LayerState) null, (Resources) null);
    }

    AdaptiveIconDrawable(LayerState layerState, Resources resources) {
        this.mTmpOutRect = new Rect();
        this.mPaint = new Paint(7);
        this.mLayerState = createConstantState(layerState, resources);
        if (sMask == null) {
            sMask = PathParser.createPathFromPathData(Resources.getSystem().getString(R.string.config_icon_mask));
        }
        this.mMask = PathParser.createPathFromPathData(Resources.getSystem().getString(R.string.config_icon_mask));
        this.mMaskMatrix = new Matrix();
        this.mCanvas = new Canvas();
        this.mTransparentRegion = new Region();
    }

    private ChildDrawable createChildDrawable(Drawable drawable) {
        ChildDrawable childDrawable = new ChildDrawable(this.mLayerState.mDensity);
        childDrawable.mDrawable = drawable;
        childDrawable.mDrawable.setCallback(this);
        this.mLayerState.mChildrenChangingConfigurations |= childDrawable.mDrawable.getChangingConfigurations();
        return childDrawable;
    }

    LayerState createConstantState(LayerState layerState, Resources resources) {
        return new LayerState(layerState, this, resources);
    }

    public AdaptiveIconDrawable(Drawable drawable, Drawable drawable2) {
        this((LayerState) null, (Resources) null);
        if (drawable != null) {
            addLayer(0, createChildDrawable(drawable));
        }
        if (drawable2 != null) {
            addLayer(1, createChildDrawable(drawable2));
        }
    }

    private void addLayer(int i, ChildDrawable childDrawable) {
        this.mLayerState.mChildren[i] = childDrawable;
        this.mLayerState.invalidateCache();
    }

    @Override
    public void inflate(Resources resources, XmlPullParser xmlPullParser, AttributeSet attributeSet, Resources.Theme theme) throws XmlPullParserException, IOException {
        super.inflate(resources, xmlPullParser, attributeSet, theme);
        LayerState layerState = this.mLayerState;
        if (layerState == null) {
            return;
        }
        int iResolveDensity = Drawable.resolveDensity(resources, 0);
        layerState.setDensity(iResolveDensity);
        layerState.mSrcDensityOverride = this.mSrcDensityOverride;
        ChildDrawable[] childDrawableArr = layerState.mChildren;
        for (int i = 0; i < layerState.mChildren.length; i++) {
            childDrawableArr[i].setDensity(iResolveDensity);
        }
        inflateLayers(resources, xmlPullParser, attributeSet, theme);
    }

    public static float getExtraInsetFraction() {
        return 0.25f;
    }

    public static float getExtraInsetPercentage() {
        return 0.25f;
    }

    public Path getIconMask() {
        return this.mMask;
    }

    public Drawable getForeground() {
        return this.mLayerState.mChildren[1].mDrawable;
    }

    public Drawable getBackground() {
        return this.mLayerState.mChildren[0].mDrawable;
    }

    @Override
    protected void onBoundsChange(Rect rect) {
        if (rect.isEmpty()) {
            return;
        }
        updateLayerBounds(rect);
    }

    private void updateLayerBounds(Rect rect) {
        if (rect.isEmpty()) {
            return;
        }
        try {
            suspendChildInvalidation();
            updateLayerBoundsInternal(rect);
            updateMaskBoundsInternal(rect);
        } finally {
            resumeChildInvalidation();
        }
    }

    private void updateLayerBoundsInternal(Rect rect) {
        Drawable drawable;
        int iWidth = rect.width() / 2;
        int iHeight = rect.height() / 2;
        LayerState layerState = this.mLayerState;
        for (int i = 0; i < 2; i++) {
            ChildDrawable childDrawable = this.mLayerState.mChildren[i];
            if (childDrawable != null && (drawable = childDrawable.mDrawable) != null) {
                int iWidth2 = (int) (rect.width() / 1.3333334f);
                int iHeight2 = (int) (rect.height() / 1.3333334f);
                Rect rect2 = this.mTmpOutRect;
                rect2.set(iWidth - iWidth2, iHeight - iHeight2, iWidth2 + iWidth, iHeight2 + iHeight);
                drawable.setBounds(rect2);
            }
        }
    }

    private void updateMaskBoundsInternal(Rect rect) {
        this.mMaskMatrix.setScale(rect.width() / 100.0f, rect.height() / 100.0f);
        sMask.transform(this.mMaskMatrix, this.mMask);
        if (this.mMaskBitmap == null || this.mMaskBitmap.getWidth() != rect.width() || this.mMaskBitmap.getHeight() != rect.height()) {
            this.mMaskBitmap = Bitmap.createBitmap(rect.width(), rect.height(), Bitmap.Config.ALPHA_8);
            this.mLayersBitmap = Bitmap.createBitmap(rect.width(), rect.height(), Bitmap.Config.ARGB_8888);
        }
        this.mCanvas.setBitmap(this.mMaskBitmap);
        this.mPaint.setShader(null);
        this.mCanvas.drawPath(this.mMask, this.mPaint);
        this.mMaskMatrix.postTranslate(rect.left, rect.top);
        this.mMask.reset();
        sMask.transform(this.mMaskMatrix, this.mMask);
        this.mTransparentRegion.setEmpty();
        this.mLayersShader = null;
    }

    @Override
    public void draw(Canvas canvas) {
        Drawable drawable;
        if (this.mLayersBitmap == null) {
            return;
        }
        if (this.mLayersShader == null) {
            this.mCanvas.setBitmap(this.mLayersBitmap);
            this.mCanvas.drawColor(-16777216);
            int i = 0;
            while (true) {
                LayerState layerState = this.mLayerState;
                if (i >= 2) {
                    break;
                }
                if (this.mLayerState.mChildren[i] != null && (drawable = this.mLayerState.mChildren[i].mDrawable) != null) {
                    drawable.draw(this.mCanvas);
                }
                i++;
            }
            this.mLayersShader = new BitmapShader(this.mLayersBitmap, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP);
            this.mPaint.setShader(this.mLayersShader);
        }
        if (this.mMaskBitmap != null) {
            Rect bounds = getBounds();
            canvas.drawBitmap(this.mMaskBitmap, bounds.left, bounds.top, this.mPaint);
        }
    }

    @Override
    public void invalidateSelf() {
        this.mLayersShader = null;
        super.invalidateSelf();
    }

    @Override
    public void getOutline(Outline outline) {
        outline.setConvexPath(this.mMask);
    }

    public Region getSafeZone() {
        this.mMaskMatrix.reset();
        this.mMaskMatrix.setScale(SAFEZONE_SCALE, SAFEZONE_SCALE, getBounds().centerX(), getBounds().centerY());
        Path path = new Path();
        this.mMask.transform(this.mMaskMatrix, path);
        Region region = new Region(getBounds());
        region.setPath(path, region);
        return region;
    }

    @Override
    public Region getTransparentRegion() {
        if (this.mTransparentRegion.isEmpty()) {
            this.mMask.toggleInverseFillType();
            this.mTransparentRegion.set(getBounds());
            this.mTransparentRegion.setPath(this.mMask, this.mTransparentRegion);
            this.mMask.toggleInverseFillType();
        }
        return this.mTransparentRegion;
    }

    @Override
    public void applyTheme(Resources.Theme theme) {
        super.applyTheme(theme);
        LayerState layerState = this.mLayerState;
        if (layerState == null) {
            return;
        }
        int iResolveDensity = Drawable.resolveDensity(theme.getResources(), 0);
        layerState.setDensity(iResolveDensity);
        ChildDrawable[] childDrawableArr = layerState.mChildren;
        for (int i = 0; i < 2; i++) {
            ChildDrawable childDrawable = childDrawableArr[i];
            childDrawable.setDensity(iResolveDensity);
            if (childDrawable.mThemeAttrs != null) {
                TypedArray typedArrayResolveAttributes = theme.resolveAttributes(childDrawable.mThemeAttrs, R.styleable.AdaptiveIconDrawableLayer);
                updateLayerFromTypedArray(childDrawable, typedArrayResolveAttributes);
                typedArrayResolveAttributes.recycle();
            }
            Drawable drawable = childDrawable.mDrawable;
            if (drawable != null && drawable.canApplyTheme()) {
                drawable.applyTheme(theme);
                layerState.mChildrenChangingConfigurations = drawable.getChangingConfigurations() | layerState.mChildrenChangingConfigurations;
            }
        }
    }

    private void inflateLayers(Resources resources, XmlPullParser xmlPullParser, AttributeSet attributeSet, Resources.Theme theme) throws XmlPullParserException, IOException {
        int i;
        int next;
        LayerState layerState = this.mLayerState;
        int depth = xmlPullParser.getDepth() + 1;
        while (true) {
            int next2 = xmlPullParser.next();
            if (next2 != 1) {
                int depth2 = xmlPullParser.getDepth();
                if (depth2 >= depth || next2 != 3) {
                    if (next2 == 2 && depth2 <= depth) {
                        String name = xmlPullParser.getName();
                        if (name.equals("background")) {
                            i = 0;
                        } else if (name.equals("foreground")) {
                            i = 1;
                        } else {
                            continue;
                        }
                        ChildDrawable childDrawable = new ChildDrawable(layerState.mDensity);
                        TypedArray typedArrayObtainAttributes = obtainAttributes(resources, theme, attributeSet, R.styleable.AdaptiveIconDrawableLayer);
                        updateLayerFromTypedArray(childDrawable, typedArrayObtainAttributes);
                        typedArrayObtainAttributes.recycle();
                        if (childDrawable.mDrawable == null && childDrawable.mThemeAttrs == null) {
                            do {
                                next = xmlPullParser.next();
                            } while (next == 4);
                            if (next != 2) {
                                throw new XmlPullParserException(xmlPullParser.getPositionDescription() + ": <foreground> or <background> tag requires a 'drawable'attribute or child tag defining a drawable");
                            }
                            childDrawable.mDrawable = Drawable.createFromXmlInnerForDensity(resources, xmlPullParser, attributeSet, this.mLayerState.mSrcDensityOverride, theme);
                            childDrawable.mDrawable.setCallback(this);
                            layerState.mChildrenChangingConfigurations |= childDrawable.mDrawable.getChangingConfigurations();
                        }
                        addLayer(i, childDrawable);
                    }
                } else {
                    return;
                }
            } else {
                return;
            }
        }
    }

    private void updateLayerFromTypedArray(ChildDrawable childDrawable, TypedArray typedArray) {
        LayerState layerState = this.mLayerState;
        layerState.mChildrenChangingConfigurations |= typedArray.getChangingConfigurations();
        childDrawable.mThemeAttrs = typedArray.extractThemeAttrs();
        Drawable drawableForDensity = typedArray.getDrawableForDensity(0, layerState.mSrcDensityOverride);
        if (drawableForDensity != null) {
            if (childDrawable.mDrawable != null) {
                childDrawable.mDrawable.setCallback(null);
            }
            childDrawable.mDrawable = drawableForDensity;
            childDrawable.mDrawable.setCallback(this);
            layerState.mChildrenChangingConfigurations = childDrawable.mDrawable.getChangingConfigurations() | layerState.mChildrenChangingConfigurations;
        }
    }

    @Override
    public boolean canApplyTheme() {
        return (this.mLayerState != null && this.mLayerState.canApplyTheme()) || super.canApplyTheme();
    }

    @Override
    public boolean isProjected() {
        if (super.isProjected()) {
            return true;
        }
        ChildDrawable[] childDrawableArr = this.mLayerState.mChildren;
        int i = 0;
        while (true) {
            LayerState layerState = this.mLayerState;
            if (i >= 2) {
                return false;
            }
            if (childDrawableArr[i].mDrawable.isProjected()) {
                return true;
            }
            i++;
        }
    }

    private void suspendChildInvalidation() {
        this.mSuspendChildInvalidation = true;
    }

    private void resumeChildInvalidation() {
        this.mSuspendChildInvalidation = false;
        if (this.mChildRequestedInvalidation) {
            this.mChildRequestedInvalidation = false;
            invalidateSelf();
        }
    }

    @Override
    public void invalidateDrawable(Drawable drawable) {
        if (this.mSuspendChildInvalidation) {
            this.mChildRequestedInvalidation = true;
        } else {
            invalidateSelf();
        }
    }

    @Override
    public void scheduleDrawable(Drawable drawable, Runnable runnable, long j) {
        scheduleSelf(runnable, j);
    }

    @Override
    public void unscheduleDrawable(Drawable drawable, Runnable runnable) {
        unscheduleSelf(runnable);
    }

    @Override
    public int getChangingConfigurations() {
        return super.getChangingConfigurations() | this.mLayerState.getChangingConfigurations();
    }

    @Override
    public void setHotspot(float f, float f2) {
        ChildDrawable[] childDrawableArr = this.mLayerState.mChildren;
        int i = 0;
        while (true) {
            LayerState layerState = this.mLayerState;
            if (i < 2) {
                Drawable drawable = childDrawableArr[i].mDrawable;
                if (drawable != null) {
                    drawable.setHotspot(f, f2);
                }
                i++;
            } else {
                return;
            }
        }
    }

    @Override
    public void setHotspotBounds(int i, int i2, int i3, int i4) {
        ChildDrawable[] childDrawableArr = this.mLayerState.mChildren;
        int i5 = 0;
        while (true) {
            LayerState layerState = this.mLayerState;
            if (i5 >= 2) {
                break;
            }
            Drawable drawable = childDrawableArr[i5].mDrawable;
            if (drawable != null) {
                drawable.setHotspotBounds(i, i2, i3, i4);
            }
            i5++;
        }
        if (this.mHotspotBounds == null) {
            this.mHotspotBounds = new Rect(i, i2, i3, i4);
        } else {
            this.mHotspotBounds.set(i, i2, i3, i4);
        }
    }

    @Override
    public void getHotspotBounds(Rect rect) {
        if (this.mHotspotBounds != null) {
            rect.set(this.mHotspotBounds);
        } else {
            super.getHotspotBounds(rect);
        }
    }

    @Override
    public boolean setVisible(boolean z, boolean z2) {
        boolean visible = super.setVisible(z, z2);
        ChildDrawable[] childDrawableArr = this.mLayerState.mChildren;
        int i = 0;
        while (true) {
            LayerState layerState = this.mLayerState;
            if (i < 2) {
                Drawable drawable = childDrawableArr[i].mDrawable;
                if (drawable != null) {
                    drawable.setVisible(z, z2);
                }
                i++;
            } else {
                return visible;
            }
        }
    }

    @Override
    public void setDither(boolean z) {
        ChildDrawable[] childDrawableArr = this.mLayerState.mChildren;
        int i = 0;
        while (true) {
            LayerState layerState = this.mLayerState;
            if (i < 2) {
                Drawable drawable = childDrawableArr[i].mDrawable;
                if (drawable != null) {
                    drawable.setDither(z);
                }
                i++;
            } else {
                return;
            }
        }
    }

    @Override
    public void setAlpha(int i) {
        this.mPaint.setAlpha(i);
    }

    @Override
    public int getAlpha() {
        return -3;
    }

    @Override
    public void setColorFilter(ColorFilter colorFilter) {
        ChildDrawable[] childDrawableArr = this.mLayerState.mChildren;
        int i = 0;
        while (true) {
            LayerState layerState = this.mLayerState;
            if (i < 2) {
                Drawable drawable = childDrawableArr[i].mDrawable;
                if (drawable != null) {
                    drawable.setColorFilter(colorFilter);
                }
                i++;
            } else {
                return;
            }
        }
    }

    @Override
    public void setTintList(ColorStateList colorStateList) {
        ChildDrawable[] childDrawableArr = this.mLayerState.mChildren;
        LayerState layerState = this.mLayerState;
        for (int i = 0; i < 2; i++) {
            Drawable drawable = childDrawableArr[i].mDrawable;
            if (drawable != null) {
                drawable.setTintList(colorStateList);
            }
        }
    }

    @Override
    public void setTintMode(PorterDuff.Mode mode) {
        ChildDrawable[] childDrawableArr = this.mLayerState.mChildren;
        LayerState layerState = this.mLayerState;
        for (int i = 0; i < 2; i++) {
            Drawable drawable = childDrawableArr[i].mDrawable;
            if (drawable != null) {
                drawable.setTintMode(mode);
            }
        }
    }

    public void setOpacity(int i) {
        this.mLayerState.mOpacityOverride = i;
    }

    @Override
    public int getOpacity() {
        if (this.mLayerState.mOpacityOverride != 0) {
            return this.mLayerState.mOpacityOverride;
        }
        return this.mLayerState.getOpacity();
    }

    @Override
    public void setAutoMirrored(boolean z) {
        this.mLayerState.mAutoMirrored = z;
        ChildDrawable[] childDrawableArr = this.mLayerState.mChildren;
        int i = 0;
        while (true) {
            LayerState layerState = this.mLayerState;
            if (i < 2) {
                Drawable drawable = childDrawableArr[i].mDrawable;
                if (drawable != null) {
                    drawable.setAutoMirrored(z);
                }
                i++;
            } else {
                return;
            }
        }
    }

    @Override
    public boolean isAutoMirrored() {
        return this.mLayerState.mAutoMirrored;
    }

    @Override
    public void jumpToCurrentState() {
        ChildDrawable[] childDrawableArr = this.mLayerState.mChildren;
        int i = 0;
        while (true) {
            LayerState layerState = this.mLayerState;
            if (i < 2) {
                Drawable drawable = childDrawableArr[i].mDrawable;
                if (drawable != null) {
                    drawable.jumpToCurrentState();
                }
                i++;
            } else {
                return;
            }
        }
    }

    @Override
    public boolean isStateful() {
        return this.mLayerState.isStateful();
    }

    @Override
    public boolean hasFocusStateSpecified() {
        return this.mLayerState.hasFocusStateSpecified();
    }

    @Override
    protected boolean onStateChange(int[] iArr) {
        ChildDrawable[] childDrawableArr = this.mLayerState.mChildren;
        int i = 0;
        boolean z = false;
        while (true) {
            LayerState layerState = this.mLayerState;
            if (i >= 2) {
                break;
            }
            Drawable drawable = childDrawableArr[i].mDrawable;
            if (drawable != null && drawable.isStateful() && drawable.setState(iArr)) {
                z = true;
            }
            i++;
        }
        if (z) {
            updateLayerBounds(getBounds());
        }
        return z;
    }

    @Override
    protected boolean onLevelChange(int i) {
        ChildDrawable[] childDrawableArr = this.mLayerState.mChildren;
        int i2 = 0;
        boolean z = false;
        while (true) {
            LayerState layerState = this.mLayerState;
            if (i2 >= 2) {
                break;
            }
            Drawable drawable = childDrawableArr[i2].mDrawable;
            if (drawable != null && drawable.setLevel(i)) {
                z = true;
            }
            i2++;
        }
        if (z) {
            updateLayerBounds(getBounds());
        }
        return z;
    }

    @Override
    public int getIntrinsicWidth() {
        return (int) (getMaxIntrinsicWidth() * DEFAULT_VIEW_PORT_SCALE);
    }

    private int getMaxIntrinsicWidth() {
        int intrinsicWidth;
        int i = -1;
        int i2 = 0;
        while (true) {
            LayerState layerState = this.mLayerState;
            if (i2 < 2) {
                ChildDrawable childDrawable = this.mLayerState.mChildren[i2];
                if (childDrawable.mDrawable != null && (intrinsicWidth = childDrawable.mDrawable.getIntrinsicWidth()) > i) {
                    i = intrinsicWidth;
                }
                i2++;
            } else {
                return i;
            }
        }
    }

    @Override
    public int getIntrinsicHeight() {
        return (int) (getMaxIntrinsicHeight() * DEFAULT_VIEW_PORT_SCALE);
    }

    private int getMaxIntrinsicHeight() {
        int intrinsicHeight;
        int i = -1;
        int i2 = 0;
        while (true) {
            LayerState layerState = this.mLayerState;
            if (i2 < 2) {
                ChildDrawable childDrawable = this.mLayerState.mChildren[i2];
                if (childDrawable.mDrawable != null && (intrinsicHeight = childDrawable.mDrawable.getIntrinsicHeight()) > i) {
                    i = intrinsicHeight;
                }
                i2++;
            } else {
                return i;
            }
        }
    }

    @Override
    public Drawable.ConstantState getConstantState() {
        if (this.mLayerState.canConstantState()) {
            this.mLayerState.mChangingConfigurations = getChangingConfigurations();
            return this.mLayerState;
        }
        return null;
    }

    @Override
    public Drawable mutate() {
        if (!this.mMutated && super.mutate() == this) {
            this.mLayerState = createConstantState(this.mLayerState, null);
            int i = 0;
            while (true) {
                LayerState layerState = this.mLayerState;
                if (i >= 2) {
                    break;
                }
                Drawable drawable = this.mLayerState.mChildren[i].mDrawable;
                if (drawable != null) {
                    drawable.mutate();
                }
                i++;
            }
            this.mMutated = true;
        }
        return this;
    }

    @Override
    public void clearMutated() {
        super.clearMutated();
        ChildDrawable[] childDrawableArr = this.mLayerState.mChildren;
        int i = 0;
        while (true) {
            LayerState layerState = this.mLayerState;
            if (i < 2) {
                Drawable drawable = childDrawableArr[i].mDrawable;
                if (drawable != null) {
                    drawable.clearMutated();
                }
                i++;
            } else {
                this.mMutated = false;
                return;
            }
        }
    }

    static class ChildDrawable {
        public int mDensity;
        public Drawable mDrawable;
        public int[] mThemeAttrs;

        ChildDrawable(int i) {
            this.mDensity = 160;
            this.mDensity = i;
        }

        ChildDrawable(ChildDrawable childDrawable, AdaptiveIconDrawable adaptiveIconDrawable, Resources resources) {
            Drawable drawableNewDrawable;
            this.mDensity = 160;
            Drawable drawable = childDrawable.mDrawable;
            if (drawable != null) {
                Drawable.ConstantState constantState = drawable.getConstantState();
                if (constantState != null) {
                    if (resources != null) {
                        drawableNewDrawable = constantState.newDrawable(resources);
                    } else {
                        drawableNewDrawable = constantState.newDrawable();
                    }
                } else {
                    drawableNewDrawable = drawable;
                }
                drawableNewDrawable.setCallback(adaptiveIconDrawable);
                drawableNewDrawable.setBounds(drawable.getBounds());
                drawableNewDrawable.setLevel(drawable.getLevel());
            } else {
                drawableNewDrawable = null;
            }
            this.mDrawable = drawableNewDrawable;
            this.mThemeAttrs = childDrawable.mThemeAttrs;
            this.mDensity = Drawable.resolveDensity(resources, childDrawable.mDensity);
        }

        public boolean canApplyTheme() {
            return this.mThemeAttrs != null || (this.mDrawable != null && this.mDrawable.canApplyTheme());
        }

        public final void setDensity(int i) {
            if (this.mDensity != i) {
                this.mDensity = i;
            }
        }
    }

    static class LayerState extends Drawable.ConstantState {
        static final int N_CHILDREN = 2;
        private boolean mAutoMirrored;
        int mChangingConfigurations;
        private boolean mCheckedOpacity;
        private boolean mCheckedStateful;
        ChildDrawable[] mChildren;
        int mChildrenChangingConfigurations;
        int mDensity;
        private boolean mIsStateful;
        private int mOpacity;
        int mOpacityOverride;
        int mSrcDensityOverride;
        private int[] mThemeAttrs;

        LayerState(LayerState layerState, AdaptiveIconDrawable adaptiveIconDrawable, Resources resources) {
            int i = 0;
            this.mSrcDensityOverride = 0;
            this.mOpacityOverride = 0;
            this.mAutoMirrored = false;
            this.mDensity = Drawable.resolveDensity(resources, layerState != null ? layerState.mDensity : 0);
            this.mChildren = new ChildDrawable[2];
            if (layerState != null) {
                ChildDrawable[] childDrawableArr = layerState.mChildren;
                this.mChangingConfigurations = layerState.mChangingConfigurations;
                this.mChildrenChangingConfigurations = layerState.mChildrenChangingConfigurations;
                while (i < 2) {
                    this.mChildren[i] = new ChildDrawable(childDrawableArr[i], adaptiveIconDrawable, resources);
                    i++;
                }
                this.mCheckedOpacity = layerState.mCheckedOpacity;
                this.mOpacity = layerState.mOpacity;
                this.mCheckedStateful = layerState.mCheckedStateful;
                this.mIsStateful = layerState.mIsStateful;
                this.mAutoMirrored = layerState.mAutoMirrored;
                this.mThemeAttrs = layerState.mThemeAttrs;
                this.mOpacityOverride = layerState.mOpacityOverride;
                this.mSrcDensityOverride = layerState.mSrcDensityOverride;
                return;
            }
            while (i < 2) {
                this.mChildren[i] = new ChildDrawable(this.mDensity);
                i++;
            }
        }

        public final void setDensity(int i) {
            if (this.mDensity != i) {
                this.mDensity = i;
            }
        }

        @Override
        public boolean canApplyTheme() {
            if (this.mThemeAttrs != null || super.canApplyTheme()) {
                return true;
            }
            ChildDrawable[] childDrawableArr = this.mChildren;
            for (int i = 0; i < 2; i++) {
                if (childDrawableArr[i].canApplyTheme()) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public Drawable newDrawable() {
            return new AdaptiveIconDrawable(this, (Resources) null);
        }

        @Override
        public Drawable newDrawable(Resources resources) {
            return new AdaptiveIconDrawable(this, resources);
        }

        @Override
        public int getChangingConfigurations() {
            return this.mChangingConfigurations | this.mChildrenChangingConfigurations;
        }

        public final int getOpacity() {
            int iResolveOpacity;
            if (this.mCheckedOpacity) {
                return this.mOpacity;
            }
            ChildDrawable[] childDrawableArr = this.mChildren;
            int i = -1;
            int i2 = 0;
            while (true) {
                if (i2 >= 2) {
                    break;
                }
                if (childDrawableArr[i2].mDrawable == null) {
                    i2++;
                } else {
                    i = i2;
                    break;
                }
            }
            if (i >= 0) {
                iResolveOpacity = childDrawableArr[i].mDrawable.getOpacity();
            } else {
                iResolveOpacity = -2;
            }
            for (int i3 = i + 1; i3 < 2; i3++) {
                Drawable drawable = childDrawableArr[i3].mDrawable;
                if (drawable != null) {
                    iResolveOpacity = Drawable.resolveOpacity(iResolveOpacity, drawable.getOpacity());
                }
            }
            this.mOpacity = iResolveOpacity;
            this.mCheckedOpacity = true;
            return iResolveOpacity;
        }

        public final boolean isStateful() {
            if (this.mCheckedStateful) {
                return this.mIsStateful;
            }
            ChildDrawable[] childDrawableArr = this.mChildren;
            boolean z = false;
            int i = 0;
            while (true) {
                if (i < 2) {
                    Drawable drawable = childDrawableArr[i].mDrawable;
                    if (drawable == null || !drawable.isStateful()) {
                        i++;
                    } else {
                        z = true;
                        break;
                    }
                } else {
                    break;
                }
            }
            this.mIsStateful = z;
            this.mCheckedStateful = true;
            return z;
        }

        public final boolean hasFocusStateSpecified() {
            ChildDrawable[] childDrawableArr = this.mChildren;
            for (int i = 0; i < 2; i++) {
                Drawable drawable = childDrawableArr[i].mDrawable;
                if (drawable != null && drawable.hasFocusStateSpecified()) {
                    return true;
                }
            }
            return false;
        }

        public final boolean canConstantState() {
            ChildDrawable[] childDrawableArr = this.mChildren;
            for (int i = 0; i < 2; i++) {
                Drawable drawable = childDrawableArr[i].mDrawable;
                if (drawable != null && drawable.getConstantState() == null) {
                    return false;
                }
            }
            return true;
        }

        public void invalidateCache() {
            this.mCheckedOpacity = false;
            this.mCheckedStateful = false;
        }
    }
}
