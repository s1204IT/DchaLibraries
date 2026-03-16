package com.android.server.telecom;

import android.app.NotificationManager;
import android.content.Context;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.SystemVibrator;
import android.os.Vibrator;
import android.provider.Settings;
import com.android.server.telecom.InCallTonePlayer;
import java.util.LinkedList;
import java.util.List;

final class Ringer extends CallsManagerListenerBase {
    private final CallAudioManager mCallAudioManager;
    private InCallTonePlayer mCallWaitingPlayer;
    private final CallsManager mCallsManager;
    private final Context mContext;
    private final InCallTonePlayer.Factory mPlayerFactory;
    private final AsyncRingtonePlayer mRingtonePlayer;
    private final Vibrator mVibrator;
    private static final long[] VIBRATION_PATTERN = {0, 1000, 1000};
    private static final AudioAttributes VIBRATION_ATTRIBUTES = new AudioAttributes.Builder().setContentType(4).setUsage(6).build();
    private final List<Call> mRingingCalls = new LinkedList();
    private boolean mIsVibrating = false;

    Ringer(CallAudioManager callAudioManager, CallsManager callsManager, InCallTonePlayer.Factory factory, Context context) {
        this.mCallAudioManager = callAudioManager;
        this.mCallsManager = callsManager;
        this.mPlayerFactory = factory;
        this.mContext = context;
        this.mVibrator = new SystemVibrator(context);
        this.mRingtonePlayer = new AsyncRingtonePlayer(context);
    }

    @Override
    public void onCallAdded(Call call) {
        if (call.isIncoming() && call.getState() == 4) {
            if (this.mRingingCalls.contains(call)) {
                Log.wtf(this, "New ringing call is already in list of unanswered calls", new Object[0]);
            }
            this.mRingingCalls.add(call);
            updateRinging();
        }
    }

    @Override
    public void onCallRemoved(Call call) {
        removeFromUnansweredCall(call);
    }

    @Override
    public void onCallStateChanged(Call call, int i, int i2) {
        if (i2 != 4) {
            removeFromUnansweredCall(call);
        }
    }

    @Override
    public void onIncomingCallAnswered(Call call) {
        onRespondedToIncomingCall(call);
    }

    @Override
    public void onIncomingCallRejected(Call call, boolean z, String str) {
        onRespondedToIncomingCall(call);
    }

    @Override
    public void onForegroundCallChanged(Call call, Call call2) {
        if (this.mRingingCalls.contains(call) || this.mRingingCalls.contains(call2)) {
            updateRinging();
        }
    }

    void silence() {
        this.mRingingCalls.clear();
        updateRinging();
    }

    private void onRespondedToIncomingCall(Call call) {
        if (getTopMostUnansweredCall() == call) {
            removeFromUnansweredCall(call);
        }
    }

    private Call getTopMostUnansweredCall() {
        if (this.mRingingCalls.isEmpty()) {
            return null;
        }
        return this.mRingingCalls.get(0);
    }

    private void removeFromUnansweredCall(Call call) {
        this.mRingingCalls.remove(call);
        updateRinging();
    }

    private void updateRinging() {
        if (this.mRingingCalls.isEmpty()) {
            stopRinging();
            stopCallWaiting();
        } else {
            startRingingOrCallWaiting();
        }
    }

    private void startRingingOrCallWaiting() {
        Call foregroundCall = this.mCallsManager.getForegroundCall();
        Log.v(this, "startRingingOrCallWaiting, foregroundCall: %s.", foregroundCall);
        if (this.mRingingCalls.contains(foregroundCall)) {
            stopCallWaiting();
            if (shouldRingForContact(foregroundCall.getContactUri())) {
                if (((AudioManager) this.mContext.getSystemService("audio")).getStreamVolume(2) > 0) {
                    Log.v(this, "startRingingOrCallWaiting", new Object[0]);
                    this.mCallAudioManager.setIsRinging(true);
                    this.mRingtonePlayer.play(foregroundCall.getRingtone());
                } else {
                    Log.v(this, "startRingingOrCallWaiting, skipping because volume is 0", new Object[0]);
                }
                if (shouldVibrate(this.mContext) && !this.mIsVibrating) {
                    this.mVibrator.vibrate(VIBRATION_PATTERN, 1, VIBRATION_ATTRIBUTES);
                    this.mIsVibrating = true;
                    return;
                }
                return;
            }
            return;
        }
        if (foregroundCall != null) {
            Log.v(this, "Playing call-waiting tone.", new Object[0]);
            stopRinging();
            if (this.mCallWaitingPlayer == null) {
                this.mCallWaitingPlayer = this.mPlayerFactory.createPlayer(4);
                this.mCallWaitingPlayer.startTone();
            }
        }
    }

    private boolean shouldRingForContact(Uri uri) {
        NotificationManager notificationManager = (NotificationManager) this.mContext.getSystemService("notification");
        Bundle bundle = new Bundle();
        if (uri != null) {
            bundle.putStringArray("android.people", new String[]{uri.toString()});
        }
        return notificationManager.matchesCallFilter(bundle);
    }

    private void stopRinging() {
        Log.v(this, "stopRinging", new Object[0]);
        this.mRingtonePlayer.stop();
        if (this.mIsVibrating) {
            this.mVibrator.cancel();
            this.mIsVibrating = false;
        }
        this.mCallAudioManager.setIsRinging(false);
    }

    private void stopCallWaiting() {
        Log.v(this, "stop call waiting.", new Object[0]);
        if (this.mCallWaitingPlayer != null) {
            this.mCallWaitingPlayer.stopTone();
            this.mCallWaitingPlayer = null;
        }
    }

    private boolean shouldVibrate(Context context) {
        int ringerModeInternal = ((AudioManager) context.getSystemService("audio")).getRingerModeInternal();
        if (getVibrateWhenRinging(context)) {
            return ringerModeInternal != 0;
        }
        return ringerModeInternal == 1;
    }

    private boolean getVibrateWhenRinging(Context context) {
        return this.mVibrator.hasVibrator() && Settings.System.getInt(context.getContentResolver(), "vibrate_when_ringing", 0) != 0;
    }
}
