package com.android.server.display;

import android.content.Context;
import android.os.Handler;
import android.os.Trace;
import android.util.FloatProperty;
import android.util.IntProperty;
import android.util.Slog;
import android.view.Choreographer;
import android.view.Display;
import java.io.PrintWriter;

final class DisplayPowerState {
    private static final String TAG = "DisplayPowerState";
    private final DisplayBlanker mBlanker;
    private Runnable mCleanListener;
    private final ColorFade mColorFade;
    private boolean mColorFadeDrawPending;
    private float mColorFadeLevel;
    private boolean mColorFadePrepared;
    private boolean mColorFadeReady;
    private int mScreenBrightness;
    private boolean mScreenReady;
    private int mScreenState;
    private boolean mScreenUpdatePending;
    private static boolean DEBUG = false;
    private static String COUNTER_COLOR_FADE = "ColorFadeLevel";
    public static final FloatProperty<DisplayPowerState> COLOR_FADE_LEVEL = new FloatProperty<DisplayPowerState>("electronBeamLevel") {
        @Override
        public void setValue(DisplayPowerState displayPowerState, float f) {
            displayPowerState.setColorFadeLevel(f);
        }

        @Override
        public Float get(DisplayPowerState displayPowerState) {
            return Float.valueOf(displayPowerState.getColorFadeLevel());
        }
    };
    public static final IntProperty<DisplayPowerState> SCREEN_BRIGHTNESS = new IntProperty<DisplayPowerState>("screenBrightness") {
        @Override
        public void setValue(DisplayPowerState displayPowerState, int i) {
            displayPowerState.setScreenBrightness(i);
        }

        @Override
        public Integer get(DisplayPowerState displayPowerState) {
            return Integer.valueOf(displayPowerState.getScreenBrightness());
        }
    };
    private final Runnable mScreenUpdateRunnable = new Runnable() {
        @Override
        public void run() {
            int i = 0;
            DisplayPowerState.this.mScreenUpdatePending = false;
            if (DisplayPowerState.this.mScreenState != 1 && DisplayPowerState.this.mColorFadeLevel > 0.0f) {
                i = DisplayPowerState.this.mScreenBrightness;
            }
            if (DisplayPowerState.this.mPhotonicModulator.setState(DisplayPowerState.this.mScreenState, i)) {
                if (DisplayPowerState.DEBUG) {
                    Slog.d(DisplayPowerState.TAG, "Screen ready");
                }
                DisplayPowerState.this.mScreenReady = true;
                DisplayPowerState.this.invokeCleanListenerIfNeeded();
                return;
            }
            if (DisplayPowerState.DEBUG) {
                Slog.d(DisplayPowerState.TAG, "Screen not ready");
            }
        }
    };
    private final Runnable mColorFadeDrawRunnable = new Runnable() {
        @Override
        public void run() {
            DisplayPowerState.this.mColorFadeDrawPending = false;
            if (DisplayPowerState.this.mColorFadePrepared) {
                DisplayPowerState.this.mColorFade.draw(DisplayPowerState.this.mColorFadeLevel);
                Trace.traceCounter(131072L, DisplayPowerState.COUNTER_COLOR_FADE, Math.round(DisplayPowerState.this.mColorFadeLevel * 100.0f));
            }
            DisplayPowerState.this.mColorFadeReady = true;
            DisplayPowerState.this.invokeCleanListenerIfNeeded();
        }
    };
    private final Handler mHandler = new Handler(true);
    private final Choreographer mChoreographer = Choreographer.getInstance();
    private final PhotonicModulator mPhotonicModulator = new PhotonicModulator();

    public DisplayPowerState(DisplayBlanker displayBlanker, ColorFade colorFade) {
        this.mBlanker = displayBlanker;
        this.mColorFade = colorFade;
        this.mPhotonicModulator.start();
        this.mScreenState = 2;
        this.mScreenBrightness = 255;
        scheduleScreenUpdate();
        this.mColorFadePrepared = false;
        this.mColorFadeLevel = 1.0f;
        this.mColorFadeReady = true;
    }

