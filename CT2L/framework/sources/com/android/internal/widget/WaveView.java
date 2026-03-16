package com.android.internal.widget;

import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.media.AudioAttributes;
import android.os.Vibrator;
import android.provider.Settings;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.accessibility.AccessibilityManager;
import com.android.internal.R;
import java.util.ArrayList;

public class WaveView extends View implements ValueAnimator.AnimatorUpdateListener {
    private static final boolean DBG = false;
    private static final long DELAY_INCREMENT = 15;
    private static final long DELAY_INCREMENT2 = 12;
    private static final long DURATION = 300;
    private static final long FINAL_DELAY = 200;
    private static final long FINAL_DURATION = 200;
    private static final float GRAB_HANDLE_RADIUS_SCALE_ACCESSIBILITY_DISABLED = 0.5f;
    private static final float GRAB_HANDLE_RADIUS_SCALE_ACCESSIBILITY_ENABLED = 1.0f;
    private static final long RESET_TIMEOUT = 3000;
    private static final long RING_DELAY = 1300;
    private static final long SHORT_DELAY = 100;
    private static final int STATE_ATTEMPTING = 3;
    private static final int STATE_READY = 1;
    private static final int STATE_RESET_LOCK = 0;
    private static final int STATE_START_ATTEMPT = 2;
    private static final int STATE_UNLOCK_ATTEMPT = 4;
    private static final int STATE_UNLOCK_SUCCESS = 5;
    private static final String TAG = "WaveView";
    private static final long VIBRATE_LONG = 20;
    private static final long VIBRATE_SHORT = 20;
    private static final AudioAttributes VIBRATION_ATTRIBUTES = new AudioAttributes.Builder().setContentType(4).setUsage(13).build();
    private static final int WAVE_COUNT = 20;
    private static final long WAVE_DELAY = 100;
    private static final long WAVE_DURATION = 2000;
    private final Runnable mAddWaveAction;
    private int mCurrentWave;
    private ArrayList<DrawableHolder> mDrawables;
    private boolean mFingerDown;
    private boolean mFinishWaves;
    private int mGrabbedState;
    private ArrayList<DrawableHolder> mLightWaves;
    private float mLockCenterX;
    private float mLockCenterY;
    private int mLockState;
    private final Runnable mLockTimerActions;
    private float mMouseX;
    private float mMouseY;
    private OnTriggerListener mOnTriggerListener;
    private float mRingRadius;
    private int mSnapRadius;
    private DrawableHolder mUnlockDefault;
    private DrawableHolder mUnlockHalo;
    private DrawableHolder mUnlockRing;
    private Vibrator mVibrator;
    private int mWaveCount;
    private long mWaveTimerDelay;
    private boolean mWavesRunning;

    public interface OnTriggerListener {
        public static final int CENTER_HANDLE = 10;
        public static final int NO_HANDLE = 0;

        void onGrabbedStateChange(View view, int i);

        void onTrigger(View view, int i);
    }

    static long access$614(WaveView x0, long x1) {
        long j = x0.mWaveTimerDelay + x1;
        x0.mWaveTimerDelay = j;
        return j;
    }

    public WaveView(Context context) {
        this(context, null);
    }

