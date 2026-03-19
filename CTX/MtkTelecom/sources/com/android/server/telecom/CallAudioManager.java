package com.android.server.telecom;

import android.media.IAudioService;
import android.telecom.CallAudioState;
import android.telecom.Log;
import android.telecom.VideoProfile;
import android.util.SparseArray;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.util.IndentingPrintWriter;
import com.android.server.telecom.CallAudioModeStateMachine;
import com.android.server.telecom.InCallTonePlayer;
import com.android.server.telecom.bluetooth.BluetoothStateReceiver;
import com.mediatek.server.telecom.MtkUtil;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;

public class CallAudioManager extends CallsManagerListenerBase {
    private final BluetoothStateReceiver mBluetoothStateReceiver;
    private final CallAudioModeStateMachine mCallAudioModeStateMachine;
    private final CallAudioRouteStateMachine mCallAudioRouteStateMachine;
    private final CallsManager mCallsManager;
    private final DtmfLocalTonePlayer mDtmfLocalTonePlayer;
    private Call mForegroundCall;
    private InCallTonePlayer mHoldTonePlayer;
    private final InCallTonePlayer.Factory mPlayerFactory;
    private final RingbackPlayer mRingbackPlayer;
    private final Ringer mRinger;
    private final String LOG_TAG = CallAudioManager.class.getSimpleName();
    private boolean mIsTonePlaying = false;
    private boolean mIsDisconnectedTonePlaying = false;
    private final LinkedHashSet<Call> mActiveDialingOrConnectingCalls = new LinkedHashSet<>();
    private final LinkedHashSet<Call> mRingingCalls = new LinkedHashSet<>();
    private final LinkedHashSet<Call> mHoldingCalls = new LinkedHashSet<>();
    private final Set<Call> mCalls = new HashSet();
    private final SparseArray<LinkedHashSet<Call>> mCallStateToCalls = new SparseArray<LinkedHashSet<Call>>() {
        {
            put(1, CallAudioManager.this.mActiveDialingOrConnectingCalls);
            put(5, CallAudioManager.this.mActiveDialingOrConnectingCalls);
            put(3, CallAudioManager.this.mActiveDialingOrConnectingCalls);
            put(10, CallAudioManager.this.mActiveDialingOrConnectingCalls);
            put(4, CallAudioManager.this.mRingingCalls);
            put(6, CallAudioManager.this.mHoldingCalls);
        }
    };

    public interface AudioServiceFactory {
        IAudioService getAudioService();
    }

    public CallAudioManager(CallAudioRouteStateMachine callAudioRouteStateMachine, CallsManager callsManager, CallAudioModeStateMachine callAudioModeStateMachine, InCallTonePlayer.Factory factory, Ringer ringer, RingbackPlayer ringbackPlayer, BluetoothStateReceiver bluetoothStateReceiver, DtmfLocalTonePlayer dtmfLocalTonePlayer) {
        this.mCallAudioRouteStateMachine = callAudioRouteStateMachine;
        this.mCallAudioModeStateMachine = callAudioModeStateMachine;
        this.mCallsManager = callsManager;
        this.mPlayerFactory = factory;
        this.mRinger = ringer;
        this.mRingbackPlayer = ringbackPlayer;
        this.mBluetoothStateReceiver = bluetoothStateReceiver;
        this.mDtmfLocalTonePlayer = dtmfLocalTonePlayer;
        this.mPlayerFactory.setCallAudioManager(this);
        this.mCallAudioModeStateMachine.setCallAudioManager(this);
        this.mCallAudioRouteStateMachine.setCallAudioManager(this);
    }

    @Override
    public void onCallStateChanged(Call call, int i, int i2) {
        if (shouldIgnoreCallForAudio(call)) {
            return;
        }
        Log.d(this.LOG_TAG, "Call state changed for TC@%s: %s -> %s", new Object[]{call.getId(), CallState.toString(i), CallState.toString(i2)});
        for (int i3 = 0; i3 < this.mCallStateToCalls.size(); i3++) {
            this.mCallStateToCalls.valueAt(i3).remove(call);
        }
        if (this.mCallStateToCalls.get(i2) != null) {
            this.mCallStateToCalls.get(i2).add(call);
        }
        updateForegroundCall();
        if (shouldPlayDisconnectTone(i, i2)) {
            playToneForDisconnectedCall(call);
        }
        onCallLeavingState(call, i);
        onCallEnteringState(call, i2);
        if (call != null && call.isEmergencyCall() && i == 3 && i2 == 1) {
            this.mCallAudioModeStateMachine.sendMessageWithArgs(10001, makeArgsForModeStateMachine());
        }
    }

