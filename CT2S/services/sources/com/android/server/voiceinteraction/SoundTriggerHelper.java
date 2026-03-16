package com.android.server.voiceinteraction;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.soundtrigger.IRecognitionStatusCallback;
import android.hardware.soundtrigger.SoundTrigger;
import android.hardware.soundtrigger.SoundTriggerModule;
import android.os.Handler;
import android.os.PowerManager;
import android.os.RemoteException;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Slog;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayList;

public class SoundTriggerHelper implements SoundTrigger.StatusListener {
    static final boolean DBG = false;
    private static final int INVALID_VALUE = Integer.MIN_VALUE;
    public static final int STATUS_ERROR = Integer.MIN_VALUE;
    public static final int STATUS_OK = 0;
    static final String TAG = "SoundTriggerHelper";
    private IRecognitionStatusCallback mActiveListener;
    private final Context mContext;
    private SoundTriggerModule mModule;
    private final PhoneStateListener mPhoneStateListener;
    private final PowerManager mPowerManager;
    private PowerSaveModeListener mPowerSaveModeListener;
    private final TelephonyManager mTelephonyManager;
    final SoundTrigger.ModuleProperties moduleProperties;
    private final Object mLock = new Object();
    private int mKeyphraseId = Integer.MIN_VALUE;
    private int mCurrentSoundModelHandle = Integer.MIN_VALUE;
    private SoundTrigger.KeyphraseSoundModel mCurrentSoundModel = null;
    private SoundTrigger.RecognitionConfig mRecognitionConfig = null;
    private boolean mRequested = DBG;
    private boolean mCallActive = DBG;
    private boolean mIsPowerSaveMode = DBG;
    private boolean mServiceDisabled = DBG;
    private boolean mStarted = DBG;

    SoundTriggerHelper(Context context) {
        ArrayList<SoundTrigger.ModuleProperties> modules = new ArrayList<>();
        int status = SoundTrigger.listModules(modules);
        this.mContext = context;
        this.mTelephonyManager = (TelephonyManager) context.getSystemService("phone");
        this.mPowerManager = (PowerManager) context.getSystemService("power");
        this.mPhoneStateListener = new MyCallStateListener();
        if (status != 0 || modules.size() == 0) {
            Slog.w(TAG, "listModules status=" + status + ", # of modules=" + modules.size());
            this.moduleProperties = null;
            this.mModule = null;
            return;
        }
        this.moduleProperties = modules.get(0);
    }

