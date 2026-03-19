package com.android.bluetooth.a2dpsink;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.media.AudioAttributes;
import android.media.AudioFocusRequest;
import android.media.AudioManager;
import android.os.Handler;
import android.os.Message;
import android.support.v4.provider.FontsContractCompat;
import android.util.Log;
import com.android.bluetooth.R;
import com.android.bluetooth.avrcpcontroller.AvrcpControllerService;
import java.util.List;

public class A2dpSinkStreamHandler extends Handler {
    public static final int AUDIO_FOCUS_CHANGE = 7;
    private static final boolean DBG = false;
    private static final int DEFAULT_DUCK_PERCENT = 25;
    public static final int DELAYED_RESUME = 9;
    public static final int DISCONNECT = 6;
    public static final int REQUEST_FOCUS = 8;
    private static final int SETTLE_TIMEOUT = 1000;
    public static final int SNK_PAUSE = 3;
    public static final int SNK_PLAY = 2;
    public static final int SRC_PAUSE = 5;
    public static final int SRC_PLAY = 4;
    public static final int SRC_STR_START = 0;
    public static final int SRC_STR_STOP = 1;
    private static final int STATE_FOCUS_GRANTED = 1;
    private static final int STATE_FOCUS_LOST = 0;
    private static final String TAG = "A2dpSinkStreamHandler";
    private A2dpSinkStateMachine mA2dpSinkSm;
    private AudioManager mAudioManager;
    private Context mContext;
    private boolean mStreamAvailable = false;
    private boolean mSentPause = false;
    private int mAudioFocus = 0;
    private AudioManager.OnAudioFocusChangeListener mAudioFocusListener = new AudioManager.OnAudioFocusChangeListener() {
        @Override
        public void onAudioFocusChange(int i) {
            A2dpSinkStreamHandler.this.obtainMessage(7, Integer.valueOf(i)).sendToTarget();
        }
    };

    public A2dpSinkStreamHandler(A2dpSinkStateMachine a2dpSinkStateMachine, Context context) {
        this.mA2dpSinkSm = a2dpSinkStateMachine;
        this.mContext = context;
        this.mAudioManager = (AudioManager) context.getSystemService("audio");
    }

    @Override
    public void handleMessage(Message message) {
        switch (message.what) {
            case 0:
                this.mStreamAvailable = true;
                if (this.mAudioFocus == 0) {
                    requestAudioFocus();
                }
                if (this.mAudioFocus == 0) {
                    sendAvrcpPause();
                } else {
                    startAvrcpUpdates();
                }
                break;
            case 1:
                this.mStreamAvailable = false;
                stopAvrcpUpdates();
                break;
            case 2:
                if (this.mAudioFocus == 0) {
                    requestAudioFocus();
                }
                startAvrcpUpdates();
                break;
            case 3:
                stopAvrcpUpdates();
                break;
            case 4:
                if (this.mAudioFocus == 0) {
                    requestAudioFocus();
                }
                Log.d(TAG, "remote device sends play command.");
                startAvrcpUpdates();
                break;
            case 5:
                stopAvrcpUpdates();
                break;
            case 6:
                stopAvrcpUpdates();
                this.mSentPause = false;
                break;
            case 7:
                int iIntValue = ((Integer) message.obj).intValue();
                if (iIntValue == 1) {
                    startAvrcpUpdates();
                    startFluorideStreaming();
                    if (this.mSentPause) {
                        sendMessageDelayed(obtainMessage(9), 1000L);
                    }
                    break;
                } else {
                    switch (iIntValue) {
                        case FontsContractCompat.FontRequestCallback.FAIL_REASON_FONT_LOAD_ERROR:
                            int integer = this.mContext.getResources().getInteger(R.integer.a2dp_sink_duck_percent);
                            if (integer < 0 || integer > 100) {
                                Log.e(TAG, "Invalid duck percent using default.");
                                integer = 25;
                            }
                            setFluorideAudioTrackGain(integer / 100.0f);
                            break;
                        case -2:
                            if (this.mStreamAvailable) {
                                sendAvrcpPause();
                                this.mSentPause = true;
                            }
                            stopFluorideStreaming();
                            break;
                        case -1:
                            abandonAudioFocus();
                            sendAvrcpPause();
                            break;
                    }
                }
                break;
            case 8:
                if (this.mAudioFocus == 0) {
                    requestAudioFocus();
                }
                break;
            case 9:
                sendAvrcpPlay();
                this.mSentPause = false;
                break;
            default:
                Log.w(TAG, "Received unexpected event: " + message.what);
                break;
        }
    }

