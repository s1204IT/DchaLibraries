package com.android.server.telecom;

import android.media.ToneGenerator;
import android.os.Handler;
import android.os.Looper;
import android.telecom.Log;
import android.telecom.Logging.Runnable;
import android.telecom.Logging.Session;
import com.android.internal.annotations.VisibleForTesting;
import com.android.server.telecom.TelecomSystem;

public class InCallTonePlayer extends Thread {
    private static int sTonesPlaying = 0;
    private final CallAudioManager mCallAudioManager;
    private final CallAudioRoutePeripheralAdapter mCallAudioRoutePeripheralAdapter;
    private final TelecomSystem.SyncRoot mLock;
    private final Handler mMainThreadHandler;
    private Session mSession;
    private final Object mSessionLock;
    private int mState;
    private final ToneGeneratorFactory mToneGenerator;
    private final int mToneId;

    public interface ToneGeneratorFactory {
        ToneGenerator get(int i, int i2);
    }

    static int access$106() {
        int i = sTonesPlaying - 1;
        sTonesPlaying = i;
        return i;
    }

    public static class Factory {
        private CallAudioManager mCallAudioManager;
        private final CallAudioRoutePeripheralAdapter mCallAudioRoutePeripheralAdapter;
        private final TelecomSystem.SyncRoot mLock;
        private final ToneGeneratorFactory mToneGeneratorFactory;

        Factory(CallAudioRoutePeripheralAdapter callAudioRoutePeripheralAdapter, TelecomSystem.SyncRoot syncRoot, ToneGeneratorFactory toneGeneratorFactory) {
            this.mCallAudioRoutePeripheralAdapter = callAudioRoutePeripheralAdapter;
            this.mLock = syncRoot;
            this.mToneGeneratorFactory = toneGeneratorFactory;
        }

        public void setCallAudioManager(CallAudioManager callAudioManager) {
            this.mCallAudioManager = callAudioManager;
        }

        public InCallTonePlayer createPlayer(int i) {
            return new InCallTonePlayer(i, this.mCallAudioManager, this.mCallAudioRoutePeripheralAdapter, this.mLock, this.mToneGeneratorFactory);
        }
    }

    private InCallTonePlayer(int i, CallAudioManager callAudioManager, CallAudioRoutePeripheralAdapter callAudioRoutePeripheralAdapter, TelecomSystem.SyncRoot syncRoot, ToneGeneratorFactory toneGeneratorFactory) {
        this.mMainThreadHandler = new Handler(Looper.getMainLooper());
        this.mSessionLock = new Object();
        this.mState = 0;
        this.mToneId = i;
        this.mCallAudioManager = callAudioManager;
        this.mCallAudioRoutePeripheralAdapter = callAudioRoutePeripheralAdapter;
        this.mLock = syncRoot;
        this.mToneGenerator = toneGeneratorFactory;
    }