    int startRecognition(int keyphraseId, SoundTrigger.KeyphraseSoundModel soundModel, IRecognitionStatusCallback listener, SoundTrigger.RecognitionConfig recognitionConfig) {
        if (soundModel == null || listener == null || recognitionConfig == null) {
            return Integer.MIN_VALUE;
        }
        synchronized (this.mLock) {
            if (!this.mStarted) {
                this.mCallActive = this.mTelephonyManager.getCallState() != 0;
                this.mTelephonyManager.listen(this.mPhoneStateListener, 32);
                if (this.mPowerSaveModeListener == null) {
                    this.mPowerSaveModeListener = new PowerSaveModeListener();
                    this.mContext.registerReceiver(this.mPowerSaveModeListener, new IntentFilter("android.os.action.POWER_SAVE_MODE_CHANGED"));
                }
                this.mIsPowerSaveMode = this.mPowerManager.isPowerSaveMode();
            }
            if (this.moduleProperties == null) {
                Slog.w(TAG, "Attempting startRecognition without the capability");
                return Integer.MIN_VALUE;
            }
            if (this.mModule == null) {
                this.mModule = SoundTrigger.attachModule(this.moduleProperties.id, this, (Handler) null);
                if (this.mModule == null) {
                    Slog.w(TAG, "startRecognition cannot attach to sound trigger module");
                    return Integer.MIN_VALUE;
                }
            }
            if (this.mCurrentSoundModelHandle != Integer.MIN_VALUE && !soundModel.equals(this.mCurrentSoundModel)) {
                Slog.w(TAG, "Unloading previous sound model");
                int status = this.mModule.unloadSoundModel(this.mCurrentSoundModelHandle);
                if (status != 0) {
                    Slog.w(TAG, "unloadSoundModel call failed with " + status);
                }
                internalClearSoundModelLocked();
                this.mStarted = DBG;
            }
            if (this.mActiveListener != null && this.mActiveListener.asBinder() != listener.asBinder()) {
                Slog.w(TAG, "Canceling previous recognition");
                try {
                    this.mActiveListener.onError(Integer.MIN_VALUE);
                } catch (RemoteException e) {
                    Slog.w(TAG, "RemoteException in onDetectionStopped", e);
                }
                this.mActiveListener = null;
            }
            int soundModelHandle = this.mCurrentSoundModelHandle;
            if (this.mCurrentSoundModelHandle == Integer.MIN_VALUE || this.mCurrentSoundModel == null) {
                int[] handle = {Integer.MIN_VALUE};
                int status2 = this.mModule.loadSoundModel(soundModel, handle);
                if (status2 != 0) {
                    Slog.w(TAG, "loadSoundModel call failed with " + status2);
                    return status2;
                }
                if (handle[0] == Integer.MIN_VALUE) {
                    Slog.w(TAG, "loadSoundModel call returned invalid sound model handle");
                    return Integer.MIN_VALUE;
                }
                soundModelHandle = handle[0];
            }
            this.mRequested = true;
            this.mKeyphraseId = keyphraseId;
            this.mCurrentSoundModelHandle = soundModelHandle;
            this.mCurrentSoundModel = soundModel;
            this.mRecognitionConfig = recognitionConfig;
            this.mActiveListener = listener;
            return updateRecognitionLocked(DBG);
        }
    }

    int stopRecognition(int keyphraseId, IRecognitionStatusCallback listener) {
        int status = Integer.MIN_VALUE;
        if (listener != null) {
            synchronized (this.mLock) {
                if (this.moduleProperties == null || this.mModule == null) {
                    Slog.w(TAG, "Attempting stopRecognition without the capability");
                } else if (this.mActiveListener == null) {
                    Slog.w(TAG, "Attempting stopRecognition without a successful startRecognition");
                } else if (this.mActiveListener.asBinder() != listener.asBinder()) {
                    Slog.w(TAG, "Attempting stopRecognition for another recognition");
                } else {
                    this.mRequested = DBG;
                    status = updateRecognitionLocked(DBG);
                    if (status == 0) {
                        internalClearStateLocked();
                    }
                }
            }
        }
        return status;
    }

    void stopAllRecognitions() {
        synchronized (this.mLock) {
            if (this.moduleProperties != null && this.mModule != null) {
                if (this.mCurrentSoundModelHandle != Integer.MIN_VALUE) {
                    this.mRequested = DBG;
                    updateRecognitionLocked(DBG);
                    internalClearStateLocked();
                }
            }
        }
    }

    public void onRecognition(SoundTrigger.RecognitionEvent event) {
        if (event == null || !(event instanceof SoundTrigger.KeyphraseRecognitionEvent)) {
            Slog.w(TAG, "Invalid recognition event!");
            return;
        }
        synchronized (this.mLock) {
            if (this.mActiveListener == null) {
                Slog.w(TAG, "received onRecognition event without any listener for it");
                return;
            }
            switch (event.status) {
                case 0:
                    onRecognitionSuccessLocked((SoundTrigger.KeyphraseRecognitionEvent) event);
                    break;
                case 1:
                    onRecognitionAbortLocked();
                    break;
                case 2:
                    onRecognitionFailureLocked();
                    break;
            }
        }
    }

    public void onSoundModelUpdate(SoundTrigger.SoundModelEvent event) {
        if (event == null) {
            Slog.w(TAG, "Invalid sound model event!");
            return;
        }
        synchronized (this.mLock) {
            onSoundModelUpdatedLocked(event);
        }
    }

