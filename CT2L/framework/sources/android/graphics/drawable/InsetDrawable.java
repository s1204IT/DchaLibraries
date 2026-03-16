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

public class InsetDrawable extends Drawable implements Drawable.Callback {
    private boolean mMutated;
    private final InsetState mState;
    private final Rect mTmpRect;

    InsetDrawable() {
        this((InsetState) null, (Resources) null);
    }

    public InsetDrawable(Drawable drawable, int inset) {
        this(drawable, inset, inset, inset, inset);
    }

    public InsetDrawable(Drawable drawable, int insetLeft, int insetTop, int insetRight, int insetBottom) {
        this((InsetState) null, (Resources) null);
        this.mState.mDrawable = drawable;
        this.mState.mInsetLeft = insetLeft;
        this.mState.mInsetTop = insetTop;
        this.mState.mInsetRight = insetRight;
        this.mState.mInsetBottom = insetBottom;
        if (drawable != null) {
            drawable.setCallback(this);
        }
    }

    @Override
    public void inflate(Resources r, XmlPullParser parser, AttributeSet attrs, Resources.Theme theme) throws XmlPullParserException, IOException {
        TypedArray a = obtainAttributes(r, theme, attrs, R.styleable.InsetDrawable);
        super.inflateWithAttributes(r, parser, a, 0);
        this.mState.mDrawable = null;
        updateStateFromTypedArray(a);
        inflateChildElements(r, parser, attrs, theme);
        verifyRequiredAttributes(a);
        a.recycle();
    }

    private void inflateChildElements(Resources r, XmlPullParser parser, AttributeSet attrs, Resources.Theme theme) throws XmlPullParserException, IOException {
        int type;
        if (this.mState.mDrawable == null) {
            do {
                type = parser.next();
            } while (type == 4);
            if (type != 2) {
                throw new XmlPullParserException(parser.getPositionDescription() + ": <inset> tag requires a 'drawable' attribute or child tag defining a drawable");
            }
            Drawable dr = Drawable.createFromXmlInner(r, parser, attrs, theme);
            this.mState.mDrawable = dr;
            dr.setCallback(this);
        }
    }

    private void verifyRequiredAttributes(TypedArray a) throws XmlPullParserException {
        if (this.mState.mDrawable == null) {
            if (this.mState.mThemeAttrs == null || this.mState.mThemeAttrs[1] == 0) {
                throw new XmlPullParserException(a.getPositionDescription() + ": <inset> tag requires a 'drawable' attribute or child tag defining a drawable");
            }
        }
    }

    private void updateStateFromTypedArray(TypedArray a) throws XmlPullParserException {
        InsetState state = this.mState;
        state.mChangingConfigurations |= a.getChangingConfigurations();
        state.mThemeAttrs = a.extractThemeAttrs();
        int N = a.getIndexCount();
        for (int i = 0; i < N; i++) {
            int attr = a.getIndex(i);
            switch (attr) {
                case 1:
                    Drawable dr = a.getDrawable(attr);
                    if (dr != null) {
                        state.mDrawable = dr;
                        dr.setCallback(this);
                    }
                    break;
                case 2:
                    state.mInsetLeft = a.getDimensionPixelOffset(attr, state.mInsetLeft);
                    break;
                case 3:
                    state.mInsetRight = a.getDimensionPixelOffset(attr, state.mInsetRight);
                    break;
                case 4:
                    state.mInsetTop = a.getDimensionPixelOffset(attr, state.mInsetTop);
                    break;
                case 5:
                    state.mInsetBottom = a.getDimensionPixelOffset(attr, state.mInsetBottom);
                    break;
                case 6:
                    int inset = a.getDimensionPixelOffset(attr, Integer.MIN_VALUE);
                    if (inset != Integer.MIN_VALUE) {
                        state.mInsetLeft = inset;
                        state.mInsetTop = inset;
                        state.mInsetRight = inset;
                        state.mInsetBottom = inset;
                    }
                    break;
            }
        }
    }

