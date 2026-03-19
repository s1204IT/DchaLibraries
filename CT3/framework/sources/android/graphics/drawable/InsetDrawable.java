package android.graphics.drawable;

import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Insets;
import android.graphics.Outline;
import android.graphics.Rect;
import android.graphics.drawable.DrawableWrapper;
import android.util.AttributeSet;
import com.android.internal.R;
import java.io.IOException;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

public class InsetDrawable extends DrawableWrapper {
    private InsetState mState;
    private final Rect mTmpRect;

    InsetDrawable(InsetState state, Resources res, InsetDrawable insetDrawable) {
        this(state, res);
    }

    InsetDrawable() {
        this(new InsetState(null, null), (Resources) null);
    }

    public InsetDrawable(Drawable drawable, int inset) {
        this(drawable, inset, inset, inset, inset);
    }

    public InsetDrawable(Drawable drawable, int insetLeft, int insetTop, int insetRight, int insetBottom) {
        this(new InsetState(null, null), (Resources) null);
        this.mState.mInsetLeft = insetLeft;
        this.mState.mInsetTop = insetTop;
        this.mState.mInsetRight = insetRight;
        this.mState.mInsetBottom = insetBottom;
        setDrawable(drawable);
    }

    @Override
    public void inflate(Resources r, XmlPullParser parser, AttributeSet attrs, Resources.Theme theme) throws XmlPullParserException, IOException {
        TypedArray a = obtainAttributes(r, theme, attrs, R.styleable.InsetDrawable);
        super.inflate(r, parser, attrs, theme);
        updateStateFromTypedArray(a);
        verifyRequiredAttributes(a);
        a.recycle();
    }

    @Override
    public void applyTheme(Resources.Theme t) {
        super.applyTheme(t);
        InsetState state = this.mState;
        if (state == null || state.mThemeAttrs == null) {
            return;
        }
        TypedArray a = t.resolveAttributes(state.mThemeAttrs, R.styleable.InsetDrawable);
        try {
            updateStateFromTypedArray(a);
            verifyRequiredAttributes(a);
        } catch (XmlPullParserException e) {
            rethrowAsRuntimeException(e);
        } finally {
            a.recycle();
        }
    }

    private void verifyRequiredAttributes(TypedArray a) throws XmlPullParserException {
        if (getDrawable() != null) {
            return;
        }
        if (this.mState.mThemeAttrs != null && this.mState.mThemeAttrs[1] != 0) {
        } else {
            throw new XmlPullParserException(a.getPositionDescription() + ": <inset> tag requires a 'drawable' attribute or child tag defining a drawable");
        }
    }

    private void updateStateFromTypedArray(TypedArray a) {
        InsetState state = this.mState;
        if (state == null) {
            return;
        }
        state.mChangingConfigurations |= a.getChangingConfigurations();
        state.mThemeAttrs = a.extractThemeAttrs();
        if (a.hasValue(6)) {
            int inset = a.getDimensionPixelOffset(6, 0);
            state.mInsetLeft = inset;
            state.mInsetTop = inset;
            state.mInsetRight = inset;
            state.mInsetBottom = inset;
        }
        state.mInsetLeft = a.getDimensionPixelOffset(2, state.mInsetLeft);
        state.mInsetRight = a.getDimensionPixelOffset(3, state.mInsetRight);
        state.mInsetTop = a.getDimensionPixelOffset(4, state.mInsetTop);
        state.mInsetBottom = a.getDimensionPixelOffset(5, state.mInsetBottom);
    }

    @Override
    public boolean getPadding(Rect padding) {
        boolean pad = super.getPadding(padding);
        padding.left += this.mState.mInsetLeft;
        padding.right += this.mState.mInsetRight;
        padding.top += this.mState.mInsetTop;
        padding.bottom += this.mState.mInsetBottom;
        return pad || (((this.mState.mInsetLeft | this.mState.mInsetRight) | this.mState.mInsetTop) | this.mState.mInsetBottom) != 0;
    }