    public void onServiceStateChange(int state) {
        synchronized (this.mLock) {
            onServiceStateChangedLocked(1 != state ? DBG : true);
        }
    }

    public void onServiceDied() {
        Slog.e(TAG, "onServiceDied!!");
        synchronized (this.mLock) {
            onServiceDiedLocked();
        }
    }

    private void onCallStateChangedLocked(boolean callActive) {
        if (this.mCallActive != callActive) {
            this.mCallActive = callActive;
            updateRecognitionLocked(true);
        }
    }

    private void onPowerSaveModeChangedLocked(boolean isPowerSaveMode) {
        if (this.mIsPowerSaveMode != isPowerSaveMode) {
            this.mIsPowerSaveMode = isPowerSaveMode;
            updateRecognitionLocked(true);
        }
    }

    private void onSoundModelUpdatedLocked(SoundTrigger.SoundModelEvent event) {
    }

    private void onServiceStateChangedLocked(boolean disabled) {
        if (disabled != this.mServiceDisabled) {
            this.mServiceDisabled = disabled;
            updateRecognitionLocked(true);
        }
    }

    private void onRecognitionAbortLocked() {
        Slog.w(TAG, "Recognition aborted");
    }

    private void onRecognitionFailureLocked() {
        Slog.w(TAG, "Recognition failure");
        try {
            if (this.mActiveListener != null) {
                this.mActiveListener.onError(Integer.MIN_VALUE);
            }
        } catch (RemoteException e) {
            Slog.w(TAG, "RemoteException in onError", e);
        } finally {
            internalClearStateLocked();
        }
    }

    private void onRecognitionSuccessLocked(SoundTrigger.KeyphraseRecognitionEvent event) {
        Slog.i(TAG, "Recognition success");
        SoundTrigger.KeyphraseRecognitionExtra[] keyphraseExtras = event.keyphraseExtras;
        if (keyphraseExtras == null || keyphraseExtras.length == 0) {
            Slog.w(TAG, "Invalid keyphrase recognition event!");
            return;
        }
        if (this.mKeyphraseId != keyphraseExtras[0].id) {
            Slog.w(TAG, "received onRecognition event for a different keyphrase");
            return;
        }
        try {
            if (this.mActiveListener != null) {
                this.mActiveListener.onDetected(event);
            }
        } catch (RemoteException e) {
            Slog.w(TAG, "RemoteException in onDetected", e);
        }
        this.mStarted = DBG;
        this.mRequested = this.mRecognitionConfig.allowMultipleTriggers;
        if (this.mRequested) {
            updateRecognitionLocked(true);
        }
    }

    private void onServiceDiedLocked() {
        try {
            try {
                if (this.mActiveListener != null) {
                    this.mActiveListener.onError(-32);
                }
                internalClearSoundModelLocked();
                internalClearStateLocked();
                if (this.mModule != null) {
                    this.mModule.detach();
                    this.mModule = null;
                }
            } catch (RemoteException e) {
                Slog.w(TAG, "RemoteException in onError", e);
                internalClearSoundModelLocked();
                internalClearStateLocked();
                if (this.mModule != null) {
                    this.mModule.detach();
                    this.mModule = null;
                }
            }
        } catch (Throwable th) {
            internalClearSoundModelLocked();
            internalClearStateLocked();
            if (this.mModule != null) {
                this.mModule.detach();
                this.mModule = null;
            }
            throw th;
        }
    }

