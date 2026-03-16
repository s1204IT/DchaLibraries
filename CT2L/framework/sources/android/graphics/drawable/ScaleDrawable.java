package android.graphics.drawable;

import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.Gravity;
import com.android.internal.R;
import java.io.IOException;
import java.util.Collection;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

public class ScaleDrawable extends Drawable implements Drawable.Callback {
    private boolean mMutated;
    private ScaleState mState;
    private final Rect mTmpRect;

    ScaleDrawable() {
        this(null, null);
    }

    public ScaleDrawable(Drawable drawable, int gravity, float scaleWidth, float scaleHeight) {
        this(null, null);
        this.mState.mDrawable = drawable;
        this.mState.mGravity = gravity;
        this.mState.mScaleWidth = scaleWidth;
        this.mState.mScaleHeight = scaleHeight;
        if (drawable != null) {
            drawable.setCallback(this);
        }
    }

    public Drawable getDrawable() {
        return this.mState.mDrawable;
    }

    private static float getPercent(TypedArray a, int name, float defaultValue) {
        String s = a.getString(name);
        if (s != null && s.endsWith("%")) {
            String f = s.substring(0, s.length() - 1);
            return Float.parseFloat(f) / 100.0f;
        }
        return defaultValue;
    }

    @Override
    public void inflate(Resources r, XmlPullParser parser, AttributeSet attrs, Resources.Theme theme) throws XmlPullParserException, IOException {
        super.inflate(r, parser, attrs, theme);
        TypedArray a = obtainAttributes(r, theme, attrs, R.styleable.ScaleDrawable);
        this.mState.mDrawable = null;
        updateStateFromTypedArray(a);
        inflateChildElements(r, parser, attrs, theme);
        verifyRequiredAttributes(a);
        a.recycle();
    }

    @Override
    public void applyTheme(Resources.Theme t) {
        super.applyTheme(t);
        ScaleState state = this.mState;
        if (state != null) {
            if (state.mThemeAttrs != null) {
                TypedArray a = t.resolveAttributes(state.mThemeAttrs, R.styleable.ScaleDrawable);
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

    private void inflateChildElements(Resources r, XmlPullParser parser, AttributeSet attrs, Resources.Theme theme) throws XmlPullParserException, IOException {
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
        if (dr != null) {
            this.mState.mDrawable = dr;
            dr.setCallback(this);
        }
    }

    private void verifyRequiredAttributes(TypedArray a) throws XmlPullParserException {
        if (this.mState.mDrawable == null) {
            if (this.mState.mThemeAttrs == null || this.mState.mThemeAttrs[0] == 0) {
                throw new XmlPullParserException(a.getPositionDescription() + ": <scale> tag requires a 'drawable' attribute or child tag defining a drawable");
            }
        }
    }

    private void updateStateFromTypedArray(TypedArray a) {
        ScaleState state = this.mState;
        state.mChangingConfigurations |= a.getChangingConfigurations();
        state.mThemeAttrs = a.extractThemeAttrs();
        state.mScaleWidth = getPercent(a, 1, state.mScaleWidth);
        state.mScaleHeight = getPercent(a, 2, state.mScaleHeight);
        state.mGravity = a.getInt(3, state.mGravity);
        state.mUseIntrinsicSizeAsMin = a.getBoolean(4, state.mUseIntrinsicSizeAsMin);
        Drawable dr = a.getDrawable(0);
        if (dr != null) {
            state.mDrawable = dr;
            dr.setCallback(this);
        }
    }

    @Override
    public boolean canApplyTheme() {
        return (this.mState != null && this.mState.canApplyTheme()) || super.canApplyTheme();
    }

    @Override
    public void invalidateDrawable(Drawable who) {
        if (getCallback() != null) {
            getCallback().invalidateDrawable(this);
        }
    }

    @Override
    public void scheduleDrawable(Drawable who, Runnable what, long when) {
        if (getCallback() != null) {
            getCallback().scheduleDrawable(this, what, when);
        }
    }

    @Override
    public void unscheduleDrawable(Drawable who, Runnable what) {
        if (getCallback() != null) {
            getCallback().unscheduleDrawable(this, what);
        }
    }

    @Override
    public void draw(Canvas canvas) {
        if (this.mState.mDrawable.getLevel() != 0) {
            this.mState.mDrawable.draw(canvas);
        }
    }

    @Override
    public int getChangingConfigurations() {
        return super.getChangingConfigurations() | this.mState.mChangingConfigurations | this.mState.mDrawable.getChangingConfigurations();
    }

    @Override
    public boolean getPadding(Rect padding) {
        return this.mState.mDrawable.getPadding(padding);
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
    public int getOpacity() {
        return this.mState.mDrawable.getOpacity();
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
        this.mState.mDrawable.setLevel(level);
        onBoundsChange(getBounds());
        invalidateSelf();
        return true;
    }

    @Override
    protected void onBoundsChange(Rect bounds) {
        Rect r = this.mTmpRect;
        boolean min = this.mState.mUseIntrinsicSizeAsMin;
        int level = getLevel();
        int w = bounds.width();
        if (this.mState.mScaleWidth > 0.0f) {
            int iw = min ? this.mState.mDrawable.getIntrinsicWidth() : 0;
            w -= (int) ((((w - iw) * (10000 - level)) * this.mState.mScaleWidth) / 10000.0f);
        }
        int h = bounds.height();
        if (this.mState.mScaleHeight > 0.0f) {
            int ih = min ? this.mState.mDrawable.getIntrinsicHeight() : 0;
            h -= (int) ((((h - ih) * (10000 - level)) * this.mState.mScaleHeight) / 10000.0f);
        }
        int layoutDirection = getLayoutDirection();
        Gravity.apply(this.mState.mGravity, w, h, bounds, r, layoutDirection);
        if (w > 0 && h > 0) {
            this.mState.mDrawable.setBounds(r.left, r.top, r.right, r.bottom);
        }
    }

    @Override
    public int getIntrinsicWidth() {
        return this.mState.mDrawable.getIntrinsicWidth();
    }

    @Override
    public int getIntrinsicHeight() {
        return this.mState.mDrawable.getIntrinsicHeight();
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

    static final class ScaleState extends Drawable.ConstantState {
        private static final float DO_NOT_SCALE = -1.0f;
        private boolean mCanConstantState;
        int mChangingConfigurations;
        private boolean mCheckedConstantState;
        Drawable mDrawable;
        int mGravity;
        float mScaleHeight;
        float mScaleWidth;
        int[] mThemeAttrs;
        boolean mUseIntrinsicSizeAsMin;

        ScaleState(ScaleState orig, ScaleDrawable owner, Resources res) {
            this.mScaleWidth = -1.0f;
            this.mScaleHeight = -1.0f;
            this.mGravity = 3;
            this.mUseIntrinsicSizeAsMin = false;
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
                this.mScaleWidth = orig.mScaleWidth;
                this.mScaleHeight = orig.mScaleHeight;
                this.mGravity = orig.mGravity;
                this.mUseIntrinsicSizeAsMin = orig.mUseIntrinsicSizeAsMin;
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
            return new ScaleDrawable(this, null);
        }

        @Override
        public Drawable newDrawable(Resources res) {
            return new ScaleDrawable(this, res);
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

    private ScaleDrawable(ScaleState state, Resources res) {
        this.mTmpRect = new Rect();
        this.mState = new ScaleState(state, this, res);
    }
}