    public WaveView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mDrawables = new ArrayList<>(3);
        this.mLightWaves = new ArrayList<>(20);
        this.mFingerDown = false;
        this.mRingRadius = 182.0f;
        this.mSnapRadius = 136;
        this.mWaveCount = 20;
        this.mWaveTimerDelay = 100L;
        this.mCurrentWave = 0;
        this.mLockState = 0;
        this.mGrabbedState = 0;
        this.mLockTimerActions = new Runnable() {
            @Override
            public void run() {
                if (WaveView.this.mLockState == 3) {
                    WaveView.this.mLockState = 0;
                }
                if (WaveView.this.mLockState == 5) {
                    WaveView.this.mLockState = 0;
                }
                WaveView.this.invalidate();
            }
        };
        this.mAddWaveAction = new Runnable() {
            @Override
            public void run() {
                double distX = WaveView.this.mMouseX - WaveView.this.mLockCenterX;
                double distY = WaveView.this.mMouseY - WaveView.this.mLockCenterY;
                int dragDistance = (int) Math.ceil(Math.hypot(distX, distY));
                if (WaveView.this.mLockState == 3 && dragDistance < WaveView.this.mSnapRadius && WaveView.this.mWaveTimerDelay >= 100) {
                    WaveView.this.mWaveTimerDelay = Math.min(WaveView.WAVE_DURATION, WaveView.this.mWaveTimerDelay + WaveView.DELAY_INCREMENT);
                    DrawableHolder wave = (DrawableHolder) WaveView.this.mLightWaves.get(WaveView.this.mCurrentWave);
                    wave.setAlpha(0.0f);
                    wave.setScaleX(0.2f);
                    wave.setScaleY(0.2f);
                    wave.setX(WaveView.this.mMouseX);
                    wave.setY(WaveView.this.mMouseY);
                    wave.addAnimTo(WaveView.WAVE_DURATION, 0L, "x", WaveView.this.mLockCenterX, true);
                    wave.addAnimTo(WaveView.WAVE_DURATION, 0L, "y", WaveView.this.mLockCenterY, true);
                    wave.addAnimTo(1333L, 0L, "alpha", 1.0f, true);
                    wave.addAnimTo(WaveView.WAVE_DURATION, 0L, "scaleX", 1.0f, true);
                    wave.addAnimTo(WaveView.WAVE_DURATION, 0L, "scaleY", 1.0f, true);
                    wave.addAnimTo(1000L, WaveView.RING_DELAY, "alpha", 0.0f, false);
                    wave.startAnimations(WaveView.this);
                    WaveView.this.mCurrentWave = (WaveView.this.mCurrentWave + 1) % WaveView.this.mWaveCount;
                } else {
                    WaveView.access$614(WaveView.this, WaveView.DELAY_INCREMENT2);
                }
                if (WaveView.this.mFinishWaves) {
                    WaveView.this.mWavesRunning = false;
                } else {
                    WaveView.this.postDelayed(WaveView.this.mAddWaveAction, WaveView.this.mWaveTimerDelay);
                }
            }
        };
        initDrawables();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        this.mLockCenterX = w * GRAB_HANDLE_RADIUS_SCALE_ACCESSIBILITY_DISABLED;
        this.mLockCenterY = h * GRAB_HANDLE_RADIUS_SCALE_ACCESSIBILITY_DISABLED;
        super.onSizeChanged(w, h, oldw, oldh);
    }

    @Override
    protected int getSuggestedMinimumWidth() {
        return this.mUnlockRing.getWidth() + this.mUnlockHalo.getWidth();
    }

    @Override
    protected int getSuggestedMinimumHeight() {
        return this.mUnlockRing.getHeight() + this.mUnlockHalo.getHeight();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width;
        int height;
        int widthSpecMode = View.MeasureSpec.getMode(widthMeasureSpec);
        int heightSpecMode = View.MeasureSpec.getMode(heightMeasureSpec);
        int widthSpecSize = View.MeasureSpec.getSize(widthMeasureSpec);
        int heightSpecSize = View.MeasureSpec.getSize(heightMeasureSpec);
        if (widthSpecMode == Integer.MIN_VALUE) {
            width = Math.min(widthSpecSize, getSuggestedMinimumWidth());
        } else if (widthSpecMode == 1073741824) {
            width = widthSpecSize;
        } else {
            width = getSuggestedMinimumWidth();
        }
        if (heightSpecMode == Integer.MIN_VALUE) {
            height = Math.min(heightSpecSize, getSuggestedMinimumWidth());
        } else if (heightSpecMode == 1073741824) {
            height = heightSpecSize;
        } else {
            height = getSuggestedMinimumHeight();
        }
        setMeasuredDimension(width, height);
    }

    private void initDrawables() {
        this.mUnlockRing = new DrawableHolder(createDrawable(R.drawable.unlock_ring));
        this.mUnlockRing.setX(this.mLockCenterX);
        this.mUnlockRing.setY(this.mLockCenterY);
        this.mUnlockRing.setScaleX(0.1f);
        this.mUnlockRing.setScaleY(0.1f);
        this.mUnlockRing.setAlpha(0.0f);
        this.mDrawables.add(this.mUnlockRing);
        this.mUnlockDefault = new DrawableHolder(createDrawable(R.drawable.unlock_default));
        this.mUnlockDefault.setX(this.mLockCenterX);
        this.mUnlockDefault.setY(this.mLockCenterY);
        this.mUnlockDefault.setScaleX(0.1f);
        this.mUnlockDefault.setScaleY(0.1f);
        this.mUnlockDefault.setAlpha(0.0f);
        this.mDrawables.add(this.mUnlockDefault);
        this.mUnlockHalo = new DrawableHolder(createDrawable(R.drawable.unlock_halo));
        this.mUnlockHalo.setX(this.mLockCenterX);
        this.mUnlockHalo.setY(this.mLockCenterY);
        this.mUnlockHalo.setScaleX(0.1f);
        this.mUnlockHalo.setScaleY(0.1f);
        this.mUnlockHalo.setAlpha(0.0f);
        this.mDrawables.add(this.mUnlockHalo);
        BitmapDrawable wave = createDrawable(R.drawable.unlock_wave);
        for (int i = 0; i < this.mWaveCount; i++) {
            DrawableHolder holder = new DrawableHolder(wave);
            this.mLightWaves.add(holder);
            holder.setAlpha(0.0f);
        }
    }

    private void waveUpdateFrame(float mouseX, float mouseY, boolean fingerDown) {
        double distX = mouseX - this.mLockCenterX;
        double distY = mouseY - this.mLockCenterY;
        int dragDistance = (int) Math.ceil(Math.hypot(distX, distY));
        double touchA = Math.atan2(distX, distY);
        float ringX = (float) (((double) this.mLockCenterX) + (((double) this.mRingRadius) * Math.sin(touchA)));
        float ringY = (float) (((double) this.mLockCenterY) + (((double) this.mRingRadius) * Math.cos(touchA)));
        switch (this.mLockState) {
            case 0:
                this.mWaveTimerDelay = 100L;
                for (int i = 0; i < this.mLightWaves.size(); i++) {
                    DrawableHolder holder = this.mLightWaves.get(i);
                    holder.addAnimTo(DURATION, 0L, "alpha", 0.0f, false);
                }
                for (int i2 = 0; i2 < this.mLightWaves.size(); i2++) {
                    this.mLightWaves.get(i2).startAnimations(this);
                }
                this.mUnlockRing.addAnimTo(DURATION, 0L, "x", this.mLockCenterX, true);
                this.mUnlockRing.addAnimTo(DURATION, 0L, "y", this.mLockCenterY, true);
                this.mUnlockRing.addAnimTo(DURATION, 0L, "scaleX", 0.1f, true);
                this.mUnlockRing.addAnimTo(DURATION, 0L, "scaleY", 0.1f, true);
                this.mUnlockRing.addAnimTo(DURATION, 0L, "alpha", 0.0f, true);
                this.mUnlockDefault.removeAnimationFor("x");
                this.mUnlockDefault.removeAnimationFor("y");
                this.mUnlockDefault.removeAnimationFor("scaleX");
                this.mUnlockDefault.removeAnimationFor("scaleY");
                this.mUnlockDefault.removeAnimationFor("alpha");
                this.mUnlockDefault.setX(this.mLockCenterX);
                this.mUnlockDefault.setY(this.mLockCenterY);
                this.mUnlockDefault.setScaleX(0.1f);
                this.mUnlockDefault.setScaleY(0.1f);
                this.mUnlockDefault.setAlpha(0.0f);
                this.mUnlockDefault.addAnimTo(DURATION, 100L, "scaleX", 1.0f, true);
                this.mUnlockDefault.addAnimTo(DURATION, 100L, "scaleY", 1.0f, true);
                this.mUnlockDefault.addAnimTo(DURATION, 100L, "alpha", 1.0f, true);
                this.mUnlockHalo.removeAnimationFor("x");
                this.mUnlockHalo.removeAnimationFor("y");
                this.mUnlockHalo.removeAnimationFor("scaleX");
                this.mUnlockHalo.removeAnimationFor("scaleY");
                this.mUnlockHalo.removeAnimationFor("alpha");
                this.mUnlockHalo.setX(this.mLockCenterX);
                this.mUnlockHalo.setY(this.mLockCenterY);
                this.mUnlockHalo.setScaleX(0.1f);
                this.mUnlockHalo.setScaleY(0.1f);
                this.mUnlockHalo.setAlpha(0.0f);
                this.mUnlockHalo.addAnimTo(DURATION, 100L, "x", this.mLockCenterX, true);
                this.mUnlockHalo.addAnimTo(DURATION, 100L, "y", this.mLockCenterY, true);
                this.mUnlockHalo.addAnimTo(DURATION, 100L, "scaleX", 1.0f, true);
                this.mUnlockHalo.addAnimTo(DURATION, 100L, "scaleY", 1.0f, true);
                this.mUnlockHalo.addAnimTo(DURATION, 100L, "alpha", 1.0f, true);
                removeCallbacks(this.mLockTimerActions);
                this.mLockState = 1;
                break;
            case 1:
                this.mWaveTimerDelay = 100L;
                break;
            case 2:
                this.mUnlockDefault.removeAnimationFor("x");
                this.mUnlockDefault.removeAnimationFor("y");
                this.mUnlockDefault.removeAnimationFor("scaleX");
                this.mUnlockDefault.removeAnimationFor("scaleY");
                this.mUnlockDefault.removeAnimationFor("alpha");
                this.mUnlockDefault.setX(this.mLockCenterX + 182.0f);
                this.mUnlockDefault.setY(this.mLockCenterY);
                this.mUnlockDefault.setScaleX(0.1f);
                this.mUnlockDefault.setScaleY(0.1f);
                this.mUnlockDefault.setAlpha(0.0f);
                this.mUnlockDefault.addAnimTo(DURATION, 100L, "scaleX", 1.0f, false);
                this.mUnlockDefault.addAnimTo(DURATION, 100L, "scaleY", 1.0f, false);
                this.mUnlockDefault.addAnimTo(DURATION, 100L, "alpha", 1.0f, false);
                this.mUnlockRing.addAnimTo(DURATION, 0L, "scaleX", 1.0f, true);
                this.mUnlockRing.addAnimTo(DURATION, 0L, "scaleY", 1.0f, true);
                this.mUnlockRing.addAnimTo(DURATION, 0L, "alpha", 1.0f, true);
                this.mLockState = 3;
                break;
            case 3:
                if (dragDistance > this.mSnapRadius) {
                    this.mFinishWaves = true;
                    if (fingerDown) {
                        this.mUnlockHalo.addAnimTo(0L, 0L, "x", ringX, true);
                        this.mUnlockHalo.addAnimTo(0L, 0L, "y", ringY, true);
                        this.mUnlockHalo.addAnimTo(0L, 0L, "scaleX", 1.0f, true);
                        this.mUnlockHalo.addAnimTo(0L, 0L, "scaleY", 1.0f, true);
                        this.mUnlockHalo.addAnimTo(0L, 0L, "alpha", 1.0f, true);
                    } else {
                        this.mLockState = 4;
                    }
                } else {
                    if (!this.mWavesRunning) {
                        this.mWavesRunning = true;
                        this.mFinishWaves = false;
                        postDelayed(this.mAddWaveAction, this.mWaveTimerDelay);
                    }
                    this.mUnlockHalo.addAnimTo(0L, 0L, "x", mouseX, true);
                    this.mUnlockHalo.addAnimTo(0L, 0L, "y", mouseY, true);
                    this.mUnlockHalo.addAnimTo(0L, 0L, "scaleX", 1.0f, true);
                    this.mUnlockHalo.addAnimTo(0L, 0L, "scaleY", 1.0f, true);
                    this.mUnlockHalo.addAnimTo(0L, 0L, "alpha", 1.0f, true);
                }
                break;
            case 4:
                if (dragDistance > this.mSnapRadius) {
                    for (int n = 0; n < this.mLightWaves.size(); n++) {
                        DrawableHolder wave = this.mLightWaves.get(n);
                        long delay = (1000 * ((long) ((n + 6) - this.mCurrentWave))) / 10;
                        wave.addAnimTo(200L, delay, "x", ringX, true);
                        wave.addAnimTo(200L, delay, "y", ringY, true);
                        wave.addAnimTo(200L, delay, "scaleX", 0.1f, true);
                        wave.addAnimTo(200L, delay, "scaleY", 0.1f, true);
                        wave.addAnimTo(200L, delay, "alpha", 0.0f, true);
                    }
                    for (int i3 = 0; i3 < this.mLightWaves.size(); i3++) {
                        this.mLightWaves.get(i3).startAnimations(this);
                    }
                    this.mUnlockRing.addAnimTo(200L, 0L, "x", ringX, false);
                    this.mUnlockRing.addAnimTo(200L, 0L, "y", ringY, false);
                    this.mUnlockRing.addAnimTo(200L, 0L, "scaleX", 0.1f, false);
                    this.mUnlockRing.addAnimTo(200L, 0L, "scaleY", 0.1f, false);
                    this.mUnlockRing.addAnimTo(200L, 0L, "alpha", 0.0f, false);
                    this.mUnlockRing.addAnimTo(200L, 200L, "alpha", 0.0f, false);
                    this.mUnlockDefault.removeAnimationFor("x");
                    this.mUnlockDefault.removeAnimationFor("y");
                    this.mUnlockDefault.removeAnimationFor("scaleX");
                    this.mUnlockDefault.removeAnimationFor("scaleY");
                    this.mUnlockDefault.removeAnimationFor("alpha");
                    this.mUnlockDefault.setX(ringX);
                    this.mUnlockDefault.setY(ringY);
                    this.mUnlockDefault.setScaleX(0.1f);
                    this.mUnlockDefault.setScaleY(0.1f);
                    this.mUnlockDefault.setAlpha(0.0f);
                    this.mUnlockDefault.addAnimTo(200L, 0L, "x", ringX, true);
                    this.mUnlockDefault.addAnimTo(200L, 0L, "y", ringY, true);
                    this.mUnlockDefault.addAnimTo(200L, 0L, "scaleX", 1.0f, true);
                    this.mUnlockDefault.addAnimTo(200L, 0L, "scaleY", 1.0f, true);
                    this.mUnlockDefault.addAnimTo(200L, 0L, "alpha", 1.0f, true);
                    this.mUnlockDefault.addAnimTo(200L, 200L, "scaleX", 3.0f, false);
                    this.mUnlockDefault.addAnimTo(200L, 200L, "scaleY", 3.0f, false);
                    this.mUnlockDefault.addAnimTo(200L, 200L, "alpha", 0.0f, false);
                    this.mUnlockHalo.addAnimTo(200L, 0L, "x", ringX, false);
                    this.mUnlockHalo.addAnimTo(200L, 0L, "y", ringY, false);
                    this.mUnlockHalo.addAnimTo(200L, 200L, "scaleX", 3.0f, false);
                    this.mUnlockHalo.addAnimTo(200L, 200L, "scaleY", 3.0f, false);
                    this.mUnlockHalo.addAnimTo(200L, 200L, "alpha", 0.0f, false);
                    removeCallbacks(this.mLockTimerActions);
                    postDelayed(this.mLockTimerActions, RESET_TIMEOUT);
                    dispatchTriggerEvent(10);
                    this.mLockState = 5;
                } else {
                    this.mLockState = 0;
                }
                break;
            case 5:
                removeCallbacks(this.mAddWaveAction);
                break;
        }
        this.mUnlockDefault.startAnimations(this);
        this.mUnlockHalo.startAnimations(this);
        this.mUnlockRing.startAnimations(this);
    }

    BitmapDrawable createDrawable(int resId) {
        Resources res = getResources();
        Bitmap bitmap = BitmapFactory.decodeResource(res, resId);
        return new BitmapDrawable(res, bitmap);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        waveUpdateFrame(this.mMouseX, this.mMouseY, this.mFingerDown);
        for (int i = 0; i < this.mDrawables.size(); i++) {
            this.mDrawables.get(i).draw(canvas);
        }
        for (int i2 = 0; i2 < this.mLightWaves.size(); i2++) {
            this.mLightWaves.get(i2).draw(canvas);
        }
    }

    @Override
    public boolean onHoverEvent(MotionEvent event) {
        if (AccessibilityManager.getInstance(this.mContext).isTouchExplorationEnabled()) {
            int action = event.getAction();
            switch (action) {
                case 7:
                    event.setAction(2);
                    break;
                case 9:
                    event.setAction(0);
                    break;
                case 10:
                    event.setAction(1);
                    break;
            }
            onTouchEvent(event);
            event.setAction(action);
        }
        return super.onHoverEvent(event);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int action = event.getAction();
        this.mMouseX = event.getX();
        this.mMouseY = event.getY();
        boolean handled = false;
        switch (action) {
            case 0:
                removeCallbacks(this.mLockTimerActions);
                this.mFingerDown = true;
                tryTransitionToStartAttemptState(event);
                handled = true;
                break;
            case 1:
                this.mFingerDown = false;
                postDelayed(this.mLockTimerActions, RESET_TIMEOUT);
                setGrabbedState(0);
                waveUpdateFrame(this.mMouseX, this.mMouseY, this.mFingerDown);
                handled = true;
                break;
            case 2:
                tryTransitionToStartAttemptState(event);
                handled = true;
                break;
            case 3:
                this.mFingerDown = false;
                handled = true;
                break;
        }
        invalidate();
        if (handled) {
            return true;
        }
        return super.onTouchEvent(event);
    }

    private void tryTransitionToStartAttemptState(MotionEvent event) {
        float dx = event.getX() - this.mUnlockHalo.getX();
        float dy = event.getY() - this.mUnlockHalo.getY();
        float dist = (float) Math.hypot(dx, dy);
        if (dist <= getScaledGrabHandleRadius()) {
            setGrabbedState(10);
            if (this.mLockState == 1) {
                this.mLockState = 2;
                if (AccessibilityManager.getInstance(this.mContext).isEnabled()) {
                    announceUnlockHandle();
                }
            }
        }
    }

    private float getScaledGrabHandleRadius() {
        return AccessibilityManager.getInstance(this.mContext).isEnabled() ? 1.0f * this.mUnlockHalo.getWidth() : GRAB_HANDLE_RADIUS_SCALE_ACCESSIBILITY_DISABLED * this.mUnlockHalo.getWidth();
    }

    private void announceUnlockHandle() {
        setContentDescription(this.mContext.getString(R.string.description_target_unlock_tablet));
        sendAccessibilityEvent(8);
        setContentDescription(null);
    }

    private synchronized void vibrate(long duration) {
        synchronized (this) {
            boolean hapticEnabled = Settings.System.getIntForUser(this.mContext.getContentResolver(), Settings.System.HAPTIC_FEEDBACK_ENABLED, 1, -2) != 0;
            if (hapticEnabled) {
                if (this.mVibrator == null) {
                    this.mVibrator = (Vibrator) getContext().getSystemService(Context.VIBRATOR_SERVICE);
                }
                this.mVibrator.vibrate(duration, VIBRATION_ATTRIBUTES);
            }
        }
    }

    public void setOnTriggerListener(OnTriggerListener listener) {
        this.mOnTriggerListener = listener;
    }

    private void dispatchTriggerEvent(int whichHandle) {
        vibrate(20L);
        if (this.mOnTriggerListener != null) {
            this.mOnTriggerListener.onTrigger(this, whichHandle);
        }
    }

    private void setGrabbedState(int newState) {
        if (newState != this.mGrabbedState) {
            this.mGrabbedState = newState;
            if (this.mOnTriggerListener != null) {
                this.mOnTriggerListener.onGrabbedStateChange(this, this.mGrabbedState);
            }
        }
    }

    @Override
    public void onAnimationUpdate(ValueAnimator animation) {
        invalidate();
    }

    public void reset() {
        this.mLockState = 0;
        invalidate();
    }
}
