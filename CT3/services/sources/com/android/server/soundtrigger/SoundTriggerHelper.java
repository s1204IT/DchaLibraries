package com.android.server.soundtrigger;

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
import com.android.internal.logging.MetricsLogger;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;

public class SoundTriggerHelper implements SoundTrigger.StatusListener {
    static final boolean DBG = false;
    private static final int INVALID_VALUE = Integer.MIN_VALUE;
    public static final int STATUS_ERROR = Integer.MIN_VALUE;
    public static final int STATUS_OK = 0;
    static final String TAG = "SoundTriggerHelper";
    private final Context mContext;
    private HashMap<Integer, UUID> mKeyphraseUuidMap;
    private final HashMap<UUID, ModelData> mModelDataMap;
    private SoundTriggerModule mModule;
    final SoundTrigger.ModuleProperties mModuleProperties;
    private final PhoneStateListener mPhoneStateListener;
    private final PowerManager mPowerManager;
    private PowerSaveModeListener mPowerSaveModeListener;
    private final TelephonyManager mTelephonyManager;
    private final Object mLock = new Object();
    private boolean mCallActive = false;
    private boolean mIsPowerSaveMode = false;
    private boolean mServiceDisabled = false;
    private boolean mRecognitionRunning = false;

    SoundTriggerHelper(Context context) {
        ArrayList<SoundTrigger.ModuleProperties> modules = new ArrayList<>();
        int status = SoundTrigger.listModules(modules);
        this.mContext = context;
        this.mTelephonyManager = (TelephonyManager) context.getSystemService("phone");
        this.mPowerManager = (PowerManager) context.getSystemService("power");
        this.mModelDataMap = new HashMap<>();
        this.mKeyphraseUuidMap = new HashMap<>();
        this.mPhoneStateListener = new MyCallStateListener();
        if (status != 0 || modules.size() == 0) {
            Slog.w(TAG, "listModules status=" + status + ", # of modules=" + modules.size());
            this.mModuleProperties = null;
            this.mModule = null;
            return;
        }
        this.mModuleProperties = modules.get(0);
    }

    int startGenericRecognition(UUID modelId, SoundTrigger.GenericSoundModel soundModel, IRecognitionStatusCallback callback, SoundTrigger.RecognitionConfig recognitionConfig) {
        MetricsLogger.count(this.mContext, "sth_start_recognition", 1);
        if (modelId == null || soundModel == null || callback == null || recognitionConfig == null) {
            Slog.w(TAG, "Passed in bad data to startGenericRecognition().");
            return Integer.MIN_VALUE;
        }
        synchronized (this.mLock) {
            ModelData modelData = getOrCreateGenericModelDataLocked(modelId);
            if (modelData == null) {
                Slog.w(TAG, "Irrecoverable error occurred, check UUID / sound model data.");
                return Integer.MIN_VALUE;
            }
            return startRecognition(soundModel, modelData, callback, recognitionConfig, Integer.MIN_VALUE);
        }
    }

    int startKeyphraseRecognition(int keyphraseId, SoundTrigger.KeyphraseSoundModel soundModel, IRecognitionStatusCallback callback, SoundTrigger.RecognitionConfig recognitionConfig) {
        synchronized (this.mLock) {
            MetricsLogger.count(this.mContext, "sth_start_recognition", 1);
            if (soundModel == null || callback == null || recognitionConfig == null) {
                return Integer.MIN_VALUE;
            }
            ModelData model = getKeyphraseModelDataLocked(keyphraseId);
            if (model != null && !model.isKeyphraseModel()) {
                Slog.e(TAG, "Generic model with same UUID exists.");
                return Integer.MIN_VALUE;
            }
            if (model != null && !model.getModelId().equals(soundModel.uuid)) {
                int status = cleanUpExistingKeyphraseModel(model);
                if (status != 0) {
                    return status;
                }
                removeKeyphraseModelLocked(keyphraseId);
                model = null;
            }
            if (model == null) {
                model = createKeyphraseModelDataLocked(soundModel.uuid, keyphraseId);
            }
            return startRecognition(soundModel, model, callback, recognitionConfig, keyphraseId);
        }
    }

