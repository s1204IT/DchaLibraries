package com.android.server.telecom;

import android.app.NotificationManager;
import android.app.Person;
import android.content.Context;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.Ringtone;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcelable;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.telecom.Log;
import com.android.internal.annotations.VisibleForTesting;
import com.android.server.telecom.InCallTonePlayer;
import java.util.ArrayList;

@VisibleForTesting
public class Ringer {
    private Call mCallWaitingCall;
    private InCallTonePlayer mCallWaitingPlayer;
    private final Context mContext;

    @VisibleForTesting
    public VibrationEffect mDefaultVibrationEffect;
    private final InCallController mInCallController;
    private boolean mIsVibrating = false;
    private final InCallTonePlayer.Factory mPlayerFactory;
    private Call mRingingCall;
    private RingtoneFactory mRingtoneFactory;
    private final AsyncRingtonePlayer mRingtonePlayer;
    private final SystemSettingsUtil mSystemSettingsUtil;
    private Call mVibratingCall;
    private final Vibrator mVibrator;
    private static final long[] PULSE_PATTERN = {0, 12, 250, 12, 500, 50, 50, 50, 50, 50, 50, 50, 50, 50, 50, 50, 50, 50, 50, 300, 1000};
    private static final int[] PULSE_AMPLITUDE = {0, 255, 0, 255, 0, 77, 77, 78, 79, 81, 84, 87, 93, 101, 114, 133, 162, 205, 255, 255, 0};
    private static final long[] SIMPLE_VIBRATION_PATTERN = {0, 1000, 1000};
    private static final int[] SIMPLE_VIBRATION_AMPLITUDE = {0, 255, 0};
    private static final AudioAttributes VIBRATION_ATTRIBUTES = new AudioAttributes.Builder().setContentType(4).setUsage(6).build();

    @VisibleForTesting
    public Ringer(InCallTonePlayer.Factory factory, Context context, SystemSettingsUtil systemSettingsUtil, AsyncRingtonePlayer asyncRingtonePlayer, RingtoneFactory ringtoneFactory, Vibrator vibrator, InCallController inCallController) {
        this.mSystemSettingsUtil = systemSettingsUtil;
        this.mPlayerFactory = factory;
        this.mContext = context;
        this.mVibrator = vibrator;
        this.mRingtonePlayer = asyncRingtonePlayer;
        this.mRingtoneFactory = ringtoneFactory;
        this.mInCallController = inCallController;
        if (this.mContext.getResources().getBoolean(R.bool.use_simple_vibration_pattern)) {
            this.mDefaultVibrationEffect = VibrationEffect.createWaveform(SIMPLE_VIBRATION_PATTERN, SIMPLE_VIBRATION_AMPLITUDE, 1);
        } else {
            this.mDefaultVibrationEffect = VibrationEffect.createWaveform(PULSE_PATTERN, PULSE_AMPLITUDE, 5);
        }
    }

    public boolean startRinging(Call call, boolean z) {
        VibrationEffect vibrationEffectForCall;
        if (call == null) {
            Log.i(this, "startRinging called with null foreground call.", new Object[0]);
            return false;
        }
        boolean z2 = ((AudioManager) this.mContext.getSystemService("audio")).getStreamVolume(2) > 0;
        boolean zShouldRingForContact = shouldRingForContact(call.getContactUri());
        boolean z3 = this.mRingtoneFactory.getRingtone(call) != null;
        boolean zIsSelfManaged = call.isSelfManaged();
        boolean z4 = z2 && zShouldRingForContact && z3;
        boolean zHasExternalRinger = hasExternalRinger(call);
        boolean z5 = z4 || (z && zShouldRingForContact) || zIsSelfManaged;
        boolean zIsTheaterModeOn = this.mSystemSettingsUtil.isTheaterModeOn(this.mContext);
        boolean zDoesConnectedDialerSupportRinging = this.mInCallController.doesConnectedDialerSupportRinging();
        if (zIsTheaterModeOn || zDoesConnectedDialerSupportRinging || zIsSelfManaged || zHasExternalRinger) {
            if (zDoesConnectedDialerSupportRinging) {
                Log.addEvent(call, "SKIP_RINGING");
            }
            Log.i(this, "Ending early -- isTheaterModeOn=%s, letDialerHandleRinging=%s, isSelfManaged=%s, hasExternalRinger=%s", new Object[]{Boolean.valueOf(zIsTheaterModeOn), Boolean.valueOf(zDoesConnectedDialerSupportRinging), Boolean.valueOf(zIsSelfManaged), Boolean.valueOf(zHasExternalRinger)});
            return z5;
        }
        stopCallWaiting();
        if (z4) {
            this.mRingingCall = call;
            Log.addEvent(call, "START_RINGER");
            this.mRingtonePlayer.play(this.mRingtoneFactory, call);
            vibrationEffectForCall = getVibrationEffectForCall(this.mRingtoneFactory, call);
        } else {
            Log.i(this, "startRinging: skipping because ringer would not be audible. isVolumeOverZero=%s, shouldRingForContact=%s, isRingtonePresent=%s", new Object[]{Boolean.valueOf(z2), Boolean.valueOf(zShouldRingForContact), Boolean.valueOf(z3)});
            vibrationEffectForCall = this.mDefaultVibrationEffect;
        }
        if (shouldVibrate(this.mContext, call) && !this.mIsVibrating && zShouldRingForContact) {
            this.mVibrator.vibrate(vibrationEffectForCall, VIBRATION_ATTRIBUTES);
            this.mIsVibrating = true;
        } else if (this.mIsVibrating) {
            Log.addEvent(call, "SKIP_VIBRATION", "already vibrating");
        }
        return z5;
    }

