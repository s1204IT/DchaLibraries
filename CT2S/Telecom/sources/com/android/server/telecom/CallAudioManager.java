package com.android.server.telecom;

import android.content.Context;
import android.media.AudioManager;
import android.os.Looper;
import android.telecom.AudioState;
import android.widget.Toast;
import com.android.internal.util.IndentingPrintWriter;
import com.android.internal.util.Preconditions;
import com.android.server.telecom.WiredHeadsetManager;
import java.util.Objects;

final class CallAudioManager extends CallsManagerListenerBase implements WiredHeadsetManager.Listener {
    private int mAudioFocusStreamType;
    private final AudioManager mAudioManager;
    private AudioState mAudioState;
    private final BluetoothManager mBluetoothManager;
    private final Context mContext;
    private boolean mIsRinging;
    private boolean mIsTonePlaying;
    private int mMostRecentlyUsedMode = 2;
    private final StatusBarNotifier mStatusBarNotifier;
    private boolean mWasSpeakerOn;
    private final WiredHeadsetManager mWiredHeadsetManager;

    CallAudioManager(Context context, StatusBarNotifier statusBarNotifier, WiredHeadsetManager wiredHeadsetManager) {
        this.mStatusBarNotifier = statusBarNotifier;
        this.mAudioManager = (AudioManager) context.getSystemService("audio");
        this.mBluetoothManager = new BluetoothManager(context, this);
        this.mWiredHeadsetManager = wiredHeadsetManager;
        this.mWiredHeadsetManager.addListener(this);
        saveAudioState(getInitialAudioState(null));
        this.mAudioFocusStreamType = -1;
        this.mContext = context;
    }

    AudioState getAudioState() {
        return this.mAudioState;
    }

    @Override
    public void onCallAdded(Call call) {
        onCallUpdated(call);
        if (hasFocus() && getForegroundCall() == call && !call.isIncoming()) {
            setSystemAudioState(false, this.mAudioState.getRoute(), this.mAudioState.getSupportedRouteMask());
        }
    }

    @Override
    public void onCallRemoved(Call call) {
        if (hasFocus()) {
            if (CallsManager.getInstance().getCalls().isEmpty()) {
                Log.v(this, "all calls removed, reseting system audio to default state", new Object[0]);
                setInitialAudioState(null, false);
                this.mWasSpeakerOn = false;
            }
            updateAudioStreamAndMode();
        }
    }

    @Override
    public void onCallStateChanged(Call call, int i, int i2) {
        onCallUpdated(call);
    }

    @Override
    public void onIncomingCallAnswered(Call call) {
        int route = this.mAudioState.getRoute();
        if ((CallsManager.getInstance().getCalls().size() == 1) && this.mBluetoothManager.isBluetoothAvailable()) {
            this.mBluetoothManager.connectBluetoothAudio();
            route = 2;
        }
        setSystemAudioState(false, route, this.mAudioState.getSupportedRouteMask());
    }

    @Override
    public void onForegroundCallChanged(Call call, Call call2) {
        onCallUpdated(call2);
        updateAudioForForegroundCall();
    }

    @Override
    public void onIsVoipAudioModeChanged(Call call) {
        updateAudioStreamAndMode();
    }

    @Override
    public void onWiredHeadsetPluggedInChanged(boolean z, boolean z2) {
        int i = 1;
        boolean z3 = false;
        if (hasFocus()) {
            boolean z4 = this.mAudioState.getRoute() == 4;
            int route = this.mAudioState.getRoute();
            if (z2) {
                ((AudioManager) this.mContext.getSystemService("audio")).setParameters("Extra_volume=false");
                Log.i(this, "Disable extra volume when mode changes from SPKR or EARPIECE to other mode", new Object[0]);
                i = 4;
            } else if (z4) {
                Call foregroundCall = getForegroundCall();
                if (foregroundCall != null && foregroundCall.isAlive()) {
                    z3 = true;
                }
                if (z3) {
                    if (this.mWasSpeakerOn) {
                        i = 8;
                    }
                }
            } else {
                i = route;
            }
            setSystemAudioState(this.mAudioState.isMuted(), i, calculateSupportedRoutes());
        }
    }

    void toggleMute() {
        mute(!this.mAudioState.isMuted());
    }

    void mute(boolean z) {
        if (hasFocus()) {
            Log.v(this, "mute, shouldMute: %b", Boolean.valueOf(z));
            if (CallsManager.getInstance().hasEmergencyCall()) {
                Log.v(this, "ignoring mute for emergency call", new Object[0]);
                z = false;
            }
            if (this.mAudioState.isMuted() != z) {
                setSystemAudioState(z, this.mAudioState.getRoute(), this.mAudioState.getSupportedRouteMask());
            }
        }
    }