    private int cleanUpExistingKeyphraseModel(ModelData modelData) {
        int status = tryStopAndUnloadLocked(modelData, true, true);
        if (status != 0) {
            Slog.w(TAG, "Unable to stop or unload previous model: " + modelData.toString());
        }
        return status;
    }

    int startRecognition(SoundTrigger.SoundModel soundModel, ModelData modelData, IRecognitionStatusCallback callback, SoundTrigger.RecognitionConfig recognitionConfig, int keyphraseId) {
        int status;
        synchronized (this.mLock) {
            if (this.mModuleProperties == null) {
                Slog.w(TAG, "Attempting startRecognition without the capability");
                return Integer.MIN_VALUE;
            }
            if (this.mModule == null) {
                this.mModule = SoundTrigger.attachModule(this.mModuleProperties.id, this, (Handler) null);
                if (this.mModule == null) {
                    Slog.w(TAG, "startRecognition cannot attach to sound trigger module");
                    return Integer.MIN_VALUE;
                }
            }
            if (!this.mRecognitionRunning) {
                initializeTelephonyAndPowerStateListeners();
            }
            if (modelData.getSoundModel() != null) {
                boolean stopModel = false;
                boolean unloadModel = false;
                if (modelData.getSoundModel().equals(soundModel) && modelData.isModelStarted()) {
                    stopModel = true;
                    unloadModel = false;
                } else if (!modelData.getSoundModel().equals(soundModel)) {
                    stopModel = modelData.isModelStarted();
                    unloadModel = modelData.isModelLoaded();
                }
                if ((stopModel || unloadModel) && (status = tryStopAndUnloadLocked(modelData, stopModel, unloadModel)) != 0) {
                    Slog.w(TAG, "Unable to stop or unload previous model: " + modelData.toString());
                    return status;
                }
            }
            IRecognitionStatusCallback oldCallback = modelData.getCallback();
            if (oldCallback != null && oldCallback.asBinder() != callback.asBinder()) {
                Slog.w(TAG, "Canceling previous recognition for model id: " + modelData.getModelId());
                try {
                    oldCallback.onError(Integer.MIN_VALUE);
                } catch (RemoteException e) {
                    Slog.w(TAG, "RemoteException in onDetectionStopped", e);
                }
                modelData.clearCallback();
            }
            if (!modelData.isModelLoaded()) {
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
                modelData.setHandle(handle[0]);
                modelData.setLoaded();
                Slog.d(TAG, "Sound model loaded with handle:" + handle[0]);
            }
            modelData.setCallback(callback);
            modelData.setRequested(true);
            modelData.setRecognitionConfig(recognitionConfig);
            modelData.setSoundModel(soundModel);
            return startRecognitionLocked(modelData, false);
        }
    }

    int stopGenericRecognition(UUID modelId, IRecognitionStatusCallback callback) {
        synchronized (this.mLock) {
            MetricsLogger.count(this.mContext, "sth_stop_recognition", 1);
            if (callback == null || modelId == null) {
                Slog.e(TAG, "Null callbackreceived for stopGenericRecognition() for modelid:" + modelId);
                return Integer.MIN_VALUE;
            }
            ModelData modelData = this.mModelDataMap.get(modelId);
            if (modelData == null || !modelData.isGenericModel()) {
                Slog.w(TAG, "Attempting stopRecognition on invalid model with id:" + modelId);
                return Integer.MIN_VALUE;
            }
            int status = stopRecognition(modelData, callback);
            if (status != 0) {
                Slog.w(TAG, "stopGenericRecognition failed: " + status);
            }
            return status;
        }
    }

    int stopKeyphraseRecognition(int keyphraseId, IRecognitionStatusCallback callback) {
        synchronized (this.mLock) {
            MetricsLogger.count(this.mContext, "sth_stop_recognition", 1);
            if (callback == null) {
                Slog.e(TAG, "Null callback received for stopKeyphraseRecognition() for keyphraseId:" + keyphraseId);
                return Integer.MIN_VALUE;
            }
            ModelData modelData = getKeyphraseModelDataLocked(keyphraseId);
            if (modelData == null || !modelData.isKeyphraseModel()) {
                Slog.e(TAG, "No model exists for given keyphrase Id.");
                return Integer.MIN_VALUE;
            }
            int status = stopRecognition(modelData, callback);
            if (status != 0) {
                return status;
            }
            return status;
        }
    }