    @Override
    public void run() throws Throwable {
        int i;
        ToneGenerator toneGenerator = null;
        try {
            synchronized (this.mSessionLock) {
                if (this.mSession != null) {
                    Log.continueSession(this.mSession, "ICTP.r");
                    this.mSession = null;
                }
            }
            Log.d(this, "run(toneId = %s)", new Object[]{Integer.valueOf(this.mToneId)});
            int i2 = 375;
            int i3 = 95;
            int i4 = 80;
            switch (this.mToneId) {
                case 1:
                    i3 = 17;
                    i2 = 4000;
                    i = !this.mCallAudioRoutePeripheralAdapter.isBluetoothAudioOn() ? 6 : 0;
                    try {
                        Log.v(this, "Creating generator", new Object[0]);
                        ToneGenerator toneGenerator2 = this.mToneGenerator.get(i, i4);
                        try {
                            synchronized (this) {
                                if (this.mState != 2) {
                                    this.mState = 1;
                                    toneGenerator2.startTone(i3);
                                    try {
                                        int i5 = i2 + 20;
                                        Log.v(this, "Starting tone %d...waiting for %d ms.", new Object[]{Integer.valueOf(this.mToneId), Integer.valueOf(i5)});
                                        wait(i5);
                                    } catch (InterruptedException e) {
                                        Log.w(this, "wait interrupted", new Object[]{e});
                                    }
                                    break;
                                } else {
                                    break;
                                }
                            }
                            this.mState = 0;
                            if (toneGenerator2 != null) {
                                toneGenerator2.release();
                            }
                            cleanUpTonePlayer();
                            Log.endSession();
                            return;
                        } catch (Throwable th) {
                            th = th;
                            toneGenerator = toneGenerator2;
                            if (toneGenerator != null) {
                                toneGenerator.release();
                            }
                            cleanUpTonePlayer();
                            Log.endSession();
                            throw th;
                        }
                    } catch (RuntimeException e2) {
                        Log.w(this, "Failed to create ToneGenerator.", new Object[]{e2});
                        cleanUpTonePlayer();
                        Log.endSession();
                        return;
                    }
                case CallState.SELECT_PHONE_ACCOUNT:
                    i3 = 27;
                    i2 = 200;
                    if (!this.mCallAudioRoutePeripheralAdapter.isBluetoothAudioOn()) {
                    }
                    Log.v(this, "Creating generator", new Object[0]);
                    ToneGenerator toneGenerator22 = this.mToneGenerator.get(i, i4);
                    synchronized (this) {
                    }
                    break;
                case CallState.DIALING:
                    throw new IllegalStateException("OTA Call ended NYI.");
                case CallState.RINGING:
                    i2 = 2147483627;
                    i3 = 22;
                    if (!this.mCallAudioRoutePeripheralAdapter.isBluetoothAudioOn()) {
                    }
                    Log.v(this, "Creating generator", new Object[0]);
                    ToneGenerator toneGenerator222 = this.mToneGenerator.get(i, i4);
                    synchronized (this) {
                    }
                    break;
                case CallState.ACTIVE:
                    i4 = 50;
                    if (!this.mCallAudioRoutePeripheralAdapter.isBluetoothAudioOn()) {
                    }
                    Log.v(this, "Creating generator", new Object[0]);
                    ToneGenerator toneGenerator2222 = this.mToneGenerator.get(i, i4);
                    synchronized (this) {
                    }
                    break;
                case CallState.ON_HOLD:
                    i3 = 18;
                    i2 = 4000;
                    if (!this.mCallAudioRoutePeripheralAdapter.isBluetoothAudioOn()) {
                    }
                    Log.v(this, "Creating generator", new Object[0]);
                    ToneGenerator toneGenerator22222 = this.mToneGenerator.get(i, i4);
                    synchronized (this) {
                    }
                    break;
                case CallState.DISCONNECTED:
                    i3 = 37;
                    i2 = 500;
                    i4 = 50;
                    if (!this.mCallAudioRoutePeripheralAdapter.isBluetoothAudioOn()) {
                    }
                    Log.v(this, "Creating generator", new Object[0]);
                    ToneGenerator toneGenerator222222 = this.mToneGenerator.get(i, i4);
                    synchronized (this) {
                    }
                    break;
                case CallState.ABORTED:
                    i4 = 50;
                    if (!this.mCallAudioRoutePeripheralAdapter.isBluetoothAudioOn()) {
                    }
                    Log.v(this, "Creating generator", new Object[0]);
                    ToneGenerator toneGenerator2222222 = this.mToneGenerator.get(i, i4);
                    synchronized (this) {
                    }
                    break;
                case 9:
                    i3 = 87;
                    i2 = ConnectionServiceFocusManager.RELEASE_FOCUS_TIMEOUT_MS;
                    i4 = 50;
                    if (!this.mCallAudioRoutePeripheralAdapter.isBluetoothAudioOn()) {
                    }
                    Log.v(this, "Creating generator", new Object[0]);
                    ToneGenerator toneGenerator22222222 = this.mToneGenerator.get(i, i4);
                    synchronized (this) {
                    }
                    break;
                case CallState.PULLING:
                    i3 = 38;
                    i2 = 4000;
                    if (!this.mCallAudioRoutePeripheralAdapter.isBluetoothAudioOn()) {
                    }
                    Log.v(this, "Creating generator", new Object[0]);
                    ToneGenerator toneGenerator222222222 = this.mToneGenerator.get(i, i4);
                    synchronized (this) {
                    }
                    break;
                case 11:
                    i3 = 23;
                    i2 = 2147483627;
                    if (!this.mCallAudioRoutePeripheralAdapter.isBluetoothAudioOn()) {
                    }
                    Log.v(this, "Creating generator", new Object[0]);
                    ToneGenerator toneGenerator2222222222 = this.mToneGenerator.get(i, i4);
                    synchronized (this) {
                    }
                    break;
                case 12:
                    i3 = 21;
                    i2 = 4000;
                    if (!this.mCallAudioRoutePeripheralAdapter.isBluetoothAudioOn()) {
                    }
                    Log.v(this, "Creating generator", new Object[0]);
                    ToneGenerator toneGenerator22222222222 = this.mToneGenerator.get(i, i4);
                    synchronized (this) {
                    }
                    break;
                case 13:
                    throw new IllegalStateException("Voice privacy tone NYI.");
                case 14:
                    i3 = 22;
                    i2 = 4000;
                    if (!this.mCallAudioRoutePeripheralAdapter.isBluetoothAudioOn()) {
                    }
                    Log.v(this, "Creating generator", new Object[0]);
                    ToneGenerator toneGenerator222222222222 = this.mToneGenerator.get(i, i4);
                    synchronized (this) {
                    }
                    break;
                default:
                    throw new IllegalStateException("Bad toneId: " + this.mToneId);
            }
        } catch (Throwable th2) {
            th = th2;
        }
    }

    @VisibleForTesting
    public void startTone() {
        sTonesPlaying++;
        if (sTonesPlaying == 1) {
            this.mCallAudioManager.setIsTonePlaying(true);
        }
        synchronized (this.mSessionLock) {
            if (this.mSession != null) {
                Log.cancelSubsession(this.mSession);
            }
            this.mSession = Log.createSubsession();
        }
        super.start();
    }

    @Override
    public void start() {
        Log.w(this, "Do not call the start method directly; use startTone instead.", new Object[0]);
    }

    @VisibleForTesting
    public void stopTone() {
        synchronized (this) {
            if (this.mState == 1) {
                Log.d(this, "Stopping the tone %d.", new Object[]{Integer.valueOf(this.mToneId)});
                notify();
            }
            this.mState = 2;
        }
    }

    private void cleanUpTonePlayer() {
        this.mMainThreadHandler.post(new Runnable("ICTP.cUTP", this.mLock) {
            public void loggedRun() {
                if (InCallTonePlayer.sTonesPlaying == 0) {
                    Log.wtf(this, "Over-releasing focus for tone player.", new Object[0]);
                } else if (InCallTonePlayer.access$106() == 0) {
                    InCallTonePlayer.this.mCallAudioManager.setIsTonePlaying(false);
                }
            }
        }.prepare());
    }
}