    boolean extraVolume(boolean z) {
        AudioManager audioManager = (AudioManager) this.mContext.getSystemService("audio");
        if (audioManager.getDevicesForStream(0) == 2 || audioManager.getDevicesForStream(0) == 1) {
            audioManager.setParameters("Extra_volume=" + (z ? "true" : "false"));
            if (z) {
                new Thread() {
                    @Override
                    public void run() {
                        Looper.prepare();
                        Toast.makeText(CallAudioManager.this.mContext, CallAudioManager.this.mContext.getText(R.string.extra_volume_on), 0).show();
                        Looper.loop();
                    }
                }.start();
            }
        } else if (z) {
            new Thread() {
                @Override
                public void run() {
                    Looper.prepare();
                    Toast.makeText(CallAudioManager.this.mContext, CallAudioManager.this.mContext.getText(R.string.extra_volume_on_wrong_device), 0).show();
                    Looper.loop();
                }
            }.start();
        }
        return true;
    }

    boolean record() {
        Exception e;
        Exception e2;
        boolean z = true;
        Log.v(this, "record ...", new Object[0]);
        CallRecorder recorder = CallRecorder.getRecorder();
        if (!CallRecorder.recorderOn) {
            try {
                recorder.startRecording();
                CallRecorder.recorderOn = true;
            } catch (Exception e3) {
                z = false;
                e2 = e3;
            }
            try {
                final String string = this.mContext.getString(R.string.vc_start_record);
                new Thread() {
                    @Override
                    public void run() {
                        Looper.prepare();
                        Toast.makeText(CallAudioManager.this.mContext, string, 0).show();
                        Looper.loop();
                    }
                }.start();
            } catch (Exception e4) {
                e2 = e4;
                recorder.deleteRecordFile();
                e2.printStackTrace();
            }
        } else {
            try {
                recorder.stopRecording();
                String strSaveToDB = recorder.saveToDB(this.mContext);
                CallRecorder.recorderOn = false;
                try {
                    final String str = strSaveToDB + " " + this.mContext.getString(R.string.vc_record_saved);
                    new Thread() {
                        @Override
                        public void run() {
                            Looper.prepare();
                            Toast.makeText(CallAudioManager.this.mContext, str, 0).show();
                            Looper.loop();
                        }
                    }.start();
                } catch (Exception e5) {
                    e = e5;
                    recorder.deleteRecordFile();
                    e.printStackTrace();
                }
            } catch (Exception e6) {
                z = false;
                e = e6;
            }
        }
        return z;
    }

    void setAudioRoute(int i) {
        if (hasFocus()) {
            Log.v(this, "setAudioRoute, route: %s", AudioState.audioRouteToString(i));
            if (i != 8 && i != 1) {
                ((AudioManager) this.mContext.getSystemService("audio")).setParameters("Extra_volume=false");
                Log.i(this, "Disable extra volume when mode changes from SPKR or EARPIECE to other mode", new Object[0]);
            }
            int iSelectWiredOrEarpiece = selectWiredOrEarpiece(i, this.mAudioState.getSupportedRouteMask());
            if ((this.mAudioState.getSupportedRouteMask() | iSelectWiredOrEarpiece) == 0) {
                Log.wtf(this, "Asking to set to a route that is unsupported: %d", Integer.valueOf(iSelectWiredOrEarpiece));
            } else if (this.mAudioState.getRoute() != iSelectWiredOrEarpiece) {
                this.mWasSpeakerOn = iSelectWiredOrEarpiece == 8;
                setSystemAudioState(this.mAudioState.isMuted(), iSelectWiredOrEarpiece, this.mAudioState.getSupportedRouteMask());
            }
        }
    }

    void setIsRinging(boolean z) {
        if (this.mIsRinging != z) {
            Log.v(this, "setIsRinging %b -> %b", Boolean.valueOf(this.mIsRinging), Boolean.valueOf(z));
            this.mIsRinging = z;
            updateAudioStreamAndMode();
        }
    }

    void setIsTonePlaying(boolean z) {
        ThreadUtil.checkOnMainThread();
        if (this.mIsTonePlaying != z) {
            Log.v(this, "mIsTonePlaying %b -> %b.", Boolean.valueOf(this.mIsTonePlaying), Boolean.valueOf(z));
            this.mIsTonePlaying = z;
            updateAudioStreamAndMode();
        }
    }