    private int stopRecognition(ModelData modelData, IRecognitionStatusCallback callback) {
        synchronized (this.mLock) {
            if (callback == null) {
                return Integer.MIN_VALUE;
            }
            if (this.mModuleProperties == null || this.mModule == null) {
                Slog.w(TAG, "Attempting stopRecognition without the capability");
                return Integer.MIN_VALUE;
            }
            IRecognitionStatusCallback currentCallback = modelData.getCallback();
            if (modelData == null || currentCallback == null || (!modelData.isRequested() && !modelData.isModelStarted())) {
                Slog.w(TAG, "Attempting stopRecognition without a successful startRecognition");
                return Integer.MIN_VALUE;
            }
            if (currentCallback.asBinder() != callback.asBinder()) {
                Slog.w(TAG, "Attempting stopRecognition for another recognition");
                return Integer.MIN_VALUE;
            }
            modelData.setRequested(false);
            int status = updateRecognitionLocked(modelData, isRecognitionAllowed(), false);
            if (status != 0) {
                return status;
            }
            modelData.setLoaded();
            modelData.clearCallback();
            modelData.setRecognitionConfig(null);
            if (!computeRecognitionRunningLocked()) {
                internalClearGlobalStateLocked();
            }
            return status;
        }
    }

    private int tryStopAndUnloadLocked(ModelData modelData, boolean stopModel, boolean unloadModel) {
        int status = 0;
        if (modelData.isModelNotLoaded()) {
            return 0;
        }
        if (stopModel && modelData.isModelStarted() && (status = stopRecognitionLocked(modelData, false)) != 0) {
            Slog.w(TAG, "stopRecognition failed: " + status);
            return status;
        }
        if (unloadModel && modelData.isModelLoaded()) {
            Slog.d(TAG, "Unloading previously loaded stale model.");
            status = this.mModule.unloadSoundModel(modelData.getHandle());
            MetricsLogger.count(this.mContext, "sth_unloading_stale_model", 1);
            if (status != 0) {
                Slog.w(TAG, "unloadSoundModel call failed with " + status);
            } else {
                modelData.clearState();
            }
        }
        return status;
    }

    public SoundTrigger.ModuleProperties getModuleProperties() {
        return this.mModuleProperties;
    }

    int unloadKeyphraseSoundModel(int keyphraseId) {
        synchronized (this.mLock) {
            MetricsLogger.count(this.mContext, "sth_unload_keyphrase_sound_model", 1);
            ModelData modelData = getKeyphraseModelDataLocked(keyphraseId);
            if (this.mModule == null || modelData == null || modelData.getHandle() == Integer.MIN_VALUE || !modelData.isKeyphraseModel()) {
                return Integer.MIN_VALUE;
            }
            modelData.setRequested(false);
            int status = updateRecognitionLocked(modelData, isRecognitionAllowed(), false);
            if (status != 0) {
                Slog.w(TAG, "Stop recognition failed for keyphrase ID:" + status);
            }
            int status2 = this.mModule.unloadSoundModel(modelData.getHandle());
            if (status2 != 0) {
                Slog.w(TAG, "unloadKeyphraseSoundModel call failed with " + status2);
            }
            removeKeyphraseModelLocked(keyphraseId);
            return status2;
        }
    }

    int unloadGenericSoundModel(UUID modelId) {
        int status;
        synchronized (this.mLock) {
            MetricsLogger.count(this.mContext, "sth_unload_generic_sound_model", 1);
            if (modelId == null || this.mModule == null) {
                return Integer.MIN_VALUE;
            }
            ModelData modelData = this.mModelDataMap.get(modelId);
            if (modelData == null || !modelData.isGenericModel()) {
                Slog.w(TAG, "Unload error: Attempting unload invalid generic model with id:" + modelId);
                return Integer.MIN_VALUE;
            }
            if (!modelData.isModelLoaded()) {
                Slog.i(TAG, "Unload: Given generic model is not loaded:" + modelId);
                return 0;
            }
            if (modelData.isModelStarted() && (status = stopRecognitionLocked(modelData, false)) != 0) {
                Slog.w(TAG, "stopGenericRecognition failed: " + status);
            }
            int status2 = this.mModule.unloadSoundModel(modelData.getHandle());
            if (status2 != 0) {
                Slog.w(TAG, "unloadGenericSoundModel() call failed with " + status2);
                Slog.w(TAG, "unloadGenericSoundModel() force-marking model as unloaded.");
            }
            this.mModelDataMap.remove(modelId);
            return status2;
        }
    }