    @Override
    public void onCallAdded(Call call) {
        if (shouldIgnoreCallForAudio(call)) {
            return;
        }
        addCall(call);
    }

    @Override
    public void onCallRemoved(Call call) {
        if (shouldIgnoreCallForAudio(call)) {
            return;
        }
        removeCall(call);
        if (this.mCallsManager.hasEmergencyCall()) {
            Log.d(this, "unmute if there has ecc", new Object[0]);
            mute(false);
        }
    }

    private void addCall(Call call) {
        if (this.mCalls.contains(call)) {
            Log.w(this.LOG_TAG, "Call TC@%s is being added twice.", new Object[]{call.getId()});
            return;
        }
        Log.d(this.LOG_TAG, "Call added with id TC@%s in state %s", new Object[]{call.getId(), CallState.toString(call.getState())});
        if (this.mCallStateToCalls.get(call.getState()) != null) {
            this.mCallStateToCalls.get(call.getState()).add(call);
        }
        updateForegroundCall();
        this.mCalls.add(call);
        if (this.mCalls.size() == 1) {
            this.mBluetoothStateReceiver.setIsInCall(true);
        }
        onCallEnteringState(call, call.getState());
    }

    private void removeCall(Call call) {
        if (!this.mCalls.contains(call)) {
            return;
        }
        Log.d(this.LOG_TAG, "Call removed with id TC@%s in state %s", new Object[]{call.getId(), CallState.toString(call.getState())});
        for (int i = 0; i < this.mCallStateToCalls.size(); i++) {
            this.mCallStateToCalls.valueAt(i).remove(call);
        }
        updateForegroundCall();
        this.mCalls.remove(call);
        if (this.mCalls.size() == 0) {
            this.mBluetoothStateReceiver.setIsInCall(false);
        }
        onCallLeavingState(call, call.getState());
    }

    @Override
    public void onExternalCallChanged(Call call, boolean z) {
        if (z) {
            Log.d(this.LOG_TAG, "Removing call which became external ID %s", new Object[]{call.getId()});
            removeCall(call);
            return;
        }
        if (!z) {
            Log.d(this.LOG_TAG, "Adding external call which was pulled with ID %s", new Object[]{call.getId()});
            addCall(call);
            if (this.mCallsManager.isSpeakerphoneAutoEnabledForVideoCalls(call.getVideoState())) {
                Log.d(this.LOG_TAG, "Switching to speaker because external video call %s was pulled." + call.getId(), new Object[0]);
                this.mCallAudioRouteStateMachine.sendMessageWithSessionInfo(1004);
            }
        }
    }

    private boolean shouldIgnoreCallForAudio(Call call) {
        return call.getParentCall() != null || call.isExternalCall();
    }

    @Override
    public void onIncomingCallAnswered(Call call) {
        if (!this.mCalls.contains(call)) {
            return;
        }
        if (call.can(262144) && this.mForegroundCall == call) {
            Log.i(this.LOG_TAG, "Invoking the MT_AUDIO_SPEEDUP mechanism. Transitioning into an active in-call audio state before connection service has connected the call.", new Object[0]);
            if (this.mCallStateToCalls.get(call.getState()) != null) {
                this.mCallStateToCalls.get(call.getState()).remove(call);
            }
            this.mActiveDialingOrConnectingCalls.add(call);
            this.mCallAudioModeStateMachine.sendMessageWithArgs(2004, makeArgsForModeStateMachine());
        }
        if (!call.isHandoverInProgress()) {
            mute(false);
        }
        maybeStopRingingAndCallWaitingForAnsweredOrRejectedCall(call);
    }

    @Override
    public void onSessionModifyRequestReceived(Call call, VideoProfile videoProfile) {
        if (videoProfile == null || call != this.mForegroundCall) {
            return;
        }
        int videoState = call.getVideoState();
        int videoState2 = videoProfile.getVideoState();
        boolean z = false;
        Log.v(this, "onSessionModifyRequestReceived : videoProfile = " + VideoProfile.videoStateToString(videoState2), new Object[0]);
        if (!VideoProfile.isReceptionEnabled(videoState) && VideoProfile.isReceptionEnabled(videoState2)) {
            z = true;
        }
        if (z) {
            this.mPlayerFactory.createPlayer(14).startTone();
        }
    }