    @Override
    public Insets getOpticalInsets() {
        Insets contentInsets = super.getOpticalInsets();
        return Insets.of(contentInsets.left + this.mState.mInsetLeft, contentInsets.top + this.mState.mInsetTop, contentInsets.right + this.mState.mInsetRight, contentInsets.bottom + this.mState.mInsetBottom);
    }

    @Override
    public int getOpacity() {
        InsetState state = this.mState;
        int opacity = getDrawable().getOpacity();
        if (opacity == -1 && (state.mInsetLeft > 0 || state.mInsetTop > 0 || state.mInsetRight > 0 || state.mInsetBottom > 0)) {
            return -3;
        }
        return opacity;
    }

    @Override
    protected void onBoundsChange(Rect bounds) {
        Rect r = this.mTmpRect;
        r.set(bounds);
        r.left += this.mState.mInsetLeft;
        r.top += this.mState.mInsetTop;
        r.right -= this.mState.mInsetRight;
        r.bottom -= this.mState.mInsetBottom;
        super.onBoundsChange(r);
    }

    @Override
    public int getIntrinsicWidth() {
        int childWidth = getDrawable().getIntrinsicWidth();
        if (childWidth < 0) {
            return -1;
        }
        return this.mState.mInsetLeft + childWidth + this.mState.mInsetRight;
    }

    @Override
    public int getIntrinsicHeight() {
        int childHeight = getDrawable().getIntrinsicHeight();
        if (childHeight < 0) {
            return -1;
        }
        return this.mState.mInsetTop + childHeight + this.mState.mInsetBottom;
    }

    @Override
    public void getOutline(Outline outline) {
        getDrawable().getOutline(outline);
    }

    @Override
    DrawableWrapper.DrawableWrapperState mutateConstantState() {
        this.mState = new InsetState(this.mState, null);
        return this.mState;
    }

    static final class InsetState extends DrawableWrapper.DrawableWrapperState {
        int mInsetBottom;
        int mInsetLeft;
        int mInsetRight;
        int mInsetTop;
        private int[] mThemeAttrs;

        InsetState(InsetState orig, Resources res) {
            super(orig, res);
            this.mInsetLeft = 0;
            this.mInsetTop = 0;
            this.mInsetRight = 0;
            this.mInsetBottom = 0;
            if (orig == null) {
                return;
            }
            this.mInsetLeft = orig.mInsetLeft;
            this.mInsetTop = orig.mInsetTop;
            this.mInsetRight = orig.mInsetRight;
            this.mInsetBottom = orig.mInsetBottom;
            if (orig.mDensity == this.mDensity) {
                return;
            }
            applyDensityScaling(orig.mDensity, this.mDensity);
        }

        @Override
        void onDensityChanged(int sourceDensity, int targetDensity) {
            super.onDensityChanged(sourceDensity, targetDensity);
            applyDensityScaling(sourceDensity, targetDensity);
        }

        private void applyDensityScaling(int sourceDensity, int targetDensity) {
            this.mInsetLeft = Bitmap.scaleFromDensity(this.mInsetLeft, sourceDensity, targetDensity);
            this.mInsetTop = Bitmap.scaleFromDensity(this.mInsetTop, sourceDensity, targetDensity);
            this.mInsetRight = Bitmap.scaleFromDensity(this.mInsetRight, sourceDensity, targetDensity);
            this.mInsetBottom = Bitmap.scaleFromDensity(this.mInsetBottom, sourceDensity, targetDensity);
        }

        @Override
        public Drawable newDrawable(Resources res) {
            InsetState state;
            InsetDrawable insetDrawable = null;
            if (res != null) {
                int densityDpi = res.getDisplayMetrics().densityDpi;
                int density = densityDpi == 0 ? 160 : densityDpi;
                if (density != this.mDensity) {
                    state = new InsetState(this, res);
                } else {
                    state = this;
                }
            } else {
                state = this;
            }
            return new InsetDrawable(state, res, insetDrawable);
        }
    }

    private InsetDrawable(InsetState state, Resources res) {
        super(state, res);
        this.mTmpRect = new Rect();
        this.mState = state;
    }
}
