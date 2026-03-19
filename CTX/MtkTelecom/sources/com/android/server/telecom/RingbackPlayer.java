package com.android.server.telecom;

import android.telecom.Log;
import com.android.internal.util.Preconditions;
import com.android.server.telecom.InCallTonePlayer;

public class RingbackPlayer {
    private Call mCall;
    private final InCallTonePlayer.Factory mPlayerFactory;
    private InCallTonePlayer mTonePlayer;

    RingbackPlayer(InCallTonePlayer.Factory factory) {
        this.mPlayerFactory = factory;
    }

    public void startRingbackForCall(Call call) {
        Preconditions.checkState(call.getState() == 3);
        if (this.mCall == call) {
            Log.w(this, "Ignoring duplicate requests to ring for %s.", new Object[]{call});
            return;
        }
        if (this.mCall != null) {
            Log.wtf(this, "Ringback player thinks there are two foreground-dialing calls.", new Object[0]);
        }
        this.mCall = call;
        if (this.mTonePlayer == null) {
            Log.i(this, "Playing the ringback tone for %s.", new Object[]{call});
            Log.addEvent(call, "START_RINGBACK");
            this.mTonePlayer = this.mPlayerFactory.createPlayer(11);
            this.mTonePlayer.startTone();
        }
    }

    public void stopRingbackForCall(Call call) {
        if (this.mCall == call) {
            this.mCall = null;
            if (this.mTonePlayer == null) {
                Log.w(this, "No player found to stop.", new Object[0]);
                return;
            }
            Log.i(this, "Stopping the ringback tone for %s.", new Object[]{call});
            Log.addEvent(call, "STOP_RINGBACK");
            this.mTonePlayer.stopTone();
            this.mTonePlayer = null;
        }
    }
}