    @Override
    public void onHoldToneRequested(Call call) {
        maybePlayHoldTone();
    }

    @Override
    public void onIsVoipAudioModeChanged(Call call) {
        if (call != this.mForegroundCall) {
            return;
        }
        this.mCallAudioModeStateMachine.sendMessageWithArgs(4001, makeArgsForModeStateMachine());
    }

    @Override
    public void onRingbackRequested(Call call, boolean z) {
        if (call == this.mForegroundCall && z) {
            this.mRingbackPlayer.startRingbackForCall(call);
        } else {
            this.mRingbackPlayer.stopRingbackForCall(call);
        }
    }

    @Override
    public void onIncomingCallRejected(Call call, boolean z, String str) {
        maybeStopRingingAndCallWaitingForAnsweredOrRejectedCall(call);
    }

    @Override
    public void onIsConferencedChanged(Call call) {
        if (call.getParentCall() == null) {
            Log.i(this.LOG_TAG, "Call TC@" + call.getId() + " left conference and will now be tracked by CallAudioManager.", new Object[0]);
            onCallAdded(call);
            return;
        }
        if (this.mCallStateToCalls.get(call.getState()) != null) {
            this.mCallStateToCalls.get(call.getState()).remove(call);
        }
        updateForegroundCall();
        this.mCalls.remove(call);
    }

    @Override
    public void onConnectionServiceChanged(Call call, ConnectionServiceWrapper connectionServiceWrapper, ConnectionServiceWrapper connectionServiceWrapper2) {
        this.mCallAudioRouteStateMachine.sendMessageWithSessionInfo(1201);
    }

    @Override
    public void onVideoStateChanged(Call call, int i, int i2) {
        if (call != getForegroundCall()) {
            Log.d(this.LOG_TAG, "Ignoring video state change from %s to %s for call %s -- not foreground.", new Object[]{VideoProfile.videoStateToString(i), VideoProfile.videoStateToString(i2), call.getId()});
        } else if (!VideoProfile.isVideo(i) && call.isVideoCallExcludeVideoRingtone() && this.mCallsManager.isSpeakerphoneAutoEnabledForVideoCalls(i2)) {
            Log.d(this.LOG_TAG, "Switching to speaker because call %s transitioned video state from %s to %s", new Object[]{call.getId(), VideoProfile.videoStateToString(i), VideoProfile.videoStateToString(i2)});
            this.mCallAudioRouteStateMachine.sendMessageWithSessionInfo(1004);
        }
    }

    public CallAudioState getCallAudioState() {
        return this.mCallAudioRouteStateMachine.getCurrentCallAudioState();
    }

    public Call getPossiblyHeldForegroundCall() {
        return this.mForegroundCall;
    }

    public Call getForegroundCall() {
        if (this.mForegroundCall != null && this.mForegroundCall.getState() != 6) {
            return this.mForegroundCall;
        }
        return null;
    }

    @VisibleForTesting
    public void toggleMute() {
        if (this.mCallsManager.hasEmergencyCall()) {
            Log.v(this, "ignoring toggleMute for emergency call", new Object[0]);
        } else {
            this.mCallAudioRouteStateMachine.sendMessageWithSessionInfo(3003);
        }
    }

    @VisibleForTesting(visibility = VisibleForTesting.Visibility.PACKAGE)
    public void onRingerModeChange() {
        this.mCallAudioModeStateMachine.sendMessageWithArgs(5001, makeArgsForModeStateMachine());
    }

    @VisibleForTesting
    public void mute(boolean z) {
        Log.v(this, "mute, shouldMute: %b", new Object[]{Boolean.valueOf(z)});
        if (this.mCallsManager.hasEmergencyCall()) {
            Log.v(this, "ignoring mute for emergency call", new Object[0]);
            z = false;
        }
        this.mCallAudioRouteStateMachine.sendMessageWithSessionInfo(z ? 3001 : 3002);
    }