    private synchronized int requestAudioFocus() {
        int iRequestAudioFocus;
        iRequestAudioFocus = this.mAudioManager.requestAudioFocus(new AudioFocusRequest.Builder(1).setAudioAttributes(new AudioAttributes.Builder().setUsage(1).setContentType(0).build()).setWillPauseWhenDucked(true).setOnAudioFocusChangeListener(this.mAudioFocusListener, this).build());
        if (iRequestAudioFocus == 1) {
            startAvrcpUpdates();
            startFluorideStreaming();
            this.mAudioFocus = 1;
        }
        return iRequestAudioFocus;
    }

    private synchronized void abandonAudioFocus() {
        stopFluorideStreaming();
        this.mAudioManager.abandonAudioFocus(this.mAudioFocusListener);
        this.mAudioFocus = 0;
    }

    private void startFluorideStreaming() {
        this.mA2dpSinkSm.informAudioFocusStateNative(1);
        this.mA2dpSinkSm.informAudioTrackGainNative(1.0f);
    }

    private void stopFluorideStreaming() {
        this.mA2dpSinkSm.informAudioFocusStateNative(0);
    }

    private void setFluorideAudioTrackGain(float f) {
        this.mA2dpSinkSm.informAudioTrackGainNative(f);
    }

    private void startAvrcpUpdates() {
        AvrcpControllerService avrcpControllerService = AvrcpControllerService.getAvrcpControllerService();
        if (avrcpControllerService != null && avrcpControllerService.getConnectedDevices().size() == 1) {
            avrcpControllerService.startAvrcpUpdates();
        } else {
            Log.e(TAG, "startAvrcpUpdates failed because of connection.");
        }
    }

    private void stopAvrcpUpdates() {
        AvrcpControllerService avrcpControllerService = AvrcpControllerService.getAvrcpControllerService();
        if (avrcpControllerService != null && avrcpControllerService.getConnectedDevices().size() == 1) {
            avrcpControllerService.stopAvrcpUpdates();
        } else {
            Log.e(TAG, "stopAvrcpUpdates failed because of connection.");
        }
    }

    private void sendAvrcpPause() {
        AvrcpControllerService avrcpControllerService = AvrcpControllerService.getAvrcpControllerService();
        if (avrcpControllerService != null) {
            List<BluetoothDevice> connectedDevices = avrcpControllerService.getConnectedDevices();
            if (!connectedDevices.isEmpty()) {
                BluetoothDevice bluetoothDevice = connectedDevices.get(0);
                avrcpControllerService.sendPassThroughCmd(bluetoothDevice, 70, 0);
                avrcpControllerService.sendPassThroughCmd(bluetoothDevice, 70, 1);
                return;
            }
            return;
        }
        Log.e(TAG, "Passthrough not sent, connection un-available.");
    }

    private void sendAvrcpPlay() {
        AvrcpControllerService avrcpControllerService = AvrcpControllerService.getAvrcpControllerService();
        if (avrcpControllerService != null) {
            List<BluetoothDevice> connectedDevices = avrcpControllerService.getConnectedDevices();
            if (!connectedDevices.isEmpty()) {
                BluetoothDevice bluetoothDevice = connectedDevices.get(0);
                avrcpControllerService.sendPassThroughCmd(bluetoothDevice, 68, 0);
                avrcpControllerService.sendPassThroughCmd(bluetoothDevice, 68, 1);
                return;
            }
            return;
        }
        Log.e(TAG, "Passthrough not sent, connection un-available.");
    }

    synchronized int getAudioFocus() {
        return this.mAudioFocus;
    }

    private boolean isIotDevice() {
        return this.mContext.getPackageManager().hasSystemFeature("android.hardware.type.embedded");
    }

    private boolean isTvDevice() {
        return this.mContext.getPackageManager().hasSystemFeature("android.software.leanback");
    }
}
