package android.graphics.drawable;

import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.DrawableWrapper;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.util.TypedValue;
import com.android.internal.R;
import java.io.IOException;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

public class AnimatedRotateDrawable extends DrawableWrapper implements Animatable {
    private float mCurrentDegrees;
    private float mIncrement;
    private final Runnable mNextFrame;
    private boolean mRunning;
    private AnimatedRotateState mState;

    AnimatedRotateDrawable(AnimatedRotateState state, Resources res, AnimatedRotateDrawable animatedRotateDrawable) {
        this(state, res);
    }

    public AnimatedRotateDrawable() {
        this(new AnimatedRotateState(null, null), null);
    }

    @Override
    public void draw(Canvas canvas) {
        Drawable drawable = getDrawable();
        Rect bounds = drawable.getBounds();
        int w = bounds.right - bounds.left;
        int h = bounds.bottom - bounds.top;
        AnimatedRotateState st = this.mState;
        float px = st.mPivotXRel ? w * st.mPivotX : st.mPivotX;
        float py = st.mPivotYRel ? h * st.mPivotY : st.mPivotY;
        int saveCount = canvas.save();
        canvas.rotate(this.mCurrentDegrees, bounds.left + px, bounds.top + py);
        drawable.draw(canvas);
        canvas.restoreToCount(saveCount);
    }

    @Override
    public void start() {
        if (this.mRunning) {
            return;
        }
        this.mRunning = true;
        nextFrame();
    }

    @Override
    public void stop() {
        this.mRunning = false;
        unscheduleSelf(this.mNextFrame);
    }

    @Override
    public boolean isRunning() {
        return this.mRunning;
    }

    private void nextFrame() {
        unscheduleSelf(this.mNextFrame);
        scheduleSelf(this.mNextFrame, SystemClock.uptimeMillis() + ((long) this.mState.mFrameDuration));
    }

    @Override
    public boolean setVisible(boolean visible, boolean restart) {
        boolean changed = super.setVisible(visible, restart);
        if (visible) {
            if (changed || restart) {
                this.mCurrentDegrees = 0.0f;
                nextFrame();
            }
        } else {
            unscheduleSelf(this.mNextFrame);
        }
        return changed;
    }

    @Override
    public void inflate(Resources r, XmlPullParser parser, AttributeSet attrs, Resources.Theme theme) throws XmlPullParserException, IOException {
        TypedArray a = obtainAttributes(r, theme, attrs, R.styleable.AnimatedRotateDrawable);
        super.inflate(r, parser, attrs, theme);
        updateStateFromTypedArray(a);
        verifyRequiredAttributes(a);
        a.recycle();
        updateLocalState();
    }

    @Override
    public void applyTheme(Resources.Theme t) {
        super.applyTheme(t);
        AnimatedRotateState state = this.mState;
        if (state == null) {
            return;
        }
        if (state.mThemeAttrs != null) {
            TypedArray a = t.resolveAttributes(state.mThemeAttrs, R.styleable.AnimatedRotateDrawable);
            try {
                updateStateFromTypedArray(a);
                verifyRequiredAttributes(a);
            } catch (XmlPullParserException e) {
                rethrowAsRuntimeException(e);
            } finally {
                a.recycle();
            }
        }
        updateLocalState();
    }

    private void verifyRequiredAttributes(TypedArray a) throws XmlPullParserException {
        if (getDrawable() != null) {
            return;
        }
        if (this.mState.mThemeAttrs != null && this.mState.mThemeAttrs[1] != 0) {
        } else {
            throw new XmlPullParserException(a.getPositionDescription() + ": <animated-rotate> tag requires a 'drawable' attribute or child tag defining a drawable");
        }
    }

    private void updateStateFromTypedArray(TypedArray a) {
        AnimatedRotateState state = this.mState;
        if (state == null) {
            return;
        }
        state.mChangingConfigurations |= a.getChangingConfigurations();
        state.mThemeAttrs = a.extractThemeAttrs();
        if (a.hasValue(2)) {
            TypedValue tv = a.peekValue(2);
            state.mPivotXRel = tv.type == 6;
            state.mPivotX = state.mPivotXRel ? tv.getFraction(1.0f, 1.0f) : tv.getFloat();
        }
        if (a.hasValue(3)) {
            TypedValue tv2 = a.peekValue(3);
            state.mPivotYRel = tv2.type == 6;
            state.mPivotY = state.mPivotYRel ? tv2.getFraction(1.0f, 1.0f) : tv2.getFloat();
        }
        setFramesCount(a.getInt(5, state.mFramesCount));
        setFramesDuration(a.getInt(4, state.mFrameDuration));
    }

    public void setFramesCount(int framesCount) {
        this.mState.mFramesCount = framesCount;
        this.mIncrement = 360.0f / this.mState.mFramesCount;
    }

    public void setFramesDuration(int framesDuration) {
        this.mState.mFrameDuration = framesDuration;
    }

    @Override
    DrawableWrapper.DrawableWrapperState mutateConstantState() {
        this.mState = new AnimatedRotateState(this.mState, null);
        return this.mState;
    }

    static final class AnimatedRotateState extends DrawableWrapper.DrawableWrapperState {
        int mFrameDuration;
        int mFramesCount;
        float mPivotX;
        boolean mPivotXRel;
        float mPivotY;
        boolean mPivotYRel;
        private int[] mThemeAttrs;

        public AnimatedRotateState(AnimatedRotateState orig, Resources res) {
            super(orig, res);
            this.mPivotXRel = false;
            this.mPivotX = 0.0f;
            this.mPivotYRel = false;
            this.mPivotY = 0.0f;
            this.mFrameDuration = 150;
            this.mFramesCount = 12;
            if (orig == null) {
                return;
            }
            this.mPivotXRel = orig.mPivotXRel;
            this.mPivotX = orig.mPivotX;
            this.mPivotYRel = orig.mPivotYRel;
            this.mPivotY = orig.mPivotY;
            this.mFramesCount = orig.mFramesCount;
            this.mFrameDuration = orig.mFrameDuration;
        }

        @Override
        public Drawable newDrawable(Resources res) {
            return new AnimatedRotateDrawable(this, res, null);
        }
    }

    private AnimatedRotateDrawable(AnimatedRotateState state, Resources res) {
        super(state, res);
        this.mNextFrame = new Runnable() {
            @Override
            public void run() {
                AnimatedRotateDrawable.this.mCurrentDegrees += AnimatedRotateDrawable.this.mIncrement;
                if (AnimatedRotateDrawable.this.mCurrentDegrees > 360.0f - AnimatedRotateDrawable.this.mIncrement) {
                    AnimatedRotateDrawable.this.mCurrentDegrees = 0.0f;
                }
                AnimatedRotateDrawable.this.invalidateSelf();
                AnimatedRotateDrawable.this.nextFrame();
            }
        };
        this.mState = state;
        updateLocalState();
    }

    private void updateLocalState() {
        AnimatedRotateState state = this.mState;
        this.mIncrement = 360.0f / state.mFramesCount;
        Drawable drawable = getDrawable();
        if (drawable == null) {
            return;
        }
        drawable.setFilterBitmap(true);
        if (!(drawable instanceof BitmapDrawable)) {
            return;
        }
        ((BitmapDrawable) drawable).setAntiAlias(true);
    }
}
