package com.android.server.telecom;

import android.telecom.AudioState;
import com.android.server.telecom.InCallTonePlayer;

public final class InCallToneMonitor extends CallsManagerListenerBase {
    private final CallsManager mCallsManager;
    private final InCallTonePlayer.Factory mPlayerFactory;

    @Override
    public void onAudioStateChanged(AudioState audioState, AudioState audioState2) {
        super.onAudioStateChanged(audioState, audioState2);
    }

    @Override
    public void onCallAdded(Call call) {
        super.onCallAdded(call);
    }

    @Override
    public void onCallRemoved(Call call) {
        super.onCallRemoved(call);
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

    InCallToneMonitor(InCallTonePlayer.Factory factory, CallsManager callsManager) {
        this.mPlayerFactory = factory;
        this.mCallsManager = callsManager;
    }

    @Override
    public void onCallStateChanged(Call call, int i, int i2) {
        int i3 = 7;
        if (this.mCallsManager.getForegroundCall() == call && i2 == 7 && call.getDisconnectCause() != null) {
            Log.v(this, "Disconnect cause: %s.", call.getDisconnectCause());
            switch (call.getDisconnectCause().getTone()) {
                case 17:
                    i3 = 1;
                    break;
                case 18:
                    i3 = 6;
                    break;
                case 21:
                    i3 = 12;
                    break;
                case 27:
                    i3 = 2;
                    break;
                case 37:
                    break;
                case 38:
                    i3 = 10;
                    break;
                case 95:
                    i3 = 5;
                    break;
                default:
                    i3 = 0;
                    break;
            }
            Log.d(this, "Found a disconnected call with tone to play %d.", Integer.valueOf(i3));
            if (i3 != 0) {
                this.mPlayerFactory.createPlayer(i3).startTone();
            }
        }
    }
}
