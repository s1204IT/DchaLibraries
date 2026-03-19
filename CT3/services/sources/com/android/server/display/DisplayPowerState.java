package com.android.server.display;

import android.content.Context;
import android.net.dhcp.DhcpPacket;
import android.os.Build;
import android.os.Handler;
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
    private static boolean DEBUG = "eng".equals(Build.TYPE);
    public static final FloatProperty<DisplayPowerState> COLOR_FADE_LEVEL = new FloatProperty<DisplayPowerState>("electronBeamLevel") {
        @Override
        public void setValue(DisplayPowerState object, float value) {
            object.setColorFadeLevel(value);
        }

        @Override
        public Float get(DisplayPowerState object) {
            return Float.valueOf(object.getColorFadeLevel());
        }
    };
    public static final IntProperty<DisplayPowerState> SCREEN_BRIGHTNESS = new IntProperty<DisplayPowerState>("screenBrightness") {
        @Override
        public void setValue(DisplayPowerState object, int value) {
            object.setScreenBrightness(value);
        }

        @Override
        public Integer get(DisplayPowerState object) {
            return Integer.valueOf(object.getScreenBrightness());
        }
    };
    private int mDelay = 0;
    private final Runnable mScreenUpdateRunnable = new Runnable() {
        @Override
        public void run() {
            DisplayPowerState.this.mScreenUpdatePending = false;
            int brightness = (DisplayPowerState.this.mScreenState == 1 || DisplayPowerState.this.mColorFadeLevel <= 0.0f) ? 0 : DisplayPowerState.this.mScreenBrightness;
            if (!DisplayPowerState.this.mPhotonicModulator.setState(DisplayPowerState.this.mScreenState, brightness)) {
                return;
            }
            DisplayPowerState.this.mScreenReady = true;
            DisplayPowerState.this.invokeCleanListenerIfNeeded();
        }
    };
    private final Runnable mColorFadeDrawRunnable = new Runnable() {
        @Override
        public void run() {
            DisplayPowerState.this.mColorFadeDrawPending = false;
            if (DisplayPowerState.this.mColorFadePrepared) {
                DisplayPowerState.this.mColorFade.draw(DisplayPowerState.this.mColorFadeLevel);
            }
            DisplayPowerState.this.mColorFadeReady = true;
            DisplayPowerState.this.invokeCleanListenerIfNeeded();
        }
    };
    private final Handler mHandler = new Handler(true);
    private final Choreographer mChoreographer = Choreographer.getInstance();
    private final PhotonicModulator mPhotonicModulator = new PhotonicModulator();

    public DisplayPowerState(DisplayBlanker blanker, ColorFade colorFade) {
        this.mBlanker = blanker;
        this.mColorFade = colorFade;
        this.mPhotonicModulator.start();
        this.mScreenState = 2;
        this.mScreenBrightness = DhcpPacket.MAX_OPTION_LEN;
        scheduleScreenUpdate();
        this.mColorFadePrepared = false;
        this.mColorFadeLevel = 1.0f;
        this.mColorFadeReady = true;
    }

    public void setIPOScreenOnDelay(int delay_msec) {
        if (DEBUG) {
            Slog.d(TAG, "setIPOScreenOnDelay: delay_msec=" + delay_msec);
        }
        this.mDelay = delay_msec;
    }

    public void setScreenState(int state) {
        if (this.mScreenState == state) {
            return;
        }
        if (DEBUG) {
            Slog.d(TAG, "setScreenState: state=" + state);
        }
        this.mScreenState = state;
        this.mScreenReady = false;
        scheduleScreenUpdate();
    }

    public int getScreenState() {
        return this.mScreenState;
    }

    public void setScreenBrightness(int brightness) {
        if (this.mScreenBrightness == brightness) {
            return;
        }
        if (DEBUG) {
            Slog.d(TAG, "setScreenBrightness: brightness=" + brightness);
        }
        this.mScreenBrightness = brightness;
        if (this.mScreenState == 1) {
            return;
        }
        this.mScreenReady = false;
        scheduleScreenUpdate();
    }

    public int getScreenBrightness() {
        return this.mScreenBrightness;
    }

    public boolean prepareColorFade(Context context, int mode) {
        if (!this.mColorFade.prepare(context, mode)) {
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
        this.mColorFade.dismiss();
        this.mColorFadePrepared = false;
        this.mColorFadeReady = true;
    }

    public void dismissColorFadeResources() {
        this.mColorFade.dismissResources();
    }

    public void setColorFadeLevel(float level) {
        if (this.mColorFadeLevel == level) {
            return;
        }
        if (DEBUG) {
            Slog.d(TAG, "setColorFadeLevel: level=" + level);
        }
        this.mColorFadeLevel = level;
        if (this.mScreenState != 1) {
            this.mScreenReady = false;
            scheduleScreenUpdate();
        }
        if (!this.mColorFadePrepared) {
            return;
        }
        this.mColorFadeReady = false;
        scheduleColorFadeDraw();
    }

    public float getColorFadeLevel() {
        return this.mColorFadeLevel;
    }

    public boolean waitUntilClean(Runnable listener) {
        if (!this.mScreenReady || !this.mColorFadeReady) {
            this.mCleanListener = listener;
            return false;
        }
        this.mCleanListener = null;
        return true;
    }

    public void dump(PrintWriter pw) {
        pw.println();
        pw.println("Display Power State:");
        pw.println("  mScreenState=" + Display.stateToString(this.mScreenState));
        pw.println("  mScreenBrightness=" + this.mScreenBrightness);
        pw.println("  mScreenReady=" + this.mScreenReady);
        pw.println("  mScreenUpdatePending=" + this.mScreenUpdatePending);
        pw.println("  mColorFadePrepared=" + this.mColorFadePrepared);
        pw.println("  mColorFadeLevel=" + this.mColorFadeLevel);
        pw.println("  mColorFadeReady=" + this.mColorFadeReady);
        pw.println("  mColorFadeDrawPending=" + this.mColorFadeDrawPending);
        this.mPhotonicModulator.dump(pw);
        this.mColorFade.dump(pw);
    }

    private void scheduleScreenUpdate() {
        if (this.mScreenUpdatePending) {
            return;
        }
        this.mScreenUpdatePending = true;
        postScreenUpdateThreadSafe();
    }

    private void postScreenUpdateThreadSafe() {
        this.mHandler.removeCallbacks(this.mScreenUpdateRunnable);
        this.mHandler.post(this.mScreenUpdateRunnable);
    }

    private void scheduleColorFadeDraw() {
        if (this.mColorFadeDrawPending) {
            return;
        }
        this.mColorFadeDrawPending = true;
        this.mChoreographer.postCallback(2, this.mColorFadeDrawRunnable, null);
    }

    private void invokeCleanListenerIfNeeded() {
        Runnable listener = this.mCleanListener;
        if (listener == null || !this.mScreenReady || !this.mColorFadeReady) {
            return;
        }
        this.mCleanListener = null;
        listener.run();
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

        public boolean setState(int state, int backlight) {
            boolean z;
            synchronized (this.mLock) {
                boolean stateChanged = state != this.mPendingState;
                boolean backlightChanged = backlight != this.mPendingBacklight;
                if (stateChanged || backlightChanged) {
                    if (DisplayPowerState.DEBUG) {
                        Slog.d(DisplayPowerState.TAG, "Requesting new screen state: state=" + Display.stateToString(state) + ", backlight=" + backlight);
                    }
                    this.mPendingState = state;
                    this.mPendingBacklight = backlight;
                    boolean z2 = !this.mStateChangeInProgress ? this.mBacklightChangeInProgress : true;
                    this.mStateChangeInProgress = stateChanged;
                    this.mBacklightChangeInProgress = backlightChanged;
                    if (!z2) {
                        this.mLock.notifyAll();
                    }
                }
                z = !this.mStateChangeInProgress;
            }
            return z;
        }

        public void dump(PrintWriter pw) {
            synchronized (this.mLock) {
                pw.println();
                pw.println("Photonic Modulator State:");
                pw.println("  mPendingState=" + Display.stateToString(this.mPendingState));
                pw.println("  mPendingBacklight=" + this.mPendingBacklight);
                pw.println("  mActualState=" + Display.stateToString(this.mActualState));
                pw.println("  mActualBacklight=" + this.mActualBacklight);
                pw.println("  mStateChangeInProgress=" + this.mStateChangeInProgress);
                pw.println("  mBacklightChangeInProgress=" + this.mBacklightChangeInProgress);
            }
        }

        @Override
        public void run() {
            while (true) {
                synchronized (this.mLock) {
                    int state = this.mPendingState;
                    boolean stateChanged = state != this.mActualState;
                    int backlight = this.mPendingBacklight;
                    boolean backlightChanged = backlight != this.mActualBacklight;
                    if (!stateChanged) {
                        DisplayPowerState.this.postScreenUpdateThreadSafe();
                        this.mStateChangeInProgress = false;
                    }
                    if (!backlightChanged) {
                        this.mBacklightChangeInProgress = false;
                    }
                    if (stateChanged || backlightChanged) {
                        this.mActualState = state;
                        this.mActualBacklight = backlight;
                        if (DisplayPowerState.DEBUG) {
                            Slog.d(DisplayPowerState.TAG, "Updating screen state: state=" + Display.stateToString(state) + ", backlight=" + backlight + ", backlightChanged=" + backlightChanged);
                        }
                        DisplayPowerState.this.mBlanker.requestDisplayState(state, backlight);
                    } else {
                        try {
                            this.mLock.wait();
                        } catch (InterruptedException e) {
                        }
                    }
                }
            }
        }
    }
}