    void setAudioRoute(int i, String str) {
        Log.v(this, "setAudioRoute, route: %s", new Object[]{CallAudioState.audioRouteToString(i)});
        switch (i) {
            case 1:
                this.mCallAudioRouteStateMachine.sendMessageWithSessionInfo(1101);
                break;
            case CallState.SELECT_PHONE_ACCOUNT:
                this.mCallAudioRouteStateMachine.sendMessageWithSessionInfo(1102, 0, str);
                break;
            case CallState.DIALING:
            case CallState.ON_HOLD:
            case CallState.DISCONNECTED:
            default:
                Log.wtf(this, "Invalid route specified: %d", new Object[]{Integer.valueOf(i)});
                break;
            case CallState.RINGING:
                this.mCallAudioRouteStateMachine.sendMessageWithSessionInfo(1103);
                break;
            case CallState.ACTIVE:
                this.mCallAudioRouteStateMachine.sendMessageWithSessionInfo(1105, 0);
                break;
            case CallState.ABORTED:
                this.mCallAudioRouteStateMachine.sendMessageWithSessionInfo(1104);
                break;
        }
    }

    void switchBaseline() {
        Log.i(this, "switchBaseline", new Object[0]);
        this.mCallAudioRouteStateMachine.sendMessageWithSessionInfo(1105, 1);
    }

    void silenceRingers() {
        Iterator<Call> it = this.mRingingCalls.iterator();
        while (it.hasNext()) {
            it.next().silence();
        }
        this.mRinger.stopRinging();
        this.mRinger.stopCallWaiting();
    }

    @VisibleForTesting
    public boolean startRinging() {
        return this.mRinger.startRinging(this.mForegroundCall, this.mCallAudioRouteStateMachine.isHfpDeviceAvailable());
    }

    @VisibleForTesting
    public void startCallWaiting() {
        if (this.mRingingCalls.size() == 1) {
            this.mRinger.startCallWaiting(this.mRingingCalls.iterator().next());
        }
    }

    @VisibleForTesting
    public void stopRinging() {
        this.mRinger.stopRinging();
    }

    @VisibleForTesting
    public void stopCallWaiting() {
        this.mRinger.stopCallWaiting();
    }

    @VisibleForTesting
    public void setCallAudioRouteFocusState(int i) {
        this.mCallAudioRouteStateMachine.sendMessageWithSessionInfo(4001, i);
    }

    @VisibleForTesting
    public CallAudioRouteStateMachine getCallAudioRouteStateMachine() {
        return this.mCallAudioRouteStateMachine;
    }

    @VisibleForTesting
    public CallAudioModeStateMachine getCallAudioModeStateMachine() {
        return this.mCallAudioModeStateMachine;
    }

    void dump(IndentingPrintWriter indentingPrintWriter) {
        indentingPrintWriter.println("All calls:");
        indentingPrintWriter.increaseIndent();
        dumpCallsInCollection(indentingPrintWriter, this.mCalls);
        indentingPrintWriter.decreaseIndent();
        indentingPrintWriter.println("Active dialing, or connecting calls:");
        indentingPrintWriter.increaseIndent();
        dumpCallsInCollection(indentingPrintWriter, this.mActiveDialingOrConnectingCalls);
        indentingPrintWriter.decreaseIndent();
        indentingPrintWriter.println("Ringing calls:");
        indentingPrintWriter.increaseIndent();
        dumpCallsInCollection(indentingPrintWriter, this.mRingingCalls);
        indentingPrintWriter.decreaseIndent();
        indentingPrintWriter.println("Holding calls:");
        indentingPrintWriter.increaseIndent();
        dumpCallsInCollection(indentingPrintWriter, this.mHoldingCalls);
        indentingPrintWriter.decreaseIndent();
        indentingPrintWriter.println("Foreground call:");
        indentingPrintWriter.println(this.mForegroundCall);
        indentingPrintWriter.println("CallAudioModeStateMachine pending messages:");
        indentingPrintWriter.increaseIndent();
        this.mCallAudioModeStateMachine.dumpPendingMessages(indentingPrintWriter);
        indentingPrintWriter.decreaseIndent();
        indentingPrintWriter.println("CallAudioRouteStateMachine pending messages:");
        indentingPrintWriter.increaseIndent();
        this.mCallAudioRouteStateMachine.dumpPendingMessages(indentingPrintWriter);
        indentingPrintWriter.decreaseIndent();
    }

