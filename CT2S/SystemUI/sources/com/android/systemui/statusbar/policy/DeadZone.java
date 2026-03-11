package com.android.systemui.statusbar.policy;

import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.util.Slog;
import android.view.MotionEvent;
import android.view.View;
import com.android.systemui.R;

public class DeadZone extends View {
    private final Runnable mDebugFlash;
    private int mDecay;
    private float mFlashFrac;
    private int mHold;
    private long mLastPokeTime;
    private boolean mShouldFlash;
    private int mSizeMax;
    private int mSizeMin;
    private boolean mVertical;

    public DeadZone(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public DeadZone(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs);
        this.mFlashFrac = 0.0f;
        this.mDebugFlash = new Runnable() {
            @Override
            public void run() {
                ObjectAnimator.ofFloat(DeadZone.this, "flash", 1.0f, 0.0f).setDuration(150L).start();
            }
        };
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.DeadZone, defStyle, 0);
        this.mHold = a.getInteger(2, 0);
        this.mDecay = a.getInteger(3, 0);
        this.mSizeMin = a.getDimensionPixelSize(0, 0);
        this.mSizeMax = a.getDimensionPixelSize(1, 0);
        int index = a.getInt(4, -1);
        this.mVertical = index == 1;
        setFlashOnTouchCapture(context.getResources().getBoolean(R.bool.config_dead_zone_flash));
    }

    static float lerp(float a, float b, float f) {
        return ((b - a) * f) + a;
    }

    private float getSize(long now) {
        if (this.mSizeMax == 0) {
            return 0.0f;
        }
        long dt = now - this.mLastPokeTime;
        if (dt > this.mHold + this.mDecay) {
            return this.mSizeMin;
        }
        if (dt < this.mHold) {
            return this.mSizeMax;
        }
        return (int) lerp(this.mSizeMax, this.mSizeMin, (dt - ((long) this.mHold)) / this.mDecay);
    }

    public void setFlashOnTouchCapture(boolean dbg) {
        this.mShouldFlash = dbg;
        this.mFlashFrac = 0.0f;
        postInvalidate();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int action = event.getAction();
        if (action == 4) {
            poke(event);
        } else if (action == 0) {
            int size = (int) getSize(event.getEventTime());
            if ((this.mVertical && event.getX() < size) || event.getY() < size) {
                Slog.v("DeadZone", "consuming errant click: (" + event.getX() + "," + event.getY() + ")");
                if (this.mShouldFlash) {
                    post(this.mDebugFlash);
                    postInvalidate();
                }
                return true;
            }
        }
        return false;
    }

    public void poke(MotionEvent event) {
        this.mLastPokeTime = event.getEventTime();
        if (this.mShouldFlash) {
            postInvalidate();
        }
    }

    @Override
    public void onDraw(Canvas can) {
        if (this.mShouldFlash && this.mFlashFrac > 0.0f) {
            int size = (int) getSize(SystemClock.uptimeMillis());
            int width = this.mVertical ? size : can.getWidth();
            if (this.mVertical) {
                size = can.getHeight();
            }
            can.clipRect(0, 0, width, size);
            float frac = this.mFlashFrac;
            can.drawARGB((int) (255.0f * frac), 221, 238, 170);
        }
    }
}
