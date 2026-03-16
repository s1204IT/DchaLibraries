package com.android.server.telecom;

import android.content.Context;
import android.os.PowerManager;
import android.telecom.AudioState;

public class ProximitySensorManager extends CallsManagerListenerBase {
    private static final String TAG = ProximitySensorManager.class.getSimpleName();
    private final PowerManager.WakeLock mProximityWakeLock;

    @Override
    public void onAudioStateChanged(AudioState audioState, AudioState audioState2) {
        super.onAudioStateChanged(audioState, audioState2);
    }

    @Override
    public void onCallAdded(Call call) {
        super.onCallAdded(call);
    }

    @Override
    public void onCallStateChanged(Call call, int i, int i2) {
        super.onCallStateChanged(call, i, i2);
    }

    @Override
    public void onCanAddCallChanged(boolean z) {
        super.onCanAddCallChanged(z);
    }

    @Override
    public void onConnectionServiceChanged(Call call, ConnectionServiceWrapper connectionServiceWrapper, ConnectionServiceWrapper connectionServiceWrapper2) {
        super.onConnectionServiceChanged(call, connectionServiceWrapper, connectionServiceWrapper2);
    }

    @Override
    public void onForegroundCallChanged(Call call, Call call2) {
        super.onForegroundCallChanged(call, call2);
    }

    @Override
    public void onIncomingCallAnswered(Call call) {
        super.onIncomingCallAnswered(call);
    }

    @Override
    public void onIncomingCallRejected(Call call, boolean z, String str) {
        super.onIncomingCallRejected(call, z, str);
    }

    @Override
    public void onIsConferencedChanged(Call call) {
        super.onIsConferencedChanged(call);
    }

    @Override
    public void onIsVoipAudioModeChanged(Call call) {
        super.onIsVoipAudioModeChanged(call);
    }

    @Override
    public void onRingbackRequested(Call call, boolean z) {
        super.onRingbackRequested(call, z);
    }

    @Override
    public void onVideoStateChanged(Call call) {
        super.onVideoStateChanged(call);
    }

    public ProximitySensorManager(Context context) {
        PowerManager powerManager = (PowerManager) context.getSystemService("power");
        if (powerManager.isWakeLockLevelSupported(32)) {
            this.mProximityWakeLock = powerManager.newWakeLock(32, TAG);
        } else {
            this.mProximityWakeLock = null;
        }
        Log.d(this, "onCreate: mProximityWakeLock: ", this.mProximityWakeLock);
    }

    @Override
    public void onCallRemoved(Call call) {
        if (CallsManager.getInstance().getCalls().isEmpty()) {
            Log.i(this, "All calls removed, resetting proximity sensor to default state", new Object[0]);
            turnOff(true);
        }
        super.onCallRemoved(call);
    }

    void turnOn() {
        if (CallsManager.getInstance().getCalls().isEmpty()) {
            Log.w(this, "Asking to turn on prox sensor without a call? I don't think so.", new Object[0]);
            return;
        }
        if (this.mProximityWakeLock != null) {
            if (!this.mProximityWakeLock.isHeld()) {
                Log.i(this, "Acquiring proximity wake lock", new Object[0]);
                this.mProximityWakeLock.acquire();
            } else {
                Log.i(this, "Proximity wake lock already acquired", new Object[0]);
            }
        }
    }

    void turnOff(boolean z) {
        if (this.mProximityWakeLock != null) {
            if (this.mProximityWakeLock.isHeld()) {
                Log.i(this, "Releasing proximity wake lock", new Object[0]);
                this.mProximityWakeLock.release(z ? 0 : 1);
            } else {
                Log.i(this, "Proximity wake lock already released", new Object[0]);
            }
        }
    }
}