    void onBluetoothStateChange(BluetoothManager bluetoothManager) {
        if (hasFocus()) {
            int iCalculateSupportedRoutes = calculateSupportedRoutes();
            int route = this.mAudioState.getRoute();
            if (bluetoothManager.isBluetoothAudioConnectedOrPending()) {
                ((AudioManager) this.mContext.getSystemService("audio")).setParameters("Extra_volume=false");
                Log.i(this, "Disable extra volume when mode changes from SPKR or EARPIECE to other mode", new Object[0]);
                route = 2;
            } else if (this.mAudioState.getRoute() == 2) {
                route = selectWiredOrEarpiece(5, iCalculateSupportedRoutes);
                this.mWasSpeakerOn = false;
            }
            setSystemAudioState(this.mAudioState.isMuted(), route, iCalculateSupportedRoutes);
        }
    }

    boolean isBluetoothAudioOn() {
        return this.mBluetoothManager.isBluetoothAudioConnected();
    }

    private void saveAudioState(AudioState audioState) {
        this.mAudioState = audioState;
        this.mStatusBarNotifier.notifyMute(this.mAudioState.isMuted());
        this.mStatusBarNotifier.notifySpeakerphone(this.mAudioState.getRoute() == 8);
    }

    private void onCallUpdated(Call call) {
        boolean z = this.mAudioFocusStreamType != 0;
        updateAudioStreamAndMode();
        if (z && this.mAudioFocusStreamType == 0) {
            setInitialAudioState(call, true);
        }
    }

    private void setSystemAudioState(boolean z, int i, int i2) {
        setSystemAudioState(false, z, i, i2);
    }

    private void setSystemAudioState(boolean z, boolean z2, int i, int i2) {
        if (hasFocus()) {
            AudioState audioState = this.mAudioState;
            saveAudioState(new AudioState(z2, i, i2));
            if (z || !Objects.equals(audioState, this.mAudioState)) {
                Log.i(this, "changing audio state from %s to %s", audioState, this.mAudioState);
                if (this.mAudioState.isMuted() != this.mAudioManager.isMicrophoneMute()) {
                    Log.i(this, "changing microphone mute state to: %b", Boolean.valueOf(this.mAudioState.isMuted()));
                    this.mAudioManager.setMicrophoneMute(this.mAudioState.isMuted());
                }
                if (this.mAudioState.getRoute() == 2) {
                    turnOnSpeaker(false);
                    turnOnBluetooth(true);
                } else if (this.mAudioState.getRoute() == 8) {
                    turnOnBluetooth(false);
                    turnOnSpeaker(true);
                } else if (this.mAudioState.getRoute() == 1 || this.mAudioState.getRoute() == 4) {
                    turnOnBluetooth(false);
                    turnOnSpeaker(false);
                }
                if (!audioState.equals(this.mAudioState)) {
                    CallsManager.getInstance().onAudioStateChanged(audioState, this.mAudioState);
                    updateAudioForForegroundCall();
                }
            }
        }
    }

    private void turnOnSpeaker(boolean z) {
        if (this.mAudioManager.isSpeakerphoneOn() != z) {
            Log.i(this, "turning speaker phone %s", Boolean.valueOf(z));
            this.mAudioManager.setSpeakerphoneOn(z);
        }
    }

    private void turnOnBluetooth(boolean z) {
        if (this.mBluetoothManager.isBluetoothAvailable() && z != this.mBluetoothManager.isBluetoothAudioConnectedOrPending()) {
            Log.i(this, "connecting bluetooth %s", Boolean.valueOf(z));
            if (z) {
                this.mBluetoothManager.connectBluetoothAudio();
            } else {
                this.mBluetoothManager.disconnectBluetoothAudio();
            }
        }
    }

    private void updateAudioStreamAndMode() {
        Log.i(this, "updateAudioStreamAndMode, mIsRinging: %b, mIsTonePlaying: %b", Boolean.valueOf(this.mIsRinging), Boolean.valueOf(this.mIsTonePlaying));
        if (this.mIsRinging) {
            requestAudioFocusAndSetMode(2, 1);
            return;
        }
        Call foregroundCall = getForegroundCall();
        Call firstCallWithState = CallsManager.getInstance().getFirstCallWithState(2);
        if (foregroundCall != null && firstCallWithState == null) {
            requestAudioFocusAndSetMode(0, foregroundCall.getIsVoipAudioMode() ? 3 : 2);
        } else if (this.mIsTonePlaying) {
            requestAudioFocusAndSetMode(0, this.mMostRecentlyUsedMode);
        } else if (!hasRingingForegroundCall()) {
            abandonAudioFocus();
        }
    }

    private void requestAudioFocusAndSetMode(int i, int i2) {
        Log.i(this, "requestAudioFocusAndSetMode, stream: %d -> %d, mode: %d", Integer.valueOf(this.mAudioFocusStreamType), Integer.valueOf(i), Integer.valueOf(i2));
        Preconditions.checkState(i != -1);
        if (this.mAudioFocusStreamType != i) {
            Log.v(this, "requesting audio focus for stream: %d", Integer.valueOf(i));
            this.mAudioManager.requestAudioFocusForCall(i, 2);
        }
        this.mAudioFocusStreamType = i;
        setMode(i2);
    }