    public void setScreenState(int i) {
        if (this.mScreenState != i) {
            if (DEBUG) {
                Slog.d(TAG, "setScreenState: state=" + i);
            }
            this.mScreenState = i;
            this.mScreenReady = false;
            scheduleScreenUpdate();
        }
    }

    public int getScreenState() {
        return this.mScreenState;
    }

    public void setScreenBrightness(int i) {
        if (this.mScreenBrightness != i) {
            if (DEBUG) {
                Slog.d(TAG, "setScreenBrightness: brightness=" + i);
            }
            this.mScreenBrightness = i;
            if (this.mScreenState != 1) {
                this.mScreenReady = false;
                scheduleScreenUpdate();
            }
        }
    }

    public int getScreenBrightness() {
        return this.mScreenBrightness;
    }

    public boolean prepareColorFade(Context context, int i) {
        if (this.mColorFade == null || !this.mColorFade.prepare(context, i)) {
            this.mColorFadePrepared = false;
            this.mColorFadeReady = true;
            return false;
        }
        this.mColorFadePrepared = true;
        this.mColorFadeReady = false;
        scheduleColorFadeDraw();
        return true;
    }

    public void dismissColorFade() {
        Trace.traceCounter(131072L, COUNTER_COLOR_FADE, 100);
        if (this.mColorFade != null) {
            this.mColorFade.dismiss();
        }
        this.mColorFadePrepared = false;
        this.mColorFadeReady = true;
    }

    public void dismissColorFadeResources() {
        if (this.mColorFade != null) {
            this.mColorFade.dismissResources();
        }
    }

    public void setColorFadeLevel(float f) {
        if (this.mColorFadeLevel != f) {
            if (DEBUG) {
                Slog.d(TAG, "setColorFadeLevel: level=" + f);
            }
            this.mColorFadeLevel = f;
            if (this.mScreenState != 1) {
                this.mScreenReady = false;
                scheduleScreenUpdate();
            }
            if (this.mColorFadePrepared) {
                this.mColorFadeReady = false;
                scheduleColorFadeDraw();
            }
        }
    }

    public float getColorFadeLevel() {
        return this.mColorFadeLevel;
    }

    public boolean waitUntilClean(Runnable runnable) {
        if (!this.mScreenReady || !this.mColorFadeReady) {
            this.mCleanListener = runnable;
            return false;
        }
        this.mCleanListener = null;
        return true;
    }

    public void dump(PrintWriter printWriter) {
        printWriter.println();
        printWriter.println("Display Power State:");
        printWriter.println("  mScreenState=" + Display.stateToString(this.mScreenState));
        printWriter.println("  mScreenBrightness=" + this.mScreenBrightness);
        printWriter.println("  mScreenReady=" + this.mScreenReady);
        printWriter.println("  mScreenUpdatePending=" + this.mScreenUpdatePending);
        printWriter.println("  mColorFadePrepared=" + this.mColorFadePrepared);
        printWriter.println("  mColorFadeLevel=" + this.mColorFadeLevel);
        printWriter.println("  mColorFadeReady=" + this.mColorFadeReady);
        printWriter.println("  mColorFadeDrawPending=" + this.mColorFadeDrawPending);
        this.mPhotonicModulator.dump(printWriter);
        if (this.mColorFade != null) {
            this.mColorFade.dump(printWriter);
        }
    }

    private void scheduleScreenUpdate() {
        if (!this.mScreenUpdatePending) {
            this.mScreenUpdatePending = true;
            postScreenUpdateThreadSafe();
        }
    }

    private void postScreenUpdateThreadSafe() {
        this.mHandler.removeCallbacks(this.mScreenUpdateRunnable);
        this.mHandler.post(this.mScreenUpdateRunnable);
    }

