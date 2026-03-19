package com.android.server.telecom;

import android.telecom.CallAudioState;
import android.telecom.VideoProfile;
import com.android.server.telecom.CallsManager;

public class CallsManagerListenerBase implements CallsManager.CallsManagerListener {
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
    public void onCallAudioStateChanged(CallAudioState callAudioState, CallAudioState callAudioState2) {
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
    public void onVideoStateChanged(Call call, int i, int i2) {
    }

    @Override
    public void onCanAddCallChanged(boolean z) {
    }

    @Override
    public void onSessionModifyRequestReceived(Call call, VideoProfile videoProfile) {
    }

    @Override
    public void onHoldToneRequested(Call call) {
    }

    @Override
    public void onExternalCallChanged(Call call, boolean z) {
    }

    @Override
    public void onDisconnectedTonePlaying(boolean z) {
    }

    @Override
    public void onCallAlertingNotified(Call call) {
    }
}