    private void abandonAudioFocus() {
        if (hasFocus()) {
            setMode(0);
            Log.v(this, "abandoning audio focus", new Object[0]);
            this.mAudioManager.abandonAudioFocusForCall();
            this.mAudioFocusStreamType = -1;
        }
    }

    private void setMode(int i) {
        Preconditions.checkState(hasFocus());
        int mode = this.mAudioManager.getMode();
        Log.v(this, "Request to change audio mode from %d to %d", Integer.valueOf(mode), Integer.valueOf(i));
        if (mode != i) {
            if (mode == 2 && i == 1) {
                Log.i(this, "Transition from IN_CALL -> RINGTONE. Resetting to NORMAL first.", new Object[0]);
                this.mAudioManager.setMode(0);
            }
            this.mAudioManager.setMode(i);
            this.mMostRecentlyUsedMode = i;
        }
    }

    private int selectWiredOrEarpiece(int i, int i2) {
        if (i == 5) {
            int i3 = i2 & 5;
            if (i3 == 0) {
                Log.wtf(this, "One of wired headset or earpiece should always be valid.", new Object[0]);
                return 1;
            }
            return i3;
        }
        return i;
    }

    private int calculateSupportedRoutes() {
        int i;
        if (this.mWiredHeadsetManager.isPluggedIn()) {
            i = 12;
        } else {
            i = 9;
        }
        if (this.mBluetoothManager.isBluetoothAvailable()) {
            return i | 2;
        }
        return i;
    }

    private AudioState getInitialAudioState(Call call) {
        int iCalculateSupportedRoutes = calculateSupportedRoutes();
        int iSelectWiredOrEarpiece = selectWiredOrEarpiece(5, iCalculateSupportedRoutes);
        if (call != null && this.mBluetoothManager.isBluetoothAvailable()) {
            switch (call.getState()) {
                case 1:
                case 3:
                case 4:
                case 5:
                case 6:
                    iSelectWiredOrEarpiece = 2;
                    break;
            }
        }
        return new AudioState(false, iSelectWiredOrEarpiece, iCalculateSupportedRoutes);
    }

    private void setInitialAudioState(Call call, boolean z) {
        AudioState initialAudioState = getInitialAudioState(call);
        Log.v(this, "setInitialAudioState %s, %s", initialAudioState, call);
        setSystemAudioState(z, initialAudioState.isMuted(), initialAudioState.getRoute(), initialAudioState.getSupportedRouteMask());
    }

    private void updateAudioForForegroundCall() {
        Call foregroundCall = CallsManager.getInstance().getForegroundCall();
        if (foregroundCall != null && foregroundCall.getConnectionService() != null) {
            foregroundCall.getConnectionService().onAudioStateChanged(foregroundCall, this.mAudioState);
        }
    }

    private Call getForegroundCall() {
        Call foregroundCall = CallsManager.getInstance().getForegroundCall();
        if (foregroundCall != null && foregroundCall.getState() == 4) {
            return null;
        }
        return foregroundCall;
    }

    private boolean hasRingingForegroundCall() {
        Call foregroundCall = CallsManager.getInstance().getForegroundCall();
        return foregroundCall != null && foregroundCall.getState() == 4;
    }

    private boolean hasFocus() {
        return this.mAudioFocusStreamType != -1;
    }

    public void dump(IndentingPrintWriter indentingPrintWriter) {
        indentingPrintWriter.println("mAudioState: " + this.mAudioState);
        indentingPrintWriter.println("mBluetoothManager:");
        indentingPrintWriter.increaseIndent();
        this.mBluetoothManager.dump(indentingPrintWriter);
        indentingPrintWriter.decreaseIndent();
        if (this.mWiredHeadsetManager != null) {
            indentingPrintWriter.println("mWiredHeadsetManager:");
            indentingPrintWriter.increaseIndent();
            this.mWiredHeadsetManager.dump(indentingPrintWriter);
            indentingPrintWriter.decreaseIndent();
        } else {
            indentingPrintWriter.println("mWiredHeadsetManager: null");
        }
        indentingPrintWriter.println("mAudioFocusStreamType: " + this.mAudioFocusStreamType);
        indentingPrintWriter.println("mIsRinging: " + this.mIsRinging);
        indentingPrintWriter.println("mIsTonePlaying: " + this.mIsTonePlaying);
        indentingPrintWriter.println("mWasSpeakerOn: " + this.mWasSpeakerOn);
        indentingPrintWriter.println("mMostRecentlyUsedMode: " + this.mMostRecentlyUsedMode);
    }
}
