package android.graphics.drawable;

import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
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
import java.util.Collection;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

public abstract class DrawableWrapper extends Drawable implements Drawable.Callback {
    private Drawable mDrawable;
    private boolean mMutated;
    private DrawableWrapperState mState;

    DrawableWrapper(DrawableWrapperState state, Resources res) {
        this.mState = state;
        updateLocalState(res);
    }

    public DrawableWrapper(Drawable dr) {
        this.mState = null;
        this.mDrawable = dr;
    }

    private void updateLocalState(Resources res) {
        if (this.mState == null || this.mState.mDrawableState == null) {
            return;
        }
        Drawable dr = this.mState.mDrawableState.newDrawable(res);
        setDrawable(dr);
    }

    public void setDrawable(Drawable dr) {
        if (this.mDrawable != null) {
            this.mDrawable.setCallback(null);
        }
        this.mDrawable = dr;
        if (dr != null) {
            dr.setCallback(this);
            dr.setVisible(isVisible(), true);
            dr.setState(getState());
            dr.setLevel(getLevel());
            dr.setBounds(getBounds());
            dr.setLayoutDirection(getLayoutDirection());
            if (this.mState != null) {
                this.mState.mDrawableState = dr.getConstantState();
            }
        }
        invalidateSelf();
    }

    public Drawable getDrawable() {
        return this.mDrawable;
    }

    @Override
    public void inflate(Resources r, XmlPullParser parser, AttributeSet attrs, Resources.Theme theme) throws XmlPullParserException, IOException {
        super.inflate(r, parser, attrs, theme);
        DrawableWrapperState state = this.mState;
        if (state == null) {
            return;
        }
        int densityDpi = r.getDisplayMetrics().densityDpi;
        int targetDensity = densityDpi == 0 ? 160 : densityDpi;
        state.setDensity(targetDensity);
        TypedArray a = obtainAttributes(r, theme, attrs, R.styleable.DrawableWrapper);
        updateStateFromTypedArray(a);
        a.recycle();
        inflateChildDrawable(r, parser, attrs, theme);
    }

    @Override
    public void applyTheme(Resources.Theme t) {
        super.applyTheme(t);
        if (this.mDrawable != null && this.mDrawable.canApplyTheme()) {
            this.mDrawable.applyTheme(t);
        }
        DrawableWrapperState state = this.mState;
        if (state == null) {
            return;
        }
        int densityDpi = t.getResources().getDisplayMetrics().densityDpi;
        int density = densityDpi == 0 ? 160 : densityDpi;
        state.setDensity(density);
        if (state.mThemeAttrs == null) {
            return;
        }
        TypedArray a = t.resolveAttributes(state.mThemeAttrs, R.styleable.DrawableWrapper);
        updateStateFromTypedArray(a);
        a.recycle();
    }

    private void updateStateFromTypedArray(TypedArray a) {
        DrawableWrapperState state = this.mState;
        if (state == null) {
            return;
        }
        state.mChangingConfigurations |= a.getChangingConfigurations();
        state.mThemeAttrs = a.extractThemeAttrs();
        if (!a.hasValueOrEmpty(0)) {
            return;
        }
        setDrawable(a.getDrawable(0));
    }

    @Override
    public boolean canApplyTheme() {
        if (this.mState == null || !this.mState.canApplyTheme()) {
            return super.canApplyTheme();
        }
        return true;
    }

    @Override
    public void invalidateDrawable(Drawable who) {
        Drawable.Callback callback = getCallback();
        if (callback == null) {
            return;
        }
        callback.invalidateDrawable(this);
    }

    @Override
    public void scheduleDrawable(Drawable who, Runnable what, long when) {
        Drawable.Callback callback = getCallback();
        if (callback == null) {
            return;
        }
        callback.scheduleDrawable(this, what, when);
    }

    @Override
    public void unscheduleDrawable(Drawable who, Runnable what) {
        Drawable.Callback callback = getCallback();
        if (callback == null) {
            return;
        }
        callback.unscheduleDrawable(this, what);
    }

    @Override
    public void draw(Canvas canvas) {
        if (this.mDrawable == null) {
            return;
        }
        this.mDrawable.draw(canvas);
    }

    @Override
    public int getChangingConfigurations() {
        return (this.mState != null ? this.mState.getChangingConfigurations() : 0) | super.getChangingConfigurations() | this.mDrawable.getChangingConfigurations();
    }

    @Override
    public boolean getPadding(Rect padding) {
        if (this.mDrawable != null) {
            return this.mDrawable.getPadding(padding);
        }
        return false;
    }

    @Override
    public Insets getOpticalInsets() {
        return this.mDrawable != null ? this.mDrawable.getOpticalInsets() : Insets.NONE;
    }

    @Override
    public void setHotspot(float x, float y) {
        if (this.mDrawable == null) {
            return;
        }
        this.mDrawable.setHotspot(x, y);
    }