    @Override
    public void applyTheme(Resources.Theme t) {
        super.applyTheme(t);
        InsetState state = this.mState;
        if (state != null) {
            if (state.mThemeAttrs != null) {
                TypedArray a = t.resolveAttributes(state.mThemeAttrs, R.styleable.InsetDrawable);
                try {
                    try {
                        updateStateFromTypedArray(a);
                        verifyRequiredAttributes(a);
                    } catch (XmlPullParserException e) {
                        throw new RuntimeException(e);
                    }
                } finally {
                    a.recycle();
                }
            }
            if (state.mDrawable != null && state.mDrawable.canApplyTheme()) {
                state.mDrawable.applyTheme(t);
            }
        }
    }

    @Override
    public boolean canApplyTheme() {
        return (this.mState != null && this.mState.canApplyTheme()) || super.canApplyTheme();
    }

    @Override
    public void invalidateDrawable(Drawable who) {
        Drawable.Callback callback = getCallback();
        if (callback != null) {
            callback.invalidateDrawable(this);
        }
    }

    @Override
    public void scheduleDrawable(Drawable who, Runnable what, long when) {
        Drawable.Callback callback = getCallback();
        if (callback != null) {
            callback.scheduleDrawable(this, what, when);
        }
    }

    @Override
    public void unscheduleDrawable(Drawable who, Runnable what) {
        Drawable.Callback callback = getCallback();
        if (callback != null) {
            callback.unscheduleDrawable(this, what);
        }
    }

    @Override
    public void draw(Canvas canvas) {
        this.mState.mDrawable.draw(canvas);
    }

    @Override
    public int getChangingConfigurations() {
        return super.getChangingConfigurations() | this.mState.mChangingConfigurations | this.mState.mDrawable.getChangingConfigurations();
    }

