package com.android.server.soundtrigger;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.soundtrigger.IRecognitionStatusCallback;
import android.hardware.soundtrigger.SoundTrigger;
import android.hardware.soundtrigger.SoundTriggerModule;
import android.os.Binder;
import android.os.DeadObjectException;
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
import java.util.Iterator;
import java.util.Map;
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
        ArrayList arrayList = new ArrayList();
        int iListModules = SoundTrigger.listModules(arrayList);
        this.mContext = context;
        this.mTelephonyManager = (TelephonyManager) context.getSystemService("phone");
        this.mPowerManager = (PowerManager) context.getSystemService("power");
        this.mModelDataMap = new HashMap<>();
        this.mKeyphraseUuidMap = new HashMap<>();
        this.mPhoneStateListener = new MyCallStateListener();
        if (iListModules != 0 || arrayList.size() == 0) {
            Slog.w(TAG, "listModules status=" + iListModules + ", # of modules=" + arrayList.size());
            this.mModuleProperties = null;
            this.mModule = null;
            return;
        }
        this.mModuleProperties = (SoundTrigger.ModuleProperties) arrayList.get(0);
    }

    int startGenericRecognition(UUID uuid, SoundTrigger.GenericSoundModel genericSoundModel, IRecognitionStatusCallback iRecognitionStatusCallback, SoundTrigger.RecognitionConfig recognitionConfig) {
        MetricsLogger.count(this.mContext, "sth_start_recognition", 1);
        if (uuid == null || genericSoundModel == null || iRecognitionStatusCallback == null || recognitionConfig == null) {
            Slog.w(TAG, "Passed in bad data to startGenericRecognition().");
            return Integer.MIN_VALUE;
        }
        synchronized (this.mLock) {
            ModelData orCreateGenericModelDataLocked = getOrCreateGenericModelDataLocked(uuid);
            if (orCreateGenericModelDataLocked == null) {
                Slog.w(TAG, "Irrecoverable error occurred, check UUID / sound model data.");
                return Integer.MIN_VALUE;
            }
            return startRecognition(genericSoundModel, orCreateGenericModelDataLocked, iRecognitionStatusCallback, recognitionConfig, Integer.MIN_VALUE);
        }
    }

    int startKeyphraseRecognition(int i, SoundTrigger.KeyphraseSoundModel keyphraseSoundModel, IRecognitionStatusCallback iRecognitionStatusCallback, SoundTrigger.RecognitionConfig recognitionConfig) {
        synchronized (this.mLock) {
            MetricsLogger.count(this.mContext, "sth_start_recognition", 1);
            if (keyphraseSoundModel != null && iRecognitionStatusCallback != null && recognitionConfig != null) {
                ModelData keyphraseModelDataLocked = getKeyphraseModelDataLocked(i);
                if (keyphraseModelDataLocked != null && !keyphraseModelDataLocked.isKeyphraseModel()) {
                    Slog.e(TAG, "Generic model with same UUID exists.");
                    return Integer.MIN_VALUE;
                }
                if (keyphraseModelDataLocked != null && !keyphraseModelDataLocked.getModelId().equals(keyphraseSoundModel.uuid)) {
                    int iCleanUpExistingKeyphraseModelLocked = cleanUpExistingKeyphraseModelLocked(keyphraseModelDataLocked);
                    if (iCleanUpExistingKeyphraseModelLocked != 0) {
                        return iCleanUpExistingKeyphraseModelLocked;
                    }
                    removeKeyphraseModelLocked(i);
                    keyphraseModelDataLocked = null;
                }
                if (keyphraseModelDataLocked == null) {
                    keyphraseModelDataLocked = createKeyphraseModelDataLocked(keyphraseSoundModel.uuid, i);
                }
                return startRecognition(keyphraseSoundModel, keyphraseModelDataLocked, iRecognitionStatusCallback, recognitionConfig, i);
            }
            return Integer.MIN_VALUE;
        }
    }

    private int cleanUpExistingKeyphraseModelLocked(ModelData modelData) {
        int iTryStopAndUnloadLocked = tryStopAndUnloadLocked(modelData, true, true);
        if (iTryStopAndUnloadLocked != 0) {
            Slog.w(TAG, "Unable to stop or unload previous model: " + modelData.toString());
        }
        return iTryStopAndUnloadLocked;
    }

    int startRecognition(SoundTrigger.SoundModel soundModel, ModelData modelData, IRecognitionStatusCallback iRecognitionStatusCallback, SoundTrigger.RecognitionConfig recognitionConfig, int i) {
        boolean zIsModelStarted;
        boolean zIsModelLoaded;
        int iTryStopAndUnloadLocked;
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
                if (!modelData.getSoundModel().equals(soundModel) || !modelData.isModelStarted()) {
                    if (!modelData.getSoundModel().equals(soundModel)) {
                        zIsModelStarted = modelData.isModelStarted();
                        zIsModelLoaded = modelData.isModelLoaded();
                    } else {
                        zIsModelStarted = false;
                        zIsModelLoaded = false;
                    }
                } else {
                    zIsModelStarted = true;
                    zIsModelLoaded = false;
                }
                if ((zIsModelStarted || zIsModelLoaded) && (iTryStopAndUnloadLocked = tryStopAndUnloadLocked(modelData, zIsModelStarted, zIsModelLoaded)) != 0) {
                    Slog.w(TAG, "Unable to stop or unload previous model: " + modelData.toString());
                    return iTryStopAndUnloadLocked;
                }
            }
            IRecognitionStatusCallback callback = modelData.getCallback();
            if (callback != null && callback.asBinder() != iRecognitionStatusCallback.asBinder()) {
                Slog.w(TAG, "Canceling previous recognition for model id: " + modelData.getModelId());
                try {
                    callback.onError(Integer.MIN_VALUE);
                } catch (RemoteException e) {
                    Slog.w(TAG, "RemoteException in onDetectionStopped", e);
                }
                modelData.clearCallback();
            }
            if (!modelData.isModelLoaded()) {
                stopAndUnloadDeadModelsLocked();
                int[] iArr = {Integer.MIN_VALUE};
                int iLoadSoundModel = this.mModule.loadSoundModel(soundModel, iArr);
                if (iLoadSoundModel != 0) {
                    Slog.w(TAG, "loadSoundModel call failed with " + iLoadSoundModel);
                    return iLoadSoundModel;
                }
                if (iArr[0] == Integer.MIN_VALUE) {
                    Slog.w(TAG, "loadSoundModel call returned invalid sound model handle");
                    return Integer.MIN_VALUE;
                }
                modelData.setHandle(iArr[0]);
                modelData.setLoaded();
                Slog.d(TAG, "Sound model loaded with handle:" + iArr[0]);
            }
            modelData.setCallback(iRecognitionStatusCallback);
            modelData.setRequested(true);
            modelData.setRecognitionConfig(recognitionConfig);
            modelData.setSoundModel(soundModel);
            return startRecognitionLocked(modelData, false);
        }
    }

    int stopGenericRecognition(UUID uuid, IRecognitionStatusCallback iRecognitionStatusCallback) {
        synchronized (this.mLock) {
            MetricsLogger.count(this.mContext, "sth_stop_recognition", 1);
            if (iRecognitionStatusCallback != null && uuid != null) {
                ModelData modelData = this.mModelDataMap.get(uuid);
                if (modelData != null && modelData.isGenericModel()) {
                    int iStopRecognition = stopRecognition(modelData, iRecognitionStatusCallback);
                    if (iStopRecognition != 0) {
                        Slog.w(TAG, "stopGenericRecognition failed: " + iStopRecognition);
                    }
                    return iStopRecognition;
                }
                Slog.w(TAG, "Attempting stopRecognition on invalid model with id:" + uuid);
                return Integer.MIN_VALUE;
            }
            Slog.e(TAG, "Null callbackreceived for stopGenericRecognition() for modelid:" + uuid);
            return Integer.MIN_VALUE;
        }
    }

    int stopKeyphraseRecognition(int i, IRecognitionStatusCallback iRecognitionStatusCallback) {
        synchronized (this.mLock) {
            MetricsLogger.count(this.mContext, "sth_stop_recognition", 1);
            if (iRecognitionStatusCallback == null) {
                Slog.e(TAG, "Null callback received for stopKeyphraseRecognition() for keyphraseId:" + i);
                return Integer.MIN_VALUE;
            }
            ModelData keyphraseModelDataLocked = getKeyphraseModelDataLocked(i);
            if (keyphraseModelDataLocked != null && keyphraseModelDataLocked.isKeyphraseModel()) {
                int iStopRecognition = stopRecognition(keyphraseModelDataLocked, iRecognitionStatusCallback);
                return iStopRecognition != 0 ? iStopRecognition : iStopRecognition;
            }
            Slog.e(TAG, "No model exists for given keyphrase Id " + i);
            return Integer.MIN_VALUE;
        }
    }

    private int stopRecognition(ModelData modelData, IRecognitionStatusCallback iRecognitionStatusCallback) {
        synchronized (this.mLock) {
            try {
                if (iRecognitionStatusCallback == null) {
                    return Integer.MIN_VALUE;
                }
                if (this.mModuleProperties != null && this.mModule != null) {
                    IRecognitionStatusCallback callback = modelData.getCallback();
                    if (modelData != null && callback != null && (modelData.isRequested() || modelData.isModelStarted())) {
                        if (callback.asBinder() != iRecognitionStatusCallback.asBinder()) {
                            Slog.w(TAG, "Attempting stopRecognition for another recognition");
                            return Integer.MIN_VALUE;
                        }
                        modelData.setRequested(false);
                        int iUpdateRecognitionLocked = updateRecognitionLocked(modelData, isRecognitionAllowed(), false);
                        if (iUpdateRecognitionLocked != 0) {
                            return iUpdateRecognitionLocked;
                        }
                        modelData.setLoaded();
                        modelData.clearCallback();
                        modelData.setRecognitionConfig(null);
                        if (!computeRecognitionRunningLocked()) {
                            internalClearGlobalStateLocked();
                        }
                        return iUpdateRecognitionLocked;
                    }
                    Slog.w(TAG, "Attempting stopRecognition without a successful startRecognition");
                    return Integer.MIN_VALUE;
                }
                Slog.w(TAG, "Attempting stopRecognition without the capability");
                return Integer.MIN_VALUE;
            } catch (Throwable th) {
                throw th;
            }
        }
    }

    private int tryStopAndUnloadLocked(ModelData modelData, boolean z, boolean z2) {
        int iUnloadSoundModel = 0;
        if (modelData.isModelNotLoaded()) {
            return 0;
        }
        if (z && modelData.isModelStarted() && (iUnloadSoundModel = stopRecognitionLocked(modelData, false)) != 0) {
            Slog.w(TAG, "stopRecognition failed: " + iUnloadSoundModel);
            return iUnloadSoundModel;
        }
        if (z2 && modelData.isModelLoaded()) {
            Slog.d(TAG, "Unloading previously loaded stale model.");
            iUnloadSoundModel = this.mModule.unloadSoundModel(modelData.getHandle());
            MetricsLogger.count(this.mContext, "sth_unloading_stale_model", 1);
            if (iUnloadSoundModel != 0) {
                Slog.w(TAG, "unloadSoundModel call failed with " + iUnloadSoundModel);
            } else {
                modelData.clearState();
            }
        }
        return iUnloadSoundModel;
    }

    public SoundTrigger.ModuleProperties getModuleProperties() {
        return this.mModuleProperties;
    }

    int unloadKeyphraseSoundModel(int i) {
        synchronized (this.mLock) {
            MetricsLogger.count(this.mContext, "sth_unload_keyphrase_sound_model", 1);
            ModelData keyphraseModelDataLocked = getKeyphraseModelDataLocked(i);
            if (this.mModule != null && keyphraseModelDataLocked != null && keyphraseModelDataLocked.getHandle() != Integer.MIN_VALUE && keyphraseModelDataLocked.isKeyphraseModel()) {
                keyphraseModelDataLocked.setRequested(false);
                int iUpdateRecognitionLocked = updateRecognitionLocked(keyphraseModelDataLocked, isRecognitionAllowed(), false);
                if (iUpdateRecognitionLocked != 0) {
                    Slog.w(TAG, "Stop recognition failed for keyphrase ID:" + iUpdateRecognitionLocked);
                }
                int iUnloadSoundModel = this.mModule.unloadSoundModel(keyphraseModelDataLocked.getHandle());
                if (iUnloadSoundModel != 0) {
                    Slog.w(TAG, "unloadKeyphraseSoundModel call failed with " + iUnloadSoundModel);
                }
                removeKeyphraseModelLocked(i);
                return iUnloadSoundModel;
            }
            return Integer.MIN_VALUE;
        }
    }

    int unloadGenericSoundModel(UUID uuid) {
        int iStopRecognitionLocked;
        synchronized (this.mLock) {
            MetricsLogger.count(this.mContext, "sth_unload_generic_sound_model", 1);
            if (uuid != null && this.mModule != null) {
                ModelData modelData = this.mModelDataMap.get(uuid);
                if (modelData != null && modelData.isGenericModel()) {
                    if (!modelData.isModelLoaded()) {
                        Slog.i(TAG, "Unload: Given generic model is not loaded:" + uuid);
                        return 0;
                    }
                    if (modelData.isModelStarted() && (iStopRecognitionLocked = stopRecognitionLocked(modelData, false)) != 0) {
                        Slog.w(TAG, "stopGenericRecognition failed: " + iStopRecognitionLocked);
                    }
                    int iUnloadSoundModel = this.mModule.unloadSoundModel(modelData.getHandle());
                    if (iUnloadSoundModel != 0) {
                        Slog.w(TAG, "unloadGenericSoundModel() call failed with " + iUnloadSoundModel);
                        Slog.w(TAG, "unloadGenericSoundModel() force-marking model as unloaded.");
                    }
                    this.mModelDataMap.remove(uuid);
                    return iUnloadSoundModel;
                }
                Slog.w(TAG, "Unload error: Attempting unload invalid generic model with id:" + uuid);
                return Integer.MIN_VALUE;
            }
            return Integer.MIN_VALUE;
        }
    }

    boolean isRecognitionRequested(UUID uuid) {
        boolean z;
        synchronized (this.mLock) {
            ModelData modelData = this.mModelDataMap.get(uuid);
            z = modelData != null && modelData.isRequested();
        }
        return z;
    }

    public void onRecognition(SoundTrigger.RecognitionEvent recognitionEvent) {
        if (recognitionEvent == null) {
            Slog.w(TAG, "Null recognition event!");
            return;
        }
        if (!(recognitionEvent instanceof SoundTrigger.KeyphraseRecognitionEvent) && !(recognitionEvent instanceof SoundTrigger.GenericRecognitionEvent)) {
            Slog.w(TAG, "Invalid recognition event type (not one of generic or keyphrase)!");
            return;
        }
        synchronized (this.mLock) {
            switch (recognitionEvent.status) {
                case 0:
                    if (isKeyphraseRecognitionEvent(recognitionEvent)) {
                        onKeyphraseRecognitionSuccessLocked((SoundTrigger.KeyphraseRecognitionEvent) recognitionEvent);
                    } else {
                        onGenericRecognitionSuccessLocked((SoundTrigger.GenericRecognitionEvent) recognitionEvent);
                    }
                    break;
                case 1:
                    onRecognitionAbortLocked(recognitionEvent);
                    break;
                case 2:
                    onRecognitionFailureLocked();
                    break;
            }
        }
    }

    private boolean isKeyphraseRecognitionEvent(SoundTrigger.RecognitionEvent recognitionEvent) {
        return recognitionEvent instanceof SoundTrigger.KeyphraseRecognitionEvent;
    }

    private void onGenericRecognitionSuccessLocked(SoundTrigger.GenericRecognitionEvent genericRecognitionEvent) {
        MetricsLogger.count(this.mContext, "sth_generic_recognition_event", 1);
        if (genericRecognitionEvent.status != 0) {
            return;
        }
        ModelData modelDataForLocked = getModelDataForLocked(genericRecognitionEvent.soundModelHandle);
        if (modelDataForLocked == null || !modelDataForLocked.isGenericModel()) {
            Slog.w(TAG, "Generic recognition event: Model does not exist for handle: " + genericRecognitionEvent.soundModelHandle);
            return;
        }
        IRecognitionStatusCallback callback = modelDataForLocked.getCallback();
        if (callback == null) {
            Slog.w(TAG, "Generic recognition event: Null callback for model handle: " + genericRecognitionEvent.soundModelHandle);
            return;
        }
        modelDataForLocked.setStopped();
        try {
            callback.onGenericSoundTriggerDetected(genericRecognitionEvent);
        } catch (DeadObjectException e) {
            forceStopAndUnloadModelLocked(modelDataForLocked, e);
            return;
        } catch (RemoteException e2) {
            Slog.w(TAG, "RemoteException in onGenericSoundTriggerDetected", e2);
        }
        SoundTrigger.RecognitionConfig recognitionConfig = modelDataForLocked.getRecognitionConfig();
        if (recognitionConfig == null) {
            Slog.w(TAG, "Generic recognition event: Null RecognitionConfig for model handle: " + genericRecognitionEvent.soundModelHandle);
            return;
        }
        modelDataForLocked.setRequested(recognitionConfig.allowMultipleTriggers);
        if (modelDataForLocked.isRequested()) {
            updateRecognitionLocked(modelDataForLocked, isRecognitionAllowed(), true);
        }
    }

    public void onSoundModelUpdate(SoundTrigger.SoundModelEvent soundModelEvent) {
        if (soundModelEvent == null) {
            Slog.w(TAG, "Invalid sound model event!");
            return;
        }
        synchronized (this.mLock) {
            MetricsLogger.count(this.mContext, "sth_sound_model_updated", 1);
            onSoundModelUpdatedLocked(soundModelEvent);
        }
    }

    public void onServiceStateChange(int i) {
        synchronized (this.mLock) {
            onServiceStateChangedLocked(1 == i);
        }
    }

    public void onServiceDied() {
        Slog.e(TAG, "onServiceDied!!");
        MetricsLogger.count(this.mContext, "sth_service_died", 1);
        synchronized (this.mLock) {
            onServiceDiedLocked();
        }
    }

    private void onCallStateChangedLocked(boolean z) {
        if (this.mCallActive == z) {
            return;
        }
        this.mCallActive = z;
        updateAllRecognitionsLocked(true);
    }

    private void onPowerSaveModeChangedLocked(boolean z) {
        if (this.mIsPowerSaveMode == z) {
            return;
        }
        this.mIsPowerSaveMode = z;
        updateAllRecognitionsLocked(true);
    }

    private void onSoundModelUpdatedLocked(SoundTrigger.SoundModelEvent soundModelEvent) {
    }

    private void onServiceStateChangedLocked(boolean z) {
        if (z == this.mServiceDisabled) {
            return;
        }
        this.mServiceDisabled = z;
        updateAllRecognitionsLocked(true);
    }

    private void onRecognitionAbortLocked(SoundTrigger.RecognitionEvent recognitionEvent) {
        Slog.w(TAG, "Recognition aborted");
        MetricsLogger.count(this.mContext, "sth_recognition_aborted", 1);
        ModelData modelDataForLocked = getModelDataForLocked(recognitionEvent.soundModelHandle);
        if (modelDataForLocked != null && modelDataForLocked.isModelStarted()) {
            modelDataForLocked.setStopped();
            try {
                modelDataForLocked.getCallback().onRecognitionPaused();
            } catch (DeadObjectException e) {
                forceStopAndUnloadModelLocked(modelDataForLocked, e);
            } catch (RemoteException e2) {
                Slog.w(TAG, "RemoteException in onRecognitionPaused", e2);
            }
        }
    }

    private void onRecognitionFailureLocked() {
        Slog.w(TAG, "Recognition failure");
        MetricsLogger.count(this.mContext, "sth_recognition_failure_event", 1);
        try {
            sendErrorCallbacksToAllLocked(Integer.MIN_VALUE);
        } finally {
            internalClearModelStateLocked();
            internalClearGlobalStateLocked();
        }
    }

    private int getKeyphraseIdFromEvent(SoundTrigger.KeyphraseRecognitionEvent keyphraseRecognitionEvent) {
        if (keyphraseRecognitionEvent == null) {
            Slog.w(TAG, "Null RecognitionEvent received.");
            return Integer.MIN_VALUE;
        }
        SoundTrigger.KeyphraseRecognitionExtra[] keyphraseRecognitionExtraArr = keyphraseRecognitionEvent.keyphraseExtras;
        if (keyphraseRecognitionExtraArr == null || keyphraseRecognitionExtraArr.length == 0) {
            Slog.w(TAG, "Invalid keyphrase recognition event!");
            return Integer.MIN_VALUE;
        }
        return keyphraseRecognitionExtraArr[0].id;
    }

    private void onKeyphraseRecognitionSuccessLocked(SoundTrigger.KeyphraseRecognitionEvent keyphraseRecognitionEvent) {
        Slog.i(TAG, "Recognition success");
        MetricsLogger.count(this.mContext, "sth_keyphrase_recognition_event", 1);
        int keyphraseIdFromEvent = getKeyphraseIdFromEvent(keyphraseRecognitionEvent);
        ModelData keyphraseModelDataLocked = getKeyphraseModelDataLocked(keyphraseIdFromEvent);
        if (keyphraseModelDataLocked == null || !keyphraseModelDataLocked.isKeyphraseModel()) {
            Slog.e(TAG, "Keyphase model data does not exist for ID:" + keyphraseIdFromEvent);
            return;
        }
        if (keyphraseModelDataLocked.getCallback() == null) {
            Slog.w(TAG, "Received onRecognition event without callback for keyphrase model.");
            return;
        }
        keyphraseModelDataLocked.setStopped();
        try {
            keyphraseModelDataLocked.getCallback().onKeyphraseDetected(keyphraseRecognitionEvent);
        } catch (DeadObjectException e) {
            forceStopAndUnloadModelLocked(keyphraseModelDataLocked, e);
            return;
        } catch (RemoteException e2) {
            Slog.w(TAG, "RemoteException in onKeyphraseDetected", e2);
        }
        SoundTrigger.RecognitionConfig recognitionConfig = keyphraseModelDataLocked.getRecognitionConfig();
        if (recognitionConfig != null) {
            keyphraseModelDataLocked.setRequested(recognitionConfig.allowMultipleTriggers);
        }
        if (keyphraseModelDataLocked.isRequested()) {
            updateRecognitionLocked(keyphraseModelDataLocked, isRecognitionAllowed(), true);
        }
    }

    private void updateAllRecognitionsLocked(boolean z) {
        boolean zIsRecognitionAllowed = isRecognitionAllowed();
        Iterator it = new ArrayList(this.mModelDataMap.values()).iterator();
        while (it.hasNext()) {
            updateRecognitionLocked((ModelData) it.next(), zIsRecognitionAllowed, z);
        }
    }

    private int updateRecognitionLocked(ModelData modelData, boolean z, boolean z2) {
        boolean z3 = modelData.isRequested() && z;
        if (z3 == modelData.isModelStarted()) {
            return 0;
        }
        if (z3) {
            return startRecognitionLocked(modelData, z2);
        }
        return stopRecognitionLocked(modelData, z2);
    }

    private void onServiceDiedLocked() {
        try {
            MetricsLogger.count(this.mContext, "sth_service_died", 1);
            sendErrorCallbacksToAllLocked(SoundTrigger.STATUS_DEAD_OBJECT);
        } finally {
            internalClearModelStateLocked();
            internalClearGlobalStateLocked();
            if (this.mModule != null) {
                this.mModule.detach();
                this.mModule = null;
            }
        }
    }

    private void internalClearGlobalStateLocked() {
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        try {
            this.mTelephonyManager.listen(this.mPhoneStateListener, 0);
            Binder.restoreCallingIdentity(jClearCallingIdentity);
            if (this.mPowerSaveModeListener != null) {
                this.mContext.unregisterReceiver(this.mPowerSaveModeListener);
                this.mPowerSaveModeListener = null;
            }
        } catch (Throwable th) {
            Binder.restoreCallingIdentity(jClearCallingIdentity);
            throw th;
        }
    }

    private void internalClearModelStateLocked() {
        Iterator<ModelData> it = this.mModelDataMap.values().iterator();
        while (it.hasNext()) {
            it.next().clearState();
        }
    }

    class MyCallStateListener extends PhoneStateListener {
        MyCallStateListener() {
        }

        @Override
        public void onCallStateChanged(int i, String str) {
            synchronized (SoundTriggerHelper.this.mLock) {
                SoundTriggerHelper.this.onCallStateChangedLocked(i != 0);
            }
        }
    }

    class PowerSaveModeListener extends BroadcastReceiver {
        PowerSaveModeListener() {
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            if ("android.os.action.POWER_SAVE_MODE_CHANGED".equals(intent.getAction())) {
                boolean z = SoundTriggerHelper.this.mPowerManager.getPowerSaveState(8).batterySaverEnabled;
                synchronized (SoundTriggerHelper.this.mLock) {
                    SoundTriggerHelper.this.onPowerSaveModeChangedLocked(z);
                }
            }
        }
    }

    void dump(FileDescriptor fileDescriptor, PrintWriter printWriter, String[] strArr) {
        synchronized (this.mLock) {
            printWriter.print("  module properties=");
            printWriter.println((Object) (this.mModuleProperties == null ? "null" : this.mModuleProperties));
            printWriter.print("  call active=");
            printWriter.println(this.mCallActive);
            printWriter.print("  power save mode active=");
            printWriter.println(this.mIsPowerSaveMode);
            printWriter.print("  service disabled=");
            printWriter.println(this.mServiceDisabled);
        }
    }

    private void initializeTelephonyAndPowerStateListeners() {
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        try {
            this.mCallActive = this.mTelephonyManager.getCallState() != 0;
            this.mTelephonyManager.listen(this.mPhoneStateListener, 32);
            if (this.mPowerSaveModeListener == null) {
                this.mPowerSaveModeListener = new PowerSaveModeListener();
                this.mContext.registerReceiver(this.mPowerSaveModeListener, new IntentFilter("android.os.action.POWER_SAVE_MODE_CHANGED"));
            }
            this.mIsPowerSaveMode = this.mPowerManager.getPowerSaveState(8).batterySaverEnabled;
        } finally {
            Binder.restoreCallingIdentity(jClearCallingIdentity);
        }
    }

    private void sendErrorCallbacksToAllLocked(int i) {
        for (ModelData modelData : this.mModelDataMap.values()) {
            IRecognitionStatusCallback callback = modelData.getCallback();
            if (callback != null) {
                try {
                    callback.onError(i);
                } catch (RemoteException e) {
                    Slog.w(TAG, "RemoteException sendErrorCallbacksToAllLocked for model handle " + modelData.getHandle(), e);
                }
            }
        }
    }

    private void forceStopAndUnloadModelLocked(ModelData modelData, Exception exc) {
        forceStopAndUnloadModelLocked(modelData, exc, null);
    }

    private void forceStopAndUnloadModelLocked(ModelData modelData, Exception exc, Iterator it) {
        if (exc != null) {
            Slog.e(TAG, "forceStopAndUnloadModel", exc);
        }
        if (modelData.isModelStarted()) {
            Slog.d(TAG, "Stopping previously started dangling model " + modelData.getHandle());
            if (this.mModule.stopRecognition(modelData.getHandle()) != 0) {
                modelData.setStopped();
                modelData.setRequested(false);
            } else {
                Slog.e(TAG, "Failed to stop model " + modelData.getHandle());
            }
        }
        if (modelData.isModelLoaded()) {
            Slog.d(TAG, "Unloading previously loaded dangling model " + modelData.getHandle());
            if (this.mModule.unloadSoundModel(modelData.getHandle()) == 0) {
                if (it != null) {
                    it.remove();
                } else {
                    this.mModelDataMap.remove(modelData.getModelId());
                }
                Iterator<Map.Entry<Integer, UUID>> it2 = this.mKeyphraseUuidMap.entrySet().iterator();
                while (it2.hasNext()) {
                    if (it2.next().getValue().equals(modelData.getModelId())) {
                        it2.remove();
                    }
                }
                modelData.clearState();
                return;
            }
            Slog.e(TAG, "Failed to unload model " + modelData.getHandle());
        }
    }

    private void stopAndUnloadDeadModelsLocked() {
        Iterator<Map.Entry<UUID, ModelData>> it = this.mModelDataMap.entrySet().iterator();
        while (it.hasNext()) {
            ModelData value = it.next().getValue();
            if (value.isModelLoaded() && (value.getCallback() == null || (value.getCallback().asBinder() != null && !value.getCallback().asBinder().pingBinder()))) {
                Slog.w(TAG, "Removing model " + value.getHandle() + " that has no clients");
                forceStopAndUnloadModelLocked(value, null, it);
            }
        }
    }

    private ModelData getOrCreateGenericModelDataLocked(UUID uuid) {
        ModelData modelData = this.mModelDataMap.get(uuid);
        if (modelData == null) {
            ModelData modelDataCreateGenericModelData = ModelData.createGenericModelData(uuid);
            this.mModelDataMap.put(uuid, modelDataCreateGenericModelData);
            return modelDataCreateGenericModelData;
        }
        if (!modelData.isGenericModel()) {
            Slog.e(TAG, "UUID already used for non-generic model.");
            return null;
        }
        return modelData;
    }

    private void removeKeyphraseModelLocked(int i) {
        UUID uuid = this.mKeyphraseUuidMap.get(Integer.valueOf(i));
        if (uuid == null) {
            return;
        }
        this.mModelDataMap.remove(uuid);
        this.mKeyphraseUuidMap.remove(Integer.valueOf(i));
    }

    private ModelData getKeyphraseModelDataLocked(int i) {
        UUID uuid = this.mKeyphraseUuidMap.get(Integer.valueOf(i));
        if (uuid == null) {
            return null;
        }
        return this.mModelDataMap.get(uuid);
    }

    private ModelData createKeyphraseModelDataLocked(UUID uuid, int i) {
        this.mKeyphraseUuidMap.remove(Integer.valueOf(i));
        this.mModelDataMap.remove(uuid);
        this.mKeyphraseUuidMap.put(Integer.valueOf(i), uuid);
        ModelData modelDataCreateKeyphraseModelData = ModelData.createKeyphraseModelData(uuid);
        this.mModelDataMap.put(uuid, modelDataCreateKeyphraseModelData);
        return modelDataCreateKeyphraseModelData;
    }

    private ModelData getModelDataForLocked(int i) {
        for (ModelData modelData : this.mModelDataMap.values()) {
            if (modelData.getHandle() == i) {
                return modelData;
            }
        }
        return null;
    }

    private boolean isRecognitionAllowed() {
        return (this.mCallActive || this.mServiceDisabled || this.mIsPowerSaveMode) ? false : true;
    }

    private int startRecognitionLocked(ModelData modelData, boolean z) {
        IRecognitionStatusCallback callback = modelData.getCallback();
        int handle = modelData.getHandle();
        SoundTrigger.RecognitionConfig recognitionConfig = modelData.getRecognitionConfig();
        if (callback == null || handle == Integer.MIN_VALUE || recognitionConfig == null) {
            Slog.w(TAG, "startRecognition: Bad data passed in.");
            MetricsLogger.count(this.mContext, "sth_start_recognition_error", 1);
            return Integer.MIN_VALUE;
        }
        if (!isRecognitionAllowed()) {
            Slog.w(TAG, "startRecognition requested but not allowed.");
            MetricsLogger.count(this.mContext, "sth_start_recognition_not_allowed", 1);
            return 0;
        }
        int iStartRecognition = this.mModule.startRecognition(handle, recognitionConfig);
        if (iStartRecognition != 0) {
            Slog.w(TAG, "startRecognition failed with " + iStartRecognition);
            MetricsLogger.count(this.mContext, "sth_start_recognition_error", 1);
            if (z) {
                try {
                    callback.onError(iStartRecognition);
                } catch (DeadObjectException e) {
                    forceStopAndUnloadModelLocked(modelData, e);
                } catch (RemoteException e2) {
                    Slog.w(TAG, "RemoteException in onError", e2);
                }
            }
        } else {
            Slog.i(TAG, "startRecognition successful.");
            MetricsLogger.count(this.mContext, "sth_start_recognition_success", 1);
            modelData.setStarted();
            if (z) {
                try {
                    callback.onRecognitionResumed();
                } catch (DeadObjectException e3) {
                    forceStopAndUnloadModelLocked(modelData, e3);
                } catch (RemoteException e4) {
                    Slog.w(TAG, "RemoteException in onRecognitionResumed", e4);
                }
            }
        }
        return iStartRecognition;
    }

    private int stopRecognitionLocked(ModelData modelData, boolean z) {
        IRecognitionStatusCallback callback = modelData.getCallback();
        int iStopRecognition = this.mModule.stopRecognition(modelData.getHandle());
        if (iStopRecognition != 0) {
            Slog.w(TAG, "stopRecognition call failed with " + iStopRecognition);
            MetricsLogger.count(this.mContext, "sth_stop_recognition_error", 1);
            if (z) {
                try {
                    callback.onError(iStopRecognition);
                } catch (DeadObjectException e) {
                    forceStopAndUnloadModelLocked(modelData, e);
                } catch (RemoteException e2) {
                    Slog.w(TAG, "RemoteException in onError", e2);
                }
            }
        } else {
            modelData.setStopped();
            MetricsLogger.count(this.mContext, "sth_stop_recognition_success", 1);
            if (z) {
                try {
                    callback.onRecognitionPaused();
                } catch (DeadObjectException e3) {
                    forceStopAndUnloadModelLocked(modelData, e3);
                } catch (RemoteException e4) {
                    Slog.w(TAG, "RemoteException in onRecognitionPaused", e4);
                }
            }
        }
        return iStopRecognition;
    }

    private void dumpModelStateLocked() {
        Iterator<UUID> it = this.mModelDataMap.keySet().iterator();
        while (it.hasNext()) {
            Slog.i(TAG, "Model :" + this.mModelDataMap.get(it.next()).toString());
        }
    }

    private boolean computeRecognitionRunningLocked() {
        if (this.mModuleProperties == null || this.mModule == null) {
            this.mRecognitionRunning = false;
            return this.mRecognitionRunning;
        }
        Iterator<ModelData> it = this.mModelDataMap.values().iterator();
        while (it.hasNext()) {
            if (it.next().isModelStarted()) {
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

        private ModelData(UUID uuid, int i) {
            this.mModelType = -1;
            this.mModelId = uuid;
            this.mModelType = i;
        }

        static ModelData createKeyphraseModelData(UUID uuid) {
            return new ModelData(uuid, 0);
        }

        static ModelData createGenericModelData(UUID uuid) {
            return new ModelData(uuid, 1);
        }

        static ModelData createModelDataOfUnknownType(UUID uuid) {
            return new ModelData(uuid, -1);
        }

        synchronized void setCallback(IRecognitionStatusCallback iRecognitionStatusCallback) {
            this.mCallback = iRecognitionStatusCallback;
        }

        synchronized IRecognitionStatusCallback getCallback() {
            return this.mCallback;
        }

        synchronized boolean isModelLoaded() {
            boolean z;
            z = true;
            if (this.mModelState != 1) {
                if (this.mModelState != 2) {
                    z = false;
                }
            }
            return z;
        }

        synchronized boolean isModelNotLoaded() {
            return this.mModelState == 0;
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

        synchronized void setHandle(int i) {
            this.mModelHandle = i;
        }

        synchronized void setRecognitionConfig(SoundTrigger.RecognitionConfig recognitionConfig) {
            this.mRecognitionConfig = recognitionConfig;
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

        synchronized void setRequested(boolean z) {
            this.mRequested = z;
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
            return this.mModelType == 0;
        }

        synchronized boolean isGenericModel() {
            return this.mModelType == 1;
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
            StringBuilder sb;
            sb = new StringBuilder();
            sb.append("Requested: ");
            sb.append(this.mRequested ? "Yes" : "No");
            return sb.toString();
        }

        synchronized String callbackToString() {
            StringBuilder sb;
            sb = new StringBuilder();
            sb.append("Callback: ");
            sb.append(this.mCallback != null ? this.mCallback.asBinder() : "null");
            return sb.toString();
        }

        synchronized String uuidToString() {
            return "UUID: " + this.mModelId;
        }

        public synchronized String toString() {
            return "Handle: " + this.mModelHandle + "\nModelState: " + stateToString() + "\n" + requestedToString() + "\n" + callbackToString() + "\n" + uuidToString() + "\n" + modelTypeToString();
        }

        synchronized String modelTypeToString() {
            String str;
            str = null;
            switch (this.mModelType) {
                case -1:
                    str = "Unknown";
                    break;
                case 0:
                    str = "Keyphrase";
                    break;
                case 1:
                    str = "Generic";
                    break;
            }
            return "Model type: " + str + "\n";
        }
    }
}