    private VibrationEffect getVibrationEffectForCall(RingtoneFactory ringtoneFactory, Call call) {
        Ringtone ringtone = ringtoneFactory.getRingtone(call);
        VibrationEffect vibrationEffect = null;
        Uri uri = ringtone != null ? ringtone.getUri() : null;
        if (uri != null) {
            vibrationEffect = VibrationEffect.get(uri, this.mContext);
        }
        if (vibrationEffect == null) {
            return this.mDefaultVibrationEffect;
        }
        return vibrationEffect;
    }

    public void startCallWaiting(Call call) {
        if (this.mSystemSettingsUtil.isTheaterModeOn(this.mContext)) {
            return;
        }
        if (this.mInCallController.doesConnectedDialerSupportRinging()) {
            Log.addEvent(call, "SKIP_RINGING");
            return;
        }
        if (call.isSelfManaged()) {
            Log.addEvent(call, "SKIP_RINGING", "Self-managed");
            return;
        }
        Log.v(this, "Playing call-waiting tone.", new Object[0]);
        stopRinging();
        if (this.mCallWaitingPlayer == null) {
            Log.addEvent(call, "START_CALL_WAITING_TONE");
            this.mCallWaitingCall = call;
            this.mCallWaitingPlayer = this.mPlayerFactory.createPlayer(4);
            this.mCallWaitingPlayer.startTone();
        }
    }

    public void stopRinging() {
        if (this.mRingingCall != null) {
            Log.addEvent(this.mRingingCall, "STOP_RINGER");
            this.mRingingCall = null;
        }
        this.mRingtonePlayer.stop();
        if (this.mIsVibrating) {
            Log.addEvent(this.mVibratingCall, "STOP_VIBRATOR");
            this.mVibrator.cancel();
            this.mIsVibrating = false;
            this.mVibratingCall = null;
        }
    }

    public synchronized void stopCallWaiting() {
        Log.v(this, "stop call waiting.", new Object[0]);
        if (this.mCallWaitingPlayer != null) {
            if (this.mCallWaitingCall != null) {
                Log.addEvent(this.mCallWaitingCall, "STOP_CALL_WAITING_TONE");
                this.mCallWaitingCall = null;
            }
            this.mCallWaitingPlayer.stopTone();
            this.mCallWaitingPlayer = null;
        }
    }

    private boolean shouldRingForContact(Uri uri) {
        NotificationManager notificationManager = (NotificationManager) this.mContext.getSystemService("notification");
        Bundle bundle = new Bundle();
        if (uri != null) {
            ArrayList<? extends Parcelable> arrayList = new ArrayList<>();
            arrayList.add(new Person.Builder().setUri(uri.toString()).build());
            bundle.putParcelableArrayList("android.people.list", arrayList);
        }
        return notificationManager.matchesCallFilter(bundle);
    }

    private boolean hasExternalRinger(Call call) {
        Bundle intentExtras = call.getIntentExtras();
        if (intentExtras != null) {
            return intentExtras.getBoolean("android.telecom.extra.CALL_EXTERNAL_RINGER", false);
        }
        return false;
    }

    private boolean shouldVibrate(Context context, Call call) {
        int ringerModeInternal = ((AudioManager) context.getSystemService("audio")).getRingerModeInternal();
        boolean z = !getVibrateWhenRinging(context) ? ringerModeInternal != 1 : ringerModeInternal == 0;
        if (z) {
            Log.addEvent(call, "START_VIBRATOR", "hasVibrator=%b, userRequestsVibrate=%b, ringerMode=%d, isVibrating=%b", new Object[]{Boolean.valueOf(this.mVibrator.hasVibrator()), Boolean.valueOf(this.mSystemSettingsUtil.canVibrateWhenRinging(context)), Integer.valueOf(ringerModeInternal), Boolean.valueOf(this.mIsVibrating)});
        } else {
            Log.addEvent(call, "SKIP_VIBRATION", "hasVibrator=%b, userRequestsVibrate=%b, ringerMode=%d, isVibrating=%b", new Object[]{Boolean.valueOf(this.mVibrator.hasVibrator()), Boolean.valueOf(this.mSystemSettingsUtil.canVibrateWhenRinging(context)), Integer.valueOf(ringerModeInternal), Boolean.valueOf(this.mIsVibrating)});
        }
        return z;
    }

    private boolean getVibrateWhenRinging(Context context) {
        if (!this.mVibrator.hasVibrator()) {
            return false;
        }
        return this.mSystemSettingsUtil.canVibrateWhenRinging(context);
    }
}
