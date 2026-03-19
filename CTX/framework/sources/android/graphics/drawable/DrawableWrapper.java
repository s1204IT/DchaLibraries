package android.graphics.drawable;

import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Insets;
import android.graphics.Outline;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import com.android.internal.R;
import java.io.IOException;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

public abstract class DrawableWrapper extends Drawable implements Drawable.Callback {
    private Drawable mDrawable;
    private boolean mMutated;
    private DrawableWrapperState mState;

    DrawableWrapper(DrawableWrapperState drawableWrapperState, Resources resources) {
        this.mState = drawableWrapperState;
        updateLocalState(resources);
    }

    public DrawableWrapper(Drawable drawable) {
        this.mState = null;
        this.mDrawable = drawable;
    }

    private void updateLocalState(Resources resources) {
        if (this.mState != null && this.mState.mDrawableState != null) {
            setDrawable(this.mState.mDrawableState.newDrawable(resources));
        }
    }

    public void setDrawable(Drawable drawable) {
        if (this.mDrawable != null) {
            this.mDrawable.setCallback(null);
        }
        this.mDrawable = drawable;
        if (drawable != null) {
            drawable.setCallback(this);
            drawable.setVisible(isVisible(), true);
            drawable.setState(getState());
            drawable.setLevel(getLevel());
            drawable.setBounds(getBounds());
            drawable.setLayoutDirection(getLayoutDirection());
            if (this.mState != null) {
                this.mState.mDrawableState = drawable.getConstantState();
            }
        }
        invalidateSelf();
    }

    public Drawable getDrawable() {
        return this.mDrawable;
    }

    @Override
    public void inflate(Resources resources, XmlPullParser xmlPullParser, AttributeSet attributeSet, Resources.Theme theme) throws XmlPullParserException, IOException {
        super.inflate(resources, xmlPullParser, attributeSet, theme);
        DrawableWrapperState drawableWrapperState = this.mState;
        if (drawableWrapperState == null) {
            return;
        }
        int i = resources.getDisplayMetrics().densityDpi;
        if (i == 0) {
            i = 160;
        }
        drawableWrapperState.setDensity(i);
        drawableWrapperState.mSrcDensityOverride = this.mSrcDensityOverride;
        TypedArray typedArrayObtainAttributes = obtainAttributes(resources, theme, attributeSet, R.styleable.DrawableWrapper);
        updateStateFromTypedArray(typedArrayObtainAttributes);
        typedArrayObtainAttributes.recycle();
        inflateChildDrawable(resources, xmlPullParser, attributeSet, theme);
    }

    @Override
    public void applyTheme(Resources.Theme theme) {
        super.applyTheme(theme);
        if (this.mDrawable != null && this.mDrawable.canApplyTheme()) {
            this.mDrawable.applyTheme(theme);
        }
        DrawableWrapperState drawableWrapperState = this.mState;
        if (drawableWrapperState == null) {
            return;
        }
        int i = theme.getResources().getDisplayMetrics().densityDpi;
        if (i == 0) {
            i = 160;
        }
        drawableWrapperState.setDensity(i);
        if (drawableWrapperState.mThemeAttrs != null) {
            TypedArray typedArrayResolveAttributes = theme.resolveAttributes(drawableWrapperState.mThemeAttrs, R.styleable.DrawableWrapper);
            updateStateFromTypedArray(typedArrayResolveAttributes);
            typedArrayResolveAttributes.recycle();
        }
    }

    private void updateStateFromTypedArray(TypedArray typedArray) {
        DrawableWrapperState drawableWrapperState = this.mState;
        if (drawableWrapperState == null) {
            return;
        }
        drawableWrapperState.mChangingConfigurations |= typedArray.getChangingConfigurations();
        drawableWrapperState.mThemeAttrs = typedArray.extractThemeAttrs();
        if (typedArray.hasValueOrEmpty(0)) {
            setDrawable(typedArray.getDrawable(0));
        }
    }

    @Override
    public boolean canApplyTheme() {
        return (this.mState != null && this.mState.canApplyTheme()) || super.canApplyTheme();
    }

    @Override
    public void invalidateDrawable(Drawable drawable) {
        Drawable.Callback callback = getCallback();
        if (callback != null) {
            callback.invalidateDrawable(this);
        }
    }