    @Override
    public boolean getPadding(Rect padding) {
        boolean pad = this.mState.mDrawable.getPadding(padding);
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
    public void setHotspot(float x, float y) {
        this.mState.mDrawable.setHotspot(x, y);
    }

    @Override
    public void setHotspotBounds(int left, int top, int right, int bottom) {
        this.mState.mDrawable.setHotspotBounds(left, top, right, bottom);
    }

    @Override
    public void getHotspotBounds(Rect outRect) {
        this.mState.mDrawable.getHotspotBounds(outRect);
    }

    @Override
    public boolean setVisible(boolean visible, boolean restart) {
        this.mState.mDrawable.setVisible(visible, restart);
        return super.setVisible(visible, restart);
    }

    @Override
    public void setAlpha(int alpha) {
        this.mState.mDrawable.setAlpha(alpha);
    }

    @Override
    public int getAlpha() {
        return this.mState.mDrawable.getAlpha();
    }

    @Override
    public void setColorFilter(ColorFilter cf) {
        this.mState.mDrawable.setColorFilter(cf);
    }

    @Override
    public void setTintList(ColorStateList tint) {
        this.mState.mDrawable.setTintList(tint);
    }

    @Override
    public void setTintMode(PorterDuff.Mode tintMode) {
        this.mState.mDrawable.setTintMode(tintMode);
    }

    @Override
    public void setLayoutDirection(int layoutDirection) {
        this.mState.mDrawable.setLayoutDirection(layoutDirection);
    }

    @Override
    public int getOpacity() {
        InsetState state = this.mState;
        int opacity = state.mDrawable.getOpacity();
        if (opacity != -1) {
            return opacity;
        }
        if (state.mInsetLeft > 0 || state.mInsetTop > 0 || state.mInsetRight > 0 || state.mInsetBottom > 0) {
            return -3;
        }
        return opacity;
    }

    @Override
    public boolean isStateful() {
        return this.mState.mDrawable.isStateful();
    }

    @Override
    protected boolean onStateChange(int[] state) {
        boolean changed = this.mState.mDrawable.setState(state);
        onBoundsChange(getBounds());
        return changed;
    }

    @Override
    protected boolean onLevelChange(int level) {
        return this.mState.mDrawable.setLevel(level);
    }

    @Override
    protected void onBoundsChange(Rect bounds) {
        Rect r = this.mTmpRect;
        r.set(bounds);
        r.left += this.mState.mInsetLeft;
        r.top += this.mState.mInsetTop;
        r.right -= this.mState.mInsetRight;
        r.bottom -= this.mState.mInsetBottom;
        this.mState.mDrawable.setBounds(r.left, r.top, r.right, r.bottom);
    }

    @Override
    public int getIntrinsicWidth() {
        return this.mState.mDrawable.getIntrinsicWidth() + this.mState.mInsetLeft + this.mState.mInsetRight;
    }

    @Override
    public int getIntrinsicHeight() {
        return this.mState.mDrawable.getIntrinsicHeight() + this.mState.mInsetTop + this.mState.mInsetBottom;
    }

    @Override
    public void getOutline(Outline outline) {
        this.mState.mDrawable.getOutline(outline);
    }

    @Override
    public Drawable.ConstantState getConstantState() {
        if (!this.mState.canConstantState()) {
            return null;
        }
        this.mState.mChangingConfigurations = getChangingConfigurations();
        return this.mState;
    }

    @Override
    public Drawable mutate() {
        if (!this.mMutated && super.mutate() == this) {
            this.mState.mDrawable.mutate();
            this.mMutated = true;
        }
        return this;
    }

    @Override
    public void clearMutated() {
        super.clearMutated();
        this.mState.mDrawable.clearMutated();
        this.mMutated = false;
    }

    public Drawable getDrawable() {
        return this.mState.mDrawable;
    }

    static final class InsetState extends Drawable.ConstantState {
        private boolean mCanConstantState;
        int mChangingConfigurations;
        private boolean mCheckedConstantState;
        Drawable mDrawable;
        int mInsetBottom;
        int mInsetLeft;
        int mInsetRight;
        int mInsetTop;
        int[] mThemeAttrs;

        InsetState(InsetState orig, InsetDrawable owner, Resources res) {
            this.mInsetLeft = 0;
            this.mInsetTop = 0;
            this.mInsetRight = 0;
            this.mInsetBottom = 0;
            if (orig != null) {
                this.mThemeAttrs = orig.mThemeAttrs;
                this.mChangingConfigurations = orig.mChangingConfigurations;
                if (res != null) {
                    this.mDrawable = orig.mDrawable.getConstantState().newDrawable(res);
                } else {
                    this.mDrawable = orig.mDrawable.getConstantState().newDrawable();
                }
                this.mDrawable.setCallback(owner);
                this.mDrawable.setLayoutDirection(orig.mDrawable.getLayoutDirection());
                this.mDrawable.setBounds(orig.mDrawable.getBounds());
                this.mDrawable.setLevel(orig.mDrawable.getLevel());
                this.mInsetLeft = orig.mInsetLeft;
                this.mInsetTop = orig.mInsetTop;
                this.mInsetRight = orig.mInsetRight;
                this.mInsetBottom = orig.mInsetBottom;
                this.mCanConstantState = true;
                this.mCheckedConstantState = true;
            }
        }

        @Override
        public boolean canApplyTheme() {
            return this.mThemeAttrs != null || (this.mDrawable != null && this.mDrawable.canApplyTheme()) || super.canApplyTheme();
        }

        @Override
        public Drawable newDrawable() {
            return new InsetDrawable(this, null);
        }

        @Override
        public Drawable newDrawable(Resources res) {
            return new InsetDrawable(this, res);
        }

        @Override
        public int getChangingConfigurations() {
            return this.mChangingConfigurations;
        }

        boolean canConstantState() {
            if (!this.mCheckedConstantState) {
                this.mCanConstantState = this.mDrawable.getConstantState() != null;
                this.mCheckedConstantState = true;
            }
            return this.mCanConstantState;
        }

        @Override
        public int addAtlasableBitmaps(Collection<Bitmap> atlasList) {
            Drawable.ConstantState state = this.mDrawable.getConstantState();
            if (state != null) {
                return state.addAtlasableBitmaps(atlasList);
            }
            return 0;
        }
    }

    private InsetDrawable(InsetState state, Resources res) {
        this.mTmpRect = new Rect();
        this.mState = new InsetState(state, this, res);
    }
}
