package com.android.server.telecom;

import com.android.internal.util.Preconditions;
import com.android.server.telecom.InCallTonePlayer;

class RingbackPlayer extends CallsManagerListenerBase {
    private Call mCall;
    private final CallsManager mCallsManager;
    private final InCallTonePlayer.Factory mPlayerFactory;
    private InCallTonePlayer mTonePlayer;

    RingbackPlayer(CallsManager callsManager, InCallTonePlayer.Factory factory) {
        this.mCallsManager = callsManager;
        this.mPlayerFactory = factory;
    }

    @Override
    public void onForegroundCallChanged(Call call, Call call2) {
        if (call != null) {
            stopRingbackForCall(call);
        }
        if (shouldStartRinging(call2)) {
            startRingbackForCall(call2);
        }
    }

    @Override
    public void onConnectionServiceChanged(Call call, ConnectionServiceWrapper connectionServiceWrapper, ConnectionServiceWrapper connectionServiceWrapper2) {
        if (shouldStartRinging(call)) {
            startRingbackForCall(call);
        } else if (connectionServiceWrapper2 == null) {
            stopRingbackForCall(call);
        }
    }

    @Override
    public void onRingbackRequested(Call call, boolean z) {
        if (shouldStartRinging(call)) {
            startRingbackForCall(call);
        } else {
            stopRingbackForCall(call);
        }
    }

    private void startRingbackForCall(Call call) {
        Preconditions.checkState(call.getState() == 3);
        ThreadUtil.checkOnMainThread();
        if (this.mCall == call) {
            Log.w(this, "Ignoring duplicate requests to ring for %s.", call);
            return;
        }
        if (this.mCall != null) {
            Log.wtf(this, "Ringback player thinks there are two foreground-dialing calls.", new Object[0]);
        }
        this.mCall = call;
        if (this.mTonePlayer == null) {
            Log.d(this, "Playing the ringback tone for %s.", call);
            this.mTonePlayer = this.mPlayerFactory.createPlayer(11);
            this.mTonePlayer.startTone();
        }
    }

    private void stopRingbackForCall(Call call) {
        ThreadUtil.checkOnMainThread();
        if (this.mCall == call) {
            this.mCall = null;
            if (this.mTonePlayer == null) {
                Log.w(this, "No player found to stop.", new Object[0]);
                return;
            }
            Log.i(this, "Stopping the ringback tone for %s.", call);
            this.mTonePlayer.stopTone();
            this.mTonePlayer = null;
        }
    }

    private boolean shouldStartRinging(Call call) {
        return call != null && this.mCallsManager.getForegroundCall() == call && call.getState() == 3 && call.isRingbackRequested();
    }
}