    @Override
    public void scheduleDrawable(Drawable drawable, Runnable runnable, long j) {
        Drawable.Callback callback = getCallback();
        if (callback != null) {
            callback.scheduleDrawable(this, runnable, j);
        }
    }

    @Override
    public void unscheduleDrawable(Drawable drawable, Runnable runnable) {
        Drawable.Callback callback = getCallback();
        if (callback != null) {
            callback.unscheduleDrawable(this, runnable);
        }
    }

    @Override
    public void draw(Canvas canvas) {
        if (this.mDrawable != null) {
            this.mDrawable.draw(canvas);
        }
    }

    @Override
    public int getChangingConfigurations() {
        return super.getChangingConfigurations() | (this.mState != null ? this.mState.getChangingConfigurations() : 0) | this.mDrawable.getChangingConfigurations();
    }

    @Override
    public boolean getPadding(Rect rect) {
        return this.mDrawable != null && this.mDrawable.getPadding(rect);
    }

    @Override
    public Insets getOpticalInsets() {
        return this.mDrawable != null ? this.mDrawable.getOpticalInsets() : Insets.NONE;
    }

    @Override
    public void setHotspot(float f, float f2) {
        if (this.mDrawable != null) {
            this.mDrawable.setHotspot(f, f2);
        }
    }

    @Override
    public void setHotspotBounds(int i, int i2, int i3, int i4) {
        if (this.mDrawable != null) {
            this.mDrawable.setHotspotBounds(i, i2, i3, i4);
        }
    }

    @Override
    public void getHotspotBounds(Rect rect) {
        if (this.mDrawable != null) {
            this.mDrawable.getHotspotBounds(rect);
        } else {
            rect.set(getBounds());
        }
    }

    @Override
    public boolean setVisible(boolean z, boolean z2) {
        return (this.mDrawable != null && this.mDrawable.setVisible(z, z2)) | super.setVisible(z, z2);
    }

    @Override
    public void setAlpha(int i) {
        if (this.mDrawable != null) {
            this.mDrawable.setAlpha(i);
        }
    }

    @Override
    public int getAlpha() {
        if (this.mDrawable != null) {
            return this.mDrawable.getAlpha();
        }
        return 255;
    }

    @Override
    public void setColorFilter(ColorFilter colorFilter) {
        if (this.mDrawable != null) {
            this.mDrawable.setColorFilter(colorFilter);
        }
    }

    @Override
    public ColorFilter getColorFilter() {
        Drawable drawable = getDrawable();
        if (drawable != null) {
            return drawable.getColorFilter();
        }
        return super.getColorFilter();
    }

    @Override
    public void setTintList(ColorStateList colorStateList) {
        if (this.mDrawable != null) {
            this.mDrawable.setTintList(colorStateList);
        }
    }

    @Override
    public void setTintMode(PorterDuff.Mode mode) {
        if (this.mDrawable != null) {
            this.mDrawable.setTintMode(mode);
        }
    }

    @Override
    public boolean onLayoutDirectionChanged(int i) {
        return this.mDrawable != null && this.mDrawable.setLayoutDirection(i);
    }

    @Override
    public int getOpacity() {
        if (this.mDrawable != null) {
            return this.mDrawable.getOpacity();
        }
        return -2;
    }

    @Override
    public boolean isStateful() {
        return this.mDrawable != null && this.mDrawable.isStateful();
    }

    @Override
    public boolean hasFocusStateSpecified() {
        return this.mDrawable != null && this.mDrawable.hasFocusStateSpecified();
    }

    @Override
    protected boolean onStateChange(int[] iArr) {
        if (this.mDrawable != null && this.mDrawable.isStateful()) {
            boolean state = this.mDrawable.setState(iArr);
            if (state) {
                onBoundsChange(getBounds());
            }
            return state;
        }
        return false;
    }

    @Override
    protected boolean onLevelChange(int i) {
        return this.mDrawable != null && this.mDrawable.setLevel(i);
    }

    @Override
    protected void onBoundsChange(Rect rect) {
        if (this.mDrawable != null) {
            this.mDrawable.setBounds(rect);
        }
    }

