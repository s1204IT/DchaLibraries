package android.graphics.drawable;

import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.os.SystemClock;

public class TransitionDrawable extends LayerDrawable implements Drawable.Callback {
    private static final int TRANSITION_NONE = 2;
    private static final int TRANSITION_RUNNING = 1;
    private static final int TRANSITION_STARTING = 0;
    private int mAlpha;
    private boolean mCrossFade;
    private int mDuration;
    private int mFrom;
    private int mOriginalDuration;
    private boolean mReverse;
    private long mStartTimeMillis;
    private int mTo;
    private int mTransitionState;

    public TransitionDrawable(Drawable[] layers) {
        this(new TransitionState(null, null, null), layers);
    }

    TransitionDrawable() {
        this(new TransitionState(null, null, null), (Resources) null);
    }

    private TransitionDrawable(TransitionState state, Resources res) {
        super(state, res);
        this.mTransitionState = 2;
        this.mAlpha = 0;
    }

    private TransitionDrawable(TransitionState state, Drawable[] layers) {
        super(layers, state);
        this.mTransitionState = 2;
        this.mAlpha = 0;
    }

    @Override
    LayerDrawable.LayerState createConstantState(LayerDrawable.LayerState state, Resources res) {
        return new TransitionState((TransitionState) state, this, res);
    }

    public void startTransition(int durationMillis) {
        this.mFrom = 0;
        this.mTo = 255;
        this.mAlpha = 0;
        this.mOriginalDuration = durationMillis;
        this.mDuration = durationMillis;
        this.mReverse = false;
        this.mTransitionState = 0;
        invalidateSelf();
    }

    public void resetTransition() {
        this.mAlpha = 0;
        this.mTransitionState = 2;
        invalidateSelf();
    }

    public void reverseTransition(int duration) {
        long time = SystemClock.uptimeMillis();
        if (time - this.mStartTimeMillis > this.mDuration) {
            if (this.mTo == 0) {
                this.mFrom = 0;
                this.mTo = 255;
                this.mAlpha = 0;
                this.mReverse = false;
            } else {
                this.mFrom = 255;
                this.mTo = 0;
                this.mAlpha = 255;
                this.mReverse = true;
            }
            this.mOriginalDuration = duration;
            this.mDuration = duration;
            this.mTransitionState = 0;
            invalidateSelf();
            return;
        }
        this.mReverse = this.mReverse ? false : true;
        this.mFrom = this.mAlpha;
        this.mTo = this.mReverse ? 0 : 255;
        this.mDuration = (int) (this.mReverse ? time - this.mStartTimeMillis : ((long) this.mOriginalDuration) - (time - this.mStartTimeMillis));
        this.mTransitionState = 0;
    }

    @Override
    public void draw(Canvas canvas) {
        boolean done = true;
        switch (this.mTransitionState) {
            case 0:
                this.mStartTimeMillis = SystemClock.uptimeMillis();
                done = false;
                this.mTransitionState = 1;
                break;
            case 1:
                if (this.mStartTimeMillis >= 0) {
                    float normalized = (SystemClock.uptimeMillis() - this.mStartTimeMillis) / this.mDuration;
                    done = normalized >= 1.0f;
                    this.mAlpha = (int) (this.mFrom + ((this.mTo - this.mFrom) * Math.min(normalized, 1.0f)));
                }
                break;
        }
        int alpha = this.mAlpha;
        boolean crossFade = this.mCrossFade;
        LayerDrawable.ChildDrawable[] array = this.mLayerState.mChildren;
        if (done) {
            if (!crossFade || alpha == 0) {
                array[0].mDrawable.draw(canvas);
            }
            if (alpha == 255) {
                array[1].mDrawable.draw(canvas);
                return;
            }
            return;
        }
        Drawable d = array[0].mDrawable;
        if (crossFade) {
            d.setAlpha(255 - alpha);
        }
        d.draw(canvas);
        if (crossFade) {
            d.setAlpha(255);
        }
        if (alpha > 0) {
            Drawable d2 = array[1].mDrawable;
            d2.setAlpha(alpha);
            d2.draw(canvas);
            d2.setAlpha(255);
        }
        if (!done) {
            invalidateSelf();
        }
    }

    public void setCrossFadeEnabled(boolean enabled) {
        this.mCrossFade = enabled;
    }

    public boolean isCrossFadeEnabled() {
        return this.mCrossFade;
    }

    static class TransitionState extends LayerDrawable.LayerState {
        TransitionState(TransitionState orig, TransitionDrawable owner, Resources res) {
            super(orig, owner, res);
        }

        @Override
        public Drawable newDrawable() {
            return new TransitionDrawable(this, (Resources) null);
        }

        @Override
        public Drawable newDrawable(Resources res) {
            return new TransitionDrawable(this, res);
        }

        @Override
        public int getChangingConfigurations() {
            return this.mChangingConfigurations;
        }
    }
}
