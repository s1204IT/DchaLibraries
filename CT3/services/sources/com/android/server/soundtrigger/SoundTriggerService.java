package com.android.server.soundtrigger;

import android.content.Context;
import android.hardware.soundtrigger.IRecognitionStatusCallback;
import android.hardware.soundtrigger.SoundTrigger;
import android.os.Parcel;
import android.os.ParcelUuid;
import android.os.RemoteException;
import android.util.Slog;
import com.android.internal.app.ISoundTriggerService;
import com.android.server.SystemService;
import java.io.FileDescriptor;
import java.io.PrintWriter;

public class SoundTriggerService extends SystemService {
    private static final boolean DEBUG = true;
    private static final String TAG = "SoundTriggerService";
    final Context mContext;
    private SoundTriggerDbHelper mDbHelper;
    private final LocalSoundTriggerService mLocalSoundTriggerService;
    private final SoundTriggerServiceStub mServiceStub;
    private SoundTriggerHelper mSoundTriggerHelper;

    public SoundTriggerService(Context context) {
        super(context);
        this.mContext = context;
        this.mServiceStub = new SoundTriggerServiceStub();
        this.mLocalSoundTriggerService = new LocalSoundTriggerService(context);
    }

    @Override
    public void onStart() {
        publishBinderService("soundtrigger", this.mServiceStub);
        publishLocalService(SoundTriggerInternal.class, this.mLocalSoundTriggerService);
    }

    @Override
    public void onBootPhase(int phase) {
        if (500 == phase) {
            initSoundTriggerHelper();
            this.mLocalSoundTriggerService.setSoundTriggerHelper(this.mSoundTriggerHelper);
        } else {
            if (600 != phase) {
                return;
            }
            this.mDbHelper = new SoundTriggerDbHelper(this.mContext);
        }
    }

    @Override
    public void onStartUser(int userHandle) {
    }

    @Override
    public void onSwitchUser(int userHandle) {
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

        public boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
            try {
                return super.onTransact(code, data, reply, flags);
            } catch (RuntimeException e) {
                if (!(e instanceof SecurityException)) {
                    Slog.wtf(SoundTriggerService.TAG, "SoundTriggerService Crash", e);
                }
                throw e;
            }
        }

        public int startRecognition(ParcelUuid parcelUuid, IRecognitionStatusCallback callback, SoundTrigger.RecognitionConfig config) {
            SoundTriggerService.this.enforceCallingPermission("android.permission.MANAGE_SOUND_TRIGGER");
            if (!SoundTriggerService.this.isInitialized()) {
                return Integer.MIN_VALUE;
            }
            Slog.i(SoundTriggerService.TAG, "startRecognition(): Uuid : " + parcelUuid);
            SoundTrigger.GenericSoundModel model = getSoundModel(parcelUuid);
            if (model == null) {
                Slog.e(SoundTriggerService.TAG, "Null model in database for id: " + parcelUuid);
                return Integer.MIN_VALUE;
            }
            return SoundTriggerService.this.mSoundTriggerHelper.startGenericRecognition(parcelUuid.getUuid(), model, callback, config);
        }

        public int stopRecognition(ParcelUuid parcelUuid, IRecognitionStatusCallback callback) {
            SoundTriggerService.this.enforceCallingPermission("android.permission.MANAGE_SOUND_TRIGGER");
            Slog.i(SoundTriggerService.TAG, "stopRecognition(): Uuid : " + parcelUuid);
            if (SoundTriggerService.this.isInitialized()) {
                return SoundTriggerService.this.mSoundTriggerHelper.stopGenericRecognition(parcelUuid.getUuid(), callback);
            }
            return Integer.MIN_VALUE;
        }

        public SoundTrigger.GenericSoundModel getSoundModel(ParcelUuid soundModelId) {
            SoundTriggerService.this.enforceCallingPermission("android.permission.MANAGE_SOUND_TRIGGER");
            Slog.i(SoundTriggerService.TAG, "getSoundModel(): id = " + soundModelId);
            SoundTrigger.GenericSoundModel model = SoundTriggerService.this.mDbHelper.getGenericSoundModel(soundModelId.getUuid());
            return model;
        }

        public void updateSoundModel(SoundTrigger.GenericSoundModel soundModel) {
            SoundTriggerService.this.enforceCallingPermission("android.permission.MANAGE_SOUND_TRIGGER");
            Slog.i(SoundTriggerService.TAG, "updateSoundModel(): model = " + soundModel);
            SoundTriggerService.this.mDbHelper.updateGenericSoundModel(soundModel);
        }

        public void deleteSoundModel(ParcelUuid soundModelId) {
            SoundTriggerService.this.enforceCallingPermission("android.permission.MANAGE_SOUND_TRIGGER");
            Slog.i(SoundTriggerService.TAG, "deleteSoundModel(): id = " + soundModelId);
            SoundTriggerService.this.mSoundTriggerHelper.unloadGenericSoundModel(soundModelId.getUuid());
            SoundTriggerService.this.mDbHelper.deleteGenericSoundModel(soundModelId.getUuid());
        }
    }

    public final class LocalSoundTriggerService extends SoundTriggerInternal {
        private final Context mContext;
        private SoundTriggerHelper mSoundTriggerHelper;

        LocalSoundTriggerService(Context context) {
            this.mContext = context;
        }

        synchronized void setSoundTriggerHelper(SoundTriggerHelper helper) {
            this.mSoundTriggerHelper = helper;
        }

        @Override
        public int startRecognition(int keyphraseId, SoundTrigger.KeyphraseSoundModel soundModel, IRecognitionStatusCallback listener, SoundTrigger.RecognitionConfig recognitionConfig) {
            if (isInitialized()) {
                return this.mSoundTriggerHelper.startKeyphraseRecognition(keyphraseId, soundModel, listener, recognitionConfig);
            }
            return Integer.MIN_VALUE;
        }

        @Override
        public synchronized int stopRecognition(int keyphraseId, IRecognitionStatusCallback listener) {
            if (!isInitialized()) {
                return Integer.MIN_VALUE;
            }
            return this.mSoundTriggerHelper.stopKeyphraseRecognition(keyphraseId, listener);
        }

        @Override
        public SoundTrigger.ModuleProperties getModuleProperties() {
            if (isInitialized()) {
                return this.mSoundTriggerHelper.getModuleProperties();
            }
            return null;
        }

        @Override
        public int unloadKeyphraseModel(int keyphraseId) {
            if (isInitialized()) {
                return this.mSoundTriggerHelper.unloadKeyphraseSoundModel(keyphraseId);
            }
            return Integer.MIN_VALUE;
        }

        @Override
        public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
            if (isInitialized()) {
                this.mSoundTriggerHelper.dump(fd, pw, args);
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

    private void enforceCallingPermission(String permission) {
        if (this.mContext.checkCallingOrSelfPermission(permission) == 0) {
        } else {
            throw new SecurityException("Caller does not hold the permission " + permission);
        }
    }
}
