package android.graphics.drawable;

import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.DrawableWrapper;
import android.util.AttributeSet;
import android.view.Gravity;
import com.android.internal.R;
import java.io.IOException;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

public class ClipDrawable extends DrawableWrapper {
    public static final int HORIZONTAL = 1;
    private static final int MAX_LEVEL = 10000;
    public static final int VERTICAL = 2;
    private ClipState mState;
    private final Rect mTmpRect;

    ClipDrawable(ClipState state, Resources res, ClipDrawable clipDrawable) {
        this(state, res);
    }

    ClipDrawable() {
        this(new ClipState(null, null), null);
    }

    public ClipDrawable(Drawable drawable, int gravity, int orientation) {
        this(new ClipState(null, null), null);
        this.mState.mGravity = gravity;
        this.mState.mOrientation = orientation;
        setDrawable(drawable);
    }

    @Override
    public void inflate(Resources r, XmlPullParser parser, AttributeSet attrs, Resources.Theme theme) throws XmlPullParserException, IOException {
        TypedArray a = obtainAttributes(r, theme, attrs, R.styleable.ClipDrawable);
        super.inflate(r, parser, attrs, theme);
        updateStateFromTypedArray(a);
        verifyRequiredAttributes(a);
        a.recycle();
    }

    @Override
    public void applyTheme(Resources.Theme t) {
        super.applyTheme(t);
        ClipState state = this.mState;
        if (state == null || state.mThemeAttrs == null) {
            return;
        }
        TypedArray a = t.resolveAttributes(state.mThemeAttrs, R.styleable.ClipDrawable);
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
            throw new XmlPullParserException(a.getPositionDescription() + ": <clip> tag requires a 'drawable' attribute or child tag defining a drawable");
        }
    }

    private void updateStateFromTypedArray(TypedArray a) {
        ClipState state = this.mState;
        if (state == null) {
            return;
        }
        state.mChangingConfigurations |= a.getChangingConfigurations();
        state.mThemeAttrs = a.extractThemeAttrs();
        state.mOrientation = a.getInt(2, state.mOrientation);
        state.mGravity = a.getInt(0, state.mGravity);
    }

    @Override
    protected boolean onLevelChange(int level) {
        super.onLevelChange(level);
        invalidateSelf();
        return true;
    }

    @Override
    public int getOpacity() {
        Drawable dr = getDrawable();
        int opacity = dr.getOpacity();
        if (opacity == -2 || dr.getLevel() == 0) {
            return -2;
        }
        int level = getLevel();
        if (level >= 10000) {
            return dr.getOpacity();
        }
        return -3;
    }

    @Override
    public void draw(Canvas canvas) {
        Drawable dr = getDrawable();
        if (dr.getLevel() == 0) {
            return;
        }
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
        if (w <= 0 || h <= 0) {
            return;
        }
        canvas.save();
        canvas.clipRect(r);
        dr.draw(canvas);
        canvas.restore();
    }

    @Override
    DrawableWrapper.DrawableWrapperState mutateConstantState() {
        this.mState = new ClipState(this.mState, null);
        return this.mState;
    }

    static final class ClipState extends DrawableWrapper.DrawableWrapperState {
        int mGravity;
        int mOrientation;
        private int[] mThemeAttrs;

        ClipState(ClipState orig, Resources res) {
            super(orig, res);
            this.mOrientation = 1;
            this.mGravity = 3;
            if (orig == null) {
                return;
            }
            this.mOrientation = orig.mOrientation;
            this.mGravity = orig.mGravity;
        }

        @Override
        public Drawable newDrawable(Resources res) {
            return new ClipDrawable(this, res, (ClipDrawable) null);
        }
    }

    private ClipDrawable(ClipState state, Resources res) {
        super(state, res);
        this.mTmpRect = new Rect();
        this.mState = state;
    }
}