    @Override
    public void setHotspotBounds(int left, int top, int right, int bottom) {
        if (this.mDrawable == null) {
            return;
        }
        this.mDrawable.setHotspotBounds(left, top, right, bottom);
    }

    @Override
    public void getHotspotBounds(Rect outRect) {
        if (this.mDrawable != null) {
            this.mDrawable.getHotspotBounds(outRect);
        } else {
            outRect.set(getBounds());
        }
    }

    @Override
    public boolean setVisible(boolean visible, boolean restart) {
        boolean superChanged = super.setVisible(visible, restart);
        boolean changed = this.mDrawable != null ? this.mDrawable.setVisible(visible, restart) : false;
        return superChanged | changed;
    }

    @Override
    public void setAlpha(int alpha) {
        if (this.mDrawable == null) {
            return;
        }
        this.mDrawable.setAlpha(alpha);
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
        if (this.mDrawable == null) {
            return;
        }
        this.mDrawable.setColorFilter(colorFilter);
    }

    @Override
    public void setTintList(ColorStateList tint) {
        if (this.mDrawable == null) {
            return;
        }
        this.mDrawable.setTintList(tint);
    }

    @Override
    public void setTintMode(PorterDuff.Mode tintMode) {
        if (this.mDrawable == null) {
            return;
        }
        this.mDrawable.setTintMode(tintMode);
    }

    @Override
    public boolean onLayoutDirectionChanged(int layoutDirection) {
        if (this.mDrawable != null) {
            return this.mDrawable.setLayoutDirection(layoutDirection);
        }
        return false;
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
        if (this.mDrawable != null) {
            return this.mDrawable.isStateful();
        }
        return false;
    }

    @Override
    protected boolean onStateChange(int[] state) {
        if (this.mDrawable != null && this.mDrawable.isStateful()) {
            boolean changed = this.mDrawable.setState(state);
            if (changed) {
                onBoundsChange(getBounds());
            }
            return changed;
        }
        return false;
    }

    @Override
    protected boolean onLevelChange(int level) {
        if (this.mDrawable != null) {
            return this.mDrawable.setLevel(level);
        }
        return false;
    }

    @Override
    protected void onBoundsChange(Rect bounds) {
        if (this.mDrawable == null) {
            return;
        }
        this.mDrawable.setBounds(bounds);
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
        if (this.mState == null || !this.mState.canConstantState()) {
            return null;
        }
        this.mState.mChangingConfigurations = getChangingConfigurations();
        return this.mState;
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

    private void inflateChildDrawable(Resources r, XmlPullParser parser, AttributeSet attrs, Resources.Theme theme) throws XmlPullParserException, IOException {
        Drawable dr = null;
        int outerDepth = parser.getDepth();
        while (true) {
            int type = parser.next();
            if (type == 1 || (type == 3 && parser.getDepth() <= outerDepth)) {
                break;
            } else if (type == 2) {
                dr = Drawable.createFromXmlInner(r, parser, attrs, theme);
            }
        }
        if (dr == null) {
            return;
        }
        setDrawable(dr);
    }

    static abstract class DrawableWrapperState extends Drawable.ConstantState {
        int mChangingConfigurations;
        int mDensity;
        Drawable.ConstantState mDrawableState;
        private int[] mThemeAttrs;

        @Override
        public abstract Drawable newDrawable(Resources resources);

        DrawableWrapperState(DrawableWrapperState orig, Resources res) {
            int density;
            this.mDensity = 160;
            if (orig != null) {
                this.mThemeAttrs = orig.mThemeAttrs;
                this.mChangingConfigurations = orig.mChangingConfigurations;
                this.mDrawableState = orig.mDrawableState;
            }
            if (res != null) {
                density = res.getDisplayMetrics().densityDpi;
            } else if (orig != null) {
                density = orig.mDensity;
            } else {
                density = 0;
            }
            this.mDensity = density == 0 ? 160 : density;
        }

        public final void setDensity(int targetDensity) {
            if (this.mDensity == targetDensity) {
                return;
            }
            int sourceDensity = this.mDensity;
            this.mDensity = targetDensity;
            onDensityChanged(sourceDensity, targetDensity);
        }

        void onDensityChanged(int sourceDensity, int targetDensity) {
        }

        @Override
        public boolean canApplyTheme() {
            if (this.mThemeAttrs != null || (this.mDrawableState != null && this.mDrawableState.canApplyTheme())) {
                return true;
            }
            return super.canApplyTheme();
        }

        @Override
        public int addAtlasableBitmaps(Collection<Bitmap> atlasList) {
            Drawable.ConstantState state = this.mDrawableState;
            if (state != null) {
                return state.addAtlasableBitmaps(atlasList);
            }
            return 0;
        }

        @Override
        public Drawable newDrawable() {
            return newDrawable(null);
        }

        @Override
        public int getChangingConfigurations() {
            return (this.mDrawableState != null ? this.mDrawableState.getChangingConfigurations() : 0) | this.mChangingConfigurations;
        }

        public boolean canConstantState() {
            return this.mDrawableState != null;
        }
    }
}
