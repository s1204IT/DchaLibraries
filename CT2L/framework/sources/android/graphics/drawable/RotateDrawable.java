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
import android.util.TypedValue;
import com.android.internal.R;
import java.io.IOException;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

public class RotateDrawable extends Drawable implements Drawable.Callback {
    private static final float MAX_LEVEL = 10000.0f;
    private boolean mMutated;
    private final RotateState mState;

    public RotateDrawable() {
        this(null, null);
    }

    private RotateDrawable(RotateState rotateState, Resources res) {
        this.mState = new RotateState(rotateState, this, res);
    }

    @Override
    public void draw(Canvas canvas) {
        RotateState st = this.mState;
        Drawable d = st.mDrawable;
        Rect bounds = d.getBounds();
        int w = bounds.right - bounds.left;
        int h = bounds.bottom - bounds.top;
        float px = st.mPivotXRel ? w * st.mPivotX : st.mPivotX;
        float py = st.mPivotYRel ? h * st.mPivotY : st.mPivotY;
        int saveCount = canvas.save();
        canvas.rotate(st.mCurrentDegrees, bounds.left + px, bounds.top + py);
        d.draw(canvas);
        canvas.restoreToCount(saveCount);
    }

    public void setDrawable(Drawable drawable) {
        Drawable oldDrawable = this.mState.mDrawable;
        if (oldDrawable != drawable) {
            if (oldDrawable != null) {
                oldDrawable.setCallback(null);
            }
            this.mState.mDrawable = drawable;
            if (drawable != null) {
                drawable.setCallback(this);
            }
        }
    }

    public Drawable getDrawable() {
        return this.mState.mDrawable;
    }