    private int updateRecognitionLocked(boolean notify) {
        if (this.mModule == null || this.moduleProperties == null || this.mCurrentSoundModelHandle == Integer.MIN_VALUE || this.mActiveListener == null) {
            return 0;
        }
        boolean start = (!this.mRequested || this.mCallActive || this.mServiceDisabled || this.mIsPowerSaveMode) ? false : true;
        if (start == this.mStarted) {
            return 0;
        }
        if (start) {
            int status = this.mModule.startRecognition(this.mCurrentSoundModelHandle, this.mRecognitionConfig);
            if (status != 0) {
                Slog.w(TAG, "startRecognition failed with " + status);
                if (notify) {
                    try {
                        this.mActiveListener.onError(status);
                    } catch (RemoteException e) {
                        Slog.w(TAG, "RemoteException in onError", e);
                    }
                }
            } else {
                this.mStarted = true;
                if (notify) {
                    try {
                        this.mActiveListener.onRecognitionResumed();
                    } catch (RemoteException e2) {
                        Slog.w(TAG, "RemoteException in onRecognitionResumed", e2);
                    }
                }
            }
            return status;
        }
        int status2 = this.mModule.stopRecognition(this.mCurrentSoundModelHandle);
        if (status2 != 0) {
            Slog.w(TAG, "stopRecognition call failed with " + status2);
            if (notify) {
                try {
                    this.mActiveListener.onError(status2);
                } catch (RemoteException e3) {
                    Slog.w(TAG, "RemoteException in onError", e3);
                }
            }
        } else {
            this.mStarted = DBG;
            if (notify) {
                try {
                    this.mActiveListener.onRecognitionPaused();
                } catch (RemoteException e4) {
                    Slog.w(TAG, "RemoteException in onRecognitionPaused", e4);
                }
            }
        }
        return status2;
    }

    private void internalClearStateLocked() {
        this.mStarted = DBG;
        this.mRequested = DBG;
        this.mKeyphraseId = Integer.MIN_VALUE;
        this.mRecognitionConfig = null;
        this.mActiveListener = null;
        this.mTelephonyManager.listen(this.mPhoneStateListener, 0);
        if (this.mPowerSaveModeListener != null) {
            this.mContext.unregisterReceiver(this.mPowerSaveModeListener);
            this.mPowerSaveModeListener = null;
        }
    }

    private void internalClearSoundModelLocked() {
        this.mCurrentSoundModelHandle = Integer.MIN_VALUE;
        this.mCurrentSoundModel = null;
    }

    class MyCallStateListener extends PhoneStateListener {
        MyCallStateListener() {
        }

        @Override
        public void onCallStateChanged(int state, String arg1) {
            synchronized (SoundTriggerHelper.this.mLock) {
                SoundTriggerHelper.this.onCallStateChangedLocked(state != 0 ? true : SoundTriggerHelper.DBG);
            }
        }
    }

    class PowerSaveModeListener extends BroadcastReceiver {
        PowerSaveModeListener() {
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            if ("android.os.action.POWER_SAVE_MODE_CHANGED".equals(intent.getAction())) {
                boolean active = SoundTriggerHelper.this.mPowerManager.isPowerSaveMode();
                synchronized (SoundTriggerHelper.this.mLock) {
                    SoundTriggerHelper.this.onPowerSaveModeChangedLocked(active);
                }
            }
        }
    }

    void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        synchronized (this.mLock) {
            pw.print("  module properties=");
            pw.println((Object) (this.moduleProperties == null ? "null" : this.moduleProperties));
            pw.print("  keyphrase ID=");
            pw.println(this.mKeyphraseId);
            pw.print("  sound model handle=");
            pw.println(this.mCurrentSoundModelHandle);
            pw.print("  sound model UUID=");
            pw.println(this.mCurrentSoundModel == null ? "null" : this.mCurrentSoundModel.uuid);
            pw.print("  current listener=");
            pw.println(this.mActiveListener == null ? "null" : this.mActiveListener.asBinder());
            pw.print("  requested=");
            pw.println(this.mRequested);
            pw.print("  started=");
            pw.println(this.mStarted);
            pw.print("  call active=");
            pw.println(this.mCallActive);
            pw.print("  power save mode active=");
            pw.println(this.mIsPowerSaveMode);
            pw.print("  service disabled=");
            pw.println(this.mServiceDisabled);
        }
    }
}
