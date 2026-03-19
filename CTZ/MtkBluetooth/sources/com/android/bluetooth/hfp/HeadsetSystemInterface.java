package com.android.bluetooth.hfp;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.IBluetoothHeadsetPhone;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.media.AudioManager;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.RemoteException;
import android.os.SystemProperties;
import android.util.Log;
import com.android.internal.annotations.VisibleForTesting;
import com.android.vcard.VCardConfig;

@VisibleForTesting
public class HeadsetSystemInterface {
    private final AudioManager mAudioManager;
    private final HeadsetPhoneState mHeadsetPhoneState;
    private final HeadsetService mHeadsetService;
    private volatile IBluetoothHeadsetPhone mPhoneProxy;
    private final ServiceConnection mPhoneProxyConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            if (HeadsetSystemInterface.DBG) {
                Log.d(HeadsetSystemInterface.TAG, "Proxy object connected");
            }
            synchronized (HeadsetSystemInterface.this) {
                HeadsetSystemInterface.this.mPhoneProxy = IBluetoothHeadsetPhone.Stub.asInterface(iBinder);
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            if (HeadsetSystemInterface.DBG) {
                Log.d(HeadsetSystemInterface.TAG, "Proxy object disconnected");
            }
            synchronized (HeadsetSystemInterface.this) {
                HeadsetSystemInterface.this.mPhoneProxy = null;
            }
        }
    };
    private PowerManager.WakeLock mVoiceRecognitionWakeLock;
    private static final String TAG = HeadsetSystemInterface.class.getSimpleName();
    private static final boolean DBG = SystemProperties.get("persist.vendor.bluetooth.hostloglevel", "").equals("sqc");

    HeadsetSystemInterface(HeadsetService headsetService) {
        if (headsetService == null) {
            Log.wtfStack(TAG, "HeadsetService parameter is null");
        }
        this.mHeadsetService = headsetService;
        this.mAudioManager = (AudioManager) this.mHeadsetService.getSystemService("audio");
        this.mVoiceRecognitionWakeLock = ((PowerManager) this.mHeadsetService.getSystemService("power")).newWakeLock(1, TAG + ":VoiceRecognition");
        this.mVoiceRecognitionWakeLock.setReferenceCounted(false);
        this.mHeadsetPhoneState = new HeadsetPhoneState(this.mHeadsetService);
    }

    public synchronized void init() {
        Intent intent = new Intent(IBluetoothHeadsetPhone.class.getName());
        intent.setComponent(intent.resolveSystemService(this.mHeadsetService.getPackageManager(), 0));
        if (intent.getComponent() == null || !this.mHeadsetService.bindService(intent, this.mPhoneProxyConnection, 0)) {
            Log.wtfStack(TAG, "Could not bind to IBluetoothHeadsetPhone Service, intent=" + intent);
        }
    }

    public synchronized void stop() {
        if (this.mPhoneProxy != null) {
            if (DBG) {
                Log.d(TAG, "Unbinding phone proxy");
            }
            this.mPhoneProxy = null;
            this.mHeadsetService.unbindService(this.mPhoneProxyConnection);
        }
        this.mHeadsetPhoneState.cleanup();
    }

    @VisibleForTesting
    public AudioManager getAudioManager() {
        return this.mAudioManager;
    }

    @VisibleForTesting
    public PowerManager.WakeLock getVoiceRecognitionWakeLock() {
        return this.mVoiceRecognitionWakeLock;
    }

    @VisibleForTesting
    public HeadsetPhoneState getHeadsetPhoneState() {
        return this.mHeadsetPhoneState;
    }

    @VisibleForTesting
    public void answerCall(BluetoothDevice bluetoothDevice) {
        if (bluetoothDevice == null) {
            Log.w(TAG, "answerCall device is null");
            return;
        }
        if (this.mPhoneProxy != null) {
            try {
                this.mHeadsetService.setActiveDevice(bluetoothDevice);
                this.mPhoneProxy.answerCall();
                return;
            } catch (RemoteException e) {
                Log.e(TAG, Log.getStackTraceString(new Throwable()));
                return;
            }
        }
        Log.e(TAG, "Handsfree phone proxy null for answering call");
    }

    @VisibleForTesting
    public void hangupCall(BluetoothDevice bluetoothDevice) {
        if (bluetoothDevice == null) {
            Log.w(TAG, "hangupCall device is null");
            return;
        }
        if (this.mHeadsetService.isVirtualCallStarted()) {
            this.mHeadsetService.stopScoUsingVirtualVoiceCall();
        } else {
            if (this.mPhoneProxy != null) {
                try {
                    this.mPhoneProxy.hangupCall();
                    return;
                } catch (RemoteException e) {
                    Log.e(TAG, Log.getStackTraceString(new Throwable()));
                    return;
                }
            }
            Log.e(TAG, "Handsfree phone proxy null for hanging up call");
        }
    }

    @VisibleForTesting
    public boolean sendDtmf(int i, BluetoothDevice bluetoothDevice) {
        if (bluetoothDevice == null) {
            Log.w(TAG, "sendDtmf device is null");
            return false;
        }
        if (this.mPhoneProxy != null) {
            try {
                return this.mPhoneProxy.sendDtmf(i);
            } catch (RemoteException e) {
                Log.e(TAG, Log.getStackTraceString(new Throwable()));
            }
        } else {
            Log.e(TAG, "Handsfree phone proxy null for sending DTMF");
        }
        return false;
    }

    @VisibleForTesting
    public boolean processChld(int i) {
        if (this.mPhoneProxy != null) {
            try {
                return this.mPhoneProxy.processChld(i);
            } catch (RemoteException e) {
                Log.e(TAG, Log.getStackTraceString(new Throwable()));
                return false;
            }
        }
        Log.e(TAG, "Handsfree phone proxy null for sending DTMF");
        return false;
    }

    @VisibleForTesting
    public String getNetworkOperator() {
        if (this.mPhoneProxy == null) {
            Log.e(TAG, "getNetworkOperator() failed: mPhoneProxy is null");
            return null;
        }
        try {
            return this.mPhoneProxy.getNetworkOperator();
        } catch (RemoteException e) {
            Log.e(TAG, "getNetworkOperator() failed: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    @VisibleForTesting
    public String getSubscriberNumber() {
        if (this.mPhoneProxy == null) {
            Log.e(TAG, "getSubscriberNumber() failed: mPhoneProxy is null");
            return null;
        }
        try {
            return this.mPhoneProxy.getSubscriberNumber();
        } catch (RemoteException e) {
            Log.e(TAG, "getSubscriberNumber() failed: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    @VisibleForTesting
    public boolean listCurrentCalls() {
        if (this.mPhoneProxy == null) {
            Log.e(TAG, "listCurrentCalls() failed: mPhoneProxy is null");
            return false;
        }
        try {
            return this.mPhoneProxy.listCurrentCalls();
        } catch (RemoteException e) {
            Log.e(TAG, "listCurrentCalls() failed: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    @VisibleForTesting
    public void queryPhoneState() {
        if (this.mPhoneProxy != null) {
            try {
                this.mPhoneProxy.queryPhoneState();
                return;
            } catch (RemoteException e) {
                Log.e(TAG, Log.getStackTraceString(new Throwable()));
                return;
            }
        }
        Log.e(TAG, "Handsfree phone proxy null for query phone state");
    }

    @VisibleForTesting
    public boolean isInCall() {
        return this.mHeadsetPhoneState.getNumActiveCall() > 0 || this.mHeadsetPhoneState.getNumHeldCall() > 0 || !(this.mHeadsetPhoneState.getCallState() == 6 || this.mHeadsetPhoneState.getCallState() == 4);
    }

    @VisibleForTesting
    public boolean isRinging() {
        return this.mHeadsetPhoneState.getCallState() == 4;
    }

    @VisibleForTesting
    public boolean isCallIdle() {
        return (isInCall() || isRinging()) ? false : true;
    }

    @VisibleForTesting
    public boolean activateVoiceRecognition() {
        Intent intent = new Intent("android.intent.action.VOICE_COMMAND");
        intent.setFlags(VCardConfig.FLAG_REFRAIN_QP_TO_NAME_PROPERTIES);
        try {
            this.mHeadsetService.startActivity(intent);
            return true;
        } catch (ActivityNotFoundException e) {
            Log.e(TAG, "activateVoiceRecognition, failed due to activity not found for " + intent);
            return false;
        }
    }

    @VisibleForTesting
    public boolean deactivateVoiceRecognition() {
        return true;
    }
}