    public void onRecognition(SoundTrigger.RecognitionEvent event) {
        if (event == null) {
            Slog.w(TAG, "Null recognition event!");
            return;
        }
        if (!(event instanceof SoundTrigger.KeyphraseRecognitionEvent) && !(event instanceof SoundTrigger.GenericRecognitionEvent)) {
            Slog.w(TAG, "Invalid recognition event type (not one of generic or keyphrase)!");
            return;
        }
        synchronized (this.mLock) {
            switch (event.status) {
                case 0:
                    if (isKeyphraseRecognitionEvent(event)) {
                        onKeyphraseRecognitionSuccessLocked((SoundTrigger.KeyphraseRecognitionEvent) event);
                    } else {
                        onGenericRecognitionSuccessLocked((SoundTrigger.GenericRecognitionEvent) event);
                    }
                    break;
                case 1:
                    onRecognitionAbortLocked(event);
                    break;
                case 2:
                    onRecognitionFailureLocked();
                    break;
            }
        }
    }

    private boolean isKeyphraseRecognitionEvent(SoundTrigger.RecognitionEvent event) {
        return event instanceof SoundTrigger.KeyphraseRecognitionEvent;
    }

    private void onGenericRecognitionSuccessLocked(SoundTrigger.GenericRecognitionEvent event) {
        MetricsLogger.count(this.mContext, "sth_generic_recognition_event", 1);
        if (event.status != 0) {
            return;
        }
        ModelData model = getModelDataForLocked(event.soundModelHandle);
        if (model == null || !model.isGenericModel()) {
            Slog.w(TAG, "Generic recognition event: Model does not exist for handle: " + event.soundModelHandle);
            return;
        }
        IRecognitionStatusCallback callback = model.getCallback();
        if (callback == null) {
            Slog.w(TAG, "Generic recognition event: Null callback for model handle: " + event.soundModelHandle);
            return;
        }
        try {
            callback.onGenericSoundTriggerDetected(event);
        } catch (RemoteException e) {
            Slog.w(TAG, "RemoteException in onGenericSoundTriggerDetected", e);
        }
        model.setStopped();
        SoundTrigger.RecognitionConfig config = model.getRecognitionConfig();
        if (config == null) {
            Slog.w(TAG, "Generic recognition event: Null RecognitionConfig for model handle: " + event.soundModelHandle);
            return;
        }
        model.setRequested(config.allowMultipleTriggers);
        if (!model.isRequested()) {
            return;
        }
        updateRecognitionLocked(model, isRecognitionAllowed(), true);
    }

    public void onSoundModelUpdate(SoundTrigger.SoundModelEvent event) {
        if (event == null) {
            Slog.w(TAG, "Invalid sound model event!");
            return;
        }
        synchronized (this.mLock) {
            MetricsLogger.count(this.mContext, "sth_sound_model_updated", 1);
            onSoundModelUpdatedLocked(event);
        }
    }

    public void onServiceStateChange(int state) {
        synchronized (this.mLock) {
            onServiceStateChangedLocked(1 == state);
        }
    }

    public void onServiceDied() {
        Slog.e(TAG, "onServiceDied!!");
        MetricsLogger.count(this.mContext, "sth_service_died", 1);
        synchronized (this.mLock) {
            onServiceDiedLocked();
        }
    }

    private void onCallStateChangedLocked(boolean callActive) {
        if (this.mCallActive == callActive) {
            return;
        }
        this.mCallActive = callActive;
        updateAllRecognitionsLocked(true);
    }

    private void onPowerSaveModeChangedLocked(boolean isPowerSaveMode) {
        if (this.mIsPowerSaveMode == isPowerSaveMode) {
            return;
        }
        this.mIsPowerSaveMode = isPowerSaveMode;
        updateAllRecognitionsLocked(true);
    }

    private void onSoundModelUpdatedLocked(SoundTrigger.SoundModelEvent event) {
    }

