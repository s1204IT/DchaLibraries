package com.android.server.telecom;

import android.content.Context;
import android.media.AudioDeviceInfo;
import android.media.AudioManager;
import android.media.AudioRecordingConfiguration;
import android.media.MediaPlayer;
import android.os.Handler;
import android.os.Looper;
import android.telecom.Log;
import com.android.internal.annotations.VisibleForTesting;
import com.android.server.telecom.TelecomSystem;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class CallRecordingTonePlayer extends CallsManagerListenerBase {
    private final AudioManager mAudioManager;
    private final Context mContext;
    private final TelecomSystem.SyncRoot mLock;
    private AudioManager.AudioRecordingCallback mAudioRecordingCallback = new AudioManager.AudioRecordingCallback() {
        @Override
        public void onRecordingConfigChanged(List<AudioRecordingConfiguration> list) {
            synchronized (CallRecordingTonePlayer.this.mLock) {
                try {
                    Log.startSession("CRTP.oRCC");
                    CallRecordingTonePlayer.this.handleRecordingConfigurationChange(list);
                    CallRecordingTonePlayer.this.maybeStartCallAudioTone();
                    CallRecordingTonePlayer.this.maybeStopCallAudioTone();
                } finally {
                    Log.endSession();
                }
            }
        }
    };
    private final Handler mMainThreadHandler = new Handler(Looper.getMainLooper());
    private boolean mIsRecording = false;
    private MediaPlayer mRecordingTonePlayer = null;
    private List<Call> mCalls = new ArrayList();

    public CallRecordingTonePlayer(Context context, AudioManager audioManager, TelecomSystem.SyncRoot syncRoot) {
        this.mContext = context;
        this.mAudioManager = audioManager;
        this.mLock = syncRoot;
    }

    @Override
    public void onCallAdded(Call call) {
        if (!shouldUseRecordingTone(call)) {
            return;
        }
        addCall(call);
    }

    @Override
    public void onCallRemoved(Call call) {
        if (!shouldUseRecordingTone(call)) {
            return;
        }
        removeCall(call);
    }

    @Override
    public void onCallStateChanged(Call call, int i, int i2) {
        if (shouldUseRecordingTone(call) && this.mIsRecording) {
            maybeStartCallAudioTone();
            maybeStopCallAudioTone();
        }
    }

    private void addCall(Call call) {
        boolean zIsEmpty = this.mCalls.isEmpty();
        this.mCalls.add(call);
        if (zIsEmpty) {
            handleRecordingConfigurationChange(this.mAudioManager.getActiveRecordingConfigurations());
            this.mAudioManager.registerAudioRecordingCallback(this.mAudioRecordingCallback, this.mMainThreadHandler);
        }
        maybeStartCallAudioTone();
    }

    private void removeCall(Call call) {
        this.mCalls.remove(call);
        if (this.mCalls.isEmpty()) {
            this.mAudioManager.unregisterAudioRecordingCallback(this.mAudioRecordingCallback);
            maybeStopCallAudioTone();
        }
    }

    private boolean shouldUseRecordingTone(Call call) {
        return call.getParentCall() == null && !call.isExternalCall() && !call.isEmergencyCall() && call.isUsingCallRecordingTone();
    }

    private void maybeStartCallAudioTone() {
        if (this.mIsRecording && hasActiveCall()) {
            startCallRecordingTone(this.mContext);
        }
    }

    private void maybeStopCallAudioTone() {
        if (!this.mIsRecording || !hasActiveCall()) {
            stopCallRecordingTone();
        }
    }

    private boolean hasActiveCall() {
        return !this.mCalls.isEmpty() && this.mCalls.stream().filter(new Predicate() {
            @Override
            public final boolean test(Object obj) {
                return ((Call) obj).isActive();
            }
        }).count() > 0;
    }

    private void handleRecordingConfigurationChange(List<AudioRecordingConfiguration> list) {
        if (list == null) {
            list = Collections.emptyList();
        }
        boolean z = this.mIsRecording;
        boolean zIsRecordingInProgress = isRecordingInProgress(list);
        if (z != zIsRecordingInProgress) {
            this.mIsRecording = zIsRecordingInProgress;
            if (zIsRecordingInProgress) {
                Log.i(this, "handleRecordingConfigurationChange: recording started", new Object[0]);
            } else {
                Log.i(this, "handleRecordingConfigurationChange: recording stopped", new Object[0]);
            }
        }
    }

    private boolean isRecordingInProgress(List<AudioRecordingConfiguration> list) {
        Log.i(this, "isRecordingInProgress: recordingPackages=%s", new Object[]{(String) list.stream().map(new Function() {
            @Override
            public final Object apply(Object obj) {
                return ((AudioRecordingConfiguration) obj).getClientPackageName();
            }
        }).collect(Collectors.joining(", "))});
        return list.stream().filter(new Predicate() {
            @Override
            public final boolean test(Object obj) {
                return CallRecordingTonePlayer.lambda$isRecordingInProgress$2(this.f$0, (AudioRecordingConfiguration) obj);
            }
        }).count() > 0;
    }

    public static boolean lambda$isRecordingInProgress$2(CallRecordingTonePlayer callRecordingTonePlayer, AudioRecordingConfiguration audioRecordingConfiguration) {
        return !callRecordingTonePlayer.hasCallForPackage(audioRecordingConfiguration.getClientPackageName());
    }

    private void startCallRecordingTone(Context context) {
        if (this.mRecordingTonePlayer != null) {
            return;
        }
        AudioDeviceInfo telephonyDevice = getTelephonyDevice(this.mAudioManager);
        if (telephonyDevice != null) {
            Log.i(this, "startCallRecordingTone: playing call recording tone to remote end.", new Object[0]);
            this.mRecordingTonePlayer = MediaPlayer.create(context, R.raw.record);
            this.mRecordingTonePlayer.setLooping(true);
            this.mRecordingTonePlayer.setPreferredDevice(telephonyDevice);
            this.mRecordingTonePlayer.setVolume(0.1f);
            this.mRecordingTonePlayer.start();
            return;
        }
        Log.w(this, "startCallRecordingTone: can't find telephony audio device.", new Object[0]);
    }

    private void stopCallRecordingTone() {
        if (this.mRecordingTonePlayer != null) {
            Log.i(this, "stopCallRecordingTone: stopping call recording tone.", new Object[0]);
            this.mRecordingTonePlayer.stop();
            this.mRecordingTonePlayer = null;
        }
    }

    private AudioDeviceInfo getTelephonyDevice(AudioManager audioManager) {
        for (AudioDeviceInfo audioDeviceInfo : audioManager.getDevices(2)) {
            if (audioDeviceInfo.getType() == 18) {
                return audioDeviceInfo;
            }
        }
        return null;
    }

    private boolean hasCallForPackage(final String str) {
        return this.mCalls.stream().filter(new Predicate() {
            @Override
            public final boolean test(Object obj) {
                return CallRecordingTonePlayer.lambda$hasCallForPackage$3(str, (Call) obj);
            }
        }).count() >= 1;
    }

    static boolean lambda$hasCallForPackage$3(String str, Call call) {
        return (call.getTargetPhoneAccount() != null && call.getTargetPhoneAccount().getComponentName().getPackageName().equals(str)) || (call.getConnectionManagerPhoneAccount() != null && call.getConnectionManagerPhoneAccount().getComponentName().getPackageName().equals(str));
    }

    @VisibleForTesting
    public boolean hasCalls() {
        return this.mCalls.size() > 0;
    }

    @VisibleForTesting
    public boolean isRecording() {
        return this.mIsRecording;
    }
}
