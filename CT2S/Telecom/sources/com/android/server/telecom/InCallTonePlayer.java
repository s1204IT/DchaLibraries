package com.android.server.telecom;

import android.media.ToneGenerator;
import android.os.Handler;
import android.os.Looper;

public final class InCallTonePlayer extends Thread {
    private static int sTonesPlaying = 0;
    private final CallAudioManager mCallAudioManager;
    private final Handler mMainThreadHandler;
    private int mState;
    private final int mToneId;

    static int access$106() {
        int i = sTonesPlaying - 1;
        sTonesPlaying = i;
        return i;
    }

    public static class Factory {
        private final CallAudioManager mCallAudioManager;

        Factory(CallAudioManager callAudioManager) {
            this.mCallAudioManager = callAudioManager;
        }

        InCallTonePlayer createPlayer(int i) {
            return new InCallTonePlayer(i, this.mCallAudioManager);
        }
    }

    private InCallTonePlayer(int i, CallAudioManager callAudioManager) {
        this.mMainThreadHandler = new Handler(Looper.getMainLooper());
        this.mState = 0;
        this.mToneId = i;
        this.mCallAudioManager = callAudioManager;
    }

    @Override
    public void run() throws Throwable {
        int i;
        int i2;
        ToneGenerator toneGenerator;
        int i3 = 50;
        ToneGenerator toneGenerator2 = null;
        try {
            Log.d(this, "run(toneId = %s)", Integer.valueOf(this.mToneId));
            switch (this.mToneId) {
                case 1:
                    i = 17;
                    i3 = 80;
                    i2 = 4000;
                    break;
                case 2:
                    i = 27;
                    i3 = 80;
                    i2 = 200;
                    break;
                case 3:
                    throw new IllegalStateException("OTA Call ended NYI.");
                case 4:
                    i = 22;
                    i3 = 80;
                    i2 = 2147483627;
                    break;
                case 5:
                    i = 95;
                    i2 = 375;
                    break;
                case 6:
                    i = 18;
                    i3 = 80;
                    i2 = 4000;
                    break;
                case 7:
                    i = 37;
                    i2 = 500;
                    break;
                case 8:
                    i = 95;
                    i2 = 375;
                    break;
                case 9:
                    i = 87;
                    i2 = 5000;
                    break;
                case 10:
                    i = 38;
                    i3 = 80;
                    i2 = 4000;
                    break;
                case 11:
                    i = 23;
                    i3 = 80;
                    i2 = 2147483627;
                    break;
                case 12:
                    i = 21;
                    i3 = 80;
                    i2 = 4000;
                    break;
                case 13:
                    throw new IllegalStateException("Voice privacy tone NYI.");
                default:
                    throw new IllegalStateException("Bad toneId: " + this.mToneId);
            }
            int i4 = this.mCallAudioManager.isBluetoothAudioOn() ? 6 : 0;
            try {
                Log.v(this, "Creating generator", new Object[0]);
                toneGenerator = new ToneGenerator(i4, i3);
            } catch (RuntimeException e) {
                Log.w(this, "Failed to create ToneGenerator.", e);
                if (0 != 0) {
                    toneGenerator2.release();
                }
                cleanUpTonePlayer();
                return;
            }
        } catch (Throwable th) {
            th = th;
        }
        try {
            synchronized (this) {
                if (this.mState != 2) {
                    this.mState = 1;
                    toneGenerator.startTone(i);
                    try {
                        Log.v(this, "Starting tone %d...waiting for %d ms.", Integer.valueOf(this.mToneId), Integer.valueOf(i2 + 20));
                        wait(i2 + 20);
                    } catch (InterruptedException e2) {
                        Log.w(this, "wait interrupted", e2);
                    }
                }
            }
            this.mState = 0;
            if (toneGenerator != null) {
                toneGenerator.release();
            }
            cleanUpTonePlayer();
        } catch (Throwable th2) {
            th = th2;
            toneGenerator2 = toneGenerator;
            if (toneGenerator2 != null) {
                toneGenerator2.release();
            }
            cleanUpTonePlayer();
            throw th;
        }
    }

    void startTone() {
        ThreadUtil.checkOnMainThread();
        sTonesPlaying++;
        if (sTonesPlaying == 1) {
            this.mCallAudioManager.setIsTonePlaying(true);
        }
        start();
    }

    void stopTone() {
        synchronized (this) {
            if (this.mState == 1) {
                Log.d(this, "Stopping the tone %d.", Integer.valueOf(this.mToneId));
                notify();
            }
            this.mState = 2;
        }
    }

    private void cleanUpTonePlayer() {
        this.mMainThreadHandler.post(new Runnable() {
            @Override
            public void run() {
                if (InCallTonePlayer.sTonesPlaying == 0) {
                    Log.wtf(this, "Over-releasing focus for tone player.", new Object[0]);
                } else if (InCallTonePlayer.access$106() == 0) {
                    InCallTonePlayer.this.mCallAudioManager.setIsTonePlaying(false);
                }
            }
        });
    }
}