    private void onServiceStateChangedLocked(boolean disabled) {
        if (disabled == this.mServiceDisabled) {
            return;
        }
        this.mServiceDisabled = disabled;
        updateAllRecognitionsLocked(true);
    }

    private void onRecognitionAbortLocked(SoundTrigger.RecognitionEvent event) {
        Slog.w(TAG, "Recognition aborted");
        MetricsLogger.count(this.mContext, "sth_recognition_aborted", 1);
        ModelData modelData = getModelDataForLocked(event.soundModelHandle);
        if (modelData == null) {
            return;
        }
        modelData.setStopped();
    }

    private void onRecognitionFailureLocked() {
        Slog.w(TAG, "Recognition failure");
        MetricsLogger.count(this.mContext, "sth_recognition_failure_event", 1);
        try {
            sendErrorCallbacksToAll(Integer.MIN_VALUE);
        } catch (RemoteException e) {
            Slog.w(TAG, "RemoteException in onError", e);
        } finally {
            internalClearModelStateLocked();
            internalClearGlobalStateLocked();
        }
    }

    private int getKeyphraseIdFromEvent(SoundTrigger.KeyphraseRecognitionEvent event) {
        if (event == null) {
            Slog.w(TAG, "Null RecognitionEvent received.");
            return Integer.MIN_VALUE;
        }
        SoundTrigger.KeyphraseRecognitionExtra[] keyphraseExtras = event.keyphraseExtras;
        if (keyphraseExtras == null || keyphraseExtras.length == 0) {
            Slog.w(TAG, "Invalid keyphrase recognition event!");
            return Integer.MIN_VALUE;
        }
        return keyphraseExtras[0].id;
    }

    private void onKeyphraseRecognitionSuccessLocked(SoundTrigger.KeyphraseRecognitionEvent event) {
        Slog.i(TAG, "Recognition success");
        MetricsLogger.count(this.mContext, "sth_keyphrase_recognition_event", 1);
        int keyphraseId = getKeyphraseIdFromEvent(event);
        ModelData modelData = getKeyphraseModelDataLocked(keyphraseId);
        if (modelData == null || !modelData.isKeyphraseModel()) {
            Slog.e(TAG, "Keyphase model data does not exist for ID:" + keyphraseId);
            return;
        }
        if (modelData.getCallback() == null) {
            Slog.w(TAG, "Received onRecognition event without callback for keyphrase model.");
            return;
        }
        try {
            modelData.getCallback().onKeyphraseDetected(event);
        } catch (RemoteException e) {
            Slog.w(TAG, "RemoteException in onKeyphraseDetected", e);
        }
        modelData.setStopped();
        SoundTrigger.RecognitionConfig config = modelData.getRecognitionConfig();
        if (config != null) {
            modelData.setRequested(config.allowMultipleTriggers);
        }
        if (!modelData.isRequested()) {
            return;
        }
        updateRecognitionLocked(modelData, isRecognitionAllowed(), true);
    }

    private void updateAllRecognitionsLocked(boolean notify) {
        boolean isAllowed = isRecognitionAllowed();
        for (ModelData modelData : this.mModelDataMap.values()) {
            updateRecognitionLocked(modelData, isAllowed, notify);
        }
    }

    private int updateRecognitionLocked(ModelData model, boolean isAllowed, boolean notify) {
        boolean start = model.isRequested() ? isAllowed : false;
        if (start == model.isModelStarted()) {
            return 0;
        }
        if (start) {
            return startRecognitionLocked(model, notify);
        }
        return stopRecognitionLocked(model, notify);
    }

    private void onServiceDiedLocked() {
        try {
            try {
                MetricsLogger.count(this.mContext, "sth_service_died", 1);
                sendErrorCallbacksToAll(SoundTrigger.STATUS_DEAD_OBJECT);
                internalClearModelStateLocked();
                internalClearGlobalStateLocked();
            } catch (RemoteException e) {
                Slog.w(TAG, "RemoteException in onError", e);
                internalClearModelStateLocked();
                internalClearGlobalStateLocked();
                if (this.mModule == null) {
                    return;
                } else {
                    this.mModule.detach();
                }
            }
            if (this.mModule == null) {
                return;
            }
            this.mModule.detach();
            this.mModule = null;
        } catch (Throwable th) {
            internalClearModelStateLocked();
            internalClearGlobalStateLocked();
            if (this.mModule != null) {
                this.mModule.detach();
                this.mModule = null;
            }
            throw th;
        }
    }

