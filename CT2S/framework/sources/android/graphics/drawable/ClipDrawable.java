package android.graphics.drawable;

import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.Gravity;
import com.android.internal.R;
import java.io.IOException;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

public class ClipDrawable extends Drawable implements Drawable.Callback {
    public static final int HORIZONTAL = 1;
    public static final int VERTICAL = 2;
    private boolean mMutated;
    private ClipState mState;
    private final Rect mTmpRect;

    ClipDrawable() {
        this(null, null);
    }

    public ClipDrawable(Drawable drawable, int gravity, int orientation) {
        this(null, null);
        this.mState.mDrawable = drawable;
        this.mState.mGravity = gravity;
        this.mState.mOrientation = orientation;
        if (drawable != null) {
            drawable.setCallback(this);
        }
    }

    @Override
    public void inflate(Resources r, XmlPullParser parser, AttributeSet attrs, Resources.Theme theme) throws XmlPullParserException, IOException {
        super.inflate(r, parser, attrs, theme);
        TypedArray a = obtainAttributes(r, theme, attrs, R.styleable.ClipDrawable);
        this.mState.mDrawable = null;
        updateStateFromTypedArray(a);
        inflateChildElements(r, parser, attrs, theme);
        verifyRequiredAttributes(a);
        a.recycle();
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
            if (this.mState.mThemeAttrs == null || this.mState.mThemeAttrs[1] == 0) {
                throw new XmlPullParserException(a.getPositionDescription() + ": <clip> tag requires a 'drawable' attribute or child tag defining a drawable");
            }
        }
    }

    private void updateStateFromTypedArray(TypedArray a) {
        ClipState state = this.mState;
        state.mChangingConfigurations |= a.getChangingConfigurations();
        state.mThemeAttrs = a.extractThemeAttrs();
        state.mOrientation = a.getInt(2, state.mOrientation);
        state.mGravity = a.getInt(0, state.mGravity);
        Drawable dr = a.getDrawable(1);
        if (dr != null) {
            state.mDrawable = dr;
            dr.setCallback(this);
        }
    }

    @Override
    public void applyTheme(Resources.Theme t) {
        super.applyTheme(t);
        ClipState state = this.mState;
        if (state != null && state.mThemeAttrs != null) {
            TypedArray a = t.resolveAttributes(state.mThemeAttrs, R.styleable.ClipDrawable);
            try {
                try {
                    updateStateFromTypedArray(a);
                    verifyRequiredAttributes(a);
                    a.recycle();
                    if (state.mDrawable != null && state.mDrawable.canApplyTheme()) {
                        state.mDrawable.applyTheme(t);
                    }
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
        return this.mState.mDrawable.setState(state);
    }

    @Override
    protected boolean onLevelChange(int level) {
        this.mState.mDrawable.setLevel(level);
        invalidateSelf();
        return true;
    }

    @Override
    protected void onBoundsChange(Rect bounds) {
        this.mState.mDrawable.setBounds(bounds);
    }

    @Override
    public void draw(Canvas canvas) {
        if (this.mState.mDrawable.getLevel() != 0) {
            Rect r = this.mTmpRect;
            Rect bounds = getBounds();
            int level = getLevel();
            int w = bounds.width();
            if ((this.mState.mOrientation & 1) != 0) {
                w -= ((w + 0) * (10000 - level)) / 10000;
            }
            int h = bounds.height();
            if ((this.mState.mOrientation & 2) != 0) {
                h -= ((h + 0) * (10000 - level)) / 10000;
            }
            int layoutDirection = getLayoutDirection();
            Gravity.apply(this.mState.mGravity, w, h, bounds, r, layoutDirection);
            if (w > 0 && h > 0) {
                canvas.save();
                canvas.clipRect(r);
                this.mState.mDrawable.draw(canvas);
                canvas.restore();
            }
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
    public void setLayoutDirection(int layoutDirection) {
        this.mState.mDrawable.setLayoutDirection(layoutDirection);
        super.setLayoutDirection(layoutDirection);
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

    static final class ClipState extends Drawable.ConstantState {
        private boolean mCanConstantState;
        int mChangingConfigurations;
        private boolean mCheckedConstantState;
        Drawable mDrawable;
        int mGravity;
        int mOrientation;
        int[] mThemeAttrs;

        ClipState(ClipState orig, ClipDrawable owner, Resources res) {
            this.mOrientation = 1;
            this.mGravity = 3;
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
                this.mOrientation = orig.mOrientation;
                this.mGravity = orig.mGravity;
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
            return new ClipDrawable(this, (Resources) null);
        }

        @Override
        public Drawable newDrawable(Resources res) {
            return new ClipDrawable(this, res);
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
    }

    private ClipDrawable(ClipState state, Resources res) {
        this.mTmpRect = new Rect();
        this.mState = new ClipState(state, this, res);
    }
}