    @Override
    public int getChangingConfigurations() {
        return super.getChangingConfigurations() | this.mState.mChangingConfigurations | this.mState.mDrawable.getChangingConfigurations();
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

    public void setFromDegrees(float fromDegrees) {
        if (this.mState.mFromDegrees != fromDegrees) {
            this.mState.mFromDegrees = fromDegrees;
            invalidateSelf();
        }
    }

    public float getFromDegrees() {
        return this.mState.mFromDegrees;
    }

    public void setToDegrees(float toDegrees) {
        if (this.mState.mToDegrees != toDegrees) {
            this.mState.mToDegrees = toDegrees;
            invalidateSelf();
        }
    }

    public float getToDegrees() {
        return this.mState.mToDegrees;
    }

    public void setPivotX(float pivotX) {
        if (this.mState.mPivotX != pivotX) {
            this.mState.mPivotX = pivotX;
            invalidateSelf();
        }
    }

    public float getPivotX() {
        return this.mState.mPivotX;
    }

    public void setPivotXRelative(boolean relative) {
        if (this.mState.mPivotXRel != relative) {
            this.mState.mPivotXRel = relative;
            invalidateSelf();
        }
    }

    public boolean isPivotXRelative() {
        return this.mState.mPivotXRel;
    }

    public void setPivotY(float pivotY) {
        if (this.mState.mPivotY != pivotY) {
            this.mState.mPivotY = pivotY;
            invalidateSelf();
        }
    }

    public float getPivotY() {
        return this.mState.mPivotY;
    }

    public void setPivotYRelative(boolean relative) {
        if (this.mState.mPivotYRel != relative) {
            this.mState.mPivotYRel = relative;
            invalidateSelf();
        }
    }

    public boolean isPivotYRelative() {
        return this.mState.mPivotYRel;
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
    public boolean getPadding(Rect padding) {
        return this.mState.mDrawable.getPadding(padding);
    }

    @Override
    public boolean setVisible(boolean visible, boolean restart) {
        this.mState.mDrawable.setVisible(visible, restart);
        return super.setVisible(visible, restart);
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
        this.mState.mCurrentDegrees = this.mState.mFromDegrees + ((this.mState.mToDegrees - this.mState.mFromDegrees) * (level / 10000.0f));
        invalidateSelf();
        return true;
    }

    @Override
    protected void onBoundsChange(Rect bounds) {
        this.mState.mDrawable.setBounds(bounds.left, bounds.top, bounds.right, bounds.bottom);
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
    public void inflate(Resources r, XmlPullParser parser, AttributeSet attrs, Resources.Theme theme) throws XmlPullParserException, IOException {
        TypedArray a = obtainAttributes(r, theme, attrs, R.styleable.RotateDrawable);
        super.inflateWithAttributes(r, parser, a, 0);
        this.mState.mDrawable = null;
        updateStateFromTypedArray(a);
        inflateChildElements(r, parser, attrs, theme);
        verifyRequiredAttributes(a);
        a.recycle();
    }

    @Override
    public void applyTheme(Resources.Theme t) {
        super.applyTheme(t);
        RotateState state = this.mState;
        if (state != null) {
            if (state.mThemeAttrs != null) {
                TypedArray a = t.resolveAttributes(state.mThemeAttrs, R.styleable.RotateDrawable);
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
                throw new XmlPullParserException(a.getPositionDescription() + ": <rotate> tag requires a 'drawable' attribute or child tag defining a drawable");
            }
        }
    }

    private void updateStateFromTypedArray(TypedArray a) {
        RotateState state = this.mState;
        state.mChangingConfigurations |= a.getChangingConfigurations();
        state.mThemeAttrs = a.extractThemeAttrs();
        if (a.hasValue(4)) {
            TypedValue tv = a.peekValue(4);
            state.mPivotXRel = tv.type == 6;
            state.mPivotX = state.mPivotXRel ? tv.getFraction(1.0f, 1.0f) : tv.getFloat();
        }
        if (a.hasValue(5)) {
            TypedValue tv2 = a.peekValue(5);
            state.mPivotYRel = tv2.type == 6;
            state.mPivotY = state.mPivotYRel ? tv2.getFraction(1.0f, 1.0f) : tv2.getFloat();
        }
        state.mFromDegrees = a.getFloat(2, state.mFromDegrees);
        state.mToDegrees = a.getFloat(3, state.mToDegrees);
        state.mCurrentDegrees = state.mFromDegrees;
        Drawable dr = a.getDrawable(1);
        if (dr != null) {
            state.mDrawable = dr;
            dr.setCallback(this);
        }
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

    static final class RotateState extends Drawable.ConstantState {
        private boolean mCanConstantState;
        int mChangingConfigurations;
        private boolean mCheckedConstantState;
        float mCurrentDegrees;
        Drawable mDrawable;
        float mFromDegrees;
        float mPivotX;
        boolean mPivotXRel;
        float mPivotY;
        boolean mPivotYRel;
        int[] mThemeAttrs;
        float mToDegrees;

        RotateState(RotateState orig, RotateDrawable owner, Resources res) {
            this.mPivotXRel = true;
            this.mPivotX = 0.5f;
            this.mPivotYRel = true;
            this.mPivotY = 0.5f;
            this.mFromDegrees = 0.0f;
            this.mToDegrees = 360.0f;
            this.mCurrentDegrees = 0.0f;
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
                this.mPivotXRel = orig.mPivotXRel;
                this.mPivotX = orig.mPivotX;
                this.mPivotYRel = orig.mPivotYRel;
                this.mPivotY = orig.mPivotY;
                this.mFromDegrees = orig.mFromDegrees;
                this.mToDegrees = orig.mToDegrees;
                this.mCurrentDegrees = orig.mCurrentDegrees;
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
            return new RotateDrawable(this, null);
        }

        @Override
        public Drawable newDrawable(Resources res) {
            return new RotateDrawable(this, res);
        }

        @Override
        public int getChangingConfigurations() {
            return this.mChangingConfigurations;
        }

        public boolean canConstantState() {
            if (!this.mCheckedConstantState) {
                this.mCanConstantState = this.mDrawable.getConstantState() != null;
                this.mCheckedConstantState = true;
            }
            return this.mCanConstantState;
        }
    }
}