    private void scheduleColorFadeDraw() {
        if (!this.mColorFadeDrawPending) {
            this.mColorFadeDrawPending = true;
            this.mChoreographer.postCallback(2, this.mColorFadeDrawRunnable, null);
        }
    }

    private void invokeCleanListenerIfNeeded() {
        Runnable runnable = this.mCleanListener;
        if (runnable != null && this.mScreenReady && this.mColorFadeReady) {
            this.mCleanListener = null;
            runnable.run();
        }
    }

    private final class PhotonicModulator extends Thread {
        private static final int INITIAL_BACKLIGHT = -1;
        private static final int INITIAL_SCREEN_STATE = 1;
        private int mActualBacklight;
        private int mActualState;
        private boolean mBacklightChangeInProgress;
        private final Object mLock;
        private int mPendingBacklight;
        private int mPendingState;
        private boolean mStateChangeInProgress;

        public PhotonicModulator() {
            super("PhotonicModulator");
            this.mLock = new Object();
            this.mPendingState = 1;
            this.mPendingBacklight = -1;
            this.mActualState = 1;
            this.mActualBacklight = -1;
        }

        public boolean setState(int i, int i2) {
            boolean z;
            synchronized (this.mLock) {
                boolean z2 = false;
                boolean z3 = i != this.mPendingState;
                boolean z4 = i2 != this.mPendingBacklight;
                if (z3 || z4) {
                    if (DisplayPowerState.DEBUG) {
                        Slog.d(DisplayPowerState.TAG, "Requesting new screen state: state=" + Display.stateToString(i) + ", backlight=" + i2);
                    }
                    this.mPendingState = i;
                    this.mPendingBacklight = i2;
                    boolean z5 = this.mStateChangeInProgress || this.mBacklightChangeInProgress;
                    this.mStateChangeInProgress = z3 || this.mStateChangeInProgress;
                    if (z4 || this.mBacklightChangeInProgress) {
                        z2 = true;
                    }
                    this.mBacklightChangeInProgress = z2;
                    if (!z5) {
                        this.mLock.notifyAll();
                    }
                }
                z = !this.mStateChangeInProgress;
            }
            return z;
        }

        public void dump(PrintWriter printWriter) {
            synchronized (this.mLock) {
                printWriter.println();
                printWriter.println("Photonic Modulator State:");
                printWriter.println("  mPendingState=" + Display.stateToString(this.mPendingState));
                printWriter.println("  mPendingBacklight=" + this.mPendingBacklight);
                printWriter.println("  mActualState=" + Display.stateToString(this.mActualState));
                printWriter.println("  mActualBacklight=" + this.mActualBacklight);
                printWriter.println("  mStateChangeInProgress=" + this.mStateChangeInProgress);
                printWriter.println("  mBacklightChangeInProgress=" + this.mBacklightChangeInProgress);
            }
        }

        @Override
        public void run() {
            while (true) {
                synchronized (this.mLock) {
                    int i = this.mPendingState;
                    boolean z = true;
                    boolean z2 = i != this.mActualState;
                    int i2 = this.mPendingBacklight;
                    if (i2 == this.mActualBacklight) {
                        z = false;
                    }
                    if (!z2) {
                        DisplayPowerState.this.postScreenUpdateThreadSafe();
                        this.mStateChangeInProgress = false;
                    }
                    if (!z) {
                        this.mBacklightChangeInProgress = false;
                    }
                    if (!z2 && !z) {
                        try {
                            this.mLock.wait();
                        } catch (InterruptedException e) {
                        }
                    } else {
                        this.mActualState = i;
                        this.mActualBacklight = i2;
                        if (DisplayPowerState.DEBUG) {
                            Slog.d(DisplayPowerState.TAG, "Updating screen state: state=" + Display.stateToString(i) + ", backlight=" + i2);
                        }
                        DisplayPowerState.this.mBlanker.requestDisplayState(i, i2);
                    }
                }
            }
        }
    }
}
