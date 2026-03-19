package com.android.server.audio;

import android.content.Context;
import android.media.AudioSystem;
import android.os.Handler;
import android.util.Log;
import android.view.OrientationEventListener;
import android.view.WindowManager;
import com.android.server.SystemService;
import com.android.server.policy.WindowOrientationListener;

class RotationHelper {
    private static final String TAG = "AudioService.RotationHelper";
    private static Context sContext;
    private static AudioOrientationListener sOrientationListener;
    private static AudioWindowOrientationListener sWindowOrientationListener;
    private static final Object sRotationLock = new Object();
    private static int sDeviceRotation = 0;

    RotationHelper() {
    }

    static void init(Context context, Handler handler) {
        if (context == null) {
            throw new IllegalArgumentException("Invalid null context");
        }
        sContext = context;
        sWindowOrientationListener = new AudioWindowOrientationListener(context, handler);
        sWindowOrientationListener.enable();
        if (sWindowOrientationListener.canDetectOrientation()) {
            return;
        }
        Log.i(TAG, "Not using WindowOrientationListener, reverting to OrientationListener");
        sWindowOrientationListener.disable();
        sWindowOrientationListener = null;
        sOrientationListener = new AudioOrientationListener(context);
        sOrientationListener.enable();
    }

    static void enable() {
        if (sWindowOrientationListener != null) {
            sWindowOrientationListener.enable();
        } else {
            sOrientationListener.enable();
        }
        updateOrientation();
    }

    static void disable() {
        if (sWindowOrientationListener != null) {
            sWindowOrientationListener.disable();
        } else {
            sOrientationListener.disable();
        }
    }

    static void updateOrientation() {
        int newRotation = ((WindowManager) sContext.getSystemService("window")).getDefaultDisplay().getRotation();
        synchronized (sRotationLock) {
            if (newRotation != sDeviceRotation) {
                sDeviceRotation = newRotation;
                publishRotation(sDeviceRotation);
            }
        }
    }

    private static void publishRotation(int rotation) {
        Log.v(TAG, "publishing device rotation =" + rotation + " (x90deg)");
        switch (rotation) {
            case 0:
                AudioSystem.setParameters("rotation=0");
                break;
            case 1:
                AudioSystem.setParameters("rotation=90");
                break;
            case 2:
                AudioSystem.setParameters("rotation=180");
                break;
            case 3:
                AudioSystem.setParameters("rotation=270");
                break;
            default:
                Log.e(TAG, "Unknown device rotation");
                break;
        }
    }

    static final class AudioOrientationListener extends OrientationEventListener {
        AudioOrientationListener(Context context) {
            super(context);
        }

        @Override
        public void onOrientationChanged(int orientation) {
            RotationHelper.updateOrientation();
        }
    }

    static final class AudioWindowOrientationListener extends WindowOrientationListener {
        private static RotationCheckThread sRotationCheckThread;

        AudioWindowOrientationListener(Context context, Handler handler) {
            super(context, handler);
        }

        @Override
        public void onProposedRotationChanged(int rotation) {
            RotationHelper.updateOrientation();
            if (sRotationCheckThread != null) {
                sRotationCheckThread.endCheck();
            }
            sRotationCheckThread = new RotationCheckThread();
            sRotationCheckThread.beginCheck();
        }
    }

    static final class RotationCheckThread extends Thread {
        private final int[] WAIT_TIMES_MS;
        private final Object mCounterLock;
        private int mWaitCounter;

        RotationCheckThread() {
            super("RotationCheck");
            this.WAIT_TIMES_MS = new int[]{10, 20, 50, 100, 100, 200, 200, SystemService.PHASE_SYSTEM_SERVICES_READY};
            this.mCounterLock = new Object();
        }

        void beginCheck() {
            synchronized (this.mCounterLock) {
                this.mWaitCounter = 0;
            }
            try {
                start();
            } catch (IllegalStateException e) {
            }
        }

        void endCheck() {
            synchronized (this.mCounterLock) {
                this.mWaitCounter = this.WAIT_TIMES_MS.length;
            }
        }

        @Override
        public void run() {
            int waitTimeMs;
            while (this.mWaitCounter < this.WAIT_TIMES_MS.length) {
                synchronized (this.mCounterLock) {
                    waitTimeMs = this.mWaitCounter < this.WAIT_TIMES_MS.length ? this.WAIT_TIMES_MS[this.mWaitCounter] : 0;
                    this.mWaitCounter++;
                }
                if (waitTimeMs > 0) {
                    try {
                        sleep(waitTimeMs);
                        RotationHelper.updateOrientation();
                    } catch (InterruptedException e) {
                    }
                }
            }
        }
    }
}
