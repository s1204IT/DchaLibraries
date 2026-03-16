package com.android.server.telecom;

import android.telecom.AudioState;
import com.android.server.telecom.CallsManager;

class CallsManagerListenerBase implements CallsManager.CallsManagerListener {
    CallsManagerListenerBase() {
    }

    @Override
    public void onCallAdded(Call call) {
    }

    @Override
    public void onCallRemoved(Call call) {
    }

    @Override
    public void onCallStateChanged(Call call, int i, int i2) {
    }

    @Override
    public void onConnectionServiceChanged(Call call, ConnectionServiceWrapper connectionServiceWrapper, ConnectionServiceWrapper connectionServiceWrapper2) {
    }

    @Override
    public void onIncomingCallAnswered(Call call) {
    }

    @Override
    public void onIncomingCallRejected(Call call, boolean z, String str) {
    }

    @Override
    public void onForegroundCallChanged(Call call, Call call2) {
    }

    @Override
    public void onAudioStateChanged(AudioState audioState, AudioState audioState2) {
    }

    @Override
    public void onRingbackRequested(Call call, boolean z) {
    }

    @Override
    public void onIsConferencedChanged(Call call) {
    }

    @Override
    public void onIsVoipAudioModeChanged(Call call) {
    }

    @Override
    public void onVideoStateChanged(Call call) {
    }

    @Override
    public void onCanAddCallChanged(boolean z) {
    }
}
