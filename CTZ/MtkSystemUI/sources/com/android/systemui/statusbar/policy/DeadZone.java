package com.android.systemui.statusbar.policy;

import android.animation.ObjectAnimator;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.os.SystemClock;
import android.util.Slog;
import android.view.MotionEvent;
import com.android.systemui.R;
import com.android.systemui.SysUiServiceProvider;
import com.android.systemui.statusbar.phone.NavigationBarView;
import com.android.systemui.statusbar.phone.StatusBar;

public class DeadZone {
    private int mDecay;
    private int mDisplayRotation;
    private int mHold;
    private long mLastPokeTime;
    private final NavigationBarView mNavigationBarView;
    private boolean mShouldFlash;
    private int mSizeMax;
    private int mSizeMin;
    private final StatusBar mStatusBar;
    private boolean mVertical;
    private float mFlashFrac = 0.0f;
    private final Runnable mDebugFlash = new Runnable() {
        @Override
        public void run() {
            ObjectAnimator.ofFloat(DeadZone.this, "flash", 1.0f, 0.0f).setDuration(150L).start();
        }
    };

    public DeadZone(NavigationBarView navigationBarView) {
        this.mNavigationBarView = navigationBarView;
        this.mStatusBar = (StatusBar) SysUiServiceProvider.getComponent(this.mNavigationBarView.getContext(), StatusBar.class);
        onConfigurationChanged(0);
    }

    static float lerp(float f, float f2, float f3) {
        return ((f2 - f) * f3) + f;
    }

    private float getSize(long j) {
        if (this.mSizeMax == 0) {
            return 0.0f;
        }
        long j2 = j - this.mLastPokeTime;
        if (j2 > this.mHold + this.mDecay) {
            return this.mSizeMin;
        }
        if (j2 < this.mHold) {
            return this.mSizeMax;
        }
        return (int) lerp(this.mSizeMax, this.mSizeMin, (j2 - ((long) this.mHold)) / this.mDecay);
    }

    public void setFlashOnTouchCapture(boolean z) {
        this.mShouldFlash = z;
        this.mFlashFrac = 0.0f;
        this.mNavigationBarView.postInvalidate();
    }

    public void onConfigurationChanged(int i) {
        this.mDisplayRotation = i;
        Resources resources = this.mNavigationBarView.getResources();
        this.mHold = resources.getInteger(R.integer.navigation_bar_deadzone_hold);
        this.mDecay = resources.getInteger(R.integer.navigation_bar_deadzone_decay);
        this.mSizeMin = resources.getDimensionPixelSize(R.dimen.navigation_bar_deadzone_size);
        this.mSizeMax = resources.getDimensionPixelSize(R.dimen.navigation_bar_deadzone_size_max);
        this.mVertical = resources.getInteger(R.integer.navigation_bar_deadzone_orientation) == 1;
        setFlashOnTouchCapture(resources.getBoolean(R.bool.config_dead_zone_flash));
    }

    public boolean onTouchEvent(MotionEvent motionEvent) {
        if (motionEvent.getToolType(0) == 3) {
            return false;
        }
        int action = motionEvent.getAction();
        if (action == 4) {
            poke(motionEvent);
            return true;
        }
        if (action == 0) {
            if (this.mStatusBar != null) {
                this.mStatusBar.touchAutoDim();
            }
            int size = (int) getSize(motionEvent.getEventTime());
            if (!this.mVertical ? motionEvent.getY() >= ((float) size) : this.mDisplayRotation != 3 ? motionEvent.getX() >= ((float) size) : motionEvent.getX() <= ((float) (this.mNavigationBarView.getWidth() - size))) {
                Slog.v("DeadZone", "consuming errant click: (" + motionEvent.getX() + "," + motionEvent.getY() + ")");
                if (this.mShouldFlash) {
                    this.mNavigationBarView.post(this.mDebugFlash);
                    this.mNavigationBarView.postInvalidate();
                }
                return true;
            }
        }
        return false;
    }

    private void poke(MotionEvent motionEvent) {
        this.mLastPokeTime = motionEvent.getEventTime();
        if (this.mShouldFlash) {
            this.mNavigationBarView.postInvalidate();
        }
    }

    public void onDraw(Canvas canvas) {
        if (!this.mShouldFlash || this.mFlashFrac <= 0.0f) {
            return;
        }
        int size = (int) getSize(SystemClock.uptimeMillis());
        if (this.mVertical) {
            if (this.mDisplayRotation == 3) {
                canvas.clipRect(canvas.getWidth() - size, 0, canvas.getWidth(), canvas.getHeight());
            } else {
                canvas.clipRect(0, 0, size, canvas.getHeight());
            }
        } else {
            canvas.clipRect(0, 0, canvas.getWidth(), size);
        }
        canvas.drawARGB((int) (this.mFlashFrac * 255.0f), 221, 238, 170);
    }
}