    private void internalClearGlobalStateLocked() {
        this.mTelephonyManager.listen(this.mPhoneStateListener, 0);
        if (this.mPowerSaveModeListener == null) {
            return;
        }
        this.mContext.unregisterReceiver(this.mPowerSaveModeListener);
        this.mPowerSaveModeListener = null;
    }

    private void internalClearModelStateLocked() {
        for (ModelData modelData : this.mModelDataMap.values()) {
            modelData.clearState();
        }
    }

    class MyCallStateListener extends PhoneStateListener {
        MyCallStateListener() {
        }

        @Override
        public void onCallStateChanged(int state, String arg1) {
            synchronized (SoundTriggerHelper.this.mLock) {
                SoundTriggerHelper.this.onCallStateChangedLocked(state != 0);
            }
        }
    }

    class PowerSaveModeListener extends BroadcastReceiver {
        PowerSaveModeListener() {
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            if (!"android.os.action.POWER_SAVE_MODE_CHANGED".equals(intent.getAction())) {
                return;
            }
            boolean active = SoundTriggerHelper.this.mPowerManager.isPowerSaveMode();
            synchronized (SoundTriggerHelper.this.mLock) {
                SoundTriggerHelper.this.onPowerSaveModeChangedLocked(active);
            }
        }
    }