    @VisibleForTesting
    public void setIsTonePlaying(boolean z) {
        this.mIsTonePlaying = z;
        this.mCallAudioModeStateMachine.sendMessageWithArgs(z ? 3001 : 3002, makeArgsForModeStateMachine());
        if (!z && this.mIsDisconnectedTonePlaying) {
            this.mCallsManager.onDisconnectedTonePlaying(false);
            this.mIsDisconnectedTonePlaying = false;
        }
    }

    private void onCallLeavingState(Call call, int i) {
        if (i != 1) {
            if (i == 10) {
                onCallLeavingActiveDialingOrConnecting();
                return;
            }
            switch (i) {
                case CallState.DIALING:
                    stopRingbackForCall(call);
                    onCallLeavingActiveDialingOrConnecting();
                    break;
                case CallState.RINGING:
                    onCallLeavingRinging();
                    break;
                case CallState.ON_HOLD:
                    onCallLeavingHold();
                    break;
            }
        }
        onCallLeavingActiveDialingOrConnecting();
    }

    private void onCallEnteringState(Call call, int i) {
        if (i != 1) {
            if (i == 10) {
                onCallEnteringActiveDialingOrConnecting();
                return;
            }
            switch (i) {
                case CallState.DIALING:
                    onCallEnteringActiveDialingOrConnecting();
                    playRingbackForCall(call);
                    break;
                case CallState.RINGING:
                    onCallEnteringRinging();
                    break;
                case CallState.ON_HOLD:
                    onCallEnteringHold();
                    break;
            }
        }
        onCallEnteringActiveDialingOrConnecting();
    }

    private void onCallLeavingActiveDialingOrConnecting() {
        if (this.mActiveDialingOrConnectingCalls.size() == 0) {
            this.mCallAudioModeStateMachine.sendMessageWithArgs(1001, makeArgsForModeStateMachine());
        }
        if (this.mActiveDialingOrConnectingCalls.size() == 1 && MtkUtil.isInDsdaMode()) {
            Log.d(this, "2A -> 1A case !", new Object[0]);
            this.mCallAudioModeStateMachine.sendMessageWithArgs(4501, makeArgsForModeStateMachine());
        }
    }

    private void onCallLeavingRinging() {
        if (this.mRingingCalls.size() == 0) {
            this.mCallAudioModeStateMachine.sendMessageWithArgs(1002, makeArgsForModeStateMachine());
        }
    }

    private void onCallLeavingHold() {
        if (this.mHoldingCalls.size() == 0) {
            this.mCallAudioModeStateMachine.sendMessageWithArgs(1003, makeArgsForModeStateMachine());
        }
    }

    private void onCallEnteringActiveDialingOrConnecting() {
        if (this.mActiveDialingOrConnectingCalls.size() == 1) {
            Call next = this.mActiveDialingOrConnectingCalls.iterator().next();
            if (next != null && next.isEmergencyCall() && next.getState() == 1) {
                return;
            }
            this.mCallAudioModeStateMachine.sendMessageWithArgs(2001, makeArgsForModeStateMachine());
        }
    }

    private void onCallEnteringRinging() {
        if (this.mRingingCalls.size() == 1) {
            this.mCallAudioModeStateMachine.sendMessageWithArgs(2002, makeArgsForModeStateMachine());
        }
    }

    private void onCallEnteringHold() {
        if (this.mHoldingCalls.size() == 1) {
            this.mCallAudioModeStateMachine.sendMessageWithArgs(2003, makeArgsForModeStateMachine());
        }
    }

    private void updateForegroundCall() {
        Call call = this.mForegroundCall;
        Call next = null;
        if (this.mActiveDialingOrConnectingCalls.size() > 0) {
            for (Call call2 : this.mActiveDialingOrConnectingCalls) {
                if (call2.getState() == 1) {
                    next = call2;
                }
            }
            if (next == null) {
                next = this.mActiveDialingOrConnectingCalls.iterator().next();
            }
            this.mForegroundCall = next;
        } else if (this.mRingingCalls.size() > 0) {
            this.mForegroundCall = this.mRingingCalls.iterator().next();
        } else if (this.mHoldingCalls.size() > 0) {
            this.mForegroundCall = this.mHoldingCalls.iterator().next();
        } else {
            this.mForegroundCall = null;
        }
        if (this.mForegroundCall != call) {
            this.mCallAudioRouteStateMachine.sendMessageWithSessionInfo(1201);
            this.mDtmfLocalTonePlayer.onForegroundCallChanged(call, this.mForegroundCall);
            maybePlayHoldTone();
            this.mCallsManager.onForegroundCallChanged(call, this.mForegroundCall);
        }
    }

