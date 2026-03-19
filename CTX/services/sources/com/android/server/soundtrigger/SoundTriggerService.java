package com.android.server.soundtrigger;

import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.ResolveInfo;
import android.hardware.broadcastradio.V2_0.IdentifierType;
import android.hardware.soundtrigger.IRecognitionStatusCallback;
import android.hardware.soundtrigger.SoundTrigger;
import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.soundtrigger.ISoundTriggerDetectionService;
import android.media.soundtrigger.ISoundTriggerDetectionServiceClient;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Parcel;
import android.os.ParcelUuid;
import android.os.Parcelable;
import android.os.PowerManager;
import android.os.RemoteException;
import android.os.UserHandle;
import android.provider.Settings;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.util.Slog;
import com.android.internal.annotations.GuardedBy;
import com.android.internal.app.ISoundTriggerService;
import com.android.internal.util.Preconditions;
import com.android.internal.util.function.pooled.PooledLambda;
import com.android.server.SystemService;
import com.android.server.job.controllers.JobStatus;
import com.android.server.soundtrigger.SoundTriggerService;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.TreeMap;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class SoundTriggerService extends SystemService {
    private static final boolean DEBUG = true;
    private static final String TAG = "SoundTriggerService";
    private PendingIntent.OnFinished mCallbackCompletedHandler;
    private final TreeMap<UUID, IRecognitionStatusCallback> mCallbacks;
    private Object mCallbacksLock;
    final Context mContext;
    private SoundTriggerDbHelper mDbHelper;
    private final TreeMap<UUID, SoundTrigger.SoundModel> mLoadedModels;
    private final LocalSoundTriggerService mLocalSoundTriggerService;
    private Object mLock;

    @GuardedBy("mLock")
    private final ArrayMap<String, NumOps> mNumOpsPerPackage;
    private final SoundTriggerServiceStub mServiceStub;
    private SoundTriggerHelper mSoundTriggerHelper;
    private PowerManager.WakeLock mWakelock;

    public SoundTriggerService(Context context) {
        super(context);
        this.mNumOpsPerPackage = new ArrayMap<>();
        this.mCallbackCompletedHandler = new PendingIntent.OnFinished() {
            @Override
            public void onSendFinished(PendingIntent pendingIntent, Intent intent, int i, String str, Bundle bundle) {
                synchronized (SoundTriggerService.this.mCallbacksLock) {
                    SoundTriggerService.this.mWakelock.release();
                }
            }
        };
        this.mContext = context;
        this.mServiceStub = new SoundTriggerServiceStub();
        this.mLocalSoundTriggerService = new LocalSoundTriggerService(context);
        this.mLoadedModels = new TreeMap<>();
        this.mCallbacksLock = new Object();
        this.mCallbacks = new TreeMap<>();
        this.mLock = new Object();
    }

    @Override
    public void onStart() {
        publishBinderService("soundtrigger", this.mServiceStub);
        publishLocalService(SoundTriggerInternal.class, this.mLocalSoundTriggerService);
    }

    @Override
    public void onBootPhase(int i) {
        if (500 == i) {
            initSoundTriggerHelper();
            this.mLocalSoundTriggerService.setSoundTriggerHelper(this.mSoundTriggerHelper);
        } else if (600 == i) {
            this.mDbHelper = new SoundTriggerDbHelper(this.mContext);
        }
    }

    @Override
    public void onStartUser(int i) {
    }

    @Override
    public void onSwitchUser(int i) {
    }

    private synchronized void initSoundTriggerHelper() {
        if (this.mSoundTriggerHelper == null) {
            this.mSoundTriggerHelper = new SoundTriggerHelper(this.mContext);
        }
    }

    private synchronized boolean isInitialized() {
        if (this.mSoundTriggerHelper == null) {
            Slog.e(TAG, "SoundTriggerHelper not initialized.");
            return false;
        }
        return true;
    }

    class SoundTriggerServiceStub extends ISoundTriggerService.Stub {
        SoundTriggerServiceStub() {
        }

        public boolean onTransact(int i, Parcel parcel, Parcel parcel2, int i2) throws RemoteException {
            try {
                return super.onTransact(i, parcel, parcel2, i2);
            } catch (RuntimeException e) {
                if (!(e instanceof SecurityException)) {
                    Slog.wtf(SoundTriggerService.TAG, "SoundTriggerService Crash", e);
                }
                throw e;
            }
        }

        public int startRecognition(ParcelUuid parcelUuid, IRecognitionStatusCallback iRecognitionStatusCallback, SoundTrigger.RecognitionConfig recognitionConfig) {
            SoundTriggerService.this.enforceCallingPermission("android.permission.MANAGE_SOUND_TRIGGER");
            if (!SoundTriggerService.this.isInitialized()) {
                return Integer.MIN_VALUE;
            }
            Slog.i(SoundTriggerService.TAG, "startRecognition(): Uuid : " + parcelUuid);
            SoundTrigger.GenericSoundModel soundModel = getSoundModel(parcelUuid);
            if (soundModel != null) {
                return SoundTriggerService.this.mSoundTriggerHelper.startGenericRecognition(parcelUuid.getUuid(), soundModel, iRecognitionStatusCallback, recognitionConfig);
            }
            Slog.e(SoundTriggerService.TAG, "Null model in database for id: " + parcelUuid);
            return Integer.MIN_VALUE;
        }

        public int stopRecognition(ParcelUuid parcelUuid, IRecognitionStatusCallback iRecognitionStatusCallback) {
            SoundTriggerService.this.enforceCallingPermission("android.permission.MANAGE_SOUND_TRIGGER");
            Slog.i(SoundTriggerService.TAG, "stopRecognition(): Uuid : " + parcelUuid);
            if (SoundTriggerService.this.isInitialized()) {
                return SoundTriggerService.this.mSoundTriggerHelper.stopGenericRecognition(parcelUuid.getUuid(), iRecognitionStatusCallback);
            }
            return Integer.MIN_VALUE;
        }

        public SoundTrigger.GenericSoundModel getSoundModel(ParcelUuid parcelUuid) {
            SoundTriggerService.this.enforceCallingPermission("android.permission.MANAGE_SOUND_TRIGGER");
            Slog.i(SoundTriggerService.TAG, "getSoundModel(): id = " + parcelUuid);
            return SoundTriggerService.this.mDbHelper.getGenericSoundModel(parcelUuid.getUuid());
        }

        public void updateSoundModel(SoundTrigger.GenericSoundModel genericSoundModel) {
            SoundTriggerService.this.enforceCallingPermission("android.permission.MANAGE_SOUND_TRIGGER");
            Slog.i(SoundTriggerService.TAG, "updateSoundModel(): model = " + genericSoundModel);
            SoundTriggerService.this.mDbHelper.updateGenericSoundModel(genericSoundModel);
        }

        public void deleteSoundModel(ParcelUuid parcelUuid) {
            SoundTriggerService.this.enforceCallingPermission("android.permission.MANAGE_SOUND_TRIGGER");
            Slog.i(SoundTriggerService.TAG, "deleteSoundModel(): id = " + parcelUuid);
            SoundTriggerService.this.mSoundTriggerHelper.unloadGenericSoundModel(parcelUuid.getUuid());
            SoundTriggerService.this.mDbHelper.deleteGenericSoundModel(parcelUuid.getUuid());
        }

        public int loadGenericSoundModel(SoundTrigger.GenericSoundModel genericSoundModel) {
            SoundTriggerService.this.enforceCallingPermission("android.permission.MANAGE_SOUND_TRIGGER");
            if (!SoundTriggerService.this.isInitialized()) {
                return Integer.MIN_VALUE;
            }
            if (genericSoundModel == null || genericSoundModel.uuid == null) {
                Slog.e(SoundTriggerService.TAG, "Invalid sound model");
                return Integer.MIN_VALUE;
            }
            Slog.i(SoundTriggerService.TAG, "loadGenericSoundModel(): id = " + genericSoundModel.uuid);
            synchronized (SoundTriggerService.this.mLock) {
                SoundTrigger.SoundModel soundModel = (SoundTrigger.SoundModel) SoundTriggerService.this.mLoadedModels.get(genericSoundModel.uuid);
                if (soundModel != null && !soundModel.equals(genericSoundModel)) {
                    SoundTriggerService.this.mSoundTriggerHelper.unloadGenericSoundModel(genericSoundModel.uuid);
                    synchronized (SoundTriggerService.this.mCallbacksLock) {
                        SoundTriggerService.this.mCallbacks.remove(genericSoundModel.uuid);
                    }
                }
                SoundTriggerService.this.mLoadedModels.put(genericSoundModel.uuid, genericSoundModel);
            }
            return 0;
        }

        public int loadKeyphraseSoundModel(SoundTrigger.KeyphraseSoundModel keyphraseSoundModel) {
            SoundTriggerService.this.enforceCallingPermission("android.permission.MANAGE_SOUND_TRIGGER");
            if (!SoundTriggerService.this.isInitialized()) {
                return Integer.MIN_VALUE;
            }
            if (keyphraseSoundModel == null || keyphraseSoundModel.uuid == null) {
                Slog.e(SoundTriggerService.TAG, "Invalid sound model");
                return Integer.MIN_VALUE;
            }
            if (keyphraseSoundModel.keyphrases == null || keyphraseSoundModel.keyphrases.length != 1) {
                Slog.e(SoundTriggerService.TAG, "Only one keyphrase per model is currently supported.");
                return Integer.MIN_VALUE;
            }
            Slog.i(SoundTriggerService.TAG, "loadKeyphraseSoundModel(): id = " + keyphraseSoundModel.uuid);
            synchronized (SoundTriggerService.this.mLock) {
                SoundTrigger.SoundModel soundModel = (SoundTrigger.SoundModel) SoundTriggerService.this.mLoadedModels.get(keyphraseSoundModel.uuid);
                if (soundModel != null && !soundModel.equals(keyphraseSoundModel)) {
                    SoundTriggerService.this.mSoundTriggerHelper.unloadKeyphraseSoundModel(keyphraseSoundModel.keyphrases[0].id);
                    synchronized (SoundTriggerService.this.mCallbacksLock) {
                        SoundTriggerService.this.mCallbacks.remove(keyphraseSoundModel.uuid);
                    }
                }
                SoundTriggerService.this.mLoadedModels.put(keyphraseSoundModel.uuid, keyphraseSoundModel);
            }
            return 0;
        }

        public int startRecognitionForService(ParcelUuid parcelUuid, Bundle bundle, ComponentName componentName, SoundTrigger.RecognitionConfig recognitionConfig) {
            Preconditions.checkNotNull(parcelUuid);
            Preconditions.checkNotNull(componentName);
            Preconditions.checkNotNull(recognitionConfig);
            return startRecognitionForInt(parcelUuid, SoundTriggerService.this.new RemoteSoundTriggerDetectionService(parcelUuid.getUuid(), bundle, componentName, Binder.getCallingUserHandle(), recognitionConfig), recognitionConfig);
        }

        public int startRecognitionForIntent(ParcelUuid parcelUuid, PendingIntent pendingIntent, SoundTrigger.RecognitionConfig recognitionConfig) {
            return startRecognitionForInt(parcelUuid, SoundTriggerService.this.new LocalSoundTriggerRecognitionStatusIntentCallback(parcelUuid.getUuid(), pendingIntent, recognitionConfig), recognitionConfig);
        }

        private int startRecognitionForInt(ParcelUuid parcelUuid, IRecognitionStatusCallback iRecognitionStatusCallback, SoundTrigger.RecognitionConfig recognitionConfig) {
            IRecognitionStatusCallback iRecognitionStatusCallback2;
            int iStartKeyphraseRecognition;
            SoundTriggerService.this.enforceCallingPermission("android.permission.MANAGE_SOUND_TRIGGER");
            if (!SoundTriggerService.this.isInitialized()) {
                return Integer.MIN_VALUE;
            }
            Slog.i(SoundTriggerService.TAG, "startRecognition(): id = " + parcelUuid);
            synchronized (SoundTriggerService.this.mLock) {
                SoundTrigger.GenericSoundModel genericSoundModel = (SoundTrigger.SoundModel) SoundTriggerService.this.mLoadedModels.get(parcelUuid.getUuid());
                if (genericSoundModel != null) {
                    synchronized (SoundTriggerService.this.mCallbacksLock) {
                        iRecognitionStatusCallback2 = (IRecognitionStatusCallback) SoundTriggerService.this.mCallbacks.get(parcelUuid.getUuid());
                    }
                    if (iRecognitionStatusCallback2 != null) {
                        Slog.e(SoundTriggerService.TAG, parcelUuid + " is already running");
                        return Integer.MIN_VALUE;
                    }
                    switch (((SoundTrigger.SoundModel) genericSoundModel).type) {
                        case 0:
                            SoundTrigger.KeyphraseSoundModel keyphraseSoundModel = (SoundTrigger.KeyphraseSoundModel) genericSoundModel;
                            iStartKeyphraseRecognition = SoundTriggerService.this.mSoundTriggerHelper.startKeyphraseRecognition(keyphraseSoundModel.keyphrases[0].id, keyphraseSoundModel, iRecognitionStatusCallback, recognitionConfig);
                            break;
                        case 1:
                            iStartKeyphraseRecognition = SoundTriggerService.this.mSoundTriggerHelper.startGenericRecognition(((SoundTrigger.SoundModel) genericSoundModel).uuid, genericSoundModel, iRecognitionStatusCallback, recognitionConfig);
                            break;
                        default:
                            Slog.e(SoundTriggerService.TAG, "Unknown model type");
                            return Integer.MIN_VALUE;
                    }
                    if (iStartKeyphraseRecognition == 0) {
                        synchronized (SoundTriggerService.this.mCallbacksLock) {
                            SoundTriggerService.this.mCallbacks.put(parcelUuid.getUuid(), iRecognitionStatusCallback);
                        }
                        return 0;
                    }
                    Slog.e(SoundTriggerService.TAG, "Failed to start model: " + iStartKeyphraseRecognition);
                    return iStartKeyphraseRecognition;
                }
                Slog.e(SoundTriggerService.TAG, parcelUuid + " is not loaded");
                return Integer.MIN_VALUE;
            }
        }

        public int stopRecognitionForIntent(ParcelUuid parcelUuid) {
            IRecognitionStatusCallback iRecognitionStatusCallback;
            int iStopKeyphraseRecognition;
            SoundTriggerService.this.enforceCallingPermission("android.permission.MANAGE_SOUND_TRIGGER");
            if (!SoundTriggerService.this.isInitialized()) {
                return Integer.MIN_VALUE;
            }
            Slog.i(SoundTriggerService.TAG, "stopRecognition(): id = " + parcelUuid);
            synchronized (SoundTriggerService.this.mLock) {
                SoundTrigger.KeyphraseSoundModel keyphraseSoundModel = (SoundTrigger.SoundModel) SoundTriggerService.this.mLoadedModels.get(parcelUuid.getUuid());
                if (keyphraseSoundModel != null) {
                    synchronized (SoundTriggerService.this.mCallbacksLock) {
                        iRecognitionStatusCallback = (IRecognitionStatusCallback) SoundTriggerService.this.mCallbacks.get(parcelUuid.getUuid());
                    }
                    if (iRecognitionStatusCallback != null) {
                        switch (((SoundTrigger.SoundModel) keyphraseSoundModel).type) {
                            case 0:
                                iStopKeyphraseRecognition = SoundTriggerService.this.mSoundTriggerHelper.stopKeyphraseRecognition(keyphraseSoundModel.keyphrases[0].id, iRecognitionStatusCallback);
                                break;
                            case 1:
                                iStopKeyphraseRecognition = SoundTriggerService.this.mSoundTriggerHelper.stopGenericRecognition(((SoundTrigger.SoundModel) keyphraseSoundModel).uuid, iRecognitionStatusCallback);
                                break;
                            default:
                                Slog.e(SoundTriggerService.TAG, "Unknown model type");
                                return Integer.MIN_VALUE;
                        }
                        if (iStopKeyphraseRecognition == 0) {
                            synchronized (SoundTriggerService.this.mCallbacksLock) {
                                SoundTriggerService.this.mCallbacks.remove(parcelUuid.getUuid());
                            }
                            return 0;
                        }
                        Slog.e(SoundTriggerService.TAG, "Failed to stop model: " + iStopKeyphraseRecognition);
                        return iStopKeyphraseRecognition;
                    }
                    Slog.e(SoundTriggerService.TAG, parcelUuid + " is not running");
                    return Integer.MIN_VALUE;
                }
                Slog.e(SoundTriggerService.TAG, parcelUuid + " is not loaded");
                return Integer.MIN_VALUE;
            }
        }

        public int unloadSoundModel(ParcelUuid parcelUuid) {
            int iUnloadKeyphraseSoundModel;
            SoundTriggerService.this.enforceCallingPermission("android.permission.MANAGE_SOUND_TRIGGER");
            if (!SoundTriggerService.this.isInitialized()) {
                return Integer.MIN_VALUE;
            }
            Slog.i(SoundTriggerService.TAG, "unloadSoundModel(): id = " + parcelUuid);
            synchronized (SoundTriggerService.this.mLock) {
                SoundTrigger.KeyphraseSoundModel keyphraseSoundModel = (SoundTrigger.SoundModel) SoundTriggerService.this.mLoadedModels.get(parcelUuid.getUuid());
                if (keyphraseSoundModel != null) {
                    switch (((SoundTrigger.SoundModel) keyphraseSoundModel).type) {
                        case 0:
                            iUnloadKeyphraseSoundModel = SoundTriggerService.this.mSoundTriggerHelper.unloadKeyphraseSoundModel(keyphraseSoundModel.keyphrases[0].id);
                            break;
                        case 1:
                            iUnloadKeyphraseSoundModel = SoundTriggerService.this.mSoundTriggerHelper.unloadGenericSoundModel(((SoundTrigger.SoundModel) keyphraseSoundModel).uuid);
                            break;
                        default:
                            Slog.e(SoundTriggerService.TAG, "Unknown model type");
                            return Integer.MIN_VALUE;
                    }
                    if (iUnloadKeyphraseSoundModel == 0) {
                        SoundTriggerService.this.mLoadedModels.remove(parcelUuid.getUuid());
                        return 0;
                    }
                    Slog.e(SoundTriggerService.TAG, "Failed to unload model");
                    return iUnloadKeyphraseSoundModel;
                }
                Slog.e(SoundTriggerService.TAG, parcelUuid + " is not loaded");
                return Integer.MIN_VALUE;
            }
        }

        public boolean isRecognitionActive(ParcelUuid parcelUuid) {
            SoundTriggerService.this.enforceCallingPermission("android.permission.MANAGE_SOUND_TRIGGER");
            if (!SoundTriggerService.this.isInitialized()) {
                return false;
            }
            synchronized (SoundTriggerService.this.mCallbacksLock) {
                if (((IRecognitionStatusCallback) SoundTriggerService.this.mCallbacks.get(parcelUuid.getUuid())) == null) {
                    return false;
                }
                return SoundTriggerService.this.mSoundTriggerHelper.isRecognitionRequested(parcelUuid.getUuid());
            }
        }
    }

    private final class LocalSoundTriggerRecognitionStatusIntentCallback extends IRecognitionStatusCallback.Stub {
        private PendingIntent mCallbackIntent;
        private SoundTrigger.RecognitionConfig mRecognitionConfig;
        private UUID mUuid;

        public LocalSoundTriggerRecognitionStatusIntentCallback(UUID uuid, PendingIntent pendingIntent, SoundTrigger.RecognitionConfig recognitionConfig) {
            this.mUuid = uuid;
            this.mCallbackIntent = pendingIntent;
            this.mRecognitionConfig = recognitionConfig;
        }

        public boolean pingBinder() {
            return this.mCallbackIntent != null;
        }

        public void onKeyphraseDetected(SoundTrigger.KeyphraseRecognitionEvent keyphraseRecognitionEvent) {
            if (this.mCallbackIntent != null) {
                SoundTriggerService.this.grabWakeLock();
                Slog.w(SoundTriggerService.TAG, "Keyphrase sound trigger event: " + keyphraseRecognitionEvent);
                Intent intent = new Intent();
                intent.putExtra("android.media.soundtrigger.MESSAGE_TYPE", 0);
                intent.putExtra("android.media.soundtrigger.RECOGNITION_EVENT", (Parcelable) keyphraseRecognitionEvent);
                try {
                    this.mCallbackIntent.send(SoundTriggerService.this.mContext, 0, intent, SoundTriggerService.this.mCallbackCompletedHandler, null);
                    if (!this.mRecognitionConfig.allowMultipleTriggers) {
                        removeCallback(false);
                    }
                } catch (PendingIntent.CanceledException e) {
                    removeCallback(true);
                }
            }
        }

        public void onGenericSoundTriggerDetected(SoundTrigger.GenericRecognitionEvent genericRecognitionEvent) {
            if (this.mCallbackIntent != null) {
                SoundTriggerService.this.grabWakeLock();
                Slog.w(SoundTriggerService.TAG, "Generic sound trigger event: " + genericRecognitionEvent);
                Intent intent = new Intent();
                intent.putExtra("android.media.soundtrigger.MESSAGE_TYPE", 0);
                intent.putExtra("android.media.soundtrigger.RECOGNITION_EVENT", (Parcelable) genericRecognitionEvent);
                try {
                    this.mCallbackIntent.send(SoundTriggerService.this.mContext, 0, intent, SoundTriggerService.this.mCallbackCompletedHandler, null);
                    if (!this.mRecognitionConfig.allowMultipleTriggers) {
                        removeCallback(false);
                    }
                } catch (PendingIntent.CanceledException e) {
                    removeCallback(true);
                }
            }
        }

        public void onError(int i) {
            if (this.mCallbackIntent != null) {
                SoundTriggerService.this.grabWakeLock();
                Slog.i(SoundTriggerService.TAG, "onError: " + i);
                Intent intent = new Intent();
                intent.putExtra("android.media.soundtrigger.MESSAGE_TYPE", 1);
                intent.putExtra("android.media.soundtrigger.STATUS", i);
                try {
                    this.mCallbackIntent.send(SoundTriggerService.this.mContext, 0, intent, SoundTriggerService.this.mCallbackCompletedHandler, null);
                    removeCallback(false);
                } catch (PendingIntent.CanceledException e) {
                    removeCallback(true);
                }
            }
        }

        public void onRecognitionPaused() {
            if (this.mCallbackIntent != null) {
                SoundTriggerService.this.grabWakeLock();
                Slog.i(SoundTriggerService.TAG, "onRecognitionPaused");
                Intent intent = new Intent();
                intent.putExtra("android.media.soundtrigger.MESSAGE_TYPE", 2);
                try {
                    this.mCallbackIntent.send(SoundTriggerService.this.mContext, 0, intent, SoundTriggerService.this.mCallbackCompletedHandler, null);
                } catch (PendingIntent.CanceledException e) {
                    removeCallback(true);
                }
            }
        }

        public void onRecognitionResumed() {
            if (this.mCallbackIntent != null) {
                SoundTriggerService.this.grabWakeLock();
                Slog.i(SoundTriggerService.TAG, "onRecognitionResumed");
                Intent intent = new Intent();
                intent.putExtra("android.media.soundtrigger.MESSAGE_TYPE", 3);
                try {
                    this.mCallbackIntent.send(SoundTriggerService.this.mContext, 0, intent, SoundTriggerService.this.mCallbackCompletedHandler, null);
                } catch (PendingIntent.CanceledException e) {
                    removeCallback(true);
                }
            }
        }

        private void removeCallback(boolean z) {
            this.mCallbackIntent = null;
            synchronized (SoundTriggerService.this.mCallbacksLock) {
                SoundTriggerService.this.mCallbacks.remove(this.mUuid);
                if (z) {
                    SoundTriggerService.this.mWakelock.release();
                }
            }
        }
    }

    private static class NumOps {

        @GuardedBy("mLock")
        private long mLastOpsHourSinceBoot;
        private final Object mLock;

        @GuardedBy("mLock")
        private int[] mNumOps;

        private NumOps() {
            this.mLock = new Object();
            this.mNumOps = new int[24];
        }

        void clearOldOps(long j) {
            synchronized (this.mLock) {
                long jConvert = TimeUnit.HOURS.convert(j, TimeUnit.NANOSECONDS);
                if (this.mLastOpsHourSinceBoot != 0) {
                    long j2 = this.mLastOpsHourSinceBoot;
                    while (true) {
                        j2++;
                        if (j2 > jConvert) {
                            break;
                        } else {
                            this.mNumOps[(int) (j2 % 24)] = 0;
                        }
                    }
                }
            }
        }

        void addOp(long j) {
            synchronized (this.mLock) {
                long jConvert = TimeUnit.HOURS.convert(j, TimeUnit.NANOSECONDS);
                int[] iArr = this.mNumOps;
                int i = (int) (jConvert % 24);
                iArr[i] = iArr[i] + 1;
                this.mLastOpsHourSinceBoot = jConvert;
            }
        }

        int getOpsAdded() {
            int i;
            synchronized (this.mLock) {
                i = 0;
                for (int i2 = 0; i2 < 24; i2++) {
                    try {
                        i += this.mNumOps[i2];
                    } catch (Throwable th) {
                        throw th;
                    }
                }
            }
            return i;
        }
    }

    private static class Operation {
        private final Runnable mDropOp;
        private final ExecuteOp mExecuteOp;
        private final Runnable mSetupOp;

        private interface ExecuteOp {
            void run(int i, ISoundTriggerDetectionService iSoundTriggerDetectionService) throws RemoteException;
        }

        private Operation(Runnable runnable, ExecuteOp executeOp, Runnable runnable2) {
            this.mSetupOp = runnable;
            this.mExecuteOp = executeOp;
            this.mDropOp = runnable2;
        }

        private void setup() {
            if (this.mSetupOp != null) {
                this.mSetupOp.run();
            }
        }

        void run(int i, ISoundTriggerDetectionService iSoundTriggerDetectionService) throws RemoteException {
            setup();
            this.mExecuteOp.run(i, iSoundTriggerDetectionService);
        }

        void drop() {
            setup();
            if (this.mDropOp != null) {
                this.mDropOp.run();
            }
        }
    }

    private class RemoteSoundTriggerDetectionService extends IRecognitionStatusCallback.Stub implements ServiceConnection {
        private static final int MSG_STOP_ALL_PENDING_OPERATIONS = 1;
        private final ISoundTriggerDetectionServiceClient mClient;

        @GuardedBy("mRemoteServiceLock")
        private boolean mDestroyOnceRunningOpsDone;

        @GuardedBy("mRemoteServiceLock")
        private boolean mIsBound;

        @GuardedBy("mRemoteServiceLock")
        private boolean mIsDestroyed;
        private final NumOps mNumOps;

        @GuardedBy("mRemoteServiceLock")
        private int mNumTotalOpsPerformed;
        private final Bundle mParams;
        private final ParcelUuid mPuuid;
        private final SoundTrigger.RecognitionConfig mRecognitionConfig;
        private final PowerManager.WakeLock mRemoteServiceWakeLock;

        @GuardedBy("mRemoteServiceLock")
        private ISoundTriggerDetectionService mService;
        private final ComponentName mServiceName;
        private final UserHandle mUser;
        private final Object mRemoteServiceLock = new Object();

        @GuardedBy("mRemoteServiceLock")
        private final ArrayList<Operation> mPendingOps = new ArrayList<>();

        @GuardedBy("mRemoteServiceLock")
        private final ArraySet<Integer> mRunningOpIds = new ArraySet<>();
        private final Handler mHandler = new Handler(Looper.getMainLooper());

        public RemoteSoundTriggerDetectionService(UUID uuid, Bundle bundle, ComponentName componentName, UserHandle userHandle, SoundTrigger.RecognitionConfig recognitionConfig) {
            this.mPuuid = new ParcelUuid(uuid);
            this.mParams = bundle;
            this.mServiceName = componentName;
            this.mUser = userHandle;
            this.mRecognitionConfig = recognitionConfig;
            this.mRemoteServiceWakeLock = ((PowerManager) SoundTriggerService.this.mContext.getSystemService("power")).newWakeLock(1, "RemoteSoundTriggerDetectionService " + this.mServiceName.getPackageName() + ":" + this.mServiceName.getClassName());
            synchronized (SoundTriggerService.this.mLock) {
                NumOps numOps = (NumOps) SoundTriggerService.this.mNumOpsPerPackage.get(this.mServiceName.getPackageName());
                if (numOps == null) {
                    numOps = new NumOps();
                    SoundTriggerService.this.mNumOpsPerPackage.put(this.mServiceName.getPackageName(), numOps);
                }
                this.mNumOps = numOps;
            }
            this.mClient = new ISoundTriggerDetectionServiceClient.Stub() {
                public void onOpFinished(int i) {
                    long jClearCallingIdentity = Binder.clearCallingIdentity();
                    try {
                        synchronized (RemoteSoundTriggerDetectionService.this.mRemoteServiceLock) {
                            RemoteSoundTriggerDetectionService.this.mRunningOpIds.remove(Integer.valueOf(i));
                            if (RemoteSoundTriggerDetectionService.this.mRunningOpIds.isEmpty() && RemoteSoundTriggerDetectionService.this.mPendingOps.isEmpty()) {
                                if (RemoteSoundTriggerDetectionService.this.mDestroyOnceRunningOpsDone) {
                                    RemoteSoundTriggerDetectionService.this.destroy();
                                } else {
                                    RemoteSoundTriggerDetectionService.this.disconnectLocked();
                                }
                            }
                        }
                    } finally {
                        Binder.restoreCallingIdentity(jClearCallingIdentity);
                    }
                }
            };
        }

        public boolean pingBinder() {
            return (this.mIsDestroyed || this.mDestroyOnceRunningOpsDone) ? false : true;
        }

        private void disconnectLocked() {
            if (this.mService != null) {
                try {
                    this.mService.removeClient(this.mPuuid);
                } catch (Exception e) {
                    Slog.e(SoundTriggerService.TAG, this.mPuuid + ": Cannot remove client", e);
                }
                this.mService = null;
            }
            if (this.mIsBound) {
                SoundTriggerService.this.mContext.unbindService(this);
                this.mIsBound = false;
                synchronized (SoundTriggerService.this.mCallbacksLock) {
                    this.mRemoteServiceWakeLock.release();
                }
            }
        }

        private void destroy() {
            Slog.v(SoundTriggerService.TAG, this.mPuuid + ": destroy");
            synchronized (this.mRemoteServiceLock) {
                disconnectLocked();
                this.mIsDestroyed = true;
            }
            if (!this.mDestroyOnceRunningOpsDone) {
                synchronized (SoundTriggerService.this.mCallbacksLock) {
                    SoundTriggerService.this.mCallbacks.remove(this.mPuuid.getUuid());
                }
            }
        }

        private void stopAllPendingOperations() {
            synchronized (this.mRemoteServiceLock) {
                if (this.mIsDestroyed) {
                    return;
                }
                if (this.mService != null) {
                    int size = this.mRunningOpIds.size();
                    for (int i = 0; i < size; i++) {
                        try {
                            this.mService.onStopOperation(this.mPuuid, this.mRunningOpIds.valueAt(i).intValue());
                        } catch (Exception e) {
                            Slog.e(SoundTriggerService.TAG, this.mPuuid + ": Could not stop operation " + this.mRunningOpIds.valueAt(i), e);
                        }
                    }
                    this.mRunningOpIds.clear();
                }
                disconnectLocked();
            }
        }

        private void bind() {
            long jClearCallingIdentity = Binder.clearCallingIdentity();
            try {
                Intent intent = new Intent();
                intent.setComponent(this.mServiceName);
                ResolveInfo resolveInfoResolveServiceAsUser = SoundTriggerService.this.mContext.getPackageManager().resolveServiceAsUser(intent, 268435588, this.mUser.getIdentifier());
                if (resolveInfoResolveServiceAsUser == null) {
                    Slog.w(SoundTriggerService.TAG, this.mPuuid + ": " + this.mServiceName + " not found");
                    return;
                }
                if (!"android.permission.BIND_SOUND_TRIGGER_DETECTION_SERVICE".equals(resolveInfoResolveServiceAsUser.serviceInfo.permission)) {
                    Slog.w(SoundTriggerService.TAG, this.mPuuid + ": " + this.mServiceName + " does not require android.permission.BIND_SOUND_TRIGGER_DETECTION_SERVICE");
                    return;
                }
                this.mIsBound = SoundTriggerService.this.mContext.bindServiceAsUser(intent, this, 67108865, this.mUser);
                if (this.mIsBound) {
                    this.mRemoteServiceWakeLock.acquire();
                } else {
                    Slog.w(SoundTriggerService.TAG, this.mPuuid + ": Could not bind to " + this.mServiceName);
                }
            } finally {
                Binder.restoreCallingIdentity(jClearCallingIdentity);
            }
        }

        private void runOrAddOperation(Operation operation) {
            synchronized (this.mRemoteServiceLock) {
                if (!this.mIsDestroyed && !this.mDestroyOnceRunningOpsDone) {
                    if (this.mService == null) {
                        this.mPendingOps.add(operation);
                        if (!this.mIsBound) {
                            bind();
                        }
                    } else {
                        long jNanoTime = System.nanoTime();
                        this.mNumOps.clearOldOps(jNanoTime);
                        Settings.Global.getInt(SoundTriggerService.this.mContext.getContentResolver(), "max_sound_trigger_detection_service_ops_per_day", Integer.MAX_VALUE);
                        this.mNumOps.getOpsAdded();
                        this.mNumOps.addOp(jNanoTime);
                        int i = this.mNumTotalOpsPerformed;
                        do {
                            this.mNumTotalOpsPerformed++;
                        } while (this.mRunningOpIds.contains(Integer.valueOf(i)));
                        Slog.v(SoundTriggerService.TAG, this.mPuuid + ": runOp " + i);
                        operation.run(i, this.mService);
                        this.mRunningOpIds.add(Integer.valueOf(i));
                        if (this.mPendingOps.isEmpty() && this.mRunningOpIds.isEmpty()) {
                            if (this.mDestroyOnceRunningOpsDone) {
                                destroy();
                            } else {
                                disconnectLocked();
                            }
                        } else {
                            this.mHandler.removeMessages(1);
                            this.mHandler.sendMessageDelayed(PooledLambda.obtainMessage(new Consumer() {
                                @Override
                                public final void accept(Object obj) {
                                    ((SoundTriggerService.RemoteSoundTriggerDetectionService) obj).stopAllPendingOperations();
                                }
                            }, this).setWhat(1), Settings.Global.getLong(SoundTriggerService.this.mContext.getContentResolver(), "sound_trigger_detection_service_op_timeout", JobStatus.NO_LATEST_RUNTIME));
                        }
                    }
                    return;
                }
                Slog.w(SoundTriggerService.TAG, this.mPuuid + ": Dropped operation as already destroyed or marked for destruction");
                operation.drop();
            }
        }

        public void onKeyphraseDetected(SoundTrigger.KeyphraseRecognitionEvent keyphraseRecognitionEvent) {
            Slog.w(SoundTriggerService.TAG, this.mPuuid + "->" + this.mServiceName + ": IGNORED onKeyphraseDetected(" + keyphraseRecognitionEvent + ")");
        }

        private AudioRecord createAudioRecordForEvent(SoundTrigger.GenericRecognitionEvent genericRecognitionEvent) {
            int sampleRate;
            int i;
            AudioAttributes.Builder builder = new AudioAttributes.Builder();
            builder.setInternalCapturePreset(IdentifierType.VENDOR_END);
            AudioAttributes audioAttributesBuild = builder.build();
            AudioFormat captureFormat = genericRecognitionEvent.getCaptureFormat();
            AudioFormat audioFormatBuild = new AudioFormat.Builder().setChannelMask(captureFormat.getChannelMask()).setEncoding(captureFormat.getEncoding()).setSampleRate(captureFormat.getSampleRate()).build();
            if (audioFormatBuild.getSampleRate() == 0) {
                sampleRate = 192000;
            } else {
                sampleRate = audioFormatBuild.getSampleRate();
            }
            if (audioFormatBuild.getChannelCount() == 2) {
                i = 12;
            } else {
                i = 16;
            }
            return new AudioRecord(audioAttributesBuild, audioFormatBuild, AudioRecord.getMinBufferSize(sampleRate, i, audioFormatBuild.getEncoding()), genericRecognitionEvent.getCaptureSession());
        }

        public void onGenericSoundTriggerDetected(final SoundTrigger.GenericRecognitionEvent genericRecognitionEvent) {
            Slog.v(SoundTriggerService.TAG, this.mPuuid + ": Generic sound trigger event: " + genericRecognitionEvent);
            runOrAddOperation(new Operation(new Runnable() {
                @Override
                public final void run() {
                    SoundTriggerService.RemoteSoundTriggerDetectionService.lambda$onGenericSoundTriggerDetected$0(this.f$0);
                }
            }, new Operation.ExecuteOp() {
                @Override
                public final void run(int i, ISoundTriggerDetectionService iSoundTriggerDetectionService) {
                    iSoundTriggerDetectionService.onGenericRecognitionEvent(this.f$0.mPuuid, i, genericRecognitionEvent);
                }
            }, new Runnable() {
                @Override
                public final void run() {
                    SoundTriggerService.RemoteSoundTriggerDetectionService.lambda$onGenericSoundTriggerDetected$2(this.f$0, genericRecognitionEvent);
                }
            }));
        }

        public static void lambda$onGenericSoundTriggerDetected$0(RemoteSoundTriggerDetectionService remoteSoundTriggerDetectionService) {
            if (!remoteSoundTriggerDetectionService.mRecognitionConfig.allowMultipleTriggers) {
                synchronized (SoundTriggerService.this.mCallbacksLock) {
                    SoundTriggerService.this.mCallbacks.remove(remoteSoundTriggerDetectionService.mPuuid.getUuid());
                }
                remoteSoundTriggerDetectionService.mDestroyOnceRunningOpsDone = true;
            }
        }

        public static void lambda$onGenericSoundTriggerDetected$2(RemoteSoundTriggerDetectionService remoteSoundTriggerDetectionService, SoundTrigger.GenericRecognitionEvent genericRecognitionEvent) {
            if (genericRecognitionEvent.isCaptureAvailable()) {
                AudioRecord audioRecordCreateAudioRecordForEvent = remoteSoundTriggerDetectionService.createAudioRecordForEvent(genericRecognitionEvent);
                audioRecordCreateAudioRecordForEvent.startRecording();
                audioRecordCreateAudioRecordForEvent.release();
            }
        }

        public void onError(final int i) {
            Slog.v(SoundTriggerService.TAG, this.mPuuid + ": onError: " + i);
            runOrAddOperation(new Operation(new Runnable() {
                @Override
                public final void run() {
                    SoundTriggerService.RemoteSoundTriggerDetectionService.lambda$onError$3(this.f$0);
                }
            }, new Operation.ExecuteOp() {
                @Override
                public final void run(int i2, ISoundTriggerDetectionService iSoundTriggerDetectionService) {
                    iSoundTriggerDetectionService.onError(this.f$0.mPuuid, i2, i);
                }
            }, null));
        }

        public static void lambda$onError$3(RemoteSoundTriggerDetectionService remoteSoundTriggerDetectionService) {
            synchronized (SoundTriggerService.this.mCallbacksLock) {
                SoundTriggerService.this.mCallbacks.remove(remoteSoundTriggerDetectionService.mPuuid.getUuid());
            }
            remoteSoundTriggerDetectionService.mDestroyOnceRunningOpsDone = true;
        }

        public void onRecognitionPaused() {
            Slog.i(SoundTriggerService.TAG, this.mPuuid + "->" + this.mServiceName + ": IGNORED onRecognitionPaused");
        }

        public void onRecognitionResumed() {
            Slog.i(SoundTriggerService.TAG, this.mPuuid + "->" + this.mServiceName + ": IGNORED onRecognitionResumed");
        }

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            Slog.v(SoundTriggerService.TAG, this.mPuuid + ": onServiceConnected(" + iBinder + ")");
            synchronized (this.mRemoteServiceLock) {
                this.mService = ISoundTriggerDetectionService.Stub.asInterface(iBinder);
                try {
                    this.mService.setClient(this.mPuuid, this.mParams, this.mClient);
                    while (!this.mPendingOps.isEmpty()) {
                        runOrAddOperation(this.mPendingOps.remove(0));
                    }
                } catch (Exception e) {
                    Slog.e(SoundTriggerService.TAG, this.mPuuid + ": Could not init " + this.mServiceName, e);
                }
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            Slog.v(SoundTriggerService.TAG, this.mPuuid + ": onServiceDisconnected");
            synchronized (this.mRemoteServiceLock) {
                this.mService = null;
            }
        }

        @Override
        public void onBindingDied(ComponentName componentName) {
            Slog.v(SoundTriggerService.TAG, this.mPuuid + ": onBindingDied");
            synchronized (this.mRemoteServiceLock) {
                destroy();
            }
        }

        @Override
        public void onNullBinding(ComponentName componentName) {
            Slog.w(SoundTriggerService.TAG, componentName + " for model " + this.mPuuid + " returned a null binding");
            synchronized (this.mRemoteServiceLock) {
                disconnectLocked();
            }
        }
    }

    private void grabWakeLock() {
        synchronized (this.mCallbacksLock) {
            if (this.mWakelock == null) {
                this.mWakelock = ((PowerManager) this.mContext.getSystemService("power")).newWakeLock(1, TAG);
            }
            this.mWakelock.acquire();
        }
    }

    public final class LocalSoundTriggerService extends SoundTriggerInternal {
        private final Context mContext;
        private SoundTriggerHelper mSoundTriggerHelper;

        LocalSoundTriggerService(Context context) {
            this.mContext = context;
        }

        synchronized void setSoundTriggerHelper(SoundTriggerHelper soundTriggerHelper) {
            this.mSoundTriggerHelper = soundTriggerHelper;
        }

        @Override
        public int startRecognition(int i, SoundTrigger.KeyphraseSoundModel keyphraseSoundModel, IRecognitionStatusCallback iRecognitionStatusCallback, SoundTrigger.RecognitionConfig recognitionConfig) {
            if (isInitialized()) {
                return this.mSoundTriggerHelper.startKeyphraseRecognition(i, keyphraseSoundModel, iRecognitionStatusCallback, recognitionConfig);
            }
            return Integer.MIN_VALUE;
        }

        @Override
        public synchronized int stopRecognition(int i, IRecognitionStatusCallback iRecognitionStatusCallback) {
            if (!isInitialized()) {
                return Integer.MIN_VALUE;
            }
            return this.mSoundTriggerHelper.stopKeyphraseRecognition(i, iRecognitionStatusCallback);
        }

        @Override
        public SoundTrigger.ModuleProperties getModuleProperties() {
            if (isInitialized()) {
                return this.mSoundTriggerHelper.getModuleProperties();
            }
            return null;
        }

        @Override
        public int unloadKeyphraseModel(int i) {
            if (isInitialized()) {
                return this.mSoundTriggerHelper.unloadKeyphraseSoundModel(i);
            }
            return Integer.MIN_VALUE;
        }

        @Override
        public void dump(FileDescriptor fileDescriptor, PrintWriter printWriter, String[] strArr) {
            if (isInitialized()) {
                this.mSoundTriggerHelper.dump(fileDescriptor, printWriter, strArr);
            }
        }

        private synchronized boolean isInitialized() {
            if (this.mSoundTriggerHelper == null) {
                Slog.e(SoundTriggerService.TAG, "SoundTriggerHelper not initialized.");
                return false;
            }
            return true;
        }
    }

    private void enforceCallingPermission(String str) {
        if (this.mContext.checkCallingOrSelfPermission(str) != 0) {
            throw new SecurityException("Caller does not hold the permission " + str);
        }
    }
}