    void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        synchronized (this.mLock) {
            pw.print("  module properties=");
            pw.println((Object) (this.mModuleProperties == null ? "null" : this.mModuleProperties));
            pw.print("  call active=");
            pw.println(this.mCallActive);
            pw.print("  power save mode active=");
            pw.println(this.mIsPowerSaveMode);
            pw.print("  service disabled=");
            pw.println(this.mServiceDisabled);
        }
    }

    private void initializeTelephonyAndPowerStateListeners() {
        this.mCallActive = this.mTelephonyManager.getCallState() != 0;
        this.mTelephonyManager.listen(this.mPhoneStateListener, 32);
        if (this.mPowerSaveModeListener == null) {
            this.mPowerSaveModeListener = new PowerSaveModeListener();
            this.mContext.registerReceiver(this.mPowerSaveModeListener, new IntentFilter("android.os.action.POWER_SAVE_MODE_CHANGED"));
        }
        this.mIsPowerSaveMode = this.mPowerManager.isPowerSaveMode();
    }

    private void sendErrorCallbacksToAll(int errorCode) throws RemoteException {
        for (ModelData modelData : this.mModelDataMap.values()) {
            IRecognitionStatusCallback callback = modelData.getCallback();
            if (callback != null) {
                callback.onError(Integer.MIN_VALUE);
            }
        }
    }

    private ModelData getOrCreateGenericModelDataLocked(UUID modelId) {
        ModelData modelData = this.mModelDataMap.get(modelId);
        if (modelData == null) {
            ModelData modelData2 = ModelData.createGenericModelData(modelId);
            this.mModelDataMap.put(modelId, modelData2);
            return modelData2;
        }
        if (!modelData.isGenericModel()) {
            Slog.e(TAG, "UUID already used for non-generic model.");
            return null;
        }
        return modelData;
    }

    private void removeKeyphraseModelLocked(int keyphraseId) {
        UUID uuid = this.mKeyphraseUuidMap.get(Integer.valueOf(keyphraseId));
        if (uuid == null) {
            return;
        }
        this.mModelDataMap.remove(uuid);
        this.mKeyphraseUuidMap.remove(Integer.valueOf(keyphraseId));
    }

    private ModelData getKeyphraseModelDataLocked(int keyphraseId) {
        UUID uuid = this.mKeyphraseUuidMap.get(Integer.valueOf(keyphraseId));
        if (uuid == null) {
            return null;
        }
        return this.mModelDataMap.get(uuid);
    }

    private ModelData createKeyphraseModelDataLocked(UUID modelId, int keyphraseId) {
        this.mKeyphraseUuidMap.remove(Integer.valueOf(keyphraseId));
        this.mModelDataMap.remove(modelId);
        this.mKeyphraseUuidMap.put(Integer.valueOf(keyphraseId), modelId);
        ModelData modelData = ModelData.createKeyphraseModelData(modelId);
        this.mModelDataMap.put(modelId, modelData);
        return modelData;
    }

    private ModelData getModelDataForLocked(int modelHandle) {
        for (ModelData model : this.mModelDataMap.values()) {
            if (model.getHandle() == modelHandle) {
                return model;
            }
        }
        return null;
    }

    private boolean isRecognitionAllowed() {
        return (this.mCallActive || this.mServiceDisabled || this.mIsPowerSaveMode) ? false : true;
    }

    private int startRecognitionLocked(ModelData modelData, boolean notify) {
        IRecognitionStatusCallback callback = modelData.getCallback();
        int handle = modelData.getHandle();
        SoundTrigger.RecognitionConfig config = modelData.getRecognitionConfig();
        if (callback == null || handle == Integer.MIN_VALUE || config == null) {
            Slog.w(TAG, "startRecognition: Bad data passed in.");
            MetricsLogger.count(this.mContext, "sth_start_recognition_error", 1);
            return Integer.MIN_VALUE;
        }
        if (!isRecognitionAllowed()) {
            Slog.w(TAG, "startRecognition requested but not allowed.");
            MetricsLogger.count(this.mContext, "sth_start_recognition_not_allowed", 1);
            return 0;
        }
        int status = this.mModule.startRecognition(handle, config);
        if (status != 0) {
            Slog.w(TAG, "startRecognition failed with " + status);
            MetricsLogger.count(this.mContext, "sth_start_recognition_error", 1);
            if (notify) {
                try {
                    callback.onError(status);
                } catch (RemoteException e) {
                    Slog.w(TAG, "RemoteException in onError", e);
                }
            }
        } else {
            Slog.i(TAG, "startRecognition successful.");
            MetricsLogger.count(this.mContext, "sth_start_recognition_success", 1);
            modelData.setStarted();
            if (notify) {
                try {
                    callback.onRecognitionResumed();
                } catch (RemoteException e2) {
                    Slog.w(TAG, "RemoteException in onRecognitionResumed", e2);
                }
            }
        }
        return status;
    }

    private int stopRecognitionLocked(ModelData modelData, boolean notify) {
        IRecognitionStatusCallback callback = modelData.getCallback();
        int status = this.mModule.stopRecognition(modelData.getHandle());
        if (status != 0) {
            Slog.w(TAG, "stopRecognition call failed with " + status);
            MetricsLogger.count(this.mContext, "sth_stop_recognition_error", 1);
            if (notify) {
                try {
                    callback.onError(status);
                } catch (RemoteException e) {
                    Slog.w(TAG, "RemoteException in onError", e);
                }
            }
        } else {
            modelData.setStopped();
            MetricsLogger.count(this.mContext, "sth_stop_recognition_success", 1);
            if (notify) {
                try {
                    callback.onRecognitionPaused();
                } catch (RemoteException e2) {
                    Slog.w(TAG, "RemoteException in onRecognitionPaused", e2);
                }
            }
        }
        return status;
    }

    private void dumpModelStateLocked() {
        for (UUID modelId : this.mModelDataMap.keySet()) {
            ModelData modelData = this.mModelDataMap.get(modelId);
            Slog.i(TAG, "Model :" + modelData.toString());
        }
    }

    private boolean computeRecognitionRunningLocked() {
        if (this.mModuleProperties == null || this.mModule == null) {
            this.mRecognitionRunning = false;
            return this.mRecognitionRunning;
        }
        for (ModelData modelData : this.mModelDataMap.values()) {
            if (modelData.isModelStarted()) {
                this.mRecognitionRunning = true;
                return this.mRecognitionRunning;
            }
        }
        this.mRecognitionRunning = false;
        return this.mRecognitionRunning;
    }

    private static class ModelData {
        static final int MODEL_LOADED = 1;
        static final int MODEL_NOTLOADED = 0;
        static final int MODEL_STARTED = 2;
        private UUID mModelId;
        private int mModelState;
        private int mModelType;
        private boolean mRequested = false;
        private IRecognitionStatusCallback mCallback = null;
        private SoundTrigger.RecognitionConfig mRecognitionConfig = null;
        private int mModelHandle = Integer.MIN_VALUE;
        private SoundTrigger.SoundModel mSoundModel = null;

        private ModelData(UUID modelId, int modelType) {
            this.mModelType = -1;
            this.mModelId = modelId;
            this.mModelType = modelType;
        }

        static ModelData createKeyphraseModelData(UUID modelId) {
            return new ModelData(modelId, 0);
        }

        static ModelData createGenericModelData(UUID modelId) {
            return new ModelData(modelId, 1);
        }

        static ModelData createModelDataOfUnknownType(UUID modelId) {
            return new ModelData(modelId, -1);
        }

        synchronized void setCallback(IRecognitionStatusCallback callback) {
            this.mCallback = callback;
        }

        synchronized IRecognitionStatusCallback getCallback() {
            return this.mCallback;
        }

        synchronized boolean isModelLoaded() {
            boolean z = true;
            synchronized (this) {
                if (this.mModelState != 1) {
                    if (this.mModelState != 2) {
                        z = false;
                    }
                }
            }
            return z;
        }

        synchronized boolean isModelNotLoaded() {
            boolean z;
            synchronized (this) {
                z = this.mModelState == 0;
            }
            return z;
        }

        synchronized void setStarted() {
            this.mModelState = 2;
        }

        synchronized void setStopped() {
            this.mModelState = 1;
        }

        synchronized void setLoaded() {
            this.mModelState = 1;
        }

        synchronized boolean isModelStarted() {
            return this.mModelState == 2;
        }

        synchronized void clearState() {
            this.mModelState = 0;
            this.mModelHandle = Integer.MIN_VALUE;
            this.mRecognitionConfig = null;
            this.mRequested = false;
            this.mCallback = null;
        }

        synchronized void clearCallback() {
            this.mCallback = null;
        }

        synchronized void setHandle(int handle) {
            this.mModelHandle = handle;
        }

        synchronized void setRecognitionConfig(SoundTrigger.RecognitionConfig config) {
            this.mRecognitionConfig = config;
        }

        synchronized int getHandle() {
            return this.mModelHandle;
        }

        synchronized UUID getModelId() {
            return this.mModelId;
        }

        synchronized SoundTrigger.RecognitionConfig getRecognitionConfig() {
            return this.mRecognitionConfig;
        }

        synchronized boolean isRequested() {
            return this.mRequested;
        }

        synchronized void setRequested(boolean requested) {
            this.mRequested = requested;
        }

        synchronized void setSoundModel(SoundTrigger.SoundModel soundModel) {
            this.mSoundModel = soundModel;
        }

        synchronized SoundTrigger.SoundModel getSoundModel() {
            return this.mSoundModel;
        }

        synchronized int getModelType() {
            return this.mModelType;
        }

        synchronized boolean isKeyphraseModel() {
            boolean z;
            synchronized (this) {
                z = this.mModelType == 0;
            }
            return z;
        }

        synchronized boolean isGenericModel() {
            boolean z;
            synchronized (this) {
                z = this.mModelType == 1;
            }
            return z;
        }

        synchronized String stateToString() {
            switch (this.mModelState) {
                case 0:
                    return "NOT_LOADED";
                case 1:
                    return "LOADED";
                case 2:
                    return "STARTED";
                default:
                    return "Unknown state";
            }
        }

        synchronized String requestedToString() {
            return "Requested: " + (this.mRequested ? "Yes" : "No");
        }

        synchronized String callbackToString() {
            return "Callback: " + (this.mCallback != null ? this.mCallback.asBinder() : "null");
        }

        synchronized String uuidToString() {
            return "UUID: " + this.mModelId;
        }

        public synchronized String toString() {
            return "Handle: " + this.mModelHandle + "\nModelState: " + stateToString() + "\n" + requestedToString() + "\n" + callbackToString() + "\n" + uuidToString() + "\n" + modelTypeToString();
        }

        synchronized String modelTypeToString() {
            String type;
            type = null;
            switch (this.mModelType) {
                case -1:
                    type = "Unknown";
                    break;
                case 0:
                    type = "Keyphrase";
                    break;
                case 1:
                    type = "Generic";
                    break;
            }
            return "Model type: " + type + "\n";
        }
    }
}