    private CallAudioModeStateMachine.MessageArgs makeArgsForModeStateMachine() {
        return new CallAudioModeStateMachine.MessageArgs(this.mActiveDialingOrConnectingCalls.size() > 0, this.mRingingCalls.size() > 0, this.mHoldingCalls.size() > 0, this.mIsTonePlaying, this.mForegroundCall != null && this.mForegroundCall.getIsVoipAudioMode(), Log.createSubsession());
    }

    private void playToneForDisconnectedCall(Call call) {
        int i;
        if (call.isHandoverInProgress()) {
            Log.i(this.LOG_TAG, "Omitting tone because %s is being handed over.", new Object[]{call});
            return;
        }
        if (this.mForegroundCall != null && call != this.mForegroundCall && this.mCalls.size() > 1) {
            Log.v(this.LOG_TAG, "Omitting tone because we are not foreground and there is another call.", new Object[0]);
            return;
        }
        if (call.getDisconnectCause() != null) {
            Log.v(this, "Disconnect cause: %s.", new Object[]{call.getDisconnectCause()});
            switch (call.getDisconnectCause().getTone()) {
                case 17:
                    i = 1;
                    break;
                case 18:
                    i = 6;
                    break;
                case 21:
                    i = 12;
                    break;
                case 27:
                    i = 2;
                    break;
                case 37:
                    i = 7;
                    break;
                case 38:
                    i = 10;
                    break;
                case 95:
                    i = 5;
                    break;
                default:
                    i = 0;
                    break;
            }
            Log.d(this, "Found a disconnected call with tone to play %d.", new Object[]{Integer.valueOf(i)});
            if (i != 0) {
                this.mPlayerFactory.createPlayer(i).startTone();
                this.mCallsManager.onDisconnectedTonePlaying(true);
                this.mIsDisconnectedTonePlaying = true;
            }
        }
    }

    private void playRingbackForCall(Call call) {
        if (call == this.mForegroundCall && call.isRingbackRequested()) {
            this.mRingbackPlayer.startRingbackForCall(call);
        }
    }

    private void stopRingbackForCall(Call call) {
        this.mRingbackPlayer.stopRingbackForCall(call);
    }

    private void maybePlayHoldTone() {
        if (shouldPlayHoldTone()) {
            if (this.mHoldTonePlayer == null) {
                this.mHoldTonePlayer = this.mPlayerFactory.createPlayer(4);
                this.mHoldTonePlayer.startTone();
                return;
            }
            return;
        }
        if (this.mHoldTonePlayer != null) {
            this.mHoldTonePlayer.stopTone();
            this.mHoldTonePlayer = null;
        }
    }

    private boolean shouldPlayHoldTone() {
        Call foregroundCall = getForegroundCall();
        if (foregroundCall == null || this.mCallsManager.hasRingingCall() || !foregroundCall.isActive()) {
            return false;
        }
        return foregroundCall.isRemotelyHeld();
    }

    private void dumpCallsInCollection(IndentingPrintWriter indentingPrintWriter, Collection<Call> collection) {
        for (Call call : collection) {
            if (call != null) {
                indentingPrintWriter.println(call.getId());
            }
        }
    }

    private void maybeStopRingingAndCallWaitingForAnsweredOrRejectedCall(Call call) {
        if (this.mRingingCalls.size() == 0 || (this.mRingingCalls.size() == 1 && call == this.mRingingCalls.iterator().next())) {
            this.mRinger.stopRinging();
            this.mRinger.stopCallWaiting();
        }
    }

    private boolean shouldPlayDisconnectTone(int i, int i2) {
        if (i2 != 7) {
            return false;
        }
        return i == 5 || i == 3 || i == 6;
    }

    @VisibleForTesting
    public Set<Call> getTrackedCalls() {
        return this.mCalls;
    }

    @VisibleForTesting
    public SparseArray<LinkedHashSet<Call>> getCallStateToCalls() {
        return this.mCallStateToCalls;
    }

    void resetAudioMode() {
        this.mCallAudioModeStateMachine.sendMessageWithArgs(10001, makeArgsForModeStateMachine());
    }

    public void restoreMuteOnWhenInCallMode() {
        this.mCallAudioRouteStateMachine.restoreMuteOnWhenInCallMode();
    }
}