    @Override
    public int getIntrinsicWidth() {
        if (this.mDrawable != null) {
            return this.mDrawable.getIntrinsicWidth();
        }
        return -1;
    }

    @Override
    public int getIntrinsicHeight() {
        if (this.mDrawable != null) {
            return this.mDrawable.getIntrinsicHeight();
        }
        return -1;
    }

    @Override
    public void getOutline(Outline outline) {
        if (this.mDrawable != null) {
            this.mDrawable.getOutline(outline);
        } else {
            super.getOutline(outline);
        }
    }

    @Override
    public Drawable.ConstantState getConstantState() {
        if (this.mState != null && this.mState.canConstantState()) {
            this.mState.mChangingConfigurations = getChangingConfigurations();
            return this.mState;
        }
        return null;
    }

    @Override
    public Drawable mutate() {
        if (!this.mMutated && super.mutate() == this) {
            this.mState = mutateConstantState();
            if (this.mDrawable != null) {
                this.mDrawable.mutate();
            }
            if (this.mState != null) {
                this.mState.mDrawableState = this.mDrawable != null ? this.mDrawable.getConstantState() : null;
            }
            this.mMutated = true;
        }
        return this;
    }

    DrawableWrapperState mutateConstantState() {
        return this.mState;
    }

    @Override
    public void clearMutated() {
        super.clearMutated();
        if (this.mDrawable != null) {
            this.mDrawable.clearMutated();
        }
        this.mMutated = false;
    }

    private void inflateChildDrawable(Resources resources, XmlPullParser xmlPullParser, AttributeSet attributeSet, Resources.Theme theme) throws XmlPullParserException, IOException {
        int depth = xmlPullParser.getDepth();
        Drawable drawableCreateFromXmlInnerForDensity = null;
        while (true) {
            int next = xmlPullParser.next();
            if (next == 1 || (next == 3 && xmlPullParser.getDepth() <= depth)) {
                break;
            } else if (next == 2) {
                drawableCreateFromXmlInnerForDensity = Drawable.createFromXmlInnerForDensity(resources, xmlPullParser, attributeSet, this.mState.mSrcDensityOverride, theme);
            }
        }
        if (drawableCreateFromXmlInnerForDensity != null) {
            setDrawable(drawableCreateFromXmlInnerForDensity);
        }
    }

    static abstract class DrawableWrapperState extends Drawable.ConstantState {
        int mChangingConfigurations;
        int mDensity;
        Drawable.ConstantState mDrawableState;
        int mSrcDensityOverride;
        private int[] mThemeAttrs;

        @Override
        public abstract Drawable newDrawable(Resources resources);

        DrawableWrapperState(DrawableWrapperState drawableWrapperState, Resources resources) {
            int i;
            this.mDensity = 160;
            this.mSrcDensityOverride = 0;
            if (drawableWrapperState != null) {
                this.mThemeAttrs = drawableWrapperState.mThemeAttrs;
                this.mChangingConfigurations = drawableWrapperState.mChangingConfigurations;
                this.mDrawableState = drawableWrapperState.mDrawableState;
                this.mSrcDensityOverride = drawableWrapperState.mSrcDensityOverride;
            }
            if (resources != null) {
                i = resources.getDisplayMetrics().densityDpi;
            } else if (drawableWrapperState != null) {
                i = drawableWrapperState.mDensity;
            } else {
                i = 0;
            }
            this.mDensity = i == 0 ? 160 : i;
        }

        public final void setDensity(int i) {
            if (this.mDensity != i) {
                int i2 = this.mDensity;
                this.mDensity = i;
                onDensityChanged(i2, i);
            }
        }

        void onDensityChanged(int i, int i2) {
        }

        @Override
        public boolean canApplyTheme() {
            return this.mThemeAttrs != null || (this.mDrawableState != null && this.mDrawableState.canApplyTheme()) || super.canApplyTheme();
        }

        @Override
        public Drawable newDrawable() {
            return newDrawable(null);
        }

        @Override
        public int getChangingConfigurations() {
            return this.mChangingConfigurations | (this.mDrawableState != null ? this.mDrawableState.getChangingConfigurations() : 0);
        }

        public boolean canConstantState() {
            return this.mDrawableState != null;
        }
    }
}
